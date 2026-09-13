#!/usr/bin/env python3
# -*- coding: utf-8 -*-
"""
dex_index.py — 混淆类名「反查」工具

用途
----
Android 加固 / R8 混淆后，类名（如 wj4.x、ro4.d）每次发版都会变，
但被 keep 规则保留的方法名（onDoubleTap、setPlaySpeed、handleVideoEvent …）
通常跨版本稳定。

本工具解析一组 dex，建立「方法/字段 -> 宿主类」的索引，
用于在目标 App 更新后快速重定位映射表里失效的类名。

用法
----
    python3 dex_index.py build   <dex_dir>
    python3 dex_index.py find-method <name> [--sig <substr>] [--class <substr>]
    python3 dex_index.py find-field  <name> [--class <substr>]
    python3 dex_index.py methods     <class>
    python3 dex_index.py exists      <class> [<class> ...]
    python3 dex_index.py which-dex   <class> [<class> ...]
    python3 dex_index.py packages    <pkg_prefix>
    python3 dex_index.py supers      <class>
    python3 dex_index.py subclasses  <base_fqn> [--class <substr>]

示例
----
    python3 dex_index.py build /tmp/dex73732
    python3 dex_index.py find-method onDoubleTap --class shortvideo
    python3 dex_index.py exists ro4.d com.dragon.read.video.layer.a
    python3 dex_index.py subclasses androidx.recyclerview.widget.RecyclerView$ViewHolder \
        --class shortvideo

索引缓存默认写在 <dex_dir>/../dex_index.json，可用 DEX_INDEX_CACHE 覆盖。
"""

import json
import os
import struct
import sys

NO_INDEX = 0xFFFFFFFF


class DexReader:
    def __init__(self, data: bytes):
        self.d = data

    def u4(self, off):
        return struct.unpack_from("<I", self.d, off)[0]

    def u2(self, off):
        return struct.unpack_from("<H", self.d, off)[0]

    def uleb128(self, off):
        result = 0
        shift = 0
        while True:
            b = self.d[off]
            off += 1
            result |= (b & 0x7F) << shift
            if not (b & 0x80):
                break
            shift += 7
        return result, off

    def string(self, idx):
        off = self.u4(self.sid_off + idx * 4) if hasattr(self, "sid_off") else None
        if off is None:
            return ""
        # string_data_item: uleb128 size + MUTF-8 + 0x00
        _, p = self.uleb128(off)
        end = self.d.index(b"\x00", p)
        return self.d[p:end].decode("utf-8", "replace")

    def load(self):
        if self.d[:4] not in (b"dex\n",):
            raise ValueError("not a dex file")
        self.string_ids_size = self.u4(0x38)
        self.sid_off = self.u4(0x3C)
        self.type_ids_size = self.u4(0x40)
        self.tid_off = self.u4(0x44)
        self.proto_ids_size = self.u4(0x48)
        self.pid_off = self.u4(0x4C)
        self.field_ids_size = self.u4(0x50)
        self.fid_off = self.u4(0x54)
        self.method_ids_size = self.u4(0x58)
        self.mid_off = self.u4(0x5C)
        self.class_defs_size = self.u4(0x60)
        self.cid_off = self.u4(0x64)

    def type_desc(self, idx):
        if idx == NO_INDEX or idx >= self.type_ids_size:
            return "?"
        return self.string(self.u4(self.tid_off + idx * 4))

    def proto_sig(self, idx):
        """返回 (params, ret)，均为简单类型描述串列表"""
        off = self.pid_off + idx * 12
        ret_idx = self.u4(off + 4)
        params_off = self.u4(off + 8)
        params = []
        if params_off:
            size = self.u4(params_off)
            for i in range(size):
                t = self.type_desc(self.u2(params_off + 4 + i * 2))
                params.append(t)
        return params, self.type_desc(ret_idx)

    def method(self, idx):
        off = self.mid_off + idx * 8
        class_idx = self.u2(off)
        proto_idx = self.u2(off + 2)
        name_idx = self.u4(off + 4)
        return self.type_desc(class_idx), self.string(name_idx), proto_idx

    def field(self, idx):
        off = self.fid_off + idx * 8
        class_idx = self.u2(off)
        type_idx = self.u2(off + 2)
        name_idx = self.u4(off + 4)
        return self.type_desc(class_idx), self.string(name_idx), self.type_desc(type_idx)


def norm_class(name: str) -> str:
    """Lcom/foo/Bar; -> com.foo.Bar"""
    if name.startswith("L") and name.endswith(";"):
        return name[1:-1].replace("/", ".")
    return name


PRIMITIVES = {
    "V": "void",
    "Z": "boolean",
    "B": "byte",
    "S": "short",
    "C": "char",
    "I": "int",
    "J": "long",
    "F": "float",
    "D": "double",
}


def sig_of(params, ret) -> str:
    def one(t):
        if t.startswith("L") and t.endswith(";"):
            return norm_class(t).rsplit(".", 1)[-1]
        if t.startswith("["):
            return one(t[1:]) + "[]"
        return PRIMITIVES.get(t, t)

    return "(" + "".join(one(p) for p in params) + ")" + one(ret)


def build(dex_dir: str, verbose=True):
    files = sorted(
        f for f in os.listdir(dex_dir) if f.endswith(".dex")
    )
    if not files:
        raise SystemExit(f"no .dex in {dex_dir}")

    methods = []   # dict: name, sig, cls
    fields = []    # dict: name, type, cls
    classes = []   # class name list
    supers = {}    # class -> superclass
    class_dex = {}  # class -> 定义所在 dex 文件名（同一类跨 dex 时取首个定义）

    for fn in files:
        path = os.path.join(dex_dir, fn)
        with open(path, "rb") as fh:
            r = DexReader(fh.read())
        r.load()

        for ci in range(r.class_defs_size):
            c_off = r.cid_off + ci * 32
            class_idx = r.u4(c_off)
            super_idx = r.u4(c_off + 8)
            class_data_off = r.u4(c_off + 24)
            cls = norm_class(r.type_desc(class_idx))
            classes.append(cls)
            class_dex.setdefault(cls, fn)
            sup = norm_class(r.type_desc(super_idx)) if super_idx != NO_INDEX else ""
            if sup:
                supers[cls] = sup
            if not class_data_off:
                continue

            p = class_data_off
            sf, p = r.uleb128(p)
            inf, p = r.uleb128(p)
            dm, p = r.uleb128(p)
            vm, p = r.uleb128(p)

            def read_fields(count, p):
                out = []
                fidx = 0
                for _ in range(count):
                    diff, p = r.uleb128(p)
                    _acc, p = r.uleb128(p)
                    fidx += diff
                    out.append(fidx)
                return out, p

            def read_methods(count, p):
                out = []
                midx = 0
                for _ in range(count):
                    diff, p = r.uleb128(p)
                    _acc, p = r.uleb128(p)
                    _code, p = r.uleb128(p)
                    midx += diff
                    out.append(midx)
                return out, p

            sf_ids, p = read_fields(sf, p)
            inf_ids, p = read_fields(inf, p)
            dm_ids, p = read_methods(dm, p)
            vm_ids, p = read_methods(vm, p)

            for fidx in sf_ids + inf_ids:
                if fidx >= r.field_ids_size:
                    continue
                owner, fname, ftype = r.field(fidx)
                fields.append({"name": fname, "type": norm_class(ftype), "cls": cls})

            for midx in dm_ids + vm_ids:
                if midx >= r.method_ids_size:
                    continue
                _owner, mname, proto_idx = r.method(midx)
                params, ret = r.proto_sig(proto_idx)
                methods.append({"name": mname, "sig": sig_of(params, ret), "cls": cls})

        if verbose:
            print(f"  {fn}: classes={r.class_defs_size}", file=sys.stderr)

    index = {"dex_dir": dex_dir, "classes": classes, "methods": methods,
             "fields": fields, "supers": supers, "class_dex": class_dex}
    return index


def cache_path(dex_dir):
    return os.environ.get(
        "DEX_INDEX_CACHE", os.path.join(os.path.dirname(os.path.abspath(dex_dir)), "dex_index.json")
    )


def load_or_build(dex_dir):
    cp = cache_path(dex_dir)
    if os.path.exists(cp) and os.path.getmtime(cp) > os.path.getmtime(dex_dir):
        with open(cp, "r", encoding="utf-8") as f:
            idx = json.load(f)
        if "class_dex" in idx:  # 旧版缓存没有该字段 -> 重建
            return idx
    print("building index (first run, may take ~30s) …", file=sys.stderr)
    idx = build(dex_dir)
    with open(cp, "w", encoding="utf-8") as f:
        json.dump(idx, f)
    print(f"index cached -> {cp}", file=sys.stderr)
    return idx


def usage():
    print(__doc__)
    raise SystemExit(1)


def main(argv):
    if len(argv) < 2:
        usage()
    cmd = argv[1]
    args = []
    opts = {}
    rest = argv[2:]
    i = 0
    while i < len(rest):
        a = rest[i]
        if a.startswith("--"):
            k, sep, v = a[2:].partition("=")
            if sep:
                opts[k] = v
            elif i + 1 < len(rest) and not rest[i + 1].startswith("--"):
                opts[k] = rest[i + 1]
                i += 1
            else:
                opts[k] = ""
        else:
            args.append(a)
        i += 1

    if cmd == "build":
        if not args:
            usage()
        idx = build(args[0])
        cp = cache_path(args[0])
        with open(cp, "w", encoding="utf-8") as f:
            json.dump(idx, f)
        print(f"classes={len(idx['classes'])} methods={len(idx['methods'])} fields={len(idx['fields'])}")
        print(f"cached -> {cp}")
        return

    dex_dir = opts.get("dex", os.environ.get("DEX_DIR", "/tmp/dex73732"))
    idx = load_or_build(dex_dir)

    if cmd == "find-method":
        if not args:
            usage()
        name = args[0]
        sigf = opts.get("sig")
        clsf = opts.get("class")
        rows = [
            m for m in idx["methods"]
            if m["name"] == name
            and (not sigf or sigf in m["sig"])
            and (not clsf or clsf in m["cls"])
        ]
        for m in sorted(rows, key=lambda x: x["cls"]):
            print(f"{m['cls']}\t{m['name']}{m['sig']}")
        print(f"# {len(rows)} hit(s)", file=sys.stderr)

    elif cmd == "find-field":
        if not args:
            usage()
        name = args[0]
        clsf = opts.get("class")
        rows = [
            f for f in idx["fields"]
            if f["name"] == name and (not clsf or clsf in f["cls"])
        ]
        for f in sorted(rows, key=lambda x: x["cls"]):
            print(f"{f['cls']}\t{f['name']}:{f['type']}")
        print(f"# {len(rows)} hit(s)", file=sys.stderr)

    elif cmd == "methods":
        if not args:
            usage()
        cls = args[0]
        rows = [m for m in idx["methods"] if m["cls"] == cls]
        for m in sorted(rows, key=lambda x: x["name"]):
            print(f"{m['name']}{m['sig']}")
        print(f"# {len(rows)} method(s)", file=sys.stderr)

    elif cmd == "exists":
        if not args:
            usage()
        want = set(args)
        found = want & set(idx["classes"])
        for c in args:
            print(f"{'存在' if c in found else '缺失'}\t{c}")

    elif cmd == "which-dex":
        # 类定义在哪个 dex —— jadx --single-class 必须给对 dex 才能反编译
        if not args:
            usage()
        m = idx.get("class_dex") or {}
        if not m:
            print("# 索引缺少 class_dex（旧缓存），请先执行 build 重建", file=sys.stderr)
            raise SystemExit(2)
        for c in args:
            print(f"{m.get(c, '缺失')}\t{c}")

    elif cmd == "packages":
        if not args:
            usage()
        pre = args[0]
        rows = sorted({c for c in idx["classes"] if c.startswith(pre)})
        for c in rows:
            print(c)
        print(f"# {len(rows)} class(es)", file=sys.stderr)

    elif cmd == "supers":
        if not args:
            usage()
        supers = idx.get("supers", {})
        c = args[0]
        chain = []
        seen = set()
        while c and c not in seen:
            seen.add(c)
            chain.append(c)
            c = supers.get(c, "")
        for i, s in enumerate(chain):
            print(("  " * i) + s)

    elif cmd == "subclasses":
        if not args:
            usage()
        base = args[0]
        pref = opts.get("class", "")
        supers = idx.get("supers", {})

        def is_sub(cls):
            cur = supers.get(cls, "")
            seen = set()
            while cur and cur not in seen:
                if cur == base:
                    return True
                seen.add(cur)
                cur = supers.get(cur, "")
            return False

        rows = sorted(c for c in idx["classes"] if is_sub(c) and pref in c)
        for c in rows:
            print(c)
        print(f"# {len(rows)} subclass(es)", file=sys.stderr)

    else:
        usage()


if __name__ == "__main__":
    main(sys.argv)

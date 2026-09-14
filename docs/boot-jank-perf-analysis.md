# 红果启动卡顿性能分析报告

分析对象：`com.phoenix.read.oversea.gp` 7.3.7.32（73732）
模块：`xyz.kejiyu.hongguo` 1.0.2（debug）
设备：OnePlus PLK110 / ColorOS，root + LSPosed(Vector)，adb serial `<设备序列号>`
分析时间：2026-09-14

> **⚠ 口径补充（2026-09-14 追加）**：本文分析的是**首帧**卡顿。用户随后纠正实际感知是
> 「**启动 2 秒左右播放画面卡顿一下**」，该现象已单独归因，见
> [playback-start-jank-analysis.md](./playback-start-jank-analysis.md)。
> 结论同为「宿主自身 GC，与本模块无关」——启动后 `+2.1s` 宿主触发一次性释放 94~381MB 的大 GC，
> 主线程掉 30~34 帧；完全移除模块后该停顿逐项相同。**改代码前请先读该文。**

---

## 一、结论（TL;DR）

| 问题 | 判定 | 证据 |
|---|---|---|
| 「刚进红果」的明显卡顿（进首页首帧停顿 ~355ms） | **不是模块造成的** | 移出模块作用域后首帧 359 / 363ms，与有模块的 356 / 389ms 无差异 |
| 冷启动整体变慢 | **是模块造成的，+50ms（约 +11%）** | 交错 A/B 各 8 次：有模块中位 518.5ms，无模块 463.5ms |
| 首屏滚动/渲染掉帧 | 无系统性影响 | Janky 1.24% / 1.85%（有）vs 1.40% / 1.20%（无） |
| 首帧停顿的真正根因 | 红果自身 **ART GC 停顿**，非 View 绘制 | 首帧窗口内 GC 相关 slice 818 个，并集覆盖 354.7/356ms ≈ 100% |

**一句话**：用户感知的那一下卡，是红果 App 自己的 GC 行为；模块确实有代价，但代价是「启动多花 50ms」，不是「进去之后卡」。

---

## 二、对照实验设计

关键点：**不能只测「有模块」就下结论**，必须构造可逆的对照组。

本机框架（Vector / LSPosed）自带 CLI `/data/adb/lspd/cli`，支持按模块改注入作用域：

```bash
# 对照组：只把海外版移出作用域（国内版与其他包不动，最小侵入）
su -c 'sh /data/adb/lspd/cli scope rm xyz.kejiyu.hongguo com.phoenix.read.oversea.gp/0'

# 恢复
su -c 'sh /data/adb/lspd/cli scope add xyz.kejiyu.hongguo com.phoenix.read.oversea.gp/0'
```

要点：

1. **用 scope 而不是停用整个模块** —— 避免影响国内版与其他模块，且完全可逆。
2. 每次测量前 `am force-stop` 保证冷启动。
3. **交错测量（A/B/A/B）** —— 设备温度、页面缓存、dex2oat 状态会随时间漂移，顺序测两组会得出错误结论。
4. 验证对照真的生效：移出作用域后冷启动，日志目录 `K_红果logs/` 不再产生新的主进程日志。

所用工具：

| 工具 | 用途 |
|---|---|
| `am start -W` | 冷启动 TotalTime / WaitTime |
| `dumpsys gfxinfo <pkg>` | 掉帧率、帧耗时分布直方图 |
| `su -c atrace -t 12 -b 32768 -a <pkg> view gfx sched freq wm am dalvik res binder_driver -o x.html` | 主线程 slice 级 trace |
| `vector-cli scope rm/add` | 构造对照组 |
| 模块自身日志 | 定位模块内部耗时点 |

> atrace 输出实际是**原始 ftrace 文本**（不是 HTML，尽管扩展名为 .html），可直接用正则解析 `tracing_mark_write: B|tid|name` / `E` 配对。

---

## 三、证据链

### 3.1 冷启动耗时（`am start -W`，交错 A/B，各 4 次 × 2 轮）

| 组 | 各次 TotalTime (ms) | 均值 | 中位 |
|---|---|---|---|
| E1 有模块 | 543, 514, 515, 514 | 521.5 | 515 |
| D1 无模块 | 463, 442, 452, 464 | 455.3 | 457.5 |
| E2 有模块 | 471, 522, 525, 545 | 515.8 | 523.5 |
| D2 无模块 | 527, 476, 478, 462 | 485.8 | 477 |

合并：**有模块 518.6ms（中位 518.5）vs 无模块 470.5ms（中位 463.5）→ 差 ≈ 50ms，约 +11%**。

### 3.2 模块内部耗时点（模块日志时间戳差值）

```
[12:51:51.983] onPackageReady: com.phoenix.read.oversea.gp
[12:51:51.991] ── installBusinessHooks ──
[12:51:51.995] 锚点解析：来源=cache 耗时=0ms
[12:51:52.012] installBusinessHooks done
```

多进程多次采样，`onPackageReady → installBusinessHooks done` 稳定在 **26~59ms**，与 A/B 实测的 +50ms 吻合。

两个重要事实：

- **锚点解析走的是 cache，耗时 0ms** —— dex 全量扫描只在首次解析时发生（缓存文件 `anchors-7.3.7.32.txt`），后续启动无重扫开销。这点设计是正确的。
- 该回调位于 **Application 创建之前的同步路径**上，因此这 50ms 直接串在启动关键路径里，无法被系统流水线掩盖。

### 3.3 进入首页首帧（ftrace，2 组对照）

| 组 | 最大帧 | 次大帧 |
|---|---|---|
| A1 有模块 | **355.79ms** | 149.20ms |
| B1 无模块 | **359.14ms** | 138.00ms |
| A2 有模块 | **389.15ms** | 182.58ms |
| B2 无模块 | **362.89ms** | 188.88ms |

有模块均值 372ms，无模块 361ms —— **差异 <3%，在噪声范围内**。

这组数据就是「卡顿不是模块造成的」的直接否定证据：**把模块完全移出，长帧照样出现，甚至更大**。

典型长帧的 slice 栈：

```
Choreographer#doFrame          364.36ms
└ traversal                    355.66ms
  └ draw-VRI[MainFragmentActivity]  355.61ms
    └ Record View#draw()       354.88ms
      └ com.android.internal.policy.DecorView, rect:0,0,1272,2772
        └ ... MainPageDrawerLayout / CustomScrollViewPager / ...
```

### 3.4 首帧 355ms 到底花在哪

对首帧窗口内主线程的所有 slice 做**区间并集**归类：

| 类别 | 去重并集 | 区间数 |
|---|---|---|
| GC / ART 运行时相关 | 354.7ms（占窗口 100%） | 818 |
| View 绘制/布局链 | 356.0ms（父链覆盖全程） | 31 |

窗口内出现的高频 slice：

| slice | 次数 |
|---|---|
| `Marking thread roots` | 534 |
| `Lock contention on InternTable lock` | 60（owner 多为非主线程 10330 / 10222） |
| `ProcessMarkStack` | 67 |
| `GC: Wait For Completion` / `WaitHoldingLocks` | 20+ |

**判定**：首帧那 355ms 不是某个 View 的 `onDraw` 慢，而是主线程在首帧期间被 **ART GC（并发标记的线程根扫描 + InternTable 锁争抢）反复打断/等待**。属于红果 App 自身的内存分配压力 + 字符串 interning 行为。

同时 `dumpsys gfxinfo` 的 GPU 分位只有 2~4ms，**GPU 侧完全不是瓶颈**。

### 3.5 掉帧统计（`dumpsys gfxinfo`，2 组对照）

| 组 | 总帧 | Janky | 99th | Slow UI thread |
|---|---|---|---|---|
| A 有模块 | 806 | 1.24% | 46ms | 9 |
| B 无模块 | 430 | 1.40% | 53ms | 5 |
| A2 有模块 | 433 | 1.85% | 57ms | 7 |
| B2 无模块 | 749 | 1.20% | 32ms | 6 |

掉帧率在 1.2%~1.85% 间随机波动，**看不出模块的系统性影响**。

> 注意：`Number High input latency` 这个指标波动极大（610 / 12 / 37 / 488），与测试时是否触屏强相关，不适合作为归因依据。

### 3.6 运行期扫描行为（代码 + 模块 DIAG 计数）

模块 DIAG 在启动窗口内的计数：

```
DIAG onCreate=1 | matchCall=3212 | inflate=311  | scanTree=3212 | addView=663  | matchHit=10
DIAG onCreate=2 | matchCall=5209 | inflate=345  | scanTree=5209 | addView=998  | matchHit=16
```

对应实现（`hooks/Hooks.kt`）：

- **L5549-5563**：hook `LayoutInflater.inflate`，每次返回 `ViewGroup` 后对**整棵返回子树**做 `scanTreeUnified(result)` 全树遍历。
- **L5607-5620**：hook `ViewGroup.addView`，每次对新增子树做 `scanTreeUnified(v)`。
- **L2305-2331 `scanTreeUnified`**：递归遍历，每个节点 `LogUtil.incr("scanTree")`，并调 `quickMatch`。
- **L568-587 `quickMatch`**：每个节点 `LogUtil.incr("matchCall")`，其中包含
  - `v.javaClass.simpleName`（**每次调用都要做字符串截取，有分配开销**）
  - `v.javaClass.name in gHideClassesSet`
  - `isComposeSeriesBar` → 读 `resources.displayMetrics`
  - `isFullscreenWatchControl` → 读 padding/background 等多项属性
- **L2353-2359 `startPeriodicScan()` 是空实现** —— 没有周期轮询，这点是好的（曾有版本会定时全树扫）。

结论：启动窗口内累计遍历 3212~5209 个节点，每个节点都走一遍上述判定。但**单节点成本低、总量仍在帧预算内**，因此没有表现为可测量的掉帧。它更像是「持续的轻微 CPU 浪费」而非「卡顿源」。

---

## 四、根因

| 现象 | 根因 | 归属 |
|---|---|---|
| 进首页那一下明显卡（~355ms） | ART GC 并发标记的线程根扫描 + InternTable 锁争抢，主线程被反复打断/等待 | **红果自身** |
| 冷启动慢 50ms | 模块在 `onPackageReady`（Application 创建前）同步安装全部 hook | **模块** |
| 首页 inflate 期间额外 CPU | `inflate`/`addView` 上挂全树扫描 | **模块**（未造成可见掉帧） |

---

## 五、优化建议（按预期收益排序）

### P1. 把非首帧必需的 hook 移出启动同步路径（可省大部分 50ms）

`installBusinessHooks` 目前在启动关键路径上同步完成所有 hook 安装，其中相当一部分（各类 Activity 追踪、工具栏 layer、广告拦截、KMP VIP、设置页入口等）**只有进入对应页面才需要**。

建议：把「首帧渲染必需」与「后续才可能命中」的 hook 分成两批，后者通过 `mainHandler.post { ... }` 或 `postDelayed(..., 0/120)` 延后到首帧之后安装。收益直接对应 3.2 节测得的 26~59ms。

注意保持第六章「已知约束」中的隔离要求：分批后各批仍需独立 try/catch。

### P2. `quickMatch` 去掉每节点的字符串操作

```kotlin
// 现状：每个节点一次 simpleName（字符串截取 + 分配）
if (v.javaClass.simpleName == "TreeLifecycleComposeContainer") { ... }
```

`Class.getSimpleName()` 每次都做子串计算。建议改为一次性把目标 Class 缓存成 `Class` 对象，用 `v.javaClass === cachedClass` 比较；或在 `ConcurrentHashMap<Class<*>, String>` 里缓存 simpleName。

### P3. `LogUtil.incr` 在生产关闭

`scanTree` / `matchCall` 两个计数覆盖了启动窗口内 6000+ 次 `ConcurrentHashMap.computeIfAbsent(...).incrementAndGet()`。建议加一个 `@Volatile var diagEnabled = false`，`incr()` 首行直接 return。DIAG 只在调试构建出。

### P4. `LogUtil.write()` 的 SimpleDateFormat 复用

```kotlin
private fun write(level: String, msg: String) {
    val timeFormat = SimpleDateFormat("HH:mm:ss.SSS", Locale.getDefault())  // 每次都 new
```

改为 `ThreadLocal<SimpleDateFormat>` 或直接用 `System.currentTimeMillis()` 记原始值、格式化延后到写盘线程。

### P5. `scanTreeUnified` 限制扫描规模

对 `inflate` 那条路径（返回的子树可能很大），可以加节点数上限（例如 800）或深度上限，超限即放弃本轮扫描，等 `onResume` 的 `scanAllWindows` 兜底。避免极端布局下退化为 O(n²)。

---

## 六、复现步骤

```bash
D=<设备序列号>
PKG=com.phoenix.read.oversea.gp
ACT=com.dragon.read.pages.splash.SplashActivity
CLI="sh /data/adb/lspd/cli"

# 1) 冷启动耗时对照
adb -s $D shell "su -c '$CLI scope rm xyz.kejiyu.hongguo $PKG/0'"   # 无模块
for i in 1 2 3 4; do
  adb -s $D shell "am force-stop $PKG"; sleep 2
  adb -s $D shell "am start -W -n $PKG/$ACT" | grep TotalTime
done
adb -s $D shell "su -c '$CLI scope add xyz.kejiyu.hongguo $PKG/0'"   # 恢复

# 2) 首帧 trace
adb -s $D shell "am force-stop $PKG"; sleep 3
adb -s $D shell "su -c 'atrace -t 12 -b 32768 -a $PKG view gfx sched freq wm am dalvik -o /data/local/tmp/t.html'" &
sleep 2; adb -s $D shell "am start -n $PKG/$ACT"
# 等 atrace 结束后
adb -s $D pull /data/local/tmp/t.html /tmp/t.html
```

`/tmp/boot_trace.html` 保留了「有模块」那份完整 trace（含 dalvik category，含 GC 归因数据，82MB）；其余 3 份已清理（未开 dalvik，拿不到 GC tag）。

解析脚本已固化为 skill：`~/.workbuddy/skills/android-perf-ab-attribution/`（含 SKILL.md 与 `scripts/parse_atrace.py`），下次直接：

```bash
python3 ~/.workbuddy/skills/android-perf-ab-attribution/scripts/parse_atrace.py /tmp/boot_trace.html --match oversea
```

---

## 七、已知约束（改代码时勿踩）

1. 各功能侧 hook 必须**独立 try/catch 隔离**，分批延后安装后依然要遵守（见项目长期笔记）。
2. 锚点缓存按**版本号**命名，改 `installBusinessHooks` 流程时不要动缓存 key 逻辑。
3. 用 CLI 改 scope 做实验后**务必恢复**，否则下次真机验证会得到「功能全失效」的假象。

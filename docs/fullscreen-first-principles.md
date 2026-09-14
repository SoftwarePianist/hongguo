# 全屏播放"各种问题"的第一性分析

> 结论先行：**问题的唯一根因是"同一份 UI 状态有两个所有者"。** 宿主(`com.phoenix.read.oversea.gp` 7.3.7.32)
> 有一套完整自洽的状态机；模块用「改方法入参 / 吞掉调用 / 直接改子视图可见性」三种方式在旁边写入，
> **且从不更新宿主的状态字段** → 宿主"以为"与"实际"分叉 → 于是「停着不收、点了没反应、点了方向反」
> 这一整类症状全部出现。**前三轮都在补"结果"，而问题在"入口"，所以永远追不上。**

本文所有宿主侧事实来自 **jadx 反编译设备上真实 APK**（`/tmp/hg_host.apk`，228MB，23 dex），
逐条给出出处，不做猜测；推断部分单独标注。

---

## 1. 宿主的契约（反编译实证）

### 1.1 唯一状态字段

```java
// com.dragon.read.component.shortvideo.impl.fullscreen.f
public boolean f158498v;                                  // ← 控件可见态（权威状态）
public final Runnable C;                                  // 5s 自动隐藏动作
f(...) { this.f158498v = true; ... }                      // 构造默认 = 可见

public final void m(boolean z19) {                        // 唯一的"写状态"方法
    this.f158498v = z19;                                  //   写状态字段
    this.f158479c.setImmersiveMode(!z19);                 //   同步沉浸模式
    this.f158480d.f163538b = z19;
    k();
}
```

**关键：`m()` 把"状态字段"和"实际沉浸效果"绑在同一行写。** 这是宿主自洽的基础。

### 1.2 唯一对外入口 `sd` —— 它的参数**就是**状态值

```java
// com.dragon.read.component.shortvideo.impl.fullscreen.i
public final void sd(boolean z19, boolean z29) {
    f fVar = this.F3;
    if (fVar != null && fVar.f158491o != null) return;   // 锁屏中：整条链忽略
    nk4.a aVar = this.f279258i;
    if (aVar != null) aVar.w0(z19, z29);                 // 另一套（工具栏容器）
    if (this.f357240d) t4(!z19, z29);                    // 横屏才做
    if (this.f357240d && fVar != null) {
        if (fVar.i()) {                                  // i() = 是否锁屏
            fVar.m(false);  fVar.n(z29);                 // 锁屏分支
        } else {
            boolean z59 = !z19;
            fVar.m(z59);                                 // ← 写状态 = !z19
            fVar.q(z59, z29);                            // ← 按状态显示/隐藏各控件
        }
    }
}
```

⇒ **`sd(z,·)` 里的 `z` 是"要隐藏"的语义，宿主直接把它取反后写进自己的状态字段 `f158498v`。**
**所以改 `sd` 的入参 = 篡改宿主的持久状态，而不是"发一条命令"。** ← 全文最重要的一句话。

### 1.3 自动隐藏 = 一次性 5000ms 计时器，**在"显示"动作里顺带 arm**

```java
// f
public final void n(boolean z19) {           // 「显示 + 排自动隐藏」
    c();                                     //   c() = B.removeCallbacks(C)
    this.f158484h.g(z19);
    this.f158484h.e(true, z19);              //   显示控件层
    c();
    this.B.postDelayed(this.C, 5000L);       //   ← arm 5s
}
public final void c() { this.B.removeCallbacks(this.C); }

// 构造里：5s 之后执行的唯一动作
this.C = new Runnable() {                    // dr4.h0
    public final void run() { this.f276905a.f158484h.e(false, true); }   // 隐藏控件层
};
```

锁屏按钮另有一套，同样 5s：

```java
// com.dragon.read.component.shortvideo.impl.fullscreen.i 构造
this.L3 = new Runnable() {                   // dr4.c1
    public final void run() { K3.a(true); }  // 只做一件事：隐藏锁屏按钮
};
// K3 = pr4.d（锁屏按钮视图，FrameLayout + Lottie 锁动画）
//   pr4.d.a(anim) = 隐藏（g.a(view,true) 淡出）
//   pr4.d.b(anim) = 显示（g.a(view,false) 淡入）
```

⇒ **宿主没有"常驻看护"。它靠"谁显示谁负责 arm 一个 5s"。**
**推论（决定性）：任何让"显示"这个动作被改道或被吞掉的干预，都会连带让 5s 计时器不被 arm → 控件永久残留。**

**全 dex 已穷举 arm 点（grep `postDelayed` 于 `f`/`i` 两个类）：**

| 类 | 行 | 动作 | 何时 arm |
|---|---|---|---|
| `f` | 303 | `B.postDelayed(C, 5000)` | `n(true)` 内（紧随 `e(true,·)` 显示之后） |
| `i` | 871 | `f.B.postDelayed(f.C, 5000)` | `b4()` 的**已锁屏且容器可见**分支（`e(true,true)` 显示 + arm） |
| `i` | 889 | `G3.postDelayed(L3, 5000)` | `b4()` 已锁屏但 `K3==null` 时（显示锁屏按钮 + arm） |
| `i` | 959 | `G3.postDelayed(L3, 5000)` | **锁屏按钮自己的点击回调**里（`ChangeLockStatusReason.CLICK_LOCK_VIEW`，锁上后 arm） |

⚠ **关键修正**：`d4()` 走的 `q()`（第 390 行起）**完全没有** `postDelayed`；`sd()` 走的
`m()+q()` 同样不含。⇒ **`sd` 与"未锁屏点按"这两条主路径本身都不 arm。**
而且上表 4 处 arm **有 3 处集中在"已锁屏 / 锁屏按钮"语义里**。所以宿主"点一下屏幕、5 秒后自己收"
的那个计时器，只可能来自 §1.2～1.4 之外的第三处（`yk4.a` 实现内部，或工具栏容器 `nk4.a.l()`）。
**这一点与"模块污染 → 永久残留"的结论不冲突，反而更严重**：既然 arm 与显示动作**不在同一个函数**里、
且只在特定状态下发生，模块任何一处"改入参/吞调用"都更容易把 arm 与显示**劈开**，
而且**我们无法从模块侧推断这一次点击到底会不会 arm** —— 这正是 §1.4 第 2 点说的那件事。

### 1.4 点按的唯一处理链

```java
// i 内部 GestureDetector
public final boolean onSingleTapConfirmed(MotionEvent e) {
    if (this.f158539a.i3())      return super.onSingleTapConfirmed(e);  // 特殊态不处理
    if (this.f158539a.b4())      return super.onSingleTapConfirmed(e);  // ① 已处理 → 返回
    this.f158539a.d4();                                                // ② 否则走这里
    ...
}

public final boolean b4() {
    if (!h.a.a().f158510a) return false;      // ← h = FullScreenLockManager
                                              //    f158510a = 是否已锁屏
                                              //    未锁屏 ⇒ 返回 false ⇒ 走 d4()
    ... 只处理「锁屏按钮」显隐（含 G3.postDelayed(L3, 5000)）...
    return true;
}

public final void d4() {
    f fVar = this.F3;
    if (fVar != null) {
        if (!fVar.f158484h.j()) {              // ← 容器查询 j()：false 才做 toggle
            z19 = false;
        } else {
            fVar.f158492p = Boolean.valueOf(areEqual(fVar.f158491o, TRUE));
            fVar.f158484h.t(true);             //   true ⇒ 只做这一件事，直接 return
            z19 = true;
        }
        if (!z19) {
            boolean z29 = !fVar.f158498v;      // ★ 用「宿主自己的状态」取反做 toggle
            fVar.m(z29);
            nk4.a aVarInvoke = fVar.f158485i.invoke();
            if (aVarInvoke != null) aVarInvoke.l();
            fVar.q(z29, true);                 //   注意：调的是 q()，不是 n()
            return;                            //   ⇒ 这条路径不 arm 5s
        }
        return;
    }
    ... F3 == null 分支：K3.a(true)/K3.b(true)  ——同样不含 postDelayed(L3)...
}
```

（`h` 已反编译确认：`public boolean f158510a` + `LogWrapper.info("FullScreenLockManager", "current status is ...")`。）

⇒ **两个要害：**
1. **未锁屏时点按走 `d4()`，方向完全由 `f158498v` 决定。** 模块一旦让状态与实际分叉，用户下一次点按就**反向**——"该收的反而显示、该显示的没反应"。
2. `b4()` 的 5s arm 只在**已锁屏**分支里；`d4()` 完全不含 arm。**模块无法预测"这次点按到底有没有 arm 计时器"**——因为那取决于宿主内部状态，而那个状态正被模块改写。

### 1.5 控件层的统一入口（模块**本该**用的那个）

```java
// yk4.a（接口）= 全屏控件层容器
void e(boolean visible, boolean animate);                        // 顶部/底部+控件层总开关
void s(FullScreenSideControl c, boolean visible, boolean animate); // 分控件（BRIGHTNESS / VOLUME）
boolean m(FullScreenSideControl c);                              // 查可见性
```

`sd → q → yk4.a.e(z39,z29)` + `yk4.a.s(BRIGHTNESS, …)` + `yk4.a.s(VOLUME, …)`：

```java
public final void q(boolean z19, boolean z29) {
    ...
    this.f158484h.e(z39, z29);
    this.f158484h.s(FullScreenSideControl.BRIGHTNESS, z49, z29);
    this.f158484h.s(FullScreenSideControl.VOLUME, z59, z29);
}
```

⇒ **亮度/音量有统一入口；锁屏按钮 `pr4.d` 没有**（它是 `g4()` 里 `addView` 的裸视图 + 私有 `a()/b()`）。
**这就是为什么"三按钮"里锁屏按钮最顽固：模块对它连一个合法入口都没有，只能事后强改。**

---

## 2. 模块当前的干预面（8 类，逐一对照）

| # | 干预 | 位置(Hooks.kt) | 本质 | 对宿主状态机 |
|---|---|---|---|---|
| 1 | 改 `sd` 入参 `false→true` | `shortNativeClean` 5523 | **改宿主状态字段的赋值** | **污染**：宿主状态与实际都变了，但"这次是显示"的语义被换成"隐藏"，连带丢 arm |
| 2 | 改 mask 入参 | `shortMask` 5550 / `cnHomeMask` 5574 | 同上 | 污染 |
| 3 | **吞掉**工具栏显示调用（返回 `null`） | `pb` 5704 / `pt` 5708 / `ctS` 5712 / `gt7c` 5717 | 让宿主"显示了但没显示" | **污染**：宿主无异常、仍以为显示成功 |
| 4 | 改 `shortLandscapeMethod` 入参 | `fb` 5792 | 改宿主状态 | 污染 |
| 5 | 全局拦 `View.setVisibility(VISIBLE)` | `sv` 5918 | 直接改地真 | 与宿主内部状态分叉；**且是全 App 最热路径** |
| 6 | 递归扫描 + 静态 ID 强隐藏 + 记忆/恢复 | `scanTreeUnified` 2516 / `rememberViewState` 1463 | 第三套状态 | 分叉（宿主再改回来就打架） |
| 7 | 各种"补收"（retract / 12s 窗口 / 1s 看护 / pause 恢复） | 本轮已删 | 第四套状态 | 与 1–3 的污染结果互相追尾 |
| 8 | 模块程序化显示后再自己收 | `restoreAllControls` 1679 + `hideShortVideoNativeControls` 2081 | 入口合法、**收尾非法** | 半合法 |

**共同点：除第 8 条的"显示"那一半，全部是"绕过宿主入口、直接改结果"。**

---

## 3. 第一性结论

### 3.1 症状 → 机制（一条根因导出全部）

| 用户看到的现象 | 机制 |
|---|---|
| 点空白处唤出控件后**永久不收** | "显示"动作被改道成"隐藏" → `n()`/`d4()` 里的 5s arm 没发生 → 无人收回 |
| 三按钮**只残留一部分**（锁屏/亮度/音量不同步） | `sd→m/q` 只覆盖亮度/音量（且还有 `f357240d`、`F3!=null`、`brightnessVolumeButton`、`f158495s` 四重门闸）；**锁屏按钮根本不在链上** → 一半隐藏、一半显示 |
| **点了没反应 / 点了方向反** | `d4()` 用 `!f158498v` 取反；状态被改写后 toggle 反向 |
| 切集后行为变化 | `F3`/holder 重建，状态字段与视图的错位量随次数累积 |

### 3.2 为什么前三轮修不好

三轮（对称 sd 补偿 → pending+4s → 全量 holder+12s）以及 1s 看护，**全部作用于"结果"**（把已经显示的控件收回去）。
而"结果为什么会出现"发生在**入口**（宿主被要求执行"隐藏"）。**在结果上打补丁，无法修好入口的语义错位**——
所以表现为"每次都修好一个场景、用户换个操作又复现"。

### 3.3 唯一正确的原则

> **P1 单一权威**：全屏控件显隐由宿主唯一拥有。
> **P2 写入必须走宿主入口**：要隐藏就调 `sd(true, z2)`（宿主会同步 `f158498v`，之后自洽）；
> **绝不**改别人的入参、**绝不**吞掉别人的调用、**绝不**直接改子视图可见性。
> **P3 只删元素、不夺状态**：对"永久删掉某个 UI 元素"（互动容器、工具栏图标、系列信息面板…）
> 这类宿主**不会再去动**的元素，用「inflate/onBind 时一次性 GONE + 打 tag」，**不要**做成
> "每次 setVisibility 都拦"或"周期扫描"——它们不参与显隐状态机，冲突面为零。
> **P4 "点击不唤出"要在输入层解决**：吞掉那一击，宿主状态机**从未进入**显示，无需回滚。

---

## 4. 修复方案

### 4.1 方案 A（推荐）：把"沉浸"完全交还宿主

1. **撤掉 1–4 全部"改入参/吞调用"**
   - `shortNativeClean` 只保留**观察**（做事件源），不再改写；
   - 删 `shortMask`/`cnHomeMask` 的改写与强置 INVISIBLE；
   - 删 `pb`/`pt`/`ctS`/`gt7c` 的 `null`（工具栏层交还宿主）；
   - 删 `fb` 的入参改写。
   - **效果**：宿主"显示"动作恢复完整 → 5s arm 恢复 → 残留自动消失；`f158498v` 不再被篡改 → 点按方向恢复正确。
2. **撤掉 5（`sv` 全局 setVisibility 拦截）**。若确需消除切集闪现，改为按 tag 对**指定元素**一次性 GONE（P3）。
3. **第 6 类降级为"元素级一次性删除"**：只在 inflate/onBind 后对白名单 ID 做一次 GONE + tag；
   删掉周期扫描、记忆/恢复、每次拦截。
4. **第 7 类保持删除**，任何形式都不复辟（包括"看护"、"自愈窗口"、"retract"）。
5. **`restore_controls_pause`（暂停恢复控件）改为走宿主入口**：
   暂停时 `sd(false, z2)`，恢复时 `sd(true, z2)`。**不要自己改可见性再自己补收。**
   —— 走入口则宿主状态与视图始终一致，**根本不需要任何补偿**。
6. **锁屏按钮：不要碰。** 撤掉针对它的一切强制/补偿后，它回到原生行为（点按显示 → 5s 自动收）。

**代价**：播放态点一下屏幕会显示控件、5s 后自动收（= 原生行为）。
**收益**：不再有任何补偿逻辑；代码量下降；症状归零。

### 4.2 方案 B（若"播放中必须保持沉浸、点击不唤出"是硬需求）

只能用 **P4（输入层）** 实现，这是唯一不产生状态分叉的位置：

```
在横屏全屏页 + 播放态下，拦掉"单击"事件本身
（onSingleTapConfirmed 之前 / dispatchTouchEvent 层）
⇒ 宿主从未收到"显示"输入 ⇒ 状态机不动 ⇒ 无需计时器 ⇒ 无残留
```

必须**放行**：双击（暂停/点赞）、长按、拖进度条、上下滑切集、侧边手势。
这比"让宿主显示再压回去"简单得多，且不与任何宿主状态交互。

### 4.3 不建议的中间态

- ❌ 保留入参改写 + 加更聪明的补偿（= 前四轮）；
- ❌ 用 `sd(true,true)` 反复"纠偏"（会与宿主的 arm/取消互相踩）；
- ❌ 在 `View.setVisibility` 上做条件过滤（热路径 + 分叉）；
- ❌ 直接改锁屏按钮 `pr4.d` 的可见性（宿主 5s 计时器不知道，必然打架）。

---

## 5. 验证判据（比像素硬）

| # | 判据 | 做法 | 通过标准 |
|---|---|---|---|
| V1 | **宿主调用序列不变性** | A/B 记录宿主 `sd`/`b4`/`d4` 的调用序列（参数+时刻） | **开模块后序列与原生一致** ← 最硬的一条 |
| V2 | 状态一致性 | 临时探针读 `f.f158498v`，与 `K3.getCurrentViewVisible()`、`yk4.a.m(BRIGHTNESS)` 比对 | 三者**始终**一致 |
| V3 | 三按钮 | **视图树 `visibility`**（不是像素占比） | 点按后 ≤6s 转 GONE |
| V4 | 交互方向 | 连点两次空白处 | 显示→隐藏→显示，方向不反 |

> 像素判据在本项目已多次假阳性（红色车身、亮窗、白字都会抬高"近白占比"）。**视图树是地真。**

---

## 6. 本次分析的边界（诚实标注）

**已确证（有反编译出处）**：
- `sd` 的参数 = 宿主状态字段 `f158498v` 的赋值（§1.2，`m(!z19)`）；
- 自动隐藏是确定位置的 **5000ms 一次性计时器**：`f.n()` 与 `b4()` 的已锁屏分支内 arm，
  锁屏按钮另有 `i.L3` 两处 arm（§1.3 表：`f:303` / `i:871` / `i:889` / `i:959`，全 dex 穷举）；
- **`sd()`/`d4()`/`q()` 三条路径均不 arm** —— arm 与显示动作不在同一函数（§1.3）；
- **锁屏按钮不在 `yk4.a` 统一入口里**、无对外方法（§1.5，`pr4.d` 只有 `a()/b()`）；
- 点按链 `i3()→b4()→d4()`，`b4()` 以"已锁屏"为条件，`d4()` 以 `f158484h.j()` 为门闸、
  用 `!f158498v` 取反且不含 arm（§1.4）；
- `fullscreen.h` = `FullScreenLockManager`，`f158510a` = 是否锁屏（§1.4）。

**尚未逐行确认（不影响修复方向）**：
- `yk4.a.j()` 的确切语义（`d4()` 的唯一前置门闸；疑似"锁屏按钮相关状态"，未反编译到实现类）；
- **未锁屏点按路径里 5s 计时器最终由谁 arm**（已穷举 `f`/`i` 两个类的全部 `postDelayed`：
  `d4()`/`q()`/`sd()` 都不 arm，故只能落在 `yk4.a` 实现内部或 `nk4.a.l()`；
  实现类由 `l2(Context)` 工厂返回，未继续追）。

> 无论上述哪条成立，结论不变：**模块"绕过入口改结果"必然产生状态分叉**。
> 先用 **V1（调用序列不变性）** 验证一次，即可当场判定方案 A 是否成立。
> —— 顺带：V1 也能**顺手把上面这两条未确认项一次性解掉**（把宿主的 `m/q/n/e` 调用序列录下来，
> 谁在什么时候 arm 就一目了然，比继续读反编译快）。

---

## 7. 方案 A 已落地（2026-09-14 20:20–20:40）

代码改动集中在 `Hooks.kt`：**净 −347 行**（+59 / −406），无新增 hook（反而少了 1 个反射 hook）。

### 7.1 撤掉了什么

| 类别 | 具体 | 说明 |
|---|---|---|
| 改宿主状态入参 | `sd(false,*) → sd(true,*)`（`shortNativeClean`） | **改造成"只观察不改写"的 V1 探针**（唯一保留者） |
| 改宿主状态入参 | `shortHolder.p4`（`fb`）入参 true→false | 整个 hook 删除 |
| 旁路强改可见性 | 清屏遮罩强制 INVISIBLE（`shortMask` / `cnHomeMask` / remember+restore+sync 共 5 函数） | 整套删除 |
| 吞掉宿主调用 | `pb` / `pt` / `ctS` / `gt7c` 返回 `null` 阻止工具栏显示 | 降级为"只登记、不拦截" |
| 旁路改工具栏 | `setVideoToolbarsVisible` / `setOneVideoToolbarVisible` / `setToolbarBaseVisible` / `registerToolbarBaseLayer` + 全部 6 处调用 | 整套删除（含 `toolbarBaseCtor` 与 `gt7c` 两个反射 hook） |
| **模块程序化显示** | `setOneShortVideoControlsVisible` / `restoreShortVideoNativeControls`（暂停恢复、关开关时调 `sd(false,*)` 显示控制层） | 整套删除 —— **见 §7.2，这条本身就是残留源** |
| 事后补收 | `armNativeControlsRetract` / `scheduleNativeRetractTick` / `hideShortVideoNativeControls` / `tryHideShortVideoNativeControls` / `nativeSideControlsVisible` / pause 补收 | 整套删除（1s 轮询、12s 窗口、4s 静默等历史方案一并清零） |
| `sv` 全局拦截 | 其中"清屏遮罩"分支 | 删除（首页/feed 分支保留，不在本次范围） |

**保留**：首页/feed 的界面精简（`scanTreeUnified` / `blindView` / `quickMatch` 等）、
广告拦截、画质倍速、下载限制、设置页入口等全部不动。

### 7.2 ★ 落地时才发现的第二个残留源：**模块程序化显示**

原以为"只撤掉改写、保留显示侧"就够，真机立刻否掉了这个判断：

```
[20:30:08.090] V1 sd(true,true)      ← 进全屏时宿主自己隐藏控制层（正确）
[20:30:08.156] V1 sd(false,true)     ← 模块「暂停恢复」程序化显示（66ms 后）
→ +7s 截图：锁屏 / 亮度 / 音量 三个按钮仍在屏幕上（v3 与 v4 字节完全一致）
```

**原因**：宿主**不会替"程序化显示"arm 自己的 5s 自动隐藏计时器**（§1.3：arm 只在
"显示"动作内发生，而 `sd → m()+q()` 这条路径根本不含 arm）。所以"显示侧"同样是一处
**没有下家的写入** —— 只要模块主动显示，就一定得自己再收，于是又回到补偿循环。
⇒ **结论升级：交还要交两半。**「不改别人的入参」还不够，**「不替宿主显示」同样必须**。
（这条与 §1.4 的"未锁屏点按 5s 由谁 arm"未确认项无关 —— 那是宿主自己的路径。）

### 7.3 真机验证（OnePlus PLK110 / 7.3.7.32 / 装机 20:32）

| # | 判据 | 做法 | 结果 |
|---|---|---|---|
| V1 | **调用序列** | `shortNativeClean` 探针录宿主 `sd` 序列 | 全部为 `(true,true)` / `(false,true)` **原样成对**，模块零改写、零新增调用 ✓ |
| V3 | 控制层 ≤6s 收回 | 进全屏 +7s / 点空白 +7s 截图 | 进全屏 +7s：三按钮区域 **0.00%** ✓；点空白显示后 +7s 亦回到 0.00% ✓ |
| V4 | 连点方向不反 | 同一空白点连点两次 | 第 1 次显示 → 第 2 次隐藏 ✓（截图裁图确认，非像素占比） |
| — | **原 bug 复现** | 点空白 → 暂停 → 播放 → 点空白，+1.5s / +7s 截图 | 两帧均无按钮、且帧间一致（无永久残留）✓ |

> 像素仍按老规矩只当筛查：本轮 volume 区域出现过 19.95% / 21.53% 的**假阳性**，
> 裁图确认是视频画面里的亮色块；lock 区域同样出现过 4.55%~10.33% 的假阳性。
> **判"有没有按钮"最终一律以裁图肉眼 + 必要时视图树为准。**

### 7.4 这次交还带来的行为变化（用户可见）

- 播放中**点一下屏幕会浮出控制层**（顶部/底部栏 + 侧边三按钮），**5 秒后由宿主自动收回** —— 即原生行为；
  模块不再"点了什么都不出来"。
- 「隐藏控件」「选集相关功能」两个开关**在播放页不再改写工具栏/控制层显隐**；
  它们仍生效的部分：`scanTreeUnified` 对**选集条**等元素的隐藏、以及首页/feed 的精简。
- 「暂停后恢复所有控件」开关仍保留（它会恢复模块在首页/feed 隐藏过的元素），
  但**不再有"恢复控制层"这个动作**（模块已不隐藏它）。


---

## 附：一句话版本

宿主把"谁可见"记在自己的 `f158498v` 里，并**在每次"显示"时顺手排一个 5 秒计时器**；
模块把 `sd(false)` 改成 `sd(true)`，等于**替宿主写下"不可见"**——于是"显示"没发生、5 秒计时器没排上，
而锁屏按钮等不在同一条链上的控件照旧显示 → **一半隐藏、一半显示，且再也不会自动收**。
**修法只有一个：要隐藏就调 `sd(true,·)`，别改别人的入参。**

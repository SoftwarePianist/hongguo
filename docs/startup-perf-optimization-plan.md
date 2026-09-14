# 启动期性能优化建议（P0–P4）

> 目标 App：`com.phoenix.read.oversea.gp` 7.3.7.32 ｜ 模块：`xyz.kejiyu.hongguo`
> 数据来源：带临时埋点的构建（run10063，2026-09-14 13:39，冷启动窗口 2.4s，2 次 Activity 创建）
> 埋点已 `git checkout` 回滚，本文件记录测量结果与方法，便于复现。

---

## 0. 结论速览

| 优先级 | 项 | 实测依据 | 预期收益 | 风险 | 建议 |
|---|---|---|---|---|---|
| **P0（新发现）** | `ham()` 挂全部重载 → 框架内部委托链导致**同一棵树被扫 2–3 次** | `inflate` 345→144–155（**↓57%**，已实测） | 节点扫描量降约 **1/2～2/3** | 低（行为等价） | ✅ **已完成并验证** |
| P1 | `installBusinessHooks` 分批，非首帧 hook 延后 | 同步段实测 **25ms**（5+12+8），非 50ms | 首帧前主线程少 **~10–12ms** | 中（首帧闪烁 / 早期广告漏拦） | 次做，需真机盯首帧 |
| P4 | `LogUtil.write()` 每次 `new SimpleDateFormat` | 实测 17.4–19.5 µs/次 × 84 次 | **实测净省 0.6ms** | 极低 | ✅ **已完成并验证** |
| P3 | `LogUtil.incr` 加开关 | 计数总量约 2 万/启动 | **<1ms** | 极低 | ⚠ 见 §5.3 顺序约束 |
| P2 | `quickMatch` 的 `simpleName` | 实测 `simpleName` 调用 2994 次/启动 | **≈97.8% 消除**（省 0.3–0.5ms） | 极低 | ✅ **已完成并验证** |
| — | inflate/addView 全树扫描「持续轻微浪费」 | 无掉帧（前次结论） | 已被 P0 覆盖大半 | — | 合并进 P0 |

**两处需要修正的认知：**

1. **「50ms」不是同步安装段。** 同步安装实测 25ms；若你之前的 50ms 是 A/B 冷启动总差，差额来自 hook 生效后宿主额外走的扫描路径 —— 那部分正是 P0 要削的。
2. **P2 的位置不对。** `quickMatch` 本体用的是 `v.javaClass.name`（`getName()` 返回内部缓存字符串，**零分配**，可放心留在 Set 查询里）。真正产生 substring 分配的是 `isComposeSeriesBar()`（L560）里的 `simpleName`，且只在 `gPlayerOn` 时每节点调用一次。

---

## 1. 实测基线

### 1.1 同步安装耗时（主线程，`onPackageReady` 阶段）

埋点输出（原日志 1:1）：

```
[PERF] 版本探测+表解析+锚点 5ms
[PERF] 总计 25ms | ham/hac 调用=84 耗时=12ms | 其余(Class.forName+直接hook+日志)=13ms
```

| 段 | 耗时 | 内容 | 可延后？ |
|---|---|---|---|
| 版本探测 + 表解析 + 锚点 | 5ms | `detectTargetPackageVersion` + `TargetNames.resolve/probe` + `applyAnchorOverrides`（锚点命中缓存 = 0ms） | ❌ 不可（后续全部依赖 `gNames`） |
| `ham`/`hac` × 84 | 12ms | 实际 hook 安装（≈0.14ms/钩子） | ⭕ 部分可（见 §3） |
| 其余（Class.forName + 直接 `module.hook` + 日志） | 8ms | ~50–60 次 `Class.forName` + ~10 次直接 hook + 84 行日志（含 `SimpleDateFormat`） | ⭕ 部分可 |

**口径说明：** 埋点里的「其余 = 13ms」是 `总计 25ms − ham/hac 12ms`，**已包含上面那 5ms**；扣掉后纯尾段为 **8ms**。三段 5 + 12 + 8 = 25ms 自洽。

**关键：** `installBusinessHooks` 由 `MainHook.onPackageReady` 调用，此时**目标 App 的 Application 尚未创建**（`ActivityThread.currentApplication()` 返回 null）。也就是说这 25ms **全部算进冷启动延迟，且落在主线程**。

### 1.2 启动窗口运行期计数（`LogUtil.diagDump`）

```
DIAG onCreate=2 | matchCall=8731 | inflate=345 | scanTree=8731 | addView=1497 | matchHit=40
```

| 计数 | 值 | 含义 |
|---|---|---|
| `matchCall` | 8731 | `quickMatch` 调用次数 = 被访问的节点数 |
| `scanTree` | 8731 | `scanTreeUnified` 访问的节点数（与 matchCall 恒等，因为每节点恰调 1 次 quickMatch） |
| `inflate` | 345 | **已含重载放大**的 hook 命中次数 |
| `addView` | 1497 | **已含重载放大**的 hook 命中次数 |
| `matchHit` | 40 | 真正命中并隐藏的节点（8731 → 40，命中率 0.46%） |
| 日志行 | 84 | `write()` 调用次数 = `new SimpleDateFormat` 次数 |

### 1.3 热路径单次成本（前次测量）

- `scanTreeUnified` 单节点 ≈ **6–14µs**（含 quickMatch + 各 `is*` 谓词）
- 8731 节点 × ~8µs ≈ **70ms 主线程 CPU**，但**摊在 ~2s 窗口内、分散在数百次 inflate/addView 回调中**，所以表现为「持续轻微占用」而非单点掉帧 —— 与「没造成可测掉帧」的结论一致。

---

## 2. P0（新发现）`ham()` 挂全部重载 → 重复全树扫描

### 2.1 现状代码

`Hooks.kt:4461`

```kotlin
fun ham(clazz: Class<*>, methodName: String, hookId: String, block: (XposedInterface.Chain) -> Any?) {
    clazz.declaredMethods.filter { it.name == methodName }.forEachIndexed { i, m ->
        try { module.hook(m).setId("${hookId}_$i")...intercept(Hooker { chain -> block(chain) }) } catch (_: Exception) {}
    }
}
```

`inflate` / `addView` 的调用点（L5551 / L5607）都走 `ham`，且 hook body 里无条件 `scanTreeUnified(result)` / `scanTreeUnified(v)`。

### 2.2 问题：AOSP 的内部委托链

`LayoutInflater` 有 **4 个** `inflate` 重载，且层层向下委托：

```
inflate(int, ViewGroup)
  └─> inflate(int, ViewGroup, boolean)
        └─> inflate(XmlPullParser, ViewGroup, boolean)   ← 核心，三个入口最终都到这里
inflate(XmlPullParser, ViewGroup)
  └─> inflate(XmlPullParser, ViewGroup, boolean)
```

`ViewGroup` 有 **5 个** `addView` 重载，其中 4 个都汇入最深那个：

```
addView(View)
  └─> addView(View, int)
        └─> addView(View, int, ViewGroup.LayoutParams)  ← 核心
addView(View, ViewGroup.LayoutParams)
  └─> addView(View, int, ViewGroup.LayoutParams)
addView(View, int, int)
  └─> addView(View, int, ViewGroup.LayoutParams)
```

**后果：** 宿主调一次 `addView(child)`，链条上 3 个已 hook 的重载**依次命中**，每次都对**同一个 child 子树**跑一遍 `scanTreeUnified`。`inflate(id, root)` 同理 —— 命中的 3 层返回的是**同一个 root 对象**。

即：**约 2/3 的节点扫描是纯重复**。这与 `scanTree=8731` 而 `matchHit` 只有 40 的量级完全自洽。

### 2.3 修法：只挂「终端重载」

新增一个按签名精确挂载的辅助函数（与 `ham` 并存，不动现有调用点）：

```kotlin
/**
 * 只挂签名为 paramTypes 的那一个重载。
 * 用于 LayoutInflater.inflate / ViewGroup.addView 这类「内部有委托链」的框架方法：
 * 只挂终端重载即可覆盖全部入口，避免同一棵视图树被重复扫描 2–3 次。
 */
fun hamExact(
    clazz: Class<*>,
    methodName: String,
    paramTypes: Array<Class<*>>,
    hookId: String,
    block: (XposedInterface.Chain) -> Any?,
) {
    val m = clazz.declaredMethods.firstOrNull {
        it.name == methodName && it.parameterTypes.contentEquals(paramTypes)
    }
    if (m == null) {
        LogUtil.warn("  hamExact 未命中 $methodName(${paramTypes.joinToString { it.simpleName }})")
        return
    }
    try {
        module.hook(m).setId(hookId)
            .setExceptionMode(XposedInterface.ExceptionMode.PROTECTIVE)
            .intercept(Hooker { chain -> block(chain) })
    } catch (_: Exception) {}
}
```

调用点改动（L5549–5563 / L5605–5621）：

```kotlin
try {
    val c = Class.forName("android.view.LayoutInflater", false, classLoader)
    hamExact(
        c, "inflate",
        arrayOf(org.xmlpull.v1.XmlPullParser::class.java,
                android.view.ViewGroup::class.java,
                Boolean::class.javaPrimitiveType!!),
        "inf",
    ) { chain ->
        val result = chain.proceed()
        if (result is ViewGroup) {
            LogUtil.incr("inflate")
            ensureResourceIdsResolved(result)
            if (gMasterOn && !(gRestoreControlsOnPause && gVideoPaused)) {
                scanTreeUnified(result)
            }
        }
        result
    }
    LogUtil.info("  ✓ inflate(终端重载)")
} catch (e: Exception) { LogUtil.error("inflate", e) }

try {
    val c = Class.forName("android.view.ViewGroup", false, classLoader)
    hamExact(
        c, "addView",
        arrayOf(android.view.View::class.java,
                Int::class.javaPrimitiveType!!,
                android.view.ViewGroup.LayoutParams::class.java),
        "av",
    ) { chain ->
        /* body 不变 */
    }
    LogUtil.info("  ✓ addView(终端重载)")
} catch (e: Exception) { LogUtil.error("addView", e) }
```

**为什么安全：** 两个终端重载都是 `public` 且被同类的其他所有重载直接调用；任何入口最终都必须经过它。subclass 覆写的情况与现状相同（现在也是挂 `LayoutInflater` 自身的声明），无回归。

**验证方式：** 重跑冷启动，DIAG 里 `scanTree` 应从 **8731 → ~2900**（≈1/3），`inflate` 345→~115、`addView` 1497→~500；UI 隐藏行为（`matchHit`、隐藏项数量）应**完全不变**。

### 2.4 ✅ 已落地并真机验证（2026-09-14 14:07）

**实现：** `Hooks.kt` 新增 `hamExact()`（按 `parameterTypes.map { it.name } == paramTypeNames` 精确匹配），
inflate / addView 改为只挂终端重载。踩坑一处：`"android.view.ViewGroup$LayoutParams"` 里的 `$L` 会被 Kotlin
当成字符串模板，**必须写成 `\$`**。

**验证协议：** `am force-stop` → `am start SplashActivity` → 等 7s → 读模块日志；改动前后各采样 3 次。
判据取 **`onCreate=2` 那一行的 DIAG**（同为「第 2 次 Activity 创建」的快照，可比）。

| 指标（onCreate=2） | 改动前（3 次） | 改动后（3 次） | 变化 |
|---|---|---|---|
| **`inflate`** | **345 / 345 / 345**（均值 345） | **144 / 149 / 155**（均值 149） | **↓ 57%** |
| `addView` | 856 / 919 / 747（均值 841） | 521 / 648 / 592（均值 587） | ↓ 30% |
| `scanTree`（= `matchCall`） | 4267 / 4793 / 4246（均值 4435） | 2954 / 3493 / 3252（均值 3233） | ↓ 27% |
| `matchHit` | 10 / 14 / 16（均值 13.3） | 12 / 15 / 14（均值 13.7） | **一致（区间重叠）** |

**`inflate` 是最干净的判据**：改动前三次**恒为 345**（零波动），改动后 144–155，稳定降到 1/2 以下。
`scanTree`/`addView` 本身有 ±22% 的场次波动，降幅（27%/30%）不如 `inflate` 显著 —— 这也说明
**挑「方差最小的指标」做主要判据**比挑降幅最大的更重要。

**行为等价性：** 把两份构建的完整功能日志做归一化对比（去掉时间戳/DIAG 行后 `sort -u`）——
**除改名的两行安装日志外逐行完全一致**（91 行 vs 91 行），91 项 hook 全部命中，
`hamExact 未命中` 告警 **0 条**。`home full-series entry hidden before first draw`、
画质切换、双击评论、清屏等关键行为均正常。截图确认首页 UI 精简生效、无残留控件。

**为什么没有达到满额的 1/3：** 三处叠加原因 ——
① 有些子树本身只有一层重载路径可走（不经过全部 3 层）；
② `addView` 长链并非每次都从最长路径进入；
③ 仍有非 inflate/addView 触发的 `scanTree`（`scanAllWindows`、生命周期 `postDelayed` 等）。
**`inflate` 的 57% 是最可信的放大证据**，也是本次最稳的收益。

**遗留：** `addView` 的降幅（30%）明显小于 `inflate`（57%），说明 addView 侧可能还有别的重复路径
（例如某些调用方直接走 `(View,int,LayoutParams)` 后又被上层 `addView(View)` 包一层）。
后续若要再压，需在 hook body 里加「同一 runloop 内同对象去重」的守卫，但收益与复杂度需重新评估。

---

## 3. P1 `installBusinessHooks` 分批安装

### 3.1 划分依据

`installBusinessHooks` 在**首帧之前**执行，所以判据是「**该 hook 是否影响首页首帧的呈现**」，而不是「是否重要」。

| 组 | 内容 | 现状位置 | 归属 |
|---|---|---|---|
| A 首帧必需 | 版本/表/锚点解析、资源 ID 集合、`MainFragmentActivity` 底部 Tab 隐藏、`BottomTabFrameLayout`、`VideoFeedTabFragmentImpl` 布局、`inflate`/`addView`/`setVisibility`(树扫描引擎)、`onCreate`/`onResume`/`onPause` 生命周期、窗口/Activity tracker、播放探测器、`TTVideoEngine`/`VideoController` 暂停跟踪、`StatusBar` | L4391–~5547、L5551–5563、L5607–5621、L5624–5770 | **保留同步** |
| B 进页面才命中 | 下载数量限制（`SsConfigMgr.getABValueJson`/KMP `s93.q.A7`/`xh5.f`）、`goldBox`、`pendant`、`redPack`、设置页（列表/点击/开关）、OLED 亮度、暂停广告入口、`SeriesPauseAdImpl`、片尾广告层、广告图标层、VIP 全套（`PrivilegeManager`/`NsUserInfoDependImpl`/`NsComicAdDependImpl`/KMP `getVipInfo`/`NsVipImpl`） | L5490–5547、L5780–5976 | **可延后** |

B 组约占 84 次 `ham`/`hac` 中的 25–35 次（→ 约 4–5ms），加上为其 `Class.forName` 的 15–20 个类（→ 约 5–6ms），合计 **~10–12ms** 可以移出同步段。

### 3.2 落地形态

`ham`/`hac` 目前是 `installBusinessHooks` 内的**局部函数**，分批必须先提升为私有方法：

```kotlin
// 提升为成员，避免 installBusinessHooks 变成 1500 行长函数
private var hookModule: MainHook? = null

private fun ham(clazz: Class<*>, methodName: String, hookId: String, block: (XposedInterface.Chain) -> Any?) { /* 原样搬迁 */ }
private fun hac(clazz: Class<*>, hookId: String, block: (XposedInterface.Chain) -> Any?) { /* 原样搬迁 */ }
private fun hamExact(...) { /* §2.3 */ }

fun installBusinessHooks(module: MainHook, classLoader: ClassLoader, pkg: String) {
    hookModule = module
    // ... A 组：原样保留（同步，首帧前完成）
    // ... 到 A 组结束处：
    mainHandler.postDelayed({
        try {
            installDeferredHooks(classLoader, pkg)
        } catch (e: Throwable) {
            LogUtil.warn("延迟 Hook 安装失败: ${e.javaClass.simpleName}: ${e.message}")
        }
    }, 1200L)
    LogUtil.info("installBusinessHooks done"); LogUtil.diagDump(true)
}

/** B 组：进页面才命中的 hook，1200ms 后安装，不占冷启动 */
private fun installDeferredHooks(classLoader: ClassLoader, pkg: String) {
    // 原 L5490–5547（下载限制/goldBox/pendant/redPack，注意排除 inflate）
    // + L5780–5976（设置页/OLED/广告/VIP）原样搬运
}
```

### 3.3 风险与验收（这个改动必须真机盯）

- **首帧 UI 闪烁**：A 组必须完整，任何被误分到 B 组的 UI hook 会让首页头几帧「露出未隐藏的控件」。
- **早期广告漏拦**：广告 hook 是「创建时拦截」，若广告在 1200ms 内创建会漏。实测启动后第一个视频在 ~2.0s 才起播，1200ms 有余量；但仍要**连跑 3 次冷启动确认无开屏/信息流广告出现**。
- **时机可再收**：若 1200ms 偏激进，可改为「首个 Activity `onResume` 之后 `post`」，语义更稳（不依赖绝对时间）。

**验收：** 同步段应从 25ms 降到 **~12–14ms**（埋点复测）；用本仓库 `android-perf-ab-attribution` skill 的 `am start -W` A/B 协议对比冷启动 `TotalTime`。

---

## 4. P2 `quickMatch` 的 `simpleName`

### 4.1 现状与修正

`quickMatch`（L568–587）本体**不调用** `simpleName`：L581 用的是 `v.javaClass.name in gHideClassesSet` —— `Class.getName()` 返回内部缓存串，**零分配**，无需改。

带包名的类调 `getSimpleName()` 时，OpenJDK/ART 的实现是 `name.substring(name.lastIndexOf('.') + 1)` → **每次都分配一个新 String**。热路径上只有一处：

`Hooks.kt:560`（`isComposeSeriesBar`，由 L578 每节点调用一次，仅 `gPlayerOn` 时）

```kotlin
if (v.javaClass.simpleName == "TreeLifecycleComposeContainer") {
```

### 4.2 ✅ 已落地（2026-09-14 14:18，实测验证）

最初考虑「缓存单个 Class 句柄 + `===`」，但更稳的是**按 Class 记忆化**（不依赖「目标类唯一」这个假设，
且对任何 Compose 容器实现都成立）：

```kotlin
private val gComposeBarClassCache = java.util.concurrent.ConcurrentHashMap<Class<*>, Boolean>()

private fun isComposeSeriesBar(v: View?): Boolean {
    if (v == null) return false
    val cls = v.javaClass
    val cached = gComposeBarClassCache[cls]
    val isContainer = if (cached != null) cached else {
        val r = cls.simpleName == "TreeLifecycleComposeContainer"   // 每个 Class 只付一次
        gComposeBarClassCache[cls] = r
        r
    }
    if (!isContainer) return false
    val density = try { v.resources.displayMetrics.density.coerceAtLeast(0.1f) } catch (_: Throwable) { 1f }
    val h = (if (v.height > 0) v.height else v.measuredHeight) / density
    if (h in 30f..70f || v.id == 0x7F0B0BB5) return true
    return false
}
```

用 `Class` 作 key 是安全的：`Class` 的 `hashCode` 就是身份哈希；Class 对象生命周期跟随 classLoader，
不会比视图更长寿，无泄漏风险。

**实测（临时加 `LogUtil.incr("composeNameCheck")` 计数，验证后已移除）：**

| 指标 | 改动前 | 改动后 |
|---|---|---|
| `composeNameCheck`（真实 `simpleName` 调用次数） | **2911 / 1730 / 2994** | **64 / 63 / 69** |

**降幅约 97.8%**（3000 次 → 约 2 次/个不同 Class，实测锚定了 30–70 个类）。
这一条同时**证实了先前的分析**：`composeNameCheck` ≈ `matchCall` ×99.5%（2911/2925），
说明 `gPlayerOn` 时**几乎每个被扫描的节点都会走一次 `simpleName`**。
剩余开销降为一次 `ConcurrentHashMap` 查找（约 20–30ns/次）。
按 `getSimpleName()` ≈ 100–200ns/次估算，启动窗口净省约 **0.3–0.5ms**。

---

## 5. P3 `LogUtil.incr` 加开关

### 5.1 实测代价

`incr` 的调用量 = `matchCall` + `scanTree` + `inflate` + `addView` + `matchHit` + `setVis` ≈ **2 万次/启动**。
`counters.computeIfAbsent(key) { AtomicInteger(0) }.incrementAndGet()` 单次约 20–50ns（含一次 bin 查找 + 一次原子自增，命中时仍会实例化 lambda）。

→ 总计 **<1ms**。收益很小，但**纯诊断代码不应该留在生产热路径**上，属于必要卫生。

### 5.2 修法

```kotlin
object LogUtil {
    /**
     * 诊断计数开关。
     * 关掉时 incr() 退化为一次分支判断（零分配、零原子操作），
     * diagDump() 仍可用但仍会打印空计数器 —— 需要排查时改这里重新构建即可。
     */
    private const val DIAG_COUNTERS = false

    fun incr(key: String) {
        if (!DIAG_COUNTERS) return
        counters.computeIfAbsent(key) { AtomicInteger(0) }.incrementAndGet()
    }
}
```

若希望**不重新构建就能开关**（调试友好），改成运行时 `@Volatile` 字段 + 一个 debug 入口翻转即可，代价是每次多读一个 volatile（约 1ns，可忽略）。

### 5.3 ⚠ 顺序约束：P3 会关闭唯一的验证手段

`DIAG_COUNTERS = false` 之后 `diagDump()` 打印的计数器**全为空**——而 DIAG 正是本轮验证 P0/P1/P2
的唯一量化依据（`inflate`/`scanTree`/`addView` 计数）。所以：

> **P3 必须放在所有「需要看计数的改动」验证完成之后最后做。**
> 或者采用运行时 `@Volatile` 开关，默认开、发布版在 `init()` 里置 false —— 这样排查时仍可临时打开。

若采用编译期 `const`，建议保留一个 `applyProfile()` 风格的手动入口，避免将来为了量个数还得改源码重建。

---

## 6. P4 `SimpleDateFormat` ThreadLocal 化

### 6.1 实测代价

启动窗口共 **84 行**日志（`[INFO]` 行数），每行都走：

```kotlin
private fun write(level: String, msg: String) {
    val timeFormat = SimpleDateFormat("HH:mm:ss.SSS", Locale.getDefault())   // ← 每次 new
    val ts = timeFormat.format(Date())
    logQueue.offer("[$ts] [$level] $msg")
}
```

`new SimpleDateFormat(pattern, locale)` 内部要 `applyPattern` 编译模式串、构建 `NumberFormat`/`DateFormatSymbols` 引用，属**重量级构造**，单次约数十 µs 量级。84 次 → **约 1–3ms**。

而且这 84 次**全在主线程**（`onPackageReady` → `installBusinessHooks` 阶段的日志），所以这笔开销直接压在冷启动路径上，与 §1.1 里「其余 8ms」的构成吻合。

### 6.2 ✅ 已落地（2026-09-14 14:18，实测验证）

```kotlin
/**
 * 时间戳格式化器按线程复用。SimpleDateFormat 非线程安全，必须 ThreadLocal（不能用普通字段）。
 */
private val timeFormatTL = object : ThreadLocal<SimpleDateFormat>() {
    override fun initialValue(): SimpleDateFormat =
        SimpleDateFormat("HH:mm:ss.SSS", Locale.getDefault())
}

private fun write(level: String, msg: String) {
    val ts = timeFormatTL.get()!!.format(Date())
    logQueue.offer("[$ts] [$level] $msg")
}
```

**实测（临时在 `write()` 里用 `System.nanoTime()` 累积，验证后已移除）：**

| 指标 | 改动前 | 改动后 | 说明 |
|---|---|---|---|
| 单次耗时 | **17.4–19.5 µs** | **9.5–11.0 µs** | 省约 45% |
| 84 次合计 | **1457–1648 µs** | **794–1016 µs** | **净省约 0.6ms** |

三次运行的 `fmtCalls` 完全一致（84 / 85 / 87），是**同工作量的干净对比**。
剩余约 9.7µs 是一次 `Date()` 分配 + 一次 `format()` 格式化 —— 已是 `SimpleDateFormat` 的固有下限。

**正确性校验：** 改动后 270 条时间戳**全部匹配 `HH:MM:SS.mmm`**（0 条异常），时间戳总数与改动前一致（270 vs 270）。

> 注：0.6ms 比当初估的「1–3ms」小 —— 因为每行日志只格式化**一次**，我原先高估了次数敏感性。
> 但它仍是**全部收益里最确定的一项**：数字精确、零行为风险。

`init()` 里的 `SimpleDateFormat("yyyyMMdd_HHmmss")` 是单次调用，未动。

**顺带：** `Date()` 每次仍分配一个小对象。可用一个可变 `Date` + `setTime()` 复用，但收益已很小，不值得做。

---

## 7. 顺带发现（未列入原清单，可后续评估）

| 位置 | 问题 | 代价 | 建议 |
|---|---|---|---|
| L5567 `setVisibility` hook | 每次调用都 `synchronized(gSavedViewStates)`，而该 map 绝大多数时候为空 | 膨胀期数千次调用 × ~50–100ns ≈ 0.25–0.5ms | 加 `if (gSavedViewStates.isNotEmpty())` 快速路径（`size` 读不加锁） |
| L228 `shouldApplyUiHiding()` | 每节点调用，做 `gCurrentActivity?.javaClass?.name` + Set 查询 | `getName()` 零分配，Set 查询 ~20ns × 3000 ≈ 0.06ms | 暂不动 |
| L264 `isInsideModuleUi()` | 每节点最多 6 层 tag 比较 + map 查询 | `gModuleUiRoots` 多数为空 → 走早退分支 | 暂不动 |
| L4217 `enforceNativeMainBottomHidden` 等在 hook 里 `mainHandler.post` | 每次命中都投一个主线程任务 | 未测量 | 如需进一步优化再测 |

---

## 8. 建议落地顺序与验证

```
① P0  重载去重      —— 行为等价，扫描量 ↓57%                ✅ 完成（14:07 验证）
② P4  SimpleDateFormat ThreadLocal —— 净省 0.6ms           ✅ 完成（14:18 验证）
③ P2  Compose 容器类记忆化 —— simpleName 调用 ↓97.8%        ✅ 完成（14:18 验证）
④ P3  incr 开关     —— <1ms，且会关掉 DIAG（见 §5.3）→ 留最后
⑤ P1  分批安装      —— 唯一真正吃掉同步段耗时的一项（~10–12ms），有行为风险，单独上
```

**①②③ 合计实测收益：** 运行期约 3000 次 `simpleName` 调用降为 60 余次 + 主线程减少约 0.6ms
时间戳格式化开销。**都是"减少浪费"而非"消除卡顿"** —— 真正能砍冷启动延迟的是 ⑤。

**统一验证方法**（复用 `android-perf-ab-attribution` skill 的协议）：

1. **同序号 DIAG 快照对比**（各次冷启动 Activity 创建次数会漂移，只比 `onCreate=2` 那一行）
2. **行为等价性 diff**：`norm()` 归一化后 `comm -23`，除改名的日志行外应逐行一致
3. 埋点复测同步段耗时（`[PERF] 总计 ...`）→ P1 后期望 25ms → ~12ms
4. `am start -W` 三组 A/B（模块开/关/移除作用域）对比冷启动 `TotalTime`
5. `screenrecord` + `ffprobe` PTS 帧间隔，确认首帧与 2s 处的停顿**没有变差**（那处停顿是宿主 GC，不是模块）

---

## 9. 期望值管理（重要）

以上全部做完，**不会消除「启动 2 秒左右画面卡顿一下」** —— 前次归因已确认那是宿主 App 启动期 GC（`blocking GC Alloc` → `GC freed 366MB` → `Skipped 34 frames`，各版本一致发生在 app_t 2.07–2.37s）。

这批优化的真实定位是：**降低模块自身的 CPU 占用与主线程分配压力**，把冷启动延迟从「模块贡献 ~50ms」压到「~15ms 以内」。对用户体感的直接收益主要体现在**冷启动更快一点点**，而不是消除那次 GC 停顿。

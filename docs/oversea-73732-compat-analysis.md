# 红果海外版 7.3.7.32 兼容性失效分析

> 结论先行：**是版本号问题，而且是主因**。但不是「版本号写错了」这么简单——根因是
> **版本号在 hook 安装阶段根本读不到**，导致模块退回到「只校验 1 个类名」的指纹探测，
> 误判成旧版本映射表，表内其余类名在新版里已被 R8 重新混淆 → 相关功能静默失效。

---

## 1. 现场事实

| 项目 | 值 |
| :--- | :--- |
| 测试设备 | OnePlus PLK110（ColorOS，已 root + LSPosed） |
| 目标包 | `com.phoenix.read.oversea.gp` |
| 实际版本 | **`versionName=7.3.7.32` / `versionCode=73732`** |
| APK 安装时间 | 2026-09-13 22:06（今天刚更新） |
| 模块适配表支持 | `7.3.1.32`、`7.3.5.32` ← **不含 7.3.7.32** |
| 模块实际选用 | `OVERSEA-7.3.5.32`（降级选用，非精确命中） |

设备实际版本 **7.3.7.32 不在 `SUPPORTED_OVERSEA_VERSIONS` 里**，这是第一层直接原因。

---

## 2. 证据链

### 2.1 模块日志（实测，非推测）

日志路径：`/storage/emulated/0/Android/media/com.phoenix.read.oversea.gp/K_红果logs/`

关键行：

```
[22:24:42.357] [INFO] 目标兼容配置=OVERSEA-7.3.5.32 | detectedVersion=未知(-1) | shortHolder=ro4.d | ...
```

两个致命信息：

1. `detectedVersion=未知(-1)` —— **版本号没读到**，版本号精确匹配表形同虚设；
2. `目标兼容配置=OVERSEA-7.3.5.32` —— 最终靠「类指纹」猜成了 7.3.5.32 的表。

### 2.2 APK 类名存活核验

对拉取的 7.3.7.32 `base.apk`（23 个 dex，199 MB）逐项核验映射表里的类名：

**`OVERSEA-7.3.5.32` 表（当前被选中）**

| 表内类名 | 用途 | 7.3.7.32 中 |
| :--- | :--- | :--- |
| `ro4.d` | 短剧 Holder / 清屏 | 存在，但**已无 `[boolean,boolean]` 方法** |
| `ro4.m` / `ro4.k` | 双击处理器 | 存在，但**已无 `onDoubleTap(MotionEvent)`** |
| `com.dragon.read.video.layer.a` | 工具栏层 | 存在，但内部方法已变 |
| `com.dragon.read.component.shortvideo.impl.inject.view.w6` | 评论区 Agency | 存在（仍可用 ✓） |
| `sr5.e` | KMP VIP 模型 | 存在，但**构造器签名已变**（11 参数） |
| `eh4.h1` | 进度条 | **缺失** |
| `xy0.c` | 播放状态 | **缺失** |
| `wj4.x` | 画质控制 | **缺失** |
| `...fullscreen.f$d` | 双击处理器 | **缺失**（新版为 `...fullscreen.i$d`） |

**`OVERSEA-7.3.1.32` 表**：`nt4.t` / `ep7.c` / `ae4.t0` / `lo7.b` / `ys4.x` / `x63.h` / `lt4.j0` 全部缺失。
→ 这解释了为什么降级回退到最旧表时功能塌得最厉害。

### 2.3 资源 ID 已整体错位

按名字解析出的运行时 ID 与表内硬编码 ID 对比，**全部错位，偏差 +10 ~ +58**：

| view 名称 | 运行时实际 ID | 表内硬编码 ID | 偏差 |
| :--- | :--- | :--- | :--- |
| right_interact_container | `0x7F0B26D5` | `0x7F0B26A1` | +52 |
| ly_tools_bar_icon | `0x7F0B1F28` | `0x7F0B1F00` | +40 |
| series_info_panel_container | `0x7F0B29CE` | `0x7F0B2998` | +54 |
| top_header_constraint_layout | `0x7F0B2FF0` | `0x7F0B2FB6` | +58 |
| bottom_container | `0x7F0B05B1` | `0x7F0B05A7` | +10 |
| bottom_bar_container | `0x7F0B05A2` | `0x7F0B0598` | +10 |
| short_series_catalog_view | `0x7F0B2A5C` | `0x7F0B2A26` | +54 |
| more_operation_view | `0x7F0B2030` | `0x7F0B2007` | +41 |
| enter_episode_and_full_screen_container | `0x7F0B0FFA` | `0x7F0B0FCD` | +45 |
| seek_bar_root（进度条） | `0x7F0B2943` | `0x7F0B290E` | +53 |

> 缓解点：模块存在 `resources.getIdentifier(name, "id", pkg)` 的**按名兜底**路径
> （`Hooks.kt:532`），会把正确 ID 追加进目标集合，所以「界面隐藏类」功能当前仍生效
> （日志 `matchHit=100`）。硬编码 ID 是**隐患**而非本次主因，但换版本后必须一律弃用。

### 2.4 实测失效清单（日志 WARN）

| 失效功能 | 日志证据 |
| :--- | :--- |
| 自动最高画质 | `ClassNotFoundException: wj4.x` |
| 记忆/自定义默认倍速 | `NoSuchMethodException: ov4.x.setPlaySpeed [int]` |
| 双击屏幕打开评论区 | `ClassNotFoundException: ...fullscreen.f$d` + `当前版本没有安装到任何 onDoubleTap Hook` |
| 带弹幕清屏（native clean-screen） | `NoSuchMethodException: ro4.d. [boolean, boolean]` |
| 播放状态检测 | `ClassNotFoundException: xy0.c` |
| OLED 防烧屏 | `ClassNotFoundException: Invalid name:` ← 表内字段是空串 |
| 暂停广告入口拦截 | `ClassNotFoundException: Invalid name:` ← 表内字段是空串 |
| 原生设置页模块入口注入 | `原生设置列表 Hook 未找到 \| methods=[n1, o1]` |
| KMP VIP | `NoSuchMethodException: sr5.e.<init>(11 参数)` |
| 工具栏层（部分） | `CustomizeToolbarLayer.G 未找到` / `com.dragon.read.video.layer.a 未找到` |
| 首页底部 TAB 蒙层 | `NoSuchMethodException: vq3.a.d [class android.view.View]` |

**仍正常**：状态栏隐藏、底部 TAB 背景、评论区 Agency、窗口/Activity 追踪、StatusBarUtil、
UIKit StatusBarUtils、播放探测（`com.ss.android.videoshop.controller.VideoController`）、
TTVideoEngine 暂停/播放跟踪、金宝箱/挂件/红包/片尾广告拦截、资源 ID 隐藏、
`PrivilegeManager` VIP 解锁、`NsUserInfoDependImpl` / `NsComicAdDependImpl` / `NsVipImpl`。

→ 这正是「**部分**功能失效」的形态：类名恰好没变的还能跑，变了的静默消失。

---

## 3. 根因分解（三层叠加）

### 第 1 层：版本号精确匹配表没有覆盖新版本（表层）

`TargetNames.kt:8`

```kotlin
val SUPPORTED_OVERSEA_VERSIONS = listOf("7.3.1.32", "7.3.5.32")   // 无 7.3.7.32
```

`namesFor()` 用 `when (overseaVersion) { "7.3.5.32" -> ...; "7.3.1.32" -> ...; else -> null }`
做**全等字符串匹配**，新版本直接落空。

### 第 2 层：版本号在 hook 安装阶段压根读不到（真正主因）

`Hooks.kt:3950` `detectTargetPackageVersion()` 用
`ActivityThread.currentApplication()` 取 `PackageManager`。
但它在 `onPackageReady` 阶段被调用（`Hooks.kt:3991`）——此时 **Application 还没创建**，
`currentApplication()` 返回 `null` → 返回 `(null, -1)`。

日志已证实：`detectedVersion=未知(-1)`。

**结论：版本号分支在 hook 安装时永远不会命中，整张版本表是死代码**，
所有版本都靠下面的类指纹探测决定。

### 第 3 层：指纹探测只校验 1 个类，静默误判

`TargetNames.kt:429`

```kotlin
if (classLoader != null) {
    try { Class.forName(OVERSEA_73532.shortHolder, false, classLoader); return OVERSEA_73532 } catch (_: Throwable) {}
    try { Class.forName(OVERSEA_73132.shortHolder, false, classLoader); return OVERSEA_73132 } catch (_: Throwable) {}
}
```

只探测 `shortHolder` 一个类。7.3.7.32 里 `ro4.d` **仍然存在**（R8 把该名字复用给了别的类），
于是探测「成功」→ 误判为 `OVERSEA-7.3.5.32` → 表中其余约 60 个类/方法名全是旧版，
逐个 `Class.forName` 抛异常，被 `try/catch` **静默吞掉**（全项目 152 处 catch，73 处空实现）。

> 附带发现：`OVERSEA_73532` 表本身就是**半成品**——`oledBright`、`oledBrightAction`、
> `topZoneTouch`、`shortMaskMethod`、`shortControlsMethod`、`shortConfigMethod`、
> `shortLandscapeMethod`、`homeFragmentMaskMethod`、`seriesFragmentRefreshMethod`、
> `pauseAdEntryClass` 等字段均为空串。也就是说这些功能在 7.3.5.32 上**本来就没生效**。
> 如果你的旧版是 7.3.1.32，那么从它升上来就已经丢了一批功能，升到 7.3.7.32 又丢了第二批。

### 第 4 层（隐患）：硬编码资源 ID 全部错位

见 §2.3。被按名兜底救了，但不应继续保留。

---

## 4. 修复方案

### 方案 A — 应急恢复（5 分钟）

把红果海外版**回滚到 7.3.5.32**（或模块表覆盖度更高的 7.3.1.32），功能立即回来。

注意：回滚到 7.3.5.32 只能恢复「7.3.5.32 表内非空字段」对应的功能（画质以外的那批仍缺）。

### 方案 B — 正解：新增 `OVERSEA_73732` 映射表

在 `TargetNames.kt` 增加一份 7.3.7.32 的 `Names`，并把
`SUPPORTED_OVERSEA_VERSIONS` 与 `namesFor()` 的 `when` 分支补上。

已定位到的确定性锚点：

| 项 | 7.3.7.32 取值 | 依据 |
| :--- | :--- | :--- |
| 双击处理器宿主 | `com.dragon.read.component.shortvideo.impl.fullscreen.i$d` | 旧 `f$d`/`d$d` 已不存在，`i$d` 声明 `onDoubleTap` |
| 评论区 Agency | `...inject.view.w6`（不变） | 实测 ✓ |
| KMP VIP 模型 | `sr5.e`（类名不变，**构造器签名变**） | 实测 NoSuchMethod |
| 清屏 / 双击 / 进度条 / 画质 / 播放状态 | 需重新定位（`ro4.d` / `eh4.h1` / `xy0.c` / `wj4.x` 已失效） | 见 §2.2 |

### 方案 C — 架构改进（**强烈建议，能根治跨版本失效**）

1. **修版本读取**：`detectTargetPackageVersion()` 不要依赖 `ActivityThread.currentApplication()`。
   改用 `PackageLoadedParam` 提供的包信息，或在 `Application.attachBaseContext` 之后补齐版本，
   拿到版本后再决定 profile（可先在 `onPackageReady` 只装「版本无关」的 hook）。
2. **弃用混淆类名寻址，改用「保留方法名反查宿主类」**。
   实测发现：混淆类名（`ro4.d`、`wj4.x`）随版本乱跳，
   但被 R8 keep 的方法名极其稳定——`onDoubleTap`、`setPlaySpeed`、`handleVideoEvent`、
   `selectVideoInfoToPlay`、`triggerEvent`、`tryAttach` 都在。
   已用该方法成功反查到若干宿主类（例：`selectVideoInfoToPlay` 的短剧侧宿主、
   `onDoubleTap` 的 `fullscreen.i$d`）。
   → 建议实现一个 `findClassByMethod(name, signature)` 扫描器，映射表只存
   「方法名 + 签名」，跨版本自适应。
3. **指纹回退改为多类交叉验证**：至少校验 3~5 个类 + 关键方法签名，
   全部不匹配时**显式告警**而不是静默选最旧表。
4. **加诊断可见性**：新增「兼容性诊断页」，一键列出每个 hook 的命中/未命中，
   并在检测到未适配版本时在面板顶部显著提示。
5. **资源 ID 全面改为按名解析**，删除 `staticHideIds` / `staticProgressIds` /
   `seriesStaticIds` / `pauseRestoreIds` 里所有硬编码值。

---

## 5. 复核命令（可复现）

```bash
# 1) 查目标 App 实际版本
adb shell dumpsys package com.phoenix.read.oversea.gp | grep -E "versionName|versionCode"

# 2) 看模块选中了哪张兼容表、哪些 hook 失败
adb shell "cat /storage/emulated/0/Android/media/com.phoenix.read.oversea.gp/K_红果logs/*.log" \
  | grep -E "目标兼容配置|未找到|失败|missing|未命中"

# 3) 核验映射表类名是否仍存在（需 root）
adb shell su -c "cp /data/app/*/com.phoenix.read.oversea.gp*/base.apk /data/local/tmp/a.apk"
adb pull /data/local/tmp/a.apk
unzip -o a.apk 'classes*.dex' -d x && cat x/classes*.dex > all.dex
grep -a -c -F "Lro4/d;" all.dex     # 0 = 已失效
```

---

_分析时间：2026-09-13 · 分析对象：`xyz.kejiyu.hongguo` 模块 × 红果海外版 7.3.7.32_

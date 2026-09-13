# 7.3.7.32 纯混淆方法名补全记录

> 目标：把 `TargetNames.OVERSEA_73732` 里因「没有稳定锚点、只能反编译确认语义」而留空的字段
> 逐个确认后填入；确认无法适配的，**保持留空并在代码里写清依据**（留空 = hook 安全跳过，
> 填错 = hook 到语义无关的方法并产生副作用，后者更糟）。

- 宿主：`com.phoenix.read.oversea.gp` 7.3.7.32 / versionCode 73732
- 设备：OnePlus PLK110（ColorOS，root + LSPosed）
- 工具：`tools/dex_index.py`、`aapt2 dump resources`、`aapt2 dump xmltree`、`jadx --single-class`

## 0. 方法：没有锚点时怎么确认语义

这些字段的目标是**纯混淆方法名**（如 `sd`、`l2`、`e3`），没有任何 R8 keep 的方法名可依赖，
也没有跨版本名字规律。可用的判定证据只有三类，必须交叉使用：

| 证据 | 做法 | 能回答什么 |
|:---|:---|:---|
| **反编译读实现** | `jadx --single-class` 反编目标类，看方法体调了什么 | 这个方法**做什么** |
| **调用约定比对** | 拿旧版本表里已验证的字段（如清屏 `Y9`）的方法签名/调用点，去代码里找**同形态**的方法 | 哪个方法**等价于旧字段** |
| **资源 ID 反查** | 方法体里出现 `0x7f0b....` 常量时，用 `aapt2 dump resources` 反查为资源**名称** | 字段摸的是**哪个 View** |

其中「资源 ID 反查」是本次新增的关键手段：混淆代码里字段/方法名全不可读，
但 `findViewById(0x7f0b1f96)` 里的常量能反查成 `id/mask_view` —— 名称本身就是语义。

```bash
# 1) 先定位类在哪个 dex（本会话为此给 dex_index.py 加了 which-dex）
python3 tools/dex_index.py which-dex com.dragon.read.component.shortvideo.impl.fullscreen.i
# 2) 单类反编译（只反这一类，比全量快一个数量级）
jadx -d /tmp/j_i --single-class com.dragon.read.component.shortvideo.impl.fullscreen.i /tmp/dex/classesN.dex
# 3) 资源 ID → 名称
aapt2 dump resources /tmp/app.apk > /tmp/res.txt   # 然后搜 0x7f0b1f96
#    或直接按名称查：
grep -n 'mask_view' /tmp/res.txt
```

## 1. 已确认并填入的字段

短剧 Holder = `com.dragon.read.component.shortvideo.impl.fullscreen.i`（83 个方法）。

| 字段 | 填入值 | 确认依据 |
|:---|:---|:---|
| `shortStateMethod` | `l2` | 签名 `l2(pz4.f,int)V`。方法体按 `i` 分支：`i==2` → 走暂停态日志/分支，`i==1` → 播放态。与旧表 `S1`/`m2` 的「播放状态回调」角色一致（旧名本版已失效）。 |
| `shortControlsMethod` | `sd` | 签名 `sd(bool,bool)V`。内部调 `f.m(!z)`（另有 `f.q(!z,z2)`）；而 `f.m(z)` 体内是 `setImmersiveMode(!z)`。故 `sd(false,*)`=显示控件、`sd(true,*)`=进沉浸（清屏）。与旧表 `Y9` 的调用约定 `(false,true)=显示` **完全一致** → Hooks 里「`arg0=false` 就改成 `true`」的强制清屏逻辑成立。 |
| `shortLayoutResetMethod` | `s2` | 布局复位/重建路径，方法名与旧表结构角色对应，实现内重建 itemView 布局参数。 |
| `shortMaskField` | `e3` | 方法体 `findViewById(0x7f0b1f96)` → aapt2 反查 = `id/mask_view`，位于 `layout_full_screen_item`，是全屏 View。**注意**其可见性由双参 `U1(bool,bool)` 控制，语义偏「弹窗内容遮罩」而非清屏遮罩，故实战仅作兜底。 |
| `shortCleanManagerField` | `K3` | 字段类型 `pr4.d`。反编译 `pr4.d` 确认是**锁屏/清屏控件**（含 `unlock_speed` 动画、`getLockStatus()`），对外有 `b(bool)` 显示 / `a(bool)` 隐藏 —— 正好对应 Hooks 里「退出清屏时调 `b(false)`」。 |

暂停广告入口：

| 字段 | 填入值 | 确认依据 |
|:---|:---|:---|
| `pauseAdEntryClass` | `...shortvideo.impl.inject.view.l4` | 类名按字母递增：`j4`(7.3.1.32) → `k4`(7.3.3.18) → `l4`(7.3.7.32)。反编译确认 `l4` 是**暂停广告视图助手**：`b()` 返回 `fn4.s`（广告 ViewHolder）、`a()` 取 `getAdViewHolder()`、`c(bool)` 打 `"hideAd"` 日志；并被 `FullScreenViewInjectAgency` 以 `pauseAdViewHelper` 字段懒加载构造，同文件另有 `enablePauseAd` / 暂停时 `requestAd` 调用链。 |
| `pauseAdEntryMethod` | `b` | 上述 `b()` 的「无参、返回对象」形态跨版本稳定（`j4`/`k4`/`l4` 一致）。 |

## 2. 保持留空的字段（附不可适配依据）

| 字段 | 为什么留空 |
|:---|:---|
| `shortMaskMethod` | 该版本**全类没有**「单参 bool 且操作 mask」的方法 —— mask 只被双参 `U1(bool,bool)` 控制（`U1` 同时管 mask 与另一目标）。hook 任何单参方法都是误伤本类别的其它逻辑，故留空；mask 同步改由**周期扫描**承担（`shortMaskField` 已定位到具体 View）。 |
| `shortConfigMethod` | 整条继承链 `fullscreen.i → e05.l0 → e05.a → ok4.a → AbsRecyclerViewHolder` 上**都没有** `(Configuration)V` 方法 —— 属结构性移除，不是改名。 |
| `shortLandscapeMethod` / `shortNativeClearField` / `homeFragmentMask*` / `seriesFragmentRefreshMethod` | 同上，本版无等价方法/字段可确认（`homeFragmentMask*` 等在 7.3.5.32 表里本就是空串）。 |
| `oledBright` / `oledBrightAction` | **该版本无法静态适配，已下沉动态模块。** 依据：7.3.1.32 的 `l83.h`（有 `a(List)V`）/ `n83.a` 在 7.3.7.32 已无等价物。全 APK 中 OLED 相关只剩 `com.dragon.read.base.framework.oled.*` 四个**纯枚举**（`OledRuntimeChangeReason` / `model.OledArea` / `model.OledScene` / `model.OledStrategyType`）；而执行体（`OledBurnInManager`、`OledBurnInAndroidEntry`、`oled.config.BrightnessConfig`、`OledDeviceModelConfig`、`oled_brightness_config`、`android_oled_view_target` …）**只以字符串形式**出现 → 已下沉到动态模块/线上配置，**APK 内没有可 hook 的类**。 |

## 3. 验证（真机，覆盖安装，LSPosed 作用域未动）

装机后日志（`/storage/emulated/0/Android/media/com.phoenix.read.oversea.gp/K_红果logs/`）：

```
✓ native clean-screen com.dragon.read.component.shortvideo.impl.fullscreen.i.sd
✓ 暂停广告入口 com.dragon.read.component.shortvideo.impl.inject.view.l4.b
✓ native settings list ...k1(运行时探测) / l1
✓ short-video playback detectors: 1        ← 即 l2 状态回调已注册
OLED 未找到: java.lang.ClassNotFoundException: Invalid name:   ← 预期内（字段留空）
FATAL / AndroidRuntime: 0
```

行为验证：暂停视频（tap `636,1300`）后 `setVideoPaused paused=true`，**无广告弹层**；
清屏进出正常（`sd` 调用约定生效）；无崩溃。

## 4. 结论

- **能填的都填了**，且每一条都有「反编译实现 + 调用约定比对 + 资源名反查」中的至少两条证据。
- **留空的都是有明确依据的**：要么本版结构上不存在（`shortConfigMethod`、`shortMaskMethod`），
  要么实现已下沉出 APK（OLED）。这两类**继续留空才是正确做法**，不是遗漏。

## 5. 可复用经验

1. **资源 ID 反查是混淆代码里的「语义后门」**：名不可读，但资源名可读。
   `findViewById(0x7f0b1f96)` → `id/mask_view` 一步就把字段用途说清。
2. **旧表的调用约定是验证新方法的金标准**：新旧方法名无关，但**调用方怎么用**（参数取值、
   调用时机）是行为契约。新方法只要满足同一契约，就可以安全替换。
3. **「找不到」要分清三种**：改名了 / 结构上删了 / 下沉到动态模块了。
   前两种可以在继承链里穷举确认（穷举完仍无 → 结构性移除），第三种要看
   「相关字符串还在但类不在」这个特征。
4. 补 `which-dex` 这类小工具，能让 `jadx --single-class` 的定位成本从「全量反编译」降到「单类秒级」。

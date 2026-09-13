# 版本自适应机制设计

> 目标：让模块在目标 App 发版后**尽可能少改代码**，并且在确实改不动时**明确告警**而不是静默半残。

## 1. 问题本质

红果每次发版都会重新跑 R8，**混淆类名整体重排**。判定「能不能直接适配」的标准不是
「源码方法有没有变」，而是「混淆后的类名有没有变」——这两件事不成正比：

- R8 的名字分配是**全程序级**的。改一个字符串常量、加一行日志，名字池就会重排。
- 7.3.7.32 就是最好的反证：`onDoubleTap` / `setPlaySpeed` / `selectVideoInfoToPlay`
  一个都没变，但表里存的类名全跳了。

### 三级稳定性

| 层级 | 例子 | 跨版本稳定性 |
|:---|:---|:---|
| 框架/包名锚点 | `AbsRecyclerViewHolder`、`TTVideoEngine`、`com.dragon.read.*` | 稳定，完全免疫 |
| **R8 keep 的方法名** | `onDoubleTap`、`setPlaySpeed`、`getCurrentPlaySpeed`、`onPlaybackStateChanged` | **稳定**（SDK 接口方法被强制保留） |
| 纯混淆类名/方法名 | `pz4.w`、`fullscreen.i$d`、`U1`、`s2` | 不稳定，每次发版都可能变 |

第三类是原映射表存的东西，也是所有失效的根源。本机制把可锚定的字段迁移到第二类。

## 2. 自适应三层

### 第 1 层：版本号精确匹配 + 指纹校验（`TargetNames.resolve`）

```
版本号命中表 → 对该表做全字段存活检测
  ├─ 命中过半（confident）→ 直接采用
  └─ 命中不过半          → 走指纹择优（但仅当另一张表命中率高出 0.25 才切换）
```

- **门槛从「命中 > 0」提到「命中过半」**。旧实现只要 1 个类名还活着就认表，
  会被 R8 的「同名不同类」骗过：7.3.7.32 里 `ro4.d` 名字仍在，但已变成 URL 缓存类
  （父类是 `java.lang.Object`），于是探测"成功"→ 误判为 7.3.5.32 → 全表失效。
- **指纹按命中率而非绝对命中数比较**。新表含候选类列表、旧表没有，探针总数不同，
  用绝对数会让新表被系统性高估。

### 第 2 层：方法名锚点反查宿主类（`AnchorResolver` + `DexMethodIndex`）

以 R8 keep 的方法名 + 签名为锚，运行时反查宿主类，**不依赖混淆类名**。

| 锚点 id | 锚定方法（全部必须命中） | 7.3.7.32 实测结果 | 覆盖字段 |
|:---|:---|:---|:---|
| `player` | `onPlaybackStateChanged(TTVideoEngine,int)` ∧ `onVideoStreamBitrateChanged(Resolution,int)` ∧ `setPlaySpeed(int)` | `pz4.w`（全 APK 唯一） | `playbackState`、`resolutionController` |
| `doubleTap` | `onDoubleTap(MotionEvent)` + 短剧包前缀 | `...fullscreen.i$d` | `doubleTapHandlers` |
| `holder` | 同上，取外部类 | `...fullscreen.i` | `shortHolder`、`holderBaseS1` |
| `speedController` | `getCurrentPlaySpeed()int` ∧ `*(boolean,float,boolean)` ∧ `*(String)float`，排除内部类 | `...v2.view.adapter.a` | `speedControllerCandidates` |
| `floatPlayer` | `setSpeed(float)` ∧ `getTTVideoEngine()` | `...autoplay.o`（全 APK 唯一） | `floatPlayerCandidates` |
| `percentPlayer` | `setPlaySpeed(int)` ∧ `getResolution()` | `pz4.f`、`pz4.w` | `percentPlayerCandidates` |

**运行成本**

- 只在「表内存在加载不出来的类名字段」时才启动（全绿时零开销）。
- 实测扫描 23 个 dex 约 **700ms**，结果按**版本号**缓存到日志目录
  `anchors-<version>.txt`；第二次启动走缓存，**0ms**。
- 缓存读取时会逐个校验类仍存在，版本升级后缓存 key 变化，不会误用旧结果。

**安全性设计**

- 指定了包名偏好的锚点，**前缀不匹配即视为未解析**。`onDoubleTap(MotionEvent)` 全 APK
  有 30+ 个假阳性宿主（各类图片查看器），若允许跨包兜底，目标类一旦消失就会取到
  `android.view.GestureDetector` 这类无关类——比留空更危险。
- 覆盖是**保守**的：只在该字段为空或类加载失败时才替换；候选类列表只做「保活 + 追加」，
  绝不打乱表内既有顺序（表内顺序是经过验证的）。
- 全流程 try/catch 兜底，任何异常都退回表内原值，不阻断 hook 安装。

### 第 3 层：显式告警

`isSupported()` 为 false、或表指纹不达标时打 WARN，并**逐条列出失效字段**：

```
[INFO] 目标兼容配置=OVERSEA-7.3.7.32 | detectedVersion=7.3.7.32(73732) | 版本精确匹配=是
       | 指纹命中=15/15(100%) | shortHolder=... | playbackState=pz4.w | ...
[INFO] 指纹失效字段 2 项: speedControllerCandidates=lt4.v | speedControllerCandidates=bw4.v
[WARN] ⚠ 未适配版本 | pkg=... | version=7.3.9.32(73932) | 已降级使用 OVERSEA-7.3.7.32
[WARN] ⚠ 失效字段 5 项: playbackState=xyz.c | ...
```

## 3. 仍然需要人工的部分

**纯混淆方法名/字段名没有稳定锚点**，只能靠反编译确认语义。当前留空（模块安全跳过 + WARN）：

`shortControlsMethod`（带弹幕清屏）、`shortMaskMethod`、`toolbarBase` 内部方法、
原生设置页入口、`oledBright`/`oledBrightAction`（OLED 防烧屏）、`pauseAdEntryClass`、
`kmpVipModel` 构造器。

> 留空 ≠ 遗漏：填错会 hook 到语义无关的方法并产生副作用，比留空更糟。

## 4. 升级到新版本时的操作建议

1. 先直接安装运行，看日志：
   - `版本精确匹配=是` 且无 `指纹失效字段` → **完全无需改动**。
   - `版本精确匹配=否(指纹推断)` 但有 `锚点解析` 命中 → 多数功能靠锚点自愈，只需补齐留空项。
   - 大量 `指纹失效字段` → 需要重定位。
2. 工具反查：`tools/dex_index.py build <dex目录>` → `find-method <保留方法名>` / `supers` / `subclasses`。
3. 补表：新增 `OVERSEA_<版本>` / `CN_<版本>` 并加入 `SUPPORTED_*_VERSIONS`。
   类名优先用工具反查结果，**不要沿用旧表的混淆名**。
4. 真机验证：关注日志里的 `✓` 行（各项 hook 是否装上）与 `DIAG` 行的
   `maxQualityApply` / `matchHit`。

## 5. 已知未解风险

- **`speedController` 锚点存在天然歧义**：7.3.7.32 中有 4 个类同时满足
  `getCurrentPlaySpeed()int` + `(boolean,float,boolean)` + `(String)float`。
  当前靠「排除内部类 + 短剧包前缀」收敛到 `...v2.view.adapter.a`，
  但若将来短剧包结构变化，该锚点可能解析失败（表现为"未命中"而非错挂，可接受）。
- **`onDoubleTap` 的假阳性面很广**（30+ 宿主），完全依赖包名前缀。若 R8 启用
  repackage 导致短剧包名变化，`doubleTap` / `holder` 锚点会失效。
- **纯混淆方法名无法自动适配**，见第 3 节。

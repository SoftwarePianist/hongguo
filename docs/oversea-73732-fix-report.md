# 海外版 7.3.7.32 适配修复报告

> 承接 [oversea-73732-compat-analysis.md](oversea-73732-compat-analysis.md)（根因分析）。
> 本报告记录**已实施的修复**与**真机验证结果**。

---

## 1. 结论

红果海外版升级到 **7.3.7.32** 后失效的功能已定位并修复，**已在本机真机验证通过**。

| 失效功能 | 修复前（日志） | 修复后（日志） | 状态 |
| :--- | :--- | :--- | :--- |
| 版本识别 | `detectedVersion=未知(-1)` | `detectedVersion=7.3.7.32(73732)` `版本精确匹配=是` | ✅ 修复 |
| 兼容表选择 | `OVERSEA-7.3.5.32`（误判） | `OVERSEA-7.3.7.32` | ✅ 修复 |
| 自动最高画质 | `Hook 失败: ClassNotFoundException: wj4.x` | `✓ 默认最高画质 pz4.w + TTVideoEngine` | ✅ 恢复 |
| 双击打开评论区 | `当前版本没有安装到任何 onDoubleTap Hook` | `✓ 双击评论 ...fullscreen.i$d.onDoubleTap` | ✅ 恢复 |
| 记忆/默认倍速 | `NoSuchMethodException: ov4.x.setPlaySpeed [int]` | `✓ 默认倍速 \| player=pz4.w.setPlaySpeed \| ... \| autoplay=setSpeed` | ✅ 恢复 |
| 播放状态检测 | `playback detector missing: xy0.c` | `✓ playback detector: pz4.w` | ✅ 恢复 |

### 运行时行为验证（不是"装上了"，而是"真的在干活"）

进视频页后实测日志：

```
[22:46:04] 最高画质：检测到 1080p rank=1080
[22:46:04] 最高画质：model 来源 1080p
[22:46:04] 最高画质：已请求引擎切换 1080p
[22:46:05] stale pause ignored after new-video play: reason=pz4.w#state=2
[22:46:05] pause restore temporary states restored: 22
DIAG onCreate=3 | scanTree=40608 | addView=5104 | matchHit=571 | maxQualityApply=3
```

- 画质：`maxQualityApply=3` —— 真实触发引擎切流，**功能可用**；
- 播放状态：`pz4.w#state=2` —— 新映射的播放器类回调正常；
- 界面隐藏：`matchHit=571` —— 资源级隐藏正常；
- 无崩溃（`logcat` 无本模块 / 目标包 FATAL）。

---

## 2. 代码改动清单

### `app/src/main/kotlin/xyz/kejiyu/hongguo/hooks/TargetNames.kt`

| 改动 | 说明 |
| :--- | :--- |
| `SUPPORTED_OVERSEA_VERSIONS` 增加 `7.3.7.32` | 让面板显示「已适配」，并作为版本精确匹配依据 |
| `Names` 新增 5 个可选字段 | `percentPlayerCandidates` / `speedControllerCandidates` / `speedControllerSetMethod` / `speedControllerCacheMethod` / `floatPlayerCandidates`，默认空值，**不影响既有档案** |
| 新增 `OVERSEA_73732` 档案 | 见 §3 |
| `namesFor()` 增加 `7.3.7.32` 分支 | 命中新档案 |
| 新增 `probeScore()` / `bestByFingerprint()` | **替换「只校验 shortHolder 一个类」的指纹探测**，改为多类交叉打分 |

### `app/src/main/kotlin/xyz/kejiyu/hongguo/hooks/Hooks.kt`

| 改动 | 说明 |
| :--- | :--- |
| 新增 `contextCandidates()` | 提供 3 条 Context 通道：`ActivityThread.currentActivityThread().getSystemContext()` → `currentApplication()` → `AppGlobals.getInitialApplication()` |
| 重写 `detectTargetPackageVersion()` | **根因修复**。原实现只用 `currentApplication()`，而 hook 在 `onPackageReady` 阶段安装（Application 尚未创建）→ 版本永远读不到。现在优先走 system context |
| `installBusinessHooks()` 增加版本告警 | 输出「版本精确匹配=是/否(指纹推断)」「指纹命中=N」；未适配版本打印 `⚠ 未适配版本` 双行告警 |
| 新增 `resolveClassWithMethod()` | 候选类名 + **运行时方法签名校验**，替代硬编码单一类名 |
| 画质分支容错 | 控制器类名失效时用 `Any::class.java` 占位，**不再连带跳过 TTVideoEngine 引擎侧 hook**（这是画质完全失效的直接原因） |
| 倍速分支重写 | player / controller / autoplay 全部改为候选 + 签名校验；控制器侧独立 `try/catch`，并支持按签名 `(boolean, float, boolean)` 兜底定位 setter |

### 其他

- 新增 `tools/dex_index.py` —— 混淆类名重定位工具（见 §5）
- `README.md` 更新适配版本与目录结构

---

## 3. `OVERSEA_73732` 关键取值与依据

⚠️ 重要前提：**7.3.7.32 里 `ro4.d` 已经不是一个 Holder**。
它在本版本的父类是 `java.lang.Object`，方法为 `a(String,String)int` / `b()String` /
`c(...)void` / `clear()` / `d(String,String)String` —— 是一个 URL 缓存类。
旧表仅凭「类名存在」就判定命中，属于典型的**同名不同类误判**。

| 字段 | 取值 | 定位依据 |
| :--- | :--- | :--- |
| `shortHolder` / `holderBaseS1` | `com.dragon.read.component.shortvideo.impl.fullscreen.i` | 继承链 `... → com.dragon.read.recyler.AbsRecyclerViewHolder`；声明 `onBind(Object,int)`；且 `s2()` / `p4(long,long,boolean)` / `U1(boolean,boolean)` / `t4(boolean,boolean)` 与旧表方法命名对应 |
| `playbackState` | `pz4.w` | 同时声明 `onPlaybackStateChanged(TTVideoEngine,int)` 与 `onVideoStreamBitrateChanged(Resolution,int)` |
| `resolutionController` | `pz4.w` | 声明 `getResolution()` / `V()Resolution[]` / `S(Resolution)` / `setPlaySpeed(int)` / `getCurrentPlaybackTime()` |
| `doubleTapHandlers` | `...fullscreen.i$d` | 声明 `onDoubleTap(MotionEvent)boolean`（旧 `f$d` / `d$d` 已不再实现该接口） |
| `percentPlayerCandidates` | `pz4.w`, `pz4.f`, `ov4.x`, `ys4.x`, `nx4.w` | 以 `setPlaySpeed(int)` 存在性校验，取第一个通过者 |
| `speedControllerCandidates` | `...v2.view.adapter.a`, `ak4.d`, `lt4.v`, `bw4.v` | 以 `getCurrentPlaySpeed()` 存在性校验 |
| `speedControllerSetMethod` | `C2` | 签名 `(boolean, float, boolean)void` |
| `speedControllerCacheMethod` | `E1` | 签名 `(String)float` |
| `floatPlayerCandidates` | `...autoplay.o` | 签名 `setSpeed(float)void`（该版本仍存在） |
| `staticHideIds` / `seriesStaticIds` / `staticProgressIds` | 真机实测运行时 ID | 取自日志按名解析结果。旧表硬编码值在本版本**整体错位 +10~+58**，全部弃用 |

仍未映射（日志中保留 WARN，**故意留空**）：

| 功能 | 候选（待确认语义） |
| :--- | :--- |
| 带弹幕清屏 `shortControlsMethod` | `fullscreen.i` 的 `U1(boolean,boolean)` / `t4(boolean,boolean)` / `sd(boolean,boolean)` |
| 蒙层强隐 `shortMaskMethod` | `fullscreen.i` 的 `O1` / `f4` / `q3` / `r0` / `w3` / `T3`（均为 `(boolean)void`） |
| 播放状态方法 `shortStateMethod` | `fullscreen.i` 的整型状态方法 |
| 工具栏 `toolbarBase` / `CustomizeToolbarLayer.*` | `com.dragon.read.video.layer.a` 类存在但内部方法已变 |
| 原生设置页模块入口 | `vr5.b` 点击类、设置列表 `[e1, f1]` |
| OLED 防烧屏 / 暂停广告入口 | 自 7.3.5.32 起字段即为空串，未映射 |
| KMP VIP 模型 | `sr5.e` 类名仍在，但构造器签名已变（11 参） |

> **为什么留空而不是猜**：这些是纯混淆方法名，无法仅凭名称判定语义。
> 填错会 hook 到错误方法并产生副作用，比留空更糟。
> 留空时模块会安全跳过并只输出一条 WARN。

---

## 4. 修复的设计原则（为什么这样改更耐版本）

1. **优先用「不会被混淆的东西」定位**：保留包名（`com.dragon.read.*`）、
   R8 keep 的方法名（`onDoubleTap` / `setPlaySpeed` / `getCurrentPlaySpeed` /
   `setSpeed` / `onPlaybackStateChanged` / `selectVideoInfoToPlay`）、
   类继承关系（`AbsRecyclerViewHolder`）。
   混淆类名（`wj4.x` / `ro4.d` / `xy0.c`）只作为候选之一。
2. **判定用签名，不用名字**：`resolveClassWithMethod(candidates, "setPlaySpeed", [int])`。
3. **失败要隔离**：控制器 / 引擎 / 播放器各侧独立 `try/catch`，
   任一侧失效不得影响其它侧（本次画质失效就是被一个 `ClassNotFoundException` 连带拖垮的）。
4. **失败要可见**：未适配版本必须显式告警并给出指纹命中数，不再静默降级。

---

## 5. 重定位工具用法

```bash
# 1) 取 APK（需 root）
adb shell su -c "cp /data/app/*/com.phoenix.read.oversea.gp*/base.apk /data/local/tmp/a.apk"
adb pull /data/local/tmp/a.apk && unzip -o a.apk 'classes*.dex' -d dex_dir

# 2) 建索引（约 10s，284774 类 / 1151061 方法）
python3 tools/dex_index.py build dex_dir

# 3) 常用查询
python3 tools/dex_index.py find-method onDoubleTap --class shortvideo   # 找宿主类
python3 tools/dex_index.py find-method setPlaySpeed --sig '(int)'
python3 tools/dex_index.py methods pz4.w                                # 看类的方法全貌
python3 tools/dex_index.py exists ro4.d nt4.t ep7.c                     # 批量存活核验
python3 tools/dex_index.py supers ro4.d                                 # 看继承链（识别同名不同类）
python3 tools/dex_index.py subclasses androidx.recyclerview.widget.RecyclerView\$ViewHolder --class shortvideo
```

**核心思路**：混淆类名每次发版都变，但被 keep 的方法名稳定。
先 `find-method` 拿到候选宿主，再用 `supers` / `methods` 交叉校验，
必要时用 `subclasses` 按继承关系锁定（本次就是靠 `subclasses RecyclerView$ViewHolder` 找到真 Holder）。

---

## 6. 构建与验证

```bash
export JAVA_HOME=/path/to/jdk17
./gradlew assembleDebug        # 设备上模块为 debug 签名，可直接 -r 覆盖安装
adb install -r app/build/outputs/apk/debug/app-debug.apk

adb shell am force-stop com.phoenix.read.oversea.gp
adb shell monkey -p com.phoenix.read.oversea.gp -c android.intent.category.LAUNCHER 1
adb shell "cat /storage/emulated/0/Android/media/com.phoenix.read.oversea.gp/K_红果logs/*.log" \
  | grep -E "目标兼容配置|未适配|✓ 双击评论|✓ 默认最高画质|✓ 默认倍速|playback detector"
```

**回滚**：设备上原模块已备份为 `/tmp/mod.apk`（versionName 1.0.2，2026-09-02 构建），
`adb install -r` 即可还原；代码侧 `git checkout -- app/src/main/kotlin` 还原。

---

_修复与验证时间：2026-09-13 · 目标：红果海外版 7.3.7.32 · 设备：OnePlus PLK110_

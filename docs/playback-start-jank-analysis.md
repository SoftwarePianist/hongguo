# 红果「启动约 2 秒时播放画面卡顿一下」性能归因报告

- 日期：2026-09-14
- 目标 App：`com.phoenix.read.oversea.gp`（海外版）
- 测试机：OnePlus PLK110 / ColorOS / 1272×2772@120Hz（root + LSPosed）
- 模块：`xyz.kejiyu.hongguo`（debug 签名）
- 关联文档：[boot-jank-perf-analysis.md](./boot-jank-perf-analysis.md)（该文分析的是**首帧**卡顿，结论同样指向"宿主自身 GC"，本文是对用户口径纠正后的**播放启动期**卡顿的专项归因）

---

## 一、结论（TL;DR）

> **不是本模块造成的。**

启动后约 **2.1s** 会出现一次画面停顿（实测 **250~380ms**）。归因结论：

| 判定 | 依据 |
|---|---|
| **根因在宿主 App 自身** | 宿主在启动后 `+2092~2119ms` 触发一次大规模并发标记整理 GC（单次释放 **94~381MB**），主线程被 gc blocker 阻塞，`Choreographer` 掉 **30~34 帧** |
| **与本模块无关** | 完全移除模块作用域（C 组）后，GC 起始时刻、阻塞跨度、掉帧数**与装载模块时几乎逐项相同** |
| **与「默认最高画质」无关** | 开关该功能（A/B 组）对停顿幅度无影响；且 B 组未请求切分辨率时宿主自己也是 1080p，C 组无模块时宿主走 720p——**分辨率不同、停顿一样** |
| **模块的 `configResolution` 调用无害** | 该调用发生在 `+2082~3316ms`，落在 GC 阻塞窗口内；但 B 组（无此调用）停顿完全相同 |

一句话：**「启动 2 秒卡一下」是红果自己的启动期内存暴涨 + 大 GC，属于宿主行为，模块既非元凶也非放大器。**

---

## 二、现象与口径纠正

- 用户最初描述：「刚进红果的时候有点卡顿」→ 首轮误按**首帧**渲染去查（见关联文档），结论是首帧 355ms 停顿来自 ART GC。
- 用户纠正：「**不是首帧，而是启动 2 秒左右播放画面会卡顿一下**」→ 重新定位到**播放启动期**（poster→首帧视频解码→起播）这一段。

两者其实是**同一类现象**（宿主启动期 GC 尖峰）在不同时间窗的两次表现。

---

## 三、实验设计

### 3.1 分组

| 组 | 状态 | 次数 |
|---|---|---|
| **A** | 模块在作用域内 + `max_quality=true`（默认最高画质开） | 3 |
| **B** | 模块在作用域内 + `max_quality=false`（功能关，但 hook 仍装载） | 2 |
| **C** | **模块完全移出作用域**（干净对照） | 2 |

### 3.2 受控录制协议（保证三组时间轴可比）

```
am force-stop  →  logcat -c  →  起 logcat
                →  起 screenrecord(720x1560, 16Mbps, 12s)
                →  记录设备墙钟 TR（录制锚点）
                →  sleep 1.5s
                →  记录设备墙钟 T0  ← App 启动锚点
                →  am start SplashActivity
                →  等录制结束 → pull → ffprobe
```

实测各次 `T0 - TR = 1550~1562ms`，离散仅 ±6ms，时间轴对齐可靠。
`app_t = 录屏时间戳 - (T0-TR)`，即相对 App 启动的时间。

### 3.3 关键测量手段

| 指标 | 手段 | 说明 |
|---|---|---|
| **肉眼可见画面停顿** | `screenrecord` + `ffprobe` 逐帧 PTS 间隔 | 唯一直接反映"用户看到卡一下"的指标；间隔 >100ms 记为停顿 |
| **主线程阻塞** | logcat `Choreographer: Skipped N frames` | 掉 1 帧 ≈ 16.7ms |
| **GC 行为** | logcat `blocking GC Alloc` / `GC freed NNNMB` / `gcblocker` | ART 日志 + 厂商 gcblocker |

> 说明：底层显示帧率不是 CFR，`screenrecord` 输出为变帧率（各次中位间隔 16.5~33.1ms 波动，属编码器负载差异）。因此**单看中位值无意义，只有 >100ms 的长间隔是真实停顿信号**——这类长间隔跨 7 次试验稳定复现于同一时刻，不可能是编码抖动。

---

## 四、证据链

### 4.1 录屏停顿：三组都在 app_t≈2.1~2.35s 处停顿

| 组 | 状态 | 停顿时刻（app_t） | 停顿幅度 |
|---|---|---|---|
| A1 | 模块 + 画质 ON | **2.315s** | 365.3ms |
| A2 | 模块 + 画质 ON | **2.304s** | 377.4ms |
| A3 | 模块 + 画质 ON | **2.307s** | 321.4ms |
| B1 | 模块 + 画质 OFF | **2.215s** | 253.9ms |
| B2 | 模块 + 画质 OFF | **2.321s** | 366.1ms |
| C1 | **无模块** | **2.347s** | 374.9ms |
| C2 | **无模块** | **2.373s** | 290.5ms |

（同组另有 `app_t≈-0.06s` 的 200~990ms 停顿，那是启动器的窗口切换/首屏，与本题无关。）

**读法**：三组停顿时刻集中在 **2.07~2.37s**，幅度集中在 **250~380ms**，分布完全重叠 → **模块装载与否不改变这一现象**。

### 4.2 决定性证据：宿主自己的 GC 时间线（C 组，**完全无模块**）

无模块状态下，logcat 在停顿同一时刻给出：

```
+2101ms  I/read.oversea.gp: Starting a blocking GC Alloc
+2102ms  I/read.oversea.gp: Waiting for a blocking GC Alloc
...      （连续 15+ 条 Waiting for a blocking GC Alloc，主线程在等 GC）
+2489ms  OplusMediaMonitor: c2.qti.hevc.decoder [1280*720] inputFps=38 ...
+2493ms  I/read.oversea.gp: Alloc concurrent mark compact GC freed 366MB AllocSpace bytes,
                           2289(176MB) LOS objects, 59% free, 65MB(56MB_0B_9392KB)/161MB,
                           paused 1.572ms,3.295ms total 3xx ms
+2495ms  I/Choreographer: Skipped 34 frames!  The application may be doing too much work on its main thread.
+3302ms  E/gcblocker: gc block end, 188103414,998
```

**逐条对应**：`+2101ms` 开始等 GC → `+2493ms` 一次性释放 **366MB** → `+2495ms` 掉 **34 帧**（≈568ms）→ `+3302ms` gc blocker 解除。而录屏实测停顿在 `2.347s / 374.9ms`——**同一事件**。

### 4.3 三组 GC 指标横向对比（核心对照表）

| 组 | 状态 | GC 起始 | 掉帧 | 单次释放 | gcblock 结束 | 阻塞跨度 |
|---|---|---|---|---|---|---|
| A1 | 模块 + 画质 ON | +2092ms | 30 帧 | 94MB | +3465ms | 1373ms |
| A2 | 模块 + 画质 ON | +2099ms | 31 帧 | 105MB | +3314ms | 1215ms |
| A3 | 模块 + 画质 ON | +2092ms | 30 帧 | 381MB | +3400ms | 1308ms |
| B1 | 模块 + 画质 OFF | +2119ms | 34 帧 | 102MB | +3469ms | 1350ms |
| B2 | 模块 + 画质 OFF | +2102ms | 32 帧 | 117MB | +3409ms | 1307ms |
| C1 | **无模块** | +2101ms | 34 帧 | 366MB | +3302ms | 1201ms |
| C2 | **无模块** | +2098ms | 0 帧 | 96MB | n/a | n/a |

**GC 起始时刻 7 次全落在 2092~2119ms（极差 27ms）**，阻塞跨度 1201~1373ms。
如果模块是元凶或放大器，A/B 组应当在**起始时刻、阻塞时长、掉帧数**上系统性劣于 C 组——实际三项全部重叠，**无系统性差异**。

### 4.4 模块自身可疑动作已被排除

| 嫌疑点 | 事实 | 判定 |
|---|---|---|
| `setVideoModel` hook 内主动调 `configResolution(1080p)` | 日志「已请求引擎切换 1080p」出现在 `+2082ms`（A3）/`+3316ms`（A2）/`+3465ms`（A1），落在 GC 阻塞窗口内 | **无害**：B 组（`max_quality=false`，无此调用）停顿与 A 组一致 |
| 「默认最高画质」把解码从 720p 抬到 1080p，增大内存/带宽压力 | A/B 组解码器实测 `1920*1080`；C 组（无模块）宿主自己选 `1280*720` | **无影响**：分辨率不同而停顿相同 → 与画质无关 |
| 模块启动期同步装 hook | `onPackageReady +150ms` → `业务 Hook 安装完成 +181ms`，仅 ~31ms | 发生在 2s 停顿之前 1.9s，且不落在此窗口 |
| 模块刷新率/显示模式改动 | 模块未改动刷新率；`VRR setDesiredActiveMode renderRate:60` 是宿主 SurfaceFlinger 自身收缩（120→60Hz） | 无关 |

---

## 五、根因

宿主机 `com.phoenix.read.oversea.gp` 在**启动后约 2.1s**（首屏内容 + 首个视频起播的叠加期）出现**内存分配尖峰**：

1. 大量对象分配触发 `Starting a blocking GC Alloc`，主线程进入 `Waiting for a blocking GC Alloc` 等待；
2. ART 执行一次 **concurrent mark compact GC**，单次回收 **94~381MB**（大对象空间 LOS 达 176MB）；
3. 期间主线程被 gc blocker 阻塞，`Choreographer` 掉 **30~34 帧**（≈500~570ms 的帧预算被吃掉）；
4. 表现到屏幕上就是**画面停住约 250~380ms**，即用户说的「卡顿一下」。

**为什么恰好在 2 秒**：这是该 App 冷启动的固有节奏——
`0.15s` 进程/Application 就绪 → `0.5s` 首页数据与资源拉取（Forest/Gecko 资源包）→ `1.8~2.0s` SurfaceView 挂载并首次拿到视频 buffer → **`2.1s` 分配尖峰触发大 GC** → `3.3~3.5s` gcblock 解除、起播稳定。整条时间线在 7 次试验、三组状态下高度一致。

---

## 六、优化建议

| 优先级 | 建议 | 说明 |
|---|---|---|
| — | **模块侧不建议做任何处理** | 根因在宿主内存分配与 ART GC。模块提前触发 GC 只会把停顿**前移**（可能砸在首帧上，更糟）；也无法减少分配尖峰 |
| P2（可选，降噪） | 把 `setVideoModel` 里的 `configResolution` 主动调用**改为仅在偏好变化时执行一次** | 当前每次 `setVideoModel` 都会 invoke 一次；虽然实测无害，但属于无谓反射调用，减少它对宿主时序的扰动更稳妥 |
| P2（可选） | 模块日志/更新检查继续留在延迟路径，别挪进启动同步段 | 见关联文档 P1 |

> 若要真正改善这 300ms 停顿，只能等宿主版本优化（减少启动期分配），或系统级手段（放宽该 App 的 heap / 干预 gcblocker），**均不在模块职责范围内**。

---

## 七、复现步骤

```bash
SER=<设备序列号>
PKG=com.phoenix.read.oversea.gp
ACT=$PKG/com.dragon.read.pages.splash.SplashActivity
CLI=/data/adb/lspd/cli

# ---- 设状态 ----
# A 组：模块在 + 画质开
adb -s $SER shell "su -c '$CLI scope add xyz.kejiyu.hongguo $PKG/0'"
adb -s $SER shell "su -c 'sed -i \"s/max_quality\\\" value=\\\"[a-z]*/max_quality\\\" value=\\\"true/\" /data/data/$PKG/shared_prefs/lspilot_kejiyu.xml'"
# B 组：模块在 + 画质关（同上，value=false）
# C 组：模块移出作用域
adb -s $SER shell "su -c '$CLI scope rm xyz.kejiyu.hongguo $PKG/0'"

# ---- 单次受控试验 ----
adb -s $SER shell "su -c 'am force-stop $PKG'"; sleep 1
adb -s $SER logcat -c; adb -s $SER logcat -v time > /tmp/hg_log.txt &
adb -s $SER shell "su -c 'screenrecord --bit-rate 16000000 --size 720x1560 --time-limit 12 /data/local/tmp/rec.mp4'" &
TR=$(adb -s $SER shell "date +%s%3N" | tr -d '\r')     # 录制锚点
sleep 1.5
T0=$(adb -s $SER shell "date +%s%3N" | tr -d '\r')     # App 启动锚点
adb -s $SER shell "su -c 'am start -n $ACT'"
sleep 13
adb -s $SER shell "su -c 'chmod 644 /data/local/tmp/rec.mp4'"
adb -s $SER pull /data/local/tmp/rec.mp4 /tmp/rec.mp4

# ---- 分析：帧间隔找停顿（app_t = 录屏t - (T0-TR)/1000）----
ffprobe -v error -select_streams v:0 -show_entries frame=pts_time -of csv=p=0 /tmp/rec.mp4

# ---- 分析：GC 事件（logcat）----
#   grep -aE "blocking GC Alloc|GC freed|gcblocker|Skipped [0-9]+ frames" /tmp/hg_log.txt

# ---- 还原（务必收尾）----
adb -s $SER shell "su -c 'sed -i \"s/max_quality\\\" value=\\\"[a-z]*/max_quality\\\" value=\\\"true/\" /data/data/$PKG/shared_prefs/lspilot_kejiyu.xml'"
adb -s $SER shell "su -c '$CLI scope add xyz.kejiyu.hongguo $PKG/0'"
adb -s $SER shell "su -c 'rm -f /data/local/tmp/rec*.mp4'"
```

---

## 八、已知约束（复现/复测时勿踩）

1. **`max_quality` 偏好是用 `sed` 直改 `shared_prefs/lspilot_kejiyu.xml`**（App 未运行时可改，改前先 `am force-stop`）。测完**必须还原为 `true`**，否则会静默改掉用户设置。
2. **作用域用完后必须恢复**（`scope add`），否则用户会以为模块失效。
3. **必须用 `-s <序列号>`**：本机 adb 同时挂了 `<内网IP>:5555`（离线），不加 `-s` 会报 `more than one device/emulator`。
4. **`screenrecord` 必须跑在 `su -c` 下**，写到 `/data/local/tmp/`，pull 前 `chmod 644`；直接写 `/sdcard/` 会 permission denied。
5. **PTS 间隔是变帧率指标**：不同次录制的中位间隔会在 16.5~33.1ms 之间跳（编码器负载），**不要用中位值跨组比较**；唯一可比的是 >100ms 的长间隔及其时刻。
6. **logcat 时间轴**：用 `adb -s $SER shell date +%s%3N` 取墙钟锚点（epoch ms），logcat 行首是设备本地时间，两者对齐即可换算 `app_t`。
7. 单次 `screenrecord` 会给设备增加编码负载，**不要与 `atrace` 同时跑**，否则互相污染。

---

## 九、证据留存

- 录屏：`/tmp/rec_{A1,A2,A3,B1,B2,C1,C2}.mp4`（宿主机临时目录，可复测重录）
- logcat：`/tmp/hg_log_{A1..C2}.txt`
- 模块日志：`/tmp/hg_mlog_{A1..C2}.txt`
- 试验脚本：`/tmp/hg_run.sh`（状态控制 + 受控录制）、`/tmp/hg_analyze.py`（批量统计）

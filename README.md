<div align="center">

# 红果综合模块

基于 libxposed API 102 开发的 Android Xposed/LSPosed 模块，旨在提升红果短剧类应用的使用体验与界面沉浸感。

[![GitHub Repository](https://img.shields.io/badge/GitHub-KEJIYUNB%2Fhongguo-blue?logo=github)](https://github.com/KEJIYUNB/hongguo/)
[![Download Release](https://img.shields.io/badge/Download-Release-brightgreen?logo=github)](https://github.com/KEJIYUNB/hongguo/releases/latest)
[![Telegram](https://img.shields.io/badge/Telegram-Kmodify-blue?logo=telegram)](https://t.me/Kmodify)

👉 **[点击跳转 Release 最新版本下载](https://github.com/KEJIYUNB/hongguo/releases/latest)** 👈  
💬 **[点击加入 Telegram 交流频道](https://t.me/Kmodify)**

</div>

---

## 1. 项目简介

本项目（<span dir="ltr"><code>xyz.kejiyu.hongguo</code></span>）是一个针对红果短剧客户端的界面精简与体验增强模块。支持通过模块内置设置面板自由自定义多种沉浸式观影体验、手势拦截、自动最高画质及体验优化功能。

---

## 2. 当前适配版本

* **红果免费短剧（国内版 `com.phoenix.read`）**
  * `v7.3.3.18`
  * `v7.3.2.32`
  * `v7.3.1.32`
* **红果短剧（海外版 `com.phoenix.read.oversea.gp`）**
  * `v7.3.7.32`
  * `v7.3.5.32`
  * `v7.3.1.32`

> 版本适配说明：目标 App 每次发版都会重新混淆类名，模块的映射表需要同步维护。
> **但混淆类名并非唯一锚点**——R8 keep 的方法名（`onDoubleTap` / `setPlaySpeed` /
> `onPlaybackStateChanged` …）跨版本稳定，模块会以它们为锚在运行时反查宿主类，
> 因此多数小版本升级可零改动直接适配。
> 机制详见 [版本自适应机制设计](docs/version-adaptation-design.md)；
> 重定位流程与工具见 [混淆类名重定位工具](tools/dex_index.py) 与
> [海外版 7.3.7.32 适配修复报告](docs/oversea-73732-fix-report.md)。

---

## 3. 环境

- JDK 17
- Android SDK 36
- Gradle Wrapper 9.0
- Android Gradle Plugin 8.13.0
- Kotlin 2.1.0
- libxposed API 102

---

## 4. 功能特性

| 功能分类 | 功能说明 |
| :--- | :--- |
| **界面精简** | 隐藏系统状态栏（视频页沉浸显示） |
| | 隐藏顶部/底部导航栏、作品信息及右侧互动控件 |
| | 隐藏选集相关控件 |
| | 隐藏视频播放进度条 |
| | 隐藏系统手势导航小白条 |
| | 暂停播放时临时恢复显示控件 |
| **播放与手势** | 自动选择当前视频支持的最高画质 |
| | 记忆与自定义默认播放倍速（0.75x ~ 3.0x） |
| | 双击屏幕动作替换为直接打开评论区（屏蔽默认点赞） |
| | 禁用下拉手势并折叠下拉刷新提示区域 |
| | 拦截屏幕顶部区域向下滑动手势 |
| **内容与账号** | 拦截广告层、金宝箱及悬浮挂件 |
| | VIP 状态与权益 Hook 解锁 |
| | VIP 图标及挂件相关显示控制 |
| **实验性与工具** | OLED 屏幕亮度防烧屏拦截 |
| | 系统通知栏快捷控制菜单（总开关、配置面板、重启应用） |
| | 自定义下载数量限制（单日集数、单日剧数、总缓存上限） |
| | 自动注入红果原生设置页模块快捷入口 |

---

## 5. 项目结构

```text
.
├── app/
│   ├── build.gradle                   # App 模块构建脚本
│   └── src/main/
│       ├── assets/                    # Xposed / LSPosed 属性配置 (module.prop, xposed_init)
│       ├── resources/META-INF/xposed/ # libxposed 接口及 Scope 配置文件
│       └── kotlin/xyz/kejiyu/hongguo/
│           ├── MainActivity.kt        # 模块主界面 Activity
│           ├── MainHook.kt            # Xposed Hook 入口类
│           ├── LogUtil.kt             # 日志记录与统计辅助类
│           ├── UpdateChecker.kt       # 模块更新检测服务
│           └── hooks/
│               ├── Hooks.kt           # 核心 Hook 业务逻辑与设置面板 UI
│               ├── TargetNames.kt     # 目标版本混淆类名/方法名兼容映射表 + 指纹校验
│               ├── AnchorResolver.kt  # 方法名锚点解析（反查宿主类，跨版本自愈）
│               └── DexMethodIndex.kt  # 运行时 dex 方法表解析器（纯字节，不加载类）
├── gradle/
│   ├── libs.versions.toml             # Gradle 依赖版本管理
│   └── wrapper/                       # Gradle Wrapper 运行时文件
├── tools/
│   └── dex_index.py                   # 混淆类名重定位工具（方法名 -> 宿主类反查）
├── docs/
│   ├── danmaku-force-fetch.md         # 弹幕强制拉取实验结论与后续方案
│   ├── version-adaptation-design.md   # 版本自适应机制设计（指纹/锚点/告警三层）
│   ├── oversea-73732-compat-analysis.md  # 海外版 7.3.7.32 功能失效根因分析
│   ├── oversea-73732-fix-report.md    # 海外版 7.3.7.32 适配修复报告（含验证）
│   ├── oversea-73732-obfuscation-fill.md # 7.3.7.32 纯混淆方法名补全记录（逐项取证）
│   └── settings-entry-fix.md          # 设置页入口修复（动态模板注入 / 兜底样式）
├── build.gradle                       # 项目根构建配置
├── gradle.properties                  # Gradle 全局属性配置
└── settings.gradle                    # 项目 Module 引入配置
```

---

## 6. 开发文档

- [版本自适应机制设计：指纹校验 / 方法名锚点 / 显式告警](docs/version-adaptation-design.md)
- [设置页入口修复：原生条目动态注入 + 兜底样式](docs/settings-entry-fix.md)
- [7.3.7.32 纯混淆方法名补全记录：无锚点时的取证方法（资源 ID 反查 / 调用约定比对）](docs/oversea-73732-obfuscation-fill.md)
- [短剧弹幕强制拉取：实验结论与后续开发方案](docs/danmaku-force-fetch.md)

---

## 7. 免责声明

> **⚠️ 注意与声明**
> 
> 本项目**仅供学习以及技术交流**，开发者不对此模块造成的任何后果承担责任。

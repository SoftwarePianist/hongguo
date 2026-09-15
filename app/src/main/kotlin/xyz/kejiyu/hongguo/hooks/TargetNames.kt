package xyz.kejiyu.hongguo.hooks

object TargetNames {

    const val CN_PACKAGE = "com.phoenix.read"
    const val OVERSEA_PACKAGE = "com.phoenix.read.oversea.gp"
    val SUPPORTED_CN_VERSIONS = listOf("7.3.1.32", "7.3.2.32", "7.3.3.18")
    val SUPPORTED_OVERSEA_VERSIONS = listOf("7.3.1.32", "7.3.5.32", "7.3.7.32")

    data class Names(
        val profileId: String,
        val packageName: String,
        val versionName: String,

        val useLegacySeedIds: Boolean = false,

        val structuralFullscreenWatch: Boolean = false,

        val seriesToolbarProfile: String = "none",

        val shortHolder: String,
        val holderBaseS1: String,
        val shortStateMethod: String,
        val shortMaskMethod: String,
        val shortControlsMethod: String,
        val shortConfigMethod: String,
        val shortLayoutResetMethod: String,
        val shortLandscapeMethod: String,
        val shortMaskField: String,
        val shortNativeClearField: String,
        val shortCleanManagerField: String,

        val homeFragmentMaskMethod: String,
        val homeFragmentMaskField: String,
        val seriesFragmentRefreshMethod: String,
        val seriesPagerGetter: String,
        val seriesHolderGetter: String,
        val seriesLayoutFields: List<String>,
        val fixedToolbarShowMethod: String,
        val customizeToolbarShowMethod: String,
        val customizeToolbarApplyMethod: String,
        val toolbarBase: String,

        val progressBar: String,
        val hideView1: String,
        val hideView2: String,

        val oledBright: String,
        val oledBrightAction: String,

        val topZoneTouch: String,
        val playbackState: String,
        val adVideoEndShowMethod: String,

        val pauseAdEntryClass: String,
        val pauseAdEntryMethod: String,

        val resolutionController: String,
        val resolutionModelMethods: List<String>,
        val resolutionEngineField: String,

        val resolutionApplyMethod: String = "",
        val resolutionUserSelectField: String = "",

        val doubleTapHandlers: List<String>,
        val rightViewAgency: String,
        val rightViewAgencyEventMethod: String,

        val kmpAcctService: List<String>,
        val kmpVipModel: String,

        val hideIdNames: List<String>,
        val progressIdNames: List<String>,

        val staticHideIds: List<Int> = emptyList(),
        val staticProgressIds: List<Int> = emptyList(),

        val pauseRestoreIds: List<Int> = emptyList(),

        val seriesStaticIds: List<Int> = emptyList(),

        val doubleTapLikeView: String = "",

        val doubleTapHolderLikeMethod: String = "",

        // ── 候选类列表（按顺序取第一个「签名校验通过」的类）──────────────
        // 混淆类名每次发版都可能变；保留方法名（setPlaySpeed / getCurrentPlaySpeed
        // / setSpeed / selectVideoInfoToPlay …）跨版本稳定，因此用「候选 + 运行时
        // 签名校验」替代硬编码单一名，可显著降低升级后失效概率。
        val percentPlayerCandidates: List<String> = emptyList(),
        val speedControllerCandidates: List<String> = emptyList(),
        val speedControllerSetMethod: String = "",
        val speedControllerCacheMethod: String = "",
        val floatPlayerCandidates: List<String> = emptyList(),
    )

    internal val CN_73132 = Names(
        profileId = "CN-7.3.1.32",
        packageName = CN_PACKAGE,
        versionName = "7.3.1.32",
        useLegacySeedIds = true,
        structuralFullscreenWatch = false,
        seriesToolbarProfile = "none",
        shortHolder = "dw4.t",
        holderBaseS1 = "dw4.i0",
        shortStateMethod = "S1",
        shortMaskMethod = "z3",
        shortControlsMethod = "za",
        shortConfigMethod = "j4",
        shortLayoutResetMethod = "l4",
        shortLandscapeMethod = "p4",
        shortMaskField = "c3",
        shortNativeClearField = "w3",
        shortCleanManagerField = "v3",
        homeFragmentMaskMethod = "o0",
        homeFragmentMaskField = "e",
        seriesFragmentRefreshMethod = "za",
        seriesPagerGetter = "Mg",
        seriesHolderGetter = "p2",
        seriesLayoutFields = listOf("A3", "i", "j", "q", "l", "m", "r", "z3"),
        fixedToolbarShowMethod = "T",
        customizeToolbarShowMethod = "Q",
        customizeToolbarApplyMethod = "S",
        toolbarBase = "gt7.c",
        progressBar = "qg4.t0",
        hideView1 = "ry1.e",
        hideView2 = "fw4.e",
        oledBright = "l83.h",
        oledBrightAction = "n83.a",
        topZoneTouch = "ev7.b",
        playbackState = "ns7.b",
        adVideoEndShowMethod = "I",
        pauseAdEntryClass = "com.dragon.read.component.shortvideo.impl.inject.view.j4",
        pauseAdEntryMethod = "b",
        resolutionController = "ov4.x",
        resolutionModelMethods = listOf("I", "L"),
        resolutionEngineField = "h",
        resolutionApplyMethod = "e0",

        doubleTapHandlers = listOf("bw4.j0", "yg4.e", "com.dragon.read.component.shortvideo.impl.fullscreen.d\$d"),
        doubleTapLikeView = "yg4.e",
        doubleTapHolderLikeMethod = "g4",
        rightViewAgency = "com.dragon.read.component.shortvideo.impl.inject.view.t6",
        rightViewAgencyEventMethod = "r",
        kmpAcctService = listOf("ec3.h"),
        kmpVipModel = "nn5.e",
        hideIdNames = listOf("hh", "book_container", "h8s", "inx", "ac5", "is7"),
        progressIdNames = emptyList(),

    )

    internal val CN_73232 = Names(
        profileId = "CN-7.3.2.32",
        packageName = CN_PACKAGE,
        versionName = "7.3.2.32",

        useLegacySeedIds = false,
        structuralFullscreenWatch = true,
        seriesToolbarProfile = "none",
        shortHolder = "dw4.t",
        holderBaseS1 = "dw4.i0",
        shortStateMethod = "S1",

        shortMaskMethod = "y3",
        shortControlsMethod = "Ea",
        shortConfigMethod = "j4",
        shortLayoutResetMethod = "l4",
        shortLandscapeMethod = "p4",
        shortMaskField = "c3",
        shortNativeClearField = "w3",
        shortCleanManagerField = "v3",
        homeFragmentMaskMethod = "o0",
        homeFragmentMaskField = "e",
        seriesFragmentRefreshMethod = "za",
        seriesPagerGetter = "Mg",
        seriesHolderGetter = "p2",
        seriesLayoutFields = listOf("A3", "i", "j", "q", "l", "m", "r", "z3"),
        fixedToolbarShowMethod = "T",
        customizeToolbarShowMethod = "Q",
        customizeToolbarApplyMethod = "S",
        toolbarBase = "gt7.c",
        progressBar = "qg4.t0",
        hideView1 = "ry1.e",
        hideView2 = "fw4.e",
        oledBright = "l83.h",
        oledBrightAction = "n83.a",
        topZoneTouch = "ev7.b",
        playbackState = "ns7.b",
        adVideoEndShowMethod = "I",
        pauseAdEntryClass = "com.dragon.read.component.shortvideo.impl.inject.view.j4",
        pauseAdEntryMethod = "b",
        resolutionController = "ov4.x",
        resolutionModelMethods = listOf("I", "L"),
        resolutionEngineField = "h",
        resolutionApplyMethod = "e0",
        doubleTapHandlers = listOf("bw4.j0", "yg4.e", "com.dragon.read.component.shortvideo.impl.fullscreen.d\$d"),
        doubleTapLikeView = "yg4.e",
        doubleTapHolderLikeMethod = "g4",

        rightViewAgency = "com.dragon.read.component.shortvideo.impl.inject.view.t6",
        rightViewAgencyEventMethod = "q",
        kmpAcctService = listOf("ec3.h"),
        kmpVipModel = "nn5.e",
        hideIdNames = listOf("hh", "book_container", "h8s", "inx", "ac5", "is7"),
        progressIdNames = emptyList(),
    )

    internal val CN_73318 = Names(
        profileId = "CN-7.3.3.18",
        packageName = CN_PACKAGE,
        versionName = "7.3.3.18",
        useLegacySeedIds = false,
        structuralFullscreenWatch = true,
        seriesToolbarProfile = "cn73318",
        shortHolder = "cy4.t",
        holderBaseS1 = "cy4.i0",
        shortStateMethod = "T1",
        shortMaskMethod = "N3",
        shortControlsMethod = "Qa",
        shortConfigMethod = "x4",
        shortLayoutResetMethod = "z4",
        shortLandscapeMethod = "B4",
        shortMaskField = "S2",
        shortNativeClearField = "n3",
        shortCleanManagerField = "m3",
        homeFragmentMaskMethod = "p0",
        homeFragmentMaskField = "e",
        seriesFragmentRefreshMethod = "Qa",

        seriesPagerGetter = "Tg",

        seriesHolderGetter = "t2",
        seriesLayoutFields = listOf("s3", "i", "j", "q", "l", "m", "r", "r3"),
        fixedToolbarShowMethod = "T",
        customizeToolbarShowMethod = "P",
        customizeToolbarApplyMethod = "R",
        toolbarBase = "hw7.c",
        progressBar = "bi4.h1",
        hideView1 = "",
        hideView2 = "fy4.e",
        oledBright = "x83.h",
        oledBrightAction = "z83.a",
        topZoneTouch = "fy7.b",
        playbackState = "ov7.b",
        adVideoEndShowMethod = "q",
        pauseAdEntryClass = "com.dragon.read.component.shortvideo.impl.inject.view.k4",
        pauseAdEntryMethod = "b",
        resolutionController = "nx4.w",
        resolutionModelMethods = listOf("f", "i"),
        resolutionEngineField = "h",
        resolutionApplyMethod = "q",

        doubleTapHandlers = listOf("ay4.g0", "ji4.e", "com.dragon.read.component.shortvideo.impl.fullscreen.f\$d"),
        doubleTapLikeView = "ji4.e",
        doubleTapHolderLikeMethod = "u4",
        rightViewAgency = "com.dragon.read.component.shortvideo.impl.inject.view.u6",
        rightViewAgencyEventMethod = "q",
        kmpAcctService = listOf(
            "rc3.h",
            "com.dragon.read.kmp.service.w",
            "com.dragon.read.kmp.service.n0",
        ),
        kmpVipModel = "wq5.e",

        hideIdNames = listOf("iu1", "fxu", "hs9"),

        progressIdNames = listOf("hox"),

        staticHideIds = listOf(0x7F1133AE, 0x7F112411, 0x7F112E0D),
        staticProgressIds = listOf(0x7F112D92),
        pauseRestoreIds = listOf(0x7F1133AE, 0x7F112411, 0x7F112E0D),
    )

    internal val OVERSEA_73132 = Names(
        profileId = "OVERSEA-7.3.1.32",
        packageName = OVERSEA_PACKAGE,
        versionName = "7.3.1.32",
        useLegacySeedIds = false,
        structuralFullscreenWatch = true,
        seriesToolbarProfile = "oversea73132",
        shortHolder = "nt4.t",
        holderBaseS1 = "nt4.i0",
        shortStateMethod = "d2",
        shortMaskMethod = "B3",
        shortControlsMethod = "Y9",
        shortConfigMethod = "k4",
        shortLayoutResetMethod = "m4",
        shortLandscapeMethod = "p4",
        shortMaskField = "g3",
        shortNativeClearField = "A3",
        shortCleanManagerField = "z3",
        homeFragmentMaskMethod = "",
        homeFragmentMaskField = "",
        seriesFragmentRefreshMethod = "Y9",
        seriesPagerGetter = "Tf",
        seriesHolderGetter = "r2",
        seriesLayoutFields = listOf("E3", "i", "j", "q", "l", "m", "r", "D3"),
        fixedToolbarShowMethod = "P",
        customizeToolbarShowMethod = "L",
        customizeToolbarApplyMethod = "N",
        toolbarBase = "ep7.c",
        progressBar = "ae4.t0",
        hideView1 = "",
        hideView2 = "pt4.e",
        oledBright = "x63.h",
        oledBrightAction = "z63.a",
        topZoneTouch = "mr7.b",
        playbackState = "lo7.b",
        adVideoEndShowMethod = "F",
        pauseAdEntryClass = "com.dragon.read.component.shortvideo.impl.inject.view.j4",
        pauseAdEntryMethod = "b",
        resolutionController = "ys4.x",
        resolutionModelMethods = listOf("K", "N"),
        resolutionEngineField = "h",

        resolutionApplyMethod = "",

        doubleTapHandlers = listOf("lt4.j0", "ie4.e", "com.dragon.read.component.shortvideo.impl.fullscreen.d\$d"),
        doubleTapLikeView = "ie4.e",
        doubleTapHolderLikeMethod = "h4",
        rightViewAgency = "com.dragon.read.component.shortvideo.impl.inject.view.u6",
        rightViewAgencyEventMethod = "o",
        kmpAcctService = listOf("com.dragon.read.kmp.service.l0", "s93.h"),
        kmpVipModel = "vk5.e",
        hideIdNames = listOf(
            "right_interact_container",
            "ly_tools_bar_icon",
            "series_info_panel_container",
            "top_header_constraint_layout",
            "bottom_container",
            "bottom_bar_container",
            // 2026-09-15 移出隐藏域（功能入口，非装饰；理由见 Hooks.seriesIdNames 注释）：
            //   short_series_catalog_view / more_operation_view / enter_episode_and_full_screen_container
            // 藏掉「选集面板 / 清晰度倍速菜单 / 进集入口」= 同时废掉选集与自动切集。
        ),
        progressIdNames = listOf("seek_bar_root"),

        // 2026-09-15 移出功能入口（非装饰；同 0a0be638）：0x7F0B2983 short_series_catalog_view、
        // 0x7F0B1FA3 more_operation_view、0x7F0B0FAE enter_episode_and_full_screen_container
        staticHideIds = listOf(0x7F0B2615, 0x7F0B1E9C, 0x7F0B28FA, 0x7F0B2F07, 0x7F0B05A6, 0x7F0B0597),
        staticProgressIds = listOf(0x7F0B2877),

        pauseRestoreIds = listOf(0x7F0B2615, 0x7F0B1E9C, 0x7F0B28FA, 0x7F0B2F07, 0x7F0B05A6, 0x7F0B0597),
    )

    internal val OVERSEA_73532 = Names(
        profileId = "OVERSEA-7.3.5.32",
        packageName = OVERSEA_PACKAGE,
        versionName = "7.3.5.32",
        useLegacySeedIds = false,
        structuralFullscreenWatch = true,
        seriesToolbarProfile = "oversea73132",
        shortHolder = "ro4.d",
        holderBaseS1 = "ro4.d",
        shortStateMethod = "m2",
        shortMaskMethod = "",
        shortControlsMethod = "",
        shortConfigMethod = "",
        shortLayoutResetMethod = "s2",
        shortLandscapeMethod = "",
        shortMaskField = "",
        shortNativeClearField = "",
        shortCleanManagerField = "",
        homeFragmentMaskMethod = "",
        homeFragmentMaskField = "",
        seriesFragmentRefreshMethod = "",
        seriesPagerGetter = "eg",
        seriesHolderGetter = "X0",
        seriesLayoutFields = listOf("i", "j", "k", "l", "m", "q", "r", "w3", "x3"),
        fixedToolbarShowMethod = "H",
        customizeToolbarShowMethod = "F",
        customizeToolbarApplyMethod = "G",
        toolbarBase = "com.dragon.read.video.layer.a",
        progressBar = "eh4.h1",
        hideView1 = "",
        hideView2 = "",
        oledBright = "",
        oledBrightAction = "",
        topZoneTouch = "",
        playbackState = "xy0.c",
        adVideoEndShowMethod = "handleVideoEvent",
        pauseAdEntryClass = "",
        pauseAdEntryMethod = "",
        resolutionController = "wj4.x",
        resolutionModelMethods = listOf("selectVideoInfoToPlay"),
        resolutionEngineField = "",
        resolutionApplyMethod = "",

        doubleTapHandlers = listOf("ro4.m", "ro4.k", "com.dragon.read.component.shortvideo.impl.fullscreen.f\$d"),
        rightViewAgency = "com.dragon.read.component.shortvideo.impl.inject.view.w6",
        rightViewAgencyEventMethod = "s",
        kmpAcctService = listOf("com.dragon.read.kmp.service.p0"),
        kmpVipModel = "sr5.e",
        hideIdNames = listOf(
            "right_interact_container",
            "ly_tools_bar_icon",
            "series_info_panel_container",
            "top_header_constraint_layout",
            "bottom_container",
            "bottom_bar_container",
            // 2026-09-15 移出隐藏域（功能入口，非装饰；理由见 Hooks.seriesIdNames 注释）：
            //   short_series_catalog_view / more_operation_view / enter_episode_and_full_screen_container
            // 藏掉「选集面板 / 清晰度倍速菜单 / 进集入口」= 同时废掉选集与自动切集。
        ),
        progressIdNames = listOf("seek_bar_root"),

        // 2026-09-15 移出功能入口（非装饰；同 0a0be638）：0x7F0B2A26 short_series_catalog_view、
        // 0x7F0B2007 more_operation_view、0x7F0B0FCD enter_episode_and_full_screen_container、
        // 0x7F0B0FCE enter_episode_btn
        staticHideIds = listOf(0x7F0B26A1, 0x7F0B1F00, 0x7F0B2998, 0x7F0B2FB6, 0x7F0B05A7, 0x7F0B0598),
        staticProgressIds = listOf(0x7F0B290E),

        pauseRestoreIds = listOf(0x7F0B26A1, 0x7F0B1F00, 0x7F0B2998, 0x7F0B2FB6, 0x7F0B05A7, 0x7F0B0598),

        seriesStaticIds = listOf(
            0x7F0B2998, 0x7F0B2FB6, 0x7F0B05A7, 0x7F0B0598,
            0x7F0B26A1, 0x7F0B1F00,
            0x7F0B1A24, 0x7F0B1A26, 0x7F0B2EF6, 0x7F0B2F01, 0x7F0B0BCF,
        ),
    )

    internal val OVERSEA_73732 = Names(
        profileId = "OVERSEA-7.3.7.32",
        packageName = OVERSEA_PACKAGE,
        versionName = "7.3.7.32",
        useLegacySeedIds = false,
        structuralFullscreenWatch = true,
        seriesToolbarProfile = "oversea73132",

        // 7.3.7.32 短剧 Holder：com.dragon.read.recyler.AbsRecyclerViewHolder 子类，
        // 且其内部类 i$d 实现 onDoubleTap(MotionEvent)。旧表的 ro4.d 在该版本已变成
        // 一个 URL 缓存类（父类 java.lang.Object），属「同名不同类」，必须替换。
        shortHolder = "com.dragon.read.component.shortvideo.impl.fullscreen.i",
        holderBaseS1 = "com.dragon.read.component.shortvideo.impl.fullscreen.i",

        // ── 7.3.7.32 短剧 Holder 内部方法/字段（本次逐个反编译确认，非沿用旧表）──
        // 依据：反编译 fullscreen.i（83 个方法）+ 用 aapt2 反查资源 ID 名称。
        //   shortStateMethod  = l2(pz4.f,int)V   —— i==2 分支走 "pause"，i==1 走 playing，
        //                        与旧表 S1/m2 的「播放状态回调」角色一致（原表名已随发版失效）。
        //   shortControlsMethod = sd(bool,bool)V —— 内部走 f.m(!z)/f.q(!z,z2)，
        //                        而 f.m(z) 里是 setImmersiveMode(!z)，故 sd(false,*) = 显示控件、
        //                        sd(true,*) = 进沉浸（清屏）。与旧表 Y9 的调用约定 (false,true)=显示
        //                        完全一致，因此 Hooks 里「arg0=false 就改成 true」的强制清屏逻辑成立。
        //   shortCleanManagerField = K3(pr4.d)   —— pr4.d 是锁屏/清屏控件（含 unlock_speed 动画、
        //                        getLockStatus()），有 b(bool) 显示 / a(bool) 隐藏，对应 Hooks 里
        //                        「退出清屏时调 b(false)」。
        //   shortMaskField = e3                  —— findViewById 传入的资源 ID 经 aapt2 反查为
        //                        id/mask_view（layout_full_screen_item 里的全屏 View）。注意它的可见性
        //                        由 U1(bool,bool) 控制，语义偏「弹窗内容遮罩」而非清屏遮罩，实战仅作兜底。
        // 仍未定位（保持留空 = 安全跳过，不猜）：
        //   shortMaskMethod —— 该版本全类没有「单参 bool 且操作 mask」的方法（mask 只被双参 U1 控制），
        //                      hook 任何单参方法都是误伤，故留空；mask 同步改由周期扫描承担。
        //   shortConfigMethod —— 整条继承链（fullscreen.i → e05.l0 → e05.a → ok4.a →
        //                      AbsRecyclerViewHolder）上都没有 (Configuration)V 方法，属结构性移除。
        shortStateMethod = "l2",
        shortMaskMethod = "",
        shortControlsMethod = "sd",
        shortConfigMethod = "",
        shortLayoutResetMethod = "s2",
        shortLandscapeMethod = "",
        shortMaskField = "e3",
        shortNativeClearField = "",
        shortCleanManagerField = "K3",
        homeFragmentMaskMethod = "",
        homeFragmentMaskField = "",
        seriesFragmentRefreshMethod = "",
        seriesPagerGetter = "eg",
        seriesHolderGetter = "X0",
        seriesLayoutFields = listOf("i", "j", "k", "l", "m", "q", "r", "w3", "x3"),
        fixedToolbarShowMethod = "H",
        customizeToolbarShowMethod = "F",
        customizeToolbarApplyMethod = "G",
        toolbarBase = "com.dragon.read.video.layer.a",

        progressBar = "",
        hideView1 = "",
        hideView2 = "",

        // OLED 亮度拦截：**该版本无法静态适配，保持留空（hook 侧会安全跳过）**。
        // 依据：7.3.1.32 的 l83.h(有 a(List)V) / n83.a 在 7.3.7.32 已无等价物 ——
        // 全 APK 中 OLED 相关只剩 com.dragon.read.base.framework.oled.* 四个纯枚举
        // （OledRuntimeChangeReason / model.OledArea / model.OledScene / model.OledStrategyType），
        // 而执行体（OledBurnInManager、OledBurnInAndroidEntry、oled.config.BrightnessConfig、
        // OledDeviceModelConfig、oled_brightness_config、android_oled_view_target …）只以
        // **字符串**形式出现，说明已下沉到动态模块/线上配置，APK 内没有可 hook 的类。
        oledBright = "",
        oledBrightAction = "",

        topZoneTouch = "",

        // 短剧播放器 / 播放状态回调：同时声明 onPlaybackStateChanged(TTVideoEngine,int)
        // 与 onVideoStreamBitrateChanged(Resolution,int)，以及 setPlaySpeed(int)、
        // getResolution()、V()Resolution[]，是旧 wj4.x / xy0.c 的对应类。
        playbackState = "pz4.w",

        adVideoEndShowMethod = "handleVideoEvent",

        // 暂停广告入口：类名随发版按字母递增（7.3.1.32=j4 → 7.3.3.18=k4 → 本版=l4），
        // 且 `b()` 的「无参、返回对象」形态跨版本保持。l4 确认是暂停广告视图助手：
        // b() 返回 fn4.s（广告 ViewHolder）、a() 取 getAdViewHolder()、c(bool) 打 "hideAd" 日志；
        // 并被 FullScreenViewInjectAgency 的 `pauseAdViewHelper` 懒加载构造，
        // 同文件另有 enablePauseAd / start pauseAd / requestAd 调用链。
        pauseAdEntryClass = "com.dragon.read.component.shortvideo.impl.inject.view.l4",
        pauseAdEntryMethod = "b",

        // 切清晰度的**原生入口**（7.3.7.32 补回；7.3.5.32 起该字段丢失，导致退化成
        // 直接捅引擎 TTVideoEngine.configResolution）。jadx 证实 pz4.w.S(Resolution)：
        //   ① `this.f367705b = resolution` 写宿主自己的清晰度状态（UI/菜单/按钮读它）
        //   ② `tTVideoEngine.configResolution(resolution)` 再下发给引擎
        //   ③ `hp4.q.f302741f = true` 打上「用户已配置清晰度」标记
        // 只做 ② 不做 ①，就会出现「引擎播 1080p、按钮显示 720P」的状态分叉。
        resolutionController = "pz4.w",
        resolutionModelMethods = listOf("M", "O", "P"),
        resolutionEngineField = "",
        resolutionApplyMethod = "S",
        resolutionUserSelectField = "c",

        // 旧表 fullscreen.f$d / d$d 已不再实现 onDoubleTap；该版本为 fullscreen.i$d。
        doubleTapHandlers = listOf("com.dragon.read.component.shortvideo.impl.fullscreen.i\$d"),
        doubleTapLikeView = "",
        doubleTapHolderLikeMethod = "",

        rightViewAgency = "com.dragon.read.component.shortvideo.impl.inject.view.w6",
        rightViewAgencyEventMethod = "s",
        kmpAcctService = listOf("com.dragon.read.kmp.service.p0"),
        kmpVipModel = "sr5.e",

        hideIdNames = listOf(
            "right_interact_container",
            "ly_tools_bar_icon",
            "series_info_panel_container",
            "top_header_constraint_layout",
            "bottom_container",
            "bottom_bar_container",
            // 2026-09-15 移出隐藏域（功能入口，非装饰；理由见 Hooks.seriesIdNames 注释）：
            //   short_series_catalog_view / more_operation_view / enter_episode_and_full_screen_container
            // 藏掉「选集面板 / 清晰度倍速菜单 / 进集入口」= 同时废掉选集与自动切集。
        ),
        progressIdNames = listOf("seek_bar_root"),

        // 下表 ID 取自 7.3.7.32 真机运行时按名解析结果（旧表硬编码值在本版本已整体错位
        // +10~+58）。名称解析仍是主路径，此处仅作为视图树尚未建立时的预置兜底。
        // 2026-09-15 移出隐藏域（功能入口，非装饰）：0x7F0B2A5C short_series_catalog_view（选集面板）、
        // 0x7F0B2030 more_operation_view（清晰度/倍速菜单）、0x7F0B0FFA enter_episode_and_full_screen_container。
        // 这三个是「隐藏控件」开关的越界项（其职责只是顶部/底部导航、作品信息、右侧互动）。
        staticHideIds = listOf(0x7F0B26D5, 0x7F0B1F28, 0x7F0B29CE, 0x7F0B2FF0, 0x7F0B05B1, 0x7F0B05A2),
        staticProgressIds = listOf(0x7F0B2943),

        // 暂停恢复域同步收窄：选集面板不再被隐藏，自然无需在暂停时「恢复」它。
        pauseRestoreIds = listOf(0x7F0B26D5, 0x7F0B1F28, 0x7F0B29CE, 0x7F0B2FF0, 0x7F0B05B1, 0x7F0B05A2),

        // 2026-09-15 再移出四项功能入口（选集面板/清晰度菜单/观看全集/进集容器）。
        // 「选集相关功能」开关的正解是 hideSeriesToolbarView 的几何判定（seriesToolbarKind，
        // 顶部 9 子项 40~48dp、底部 3 子项 36~44dp 的纯装饰条），ID 表只是冗余兜底；
        // 兜底里混进功能入口 ⇒ 开关一开就把选集与自动切集一起废掉。
        seriesStaticIds = listOf(
            0x7F0B29CE, 0x7F0B2FF0, 0x7F0B05B1, 0x7F0B05A2,
            0x7F0B26D5, 0x7F0B1F28, 0x7F0B04B0, 0x7F0B1A52, 0x7F0B1A54, 0x7F0B0BDE, 0x7F0B2F3B, 0x7F0B2F30,
        ),

        percentPlayerCandidates = listOf("pz4.w", "pz4.f", "ov4.x", "ys4.x", "nx4.w"),
        speedControllerCandidates = listOf(
            "com.dragon.read.component.shortvideo.impl.v2.view.adapter.a",
            "ak4.d",
            "lt4.v",
            "bw4.v",
        ),
        speedControllerSetMethod = "C2",
        speedControllerCacheMethod = "E1",
        floatPlayerCandidates = listOf("com.dragon.read.component.shortvideo.impl.autoplay.o"),
    )

    internal val CN: Names get() = CN_73132
    internal val OVERSEA: Names get() = OVERSEA_73132

    /** 指纹检测报告：命中/未命中的「字段名=类名」明细 */
    class ProbeReport(
        val score: Int,
        val total: Int,
        val hits: List<String>,
        val misses: List<String>,
    ) {
        val ratio: Float get() = if (total == 0) 0f else score.toFloat() / total

        /**
         * 命中过半才认为该档案可信。
         * 原实现是「命中 > 0」——只要一个类名还活着就认表，会被 R8 的
         * 「同名不同类」骗过（7.3.7.32 的 ro4.d 就是这种：名字在，语义已变），
         * 因此门槛提高到半数以上。
         */
        val confident: Boolean get() = total > 0 && score * 2 >= total

        /** 便于日志输出的紧凑摘要 */
        fun summary(): String = "$score/$total(${(ratio * 100).toInt()}%)"
    }

    /**
     * 档案里所有「字段名 → 类名」项。
     * 既用于跨版本指纹打分，也用于向日志输出**具体哪些字段已失效**。
     * 只收可 Class.forName 校验的类名字段；纯混淆方法名/字段名无法校验，不参与。
     */
    fun probeEntries(n: Names): List<Pair<String, String>> = buildList {
        add("shortHolder" to n.shortHolder)
        add("holderBaseS1" to n.holderBaseS1)
        add("toolbarBase" to n.toolbarBase)
        add("progressBar" to n.progressBar)
        add("hideView1" to n.hideView1)
        add("hideView2" to n.hideView2)
        add("oledBright" to n.oledBright)
        add("oledBrightAction" to n.oledBrightAction)
        add("topZoneTouch" to n.topZoneTouch)
        add("playbackState" to n.playbackState)
        add("pauseAdEntryClass" to n.pauseAdEntryClass)
        add("resolutionController" to n.resolutionController)
        add("rightViewAgency" to n.rightViewAgency)
        add("kmpVipModel" to n.kmpVipModel)
        n.doubleTapHandlers.forEach { add("doubleTapHandlers" to it) }
        n.kmpAcctService.forEach { add("kmpAcctService" to it) }
        n.percentPlayerCandidates.forEach { add("percentPlayerCandidates" to it) }
        n.speedControllerCandidates.forEach { add("speedControllerCandidates" to it) }
        n.floatPlayerCandidates.forEach { add("floatPlayerCandidates" to it) }
    }.filter { it.second.isNotBlank() }

    /** 逐个校验档案中的类名字段是否真实存在，返回明细报告 */
    fun probe(n: Names, classLoader: ClassLoader?): ProbeReport {
        val entries = probeEntries(n)
        if (classLoader == null) {
            return ProbeReport(0, entries.size, emptyList(), entries.map { it.first })
        }
        val cache = HashMap<String, Boolean>()
        val hits = ArrayList<String>()
        val misses = ArrayList<String>()
        entries.forEach { (field, cls) ->
            val ok = cache.getOrPut(cls) {
                try {
                    Class.forName(cls, false, classLoader)
                    true
                } catch (_: Throwable) {
                    false
                }
            }
            if (ok) hits += "$field=$cls" else misses += "$field=$cls"
        }
        return ProbeReport(hits.size, entries.size, hits, misses)
    }

    fun probeScore(n: Names, classLoader: ClassLoader?): Int = probe(n, classLoader).score

    /**
     * 取指纹可信度最高的档案。
     * 不同档案的探针**总数不同**（新表含候选类列表，旧表没有），
     * 因此按命中率而非绝对命中数比较，否则新表会被系统性高估。
     */
    fun bestByFingerprint(candidates: List<Names>, classLoader: ClassLoader?): Names {
        if (classLoader == null) return candidates.first()
        var best = candidates.first()
        var bestRatio = -1f
        var bestScore = -1
        candidates.forEach { n ->
            val r = probe(n, classLoader)
            if (r.ratio > bestRatio || (r.ratio == bestRatio && r.score > bestScore)) {
                bestRatio = r.ratio
                bestScore = r.score
                best = n
            }
        }
        return best
    }

    /** 解析结果：档案 + 指纹报告 + 是否版本号精确命中 */
    class Resolution(
        val names: Names,
        val probe: ProbeReport,
        val exactVersion: Boolean,
    ) {
        /** 版本号未精确命中，或命中的表自身指纹已不达标 */
        val degraded: Boolean get() = !exactVersion || !probe.confident
    }

    /**
     * 解析目标档案。相比直接比对版本号的旧实现，这里增加了：
     *  - 版本号命中后仍校验该表指纹可信度（防止「版本号对、表已被打散」的半残状态）
     *  - 返回指纹明细，供调用方打印失效字段清单
     */
    fun resolve(pkg: String, versionName: String?, classLoader: ClassLoader? = null): Resolution {
        if (pkg != CN_PACKAGE) {
            if (pkg != OVERSEA_PACKAGE) {
                return Resolution(CN_73132, ProbeReport(0, 0, emptyList(), emptyList()), false)
            }

            val v = versionName?.trim()?.substringBefore(' ') ?: ""
            val exact = when (v) {
                "7.3.7.32" -> OVERSEA_73732
                "7.3.5.32" -> OVERSEA_73532
                "7.3.1.32" -> OVERSEA_73132
                else -> null
            }
            return pick(exact, listOf(OVERSEA_73732, OVERSEA_73532, OVERSEA_73132), OVERSEA_73732, classLoader)
        }

        val v = versionName?.trim()?.substringBefore(' ') ?: ""
        val exact = when (v) {
            "7.3.3.18" -> CN_73318
            "7.3.2.32" -> CN_73232
            "7.3.1.32" -> CN_73132
            else -> null
        }
        return pick(exact, listOf(CN_73318, CN_73232, CN_73132), CN_73318, classLoader)
    }

    private fun pick(
        exact: Names?,
        tables: List<Names>,
        fallback: Names,
        classLoader: ClassLoader?,
    ): Resolution {
        if (exact == null) {
            if (classLoader == null) return Resolution(fallback, ProbeReport(0, 0, emptyList(), emptyList()), false)
            val best = bestByFingerprint(tables, classLoader)
            return Resolution(best, probe(best, classLoader), false)
        }
        val report = probe(exact, classLoader)
        if (classLoader == null || report.confident) return Resolution(exact, report, true)

        // 版本号对得上，但表里过半类名已失效（新版混淆重排）。
        // 只有在另一张表**明显**更匹配时才切换，否则保留本表——版本号本身是权威信息。
        val alt = bestByFingerprint(tables, classLoader)
        if (alt === exact) return Resolution(exact, report, false)
        val altReport = probe(alt, classLoader)
        return if (altReport.ratio >= report.ratio + 0.25f) {
            Resolution(alt, altReport, false)
        } else {
            Resolution(exact, report, false)
        }
    }

    fun namesFor(pkg: String, versionName: String?, classLoader: ClassLoader? = null): Names =
        resolve(pkg, versionName, classLoader).names

    fun isSupported(pkg: String, versionName: String?): Boolean {
        val v = versionName?.trim()?.substringBefore(' ') ?: return false
        return when (pkg) {
            CN_PACKAGE -> v in SUPPORTED_CN_VERSIONS
            OVERSEA_PACKAGE -> v in SUPPORTED_OVERSEA_VERSIONS
            else -> false
        }
    }
}

package xyz.kejiyu.hongguo.hooks

import android.app.Activity
import android.app.Dialog
import android.app.NotificationChannel
import android.app.NotificationManager
import android.app.PendingIntent
import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent
import android.content.IntentFilter
import android.content.SharedPreferences
import android.content.res.ColorStateList
import android.graphics.Rect
import android.graphics.Typeface
import android.graphics.drawable.GradientDrawable
import android.os.Build
import android.os.Handler
import android.os.Looper
import android.view.Gravity
import android.view.View
import android.view.ViewGroup
import android.view.WindowInsets
import android.graphics.Color
import android.view.WindowManager
import android.view.WindowInsetsController
import android.widget.*
import xyz.kejiyu.hongguo.LogUtil
import xyz.kejiyu.hongguo.MainHook
import xyz.kejiyu.hongguo.BuildConfig
import xyz.kejiyu.hongguo.UpdateChecker
import io.github.libxposed.api.XposedInterface
import io.github.libxposed.api.XposedInterface.Hooker
import java.io.File

object Hooks {

    private var gPrefs: SharedPreferences? = null

    private var gMasterOn = false
    private var gStatusOn = false
    private var gControlOn = false
    private var gPlayerOn = false
    private var gAdOn = false
    private var gRefreshOff = false
    private var gTopZoneOn = false
    private var gNavBarOff = false
    private var gProgressOff = false
    private var gRestoreControlsOnPause = true
    private var gVipOn = false
    private var gVipIconOn = false
    private var gMaxQualityOn = false

    private var gDefaultSpeedOn = false
    private var gDefaultSpeed = 1.0f
    private const val DEFAULT_SPEED_PREF = "default_speed"
    private const val DEFAULT_SPEED_VALUE_PREF = "default_speed_value"
    private val DEFAULT_SPEED_OPTIONS = floatArrayOf(0.75f, 1.0f, 1.25f, 1.5f, 1.75f, 2.0f, 3.0f)
    private var gDoubleTapCommentOn = false

    private var gOledProtectOn = false

    private var gNotificationMenuOn = false

    private var gDownloadLimitUnlimitOn = false
    private const val DOWNLOAD_LIMIT_PREF = "download_limit_unlimit"
    private const val DOWNLOAD_LIMIT_EPISODE_PREF = "download_limit_one_day_episode"
    private const val DOWNLOAD_LIMIT_SERIES_PREF = "download_limit_one_day_series"
    private const val DOWNLOAD_LIMIT_TOTAL_PREF = "download_limit_total_series"
    private const val DOWNLOAD_LIMIT_AB_KEY = "video_download_limit_expansion_v711"
    private const val DOWNLOAD_LIMIT_DEFAULT_VALUE = 99999
    private var gDownloadOneDayEpisode = DOWNLOAD_LIMIT_DEFAULT_VALUE
    private var gDownloadOneDaySeries = DOWNLOAD_LIMIT_DEFAULT_VALUE
    private var gDownloadTotalSeries = DOWNLOAD_LIMIT_DEFAULT_VALUE
    @Volatile private var gVideoPaused = false

    private val gEngineMaxResolution = java.util.Collections.synchronizedMap(
        java.util.WeakHashMap<Any, Any>()
    )

    private val gControllerMaxResolution = java.util.Collections.synchronizedMap(
        java.util.WeakHashMap<Any, Any>()
    )

    private val gKnownPercentSpeedPlayers = java.util.Collections.synchronizedMap(
        java.util.WeakHashMap<Any, Boolean>()
    )
    private val gKnownFloatSpeedPlayers = java.util.Collections.synchronizedMap(
        java.util.WeakHashMap<Any, Boolean>()
    )

    private val gRightViewAgencies = mutableListOf<java.lang.ref.WeakReference<Any>>()
    @Volatile private var gLastDoubleTapCommentAt = 0L

    @Volatile private var gSuppressDoubleTapLikeUntil = 0L

    private val gForcedShortMaskVisibility = java.util.Collections.synchronizedMap(
        java.util.WeakHashMap<View, Int>()
    )

    private data class SavedViewState(var visibility: Int, val alpha: Float)
    private val gSavedViewStates = java.util.Collections.synchronizedMap(
        java.util.WeakHashMap<View, SavedViewState>()
    )

    private data class PauseForcedState(
        val visibility: Int,
        val alpha: Float,
        val translationX: Float,
        val translationY: Float,
    )
    private val gPauseForcedStates = java.util.Collections.synchronizedMap(
        java.util.WeakHashMap<View, PauseForcedState>()
    )

    private val gCleanMaskViews = java.util.Collections.synchronizedMap(
        java.util.WeakHashMap<View, Boolean>()
    )
    private val gInternalViewMutation = object : ThreadLocal<Boolean>() {
        override fun initialValue(): Boolean = false
    }

    private val gInsideFeedBottomMarginWrite = object : ThreadLocal<Boolean>() {
        override fun initialValue(): Boolean = false
    }

    private val gSavedTopMargins = java.util.Collections.synchronizedMap(
        java.util.WeakHashMap<View, Int>()
    )

    private data class SavedBottomLayoutState(
        val width: Int,
        val height: Int,
        val paddingLeft: Int,
        val paddingTop: Int,
        val paddingRight: Int,
        val paddingBottom: Int,
        val marginLeft: Int?,
        val marginTop: Int?,
        val marginRight: Int?,
        val marginBottom: Int?,
        val bottomToTop: Int?,
        val bottomToBottom: Int?,
        val goneBottomMargin: Int?,
    )
    private val gSavedBottomLayoutStates = java.util.Collections.synchronizedMap(
        java.util.WeakHashMap<View, SavedBottomLayoutState>()
    )

    private val gSeriesGuardedViews = java.util.Collections.synchronizedMap(
        java.util.WeakHashMap<View, Boolean>()
    )

    private val gKnownSeriesToolbars = java.util.Collections.synchronizedMap(
        java.util.WeakHashMap<View, Int>()
    )

    private val gKnownRefreshAccessoryViews = java.util.Collections.synchronizedMap(
        java.util.WeakHashMap<View, Boolean>()
    )

    private val gKnownMainBottomNavViews = java.util.Collections.synchronizedMap(
        java.util.WeakHashMap<View, Boolean>()
    )

    private val gKnownFullSeriesEntryViews = java.util.Collections.synchronizedMap(
        java.util.WeakHashMap<View, Boolean>()
    )

    private val gKnownBottomBackdropViews = java.util.Collections.synchronizedMap(
        java.util.WeakHashMap<View, Boolean>()
    )

    private val gSavedNativeNavBarColors = java.util.Collections.synchronizedMap(
        java.util.WeakHashMap<Activity, Int>()
    )

    private val gFeedViewportNativeBottomMargins = java.util.Collections.synchronizedMap(
        java.util.WeakHashMap<View, Int>()
    )
    private val gKnownFeedViewportViews = java.util.Collections.synchronizedMap(
        java.util.WeakHashMap<View, Boolean>()
    )

    private val gVideoToolbarLayers = mutableListOf<java.lang.ref.WeakReference<Any>>()
    private val gToolbarBaseLayers = mutableListOf<java.lang.ref.WeakReference<Any>>()
    private data class ShortVideoHolderState(
        val ref: java.lang.ref.WeakReference<Any>,
        var playbackState: Int,
        var updatedAt: Long,
    )
    private val gShortVideoHolders = mutableListOf<ShortVideoHolderState>()
    private val gShortSeriesFragments =
        mutableListOf<java.lang.ref.WeakReference<Any>>()
    private val gCnHomeFragments =
        mutableListOf<java.lang.ref.WeakReference<Any>>()
    private var gLastVideoStateAt = 0L
    private var gLastVideoStateReason = "init"
    private var gLastWindowedMode: Boolean? = null

    private var gReceiverRegistered = false
    private var gCurrentActivity: Activity? = null
    @Volatile private var gResumedShortSeriesFragment: java.lang.ref.WeakReference<Any>? = null

    private val seriesDetailActivityNames = setOf(
        "com.dragon.read.component.shortvideo.impl.ShortSeriesActivity",
        "com.dragon.read.component.shortvideo.impl.seriesdetail.ShortSeriesDetailActivity",
        "com.dragon.read.component.shortvideo.impl.albumdetail.VideoAlbumDetailActivity",
        "com.dragon.read.component.shortvideo.impl.fullscreen.ShortSeriesLandActivity",
    )

    private fun isLandscapeFullscreenActivity(act: Activity?): Boolean {
        if (act == null) return false
        val name = act.javaClass.name
        if (name == "com.dragon.read.component.shortvideo.impl.fullscreen.ShortSeriesLandActivity" ||
            name.contains(".fullscreen.") ||
            name.endsWith("LandActivity")
        ) return true
        val orientation = try { act.resources?.configuration?.orientation } catch (_: Throwable) { null }
        return orientation == android.content.res.Configuration.ORIENTATION_LANDSCAPE
    }

    private fun isSeriesDetailActivity(act: Activity?): Boolean {
        if (act == null) return false
        return act.javaClass.name in seriesDetailActivityNames
    }

    private fun shouldApplyUiHiding(): Boolean {
        val name = gCurrentActivity?.javaClass?.name ?: return false
        return if (name == "com.dragon.read.component.shortvideo.impl.ShortSeriesActivity") {
            gResumedShortSeriesFragment?.get() != null
        } else name in seriesDetailActivityNames
    }

    private val gTargetIdSet = mutableSetOf<Int>()
    private val gSeriesTargetIdSet = mutableSetOf<Int>()
    private val gProgressIdSet = mutableSetOf<Int>()
    private val gPauseRestoreIdSet = mutableSetOf<Int>()

    private val seriesIdNames = listOf(
        "series_info_panel_container", "top_header_constraint_layout",
        "bottom_container", "bottom_bar_container", "short_series_catalog_view", "more_operation_view",
        "enter_episode_and_full_screen_container", "enter_episode_btn",
        "iu1", "hs9", "book_container", "is7",
        "kmp_compose_all_container", "kmp_compose_view", "compose_title",
        "title_bar_panel", "title_bar_container",
    )
    private val staticSeriesIds = setOf(
        0x7F0B28FA, 0x7F0B2F07,
        0x7F0B05A6, 0x7F0B0597, 0x7F0B2983, 0x7F0B1FA3,
        0x7F0B0FAE, 0x7F0B0FAF,
        0x7F1133AE, 0x7F112E0D,
        0x7F0B19E4, 0x7F0B19E6, 0x7F0B0BB5,
        0x7F0B2E54, 0x7F0B2E49,
    )

    private val gModuleUiRoots = java.util.concurrent.ConcurrentHashMap<View, Boolean>()

    private fun markAsModuleUi(v: View?) {
        if (v == null) return
        gModuleUiRoots[v] = true
    }

    private fun isInsideModuleUi(v: View?): Boolean {
        if (v == null) return false
        if (gModuleUiRoots.isEmpty()) {
            if (v.tag == gKejiyuBtnTag || v.tag == gKejiyuSettingsWrapperTag) return true
            return false
        }
        var node: View? = v
        var depth = 0
        while (node != null && depth++ < 6) {
            if (node.tag == gKejiyuBtnTag || node.tag == gKejiyuSettingsWrapperTag) return true
            if (gModuleUiRoots.containsKey(node)) return true
            node = node.parent as? View
        }
        return false
    }

    @Volatile private var gHideClassesSet: Set<String> = emptySet()
    @Volatile private var gNames: TargetNames.Names = TargetNames.CN
        set(value) {
            field = value
            gHideClassesSet = listOf(value.hideView1, value.hideView2).filter { it.isNotBlank() }.toSet()
        }
    @Volatile private var gPkg = TargetNames.CN_PACKAGE
    @Volatile private var gTargetVersionName = "未知"
    @Volatile private var gTargetVersionCode = -1L

    private val mainHandler = Handler(Looper.getMainLooper())
    private var appCtx: Context? = null
    private var gIsNight = false
    private var gSettingsActivity: Activity? = null
    private val gKejiyuBtnTag = "KEJIYU_BTN_TAG"
    private val gKejiyuSettingsWrapperTag = "KEJIYU_SETTINGS_WRAPPER_TAG"
    private const val SETTINGS_ENTRY_TITLE = "模块设置"

    private var gTopZoneTracking = false
    private var gTopZoneActive = false
    private var gTopZoneStartY = 0f
    private const val TOP_ZONE_HEIGHT = 250

    private val seedIds = intArrayOf(
        0x7F0B03E3, 0x7F0B071E, 0x7F0B0412,
        0x7F0B0BCB, 0x7F0A00D2, 0x7F0B0C6A,
    )

    private fun initPrefs(ctx: Context) {
        if (gPrefs == null) {
            gPrefs = ctx.getSharedPreferences("lspilot_kejiyu", 0)

            gMasterOn = gPrefs!!.getBoolean("master_on", false)
            gStatusOn = gPrefs!!.getBoolean("status_bar", false)
            gControlOn = gPrefs!!.getBoolean("control_hide", false)
            gPlayerOn = gPrefs!!.getBoolean("player_bar", false)
            gAdOn = gPrefs!!.getBoolean("ad_block", false)
            gRefreshOff = gPrefs!!.getBoolean("pull_refresh", false)
            gTopZoneOn = gPrefs!!.getBoolean("top_zone", false)
            gNavBarOff = gPrefs!!.getBoolean("nav_bar_off", false)
            gProgressOff = gPrefs!!.getBoolean("progress_off", false)
            gRestoreControlsOnPause = gPrefs!!.getBoolean("restore_controls_pause", true)
            gVipOn = gPrefs!!.getBoolean("vip_unlock", false)
            gVipIconOn = gPrefs!!.getBoolean("vip_icon", false)
            gMaxQualityOn = gPrefs!!.getBoolean("max_quality", false)
            gDefaultSpeedOn = gPrefs!!.getBoolean(DEFAULT_SPEED_PREF, false)
            gDefaultSpeed = normalizeDefaultSpeed(gPrefs!!.getFloat(DEFAULT_SPEED_VALUE_PREF, 1.0f))
            gDoubleTapCommentOn = gPrefs!!.getBoolean("double_tap_comment", false)
            gOledProtectOn = gPrefs!!.getBoolean("oled_protect", false)
            gNotificationMenuOn = gPrefs!!.getBoolean("notification_menu", false)
            gDownloadLimitUnlimitOn = gPrefs!!.getBoolean(DOWNLOAD_LIMIT_PREF, false)
            gDownloadOneDayEpisode = gPrefs!!.getInt(DOWNLOAD_LIMIT_EPISODE_PREF, DOWNLOAD_LIMIT_DEFAULT_VALUE)
            gDownloadOneDaySeries = gPrefs!!.getInt(DOWNLOAD_LIMIT_SERIES_PREF, DOWNLOAD_LIMIT_DEFAULT_VALUE)
            gDownloadTotalSeries = gPrefs!!.getInt(DOWNLOAD_LIMIT_TOTAL_PREF, DOWNLOAD_LIMIT_DEFAULT_VALUE)
        }
    }
    private fun savePref(key: String, value: Boolean) {
        val editor = gPrefs?.edit()?.putBoolean(key, value) ?: return

        if (key == DOWNLOAD_LIMIT_PREF || key == "master_on") editor.commit()
        else editor.apply()
    }

    private fun normalizeDefaultSpeed(value: Float): Float {
        if (!value.isFinite()) return 1.0f
        return DEFAULT_SPEED_OPTIONS.minByOrNull { kotlin.math.abs(it - value) } ?: 1.0f
    }

    private fun defaultSpeedPercent(): Int = (normalizeDefaultSpeed(gDefaultSpeed) * 100f + 0.5f).toInt()

    private fun formatSpeed(value: Float): String {
        val v = normalizeDefaultSpeed(value)
        return if (kotlin.math.abs(v - v.toInt().toFloat()) < 0.001f) "${v.toInt()}.0x"
        else ("%.2fx".format(java.util.Locale.US, v)).replace("0x", "x")
    }

    private fun defaultSpeedSummary(): String = "当前 ${formatSpeed(gDefaultSpeed)}"

    private fun saveDefaultSpeedValue(value: Float) {
        gDefaultSpeed = normalizeDefaultSpeed(value)
        gPrefs?.edit()?.putFloat(DEFAULT_SPEED_VALUE_PREF, gDefaultSpeed)?.apply()
    }

    private fun defaultSpeedEnabledNow(): Boolean = gMasterOn && gDefaultSpeedOn

    private fun weakPlayerSnapshot(map: MutableMap<Any, Boolean>): List<Any> {
        return try { synchronized(map) { map.keys.toList() } } catch (_: Throwable) { emptyList() }
    }

    private fun applySpeedToPercentPlayer(player: Any, reason: String): Boolean {
        if (!defaultSpeedEnabledNow()) return false
        return try {
            val method = player.javaClass.getMethod("setPlaySpeed", Integer.TYPE)
            method.invoke(player, defaultSpeedPercent())
            LogUtil.incr("defaultSpeedImmediatePercent")
            true
        } catch (_: Throwable) {
            try {
                val method = player.javaClass.getDeclaredMethod("setPlaySpeed", Integer.TYPE).apply { isAccessible = true }
                method.invoke(player, defaultSpeedPercent())
                LogUtil.incr("defaultSpeedImmediatePercent")
                true
            } catch (e: Throwable) {
                LogUtil.debug("默认倍速 percent 应用失败[$reason]: ${e.javaClass.simpleName}: ${e.message}")
                false
            }
        }
    }

    private fun applySpeedToFloatPlayer(player: Any, reason: String): Boolean {
        if (!defaultSpeedEnabledNow()) return false
        return try {
            val method = player.javaClass.getMethod("setSpeed", java.lang.Float.TYPE)
            method.invoke(player, gDefaultSpeed)
            LogUtil.incr("defaultSpeedImmediateFloat")
            true
        } catch (_: Throwable) {
            try {
                val method = player.javaClass.getDeclaredMethod("setSpeed", java.lang.Float.TYPE).apply { isAccessible = true }
                method.invoke(player, gDefaultSpeed)
                LogUtil.incr("defaultSpeedImmediateFloat")
                true
            } catch (e: Throwable) {
                LogUtil.debug("默认倍速 float 应用失败[$reason]: ${e.javaClass.simpleName}: ${e.message}")
                false
            }
        }
    }

    private fun applyDefaultSpeedNow(reason: String) {
        if (!defaultSpeedEnabledNow()) return
        val run = Runnable {
            var applied = 0
            weakPlayerSnapshot(gKnownPercentSpeedPlayers).forEach { if (applySpeedToPercentPlayer(it, reason)) applied++ }
            weakPlayerSnapshot(gKnownFloatSpeedPlayers).forEach { if (applySpeedToFloatPlayer(it, reason)) applied++ }
            LogUtil.info("默认倍速立即应用 | speed=${formatSpeed(gDefaultSpeed)} | players=$applied | reason=$reason")
        }
        if (Looper.myLooper() == Looper.getMainLooper()) run.run() else mainHandler.post(run)
    }

    private fun scheduleDefaultSpeedApply(reason: String) {
        if (!defaultSpeedEnabledNow()) return
        mainHandler.post { applyDefaultSpeedNow("$reason/0") }
        mainHandler.postDelayed({ applyDefaultSpeedNow("$reason/120") }, 120L)
        mainHandler.postDelayed({ applyDefaultSpeedNow("$reason/420") }, 420L)
    }

    private fun saveDownloadLimitValues(episode: Int, series: Int, total: Int) {
        gDownloadOneDayEpisode = episode
        gDownloadOneDaySeries = series
        gDownloadTotalSeries = total
        gPrefs?.edit()
            ?.putInt(DOWNLOAD_LIMIT_EPISODE_PREF, episode)
            ?.putInt(DOWNLOAD_LIMIT_SERIES_PREF, series)
            ?.putInt(DOWNLOAD_LIMIT_TOTAL_PREF, total)
            ?.commit()
    }

    private fun readBooleanPrefFromDisk(key: String, defaultValue: Boolean = false): Boolean {
        try {
            val dataDir = try {
                appCtx?.applicationInfo?.dataDir
            } catch (_: Throwable) { null } ?: "/data/user/0/$gPkg"
            val file = java.io.File(dataDir, "shared_prefs/lspilot_kejiyu.xml")
            if (!file.isFile) return defaultValue
            java.io.FileInputStream(file).use { input ->
                val parser = android.util.Xml.newPullParser()
                parser.setInput(input, "UTF-8")
                var event = parser.eventType
                while (event != org.xmlpull.v1.XmlPullParser.END_DOCUMENT) {
                    if (event == org.xmlpull.v1.XmlPullParser.START_TAG && parser.name == "boolean") {
                        if (parser.getAttributeValue(null, "name") == key) {
                            return parser.getAttributeValue(null, "value")?.toBooleanStrictOrNull() ?: defaultValue
                        }
                    }
                    event = parser.next()
                }
            }
        } catch (_: Throwable) {}
        return defaultValue
    }

    private fun readIntPrefFromDisk(key: String, defaultValue: Int): Int {
        try {
            val dataDir = try {
                appCtx?.applicationInfo?.dataDir
            } catch (_: Throwable) { null } ?: "/data/user/0/$gPkg"
            val file = java.io.File(dataDir, "shared_prefs/lspilot_kejiyu.xml")
            if (!file.isFile) return defaultValue
            java.io.FileInputStream(file).use { input ->
                val parser = android.util.Xml.newPullParser()
                parser.setInput(input, "UTF-8")
                var event = parser.eventType
                while (event != org.xmlpull.v1.XmlPullParser.END_DOCUMENT) {
                    if (event == org.xmlpull.v1.XmlPullParser.START_TAG && parser.name == "int") {
                        if (parser.getAttributeValue(null, "name") == key) {
                            return parser.getAttributeValue(null, "value")?.toIntOrNull() ?: defaultValue
                        }
                    }
                    event = parser.next()
                }
            }
        } catch (_: Throwable) {}
        return defaultValue
    }

    private fun downloadUnlimitEnabledNow(): Boolean {
        val master = readBooleanPrefFromDisk("master_on", gMasterOn)
        val feature = readBooleanPrefFromDisk(DOWNLOAD_LIMIT_PREF, gDownloadLimitUnlimitOn)
        return master && feature
    }

    private fun currentDownloadLimitValues(): Triple<Int, Int, Int> {
        fun clean(v: Int): Int = v.coerceIn(1, 999_999_999)
        val episode = clean(readIntPrefFromDisk(DOWNLOAD_LIMIT_EPISODE_PREF, gDownloadOneDayEpisode))
        val series = clean(readIntPrefFromDisk(DOWNLOAD_LIMIT_SERIES_PREF, gDownloadOneDaySeries))
        val total = clean(readIntPrefFromDisk(DOWNLOAD_LIMIT_TOTAL_PREF, gDownloadTotalSeries))
        return Triple(episode, series, total)
    }

    private fun customDownloadLimitJson(): String {
        val (episode, series, total) = currentDownloadLimitValues()
        return "{\"one_day_max_episode_limit\":" + episode +
            ",\"one_day_max_series_limit\":" + series +
            ",\"total_max_series_limit\":" + total + "}"
    }

    private fun overwriteDownloadLimitObject(obj: Any?): Boolean {
        if (obj == null) return false
        val (episode, series, total) = currentDownloadLimitValues()
        val values = mapOf("a" to episode, "b" to series, "c" to total)
        return try {
            var changed = false
            for ((name, value) in values) {
                try {
                    val f = obj.javaClass.getDeclaredField(name)
                    if (f.type == Int::class.javaPrimitiveType) {
                        f.isAccessible = true
                        f.setInt(obj, value)
                        changed = true
                    }
                } catch (_: Throwable) {}
            }
            changed
        } catch (_: Throwable) { false }
    }
    private fun resolveEntryId(
        entry: String,
        root: View?,
        targetSet: MutableSet<Int> = gTargetIdSet,
        logPrefix: String = "资源",
    ) {
        if (root == null) return
        try {
            val id = root.resources.getIdentifier(entry, "id", gPkg)
            if (id > 0 && targetSet.add(id)) LogUtil.info("$logPrefix $entry ID: $id")
        } catch (_: Exception) {}
    }

    private fun resolvePauseRestoreIds(root: View?) {
        if (root == null) return
        gNames.hideIdNames.forEach { resolveEntryId(it, root, gPauseRestoreIdSet, "暂停恢复资源") }
        seriesIdNames.forEach { resolveEntryId(it, root, gPauseRestoreIdSet, "暂停恢复资源") }
    }

    @Volatile private var gResourceIdsResolved = false

    private fun ensureResourceIdsResolved(root: View?) {
        if (gResourceIdsResolved || root == null) return
        synchronized(gTargetIdSet) {
            if (gResourceIdsResolved) return
            gNames.hideIdNames.forEach { resolveEntryId(it, root) }
            seriesIdNames.forEach { resolveEntryId(it, root, gSeriesTargetIdSet, "选集资源") }
            gNames.progressIdNames.forEach { resolveEntryId(it, root, gProgressIdSet, "进度条资源") }
            resolvePauseRestoreIds(root)
            gResourceIdsResolved = true
        }
    }
    /**
     * 「这个 Class 是不是 Compose 容器」的记忆表。
     *
     * `Class.getSimpleName()` 对带包名的类实现为 `name.substring(name.lastIndexOf('.') + 1)` ——
     * **每次调用都分配一个新 String**。而 `isComposeSeriesBar` 在 `gPlayerOn` 时会被每个被扫描的节点
     * 调用一次（实测占节点数的 99.5%），启动窗口内近 3000 次。
     *
     * 视图中不同 Class 的数量是有限的（数百级），按 Class 记忆后：
     * 每个 Class 只付一次 `simpleName` 代价，之后全部命中缓存、**零字符串分配**。
     * 用 `Class` 作 key 是安全的 —— `Class` 的 hashCode 就是身份哈希，且 Class 对象本身生命周期
     * 跟随 classLoader，不会造成泄漏。
     */
    private val gComposeBarClassCache = java.util.concurrent.ConcurrentHashMap<Class<*>, Boolean>()

    private fun isComposeSeriesBar(v: View?): Boolean {
        if (v == null) return false
        val cls = v.javaClass
        val cached = gComposeBarClassCache[cls]
        val isContainer = if (cached != null) cached else {
            val r = cls.simpleName == "TreeLifecycleComposeContainer"
            gComposeBarClassCache[cls] = r
            r
        }
        if (!isContainer) return false
        val density = try { v.resources.displayMetrics.density.coerceAtLeast(0.1f) } catch (_: Throwable) { 1f }
        val h = (if (v.height > 0) v.height else v.measuredHeight) / density
        if (h in 30f..70f || v.id == 0x7F0B0BB5) return true
        return false
    }

    private fun quickMatch(v: View?): Boolean {
        if (v == null || isInsideModuleUi(v)) return false
        LogUtil.incr("matchCall")
        try {
            val id = v.id
            if (id > 0) {
                if (gControlOn && gTargetIdSet.contains(id)) { LogUtil.incr("matchHit"); return true }
                if (gPlayerOn && gSeriesTargetIdSet.contains(id)) { LogUtil.incr("matchHit"); return true }
            }
        } catch (_: Exception) {}
        if (gPlayerOn && isComposeSeriesBar(v)) { LogUtil.incr("matchHit"); return true }
        if (gControlOn) {
            if (gHideClassesSet.isNotEmpty()) {
                try { if (v.javaClass.name in gHideClassesSet) { LogUtil.incr("matchHit"); return true } } catch (_: Exception) {}
            }
            if (isFullscreenWatchControl(v)) { LogUtil.incr("matchHit"); return true }
            if (isKnownFullSeriesEntry(v) || isHomeFullSeriesEntry(v)) { LogUtil.incr("matchHit"); return true }
        }
        return false
    }

    private fun isFullscreenWatchControl(v: View?): Boolean {
        if (!gNames.structuralFullscreenWatch || v !is LinearLayout) return false
        try { if (v.id > 0) return false } catch (_: Throwable) {}
        val density = try { v.resources.displayMetrics.density.coerceAtLeast(0.1f) } catch (_: Throwable) { 1f }
        val width = if (v.width > 0) v.width else v.measuredWidth
        val height = if (v.height > 0) v.height else v.measuredHeight
        if (width <= 0 || height <= 0) return false
        val widthDp = width / density
        val heightDp = height / density
        val leftDp = v.paddingLeft / density
        val topDp = v.paddingTop / density
        val rightDp = v.paddingRight / density
        val bottomDp = v.paddingBottom / density
        val bgOk = try { v.background?.javaClass?.name == "android.graphics.drawable.GradientDrawable" } catch (_: Throwable) { false }
        return bgOk && widthDp in 84f..94f && heightDp in 25f..32f &&
            leftDp in 8f..12f && rightDp in 8f..12f &&
            topDp in 4f..8f && bottomDp in 4f..8f
    }

    private fun isInsideCurrentActivityDecor(v: View?): Boolean {
        if (v == null) return false
        val decor = try { gCurrentActivity?.window?.decorView } catch (_: Throwable) { null } ?: return false
        var node: View? = v
        var guard = 0
        while (node != null && guard++ < 80) {
            if (node === decor) return true
            node = node.parent as? View
        }
        return false
    }

    private fun viewEntryName(v: View?): String {
        if (v == null) return ""
        return try {
            if (v.id > 0) v.resources.getResourceEntryName(v.id) else ""
        } catch (_: Throwable) { "" }
    }

    private fun collectUiTexts(root: View?, maxDepth: Int = 4): List<String> {
        if (root == null) return emptyList()
        val out = ArrayList<String>(8)
        fun walk(v: View, depth: Int) {
            if (depth > maxDepth || out.size >= 24) return
            if (v is TextView) {
                val text = try { v.text?.toString()?.trim().orEmpty() } catch (_: Throwable) { "" }
                if (text.isNotEmpty()) out.add(text)
            }
            val desc = try { v.contentDescription?.toString()?.trim().orEmpty() } catch (_: Throwable) { "" }
            if (desc.isNotEmpty()) out.add(desc)
            if (v is ViewGroup) {
                for (i in 0 until v.childCount) walk(v.getChildAt(i), depth + 1)
            }
        }
        walk(root, 0)
        return out
    }

    private val fullSeriesCountPattern = Regex("全\\d+集")
    private val exactFullSeriesEntryPattern = Regex("^(观看完整漫剧|观看完整短剧|观看全集|完整剧集)全?\\d+集$")
    private val exactFullSeriesCountPattern = Regex("^全\\d+集$")

    private fun isFullSeriesEntryLabel(raw: String): Boolean {
        val text = raw.replace(" ", "")
        val lower = text.lowercase()
        return text.contains("观看完整漫剧") || text.contains("观看完整短剧") ||
            text.contains("观看全集") || text.contains("完整剧集") ||
            lower.contains("watchall") || lower.contains("allepisodes") || lower.contains("fullseries") ||
            (text.contains("观看") && fullSeriesCountPattern.containsMatchIn(text))
    }

    private fun hasExactFullSeriesEntryLabel(rawTexts: List<String>): Boolean {
        val texts = rawTexts.map { raw -> raw.filterNot { it.isWhitespace() } }
        if (texts.any(exactFullSeriesEntryPattern::matches)) return true
        val hasTitle = texts.any { it == "观看完整漫剧" || it == "观看完整短剧" }
        return hasTitle && texts.any(exactFullSeriesCountPattern::matches)
    }

    private fun hideFullSeriesEntryBeforeDraw(label: TextView?) {
        if (label == null || gPkg != TargetNames.OVERSEA_PACKAGE || !gMasterOn || !gControlOn ||
            (gRestoreControlsOnPause && gVideoPaused) || isInsideModuleUi(label)
        ) return
        val text = try { label.text?.toString().orEmpty() } catch (_: Throwable) { "" }
        val normalized = text.filterNot { it.isWhitespace() }
        if (!normalized.contains("观看完整漫剧") && !normalized.contains("观看完整短剧") &&
            !exactFullSeriesCountPattern.matches(normalized)
        ) return
        var node = label.parent as? View
        var depth = 0
        while (node != null && depth++ < 8) {
            if (node is ViewGroup && node.javaClass.name == "android.widget.FrameLayout" &&
                viewEntryName(node) == "root_layout"
            ) {
                if (!hasExactFullSeriesEntryLabel(collectUiTexts(node, 5))) return
                val first = synchronized(gKnownFullSeriesEntryViews) {
                    if (gKnownFullSeriesEntryViews.containsKey(node)) false
                    else true.also { gKnownFullSeriesEntryViews[node] = true }
                }
                if (first) {
                    blindView(node)
                    LogUtil.info("home full-series entry hidden before first draw")
                }
                return
            }
            node = node.parent as? View
        }
    }

    private fun isRefreshText(text: String): Boolean {
        val t = text.lowercase()
        return text.contains("下拉刷新") || text.contains("刷新内容") || text.contains("松开刷新") ||
            text.contains("正在刷新") || t.contains("pull to refresh") || t.contains("release to refresh") ||
            t.contains("refreshing")
    }

    private fun isRefreshAccessoryContainer(v: View?): Boolean {
        if (v !is ViewGroup || !isInsideCurrentActivityDecor(v)) return false
        val density = try { v.resources.displayMetrics.density.coerceAtLeast(0.1f) } catch (_: Throwable) { 1f }
        val width = if (v.width > 0) v.width else v.measuredWidth
        val height = if (v.height > 0) v.height else v.measuredHeight
        if (width <= 0 || height <= 0) return false
        val hDp = height / density
        if (hDp !in 39f..56f) return false
        val screenW = try { v.resources.displayMetrics.widthPixels } catch (_: Throwable) { width }
        if (screenW > 0 && width.toFloat() / screenW.toFloat() < 0.93f) return false

        if (collectUiTexts(v, 4).any(::isRefreshText)) return true
        val idName = viewEntryName(v)
        if (idName == "root_layout") {
            val parent = v.parent as? View
            if (collectUiTexts(parent, 3).any(::isRefreshText)) return true
        }
        return false
    }

    private fun hideRefreshAccessory(v: View?) {
        if (v == null || !shouldApplyUiHiding() || !gMasterOn || !gRefreshOff) return
        try {
            synchronized(gKnownRefreshAccessoryViews) { gKnownRefreshAccessoryViews[v] = true }
            if (v.visibility != View.GONE) {
                rememberViewState(v)

                setModuleVisibility(v, View.GONE)
                v.requestLayout()
                (v.parent as? View)?.requestLayout()
                LogUtil.incr("refreshAccessoryHide")
            }
        } catch (_: Throwable) {}
    }

    private fun scanTreeRefreshAccessory(v: View?) {
        if (v == null || !gMasterOn || !gRefreshOff) return
        if (isRefreshAccessoryContainer(v)) {
            hideRefreshAccessory(v)
            return
        }
        if (v is ViewGroup) for (i in 0 until v.childCount) scanTreeRefreshAccessory(v.getChildAt(i))
    }

    private fun restoreRefreshAccessories() {
        val views = synchronized(gKnownRefreshAccessoryViews) {
            gKnownRefreshAccessoryViews.keys.toList().also { gKnownRefreshAccessoryViews.clear() }
        }
        for (v in views) restoreView(v)
        if (views.isNotEmpty()) LogUtil.info("restore refresh accessories: ${views.size}")
    }

    private fun isHomeFullSeriesEntry(v: View?): Boolean {
        if (gPkg !in setOf(TargetNames.CN_PACKAGE, TargetNames.OVERSEA_PACKAGE) ||
            v !is ViewGroup || !isInsideCurrentActivityDecor(v)) return false
        synchronized(gKnownFullSeriesEntryViews) {
            if (gKnownFullSeriesEntryViews.containsKey(v)) return true
        }
        if (v.javaClass.name != "android.widget.FrameLayout") return false
        if (viewEntryName(v) != "root_layout") return false

        val texts = collectUiTexts(v, 5)
        val matchedByText = texts.any(::isFullSeriesEntryLabel)

        val dm = try { v.resources.displayMetrics } catch (_: Throwable) { return false }
        val density = dm.density.coerceAtLeast(0.1f)
        val width = if (v.width > 0) v.width else v.measuredWidth
        val height = if (v.height > 0) v.height else v.measuredHeight
        if (width <= 0 || height <= 0) {
            if (gPkg != TargetNames.OVERSEA_PACKAGE || !hasExactFullSeriesEntryLabel(texts)) return false
            synchronized(gKnownFullSeriesEntryViews) { gKnownFullSeriesEntryViews[v] = true }
            LogUtil.info("home full-series entry recognized before layout")
            return true
        }
        val hDp = height / density
        if (hDp !in 41f..47.5f) return false
        if (width.toFloat() / dm.widthPixels.coerceAtLeast(1).toFloat() < 0.94f) return false

        val loc = IntArray(2)
        try { v.getLocationOnScreen(loc) } catch (_: Throwable) { return false }

        if (loc[1] < dm.heightPixels * 0.68f) return false

        if (!matchedByText && gPkg != TargetNames.OVERSEA_PACKAGE) return false

        synchronized(gKnownFullSeriesEntryViews) { gKnownFullSeriesEntryViews[v] = true }
        LogUtil.info("home full-series entry recognized: root_layout, h=${"%.1f".format(hDp)}dp")
        return true
    }

    private fun isKnownFullSeriesEntry(v: View?): Boolean = if (v == null) false else
        synchronized(gKnownFullSeriesEntryViews) { gKnownFullSeriesEntryViews.containsKey(v) }

    private fun isHomeBottomBackdropMarker(v: View?): Boolean {
        if (gPkg !in setOf(TargetNames.CN_PACKAGE, TargetNames.OVERSEA_PACKAGE) ||
            v == null || !isInsideCurrentActivityDecor(v)) return false
        val markerName = viewEntryName(v)
        val markerMatched = when {
            gPkg == TargetNames.OVERSEA_PACKAGE -> markerName == "bottom_tab_mask"
            gNames.profileId == "CN-7.3.3.18" -> markerName == "ar9"
            gNames.profileId == "CN-7.3.2.32" || gNames.profileId == "CN-7.3.1.32" -> markerName == "ar8"
            else -> markerName == "ar9" || markerName == "ar8"
        }
        if (!markerMatched) return false
        val dm = try { v.resources.displayMetrics } catch (_: Throwable) { return false }
        val density = dm.density.coerceAtLeast(0.1f)
        val width = if (v.width > 0) v.width else v.measuredWidth
        val height = if (v.height > 0) v.height else v.measuredHeight
        if (width <= 0 || height <= 0) return false
        val hDp = height / density
        if (hDp !in 50f..61f) return false
        if (width.toFloat() / dm.widthPixels.coerceAtLeast(1).toFloat() < 0.94f) return false
        val loc = IntArray(2)
        try { v.getLocationOnScreen(loc) } catch (_: Throwable) { return false }
        return loc[1] + height >= dm.heightPixels * 0.94f
    }

    private fun isKnownBottomBackdrop(v: View?): Boolean = if (v == null) false else
        synchronized(gKnownBottomBackdropViews) { gKnownBottomBackdropViews.containsKey(v) }

    private fun isNativeMainBottomFrame(v: View?): Boolean {
        if (gPkg !in setOf(TargetNames.CN_PACKAGE, TargetNames.OVERSEA_PACKAGE) || v == null) return false
        return try {
            val idName = viewEntryName(v)
            v.javaClass.name == "com.dragon.read.widget.BottomTabFrameLayout" ||
                (v is ViewGroup && (idName == "aot" || idName == "bottom_bar_layout"))
        } catch (_: Throwable) { false }
    }

    private fun isNativeVideoFeedBottomMask(v: View?): Boolean {
        if (gPkg !in setOf(TargetNames.CN_PACKAGE, TargetNames.OVERSEA_PACKAGE) || v == null) return false
        return try {
            val idName = viewEntryName(v)
            when {
                gPkg == TargetNames.OVERSEA_PACKAGE -> idName == "bottom_tab_mask"
                gNames.profileId == "CN-7.3.3.18" -> idName == "ar9"
                gNames.profileId == "CN-7.3.2.32" || gNames.profileId == "CN-7.3.1.32" -> idName == "ar8"
                gPkg == TargetNames.CN_PACKAGE -> idName == "ar9" || idName == "ar8"
                else -> false
            }
        } catch (_: Throwable) { false }
    }

    private fun rememberNativeNavBarColor(act: Activity?) {
        if (act == null) return
        try {
            synchronized(gSavedNativeNavBarColors) {
                if (!gSavedNativeNavBarColors.containsKey(act)) {
                    gSavedNativeNavBarColors[act] = act.window.navigationBarColor
                }
            }
        } catch (_: Throwable) {}
    }

    private fun updateNativeNavBarRestoreColor(act: Activity?) {
        if (act == null) return
        try { synchronized(gSavedNativeNavBarColors) { gSavedNativeNavBarColors[act] = act.window.navigationBarColor } }
        catch (_: Throwable) {}
    }

    private fun restoreNativeBottomWindowColor(act: Activity?) {
        if (act == null || gNavBarOff) return
        val color = synchronized(gSavedNativeNavBarColors) { gSavedNativeNavBarColors.remove(act) } ?: return
        try {
            act.window.navigationBarColor = color
            if (Build.VERSION.SDK_INT >= 30) {
                act.window.setDecorFitsSystemWindows(false)
                act.window.decorView.windowInsetsController?.show(WindowInsets.Type.navigationBars())
            }
            act.window.decorView.requestApplyInsets()
        } catch (_: Throwable) {}
    }

    private fun collapseNativeMainBottomFrame(v: View?) {
        if (v == null || !shouldApplyUiHiding() || !gMasterOn || !gControlOn || (gRestoreControlsOnPause && gVideoPaused)) return
        if (!isNativeMainBottomFrame(v)) return
        try {
            synchronized(gKnownMainBottomNavViews) { gKnownMainBottomNavViews[v] = true }
            if (v.visibility != View.GONE) {
                rememberViewState(v)
                val oldHeight = if (v.height > 0) v.height else v.measuredHeight
                setModuleVisibility(v, View.GONE)
                LogUtil.info("底部原生栏已折叠 | pkg=$gPkg | id=${viewEntryName(v)} | h=${oldHeight}px")
            }
            v.requestLayout()
            (v.parent as? View)?.requestLayout()
            rememberNativeNavBarColor(gCurrentActivity)
            applyBottomEdgeToEdge(gCurrentActivity, gNavBarOff)
            LogUtil.incr("nativeBottomFrameCollapse")
        } catch (e: Throwable) {
            LogUtil.warn("collapse native BottomTabFrameLayout failed: $e")
        }
    }

    private fun collapseNativeVideoFeedBottomMask(v: View?) {
        if (v == null || !shouldApplyUiHiding() || !gMasterOn || !gControlOn || (gRestoreControlsOnPause && gVideoPaused)) return
        if (!isNativeVideoFeedBottomMask(v)) return
        try {
            synchronized(gKnownBottomBackdropViews) { gKnownBottomBackdropViews[v] = true }
            if (v.visibility != View.GONE) {
                rememberViewState(v)
                val oldHeight = if (v.height > 0) v.height else v.measuredHeight
                setModuleVisibility(v, View.GONE)
                LogUtil.info("短视频底部 mask 已折叠 | pkg=$gPkg | id=${viewEntryName(v)} | h=${oldHeight}px")
            }
            v.requestLayout()
            (v.parent as? View)?.requestLayout()
            LogUtil.incr("nativeVideoFeedBottomMaskCollapse")
        } catch (e: Throwable) {
            LogUtil.warn("collapse VideoFeedTabBottomMask failed: $e")
        }
    }

    private fun findNativeMainBottomFrame(act: Activity?): View? {
        if (act == null || gPkg !in setOf(TargetNames.CN_PACKAGE, TargetNames.OVERSEA_PACKAGE)) return null
        val fieldCandidates = when (gPkg) {
            TargetNames.CN_PACKAGE -> listOf("H", "G")
            TargetNames.OVERSEA_PACKAGE -> listOf("G", "H")
            else -> emptyList()
        }
        for (fieldName in fieldCandidates) {
            try {
                val byField = findFieldValue(act, fieldName) as? View
                if (isNativeMainBottomFrame(byField)) return byField
            } catch (_: Throwable) {}
        }
        val idCandidates = when (gPkg) {
            TargetNames.CN_PACKAGE -> listOf("aot", "bottom_bar_layout")
            TargetNames.OVERSEA_PACKAGE -> listOf("bottom_bar_layout", "aot")
            else -> emptyList()
        }
        for (entry in idCandidates) {
            try {
                val id = act.resources.getIdentifier(entry, "id", gPkg)
                if (id != 0) {
                    val byId = act.findViewById<View>(id)
                    if (isNativeMainBottomFrame(byId)) return byId
                }
            } catch (_: Throwable) {}
        }
        fun walk(v: View?): View? {
            if (v == null) return null
            if (isNativeMainBottomFrame(v)) return v
            if (v is ViewGroup) for (i in 0 until v.childCount) {
                val hit = walk(v.getChildAt(i))
                if (hit != null) return hit
            }
            return null
        }
        return try { walk(act.window?.decorView) } catch (_: Throwable) { null }
    }

    private fun enforceNativeMainBottomHidden(act: Activity?) {
        if (!shouldApplyUiHiding() || !gMasterOn || !gControlOn || (gRestoreControlsOnPause && gVideoPaused)) return
        collapseNativeMainBottomFrame(findNativeMainBottomFrame(act))
    }

    private fun findFeedViewport(root: View?): View? {
        if (root == null) return null
        try {
            if (root.javaClass.name == "androidx.viewpager2.widget.ViewPager2") return root
            val idName = viewEntryName(root)

            if (idName == "kkk" || idName == "kij" || idName == "view_pager_container") return root
        } catch (_: Throwable) {}
        if (root is ViewGroup) {
            for (i in 0 until root.childCount) {
                val hit = findFeedViewport(root.getChildAt(i))
                if (hit != null) return hit
            }
        }
        return null
    }

    private fun reclaimFeedViewportBottomMargin(pager: View?) {
        if (pager == null || !shouldApplyUiHiding() || !gMasterOn || !gControlOn || (gRestoreControlsOnPause && gVideoPaused)) return
        try {
            val actName = gCurrentActivity?.javaClass?.simpleName ?: ""
            if (actName == "ShortSeriesActivity") return
            synchronized(gKnownFeedViewportViews) { gKnownFeedViewportViews[pager] = true }
            val lp = pager.layoutParams as? ViewGroup.MarginLayoutParams ?: return
            val nativeBottom = lp.bottomMargin
            if (nativeBottom > 0) {
                synchronized(gFeedViewportNativeBottomMargins) {
                    gFeedViewportNativeBottomMargins[pager] = nativeBottom
                }
                lp.bottomMargin = 0
                pager.layoutParams = lp
                LogUtil.info("首页视频底部占位已回收 | pkg=$gPkg | id=${viewEntryName(pager)} | ${nativeBottom}px -> 0")
                LogUtil.incr("feedViewportBottomMarginReclaim")
            }
        } catch (_: Throwable) {}
    }

    private fun enforceKnownFeedViewportBottomMargin() {
        if (!gMasterOn || !gControlOn || (gRestoreControlsOnPause && gVideoPaused)) return
        val views = synchronized(gKnownFeedViewportViews) { gKnownFeedViewportViews.keys.toList() }
        for (pager in views) reclaimFeedViewportBottomMargin(pager)
    }

    private fun restoreFeedViewportBottomMargins(clear: Boolean = true) {
        val entries = synchronized(gFeedViewportNativeBottomMargins) {
            gFeedViewportNativeBottomMargins.entries.map { it.key to it.value }.also {
                if (clear) gFeedViewportNativeBottomMargins.clear()
            }
        }
        for ((pager, bottom) in entries) {
            try {
                val lp = pager.layoutParams as? ViewGroup.MarginLayoutParams ?: continue
                if (lp.bottomMargin != bottom) {
                    lp.bottomMargin = bottom
                    pager.layoutParams = lp
                }
            } catch (_: Throwable) {}
        }
        if (clear) synchronized(gKnownFeedViewportViews) { gKnownFeedViewportViews.clear() }
        if (entries.isNotEmpty()) LogUtil.info("首页视频底部占位已恢复 | count=${entries.size}")
    }

    private fun reflectIntField(obj: Any?, name: String): Int? {
        if (obj == null) return null
        var c: Class<*>? = obj.javaClass
        while (c != null) {
            try {
                val f = c.getDeclaredField(name)
                f.isAccessible = true
                return f.getInt(obj)
            } catch (_: Throwable) {}
            c = c.superclass
        }
        return null
    }

    private fun setReflectIntField(obj: Any?, name: String, value: Int): Boolean {
        if (obj == null) return false
        var c: Class<*>? = obj.javaClass
        while (c != null) {
            try {
                val f = c.getDeclaredField(name)
                f.isAccessible = true
                f.setInt(obj, value)
                return true
            } catch (_: Throwable) {}
            c = c.superclass
        }
        return false
    }

    private fun rememberBottomLayoutState(v: View) {
        synchronized(gSavedBottomLayoutStates) {
            if (gSavedBottomLayoutStates.containsKey(v)) return
            val lp = v.layoutParams
            val mlp = lp as? ViewGroup.MarginLayoutParams
            gSavedBottomLayoutStates[v] = SavedBottomLayoutState(
                width = lp?.width ?: ViewGroup.LayoutParams.WRAP_CONTENT,
                height = lp?.height ?: ViewGroup.LayoutParams.WRAP_CONTENT,
                paddingLeft = v.paddingLeft,
                paddingTop = v.paddingTop,
                paddingRight = v.paddingRight,
                paddingBottom = v.paddingBottom,
                marginLeft = mlp?.leftMargin,
                marginTop = mlp?.topMargin,
                marginRight = mlp?.rightMargin,
                marginBottom = mlp?.bottomMargin,
                bottomToTop = reflectIntField(lp, "bottomToTop"),
                bottomToBottom = reflectIntField(lp, "bottomToBottom"),
                goneBottomMargin = reflectIntField(lp, "goneBottomMargin"),
            )
        }
    }

    private fun restoreBottomLayoutReclaim() {
        val entries = synchronized(gSavedBottomLayoutStates) {
            val copy = gSavedBottomLayoutStates.entries.map { it.key to it.value }
            gSavedBottomLayoutStates.clear()
            copy
        }
        for ((v, st) in entries) {
            try {
                val lp = v.layoutParams
                if (lp != null) {
                    lp.width = st.width
                    lp.height = st.height
                    val mlp = lp as? ViewGroup.MarginLayoutParams
                    if (mlp != null && st.marginLeft != null && st.marginTop != null &&
                        st.marginRight != null && st.marginBottom != null) {
                        mlp.setMargins(st.marginLeft, st.marginTop, st.marginRight, st.marginBottom)
                    }
                    st.bottomToTop?.let { setReflectIntField(lp, "bottomToTop", it) }
                    st.bottomToBottom?.let { setReflectIntField(lp, "bottomToBottom", it) }
                    st.goneBottomMargin?.let { setReflectIntField(lp, "goneBottomMargin", it) }
                    v.layoutParams = lp
                }
                v.setPadding(st.paddingLeft, st.paddingTop, st.paddingRight, st.paddingBottom)
                v.requestLayout()
                (v.parent as? View)?.requestLayout()
            } catch (_: Throwable) {}
        }
        if (entries.isNotEmpty()) LogUtil.info("restore bottom layout reclaim: ${entries.size}")
    }

    private fun isNear(value: Int, target: Int, tolerance: Int): Boolean =
        kotlin.math.abs(value - target) <= tolerance

    private fun reclaimBottomSpaceFromAr9(marker: View) {
        if (!gMasterOn || !gControlOn || (gRestoreControlsOnPause && gVideoPaused)) return
        try {
            val dm = marker.resources.displayMetrics
            val density = dm.density.coerceAtLeast(0.1f)
            val markerLoc = IntArray(2)
            marker.getLocationOnScreen(markerLoc)
            val markerTop = markerLoc[1]
            val markerHeight = (if (marker.height > 0) marker.height else marker.measuredHeight).coerceAtLeast(1)
            val tol = (12f * density).toInt().coerceAtLeast(8)
            val minLargeHeight = (dm.heightPixels * 0.28f).toInt()

            val anchorIds = linkedSetOf<Int>()
            var idNode: View? = marker
            var idDepth = 0
            while (idNode != null && idDepth++ < 8) {
                if (idNode.id > 0) anchorIds.add(idNode.id)
                idNode = idNode.parent as? View
            }

            fun reclaimLargeBranch(root: View?, depth: Int = 0) {
                if (root == null || depth > 4) return
                try {
                    val w = if (root.width > 0) root.width else root.measuredWidth
                    val h = if (root.height > 0) root.height else root.measuredHeight
                    if (w > 0 && h > minLargeHeight &&
                        w.toFloat() / dm.widthPixels.coerceAtLeast(1).toFloat() >= 0.82f) {
                        val lp = root.layoutParams
                        val mlp = lp as? ViewGroup.MarginLayoutParams
                        val bottomMargin = mlp?.bottomMargin ?: 0
                        val padBottom = root.paddingBottom
                        val barLikeMin = (markerHeight * 0.55f).toInt()
                        val barLikeMax = (markerHeight * 1.75f).toInt()
                        var changed = false
                        if (padBottom in barLikeMin..barLikeMax && padBottom > 0) {
                            rememberBottomLayoutState(root)
                            root.setPadding(root.paddingLeft, root.paddingTop, root.paddingRight, 0)
                            changed = true
                        }
                        if (mlp != null && bottomMargin in barLikeMin..barLikeMax && bottomMargin > 0) {
                            rememberBottomLayoutState(root)
                            mlp.bottomMargin = 0
                            root.layoutParams = mlp
                            changed = true
                        }
                        if (changed) {
                            root.requestLayout()
                            LogUtil.incr("bottomInsetReclaim")
                        }
                    }
                } catch (_: Throwable) {}
                if (root is ViewGroup) {
                    for (i in 0 until root.childCount) reclaimLargeBranch(root.getChildAt(i), depth + 1)
                }
            }

            var branch: View = marker
            var parent = marker.parent as? ViewGroup
            var depth = 0
            while (parent != null && depth++ < 7) {
                val parentChildren = parent.childCount
                for (i in 0 until parentChildren) {
                    val child = parent.getChildAt(i)
                    if (child === branch) continue
                    try {
                        val lp = child.layoutParams
                        var changed = false

                        val btt = reflectIntField(lp, "bottomToTop")
                        if (btt != null && btt >= 0 && anchorIds.contains(btt)) {
                            rememberBottomLayoutState(child)
                            setReflectIntField(lp, "bottomToTop", -1)
                            setReflectIntField(lp, "bottomToBottom", 0)
                            setReflectIntField(lp, "goneBottomMargin", 0)
                            child.layoutParams = lp
                            changed = true
                            LogUtil.info("bottom constraint reclaimed: ${child.javaClass.name}, anchor=$btt")
                            LogUtil.incr("bottomConstraintReclaim")
                        }

                        val loc = IntArray(2)
                        child.getLocationOnScreen(loc)
                        val w = if (child.width > 0) child.width else child.measuredWidth
                        val h = if (child.height > 0) child.height else child.measuredHeight
                        val childBottom = loc[1] + h
                        val looksLikeMainContent = w > 0 && h > minLargeHeight &&
                            w.toFloat() / dm.widthPixels.coerceAtLeast(1).toFloat() >= 0.82f &&
                            loc[1] < markerTop - markerHeight
                        if (looksLikeMainContent && isNear(childBottom, markerTop, tol) && lp != null) {
                            val parentLoc = IntArray(2)
                            parent.getLocationOnScreen(parentLoc)
                            val parentH = if (parent.height > 0) parent.height else parent.measuredHeight
                            val desiredHeight = parentLoc[1] + parentH - loc[1]
                            val growBy = desiredHeight - h
                            val maxGrow = (markerHeight * 1.8f).toInt().coerceAtLeast(markerHeight + tol)
                            if (growBy > tol && growBy <= maxGrow) {
                                rememberBottomLayoutState(child)
                                lp.height = desiredHeight
                                child.layoutParams = lp
                                changed = true
                                LogUtil.info("bottom content extended: ${child.javaClass.name}, ${h}px -> ${desiredHeight}px")
                                LogUtil.incr("bottomHeightReclaim")
                            }
                        }

                        if (looksLikeMainContent) reclaimLargeBranch(child)

                        if (changed) {
                            child.requestLayout()
                            parent.requestLayout()
                        }
                    } catch (_: Throwable) {}
                }
                branch = parent
                parent = parent.parent as? ViewGroup
            }

            val decor = gCurrentActivity?.window?.decorView
            reclaimLargeBranch(decor)
        } catch (e: Throwable) {
            LogUtil.warn("reclaim bottom space from ar9 failed: $e")
        }
    }

    private fun bottomBackdropCollapseTarget(marker: View): View {
        val dm = try { marker.resources.displayMetrics } catch (_: Throwable) { return marker }
        val density = dm.density.coerceAtLeast(0.1f)
        var target: View = marker
        var cur: View = marker
        repeat(4) {
            val parent = cur.parent as? View ?: return@repeat
            val w = if (parent.width > 0) parent.width else parent.measuredWidth
            val h = if (parent.height > 0) parent.height else parent.measuredHeight
            if (w <= 0 || h <= 0) return@repeat
            val hDp = h / density
            val loc = IntArray(2)
            try { parent.getLocationOnScreen(loc) } catch (_: Throwable) { return@repeat }
            val fullWidth = w.toFloat() / dm.widthPixels.coerceAtLeast(1).toFloat() >= 0.92f
            val compact = hDp in 48f..128f
            val nearBottom = loc[1] + h >= dm.heightPixels * 0.92f
            if (!fullWidth || !compact || !nearBottom) return@repeat
            target = parent
            cur = parent
        }
        return target
    }

    private fun collapseHomeBottomBackdrop(marker: View?) {
        if (marker == null || !shouldApplyUiHiding() || !gMasterOn || !gControlOn || (gRestoreControlsOnPause && gVideoPaused)) return

        if (isNativeVideoFeedBottomMask(marker) || isHomeBottomBackdropMarker(marker) || isKnownBottomBackdrop(marker)) {
            collapseNativeVideoFeedBottomMask(marker)
            enforceNativeMainBottomHidden(gCurrentActivity)
        }
    }

    private val mainBottomNavLabels = setOf("首页", "剧场", "商城", "赚钱", "我的")

    private fun isMainBottomNavContainer(v: View?): Boolean {
        if (gPkg != TargetNames.CN_PACKAGE || v !is ViewGroup || !isInsideCurrentActivityDecor(v)) return false
        synchronized(gKnownMainBottomNavViews) { if (gKnownMainBottomNavViews.containsKey(v)) return true }
        val density = try { v.resources.displayMetrics.density.coerceAtLeast(0.1f) } catch (_: Throwable) { 1f }
        val width = if (v.width > 0) v.width else v.measuredWidth
        val height = if (v.height > 0) v.height else v.measuredHeight
        if (width <= 0 || height <= 0) return false
        val hDp = height / density
        if (hDp !in 38f..105f) return false
        val dm = try { v.resources.displayMetrics } catch (_: Throwable) { null } ?: return false
        if (width.toFloat() / dm.widthPixels.coerceAtLeast(1).toFloat() < 0.90f) return false
        val loc = IntArray(2)
        try { v.getLocationOnScreen(loc) } catch (_: Throwable) { return false }
        if (loc[1] < dm.heightPixels * 0.55f) return false
        val labels = collectUiTexts(v, 5).map { it.trim() }.toSet()
        val hitCount = mainBottomNavLabels.count { target -> labels.any { it == target } }

        var structuralSlots = 0
        if (v.childCount in 4..7) {
            for (i in 0 until v.childCount) {
                val child = v.getChildAt(i) ?: continue
                val cw = if (child.width > 0) child.width else child.measuredWidth
                val ch = if (child.height > 0) child.height else child.measuredHeight
                val cwRatio = cw.toFloat() / dm.widthPixels.coerceAtLeast(1).toFloat()
                val chDp = ch / density
                if (cwRatio in 0.12f..0.30f && chDp >= 24f) structuralSlots++
            }
        }
        val nearBottom = loc[1] + height >= dm.heightPixels * 0.90f
        val structuralMatch = nearBottom && (
            structuralSlots >= 4 ||

                (v.childCount == 5 && hDp in 48f..88f)
            )
        if (hitCount < 4 && !structuralMatch) return false
        synchronized(gKnownMainBottomNavViews) { gKnownMainBottomNavViews[v] = true }
        LogUtil.info("main bottom nav recognized: ${v.javaClass.name}, h=${"%.1f".format(hDp)}dp, labels=$hitCount, slots=$structuralSlots")
        return true
    }

    private fun isKnownMainBottomNav(v: View?): Boolean = if (v == null) false else
        synchronized(gKnownMainBottomNavViews) { gKnownMainBottomNavViews.containsKey(v) }

    private fun shouldCollapseControl(v: View?): Boolean {
        if (v == null) return false
        val idName = viewEntryName(v)

        if (idName == "iu1" || idName == "is7") return true
        if (isKnownFullSeriesEntry(v) || isHomeFullSeriesEntry(v)) return true
        if (isNativeMainBottomFrame(v) || isNativeVideoFeedBottomMask(v)) return true
        if (isKnownBottomBackdrop(v) || isHomeBottomBackdropMarker(v)) return true
        return isMainBottomNavContainer(v)
    }

    private fun installSeriesToolbarLayoutGuard(v: View?) {
        if (gNames.seriesToolbarProfile == "none" || v !is ViewGroup) return
        if (v.javaClass.name != "androidx.constraintlayout.widget.ConstraintLayout") return
        try { if (v.id > 0) return } catch (_: Throwable) {}
        synchronized(gSeriesGuardedViews) {
            if (gSeriesGuardedViews.containsKey(v)) return
            gSeriesGuardedViews[v] = true
        }
        try {
            v.addOnLayoutChangeListener { view, _, _, _, _, _, _, _, _ ->
                if (!shouldApplyUiHiding() || !gMasterOn || !gPlayerOn || (gRestoreControlsOnPause && gVideoPaused)) return@addOnLayoutChangeListener
                val kind = seriesToolbarKind(view)
                if (kind != 0) rememberSeriesToolbar(view, kind)
                if (kind != 0 && view.visibility == View.VISIBLE) {
                    rememberViewState(view)
                    setModuleVisibility(view, View.INVISIBLE)
                    LogUtil.incr("seriesLayoutGuardHide")
                }
            }
        } catch (_: Throwable) {}
    }

    private fun installSeriesToolbarGuardsInTree(v: View?) {
        if (v == null || gNames.seriesToolbarProfile == "none") return
        installSeriesToolbarLayoutGuard(v)
        if (v is ViewGroup) {
            for (i in 0 until v.childCount) installSeriesToolbarGuardsInTree(v.getChildAt(i))
        }
    }

    private fun seriesToolbarKind(v: View?): Int {
        if (gNames.seriesToolbarProfile == "none" || v !is ViewGroup) return 0
        if (v.javaClass.name != "androidx.constraintlayout.widget.ConstraintLayout") return 0

        if (!isInsideCurrentActivityDecor(v)) return 0
        try { if (v.id > 0) return 0 } catch (_: Throwable) {}
        if (v.paddingLeft != 0 || v.paddingTop != 0 || v.paddingRight != 0 || v.paddingBottom != 0) return 0
        try {
            val lp = v.layoutParams
            if (lp is ViewGroup.MarginLayoutParams &&
                (lp.leftMargin != 0 || lp.topMargin != 0 || lp.rightMargin != 0 || lp.bottomMargin != 0)) return 0
        } catch (_: Throwable) {}

        val density = try { v.resources.displayMetrics.density.coerceAtLeast(0.1f) } catch (_: Throwable) { 1f }
        val width = if (v.width > 0) v.width else v.measuredWidth
        val height = if (v.height > 0) v.height else v.measuredHeight
        if (width <= 0 || height <= 0) return 0
        val widthDp = width / density
        val heightDp = height / density
        val parentWidth = ((v.parent as? View)?.width ?: 0).takeIf { it > 0 }
            ?: try { v.resources.displayMetrics.widthPixels } catch (_: Throwable) { width }
        val widthRatio = if (parentWidth > 0) width.toFloat() / parentWidth.toFloat() else 1f
        val totalInsetDp = if (parentWidth >= width) (parentWidth - width) / density else 0f

        when (gNames.seriesToolbarProfile) {
            "oversea73132" -> {
                val isTop = v.childCount == 9 && heightDp in 40f..48f &&
                    (widthDp >= 400f || widthRatio >= 0.97f)
                if (isTop) return 1
                val isBottom = v.childCount == 3 && heightDp in 36f..44f &&
                    (widthDp in 378f..408f || totalInsetDp in 24f..42f)
                if (isBottom) return 2
            }
            "cn73318" -> {

                val isTop = heightDp in 41f..47f && (widthDp >= 410f || widthRatio >= 0.97f)
                if (isTop) return 1

                val isBottom = heightDp in 36f..44f &&
                    (widthDp in 378f..408f || totalInsetDp in 24f..42f)
                if (isBottom) return 2
            }
        }
        return 0
    }

    private fun rememberSeriesToolbar(v: View?, kind: Int) {
        if (v == null || kind == 0) return
        synchronized(gKnownSeriesToolbars) { gKnownSeriesToolbars[v] = kind }
    }

    private fun knownSeriesToolbarSnapshot(): List<Pair<View, Int>> = synchronized(gKnownSeriesToolbars) {
        gKnownSeriesToolbars.entries.mapNotNull { (view, kind) ->
            if (view.windowToken != null || view.isAttachedToWindow) Pair(view, kind) else null
        }
    }

    private fun hideSeriesToolbarView(v: View?) {
        if (v == null || !shouldApplyUiHiding() || !gMasterOn || !gPlayerOn || (gRestoreControlsOnPause && gVideoPaused)) return
        val kind = seriesToolbarKind(v)
        if (kind == 0) return
        rememberSeriesToolbar(v, kind)
        try {
            if (v.visibility == View.VISIBLE) {
                rememberViewState(v)
                setModuleVisibility(v, View.INVISIBLE)
                LogUtil.incr(if (kind == 1) "seriesTopHide" else "seriesBottomHide")
            }
        } catch (_: Throwable) {}
    }

    private fun scanTreePlayer(v: View?) {
        if (v == null || !gMasterOn || !gPlayerOn || (gRestoreControlsOnPause && gVideoPaused)) return
        if (seriesToolbarKind(v) != 0) { hideSeriesToolbarView(v); return }
        if (v is ViewGroup) for (i in 0 until v.childCount) scanTreePlayer(v.getChildAt(i))
    }

    private fun scanTreePlayerRestore(v: View?) {
        if (v == null) return
        val knownKind = synchronized(gKnownSeriesToolbars) { gKnownSeriesToolbars[v] ?: 0 }
        val kind = if (knownKind != 0) knownKind else seriesToolbarKind(v)
        if (kind != 0) {
            rememberSeriesToolbar(v, kind)
            restoreView(v)
            return
        }
        if (v is ViewGroup) for (i in 0 until v.childCount) scanTreePlayerRestore(v.getChildAt(i))
    }
    private inline fun <T> internalViewMutation(block: () -> T): T {
        val old = gInternalViewMutation.get() == true
        gInternalViewMutation.set(true)
        return try { block() } finally { gInternalViewMutation.set(old) }
    }

    private fun rememberViewState(v: View, desiredVisibility: Int? = null) {
        synchronized(gSavedViewStates) {
            val old = gSavedViewStates[v]
            if (old == null) {
                gSavedViewStates[v] = SavedViewState(desiredVisibility ?: v.visibility, v.alpha)
            } else if (desiredVisibility != null) {

                old.visibility = desiredVisibility
            }
        }
    }

    private fun setModuleVisibility(v: View, visibility: Int) {
        internalViewMutation { v.visibility = visibility }
    }

    private fun blindView(v: View?) {
        if (v == null || !shouldApplyUiHiding() || !gMasterOn || (!gControlOn && !gPlayerOn) || (gRestoreControlsOnPause && gVideoPaused)) return
        try {

            if (v.visibility != View.VISIBLE) return
            rememberViewState(v)
            val collapse = shouldCollapseControl(v)
            setModuleVisibility(v, if (collapse) View.GONE else View.INVISIBLE)
            if (collapse) {
                v.requestLayout()
                (v.parent as? View)?.requestLayout()
                LogUtil.incr("collapseControl")
            }
            LogUtil.incr("blindOK")
        } catch (_: Exception) {}
    }

    private fun restoreView(v: View?) {
        if (v == null) return
        val state = synchronized(gSavedViewStates) { gSavedViewStates.remove(v) } ?: return
        try {
            internalViewMutation {
                v.alpha = state.alpha
                v.visibility = state.visibility
            }
        } catch (_: Exception) {}
        try { v.requestLayout() } catch (_: Exception) {}
        try { (v.parent as? View)?.requestLayout() } catch (_: Exception) {}
    }

    private fun restoreAllSavedViews() {
        restoreFeedViewportBottomMargins(clear = true)
        restoreBottomLayoutReclaim()
        val entries = synchronized(gSavedViewStates) {
            val copy = gSavedViewStates.entries.map { Pair(it.key, SavedViewState(it.value.visibility, it.value.alpha)) }
            gSavedViewStates.clear()
            copy
        }
        for ((v, state) in entries) {
            try {
                internalViewMutation {
                    v.alpha = state.alpha
                    v.visibility = state.visibility
                }
                v.requestLayout()
                (v.parent as? View)?.requestLayout()
            } catch (_: Throwable) {}
        }
        synchronized(gForcedShortMaskVisibility) { gForcedShortMaskVisibility.clear() }
        synchronized(gCleanMaskViews) { gCleanMaskViews.clear() }
        if (entries.isNotEmpty()) LogUtil.info("restore saved views: ${entries.size}")
    }
    private fun forceOnePauseRestoreView(v: View?) {
        if (v == null) return
        try {
            synchronized(gPauseForcedStates) {
                if (!gPauseForcedStates.containsKey(v)) {
                    gPauseForcedStates[v] = PauseForcedState(
                        v.visibility, v.alpha, v.translationX, v.translationY,
                    )
                }
            }
            internalViewMutation {
                v.visibility = View.VISIBLE
                v.alpha = 1f
                v.translationX = 0f
                v.translationY = 0f
            }
        } catch (_: Throwable) {}
    }

    private fun shouldRestoreOnPause(id: Int): Boolean {
        if (gControlOn && gTargetIdSet.contains(id)) return true
        if (gPlayerOn && gSeriesTargetIdSet.contains(id)) return true
        if (gProgressOff && gProgressIdSet.contains(id)) return true
        return false
    }

    private fun scanTreePauseRestore(v: View?) {
        if (v == null) return
        try {
            val id = v.id
            if (id > 0 && gPauseRestoreIdSet.contains(id) && shouldRestoreOnPause(id)) {
                forceOnePauseRestoreView(v)
            }
        } catch (_: Throwable) {}
        if (v is ViewGroup) for (i in 0 until v.childCount) scanTreePauseRestore(v.getChildAt(i))
    }

    private fun forcePauseEpisodeSelector(root: View?) {
        if (root == null) return
        if (root is TextView) {
            val text = try { root.text?.toString()?.trim().orEmpty() } catch (_: Throwable) { "" }
            val hit = text.contains("选集") || text.equals("Episodes", true) || text.startsWith("Episode", true)
            if (hit) {
                forceOnePauseRestoreView(root)

                var node: View? = root.parent as? View
                var depth = 0
                var candidate: View? = null
                while (node != null && depth++ < 4) {
                    if (node is ViewGroup && node.width > 0 && node.height > 0) {
                        val density = try { node.resources.displayMetrics.density.coerceAtLeast(0.1f) } catch (_: Throwable) { 1f }
                        val h = node.height / density
                        if (h in 30f..64f) { candidate = node; break }
                    }
                    node = node.parent as? View
                }
                if (candidate != null) {
                    forceOnePauseRestoreView(candidate)
                    rememberSeriesToolbar(candidate, 2)
                }
            }
        }
        if (root is ViewGroup) for (i in 0 until root.childCount) forcePauseEpisodeSelector(root.getChildAt(i))
    }

    private fun forcePauseRightAgencyTree(agency: Any) {
        val rightRoot = findFieldValue(agency, "p") as? View ?: return
        forceOnePauseRestoreView(rightRoot)
        scanTreePauseRestore(rightRoot)

        val decor = try { gCurrentActivity?.window?.decorView } catch (_: Throwable) { null }
        var node = rightRoot.parent as? View
        var depth = 0
        while (node != null && depth++ < 3) {
            if (node === decor) break
            val suppressed = try {
                node.visibility != View.VISIBLE ||
                    node.alpha < 0.99f ||
                    java.lang.Math.abs(node.translationX) > 1f ||
                    java.lang.Math.abs(node.translationY) > 1f
            } catch (_: Throwable) { false }
            if (suppressed) forceOnePauseRestoreView(node)
            node = node.parent as? View
        }
    }

    private fun forcePauseRestoreControls() {
        if (!gMasterOn || !gRestoreControlsOnPause || !gVideoPaused) return

        for (root in collectAllWindows()) {
            if (!isInsideModuleUi(root)) scanTreePauseRestore(root)
        }

        if (gPlayerOn) {
            for ((toolbar, _) in knownSeriesToolbarSnapshot()) {
                if (isInsideCurrentActivityDecor(toolbar)) forceOnePauseRestoreView(toolbar)
            }
        }

        if (gControlOn) {
            for (agency in rightViewAgencySnapshot()) {
                try {
                    forcePauseRightAgencyTree(agency)
                } catch (_: Throwable) {}
            }
        }
    }

    private fun restorePauseForcedViews() {
        val entries = synchronized(gPauseForcedStates) {
            val copy = gPauseForcedStates.entries.map { Pair(it.key, it.value) }
            gPauseForcedStates.clear()
            copy
        }
        for ((v, state) in entries) {
            try {
                internalViewMutation {
                    v.visibility = state.visibility
                    v.alpha = state.alpha
                    v.translationX = state.translationX
                    v.translationY = state.translationY
                }
            } catch (_: Throwable) {}
        }
        if (entries.isNotEmpty()) LogUtil.info("pause restore temporary states restored: ${entries.size}")
    }

    private fun isRedGuoAd(v: View?): Boolean {
        if (v == null) return false
        try { if (v is TextView && (v.text?.toString() ?: "").contains("红果")) return true } catch (_: Exception) {}
        if (v is ViewGroup) for (i in 0 until v.childCount) if (isRedGuoAd(v.getChildAt(i))) return true
        return false
    }
    private fun scanTreeQuick(v: View?) {
        if (v == null || !gMasterOn || (!gControlOn && !gPlayerOn) || (gRestoreControlsOnPause && gVideoPaused)) return
        if (isInsideModuleUi(v)) return
        LogUtil.incr("scanTree")

        if (gControlOn) {
            if (isNativeMainBottomFrame(v)) { collapseNativeMainBottomFrame(v); return }
            if (isNativeVideoFeedBottomMask(v)) { collapseNativeVideoFeedBottomMask(v); return }
            if (isHomeBottomBackdropMarker(v)) { collapseHomeBottomBackdrop(v); return }
            if (isKnownBottomBackdrop(v)) { blindView(v); return }
            if (isMainBottomNavContainer(v)) { blindView(v); return }
        }
        if (quickMatch(v)) { blindView(v); return }
        if (v is ViewGroup) for (i in 0 until v.childCount) scanTreeQuick(v.getChildAt(i))
    }
    private fun restoreAllControls() {
        mainHandler.post {
            restoreFeedViewportBottomMargins(clear = false)
            restoreBottomLayoutReclaim()
            restoreShortVideoNativeControls()
            val roots = collectAllWindows()
            for (root in roots) {
                scanTreeRestore(root)
                scanTreePlayerRestore(root)
                scanTreeProgressRestore(root)
            }

            if (gRestoreControlsOnPause && gVideoPaused) forcePauseRestoreControls()
        }
    }

    private fun restoreAfterSeriesDetailExit() {
        mainHandler.post {
            if (shouldApplyUiHiding()) return@post
            restoreShortVideoNativeControls()
            restorePauseForcedViews()
            restoreAllSavedViews()
            setVideoToolbarsVisible(true)
            applyCleanTop(gCurrentActivity)
            applyNavBar(gCurrentActivity)
        }
    }

    private fun registerShortVideoHolder(holder: Any?, playbackState: Int? = null) {
        if (holder == null) return
        val now = android.os.SystemClock.uptimeMillis()
        gLastHolderBindAt = now
        synchronized(gShortVideoHolders) {
            gShortVideoHolders.removeAll { it.ref.get() == null }
            val old = gShortVideoHolders.firstOrNull { it.ref.get() === holder }
            if (old != null) {
                gShortVideoHolders.remove(old)
                if (playbackState != null) old.playbackState = playbackState
                old.updatedAt = now
                gShortVideoHolders.add(old)
            } else {
                gShortVideoHolders.add(
                    ShortVideoHolderState(
                        java.lang.ref.WeakReference(holder),
                        playbackState ?: 0,
                        now,
                    )
                )
            }
        }
    }

    private fun shortVideoHolderSnapshot(): List<Pair<Any, Int>> = synchronized(gShortVideoHolders) {
        val result = mutableListOf<Pair<Any, Int>>()
        val iterator = gShortVideoHolders.iterator()
        while (iterator.hasNext()) {
            val item = iterator.next()
            val holder = item.ref.get()
            if (holder == null) iterator.remove() else result.add(Pair(holder, item.playbackState))
        }
        result
    }

    private fun findFieldValue(instance: Any, fieldName: String): Any? {
        var clazz: Class<*>? = instance.javaClass
        while (clazz != null) {
            try {
                return clazz.getDeclaredField(fieldName).apply { isAccessible = true }.get(instance)
            } catch (_: Throwable) {}
            clazz = clazz.superclass
        }
        return null
    }

    private fun registerRightViewAgency(agency: Any?) {
        if (agency == null) return
        synchronized(gRightViewAgencies) {
            val it = gRightViewAgencies.iterator()
            while (it.hasNext()) {
                val current = it.next().get()
                if (current == null || current === agency) it.remove()
            }
            gRightViewAgencies.add(java.lang.ref.WeakReference(agency))
        }
    }

    private fun rightViewAgencySnapshot(): List<Any> = synchronized(gRightViewAgencies) {
        val result = mutableListOf<Any>()
        val it = gRightViewAgencies.iterator()
        while (it.hasNext()) {
            val agency = it.next().get()
            if (agency == null) it.remove() else result.add(agency)
        }
        result
    }

    private fun rightViewAgencyRoot(agency: Any): View? = findFieldValue(agency, "p") as? View

    private fun isRightViewAgencyVisible(agency: Any): Boolean {
        val rightView = rightViewAgencyRoot(agency) ?: return false
        return try {
            rightView.windowToken != null && rightView.width > 0 && rightView.height > 0
        } catch (_: Throwable) { false }
    }

    private fun isAgencyInCurrentActivityWindow(agency: Any): Boolean {
        val rightView = rightViewAgencyRoot(agency) ?: return false
        val currentToken = try { gCurrentActivity?.window?.decorView?.windowToken } catch (_: Throwable) { null }

        return try {
            if (currentToken == null) {
                rightView.windowToken != null && rightView.isAttachedToWindow
            } else {
                rightView.windowToken === currentToken && rightView.isAttachedToWindow
            }
        } catch (_: Throwable) { false }
    }

    private fun openCurrentCommentFromDoubleTap(): Boolean {
        if (!gMasterOn || !gDoubleTapCommentOn) return false
        val snapshot = rightViewAgencySnapshot()
        if (snapshot.isEmpty()) return false

        val currentWindow = snapshot.filter { isAgencyInCurrentActivityWindow(it) }.asReversed()
        val visibleFallback = snapshot.filter { isRightViewAgencyVisible(it) }.asReversed()
        val candidates = if (currentWindow.isNotEmpty()) currentWindow else visibleFallback

        for (agency in candidates) {
            try {
                var clazz: Class<*>? = agency.javaClass
                var method: java.lang.reflect.Method? = null
                while (clazz != null && method == null) {
                    method = try {
                        clazz.getDeclaredMethod(
                            gNames.rightViewAgencyEventMethod,
                            android.os.Bundle::class.java,
                            String::class.java,
                        ).apply { isAccessible = true }
                    } catch (_: Throwable) { null }
                    clazz = clazz.superclass
                }
                if (method == null) continue
                method.invoke(agency, android.os.Bundle(), "show_comment_dialog")
                LogUtil.incr("doubleTapOpenComment")
                LogUtil.info("双击评论事件已发送 | profile=${gNames.profileId} | agency=${agency.javaClass.name}.${gNames.rightViewAgencyEventMethod}")
                return true
            } catch (e: Throwable) {
                LogUtil.warn("双击打开评论区失败(${agency.javaClass.name}): $e")
            }
        }
        return false
    }

    private fun registerShortSeriesFragment(fragment: Any?) {
        if (fragment == null) return
        synchronized(gShortSeriesFragments) {
            gShortSeriesFragments.removeAll { it.get() == null }
            val old = gShortSeriesFragments.firstOrNull { it.get() === fragment }
            if (old != null) gShortSeriesFragments.remove(old)
            gShortSeriesFragments.add(java.lang.ref.WeakReference(fragment))
        }
    }

    private fun shortSeriesFragmentSnapshot(): List<Any> = synchronized(gShortSeriesFragments) {
        val result = mutableListOf<Any>()
        val iterator = gShortSeriesFragments.iterator()
        while (iterator.hasNext()) {
            val fragment = iterator.next().get()
            if (fragment == null) iterator.remove() else result.add(fragment)
        }
        result
    }

    private fun activityFromFragment(fragment: Any?): Activity? {
        if (fragment == null) return null
        return try {
            fragment.javaClass.getMethod("getActivity").invoke(fragment) as? Activity
        } catch (_: Throwable) { null }
    }

    private fun shortVideoHolderRoot(holder: Any): View? {
        try {
            val root = holder.javaClass.getMethod("getRootView").invoke(holder) as? View
            if (root != null) return root
        } catch (_: Throwable) {}
        return findFieldValue(holder, "itemView") as? View
    }

    private fun isShortVideoHolderVisible(holder: Any): Boolean {
        val root = shortVideoHolderRoot(holder) ?: return false
        return try {
            if (!root.isShown || root.windowToken == null || root.width <= 0 || root.height <= 0) return false
            val visible = Rect()
            if (!root.getGlobalVisibleRect(visible)) return false
            val visibleArea = visible.width().toLong() * visible.height().toLong()
            val totalArea = root.width.toLong() * root.height.toLong()
            visibleArea * 4L >= totalArea
        } catch (_: Throwable) { false }
    }

    private fun detectPausedFromShortVideoHolders(): Boolean? {
        for ((holder, state) in shortVideoHolderSnapshot().asReversed()) {
            if (!isShortVideoHolderVisible(holder)) continue
            when (state) {
                1 -> return false
                2 -> return true
            }
            try {
                val playing = holder.javaClass.getMethod("isVideoPlaying").invoke(holder) as? Boolean
                if (playing == true) return false
            } catch (_: Throwable) {}
        }
        return null
    }

    private fun shouldForceShortVideoCleanMask(): Boolean {
        return shouldApplyUiHiding() && gMasterOn && gControlOn && !(gRestoreControlsOnPause && gVideoPaused)
    }

    private fun rememberAndForceShortVideoMaskInvisible(holder: Any) {
        try {
            val mask = findFieldValue(holder, gNames.shortMaskField) as? View ?: return
            if (mask.visibility != View.VISIBLE) return
            synchronized(gForcedShortMaskVisibility) {
                if (!gForcedShortMaskVisibility.containsKey(mask)) {
                    gForcedShortMaskVisibility[mask] = mask.visibility
                }
            }
            rememberViewState(mask)
            synchronized(gCleanMaskViews) { gCleanMaskViews[mask] = true }
            setModuleVisibility(mask, View.INVISIBLE)
            LogUtil.incr("shortMaskDirectFallback")
        } catch (e: Throwable) {
            LogUtil.warn("force short-video mask invisible failed: ${holder.javaClass.name}: $e")
        }
    }

    private fun restoreForcedShortVideoMask(holder: Any) {
        try {
            val mask = findFieldValue(holder, gNames.shortMaskField) as? View ?: return
            val original = synchronized(gForcedShortMaskVisibility) {
                gForcedShortMaskVisibility.remove(mask)
            } ?: return

            synchronized(gCleanMaskViews) { gCleanMaskViews.remove(mask) }
            val saved = synchronized(gSavedViewStates) { gSavedViewStates[mask] }
            if (saved != null) restoreView(mask) else internalViewMutation { mask.visibility = original }
            LogUtil.incr("shortMaskDirectRestore")
        } catch (e: Throwable) {
            LogUtil.warn("restore short-video mask failed: ${holder.javaClass.name}: $e")
        }
    }

    private fun setOneShortVideoMaskClear(holder: Any, clear: Boolean) {
        try {

            if (clear) rememberAndForceShortVideoMaskInvisible(holder)
            else restoreForcedShortVideoMask(holder)
        } catch (e: Throwable) {
            LogUtil.warn("set short-video mask clear=$clear failed: ${holder.javaClass.name}: $e")
        }
    }

    private fun syncShortVideoMasks() {
        val holders = shortVideoHolderSnapshot().asReversed()
        if (shouldForceShortVideoCleanMask()) {
            var handledVisibleHolder = false
            for ((holder, _) in holders) {
                if (!isShortVideoHolderVisible(holder)) continue
                setOneShortVideoMaskClear(holder, true)
                handledVisibleHolder = true
            }

            if (!handledVisibleHolder) holders.firstOrNull()?.first?.let {
                setOneShortVideoMaskClear(it, true)
            }
            return
        }

        for ((holder, _) in holders) restoreForcedShortVideoMask(holder)
    }

    private fun registerCnHomeFragment(fragment: Any?) {
        if (fragment == null || gPkg != "com.phoenix.read") return
        synchronized(gCnHomeFragments) {
            gCnHomeFragments.removeAll { it.get() == null }
            val old = gCnHomeFragments.firstOrNull { it.get() === fragment }
            if (old != null) gCnHomeFragments.remove(old)
            gCnHomeFragments.add(java.lang.ref.WeakReference(fragment))
        }
    }

    private fun cnHomeFragmentSnapshot(): List<Any> = synchronized(gCnHomeFragments) {
        val result = mutableListOf<Any>()
        val it = gCnHomeFragments.iterator()
        while (it.hasNext()) {
            val item = it.next().get()
            if (item == null) it.remove() else result.add(item)
        }
        result
    }

    private fun forceCnHomeFragmentMaskInvisible(fragment: Any) {
        if (gPkg != "com.phoenix.read" || !shouldForceShortVideoCleanMask()) return
        try {
            val fieldName = gNames.homeFragmentMaskField
            if (fieldName.isBlank()) return
            val mask = findFieldValue(fragment, fieldName) as? View ?: return
            if (mask.visibility != View.VISIBLE) return
            rememberViewState(mask)
            synchronized(gCleanMaskViews) { gCleanMaskViews[mask] = true }
            setModuleVisibility(mask, View.INVISIBLE)
            LogUtil.incr("homeFragmentMaskHide")
        } catch (e: Throwable) {
            LogUtil.warn("home fragment mask hide failed: $e")
        }
    }

    private fun syncCnHomeFragmentMasks() {
        if (gPkg != "com.phoenix.read") return
        if (shouldForceShortVideoCleanMask()) {
            for (fragment in cnHomeFragmentSnapshot().asReversed()) {
                forceCnHomeFragmentMaskInvisible(fragment)
            }
        } else {
            for (fragment in cnHomeFragmentSnapshot().asReversed()) {
                try {
                    val fieldName = gNames.homeFragmentMaskField
                    if (fieldName.isBlank()) continue
                    val mask = findFieldValue(fragment, fieldName) as? View ?: continue
                    restoreView(mask)
                } catch (_: Throwable) {}
            }
        }
    }

    private fun setOneShortVideoControlsVisible(holder: Any, visible: Boolean) {
        if (!visible) return
        var managerHandled = false
        try {
            val cleanScreenManager = findFieldValue(holder, gNames.shortCleanManagerField)
            if (cleanScreenManager != null) {
                cleanScreenManager.javaClass.getDeclaredMethod("b", Boolean::class.java).apply {
                    isAccessible = true
                }.invoke(cleanScreenManager, false)
                managerHandled = true
            }
        } catch (e: Throwable) {
            LogUtil.warn("exit short-video clean screen failed: $e")
        }

        try {
            holder.javaClass.getMethod(gNames.shortControlsMethod, Boolean::class.java, Boolean::class.java)
                .invoke(holder, false, true)
        } catch (e: Throwable) {
            if (!managerHandled) {
                LogUtil.warn("show short-video controls failed: ${holder.javaClass.name}: $e")
            }
        }
    }

    /**
     * 隐藏短剧 Holder 的全部控制层（含横屏全屏页左右两侧的锁屏 / 亮度 / 音量）。
     *
     * 与 [setOneShortVideoControlsVisible] 严格对称 —— 那条链路把控制层「显示」出来，
     * 但**恢复播放时只隐藏了工具栏层**（gVideoToolbarLayers 里只有 ToolbarLayerFixed /
     * CustomizeToolbarLayer，不含宿主自管的横屏侧边按钮），于是被「顺手」显示出来的
     * 锁屏 / 亮度 / 音量再没有任何路径收回去 → 常驻不消失。
     *
     * 触发场景（2026-09-14 实测定位）：进横屏全屏页时宿主重建播放器会产生约 1ms 的
     * 瞬时 pause→resume。因为用户刚点过「全屏观看」按钮，暂停恢复的 baseDelay 落在
     * isUserClick 分支（=0）→ 当帧就执行 restoreAllControls()；而紧接着的 resume
     * 分支只管工具栏层 → 三个按钮留在屏幕上。关闭「暂停后恢复所有控件」即恢复正常
     * （已用 A/B 对照验证），确认是这条链路。
     *
     * 修法：对称调用同一个宿主入口 `sd(true, *)`，让宿主自己把所有控制层收回去 ——
     * 不去逐个猜每个按钮归哪个管理器持有，行为与宿主自身一致。
     */
    private fun setOneShortVideoControlsHidden(holder: Any) {
        if (gNames.shortControlsMethod.isBlank()) {
            LogUtil.warn("hide short-video native controls: shortControlsMethod 为空，跳过")
            return
        }
        try {
            holder.javaClass.getMethod(
                gNames.shortControlsMethod,
                Boolean::class.java,
                Boolean::class.java,
            ).invoke(holder, true, true)
            LogUtil.incr("shortNativeControlsHide")
        } catch (e: Throwable) {
            LogUtil.warn("hide short-video native controls failed: ${holder.javaClass.name}: $e")
        }
    }

    // 「暂停恢复」会把宿主控制层（含横屏侧边按钮）显示出来，而宿主**不会**替模块收回
    // （程序化显示不会启动宿主自身的自动隐藏计时器）—— 必须由模块自己补收。
    // 但**不能用一次性的 post**：进全屏 / 切集会在几十毫秒内再翻转一次 paused，
    // 单次 post 执行时被守卫拦下后就**永久丢失**（历史现象：恢复播放后侧边按钮仍常驻）。
    // 故用 pending 标记跨暂停保留，直到在「播放态」真正收回成功为止。
    private var gNativeControlsHidePending = false

    private fun hideShortVideoNativeControls(force: Boolean = false) {
        if (!gMasterOn) return
        gNativeControlsHidePending = true
        mainHandler.post { tryHideShortVideoNativeControls(force) }
    }

    // 「暂停恢复」的补收计时器。宿主只会为**用户真实点击**触发的显示启动自动隐藏计时器；
    // 模块用 sd(false,true) 程序化显示的控制层宿主不会计时 → 若这次暂停是进全屏 / 切集
    // 产生的瞬时 pause（非用户点击），侧边按钮就会永久残留（实测 50s+ 不消失）。
    // 故模块自己补一个与宿主一致的「静默 4s 后收回」。
    private var gPauseRestoreRetractRunnable: Runnable? = null

    private fun schedulePauseRestoreRetract() {
        gPauseRestoreRetractRunnable?.let { mainHandler.removeCallbacks(it) }
        val r = object : Runnable {
            override fun run() {
                gPauseRestoreRetractRunnable = null
                if (!gMasterOn || !gRestoreControlsOnPause || !gVideoPaused) return
                // 用户正在操作（拖进度条 / 点按钮）→ 不抢控件，等静默后再收。
                if (android.os.SystemClock.uptimeMillis() - gLastUserClickAt < 2500L) {
                    schedulePauseRestoreRetract()
                    return
                }
                LogUtil.info("pause restore auto-retract: hide native controls")
                hideShortVideoNativeControls(force = true)
            }
        }
        gPauseRestoreRetractRunnable = r
        mainHandler.postDelayed(r, 4000L)
    }

    // ── 侧边控件（锁屏 / 亮度 / 音量）的低频自愈看护 ────────────────────────────
    // 为什么需要：模块此前只有「pause → resume」这一个收回触发点，但**宿主自己**也会显示
    // 控制层 —— 用户点一下屏幕空白处，宿主就把控制层显示出来。而宿主的自动隐藏计时器在这个
    // 状态下并不可靠。2026-09-14 实测（ebb811b）：
    //   18:26:23.956 播放 → 模块在 resume 分支收回成功（像素 0.00）
    //   18:26:26.674 用户点空白处 → 三按钮重新出现，此后**模块日志一行都没有**
    //   18:29:0x     已过 3 分钟仍可见（像素 1.39/1.20/1.24）；再点一次空白处才收回
    // 即：这类残留模块全程无动作，只能靠用户再点一次 —— 与「控件该自己消失」的预期不符。
    // 做法：不再依赖任何单次事件（历史教训：一次性 post 会被状态翻转吞掉；按 12s 窗口也会漏掉
    // 宿主发起的显示），改成周期性核对**视图地真**：横屏全屏页 + 播放态 + 用户静默 + 侧边按钮
    // 仍可见 → 补收。节奏与宿主自身对工具栏的自动隐藏一致（实测宿主 4~9s）。
    @Volatile private var gSideWatchRunning = false
    private val gSideWatchIntervalMs = 1000L
    private val gSideWatchIdleMs = 3000L

    // ID 用**名称**解析：资源 ID 是 aapt 生成的，每次发版整体漂移，名称才稳定。
    private val nativeSideControlNames = arrayOf(
        "full_screen_lock_view",
        "full_screen_brightness_control",
        "full_screen_volume_control",
    )

    /** 三个侧边按钮里只要有一个可见，就说明宿主控制层没收回去。 */
    private fun nativeSideControlsVisible(): Boolean {
        val act = gCurrentActivity ?: return false
        return try {
            for (name in nativeSideControlNames) {
                val id = act.resources.getIdentifier(name, "id", gPkg)
                if (id == 0) continue
                val v = act.findViewById<View>(id) ?: continue
                if (v.visibility == View.VISIBLE && v.alpha > 0.01f) return true
            }
            false
        } catch (_: Throwable) {
            false
        }
    }

    private fun startSideControlsWatchIfNeeded() {
        if (gSideWatchRunning) return
        gSideWatchRunning = true
        val r = object : Runnable {
            override fun run() {
                try {
                    sideControlsWatchTick()
                } catch (_: Throwable) {
                }
                mainHandler.postDelayed(this, gSideWatchIntervalMs)
            }
        }
        mainHandler.postDelayed(r, gSideWatchIntervalMs)
    }

    private fun sideControlsWatchTick() {
        if (!gMasterOn) return
        val act = gCurrentActivity ?: return
        if (!isLandscapeFullscreenActivity(act)) return
        if (!nativeSideControlsVisible()) return
        // 暂停态控件本就该显示；用户正在操作时不抢。
        if (gVideoPaused) return
        if (android.os.SystemClock.uptimeMillis() - gLastUserClickAt < gSideWatchIdleMs) return
        LogUtil.info("side controls idle retract: hide native controls")
        hideShortVideoNativeControls()
    }

    private fun tryHideShortVideoNativeControls(force: Boolean = false) {
        if (!gMasterOn) {
            gNativeControlsHidePending = false
            return
        }
        val snap = shortVideoHolderSnapshot()
        // 门禁必须与「显示侧」严格对称 —— 这是本 bug 的核心。
        // 显示链路 restoreAllControls() → restoreShortVideoNativeControls() → sd(false,true)
        // **没有任何门禁**（不查 gControlOn / gPlayerOn / shouldApplyUiHiding）。
        // 隐藏侧若额外加门，就会出现「显示侧放行、隐藏侧被拦」→ 控件常驻。
        // 因此只保留总开关 + 「当前不是暂停态」（暂停时控件本就该显示；force 用于暂停态补收）。
        if (gVideoPaused && !force) {
            // 又回到暂停态：保留 pending，等下一次 resume 再补收（绝不丢）。
            return
        }
        var handled = false
        for ((holder, _) in snap.asReversed()) {
            if (!isShortVideoHolderVisible(holder)) continue
            setOneShortVideoControlsHidden(holder)
            handled = true
        }
        if (!handled) snap.firstOrNull()?.first?.let { setOneShortVideoControlsHidden(it) }
        gNativeControlsHidePending = false
    }

    private fun restoreShortVideoNativeControls() {
        val holders = shortVideoHolderSnapshot().asReversed()
        var restoredVisibleHolder = false
        for ((holder, _) in holders) {
            if (!isShortVideoHolderVisible(holder)) continue
            setOneShortVideoControlsVisible(holder, true)
            restoreForcedShortVideoMask(holder)
            shortVideoHolderRoot(holder)?.requestLayout()
            restoredVisibleHolder = true
        }

        if (!restoredVisibleHolder) holders.firstOrNull()?.first?.let {
            setOneShortVideoControlsVisible(it, true)
            restoreForcedShortVideoMask(it)
        }
    }

    private fun registerVideoToolbarLayer(layer: Any?) {
        if (layer == null) return
        var added = false
        synchronized(gVideoToolbarLayers) {
            gVideoToolbarLayers.removeAll { it.get() == null }
            val old = gVideoToolbarLayers.firstOrNull { it.get() === layer }
            if (old != null) {

                gVideoToolbarLayers.remove(old)
            } else {
                added = true
            }
            gVideoToolbarLayers.add(java.lang.ref.WeakReference(layer))
        }
        if (added) LogUtil.info("video layer registered: ${layer.javaClass.name}")
        if (added && gMasterOn && gPlayerOn) {
            // 新构造图层默认可见：构造hook内同步隐藏（此时View未挂树未绘制，零闪现）；用户暂停窗口保持可见
            val userPauseWindow = gRestoreControlsOnPause && gVideoPaused && !isEpisodeSwitchPause()
            val show = !userPauseWindow
            if (android.os.Looper.myLooper() == mainHandler.looper) setOneVideoToolbarVisible(layer, show)
            else mainHandler.post { setOneVideoToolbarVisible(layer, show) }
        }
    }
    private fun videoToolbarLayerSnapshot(): List<Any> = synchronized(gVideoToolbarLayers) {
        val result = mutableListOf<Any>()
        val iterator = gVideoToolbarLayers.iterator()
        while (iterator.hasNext()) {
            val layer = iterator.next().get()
            if (layer == null) iterator.remove() else result.add(layer)
        }
        result
    }

    private fun registerToolbarBaseLayer(layer: Any?) {
        if (layer == null) return
        synchronized(gToolbarBaseLayers) {
            gToolbarBaseLayers.removeAll { it.get() == null }
            val old = gToolbarBaseLayers.firstOrNull { it.get() === layer }
            if (old != null) gToolbarBaseLayers.remove(old)
            gToolbarBaseLayers.add(java.lang.ref.WeakReference(layer))
        }
    }

    private fun toolbarBaseLayerSnapshot(): List<Any> = synchronized(gToolbarBaseLayers) {
        val out = mutableListOf<Any>()
        val it = gToolbarBaseLayers.iterator()
        while (it.hasNext()) {
            val v = it.next().get()
            if (v == null) it.remove() else out.add(v)
        }
        out
    }

    private fun setToolbarBaseVisible(visible: Boolean) {
        for (layer in toolbarBaseLayerSnapshot()) {
            try {
                var type: Class<*>? = layer.javaClass
                var method: java.lang.reflect.Method? = null
                while (type != null && method == null) {
                    method = try { type.getDeclaredMethod("a", Boolean::class.java) } catch (_: Throwable) { null }
                    type = type.superclass
                }
                method?.apply { isAccessible = true }?.invoke(layer, visible)
            } catch (_: Throwable) {}
        }
    }

    private fun setOneVideoToolbarVisible(layer: Any, visible: Boolean) {
        try {
            when (layer.javaClass.name) {
                "com.dragon.read.pages.video.layers.toolbarlayer.ToolbarLayerFixed" ->
                    layer.javaClass.getDeclaredMethod(gNames.fixedToolbarShowMethod, Boolean::class.java).apply { isAccessible = true }.invoke(layer, visible)
                "com.dragon.read.pages.video.customizelayers.CustomizeToolbarLayer" ->
                    layer.javaClass.getDeclaredMethod(gNames.customizeToolbarShowMethod, Boolean::class.java).apply { isAccessible = true }.invoke(layer, visible)
            }
        } catch (e: Throwable) {
            LogUtil.warn("set video toolbar visible=$visible failed: ${layer.javaClass.name}: $e")
        }
    }

    private fun setVideoToolbarsVisible(visible: Boolean) {
        if (!visible && !shouldApplyUiHiding()) return
        mainHandler.post {
            for (layer in videoToolbarLayerSnapshot()) {
                setOneVideoToolbarVisible(layer, visible)

                if (visible && layer.javaClass.name == "com.dragon.read.pages.video.customizelayers.CustomizeToolbarLayer") {
                    try {
                        layer.javaClass.getDeclaredMethod(
                            gNames.customizeToolbarApplyMethod,
                            Boolean::class.java,
                            Boolean::class.java,
                            Boolean::class.java,
                        ).apply { isAccessible = true }.invoke(layer, true, false, false)
                    } catch (_: Throwable) {}
                }
            }
            setToolbarBaseVisible(visible)
        }
    }

    private fun detectPausedFromVideoLayers(): Boolean? {

        for (layer in videoToolbarLayerSnapshot().asReversed()) {
            try {
                val inquirer = layer.javaClass.getMethod("getVideoStateInquirer").invoke(layer) ?: continue
                val paused = inquirer.javaClass.getMethod("isPaused").invoke(inquirer) as? Boolean ?: false
                if (paused) return true
                val playing = inquirer.javaClass.getMethod("isPlaying").invoke(inquirer) as? Boolean ?: false
                if (playing) return false
            } catch (_: Throwable) {}
        }
        return null
    }
    private fun refreshVideoPauseState(reason: String, fallback: Boolean? = null) {
        mainHandler.postDelayed({
            val shortVideoState = detectPausedFromShortVideoHolders()
            val detected = shortVideoState ?: detectPausedFromVideoLayers() ?: fallback
            if (detected != null) {
                val source = if (shortVideoState != null) "short-holder" else "layer"
                setVideoPaused(detected, "$reason/$source")
            }
            else LogUtil.warn("video state undetermined: $reason")
        }, 60L)
    }

    @Volatile private var gPauseRestoreRunnable: Runnable? = null
    @Volatile private var gVideoStateEverSet = false
    @Volatile private var gLastHolderBindAt = 0L
    @Volatile private var gPauseStartedAt = 0L
    @Volatile private var gLastPlayAt = 0L
    @Volatile private var gLastVideoModelAt = 0L
    @Volatile private var gLastUserClickAt = 0L
    @Volatile private var gTouchDownX = 0f
    @Volatile private var gTouchDownY = 0f

    // 暂停后出现新视频模型/新holder绑定 = 自动连播切集窗口，此时不放行播放器控制栏显示
    private fun isEpisodeSwitchPause(): Boolean =
        gPauseStartedAt > 0L && (gLastVideoModelAt > gPauseStartedAt || gLastHolderBindAt > gPauseStartedAt)

    private fun setVideoPaused(paused: Boolean, reason: String = "callback") {
        val now = android.os.SystemClock.uptimeMillis()
        val userPausedAfterPlay = gLastUserClickAt > gLastPlayAt && now - gLastUserClickAt < 800L
        if (paused && !reason.contains("completed") &&
            !userPausedAfterPlay &&
            now - gLastPlayAt < 600L && now - gLastVideoModelAt < 1500L
        ) {
            LogUtil.info("stale pause ignored after new-video play: reason=$reason")
            return
        }
        if (!paused) gLastPlayAt = now
        val first = !gVideoStateEverSet
        val changed = first || gVideoPaused != paused
        gVideoStateEverSet = true
        gVideoPaused = paused
        gLastVideoStateAt = android.os.SystemClock.uptimeMillis()
        gLastVideoStateReason = reason
        if (changed) LogUtil.info("setVideoPaused: paused=$paused reason=$reason restoreOnPause=$gRestoreControlsOnPause")

        if (!gMasterOn || !gRestoreControlsOnPause) return
        if (paused) {
            if (!changed) return
            gPauseStartedAt = android.os.SystemClock.uptimeMillis()
            gPauseRestoreRunnable?.let { mainHandler.removeCallbacks(it) }
            gPauseRestoreRetractRunnable?.let { mainHandler.removeCallbacks(it) }
            gPauseRestoreRetractRunnable = null
            val isCompletedPause = reason.contains("completed")
            val isUserClick = android.os.SystemClock.uptimeMillis() - gLastUserClickAt < 800L
            val baseDelay = when {
                isUserClick -> 0L
                isCompletedPause -> 3000L
                else -> 900L
            }
            if (baseDelay > 0L) LogUtil.info("pause restore delay=${baseDelay}ms (completed=$isCompletedPause click=$isUserClick reason=$reason)")
            val r = object : Runnable {
                override fun run() {
                    if (!gMasterOn || !gRestoreControlsOnPause || !gVideoPaused) return
                    if (!isUserClick) {
                        val now = android.os.SystemClock.uptimeMillis()
                        val sinceBind = now - gLastHolderBindAt
                        val newModelAfterPause = gLastVideoModelAt > gPauseStartedAt
                        if (sinceBind < 2500L || newModelAfterPause) {
                            LogUtil.info("video paused during episode switch(bind=${sinceBind}ms newModel=$newModelAfterPause), defer restore")
                            mainHandler.postDelayed(this, 400L)
                            return
                        }
                    }
                    restoreAllControls()
                    restoreNativeBottomWindowColor(gCurrentActivity)
                    setVideoToolbarsVisible(true)
                    forcePauseRestoreControls()
                    LogUtil.info("video paused: restore controls, reason=$reason")

                    mainHandler.postDelayed({
                        if (gMasterOn && gRestoreControlsOnPause && gVideoPaused &&
                            gLastVideoModelAt > gPauseStartedAt
                        ) {
                            LogUtil.info("pause restore re-check: hide controls again")
                            mainHandler.post { scanAllWindows() }
                        }
                    }, 2500L)

                    // 「暂停恢复」显示的是模块**程序化**调起的控制层，宿主不会为它启动自动隐藏
                    // 计时器（只有真实点击才会）→ 非点击场景下侧边按钮会永久残留（实测 50s+）。
                    // 故模块自己补一个与宿主一致的「静默 4s 后收回」；用户正在操作时自动让路。
                    schedulePauseRestoreRetract()
                }
            }
            gPauseRestoreRunnable = r
            mainHandler.postDelayed(r, baseDelay)
        } else {
            val hadPauseRestore = gPauseRestoreRunnable != null
            gPauseRestoreRunnable?.let { mainHandler.removeCallbacks(it) }
            gPauseRestoreRunnable = null
            gPauseRestoreRetractRunnable?.let { mainHandler.removeCallbacks(it) }
            gPauseRestoreRetractRunnable = null
            restorePauseForcedViews()
            if (gPlayerOn) setVideoToolbarsVisible(false)
            // 对称补偿：暂停恢复曾把控制层（含横屏侧边按钮）显示出来，而上面只隐藏了工具栏层。
            // hadPauseRestore：本次暂停确实安排过恢复 → 需要补收；
            // pending：上一次补收被瞬时 pause 拦下未完成 → 借这次 resume 重试，避免永久丢失。
            if (hadPauseRestore || gNativeControlsHidePending) hideShortVideoNativeControls()
            if (changed) {
                LogUtil.info("video resumed: hide controls, reason=$reason")
                if (first) {
                    mainHandler.postDelayed({ if (!gVideoPaused) scanAllWindows() }, 1200L)
                    mainHandler.postDelayed({ if (!gVideoPaused) scanAllWindows() }, 3000L)
                }
            }
            mainHandler.post { scanAllWindows() }
        }
    }
    private fun scanTreeRestore(v: View?) {
        if (v == null || isInsideModuleUi(v)) return
        if ((quickMatch(v) || isKnownMainBottomNav(v) || isKnownFullSeriesEntry(v) || isKnownBottomBackdrop(v)) && !isRedGuoAd(v)) restoreView(v)
        if (v is ViewGroup) for (i in 0 until v.childCount) scanTreeRestore(v.getChildAt(i))
    }

    private fun isProgressBar(v: View?): Boolean {
        if (v == null) return false
        try { val id = v.id; if (id > 0 && gProgressIdSet.contains(id)) return true } catch (_: Exception) {}
        try { if (v.javaClass.name == gNames.progressBar) return true } catch (_: Exception) {}
        return false
    }
    private fun scanTreeProgress(v: View?) {
        if (v == null || !gMasterOn || !gProgressOff) return
        if (isProgressBar(v)) {
            try {
                if (v.visibility == View.VISIBLE) {
                    rememberViewState(v)
                    setModuleVisibility(v, View.GONE)
                    LogUtil.incr("progressHide")
                }
            } catch (_: Exception) {}
            return
        }
        if (v is ViewGroup) for (i in 0 until v.childCount) scanTreeProgress(v.getChildAt(i))
    }
    private fun scanTreeProgressRestore(v: View?) {
        if (v == null) return
        if (isProgressBar(v)) restoreView(v)
        if (v is ViewGroup) for (i in 0 until v.childCount) scanTreeProgressRestore(v.getChildAt(i))
    }

    private fun collectAllWindows(): MutableList<View> {
        val roots = mutableListOf<View>()
        try {
            val wmgClass = Class.forName("android.view.WindowManagerGlobal")
            val instance = wmgClass.getMethod("getInstance").invoke(null)
            val mViewsField = wmgClass.getDeclaredField("mViews")
            mViewsField.isAccessible = true
            val mViews = mViewsField.get(instance) as? ArrayList<View>
            if (mViews != null) roots.addAll(mViews)
        } catch (_: Throwable) {

        }
        try {
            val decor = gCurrentActivity?.window?.decorView
            if (decor != null && roots.none { it === decor }) roots.add(decor)
        } catch (_: Throwable) {}
        return roots
    }
    private fun scanTreeUnified(v: View?) {
        if (v == null || isInsideModuleUi(v)) return
        LogUtil.incr("scanTree")

        val masterActive = shouldApplyUiHiding() && gMasterOn && !(gRestoreControlsOnPause && gVideoPaused)
        if (masterActive) {
            if ((gControlOn || gPlayerOn) && quickMatch(v)) { blindView(v); return }
            if (gPlayerOn && seriesToolbarKind(v) != 0) { hideSeriesToolbarView(v); return }
            if (gProgressOff && isProgressBar(v)) {
                if (v.visibility == View.VISIBLE) {
                    rememberViewState(v)
                    setModuleVisibility(v, View.GONE)
                }
                return
            }
            if (gRefreshOff && isRefreshAccessoryContainer(v)) { hideRefreshAccessory(v); return }
        } else {
            if ((quickMatch(v) || isKnownFullSeriesEntry(v)) && !isRedGuoAd(v)) {
                restoreView(v)
            }
            if (isProgressBar(v)) restoreView(v)
        }

        if (v is ViewGroup) {
            for (i in 0 until v.childCount) scanTreeUnified(v.getChildAt(i))
        }
    }

    private fun scanAllWindows() {
        try {
            val act = gCurrentActivity ?: return
            val decor = try { act.window?.decorView } catch (_: Exception) { null } ?: return
            ensureResourceIdsResolved(decor)

            scanTreeUnified(decor)

            if (gControlOn) {
                syncShortVideoMasks()
                syncCnHomeFragmentMasks()
            }
            if (gMasterOn && gControlOn && !(gRestoreControlsOnPause && gVideoPaused)) {
                enforceNativeMainBottomHidden(act)
                enforceKnownFeedViewportBottomMargin()
            }
        } catch (_: Exception) {}
    }

    private var gScanRunnable: Runnable? = null
    private fun startPeriodicScan() {
        stopPeriodicScan()
    }
    private fun stopPeriodicScan() {
        gScanRunnable?.let { mainHandler.removeCallbacks(it) }
        gScanRunnable = null
    }
    private fun applyToCurrent() { mainHandler.post { scanAllWindows() } }

    private fun isWindowedMode(act: Activity?): Boolean {
        if (act == null) return false
        try {
            if (Build.VERSION.SDK_INT >= 24 && act.isInMultiWindowMode) return true
            if (Build.VERSION.SDK_INT >= 26 && act.isInPictureInPictureMode) return true

            if (Build.VERSION.SDK_INT >= 30) {
                val current = act.windowManager.currentWindowMetrics.bounds
                val maximum = act.windowManager.maximumWindowMetrics.bounds
                if (maximum.width() > 0 && maximum.height() > 0) {
                    val widthReduced = current.width().toLong() * 100L < maximum.width().toLong() * 88L
                    val heightReduced = current.height().toLong() * 100L < maximum.height().toLong() * 88L
                    if (widthReduced || heightReduced) return true
                }
            }
        } catch (_: Throwable) {}
        return false
    }

    private fun shouldHideStatusBar(): Boolean {
        return shouldApplyUiHiding() && gMasterOn && gStatusOn
    }

    private fun updateSeriesMallTopMargin(act: Activity, statusHidden: Boolean) {
        try {

            val topBarId = act.resources.getIdentifier("is7", "id", gPkg)
            if (topBarId == 0) return
            val topBar = act.findViewById<View>(topBarId) ?: return
            val params = topBar.layoutParams as? ViewGroup.MarginLayoutParams ?: return
            if (statusHidden) {
                synchronized(gSavedTopMargins) {
                    if (!gSavedTopMargins.containsKey(topBar)) gSavedTopMargins[topBar] = params.topMargin
                }
                if (params.topMargin != 0) {
                    params.topMargin = 0
                    topBar.layoutParams = params
                    topBar.requestLayout()
                }
            } else {
                val original = synchronized(gSavedTopMargins) { gSavedTopMargins.remove(topBar) } ?: return
                if (params.topMargin != original) {
                    params.topMargin = original
                    topBar.layoutParams = params
                    topBar.requestLayout()
                }
            }
        } catch (_: Exception) {}
    }

    private fun refreshOneShortVideoHolder(
        holder: Any,
        configuration: android.content.res.Configuration,
    ) {
        try {
            holder.javaClass.getMethod(gNames.shortConfigMethod, android.content.res.Configuration::class.java)
                .invoke(holder, configuration)

            holder.javaClass.getMethod(gNames.shortLayoutResetMethod).invoke(holder)
        } catch (_: Throwable) {}
        shortVideoHolderRoot(holder)?.requestLayout()
    }

    private fun refreshShortVideoWindowLayout(act: Activity) {
        val configuration = act.resources.configuration

        for (fragment in shortSeriesFragmentSnapshot().asReversed()) {
            if (activityFromFragment(fragment) !== act) continue
            for (fieldName in gNames.seriesLayoutFields) {
                val view = findFieldValue(fragment, fieldName) as? View ?: continue
                try { view.requestLayout() } catch (_: Throwable) {}
            }
            try {
                val pager = fragment.javaClass.getMethod(gNames.seriesPagerGetter).invoke(fragment) ?: continue
                val holder = pager.javaClass.getMethod(gNames.seriesHolderGetter).invoke(pager) ?: continue
                registerShortVideoHolder(holder)
                refreshOneShortVideoHolder(holder, configuration)
            } catch (_: Throwable) {}
        }

        for ((holder, _) in shortVideoHolderSnapshot().asReversed()) {
            if (!isShortVideoHolderVisible(holder)) continue
            refreshOneShortVideoHolder(holder, configuration)
        }
    }

    private fun applyWindowedTop(act: Activity) {
        try {
            val window = act.window ?: return
            val decor = window.decorView

            fun applyOnce() {

                window.clearFlags(
                    WindowManager.LayoutParams.FLAG_FULLSCREEN or
                        WindowManager.LayoutParams.FLAG_TRANSLUCENT_STATUS
                )
                window.addFlags(WindowManager.LayoutParams.FLAG_DRAWS_SYSTEM_BAR_BACKGROUNDS)
                if (Build.VERSION.SDK_INT >= 28) {
                    val attrs = window.attributes
                    attrs.layoutInDisplayCutoutMode =
                        WindowManager.LayoutParams.LAYOUT_IN_DISPLAY_CUTOUT_MODE_DEFAULT
                    window.attributes = attrs
                }

                if (Build.VERSION.SDK_INT >= 30) {

                    @Suppress("DEPRECATION")
                    decor.systemUiVisibility = decor.systemUiVisibility and
                        View.SYSTEM_UI_FLAG_LAYOUT_FULLSCREEN.inv() and
                        View.SYSTEM_UI_FLAG_FULLSCREEN.inv() and
                        View.SYSTEM_UI_FLAG_IMMERSIVE_STICKY.inv()
                    window.setDecorFitsSystemWindows(false)
                    window.statusBarColor = Color.TRANSPARENT
                    decor.windowInsetsController?.apply {
                        setSystemBarsAppearance(0, WindowInsetsController.APPEARANCE_LIGHT_STATUS_BARS)
                        if (Build.VERSION.SDK_INT >= 35) {
                            setSystemBarsAppearance(
                                WindowInsetsController.APPEARANCE_TRANSPARENT_CAPTION_BAR_BACKGROUND,
                                WindowInsetsController.APPEARANCE_TRANSPARENT_CAPTION_BAR_BACKGROUND,
                            )
                        }
                        var topTypes = WindowInsets.Type.statusBars() or WindowInsets.Type.captionBar()
                        if (Build.VERSION.SDK_INT >= 34) {
                            topTypes = topTypes or WindowInsets.Type.systemOverlays()
                        }
                        hide(topTypes)
                        systemBarsBehavior =
                            WindowInsetsController.BEHAVIOR_SHOW_TRANSIENT_BARS_BY_SWIPE
                    }
                } else {

                    @Suppress("DEPRECATION")
                    decor.systemUiVisibility = decor.systemUiVisibility or
                        View.SYSTEM_UI_FLAG_LAYOUT_STABLE or
                        View.SYSTEM_UI_FLAG_LAYOUT_FULLSCREEN or
                        View.SYSTEM_UI_FLAG_FULLSCREEN or
                        View.SYSTEM_UI_FLAG_IMMERSIVE_STICKY
                    window.statusBarColor = Color.BLACK
                }
                decor.requestApplyInsets()
                updateSeriesMallTopMargin(act, true)
            }

            applyOnce()

            for (delay in longArrayOf(120L, 360L)) mainHandler.postDelayed({
                if (!act.isFinishing && isWindowedMode(act) && gMasterOn && gStatusOn) {
                    applyOnce()
                    refreshShortVideoWindowLayout(act)
                }
            }, delay)
        } catch (e: Throwable) {
            LogUtil.warn("apply windowed mode failed: $e")
        }
    }

    private fun reapplyAfterWindowModeChange(
        act: Activity?,
        reason: String,
        refreshLayout: Boolean = true,
    ) {
        if (act == null || !gMasterOn || act.isFinishing) return
        val windowed = isWindowedMode(act)
        if (gLastWindowedMode != windowed) {
            gLastWindowedMode = windowed
            LogUtil.info("window mode -> ${if (windowed) "windowed" else "fullscreen"}, reason=$reason")
        }
        if (gStatusOn) applyCleanTop(act) else showStatusBar(act)
        if (gNavBarOff) applyNavBar(act) else showNavBar(act)
        if (refreshLayout) refreshShortVideoWindowLayout(act)
    }

    @Volatile private var gLastSeriesPageReapply = 0L
    @Volatile private var gSeriesReapplyPendingRunnable: Runnable? = null

    private fun reapplySeriesPageState(
        owner: Any?,
        reason: String,
        refreshLayout: Boolean = true,
    ) {
        val act = owner as? Activity ?: activityFromFragment(owner) ?: return
        val now = android.os.SystemClock.uptimeMillis()
        if (now - gLastSeriesPageReapply < 150L) {
            gSeriesReapplyPendingRunnable?.let { mainHandler.removeCallbacks(it) }
            val r = Runnable {
                reapplyAfterWindowModeChange(act, reason, refreshLayout)
                scanAllWindows()
            }
            gSeriesReapplyPendingRunnable = r
            mainHandler.postDelayed(r, 150L)
            return
        }
        gLastSeriesPageReapply = now
        gSeriesReapplyPendingRunnable?.let { mainHandler.removeCallbacks(it) }
        gSeriesReapplyPendingRunnable = null

        reapplyAfterWindowModeChange(act, reason, refreshLayout)
        scanAllWindows()
    }

    private fun applyCleanTop(act: Activity?) {
        if (act == null || !isSeriesDetailActivity(act)) return
        if (!shouldHideStatusBar()) {
            if (!isLandscapeFullscreenActivity(act)) {
                showStatusBar(act)
            }
            return
        }
        if (isWindowedMode(act)) {
            applyWindowedTop(act)
            return
        }
        try {
            val window = act.window ?: return
            val decor = window.decorView

            if (Build.VERSION.SDK_INT >= 30) {
                window.setDecorFitsSystemWindows(false)
            }
            if (Build.VERSION.SDK_INT >= 28) {
                val attrs = window.attributes
                attrs.layoutInDisplayCutoutMode = if (Build.VERSION.SDK_INT >= 30) {
                    WindowManager.LayoutParams.LAYOUT_IN_DISPLAY_CUTOUT_MODE_ALWAYS
                } else {
                    WindowManager.LayoutParams.LAYOUT_IN_DISPLAY_CUTOUT_MODE_SHORT_EDGES
                }
                window.attributes = attrs
            }

            window.clearFlags(WindowManager.LayoutParams.FLAG_TRANSLUCENT_STATUS)
            window.addFlags(
                WindowManager.LayoutParams.FLAG_FULLSCREEN or
                    WindowManager.LayoutParams.FLAG_DRAWS_SYSTEM_BAR_BACKGROUNDS
            )
            window.statusBarColor = Color.TRANSPARENT

            if (Build.VERSION.SDK_INT >= 30) {
                decor.windowInsetsController?.apply {
                    hide(WindowInsets.Type.statusBars())
                    systemBarsBehavior = WindowInsetsController.BEHAVIOR_SHOW_TRANSIENT_BARS_BY_SWIPE
                }
            }

            @Suppress("DEPRECATION")
            decor.systemUiVisibility = decor.systemUiVisibility or
                View.SYSTEM_UI_FLAG_LAYOUT_STABLE or
                View.SYSTEM_UI_FLAG_LAYOUT_FULLSCREEN or
                View.SYSTEM_UI_FLAG_FULLSCREEN or
                View.SYSTEM_UI_FLAG_IMMERSIVE_STICKY
            decor.requestApplyInsets()
            updateSeriesMallTopMargin(act, true)
        } catch (_: Exception) {}
    }
    private fun showStatusBar(act: Activity?) {
        if (act == null || !isSeriesDetailActivity(act) || isLandscapeFullscreenActivity(act)) return
        try {
            val window = act.window ?: return
            val decor = window.decorView
            window.clearFlags(WindowManager.LayoutParams.FLAG_FULLSCREEN)
            if (Build.VERSION.SDK_INT >= 30) {

                window.setDecorFitsSystemWindows(false)
                var topTypes = WindowInsets.Type.statusBars()
                if (isWindowedMode(act)) topTypes = topTypes or WindowInsets.Type.captionBar()
                decor.windowInsetsController?.show(topTypes)
            }
            if (Build.VERSION.SDK_INT >= 28) {
                val attrs = window.attributes
                attrs.layoutInDisplayCutoutMode = WindowManager.LayoutParams.LAYOUT_IN_DISPLAY_CUTOUT_MODE_DEFAULT
                window.attributes = attrs
            }
            @Suppress("DEPRECATION")
            decor.systemUiVisibility = decor.systemUiVisibility and
                View.SYSTEM_UI_FLAG_LAYOUT_FULLSCREEN.inv() and
                View.SYSTEM_UI_FLAG_FULLSCREEN.inv() and
                View.SYSTEM_UI_FLAG_IMMERSIVE_STICKY.inv()
            decor.requestApplyInsets()
            updateSeriesMallTopMargin(act, false)
        } catch (_: Exception) {}
    }

    private fun applyBottomEdgeToEdge(act: Activity?, hideNavigationBar: Boolean) {
        if (act == null) return
        try {
            val window = act.window ?: return
            val decor = window.decorView
            if (Build.VERSION.SDK_INT >= 30) {
                window.setDecorFitsSystemWindows(false)
            }
            window.addFlags(WindowManager.LayoutParams.FLAG_DRAWS_SYSTEM_BAR_BACKGROUNDS)
            window.navigationBarColor = Color.TRANSPARENT
            if (Build.VERSION.SDK_INT >= 29) {
                try { window.isNavigationBarContrastEnforced = false } catch (_: Throwable) {}
            }
            if (Build.VERSION.SDK_INT >= 30) {
                decor.windowInsetsController?.apply {
                    if (hideNavigationBar) hide(WindowInsets.Type.navigationBars())
                    systemBarsBehavior = WindowInsetsController.BEHAVIOR_SHOW_TRANSIENT_BARS_BY_SWIPE
                }
            }
            @Suppress("DEPRECATION")
            decor.systemUiVisibility = decor.systemUiVisibility or
                View.SYSTEM_UI_FLAG_LAYOUT_STABLE or
                View.SYSTEM_UI_FLAG_LAYOUT_HIDE_NAVIGATION or
                View.SYSTEM_UI_FLAG_IMMERSIVE_STICKY or
                (if (hideNavigationBar) View.SYSTEM_UI_FLAG_HIDE_NAVIGATION else 0)
            decor.requestApplyInsets()
        } catch (e: Throwable) {
            LogUtil.warn("apply bottom edge-to-edge failed: $e")
        }
    }

    private fun extendKnownBottomContentRoot(act: Activity?, expectedGapPx: Int) {
        if (act == null || expectedGapPx <= 0) return
        try {
            val id = act.resources.getIdentifier("hsw", "id", gPkg)
            if (id == 0) return
            val content = act.findViewById<View>(id) ?: return
            val parent = content.parent as? View ?: return
            val lp = content.layoutParams ?: return
            val contentLoc = IntArray(2)
            val parentLoc = IntArray(2)
            content.getLocationOnScreen(contentLoc)
            parent.getLocationOnScreen(parentLoc)
            val contentH = if (content.height > 0) content.height else content.measuredHeight
            val parentH = if (parent.height > 0) parent.height else parent.measuredHeight
            if (contentH <= 0 || parentH <= 0) return
            val desired = parentLoc[1] + parentH - contentLoc[1]
            val gap = desired - contentH
            val minGap = (expectedGapPx * 0.55f).toInt().coerceAtLeast(1)
            val maxGap = (expectedGapPx * 1.8f).toInt().coerceAtLeast(expectedGapPx)
            if (gap in minGap..maxGap && lp.height != desired) {
                rememberBottomLayoutState(content)
                lp.height = desired
                content.layoutParams = lp
                content.requestLayout()
                (content.parent as? View)?.requestLayout()
                LogUtil.info("hsw bottom extended: ${contentH}px -> ${desired}px, gap=${gap}px")
                LogUtil.incr("bottomHswExtend")
            }
        } catch (e: Throwable) {
            LogUtil.warn("extend hsw bottom failed: $e")
        }
    }

    private fun applyNavBar(act: Activity?) {
        if (act == null || !isSeriesDetailActivity(act)) return
        if (!shouldApplyUiHiding() || !gMasterOn || !gNavBarOff) {
            if (!isLandscapeFullscreenActivity(act)) {
                showNavBar(act)
            }
            return
        }
        applyBottomEdgeToEdge(act, true)
    }
    private fun showNavBar(act: Activity?) {
        if (act == null || !isSeriesDetailActivity(act) || isLandscapeFullscreenActivity(act)) return
        try {
            val decor = act.window.decorView
            if (Build.VERSION.SDK_INT >= 30) decor.windowInsetsController?.show(WindowInsets.Type.navigationBars())
            else @Suppress("DEPRECATION") decor.systemUiVisibility = decor.systemUiVisibility and View.SYSTEM_UI_FLAG_HIDE_NAVIGATION.inv()
        } catch (_: Exception) {}
    }

    private fun resolutionRank(resolution: Any?): Int {
        if (resolution == null) return Int.MIN_VALUE
        val enumName = try { (resolution as? Enum<*>)?.name ?: "" } catch (_: Throwable) { "" }
        val text = try { (enumName + " " + resolution.toString()).lowercase() } catch (_: Throwable) { enumName.lowercase() }
        if ("auto" in text || "undefine" in text) return Int.MIN_VALUE

        return when {
            "eightk" in text || "8k" in text -> 8000
            "fourk" in text || "4k" in text || "2160" in text -> 4000
            "twok" in text || "2k" in text || "1440" in text -> 2000
            "extremelyhighplus" in text || "1080p+" in text -> 1081
            "extremelyhigh" in text || "1080" in text -> 1080
            "superhigh" in text || "720" in text -> 720
            "h_high" in text || "540" in text -> 540

            enumName.equals("High", true) || " 480p" in " $text" -> 480
            "l_standard" in text || "240" in text -> 240
            "standard" in text || "360" in text -> 360
            else -> {

                try {
                    val m = resolution.javaClass.methods.firstOrNull { it.name == "getIndex" && it.parameterCount == 0 }
                    ((m?.invoke(resolution) as? Number)?.toInt() ?: 0) - 10000
                } catch (_: Throwable) { -10000 }
            }
        }
    }

    @Volatile private var supportResolutionsMethodCache: java.lang.reflect.Method? = null
    @Volatile private var configResolutionMethodCache: java.lang.reflect.Method? = null

    private fun findHighestResolution(model: Any?): Any? {
        if (model == null || !gMasterOn || !gMaxQualityOn) return null
        return try {
            val getter = supportResolutionsMethodCache ?: model.javaClass.methods.firstOrNull {
                it.name == "getSupportResolutions" && it.parameterCount == 0
            }?.also { supportResolutionsMethodCache = it } ?: return null
            val array = getter.invoke(model) ?: return null
            val count = java.lang.reflect.Array.getLength(array)
            var best: Any? = null
            var bestRank = Int.MIN_VALUE
            for (i in 0 until count) {
                val r = java.lang.reflect.Array.get(array, i) ?: continue
                val rank = resolutionRank(r)
                if (rank > bestRank) {
                    bestRank = rank
                    best = r
                }
            }
            best
        } catch (_: Throwable) {
            null
        }
    }

    @Volatile private var videoInfoListMethodCache: java.lang.reflect.Method? = null

    private fun findVideoInfoByResolution(model: Any?, highest: Any?): Any? {
        if (model == null || highest == null) return null
        return try {
            val listGetter = videoInfoListMethodCache ?: model.javaClass.methods.firstOrNull {
                it.name == "getVideoInfoList" && it.parameterCount == 0
            }?.also { videoInfoListMethodCache = it } ?: return null
            val list = listGetter.invoke(model) as? List<*> ?: return null
            list.firstOrNull { info ->
                try {
                    val res = info?.javaClass?.methods?.firstOrNull { it.name == "getResolution" && it.parameterCount == 0 }?.invoke(info)
                    res === highest
                } catch (_: Throwable) { false }
            }
        } catch (_: Throwable) { null }
    }

    private fun rememberAndApplyHighestResolution(engine: Any?, model: Any?) {
        if (engine == null || model == null || !gMasterOn || !gMaxQualityOn) return
        val highest = findHighestResolution(model) ?: return
        try {
            gEngineMaxResolution[engine] = highest
        } catch (_: Throwable) {}
        try {
            val method = configResolutionMethodCache ?: engine.javaClass.methods.firstOrNull {
                it.name == "configResolution" && it.parameterCount == 1 &&
                    it.parameterTypes[0].isInstance(highest)
            }?.also { configResolutionMethodCache = it } ?: return
            method.invoke(engine, highest)
            LogUtil.incr("maxQualityApply")
        } catch (_: Throwable) {}
    }

    private fun applyHighestViaController(controller: Any?, highest: Any?): Boolean {
        if (controller == null || highest == null || gNames.resolutionApplyMethod.isBlank()) return false
        return try {
            var clazz: Class<*>? = controller.javaClass
            var target: java.lang.reflect.Method? = null
            while (clazz != null && target == null) {
                target = clazz.declaredMethods.firstOrNull {
                    it.name == gNames.resolutionApplyMethod && it.parameterCount == 1 &&
                        it.parameterTypes[0].isInstance(highest)
                }?.apply { isAccessible = true }
                clazz = clazz.superclass
            }
            if (target == null) return false
            target.invoke(controller, highest)
            LogUtil.info("最高画质：原生控制器 ${gNames.resolutionApplyMethod}($highest)")
            LogUtil.incr("maxQualityNativeApply")
            true
        } catch (e: Throwable) {
            LogUtil.warn("最高画质：原生控制器应用失败: $e")
            false
        }
    }

    private fun applyHighestToKnownEngines() {
        if (!gMasterOn || !gMaxQualityOn) return

        val controllers = try {
            synchronized(gControllerMaxResolution) { gControllerMaxResolution.entries.map { it.key to it.value } }
        } catch (_: Throwable) { emptyList() }
        for ((controller, highest) in controllers) applyHighestViaController(controller, highest)

        val engines = try {
            synchronized(gEngineMaxResolution) { gEngineMaxResolution.entries.map { it.key to it.value } }
        } catch (_: Throwable) { emptyList() }
        for ((engine, highest) in engines) {
            try {
                val method = engine.javaClass.methods.firstOrNull {
                    it.name == "configResolution" && it.parameterCount == 1 &&
                        it.parameterTypes[0].isInstance(highest)
                } ?: continue
                method.invoke(engine, highest)
                LogUtil.incr("maxQualityApply")
            } catch (_: Throwable) {}
        }
    }

    private fun createNotification(ctx: Context) {
        try {
            val nm = ctx.getSystemService(Context.NOTIFICATION_SERVICE) as NotificationManager
            if (!gNotificationMenuOn) {
                nm.cancel(9999)
                return
            }
            if (Build.VERSION.SDK_INT >= 26) nm.createNotificationChannel(NotificationChannel("lspilot_toggle", "KEJIYU", NotificationManager.IMPORTANCE_LOW).apply { setSound(null, null) })
            val flag = if (Build.VERSION.SDK_INT >= 23) PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE else PendingIntent.FLAG_UPDATE_CURRENT
            fun pi(code: Int, action: String) = PendingIntent.getBroadcast(ctx, code, Intent(action).apply { setPackage(gPkg); addFlags(Intent.FLAG_RECEIVER_FOREGROUND) }, flag)
            val n = if (Build.VERSION.SDK_INT >= 26) android.app.Notification.Builder(ctx, "lspilot_toggle") else @Suppress("DEPRECATION") android.app.Notification.Builder(ctx)
            n.setContentTitle("KEJIYU 模块"); n.setContentText(if (gMasterOn) "已启用" else "已停用"); n.setSmallIcon(android.R.drawable.ic_menu_view); n.setOngoing(true)
            n.setContentIntent(pi(9999, "$gPkg.LSPilot.TOGGLE"))
            n.addAction(android.R.drawable.ic_menu_view, if (gMasterOn) "隐藏" else "显示", pi(10001, "$gPkg.LSPilot.TOGGLE"))
            n.addAction(android.R.drawable.ic_menu_edit, "面板", pi(10002, "$gPkg.LSPilot.OPEN_PANEL"))
            n.addAction(android.R.drawable.ic_menu_close_clear_cancel, "重启", pi(10003, "$gPkg.LSPilot.RESTART"))
            nm.notify(9999, n.build())
        } catch (e: Exception) { LogUtil.error("通知", e) }
    }
    private fun ensureReceiver(ctx: Context) {
        if (gReceiverRegistered) return
        try {
            val receiver = object : BroadcastReceiver() {
                override fun onReceive(context: Context, intent: Intent) {
                    try { when (intent.action) { "$gPkg.LSPilot.TOGGLE" -> doToggle(ctx); "$gPkg.LSPilot.OPEN_PANEL" -> openPanel(); "$gPkg.LSPilot.RESTART" -> restartApp(ctx) } } catch (e: Exception) { LogUtil.error("receiver", e) }
                }
            }
            val filter = IntentFilter("$gPkg.LSPilot.TOGGLE").apply { addAction("$gPkg.LSPilot.OPEN_PANEL"); addAction("$gPkg.LSPilot.RESTART") }
            if (Build.VERSION.SDK_INT >= 33) ctx.registerReceiver(receiver, filter, Context.RECEIVER_NOT_EXPORTED)
            else ctx.registerReceiver(receiver, filter)
            gReceiverRegistered = true
        } catch (e: Exception) { LogUtil.error("receiver", e) }
    }
    private fun doToggle(ctx: Context) {
        gMasterOn = !gMasterOn
        savePref("master_on", gMasterOn)
        if (!gMasterOn) {
            restoreAllSavedViews()
            restoreShortVideoNativeControls()
            setVideoToolbarsVisible(true)
        }
        createNotification(ctx)
        applyToCurrent()
        LogUtil.info("总开关 → $gMasterOn")
    }
    private fun restartApp(ctx: Context) { try { val i = ctx.packageManager.getLaunchIntentForPackage(gPkg); if (i != null) { i.addFlags(Intent.FLAG_ACTIVITY_NEW_TASK or Intent.FLAG_ACTIVITY_CLEAR_TASK); ctx.startActivity(i) }; mainHandler.postDelayed({ android.os.Process.killProcess(android.os.Process.myPid()) }, 1000) } catch (_: Exception) {} }

    private data class PanelPalette(
        val page: Int,
        val surface: Int,
        val surfaceAlt: Int,
        val text: Int,
        val textSecondary: Int,
        val accent: Int,
        val accentEnd: Int,
        val accentSoft: Int,
        val divider: Int,
        val switchOffThumb: Int,
        val switchOffTrack: Int,
        val switchOnTrack: Int,
    )

    private fun safeRgb(r: Int, g: Int, b: Int): Int = Color.rgb(r, g, b)
    private fun safeArgb(a: Int, r: Int, g: Int, b: Int): Int = Color.argb(a, r, g, b)
    private fun dp(ctx: Context, value: Float): Int = (value * ctx.resources.displayMetrics.density + 0.5f).toInt()

    private fun panelPalette(ctx: Context): PanelPalette {
        val night = ((ctx.resources.configuration.uiMode and 0x30) == 0x20)
        gIsNight = night
        return if (night) {
            // 晴空白 · 暖墨夜色：深而不闷，层与层之间保持可感知的明度阶梯
            PanelPalette(
                page = safeRgb(20, 18, 16),
                surface = safeRgb(30, 27, 24),
                surfaceAlt = safeRgb(38, 34, 31),
                text = safeRgb(245, 243, 240),
                textSecondary = safeRgb(168, 158, 148),
                accent = safeRgb(255, 122, 92),
                accentEnd = safeRgb(255, 154, 122),
                accentSoft = safeRgb(58, 36, 30),
                divider = safeArgb(26, 255, 255, 255),
                switchOffThumb = safeRgb(212, 206, 200),
                switchOffTrack = safeRgb(88, 80, 74),
                switchOnTrack = safeRgb(160, 82, 62),
            )
        } else {
            // 晴空白 · 暖白纸面：明亮清爽，告别灰暗
            PanelPalette(
                page = safeRgb(250, 249, 248),
                surface = safeRgb(255, 255, 255),
                surfaceAlt = safeRgb(245, 243, 241),
                text = safeRgb(28, 25, 23),
                textSecondary = safeRgb(138, 131, 124),
                accent = safeRgb(255, 90, 60),
                accentEnd = safeRgb(255, 138, 92),
                accentSoft = safeRgb(255, 237, 232),
                divider = safeArgb(20, 0, 0, 0),
                switchOffThumb = safeRgb(248, 247, 246),
                switchOffTrack = safeRgb(207, 200, 194),
                switchOnTrack = safeRgb(255, 168, 150),
            )
        }
    }

    private fun roundedBg(ctx: Context, color: Int, radiusDp: Float, strokeColor: Int? = null, strokeDp: Float = 0f): GradientDrawable {
        return GradientDrawable().apply {
            shape = GradientDrawable.RECTANGLE
            setColor(color)
            cornerRadius = dp(ctx, radiusDp).toFloat()
            if (strokeColor != null && strokeDp > 0f) setStroke(dp(ctx, strokeDp), strokeColor)
        }
    }

    /** 品牌朱砂渐变：全面板仅主动作按钮使用，一处点睛 */
    private fun accentGradientBg(ctx: Context, p: PanelPalette, radiusDp: Float): GradientDrawable {
        return GradientDrawable(GradientDrawable.Orientation.LEFT_RIGHT, intArrayOf(p.accent, p.accentEnd)).apply {
            shape = GradientDrawable.RECTANGLE
            cornerRadius = dp(ctx, radiusDp).toFloat()
        }
    }

    private fun styleSwitch(sw: Switch, p: PanelPalette) {
        try {
            val states = arrayOf(
                intArrayOf(android.R.attr.state_checked),
                intArrayOf(-android.R.attr.state_checked),
            )
            sw.thumbTintList = ColorStateList(states, intArrayOf(p.accent, p.switchOffThumb))
            sw.trackTintList = ColorStateList(states, intArrayOf(p.switchOnTrack, p.switchOffTrack))
        } catch (_: Throwable) {

        }
    }

    private fun sectionTitle(ctx: Context, title: String, p: PanelPalette): TextView = TextView(ctx).apply {
        text = title
        textSize = 12f
        setTypeface(Typeface.DEFAULT, Typeface.BOLD)
        setTextColor(p.textSecondary)
        letterSpacing = 0.06f
        isAllCaps = false
        setPadding(dp(ctx, 4f), dp(ctx, 18f), dp(ctx, 4f), dp(ctx, 5f))
    }

    private fun makePanelRow(
        ctx: Context,
        title: String,
        description: String,
        checked: Boolean,
        p: PanelPalette,
        emphasis: Boolean = false,
        hasSubmenu: Boolean = false,
    ): Pair<LinearLayout, Switch> {
        val row = LinearLayout(ctx).apply {
            orientation = LinearLayout.HORIZONTAL
            gravity = Gravity.CENTER_VERTICAL
            setPadding(dp(ctx, 15f), dp(ctx, 12f), dp(ctx, 12f), dp(ctx, 12f))
            background = roundedBg(ctx, if (emphasis) p.accentSoft else p.surfaceAlt, 14f)
            minimumHeight = dp(ctx, 64f)
        }
        val labels = LinearLayout(ctx).apply {
            orientation = LinearLayout.VERTICAL
            gravity = Gravity.CENTER_VERTICAL
        }
        labels.addView(TextView(ctx).apply {
            text = title
            textSize = 15f
            setTypeface(Typeface.DEFAULT, if (emphasis) Typeface.BOLD else Typeface.NORMAL)
            setTextColor(p.text)
            includeFontPadding = false
        })
        labels.addView(TextView(ctx).apply {
            text = description
            textSize = 12f
            setTextColor(p.textSecondary)
            includeFontPadding = false
            setPadding(0, dp(ctx, 4f), dp(ctx, 6f), 0)
        })
        row.addView(labels, LinearLayout.LayoutParams(0, ViewGroup.LayoutParams.WRAP_CONTENT, 1f))
        if (hasSubmenu) {

            val handle = LinearLayout(ctx).apply {
                orientation = LinearLayout.HORIZONTAL
                gravity = Gravity.CENTER
                setPadding(dp(ctx, 5f), 0, dp(ctx, 4f), 0)
            }
            val heights = floatArrayOf(13f, 19f, 15f)
            for (height in heights) {
                handle.addView(View(ctx).apply {
                    background = roundedBg(ctx, p.textSecondary, 1.5f)
                }, LinearLayout.LayoutParams(dp(ctx, 2f), dp(ctx, height)).apply {
                    leftMargin = dp(ctx, 1.5f)
                    rightMargin = dp(ctx, 1.5f)
                })
            }
            row.addView(handle, LinearLayout.LayoutParams(dp(ctx, 28f), dp(ctx, 30f)))
        }
        val sw = Switch(ctx).apply {
            isChecked = checked
            setShowText(false)
            setPadding(dp(ctx, 8f), 0, 0, 0)
        }
        styleSwitch(sw, p)
        row.addView(sw, LinearLayout.LayoutParams(ViewGroup.LayoutParams.WRAP_CONTENT, ViewGroup.LayoutParams.WRAP_CONTENT))
        return row to sw
    }

    private fun showDefaultSpeedEditor(act: Activity, p: PanelPalette, onSelected: (Float) -> Unit) {
        try {
            val dialog = Dialog(act)
            dialog.requestWindowFeature(android.view.Window.FEATURE_NO_TITLE)
            val root = LinearLayout(act).apply {
                orientation = LinearLayout.VERTICAL
                setPadding(dp(act, 18f), dp(act, 18f), dp(act, 18f), dp(act, 16f))
                background = roundedBg(act, p.page, 24f, p.divider, 1f)
                markAsModuleUi(this)
            }
            root.addView(TextView(act).apply {
                text = "默认倍速"
                textSize = 20f
                setTypeface(Typeface.DEFAULT, Typeface.BOLD)
                setTextColor(p.text)
                includeFontPadding = false
            })
            root.addView(TextView(act).apply {
                text = "选择后立即生效，并记住为以后播放的默认速度"
                textSize = 12f
                setTextColor(p.textSecondary)
                includeFontPadding = false
                setPadding(0, dp(act, 6f), 0, dp(act, 10f))
            })

            DEFAULT_SPEED_OPTIONS.forEach { speed ->
                val selected = kotlin.math.abs(speed - gDefaultSpeed) < 0.001f
                val row = LinearLayout(act).apply {
                    orientation = LinearLayout.HORIZONTAL
                    gravity = Gravity.CENTER_VERTICAL
                    setPadding(dp(act, 15f), dp(act, 12f), dp(act, 15f), dp(act, 12f))
                    background = roundedBg(act, if (selected) p.accentSoft else p.surfaceAlt, 14f)
                    minimumHeight = dp(act, 50f)
                }
                row.addView(TextView(act).apply {
                    text = formatSpeed(speed)
                    textSize = 15f
                    setTypeface(Typeface.DEFAULT, if (selected) Typeface.BOLD else Typeface.NORMAL)
                    setTextColor(if (selected) p.accent else p.text)
                    includeFontPadding = false
                }, LinearLayout.LayoutParams(0, ViewGroup.LayoutParams.WRAP_CONTENT, 1f))
                row.addView(TextView(act).apply {
                    text = if (selected) "✓" else ""
                    textSize = 18f
                    setTypeface(Typeface.DEFAULT, Typeface.BOLD)
                    setTextColor(p.accent)
                    gravity = Gravity.CENTER
                    includeFontPadding = false
                }, LinearLayout.LayoutParams(dp(act, 30f), ViewGroup.LayoutParams.WRAP_CONTENT))
                row.setOnClickListener {
                    saveDefaultSpeedValue(speed)

                    if (!gDefaultSpeedOn) {
                        gDefaultSpeedOn = true
                        savePref(DEFAULT_SPEED_PREF, true)
                    }
                    onSelected(speed)
                    dialog.dismiss()
                    scheduleDefaultSpeedApply("menu-select")
                    Toast.makeText(act, "默认倍速已设为 ${formatSpeed(speed)}", Toast.LENGTH_SHORT).show()
                }
                root.addView(row, LinearLayout.LayoutParams(ViewGroup.LayoutParams.MATCH_PARENT, ViewGroup.LayoutParams.WRAP_CONTENT).apply {
                    topMargin = dp(act, 7f)
                })
            }

            dialog.setContentView(root)
            dialog.setCanceledOnTouchOutside(true)
            dialog.setOnShowListener {
                try {
                    dialog.window?.apply {
                        setBackgroundDrawable(android.graphics.drawable.ColorDrawable(Color.TRANSPARENT))
                        addFlags(WindowManager.LayoutParams.FLAG_DIM_BEHIND)
                        attributes = attributes.apply { dimAmount = 0.62f }
                        setLayout((act.resources.displayMetrics.widthPixels * 0.82f).toInt(), ViewGroup.LayoutParams.WRAP_CONTENT)
                    }
                } catch (e: Throwable) { LogUtil.error("默认倍速窗口", e) }
            }
            dialog.show()
            markAsModuleUi(dialog.window?.decorView)
        } catch (e: Throwable) {
            LogUtil.error("默认倍速编辑", e)
        }
    }

    private fun downloadLimitSummary(): String {
        val (episode, series, total) = currentDownloadLimitValues()
        return "单日集数 $episode  ·  单日剧数 $series  ·  总缓存 $total"
    }

    private fun showDownloadLimitEditor(act: Activity, p: PanelPalette, onSaved: () -> Unit) {
        try {
            val (episodeNow, seriesNow, totalNow) = currentDownloadLimitValues()
            val dialog = Dialog(act)
            dialog.requestWindowFeature(android.view.Window.FEATURE_NO_TITLE)

            val root = LinearLayout(act).apply {
                orientation = LinearLayout.VERTICAL
                setPadding(dp(act, 18f), dp(act, 18f), dp(act, 18f), dp(act, 16f))
                background = roundedBg(act, p.page, 24f, p.divider, 1f)
                markAsModuleUi(this)
            }
            root.addView(TextView(act).apply {
                text = "自定义下载限制"
                textSize = 20f
                setTypeface(Typeface.DEFAULT, Typeface.BOLD)
                setTextColor(p.text)
                includeFontPadding = false
            })
            root.addView(TextView(act).apply {
                text = "默认 99999。测试时可以改成 2、3 等小数值。"
                textSize = 12f
                setTextColor(p.textSecondary)
                includeFontPadding = false
                setPadding(0, dp(act, 6f), 0, dp(act, 12f))
            })

            fun field(label: String, value: Int): EditText {
                root.addView(TextView(act).apply {
                    text = label
                    textSize = 12f
                    setTypeface(Typeface.DEFAULT, Typeface.BOLD)
                    setTextColor(p.textSecondary)
                    includeFontPadding = false
                    setPadding(dp(act, 2f), dp(act, 7f), 0, dp(act, 5f))
                })
                return EditText(act).apply {
                    setText(value.toString())
                    textSize = 15f
                    setTextColor(p.text)
                    setHintTextColor(p.textSecondary)
                    inputType = android.text.InputType.TYPE_CLASS_NUMBER
                    setSingleLine(true)
                    setSelectAllOnFocus(true)
                    setPadding(dp(act, 13f), 0, dp(act, 13f), 0)
                    background = roundedBg(act, p.surfaceAlt, 12f, p.divider, 1f)
                    root.addView(this, LinearLayout.LayoutParams(
                        ViewGroup.LayoutParams.MATCH_PARENT,
                        dp(act, 48f),
                    ))
                }
            }

            val episodeField = field("单日最大下载集数", episodeNow)
            val seriesField = field("单日最大下载剧数", seriesNow)
            val totalField = field("总缓存剧数上限", totalNow)

            root.addView(TextView(act).apply {
                text = "修改数值后需要强停红果并重新打开，确保主进程和 :downloader 的缓存配置全部刷新。"
                textSize = 11f
                setTextColor(p.accent)
                includeFontPadding = false
                setPadding(dp(act, 2f), dp(act, 12f), dp(act, 2f), dp(act, 4f))
            })

            val buttons = LinearLayout(act).apply {
                orientation = LinearLayout.HORIZONTAL
                gravity = Gravity.CENTER
                setPadding(0, dp(act, 12f), 0, 0)
            }
            val cancel = TextView(act).apply {
                text = "取消"
                textSize = 14f
                gravity = Gravity.CENTER
                setTextColor(p.text)
                background = roundedBg(act, p.surfaceAlt, 14f)
                setOnClickListener { dialog.dismiss() }
            }
            val save = TextView(act).apply {
                text = "保存"
                textSize = 14f
                gravity = Gravity.CENTER
                setTypeface(Typeface.DEFAULT, Typeface.BOLD)
                setTextColor(Color.WHITE)
                background = accentGradientBg(act, p, 14f)
                setOnClickListener {
                    val episode = episodeField.text?.toString()?.trim()?.toIntOrNull()
                    val series = seriesField.text?.toString()?.trim()?.toIntOrNull()
                    val total = totalField.text?.toString()?.trim()?.toIntOrNull()
                    if (episode == null || series == null || total == null ||
                        episode !in 1..999_999_999 || series !in 1..999_999_999 || total !in 1..999_999_999
                    ) {
                        Toast.makeText(act, "请输入 1 ~ 999999999 的整数", Toast.LENGTH_SHORT).show()
                        return@setOnClickListener
                    }
                    saveDownloadLimitValues(episode, series, total)
                    LogUtil.info("download limit custom values saved: episode=$episode series=$series total=$total")
                    onSaved()
                    dialog.dismiss()
                    Toast.makeText(act, "已保存，需要强停红果后重新打开生效", Toast.LENGTH_LONG).show()
                }
            }
            buttons.addView(cancel, LinearLayout.LayoutParams(0, dp(act, 44f), 1f).apply { rightMargin = dp(act, 6f) })
            buttons.addView(save, LinearLayout.LayoutParams(0, dp(act, 44f), 1f).apply { leftMargin = dp(act, 6f) })
            root.addView(buttons)

            dialog.setContentView(root)
            dialog.setCanceledOnTouchOutside(true)
            dialog.setOnShowListener {
                try {
                    dialog.window?.apply {
                        setBackgroundDrawable(android.graphics.drawable.ColorDrawable(Color.TRANSPARENT))
                        addFlags(WindowManager.LayoutParams.FLAG_DIM_BEHIND)
                        attributes = attributes.apply { dimAmount = 0.62f }
                        setSoftInputMode(WindowManager.LayoutParams.SOFT_INPUT_ADJUST_RESIZE)
                        setLayout((act.resources.displayMetrics.widthPixels * 0.86f).toInt(), ViewGroup.LayoutParams.WRAP_CONTENT)
                    }
                } catch (e: Throwable) { LogUtil.error("下载限制编辑窗口", e) }
            }
            dialog.show()
            markAsModuleUi(dialog.window?.decorView)
        } catch (e: Throwable) {
            LogUtil.error("下载限制编辑", e)
        }
    }

    private fun targetCompatLabel(ctx: Context): String {
        try {
            @Suppress("DEPRECATION")
            val pi = ctx.packageManager.getPackageInfo(gPkg, 0)
            val version = pi.versionName ?: gTargetVersionName
            @Suppress("DEPRECATION")
            val code = if (Build.VERSION.SDK_INT >= 28) pi.longVersionCode else pi.versionCode.toLong()
            gTargetVersionName = version
            gTargetVersionCode = code
            val state = if (TargetNames.isSupported(gPkg, version)) "已适配" else "兼容表未列出"
            return "$version  ·  versionCode $code  ·  $state\n${gNames.profileId}"
        } catch (_: Throwable) {
            return "$gTargetVersionName  ·  versionCode $gTargetVersionCode\n${gNames.profileId}"
        }
    }

    private fun showPanel(ctx: Context?) {

        val act: Activity = (ctx as? Activity) ?: gCurrentActivity ?: return
        try {
            val p = panelPalette(act)
            val content = LinearLayout(act).apply {
                orientation = LinearLayout.VERTICAL
                setPadding(dp(act, 16f), dp(act, 8f), dp(act, 16f), dp(act, 18f))
                background = roundedBg(act, p.page, 24f)
                markAsModuleUi(this)
            }

            val header = LinearLayout(act).apply {
                orientation = LinearLayout.VERTICAL
                setPadding(dp(act, 6f), dp(act, 18f), dp(act, 6f), dp(act, 14f))
            }
            header.addView(TextView(act).apply {
                text = "KEJIYU"
                textSize = 25f
                setTypeface(Typeface.DEFAULT, Typeface.BOLD)
                setTextColor(p.text)
                letterSpacing = 0.08f
                includeFontPadding = false
            })
            header.addView(TextView(act).apply {
                text = "红果综合模块  ·  ${BuildConfig.VERSION_NAME}"
                textSize = 12f
                setTextColor(p.textSecondary)
                includeFontPadding = false
                setPadding(0, dp(act, 5f), 0, 0)
            })
            content.addView(header)

            content.addView(LinearLayout(act).apply {
                orientation = LinearLayout.VERTICAL
                background = roundedBg(act, p.surface, 20f, p.divider, 1f)
                setPadding(dp(act, 15f), dp(act, 13f), dp(act, 15f), dp(act, 13f))
                addView(TextView(act).apply {
                    text = "当前兼容配置"
                    textSize = 11f
                    setTypeface(Typeface.DEFAULT, Typeface.BOLD)
                    setTextColor(p.accent)
                    letterSpacing = 0.06f
                    includeFontPadding = false
                })
                addView(TextView(act).apply {
                    text = targetCompatLabel(act)
                    textSize = 12.5f
                    setTextColor(p.text)
                    includeFontPadding = false
                    setPadding(0, dp(act, 6f), 0, 0)
                })
            }, LinearLayout.LayoutParams(ViewGroup.LayoutParams.MATCH_PARENT, ViewGroup.LayoutParams.WRAP_CONTENT).apply {
                bottomMargin = dp(act, 4f)
            })

            fun addSwitch(
                title: String,
                description: String,
                getter: () -> Boolean,
                setter: (Boolean) -> Unit,
                saveKey: String,
                emphasis: Boolean = false,
                onRowAction: (() -> Unit)? = null,
            ): Pair<LinearLayout, Switch> {
                val (row, sw) = makePanelRow(act, title, description, getter(), p, emphasis, onRowAction != null)
                sw.setOnCheckedChangeListener { _, v ->
                    setter(v)
                    savePref(saveKey, v)
                    if (saveKey != "ad_block") applyToCurrent()
                }
                row.setOnClickListener {
                    if (onRowAction != null) onRowAction() else sw.isChecked = !sw.isChecked
                }
                content.addView(row, LinearLayout.LayoutParams(ViewGroup.LayoutParams.MATCH_PARENT, ViewGroup.LayoutParams.WRAP_CONTENT).apply {
                    topMargin = dp(act, 7f)
                })
                return row to sw
            }

            content.addView(sectionTitle(act, "核心", p))
            addSwitch("模块总开关", "所有功能的总控制；关闭后恢复模块修改的界面", { gMasterOn }, {
                gMasterOn = it
                if (!it) {
                    restoreAllSavedViews()
                    restoreShortVideoNativeControls()
                    restoreNativeBottomWindowColor(gCurrentActivity)
                    setVideoToolbarsVisible(true)
                }
            }, "master_on", true)

            content.addView(sectionTitle(act, "界面精简", p))
            addSwitch("隐藏状态栏", "视频页面沉浸显示", { gStatusOn }, { gStatusOn = it }, "status_bar")
            addSwitch("隐藏控件", "隐藏顶部/底部导航、作品信息和右侧互动等已适配区域", { gControlOn }, {
                gControlOn = it
                if (!it) {

                    restoreAllSavedViews()
                    restoreShortVideoNativeControls()
                    restoreNativeBottomWindowColor(gCurrentActivity)
                    mainHandler.postDelayed({ restoreShortVideoNativeControls(); scanAllWindows() }, 120L)
                }
            }, "control_hide")
            addSwitch("选集相关功能", "隐藏联播页顶部和底部的选集相关控件", { gPlayerOn }, {
                gPlayerOn = it
                if (!it || (gRestoreControlsOnPause && gVideoPaused)) setVideoToolbarsVisible(true)
                else setVideoToolbarsVisible(false)
            }, "player_bar")
            addSwitch("隐藏视频进度条", "隐藏首页和连续播放页的进度条", { gProgressOff }, {
                gProgressOff = it
                if (!it) {
                    val roots = collectAllWindows()
                    for (root in roots) scanTreeProgressRestore(root)
                }
            }, "progress_off")
            addSwitch("隐藏底部小白条", "隐藏系统手势导航提示条", { gNavBarOff }, { gNavBarOff = it }, "nav_bar_off")
            addSwitch("暂停后恢复所有控件", "暂停视频时临时恢复控件，继续播放后按规则隐藏", { gRestoreControlsOnPause }, {
                gRestoreControlsOnPause = it
                if (it) refreshVideoPauseState("switch-enabled", gVideoPaused)
                else mainHandler.post { scanAllWindows() }
            }, "restore_controls_pause")

            content.addView(sectionTitle(act, "播放与手势", p))
            if (gNames.resolutionController.isNotBlank()) {
                addSwitch("默认最高画质", "播放时自动选择当前视频支持的最高画质", { gMaxQualityOn }, {
                    gMaxQualityOn = it
                    if (it) mainHandler.post { applyHighestToKnownEngines() }
                }, "max_quality")
            }
            lateinit var defaultSpeedRow: LinearLayout
            lateinit var defaultSpeedSwitch: Switch
            val defaultSpeedPair = addSwitch(
                "默认倍速",
                defaultSpeedSummary(),
                { gDefaultSpeedOn },
                { enabled ->
                    gDefaultSpeedOn = enabled
                    if (enabled) scheduleDefaultSpeedApply("switch-enabled")
                },
                DEFAULT_SPEED_PREF,
                onRowAction = {
                    showDefaultSpeedEditor(act, p) { _ ->
                        try {
                            val labels = defaultSpeedRow.getChildAt(0) as? ViewGroup
                            (labels?.getChildAt(1) as? TextView)?.text = defaultSpeedSummary()
                            if (!defaultSpeedSwitch.isChecked) defaultSpeedSwitch.isChecked = true
                        } catch (_: Throwable) {}
                    }
                },
            )
            defaultSpeedRow = defaultSpeedPair.first
            defaultSpeedSwitch = defaultSpeedPair.second
            addSwitch("双击打开评论区", "替换原双击点赞动作，双击直接打开评论", { gDoubleTapCommentOn }, { gDoubleTapCommentOn = it }, "double_tap_comment")
            addSwitch("禁用下拉刷新", "禁用下拉手势，并折叠下拉刷新提示区域", { gRefreshOff }, {
                gRefreshOff = it
                if (!it) restoreRefreshAccessories() else mainHandler.post { scanAllWindows() }
            }, "pull_refresh")
            addSwitch("顶部区域拦截下滑", "拦截屏幕顶部区域向下滑动手势", { gTopZoneOn }, { gTopZoneOn = it }, "top_zone")

            content.addView(sectionTitle(act, "内容与账号", p))
            addSwitch("拦截广告 / 挂件", "拦截已适配的广告层、金宝箱和悬浮挂件", { gAdOn }, { gAdOn = it }, "ad_block")
            addSwitch("解锁 VIP", "启用已适配的 VIP 状态 Hook", { gVipOn }, { gVipOn = it }, "vip_unlock")
            addSwitch("显示 VIP 图标", "控制 VIP 图标相关显示逻辑", { gVipIconOn }, { gVipIconOn = it }, "vip_icon")

            content.addView(sectionTitle(act, "实验性功能", p))
            addSwitch("OLED 亮度拦截", "实验功能：仅在需要时手动开启", { gOledProtectOn }, { gOledProtectOn = it }, "oled_protect")
            addSwitch("通知栏快捷菜单", "在通知栏显示模块开关、面板和重启快捷操作。没有效果 记得打开红果的通知权限", { gNotificationMenuOn }, {
                gNotificationMenuOn = it
                createNotification(act.applicationContext)
            }, "notification_menu")
            addSwitch(
                "自定义下载数量限制",
                downloadLimitSummary(),
                { gDownloadLimitUnlimitOn },
                {
                    gDownloadLimitUnlimitOn = it
                    if (it) Toast.makeText(act, "已开启。为确保下载进程读取新配置，请强停红果后重新打开", Toast.LENGTH_LONG).show()
                },
                DOWNLOAD_LIMIT_PREF,
                onRowAction = { showDownloadLimitEditor(act, p) {  } },
            )

            val scroll = ScrollView(act).apply {
                isFillViewport = false
                isVerticalScrollBarEnabled = false
                overScrollMode = View.OVER_SCROLL_NEVER
                addView(content, ViewGroup.LayoutParams(ViewGroup.LayoutParams.MATCH_PARENT, ViewGroup.LayoutParams.WRAP_CONTENT))
            }

            val dialog = Dialog(act)
            dialog.requestWindowFeature(android.view.Window.FEATURE_NO_TITLE)
            val shell = LinearLayout(act).apply {
                orientation = LinearLayout.VERTICAL
                background = roundedBg(act, p.page, 24f, p.divider, 1f)
                clipToPadding = false
            }
            shell.addView(scroll, LinearLayout.LayoutParams(
                ViewGroup.LayoutParams.MATCH_PARENT,
                0,
                1f,
            ))

            val footer = LinearLayout(act).apply {
                orientation = LinearLayout.VERTICAL
                setPadding(dp(act, 16f), dp(act, 10f), dp(act, 16f), dp(act, 16f))
            }
            val done = TextView(act).apply {
                text = "完成"
                textSize = 14f
                gravity = Gravity.CENTER
                setTypeface(Typeface.DEFAULT, Typeface.BOLD)
                setTextColor(Color.WHITE)
                background = accentGradientBg(act, p, 14f)
                isClickable = true
                isFocusable = true
                minimumHeight = dp(act, 46f)
                setOnClickListener {

                    shell.animate().cancel()
                    shell.animate()
                        .alpha(0f)
                        .translationY(dp(act, 14f).toFloat())
                        .scaleX(0.985f)
                        .scaleY(0.985f)
                        .setDuration(130L)
                        .withEndAction { if (dialog.isShowing) dialog.dismiss() }
                        .start()
                }
            }
            footer.addView(done, LinearLayout.LayoutParams(
                ViewGroup.LayoutParams.MATCH_PARENT,
                dp(act, 46f),
            ))
            shell.addView(footer, LinearLayout.LayoutParams(
                ViewGroup.LayoutParams.MATCH_PARENT,
                ViewGroup.LayoutParams.WRAP_CONTENT,
            ))

            shell.alpha = 0f
            shell.translationY = dp(act, 18f).toFloat()
            shell.scaleX = 0.975f
            shell.scaleY = 0.975f

            dialog.setContentView(shell)
            dialog.setCanceledOnTouchOutside(true)
            dialog.setOnShowListener {
                try {
                    val dm = act.resources.displayMetrics
                    dialog.window?.apply {

                        setBackgroundDrawable(android.graphics.drawable.ColorDrawable(Color.TRANSPARENT))
                        addFlags(WindowManager.LayoutParams.FLAG_DIM_BEHIND)
                        attributes = attributes.apply {
                            dimAmount = 0.58f
                            windowAnimations = 0
                        }
                        setLayout((dm.widthPixels * 0.90f).toInt(), (dm.heightPixels * 0.86f).toInt())
                    }
                    shell.post {
                        shell.animate().cancel()
                        shell.animate()
                            .alpha(1f)
                            .translationY(0f)
                            .scaleX(1f)
                            .scaleY(1f)
                            .setDuration(220L)
                            .setInterpolator(android.view.animation.DecelerateInterpolator(1.6f))
                            .start()
                    }
                } catch (e: Throwable) {
                    LogUtil.error("面板窗口样式", e)
                }
            }
            dialog.show()
            markAsModuleUi(dialog.window?.decorView)
        } catch (e: Exception) {
            LogUtil.error("面板", e)
        }
    }
    private fun openPanel() { mainHandler.post { showPanel(gCurrentActivity) } }

    private fun handleTopZoneEvent(ev: android.view.MotionEvent?): Boolean {
        if (ev == null) return false
        try {
            when (ev.actionMasked) { 0 -> { val y = ev.rawY; if (y in 0f..TOP_ZONE_HEIGHT.toFloat()) { gTopZoneTracking = true; gTopZoneStartY = y; gTopZoneActive = false } else { gTopZoneTracking = false; gTopZoneActive = false }; return false } }
            if (gTopZoneTracking) { if (!gTopZoneActive && ev.actionMasked == 2) { if (ev.rawY - gTopZoneStartY > 60) gTopZoneActive = true }; if (gTopZoneActive) { if (ev.actionMasked == 1 || ev.actionMasked == 3) { gTopZoneTracking = false; gTopZoneActive = false }; return true }; if (ev.actionMasked == 1 || ev.actionMasked == 3) gTopZoneTracking = false }
        } catch (_: Exception) {}
        return false
    }

    private fun findNativeSettingsContainer(root: View?): ViewGroup? {
        if (root !is ViewGroup) return null
        var best: ViewGroup? = null
        var bestScore = Int.MIN_VALUE
        fun walk(group: ViewGroup, depth: Int) {
            if (depth > 9) return
            val name = group.javaClass.name
            val isRecycler = name.contains("RecyclerView") || name.contains("ListView")
            val isVerticalLinear = group is LinearLayout && group.orientation == LinearLayout.VERTICAL
            if (!isRecycler && isVerticalLinear && group.childCount in 2..40) {
                var textCount = 0
                var clickableCount = 0
                for (i in 0 until group.childCount) {
                    val child = group.getChildAt(i)
                    if (child is TextView) textCount++
                    if (child.isClickable) clickableCount++
                    if (child is ViewGroup) {
                        for (j in 0 until child.childCount) if (child.getChildAt(j) is TextView) { textCount++; break }
                    }
                }
                val widthOk = group.width <= 0 || group.width >= actWidthPx(group.context) * 0.72f
                val score = textCount * 3 + clickableCount * 2 + group.childCount - depth * 2 + if (widthOk) 8 else 0
                if (textCount >= 2 && score > bestScore) {
                    best = group
                    bestScore = score
                }
            }
            for (i in 0 until group.childCount) {
                val child = group.getChildAt(i)
                if (child is ViewGroup && !child.javaClass.name.contains("RecyclerView")) walk(child, depth + 1)
            }
        }
        walk(root, 0)
        return best
    }

    private fun actWidthPx(ctx: Context): Int = try {
        ctx.resources.displayMetrics.widthPixels
    } catch (_: Throwable) { 1080 }

    private fun setFieldValue(instance: Any, fieldName: String, value: Any?): Boolean {
        var clazz: Class<*>? = instance.javaClass
        while (clazz != null) {
            try {
                clazz.getDeclaredField(fieldName).apply { isAccessible = true }.set(instance, value)
                return true
            } catch (_: Throwable) {}
            clazz = clazz.superclass
        }
        return false
    }

    private fun callNoArgMethod(instance: Any, methodName: String): Any? {
        var clazz: Class<*>? = instance.javaClass
        while (clazz != null) {
            try {
                val method = clazz.declaredMethods.firstOrNull { it.name == methodName && it.parameterCount == 0 }
                if (method != null) {
                    method.isAccessible = true
                    return method.invoke(instance)
                }
            } catch (_: Throwable) {}
            clazz = clazz.superclass
        }
        return null
    }

    private data class NativeSettingsSpec(
        val listMethods: Set<String>,
        val itemClass: String,
        val clickClass: String,
        val checkedClass: String?,
        val style: String,
    )

    private fun nativeSettingsSpec(): NativeSettingsSpec = when {
        gPkg == TargetNames.OVERSEA_PACKAGE && gNames.profileId == "OVERSEA-7.3.5.32" -> NativeSettingsSpec(
            listMethods = setOf("n1", "o1"),
            itemClass = "hz5.e",
            clickClass = "hz5.b",
            checkedClass = null,
            style = "oversea-arrow",
        )
        gPkg == TargetNames.OVERSEA_PACKAGE -> NativeSettingsSpec(
            listMethods = setOf("e1", "f1"),
            itemClass = "vr5.e",
            clickClass = "vr5.b",
            checkedClass = null,
            style = "oversea-arrow",
        )
        gNames.profileId == "CN-7.3.3.18" -> NativeSettingsSpec(
            listMethods = setOf("X0", "Y0"),
            itemClass = "mz5.e",
            clickClass = "mz5.b",
            checkedClass = "mz5.c",
            style = "atomic-arrow",
        )
        gNames.profileId == "CN-7.3.2.32" -> NativeSettingsSpec(
            listMethods = setOf("c1", "d1"),
            itemClass = "qv5.e",
            clickClass = "qv5.b",
            checkedClass = "qv5.c",
            style = "grouped-atomic-arrow",
        )
        else -> NativeSettingsSpec(

            listMethods = setOf("c1", "d1"),
            itemClass = "pv5.e",
            clickClass = "pv5.b",
            checkedClass = "pv5.c",
            style = "grouped-atomic-arrow",
        )
    }

    private fun injectNativeSettingsList(
        listObj: Any?,
        classLoader: ClassLoader,
        spec: NativeSettingsSpec = nativeSettingsSpec(),
    ): Boolean {
        val list = listObj as? MutableList<Any?> ?: return false
        try {
            for (item in list) {
                if (item == null) continue
                val title = findFieldValue(item, "e")?.toString()
                if (title == "模块设置") return true
            }

            val itemClass = Class.forName(spec.itemClass, false, classLoader)
            val ctor = itemClass.declaredConstructors.firstOrNull { it.parameterCount == 0 } ?: return false
            ctor.isAccessible = true
            val item = ctor.newInstance()
            if (!setFieldValue(item, "e", "模块设置")) return false
            setFieldValue(item, "f", "点击打开模块菜单")

            when (spec.style) {
                "oversea-arrow" -> {

                    setFieldValue(item, "i", true)
                    setFieldValue(item, "k", true)
                    setFieldValue(item, "a", true)
                    setFieldValue(item, "b", true)
                    setFieldValue(item, "c", true)
                }
                "grouped-atomic-arrow" -> {

                    setFieldValue(item, "i", true)
                    setFieldValue(item, "a", true)
                    setFieldValue(item, "b", true)
                    setFieldValue(item, "c", true)
                    setFieldValue(item, "o", java.util.concurrent.atomic.AtomicBoolean(gMasterOn))
                }
                else -> {

                    setFieldValue(item, "i", true)
                    setFieldValue(item, "o", java.util.concurrent.atomic.AtomicBoolean(gMasterOn))
                }
            }

            list.add(0, item)
            LogUtil.info("原生设置数据项已注入: ${spec.itemClass} / index=0 / profile=${gNames.profileId}")
            return true
        } catch (e: Throwable) {
            LogUtil.warn("原生设置数据项注入失败(${spec.itemClass}): $e")
            return false
        }
    }

    // ───────────────────────── 原生设置项 · 动态注入 ─────────────────────────
    // 背景：宿主每次发版都会整体重排 item 类与列表方法名（7.3.7.32 里 7.3.5.32 的
    // hz5.e / e1 / f1 全部失效）。与其继续维护映射表，不如用「同页已有的普通条目」
    // 当模板，复制出一个同构条目——类名、方法名、字段名全部运行时推导。

    private fun fieldsOfAll(instance: Any): List<java.lang.reflect.Field> {
        val out = ArrayList<java.lang.reflect.Field>()
        var c: Class<*>? = instance.javaClass
        while (c != null && c != Any::class.java) {
            c.declaredFields.forEach { runCatching { it.isAccessible = true }; out += it }
            c = c.superclass
        }
        return out
    }

    private fun fieldByName(instance: Any, name: String): java.lang.reflect.Field? {
        var c: Class<*>? = instance.javaClass
        while (c != null) {
            try {
                return c.getDeclaredField(name).apply { isAccessible = true }
            } catch (_: Throwable) {}
            c = c.superclass
        }
        return null
    }

    /** 读条目标题：优先约定字段，失败则取第一个非空文本字段（含声明顺序兜底） */
    private fun readItemTitleField(item: Any): Pair<java.lang.reflect.Field, String>? {
        fieldByName(item, "e")?.let { f ->
            val v = runCatching { f.get(item) }.getOrNull()
            if (v is CharSequence && v.isNotBlank()) return f to v.toString()
        }
        for (f in fieldsOfAll(item)) {
            if (f.type == CharSequence::class.java || f.type == java.lang.String::class.java) {
                val v = runCatching { f.get(item) }.getOrNull()
                if (v is CharSequence && v.isNotBlank()) return f to v.toString()
            }
        }
        return null
    }

    /** 点击回调字段：约定字段优先，失败则取第一个「接口类型且已赋值」的字段 */
    private fun clickCallbackField(item: Any): java.lang.reflect.Field? {
        fieldByName(item, "n")?.let {
            if (it.type.isInterface && runCatching { it.get(item) }.getOrNull() != null) return it
        }
        return fieldsOfAll(item).firstOrNull {
            it.type.isInterface && runCatching { it.get(item) }.getOrNull() != null
        }
    }

    /**
     * 挑一个「可点击的普通条目」当模板：有标题、有已赋值的接口型点击回调、不是开关项，
     * 且能就地构造（部分条目需要额外参数，如「清理缓存」要传 adapter，不能拿来复制）。
     * 注意「是否开关项」的判据必须是字段值而非字段类型——开关字段声明在基类上，
     * 每个条目都有这个字段，只有开关项才有值；按类型筛会把所有条目都排除掉。
     */
    private fun pickSettingsTemplateItem(list: List<Any?>, act: Activity?): Any? {
        for (item in list) {
            if (item == null) continue
            if (readItemTitleField(item) == null) continue
            val switchState = fieldByName(item, "o")?.let { runCatching { it.get(item) }.getOrNull() }
            if (switchState is java.util.concurrent.atomic.AtomicBoolean) continue
            if (clickCallbackField(item) == null) continue
            if (!canConstructLike(item, act)) continue
            return item
        }
        return null
    }

    private fun canConstructLike(template: Any, act: Activity?): Boolean {
        val appCtx = act?.applicationContext
        for (ctor in template.javaClass.declaredConstructors) {
            when (ctor.parameterCount) {
                0 -> return true
                1 -> {
                    val p0 = ctor.parameterTypes[0]
                    if ((act != null && p0.isInstance(act)) || (appCtx != null && p0.isInstance(appCtx))) return true
                }
            }
        }
        return false
    }

    /** 用模板的类构造同构实例；优先单参构造（Context / AbsActivity 等） */
    private fun newItemLikeTemplate(template: Any, act: Activity?): Any? {
        val appCtx = act?.applicationContext
        for (ctor in template.javaClass.declaredConstructors.sortedBy { it.parameterCount }) {
            try {
                ctor.isAccessible = true
                val types = ctor.parameterTypes
                when {
                    types.isEmpty() -> return ctor.newInstance()
                    types.size == 1 -> {
                        if (act != null && types[0].isInstance(act)) return ctor.newInstance(act)
                        if (appCtx != null && types[0].isInstance(appCtx)) return ctor.newInstance(appCtx)
                    }
                }
            } catch (_: Throwable) {}
        }
        return null
    }

    /** 用动态代理接管条目的点击回调（宿主用的是接口，不需要 hook 任何类） */
    private fun installClickProxy(item: Any, cbField: java.lang.reflect.Field, act: Activity?) {
        val iface = cbField.type
        if (!iface.isInterface) return
        val handler = java.lang.reflect.InvocationHandler { _, method, _ ->
            when (method.name) {
                "equals" -> false
                "hashCode" -> System.identityHashCode(item)
                "toString" -> "KejiyuSettingsClick"
                else -> {
                    mainHandler.post {
                        try { showPanel(gSettingsActivity ?: act ?: gCurrentActivity) } catch (_: Throwable) {}
                    }
                    null
                }
            }
        }
        val proxy = java.lang.reflect.Proxy.newProxyInstance(
            iface.classLoader ?: item.javaClass.classLoader,
            arrayOf(iface),
            handler,
        )
        cbField.set(item, proxy)
    }

    /**
     * 让条目按「独立分组卡片」形态渲染（上下都圆角）。
     *
     * 7.3.7.32 起，条目圆角不再由条目自身的样式字段决定，而是由**分组基类**
     * （item 的 abstract 父类，实测 `p26.a$a`）上的三个 boolean 驱动：
     *   a = 属于分组卡片（决定是否走卡片分支）
     *   b = 组内首项（上圆角背景可见）
     *   c = 组内末项（下圆角背景可见）
     * 由分组管理器（实测 `p26.a`）的 `c()` 按「首项 b=true / 末项 c=true / 其余 a=true」
     * 统一赋值，数据构建结束时调用一次。
     *
     * 我们的条目是 hook 列表方法返回之后才塞进去的，既没进任何分组，也没同步这三个
     * 字段，onBind 于是走 else 分支把上下圆角背景全部 GONE —— 用户看到的就是「直角」。
     * 这里按「独立卡片」直接置位 a=b=c=true：因为不进分组，宿主之后再怎么重算
     * 分组状态（如 onResume 里的 n1()）都不会覆盖它，比挂进别人分组更稳。
     */
    private fun applyGroupCardStyle(item: Any, list: List<Any?>): String {
        val isBool = { f: java.lang.reflect.Field -> f.type == java.lang.Boolean.TYPE }
        val byName = listOf("a", "b", "c").map { fieldByName(item, it)?.takeIf(isBool) }
        if (byName.all { it != null }) {
            byName.forEach { it!!.set(item, true) }
            fieldByName(item, "d")?.takeIf { it.type == CharSequence::class.java }?.set(item, "")
            return "字段名 a/b/c"
        }

        // 兜底：字段名漂移时，按「item 的 abstract 父类声明顺序」取前三个 boolean。
        // 前提是列表里已存在 a=true 的条目 —— 那才证明确实是这套分组卡片机制，
        // 否则（例如国内版旧结构）宁可不设，避免把无关字段改成 true。
        var base: Class<*>? = item.javaClass
        while (base != null && base != Any::class.java && !java.lang.reflect.Modifier.isAbstract(base.modifiers)) {
            base = base.superclass
        }
        val groupBase = base?.takeIf { it != Any::class.java } ?: return "跳过(未找到分组基类)"
        val bools = groupBase.declaredFields.filter(isBool)
        if (bools.size < 3) return "跳过(分组基类 boolean 不足:${bools.size})"
        val positionFields = bools.take(3)
        val inUse = list.any { other ->
            other != null && positionFields.any { f ->
                fieldByName(other, f.name)?.takeIf(isBool)?.let { runCatching { it.get(other) }.getOrNull() } == true
            }
        }
        if (!inUse) return "跳过(宿主未使用分组卡片)"
        positionFields.forEach { runCatching { it.isAccessible = true; it.set(item, true) } }
        groupBase.declaredFields.firstOrNull { it.type == CharSequence::class.java }
            ?.let { runCatching { it.isAccessible = true; it.set(item, "") } }
        return "声明顺序兜底 ${positionFields.map { it.name }}"
    }

    internal fun injectNativeSettingsItemDynamic(listObj: Any?, act: Activity?, classLoader: ClassLoader): Boolean {
        val list = listObj as? MutableList<Any?> ?: return false
        try {
            for (item in list) {
                if (item != null && readItemTitleField(item)?.second == SETTINGS_ENTRY_TITLE) return true
            }

            val template = pickSettingsTemplateItem(list, act) ?: run {
                LogUtil.warn("原生设置动态注入跳过：未找到可用模板条目")
                return false
            }
            val (titleField, templateTitle) = readItemTitleField(template) ?: return false
            val item = newItemLikeTemplate(template, act) ?: run {
                LogUtil.warn("原生设置动态注入跳过：无法构造 ${template.javaClass.name}")
                return false
            }

            val newTitleField = fieldByName(item, titleField.name) ?: run {
                LogUtil.warn("原生设置动态注入跳过：新实例缺少标题字段 ${titleField.name}")
                return false
            }
            newTitleField.set(item, SETTINGS_ENTRY_TITLE)

            fieldByName(item, "f")?.takeIf { it.type == CharSequence::class.java }
                ?.set(item, "点击打开模块菜单")
            fieldByName(item, "i")?.takeIf { it.type == java.lang.Boolean.TYPE }
                ?.set(item, true)
            val cardStyle = applyGroupCardStyle(item, list)
            clickCallbackField(item)?.let { installClickProxy(item, it, act) }

            // 固定插到列表最顶部（与旧版一致），而不是挨着模板条目 —— 挨着模板会跟着
            // 模板所在分组跑，位置随宿主条目的增删飘移。
            list.add(0, item)
            LogUtil.info(
                "原生设置项已动态注入: 模板=${template.javaClass.name}「$templateTitle」 index=0" +
                    " | 标题字段=${titleField.name} | 卡片样式=$cardStyle | profile=${gNames.profileId}",
            )
            return true
        } catch (e: Throwable) {
            LogUtil.warn("原生设置项动态注入失败: ${e.javaClass.simpleName}: ${e.message}")
            return false
        }
    }

    private fun boundSettingsTitle(clickHost: Any): String? {
        return try {
            val holder = findFieldValue(clickHost, "a") ?: return null
            val data = callNoArgMethod(holder, "getBoundData") ?: return null
            findFieldValue(data, "e")?.toString()
        } catch (_: Throwable) { null }
    }

    private fun nearestSettingsTextColor(root: View?, fallback: Int): Int {
        if (root == null) return fallback
        if (root is TextView) {
            val text = try { root.text?.toString().orEmpty() } catch (_: Throwable) { "" }
            if (text.isNotBlank() && root.textSize > 12f) return try { root.currentTextColor } catch (_: Throwable) { fallback }
        }
        if (root is ViewGroup) for (i in 0 until root.childCount) {
            val c = nearestSettingsTextColor(root.getChildAt(i), fallback)
            if (c != fallback) return c
        }
        return fallback
    }

    /** 感知亮度（0..255），用于推导对比色 */
    private fun perceivedLuminance(color: Int): Int =
        (Color.red(color) * 299 + Color.green(color) * 587 + Color.blue(color) * 114) / 1000

    /**
     * 保证文字在给定背景上可读：优先沿用宿主文字色，明暗对比不足时按背景明暗切换。
     * 历史事故：兜底入口背景透明、文字色取自隔壁设置项（浅色），恰好落在 App 自带的
     * 白色区域上 → 白底白字，用户只看到一个白色色块。
     */
    private fun readableTextOn(bg: Int, preferred: Int): Int {
        if (Color.alpha(bg) < 40) return preferred
        val lb = perceivedLuminance(bg)
        val lp = perceivedLuminance(preferred)
        if (Math.abs(lb - lp) >= 80) return preferred
        return if (lb > 150) safeRgb(28, 25, 23) else safeRgb(245, 243, 240)
    }

    /** 取 Drawable 的纯色；渐变/ripple 等返回 null */
    private fun solidColorOf(drawable: android.graphics.drawable.Drawable?): Int? = when (drawable) {
        is android.graphics.drawable.ColorDrawable -> drawable.color
        is android.graphics.drawable.GradientDrawable ->
            runCatching { drawable.color?.defaultColor }.getOrNull()
        else -> null
    }

    /** 在视图树里找第一个不透明纯色背景（宿主页面/卡片底色），跳过模块自己注入的 view */
    private fun resolveHostSurfaceColor(root: View?, depth: Int = 0): Int? {
        if (root == null || depth > 8) return null
        if (root.tag == gKejiyuBtnTag || root.tag == gKejiyuSettingsWrapperTag) return null
        solidColorOf(root.background)?.let { if (Color.alpha(it) == 255) return it }
        if (root is ViewGroup) {
            for (i in 0 until root.childCount) {
                resolveHostSurfaceColor(root.getChildAt(i), depth + 1)?.let { return it }
            }
        }
        return null
    }

    private fun makeNativeSettingsEntry(act: Activity, parent: ViewGroup): View {
        val p = panelPalette(act)
        val hostText = nearestSettingsTextColor(parent, p.text)
        // 不透明背景 + 按背景算出的对比色：不再依赖「父容器是什么颜色」这个不可控前提。
        val bgColor = p.surface
        val textColor = readableTextOn(bgColor, hostText)
        return LinearLayout(act).apply {
            orientation = LinearLayout.HORIZONTAL
            gravity = Gravity.CENTER_VERTICAL
            setPadding(dp(act, 18f), 0, dp(act, 16f), 0)
            minimumHeight = dp(act, 56f)
            tag = gKejiyuBtnTag
            markAsModuleUi(this)
            isClickable = true
            isFocusable = true
            try {
                val rippleColor = safeArgb(38, Color.red(textColor), Color.green(textColor), Color.blue(textColor))
                // 圆角跟着宿主卡片走：兜底入口是自绘 View，不圆角会显得比原生条目「硬」
                val content = android.graphics.drawable.GradientDrawable().apply {
                    shape = android.graphics.drawable.GradientDrawable.RECTANGLE
                    setColor(bgColor)
                    cornerRadius = dp(act, 12f).toFloat()
                }
                background = android.graphics.drawable.RippleDrawable(
                    ColorStateList.valueOf(rippleColor),
                    content,
                    android.graphics.drawable.ColorDrawable(Color.WHITE),
                )
            } catch (_: Throwable) {
                background = android.graphics.drawable.ColorDrawable(bgColor)
            }
            addView(TextView(act).apply {
                text = SETTINGS_ENTRY_TITLE
                textSize = 15f
                setTextColor(textColor)
                includeFontPadding = false
                gravity = Gravity.CENTER_VERTICAL
            }, LinearLayout.LayoutParams(0, ViewGroup.LayoutParams.MATCH_PARENT, 1f))
            addView(TextView(act).apply {
                text = "›"
                textSize = 25f
                setTextColor(readableTextOn(bgColor, p.textSecondary))
                includeFontPadding = false
                gravity = Gravity.CENTER
            }, LinearLayout.LayoutParams(dp(act, 30f), ViewGroup.LayoutParams.MATCH_PARENT))
            setOnClickListener { showPanel(gSettingsActivity ?: gCurrentActivity) }
        }
    }

    private fun containsKejiyuEntry(root: View?): Boolean {
        if (root == null) return false
        if (root.tag == gKejiyuBtnTag) return true
        if (root is TextView) {
            val text = try { root.text?.toString()?.trim().orEmpty() } catch (_: Throwable) { "" }
            if (text == "模块设置" || text == "KEJIYU 模块设置") return true
        }
        if (root is ViewGroup) for (i in 0 until root.childCount) if (containsKejiyuEntry(root.getChildAt(i))) return true
        return false
    }

    private fun isLikelySettingsActivity(act: Activity?): Boolean {
        if (act == null) return false
        val name = try { act.javaClass.name } catch (_: Throwable) { return false }
        if (name == "com.dragon.read.component.biz.impl.mine.settings.SettingsActivity" ||
            name == "com.dragon.read.component.biz.impl.mine.settings.KmpSettingsActivity") return true
        val lower = name.lowercase()
        return lower.startsWith("com.dragon.read") && lower.contains(".mine.") &&
            (lower.endsWith("settingsactivity") || lower.endsWith("settingactivity"))
    }

    private fun installSettingsFooterFallback(act: Activity, content: ViewGroup): Boolean {
        if (containsKejiyuEntry(content)) return true
        if (content.childCount <= 0) return false
        try {

            val oldChildren = ArrayList<Pair<View, ViewGroup.LayoutParams?>>(content.childCount)
            for (i in 0 until content.childCount) {
                val child = content.getChildAt(i)
                oldChildren.add(child to child.layoutParams)
            }
            content.removeAllViews()

            val wrapper = LinearLayout(act).apply {
                orientation = LinearLayout.VERTICAL
                tag = gKejiyuSettingsWrapperTag
                layoutParams = ViewGroup.LayoutParams(
                    ViewGroup.LayoutParams.MATCH_PARENT, ViewGroup.LayoutParams.MATCH_PARENT
                )
            }
            val stage = FrameLayout(act)
            wrapper.addView(stage, LinearLayout.LayoutParams(
                ViewGroup.LayoutParams.MATCH_PARENT, 0, 1f
            ))
            for ((child, oldLp) in oldChildren) {
                val lp = if (oldLp is FrameLayout.LayoutParams) oldLp else FrameLayout.LayoutParams(
                    oldLp?.width ?: ViewGroup.LayoutParams.MATCH_PARENT,
                    oldLp?.height ?: ViewGroup.LayoutParams.MATCH_PARENT,
                )
                stage.addView(child, lp)
            }
            // 铺上宿主底色：App 底部常自带白底区域，不铺会让兜底入口「悬」在一片白块上
            wrapper.setBackgroundColor(resolveHostSurfaceColor(stage) ?: panelPalette(act).page)

            val entry = makeNativeSettingsEntry(act, wrapper)

            val divider = View(act).apply {
                background = android.graphics.drawable.ColorDrawable(panelPalette(act).divider)
            }
            wrapper.addView(divider, LinearLayout.LayoutParams(
                ViewGroup.LayoutParams.MATCH_PARENT, dp(act, 1f)
            ))
            wrapper.addView(entry, LinearLayout.LayoutParams(
                ViewGroup.LayoutParams.MATCH_PARENT, dp(act, 56f)
            ))
            content.addView(wrapper, ViewGroup.LayoutParams(
                ViewGroup.LayoutParams.MATCH_PARENT, ViewGroup.LayoutParams.MATCH_PARENT
            ))
            LogUtil.info("设置页入口已使用固定布局 footer 兜底: children=${oldChildren.size}")
            return true
        } catch (e: Throwable) {
            LogUtil.error("设置页 footer 兜底", e)
            return false
        }
    }

    private fun injectSettingsButton(act: Activity, allowFallback: Boolean = true) {
        try {
            if (act.isFinishing || !isLikelySettingsActivity(act)) return
            val content = act.findViewById<ViewGroup>(android.R.id.content) ?: return
            if (containsKejiyuEntry(content)) return

            val nativeContainer = findNativeSettingsContainer(content)
            if (nativeContainer != null) {
                val entry = makeNativeSettingsEntry(act, nativeContainer)
                nativeContainer.addView(entry, ViewGroup.LayoutParams(
                    ViewGroup.LayoutParams.MATCH_PARENT, dp(act, 56f),
                ))
                LogUtil.info("设置页入口已注入原生容器: ${nativeContainer.javaClass.name}")
                return
            }

            if (allowFallback) installSettingsFooterFallback(act, content)
        } catch (e: Exception) { LogUtil.error("设置页入口", e) }
    }

    private fun scheduleSettingsEntryInjection(act: Activity?) {
        if (!isLikelySettingsActivity(act)) return
        val a = act ?: return
        gSettingsActivity = a

        val attempts = arrayOf(
            150L to false,
            550L to false,
            1400L to false,
            2800L to true,
            4600L to true,
        )
        for ((delay, allowFallback) in attempts) {
            mainHandler.postDelayed({
                if (!a.isFinishing && isLikelySettingsActivity(a)) injectSettingsButton(a, allowFallback)
            }, delay)
        }
    }

    private fun pollUpdatePrompt(a: Activity, attempt: Int) {
        if (a.isFinishing || attempt > 10) return
        mainHandler.postDelayed({
            try {
                if (UpdateChecker.hasUpdate || UpdateChecker.lastError != null) {
                    UpdateChecker.showUpdateDialogIfNeeded(a)
                } else if (!a.isFinishing) {
                    pollUpdatePrompt(a, attempt + 1)
                }
            } catch (_: Exception) {}
        }, 1000)
    }

    fun showUpdateDialogIfAvailable() {
        val a = gCurrentActivity ?: return
        if (a.isFinishing) return
        mainHandler.post { UpdateChecker.showUpdateDialogIfNeeded(a) }
    }

    /**
     * 收集可用的 Context 候选。
     *
     * 关键点：hook 是在 onPackageReady 阶段安装的，此时目标进程的 Application
     * 还没有创建，`ActivityThread.currentApplication()` 会返回 null —— 这正是
     * 之前 detectedVersion 一直是「未知」的原因。system context 在该阶段已经可用，
     * 因此优先用它拿 PackageManager。
     */
    private fun contextCandidates(): List<Context> {
        val out = mutableListOf<Context>()
        try {
            val at = Class.forName("android.app.ActivityThread")
                .getDeclaredMethod("currentActivityThread").invoke(null)
            if (at != null) {
                val sysCtx = at.javaClass.getMethod("getSystemContext").invoke(at) as? Context
                if (sysCtx != null) out.add(sysCtx)
            }
        } catch (_: Throwable) {
        }
        try {
            (Class.forName("android.app.ActivityThread")
                .getDeclaredMethod("currentApplication").invoke(null) as? Context)?.let { out.add(it) }
        } catch (_: Throwable) {
        }
        try {
            (Class.forName("android.app.AppGlobals")
                .getDeclaredMethod("getInitialApplication").invoke(null) as? Context)?.let { out.add(it) }
        } catch (_: Throwable) {
        }
        return out
    }

    private fun detectTargetPackageVersion(pkg: String): Pair<String?, Long> {
        contextCandidates().forEach { ctx ->
            try {
                @Suppress("DEPRECATION")
                val pi = ctx.packageManager.getPackageInfo(pkg, 0)
                if (pi != null) {
                    @Suppress("DEPRECATION")
                    val code = if (Build.VERSION.SDK_INT >= 28) pi.longVersionCode else pi.versionCode.toLong()
                    if (!pi.versionName.isNullOrBlank() || code > 0L) return pi.versionName to code
                }
            } catch (_: Throwable) {
            }
        }
        LogUtil.warn("读取目标包版本失败（将回退到类指纹识别）")
        return null to -1L
    }

    /**
     * 用「方法名锚点」修正映射表中已失效的混淆类名。
     *
     * 只在该字段**为空或类已加载不出来**时才覆盖——已适配版本零改动、零开销；
     * 目标 App 发版导致类名整体重排时，锚点自动补位，避免整条功能链静默失效。
     * 锚点解析结果按 profile 缓存到日志目录，同一版本只在首次运行付出扫描成本。
     */
    private fun applyAnchorOverrides(
        names: TargetNames.Names,
        classLoader: ClassLoader?,
        pkg: String,
        versionLabel: String?,
    ): TargetNames.Names {
        if (classLoader == null) return names
        val apkPaths = AnchorResolver.apkPaths(contextCandidates(), pkg)
        if (apkPaths.isEmpty()) return names

        val safeVersion = (versionLabel ?: "未知").replace(Regex("[^A-Za-z0-9._-]"), "_")
        val resolution = try {
            AnchorResolver.resolve(
                apkPaths = apkPaths,
                classLoader = classLoader,
                cacheFile = File(LogUtil.logDir(), "anchors-$safeVersion.txt"),
            )
        } catch (e: Throwable) {
            LogUtil.warn("锚点解析异常，沿用表内类名: ${e.javaClass.simpleName}: ${e.message}")
            return names
        }

        if (resolution.byId.isEmpty()) {
            LogUtil.info("锚点解析：无命中（来源=${resolution.source} 耗时=${resolution.costMs}ms）")
            return names
        }

        val applied = ArrayList<String>()

        fun pick(fieldName: String, current: String, anchorId: String): String {
            val candidate = resolution.first(anchorId) ?: return current
            if (current.isNotBlank() && classExists(current, classLoader)) return current
            applied += "$fieldName=${current.ifBlank { "<空>" }}→$candidate"
            return candidate
        }

        fun merge(fieldName: String, current: List<String>, anchorId: String): List<String> {
            val extra = resolution.all(anchorId)
            if (extra.isEmpty()) return current
            val alive = current.filter { classExists(it, classLoader) }
            if (current.isNotEmpty() && alive.size == current.size) return current
            applied += "$fieldName 候选 ${current.size}→${(alive + extra).distinct().size}"
            return (alive + extra).distinct()
        }

        var out = names.copy(
            shortHolder = pick("shortHolder", names.shortHolder, "holder"),
            holderBaseS1 = pick("holderBaseS1", names.holderBaseS1, "holder"),
            playbackState = pick("playbackState", names.playbackState, "player"),
            resolutionController = pick("resolutionController", names.resolutionController, "player"),
        )

        val doubleTapHost = resolution.first("doubleTap")
        if (doubleTapHost != null && names.doubleTapHandlers.none { classExists(it, classLoader) }) {
            applied += "doubleTapHandlers=${names.doubleTapHandlers.ifEmpty { listOf("<空>") }.joinToString(",")}→$doubleTapHost"
            out = out.copy(doubleTapHandlers = listOf(doubleTapHost))
        }

        out = out.copy(
            percentPlayerCandidates = merge("percentPlayerCandidates", names.percentPlayerCandidates, "percentPlayer"),
            speedControllerCandidates = merge("speedControllerCandidates", names.speedControllerCandidates, "speedController"),
            floatPlayerCandidates = merge("floatPlayerCandidates", names.floatPlayerCandidates, "floatPlayer"),
        )

        LogUtil.info(
            "锚点解析：来源=${resolution.source} 耗时=${resolution.costMs}ms | 命中=" +
                resolution.byId.entries.joinToString(", ") { "${it.key}=${it.value.first()}" } +
                " | 生效覆盖=" + if (applied.isEmpty()) "无（表内类名均有效）" else applied.joinToString(" ; "),
        )
        if (resolution.truncated) {
            LogUtil.warn("锚点扫描超出时间预算被截断，结果可能不完整")
        }
        return out
    }

    private fun classExists(cls: String, classLoader: ClassLoader?): Boolean {
        if (cls.isBlank()) return false
        return try {
            Class.forName(cls, false, classLoader)
            true
        } catch (_: Throwable) {
            false
        }
    }

    /**
     * 依次尝试候选类名，返回第一个「存在且声明了指定方法」的类。
     *
     * 动机：混淆类名（ov4.x / ys4.x / pz4.w …）每次发版都可能变，
     * 但被 keep 规则保留的方法名（setPlaySpeed、getCurrentPlaySpeed、
     * setSpeed、onDoubleTap …）跨版本稳定得多。用「类名候选 + 方法签名校验」
     * 定位目标类，比硬编码单一类名鲁棒。
     */
    private fun resolveClassWithMethod(
        candidates: List<String>,
        method: String,
        paramTypes: Array<Class<*>>,
        classLoader: ClassLoader?,
    ): Class<*>? {
        for (name in candidates.filter { it.isNotBlank() }.distinct()) {
            try {
                val c = Class.forName(name, false, classLoader)
                val ok = if (paramTypes.isEmpty()) {
                    c.declaredMethods.any { it.name == method }
                } else {
                    runCatching { c.getDeclaredMethod(method, *paramTypes) }.isSuccess
                }
                if (ok) return c
            } catch (_: Throwable) {
            }
        }
        return null
    }

    fun installBusinessHooks(module: MainHook, classLoader: ClassLoader, pkg: String) {
        LogUtil.info("── installBusinessHooks ── pkg=$pkg")
        gTargetIdSet.clear()
        gSeriesTargetIdSet.clear()
        gProgressIdSet.clear()
        gPauseRestoreIdSet.clear()
        gPkg = pkg

        try {
            val processCtx = try {
                Class.forName("android.app.ActivityThread").getDeclaredMethod("currentApplication").invoke(null) as? Context
            } catch (_: Throwable) {
                try { Class.forName("android.app.AppGlobals").getDeclaredMethod("getInitialApplication").invoke(null) as? Context } catch (_: Throwable) { null }
            }
            if (processCtx != null) {
                appCtx = processCtx.applicationContext
                initPrefs(processCtx.applicationContext)
            }
        } catch (e: Throwable) {
            LogUtil.warn("下载进程设置初始化失败，将使用磁盘直读兜底: ${e.javaClass.simpleName}: ${e.message}")
        }
        val detected = detectTargetPackageVersion(pkg)
        gTargetVersionName = detected.first ?: "未知"
        gTargetVersionCode = detected.second

        val resolution = TargetNames.resolve(pkg, detected.first, classLoader)
        // 仅当表内存在加载不出来的类名字段时才启动锚点解析：全绿时零额外开销
        val needAnchors = TargetNames.probe(resolution.names, classLoader).misses.isNotEmpty()
        gNames = if (needAnchors) {
            applyAnchorOverrides(resolution.names, classLoader, pkg, detected.first)
        } else {
            resolution.names
        }

        if (gNames.useLegacySeedIds) seedIds.forEach { gTargetIdSet.add(it) }
        gNames.staticHideIds.forEach { gTargetIdSet.add(it) }
        gNames.staticProgressIds.forEach { gProgressIdSet.add(it) }
        gNames.pauseRestoreIds.forEach { gPauseRestoreIdSet.add(it) }
        val effectiveSeriesIds = if (gNames.seriesStaticIds.isNotEmpty()) gNames.seriesStaticIds else staticSeriesIds
        effectiveSeriesIds.forEach {
            gSeriesTargetIdSet.add(it)
            gPauseRestoreIdSet.add(it)
        }
        val versionExact = TargetNames.isSupported(pkg, detected.first) && resolution.exactVersion
        val report = TargetNames.probe(gNames, classLoader)
        LogUtil.info("目标兼容配置=${gNames.profileId} | detectedVersion=${gTargetVersionName}(${gTargetVersionCode}) | 版本精确匹配=${if (versionExact) "是" else "否(指纹推断)"} | 指纹命中=${report.summary()} | shortHolder=${gNames.shortHolder} | holderBaseS1=${gNames.holderBaseS1} | toolbarBase=${gNames.toolbarBase} | playbackState=${gNames.playbackState} | kmpVipModel=${gNames.kmpVipModel}")
        if (report.misses.isNotEmpty()) {
            LogUtil.info("指纹失效字段 ${report.misses.size} 项: ${report.misses.joinToString(" | ")}")
        }
        if (!versionExact) {
            LogUtil.warn("⚠ 未适配版本 | pkg=$pkg | version=$gTargetVersionName($gTargetVersionCode) | 已降级使用 ${gNames.profileId} | 指纹命中=${report.summary()}")
            if (report.misses.isNotEmpty()) {
                LogUtil.warn("⚠ 失效字段 ${report.misses.size} 项: ${report.misses.joinToString(" | ")}")
            }
            LogUtil.warn("⚠ 该版本不在适配表内，部分功能可能静默失效；请补充映射或回滚到已适配版本")
        }
        LogUtil.info("资源兼容：hideIds=${gTargetIdSet.joinToString { "0x%08X".format(it) }} | seriesIds=${gSeriesTargetIdSet.joinToString { "0x%08X".format(it) }} | progressIds=${gProgressIdSet.joinToString { "0x%08X".format(it) }} | pauseIds=${gPauseRestoreIdSet.joinToString { "0x%08X".format(it) }}")

        mainHandler.postDelayed({
            try {
                if (!UpdateChecker.checking && UpdateChecker.latestVersion == null) {
                    val ver = BuildConfig.VERSION_NAME.substringBefore(' ').substringBefore('(')
                    UpdateChecker.checkUpdate(ver) { latest ->
                        if (latest != null) {
                            showUpdateDialogIfAvailable()
                        }
                    }
                }
            } catch (_: Throwable) {}
        }, 8000L)

        fun ham(clazz: Class<*>, methodName: String, hookId: String, block: (XposedInterface.Chain) -> Any?) {
            clazz.declaredMethods.filter { it.name == methodName }.forEachIndexed { i, m -> try { module.hook(m).setId("${hookId}_$i").setExceptionMode(XposedInterface.ExceptionMode.PROTECTIVE).intercept(Hooker { chain -> block(chain) }) } catch (_: Exception) {} }
        }
        fun hac(clazz: Class<*>, hookId: String, block: (XposedInterface.Chain) -> Any?) {
            clazz.declaredConstructors.forEachIndexed { i, c -> try { module.hook(c).setId("${hookId}_$i").setExceptionMode(XposedInterface.ExceptionMode.PROTECTIVE).intercept(Hooker { chain -> block(chain) }) } catch (_: Exception) {} }
        }

        /**
         * 只挂「参数类型精确匹配」的那一个重载。
         *
         * paramTypeNames 用 `Class.getName()` 的形式：类写全限定名，基本类型写 "int"/"boolean"。
         *
         * 为什么需要它：`ham()` 会把**同名的全部重载**都挂上，而框架方法内部普遍存在委托链 ——
         * 例如 `LayoutInflater.inflate` 的 4 个重载最终都汇入
         * `inflate(XmlPullParser, ViewGroup, boolean)`，`ViewGroup.addView` 的 5 个重载最终都汇入
         * `addView(View, int, LayoutParams)`。于是宿主调用**一次** `addView(child)`，链条上多个已 hook 的
         * 重载会**依次命中**，各自对**同一棵子树**跑一遍 `scanTreeUnified` —— 实测把节点扫描量放大约 3 倍。
         * 只挂终端重载即可覆盖全部入口，且与「挂全部重载」的覆盖范围等价。
         */
        fun hamExact(clazz: Class<*>, methodName: String, paramTypeNames: List<String>, hookId: String, block: (XposedInterface.Chain) -> Any?) {
            val target = clazz.declaredMethods.firstOrNull { m ->
                m.name == methodName && m.parameterTypes.map { it.name } == paramTypeNames
            }
            if (target == null) {
                LogUtil.warn("  hamExact 未命中 $methodName(${paramTypeNames.joinToString()})")
                return
            }
            try {
                module.hook(target).setId(hookId).setExceptionMode(XposedInterface.ExceptionMode.PROTECTIVE).intercept(Hooker { chain -> block(chain) })
            } catch (_: Exception) {}
        }

        if (pkg == TargetNames.CN_PACKAGE || pkg == TargetNames.OVERSEA_PACKAGE) {
            try {
                val mainClazz = Class.forName("com.dragon.read.pages.main.MainFragmentActivity", false, classLoader)
                val nativeMethods = when {
                    pkg == TargetNames.OVERSEA_PACKAGE && gNames.profileId == "OVERSEA-7.3.5.32" -> {

                        listOf("onCreate", "B2", "J2", "o1", "n1")
                    }
                    pkg == TargetNames.OVERSEA_PACKAGE -> {

                        listOf("onCreate", "B2", "N1", "f1", "j2")
                    }
                    gNames.profileId == "CN-7.3.3.18" -> {

                        listOf("onCreate", "X0", "z2", "M1", "h2")
                    }
                    gNames.profileId == "CN-7.3.2.32" -> {

                        listOf("onCreate", "d1", "y2", "L1", "g2")
                    }
                    else -> {

                        listOf("onCreate", "d1", "z2", "L1", "g2")
                    }
                }
                for ((index, methodName) in nativeMethods.withIndex()) {
                    ham(mainClazz, methodName, "nativeBottomFrame_${pkg.hashCode()}_$index") { chain ->
                        val act = chain.thisObject as? Activity
                        val result = chain.proceed()
                        if (act != null && shouldApplyUiHiding() && gMasterOn && gControlOn && !(gRestoreControlsOnPause && gVideoPaused)) {
                            mainHandler.post { enforceNativeMainBottomHidden(act) }
                        }
                        result
                    }
                }
                LogUtil.info("  ✓ native MainFragmentActivity BottomTabFrameLayout | pkg=$pkg")
            } catch (e: Throwable) {
                LogUtil.warn("  native BottomTabFrameLayout Hook 未命中 | pkg=$pkg: $e")
            }

            try {
                val bottomFrameClazz = Class.forName("com.dragon.read.widget.BottomTabFrameLayout", false, classLoader)
                ham(bottomFrameClazz, "setBottomTabBackground", "nativeBottomBackground_${pkg.hashCode()}") { chain ->
                    val frame = chain.thisObject as? View
                    val result = chain.proceed()
                    if (shouldApplyUiHiding() && gMasterOn && gControlOn && !(gRestoreControlsOnPause && gVideoPaused)) {
                        updateNativeNavBarRestoreColor(gCurrentActivity)
                        collapseNativeMainBottomFrame(frame)
                        applyBottomEdgeToEdge(gCurrentActivity, gNavBarOff)
                    }
                    result
                }
                LogUtil.info("  ✓ BottomTabFrameLayout.setBottomTabBackground/windowNavColor | pkg=$pkg")
            } catch (e: Throwable) {
                LogUtil.warn("  BottomTabFrameLayout 背景 Hook 未命中 | pkg=$pkg: $e")
            }

            try {
                val maskClassName: String
                val maskMethodName: String
                val maskIdName: String
                when {
                    pkg == TargetNames.OVERSEA_PACKAGE -> {
                        maskClassName = "vq3.a"
                        maskMethodName = "d"
                        maskIdName = "bottom_tab_mask"
                    }
                    gNames.profileId == "CN-7.3.3.18" -> {
                        maskClassName = "bu3.a"
                        maskMethodName = "e"
                        maskIdName = "ar9"
                    }
                    else -> {

                        maskClassName = "it3.a"
                        maskMethodName = "d"
                        maskIdName = "ar8"
                    }
                }
                val maskController = Class.forName(maskClassName, false, classLoader)
                val bind = maskController.getDeclaredMethod(maskMethodName, View::class.java)
                module.hook(bind).setId("videoFeedTabBottomMask_${pkg.hashCode()}")
                    .setExceptionMode(XposedInterface.ExceptionMode.PROTECTIVE)
                    .intercept(Hooker { chain ->
                        val root = try { chain.getArg(0) as? View } catch (_: Throwable) { null }
                        val result = chain.proceed()
                        if (shouldApplyUiHiding() && gMasterOn && gControlOn && !(gRestoreControlsOnPause && gVideoPaused)) {
                            try {

                                val controllerMask = findFieldValue(chain.thisObject, "h") as? View
                                val byId = if (root != null) {
                                    val id = root.resources.getIdentifier(maskIdName, "id", gPkg)
                                    if (id != 0) root.findViewById<View>(id) else null
                                } else null
                                collapseNativeVideoFeedBottomMask(controllerMask ?: byId)
                                enforceNativeMainBottomHidden(gCurrentActivity)
                            } catch (_: Throwable) {}
                        }
                        result
                    })
                LogUtil.info("  ✓ VideoFeedTabBottomMask $maskClassName.$maskMethodName | id=$maskIdName")
            } catch (e: Throwable) {
                LogUtil.warn("  VideoFeedTabBottomMask Hook 未命中 | pkg=$pkg: $e")
            }

            try {
                val feedClazz = Class.forName(
                    "com.dragon.read.component.shortvideo.impl.feedtab.VideoFeedTabFragmentImpl",
                    false,
                    classLoader,
                )
                val layoutMethod = when {
                    pkg == TargetNames.OVERSEA_PACKAGE -> "Mf"
                    gNames.profileId == "CN-7.3.3.18" -> "Lg"

                    else -> "Eg"
                }
                ham(feedClazz, layoutMethod, "feedViewportBottomMargin_${pkg.hashCode()}") { chain ->
                    val root = try { chain.getArg(0) as? View } catch (_: Throwable) { null }
                    val exactOldFeed = pkg == TargetNames.CN_PACKAGE &&
                        (gNames.profileId == "CN-7.3.1.32" || gNames.profileId == "CN-7.3.2.32")
                    val oldMarker = gInsideFeedBottomMarginWrite.get() == true
                    if (exactOldFeed) gInsideFeedBottomMarginWrite.set(true)
                    val result = try { chain.proceed() } finally {
                        if (exactOldFeed) gInsideFeedBottomMarginWrite.set(oldMarker)
                    }
                    if (shouldApplyUiHiding() && gMasterOn && gControlOn && !(gRestoreControlsOnPause && gVideoPaused)) {

                        if (gNames.profileId == "CN-7.3.2.32" || gNames.profileId == "CN-7.3.1.32") {
                            for (delay in longArrayOf(0L, 32L, 120L)) {
                                mainHandler.postDelayed({
                                    if (shouldApplyUiHiding() && gMasterOn && gControlOn && !(gRestoreControlsOnPause && gVideoPaused)) {
                                        reclaimFeedViewportBottomMargin(root)
                                        enforceKnownFeedViewportBottomMargin()
                                    }
                                }, delay)
                            }
                        } else {
                            mainHandler.post { reclaimFeedViewportBottomMargin(root) }
                        }
                    }
                    result
                }

                ham(feedClazz, "onConfigurationChanged", "feedViewportConfig_${pkg.hashCode()}") { chain ->
                    val result = chain.proceed()
                    if (shouldApplyUiHiding() && gMasterOn && gControlOn && !(gRestoreControlsOnPause && gVideoPaused)) {
                        val root = try { gCurrentActivity?.window?.decorView } catch (_: Throwable) { null }
                        for (delay in longArrayOf(0L, 24L, 90L, 180L)) {
                            mainHandler.postDelayed({
                                if (shouldApplyUiHiding() && gMasterOn && gControlOn && !(gRestoreControlsOnPause && gVideoPaused)) {
                                    reclaimFeedViewportBottomMargin(root)
                                    enforceKnownFeedViewportBottomMargin()
                                }
                            }, delay)
                        }
                    }
                    result
                }
                LogUtil.info("  ✓ VideoFeedTabFragmentImpl.$layoutMethod(View) bottomMargin=0 | pkg=$pkg")
            } catch (e: Throwable) {
                LogUtil.warn("  VideoFeedTabFragmentImpl bottomMargin Hook 未命中 | pkg=$pkg: $e")
            }

            if (pkg == TargetNames.CN_PACKAGE &&
                (gNames.profileId == "CN-7.3.1.32" || gNames.profileId == "CN-7.3.2.32")) {
                try {
                    val uiUtils = Class.forName("com.bytedance.common.utility.UIUtils", false, classLoader)
                    val updateMargin = uiUtils.getDeclaredMethod(
                        "updateLayoutMargin",
                        View::class.java,
                        Int::class.javaPrimitiveType,
                        Int::class.javaPrimitiveType,
                        Int::class.javaPrimitiveType,
                        Int::class.javaPrimitiveType,
                    )
                    module.hook(updateMargin)
                        .setId("oldFeedBottomMarginWrite_${pkg.hashCode()}")
                        .setExceptionMode(XposedInterface.ExceptionMode.PROTECTIVE)
                        .intercept(Hooker { chain ->
                            var replacementArgs: Array<Any?>? = null
                            if (gInsideFeedBottomMarginWrite.get() == true &&
                                gMasterOn && gControlOn && !(gRestoreControlsOnPause && gVideoPaused)) {
                                val view = try { chain.getArg(0) as? View } catch (_: Throwable) { null }
                                val bottom = try { chain.getArg(4) as? Int } catch (_: Throwable) { null }
                                if (view != null && bottom != null && bottom > 0) {
                                    replacementArgs = (chain.args as Array<Any?>).copyOf().also { it[4] = 0 }
                                    synchronized(gKnownFeedViewportViews) { gKnownFeedViewportViews[view] = true }
                                    synchronized(gFeedViewportNativeBottomMargins) { gFeedViewportNativeBottomMargins[view] = bottom }
                                    LogUtil.info("旧版首页 bottomMargin 写入已拦截 | profile=${gNames.profileId} | id=${viewEntryName(view)} | ${bottom}px -> 0")
                                    LogUtil.incr("oldFeedBottomMarginWriteBlock")
                                }
                            }
                            if (replacementArgs != null) chain.proceed(replacementArgs) else chain.proceed()
                        })
                    LogUtil.info("  ✓ old CN UIUtils.updateLayoutMargin 精确拦截 | profile=${gNames.profileId}")
                } catch (e: Throwable) {
                    LogUtil.warn("  old CN updateLayoutMargin 精确拦截未命中: $e")
                }
            }
        }

        try {
            val agencyClass = Class.forName(gNames.rightViewAgency, false, classLoader)
            hac(agencyClass, "rightViewAgencyCtor") { chain ->
                val result = chain.proceed()
                registerRightViewAgency(chain.thisObject)
                result
            }

            ham(agencyClass, gNames.rightViewAgencyEventMethod, "rightViewAgencyEvent") { chain ->
                registerRightViewAgency(chain.thisObject)
                chain.proceed()
            }
            LogUtil.info("  ✓ 评论区 Agency ${gNames.rightViewAgency}.${gNames.rightViewAgencyEventMethod}")
        } catch (e: Throwable) {
            LogUtil.warn("  评论区 Agency Hook 失败: $e")
        }

        var doubleTapHookCount = 0
        for ((handlerIndex, handlerName) in gNames.doubleTapHandlers.withIndex()) {
            try {
                val doubleTapClass = Class.forName(handlerName, false, classLoader)
                val methods = doubleTapClass.declaredMethods.filter {
                    it.name == "onDoubleTap" && it.parameterCount == 1 &&
                        it.parameterTypes[0] == android.view.MotionEvent::class.java
                }
                for ((methodIndex, method) in methods.withIndex()) {
                    module.hook(method).setId("doubleTapComment_${handlerIndex}_${methodIndex}")
                        .setExceptionMode(XposedInterface.ExceptionMode.PROTECTIVE)
                        .intercept(Hooker { chain ->
                            if (!gMasterOn || !gDoubleTapCommentOn) return@Hooker chain.proceed()
                            val now = android.os.SystemClock.uptimeMillis()

                            gSuppressDoubleTapLikeUntil = maxOf(gSuppressDoubleTapLikeUntil, now + 1500L)

                            if (now - gLastDoubleTapCommentAt > 350L) {
                                gLastDoubleTapCommentAt = now
                                val opened = openCurrentCommentFromDoubleTap()
                                LogUtil.incr(if (opened) "doubleTapOpenCommentOK" else "doubleTapOpenCommentNoAgency")
                            } else {
                                LogUtil.incr("doubleTapDuplicateConsumed")
                            }

                            LogUtil.incr("doubleTapLikeBlocked")
                            true
                        })
                    doubleTapHookCount++
                    LogUtil.info("  ✓ 双击评论 $handlerName.onDoubleTap")
                }
            } catch (e: Throwable) {
                LogUtil.warn("  双击处理器 $handlerName Hook 失败: $e")
            }
        }
        if (doubleTapHookCount == 0) LogUtil.warn("  双击评论：当前版本没有安装到任何 onDoubleTap Hook")

        if (gNames.doubleTapLikeView.isNotBlank()) {
            try {
                val likeViewClass = Class.forName(gNames.doubleTapLikeView, false, classLoader)
                likeViewClass.declaredMethods.filter {
                    it.name == "a" && it.parameterCount == 1 &&
                        it.parameterTypes[0].name == "kotlin.Pair" && it.returnType == Void.TYPE
                }.forEachIndexed { index, method ->
                    module.hook(method).setId("doubleTapDiggBlock_$index")
                        .setExceptionMode(XposedInterface.ExceptionMode.PROTECTIVE)
                        .intercept(Hooker { chain ->
                            val now = android.os.SystemClock.uptimeMillis()
                            if (gMasterOn && gDoubleTapCommentOn && now <= gSuppressDoubleTapLikeUntil) {
                                LogUtil.incr("doubleTapDiggActionBlocked")
                                null
                            } else chain.proceed()
                        })
                }
                LogUtil.info("  ✓ 双击点赞动作兜底 ${gNames.doubleTapLikeView}.a(Pair)")
            } catch (e: Throwable) {
                LogUtil.warn("  双击点赞动作兜底安装失败: $e")
            }
        }

        if (gNames.doubleTapHolderLikeMethod.isNotBlank()) {
            try {
                val holderClass = Class.forName(gNames.shortHolder, false, classLoader)
                holderClass.declaredMethods.filter {
                    it.name == gNames.doubleTapHolderLikeMethod &&
                        it.parameterCount == 0 && it.returnType == Void.TYPE
                }.forEachIndexed { index, method ->
                    module.hook(method).setId("doubleTapHolderLikeBlock_$index")
                        .setExceptionMode(XposedInterface.ExceptionMode.PROTECTIVE)
                        .intercept(Hooker { chain ->
                            val now = android.os.SystemClock.uptimeMillis()
                            if (gMasterOn && gDoubleTapCommentOn && now <= gSuppressDoubleTapLikeUntil) {
                                LogUtil.incr("doubleTapHolderLikeBlocked")
                                null
                            } else chain.proceed()
                        })
                }
                LogUtil.info("  ✓ 双击最终点赞兜底 ${gNames.shortHolder}.${gNames.doubleTapHolderLikeMethod}()")
            } catch (e: Throwable) {
                LogUtil.warn("  双击最终点赞兜底安装失败: $e")
            }
        }

        if (gNames.resolutionController.isNotBlank() || gNames.resolutionApplyMethod.isBlank()) {
            try {
                // 控制器混淆类名可能已随版本失效。这里必须容错：若直接抛异常，
                // 整个画质分支（含下方 TTVideoEngine 引擎侧 hook）都会被一起跳过，
                // 表现为「自动最高画质完全失效」。占位类不会有同名方法，
                // ham() 过滤后自然不会安装任何 hook。
                val controllerClass = try {
                    Class.forName(gNames.resolutionController, false, classLoader)
                } catch (_: Throwable) {
                    LogUtil.warn("  画质控制器 ${gNames.resolutionController} 不存在，仅安装引擎侧 hook")
                    Any::class.java
                }
                val engineField = if (gNames.resolutionEngineField.isNotBlank()) {
                    runCatching {
                        controllerClass.getDeclaredField(gNames.resolutionEngineField).apply { isAccessible = true }
                    }.getOrNull()
                } else null
                for ((methodIndex, methodName) in gNames.resolutionModelMethods.withIndex()) {
                    ham(controllerClass, methodName, "maxQualityModel_${methodIndex}") { chain ->
                        val controller = chain.thisObject
                        val model = try { chain.getArg(0) } catch (_: Throwable) { null }
                        val highest = findHighestResolution(model)

                        if (gNames.resolutionApplyMethod.isBlank()) {

                            try {
                                val engineBefore = if (engineField != null && controller != null) engineField.get(controller) else null
                                if (engineBefore != null && highest != null) gEngineMaxResolution[engineBefore] = highest
                            } catch (_: Throwable) {}
                        } else if (controller != null && highest != null) {
                            gControllerMaxResolution[controller] = highest
                        }

                        val result = chain.proceed()
                        try {
                            if (highest != null) {
                                LogUtil.info("最高画质：检测到 $highest rank=${resolutionRank(highest)}")
                                if (gNames.resolutionApplyMethod.isNotBlank()) {
                                    if (gMasterOn && gMaxQualityOn) applyHighestViaController(controller, highest)
                                } else if (engineField != null) {
                                    val engine = if (controller != null) engineField.get(controller) else null
                                    rememberAndApplyHighestResolution(engine, model)
                                } else if (gMasterOn && gMaxQualityOn && result != null) {
                                    val targetVideoInfo = findVideoInfoByResolution(model, highest)
                                    if (targetVideoInfo != null && targetVideoInfo !== result) {
                                        LogUtil.info("最高画质：替换播放流 -> $highest")
                                        LogUtil.incr("maxQualityApply")
                                        return@ham targetVideoInfo
                                    }
                                }
                            }
                        } catch (e: Throwable) {
                            LogUtil.warn("最高画质：应用失败: $e")
                        }
                        result
                    }
                }

                if (gNames.resolutionApplyMethod.isBlank()) {
                    val engineClass = Class.forName("com.ss.ttvideoengine.TTVideoEngine", false, classLoader)
                    try {
                        val vcDiagClass = Class.forName(gNames.playbackState, false, classLoader)
                        ham(vcDiagClass, "onVideoStreamBitrateChanged", "maxQualityDiag") { chain ->
                            try {
                                val res = chain.getArg(0)
                                LogUtil.info("最高画质：实际播放流=$res")
                            } catch (_: Throwable) {}
                            chain.proceed()
                        }
                    } catch (_: Throwable) {}
                    ham(engineClass, "setVideoModel", "maxQualityModelSource") { chain ->
                        val result = chain.proceed()
                        try {
                            gLastVideoModelAt = android.os.SystemClock.uptimeMillis()
                            val engine = chain.thisObject
                            val model = chain.getArg(0)
                            val highest = findHighestResolution(model)
                            if (engine != null && highest != null) {
                                gEngineMaxResolution[engine] = highest
                                LogUtil.info("最高画质：model 来源 $highest")
                                if (gMasterOn && gMaxQualityOn) {
                                    try {
                                        engineClass.getDeclaredMethod("configResolution", highest.javaClass)
                                            .invoke(engine, highest)
                                        LogUtil.incr("maxQualityApply")
                                        LogUtil.info("最高画质：已请求引擎切换 $highest")
                                    } catch (_: Throwable) {}
                                }
                            }
                        } catch (_: Throwable) {}
                        result
                    }
                    ham(engineClass, "configResolution", "maxQualityConfig") { chain ->
                        if (!gMasterOn || !gMaxQualityOn) return@ham chain.proceed()
                        val engine = chain.thisObject
                        var highest = try { gEngineMaxResolution[engine] } catch (_: Throwable) { null }
                        if (highest == null) {
                            try {
                                val model = engineClass.getDeclaredMethod("getVideoModel").invoke(engine)
                                highest = findHighestResolution(model)
                                if (highest != null) gEngineMaxResolution[engine] = highest
                            } catch (_: Throwable) {}
                        }
                        if (highest == null) return@ham chain.proceed()
                        try {
                            val requested = chain.getArg(0)
                            if (requested !== highest && resolutionRank(requested) < resolutionRank(highest)) {
                                val args = (chain.args as Array<Any?>).copyOf()
                                args[0] = highest
                                LogUtil.info("最高画质：拦截 $requested -> $highest")
                                LogUtil.incr("maxQualityOverride")
                                return@ham chain.proceed(args)
                            }
                        } catch (_: Throwable) {}
                        chain.proceed()
                    }
                    LogUtil.info("  ✓ 默认最高画质 ${gNames.resolutionController} + TTVideoEngine")
                } else {
                    LogUtil.info("  ✓ 默认最高画质 ${gNames.resolutionController}.${gNames.resolutionApplyMethod}（原生切画质链）")
                }
            } catch (e: Throwable) {
                LogUtil.warn("  默认最高画质 Hook 失败: $e")
            }
        }

        try {
            val c = Class.forName("android.app.Activity", false, classLoader)
            module.hook(c.getDeclaredMethod("onWindowFocusChanged", Boolean::class.java))
                .setId("wf")
                .setExceptionMode(XposedInterface.ExceptionMode.PROTECTIVE)
                .intercept(Hooker { chain ->
                    val focused = chain.getArg(0) as? Boolean == true
                    val result = chain.proceed()
                    if (focused) {
                        val a = chain.thisObject as? Activity
                        try {
                            if (gMasterOn && gStatusOn) applyCleanTop(a) else showStatusBar(a)
                            if (gMasterOn && gNavBarOff) applyNavBar(a) else showNavBar(a)
                        } catch (_: Exception) {}
                    }
                    result
                })
            LogUtil.info("  ✓ winFocus")
        } catch (e: Exception) { LogUtil.error("wf", e) }

        try {
            val c = Class.forName("android.app.Activity", false, classLoader)
            ham(c, "onMultiWindowModeChanged", "multiWindow") { chain ->
                val result = chain.proceed()
                reapplyAfterWindowModeChange(chain.thisObject as? Activity, "multi-window")
                result
            }
            ham(c, "onPictureInPictureModeChanged", "pictureInPicture") { chain ->
                val result = chain.proceed()
                reapplyAfterWindowModeChange(chain.thisObject as? Activity, "picture-in-picture")
                result
            }
            ham(c, "onConfigurationChanged", "windowConfiguration") { chain ->
                val result = chain.proceed()
                reapplyAfterWindowModeChange(chain.thisObject as? Activity, "configuration")
                result
            }
            LogUtil.info("  ✓ window mode callbacks")
        } catch (e: Throwable) { LogUtil.warn("  window mode callbacks missing: $e") }

        val seriesActivityClasses = listOf(
            "com.dragon.read.component.shortvideo.impl.ShortSeriesActivity",
            "com.dragon.read.component.shortvideo.impl.seriesdetail.ShortSeriesDetailActivity",
            "com.dragon.read.component.shortvideo.impl.albumdetail.VideoAlbumDetailActivity",
        )
        for ((classIndex, className) in seriesActivityClasses.withIndex()) {
            try {
                val c = Class.forName(className, false, classLoader)
                for (methodName in listOf(
                    "onConfigurationChanged",
                    "onMultiWindowModeChanged",
                    "onPictureInPictureModeChanged",
                )) {
                    ham(c, methodName, "seriesWindow_${classIndex}_$methodName") { chain ->
                        val result = chain.proceed()
                        reapplySeriesPageState(
                            chain.thisObject,
                            "$className#$methodName",
                            refreshLayout = true,
                        )
                        result
                    }
                }
                LogUtil.info("  ✓ series window tracker: $className")
            } catch (e: Throwable) {
                LogUtil.warn("  series window tracker missing: $className: $e")
            }
        }

        try {
            val className =
                "com.dragon.read.component.shortvideo.impl.v2.ShortSeriesSingleFragment"
            val c = Class.forName(className, false, classLoader)
            hac(c, "seriesFragmentCtor") { chain ->
                val result = chain.proceed()
                registerShortSeriesFragment(chain.thisObject)
                result
            }
            ham(c, "onResume", "seriesFragment_onResume") { chain ->
                val result = chain.proceed()
                registerShortSeriesFragment(chain.thisObject)
                gResumedShortSeriesFragment = java.lang.ref.WeakReference(chain.thisObject)
                mainHandler.post {
                    if (shouldApplyUiHiding()) {
                        setVideoPaused(false, "series-fragment-onResume")
                        scanAllWindows()
                    }
                }
                result
            }
            ham(c, "onPause", "seriesFragment_onPause") { chain ->
                val result = chain.proceed()
                if (gResumedShortSeriesFragment?.get() === chain.thisObject) {
                    gResumedShortSeriesFragment = null
                }
                restoreAfterSeriesDetailExit()
                result
            }
            ham(c, "onConfigurationChanged", "seriesFragment_onConfigurationChanged") { chain ->
                val result = chain.proceed()
                registerShortSeriesFragment(chain.thisObject)
                reapplySeriesPageState(
                    chain.thisObject,
                    "$className#onConfigurationChanged",
                    refreshLayout = true,
                )
                result
            }
            LogUtil.info("  ✓ series fragment window tracker")
        } catch (e: Throwable) {
            LogUtil.warn("  series fragment window tracker missing: $e")
        }

        try {
            val c = Class.forName("com.dragon.read.base.ui.util.StatusBarUtil", false, classLoader)
            ham(c, "clearFullScreenFlag", "sbC") { chain ->
                val result = chain.proceed()
                if (shouldHideStatusBar()) {
                    val a = chain.getArg(0) as? Activity
                    mainHandler.post { applyCleanTop(a) }
                }
                result
            }
            ham(c, "hideStatusBar", "sbH") { chain ->
                if (!shouldHideStatusBar()) {
                    chain.proceed()
                } else try {
                    if (chain.args.size >= 2 && chain.getArg(1) is Boolean && !(chain.getArg(1) as Boolean)) {
                        val args = (chain.args as Array<Any?>).copyOf()
                        args[1] = true
                        chain.proceed(args)
                    } else chain.proceed()
                } catch (_: Exception) { chain.proceed() }
            }

            ham(c, "getStatusHeight", "sbHeight") { chain ->
                if (shouldHideStatusBar()) 0 else chain.proceed()
            }
            LogUtil.info("  ✓ StatusBarUtil")
        } catch (e: Exception) { LogUtil.warn("  StatusBarUtil 未找到") }

        try {
            val c = Class.forName("com.bytedance.ies.uikit.statusbar.StatusBarUtils", false, classLoader)
            module.hook(c.getDeclaredMethod("getStatusBarHeight", Context::class.java))
                .setId("uikitStatusHeight")
                .setExceptionMode(XposedInterface.ExceptionMode.PROTECTIVE)
                .intercept(Hooker { chain ->
                    if (shouldHideStatusBar()) 0 else chain.proceed()
                })
            LogUtil.info("  ✓ UIKit StatusBarUtils")
        } catch (e: Exception) { LogUtil.warn("  UIKit StatusBarUtils 未找到: $e") }

        try {
            val isNewCn = gNames.profileId == "CN-7.3.3.18"
            val isOversea = gNames.profileId == "OVERSEA-7.3.1.32"

            // 旧版本单一路径，保留为兜底
            val legacyPlayer = when {
                isNewCn -> "nx4.w"
                isOversea -> "ys4.x"
                else -> "ov4.x"
            }
            val legacyControllerClass = when {
                isNewCn -> "com.dragon.read.component.shortvideo.impl.v2.view.adapter.a"
                isOversea -> "lt4.v"
                else -> "bw4.v"
            }
            val legacySetMethod = when {
                isNewCn -> "v2"
                isOversea -> "u2"
                else -> "r2"
            }
            val legacyCacheMethod = if (isNewCn) "w" else "getCacheVideoSpeed"

            // 候选 + 运行时签名校验：判定依据是「类里确实声明了 setPlaySpeed(int)」，
            // 而不是类名本身。旧逻辑直接 Class.forName("ov4.x") 后取方法，一旦
            // 类名被复用给别的类就会抛 NoSuchMethodException，整段倍速功能全丢。
            val playerClass = resolveClassWithMethod(
                gNames.percentPlayerCandidates + legacyPlayer,
                "setPlaySpeed",
                arrayOf(Integer.TYPE),
                classLoader,
            ) ?: throw NoSuchMethodException(
                "未找到声明 setPlaySpeed(int) 的播放器类，候选=${gNames.percentPlayerCandidates + legacyPlayer}"
            )
            val percentPlayerClassName = playerClass.name

            val controllerSetMethod = gNames.speedControllerSetMethod.ifBlank { legacySetMethod }
            val controllerCacheMethod = gNames.speedControllerCacheMethod.ifBlank { legacyCacheMethod }
            val controllerClassName = resolveClassWithMethod(
                gNames.speedControllerCandidates + legacyControllerClass,
                "getCurrentPlaySpeed",
                emptyArray(),
                classLoader,
            )?.name ?: legacyControllerClass

            hac(playerClass, "defaultSpeedPlayerCtor") { chain ->
                val result = chain.proceed()
                try {
                    gKnownPercentSpeedPlayers[chain.thisObject] = true
                    if (defaultSpeedEnabledNow()) {
                        val player = chain.thisObject
                        mainHandler.postDelayed({ applySpeedToPercentPlayer(player, "player-ctor") }, 120L)
                    }
                } catch (_: Throwable) {}
                result
            }
            val setPlaySpeed = playerClass.getDeclaredMethod("setPlaySpeed", Integer.TYPE)
            module.hook(setPlaySpeed).setId("defaultSpeedPercentPlayer")
                .setExceptionMode(XposedInterface.ExceptionMode.PROTECTIVE)
                .intercept(Hooker { chain ->
                    try { gKnownPercentSpeedPlayers[chain.thisObject] = true } catch (_: Throwable) {}
                    if (!defaultSpeedEnabledNow()) return@Hooker chain.proceed()
                    val wanted = defaultSpeedPercent()
                    val requested = try { chain.getArg(0) as? Int } catch (_: Throwable) { null }
                    if (requested == wanted) return@Hooker chain.proceed()
                    val args = (chain.args as Array<Any?>).copyOf()
                    args[0] = wanted
                    LogUtil.incr("defaultSpeedPercentForce")
                    chain.proceed(args)
                })

            // 控制器侧整体容错：失败不能影响已安装的播放器侧 hook，
            // 也不能影响其后的 autoplay(setSpeed/float) 侧 hook。
            try {
                val controllerClass = Class.forName(controllerClassName, false, classLoader)
                // 先按档案里的方法名找；找不到再按签名 (boolean, float, boolean) 兜底，
                // 这样混淆方法名变化时依然能命中。
                val controllerSetter = runCatching {
                    controllerClass.getDeclaredMethod(
                        controllerSetMethod,
                        Boolean::class.javaPrimitiveType,
                        java.lang.Float.TYPE,
                        Boolean::class.javaPrimitiveType,
                    )
                }.getOrNull() ?: controllerClass.declaredMethods.firstOrNull { m ->
                    m.parameterTypes.size == 3 &&
                        m.parameterTypes[0] == Boolean::class.javaPrimitiveType &&
                        m.parameterTypes[1] == java.lang.Float.TYPE &&
                        m.parameterTypes[2] == Boolean::class.javaPrimitiveType
                } ?: throw NoSuchMethodException(
                    "倍速控制器 setter 未找到: $controllerClassName | name=$controllerSetMethod | sig=(boolean,float,boolean)"
                )
                module.hook(controllerSetter).setId("defaultSpeedControllerSet")
                    .setExceptionMode(XposedInterface.ExceptionMode.PROTECTIVE)
                    .intercept(Hooker { chain ->
                        if (!defaultSpeedEnabledNow()) return@Hooker chain.proceed()
                        val args = (chain.args as Array<Any?>).copyOf()
                        args[1] = gDefaultSpeed
                        LogUtil.incr("defaultSpeedControllerForce")
                        chain.proceed(args)
                    })
                runCatching { controllerClass.getDeclaredMethod(controllerCacheMethod, String::class.java) }
                    .getOrNull()?.let { cacheGetter ->
                        module.hook(cacheGetter).setId("defaultSpeedCacheGetter")
                            .setExceptionMode(XposedInterface.ExceptionMode.PROTECTIVE)
                            .intercept(Hooker { chain ->
                                if (defaultSpeedEnabledNow()) gDefaultSpeed else chain.proceed()
                            })
                    }
                runCatching { controllerClass.getDeclaredMethod("getCurrentPlaySpeed") }
                    .getOrNull()?.let { currentGetter ->
                        module.hook(currentGetter).setId("defaultSpeedCurrentGetter")
                            .setExceptionMode(XposedInterface.ExceptionMode.PROTECTIVE)
                            .intercept(Hooker { chain ->
                                if (defaultSpeedEnabledNow()) defaultSpeedPercent() else chain.proceed()
                            })
                    }
                LogUtil.info("  ✓ 倍速控制器 $controllerClassName.$controllerSetMethod/$controllerCacheMethod")
            } catch (e: Throwable) {
                LogUtil.warn("  倍速控制器侧 Hook 失败（播放器侧已生效）: $e")
            }

            // autoplay(float) 侧：同样改为候选 + 签名校验(setSpeed(float))
            val autoplayClass = resolveClassWithMethod(
                gNames.floatPlayerCandidates + "com.dragon.read.component.shortvideo.impl.autoplay.o",
                "setSpeed",
                arrayOf(java.lang.Float.TYPE),
                classLoader,
            ) ?: throw NoSuchMethodException(
                "未找到声明 setSpeed(float) 的 autoplay 播放器类，候选=${gNames.floatPlayerCandidates}"
            )
            hac(autoplayClass, "defaultSpeedAutoplayCtor") { chain ->
                val result = chain.proceed()
                try {
                    gKnownFloatSpeedPlayers[chain.thisObject] = true
                    if (defaultSpeedEnabledNow()) {
                        val player = chain.thisObject
                        mainHandler.postDelayed({ applySpeedToFloatPlayer(player, "autoplay-ctor") }, 120L)
                    }
                } catch (_: Throwable) {}
                result
            }
            val autoSetSpeed = autoplayClass.getDeclaredMethod("setSpeed", java.lang.Float.TYPE)
            module.hook(autoSetSpeed).setId("defaultSpeedFloatPlayer")
                .setExceptionMode(XposedInterface.ExceptionMode.PROTECTIVE)
                .intercept(Hooker { chain ->
                    try { gKnownFloatSpeedPlayers[chain.thisObject] = true } catch (_: Throwable) {}
                    if (!defaultSpeedEnabledNow()) return@Hooker chain.proceed()
                    val requested = try { chain.getArg(0) as? Float } catch (_: Throwable) { null }
                    if (requested != null && kotlin.math.abs(requested - gDefaultSpeed) < 0.001f) return@Hooker chain.proceed()
                    val args = (chain.args as Array<Any?>).copyOf()
                    args[0] = gDefaultSpeed
                    LogUtil.incr("defaultSpeedFloatForce")
                    chain.proceed(args)
                })

            LogUtil.info("  ✓ 默认倍速 | profile=${gNames.profileId} | player=$percentPlayerClassName.setPlaySpeed | controller=$controllerClassName.$controllerSetMethod/$controllerCacheMethod | autoplay=setSpeed")
        } catch (e: Throwable) {
            LogUtil.warn("  默认倍速 Hook 失败 | profile=${gNames.profileId}: $e")
        }

        fun hookPlaybackState(className: String, hookId: String) {
            try {
                val c = Class.forName(className, false, classLoader)
                ham(c, "onPlaybackStateChanged", hookId) { chain ->
                    val result = chain.proceed()
                    try {
                        val state = chain.args.lastOrNull { it is Int } as? Int
                        when (state) {
                            1 -> {
                                setVideoPaused(false, "$className#state=1")
                                scheduleDefaultSpeedApply("$className#state=1")
                            }
                            2 -> setVideoPaused(true, "$className#state=2")
                            0, 3 -> if (gVideoPaused) refreshVideoPauseState("$className#state=$state", false)
                        }
                    } catch (e: Throwable) { LogUtil.warn("playback state parse failed: $className: $e") }
                    result
                }
                LogUtil.info("  ✓ playback detector: $className")
            } catch (e: Throwable) { LogUtil.warn("  playback detector missing: $className: $e") }
        }

        hookPlaybackState("com.ss.android.videoshop.controller.VideoController", "vcState")
        hookPlaybackState(gNames.playbackState, "nsState")

        val shortVideoHolderClasses = listOf(gNames.holderBaseS1)
        var shortPlaybackHookCount = 0
        shortVideoHolderClasses.forEachIndexed { classIndex, className ->
            try {
                val c = Class.forName(className, false, classLoader)
                ham(c, gNames.shortStateMethod, "shortState_${classIndex}") { chain ->
                    val result = chain.proceed()
                    try {
                        val holder = chain.thisObject
                        val state = chain.args.lastOrNull { it is Int } as? Int
                        registerShortVideoHolder(holder, state)
                        if (state == 1 || state == 2) {
                            mainHandler.post {
                                if (isShortVideoHolderVisible(holder)) {
                                    setVideoPaused(state == 2, "$className#${gNames.shortStateMethod}=$state")
                                    if (state == 1) scheduleDefaultSpeedApply("$className#${gNames.shortStateMethod}=1")
                                }
                            }
                        }
                    } catch (e: Throwable) {
                        LogUtil.warn("short-video playback parse failed: $className: $e")
                    }
                    result
                }
                shortPlaybackHookCount++
            } catch (_: Throwable) {}
        }
        try {
            val c = Class.forName(gNames.shortHolder, false, classLoader)
            hac(c, "shortHolderCtor") { chain ->
                val result = chain.proceed()
                registerShortVideoHolder(chain.thisObject)
                result
            }
            ham(c, "onBind", "shortHolderBind") { chain ->
                val result = chain.proceed()

                registerShortVideoHolder(chain.thisObject, 0)
                if (gMasterOn && !(gRestoreControlsOnPause && gVideoPaused)) {
                    if (gPlayerOn) setVideoToolbarsVisible(false)
                    val holderRoot = shortVideoHolderRoot(chain.thisObject)
                    if (holderRoot != null) scanTreeUnified(holderRoot)
                }
                result
            }

            try {
                val nativeClean = c.getDeclaredMethod(
                    gNames.shortControlsMethod,
                    Boolean::class.java,
                    Boolean::class.java,
                )
                module.hook(nativeClean)
                    .setId("shortNativeClean")
                    .setExceptionMode(XposedInterface.ExceptionMode.PROTECTIVE)
                    .intercept(Hooker { chain ->
                        if (!shouldForceShortVideoCleanMask()) return@Hooker chain.proceed()
                        try {
                            val requested = chain.getArg(0) as? Boolean
                            if (requested == false) {
                                val args = (chain.args as Array<Any?>).copyOf()
                                args[0] = true
                                LogUtil.incr("shortNativeCleanForce")
                                return@Hooker chain.proceed(args)
                            }
                        } catch (_: Throwable) {}
                        chain.proceed()
                    })
                LogUtil.info("  ✓ native clean-screen ${gNames.shortHolder}.${gNames.shortControlsMethod}")
            } catch (e: Throwable) {
                LogUtil.warn("  native clean-screen hook missing: $e")
            }

            module.hook(c.getDeclaredMethod(gNames.shortMaskMethod, Boolean::class.java))
                .setId("shortMask")
                .setExceptionMode(XposedInterface.ExceptionMode.PROTECTIVE)
                .intercept(Hooker { chain ->
                    val result = chain.proceed()
                    if (shouldForceShortVideoCleanMask()) rememberAndForceShortVideoMaskInvisible(chain.thisObject)
                    result
                })
        } catch (_: Throwable) {}

        if (gPkg == "com.phoenix.read") {
            try {
                val homeFragment = Class.forName(
                    "com.dragon.read.component.shortvideo.impl.v2.SeriesBookMallTabFragment",
                    false,
                    classLoader,
                )
                hac(homeFragment, "cnHomeFragmentCtor") { chain ->
                    val result = chain.proceed()
                    registerCnHomeFragment(chain.thisObject)
                    result
                }
                val homeMaskMethod = gNames.homeFragmentMaskMethod
                if (homeMaskMethod.isBlank()) throw NoSuchMethodException("homeFragmentMaskMethod blank")
                module.hook(homeFragment.getDeclaredMethod(homeMaskMethod, Boolean::class.java))
                    .setId("cnHomeMask")
                    .setExceptionMode(XposedInterface.ExceptionMode.PROTECTIVE)
                    .intercept(Hooker { chain ->
                        registerCnHomeFragment(chain.thisObject)

                        val result = if (shouldForceShortVideoCleanMask()) {
                            try {
                                val requested = chain.getArg(0) as? Boolean
                                if (requested == false) {
                                    val args = (chain.args as Array<Any?>).copyOf()
                                    args[0] = true
                                    LogUtil.incr("homeNativeCleanForce")
                                    chain.proceed(args)
                                } else chain.proceed()
                            } catch (_: Throwable) { chain.proceed() }
                        } else chain.proceed()
                        forceCnHomeFragmentMaskInvisible(chain.thisObject)
                        result
                    })
                LogUtil.info("  ✓ CN home fragment mask ${gNames.homeFragmentMaskMethod}/${gNames.homeFragmentMaskField}")
            } catch (e: Throwable) {
                LogUtil.warn("  CN home fragment mask missing: $e")
            }
        }

        LogUtil.info("  ✓ short-video playback detectors: $shortPlaybackHookCount")

        try {
            val c = Class.forName("com.ss.ttvideoengine.TTVideoEngine", false, classLoader)
            ham(c, "pause", "ttPause") { chain ->
                val result = chain.proceed()
                val completed = try {
                    val engine = chain.thisObject
                    val pos = (c.getDeclaredMethod("getCurrentPosition").invoke(engine) as? Number)?.toInt() ?: 0
                    val dur = (c.getDeclaredMethod("getDuration").invoke(engine) as? Number)?.toInt() ?: 0
                    dur > 0 && pos >= dur - 800
                } catch (_: Throwable) { false }
                setVideoPaused(true, if (completed) "TTVideoEngine#pause(completed)" else "TTVideoEngine#pause")
                result
            }
            ham(c, "play", "ttPlay") { chain ->
                val result = chain.proceed()
                setVideoPaused(false, "TTVideoEngine#play")
                scheduleDefaultSpeedApply("TTVideoEngine#play")
                result
            }
            LogUtil.info("  ✓ TTVideoEngine pause/play tracker")
        } catch (e: Throwable) { LogUtil.warn("  TTVideoEngine missing: $e") }

        try {
            val c = Class.forName("com.ss.android.videoshop.controller.VideoController", false, classLoader)
            ham(c, "pause", "vcPause") { chain ->
                val result = chain.proceed()
                setVideoPaused(true, "VideoController#pause")
                result
            }
            ham(c, "play", "vcPlay") { chain ->
                val result = chain.proceed()
                setVideoPaused(false, "VideoController#play")
                scheduleDefaultSpeedApply("VideoController#play")
                result
            }
            LogUtil.info("  ✓ VideoController pause/play tracker")
        } catch (e: Throwable) { LogUtil.warn("  VideoController pause/play missing: $e") }

        try {
            val c = Class.forName("com.ss.android.videoshop.mediaview.LayerHostMediaLayout", false, classLoader)
            ham(c, "pause", "lhPause") { chain ->
                val result = chain.proceed()
                setVideoPaused(true, "LayerHostMediaLayout#pause")
                result
            }
            ham(c, "play", "lhPlay") { chain ->
                val result = chain.proceed()
                setVideoPaused(false, "LayerHostMediaLayout#play")
                scheduleDefaultSpeedApply("LayerHostMediaLayout#play")
                result
            }
            ham(c, "execCommand", "videoCmd") { chain ->
                var command: Int? = null
                try {
                    val cmd = chain.getArg(0)
                    if (cmd != null) command = cmd.javaClass.getMethod("getCommand").invoke(cmd) as? Int
                } catch (_: Throwable) {}
                val result = chain.proceed()
                when (command) {
                    208 -> refreshVideoPauseState("command=208", true)
                    207, 214 -> refreshVideoPauseState("command=$command", false)
                }
                result
            }
            ham(c, "onVideoPause", "videoPauseDirect") { chain ->
                val result = chain.proceed()
                setVideoPaused(true, "LayerHostMediaLayout#onVideoPause")
                result
            }
            ham(c, "onVideoPlay", "videoPlayDirect") { chain ->
                val result = chain.proceed()
                setVideoPaused(false, "LayerHostMediaLayout#onVideoPlay")
                scheduleDefaultSpeedApply("LayerHostMediaLayout#onVideoPlay")
                if (gMasterOn && gPlayerOn && !(gRestoreControlsOnPause && gVideoPaused && !isEpisodeSwitchPause())) {
                    setVideoToolbarsVisible(false)
                    mainHandler.post { scanAllWindows() }
                }
                result
            }
            LogUtil.info("  ✓ video command detector")
        } catch (e: Throwable) { LogUtil.warn("  video command detector missing: $e") }

        fun hookToolbarLayer(className: String, hookId: String) {
            try {
                val c = Class.forName(className, false, classLoader)
                hac(c, hookId) { chain ->
                    val result = chain.proceed()
                    registerVideoToolbarLayer(chain.thisObject)
                    refreshVideoPauseState("layer-created:$className")
                    result
                }
                LogUtil.info("  ✓ toolbar layer tracker: $className")
            } catch (e: Throwable) { LogUtil.warn("  toolbar layer tracker missing: $className: $e") }
        }
        hookToolbarLayer("com.dragon.read.pages.video.layers.toolbarlayer.ToolbarLayerFixed", "fixedLayer")
        hookToolbarLayer("com.dragon.read.pages.video.customizelayers.CustomizeToolbarLayer", "customLayer")

        fun trackLayerFromCall(chain: XposedInterface.Chain) {
            try { registerVideoToolbarLayer(chain.thisObject) } catch (_: Throwable) {}
        }

        try { val c = Class.forName("com.dragon.read.pages.video.layers.toolbarlayer.ToolbarLayerFixed", false, classLoader)
            ham(c, gNames.fixedToolbarShowMethod, "pb") { chain -> trackLayerFromCall(chain); if (!gMasterOn || !gPlayerOn || (gRestoreControlsOnPause && gVideoPaused && !isEpisodeSwitchPause())) chain.proceed() else try { if (chain.getArg(0) as? Boolean == true) { LogUtil.incr("btmBlock"); null } else chain.proceed() } catch (_: Exception) { chain.proceed() } }
            LogUtil.info("  ✓ playerBtm") } catch (e: Exception) { LogUtil.warn("  ToolbarLayerFixed 未找到") }

        try { val c = Class.forName("com.dragon.read.pages.video.customizelayers.CustomizeToolbarLayer", false, classLoader)
            ham(c, gNames.customizeToolbarShowMethod, "pt") { chain -> trackLayerFromCall(chain); if (!gMasterOn || !gPlayerOn || (gRestoreControlsOnPause && gVideoPaused && !isEpisodeSwitchPause())) chain.proceed() else try { if (chain.getArg(0) as? Boolean == true) { LogUtil.incr("topBlock"); null } else chain.proceed() } catch (_: Exception) { chain.proceed() } }
            LogUtil.info("  ✓ playerTop") } catch (e: Exception) { LogUtil.warn("  CustomizeToolbarLayer 未找到") }

        try { val c = Class.forName("com.dragon.read.pages.video.customizelayers.CustomizeToolbarLayer", false, classLoader)
            module.hook(c.getDeclaredMethod(gNames.customizeToolbarApplyMethod, Boolean::class.java, Boolean::class.java, Boolean::class.java)).setId("ctS").setExceptionMode(XposedInterface.ExceptionMode.PROTECTIVE).intercept(Hooker { chain -> trackLayerFromCall(chain); if (!gMasterOn || !gPlayerOn || (gRestoreControlsOnPause && gVideoPaused && !isEpisodeSwitchPause())) chain.proceed() else try { if (chain.getArg(0) as? Boolean == true) { LogUtil.incr("topBlock"); null } else chain.proceed() } catch (_: Exception) { chain.proceed() } })
            LogUtil.info("  ✓ CustomizeToolbarLayer.${gNames.customizeToolbarApplyMethod}") } catch (e: Exception) { LogUtil.warn("  CustomizeToolbarLayer.${gNames.customizeToolbarApplyMethod} 未找到") }

        try { val c = Class.forName(gNames.toolbarBase, false, classLoader)
            hac(c, "toolbarBaseCtor") { chain -> val r = chain.proceed(); registerToolbarBaseLayer(chain.thisObject); r }
            module.hook(c.getDeclaredMethod("a", Boolean::class.java)).setId("gt7c").setExceptionMode(XposedInterface.ExceptionMode.PROTECTIVE).intercept(Hooker { chain ->
                registerToolbarBaseLayer(chain.thisObject)
                if (!gMasterOn || !gPlayerOn || (gRestoreControlsOnPause && gVideoPaused && !isEpisodeSwitchPause())) chain.proceed() else try { if (chain.getArg(0) as? Boolean == true) { LogUtil.incr("toolbarBlock"); null } else chain.proceed() } catch (_: Exception) { chain.proceed() }
            })
            LogUtil.info("  ✓ ${gNames.toolbarBase}.a") } catch (e: Exception) { LogUtil.warn("  ${gNames.toolbarBase} 未找到") }

        try {
            val mgrClass = Class.forName("com.dragon.read.base.ssconfig.SsConfigMgr", false, classLoader)
            ham(mgrClass, "getABValueJson", "downloadLimitAB") { chain ->
                try {
                    val key = chain.getArg(0)?.toString()
                    if (key == DOWNLOAD_LIMIT_AB_KEY && downloadUnlimitEnabledNow()) {
                        val json = customDownloadLimitJson()
                        LogUtil.incr("downloadLimitABOverride")
                        LogUtil.info("download limit override: SsConfigMgr.getABValueJson -> $json profile=${gNames.profileId}")
                        return@ham json
                    }
                } catch (e: Throwable) {
                    LogUtil.warn("download limit AB override failed: ${e.javaClass.simpleName}: ${e.message}")
                }
                chain.proceed()
            }
            LogUtil.info("  ✓ 下载数量限制 AB Hook SsConfigMgr.getABValueJson")
        } catch (e: Throwable) {
            LogUtil.warn("  下载数量限制 AB Hook 失败: $e")
        }

        try {
            val svcClass = Class.forName("s93.q", false, classLoader)
            val a7Methods = svcClass.declaredMethods.filter { it.name == "A7" && it.parameterCount >= 1 }
            a7Methods.forEachIndexed { index, method ->
                module.hook(method).setId("downloadLimitKmpA7_$index")
                    .setExceptionMode(XposedInterface.ExceptionMode.PROTECTIVE)
                    .intercept(Hooker { chain ->
                        val key = try { chain.getArg(0)?.toString() } catch (_: Throwable) { null }
                        if (key == DOWNLOAD_LIMIT_AB_KEY && downloadUnlimitEnabledNow()) {
                            val json = customDownloadLimitJson()
                            LogUtil.incr("downloadLimitKmpOverride")
                            LogUtil.info("download limit override: s93.q.A7 -> $json profile=${gNames.profileId}")
                            json
                        } else chain.proceed()
                    })
            }
            if (a7Methods.isNotEmpty()) LogUtil.info("  ✓ 下载数量限制 KMP Hook s93.q.A7")
        } catch (_: Throwable) {}

        try {
            val configClass = Class.forName("xh5.f", false, classLoader)
            hac(configClass, "downloadLimitConfigCtor") { chain ->
                val result = chain.proceed()
                if (downloadUnlimitEnabledNow() && overwriteDownloadLimitObject(chain.thisObject)) {
                    LogUtil.incr("downloadLimitObjectOverride")
                    LogUtil.info("download limit override: xh5.f ctor a/b/c=${currentDownloadLimitValues()}")
                }
                result
            }
            try {
                val serializerClass = Class.forName("xh5.f\$a", false, classLoader)
                ham(serializerClass, "deserialize", "downloadLimitDeserialize") { chain ->
                    val result = chain.proceed()
                    if (downloadUnlimitEnabledNow() && overwriteDownloadLimitObject(result)) {
                        LogUtil.incr("downloadLimitDeserializeOverride")
                        LogUtil.info("download limit override: xh5.f\$a.deserialize a/b/c=${currentDownloadLimitValues()}")
                    }
                    result
                }
            } catch (_: Throwable) {}
            LogUtil.info("  ✓ 下载数量限制对象 Hook xh5.f")
        } catch (_: Throwable) {}

        try { val c = Class.forName("com.dragon.read.component.biz.impl.BsGoldBoxServiceImpl", false, classLoader); ham(c, "tryAttach", "gb") { chain -> if (gMasterOn && gAdOn) null else chain.proceed() }; LogUtil.info("  ✓ goldBox") } catch (e: Exception) { LogUtil.warn("  BsGoldBoxServiceImpl 未找到") }
        try { val c = Class.forName("com.bytedance.ug.sdk.novel.pendant.PlanPendantServiceImpl", false, classLoader); ham(c, "triggerEvent", "pd") { chain -> if (gMasterOn && gAdOn) null else chain.proceed() }; LogUtil.info("  ✓ pendant") } catch (e: Exception) { LogUtil.warn("  PlanPendantServiceImpl 未找到") }

        try { var rc: Class<*>? = null; try { rc = Class.forName("com.dragon.read.component.biz.impl.bookmall.holder.video.VideoRedPacketHolder", false, classLoader) } catch (_: Exception) {}; if (rc != null) { hac(rc, "rp") { chain -> if (!gMasterOn || !gAdOn) chain.proceed() else { val r = chain.proceed(); try { val iv = chain.thisObject.javaClass.getField("itemView").get(chain.thisObject) as? View; iv?.visibility = View.GONE; iv?.layoutParams = ViewGroup.LayoutParams(0, 0) } catch (_: Exception) {}; r } }; LogUtil.info("  ✓ redPack") } } catch (e: Exception) { LogUtil.warn("redPack: $e") }

        try { val c = Class.forName(gNames.shortHolder, false, classLoader); module.hook(c.getDeclaredMethod(gNames.shortLandscapeMethod, Boolean::class.java)).setId("fb").setExceptionMode(XposedInterface.ExceptionMode.PROTECTIVE).intercept(Hooker { chain -> if (!gMasterOn || !gControlOn || (gRestoreControlsOnPause && gVideoPaused)) chain.proceed() else try { if (chain.getArg(0) as? Boolean == true) { val na = (chain.args as Array<Any?>).copyOf(); na[0] = false; chain.proceed(na) } else chain.proceed() } catch (_: Exception) { chain.proceed() } }); LogUtil.info("  ✓ ${gNames.shortHolder}.${gNames.shortLandscapeMethod}") } catch (e: Exception) { LogUtil.warn("  ${gNames.shortHolder} 未找到") }

        try {
            val c = Class.forName("android.view.LayoutInflater", false, classLoader)
            // 只挂终端重载：其余 3 个 inflate 最终都会委托到它。
            // 用 ham 会把 4 个重载全挂上，同一次 inflate 会命中 2~3 层 → 同一棵子树被重复扫描。
            hamExact(
                c, "inflate",
                listOf("org.xmlpull.v1.XmlPullParser", "android.view.ViewGroup", "boolean"),
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
            LogUtil.info("  ✓ inflate（仅终端重载）")
        } catch (e: Exception) { LogUtil.error("inflate", e) }

        try {
            val c = Class.forName("android.view.View", false, classLoader)
            ham(c, "setVisibility", "sv") { chain ->
                if (gInternalViewMutation.get() == true) return@ham chain.proceed()
                val view = chain.thisObject as? View
                val targetVisibility = chain.getArg(0) as? Int

                if (view != null && targetVisibility != null) {
                    synchronized(gSavedViewStates) {
                        gSavedViewStates[view]?.visibility = targetVisibility
                    }
                }

                if (!shouldApplyUiHiding() || !gMasterOn || (gRestoreControlsOnPause && gVideoPaused)) return@ham chain.proceed()
                try {
                    LogUtil.incr("setVis")
                    if (targetVisibility == View.VISIBLE && view != null) {
                        val replacement = when {
                            shouldForceShortVideoCleanMask() && synchronized(gCleanMaskViews) { gCleanMaskViews.containsKey(view) } -> View.INVISIBLE
                            gRefreshOff && (synchronized(gKnownRefreshAccessoryViews) { gKnownRefreshAccessoryViews.containsKey(view) } || isRefreshAccessoryContainer(view)) -> {
                                synchronized(gKnownRefreshAccessoryViews) { gKnownRefreshAccessoryViews[view] = true }
                                View.GONE
                            }
                            (gControlOn || gPlayerOn) && quickMatch(view) ->
                                if (shouldCollapseControl(view)) View.GONE else View.INVISIBLE
                            gProgressOff && isProgressBar(view) -> View.GONE
                            else -> null
                        }
                        if (replacement != null) {
                            rememberViewState(view, View.VISIBLE)
                            val args = (chain.args as Array<Any?>).copyOf()
                            args[0] = replacement
                            return@ham chain.proceed(args)
                        }
                    }
                    chain.proceed()
                } catch (_: Exception) { chain.proceed() }
            }
            LogUtil.info("  ✓ setVis")
        } catch (e: Exception) { LogUtil.error("setVis", e) }
        try {
            val c = Class.forName("android.view.ViewGroup", false, classLoader)
            // 同样只挂终端重载：addView(View) / (View,int) / (View,LP) / (View,int,int)
            // 最终都会委托到 addView(View, int, LayoutParams)。
            hamExact(
                c, "addView",
                listOf("android.view.View", "int", "android.view.ViewGroup\$LayoutParams"),
                "av",
            ) { chain ->
                val v = try { chain.getArg(0) as? View } catch (_: Throwable) { null }
                val result = chain.proceed()
                try {
                    if (v != null && isInsideModuleUi(v)) return@hamExact result
                    if (!gMasterOn || (gRestoreControlsOnPause && gVideoPaused)) return@hamExact result
                    LogUtil.incr("addView")
                    if (v != null) {
                        scanTreeUnified(v)
                    }
                } catch (_: Exception) {}
                result
            }
            LogUtil.info("  ✓ addView（仅终端重载）")
        } catch (e: Exception) { LogUtil.error("addView", e) }

        try {
            val c = Class.forName("android.widget.TextView", false, classLoader)
            module.hook(c.getDeclaredMethod("setText", CharSequence::class.java, TextView.BufferType::class.java))
                .setId("fullSeriesText")
                .setExceptionMode(XposedInterface.ExceptionMode.PROTECTIVE)
                .intercept(Hooker { chain ->
                    val result = chain.proceed()
                    try { hideFullSeriesEntryBeforeDraw(chain.thisObject as? TextView) } catch (_: Throwable) {}
                    result
                })
            LogUtil.info("  ✓ full-series label pre-draw guard")
        } catch (e: Exception) { LogUtil.error("full-series label guard", e) }

        try {
            val c = Class.forName("com.dragon.read.recyler.AbsRecyclerViewHolder", false, classLoader)
            try {
                module.hook(c.getDeclaredMethod("onBind", Object::class.java, Int::class.java))
                    .setId("cb")
                    .setExceptionMode(XposedInterface.ExceptionMode.PROTECTIVE)
                    .intercept(Hooker { chain ->
                        if (gMasterOn) try {
                            val iv = chain.thisObject.javaClass.getField("itemView").get(chain.thisObject) as? View
                            if (iv != null) scanTreeUnified(iv)
                        } catch (_: Exception) {}
                        chain.proceed()
                    })
            } catch (_: Exception) {}
            LogUtil.info("  ✓ card")
        } catch (e: Exception) { LogUtil.warn("  AbsRecyclerViewHolder 未找到") }

        try { val c = Class.forName("androidx.swiperefreshlayout.widget.SwipeRefreshLayout", false, classLoader); module.hook(c.getDeclaredMethod("onInterceptTouchEvent", android.view.MotionEvent::class.java)).setId("sw").setExceptionMode(XposedInterface.ExceptionMode.PROTECTIVE).intercept(Hooker { chain -> if (gMasterOn && gRefreshOff) false else chain.proceed() }); LogUtil.info("  ✓ swipe") } catch (e: Exception) { LogUtil.warn("  SwipeRefreshLayout 未找到") }

        try { val c = Class.forName(gNames.topZoneTouch, false, classLoader); module.hook(c.getDeclaredMethod("onTouchEvent", android.view.MotionEvent::class.java)).setId("tz1").setExceptionMode(XposedInterface.ExceptionMode.PROTECTIVE).intercept(Hooker { chain -> if (gMasterOn && gTopZoneOn && handleTopZoneEvent(chain.getArg(0) as? android.view.MotionEvent)) true else chain.proceed() }); LogUtil.info("  ✓ ${gNames.topZoneTouch}") } catch (e: Exception) { LogUtil.warn("  ${gNames.topZoneTouch} 未找到") }
        try { val c = Class.forName("android.app.Activity", false, classLoader); module.hook(c.getDeclaredMethod("dispatchTouchEvent", android.view.MotionEvent::class.java)).setId("tz2").setExceptionMode(XposedInterface.ExceptionMode.PROTECTIVE).intercept(Hooker { chain -> try { val ev = chain.getArg(0) as? android.view.MotionEvent; if (ev != null) { when (ev.actionMasked) { android.view.MotionEvent.ACTION_DOWN -> { gTouchDownX = ev.x; gTouchDownY = ev.y } android.view.MotionEvent.ACTION_UP -> { if (kotlin.math.abs(ev.x - gTouchDownX) < 25f && kotlin.math.abs(ev.y - gTouchDownY) < 25f) gLastUserClickAt = android.os.SystemClock.uptimeMillis() } else -> {} } } } catch (_: Throwable) {}; if (gMasterOn && gTopZoneOn && handleTopZoneEvent(chain.getArg(0) as? android.view.MotionEvent)) true else chain.proceed() }); LogUtil.info("  ✓ dispatchTouch") } catch (e: Exception) { LogUtil.error("tz2", e) }

        // 应隐藏状态下 app 将目标UI设为可见时同步改回隐藏，消除切集闪现（与收藏/评论的零闪现机制对齐）
        try {
            val viewClass = Class.forName("android.view.View", false, classLoader)
            module.hook(viewClass.getDeclaredMethod("setVisibility", Int::class.javaPrimitiveType)).setId("sv").setExceptionMode(XposedInterface.ExceptionMode.PROTECTIVE).intercept(Hooker { chain ->
                try {
                    val blocked: Boolean
                    if (chain.getArg(0) as? Int == View.VISIBLE && gInternalViewMutation.get() != true) {
                        val v = chain.thisObject as? View
                        blocked = v != null && v.visibility != View.VISIBLE &&
                            shouldApplyUiHiding() && gMasterOn && (
                                ((gControlOn || gPlayerOn) && quickMatch(v)) ||
                                (gProgressOff && isProgressBar(v))
                            ) &&
                            !(gRestoreControlsOnPause && gVideoPaused && !isEpisodeSwitchPause())
                    } else blocked = false
                    if (blocked) {
                        LogUtil.incr("showBlock")
                        chain.proceed(arrayOf<Any?>(View.INVISIBLE as Any?))
                    } else chain.proceed()
                } catch (_: Throwable) { chain.proceed() }
            })
            LogUtil.info("  ✓ setVisibility guard")
        } catch (e: Exception) { LogUtil.error("sv", e) }

        try {
            val c = Class.forName("android.app.Activity", false, classLoader)
            module.hook(c.getDeclaredMethod("onCreate", android.os.Bundle::class.java)).setId("oc").setExceptionMode(XposedInterface.ExceptionMode.PROTECTIVE).intercept(Hooker { chain ->
                LogUtil.incr("onCreate")
                val a = chain.thisObject as? Activity

                try {
                    if (a != null) {
                        gCurrentActivity = a
                        if (gPrefs == null) initPrefs(a.applicationContext)
                        appCtx = a.applicationContext
                        ensureReceiver(a.applicationContext)
                    }
                } catch (_: Exception) {}
                val result = chain.proceed()
                try {
                    if (a != null) {
                        if (gMasterOn && gStatusOn) applyCleanTop(a)
                        if (gMasterOn && gNavBarOff) applyNavBar(a)
                        if (gNotificationMenuOn) createNotification(a)
                    }
                } catch (_: Exception) {}
                result
            })
            LogUtil.info("  ✓ onCreate")
            module.hook(c.getDeclaredMethod("onResume")).setId("or").setExceptionMode(XposedInterface.ExceptionMode.PROTECTIVE).intercept(Hooker { chain ->
                LogUtil.incr("onResume")
                val a = chain.thisObject as? Activity
                gCurrentActivity = a

                scheduleSettingsEntryInjection(a)

                try {
                    if (a != null) {
                        if (gPrefs == null) initPrefs(a.applicationContext)
                        appCtx = a.applicationContext
                        ensureReceiver(a.applicationContext)
                    }
                } catch (_: Exception) {}
                val result = chain.proceed()
                try {
                    if (a != null) {
                        if (shouldApplyUiHiding()) {
                            setVideoPaused(false, "series-detail-onResume")
                        }
                        if (gMasterOn && gStatusOn) applyCleanTop(a)
                        if (gMasterOn && gNavBarOff) applyNavBar(a)
                        if (gNotificationMenuOn) createNotification(a)
                    }
                } catch (_: Exception) {}
                mainHandler.postDelayed({
                    scanAllWindows()
                    if (a != null) {
                        if (gMasterOn && gStatusOn) applyCleanTop(a)
                        if (gMasterOn && gNavBarOff) applyNavBar(a)
                    }
                }, 400)
                startPeriodicScan()
                LogUtil.diagDump(true)
                result
            })
            LogUtil.info("  ✓ onResume")
            module.hook(c.getDeclaredMethod("onPause")).setId("op").setExceptionMode(XposedInterface.ExceptionMode.PROTECTIVE).intercept(Hooker { chain ->
                val a = chain.thisObject as? Activity
                stopPeriodicScan()
                val result = chain.proceed()
                mainHandler.postDelayed({
                    try {

                        if (a != null && a.hasWindowFocus() && !a.isFinishing) refreshVideoPauseState("activity-onPause")
                    } catch (_: Throwable) {}
                }, 120L)
                result
            })
            LogUtil.info("  ✓ onPause")
        } catch (e: Exception) { LogUtil.error("lifecycle", e) }

        val settingsSpec = nativeSettingsSpec()
        var nativeSettingsListHookCount = 0
        for ((classIndex, settingsClassName) in listOf(
            "com.dragon.read.component.biz.impl.mine.settings.SettingsActivity",
            "com.dragon.read.component.biz.impl.mine.settings.KmpSettingsActivity",
        ).withIndex()) {
            try {
                val settingsClass = Class.forName(settingsClassName, false, classLoader)
                // 列表方法名每次发版都会重排（7.3.7.32 实测：n1/o1 → k1/l1，且 item 类整体换代）。
                // 因此优先按表内名字命中，命中不到就退化为「带参且返回 List 的方法」——
                // 设置页必然有且仅有一两个这样的方法。
                val listMethods = settingsClass.declaredMethods
                    .filter { java.util.List::class.java.isAssignableFrom(it.returnType) && it.parameterCount in 1..2 }
                    .sortedByDescending { it.name in settingsSpec.listMethods }
                if (listMethods.isEmpty()) continue
                val named = listMethods.any { it.name in settingsSpec.listMethods }
                listMethods.forEachIndexed { methodIndex, method ->
                    module.hook(method)
                        .setId("nativeSettingsList_${classIndex}_$methodIndex")
                        .setExceptionMode(XposedInterface.ExceptionMode.PROTECTIVE)
                        .intercept(Hooker { chain ->
                            val result = chain.proceed()
                            val act = chain.thisObject as? Activity
                            if (!injectNativeSettingsList(result, classLoader, settingsSpec)) {
                                injectNativeSettingsItemDynamic(result, act, classLoader)
                            }
                            result
                        })
                    nativeSettingsListHookCount++
                    LogUtil.info("  ✓ native settings list $settingsClassName.${method.name}(${if (named) "表内命中" else "运行时探测"})")
                }
            } catch (_: Throwable) {}
        }

        try {
            val clickClass = Class.forName(settingsSpec.clickClass, false, classLoader)
            clickClass.declaredMethods.filter { it.name == "onClick" }.forEachIndexed { index, method ->
                module.hook(method)
                    .setId("nativeSettingsClick_$index")
                    .setExceptionMode(XposedInterface.ExceptionMode.PROTECTIVE)
                    .intercept(Hooker { chain ->
                        val host = chain.thisObject
                        if (host != null && boundSettingsTitle(host) == "模块设置") {
                            LogUtil.info("点击原生模块设置项 | ${settingsSpec.clickClass}")
                            mainHandler.post { showPanel(gSettingsActivity ?: gCurrentActivity) }
                            null
                        } else chain.proceed()
                    })
            }
            LogUtil.info("  ✓ native settings click ${settingsSpec.clickClass}")
        } catch (e: Throwable) {
            LogUtil.warn("  ${settingsSpec.clickClass} 设置点击 Hook 失败: $e")
        }

        settingsSpec.checkedClass?.let { checkedClassName ->
            try {
                val checkedClass = Class.forName(checkedClassName, false, classLoader)
                checkedClass.declaredMethods.filter { it.name == "onCheckedChanged" }.forEachIndexed { index, method ->
                    module.hook(method)
                        .setId("nativeSettingsChecked_$index")
                        .setExceptionMode(XposedInterface.ExceptionMode.PROTECTIVE)
                        .intercept(Hooker { chain ->
                            val host = chain.thisObject
                            if (host != null && boundSettingsTitle(host) == "模块设置") {
                                val checked = try { chain.getArg(1) as? Boolean } catch (_: Throwable) { null }
                                if (checked != null) {
                                    gMasterOn = checked
                                    savePref("master_on", checked)
                                    if (!checked) {
                                        restoreAllSavedViews()
                                        restoreShortVideoNativeControls()
                                        setVideoToolbarsVisible(true)
                                    }
                                    val notificationCtx = appCtx ?: gCurrentActivity?.applicationContext
                                    if (notificationCtx != null) createNotification(notificationCtx)
                                    applyToCurrent()
                                    LogUtil.info("原生设置项同步模块总开关 -> $checked")
                                }
                            }
                            chain.proceed()
                        })
                }
                LogUtil.info("  ✓ native settings checked $checkedClassName")
            } catch (e: Throwable) {
                LogUtil.warn("  $checkedClassName 设置开关 Hook 失败: $e")
            }
        }

        if (nativeSettingsListHookCount == 0) {
            LogUtil.warn("  原生设置列表 Hook 未找到 | profile=${gNames.profileId} | methods=${settingsSpec.listMethods}")
        }

        var settingsHookCount = 0
        for ((index, settingsClassName) in listOf(
            "com.dragon.read.component.biz.impl.mine.settings.SettingsActivity",
            "com.dragon.read.component.biz.impl.mine.settings.KmpSettingsActivity",
        ).withIndex()) {
            try {
                val c = Class.forName(settingsClassName, false, classLoader)
                module.hook(c.getDeclaredMethod("onCreate", android.os.Bundle::class.java))
                    .setId("st$index")
                    .setExceptionMode(XposedInterface.ExceptionMode.PROTECTIVE)
                    .intercept(Hooker { chain ->
                        val a = chain.thisObject as? Activity
                        gSettingsActivity = a
                        val r = chain.proceed()
                        if (a != null) scheduleSettingsEntryInjection(a)
                        r
                    })
                settingsHookCount++
                LogUtil.info("  ✓ settings $settingsClassName")
            } catch (_: Throwable) {}
        }
        if (settingsHookCount == 0) LogUtil.warn("  设置页 Activity 未找到")

        try {
            val hClass = Class.forName(gNames.oledBright, false, classLoader)
            val brightActionClass = Class.forName(gNames.oledBrightAction, false, classLoader)
            module.hook(hClass.getDeclaredMethod("a", java.util.List::class.java)).setId("oled").setExceptionMode(XposedInterface.ExceptionMode.PROTECTIVE).intercept(Hooker { chain ->
                if (!gMasterOn || !gOledProtectOn) return@Hooker chain.proceed()
                try {
                    val list = chain.getArg(0) as? java.util.List<*>
                    if (list != null && list.isNotEmpty()) {
                        val filtered = list.filter { it == null || !brightActionClass.isInstance(it) }
                        if (filtered.size < list.size) {
                            LogUtil.incr("oledBrightBlock")
                            val na = (chain.args as Array<Any?>).copyOf()
                            na[0] = java.util.ArrayList(filtered)
                            return@Hooker chain.proceed(na)
                        }
                    }
                } catch (_: Exception) {}
                chain.proceed()
            })
            LogUtil.info("  ✓ OLED 亮度拦截（默认关闭）")
        } catch (e: Exception) { LogUtil.warn("  ${gNames.oledBright} OLED 未找到: $e") }

        try { val c = Class.forName(gNames.pauseAdEntryClass, false, classLoader)
            ham(c, gNames.pauseAdEntryMethod, "pauseAdEntry") { chain -> if (gMasterOn && gAdOn) null else chain.proceed() }
            LogUtil.info("  ✓ 暂停广告入口 ${gNames.pauseAdEntryClass}.${gNames.pauseAdEntryMethod}")
        } catch (e: Exception) {
            LogUtil.warn("  暂停广告入口 ${gNames.pauseAdEntryClass}.${gNames.pauseAdEntryMethod} 未找到: $e")
        }

        try { val c = Class.forName("com.dragon.read.ad.onestop.seriespause.impl.SeriesPauseAdImpl", false, classLoader)
            ham(c, "canShowPauseAd", "spCan") { chain -> if (gMasterOn && gAdOn) false else chain.proceed() }
            ham(c, "enablePauseAd", "spEnable") { chain -> if (gMasterOn && gAdOn) false else chain.proceed() }
            ham(c, "requestAd", "spReq") { chain -> if (gMasterOn && gAdOn) null else chain.proceed() }
            ham(c, "onPauseAdShow", "spShow") { chain -> if (gMasterOn && gAdOn) null else chain.proceed() }
            ham(c, "enableCoinBox", "spCoin") { chain -> if (gMasterOn && gAdOn) false else chain.proceed() }
            LogUtil.info("  ✓ SeriesPauseAdImpl 拦截") } catch (e: Exception) { LogUtil.warn("  SeriesPauseAdImpl 未找到: $e") }

        try { val c = Class.forName("com.dragon.read.pages.video.layers.advideoendlayer.AdVideoEndLayer", false, classLoader)
            ham(c, "handleVideoEvent", "veAd") { chain -> if (gMasterOn && gAdOn) { try { val ev = chain.getArg(0); val m = ev?.javaClass?.getMethod("getType"); if (m != null && (m.invoke(ev) as? Int) == 102) return@ham false } catch (_: Exception) {} }; chain.proceed() }
            ham(c, gNames.adVideoEndShowMethod, "veAdI") { chain -> if (gMasterOn && gAdOn) null else chain.proceed() }
            LogUtil.info("  ✓ 片尾广告层") } catch (e: Exception) { LogUtil.warn("  AdVideoEndLayer 未找到: $e") }

        try { val c = Class.forName("com.dragon.read.pages.video.layers.adiconlayer.AdIconLayer", false, classLoader)
            ham(c, "handleVideoEvent", "aiAd") { chain -> if (gMasterOn && gAdOn) false else chain.proceed() }
            LogUtil.info("  ✓ 广告图标层") } catch (e: Exception) { LogUtil.warn("  AdIconLayer 未找到: $e") }

        try { val c = Class.forName("com.dragon.read.component.biz.impl.privilege.PrivilegeManager", false, classLoader)
            ham(c, "isVip", "vipIsVip") { chain -> if (gMasterOn && gVipOn) true else chain.proceed() }
            ham(c, "isAnyVip", "vipAny") { chain -> if (gMasterOn && gVipOn) true else chain.proceed() }
            ham(c, "canReadShortStory", "vipRead") { chain -> if (gMasterOn && gVipOn) true else chain.proceed() }
            ham(c, "hasVipShortSeriesPrivilege", "vipSeries") { chain -> if (gMasterOn && gVipOn) true else chain.proceed() }
            ham(c, "b", "vipSubType") { chain -> if (gMasterOn && gVipOn) true else chain.proceed() }
            ham(c, "hasNoAdFollAllScene", "vipNoAd") { chain -> if (gMasterOn && gVipOn) true else chain.proceed() }
            ham(c, "hasNoAdForShortSeries", "vipNoAdS") { chain -> if (gMasterOn && gVipOn) true else chain.proceed() }

            try {
                val vim = Class.forName("com.dragon.read.user.model.VipInfoModel", false, classLoader)
                val vct = Class.forName("com.dragon.read.rpc.model.VipCommonSubType", false, classLoader)
                val vc = vim.getDeclaredConstructor(String::class.java, String::class.java, String::class.java, Boolean::class.javaPrimitiveType, Boolean::class.javaPrimitiveType, Int::class.javaPrimitiveType, Boolean::class.javaPrimitiveType, vct)
                vc.isAccessible = true
                val de = vct.getEnumConstants()[0]
                val fakeModel = { vc.newInstance("2099-12-31 23:59:59", "1", "99999999", true, false, 0, true, de) }
                c.declaredMethods.filter { it.name == "getVipInfo" }.forEachIndexed { i, m -> try { module.hook(m).setId("vipGetInfo_$i").setExceptionMode(XposedInterface.ExceptionMode.PROTECTIVE).intercept(Hooker { chain -> if (gMasterOn && gVipOn) fakeModel() else chain.proceed() }) } catch (_: Exception) {} }
                module.hook(c.getDeclaredMethod("getAllVipInfo")).setId("vipGetAll").setExceptionMode(XposedInterface.ExceptionMode.PROTECTIVE).intercept(Hooker { chain -> if (gMasterOn && gVipOn) java.util.Collections.singletonList(fakeModel()) else chain.proceed() })
                LogUtil.info("  ✓ getVipInfo 模型")
            } catch (e2: Exception) { LogUtil.warn("  getVipInfo 模型 hook 失败: $e2") }
            LogUtil.info("  ✓ VIP 解锁 (PrivilegeManager)") } catch (e: Exception) { LogUtil.warn("  PrivilegeManager 未找到: $e") }
        try { val c = Class.forName("com.dragon.read.component.NsUserInfoDependImpl", false, classLoader)
            ham(c, "isVip", "nsVip") { chain -> if (gMasterOn && gVipOn) true else chain.proceed() }
            try {
                val vim = Class.forName("com.dragon.read.user.model.VipInfoModel", false, classLoader)
                val vct = Class.forName("com.dragon.read.rpc.model.VipCommonSubType", false, classLoader)
                val vc = vim.getDeclaredConstructor(String::class.java, String::class.java, String::class.java, Boolean::class.javaPrimitiveType, Boolean::class.javaPrimitiveType, Int::class.javaPrimitiveType, Boolean::class.javaPrimitiveType, vct)
                vc.isAccessible = true
                val de = vct.getEnumConstants()[0]
                module.hook(c.getDeclaredMethod("getVipInfoModel")).setId("vipGetModel").setExceptionMode(XposedInterface.ExceptionMode.PROTECTIVE).intercept(Hooker { chain -> if (gMasterOn && gVipOn) vc.newInstance("2099-12-31 23:59:59", "1", "99999999", true, false, 0, true, de) else chain.proceed() })
                LogUtil.info("  ✓ getVipInfoModel")
            } catch (e2: Exception) { LogUtil.warn("  getVipInfoModel hook 失败: $e2") }
            LogUtil.info("  ✓ NsUserInfoDependImpl.isVip") } catch (e: Exception) { LogUtil.warn("  NsUserInfoDependImpl 未找到: $e") }
        try { val c = Class.forName("com.dragon.read.component.NsComicAdDependImpl", false, classLoader)
            ham(c, "isVipUser", "comicVip") { chain -> if (gMasterOn && gVipOn) true else chain.proceed() }
            LogUtil.info("  ✓ NsComicAdDependImpl.isVipUser") } catch (e: Exception) { LogUtil.warn("  NsComicAdDependImpl 未找到: $e") }

        try {
            val eCls = Class.forName(gNames.kmpVipModel, false, classLoader)
            val eCtor = eCls.getDeclaredConstructor(String::class.java, String::class.java, String::class.java, java.lang.Boolean::class.java, java.lang.Boolean::class.java, Integer::class.java, java.lang.Boolean::class.java, Integer::class.java, java.lang.Boolean::class.java, Integer::class.java, java.lang.Boolean::class.java)
            eCtor.isAccessible = true
            var kmpHooked = 0
            for (svc in gNames.kmpAcctService) {
                try {
                    val c = Class.forName(svc, false, classLoader)
                    module.hook(c.getDeclaredMethod("getVipInfo")).setId("kmpVipInfo_$svc").setExceptionMode(XposedInterface.ExceptionMode.PROTECTIVE).intercept(Hooker { chain ->
                        if (gMasterOn && gVipOn) { try { eCtor.newInstance("1", "2099-12-31 23:59:59", "99999999", true, true, 0, true, 0, true, 0, true) } catch (_: Exception) { chain.proceed() } } else chain.proceed()
                    })
                    kmpHooked++
                } catch (_: Exception) {}
            }
            LogUtil.info("  ✓ KMP getVipInfo hooked=$kmpHooked ($gNames.kmpVipModel)") } catch (e: Exception) { LogUtil.warn("  KMP vip 模型 ${gNames.kmpVipModel} 未找到: $e") }
        try { val c = Class.forName("com.dragon.read.component.biz.impl.NsVipImpl", false, classLoader)
            ham(c, "isVip", "nvVip") { chain -> if (gMasterOn && gVipOn) true else chain.proceed() }
            ham(c, "isSpecificVipOrHigher", "nvSpec") { chain -> if (gMasterOn && gVipOn) true else chain.proceed() }
            ham(c, "canShowVipCenter", "nvCenter") { chain -> if (gMasterOn && gVipOn) true else chain.proceed() }
            LogUtil.info("  ✓ NsVipImpl") } catch (e: Exception) { LogUtil.warn("  NsVipImpl 未找到: $e") }

        LogUtil.info("installBusinessHooks done"); LogUtil.diagDump(true)
        startSideControlsWatchIfNeeded()
    }

    fun installDemoHooks(module: MainHook, classLoader: ClassLoader) {
        LogUtil.info("demo")
        try { val c = Class.forName("xyz.kejiyu.hongguo.MainActivity", false, classLoader); module.hook(c.getMethod("onCreate", android.os.Bundle::class.java)).setPriority(XposedInterface.PRIORITY_DEFAULT).setExceptionMode(XposedInterface.ExceptionMode.PROTECTIVE).setId("dm").intercept(Hooker { chain -> chain.proceed() }); LogUtil.info("demo ok") } catch (e: Exception) { LogUtil.error("demo", e) }
    }
}

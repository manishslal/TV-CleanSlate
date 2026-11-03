package app.orchard.tv.system

import android.app.usage.UsageStatsManager
import android.content.Context
import android.content.Intent
import android.content.pm.ApplicationInfo
import android.graphics.Bitmap
import android.graphics.drawable.Drawable
import app.orchard.tv.apps.LogoPack
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.setValue
import androidx.core.content.edit
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext

/**
 * An installed VPN app: [label] for the button, [mark] its logo from the logo pack (white on clear,
 * so it can be tinted like a glyph), else [icon], its launcher icon; [launch] opens it.
 */
class VpnApp(val packageName: String, val label: String, val icon: Drawable, val mark: Bitmap?, val launch: Intent)

/**
 * The VPN app for Control Center's button. An app is a VPN when it declares an Android VpnService,
 * so no list of brands is needed. With several installed, the one used most recently wins: the
 * system doesn't say which of them owns the tunnel that is up, and the app opened last is almost
 * always the one that connected it.
 */
object VpnApps {
    /**
     * The VPN app, looked up ahead of time so Control Center shows its button with the others: a
     * lookup when the panel opened (30 days of usage stats, the app's icon, its logo) made the
     * button appear a second or two after the rest on a SHIELD.
     */
    var app by mutableStateOf<VpnApp?>(null); private set

    /**
     * Whether a VPN has ever been up on this device while Home was running. Until then there is no
     * VPN button: having a VPN app installed isn't a reason to show one.
     */
    var seen by mutableStateOf(false); private set

    private const val PREFS = "aerial"
    private const val KEY_SEEN = "vpn_seen"

    /** Remembers that a VPN was up, once. */
    fun sawVpn(context: Context) {
        if (seen) return
        seen = true
        context.getSharedPreferences(PREFS, Context.MODE_PRIVATE).edit { putBoolean(KEY_SEEN, true) }
    }

    /** Looks the app up again, off the main thread; the last one stays shown meanwhile. */
    suspend fun refresh(context: Context) {
        if (!seen) seen = context.getSharedPreferences(PREFS, Context.MODE_PRIVATE).getBoolean(KEY_SEEN, false)
        val found = withContext(Dispatchers.IO) { current(context) }
        if (found?.packageName != app?.packageName || (found == null) != (app == null)) app = found
    }

    /** Off the main thread: it reads the logo pack. */
    private fun current(context: Context): VpnApp? = runCatching {
        val pm = context.packageManager
        val found = pm.queryIntentServices(Intent("android.net.VpnService"), 0)
            .mapNotNull { it.serviceInfo?.applicationInfo }
            .filter { it.packageName != context.packageName && it.flags and ApplicationInfo.FLAG_SYSTEM == 0 }
            .distinctBy { it.packageName }
            .mapNotNull { info -> (pm.getLeanbackLaunchIntentForPackage(info.packageName) ?: pm.getLaunchIntentForPackage(info.packageName))?.let { info to it } }
        val recent = lastUsed(context)
        val (info, launch) = found.maxByOrNull { recent[it.first.packageName] ?: 0L } ?: return null
        VpnApp(info.packageName, info.loadLabel(pm).toString(), info.loadIcon(pm), LogoPack.load(info.packageName)?.bitmap, launch)
    }.getOrNull()

    private fun lastUsed(context: Context): Map<String, Long> = runCatching {
        val now = System.currentTimeMillis()
        val stats = context.getSystemService(Context.USAGE_STATS_SERVICE) as UsageStatsManager
        stats.queryUsageStats(UsageStatsManager.INTERVAL_DAILY, now - 30L * 24 * 3600 * 1000, now)
            .groupBy { it.packageName }.mapValues { (_, days) -> days.maxOf { it.lastTimeUsed } }
    }.getOrDefault(emptyMap())
}

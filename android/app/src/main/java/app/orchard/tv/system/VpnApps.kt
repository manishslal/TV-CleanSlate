package app.orchard.tv.system

import android.app.usage.UsageStatsManager
import android.content.Context
import android.content.Intent
import android.content.pm.ApplicationInfo
import android.graphics.Bitmap
import android.graphics.drawable.Drawable
import app.orchard.tv.apps.LogoPack

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
    /** Off the main thread: it reads the logo pack. */
    fun current(context: Context): VpnApp? = runCatching {
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

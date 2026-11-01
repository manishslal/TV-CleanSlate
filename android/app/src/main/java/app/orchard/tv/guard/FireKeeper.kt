package app.orchard.tv.guard

import android.app.ActivityManager
import android.app.AlarmManager
import android.app.AppOpsManager
import android.app.Notification
import android.app.PendingIntent
import android.app.Service
import android.app.usage.UsageEvents
import android.app.usage.UsageStatsManager
import android.content.Context
import android.content.Intent
import android.os.Build
import android.os.Handler
import android.os.HandlerThread
import android.os.IBinder
import android.os.Process
import android.os.SystemClock
import android.provider.Settings
import android.util.Log
import androidx.core.content.getSystemService
import app.orchard.tv.MainActivity
import app.orchard.tv.R
import kotlin.concurrent.thread

/**
 * Fork: Fire OS 6.7 never binds a third-party accessibility service, so Home Guard cannot run there
 * (`services:{}` on Fire TV Stick 4K AFTMM, 6.7.1.1). This watches usage stats for Amazon's home
 * activity and moves Home's task to the front. Moving a task is not an activity start, so the 5 s
 * delay after the Home key does not apply. Approach from dumbtv's Keeper (MIT, see CREDITS.md).
 */
class FireKeeper : Service() {
    private lateinit var thread: HandlerThread
    private lateinit var worker: Handler
    private var usage: UsageStatsManager? = null
    private var top: String? = null
    private var topClass: String? = null
    private var seenUpTo = 0L
    @Volatile private var log: java.lang.Process? = null
    private var logStartedAt = -LOG_RETRY_MS
    @Volatile private var homeKeyAt = 0L
    private var movedAt = 0L

    private val poll = object : Runnable {
        override fun run() {
            // One bad query must not end the watch.
            runCatching { check() }.onFailure { Log.w(TAG, "check failed", it) }
            worker.postDelayed(this, POLL_MS)
        }
    }

    override fun onCreate() {
        super.onCreate()
        isRunning = true
        usage = getSystemService()
        @Suppress("DEPRECATION")
        startForeground(
            NOTIFICATION,
            Notification.Builder(this).setSmallIcon(R.mipmap.ic_launcher).setContentTitle(getString(R.string.guard_name)).build(),
        )
        // Off the main thread: this process draws Home.
        thread = HandlerThread("FireKeeper").apply { start() }
        worker = Handler(thread.looper)
        worker.post(poll)
        // At boot Amazon's home came up before this service, so no event will say so.
        worker.postDelayed({ if (noHomeTask()) bringHomeBack(System.currentTimeMillis()) }, FIRST_START_MS)
    }

    override fun onDestroy() {
        isRunning = false
        watchLog(false)
        thread.quitSafely()
        super.onDestroy()
    }

    override fun onStartCommand(intent: Intent?, flags: Int, startId: Int): Int {
        // Fire OS runs whichever dream is set here, and an update can put Amazon's back.
        if (GuardSetup.isUserUnlocked(this)) GuardSetup.setOwnScreensaver(this, true)
        return START_STICKY
    }

    override fun onBind(intent: Intent?): IBinder? = null

    private fun check() {
        val now = System.currentTimeMillis()
        readEvents(now)
        val front = top ?: return
        watchLog(paused || front == AMAZON_SETTINGS_PACKAGE)
        if (front != AMAZON_HOME_PACKAGE) {
            // Not until Amazon has had time to come up after pause().
            if (front != "com.android.systemui" && front != "android" && SystemClock.uptimeMillis() - pausedAt > PAUSE_GRACE_MS) paused = false
            return
        }
        // Only the home activity: Amazon's settings pages live in the same package.
        if (topClass != AMAZON_HOME || paused || now - movedAt < DEBOUNCE_MS) return
        movedAt = now
        bringHomeBack(now)
    }

    private fun readEvents(now: Long) {
        val events = usage?.queryEvents(now - LOOK_BACK_MS, now) ?: return
        val event = UsageEvents.Event()
        while (events.hasNextEvent()) {
            events.getNextEvent(event)
            if (event.eventType != UsageEvents.Event.MOVE_TO_FOREGROUND) continue
            // Settings is a tab of Amazon's home here: its entry in Amazon's settings app starts the
            // home activity. Going Back out of a settings page does too, and a Home press ends it.
            if (top == AMAZON_SETTINGS_PACKAGE && topClass != BOOT_FALLBACK && event.className == AMAZON_HOME &&
                event.timeStamp > seenUpTo &&
                event.timeStamp - homeKeyAt > HOME_KEY_MS
            ) paused = true
            seenUpTo = maxOf(seenUpTo, event.timeStamp)
            top = event.packageName
            topClass = event.className
        }
    }

    /**
     * Fire OS keeps "homekey" for Amazon-signed apps, and Home with Amazon's home already in front
     * raises no usage event, so the press is read from the log. Only while a press would otherwise
     * be missed. Needs READ_LOGS, which only ADB can give (tools/install.sh).
     */
    private fun watchLog(on: Boolean) {
        if (on == (log != null)) return
        log?.destroy()
        log = null
        val now = SystemClock.uptimeMillis()
        if (!on || now - logStartedAt < LOG_RETRY_MS) return
        logStartedAt = now
        val process = runCatching {
            ProcessBuilder("logcat", "-T", "1", "ActivityManager:I", "*:S").redirectErrorStream(true).start()
        }.getOrNull() ?: return
        log = process
        thread(name = "FireKeeperLog", isDaemon = true) {
            runCatching {
                process.inputStream.bufferedReader().forEachLine {
                    if (HOME_KEY_START in it && "com.amazon.tv.launcher/.ui.HomeActivity_vNext" in it) {
                        val at = System.currentTimeMillis()
                        homeKeyAt = at
                        paused = false
                        // Not left to the next check: what usage stats last said may be out of date.
                        worker.post { movedAt = at; bringHomeBack(at) }
                    }
                }
            }
            if (log === process) log = null
        }
    }

    private fun noHomeTask() = runCatching { getSystemService<ActivityManager>()?.appTasks.isNullOrEmpty() }.getOrDefault(false)

    private fun bringHomeBack(now: Long) {
        val moved = runCatching {
            // An empty task is one Home's own reset is tearing down; moving it restarts a dead activity.
            getSystemService<ActivityManager>()?.appTasks?.firstOrNull {
                it.taskInfo.topActivity != null || it.taskInfo.baseActivity != null
            }?.moveToFront()
        }.onFailure { Log.w(TAG, "moveToFront failed", it) }.getOrNull() != null
        if (moved) return
        // No task yet, right after a boot: an alarm's start is exempt from the delay.
        val intent = PendingIntent.getActivity(
            this, 0, MainActivity.bringBackIntent(this),
            PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE,
        )
        getSystemService<AlarmManager>()?.setExact(AlarmManager.ELAPSED_REALTIME, SystemClock.elapsedRealtime(), intent)
    }

    companion object {
        private const val TAG = "FireKeeper"
        private const val NOTIFICATION = 7
        private const val AMAZON_HOME_PACKAGE = "com.amazon.tv.launcher"
        private const val AMAZON_HOME = "com.amazon.tv.launcher.ui.HomeActivity_vNext"
        private const val AMAZON_SETTINGS_PACKAGE = "com.amazon.tv.settings.v2"
        private const val BOOT_FALLBACK = "com.amazon.tv.settings.v2.system.FallbackHome"
        private const val AMAZON_HOME_STARTER = "com.amazon.firehomestarter"
        private const val PAUSE_GRACE_MS = 5_000L
        private const val HOME_KEY_MS = 2_000L
        private const val LOG_RETRY_MS = 2_000L
        private const val HOME_KEY_START = "cat=[android.intent.category.HOME] flg=0x10200000"
        private const val CHECKED_BUILD = "home_fire_build"
        private const val POLL_MS = 250L
        private const val DEBOUNCE_MS = 1_500L
        private const val LOOK_BACK_MS = 4_000L
        private const val FIRST_START_MS = 500L

        @Volatile var isRunning = false
            private set
        @Volatile private var paused = false
        @Volatile private var pausedAt = Long.MIN_VALUE / 2

        /** Fire OS before Android 8. Fire OS 7 and 8 bind Home Guard. */
        val applies: Boolean get() = Build.MANUFACTURER == "Amazon" && Build.VERSION.SDK_INT < Build.VERSION_CODES.O

        /** Fire OS has changed since tools/fire-reapply.sh last ran, so the debloat may be undone. */
        fun firmwareChanged(context: Context): Boolean {
            if (!applies) return false
            val checked = Settings.Global.getString(context.contentResolver, CHECKED_BUILD) ?: return false
            return checked != Build.FINGERPRINT
        }

        /** Amazon's home stays in front, from Home's tile, until another app opens. */
        fun pause() {
            pausedAt = SystemClock.uptimeMillis()
            paused = true
        }

        fun isAmazonHome(packageName: String) = packageName == AMAZON_HOME_PACKAGE || packageName == AMAZON_HOME_STARTER

        fun amazonHomeIntent(): Intent = Intent(Intent.ACTION_MAIN)
            .setClassName(AMAZON_HOME_PACKAGE, AMAZON_HOME)
            .addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)

        /** Amazon's launcher answers ACTION_SETTINGS, so its tile is the gear. */
        fun isAmazonSettingsTile(packageName: String) = packageName == AMAZON_HOME_PACKAGE

        /** Needs usage stats, which only ADB can give (tools/install.sh). */
        fun start(context: Context) {
            if (!applies) return
            val ops = context.getSystemService<AppOpsManager>() ?: return
            val mode = ops.checkOpNoThrow(AppOpsManager.OPSTR_GET_USAGE_STATS, Process.myUid(), context.packageName)
            if (mode != AppOpsManager.MODE_ALLOWED) return
            runCatching { context.startService(Intent(context, FireKeeper::class.java)) }
                .onFailure { Log.w(TAG, "could not start", it) }
        }
    }
}

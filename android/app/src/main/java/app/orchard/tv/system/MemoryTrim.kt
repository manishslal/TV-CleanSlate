package app.orchard.tv.system

import android.content.ComponentCallbacks2
import android.util.Log
import app.orchard.tv.wallpaper.Wallpapers

/**
 * Fork: gives back what the screensaver and the panels leave behind once they close. Their photos,
 * players and glass are out of the composition by then, but a picture's memory goes only at the
 * next garbage collection, and the GPU keeps its textures cached until told: measured on the onn.
 * 4K Plus (2 GB), Home was 171 MB fresh and 284 MB after a screensaver and a few panels.
 *
 * The GPU's cache is asked to let go of what nothing draws (RUNNING_CRITICAL), not torn down as
 * when Home is hidden: the next frame would build it all again, a hitch on the way back. That
 * call is hidden on Android 14 (the onn. 4K Plus refuses it, NoSuchMethodException), where the
 * renderer keeps about 40 MB of reusable layers within its own budget until the system trims Home;
 * the pictures and the garbage collection still give back their share there.
 */
object MemoryTrim {
    private const val TAG = "AerialMemory"

    fun release(why: String, level: Int = LEVEL) {
        Wallpapers.trimMemory()
        Runtime.getRuntime().gc()
        // What the system itself calls on a trim reaches the renderer's caches, which no public call
        // does: HardwareRenderer.trimMemory on Android 14, WindowManagerGlobal.trimMemory before.
        // Logged, and nothing more, where it is refused.
        val gpu = runCatching {
            Class.forName("android.graphics.HardwareRenderer").getMethod("trimMemory", Int::class.javaPrimitiveType).invoke(null, level)
        }.recoverCatching {
            val global = Class.forName("android.view.WindowManagerGlobal")
            global.getMethod("trimMemory", Int::class.javaPrimitiveType).invoke(global.getMethod("getInstance").invoke(null), level)
        }
        Log.i(TAG, "released after $why (level $level)" + (gpu.exceptionOrNull()?.let { "; GPU cache not reached: ${it.javaClass.simpleName}" } ?: ""))
    }

    /** RUNNING_CRITICAL: the renderer drops cached textures and layers nothing draws, and keeps the rest. */
    @Suppress("DEPRECATION")
    private const val LEVEL = ComponentCallbacks2.TRIM_MEMORY_RUNNING_CRITICAL
}

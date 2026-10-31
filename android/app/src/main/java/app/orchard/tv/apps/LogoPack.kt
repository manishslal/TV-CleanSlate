package app.orchard.tv.apps

import android.content.Context
import android.graphics.Bitmap
import android.graphics.BitmapFactory
import android.util.Log
import org.json.JSONObject

/**
 * Fork: the owner's logo pack for Dark Glass tiles, by package. It is copied into the build from
 * logos/ when that folder is there (app/build.gradle.kts) and never kept in this repo:
 * the logos are trademarks, for personal use. A build without it has no such asset, and every
 * tile is drawn from the app's own icon as before.
 */
object LogoPack {
    private const val DIR = "logopack"
    private const val TAG = "LogoPack"

    /** Decoded no larger than this: 2 GB TVs draw a tile at 352×198, and masters run to a few thousand pixels. */
    private const val MAX_SIDE = 1024

    private lateinit var context: Context
    @Volatile private var files: Map<String, String>? = null

    /** Called before tiles are drawn. */
    fun use(context: Context) { this.context = context.applicationContext }

    private fun files(): Map<String, String> = files ?: runCatching {
        val json = JSONObject(context.assets.open("$DIR/manifest.json").bufferedReader().use { it.readText() })
        buildMap {
            json.keys().forEach { brand ->
                val entry = json.getJSONObject(brand)
                val ids = entry.optJSONArray("android") ?: return@forEach
                val file = entry.getString("file")
                // Android draws no SVG from assets: that brand keeps its processed icon.
                if (file.endsWith(".svg")) return@forEach
                for (i in 0 until ids.length()) put(ids.getString(i), file)
            }
        }
    }.getOrElse { emptyMap() }.also { files = it }

    /** The logo for [pkg], or null: not in the pack, or no pack in this build. Off the main thread. */
    fun load(pkg: String): Bitmap? {
        val file = files()[pkg] ?: return null
        return runCatching {
            val path = "$DIR/$file"
            val size = BitmapFactory.Options().apply { inJustDecodeBounds = true }
            context.assets.open(path).use { BitmapFactory.decodeStream(it, null, size) }
            var sample = 1
            while (maxOf(size.outWidth, size.outHeight) / sample > MAX_SIDE) sample *= 2
            context.assets.open(path).use { BitmapFactory.decodeStream(it, null, BitmapFactory.Options().apply { inSampleSize = sample }) }
        }.onFailure { Log.w(TAG, "could not read the logo of $pkg", it) }.getOrNull()
    }
}

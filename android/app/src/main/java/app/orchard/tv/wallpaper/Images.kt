package app.orchard.tv.wallpaper

import android.graphics.Bitmap
import android.graphics.BitmapFactory
import android.graphics.Canvas
import android.graphics.LinearGradient
import android.graphics.Matrix
import android.graphics.Paint
import android.graphics.RadialGradient
import android.graphics.Rect
import android.graphics.Shader
import android.media.ExifInterface
import androidx.core.graphics.createBitmap
import java.io.File
import kotlin.math.abs
import kotlin.math.max
import kotlin.math.roundToInt
import kotlin.random.Random

/** Bitmap helpers shared by the wallpaper code. All of it runs off the main thread. */
object Images {

    /** Decodes [file] no larger than needed for [w]×[h], upright, then centre-crops it to exactly that size. */
    fun decodeCover(file: File, w: Int, h: Int): Bitmap? {
        val bounds = BitmapFactory.Options().apply { inJustDecodeBounds = true }
        BitmapFactory.decodeFile(file.path, bounds)
        if (bounds.outWidth <= 0 || bounds.outHeight <= 0) return null
        val orientation = exifOrientation(file)
        val turned = quarterTurn(orientation)
        val bw = if (turned) bounds.outHeight else bounds.outWidth
        val bh = if (turned) bounds.outWidth else bounds.outHeight
        var sample = 1
        while (bw / (sample * 2) >= w && bh / (sample * 2) >= h) sample *= 2
        val decoded = BitmapFactory.decodeFile(file.path, BitmapFactory.Options().apply { inSampleSize = sample }) ?: return null
        val src = upright(decoded, orientation)
        return cover(src, w, h).also { if (it !== src) src.recycle() }
    }

    /**
     * How the camera stored [file]'s picture (EXIF Orientation). Phones keep the sensor's pixels as
     * they were and write the turn into the file, so a photo taken upright decoded on its side or
     * upside down (issue #647). Normal for anything without the tag (PNG, Bing's pictures).
     */
    fun exifOrientation(file: File): Int = runCatching {
        ExifInterface(file.path).getAttributeInt(ExifInterface.TAG_ORIENTATION, ExifInterface.ORIENTATION_NORMAL)
    }.getOrDefault(ExifInterface.ORIENTATION_NORMAL)

    /** Whether [orientation] turns the picture a quarter, so its width and height swap. */
    fun quarterTurn(orientation: Int): Boolean = when (orientation) {
        ExifInterface.ORIENTATION_TRANSPOSE, ExifInterface.ORIENTATION_ROTATE_90,
        ExifInterface.ORIENTATION_TRANSVERSE, ExifInterface.ORIENTATION_ROTATE_270 -> true
        else -> false
    }

    /** [src] turned and mirrored upright for [orientation]; [src] itself when it already is, else recycled. */
    fun upright(src: Bitmap, orientation: Int): Bitmap {
        val m = Matrix()
        when (orientation) {
            ExifInterface.ORIENTATION_FLIP_HORIZONTAL -> m.setScale(-1f, 1f)
            ExifInterface.ORIENTATION_ROTATE_180 -> m.setRotate(180f)
            ExifInterface.ORIENTATION_FLIP_VERTICAL -> m.setScale(1f, -1f)
            ExifInterface.ORIENTATION_TRANSPOSE -> { m.setRotate(90f); m.postScale(-1f, 1f) }
            ExifInterface.ORIENTATION_ROTATE_90 -> m.setRotate(90f)
            ExifInterface.ORIENTATION_TRANSVERSE -> { m.setRotate(-90f); m.postScale(-1f, 1f) }
            ExifInterface.ORIENTATION_ROTATE_270 -> m.setRotate(-90f)
            else -> return src
        }
        return Bitmap.createBitmap(src, 0, 0, src.width, src.height, m, true).also { if (it !== src) src.recycle() }
    }

    fun cover(src: Bitmap, w: Int, h: Int): Bitmap {
        if (src.width == w && src.height == h) return src
        val scale = max(w / src.width.toFloat(), h / src.height.toFloat())
        val cw = (w / scale).roundToInt().coerceAtMost(src.width)
        val ch = (h / scale).roundToInt().coerceAtMost(src.height)
        val l = (src.width - cw) / 2
        val t = (src.height - ch) / 2
        val out = createBitmap(w, h)
        Canvas(out).drawBitmap(src, Rect(l, t, l + cw, t + ch), Rect(0, 0, w, h), Paint(Paint.FILTER_BITMAP_FLAG))
        return out
    }

    /**
     * Shading baked into a home backdrop so the clock and the top row read over any picture, the way
     * tvOS darkens the area under its app rows. Baked once, so the TV draws a single full-screen layer.
     * Light since 1.28: the grid has its own darkened frosted background and the top row its plate,
     * and the old shade (88% black at the bottom, 42% more on the left) made photos look dim (#157).
     */
    /** The home shading, top to bottom: black at these alphas at [SHADE_AT]; and [clockShade] behind the clock. */
    val SHADE = floatArrayOf(0.12f, 0.04f, 0.22f, 0.5f)
    val SHADE_AT = floatArrayOf(0f, 0.3f, 0.66f, 1f)
    const val SHADE_LEFT = 0.2f

    /**
     * Fork: where the clock goes on [b] and whether the picture is bright there. With [chosen] Auto,
     * the calmest of the three places along the top (least edge and contrast, and not bright, which
     * would need a deep shade), with a small lean to the left, where a reader starts. Read from a
     * 64×36 copy, so it costs nothing beside the baking.
     */
    fun clockSpot(b: Bitmap, chosen: app.orchard.tv.settings.ClockPosition): ClockSpot {
        val w = 64
        val h = 36
        val small = Bitmap.createScaledBitmap(b, w, h, true)
        val luma = FloatArray(w * h)
        for (y in 0 until h) for (x in 0 until w) {
            val c = small.getPixel(x, y)
            luma[y * w + x] = 0.2126f * android.graphics.Color.red(c) + 0.7152f * android.graphics.Color.green(c) + 0.0722f * android.graphics.Color.blue(c)
        }
        if (small !== b) small.recycle()
        // The band the clock takes, top 8% to 45%, and the three places along it.
        val y0 = 3
        val y1 = 16
        fun region(pos: app.orchard.tv.settings.ClockPosition): IntRange = when (pos) {
            app.orchard.tv.settings.ClockPosition.Middle -> 18 until 46
            app.orchard.tv.settings.ClockPosition.Right -> 37 until 64
            else -> 0 until 27
        }
        fun stats(xs: IntRange): Pair<Float, Float> {
            var sum = 0f; var sq = 0f; var edge = 0f; var n = 0
            for (y in y0 until y1) for (x in xs) {
                val v = luma[y * w + x]
                sum += v; sq += v * v; n++
                if (x + 1 < w) edge += abs(v - luma[y * w + x + 1])
                if (y + 1 < h) edge += abs(v - luma[(y + 1) * w + x])
            }
            val mean = sum / n
            val sd = kotlin.math.sqrt((sq / n - mean * mean).coerceAtLeast(0f))
            return mean to (edge / n + sd * 0.5f)
        }
        val pos = if (chosen != app.orchard.tv.settings.ClockPosition.Auto) chosen else {
            listOf(app.orchard.tv.settings.ClockPosition.Left, app.orchard.tv.settings.ClockPosition.Middle, app.orchard.tv.settings.ClockPosition.Right)
                .minByOrNull { p ->
                    val (mean, busy) = stats(region(p))
                    busy + (mean - 170f).coerceAtLeast(0f) * 0.15f - if (p == app.orchard.tv.settings.ClockPosition.Left) 0.6f else 0f
                }!!
        }
        return ClockSpot(pos, stats(region(pos)).first > BRIGHT)
    }

    /** Fork: a clock's place where the picture is lighter than this (of 255) takes a deeper shade. */
    private const val BRIGHT = 150f

    fun shadeForHome(b: Bitmap, spot: ClockSpot = clockSpot(b, app.orchard.tv.settings.AerialSettings.clockPosition)): Bitmap {
        val out = if (b.isMutable) b else b.copy(Bitmap.Config.ARGB_8888, true)
        val c = Canvas(out)
        val w = out.width.toFloat()
        val h = out.height.toFloat()
        c.drawRect(0f, 0f, w, h, Paint().apply {
            shader = LinearGradient(
                0f, 0f, 0f, h,
                SHADE.map(::black).toIntArray(),
                SHADE_AT, Shader.TileMode.CLAMP,
            )
        })
        c.drawRect(0f, 0f, w, h, Paint().apply { shader = clockShade(w, h, spot) })
        return out
    }

    /**
     * Fork: the shade behind the clock, wherever it stands (AerialSettings.clockPosition), so the
     * clock reads on a bright sky at the right or in the middle as at the left. At the side, black
     * at [SHADE_LEFT] fading out by mid-screen; in the middle, a soft oval around where the clock
     * stands. The huge clock covers more of the picture, so its shade is a little deeper and wider.
     * A shader for [w] by [h], so Home draws the same over a paused film as it bakes into a picture.
     */
    fun clockShade(w: Float, h: Float, spot: ClockSpot = Wallpapers.clockSpot): Shader {
        val huge = app.orchard.tv.settings.AerialSettings.hugeClock
        // Fork: a bright sky or snow where the clock stands takes a third more shade.
        val a = (if (huge) SHADE_HUGE else SHADE_LEFT) * if (spot.bright) 1.35f else 1f
        val reach = if (huge) 0.62f else 0.5f
        val pos = app.orchard.tv.settings.AerialSettings.clockPosition.takeIf { it != app.orchard.tv.settings.ClockPosition.Auto } ?: spot.position
        return when (pos) {
            app.orchard.tv.settings.ClockPosition.Left, app.orchard.tv.settings.ClockPosition.Auto -> LinearGradient(0f, 0f, w * reach, 0f, black(a), 0, Shader.TileMode.CLAMP)
            app.orchard.tv.settings.ClockPosition.Right -> LinearGradient(w, 0f, w * (1f - reach), 0f, black(a), 0, Shader.TileMode.CLAMP)
            app.orchard.tv.settings.ClockPosition.Middle -> {
                // A circle stretched sideways into an oval, centred on the clock.
                val cy = h * (if (huge) 0.3f else 0.24f)
                val r = h * (if (huge) 0.5f else 0.42f)
                RadialGradient(w / 2f, cy, r, intArrayOf(black(a * 1.15f), black(a * 0.55f), 0), floatArrayOf(0f, 0.55f, 1f), Shader.TileMode.CLAMP)
                    .apply { setLocalMatrix(Matrix().apply { setScale(1.9f, 1f, w / 2f, cy) }) }
            }
        }
    }

    /** The huge clock's side shade. */
    const val SHADE_HUGE = 0.28f

    /** Changes when [clockShade] would: part of the baked backdrop's key. */
    val shadeKey: String
        get() = "${app.orchard.tv.settings.AerialSettings.clockPosition.key}-${app.orchard.tv.settings.AerialSettings.hugeClock}"

    /**
     * A home backdrop frosted: shrunk and blurred three times over. Small
     * on purpose; it is only ever seen blurred, and it is drawn stretched in a single pass.
     */
    fun frost(b: Bitmap): Bitmap {
        val w = 320
        val h = 180
        val small = Bitmap.createScaledBitmap(b, w, h, true)
        val px = IntArray(w * h)
        small.getPixels(px, 0, w, 0, 0, w, h)
        repeat(3) { boxBlur(px, w, h, 7) }
        val out = small.copy(Bitmap.Config.ARGB_8888, true).also { if (it !== small) small.recycle() }
        out.setPixels(px, 0, w, 0, 0, w, h)
        return out
    }

    /** Fine noise, so long gradients do not band on 8-bit panels. */
    fun dither(b: Bitmap) {
        val w = b.width
        val h = b.height
        val row = IntArray(w)
        val rnd = Random(7)
        for (y in 0 until h) {
            b.getPixels(row, 0, w, 0, y, w, 1)
            for (x in 0 until w) {
                val c = row[x]
                val n = rnd.nextInt(-2, 3)
                val r = ((c shr 16) and 0xFF) + n
                val g = ((c shr 8) and 0xFF) + n
                val bl = (c and 0xFF) + n
                row[x] = (c and 0xFF000000.toInt()) or (r.coerceIn(0, 255) shl 16) or (g.coerceIn(0, 255) shl 8) or bl.coerceIn(0, 255)
            }
            b.setPixels(row, 0, w, 0, y, w, 1)
        }
    }

    fun boxBlur(px: IntArray, w: Int, h: Int, r: Int) {
        val tmp = IntArray(px.size)
        pass(px, tmp, w, h, r, horizontal = true)
        pass(tmp, px, w, h, r, horizontal = false)
    }

    private fun pass(src: IntArray, dst: IntArray, w: Int, h: Int, r: Int, horizontal: Boolean) {
        val lines = if (horizontal) h else w
        val len = if (horizontal) w else h
        for (l in 0 until lines) {
            for (i in 0 until len) {
                var a = 0; var rr = 0; var g = 0; var b = 0; var n = 0
                for (k in -r..r) {
                    val j = (i + k).coerceIn(0, len - 1)
                    val c = if (horizontal) src[l * w + j] else src[j * w + l]
                    a += c ushr 24; rr += (c shr 16) and 0xFF; g += (c shr 8) and 0xFF; b += c and 0xFF; n++
                }
                val idx = if (horizontal) l * w + i else i * w + l
                dst[idx] = ((a / n) shl 24) or ((rr / n) shl 16) or ((g / n) shl 8) or (b / n)
            }
        }
    }

    private fun black(alpha: Float) = android.graphics.Color.argb((alpha * 255).roundToInt(), 0, 0, 0)
}

/** Fork: where the clock stands on a picture, and whether the picture is bright there (Images.clockSpot). */
data class ClockSpot(val position: app.orchard.tv.settings.ClockPosition, val bright: Boolean) {
    val key: String get() = "${position.key}${if (bright) "+" else ""}"

    companion object {
        fun of(key: String?): ClockSpot? {
            if (key == null) return null
            val p = app.orchard.tv.settings.ClockPosition.of(key.removeSuffix("+")) ?: return null
            return ClockSpot(p, key.endsWith("+"))
        }
    }
}

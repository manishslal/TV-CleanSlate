package app.orchard.tv.ui.home

import androidx.compose.foundation.Canvas
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.runtime.Composable
import androidx.compose.runtime.remember
import androidx.compose.ui.Modifier
import androidx.compose.ui.geometry.Rect
import androidx.compose.ui.graphics.BlendMode
import androidx.compose.ui.graphics.CompositingStrategy
import androidx.compose.ui.graphics.drawscope.clipRect
import androidx.compose.ui.graphics.drawscope.translate
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.unit.IntSize
import app.orchard.tv.wallpaper.Depth
import kotlin.math.roundToInt

/**
 * Fork: the wallpaper's subject in front of the large clock (Depth), as a phone's lock screen
 * lets a mountain stand before the time. Drawn over the clock and only within its bounds
 * ([clock], in the pixels of the layer that scrolls with it, which at rest is the screen's): the same pixels as the backdrop beneath, with the same wash,
 * so it shows only where it covers the clock. Left out unless the subject reaches the clock and
 * covers between [MIN_OVER] and [MAX_OVER] of it: more, and the time could not be read.
 * [scroll] is how far Home has scrolled (the clock goes up with it; the backdrop does not), and
 * [alpha] the clock's own fade.
 */
@Composable
fun DepthOverClock(clock: Rect?, scroll: () -> Float, alpha: () -> Float) {
    val front = Depth.front ?: return
    val mask = Depth.mask ?: return
    if (clock == null) return
    // How much of the clock the subject covers, read from the mask once for each place the clock takes.
    val (over, worst) = remember(mask, clock) {
        coverage(mask, clock, front.width.toFloat()).also { (o, w) -> app.orchard.tv.system.DiagLog.i("depth: subject covers ${"%.2f".format(o)} of the time, ${"%.2f".format(w)} of its most covered digit") }
    }
    if (over < MIN_OVER || over > MAX_OVER || worst > MAX_DIGIT) return
    Canvas(Modifier.fillMaxSize().graphicsLayer { this.alpha = alpha(); compositingStrategy = CompositingStrategy.Offscreen }) {
        val (wash, washAlpha) = wallpaperWash()
        val s = scroll()
        // Cut to the clock, which scrolls with this layer; the front lies where the backdrop does,
        // which does not scroll, so it is drawn back down by as much.
        clipRect(clock.left, clock.top, clock.right, clock.bottom) {
            translate(0f, s) {
                drawImage(front, dstSize = IntSize(size.width.roundToInt(), (size.height * Depth.FRONT_SHARE).roundToInt()))
                if (washAlpha > 0.005f) drawRect(wash, alpha = washAlpha, blendMode = BlendMode.SrcAtop)
            }
        }
    }
}

/**
 * The share of [clock] (screen pixels, the screen [screenW] wide) that the mask marks as subject,
 * and the most of any one of [SLICES] slices across it, each about a digit wide: a subject that
 * leaves the time three-quarters clear can still hide one digit whole, and "3:09" read "3:0".
 */
private fun coverage(mask: ByteArray, clock: Rect, screenW: Float): Pair<Float, Float> {
    val k = Depth.MASK_W / screenW
    val x0 = (clock.left * k).toInt().coerceIn(0, Depth.MASK_W - 1)
    val x1 = (clock.right * k).toInt().coerceIn(x0 + 1, Depth.MASK_W)
    val y0 = (clock.top * k).toInt().coerceIn(0, Depth.MASK_H - 1)
    val y1 = (clock.bottom * k).toInt().coerceIn(y0 + 1, Depth.MASK_H)
    val on = IntArray(SLICES)
    val all = IntArray(SLICES)
    for (y in y0 until y1 step 2) for (x in x0 until x1 step 2) {
        val i = ((x - x0) * SLICES / (x1 - x0)).coerceIn(0, SLICES - 1)
        all[i]++
        if ((mask[y * Depth.MASK_W + x].toInt() and 0xFF) > 128) on[i]++
    }
    val total = all.sum()
    if (total == 0) return 0f to 0f
    val worst = (0 until SLICES).maxOf { if (all[it] == 0) 0f else on[it].toFloat() / all[it] }
    return on.sum().toFloat() / total to worst
}

/** About one slice per digit of "10:45 PM". */
private const val SLICES = 5

/** No digit more than half hidden. */
private const val MAX_DIGIT = 0.5f

/** The subject must reach into the clock, and leave most of it to be read. */
private const val MIN_OVER = 0.03f
private const val MAX_OVER = 0.35f

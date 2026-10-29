package app.orchard.tv.ui.home

import androidx.compose.animation.core.Animatable
import androidx.compose.animation.core.CubicBezierEasing
import androidx.compose.animation.core.tween
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.text.BasicText
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.key
import androidx.compose.runtime.mutableFloatStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.drawWithContent
import androidx.compose.ui.graphics.TransformOrigin
import androidx.compose.ui.graphics.drawscope.clipRect
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.layout.FirstBaseline
import androidx.compose.ui.layout.layout
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.rememberTextMeasurer
import app.orchard.tv.settings.AerialSettings
import app.orchard.tv.ui.theme.LocalReducedMotion
import kotlinx.coroutines.delay
import kotlin.math.max
import kotlin.math.roundToInt

/**
 * Fork: text that changes the way a departure board's flaps do. Each character that changes folds
 * its top half down over the new one, and the new bottom half swings up into place; a digit that
 * has further to go (back from sleep, the hour rolling over) runs through the ones between, fast.
 *
 * Nothing moves at rest: a character animates only when it changes. With the flip clock off, or
 * the TV's animations removed, the text simply changes. Characters line up from the right, so
 * "12:59 PM" to "1:00 PM" turns the hour's last digit and lets the first one fall away.
 */
@Composable
fun FlipText(text: String, style: TextStyle, modifier: Modifier = Modifier) {
    if (!AerialSettings.flipClock || LocalReducedMotion.current) {
        BasicText(text, style = style, modifier = modifier)
        return
    }
    // Tabular figures: every digit the same width, so a turning digit never nudges its neighbours.
    val cellStyle = remember(style) { style.copy(fontFeatureSettings = "tnum") }
    // The longest the text has been, so a cell keeps its key when "12:59" becomes "1:00".
    val longest = remember { IntArray(1) }
    longest[0] = max(longest[0], text.length)
    val padded = text.padStart(longest[0])
    // The row tells its parent where the text's baseline is, as a BasicText does: the clocks
    // above are cut to the digits' cap height from it (capBox).
    val measurer = rememberTextMeasurer()
    val baseline = remember(cellStyle, measurer) { measurer.measure("0", cellStyle).firstBaseline.roundToInt() }
    Row(modifier.layout { m, c ->
        val p = m.measure(c)
        layout(p.width, p.height, mapOf(FirstBaseline to baseline)) { p.place(0, 0) }
    }) {
        padded.forEachIndexed { i, c ->
            // Keyed from the right, so a cell keeps its place as the string grows or shrinks.
            key(padded.length - i) { Flap(c, cellStyle, delayMs = i * STAGGER_MS, leading = i < padded.length - text.length) }
        }
    }
}

@Composable
private fun Flap(target: Char, style: TextStyle, delayMs: Int, leading: Boolean) {
    // [shown] is the character at rest; [next] the one the current flap turns to.
    var shown by remember { mutableStateOf(target) }
    var next by remember { mutableStateOf(target) }
    val turn = remember { Animatable(1f) }
    LaunchedEffect(target) {
        if (target == shown && turn.value >= 1f) return@LaunchedEffect
        val steps = steps(shown, target)
        delay(delayMs.toLong())
        for (c in steps) {
            turn.snapTo(0f)
            next = c
            turn.animateTo(1f, tween(if (steps.size == 1) ONE_FLIP_MS else RUN_FLIP_MS, easing = FlapEasing))
            shown = c
        }
    }
    // A leading space at rest takes no room: the hour's first digit falls away rather than leave a gap.
    if (leading && shown == ' ' && next == ' ') return
    var split by remember { mutableFloatStateOf(0f) }
    val p = turn.value
    val flipping = shown != next
    Box {
        // Behind: the new top half, coming up as the old one falls away from it, and the old
        // bottom half until the new one lands over it. Without the cards of a real board the halves
        // behind fade, so two characters never show at once.
        Half(next, style, top = true, split = { split }, alpha = if (flipping) p * 2f else 1f) { split = it }
        Half(shown, style, top = false, split = { split }, alpha = if (flipping) 2f - p * 2f else 1f)
        if (flipping && p < 0.5f) {
            // The old top half falls toward the viewer, folding on the split.
            Half(shown, style, top = true, split = { split }, alpha = 1f - p * 1.2f, rotation = -180f * p)
        } else if (flipping) {
            // The new bottom half swings up from flat into place.
            Half(next, style, top = false, split = { split }, alpha = 0.4f + 0.6f * (p - 0.5f) * 2f, rotation = 90f * (1f - (p - 0.5f) * 2f))
        }
    }
}

/** The top or bottom half of [c], cut at the middle of a digit's height, turned [rotation] degrees about that line. */
@Composable
private fun Half(
    c: Char,
    style: TextStyle,
    top: Boolean,
    split: () -> Float,
    alpha: Float,
    rotation: Float = 0f,
    onSplit: ((Float) -> Unit)? = null,
) {
    BasicText(
        c.toString(),
        style = style,
        onTextLayout = { layout ->
            // The middle of a digit: half the cap height above the baseline.
            onSplit?.invoke(layout.firstBaseline - with(layout.layoutInput.density) { style.fontSize.toPx() } * CAP_HALF)
        },
        modifier = Modifier
            .graphicsLayer {
                this.alpha = alpha.coerceIn(0f, 1f)
                rotationX = rotation
                cameraDistance = 10f * density
                val s = split()
                transformOrigin = TransformOrigin(0.5f, if (size.height > 0f) s / size.height else 0.5f)
            }
            .drawWithContent {
                val s = split()
                // Generous on the other three sides: the text's shadow reaches past its box.
                val far = size.height * 2f
                if (top) clipRect(-far, -far, size.width + far, s) { this@drawWithContent.drawContent() }
                else clipRect(-far, s, size.width + far, size.height + far) { this@drawWithContent.drawContent() }
            },
    )
}

/** The characters a flap turns through to get from [from] to [to]: a digit counts up through the ones between. */
private fun steps(from: Char, to: Char): List<Char> {
    if (from == to) return listOf(to)
    if (!from.isDigit() || !to.isDigit()) return listOf(to)
    val out = ArrayList<Char>()
    var d = from - '0'
    val end = to - '0'
    while (d != end) {
        d = (d + 1) % 10
        out += '0' + d
    }
    return out
}

/** Half Inter's cap height, in ems: where a digit's middle is. */
private const val CAP_HALF = 0.727f / 2f

/** One flap, a minute changing: quick to fall, settling as it lands. */
private const val ONE_FLIP_MS = 520

/** Each flap in a run (several digits to go): the board's rattle. */
private const val RUN_FLIP_MS = 130

/** Between neighbouring characters starting, left to right. */
private const val STAGGER_MS = 45

private val FlapEasing = CubicBezierEasing(0.5f, 0f, 0.3f, 1f)

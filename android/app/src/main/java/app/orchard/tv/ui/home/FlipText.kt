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
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.drawWithContent
import androidx.compose.foundation.layout.padding
import androidx.compose.ui.geometry.CornerRadius
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.TransformOrigin
import androidx.compose.ui.graphics.drawscope.clipRect
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.layout.FirstBaseline
import androidx.compose.ui.layout.layout
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.rememberTextMeasurer
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.unit.sp
import app.orchard.tv.settings.ClockStyle
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
 * With [cards] (and Clock set to Cards) each letter and digit sits on its own dark flap card with
 * the board's split across it, and the card's halves turn with it.
 *
 * Nothing moves at rest: a character animates only when it changes. With Clock set to Plain, or
 * the TV's animations removed, the text simply changes. Characters line up from the right, so
 * "12:59 PM" to "1:00 PM" turns the hour's last digit and lets the first one fall away.
 */
@Composable
fun FlipText(text: String, style: TextStyle, modifier: Modifier = Modifier, cards: Boolean = false) {
    val clockStyle = AerialSettings.clockStyle
    if (clockStyle == ClockStyle.Plain) {
        BasicText(text, style = style, modifier = modifier)
        return
    }
    // Cards only where the clock is large enough to carry them; the small clocks just turn.
    val onCards = cards && clockStyle == ClockStyle.Cards
    // Removed animations: the cards stay, and a changed character is simply there.
    val still = LocalReducedMotion.current
    // Tabular figures: every digit the same width, so a turning digit never nudges its neighbours.
    // On cards the text loses its shadow: the card is what stands out from the wallpaper.
    // Each card holds its glyph whole: the clock's tight tracking would push glyphs past their card's edge.
    val cellStyle = remember(style, onCards) {
        style.copy(fontFeatureSettings = "tnum", shadow = if (onCards) null else style.shadow, letterSpacing = if (onCards) 0.sp else style.letterSpacing)
    }
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
            key(padded.length - i) {
                Flap(c, cellStyle, delayMs = i * STAGGER_MS, leading = i < padded.length - text.length, card = onCards && c.isLetterOrDigit(), cardSpace = onCards, still = still)
            }
        }
    }
}

@Composable
private fun Flap(target: Char, style: TextStyle, delayMs: Int, leading: Boolean, card: Boolean, cardSpace: Boolean, still: Boolean) {
    // [shown] is the character at rest; [next] the one the current flap turns to.
    var shown by remember { mutableStateOf(target) }
    var next by remember { mutableStateOf(target) }
    val turn = remember { Animatable(1f) }
    LaunchedEffect(target) {
        if (target == shown && turn.value >= 1f) return@LaunchedEffect
        if (still) { shown = target; next = target; return@LaunchedEffect }
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
    var lines by remember { mutableStateOf(Lines(0f, 0f)) }
    val p = turn.value
    val flipping = shown != next
    // Each card stands a little apart from the next, as a board's flaps do; the colon and the
    // space between the time and AM/PM sit between cards, without one.
    val em = with(LocalDensity.current) { style.fontSize.toDp() }
    val gap = if (cardSpace) Modifier.padding(horizontal = em * CardGap) else Modifier
    Box(gap) {
        // On cards the flap hides what is behind it, as on a real board. Bare, the halves behind
        // fade, so two characters never show at once.
        val fade = flipping && !card
        // Behind: the new top half, and the old bottom half until the new one lands over it.
        Half(next, style, top = true, card = card, lines = { lines }, alpha = if (fade) p * 2f else 1f) { lines = it }
        Half(shown, style, top = false, card = card, lines = { lines }, alpha = if (fade) 2f - p * 2f else 1f)
        if (flipping && p < 0.5f) {
            // The old top half falls toward the viewer, folding on the split, darkening as it turns away from the light.
            Half(shown, style, top = true, card = card, lines = { lines }, alpha = if (card) 1f else 1f - p * 1.2f, rotation = -180f * p, shade = p * 2f)
        } else if (flipping) {
            // The new bottom half swings up from flat into place, lit as it comes up.
            Half(next, style, top = false, card = card, lines = { lines }, alpha = if (card) 1f else 0.4f + 0.6f * (p - 0.5f) * 2f, rotation = 90f * (1f - (p - 0.5f) * 2f), shade = 1f - (p - 0.5f) * 2f)
        }
    }
}

/** Where a character's digit sits in its box: [baseline], and [split], the middle of a digit's height. */
@androidx.compose.runtime.Immutable
private class Lines(val baseline: Float, val split: Float)

/**
 * The top or bottom half of [c], cut at the middle of a digit's height, turned [rotation] degrees
 * about that line; on a [card] when asked, its half of the card turning with it. [shade] (0 to 1)
 * darkens a half turned away from the light.
 */
@Composable
private fun Half(
    c: Char,
    style: TextStyle,
    top: Boolean,
    card: Boolean,
    lines: () -> Lines,
    alpha: Float,
    rotation: Float = 0f,
    shade: Float = 0f,
    onLines: ((Lines) -> Unit)? = null,
) {
    BasicText(
        c.toString(),
        style = style,
        onTextLayout = { layout ->
            val size = with(layout.layoutInput.density) { style.fontSize.toPx() }
            onLines?.invoke(Lines(layout.firstBaseline, layout.firstBaseline - size * CAP_HALF))
        },
        modifier = Modifier
            .graphicsLayer {
                this.alpha = alpha.coerceIn(0f, 1f)
                rotationX = rotation
                cameraDistance = 10f * density
                val s = lines().split
                transformOrigin = TransformOrigin(0.5f, if (size.height > 0f) s / size.height else 0.5f)
            }
            .then(if (card) Modifier.padding(horizontal = with(LocalDensity.current) { style.fontSize.toDp() } * CardPadH) else Modifier)
            .drawWithContent {
                val l = lines()
                val s = l.split
                // Generous on the other three sides: the text's shadow reaches past its box.
                val far = size.height * 2f
                val (y0, y1) = if (top) -far to s else s to size.height + far
                clipRect(-far, y0, size.width + far, y1) {
                    if (card) {
                        val em = style.fontSize.toPx()
                        val padV = em * CardPadV
                        val cap = (l.baseline - s) * 2f
                        val cardTop = l.baseline - cap - padV
                        val cardBottom = l.baseline + padV
                        val r = CornerRadius(em * CardCorner)
                        drawRoundRect(
                            Brush.verticalGradient(0f to CardTop, 1f to CardBottom, startY = cardTop, endY = cardBottom),
                            topLeft = Offset(0f, cardTop), size = Size(size.width, cardBottom - cardTop), cornerRadius = r,
                        )
                        this@drawWithContent.drawContent()
                        // The board's split: a dark hairline where the two halves meet.
                        val h = (em * SplitLine).coerceAtLeast(1f)
                        drawRect(Color.Black.copy(alpha = 0.55f), Offset(0f, s - h / 2f), Size(size.width, h))
                        if (shade > 0.01f) drawRoundRect(
                            Color.Black.copy(alpha = 0.45f * shade),
                            topLeft = Offset(0f, cardTop), size = Size(size.width, cardBottom - cardTop), cornerRadius = r,
                        )
                    } else this@drawWithContent.drawContent()
                }
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

/** A card's dark face, lit a touch from above. */
private val CardTop = Color(0xF2242427)
private val CardBottom = Color(0xF2141416)
// A card's size follows the text's: in ems, so the screensaver's smaller clock has smaller cards.
private const val CardPadH = 0.08f
private const val CardPadV = 0.11f
private const val CardGap = 0.025f
private const val CardCorner = 0.07f
private const val SplitLine = 0.014f

/** Half Inter's cap height, in ems: where a digit's middle is. */
private const val CAP_HALF = 0.727f / 2f

/** One flap, a minute changing: quick to fall, settling as it lands. */
private const val ONE_FLIP_MS = 520

/** Each flap in a run (several digits to go): the board's rattle. */
private const val RUN_FLIP_MS = 130

/** Between neighbouring characters starting, left to right. */
private const val STAGGER_MS = 45

private val FlapEasing = CubicBezierEasing(0.5f, 0f, 0.3f, 1f)

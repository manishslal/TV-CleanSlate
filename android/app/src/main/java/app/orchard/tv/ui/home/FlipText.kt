package app.orchard.tv.ui.home

import androidx.compose.animation.core.Animatable
import androidx.compose.animation.core.CubicBezierEasing
import androidx.compose.animation.core.LinearEasing
import androidx.compose.animation.core.tween
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.text.BasicText
import androidx.compose.runtime.Composable
import androidx.compose.runtime.Immutable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.key
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.drawWithContent
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
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.font.Font
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.rememberTextMeasurer
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.Dp
import app.orchard.tv.R
import app.orchard.tv.settings.AerialSettings
import app.orchard.tv.settings.ClockStyle
import app.orchard.tv.system.Sounds
import app.orchard.tv.ui.theme.LocalReducedMotion
import kotlinx.coroutines.delay
import kotlin.math.max
import kotlin.math.roundToInt

/**
 * Fork: text that changes the way a departure board's flaps do. Each character that changes folds
 * its top half down over the new one, and the new bottom half swings up into place; a digit that
 * has further to go (back from sleep, the hour rolling over) runs through the ones between, fast.
 *
 * With [cards] (and Clock set to Cards) the text is set as a board is: Barlow Condensed on flap
 * cards that are all one width, the widest character's, each character centred on its own; the
 * colon stands between the cards, and AM/PM sits on smaller cards beside the time. With [sound]
 * (and the flap sound on) each flap clacks.
 *
 * Nothing moves at rest: a character animates only when it changes. With Clock set to Plain, or
 * the TV's animations removed, the text simply changes. Characters line up from the right, so
 * "12:59 PM" to "1:00 PM" turns the hour's last digit and lets the first one fall away.
 */
@Composable
fun FlipText(text: String, style: TextStyle, modifier: Modifier = Modifier, cards: Boolean = false, sound: Boolean = false) {
    val clockStyle = AerialSettings.clockStyle
    if (clockStyle == ClockStyle.Plain) {
        BasicText(text, style = style, modifier = modifier)
        return
    }
    // Removed animations: the cards stay, and a changed character is simply there.
    val still = LocalReducedMotion.current
    // Cards only where the clock is large enough to carry them; the small clocks just turn.
    if (cards && clockStyle == ClockStyle.Cards) {
        Board(text, style, modifier, still, sound)
        return
    }
    // Tabular figures: every digit the same width, so a turning digit never nudges its neighbours.
    val cellStyle = remember(style) { style.copy(fontFeatureSettings = "tnum") }
    Row(modifier.baselineOf(cellStyle)) {
        Cells(text) { c, i, leading ->
            Flap(c, cellStyle, delayMs = i * STAGGER_MS, leading = leading, card = null, still = still, sound = false)
        }
    }
}

/** Fork: the board's own type, for the date and weather lines set to match the cards (HomeChrome). */
val BoardFont = FontFamily(Font(R.font.barlow_condensed_semibold))

/** Painted flaps: warm white, not screen white. */
val BoardInk = Color(0xFFEFE7D2)

/** The board: the time on full-size cards, then AM/PM on half-size ones, lined up on their baseline. */
@Composable
private fun Board(text: String, style: TextStyle, modifier: Modifier, still: Boolean, sound: Boolean) {
    val board = remember(style.fontSize) {
        TextStyle(fontFamily = BoardFont, fontSize = style.fontSize, color = BoardInk, textAlign = TextAlign.Center, fontFeatureSettings = "tnum")
    }
    val small = remember(board) { board.copy(fontSize = board.fontSize * SUFFIX_SCALE) }
    val main = text.substringBeforeLast(' ')
    val suffix = if (' ' in text) text.substringAfterLast(' ') else ""
    val density = LocalDensity.current
    val em = with(density) { board.fontSize.toDp() }
    // AM/PM stands on the time's baseline at a fixed offset worked out once: lined up by the rows'
    // baselines, it jumped while a card turned.
    val measurer = rememberTextMeasurer()
    val drop = remember(board, small, measurer) {
        with(density) { (measurer.measure("0", board).firstBaseline - measurer.measure("0", small).firstBaseline).toDp() }
    }
    Row(modifier.baselineOf(board)) {
        CardRow(main, board, still, sound, Modifier)
        if (suffix.isNotEmpty()) {
            Spacer(Modifier.width(em * SUFFIX_GAP))
            CardRow(suffix, small, still, sound = false, modifier = Modifier.padding(top = drop))
        }
    }
}

/** One run of cards, all [style]'s card width; a colon stands between them without one. */
@Composable
private fun CardRow(text: String, style: TextStyle, still: Boolean, sound: Boolean, modifier: Modifier) {
    val density = LocalDensity.current
    val measurer = rememberTextMeasurer()
    val em = with(density) { style.fontSize.toDp() }
    // Every card the width of the widest character it may show, so the board is an even grid.
    val glyph = remember(style, measurer) {
        with(density) { CARD_CHARS.maxOf { measurer.measure(it.toString(), style).size.width }.toDp() }
    }
    val card = Card(width = glyph + em * CARD_PAD_H * 2, em = em)
    Row(modifier) {
        Cells(text) { c, i, leading ->
            if (c == ':') BasicText(":", style = style, modifier = Modifier.padding(horizontal = em * COLON_PAD))
            else Box(Modifier.padding(horizontal = em * CARD_GAP)) {
                Flap(c, style, delayMs = i * STAGGER_MS, leading = leading, card = card, still = still, sound = sound)
            }
        }
    }
}

/**
 * Each character of [text], keyed from the right so a cell keeps its place as the string grows or
 * shrinks; [leading] for a position the string has outgrown (the hour's first digit at 1:00).
 */
@Composable
private fun Cells(text: String, cell: @Composable (c: Char, index: Int, leading: Boolean) -> Unit) {
    // The longest the text has been, so a cell keeps its key when "12:59" becomes "1:00".
    val longest = remember { IntArray(1) }
    longest[0] = max(longest[0], text.length)
    val padded = text.padStart(longest[0])
    padded.forEachIndexed { i, c ->
        key(padded.length - i) { cell(c, i, i < padded.length - text.length) }
    }
}

/**
 * The row tells its parent where the text's baseline is, as a BasicText does: the clocks above are
 * cut to the digits' cap height from it (capBox).
 */
@Composable
private fun Modifier.baselineOf(style: TextStyle): Modifier {
    val measurer = rememberTextMeasurer()
    val baseline = remember(style, measurer) { measurer.measure("0", style).firstBaseline.roundToInt() }
    return layout { m, c ->
        val p = m.measure(c)
        layout(p.width, p.height, mapOf(FirstBaseline to baseline)) { p.place(0, 0) }
    }
}

/** A flap card's size: its [width], and the text's [em] its padding and corners follow. */
@Immutable
private class Card(val width: Dp, val em: Dp)

@Composable
private fun Flap(target: Char, style: TextStyle, delayMs: Int, leading: Boolean, card: Card?, still: Boolean, sound: Boolean) {
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
            if (sound) Sounds.flap()
            turn.animateTo(1f, tween(if (steps.size == 1) ONE_FLIP_MS else RUN_FLIP_MS, easing = LinearEasing))
            shown = c
        }
    }
    // A leading space at rest takes no room: the hour's first digit falls away rather than leave a gap.
    if (leading && shown == ' ' && next == ' ') return
    var lines by remember { mutableStateOf(Lines(0f, 0f)) }
    val p = turn.value
    val flipping = shown != next
    // The falling half gathers speed as it drops, as a flap does under its own weight.
    val fall = (p / 0.5f).coerceIn(0f, 1f).let { it * it }
    // The rising half overshoots a touch past flat and settles: the flap bouncing on its stop.
    val q = ((p - 0.5f) / 0.5f).coerceIn(0f, 1f)
    val rise = if (q < 0.8f) lerp(90f, -8f, LandEasing.transform(q / 0.8f)) else lerp(-8f, 0f, (q - 0.8f) / 0.2f)
    Box {
        // Cards are opaque: the flap hides what is behind it, as on a real board. Bare, the halves
        // behind fade, so two characters never show at once.
        val fade = flipping && card == null
        // Behind: the new top half, and the old bottom half until the new one lands over it.
        Half(next, style, top = true, card = card, lines = { lines }, alpha = if (fade) p * 2f else 1f) { lines = it }
        Half(shown, style, top = false, card = card, lines = { lines }, alpha = if (fade) 2f - p * 2f else 1f, shadow = true)
        if (flipping && p < 0.5f) {
            // The old top half falls toward the viewer, folding on the split, darkening as it turns away from the light.
            Half(shown, style, top = true, card = card, lines = { lines }, alpha = if (card != null) 1f else 1f - p * 1.2f, rotation = -90f * fall, shade = fall)
        } else if (flipping) {
            Half(next, style, top = false, card = card, lines = { lines }, alpha = if (card != null) 1f else 0.4f + 0.6f * q, rotation = rise, shade = 1f - q)
        }
        // The axle the flaps turn on: a pin at each side of the split, standing out from the card.
        if (card != null) Canvas(Modifier.matchParentSize()) {
            val s = lines.split
            val w = card.em.toPx() * PIN_W
            val h = card.em.toPx() * PIN_H
            drawRect(Pin, Offset(-w * 0.6f, s - h / 2f), Size(w, h))
            drawRect(Pin, Offset(size.width - w * 0.4f, s - h / 2f), Size(w, h))
        }
    }
}

/** Where a character's digit sits in its box: [baseline], and [split], the middle of a digit's height. */
@Immutable
private class Lines(val baseline: Float, val split: Float)

/**
 * The top or bottom half of [c], cut at the middle of a digit's height, turned [rotation] degrees
 * about that line; on a [card] when given, its half of the card turning with it. [shade] (0 to 1)
 * darkens a half turned away from the light; [shadow] casts the card's shadow under it.
 */
@Composable
private fun Half(
    c: Char,
    style: TextStyle,
    top: Boolean,
    card: Card?,
    lines: () -> Lines,
    alpha: Float,
    rotation: Float = 0f,
    shade: Float = 0f,
    shadow: Boolean = false,
    onLines: ((Lines) -> Unit)? = null,
) {
    // Board cards use Barlow's cap height; bare text keeps the font it came with (Inter).
    val capHalf = (if (card != null) BOARD_CAP else INTER_CAP) / 2f
    BasicText(
        c.toString(),
        style = style,
        onTextLayout = { layout ->
            val size = with(layout.layoutInput.density) { style.fontSize.toPx() }
            onLines?.invoke(Lines(layout.firstBaseline, layout.firstBaseline - size * capHalf))
        },
        modifier = Modifier
            .graphicsLayer {
                this.alpha = alpha.coerceIn(0f, 1f)
                rotationX = rotation
                cameraDistance = 10f * density
                val s = lines().split
                transformOrigin = TransformOrigin(0.5f, if (size.height > 0f) s / size.height else 0.5f)
            }
            .then(if (card != null) Modifier.width(card.width) else Modifier)
            .drawWithContent {
                val l = lines()
                val s = l.split
                // Generous on the other three sides: the text's shadow reaches past its box.
                val far = size.height * 2f
                val (y0, y1) = if (top) -far to s else s to size.height + far
                if (card == null) {
                    clipRect(-far, y0, size.width + far, y1) { this@drawWithContent.drawContent() }
                    return@drawWithContent
                }
                val em = card.em.toPx()
                val padV = em * CARD_PAD_V
                val cap = (l.baseline - s) * 2f
                val cardTop = l.baseline - cap - padV
                val cardBottom = l.baseline + padV
                val r = CornerRadius(em * CARD_CORNER)
                // The card's shadow on the wallpaper, under the lower half.
                if (shadow) drawRoundRect(
                    Color.Black.copy(alpha = 0.35f),
                    topLeft = Offset(em * 0.01f, s + em * 0.03f), size = Size(size.width, cardBottom - s),
                    cornerRadius = r,
                )
                clipRect(-far, y0, size.width + far, y1) {
                    // Lit from above: the top half's face lighter than the bottom's.
                    val (a, b) = if (top) TopFaceA to TopFaceB else BottomFaceA to BottomFaceB
                    drawRoundRect(
                        Brush.verticalGradient(0f to a, 1f to b, startY = if (top) cardTop else s, endY = if (top) s else cardBottom),
                        topLeft = Offset(0f, cardTop), size = Size(size.width, cardBottom - cardTop), cornerRadius = r,
                    )
                    this@drawWithContent.drawContent()
                    // The gap between the two flaps.
                    val gap = (em * SPLIT_GAP).coerceAtLeast(1.5f)
                    drawRect(Gap, Offset(0f, s - gap / 2f), Size(size.width, gap))
                    if (shade > 0.01f) drawRoundRect(
                        Color.Black.copy(alpha = 0.5f * shade),
                        topLeft = Offset(0f, cardTop), size = Size(size.width, cardBottom - cardTop), cornerRadius = r,
                    )
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

private fun lerp(a: Float, b: Float, t: Float) = a + (b - a) * t

/** The characters a clock's card may show; the widest sets every card's width. */
private const val CARD_CHARS = "0123456789AMP"

/** A card's faces, lit from above, and the gap between the flaps and the axle's pins. */
private val TopFaceA = Color(0xF22A2B2E)
private val TopFaceB = Color(0xF2202124)
private val BottomFaceA = Color(0xF21B1C1E)
private val BottomFaceB = Color(0xF2141517)
private val Gap = Color(0xFF060607)
private val Pin = Color(0xFF45474B)

// A card's proportions, in ems of its text, so the screensaver's smaller clock has smaller cards.
private const val CARD_PAD_H = 0.07f
private const val CARD_PAD_V = 0.1f
private const val CARD_GAP = 0.03f
private const val CARD_CORNER = 0.06f
private const val SPLIT_GAP = 0.018f
private const val COLON_PAD = 0.04f
private const val PIN_W = 0.03f
private const val PIN_H = 0.07f

/** AM/PM's cards beside the time's, and the space before them. */
private const val SUFFIX_SCALE = 0.5f
private const val SUFFIX_GAP = 0.12f

/** Cap heights, in ems: where a digit's middle is. */
private const val INTER_CAP = 0.727f
private const val BOARD_CAP = 0.7f

/** One flap, a minute changing. */
private const val ONE_FLIP_MS = 460

/** Each flap in a run (several digits to go): the board's rattle. */
private const val RUN_FLIP_MS = 140

/** Between neighbouring characters starting, left to right. */
private const val STAGGER_MS = 60

/** The rising half's swing to its stop. */
private val LandEasing = CubicBezierEasing(0.2f, 0.6f, 0.35f, 1f)

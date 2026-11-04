package app.orchard.tv.ui.components

import android.graphics.BitmapShader
import android.graphics.LinearGradient
import android.graphics.Matrix
import android.graphics.Paint
import android.graphics.Shader
import androidx.compose.animation.core.Animatable
import androidx.compose.animation.core.tween
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.composed
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.drawWithCache
import androidx.compose.ui.geometry.CornerRadius
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Rect
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.BlendMode
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.ColorFilter
import androidx.compose.ui.graphics.ColorMatrix
import androidx.compose.ui.graphics.ImageBitmap
import androidx.compose.ui.graphics.ShaderBrush
import androidx.compose.ui.graphics.TileMode
import androidx.compose.ui.graphics.asAndroidBitmap
import androidx.compose.ui.graphics.asAndroidPath
import androidx.compose.ui.graphics.asImageBitmap
import androidx.compose.ui.graphics.drawscope.DrawScope
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.graphics.layer.CompositingStrategy
import androidx.compose.ui.graphics.layer.drawLayer
import androidx.compose.ui.graphics.toArgb
import androidx.compose.ui.layout.onGloballyPositioned
import androidx.compose.ui.layout.positionInRoot
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.IntSize
import androidx.compose.ui.unit.dp
import androidx.core.graphics.createBitmap
import app.orchard.tv.settings.AerialSettings
import app.orchard.tv.ui.theme.LocalLightPanel
import app.orchard.tv.ui.theme.Palette
import app.orchard.tv.ui.theme.Motion
import app.orchard.tv.wallpaper.Wallpapers
import kotlin.math.roundToInt

/**
 * Liquid Glass, the material of tvOS 26's panels, without a live blur: the TVs here mostly run
 * Android 9–11 (no RenderEffect) and their GPUs could not afford one. The surface shows the part
 * of the frosted wallpaper (Wallpapers.frosted, baked small) that lies behind it, dimmed so white
 * text reads on any picture, and light catches its rim. The frosted picture is stretched over the
 * screen through a shader, so a panel costs one fill of its own area, as a flat color did. In
 * "Follow App" mode there is no frosted wallpaper and the panel is the plain dark glass color.
 *
 * Replaces clip + background + border; [bottomCorners] false for a sheet rising from the bottom.
 * The continuous outline is a baked mask (see SmoothCorners.kt); children are clipped to a plain
 * rounded rect, which holds it.
 */
fun Modifier.liquidGlass(corner: Dp, bottomCorners: Boolean = true, band: Dp = 26.dp, lightPanel: Boolean? = null, tile: Boolean = false, clear: Boolean = false): Modifier = composed {
    val (frost, under, swap) = rememberFrostSwap(Wallpapers.frosted.takeIf { Wallpapers.backdrop != null })
    // Fork: a light panel brightens the frosted picture instead of dimming it.
    val light = lightPanel ?: LocalLightPanel.current
    // Fork: [clear] is the dock's glass (most of the picture, a little lift), for Home's own
    // controls that sit on the wallpaper beside it, such as the status pill's focus.
    val tint = if (clear) ClearTint else if (light) frost?.let(::lightPanelTint) ?: LightPanelTint else PanelTint
    val plain = if (clear) Color.White.copy(alpha = 0.2f) else if (light) Palette.Light.Glass else Palette.Dark.Glass
    var at by remember { mutableStateOf(Offset.Zero) }
    var root by remember { mutableStateOf(IntSize.Zero) }
    val shape = if (bottomCorners) RoundedCornerShape(corner) else RoundedCornerShape(topStart = corner, topEnd = corner)
    this
        .onGloballyPositioned { c ->
            at = c.positionInRoot()
            var r = c
            while (true) r = r.parentLayoutCoordinates ?: break
            root = r.size
        }
        .clip(shape)
        .drawWithCache {
            // Read here so the glass is made again when the appearance or the glass changes.
            val tint = tint
            val lens = AerialSettings.liquidGlass && LiquidLens.supported
            val shader = frost?.let { BitmapShader(it.asAndroidBitmap(), Shader.TileMode.CLAMP, Shader.TileMode.CLAMP) }
            val underShader = under?.let { BitmapShader(it.asAndroidBitmap(), Shader.TileMode.CLAMP, Shader.TileMode.CLAMP) }
            val radius = corner.toPx()
            val w = size.width.roundToInt().coerceAtLeast(1)
            val h = size.height.roundToInt().coerceAtLeast(1)
            val mask = smoothMask(w, h, radius, if (bottomCorners) radius else 0f)
            val brush = shader?.let { ShaderBrush(it) }
            val flat = mask.asImageBitmap()
            // The glass in a layer the GPU keeps: drawn again only when it moves or its picture
            // fades, not with every frame of what lies on it. Drawn each frame, the Personalize
            // sheet cost MiTV-MFTR0's GPU about 29 ms a frame while focus moved along its cards.
            val glass = obtainGraphicsLayer().apply { compositingStrategy = CompositingStrategy.Offscreen }
            var drawnAt: Offset? = null
            var drawnRoot = IntSize.Zero
            var drawnT = -1f
            onDrawBehind {
                if (shader == null || brush == null || root == IntSize.Zero) {
                    drawImage(flat, dstSize = IntSize(w, h), colorFilter = ColorFilter.tint(plain))
                    drawGlassEdge(size, radius, bottomCorners, light)
                } else if (AerialSettings.lite) {
                    // Lite (AerialSettings.lite): one solid color, the frosted wallpaper's average as
                    // the glass would dim it, without the layer the glass needs every frame, which
                    // weak GPUs feel most while a panel slides in. Solid: the plain glass color is
                    // see-through and showed the tiles under the Personalize sheet.
                    drawImage(flat, dstSize = IntSize(w, h), colorFilter = ColorFilter.tint(if (clear) liteClear(frost) else liteGlass(frost, light)))
                    drawGlassEdge(size, radius, bottomCorners, light)
                } else {
                    val t = swap()
                    if (drawnAt != at || drawnRoot != root || drawnT != t) {
                        drawnAt = at
                        drawnRoot = root
                        drawnT = t
                        fun place(s: BitmapShader, image: ImageBitmap) = s.setLocalMatrix(Matrix().apply {
                            setScale(root.width.toFloat() / image.width, root.height.toFloat() / image.height)
                            postTranslate(-at.x, -at.y)
                        })
                        place(shader, frost)
                        // The outline is drawn first in the layer and the glass kept where it lies.
                        // One shader per draw: a ComposeShader of the glass and the mask (1.27-1.30)
                        // came out an opaque dark slab on Xiaomi MiTV-ASTP0 (Android 9, Mali-G31),
                        // and before Android 9 hardware drawing cannot compose two bitmaps.
                        glass.record(this, layoutDirection, IntSize(w, h)) {
                            drawImage(flat, dstSize = IntSize(w, h))
                            if (lens && under != null && t < 1f) {
                                // A new picture fades in over the old one through the lens, so the
                                // glass stays liquid while it changes rather than going flat.
                                drawRect(ShaderBrush(LiquidLens.shader(under, if (tile) null else Wallpapers.backdrop, IntSize(w, h), radius, !bottomCorners, at, root, light, density, band.toPx(), tile = tile)), blendMode = BlendMode.SrcIn)
                                drawRect(ShaderBrush(LiquidLens.shader(frost, if (tile) null else Wallpapers.backdrop, IntSize(w, h), radius, !bottomCorners, at, root, light, density, band.toPx(), tile = tile)), alpha = t, blendMode = BlendMode.SrcAtop)
                            } else if (underShader != null && under != null && t < 1f) {
                                // A new frosted picture fades in over the old within the outline.
                                place(underShader, under)
                                drawRect(ShaderBrush(underShader), colorFilter = tint, blendMode = BlendMode.SrcIn)
                                drawRect(brush, alpha = t, colorFilter = tint, blendMode = BlendMode.SrcAtop)
                            } else if (lens) {
                                // Fork: Liquid Glass: the lens shader brings its own rim and light.
                                drawRect(ShaderBrush(LiquidLens.shader(frost, if (tile) null else Wallpapers.backdrop, IntSize(w, h), radius, !bottomCorners, at, root, light, density, band.toPx(), tile = tile)), blendMode = BlendMode.SrcIn)
                            } else {
                                drawRect(brush, colorFilter = tint, blendMode = BlendMode.SrcIn)
                            }
                            if (!lens) drawGlassEdge(size, radius, bottomCorners, light)
                        }
                    }
                    drawLayer(glass)
                }
            }
        }
}

/**
 * The frosted wallpaper as glass and backdrops show it: a new one fades in over the last
 * ([FROST_SWAP_MS]) rather than cutting to it. A moving film that stops gives Home its frosted
 * picture in two or three steps (a small frame, then the exact one, LiveWallpaper), and with a
 * panel up each cut was a jump in its glass (Xiaomi MiTV-MFTR0, TCL MT5879). Returns the picture,
 * the one under it while it fades in, and how far it has (0 to 1).
 */
@Composable
internal fun rememberFrostSwap(frost: ImageBitmap?): Triple<ImageBitmap?, ImageBitmap?, () -> Float> {
    var shown by remember { mutableStateOf(frost) }
    var under by remember { mutableStateOf<ImageBitmap?>(null) }
    val swap = remember { Animatable(1f) }
    LaunchedEffect(frost) {
        if (frost === shown) return@LaunchedEffect
        val old = shown
        shown = frost
        // From nothing, or to nothing: it simply takes over.
        if (old == null || frost == null) {
            under = null
            swap.snapTo(1f)
            return@LaunchedEffect
        }
        // Mid-fade, the one further in stays under the next.
        under = if (swap.value > 0.5f) old else under ?: old
        swap.snapTo(0f)
        swap.animateTo(1f, tween(FROST_SWAP_MS, easing = Motion.EaseInOut))
        under = null
    }
    return Triple(shown, under, { swap.value })
}

private const val FROST_SWAP_MS = 900

/** The lit edge of a piece of glass and its outline, as [glassEdge] bakes them. */
private fun DrawScope.drawGlassEdge(size: Size, radius: Float, bottomCorners: Boolean, light: Boolean) {
    val w = size.width.roundToInt()
    val h = size.height.roundToInt()
    if (w <= 0 || h <= 0) return
    drawImage(glassEdge(w, h, radius, Glisse.RIM.dp.toPx(), bottomCorners, light, Glisse.EDGE.dp.toPx()), dstSize = IntSize(w, h))
}

/**
 * Fork: the lit edge alone, for a control too small for the lens to show: a sheen and a rim over
 * what it holds. White light has nothing to catch on a light panel, so a faint dark line holds the
 * edge there. One baked picture per size, so it works on every TV.
 */
fun Modifier.glassRim(corner: Dp): Modifier = composed {
    val light = LocalLightPanel.current
    drawWithCache {
        val w = size.width.roundToInt().coerceAtLeast(1)
        val h = size.height.roundToInt().coerceAtLeast(1)
        val radius = corner.toPx().coerceAtMost(size.minDimension / 2)
        val edge = glassEdge(w, h, radius, Glisse.RIM.dp.toPx(), light = light, edge = Glisse.EDGE.dp.toPx())
        onDrawWithContent {
            drawContent()
            drawImage(edge, dstSize = IntSize(w, h))
        }
    }
}

/**
 * The lit edge of a [w]×[h] piece of glass, as glisse measures iOS 26.1's: a line [rim] wide.
 * Glisse fades it from the very edge to nothing at its inner side; a line this thin is drawn even,
 * at that fade's average (0.6 of the shine), so it carries the same light. Light from the top left lights the left
 * side and the top, and its reflection the right side and the bottom; it fades out over the two
 * corners between (top right, bottom left). [light] panels get the stronger shine of a light page.
 * Under it, an [edge]-wide line holds the outline: white on dark glass, black on light. Made once
 * per size, so it costs nothing to draw and works on every TV.
 */
fun glassEdge(w: Int, h: Int, radius: Float, rim: Float, bottomCorners: Boolean = true, light: Boolean = false, edge: Float = 0f): ImageBitmap =
    baked(listOf("glass-edge-2", w, h, radius, rim, bottomCorners, light, edge)) {
        val bitmap = createBitmap(w, h)
        val canvas = android.graphics.Canvas(bitmap)
        // A sheet's rim runs off the bottom with its corners.
        val bottom = if (bottomCorners) h.toFloat() else h + radius
        val shine = (if (light) Glisse.SHINE_LIGHT else Glisse.SHINE_DARK) * 0.6f
        fun white(alpha: Float) = Color.White.copy(alpha = alpha).toArgb()
        if (edge > 0f) canvas.drawPath(
            smoothRectPath(Rect(edge / 2, edge / 2, w - edge / 2, bottom - edge / 2), radius - edge / 2).asAndroidPath(),
            Paint(Paint.ANTI_ALIAS_FLAG).apply {
                style = Paint.Style.STROKE
                strokeWidth = edge
                color = if (light) Color.Black.copy(alpha = Glisse.EDGE_ALPHA).toArgb() else white(Glisse.EDGE_ALPHA)
            },
        )
        // Around the centre: full shine on every side, none at the top-right and bottom-left
        // corners, fading over each corner's curve.
        val cx = w / 2f
        val cy = bottom / 2f
        fun angle(x: Float, y: Float) = ((Math.toDegrees(kotlin.math.atan2((y - cy).toDouble(), (x - cx).toDouble())) + 360) % 360 / 360).toFloat()
        val r = radius.coerceAtMost(minOf(w.toFloat(), bottom) / 2)
        val trStart = angle(w - r, 0f) // the top side meets the corner
        val trMid = angle(w - r * 0.29f, r * 0.29f)
        val trEnd = angle(w.toFloat(), r) // the corner meets the right side
        val blStart = angle(r, bottom)
        val blMid = angle(r * 0.29f, bottom - r * 0.29f)
        val blEnd = angle(0f, bottom - r)
        val on = white(shine)
        // Transparent white, not transparent black: the gradient blends unpremultiplied, and a
        // fade to black would pass through grey and smudge light glass at those corners.
        val off = white(0f)
        // Square corners have no curve to fade over: the rim stays even all round.
        val stops = if (r < 1f) listOf(0f to on, 1f to on)
        else listOf(0f to on, trEnd to on, blStart to on, blMid to off, blEnd to on, trStart to on, trMid to off, 1f to on)
            .let { list -> list.sortedBy { it.first } }
        canvas.drawPath(
            smoothRectPath(Rect(rim / 2, rim / 2, w - rim / 2, bottom - rim / 2), radius - rim / 2).asAndroidPath(),
            Paint(Paint.ANTI_ALIAS_FLAG).apply {
                style = Paint.Style.STROKE
                strokeWidth = rim
                shader = android.graphics.SweepGradient(cx, cy, stops.map { it.second }.toIntArray(), stops.map { it.first }.toFloatArray())
            },
        )
        bitmap.frozen().asImageBitmap()
    }

/**
 * Glass as glisse (github.com/0kzh/glisse) measures iOS 26: its numbers, not ours. Only the
 * numbers are taken, no code. See docs/research/liquid-glass.md.
 */
object Glisse {
    /** The rim's width and the outline under it, in dp (glisse's 1.22 px and 0.5 px). */
    const val RIM = 1.22f
    const val EDGE = 0.5f
    /** How bright the rim is: 0.13 on dark glass, 0.3 on light. */
    const val SHINE_DARK = 0.13f
    const val SHINE_LIGHT = 0.3f
    /** The outline: white at 8% on dark glass, black at 8% on light. */
    const val EDGE_ALPHA = 0.08f
    /** What lies behind is saturated 1.5 times before it is tinted. */
    const val SATURATE = 1.5f
    /** Dark glass: rgb(40 40 40 / 0.62) over the picture. */
    const val DARK_KEEP = 0.38f
    const val DARK_ADD = 40f * 0.62f
    /**
     * Light glass, kept clear: a soft white at 0.52 over the picture (glisse uses white at 0.6), so
     * more of the picture shows and it is less bright than a white sheet, while dark text still
     * reads on a night sky: over black it comes out at 118, about 3.6:1 against #1D1D1F.
     */
    const val LIGHT_KEEP = 0.48f
    const val LIGHT_ADD = 226f * 0.52f
    /**
     * Fork: light glass made for its picture (lightPanelTint): 58% of the picture shows; white brings
     * the panel's average to 204, never less than 96 nor more than 150 white added. Over black it
     * stays at least 96 + the white cards' lift: dark text sits in the white groups, not on the glass.
     */
    const val LIGHT_CLEAR = 0.58f
    const val LIGHT_TARGET = 204f
    const val LIGHT_ADD_MIN = 96f
    const val LIGHT_ADD_MAX = 150f
    /**
     * Fork: the dock's glass (84% of the picture), which the pill's focus matches, with a little
     * more white lift (44 against the dock's 16): the focus must read as lit, the dock as a surface.
     */
    const val CLEAR_KEEP = 0.84f
    const val CLEAR_ADD = 44f
    /**
     * Glisse's pointer press spring: stiffness and damping ratio (its 29.2 damping). Its release
     * bounce (230, 0.78) isn't used: an overshoot on focus made the focus ring shimmer.
     */
    const val PRESS_STIFFNESS = 417.6f
    const val PRESS_DAMPING = 0.714f
}

/** [keep] of the picture, saturated by [Glisse.SATURATE], plus [add] of the glass's own color. */
private fun glassTint(keep: Float, add: Float, addBlue: Float = add): ColorFilter {
    val s = Glisse.SATURATE
    val lr = 0.2126f * (1 - s); val lg = 0.7152f * (1 - s); val lb = 0.0722f * (1 - s)
    return ColorFilter.colorMatrix(ColorMatrix(floatArrayOf(
        keep * (lr + s), keep * lg, keep * lb, 0f, add,
        keep * lr, keep * (lg + s), keep * lb, 0f, add,
        keep * lr, keep * lg, keep * (lb + s), 0f, addBlue,
        0f, 0f, 0f, 1f, 0f,
    )))
}

/** Panels: the frosted picture at half its light, lifted a little, so white text reads on snow. */
/** Lite's panel color for [frost]: its average color, dimmed as [PanelTint] dims the glass. Made once per picture. */
private fun liteGlass(frost: ImageBitmap, light: Boolean): Color = synchronized(liteColors) {
    liteColors.getOrPut(frost to light) {
        val small = android.graphics.Bitmap.createScaledBitmap(frost.asAndroidBitmap(), 16, 9, true)
        var r = 0f; var g = 0f; var b = 0f
        for (y in 0 until 9) for (x in 0 until 16) {
            val c = small.getPixel(x, y)
            r += android.graphics.Color.red(c); g += android.graphics.Color.green(c); b += android.graphics.Color.blue(c)
        }
        // Light: as lightPanelTint makes it, the white brought in to lift the average to its target.
        val luma = (0.2126f * r + 0.7152f * g + 0.0722f * b) / 144f
        val lightAdd = (Glisse.LIGHT_TARGET - Glisse.LIGHT_CLEAR * luma).coerceIn(Glisse.LIGHT_ADD_MIN, Glisse.LIGHT_ADD_MAX)
        fun dim(sum: Float) = ((sum / 144f) * (if (light) Glisse.LIGHT_CLEAR else Glisse.DARK_KEEP) + (if (light) lightAdd else Glisse.DARK_ADD)).coerceIn(0f, 255f) / 255f
        Color(dim(r), dim(g), dim(b))
    }
}

private val liteColors = HashMap<Pair<ImageBitmap, Boolean>, Color>()

/** Fork: a light panel, the frosted picture under clear white glass, so dark text reads on night skies. */
private val LightPanelTint = glassTint(Glisse.LIGHT_KEEP, Glisse.LIGHT_ADD, Glisse.LIGHT_ADD + 3f)

/**
 * Fork: light glass made for [frost]: [Glisse.LIGHT_CLEAR] of the picture shows, and the white
 * added brings the panel's average to [Glisse.LIGHT_TARGET], so a dark wallpaper gets more white
 * and a bright one less. With one fixed amount (52% white) the light panel was a milky sheet over
 * a bright picture and the glass barely showed (the owner, SHIELD); the white is held between
 * [Glisse.LIGHT_ADD_MIN] and [Glisse.LIGHT_ADD_MAX] so dark text still reads. Made once per picture.
 */
private fun lightPanelTint(frost: ImageBitmap): ColorFilter = synchronized(lightTints) {
    lightTints.getOrPut(frost) {
        val avg = averageLuma(frost)
        val add = (Glisse.LIGHT_TARGET - Glisse.LIGHT_CLEAR * avg).coerceIn(Glisse.LIGHT_ADD_MIN, Glisse.LIGHT_ADD_MAX)
        glassTint(Glisse.LIGHT_CLEAR, add, add + 3f)
    }
}

private val lightTints = java.util.WeakHashMap<ImageBitmap, ColorFilter>()

/** The picture's average brightness, 0-255, after the glass's saturation (which keeps luma). */
private fun averageLuma(frost: ImageBitmap): Float {
    val small = android.graphics.Bitmap.createScaledBitmap(frost.asAndroidBitmap(), 16, 9, true)
    var sum = 0f
    for (y in 0 until 9) for (x in 0 until 16) {
        val c = small.getPixel(x, y)
        sum += 0.2126f * android.graphics.Color.red(c) + 0.7152f * android.graphics.Color.green(c) + 0.0722f * android.graphics.Color.blue(c)
    }
    return sum / 144f
}

/** Fork: the dock's glass (HomeChrome's PlateTint): most of the picture, saturated, a little lifted. */
private val ClearTint = glassTint(Glisse.CLEAR_KEEP, Glisse.CLEAR_ADD)

/** Lite's clear glass for [frost]: its average color as [ClearTint] would leave it. Made once per picture. */
private fun liteClear(frost: ImageBitmap): Color = synchronized(liteClears) { liteClears.getOrPut(frost) { liteClearOf(frost) } }

private val liteClears = java.util.WeakHashMap<ImageBitmap, Color>()

private fun liteClearOf(frost: ImageBitmap): Color {
    val small = android.graphics.Bitmap.createScaledBitmap(frost.asAndroidBitmap(), 16, 9, true)
    var r = 0f; var g = 0f; var b = 0f
    for (y in 0 until 9) for (x in 0 until 16) {
        val c = small.getPixel(x, y)
        r += android.graphics.Color.red(c); g += android.graphics.Color.green(c); b += android.graphics.Color.blue(c)
    }
    fun v(sum: Float) = ((sum / 144f) * Glisse.CLEAR_KEEP + Glisse.CLEAR_ADD).coerceIn(0f, 255f) / 255f
    return Color(v(r), v(g), v(b))
}

private val PanelTint = glassTint(Glisse.DARK_KEEP, Glisse.DARK_ADD)

package app.orchard.tv.ui.components

import android.graphics.BitmapShader
import android.graphics.RuntimeShader
import android.graphics.Shader
import android.os.Build
import androidx.annotation.RequiresApi
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.ImageBitmap
import androidx.compose.ui.graphics.asAndroidBitmap
import androidx.compose.ui.unit.IntSize

/**
 * Fork: Liquid Glass as iOS 26 draws it, on Android 13 and later (AGSL). Inside, the panel is the
 * frosted wallpaper as before, so text reads; toward the edge the sharp wallpaper takes over, bent
 * inward as a thick lens bends what lies behind its rim, with red and blue parting a little there
 * and light catching the top-left edge and, fainter, the bottom-right.
 *
 * It bends the wallpaper only, not the tiles under a panel: catching what is drawn under the
 * panel every frame is more than these TVs' GPUs can spare. The glass is drawn into a layer the
 * GPU keeps (liquidGlass), so the shader runs only when the panel moves or the picture changes.
 */
object LiquidLens {
    /** Whether this TV can draw it: AGSL needs Android 13. */
    val supported: Boolean get() = Build.VERSION.SDK_INT >= 33

    /**
     * The glass for a panel of [size] px at [at] in a root of [root] px, with corners of [radius]
     * px ([openBottom]: a sheet whose bottom runs off the screen). [frost] is the frosted wallpaper,
     * [sharp] the wallpaper itself (or null: the frosted one bends instead). [light] for a light
     * panel. [dp] is pixels per dp. [bandPx] is how far in the lens bends, less on a small control;
     * [dock]: the milkier tint of the plate under the top row.
     */
    @RequiresApi(33)
    fun shader(
        frost: ImageBitmap, sharp: ImageBitmap?, size: IntSize, radius: Float, openBottom: Boolean,
        at: Offset, root: IntSize, light: Boolean, dp: Float, bandPx: Float = BAND_DP * dp, dock: Boolean = false,
    ): Shader = RuntimeShader(SOURCE).apply {
        setInputShader("frost", BitmapShader(frost.asAndroidBitmap(), Shader.TileMode.CLAMP, Shader.TileMode.CLAMP))
        val image = sharp ?: frost
        setInputShader("sharp", BitmapShader(image.asAndroidBitmap(), Shader.TileMode.CLAMP, Shader.TileMode.CLAMP))
        setFloatUniform("size", size.width.toFloat(), size.height.toFloat())
        setFloatUniform("radius", radius)
        setFloatUniform("openBottom", if (openBottom) 1f else 0f)
        setFloatUniform("origin", at.x, at.y)
        setFloatUniform("frostScale", frost.width / root.width.toFloat(), frost.height / root.height.toFloat())
        setFloatUniform("sharpScale", image.width / root.width.toFloat(), image.height / root.height.toFloat())
        // On a light panel the middle stays milky for dark type, but the rim keeps the picture's
        // color: through the same milk its bending hardly showed, and white light has nothing to
        // catch on white, so the rim also falls into shade behind the highlight.
        when {
            dock -> { setFloatUniform("tintMix", 0.8f, 0.118f); setFloatUniform("rimMix", 0.9f, 0.06f) }
            light -> { setFloatUniform("tintMix", 0.3f, 0.675f); setFloatUniform("rimMix", 0.68f, 0.24f) }
            else -> { setFloatUniform("tintMix", 0.48f, 0.055f); setFloatUniform("rimMix", 0.48f, 0.055f) }
        }
        // Less on a small control, where a heavy shade reads as a bevel, not as glass.
        setFloatUniform("shade", if (light) 0.14f * (bandPx / (BAND_DP * dp)).coerceAtMost(1f).let { it * it } else 0f)
        setFloatUniform("band", bandPx)
        setFloatUniform("dp", dp)
    }

    /** How far in from the edge the lens bends, in dp. */
    private const val BAND_DP = 26f

    private const val SOURCE = """
uniform shader frost;
uniform shader sharp;
uniform float2 size;
uniform float radius;
uniform float openBottom;
uniform float2 origin;
uniform float2 frostScale;
uniform float2 sharpScale;
uniform float2 tintMix;
uniform float2 rimMix;
uniform float shade;
uniform float band;
uniform float dp;

// Signed distance to a rounded rectangle centred on 0 with half-size b: negative inside.
float box(float2 p, float2 b, float r) {
    float2 q = abs(p) - b + r;
    return length(max(q, 0.0)) + min(max(q.x, q.y), 0.0) - r;
}

float edge(float2 p) {
    // A sheet open at the bottom: its outline runs on below the screen, so no rim there.
    float2 b = size * 0.5;
    float2 c = p - b;
    if (openBottom > 0.5) { c.y -= radius * 2.0; b.y += radius * 2.0; }
    return box(c, b, radius);
}

half4 main(float2 p) {
    float d = edge(p);
    float inside = -d;
    float2 n = normalize(float2(edge(p + float2(1.0, 0.0)) - edge(p - float2(1.0, 0.0)),
                                edge(p + float2(0.0, 1.0)) - edge(p - float2(0.0, 1.0))) + 0.00001);
    // 1 at the rim, 0 a band's width in: how much lens there is here.
    float k = clamp(1.0 - inside / band, 0.0, 1.0);
    float2 root = origin + p;
    // A lens's rim shows what lies further in, squeezed toward the edge.
    float2 bent = root - n * k * k * band * 0.55;
    float split = k * k * 1.6 * dp;
    half3 rim = half3(
        sharp.eval((bent - n * split) * sharpScale).r,
        sharp.eval(bent * sharpScale).g,
        sharp.eval((bent + n * split) * sharpScale).b
    );
    half3 inner = frost.eval(root * frostScale).rgb;
    // The panel's tint in the middle, the rim's toward the edge.
    float r = smoothstep(0.0, 1.0, k) * 0.8;
    half3 col = mix(inner * tintMix.x + tintMix.y, rim * rimMix.x + rimMix.y, r);
    col *= 1.0 - shade * k * k;
    // Light from the top left catches the rim; the far edge glints back, fainter.
    float lit = clamp(dot(n, normalize(float2(-0.6, -0.8))), 0.0, 1.0);
    float back = clamp(dot(n, normalize(float2(0.6, 0.8))), 0.0, 1.0);
    col += half3(pow(k, 4.0) * (lit * 0.5 + back * 0.2));
    // A thin bright line right on the edge, the glass's thickness.
    col += half3(smoothstep(1.5 * dp, 0.0, inside) * 0.18);
    return half4(col, 1.0);
}
"""
}

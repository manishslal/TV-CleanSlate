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
     * panel. [dp] is pixels per dp.
     */
    @RequiresApi(33)
    fun shader(
        frost: ImageBitmap, sharp: ImageBitmap?, size: IntSize, radius: Float, openBottom: Boolean,
        at: Offset, root: IntSize, light: Boolean, dp: Float,
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
        setFloatUniform("light", if (light) 1f else 0f)
        setFloatUniform("band", BAND_DP * dp)
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
uniform float light;
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

// The panel tints, as the frosted glass has them: dimmed for dark type's opposite, milky for light.
half3 tint(half3 c) {
    return light > 0.5 ? c * 0.3 + 0.675 : c * 0.48 + 0.055;
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
    half3 col = tint(mix(inner, rim, smoothstep(0.0, 1.0, k) * 0.8));
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

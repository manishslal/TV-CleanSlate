package app.orchard.tv.ui.theme

import android.os.Build
import androidx.compose.animation.core.CubicBezierEasing
import androidx.compose.runtime.Composable
import androidx.compose.runtime.ReadOnlyComposable
import androidx.compose.runtime.compositionLocalOf
import androidx.compose.runtime.staticCompositionLocalOf
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.ExperimentalTextApi
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.font.Font
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontVariation
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.em
import androidx.compose.ui.unit.sp
import app.orchard.tv.R

/**
 * Pixels per dp of Aerial's 960×540 design canvas on this screen: 2 for a 1080p UI, 4 for a TV
 * that draws its UI at 4K. Anything rasterized ahead of time (tile art, wallpapers) is sized from
 * this too, so it stays as sharp as the text drawn live.
 */
fun canvasDensity(metrics: android.util.DisplayMetrics): Float =
    minOf(metrics.widthPixels, metrics.heightPixels * 16 / 9) / 960f

/**
 * Fork: true inside a panel (Control Center, Personalize, menus) while Appearance is light: the
 * panel is light frosted glass with dark type, as iOS draws its sheets in light mode. Home's own
 * type over the wallpaper stays white either way. Provided around the panels (HomeScreen).
 */
val LocalLightPanel = compositionLocalOf { false }

/**
 * tvOS dark appearance. Values follow Apple's system colors for the dark, elevated context.
 * Fork: read in a light panel ([LocalLightPanel]), each gives Apple's light counterpart.
 */
object Palette {
    val Background = Color(0xFF000000)
    val Label: Color @Composable @ReadOnlyComposable get() = if (LocalLightPanel.current) Light.Label else Dark.Label
    val SecondaryLabel: Color @Composable @ReadOnlyComposable get() = if (LocalLightPanel.current) Light.SecondaryLabel else Dark.SecondaryLabel
    val TertiaryLabel: Color @Composable @ReadOnlyComposable get() = if (LocalLightPanel.current) Light.TertiaryLabel else Dark.TertiaryLabel
    val Glass: Color @Composable @ReadOnlyComposable get() = if (LocalLightPanel.current) Light.Glass else Dark.Glass
    val Fill: Color @Composable @ReadOnlyComposable get() = if (LocalLightPanel.current) Light.Fill else Dark.Fill
    val FillStrong: Color @Composable @ReadOnlyComposable get() = if (LocalLightPanel.current) Light.FillStrong else Dark.FillStrong
    val Hairline: Color @Composable @ReadOnlyComposable get() = if (LocalLightPanel.current) Light.Hairline else Dark.Hairline
    val Green = Color(0xFF30D158)
    val Orange = Color(0xFFFF9F0A)
    val Red = Color(0xFFFF453A)

    /**
     * tvOS draws a focused control as a near-white plate with dark content. Fork: on a light
     * panel a near-white plate would hardly stand out, so it turns over: dark plate, white content.
     */
    val Focus: Color @Composable @ReadOnlyComposable get() = if (LocalLightPanel.current) Light.Focus else Dark.Focus
    val OnFocus: Color @Composable @ReadOnlyComposable get() = if (LocalLightPanel.current) Light.OnFocus else Dark.OnFocus

    /** The dark values, for drawing outside composition. */
    object Dark {
        val Label = Color(0xFFFFFFFF)
        val SecondaryLabel = Color(0x99EBEBF5)
        val TertiaryLabel = Color(0x4DEBEBF5)
        val Glass = Color(0xE6202022)
        val Fill = Color(0x2E787880)
        val FillStrong = Color(0x52787880)
        val Hairline = Color(0x1AFFFFFF)
        val Focus = Color(0xFFF5F5F7)
        val OnFocus = Color(0xFF1D1D1F)
    }

    /** Fork: Apple's light system colors, for a light panel. */
    object Light {
        val Label = Color(0xFF1D1D1F)
        val SecondaryLabel = Color(0x993C3C43)
        val TertiaryLabel = Color(0x4D3C3C43)
        val Glass = Color(0xEBF2F2F7)
        val Fill = Color(0x29787880)
        val FillStrong = Color(0x3D787880)
        val Hairline = Color(0x1F000000)
        val Focus = Color(0xFF1D1D1F)
        val OnFocus = Color(0xFFFFFFFF)
    }
}

object Motion {
    /** Strong ease-out: entrances, anything that answers the user. */
    val EaseOut = CubicBezierEasing(0.23f, 1f, 0.32f, 1f)

    /** On-screen movement between two resting places. */
    val EaseInOut = CubicBezierEasing(0.77f, 0f, 0.175f, 1f)

    /** iOS sheet curve, for panels sliding in. */
    val Drawer = CubicBezierEasing(0.32f, 0.72f, 0f, 1f)
}

/** True when the TV's animator scale is 0 ("remove animations"). Motion collapses to fades. */
val LocalReducedMotion = staticCompositionLocalOf { false }

// Inter has an optical size axis. Text sizes get the 14pt master, display sizes the 32pt master,
// the way SF Pro switches between Text and Display. Font variations need Android 8.0; before that
// every weight would come out as Inter Regular, so those TVs use the system's Roboto instead.
@OptIn(ExperimentalTextApi::class)
private fun inter(opticalSize: Float) = if (Build.VERSION.SDK_INT < 26) FontFamily.SansSerif else FontFamily(
    (1..9).map { i ->
        val w = i * 100
        Font(
            R.font.inter,
            FontWeight(w),
            variationSettings = FontVariation.Settings(
                FontVariation.weight(w),
                FontVariation.Setting("opsz", opticalSize),
            ),
        )
    },
)

val InterText = inter(14f)
val InterDisplay = inter(32f)

object Type {
    private val ClockBase = TextStyle(
        fontFamily = InterDisplay, fontWeight = FontWeight.W700,
        fontSize = 104.sp, letterSpacing = (-0.04).em,
    )
    val Clock: TextStyle @Composable @ReadOnlyComposable get() = ClockBase.copy(color = Palette.Label)

    /** Over and under the large clock: the date, and the weather. */
    private val ClockDateBase = TextStyle(
        fontFamily = InterDisplay, fontWeight = FontWeight.W600,
        fontSize = 19.sp, letterSpacing = (-0.005).em,
    )
    val ClockDate: TextStyle @Composable @ReadOnlyComposable get() = ClockDateBase.copy(color = Palette.Label)

    private val ClockDetailBase = TextStyle(
        fontFamily = InterText, fontWeight = FontWeight.W600,
        fontSize = 16.sp,
    )
    val ClockDetail: TextStyle @Composable @ReadOnlyComposable get() = ClockDetailBase.copy(color = Palette.Label)

    /**
     * The smaller clocks (Home's small clock, Control Center, the screensaver). Display cuts and
     * negative tracking made their digits run together at these sizes (#494), so they take the
     * text cut and open tracking. Size is set where used.
     */
    private val SmallClockBase = TextStyle(
        fontFamily = InterText, fontWeight = FontWeight.W600,
        letterSpacing = 0.05.em,
    )
    val SmallClock: TextStyle @Composable @ReadOnlyComposable get() = SmallClockBase.copy(color = Palette.Label)

    private val LargeTitleBase = TextStyle(
        fontFamily = InterDisplay, fontWeight = FontWeight.W600,
        fontSize = 26.sp, letterSpacing = (-0.015).em,
    )
    val LargeTitle: TextStyle @Composable @ReadOnlyComposable get() = LargeTitleBase.copy(color = Palette.Label)

    private val TitleBase = TextStyle(
        fontFamily = InterDisplay, fontWeight = FontWeight.W600,
        fontSize = 19.sp, letterSpacing = (-0.01).em,
    )
    val Title: TextStyle @Composable @ReadOnlyComposable get() = TitleBase.copy(color = Palette.Label)

    private val HeadlineBase = TextStyle(
        fontFamily = InterText, fontWeight = FontWeight.W600,
        fontSize = 15.sp, letterSpacing = (-0.005).em,
    )
    val Headline: TextStyle @Composable @ReadOnlyComposable get() = HeadlineBase.copy(color = Palette.Label)

    private val BodyBase = TextStyle(
        fontFamily = InterText, fontWeight = FontWeight.W500,
        fontSize = 14.sp,
    )
    val Body: TextStyle @Composable @ReadOnlyComposable get() = BodyBase.copy(color = Palette.Label)

    private val CalloutBase = TextStyle(
        fontFamily = InterText, fontWeight = FontWeight.W500,
        fontSize = 13.sp,
    )
    val Callout: TextStyle @Composable @ReadOnlyComposable get() = CalloutBase.copy(color = Palette.SecondaryLabel)

    private val CaptionBase = TextStyle(
        fontFamily = InterText, fontWeight = FontWeight.W500,
        fontSize = 11.sp, letterSpacing = 0.01.em,
    )
    val Caption: TextStyle @Composable @ReadOnlyComposable get() = CaptionBase.copy(color = Palette.SecondaryLabel)

    /** Fork: the styles as Home draws them over the wallpaper, white, for use outside composition. */
    object OnWallpaper {
        val Clock: TextStyle get() = ClockBase.copy(color = Palette.Dark.Label)
        val ClockDate: TextStyle get() = ClockDateBase.copy(color = Palette.Dark.Label)
        val ClockDetail: TextStyle get() = ClockDetailBase.copy(color = Palette.Dark.Label)
        val SmallClock: TextStyle get() = SmallClockBase.copy(color = Palette.Dark.Label)
        val LargeTitle: TextStyle get() = LargeTitleBase.copy(color = Palette.Dark.Label)
        val Title: TextStyle get() = TitleBase.copy(color = Palette.Dark.Label)
        val Headline: TextStyle get() = HeadlineBase.copy(color = Palette.Dark.Label)
        val Body: TextStyle get() = BodyBase.copy(color = Palette.Dark.Label)
        val Callout: TextStyle get() = CalloutBase.copy(color = Palette.Dark.SecondaryLabel)
        val Caption: TextStyle get() = CaptionBase.copy(color = Palette.Dark.SecondaryLabel)
    }
}

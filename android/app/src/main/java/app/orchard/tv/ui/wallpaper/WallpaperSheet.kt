package app.orchard.tv.ui.wallpaper

import android.view.KeyEvent as NativeKeyEvent
import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.foundation.layout.RowScope
import androidx.compose.foundation.layout.aspectRatio
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.lazy.grid.GridCells
import androidx.compose.foundation.lazy.grid.GridItemSpan
import androidx.compose.foundation.lazy.grid.LazyVerticalGrid
import androidx.compose.foundation.lazy.grid.items
import androidx.compose.runtime.CompositionLocalProvider
import androidx.compose.runtime.SideEffect
import androidx.compose.runtime.staticCompositionLocalOf
import androidx.compose.ui.geometry.CornerRadius
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.luminance
import androidx.compose.ui.input.key.KeyEvent
import androidx.compose.ui.input.key.KeyEventType
import androidx.compose.ui.input.key.type
import androidx.compose.ui.text.font.FontWeight
import app.orchard.tv.apps.AppEntry
import app.orchard.tv.ui.home.MiniTile
import androidx.compose.foundation.text.BasicTextField
import androidx.compose.foundation.text.KeyboardActions
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.ui.focus.focusRequester
import androidx.compose.ui.graphics.SolidColor
import androidx.compose.ui.platform.LocalSoftwareKeyboardController
import androidx.compose.ui.text.input.ImeAction
import kotlinx.coroutines.delay
import app.orchard.tv.plex.Plex
import android.os.Build
import androidx.activity.compose.BackHandler
import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.animateColorAsState
import androidx.compose.animation.core.tween
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.animation.slideInVertically
import androidx.compose.animation.slideOutVertically
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.ColumnScope
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxHeight
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.widthIn
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.offset
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.BasicText
import androidx.compose.foundation.verticalScroll
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.produceState
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.runtime.withFrameNanos
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.focus.focusRequester
import androidx.compose.foundation.focusGroup
import androidx.compose.foundation.focusable
import app.orchard.tv.ui.components.SidePanel
import app.orchard.tv.ui.home.MenuRow
import androidx.compose.ui.draw.clip
import androidx.compose.ui.focus.FocusRequester
import androidx.compose.ui.focus.onFocusChanged
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.FilterQuality
import androidx.compose.ui.graphics.ImageBitmap
import androidx.compose.ui.graphics.asImageBitmap
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.IntSize
import androidx.compose.ui.unit.dp
import app.orchard.tv.R
import app.orchard.tv.settings.AerialSettings
import app.orchard.tv.news.NewsTopic
import app.orchard.tv.settings.Appearance
import app.orchard.tv.settings.ClockStyle
import android.content.res.Configuration
import androidx.compose.ui.platform.LocalConfiguration
import androidx.compose.ui.semantics.Role
import app.orchard.tv.settings.FocusStyle
import app.orchard.tv.settings.TileStyle
import app.orchard.tv.settings.UiSounds
import app.orchard.tv.settings.Effects
import app.orchard.tv.system.Sounds
import app.orchard.tv.ui.components.Pressable
import app.orchard.tv.ui.components.consumeTaps
import app.orchard.tv.ui.components.dismissOnTap
import app.orchard.tv.ui.components.glassRim
import app.orchard.tv.ui.components.liquidGlass
import app.orchard.tv.ui.theme.Glyph
import app.orchard.tv.ui.theme.GlyphIcon
import app.orchard.tv.ui.theme.Motion
import app.orchard.tv.ui.theme.Palette
import app.orchard.tv.ui.theme.Type
import app.orchard.tv.wallpaper.PhoneServer
import app.orchard.tv.wallpaper.AerialFormats
import app.orchard.tv.wallpaper.AerialQuality
import app.orchard.tv.wallpaper.ScreensaverMode
import app.orchard.tv.system.SystemScreensaver
import app.orchard.tv.wallpaper.Wallpaper
import app.orchard.tv.wallpaper.WallpaperKind
import app.orchard.tv.wallpaper.Wallpapers
import app.orchard.tv.weather.Weather
import app.orchard.tv.weather.WeatherKind
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import kotlin.math.roundToInt

/**
 * Personalize, chosen on the TV. Fork: a panel docked on the right (SidePanel), a grouped list that
 * leads to a page for each subject; Home stays in view on the left, so a new wallpaper can be seen
 * the moment it is chosen. The last page's QR code opens the same settings on a phone, where
 * sending photos is easy.
 *
 * Every setting is one line and one focus stop: a switch for on or off, a value that Left and Right
 * change for up to four choices, and a row that opens a list for more. The focused row's note sits
 * in a fixed place at the panel's foot.
 */
@Composable
fun WallpaperSheet(
    visible: Boolean,
    onDismiss: () -> Unit,
    hiddenApps: List<AppEntry>,
    onUnhide: (String) -> Unit,
    onHiddenApps: (open: () -> Unit) -> Unit,
    onStartScreensaver: () -> Unit,
    onPin: (on: Boolean) -> Unit,
) {
    val context = LocalContext.current
    var phoneUrl by remember { mutableStateOf<String?>(null) }
    var qr by remember { mutableStateOf<ImageBitmap?>(null) }


    // The phone page listens all along (PhoneServer.listen); the address is read again each time,
    // as the TV's may have changed.
    LaunchedEffect(visible) {
        if (!visible) return@LaunchedEffect
        val url = withContext(Dispatchers.IO) { PhoneServer.start(context) }
        phoneUrl = url
        qr = url?.let { withContext(Dispatchers.Default) { PhoneServer.qr(it).asImageBitmap() } }
    }

    AnimatedVisibility(visible = visible, enter = fadeIn(tween(Motion.Enter, easing = Motion.EaseOut)), exit = fadeOut(tween(Motion.Quick, easing = Motion.EaseOut)), modifier = Modifier.fillMaxSize()) {
        // A tap above the sheet closes it, back to Control Center as Back does.
        Box(Modifier.fillMaxSize().dismissOnTap(onDismiss).background(Color.Black.copy(alpha = 0.25f)))
    }

    val panel = remember { PanelState() }
    // Back leaves a list for its page, a page for the grouped list, then Personalize.
    var page by remember { mutableStateOf(PersonalizePage.Root) }
    var cameFrom by remember { mutableStateOf<PersonalizePage?>(null) }
    val back = {
        when {
            panel.list != null -> panel.list = null
            page == PersonalizePage.Root -> onDismiss()
            else -> { cameFrom = page; page = PersonalizePage.Root }
        }
    }
    BackHandler(enabled = visible, onBack = back)

    SidePanel(visible = visible) {
        CompositionLocalProvider(LocalPanel provides panel) {
            val scroll = rememberScrollState()
            // What a page opens on: its first control, or the row a page was left from.
            val pageStart = remember { FocusRequester() }
            val list = panel.list
            LaunchedEffect(page, list) {
                panel.rowNote = null
                panel.pageNote = null
                scroll.scrollTo(0)
                withFrameNanos { }
                runCatching { pageStart.requestFocus() }
            }
            BasicText(stringResource(list?.label ?: page.title), style = Type.Title, modifier = Modifier.padding(start = 4.dp))
            Spacer(Modifier.height(12.dp))
            val body = Modifier.weight(1f).fillMaxWidth()
            when {
                list != null -> Column(body.verticalScroll(scroll).focusRequester(pageStart).focusGroup()) { ChoiceList(list) }
                page == PersonalizePage.Wallpaper -> WallpaperGrid(body.focusRequester(pageStart).focusGroup())
                page == PersonalizePage.Hidden -> HiddenGrid(hiddenApps, onUnhide, body.focusRequester(pageStart).focusGroup())
                else -> Column(body.verticalScroll(scroll).focusRequester(pageStart).focusGroup()) {
                    when (page) {
                        PersonalizePage.Root -> RootPage(hiddenApps.size, cameFrom) { to ->
                            if (to == PersonalizePage.Hidden) onHiddenApps { page = to } else page = to
                        }
                        PersonalizePage.Layout -> LayoutPage()
                        PersonalizePage.Appearance -> AppearancePage()
                        PersonalizePage.Clock -> ClockPage()
                        PersonalizePage.Screensaver -> ScreensaverPage(onStartScreensaver)
                        PersonalizePage.Headlines -> HeadlinesPage()
                        PersonalizePage.Plex -> PlexPage()
                        PersonalizePage.Passcode -> PasscodePage(onPin)
                        PersonalizePage.Phone -> PhoneCard(phoneUrl, qr)
                        PersonalizePage.Wallpaper, PersonalizePage.Hidden -> Unit
                    }
                }
            }
            if (page != PersonalizePage.Root && page != PersonalizePage.Phone && list == null) NoteFooter()
        }
    }
}

/** Fork: the panel's foot and the list a row has opened, shared by the rows of a page. */
private class PanelState {
    var rowNote by mutableStateOf<String?>(null)
    var pageNote by mutableStateOf<String?>(null)
    var list by mutableStateOf<OpenList?>(null)
    var returnTo by mutableStateOf<Any?>(null)
}

private val LocalPanel = staticCompositionLocalOf { PanelState() }

/** The choices a [ListRow] opened; [returnTo][PanelState.returnTo] takes focus back to that row. */
private class OpenList(val label: Int, val options: List<Pair<Any?, String>>, val current: Any?, val choose: (Any?) -> Unit)

/** Fork: the list Personalize opens on, and the page each row leads to, in their groups. */
private enum class PersonalizeGroup(val title: Int) {
    Look(R.string.fk_group_look),
    Home(R.string.st_home_layout),
    Access(R.string.fk_group_access),
}

private enum class PersonalizePage(val title: Int, val glyph: Glyph, val group: PersonalizeGroup?) {
    Root(R.string.cc_personalize, Glyph.Photo, null),
    Wallpaper(R.string.wp_wallpaper, Glyph.Photo, PersonalizeGroup.Look),
    Appearance(R.string.fk_tiles, Glyph.Sun, PersonalizeGroup.Look),
    Screensaver(R.string.wp_screensaver, Glyph.Moon, PersonalizeGroup.Look),
    Layout(R.string.fk_page_layout, Glyph.Home, PersonalizeGroup.Home),
    Clock(R.string.fk_page_clock, Glyph.Calendar, PersonalizeGroup.Home),
    Headlines(R.string.fk_headlines, Glyph.Globe, PersonalizeGroup.Home),
    Hidden(R.string.st_hidden_apps, Glyph.EyeSlash, PersonalizeGroup.Access),
    Passcode(R.string.st_pin, Glyph.Shield, PersonalizeGroup.Access),
    Plex(R.string.fk_plex, Glyph.Play, PersonalizeGroup.Access),
    Phone(R.string.wp_phone_title, Glyph.Cast, PersonalizeGroup.Access),
}

/** What a page is set to, shown on its row so it need not be opened to find out. */
@Composable
private fun pageValue(page: PersonalizePage, hiddenCount: Int): String? = when (page) {
    PersonalizePage.Wallpaper -> when (Wallpapers.selectedId) {
        Wallpapers.VIDEOS -> stringResource(R.string.wp_videos_rotate)
        Wallpapers.LATEST_DAILY -> stringResource(R.string.wp_daily_latest)
        Wallpapers.ROTATE_PHOTOS -> stringResource(R.string.wp_photos_rotate)
        else -> Wallpapers.items.firstOrNull { it.id == Wallpapers.selectedId }?.title
    }
    PersonalizePage.Appearance -> stringResource(
        when (AerialSettings.appearance) {
            Appearance.Dark -> R.string.fk_tiles_dark
            Appearance.Light -> R.string.fk_tiles_color
            Appearance.Auto -> R.string.fk_tiles_auto
        },
    )
    PersonalizePage.Screensaver -> stringResource(screensaverName(Wallpapers.screensaver))
    PersonalizePage.Layout -> stringResource(if (AerialSettings.oneRowHome) R.string.st_home_one_row else R.string.st_home_standard) + ", ${AerialSettings.homeColumns}"
    PersonalizePage.Clock -> stringResource(if (AerialSettings.largeClock) R.string.st_clock_large else R.string.st_clock_small)
    PersonalizePage.Headlines -> stringResource(if (AerialSettings.headlines) R.string.st_on else R.string.st_off)
    PersonalizePage.Hidden -> hiddenCount.takeIf { it > 0 }?.toString()
    PersonalizePage.Passcode -> stringResource(if (AerialSettings.pinSet) R.string.st_pin_on else R.string.st_pin_off)
    PersonalizePage.Plex -> if (Plex.linked) stringResource(R.string.fk_plex_linked) else null
    else -> null
}

private fun screensaverName(mode: ScreensaverMode) = when (mode) {
    ScreensaverMode.Aerial -> R.string.ss_aerial
    ScreensaverMode.Photos -> R.string.ss_photos
    ScreensaverMode.Clock -> R.string.ss_clock
    ScreensaverMode.Videos -> R.string.ss_videos
    ScreensaverMode.System -> R.string.ss_system
}

@Composable
private fun ColumnScope.RootPage(hiddenCount: Int, cameFrom: PersonalizePage?, open: (PersonalizePage) -> Unit) {
    PersonalizeGroup.entries.forEach { group ->
        GroupTitle(group.title)
        PersonalizePage.entries.filter { it.group == group }.forEach { to ->
            val row = remember { FocusRequester() }
            LaunchedEffect(Unit) {
                if (to == cameFrom) { withFrameNanos { }; withFrameNanos { }; runCatching { row.requestFocus() } }
            }
            MenuRow(
                to.glyph, stringResource(to.title),
                onClick = { open(to) },
                focusRequester = row,
                trailing = (pageValue(to, hiddenCount)?.let { "$it " } ?: "") + "›",
            )
        }
    }
}

@Composable
private fun LayoutPage() {
    Group(R.string.st_layout) {
        ValueRow(
            R.string.fk_label_rows, stringResource(R.string.st_home_layout_hint),
            listOf(true to R.string.st_home_one_row, false to R.string.st_home_standard),
            AerialSettings.oneRowHome, AerialSettings::chooseOneRowHome,
        )
        ValueRow(
            R.string.st_columns, stringResource(R.string.st_columns_hint),
            AerialSettings.HOME_COLUMNS.map { it to it.toString() },
            AerialSettings.homeColumns, AerialSettings::chooseHomeColumns,
        )
        SwitchRow(R.string.fk_label_plate, stringResource(R.string.st_plate_hint), AerialSettings.homePlate, AerialSettings::chooseHomePlate)
        // Two rows show the second row whole; only one row has a peek to hide.
        if (AerialSettings.oneRowHome) SwitchRow(R.string.fk_label_peek, stringResource(R.string.st_peek_hint), AerialSettings.peekNextRow, AerialSettings::choosePeekNextRow)
    }
    Group(R.string.fk_sec_tiles) {
        ValueRow(
            R.string.st_names, stringResource(R.string.st_names_hint),
            listOf(false to R.string.st_names_focused, true to R.string.st_names_all),
            AerialSettings.showAllNames, AerialSettings::chooseShowAllNames,
        )
    }
}

@Composable
private fun AppearancePage() {
    Group(R.string.fk_sec_theme) {
        ValueRow(
            R.string.fk_label_mode, stringResource(R.string.fk_tiles_hint),
            listOf(Appearance.Dark to R.string.fk_tiles_dark, Appearance.Light to R.string.fk_tiles_color, Appearance.Auto to R.string.fk_tiles_auto),
            AerialSettings.appearance, AerialSettings::chooseAppearance,
        )
        ValueRow(
            R.string.fk_label_tile_style, stringResource(R.string.fk_tile_style_hint),
            listOf(TileStyle.System to R.string.fk_tiles_system, TileStyle.Glass to R.string.fk_tiles_glass, TileStyle.Full to R.string.fk_tiles_full),
            AerialSettings.tileStyle, AerialSettings::chooseTileStyle,
        )
        // Fork: only where the TV can draw Liquid Glass (Android 13 and later).
        if (app.orchard.tv.ui.components.LiquidLens.supported && !AerialSettings.lite) ValueRow(
            R.string.fk_glass, stringResource(R.string.fk_glass_hint),
            listOf(true to R.string.fk_glass_liquid, false to R.string.fk_glass_frosted),
            AerialSettings.liquidGlass, AerialSettings::chooseLiquidGlass,
        )
        ValueRow(
            R.string.st_focus_style,
            stringResource(if (AerialSettings.focusStyle == FocusStyle.HighContrast) R.string.st_focus_high_hint else R.string.st_focus_standard_hint),
            listOf(FocusStyle.HighContrast to R.string.st_focus_high, FocusStyle.Standard to R.string.st_focus_standard),
            AerialSettings.focusStyle, AerialSettings::chooseFocusStyle,
        )
    }
    Group(R.string.fk_sec_motion) {
        ValueRow(
            R.string.st_sounds,
            stringResource(if (AerialSettings.uiSounds == UiSounds.Tv) R.string.st_sounds_tv_hint else R.string.st_sounds_hint),
            listOf(UiSounds.Tv to R.string.st_sounds_tv, UiSounds.Aerial to R.string.st_sounds_aerial, UiSounds.Off to R.string.st_off),
            AerialSettings.uiSounds, { AerialSettings.chooseUiSounds(it); Sounds.prepare() },
        )
        ValueRow(
            R.string.st_effects,
            stringResource(
                if (AerialSettings.effects == Effects.Auto && AerialSettings.lowEnd) R.string.st_effects_auto_lite_hint
                else R.string.st_effects_hint,
            ),
            listOf(Effects.Auto to R.string.st_effects_auto, Effects.Full to R.string.st_effects_full, Effects.Lite to R.string.st_effects_lite),
            AerialSettings.effects, AerialSettings::chooseEffects,
        )
    }
}

@Composable
private fun ClockPage() {
    // Fork: the city is chosen here too, not only from the phone page.
    var pickingPlace by remember { mutableStateOf(false) }
    Group(R.string.st_clock) {
        ValueRow(
            R.string.fk_label_position, stringResource(R.string.st_clock_hint),
            listOf(false to R.string.st_clock_left, true to R.string.st_clock_right),
            AerialSettings.clockRight, AerialSettings::chooseClockRight,
        )
        ValueRow(
            R.string.fk_label_size, stringResource(R.string.st_clock_size_hint),
            listOf(false to R.string.st_clock_small, true to R.string.st_clock_large),
            AerialSettings.largeClock, AerialSettings::chooseLargeClock,
        )
        ValueRow(
            R.string.ss_kind, stringResource(R.string.fk_flip_hint),
            listOf(ClockStyle.Cards to R.string.fk_flip_cards, ClockStyle.Flip to R.string.fk_flip_on, ClockStyle.Plain to R.string.fk_flip_off),
            AerialSettings.clockStyle, AerialSettings::chooseClockStyle,
        )
        ValueRow(
            R.string.fk_time_format, null,
            listOf(true to R.string.fk_time_12, false to R.string.fk_time_24),
            AerialSettings.clock12, AerialSettings::chooseClock12,
        )
        if (AerialSettings.clockStyle == ClockStyle.Cards) SwitchRow(R.string.fk_flap_sound, null, AerialSettings.flapSound, AerialSettings::chooseFlapSound)
    }
    Group(R.string.wp_weather, stringResource(if (Weather.chosen) R.string.fk_weather_chosen else R.string.fk_weather_guessed)) {
        SwitchRow(R.string.fk_show_on_home, null, AerialSettings.showWeather, AerialSettings::chooseShowWeather)
        PanelRow(R.string.wp_weather_city, stringResource(R.string.wp_weather_city), null, onClick = { pickingPlace = !pickingPlace }) { fg, _ ->
            WeatherValue(fg)
            GlyphIcon(Glyph.ChevronRight, fg, 14.dp)
        }
        if (pickingPlace) LocationPicker { pickingPlace = false }
        ValueRow(
            R.string.fk_units, null,
            listOf(true to R.string.fk_units_f, false to R.string.fk_units_c),
            AerialSettings.fahrenheit, AerialSettings::chooseFahrenheit,
        )
    }
}

@Composable
private fun ScreensaverPage(onStart: () -> Unit) {
    val context = LocalContext.current
    Group(
        note = when {
            Wallpapers.screensaver == ScreensaverMode.Videos -> stringResource(R.string.ss_videos_hint)
            Wallpapers.screensaver == ScreensaverMode.System -> stringResource(R.string.ss_system_hint)
            Wallpapers.screensaverFollows -> stringResource(R.string.ss_follow_hint)
            AerialSettings.backStartsScreensaver -> stringResource(R.string.ss_back_hint)
            else -> null
        },
    ) {
        OptionRow(
            R.string.ss_kind, null,
            ScreensaverMode.entries.map { it to screensaverName(it) },
            Wallpapers.screensaver, Wallpapers::chooseScreensaver,
        )
        // The action that tries the choice, right after it.
        PanelRow(R.string.ss_start, stringResource(R.string.ss_start), null, onClick = onStart) { fg, _ -> GlyphIcon(Glyph.Play, fg, 14.dp) }
        // Only the qualities this TV can play; a 1080p-only TV has nothing to choose.
        val qualities = remember { AerialFormats.supported(context) }
        if (qualities.size > 1) ValueRow(
            R.string.ss_quality, stringResource(R.string.ss_quality_hint),
            listOf(AerialQuality.Auto to R.string.ss_quality_auto, AerialQuality.Hd to R.string.ss_quality_hd) +
                listOf(AerialQuality.Uhd to R.string.ss_quality_uhd, AerialQuality.DolbyVision to R.string.ss_quality_dv)
                    .filter { it.first in qualities },
            AerialSettings.aerialQuality, AerialSettings::chooseAerialQuality,
        )
        // The clock screensaver is its clock, and the others show none.
        if (Wallpapers.screensaver == ScreensaverMode.Aerial || Wallpapers.screensaver == ScreensaverMode.Photos) {
            SwitchRow(R.string.ss_show_time, stringResource(R.string.ss_show_time_hint), AerialSettings.screensaverClock, AerialSettings::chooseScreensaverClock)
        }
        // The firmware's screensaver starts on the firmware's timer: Aerial's delay
        // did nothing there, and Xiaomi's 1 minute came before the 5 chosen (issue #675).
        if (Wallpapers.screensaver == ScreensaverMode.System) {
            val delay = remember { SystemScreensaver.firmwareDelayMs(context) }
            val text = when {
                delay == null -> stringResource(R.string.ss_delay_system)
                delay == 0L -> stringResource(R.string.ss_delay_system_off)
                else -> stringResource(R.string.ss_delay_system_minutes, ((delay + 30_000) / 60_000).coerceAtLeast(1).toInt())
            }
            PanelRow(R.string.ss_delay, stringResource(R.string.ss_delay), null, onClick = {}) { fg, _ -> RowValue(text, fg) }
        } else OptionRow(
            R.string.ss_delay, null,
            AerialSettings.screensaverChoices.map {
                it to if (it == AerialSettings.NEVER) stringResource(R.string.ss_never) else stringResource(R.string.ss_minutes, it)
            },
            AerialSettings.screensaverMinutes, AerialSettings::chooseScreensaverMinutes,
        )
        ValueRow(
            R.string.ss_back, stringResource(R.string.ss_back_choice_hint),
            listOf(true to R.string.ss_back_on, false to R.string.ss_back_off),
            AerialSettings.backStartsScreensaver, AerialSettings::chooseBackStartsScreensaver,
        )
    }
}

@Composable
private fun HeadlinesPage() {
    Group {
        SwitchRow(R.string.fk_headlines, stringResource(R.string.fk_headlines_hint), AerialSettings.headlines, AerialSettings::chooseHeadlines)
        // Fork: what the headlines are about, several at once, and their order.
        if (AerialSettings.headlines) ValueRow(
            R.string.fk_news_order, stringResource(R.string.fk_news_order_hint),
            listOf(false to R.string.fk_news_by_topic, true to R.string.fk_news_shuffle),
            AerialSettings.newsShuffle, AerialSettings::chooseNewsShuffle,
        )
    }
    if (AerialSettings.headlines) Group(R.string.fk_news_topics, stringResource(R.string.fk_news_topics_hint)) {
        NewsTopic.entries.forEach { topic ->
            SwitchRow(topic.label, null, topic in AerialSettings.newsTopics, { AerialSettings.toggleNewsTopic(topic) })
        }
    }
}

@Composable
private fun PlexPage() {
    Group(note = Plex.problem?.let { stringResource(it) } ?: stringResource(if (Plex.linked) R.string.fk_plex_linked_hint else R.string.fk_plex_hint)) {
        val code = Plex.linkCode
        val account = when {
            code != null -> stringResource(R.string.fk_plex_code, code)
            Plex.linked -> Plex.serverName ?: stringResource(R.string.fk_plex_linked)
            else -> stringResource(R.string.fk_plex_link)
        }
        PanelRow(R.string.fk_plex_account, stringResource(R.string.fk_plex_account), null, onClick = { if (Plex.linked) Plex.refresh(photosIfStale = false) else Plex.startLink() }) { fg, _ ->
            RowValue(account, fg)
            GlyphIcon(Glyph.ChevronRight, fg, 14.dp)
        }
        if (Plex.linked) {
            val none = "" to stringResource(R.string.fk_plex_none)
            ListRow(
                R.string.fk_plex_videos, null,
                listOf(none) + Plex.sections.filter { it.type == "movie" || it.type == "show" }.map { it.key to it.title },
                Plex.videoSection, Plex::chooseVideoSection,
            )
            ListRow(
                R.string.fk_plex_photos, null,
                listOf(none) + Plex.sections.filter { it.type == "photo" }.map { it.key to it.title },
                Plex.photoSection, Plex::choosePhotoSection,
            )
            PanelRow(R.string.fk_plex_unlink, stringResource(R.string.fk_plex_unlink_action), null, onClick = { Plex.unlink() }) { _, _ -> }
        }
    }
}

/** Parental PIN on or off; either way goes through the PIN pad (issue #10). */
@Composable
private fun PasscodePage(onPin: (on: Boolean) -> Unit) {
    Group(note = stringResource(R.string.st_pin_hint)) {
        SwitchRow(R.string.st_pin, null, AerialSettings.pinSet, { onPin(it) })
    }
}

/** Every wallpaper in a grid, in the order most people want them, grouped by kind. */
@Composable
private fun WallpaperGrid(modifier: Modifier) {
    val items = Wallpapers.items
    val selected = Wallpapers.selectedId
    val first = remember { FocusRequester() }
    val panel = LocalPanel.current
    LaunchedEffect(Unit) {
        withFrameNanos { }
        runCatching { first.requestFocus() }
    }
    val dailyTitle = stringResource(R.string.wp_daily_latest)
    val rotateTitle = stringResource(R.string.wp_photos_rotate)
    val videosTitle = stringResource(R.string.wp_videos_rotate)
    val list = remember(items, selected, dailyTitle, rotateTitle, videosTitle) {
        buildList {
            addAll(items.filter { it.kind == WallpaperKind.Live })
            // Each video on its own, and with two or more one card that plays them in turn (#762).
            val videos = items.filter { it.kind == WallpaperKind.Video && it.id != Wallpapers.VIDEOS }
            if (videos.size > 1 || selected == Wallpapers.VIDEOS) add(Wallpaper(Wallpapers.VIDEOS, WallpaperKind.Video, videosTitle))
            addAll(videos)
            if (items.any { it.kind == WallpaperKind.Daily }) add(Wallpaper(Wallpapers.LATEST_DAILY, WallpaperKind.Daily, dailyTitle))
            addAll(items.filter { it.kind == WallpaperKind.Daily && it.id == selected })
            val photos = items.filter { it.kind == WallpaperKind.Photo }
            // With two or more, one card takes turns through all of them, one a day (#27).
            if (photos.size > 1) add(Wallpaper(Wallpapers.ROTATE_PHOTOS, WallpaperKind.Photo, rotateTitle))
            addAll(photos)
            addAll(items.filter { it.kind == WallpaperKind.Builtin && (it.id in Wallpapers.FEATURED_BUILTINS || it.id == selected) })
            addAll(items.filter { it.kind == WallpaperKind.Auto })
            // The inputs' pictures last: a choice for a home with a set-top box, not the usual one.
            addAll(items.filter { it.kind == WallpaperKind.Source })
        }
    }
    LazyVerticalGrid(
        columns = GridCells.Fixed(2),
        modifier = modifier,
        horizontalArrangement = Arrangement.spacedBy(12.dp),
        verticalArrangement = Arrangement.spacedBy(10.dp),
        contentPadding = PaddingValues(6.dp),
    ) {
        items(list, key = { it.id }) { wp ->
            val hint = wallpaperNote(wp)
            WallpaperCard(wp, if (wp === list.first()) first else null, onFocused = { panel.rowNote = hint })
        }
        // Only for the owner's videos: whether keys stop them. The aerial films always move.
        if (Wallpapers.isVideo()) item(span = { GridItemSpan(maxLineSpan) }) {
            ValueRow(
                R.string.st_play, stringResource(R.string.st_play_hint),
                listOf(false to R.string.st_play_still, true to R.string.st_play_always),
                AerialSettings.wallpaperAlwaysMoves, AerialSettings::chooseWallpaperAlwaysMoves,
            )
        }
    }
}

/** What a wallpaper card is, for the panel's note; null where its picture says it all. */
@Composable
private fun wallpaperNote(wp: Wallpaper): String? = when {
    wp.kind == WallpaperKind.Live -> stringResource(R.string.wp_live)
    wp.id == Wallpapers.VIDEOS -> stringResource(R.string.wp_videos_note)
    wp.kind == WallpaperKind.Video -> stringResource(R.string.wp_video_note)
    wp.kind == WallpaperKind.Source -> stringResource(R.string.wp_source_note)
    wp.id == Wallpapers.LATEST_DAILY -> stringResource(R.string.wp_daily_note)
    wp.id == Wallpapers.ROTATE_PHOTOS -> stringResource(R.string.wp_photos_rotate_note)
    wp.kind == WallpaperKind.Photo -> stringResource(R.string.wp_photo_note)
    wp.kind == WallpaperKind.Auto -> stringResource(R.string.wp_auto_note)
    wp.kind == WallpaperKind.Daily -> wp.caption
    else -> null
}

@Composable
private fun WallpaperCard(wp: Wallpaper, focusRequester: FocusRequester?, onFocused: () -> Unit) {
    val selected = Wallpapers.selectedId == wp.id
    val thumb by produceState<ImageBitmap?>(null, wp.id, Wallpapers.items) {
        value = withContext(Dispatchers.Default) { Wallpapers.thumbnail(wp.id)?.asImageBitmap() }
    }
    Pressable(onClick = { Wallpapers.select(wp.id) }, focusRequester = focusRequester, focusedScale = 1.06f) { state ->
        LaunchedEffect(state.focused) { if (state.focused) onFocused() }
        Column(Modifier.fillMaxWidth()) {
            Box(
                Modifier
                    .fillMaxWidth()
                    .aspectRatio(16f / 9f)
                    .clip(RoundedCornerShape(12.dp))
                    .background(Palette.Fill)
                    .border(
                        if (state.focused || selected) 3.dp else 1.dp,
                        when {
                            state.focused -> Palette.Focus
                            selected -> Palette.Focus.copy(alpha = 0.55f)
                            else -> Palette.Hairline
                        },
                        RoundedCornerShape(12.dp),
                    ),
            ) {
                thumb?.let {
                    Canvas(Modifier.fillMaxSize()) {
                        drawImage(it, dstSize = IntSize(size.width.roundToInt(), size.height.roundToInt()), filterQuality = FilterQuality.Medium)
                    }
                }
                if (selected) {
                    Box(
                        Modifier.align(Alignment.TopEnd).padding(6.dp).size(20.dp).clip(CircleShape).background(Palette.Focus),
                        contentAlignment = Alignment.Center,
                    ) { GlyphIcon(Glyph.Check, Palette.OnFocus, 12.dp) }
                }
                // The aerial wallpaper moves; its card shows the last frame it stopped on.
                if (wp.kind == WallpaperKind.Live || wp.kind == WallpaperKind.Video) {
                    GlyphIcon(Glyph.Play, Color.White, 20.dp, Modifier.align(Alignment.BottomStart).padding(6.dp))
                }
                // An input's picture cannot be copied for a card: its mark over the dark picture.
                if (wp.kind == WallpaperKind.Source) {
                    GlyphIcon(Glyph.Hdmi, Color.White, 30.dp, Modifier.align(Alignment.Center))
                }
            }
            Spacer(Modifier.height(5.dp))
            BasicText(
                wp.title, maxLines = 1, overflow = TextOverflow.Ellipsis,
                style = Type.Caption.copy(color = if (state.focused) Palette.Label else Palette.SecondaryLabel),
            )
        }
    }
}

/** Fork: hidden apps inside the panel, in place of a sheet of their own. OK puts one back on Home. */
@Composable
private fun HiddenGrid(apps: List<AppEntry>, onUnhide: (String) -> Unit, modifier: Modifier) {
    val panel = LocalPanel.current
    val note = stringResource(if (apps.isEmpty()) R.string.hidden_empty else R.string.hidden_hint)
    SideEffect { panel.pageNote = note }
    LazyVerticalGrid(
        columns = GridCells.Fixed(2),
        modifier = modifier,
        horizontalArrangement = Arrangement.spacedBy(12.dp),
        verticalArrangement = Arrangement.spacedBy(10.dp),
        contentPadding = PaddingValues(6.dp),
    ) {
        items(apps, key = { it.key }) { app ->
            Pressable(onClick = { onUnhide(app.key) }, focusedScale = 1.06f) { state ->
                Column {
                    MiniTile(app, state.focused, Modifier.fillMaxWidth().aspectRatio(16f / 9f))
                    Spacer(Modifier.height(5.dp))
                    BasicText(
                        app.label, maxLines = 1, overflow = TextOverflow.Ellipsis,
                        style = Type.Caption.copy(color = if (state.focused) Palette.Label else Palette.SecondaryLabel),
                    )
                }
            }
        }
    }
}

/** The list a [ListRow] opened. Choosing goes back to the row, which then shows the choice. */
@Composable
private fun ChoiceList(list: OpenList) {
    val panel = LocalPanel.current
    Group {
        list.options.forEach { (value, text) ->
            PanelRow(value ?: text, text, null, onClick = { list.choose(value); panel.list = null }) { fg, _ ->
                if (value == list.current) GlyphIcon(Glyph.Check, fg, 16.dp)
            }
        }
    }
}

/** The name over a group of rows. */
@Composable
private fun GroupTitle(title: Int) {
    BasicText(
        stringResource(title),
        style = Type.Callout.copy(color = Palette.Label.copy(alpha = 0.72f), fontWeight = FontWeight.W600),
        modifier = Modifier.padding(start = 14.dp, top = 8.dp, bottom = 5.dp),
    )
}

/** Rows that belong together on one surface; [note] stands at the panel's foot unless a row has its own. */
@Composable
private fun Group(title: Int? = null, note: String? = null, rows: @Composable ColumnScope.() -> Unit) {
    val panel = LocalPanel.current
    if (note != null) SideEffect { panel.pageNote = note }
    if (title != null) GroupTitle(title)
    Column(
        Modifier
            .fillMaxWidth()
            .clip(RoundedCornerShape(16.dp))
            .background(Color.White.copy(alpha = 0.05f))
            .padding(4.dp),
    ) { rows() }
    Spacer(Modifier.height(10.dp))
}

/** The focused row's note, or the page's, in one place at the panel's foot. */
@Composable
private fun NoteFooter() {
    val panel = LocalPanel.current
    Box(Modifier.fillMaxWidth().heightIn(min = 52.dp).padding(start = 14.dp, end = 6.dp, top = 8.dp)) {
        (panel.rowNote ?: panel.pageNote)?.let {
            BasicText(it, style = Type.Caption.copy(color = Palette.Label.copy(alpha = 0.72f)), maxLines = 7, overflow = TextOverflow.Ellipsis)
        }
    }
}

/**
 * One setting on one line, one focus stop: the name on the left, [trailing] on the right. [key]
 * finds the row again when a list it opened closes.
 */
@Composable
private fun PanelRow(
    key: Any,
    label: String,
    hint: String?,
    onClick: () -> Unit,
    onKey: ((KeyEvent) -> Boolean)? = null,
    spokenState: String? = null,
    role: Role = Role.Button,
    trailing: @Composable RowScope.(fg: Color, focused: Boolean) -> Unit,
) {
    val panel = LocalPanel.current
    val requester = remember { FocusRequester() }
    LaunchedEffect(Unit) {
        if (panel.returnTo == key) {
            panel.returnTo = null
            withFrameNanos { }
            withFrameNanos { }
            runCatching { requester.requestFocus() }
        }
    }
    Pressable(onClick = onClick, focusRequester = requester, focusedScale = 1.02f, onKey = onKey, spokenState = spokenState, role = role, modifier = Modifier.fillMaxWidth()) { state ->
        LaunchedEffect(state.focused, hint) { if (state.focused) panel.rowNote = hint }
        val bg by animateColorAsState(if (state.focused) Palette.Focus else Color.Transparent, tween(Motion.Quick, easing = Motion.EaseOut), label = "row-bg")
        val fg = if (state.focused) Palette.OnFocus else Palette.Label
        Row(
            Modifier.fillMaxWidth().clip(RoundedCornerShape(12.dp)).background(bg).height(44.dp).padding(horizontal = 10.dp),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.spacedBy(6.dp),
        ) {
            BasicText(label, style = Type.Row.copy(color = fg), maxLines = 1, overflow = TextOverflow.Ellipsis, modifier = Modifier.weight(1f))
            trailing(fg, state.focused)
        }
    }
}

@Composable
private fun PanelRow(key: Int, label: String, hint: String?, onClick: () -> Unit, trailing: @Composable RowScope.(fg: Color, focused: Boolean) -> Unit) =
    PanelRow(key as Any, label, hint, onClick, trailing = trailing)

/** The value a row is set to. */
@Composable
private fun RowValue(text: String, fg: Color) {
    BasicText(text, style = Type.Callout.copy(color = fg.copy(alpha = 0.72f)), maxLines = 1, overflow = TextOverflow.Ellipsis, modifier = Modifier.widthIn(max = 150.dp))
}

/** On or off: OK flips it. Ink alone says which, as the panels keep color for status. */
@Composable
private fun SwitchRow(label: Int, hint: String?, on: Boolean, choose: (Boolean) -> Unit) =
    SwitchRow(label, stringResource(label), hint, on, choose)

@Composable
private fun SwitchRow(key: Any, label: String, hint: String?, on: Boolean, choose: (Boolean) -> Unit) {
    val travel by animateFloatAsState(if (on) 1f else 0f, tween(Motion.Quick, easing = Motion.EaseOut), label = "switch")
    PanelRow(key, label, hint, onClick = { choose(!on) }, onKey = null, spokenState = stringResource(if (on) R.string.st_on else R.string.st_off), role = Role.Switch) { fg, _ ->
        Canvas(Modifier.size(36.dp, 20.dp).glassRim(10.dp)) {
            val r = CornerRadius(size.height / 2)
            drawRoundRect(fg, alpha = 0.22f + 0.78f * travel, cornerRadius = r)
            val knob = size.height / 2 - 2.dp.toPx()
            val x = size.height / 2 + (size.width - size.height) * travel
            drawCircle(if (on) knobOn(fg) else fg, radius = knob, center = Offset(x, size.height / 2))
        }
    }
}

/** The knob on a filled track: the track's opposite. */
private fun knobOn(track: Color) = if (track.luminance() > 0.5f) Color(0xFF1D1D1F) else Color.White

/** Up to four choices, or a list for more. */
@Composable
private fun <T> OptionRow(label: Int, hint: String?, options: List<Pair<T, Any>>, current: T, choose: (T) -> Unit) {
    if (options.size <= 4) ValueRow(label, hint, options, current, choose) else ListRow(label, hint, options, current, choose)
}

/**
 * A few choices in one focus stop: Left and Right move through them and take effect at once, so
 * Home shows the result behind the panel. Chevrons appear with focus.
 */
@Composable
private fun <T> ValueRow(label: Int, hint: String?, options: List<Pair<T, Any>>, current: T, choose: (T) -> Unit) {
    val at = options.indexOfFirst { it.first == current }.coerceAtLeast(0)
    // OK does nothing with a remote: a press too many should not change a setting unseen. A touch
    // screen has no Left or Right, so a tap moves on.
    val touch = LocalConfiguration.current.touchscreen != Configuration.TOUCHSCREEN_NOTOUCH
    PanelRow(
        label, stringResource(label), hint,
        onClick = { if (touch) choose(options[(at + 1) % options.size].first) },
        onKey = { e ->
            val code = e.nativeKeyEvent.keyCode
            if (code != NativeKeyEvent.KEYCODE_DPAD_LEFT && code != NativeKeyEvent.KEYCODE_DPAD_RIGHT) return@PanelRow false
            if (e.type == KeyEventType.KeyDown && e.nativeKeyEvent.repeatCount == 0) {
                val to = at + if (code == NativeKeyEvent.KEYCODE_DPAD_RIGHT) 1 else -1
                if (to in options.indices) { Sounds.step(); choose(options[to].first) } else Sounds.limit()
            }
            true
        },
    ) { fg, focused ->
        if (focused) GlyphIcon(Glyph.ChevronLeft, fg.copy(alpha = if (at > 0) 1f else 0.25f), 14.dp)
        RowValue(optionText(options[at].second), fg)
        if (focused) GlyphIcon(Glyph.ChevronRight, fg.copy(alpha = if (at < options.lastIndex) 1f else 0.25f), 14.dp)
    }
}

/** Many choices, or an open-ended list: OK opens them on a page of their own. */
@Composable
private fun <T> ListRow(label: Int, hint: String?, options: List<Pair<T, Any>>, current: T, choose: (T) -> Unit) {
    val panel = LocalPanel.current
    val named = options.map { (value, text) -> value to optionText(text) }
    PanelRow(label, stringResource(label), hint, onClick = {
        panel.returnTo = label
        @Suppress("UNCHECKED_CAST")
        panel.list = OpenList(label, named as List<Pair<Any?, String>>, current) { choose(it as T) }
    }) { fg, _ ->
        RowValue(named.firstOrNull { it.first == current }?.second.orEmpty(), fg)
        GlyphIcon(Glyph.ChevronRight, fg, 14.dp)
    }
}

@Composable
private fun optionText(text: Any) = if (text is Int) stringResource(text) else text.toString()

/** Where the weather is for and what it is now. */
@Composable
private fun WeatherValue(fg: Color) {
    val w = Weather.now
    val place = Weather.place
    RowValue(if (w != null) listOfNotNull(place, "${w.degrees}°").joinToString(" · ") else stringResource(R.string.wp_weather_none), fg)
}

/**
 * Fork: the weather's city chosen on the TV. A name typed with the TV's keyboard is looked up
 * (Weather.search) as typing pauses or on Search; each place found says its region and country,
 * so a city name shared by two countries resolves to the right one. Automatic goes back to the network's guess.
 */
@Composable
private fun LocationPicker(onDone: () -> Unit) {
    var query by remember { mutableStateOf("") }
    var results by remember { mutableStateOf<List<Weather.Place>?>(null) }
    var searching by remember { mutableStateOf(false) }
    var fieldFocused by remember { mutableStateOf(false) }
    val scope = rememberCoroutineScope()
    val field = remember { FocusRequester() }
    val keyboard = LocalSoftwareKeyboardController.current
    fun search() {
        val q = query.trim()
        if (q.length < 2) return
        searching = true
        scope.launch {
            results = Weather.search(q)
            searching = false
        }
    }
    LaunchedEffect(Unit) { runCatching { field.requestFocus() } }
    LaunchedEffect(query) {
        if (query.trim().length < 3) return@LaunchedEffect
        delay(700)
        search()
    }
    Column(
        Modifier.fillMaxWidth().padding(start = 18.dp, end = 10.dp, bottom = 14.dp),
        verticalArrangement = Arrangement.spacedBy(8.dp),
    ) {
        Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(10.dp)) {
            val label = Palette.Label
            BasicTextField(
                value = query,
                onValueChange = { query = it },
                singleLine = true,
                textStyle = Type.Body.copy(color = label),
                cursorBrush = SolidColor(label),
                keyboardOptions = KeyboardOptions(imeAction = ImeAction.Search),
                keyboardActions = KeyboardActions(onSearch = { keyboard?.hide(); search() }),
                modifier = Modifier
                    .weight(1f)
                    .focusRequester(field)
                    .onFocusChanged {
                        fieldFocused = it.isFocused
                        if (it.isFocused) keyboard?.show()
                    }
                    .clip(RoundedCornerShape(50))
                    .background(if (fieldFocused) Palette.FillStrong else Palette.Fill)
                    .border(if (fieldFocused) 2.dp else 0.dp, if (fieldFocused) Palette.Focus else Color.Transparent, RoundedCornerShape(50))
                    .height(40.dp)
                    .padding(horizontal = 18.dp, vertical = 10.dp),
                decorationBox = { inner ->
                    Box {
                        if (query.isEmpty()) BasicText(stringResource(R.string.fk_loc_hint), style = Type.Body.copy(color = Palette.TertiaryLabel))
                        inner()
                    }
                },
            )
            Chip(stringResource(R.string.fk_loc_auto), on = !Weather.chosen) {
                Weather.useNetworkLocation()
                onDone()
            }
        }
        when {
            searching -> BasicText(stringResource(R.string.fk_loc_searching), style = Type.Callout)
            results?.isEmpty() == true -> BasicText(stringResource(R.string.fk_loc_none), style = Type.Callout)
        }
        results?.forEach { place ->
            PlaceRow(place) {
                Weather.choose(place)
                onDone()
            }
        }
    }
}

/** Fork: one place [LocationPicker] found: its name, then its region and country. */
@Composable
private fun PlaceRow(place: Weather.Place, onClick: () -> Unit) {
    Pressable(onClick = onClick, focusedScale = 1.02f, modifier = Modifier.fillMaxWidth()) { state ->
        val bg by animateColorAsState(if (state.focused) Palette.Focus else Palette.Fill, tween(Motion.Quick, easing = Motion.EaseOut), label = "place-bg")
        val fg = if (state.focused) Palette.OnFocus else Palette.Label
        Row(
            Modifier.fillMaxWidth().clip(RoundedCornerShape(12.dp)).background(bg).height(44.dp).padding(horizontal = 18.dp),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.spacedBy(10.dp),
        ) {
            BasicText(place.name, style = Type.Body.copy(color = fg))
            BasicText(place.detail, style = Type.Callout.copy(color = fg.copy(alpha = 0.6f)), maxLines = 1)
        }
    }
}

/** A pill to pick one of a few options: a check on the chosen one, white when focused. */
@Composable
private fun Chip(label: String, on: Boolean, onFocused: (() -> Unit)? = null, onClick: () -> Unit) {
    Pressable(onClick = onClick, focusedScale = 1.06f) { state ->
        if (onFocused != null) LaunchedEffect(state.focused) { if (state.focused) onFocused() }
        val bg by animateColorAsState(
            when {
                state.focused -> Palette.Focus
                on -> Palette.FillStrong
                else -> Palette.Fill
            },
            tween(Motion.Quick, easing = Motion.EaseOut), label = "chip-bg",
        )
        val fg = if (state.focused) Palette.OnFocus else Palette.Label
        Row(
            Modifier.clip(RoundedCornerShape(50)).background(bg).height(40.dp).glassRim(20.dp).padding(horizontal = 20.dp),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.spacedBy(8.dp),
        ) {
            if (on) GlyphIcon(Glyph.Check, fg, 14.dp)
            BasicText(label, style = Type.Body.copy(color = fg))
        }
    }
}

@Composable
private fun PhoneCard(url: String?, qr: ImageBitmap?) {
    Column(Modifier.fillMaxWidth().padding(top = 6.dp).focusable(), horizontalAlignment = Alignment.CenterHorizontally) {
        Box(
            Modifier.size(176.dp).clip(RoundedCornerShape(18.dp)).background(Color.White).padding(14.dp),
            contentAlignment = Alignment.Center,
        ) {
            if (qr != null) {
                Image(qr, contentDescription = null, contentScale = ContentScale.Fit, filterQuality = FilterQuality.None, modifier = Modifier.fillMaxSize())
            } else {
                GlyphIcon(Glyph.Offline, Color.Black.copy(alpha = 0.35f), 40.dp)
            }
        }
        Spacer(Modifier.height(14.dp))
        BasicText(stringResource(R.string.wp_phone_title), style = Type.Headline)
        Spacer(Modifier.height(4.dp))
        BasicText(
            stringResource(if (url != null) R.string.wp_phone_body else R.string.wp_phone_offline),
            style = Type.Caption.copy(textAlign = androidx.compose.ui.text.style.TextAlign.Center),
        )
        if (url != null) {
            Spacer(Modifier.height(6.dp))
            // The whole address, token and all, to type into a computer's browser.
            BasicText(url.removePrefix("http://"), style = Type.Caption.copy(color = Palette.SecondaryLabel))
        }
    }
}

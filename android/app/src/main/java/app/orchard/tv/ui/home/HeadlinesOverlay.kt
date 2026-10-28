package app.orchard.tv.ui.home

import androidx.compose.animation.AnimatedContent
import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.animation.core.tween
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.animation.togetherWith
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.text.BasicText
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.Shadow
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import app.orchard.tv.R
import app.orchard.tv.news.Headlines
import app.orchard.tv.settings.AerialSettings
import app.orchard.tv.ui.theme.Motion
import app.orchard.tv.ui.theme.Type
import kotlinx.coroutines.delay
import java.time.LocalDateTime

private const val FIRST_AFTER_MS = 20_000L
private const val EACH_MS = 20_000L

/**
 * Fork: one technology headline at a time, bottom right, opposite the place name. No card and no
 * blur (a blur over moving video is a full-screen draw every frame): white text on a neutral shade,
 * like the place name. It appears 20 s in, changes every 20 s, and does not move between changes
 * apart from the screensaver's slow drift against burn-in.
 */
@Composable
internal fun HeadlinesOverlay(now: LocalDateTime) {
    if (!AerialSettings.headlines) return
    val context = LocalContext.current
    LaunchedEffect(Unit) { Headlines.init(context); Headlines.refreshIfStale() }
    val list = Headlines.items
    var shown by remember { mutableStateOf(false) }
    var index by remember { mutableIntStateOf(0) }
    LaunchedEffect(list.size) {
        if (list.isEmpty()) return@LaunchedEffect
        if (!shown) { delay(FIRST_AFTER_MS); shown = true }
        while (true) { delay(EACH_MS); index = (index + 1) % list.size }
    }
    val alpha by animateFloatAsState(if (shown && list.isNotEmpty()) 1f else 0f, tween(1200, easing = Motion.EaseOut), label = "headlines")
    if (alpha == 0f) return
    val item = list.getOrNull(index % list.size.coerceAtLeast(1)) ?: return
    val drift = saverDrift(now)
    Box(Modifier.fillMaxSize().graphicsLayer { this.alpha = alpha }) {
        Canvas(Modifier.fillMaxSize()) {
            // A soft shade from the bottom-right corner, just enough for white text over snow or sky.
            drawRect(Brush.radialGradient(0f to Color.Black.copy(alpha = 0.42f), 1f to Color.Transparent, center = Offset(size.width, size.height), radius = size.width * 0.55f))
        }
        AnimatedContent(
            targetState = item,
            transitionSpec = { fadeIn(tween(600, delayMillis = 400, easing = Motion.EaseOut)) togetherWith fadeOut(tween(400, easing = Motion.EaseOut)) },
            modifier = Modifier.align(Alignment.BottomEnd).padding(end = 56.dp, bottom = 44.dp)
                .graphicsLayer { translationX = -drift.x.toPx(); translationY = -drift.y.toPx() },
            label = "headline",
        ) { h ->
            Column(Modifier.width(330.dp)) {
                BasicText(
                    h.title, maxLines = 2, overflow = TextOverflow.Ellipsis,
                    style = Type.Title.copy(color = Color.White.copy(alpha = 0.94f), shadow = TextShade),
                )
                Spacer(Modifier.height(6.dp))
                BasicText(
                    listOf(h.source, age(h.published)).filter { it.isNotBlank() }.joinToString(" · "),
                    maxLines = 1,
                    style = Type.Callout.copy(color = Color.White.copy(alpha = 0.62f), shadow = TextShade),
                )
            }
        }
    }
}

private val TextShade = Shadow(Color.Black.copy(alpha = 0.45f), Offset(0f, 1f), 6f)

@Composable
private fun age(published: Long): String {
    val minutes = ((System.currentTimeMillis() - published) / 60_000).coerceAtLeast(1)
    return if (minutes < 60) stringResource(R.string.fk_age_minutes, minutes.toInt())
    else stringResource(R.string.fk_age_hours, (minutes / 60).toInt())
}

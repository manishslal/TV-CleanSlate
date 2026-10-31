package app.orchard.tv.ui.components

import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.core.tween
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.animation.slideInHorizontally
import androidx.compose.animation.slideOutHorizontally
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.ColumnScope
import androidx.compose.foundation.layout.fillMaxHeight
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.width
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import app.orchard.tv.ui.theme.Motion

private val MarginTop = 24.dp
private val MarginEnd = 32.dp
private val Width = 320.dp
private val Corner = 24.dp
private val Inset = 16.dp

/**
 * Fork: a panel docked on the right, full height, under the same corner as Control Center. It keeps
 * the wallpaper in view on its left, so a new one can be seen the moment it is chosen.
 */
@Composable
fun SidePanel(visible: Boolean, content: @Composable ColumnScope.() -> Unit) {
    Box(Modifier.fillMaxSize().padding(top = MarginTop, bottom = MarginTop, end = MarginEnd), contentAlignment = Alignment.TopEnd) {
        AnimatedVisibility(
            visible = visible,
            enter = fadeIn(tween(Motion.Quick, easing = Motion.EaseOut)) + slideInHorizontally(tween(Motion.Enter, easing = Motion.Drawer)) { it / 6 },
            exit = fadeOut(tween(Motion.Quick, easing = Motion.EaseOut)) + slideOutHorizontally(tween(Motion.Quick, easing = Motion.EaseOut)) { it / 8 },
        ) {
            Column(Modifier.width(Width).fillMaxHeight().consumeTaps().liquidGlass(Corner).padding(Inset), content = content)
        }
    }
}

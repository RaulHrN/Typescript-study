package com.raulhrn.vox.ui

import androidx.activity.compose.BackHandler
import androidx.compose.animation.Crossfade
import androidx.compose.animation.core.tween
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.safeDrawingPadding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateListOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.unit.dp
import com.raulhrn.vox.ui.screens.AddSongScreen
import com.raulhrn.vox.ui.screens.ArticleScreen
import com.raulhrn.vox.ui.screens.ExerciseScreen
import com.raulhrn.vox.ui.screens.LiveScreen
import com.raulhrn.vox.ui.screens.PracticeScreen
import com.raulhrn.vox.ui.screens.RangeTestScreen
import com.raulhrn.vox.ui.screens.SettingsScreen
import com.raulhrn.vox.ui.screens.SongsScreen
import com.raulhrn.vox.ui.screens.StudyScreen
import com.raulhrn.vox.ui.screens.TrainScreen

enum class Tab(val label: String) {
    LIVE("Ouvir"),
    TRAIN("Treinar"),
    SONGS("Músicas"),
    STUDY("Estudar"),
}

sealed interface Route {
    data class Exercise(val id: String) : Route
    data object RangeTest : Route
    data object AddSong : Route
    data class Practice(val id: String) : Route
    data class Article(val index: Int) : Route
    data object Settings : Route
}

/** Minimal navigation: one tab plus a stack of pushed screens. */
class Nav {
    var tab by mutableStateOf(Tab.LIVE)
    val stack = mutableStateListOf<Route>()

    fun push(route: Route) {
        stack.add(route)
    }

    fun pop(): Boolean {
        if (stack.isEmpty()) return false
        stack.removeAt(stack.lastIndex)
        return true
    }

    fun replace(route: Route) {
        if (stack.isNotEmpty()) stack.removeAt(stack.lastIndex)
        stack.add(route)
    }
}

@Composable
fun VoxApp() {
    val c = Vox.colors
    val nav = remember { Nav() }
    BackHandler(enabled = nav.stack.isNotEmpty() || nav.tab != Tab.LIVE) {
        if (!nav.pop()) nav.tab = Tab.LIVE
    }
    Column(Modifier.fillMaxSize().background(c.bg).safeDrawingPadding()) {
        val top: Any = nav.stack.lastOrNull() ?: nav.tab
        Box(Modifier.weight(1f).fillMaxWidth()) {
            Crossfade(targetState = top, animationSpec = tween(160), label = "screen") { screen ->
                Box(Modifier.fillMaxSize()) {
                    when (screen) {
                        Tab.LIVE -> LiveScreen(nav)
                        Tab.TRAIN -> TrainScreen(nav)
                        Tab.SONGS -> SongsScreen(nav)
                        Tab.STUDY -> StudyScreen(nav)
                        is Route.Exercise -> ExerciseScreen(nav, screen.id)
                        Route.RangeTest -> RangeTestScreen(nav)
                        Route.AddSong -> AddSongScreen(nav)
                        is Route.Practice -> PracticeScreen(nav, screen.id)
                        is Route.Article -> ArticleScreen(nav, screen.index)
                        Route.Settings -> SettingsScreen(nav)
                    }
                }
            }
        }
        if (nav.stack.isEmpty()) BottomBar(nav)
    }
}

@Composable
private fun BottomBar(nav: Nav) {
    val c = Vox.colors
    Column {
        Hairline()
        Row(
            Modifier.fillMaxWidth().height(60.dp).padding(horizontal = 8.dp),
            horizontalArrangement = Arrangement.SpaceEvenly,
            verticalAlignment = Alignment.CenterVertically,
        ) {
            for (tab in Tab.entries) {
                val selected = nav.tab == tab
                Column(
                    Modifier
                        .weight(1f)
                        .clip(CircleShape)
                        .clickable { nav.tab = tab }
                        .padding(vertical = 8.dp),
                    horizontalAlignment = Alignment.CenterHorizontally,
                ) {
                    Text(tab.label, style = VoxType.label, color = if (selected) c.text else c.muted)
                    Spacer(Modifier.height(6.dp))
                    Box(
                        Modifier
                            .size(4.dp)
                            .clip(CircleShape)
                            .background(if (selected) c.accent else c.bg),
                    )
                }
            }
        }
    }
}

package com.raulhrn.vox.ui.screens

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import com.raulhrn.vox.core.Exercises
import com.raulhrn.vox.core.NoteNames
import com.raulhrn.vox.core.Tips
import com.raulhrn.vox.core.VoiceType
import com.raulhrn.vox.data.Prefs
import com.raulhrn.vox.ui.Header
import com.raulhrn.vox.ui.ListRow
import com.raulhrn.vox.ui.Nav
import com.raulhrn.vox.ui.Panel
import com.raulhrn.vox.ui.Route
import com.raulhrn.vox.ui.ScreenPadding
import com.raulhrn.vox.ui.Section
import com.raulhrn.vox.ui.Vox
import com.raulhrn.vox.ui.VoxButton
import com.raulhrn.vox.ui.VoxType

@Composable
fun TrainScreen(nav: Nav) {
    val c = Vox.colors
    val naming = Prefs.naming
    val tip = remember { Tips.ofTheDay(System.currentTimeMillis() / 86_400_000L) }
    val groups = remember { Exercises.all.groupBy { it.category } }
    LazyColumn(Modifier.fillMaxSize().padding(horizontal = ScreenPadding)) {
        item { Header("Treinar") }
        item {
            Panel {
                Text("DICA DO DIA", style = VoxType.caps, color = c.accent)
                Spacer(Modifier.height(6.dp))
                Text(tip, style = VoxType.body, color = c.text)
            }
            Spacer(Modifier.height(12.dp))
            Row(horizontalArrangement = Arrangement.spacedBy(12.dp)) {
                Panel(Modifier.weight(1f), onClick = { nav.push(Route.Lessons) }) {
                    Text("APRENDER", style = VoxType.caps, color = c.accent)
                    Spacer(Modifier.height(6.dp))
                    Text("Tipos de segunda voz", style = VoxType.label, color = c.text)
                    Text("8 tipos, do básico ao avançado", style = VoxType.small, color = c.muted)
                }
                Panel(Modifier.weight(1f), onClick = { nav.push(Route.BackingLab) }) {
                    Text("LABORATÓRIO", style = VoxType.caps, color = c.accent)
                    Spacer(Modifier.height(6.dp))
                    Text("Backing vocal", style = VoxType.label, color = c.text)
                    Text("Conforme a voz do regente", style = VoxType.small, color = c.muted)
                }
            }
            Spacer(Modifier.height(12.dp))
            Panel {
                Text("SUA EXTENSÃO", style = VoxType.caps, color = c.muted)
                Spacer(Modifier.height(6.dp))
                Row(verticalAlignment = Alignment.CenterVertically) {
                    androidx.compose.foundation.layout.Column(Modifier.weight(1f)) {
                        Text(
                            "${NoteNames.full(Prefs.rangeLow, naming)} – ${NoteNames.full(Prefs.rangeHigh, naming)}",
                            style = VoxType.heading,
                            color = c.text,
                        )
                        Text(
                            if (Prefs.rangeMeasured) VoiceType.classify(Prefs.rangeLow, Prefs.rangeHigh).label else "Padrão, ainda não medida",
                            style = VoxType.small,
                            color = c.muted,
                        )
                    }
                    VoxButton(if (Prefs.rangeMeasured) "Refazer" else "Medir", { nav.push(Route.RangeTest) }, primary = !Prefs.rangeMeasured)
                }
            }
        }
        for ((category, list) in groups) {
            item(key = "h-$category") { Section(category) }
            items(list, key = { it.id }) { ex ->
                ListRow(ex.title, ex.summary, "${ex.minutes} min") { nav.push(Route.Exercise(ex.id)) }
            }
        }
        item { Spacer(Modifier.height(24.dp)) }
    }
}

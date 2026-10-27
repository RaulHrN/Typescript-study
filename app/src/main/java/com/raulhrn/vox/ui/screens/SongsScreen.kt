package com.raulhrn.vox.ui.screens

import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import com.raulhrn.vox.data.Prefs
import com.raulhrn.vox.data.SongStore
import com.raulhrn.vox.ui.Header
import com.raulhrn.vox.ui.IconBtn
import com.raulhrn.vox.ui.ListRow
import com.raulhrn.vox.ui.Nav
import com.raulhrn.vox.ui.Panel
import com.raulhrn.vox.ui.Route
import com.raulhrn.vox.ui.ScreenPadding
import com.raulhrn.vox.ui.Vox
import com.raulhrn.vox.ui.VoxButton
import com.raulhrn.vox.ui.VoxIcons
import com.raulhrn.vox.ui.VoxType
import com.raulhrn.vox.ui.formatTime

@Composable
fun SongsScreen(nav: Nav) {
    val c = Vox.colors
    val naming = Prefs.naming
    LazyColumn(Modifier.fillMaxSize().padding(horizontal = ScreenPadding)) {
        item {
            Header("Músicas") {
                IconBtn(VoxIcons.Add, "Adicionar música", { nav.push(Route.AddSong) })
            }
        }
        if (SongStore.loaded.value && SongStore.songs.isEmpty()) {
            item {
                Panel {
                    Text("Aprenda a fazer segunda voz", style = VoxType.heading, color = c.text)
                    Spacer(Modifier.height(8.dp))
                    Text(
                        "Adicione uma música e o Vox encontra a melodia, descobre o tom e cria a segunda voz em terças ou sextas. " +
                            "Depois é só ouvir, cantar junto e acompanhar a sua afinação na tela.",
                        style = VoxType.small,
                        color = c.muted,
                    )
                    Spacer(Modifier.height(12.dp))
                    for (line in listOf(
                        "Arquivo de áudio: melhor com voz em destaque ou a cappella.",
                        "Cantando: você canta a melodia e o app transcreve.",
                        "Digitando: escreva as notas, como \"Dó Ré Mi:2\".",
                    )) {
                        Row(Modifier.padding(vertical = 3.dp)) {
                            Text("•", style = VoxType.small, color = c.accent, modifier = Modifier.width(14.dp))
                            Text(line, style = VoxType.small, color = c.text)
                        }
                    }
                    Spacer(Modifier.height(16.dp))
                    VoxButton("Adicionar música", { nav.push(Route.AddSong) }, Modifier.fillMaxWidth(), icon = VoxIcons.Add)
                }
            }
        }
        items(SongStore.songs, key = { it.id }) { song ->
            ListRow(
                song.title,
                "${song.key.label(naming)} · ${song.mode.label}",
                formatTime(song.durationMs.toFloat()),
            ) { nav.push(Route.Practice(song.id)) }
        }
        item { Spacer(Modifier.height(24.dp)) }
    }
}

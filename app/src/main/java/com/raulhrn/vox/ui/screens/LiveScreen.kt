package com.raulhrn.vox.ui.screens

import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableFloatStateOf
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.mutableLongStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.runtime.withFrameMillis
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import com.raulhrn.vox.audio.MicEngine
import com.raulhrn.vox.core.NoteFollower
import com.raulhrn.vox.core.NoteNames
import com.raulhrn.vox.core.Pitch
import com.raulhrn.vox.data.Prefs
import com.raulhrn.vox.ui.CentsMeter
import com.raulhrn.vox.ui.Header
import com.raulhrn.vox.ui.IconBtn
import com.raulhrn.vox.ui.LevelBar
import com.raulhrn.vox.ui.LiveTrace
import com.raulhrn.vox.ui.MicActive
import com.raulhrn.vox.ui.MicGate
import com.raulhrn.vox.ui.NoteReadout
import com.raulhrn.vox.ui.Route
import com.raulhrn.vox.ui.ScreenPadding
import com.raulhrn.vox.ui.Nav
import com.raulhrn.vox.ui.Vox
import com.raulhrn.vox.ui.VoxIcons
import com.raulhrn.vox.ui.VoxType
import java.util.Locale
import kotlin.math.abs
import kotlin.math.max
import kotlin.math.roundToInt

@Composable
fun LiveScreen(nav: Nav) {
    Column(Modifier.fillMaxSize().padding(horizontal = ScreenPadding)) {
        Header("Ouvir") {
            IconBtn(VoxIcons.Tune, "Ajustes", { nav.push(Route.Settings) })
        }
        MicGate { LiveContent() }
    }
}

@Composable
private fun LiveContent() {
    val c = Vox.colors
    val naming = Prefs.naming
    MicActive()

    val follower = remember { NoteFollower(historySize = 8) }
    val tick = remember { mutableLongStateOf(0L) }
    var note by remember { mutableIntStateOf(-1) }
    val cents = remember { mutableFloatStateOf(0f) }
    var info by remember { mutableStateOf("") }
    var recentVersion by remember { mutableIntStateOf(0) }
    val level = remember { mutableFloatStateOf(-100f) }
    var error by remember { mutableStateOf<String?>(null) }

    LaunchedEffect(Unit) {
        var lastFrame = MicEngine.frames
        var lastInfo = 0L
        var lastDraw = 0L
        var lastNote = -1
        var smoothCents = 0f
        while (true) {
            withFrameMillis { now ->
                val f = MicEngine.frames
                var i = max(lastFrame, f - 32)
                while (i < f) {
                    val idx = MicEngine.index(i)
                    follower.update(MicEngine.midi[idx], MicEngine.timeNs[idx] / 1_000_000)
                    i++
                }
                lastFrame = f
                // Smooth the meter (~120 ms) so a natural vibrato does not make it jump around.
                if (follower.note != lastNote) {
                    lastNote = follower.note
                    smoothCents = follower.cents
                } else {
                    smoothCents += (follower.cents - smoothCents) * 0.12f
                }
                note = follower.note
                cents.floatValue = smoothCents
                recentVersion = follower.version
                level.floatValue = MicEngine.latestLevel()
                error = MicEngine.error
                if (now - lastInfo > 120) {
                    lastInfo = now
                    info = if (follower.note < 0) {
                        ""
                    } else {
                        val hz = Pitch.midiToHz(follower.note + smoothCents / 100f, Prefs.a4)
                        val cc = smoothCents.roundToInt()
                        val tuning = if (abs(cc) <= 5) "afinado" else (if (cc > 0) "+$cc" else "$cc") + " cents"
                        String.format(Locale("pt", "BR"), "%.1f Hz  ·  %s", hz, tuning)
                    }
                }
                if (now - lastDraw >= 15) {
                    lastDraw = now
                    tick.longValue = now
                }
            }
        }
    }

    Column(Modifier.fillMaxSize()) {
        Spacer(Modifier.height(8.dp))
        NoteReadout(note, naming, Modifier.fillMaxWidth())
        Text(
            when {
                error != null -> error!!
                note < 0 -> "Cante, fale ou toque uma nota"
                else -> info
            },
            style = VoxType.small,
            color = c.muted,
            textAlign = TextAlign.Center,
            modifier = Modifier.fillMaxWidth(),
        )
        Spacer(Modifier.height(12.dp))
        CentsMeter(cents = { cents.floatValue }, active = { note >= 0 })
        Spacer(Modifier.height(12.dp))
        LiveTrace(tick, Modifier.weight(1f).fillMaxWidth(), naming = naming)
        Spacer(Modifier.height(12.dp))

        val recent = remember(recentVersion) { follower.recentNotes().takeLast(6) }
        Row(Modifier.fillMaxWidth(), verticalAlignment = Alignment.CenterVertically) {
            Text("RECENTES", style = VoxType.caps, color = c.faint, modifier = Modifier.weight(1f))
            if (recent.isNotEmpty()) {
                Text(
                    "limpar",
                    style = VoxType.small,
                    color = c.muted,
                    modifier = Modifier.clickable { follower.clear() }.padding(6.dp),
                )
            }
        }
        Row(Modifier.fillMaxWidth().height(28.dp), horizontalArrangement = Arrangement.spacedBy(14.dp)) {
            for ((k, n) in recent.withIndex()) {
                Text(
                    NoteNames.full(n, naming),
                    style = VoxType.label,
                    color = if (k == recent.lastIndex) c.text else c.muted,
                    maxLines = 1,
                )
            }
        }
        Spacer(Modifier.height(10.dp))
        LevelBar({ level.floatValue })
        Spacer(Modifier.height(12.dp))
    }
}

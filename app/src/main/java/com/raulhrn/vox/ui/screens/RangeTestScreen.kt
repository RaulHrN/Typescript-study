package com.raulhrn.vox.ui.screens

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
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.mutableLongStateOf
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
import com.raulhrn.vox.core.VoiceType
import com.raulhrn.vox.data.Prefs
import com.raulhrn.vox.ui.Header
import com.raulhrn.vox.ui.LiveTrace
import com.raulhrn.vox.ui.MicActive
import com.raulhrn.vox.ui.MicGate
import com.raulhrn.vox.ui.Nav
import com.raulhrn.vox.ui.NoteReadout
import com.raulhrn.vox.ui.Panel
import com.raulhrn.vox.ui.ScreenPadding
import com.raulhrn.vox.ui.Vox
import com.raulhrn.vox.ui.VoxButton
import com.raulhrn.vox.ui.VoxType
import kotlin.math.abs
import kotlin.math.max
import kotlin.math.roundToInt

@Composable
fun RangeTestScreen(nav: Nav) {
    Column(Modifier.fillMaxSize().padding(horizontal = ScreenPadding)) {
        Header("Teste de extensão", onBack = { nav.pop() })
        MicGate { RangeContent(nav) }
    }
}

@Composable
private fun RangeContent(nav: Nav) {
    val c = Vox.colors
    val naming = Prefs.naming
    var step by remember { mutableIntStateOf(0) }
    var low by remember { mutableIntStateOf(-1) }
    var high by remember { mutableIntStateOf(-1) }
    var current by remember { mutableIntStateOf(-1) }
    val tick = remember { mutableLongStateOf(0L) }

    MicActive(enabled = step == 1 || step == 2)

    LaunchedEffect(step) {
        if (step != 1 && step != 2) return@LaunchedEffect
        val follower = NoteFollower()
        var lastFrame = MicEngine.frames
        var runNote = -1
        var runStartMs = 0L
        while (true) {
            withFrameMillis { now ->
                val f = MicEngine.frames
                var i = max(lastFrame, f - 32)
                while (i < f) {
                    val idx = MicEngine.index(i)
                    val m = MicEngine.midi[idx]
                    val ms = MicEngine.timeNs[idx] / 1_000_000
                    follower.update(m, ms)
                    if (m.isNaN()) {
                        runNote = -1
                    } else {
                        val r = m.roundToInt()
                        if (r == runNote && abs(m - r) < 0.45f) {
                            // A note counts once it is held steadily for 0.4 s.
                            if (ms - runStartMs >= 400) {
                                if (step == 1 && (low < 0 || r < low)) low = r
                                if (step == 2 && (high < 0 || r > high)) high = r
                            }
                        } else {
                            runNote = r
                            runStartMs = ms
                        }
                    }
                    i++
                }
                lastFrame = f
                current = follower.note
                tick.longValue = now
            }
        }
    }

    Column(Modifier.fillMaxSize()) {
        when (step) {
            0 -> {
                Panel {
                    Text("Vamos descobrir as notas mais grave e mais aguda que você canta com conforto.", style = VoxType.body, color = c.text)
                    Spacer(Modifier.height(10.dp))
                    Text(
                        "Fique num lugar silencioso. Cante em \"a\" ou \"u\", sem forçar, e sustente cada nota por um segundo. Só contam notas firmes.",
                        style = VoxType.small,
                        color = c.muted,
                    )
                }
                Spacer(Modifier.weight(1f))
                VoxButton("Começar", { step = 1 }, Modifier.fillMaxWidth())
                Spacer(Modifier.height(16.dp))
            }
            1, 2 -> {
                Text(
                    if (step == 1) "Desça até a nota mais GRAVE" else "Agora suba até a mais AGUDA",
                    style = VoxType.heading,
                    color = c.text,
                )
                Text(
                    if (step == 1) "Vá descendo devagar, nota por nota, até onde a voz ainda soa limpa." else "Suba devagar. Pode usar voz leve ou falsete, mas sem gritar.",
                    style = VoxType.small,
                    color = c.muted,
                )
                Spacer(Modifier.height(12.dp))
                NoteReadout(current, naming, Modifier.fillMaxWidth())
                val record = if (step == 1) low else high
                Text(
                    if (record < 0) "Sustente uma nota…" else (if (step == 1) "Mais grave: " else "Mais aguda: ") + NoteNames.full(record, naming),
                    style = VoxType.label,
                    color = if (record < 0) c.muted else c.accent,
                    textAlign = TextAlign.Center,
                    modifier = Modifier.fillMaxWidth(),
                )
                Spacer(Modifier.height(12.dp))
                LiveTrace(tick, Modifier.weight(1f).fillMaxWidth(), naming = naming)
                Spacer(Modifier.height(12.dp))
                Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(12.dp)) {
                    VoxButton("Refazer", { if (step == 1) low = -1 else high = -1 }, Modifier.weight(1f), primary = false)
                    VoxButton(
                        if (step == 1) "Próximo" else "Ver resultado",
                        { step++ },
                        Modifier.weight(1f),
                        enabled = record >= 0,
                    )
                }
                Spacer(Modifier.height(16.dp))
            }
            else -> {
                val ok = low >= 0 && high >= 0 && high - low >= 5
                if (ok) {
                    val type = VoiceType.classify(low, high)
                    val span = high - low
                    Panel {
                        Text("SUA EXTENSÃO", style = VoxType.caps, color = c.muted)
                        Spacer(Modifier.height(6.dp))
                        Text("${NoteNames.full(low, naming)} – ${NoteNames.full(high, naming)}", style = VoxType.title, color = c.text)
                        Text(
                            "$span semitons" + if (span >= 12) " · ${"%.1f".format(span / 12f).replace('.', ',')} oitavas" else "",
                            style = VoxType.small,
                            color = c.muted,
                        )
                        Spacer(Modifier.height(16.dp))
                        Text("VOZ PROVÁVEL", style = VoxType.caps, color = c.muted)
                        Spacer(Modifier.height(6.dp))
                        Text(type.label, style = VoxType.heading, color = c.accent)
                        Text(
                            "Faixa típica: ${NoteNames.full(type.low, naming)} – ${NoteNames.full(type.high, naming)}. " +
                                "É só uma referência: timbre e conforto também contam, e a extensão cresce com treino.",
                            style = VoxType.small,
                            color = c.muted,
                        )
                    }
                    Spacer(Modifier.weight(1f))
                    VoxButton("Salvar e usar nos exercícios", {
                        Prefs.setRange(low, high, measured = true)
                        nav.pop()
                    }, Modifier.fillMaxWidth())
                } else {
                    Panel {
                        Text("Não consegui medir uma extensão confiável.", style = VoxType.body, color = c.text)
                        Text("Tente de novo num lugar mais silencioso, sustentando bem cada nota.", style = VoxType.small, color = c.muted)
                    }
                    Spacer(Modifier.weight(1f))
                }
                Spacer(Modifier.height(10.dp))
                VoxButton("Refazer o teste", {
                    low = -1
                    high = -1
                    step = 1
                }, Modifier.fillMaxWidth(), primary = false)
                Spacer(Modifier.height(16.dp))
            }
        }
    }
}

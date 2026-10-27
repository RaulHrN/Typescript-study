package com.raulhrn.vox.ui.screens

import androidx.compose.foundation.Canvas
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
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.material3.Icon
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.DisposableEffect
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableFloatStateOf
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.mutableLongStateOf
import androidx.compose.runtime.mutableStateListOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.runtime.withFrameMillis
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import com.raulhrn.vox.audio.MicEngine
import com.raulhrn.vox.audio.Synth
import com.raulhrn.vox.audio.SynthTrack
import com.raulhrn.vox.core.Exercise
import com.raulhrn.vox.core.ExerciseKind
import com.raulhrn.vox.core.ExercisePlan
import com.raulhrn.vox.core.ExerciseTimeline
import com.raulhrn.vox.core.Exercises
import com.raulhrn.vox.core.FloatList
import com.raulhrn.vox.core.NoteNames
import com.raulhrn.vox.core.Role
import com.raulhrn.vox.core.Scorer
import com.raulhrn.vox.core.Timeline
import com.raulhrn.vox.data.Prefs
import com.raulhrn.vox.ui.Header
import com.raulhrn.vox.ui.LevelBar
import com.raulhrn.vox.ui.MicActive
import com.raulhrn.vox.ui.MicGate
import com.raulhrn.vox.ui.Nav
import com.raulhrn.vox.ui.Panel
import com.raulhrn.vox.ui.PianoRoll
import com.raulhrn.vox.ui.RollKind
import com.raulhrn.vox.ui.RollNote
import com.raulhrn.vox.ui.RoundButton
import com.raulhrn.vox.ui.ScreenPadding
import com.raulhrn.vox.ui.ToggleRow
import com.raulhrn.vox.ui.Trace
import com.raulhrn.vox.ui.Vox
import com.raulhrn.vox.ui.VoxIcons
import com.raulhrn.vox.ui.VoxType
import kotlin.math.max
import kotlin.math.roundToInt
import kotlin.math.sqrt

@Composable
fun ExerciseScreen(nav: Nav, id: String) {
    val ex = remember(id) { Exercises.byId(id) }
    Column(Modifier.fillMaxSize().padding(horizontal = ScreenPadding)) {
        Header(ex?.title ?: "Exercício", onBack = { nav.pop() }, subtitle = ex?.category)
        if (ex == null) {
            Text("Exercício não encontrado.", style = VoxType.body, color = Vox.colors.muted)
            return@Column
        }
        MicGate {
            if (ex.kind == ExerciseKind.BREATH) BreathContent(ex) else PatternContent(ex)
        }
    }
}

private enum class Phase(val label: String) {
    IDLE("Pronto?"),
    LISTEN("Ouça"),
    READY("Prepare-se"),
    SING("Sua vez"),
    PAUSED("Pausado"),
    DONE("Concluído"),
}

private fun rollNotes(tl: Timeline): List<RollNote> {
    var target = 0
    return tl.notes.map {
        when (it.role) {
            Role.LISTEN -> RollNote(it.startMs, it.durMs, it.midi, RollKind.GUIDE)
            Role.ACCOMP -> RollNote(it.startMs, it.durMs, it.midi, RollKind.MELODY)
            Role.TARGET -> RollNote(it.startMs, it.durMs, it.midi, RollKind.TARGET, target++)
        }
    }
}

@Composable
private fun PatternContent(ex: Exercise) {
    val c = Vox.colors
    val naming = Prefs.naming
    val low = Prefs.rangeLow
    val high = Prefs.rangeHigh
    val roots = remember(ex.id, low, high) { ExercisePlan.roots(ex, low, high) }
    val synth = remember { Synth() }
    DisposableEffect(Unit) { onDispose { synth.stop() } }
    synth.a4 = Prefs.a4

    var running by remember { mutableStateOf(false) }
    var rep by remember { mutableIntStateOf(0) }
    var transpose by remember { mutableIntStateOf(0) }
    var guide by remember { mutableStateOf(false) }
    var phase by remember { mutableStateOf(Phase.IDLE) }
    var showSteps by remember { mutableStateOf(true) }
    var timeline by remember { mutableStateOf(ExerciseTimeline.build(ex, roots.first())) }
    var scorer by remember { mutableStateOf(Scorer(timeline.targets)) }
    val guideTrack = remember { arrayOfNulls<SynthTrack>(1) }
    val results = remember { mutableStateListOf<Float>() }
    val trace = remember { Trace() }
    val tick = remember { mutableLongStateOf(0L) }

    MicActive(enabled = running)

    LaunchedEffect(running) {
        if (!running) return@LaunchedEffect
        showSteps = false
        try {
            while (rep < roots.size) {
                val root = (roots[rep] + transpose).coerceIn(24, 96)
                val tl = ExerciseTimeline.build(ex, root)
                val sc = Scorer(tl.targets)
                timeline = tl
                scorer = sc
                trace.clear()
                val listenEnd = tl.notes.filter { it.role == Role.LISTEN }.maxOfOrNull { it.endMs } ?: 0
                val main = SynthTrack.ofTimed(tl.notes.filter { it.role != Role.TARGET }, gain = 0.3f)
                val g = SynthTrack.ofTimed(tl.targets, gain = 0.2f, bright = true).also { it.enabled = guide }
                guideTrack[0] = g
                synth.start(listOf(main, g), 0f)
                var lastFrame = MicEngine.frames
                var done = false
                while (!done) {
                    withFrameMillis { now ->
                        val pos = synth.positionMs()
                        val f = MicEngine.frames
                        val nowNs = System.nanoTime()
                        var i = max(lastFrame, f - 64)
                        while (i < f) {
                            val idx = MicEngine.index(i)
                            val t = pos - (nowNs - MicEngine.timeNs[idx]) / 1e6f - MicEngine.LATENCY_MS
                            val m = MicEngine.midi[idx]
                            sc.add(t, m)
                            if (t >= listenEnd) trace.add(t, m)
                            i++
                        }
                        lastFrame = f
                        phase = when {
                            pos < listenEnd -> Phase.LISTEN
                            pos < tl.singStartMs -> Phase.READY
                            else -> Phase.SING
                        }
                        tick.longValue = now
                        done = pos >= tl.lengthMs
                    }
                }
                synth.stop()
                results.add(max(0f, sc.total()))
                rep++
            }
            phase = Phase.DONE
            running = false
        } finally {
            synth.stop()
            if (phase != Phase.DONE) phase = Phase.PAUSED
        }
    }

    val notes = remember(timeline) { rollNotes(timeline) }
    val currentRoot = (roots[rep.coerceAtMost(roots.lastIndex)] + transpose).coerceIn(24, 96)

    Column(Modifier.fillMaxSize()) {
        if (ex.needsHeadphones) {
            Row(Modifier.padding(bottom = 8.dp), verticalAlignment = Alignment.CenterVertically) {
                Icon(VoxIcons.Headphones, null, tint = c.muted, modifier = Modifier.size(16.dp))
                Spacer(Modifier.width(6.dp))
                Text("Use fones de ouvido", style = VoxType.small, color = c.muted)
            }
        }
        Panel(onClick = { showSteps = !showSteps }) {
            Row(verticalAlignment = Alignment.CenterVertically) {
                Text("COMO FAZER", style = VoxType.caps, color = c.muted, modifier = Modifier.weight(1f))
                Text(if (showSteps) "ocultar" else "mostrar", style = VoxType.small, color = c.muted)
            }
            if (showSteps) {
                for (s in ex.steps) {
                    Row(Modifier.padding(top = 8.dp)) {
                        Text("•", style = VoxType.body, color = c.accent, modifier = Modifier.width(16.dp))
                        Text(s, style = VoxType.small, color = c.text)
                    }
                }
            }
        }
        Spacer(Modifier.height(16.dp))
        Row(verticalAlignment = Alignment.Bottom) {
            Text(phase.label, style = VoxType.title, color = if (phase == Phase.SING) c.accent else c.text, modifier = Modifier.weight(1f))
            Column(horizontalAlignment = Alignment.End) {
                Text("Tom ${NoteNames.full(currentRoot, naming)}", style = VoxType.label, color = c.text)
                Text("${(rep + 1).coerceAtMost(roots.size)} de ${roots.size}", style = VoxType.small, color = c.muted)
            }
        }
        Spacer(Modifier.height(8.dp))
        PianoRoll(
            notes = notes,
            clock = { synth.positionMs() },
            tick = tick,
            modifier = Modifier.weight(1f).fillMaxWidth(),
            trace = trace,
            naming = naming,
            windowMs = 7000f,
            playhead = 0.25f,
            accuracy = { scorer.noteAccuracy(it) },
        )
        Spacer(Modifier.height(8.dp))
        val last = results.lastOrNull()
        Text(
            if (last == null) {
                "Ouça primeiro, depois cante quando a barra ficar colorida."
            } else {
                "Última: ${(last * 100).roundToInt()}%   ·   Média: ${(results.average() * 100).roundToInt()}%"
            },
            style = VoxType.small,
            color = c.muted,
            textAlign = TextAlign.Center,
            modifier = Modifier.fillMaxWidth(),
        )
        Spacer(Modifier.height(12.dp))
        Row(
            Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.SpaceEvenly,
            verticalAlignment = Alignment.CenterVertically,
        ) {
            RoundButton(VoxIcons.Remove, "Tom abaixo", { transpose-- }, size = 48.dp, primary = false)
            RoundButton(
                if (running) VoxIcons.Pause else VoxIcons.Play,
                if (running) "Pausar" else "Começar",
                {
                    if (!running && rep >= roots.size) {
                        rep = 0
                        results.clear()
                    }
                    running = !running
                },
                size = 72.dp,
            )
            RoundButton(VoxIcons.Add, "Tom acima", { transpose++ }, size = 48.dp, primary = false)
        }
        ToggleRow(
            "Tocar o alvo enquanto canto",
            guide,
            {
                guide = it
                guideTrack[0]?.enabled = it
            },
            detail = "Ajuda no começo. Use fones.",
        )
        Spacer(Modifier.height(8.dp))
    }
}

@Composable
private fun BreathContent(ex: Exercise) {
    val c = Vox.colors
    val cycles = 5
    var running by remember { mutableStateOf(false) }
    var cycle by remember { mutableIntStateOf(0) }
    var phaseName by remember { mutableStateOf("Pronto?") }
    var seconds by remember { mutableIntStateOf(0) }
    var feedback by remember { mutableStateOf("") }
    val progress = remember { mutableFloatStateOf(0.15f) }
    val level = remember { mutableFloatStateOf(-100f) }

    MicActive(enabled = running)

    LaunchedEffect(running) {
        if (!running) return@LaunchedEffect
        if (cycle >= cycles) cycle = 0
        val t0 = withFrameMillis { it } - cycle * 16_000L
        val exhale = FloatList(512)
        var lastFrame = MicEngine.frames
        try {
            while (cycle < cycles) {
                withFrameMillis { now ->
                    val e = now - t0 - cycle * 16_000L
                    level.floatValue = MicEngine.latestLevel()
                    when {
                        e < 4_000 -> {
                            phaseName = "Inspire"
                            seconds = 4 - (e / 1000).toInt()
                            progress.floatValue = 0.15f + 0.85f * e / 4000f
                            exhale.clear()
                        }
                        e < 8_000 -> {
                            phaseName = "Segure"
                            seconds = 4 - ((e - 4000) / 1000).toInt()
                            progress.floatValue = 1f
                        }
                        e < 16_000 -> {
                            phaseName = "Solte em \"sss\""
                            seconds = 8 - ((e - 8000) / 1000).toInt()
                            progress.floatValue = 1f - 0.85f * (e - 8000) / 8000f
                            val f = MicEngine.frames
                            var i = max(lastFrame, f - 64)
                            while (i < f) {
                                exhale.add(MicEngine.levelDb[MicEngine.index(i)])
                                i++
                            }
                        }
                        else -> {
                            feedback = steadiness(exhale)
                            cycle++
                        }
                    }
                    lastFrame = MicEngine.frames
                }
            }
            phaseName = "Concluído"
            running = false
        } finally {
            if (cycle < cycles && phaseName != "Concluído") phaseName = "Pausado"
        }
    }

    Column(Modifier.fillMaxSize(), horizontalAlignment = Alignment.CenterHorizontally) {
        Panel {
            for (s in ex.steps) {
                Row(Modifier.padding(vertical = 3.dp)) {
                    Text("•", style = VoxType.body, color = c.accent, modifier = Modifier.width(16.dp))
                    Text(s, style = VoxType.small, color = c.text)
                }
            }
        }
        Box(Modifier.weight(1f).fillMaxWidth(), contentAlignment = Alignment.Center) {
            Canvas(Modifier.fillMaxSize()) {
                val r = size.minDimension / 2f * 0.9f
                drawCircle(c.line, radius = r, style = Stroke(width = 1.dp.toPx()))
                drawCircle(c.accent.copy(alpha = 0.18f), radius = r * progress.floatValue)
                drawCircle(c.accent, radius = r * progress.floatValue, style = Stroke(width = 2.dp.toPx()), center = Offset(size.width / 2f, size.height / 2f))
            }
            Column(horizontalAlignment = Alignment.CenterHorizontally) {
                Text(phaseName, style = VoxType.title, color = c.text)
                if (running) Text("$seconds", style = VoxType.heading, color = c.muted)
            }
        }
        Text(
            if (feedback.isEmpty()) "Ciclo ${(cycle + 1).coerceAtMost(cycles)} de $cycles" else "Ciclo ${(cycle + 1).coerceAtMost(cycles)} de $cycles  ·  $feedback",
            style = VoxType.small,
            color = c.muted,
        )
        Spacer(Modifier.height(8.dp))
        LevelBar({ level.floatValue })
        Spacer(Modifier.height(16.dp))
        RoundButton(
            if (running) VoxIcons.Pause else VoxIcons.Play,
            if (running) "Pausar" else "Começar",
            {
                if (!running && cycle >= cycles) {
                    cycle = 0
                    feedback = ""
                }
                running = !running
            },
            size = 72.dp,
        )
        Spacer(Modifier.height(8.dp))
        Text(
            "Recomeçar",
            style = VoxType.small,
            color = c.muted,
            modifier = Modifier
                .clickable {
                    running = false
                    cycle = 0
                    feedback = ""
                    phaseName = "Pronto?"
                    progress.floatValue = 0.15f
                }
                .padding(8.dp),
        )
        Spacer(Modifier.height(8.dp))
    }
}

/** Judges how even the "sss" was from the spread of the input level. */
private fun steadiness(levels: FloatList): String {
    var n = 0
    var sum = 0.0
    for (i in 0 until levels.size) {
        if (levels[i] > -55f) {
            sum += levels[i]
            n++
        }
    }
    if (n < 20) return "não ouvi o \"sss\""
    val mean = sum / n
    var v = 0.0
    for (i in 0 until levels.size) if (levels[i] > -55f) v += (levels[i] - mean) * (levels[i] - mean)
    val sd = sqrt(v / n)
    return when {
        sd < 2.5 -> "sopro bem constante"
        sd < 5.0 -> "quase constante"
        else -> "sopro irregular"
    }
}

package com.raulhrn.vox.ui.screens

import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxHeight
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.DisposableEffect
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.mutableLongStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.runtime.withFrameMillis
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.unit.dp
import com.raulhrn.vox.audio.Synth
import com.raulhrn.vox.audio.SynthTrack
import com.raulhrn.vox.core.Backing
import com.raulhrn.vox.core.BackingStyle
import com.raulhrn.vox.core.Exercise
import com.raulhrn.vox.core.ExerciseTimeline
import com.raulhrn.vox.core.Harmony
import com.raulhrn.vox.core.HarmonyMode
import com.raulhrn.vox.core.Key
import com.raulhrn.vox.core.NoteEvent
import com.raulhrn.vox.core.NoteNames
import com.raulhrn.vox.core.SecondVoiceLessons
import com.raulhrn.vox.core.VoiceType
import com.raulhrn.vox.core.dbToGain
import com.raulhrn.vox.data.Prefs
import com.raulhrn.vox.ui.ChoiceGrid
import com.raulhrn.vox.ui.Header
import com.raulhrn.vox.ui.ListRow
import com.raulhrn.vox.ui.Nav
import com.raulhrn.vox.ui.Panel
import com.raulhrn.vox.ui.PianoRoll
import com.raulhrn.vox.ui.RollKind
import com.raulhrn.vox.ui.RollNote
import com.raulhrn.vox.ui.Route
import com.raulhrn.vox.ui.ScreenPadding
import com.raulhrn.vox.ui.Section
import com.raulhrn.vox.ui.Vox
import com.raulhrn.vox.ui.VoxButton
import com.raulhrn.vox.ui.VoxIcons
import com.raulhrn.vox.ui.VoxType
import kotlin.math.max
import kotlin.math.roundToInt

/** The example phrase ("Ode à Alegria") placed comfortably in the singer's range. */
private fun exampleMelody(range: IntRange): Pair<List<NoteEvent>, Key> {
    val root = (range.first + range.last) / 2 - 4
    val ex = Exercise("exemplo", "", "", "", emptyList(), pattern = SecondVoiceLessons.example, beats = SecondVoiceLessons.exampleBeats, bpm = 96)
    return ExerciseTimeline.melody(ex, root) to Key(Math.floorMod(root, 12), false)
}

/** A short example on a piano roll, with a synth that plays chosen lines. */
@Composable
private fun ExamplePlayer(notes: List<RollNote>, lengthMs: Int, height: Int = 220, controls: @Composable (play: (List<SynthTrack>) -> Unit, playing: Boolean, stop: () -> Unit) -> Unit) {
    val synth = remember { Synth() }
    var playing by remember { mutableStateOf(false) }
    val tick = remember { mutableLongStateOf(0L) }
    DisposableEffect(Unit) { onDispose { synth.stop() } }
    LaunchedEffect(playing) {
        if (!playing) return@LaunchedEffect
        while (true) {
            val done = withFrameMillis {
                tick.longValue = it
                synth.positionMs() > lengthMs + 300
            }
            if (done) break
        }
        synth.stop()
        playing = false
    }
    synth.a4 = Prefs.a4
    PianoRoll(
        notes = notes,
        clock = { if (playing) synth.positionMs() else 0f },
        tick = tick,
        modifier = Modifier.fillMaxWidth().height(height.dp),
        naming = Prefs.naming,
        windowMs = (lengthMs + 1200).toFloat(),
        playhead = 0.04f,
    )
    controls(
        { tracks ->
            synth.start(tracks, 0f)
            playing = true
        },
        playing,
        {
            synth.stop()
            playing = false
        },
    )
}

@Composable
fun LessonsScreen(nav: Nav) {
    val c = Vox.colors
    LazyColumn(Modifier.fillMaxSize().padding(horizontal = ScreenPadding)) {
        item { Header("Tipos de segunda voz", onBack = { nav.pop() }, subtitle = "Aprender") }
        item {
            Text(
                "Cada tipo tem um som e uma função. Comece pelos de nível 1 e avance quando a afinação estiver firme.",
                style = VoxType.small,
                color = c.muted,
                modifier = Modifier.padding(bottom = 8.dp),
            )
        }
        items(HarmonyMode.entries.toList(), key = { it.name }) { mode ->
            Row(verticalAlignment = Alignment.CenterVertically) {
                LevelDots(mode.level)
                Spacer(Modifier.width(12.dp))
                Box(Modifier.weight(1f)) {
                    ListRow(mode.label, mode.summary) { nav.push(Route.Lesson(mode)) }
                }
            }
        }
        item { Spacer(Modifier.height(24.dp)) }
    }
}

@Composable
private fun LevelDots(level: Int) {
    val c = Vox.colors
    Column(verticalArrangement = Arrangement.spacedBy(3.dp)) {
        for (i in 3 downTo 1) {
            Box(Modifier.size(6.dp).clip(CircleShape).background(if (i <= level) c.accent else c.line))
        }
    }
}

@Composable
fun LessonScreen(nav: Nav, mode: HarmonyMode) {
    val c = Vox.colors
    val lesson = remember(mode) { SecondVoiceLessons.of(mode) }
    val range = Prefs.rangeLow..Prefs.rangeHigh
    val example = remember(mode, range) {
        val (melody, key) = exampleMelody(range)
        melody to Harmony.voice(melody, key, mode, range = range)
    }
    val (melody, voice) = example
    val length = max(melody.lastOrNull()?.endMs ?: 0, voice.lastOrNull()?.endMs ?: 0)
    val notes = remember(example) {
        melody.map { RollNote(it.startMs, it.durMs, it.midi, RollKind.MELODY) } +
            voice.mapIndexed { i, n -> RollNote(n.startMs, n.durMs, n.midi, RollKind.TARGET, i) }
    }
    Column(Modifier.fillMaxSize().padding(horizontal = ScreenPadding)) {
        Header(mode.label, onBack = { nav.pop() }, subtitle = "Segunda voz · nível ${mode.level}")
        Column(Modifier.weight(1f).verticalScroll(rememberScrollState())) {
            Text(lesson.whenToUse, style = VoxType.body, color = c.text)
            Section("Como pensar")
            for (step in lesson.howTo) {
                Row(Modifier.padding(vertical = 3.dp)) {
                    Text("•", style = VoxType.body, color = c.accent, modifier = Modifier.width(18.dp))
                    Text(step, style = VoxType.body, color = c.text)
                }
            }
            Section("Exemplo: \"Ode à Alegria\"")
            Legend(listOf(c.melody to "melodia", c.accent to "segunda voz"))
            Spacer(Modifier.height(6.dp))
            ExamplePlayer(notes, length) { play, playing, stop ->
                Spacer(Modifier.height(10.dp))
                Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                    VoxButton(
                        if (playing) "Parar" else "Ouvir os dois",
                        {
                            if (playing) stop() else play(listOf(SynthTrack.of(melody, 0.26f), SynthTrack.of(voice, 0.24f, bright = true)))
                        },
                        Modifier.weight(1f),
                        icon = if (playing) VoxIcons.Stop else VoxIcons.Play,
                        primary = false,
                    )
                    VoxButton(
                        "Só a 2ª voz",
                        { play(listOf(SynthTrack.of(voice, 0.26f, bright = true))) },
                        Modifier.weight(1f),
                        primary = false,
                    )
                }
            }
            Spacer(Modifier.height(16.dp))
        }
        VoxButton("Praticar este tipo", { nav.push(Route.Exercise(lesson.exerciseId)) }, Modifier.fillMaxWidth(), icon = VoxIcons.Mic)
        Spacer(Modifier.height(12.dp))
    }
}

@Composable
private fun Legend(items: List<Pair<Color, String>>) {
    val c = Vox.colors
    Row(horizontalArrangement = Arrangement.spacedBy(14.dp), verticalAlignment = Alignment.CenterVertically) {
        for ((color, label) in items) {
            Row(verticalAlignment = Alignment.CenterVertically) {
                Box(Modifier.size(width = 14.dp, height = 6.dp).clip(RoundedCornerShape(3.dp)).background(color))
                Spacer(Modifier.width(6.dp))
                Text(label, style = VoxType.small, color = c.muted)
            }
        }
    }
}

private fun backingExercise(style: BackingStyle, voices: Int): String = when (style) {
    BackingStyle.HARMONY -> if (voices == 2) "backing-trio" else "backing-harmonia"
    BackingStyle.PAD -> "backing-pad"
    BackingStyle.ECHO -> "backing-resposta"
    BackingStyle.DOUBLE -> "backing-dobra"
}

/**
 * Backing vocal lab: pick the lead singer's voice type and a style; see where your part goes,
 * how loud each voice should be, and listen to the balanced arrangement.
 */
@Composable
fun BackingLabScreen(nav: Nav) {
    val c = Vox.colors
    val naming = Prefs.naming
    val singer = Prefs.rangeLow..Prefs.rangeHigh
    val singerType = VoiceType.classify(singer.first, singer.last)
    var lead by remember { mutableStateOf(singerType) }
    var style by remember { mutableStateOf(BackingStyle.HARMONY) }
    var voices by remember { mutableIntStateOf(1) }
    var part by remember { mutableIntStateOf(0) }

    val plan = remember(lead, style, voices, singer) {
        val (melody, key) = exampleMelody(singer)
        // Echoes need rests: split the example into two phrases.
        val withRests = if (style == BackingStyle.ECHO) {
            melody.map { if (it.startMs >= 4000) it.copy(startMs = it.startMs + 1800) else it }
        } else {
            melody
        }
        Backing.arrange(withRests, key, style, lead, singer, voices)
    }
    val mine = plan.parts.getOrNull(part.coerceIn(0, max(0, plan.parts.size - 1)))
    val notes = remember(plan, mine) {
        plan.lead.map { RollNote(it.startMs, it.durMs, it.midi, RollKind.MELODY) } +
            plan.parts.filter { it !== mine }.flatMap { p -> p.notes.map { RollNote(it.startMs, it.durMs, it.midi, RollKind.PART) } } +
            (mine?.notes ?: emptyList()).mapIndexed { i, n -> RollNote(n.startMs, n.durMs, n.midi, RollKind.TARGET, i) }
    }
    val length = (plan.lead + plan.parts.flatMap { it.notes }).maxOfOrNull { it.endMs } ?: 0

    Column(Modifier.fillMaxSize().padding(horizontal = ScreenPadding)) {
        Header("Backing vocal", onBack = { nav.pop() }, subtitle = "Laboratório")
        Column(Modifier.weight(1f).verticalScroll(rememberScrollState())) {
            Text("VOZ DO REGENTE", style = VoxType.caps, color = c.muted)
            Spacer(Modifier.height(6.dp))
            ChoiceGrid(VoiceType.entries.toList(), columns = 3, selected = { it == lead }, label = { it.label }, onSelect = { lead = it })
            Row(Modifier.padding(top = 6.dp), verticalAlignment = Alignment.CenterVertically) {
                Text(
                    "Sua voz: ${singerType.label} (${NoteNames.full(singer.first, naming)}–${NoteNames.full(singer.last, naming)})",
                    style = VoxType.small,
                    color = c.muted,
                    modifier = Modifier.weight(1f),
                )
                Text("medir", style = VoxType.label, color = c.accent, modifier = Modifier.clickable { nav.push(Route.RangeTest) }.padding(6.dp))
            }
            Spacer(Modifier.height(10.dp))
            Text("ESTILO", style = VoxType.caps, color = c.muted)
            Spacer(Modifier.height(6.dp))
            ChoiceGrid(BackingStyle.entries.toList(), columns = 4, selected = { it == style }, label = { it.label }, onSelect = { style = it })
            Text(style.summary, style = VoxType.small, color = c.muted, modifier = Modifier.padding(top = 6.dp))
            Spacer(Modifier.height(10.dp))
            ChoiceGrid(
                listOf(1, 2),
                columns = 2,
                selected = { it == voices },
                label = { if (it == 1) "1 voz de backing" else "2 vozes de backing" },
                onSelect = {
                    voices = it
                    if (it == 1) part = 0
                },
            )
            if (plan.parts.size == 2) {
                Spacer(Modifier.height(6.dp))
                ChoiceGrid(
                    listOf(0, 1),
                    columns = 2,
                    selected = { it == part },
                    label = { if (it == 0) "Cantar a aguda" else "Cantar a grave" },
                    onSelect = { part = it },
                )
            }

            Section("Equilíbrio")
            Panel {
                BalanceRow("Regente (${lead.label})", "melodia", 0f, c.melody)
                for (p in plan.parts) {
                    Spacer(Modifier.height(10.dp))
                    BalanceRow(
                        if (p === mine) "${p.name} · você" else p.name,
                        p.where,
                        p.gainDb,
                        if (p === mine) c.accent else c.rose,
                    )
                }
            }

            Section("Exemplo: \"Ode à Alegria\"")
            Legend(listOf(c.melody to "regente", c.accent to "sua parte", c.rose to "outra voz"))
            Spacer(Modifier.height(6.dp))
            ExamplePlayer(notes, length) { play, playing, stop ->
                Spacer(Modifier.height(10.dp))
                Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                    VoxButton(
                        if (playing) "Parar" else "Ouvir equilibrado",
                        {
                            if (playing) {
                                stop()
                            } else {
                                play(
                                    listOf(SynthTrack.of(plan.lead, 0.3f)) +
                                        plan.parts.map { SynthTrack.of(it.notes, 0.3f * dbToGain(it.gainDb) * 1.4f, bright = it === mine) },
                                )
                            }
                        },
                        Modifier.weight(1f),
                        icon = if (playing) VoxIcons.Stop else VoxIcons.Play,
                        primary = false,
                    )
                    VoxButton(
                        "Só a sua parte",
                        { mine?.let { play(listOf(SynthTrack.of(it.notes, 0.28f, bright = true))) } },
                        Modifier.weight(1f),
                        primary = false,
                        enabled = mine != null,
                    )
                }
            }

            Section("Dicas para esta combinação")
            for (tip in plan.tips) {
                Row(Modifier.padding(vertical = 4.dp)) {
                    Text("•", style = VoxType.body, color = c.accent, modifier = Modifier.width(18.dp))
                    Text(tip, style = VoxType.small, color = c.text)
                }
            }
            Spacer(Modifier.height(16.dp))
        }
        VoxButton(
            "Praticar",
            { nav.push(Route.Exercise(backingExercise(style, voices), lead)) },
            Modifier.fillMaxWidth(),
            icon = VoxIcons.Mic,
        )
        Spacer(Modifier.height(12.dp))
    }
}

/** One voice of the arrangement with its loudness relative to the lead (0 dB). */
@Composable
private fun BalanceRow(name: String, where: String, db: Float, color: Color) {
    val c = Vox.colors
    Row(verticalAlignment = Alignment.CenterVertically) {
        Column(Modifier.weight(1f)) {
            Text(name, style = VoxType.label, color = c.text)
            if (where.isNotEmpty()) Text(where, style = VoxType.small, color = c.muted)
        }
        Text(if (db == 0f) "0 dB" else "${db.roundToInt()} dB", style = VoxType.label, color = c.text)
    }
    Spacer(Modifier.height(6.dp))
    // Bar length follows perceived loudness: about half for every 10 dB.
    val share = Math.pow(2.0, db / 10.0).toFloat().coerceIn(0.05f, 1f)
    Box(Modifier.fillMaxWidth().height(6.dp).clip(RoundedCornerShape(3.dp)).background(c.surface2)) {
        Box(Modifier.fillMaxWidth(share).fillMaxHeight().clip(RoundedCornerShape(3.dp)).background(color))
    }
}

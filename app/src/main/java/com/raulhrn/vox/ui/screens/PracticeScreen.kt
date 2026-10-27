package com.raulhrn.vox.ui.screens

import android.net.Uri
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.Slider
import androidx.compose.material3.SliderDefaults
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.DisposableEffect
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.SideEffect
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
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.unit.dp
import com.raulhrn.vox.audio.MicEngine
import com.raulhrn.vox.audio.SongPlayer
import com.raulhrn.vox.audio.Synth
import com.raulhrn.vox.audio.SynthTrack
import com.raulhrn.vox.core.Harmony
import com.raulhrn.vox.core.HarmonyMode
import com.raulhrn.vox.core.Key
import com.raulhrn.vox.core.NoteEvent
import com.raulhrn.vox.core.NoteNames
import com.raulhrn.vox.core.Phrases
import com.raulhrn.vox.core.Role
import com.raulhrn.vox.core.Scorer
import com.raulhrn.vox.core.TimedNote
import com.raulhrn.vox.data.Prefs
import com.raulhrn.vox.data.Song
import com.raulhrn.vox.data.SongStore
import com.raulhrn.vox.ui.ChoiceGrid
import com.raulhrn.vox.ui.Chip
import com.raulhrn.vox.ui.Header
import com.raulhrn.vox.ui.IconBtn
import com.raulhrn.vox.ui.MicActive
import com.raulhrn.vox.ui.MicGate
import com.raulhrn.vox.ui.Nav
import com.raulhrn.vox.ui.Panel
import com.raulhrn.vox.ui.PianoRoll
import com.raulhrn.vox.ui.RollKind
import com.raulhrn.vox.ui.RollNote
import com.raulhrn.vox.ui.RoundButton
import com.raulhrn.vox.ui.ScreenPadding
import com.raulhrn.vox.ui.Stepper
import com.raulhrn.vox.ui.Trace
import com.raulhrn.vox.ui.Vox
import com.raulhrn.vox.ui.VoxButton
import com.raulhrn.vox.ui.VoxIcons
import com.raulhrn.vox.ui.VoxType
import com.raulhrn.vox.ui.formatTime
import kotlinx.coroutines.delay
import kotlin.math.abs
import kotlin.math.max
import kotlin.math.roundToInt

@Composable
fun PracticeScreen(nav: Nav, id: String) {
    val song = SongStore.find(id)
    Column(Modifier.fillMaxSize().padding(horizontal = ScreenPadding)) {
        if (song == null) {
            Header("Música", onBack = { nav.pop() })
            Text(
                if (SongStore.loaded.value) "Música não encontrada." else "Carregando…",
                style = VoxType.body,
                color = Vox.colors.muted,
            )
            return@Column
        }
        MicGate { PracticeContent(nav, song) }
    }
}

private val SPEEDS = listOf(0.5f, 0.75f, 1f)

@Composable
private fun PracticeContent(nav: Nav, song: Song) {
    val c = Vox.colors
    val context = LocalContext.current
    val naming = Prefs.naming

    var key by remember { mutableStateOf(song.key) }
    var mode by remember { mutableStateOf(song.mode) }
    var octave by remember { mutableIntStateOf(song.octave) }
    LaunchedEffect(key, mode, octave) {
        val saved = SongStore.find(song.id)
        if (saved != null && (saved.key != key || saved.mode != mode || saved.octave != octave)) {
            SongStore.upsert(saved.copy(key = key, mode = mode, octave = octave))
        }
    }

    val harmony = remember(song.notes, key, mode, octave) { Harmony.voice(song.notes, key, mode, octave * 12) }
    val scorer = remember(harmony) { Scorer(harmony.map { TimedNote(it.startMs, it.durMs, it.midi, Role.TARGET) }) }
    val roll = remember(song.notes, harmony) {
        song.notes.map { RollNote(it.startMs, it.durMs, it.midi, RollKind.MELODY) } +
            harmony.mapIndexed { i, n -> RollNote(n.startMs, n.durMs, n.midi, RollKind.TARGET, i) }
    }
    val phrases = remember(song.notes) { Phrases.split(song.notes) }
    val totalMs = max(song.durationMs, song.notes.lastOrNull()?.endMs ?: 0).toFloat()

    val synth = remember { Synth() }
    val player = remember(song.audioUri) { song.audioUri?.let { SongPlayer(context, Uri.parse(it)) } }
    var playerState by remember { mutableIntStateOf(0) } // 0 loading, 1 ready, 2 failed
    DisposableEffect(player) {
        player?.prepare { playerState = 1 }
        onDispose {
            player?.release()
            synth.stop()
        }
    }
    LaunchedEffect(player) {
        if (player == null) return@LaunchedEffect
        while (playerState == 0) {
            if (player.failed) playerState = 2
            delay(250)
        }
    }

    var playing by remember { mutableStateOf(false) }
    val pausedAt = remember { mutableFloatStateOf(0f) }
    var sliderPos by remember { mutableFloatStateOf(0f) }
    var dragging by remember { mutableStateOf<Float?>(null) }
    var speed by remember { mutableFloatStateOf(1f) }
    var audioOn by remember { mutableStateOf(true) }
    var melodyOn by remember { mutableStateOf(song.audioUri == null) }
    var voiceOn by remember { mutableStateOf(true) }
    var loop by remember { mutableStateOf(false) }
    var phraseIndex by remember { mutableIntStateOf(0) }
    var showSettings by remember { mutableStateOf(false) }
    var showHelp by remember { mutableStateOf(false) }
    var confirmDelete by remember { mutableStateOf(false) }
    var accuracy by remember { mutableIntStateOf(-1) }
    var editing by remember { mutableStateOf(false) }
    var selected by remember { mutableIntStateOf(-1) }
    val trace = remember { Trace() }
    val tick = remember { mutableLongStateOf(0L) }

    val melodyTrack = remember(song.notes) { SynthTrack.of(song.notes, gain = 0.26f) }
    val voiceTrack = remember(harmony) { SynthTrack.of(harmony, gain = 0.24f, bright = true) }
    SideEffect {
        melodyTrack.enabled = melodyOn
        voiceTrack.enabled = voiceOn
        synth.a4 = Prefs.a4
        player?.setVolume(if (audioOn) 1f else 0f)
    }

    val useAudio = player != null && playerState == 1

    fun now(): Float = when {
        !playing -> pausedAt.floatValue
        useAudio -> player!!.positionMs()
        else -> synth.positionMs()
    }

    fun start(from: Float) {
        val f = from.coerceIn(0f, totalMs)
        if (useAudio) {
            player!!.setVolume(if (audioOn) 1f else 0f)
            player.play(f.roundToInt(), speed)
        }
        synth.start(listOf(melodyTrack, voiceTrack), f, speed)
        trace.clear()
        pausedAt.floatValue = f
        playing = true
    }

    fun pause() {
        pausedAt.floatValue = now()
        playing = false
        player?.pause()
        synth.stop()
        sliderPos = pausedAt.floatValue
    }

    fun seek(to: Float) {
        val t = to.coerceIn(0f, totalMs)
        if (playing) {
            pause()
            start(t)
        } else {
            pausedAt.floatValue = t
            sliderPos = t
            trace.clear()
        }
    }

    fun pickNote(t: Float, m: Float) {
        var best = -1
        var bestScore = Float.MAX_VALUE
        song.notes.forEachIndexed { i, n ->
            val dt = when {
                t < n.startMs -> n.startMs - t
                t > n.endMs -> t - n.endMs
                else -> 0f
            }
            val score = dt / 150f + abs(n.midi - m)
            if (score < bestScore) {
                bestScore = score
                best = i
            }
        }
        selected = if (bestScore < 4f) best else -1
        if (selected >= 0) synth.preview(song.notes[selected].midi)
    }

    fun editSelected(op: (NoteEvent) -> NoteEvent?) {
        val saved = SongStore.find(song.id) ?: return
        val i = selected
        if (i !in saved.notes.indices) return
        val list = saved.notes.toMutableList()
        val changed = op(list[i])
        if (changed == null) {
            list.removeAt(i)
            selected = -1
        } else {
            list[i] = changed
            synth.preview(changed.midi)
        }
        SongStore.upsert(saved.copy(notes = list))
    }

    fun goToPhrase(i: Int) {
        if (phrases.isEmpty()) return
        phraseIndex = i.coerceIn(0, phrases.lastIndex)
        seek(max(0f, phrases[phraseIndex].startMs - 800f))
    }

    // A new second voice while playing: restart the guide from the current position.
    LaunchedEffect(voiceTrack) {
        if (playing) synth.start(listOf(melodyTrack, voiceTrack), now(), speed)
    }

    MicActive(enabled = playing)

    LaunchedEffect(playing) {
        if (!playing) return@LaunchedEffect
        var lastFrame = MicEngine.frames
        var lastUi = 0L
        while (true) {
            withFrameMillis { frameMs ->
                val pos = now()
                val f = MicEngine.frames
                val nowNs = System.nanoTime()
                var i = max(lastFrame, f - 64)
                while (i < f) {
                    val idx = MicEngine.index(i)
                    val ageMs = (nowNs - MicEngine.timeNs[idx]) / 1e6f + MicEngine.LATENCY_MS
                    val t = pos - ageMs * speed
                    val m = MicEngine.midi[idx]
                    scorer.add(t, m)
                    trace.add(t, m)
                    i++
                }
                lastFrame = f

                if (phrases.isNotEmpty()) {
                    if (loop) {
                        val ph = phrases[phraseIndex]
                        if (pos > ph.endMs + 500f) seek(max(0f, ph.startMs - 800f))
                    } else {
                        var p = phraseIndex
                        while (p < phrases.lastIndex && pos >= phrases[p + 1].startMs - 300) p++
                        while (p > 0 && pos < phrases[p].startMs - 300) p--
                        if (p != phraseIndex) phraseIndex = p
                    }
                }
                if (pos >= totalMs + 600f) {
                    pause()
                    pausedAt.floatValue = 0f
                    sliderPos = 0f
                }
                if (frameMs - lastUi > 120) {
                    lastUi = frameMs
                    if (playing) sliderPos = pos
                    val a = scorer.frameAccuracy()
                    accuracy = if (a < 0f) -1 else (a * 100).roundToInt()
                }
                tick.longValue = frameMs
            }
        }
    }

    Column(Modifier.fillMaxSize()) {
        Header(
            song.title,
            onBack = { nav.pop() },
            subtitle = "${key.label(naming)} · ${mode.label}",
        ) {
            IconBtn(VoxIcons.Tune, "Ajustes", { showSettings = !showSettings }, tint = if (showSettings) c.accent else null)
            IconBtn(VoxIcons.Delete, "Excluir", { confirmDelete = true })
        }

        if (confirmDelete) {
            Panel {
                Text("Excluir \"${song.title}\"?", style = VoxType.body, color = c.text)
                Spacer(Modifier.height(10.dp))
                Row(horizontalArrangement = Arrangement.spacedBy(10.dp)) {
                    VoxButton("Cancelar", { confirmDelete = false }, Modifier.weight(1f), primary = false)
                    VoxButton("Excluir", {
                        pause()
                        SongStore.delete(song.id)
                        nav.pop()
                    }, Modifier.weight(1f))
                }
            }
            Spacer(Modifier.height(8.dp))
        }

        if (showSettings) {
            Column(Modifier.heightIn(max = 400.dp).verticalScroll(rememberScrollState())) {
                Text("TOM DA MÚSICA", style = VoxType.caps, color = c.muted)
                Spacer(Modifier.height(6.dp))
                ChoiceGrid(
                    (0 until 12).toList(),
                    columns = 6,
                    selected = { it == key.tonic },
                    label = { NoteNames.pitchClass(it, naming, Key(it, key.minor).prefersFlats) },
                    onSelect = { key = Key(it, key.minor) },
                )
                Spacer(Modifier.height(6.dp))
                ChoiceGrid(
                    listOf(false, true),
                    columns = 2,
                    selected = { it == key.minor },
                    label = { if (it) "Menor" else "Maior" },
                    onSelect = { key = Key(key.tonic, it) },
                )
                Spacer(Modifier.height(14.dp))
                Text("SEGUNDA VOZ", style = VoxType.caps, color = c.muted)
                Spacer(Modifier.height(6.dp))
                ChoiceGrid(
                    HarmonyMode.entries.toList(),
                    columns = 4,
                    selected = { it == mode },
                    label = { it.label },
                    onSelect = { mode = it },
                )
                Stepper(
                    "Oitava da 2ª voz",
                    if (octave > 0) "+$octave" else "$octave",
                    { octave = (octave - 1).coerceAtLeast(-2) },
                    { octave = (octave + 1).coerceAtMost(2) },
                )
                VoxButton(
                    "Corrigir notas da melodia",
                    {
                        if (playing) pause()
                        editing = true
                        showSettings = false
                    },
                    Modifier.fillMaxWidth(),
                    primary = false,
                )
                Spacer(Modifier.height(8.dp))
            }
        }

        PianoRoll(
            notes = roll,
            clock = { now() },
            tick = tick,
            modifier = Modifier.weight(1f).fillMaxWidth(),
            trace = trace,
            naming = naming,
            flats = key.prefersFlats,
            windowMs = 6000f,
            accuracy = { scorer.noteAccuracy(it) },
            selected = if (editing) selected else -1,
            onTap = if (editing) ({ t, m -> pickNote(t, m) }) else null,
            onScrub = if (!playing) ({ d -> seek(pausedAt.floatValue + d) }) else null,
        )

        if (editing) {
            Spacer(Modifier.height(8.dp))
            Panel {
                Row(verticalAlignment = Alignment.CenterVertically) {
                    val n = song.notes.getOrNull(selected)
                    Text(
                        if (n == null) "Toque numa nota da melodia" else "${NoteNames.full(n.midi, naming, key.prefersFlats)} · ${formatTime(n.startMs.toFloat())}",
                        style = VoxType.label,
                        color = c.text,
                        modifier = Modifier.weight(1f),
                    )
                    Text(
                        "concluir",
                        style = VoxType.label,
                        color = c.accent,
                        modifier = Modifier
                            .clickable {
                                editing = false
                                selected = -1
                                synth.stop()
                            }
                            .padding(6.dp),
                    )
                }
                Spacer(Modifier.height(8.dp))
                ChoiceGrid(
                    EDITS,
                    columns = EDITS.size,
                    selected = { false },
                    label = { it.first },
                    onSelect = { e -> editSelected(e.second) },
                )
                Spacer(Modifier.height(4.dp))
                Text("Arraste a faixa para os lados para andar pela música.", style = VoxType.small, color = c.muted)
            }
        }

        Row(Modifier.fillMaxWidth().padding(top = 8.dp), verticalAlignment = Alignment.CenterVertically) {
            Text(
                "${formatTime(dragging ?: sliderPos)} / ${formatTime(totalMs)}",
                style = VoxType.small,
                color = c.muted,
                modifier = Modifier.weight(1f),
            )
            Text(
                when {
                    playerState == 2 -> "áudio indisponível"
                    accuracy < 0 -> if (phrases.isNotEmpty()) "frase ${phraseIndex + 1} de ${phrases.size}" else ""
                    else -> "2ª voz $accuracy%"
                },
                style = VoxType.label,
                color = when {
                    playerState == 2 -> c.bad
                    accuracy < 0 -> c.muted
                    accuracy >= 70 -> c.accent
                    accuracy >= 40 -> c.warn
                    else -> c.bad
                },
            )
        }
        Slider(
            value = (dragging ?: sliderPos).coerceIn(0f, max(1f, totalMs)),
            onValueChange = { dragging = it },
            onValueChangeFinished = {
                dragging?.let { seek(it) }
                dragging = null
            },
            valueRange = 0f..max(1f, totalMs),
            colors = SliderDefaults.colors(
                thumbColor = c.text,
                activeTrackColor = c.text,
                inactiveTrackColor = c.line,
            ),
        )

        Row(
            Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.SpaceEvenly,
            verticalAlignment = Alignment.CenterVertically,
        ) {
            IconBtn(VoxIcons.Restart, "Do começo", {
                scorer.reset()
                accuracy = -1
                phraseIndex = 0
                seek(0f)
            })
            IconBtn(VoxIcons.Prev, "Frase anterior", { goToPhrase(phraseIndex - 1) })
            RoundButton(
                if (playing) VoxIcons.Pause else VoxIcons.Play,
                if (playing) "Pausar" else "Tocar",
                { if (playing) pause() else start(pausedAt.floatValue) },
                size = 64.dp,
            )
            IconBtn(VoxIcons.Next, "Próxima frase", { goToPhrase(phraseIndex + 1) })
            IconBtn(VoxIcons.Loop, "Repetir frase", { loop = !loop }, tint = if (loop) c.accent else c.muted)
        }
        Spacer(Modifier.height(10.dp))

        Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(8.dp)) {
            if (player != null) Chip("Música", audioOn && playerState != 2, { audioOn = !audioOn })
            Chip("Melodia", melodyOn, { melodyOn = !melodyOn })
            Chip("2ª voz", voiceOn, { voiceOn = !voiceOn })
            Spacer(Modifier.weight(1f))
            val label = if (speed == 1f) "1×" else "%s×".format(speed.toString().replace('.', ','))
            Chip(label, speed != 1f, {
                speed = SPEEDS[(SPEEDS.indexOf(speed) + 1) % SPEEDS.size]
                if (playing) seek(now())
            })
        }
        Spacer(Modifier.height(10.dp))
        Panel(onClick = { showHelp = !showHelp }) {
            Row(verticalAlignment = Alignment.CenterVertically) {
                Text("COMO PRATICAR", style = VoxType.caps, color = c.muted, modifier = Modifier.weight(1f))
                Text(if (showHelp) "ocultar" else "mostrar", style = VoxType.small, color = c.muted)
            }
            if (showHelp) {
                for ((n, step) in HELP.withIndex()) {
                    Row(Modifier.padding(top = 8.dp)) {
                        Text("${n + 1}", style = VoxType.label, color = c.accent, modifier = Modifier.width(18.dp))
                        Text(step, style = VoxType.small, color = c.text)
                    }
                }
            }
        }
        Spacer(Modifier.height(10.dp))
    }
}

private val EDITS: List<Pair<String, (NoteEvent) -> NoteEvent?>> = listOf(
    "8ª↓" to { n -> n.copy(midi = n.midi - 12) },
    "−1" to { n -> n.copy(midi = n.midi - 1) },
    "+1" to { n -> n.copy(midi = n.midi + 1) },
    "8ª↑" to { n -> n.copy(midi = n.midi + 12) },
    "Apagar" to { _ -> null },
)

private val HELP = listOf(
    "Ouça com \"Melodia\" ligada até conhecer bem a linha principal.",
    "Desligue \"Música\" e \"Melodia\" e ouça só a \"2ª voz\" algumas vezes.",
    "Com fones, cante junto com a 2ª voz. A linha branca é você; as barras verdes são o alvo.",
    "Ligue a música e cante com o guia da 2ª voz.",
    "Desligue o guia e cante só com a música. Mire em 80% ou mais.",
    "Use ◀ ▶ para andar de frase em frase, o botão de repetir para fixar um trecho e 0,75× para estudar devagar.",
)

package com.raulhrn.vox.ui.screens

import android.content.Context
import android.content.Intent
import android.net.Uri
import android.provider.OpenableColumns
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.Icon
import androidx.compose.material3.LinearProgressIndicator
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.DisposableEffect
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableFloatStateOf
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.mutableLongStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.runtime.withFrameMillis
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.unit.dp
import com.raulhrn.vox.audio.FileAnalyzer
import com.raulhrn.vox.audio.MicEngine
import com.raulhrn.vox.audio.SongPlayer
import com.raulhrn.vox.core.FloatList
import com.raulhrn.vox.core.Harmony
import com.raulhrn.vox.core.HarmonyMode
import com.raulhrn.vox.core.Key
import com.raulhrn.vox.core.KeyDetector
import com.raulhrn.vox.core.NoteEvent
import com.raulhrn.vox.core.NoteFollower
import com.raulhrn.vox.core.Notation
import com.raulhrn.vox.core.Segmenter
import com.raulhrn.vox.data.Prefs
import com.raulhrn.vox.data.Song
import com.raulhrn.vox.data.SongStore
import com.raulhrn.vox.ui.Chip
import com.raulhrn.vox.ui.Header
import com.raulhrn.vox.ui.LiveTrace
import com.raulhrn.vox.ui.MicActive
import com.raulhrn.vox.ui.MicGate
import com.raulhrn.vox.ui.Nav
import com.raulhrn.vox.ui.NoteReadout
import com.raulhrn.vox.ui.Panel
import com.raulhrn.vox.ui.RoundButton
import com.raulhrn.vox.ui.Route
import com.raulhrn.vox.ui.ScreenPadding
import com.raulhrn.vox.ui.Section
import com.raulhrn.vox.ui.Stepper
import com.raulhrn.vox.ui.ToggleRow
import com.raulhrn.vox.ui.Vox
import com.raulhrn.vox.ui.VoxButton
import com.raulhrn.vox.ui.VoxIcons
import com.raulhrn.vox.ui.VoxType
import com.raulhrn.vox.ui.fieldColors
import com.raulhrn.vox.ui.formatTime
import kotlinx.coroutines.CancellationException
import kotlinx.coroutines.Job
import kotlinx.coroutines.launch
import java.util.UUID
import kotlin.math.max
import kotlin.math.roundToInt

private enum class Source(val label: String) {
    FILE("Do arquivo"),
    SING("Cantando"),
    TYPE("Digitando"),
}

@Composable
fun AddSongScreen(nav: Nav) {
    val c = Vox.colors
    val context = LocalContext.current
    val scope = rememberCoroutineScope()
    val naming = Prefs.naming
    var title by remember { mutableStateOf("") }
    var audioUri by remember { mutableStateOf<String?>(null) }
    var audioName by remember { mutableStateOf<String?>(null) }
    var source by remember { mutableStateOf(Source.FILE) }
    var notes by remember { mutableStateOf<List<NoteEvent>?>(null) }
    var durationMs by remember { mutableIntStateOf(0) }
    var error by remember { mutableStateOf<String?>(null) }
    var progress by remember { mutableFloatStateOf(-1f) }
    var job by remember { mutableStateOf<Job?>(null) }

    val picker = rememberLauncherForActivityResult(ActivityResultContracts.OpenDocument()) { uri ->
        if (uri != null) {
            try {
                context.contentResolver.takePersistableUriPermission(uri, Intent.FLAG_GRANT_READ_URI_PERMISSION)
            } catch (_: Exception) {
            }
            audioUri = uri.toString()
            val name = displayName(context, uri)
            audioName = name ?: "Arquivo de áudio"
            if (title.isBlank()) title = name?.substringBeforeLast('.') ?: ""
            if (source == Source.FILE) notes = null
            error = null
        }
    }

    fun save() {
        val n = notes ?: return
        if (n.isEmpty()) return
        val key = KeyDetector.detect(n)
        val song = Song(
            id = UUID.randomUUID().toString(),
            title = title.trim().ifBlank { "Sem título" },
            audioUri = audioUri,
            notes = n,
            key = key,
            mode = HarmonyMode.THIRD_ABOVE,
            octave = bestOctave(n, key),
            createdAt = System.currentTimeMillis(),
            durationMs = max(durationMs, n.last().endMs),
        )
        SongStore.upsert(song)
        nav.replace(Route.Practice(song.id))
    }

    Column(Modifier.fillMaxSize().padding(horizontal = ScreenPadding)) {
        Header("Nova música", onBack = {
            job?.cancel()
            nav.pop()
        })
        Column(Modifier.weight(1f).verticalScroll(rememberScrollState())) {
            OutlinedTextField(
                value = title,
                onValueChange = { title = it },
                label = { Text("Nome da música") },
                singleLine = true,
                modifier = Modifier.fillMaxWidth(),
                colors = fieldColors(),
            )
            Section("Áudio (opcional)")
            Panel(onClick = { picker.launch(arrayOf("audio/*")) }) {
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Icon(VoxIcons.File, null, tint = c.accent, modifier = Modifier.size(22.dp))
                    Spacer(Modifier.width(12.dp))
                    Column(Modifier.weight(1f)) {
                        Text(audioName ?: "Escolher arquivo", style = VoxType.body, color = c.text, maxLines = 1)
                        Text(
                            if (audioName == null) "MP3, M4A, WAV, OGG… para tocar junto e extrair a melodia" else "Toque para trocar",
                            style = VoxType.small,
                            color = c.muted,
                        )
                    }
                }
            }
            Section("Melodia")
            Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                for (s in Source.entries) {
                    Chip(s.label, source == s, {
                        if (source != s) {
                            job?.cancel()
                            source = s
                            notes = null
                            error = null
                        }
                    })
                }
            }
            Spacer(Modifier.height(14.dp))
            when (source) {
                Source.FILE -> {
                    Text(
                        "O app procura a voz principal. Funciona melhor com voz em destaque, a cappella ou voz e violão. " +
                            "Em mixagens cheias a melodia pode sair com erros: nesse caso, use \"Cantando\".",
                        style = VoxType.small,
                        color = c.muted,
                    )
                    Spacer(Modifier.height(12.dp))
                    when {
                        audioUri == null -> Text("Escolha um arquivo de áudio acima.", style = VoxType.label, color = c.text)
                        progress >= 0f -> {
                            LinearProgressIndicator(
                                progress = { progress },
                                modifier = Modifier.fillMaxWidth(),
                                color = c.accent,
                                trackColor = c.surface2,
                            )
                            Spacer(Modifier.height(8.dp))
                            Row(verticalAlignment = Alignment.CenterVertically) {
                                Text("Analisando… ${(progress * 100).roundToInt()}%", style = VoxType.small, color = c.muted, modifier = Modifier.weight(1f))
                                Text("cancelar", style = VoxType.small, color = c.muted, modifier = Modifier.clickable { job?.cancel() }.padding(6.dp))
                            }
                        }
                        notes == null -> VoxButton("Encontrar a melodia", {
                            val uri = Uri.parse(audioUri ?: return@VoxButton)
                            error = null
                            job = scope.launch {
                                progress = 0f
                                try {
                                    val r = FileAnalyzer.analyze(context, uri) { p -> progress = p }
                                    if (r.notes.size < 4) {
                                        error = "Não encontrei uma melodia clara neste arquivo. Tente \"Cantando\"."
                                    } else {
                                        notes = r.notes
                                        durationMs = r.durationMs
                                    }
                                } catch (e: CancellationException) {
                                    throw e
                                } catch (e: Exception) {
                                    error = "Não consegui ler este arquivo."
                                } finally {
                                    progress = -1f
                                }
                            }
                        }, Modifier.fillMaxWidth())
                    }
                }
                Source.SING -> SingSource(audioUri) { n, d ->
                    if (n.size < 2) {
                        error = "Não ouvi notas suficientes. Cante mais perto do celular."
                        notes = null
                    } else {
                        error = null
                        notes = n
                        durationMs = d
                    }
                }
                Source.TYPE -> TypeSource { n, e ->
                    notes = n
                    error = e
                    durationMs = 0
                }
            }

            val n = notes
            if (n != null && n.isNotEmpty()) {
                Spacer(Modifier.height(16.dp))
                Panel {
                    Text("MELODIA ENCONTRADA", style = VoxType.caps, color = c.accent)
                    Spacer(Modifier.height(6.dp))
                    val key = remember(n) { KeyDetector.detect(n) }
                    Text(
                        "${n.size} notas · ${key.label(naming)} · ${formatTime(max(durationMs, n.last().endMs).toFloat())}",
                        style = VoxType.body,
                        color = c.text,
                    )
                    Text("Você pode ajustar o tom e o intervalo da segunda voz na próxima tela.", style = VoxType.small, color = c.muted)
                }
            }
            error?.let {
                Spacer(Modifier.height(12.dp))
                Text(it, style = VoxType.small, color = c.bad)
            }
            Spacer(Modifier.height(16.dp))
        }
        VoxButton(
            "Salvar e praticar",
            { save() },
            Modifier.fillMaxWidth(),
            enabled = notes?.isNotEmpty() == true && progress < 0f,
        )
        Spacer(Modifier.height(12.dp))
    }
}

@Composable
private fun SingSource(audioUri: String?, onResult: (List<NoteEvent>, Int) -> Unit) {
    val c = Vox.colors
    val context = LocalContext.current
    val naming = Prefs.naming
    MicGate(Modifier.height(320.dp)) {
        var recording by remember { mutableStateOf(false) }
        var withMusic by remember { mutableStateOf(audioUri != null) }
        var elapsed by remember { mutableFloatStateOf(0f) }
        var current by remember { mutableIntStateOf(-1) }
        val tick = remember { mutableLongStateOf(0L) }
        val player = remember(audioUri) { audioUri?.let { SongPlayer(context, Uri.parse(it)) } }
        DisposableEffect(player) {
            player?.prepare {}
            onDispose { player?.release() }
        }

        MicActive(enabled = recording)

        LaunchedEffect(recording) {
            if (!recording) return@LaunchedEffect
            val track = FloatList(2048)
            val levels = FloatList(2048)
            val useMusic = withMusic && player != null && player.ready
            if (useMusic) player!!.play(0, 1f)
            val startNs = System.nanoTime()
            var firstMs = Float.NaN
            var lastFrame = MicEngine.frames
            val follower = NoteFollower()
            try {
                while (true) {
                    withFrameMillis { now ->
                        val f = MicEngine.frames
                        val nowNs = System.nanoTime()
                        val clock = if (useMusic) player!!.positionMs() else (nowNs - startNs) / 1e6f
                        var i = max(lastFrame, f - 64)
                        while (i < f) {
                            val idx = MicEngine.index(i)
                            if (MicEngine.timeNs[idx] >= startNs) {
                                if (firstMs.isNaN()) {
                                    firstMs = clock - (nowNs - MicEngine.timeNs[idx]) / 1e6f - MicEngine.LATENCY_MS
                                }
                                track.add(MicEngine.midi[idx])
                                levels.add(MicEngine.levelDb[idx])
                                follower.update(MicEngine.midi[idx], MicEngine.timeNs[idx] / 1_000_000)
                            }
                            i++
                        }
                        lastFrame = f
                        elapsed = clock
                        current = follower.note
                        tick.longValue = now
                    }
                }
            } finally {
                player?.pause()
                val notes = if (track.size < 10) {
                    emptyList()
                } else {
                    Segmenter.segment(
                        track.toArray(),
                        MicEngine.HOP_MS,
                        max(0f, if (firstMs.isNaN()) 0f else firstMs),
                        onsets = Segmenter.onsets(levels.toArray(), minDb = -55f),
                    )
                }
                val duration = if (useMusic) player!!.durationMs else (notes.lastOrNull()?.endMs ?: 0)
                onResult(notes, duration)
            }
        }

        Column {
            if (audioUri != null) {
                ToggleRow("Tocar a música enquanto canto", withMusic, { if (!recording) withMusic = it }, detail = "Use fones para o microfone ouvir só você")
            }
            Text(
                if (recording) "Cante a melodia principal, sem pressa." else "Toque no microfone e cante a melodia que quer harmonizar.",
                style = VoxType.small,
                color = c.muted,
            )
            Spacer(Modifier.height(8.dp))
            Row(verticalAlignment = Alignment.CenterVertically) {
                RoundButton(
                    if (recording) VoxIcons.Stop else VoxIcons.Mic,
                    if (recording) "Parar" else "Gravar",
                    { recording = !recording },
                    size = 64.dp,
                )
                Spacer(Modifier.width(16.dp))
                Column(Modifier.weight(1f)) {
                    Text(if (recording) "Gravando" else "Pronto", style = VoxType.label, color = if (recording) c.accent else c.text)
                    Text(formatTime(elapsed), style = VoxType.small, color = c.muted)
                }
                NoteReadout(current, naming, large = false)
            }
            Spacer(Modifier.height(8.dp))
            LiveTrace(tick, Modifier.fillMaxWidth().height(140.dp), naming = naming)
        }
    }
}

@Composable
private fun TypeSource(onResult: (List<NoteEvent>?, String?) -> Unit) {
    val c = Vox.colors
    var text by remember { mutableStateOf("") }
    var bpm by remember { mutableIntStateOf(90) }
    LaunchedEffect(text, bpm) {
        if (text.isBlank()) {
            onResult(null, null)
        } else {
            val r = Notation.parse(text, bpm)
            onResult(if (r.error == null) r.notes else null, r.error)
        }
    }
    OutlinedTextField(
        value = text,
        onValueChange = { text = it },
        label = { Text("Notas") },
        minLines = 4,
        modifier = Modifier.fillMaxWidth(),
        colors = fieldColors(),
    )
    Spacer(Modifier.height(8.dp))
    Text(
        "Separe as notas por espaço: Dó Ré Mi Fá# Sol (ou C D E F# G).\n" +
            "\":2\" = dura 2 tempos · \"-\" = pausa · número = oitava (Dó4 é o Dó central).",
        style = VoxType.small,
        color = c.muted,
    )
    Text(
        "Inserir exemplo",
        style = VoxType.label,
        color = c.accent,
        modifier = Modifier
            .clickable { text = "Dó4 Dó Sol Sol Lá Lá Sol:2 Fá Fá Mi Mi Ré Ré Dó:2" }
            .padding(vertical = 8.dp),
    )
    Stepper("Andamento", "$bpm bpm", { bpm = (bpm - 4).coerceAtLeast(40) }, { bpm = (bpm + 4).coerceAtMost(220) })
}

/** Octave shift that brings the default second voice closest to the user's vocal range. */
private fun bestOctave(notes: List<NoteEvent>, key: Key): Int {
    val voice = Harmony.voice(notes, key, HarmonyMode.THIRD_ABOVE).map { it.midi }.sorted()
    if (voice.isEmpty()) return 0
    val median = voice[voice.size / 2]
    val center = (Prefs.rangeLow + Prefs.rangeHigh) / 2
    return Math.round((center - median) / 12f).coerceIn(-2, 2)
}

private fun displayName(context: Context, uri: Uri): String? = try {
    context.contentResolver.query(uri, arrayOf(OpenableColumns.DISPLAY_NAME), null, null, null)?.use { cursor ->
        if (cursor.moveToFirst()) cursor.getString(0) else null
    }
} catch (_: Exception) {
    null
}

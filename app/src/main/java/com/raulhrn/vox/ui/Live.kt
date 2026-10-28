package com.raulhrn.vox.ui

import android.Manifest
import android.content.Intent
import android.content.pm.PackageManager
import android.net.Uri
import android.provider.Settings
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.background
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
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.Icon
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.DisposableEffect
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.MutableLongState
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
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.LocalView
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.lifecycle.Lifecycle
import androidx.lifecycle.LifecycleEventObserver
import androidx.lifecycle.compose.LocalLifecycleOwner
import com.raulhrn.vox.audio.MicEngine
import com.raulhrn.vox.core.Naming
import com.raulhrn.vox.core.NoteNames
import kotlin.math.abs

/** A state that changes on every display frame (capped near 60 fps) while it is composed. */
@Composable
fun rememberFrameTick(): MutableLongState {
    val tick = remember { mutableLongStateOf(0L) }
    LaunchedEffect(Unit) {
        var last = 0L
        while (true) {
            withFrameMillis {
                if (it - last >= 15) {
                    last = it
                    tick.longValue = it
                }
            }
        }
    }
    return tick
}

/**
 * Keeps the shared microphone engine running while this is composed and the app is visible.
 * Also keeps the screen awake while listening.
 */
@Composable
fun MicActive(enabled: Boolean = true) {
    val owner = LocalLifecycleOwner.current
    val view = LocalView.current
    DisposableEffect(owner, enabled) {
        var held = false
        fun sync() {
            val want = enabled && owner.lifecycle.currentState.isAtLeast(Lifecycle.State.STARTED)
            if (want && !held) {
                MicEngine.acquire()
                held = true
                view.keepScreenOn = true
            } else if (!want && held) {
                MicEngine.release()
                held = false
                view.keepScreenOn = MicEngine.active
            }
        }
        val observer = LifecycleEventObserver { _, _ -> sync() }
        owner.lifecycle.addObserver(observer)
        sync()
        onDispose {
            owner.lifecycle.removeObserver(observer)
            if (held) {
                MicEngine.release()
                view.keepScreenOn = MicEngine.active
            }
        }
    }
}

/** Shows [content] only once microphone permission is granted; otherwise explains and asks. */
@Composable
fun MicGate(modifier: Modifier = Modifier, content: @Composable () -> Unit) {
    val context = LocalContext.current
    fun check() = context.checkSelfPermission(Manifest.permission.RECORD_AUDIO) == PackageManager.PERMISSION_GRANTED
    var granted by remember { mutableStateOf(check()) }
    var denials by remember { mutableIntStateOf(0) }
    val launcher = rememberLauncherForActivityResult(ActivityResultContracts.RequestPermission()) { ok ->
        granted = ok
        if (!ok) denials++
    }
    val owner = LocalLifecycleOwner.current
    DisposableEffect(owner) {
        val observer = LifecycleEventObserver { _, event ->
            if (event == Lifecycle.Event.ON_RESUME) granted = check()
        }
        owner.lifecycle.addObserver(observer)
        onDispose { owner.lifecycle.removeObserver(observer) }
    }
    if (granted) {
        content()
        return
    }
    val c = Vox.colors
    Column(
        modifier.fillMaxSize().padding(24.dp),
        verticalArrangement = Arrangement.Center,
        horizontalAlignment = Alignment.CenterHorizontally,
    ) {
        Icon(VoxIcons.Mic, null, tint = c.accent, modifier = Modifier.size(40.dp))
        Spacer(Modifier.height(16.dp))
        Text("Preciso ouvir sua voz", style = VoxType.heading, color = c.text)
        Spacer(Modifier.height(8.dp))
        Text(
            "O som é analisado no próprio celular e nunca é gravado nem enviado para a internet.",
            style = VoxType.small,
            color = c.muted,
            textAlign = TextAlign.Center,
        )
        Spacer(Modifier.height(20.dp))
        if (denials >= 2) {
            VoxButton("Abrir configurações", {
                context.startActivity(
                    Intent(Settings.ACTION_APPLICATION_DETAILS_SETTINGS, Uri.fromParts("package", context.packageName, null))
                        .addFlags(Intent.FLAG_ACTIVITY_NEW_TASK),
                )
            })
        } else {
            VoxButton("Permitir microfone", { launcher.launch(Manifest.permission.RECORD_AUDIO) }, icon = VoxIcons.Mic)
        }
    }
}

/** The big note name with its octave. */
@Composable
fun NoteReadout(note: Int, naming: Naming, modifier: Modifier = Modifier, large: Boolean = true) {
    val c = Vox.colors
    val style = if (large) VoxType.display else VoxType.display.copy(fontSize = VoxType.display.fontSize * 0.6f, lineHeight = VoxType.display.lineHeight * 0.6f)
    Row(modifier, verticalAlignment = Alignment.Bottom, horizontalArrangement = Arrangement.Center) {
        if (note < 0) {
            Text("—", style = style, color = c.faint)
        } else {
            Text(NoteNames.name(note, naming), style = style, color = c.text)
            Text(
                NoteNames.octave(note).toString(),
                style = if (large) VoxType.title else VoxType.heading,
                color = c.muted,
                modifier = Modifier.padding(bottom = if (large) 18.dp else 10.dp, start = 4.dp),
            )
        }
    }
}

/** Horizontal tuning meter, -50..+50 cents. Reads its values in the draw phase only. */
@Composable
fun CentsMeter(cents: () -> Float, active: () -> Boolean, modifier: Modifier = Modifier) {
    val c = Vox.colors
    Canvas(modifier.fillMaxWidth().height(40.dp)) {
        val pad = 10.dp.toPx()
        val half = size.width / 2f - pad
        val mid = size.width / 2f
        val y = size.height / 2f
        drawLine(c.line, Offset(pad, y), Offset(size.width - pad, y), strokeWidth = 1.dp.toPx())
        for (k in -50..50 step 10) {
            val x = mid + k / 50f * half
            val len = when {
                k == 0 -> 14.dp.toPx()
                abs(k) == 50 -> 8.dp.toPx()
                else -> 4.dp.toPx()
            }
            drawLine(if (k == 0) c.muted else c.line, Offset(x, y - len), Offset(x, y + len), strokeWidth = 1.5.dp.toPx())
        }
        if (active()) {
            val v = cents().coerceIn(-50f, 50f)
            val color = when {
                abs(v) <= 10f -> c.accent
                abs(v) <= 25f -> c.warn
                else -> c.bad
            }
            val x = mid + v / 50f * half
            drawLine(color, Offset(mid, y), Offset(x, y), strokeWidth = 3.dp.toPx())
            drawCircle(color, radius = 8.dp.toPx(), center = Offset(x, y))
        }
    }
}

/** Thin input level bar. */
@Composable
fun LevelBar(level: () -> Float, modifier: Modifier = Modifier) {
    val c = Vox.colors
    Box(modifier.fillMaxWidth().height(4.dp).clip(RoundedCornerShape(2.dp)).background(c.surface2)) {
        Canvas(Modifier.fillMaxHeight().fillMaxWidth()) {
            val f = ((level() + 60f) / 50f).coerceIn(0f, 1f)
            drawRect(c.muted, size = size.copy(width = size.width * f))
        }
    }
}

/**
 * Backing balance meter: your level relative to the lead-voice reference, from -18 to +4 dB,
 * with the recommended zone ([target] ± 2.5 dB) highlighted.
 */
@Composable
fun BalanceMeter(relative: () -> Float, target: Float, modifier: Modifier = Modifier) {
    val c = Vox.colors
    Canvas(modifier.fillMaxWidth().height(28.dp)) {
        val lo = -18f
        val hi = 4f
        fun x(db: Float) = (db.coerceIn(lo, hi) - lo) / (hi - lo) * size.width
        val y = size.height / 2f
        drawLine(c.line, Offset(0f, y), Offset(size.width, y), strokeWidth = 4.dp.toPx())
        drawLine(c.accent.copy(alpha = 0.35f), Offset(x(target - 2.5f), y), Offset(x(target + 2.5f), y), strokeWidth = 10.dp.toPx())
        // 0 dB = the lead singer.
        drawLine(c.muted, Offset(x(0f), y - 9.dp.toPx()), Offset(x(0f), y + 9.dp.toPx()), strokeWidth = 1.5.dp.toPx())
        val v = relative()
        if (!v.isNaN()) {
            val ok = abs(v - target) <= 2.5f
            drawCircle(if (ok) c.accent else c.warn, radius = 7.dp.toPx(), center = Offset(x(v), y))
        }
    }
}

/** Words for where the singer is compared to the balance target. */
fun balanceHint(relative: Float, target: Float): String = when {
    relative.isNaN() -> "cante para medir"
    relative - target > 2.5f -> "mais suave"
    target - relative > 2.5f -> "um pouco mais forte"
    else -> "na medida"
}

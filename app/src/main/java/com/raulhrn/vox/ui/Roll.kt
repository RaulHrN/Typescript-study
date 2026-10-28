package com.raulhrn.vox.ui

import android.graphics.Paint
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.gestures.detectHorizontalDragGestures
import androidx.compose.foundation.gestures.detectTapGestures
import androidx.compose.runtime.Composable
import androidx.compose.runtime.State
import androidx.compose.runtime.getValue
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberUpdatedState
import androidx.compose.ui.Modifier
import androidx.compose.ui.geometry.CornerRadius
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.Path
import androidx.compose.ui.graphics.StrokeCap
import androidx.compose.ui.graphics.StrokeJoin
import androidx.compose.ui.graphics.drawscope.DrawScope
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.graphics.nativeCanvas
import androidx.compose.ui.graphics.toArgb
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.raulhrn.vox.audio.MicEngine
import com.raulhrn.vox.core.Naming
import com.raulhrn.vox.core.NoteNames
import kotlinx.coroutines.coroutineScope
import kotlinx.coroutines.launch
import kotlin.math.ceil
import kotlin.math.floor
import kotlin.math.max
import kotlin.math.min

/** GUIDE: notes to listen to; MELODY: the lead; PART: another harmony part; TARGET: what you sing. */
enum class RollKind { GUIDE, MELODY, PART, TARGET }

class RollNote(val startMs: Int, val durMs: Int, val midi: Int, val kind: RollKind, val target: Int = -1)

/** Ring buffer of sung pitch points in timeline time. Written and drawn on the main thread. */
class Trace(private val capacity: Int = 720) {
    private val time = FloatArray(capacity)
    private val midi = FloatArray(capacity)
    private var start = 0
    var size = 0
        private set

    fun add(t: Float, m: Float) {
        val i = (start + size) % capacity
        if (size < capacity) size++ else start = (start + 1) % capacity
        time[i] = t
        midi[i] = m
    }

    fun clear() {
        start = 0
        size = 0
    }

    fun lastMidi(): Float {
        for (k in size - 1 downTo 0) {
            val m = midi[(start + k) % capacity]
            if (!m.isNaN()) return m
        }
        return Float.NaN
    }

    inline fun forEach(action: (t: Float, m: Float) -> Unit) {
        for (k in 0 until size) {
            val i = indexOf(k)
            action(timeAt(i), midiAt(i))
        }
    }

    fun indexOf(k: Int): Int = (start + k) % capacity
    fun timeAt(i: Int): Float = time[i]
    fun midiAt(i: Int): Float = midi[i]
}

/** Smoothly follows the pitch range that should be visible, so the grid glides instead of jumping. */
class RangeFollower {
    var lo = Float.NaN
        private set
    var hi = Float.NaN
        private set

    fun update(targetLo: Float, targetHi: Float, minSpan: Float = 12f) {
        var a = targetLo - 2.5f
        var b = targetHi + 2.5f
        if (b - a < minSpan) {
            val mid = (a + b) / 2f
            a = mid - minSpan / 2f
            b = mid + minSpan / 2f
        }
        if (lo.isNaN()) {
            lo = a
            hi = b
        } else {
            lo += (a - lo) * 0.08f
            hi += (b - hi) * 0.08f
        }
    }
}

private val NATURAL = booleanArrayOf(true, false, true, false, true, true, false, true, false, true, false, true)

private fun DrawScope.drawGrid(range: RangeFollower, colors: VoxColors, paint: Paint, naming: Naming, flats: Boolean) {
    val lo = range.lo
    val hi = range.hi
    val h = size.height
    val rowH = h / (hi - lo)
    paint.color = colors.muted.toArgb()
    paint.textSize = 10.sp.toPx()
    val showAll = rowH > 11.dp.toPx()
    for (m in ceil(lo).toInt()..floor(hi).toInt()) {
        val y = h - (m - lo) / (hi - lo) * h
        val pc = Math.floorMod(m, 12)
        val strong = pc == 0
        drawLine(
            if (strong) colors.line else colors.line.copy(alpha = if (NATURAL[pc]) 0.55f else 0.25f),
            Offset(0f, y),
            Offset(size.width, y),
            strokeWidth = 1.dp.toPx(),
        )
        if (strong || (showAll && NATURAL[pc])) {
            drawContext.canvas.nativeCanvas.drawText(
                if (strong) NoteNames.full(m, naming, flats) else NoteNames.name(m, naming, flats),
                4.dp.toPx(),
                y - 3.dp.toPx(),
                paint,
            )
        }
    }
}

/**
 * Scrolling piano roll: target notes as bars, the singer's pitch as a line and a fixed playhead.
 * All drawing happens in the draw phase, so a moving roll never recomposes.
 */
@Composable
fun PianoRoll(
    notes: List<RollNote>,
    clock: () -> Float,
    tick: State<Long>,
    modifier: Modifier = Modifier,
    trace: Trace? = null,
    naming: Naming = Naming.SOLFEGE,
    flats: Boolean = false,
    windowMs: Float = 6000f,
    playhead: Float = 0.3f,
    accuracy: ((Int) -> Float)? = null,
    selected: Int = -1,
    /** Shown when no note is near the playhead, e.g. "trecho instrumental". */
    emptyLabel: String? = null,
    onTap: ((timeMs: Float, midi: Float) -> Unit)? = null,
    onScrub: ((deltaMs: Float) -> Unit)? = null,
) {
    val c = Vox.colors
    val paint = remember { Paint(Paint.ANTI_ALIAS_FLAG) }
    val range = remember { RangeFollower() }
    val path = remember { Path() }
    // Gesture handlers outlive recompositions (a drag must not restart), so read the latest lambdas.
    val currentClock by rememberUpdatedState(clock)
    val currentTap by rememberUpdatedState(onTap)
    val currentScrub by rememberUpdatedState(onScrub)
    val gestures = if (onTap == null && onScrub == null) {
        Modifier
    } else {
        Modifier.pointerInput(onTap != null, onScrub != null, windowMs, playhead) {
            coroutineScope {
                if (onTap != null) {
                    launch {
                        detectTapGestures { pos ->
                            if (range.lo.isNaN()) return@detectTapGestures
                            val w = size.width.toFloat()
                            val h = size.height.toFloat()
                            val time = currentClock() + (pos.x - w * playhead) / w * windowMs
                            val midi = range.lo + (h - pos.y) / h * (range.hi - range.lo)
                            currentTap?.invoke(time, midi)
                        }
                    }
                }
                if (onScrub != null) {
                    launch {
                        detectHorizontalDragGestures { change, dx ->
                            change.consume()
                            currentScrub?.invoke(-dx / size.width * windowMs)
                        }
                    }
                }
            }
        }
    }
    Canvas(modifier.then(gestures)) {
        tick.value // redraw on every frame tick
        val t = clock()
        val w = size.width
        val h = size.height
        val px = w * playhead
        val tStart = t - windowMs * playhead
        val tEnd = tStart + windowMs

        var lo = Float.MAX_VALUE
        var hi = -Float.MAX_VALUE
        for (n in notes) {
            if (n.startMs + n.durMs < tStart - 1500 || n.startMs > tEnd + 1500) continue
            lo = min(lo, n.midi.toFloat())
            hi = max(hi, n.midi.toFloat())
        }
        if (trace != null) {
            trace.forEach { tt, m ->
                if (tt >= tStart && !m.isNaN()) {
                    lo = min(lo, m)
                    hi = max(hi, m)
                }
            }
        }
        if (lo > hi) {
            val center = if (range.lo.isNaN()) 60f else (range.lo + range.hi) / 2f
            lo = center
            hi = center
        }
        range.update(lo, hi)
        drawGrid(range, c, paint, naming, flats)
        val rLo = range.lo
        val span = range.hi - range.lo
        val rowH = h / span
        val barH = (rowH * 0.5f).coerceIn(6.dp.toPx(), 14.dp.toPx())

        for (n in notes) {
            val x0 = px + (n.startMs - t) / windowMs * w
            val x1 = px + (n.startMs + n.durMs - t) / windowMs * w
            if (x1 < 0f || x0 > w) continue
            val y = h - (n.midi - rLo) / span * h
            val color = when (n.kind) {
                RollKind.GUIDE -> c.faint
                RollKind.MELODY -> c.melody.copy(alpha = 0.6f)
                RollKind.PART -> c.rose.copy(alpha = 0.6f)
                RollKind.TARGET -> {
                    val acc = if (accuracy != null && n.target >= 0) accuracy(n.target) else -1f
                    when {
                        acc < 0f -> c.accent.copy(alpha = if (n.startMs + n.durMs < t) 0.5f else 0.9f)
                        acc >= 0.7f -> c.accent
                        acc >= 0.4f -> c.warn
                        else -> c.bad
                    }
                }
            }
            drawRoundRect(
                color,
                topLeft = Offset(x0, y - barH / 2f),
                size = Size(max(3f, x1 - x0), barH),
                cornerRadius = CornerRadius(barH / 2f),
            )
        }
        if (selected in notes.indices) {
            val n = notes[selected]
            val x0 = px + (n.startMs - t) / windowMs * w
            val x1 = px + (n.startMs + n.durMs - t) / windowMs * w
            val y = h - (n.midi - rLo) / span * h
            val pad = 3.dp.toPx()
            drawRoundRect(
                c.text,
                topLeft = Offset(x0 - pad, y - barH / 2f - pad),
                size = Size(max(3f, x1 - x0) + 2 * pad, barH + 2 * pad),
                cornerRadius = CornerRadius(barH / 2f + pad),
                style = Stroke(width = 2.dp.toPx()),
            )
        }

        drawLine(c.text.copy(alpha = 0.3f), Offset(px, 0f), Offset(px, h), strokeWidth = 1.dp.toPx())

        if (emptyLabel != null && notes.isNotEmpty() && notes.none { it.startMs < t + 1600 && it.startMs + it.durMs > t - 400 }) {
            paint.color = c.muted.toArgb()
            paint.textSize = 14.sp.toPx()
            val textWidth = paint.measureText(emptyLabel)
            drawContext.canvas.nativeCanvas.drawText(emptyLabel, (w - textWidth) / 2f, h / 2f, paint)
        }

        if (trace != null) {
            path.reset()
            var pen = false
            var prevT = 0f
            var lastX = 0f
            var lastY = 0f
            var lastT = -1e9f
            trace.forEach { tt, m ->
                if (tt < tStart - 200f || tt > t + 100f || m.isNaN() || (pen && tt - prevT > 90f)) {
                    pen = false
                } else {
                    val x = px + (tt - t) / windowMs * w
                    val y = h - (m - rLo) / span * h
                    if (pen) path.lineTo(x, y) else path.moveTo(x, y)
                    pen = true
                    lastX = x
                    lastY = y
                    lastT = tt
                }
                prevT = tt
            }
            drawPath(path, c.text, style = Stroke(width = 2.5.dp.toPx(), cap = StrokeCap.Round, join = StrokeJoin.Round))
            if (t - lastT < 200f) drawCircle(c.text, radius = 5.dp.toPx(), center = Offset(lastX, lastY))
        }
    }
}

/** Live pitch line straight from the microphone ring buffer, newest at the right edge. */
@Composable
fun LiveTrace(
    tick: State<Long>,
    modifier: Modifier = Modifier,
    naming: Naming = Naming.SOLFEGE,
    windowMs: Float = 5000f,
    lineColor: Color? = null,
) {
    val c = Vox.colors
    val paint = remember { Paint(Paint.ANTI_ALIAS_FLAG) }
    val range = remember { RangeFollower() }
    val path = remember { Path() }
    Canvas(modifier) {
        tick.value
        val nowNs = System.nanoTime()
        val w = size.width
        val h = size.height
        val f = MicEngine.frames
        val count = min(f, min((windowMs / MicEngine.HOP_MS).toLong() + 4, MicEngine.CAPACITY - 1L)).toInt()

        var lo = Float.MAX_VALUE
        var hi = -Float.MAX_VALUE
        for (k in 0 until count) {
            val m = MicEngine.midi[MicEngine.index(f - 1 - k)]
            if (!m.isNaN()) {
                lo = min(lo, m)
                hi = max(hi, m)
            }
        }
        if (lo > hi) {
            val center = if (range.lo.isNaN()) 60f else (range.lo + range.hi) / 2f
            lo = center
            hi = center
        }
        range.update(lo, hi)
        drawGrid(range, c, paint, naming, false)
        val rLo = range.lo
        val span = range.hi - range.lo

        path.reset()
        var pen = false
        var lastX = 0f
        var lastY = 0f
        for (k in count - 1 downTo 0) {
            val i = MicEngine.index(f - 1 - k)
            val m = MicEngine.midi[i]
            val age = (nowNs - MicEngine.timeNs[i]) / 1_000_000f
            if (m.isNaN() || age > windowMs) {
                pen = false
                continue
            }
            val x = w - age / windowMs * w
            val y = h - (m - rLo) / span * h
            if (pen) path.lineTo(x, y) else path.moveTo(x, y)
            pen = true
            lastX = x
            lastY = y
        }
        val newestVoiced = count > 0 && !MicEngine.midi[MicEngine.index(f - 1)].isNaN()
        val color = lineColor ?: c.accent
        drawPath(path, color, style = Stroke(width = 3.dp.toPx(), cap = StrokeCap.Round, join = StrokeJoin.Round))
        if (newestVoiced) drawCircle(color, radius = 6.dp.toPx(), center = Offset(lastX, lastY))
    }
}

package com.raulhrn.vox.audio

import android.media.AudioAttributes
import android.media.AudioFormat
import android.media.AudioManager
import android.media.AudioTrack
import android.os.Process
import com.raulhrn.vox.core.NoteEvent
import com.raulhrn.vox.core.Pitch
import com.raulhrn.vox.core.TimedNote
import kotlin.math.PI
import kotlin.math.exp
import kotlin.math.floor
import kotlin.math.max
import kotlin.math.min
import kotlin.math.sin

/** A monophonic line for the [Synth]. Can be muted live through [enabled]. */
class SynthTrack(
    val starts: IntArray,
    val durs: IntArray,
    val midis: IntArray,
    val gain: Float,
    val bright: Boolean = false,
) {
    @Volatile
    var enabled = true

    companion object {
        fun of(notes: List<NoteEvent>, gain: Float, bright: Boolean = false): SynthTrack {
            val sorted = notes.sortedBy { it.startMs }
            return SynthTrack(
                IntArray(sorted.size) { sorted[it].startMs },
                IntArray(sorted.size) { sorted[it].durMs },
                IntArray(sorted.size) { sorted[it].midi },
                gain,
                bright,
            )
        }

        fun ofTimed(notes: List<TimedNote>, gain: Float, bright: Boolean = false): SynthTrack =
            of(notes.map { NoteEvent(it.startMs, it.durMs, it.midi) }, gain, bright)
    }
}

/**
 * Small streaming synthesizer for reference tones and guide voices.
 * Renders on its own thread with a wavetable, straight into a low-latency AudioTrack.
 * Its playback head doubles as the clock for exercises and songs without audio.
 */
class Synth {
    private val sampleRate = AudioTrack.getNativeOutputSampleRate(AudioManager.STREAM_MUSIC).takeIf { it in 8000..192000 } ?: 48000

    @Volatile
    private var worker: Worker? = null

    @Volatile
    private var lastPosition = 0f

    @Volatile
    var a4 = 440f

    val isPlaying: Boolean get() = worker != null

    /** Current position in song time (ms). */
    fun positionMs(): Float = worker?.positionMs() ?: lastPosition

    /** Plays [tracks] from [fromMs]; the stream ends by itself after [untilMs] (song time). */
    fun start(tracks: List<SynthTrack>, fromMs: Float, speed: Float = 1f, untilMs: Float = Float.MAX_VALUE) {
        stop()
        lastPosition = fromMs
        worker = Worker(tracks, fromMs, speed, untilMs).also { it.start() }
    }

    /** Short preview of a single note. */
    fun preview(midi: Int) {
        start(listOf(SynthTrack.of(listOf(NoteEvent(0, 450, midi)), gain = 0.3f)), 0f, untilMs = 700f)
    }

    fun stop() {
        val w = worker ?: return
        lastPosition = w.positionMs()
        w.alive = false
        worker = null
    }

    private inner class Worker(
        private val tracks: List<SynthTrack>,
        private val fromMs: Float,
        private val speed: Float,
        private val untilMs: Float,
    ) : Thread("vox-synth") {
        @Volatile
        var alive = true

        @Volatile
        private var track: AudioTrack? = null

        fun positionMs(): Float {
            val t = track ?: return fromMs
            val frames = try {
                t.playbackHeadPosition
            } catch (_: Exception) {
                0
            }
            return fromMs + frames * 1000f / sampleRate * speed
        }

        override fun run() {
            Process.setThreadPriority(Process.THREAD_PRIORITY_AUDIO)
            val block = 256
            val minBuf = AudioTrack.getMinBufferSize(sampleRate, AudioFormat.CHANNEL_OUT_MONO, AudioFormat.ENCODING_PCM_16BIT)
            val at = try {
                AudioTrack.Builder()
                    .setAudioAttributes(
                        AudioAttributes.Builder()
                            .setUsage(AudioAttributes.USAGE_MEDIA)
                            .setContentType(AudioAttributes.CONTENT_TYPE_MUSIC)
                            .build(),
                    )
                    .setAudioFormat(
                        AudioFormat.Builder()
                            .setSampleRate(sampleRate)
                            .setEncoding(AudioFormat.ENCODING_PCM_16BIT)
                            .setChannelMask(AudioFormat.CHANNEL_OUT_MONO)
                            .build(),
                    )
                    .setBufferSizeInBytes(max(minBuf, block * 2 * 8))
                    .setTransferMode(AudioTrack.MODE_STREAM)
                    .setPerformanceMode(AudioTrack.PERFORMANCE_MODE_LOW_LATENCY)
                    .build()
            } catch (_: Exception) {
                return
            }
            val mix = FloatArray(block)
            val out = ShortArray(block)
            val msPerFrame = 1000.0 / sampleRate * speed
            var frame = 0L
            try {
                at.play()
                track = at
                while (alive) {
                    mix.fill(0f)
                    val t0 = fromMs + frame * msPerFrame
                    if (t0 > untilMs) break
                    for (tr in tracks) if (tr.enabled) render(tr, t0, msPerFrame, mix)
                    for (i in 0 until block) {
                        val v = mix[i].coerceIn(-1f, 1f)
                        out[i] = (v * 32767f).toInt().toShort()
                    }
                    var off = 0
                    while (off < block && alive) {
                        val n = at.write(out, off, block - off)
                        if (n <= 0) break
                        off += n
                    }
                    frame += block
                }
            } catch (_: Exception) {
            } finally {
                track = null
                try {
                    at.pause()
                    at.flush()
                } catch (_: Exception) {
                }
                at.release()
            }
        }

        private fun render(tr: SynthTrack, t0: Double, msPerFrame: Double, mix: FloatArray) {
            val n = mix.size
            val t1 = t0 + n * msPerFrame
            // Last note that starts before the end of this block.
            var hi = tr.starts.size - 1
            var lo = 0
            var last = -1
            while (lo <= hi) {
                val mid = (lo + hi) ushr 1
                if (tr.starts[mid] < t1) {
                    last = mid
                    lo = mid + 1
                } else {
                    hi = mid - 1
                }
            }
            val table = if (tr.bright) BRIGHT else SOFT
            var k = last
            var checked = 0
            while (k >= 0 && checked < 6) {
                val start = tr.starts[k]
                val dur = tr.durs[k]
                if (start + dur + RELEASE_MS >= t0) {
                    val hz = Pitch.midiToHz(tr.midis[k].toFloat(), a4).toDouble()
                    for (i in 0 until n) {
                        val rel = t0 + i * msPerFrame - start
                        if (rel < 0 || rel > dur + RELEASE_MS) continue
                        var env = min(1.0, rel / ATTACK_MS) * (0.78 + 0.22 * exp(-rel / 300.0))
                        if (rel > dur) env *= 1.0 - (rel - dur) / RELEASE_MS
                        // Phase from elapsed real time keeps the renderer stateless across blocks and seeks.
                        val cycles = hz * rel / 1000.0 / speed
                        val pos = (cycles - floor(cycles)) * TABLE_SIZE
                        val idx = pos.toInt()
                        val frac = (pos - idx).toFloat()
                        val s = table[idx] + (table[idx + 1] - table[idx]) * frac
                        mix[i] += (s * env * tr.gain).toFloat()
                    }
                }
                checked++
                k--
            }
        }
    }

    companion object {
        private const val TABLE_SIZE = 2048
        private const val ATTACK_MS = 25.0
        private const val RELEASE_MS = 90.0
        private val SOFT = wavetable(floatArrayOf(1f, 0.18f, 0.06f))
        private val BRIGHT = wavetable(floatArrayOf(1f, 0.45f, 0.28f, 0.14f, 0.08f, 0.04f))

        private fun wavetable(harmonics: FloatArray): FloatArray {
            val t = FloatArray(TABLE_SIZE + 1)
            var peak = 0f
            for (i in 0 until TABLE_SIZE) {
                var v = 0.0
                for (h in harmonics.indices) v += harmonics[h] * sin(2.0 * PI * (h + 1) * i / TABLE_SIZE)
                t[i] = v.toFloat()
                peak = max(peak, kotlin.math.abs(t[i]))
            }
            for (i in 0 until TABLE_SIZE) t[i] /= peak
            t[TABLE_SIZE] = t[0]
            return t
        }
    }
}

package com.raulhrn.vox.audio

import android.annotation.SuppressLint
import android.media.AudioFormat
import android.media.AudioRecord
import android.media.MediaRecorder
import android.os.Process
import com.raulhrn.vox.core.Biquad
import com.raulhrn.vox.core.Pitch
import com.raulhrn.vox.core.PitchDetector
import com.raulhrn.vox.core.PitchSmoother
import kotlin.math.log10
import kotlin.math.max

/**
 * Shared microphone analyser.
 *
 * One background thread reads the microphone, detects the pitch and writes the result
 * into ring buffers that the UI samples once per display frame. Nothing is allocated
 * per audio block. The thread only runs while some screen holds the engine
 * ([acquire]/[release]), so the microphone and CPU are idle everywhere else.
 */
object MicEngine {
    private const val CAPTURE_RATE = 44100
    private const val DECIMATION = 2
    const val ANALYSIS_RATE = CAPTURE_RATE / DECIMATION
    const val WINDOW = 1024
    const val HOP = 512
    const val HOP_MS = HOP * 1000f / ANALYSIS_RATE

    /** Age of the sound in a frame relative to its timestamp (half an analysis window). */
    const val LATENCY_MS = WINDOW * 500f / ANALYSIS_RATE

    const val CAPACITY = 1024
    private const val MIN_CLARITY = 0.72f

    /** Smoothed MIDI pitch per frame, NaN when silent or unpitched. */
    val midi = FloatArray(CAPACITY)

    /** Input level per frame in dBFS. */
    val levelDb = FloatArray(CAPACITY)

    /** System.nanoTime() when each frame was captured. */
    val timeNs = LongArray(CAPACITY)

    /** Number of frames written so far. Frame f lives at index [index] (f). */
    @Volatile
    var frames = 0L
        private set

    @Volatile
    var error: String? = null
        private set

    /** Frames quieter than this are treated as silence. Set from the sensitivity preference. */
    @Volatile
    var gateDb = -50f

    @Volatile
    var a4 = 440f

    private var users = 0
    private var worker: Worker? = null
    private var stopping: Worker? = null

    fun index(frame: Long): Int = (frame % CAPACITY).toInt()

    fun latestMidi(): Float {
        val f = frames
        return if (f == 0L) Float.NaN else midi[index(f - 1)]
    }

    fun latestLevel(): Float {
        val f = frames
        return if (f == 0L) -100f else levelDb[index(f - 1)]
    }

    val active: Boolean
        @Synchronized get() = users > 0

    @Synchronized
    fun acquire() {
        if (users++ == 0) start()
    }

    @Synchronized
    fun release() {
        if (users > 0 && --users == 0) stop()
    }

    private fun start() {
        stopping?.join(400)
        stopping = null
        error = null
        worker = Worker().also { it.start() }
    }

    private fun stop() {
        worker?.alive = false
        stopping = worker
        worker = null
    }

    private fun publish(now: Long, m: Float, db: Float) {
        val i = index(frames)
        midi[i] = m
        levelDb[i] = db
        timeNs[i] = now
        frames++ // volatile write publishes the slot to readers
    }

    private class Worker : Thread("vox-mic") {
        @Volatile
        var alive = true

        @SuppressLint("MissingPermission") // Screens only acquire the engine after the permission is granted.
        override fun run() {
            Process.setThreadPriority(Process.THREAD_PRIORITY_AUDIO)
            val minBuf = AudioRecord.getMinBufferSize(CAPTURE_RATE, AudioFormat.CHANNEL_IN_MONO, AudioFormat.ENCODING_PCM_16BIT)
            if (minBuf <= 0) {
                error = "Microfone indisponível"
                return
            }
            val record = try {
                AudioRecord(
                    MediaRecorder.AudioSource.VOICE_RECOGNITION,
                    CAPTURE_RATE,
                    AudioFormat.CHANNEL_IN_MONO,
                    AudioFormat.ENCODING_PCM_16BIT,
                    max(minBuf, HOP * DECIMATION * 2 * 4),
                )
            } catch (e: Exception) {
                error = "Não foi possível abrir o microfone"
                return
            }
            if (record.state != AudioRecord.STATE_INITIALIZED) {
                record.release()
                error = "Não foi possível abrir o microfone"
                return
            }

            val raw = ShortArray(HOP * DECIMATION)
            val window = FloatArray(WINDOW)
            val detector = PitchDetector(ANALYSIS_RATE.toFloat(), WINDOW, minFrequency = 60f, maxFrequency = 1500f)
            val highPass = Biquad.highPass(ANALYSIS_RATE.toFloat(), 70f)
            val smoother = PitchSmoother()
            val scale = 0.5f / 32768f

            try {
                record.startRecording()
                while (alive) {
                    var got = 0
                    while (got < raw.size && alive) {
                        val n = record.read(raw, got, raw.size - got)
                        if (n < 0) {
                            error = "Erro ao ler o microfone"
                            alive = false
                            break
                        }
                        got += n
                    }
                    if (!alive) break
                    val now = System.nanoTime()

                    // Slide the window and append the new block, decimated to 22.05 kHz.
                    System.arraycopy(window, HOP, window, 0, WINDOW - HOP)
                    var j = WINDOW - HOP
                    var i = 0
                    while (i < raw.size) {
                        window[j++] = highPass.process((raw[i] + raw[i + 1]) * scale)
                        i += 2
                    }

                    val hz = detector.detect(window)
                    val rms = detector.rms
                    val db = if (rms > 1e-7f) 20f * log10(rms) else -140f
                    val voiced = hz > 0f && db > gateDb && detector.clarity >= MIN_CLARITY
                    val m = smoother.push(if (voiced) Pitch.hzToMidi(hz, a4) else Float.NaN)
                    publish(now, m, db)
                }
            } catch (e: Exception) {
                error = "Erro no microfone"
            } finally {
                try {
                    record.stop()
                } catch (_: Exception) {
                }
                record.release()
            }
        }
    }
}

package com.raulhrn.vox.core

import kotlin.math.log10
import kotlin.math.max

/**
 * Offline pitch tracking: feed mono samples (already at [sampleRate]) and get one
 * MIDI value per hop. Used for imported audio files.
 */
class TrackAnalyzer(
    val sampleRate: Float,
    val window: Int = 1024,
    val hop: Int = 512,
    private val minClarity: Float = 0.8f,
    highPassHz: Float = 0f,
    private val a4: Float = 440f,
) {
    private val detector = PitchDetector(sampleRate, window, minFrequency = 65f, maxFrequency = 1300f)
    private val filter = if (highPassHz > 0f) Biquad.highPass(sampleRate, highPassHz) else null
    private val buf = FloatArray(window)
    private var fill = 0
    private val pitches = FloatList(4096)
    private val levels = FloatList(4096)

    val hopMs: Float get() = hop * 1000f / sampleRate

    /** Time of the centre of the first analysis window. */
    val firstFrameMs: Float get() = window * 500f / sampleRate

    val frames: Int get() = pitches.size

    fun push(sample: Float) {
        buf[fill++] = filter?.process(sample) ?: sample
        if (fill == window) {
            val hz = detector.detect(buf)
            val rms = detector.rms
            levels.add(if (rms > 1e-7f) 20f * log10(rms) else -140f)
            pitches.add(if (hz > 0f && detector.clarity >= minClarity) Pitch.hzToMidi(hz, a4) else Float.NaN)
            System.arraycopy(buf, hop, buf, 0, window - hop)
            fill = window - hop
        }
    }

    /** Level of each frame in dBFS. */
    fun levels(): FloatArray = levels.toArray()

    /**
     * Returns the pitch track with quiet frames removed. A frame is kept when it is louder
     * than [absoluteGateDb] and within [relativeGateDb] of the loud parts of the recording.
     */
    fun track(relativeGateDb: Float = 30f, absoluteGateDb: Float = -55f): FloatArray {
        val n = pitches.size
        val out = FloatArray(n)
        if (n == 0) return out
        val sorted = levels.toArray().also { it.sort() }
        val loud = sorted[((n - 1) * 0.95f).toInt()]
        val gate = max(absoluteGateDb, loud - relativeGateDb)
        for (i in 0 until n) out[i] = if (levels[i] >= gate) pitches[i] else Float.NaN
        return out
    }
}

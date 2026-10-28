package com.raulhrn.vox.core

import kotlin.math.PI
import kotlin.math.abs
import kotlin.math.ceil
import kotlin.math.cos
import kotlin.math.floor
import kotlin.math.max
import kotlin.math.min
import kotlin.math.sin
import kotlin.math.sqrt

/**
 * Monophonic pitch detector based on the McLeod Pitch Method (MPM).
 *
 * The normalized square difference function is computed with an FFT based
 * autocorrelation, so each call costs O(N log N). All buffers are allocated once,
 * which keeps the real-time audio thread free of garbage collection pauses.
 */
class PitchDetector(
    val sampleRate: Float,
    val windowSize: Int,
    minFrequency: Float = 60f,
    maxFrequency: Float = 1500f,
    private val peakThreshold: Float = 0.9f,
) {
    private val fftSize = Integer.highestOneBit(windowSize * 2 - 1) shl 1
    private val fft = Fft(fftSize)
    private val re = FloatArray(fftSize)
    private val im = FloatArray(fftSize)
    private val x = FloatArray(windowSize)
    private val minTau = max(2, floor(sampleRate / maxFrequency).toInt())
    private val maxTau = min(windowSize / 2, ceil(sampleRate / minFrequency).toInt())
    private val nsdf = FloatArray(maxTau + 2)
    private val peaks = IntArray(64)

    /** Periodicity of the last analysed window, 0..1. Values above ~0.8 mean a clear tone. */
    var clarity = 0f
        private set

    /** Root mean square level of the last analysed window (after DC removal). */
    var rms = 0f
        private set

    /** Returns the fundamental frequency in Hz, or -1 when no pitch is found. */
    fun detect(input: FloatArray, offset: Int = 0): Float {
        clarity = 0f
        var sum = 0.0
        for (i in 0 until windowSize) sum += input[offset + i]
        val mean = (sum / windowSize).toFloat()
        var energy = 0.0
        for (i in 0 until windowSize) {
            val v = input[offset + i] - mean
            x[i] = v
            energy += v * v
        }
        rms = sqrt(energy / windowSize).toFloat()
        if (energy < 1e-10) return -1f

        // Autocorrelation r(tau) = IFFT(|FFT(x)|^2), zero padded to avoid circular wrap.
        x.copyInto(re)
        re.fill(0f, windowSize, fftSize)
        im.fill(0f)
        fft.forward(re, im)
        for (k in 0 until fftSize) {
            re[k] = re[k] * re[k] + im[k] * im[k]
            im[k] = 0f
        }
        // The power spectrum is real and even, so a second forward FFT equals N * IFFT.
        fft.forward(re, im)
        val scale = 1.0 / fftSize

        var m = 2.0 * energy
        for (tau in 0..maxTau + 1) {
            nsdf[tau] = if (m > 1e-12) (2.0 * re[tau] * scale / m).toFloat() else 0f
            val a = x[tau]
            val b = x[windowSize - 1 - tau]
            m -= a * a + b * b
        }

        // Key maxima: the highest point of each positive lobe after the first zero crossing.
        var count = 0
        var best = 0f
        var tau = 1
        while (tau <= maxTau && nsdf[tau] > 0f) tau++
        while (tau <= maxTau && count < peaks.size) {
            while (tau <= maxTau && nsdf[tau] <= 0f) tau++
            if (tau > maxTau) break
            var peak = tau
            while (tau <= maxTau && nsdf[tau] > 0f) {
                if (nsdf[tau] > nsdf[peak]) peak = tau
                tau++
            }
            if (peak >= minTau) {
                peaks[count++] = peak
                if (nsdf[peak] > best) best = nsdf[peak]
            }
        }
        if (count == 0) return -1f

        val threshold = peakThreshold * best
        var chosen = peaks[0]
        for (i in 0 until count) {
            if (nsdf[peaks[i]] >= threshold) {
                chosen = peaks[i]
                break
            }
        }

        val a = nsdf[chosen - 1]
        val b = nsdf[chosen]
        val c = nsdf[chosen + 1]
        val denom = a - 2f * b + c
        var delta = 0f
        var peakValue = b
        if (denom < 0f) {
            delta = 0.5f * (a - c) / denom
            if (abs(delta) <= 1f) peakValue = b - 0.25f * (a - c) * delta else delta = 0f
        }
        clarity = min(peakValue, 1f)
        return sampleRate / (chosen + delta)
    }
}

/** Second order IIR filter (RBJ cookbook). Used to strip rumble and bass before pitch analysis. */
class Biquad private constructor(
    private val b0: Float,
    private val b1: Float,
    private val b2: Float,
    private val a1: Float,
    private val a2: Float,
) {
    private var x1 = 0f
    private var x2 = 0f
    private var y1 = 0f
    private var y2 = 0f

    fun process(x: Float): Float {
        val y = b0 * x + b1 * x1 + b2 * x2 - a1 * y1 - a2 * y2
        x2 = x1; x1 = x
        y2 = y1; y1 = y
        return y
    }

    fun reset() {
        x1 = 0f; x2 = 0f; y1 = 0f; y2 = 0f
    }

    companion object {
        fun lowPass(sampleRate: Float, cutoff: Float, q: Float = 0.7071f): Biquad {
            val w0 = 2.0 * PI * cutoff / sampleRate
            val cw = cos(w0)
            val alpha = sin(w0) / (2.0 * q)
            val a0 = 1.0 + alpha
            return Biquad(
                b0 = ((1.0 - cw) / 2.0 / a0).toFloat(),
                b1 = ((1.0 - cw) / a0).toFloat(),
                b2 = ((1.0 - cw) / 2.0 / a0).toFloat(),
                a1 = (-2.0 * cw / a0).toFloat(),
                a2 = ((1.0 - alpha) / a0).toFloat(),
            )
        }

        fun highPass(sampleRate: Float, cutoff: Float, q: Float = 0.7071f): Biquad {
            val w0 = 2.0 * PI * cutoff / sampleRate
            val cw = cos(w0)
            val alpha = sin(w0) / (2.0 * q)
            val a0 = 1.0 + alpha
            return Biquad(
                b0 = ((1.0 + cw) / 2.0 / a0).toFloat(),
                b1 = (-(1.0 + cw) / a0).toFloat(),
                b2 = ((1.0 + cw) / 2.0 / a0).toFloat(),
                a1 = (-2.0 * cw / a0).toFloat(),
                a2 = ((1.0 - alpha) / a0).toFloat(),
            )
        }
    }
}

/**
 * Median-of-three smoothing for a stream of MIDI pitches (NaN = silence).
 * Removes single-frame octave jumps without adding noticeable latency.
 */
class PitchSmoother {
    private val recent = FloatArray(3)
    private var count = 0
    private var silent = 0

    fun push(midi: Float): Float {
        if (midi.isNaN()) {
            silent++
            if (silent >= 3) count = 0
            return Float.NaN
        }
        silent = 0
        recent[0] = recent[1]
        recent[1] = recent[2]
        recent[2] = midi
        if (count < 3) count++
        if (count < 3) return midi
        val a = recent[0]
        val b = recent[1]
        val c = recent[2]
        return max(min(a, b), min(max(a, b), c))
    }

    fun reset() {
        count = 0
        silent = 0
    }
}

package com.raulhrn.vox.core

import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Test
import java.util.Random
import kotlin.math.PI
import kotlin.math.abs
import kotlin.math.sin

class PitchDetectorTest {
    private val rate = 22050f
    private val window = 1024

    private fun tone(hz: Float, harmonics: FloatArray = floatArrayOf(1f), noise: Float = 0f, seed: Long = 1): FloatArray {
        val rnd = Random(seed)
        return FloatArray(window) { i ->
            var v = 0.0
            for (h in harmonics.indices) v += harmonics[h] * sin(2.0 * PI * hz * (h + 1) * i / rate + h)
            (0.3 * v + noise * rnd.nextGaussian()).toFloat()
        }
    }

    private fun centsError(expected: Float, actual: Float) = abs(Pitch.hzToMidi(actual) - Pitch.hzToMidi(expected)) * 100f

    @Test
    fun detectsPureTonesAcrossVocalRange() {
        val detector = PitchDetector(rate, window)
        for (hz in floatArrayOf(70f, 82.4f, 110f, 196f, 261.6f, 440f, 523.3f, 880f, 1046.5f, 1318.5f)) {
            val f = detector.detect(tone(hz))
            assertTrue("no pitch for $hz", f > 0f)
            assertTrue("$hz detected as $f", centsError(hz, f) < 5f)
            assertTrue("clarity ${detector.clarity} for $hz", detector.clarity > 0.9f)
        }
    }

    @Test
    fun voiceLikeSpectrumHasNoOctaveErrors() {
        val detector = PitchDetector(rate, window)
        // Strong second harmonic and weak fundamental, typical of a sung vowel.
        val spectrum = floatArrayOf(0.4f, 1f, 0.7f, 0.5f, 0.3f, 0.2f, 0.1f)
        for (hz in floatArrayOf(98f, 147f, 220f, 330f, 494f, 660f)) {
            val f = detector.detect(tone(hz, spectrum))
            assertTrue("$hz detected as $f", centsError(hz, f) < 10f)
        }
    }

    @Test
    fun missingFundamentalStillGivesPitch() {
        val detector = PitchDetector(rate, window)
        val f = detector.detect(tone(150f, floatArrayOf(0f, 1f, 0.8f, 0.6f)))
        assertTrue("detected $f", centsError(150f, f) < 10f)
    }

    @Test
    fun noisyToneIsStillDetected() {
        val detector = PitchDetector(rate, window)
        val f = detector.detect(tone(311f, floatArrayOf(1f, 0.5f, 0.3f), noise = 0.05f))
        assertTrue("detected $f", centsError(311f, f) < 10f)
    }

    @Test
    fun whiteNoiseHasLowClarity() {
        val detector = PitchDetector(rate, window)
        val rnd = Random(7)
        val noise = FloatArray(window) { (0.2 * rnd.nextGaussian()).toFloat() }
        detector.detect(noise)
        assertTrue("clarity ${detector.clarity}", detector.clarity < 0.6f)
    }

    @Test
    fun silenceReturnsNoPitch() {
        val detector = PitchDetector(rate, window)
        assertEquals(-1f, detector.detect(FloatArray(window)), 0f)
    }

    @Test
    fun highPassKeepsVoiceAndRemovesRumble() {
        val hp = Biquad.highPass(rate, 70f)
        var maxLow = 0f
        for (i in 0 until 8000) {
            val y = hp.process((0.5 * sin(2.0 * PI * 15.0 * i / rate)).toFloat())
            if (i > 4000) maxLow = maxOf(maxLow, abs(y))
        }
        assertTrue("15 Hz leaked $maxLow", maxLow < 0.03f)
        hp.reset()
        var maxHigh = 0f
        for (i in 0 until 8000) {
            val y = hp.process((0.5 * sin(2.0 * PI * 300.0 * i / rate)).toFloat())
            if (i > 4000) maxHigh = maxOf(maxHigh, abs(y))
        }
        assertTrue("300 Hz attenuated to $maxHigh", maxHigh > 0.45f)
    }

    @Test
    fun smootherRemovesSingleOctaveJump() {
        val s = PitchSmoother()
        s.push(60f); s.push(60.1f)
        val out = s.push(72f)
        assertEquals(60.1f, out, 0.001f)
        assertEquals(60.2f, s.push(60.2f), 0.001f)
    }
}

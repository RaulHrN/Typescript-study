package com.raulhrn.vox.core

import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Test
import java.util.Random
import kotlin.math.PI
import kotlin.math.abs
import kotlin.math.sin

class AnalysisTest {
    private val rate = 22050f

    /** Renders a "sung" melody: harmonics, 5.5 Hz vibrato, onset glides, breath noise and rests. */
    private fun sing(melody: IntArray, noteMs: Int, restMs: Int, seed: Long = 11): FloatArray {
        val rnd = Random(seed)
        val noteSamples = (noteMs * rate / 1000).toInt()
        val restSamples = (restMs * rate / 1000).toInt()
        val out = FloatArray(melody.size * (noteSamples + restSamples) + restSamples)
        val harmonics = floatArrayOf(0.6f, 1f, 0.6f, 0.4f, 0.25f, 0.15f)
        var pos = restSamples
        var prev = melody[0].toFloat()
        for (m in melody) {
            var phase = 0.0
            for (i in 0 until noteSamples) {
                val t = i / rate
                val glide = if (t < 0.06f) (prev - m) * (1f - t / 0.06f) else 0f
                val vibrato = 0.3f * sin(2.0 * PI * 5.5 * t).toFloat() * minOf(1f, t / 0.3f)
                val hz = Pitch.midiToHz(m + glide + vibrato)
                phase += 2.0 * PI * hz / rate
                var v = 0.0
                for (h in harmonics.indices) v += harmonics[h] * sin(phase * (h + 1))
                val env = minOf(1f, i / 400f) * minOf(1f, (noteSamples - i) / 600f)
                out[pos + i] = (0.15 * v * env + 0.01 * rnd.nextGaussian()).toFloat()
            }
            prev = m.toFloat()
            pos += noteSamples + restSamples
        }
        for (i in out.indices) if (out[i] == 0f) out[i] = (0.003 * rnd.nextGaussian()).toFloat()
        return out
    }

    @Test
    fun sungMelodyIsTranscribed() {
        // "Brilha, brilha, estrelinha" in G major.
        val melody = intArrayOf(55, 55, 62, 62, 64, 64, 62, 60, 60, 59, 59, 57, 57, 55)
        val audio = sing(melody, noteMs = 420, restMs = 80)
        val analyzer = TrackAnalyzer(rate)
        for (s in audio) analyzer.push(s)
        val notes = Segmenter.segment(analyzer.track(), analyzer.hopMs, analyzer.firstFrameMs, onsets = Segmenter.onsets(analyzer.levels()))
        assertEquals(melody.toList(), notes.map { it.midi })
        for ((i, n) in notes.withIndex()) {
            val expectedStart = 80 + i * 500
            assertTrue("note $i starts at ${n.startMs}, expected ~$expectedStart", abs(n.startMs - expectedStart) < 80)
        }
        assertEquals(Key(7, false), KeyDetector.detect(notes))
        val second = Harmony.voice(notes, Key(7, false), HarmonyMode.THIRD_ABOVE).map { it.midi }
        assertEquals(listOf(59, 59, 66, 66, 67, 67, 66, 64, 64, 62, 62, 60, 60, 59), second)
    }

    @Test
    fun legatoMelodyWithoutRests() {
        val melody = intArrayOf(60, 62, 64, 65, 67, 65, 64, 62, 60)
        val audio = sing(melody, noteMs = 350, restMs = 0)
        val analyzer = TrackAnalyzer(rate)
        for (s in audio) analyzer.push(s)
        val notes = Segmenter.segment(analyzer.track(), analyzer.hopMs, analyzer.firstFrameMs, onsets = Segmenter.onsets(analyzer.levels()))
        assertEquals(melody.toList(), notes.map { it.midi })
    }

    @Test
    fun detectorIsFastEnoughForRealTime() {
        val detector = PitchDetector(rate, 1024)
        val buf = FloatArray(1024) { (0.3 * sin(2.0 * PI * 220.0 * it / rate)).toFloat() }
        repeat(2000) { detector.detect(buf) } // warm up the JIT
        val frames = 5000
        val t0 = System.nanoTime()
        repeat(frames) { detector.detect(buf) }
        val perFrameUs = (System.nanoTime() - t0) / 1000.0 / frames
        println("PitchDetector: %.1f us per frame (budget 23 220 us)".format(perFrameUs))
        assertTrue(perFrameUs < 2000)
    }
}

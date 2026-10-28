package com.raulhrn.vox.core

import org.junit.Assert.assertTrue
import org.junit.Test
import java.util.Random
import kotlin.math.PI
import kotlin.math.exp
import kotlin.math.sin

/**
 * Melody extraction on synthetic songs: a sung line (centre, vibrato) mixed with
 * stereo chords, bass, a plucked guitar in the same register and drums.
 */
class ExtractionTest {
    private val rate = 22050f
    private val noteMs = 450
    private val restMs = 50

    // "Ode à Alegria" in C, twice.
    private val melody = intArrayOf(64, 64, 65, 67, 67, 65, 64, 62, 60, 60, 62, 64, 64, 62, 62, 64, 64, 65, 67, 67, 65, 64, 62, 60, 60, 62, 64, 62, 60, 60)
    private val voiceStartMs = 1000
    private val totalMs = voiceStartMs + melody.size * (noteMs + restMs) + 1000

    private fun samples(ms: Int) = (ms * rate / 1000).toInt()

    private fun addTone(
        l: FloatArray, r: FloatArray, startMs: Int, durMs: Int, midi: Float, amp: Float,
        panL: Float, panR: Float, harmonics: Int, tilt: Double, decayMs: Float = 0f, vibrato: Float = 0f,
        drift: Random? = null,
    ) {
        val start = samples(startMs)
        val n = samples(durMs)
        var phase = 0.0
        var wander = 0f
        for (i in 0 until n) {
            val idx = start + i
            if (idx >= l.size) break
            val t = i / rate
            // Natural pitch drift of a straight (non-vibrato) voice: slow random wander of ~10 cents.
            if (drift != null && i % 64 == 0) wander = (0.995f * wander + 0.012f * drift.nextGaussian().toFloat()).coerceIn(-0.2f, 0.2f)
            val vib = vibrato * sin(2.0 * PI * 5.5 * t).toFloat() * minOf(1f, t / 0.25f) + wander
            val hz = Pitch.midiToHz(midi + vib)
            phase += 2.0 * PI * hz / rate
            var v = 0.0
            for (h in 1..harmonics) v += sin(phase * h) / Math.pow(h.toDouble(), tilt)
            var env = minOf(1f, i / (0.02f * rate)) * minOf(1f, (n - i) / (0.03f * rate))
            if (decayMs > 0f) env *= exp(-t * 1000f / decayMs)
            val s = (v * amp * env).toFloat()
            l[idx] += s * panL
            r[idx] += s * panR
        }
    }

    private fun song(
        withVoice: Boolean,
        withBand: Boolean,
        seed: Long = 9,
        voiceAmp: Float = 0.10f,
        mono: Boolean = false,
        vibrato: Float = 0.3f,
    ): Pair<FloatArray, FloatArray> {
        val len = samples(totalMs)
        val l = FloatArray(len)
        val r = FloatArray(len)
        if (withVoice) {
            for ((i, m) in melody.withIndex()) {
                // Voice: centre, vibrato, a scoop into each note, strong low harmonics.
                addTone(
                    l, r, voiceStartMs + i * (noteMs + restMs), noteMs, m.toFloat(), voiceAmp, 1f, 1f, 8, 0.9,
                    vibrato = vibrato, drift = if (vibrato == 0f) Random(seed + i) else null,
                )
            }
        }
        if (withBand) {
            val chords = listOf(intArrayOf(60, 64, 67), intArrayOf(65, 69, 72), intArrayOf(67, 71, 74), intArrayOf(60, 64, 67))
            val bass = intArrayOf(36, 41, 43, 36)
            var t = 0
            var c = 0
            while (t < totalMs) {
                val chord = chords[c % chords.size]
                // Wide stereo pad, flat pitch.
                addTone(l, r, t, 2000, chord[0].toFloat(), 0.05f, 1f, 0.2f, 10, 1.0)
                addTone(l, r, t, 2000, chord[1].toFloat(), 0.05f, 0.2f, 1f, 10, 1.0)
                addTone(l, r, t, 2000, chord[2].toFloat(), 0.04f, 0.6f, 0.6f, 10, 1.0)
                // Bass in the centre, loud.
                addTone(l, r, t, 2000, bass[c % bass.size].toFloat(), 0.16f, 1f, 1f, 5, 1.0)
                // Plucked guitar arpeggio in the vocal register, panned left.
                for (k in 0 until 8) {
                    val note = chord[k % 3] + if (k % 4 == 3) 12 else 0
                    addTone(l, r, t + k * 250, 600, note.toFloat(), 0.09f, 0.9f, 0.35f, 6, 1.5, decayMs = 250f)
                }
                t += 2000
                c++
            }
            // Drums: noise bursts in the centre.
            val rnd = Random(seed)
            var d = 0
            while (d < totalMs) {
                val s = samples(d)
                for (i in 0 until samples(60)) {
                    if (s + i >= len) break
                    val v = (rnd.nextGaussian() * 0.08 * exp(-i / (0.015 * rate))).toFloat()
                    l[s + i] += v
                    r[s + i] += v
                }
                d += 500
            }
        }
        if (mono) {
            for (i in l.indices) {
                val m = (l[i] + r[i]) * 0.5f
                l[i] = m
                r[i] = m
            }
        }
        return l to r
    }

    private fun extract(l: FloatArray, r: FloatArray): Pair<MelodyExtractor.Result, List<NoteEvent>> {
        val ex = MelodyExtractor(rate)
        for (i in l.indices) ex.push(l[i], r[i])
        val res = ex.finish()
        return res to Segmenter.segment(res.track, res.hopMs, res.firstFrameMs, onsets = Segmenter.onsets(res.levels))
    }

    /** Share of sung notes found with the right pitch at their centre. */
    private fun accuracy(notes: List<NoteEvent>): Float {
        var ok = 0
        for ((i, m) in melody.withIndex()) {
            val centre = voiceStartMs + i * (noteMs + restMs) + noteMs / 2
            if (notes.any { centre in it.startMs..it.endMs && it.midi == m }) ok++
        }
        return ok.toFloat() / melody.size
    }

    @Test
    fun findsTheVoiceInsideAFullMix() {
        val (l, r) = song(withVoice = true, withBand = true)
        val (_, notes) = extract(l, r)
        val acc = accuracy(notes)

        // The old approach (single-pitch detector on the mono mix) for comparison.
        val old = TrackAnalyzer(rate, minClarity = 0.8f, highPassHz = 110f)
        for (i in l.indices) old.push((l[i] + r[i]) * 0.5f)
        val oldNotes = Segmenter.segment(old.track(), old.hopMs, old.firstFrameMs, onsets = Segmenter.onsets(old.levels()))
        val oldAcc = accuracy(oldNotes)
        println("Melody accuracy in a full mix: new %.0f%%, old %.0f%%".format(acc * 100, oldAcc * 100))

        assertTrue("accuracy $acc", acc >= 0.8f)
        assertTrue("not better than before ($acc vs $oldAcc)", acc > oldAcc + 0.2f)
        // Instrument notes outside the sung part are not reported as melody.
        val outside = notes.count { it.endMs < voiceStartMs - 200 || it.startMs > voiceStartMs + melody.size * (noteMs + restMs) + 200 }
        assertTrue("$outside notes outside the vocal part", outside <= 2)
    }

    @Test
    fun quieterVoiceAndMonoMix() {
        val (l1, r1) = song(withVoice = true, withBand = true, voiceAmp = 0.06f)
        val quiet = accuracy(extract(l1, r1).second)
        val (l2, r2) = song(withVoice = true, withBand = true, mono = true)
        val mono = accuracy(extract(l2, r2).second)
        println("Quieter voice: %.0f%%, mono mix: %.0f%%".format(quiet * 100, mono * 100))
        assertTrue("quiet $quiet", quiet >= 0.7f)
        assertTrue("mono $mono", mono >= 0.7f)
    }

    @Test
    fun straightVoiceWithoutVibrato() {
        val (l, r) = song(withVoice = true, withBand = true, vibrato = 0f)
        val acc = accuracy(extract(l, r).second)
        println("Voice without vibrato: %.0f%%".format(acc * 100))
        assertTrue("accuracy $acc", acc >= 0.6f)
    }

    @Test
    fun aCappellaVoiceIsTranscribed() {
        val (l, r) = song(withVoice = true, withBand = false)
        val (_, notes) = extract(l, r)
        assertTrue("accuracy ${accuracy(notes)}", accuracy(notes) >= 0.9f)
    }

    @Test
    fun instrumentalOnlyHasAlmostNoMelody() {
        val (l, r) = song(withVoice = false, withBand = true)
        val (res, notes) = extract(l, r)
        println("Instrumental only: voiced %.0f%%, %d notes".format(res.voicedRatio * 100, notes.size))
        assertTrue("voiced ${res.voicedRatio}", res.voicedRatio < 0.3f)
    }

    @Test
    fun extractionIsFast() {
        val (l, r) = song(withVoice = true, withBand = true)
        val t0 = System.nanoTime()
        repeat(3) { extract(l, r) }
        val secondsOfAudio = l.size / rate * 3
        val ms = (System.nanoTime() - t0) / 1e6
        println("Extraction: %.0f ms for %.0f s of audio".format(ms, secondsOfAudio))
        assertTrue(ms < secondsOfAudio * 1000 / 5)
    }
}

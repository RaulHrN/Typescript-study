package com.raulhrn.vox.core

import org.junit.Assert.assertArrayEquals
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNotNull
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Test
import kotlin.math.roundToInt

class MusicTest {
    @Test
    fun midiAndFrequencyRoundTrip() {
        assertEquals(69f, Pitch.hzToMidi(440f), 1e-4f)
        assertEquals(60f, Pitch.hzToMidi(261.6256f), 1e-3f)
        assertEquals(440f, Pitch.midiToHz(69f), 1e-3f)
        assertEquals(69f, Pitch.hzToMidi(432f, a4 = 432f), 1e-4f)
    }

    @Test
    fun foldedCentsForgivesOctaves() {
        assertEquals(0f, Pitch.foldedCents(72f, 60), 1e-3f)
        assertEquals(20f, Pitch.foldedCents(48.2f, 60), 1e-2f)
        assertEquals(-30f, Pitch.foldedCents(59.7f, 60), 1e-2f)
    }

    @Test
    fun noteNames() {
        assertEquals("Lá4", NoteNames.full(69, Naming.SOLFEGE))
        assertEquals("C4", NoteNames.full(60, Naming.LETTERS))
        assertEquals("Sib", NoteNames.pitchClass(10, Naming.SOLFEGE, flats = true))
        assertEquals("F#3", NoteNames.full(54, Naming.LETTERS))
        assertEquals(-1, NoteNames.octave(0))
    }

    @Test
    fun keyDetection() {
        // "Parabéns pra você" in C major.
        val melody = listOf(67, 67, 69, 67, 72, 71, 67, 67, 69, 67, 74, 72, 67, 67, 79, 76, 72, 71, 69, 77, 77, 76, 72, 74, 72)
        val notes = melody.mapIndexed { i, m -> NoteEvent(i * 400, 380, m) }
        assertEquals(Key(0, false), KeyDetector.detect(notes))

        // A natural minor scale up and down, with a long tonic.
        val minor = listOf(57, 59, 60, 62, 64, 65, 67, 69, 67, 65, 64, 62, 60, 59, 57)
        val minorNotes = minor.mapIndexed { i, m -> NoteEvent(i * 400, if (m == 57) 1500 else 380, m) }
        assertEquals(Key(9, true), KeyDetector.detect(minorNotes))
    }

    @Test
    fun diatonicThirdsInMajor() {
        val c = Key(0, false)
        // C D E F G A B C -> E F G A B C D E
        val melody = intArrayOf(60, 62, 64, 65, 67, 69, 71, 72)
        val above = melody.map { Harmony.note(it, c, 2) }.toIntArray()
        assertArrayEquals(intArrayOf(64, 65, 67, 69, 71, 72, 74, 76), above)
        val below = melody.map { Harmony.note(it, c, -2) }.toIntArray()
        assertArrayEquals(intArrayOf(57, 59, 60, 62, 64, 65, 67, 69), below)
    }

    @Test
    fun chromaticNotesGetMusicalThirds() {
        val c = Key(0, false)
        assertEquals(69, Harmony.note(66, c, 2)) // F# -> A (D7)
        assertEquals(74, Harmony.note(70, c, 2)) // Bb -> D
        assertEquals(67, Harmony.note(63, c, 2)) // Eb -> G
        val am = Key(9, true)
        assertEquals(71, Harmony.note(68, am, 2)) // G# -> B (E major chord)
        assertEquals(64, Harmony.note(68, am, -2)) // G# -> E
    }

    @Test
    fun harmonyModesShiftOctaves() {
        val g = Key(7, false)
        val melody = listOf(NoteEvent(0, 500, 67), NoteEvent(500, 500, 71))
        val sixthBelow = Harmony.voice(melody, g, HarmonyMode.SIXTH_BELOW)
        assertEquals(listOf(59, 62), sixthBelow.map { it.midi })
        val sixthAbove = Harmony.voice(melody, g, HarmonyMode.SIXTH_ABOVE)
        assertEquals(listOf(76, 79), sixthAbove.map { it.midi })
    }

    @Test
    fun voiceTypes() {
        assertEquals(VoiceType.BARITONE, VoiceType.classify(45, 68))
        assertEquals(VoiceType.SOPRANO, VoiceType.classify(60, 86))
        assertEquals(VoiceType.TENOR, VoiceType.classify(48, 72))
        assertEquals(VoiceType.MEZZO, VoiceType.classify(57, 80))
    }

    @Test
    fun notationParsing() {
        val r = Notation.parse("Dó Ré Mi:2 - Fá#4 sol/0.5 C5 Bb4 si", 120)
        assertNull(r.error)
        assertEquals(listOf(60, 62, 64, 66, 67, 72, 70, 71), r.notes.map { it.midi })
        assertEquals(0, r.notes[0].startMs)
        assertEquals(500, r.notes[1].startMs)
        assertEquals(1000, r.notes[2].startMs)
        // Mi lasts 2 beats, then a 2-beat rest (the last duration is kept).
        assertEquals(3000, r.notes[3].startMs)
        assertNotNull(Notation.parse("xyz", 100).error)
        assertNotNull(Notation.parse("", 100).error)
    }

    @Test
    fun notationPicksClosestOctave() {
        val r = Notation.parse("Si4 Dó Ré", 100)
        assertEquals(listOf(71, 72, 74), r.notes.map { it.midi })
        val down = Notation.parse("Dó5 Si Lá", 100)
        assertEquals(listOf(72, 71, 69), down.notes.map { it.midi })
    }

    @Test
    fun phrasesSplitAtRests() {
        val notes = listOf(
            NoteEvent(0, 400, 60), NoteEvent(400, 400, 62), NoteEvent(800, 400, 64),
            NoteEvent(2500, 400, 65), NoteEvent(2900, 400, 67),
        )
        val p = Phrases.split(notes)
        assertEquals(2, p.size)
        assertEquals(Phrase(0, 1200), p[0])
        assertEquals(Phrase(2500, 3300), p[1])
    }

    @Test
    fun segmenterFindsNotesInNoisyTrack() {
        val hop = 23.2f
        val rnd = java.util.Random(3)
        val track = FloatList()
        // 400 ms of each note, slightly sharp (+20 cents) with vibrato-like jitter and short gaps.
        val melody = intArrayOf(60, 62, 64, 64, 67)
        for (m in melody) {
            repeat(17) { track.add(m + 0.2f + (rnd.nextGaussian() * 0.12).toFloat()) }
            repeat(4) { track.add(Float.NaN) }
        }
        // Onset glide on the last note and one octave glitch.
        track.data[(4 * 21)] = 66f
        track.data[(2 * 21) + 8] = 76f
        val notes = Segmenter.segment(track.toArray(), hop)
        assertEquals(melody.toList(), notes.map { it.midi })
        for ((i, n) in notes.withIndex()) {
            assertEquals((i * 21 * hop), n.startMs.toFloat(), 3 * hop)
            assertTrue(n.durMs in 300..450)
        }
    }

    @Test
    fun segmenterSplitsLegatoSteps() {
        val hop = 23.2f
        val track = FloatList()
        for (m in intArrayOf(67, 69, 71, 72)) repeat(15) { track.add(m.toFloat()) }
        val notes = Segmenter.segment(track.toArray(), hop)
        assertEquals(listOf(67, 69, 71, 72), notes.map { it.midi })
        assertTrue(kotlin.math.abs((15 * hop).roundToInt() - notes[1].startMs) <= 2 * hop.roundToInt())
    }
}

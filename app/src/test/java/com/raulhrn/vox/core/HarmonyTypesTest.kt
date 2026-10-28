package com.raulhrn.vox.core

import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Test
import kotlin.math.abs

class HarmonyTypesTest {
    private val c = Key(0, false)

    private fun line(vararg midi: Int, dur: Int = 450, gap: Int = 50) =
        midi.mapIndexed { i, m -> NoteEvent(i * (dur + gap), dur, m) }

    // "Ode à Alegria" in C, around C4.
    private val ode = line(64, 64, 65, 67, 67, 65, 64, 62, 60, 60, 62, 64, 64, 62, 62)

    private fun intervals(melody: List<NoteEvent>, voice: List<NoteEvent>) =
        melody.zip(voice).map { (m, h) -> abs(h.midi - m.midi) }

    @Test
    fun guidedStaysInRangeAndMovesLittle() {
        val range = 52..64
        val v = Harmony.voice(ode, c, HarmonyMode.GUIDED, range = range)
        assertEquals(ode.size, v.size)
        assertTrue(v.all { it.midi in range })
        for (iv in intervals(ode, v)) assertTrue("interval $iv", iv % 12 in setOf(3, 4, 8, 9))
        val maxLeap = v.zipWithNext().maxOf { (a, b) -> abs(a.midi - b.midi) }
        assertTrue("leap $maxLeap", maxLeap <= 4)
        // Every note stays in the key.
        assertTrue(v.all { c.degreeOf(it.midi) >= 0 })
    }

    @Test
    fun guidedCanBeForcedAboveOrBelow() {
        val above = Harmony.guided(ode, c, 55..79, preferAbove = true)
        assertTrue(ode.zip(above).all { (m, h) -> h.midi > m.midi })
        val below = Harmony.guided(ode, c, 45..67, preferAbove = false)
        assertTrue(ode.zip(below).all { (m, h) -> h.midi < m.midi })
    }

    @Test
    fun contraryMovesAgainstTheMelody() {
        val scale = line(60, 62, 64, 65, 67, 69, 71, 72)
        val v = Harmony.voice(scale, c, HarmonyMode.CONTRARY, range = 48..72)
        for (iv in intervals(scale, v)) assertTrue("interval $iv", iv in setOf(3, 4, 7, 8, 9, 12, 15, 16))
        var contrary = 0
        for (i in 1 until scale.size) {
            val dm = scale[i].midi - scale[i - 1].midi
            val dv = v[i].midi - v[i - 1].midi
            if (dv != 0 && (dv > 0) != (dm > 0)) contrary++
        }
        assertTrue("only $contrary contrary moves in ${v.map { it.midi }}", contrary >= 4)
        // No parallel fifths or octaves.
        for (i in 1 until scale.size) {
            val a = abs(v[i - 1].midi - scale[i - 1].midi) % 12
            val b = abs(v[i].midi - scale[i].midi) % 12
            if (v[i].midi != v[i - 1].midi) assertTrue(!(a == b && (a == 0 || a == 7)))
        }
    }

    @Test
    fun pedalHoldsOneNote() {
        // "Brilha, brilha": C C G G A A G | F F E E D D C
        val brilha = line(60, 60, 67, 67, 69, 69, 67, 65, 65, 64, 64, 62, 62, 60)
        val v = Harmony.voice(brilha, c, HarmonyMode.PEDAL, range = 48..60)
        assertTrue("too many notes: ${v.map { it.midi }}", v.size <= 6)
        val longest = v.maxBy { it.durMs }
        assertEquals(0, Math.floorMod(longest.midi, 12)) // the tonic
        assertTrue(longest.durMs >= 2000)
        // Every melody note is consonant with the voice sounding under it.
        for (n in brilha) {
            val h = v.first { n.startMs >= it.startMs && n.startMs < it.endMs }
            assertTrue("${n.midi} over ${h.midi}", abs(n.midi - h.midi) % 12 !in setOf(1, 2, 6, 10, 11))
        }
    }

    @Test
    fun unisonFitsTheRange() {
        val v = Harmony.voice(ode, c, HarmonyMode.UNISON, range = 72..86)
        assertEquals(ode.map { it.midi + 12 }, v.map { it.midi })
    }

    @Test
    fun parallelTypesIgnoreTheRange() {
        val v = Harmony.voice(ode, c, HarmonyMode.THIRD_ABOVE, range = 30..40)
        assertEquals(Harmony.note(64, c, 2), v[0].midi)
    }

    @Test
    fun everyTypeHasALesson() {
        for (mode in HarmonyMode.entries) {
            val lesson = SecondVoiceLessons.of(mode)
            assertTrue(Exercises.byId(lesson.exerciseId) != null)
        }
    }
}

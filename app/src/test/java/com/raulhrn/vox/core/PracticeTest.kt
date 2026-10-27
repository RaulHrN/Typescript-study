package com.raulhrn.vox.core

import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Test

class PracticeTest {
    private val scale = Exercise(
        id = "t", title = "t", category = "t", summary = "", steps = emptyList(),
        pattern = intArrayOf(0, 2, 4, 5, 7, 5, 4, 2, 0), bpm = 120,
    )

    @Test
    fun timelineHasListenThenSing() {
        val tl = ExerciseTimeline.build(scale, 60)
        val listen = tl.notes.filter { it.role == Role.LISTEN }
        val sing = tl.notes.filter { it.role == Role.TARGET }
        assertEquals(9, listen.size)
        assertEquals(9, sing.size)
        assertTrue(listen.last().endMs < tl.singStartMs)
        assertEquals(tl.singStartMs, sing.first().startMs)
        assertEquals(listOf(60, 62, 64, 65, 67, 65, 64, 62, 60), sing.map { it.midi })
        assertTrue(tl.lengthMs > sing.last().endMs)
    }

    @Test
    fun harmonyExerciseTargetsTheSecondVoice() {
        val ex = Exercise(
            id = "h", title = "h", category = "h", summary = "", steps = emptyList(),
            pattern = intArrayOf(0, 2, 4), harmonySteps = 2,
        )
        val tl = ExerciseTimeline.build(ex, 62) // D major
        val targets = tl.notes.filter { it.role == Role.TARGET }.map { it.midi }
        val accomp = tl.notes.filter { it.role == Role.ACCOMP }.map { it.midi }
        assertEquals(listOf(62, 64, 66), accomp)
        assertEquals(listOf(66, 67, 69), targets)
    }

    @Test
    fun rootsStayInsideRange() {
        val roots = ExercisePlan.roots(scale, 48, 67)
        assertTrue(roots.isNotEmpty())
        for (r in roots) {
            assertTrue(r >= 48)
            assertTrue(r + 7 <= 67)
        }
        // Goes up then comes back down.
        val peak = roots.indexOf(roots.max())
        assertTrue(roots.take(peak + 1).zipWithNext().all { (a, b) -> b > a })
        assertTrue(roots.drop(peak).zipWithNext().all { (a, b) -> b < a })
    }

    @Test
    fun narrowRangeStillGivesARoot() {
        val roots = ExercisePlan.roots(scale, 60, 64)
        assertTrue(roots.isNotEmpty())
    }

    @Test
    fun echoRootsAreRandomButInRange() {
        val ex = Exercise("e", "e", "e", "", emptyList(), kind = ExerciseKind.ECHO)
        val roots = ExercisePlan.roots(ex, 50, 62, seed = 42)
        assertEquals(10, roots.size)
        assertTrue(roots.all { it in 51..61 })
        assertTrue(roots.toList().zipWithNext().none { (a, b) -> a == b })
    }

    @Test
    fun scorerCountsHitsWithinTolerance() {
        val targets = listOf(TimedNote(0, 1000, 60, Role.TARGET), TimedNote(1000, 1000, 64, Role.TARGET))
        val s = Scorer(targets)
        var t = 0f
        while (t < 2000f) {
            val midi = if (t < 1000f) 60.2f else 65f // first in tune, second a semitone sharp
            s.add(t, midi)
            t += 23f
        }
        assertEquals(1f, s.noteAccuracy(0), 1e-4f)
        assertEquals(0f, s.noteAccuracy(1), 1e-4f)
        assertEquals(0.5f, s.total(), 1e-4f)
    }

    @Test
    fun scorerForgivesOctaveAndCountsSilenceAsMiss() {
        val s = Scorer(listOf(TimedNote(0, 1000, 60, Role.TARGET)))
        var t = 0f
        var i = 0
        while (t < 1000f) {
            s.add(t, if (i % 2 == 0) 72.1f else Float.NaN)
            t += 20f
            i++
        }
        assertEquals(0.5f, s.noteAccuracy(0), 0.05f)
    }

    @Test
    fun followerHasHysteresisAndHistory() {
        val f = NoteFollower()
        var now = 0L
        fun feed(m: Float, frames: Int) = repeat(frames) { f.update(m, now); now += 23 }
        feed(60.1f, 10)
        assertEquals(60, f.note)
        feed(60.55f, 3) // within hysteresis: still 60
        assertEquals(60, f.note)
        feed(62f, 10)
        assertEquals(62, f.note)
        feed(Float.NaN, 15)
        assertEquals(-1, f.note)
        feed(62f, 10)
        assertEquals(listOf(60, 62, 62), f.recentNotes().toList())
    }
}

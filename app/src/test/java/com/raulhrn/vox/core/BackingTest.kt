package com.raulhrn.vox.core

import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Test
import kotlin.math.abs

class BackingTest {
    private val c = Key(0, false)

    private fun line(vararg midi: Int, dur: Int = 450, gap: Int = 50) =
        midi.mapIndexed { i, m -> NoteEvent(i * (dur + gap), dur, m) }

    private val ode = line(64, 64, 65, 67, 67, 65, 64, 62, 60, 60, 62, 64, 64, 62, 62)

    @Test
    fun relationFollowsVoiceTypes() {
        assertEquals(Relation.LOWER, Backing.relation(VoiceType.TENOR, VoiceType.BASS.range))
        assertEquals(Relation.HIGHER, Backing.relation(VoiceType.BARITONE, VoiceType.SOPRANO.range))
        assertEquals(Relation.SAME, Backing.relation(VoiceType.TENOR, VoiceType.TENOR.range))
    }

    @Test
    fun leadIsPlacedInTheLeadRegister() {
        val plan = Backing.arrange(ode, c, BackingStyle.HARMONY, VoiceType.BARITONE, VoiceType.TENOR.range)
        val median = Harmony.median(plan.lead)
        assertTrue("median $median", median in VoiceType.BARITONE.low..VoiceType.BARITONE.high)
        assertEquals(0, plan.leadShift % 12)
    }

    @Test
    fun lowerBackingSingsBelowAndSofter() {
        val plan = Backing.arrange(ode, c, BackingStyle.HARMONY, VoiceType.TENOR, VoiceType.BARITONE.range)
        assertEquals(Relation.LOWER, plan.relation)
        val part = plan.parts.single()
        assertTrue(plan.lead.zip(part.notes).all { (l, b) -> b.midi < l.midi })
        assertTrue(part.notes.all { it.midi in VoiceType.BARITONE.range })
        assertTrue(part.gainDb in -7f..-4f)
        assertTrue(plan.tips.isNotEmpty())
    }

    @Test
    fun femaleBackingOverMaleLeadSingsAboveAndQuieter() {
        val plan = Backing.arrange(ode, c, BackingStyle.HARMONY, VoiceType.BARITONE, VoiceType.MEZZO.range)
        assertEquals(Relation.HIGHER, plan.relation)
        val part = plan.parts.single()
        assertTrue(plan.lead.zip(part.notes).all { (l, b) -> b.midi > l.midi })
        assertTrue(part.notes.all { it.midi in VoiceType.MEZZO.range })
        assertTrue("gain ${part.gainDb}", part.gainDb <= -8f)
        assertTrue(plan.tips.any { "oitava" in it })
    }

    @Test
    fun trioFormsTriads() {
        val plan = Backing.arrange(ode, c, BackingStyle.HARMONY, VoiceType.TENOR, VoiceType.TENOR.range, voices = 2)
        assertEquals(2, plan.parts.size)
        val (hi, lo) = plan.parts
        for (i in plan.lead.indices) {
            val pcs = setOf(plan.lead[i].midi, hi.notes[i].midi, lo.notes[i].midi).map { Math.floorMod(it, 12) }.toSet()
            assertEquals("chord at $i: $pcs", 3, pcs.size)
            // Some major or minor triad of C major contains all three notes.
            val ok = (0 until 7).any { r ->
                val tones = intArrayOf(r, (r + 2) % 7, (r + 4) % 7).map { Key.MAJOR[it] }.toSet()
                tones.containsAll(pcs) && r != 6
            }
            assertTrue("not a triad: $pcs", ok)
            assertTrue(hi.notes[i].midi > lo.notes[i].midi)
        }
        assertTrue(plan.parts[0].gainDb < plan.parts[1].gainDb)
    }

    @Test
    fun padHoldsChordTones() {
        val plan = Backing.arrange(ode, c, BackingStyle.PAD, VoiceType.TENOR, VoiceType.BARITONE.range)
        val pad = plan.parts.single().notes
        assertTrue("pad has ${pad.size} notes", pad.size in 3..ode.size / 2)
        assertTrue(pad.all { c.degreeOf(it.midi) >= 0 })
        assertTrue(pad.all { it.midi in VoiceType.BARITONE.range })
        assertTrue(plan.parts.single().gainDb <= -9f)
        // Every melody note is consonant with the pad note under it.
        for (n in plan.lead) {
            val p = pad.firstOrNull { n.startMs >= it.startMs && n.startMs < it.endMs + 100 } ?: continue
            assertTrue("${n.midi} over ${p.midi}", abs(n.midi - p.midi) % 12 !in setOf(1, 6, 11))
        }
    }

    @Test
    fun echoAnswersInTheGaps() {
        val phrase1 = line(60, 62, 64, 60)
        val phrase2 = line(64, 65, 67).map { it.copy(startMs = it.startMs + 3500) }
        val melody = phrase1 + phrase2
        val plan = Backing.arrange(melody, c, BackingStyle.ECHO, VoiceType.TENOR, VoiceType.TENOR.range)
        val echo = plan.parts.single().notes
        assertTrue(echo.isNotEmpty())
        // No echo note overlaps the lead.
        for (e in echo) for (l in plan.lead) assertTrue(e.endMs <= l.startMs || e.startMs >= l.endMs)
        // The first answer repeats the end of the first phrase.
        val first = echo.filter { it.startMs < 3500 }.map { Math.floorMod(it.midi, 12) }
        assertEquals(listOf(0, 2, 4, 0).takeLast(first.size), first)
    }

    @Test
    fun doubleIsUnisonOrOctave() {
        val plan = Backing.arrange(ode, c, BackingStyle.DOUBLE, VoiceType.TENOR, VoiceType.SOPRANO.range)
        val d = plan.parts.single().notes
        assertTrue(plan.lead.zip(d).all { (l, b) -> (b.midi - l.midi) % 12 == 0 })
        assertTrue(d.all { it.midi in VoiceType.SOPRANO.low..VoiceType.SOPRANO.high })
    }

    @Test
    fun exerciseTimelinesForBackingAndHarmonyTypes() {
        val range = 48..67
        for (id in listOf("backing-pad", "backing-resposta", "backing-dobra", "backing-trio", "segunda-conduzida", "segunda-pedal", "segunda-contraria")) {
            val ex = Exercises.byId(id)!!
            val roots = ExercisePlan.roots(ex, range.first, range.last)
            val tl = ExerciseTimeline.build(ex, roots.first(), range)
            assertTrue("$id has no targets", tl.targets.isNotEmpty())
            assertTrue("$id targets start before singing", tl.targets.all { it.startMs >= tl.singStartMs })
            assertTrue("$id targets out of order", tl.targets.zipWithNext().all { (a, b) -> a.startMs <= b.startMs })
            assertTrue("$id ends early", tl.lengthMs > tl.targets.last().endMs)
        }
        val trio = ExerciseTimeline.build(Exercises.byId("backing-trio")!!, 55, range)
        assertTrue(trio.notes.any { it.role == Role.ACCOMP && it.gain < 1f })
    }
}

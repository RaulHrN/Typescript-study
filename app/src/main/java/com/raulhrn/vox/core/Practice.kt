package com.raulhrn.vox.core

import kotlin.math.abs
import kotlin.math.max
import kotlin.math.min
import kotlin.math.roundToInt

/** How a note in a practice timeline is used. */
enum class Role {
    /** Played by the app for the singer to hear first. */
    LISTEN,

    /** What the singer must sing. Scored, and only played when the guide is on. */
    TARGET,

    /** Played while the singer sings (e.g. the melody under a second voice). */
    ACCOMP,
}

class TimedNote(val startMs: Int, val durMs: Int, val midi: Int, val role: Role) {
    val endMs: Int get() = startMs + durMs
}

class Timeline(val notes: List<TimedNote>, val lengthMs: Int, val singStartMs: Int) {
    val targets: List<TimedNote> = notes.filter { it.role == Role.TARGET }
}

enum class ExerciseKind { PATTERN, SUSTAIN, ECHO, BREATH }

class Exercise(
    val id: String,
    val title: String,
    val category: String,
    val summary: String,
    val steps: List<String>,
    val kind: ExerciseKind = ExerciseKind.PATTERN,
    /** Semitone offsets from the root (major scale based). */
    val pattern: IntArray = intArrayOf(0),
    /** Beats per note; defaults to one beat each. */
    val beats: FloatArray? = null,
    val bpm: Int = 90,
    /** Fraction of each beat that sounds (1 = legato, 0.4 = staccato). */
    val legato: Float = 0.92f,
    /** Diatonic steps for a second voice exercise (2 = third above, -2 = third below, 0 = none). */
    val harmonySteps: Int = 0,
    /** Semitones the root moves between repetitions. */
    val rootStep: Int = 1,
    val minutes: Int = 3,
    val needsHeadphones: Boolean = false,
)

object ExerciseTimeline {
    private const val LEAD_IN_MS = 400
    private const val TAIL_MS = 700

    fun build(ex: Exercise, root: Int): Timeline {
        val beatMs = 60000f / ex.bpm
        val key = Key(Math.floorMod(root, 12), false)
        val melody = ArrayList<Pair<Float, Float>>() // start, duration in ms (relative)
        var t = 0f
        for (i in ex.pattern.indices) {
            val b = ex.beats?.getOrNull(i) ?: 1f
            melody.add(t to b * beatMs)
            t += b * beatMs
        }
        val phraseMs = t
        val notes = ArrayList<TimedNote>()

        fun addPhrase(offset: Float, melodyRole: Role, harmonyRole: Role?) {
            for (i in ex.pattern.indices) {
                val (s, d) = melody[i]
                val midi = root + ex.pattern[i]
                val start = (offset + s).roundToInt()
                val dur = (d * ex.legato).roundToInt()
                notes.add(TimedNote(start, dur, midi, melodyRole))
                if (harmonyRole != null && ex.harmonySteps != 0) {
                    notes.add(TimedNote(start, dur, Harmony.note(midi, key, ex.harmonySteps), harmonyRole))
                }
            }
        }

        return when (ex.kind) {
            ExerciseKind.SUSTAIN -> {
                notes.add(TimedNote(LEAD_IN_MS, 1500, root, Role.LISTEN))
                val sing = LEAD_IN_MS + 1500 + 900
                notes.add(TimedNote(sing, 5000, root, Role.TARGET))
                Timeline(notes, sing + 5000 + TAIL_MS, sing)
            }
            ExerciseKind.ECHO -> {
                notes.add(TimedNote(LEAD_IN_MS, 1200, root, Role.LISTEN))
                val sing = LEAD_IN_MS + 1200 + 700
                notes.add(TimedNote(sing, 2500, root, Role.TARGET))
                Timeline(notes, sing + 2500 + TAIL_MS, sing)
            }
            else -> {
                if (ex.harmonySteps != 0) {
                    addPhrase(LEAD_IN_MS.toFloat(), Role.LISTEN, Role.LISTEN)
                    val sing = LEAD_IN_MS + phraseMs + 2 * beatMs
                    addPhrase(sing, Role.ACCOMP, Role.TARGET)
                    Timeline(notes, (sing + phraseMs).roundToInt() + TAIL_MS, sing.roundToInt())
                } else {
                    addPhrase(LEAD_IN_MS.toFloat(), Role.LISTEN, null)
                    val sing = LEAD_IN_MS + phraseMs + 2 * beatMs
                    addPhrase(sing, Role.TARGET, null)
                    Timeline(notes, (sing + phraseMs).roundToInt() + TAIL_MS, sing.roundToInt())
                }
            }
        }
    }

    /** Lowest and highest semitone offsets an exercise reaches (including the second voice). */
    fun span(ex: Exercise): IntRange {
        var lo = ex.pattern.min()
        var hi = ex.pattern.max()
        if (ex.harmonySteps != 0) {
            // Diatonic thirds are at most 4 semitones away.
            if (ex.harmonySteps > 0) hi += 4 else lo -= 4
        }
        return lo..hi
    }
}

/** Sequence of roots for an exercise: up by [Exercise.rootStep] and back down, inside the voice range. */
object ExercisePlan {
    fun roots(ex: Exercise, low: Int, high: Int, maxReps: Int = 16, seed: Long = System.nanoTime()): IntArray {
        val span = ExerciseTimeline.span(ex)
        if (ex.kind == ExerciseKind.ECHO) {
            val rnd = java.util.Random(seed)
            val lo = low + 1
            val hi = max(lo, high - 1)
            var last = -1
            return IntArray(10) {
                var r: Int
                do {
                    r = lo + rnd.nextInt(hi - lo + 1)
                } while (r == last && hi > lo)
                last = r
                r
            }
        }
        val minRoot = low - span.first
        val maxRoot = high - span.last
        var first = minRoot + 2
        var top = maxRoot - 1
        if (first > top) {
            first = (minRoot + maxRoot) / 2
            top = first
        }
        val up = ArrayList<Int>()
        var r = first
        while (r <= top && up.size < maxReps / 2 + 1) {
            up.add(r)
            r += ex.rootStep
        }
        val down = up.dropLast(1).reversed()
        return (up + down).take(maxReps).toIntArray()
    }
}

/**
 * Scores sung pitch against the TARGET notes of a timeline.
 * A frame is a hit when it is within [toleranceCents] of the target (octave errors forgiven).
 */
class Scorer(private val targets: List<TimedNote>, private val toleranceCents: Float = 50f) {
    private val hits = IntArray(targets.size)
    private val totals = IntArray(targets.size)
    private var cursor = 0

    fun reset() {
        hits.fill(0)
        totals.fill(0)
        cursor = 0
    }

    /** Adds one analysis frame. [timeMs] is timeline time; [midi] is NaN when silent. */
    fun add(timeMs: Float, midi: Float) {
        if (targets.isEmpty()) return
        while (cursor > 0 && targets[cursor].startMs > timeMs) cursor--
        while (cursor < targets.size - 1 && targets[cursor].endMs < timeMs) cursor++
        val n = targets[cursor]
        val from = n.startMs + min(150, n.durMs / 4)
        val to = n.endMs - min(60, n.durMs / 8)
        if (timeMs < from || timeMs > to) return
        totals[cursor]++
        if (!midi.isNaN() && abs(Pitch.foldedCents(midi, n.midi)) <= toleranceCents) hits[cursor]++
    }

    /** Accuracy 0..1 of note [i], or -1 if it has not been evaluated yet. */
    fun noteAccuracy(i: Int): Float = if (totals[i] == 0) -1f else hits[i].toFloat() / totals[i]

    /** Average accuracy over the notes evaluated so far, or -1. */
    fun total(): Float {
        var sum = 0f
        var n = 0
        for (i in targets.indices) {
            if (totals[i] > 0) {
                sum += hits[i].toFloat() / totals[i]
                n++
            }
        }
        return if (n == 0) -1f else sum / n
    }

    /** Fraction of all evaluated frames that were hits (weights long notes more), or -1. */
    fun frameAccuracy(): Float {
        val t = totals.sum()
        return if (t == 0) -1f else hits.sum().toFloat() / t
    }

    val size: Int get() = targets.size
}

/**
 * Turns the smoothed pitch stream into what the live screen shows: the current note
 * with hysteresis (so it does not flicker between neighbours) and a list of recent notes.
 */
class NoteFollower(private val historySize: Int = 10) {
    /** Current displayed note, or -1. */
    var note = -1
        private set

    /** Deviation from [note] in cents. */
    var cents = 0f
        private set

    private var lastVoicedMs = 0L
    private var candidate = -1
    private var candidateSinceMs = 0L
    private var committed = -1
    private val recent = IntArray(historySize)
    private var recentCount = 0

    /** Incremented whenever the recent list changes. */
    var version = 0
        private set

    fun update(midi: Float, nowMs: Long) {
        if (midi.isNaN()) {
            if (note >= 0 && nowMs - lastVoicedMs > 250) {
                note = -1
                committed = -1
                candidate = -1
            }
            return
        }
        lastVoicedMs = nowMs
        val nearest = midi.roundToInt()
        if (note < 0 || (nearest != note && abs(midi - note) > 0.65f)) note = nearest
        cents = ((midi - note) * 100f).coerceIn(-50f, 50f)

        if (note != candidate) {
            candidate = note
            candidateSinceMs = nowMs
        } else if (note != committed && nowMs - candidateSinceMs >= 140) {
            committed = note
            push(note)
        }
    }

    private fun push(n: Int) {
        if (recentCount == historySize) {
            System.arraycopy(recent, 1, recent, 0, historySize - 1)
            recentCount--
        }
        recent[recentCount++] = n
        version++
    }

    fun recentNotes(): IntArray = recent.copyOf(recentCount)

    fun clear() {
        recentCount = 0
        note = -1
        committed = -1
        candidate = -1
        version++
    }
}

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

class TimedNote(val startMs: Int, val durMs: Int, val midi: Int, val role: Role, val gain: Float = 1f) {
    val endMs: Int get() = startMs + durMs
}

class Timeline(val notes: List<TimedNote>, val lengthMs: Int, val singStartMs: Int) {
    val targets: List<TimedNote> = notes.filter { it.role == Role.TARGET }
}

enum class ExerciseKind { PATTERN, SUSTAIN, ECHO, BREATH, BALANCE }

class Exercise(
    val id: String,
    val title: String,
    val category: String,
    val summary: String,
    val steps: List<String>,
    val kind: ExerciseKind = ExerciseKind.PATTERN,
    /** Semitone offsets from the root (major scale based); [REST] is a pause. */
    val pattern: IntArray = intArrayOf(0),
    /** Beats per note; defaults to one beat each. */
    val beats: FloatArray? = null,
    val bpm: Int = 90,
    /** Fraction of each beat that sounds (1 = legato, 0.4 = staccato). */
    val legato: Float = 0.92f,
    /** Second voice exercise: the singer sings this kind of second voice under the melody. */
    val harmony: HarmonyMode? = null,
    /** Backing vocal exercise: style, number of backing voices and the one the singer takes. */
    val backing: BackingStyle? = null,
    val backingVoices: Int = 1,
    val backingPart: Int = 0,
    /** Semitones the root moves between repetitions. */
    val rootStep: Int = 1,
    val minutes: Int = 3,
    val needsHeadphones: Boolean = false,
) {
    companion object {
        const val REST = Int.MIN_VALUE
    }
}

fun dbToGain(db: Float): Float = Math.pow(10.0, db / 20.0).toFloat()

object ExerciseTimeline {
    private const val LEAD_IN_MS = 400
    private const val TAIL_MS = 700

    private class Line(val notes: List<NoteEvent>, val gain: Float, val target: Boolean)

    /** Melody of a pattern exercise, starting at time 0. */
    fun melody(ex: Exercise, root: Int): List<NoteEvent> {
        val beatMs = 60000f / ex.bpm
        val out = ArrayList<NoteEvent>()
        var t = 0f
        for (i in ex.pattern.indices) {
            val b = ex.beats?.getOrNull(i) ?: 1f
            if (ex.pattern[i] != Exercise.REST) {
                out.add(NoteEvent(t.roundToInt(), (b * beatMs * ex.legato).roundToInt(), root + ex.pattern[i]))
            }
            t += b * beatMs
        }
        return out
    }

    /**
     * [range] is the singer's range, used by second voices and backing parts that follow the voice.
     * [lead] is the lead singer's voice type in backing exercises (defaults to the singer's own).
     */
    fun build(ex: Exercise, root: Int, range: IntRange = root - 5..root + 19, lead: VoiceType? = null): Timeline {
        val beatMs = 60000f / ex.bpm
        val key = Key(Math.floorMod(root, 12), false)
        val notes = ArrayList<TimedNote>()
        when (ex.kind) {
            ExerciseKind.SUSTAIN -> {
                notes.add(TimedNote(LEAD_IN_MS, 1500, root, Role.LISTEN))
                val sing = LEAD_IN_MS + 1500 + 900
                notes.add(TimedNote(sing, 5000, root, Role.TARGET))
                return Timeline(notes, sing + 5000 + TAIL_MS, sing)
            }
            ExerciseKind.ECHO -> {
                notes.add(TimedNote(LEAD_IN_MS, 1200, root, Role.LISTEN))
                val sing = LEAD_IN_MS + 1200 + 700
                notes.add(TimedNote(sing, 2500, root, Role.TARGET))
                return Timeline(notes, sing + 2500 + TAIL_MS, sing)
            }
            ExerciseKind.BREATH, ExerciseKind.BALANCE -> return Timeline(notes, 0, 0)
            ExerciseKind.PATTERN -> Unit
        }

        val melody = melody(ex, root)
        val lines = ArrayList<Line>()
        when {
            ex.harmony != null -> {
                lines.add(Line(melody, 1f, false))
                lines.add(Line(Harmony.voice(melody, key, ex.harmony, range = range), 1f, true))
            }
            ex.backing != null -> {
                val plan = Backing.arrange(melody, key, ex.backing, lead ?: VoiceType.classify(range.first, range.last), range, ex.backingVoices)
                lines.add(Line(plan.lead, 1f, false))
                val mine = ex.backingPart.coerceIn(0, (plan.parts.size - 1).coerceAtLeast(0))
                plan.parts.forEachIndexed { i, part -> lines.add(Line(part.notes, dbToGain(part.gainDb), i == mine)) }
            }
            else -> lines.add(Line(melody, 1f, true))
        }
        var beatsTotal = 0f
        for (i in ex.pattern.indices) beatsTotal += ex.beats?.getOrNull(i) ?: 1f
        val patternMs = beatsTotal * beatMs
        val contentMs = maxOf(patternMs, lines.maxOf { l -> l.notes.maxOfOrNull { it.endMs } ?: 0 }.toFloat())
        val sing = LEAD_IN_MS + contentMs + 2 * beatMs
        val singOnly = lines.size == 1
        for (line in lines) {
            for (n in line.notes) {
                notes.add(TimedNote(LEAD_IN_MS + n.startMs, n.durMs, n.midi, Role.LISTEN, line.gain))
                val role = if (line.target) Role.TARGET else Role.ACCOMP
                if (!singOnly || line.target) {
                    notes.add(TimedNote((sing + n.startMs).roundToInt(), n.durMs, n.midi, role, line.gain))
                }
            }
        }
        notes.sortBy { it.startMs }
        return Timeline(notes, (sing + contentMs).roundToInt() + TAIL_MS, sing.roundToInt())
    }

    /** Lowest and highest semitone offsets an exercise reaches (including parallel second voices). */
    fun span(ex: Exercise): IntRange {
        val offsets = ex.pattern.filter { it != Exercise.REST }
        var lo = offsets.minOrNull() ?: 0
        var hi = offsets.maxOrNull() ?: 0
        val h = ex.harmony
        if (h != null && h.parallel) {
            val shift = if (h.octave != 0) (if (h.steps > 0) -9 else 9) else (if (h.steps > 0) 4 else -4)
            if (shift > 0) hi += shift else lo += shift
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

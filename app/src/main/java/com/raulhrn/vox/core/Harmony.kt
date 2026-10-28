package com.raulhrn.vox.core

import kotlin.math.abs

/** The kinds of second voice the app can write and teach. */
enum class HarmonyMode(
    val label: String,
    val summary: String,
    /** 1 = first steps, 3 = advanced. */
    val level: Int,
    internal val steps: Int = 0,
    internal val octave: Int = 0,
) {
    THIRD_ABOVE("3ª acima", "Paralela, logo acima da melodia", 1, steps = 2),
    THIRD_BELOW("3ª abaixo", "Paralela, logo abaixo da melodia", 1, steps = -2),
    SIXTH_BELOW("6ª abaixo", "A 3ª acima levada uma oitava para baixo", 2, steps = 2, octave = -12),
    SIXTH_ABOVE("6ª acima", "A 3ª abaixo levada uma oitava para cima", 2, steps = -2, octave = 12),
    GUIDED("Conduzida", "Alterna 3ª e 6ª para andar pouco e caber na sua voz", 2),
    CONTRARY("Contrária", "Anda no sentido oposto ao da melodia", 3),
    PEDAL("Nota pedal", "Segura uma nota enquanto a melodia anda", 2),
    UNISON("Dobra", "A mesma melodia, em uníssono ou oitava", 1),
    ;

    /** Parallel voices move with the melody and accept a manual octave shift. */
    val parallel: Boolean get() = steps != 0
}

/**
 * Second-voice writer. Parallel types move every note by a diatonic third (or sixth);
 * the others choose, note by note, the best option with a small Viterbi search over
 * candidate pitches (voice-leading cost, consonance, contrary motion, vocal range).
 */
object Harmony {
    private const val NONE = Int.MIN_VALUE

    /**
     * Second voice for [melody]. [range] is the singer's range: the non-parallel types
     * stay inside it. [octaveShift] is applied to parallel types only.
     */
    fun voice(
        melody: List<NoteEvent>,
        key: Key,
        mode: HarmonyMode,
        octaveShift: Int = 0,
        range: IntRange? = null,
    ): List<NoteEvent> {
        if (melody.isEmpty()) return emptyList()
        return when (mode) {
            HarmonyMode.THIRD_ABOVE, HarmonyMode.THIRD_BELOW, HarmonyMode.SIXTH_BELOW, HarmonyMode.SIXTH_ABOVE ->
                melody.map { it.copy(midi = note(it.midi, key, mode.steps) + mode.octave + octaveShift) }
            HarmonyMode.GUIDED -> guided(melody, key, range ?: around(melody, 3), preferAbove = null)
            HarmonyMode.CONTRARY -> contrary(melody, key, range ?: around(melody, -2))
            HarmonyMode.PEDAL -> pedal(melody, key, range ?: around(melody, -5))
            HarmonyMode.UNISON -> fitOctave(melody, range)
        }
    }

    /** Moves [midi] by [steps] scale degrees within [key]; chromatic notes get a sensible third. */
    fun note(midi: Int, key: Key, steps: Int): Int {
        val scale = key.scale
        val rel = Math.floorMod(midi - key.tonic, 12)
        val degree = scale.indexOf(rel)
        if (degree >= 0) {
            val target = degree + steps
            val octaves = Math.floorDiv(target, 7)
            val idx = Math.floorMod(target, 7)
            return midi + scale[idx] + 12 * octaves - scale[degree]
        }
        // Raised notes (leading tones, secondary dominants) take a minor third above;
        // lowered notes (borrowed chords) take a major third above.
        val raised = if (key.minor) rel in MINOR_RAISED else rel in MAJOR_RAISED
        val third = when {
            steps > 0 -> if (raised) 3 else 4
            else -> if (raised) -4 else -3
        }
        val extraOctaves = if (steps > 0) (steps - 2) / 7 else (steps + 2) / 7
        return midi + third + 12 * extraOctaves
    }

    /**
     * Thirds/sixths that keep the voice smooth and inside [range]. [preferAbove] = true keeps
     * the line above the melody (false below, null free), used by backing vocals.
     */
    fun guided(melody: List<NoteEvent>, key: Key, range: IntRange, preferAbove: Boolean?): List<NoteEvent> {
        val center = (range.first + range.last) / 2f
        val path = viterbi(
            melody.size,
            candidates = { i ->
                val m = melody[i].midi
                val up = note(m, key, 2)
                val down = note(m, key, -2)
                intArrayOf(up, up - 12, up + 12, down, down + 12, down - 12)
            },
            cost = { i, prev, c ->
                val m = melody[i].midi
                var cost = rangeCost(c, range, center)
                if (preferAbove == true && c < m) cost += 4f
                if (preferAbove == false && c > m) cost += 4f
                if (prev != NONE) {
                    val leap = abs(c - prev)
                    val joined = melody[i].startMs - melody[i - 1].endMs < 600
                    cost += (if (joined) 0.45f else 0.15f) * leap + if (leap > 7 && joined) 3f else 0f
                    if ((c > m) != (prev > melody[i - 1].midi)) cost += 1.5f
                }
                cost
            },
        )
        return melody.mapIndexed { i, n -> n.copy(midi = path[i]) }
    }

    private fun contrary(melody: List<NoteEvent>, key: Key, range: IntRange): List<NoteEvent> {
        val center = (range.first + range.last) / 2f
        val path = viterbi(
            melody.size,
            candidates = { i ->
                val m = melody[i].midi
                val list = ArrayList<Int>()
                for (c in m - 16..m + 16) {
                    if (c == m || key.degreeOf(c) < 0) continue
                    if (intervalCost(abs(c - m)) < 9f) list.add(c)
                }
                // Chromatic melody notes still get their thirds.
                for (t in intArrayOf(note(m, key, 2), note(m, key, -2))) if (t !in list) list.add(t)
                list.toIntArray()
            },
            cost = { i, prev, c ->
                val m = melody[i].midi
                var cost = intervalCost(abs(c - m)) + rangeCost(c, range, center)
                if (prev != NONE) {
                    val pm = melody[i - 1].midi
                    val dm = m - pm
                    val dc = c - prev
                    cost += 0.3f * abs(dc) + if (abs(dc) > 5) 2f else 0f
                    if (dm != 0) {
                        cost += when {
                            dc == 0 -> -0.3f
                            (dc > 0) != (dm > 0) -> -1.4f
                            else -> 0.8f
                        }
                        val now = abs(c - m) % 12
                        val before = abs(prev - pm) % 12
                        if (dc != 0 && (now == 0 || now == 7) && now == before) cost += 5f
                    } else if (dc == 0) {
                        cost -= 0.2f
                    }
                }
                cost
            },
        )
        return melody.mapIndexed { i, n -> n.copy(midi = path[i]) }
    }

    /** Cost of a vertical interval for a two-voice texture (thirds and sixths are best). */
    private fun intervalCost(semitones: Int): Float = when (semitones) {
        3, 4, 8, 9, 15, 16 -> 0f
        7 -> 0.8f
        12 -> 1.5f
        else -> 10f
    }

    private fun pedal(melody: List<NoteEvent>, key: Key, range: IntRange): List<NoteEvent> {
        val center = (range.first + range.last) / 2
        val out = ArrayList<NoteEvent>(melody.size)
        var i = 0
        for (ph in Phrases.split(melody)) {
            val from = i
            while (i < melody.size && melody[i].startMs < ph.endMs) i++
            val notes = melody.subList(from, i)
            if (notes.isEmpty()) continue
            // Candidates: tonic, fifth and third of the key, in every octave of the range.
            var best = center
            var bestScore = -Float.MAX_VALUE
            for (degree in intArrayOf(0, 4, 2)) {
                val pc = key.tonic + key.scale[degree]
                for (p in range) {
                    if (Math.floorMod(p - pc, 12) != 0) continue
                    var score = 0f
                    for (n in notes) score += n.durMs * pedalConsonance(abs(n.midi - p) % 12)
                    score -= 3f * abs(p - center) + (if (degree == 0) 0f else 40f)
                    if (score > bestScore) {
                        bestScore = score
                        best = p
                    }
                }
            }
            for (n in notes) {
                val h = if (pedalConsonance(abs(n.midi - best) % 12) >= 0.6f) {
                    best
                } else {
                    // Clash: step out to the nearest third, then come back to the pedal.
                    val t = note(n.midi, key, if (best < n.midi) -2 else 2)
                    intArrayOf(t, t - 12, t + 12).minBy { abs(it - best) }
                }
                out.add(n.copy(midi = h))
            }
        }
        return merge(out, maxGapMs = 400)
    }

    private fun pedalConsonance(ic: Int): Float = when (ic) {
        0, 3, 4, 7 -> 1f
        8, 9 -> 0.9f
        5 -> 0.6f
        else -> 0f
    }

    /** Shifts the whole line by octaves so it sits in [range] (unchanged when null). */
    fun fitOctave(line: List<NoteEvent>, range: IntRange?): List<NoteEvent> {
        if (range == null || line.isEmpty()) return line
        val shift = octaveFor(line, range)
        return if (shift == 0) line else line.map { it.copy(midi = it.midi + shift) }
    }

    /** Octave shift (multiple of 12) that keeps most of [line] inside [range]. */
    fun octaveFor(line: List<NoteEvent>, range: IntRange): Int = (-3..3).map { it * 12 }.minBy { shift ->
        line.sumOf { n ->
            val m = n.midi + shift
            (if (m < range.first) range.first - m else if (m > range.last) m - range.last else 0) * n.durMs
        } * 10L + abs(shift)
    }

    /** Joins consecutive notes of equal pitch separated by at most [maxGapMs]. */
    fun merge(notes: List<NoteEvent>, maxGapMs: Int): List<NoteEvent> {
        if (notes.isEmpty()) return notes
        val out = ArrayList<NoteEvent>()
        var cur = notes[0]
        for (k in 1 until notes.size) {
            val n = notes[k]
            cur = if (n.midi == cur.midi && n.startMs - cur.endMs <= maxGapMs) {
                NoteEvent(cur.startMs, n.endMs - cur.startMs, cur.midi)
            } else {
                out.add(cur)
                n
            }
        }
        out.add(cur)
        return out
    }

    internal fun rangeCost(c: Int, range: IntRange, center: Float): Float {
        val outside = when {
            c < range.first -> range.first - c
            c > range.last -> c - range.last
            else -> 0
        }
        return 3f * outside + 0.04f * abs(c - center)
    }

    /** A plausible range around the melody, used when the singer's range is unknown. */
    private fun around(melody: List<NoteEvent>, offset: Int): IntRange {
        val sorted = melody.map { it.midi }.sorted()
        val median = sorted[sorted.size / 2] + offset
        return median - 9..median + 9
    }

    /** Minimum-cost path through per-note candidate pitches. [cost] gets NONE as prev for the first note. */
    internal fun viterbi(
        n: Int,
        candidates: (Int) -> IntArray,
        cost: (i: Int, prev: Int, c: Int) -> Float,
    ): IntArray {
        val cands = Array(n) { candidates(it) }
        val score = Array(n) { FloatArray(cands[it].size) }
        val back = Array(n) { IntArray(cands[it].size) }
        for (j in cands[0].indices) score[0][j] = cost(0, NONE, cands[0][j])
        for (i in 1 until n) {
            for (j in cands[i].indices) {
                var best = Float.MAX_VALUE
                var arg = 0
                for (k in cands[i - 1].indices) {
                    val s = score[i - 1][k] + cost(i, cands[i - 1][k], cands[i][j])
                    if (s < best) {
                        best = s
                        arg = k
                    }
                }
                score[i][j] = best
                back[i][j] = arg
            }
        }
        var j = score[n - 1].indices.minBy { score[n - 1][it] }
        val out = IntArray(n)
        for (i in n - 1 downTo 0) {
            out[i] = cands[i][j]
            j = back[i][j]
        }
        return out
    }

    /** Median pitch of a line, rounded. */
    fun median(line: List<NoteEvent>): Int {
        if (line.isEmpty()) return 60
        val s = line.map { it.midi }.sorted()
        return s[s.size / 2]
    }

    private val MAJOR_RAISED = setOf(1, 6)
    private val MINOR_RAISED = setOf(4, 6, 9, 11)
}

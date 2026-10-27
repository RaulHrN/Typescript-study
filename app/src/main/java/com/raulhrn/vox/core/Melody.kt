package com.raulhrn.vox.core

import kotlin.math.PI
import kotlin.math.abs
import kotlin.math.atan2
import kotlin.math.cos
import kotlin.math.roundToInt
import kotlin.math.sin
import kotlin.math.sqrt

data class NoteEvent(val startMs: Int, val durMs: Int, val midi: Int) {
    val endMs: Int get() = startMs + durMs
}

/** Growable primitive float array, avoids boxing when collecting pitch tracks. */
class FloatList(capacity: Int = 1024) {
    var data = FloatArray(capacity)
        private set
    var size = 0
        private set

    fun add(v: Float) {
        if (size == data.size) data = data.copyOf(size * 2)
        data[size++] = v
    }

    operator fun get(i: Int): Float = data[i]

    fun clear() {
        size = 0
    }

    fun toArray(): FloatArray = data.copyOf(size)
}

/** Turns a frame-by-frame pitch track (MIDI, NaN = unvoiced) into discrete notes. */
object Segmenter {
    fun segment(
        track: FloatArray,
        hopMs: Float,
        firstFrameMs: Float = 0f,
        minNoteMs: Int = 90,
        correctTuning: Boolean = true,
        onsets: BooleanArray? = null,
    ): List<NoteEvent> {
        val n = track.size
        if (n == 0) return emptyList()
        val f = track.copyOf()

        // Bridge tiny dropouts (up to 2 frames) inside a stable sound.
        var i = 1
        while (i < n) {
            if (f[i].isNaN() && !f[i - 1].isNaN()) {
                var j = i
                while (j < n && f[j].isNaN() && j - i < 3) j++
                val reattack = onsets != null && onsets[j.coerceAtMost(n - 1)]
                if (j < n && j - i <= 2 && !f[j].isNaN() && abs(f[j] - f[i - 1]) < 1f && !reattack) {
                    for (k in i until j) f[k] = f[i - 1] + (f[j] - f[i - 1]) * (k - i + 1) / (j - i + 1)
                }
                i = j
            } else {
                i++
            }
        }

        val smooth = medianFilter(f, 5)

        // Global tuning offset (circular mean of the deviation from equal temperament).
        var offset = 0f
        if (correctTuning) {
            var c = 0.0
            var s = 0.0
            for (v in smooth) {
                if (v.isNaN()) continue
                val a = 2.0 * PI * (v - v.roundToInt())
                c += cos(a); s += sin(a)
            }
            if (c != 0.0 || s != 0.0) {
                val o = (atan2(s, c) / (2.0 * PI)).toFloat()
                if (abs(o) > 0.08f) offset = o
            }
        }
        for (k in smooth.indices) if (!smooth[k].isNaN()) smooth[k] -= offset

        val notes = ArrayList<NoteEvent>()
        val attacked = ArrayList<Boolean>()
        val maxGap = 3
        val changeFrames = 3
        var segStart = -1
        var lastVoiced = -1
        var center = 0f
        var devStart = -1
        var lastOnset = -1
        var lastEnd = -1

        fun close(a: Int, b: Int) {
            if (a < 0 || b < a) return
            lastEnd = b
            val values = FloatList(b - a + 1)
            for (k in a..b) if (!smooth[k].isNaN()) values.add(smooth[k])
            if (values.size == 0) return
            val durMs = ((b - a + 1) * hopMs).roundToInt()
            if (durMs < minNoteMs) return
            val pitch = median(values.toArray()).roundToInt()
            notes.add(NoteEvent((firstFrameMs + a * hopMs).roundToInt(), durMs, pitch))
            attacked.add(onsets != null && onsets[a])
        }

        for (k in 0 until n) {
            val v = smooth[k]
            if (onsets != null && onsets[k]) lastOnset = k
            if (v.isNaN()) {
                if (segStart >= 0 && k - lastVoiced > maxGap) {
                    close(segStart, lastVoiced)
                    segStart = -1
                    devStart = -1
                }
                continue
            }
            if (segStart >= 0 && onsets != null && onsets[k] && k - segStart >= 3) {
                // Re-articulation (a consonant or breath) on the same or another pitch.
                close(segStart, lastVoiced)
                segStart = -1
            }
            if (segStart < 0) {
                // Start at the attack when the pitch only became clear a few frames later.
                segStart = if (lastOnset in (lastEnd + 1)..k && k - lastOnset <= 5) lastOnset else k
                center = v
                devStart = -1
                lastVoiced = k
                continue
            }
            // Refine the reference pitch while the note is young (onsets often glide).
            if (k - segStart in 1..5 && devStart < 0) {
                center = median(smooth.copyOfRange(segStart, k + 1).filterNot { it.isNaN() }.toFloatArray())
            }
            if (abs(v - center) > 0.75f) {
                if (devStart < 0) devStart = k
                if (k - devStart + 1 >= changeFrames) {
                    // A too-short first part is an onset glide: it belongs to the new note.
                    if ((devStart - segStart) * hopMs >= minNoteMs) {
                        close(segStart, devStart - 1)
                        segStart = devStart
                    }
                    center = median(smooth.copyOfRange(devStart, k + 1).filterNot { it.isNaN() }.toFloatArray())
                    devStart = -1
                }
            } else {
                devStart = -1
            }
            lastVoiced = k
        }
        if (segStart >= 0) close(segStart, lastVoiced)
        return mergeRepeats(notes, gapMs = (hopMs * 3).roundToInt(), attacked)
    }

    /**
     * Joins consecutive notes of the same pitch separated by a very short gap,
     * unless the second one was clearly re-attacked ([attacked]).
     */
    fun mergeRepeats(notes: List<NoteEvent>, gapMs: Int, attacked: List<Boolean>? = null): List<NoteEvent> {
        if (notes.isEmpty()) return notes
        val out = ArrayList<NoteEvent>(notes.size)
        var cur = notes[0]
        for (k in 1 until notes.size) {
            val nx = notes[k]
            val reattack = attacked?.getOrNull(k) == true
            cur = if (nx.midi == cur.midi && nx.startMs - cur.endMs <= gapMs && !reattack) {
                NoteEvent(cur.startMs, nx.endMs - cur.startMs, cur.midi)
            } else {
                out.add(cur)
                nx
            }
        }
        out.add(cur)
        return out
    }

    private fun medianFilter(src: FloatArray, width: Int): FloatArray {
        val half = width / 2
        val out = FloatArray(src.size)
        val tmp = FloatArray(width)
        for (k in src.indices) {
            if (src[k].isNaN()) {
                out[k] = Float.NaN
                continue
            }
            var c = 0
            for (j in k - half..k + half) {
                if (j < 0 || j >= src.size) continue
                val v = src[j]
                if (!v.isNaN()) tmp[c++] = v
            }
            out[k] = median(tmp, c)
        }
        return out
    }

    /**
     * Marks frames where the level jumps by at least [riseDb] over the previous few frames:
     * the start of a sung syllable. Used to keep repeated notes apart.
     */
    fun onsets(levelsDb: FloatArray, riseDb: Float = 8f, minDb: Float = -60f): BooleanArray {
        val out = BooleanArray(levelsDb.size)
        for (i in levelsDb.indices) {
            if (levelsDb[i] < minDb) continue
            var low = Float.MAX_VALUE
            for (j in maxOf(0, i - 4) until i) low = minOf(low, levelsDb[j])
            if (low == Float.MAX_VALUE) continue
            val recent = (i >= 1 && out[i - 1]) || (i >= 2 && out[i - 2])
            if (levelsDb[i] - low >= riseDb && !recent) out[i] = true
        }
        return out
    }

    internal fun median(values: FloatArray, count: Int = values.size): Float {
        if (count == 0) return Float.NaN
        val s = values.copyOf(count)
        s.sort()
        return if (count % 2 == 1) s[count / 2] else (s[count / 2 - 1] + s[count / 2]) / 2f
    }
}

/** Krumhansl-Schmuckler key finding, weighted by note duration. */
object KeyDetector {
    private val majorProfile = doubleArrayOf(6.35, 2.23, 3.48, 2.33, 4.38, 4.09, 2.52, 5.19, 2.39, 3.66, 2.29, 2.88)
    private val minorProfile = doubleArrayOf(6.33, 2.68, 3.52, 5.38, 2.60, 3.53, 2.54, 4.75, 3.98, 2.69, 3.34, 3.17)

    fun detect(notes: List<NoteEvent>): Key {
        val hist = DoubleArray(12)
        for (n in notes) hist[Pitch.pitchClass(n.midi)] += n.durMs.toDouble()
        if (hist.sum() == 0.0) return Key(0, false)
        var best = Key(0, false)
        var bestR = Double.NEGATIVE_INFINITY
        for (tonic in 0 until 12) {
            for (minor in booleanArrayOf(false, true)) {
                val profile = if (minor) minorProfile else majorProfile
                val r = correlation(hist) { pc -> profile[Math.floorMod(pc - tonic, 12)] }
                if (r > bestR) {
                    bestR = r
                    best = Key(tonic, minor)
                }
            }
        }
        return best
    }

    private inline fun correlation(x: DoubleArray, y: (Int) -> Double): Double {
        val mx = x.average()
        var my = 0.0
        for (i in 0 until 12) my += y(i)
        my /= 12.0
        var num = 0.0
        var dx = 0.0
        var dy = 0.0
        for (i in 0 until 12) {
            val a = x[i] - mx
            val b = y(i) - my
            num += a * b; dx += a * a; dy += b * b
        }
        return if (dx == 0.0 || dy == 0.0) 0.0 else num / sqrt(dx * dy)
    }
}

enum class HarmonyMode(val label: String, val steps: Int, val octave: Int) {
    THIRD_ABOVE("3ª acima", 2, 0),
    THIRD_BELOW("3ª abaixo", -2, 0),
    SIXTH_BELOW("6ª abaixo", 2, -12),
    SIXTH_ABOVE("6ª acima", -2, 12),
}

/**
 * Builds a second voice by moving each melody note a diatonic third (or sixth)
 * inside the key, the way duets are usually arranged in popular music.
 */
object Harmony {
    fun voice(melody: List<NoteEvent>, key: Key, mode: HarmonyMode, octaveShift: Int = 0): List<NoteEvent> =
        melody.map { NoteEvent(it.startMs, it.durMs, note(it.midi, key, mode.steps) + mode.octave + octaveShift) }

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

    private val MAJOR_RAISED = setOf(1, 6)
    private val MINOR_RAISED = setOf(4, 6, 9, 11)
}

data class Phrase(val startMs: Int, val endMs: Int)

object Phrases {
    /** Splits a melody at rests longer than [gapMs], keeping phrases shorter than [maxMs]. */
    fun split(notes: List<NoteEvent>, gapMs: Int = 650, maxMs: Int = 14000): List<Phrase> {
        if (notes.isEmpty()) return emptyList()
        val out = ArrayList<Phrase>()
        var start = notes[0].startMs
        var end = notes[0].endMs
        for (k in 1 until notes.size) {
            val n = notes[k]
            if (n.startMs - end > gapMs || n.endMs - start > maxMs) {
                out.add(Phrase(start, end))
                start = n.startMs
            }
            end = maxOf(end, n.endMs)
        }
        out.add(Phrase(start, end))
        return out
    }
}

/**
 * Parses a typed melody such as "Dó Ré Mi:2 - Fá#4 Sol/0.5" or "C D E:2 F#4".
 * Names: Dó/Do Ré/Re Mi Fá/Fa Sol Lá/La Si or C D E F G A B, with # or b.
 * Octave is optional (closest to the previous note). Duration in beats after ':' or '/'.
 * '-' or 'p' is a rest.
 */
object Notation {
    class Result(val notes: List<NoteEvent>, val error: String?)

    private val solfege = listOf("sol" to 7, "do" to 0, "re" to 2, "mi" to 4, "fa" to 5, "la" to 9, "si" to 11)
    private val letters = mapOf('c' to 0, 'd' to 2, 'e' to 4, 'f' to 5, 'g' to 7, 'a' to 9, 'b' to 11)

    fun parse(text: String, bpm: Int): Result {
        val beatMs = 60000f / bpm.coerceIn(30, 300)
        val tokens = text.split(' ', ',', ';', '\n', '\t', '|').filter { it.isNotBlank() }
        val notes = ArrayList<NoteEvent>()
        var t = 0f
        var prev = 60
        var lastBeats = 1f
        for (raw in tokens) {
            val parts = raw.split(':', '/')
            if (parts.size > 2) return Result(notes, "Não entendi \"$raw\"")
            val beats = if (parts.size == 2) {
                parts[1].replace(',', '.').toFloatOrNull()?.takeIf { it > 0f && it <= 64f }
                    ?: return Result(notes, "Duração inválida em \"$raw\"")
            } else {
                lastBeats
            }
            lastBeats = beats
            val head = normalize(parts[0])
            if (head == "-" || head == "p" || head == "pausa") {
                t += beats * beatMs
                continue
            }
            val midi = parseNote(head, prev) ?: return Result(notes, "Não entendi a nota \"$raw\"")
            notes.add(NoteEvent(t.roundToInt(), (beats * beatMs * 0.95f).roundToInt(), midi))
            prev = midi
            t += beats * beatMs
        }
        if (notes.isEmpty()) return Result(notes, "Digite pelo menos uma nota")
        return Result(notes, null)
    }

    private fun normalize(s: String): String = s.lowercase()
        .replace('ó', 'o').replace('é', 'e').replace('á', 'a').replace('♯', '#').replace('♭', 'b')

    private fun parseNote(s: String, prev: Int): Int? {
        var rest: String
        var pc: Int
        val sol = solfege.firstOrNull { s.startsWith(it.first) }
        if (sol != null) {
            pc = sol.second
            rest = s.substring(sol.first.length)
        } else {
            pc = letters[s.firstOrNull() ?: return null] ?: return null
            rest = s.substring(1)
        }
        while (rest.isNotEmpty() && (rest[0] == '#' || rest[0] == 'b')) {
            pc += if (rest[0] == '#') 1 else -1
            rest = rest.substring(1)
        }
        if (rest.isEmpty()) {
            // Pick the octave that lands closest to the previous note.
            val base = Math.floorMod(pc, 12)
            var best = base
            var bestDist = Int.MAX_VALUE
            for (oct in 0..9) {
                val m = base + 12 * oct
                val d = abs(m - prev)
                if (d < bestDist) {
                    bestDist = d
                    best = m
                }
            }
            return best.coerceIn(24, 96)
        }
        val octave = rest.toIntOrNull() ?: return null
        if (octave !in 0..8) return null
        return (pc + 12 * (octave + 1)).coerceIn(12, 120)
    }
}

package com.raulhrn.vox.core

import kotlin.math.abs
import kotlin.math.roundToInt

enum class BackingStyle(val label: String, val summary: String) {
    HARMONY("Harmonia", "Canta a letra junto com o regente, em terças"),
    PAD("Pad", "Notas longas do acorde em \"Uh\" ou \"Ah\""),
    ECHO("Resposta", "Repete o fim das frases, como um eco"),
    DOUBLE("Dobra", "Reforça a melodia em uníssono ou oitava"),
}

/** Where the backing singer's voice sits compared to the lead singer (regente). */
enum class Relation { HIGHER, LOWER, SAME }

class BackingPart(val name: String, val notes: List<NoteEvent>, val gainDb: Float, val where: String)

class BackingPlan(
    /** The melody placed in the lead singer's register. */
    val lead: List<NoteEvent>,
    val leadShift: Int,
    val parts: List<BackingPart>,
    val relation: Relation,
    val tips: List<String>,
)

/**
 * Writes backing vocal parts for a melody, taking into account the lead singer's voice
 * type (regente) and the backing singer's range, and suggests how loud each part should be.
 */
object Backing {
    fun relation(lead: VoiceType, singer: IntRange): Relation {
        val diff = (singer.first + singer.last) / 2 - lead.center
        return when {
            diff >= 3 -> Relation.HIGHER
            diff <= -3 -> Relation.LOWER
            else -> Relation.SAME
        }
    }

    /** Octave shift that puts the melody where a [lead] voice would sing it. */
    fun leadShift(melody: List<NoteEvent>, lead: VoiceType): Int {
        if (melody.isEmpty()) return 0
        return ((lead.center + 2 - Harmony.median(melody)) / 12f).roundToInt() * 12
    }

    fun arrange(
        melody: List<NoteEvent>,
        key: Key,
        style: BackingStyle,
        lead: VoiceType,
        singer: IntRange,
        voices: Int = 1,
    ): BackingPlan {
        val shift = leadShift(melody, lead)
        val leadLine = melody.map { it.copy(midi = it.midi + shift) }
        val rel = relation(lead, singer)
        val n = voices.coerceIn(1, 2)
        if (leadLine.isEmpty()) return BackingPlan(leadLine, shift, emptyList(), rel, emptyList())
        // In the same register, sing above a melody that sits low and below one that sits high.
        val above = when (rel) {
            Relation.HIGHER -> true
            Relation.LOWER -> false
            Relation.SAME -> Harmony.median(leadLine) <= lead.center
        }
        val raw = when (style) {
            BackingStyle.HARMONY -> harmonyParts(leadLine, key, above, singer, n)
            BackingStyle.PAD -> padParts(leadLine, key, singer, n)
            BackingStyle.ECHO -> echoParts(leadLine, key, above, singer, n)
            BackingStyle.DOUBLE -> doubleParts(leadLine, singer, n)
        }
        val leadMedian = Harmony.median(leadLine)
        val base = when (style) {
            BackingStyle.HARMONY -> if (n == 1) -6f else -8f
            BackingStyle.PAD -> -10f
            BackingStyle.ECHO -> -3f
            BackingStyle.DOUBLE -> -9f
        }
        val parts = raw.mapIndexed { i, (name, notes) ->
            val median = Harmony.median(notes)
            var gain = base
            // High voices cut through more easily, low voices can carry a little more body.
            if (median > leadMedian + 4) gain -= 2f else if (median < leadMedian - 4) gain += 1f
            if (n == 2 && i == 0) gain -= 1f
            val where = when {
                notes.isEmpty() -> ""
                median > leadMedian + 2 -> "acima do regente"
                median < leadMedian - 2 -> "abaixo do regente"
                else -> "na altura do regente"
            }
            BackingPart(name, notes, gain, where)
        }
        return BackingPlan(leadLine, shift, parts, rel, tips(style, rel, lead, singer, parts))
    }

    private fun harmonyParts(lead: List<NoteEvent>, key: Key, above: Boolean, singer: IntRange, n: Int): List<Pair<String, List<NoteEvent>>> {
        if (n == 1) {
            val line = Harmony.guided(lead, key, singer, preferAbove = above)
            return listOf((if (above) "Backing (3ª acima)" else "Backing (3ª abaixo)") to line)
        }
        // Close-harmony trio: the melody plus two chord tones stacked above or below it.
        val near = ArrayList<NoteEvent>(lead.size)
        val far = ArrayList<NoteEvent>(lead.size)
        for (note in lead) {
            val m = note.midi
            val deg = key.degreeOf(m)
            val first = Harmony.note(m, key, if (above) 2 else -2)
            val second = if (deg < 0) {
                Harmony.note(first, key, if (above) 2 else -2)
            } else {
                val rootDeg = if (above) deg else Math.floorMod(deg - 4, 7)
                val steps = if (diminished(rootDeg, key)) 5 else 4
                Harmony.note(m, key, if (above) steps else -steps)
            }
            near.add(note.copy(midi = first))
            far.add(note.copy(midi = second))
        }
        val shift = Harmony.octaveFor(near + far, singer)
        val a = near.map { it.copy(midi = it.midi + shift) }
        val b = far.map { it.copy(midi = it.midi + shift) }
        return if (above) {
            listOf("Backing 1 (agudo)" to b, "Backing 2 (grave)" to a)
        } else {
            listOf("Backing 1 (agudo)" to a, "Backing 2 (grave)" to b)
        }
    }

    private val CLASHES = setOf(1, 6, 11)

    private fun diminished(rootDegree: Int, key: Key) = if (key.minor) rootDegree == 1 else rootDegree == 6

    private fun padParts(lead: List<NoteEvent>, key: Key, singer: IntRange, n: Int): List<Pair<String, List<NoteEvent>>> {
        // Chunks of up to ~1.6 s inside a phrase, each harmonised with one diatonic triad.
        class Chunk(val start: Int, val end: Int, val leadMedian: Int, val tones: IntArray, val notes: List<NoteEvent>)
        val chunks = ArrayList<Chunk>()
        var i = 0
        while (i < lead.size) {
            var j = i
            while (j + 1 < lead.size && lead[j + 1].endMs - lead[i].startMs <= 1600 && lead[j + 1].startMs - lead[j].endMs < 700) j++
            val notes = lead.subList(i, j + 1)
            var bestRoot = 0
            var bestScore = -1f
            for (root in 0 until 7) {
                val degrees = intArrayOf(root, (root + 2) % 7, (root + 4) % 7)
                var cover = 0f
                for (nt in notes) {
                    val d = key.degreeOf(nt.midi)
                    // Chord tones count; a melody note on the chord's root counts a little more.
                    if (d in degrees) cover += nt.durMs * (if (d == root) 1.1f else 1f)
                }
                cover *= when (root) {
                    0, 3, 4 -> 1.15f
                    5 -> 1.05f
                    else -> if (diminished(root, key)) 0.7f else 1f
                }
                if (cover > bestScore) {
                    bestScore = cover
                    bestRoot = root
                }
            }
            val tones = intArrayOf(bestRoot, (bestRoot + 2) % 7, (bestRoot + 4) % 7).map { key.tonic + key.scale[it] }.toIntArray()
            chunks.add(Chunk(notes.first().startMs, notes.last().endMs, Harmony.median(notes), tones, notes))
            i = j + 1
        }
        val center = (singer.first + singer.last) / 2f
        fun line(below: IntArray?): IntArray = Harmony.viterbi(
            chunks.size,
            candidates = { k ->
                val list = ArrayList<Int>()
                for (p in singer.first - 2..singer.last + 2) {
                    if (chunks[k].tones.any { Math.floorMod(p - it, 12) == 0 }) list.add(p)
                }
                list.toIntArray()
            },
            cost = { k, prev, c ->
                val ch = chunks[k]
                var cost = Harmony.rangeCost(c, singer, center)
                if (abs(c - ch.leadMedian) < 1) cost += 2f
                // Avoid harsh clashes (semitones, sevenths, tritones) with passing melody notes.
                val total = (ch.end - ch.start).coerceAtLeast(1)
                for (nt in ch.notes) if (abs(c - nt.midi) % 12 in CLASHES) cost += 2.5f * nt.durMs / total
                // Prefer the third (colour), then the fifth, then the root.
                if (Math.floorMod(c - ch.tones[0], 12) == 0) cost += 1.2f
                if (Math.floorMod(c - ch.tones[2], 12) == 0) cost += 0.8f
                if (below != null && c > below[k] - 3) cost += 6f
                if (prev != Int.MIN_VALUE) cost += 0.25f * abs(c - prev)
                cost
            },
        )
        val first = line(null)
        fun notes(path: IntArray) = Harmony.merge(
            chunks.mapIndexed { k, ch -> NoteEvent(ch.start, (ch.end - ch.start - 80).coerceAtLeast(120), path[k]) },
            maxGapMs = 150,
        )
        val parts = mutableListOf("Pad (Uh)" to notes(first))
        if (n == 2) parts.add("Pad grave (Uh)" to notes(line(first)))
        if (n == 2) parts[0] = "Pad agudo (Uh)" to parts[0].second
        return parts
    }

    private fun echoParts(lead: List<NoteEvent>, key: Key, above: Boolean, singer: IntRange, n: Int): List<Pair<String, List<NoteEvent>>> {
        val phrases = Phrases.split(lead)
        val echo = ArrayList<NoteEvent>()
        for ((p, ph) in phrases.withIndex()) {
            val next = phrases.getOrNull(p + 1)?.startMs ?: (ph.endMs + 2400)
            val gap = next - ph.endMs
            if (gap < 700) continue
            val length = minOf(gap - 250, 1800)
            val tail = lead.filter { it.startMs >= ph.startMs && it.startMs < ph.endMs && it.startMs >= ph.endMs - length }
                .ifEmpty { listOfNotNull(lead.lastOrNull { it.startMs < ph.endMs }) }
            if (tail.isEmpty()) continue
            val offset = ph.endMs + 150 - tail.first().startMs
            for (t in tail) {
                val start = t.startMs + offset
                val dur = minOf(t.durMs, next - 100 - start)
                if (dur >= 80) echo.add(NoteEvent(start, dur, t.midi))
            }
        }
        if (echo.isEmpty()) return emptyList()
        val unison = Harmony.fitOctave(echo, singer)
        if (n == 1) return listOf("Resposta" to unison)
        return listOf("Resposta" to unison, "Resposta (3ª)" to Harmony.guided(unison, key, singer, preferAbove = above))
    }

    private fun doubleParts(lead: List<NoteEvent>, singer: IntRange, n: Int): List<Pair<String, List<NoteEvent>>> {
        val unison = Harmony.fitOctave(lead, singer)
        if (n == 1) return listOf("Dobra" to unison)
        val up = Harmony.median(unison) + 12 <= singer.last + 2
        val octave = unison.map { it.copy(midi = it.midi + if (up) 12 else -12) }
        return if (up) listOf("Dobra (8ª acima)" to octave, "Dobra" to unison) else listOf("Dobra" to unison, "Dobra (8ª abaixo)" to octave)
    }

    private fun tips(style: BackingStyle, rel: Relation, lead: VoiceType, singer: IntRange, parts: List<BackingPart>): List<String> {
        val tips = ArrayList<String>()
        val singerType = VoiceType.classify(singer.first, singer.last)
        val gain = parts.firstOrNull()?.gainDb?.roundToInt() ?: -6
        tips += "Fique cerca de ${-gain} dB abaixo do regente: a melodia vem na frente, o backing é a moldura."
        tips += when (rel) {
            Relation.HIGHER ->
                "Sua voz (${singerType.label}) é mais aguda que a do regente (${lead.label}). Agudos se destacam: use voz leve, pouco vibrato e segure o volume."
            Relation.LOWER ->
                "Sua voz (${singerType.label}) é mais grave que a do regente (${lead.label}). Você é a base: pode usar um pouco mais de corpo, sem cobrir a melodia."
            Relation.SAME ->
                "Você (${singerType.label}) e o regente (${lead.label}) estão na mesma região. Mantenha a sua linha firme para não escorregar para a melodia dele."
        }
        if (lead.female != singerType.female) {
            tips += "Vozes masculina e feminina soam uma oitava deslocadas: a terça vira décima. Fica aberto e bonito, desde que o backing esteja mais baixo."
        }
        tips += when (style) {
            BackingStyle.HARMONY -> "Pronuncie igual ao regente: mesmas vogais, mesmas respirações e finais cortados juntos."
            BackingStyle.PAD -> "Use \"Uh\" para um som macio ou \"Ah\" para mais brilho. Entre e saia sem ataque, como um órgão."
            BackingStyle.ECHO -> "A resposta pode vir quase no volume do regente, mas só depois que ele termina a frase."
            BackingStyle.DOUBLE -> "Na dobra ninguém deve perceber duas vozes: copie o timbre, tire o vibrato e cante um pouco atrás."
        }
        if (parts.size == 2) tips += "Com duas vozes de backing, a mais aguda canta 1 dB mais suave para não chamar atenção."
        tips += "No palco, fique um pouco mais longe do microfone que o regente e acompanhe a dinâmica dele: ele cresce, você cresce junto."
        return tips
    }
}

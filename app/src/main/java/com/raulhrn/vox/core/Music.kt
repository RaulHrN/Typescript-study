package com.raulhrn.vox.core

import kotlin.math.abs
import kotlin.math.ln
import kotlin.math.pow

object Pitch {
    private val LN2 = ln(2.0)

    fun hzToMidi(hz: Float, a4: Float = 440f): Float = (69.0 + 12.0 * ln(hz.toDouble() / a4) / LN2).toFloat()

    fun midiToHz(midi: Float, a4: Float = 440f): Float = (a4 * 2.0.pow((midi - 69.0) / 12.0)).toFloat()

    fun pitchClass(midi: Int): Int = Math.floorMod(midi, 12)

    /** Distance in cents between [midi] and [target], folded to the nearest octave (-600..600]. */
    fun foldedCents(midi: Float, target: Int): Float {
        var d = (midi - target) * 100f
        d %= 1200f
        if (d > 600f) d -= 1200f
        if (d <= -600f) d += 1200f
        return d
    }
}

enum class Naming { SOLFEGE, LETTERS }

object NoteNames {
    private val solfegeSharp = arrayOf("Dó", "Dó#", "Ré", "Ré#", "Mi", "Fá", "Fá#", "Sol", "Sol#", "Lá", "Lá#", "Si")
    private val solfegeFlat = arrayOf("Dó", "Réb", "Ré", "Mib", "Mi", "Fá", "Solb", "Sol", "Láb", "Lá", "Sib", "Si")
    private val letterSharp = arrayOf("C", "C#", "D", "D#", "E", "F", "F#", "G", "G#", "A", "A#", "B")
    private val letterFlat = arrayOf("C", "Db", "D", "Eb", "E", "F", "Gb", "G", "Ab", "A", "Bb", "B")
    private val tables = arrayOf(solfegeSharp, solfegeFlat, letterSharp, letterFlat)

    // Full names ("Lá4") cached for every MIDI note so the UI never builds strings per frame.
    private val full = Array(4) { t -> Array(128) { m -> tables[t][m % 12] + (m / 12 - 1) } }

    private fun table(naming: Naming, flats: Boolean) = (if (naming == Naming.SOLFEGE) 0 else 2) + (if (flats) 1 else 0)

    fun pitchClass(pc: Int, naming: Naming, flats: Boolean = false): String =
        tables[table(naming, flats)][Math.floorMod(pc, 12)]

    fun name(midi: Int, naming: Naming, flats: Boolean = false): String = pitchClass(midi, naming, flats)

    fun octave(midi: Int): Int = Math.floorDiv(midi, 12) - 1

    fun full(midi: Int, naming: Naming, flats: Boolean = false): String {
        if (midi !in 0..127) return pitchClass(midi, naming, flats) + octave(midi)
        return full[table(naming, flats)][midi]
    }
}

/** A tonality: tonic pitch class (0 = C) and mode. Minor uses the natural minor scale. */
data class Key(val tonic: Int, val minor: Boolean) {
    val scale: IntArray get() = if (minor) MINOR else MAJOR

    val prefersFlats: Boolean
        get() = if (minor) tonic in MINOR_FLATS else tonic in MAJOR_FLATS

    /** Scale degree (0..6) of a pitch class, or -1 if it is chromatic in this key. */
    fun degreeOf(pc: Int): Int = scale.indexOf(Math.floorMod(pc - tonic, 12))

    fun label(naming: Naming): String =
        NoteNames.pitchClass(tonic, naming, prefersFlats) + if (minor) " menor" else " maior"

    companion object {
        val MAJOR = intArrayOf(0, 2, 4, 5, 7, 9, 11)
        val MINOR = intArrayOf(0, 2, 3, 5, 7, 8, 10)
        private val MAJOR_FLATS = setOf(5, 10, 3, 8, 1)
        private val MINOR_FLATS = setOf(2, 7, 0, 5, 10, 3)

        /** All 24 keys, majors first, ordered by tonic. */
        val ALL: List<Key> = (0..11).map { Key(it, false) } + (0..11).map { Key(it, true) }
    }
}

enum class VoiceType(val label: String, val low: Int, val high: Int) {
    BASS("Baixo", 40, 64),
    BARITONE("Barítono", 45, 69),
    TENOR("Tenor", 48, 72),
    CONTRALTO("Contralto", 53, 77),
    MEZZO("Mezzo-soprano", 57, 81),
    SOPRANO("Soprano", 60, 84);

    val center: Int get() = (low + high) / 2

    /** Typical comfortable range for parts written for this voice. */
    val range: IntRange get() = low + 2..high - 2

    val female: Boolean get() = this == CONTRALTO || this == MEZZO || this == SOPRANO

    companion object {
        /** Picks the type whose typical range is closest to the measured one. */
        fun classify(low: Int, high: Int): VoiceType = entries.minBy {
            abs(it.low - low) + abs(it.high - high) + 0.5f * abs((it.low + it.high) - (low + high))
        }
    }
}

package com.raulhrn.vox.core

import kotlin.math.PI
import kotlin.math.abs
import kotlin.math.cos
import kotlin.math.exp
import kotlin.math.ln
import kotlin.math.log10
import kotlin.math.max
import kotlin.math.min
import kotlin.math.pow
import kotlin.math.roundToInt
import kotlin.math.sin
import kotlin.math.sqrt

/**
 * Finds the sung melody inside a music mix, telling the voice apart from the instruments.
 *
 * 1. Stereo STFT. Lead vocals are almost always mixed to the centre, so each frequency bin is
 *    weighted by how similar the left and right channels are there, and by a vocal band filter.
 * 2. Harmonic summation turns spectral peaks into a pitch salience function (10-cent bins).
 * 3. Salience peaks are linked into pitch contours.
 * 4. Contours are described (salience, pitch, vibrato, pitch fluctuation). Singing has vibrato
 *    and small natural pitch movements; most instruments hold a flat pitch. Weak, non-vocal
 *    contours and octave duplicates are dropped, and the melody is the best remaining contour
 *    in each frame.
 *
 * Streaming: memory grows only with a few numbers per frame, never with the audio itself.
 */
class MelodyExtractor(val sampleRate: Float, private val a4: Float = 440f) {
    class Result(
        /** MIDI pitch per frame, NaN where nobody sings. */
        val track: FloatArray,
        /** Level of each frame in dB (vocal-weighted). */
        val levels: FloatArray,
        val hopMs: Float,
        val firstFrameMs: Float,
        /** Fraction of frames with a voice. */
        val voicedRatio: Float,
    )

    private val fft = Fft(FFT)
    private val re = FloatArray(FFT)
    private val im = FloatArray(FFT)
    private val window = FloatArray(WINDOW) { (0.5 - 0.5 * cos(2.0 * PI * it / (WINDOW - 1))).toFloat() }
    private val bufL = FloatArray(WINDOW)
    private val bufR = FloatArray(WINDOW)
    private var fill = 0
    private val bins = FFT / 2
    private val mag = FloatArray(bins + 1)
    private val bandWeight = FloatArray(bins + 1) { k -> bandWeight(k * sampleRate / FFT) }
    private val salience = FloatArray(SAL_BINS)
    private val peakFreq = FloatArray(MAX_PEAKS)
    private val peakAmp = FloatArray(MAX_PEAKS)

    // Per frame: up to PEAKS_PER_FRAME (pitch, salience) pairs.
    private val framePitch = FloatList(8192)
    private val frameSal = FloatList(8192)
    private val levels = FloatList(2048)
    private var frames = 0

    val hopMs: Float get() = HOP * 1000f / sampleRate

    fun push(left: Float, right: Float) {
        bufL[fill] = left
        bufR[fill] = right
        fill++
        if (fill == WINDOW) {
            analyse()
            System.arraycopy(bufL, HOP, bufL, 0, WINDOW - HOP)
            System.arraycopy(bufR, HOP, bufR, 0, WINDOW - HOP)
            fill = WINDOW - HOP
        }
    }

    private fun analyse() {
        // Left in the real part, right in the imaginary part: one FFT gives both spectra.
        for (i in 0 until WINDOW) {
            re[i] = bufL[i] * window[i]
            im[i] = bufR[i] * window[i]
        }
        re.fill(0f, WINDOW, FFT)
        im.fill(0f, WINDOW, FFT)
        fft.forward(re, im)

        var energy = 0.0
        var maxMag = 0f
        mag[0] = 0f
        mag[bins] = 0f
        for (k in 1 until bins) {
            val lr = (re[k] + re[FFT - k]) * 0.5f
            val li = (im[k] - im[FFT - k]) * 0.5f
            val rr = (im[k] + im[FFT - k]) * 0.5f
            val ri = (re[FFT - k] - re[k]) * 0.5f
            val pl = lr * lr + li * li
            val pr = rr * rr + ri * ri
            val similarity = 2f * (lr * rr + li * ri) / (pl + pr + 1e-12f)
            val centre = max(0f, similarity)
            val mr = lr + rr
            val mi = li + ri
            val m = sqrt(mr * mr + mi * mi) * 0.5f * (0.25f + 0.75f * centre * centre) * bandWeight[k]
            mag[k] = m
            energy += (m * m).toDouble()
            if (m > maxMag) maxMag = m
        }
        val db = if (energy > 1e-14) (10.0 * log10(energy / WINDOW)).toFloat() else -140f
        levels.add(db)

        val start = framePitch.size
        if (db > -75f && maxMag > 0f) {
            val count = peaks(maxMag)
            salienceFrom(count)
            pickSaliencePeaks(count)
        }
        // Pad the frame to a fixed number of slots.
        while (framePitch.size < start + PEAKS_PER_FRAME) {
            framePitch.add(Float.NaN)
            frameSal.add(0f)
        }
        frames++
    }

    /** Spectral peaks above -40 dB of the frame maximum, with parabolic interpolation. */
    private fun peaks(maxMag: Float): Int {
        val floor = maxMag * 0.01f
        val kMin = max(2, (50f * FFT / sampleRate).toInt())
        val kMax = min(bins - 2, (5000f * FFT / sampleRate).toInt())
        var count = 0
        var k = kMin
        while (k <= kMax) {
            val m = mag[k]
            if (m > floor && m > mag[k - 1] && m >= mag[k + 1]) {
                val a = ln(mag[k - 1] + 1e-12f)
                val b = ln(m)
                val c = ln(mag[k + 1] + 1e-12f)
                val denom = a - 2f * b + c
                val delta = if (denom < 0f) (0.5f * (a - c) / denom).coerceIn(-0.5f, 0.5f) else 0f
                val amp = exp(b - 0.25f * (a - c) * delta)
                if (count < MAX_PEAKS) {
                    peakFreq[count] = (k + delta) * sampleRate / FFT
                    peakAmp[count] = amp
                    count++
                } else {
                    // Replace the weakest stored peak.
                    var weakest = 0
                    for (j in 1 until MAX_PEAKS) if (peakAmp[j] < peakAmp[weakest]) weakest = j
                    if (amp > peakAmp[weakest]) {
                        peakFreq[weakest] = (k + delta) * sampleRate / FFT
                        peakAmp[weakest] = amp
                    }
                }
            }
            k++
        }
        return count
    }

    /** Harmonic summation: each peak votes for the fundamentals it could be a harmonic of. */
    private fun salienceFrom(count: Int) {
        salience.fill(0f)
        for (p in 0 until count) {
            val f = peakFreq[p]
            val a = peakAmp[p]
            var weight = 1f
            for (h in 1..HARMONICS) {
                val f0 = f / h
                if (f0 < MIN_HZ) break
                val b = (Pitch.hzToMidi(f0, a4) - MIN_MIDI) * 10f
                if (b < SAL_BINS + 10) {
                    val centre = b.roundToInt()
                    for (d in -10..10) {
                        val bin = centre + d
                        if (bin < 0 || bin >= SAL_BINS) continue
                        val dist = abs(bin - b) / 10f
                        if (dist >= 1f) continue
                        val g = cos(PI.toFloat() / 2f * dist)
                        salience[bin] += a * weight * g * g
                    }
                }
                weight *= ALPHA
            }
        }
    }

    private fun pickSaliencePeaks(peakCount: Int) {
        var best = 0f
        for (v in salience) if (v > best) best = v
        if (best <= 0f) return
        // Keep the strongest local maxima above 25% of the frame maximum.
        val slots = FloatArray(PEAKS_PER_FRAME) { Float.NaN }
        val vals = FloatArray(PEAKS_PER_FRAME)
        for (b in 1 until SAL_BINS - 1) {
            val v = salience[b]
            if (v < 0.25f * best || v <= salience[b - 1] || v < salience[b + 1]) continue
            var weakest = 0
            for (j in 1 until PEAKS_PER_FRAME) if (vals[j] < vals[weakest]) weakest = j
            if (v > vals[weakest]) {
                val a = salience[b - 1]
                val c = salience[b + 1]
                val denom = a - 2f * v + c
                val delta = if (denom < 0f) (0.5f * (a - c) / denom).coerceIn(-0.5f, 0.5f) else 0f
                slots[weakest] = MIN_MIDI + (b + delta) / 10f
                vals[weakest] = v
            }
        }
        for (j in 0 until PEAKS_PER_FRAME) {
            framePitch.add(if (slots[j].isNaN()) Float.NaN else refine(slots[j], peakCount))
            frameSal.add(vals[j])
        }
    }

    /**
     * The salience grid is coarse and is pulled around by other sources, so the pitch is
     * re-measured from the frequencies of the partials actually present (f = h * f0).
     */
    private fun refine(midi: Float, peakCount: Int): Float {
        val f0 = Pitch.midiToHz(midi, a4)
        var num = 0f
        var den = 0f
        for (h in 1..6) {
            val target = f0 * h
            var bestA = 0f
            var bestF = 0f
            for (p in 0 until peakCount) {
                val f = peakFreq[p]
                if (f > target * 0.977f && f < target * 1.0235f && peakAmp[p] > bestA) {
                    bestA = peakAmp[p]
                    bestF = f
                }
            }
            if (bestA > 0f) {
                num += bestA * bestF / h
                den += bestA
            }
        }
        return if (den > 0f) Pitch.hzToMidi(num / den, a4) else midi
    }

    private class Contour(val start: Int) {
        val pitch = FloatList(64)
        val sal = FloatList(64)
        var last = start
        var lastPitch = 0f
        var meanSal = 0f
        var totalSal = 0f
        var meanPitch = 0f
        var vocal = 0f
        var dev = FloatArray(0)
        var removed = false
        val end: Int get() = start + pitch.size - 1

        fun pitchAt(frame: Int): Float {
            val i = frame - start
            return if (i < 0 || i >= pitch.size) Float.NaN else pitch[i]
        }
    }

    fun finish(): Result {
        val contours = track()
        describe(contours)
        select(contours)
        val melody = FloatArray(frames) { Float.NaN }
        val score = FloatArray(frames)
        for (c in contours) {
            if (c.removed) continue
            val s = c.meanSal * (1f + 0.8f * c.vocal)
            for (f in c.start..c.end) {
                val p = c.pitchAt(f)
                if (!p.isNaN() && s > score[f]) {
                    score[f] = s
                    melody[f] = p
                }
            }
        }
        val voiced = melody.count { !it.isNaN() }
        return Result(melody, levels.toArray(), hopMs, WINDOW * 500f / sampleRate, if (frames == 0) 0f else voiced.toFloat() / frames)
    }

    /**
     * Links salience peaks into continuous pitch contours. Only strong peaks (close to the
     * frame maximum) start a contour; it then grows forwards and backwards through any peak
     * that continues its pitch. This keeps the weak sub-harmonic "ghosts" of a voice from
     * becoming contours of their own.
     */
    private fun track(): List<Contour> {
        val p = PEAKS_PER_FRAME
        val total = frames * p
        val assigned = BooleanArray(total)
        val frameMax = FloatArray(frames)
        for (i in 0 until total) if (frameSal[i] > frameMax[i / p]) frameMax[i / p] = frameSal[i]
        val seeds = (0 until total)
            .filter { !framePitch[it].isNaN() && frameSal[it] >= SEED_RATIO * frameMax[it / p] }
            .sortedByDescending { frameSal[it] }

        fun follow(from: Int, pitch0: Float, step: Int, out: FloatList, sal: FloatList) {
            var pitch = pitch0
            var f = from
            var gap = 0
            while (true) {
                f += step
                if (f < 0 || f >= frames) break
                var best = -1
                var bestD = MAX_STEP
                for (j in 0 until p) {
                    val i = f * p + j
                    if (assigned[i] || framePitch[i].isNaN()) continue
                    val d = abs(framePitch[i] - pitch)
                    if (d < bestD) {
                        bestD = d
                        best = i
                    }
                }
                if (best < 0) {
                    if (++gap > MAX_GAP) break
                    out.add(Float.NaN)
                    sal.add(0f)
                    continue
                }
                gap = 0
                assigned[best] = true
                pitch = framePitch[best]
                out.add(pitch)
                sal.add(frameSal[best])
            }
            // Drop the unvoiced tail left by the gap allowance.
            while (out.size > 0 && out[out.size - 1].isNaN()) out.removeLast().also { sal.removeLast() }
        }

        val contours = ArrayList<Contour>()
        val fwd = FloatList(256)
        val fwdSal = FloatList(256)
        val back = FloatList(256)
        val backSal = FloatList(256)
        for (seed in seeds) {
            if (assigned[seed]) continue
            assigned[seed] = true
            val f0 = seed / p
            fwd.clear(); fwdSal.clear(); back.clear(); backSal.clear()
            follow(f0, framePitch[seed], 1, fwd, fwdSal)
            follow(f0, framePitch[seed], -1, back, backSal)
            val c = Contour(f0 - back.size)
            for (k in back.size - 1 downTo 0) {
                c.pitch.add(back[k])
                c.sal.add(backSal[k])
            }
            c.pitch.add(framePitch[seed])
            c.sal.add(frameSal[seed])
            for (k in 0 until fwd.size) {
                c.pitch.add(fwd[k])
                c.sal.add(fwdSal[k])
            }
            if ((0 until c.pitch.size).count { !c.pitch[it].isNaN() } >= MIN_LENGTH) contours.add(c)
        }
        return contours
    }

    /** Salience statistics and how voice-like each contour is (vibrato, natural pitch movement). */
    private fun describe(contours: List<Contour>) {
        for (c in contours) {
            var n = 0
            var sum = 0f
            var pitchSum = 0f
            for (i in 0 until c.pitch.size) {
                if (c.pitch[i].isNaN()) continue
                n++
                sum += c.sal[i]
                pitchSum += c.pitch[i]
            }
            c.totalSal = sum
            c.meanSal = sum / n
            c.meanPitch = pitchSum / n

            // Detrend with a ~200 ms moving average and look at what is left.
            val len = c.pitch.size
            val dev = FloatArray(len) { Float.NaN }
            for (i in 0 until len) {
                if (c.pitch[i].isNaN()) continue
                var s = 0f
                var k = 0
                for (j in max(0, i - 4)..min(len - 1, i + 4)) {
                    if (!c.pitch[j].isNaN()) {
                        s += c.pitch[j]
                        k++
                    }
                }
                dev[i] = (c.pitch[i] - s / k) * 100f
            }
            var sq = 0f
            var m = 0
            for (v in dev) {
                if (v.isNaN()) continue
                sq += v * v
                m++
            }
            val wobble = if (m > 0) sqrt(sq / m) else 0f
            // Vibrato is a regular oscillation: look for a clear periodogram peak at 3.5-8.5 Hz.
            // Interference from other instruments also moves the pitch, but irregularly.
            var purity = 0f
            if (len >= 12 && sq > 0f) {
                var r = 3.5f
                while (r <= 8.5f) {
                    var xc = 0.0
                    var xs = 0.0
                    for (i in 0 until len) {
                        val v = dev[i]
                        if (v.isNaN()) continue
                        val ph = 2.0 * PI * r * i * hopMs / 1000.0
                        xc += v * cos(ph)
                        xs += v * sin(ph)
                    }
                    val p = (2.0 * (xc * xc + xs * xs) / (m.toDouble() * sq)).toFloat()
                    if (p > purity) purity = p
                    r += 0.25f
                }
            }
            val vibrato = purity >= 0.45f && wobble in 7f..90f
            // Plain flat lines (keys, guitars, pads) score 0; lively voice-like lines up to 2.
            c.vocal = (if (vibrato) 1f else 0f) + ((wobble - 3f) / 12f).coerceIn(0f, if (vibrato) 1f else 0.7f)
            c.dev = dev
        }
    }

    /** Voicing detection and octave-error removal, then leaves the melody contours. */
    private fun select(contours: List<Contour>) {
        for (c in contours) {
            // Outside the singing range, or a flat instrument-like line: not the voice.
            if (c.meanPitch < 42f || c.meanPitch > 86f || c.vocal < 0.2f) c.removed = true
        }
        val alive = contours.filter { !it.removed }
        if (alive.isEmpty()) return
        val mean = alive.map { it.meanSal }.average().toFloat()
        val std = sqrt(alive.map { (it.meanSal - mean).pow(2) }.average().toFloat())
        for (c in alive) {
            val threshold = if (c.vocal >= 1f) mean - 0.9f * std else mean - 0.3f * std
            if (c.meanSal < threshold) c.removed = true
        }
        // Harmonic ghosts: a voice also creates weaker contours an octave, an octave and a
        // fifth, or two octaves away, moving in sync with it. Keep the strongest of the family.
        val kept = contours.filter { !it.removed }
        for (a in kept) for (b in kept) {
            if (a === b || a.removed || b.removed) continue
            if (!overlaps(a, b)) continue
            val diff = abs(a.meanPitch - b.meanPitch)
            if (GHOSTS.none { abs(diff - it) < 0.6f }) continue
            // A ghost moves exactly with its source; independent lines are left alone.
            if (correlation(a, b) < 0.5f) continue
            if (a.meanSal < b.meanSal / 1.1f) a.removed = true else if (b.meanSal < a.meanSal / 1.1f) b.removed = true
        }
        repeat(2) {
            val trend = melodyTrend(contours)
            val left = contours.filter { !it.removed }
            // Remaining octave doubts: keep the contour closer to where the melody is.
            for (a in left) for (b in left) {
                if (a === b || a.removed || b.removed) continue
                if (!overlaps(a, b) || abs(abs(a.meanPitch - b.meanPitch) - 12f) > 1.5f) continue
                if (distanceToTrend(a, trend) > distanceToTrend(b, trend)) a.removed = true else b.removed = true
            }
            for (c in left) if (!c.removed && distanceToTrend(c, trend) > 8f) c.removed = true
        }
    }

    /** Pearson correlation of the pitch movements of two contours where they overlap. */
    private fun correlation(a: Contour, b: Contour): Float {
        var n = 0
        var sa = 0.0
        var sb = 0.0
        var saa = 0.0
        var sbb = 0.0
        var sab = 0.0
        for (f in max(a.start, b.start)..min(a.end, b.end)) {
            val x = a.dev.getOrNull(f - a.start) ?: continue
            val y = b.dev.getOrNull(f - b.start) ?: continue
            if (x.isNaN() || y.isNaN()) continue
            n++
            sa += x; sb += y; saa += x * x; sbb += y * y; sab += x * y
        }
        if (n < 5) return 0f
        val cov = sab - sa * sb / n
        val va = saa - sa * sa / n
        val vb = sbb - sb * sb / n
        return if (va <= 1e-9 || vb <= 1e-9) 0f else (cov / sqrt(va * vb)).toFloat()
    }

    private fun overlaps(a: Contour, b: Contour): Boolean {
        val overlap = min(a.end, b.end) - max(a.start, b.start)
        return overlap >= 0.5f * min(a.end - a.start, b.end - b.start)
    }

    /** Salience-weighted mean melody pitch, smoothed over ~4 s. */
    private fun melodyTrend(contours: List<Contour>): FloatArray {
        val sum = FloatArray(frames)
        val weight = FloatArray(frames)
        for (c in contours) {
            if (c.removed) continue
            val w = c.meanSal * (1f + c.vocal) * (1f + c.vocal)
            for (f in c.start..c.end) {
                val p = c.pitchAt(f)
                if (p.isNaN()) continue
                sum[f] += p * w
                weight[f] += w
            }
        }
        val raw = FloatArray(frames) { if (weight[it] > 0f) sum[it] / weight[it] else Float.NaN }
        val half = (2000f / hopMs).toInt()
        val out = FloatArray(frames)
        var s = 0.0
        var n = 0
        // Running window mean over voiced frames.
        var lo = 0
        var hi = -1
        var last = 60f
        for (f in 0 until frames) {
            while (hi < min(frames - 1, f + half)) {
                hi++
                if (!raw[hi].isNaN()) { s += raw[hi]; n++ }
            }
            while (lo < f - half) {
                if (!raw[lo].isNaN()) { s -= raw[lo]; n-- }
                lo++
            }
            if (n > 0) last = (s / n).toFloat()
            out[f] = last
        }
        return out
    }

    private fun distanceToTrend(c: Contour, trend: FloatArray): Float {
        var s = 0f
        var n = 0
        for (f in c.start..c.end) {
            val p = c.pitchAt(f)
            if (p.isNaN()) continue
            s += abs(p - trend[f])
            n++
        }
        return if (n == 0) 99f else s / n
    }

    companion object {
        const val WINDOW = 1024
        const val HOP = 512
        private const val FFT = 2048
        private const val MAX_PEAKS = 48
        private const val HARMONICS = 8
        private const val ALPHA = 0.8f
        private const val MIN_HZ = 55f
        private const val MIN_MIDI = 33f
        private const val SAL_BINS = 600
        private const val PEAKS_PER_FRAME = 5
        private const val MAX_STEP = 0.9f
        private const val MAX_GAP = 4
        private const val MIN_LENGTH = 5
        private const val SEED_RATIO = 0.6f
        private val GHOSTS = floatArrayOf(12f, 19f, 24f)

        /** Emphasises the vocal band, damping bass and cymbals. */
        private fun bandWeight(hz: Float): Float = when {
            hz < 60f -> 0.05f
            hz < 180f -> 0.05f + 0.95f * (hz - 60f) / 120f
            hz < 4000f -> 1f
            hz < 8000f -> 1f - 0.8f * (hz - 4000f) / 4000f
            else -> 0.2f
        }
    }
}

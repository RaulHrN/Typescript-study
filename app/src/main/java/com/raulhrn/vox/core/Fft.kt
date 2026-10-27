package com.raulhrn.vox.core

import kotlin.math.PI
import kotlin.math.cos
import kotlin.math.sin

/**
 * In-place radix-2 complex FFT with precomputed twiddle and bit-reversal tables.
 * Allocation-free after construction. Not thread-safe: use one instance per thread.
 */
class Fft(val size: Int) {
    private val cosTable = FloatArray(size / 2)
    private val sinTable = FloatArray(size / 2)
    private val bitReverse = IntArray(size)

    init {
        require(size >= 2 && size and (size - 1) == 0) { "size must be a power of two" }
        val levels = Integer.numberOfTrailingZeros(size)
        for (i in 0 until size / 2) {
            cosTable[i] = cos(2.0 * PI * i / size).toFloat()
            sinTable[i] = sin(2.0 * PI * i / size).toFloat()
        }
        for (i in 0 until size) bitReverse[i] = Integer.reverse(i) ushr (32 - levels)
    }

    /** Forward DFT: X[k] = sum x[n] * e^(-2*pi*i*k*n/N). */
    fun forward(re: FloatArray, im: FloatArray) {
        for (i in 0 until size) {
            val j = bitReverse[i]
            if (j > i) {
                val tr = re[i]; re[i] = re[j]; re[j] = tr
                val ti = im[i]; im[i] = im[j]; im[j] = ti
            }
        }
        var half = 1
        while (half < size) {
            val step = size / (half * 2)
            var start = 0
            while (start < size) {
                var k = 0
                for (j in start until start + half) {
                    val c = cosTable[k]
                    val s = sinTable[k]
                    val l = j + half
                    val tre = re[l] * c + im[l] * s
                    val tim = im[l] * c - re[l] * s
                    re[l] = re[j] - tre
                    im[l] = im[j] - tim
                    re[j] += tre
                    im[j] += tim
                    k += step
                }
                start += half * 2
            }
            half *= 2
        }
    }
}

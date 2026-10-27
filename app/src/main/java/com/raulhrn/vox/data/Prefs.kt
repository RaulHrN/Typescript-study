package com.raulhrn.vox.data

import android.content.Context
import android.content.SharedPreferences
import androidx.compose.runtime.mutableFloatStateOf
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.mutableStateOf
import com.raulhrn.vox.audio.MicEngine
import com.raulhrn.vox.core.Naming

/** User preferences, backed by SharedPreferences and exposed as Compose state. */
object Prefs {
    private lateinit var sp: SharedPreferences

    private val namingState = mutableStateOf(Naming.SOLFEGE)
    private val a4State = mutableFloatStateOf(440f)
    private val sensitivityState = mutableFloatStateOf(0.5f)
    private val lowState = mutableIntStateOf(48)
    private val highState = mutableIntStateOf(67)
    private val measuredState = mutableStateOf(false)

    fun init(context: Context) {
        if (::sp.isInitialized) return
        sp = context.getSharedPreferences("vox", Context.MODE_PRIVATE)
        namingState.value = if (sp.getString("naming", null) == Naming.LETTERS.name) Naming.LETTERS else Naming.SOLFEGE
        a4State.floatValue = sp.getFloat("a4", 440f)
        sensitivityState.floatValue = sp.getFloat("sensitivity", 0.5f)
        lowState.intValue = sp.getInt("low", 48)
        highState.intValue = sp.getInt("high", 67)
        measuredState.value = sp.getBoolean("measured", false)
        applyToEngine()
    }

    var naming: Naming
        get() = namingState.value
        set(v) {
            namingState.value = v
            sp.edit().putString("naming", v.name).apply()
        }

    var a4: Float
        get() = a4State.floatValue
        set(v) {
            a4State.floatValue = v
            sp.edit().putFloat("a4", v).apply()
            applyToEngine()
        }

    /** 0 = only loud sounds, 1 = picks up very soft singing. */
    var sensitivity: Float
        get() = sensitivityState.floatValue
        set(v) {
            sensitivityState.floatValue = v
            sp.edit().putFloat("sensitivity", v).apply()
            applyToEngine()
        }

    val rangeLow: Int get() = lowState.intValue
    val rangeHigh: Int get() = highState.intValue
    val rangeMeasured: Boolean get() = measuredState.value

    fun setRange(low: Int, high: Int, measured: Boolean) {
        val lo = minOf(low, high - 5).coerceIn(28, 90)
        val hi = maxOf(high, lo + 5).coerceIn(lo + 5, 100)
        lowState.intValue = lo
        highState.intValue = hi
        measuredState.value = measured
        sp.edit().putInt("low", lo).putInt("high", hi).putBoolean("measured", measured).apply()
    }

    private fun applyToEngine() {
        MicEngine.a4 = a4
        MicEngine.gateDb = -38f - 24f * sensitivity
    }
}

package com.raulhrn.vox.ui.screens

import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.Slider
import androidx.compose.material3.SliderDefaults
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import com.raulhrn.vox.core.Naming
import com.raulhrn.vox.core.NoteNames
import com.raulhrn.vox.core.VoiceType
import com.raulhrn.vox.data.Prefs
import com.raulhrn.vox.ui.ChoiceGrid
import com.raulhrn.vox.ui.Header
import com.raulhrn.vox.ui.Nav
import com.raulhrn.vox.ui.Route
import com.raulhrn.vox.ui.ScreenPadding
import com.raulhrn.vox.ui.Section
import com.raulhrn.vox.ui.Stepper
import com.raulhrn.vox.ui.Vox
import com.raulhrn.vox.ui.VoxButton
import com.raulhrn.vox.ui.VoxType
import kotlin.math.roundToInt

@Composable
fun SettingsScreen(nav: Nav) {
    val c = Vox.colors
    val naming = Prefs.naming
    Column(Modifier.fillMaxSize().padding(horizontal = ScreenPadding)) {
        Header("Ajustes", onBack = { nav.pop() })
        Column(Modifier.weight(1f).verticalScroll(rememberScrollState())) {
            Section("Nomes das notas")
            ChoiceGrid(
                Naming.entries.toList(),
                columns = 2,
                selected = { it == naming },
                label = { if (it == Naming.SOLFEGE) "Dó Ré Mi" else "C D E" },
                onSelect = { Prefs.naming = it },
            )

            Section("Afinação")
            Stepper(
                "Lá central (A4)",
                "${Prefs.a4.roundToInt()} Hz",
                { Prefs.a4 = (Prefs.a4 - 1f).coerceAtLeast(415f) },
                { Prefs.a4 = (Prefs.a4 + 1f).coerceAtMost(466f) },
                detail = "O padrão é 440 Hz",
            )

            Section("Microfone")
            Text("Sensibilidade", style = VoxType.body, color = c.text)
            Text(
                "Aumente se o app não ouvir cantos suaves; diminua se captar ruído do ambiente.",
                style = VoxType.small,
                color = c.muted,
            )
            Slider(
                value = Prefs.sensitivity,
                onValueChange = { Prefs.sensitivity = it },
                colors = SliderDefaults.colors(thumbColor = c.text, activeTrackColor = c.text, inactiveTrackColor = c.line),
            )

            Section("Extensão vocal")
            Stepper(
                "Nota mais grave",
                NoteNames.full(Prefs.rangeLow, naming),
                { Prefs.setRange(Prefs.rangeLow - 1, Prefs.rangeHigh, Prefs.rangeMeasured) },
                { Prefs.setRange(Prefs.rangeLow + 1, Prefs.rangeHigh, Prefs.rangeMeasured) },
            )
            Stepper(
                "Nota mais aguda",
                NoteNames.full(Prefs.rangeHigh, naming),
                { Prefs.setRange(Prefs.rangeLow, Prefs.rangeHigh - 1, Prefs.rangeMeasured) },
                { Prefs.setRange(Prefs.rangeLow, Prefs.rangeHigh + 1, Prefs.rangeMeasured) },
                detail = VoiceType.classify(Prefs.rangeLow, Prefs.rangeHigh).label,
            )
            Spacer(Modifier.height(8.dp))
            VoxButton("Medir com o teste de extensão", { nav.push(Route.RangeTest) }, Modifier.fillMaxWidth(), primary = false)

            Section("Sobre")
            Text(
                "Cantorial: afinador vocal, exercícios, segunda voz e backing vocal. Tudo roda no seu celular: o app não tem acesso à internet " +
                    "e o som do microfone nunca é gravado nem enviado.",
                style = VoxType.small,
                color = c.muted,
            )
            Spacer(Modifier.height(32.dp))
        }
    }
}

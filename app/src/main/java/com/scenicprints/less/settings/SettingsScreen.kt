package com.scenicprints.less.settings

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.WindowInsets
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.safeDrawing
import androidx.compose.foundation.layout.windowInsetsPadding
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import com.scenicprints.less.data.Colourway
import com.scenicprints.less.data.Settings
import com.scenicprints.less.ui.Action
import com.scenicprints.less.ui.Choice
import com.scenicprints.less.ui.Label
import com.scenicprints.less.ui.LocalPalette
import com.scenicprints.less.ui.LocalType
import com.scenicprints.less.ui.Rule

class SettingsEvents(
    val colourway: (Colourway) -> Unit = {},
    val makeHome: () -> Unit = {},
    val unhide: (String) -> Unit = {},
    val update: () -> Unit = {},
    val back: () -> Unit = {}
)

/** Settings: every option in view, the chosen one marked, no switches. */
@Composable
fun SettingsScreen(
    settings: Settings,
    isHome: Boolean,
    hiddenLabels: Map<String, String>,
    updateLabel: String,
    version: String,
    on: SettingsEvents
) {
    val p = LocalPalette.current
    val type = LocalType.current
    Column(
        Modifier
            .fillMaxSize()
            .background(p.ground)
            .windowInsetsPadding(WindowInsets.safeDrawing)
            .padding(horizontal = 28.dp)
            .verticalScroll(rememberScrollState())
    ) {
        Spacer(Modifier.height(24.dp))
        Label("less")
        Spacer(Modifier.height(8.dp))
        Text("Settings", color = p.ink, style = type.search)

        Spacer(Modifier.height(26.dp))
        Label("appearance", colour = p.ink, modifier = Modifier.padding(bottom = 8.dp))
        Choice("Colourway", Colourway.entries, settings.colourway, { if (it == Colourway.BLACK) "Black" else "White" }, on.colourway)

        Spacer(Modifier.height(26.dp))
        Label("phone", colour = p.ink, modifier = Modifier.padding(bottom = 8.dp))
        Rule()
        if (!isHome) Action("Make Less the home screen", on.makeHome)
        Action(updateLabel, on.update, trailing = version)

        if (hiddenLabels.isNotEmpty()) {
            Spacer(Modifier.height(26.dp))
            Label("hidden from the drawer", colour = p.ink, modifier = Modifier.padding(bottom = 8.dp))
            Rule()
            hiddenLabels.forEach { (key, label) -> Action(label, { on.unhide(key) }, trailing = "show") }
        }

        Spacer(Modifier.height(40.dp))
        Row(Modifier.fillMaxWidth()) {
            Label("Hold the ground on the home to come back here. Back closes this.", Modifier.weight(1f))
        }
        Spacer(Modifier.height(40.dp))
    }
}

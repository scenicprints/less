package com.scenicprints.less.menu

import android.appwidget.AppWidgetProviderInfo
import android.content.pm.ShortcutInfo
import android.graphics.Rect
import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.gestures.detectTapGestures
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.offset
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.layout.wrapContentHeight
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.text.BasicTextField
import androidx.compose.foundation.text.KeyboardActions
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.ImageBitmap
import androidx.compose.ui.graphics.SolidColor
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.layout.onSizeChanged
import androidx.compose.ui.platform.LocalConfiguration
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.text.input.ImeAction
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.IntOffset
import androidx.compose.ui.unit.dp
import com.scenicprints.less.ui.Label
import com.scenicprints.less.ui.LocalPalette
import com.scenicprints.less.ui.LocalType
import com.scenicprints.less.ui.Rule
import kotlin.math.roundToInt

/** What was held. */
sealed class Target {
    data class App(val key: String, val onHome: Boolean) : Target()
    data class Deck(val keys: List<String>) : Target()
    data class Widget(val id: Int) : Target()
}

/** What the menu can do. The menu holds no logic; the activity does these. */
class MenuEvents(
    val shortcut: (ShortcutInfo) -> Unit = {},
    val appInfo: (String) -> Unit = {},
    val notifications: (String) -> Unit = {},
    val playStore: (String) -> Unit = {},
    val uninstall: (String) -> Unit = {},
    val rename: (String, String) -> Unit = { _, _ -> },
    val changeIcon: (String, String?) -> Unit = { _, _ -> },
    val hide: (String) -> Unit = {},
    val addToHome: (String) -> Unit = {},
    val removeFromHome: (String) -> Unit = {},
    val addWidget: (AppWidgetProviderInfo) -> Unit = {},
    val takeApart: () -> Unit = {},
    val removeDeck: () -> Unit = {},
    val removeWidget: (Int) -> Unit = {},
    val close: () -> Unit = {}
)

private enum class Mode { LIST, RENAME, ICON }

/**
 * A plain list over the icon: the shortcuts the app itself provides, then the things you can do.
 * Nothing else. Rename and change icon open inside it rather than in another screen.
 */
@Composable
fun HoldMenu(
    target: Target,
    anchor: Rect,
    label: String,
    shortcuts: List<ShortcutInfo>,
    shortcutIcon: (ShortcutInfo) -> ImageBitmap?,
    widgets: List<AppWidgetProviderInfo>,
    widgetLabel: (AppWidgetProviderInfo) -> String,
    drawn: Map<String, Int>,
    on: MenuEvents
) {
    val p = LocalPalette.current
    val type = LocalType.current
    val density = LocalDensity.current
    val config = LocalConfiguration.current
    var mode by remember { mutableStateOf(Mode.LIST) }
    var menuHeight by remember { mutableStateOf(0) }
    val screenH = with(density) { config.screenHeightDp.dp.toPx() }
    val gap = with(density) { 10.dp.toPx() }
    val below = anchor.bottom + gap + menuHeight < screenH
    val top = if (below) anchor.bottom + gap else anchor.top - gap - menuHeight

    Box(
        Modifier
            .fillMaxSize()
            .pointerInput(Unit) { detectTapGestures { on.close() } }
    ) {
        Column(
            Modifier
                .offset { IntOffset(0, top.roundToInt().coerceAtLeast(0)) }
                .padding(horizontal = 28.dp)
                .fillMaxWidth()
                .wrapContentHeight()
                .onSizeChanged { menuHeight = it.height }
                .background(p.ground)
                .border(1.dp, p.line)
                .pointerInput(Unit) { detectTapGestures { } }
                .padding(horizontal = 18.dp, vertical = 6.dp)
                .verticalScroll(rememberScrollState())
        ) {
            when (mode) {
                Mode.LIST -> ListMode(target, label, shortcuts, shortcutIcon, widgets, widgetLabel, on, { mode = it })
                Mode.RENAME -> RenameMode(target, label, on)
                Mode.ICON -> IconMode(target, drawn, on)
            }
        }
    }
}

@Composable
private fun ListMode(
    target: Target,
    label: String,
    shortcuts: List<ShortcutInfo>,
    shortcutIcon: (ShortcutInfo) -> ImageBitmap?,
    widgets: List<AppWidgetProviderInfo>,
    widgetLabel: (AppWidgetProviderInfo) -> String,
    on: MenuEvents,
    setMode: (Mode) -> Unit
) {
    val p = LocalPalette.current
    val type = LocalType.current
    Label(label, Modifier.padding(top = 10.dp, bottom = 8.dp), colour = p.ink)
    Rule()
    when (target) {
        is Target.App -> {
            shortcuts.forEach { s ->
                Row(
                    Modifier.fillMaxWidth().clickable { on.shortcut(s); on.close() }.padding(vertical = 10.dp),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    val bmp = shortcutIcon(s)
                    if (bmp != null) Image(bmp, null, Modifier.size(24.dp)) else Spacer(Modifier.size(24.dp))
                    Spacer(Modifier.width(14.dp))
                    Text(
                        (s.shortLabel ?: s.longLabel ?: "").toString(), color = p.ink, style = type.body,
                        maxLines = 1, overflow = TextOverflow.Ellipsis
                    )
                }
            }
            if (shortcuts.isNotEmpty()) Rule()
            Line("App info") { on.appInfo(target.key); on.close() }
            Line("Notifications") { on.notifications(target.key); on.close() }
            Line("Play Store") { on.playStore(target.key); on.close() }
            Line("Uninstall") { on.uninstall(target.key); on.close() }
            Rule()
            Line("Rename") { setMode(Mode.RENAME) }
            Line("Change icon") { setMode(Mode.ICON) }
            Line("Hide") { on.hide(target.key); on.close() }
            if (target.onHome) Line("Remove from home") { on.removeFromHome(target.key); on.close() }
            else Line("Add to home") { on.addToHome(target.key); on.close() }
            if (widgets.isNotEmpty()) {
                Rule()
                widgets.forEach { w -> Line("Widget: ${widgetLabel(w)}") { on.addWidget(w); on.close() } }
            }
        }
        is Target.Deck -> {
            Line("Take apart") { on.takeApart(); on.close() }
            Line("Remove from home") { on.removeDeck(); on.close() }
        }
        is Target.Widget -> {
            Line("Remove") { on.removeWidget(target.id); on.close() }
        }
    }
}

@Composable
private fun Line(text: String, onClick: () -> Unit) {
    val p = LocalPalette.current
    val type = LocalType.current
    Text(
        text, Modifier.fillMaxWidth().clickable(onClick = onClick).padding(vertical = 11.dp),
        color = p.ink, style = type.body
    )
}

@Composable
private fun RenameMode(target: Target, label: String, on: MenuEvents) {
    val p = LocalPalette.current
    val type = LocalType.current
    val key = (target as? Target.App)?.key ?: return
    var text by remember { mutableStateOf(label) }
    Label("rename", Modifier.padding(top = 10.dp, bottom = 8.dp))
    Rule()
    BasicTextField(
        value = text,
        onValueChange = { text = it },
        modifier = Modifier.fillMaxWidth().padding(vertical = 12.dp),
        textStyle = type.name.copy(color = p.ink),
        singleLine = true,
        cursorBrush = SolidColor(p.accent),
        keyboardOptions = KeyboardOptions(imeAction = ImeAction.Done),
        keyboardActions = KeyboardActions(onDone = { on.rename(key, text.trim()); on.close() })
    )
    Rule()
    Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween) {
        Label("original", Modifier.clickable { on.rename(key, ""); on.close() }.padding(vertical = 12.dp))
        Label("done", Modifier.clickable { on.rename(key, text.trim()); on.close() }.padding(vertical = 12.dp), colour = p.accent)
    }
}

@Composable
private fun IconMode(target: Target, drawn: Map<String, Int>, on: MenuEvents) {
    val p = LocalPalette.current
    val key = (target as? Target.App)?.key ?: return
    Label("change icon", Modifier.padding(top = 10.dp, bottom = 8.dp))
    Rule()
    val names = drawn.keys.toList()
    val perRow = 5
    Column(Modifier.padding(vertical = 12.dp), verticalArrangement = Arrangement.spacedBy(12.dp)) {
        names.chunked(perRow).forEach { row ->
            Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(12.dp)) {
                row.forEach { name ->
                    Image(
                        painterResource(drawn.getValue(name)), name,
                        Modifier.size(40.dp).clickable { on.changeIcon(key, name); on.close() }
                    )
                }
            }
        }
    }
    Rule()
    Label("stock icon", Modifier.clickable { on.changeIcon(key, null); on.close() }.padding(vertical = 12.dp), colour = p.accent)
}

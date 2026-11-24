package com.scenicprints.less.drawer

import android.graphics.Rect
import androidx.compose.animation.core.Animatable
import androidx.compose.foundation.ExperimentalFoundationApi
import androidx.compose.foundation.background
import androidx.compose.foundation.combinedClickable
import androidx.compose.foundation.gestures.detectTapGestures
import androidx.compose.foundation.gestures.detectVerticalDragGestures
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.interaction.collectIsFocusedAsState
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.WindowInsets
import androidx.compose.foundation.layout.fillMaxHeight
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.safeDrawing
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.layout.windowInsetsPadding
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.rememberLazyListState
import androidx.compose.foundation.text.BasicTextField
import androidx.compose.foundation.text.KeyboardActions
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.derivedStateOf
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.SolidColor
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.input.nestedscroll.NestedScrollConnection
import androidx.compose.ui.input.nestedscroll.NestedScrollSource
import androidx.compose.ui.input.nestedscroll.nestedScroll
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.layout.boundsInWindow
import androidx.compose.ui.layout.onGloballyPositioned
import androidx.compose.ui.layout.onSizeChanged
import androidx.compose.ui.platform.LocalFocusManager
import androidx.compose.ui.text.input.ImeAction
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.Velocity
import androidx.compose.ui.unit.dp
import com.scenicprints.less.apps.Entry
import com.scenicprints.less.apps.Index
import com.scenicprints.less.ui.Action
import com.scenicprints.less.ui.Label
import com.scenicprints.less.ui.LocalPalette
import com.scenicprints.less.ui.LocalType
import com.scenicprints.less.ui.Pull
import com.scenicprints.less.ui.Rule
import com.scenicprints.less.ui.mechanism
import kotlinx.coroutines.launch

/** What the search panel shows before a letter is typed. */
enum class Tab { FREQUENT, RECENT, NEW }

data class PanelData(
    val frequent: List<String> = emptyList(),
    val recent: List<String> = emptyList(),
    val newest: List<String> = emptyList(),
    /** Whether usage access is granted; without it FREQUENT and RECENT are a sentence asking for it. */
    val usageAllowed: Boolean = false
)

private sealed class Line {
    data class Letter(val c: Char) : Line()
    data class App(val e: Entry) : Line()
}

/**
 * The drawer: the only place with names. It slides up over the home as a panel with the pull on
 * its top edge and follows the finger. Search is the first line and never opens the keyboard by
 * itself. Letters are legends with a hairline. The knob on the right is the fast scroller.
 */
@OptIn(ExperimentalFoundationApi::class)
@Composable
fun Drawer(
    open: Boolean,
    setOpen: (Boolean) -> Unit,
    entries: List<Entry>,
    panel: PanelData,
    refreshPanel: () -> Unit,
    askUsage: () -> Unit,
    icon: @Composable (String, Dp) -> Unit,
    launch: (String, Rect?) -> Unit,
    hold: (String, Rect) -> Unit,
    reset: Int
) {
    val p = LocalPalette.current
    val type = LocalType.current
    val scope = rememberCoroutineScope()
    val focus = LocalFocusManager.current
    var height by remember { mutableStateOf(1f) }
    val offset = remember { Animatable(1f) } // 0 open, 1 closed, as a fraction of the height
    var query by remember { mutableStateOf("") }
    val list = rememberLazyListState()
    val searchFocus = remember { MutableInteractionSource() }
    val searching by searchFocus.collectIsFocusedAsState()
    var tab by remember { mutableStateOf(Tab.FREQUENT) }

    LaunchedEffect(open) {
        if (!open) focus.clearFocus()
        offset.animateTo(if (open) 0f else 1f, mechanism())
    }
    LaunchedEffect(reset) {
        query = ""
        focus.clearFocus()
        list.scrollToItem(0)
    }
    LaunchedEffect(searching) { if (searching) refreshPanel() }

    val lines: List<Line> = remember(entries, query) {
        if (query.isNotBlank()) Index.search(entries, query).map { Line.App(it) }
        else Index.grouped(entries).flatMap { g -> listOf(Line.Letter(g.letter)) + g.entries.map { Line.App(it) } }
    }
    val headers: Map<Char, Int> = remember(lines) {
        lines.withIndex().filter { it.value is Line.Letter }.associate { (it.value as Line.Letter).c to it.index }
    }
    val present = remember(headers) { headers.keys }
    val results = remember(lines) { lines.filterIsInstance<Line.App>() }

    // Where the knob's index points, from what the list is showing.
    val position by remember(lines) {
        derivedStateOf {
            val first = list.firstVisibleItemIndex
            val rail = Index.rail
            var letterStart = 0
            var letter = 0
            for ((c, i) in headers) {
                if (i <= first && i >= letterStart) { letterStart = i; letter = rail.indexOf(c) }
            }
            val next = headers.values.filter { it > letterStart }.minOrNull() ?: lines.size
            val span = (next - letterStart).coerceAtLeast(1)
            val within = (first - letterStart).coerceAtLeast(0) + (list.firstVisibleItemScrollOffset / 200f).coerceIn(0f, 0.99f)
            letter + (within / span).coerceIn(0f, 0.99f)
        }
    }
    var knobDrag by remember { mutableStateOf<Float?>(null) }

    // The list pushes the panel down when it is already at the top and the finger keeps going.
    val connection = remember {
        object : NestedScrollConnection {
            override fun onPostScroll(consumed: androidx.compose.ui.geometry.Offset, available: androidx.compose.ui.geometry.Offset, source: NestedScrollSource): androidx.compose.ui.geometry.Offset {
                if (available.y > 0 && source == NestedScrollSource.UserInput) {
                    scope.launch { offset.snapTo((offset.value + available.y / height).coerceIn(0f, 1f)) }
                    return available
                }
                return androidx.compose.ui.geometry.Offset.Zero
            }
            override suspend fun onPreFling(available: Velocity): Velocity {
                if (offset.value > 0f) {
                    val close = offset.value > 0.25f || available.y > 1500f
                    setOpen(!close)
                    offset.animateTo(if (close) 1f else 0f, mechanism())
                    return available
                }
                return Velocity.Zero
            }
        }
    }

    Box(
        Modifier
            .fillMaxSize()
            .onSizeChanged { height = it.height.toFloat().coerceAtLeast(1f) }
            .graphicsLayer { translationY = offset.value * height }
            .background(p.ground)
            .padding(start = 28.dp)
            .windowInsetsPadding(WindowInsets.safeDrawing)
    ) {
        Column(Modifier.fillMaxSize()) {
            // The pull rides on the panel's top edge, and the top of the panel follows the finger.
            Box(
                Modifier
                    .fillMaxWidth()
                    .padding(end = 28.dp)
                    .pointerInput(Unit) {
                        detectVerticalDragGestures(
                            onDragEnd = {
                                val close = offset.value > 0.25f
                                setOpen(!close)
                                scope.launch { offset.animateTo(if (close) 1f else 0f, mechanism()) }
                            }
                        ) { change, dy ->
                            scope.launch { offset.snapTo((offset.value + dy / height).coerceIn(0f, 1f)) }
                            change.consume()
                        }
                    }
            ) {
                Pull(Modifier.padding(top = 10.dp, bottom = 10.dp))
            }

            Row(Modifier.fillMaxWidth().padding(end = 28.dp, top = 8.dp, bottom = 12.dp), verticalAlignment = Alignment.CenterVertically) {
                BasicTextField(
                    value = query,
                    onValueChange = { query = it },
                    modifier = Modifier.weight(1f),
                    textStyle = type.search.copy(color = p.ink),
                    singleLine = true,
                    interactionSource = searchFocus,
                    cursorBrush = SolidColor(p.accent),
                    keyboardOptions = KeyboardOptions(imeAction = ImeAction.Go),
                    keyboardActions = KeyboardActions(onGo = { results.firstOrNull()?.let { launch(it.e.key, null) } }),
                    decorationBox = { field ->
                        Box {
                            if (query.isEmpty()) Text("Search", color = p.dim, style = type.search)
                            field()
                        }
                    }
                )
                if (query.isNotEmpty()) {
                    Label("clear", Modifier.pointerInput(Unit) { detectTapGestures { query = "" } })
                }
            }
            Rule(Modifier.padding(end = 28.dp), colour = if (searching) p.dim else p.line)

            if (searching && query.isBlank()) {
                SearchPanel(panel, tab, { tab = it }, askUsage, icon, launch)
            }

            Row(Modifier.weight(1f)) {
                LazyColumn(
                    Modifier
                        .weight(1f)
                        .fillMaxHeight()
                        .nestedScroll(connection),
                    state = list
                ) {
                    items(lines.size, key = { i -> when (val l = lines[i]) { is Line.Letter -> "L${l.c}"; is Line.App -> l.e.key } }) { i ->
                        when (val l = lines[i]) {
                            is Line.Letter -> {
                                Spacer(Modifier.height(22.dp))
                                Label(l.c.toString(), Modifier.padding(bottom = 8.dp))
                                Rule(Modifier.padding(end = 12.dp))
                            }
                            is Line.App -> {
                                var bounds by remember(l.e.key) { mutableStateOf<Rect?>(null) }
                                Row(
                                    Modifier
                                        .fillMaxWidth()
                                        .onGloballyPositioned { c ->
                                            val b = c.boundsInWindow()
                                            bounds = Rect(b.left.toInt(), b.top.toInt(), b.right.toInt(), b.bottom.toInt())
                                        }
                                        .combinedClickable(
                                            onClick = { launch(l.e.key, bounds) },
                                            onLongClick = { bounds?.let { hold(l.e.key, it) } }
                                        )
                                        .padding(vertical = 11.dp),
                                    verticalAlignment = Alignment.CenterVertically
                                ) {
                                    Box(Modifier.size(40.dp)) { icon(l.e.key, 40.dp) }
                                    Spacer(Modifier.width(18.dp))
                                    Text(l.e.label, color = p.ink, style = type.name, maxLines = 1, overflow = TextOverflow.Ellipsis)
                                }
                            }
                        }
                    }
                    item { Spacer(Modifier.height(48.dp)) }
                }

                if (query.isBlank() && !searching) {
                    Knob(
                        position = knobDrag ?: position,
                        present = present,
                        modifier = Modifier.width(130.dp).fillMaxHeight(),
                        onDrag = { delta ->
                            val next = ((knobDrag ?: position) + delta).coerceIn(0f, Index.rail.size - 0.01f)
                            knobDrag = next
                            val letter = Index.rail[next.toInt()]
                            val target = headers[letter] ?: headers.entries
                                .filter { Index.rail.indexOf(it.key) >= next.toInt() }
                                .minByOrNull { it.value }?.value
                            if (target != null) scope.launch { list.scrollToItem(target) }
                        },
                        onRelease = { knobDrag = null }
                    )
                }
            }
        }
    }
}

/** Beneath the search line, before a letter is typed: the chooser and two rows of five. */
@Composable
private fun SearchPanel(
    panel: PanelData,
    tab: Tab,
    setTab: (Tab) -> Unit,
    askUsage: () -> Unit,
    icon: @Composable (String, Dp) -> Unit,
    launch: (String, Rect?) -> Unit
) {
    val p = LocalPalette.current
    val type = LocalType.current
    Column(Modifier.fillMaxWidth().padding(end = 28.dp, top = 18.dp, bottom = 4.dp)) {
        Row(horizontalArrangement = Arrangement.spacedBy(26.dp)) {
            Tab.entries.forEach { t ->
                val on = t == tab
                Column(Modifier.pointerInput(t) { detectTapGestures { setTab(t) } }, horizontalAlignment = Alignment.CenterHorizontally) {
                    Label(t.name.lowercase(), colour = if (on) p.ink else p.dim)
                    Spacer(Modifier.height(6.dp))
                    Box(Modifier.width(14.dp).height(2.dp).background(if (on) p.accent else androidx.compose.ui.graphics.Color.Transparent))
                }
            }
        }
        val keys = when (tab) { Tab.FREQUENT -> panel.frequent; Tab.RECENT -> panel.recent; Tab.NEW -> panel.newest }
        if (tab != Tab.NEW && !panel.usageAllowed) {
            Spacer(Modifier.height(12.dp))
            Action("Allow usage access, so Less can tell which apps you open", askUsage, trailing = "settings")
        } else {
            Spacer(Modifier.height(20.dp))
            for (r in 0 until 2) {
                Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween) {
                    for (c in 0 until 5) {
                        val key = keys.getOrNull(r * 5 + c)
                        Box(Modifier.width(50.dp), contentAlignment = Alignment.Center) {
                            if (key != null) {
                                var bounds by remember(key) { mutableStateOf<Rect?>(null) }
                                Box(
                                    Modifier
                                        .size(50.dp)
                                        .onGloballyPositioned { co ->
                                            val b = co.boundsInWindow()
                                            bounds = Rect(b.left.toInt(), b.top.toInt(), b.right.toInt(), b.bottom.toInt())
                                        }
                                        .pointerInput(key) { detectTapGestures { launch(key, bounds) } }
                                ) { icon(key, 50.dp) }
                            }
                        }
                    }
                }
                if (r == 0) Spacer(Modifier.height(18.dp))
            }
        }
        Spacer(Modifier.height(20.dp))
        Rule()
    }
}

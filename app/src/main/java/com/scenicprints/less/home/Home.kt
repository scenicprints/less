package com.scenicprints.less.home

import android.graphics.Rect
import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.expandVertically
import androidx.compose.animation.shrinkVertically
import androidx.compose.foundation.background
import androidx.compose.foundation.gestures.detectDragGestures
import androidx.compose.foundation.gestures.detectTapGestures
import androidx.compose.foundation.gestures.detectVerticalDragGestures
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.BoxWithConstraints
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.WindowInsets
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.offset
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.safeDrawing
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.windowInsetsPadding
import androidx.compose.foundation.pager.HorizontalPager
import androidx.compose.foundation.pager.rememberPagerState
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.layout.Layout
import androidx.compose.ui.layout.boundsInWindow
import androidx.compose.ui.layout.onGloballyPositioned
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.IntOffset
import androidx.compose.ui.unit.dp
import com.scenicprints.less.data.COLUMNS
import com.scenicprints.less.data.Cell
import com.scenicprints.less.data.Item
import com.scenicprints.less.data.Layout as HomeLayout
import com.scenicprints.less.ui.Label
import com.scenicprints.less.ui.LocalPalette
import com.scenicprints.less.ui.LocalType
import com.scenicprints.less.ui.Pull
import com.scenicprints.less.ui.Rule
import com.scenicprints.less.ui.Scale
import com.scenicprints.less.ui.mechanism
import java.time.LocalDateTime
import java.time.format.DateTimeFormatter
import java.util.Locale
import com.scenicprints.less.ui.holdGround
import com.scenicprints.less.ui.holdable
import kotlinx.coroutines.launch
import kotlin.math.abs
import kotlin.math.roundToInt

data class Battery(val level: Int = 100, val charging: Boolean = false)

/** Where an icon sits: on a page, or in the dock. */
sealed class Place {
    data class OnPage(val page: Int, val item: Item) : Place()
    data class InDock(val index: Int) : Place()
}

/** Everything the home can ask for. The screen holds no logic and touches no system. */
class HomeEvents(
    val launch: (String, Rect?) -> Unit = { _, _ -> },
    /** An icon held still: show its menu at these bounds. */
    val hold: (Cell, Place, Rect) -> Unit = { _, _, _ -> },
    /** Something was dragged somewhere; this is the layout that results. */
    val arranged: (HomeLayout) -> Unit = {},
    val drawer: () -> Unit = {},
    val notifications: () -> Unit = {},
    val settings: () -> Unit = {},
    /** How many rows the pages can show, once measured, so the activity can place things. */
    val measured: (Int) -> Unit = {}
)

const val ICON = 56
private const val DOCK_SLOTS = 5

/**
 * The home as a Braun fascia: legend, clock, the battery scale, pages of icons, the dial, the
 * dock, the pull. Nothing floats and nothing is transparent. See DESIGN.md.
 */
@Composable
fun Home(
    layout: HomeLayout,
    now: LocalDateTime,
    hour24: Boolean,
    battery: Battery,
    icon: @Composable (String, Dp) -> Unit,
    widget: @Composable (Cell.Widget, Dp, Dp, (Rect) -> Unit, (Offset) -> Unit, () -> Unit) -> Unit,
    reset: Int,
    on: HomeEvents
) {
    val p = LocalPalette.current
    // One spare empty page past the last, always, so there is somewhere to swipe to and drop on.
    val pageCount = layout.pages.size + 1
    val pager = rememberPagerState(pageCount = { pageCount })
    val scope = rememberCoroutineScope()
    val density = LocalDensity.current
    val drag = remember { DragState() }
    var pagesBounds by remember { mutableStateOf<androidx.compose.ui.geometry.Rect?>(null) }
    var dockBounds by remember { mutableStateOf<androidx.compose.ui.geometry.Rect?>(null) }
    var rows by remember { mutableStateOf(1) }
    var cellPx by remember { mutableStateOf(1f) }
    var openDeck by remember { mutableStateOf<Place?>(null) }
    var swipe by remember { mutableStateOf(0f) }

    // The home button returns to the first page.
    LaunchedEffect(reset) { if (pager.currentPage != 0) pager.animateScrollToPage(0, animationSpec = mechanism()) }
    LaunchedEffect(rows) { on.measured(rows) }

    // A page held at an edge while dragging turns to the next one, after a pause.
    LaunchedEffect(drag.edge) {
        val e = drag.edge ?: return@LaunchedEffect
        kotlinx.coroutines.delay(450)
        val target = pager.currentPage + e
        if (target in 0 until pageCount) pager.animateScrollToPage(target, animationSpec = mechanism())
        drag.edge = null
    }

    fun drop() {
        val d = drag.dragging ?: return
        val pos = drag.pos
        val pb = pagesBounds
        val db = dockBounds
        var next = layout
        val cell = when (d) { is Place.OnPage -> d.item.cell; is Place.InDock -> layout.dock[d.index] }
        val w = (d as? Place.OnPage)?.item?.w ?: 1
        val h = (d as? Place.OnPage)?.item?.h ?: 1
        val lifted = when (d) {
            is Place.OnPage -> layout.remove(d.item)
            is Place.InDock -> layout.removeDock(d.index)
        }
        if (db != null && db.contains(pos) && cell !is Cell.Widget) {
            val slots = lifted.dock.size
            val x = (pos.x - db.left) / db.width
            val onCell = slots > 0 && run {
                val i = (x * slots).toInt().coerceIn(0, slots - 1)
                val centre = (i + 0.5f) / slots
                abs(x - centre) < 0.5f / slots * 0.6f
            }
            next = if (onCell) {
                val i = (x * slots).toInt().coerceIn(0, slots - 1)
                lifted.dockMerge(cell, i)
            } else {
                val i = (x * (slots + 1)).toInt().coerceIn(0, slots)
                if (slots < DOCK_SLOTS) lifted.dockAt(cell, i) else layout
            }
        } else if (pb != null && pb.contains(pos)) {
            val col = ((pos.x - pb.left) / cellPx - (w - 1) / 2f).roundToInt().coerceIn(0, COLUMNS - w)
            val row = ((pos.y - pb.top) / cellPx - (h - 1) / 2f).roundToInt().coerceIn(0, (rows - h).coerceAtLeast(0))
            val item = (d as? Place.OnPage)?.item ?: Item(cell, 0, 0)
            val page = pager.currentPage
            next = when (d) {
                is Place.OnPage -> layout.move(item, page, col, row, rows)
                is Place.InDock -> {
                    val target = Item(cell, col, row)
                    if (lifted.fits(page, target, rows)) lifted.withPage(page).put(page, target)
                    else lifted.itemAt(page, col, row)?.let { under ->
                        HomeLayout.merge(cell, under.cell)?.let { m -> lifted.remove(under).put(page, under.copy(cell = m)) }
                    } ?: layout
                }
            }
        }
        drag.dragging = null
        drag.edge = null
        if (next != layout) on.arranged(next.pruned())
    }

    Box(
        Modifier
            .fillMaxSize()
            .background(p.ground)
            .pointerInput(Unit) {
                detectVerticalDragGestures(
                    onDragStart = { swipe = 0f },
                    onDragEnd = {
                        if (swipe < -72.dp.toPx()) on.drawer()
                        if (swipe > 72.dp.toPx()) on.notifications()
                    }
                ) { _, dy -> swipe += dy }
            }
            .pointerInput(Unit) { detectTapGestures { openDeck = null } }
            .holdGround(claimed = { drag.claimed }, onDown = { drag.claimed = false }, onHold = on.settings)
    ) {
        Column(
            Modifier
                .fillMaxSize()
                .windowInsetsPadding(WindowInsets.safeDrawing)
                .padding(horizontal = 28.dp)
        ) {
            Spacer(Modifier.height(24.dp))
            Label(now.format(DateTimeFormatter.ofPattern("EEEE d MMMM", Locale.getDefault())))
            Spacer(Modifier.height(10.dp))
            Clock(now, hour24)
            Spacer(Modifier.height(18.dp))
            Scale(battery.level / 100f)
            Spacer(Modifier.height(8.dp))
            Row(Modifier.fillMaxWidth()) {
                Label(if (battery.charging) "charging" else "battery", Modifier.weight(1f))
                Label("${battery.level}", colour = p.ink)
            }
            Spacer(Modifier.height(18.dp))

            BoxWithConstraints(
                Modifier
                    .weight(1f)
                    .fillMaxWidth()
                    .onGloballyPositioned { pagesBounds = it.boundsInWindow() }
            ) {
                val cell = maxWidth / COLUMNS
                cellPx = with(density) { cell.toPx() }
                rows = (maxHeight / cell).toInt().coerceAtLeast(1)
                HorizontalPager(
                    pager,
                    Modifier.fillMaxSize(),
                    userScrollEnabled = drag.dragging == null,
                    beyondViewportPageCount = 1
                ) { page ->
                    PageGrid(
                        page = page,
                        items = layout.pages.getOrNull(page)?.items.orEmpty(),
                        cell = cell,
                        hidden = (drag.dragging as? Place.OnPage)?.takeIf { it.page == page }?.item,
                        icon = icon,
                        widget = widget,
                        drag = drag,
                        pagesBounds = { pagesBounds },
                        on = on,
                        openDeck = openDeck,
                        setOpenDeck = { openDeck = it },
                        drop = ::drop
                    )
                }
            }

            Spacer(Modifier.height(14.dp))
            Dial(
                pager, pageCount,
                scrollTo = { page, fraction -> scope.launch { pager.scrollToPage(page, fraction) } },
                snap = { scope.launch { pager.animateScrollToPage(pager.currentPage, animationSpec = mechanism()) } }
            )
            Spacer(Modifier.height(22.dp))

            Dock(
                dock = layout.dock,
                hidden = (drag.dragging as? Place.InDock)?.index,
                icon = icon,
                drag = drag,
                on = on,
                openDeck = openDeck,
                setOpenDeck = { openDeck = it },
                modifier = Modifier.onGloballyPositioned { dockBounds = it.boundsInWindow() },
                drop = ::drop
            )
            Spacer(Modifier.height(22.dp))
            Pull(Modifier.pointerInput(Unit) { detectTapGestures { on.drawer() } })
            Spacer(Modifier.height(6.dp))
        }

        // The deck that is open: its apps on a hairline track beneath the pile.
        val deck = openDeck
        val deckCell = when (deck) {
            is Place.OnPage -> deck.item.cell as? Cell.Deck
            is Place.InDock -> layout.dock.getOrNull(deck.index) as? Cell.Deck
            null -> null
        }
        val anchor = drag.anchors[deck]
        if (deckCell != null && anchor != null) {
            DeckRow(deckCell, anchor, icon) { key, bounds ->
                openDeck = null
                on.launch(key, bounds)
            }
        }

        // The icon in flight, drawn over everything at the finger.
        val d = drag.dragging
        if (d != null) {
            val cell = when (d) { is Place.OnPage -> d.item.cell; is Place.InDock -> layout.dock.getOrNull(d.index) }
            if (cell != null && cell !is Cell.Widget) {
                val half = with(density) { (ICON / 2).dp.toPx() }
                Box(
                    Modifier
                        .offset { IntOffset((drag.pos.x - half).roundToInt(), (drag.pos.y - half).roundToInt()) }
                        .graphicsLayer { scaleX = 1.08f; scaleY = 1.08f }
                ) { CellIcon(cell, icon) }
            }
        }
    }
}

/** What is being carried, where the finger is, and which edge it is resting on. */
class DragState {
    var dragging by mutableStateOf<Place?>(null)
    var pos by mutableStateOf(Offset.Zero)
    var edge by mutableStateOf<Int?>(null)
    /** Set the moment an icon is held, so a hold on the ground underneath it is not also a hold on the ground. */
    var claimed = false
    /** Where each icon last sat on screen, so menus and decks can be anchored to it. */
    val anchors = HashMap<Place, Rect>()
}

@Composable
private fun Clock(now: LocalDateTime, hour24: Boolean) {
    val p = LocalPalette.current
    val type = LocalType.current
    val pattern = if (hour24) "HH:mm" else "h:mm"
    Row(verticalAlignment = Alignment.Bottom) {
        Text(now.format(DateTimeFormatter.ofPattern(pattern, Locale.getDefault())), color = p.ink, style = type.clock)
        if (!hour24) {
            Spacer(Modifier.size(10.dp))
            Label(now.format(DateTimeFormatter.ofPattern("a", Locale.getDefault())), Modifier.padding(bottom = 14.dp))
        }
    }
}

/** The dial: the scale with one detent per page, the pointer geared to the pager. */
@Composable
private fun Dial(pager: androidx.compose.foundation.pager.PagerState, pages: Int, scrollTo: (Int, Float) -> Unit, snap: () -> Unit) {
    val progress = if (pages <= 1) 0.5f else (pager.currentPage + pager.currentPageOffsetFraction) / (pages - 1)
    var width by remember { mutableStateOf(1f) }
    Scale(
        progress.coerceIn(0f, 1f),
        Modifier
            .onGloballyPositioned { width = it.size.width.toFloat() }
            .pointerInput(pages) {
                detectDragGestures(onDragEnd = { snap() }, onDragCancel = { snap() }) { change, _ ->
                    if (pages <= 1) return@detectDragGestures
                    val f = (change.position.x / width).coerceIn(0f, 1f) * (pages - 1)
                    val page = f.roundToInt().coerceIn(0, pages - 1)
                    scrollTo(page, (f - page).coerceIn(-0.5f, 0.5f))
                    change.consume()
                }
            },
        detents = pages.coerceAtLeast(2),
        pointerWidth = 10f,
        pointerHeight = 20f
    )
}

/**
 * One page: items placed by column and row in a grid [COLUMNS] wide, apps and decks one cell,
 * widgets as many as they span.
 */
@Composable
private fun PageGrid(
    page: Int,
    items: List<Item>,
    cell: Dp,
    hidden: Item?,
    icon: @Composable (String, Dp) -> Unit,
    widget: @Composable (Cell.Widget, Dp, Dp, (Rect) -> Unit, (Offset) -> Unit, () -> Unit) -> Unit,
    drag: DragState,
    pagesBounds: () -> androidx.compose.ui.geometry.Rect?,
    on: HomeEvents,
    openDeck: Place?,
    setOpenDeck: (Place?) -> Unit,
    drop: () -> Unit
) {
    val density = LocalDensity.current
    val cellPx = with(density) { cell.roundToPx() }
    Layout(
        content = {
            items.forEach { item ->
                val place = Place.OnPage(page, item)
                Box(
                    Modifier
                        .onGloballyPositioned { c ->
                            val b = c.boundsInWindow()
                            drag.anchors[place] = Rect(b.left.toInt(), b.top.toInt(), b.right.toInt(), b.bottom.toInt())
                        }
                        .graphicsLayer { alpha = if (item == hidden) 0f else 1f },
                    contentAlignment = Alignment.Center
                ) {
                    when (val c = item.cell) {
                        is Cell.Widget -> widget(
                            c, cell * item.w, cell * item.h,
                            { r -> drag.anchors[place] = r; on.hold(c, place, r) },
                            { pos -> if (drag.dragging == null) drag.dragging = place; drag.pos = pos; drag.edge = edgeOf(pos, pagesBounds(), density.density) },
                            { drop() }
                        )
                        else -> Holdable(
                            cell = c,
                            place = place,
                            icon = icon,
                            drag = drag,
                            pagesBounds = pagesBounds,
                            on = on,
                            openDeck = openDeck,
                            setOpenDeck = setOpenDeck,
                            drop = drop
                        )
                    }
                }
            }
        }
    ) { measurables, constraints ->
        val placeables = measurables.mapIndexed { i, m ->
            val item = items[i]
            m.measure(
                androidx.compose.ui.unit.Constraints.fixed(
                    (cellPx * item.w).coerceAtMost(constraints.maxWidth),
                    (cellPx * item.h).coerceAtMost(constraints.maxHeight.coerceAtLeast(cellPx))
                )
            )
        }
        layout(constraints.maxWidth, constraints.maxHeight) {
            placeables.forEachIndexed { i, pl ->
                val item = items[i]
                pl.place(item.col * cellPx, item.row * cellPx)
            }
        }
    }
}

/** Which edge of the pages the finger rests on while carrying something: -1 left, 1 right, else null. */
private fun edgeOf(pos: Offset, bounds: androidx.compose.ui.geometry.Rect?, density: Float): Int? {
    bounds ?: return null
    val margin = 24 * density
    return when {
        pos.y < bounds.top || pos.y > bounds.bottom -> null
        pos.x < bounds.left + margin -> -1
        pos.x > bounds.right - margin -> 1
        else -> null
    }
}

/**
 * An icon that can be tapped, held, and carried. A hold shows the menu; a hold that then moves
 * is a drag, and the menu goes away as the icon lifts.
 */
@Composable
private fun Holdable(
    cell: Cell,
    place: Place,
    icon: @Composable (String, Dp) -> Unit,
    drag: DragState,
    pagesBounds: () -> androidx.compose.ui.geometry.Rect?,
    on: HomeEvents,
    openDeck: Place?,
    setOpenDeck: (Place?) -> Unit,
    drop: () -> Unit
) {
    val density = LocalDensity.current
    Box(
        Modifier
            .size(ICON.dp)
            .holdable(
                key = place,
                onTap = {
                    when (cell) {
                        is Cell.App -> on.launch(cell.key, drag.anchors[place])
                        is Cell.Deck -> setOpenDeck(if (openDeck == place) null else place)
                        is Cell.Widget -> Unit
                    }
                },
                onHold = {
                    drag.claimed = true
                    drag.anchors[place]?.let { on.hold(cell, place, it) }
                },
                onLift = {
                    drag.dragging = place
                    setOpenDeck(null)
                },
                onMove = { delta ->
                    val a = drag.anchors[place] ?: return@holdable
                    drag.pos = Offset(a.exactCenterX() + delta.x, a.exactCenterY() + delta.y)
                    drag.edge = edgeOf(drag.pos, pagesBounds(), density.density)
                },
                onDrop = drop
            ),
        contentAlignment = Alignment.Center
    ) {
        CellIcon(cell, icon)
    }
}

/** An app as its icon; a deck as a short pile, the next icon's edge showing beneath the top one. */
@Composable
fun CellIcon(cell: Cell, icon: @Composable (String, Dp) -> Unit) {
    when (cell) {
        is Cell.App -> icon(cell.key, ICON.dp)
        is Cell.Deck -> Box(Modifier.size(ICON.dp)) {
            cell.keys.getOrNull(1)?.let { under ->
                Box(Modifier.offset(x = 0.dp, y = 5.dp).graphicsLayer { scaleX = 0.92f; scaleY = 0.92f; alpha = 0.8f }) { icon(under, ICON.dp) }
            }
            icon(cell.keys[0], ICON.dp)
        }
        is Cell.Widget -> Unit
    }
}

/** The dock: a fixed row below the dial, up to five, spaced across the width. */
@Composable
private fun Dock(
    dock: List<Cell>,
    hidden: Int?,
    icon: @Composable (String, Dp) -> Unit,
    drag: DragState,
    on: HomeEvents,
    openDeck: Place?,
    setOpenDeck: (Place?) -> Unit,
    modifier: Modifier,
    drop: () -> Unit
) {
    Row(
        modifier.fillMaxWidth().height(ICON.dp),
        horizontalArrangement = if (dock.size >= 2) Arrangement.SpaceBetween else Arrangement.Center
    ) {
        dock.forEachIndexed { i, cell ->
            val place = Place.InDock(i)
            Box(
                Modifier
                    .onGloballyPositioned { c ->
                        val b = c.boundsInWindow()
                        drag.anchors[place] = Rect(b.left.toInt(), b.top.toInt(), b.right.toInt(), b.bottom.toInt())
                    }
                    .graphicsLayer { alpha = if (i == hidden) 0f else 1f }
            ) {
                Holdable(cell, place, icon, drag, { null }, on, openDeck, setOpenDeck, drop)
            }
        }
    }
}

/** A deck opened: its apps in a row beneath the pile, on a hairline track. */
@Composable
private fun DeckRow(deck: Cell.Deck, anchor: Rect, icon: @Composable (String, Dp) -> Unit, launch: (String, Rect?) -> Unit) {
    val p = LocalPalette.current
    val density = LocalDensity.current
    var visible by remember { mutableStateOf(false) }
    LaunchedEffect(Unit) { visible = true }
    val top = with(density) { (anchor.bottom + 10.dp.toPx()).roundToInt() }
    Box(
        Modifier
            .offset { IntOffset(0, top) }
            .fillMaxWidth()
            .padding(horizontal = 28.dp)
    ) {
        AnimatedVisibility(visible, enter = expandVertically(mechanism()), exit = shrinkVertically(mechanism())) {
            Column(Modifier.fillMaxWidth().background(p.ground)) {
                Rule()
                Row(
                    Modifier.fillMaxWidth().padding(vertical = 12.dp),
                    horizontalArrangement = Arrangement.spacedBy(16.dp)
                ) {
                    deck.keys.forEach { key ->
                        var bounds by remember(key) { mutableStateOf<Rect?>(null) }
                        Box(
                            Modifier
                                .size(ICON.dp)
                                .onGloballyPositioned { c ->
                                    val b = c.boundsInWindow()
                                    bounds = Rect(b.left.toInt(), b.top.toInt(), b.right.toInt(), b.bottom.toInt())
                                }
                                .pointerInput(key) { detectTapGestures { launch(key, bounds) } }
                        ) { icon(key, ICON.dp) }
                    }
                }
                Rule()
            }
        }
    }
}

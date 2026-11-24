package com.scenicprints.less.data

/**
 * What sits on the home, and where. Pure data with the moves on it, nothing from Android, so the
 * arranging rules can be tested on a desk.
 *
 * A page is a grid [COLUMNS] wide and as many rows as the phone can show. An [Item] holds a
 * [Cell] at a column and row with a span; apps and decks are one by one, a widget is whatever it
 * needs. The dock is a short row of one-by-one cells that stays on every page.
 */
const val COLUMNS = 4
const val DOCK_MAX = 5

sealed class Cell {
    data class App(val key: String) : Cell()
    /** A short pile of apps. [keys] is top first. */
    data class Deck(val keys: List<String>) : Cell()
    data class Widget(val id: Int, val provider: String) : Cell()
}

data class Item(val cell: Cell, val col: Int, val row: Int, val w: Int = 1, val h: Int = 1) {
    fun covers(c: Int, r: Int) = c in col until col + w && r in row until row + h
    fun overlaps(o: Item) = col < o.col + o.w && o.col < col + w && row < o.row + o.h && o.row < row + h
}

data class Page(val items: List<Item> = emptyList())

data class Layout(val pages: List<Page> = listOf(Page()), val dock: List<Cell> = emptyList()) {

    /** Every app key on the home, inside decks too. */
    fun keys(): Set<String> = buildSet {
        for (p in pages) for (i in p.items) addAll(i.cell.appKeys())
        for (c in dock) addAll(c.appKeys())
    }

    fun widgetIds(): List<Int> = pages.flatMap { p -> p.items.mapNotNull { (it.cell as? Cell.Widget)?.id } }

    fun itemAt(page: Int, col: Int, row: Int): Item? = pages.getOrNull(page)?.items?.firstOrNull { it.covers(col, row) }

    fun fits(page: Int, candidate: Item, rows: Int, ignoring: Item? = null): Boolean {
        if (candidate.col < 0 || candidate.row < 0) return false
        if (candidate.col + candidate.w > COLUMNS || candidate.row + candidate.h > rows) return false
        val items = pages.getOrNull(page)?.items ?: return true
        return items.none { it != ignoring && it.overlaps(candidate) }
    }

    /** The first free spot for a [w] by [h] thing, scanning pages in order, or null when the home is full. */
    fun firstFree(w: Int, h: Int, rows: Int): Triple<Int, Int, Int>? {
        for (p in pages.indices) for (r in 0..rows - h) for (c in 0..COLUMNS - w) {
            if (fits(p, Item(Cell.App(""), c, r, w, h), rows)) return Triple(p, c, r)
        }
        return null
    }

    /** Puts [cell] at the first free spot, opening a new page if every page is full. */
    fun add(cell: Cell, rows: Int, w: Int = 1, h: Int = 1): Layout {
        val spot = firstFree(w, h, rows)
        return if (spot != null) {
            val (p, c, r) = spot
            put(p, Item(cell, c, r, w, h))
        } else {
            copy(pages = pages + Page(listOf(Item(cell, 0, 0, w, h))))
        }
    }

    fun put(page: Int, item: Item): Layout = copy(pages = pages.mapIndexed { i, p ->
        if (i == page) Page(p.items + item) else p
    })

    fun remove(item: Item): Layout = copy(pages = pages.map { p -> Page(p.items.filter { it != item }) })

    fun removeDock(index: Int): Layout = copy(dock = dock.filterIndexed { i, _ -> i != index })

    /** Takes an app off the home wherever it is: on a page, in a deck, or in the dock. */
    fun removeApp(key: String): Layout {
        val newPages = pages.map { p ->
            Page(p.items.mapNotNull { it.without(key) })
        }
        val newDock = dock.mapNotNull { it.without(key) }
        return Layout(newPages, newDock)
    }

    /**
     * Moves [item] to [col],[row] on [page]. Onto an empty spot it just goes there. Onto an app
     * or deck it makes or joins a deck, which is how a deck is made: one icon dropped on another.
     * Onto a widget nothing happens.
     */
    fun move(item: Item, page: Int, col: Int, row: Int, rows: Int): Layout {
        val target = Item(item.cell, col, row, item.w, item.h)
        val without = remove(item).withPage(page)
        if (without.fits(page, target, rows)) return without.put(page, target)
        val under = without.itemAt(page, col, row) ?: return this
        val merged = merge(item.cell, under.cell) ?: return this
        return without.remove(under).put(page, under.copy(cell = merged))
    }

    /** Puts [cell] in the dock at [index], shifting the rest along. Full dock: nothing happens. */
    fun dockAt(cell: Cell, index: Int): Layout {
        if (cell is Cell.Widget || dock.size >= DOCK_MAX) return this
        val i = index.coerceIn(0, dock.size)
        return copy(dock = dock.toMutableList().apply { add(i, cell) })
    }

    /** Drops [cell] onto the dock cell at [index]: makes or joins a deck there. */
    fun dockMerge(cell: Cell, index: Int): Layout {
        val under = dock.getOrNull(index) ?: return this
        val merged = merge(cell, under) ?: return this
        return copy(dock = dock.mapIndexed { i, c -> if (i == index) merged else c })
    }

    /** Spreads a deck back into its apps, each placed where there is room. */
    fun takeApart(item: Item, rows: Int): Layout {
        val deck = item.cell as? Cell.Deck ?: return this
        var l = remove(item)
        for (k in deck.keys) l = l.add(Cell.App(k), rows)
        return l
    }

    /** Drops the pages that have nothing on them, keeping at least one. */
    fun pruned(): Layout {
        val kept = pages.filter { it.items.isNotEmpty() }
        return copy(pages = kept.ifEmpty { listOf(Page()) })
    }

    fun withPage(index: Int): Layout = if (index < pages.size) this else copy(pages = pages + List(index - pages.size + 1) { Page() })

    fun addPage(): Layout = copy(pages = pages + Page())

    companion object {
        fun merge(dropped: Cell, under: Cell): Cell? {
            val top = dropped.appKeys()
            if (top.isEmpty() || dropped is Cell.Widget) return null
            return when (under) {
                is Cell.App -> Cell.Deck(top + under.key)
                is Cell.Deck -> Cell.Deck(top + under.keys)
                is Cell.Widget -> null
            }
        }
    }
}

fun Cell.appKeys(): List<String> = when (this) {
    is Cell.App -> listOf(key)
    is Cell.Deck -> keys
    is Cell.Widget -> emptyList()
}

/** The cell without [key] in it: a deck of one becomes an app, an empty deck or the app itself becomes null. */
fun Cell.without(key: String): Cell? = when (this) {
    is Cell.App -> if (this.key == key) null else this
    is Cell.Deck -> {
        val rest = keys.filter { it != key }
        when (rest.size) {
            0 -> null
            1 -> Cell.App(rest[0])
            else -> Cell.Deck(rest)
        }
    }
    is Cell.Widget -> this
}

fun Item.without(key: String): Item? = cell.without(key)?.let { copy(cell = it) }

package com.scenicprints.less

import com.scenicprints.less.data.COLUMNS
import com.scenicprints.less.data.Cell
import com.scenicprints.less.data.Item
import com.scenicprints.less.data.Layout
import com.scenicprints.less.data.Page
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Test

class LayoutTest {

    private val rows = 3

    @Test
    fun addsIntoTheFirstFreeSpotAndOpensAPageWhenFull() {
        var l = Layout()
        repeat(COLUMNS * rows) { l = l.add(Cell.App("a$it"), rows) }
        assertEquals(1, l.pages.size)
        l = l.add(Cell.App("extra"), rows)
        assertEquals(2, l.pages.size)
        assertEquals(Item(Cell.App("extra"), 0, 0), l.pages[1].items.single())
    }

    @Test
    fun widgetsSpanCellsAndNothingLandsUnderThem() {
        val l = Layout().add(Cell.Widget(7, "p"), rows, w = 4, h = 2).add(Cell.App("a"), rows)
        assertEquals(Item(Cell.App("a"), 0, 2), l.pages[0].items[1])
    }

    @Test
    fun droppingOneIconOnAnotherMakesADeck() {
        val a = Item(Cell.App("a"), 0, 0)
        val b = Item(Cell.App("b"), 1, 0)
        val l = Layout(listOf(Page(listOf(a, b)))).move(a, 0, 1, 0, rows)
        assertEquals(listOf(Item(Cell.Deck(listOf("a", "b")), 1, 0)), l.pages[0].items)
    }

    @Test
    fun movingToAnEmptyCellJustMoves() {
        val a = Item(Cell.App("a"), 0, 0)
        val l = Layout(listOf(Page(listOf(a)))).move(a, 0, 3, 2, rows)
        assertEquals(listOf(Item(Cell.App("a"), 3, 2)), l.pages[0].items)
    }

    @Test
    fun movingPastTheLastPageMakesOne() {
        val a = Item(Cell.App("a"), 0, 0)
        val l = Layout(listOf(Page(listOf(a)))).move(a, 1, 0, 0, rows)
        assertEquals(2, l.pages.size)
        assertTrue(l.pages[0].items.isEmpty())
        assertEquals(listOf(Item(Cell.App("a"), 0, 0)), l.pages[1].items)
        assertEquals(1, l.pruned().pages.size)
    }

    @Test
    fun removingAnAppReachesIntoDecksAndTheDock() {
        val deck = Item(Cell.Deck(listOf("a", "b")), 0, 0)
        val l = Layout(listOf(Page(listOf(deck))), dock = listOf(Cell.App("a"), Cell.Deck(listOf("a", "c", "d"))))
            .removeApp("a")
        assertEquals(listOf(Item(Cell.App("b"), 0, 0)), l.pages[0].items)
        assertEquals(listOf(Cell.Deck(listOf("c", "d"))), l.dock)
    }

    @Test
    fun takingADeckApartSpreadsItsApps() {
        val deck = Item(Cell.Deck(listOf("a", "b", "c")), 0, 0)
        val l = Layout(listOf(Page(listOf(deck)))).takeApart(deck, rows)
        assertEquals(setOf("a", "b", "c"), l.keys())
        assertEquals(3, l.pages[0].items.size)
        assertNull(l.pages[0].items.firstOrNull { it.cell is Cell.Deck })
    }

    @Test
    fun dockHoldsFiveAndMergesIntoDecks() {
        var l = Layout()
        repeat(6) { l = l.dockAt(Cell.App("d$it"), it) }
        assertEquals(5, l.dock.size)
        l = l.dockMerge(Cell.App("x"), 0)
        assertEquals(Cell.Deck(listOf("x", "d0")), l.dock[0])
    }
}

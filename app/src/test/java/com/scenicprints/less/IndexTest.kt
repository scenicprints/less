package com.scenicprints.less

import com.scenicprints.less.apps.Entry
import com.scenicprints.less.apps.Index
import com.scenicprints.less.apps.fraction
import com.scenicprints.less.apps.rows
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test

class IndexTest {

    private val apps = listOf(
        Entry("maps", "Maps"),
        Entry("gmaps", "Google Maps"),
        Entry("the", "The Guardian"),
        Entry("num", "1Password"),
        Entry("cam", "camera"),
        Entry("cal", "Calendar")
    )

    @Test
    fun sortsIgnoringCaseAndArticles() {
        assertEquals(
            listOf("1Password", "Calendar", "camera", "Google Maps", "The Guardian", "Maps"),
            Index.sorted(apps).map { it.label }
        )
    }

    @Test
    fun groupsUnderLettersWithHashForDigits() {
        val g = Index.grouped(apps)
        assertEquals(listOf('#', 'C', 'G', 'M'), g.map { it.letter })
        assertEquals(listOf("Calendar", "camera"), g[1].entries.map { it.label })
        assertEquals(listOf("Google Maps", "The Guardian"), g[2].entries.map { it.label })
    }

    @Test
    fun searchIsAPrefixOnAnyWord() {
        assertEquals(listOf("Google Maps", "Maps"), Index.search(apps, "ma").map { it.label })
        assertTrue(Index.matches("Google Maps", "goo"))
        assertFalse(Index.matches("Google Maps", "oogle"))
        assertTrue(Index.matches("Anything", "  "))
    }

    @Test
    fun railEndsWithHash() {
        assertEquals(27, Index.rail.size)
        assertEquals('#', Index.rail.last())
        assertEquals('A', Index.rail.first())
    }

    @Test
    fun rowsRoundUp() {
        assertEquals(0, rows(0, 4))
        assertEquals(1, rows(4, 4))
        assertEquals(2, rows(5, 4))
        assertEquals(3, rows(12, 4))
    }

    @Test
    fun fractionStaysOnTheScale() {
        assertEquals(0.5f, fraction(50.0, 0.0, 100.0), 0.0001f)
        assertEquals(0f, fraction(-3.0, 0.0, 100.0), 0.0001f)
        assertEquals(1f, fraction(300.0, 0.0, 100.0), 0.0001f)
    }
}

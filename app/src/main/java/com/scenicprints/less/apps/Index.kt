package com.scenicprints.less.apps

/**
 * The list logic, with nothing from Android in it so it can be tested on a desk.
 *
 * An [Entry] is one launchable activity. [key] is its flattened component name, which is what the
 * settings store, and [label] is what the user sees.
 */
data class Entry(val key: String, val label: String)

/** A run of entries under one letter of the rail. */
data class Group(val letter: Char, val entries: List<Entry>)

object Index {

    /** Alphabetical by label, ignoring case and leading articles like "The". Ties fall to the key. */
    fun sorted(entries: List<Entry>): List<Entry> =
        entries.sortedWith(compareBy<Entry> { sortName(it.label) }.thenBy { it.key })

    /** The letter an app files under: its first letter, or # for anything that is not a letter. */
    fun letterOf(label: String): Char {
        val c = sortName(label).firstOrNull() ?: return '#'
        return if (c.isLetter()) c.uppercaseChar() else '#'
    }

    fun grouped(entries: List<Entry>): List<Group> =
        sorted(entries).groupBy { letterOf(it.label) }.map { (l, e) -> Group(l, e) }

    /** The 27 stops on the rail, # last, so a drag along it has somewhere to land for every letter. */
    val rail: List<Char> = ('A'..'Z').toList() + '#'

    /**
     * Search is a prefix match on any word of the label, so "ma" finds Maps and Google Maps alike,
     * and nothing is found by a letter buried in the middle of a word.
     */
    fun matches(label: String, query: String): Boolean {
        val q = query.trim().lowercase()
        if (q.isEmpty()) return true
        return label.lowercase().split(' ', '-', '_', '.').any { it.startsWith(q) }
    }

    fun search(entries: List<Entry>, query: String): List<Entry> =
        sorted(entries.filter { matches(it.label, query) })

    private fun sortName(label: String): String {
        val t = label.trim().lowercase()
        return if (t.length > 4 && t.startsWith("the ")) t.substring(4) else t
    }
}

/** How many rows a grid of [count] keys needs at [columns] across. */
fun rows(count: Int, columns: Int): Int = if (count <= 0) 0 else (count + columns - 1) / columns

/** A value as a position along a scale, held inside the scale. */
fun fraction(value: Double, min: Double, max: Double): Float =
    ((value - min) / (max - min)).coerceIn(0.0, 1.0).toFloat()

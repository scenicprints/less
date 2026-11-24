package com.scenicprints.less.data

/** The two Braun colourways. Chosen once in settings; never switched by the clock or the system. */
enum class Colourway { BLACK, WHITE }

data class Settings(
    val colourway: Colourway = Colourway.BLACK,
    /** Apps taken out of the drawer and search. Still installed, just not in the way. */
    val hidden: Set<String> = emptySet(),
    /** Drawer labels the user has changed, by component key. */
    val names: Map<String, String> = emptyMap(),
    /** Drawn icons assigned by hand, by component key, naming an entry in the icon table. */
    val icons: Map<String, String> = emptyMap(),
    /** Whether the first-run home has been laid out, so an emptied home stays empty. */
    val seeded: Boolean = false
)

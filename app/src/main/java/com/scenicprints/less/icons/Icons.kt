package com.scenicprints.less.icons

import android.content.ComponentName
import android.content.Context
import org.json.JSONObject

/**
 * The icon database, read from assets/icons.json, which icons/make.py writes: package name to
 * drawable name. Every drawn icon is a vector drawable in res/drawable, drawn on the 56 unit grid
 * with the 14 unit corner; see icons/README.md for the rules.
 *
 * An app with no row shows its stock icon until one is drawn. The hold menu can assign any drawn
 * icon to any app, which lands in Settings.icons and wins over the index.
 */
class Icons(c: Context) {

    /** Drawable name, like ic_phone, by package. */
    val byPackage: Map<String, String>

    /** Every drawable name in the set, by its short name, like phone, for the picker. */
    val drawn: Map<String, Int>

    init {
        val index = runCatching {
            val o = JSONObject(c.assets.open("icons.json").bufferedReader().use { it.readText() })
            o.keys().asSequence().associateWith { o.getString(it) }
        }.getOrDefault(emptyMap())
        byPackage = index
        val res = c.resources
        drawn = index.values.distinct().sorted().mapNotNull { name ->
            val id = res.getIdentifier(name, "drawable", c.packageName)
            if (id == 0) null else name.removePrefix("ic_") to id
        }.toMap()
    }

    /** The drawable for a component key, honouring a hand assignment first, or null for stock. */
    fun of(key: String, assigned: Map<String, String>): Int? {
        assigned[key]?.let { name -> drawn[name]?.let { return it } }
        val pkg = ComponentName.unflattenFromString(key)?.packageName ?: return null
        return byPackage[pkg]?.let { drawn[it.removePrefix("ic_")] }
    }
}

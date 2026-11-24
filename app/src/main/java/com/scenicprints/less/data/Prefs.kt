package com.scenicprints.less.data

import android.content.Context
import androidx.core.content.edit
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import org.json.JSONObject

/** Settings in SharedPreferences, read once and then kept in a flow the screens watch. */
object Prefs {
    private const val FILE = "less"
    private var flow: MutableStateFlow<Settings>? = null

    fun settings(c: Context): StateFlow<Settings> {
        flow?.let { return it }
        return MutableStateFlow(read(c)).also { flow = it }
    }

    fun update(c: Context, change: (Settings) -> Settings) {
        val f = settings(c) as MutableStateFlow<Settings>
        val next = change(f.value)
        if (next == f.value) return
        f.value = next
        write(c, next)
    }

    private fun read(c: Context): Settings {
        val p = c.getSharedPreferences(FILE, Context.MODE_PRIVATE)
        return Settings(
            colourway = Colourway.entries.firstOrNull { it.name == p.getString("colourway", null) } ?: Colourway.BLACK,
            hidden = p.getStringSet("hidden", emptySet()).orEmpty(),
            names = map(p.getString("names", null)),
            icons = map(p.getString("icons", null)),
            seeded = p.getBoolean("seeded", false)
        )
    }

    private fun write(c: Context, s: Settings) {
        c.getSharedPreferences(FILE, Context.MODE_PRIVATE).edit {
            putString("colourway", s.colourway.name)
            putStringSet("hidden", s.hidden)
            putString("names", JSONObject(s.names).toString())
            putString("icons", JSONObject(s.icons).toString())
            putBoolean("seeded", s.seeded)
        }
    }

    private fun map(json: String?): Map<String, String> = runCatching {
        val o = JSONObject(json ?: return emptyMap())
        o.keys().asSequence().associateWith { o.getString(it) }
    }.getOrDefault(emptyMap())
}

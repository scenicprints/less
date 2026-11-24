package com.scenicprints.less.data

import android.content.Context
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import org.json.JSONArray
import org.json.JSONObject
import java.io.File

/**
 * The home layout, in memory and mirrored to one JSON file. It is small, so it is rewritten whole
 * on every change, and the file is plain enough to read: that is the backup.
 */
object LayoutStore {
    private val _flow = MutableStateFlow(Layout())
    val flow: StateFlow<Layout> = _flow.asStateFlow()
    private var loaded = false

    private fun file(c: Context) = File(c.filesDir, "layout.json")

    @Synchronized
    fun load(c: Context) {
        if (loaded) return
        loaded = true
        _flow.value = runCatching { fromJson(JSONObject(file(c).readText())) }.getOrDefault(Layout())
    }

    @Synchronized
    fun update(c: Context, change: (Layout) -> Layout) {
        load(c)
        val next = change(_flow.value)
        if (next == _flow.value) return
        _flow.value = next
        runCatching {
            val tmp = File(c.filesDir, "layout.json.tmp")
            tmp.writeText(toJson(next).toString(2))
            tmp.renameTo(file(c))
        }
    }

    fun toJson(l: Layout): JSONObject = JSONObject().apply {
        put("pages", JSONArray().apply {
            l.pages.forEach { p ->
                put(JSONArray().apply { p.items.forEach { put(itemJson(it)) } })
            }
        })
        put("dock", JSONArray().apply { l.dock.forEach { put(cellJson(it)) } })
    }

    fun fromJson(o: JSONObject): Layout {
        val pages = o.getJSONArray("pages").let { arr ->
            (0 until arr.length()).map { i ->
                val items = arr.getJSONArray(i)
                Page((0 until items.length()).mapNotNull { j -> runCatching { item(items.getJSONObject(j)) }.getOrNull() })
            }
        }
        val dock = o.optJSONArray("dock")?.let { arr ->
            (0 until arr.length()).mapNotNull { runCatching { cell(arr.getJSONObject(it)) }.getOrNull() }
        }.orEmpty()
        return Layout(pages.ifEmpty { listOf(Page()) }, dock)
    }

    private fun itemJson(i: Item) = cellJson(i.cell).apply {
        put("col", i.col); put("row", i.row); put("w", i.w); put("h", i.h)
    }

    private fun cellJson(c: Cell) = JSONObject().apply {
        when (c) {
            is Cell.App -> { put("type", "app"); put("key", c.key) }
            is Cell.Deck -> { put("type", "deck"); put("keys", JSONArray(c.keys)) }
            is Cell.Widget -> { put("type", "widget"); put("id", c.id); put("provider", c.provider) }
        }
    }

    private fun item(o: JSONObject) = Item(cell(o), o.getInt("col"), o.getInt("row"), o.optInt("w", 1), o.optInt("h", 1))

    private fun cell(o: JSONObject): Cell = when (o.getString("type")) {
        "app" -> Cell.App(o.getString("key"))
        "deck" -> Cell.Deck(o.getJSONArray("keys").let { a -> (0 until a.length()).map { a.getString(it) } })
        "widget" -> Cell.Widget(o.getInt("id"), o.optString("provider"))
        else -> throw IllegalArgumentException("unknown cell")
    }
}

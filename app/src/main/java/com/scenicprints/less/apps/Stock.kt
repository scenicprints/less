package com.scenicprints.less.apps

import android.content.pm.LauncherActivityInfo
import android.graphics.Bitmap
import android.graphics.Canvas
import androidx.compose.ui.graphics.ImageBitmap
import androidx.compose.ui.graphics.asImageBitmap

/** An app's own icon, as the app shipped it, for every app that has not been drawn yet. */
object Stock {
    private val cache = HashMap<String, ImageBitmap>()

    fun clear() = cache.clear()

    fun of(info: LauncherActivityInfo, sizePx: Int): ImageBitmap {
        val id = "${info.componentName.flattenToString()}/$sizePx"
        cache[id]?.let { return it }
        val icon = info.getIcon(0)
        val bitmap = Bitmap.createBitmap(sizePx, sizePx, Bitmap.Config.ARGB_8888)
        icon.setBounds(0, 0, sizePx, sizePx)
        icon.draw(Canvas(bitmap))
        return bitmap.asImageBitmap().also { cache[id] = it }
    }
}

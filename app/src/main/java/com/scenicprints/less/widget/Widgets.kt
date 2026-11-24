package com.scenicprints.less.widget

import android.annotation.SuppressLint
import android.app.Activity
import android.appwidget.AppWidgetHost
import android.appwidget.AppWidgetHostView
import android.appwidget.AppWidgetManager
import android.appwidget.AppWidgetProviderInfo
import android.content.Context
import android.content.Intent
import android.graphics.Rect
import android.os.Bundle
import android.os.Handler
import android.os.Looper
import android.util.SizeF
import android.view.MotionEvent
import android.view.ViewConfiguration
import androidx.compose.foundation.layout.size
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.viewinterop.AndroidView
import kotlin.math.abs

/**
 * Widgets are arranged exactly like apps: they sit in the grid, span the cells they need, and are
 * held and carried the same way. The host is the plain platform one; the widget is drawn by its
 * own app and we give it a flat place to stand.
 */
class Widgets(private val activity: Activity) {

    /** The host makes our view for every widget, so updates reach it and it can be held and carried. */
    private val host = object : AppWidgetHost(activity, HOST_ID) {
        override fun onCreateView(context: Context, appWidgetId: Int, appWidget: AppWidgetProviderInfo?): AppWidgetHostView =
            HoldableHostView(context)
    }
    val manager: AppWidgetManager = AppWidgetManager.getInstance(activity)

    fun startListening() = runCatching { host.startListening() }
    fun stopListening() = runCatching { host.stopListening() }

    fun info(id: Int): AppWidgetProviderInfo? = if (id < 0) null else manager.getAppWidgetInfo(id)

    /** Every home screen widget on the phone, by its app's name then its own. */
    fun providers(): List<AppWidgetProviderInfo> = manager.installedProviders
        .filter { it.widgetCategory and AppWidgetProviderInfo.WIDGET_CATEGORY_HOME_SCREEN != 0 }
        .sortedWith(compareBy({ appName(it).lowercase() }, { label(it).lowercase() }))

    fun providersOf(pkg: String): List<AppWidgetProviderInfo> = providers().filter { it.provider.packageName == pkg }

    fun appName(p: AppWidgetProviderInfo): String = runCatching {
        val pm = activity.packageManager
        pm.getApplicationLabel(pm.getApplicationInfo(p.provider.packageName, 0)).toString()
    }.getOrDefault(p.provider.packageName)

    fun label(p: AppWidgetProviderInfo): String = runCatching { p.loadLabel(activity.packageManager) }.getOrDefault("Widget")

    fun allocate(): Int = host.allocateAppWidgetId()

    fun delete(id: Int) {
        if (id >= 0) runCatching { host.deleteAppWidgetId(id) }
    }

    /** True if bound. False means the system wants to ask the user first; see [bindIntent]. */
    fun bind(id: Int, p: AppWidgetProviderInfo): Boolean = manager.bindAppWidgetIdIfAllowed(id, p.profile, p.provider, null)

    fun bindIntent(id: Int, p: AppWidgetProviderInfo): Intent = Intent(AppWidgetManager.ACTION_APPWIDGET_BIND)
        .putExtra(AppWidgetManager.EXTRA_APPWIDGET_ID, id)
        .putExtra(AppWidgetManager.EXTRA_APPWIDGET_PROVIDER, p.provider)
        .putExtra(AppWidgetManager.EXTRA_APPWIDGET_PROVIDER_PROFILE, p.profile)

    fun needsConfigure(p: AppWidgetProviderInfo): Boolean = p.configure != null

    /** The widget's own set-up screen. Its answer comes back through the activity's onActivityResult. */
    fun configure(id: Int) {
        runCatching { host.startAppWidgetConfigureActivityForResult(activity, id, 0, REQUEST_CONFIGURE, null) }
    }

    /** How many grid cells a widget wants, from its minimum size, never less than one by one. */
    fun span(p: AppWidgetProviderInfo, cellDp: Float): Pair<Int, Int> {
        val density = activity.resources.displayMetrics.density
        val w = Math.ceil((p.minWidth / density / cellDp).toDouble()).toInt().coerceIn(1, 4)
        val h = Math.ceil((p.minHeight / density / cellDp).toDouble()).toInt().coerceIn(1, 4)
        return w to h
    }

    /** Goes through the host, which registers the view and gives it the widget's current drawing. */
    fun view(c: Context, id: Int, info: AppWidgetProviderInfo): HoldableHostView =
        host.createView(c, id, info) as HoldableHostView

    companion object {
        const val HOST_ID = 1024
        const val REQUEST_CONFIGURE = 41
    }
}

/**
 * A host view that notices a hold. The widget gets every tap and scroll as usual; only a finger
 * that stays still is taken, and from then on the moves are ours, so the widget can be carried
 * like an icon.
 */
@SuppressLint("ViewConstructor")
class HoldableHostView(c: Context) : AppWidgetHostView(c) {
    var onHold: ((Rect) -> Unit)? = null
    var onMove: ((Offset) -> Unit)? = null
    var onRelease: (() -> Unit)? = null

    private val handler = Handler(Looper.getMainLooper())
    private val slop = ViewConfiguration.get(c).scaledTouchSlop
    private var downX = 0f
    private var downY = 0f
    private var held = false
    private var moved = false
    private val fire = Runnable {
        held = true
        moved = false
        val r = Rect()
        getGlobalVisibleRect(r)
        onHold?.invoke(r)
    }

    override fun onInterceptTouchEvent(ev: MotionEvent): Boolean {
        when (ev.actionMasked) {
            MotionEvent.ACTION_DOWN -> {
                downX = ev.rawX; downY = ev.rawY; held = false; moved = false
                handler.postDelayed(fire, ViewConfiguration.getLongPressTimeout().toLong())
            }
            MotionEvent.ACTION_MOVE -> if (!held && (abs(ev.rawX - downX) > slop || abs(ev.rawY - downY) > slop)) handler.removeCallbacks(fire)
            MotionEvent.ACTION_UP, MotionEvent.ACTION_CANCEL -> handler.removeCallbacks(fire)
        }
        return held
    }

    override fun onTouchEvent(ev: MotionEvent): Boolean {
        if (!held) return super.onTouchEvent(ev)
        when (ev.actionMasked) {
            MotionEvent.ACTION_MOVE -> {
                if (!moved && (abs(ev.rawX - downX) > slop * 2 || abs(ev.rawY - downY) > slop * 2)) moved = true
                if (moved) {
                    val loc = IntArray(2)
                    rootView.getLocationOnScreen(loc)
                    onMove?.invoke(Offset(ev.rawX - loc[0], ev.rawY - loc[1]))
                }
            }
            MotionEvent.ACTION_UP, MotionEvent.ACTION_CANCEL -> {
                held = false
                if (moved) onRelease?.invoke()
            }
        }
        return true
    }
}

@Composable
fun WidgetSlot(
    widgets: Widgets,
    id: Int,
    info: AppWidgetProviderInfo,
    width: Dp,
    height: Dp,
    onHold: (Rect) -> Unit,
    onMove: (Offset) -> Unit,
    onRelease: () -> Unit
) {
    AndroidView(
        factory = { c ->
            widgets.view(c, id, info).apply {
                updateAppWidgetSize(Bundle(), listOf(SizeF(width.value, height.value)))
            }
        },
        update = { v ->
            v.onHold = onHold
            v.onMove = onMove
            v.onRelease = onRelease
        },
        modifier = Modifier.size(width, height)
    )
}

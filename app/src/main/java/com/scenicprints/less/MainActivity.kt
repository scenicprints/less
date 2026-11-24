package com.scenicprints.less

import android.app.Activity
import android.app.role.RoleManager
import android.appwidget.AppWidgetManager
import android.appwidget.AppWidgetProviderInfo
import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent
import android.content.IntentFilter
import android.graphics.Rect
import android.os.BatteryManager
import android.os.Bundle
import android.text.format.DateFormat
import android.widget.Toast
import androidx.activity.ComponentActivity
import androidx.activity.compose.BackHandler
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.Image
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.size
import androidx.compose.runtime.Composable
import androidx.compose.runtime.DisposableEffect
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.asImageBitmap
import androidx.compose.ui.platform.LocalConfiguration
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.unit.Dp
import androidx.core.graphics.drawable.toBitmap
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.scenicprints.less.apps.Apps
import com.scenicprints.less.apps.Index
import com.scenicprints.less.apps.Stock
import com.scenicprints.less.apps.Usage
import com.scenicprints.less.data.COLUMNS
import com.scenicprints.less.data.Cell
import com.scenicprints.less.data.Layout
import com.scenicprints.less.data.LayoutStore
import com.scenicprints.less.data.Page
import com.scenicprints.less.data.Prefs
import com.scenicprints.less.drawer.Drawer
import com.scenicprints.less.drawer.PanelData
import com.scenicprints.less.home.Battery
import com.scenicprints.less.home.Home
import com.scenicprints.less.home.HomeEvents
import com.scenicprints.less.home.Place
import com.scenicprints.less.icons.Icons
import com.scenicprints.less.menu.HoldMenu
import com.scenicprints.less.menu.MenuEvents
import com.scenicprints.less.menu.Target
import com.scenicprints.less.settings.SettingsEvents
import com.scenicprints.less.settings.SettingsScreen
import com.scenicprints.less.ui.LessTheme
import com.scenicprints.less.update.Updater
import com.scenicprints.less.widget.WidgetSlot
import com.scenicprints.less.widget.Widgets
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch
import java.time.LocalDateTime

class MainActivity : ComponentActivity() {

    lateinit var apps: Apps
    lateinit var widgets: Widgets
    lateinit var icons: Icons
    lateinit var usage: Usage

    /** How many times the home button has been pressed while we were already up. */
    private val homePresses = mutableIntStateOf(0)

    /** A widget whose own set-up screen has just finished, by id; -1 for one that was abandoned. */
    val configured = mutableStateOf<Pair<Int, Boolean>?>(null)

    override fun onCreate(savedInstanceState: Bundle?) {
        enableEdgeToEdge()
        super.onCreate(savedInstanceState)
        LayoutStore.load(this)
        apps = Apps(this)
        widgets = Widgets(this)
        icons = Icons(this)
        usage = Usage(this)
        setContent {
            val settings by Prefs.settings(this).collectAsStateWithLifecycle()
            LessTheme(settings.colourway) { Root(homePresses.intValue) }
        }
    }

    override fun onStart() {
        super.onStart()
        apps.start()
        widgets.startListening()
    }

    override fun onStop() {
        widgets.stopListening()
        apps.stop()
        super.onStop()
    }

    override fun onNewIntent(intent: Intent) {
        super.onNewIntent(intent)
        homePresses.intValue++
    }

    @Deprecated("The widget host hands its set-up result back this way; there is no newer path for it.")
    override fun onActivityResult(requestCode: Int, resultCode: Int, data: Intent?) {
        @Suppress("DEPRECATION")
        super.onActivityResult(requestCode, resultCode, data)
        if (requestCode == Widgets.REQUEST_CONFIGURE) {
            val id = data?.getIntExtra(AppWidgetManager.EXTRA_APPWIDGET_ID, -1) ?: -1
            configured.value = id to (resultCode == Activity.RESULT_OK)
        }
    }
}

private class Menu(val target: Target, val anchor: Rect, val place: Place?)

@Composable
private fun Root(homePresses: Int) {
    val c = LocalContext.current
    val activity = c as MainActivity
    val apps = activity.apps
    val widgets = activity.widgets
    val icons = activity.icons
    val usage = activity.usage
    val scope = rememberCoroutineScope()
    val config = LocalConfiguration.current
    val density = LocalDensity.current

    val settings by Prefs.settings(c).collectAsStateWithLifecycle()
    val layout by LayoutStore.flow.collectAsStateWithLifecycle()
    val all by apps.entries.collectAsStateWithLifecycle()
    var now by remember { mutableStateOf(LocalDateTime.now()) }
    var battery by remember { mutableStateOf(Battery()) }
    var drawerOpen by remember { mutableStateOf(false) }
    var settingsOpen by remember { mutableStateOf(false) }
    var menu by remember { mutableStateOf<Menu?>(null) }
    var panel by remember { mutableStateOf(PanelData()) }
    var rows by remember { mutableIntStateOf(4) }
    var updateLabel by remember { mutableStateOf("Check for update") }
    var found by remember { mutableStateOf<Updater.Release?>(null) }
    var pendingWidget by remember { mutableStateOf<Pair<Int, AppWidgetProviderInfo>?>(null) }
    val hour24 = remember { DateFormat.is24HourFormat(c) }
    val cellDp = (config.screenWidthDp - 56) / COLUMNS.toFloat()

    // The apps as the drawer shows them: hidden ones out, renamed ones renamed, then sorted again.
    val entries = remember(all, settings.hidden, settings.names) {
        Index.sorted(all.filter { it.key !in settings.hidden }.map { e -> e.copy(label = settings.names[e.key] ?: e.label) })
    }
    val isHome = remember(homePresses, settingsOpen) {
        c.getSystemService(RoleManager::class.java)?.isRoleHeld(RoleManager.ROLE_HOME) == true
    }

    // The clock ticks on the minute.
    LaunchedEffect(Unit) {
        while (true) {
            now = LocalDateTime.now()
            delay(60_000L - (System.currentTimeMillis() % 60_000L))
        }
    }
    // The battery as the system reports it; the sticky broadcast answers at once.
    DisposableEffect(Unit) {
        val receiver = object : BroadcastReceiver() {
            override fun onReceive(ctx: Context, i: Intent) {
                val level = i.getIntExtra(BatteryManager.EXTRA_LEVEL, -1)
                val scale = i.getIntExtra(BatteryManager.EXTRA_SCALE, 100)
                val status = i.getIntExtra(BatteryManager.EXTRA_STATUS, -1)
                val charging = status == BatteryManager.BATTERY_STATUS_CHARGING || status == BatteryManager.BATTERY_STATUS_FULL
                if (level >= 0) battery = Battery(level * 100 / scale.coerceAtLeast(1), charging)
            }
        }
        c.registerReceiver(receiver, IntentFilter(Intent.ACTION_BATTERY_CHANGED))
        onDispose { runCatching { c.unregisterReceiver(receiver) } }
    }
    // The home button closes everything and returns to the first page.
    LaunchedEffect(homePresses) {
        menu = null
        settingsOpen = false
        drawerOpen = false
    }
    // First run: the phone's own basics in the dock and on the first page, so the home is not empty.
    LaunchedEffect(all) {
        if (all.isEmpty()) return@LaunchedEffect
        if (!settings.seeded) {
            val basics = apps.basics()
            var l = Layout(dock = basics.take(4).map { Cell.App(it) })
            basics.drop(4).forEach { l = l.add(Cell.App(it), rows) }
            LayoutStore.update(c) { l }
            Prefs.update(c) { it.copy(seeded = true) }
        }
        // Apps that have gone leave the home with them.
        val gone = layout.keys().filter { !apps.exists(it) }
        if (gone.isNotEmpty()) LayoutStore.update(c) { lay -> gone.fold(lay) { acc, k -> acc.removeApp(k) }.pruned() }
    }
    // A widget whose set-up finished is placed; one that was abandoned is forgotten.
    LaunchedEffect(activity.configured.value) {
        val (id, ok) = activity.configured.value ?: return@LaunchedEffect
        activity.configured.value = null
        val pending = pendingWidget
        pendingWidget = null
        if (ok) {
            val info = widgets.info(id) ?: pending?.second ?: return@LaunchedEffect
            val (w, h) = widgets.span(info, cellDp)
            LayoutStore.update(c) { it.add(Cell.Widget(id, info.provider.flattenToString()), rows, w, h) }
        } else widgets.delete(id)
    }

    val bindLauncher = rememberLauncherForActivityResult(ActivityResultContracts.StartActivityForResult()) { result ->
        val (id, info) = pendingWidget ?: return@rememberLauncherForActivityResult
        if (result.resultCode == Activity.RESULT_OK) {
            if (widgets.needsConfigure(info)) widgets.configure(id)
            else {
                pendingWidget = null
                val (w, h) = widgets.span(info, cellDp)
                LayoutStore.update(c) { it.add(Cell.Widget(id, info.provider.flattenToString()), rows, w, h) }
            }
        } else {
            widgets.delete(id)
            pendingWidget = null
        }
    }
    val roleLauncher = rememberLauncherForActivityResult(ActivityResultContracts.StartActivityForResult()) { }

    fun addWidget(info: AppWidgetProviderInfo) {
        val id = widgets.allocate()
        pendingWidget = id to info
        if (widgets.bind(id, info)) {
            if (widgets.needsConfigure(info)) widgets.configure(id)
            else {
                pendingWidget = null
                val (w, h) = widgets.span(info, cellDp)
                LayoutStore.update(c) { it.add(Cell.Widget(id, info.provider.flattenToString()), rows, w, h) }
            }
        } else bindLauncher.launch(widgets.bindIntent(id, info))
    }

    fun refreshPanel() {
        val byPackage = entries.groupBy { apps.packageOf(it.key) ?: "" }
        fun keysFor(packages: List<String>) = packages.mapNotNull { byPackage[it]?.firstOrNull()?.key }.take(10)
        val allowed = usage.allowed()
        panel = PanelData(
            frequent = if (allowed) keysFor(usage.frequent(40)) else emptyList(),
            recent = if (allowed) keysFor(usage.recent(40)) else emptyList(),
            newest = keysFor(usage.newest(10, byPackage.keys.filter { it.isNotEmpty() })),
            usageAllowed = allowed
        )
    }

    fun expandNotifications() {
        runCatching {
            val sb = c.getSystemService("statusbar")
            sb.javaClass.getMethod("expandNotificationsPanel").invoke(sb)
        }
    }

    val icon: @Composable (String, Dp) -> Unit = { key, size ->
        val res = icons.of(key, settings.icons)
        if (res != null) Image(painterResource(res), null, Modifier.size(size))
        else {
            val info = apps.info(key)
            if (info != null) {
                val px = with(density) { size.roundToPx() }
                Image(Stock.of(info, px), null, Modifier.size(size))
            } else Box(Modifier.size(size))
        }
    }

    BackHandler(enabled = menu != null || settingsOpen || drawerOpen) {
        when {
            menu != null -> menu = null
            settingsOpen -> settingsOpen = false
            else -> drawerOpen = false
        }
    }

    if (settingsOpen) {
        SettingsScreen(
            settings = settings,
            isHome = isHome,
            hiddenLabels = settings.hidden.associateWith { apps.label(it) },
            updateLabel = updateLabel,
            version = Updater.currentVersionName(c),
            on = SettingsEvents(
                colourway = { cw -> Prefs.update(c) { it.copy(colourway = cw) } },
                makeHome = {
                    val rm = c.getSystemService(RoleManager::class.java)
                    if (rm != null && rm.isRoleAvailable(RoleManager.ROLE_HOME)) roleLauncher.launch(rm.createRequestRoleIntent(RoleManager.ROLE_HOME))
                },
                unhide = { key -> Prefs.update(c) { it.copy(hidden = it.hidden - key) } },
                update = {
                    scope.launch {
                        val release = found
                        if (release == null) {
                            updateLabel = "Checking"
                            val latest = Updater.latest()
                            when {
                                latest == null -> updateLabel = "Could not check"
                                latest.versionCode > Updater.currentVersionCode(c) -> { found = latest; updateLabel = "Install ${latest.name}" }
                                else -> updateLabel = "Up to date"
                            }
                        } else if (Updater.needsInstallPermission(c)) {
                            Updater.openInstallPermissionSettings(c)
                        } else {
                            updateLabel = "Downloading"
                            val apk = Updater.download(c, release)
                            if (apk == null) updateLabel = "Download failed" else {
                                updateLabel = "Install ${release.name}"
                                Updater.install(c, apk)
                            }
                        }
                    }
                },
                back = { settingsOpen = false }
            )
        )
        return
    }

    Home(
        layout = layout,
        now = now,
        hour24 = hour24,
        battery = battery,
        icon = icon,
        widget = { w, wDp, hDp, onHold, onMove, onRelease ->
            val info = widgets.info(w.id)
            if (info != null) WidgetSlot(widgets, w.id, info, wDp, hDp, onHold, onMove, onRelease)
            else Box(Modifier.size(wDp, hDp))
        },
        reset = homePresses,
        on = HomeEvents(
            launch = { key, bounds -> apps.launch(key, bounds) },
            hold = { cell, place, rect ->
                val target = when (cell) {
                    is Cell.App -> Target.App(cell.key, onHome = true)
                    is Cell.Deck -> Target.Deck(cell.keys)
                    is Cell.Widget -> Target.Widget(cell.id)
                }
                menu = Menu(target, rect, place)
            },
            arranged = { next -> LayoutStore.update(c) { next } },
            drawer = { drawerOpen = true },
            notifications = { expandNotifications() },
            settings = { settingsOpen = true },
            measured = { rows = it }
        )
    )

    Drawer(
        open = drawerOpen,
        setOpen = { drawerOpen = it },
        entries = entries,
        panel = panel,
        refreshPanel = { refreshPanel() },
        askUsage = { usage.ask() },
        icon = icon,
        launch = { key, bounds -> apps.launch(key, bounds) },
        hold = { key, rect -> menu = Menu(Target.App(key, onHome = key in layout.keys()), rect, null) },
        reset = homePresses
    )

    val m = menu
    if (m != null) {
        val key = (m.target as? Target.App)?.key
        val shortcuts = remember(m) { if (key != null) apps.shortcuts(key) else emptyList() }
        val providers = remember(m) { key?.let { apps.packageOf(it) }?.let { widgets.providersOf(it) }.orEmpty() }
        HoldMenu(
            target = m.target,
            anchor = m.anchor,
            label = when (val t = m.target) {
                is Target.App -> settings.names[t.key] ?: apps.label(t.key)
                is Target.Deck -> "Deck"
                is Target.Widget -> widgets.info(t.id)?.let { widgets.label(it) } ?: "Widget"
            },
            shortcuts = shortcuts,
            shortcutIcon = { s -> apps.shortcutIcon(s, config.densityDpi)?.toBitmap(96, 96)?.asImageBitmap() },
            widgets = providers,
            widgetLabel = { widgets.label(it) },
            drawn = icons.drawn,
            on = MenuEvents(
                shortcut = { s -> apps.launchShortcut(s, m.anchor) },
                appInfo = { apps.appInfo(it) },
                notifications = { apps.notificationSettings(it) },
                playStore = { apps.playStore(it) },
                uninstall = { apps.uninstall(it) },
                rename = { k, name -> Prefs.update(c) { it.copy(names = if (name.isBlank()) it.names - k else it.names + (k to name)) } },
                changeIcon = { k, name -> Prefs.update(c) { it.copy(icons = if (name == null) it.icons - k else it.icons + (k to name)) } },
                hide = { k ->
                    Prefs.update(c) { it.copy(hidden = it.hidden + k) }
                    LayoutStore.update(c) { it.removeApp(k).pruned() }
                },
                addToHome = { k -> LayoutStore.update(c) { it.add(Cell.App(k), rows) }; drawerOpen = false },
                removeFromHome = { k -> LayoutStore.update(c) { it.removeApp(k).pruned() } },
                addWidget = { info -> addWidget(info); drawerOpen = false },
                takeApart = {
                    (m.place as? Place.OnPage)?.let { pl -> LayoutStore.update(c) { it.takeApart(pl.item, rows) } }
                    (m.place as? Place.InDock)?.let { pl ->
                        LayoutStore.update(c) { lay ->
                            val deck = lay.dock.getOrNull(pl.index) as? Cell.Deck ?: return@update lay
                            var l = lay.removeDock(pl.index)
                            deck.keys.forEach { k -> l = l.add(Cell.App(k), rows) }
                            l
                        }
                    }
                },
                removeDeck = {
                    (m.place as? Place.OnPage)?.let { pl -> LayoutStore.update(c) { it.remove(pl.item).pruned() } }
                    (m.place as? Place.InDock)?.let { pl -> LayoutStore.update(c) { it.removeDock(pl.index) } }
                },
                removeWidget = { id ->
                    LayoutStore.update(c) { lay ->
                        Layout(lay.pages.map { pg -> Page(pg.items.filter { (it.cell as? Cell.Widget)?.id != id }) }, lay.dock).pruned()
                    }
                    widgets.delete(id)
                },
                close = { menu = null }
            )
        )
    }

    LaunchedEffect(Unit) {
        if (!c.getSystemService(RoleManager::class.java).isRoleHeld(RoleManager.ROLE_HOME)) {
            Toast.makeText(c, "Hold the ground for settings, and make Less the home there.", Toast.LENGTH_LONG).show()
        }
    }
}

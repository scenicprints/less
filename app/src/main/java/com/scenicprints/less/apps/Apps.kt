package com.scenicprints.less.apps

import android.content.ComponentName
import android.content.Context
import android.content.Intent
import android.content.pm.LauncherActivityInfo
import android.content.pm.LauncherApps
import android.content.pm.ShortcutInfo
import android.graphics.Rect
import android.net.Uri
import android.os.Process
import android.os.UserHandle
import android.provider.Settings
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow

/**
 * Every app the phone can launch, kept current by [LauncherApps] callbacks rather than by polling.
 * Only the owner's apps: a work profile is a different device and does not belong on this one.
 */
class Apps(private val c: Context) {

    private val launcher = c.getSystemService(LauncherApps::class.java)
    private val user: UserHandle = Process.myUserHandle()
    private val infos = MutableStateFlow<Map<String, LauncherActivityInfo>>(emptyMap())
    private val _entries = MutableStateFlow<List<Entry>>(emptyList())
    /** Every launchable app, sorted, before hiding and renaming. */
    val entries: StateFlow<List<Entry>> = _entries

    private val callback = object : LauncherApps.Callback() {
        override fun onPackageRemoved(p: String, u: UserHandle) = reload()
        override fun onPackageAdded(p: String, u: UserHandle) = reload()
        override fun onPackageChanged(p: String, u: UserHandle) = reload()
        override fun onPackagesAvailable(p: Array<out String>, u: UserHandle, r: Boolean) = reload()
        override fun onPackagesUnavailable(p: Array<out String>, u: UserHandle, r: Boolean) = reload()
    }

    fun start() {
        launcher.registerCallback(callback)
        reload()
    }

    fun stop() = launcher.unregisterCallback(callback)

    fun reload() {
        Stock.clear()
        val list = launcher.getActivityList(null, user).filter { it.componentName.packageName != c.packageName }
        infos.value = list.associateBy { it.componentName.flattenToString() }
        _entries.value = Index.sorted(list.map { Entry(it.componentName.flattenToString(), it.label.toString()) })
    }

    fun info(key: String): LauncherActivityInfo? = infos.value[key]

    fun exists(key: String): Boolean = infos.value.containsKey(key)

    fun label(key: String): String = infos.value[key]?.label?.toString() ?: key.substringAfterLast('.')

    fun packageOf(key: String): String? = ComponentName.unflattenFromString(key)?.packageName

    /** Opens the app with the tapped icon as the source, so the system animates out of it and back. */
    fun launch(key: String, bounds: Rect?) {
        val component = ComponentName.unflattenFromString(key) ?: return
        runCatching { launcher.startMainActivity(component, user, bounds, null) }
    }

    /** The shortcuts the app itself provides, which only the default home is allowed to see. */
    fun shortcuts(key: String): List<ShortcutInfo> {
        val pkg = packageOf(key) ?: return emptyList()
        if (!launcher.hasShortcutHostPermission()) return emptyList()
        val q = LauncherApps.ShortcutQuery()
            .setPackage(pkg)
            .setQueryFlags(
                LauncherApps.ShortcutQuery.FLAG_MATCH_MANIFEST or
                    LauncherApps.ShortcutQuery.FLAG_MATCH_DYNAMIC or
                    LauncherApps.ShortcutQuery.FLAG_MATCH_PINNED
            )
        return runCatching { launcher.getShortcuts(q, user).orEmpty() }.getOrDefault(emptyList())
            .filter { it.isEnabled }
            .sortedBy { it.rank }
            .take(4)
    }

    fun launchShortcut(s: ShortcutInfo, bounds: Rect?) {
        runCatching { launcher.startShortcut(s, bounds, null) }
    }

    fun shortcutIcon(s: ShortcutInfo, density: Int) = runCatching { launcher.getShortcutIconDrawable(s, density) }.getOrNull()

    fun appInfo(key: String) {
        val component = ComponentName.unflattenFromString(key) ?: return
        runCatching { launcher.startAppDetailsActivity(component, user, null, null) }
    }

    fun notificationSettings(key: String) {
        val pkg = packageOf(key) ?: return
        val i = Intent(Settings.ACTION_APP_NOTIFICATION_SETTINGS)
            .putExtra(Settings.EXTRA_APP_PACKAGE, pkg)
            .addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
        runCatching { c.startActivity(i) }
    }

    fun playStore(key: String) {
        val pkg = packageOf(key) ?: return
        val i = Intent(Intent.ACTION_VIEW, Uri.parse("https://play.google.com/store/apps/details?id=$pkg"))
            .addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
        runCatching { c.startActivity(i) }
    }

    fun uninstall(key: String) {
        val pkg = packageOf(key) ?: return
        val i = Intent(Intent.ACTION_DELETE, Uri.parse("package:$pkg")).addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
        runCatching { c.startActivity(i) }
    }

    /** The phone's own basics, resolved by what they do, for the first home before anything is arranged. */
    fun basics(): List<String> {
        val pm = c.packageManager
        fun resolve(i: Intent): String? = pm.resolveActivity(i, 0)?.activityInfo?.packageName
        val packages = listOfNotNull(
            resolve(Intent(Intent.ACTION_DIAL)),
            resolve(Intent.makeMainSelectorActivity(Intent.ACTION_MAIN, Intent.CATEGORY_APP_MESSAGING)),
            resolve(Intent.makeMainSelectorActivity(Intent.ACTION_MAIN, Intent.CATEGORY_APP_BROWSER)),
            resolve(Intent("android.media.action.STILL_IMAGE_CAMERA")),
            resolve(Intent.makeMainSelectorActivity(Intent.ACTION_MAIN, Intent.CATEGORY_APP_EMAIL)),
            resolve(Intent.makeMainSelectorActivity(Intent.ACTION_MAIN, Intent.CATEGORY_APP_MAPS)),
            resolve(Intent.makeMainSelectorActivity(Intent.ACTION_MAIN, Intent.CATEGORY_APP_CALENDAR)),
            resolve(Intent.makeMainSelectorActivity(Intent.ACTION_MAIN, Intent.CATEGORY_APP_GALLERY)),
            resolve(Intent.makeMainSelectorActivity(Intent.ACTION_MAIN, Intent.CATEGORY_APP_MUSIC))
        )
        val byPackage = infos.value.values.groupBy { it.componentName.packageName }
        return packages.mapNotNull { byPackage[it]?.firstOrNull()?.componentName?.flattenToString() }.distinct()
    }
}

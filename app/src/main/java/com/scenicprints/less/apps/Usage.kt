package com.scenicprints.less.apps

import android.app.AppOpsManager
import android.app.usage.UsageStatsManager
import android.content.Context
import android.content.Intent
import android.content.pm.PackageManager
import android.os.Process
import android.provider.Settings

/**
 * What the search panel shows before a letter is typed: the apps opened most, the apps opened
 * last, and the apps installed last. The first two need usage access, which Android grants once
 * from its own settings page; until then those rows are simply empty.
 */
class Usage(private val c: Context) {

    fun allowed(): Boolean {
        val ops = c.getSystemService(AppOpsManager::class.java)
        val mode = ops.unsafeCheckOpNoThrow(AppOpsManager.OPSTR_GET_USAGE_STATS, Process.myUid(), c.packageName)
        return mode == AppOpsManager.MODE_ALLOWED
    }

    fun ask() {
        runCatching { c.startActivity(Intent(Settings.ACTION_USAGE_ACCESS_SETTINGS).addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)) }
    }

    /** Packages by time in the foreground over the last two weeks, most first. */
    fun frequent(limit: Int): List<String> = stats(14) { it.totalTimeInForeground }.take(limit)

    /** Packages by when they were last in front, latest first. */
    fun recent(limit: Int): List<String> = stats(14) { it.lastTimeUsed }.take(limit)

    /** Packages by install date, newest first. Needs no permission. */
    fun newest(limit: Int, candidates: Collection<String>): List<String> {
        val pm = c.packageManager
        return candidates
            .mapNotNull { pkg -> runCatching { pkg to pm.getPackageInfo(pkg, 0).firstInstallTime }.getOrNull() }
            .sortedByDescending { it.second }
            .map { it.first }
            .take(limit)
    }

    private fun stats(days: Int, by: (android.app.usage.UsageStats) -> Long): List<String> {
        if (!allowed()) return emptyList()
        val m = c.getSystemService(UsageStatsManager::class.java)
        val now = System.currentTimeMillis()
        val list = runCatching {
            m.queryUsageStats(UsageStatsManager.INTERVAL_BEST, now - days * 24L * 3600_000, now)
        }.getOrNull().orEmpty()
        return list
            .filter { it.packageName != c.packageName && by(it) > 0 }
            .groupBy { it.packageName }
            .mapValues { (_, v) -> v.maxOf(by) }
            .entries.sortedByDescending { it.value }
            .map { it.key }
    }

    companion object {
        fun installed(pm: PackageManager, pkg: String): Boolean = runCatching { pm.getPackageInfo(pkg, 0); true }.getOrDefault(false)
    }
}

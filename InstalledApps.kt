package com.calcvault.app

import android.content.Context
import android.content.pm.ApplicationInfo
import android.content.pm.PackageManager
import android.graphics.drawable.Drawable

object InstalledApps {
    data class AppInfo(val label: String, val packageName: String, val icon: Drawable?)

    fun list(ctx: Context): List<AppInfo> {
        val pm = ctx.packageManager
        val apps = pm.getInstalledApplications(PackageManager.GET_META_DATA)
        return apps
            .filter { shouldShow(it, pm) }
            .map { AppInfo(it.loadLabel(pm).toString(), it.packageName, try { it.loadIcon(pm) } catch (_: Exception) { null }) }
            .sortedBy { it.label.lowercase() }
    }

    private fun shouldShow(info: ApplicationInfo, pm: PackageManager): Boolean {
        // Skip our own app and pure system apps without a launcher entry.
        if (info.packageName == "com.calcvault.app") return false
        val intent = pm.getLaunchIntentForPackage(info.packageName)
        return intent != null && (info.flags and ApplicationInfo.FLAG_SYSTEM) == 0 || intent != null
    }
}

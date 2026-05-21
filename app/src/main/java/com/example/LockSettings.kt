package com.example

import android.content.Context
import android.content.Intent
import android.content.pm.PackageManager

object LockSettings {
    private const val PREFS_NAME = "zenlock_prefs"
    private const val KEY_BLOCKED_APPS = "blocked_apps"
    private const val KEY_LOCKDOWN_END = "lockdown_end"

    fun isLockdownActive(context: Context): Boolean {
        val prefs = context.getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE)
        val endMillis = prefs.getLong(KEY_LOCKDOWN_END, 0L)
        return System.currentTimeMillis() < endMillis
    }

    fun setLockdown(context: Context, active: Boolean, durationSecs: Int = 0) {
        val prefs = context.getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE)
        val endMillis = if (active) System.currentTimeMillis() + (durationSecs * 1000L) else 0L
        prefs.edit().putLong(KEY_LOCKDOWN_END, endMillis).apply()
    }

    fun getLockdownEndTime(context: Context): Long {
        val prefs = context.getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE)
        return prefs.getLong(KEY_LOCKDOWN_END, 0L)
    }

    fun setBlockedApps(context: Context, packageNames: Set<String>) {
        val prefs = context.getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE)
        prefs.edit().putStringSet(KEY_BLOCKED_APPS, packageNames).apply()
    }

    fun getBlockedApps(context: Context): Set<String> {
        val prefs = context.getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE)
        return prefs.getStringSet(KEY_BLOCKED_APPS, emptySet()) ?: emptySet()
    }

    // Safely retrieves the current active system launcher (home app) package name.
    fun getHomeLauncherPackage(context: Context): String? {
        return try {
            val intent = Intent(Intent.ACTION_MAIN).apply {
                addCategory(Intent.CATEGORY_HOME)
            }
            val resolveInfo = context.packageManager.resolveActivity(intent, PackageManager.MATCH_DEFAULT_ONLY)
            resolveInfo?.activityInfo?.packageName
        } catch (e: Exception) {
            e.printStackTrace()
            null
        }
    }

    fun isAppBlocked(context: Context, packageName: String): Boolean {
        // 1. Safety Exception: Protect our own package
        if (packageName == context.packageName) return false
        
        // 2. Safety Exception: Protect core Android system services and System UI (notification shade, etc.)
        if (packageName == "android" || packageName == "com.android.systemui") return false
        
        // 3. Safety Exception: Protect the current default Home/Launcher application
        val homeLauncher = getHomeLauncherPackage(context)
        if (packageName == homeLauncher) return false
        
        val blocked = getBlockedApps(context)
        return blocked.contains(packageName)
    }
}

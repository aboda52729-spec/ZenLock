package com.example

import android.content.Context
import android.content.Intent
import android.content.pm.PackageManager

object LockSettings {
    private const val PREFS_NAME = "zenlock_prefs"
    private const val KEY_BLOCKED_APPS = "blocked_apps"
    private const val KEY_LOCKDOWN_END = "lockdown_end"
    private const val KEY_STEALTH_MODE_ENABLED = "stealth_mode_enabled"
    private const val KEY_ANTI_PORN_SHIELD_ENABLED = "anti_porn_shield_enabled"
    private const val KEY_IS_DARK_THEME = "is_dark_theme"
    private const val KEY_UNINSTALL_PROTECTION_END = "uninstall_protection_end"

    fun isDarkThemePreferred(context: Context): Boolean? {
        val prefs = context.getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE)
        return if (prefs.contains(KEY_IS_DARK_THEME)) {
            prefs.getBoolean(KEY_IS_DARK_THEME, false)
        } else {
            null
        }
    }

    fun setDarkThemePreferred(context: Context, isDark: Boolean) {
        val prefs = context.getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE)
        prefs.edit().putBoolean(KEY_IS_DARK_THEME, isDark).apply()
    }

    fun getUninstallProtectionEndTime(context: Context): Long {
        val prefs = context.getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE)
        return prefs.getLong(KEY_UNINSTALL_PROTECTION_END, 0L)
    }

    fun setUninstallProtectionDays(context: Context, days: Int) {
        val prefs = context.getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE)
        val endMillis = System.currentTimeMillis() + (days * 24L * 60L * 60L * 1000L)
        prefs.edit().putLong(KEY_UNINSTALL_PROTECTION_END, endMillis).apply()
    }

    fun isUninstallProtectionActive(context: Context): Boolean {
        val endMillis = getUninstallProtectionEndTime(context)
        return System.currentTimeMillis() < endMillis
    }

    fun isStealthModeEnabled(context: Context): Boolean {
        val prefs = context.getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE)
        return prefs.getBoolean(KEY_STEALTH_MODE_ENABLED, false)
    }

    fun setStealthModeEnabled(context: Context, enabled: Boolean) {
        val prefs = context.getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE)
        prefs.edit().putBoolean(KEY_STEALTH_MODE_ENABLED, enabled).apply()
    }

    fun isAntiPornShieldEnabled(context: Context): Boolean {
        val prefs = context.getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE)
        return prefs.getBoolean(KEY_ANTI_PORN_SHIELD_ENABLED, false)
    }

    fun setAntiPornShieldEnabled(context: Context, enabled: Boolean) {
        val prefs = context.getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE)
        prefs.edit().putBoolean(KEY_ANTI_PORN_SHIELD_ENABLED, enabled).apply()
    }

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

    fun getSavedPresets(context: Context): List<Pair<String, Int>> {
        val prefs = context.getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE)
        val saved = prefs.getString("custom_presets_v2", null)
        if (saved == null) {
            return listOf(
                Pair("بومودورو (25د)", 25 * 60),
                Pair("تركيز عميق (1س)", 60 * 60),
                Pair("فحص تركيز (10ث)", 10),
                Pair("انطلاقة (5د)", 5 * 60)
            )
        }
        return try {
            saved.split(";").filter { it.isNotEmpty() }.map {
                val parts = it.split(",")
                Pair(parts[0], parts[1].toInt())
            }
        } catch(e: Exception) {
            listOf(
                Pair("بومودورو (25د)", 25 * 60),
                Pair("تركيز عميق (1س)", 60 * 60),
                Pair("فحص تركيز (10ث)", 10),
                Pair("انطلاقة (5د)", 5 * 60)
            )
        }
    }

    fun savePreset(context: Context, name: String, durationSecs: Int) {
        val prefs = context.getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE)
        val current = getSavedPresets(context).toMutableList()
        if (current.none { it.second == durationSecs }) {
            current.add(Pair(name, durationSecs))
            val serialized = current.joinToString(";") { "${it.first},${it.second}" }
            prefs.edit().putString("custom_presets_v2", serialized).apply()
        }
    }

    fun deletePreset(context: Context, index: Int) {
        val prefs = context.getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE)
        val current = getSavedPresets(context).toMutableList()
        if (index >= 0 && index < current.size) {
            current.removeAt(index)
            val serialized = current.joinToString(";") { "${it.first},${it.second}" }
            prefs.edit().putString("custom_presets_v2", serialized).apply()
        }
    }

    fun getSelectedLanguage(context: Context): String {
        val prefs = context.getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE)
        var lang = prefs.getString("selected_language", "") ?: ""
        if (lang.isEmpty()) {
            val systemLanguage = java.util.Locale.getDefault().language
            lang = if (listOf("ar", "en", "es", "fr", "hi").contains(systemLanguage)) {
                systemLanguage
            } else {
                "en"
            }
            prefs.edit().putString("selected_language", lang).apply()
        }
        return lang
    }

    fun setSelectedLanguage(context: Context, lang: String) {
        val prefs = context.getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE)
        prefs.edit().putString("selected_language", lang).apply()
    }
}

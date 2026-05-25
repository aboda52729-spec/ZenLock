package com.example

import android.accessibilityservice.AccessibilityService
import android.content.Context
import android.content.Intent
import android.net.ConnectivityManager
import android.net.NetworkCapabilities
import android.os.Handler
import android.os.Looper
import android.view.accessibility.AccessibilityEvent
import android.view.accessibility.AccessibilityNodeInfo
import android.widget.Toast

class FocusAccessibilityService : AccessibilityService() {

    private var lastBlockTime = 0L
    private var currentToast: Toast? = null

    override fun onAccessibilityEvent(event: AccessibilityEvent) {
        try {
            val eventType = event.eventType
            if (eventType == AccessibilityEvent.TYPE_WINDOW_STATE_CHANGED || 
                eventType == AccessibilityEvent.TYPE_WINDOW_CONTENT_CHANGED) {
                
                val packageName = event.packageName?.toString() ?: return
                
                // Fast check to avoid heavy processing on every content change our own package
                if (packageName == "com.example" || packageName == "com.android.systemui") return
                
                // Check if lockdown session is active
                val lockdownActive = LockSettings.isLockdownActive(this)
                if (lockdownActive) {
                    // Prevent uninstalling or deactivating ZenLock during an active focus/lockdown session
                    if (packageName == "com.android.settings") {
                        val rootNode = rootInActiveWindow
                        if (rootNode != null && isTryingToUninstallOrDeactivate(rootNode)) {
                            val lang = LockSettings.getSelectedLanguage(this)
                            val blockMsg = if (lang == "ar") {
                                "🛡️ لا يمكنك إلغاء تثبيت التطبيق أو إلغاء تنشيطه أثناء جلسة التركيز النشطة!"
                            } else if (lang == "es") {
                                "🛡️ ¡No puedes desinstalar o desactivar la aplicación durante una sesión de enfoque activa!"
                            } else if (lang == "fr") {
                                "🛡️ Vous ne pouvez pas désinstaller ou désactiver l'application pendant une session de mise au point active !"
                            } else {
                                "🛡️ You cannot uninstall or deactivate the app during an active focus session!"
                            }
                            triggerBlock("com.example", true, blockMsg)
                            return
                        }
                    }

                    val antiPornEnabled = LockSettings.isAntiPornShieldEnabled(this)
                    
                    if (antiPornEnabled) {
                        // 1. Shield VPN and Proxy dialogs / applications
                        if (packageName == "com.android.vpndialogs" || 
                            packageName.contains("vpn", ignoreCase = true) || 
                            packageName.contains("proxy", ignoreCase = true) || 
                            packageName.contains("psiphon", ignoreCase = true) || 
                            packageName.contains("torbrowser", ignoreCase = true) || 
                            packageName.contains("orbot", ignoreCase = true)
                        ) {
                            triggerBlock("adult_content_blocked_shield", true, "🛡️ درع التعافي يمنع تشغيل تطبيقات الـ VPN لمنع الالتفاف!")
                            return
                        }
                        
                        // 3. Detect active system-wide VPN transport
                        if (isVpnActive()) {
                            triggerBlock("adult_content_blocked_shield", true, "🛡️ تم كشف اتصال VPN نشط! تم حظره فوراً لحمايتك.")
                            return
                        }
                        
                        // 4. Inspect browser content or input text for adult-related search phrases or websites
                        val rootNode = rootInActiveWindow
                        if (rootNode != null && hasAdultKeywords(rootNode)) {
                            triggerBlock("adult_content_blocked_shield", false, "🛡️ تم حجب المحتوى تلقائياً بواسطة درع التعافي الذكي لـ ZenLock!")
                            return
                        }
                    }
                    
                    // 5. Existing lockdown logic: block targeted distracting apps
                    if (LockSettings.isAppBlocked(this, packageName)) {
                        triggerBlock(packageName, false, "🛡️ وضع التركيز نشط حالياً. تم تقييد الوصول لضمان إنتاجيتك.")
                        return
                    }
                }
            }
        } catch (e: Exception) {
            e.printStackTrace()
        }
    }

    private fun triggerBlock(pkgToReport: String, isHomeForceDouble: Boolean, toastMessage: String) {
        val now = System.currentTimeMillis()
        if (now - lastBlockTime < 3000L) {
            // Rate limit blocks to prevent endless UI loops or redundant intent launches
            return
        }
        lastBlockTime = now

        // 1. Force back to Home screen
        performGlobalAction(GLOBAL_ACTION_HOME)
        if (isHomeForceDouble) {
            performGlobalAction(GLOBAL_ACTION_RECENTS)
            performGlobalAction(GLOBAL_ACTION_HOME)
        }
        
        // 2. Show alert toast on main thread with automatic cancellation of previous toasts
        Handler(Looper.getMainLooper()).post {
            try {
                currentToast?.cancel()
                val toast = Toast.makeText(this, toastMessage, Toast.LENGTH_SHORT)
                currentToast = toast
                toast.show()
            } catch (e: Exception) {
                e.printStackTrace()
            }
        }

        // 3. Bring ZenLock back to foreground as the primary enforcement barrier
        val mainIntent = Intent(this, MainActivity::class.java).apply {
            addFlags(Intent.FLAG_ACTIVITY_NEW_TASK or Intent.FLAG_ACTIVITY_SINGLE_TOP or Intent.FLAG_ACTIVITY_CLEAR_TOP)
            putExtra("FROM_BLOCK_TRIGGER", true)
            putExtra("BLOCKED_APP_NAME", pkgToReport)
        }
        startActivity(mainIntent)
    }

    private fun isVpnActive(): Boolean {
        return try {
            val cm = getSystemService(Context.CONNECTIVITY_SERVICE) as? ConnectivityManager
            val activeNetwork = cm?.activeNetwork ?: return false
            val caps = cm.getNetworkCapabilities(activeNetwork) ?: return false
            caps.hasTransport(NetworkCapabilities.TRANSPORT_VPN)
        } catch (e: Exception) {
            false
        }
    }

    private fun hasAdultKeywords(node: AccessibilityNodeInfo?): Boolean {
        if (node == null) return false
        
        // Inspect node text and content descriptions
        val text = node.text?.toString()?.lowercase() ?: ""
        val contentDesc = node.contentDescription?.toString()?.lowercase() ?: ""
        
        if (containsAdultKeywords(text) || containsAdultKeywords(contentDesc)) {
            return true
        }
        
        // Traversal
        val childCount = node.childCount
        for (i in 0 until childCount) {
            val child = node.getChild(i) ?: continue
            val found = hasAdultKeywords(child)
            child.recycle()
            if (found) return true
        }
        return false
    }

    private fun containsAdultKeywords(input: String): Boolean {
        if (input.isEmpty()) return false
        val text = input.trim().lowercase()
        
        // Extended high-performance check of top adult labels and domains
        val keywords = listOf(
            "xvideos", "pornhub", "xnxx", "redtube", "xhamster", "brazzers", "youporn", "stripchat", 
            "chaturbate", "cam4", "eporner", "hqporner", "spankbang", "hentai", "rule34", "porno", "pornography", 
            "sexy", "adultsite", "erotic", "nude", "nudity", "playboy", "onlyfans", "x-rated", "xxx", "sex.com"
        )
        for (kw in keywords) {
            if (text.contains(kw)) return true
        }
        
        // Strict Arabic blocking lists
        val arabicKeywords = listOf(
            "سكس", "اباحي", "أباحي", "إباحي", "بورن", "مواقع كبار", "افلام كبار", "أفلام كبار", 
            "ميا خليفة", "صور عارية", "فيديو عاري", "افلام بورن", "موقع جنس", "موقع اباحي", "سحاق", "لواط",
            "عاري", "عارية", "مؤخرة", "ثدي", "افلام جنس", "أفلام جنس"
        )
        for (akw in arabicKeywords) {
            if (text.contains(akw)) return true
        }
        return false
    }

    private fun isTryingToUninstallOrDeactivate(rootNode: AccessibilityNodeInfo): Boolean {
        val texts = mutableListOf<String>()
        collectAllTexts(rootNode, texts)
        
        var hasZenLock = false
        var hasActionKeyword = false
        
        for (t in texts) {
            val lower = t.lowercase()
            if (lower.contains("zenlock")) {
                hasZenLock = true
            }
            if (lower.contains("uninstall") || 
                lower.contains("deactivate") || 
                lower.contains("force stop") || 
                lower.contains("disable") || 
                lower.contains("turn off") ||
                lower.contains("clear data") ||
                lower.contains("إلغاء التثبيت") || 
                lower.contains("إلغاء تنشيط") || 
                lower.contains("إلغاء التنشيط") || 
                lower.contains("فرض الإيقاف") || 
                lower.contains("إيقاف إجباري") || 
                lower.contains("إلغاء تفعيل") ||
                lower.contains("مسح البيانات") ||
                lower.contains("إيقاف") ||
                lower.contains("إزالة")
            ) {
                hasActionKeyword = true
            }
        }
        
        return hasZenLock && hasActionKeyword
    }

    private fun collectAllTexts(node: AccessibilityNodeInfo?, list: MutableList<String>) {
        if (node == null) return
        node.text?.toString()?.let { list.add(it) }
        node.contentDescription?.toString()?.let { list.add(it) }
        
        val childCount = node.childCount
        for (i in 0 until childCount) {
            val child = node.getChild(i) ?: continue
            collectAllTexts(child, list)
            child.recycle()
        }
    }

    override fun onInterrupt() {}
}

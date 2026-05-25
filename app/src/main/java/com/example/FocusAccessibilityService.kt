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

    override fun onAccessibilityEvent(event: AccessibilityEvent) {
        try {
            val eventType = event.eventType
            if (eventType == AccessibilityEvent.TYPE_WINDOW_STATE_CHANGED || 
                eventType == AccessibilityEvent.TYPE_WINDOW_CONTENT_CHANGED) {
                
                val packageName = event.packageName?.toString() ?: return
                
                // Fast check to avoid heavy processing on every content change our own package
                if (packageName == "com.example" || packageName == "com.android.systemui") return
                
                // Prevent uninstallation if protection is active
                val isProtectionActive = LockSettings.isUninstallProtectionActive(this)
                if (isProtectionActive) {
                    if (packageName == "com.android.settings") {
                        val rootNode = rootInActiveWindow
                        val isUninstallPage = rootNode != null && (hasUninstallKeywords(rootNode))
                        if (isUninstallPage) {
                            val endMillis = LockSettings.getUninstallProtectionEndTime(this)
                            val remain = maxOf(0L, endMillis - System.currentTimeMillis())
                            val remainDays = (remain / (1000 * 60 * 60 * 24)).toInt()
                            triggerBlock("adult_content_blocked_shield", true, "🛡️ حماية إزالة التطبيق نشطة! تنتهي بعد $remainDays يوم.")
                            return
                        }
                    }
                }
                
                // Check if lockdown session is active
                val lockdownActive = LockSettings.isLockdownActive(this)
                if (lockdownActive) {
                    val antiPornEnabled = LockSettings.isAntiPornShieldEnabled(this)
                    
                    if (antiPornEnabled) {
                        // 1. Shield settings (prevent disabling service or settings walkthroughs during lockdown)
                        if (packageName == "com.android.settings") {
                            triggerBlock("adult_content_blocked_shield", true, "🛡️ تم قفل الإعدادات حمايةً لالتزامك حتى انتهاء العداد!")
                            return
                        }
                        
                        // 2. Shield VPN and Proxy dialogs / applications
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
        // 1. Force back to Home screen
        performGlobalAction(GLOBAL_ACTION_HOME)
        if (isHomeForceDouble) {
            performGlobalAction(GLOBAL_ACTION_RECENTS)
            performGlobalAction(GLOBAL_ACTION_HOME)
        }
        
        // 2. Show alert toast on main thread
        Handler(Looper.getMainLooper()).post {
            try {
                Toast.makeText(this, toastMessage, Toast.LENGTH_LONG).show()
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

    private fun hasUninstallKeywords(node: AccessibilityNodeInfo?): Boolean {
        if (node == null) return false
        
        val text = node.text?.toString()?.lowercase() ?: ""
        val contentDesc = node.contentDescription?.toString()?.lowercase() ?: ""
        
        if (text.contains("deactivate") || text.contains("إلغاء التنشيط") || text.contains("إلغاء تنشيط")
           || text.contains("uninstall") || text.contains("إزالة التطبيق") || text.contains("إزالة التثبيت")
           || text.contains("الغاء التثبيت") || contentDesc.contains("deactivate") || contentDesc.contains("uninstall")) {
            
            // Just double checking we are talking about this app (ZenLock) or Device Admin pages
            if (text.contains("zenlock") || text.contains("responsables") || text.contains("device admin") || text.contains("مسؤول الجهاز") || text.contains("مسؤولو الجهاز")) {
                return true
            }
        }
        
        val childCount = node.childCount
        for (i in 0 until childCount) {
            val child = node.getChild(i) ?: continue
            val found = hasUninstallKeywords(child)
            child.recycle()
            if (found) return true
        }
        return false
    }

    override fun onInterrupt() {}
}

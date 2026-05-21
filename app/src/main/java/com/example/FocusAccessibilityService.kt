package com.example

import android.accessibilityservice.AccessibilityService
import android.content.Intent
import android.view.accessibility.AccessibilityEvent
import android.widget.Toast
import android.os.Handler
import android.os.Looper

class FocusAccessibilityService : AccessibilityService() {

    override fun onAccessibilityEvent(event: AccessibilityEvent) {
        try {
            val eventType = event.eventType
            if (eventType == AccessibilityEvent.TYPE_WINDOW_STATE_CHANGED || 
                eventType == AccessibilityEvent.TYPE_WINDOW_CONTENT_CHANGED) {
                
                val packageName = event.packageName?.toString() ?: return
                
                // Fast check to avoid heavy processing on every content change
                if (packageName == "com.example" || packageName == "com.android.systemui") return
                
                // Check if lockdown session is active and check if the package is in the blocked list
                if (LockSettings.isLockdownActive(this) && LockSettings.isAppBlocked(this, packageName)) {
                    
                    // 1. Force screen back to Home
                    performGlobalAction(GLOBAL_ACTION_HOME)
                    
                    // 2. Collapse notification shade if they tried to bypass via notifications
                    performGlobalAction(GLOBAL_ACTION_RECENTS) // optional trick: sometimes opens recents then we close it, but HOME is better.
                    performGlobalAction(GLOBAL_ACTION_HOME) // Double enforce
                    
                    // Show descriptive system toast to inform them it's locked down
                    Handler(Looper.getMainLooper()).post {
                        try {
                            Toast.makeText(
                                this,
                                "ZenLock Shield: Access Denied to $packageName",
                                Toast.LENGTH_SHORT
                            ).show()
                        } catch (e: Exception) {
                            e.printStackTrace()
                        }
                    }

                    // Bring ZenLock back to the foreground to reinforce focus
                    val mainIntent = Intent(this, MainActivity::class.java).apply {
                        addFlags(Intent.FLAG_ACTIVITY_NEW_TASK or Intent.FLAG_ACTIVITY_SINGLE_TOP or Intent.FLAG_ACTIVITY_CLEAR_TOP)
                        putExtra("FROM_BLOCK_TRIGGER", true)
                        putExtra("BLOCKED_APP_NAME", packageName)
                    }
                    startActivity(mainIntent)
                }
            }
        } catch (e: Exception) {
            e.printStackTrace()
        }
    }

    override fun onInterrupt() {}
}

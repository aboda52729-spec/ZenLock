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
            if (event.eventType == AccessibilityEvent.TYPE_WINDOW_STATE_CHANGED) {
                val packageName = event.packageName?.toString() ?: return
                
                // Check if lockdown session is active and check if the package is in the blocked list
                if (LockSettings.isLockdownActive(this) && LockSettings.isAppBlocked(this, packageName)) {
                    // Perform global home action to pull them out of the distraction app
                    performGlobalAction(GLOBAL_ACTION_HOME)
                    
                    // Show descriptive system toast to inform them it's locked down
                    Handler(Looper.getMainLooper()).post {
                        try {
                            Toast.makeText(
                                this,
                                "ZenLock: Application locked. Focus session in progress!",
                                Toast.LENGTH_SHORT
                            ).show()
                        } catch (e: Exception) {
                            e.printStackTrace()
                        }
                    }

                    // Bring ZenLock back to the foreground to reinforce focus
                    val mainIntent = Intent(this, MainActivity::class.java).apply {
                        addFlags(Intent.FLAG_ACTIVITY_NEW_TASK or Intent.FLAG_ACTIVITY_SINGLE_TOP)
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

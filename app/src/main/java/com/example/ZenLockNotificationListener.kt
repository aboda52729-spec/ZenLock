package com.example

import android.service.notification.NotificationListenerService
import android.service.notification.StatusBarNotification
import android.util.Log

class ZenLockNotificationListener : NotificationListenerService() {

    override fun onNotificationPosted(sbn: StatusBarNotification?) {
        super.onNotificationPosted(sbn)
        sbn?.let {
            val packageName = it.packageName
            // Fast check
            val myPkg = this.packageName
            if (packageName == myPkg || packageName == "com.example" || packageName == "com.android.systemui") return
            
            if (LockSettings.isLockdownActive(this) && LockSettings.isAppBlocked(this, packageName)) {
                Log.d("ZenLockNotification", "Blocked notification from: $packageName")
                cancelNotification(it.key)
            }
        }
    }
}

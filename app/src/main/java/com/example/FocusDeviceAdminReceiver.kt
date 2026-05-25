package com.example

import android.app.admin.DeviceAdminReceiver
import android.content.Context
import android.content.Intent
import android.widget.Toast

class FocusDeviceAdminReceiver : DeviceAdminReceiver() {
    override fun onEnabled(context: Context, intent: Intent) {
        super.onEnabled(context, intent)
    }

    override fun onDisableRequested(context: Context, intent: Intent): CharSequence {
        val endMillis = LockSettings.getUninstallProtectionEndTime(context)
        if (System.currentTimeMillis() < endMillis) {
            val remain = endMillis - System.currentTimeMillis()
            val remainDays = (remain / (1000 * 60 * 60 * 24)).toInt()
            val remainHours = ((remain / (1000 * 60 * 60)) % 24).toInt()
            val remainMins = ((remain / (1000 * 60)) % 60).toInt()
            if (remainDays > 0) {
                return "حماية إزالة التطبيق نشطة. باقٍ للانتهاء $remainDays يوم و $remainHours ساعة."
            } else {
                return "حماية إزالة التطبيق نشطة. باقٍ للانتهاء $remainHours ساعة و $remainMins دقيقة."
            }
        }
        return "إيقاف مسؤول الجهاز سيؤدي إلى إضعاف جلسات التركيز الخاصة بك."
    }

    override fun onDisabled(context: Context, intent: Intent) {
        super.onDisabled(context, intent)
    }
}

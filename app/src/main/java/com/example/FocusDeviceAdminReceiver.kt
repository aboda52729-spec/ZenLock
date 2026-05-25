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
            
            val lang = LockSettings.getSelectedLanguage(context)
            if (lang == "ar") {
                return if (remainDays > 0) {
                    "🛡️ حماية إزالة التطبيق نشطة. باقٍ للانتهاء $remainDays يوم و $remainHours ساعة."
                } else {
                    "🛡️ حماية إزالة التطبيق نشطة. باقٍ للانتهاء $remainHours ساعة و $remainMins دقيقة."
                }
            } else if (lang == "es") {
                return if (remainDays > 0) {
                    "🛡️ La protección de desinstalación está activa. Faltan $remainDays días y $remainHours horas."
                } else {
                    "🛡️ La protección de desinstalación está activa. Faltan $remainHours horas y $remainMins minutos."
                }
            } else if (lang == "fr") {
                return if (remainDays > 0) {
                    "🛡️ Protection de désinstallation active. Restant : $remainDays jours et $remainHours heures."
                } else {
                    "🛡️ Protection de désinstallation active. Restant : $remainHours heures et $remainMins minutes."
                }
            } else if (lang == "hi") {
                return if (remainDays > 0) {
                    "🛡️ अनइंस्टॉल सुरक्षा सक्रिय है। $remainDays दिन और $remainHours घंटे शेष हैं।"
                } else {
                    "🛡️ अनइंस्टॉल सुरक्षा सक्रिय है। $remainHours घंटे और $remainMins मिनट शेष हैं।"
                }
            } else {
                return if (remainDays > 0) {
                    "🛡️ App uninstall protection is active. $remainDays days and $remainHours hours remaining."
                } else {
                    "🛡️ App uninstall protection is active. $remainHours hours and $remainMins minutes remaining."
                }
            }
        }
        return "إيقاف مسؤول الجهاز سيؤدي إلى إضعاف جلسات التركيز الخاصة بك."
    }

    override fun onDisabled(context: Context, intent: Intent) {
        super.onDisabled(context, intent)
    }
}

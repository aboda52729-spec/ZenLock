package com.example

object Translations {
    val LANGUAGES = listOf(
        LanguageInfo("ar", "العربية", "🇸🇦"),
        LanguageInfo("en", "English", "🇺🇸"),
        LanguageInfo("es", "Español", "🇪🇸"),
        LanguageInfo("fr", "Français", "🇫🇷"),
        LanguageInfo("hi", "हिन्दी", "🇮🇳")
    )

    data class LanguageInfo(val code: String, val name: String, val flag: String)

    private val pool = mapOf(
        "ar" to mapOf(
            "app_title" to "ZenLock",
            "app_subtitle" to "درع التركيز والتعافي العميق",
            "stealth_title" to "ميزة عدم الأثر (Stealth)",
            "stealth_subtitle" to "وضع التخفي والبصري والسرية الحصري",
            "stealth_desc" to "عند تفعيل وضع عدم الأثر، يتم تحويل شارات وأيقونات البرامج المحجبة إلى شارة وإشارة غامضة باسم 'Blocked' مع إخفاء رمز واسم التطبيقات الأصلي لمنع استثارة فضولك أو إعادة الرغبة البصرية في زيارتها.",
            "shield_title" to "درع التعافي والوقاية الكاملة",
            "shield_subtitle" to "ضد المواقع الإباحية والـ VPN والإعدادات",
            "shield_desc" to "عند تفعيل الميزة، ينشط جدار وقائي ذكي يمنع تماماً محاولات فتح أي موقع إباحي عبر كافة المتصفحات، ويعزل تطبيقات والتحكمات الخاصة بـ VPN أو تغيير إعدادات إمكانية الوصول لمنع الالتفاف حتى ينتهي العداد مع الإغلاق الكلي للمنشط.",
            "timer_title" to "مؤقت احترافي",
            "timer_subtitle" to "اسحب لأعلى أو لأسفل لضبط الوقت بدقة بالغة",
            "seconds" to "ثواني",
            "minutes" to "دقائق",
            "hours" to "ساعات",
            "save_preset" to "حفظ كإعداد مسبق",
            "reset" to "إعادة ضبط",
            "presets_title" to "المفضلات المخزنة والافتراضية:",
            "pomodoro" to "بومودورو (25د)",
            "deep_focus" to "تركيز عميق (1س)",
            "focus_test" to "فحص تركيز (10ث)",
            "kickstart" to "انطلاقة (5د)",
            "activate_lock" to "تفعيل القفل",
            "shielded_apps" to "التطبيقات المحمية",
            "locked_count" to "مغلق",
            "no_apps_targeted" to "لا توجد تطبيقات مستهدفة.\nاضغط أدناه لاختيار التطبيقات لقفلها.",
            "add_edit_list" to "إضافة / تعديل قائمة الحظر +",
            "strict_active_title" to "تم تفعيل القفل الصارم بنجاح",
            "strict_active_desc" to "لا توجد ثغرات أو مهرب. الجلسة مغلقة تماماً حتى ينتهي العداد لضمان انضباطك المطلق.",
            "deep_security_active" to "الأمن العميق نشط",
            "time_remaining" to "الوقت المتبقي",
            "until_unlock" to "حتى فك القفل التلقائي",
            "no_distractions" to "لا ملهيات بصرية",
            "no_distractions_sub" to "تم تعطيل كافة الثغرات والالتفافات. ابقَ في تدفق.",
            "blocked_screen_title" to "الوصول غير متاح",
            "blocked_screen_sub" to "الوصول مقيد حالياً لضمان عزمك.",
            "return_to_work" to "العودة للعمل",
            "on_the_covenant" to "أنا ثابت وعلى العهد",
            "recovery_shield_success" to "تم صد محاولة تصفح الإباحية، تشغيل الـ VPN، أو العبث بالإعدادات بنجاح. حريتك ونقاء عقلك وروحك هما الأهم. غايتك السامية تستحق الثبات!",
            "stealth_mode_active_info" to "تم إخفاء وعزل هذا التطبيق تماماً تحت وضع عدم الأثر لضمان عدم توفير أي مشتتات أو رغبة بصرية في فتحه. واصل التركيز!",
            "fallback_blocked_desc" to "يقع هذا التطبيق ضمن قائمة التطبيقات الممنوعة خلال جلسة التركيز الحالية. حافظ على إنتاجيتك واستمر في التركيز.",
            "cannot_open_settings" to "🛡️ تم قفل الإعدادات حمايةً لالتزامك حتى انتهاء العداد!",
            "cannot_open_vpn" to "🛡️ درع الوقاية يمنع تشغيل تطبيقات الـ VPN لمنع الالتفاف!",
            "vpn_detected" to "🛡️ تم كشف اتصال VPN نشط! تم حظره فوراً لحمايتك.",
            "porn_content_detected" to "🛡️ تم حجب المحتوى تلقائياً بواسطة درع التعافي الذكي لـ ZenLock!",
            "focus_active_default_msg" to "🛡️ وضع التركيز نشط حالياً. تم تقييد الوصول لضمان إنتاجيتك.",
            "language_selection" to "لغة التطبيق"
        ),
        "en" to mapOf(
            "app_title" to "ZenLock",
            "app_subtitle" to "Deep Focus & Recovery Shield",
            "stealth_title" to "Stealth Mode (No Trace)",
            "stealth_subtitle" to "Exclusive Privacy & Visual Hiding",
            "stealth_desc" to "When enabled, blocked apps have their icons and labels transformed into generic 'Blocked' placeholders inside ZenLock to prevent psychological cravings and visual temptation.",
            "shield_title" to "Recovery & Prevention Shield",
            "shield_subtitle" to "Anti-Pornography, Anti-VPN & System Guard",
            "shield_desc" to "Once activated, a dynamic background shield completely blocks pornographic websites on all browsers, and restricts VPN tools, proxies, and system accessibility settings to eliminate loop-holes until the lock expires.",
            "timer_title" to "Professional Timer Selector",
            "timer_subtitle" to "Swipe up or down to configure session duration",
            "seconds" to "seconds",
            "minutes" to "minutes",
            "hours" to "hours",
            "save_preset" to "Save Preset",
            "reset" to "Reset Picker",
            "presets_title" to "Stored & Default Presets:",
            "pomodoro" to "Pomodoro (25m)",
            "deep_focus" to "Deep Focus (1h)",
            "focus_test" to "Focus Test (10s)",
            "kickstart" to "Kickstart (5m)",
            "activate_lock" to "ACTIVATE LOCK",
            "shielded_apps" to "Shielded Apps",
            "locked_count" to "Locked",
            "no_apps_targeted" to "No apps targeted.\nTap below to select apps to lock.",
            "add_edit_list" to "Add / Edit Block List +",
            "strict_active_title" to "Strict Lock Active",
            "strict_active_desc" to "All pathways are sealed. The session is fully enforced until the timer hits zero for complete self-discipline.",
            "deep_security_active" to "DEEP SECURITY ACTIVE",
            "time_remaining" to "TIME REMAINING",
            "until_unlock" to "until deep-unlock",
            "no_distractions" to "NO DISTRACTIONS",
            "no_distractions_sub" to "Loopholes are disabled. Stay in flow.",
            "blocked_screen_title" to "Access Restricted",
            "blocked_screen_sub" to "This application is currently closed to preserve your focus.",
            "return_to_work" to "Return to Work",
            "on_the_covenant" to "I Stand Resolved",
            "recovery_shield_success" to "Pornography attempt, VPN launch, or settings manipulation blocked. Your freedom, mental purity, and goals deserve absolute persistence!",
            "stealth_mode_active_info" to "This application is isolated under Stealth Mode to avoid visual triggers. Keep driving forward!",
            "fallback_blocked_desc" to "This app is in your focus shield list. Maintain self-discipline and resume focus.",
            "cannot_open_settings" to "🛡️ System settings locked to guard your commitment until time expires!",
            "cannot_open_vpn" to "🛡️ Recovery Shield prevents VPN launch to avoid bypass!",
            "vpn_detected" to "🛡️ Active VPN detected! Blocked instantly for your protection.",
            "porn_content_detected" to "🛡️ Content blocked by ZenLock Smart Recovery Shield!",
            "focus_active_default_msg" to "🛡️ Focus session active. Access restricted to maintain productivity.",
            "language_selection" to "App Language"
        ),
        "es" to mapOf(
            "app_title" to "ZenLock",
            "app_subtitle" to "Escudo de Enfoque y Recuperación Profunda",
            "stealth_title" to "Modo Sigiloso (Sin Rastro)",
            "stealth_subtitle" to "Ocultación Visual y Privacidad Exclusiva",
            "stealth_desc" to "Cuando está habilitado, las aplicaciones bloqueadas transforman sus íconos y nombres en marcadores genéricos de 'Bloqueados' dentro de ZenLock para prevenir tentaciones visuales.",
            "shield_title" to "Escudo de Recuperación y Prevención",
            "shield_subtitle" to "Anti-Pornografía, Anti-VPN y Guardia del Sistema",
            "shield_desc" to "Una vez activo, un escudo inteligente bloquea páginas para adultos en cualquier navegador, bloquea VPNs, proxies y evita desactivar la accesibilidad hasta que expire el tiempo.",
            "timer_title" to "Selector de Tiempo Profesional",
            "timer_subtitle" to "Desliza hacia arriba o abajo para configurar la sesión",
            "seconds" to "segundos",
            "minutes" to "minutos",
            "hours" to "horas",
            "save_preset" to "Guardar Ajuste",
            "reset" to "Restablecer",
            "presets_title" to "Preajustes guardados y por defecto:",
            "pomodoro" to "Pomodoro (25m)",
            "deep_focus" to "Enfoque Profundo (1h)",
            "focus_test" to "Prueba de Enfoque (10s)",
            "kickstart" to "Comienzo Rápido (5m)",
            "activate_lock" to "ACTIVAR BLOQUEO",
            "shielded_apps" to "Apps Protegidas",
            "locked_count" to "Bloqueado",
            "no_apps_targeted" to "No hay apps seleccionadas.\nToca abajo para bloquear aplicaciones.",
            "add_edit_list" to "Añadir / Editar lista de bloqueo +",
            "strict_active_title" to "Cierre Estricto Activo",
            "strict_active_desc" to "Todas las salidas están selladas. La sesión se cumple estrictamente hasta que el contador llegue a cero.",
            "deep_security_active" to "SEGURIDAD DEEP ACTIVA",
            "time_remaining" to "TIEMPO RESTANTE",
            "until_unlock" to "hasta desbloqueo",
            "no_distractions" to "SIN DISTRACCIONES",
            "no_distractions_sub" to "Los atajos están inactivos. Mantén el flujo.",
            "blocked_screen_title" to "Acceso Restringido",
            "blocked_screen_sub" to "Esta aplicación está bloqueada para preservar tu calma y enfoque.",
            "return_to_work" to "Volver al Trabajo",
            "on_the_covenant" to "Me mantengo firme",
            "recovery_shield_success" to "Intento de pornografía, VPN o alteración de ajustes bloqueado. ¡Tu libertad y pureza mental merecen constancia!",
            "stealth_mode_active_info" to "Esta aplicación está silenciada bajo Modo Sigiloso. ¡Mantén el enfoque en lo que importa!",
            "fallback_blocked_desc" to "Esta app está en tu lista de bloqueo activa. Sé fuerte.",
            "cannot_open_settings" to "🛡️ ¡Ajustes bloqueados para resguardar tu objetivo!",
            "cannot_open_vpn" to "🛡️ ¡El Escudo impide abrir la VPN para evitar desvíos!",
            "vpn_detected" to "🛡️ ¡Conexión VPN detectada! Bloqueada al instante para protegerte.",
            "porn_content_detected" to "🛡️ ¡Contenido bloqueado por el Escudo Inteligente ZenLock!",
            "focus_active_default_msg" to "🛡️ Sesión activa. Acceso restringido para maximizar tu productividad.",
            "language_selection" to "Idioma"
        ),
        "fr" to mapOf(
            "app_title" to "ZenLock",
            "app_subtitle" to "Bouclier de Concentration et Récupération",
            "stealth_title" to "Mode Furtif (Sans Trace)",
            "stealth_subtitle" to "Confidentialité et Masquage Visuel",
            "stealth_desc" to "Une fois activé, l'icône de l'app bloquée se transforme sous ZenLock en espace générique 'Bloqué' afin d'enrayer toute envie addictive.",
            "shield_title" to "Bouclier de Préservation & Contre-Rechute",
            "shield_subtitle" to "Anti-Pornographie, Bloqueur VPN & Paramètres",
            "shield_desc" to "Active un filtre intelligent contre l'accès porno dans les navigateurs, verrouille les VPN, proxies et protège les paramètres d'accessibilité durant la session.",
            "timer_title" to "Sélecteur Temporel Pro",
            "timer_subtitle" to "Glissez vers le haut ou le bas pour ajuster la durée",
            "seconds" to "secondes",
            "minutes" to "minutes",
            "hours" to "heures",
            "save_preset" to "Enregistrer",
            "reset" to "Réinitialiser",
            "presets_title" to "Préconfigurations disponibles :",
            "pomodoro" to "Pomodoro (25m)",
            "deep_focus" to "Concentration Forte (1h)",
            "focus_test" to "Test Flash (10s)",
            "kickstart" to "Décollage (5m)",
            "activate_lock" to "ACTIVER LE VERROU",
            "shielded_apps" to "Apps Sécurisées",
            "locked_count" to "Bloqué",
            "no_apps_targeted" to "Aucune app ciblée.\nTapez ci-dessous pour choisir.",
            "add_edit_list" to "Ajouter / Modifier la liste +",
            "strict_active_title" to "Verrouillage Strict Activé",
            "strict_active_desc" to "Aucune issue possible. Session totalement protégée jusqu'à la fin de la minuterie pour un focus d'acier.",
            "deep_security_active" to "SÉCURITÉ PROFONDE ACTIVE",
            "time_remaining" to "TEMPS RESTANT",
            "until_unlock" to "avant déverrouillage",
            "no_distractions" to "SANS DISTRACTION",
            "no_distractions_sub" to "Contournements bloqués. Persévérez.",
            "blocked_screen_title" to "Accès Restreint",
            "blocked_screen_sub" to "Cette application est suspendue pour protéger votre session.",
            "return_to_work" to "Retour au travail",
            "on_the_covenant" to "Je reste résolu",
            "recovery_shield_success" to "Accès pour adultes, VPN ou réglages bloqués. Votre calme, votre clarté et vos ambitions méritent cette rigueur !",
            "stealth_mode_active_info" to "Cette app est sous cloche furtive. Poursuivez vos efforts !",
            "fallback_blocked_desc" to "Cette application fait partie de votre zone protégée actuelle. Restez concentré.",
            "cannot_open_settings" to "🛡️ Accès aux réglages bloqué pour préserver votre discipline !",
            "cannot_open_vpn" to "🛡️ Lancement VPN bloqué pour écarter tout contournement !",
            "vpn_detected" to "🛡️ VPN détecté ! Coupé instantanément par précaution.",
            "porn_content_detected" to "🛡️ Filtre système : page pour adultes interceptée avec succès !",
            "focus_active_default_msg" to "🛡️ Focus en cours. Accès limité pour vous guider vers vos objectifs.",
            "language_selection" to "Langue de l'app"
        ),
        "hi" to mapOf(
            "app_title" to "ZenLock",
            "app_subtitle" to "डीप फोकस और रिकवरी शील्ड",
            "stealth_title" to "स्टील्थ मोड (कोई निशान नहीं)",
            "stealth_subtitle" to "विशेष गोपनीयता और दृश्य छिपाव",
            "stealth_desc" to "सक्रिय होने पर, ZenLock के भीतर ब्लॉक किए गए ऐप्स के आइकन 'Blocked' में बदल जाते हैं ताकि आंखों की उत्तेजना से बचा जा सके।",
            "shield_title" to "रिकवरी और पूर्ण रोकथाम शील्ड",
            "shield_subtitle" to "अश्लील वेबसाइट, वीपीएन और नियंत्रण गार्ड",
            "shield_desc" to "इसे चालू करते ही पृष्ठभूमि रक्षात्मक दीवार सभी ब्राउज़रों में वयस्क वेबसाइटों को अवरुद्ध कर देती है, और वीपीएन तथा सेटिंग्स को निष्क्रिय कर देती है।",
            "timer_title" to "प्रोफेशनल टाइमर चयनकर्ता",
            "timer_subtitle" to "सत्र का समय निर्धारित करने के लिए ऊपर या नीचे स्वाइप करें",
            "seconds" to "सेकंड",
            "minutes" to "मिनट",
            "hours" to "घंटे",
            "save_preset" to "प्रीसेट सहेजें",
            "reset" to "रीसेट करें",
            "presets_title" to "सहेजे गए और डिफ़ॉल्ट प्रीसेट:",
            "pomodoro" to "पोमोडोरो (25मि)",
            "deep_focus" to "गहन ध्यान (1घं)",
            "focus_test" to "फोकस टेस्ट (10से)",
            "kickstart" to "किकस्टार्ट (5मि)",
            "activate_lock" to "लॉक सक्रिय करें",
            "shielded_apps" to "संरक्षित ऐप्स",
            "locked_count" to "अवरुद्ध",
            "no_apps_targeted" to "कोई ऐप लक्षित नहीं है।\nऐप्स चुनने के लिए नीचे दबाएं।",
            "add_edit_list" to "ब्लॉक सूची बदलें +",
            "strict_active_title" to "कठोर शील सक्रिय",
            "strict_active_desc" to "पूर्ण आत्म-अनुशासन के लिए समय समाप्त होने तक सत्र कड़ाई से लागू रहता है।",
            "deep_security_active" to "गहन सुरक्षा सक्रिय",
            "time_remaining" to "शेष समय",
            "until_unlock" to "अनलॉक होने तक",
            "no_distractions" to "कोई व्याकुलता नहीं",
            "no_distractions_sub" to "सभी अंतराल मार्ग बंद हैं। सहजता से आगे बढ़ें।",
            "blocked_screen_title" to "पहुँच अवरुद्ध",
            "blocked_screen_sub" to "आपकी प्रतिज्ञा की गरिमा को बनाए रखने के लिए यह ऐप अभी बंद है।",
            "return_to_work" to "वापस कार्य पर जाएं",
            "on_the_covenant" to "मैं संकल्प पर अटल हूँ",
            "recovery_shield_success" to "वयस्क सामग्री का प्रयास, वीपीएन चलाना या सेटिंग्स परिवर्तन अवरुद्ध किया गया। आपकी मुक्ति और मन की शुद्धता अमूल्य है!",
            "stealth_mode_active_info" to "यह ऐप ध्यान भटकाने से बचाने के लिए पूरी तरह से अलग-थलग कर दिया गया है।",
            "fallback_blocked_desc" to "यह ऐप आपकी वर्तमान सत्र रक्षण सूची में है। संयम बरतें।",
            "cannot_open_settings" to "🛡️ आपकी दृढ़ता बनाए रखने के लिए सेटिंग्स ब्लॉक कर दी गई है!",
            "cannot_open_vpn" to "🛡️ बाईपास रोकने के लिए शील्ड वीपीएन को चलाने से रोकता है!",
            "vpn_detected" to "🛡️ सक्रिय वीपीएन पकड़ा गया! आपकी सुरक्षा के लिए इसे तुरंत रोका गया।",
            "porn_content_detected" to "🛡️ ZenLock सचेत रिकवरी शील्ड द्वारा वयस्क सामग्री अवरुद्ध!",
            "focus_active_default_msg" to "🛡️ फोकस सत्र सक्रिय है। कार्यकुशल बने रहें।",
            "language_selection" to "ऐप की भाषा"
        )
    )

    fun get(key: String, lang: String): String {
        return pool[lang]?.get(key) ?: pool["en"]?.get(key) ?: getStaticFallback(key, lang)
    }

    private fun getStaticFallback(key: String, lang: String): String {
        return when (key) {
            "permission_setup_title" -> when (lang) {
                "ar" -> "إعداد الحماية القصوى"
                "es" -> "Activando Protección Suprema"
                "fr" -> "Activation de la Protection Suprême"
                "hi" -> "परम सुरक्षा सक्षम करना"
                else -> "Enabling Supreme Protection"
            }
            "permission_setup_desc" -> when (lang) {
                "ar" -> "يتطلب ZenLock تفعيل 3 أذونات أمان أساسية لضمان إغلاق محكم ومنع أي محاولات للالتفاف على نظام التركيز."
                "es" -> "ZenLock requiere habilitar 3 permisos de seguridad esenciales para garantizar un bloqueo sólido y evitar desvíos."
                "fr" -> "ZenLock nécessite l'activation de 3 permissions de sécurité essentielles pour garantir un verrouillage solide."
                "hi" -> "ZenLock को ठोस लॉकडाउन की गारंटी देने और बाईपास को रोकने के लिए 3 आवश्यक सुरक्षा अनुमतियों की आवश्यकता होती है।"
                else -> "ZenLock requires enabling 3 essential security permissions to guarantee solid lockdown and prevent focus bypass."
            }
            "perm_accessibility_title" -> when (lang) {
                "ar" -> "درع إمكانية الوصول"
                "es" -> "Escudo de Accesibilidad"
                "fr" -> "Bouclier d'Accessibilité"
                "hi" -> "एक्सेसिबिलिटी शील्ड"
                else -> "Accessibility Shield"
            }
            "perm_accessibility_desc" -> when (lang) {
                "ar" -> "أساسي لرصد وإغلاق التطبيقات المشتتة فور محاولة فتحها."
                "es" -> "Esencial para detectar y bloquear aplicaciones de distracción al instante."
                "fr" -> "Crucial pour détecter et verrouiller instantanément les applications perturbatrices."
                "hi" -> "ध्यान भटकाने वाले ऐप्स का तुरंत पता लगाने और ब्लॉक करने के लिए महत्वपूर्ण है।"
                else -> "Critical for detecting and blocking distracting applications instantly."
            }
            "perm_admin_title" -> when (lang) {
                "ar" -> "مشرف الجهاز (الحماية القصوى)"
                "es" -> "Administrador de Dispositivo"
                "fr" -> "Administrateur de l'appareil"
                "hi" -> "डिवाइस एडमिन (परम सुरक्षा)"
                else -> "Device Admin (Supreme Protection)"
            }
            "perm_admin_desc" -> when (lang) {
                "ar" -> "يمنع إلغاء تثبيت التطبيق أو إيقافه إجبارياً أثناء جلسة التركيز."
                "es" -> "Evita la desinstalación o el cierre forzado de la aplicación durante la sesión."
                "fr" -> "Empêche la désinstallation ou l'arrêt forcé de l'application pendant la session."
                "hi" -> "फोकस सत्र के दौरान ऐप को अनइंस्टॉल करने या ज़बरदस्ती बंद करने से रोकता है।"
                else -> "Prevents app uninstallation or force-stopping during focus sessions."
            }
            "perm_notif_title" -> when (lang) {
                "ar" -> "وضع إخفاء الإشعارات"
                "es" -> "Escudo de Notificaciones"
                "fr" -> "Bouclier de Notifications"
                "hi" -> "अधिसूचना शील्ड"
                else -> "Do Not Disturb/Notification Shield"
            }
            "perm_notif_desc" -> when (lang) {
                "ar" -> "يقوم بإسكات إشعارات التطبيقات المحظورة لضمان هدوء تام."
                "es" -> "Silencia notificaciones de aplicaciones bloqueadas para mantener serenidad."
                "fr" -> "Masque les notifications des applications bloquées pour une sérénité totale."
                "hi" -> "अखंड शांति बनाए रखने के लिए अवरुद्ध ऐप्स से आने वाली सूचनाओं को मौन करता है।"
                else -> "Mutes notifications from blocked applications to maintain complete serenity."
            }
            "perm_waiting" -> when (lang) {
                "ar" -> "في انتظار تفعيل كافة الصلاحيات..."
                "es" -> "Esperando la activación de todos los permisos..."
                "fr" -> "En attente de l'activation des permissions..."
                "hi" -> "सभी सुरक्षा अनुमतियों के सक्रिय होने की प्रतीक्षा की जा रही है..."
                else -> "Awaiting activation of all security permissions..."
            }
            else -> key
        }
    }
}

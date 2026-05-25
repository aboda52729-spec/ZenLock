package com.example

import android.accessibilityservice.AccessibilityService
import android.content.ComponentName
import android.content.Context
import android.content.Intent
import android.content.pm.PackageManager
import android.os.Bundle
import android.provider.Settings
import android.text.TextUtils
import androidx.activity.ComponentActivity
import androidx.activity.SystemBarStyle
import androidx.activity.compose.BackHandler
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.compose.ui.graphics.asImageBitmap
import androidx.core.graphics.drawable.toBitmap
import androidx.compose.foundation.Image
import androidx.compose.animation.AnimatedContent
import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.animateColorAsState
import androidx.compose.animation.core.*
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.animation.scaleIn
import androidx.compose.animation.scaleOut
import androidx.compose.animation.togetherWith
import androidx.compose.foundation.*
import androidx.compose.foundation.gestures.detectTapGestures
import androidx.compose.foundation.gestures.detectVerticalDragGestures
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.rememberLazyListState
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.foundation.lazy.grid.GridCells
import androidx.compose.foundation.lazy.grid.LazyVerticalGrid
import androidx.compose.foundation.lazy.grid.items
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.*
import androidx.compose.material.icons.outlined.Lock
import androidx.compose.material.icons.outlined.Settings
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.drawBehind
import androidx.compose.ui.draw.rotate
import androidx.compose.ui.draw.scale
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.StrokeCap
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.ui.window.Dialog
import androidx.compose.ui.window.DialogProperties
import com.example.ui.theme.MyApplicationTheme
import kotlinx.coroutines.delay
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import androidx.compose.ui.platform.LocalLayoutDirection
import androidx.compose.ui.unit.LayoutDirection
import androidx.compose.ui.graphics.ImageBitmap
import androidx.compose.ui.graphics.ColorFilter
import androidx.compose.ui.graphics.ColorMatrix

class MainActivity : ComponentActivity() {
    private val blockedAppNameState = androidx.compose.runtime.mutableStateOf<String?>(null)

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        handleIntent(intent)
        enableEdgeToEdge()
        setContent {
            val context = LocalContext.current
            var isDarkTheme by remember { mutableStateOf<Boolean?>(LockSettings.isDarkThemePreferred(context)) }
            val systemTheme = isSystemInDarkTheme()
            val useDark = isDarkTheme ?: systemTheme

            // Robust dynamic status and navigation bar styling
            LaunchedEffect(useDark) {
                try {
                    enableEdgeToEdge(
                        statusBarStyle = SystemBarStyle.auto(
                            android.graphics.Color.TRANSPARENT,
                            android.graphics.Color.TRANSPARENT
                        ) { useDark },
                        navigationBarStyle = SystemBarStyle.auto(
                            android.graphics.Color.TRANSPARENT,
                            android.graphics.Color.TRANSPARENT
                        ) { useDark }
                    )
                } catch (e: Exception) {
                    e.printStackTrace()
                }
            }

            MyApplicationTheme(darkTheme = useDark) {
                val blockedPackage by blockedAppNameState
                ZenLockApp(
                    isDarkTheme = useDark,
                    onToggleTheme = {
                        val next = !useDark
                        LockSettings.setDarkThemePreferred(context, next)
                        isDarkTheme = next
                    },
                    blockedPackage = blockedPackage,
                    onClearBlockedMessage = {
                        blockedAppNameState.value = null
                    }
                )
            }
        }
    }

    override fun onNewIntent(intent: Intent) {
        super.onNewIntent(intent)
        setIntent(intent)
        handleIntent(intent)
    }

    private fun handleIntent(intent: Intent?) {
        if (intent != null && intent.getBooleanExtra("FROM_BLOCK_TRIGGER", false)) {
            val pkg = intent.getStringExtra("BLOCKED_APP_NAME")
            if (pkg != null) {
                blockedAppNameState.value = pkg
            }
            // Consume the intent extras so they do not persist through configuration changes, reconstructions, or rotations
            intent.removeExtra("FROM_BLOCK_TRIGGER")
            intent.removeExtra("BLOCKED_APP_NAME")
        }
    }
}

data class DeviceAppInfo(
    val packageName: String,
    val label: String,
    val color: Color
)

object AppIconCache {
    private val cache = java.util.concurrent.ConcurrentHashMap<String, ImageBitmap>()
    
    fun get(packageName: String): ImageBitmap? = cache[packageName]
    fun put(packageName: String, bitmap: ImageBitmap) {
        cache[packageName] = bitmap
    }
}

@Composable
fun AppIcon(
    packageName: String,
    modifier: Modifier = Modifier,
    fallbackLabel: String,
    fallbackColor: Color,
    isStealthMode: Boolean = false,
    textStyle: androidx.compose.ui.text.TextStyle = MaterialTheme.typography.titleMedium,
    colorFilter: ColorFilter? = null
) {
    if (isStealthMode) {
        Box(
            modifier = modifier
                .background(Color(0xFF475569).copy(alpha = 0.15f)),
            contentAlignment = Alignment.Center
        ) {
            Icon(
                imageVector = Icons.Filled.Lock,
                contentDescription = "Hidden",
                tint = Color(0xFF64748B),
                modifier = Modifier.fillMaxSize(0.6f)
            )
        }
    } else {
        val context = LocalContext.current
        var bitmapState by remember(packageName) { mutableStateOf<ImageBitmap?>(AppIconCache.get(packageName)) }

        if (bitmapState == null) {
            LaunchedEffect(packageName) {
                val cached = AppIconCache.get(packageName)
                if (cached != null) {
                    bitmapState = cached
                } else {
                    val b = withContext(Dispatchers.IO) {
                        try {
                            val pm = context.packageManager
                            val drawable = pm.getApplicationIcon(packageName)
                            drawable.toBitmap(width = 100, height = 100).asImageBitmap()
                        } catch (e: Exception) {
                            null
                        }
                    }
                    if (b != null) {
                        AppIconCache.put(packageName, b)
                        bitmapState = b
                    }
                }
            }
        }

        val appIcon = bitmapState
        if (appIcon != null) {
            Image(
                bitmap = appIcon,
                contentDescription = "$fallbackLabel icon",
                modifier = modifier,
                colorFilter = colorFilter
            )
        } else {
            Box(
                modifier = modifier
                    .background(fallbackColor),
                contentAlignment = Alignment.Center
            ) {
                Text(
                    text = fallbackLabel.firstOrNull()?.uppercase() ?: "",
                    style = textStyle,
                    color = Color.White,
                    fontWeight = FontWeight.Black
                )
            }
        }
    }
}

@Composable
fun BlockedNotificationDialog(
    packageName: String,
    onDismiss: () -> Unit
) {
    val context = LocalContext.current
    val selectedLang = remember { LockSettings.getSelectedLanguage(context) }
    fun t(key: String): String = Translations.get(key, selectedLang)

    var appLabelState by remember(packageName) { mutableStateOf(packageName) }
    LaunchedEffect(packageName) {
        if (packageName == "adult_content_blocked_shield") {
            appLabelState = t("shield_title")
        } else {
            val label = withContext(Dispatchers.IO) {
                try {
                    val pm = context.packageManager
                    val info = pm.getApplicationInfo(packageName, 0)
                    pm.getApplicationLabel(info).toString()
                } catch (e: Exception) {
                    packageName
                }
            }
            appLabelState = label
        }
    }

    // Auto-dismiss the blocked dialog after 4 seconds to prevent it from remaining on screen
    LaunchedEffect(packageName) {
        delay(4000)
        onDismiss()
    }

    val appLabel = appLabelState

    val layoutDirection = if (selectedLang == "ar") LayoutDirection.Rtl else LayoutDirection.Ltr

    Dialog(
        onDismissRequest = onDismiss,
        properties = DialogProperties(dismissOnBackPress = true, dismissOnClickOutside = true)
    ) {
        Box(
            modifier = Modifier
                .fillMaxWidth()
                .padding(16.dp),
            contentAlignment = Alignment.Center
        ) {
            CompositionLocalProvider(LocalLayoutDirection provides layoutDirection) {
                Surface(
                    shape = RoundedCornerShape(28.dp),
                    color = Color(0xFF0F0F12),
                    tonalElevation = 12.dp,
                    shadowElevation = 24.dp,
                    modifier = Modifier
                        .fillMaxWidth()
                        .border(1.dp, Color(0xFFEF4444).copy(alpha = 0.4f), RoundedCornerShape(28.dp))
                ) {
                    Column(
                        modifier = Modifier.padding(32.dp),
                        horizontalAlignment = Alignment.CenterHorizontally
                    ) {
                        Box(
                            modifier = Modifier
                                .size(80.dp)
                                .clip(CircleShape)
                                .background(Brush.radialGradient(listOf(Color(0xFFEF4444).copy(alpha = 0.2f), Color.Transparent))),
                            contentAlignment = Alignment.Center
                        ) {
                            if (packageName == "adult_content_blocked_shield") {
                                Box(
                                    modifier = Modifier
                                        .size(56.dp)
                                        .clip(CircleShape)
                                        .background(Color(0xFFEF4444).copy(alpha = 0.15f))
                                        .border(2.dp, Color(0xFFEF4444), CircleShape),
                                    contentAlignment = Alignment.Center
                                ) {
                                    Icon(
                                        imageVector = Icons.Filled.Lock,
                                        contentDescription = "Shield Active",
                                        tint = Color(0xFFEF4444),
                                        modifier = Modifier.size(28.dp)
                                    )
                                }
                            } else {
                                AppIcon(
                                    packageName = packageName,
                                    modifier = Modifier
                                        .size(56.dp)
                                        .clip(CircleShape)
                                        .border(2.dp, Color(0xFFEF4444).copy(alpha = 0.5f), CircleShape),
                                    fallbackLabel = appLabel,
                                    fallbackColor = Color(0xFFEF4444),
                                    isStealthMode = false
                                )
                            }
                        }

                        Spacer(modifier = Modifier.height(24.dp))

                        Text(
                            text = if (packageName == "adult_content_blocked_shield") {
                                t("shield_title")
                            } else {
                                t("blocked_screen_title")
                            },
                            style = MaterialTheme.typography.headlineSmall,
                            fontWeight = FontWeight.ExtraBold,
                            color = Color(0xFFEF4444)
                        )

                        Spacer(modifier = Modifier.height(12.dp))

                        Text(
                            text = if (packageName == "adult_content_blocked_shield") {
                                t("recovery_shield_success")
                            } else {
                                t("fallback_blocked_desc").replace("this", "\"$appLabel\"").replace("This app", "\"$appLabel\"")
                            },
                            style = MaterialTheme.typography.bodyMedium.copy(lineHeight = 24.sp),
                            color = Color(0xFFE2E8F0),
                            textAlign = TextAlign.Center
                        )

                        Spacer(modifier = Modifier.height(28.dp))

                        Button(
                            onClick = onDismiss,
                            colors = ButtonDefaults.buttonColors(
                                containerColor = Color(0xFFEF4444)
                            ),
                            shape = RoundedCornerShape(16.dp),
                            modifier = Modifier
                                .fillMaxWidth()
                                .height(54.dp)
                        ) {
                            Text(
                                if (packageName == "adult_content_blocked_shield") t("on_the_covenant") else t("return_to_work"), 
                                color = Color.White,
                                style = MaterialTheme.typography.titleMedium,
                                fontWeight = FontWeight.Bold
                            )
                        }
                    }
                }
            }
        }
    }
}

// Queries all applications currently installed on the phone that have launcher activities.
fun getInstalledLauncherApps(context: Context): List<DeviceAppInfo> {
    val appList = mutableListOf<DeviceAppInfo>()
    try {
        val pm = context.packageManager
        val intent = Intent(Intent.ACTION_MAIN, null).apply {
            addCategory(Intent.CATEGORY_LAUNCHER)
        }
        
        val list = pm.queryIntentActivities(intent, 0)
        val processed = mutableSetOf<String>()
        
        val palette = listOf(
            Color(0xFFEC4899), // Pink
            Color(0xFF3B82F6), // Blue
            Color(0xFF10B981), // Green
            Color(0xFF8B5CF6), // Purple
            Color(0xFFF59E0B), // Orange
            Color(0xFF06B6D4), // Cyan
            Color(0xFFEF4444), // Red
            Color(0xFF14B8A6), // Teal
            Color(0xFF6366F1)  // Indigo
        )
        
        for (info in list) {
            val packageName = info.activityInfo.packageName
            if (packageName == context.packageName) continue // Protect self-blocking
            if (!processed.contains(packageName)) {
                processed.add(packageName)
                val label = info.loadLabel(pm).toString()
                val hash = Math.abs(packageName.hashCode())
                val color = palette[hash % palette.size]
                appList.add(DeviceAppInfo(packageName, label, color))
            }
        }
    } catch (e: Exception) {
        e.printStackTrace()
    }
    return appList.sortedBy { it.label.lowercase() }
}

// Check if accessibility permission is enabled
fun isAccessibilityServiceEnabled(context: Context): Boolean {
    try {
        val enabled = Settings.Secure.getInt(
            context.contentResolver,
            Settings.Secure.ACCESSIBILITY_ENABLED,
            0
        )
        if (enabled == 1) {
            val settingValue = Settings.Secure.getString(
                context.contentResolver,
                Settings.Secure.ENABLED_ACCESSIBILITY_SERVICES
            )
            if (settingValue != null) {
                val splitter = TextUtils.SimpleStringSplitter(':')
                splitter.setString(settingValue)
                while (splitter.hasNext()) {
                    val accessService = splitter.next()
                    if (accessService.contains(context.packageName, ignoreCase = true)) {
                        return true
                    }
                }
            }
        }
    } catch (e: Exception) {
        e.printStackTrace()
    }
    return false
}

// Check if device admin is enabled
fun isDeviceAdminEnabled(context: Context): Boolean {
    val devicePolicyManager = context.getSystemService(Context.DEVICE_POLICY_SERVICE) as android.app.admin.DevicePolicyManager
    val componentName = ComponentName(context, FocusDeviceAdminReceiver::class.java)
    return devicePolicyManager.isAdminActive(componentName)
}

// Check if notification listener service is enabled
fun isNotificationServiceEnabled(context: Context): Boolean {
    val pkgName = context.packageName
    val flat = Settings.Secure.getString(context.contentResolver, "enabled_notification_listeners")
    return flat != null && flat.contains(pkgName)
}

@Composable
fun ZenLockApp(
    isDarkTheme: Boolean,
    onToggleTheme: () -> Unit,
    blockedPackage: String?,
    onClearBlockedMessage: () -> Unit
) {
    val context = LocalContext.current
    
    // Core state loaded selectively from real persisted settings
    var isLockdown by remember { mutableStateOf(LockSettings.isLockdownActive(context)) }
    var selectedApps by remember { mutableStateOf(LockSettings.getBlockedApps(context)) }
    var lockdownDurationSecs by remember { mutableIntStateOf(LockSettings.getDefaultDurationSecs(context)) }
    var selectedLang by remember { mutableStateOf(LockSettings.getSelectedLanguage(context)) }
    
    // Loaded list of launcher applications
    var installedApps by remember { mutableStateOf<List<DeviceAppInfo>>(emptyList()) }
    
    // Shield configuration state
    var isShieldActive by remember { mutableStateOf(isAccessibilityServiceEnabled(context)) }
    var isNotificationActive by remember { mutableStateOf(isNotificationServiceEnabled(context)) }
    var isDeviceAdminActive by remember { mutableStateOf(isDeviceAdminEnabled(context)) }
    var isAntiPornShieldOn by remember { mutableStateOf(LockSettings.isAntiPornShieldEnabled(context)) }

    val allPermissionsGranted = isShieldActive && isNotificationActive && isDeviceAdminActive

    if (blockedPackage != null) {
        BlockedNotificationDialog(
            packageName = blockedPackage,
            onDismiss = onClearBlockedMessage
        )
    }

    // Fetch installed apps on launch (lazily deferred until shield permission is active to optimize performance)
    LaunchedEffect(allPermissionsGranted) {
        if (allPermissionsGranted && installedApps.isEmpty()) {
            installedApps = withContext(Dispatchers.IO) {
                getInstalledLauncherApps(context)
            }
        }
    }

    // Continuously monitor shield state when in app
    LaunchedEffect(isLockdown) {
        while (true) {
            isShieldActive = isAccessibilityServiceEnabled(context)
            isNotificationActive = isNotificationServiceEnabled(context)
            isDeviceAdminActive = isDeviceAdminEnabled(context)
            delay(2000)
        }
    }

    Box(
        modifier = Modifier
            .fillMaxSize()
            .background(if (isDarkTheme) com.example.ui.theme.FrostedBackgroundDark else com.example.ui.theme.FrostedBackgroundLight)
    ) {
        AnimatedContent(
            targetState = Triple(isLockdown, allPermissionsGranted, Unit),
            transitionSpec = {
                (fadeIn(animationSpec = tween(600)) + scaleIn(initialScale = 0.9f)) togetherWith
                        (fadeOut(animationSpec = tween(600)) + scaleOut(targetScale = 1.1f))
            },
            label = "AppScreenTransition"
        ) { (lockdown, permissionsGranted, _) ->
            if (!permissionsGranted) {
                PermissionGateScreen(
                    isDarkTheme = isDarkTheme,
                    isAccessibilityActive = isShieldActive,
                    isNotificationActive = isNotificationActive,
                    isDeviceAdminActive = isDeviceAdminActive,
                    onEnableAccessibility = {
                        try {
                            if (android.os.Build.VERSION.SDK_INT >= 33) {
                                android.widget.Toast.makeText(context, "إذا تم نقلك إلى 'معلومات التطبيق'، انقر على النقاط الثلاث بالأعلى واختر 'السماح بالإعدادات المقيدة'", android.widget.Toast.LENGTH_LONG).show()
                            }
                            val intent = Intent(Settings.ACTION_ACCESSIBILITY_SETTINGS)
                            context.startActivity(intent)
                        } catch (e: Exception) {
                            e.printStackTrace()
                        }
                    },
                    onEnableNotification = {
                        try {
                            val intent = Intent(Settings.ACTION_NOTIFICATION_LISTENER_SETTINGS)
                            context.startActivity(intent)
                        } catch (e: Exception) {
                            e.printStackTrace()
                        }
                    },
                    onEnableDeviceAdmin = {
                        try {
                            val intent = Intent(android.app.admin.DevicePolicyManager.ACTION_ADD_DEVICE_ADMIN).apply {
                                putExtra(android.app.admin.DevicePolicyManager.EXTRA_DEVICE_ADMIN, ComponentName(context, FocusDeviceAdminReceiver::class.java))
                                putExtra(android.app.admin.DevicePolicyManager.EXTRA_ADD_EXPLANATION, "Required to prevent bypassing the focus lock.")
                            }
                            context.startActivity(intent)
                        } catch (e: Exception) {
                            e.printStackTrace()
                        }
                    },
                    selectedLang = selectedLang,
                    onLangChange = { lang ->
                        LockSettings.setSelectedLanguage(context, lang)
                        selectedLang = lang
                    }
                )
            } else if (lockdown) {
                val activeEndTime = LockSettings.getLockdownEndTime(context)
                val currentRem = maxOf(0L, (activeEndTime - System.currentTimeMillis()) / 1000L).toInt()
                val actualTime = if (currentRem > 0) currentRem else lockdownDurationSecs
                
                LockdownScreen(
                    isDarkTheme = isDarkTheme,
                    durationSecs = actualTime,
                    selectedApps = selectedApps,
                    installedApps = installedApps,
                    onUnlock = {
                        LockSettings.setLockdown(context, false)
                        isLockdown = false
                    }
                )
            } else {
                SetupScreen(
                    isDarkTheme = isDarkTheme,
                    onToggleTheme = onToggleTheme,
                    durationSecs = lockdownDurationSecs,
                    onDurationChange = { lockdownDurationSecs = it },
                    selectedApps = selectedApps,
                    installedApps = installedApps,
                    isShieldActive = isShieldActive,
                    isNotificationActive = isNotificationActive,
                    isAntiPornShieldOn = isAntiPornShieldOn,
                    onAntiPornShieldToggle = { enabled ->
                        LockSettings.setAntiPornShieldEnabled(context, enabled)
                        isAntiPornShieldOn = enabled
                    },
                    onAppToggle = { pkgName ->
                        selectedApps = if (selectedApps.contains(pkgName)) {
                            selectedApps - pkgName
                        } else {
                            selectedApps + pkgName
                        }
                        LockSettings.setBlockedApps(context, selectedApps)
                    },
                    onStart = {
                        if (selectedApps.isNotEmpty() || isAntiPornShieldOn) {
                            LockSettings.setBlockedApps(context, selectedApps)
                            LockSettings.setLockdown(context, true, lockdownDurationSecs)
                            isLockdown = true
                        }
                    },
                    selectedLang = selectedLang,
                    onLangChange = { lang ->
                        LockSettings.setSelectedLanguage(context, lang)
                        selectedLang = lang
                    }
                )
            }
        }
    }
}

@Composable
fun SetupScreen(
    isDarkTheme: Boolean,
    onToggleTheme: () -> Unit,
    durationSecs: Int,
    onDurationChange: (Int) -> Unit,
    selectedApps: Set<String>,
    installedApps: List<DeviceAppInfo>,
    isShieldActive: Boolean,
    isNotificationActive: Boolean,
    isAntiPornShieldOn: Boolean,
    onAntiPornShieldToggle: (Boolean) -> Unit,
    onAppToggle: (String) -> Unit,
    onStart: () -> Unit,
    selectedLang: String,
    onLangChange: (String) -> Unit
) {
    val context = LocalContext.current
    var showAppPicker by remember { mutableStateOf(false) }
    var activeTab by remember { mutableIntStateOf(0) } // 0: Focus, 1: Settings
    var protectionDaysSelected by remember { mutableIntStateOf(1) }

    val layoutDirection = if (selectedLang == "ar") LayoutDirection.Rtl else LayoutDirection.Ltr

    fun t(key: String): String = Translations.get(key, selectedLang)

    CompositionLocalProvider(LocalLayoutDirection provides layoutDirection) {
        Scaffold(
            modifier = Modifier.fillMaxSize(),
            containerColor = Color.Transparent,
            bottomBar = {
                NavigationBar(
                    containerColor = if (isDarkTheme) Color(0xFF0F172A).copy(alpha = 0.95f) else Color.White.copy(alpha = 0.95f),
                    tonalElevation = 8.dp,
                    modifier = Modifier.clip(RoundedCornerShape(topStart = 24.dp, topEnd = 24.dp))
                ) {
                    NavigationBarItem(
                        selected = activeTab == 0,
                        onClick = { activeTab = 0 },
                        icon = {
                            Icon(
                                imageVector = if (activeTab == 0) Icons.Filled.Lock else Icons.Outlined.Lock,
                                contentDescription = t("focus_tab")
                            )
                        },
                        label = {
                            Text(
                                text = t("focus_tab"),
                                style = MaterialTheme.typography.labelMedium,
                                fontWeight = if (activeTab == 0) FontWeight.Bold else FontWeight.Normal
                            )
                        },
                        colors = NavigationBarItemDefaults.colors(
                            selectedIconColor = Color.White,
                            selectedTextColor = com.example.ui.theme.FrostedPrimary,
                            indicatorColor = com.example.ui.theme.FrostedPrimary,
                            unselectedIconColor = if (isDarkTheme) Color(0xFF94A3B8) else Color(0xFF64748B),
                            unselectedTextColor = if (isDarkTheme) Color(0xFF94A3B8) else Color(0xFF64748B)
                        )
                    )
                    
                    NavigationBarItem(
                        selected = activeTab == 1,
                        onClick = { activeTab = 1 },
                        icon = {
                            Icon(
                                imageVector = if (activeTab == 1) Icons.Filled.Settings else Icons.Outlined.Settings,
                                contentDescription = t("settings_tab")
                            )
                        },
                        label = {
                            Text(
                                text = t("settings_tab"),
                                style = MaterialTheme.typography.labelMedium,
                                fontWeight = if (activeTab == 1) FontWeight.Bold else FontWeight.Normal
                            )
                        },
                        colors = NavigationBarItemDefaults.colors(
                            selectedIconColor = Color.White,
                            selectedTextColor = com.example.ui.theme.FrostedPrimary,
                            indicatorColor = com.example.ui.theme.FrostedPrimary,
                            unselectedIconColor = if (isDarkTheme) Color(0xFF94A3B8) else Color(0xFF64748B),
                            unselectedTextColor = if (isDarkTheme) Color(0xFF94A3B8) else Color(0xFF64748B)
                        )
                    )
                }
            }
        ) { innerPadding ->
            Column(
                modifier = Modifier
                    .padding(innerPadding)
                    .fillMaxSize()
                    .verticalScroll(rememberScrollState())
                    .padding(horizontal = 24.dp),
                horizontalAlignment = Alignment.CenterHorizontally
            ) {
                if (activeTab == 0) {
                    Spacer(modifier = Modifier.height(16.dp))
                    HeaderBlock(isDarkTheme, onToggleTheme, selectedLang)
                Spacer(modifier = Modifier.height(24.dp))
                
                // Apps selected Card
                Box(
                    modifier = Modifier
                        .fillMaxWidth()
                        .clip(RoundedCornerShape(32.dp))
                        .background(if (isDarkTheme) Color.White.copy(alpha = 0.05f) else Color.Black.copy(alpha = 0.03f))
                        .border(1.dp, if (isDarkTheme) Color.White.copy(alpha = 0.08f) else Color.Black.copy(alpha = 0.05f), RoundedCornerShape(32.dp))
                        .padding(24.dp)
                ) {
                    Column {
                        Row(
                            modifier = Modifier
                                .fillMaxWidth()
                                .padding(bottom = 16.dp),
                            horizontalArrangement = Arrangement.SpaceBetween,
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Text(
                                text = t("shielded_apps"),
                                style = MaterialTheme.typography.titleMedium,
                                color = MaterialTheme.colorScheme.onBackground,
                                fontWeight = FontWeight.Bold
                            )
                            Text(
                                text = "${selectedApps.size} ${t("locked_count")}",
                                style = MaterialTheme.typography.labelSmall,
                                color = com.example.ui.theme.FrostedPrimary,
                                fontWeight = FontWeight.SemiBold,
                                modifier = Modifier
                                    .clip(RoundedCornerShape(8.dp))
                                    .background(com.example.ui.theme.FrostedPrimary.copy(alpha = 0.15f))
                                    .padding(horizontal = 8.dp, vertical = 4.dp)
                            )
                        }

                        // Selected apps previews
                        val filteredSelected = installedApps.filter { selectedApps.contains(it.packageName) }
                        
                        if (filteredSelected.isEmpty()) {
                            Box(
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .height(120.dp),
                                contentAlignment = Alignment.Center
                            ) {
                                Text(
                                    text = t("no_apps_targeted"),
                                    style = MaterialTheme.typography.bodyMedium,
                                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                                    textAlign = TextAlign.Center
                                )
                            }
                        } else {
                            // Display up to 6 selected apps in small dynamic badges
                            FlowRow(
                                horizontalArrangement = Arrangement.spacedBy(8.dp),
                                verticalArrangement = Arrangement.spacedBy(8.dp),
                                modifier = Modifier.fillMaxWidth()
                            ) {
                                filteredSelected.take(8).forEach { app ->
                                    SelectedBadge(app) { onAppToggle(app.packageName) }
                                }
                                if (filteredSelected.size > 8) {
                                    Box(
                                        modifier = Modifier
                                            .clip(RoundedCornerShape(12.dp))
                                            .background(Color.White.copy(alpha = 0.08f))
                                            .padding(horizontal = 10.dp, vertical = 6.dp)
                                    ) {
                                        Text(
                                            text = "+${filteredSelected.size - 8} more",
                                            style = MaterialTheme.typography.labelSmall,
                                            color = MaterialTheme.colorScheme.onSurface
                                        )
                                    }
                                }
                            }
                            Spacer(modifier = Modifier.height(16.dp))
                        }
                        
                        Button(
                            onClick = { showAppPicker = true },
                            modifier = Modifier
                                .fillMaxWidth()
                                .height(48.dp),
                            colors = ButtonDefaults.buttonColors(
                                containerColor = if (isDarkTheme) Color.White.copy(alpha = 0.08f) else Color.Black.copy(alpha = 0.08f),
                                contentColor = MaterialTheme.colorScheme.onBackground
                            ),
                            shape = RoundedCornerShape(16.dp)
                        ) {
                            Icon(imageVector = Icons.Filled.Add, contentDescription = null, modifier = Modifier.size(16.dp))
                            Spacer(modifier = Modifier.width(8.dp))
                            Text(text = t("add_edit_list"), style = MaterialTheme.typography.labelLarge)
                        }
                    }
                }
                
                Spacer(modifier = Modifier.height(24.dp))

                // Adult & VPN Anti-Relapse Shield Card
                Box(
                    modifier = Modifier
                        .fillMaxWidth()
                        .clip(RoundedCornerShape(32.dp))
                        .background(
                            if (isDarkTheme) {
                                Brush.verticalGradient(
                                    listOf(
                                        Color(0xFFEF4444).copy(alpha = 0.12f),
                                        Color(0xFF7F1D1D).copy(alpha = 0.08f)
                                    )
                                )
                            } else {
                                Brush.verticalGradient(
                                    listOf(
                                        Color(0xFFFEF2F2).copy(alpha = 0.9f),
                                        Color(0xFFFCA5A5).copy(alpha = 0.3f)
                                    )
                                )
                            }
                        )
                        .border(
                            width = 1.dp,
                            brush = Brush.horizontalGradient(
                                if (isDarkTheme) {
                                    listOf(Color(0xFFEF4444).copy(alpha = 0.35f), Color(0xFF7F1D1D).copy(alpha = 0.1f))
                                } else {
                                    listOf(Color(0xFFEF4444).copy(alpha = 0.25f), Color(0xFFFCA5A5).copy(alpha = 0.4f))
                                }
                            ),
                            shape = RoundedCornerShape(32.dp)
                        )
                        .padding(24.dp)
                ) {
                    Column {
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.SpaceBetween,
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Row(verticalAlignment = Alignment.CenterVertically, modifier = Modifier.weight(1f)) {
                                Box(
                                    modifier = Modifier
                                        .size(40.dp)
                                        .clip(CircleShape)
                                        .background(if (isDarkTheme) Color(0xFFEF4444).copy(alpha = 0.2f) else Color(0xFFEF4444).copy(alpha = 0.12f)),
                                    contentAlignment = Alignment.Center
                                ) {
                                    Icon(
                                        imageVector = Icons.Filled.Lock,
                                        contentDescription = "Shield Active Icon",
                                        tint = Color(0xFFEF4444),
                                        modifier = Modifier.size(20.dp)
                                    )
                                }
                                Spacer(modifier = Modifier.width(12.dp))
                                Column {
                                    Text(
                                        text = t("shield_title"),
                                        style = MaterialTheme.typography.titleMedium.copy(fontWeight = FontWeight.Bold),
                                        color = if (isDarkTheme) Color.White else Color(0xFF7F1D1D)
                                    )
                                    Text(
                                        text = t("shield_subtitle"),
                                        style = MaterialTheme.typography.labelSmall,
                                        color = if (isDarkTheme) Color(0xFFFCA5A5) else Color(0xFF991B1B)
                                    )
                                }
                            }
                            Switch(
                                checked = isAntiPornShieldOn,
                                onCheckedChange = { onAntiPornShieldToggle(it) },
                                colors = SwitchDefaults.colors(
                                    checkedThumbColor = Color.White,
                                    checkedTrackColor = Color(0xFFEF4444),
                                    uncheckedThumbColor = if (isDarkTheme) Color(0xFF64748B) else Color(0xFF94A3B8),
                                    uncheckedTrackColor = if (isDarkTheme) Color.White.copy(alpha = 0.1f) else Color.Black.copy(alpha = 0.1f)
                                )
                            )
                        }
                        Spacer(modifier = Modifier.height(12.dp))
                        Text(
                            text = t("shield_desc"),
                            style = MaterialTheme.typography.bodySmall.copy(lineHeight = 18.sp),
                            color = if (isDarkTheme) Color(0xFFF3F4F6).copy(alpha = 0.8f) else Color(0xFF1F2937).copy(alpha = 0.9f)
                        )
                    }
                }
                
                Spacer(modifier = Modifier.height(24.dp))
                
                DurationSelector(isDarkTheme, durationSecs, onDurationChange, selectedLang)
                
                Spacer(modifier = Modifier.height(32.dp))
                
                StartButton(isDarkTheme, onStart, enabled = selectedApps.isNotEmpty() || isAntiPornShieldOn, selectedLang = selectedLang)
                
                Spacer(modifier = Modifier.height(32.dp))
                } else {
                    // TAB 1: SETTINGS SCREEN
                    Spacer(modifier = Modifier.height(16.dp))
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.Start,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Column {
                            Text(
                                text = t("settings_tab"),
                                style = MaterialTheme.typography.headlineLarge,
                                fontWeight = FontWeight.ExtraBold,
                                color = MaterialTheme.colorScheme.onBackground
                            )
                            Text(
                                text = t("app_subtitle"),
                                style = MaterialTheme.typography.bodyMedium,
                                color = MaterialTheme.colorScheme.onSurfaceVariant
                            )
                        }
                    }
                    
                    Spacer(modifier = Modifier.height(24.dp))
                    
                    // Theme setting Card
                    Box(
                        modifier = Modifier
                            .fillMaxWidth()
                            .clip(RoundedCornerShape(32.dp))
                            .background(if (isDarkTheme) Color.White.copy(alpha = 0.05f) else Color.Black.copy(alpha = 0.03f))
                            .border(1.dp, if (isDarkTheme) Color.White.copy(alpha = 0.08f) else Color.Black.copy(alpha = 0.05f), RoundedCornerShape(32.dp))
                            .padding(24.dp)
                    ) {
                        Column {
                            Row(
                                modifier = Modifier.fillMaxWidth(),
                                horizontalArrangement = Arrangement.SpaceBetween,
                                verticalAlignment = Alignment.CenterVertically
                            ) {
                                Row(verticalAlignment = Alignment.CenterVertically) {
                                    Icon(
                                        imageVector = Icons.Filled.Palette,
                                        contentDescription = null,
                                        tint = com.example.ui.theme.FrostedPrimary,
                                        modifier = Modifier.size(24.dp)
                                    )
                                    Spacer(modifier = Modifier.width(12.dp))
                                    Text(
                                        text = t("theme_setting"),
                                        style = MaterialTheme.typography.titleMedium,
                                        fontWeight = FontWeight.Bold,
                                        color = MaterialTheme.colorScheme.onBackground
                                    )
                                }
                                
                                Row(
                                    verticalAlignment = Alignment.CenterVertically,
                                    modifier = Modifier
                                        .clip(RoundedCornerShape(16.dp))
                                        .background(if (isDarkTheme) Color.White.copy(alpha = 0.08f) else Color.Black.copy(alpha = 0.08f))
                                        .clickable { onToggleTheme() }
                                        .padding(horizontal = 12.dp, vertical = 6.dp)
                                ) {
                                    Icon(
                                        imageVector = if (isDarkTheme) Icons.Filled.DarkMode else Icons.Filled.LightMode,
                                        contentDescription = null,
                                        tint = com.example.ui.theme.FrostedPrimary,
                                        modifier = Modifier.size(16.dp)
                                    )
                                    Spacer(modifier = Modifier.width(6.dp))
                                    Text(
                                        text = if (isDarkTheme) t("dark_mode") else t("light_mode"),
                                        style = MaterialTheme.typography.labelMedium.copy(fontWeight = FontWeight.Bold),
                                        color = MaterialTheme.colorScheme.onBackground
                                    )
                                }
                            }
                        }
                    }
                    
                    Spacer(modifier = Modifier.height(16.dp))
                    
                    // Language Selector Card
                    Box(
                        modifier = Modifier
                            .fillMaxWidth()
                            .clip(RoundedCornerShape(32.dp))
                            .background(if (isDarkTheme) Color.White.copy(alpha = 0.05f) else Color.Black.copy(alpha = 0.03f))
                            .border(1.dp, if (isDarkTheme) Color.White.copy(alpha = 0.08f) else Color.Black.copy(alpha = 0.05f), RoundedCornerShape(32.dp))
                            .padding(24.dp)
                    ) {
                        Column {
                            Row(verticalAlignment = Alignment.CenterVertically, modifier = Modifier.fillMaxWidth()) {
                                Icon(
                                    imageVector = Icons.Filled.Language,
                                    contentDescription = null,
                                    tint = com.example.ui.theme.FrostedPrimary,
                                    modifier = Modifier.size(24.dp)
                                )
                                Spacer(modifier = Modifier.width(12.dp))
                                Text(
                                    text = t("language_selection"),
                                    style = MaterialTheme.typography.titleMedium,
                                    fontWeight = FontWeight.Bold,
                                    color = MaterialTheme.colorScheme.onBackground
                                )
                            }
                            Spacer(modifier = Modifier.height(16.dp))
                            FlowRow(
                                horizontalArrangement = Arrangement.spacedBy(8.dp),
                                verticalArrangement = Arrangement.spacedBy(8.dp),
                                modifier = Modifier.fillMaxWidth()
                            ) {
                                Translations.LANGUAGES.forEach { lang ->
                                    val isSelected = lang.code == selectedLang
                                    Box(
                                        modifier = Modifier
                                            .clip(RoundedCornerShape(16.dp))
                                            .background(
                                                if (isSelected) {
                                                    com.example.ui.theme.FrostedPrimary
                                                } else {
                                                    if (isDarkTheme) Color(0xFF334155).copy(alpha = 0.3f) else Color(0xFFE2E8F0)
                                                }
                                            )
                                            .clickable {
                                                onLangChange(lang.code)
                                            }
                                            .padding(horizontal = 14.dp, vertical = 8.dp),
                                        contentAlignment = Alignment.Center
                                    ) {
                                        Row(verticalAlignment = Alignment.CenterVertically) {
                                            Text(text = lang.flag, style = MaterialTheme.typography.bodyMedium)
                                            Spacer(modifier = Modifier.width(6.dp))
                                            Text(
                                                text = lang.name,
                                                style = MaterialTheme.typography.labelMedium.copy(fontWeight = FontWeight.Bold),
                                                color = if (isSelected) Color.White else (if (isDarkTheme) Color.White.copy(alpha = 0.8f) else Color(0xFF475569))
                                            )
                                        }
                                    }
                                }
                            }
                        }
                    }
                    
                    Spacer(modifier = Modifier.height(16.dp))
                    
                    // Default Session Duration Card
                    Box(
                        modifier = Modifier
                            .fillMaxWidth()
                            .clip(RoundedCornerShape(32.dp))
                            .background(if (isDarkTheme) Color.White.copy(alpha = 0.05f) else Color.Black.copy(alpha = 0.03f))
                            .border(1.dp, if (isDarkTheme) Color.White.copy(alpha = 0.08f) else Color.Black.copy(alpha = 0.05f), RoundedCornerShape(32.dp))
                            .padding(24.dp)
                    ) {
                        Column {
                            Row(verticalAlignment = Alignment.CenterVertically) {
                                Icon(
                                    imageVector = Icons.Filled.Update,
                                    contentDescription = null,
                                    tint = com.example.ui.theme.FrostedPrimary,
                                    modifier = Modifier.size(24.dp)
                                )
                                Spacer(modifier = Modifier.width(12.dp))
                                Text(
                                    text = t("default_lock_duration_setting"),
                                    style = MaterialTheme.typography.titleMedium,
                                    fontWeight = FontWeight.Bold,
                                    color = MaterialTheme.colorScheme.onBackground
                                )
                            }
                            Spacer(modifier = Modifier.height(4.dp))
                            Text(
                                text = t("default_lock_duration_desc"),
                                style = MaterialTheme.typography.bodySmall,
                                color = if (isDarkTheme) Color(0xFF94A3B8) else Color(0xFF64748B)
                            )
                            Spacer(modifier = Modifier.height(16.dp))
                            
                            val presets = listOf(
                                Pair("10s", 10),
                                Pair("5m", 5 * 60),
                                Pair("25m", 25 * 60),
                                Pair("1h", 60 * 60),
                                Pair("2h", 2 * 60 * 60)
                            )
                            Row(
                                modifier = Modifier.fillMaxWidth(),
                                horizontalArrangement = Arrangement.spacedBy(8.dp)
                            ) {
                                presets.forEach { (label, secs) ->
                                    val isChosen = durationSecs == secs
                                    Box(
                                        modifier = Modifier
                                            .weight(1f)
                                            .clip(RoundedCornerShape(12.dp))
                                            .background(
                                                if (isChosen) com.example.ui.theme.FrostedPrimary
                                                else (if (isDarkTheme) Color.White.copy(alpha = 0.05f) else Color.Black.copy(alpha = 0.05f))
                                            )
                                            .clickable {
                                                LockSettings.setDefaultDurationSecs(context, secs)
                                                onDurationChange(secs)
                                            }
                                            .padding(vertical = 10.dp),
                                        contentAlignment = Alignment.Center
                                    ) {
                                        Text(
                                            text = label,
                                            style = MaterialTheme.typography.labelMedium.copy(fontWeight = FontWeight.Bold),
                                            color = if (isChosen) Color.White else (if (isDarkTheme) Color.White.copy(alpha = 0.8f) else Color(0xFF475569))
                                        )
                                    }
                                }
                            }
                        }
                    }
                    
                    Spacer(modifier = Modifier.height(16.dp))
                    
                    // Uninstall Protection Card
                    var protectionEnd by remember(activeTab) { mutableStateOf(LockSettings.getUninstallProtectionEndTime(context)) }
                    val protectionActive = protectionEnd > System.currentTimeMillis()
                    
                    if (!protectionActive) {
                        Box(
                            modifier = Modifier
                                .fillMaxWidth()
                                .clip(RoundedCornerShape(32.dp))
                                .background(
                                    if (isDarkTheme) {
                                        Brush.verticalGradient(
                                            listOf(
                                                Color(0xFF6366F1).copy(alpha = 0.12f),
                                                Color(0xFF312E81).copy(alpha = 0.08f)
                                            )
                                        )
                                    } else {
                                        Brush.verticalGradient(
                                            listOf(
                                                Color(0xFFEEF2FF).copy(alpha = 0.9f),
                                                Color(0xFFC7D2FE).copy(alpha = 0.3f)
                                            )
                                        )
                                    }
                                )
                                .border(
                                    width = 1.dp,
                                    brush = Brush.horizontalGradient(
                                        if (isDarkTheme) {
                                            listOf(Color(0xFF818CF8).copy(alpha = 0.35f), Color(0xFF312E81).copy(alpha = 0.1f))
                                        } else {
                                            listOf(Color(0xFF6366F1).copy(alpha = 0.25f), Color(0xFFC7D2FE).copy(alpha = 0.4f))
                                        }
                                    ),
                                    shape = RoundedCornerShape(32.dp)
                                )
                                .padding(24.dp)
                        ) {
                            Column {
                                Row(
                                    modifier = Modifier.fillMaxWidth(),
                                    verticalAlignment = Alignment.CenterVertically
                                ) {
                                    Box(
                                        modifier = Modifier
                                            .size(40.dp)
                                            .clip(CircleShape)
                                            .background(if (isDarkTheme) Color(0xFF6366F1).copy(alpha = 0.2f) else Color(0xFF6366F1).copy(alpha = 0.12f)),
                                        contentAlignment = Alignment.Center
                                    ) {
                                        Icon(
                                            imageVector = Icons.Filled.Security,
                                            contentDescription = null,
                                            tint = Color(0xFF6366F1),
                                            modifier = Modifier.size(20.dp)
                                        )
                                    }
                                    Spacer(modifier = Modifier.width(12.dp))
                                    Column {
                                        Text(
                                            text = t("uninstall_protection_setting"),
                                            style = MaterialTheme.typography.titleMedium.copy(fontWeight = FontWeight.Bold),
                                            color = if (isDarkTheme) Color.White else Color(0xFF312E81)
                                        )
                                    }
                                }
                                Spacer(modifier = Modifier.height(12.dp))
                                Text(
                                    text = t("uninstall_protection_desc"),
                                    style = MaterialTheme.typography.bodySmall.copy(lineHeight = 18.sp),
                                    color = if (isDarkTheme) Color(0xFFE2E8F0) else Color(0xFF334155)
                                )
                                
                                Spacer(modifier = Modifier.height(16.dp))
                                
                                // Beautiful 3D scrollable time picker for DAYS only
                                Box(
                                    modifier = Modifier
                                        .fillMaxWidth()
                                        .height(168.dp),
                                    contentAlignment = Alignment.TopCenter
                                ) {
                                    Box(
                                        modifier = Modifier
                                            .fillMaxWidth(0.85f)
                                            .height(52.dp)
                                            .offset(y = 40.dp)
                                            .clip(RoundedCornerShape(12.dp))
                                            .background(
                                                if (isDarkTheme) Color(0xFF6366F1).copy(alpha = 0.12f) else Color(0xFF6366F1).copy(alpha = 0.05f)
                                            )
                                    )

                                    Row(
                                        modifier = Modifier.fillMaxSize(),
                                        horizontalArrangement = Arrangement.Center,
                                        verticalAlignment = Alignment.Top
                                    ) {
                                        TimeDrumColumn(
                                            value = protectionDaysSelected,
                                            labelArabic = "أيام",
                                            labelEnglish = "DAYS",
                                            range = 1..365,
                                            isDarkTheme = isDarkTheme,
                                            onValueChange = { newDays ->
                                                protectionDaysSelected = newDays
                                            }
                                        )
                                    }
                                }
                                
                                Spacer(modifier = Modifier.height(20.dp))
                                
                                // Activation action button
                                Button(
                                    onClick = {
                                        val durationMs = protectionDaysSelected * 24L * 60L * 60L * 1000L
                                        val end = System.currentTimeMillis() + durationMs
                                        LockSettings.setUninstallProtectionEndTime(context, end)
                                        protectionEnd = end
                                    },
                                    modifier = Modifier
                                        .fillMaxWidth()
                                        .height(48.dp),
                                    colors = ButtonDefaults.buttonColors(
                                        containerColor = Color(0xFF6366F1)
                                    ),
                                    shape = RoundedCornerShape(16.dp)
                                ) {
                                    Icon(
                                        imageVector = Icons.Filled.Check,
                                        contentDescription = null,
                                        tint = Color.White,
                                        modifier = Modifier.size(18.dp)
                                    )
                                    Spacer(modifier = Modifier.width(8.dp))
                                    Text(
                                        text = t("confirm_uninstall_protection"),
                                        style = MaterialTheme.typography.labelLarge.copy(fontWeight = FontWeight.Bold),
                                        color = Color.White
                                    )
                                }
                            }
                        }
                    }
                }
                
                Spacer(modifier = Modifier.height(32.dp))
            }
        }
    }

    if (showAppPicker) {
        AppPickerDialog(
            installedApps = installedApps,
            selectedApps = selectedApps,
            onAppToggle = onAppToggle,
            onClose = { showAppPicker = false }
        )
    }
}

@Composable
fun SelectedBadge(app: DeviceAppInfo, onRemove: () -> Unit) {
    val context = LocalContext.current
    // Inside the app itself, we always display the true labels, colors and icons to make configuring simple and clear.
    val displayLabel = app.label
    val displayColor = app.color

    Row(
        verticalAlignment = Alignment.CenterVertically,
        modifier = Modifier
            .clip(RoundedCornerShape(12.dp))
            .background(displayColor.copy(alpha = 0.15f))
            .border(1.dp, displayColor.copy(alpha = 0.3f), RoundedCornerShape(12.dp))
            .clickable { onRemove() }
            .padding(horizontal = 10.dp, vertical = 6.dp)
    ) {
        AppIcon(
            packageName = app.packageName,
            modifier = Modifier
                .size(20.dp)
                .clip(CircleShape),
            fallbackLabel = displayLabel,
            fallbackColor = displayColor,
            isStealthMode = false,
            textStyle = MaterialTheme.typography.labelSmall.copy(fontSize = 11.sp, fontWeight = FontWeight.Bold)
        )
        Spacer(modifier = Modifier.width(6.dp))
        Text(
            text = displayLabel,
            style = MaterialTheme.typography.labelSmall,
            color = displayColor,
            fontWeight = FontWeight.Medium
        )
        Spacer(modifier = Modifier.width(4.dp))
        Icon(
            imageVector = Icons.Filled.Close,
            contentDescription = "Remove",
            tint = displayColor,
            modifier = Modifier.size(12.dp)
        )
    }
}

@Composable
fun ShieldStatusBanner(isActive: Boolean, onClick: () -> Unit) {
    val containerColor by animateColorAsState(
        targetValue = if (isActive) Color(0x1510B981) else Color(0x15EF4444),
        label = "bannerBg"
    )
    val borderColor by animateColorAsState(
        targetValue = if (isActive) Color(0x4410B981) else Color(0x44EF4444),
        label = "bannerBorder"
    )

    Box(
        modifier = Modifier
            .fillMaxWidth()
            .clip(RoundedCornerShape(20.dp))
            .background(containerColor)
            .border(1.dp, borderColor, RoundedCornerShape(20.dp))
            .clickable { onClick() }
            .padding(16.dp)
    ) {
        Row(
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.SpaceBetween,
            modifier = Modifier.fillMaxWidth()
        ) {
            Row(
                verticalAlignment = Alignment.CenterVertically,
                modifier = Modifier.weight(1f)
            ) {
                Icon(
                    imageVector = if (isActive) Icons.Filled.CheckCircle else Icons.Filled.Warning,
                    contentDescription = null,
                    tint = if (isActive) Color(0xFF10B981) else Color(0xFFEF4444),
                    modifier = Modifier.size(24.dp)
                )
                Spacer(modifier = Modifier.width(12.dp))
                Column {
                    Text(
                        text = if (isActive) "ActiveShield Enabled" else "ActiveShield Disabled",
                        style = MaterialTheme.typography.titleSmall,
                        fontWeight = FontWeight.Bold,
                        color = if (isActive) Color(0xFF10B981) else Color(0xFFEF4444)
                    )
                    Text(
                        text = if (isActive) "Operating system connection active." else "Grant permission to enforce phone-level app blocking.",
                        style = MaterialTheme.typography.labelSmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                }
            }
            Spacer(modifier = Modifier.width(8.dp))
            
            if (!isActive) {
                Box(
                    modifier = Modifier
                        .clip(RoundedCornerShape(12.dp))
                        .background(Color(0xFFEF4444))
                        .padding(horizontal = 12.dp, vertical = 6.dp)
                ) {
                    Text(
                        text = "ENABLE",
                        style = MaterialTheme.typography.labelMedium.copy(fontSize = 11.sp),
                        fontWeight = FontWeight.Bold,
                        color = Color.White
                    )
                }
            }
        }
    }
}


// Complete Searchable App Selector Overlay Modal/Dialog
@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun AppPickerDialog(
    installedApps: List<DeviceAppInfo>,
    selectedApps: Set<String>,
    onAppToggle: (String) -> Unit,
    onClose: () -> Unit
) {
    var searchQuery by remember { mutableStateOf("") }
    val filteredList = remember(searchQuery, installedApps) {
        installedApps.filter { it.label.contains(searchQuery, ignoreCase = true) }
    }

    Dialog(
        onDismissRequest = onClose,
        properties = DialogProperties(usePlatformDefaultWidth = false)
    ) {
        Box(
            modifier = Modifier
                .fillMaxSize()
                .background(Color.Black.copy(alpha = 0.5f))
                .padding(24.dp),
            contentAlignment = Alignment.Center
        ) {
            Surface(
                modifier = Modifier
                    .fillMaxWidth()
                    .fillMaxHeight(0.85f),
                shape = RoundedCornerShape(28.dp),
                color = MaterialTheme.colorScheme.surface,
                tonalElevation = 8.dp
            ) {
                CompositionLocalProvider(LocalLayoutDirection provides LayoutDirection.Ltr) {
                    Column(
                        modifier = Modifier
                            .fillMaxSize()
                            .padding(24.dp)
                    ) {
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.SpaceBetween,
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Text(
                                text = "Select Distractions",
                                style = MaterialTheme.typography.headlineSmall,
                                fontWeight = FontWeight.Bold
                            )
                            IconButton(onClick = onClose) {
                                Icon(imageVector = Icons.Filled.Close, contentDescription = "Close")
                            }
                        }
                        
                        Spacer(modifier = Modifier.height(16.dp))
                        
                        OutlinedTextField(
                            value = searchQuery,
                            onValueChange = { searchQuery = it },
                            modifier = Modifier.fillMaxWidth(),
                            placeholder = { Text("Search system applications...") },
                            leadingIcon = { Icon(imageVector = Icons.Filled.Search, contentDescription = null) },
                            singleLine = true,
                            maxLines = 1,
                            colors = OutlinedTextFieldDefaults.colors(
                                focusedBorderColor = com.example.ui.theme.FrostedPrimary,
                                unfocusedBorderColor = MaterialTheme.colorScheme.outline.copy(alpha = 0.3f)
                            ),
                            shape = RoundedCornerShape(16.dp)
                        )
                        
                        Spacer(modifier = Modifier.height(16.dp))
                        
                        Box(modifier = Modifier.weight(1f)) {
                            if (filteredList.isEmpty()) {
                                Box(modifier = Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
                                    Text(
                                        text = "No matching apps found.",
                                        style = MaterialTheme.typography.bodyMedium,
                                        color = MaterialTheme.colorScheme.onSurfaceVariant
                                    )
                                }
                            } else {
                                LazyColumn(
                                    modifier = Modifier.fillMaxSize(),
                                    verticalArrangement = Arrangement.spacedBy(8.dp)
                                ) {
                                    items(filteredList, key = { it.packageName }) { app ->
                                        val isChecked = selectedApps.contains(app.packageName)
                                        AppSelectionRow(
                                            app = app,
                                            isChecked = isChecked,
                                            onToggle = { onAppToggle(app.packageName) }
                                        )
                                    }
                                }
                            }
                        }
                        
                        Spacer(modifier = Modifier.height(16.dp))
                        
                        Button(
                            onClick = onClose,
                            modifier = Modifier
                                .fillMaxWidth()
                                .height(52.dp),
                            colors = ButtonDefaults.buttonColors(
                                containerColor = com.example.ui.theme.FrostedPrimary,
                                contentColor = Color.White
                            ),
                            shape = RoundedCornerShape(16.dp)
                        ) {
                            Text(
                                text = "APPLY (${selectedApps.size} APPS)",
                                style = MaterialTheme.typography.titleMedium.copy(fontSize = 16.sp),
                                fontWeight = FontWeight.Bold
                            )
                        }
                    }
                }
            }
        }
    }
}

@Composable
fun AppSelectionRow(
    app: DeviceAppInfo,
    isChecked: Boolean,
    onToggle: () -> Unit
) {
    // Inside the app, we always display the true labels, colors and icons to make selection and management clear.
    val displayLabel = app.label
    val displayColor = app.color

    Row(
        modifier = Modifier
            .fillMaxWidth()
            .clip(RoundedCornerShape(16.dp))
            .clickable { onToggle() }
            .padding(vertical = 10.dp, horizontal = 12.dp),
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.SpaceBetween
    ) {
        Row(verticalAlignment = Alignment.CenterVertically) {
            AppIcon(
                packageName = app.packageName,
                modifier = Modifier
                    .size(40.dp)
                    .clip(CircleShape),
                fallbackLabel = displayLabel,
                fallbackColor = displayColor,
                isStealthMode = false
            )
            Spacer(modifier = Modifier.width(16.dp))
            Text(
                text = displayLabel,
                style = MaterialTheme.typography.bodyLarge,
                fontWeight = FontWeight.SemiBold,
                color = MaterialTheme.colorScheme.onSurface
            )
        }
        Checkbox(
            checked = isChecked,
            onCheckedChange = { onToggle() },
            colors = CheckboxDefaults.colors(
                checkedColor = com.example.ui.theme.FrostedPrimary
            )
        )
    }
}

// Horizontal flow row implementation
@OptIn(androidx.compose.foundation.layout.ExperimentalLayoutApi::class)
@Composable
fun FlowRow(
    horizontalArrangement: Arrangement.Horizontal,
    verticalArrangement: Arrangement.Vertical,
    modifier: Modifier = Modifier,
    content: @Composable () -> Unit
) {
    androidx.compose.foundation.layout.FlowRow(
        modifier = modifier,
        horizontalArrangement = horizontalArrangement,
        verticalArrangement = verticalArrangement
    ) {
        content()
    }
}

@Composable
fun HeaderBlock(isDarkTheme: Boolean, onToggleTheme: () -> Unit, selectedLang: String) {
    Row(
        modifier = Modifier.fillMaxWidth(),
        horizontalArrangement = Arrangement.SpaceBetween,
        verticalAlignment = Alignment.CenterVertically
    ) {
        Column {
            Text(
                text = Translations.get("app_title", selectedLang),
                style = MaterialTheme.typography.headlineLarge,
                fontWeight = FontWeight.ExtraBold,
                color = MaterialTheme.colorScheme.onBackground
            )
            Text(
                text = Translations.get("app_subtitle", selectedLang),
                style = MaterialTheme.typography.bodyMedium,
                color = MaterialTheme.colorScheme.onSurfaceVariant
            )
        }
        ThemeToggleButton(isDarkTheme, onToggleTheme)
    }
}

@Composable
fun ThemeToggleButton(isDarkTheme: Boolean, onToggleTheme: () -> Unit) {
    val transition = updateTransition(targetState = isDarkTheme, label = "ThemeTransition")
    val thumbOffset by transition.animateDp(
        transitionSpec = { spring(dampingRatio = Spring.DampingRatioLowBouncy, stiffness = Spring.StiffnessLow) },
        label = "ThumbOffset"
    ) { dark ->
        if (dark) 24.dp else 0.dp
    }
    
    val containerBg by animateColorAsState(
        targetValue = if (isDarkTheme) Color(0x336366F1) else Color(0x14000000),
        label = "ContainerBg"
    )

    Box(
        modifier = Modifier
            .width(56.dp)
            .height(32.dp)
            .clip(RoundedCornerShape(16.dp))
            .background(containerBg)
            .border(1.dp, if (isDarkTheme) Color(0x33FFFFFF) else Color(0x1B000000), RoundedCornerShape(16.dp))
            .clickable { onToggleTheme() }
            .padding(4.dp),
        contentAlignment = Alignment.CenterStart
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 4.dp),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically
        ) {
            Icon(
                imageVector = Icons.Filled.LightMode,
                contentDescription = null,
                tint = if (isDarkTheme) Color.White.copy(alpha = 0.3f) else Color(0xFFF59E0B),
                modifier = Modifier.size(12.dp)
            )
            Icon(
                imageVector = Icons.Filled.DarkMode,
                contentDescription = null,
                tint = if (isDarkTheme) Color(0xFFA78BFA) else Color.Black.copy(alpha = 0.15f),
                modifier = Modifier.size(12.dp)
            )
        }
        
        Box(
            modifier = Modifier
                .offset(x = thumbOffset)
                .size(24.dp)
                .clip(CircleShape)
                .background(
                    brush = Brush.radialGradient(
                        colors = if (isDarkTheme) {
                            listOf(Color(0xFFA78BFA), Color(0xFF6366F1))
                        } else {
                            listOf(Color(0xFFFCD34D), Color(0xFFF59E0B))
                        }
                    )
                ),
            contentAlignment = Alignment.Center
        ) {
            Icon(
                imageVector = if (isDarkTheme) Icons.Filled.DarkMode else Icons.Filled.LightMode,
                contentDescription = null,
                tint = Color.White,
                modifier = Modifier.size(14.dp)
            )
        }
    }
}

@Composable
fun DurationSelector(isDarkTheme: Boolean, durationSecs: Int, onDurationChange: (Int) -> Unit, selectedLang: String) {
    val context = LocalContext.current
    var presets by remember { mutableStateOf(LockSettings.getSavedPresets(context)) }
    
    val hours = durationSecs / 3600
    val minutes = (durationSecs % 3600) / 60
    val seconds = durationSecs % 60
    
    var showAddPresetDialog by remember { mutableStateOf(false) }
    var newPresetName by remember { mutableStateOf("") }

    val tTitle = Translations.get("timer_title", selectedLang)
    val tSubtitle = Translations.get("timer_subtitle", selectedLang)
    val tSavePreset = Translations.get("save_preset", selectedLang)
    val tPresetsTitle = Translations.get("presets_title", selectedLang)

    fun trans(key: String): String {
        return when (key) {
            "save_cust_preset" -> when (selectedLang) {
                "ar" -> "حفظ كإعداد مسبق مخصص"
                "es" -> "Guardar ajuste personalizado"
                "fr" -> "Enregistrer préréglage"
                "hi" -> "कस्टम प्रीसेट सहेजें"
                else -> "Save Custom Preset"
            }
            "save_cust_preset_desc" -> when (selectedLang) {
                "ar" -> "سيتم حفظ الوقت الحالي (${hours}س و ${minutes}د و ${seconds}ث) للوصول السريع مستقبلاً."
                "es" -> "Se guardará el tiempo actual (${hours}h ${minutes}m ${seconds}s) para acceso rápido."
                "fr" -> "Le temps actuel (${hours}h ${minutes}m ${seconds}s) sera sauvegardé."
                "hi" -> "त्वरित पहुँच के लिए वर्तमान समय (${hours}घं ${minutes}मि ${seconds}से) सहेजा जाएगा।"
                else -> "The current time (${hours}h ${minutes}m ${seconds}s) will be saved for quick access."
            }
            "preset_name_label" -> when (selectedLang) {
                "ar" -> "اسم الإعداد المسبق"
                "es" -> "Nombre del ajuste"
                "fr" -> "Nom du préréglage"
                "hi" -> "प्रीसेट का नाम"
                else -> "Preset Name"
            }
            "cancel" -> when (selectedLang) {
                "ar" -> "إلغاء"
                "es" -> "Cancelar"
                "fr" -> "Annuler"
                "hi" -> "रद्द करें"
                else -> "Cancel"
            }
            "save_now" -> when (selectedLang) {
                "ar" -> "حفظ الآن"
                "es" -> "Guardar"
                "fr" -> "Enregistrer"
                "hi" -> "अभी सहेजें"
                else -> "Save Now"
            }
            "timer_final_name" -> when (selectedLang) {
                "ar" -> "مؤقت ${hours}س:${minutes}د:${seconds}ث"
                "es" -> "Temporizador ${hours}h:${minutes}m:${seconds}s"
                "fr" -> "Minuteur ${hours}h:${minutes}m:${seconds}s"
                "hi" -> "टाइमर ${hours}घं:${minutes}मि:${seconds}से"
                else -> "Timer ${hours}h:${minutes}m:${seconds}s"
            }
            "reset_lbl" -> when (selectedLang) {
                "ar" -> "إعادة ضبط (٢٥د)"
                "es" -> "Reiniciar (25m)"
                "fr" -> "Réinitialiser (25m)"
                "hi" -> "रीसेट (25मि)"
                else -> "Reset (25m)"
            }
            else -> key
        }
    }

    fun getPresetDisplayName(rawName: String): String {
        return when (rawName) {
            "بومودورو (25د)", "بومودورو ٢٥د", "Pomodoro (25m)" -> Translations.get("pomodoro", selectedLang)
            "تركيز عميق (1س)", "تركيز عميق أولى", "Deep Focus (1h)" -> Translations.get("deep_focus", selectedLang)
            "فحص تركيز (10ث)", "تجريبي", "Focus Test (10s)" -> Translations.get("focus_test", selectedLang)
            "انطلاقة (5د)", "انطلاقة هادئة", "Kickstart (5m)" -> Translations.get("kickstart", selectedLang)
            else -> rawName
        }
    }
    
    Box(
        modifier = Modifier
            .fillMaxWidth()
            .clip(RoundedCornerShape(32.dp))
            .background(
                if (isDarkTheme) {
                    Brush.verticalGradient(
                        listOf(
                            Color(0xFF1E293B).copy(alpha = 0.5f),
                            Color(0xFF0F172A).copy(alpha = 0.5f)
                        )
                    )
                } else {
                    Brush.verticalGradient(
                        listOf(
                            Color(0xFFFFFFFF).copy(alpha = 0.9f),
                            Color(0xFFF1F5F9).copy(alpha = 0.9f)
                        )
                    )
                }
            )
            .border(
                1.dp,
                if (isDarkTheme) Color.White.copy(alpha = 0.12f) else Color(0xFFE2E8F0),
                RoundedCornerShape(32.dp)
            )
            .padding(24.dp)
    ) {
        Column(
            horizontalAlignment = Alignment.CenterHorizontally,
            modifier = Modifier.fillMaxWidth()
        ) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Column {
                    Text(
                        text = tTitle,
                        style = MaterialTheme.typography.titleMedium.copy(fontWeight = FontWeight.ExtraBold),
                        color = if (isDarkTheme) Color.White else Color(0xFF0F172A)
                    )
                    Text(
                        text = tSubtitle,
                        style = MaterialTheme.typography.labelSmall,
                        color = if (isDarkTheme) Color(0xFF818CF8) else Color(0xFF6366F1)
                    )
                }
                
                Icon(
                    imageVector = Icons.Filled.Timer,
                    contentDescription = null,
                    tint = Color(0xFF6366F1),
                    modifier = Modifier.size(28.dp)
                )
            }
            
            Spacer(modifier = Modifier.height(24.dp))
            
            Box(
                modifier = Modifier
                    .fillMaxWidth()
                    .height(168.dp),
                contentAlignment = Alignment.TopCenter
            ) {
                // Focus Active Selector Banner - Clean minimalist floating selector
                Box(
                    modifier = Modifier
                        .fillMaxWidth(0.85f)
                        .height(52.dp)
                        .offset(y = 40.dp)
                        .clip(RoundedCornerShape(12.dp))
                        .background(
                            if (isDarkTheme) Color(0xFF6366F1).copy(alpha = 0.12f) else Color(0xFF6366F1).copy(alpha = 0.05f)
                        )
                )

                Row(
                    modifier = Modifier.fillMaxSize(),
                    horizontalArrangement = Arrangement.Center,
                    verticalAlignment = Alignment.Top
                ) {
                    TimeDrumColumn(
                        value = hours,
                        labelArabic = "ساعات",
                        labelEnglish = "HOURS",
                        range = 0..23,
                        isDarkTheme = isDarkTheme,
                        onValueChange = { newH ->
                            onDurationChange(newH * 3600 + minutes * 60 + seconds)
                        }
                    )
                    
                    Box(
                        modifier = Modifier
                            .height(132.dp),
                        contentAlignment = Alignment.Center
                    ) {
                        Text(
                            text = ":",
                            style = MaterialTheme.typography.titleLarge.copy(
                                fontWeight = FontWeight.Black,
                                fontSize = 24.sp
                            ),
                            color = Color(0xFF6366F1),
                            modifier = Modifier.padding(horizontal = 8.dp)
                        )
                    }

                    TimeDrumColumn(
                        value = minutes,
                        labelArabic = "دقائق",
                        labelEnglish = "MINUTES",
                        range = 0..59,
                        isDarkTheme = isDarkTheme,
                        onValueChange = { newM ->
                            onDurationChange(hours * 3600 + newM * 60 + seconds)
                        }
                    )
                    
                    Box(
                        modifier = Modifier
                            .height(132.dp),
                        contentAlignment = Alignment.Center
                    ) {
                        Text(
                            text = ":",
                            style = MaterialTheme.typography.titleLarge.copy(
                                fontWeight = FontWeight.Black,
                                fontSize = 24.sp
                            ),
                            color = Color(0xFF6366F1),
                            modifier = Modifier.padding(horizontal = 8.dp)
                        )
                    }

                    TimeDrumColumn(
                        value = seconds,
                        labelArabic = "ثواني",
                        labelEnglish = "SECONDS",
                        range = 0..59,
                        isDarkTheme = isDarkTheme,
                        onValueChange = { newS ->
                            onDurationChange(hours * 3600 + minutes * 60 + newS)
                        }
                    )
                }
            }
            
            Spacer(modifier = Modifier.height(20.dp))
            
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(12.dp),
                verticalAlignment = Alignment.CenterVertically
            ) {
                Button(
                    onClick = { showAddPresetDialog = true },
                    modifier = Modifier
                        .weight(1f)
                        .heightIn(min = 46.dp),
                    colors = ButtonDefaults.buttonColors(
                        containerColor = if (isDarkTheme) Color.White.copy(alpha = 0.08f) else Color(0xFFF1F5F9),
                        contentColor = if (isDarkTheme) Color.White else Color(0xFF475569)
                    ),
                    shape = RoundedCornerShape(14.dp),
                    border = BorderStroke(1.dp, if (isDarkTheme) Color.White.copy(alpha = 0.1f) else Color(0xFFE2E8F0)),
                    contentPadding = PaddingValues(horizontal = 8.dp, vertical = 6.dp)
                ) {
                    Icon(imageVector = Icons.Filled.Save, contentDescription = null, modifier = Modifier.size(16.dp))
                    Spacer(modifier = Modifier.width(6.dp))
                    Text(text = tSavePreset, style = MaterialTheme.typography.labelMedium)
                }
                
                Button(
                    onClick = {
                        onDurationChange(25 * 60)
                    },
                    modifier = Modifier
                        .weight(1f)
                        .heightIn(min = 46.dp),
                    colors = ButtonDefaults.buttonColors(
                        containerColor = Color(0xFF6366F1).copy(alpha = 0.15f),
                        contentColor = Color(0xFF6366F1)
                    ),
                    shape = RoundedCornerShape(14.dp),
                    contentPadding = PaddingValues(horizontal = 8.dp, vertical = 6.dp)
                ) {
                    Icon(imageVector = Icons.Filled.Refresh, contentDescription = null, modifier = Modifier.size(16.dp))
                    Spacer(modifier = Modifier.width(6.dp))
                    Text(text = trans("reset_lbl"), style = MaterialTheme.typography.labelMedium)
                }
            }
            
            Spacer(modifier = Modifier.height(16.dp))
            
            Text(
                text = tPresetsTitle,
                style = MaterialTheme.typography.labelSmall,
                color = if (isDarkTheme) Color(0xFF94A3B8) else Color(0xFF64748B),
                modifier = Modifier.align(Alignment.Start).padding(bottom = 8.dp)
            )
            
            FlowRow(
                horizontalArrangement = Arrangement.spacedBy(6.dp),
                verticalArrangement = Arrangement.spacedBy(6.dp),
                modifier = Modifier.fillMaxWidth()
            ) {
                presets.forEachIndexed { idx, pair ->
                    Box(
                        modifier = Modifier
                            .clip(RoundedCornerShape(20.dp))
                            .background(
                                if (durationSecs == pair.second) {
                                    Color(0xFF6366F1).copy(alpha = 0.2f)
                                } else {
                                    if (isDarkTheme) Color.White.copy(alpha = 0.04f) else Color.Black.copy(alpha = 0.03f)
                                }
                            )
                            .border(
                                1.dp,
                                if (durationSecs == pair.second) Color(0xFF818CF8) else (if (isDarkTheme) Color.White.copy(alpha = 0.08f) else Color(0xFFE2E8F0)),
                                RoundedCornerShape(20.dp)
                            )
                            .clickable {
                                onDurationChange(pair.second)
                            }
                            .padding(horizontal = 10.dp, vertical = 6.dp)
                    ) {
                        Row(verticalAlignment = Alignment.CenterVertically) {
                            Text(
                                text = getPresetDisplayName(pair.first),
                                style = MaterialTheme.typography.labelSmall.copy(fontWeight = FontWeight.Bold),
                                color = if (durationSecs == pair.second) Color(0xFF818CF8) else (if (isDarkTheme) Color.White else Color(0xFF1E293B))
                            )
                            if (idx >= 4) {
                                Spacer(modifier = Modifier.width(4.dp))
                                Icon(
                                    imageVector = Icons.Filled.Close,
                                    contentDescription = "Delete Preset",
                                    tint = Color.Red.copy(alpha = 0.7f),
                                    modifier = Modifier
                                        .size(12.dp)
                                        .clickable {
                                            LockSettings.deletePreset(context, idx)
                                            presets = LockSettings.getSavedPresets(context)
                                        }
                                )
                            }
                        }
                    }
                }
            }
        }
    }
    
    if (showAddPresetDialog) {
        Dialog(onDismissRequest = { showAddPresetDialog = false }) {
            Box(
                modifier = Modifier
                    .clip(RoundedCornerShape(24.dp))
                    .background(if (isDarkTheme) Color(0xFF1E293B) else Color.White)
                    .border(1.dp, Color(0xFF6366F1).copy(alpha = 0.3f), RoundedCornerShape(24.dp))
                    .padding(24.dp)
            ) {
                Column(horizontalAlignment = Alignment.CenterHorizontally) {
                    Text(
                        text = trans("save_cust_preset"),
                        style = MaterialTheme.typography.titleMedium.copy(fontWeight = FontWeight.Bold),
                        color = if (isDarkTheme) Color.White else Color(0xFF0F172A),
                        textAlign = TextAlign.Center
                    )
                    Spacer(modifier = Modifier.height(12.dp))
                    Text(
                        text = trans("save_cust_preset_desc"),
                        style = MaterialTheme.typography.bodySmall,
                        color = if (isDarkTheme) Color(0xFF94A3B8) else Color(0xFF475569),
                        textAlign = TextAlign.Center
                    )
                    Spacer(modifier = Modifier.height(16.dp))
                    OutlinedTextField(
                        value = newPresetName,
                        onValueChange = { newPresetName = it },
                        label = { Text(trans("preset_name_label")) },
                        singleLine = true,
                        colors = OutlinedTextFieldDefaults.colors(
                            focusedBorderColor = Color(0xFF6366F1),
                            unfocusedBorderColor = if (isDarkTheme) Color.White.copy(alpha = 0.15f) else Color(0xFFCBD5E1)
                        ),
                        modifier = Modifier.fillMaxWidth()
                    )
                    Spacer(modifier = Modifier.height(24.dp))
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.spacedBy(12.dp)
                    ) {
                        Button(
                            onClick = { showAddPresetDialog = false },
                            colors = ButtonDefaults.buttonColors(containerColor = Color.Transparent, contentColor = if (isDarkTheme) Color.White else Color(0xFF475569)),
                            modifier = Modifier.weight(1f)
                        ) {
                            Text(trans("cancel"))
                        }
                        
                        Button(
                            onClick = {
                                val finalizedName = newPresetName.trim().ifEmpty { trans("timer_final_name") }
                                LockSettings.savePreset(context, finalizedName, durationSecs)
                                presets = LockSettings.getSavedPresets(context)
                                newPresetName = ""
                                showAddPresetDialog = false
                            },
                            colors = ButtonDefaults.buttonColors(containerColor = Color(0xFF6366F1)),
                            modifier = Modifier.weight(1.5f)
                        ) {
                            Text(trans("save_now"), color = Color.White)
                        }
                    }
                }
            }
        }
    }
}

@Composable
fun TimeDrumColumn(
    value: Int,
    labelArabic: String,
    labelEnglish: String,
    range: IntRange,
    isDarkTheme: Boolean,
    onValueChange: (Int) -> Unit
) {
    val context = LocalContext.current
    val selectedLang = remember { LockSettings.getSelectedLanguage(context) }
    val displayLabel = if (selectedLang == "ar") labelArabic else labelEnglish

    val rangeCount = range.last - range.first + 1
    // Infinite multiplier base to allow circular wrapping
    val baseMultiplier = 1000
    val totalItems = baseMultiplier * rangeCount

    val listState = rememberLazyListState()
    val density = LocalDensity.current
    val itemHeightDp = 44.dp
    val itemHeightPx = itemHeightDp.value * density.density

    // Initialize list to center on the current value
    LaunchedEffect(Unit) {
        val initialCenterIndex = (baseMultiplier / 2) * rangeCount + (value - range.first)
        listState.scrollToItem(initialCenterIndex - 1, 0)
    }

    // Sync programmatically from external changes (Presets / Resets) when NOT scrolling
    LaunchedEffect(value) {
        if (!listState.isScrollInProgress) {
            val currentCenter = listState.firstVisibleItemIndex + 1
            val currentCenterValInDomain = (currentCenter % rangeCount) + range.first
            if (value != currentCenterValInDomain) {
                val diff = value - currentCenterValInDomain
                var shortDiff = diff % rangeCount
                if (shortDiff > rangeCount / 2) shortDiff -= rangeCount
                if (shortDiff < -rangeCount / 2) shortDiff += rangeCount

                val targetCenter = currentCenter + shortDiff
                listState.animateScrollToItem(targetCenter - 1, 0)
            }
        }
    }

    // Capture the snap target when user finishes dragging/flinging
    LaunchedEffect(listState.isScrollInProgress) {
        if (!listState.isScrollInProgress) {
            val firstVisibleIndex = listState.firstVisibleItemIndex
            val scrollOffset = listState.firstVisibleItemScrollOffset
            
            val snapIndex = if (scrollOffset.toFloat() > itemHeightPx / 2f) {
                firstVisibleIndex + 1
            } else {
                firstVisibleIndex
            }
            listState.animateScrollToItem(snapIndex, 0)
            
            val selectedIndex = (snapIndex + 1) % rangeCount
            val newValue = range.first + selectedIndex
            onValueChange(newValue)
        }
    }

    Column(
        horizontalAlignment = Alignment.CenterHorizontally,
        modifier = Modifier.width(72.dp)
    ) {
        Box(
            modifier = Modifier
                .size(width = 72.dp, height = 132.dp),
            contentAlignment = Alignment.Center
        ) {
            LazyColumn(
                state = listState,
                modifier = Modifier
                    .size(width = 68.dp, height = 132.dp),
                contentPadding = PaddingValues(0.dp)
            ) {
                items(totalItems) { index ->
                    val indexInDomain = index % rangeCount
                    val displayValue = range.first + indexInDomain

                    val firstVisibleIndex = listState.firstVisibleItemIndex
                    val firstVisibleItemScrollOffset = listState.firstVisibleItemScrollOffset
                    
                    val relativePos = index - firstVisibleIndex - (firstVisibleItemScrollOffset.toFloat() / itemHeightPx)
                    val distance = if (relativePos > 1.0f) relativePos - 1.0f else 1.0f - relativePos

                    // 3D Cylinder roll projection metrics
                    val scale = (1.25f - (distance * 0.35f)).coerceIn(0.7f, 1.25f)
                    val alpha = (1.0f - (distance * 0.65f)).coerceIn(0.15f, 1.0f)
                    val rotationX = (relativePos - 1.0f) * -35f

                    Box(
                        modifier = Modifier
                            .fillMaxWidth()
                            .height(itemHeightDp)
                            .graphicsLayer {
                                scaleX = scale
                                scaleY = scale
                                this.alpha = alpha
                                this.rotationX = rotationX
                                cameraDistance = 8f * density.density
                            },
                        contentAlignment = Alignment.Center
                    ) {
                        Text(
                            text = String.format("%02d", displayValue),
                            style = MaterialTheme.typography.titleLarge.copy(
                                fontSize = 24.sp,
                                fontWeight = FontWeight.Bold,
                                fontFamily = androidx.compose.ui.text.font.FontFamily.Default
                            ),
                            color = if (distance < 0.35f) {
                                Color(0xFF6366F1)
                            } else {
                                if (isDarkTheme) Color.White.copy(alpha = 0.3f) else Color(0xFF0F172A).copy(alpha = 0.2f)
                            }
                        )
                    }
                }
            }
        }
        
        Spacer(modifier = Modifier.height(8.dp))
        
        Text(
            text = displayLabel,
            style = MaterialTheme.typography.labelSmall.copy(fontWeight = FontWeight.Bold),
            color = if (isDarkTheme) Color(0xFF94A3B8) else Color(0xFF475569)
        )
    }
}

@Composable
fun ChallengeUnlockDialog(
    isDarkTheme: Boolean,
    onSuccess: () -> Unit,
    onDismiss: () -> Unit
) {
    var stage by remember { mutableIntStateOf(1) }

    val mathQuiz = remember {
        val first = (5..12).random()
        val second = (3..8).random()
        val third = (4..15).random()
        val isAddition = listOf(true, false).random()
        val answer = if (isAddition) (first * second) + third else (first * second) - third
        Triple("$first × $second ${if (isAddition) "+" else "-"} $third", answer, "احسب الناتج لتفعيل عقلك العقلاني")
    }
    var mathInput by remember { mutableStateOf("") }
    var mathError by remember { mutableStateOf(false) }

    val targetQuote = "أنا سيد فكري وقراري ولن أدع اللحظات الضعيفة تشتت مستقبلي المشرق"
    var quoteInput by remember { mutableStateOf("") }
    var quoteError by remember { mutableStateOf(false) }

    var holdProgress by remember { mutableFloatStateOf(0f) }
    var isHolding by remember { mutableStateOf(false) }
    
    val currentHoldMilli = 8000L
    LaunchedEffect(isHolding) {
        if (isHolding) {
            val startTime = System.currentTimeMillis()
            while (holdProgress < 1f && isHolding) {
                delay(30)
                val elapsed = System.currentTimeMillis() - startTime
                holdProgress = minOf(1f, elapsed.toFloat() / currentHoldMilli)
            }
            if (holdProgress >= 1f) {
                onSuccess()
            }
        } else {
            while (holdProgress > 0f && !isHolding) {
                delay(16)
                holdProgress -= 0.04f
                if (holdProgress < 0f) holdProgress = 0f
            }
        }
    }

    Dialog(
        onDismissRequest = onDismiss,
        properties = DialogProperties(dismissOnBackPress = false, dismissOnClickOutside = false)
    ) {
        Box(
            modifier = Modifier
                .fillMaxWidth()
                .clip(RoundedCornerShape(32.dp))
                .background(if (isDarkTheme) Color(0xFF0F0F12) else Color.White)
                .border(2.dp, Brush.sweepGradient(listOf(Color(0xFFEF4444), Color(0xFF6366F1), Color(0xFFEF4444))), RoundedCornerShape(32.dp))
                .padding(24.dp)
        ) {
            Column(
                horizontalAlignment = Alignment.CenterHorizontally,
                modifier = Modifier.fillMaxWidth()
            ) {
                Icon(
                    imageVector = Icons.Filled.Warning,
                    contentDescription = null,
                    tint = Color(0xFFEF4444),
                    modifier = Modifier.size(40.dp)
                )
                Spacer(modifier = Modifier.height(8.dp))
                Text(
                    text = "حاجز الوعي ضد التشتت",
                    style = MaterialTheme.typography.titleLarge.copy(fontWeight = FontWeight.ExtraBold),
                    color = if (isDarkTheme) Color.White else Color(0xFF0F172A),
                    textAlign = TextAlign.Center
                )
                Text(
                    text = "لأنك قررت عزل التطبيقات بوعيك التام، فلن يدعك ZenLock تنهي الجلسة إلا بعد برهنة تحكمك وانتباهك العقلي التام.",
                    style = MaterialTheme.typography.labelSmall,
                    color = if (isDarkTheme) Color(0xFF94A3B8) else Color(0xFF64748B),
                    textAlign = TextAlign.Center,
                    modifier = Modifier.padding(horizontal = 4.dp, vertical = 4.dp)
                )

                Spacer(modifier = Modifier.height(20.dp))

                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceEvenly,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    ChallengeStepCircle(step = 1, currentStep = stage, text = "العقل المنطقي")
                    Box(modifier = Modifier.width(24.dp).height(2.dp).background(if (stage > 1) Color(0xFF10B981) else Color.Gray.copy(alpha = 0.3f)))
                    ChallengeStepCircle(step = 2, currentStep = stage, text = "تأكيد الإرادة")
                    Box(modifier = Modifier.width(24.dp).height(2.dp).background(if (stage > 2) Color(0xFF10B981) else Color.Gray.copy(alpha = 0.3f)))
                    ChallengeStepCircle(step = 3, currentStep = stage, text = "التنفس الواعي")
                }

                Spacer(modifier = Modifier.height(24.dp))

                when (stage) {
                    1 -> {
                        Text(
                            text = "للتأكد من نشاط الفص الجبهي المسؤول عن الانتباه، احسب الإجابة التالية:",
                            style = MaterialTheme.typography.bodySmall,
                            color = if (isDarkTheme) Color(0xFF94A3B8) else Color(0xFF475569),
                            textAlign = TextAlign.Center
                        )
                        Spacer(modifier = Modifier.height(16.dp))
                        
                        Box(
                            modifier = Modifier
                                .fillMaxWidth()
                                .height(64.dp)
                                .clip(RoundedCornerShape(16.dp))
                                .background(if (isDarkTheme) Color.White.copy(alpha = 0.05f) else Color(0xFFF1F5F9))
                                .border(1.dp, Color(0xFF6366F1).copy(alpha = 0.4f), RoundedCornerShape(16.dp)),
                            contentAlignment = Alignment.Center
                        ) {
                            Text(
                                text = mathQuiz.first,
                                style = MaterialTheme.typography.headlineMedium.copy(fontWeight = FontWeight.Black),
                                color = if (isDarkTheme) Color.White else Color(0xFF0F172A)
                            )
                        }

                        Spacer(modifier = Modifier.height(16.dp))

                        OutlinedTextField(
                            value = mathInput,
                            onValueChange = {
                                mathInput = it
                                mathError = false
                            },
                            label = { Text("أدخل إجابتك الرياضية") },
                            isError = mathError,
                            singleLine = true,
                            keyboardOptions = androidx.compose.foundation.text.KeyboardOptions(
                                keyboardType = androidx.compose.ui.text.input.KeyboardType.Number
                            ),
                            colors = OutlinedTextFieldDefaults.colors(
                                focusedBorderColor = Color(0xFF6366F1),
                                errorBorderColor = Color(0xFFEF4444)
                            ),
                            modifier = Modifier.fillMaxWidth()
                        )
                        
                        if (mathError) {
                            Text(
                                text = "الناتج غير صحيح. خذ وقتك للتفكير واكتب بدقة.",
                                color = Color(0xFFEF4444),
                                style = MaterialTheme.typography.labelSmall,
                                modifier = Modifier.padding(top = 4.dp)
                            )
                        }

                        Spacer(modifier = Modifier.height(24.dp))

                        Button(
                            onClick = {
                                val entered = mathInput.trim().toIntOrNull()
                                if (entered == mathQuiz.second) {
                                    stage = 2
                                } else {
                                    mathError = true
                                }
                            },
                            colors = ButtonDefaults.buttonColors(containerColor = Color(0xFF6366F1)),
                            modifier = Modifier.fillMaxWidth().height(48.dp),
                            shape = RoundedCornerShape(16.dp)
                        ) {
                            Text("تأكيد وحل اللغز", color = Color.White)
                        }
                    }
                    2 -> {
                        Text(
                            text = "اكتب العبارة بدقة متناهية ودون أخطاء لكبح شهوة الفتح التلقائية:",
                            style = MaterialTheme.typography.bodySmall,
                            color = if (isDarkTheme) Color(0xFF94A3B8) else Color(0xFF475569),
                            textAlign = TextAlign.Center
                        )
                        Spacer(modifier = Modifier.height(12.dp))
                        
                        Box(
                            modifier = Modifier
                                .fillMaxWidth()
                                .clip(RoundedCornerShape(16.dp))
                                .background(Color(0xFF6366F1).copy(alpha = 0.08f))
                                .border(1.dp, Color(0xFF6366F1).copy(alpha = 0.2f), RoundedCornerShape(16.dp))
                                .padding(16.dp)
                        ) {
                            Text(
                                text = targetQuote,
                                style = MaterialTheme.typography.bodyMedium.copy(lineHeight = 22.sp, fontWeight = FontWeight.Bold),
                                color = if (isDarkTheme) Color(0xFF818CF8) else Color(0xFF4F46E5),
                                textAlign = TextAlign.Center,
                                modifier = Modifier.fillMaxWidth()
                            )
                        }

                        Spacer(modifier = Modifier.height(16.dp))

                        OutlinedTextField(
                            value = quoteInput,
                            onValueChange = {
                                quoteInput = it
                                quoteError = false
                            },
                            label = { Text("اكتب العبارة حرفيًا هنا") },
                            isError = quoteError,
                            colors = OutlinedTextFieldDefaults.colors(
                                focusedBorderColor = Color(0xFF6366F1),
                                errorBorderColor = Color(0xFFEF4444)
                            ),
                            modifier = Modifier.fillMaxWidth()
                        )
                        
                        if (quoteError) {
                            Text(
                                text = "العبارة غير متطابقة. يرجى كتابتها بدقة متناهية.",
                                color = Color(0xFFEF4444),
                                style = MaterialTheme.typography.labelSmall,
                                modifier = Modifier.padding(top = 4.dp)
                            )
                        }

                        Spacer(modifier = Modifier.height(24.dp))

                        Button(
                            onClick = {
                                if (quoteInput.trim() == targetQuote) {
                                    stage = 3
                                } else {
                                    quoteError = true
                                }
                            },
                            colors = ButtonDefaults.buttonColors(containerColor = Color(0xFF6366F1)),
                            modifier = Modifier.fillMaxWidth().height(48.dp),
                            shape = RoundedCornerShape(16.dp)
                        ) {
                            Text("تأكيد القسم والتالي", color = Color.White)
                        }
                    }
                    3 -> {
                        val instructionsText = when {
                            holdProgress <= 0.25f -> "خذ شهيقاً عميقاً وصافياً..."
                            holdProgress <= 0.5f  -> "احبس أنفاسك واشعر بقوتك الذاتية..."
                            holdProgress <= 0.75f -> "أخرج زفير الضعف والتشتت من صدرك..."
                            else                  -> "أنت الآن واعي وتتحكم بعقلك بالكامل..."
                        }

                        Text(
                            text = instructionsText,
                            style = MaterialTheme.typography.bodyMedium.copy(fontWeight = FontWeight.Bold),
                            color = Color(0xFF10B981),
                            textAlign = TextAlign.Center,
                            modifier = Modifier.height(24.dp)
                        )
                        Spacer(modifier = Modifier.height(16.dp))

                        Box(
                            modifier = Modifier
                                .size(140.dp)
                                .pointerInput(Unit) {
                                    detectTapGestures(
                                        onPress = {
                                            isHolding = true
                                            tryAwaitRelease()
                                            isHolding = false
                                        }
                                    )
                                },
                            contentAlignment = Alignment.Center
                        ) {
                            Canvas(modifier = Modifier.fillMaxSize()) {
                                val strokeWidth = 8.dp.toPx()
                                drawArc(
                                    color = if (isDarkTheme) Color.White.copy(alpha = 0.05f) else Color.Black.copy(alpha = 0.06f),
                                    startAngle = 0f,
                                    sweepAngle = 360f,
                                    useCenter = false,
                                    style = Stroke(width = strokeWidth)
                                )
                                if (holdProgress > 0f) {
                                    drawArc(
                                        color = Color(0xFF10B981),
                                        startAngle = -90f,
                                        sweepAngle = 360f * holdProgress,
                                        useCenter = false,
                                        style = Stroke(width = strokeWidth, cap = StrokeCap.Round)
                                    )
                                }
                            }

                            Box(
                                modifier = Modifier
                                    .size(100.dp)
                                    .clip(CircleShape)
                                    .background(
                                        Brush.radialGradient(
                                            if (isHolding) {
                                                listOf(Color(0xFF10B981).copy(alpha = 0.3f), Color(0xFF10B981).copy(alpha = 0.05f))
                                            } else {
                                                listOf(Color(0xFFEF4444).copy(alpha = 0.2f), Color.Transparent)
                                            }
                                        )
                                    )
                                    .border(
                                        width = 1.dp,
                                        color = if (isHolding) Color(0xFF10B981) else Color(0xFFEF4444).copy(alpha = 0.5f),
                                        shape = CircleShape
                                    ),
                                contentAlignment = Alignment.Center
                            ) {
                                Column(horizontalAlignment = Alignment.CenterHorizontally) {
                                    Icon(
                                        imageVector = Icons.Filled.Fingerprint,
                                        contentDescription = "Hold fingerprint to breathe",
                                        tint = if (isHolding) Color(0xFF10B981) else Color(0xFFEF4444),
                                        modifier = Modifier.size(44.dp)
                                    )
                                    Text(
                                        text = "${(8 - (holdProgress * 8).toInt())}ث",
                                        style = MaterialTheme.typography.labelSmall.copy(fontWeight = FontWeight.Bold),
                                        color = if (isHolding) Color(0xFF10B981) else Color(0xFFEF4444)
                                    )
                                }
                            }
                        }

                        Spacer(modifier = Modifier.height(16.dp))

                        Text(
                            text = "ضع إصبعك واستمر بالضغط لـ ٨ ثواني متواصلة.\n(إذا تركت سيعيد المؤشر من البداية لتسهيل الهدوء والتنفس المستقر)",
                            style = MaterialTheme.typography.labelSmall,
                            color = if (isDarkTheme) Color(0xFF94A3B8) else Color(0xFF475569),
                            textAlign = TextAlign.Center
                        )
                    }
                }

                Spacer(modifier = Modifier.height(32.dp))

                OutlinedButton(
                    onClick = onDismiss,
                    colors = ButtonDefaults.outlinedButtonColors(
                        contentColor = if (isDarkTheme) Color.White.copy(alpha = 0.6f) else Color(0xFF475569)
                    ),
                    shape = RoundedCornerShape(16.dp),
                    border = BorderStroke(1.dp, if (isDarkTheme) Color.White.copy(alpha = 0.12f) else Color(0xFFE2E8F0)),
                    modifier = Modifier.fillMaxWidth().height(48.dp)
                ) {
                    Text("إلغاء والتراجع عن كسر التركيز", style = MaterialTheme.typography.labelLarge)
                }
            }
        }
    }
}

@Composable
fun ChallengeStepCircle(step: Int, currentStep: Int, text: String) {
    val isPassed = currentStep > step
    val isActive = currentStep == step
    
    val color = when {
        isPassed -> Color(0xFF10B981)
        isActive -> Color(0xFF6366F1)
        else     -> Color.Gray.copy(alpha = 0.3f)
    }

    Column(horizontalAlignment = Alignment.CenterHorizontally) {
        Box(
            modifier = Modifier
                .size(32.dp)
                .clip(CircleShape)
                .background(color),
            contentAlignment = Alignment.Center
        ) {
            if (isPassed) {
                Icon(
                    imageVector = Icons.Filled.Check,
                    contentDescription = null,
                    tint = Color.White,
                    modifier = Modifier.size(16.dp)
                )
            } else {
                Text(
                    text = "$step",
                    style = MaterialTheme.typography.labelLarge.copy(color = Color.White, fontWeight = FontWeight.Bold)
                )
            }
        }
        Spacer(modifier = Modifier.height(4.dp))
        Text(
            text = text,
            style = MaterialTheme.typography.labelSmall.copy(fontSize = 9.sp, fontWeight = FontWeight.SemiBold),
            color = color
        )
    }
}

@Composable
fun StartButton(isDarkTheme: Boolean, onClick: () -> Unit, enabled: Boolean, selectedLang: String) {
    var isPressed by remember { mutableStateOf(false) }
    val scale by animateFloatAsState(
        targetValue = if (isPressed) 0.95f else 1f,
        animationSpec = spring(dampingRatio = Spring.DampingRatioMediumBouncy),
        label = "StartBtnScale"
    )
    
    val gradientColors = if (enabled) {
        listOf(com.example.ui.theme.FrostedPurple, com.example.ui.theme.FrostedPrimary)
    } else {
        listOf(if (isDarkTheme) Color.White.copy(alpha = 0.08f) else Color(0xFFE2E8F0), if (isDarkTheme) Color.White.copy(alpha = 0.08f) else Color(0xFFE2E8F0))
    }

    Box(
        modifier = Modifier
            .fillMaxWidth()
            .height(64.dp)
            .scale(scale)
            .clip(RoundedCornerShape(24.dp))
            .background(Brush.horizontalGradient(gradientColors))
            .border(
                width = 1.dp,
                brush = Brush.linearGradient(
                    colors = if (enabled) {
                        listOf(if (isDarkTheme) Color.White.copy(alpha = 0.3f) else Color.White.copy(alpha = 0.6f), Color.Transparent)
                    } else {
                        listOf(if (isDarkTheme) Color.White.copy(alpha = 0.05f) else Color.Transparent, Color.Transparent)
                    }
                ),
                shape = RoundedCornerShape(24.dp)
            )
            .clickable(enabled = enabled) {
                onClick()
            }
            .testTag("start_lockdown_button"),
        contentAlignment = Alignment.Center
    ) {
        Row(verticalAlignment = Alignment.CenterVertically) {
            Icon(
                imageVector = Icons.Filled.Lock,
                contentDescription = null,
                tint = if (enabled) Color.White else (if (isDarkTheme) Color.White.copy(alpha = 0.3f) else Color(0xFF94A3B8)),
                modifier = Modifier.size(20.dp)
            )
            Spacer(modifier = Modifier.width(10.dp))
            Text(
                text = Translations.get("activate_lock", selectedLang),
                style = MaterialTheme.typography.titleMedium,
                fontWeight = FontWeight.Black,
                color = if (enabled) Color.White else (if (isDarkTheme) Color.White.copy(alpha = 0.3f) else Color(0xFF94A3B8)),
                letterSpacing = 1.5.sp
            )
        }
    }
}

@Composable
fun LockdownScreen(
    isDarkTheme: Boolean,
    durationSecs: Int,
    selectedApps: Set<String>,
    installedApps: List<DeviceAppInfo>,
    onUnlock: () -> Unit
) {
    val context = LocalContext.current
    var timeLeft by remember { mutableIntStateOf(durationSecs) }
    
    // Intercept back click so they cannot exit ZenLock during lockdown
    BackHandler(enabled = true) {
        // Do nothing or show a custom message, preventing back out
    }
    
    LaunchedEffect(Unit) {
        while (timeLeft > 0) {
            delay(1000)
            timeLeft--
        }
        onUnlock()
    }

    val mins = timeLeft / 60
    val secs = timeLeft % 60
    val timeString = String.format("%02d:%02d", mins, secs)
    
    val infiniteTransition = rememberInfiniteTransition(label = "pulse")
    val pulseScale by infiniteTransition.animateFloat(
        initialValue = 1f,
        targetValue = 1.05f,
        animationSpec = infiniteRepeatable(
            animation = tween(2000, easing = FastOutSlowInEasing),
            repeatMode = RepeatMode.Reverse
        ),
        label = "pulseScale"
    )

    val spinTransition = rememberInfiniteTransition(label = "SpinnerTransition")
    val spinAngle by spinTransition.animateFloat(
        initialValue = 0f,
        targetValue = 360f,
        animationSpec = infiniteRepeatable(
            animation = tween(12000, easing = LinearEasing),
            repeatMode = RepeatMode.Restart
        ),
        label = "spinAngle"
    )

    Box(
        modifier = Modifier
            .fillMaxSize()
            .background(Color.Transparent),
        contentAlignment = Alignment.Center
    ) {
        Column(
            horizontalAlignment = Alignment.CenterHorizontally,
            modifier = Modifier.padding(24.dp)
        ) {
            // Security state badge at the top
            Row(
                modifier = Modifier
                    .clip(RoundedCornerShape(20.dp))
                    .background(com.example.ui.theme.FrostedPrimary.copy(alpha = 0.15f))
                    .border(1.dp, com.example.ui.theme.FrostedPrimary.copy(alpha = 0.3f), RoundedCornerShape(20.dp))
                    .padding(horizontal = 14.dp, vertical = 6.dp),
                verticalAlignment = Alignment.CenterVertically
            ) {
                Box(
                    modifier = Modifier
                        .size(6.dp)
                        .clip(CircleShape)
                        .background(com.example.ui.theme.FrostedPrimary)
                )
                Spacer(modifier = Modifier.width(8.dp))
                Text(
                    text = "DEEP SECURITY ACTIVE",
                    style = MaterialTheme.typography.labelSmall.copy(fontSize = 10.sp, letterSpacing = 1.sp),
                    fontWeight = FontWeight.Bold,
                    color = if (isDarkTheme) Color.White else com.example.ui.theme.FrostedPrimary
                )
            }
            
            Spacer(modifier = Modifier.height(48.dp))
            
            // Spinning premium Ring effect around timer
            Box(
                modifier = Modifier.size(260.dp),
                contentAlignment = Alignment.Center
            ) {
                // Background 3D ring track
                Canvas(modifier = Modifier.fillMaxSize()) {
                    drawCircle(
                        color = if (isDarkTheme) Color.White.copy(alpha = 0.04f) else Color.Black.copy(alpha = 0.06f),
                        radius = size.width / 2f - 10.dp.toPx(),
                        style = Stroke(width = 8.dp.toPx())
                    )
                }

                // Rotating gradient arc
                Canvas(modifier = Modifier.fillMaxSize().rotate(spinAngle)) {
                    val strokePadding = 20.dp.toPx()
                    drawArc(
                        brush = Brush.sweepGradient(
                            colors = listOf(
                                com.example.ui.theme.FrostedPrimary,
                                com.example.ui.theme.FrostedPurple,
                                Color.Transparent
                            )
                        ),
                        startAngle = 0f,
                        sweepAngle = 280f,
                        useCenter = false,
                        style = Stroke(width = 8.dp.toPx(), cap = StrokeCap.Round),
                        size = androidx.compose.ui.geometry.Size(this.size.width - strokePadding, this.size.height - strokePadding),
                        topLeft = Offset(10.dp.toPx(), 10.dp.toPx())
                    )
                }

                // Inner Glass card
                Box(
                    modifier = Modifier
                        .size(190.dp)
                        .scale(pulseScale)
                        .clip(CircleShape)
                        .background(if (isDarkTheme) Color.White.copy(alpha = 0.05f) else Color.White)
                        .border(1.dp, if (isDarkTheme) Color.White.copy(alpha = 0.12f) else Color(0xFFE2E8F0), CircleShape),
                    contentAlignment = Alignment.Center
                ) {
                    Column(horizontalAlignment = Alignment.CenterHorizontally) {
                        Text(
                            text = "TIME REMAINING",
                            style = MaterialTheme.typography.labelSmall.copy(fontSize = 11.sp, letterSpacing = 2.sp),
                            fontWeight = FontWeight.Bold,
                            color = com.example.ui.theme.FrostedPrimary
                        )
                        Spacer(modifier = Modifier.height(8.dp))
                        Text(
                            text = timeString,
                            style = MaterialTheme.typography.displayMedium.copy(
                                fontSize = 42.sp,
                                fontWeight = FontWeight.Black
                            ),
                            color = if (isDarkTheme) Color.White else com.example.ui.theme.FrostedTextLight
                        )
                        Spacer(modifier = Modifier.height(6.dp))
                        Text(
                            text = "until deep-unlock",
                            style = MaterialTheme.typography.labelSmall,
                            color = if (isDarkTheme) Color.White.copy(alpha = 0.5f) else com.example.ui.theme.FrostedTextSecondaryLight
                        )
                    }
                }
            }

            Spacer(modifier = Modifier.height(56.dp))

            Text(
                text = "NO DISTRACTIONS",
                style = MaterialTheme.typography.titleMedium.copy(letterSpacing = 3.sp),
                fontWeight = FontWeight.Black,
                color = if (isDarkTheme) Color.White else com.example.ui.theme.FrostedTextLight
            )
            Spacer(modifier = Modifier.height(8.dp))
            Text(
                text = "Loopholes are disabled. Stay in flow.",
                style = MaterialTheme.typography.bodySmall,
                color = if (isDarkTheme) Color.White.copy(alpha = 0.5f) else com.example.ui.theme.FrostedTextSecondaryLight,
                textAlign = TextAlign.Center
            )

            Spacer(modifier = Modifier.height(24.dp))

            // Shielded applications icons display with dynamic loading
            val filteredShielded = installedApps.filter { selectedApps.contains(it.packageName) }
            if (filteredShielded.isNotEmpty()) {
                Column(horizontalAlignment = Alignment.CenterHorizontally) {
                    Text(
                        text = "SHIELDED APPLICATIONS",
                        style = MaterialTheme.typography.labelSmall.copy(letterSpacing = 1.sp, fontSize = 9.sp),
                        fontWeight = FontWeight.Bold,
                        color = if (isDarkTheme) Color.White.copy(alpha = 0.4f) else com.example.ui.theme.FrostedTextSecondaryLight
                    )
                    Spacer(modifier = Modifier.height(10.dp))
                    Row(
                        horizontalArrangement = Arrangement.spacedBy(10.dp, Alignment.CenterHorizontally),
                        verticalAlignment = Alignment.CenterVertically,
                        modifier = Modifier
                            .clip(RoundedCornerShape(16.dp))
                            .background(if (isDarkTheme) Color.White.copy(alpha = 0.04f) else Color(0xFFF1F5F9))
                            .border(1.dp, if (isDarkTheme) Color.White.copy(alpha = 0.06f) else Color(0xFFE2E8F0), RoundedCornerShape(16.dp))
                            .padding(10.dp)
                    ) {
                        filteredShielded.take(6).forEach { app ->
                            Box(contentAlignment = Alignment.BottomEnd) {
                                AppIcon(
                                    packageName = app.packageName,
                                    modifier = Modifier
                                        .size(32.dp)
                                        .clip(RoundedCornerShape(8.dp))
                                        .graphicsLayer { 
                                            alpha = 0.7f
                                        },
                                    fallbackLabel = app.label,
                                    fallbackColor = app.color,
                                    isStealthMode = false,
                                    textStyle = MaterialTheme.typography.labelSmall.copy(fontSize = 9.sp, fontWeight = FontWeight.Black),
                                    colorFilter = ColorFilter.colorMatrix(ColorMatrix().apply { setToSaturation(0.1f) })
                                )
                                Icon(
                                    imageVector = Icons.Filled.Lock,
                                    contentDescription = null,
                                    tint = Color(0xFF6366F1),
                                    modifier = Modifier
                                        .size(10.dp)
                                        .offset(x = 2.dp, y = 2.dp)
                                        .background(if (isDarkTheme) Color(0xFF0F172A) else Color.White, CircleShape)
                                        .padding(1.dp)
                                )
                            }
                        }
                        if (filteredShielded.size > 6) {
                            Box(
                                modifier = Modifier
                                    .size(28.dp)
                                    .clip(CircleShape)
                                    .background(if (isDarkTheme) Color.White.copy(alpha = 0.1f) else Color.Black.copy(alpha = 0.12f)),
                                contentAlignment = Alignment.Center
                            ) {
                                Text(
                                    text = "+${filteredShielded.size - 6}",
                                    style = MaterialTheme.typography.labelSmall.copy(fontSize = 11.sp),
                                    fontWeight = FontWeight.Bold,
                                    color = if (isDarkTheme) Color.White else com.example.ui.theme.FrostedTextLight
                                )
                            }
                        }
                    }
                }
            }

            Spacer(modifier = Modifier.height(36.dp))

            Box(
                modifier = Modifier
                    .fillMaxWidth()
                    .clip(RoundedCornerShape(24.dp))
                    .background(
                        if (isDarkTheme) Color(0xFFEF4444).copy(alpha = 0.08f)
                        else Color(0xFFFEF2F2)
                    )
                    .border(
                        width = 1.5.dp,
                        color = if (isDarkTheme) Color(0xFFEF4444).copy(alpha = 0.25f) else Color(0xFFFCA5A5),
                        shape = RoundedCornerShape(24.dp)
                    )
                    .padding(20.dp)
            ) {
                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(16.dp)
                ) {
                    Box(
                        modifier = Modifier
                            .size(48.dp)
                            .clip(CircleShape)
                            .background(Color(0xFFEF4444).copy(alpha = 0.15f)),
                        contentAlignment = Alignment.Center
                    ) {
                        Icon(
                            imageVector = Icons.Filled.Lock,
                            contentDescription = "Strict Mode Active",
                            tint = Color(0xFFEF4444),
                            modifier = Modifier.size(24.dp)
                        )
                    }

                    Column(modifier = Modifier.weight(1f)) {
                        Text(
                            text = "تم تفعيل القفل الصارم بنجاح",
                            style = MaterialTheme.typography.titleSmall.copy(fontWeight = FontWeight.Bold),
                            color = if (isDarkTheme) Color.White else Color(0xFF991B1B)
                        )
                        Spacer(modifier = Modifier.height(4.dp))
                        Text(
                            text = "لا توجد ثغرات أو مهرب. الجلسة مغلقة تماماً حتى ينتهي العداد لضمان انضباطك المطلق.",
                            style = MaterialTheme.typography.labelSmall,
                            color = if (isDarkTheme) Color(0xFFFCA5A5) else Color(0xFF7F1D1D)
                        )
                    }
                }
            }
        }
    }
}

@Composable
fun EmergencyUnlockButton(onUnlock: () -> Unit) {
    var holdProgress by remember { mutableFloatStateOf(0f) }
    var isHolding by remember { mutableStateOf(false) }
    
    LaunchedEffect(isHolding) {
        if (isHolding) {
            while (holdProgress < 1f) {
                delay(16) // ~60fps
                holdProgress += 0.01f // requires ~1.6s to fill
            }
            if (holdProgress >= 1f) {
                onUnlock()
            }
        } else {
            while (holdProgress > 0f) {
                delay(16)
                holdProgress -= 0.05f
                if (holdProgress < 0f) holdProgress = 0f
            }
        }
    }

    Box(
        modifier = Modifier
            .size(120.dp)
            .pointerInput(Unit) {
                detectTapGestures(
                    onPress = {
                        isHolding = true
                        tryAwaitRelease()
                        isHolding = false
                    }
                )
            },
        contentAlignment = Alignment.Center
    ) {
        Canvas(modifier = Modifier.fillMaxSize()) {
            val strokeWidth = 6.dp.toPx()
            // Background track
            drawArc(
                color = Color(0xFF1E293B),
                startAngle = 0f,
                sweepAngle = 360f,
                useCenter = false,
                style = Stroke(width = strokeWidth)
            )
            // Progress arc
            if (holdProgress > 0f) {
                drawArc(
                    color = Color(0xFFEF4444), // Red for emergency
                    startAngle = -90f,
                    sweepAngle = 360f * holdProgress,
                    useCenter = false,
                    style = Stroke(width = strokeWidth, cap = StrokeCap.Round)
                )
            }
        }
        
        Column(horizontalAlignment = Alignment.CenterHorizontally) {
            Text(
                text = "HOLD",
                style = MaterialTheme.typography.labelLarge,
                fontWeight = FontWeight.Bold,
                color = Color(0xFFEF4444),
                letterSpacing = 2.sp
            )
            Text(
                text = "TO EXIT",
                style = MaterialTheme.typography.labelSmall,
                color = Color(0xFF94A3B8)
            )
        }
    }
}

@Composable
fun PermissionGateScreen(
    isDarkTheme: Boolean,
    isAccessibilityActive: Boolean,
    isNotificationActive: Boolean,
    isDeviceAdminActive: Boolean,
    onEnableAccessibility: () -> Unit,
    onEnableNotification: () -> Unit,
    onEnableDeviceAdmin: () -> Unit,
    selectedLang: String,
    onLangChange: (String) -> Unit
) {
    val infiniteTransition = rememberInfiniteTransition(label = "ShieldPulse")
    val pulseScale by infiniteTransition.animateFloat(
        initialValue = 1f,
        targetValue = 1.05f,
        animationSpec = infiniteRepeatable(
            animation = tween(2500, easing = FastOutSlowInEasing),
            repeatMode = RepeatMode.Reverse
        ),
        label = "pulse"
    )

    val screenBg = if (isDarkTheme) Color(0xFF0F0F12) else com.example.ui.theme.FrostedBackgroundLight
    val textColor = if (isDarkTheme) Color(0xFFF1F5F9) else com.example.ui.theme.FrostedTextLight
    val textSecondary = if (isDarkTheme) Color(0xFF94A3B8) else com.example.ui.theme.FrostedTextSecondaryLight

    val layoutDirection = if (selectedLang == "ar") LayoutDirection.Rtl else LayoutDirection.Ltr

    CompositionLocalProvider(LocalLayoutDirection provides layoutDirection) {
        Scaffold(
            modifier = Modifier.fillMaxSize(),
            containerColor = screenBg
        ) { innerPadding ->
            Column(
                modifier = Modifier
                    .padding(innerPadding)
                    .fillMaxSize()
                    .background(screenBg)
                    .verticalScroll(rememberScrollState())
                    .padding(horizontal = 24.dp, vertical = 24.dp),
                horizontalAlignment = Alignment.CenterHorizontally
            ) {
                
                Spacer(modifier = Modifier.height(16.dp))

                // Language selector row
                Box(
                    modifier = Modifier
                        .fillMaxWidth()
                        .clip(RoundedCornerShape(24.dp))
                        .background(if (isDarkTheme) Color.White.copy(alpha = 0.04f) else Color.Black.copy(alpha = 0.02f))
                        .border(1.dp, if (isDarkTheme) Color.White.copy(alpha = 0.06f) else Color.Black.copy(alpha = 0.04f), RoundedCornerShape(24.dp))
                        .padding(horizontal = 16.dp, vertical = 10.dp)
                ) {
                    Column(horizontalAlignment = Alignment.CenterHorizontally, modifier = Modifier.fillMaxWidth()) {
                        Text(
                            text = Translations.get("language_selection", selectedLang),
                            style = MaterialTheme.typography.labelSmall.copy(fontWeight = FontWeight.Bold),
                            color = com.example.ui.theme.FrostedPrimary,
                            modifier = Modifier.padding(bottom = 8.dp)
                        )
                        LazyRow(
                            horizontalArrangement = Arrangement.spacedBy(8.dp),
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            items(Translations.LANGUAGES) { lang ->
                                val isSelected = lang.code == selectedLang
                                Box(
                                    modifier = Modifier
                                        .clip(RoundedCornerShape(16.dp))
                                        .background(
                                            if (isSelected) {
                                                com.example.ui.theme.FrostedPrimary
                                            } else {
                                                if (isDarkTheme) Color(0xFF334155).copy(alpha = 0.3f) else Color(0xFFE2E8F0)
                                            }
                                        )
                                        .clickable {
                                            onLangChange(lang.code)
                                        }
                                        .padding(horizontal = 12.dp, vertical = 6.dp),
                                    contentAlignment = Alignment.Center
                                ) {
                                    Row(verticalAlignment = Alignment.CenterVertically) {
                                        Text(text = lang.flag, style = MaterialTheme.typography.bodyMedium)
                                        Spacer(modifier = Modifier.width(6.dp))
                                        Text(
                                            text = lang.name,
                                            style = MaterialTheme.typography.labelMedium.copy(fontWeight = FontWeight.Bold),
                                            color = if (isSelected) Color.White else (if (isDarkTheme) Color.White.copy(alpha = 0.8f) else Color(0xFF475569))
                                        )
                                    }
                                }
                            }
                        }
                    }
                }

                Spacer(modifier = Modifier.height(24.dp))

                Box(
                    contentAlignment = Alignment.Center,
                    modifier = Modifier.size(140.dp)
                ) {
                    Box(
                        modifier = Modifier
                            .size(120.dp)
                            .scale(pulseScale)
                            .clip(CircleShape)
                            .background(
                                Brush.radialGradient(
                                    colors = listOf(
                                        com.example.ui.theme.FrostedPrimary.copy(alpha = 0.25f),
                                        Color.Transparent
                                    )
                                )
                            )
                    )

                    Box(
                        modifier = Modifier
                            .size(80.dp)
                            .clip(CircleShape)
                            .background(if (isDarkTheme) Color.White.copy(alpha = 0.05f) else Color.White)
                            .border(1.dp, if (isDarkTheme) Color.White.copy(alpha = 0.15f) else Color(0xFFE2E8F0), CircleShape)
                            .padding(12.dp),
                        contentAlignment = Alignment.Center
                    ) {
                        Icon(
                            imageVector = Icons.Filled.Security,
                            contentDescription = "Shield Icon",
                            tint = com.example.ui.theme.FrostedPrimary,
                            modifier = Modifier
                                .size(42.dp)
                                .scale(pulseScale)
                        )
                    }
                }

                Spacer(modifier = Modifier.height(24.dp))

                Text(
                    text = Translations.get("permission_setup_title", selectedLang),
                    style = MaterialTheme.typography.headlineMedium.copy(
                        fontWeight = FontWeight.ExtraBold,
                        letterSpacing = (-0.5).sp
                    ),
                    color = textColor,
                    textAlign = TextAlign.Center
                )

                Spacer(modifier = Modifier.height(10.dp))

                Text(
                    text = Translations.get("permission_setup_desc", selectedLang),
                    style = MaterialTheme.typography.bodyMedium.copy(lineHeight = 22.sp),
                    color = textSecondary,
                    textAlign = TextAlign.Center,
                    modifier = Modifier.padding(horizontal = 8.dp)
                )

                Spacer(modifier = Modifier.height(32.dp))

                Column(
                    modifier = Modifier.fillMaxWidth(),
                    verticalArrangement = Arrangement.spacedBy(16.dp)
                ) {
                    PermissionCard(
                        title = Translations.get("perm_accessibility_title", selectedLang),
                        description = Translations.get("perm_accessibility_desc", selectedLang),
                        isGranted = isAccessibilityActive,
                        isDarkTheme = isDarkTheme,
                        icon = Icons.Filled.AccessibilityNew,
                        onClick = onEnableAccessibility,
                        selectedLang = selectedLang
                    )

                    PermissionCard(
                        title = Translations.get("perm_admin_title", selectedLang),
                        description = Translations.get("perm_admin_desc", selectedLang),
                        isGranted = isDeviceAdminActive,
                        isDarkTheme = isDarkTheme,
                        icon = Icons.Filled.AdminPanelSettings,
                        onClick = onEnableDeviceAdmin,
                        selectedLang = selectedLang
                    )

                    PermissionCard(
                        title = Translations.get("perm_notif_title", selectedLang),
                        description = Translations.get("perm_notif_desc", selectedLang),
                        isGranted = isNotificationActive,
                        isDarkTheme = isDarkTheme,
                        icon = Icons.Filled.NotificationsOff,
                        onClick = onEnableNotification,
                        selectedLang = selectedLang
                    )
                }

                Spacer(modifier = Modifier.height(32.dp))

                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.Center,
                    modifier = Modifier.padding(bottom = 16.dp)
                ) {
                    CircularProgressIndicator(
                        modifier = Modifier.size(16.dp),
                        color = com.example.ui.theme.FrostedPrimary,
                        strokeWidth = 2.dp
                    )
                    Spacer(modifier = Modifier.width(12.dp))
                    Text(
                        text = Translations.get("perm_waiting", selectedLang),
                        style = MaterialTheme.typography.labelSmall,
                        color = textColor.copy(alpha = 0.6f)
                    )
                }
            }
        }
    }
}

@Composable
fun PermissionCard(
    title: String,
    description: String,
    isGranted: Boolean,
    isDarkTheme: Boolean,
    icon: androidx.compose.ui.graphics.vector.ImageVector,
    onClick: () -> Unit,
    selectedLang: String
) {
    val borderColor = if (isGranted) Color(0xFF10B981) else (if (isDarkTheme) Color.White.copy(alpha = 0.08f) else Color(0xFFE2E8F0))
    val bgColor = if (isGranted) Color(0xFF10B981).copy(alpha = 0.05f) else (if (isDarkTheme) Color.White.copy(alpha = 0.04f) else Color(0xFFF1F5F9))

    Row(
        modifier = Modifier
            .fillMaxWidth()
            .clip(RoundedCornerShape(20.dp))
            .background(bgColor)
            .border(1.dp, borderColor, RoundedCornerShape(20.dp))
            .clickable(enabled = !isGranted, onClick = onClick)
            .padding(16.dp),
        verticalAlignment = Alignment.CenterVertically
    ) {
        Box(
            modifier = Modifier
                .size(42.dp)
                .clip(CircleShape)
                .background(if (isGranted) Color(0xFF10B981).copy(alpha = 0.15f) else (if (isDarkTheme) Color.White.copy(alpha = 0.1f) else Color.Black.copy(alpha = 0.05f))),
            contentAlignment = Alignment.Center
        ) {
            Icon(
                imageVector = if (isGranted) Icons.Filled.Check else icon,
                contentDescription = null,
                tint = if (isGranted) Color(0xFF10B981) else (if (isDarkTheme) Color.White else Color(0xFF475569)),
                modifier = Modifier.size(22.dp)
            )
        }
        Spacer(modifier = Modifier.width(16.dp))
        Column(modifier = Modifier.weight(1f)) {
            Text(
                text = title,
                style = MaterialTheme.typography.titleSmall.copy(fontWeight = FontWeight.Bold),
                color = if (isGranted) Color(0xFF10B981) else (if (isDarkTheme) Color.White else Color(0xFF0F172A))
            )
            Spacer(modifier = Modifier.height(4.dp))
            Text(
                text = description,
                style = MaterialTheme.typography.labelSmall.copy(lineHeight = 14.sp),
                color = if (isDarkTheme) Color(0xFF94A3B8) else Color(0xFF64748B)
            )
        }
        
        if (!isGranted) {
            val arrowIcon = if (selectedLang == "ar") Icons.Default.KeyboardArrowLeft else Icons.Default.KeyboardArrowRight
            Icon(
                imageVector = arrowIcon,
                contentDescription = "Grant Permission",
                tint = if (isDarkTheme) Color.White.copy(alpha = 0.5f) else Color(0xFF475569),
                modifier = Modifier.size(24.dp)
            )
        }
    }
}

@Composable
fun textSecondary(): Color = Color(0xFF94A3B8)

@Composable
fun InstructionStepRow(
    stepNumber: String,
    title: String,
    desc: String,
    textColor: Color
) {
    Row(
        modifier = Modifier.fillMaxWidth(),
        verticalAlignment = Alignment.Top
    ) {
        Box(
            modifier = Modifier
                .size(26.dp)
                .clip(CircleShape)
                .background(com.example.ui.theme.FrostedPrimary.copy(alpha = 0.15f))
                .border(1.dp, com.example.ui.theme.FrostedPrimary.copy(alpha = 0.3f), CircleShape),
            contentAlignment = Alignment.Center
        ) {
            Text(
                text = stepNumber,
                style = MaterialTheme.typography.labelSmall.copy(fontWeight = FontWeight.Black),
                color = com.example.ui.theme.FrostedPrimary
            )
        }
        Spacer(modifier = Modifier.width(12.dp))
        Column {
            Text(
                text = title,
                style = MaterialTheme.typography.bodyMedium.copy(fontWeight = FontWeight.Bold),
                color = textColor
            )
            Spacer(modifier = Modifier.height(2.dp))
            Text(
                text = desc,
                style = MaterialTheme.typography.bodySmall,
                color = textColor.copy(alpha = 0.6f)
            )
        }
    }
}

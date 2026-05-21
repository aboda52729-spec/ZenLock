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
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.grid.GridCells
import androidx.compose.foundation.lazy.grid.LazyVerticalGrid
import androidx.compose.foundation.lazy.grid.items
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.*
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
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.ui.window.Dialog
import androidx.compose.ui.window.DialogProperties
import com.example.ui.theme.MyApplicationTheme
import kotlinx.coroutines.delay

class MainActivity : ComponentActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()
        setContent {
            var isDarkTheme by remember { mutableStateOf<Boolean?>(null) }
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
                ZenLockApp(
                    isDarkTheme = useDark,
                    onToggleTheme = { isDarkTheme = !useDark }
                )
            }
        }
    }
}

data class DeviceAppInfo(
    val packageName: String,
    val label: String,
    val color: Color
)

@Composable
fun AppIcon(
    packageName: String,
    modifier: Modifier = Modifier,
    fallbackLabel: String,
    fallbackColor: Color,
    textStyle: androidx.compose.ui.text.TextStyle = MaterialTheme.typography.titleMedium
) {
    val context = LocalContext.current
    val appIcon = remember(packageName) {
        try {
            val pm = context.packageManager
            val drawable = pm.getApplicationIcon(packageName)
            drawable.toBitmap(width = 120, height = 120).asImageBitmap()
        } catch (e: Exception) {
            null
        }
    }

    if (appIcon != null) {
        Image(
            bitmap = appIcon,
            contentDescription = "$fallbackLabel icon",
            modifier = modifier
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

@Composable
fun ZenLockApp(isDarkTheme: Boolean, onToggleTheme: () -> Unit) {
    val context = LocalContext.current
    
    // Core state loaded selectively from real persisted settings
    var isLockdown by remember { mutableStateOf(LockSettings.isLockdownActive(context)) }
    var selectedApps by remember { mutableStateOf(LockSettings.getBlockedApps(context)) }
    var lockdownDurationSecs by remember { mutableIntStateOf(25 * 60) } // Default 25 min
    
    // Loaded list of launcher applications
    var installedApps by remember { mutableStateOf<List<DeviceAppInfo>>(emptyList()) }
    
    // Shield configuration state
    var isShieldActive by remember { mutableStateOf(isAccessibilityServiceEnabled(context)) }

    // Fetch installed apps on launch
    LaunchedEffect(Unit) {
        installedApps = getInstalledLauncherApps(context)
    }

    // Continuously monitor shield state when in app
    LaunchedEffect(isLockdown) {
        while (true) {
            isShieldActive = isAccessibilityServiceEnabled(context)
            delay(2000)
        }
    }

    val infiniteTransition = rememberInfiniteTransition(label = "BgAnimation")
    val glowAnimX1 by infiniteTransition.animateFloat(
        initialValue = 0.05f,
        targetValue = 0.15f,
        animationSpec = infiniteRepeatable(
            animation = tween(4000, easing = LinearEasing),
            repeatMode = RepeatMode.Reverse
        ),
        label = "glowX1"
    )
    val glowAnimY1 by infiniteTransition.animateFloat(
        initialValue = 0.05f,
        targetValue = 0.15f,
        animationSpec = infiniteRepeatable(
            animation = tween(5000, easing = LinearEasing),
            repeatMode = RepeatMode.Reverse
        ),
        label = "glowY1"
    )
    val glowAnimX2 by infiniteTransition.animateFloat(
        initialValue = 0.85f,
        targetValue = 0.95f,
        animationSpec = infiniteRepeatable(
            animation = tween(6000, easing = LinearEasing),
            repeatMode = RepeatMode.Reverse
        ),
        label = "glowX2"
    )
    val glowAnimY2 by infiniteTransition.animateFloat(
        initialValue = 0.85f,
        targetValue = 0.95f,
        animationSpec = infiniteRepeatable(
            animation = tween(4500, easing = LinearEasing),
            repeatMode = RepeatMode.Reverse
        ),
        label = "glowY2"
    )

    Box(
        modifier = Modifier
            .fillMaxSize()
            .background(if (isDarkTheme) com.example.ui.theme.FrostedBackgroundDark else com.example.ui.theme.FrostedBackgroundLight)
            .drawBehind {
                drawCircle(
                    brush = Brush.radialGradient(
                        colors = listOf(com.example.ui.theme.FrostedPurple.copy(alpha = if (isDarkTheme) 0.35f else 0.18f), Color.Transparent),
                        center = Offset(size.width * glowAnimX1, size.height * glowAnimY1),
                        radius = size.width * 0.9f
                    ),
                    radius = size.width * 0.9f,
                    center = Offset(size.width * glowAnimX1, size.height * glowAnimY1)
                )
                drawCircle(
                    brush = Brush.radialGradient(
                        colors = listOf(com.example.ui.theme.FrostedBlue.copy(alpha = if (isDarkTheme) 0.35f else 0.18f), Color.Transparent),
                        center = Offset(size.width * glowAnimX2, size.height * glowAnimY2),
                        radius = size.width * 0.9f
                    ),
                    radius = size.width * 0.9f,
                    center = Offset(size.width * glowAnimX2, size.height * glowAnimY2)
                )
            }
    ) {
        AnimatedContent(
            targetState = isLockdown,
            transitionSpec = {
                (fadeIn(animationSpec = tween(600)) + scaleIn(initialScale = 0.9f)) togetherWith
                        (fadeOut(animationSpec = tween(600)) + scaleOut(targetScale = 1.1f))
            },
            label = "AppScreenTransition"
        ) { lockdown ->
            if (lockdown) {
                val activeEndTime = LockSettings.getLockdownEndTime(context)
                val currentRem = maxOf(0L, (activeEndTime - System.currentTimeMillis()) / 1000L).toInt()
                val actualTime = if (currentRem > 0) currentRem else lockdownDurationSecs
                
                LockdownScreen(
                    durationSecs = actualTime,
                    onUnlock = {
                        LockSettings.setLockdown(context, false)
                        isLockdown = false
                    }
                )
            } else {
                SetupScreen(
                    isDarkTheme = isDarkTheme,
                    onToggleTheme = onToggleTheme,
                    durationMins = lockdownDurationSecs / 60,
                    onDurationChange = { lockdownDurationSecs = it * 60 },
                    selectedApps = selectedApps,
                    installedApps = installedApps,
                    isShieldActive = isShieldActive,
                    onAppToggle = { pkgName ->
                        selectedApps = if (selectedApps.contains(pkgName)) {
                            selectedApps - pkgName
                        } else {
                            selectedApps + pkgName
                        }
                        LockSettings.setBlockedApps(context, selectedApps)
                    },
                    onStart = {
                        if (selectedApps.isNotEmpty()) {
                            LockSettings.setBlockedApps(context, selectedApps)
                            LockSettings.setLockdown(context, true, lockdownDurationSecs)
                            isLockdown = true
                        }
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
    durationMins: Int,
    onDurationChange: (Int) -> Unit,
    selectedApps: Set<String>,
    installedApps: List<DeviceAppInfo>,
    isShieldActive: Boolean,
    onAppToggle: (String) -> Unit,
    onStart: () -> Unit
) {
    val context = LocalContext.current
    var showAppPicker by remember { mutableStateOf(false) }

    Scaffold(
        modifier = Modifier.fillMaxSize(),
        containerColor = Color.Transparent
    ) { innerPadding ->
        Column(
            modifier = Modifier
                .padding(innerPadding)
                .fillMaxSize()
                .verticalScroll(rememberScrollState())
                .padding(horizontal = 24.dp),
            horizontalAlignment = Alignment.CenterHorizontally
        ) {
            Spacer(modifier = Modifier.height(16.dp))
            HeaderBlock(isDarkTheme, onToggleTheme)
            Spacer(modifier = Modifier.height(24.dp))
            
            // System-level Accessibility Link banner (System Shield)
            ShieldStatusBanner(
                isActive = isShieldActive,
                onClick = {
                    val intent = Intent(Settings.ACTION_ACCESSIBILITY_SETTINGS)
                    context.startActivity(intent)
                }
            )
            
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
                            text = "Shielded Apps",
                            style = MaterialTheme.typography.titleMedium,
                            color = MaterialTheme.colorScheme.onBackground,
                            fontWeight = FontWeight.Bold
                        )
                        Text(
                            text = "${selectedApps.size} Locked",
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
                                text = "No apps targeted.\nTap below to select apps to lock.",
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
                        Text(text = "Add / Edit Block List", style = MaterialTheme.typography.labelLarge)
                    }
                }
            }
            
            Spacer(modifier = Modifier.height(24.dp))
            
            DurationSelector(durationMins, onDurationChange)
            
            Spacer(modifier = Modifier.height(32.dp))
            
            StartButton(onStart, enabled = selectedApps.isNotEmpty())
            
            Spacer(modifier = Modifier.height(32.dp))
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
    Row(
        verticalAlignment = Alignment.CenterVertically,
        modifier = Modifier
            .clip(RoundedCornerShape(12.dp))
            .background(app.color.copy(alpha = 0.15f))
            .border(1.dp, app.color.copy(alpha = 0.3f), RoundedCornerShape(12.dp))
            .clickable { onRemove() }
            .padding(horizontal = 10.dp, vertical = 6.dp)
    ) {
        AppIcon(
            packageName = app.packageName,
            modifier = Modifier
                .size(20.dp)
                .clip(CircleShape),
            fallbackLabel = app.label,
            fallbackColor = app.color,
            textStyle = MaterialTheme.typography.labelSmall.copy(fontSize = 11.sp, fontWeight = FontWeight.Bold)
        )
        Spacer(modifier = Modifier.width(6.dp))
        Text(
            text = app.label,
            style = MaterialTheme.typography.labelSmall,
            color = app.color,
            fontWeight = FontWeight.Medium
        )
        Spacer(modifier = Modifier.width(4.dp))
        Icon(
            imageVector = Icons.Filled.Close,
            contentDescription = "Remove",
            tint = app.color,
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

@Composable
fun AppSelectionRow(
    app: DeviceAppInfo,
    isChecked: Boolean,
    onToggle: () -> Unit
) {
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
                fallbackLabel = app.label,
                fallbackColor = app.color
            )
            Spacer(modifier = Modifier.width(16.dp))
            Column {
                Text(
                    text = app.label,
                    style = MaterialTheme.typography.bodyLarge,
                    fontWeight = FontWeight.SemiBold,
                    color = MaterialTheme.colorScheme.onSurface
                )
                Text(
                    text = app.packageName,
                    style = MaterialTheme.typography.labelSmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
            }
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
fun HeaderBlock(isDarkTheme: Boolean, onToggleTheme: () -> Unit) {
    Row(
        modifier = Modifier.fillMaxWidth(),
        horizontalArrangement = Arrangement.SpaceBetween,
        verticalAlignment = Alignment.CenterVertically
    ) {
        Column {
            Text(
                text = "ZenLock",
                style = MaterialTheme.typography.headlineLarge,
                fontWeight = FontWeight.ExtraBold,
                color = MaterialTheme.colorScheme.onBackground
            )
            Text(
                text = "Deep Focus Shield",
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
fun DurationSelector(durationMins: Int, onDurationChange: (Int) -> Unit) {
    Box(
        modifier = Modifier
            .fillMaxWidth()
            .clip(RoundedCornerShape(28.dp))
            .background(Color.White.copy(alpha = 0.04f))
            .border(1.dp, Color.White.copy(alpha = 0.08f), RoundedCornerShape(28.dp))
            .padding(20.dp)
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
                Text(
                    text = "Focus Duration",
                    style = MaterialTheme.typography.titleMedium,
                    fontWeight = FontWeight.Bold,
                    color = MaterialTheme.colorScheme.onBackground
                )
                
                val adviceText = when {
                    durationMins <= 15 -> "Quick Burst"
                    durationMins <= 30 -> "Pomodoro"
                    durationMins <= 60 -> "Deep Focus"
                    else -> "Extreme Focus"
                }
                Text(
                    text = adviceText,
                    style = MaterialTheme.typography.labelMedium,
                    color = MaterialTheme.colorScheme.primary,
                    fontWeight = FontWeight.SemiBold,
                    modifier = Modifier
                        .clip(RoundedCornerShape(8.dp))
                        .background(MaterialTheme.colorScheme.primary.copy(alpha = 0.15f))
                        .padding(horizontal = 8.dp, vertical = 4.dp)
                )
            }
            
            Spacer(modifier = Modifier.height(16.dp))
            
            Row(
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.Center,
                modifier = Modifier.fillMaxWidth()
            ) {
                Text(
                    text = "$durationMins",
                    style = MaterialTheme.typography.displayMedium.copy(
                        fontSize = 44.sp,
                        fontWeight = FontWeight.Black
                    ),
                    color = MaterialTheme.colorScheme.onBackground
                )
                Spacer(modifier = Modifier.width(6.dp))
                Text(
                    text = "minutes",
                    style = MaterialTheme.typography.titleMedium,
                    fontWeight = FontWeight.Bold,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
            }
            
            Spacer(modifier = Modifier.height(12.dp))
            
            Slider(
                value = durationMins.toFloat(),
                onValueChange = { onDurationChange(it.toInt()) },
                valueRange = 5f..120f,
                steps = 22,
                colors = SliderDefaults.colors(
                    thumbColor = MaterialTheme.colorScheme.primary,
                    activeTrackColor = MaterialTheme.colorScheme.primary,
                    inactiveTrackColor = Color.White.copy(alpha = 0.1f),
                    activeTickColor = Color.Transparent,
                    inactiveTickColor = Color.Transparent
                ),
                modifier = Modifier.fillMaxWidth()
            )
            
            Spacer(modifier = Modifier.height(8.dp))
            Text(
                text = "Locks selected distracting apps completely until the timer expires.",
                style = MaterialTheme.typography.labelSmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
                textAlign = TextAlign.Center,
                modifier = Modifier.fillMaxWidth()
            )
        }
    }
}

@Composable
fun StartButton(onClick: () -> Unit, enabled: Boolean) {
    var isPressed by remember { mutableStateOf(false) }
    val scale by animateFloatAsState(
        targetValue = if (isPressed) 0.95f else 1f,
        animationSpec = spring(dampingRatio = Spring.DampingRatioMediumBouncy),
        label = "StartBtnScale"
    )
    
    val gradientColors = if (enabled) {
        listOf(com.example.ui.theme.FrostedPurple, com.example.ui.theme.FrostedPrimary)
    } else {
        listOf(Color.White.copy(alpha = 0.08f), Color.White.copy(alpha = 0.08f))
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
                        listOf(Color.White.copy(alpha = 0.3f), Color.Transparent)
                    } else {
                        listOf(Color.White.copy(alpha = 0.05f), Color.Transparent)
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
                tint = if (enabled) Color.White else Color.White.copy(alpha = 0.3f),
                modifier = Modifier.size(20.dp)
            )
            Spacer(modifier = Modifier.width(10.dp))
            Text(
                text = "ACTIVATE LOCK",
                style = MaterialTheme.typography.titleMedium,
                fontWeight = FontWeight.Black,
                color = if (enabled) Color.White else Color.White.copy(alpha = 0.3f),
                letterSpacing = 1.5.sp
            )
        }
    }
}

@Composable
fun LockdownScreen(durationSecs: Int, onUnlock: () -> Unit) {
    var timeLeft by remember { mutableIntStateOf(durationSecs) }
    
    LaunchedEffect(Unit) {
        while (timeLeft > 0) {
            delay(1000)
            timeLeft--
        }
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
                    color = Color.White
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
                        color = Color.White.copy(alpha = 0.04f),
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
                        .background(Color.White.copy(alpha = 0.05f))
                        .border(1.dp, Color.White.copy(alpha = 0.12f), CircleShape),
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
                            color = Color.White
                        )
                        Spacer(modifier = Modifier.height(6.dp))
                        Text(
                            text = "until deep-unlock",
                            style = MaterialTheme.typography.labelSmall,
                            color = Color.White.copy(alpha = 0.5f)
                        )
                    }
                }
            }

            Spacer(modifier = Modifier.height(56.dp))

            Text(
                text = "NO DISTRACTIONS",
                style = MaterialTheme.typography.titleMedium.copy(letterSpacing = 3.sp),
                fontWeight = FontWeight.Black,
                color = Color.White
            )
            Spacer(modifier = Modifier.height(8.dp))
            Text(
                text = "Loopholes are disabled. Stay in flow.",
                style = MaterialTheme.typography.bodySmall,
                color = Color.White.copy(alpha = 0.5f),
                textAlign = TextAlign.Center
            )

            Spacer(modifier = Modifier.height(64.dp))

            EmergencyUnlockButton(onUnlock)
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

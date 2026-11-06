package com.ghorerkhobor.user

import android.content.Context
import android.content.Intent
import android.net.Uri
import android.os.Bundle
import android.os.PowerManager
import android.provider.Settings
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.material3.Button
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.Divider
import androidx.compose.material3.DropdownMenu
import androidx.compose.material3.DropdownMenuItem
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Surface
import androidx.compose.material3.Switch
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.material3.TopAppBar
import androidx.compose.material3.TopAppBarDefaults
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.unit.dp
import androidx.work.WorkInfo
import com.ghorerkhobor.user.data.AppDatabase
import com.ghorerkhobor.user.data.Prefs
import com.ghorerkhobor.user.sync.SyncScheduler
import com.ghorerkhobor.user.ui.theme.GhorerKhoborTheme
import com.ghorerkhobor.user.ui.theme.TealPrimary
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale

class MainActivity : ComponentActivity() {

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContent {
            GhorerKhoborTheme {
                RootScreen()
            }
        }
    }
}

@Composable
fun RootScreen() {
    val context = LocalContext.current
    var setupDone by remember { mutableStateOf(Prefs.isRegistered(context) || Prefs.serverUrl(context).isNotBlank()) }

    Surface(modifier = Modifier.fillMaxSize(), color = MaterialTheme.colorScheme.background) {
        if (!setupDone) {
            SetupScreen(onSetupSaved = { setupDone = true })
        } else {
            StatusScreen(onEditSetup = { setupDone = false })
        }
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun SetupScreen(onSetupSaved: () -> Unit) {
    val context = LocalContext.current
    var serverUrl by remember { mutableStateOf(Prefs.serverUrl(context)) }
    var apiKey by remember { mutableStateOf(Prefs.apiKey(context)) }
    var person by remember { mutableStateOf(Prefs.personName(context)) }
    var role by remember { mutableStateOf(Prefs.role(context).ifBlank { "ছেলে" }) }
    var roleMenuOpen by remember { mutableStateOf(false) }
    val roles = listOf("ছেলে", "মেয়ে", "আম্মু", "অন্যান্য")

    Scaffold(topBar = { AppTopBar(title = "প্রাথমিক সেটআপ") }) { padding ->
    Column(
        modifier = Modifier
            .padding(padding)
            .padding(24.dp)
            .fillMaxSize()
            .verticalScroll(rememberScrollState())
    ) {
        Text("প্রাথমিক সেটআপ", style = MaterialTheme.typography.headlineSmall)
        Spacer(Modifier.height(4.dp))
        Text("এই ফোনটা কার, আর ব্যাকএন্ড ঠিকানা দিন", style = MaterialTheme.typography.bodySmall)

        Spacer(Modifier.height(20.dp))
        NotificationAccessRow()

        Spacer(Modifier.height(16.dp))
        BatteryOptimizationRow()

        Spacer(Modifier.height(24.dp))
        OutlinedTextField(
            value = person,
            onValueChange = { person = it },
            label = { Text("ব্যবহারকারীর নাম (যেমন: রাফি)") },
            modifier = Modifier.fillMaxWidth()
        )

        Spacer(Modifier.height(12.dp))
        Column {
            OutlinedButton(onClick = { roleMenuOpen = true }, modifier = Modifier.fillMaxWidth()) {
                Text("সম্পর্ক: $role")
            }
            DropdownMenu(expanded = roleMenuOpen, onDismissRequest = { roleMenuOpen = false }) {
                roles.forEach { option ->
                    DropdownMenuItem(text = { Text(option) }, onClick = {
                        role = option
                        roleMenuOpen = false
                    })
                }
            }
        }

        Spacer(Modifier.height(12.dp))
        OutlinedTextField(
            value = serverUrl,
            onValueChange = { serverUrl = it },
            label = { Text("Apps Script Web App URL") },
            modifier = Modifier.fillMaxWidth()
        )

        Spacer(Modifier.height(12.dp))
        OutlinedTextField(
            value = apiKey,
            onValueChange = { apiKey = it },
            label = { Text("API Key") },
            modifier = Modifier.fillMaxWidth()
        )

        Spacer(Modifier.height(24.dp))
        Button(
            onClick = {
                Prefs.saveSetup(context, serverUrl, apiKey, person, role)
                Prefs.setRegistered(context, false)
                SyncScheduler.schedulePeriodic(context)
                SyncScheduler.syncNow(context)
                onSetupSaved()
            },
            modifier = Modifier.fillMaxWidth(),
            enabled = serverUrl.isNotBlank() && apiKey.isNotBlank() && person.isNotBlank()
        ) {
            Text("সংরক্ষণ করুন ও রেজিস্টার করুন")
        }
    }
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun StatusScreen(onEditSetup: () -> Unit) {
    val context = LocalContext.current
    val db = remember { AppDatabase.get(context) }
    val savedCount by db.messageDao().countFlow().collectAsState(initial = 0)
    val pendingCount by db.messageDao().unsyncedCountFlow().collectAsState(initial = 0)

    var stopped by remember { mutableStateOf(Prefs.isStopped(context)) }
    var lastSyncAt by remember { mutableStateOf(Prefs.lastSyncAt(context)) }
    var lastSyncResult by remember { mutableStateOf(Prefs.lastSyncResult(context)) }
    val syncState by SyncScheduler.manualSyncStateFlow(context).collectAsState(initial = null)
    val syncing = syncState == WorkInfo.State.ENQUEUED || syncState == WorkInfo.State.RUNNING

    LaunchedEffect(syncState) {
        if (syncState != null && syncState!!.isFinished) {
            lastSyncAt = Prefs.lastSyncAt(context)
            lastSyncResult = Prefs.lastSyncResult(context)
        }
    }

    Scaffold(topBar = { AppTopBar(title = "ঘরের খবর") }) { padding ->
    Column(
        modifier = Modifier
            .padding(padding)
            .padding(24.dp)
            .fillMaxSize()
            .verticalScroll(rememberScrollState())
    ) {
        Text("${Prefs.personName(context)} (${Prefs.role(context)})", style = MaterialTheme.typography.bodyMedium)

        Spacer(Modifier.height(20.dp))
        NotificationAccessRow()

        Spacer(Modifier.height(16.dp))
        BatteryOptimizationRow()

        Spacer(Modifier.height(24.dp))
        Divider()
        Spacer(Modifier.height(16.dp))

        Row(verticalAlignment = Alignment.CenterVertically) {
            Text("ইমার্জেন্সি বন্ধ", modifier = Modifier.weight(1f))
            Switch(checked = stopped, onCheckedChange = {
                stopped = it
                Prefs.setStopped(context, it)
            })
        }
        Text(
            if (stopped) "অ্যাপ এখন মেসেজ ধরছে ও পাঠাচ্ছে না" else "অ্যাপ স্বাভাবিকভাবে কাজ করছে",
            style = MaterialTheme.typography.bodySmall
        )

        Spacer(Modifier.height(20.dp))
        Text("সংরক্ষিত মেসেজ: $savedCount")
        Text("এখনো না পাঠানো: $pendingCount")
        Text("শেষ সিঙ্ক: ${formatTime(lastSyncAt)}")
        if (lastSyncResult.isNotBlank()) {
            Text(lastSyncResult, style = MaterialTheme.typography.bodySmall)
        }

        Spacer(Modifier.height(12.dp))
        Button(
            onClick = {
                SyncScheduler.syncNow(context)
            },
            modifier = Modifier.fillMaxWidth(),
            enabled = !stopped && !syncing
        ) {
            if (syncing) {
                CircularProgressIndicator(modifier = Modifier.size(18.dp), color = Color.White)
                Spacer(Modifier.width(8.dp))
            }
            Text(if (syncing) "সিঙ্ক হচ্ছে..." else "এখনই সিঙ্ক করুন")
        }

        Spacer(Modifier.height(24.dp))
        TextButton(onClick = onEditSetup) {
            Text("সেটআপ পরিবর্তন করুন")
        }
    }
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun AppTopBar(title: String) {
    TopAppBar(
        title = { Text(title) },
        colors = TopAppBarDefaults.topAppBarColors(
            containerColor = TealPrimary,
            titleContentColor = Color.White
        )
    )
}

@Composable
fun NotificationAccessRow() {
    val context = LocalContext.current
    var hasAccess by remember { mutableStateOf(isNotificationAccessGranted(context)) }
    LaunchedEffect(Unit) { hasAccess = isNotificationAccessGranted(context) }

    Text(if (hasAccess) "নোটিফিকেশন অ্যাক্সেস: চালু আছে" else "নোটিফিকেশন অ্যাক্সেস: বন্ধ আছে")
    if (!hasAccess) {
        Spacer(Modifier.height(8.dp))
        Button(onClick = {
            context.startActivity(Intent(Settings.ACTION_NOTIFICATION_LISTENER_SETTINGS))
        }) {
            Text("নোটিফিকেশন অ্যাক্সেস দিন")
        }
    }
}

@Composable
fun BatteryOptimizationRow() {
    val context = LocalContext.current
    var ignoring by remember { mutableStateOf(isIgnoringBatteryOptimizations(context)) }
    LaunchedEffect(Unit) { ignoring = isIgnoringBatteryOptimizations(context) }

    Text(if (ignoring) "ব্যাটারি অপ্টিমাইজেশন: বাদ দেওয়া আছে" else "ব্যাটারি অপ্টিমাইজেশন: চালু আছে (সিঙ্ক দেরি হতে পারে)")
    if (!ignoring) {
        Spacer(Modifier.height(8.dp))
        Button(onClick = {
            val intent = Intent(Settings.ACTION_REQUEST_IGNORE_BATTERY_OPTIMIZATIONS)
            intent.data = Uri.parse("package:" + context.packageName)
            context.startActivity(intent)
        }) {
            Text("ব্যাটারি অপ্টিমাইজেশন থেকে বাদ দিন")
        }
    }
}

fun isNotificationAccessGranted(context: Context): Boolean {
    val enabled = Settings.Secure.getString(context.contentResolver, "enabled_notification_listeners")
    return enabled?.contains(context.packageName) == true
}

fun isIgnoringBatteryOptimizations(context: Context): Boolean {
    val powerManager = context.getSystemService(Context.POWER_SERVICE) as PowerManager
    return powerManager.isIgnoringBatteryOptimizations(context.packageName)
}

fun formatTime(millis: Long): String {
    if (millis == 0L) return "এখনো হয়নি"
    val format = SimpleDateFormat("dd MMM, hh:mm a", Locale("bn"))
    return format.format(Date(millis))
}

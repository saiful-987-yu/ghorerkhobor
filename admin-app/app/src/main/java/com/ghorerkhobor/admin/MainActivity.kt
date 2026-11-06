package com.ghorerkhobor.admin

import android.graphics.BitmapFactory
import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.compose.foundation.Image
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.ArrowBack
import androidx.compose.material.icons.filled.DarkMode
import androidx.compose.material.icons.filled.LightMode
import androidx.compose.material.icons.filled.Refresh
import androidx.compose.material.icons.filled.Settings
import androidx.compose.material3.Button
import androidx.compose.material3.Card
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.DatePicker
import androidx.compose.material3.DatePickerDialog
import androidx.compose.material3.DropdownMenu
import androidx.compose.material3.DropdownMenuItem
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.material3.TopAppBar
import androidx.compose.material3.TopAppBarDefaults
import androidx.compose.material3.rememberDatePickerState
import androidx.compose.ui.graphics.Color
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateListOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.asImageBitmap
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.unit.dp
import com.ghorerkhobor.admin.data.Device
import com.ghorerkhobor.admin.data.Message
import com.ghorerkhobor.admin.data.Prefs
import com.ghorerkhobor.admin.network.AdminApiClient
import com.ghorerkhobor.admin.ui.theme.GhorerKhoborAdminTheme
import kotlinx.coroutines.launch
import java.text.SimpleDateFormat
import java.util.Calendar
import java.util.Date
import java.util.Locale

private const val PAGE_SIZE = 200
private const val MAX_PAGES_PER_LOAD = 10

sealed class Screen {
    object PinSetup : Screen()
    object PinEntry : Screen()
    object Settings : Screen()
    object ListScreen : Screen()
    data class Detail(val message: Message) : Screen()
}

class MainActivity : ComponentActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContent {
            val context = LocalContext.current
            var themeMode by remember { mutableStateOf(Prefs.themeMode(context)) }
            GhorerKhoborAdminTheme(themeMode = themeMode) {
                RootScreen(
                    themeMode = themeMode,
                    onThemeModeChange = {
                        themeMode = it
                        Prefs.setThemeMode(context, it)
                    }
                )
            }
        }
    }
}

@Composable
fun RootScreen(themeMode: String, onThemeModeChange: (String) -> Unit) {
    val context = LocalContext.current
    var screen by remember {
        mutableStateOf<Screen>(
            if (!Prefs.isPinSet(context)) Screen.PinSetup else Screen.PinEntry
        )
    }

    Surface(modifier = Modifier.fillMaxSize(), color = MaterialTheme.colorScheme.background) {
        when (val s = screen) {
            is Screen.PinSetup -> PinSetupScreen(onDone = {
                screen = if (Prefs.serverUrl(context).isBlank()) Screen.Settings else Screen.ListScreen
            })
            is Screen.PinEntry -> PinEntryScreen(onUnlocked = {
                screen = if (Prefs.serverUrl(context).isBlank()) Screen.Settings else Screen.ListScreen
            })
            is Screen.Settings -> SettingsScreen(onSaved = { screen = Screen.ListScreen })
            is Screen.ListScreen -> MessageListScreen(
                themeMode = themeMode,
                onThemeModeChange = onThemeModeChange,
                onOpenSettings = { screen = Screen.Settings },
                onOpenMessage = { screen = Screen.Detail(it) }
            )
            is Screen.Detail -> DetailScreen(message = s.message, onBack = { screen = Screen.ListScreen })
        }
    }
}

@Composable
fun PinSetupScreen(onDone: () -> Unit) {
    val context = LocalContext.current
    var pin by remember { mutableStateOf("") }
    var confirm by remember { mutableStateOf("") }
    var error by remember { mutableStateOf("") }

    Column(
        modifier = Modifier.fillMaxSize().padding(24.dp),
        verticalArrangement = Arrangement.Center
    ) {
        Text("একটা পিন সেট করুন", style = MaterialTheme.typography.headlineSmall)
        Text("অ্যাডমিন অ্যাপ খুলতে এই পিন লাগবে", style = MaterialTheme.typography.bodySmall)
        Spacer(Modifier.height(20.dp))
        OutlinedTextField(
            value = pin,
            onValueChange = { if (it.length <= 6) pin = it.filter { c -> c.isDigit() } },
            label = { Text("পিন (৪-৬ সংখ্যা)") },
            modifier = Modifier.fillMaxWidth()
        )
        Spacer(Modifier.height(12.dp))
        OutlinedTextField(
            value = confirm,
            onValueChange = { if (it.length <= 6) confirm = it.filter { c -> c.isDigit() } },
            label = { Text("আবার লিখুন") },
            modifier = Modifier.fillMaxWidth()
        )
        if (error.isNotBlank()) {
            Spacer(Modifier.height(8.dp))
            Text(error, color = MaterialTheme.colorScheme.error)
        }
        Spacer(Modifier.height(16.dp))
        Button(
            onClick = {
                if (pin.length < 4) {
                    error = "কমপক্ষে ৪ সংখ্যার পিন দিন"
                } else if (pin != confirm) {
                    error = "দুটো পিন মিলছে না"
                } else {
                    Prefs.setPin(context, pin)
                    onDone()
                }
            },
            modifier = Modifier.fillMaxWidth()
        ) {
            Text("পিন সেট করুন")
        }
    }
}

@Composable
fun PinEntryScreen(onUnlocked: () -> Unit) {
    val context = LocalContext.current
    var pin by remember { mutableStateOf("") }
    var error by remember { mutableStateOf("") }

    Column(
        modifier = Modifier.fillMaxSize().padding(24.dp),
        verticalArrangement = Arrangement.Center
    ) {
        Text("পিন দিন", style = MaterialTheme.typography.headlineSmall)
        Spacer(Modifier.height(16.dp))
        OutlinedTextField(
            value = pin,
            onValueChange = { if (it.length <= 6) pin = it.filter { c -> c.isDigit() } },
            label = { Text("পিন") },
            modifier = Modifier.fillMaxWidth()
        )
        if (error.isNotBlank()) {
            Spacer(Modifier.height(8.dp))
            Text(error, color = MaterialTheme.colorScheme.error)
        }
        Spacer(Modifier.height(16.dp))
        Button(
            onClick = {
                if (Prefs.verifyPin(context, pin)) {
                    onUnlocked()
                } else {
                    error = "পিন ভুল হয়েছে"
                    pin = ""
                }
            },
            modifier = Modifier.fillMaxWidth()
        ) {
            Text("খুলুন")
        }
    }
}

@Composable
fun SettingsScreen(onSaved: () -> Unit) {
    val context = LocalContext.current
    var serverUrl by remember { mutableStateOf(Prefs.serverUrl(context)) }
    var adminKey by remember { mutableStateOf(Prefs.adminKey(context)) }

    Column(modifier = Modifier.fillMaxSize().padding(24.dp)) {
        Text("সার্ভার সেটিংস", style = MaterialTheme.typography.headlineSmall)
        Spacer(Modifier.height(16.dp))
        OutlinedTextField(
            value = serverUrl,
            onValueChange = { serverUrl = it },
            label = { Text("Apps Script Web App URL") },
            modifier = Modifier.fillMaxWidth()
        )
        Spacer(Modifier.height(12.dp))
        OutlinedTextField(
            value = adminKey,
            onValueChange = { adminKey = it },
            label = { Text("Admin Key") },
            modifier = Modifier.fillMaxWidth()
        )
        Spacer(Modifier.height(20.dp))
        Button(
            onClick = {
                Prefs.saveServerSettings(context, serverUrl, adminKey)
                onSaved()
            },
            modifier = Modifier.fillMaxWidth(),
            enabled = serverUrl.isNotBlank() && adminKey.isNotBlank()
        ) {
            Text("সংরক্ষণ করুন")
        }
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun MessageListScreen(
    themeMode: String,
    onThemeModeChange: (String) -> Unit,
    onOpenSettings: () -> Unit,
    onOpenMessage: (Message) -> Unit
) {
    val context = LocalContext.current
    val scope = rememberCoroutineScope()

    val serverUrl = Prefs.serverUrl(context)
    val adminKey = Prefs.adminKey(context)

    var devices by remember { mutableStateOf<List<Device>>(emptyList()) }
    var selectedDeviceId by remember { mutableStateOf<String?>(null) }
    var selectedAppPackage by remember { mutableStateOf<String?>(null) }
    var selectedDateMillis by remember { mutableStateOf<Long?>(null) }
    var searchQuery by remember { mutableStateOf("") }

    val messages = remember { mutableStateListOf<Message>() }
    var loading by remember { mutableStateOf(false) }
    var hasMore by remember { mutableStateOf(false) }
    var lastCursor by remember { mutableStateOf<Long?>(null) }
    var errorText by remember { mutableStateOf("") }
    var showDatePicker by remember { mutableStateOf(false) }
    var personMenuOpen by remember { mutableStateOf(false) }
    var appMenuOpen by remember { mutableStateOf(false) }

    val appOptions = listOf(
        null to "সব অ্যাপ",
        "com.whatsapp" to "WhatsApp",
        "com.whatsapp.w4b" to "WhatsApp Business",
        "com.imo.android.imoim" to "imo",
        "com.imo.android.imoimhd" to "imo HD",
        "com.facebook.orca" to "Messenger",
        "org.telegram.messenger" to "Telegram",
        "com.viber.voip" to "Viber"
    )

    fun isFiltered() = selectedDateMillis != null || searchQuery.isNotBlank()

    fun dayRange(dayMillis: Long): Pair<Long, Long> {
        val cal = Calendar.getInstance()
        cal.timeInMillis = dayMillis
        cal.set(Calendar.HOUR_OF_DAY, 0)
        cal.set(Calendar.MINUTE, 0)
        cal.set(Calendar.SECOND, 0)
        cal.set(Calendar.MILLISECOND, 0)
        val start = cal.timeInMillis
        cal.add(Calendar.DAY_OF_MONTH, 1)
        val end = cal.timeInMillis
        return start to end
    }

    suspend fun fetchSimple(reset: Boolean) {
        val before = if (reset) null else lastCursor
        val result = AdminApiClient.list(serverUrl, adminKey, PAGE_SIZE, before, selectedDeviceId, selectedAppPackage)
        result.onSuccess { page ->
            if (reset) messages.clear()
            messages.addAll(page.items)
            hasMore = page.hasMore && page.items.isNotEmpty()
            lastCursor = page.items.lastOrNull()?.postedAt
            errorText = ""
        }.onFailure {
            errorText = "ডাটা আনা যায়নি, ইন্টারনেট ও সেটিংস দেখুন"
        }
    }

    suspend fun fetchFiltered(reset: Boolean) {
        var cursor = if (reset) null else lastCursor
        if (reset) messages.clear()
        val range = selectedDateMillis?.let { dayRange(it) }
        var pages = 0
        var stop = false
        var more = false
        while (pages < MAX_PAGES_PER_LOAD && !stop) {
            val result = AdminApiClient.list(serverUrl, adminKey, PAGE_SIZE, cursor, selectedDeviceId, selectedAppPackage)
            val page = result.getOrNull()
            if (page == null) {
                errorText = "ডাটা আনা যায়নি, ইন্টারনেট ও সেটিংস দেখুন"
                stop = true
                break
            }
            errorText = ""
            if (page.items.isEmpty()) {
                more = false
                stop = true
                break
            }
            val matches = page.items.filter { msg ->
                val dateOk = range == null || (msg.postedAt >= range.first && msg.postedAt < range.second)
                val text = (msg.sender + " " + msg.title + " " + msg.text + " " + msg.conversation).lowercase()
                val searchOk = searchQuery.isBlank() || text.contains(searchQuery.lowercase())
                dateOk && searchOk
            }
            messages.addAll(matches)
            cursor = page.items.last().postedAt
            pages += 1
            if (range != null && page.items.last().postedAt < range.first) {
                stop = true
                more = false
            } else if (!page.hasMore) {
                stop = true
                more = false
            } else if (pages >= MAX_PAGES_PER_LOAD) {
                more = true
            }
        }
        hasMore = more
        lastCursor = cursor
    }

    suspend fun refresh() {
        loading = true
        lastCursor = null
        if (isFiltered()) fetchFiltered(true) else fetchSimple(true)
        loading = false
    }

    suspend fun loadMore() {
        loading = true
        if (isFiltered()) fetchFiltered(false) else fetchSimple(false)
        loading = false
    }

    LaunchedEffect(Unit) {
        AdminApiClient.devices(serverUrl, adminKey).onSuccess { devices = it }
        refresh()
    }

    Column(modifier = Modifier.fillMaxSize()) {
        TopAppBar(
            title = { Text("ঘরের খবর") },
            colors = TopAppBarDefaults.topAppBarColors(
                containerColor = MaterialTheme.colorScheme.primary,
                titleContentColor = Color.White,
                actionIconContentColor = Color.White
            ),
            actions = {
                IconButton(onClick = {
                    onThemeModeChange(if (themeMode == "dark") "light" else "dark")
                }) {
                    Icon(
                        if (themeMode == "dark") Icons.Filled.LightMode else Icons.Filled.DarkMode,
                        contentDescription = "থিম"
                    )
                }
                IconButton(onClick = { scope.launch { refresh() } }) {
                    Icon(Icons.Filled.Refresh, contentDescription = "রিফ্রেশ")
                }
                IconButton(onClick = onOpenSettings) {
                    Icon(Icons.Filled.Settings, contentDescription = "সেটিংস")
                }
            }
        )

        Column(modifier = Modifier.padding(horizontal = 16.dp, vertical = 8.dp)) {
            OutlinedTextField(
                value = searchQuery,
                onValueChange = { searchQuery = it },
                label = { Text("খুঁজুন (নাম, লেখা)") },
                modifier = Modifier.fillMaxWidth()
            )
            Spacer(Modifier.height(8.dp))
            Row(modifier = Modifier.fillMaxWidth()) {
                Box(modifier = Modifier.weight(1f)) {
                    OutlinedButton(onClick = { personMenuOpen = true }, modifier = Modifier.fillMaxWidth()) {
                        Text(devices.find { it.deviceId == selectedDeviceId }?.person ?: "সব ব্যক্তি")
                    }
                    DropdownMenu(expanded = personMenuOpen, onDismissRequest = { personMenuOpen = false }) {
                        DropdownMenuItem(text = { Text("সব ব্যক্তি") }, onClick = {
                            selectedDeviceId = null
                            personMenuOpen = false
                        })
                        devices.forEach { d ->
                            DropdownMenuItem(text = { Text("${d.person} (${d.role})") }, onClick = {
                                selectedDeviceId = d.deviceId
                                personMenuOpen = false
                            })
                        }
                    }
                }
                Box(modifier = Modifier.weight(1f).padding(start = 8.dp)) {
                    OutlinedButton(onClick = { appMenuOpen = true }, modifier = Modifier.fillMaxWidth()) {
                        Text(appOptions.find { it.first == selectedAppPackage }?.second ?: "সব অ্যাপ")
                    }
                    DropdownMenu(expanded = appMenuOpen, onDismissRequest = { appMenuOpen = false }) {
                        appOptions.forEach { (pkg, label) ->
                            DropdownMenuItem(text = { Text(label) }, onClick = {
                                selectedAppPackage = pkg
                                appMenuOpen = false
                            })
                        }
                    }
                }
            }
            Spacer(Modifier.height(8.dp))
            Row(verticalAlignment = Alignment.CenterVertically) {
                OutlinedButton(onClick = { showDatePicker = true }) {
                    Text(
                        selectedDateMillis?.let { SimpleDateFormat("dd MMM yyyy", Locale("bn")).format(Date(it)) }
                            ?: "তারিখ বাছাই করুন"
                    )
                }
                if (selectedDateMillis != null) {
                    TextButton(onClick = { selectedDateMillis = null }) {
                        Text("বাদ দিন")
                    }
                }
            }
            Spacer(Modifier.height(8.dp))
            Button(onClick = { scope.launch { refresh() } }, modifier = Modifier.fillMaxWidth()) {
                Text("ফিল্টার প্রয়োগ করুন")
            }
        }

        if (errorText.isNotBlank()) {
            Text(errorText, color = MaterialTheme.colorScheme.error, modifier = Modifier.padding(horizontal = 16.dp))
        }

        if (loading && messages.isEmpty()) {
            Box(modifier = Modifier.fillMaxWidth().padding(32.dp), contentAlignment = Alignment.Center) {
                CircularProgressIndicator()
            }
        } else if (!loading && messages.isEmpty() && errorText.isBlank()) {
            Box(modifier = Modifier.fillMaxWidth().padding(32.dp), contentAlignment = Alignment.Center) {
                Text("কোনো মেসেজ পাওয়া যায়নি", style = MaterialTheme.typography.bodyMedium)
            }
        }

        LazyColumn(modifier = Modifier.weight(1f).padding(horizontal = 16.dp)) {
            items(messages) { msg ->
                MessageCard(msg, onClick = { onOpenMessage(msg) })
                Spacer(Modifier.height(8.dp))
            }
            item {
                if (hasMore) {
                    Button(
                        onClick = { scope.launch { loadMore() } },
                        modifier = Modifier.fillMaxWidth().padding(bottom = 24.dp),
                        enabled = !loading
                    ) {
                        Text(if (loading) "লোড হচ্ছে..." else "আরও লোড করুন")
                    }
                } else {
                    Spacer(Modifier.height(24.dp))
                }
            }
        }
    }

    if (showDatePicker) {
        val state = rememberDatePickerState()
        DatePickerDialog(
            onDismissRequest = { showDatePicker = false },
            confirmButton = {
                TextButton(onClick = {
                    selectedDateMillis = state.selectedDateMillis
                    showDatePicker = false
                }) { Text("ঠিক আছে") }
            },
            dismissButton = {
                TextButton(onClick = { showDatePicker = false }) { Text("বাতিল") }
            }
        ) {
            DatePicker(state = state)
        }
    }
}

@Composable
fun MessageCard(msg: Message, onClick: () -> Unit) {
    Card(
        onClick = onClick,
        modifier = Modifier.fillMaxWidth(),
        shape = RoundedCornerShape(12.dp)
    ) {
        Column(modifier = Modifier.padding(12.dp)) {
            Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween) {
                Text(msg.sender.ifBlank { msg.title }, style = MaterialTheme.typography.titleSmall)
                Text(msg.appName, style = MaterialTheme.typography.labelSmall)
            }
            Text(msg.person, style = MaterialTheme.typography.labelSmall)
            Spacer(Modifier.height(4.dp))
            Text(
                msg.text.ifBlank { "(ছবি বা অন্য কিছু)" },
                style = MaterialTheme.typography.bodyMedium,
                maxLines = 2
            )
            Spacer(Modifier.height(4.dp))
            Text(msg.postedTime, style = MaterialTheme.typography.labelSmall)
        }
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun DetailScreen(message: Message, onBack: () -> Unit) {
    val context = LocalContext.current
    var imageBytes by remember { mutableStateOf<ByteArray?>(null) }
    var imageError by remember { mutableStateOf(false) }

    LaunchedEffect(message.imageId) {
        if (message.imageId.isNotBlank()) {
            val serverUrl = Prefs.serverUrl(context)
            val adminKey = Prefs.adminKey(context)
            AdminApiClient.image(serverUrl, adminKey, message.imageId)
                .onSuccess { imageBytes = it }
                .onFailure { imageError = true }
        }
    }

    Column(modifier = Modifier.fillMaxSize()) {
        TopAppBar(
            title = { Text(message.sender.ifBlank { message.title }) },
            navigationIcon = {
                IconButton(onClick = onBack) {
                    Icon(Icons.Filled.ArrowBack, contentDescription = "ফিরে যান")
                }
            }
        )
        Column(
            modifier = Modifier
                .padding(16.dp)
                .verticalScroll(rememberScrollState())
        ) {
            Text("ব্যক্তি: ${message.person}", style = MaterialTheme.typography.bodyMedium)
            Text("অ্যাপ: ${message.appName}", style = MaterialTheme.typography.bodyMedium)
            if (message.conversation.isNotBlank()) {
                Text("কথোপকথন: ${message.conversation}", style = MaterialTheme.typography.bodyMedium)
            }
            Text("সময়: ${message.postedTime}", style = MaterialTheme.typography.bodyMedium)
            Spacer(Modifier.height(12.dp))
            Text(message.text, style = MaterialTheme.typography.bodyLarge)

            if (message.imageId.isNotBlank()) {
                Spacer(Modifier.height(16.dp))
                val bytes = imageBytes
                if (bytes != null) {
                    val bitmap = remember(bytes) { BitmapFactory.decodeByteArray(bytes, 0, bytes.size) }
                    if (bitmap != null) {
                        Image(
                            bitmap = bitmap.asImageBitmap(),
                            contentDescription = "ছবি",
                            modifier = Modifier.fillMaxWidth()
                        )
                    }
                } else if (imageError) {
                    Text("ছবি আনা যায়নি", color = MaterialTheme.colorScheme.error)
                } else {
                    Text("ছবি লোড হচ্ছে...")
                }
            }
        }
    }
}

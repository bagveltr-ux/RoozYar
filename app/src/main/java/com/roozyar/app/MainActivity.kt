
package com.roozyar.app

import android.app.AppOpsManager
import android.app.usage.UsageStatsManager
import android.content.Context
import android.content.Intent
import android.graphics.Color as AndroidColor
import android.os.Bundle
import android.provider.Settings
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.StrokeCap
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import java.text.SimpleDateFormat
import java.util.Calendar
import java.util.Date
import java.util.Locale

private val bg = Color(0xFF10131B)
private val panel = Color(0xFF1B202B)
private val muted = Color(0xFF929AAA)
private val accent = Color(0xFF7C9CFF)
private val orange = Color(0xFFFFA45B)
private val green = Color(0xFF54D6A0)

data class ActivityItem(
    val name: String,
    val minutes: Int,
    val color: Color
)

class MainActivity : ComponentActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContent {
            MaterialTheme(
                colorScheme = darkColorScheme(
                    background = bg,
                    surface = panel,
                    primary = accent
                )
            ) {
                RoozYarScreen(this)
            }
        }
    }
}

@Composable
fun RoozYarScreen(context: Context) {
    val prefs = remember {
        context.getSharedPreferences("roozyar", Context.MODE_PRIVATE)
    }

    var activities by remember {
        mutableStateOf(
            listOf(
                ActivityItem("خواب", 480, Color(0xFF6475A8)),
                ActivityItem("گوشی", 480, orange),
                ActivityItem("تلویزیون", 120, Color(0xFFB17CE8))
            )
        )
    }

    var newName by remember { mutableStateOf("") }
    var newHours by remember { mutableStateOf("1") }
    var phoneMinutes by remember { mutableIntStateOf(0) }
    var showAdd by remember { mutableStateOf(false) }
    var timerName by remember { mutableStateOf("") }
    var timerRunning by remember { mutableStateOf(false) }
    var timerStart by remember { mutableLongStateOf(0L) }
    var timerSeconds by remember { mutableIntStateOf(0) }

    fun save() {
        val data = activities.joinToString("|") {
            "${it.name},${it.minutes}"
        }
        prefs.edit().putString("activities", data).apply()
    }

    fun refreshUsage() {
        try {
            val appOps = context.getSystemService(Context.APP_OPS_SERVICE) as AppOpsManager
            val mode = appOps.checkOpNoThrow(
                "android:get_usage_stats",
                android.os.Process.myUid(),
                context.packageName
            )

            if (mode == AppOpsManager.MODE_ALLOWED) {
                val manager = context.getSystemService(Context.USAGE_STATS_SERVICE) as UsageStatsManager
                val calendar = Calendar.getInstance()
                calendar.set(Calendar.HOUR_OF_DAY, 0)
                calendar.set(Calendar.MINUTE, 0)
                calendar.set(Calendar.SECOND, 0)
                calendar.set(Calendar.MILLISECOND, 0)

                val stats = manager.queryUsageStats(
                    UsageStatsManager.INTERVAL_DAILY,
                    calendar.timeInMillis,
                    System.currentTimeMillis()
                )

                phoneMinutes = (stats.sumOf { it.totalTimeInForeground } / 60000).toInt()
            }
        } catch (_: Exception) {
            phoneMinutes = 0
        }
    }

    LaunchedEffect(Unit) {
        val saved = prefs.getString("activities", null)
        if (saved != null) {
            activities = saved.split("|").mapNotNull {
                val parts = it.split(",")
                if (parts.size == 2) {
                    val color = when (parts[0]) {
                        "خواب" -> Color(0xFF6475A8)
                        "گوشی" -> orange
                        "تلویزیون" -> Color(0xFFB17CE8)
                        else -> green
                    }
                    ActivityItem(parts[0], parts[1].toIntOrNull() ?: 0, color)
                } else null
            }
        }
        refreshUsage()
    }

    val totalMinutes = activities.sumOf { it.minutes }
    val remaining = (1440 - totalMinutes).coerceAtLeast(0)

    Column(
        modifier = Modifier
            .fillMaxSize()
            .background(bg)
            .padding(20.dp)
    ) {
        Text(
            "روز‌یار",
            color = Color.White,
            fontSize = 28.sp,
            fontWeight = FontWeight.Bold
        )
        Text(
            SimpleDateFormat("EEEE، d MMMM", Locale("fa")).format(Date()),
            color = muted,
            fontSize = 13.sp
        )

        Spacer(Modifier.height(22.dp))

        Box(
            modifier = Modifier
                .fillMaxWidth()
                .height(270.dp),
            contentAlignment = Alignment.Center
        ) {
            Canvas(Modifier.size(245.dp)) {
                val stroke = 24.dp.toPx()
                drawArc(
                    color = panel,
                    startAngle = -90f,
                    sweepAngle = 360f,
                    useCenter = false,
                    style = Stroke(stroke, cap = StrokeCap.Round)
                )

                var angle = -90f
                activities.forEach { item ->
                    val sweep = item.minutes.coerceAtMost(1440) / 1440f * 360f
                    if (sweep > 0) {
                        drawArc(
                            color = item.color,
                            startAngle = angle,
                            sweepAngle = sweep,
                            useCenter = false,
                            style = Stroke(stroke, cap = StrokeCap.Round)
                        )
                        angle += sweep
                    }
                }
            }

            Column(horizontalAlignment = Alignment.CenterHorizontally) {
                Text("از ۲۴ ساعت", color = muted, fontSize = 13.sp)
                Text(
                    "${remaining / 60} ساعت",
                    color = Color.White,
                    fontSize = 32.sp,
                    fontWeight = FontWeight.Bold
                )
                Text("زمان ثبت‌نشده", color = muted, fontSize = 12.sp)
            }
        }

        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.SpaceBetween
        ) {
            Text("فعالیت‌های امروز", color = Color.White, fontSize = 18.sp, fontWeight = FontWeight.Bold)
            TextButton(onClick = { showAdd = true }) {
                Text("+ افزودن")
            }
        }

        LazyColumn(
            modifier = Modifier.weight(1f),
            verticalArrangement = Arrangement.spacedBy(8.dp)
        ) {
            items(activities) { item ->
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .background(panel, RoundedCornerShape(14.dp))
                        .padding(14.dp),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Box(
                        Modifier
                            .size(10.dp)
                            .background(item.color, CircleShape)
                    )
                    Spacer(Modifier.width(12.dp))
                    Text(item.name, color = Color.White, modifier = Modifier.weight(1f))
                    Text(
                        "${item.minutes / 60}س ${item.minutes % 60}د",
                        color = muted
                    )
                    TextButton(onClick = {
                        activities = activities - item
                        save()
                    }) {
                        Text("حذف", color = Color(0xFFFF7777))
                    }
                }
            }
        }

        Spacer(Modifier.height(8.dp))

        Card(
            colors = CardDefaults.cardColors(containerColor = panel),
            shape = RoundedCornerShape(16.dp)
        ) {
            Column(Modifier.padding(16.dp)) {
                Text("استفاده از گوشی", color = Color.White, fontWeight = FontWeight.Bold)
                Spacer(Modifier.height(6.dp))
                Text(
                    "${phoneMinutes / 60} ساعت و ${phoneMinutes % 60} دقیقه",
                    color = orange,
                    fontSize = 22.sp
                )
                Text(
                    "برای مشاهده آمار دقیق، دسترسی Usage Access را فعال کن.",
                    color = muted,
                    fontSize = 11.sp
                )
                Row {
                    TextButton(onClick = {
                        context.startActivity(Intent(Settings.ACTION_USAGE_ACCESS_SETTINGS))
                    }) {
                        Text("فعال‌سازی دسترسی")
                    }
                    TextButton(onClick = { refreshUsage() }) {
                        Text("به‌روزرسانی")
                    }
                }
            }
        }

        Spacer(Modifier.height(8.dp))

        Button(
            onClick = { showAdd = true },
            modifier = Modifier.fillMaxWidth()
        ) {
            Text("ثبت فعالیت جدید")
        }
    }

    if (showAdd) {
        AlertDialog(
            onDismissRequest = { showAdd = false },
            title = { Text("افزودن فعالیت") },
            text = {
                Column {
                    OutlinedTextField(
                        value = newName,
                        onValueChange = { newName = it },
                        label = { Text("نام فعالیت") },
                        singleLine = true
                    )
                    Spacer(Modifier.height(8.dp))
                    OutlinedTextField(
                        value = newHours,
                        onValueChange = { newHours = it },
                        label = { Text("مدت زمان به ساعت") },
                        singleLine = true
                    )
                }
            },
            confirmButton = {
                TextButton(onClick = {
                    val hours = newHours.toIntOrNull() ?: 0
                    if (newName.isNotBlank() && hours > 0 && hours <= 24) {
                        activities = activities + ActivityItem(
                            newName,
                            hours * 60,
                            green
                        )
                        save()
                        newName = ""
                        newHours = "1"
                        showAdd = false
                    }
                }) {
                    Text("ثبت")
                }
            },
            dismissButton = {
                TextButton(onClick = { showAdd = false }) {
                    Text("لغو")
                }
            }
        )
    }
}

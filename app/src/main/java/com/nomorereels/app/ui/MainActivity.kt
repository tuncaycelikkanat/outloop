package com.nomorereels.app.ui

import android.content.Intent
import android.os.Bundle
import android.provider.Settings
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.compose.animation.AnimatedVisibility
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
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
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.nomorereels.app.detection.DetectionEventLog
import com.nomorereels.app.detection.DetectionLogStore
import com.nomorereels.app.detection.ScreenType
import com.nomorereels.app.service.ReelsAccessibilityService

class MainActivity : ComponentActivity() {

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContent {
            MaterialTheme {
                Surface(
                    modifier = Modifier.fillMaxSize(),
                    color = MaterialTheme.colorScheme.background
                ) {
                    DashboardScreen(
                        onOpenAccessibilitySettings = {
                            startActivity(Intent(Settings.ACTION_ACCESSIBILITY_SETTINGS))
                        }
                    )
                }
            }
        }
    }
}

@Composable
fun DashboardScreen(
    onOpenAccessibilitySettings: () -> Unit
) {
    val isServiceActive by ReelsAccessibilityService.isServiceActive.collectAsState()
    val blockCount by ReelsAccessibilityService.actionExecutor.blockCount.collectAsState()
    val lastScreen by ReelsAccessibilityService.lastDetectedType.collectAsState()
    val ruleVersion by ReelsAccessibilityService.ruleVersion.collectAsState()
    val eventLogs by DetectionLogStore.logs.collectAsState()

    LazyColumn(
        modifier = Modifier
            .fillMaxSize()
            .padding(horizontal = 20.dp, vertical = 16.dp),
        verticalArrangement = Arrangement.spacedBy(16.dp)
    ) {
        // Başlık
        item {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Column {
                    Text(
                        text = "No More Reels",
                        fontSize = 28.sp,
                        fontWeight = FontWeight.Bold,
                        color = MaterialTheme.colorScheme.primary
                    )
                    Text(
                        text = "Faz 1 · Akıllı Algılama Çekirdeği",
                        fontSize = 13.sp,
                        color = Color.Gray
                    )
                }
                Box(
                    modifier = Modifier
                        .background(Color(0xFFE0F2FE), RoundedCornerShape(8.dp))
                        .padding(horizontal = 10.dp, vertical = 4.dp)
                ) {
                    Text(
                        text = "Kural v$ruleVersion",
                        fontSize = 12.sp,
                        fontWeight = FontWeight.Bold,
                        color = Color(0xFF0369A1)
                    )
                }
            }
        }

        // Durum Kartı
        item {
            Card(
                modifier = Modifier.fillMaxWidth(),
                shape = RoundedCornerShape(16.dp),
                colors = CardDefaults.cardColors(
                    containerColor = if (isServiceActive) Color(0xFFE8F5E9) else Color(0xFFFFEBEE)
                )
            ) {
                Column(modifier = Modifier.padding(16.dp)) {
                    Row(
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.spacedBy(10.dp)
                    ) {
                        Box(
                            modifier = Modifier
                                .size(14.dp)
                                .background(
                                    color = if (isServiceActive) Color(0xFF2E7D32) else Color(0xFFC62828),
                                    shape = CircleShape
                                )
                        )
                        Text(
                            text = if (isServiceActive) "Hizmet Aktif & Dinliyor" else "Erişilebilirlik Hizmeti Kapalı",
                            fontSize = 16.sp,
                            fontWeight = FontWeight.SemiBold,
                            color = if (isServiceActive) Color(0xFF1B5E20) else Color(0xFFB71C1C)
                        )
                    }

                    Spacer(modifier = Modifier.height(10.dp))

                    if (!isServiceActive) {
                        Text(
                            text = "Algılama ve korumanın çalışması için telefonun Erişilebilirlik menüsünden 'No More Reels Hizmeti'ni açmanız gerekmektedir.",
                            fontSize = 13.sp,
                            color = Color(0xFF424242)
                        )
                        Spacer(modifier = Modifier.height(12.dp))
                        Button(
                            onClick = onOpenAccessibilitySettings,
                            colors = ButtonDefaults.buttonColors(containerColor = MaterialTheme.colorScheme.primary),
                            shape = RoundedCornerShape(10.dp)
                        ) {
                            Text("Erişilebilirlik Ayarlarını Aç")
                        }
                    } else {
                        Text(
                            text = "Gömülü rules.json devrede. Instagram olayları tıklandığında detayları açmak için karta dokunabilirsiniz.",
                            fontSize = 13.sp,
                            color = Color(0xFF2E7D32)
                        )
                    }
                }
            }
        }

        // Canlı Metrikler Kartı
        item {
            Card(
                modifier = Modifier.fillMaxWidth(),
                shape = RoundedCornerShape(16.dp)
            ) {
                Column(modifier = Modifier.padding(16.dp)) {
                    Text(
                        text = "Canlı Durum",
                        fontSize = 16.sp,
                        fontWeight = FontWeight.Bold
                    )
                    Spacer(modifier = Modifier.height(12.dp))
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween
                    ) {
                        Column {
                            Text(text = "Engellenen Reels", fontSize = 13.sp, color = Color.Gray)
                            Text(
                                text = "$blockCount",
                                fontSize = 24.sp,
                                fontWeight = FontWeight.Bold,
                                color = MaterialTheme.colorScheme.primary
                            )
                        }
                        Column(horizontalAlignment = Alignment.End) {
                            Text(text = "Son Ekran", fontSize = 13.sp, color = Color.Gray)
                            Text(
                                text = lastScreen.name,
                                fontSize = 15.sp,
                                fontWeight = FontWeight.SemiBold,
                                color = getScreenColor(lastScreen)
                            )
                        }
                    }
                }
            }
        }

        // Canlı Algılama Günlüğü (Debug)
        item {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Text(
                    text = "Algılama Günlüğü (${eventLogs.size}) - Dokun ve Aç",
                    fontSize = 15.sp,
                    fontWeight = FontWeight.Bold
                )
                if (eventLogs.isNotEmpty()) {
                    TextButton(onClick = { DetectionLogStore.clear() }) {
                        Text("Temizle", fontSize = 12.sp)
                    }
                }
            }
        }

        if (eventLogs.isEmpty()) {
            item {
                Card(
                    modifier = Modifier.fillMaxWidth(),
                    shape = RoundedCornerShape(12.dp),
                    colors = CardDefaults.cardColors(containerColor = Color(0xFFF5F5F5))
                ) {
                    Text(
                        text = "Henüz bir Instagram olayı yakalanmadı. Instagram'da gezinirken olaylar burada canlı olarak listelenecektir.",
                        fontSize = 12.sp,
                        color = Color.Gray,
                        modifier = Modifier.padding(14.dp)
                    )
                }
            }
        } else {
            items(eventLogs, key = { it.id }) { log ->
                LogItemView(log)
            }
        }
    }
}

@Composable
fun LogItemView(log: DetectionEventLog) {
    var isExpanded by remember { mutableStateOf(false) }

    Card(
        modifier = Modifier
            .fillMaxWidth()
            .clickable { isExpanded = !isExpanded },
        shape = RoundedCornerShape(10.dp),
        colors = CardDefaults.cardColors(
            containerColor = if (log.screenType.isReels) Color(0xFFFFF1F2) else Color(0xFFF8FAFC)
        )
    ) {
        Column(modifier = Modifier.padding(12.dp)) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    Text(
                        text = log.timeFormatted,
                        fontFamily = FontFamily.Monospace,
                        fontSize = 11.sp,
                        color = Color.Gray
                    )
                    Box(
                        modifier = Modifier
                            .background(
                                color = getScreenColor(log.screenType).copy(alpha = 0.15f),
                                shape = RoundedCornerShape(4.dp)
                            )
                            .padding(horizontal = 6.dp, vertical = 2.dp)
                    ) {
                        Text(
                            text = log.screenType.name,
                            fontSize = 11.sp,
                            fontWeight = FontWeight.Bold,
                            color = getScreenColor(log.screenType)
                        )
                    }
                }

                if (log.actionTaken != null) {
                    Text(
                        text = log.actionTaken,
                        fontSize = 11.sp,
                        fontWeight = FontWeight.SemiBold,
                        color = Color(0xFFDC2626)
                    )
                }
            }

            Spacer(modifier = Modifier.height(4.dp))

            Text(
                text = log.details,
                fontSize = 11.sp,
                color = Color.DarkGray,
                fontFamily = FontFamily.Monospace
            )

            // Tıklandığında genişleyen düğüm ayrıntıları
            AnimatedVisibility(visible = isExpanded) {
                Column(modifier = Modifier.padding(top = 8.dp)) {
                    Text(
                        text = "Ekrandaki Düğümler:",
                        fontSize = 11.sp,
                        fontWeight = FontWeight.Bold,
                        color = Color.Black
                    )
                    if (log.keyNodes.isEmpty()) {
                        Text(
                            text = "Özel düğüm yakalanmadı.",
                            fontSize = 10.sp,
                            color = Color.Gray
                        )
                    } else {
                        log.keyNodes.forEach { nodeInfo ->
                            Text(
                                text = "• $nodeInfo",
                                fontSize = 10.sp,
                                fontFamily = FontFamily.Monospace,
                                color = Color(0xFF334155),
                                modifier = Modifier.padding(vertical = 1.dp)
                            )
                        }
                    }
                }
            }
        }
    }
}

fun getScreenColor(screenType: ScreenType): Color {
    return when (screenType) {
        ScreenType.MAIN_FEED -> Color(0xFF16A34A)       // Yeşil
        ScreenType.REELS_TAB -> Color(0xFFDC2626)       // Kırmızı
        ScreenType.REELS_VIEWER -> Color(0xFFEA580C)    // Turuncu
        ScreenType.DIRECT_MESSAGES -> Color(0xFF2563EB) // Mavi
        ScreenType.EXPLORE -> Color(0xFF9333EA)         // Mor
        ScreenType.PROFILE -> Color(0xFF0891B2)         // Camgöbeği
        ScreenType.UNKNOWN -> Color.Gray
    }
}

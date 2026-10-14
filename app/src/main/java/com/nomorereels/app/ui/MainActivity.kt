package com.nomorereels.app.ui

import android.content.Intent
import android.os.Bundle
import android.provider.Settings
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.compose.foundation.background
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
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
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

    Column(
        modifier = Modifier
            .fillMaxSize()
            .padding(20.dp),
        verticalArrangement = Arrangement.spacedBy(16.dp)
    ) {
        // Başlık
        Text(
            text = "No More Reels",
            fontSize = 28.sp,
            fontWeight = FontWeight.Bold,
            color = MaterialTheme.colorScheme.primary
        )
        Text(
            text = "Faz 0 Prototip · Instagram Reels Engelleyici",
            fontSize = 14.sp,
            color = Color.Gray
        )

        Spacer(modifier = Modifier.height(8.dp))

        // Durum Kartı
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

                Spacer(modifier = Modifier.height(12.dp))

                if (!isServiceActive) {
                    Text(
                        text = "Reels algılaması yapabilmek için telefonun Erişilebilirlik menüsünden 'No More Reels Hizmeti'ni açmanız gerekmektedir.",
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
                        text = "Sadece Instagram olayları izleniyor. Reels açıldığında otomatik olarak geri dönülecektir.",
                        fontSize = 13.sp,
                        color = Color(0xFF2E7D32)
                    )
                }
            }
        }

        // Canlı Metrikler Kartı
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
                        Text(text = "Son Algılanan Ekran", fontSize = 13.sp, color = Color.Gray)
                        Text(
                            text = lastScreen.name,
                            fontSize = 16.sp,
                            fontWeight = FontWeight.SemiBold
                        )
                    }
                }
            }
        }

        // Faz 0 Test Rehberi
        Card(
            modifier = Modifier.fillMaxWidth(),
            shape = RoundedCornerShape(16.dp)
        ) {
            Column(modifier = Modifier.padding(16.dp)) {
                Text(
                    text = "Nasıl Test Edilir?",
                    fontSize = 16.sp,
                    fontWeight = FontWeight.Bold
                )
                Spacer(modifier = Modifier.height(8.dp))
                Text(
                    text = "1. Yukarıdaki butona basarak erişilebilirlik iznini verin.\n" +
                           "2. Instagram uygulamasını açın.\n" +
                           "3. Alt menüden 'Reels' sekmesine dokunun.\n" +
                           "4. Servis Reels sekmesini görür görmez 'Geri (Back)' tuşunu tetikleyecektir.",
                    fontSize = 13.sp,
                    lineHeight = 20.sp
                )
                Spacer(modifier = Modifier.height(8.dp))
                Text(
                    text = "Logcat komutu:\nadb logcat -s NoMoreReels",
                    fontFamily = FontFamily.Monospace,
                    fontSize = 12.sp,
                    color = Color.DarkGray
                )
            }
        }
    }
}

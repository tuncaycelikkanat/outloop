package com.nomorereels.app.ui.dialogs

import androidx.compose.animation.core.EaseInOut
import androidx.compose.animation.core.RepeatMode
import androidx.compose.animation.core.animateFloat
import androidx.compose.animation.core.infiniteRepeatable
import androidx.compose.animation.core.rememberInfiniteTransition
import androidx.compose.animation.core.tween
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.scale
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextDecoration
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.ui.window.Dialog
import com.nomorereels.app.data.MindfulBarrierType
import com.nomorereels.app.ui.i18n.LocalAppStrings
import com.nomorereels.app.ui.i18n.TurkishStrings
import com.nomorereels.app.ui.theme.AmoledAccent
import com.nomorereels.app.ui.theme.AmoledBorder
import com.nomorereels.app.ui.theme.AmoledCanvas
import com.nomorereels.app.ui.theme.AmoledPrimary
import com.nomorereels.app.ui.theme.AmoledSurface
import com.nomorereels.app.ui.theme.AmoledTextMuted
import com.nomorereels.app.ui.theme.AmoledTextPrimary
import com.nomorereels.app.ui.theme.LightAccent
import com.nomorereels.app.ui.theme.LightBorder
import com.nomorereels.app.ui.theme.LightPrimary
import com.nomorereels.app.ui.theme.LightSurface
import com.nomorereels.app.ui.theme.LightTextMuted
import com.nomorereels.app.ui.theme.LightTextPrimary

@Composable
fun MindfulGatewayDialog(
    barrierType: MindfulBarrierType = MindfulBarrierType.BREATH_AND_INTENT,
    isNightMode: Boolean,
    onDismiss: () -> Unit,
    onApproved: (durationMinutes: Int, microPlan: String) -> Unit
) {
    val strings = LocalAppStrings.current
    val isEnglish = strings !is TurkishStrings

    val isDark = isNightMode || (MaterialTheme.colorScheme.background == AmoledCanvas)

    val cardBg = if (isDark) AmoledSurface else LightSurface
    val cardBorder = if (isDark) AmoledBorder else LightBorder
    val titleColor = if (isDark) AmoledTextPrimary else LightTextPrimary
    val textColor = if (isDark) AmoledTextMuted else LightTextMuted
    val badgeBg = if (isDark) AmoledAccent else LightAccent
    val badgeText = if (isDark) AmoledPrimary else LightPrimary
    val primaryBtnBg = if (isDark) AmoledPrimary else LightPrimary
    val primaryBtnText = if (isDark) AmoledSurface else Color.White
    val secondaryBtnBg = if (isDark) AmoledAccent else LightAccent

    var remainingSeconds by remember { mutableIntStateOf(10) }

    LaunchedEffect(Unit) {
        while (remainingSeconds > 0) {
            kotlinx.coroutines.delay(1000L)
            remainingSeconds--
        }
    }

    val editorialTitleStyle = MaterialTheme.typography.headlineMedium.copy(
        fontWeight = FontWeight.Bold,
        fontSize = 22.sp,
        lineHeight = 28.sp,
        letterSpacing = (-0.3).sp
    )

    Dialog(onDismissRequest = onDismiss) {
        Surface(
            shape = RoundedCornerShape(24.dp),
            color = cardBg,
            border = BorderStroke(1.dp, cardBorder),
            shadowElevation = 8.dp,
            modifier = Modifier.fillMaxWidth().padding(16.dp)
        ) {
            Column(
                modifier = Modifier
                    .padding(24.dp)
                    .verticalScroll(rememberScrollState()),
                horizontalAlignment = Alignment.CenterHorizontally
            ) {
                // Rozet
                Surface(
                    shape = RoundedCornerShape(16.dp),
                    color = badgeBg
                ) {
                    Text(
                        text = if (isNightMode) {
                            if (isEnglish) "🌙 NIGHT REST MODE" else "🌙 GECE DİNLENME MODU"
                        } else {
                            if (isEnglish) "🌿 MINDFUL PAUSE" else "🌿 FARKINDALIK KAPISI"
                        },
                        style = MaterialTheme.typography.labelSmall.copy(fontWeight = FontWeight.SemiBold),
                        color = badgeText,
                        modifier = Modifier.padding(horizontal = 12.dp, vertical = 6.dp)
                    )
                }

                Spacer(modifier = Modifier.height(16.dp))

                // Başlık
                Text(
                    text = if (isNightMode) {
                        if (isEnglish) "Time to Rest" else "Gece Dinlenme Zamanı"
                    } else {
                        if (isEnglish) "Take a Deep Breath" else "Derin Bir Nefes Al"
                    },
                    style = editorialTitleStyle,
                    color = titleColor,
                    textAlign = TextAlign.Center
                )
                Spacer(modifier = Modifier.height(8.dp))

                // Açıklama
                Text(
                    text = if (isNightMode) {
                        if (isEnglish)
                            "Late-night screens disrupt deep sleep. Give your mind permission to recharge."
                        else
                            "Gece ekran ışığı derin uyku ritmini bozar. Zihnine dinlenmesi için alan açmak ister misin?"
                    } else {
                        if (isEnglish)
                            "Notice the automatic scrolling reflex. Give yourself a moment to choose consciously."
                        else
                            "Otomatik kaydırma refleksini fark et. Bilinçli bir seçim yapmak için kendine bir an tanı."
                    },
                    style = MaterialTheme.typography.bodyMedium.copy(lineHeight = 20.sp),
                    color = textColor,
                    textAlign = TextAlign.Center
                )
                Spacer(modifier = Modifier.height(24.dp))

                // Nefes Çemberi Animasyonu
                val transition = rememberInfiniteTransition(label = "breath")
                val scale by transition.animateFloat(
                    initialValue = 0.75f,
                    targetValue = 1.15f,
                    animationSpec = infiniteRepeatable(
                        animation = tween(2200, easing = EaseInOut),
                        repeatMode = RepeatMode.Reverse
                    ),
                    label = "scale"
                )

                Box(
                    modifier = Modifier
                        .size(80.dp)
                        .scale(scale)
                        .background(secondaryBtnBg, CircleShape)
                        .border(3.dp, primaryBtnBg, CircleShape),
                    contentAlignment = Alignment.Center
                ) {
                    Box(
                        modifier = Modifier
                            .size(40.dp)
                            .background(primaryBtnBg, CircleShape)
                    )
                }

                Spacer(modifier = Modifier.height(28.dp))

                // Birincil Buton: "Vazgeç ve Geri Dön"
                Button(
                    onClick = onDismiss,
                    modifier = Modifier.fillMaxWidth().height(52.dp),
                    colors = ButtonDefaults.buttonColors(containerColor = primaryBtnBg),
                    shape = RoundedCornerShape(22.dp)
                ) {
                    Text(
                        text = if (isEnglish) "Put Phone Down & Return" else "Vazgeç ve Geri Dön",
                        color = primaryBtnText,
                        fontWeight = FontWeight.SemiBold
                    )
                }

                Spacer(modifier = Modifier.height(12.dp))

                // İkincil Eylem: "5 dakika izin ver"
                if (remainingSeconds > 0) {
                    Text(
                        text = if (isEnglish)
                            "Allow 5 minutes (${remainingSeconds}s)"
                        else
                            "5 dakika izin ver (${remainingSeconds}s)",
                        style = MaterialTheme.typography.bodyMedium,
                        color = textColor.copy(alpha = 0.6f),
                        textAlign = TextAlign.Center,
                        modifier = Modifier.padding(vertical = 8.dp)
                    )
                } else {
                    Text(
                        text = if (isEnglish) "Allow 5 minutes" else "5 dakika izin ver",
                        style = MaterialTheme.typography.bodyMedium.copy(
                            fontWeight = FontWeight.Medium,
                            textDecoration = TextDecoration.Underline
                        ),
                        color = primaryBtnBg,
                        textAlign = TextAlign.Center,
                        modifier = Modifier
                            .clip(RoundedCornerShape(8.dp))
                            .clickable {
                                onApproved(5, if (isEnglish) "Mindful Pause" else "Bilinçli Mola")
                            }
                            .padding(horizontal = 12.dp, vertical = 8.dp)
                    )
                }
            }
        }
    }
}

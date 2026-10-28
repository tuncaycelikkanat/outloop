package com.nomorereels.app.ui.dialogs

import android.animation.ValueAnimator
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
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
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
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.scale
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.semantics.LiveRegionMode
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.ui.semantics.liveRegion
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextDecoration
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.ui.window.Dialog
import com.nomorereels.app.data.SettingsStore
import com.nomorereels.app.policy.GatewaySpec
import com.nomorereels.app.policy.PolicyDecision
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
    onDismiss: () -> Unit,
    onApproved: (durationMinutes: Int, microPlan: String) -> Unit,
    decision: PolicyDecision? = null,
    originLabel: String? = null
) {
    val strings = LocalAppStrings.current
    val isEnglish = strings !is TurkishStrings

    val isDark = MaterialTheme.colorScheme.background == AmoledCanvas

    val cardBg = if (isDark) AmoledSurface else LightSurface
    val cardBorder = if (isDark) AmoledBorder else LightBorder
    val titleColor = if (isDark) AmoledTextPrimary else LightTextPrimary
    val textColor = if (isDark) AmoledTextMuted else LightTextMuted
    val badgeBg = if (isDark) AmoledAccent else LightAccent
    val badgeText = if (isDark) AmoledPrimary else LightPrimary
    val primaryBtnBg = if (isDark) AmoledPrimary else LightPrimary
    val primaryBtnText = if (isDark) AmoledSurface else Color.White
    val secondaryBtnBg = if (isDark) AmoledAccent else LightAccent

    val snoozeCountToday by SettingsStore.snoozeCountToday.collectAsState()
    val isBedtime = SettingsStore.isBedtimeActiveNow()
    val canApprove = GatewaySpec.canApproveSnooze(isBedtime, snoozeCountToday)
    val remainingSnoozes = GatewaySpec.remainingDailySnoozes(snoozeCountToday)
    val initialWaitSeconds = remember(snoozeCountToday) {
        GatewaySpec.computeInitialWaitSeconds(snoozeCountToday)
    }

    var remainingSeconds by remember(initialWaitSeconds) { mutableIntStateOf(initialWaitSeconds) }
    var selectedDuration by remember { mutableIntStateOf(GatewaySpec.DEFAULT_DURATION_MINUTES) }
    var isInhale by remember { mutableStateOf(true) }

    LaunchedEffect(canApprove, initialWaitSeconds) {
        if (!canApprove) return@LaunchedEffect
        while (remainingSeconds > 0) {
            kotlinx.coroutines.delay(1000L)
            remainingSeconds--
        }
    }

    LaunchedEffect(Unit) {
        while (true) {
            kotlinx.coroutines.delay(2200L)
            isInhale = !isInhale
        }
    }

    val editorialTitleStyle = MaterialTheme.typography.headlineMedium.copy(
        fontWeight = FontWeight.Bold,
        fontSize = 22.sp,
        lineHeight = 28.sp,
        letterSpacing = (-0.3).sp
    )

    val animatorsEnabled = remember {
        try {
            ValueAnimator.areAnimatorsEnabled()
        } catch (_: Throwable) {
            true
        }
    }

    Dialog(onDismissRequest = onDismiss) {
        Surface(
            shape = RoundedCornerShape(24.dp),
            color = cardBg,
            border = BorderStroke(1.dp, cardBorder),
            shadowElevation = 8.dp,
            modifier = Modifier
                .fillMaxWidth()
                .padding(16.dp)
                .semantics {
                    liveRegion = LiveRegionMode.Polite
                    contentDescription = if (isEnglish) "Mindful Gateway Pause" else "Farkındalık Kapısı Duraklaması"
                }
        ) {
            Column(
                modifier = Modifier
                    .padding(24.dp)
                    .verticalScroll(rememberScrollState()),
                horizontalAlignment = Alignment.CenterHorizontally
            ) {
                // 1. Bağlamsal Rozet (4.3-4)
                Surface(
                    shape = RoundedCornerShape(16.dp),
                    color = badgeBg
                ) {
                    Text(
                        text = GatewaySpec.contextualBadgeText(decision, originLabel, isEnglish),
                        style = MaterialTheme.typography.labelSmall.copy(fontWeight = FontWeight.SemiBold),
                        color = badgeText,
                        modifier = Modifier.padding(horizontal = 12.dp, vertical = 6.dp)
                    )
                }

                Spacer(modifier = Modifier.height(14.dp))

                // 2. Başlık
                Text(
                    text = if (isEnglish) "Take a Deep Breath" else "Derin Bir Nefes Al",
                    style = editorialTitleStyle,
                    color = titleColor,
                    textAlign = TextAlign.Center
                )
                Spacer(modifier = Modifier.height(6.dp))

                // 3. Açıklama
                Text(
                    text = if (isEnglish)
                        "Notice the automatic scrolling reflex. Give yourself a moment to choose consciously."
                    else
                        "Otomatik kaydırma refleksini fark et. Bilinçli bir seçim yapmak için kendine bir an tanı.",
                    style = MaterialTheme.typography.bodyMedium.copy(lineHeight = 20.sp),
                    color = textColor,
                    textAlign = TextAlign.Center
                )
                Spacer(modifier = Modifier.height(18.dp))

                // 4. Nefes Çemberi Animasyonu + Nefes Ritmi Metni (4.3-5)
                val transition = rememberInfiniteTransition(label = "breath")
                val animatedScale by transition.animateFloat(
                    initialValue = 0.75f,
                    targetValue = 1.15f,
                    animationSpec = infiniteRepeatable(
                        animation = tween(2200, easing = EaseInOut),
                        repeatMode = RepeatMode.Reverse
                    ),
                    label = "scale"
                )
                val scale = if (animatorsEnabled) animatedScale else 1f

                Box(
                    modifier = Modifier
                        .size(76.dp)
                        .scale(scale)
                        .background(secondaryBtnBg, CircleShape)
                        .border(3.dp, primaryBtnBg, CircleShape),
                    contentAlignment = Alignment.Center
                ) {
                    Box(
                        modifier = Modifier
                            .size(38.dp)
                            .background(primaryBtnBg, CircleShape)
                    )
                }
                Spacer(modifier = Modifier.height(8.dp))
                Text(
                    text = GatewaySpec.breathingPhaseText(isInhale, isEnglish),
                    style = MaterialTheme.typography.labelMedium,
                    color = badgeText
                )

                Spacer(modifier = Modifier.height(22.dp))

                // 5. Birincil Buton: "Vazgeç ve Geri Dön"
                Button(
                    onClick = onDismiss,
                    modifier = Modifier.fillMaxWidth().height(50.dp),
                    colors = ButtonDefaults.buttonColors(containerColor = primaryBtnBg),
                    shape = RoundedCornerShape(22.dp)
                ) {
                    Text(
                        text = if (isEnglish) "Put Phone Down & Return" else "Vazgeç ve Geri Dön",
                        color = primaryBtnText,
                        fontWeight = FontWeight.SemiBold
                    )
                }

                Spacer(modifier = Modifier.height(14.dp))

                // 6. İkincil Eylem: Bedtime / Günlük Sınır Kontrolü + Süre Seçimi (P0-5, 4.3-1, 4.3-2)
                val lockReason = GatewaySpec.lockReasonText(isBedtime, snoozeCountToday, isEnglish)
                if (lockReason != null) {
                    Text(
                        text = lockReason,
                        style = MaterialTheme.typography.bodySmall,
                        color = textColor,
                        textAlign = TextAlign.Center,
                        modifier = Modifier.padding(vertical = 6.dp)
                    )
                } else {
                    // Süre çipleri (3 / 5 / 10 dk)
                    Row(
                        horizontalArrangement = Arrangement.spacedBy(8.dp),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        GatewaySpec.DURATION_OPTIONS.forEach { mins ->
                            val isSelected = selectedDuration == mins
                            Surface(
                                shape = RoundedCornerShape(12.dp),
                                color = if (isSelected) badgeBg else Color.Transparent,
                                border = BorderStroke(1.dp, if (isSelected) primaryBtnBg else cardBorder),
                                modifier = Modifier
                                    .clip(RoundedCornerShape(12.dp))
                                    .clickable { selectedDuration = mins }
                            ) {
                                Text(
                                    text = if (isEnglish) "$mins min" else "$mins dk",
                                    style = MaterialTheme.typography.labelSmall.copy(
                                        fontWeight = if (isSelected) FontWeight.Bold else FontWeight.Normal
                                    ),
                                    color = if (isSelected) badgeText else textColor,
                                    modifier = Modifier.padding(horizontal = 10.dp, vertical = 5.dp)
                                )
                            }
                        }
                    }

                    Spacer(modifier = Modifier.height(8.dp))

                    val allowLabel = GatewaySpec.allowActionText(
                        durationMinutes = selectedDuration,
                        remainingSeconds = remainingSeconds,
                        remainingSnoozes = remainingSnoozes,
                        isEnglish = isEnglish
                    )
                    if (remainingSeconds > 0) {
                        Text(
                            text = allowLabel,
                            style = MaterialTheme.typography.bodySmall,
                            color = textColor.copy(alpha = 0.6f),
                            textAlign = TextAlign.Center,
                            modifier = Modifier.padding(vertical = 6.dp)
                        )
                    } else {
                        Text(
                            text = allowLabel,
                            style = MaterialTheme.typography.bodyMedium.copy(
                                fontWeight = FontWeight.Medium,
                                textDecoration = TextDecoration.Underline
                            ),
                            color = primaryBtnBg,
                            textAlign = TextAlign.Center,
                            modifier = Modifier
                                .clip(RoundedCornerShape(8.dp))
                                .clickable {
                                    onApproved(selectedDuration, "")
                                }
                                .padding(horizontal = 12.dp, vertical = 8.dp)
                        )
                    }
                }
            }
        }
    }
}

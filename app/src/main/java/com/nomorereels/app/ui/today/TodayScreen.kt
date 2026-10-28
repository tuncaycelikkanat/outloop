package com.nomorereels.app.ui.today

import android.content.Intent
import android.provider.Settings
import androidx.compose.foundation.BorderStroke
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
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.outlined.ArrowBack
import androidx.compose.material.icons.outlined.ChevronRight
import androidx.compose.material.icons.outlined.Info
import androidx.compose.material.icons.outlined.Pause
import androidx.compose.material.icons.outlined.PlayArrow
import androidx.compose.material.icons.outlined.Spa
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Switch
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontStyle
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.nomorereels.app.policy.DmReelMode
import com.nomorereels.app.ui.MindfulQuotes
import com.nomorereels.app.ui.PracticalSuggestions
import com.nomorereels.app.ui.i18n.LocalAppStrings
import com.nomorereels.app.ui.theme.NmrTheme
import com.nomorereels.app.ui.theme.StatusGreen
import com.nomorereels.app.ui.theme.StatusOrange
import com.nomorereels.app.ui.theme.StatusRed

@Composable
fun TodayScreen(
    viewModel: TodayViewModel,
    onNavigateToProtection: () -> Unit,
    onStartSnoozeClick: () -> Unit,
    modifier: Modifier = Modifier,
    isServiceActiveOverride: Boolean? = null
) {
    val uiState by viewModel.uiState.collectAsStateWithLifecycle()
    val isEffectiveActive = isServiceActiveOverride ?: uiState.isServiceActive
    val context = LocalContext.current

    TodayScreenContent(
        uiState = uiState.copy(isServiceActive = isEffectiveActive),
        onProtectionMasterToggle = { viewModel.toggleProtectionMaster(it) },
        onOpenAccessibilitySettings = {
            context.startActivity(Intent(Settings.ACTION_ACCESSIBILITY_SETTINGS))
        },
        onNavigateToProtection = onNavigateToProtection,
        onStartSnoozeClick = onStartSnoozeClick,
        onEndSnoozeClick = { viewModel.stopSnooze() },
        modifier = modifier
    )
}

@Composable
fun TodayScreenContent(
    uiState: TodayUiState,
    onProtectionMasterToggle: (Boolean) -> Unit,
    onOpenAccessibilitySettings: () -> Unit,
    onNavigateToProtection: () -> Unit,
    onStartSnoozeClick: () -> Unit,
    onEndSnoozeClick: () -> Unit,
    modifier: Modifier = Modifier
) {
    val strings = LocalAppStrings.current
    val savedSeconds = uiState.savedSeconds
    val savedMinutes = uiState.savedMinutes
    val blockCount = uiState.blockCount
    val isServiceActive = uiState.isServiceActive
    val protectionMasterEnabled = uiState.protectionMasterEnabled
    val isSnoozeActive = uiState.isSnoozeActive
    val generalMode = uiState.generalMode
    val policySummary = uiState.policySummary
    val snoozeRemainingMinutes = uiState.snoozeRemainingMinutes

    LazyColumn(
        modifier = modifier
            .fillMaxSize()
            .padding(horizontal = NmrTheme.dimens.screenHorizontalMargin),
        verticalArrangement = Arrangement.spacedBy(14.dp)
    ) {
        // Üst Alan: Tarih + Selamlama + Switch
        item {
            Spacer(modifier = Modifier.height(10.dp))
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Column(modifier = Modifier.weight(1f)) {
                    Text(
                        text = uiState.todayDateFormatted,
                        style = MaterialTheme.typography.labelSmall.copy(
                            letterSpacing = 1.4.sp,
                            fontWeight = FontWeight.SemiBold
                        ),
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                    Spacer(modifier = Modifier.height(3.dp))
                    Text(
                        text = strings.dashboardGreeting,
                        style = MaterialTheme.typography.headlineMedium.copy(
                            fontWeight = FontWeight.Bold,
                            fontSize = 25.sp,
                            lineHeight = 31.sp,
                            letterSpacing = (-0.4).sp
                        ),
                        color = MaterialTheme.colorScheme.onBackground
                    )
                }

                Switch(
                    checked = isServiceActive && protectionMasterEnabled,
                    onCheckedChange = { checked ->
                        if (!isServiceActive) {
                            onOpenAccessibilitySettings()
                        } else {
                            onProtectionMasterToggle(checked)
                        }
                    }
                )
            }
        }

        // Servis İzni Kapalıysa Dikkat Çekici Uyarı Kartı
        if (!isServiceActive) {
            item {
                Surface(
                    shape = RoundedCornerShape(16.dp),
                    color = StatusRed.copy(alpha = 0.12f),
                    border = BorderStroke(1.dp, StatusRed.copy(alpha = 0.35f)),
                    modifier = Modifier.fillMaxWidth().clickable { onOpenAccessibilitySettings() }
                ) {
                    Row(
                        modifier = Modifier.padding(16.dp),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Surface(
                            shape = CircleShape,
                            color = StatusRed.copy(alpha = 0.18f),
                            modifier = Modifier.size(38.dp)
                        ) {
                            Box(contentAlignment = Alignment.Center) {
                                Icon(
                                    imageVector = Icons.Outlined.Info,
                                    contentDescription = null,
                                    tint = StatusRed,
                                    modifier = Modifier.size(20.dp)
                                )
                            }
                        }
                        Spacer(modifier = Modifier.width(14.dp))
                        Column(modifier = Modifier.weight(1f)) {
                            Text(
                                text = strings.serviceInactiveTitle,
                                style = MaterialTheme.typography.titleSmall.copy(fontWeight = FontWeight.Bold),
                                color = MaterialTheme.colorScheme.onBackground
                            )
                            Spacer(modifier = Modifier.height(2.dp))
                            Text(
                                text = strings.serviceInactiveDesc,
                                style = MaterialTheme.typography.bodySmall,
                                color = MaterialTheme.colorScheme.onSurfaceVariant
                            )
                        }
                        Icon(
                            imageVector = Icons.Outlined.ChevronRight,
                            contentDescription = null,
                            tint = StatusRed,
                            modifier = Modifier.size(20.dp)
                        )
                    }
                }
            }
        }

        // 1. KAHRAMAN ALANI (HERO): KURTARILAN ZAMAN VE DÖNGÜ SAYACI
        item {
            Surface(
                shape = RoundedCornerShape(18.dp),
                color = MaterialTheme.colorScheme.surface,
                border = BorderStroke(1.dp, MaterialTheme.colorScheme.outline),
                modifier = Modifier.fillMaxWidth()
            ) {
                Column(modifier = Modifier.padding(18.dp)) {
                    Text(
                        text = strings.savedFocusTitle,
                        style = MaterialTheme.typography.labelSmall.copy(
                            letterSpacing = 1.3.sp,
                            fontWeight = FontWeight.Bold
                        ),
                        color = MaterialTheme.colorScheme.primary
                    )
                    Spacer(modifier = Modifier.height(8.dp))

                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        verticalAlignment = Alignment.Bottom,
                        horizontalArrangement = Arrangement.SpaceBetween
                    ) {
                        Column {
                            Row(verticalAlignment = Alignment.Bottom) {
                                if (savedMinutes > 0) {
                                    Text(
                                        text = "$savedMinutes",
                                        style = MaterialTheme.typography.headlineLarge.copy(
                                            fontSize = 44.sp,
                                            fontWeight = FontWeight.Black,
                                            letterSpacing = (-1.5).sp
                                        ),
                                        color = MaterialTheme.colorScheme.onBackground
                                    )
                                    Spacer(modifier = Modifier.width(4.dp))
                                    Text(
                                        text = if (strings is com.nomorereels.app.ui.i18n.EnglishStrings) "min focus" else "dk odak",
                                        style = MaterialTheme.typography.titleMedium.copy(fontWeight = FontWeight.SemiBold),
                                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                                        modifier = Modifier.padding(bottom = 8.dp)
                                    )
                                } else {
                                    Text(
                                        text = "$savedSeconds",
                                        style = MaterialTheme.typography.headlineLarge.copy(
                                            fontSize = 44.sp,
                                            fontWeight = FontWeight.Black,
                                            letterSpacing = (-1.5).sp
                                        ),
                                        color = MaterialTheme.colorScheme.onBackground
                                    )
                                    Spacer(modifier = Modifier.width(4.dp))
                                    Text(
                                        text = if (strings is com.nomorereels.app.ui.i18n.EnglishStrings) "sec focus" else "sn odak",
                                        style = MaterialTheme.typography.titleMedium.copy(fontWeight = FontWeight.SemiBold),
                                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                                        modifier = Modifier.padding(bottom = 8.dp)
                                    )
                                }
                            }
                        }

                        // Döngü Sayacı Rozeti (Pill)
                        Surface(
                            shape = RoundedCornerShape(12.dp),
                            color = MaterialTheme.colorScheme.primary.copy(alpha = 0.12f),
                            border = BorderStroke(1.dp, MaterialTheme.colorScheme.primary.copy(alpha = 0.25f))
                        ) {
                            Row(
                                modifier = Modifier.padding(horizontal = 10.dp, vertical = 6.dp),
                                verticalAlignment = Alignment.CenterVertically
                            ) {
                                Icon(
                                    imageVector = Icons.Outlined.Spa,
                                    contentDescription = null,
                                    tint = MaterialTheme.colorScheme.primary,
                                    modifier = Modifier.size(15.dp)
                                )
                                Spacer(modifier = Modifier.width(5.dp))
                                Text(
                                    text = "$blockCount ${strings.loopsBrokenTitle.lowercase()}",
                                    style = MaterialTheme.typography.labelSmall.copy(fontWeight = FontWeight.Bold),
                                    color = MaterialTheme.colorScheme.primary
                                )
                            }
                        }
                    }

                    Spacer(modifier = Modifier.height(12.dp))

                    Text(
                        text = strings.savedFocusFootnote,
                        style = MaterialTheme.typography.bodySmall.copy(fontSize = 11.5.sp),
                        color = MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.85f)
                    )
                }
            }
        }

        // 2. KORUMA DURUMU VE REHBERLİK KARTI
        item {
            Surface(
                shape = RoundedCornerShape(18.dp),
                color = MaterialTheme.colorScheme.surface,
                border = BorderStroke(1.dp, MaterialTheme.colorScheme.outline),
                modifier = Modifier.fillMaxWidth()
            ) {
                Column(modifier = Modifier.padding(18.dp)) {
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Text(
                            text = if (strings is com.nomorereels.app.ui.i18n.EnglishStrings) "PROTECTION SUMMARY" else "KORUMA ÖZETİ",
                            style = MaterialTheme.typography.labelSmall.copy(
                                letterSpacing = 1.2.sp,
                                fontWeight = FontWeight.Bold
                            ),
                            color = MaterialTheme.colorScheme.onSurfaceVariant
                        )

                        Text(
                            text = if (strings is com.nomorereels.app.ui.i18n.EnglishStrings) "Edit →" else "Düzenle →",
                            style = MaterialTheme.typography.labelSmall.copy(fontWeight = FontWeight.SemiBold),
                            color = MaterialTheme.colorScheme.primary,
                            modifier = Modifier.clickable { onNavigateToProtection() }
                        )
                    }

                    Spacer(modifier = Modifier.height(10.dp))

                    Text(
                        text = policySummary,
                        style = MaterialTheme.typography.titleMedium.copy(
                            fontWeight = FontWeight.SemiBold,
                            lineHeight = 22.sp
                        ),
                        color = MaterialTheme.colorScheme.onBackground
                    )

                    Spacer(modifier = Modifier.height(6.dp))

                    Row(verticalAlignment = Alignment.CenterVertically) {
                        val statusColor = if (isServiceActive && protectionMasterEnabled) StatusGreen else StatusOrange
                        Box(
                            modifier = Modifier
                                .size(7.dp)
                                .clip(CircleShape)
                                .background(statusColor)
                        )
                        Spacer(modifier = Modifier.width(6.dp))
                        Text(
                            text = if (isServiceActive && protectionMasterEnabled)
                                strings.statusActivePill
                            else
                                strings.statusInactivePill,
                            style = MaterialTheme.typography.bodySmall.copy(fontSize = 11.5.sp),
                            color = statusColor
                        )
                    }
                }
            }
        }

        // 3. FARKINDALIK VE MOLA ALANI (SNOOZE KARTI)
        item {
            Surface(
                shape = RoundedCornerShape(18.dp),
                color = if (isSnoozeActive) StatusOrange.copy(alpha = 0.08f) else MaterialTheme.colorScheme.surface,
                border = BorderStroke(
                    1.dp,
                    if (isSnoozeActive) StatusOrange.copy(alpha = 0.35f) else MaterialTheme.colorScheme.outline
                ),
                modifier = Modifier.fillMaxWidth()
            ) {
                Row(
                    modifier = Modifier.padding(16.dp),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Row(
                        modifier = Modifier.weight(1f),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Surface(
                            shape = CircleShape,
                            color = if (isSnoozeActive) StatusOrange.copy(alpha = 0.16f) else MaterialTheme.colorScheme.primary.copy(alpha = 0.12f),
                            modifier = Modifier.size(38.dp)
                        ) {
                            Box(contentAlignment = Alignment.Center) {
                                Icon(
                                    imageVector = if (isSnoozeActive) Icons.Outlined.PlayArrow else Icons.Outlined.Pause,
                                    contentDescription = null,
                                    tint = if (isSnoozeActive) StatusOrange else MaterialTheme.colorScheme.primary,
                                    modifier = Modifier.size(18.dp)
                                )
                            }
                        }
                        Spacer(modifier = Modifier.width(12.dp))
                        Column {
                            Text(
                                text = strings.mindfulBreakTitle,
                                style = MaterialTheme.typography.titleSmall.copy(fontWeight = FontWeight.Bold),
                                color = MaterialTheme.colorScheme.onBackground
                            )
                            Text(
                                text = if (isSnoozeActive) {
                                    if (strings is com.nomorereels.app.ui.i18n.EnglishStrings)
                                        "Focus window active: $snoozeRemainingMinutes min left"
                                    else
                                        "Mola aktif: $snoozeRemainingMinutes dakika kaldı"
                                } else {
                                    if (strings is com.nomorereels.app.ui.i18n.EnglishStrings) "Autopilot stopped" else "Otopilot durduruldu"
                                },
                                style = MaterialTheme.typography.bodySmall.copy(fontSize = 11.5.sp),
                                color = MaterialTheme.colorScheme.onSurfaceVariant
                            )
                        }
                    }

                    Surface(
                        shape = RoundedCornerShape(10.dp),
                        color = if (isSnoozeActive) StatusOrange.copy(alpha = 0.15f) else MaterialTheme.colorScheme.primary.copy(alpha = 0.12f),
                        modifier = Modifier.clickable {
                            if (isSnoozeActive) {
                                onEndSnoozeClick()
                            } else {
                                onStartSnoozeClick()
                            }
                        }
                    ) {
                        Text(
                            modifier = Modifier.padding(horizontal = 10.dp, vertical = 6.dp),
                            text = if (isSnoozeActive) {
                                if (strings is com.nomorereels.app.ui.i18n.EnglishStrings) "Tap to end" else "Bitirmek için tıkla"
                            } else {
                                if (strings is com.nomorereels.app.ui.i18n.EnglishStrings) "Take a break →" else "Mola başlat →"
                            },
                            style = MaterialTheme.typography.bodySmall.copy(
                                fontSize = 11.5.sp,
                                fontWeight = FontWeight.SemiBold
                            ),
                            color = if (isSnoozeActive) StatusOrange else MaterialTheme.colorScheme.primary
                        )
                    }
                }
            }
        }

        // Instagram Güncellemesi / Tespit Sağlığı Uyarısı (30+ dk IG kullanımında 0 tespit varsa)
        item {
            val isDetectionWarning by com.nomorereels.app.data.LocalDiagnosticsStore
                .isDetectionHealthWarningActive.collectAsStateWithLifecycle()
            if (isDetectionWarning && isServiceActive) {
                val ctx = LocalContext.current
                val isEng = strings is com.nomorereels.app.ui.i18n.EnglishStrings
                Surface(
                    shape = RoundedCornerShape(16.dp),
                    color = StatusOrange.copy(alpha = 0.12f),
                    border = BorderStroke(1.dp, StatusOrange.copy(alpha = 0.35f)),
                    modifier = Modifier
                        .fillMaxWidth()
                        .clickable {
                            com.nomorereels.app.data.LocalDiagnosticsStore.shareDiagnosticReport(ctx)
                        }
                ) {
                    Row(
                        modifier = Modifier.padding(14.dp),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Icon(
                            imageVector = Icons.Outlined.Info,
                            contentDescription = null,
                            tint = StatusOrange,
                            modifier = Modifier.size(20.dp)
                        )
                        Spacer(modifier = Modifier.width(12.dp))
                        Column(modifier = Modifier.weight(1f)) {
                            Text(
                                text = if (isEng) "Instagram UI Update Detected?" else "Instagram Güncellemesi Uyarısı",
                                style = MaterialTheme.typography.titleSmall.copy(fontWeight = FontWeight.Bold),
                                color = MaterialTheme.colorScheme.onBackground
                            )
                            Spacer(modifier = Modifier.height(2.dp))
                            Text(
                                text = if (isEng)
                                    "No Reels were detected during extended Instagram usage. Tap to share a local diagnostic log."
                                else
                                    "Uzun süreli Instagram kullanımında hiç Reels tespit edilmedi. Tanılama günlüğünü paylaşmak için dokunun.",
                                style = MaterialTheme.typography.bodySmall.copy(fontSize = 11.5.sp),
                                color = MaterialTheme.colorScheme.onSurfaceVariant
                            )
                        }
                    }
                }
            }
        }

        // 4. KURTARILAN ZAMANLA NELER MÜMKÜN? (Detaylandırılmış İnsan Odaklı Anlam Kartı)
        item {
            val isEnglish = strings is com.nomorereels.app.ui.i18n.EnglishStrings
            KurtarilanZamanCard(
                savedMinutes = savedMinutes,
                savedSeconds = savedSeconds,
                blockCount = blockCount,
                isEnglish = isEnglish
            )
        }

        // 5. GÜNÜN SÖZÜ
        item {
            val isEnglish = strings is com.nomorereels.app.ui.i18n.EnglishStrings
            val quoteOfTheDay = remember { MindfulQuotes.getQuoteForDay() }
            Surface(
                modifier = Modifier
                    .fillMaxWidth()
                    .clip(RoundedCornerShape(NmrTheme.dimens.cardRadius)),
                shape = RoundedCornerShape(NmrTheme.dimens.cardRadius),
                color = NmrTheme.colors.mistBlue.copy(alpha = 0.35f),
                border = BorderStroke(0.5.dp, MaterialTheme.colorScheme.outline.copy(alpha = 0.4f)),
                tonalElevation = 0.dp
            ) {
                Column(modifier = Modifier.padding(18.dp)) {
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Row(
                            verticalAlignment = Alignment.CenterVertically,
                            horizontalArrangement = Arrangement.spacedBy(8.dp)
                        ) {
                            Text(text = "🌿", fontSize = 18.sp)
                            Text(
                                text = strings.quoteOfTheDayTitle,
                                style = MaterialTheme.typography.labelSmall.copy(
                                    fontWeight = FontWeight.SemiBold,
                                    letterSpacing = 1.1.sp
                                ),
                                color = MaterialTheme.colorScheme.primary
                            )
                        }
                    }
                    Spacer(modifier = Modifier.height(10.dp))
                    Text(
                        text = "“${quoteOfTheDay.localized(isEnglish)}”",
                        style = MaterialTheme.typography.bodyMedium.copy(
                            fontWeight = FontWeight.Medium,
                            fontStyle = FontStyle.Italic,
                            lineHeight = 22.sp
                        ),
                        color = MaterialTheme.colorScheme.onBackground
                    )
                    Spacer(modifier = Modifier.height(6.dp))
                    Text(
                        text = "— ${quoteOfTheDay.author}",
                        style = MaterialTheme.typography.labelSmall.copy(
                            fontWeight = FontWeight.SemiBold,
                            color = MaterialTheme.colorScheme.primary
                        ),
                        modifier = Modifier.align(Alignment.End)
                    )
                }
            }
        }

        item {
            Spacer(modifier = Modifier.height(28.dp))
        }
    }
}

@Composable
fun KurtarilanZamanCard(
    savedMinutes: Long,
    savedSeconds: Long,
    blockCount: Int,
    isEnglish: Boolean
) {
    val headerTitle = when {
        savedMinutes >= 1 -> if (isEnglish) "With your ~$savedMinutes min focus today:" else "Bugün kazandığın ~$savedMinutes dakika ile:"
        savedSeconds > 0 -> if (isEnglish) "With your ~$savedSeconds s pause today:" else "Bugün kazandığın ~$savedSeconds saniye ile:"
        else -> if (isEnglish) "What saved time gives you:" else "Reels yerine kendine alan açtığında:"
    }

    val suggestionText = remember(savedMinutes, savedSeconds, isEnglish) {
        PracticalSuggestions.forTimeSingle(savedMinutes, savedSeconds, isEnglish)
    }

    Surface(
        shape = RoundedCornerShape(18.dp),
        color = MaterialTheme.colorScheme.surface,
        border = BorderStroke(1.dp, MaterialTheme.colorScheme.outline),
        modifier = Modifier.fillMaxWidth()
    ) {
        Column(modifier = Modifier.padding(18.dp)) {
            Text(
                text = headerTitle,
                style = MaterialTheme.typography.titleMedium.copy(
                    fontWeight = FontWeight.Bold,
                    fontSize = 15.sp
                ),
                color = MaterialTheme.colorScheme.onBackground
            )

            Spacer(modifier = Modifier.height(8.dp))

            Text(
                text = suggestionText,
                style = MaterialTheme.typography.bodyMedium.copy(
                    fontSize = 13.5.sp,
                    lineHeight = 20.sp
                ),
                color = MaterialTheme.colorScheme.onSurfaceVariant
            )
        }
    }
}

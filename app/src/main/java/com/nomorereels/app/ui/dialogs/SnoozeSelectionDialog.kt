package com.nomorereels.app.ui.dialogs

import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.outlined.ChevronRight
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.alpha
import androidx.compose.ui.draw.clip
import androidx.compose.ui.unit.dp
import com.nomorereels.app.data.AppLanguage
import com.nomorereels.app.data.SettingsStore
import com.nomorereels.app.ui.i18n.LocalAppStrings
import com.nomorereels.app.ui.theme.NmrTheme
import kotlinx.coroutines.delay

@Composable
fun SnoozeSelectionDialog(
    onDismiss: () -> Unit,
    onSelectMinutes: (Int) -> Unit
) {
    val strings = LocalAppStrings.current
    val isEnglish = SettingsStore.appLanguage.collectAsState().value == AppLanguage.EN
    val snoozeCountToday by SettingsStore.snoozeCountToday.collectAsState()
    val isBedtime = SettingsStore.isBedtimeActiveNow()
    val remainingSnoozes = (SettingsStore.MAX_DAILY_SNOOZES - snoozeCountToday).coerceAtLeast(0)
    val canSnooze = !isBedtime && remainingSnoozes > 0

    // Kademeli bekleme süresi: 1. mola 10sn, 2. mola 20sn, 3. mola 40sn
    val initialWaitSeconds = remember(snoozeCountToday) {
        when (snoozeCountToday) {
            0 -> 10
            1 -> 20
            else -> 40
        }
    }
    var remainingWaitSeconds by remember(initialWaitSeconds) { mutableIntStateOf(if (canSnooze) initialWaitSeconds else 0) }

    LaunchedEffect(canSnooze, initialWaitSeconds) {
        if (!canSnooze) return@LaunchedEffect
        while (remainingWaitSeconds > 0) {
            delay(1000L)
            remainingWaitSeconds--
        }
    }

    AlertDialog(
        onDismissRequest = onDismiss,
        shape = RoundedCornerShape(NmrTheme.dimens.dialogRadius),
        containerColor = MaterialTheme.colorScheme.surface,
        tonalElevation = 0.dp,
        title = {
            Text(
                text = strings.snoozeDialogTitle,
                style = MaterialTheme.typography.titleLarge,
                color = MaterialTheme.colorScheme.onBackground
            )
        },
        text = {
            Column(verticalArrangement = Arrangement.spacedBy(10.dp)) {
                if (isBedtime) {
                    Text(
                        text = if (isEnglish) {
                            "Bedtime Focus Hours are active. Breaks are disabled during bedtime to protect your sleep."
                        } else {
                            "Gece Odak Saati aktif. Uyku düzenini korumak için bu saatlerde mola verilemez."
                        },
                        style = MaterialTheme.typography.bodyMedium,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                } else if (remainingSnoozes <= 0) {
                    Text(
                        text = if (isEnglish) {
                            "You have used all ${SettingsStore.MAX_DAILY_SNOOZES} mindful breaks for today. Rights reset at midnight."
                        } else {
                            "Bugünkü ${SettingsStore.MAX_DAILY_SNOOZES} bilinçli mola hakkının tamamını kullandın. Hakların gece yarısı yenilenir."
                        },
                        style = MaterialTheme.typography.bodyMedium,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                } else {
                    Text(
                        text = strings.snoozeDialogDesc,
                        style = MaterialTheme.typography.bodyMedium,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                    Text(
                        text = if (remainingWaitSeconds > 0) {
                            if (isEnglish) {
                                "Take a breath ($remainingWaitSeconds s) · Remaining today: $remainingSnoozes/${SettingsStore.MAX_DAILY_SNOOZES}"
                            } else {
                                "Kısa bir nefes al ($remainingWaitSeconds sn) · Bugün kalan hak: $remainingSnoozes/${SettingsStore.MAX_DAILY_SNOOZES}"
                            }
                        } else {
                            if (isEnglish) {
                                "Ready · Remaining breaks today: $remainingSnoozes/${SettingsStore.MAX_DAILY_SNOOZES}"
                            } else {
                                "Hazır · Bugün kalan mola hakkı: $remainingSnoozes/${SettingsStore.MAX_DAILY_SNOOZES}"
                            }
                        },
                        style = MaterialTheme.typography.labelMedium,
                        color = MaterialTheme.colorScheme.primary
                    )
                    Spacer(modifier = Modifier.height(4.dp))
                    val snoozeOptions = listOf(
                        5 to (if (isEnglish) "5 Minutes (Short pause)" else "5 Dakika (Kısa mola)"),
                        10 to (if (isEnglish) "10 Minutes (Mindful break)" else "10 Dakika (Bilinçli mola)"),
                        15 to strings.snooze15Min
                    )
                    val isUnlocked = remainingWaitSeconds <= 0
                    snoozeOptions.forEach { (minutes, label) ->
                        Surface(
                            modifier = Modifier
                                .fillMaxWidth()
                                .alpha(if (isUnlocked) 1f else 0.45f)
                                .clip(RoundedCornerShape(NmrTheme.dimens.cardRadius))
                                .clickable(enabled = isUnlocked) { onSelectMinutes(minutes) },
                            shape = RoundedCornerShape(NmrTheme.dimens.cardRadius),
                            color = MaterialTheme.colorScheme.background,
                            border = BorderStroke(1.dp, MaterialTheme.colorScheme.outline),
                            tonalElevation = 0.dp
                        ) {
                            Row(
                                modifier = Modifier.padding(horizontal = 16.dp, vertical = 14.dp),
                                verticalAlignment = Alignment.CenterVertically,
                                horizontalArrangement = Arrangement.SpaceBetween
                            ) {
                                Text(
                                    text = label,
                                    style = MaterialTheme.typography.titleMedium,
                                    color = MaterialTheme.colorScheme.onBackground
                                )
                                Icon(
                                    imageVector = Icons.Outlined.ChevronRight,
                                    contentDescription = null,
                                    tint = MaterialTheme.colorScheme.primary,
                                    modifier = Modifier.size(20.dp)
                                )
                            }
                        }
                    }
                }
            }
        },
        confirmButton = {},
        dismissButton = {
            TextButton(onClick = onDismiss) {
                Text(text = strings.cancel, style = MaterialTheme.typography.titleMedium, color = MaterialTheme.colorScheme.onBackground)
            }
        }
    )
}


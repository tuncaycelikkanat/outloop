package com.nomorereels.app.ui.settings

import androidx.compose.animation.AnimatedVisibility
import androidx.compose.foundation.BorderStroke
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
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.outlined.ArrowBack
import androidx.compose.material.icons.outlined.Delete
import androidx.compose.material.icons.outlined.Pause
import androidx.compose.material.icons.outlined.PlayArrow
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.nomorereels.app.detection.DetectionLogStore
import com.nomorereels.app.ui.i18n.EnglishStrings
import com.nomorereels.app.ui.i18n.LocalAppStrings
import com.nomorereels.app.ui.theme.NmrChip
import com.nomorereels.app.ui.theme.NmrHairlineDivider
import com.nomorereels.app.ui.theme.NmrLogTextStyle
import com.nomorereels.app.ui.theme.NmrTheme

enum class LogFilterType {
    ALL, SCREEN_CHANGE, ACTION, NODE_TREE
}

@Composable
fun LiveLogsScreen(
    onBack: () -> Unit,
    showBackButton: Boolean = false
) {
    val strings = LocalAppStrings.current
    val rawLogs by DetectionLogStore.logs.collectAsState()
    var isPaused by remember { mutableStateOf(false) }
    var displayedLogs by remember { mutableStateOf(rawLogs) }
    var selectedFilter by remember { mutableStateOf(LogFilterType.ALL) }
    var expandedLogId by remember { mutableStateOf<Long?>(null) }

    LaunchedEffect(rawLogs, isPaused) {
        if (!isPaused) {
            displayedLogs = rawLogs
        }
    }

    val filteredLogs = remember(displayedLogs, selectedFilter) {
        when (selectedFilter) {
            LogFilterType.SCREEN_CHANGE -> displayedLogs.filter { it.actionTaken == null }
            LogFilterType.ACTION -> displayedLogs.filter { it.actionTaken != null }
            LogFilterType.NODE_TREE -> displayedLogs.filter { it.keyNodes.isNotEmpty() }
            LogFilterType.ALL -> displayedLogs
        }
    }

    Column(
        modifier = Modifier
            .fillMaxSize()
            .padding(horizontal = NmrTheme.dimens.screenHorizontalMargin, vertical = 12.dp)
    ) {
        Spacer(modifier = Modifier.height(10.dp))
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(bottom = 12.dp),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically
        ) {
            Row(
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.spacedBy(10.dp),
                modifier = Modifier.weight(1f)
            ) {
                if (showBackButton) {
                    IconButton(onClick = onBack) {
                        Icon(
                            imageVector = Icons.AutoMirrored.Outlined.ArrowBack,
                            contentDescription = strings.backButton,
                            tint = MaterialTheme.colorScheme.onBackground
                        )
                    }
                }
                Column {
                    Text(
                        text = if (strings is EnglishStrings)
                            "SYSTEM TRANSPARENCY"
                        else
                            "SİSTEM ŞEFFAFLIĞI",
                        style = MaterialTheme.typography.labelSmall.copy(
                            letterSpacing = 1.6.sp,
                            fontWeight = FontWeight.Medium
                        ),
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                    Spacer(modifier = Modifier.height(3.dp))
                    Text(
                        text = if (strings is EnglishStrings)
                            "Live Diagnostics"
                        else
                            "Canlı Teşhis",
                        style = MaterialTheme.typography.headlineMedium.copy(
                            fontWeight = FontWeight.Bold,
                            fontSize = 25.sp,
                            lineHeight = 31.sp,
                            letterSpacing = (-0.4).sp
                        ),
                        color = MaterialTheme.colorScheme.onBackground
                    )
                    Spacer(modifier = Modifier.height(2.dp))
                    Text(
                        text = if (isPaused) strings.logsPausedStatus else strings.logsActiveStatus(filteredLogs.size),
                        style = MaterialTheme.typography.labelSmall,
                        color = if (isPaused) NmrTheme.colors.statusOrange else MaterialTheme.colorScheme.onSurfaceVariant
                    )
                }
            }

            Row(horizontalArrangement = Arrangement.spacedBy(4.dp)) {
                IconButton(onClick = { isPaused = !isPaused }) {
                    Icon(
                        imageVector = if (isPaused) Icons.Outlined.PlayArrow else Icons.Outlined.Pause,
                        contentDescription = if (isPaused) "Devam Et" else "Duraklat",
                        tint = if (isPaused) NmrTheme.colors.statusOrange else MaterialTheme.colorScheme.onBackground
                    )
                }
                IconButton(onClick = { DetectionLogStore.clear() }) {
                    Icon(
                        imageVector = Icons.Outlined.Delete,
                        contentDescription = "Temizle",
                        tint = MaterialTheme.colorScheme.onBackground
                    )
                }
            }
        }

        Row(
            modifier = Modifier.fillMaxWidth().padding(bottom = 12.dp),
            horizontalArrangement = Arrangement.spacedBy(8.dp)
        ) {
            val filterOptions = listOf(
                LogFilterType.ALL to strings.logsFilterAll,
                LogFilterType.SCREEN_CHANGE to strings.logsFilterScreenChange,
                LogFilterType.ACTION to strings.logsFilterAction,
                LogFilterType.NODE_TREE to strings.logsFilterNodeTree
            )
            filterOptions.forEach { (type, label) ->
                NmrChip(
                    label = label,
                    selected = selectedFilter == type,
                    onClick = { selectedFilter = type }
                )
            }
        }

        NmrHairlineDivider()
        Spacer(modifier = Modifier.height(8.dp))

        if (filteredLogs.isEmpty()) {
            Box(
                modifier = Modifier
                    .weight(1f)
                    .fillMaxWidth()
                    .padding(24.dp),
                contentAlignment = Alignment.Center
            ) {
                Column(horizontalAlignment = Alignment.CenterHorizontally) {
                    Text(
                        text = strings.logsEmptyTitle,
                        style = MaterialTheme.typography.titleMedium,
                        color = MaterialTheme.colorScheme.onBackground
                    )
                    Spacer(modifier = Modifier.height(4.dp))
                    Text(
                        text = strings.logsEmptySub,
                        style = MaterialTheme.typography.bodyMedium,
                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                        textAlign = TextAlign.Center
                    )
                }
            }
        } else {
            LazyColumn(
                modifier = Modifier.weight(1f).fillMaxWidth(),
                verticalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                items(filteredLogs, key = { it.id }) { logItem ->
                    val tag = when {
                        logItem.actionTaken != null -> "EYLEM"
                        logItem.screenType.isReels -> "ALGI"
                        else -> "EKRAN"
                    }
                    val badgeColor = when (tag) {
                        "EKRAN" -> MaterialTheme.colorScheme.outline
                        "ALGI" -> NmrTheme.colors.statusOrange
                        "EYLEM" -> NmrTheme.colors.statusGreen
                        else -> MaterialTheme.colorScheme.outline
                    }
                    val badgeContainer = when (tag) {
                        "EKRAN" -> MaterialTheme.colorScheme.surface
                        "ALGI" -> NmrTheme.colors.statusOrangeContainer
                        "EYLEM" -> NmrTheme.colors.statusGreenContainer
                        else -> MaterialTheme.colorScheme.surface
                    }

                    val isExpanded = expandedLogId == logItem.id

                    Surface(
                        modifier = Modifier
                            .fillMaxWidth()
                            .clickable {
                                expandedLogId = if (isExpanded) null else logItem.id
                            },
                        shape = RoundedCornerShape(8.dp),
                        color = MaterialTheme.colorScheme.surface,
                        border = BorderStroke(1.dp, MaterialTheme.colorScheme.outline),
                        tonalElevation = 0.dp
                    ) {
                        Column(modifier = Modifier.padding(10.dp)) {
                            Row(
                                modifier = Modifier.fillMaxWidth(),
                                verticalAlignment = Alignment.CenterVertically
                            ) {
                                Text(
                                    text = logItem.timeFormatted,
                                    style = NmrLogTextStyle,
                                    color = MaterialTheme.colorScheme.onSurfaceVariant
                                )
                                Spacer(modifier = Modifier.width(8.dp))
                                Surface(
                                    shape = RoundedCornerShape(4.dp),
                                    color = badgeContainer,
                                    tonalElevation = 0.dp
                                ) {
                                    Text(
                                        text = tag,
                                        style = NmrLogTextStyle.copy(fontWeight = FontWeight.Bold),
                                        color = badgeColor,
                                        modifier = Modifier.padding(horizontal = 6.dp, vertical = 2.dp)
                                    )
                                }
                                Spacer(modifier = Modifier.width(8.dp))
                                Text(
                                    text = logItem.details,
                                    style = NmrLogTextStyle,
                                    color = MaterialTheme.colorScheme.onBackground,
                                    modifier = Modifier.weight(1f)
                                )
                            }

                            if (logItem.actionTaken != null) {
                                Spacer(modifier = Modifier.height(4.dp))
                                Text(
                                    text = "↳ Sonuç: ${logItem.actionTaken}",
                                    style = NmrLogTextStyle.copy(fontWeight = FontWeight.SemiBold),
                                    color = MaterialTheme.colorScheme.primary
                                )
                            }

                            if (logItem.keyNodes.isNotEmpty()) {
                                AnimatedVisibility(visible = isExpanded) {
                                    Column(modifier = Modifier.padding(top = 8.dp)) {
                                        NmrHairlineDivider()
                                        Spacer(modifier = Modifier.height(6.dp))
                                        Text(
                                            text = "Algılama Düğümleri:",
                                            style = MaterialTheme.typography.labelSmall,
                                            color = MaterialTheme.colorScheme.onSurfaceVariant
                                        )
                                        logItem.keyNodes.forEach { nodeInfo ->
                                            Text(
                                                text = "• $nodeInfo",
                                                style = NmrLogTextStyle,
                                                color = MaterialTheme.colorScheme.onBackground
                                            )
                                        }
                                    }
                                }
                            }
                        }
                    }
                }
            }
        }
    }
}

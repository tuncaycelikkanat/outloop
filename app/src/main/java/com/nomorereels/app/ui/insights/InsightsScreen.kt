package com.nomorereels.app.ui.insights

import androidx.compose.animation.AnimatedContent
import androidx.compose.animation.core.Spring
import androidx.compose.animation.core.animateDpAsState
import androidx.compose.animation.core.spring
import androidx.compose.animation.core.tween
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.animation.togetherWith
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.gestures.awaitEachGesture
import androidx.compose.foundation.gestures.awaitFirstDown
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxHeight
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.offset
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.outlined.ChevronRight
import androidx.compose.material3.Icon
import androidx.compose.material3.LinearProgressIndicator
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.geometry.CornerRadius
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.PathEffect
import androidx.compose.ui.hapticfeedback.HapticFeedbackType
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.platform.LocalHapticFeedback
import androidx.compose.ui.text.font.FontStyle
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.IntOffset
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.nomorereels.app.data.QuotaStore
import com.nomorereels.app.policy.DmReelMode
import com.nomorereels.app.ui.i18n.EnglishStrings
import com.nomorereels.app.ui.i18n.LocalAppStrings
import com.nomorereels.app.ui.theme.NmrTheme
import java.time.LocalDate

@Composable
fun InsightsScreen(
    viewModel: InsightsViewModel,
    onNavigateToSupport: () -> Unit,
    modifier: Modifier = Modifier
) {
    val uiState by viewModel.uiState.collectAsStateWithLifecycle()

    InsightsScreenContent(
        uiState = uiState,
        onNavigateToSupport = onNavigateToSupport,
        modifier = modifier
    )
}

@Composable
fun InsightsScreenContent(
    uiState: InsightsUiState,
    onNavigateToSupport: () -> Unit,
    modifier: Modifier = Modifier
) {
    val strings = LocalAppStrings.current
    val isEnglish = strings is EnglishStrings

    val generalMode = uiState.generalMode
    val blockCount = uiState.blockCount
    val todayReelCount = uiState.todayReelCount
    val dailyCountLimit = uiState.dailyCountLimit
    val todayMinutesSpent = uiState.todayMinutesSpent
    val dailyTimeLimitMinutes = uiState.dailyTimeLimitMinutes
    val isSupporter = uiState.isSupporter
    val matrix = uiState.matrix
    val dailyHistory = uiState.dailyHistory

    val isQuotaModeActive = generalMode == DmReelMode.COUNT_QUOTA || generalMode == DmReelMode.TIME_QUOTA

    val savedMinutes = uiState.todaySavedMinutes

    // Haftalık kazanım verisi (HeatmapStore gerçek matris verisiyle)
    val dayNames = if (isEnglish) {
        listOf("Mon", "Tue", "Wed", "Thu", "Fri", "Sat", "Sun")
    } else {
        listOf("Pzt", "Sal", "Çar", "Per", "Cum", "Cmt", "Paz")
    }
    val currentDayIndex = remember {
        val dow = LocalDate.now().dayOfWeek.value
        (dow - 1).coerceIn(0, 6)
    }
    val weeklyDayMinutes = remember(matrix, savedMinutes, currentDayIndex) {
        (0..6).map { dayIdx ->
            val attempts = if (matrix.size >= 168) {
                (0..23).sumOf { h -> matrix[dayIdx * 24 + h] }
            } else 0
            val estMinutes = (attempts * 45L) / 60L
            if (dayIdx == currentDayIndex) maxOf(estMinutes, savedMinutes) else estMinutes
        }
    }
    val fullDayNames = if (isEnglish) {
        listOf("Monday", "Tuesday", "Wednesday", "Thursday", "Friday", "Saturday", "Sunday")
    } else {
        listOf("Pazartesi", "Salı", "Çarşamba", "Perşembe", "Cuma", "Cumartesi", "Pazar")
    }

    val tenDayAvgMinutes = remember(dailyHistory, weeklyDayMinutes) {
        QuotaStore.getTenDayAverageMinutes(weeklyDayMinutes)
    }
    val maxWeeklyMin = maxOf(
        weeklyDayMinutes.maxOrNull()?.coerceAtLeast(15L) ?: 15L,
        tenDayAvgMinutes + 5L
    )
    val totalWeeklyMinutes = weeklyDayMinutes.sum()
    val weeklyTotalHours = (totalWeeklyMinutes / 60).toInt()
    val weeklyTotalRemMinutes = (totalWeeklyMinutes % 60).toInt()

    var selectedDayIndex by remember { mutableStateOf<Int?>(null) }
    val haptic = LocalHapticFeedback.current

    val totalAttempts = remember(matrix) { matrix.sum() }
    val maxCellAttempts = remember(matrix) { matrix.maxOrNull()?.coerceAtLeast(1) ?: 1 }

    LazyColumn(
        modifier = modifier
            .fillMaxSize()
            .padding(horizontal = NmrTheme.dimens.screenHorizontalMargin),
        verticalArrangement = Arrangement.spacedBy(16.dp)
    ) {
        // 1. Ekran Başlığı
        item {
            Spacer(modifier = Modifier.height(10.dp))
            Column {
                Text(
                    text = strings.insightsTagline,
                    style = MaterialTheme.typography.headlineMedium.copy(
                        fontWeight = FontWeight.Bold,
                        fontSize = 25.sp,
                        lineHeight = 31.sp,
                        letterSpacing = (-0.4).sp
                    ),
                    color = MaterialTheme.colorScheme.onBackground
                )
                Spacer(modifier = Modifier.height(4.dp))
                Text(
                    text = strings.insightsSubtext,
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
            }
        }

        // 2. KART 1: Haftalık Kazanım Özeti & Çubuk Grafik
        item {
            Surface(
                modifier = Modifier
                    .fillMaxWidth()
                    .clip(RoundedCornerShape(NmrTheme.dimens.cardRadius)),
                shape = RoundedCornerShape(NmrTheme.dimens.cardRadius),
                color = MaterialTheme.colorScheme.surface,
                border = BorderStroke(0.5.dp, MaterialTheme.colorScheme.outline.copy(alpha = 0.5f)),
                tonalElevation = 0.dp
            ) {
                Column(modifier = Modifier.padding(18.dp)) {
                    AnimatedContent(
                        targetState = selectedDayIndex,
                        transitionSpec = {
                            fadeIn(tween(140)) togetherWith fadeOut(tween(100))
                        },
                        label = "ChartHeaderTransition"
                    ) { selIdx ->
                        if (selIdx != null) {
                            val selMins = weeklyDayMinutes[selIdx]
                            val selDayName = fullDayNames[selIdx]
                            val isToday = (selIdx == currentDayIndex)
                            val timeStr = if (selMins >= 60) {
                                val h = selMins / 60
                                val m = selMins % 60
                                if (m > 0) "$h sa $m dk" else "$h saat"
                            } else {
                                "$selMins dk"
                            }
                            Column {
                                Row(
                                    verticalAlignment = Alignment.CenterVertically,
                                    horizontalArrangement = Arrangement.spacedBy(8.dp)
                                ) {
                                    Text(
                                        text = timeStr,
                                        style = MaterialTheme.typography.titleLarge.copy(
                                            fontWeight = FontWeight.Bold,
                                            fontSize = 22.sp
                                        ),
                                        color = MaterialTheme.colorScheme.primary
                                    )
                                    Surface(
                                        shape = RoundedCornerShape(6.dp),
                                        color = MaterialTheme.colorScheme.primary.copy(alpha = 0.15f)
                                    ) {
                                        Text(
                                            text = if (isToday) (if (isEnglish) "Today · $selDayName" else "Bugün · $selDayName") else selDayName,
                                            style = MaterialTheme.typography.labelSmall.copy(fontWeight = FontWeight.SemiBold),
                                            color = MaterialTheme.colorScheme.primary,
                                            modifier = Modifier.padding(horizontal = 7.dp, vertical = 3.dp)
                                        )
                                    }
                                }
                                Spacer(modifier = Modifier.height(2.dp))
                                Text(
                                    text = if (isEnglish) "Recorded focus time for this day" else "O gün kaydedilen odak / kazanım süresi",
                                    style = MaterialTheme.typography.bodySmall,
                                    color = MaterialTheme.colorScheme.onSurfaceVariant
                                )
                            }
                        } else {
                            Column {
                                Text(
                                    text = strings.weeklyFocusDesc(weeklyTotalHours, weeklyTotalRemMinutes),
                                    style = MaterialTheme.typography.titleMedium.copy(fontWeight = FontWeight.SemiBold),
                                    color = MaterialTheme.colorScheme.onBackground
                                )
                                Spacer(modifier = Modifier.height(2.dp))
                                Row(
                                    modifier = Modifier.fillMaxWidth(),
                                    horizontalArrangement = Arrangement.SpaceBetween,
                                    verticalAlignment = Alignment.CenterVertically
                                ) {
                                    Text(
                                        text = if (isEnglish) "Daily Focus Distribution (Mon – Sun)" else "Günlük Odak Dağılımı (Pzt – Paz)",
                                        style = MaterialTheme.typography.bodySmall,
                                        color = MaterialTheme.colorScheme.onSurfaceVariant
                                    )
                                    val legendDashColor = MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.8f)
                                    Row(
                                        verticalAlignment = Alignment.CenterVertically,
                                        horizontalArrangement = Arrangement.spacedBy(5.dp)
                                    ) {
                                        Canvas(modifier = Modifier.size(width = 14.dp, height = 2.dp)) {
                                            drawLine(
                                                color = legendDashColor,
                                                start = Offset(0f, size.height / 2),
                                                end = Offset(size.width, size.height / 2),
                                                strokeWidth = size.height,
                                                pathEffect = PathEffect.dashPathEffect(floatArrayOf(4.dp.toPx(), 3.dp.toPx()), 0f)
                                            )
                                        }
                                        Text(
                                            text = if (isEnglish) "10-day avg: $tenDayAvgMinutes min" else "10G ort: $tenDayAvgMinutes dk",
                                            style = MaterialTheme.typography.labelSmall.copy(
                                                fontSize = 11.sp,
                                                fontWeight = FontWeight.Medium
                                            ),
                                            color = MaterialTheme.colorScheme.onSurfaceVariant
                                        )
                                    }
                                }
                            }
                        }
                    }

                    Spacer(modifier = Modifier.height(16.dp))

                    // 7 Günlük Çubuk Grafik
                    Box(
                        modifier = Modifier
                            .fillMaxWidth()
                            .height(96.dp)
                            .pointerInput(Unit) {
                                awaitEachGesture {
                                    try {
                                        val down = awaitFirstDown(requireUnconsumed = false)
                                        val dayWidth = size.width / 7f
                                        val startIdx = (down.position.x / dayWidth).toInt().coerceIn(0, 6)
                                        selectedDayIndex = startIdx
                                        haptic.performHapticFeedback(HapticFeedbackType.TextHandleMove)

                                        var lastIdx = startIdx
                                        while (true) {
                                            val event = awaitPointerEvent()
                                            if (event.changes.all { !it.pressed }) {
                                                selectedDayIndex = null
                                                break
                                            }
                                            if (event.changes.any { it.isConsumed }) {
                                                selectedDayIndex = null
                                                break
                                            }
                                            val activeChange = event.changes.firstOrNull { it.pressed }
                                            if (activeChange != null) {
                                                val dx = kotlin.math.abs(activeChange.position.x - down.position.x)
                                                val dy = kotlin.math.abs(activeChange.position.y - down.position.y)
                                                if (dx > dy && dx > 8f) {
                                                    activeChange.consume()
                                                }
                                                val currentIdx = (activeChange.position.x / dayWidth).toInt().coerceIn(0, 6)
                                                if (currentIdx != lastIdx) {
                                                    lastIdx = currentIdx
                                                    selectedDayIndex = currentIdx
                                                    haptic.performHapticFeedback(HapticFeedbackType.TextHandleMove)
                                                }
                                            }
                                        }
                                    } finally {
                                        selectedDayIndex = null
                                    }
                                }
                            }
                    ) {
                        val avgRatio = (tenDayAvgMinutes.toFloat() / maxWeeklyMin.toFloat()).coerceIn(0.10f, 0.90f)
                        val dashedLineColor = MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.50f)

                        Canvas(
                            modifier = Modifier
                                .fillMaxWidth()
                                .height(68.dp)
                        ) {
                            val strokePx = 1.2.dp.toPx()
                            val yPos = size.height * (1f - avgRatio)
                            drawLine(
                                color = dashedLineColor,
                                start = Offset(0f, yPos),
                                end = Offset(size.width, yPos),
                                strokeWidth = strokePx,
                                pathEffect = PathEffect.dashPathEffect(floatArrayOf(6.dp.toPx(), 5.dp.toPx()), 0f)
                            )
                        }

                        Box(
                            modifier = Modifier
                                .align(Alignment.TopEnd)
                                .offset {
                                    val chartHeightPx = 68.dp.toPx()
                                    val y = (chartHeightPx * (1f - avgRatio) - 15.dp.toPx()).toInt().coerceAtLeast(0)
                                    IntOffset(0, y)
                                }
                        ) {
                            Surface(
                                shape = RoundedCornerShape(4.dp),
                                color = MaterialTheme.colorScheme.surface.copy(alpha = 0.88f),
                                border = BorderStroke(0.5.dp, MaterialTheme.colorScheme.outline.copy(alpha = 0.35f))
                            ) {
                                Text(
                                    text = if (isEnglish) "Avg: $tenDayAvgMinutes m" else "Ort: $tenDayAvgMinutes dk",
                                    style = MaterialTheme.typography.labelSmall.copy(fontSize = 9.sp, fontWeight = FontWeight.SemiBold),
                                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                                    modifier = Modifier.padding(horizontal = 4.dp, vertical = 1.5.dp)
                                )
                            }
                        }

                        Row(
                            modifier = Modifier
                                .fillMaxWidth()
                                .fillMaxHeight(),
                            horizontalArrangement = Arrangement.SpaceBetween,
                            verticalAlignment = Alignment.Bottom
                        ) {
                            dayNames.forEachIndexed { idx, day ->
                                val mins = weeklyDayMinutes[idx]
                                val barHeightDp = ((mins.toFloat() / maxWeeklyMin.toFloat()).coerceIn(0.12f, 1f) * 64).dp
                                val isCurrentDay = (idx == currentDayIndex)
                                val isSelected = (selectedDayIndex == idx)
                                val isAnySelected = (selectedDayIndex != null)

                                val animatedBarWidth by animateDpAsState(
                                    targetValue = if (isSelected) 22.dp else 16.dp,
                                    animationSpec = spring(stiffness = Spring.StiffnessMediumLow),
                                    label = "barWidth_$idx"
                                )

                                val barColor = when {
                                    isSelected -> MaterialTheme.colorScheme.primary
                                    isAnySelected -> MaterialTheme.colorScheme.primary.copy(alpha = 0.18f)
                                    isCurrentDay -> MaterialTheme.colorScheme.primary
                                    else -> MaterialTheme.colorScheme.primary.copy(alpha = 0.35f)
                                }

                                Column(
                                    horizontalAlignment = Alignment.CenterHorizontally,
                                    verticalArrangement = Arrangement.Bottom,
                                    modifier = Modifier
                                        .weight(1f)
                                        .fillMaxHeight()
                                ) {
                                    Box(
                                        modifier = Modifier
                                            .height(68.dp)
                                            .fillMaxWidth(),
                                        contentAlignment = Alignment.BottomCenter
                                    ) {
                                        Box(
                                            modifier = Modifier
                                                .width(animatedBarWidth)
                                                .height(barHeightDp)
                                                .clip(RoundedCornerShape(topStart = 6.dp, topEnd = 6.dp, bottomStart = 2.dp, bottomEnd = 2.dp))
                                                .background(barColor)
                                        )
                                    }
                                    Spacer(modifier = Modifier.height(6.dp))
                                    Text(
                                        text = day,
                                        style = MaterialTheme.typography.labelSmall.copy(
                                            fontSize = 11.5.sp,
                                            fontWeight = if (isSelected || isCurrentDay) FontWeight.Bold else FontWeight.Normal
                                        ),
                                        color = if (isSelected || isCurrentDay) MaterialTheme.colorScheme.primary else MaterialTheme.colorScheme.onSurfaceVariant
                                    )
                                    Spacer(modifier = Modifier.height(2.dp))
                                    if (isCurrentDay) {
                                        Box(
                                            modifier = Modifier
                                                .size(4.dp)
                                                .clip(CircleShape)
                                                .background(MaterialTheme.colorScheme.primary)
                                        )
                                    } else {
                                        Spacer(modifier = Modifier.size(4.dp))
                                    }
                                }
                            }
                        }
                    }

                    Spacer(modifier = Modifier.height(14.dp))
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Text(
                            text = if (isEnglish)
                                "7d saved: ~${uiState.weeklySavedMinutes}m · 30d: ~${uiState.monthlySavedMinutes}m"
                            else
                                "7G kazanım: ~${uiState.weeklySavedMinutes} dk · 30G: ~${uiState.monthlySavedMinutes} dk",
                            style = MaterialTheme.typography.labelSmall.copy(
                                fontSize = 11.sp,
                                fontWeight = FontWeight.Medium
                            ),
                            color = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                        if (uiState.totalGatewayDismissCount > 0 || uiState.todayGatewayDismissCount > 0) {
                            Text(
                                text = if (isEnglish)
                                    "Gateway turns: ${uiState.todayGatewayDismissCount} today (${uiState.totalGatewayDismissCount} total)"
                                else
                                    "Kapıdan dönüş: bugün ${uiState.todayGatewayDismissCount} (toplam ${uiState.totalGatewayDismissCount})",
                                style = MaterialTheme.typography.labelSmall.copy(
                                    fontSize = 11.sp,
                                    fontWeight = FontWeight.SemiBold
                                ),
                                color = MaterialTheme.colorScheme.primary
                            )
                        }
                    }
                }
            }
        }

        // 3. KART 2: 7x24 Alışkanlık Isı Haritası
        item {
            Surface(
                modifier = Modifier
                    .fillMaxWidth()
                    .clip(RoundedCornerShape(NmrTheme.dimens.cardRadius)),
                shape = RoundedCornerShape(NmrTheme.dimens.cardRadius),
                color = MaterialTheme.colorScheme.surface,
                border = BorderStroke(0.5.dp, MaterialTheme.colorScheme.outline.copy(alpha = 0.5f)),
                tonalElevation = 0.dp
            ) {
                Column(modifier = Modifier.padding(18.dp)) {
                    Text(
                        text = strings.habitHeatmapTitle,
                        style = MaterialTheme.typography.titleMedium.copy(fontWeight = FontWeight.SemiBold),
                        color = MaterialTheme.colorScheme.onBackground
                    )
                    Spacer(modifier = Modifier.height(2.dp))
                    Text(
                        text = strings.habitHeatmapSub,
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )

                    Spacer(modifier = Modifier.height(16.dp))

                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Spacer(modifier = Modifier.width(28.dp))
                        Row(
                            modifier = Modifier.weight(1f),
                            horizontalArrangement = Arrangement.SpaceBetween
                        ) {
                            listOf("00", "06", "12", "18", "23").forEach { hr ->
                                Text(
                                    text = hr,
                                    style = MaterialTheme.typography.labelSmall.copy(
                                        fontSize = 10.sp,
                                        fontWeight = FontWeight.Normal
                                    ),
                                    color = MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.8f)
                                )
                            }
                        }
                    }
                    Spacer(modifier = Modifier.height(6.dp))

                    val primaryColor = MaterialTheme.colorScheme.primary
                    val outlineAlpha = MaterialTheme.colorScheme.outline.copy(alpha = 0.15f)
                    Column(
                        modifier = Modifier.fillMaxWidth(),
                        verticalArrangement = Arrangement.spacedBy(3.dp)
                    ) {
                        dayNames.forEachIndexed { dayIdx, dayName ->
                            Row(
                                modifier = Modifier.fillMaxWidth(),
                                verticalAlignment = Alignment.CenterVertically,
                                horizontalArrangement = Arrangement.spacedBy(2.dp)
                            ) {
                                Text(
                                    text = dayName.take(3),
                                    style = MaterialTheme.typography.labelSmall.copy(
                                        fontSize = 10.sp,
                                        fontWeight = if (dayIdx == currentDayIndex) FontWeight.Bold else FontWeight.Normal
                                    ),
                                    color = if (dayIdx == currentDayIndex) MaterialTheme.colorScheme.primary else MaterialTheme.colorScheme.onSurfaceVariant,
                                    modifier = Modifier.width(28.dp)
                                )
                                Canvas(
                                    modifier = Modifier
                                        .weight(1f)
                                        .height(9.dp)
                                ) {
                                    val spacing = 2.dp.toPx()
                                    val cellWidth = (size.width - spacing * 23) / 24f
                                    val cellHeight = size.height
                                    val corner = CornerRadius(2.dp.toPx())
                                    for (hour in 0..23) {
                                        val cellAttempts = if (matrix.size >= 168) matrix[dayIdx * 24 + hour] else 0
                                        val cellColor = if (cellAttempts == 0) {
                                            outlineAlpha
                                        } else {
                                            val intensity = (0.3f + (cellAttempts.toFloat() / maxCellAttempts.toFloat()) * 0.7f).coerceIn(0.3f, 1f)
                                            primaryColor.copy(alpha = intensity)
                                        }
                                        val left = hour * (cellWidth + spacing)
                                        drawRoundRect(
                                            color = cellColor,
                                            topLeft = Offset(left, 0f),
                                            size = Size(cellWidth, cellHeight),
                                            cornerRadius = corner
                                        )
                                    }
                                }
                            }
                        }
                    }

                    Spacer(modifier = Modifier.height(14.dp))

                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        if (totalAttempts == 0) {
                            Text(
                                text = strings.habitHeatmapEmpty,
                                style = MaterialTheme.typography.labelSmall.copy(fontSize = 11.sp),
                                color = MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.8f)
                            )
                        } else {
                            Text(
                                text = "${totalAttempts} ${if (isEnglish) "total checks" else "toplam yoklama"}",
                                style = MaterialTheme.typography.labelSmall.copy(fontSize = 11.sp),
                                color = MaterialTheme.colorScheme.onSurfaceVariant
                            )
                        }

                        Row(
                            verticalAlignment = Alignment.CenterVertically,
                            horizontalArrangement = Arrangement.spacedBy(4.dp)
                        ) {
                            Text(
                                text = strings.habitHeatmapLess,
                                style = MaterialTheme.typography.labelSmall.copy(fontSize = 10.sp),
                                color = MaterialTheme.colorScheme.onSurfaceVariant
                            )
                            Box(
                                modifier = Modifier
                                    .size(8.dp)
                                    .clip(RoundedCornerShape(2.dp))
                                    .background(MaterialTheme.colorScheme.outline.copy(alpha = 0.2f))
                            )
                            Box(
                                modifier = Modifier
                                    .size(8.dp)
                                    .clip(RoundedCornerShape(2.dp))
                                    .background(MaterialTheme.colorScheme.primary.copy(alpha = 0.45f))
                            )
                            Box(
                                modifier = Modifier
                                    .size(8.dp)
                                    .clip(RoundedCornerShape(2.dp))
                                    .background(MaterialTheme.colorScheme.primary)
                            )
                            Text(
                                text = strings.habitHeatmapMore,
                                style = MaterialTheme.typography.labelSmall.copy(fontSize = 10.sp),
                                color = MaterialTheme.colorScheme.onSurfaceVariant
                            )
                        }
                    }
                }
            }
        }

        // 4. Günlük Bütçe & Kota Durumu — SADECE KOTA MODU AKTİFSE
        if (isQuotaModeActive) {
            item {
                Surface(
                    modifier = Modifier
                        .fillMaxWidth()
                        .clip(RoundedCornerShape(NmrTheme.dimens.cardRadius)),
                    shape = RoundedCornerShape(NmrTheme.dimens.cardRadius),
                    color = MaterialTheme.colorScheme.surface,
                    border = BorderStroke(0.5.dp, MaterialTheme.colorScheme.outline.copy(alpha = 0.5f)),
                    tonalElevation = 0.dp
                ) {
                    Column(
                        modifier = Modifier.padding(18.dp)
                    ) {
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.SpaceBetween,
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Text(
                                text = strings.quotaBudgetTitle,
                                style = MaterialTheme.typography.labelSmall.copy(
                                    letterSpacing = 1.2.sp,
                                    fontWeight = FontWeight.SemiBold
                                ),
                                color = MaterialTheme.colorScheme.onSurfaceVariant
                            )
                            Surface(
                                shape = CircleShape,
                                color = NmrTheme.colors.selectedSurface,
                                border = BorderStroke(0.5.dp, MaterialTheme.colorScheme.primary.copy(alpha = 0.25f))
                            ) {
                                Text(
                                    text = strings.activeBadge,
                                    style = MaterialTheme.typography.labelSmall.copy(fontWeight = FontWeight.Medium),
                                    color = MaterialTheme.colorScheme.primary,
                                    modifier = Modifier.padding(horizontal = 10.dp, vertical = 4.dp)
                                )
                            }
                        }

                        Spacer(modifier = Modifier.height(14.dp))

                        val isTimeQuota = generalMode == DmReelMode.TIME_QUOTA

                        // Sayaç 1: Günlük Reel Adedi
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.SpaceBetween,
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Row(
                                verticalAlignment = Alignment.CenterVertically,
                                horizontalArrangement = Arrangement.spacedBy(6.dp)
                            ) {
                                Text(
                                    text = strings.todayWatchedReels,
                                    style = MaterialTheme.typography.bodyMedium.copy(fontWeight = FontWeight.Medium),
                                    color = MaterialTheme.colorScheme.onBackground
                                )
                                Surface(
                                    shape = RoundedCornerShape(4.dp),
                                    color = if (!isTimeQuota) MaterialTheme.colorScheme.primary.copy(alpha = 0.15f) else MaterialTheme.colorScheme.outline.copy(alpha = 0.15f)
                                ) {
                                    Text(
                                        text = if (!isTimeQuota) (if (isEnglish) "ACTIVE RULE" else "AKTİF LİMİT") else (if (isEnglish) "TRACKING" else "BİLGİ"),
                                        style = MaterialTheme.typography.labelSmall.copy(fontSize = 9.sp, fontWeight = FontWeight.Bold),
                                        color = if (!isTimeQuota) MaterialTheme.colorScheme.primary else MaterialTheme.colorScheme.onSurfaceVariant,
                                        modifier = Modifier.padding(horizontal = 5.dp, vertical = 2.dp)
                                    )
                                }
                            }
                            Text(
                                text = "$todayReelCount / $dailyCountLimit",
                                style = MaterialTheme.typography.titleSmall.copy(
                                    fontWeight = FontWeight.SemiBold,
                                    fontSize = 17.sp
                                ),
                                color = MaterialTheme.colorScheme.onBackground
                            )
                        }
                        Spacer(modifier = Modifier.height(6.dp))
                        LinearProgressIndicator(
                            progress = { (todayReelCount.toFloat() / maxOf(1, dailyCountLimit).toFloat()).coerceIn(0f, 1f) },
                            modifier = Modifier
                                .fillMaxWidth()
                                .height(6.dp)
                                .clip(RoundedCornerShape(3.dp)),
                            color = if (!isTimeQuota) MaterialTheme.colorScheme.primary else MaterialTheme.colorScheme.primary.copy(alpha = 0.5f),
                            trackColor = MaterialTheme.colorScheme.outline.copy(alpha = 0.4f)
                        )

                        Spacer(modifier = Modifier.height(14.dp))

                        // Sayaç 2: Günlük Süre
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.SpaceBetween,
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Row(
                                verticalAlignment = Alignment.CenterVertically,
                                horizontalArrangement = Arrangement.spacedBy(6.dp)
                            ) {
                                Text(
                                    text = strings.todayTimeSpent,
                                    style = MaterialTheme.typography.bodyMedium.copy(fontWeight = FontWeight.Medium),
                                    color = MaterialTheme.colorScheme.onBackground
                                )
                                Surface(
                                    shape = RoundedCornerShape(4.dp),
                                    color = if (isTimeQuota) MaterialTheme.colorScheme.primary.copy(alpha = 0.15f) else MaterialTheme.colorScheme.outline.copy(alpha = 0.15f)
                                ) {
                                    Text(
                                        text = if (isTimeQuota) (if (isEnglish) "ACTIVE RULE" else "AKTİF LİMİT") else (if (isEnglish) "TRACKING" else "BİLGİ"),
                                        style = MaterialTheme.typography.labelSmall.copy(fontSize = 9.sp, fontWeight = FontWeight.Bold),
                                        color = if (isTimeQuota) MaterialTheme.colorScheme.primary else MaterialTheme.colorScheme.onSurfaceVariant,
                                        modifier = Modifier.padding(horizontal = 5.dp, vertical = 2.dp)
                                    )
                                }
                            }
                            Text(
                                text = "$todayMinutesSpent / $dailyTimeLimitMinutes ${strings.quotaSuffixMinutes}",
                                style = MaterialTheme.typography.titleSmall.copy(
                                    fontWeight = FontWeight.SemiBold,
                                    fontSize = 17.sp
                                ),
                                color = MaterialTheme.colorScheme.onBackground
                            )
                        }
                        Spacer(modifier = Modifier.height(6.dp))
                        LinearProgressIndicator(
                            progress = { (todayMinutesSpent.toFloat() / maxOf(1, dailyTimeLimitMinutes).toFloat()).coerceIn(0f, 1f) },
                            modifier = Modifier
                                .fillMaxWidth()
                                .height(6.dp)
                                .clip(RoundedCornerShape(3.dp)),
                            color = if (isTimeQuota) MaterialTheme.colorScheme.primary else MaterialTheme.colorScheme.primary.copy(alpha = 0.5f),
                            trackColor = MaterialTheme.colorScheme.outline.copy(alpha = 0.4f)
                        )

                        Spacer(modifier = Modifier.height(14.dp))

                        val isReached = if (isTimeQuota) (todayMinutesSpent >= dailyTimeLimitMinutes) else (todayReelCount >= dailyCountLimit)
                        if (isReached) {
                            Surface(
                                modifier = Modifier.fillMaxWidth(),
                                shape = RoundedCornerShape(12.dp),
                                color = NmrTheme.colors.selectedSurface
                            ) {
                                Text(
                                    text = strings.quotaReachedSerenity,
                                    style = MaterialTheme.typography.bodyMedium.copy(fontWeight = FontWeight.Medium),
                                    color = MaterialTheme.colorScheme.primary,
                                    modifier = Modifier.padding(12.dp)
                                )
                            }
                        } else {
                            Text(
                                text = strings.quotaUnderBudget,
                                style = MaterialTheme.typography.bodySmall,
                                color = MaterialTheme.colorScheme.onSurfaceVariant
                            )
                        }
                    }
                }
            }
        }

        // 5. DESTEK KARTI
        if (!isSupporter) {
            item {
                Surface(
                    shape = RoundedCornerShape(16.dp),
                    color = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.5f),
                    border = BorderStroke(1.dp, MaterialTheme.colorScheme.outlineVariant.copy(alpha = 0.4f)),
                    modifier = Modifier
                        .fillMaxWidth()
                        .clickable(onClick = onNavigateToSupport)
                ) {
                    Row(
                        modifier = Modifier.padding(16.dp),
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.spacedBy(12.dp)
                    ) {
                        Text(text = "☕", fontSize = 24.sp)
                        Column(modifier = Modifier.weight(1f)) {
                            Text(
                                text = if (isEnglish)
                                    "Outloop making a difference?"
                                else
                                    "Outloop hayatında fark yarattı mı?",
                                style = MaterialTheme.typography.titleSmall.copy(fontWeight = FontWeight.SemiBold),
                                color = MaterialTheme.colorScheme.onBackground
                            )
                            Text(
                                text = if (isEnglish)
                                    "Support independent, ad-free development."
                                else
                                    "Bağımsız ve reklamsız geliştirmeyi destekle.",
                                style = MaterialTheme.typography.bodySmall,
                                color = MaterialTheme.colorScheme.onSurfaceVariant
                            )
                        }
                        Icon(
                            imageVector = Icons.Outlined.ChevronRight,
                            contentDescription = null,
                            tint = MaterialTheme.colorScheme.onSurfaceVariant,
                            modifier = Modifier.size(20.dp)
                        )
                    }
                }
                Spacer(modifier = Modifier.height(16.dp))
            }
        }
    }
}

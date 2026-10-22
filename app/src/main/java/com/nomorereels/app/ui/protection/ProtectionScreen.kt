package com.nomorereels.app.ui.protection

import androidx.compose.animation.AnimatedContent
import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.animateColorAsState
import androidx.compose.animation.core.FastOutLinearInEasing
import androidx.compose.animation.core.FastOutSlowInEasing
import androidx.compose.animation.core.LinearOutSlowInEasing
import androidx.compose.animation.core.Spring
import androidx.compose.animation.core.animateDpAsState
import androidx.compose.animation.core.spring
import androidx.compose.animation.core.tween
import androidx.compose.animation.expandVertically
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.animation.scaleIn
import androidx.compose.animation.shrinkVertically
import androidx.compose.animation.togetherWith
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.horizontalScroll
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.outlined.ChevronRight
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.hapticfeedback.HapticFeedbackType
import androidx.compose.ui.platform.LocalHapticFeedback
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.nomorereels.app.policy.DmReelMode
import com.nomorereels.app.policy.ProtectionLevel
import com.nomorereels.app.ui.i18n.EnglishStrings
import com.nomorereels.app.ui.i18n.LocalAppStrings
import com.nomorereels.app.ui.theme.NmrChip
import com.nomorereels.app.ui.theme.NmrIcons
import com.nomorereels.app.ui.theme.NmrTheme

@Composable
fun ProtectionScreen(
    viewModel: ProtectionViewModel,
    onSelectRegion: (String) -> Unit,
    onOpenCustomCount: () -> Unit,
    onOpenCustomTime: () -> Unit,
    modifier: Modifier = Modifier
) {
    val uiState by viewModel.uiState.collectAsStateWithLifecycle()

    ProtectionScreenContent(
        uiState = uiState,
        onGeneralModeChange = { viewModel.setGeneralMode(it) },
        onApplyProfile = { viewModel.applyProtectionProfile(it) },
        onSelectRegion = onSelectRegion,
        onCountLimitChange = { viewModel.setDailyCountLimit(it) },
        onTimeLimitChange = { viewModel.setDailyTimeLimitMinutes(it) },
        onOpenCustomCount = onOpenCustomCount,
        onOpenCustomTime = onOpenCustomTime,
        modifier = modifier
    )
}

@Composable
fun ProtectionScreenContent(
    uiState: ProtectionUiState,
    onGeneralModeChange: (DmReelMode) -> Unit,
    onApplyProfile: (ProtectionLevel) -> Unit,
    onSelectRegion: (String) -> Unit,
    onCountLimitChange: (Int) -> Unit,
    onTimeLimitChange: (Int) -> Unit,
    onOpenCustomCount: () -> Unit,
    onOpenCustomTime: () -> Unit,
    modifier: Modifier = Modifier
) {
    val strings = LocalAppStrings.current
    val generalMode = uiState.generalMode
    val dmMode = uiState.dmMode
    val exploreMode = uiState.exploreMode
    val feedMode = uiState.feedMode
    val profileMode = uiState.profileMode
    val reelsTabMode = uiState.reelsTabMode
    val dailyCountLimit = uiState.dailyCountLimit
    val dailyTimeLimitMinutes = uiState.dailyTimeLimitMinutes

    LazyColumn(
        modifier = modifier
            .fillMaxSize()
            .padding(horizontal = NmrTheme.dimens.screenHorizontalMargin),
        verticalArrangement = Arrangement.spacedBy(16.dp)
    ) {
        // 1. Ekran Başlığı (Header)
        item {
            Spacer(modifier = Modifier.height(10.dp))
            Column {
                Text(
                    text = if (strings is EnglishStrings)
                        "PROTECTION CENTER"
                    else
                        "KORUMA MERKEZİ",
                    style = MaterialTheme.typography.labelSmall.copy(
                        letterSpacing = 1.6.sp,
                        fontWeight = FontWeight.Medium
                    ),
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
                Spacer(modifier = Modifier.height(4.dp))
                Text(
                    text = strings.protectionCenterTagline,
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
                    text = strings.protectionCenterSubtext,
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
            }
        }

        // 2. Üst Bölüm: Genel Koruma Modu (2x2 Grid)
        item {
            Column {
                Column {
                    Text(
                        text = strings.generalPolicyHeader,
                        style = MaterialTheme.typography.labelSmall.copy(
                            letterSpacing = 1.2.sp,
                            fontWeight = FontWeight.SemiBold
                        ),
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                    Spacer(modifier = Modifier.height(2.dp))
                    Text(
                        text = strings.generalPolicySubtext,
                        style = MaterialTheme.typography.bodySmall.copy(fontSize = 11.5.sp),
                        color = MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.8f)
                    )
                }
                Spacer(modifier = Modifier.height(10.dp))

                // Satır 1: Komple Yasak & Sadece 1 Tane
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(10.dp)
                ) {
                    ProtectionCenterModeCard(
                        title = strings.blockAllTitle,
                        desc = strings.blockAllSub,
                        icon = NmrIcons.KompleYasak,
                        selected = generalMode == DmReelMode.BLOCK_ALL,
                        onClick = { onGeneralModeChange(DmReelMode.BLOCK_ALL) },
                        modifier = Modifier.weight(1f)
                    )
                    ProtectionCenterModeCard(
                        title = strings.singleReelTitle,
                        desc = strings.singleReelSub,
                        icon = NmrIcons.SadeceBirTane,
                        selected = generalMode == DmReelMode.SINGLE_REEL,
                        onClick = { onGeneralModeChange(DmReelMode.SINGLE_REEL) },
                        modifier = Modifier.weight(1f)
                    )
                }
                Spacer(modifier = Modifier.height(10.dp))

                // Satır 2: X Tane & X Süre
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(10.dp)
                ) {
                    ProtectionCenterModeCard(
                        title = strings.countQuotaTitle,
                        desc = strings.countQuotaSub,
                        icon = NmrIcons.XTane,
                        selected = generalMode == DmReelMode.COUNT_QUOTA,
                        onClick = { onGeneralModeChange(DmReelMode.COUNT_QUOTA) },
                        modifier = Modifier.weight(1f)
                    )
                    ProtectionCenterModeCard(
                        title = strings.timeQuotaTitle,
                        desc = strings.timeQuotaSub,
                        icon = NmrIcons.XDakika,
                        selected = generalMode == DmReelMode.TIME_QUOTA,
                        onClick = { onGeneralModeChange(DmReelMode.TIME_QUOTA) },
                        modifier = Modifier.weight(1f)
                    )
                }

                // Eğer Kota Modu seçiliyse bütçe sınırlarını belirleme alanı
                AnimatedVisibility(
                    visible = generalMode == DmReelMode.COUNT_QUOTA || generalMode == DmReelMode.TIME_QUOTA,
                    enter = fadeIn(animationSpec = tween(220, easing = LinearOutSlowInEasing)) +
                        expandVertically(
                            animationSpec = spring(
                                dampingRatio = Spring.DampingRatioNoBouncy,
                                stiffness = Spring.StiffnessMediumLow
                            ),
                            expandFrom = Alignment.Top
                        ),
                    exit = fadeOut(animationSpec = tween(140, easing = FastOutLinearInEasing)) +
                        shrinkVertically(
                            animationSpec = spring(
                                dampingRatio = Spring.DampingRatioNoBouncy,
                                stiffness = Spring.StiffnessMediumLow
                            ),
                            shrinkTowards = Alignment.Top
                        )
                ) {
                    Column {
                        Spacer(modifier = Modifier.height(10.dp))
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
                                modifier = Modifier.padding(16.dp),
                                verticalArrangement = Arrangement.spacedBy(14.dp)
                            ) {
                                AnimatedContent(
                                    targetState = generalMode == DmReelMode.TIME_QUOTA,
                                    transitionSpec = {
                                        fadeIn(animationSpec = tween(180, easing = LinearOutSlowInEasing)) togetherWith
                                            fadeOut(animationSpec = tween(120, easing = FastOutLinearInEasing))
                                    },
                                    label = "QuotaTypeContentTransition"
                                ) { isTimeQuota ->
                                    if (isTimeQuota) {
                                        // Toplam Süre Sınırı
                                        Column(verticalArrangement = Arrangement.spacedBy(6.dp)) {
                                            Row(
                                                modifier = Modifier.fillMaxWidth(),
                                                horizontalArrangement = Arrangement.SpaceBetween,
                                                verticalAlignment = Alignment.CenterVertically
                                            ) {
                                                Text(
                                                    text = strings.dailyTimeLimitHeader,
                                                    style = MaterialTheme.typography.labelSmall.copy(fontWeight = FontWeight.Medium),
                                                    color = MaterialTheme.colorScheme.onSurfaceVariant
                                                )
                                                Text(
                                                    text = "$dailyTimeLimitMinutes ${strings.quotaSuffixMinutes}",
                                                    style = MaterialTheme.typography.labelMedium.copy(fontWeight = FontWeight.SemiBold),
                                                    color = MaterialTheme.colorScheme.primary
                                                )
                                            }
                                            Row(
                                                modifier = Modifier
                                                    .fillMaxWidth()
                                                    .horizontalScroll(rememberScrollState()),
                                                horizontalArrangement = Arrangement.spacedBy(8.dp)
                                            ) {
                                                listOf(5, 15, 30, 60).forEach { mins ->
                                                    NmrChip(
                                                        label = "$mins ${strings.quotaSuffixMinutes}",
                                                        selected = dailyTimeLimitMinutes == mins,
                                                        onClick = { onTimeLimitChange(mins) }
                                                    )
                                                }
                                                val isTimeCustom = dailyTimeLimitMinutes !in listOf(5, 15, 30, 60)
                                                NmrChip(
                                                    label = if (isTimeCustom) "$dailyTimeLimitMinutes ${strings.quotaSuffixMinutes}" else strings.customInput,
                                                    selected = isTimeCustom,
                                                    onClick = onOpenCustomTime
                                                )
                                            }
                                        }
                                    } else {
                                        // Video Sayısı Sınırı
                                        Column(verticalArrangement = Arrangement.spacedBy(6.dp)) {
                                            Row(
                                                modifier = Modifier.fillMaxWidth(),
                                                horizontalArrangement = Arrangement.SpaceBetween,
                                                verticalAlignment = Alignment.CenterVertically
                                            ) {
                                                Text(
                                                    text = strings.dailyReelLimitHeader,
                                                    style = MaterialTheme.typography.labelSmall.copy(fontWeight = FontWeight.Medium),
                                                    color = MaterialTheme.colorScheme.onSurfaceVariant
                                                )
                                                Text(
                                                    text = "$dailyCountLimit ${strings.quotaSuffixReels}",
                                                    style = MaterialTheme.typography.labelMedium.copy(fontWeight = FontWeight.SemiBold),
                                                    color = MaterialTheme.colorScheme.primary
                                                )
                                            }
                                            Row(
                                                modifier = Modifier
                                                    .fillMaxWidth()
                                                    .horizontalScroll(rememberScrollState()),
                                                horizontalArrangement = Arrangement.spacedBy(8.dp)
                                            ) {
                                                listOf(3, 5, 10, 20, 50, 100).forEach { limit ->
                                                    NmrChip(
                                                        label = "$limit",
                                                        selected = dailyCountLimit == limit,
                                                        onClick = { onCountLimitChange(limit) }
                                                    )
                                                }
                                                val isCustom = dailyCountLimit !in listOf(3, 5, 10, 20, 50, 100)
                                                NmrChip(
                                                    label = if (isCustom) "$dailyCountLimit" else strings.customInput,
                                                    selected = isCustom,
                                                    onClick = onOpenCustomCount
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

        // 3. Orta Bölüm: Instagram Yüzeyleri (5 Bölge)
        item {
            Column(verticalArrangement = Arrangement.spacedBy(10.dp)) {
                Column {
                    Text(
                        text = strings.surfacesHeader,
                        style = MaterialTheme.typography.labelSmall.copy(
                            letterSpacing = 1.2.sp,
                            fontWeight = FontWeight.SemiBold
                        ),
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                    Spacer(modifier = Modifier.height(2.dp))
                    Text(
                        text = strings.surfacesSubtext,
                        style = MaterialTheme.typography.bodySmall.copy(fontSize = 11.5.sp),
                        color = MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.8f)
                    )
                }

                Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                    RegionSettingRow(
                        title = strings.dmTitle,
                        subtitle = strings.dmSub,
                        icon = NmrIcons.Dm,
                        modeLabel = strings.modeTitle(dmMode),
                        onClick = { onSelectRegion("DM") }
                    )
                    RegionSettingRow(
                        title = strings.exploreTitle,
                        subtitle = strings.exploreSub,
                        icon = NmrIcons.Kesfet,
                        modeLabel = strings.modeTitle(exploreMode),
                        onClick = { onSelectRegion("EXPLORE") }
                    )
                    RegionSettingRow(
                        title = strings.feedTitle,
                        subtitle = strings.feedSub,
                        icon = NmrIcons.AnaSayfa,
                        modeLabel = strings.modeTitle(feedMode),
                        onClick = { onSelectRegion("FEED") }
                    )
                    RegionSettingRow(
                        title = strings.profileTitle,
                        subtitle = strings.profileSub,
                        icon = NmrIcons.Profil,
                        modeLabel = strings.modeTitle(profileMode),
                        onClick = { onSelectRegion("PROFILE") }
                    )
                    RegionSettingRow(
                        title = strings.reelsTabTitle,
                        subtitle = strings.reelsTabSub,
                        icon = NmrIcons.ReelsSekmesi,
                        modeLabel = strings.modeTitle(reelsTabMode),
                        onClick = { onSelectRegion("REELS_TAB") }
                    )
                }
            }
        }

        item {
            Spacer(modifier = Modifier.height(20.dp))
        }
    }
}

@Composable
fun ProtectionCenterModeCard(
    title: String,
    desc: String,
    icon: ImageVector,
    selected: Boolean,
    onClick: () -> Unit,
    modifier: Modifier = Modifier
) {
    val haptic = LocalHapticFeedback.current
    val cardBg by animateColorAsState(
        targetValue = if (selected) NmrTheme.colors.selectedSurface else MaterialTheme.colorScheme.surface,
        animationSpec = tween(durationMillis = 200, easing = FastOutSlowInEasing),
        label = "modeCardBg"
    )
    val cardBorderColor by animateColorAsState(
        targetValue = if (selected) MaterialTheme.colorScheme.primary else MaterialTheme.colorScheme.outline.copy(alpha = 0.5f),
        animationSpec = tween(durationMillis = 200, easing = FastOutSlowInEasing),
        label = "modeCardBorderColor"
    )
    val cardBorderWidth by animateDpAsState(
        targetValue = if (selected) 1.5.dp else 0.5.dp,
        animationSpec = tween(durationMillis = 200, easing = FastOutSlowInEasing),
        label = "modeCardBorderWidth"
    )

    Surface(
        modifier = modifier
            .fillMaxWidth()
            .heightIn(min = 120.dp)
            .clip(RoundedCornerShape(20.dp))
            .clickable {
                haptic.performHapticFeedback(HapticFeedbackType.TextHandleMove)
                onClick()
            },
        shape = RoundedCornerShape(20.dp),
        color = cardBg,
        border = BorderStroke(cardBorderWidth, cardBorderColor),
        tonalElevation = 0.dp
    ) {
        Column(
            modifier = Modifier
                .padding(14.dp)
                .fillMaxWidth(),
            verticalArrangement = Arrangement.SpaceBetween
        ) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                val iconBg by animateColorAsState(
                    targetValue = if (selected) MaterialTheme.colorScheme.primary.copy(alpha = 0.15f)
                    else MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.6f),
                    animationSpec = tween(200),
                    label = "iconBg"
                )
                val iconTint by animateColorAsState(
                    targetValue = if (selected) MaterialTheme.colorScheme.primary else MaterialTheme.colorScheme.onSurfaceVariant,
                    animationSpec = tween(200),
                    label = "iconTint"
                )
                Box(
                    modifier = Modifier
                        .size(36.dp)
                        .clip(CircleShape)
                        .background(iconBg),
                    contentAlignment = Alignment.Center
                ) {
                    Icon(
                        imageVector = icon,
                        contentDescription = null,
                        tint = iconTint,
                        modifier = Modifier.size(20.dp)
                    )
                }
                AnimatedVisibility(
                    visible = selected,
                    enter = fadeIn(tween(160)) + scaleIn(initialScale = 0.5f, animationSpec = tween(160)),
                    exit = fadeOut(tween(100))
                ) {
                    Box(
                        modifier = Modifier
                            .size(9.dp)
                            .clip(CircleShape)
                            .background(MaterialTheme.colorScheme.primary)
                    )
                }
            }
            Spacer(modifier = Modifier.height(10.dp))
            Text(
                text = title,
                style = MaterialTheme.typography.titleSmall.copy(fontWeight = FontWeight.SemiBold),
                color = MaterialTheme.colorScheme.onBackground
            )
            Spacer(modifier = Modifier.height(3.dp))
            Text(
                text = desc,
                style = MaterialTheme.typography.bodySmall.copy(fontSize = 11.5.sp, lineHeight = 15.sp),
                color = MaterialTheme.colorScheme.onSurfaceVariant,
                maxLines = 2
            )
        }
    }
}

@Composable
fun QuotaModeSegmentedSwitcher(
    selectedMode: DmReelMode,
    onModeSelect: (DmReelMode) -> Unit,
    modifier: Modifier = Modifier
) {
    val haptic = LocalHapticFeedback.current
    val strings = LocalAppStrings.current
    val isTime = selectedMode == DmReelMode.TIME_QUOTA

    Surface(
        modifier = modifier.fillMaxWidth(),
        shape = RoundedCornerShape(12.dp),
        color = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.45f),
        border = BorderStroke(0.5.dp, MaterialTheme.colorScheme.outline.copy(alpha = 0.25f))
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(3.dp),
            horizontalArrangement = Arrangement.spacedBy(4.dp)
        ) {
            // Tab 1: Reel Sayısı
            val countSelected = !isTime
            val countBg by animateColorAsState(
                targetValue = if (countSelected) MaterialTheme.colorScheme.primary else Color.Transparent,
                animationSpec = tween(durationMillis = 180, easing = FastOutSlowInEasing),
                label = "CountTabBg"
            )
            val countContentColor by animateColorAsState(
                targetValue = if (countSelected) MaterialTheme.colorScheme.onPrimary else MaterialTheme.colorScheme.onSurfaceVariant,
                animationSpec = tween(durationMillis = 180, easing = FastOutSlowInEasing),
                label = "CountTabContent"
            )

            Box(
                modifier = Modifier
                    .weight(1f)
                    .clip(RoundedCornerShape(9.dp))
                    .background(countBg)
                    .clickable {
                        if (isTime) {
                            haptic.performHapticFeedback(HapticFeedbackType.LongPress)
                            onModeSelect(DmReelMode.COUNT_QUOTA)
                        }
                    }
                    .padding(vertical = 9.dp),
                contentAlignment = Alignment.Center
            ) {
                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(6.dp)
                ) {
                    Icon(
                        imageVector = NmrIcons.XTane,
                        contentDescription = null,
                        tint = countContentColor,
                        modifier = Modifier.size(15.dp)
                    )
                    Text(
                        text = strings.quotaTypeReelCount,
                        style = MaterialTheme.typography.labelMedium.copy(
                            fontWeight = if (countSelected) FontWeight.SemiBold else FontWeight.Medium
                        ),
                        color = countContentColor
                    )
                }
            }

            // Tab 2: Toplam Süre
            val timeSelected = isTime
            val timeBg by animateColorAsState(
                targetValue = if (timeSelected) MaterialTheme.colorScheme.primary else Color.Transparent,
                animationSpec = tween(durationMillis = 180, easing = FastOutSlowInEasing),
                label = "TimeTabBg"
            )
            val timeContentColor by animateColorAsState(
                targetValue = if (timeSelected) MaterialTheme.colorScheme.onPrimary else MaterialTheme.colorScheme.onSurfaceVariant,
                animationSpec = tween(durationMillis = 180, easing = FastOutSlowInEasing),
                label = "TimeTabContent"
            )

            Box(
                modifier = Modifier
                    .weight(1f)
                    .clip(RoundedCornerShape(9.dp))
                    .background(timeBg)
                    .clickable {
                        if (!isTime) {
                            haptic.performHapticFeedback(HapticFeedbackType.LongPress)
                            onModeSelect(DmReelMode.TIME_QUOTA)
                        }
                    }
                    .padding(vertical = 9.dp),
                contentAlignment = Alignment.Center
            ) {
                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(6.dp)
                ) {
                    Icon(
                        imageVector = NmrIcons.XDakika,
                        contentDescription = null,
                        tint = timeContentColor,
                        modifier = Modifier.size(15.dp)
                    )
                    Text(
                        text = strings.quotaTypeTotalTime,
                        style = MaterialTheme.typography.labelMedium.copy(
                            fontWeight = if (timeSelected) FontWeight.SemiBold else FontWeight.Medium
                        ),
                        color = timeContentColor
                    )
                }
            }
        }
    }
}

@Composable
fun RegionSettingRow(
    title: String,
    subtitle: String,
    icon: ImageVector,
    modeLabel: String,
    onClick: () -> Unit
) {
    Surface(
        modifier = Modifier
            .fillMaxWidth()
            .clip(RoundedCornerShape(NmrTheme.dimens.cardRadius))
            .clickable(onClick = onClick),
        shape = RoundedCornerShape(NmrTheme.dimens.cardRadius),
        color = MaterialTheme.colorScheme.surface,
        border = BorderStroke(0.5.dp, MaterialTheme.colorScheme.outline.copy(alpha = 0.5f)),
        tonalElevation = 0.dp
    ) {
        Row(
            modifier = Modifier.padding(horizontal = 16.dp, vertical = 14.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            Box(
                modifier = Modifier
                    .size(42.dp)
                    .clip(CircleShape)
                    .background(NmrTheme.colors.selectedSurface),
                contentAlignment = Alignment.Center
            ) {
                Icon(
                    imageVector = icon,
                    contentDescription = null,
                    tint = MaterialTheme.colorScheme.primary,
                    modifier = Modifier.size(22.dp)
                )
            }
            Spacer(modifier = Modifier.width(14.dp))
            Column(modifier = Modifier.weight(1f)) {
                Text(
                    text = title,
                    style = MaterialTheme.typography.titleMedium.copy(fontWeight = FontWeight.SemiBold),
                    color = MaterialTheme.colorScheme.onBackground
                )
                Spacer(modifier = Modifier.height(2.dp))
                Text(
                    text = subtitle,
                    style = MaterialTheme.typography.bodyMedium,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
            }
            Spacer(modifier = Modifier.width(10.dp))
            Surface(
                shape = CircleShape,
                color = NmrTheme.colors.selectedSurface,
                border = BorderStroke(0.5.dp, MaterialTheme.colorScheme.primary.copy(alpha = 0.25f)),
                tonalElevation = 0.dp
            ) {
                Text(
                    text = modeLabel,
                    style = MaterialTheme.typography.labelSmall.copy(fontWeight = FontWeight.Medium),
                    color = MaterialTheme.colorScheme.primary,
                    modifier = Modifier.padding(horizontal = 12.dp, vertical = 6.dp)
                )
            }
            Spacer(modifier = Modifier.width(4.dp))
            Icon(
                imageVector = Icons.Outlined.ChevronRight,
                contentDescription = null,
                tint = MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.5f),
                modifier = Modifier.size(18.dp)
            )
        }
    }
}

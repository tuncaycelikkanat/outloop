package com.nomorereels.app.ui.onboarding

import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.background
import androidx.compose.foundation.border
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
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.outlined.Check
import androidx.compose.material.icons.outlined.Close
import androidx.compose.material.icons.outlined.Security
import androidx.compose.material.icons.outlined.WifiOff
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.nomorereels.app.policy.ProtectionLevel
import com.nomorereels.app.ui.i18n.LocalAppStrings
import com.nomorereels.app.ui.theme.NmrFilledButton
import com.nomorereels.app.ui.theme.NmrHairlineDivider
import com.nomorereels.app.ui.theme.NmrIcons
import com.nomorereels.app.ui.theme.NmrLanguage
import com.nomorereels.app.ui.theme.NmrSectionHeader
import com.nomorereels.app.ui.theme.NmrSegmentedControl
import com.nomorereels.app.ui.theme.NmrSelectableRow
import com.nomorereels.app.ui.theme.NmrStatusChip
import com.nomorereels.app.ui.theme.NmrTheme

@Composable
fun OnboardingFlow(
    step: Int,
    language: NmrLanguage,
    isServiceActive: Boolean,
    isReviewMode: Boolean = false,
    onCloseReview: () -> Unit = {},
    onLanguageChange: (NmrLanguage) -> Unit,
    onNextStep: () -> Unit,
    onPreviousStep: () -> Unit,
    onOpenAccessibilitySettings: () -> Unit,
    onSelectProfile: (ProtectionLevel) -> Unit,
    modifier: Modifier = Modifier
) {
    val strings = LocalAppStrings.current
    Column(
        modifier = modifier
            .fillMaxSize()
            .padding(horizontal = NmrTheme.dimens.screenHorizontalMargin, vertical = 14.dp)
    ) {
        // Üst Bar: 4 Segmentli İlerleme Çubuğu ve Sağ Üstte Dil / Kapat Butonu
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(top = 4.dp, bottom = 12.dp),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically
        ) {
            // 4 Segmentli İnce İlerleme Çubuğu
            Row(
                modifier = Modifier.weight(1f).padding(end = 16.dp),
                horizontalArrangement = Arrangement.spacedBy(NmrTheme.dimens.gridUnit)
            ) {
                repeat(4) { index ->
                    Box(
                        modifier = Modifier
                            .weight(1f)
                            .height(3.dp)
                            .clip(RoundedCornerShape(2.dp))
                            .background(
                                if (index <= step) MaterialTheme.colorScheme.primary else MaterialTheme.colorScheme.outline
                            )
                    )
                }
            }

            // Sağ Üst: Dil Seçimi ve (İnceleme Modundaysa) Kapat Butonu
            Row(
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                NmrSegmentedControl(
                    options = listOf(NmrLanguage.TR, NmrLanguage.EN),
                    selectedOption = language,
                    onOptionSelected = onLanguageChange,
                    labelProvider = { it.code }
                )

                if (isReviewMode) {
                    IconButton(
                        onClick = onCloseReview,
                        modifier = Modifier.size(36.dp)
                    ) {
                        Icon(
                            imageVector = Icons.Outlined.Close,
                            contentDescription = strings.closeButton,
                            tint = MaterialTheme.colorScheme.onBackground
                        )
                    }
                }
            }
        }

        // Adım İçeriği — Kaydırılabilir Alan
        Box(
            modifier = Modifier
                .weight(1f)
                .fillMaxWidth()
                .verticalScroll(rememberScrollState())
        ) {
            when (step) {
                0 -> OnboardingStepIntro()
                1 -> OnboardingStepPrivacy()
                2 -> OnboardingStepAccessibility(isServiceActive = isServiceActive)
                3 -> OnboardingStepProfile(onSelectProfile = onSelectProfile)
            }
        }

        // Alt Butonlar — Sabit Alt Panel
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .padding(top = 8.dp),
            horizontalAlignment = Alignment.CenterHorizontally,
            verticalArrangement = Arrangement.spacedBy(6.dp)
        ) {
            NmrFilledButton(
                text = when (step) {
                    0, 1 -> strings.nextButton
                    2 -> if (isServiceActive || isReviewMode) strings.nextButton else strings.openSettingsButton
                    else -> if (isReviewMode) strings.closeButton else strings.startButton
                },
                onClick = {
                    if (step == 2 && !isServiceActive && !isReviewMode) {
                        onOpenAccessibilitySettings()
                    } else {
                        onNextStep()
                    }
                }
            )

            if (step > 0) {
                TextButton(
                    onClick = onPreviousStep,
                    modifier = Modifier.heightIn(min = NmrTheme.dimens.minTouchTarget)
                ) {
                    Text(
                        text = strings.backButton,
                        style = MaterialTheme.typography.titleMedium,
                        color = MaterialTheme.colorScheme.onBackground
                    )
                }
            } else {
                Spacer(modifier = Modifier.height(NmrTheme.dimens.minTouchTarget))
            }
        }
    }
}

// Ekran 0 — Onboarding 1/4 (Uygulama Tanıtımı)
@Composable
fun OnboardingStepIntro() {
    val strings = LocalAppStrings.current
    Column(
        modifier = Modifier.fillMaxWidth(),
        verticalArrangement = Arrangement.spacedBy(10.dp)
    ) {
        Spacer(modifier = Modifier.height(8.dp))
        Box(
            modifier = Modifier
                .size(52.dp)
                .clip(RoundedCornerShape(16.dp))
                .background(NmrTheme.colors.selectedSurface),
            contentAlignment = Alignment.Center
        ) {
            Icon(
                imageVector = NmrIcons.KorumaNav,
                contentDescription = null,
                tint = MaterialTheme.colorScheme.primary,
                modifier = Modifier.size(28.dp)
            )
        }

        Spacer(modifier = Modifier.height(4.dp))
        Text(
            text = strings.introHeroTitle,
            style = MaterialTheme.typography.headlineMedium.copy(
                fontWeight = FontWeight.Bold,
                fontSize = 24.sp,
                lineHeight = 32.sp
            ),
            color = MaterialTheme.colorScheme.onBackground
        )

        Spacer(modifier = Modifier.height(6.dp))
        NmrHairlineDivider()

        IntroFeatureRow(
            icon = NmrIcons.KompleYasak,
            title = strings.introFeature1Title,
            subtitle = strings.introFeature1Sub
        )
        NmrHairlineDivider()

        IntroFeatureRow(
            icon = NmrIcons.BugunNav,
            title = strings.introFeature2Title,
            subtitle = strings.introFeature2Sub
        )
        NmrHairlineDivider()

        IntroFeatureRow(
            icon = NmrIcons.KotaSureNav,
            title = strings.introFeature3Title,
            subtitle = strings.introFeature3Sub
        )
        Spacer(modifier = Modifier.height(8.dp))
    }
}

@Composable
fun IntroFeatureRow(icon: ImageVector, title: String, subtitle: String) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .padding(vertical = 8.dp),
        horizontalArrangement = Arrangement.spacedBy(14.dp),
        verticalAlignment = Alignment.CenterVertically
    ) {
        Box(
            modifier = Modifier
                .size(40.dp)
                .clip(RoundedCornerShape(12.dp))
                .background(MaterialTheme.colorScheme.primary.copy(alpha = 0.12f)),
            contentAlignment = Alignment.Center
        ) {
            Icon(
                imageVector = icon,
                contentDescription = null,
                tint = MaterialTheme.colorScheme.primary,
                modifier = Modifier.size(22.dp)
            )
        }
        Column(modifier = Modifier.weight(1f)) {
            Text(
                text = title,
                style = MaterialTheme.typography.titleMedium.copy(fontWeight = FontWeight.SemiBold),
                color = MaterialTheme.colorScheme.onBackground
            )
            Spacer(modifier = Modifier.height(2.dp))
            Text(
                text = subtitle,
                style = MaterialTheme.typography.bodyMedium.copy(lineHeight = 18.sp),
                color = MaterialTheme.colorScheme.onSurfaceVariant
            )
        }
    }
}

// Ekran 1 — Onboarding 2/4 (Gizlilik)
@Composable
fun OnboardingStepPrivacy() {
    val strings = LocalAppStrings.current
    Column(
        modifier = Modifier.fillMaxWidth(),
        verticalArrangement = Arrangement.spacedBy(10.dp)
    ) {
        Spacer(modifier = Modifier.height(8.dp))
        Box(
            modifier = Modifier
                .size(52.dp)
                .clip(RoundedCornerShape(16.dp))
                .background(NmrTheme.colors.selectedSurface),
            contentAlignment = Alignment.Center
        ) {
            Icon(
                imageVector = Icons.Outlined.WifiOff,
                contentDescription = null,
                tint = MaterialTheme.colorScheme.primary,
                modifier = Modifier.size(28.dp)
            )
        }

        Spacer(modifier = Modifier.height(4.dp))
        Text(
            text = strings.privacyHeroTitle,
            style = MaterialTheme.typography.headlineMedium.copy(
                fontWeight = FontWeight.Bold,
                fontSize = 24.sp,
                lineHeight = 32.sp
            ),
            color = MaterialTheme.colorScheme.onBackground
        )

        Spacer(modifier = Modifier.height(6.dp))
        NmrHairlineDivider()

        PrivacyFeatureRow(
            title = strings.privacyFeature1Title,
            subtitle = strings.privacyFeature1Sub
        )
        NmrHairlineDivider()

        PrivacyFeatureRow(
            title = strings.privacyFeature2Title,
            subtitle = strings.privacyFeature2Sub
        )
        NmrHairlineDivider()

        PrivacyFeatureRow(
            title = strings.privacyFeature3Title,
            subtitle = strings.privacyFeature3Sub
        )
        Spacer(modifier = Modifier.height(8.dp))
    }
}

@Composable
fun PrivacyFeatureRow(title: String, subtitle: String) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .padding(vertical = 10.dp),
        horizontalArrangement = Arrangement.spacedBy(16.dp),
        verticalAlignment = Alignment.Top
    ) {
        Icon(
            imageVector = Icons.Outlined.Check,
            contentDescription = null,
            tint = MaterialTheme.colorScheme.primary,
            modifier = Modifier.size(NmrTheme.dimens.iconSize)
        )
        Column(modifier = Modifier.weight(1f)) {
            Text(
                text = title,
                style = MaterialTheme.typography.titleMedium,
                color = MaterialTheme.colorScheme.onBackground
            )
            Spacer(modifier = Modifier.height(2.dp))
            Text(
                text = subtitle,
                style = MaterialTheme.typography.bodyMedium,
                color = MaterialTheme.colorScheme.onSurfaceVariant
            )
        }
    }
}

// Ekran 2 — Onboarding 3/4 (Erişilebilirlik İzni)
@Composable
fun OnboardingStepAccessibility(isServiceActive: Boolean) {
    val strings = LocalAppStrings.current
    Column(
        modifier = Modifier.fillMaxWidth(),
        verticalArrangement = Arrangement.spacedBy(12.dp)
    ) {
        Spacer(modifier = Modifier.height(8.dp))
        Text(
            text = strings.accessibilityStepTitle,
            style = MaterialTheme.typography.headlineMedium.copy(
                fontWeight = FontWeight.Bold,
                fontSize = 24.sp,
                lineHeight = 32.sp
            ),
            color = MaterialTheme.colorScheme.onBackground
        )
        Text(
            text = strings.accessibilityStepSub,
            style = MaterialTheme.typography.bodyLarge,
            color = MaterialTheme.colorScheme.onSurfaceVariant
        )

        Card(
            modifier = Modifier.fillMaxWidth(),
            shape = RoundedCornerShape(16.dp),
            colors = CardDefaults.cardColors(
                containerColor = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.45f)
            ),
            border = BorderStroke(1.dp, MaterialTheme.colorScheme.outline.copy(alpha = 0.4f))
        ) {
            Column(
                modifier = Modifier.padding(14.dp),
                verticalArrangement = Arrangement.spacedBy(6.dp)
            ) {
                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    Icon(
                        imageVector = Icons.Outlined.Security,
                        contentDescription = null,
                        tint = MaterialTheme.colorScheme.primary,
                        modifier = Modifier.size(18.dp)
                    )
                    Text(
                        text = strings.accessibilityDisclosureTitle,
                        style = MaterialTheme.typography.labelMedium.copy(fontWeight = FontWeight.Bold),
                        color = MaterialTheme.colorScheme.primary
                    )
                }
                Text(
                    text = strings.accessibilityDisclosureBody,
                    style = MaterialTheme.typography.bodySmall.copy(lineHeight = 16.sp),
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
            }
        }

        Spacer(modifier = Modifier.height(8.dp))

        Column(verticalArrangement = Arrangement.spacedBy(16.dp)) {
            NumberedStepRow(stepNumber = 1, text = strings.step1Text)
            NumberedStepRow(stepNumber = 2, text = strings.step2Text)
            NumberedStepRow(stepNumber = 3, text = strings.step3Text)
        }

        Spacer(modifier = Modifier.height(16.dp))

        if (!isServiceActive) {
            NmrStatusChip(
                text = strings.waitingPermission,
                statusColor = NmrTheme.colors.statusOrange
            )
        } else {
            NmrStatusChip(
                text = strings.permissionGranted,
                statusColor = NmrTheme.colors.statusGreen
            )
        }
    }
}

@Composable
fun NumberedStepRow(stepNumber: Int, text: String) {
    Row(
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.spacedBy(16.dp)
    ) {
        Box(
            modifier = Modifier
                .size(32.dp)
                .clip(CircleShape)
                .border(BorderStroke(1.5.dp, MaterialTheme.colorScheme.primary), CircleShape),
            contentAlignment = Alignment.Center
        ) {
            Text(
                text = "$stepNumber",
                style = MaterialTheme.typography.titleMedium,
                color = MaterialTheme.colorScheme.primary
            )
        }
        Text(
            text = text,
            style = MaterialTheme.typography.titleMedium,
            color = MaterialTheme.colorScheme.onBackground
        )
    }
}

// Ekran 3 — Onboarding 4/4 (Profil Seçimi)
@Composable
fun OnboardingStepProfile(
    onSelectProfile: (ProtectionLevel) -> Unit
) {
    val strings = LocalAppStrings.current
    var selectedPreset by remember { mutableStateOf(ProtectionLevel.BALANCED) }

    Column(
        modifier = Modifier.fillMaxWidth(),
        verticalArrangement = Arrangement.spacedBy(10.dp)
    ) {
        Spacer(modifier = Modifier.height(8.dp))
        Text(
            text = strings.profileStepTitle,
            style = MaterialTheme.typography.headlineMedium.copy(
                fontWeight = FontWeight.Bold,
                fontSize = 24.sp,
                lineHeight = 32.sp
            ),
            color = MaterialTheme.colorScheme.onBackground
        )

        Spacer(modifier = Modifier.height(8.dp))

        NmrSelectableRow(
            title = strings.presetBalanced,
            subtitle = strings.profileStepBalancedSub,
            icon = NmrIcons.Dengeli,
            selected = selectedPreset == ProtectionLevel.BALANCED,
            onClick = {
                selectedPreset = ProtectionLevel.BALANCED
                onSelectProfile(ProtectionLevel.BALANCED)
            },
            useRadioIndicator = false
        )

        NmrSelectableRow(
            title = strings.presetMindful,
            subtitle = strings.profileStepMindfulSub,
            icon = NmrIcons.Bilincli,
            selected = selectedPreset == ProtectionLevel.MINDFUL,
            onClick = {
                selectedPreset = ProtectionLevel.MINDFUL
                onSelectProfile(ProtectionLevel.MINDFUL)
            },
            useRadioIndicator = false
        )

        NmrSelectableRow(
            title = strings.blockAllTitle,
            subtitle = strings.profileStepStrictSub,
            icon = NmrIcons.KompleYasak,
            selected = selectedPreset == ProtectionLevel.STRICT,
            onClick = {
                selectedPreset = ProtectionLevel.STRICT
                onSelectProfile(ProtectionLevel.STRICT)
            },
            useRadioIndicator = false
        )

        Spacer(modifier = Modifier.height(8.dp))
        Text(
            text = strings.profileStepFooter,
            style = MaterialTheme.typography.bodyMedium,
            color = MaterialTheme.colorScheme.onSurfaceVariant
        )
    }
}

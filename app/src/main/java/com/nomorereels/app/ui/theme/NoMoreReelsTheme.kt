package com.nomorereels.app.ui.theme

import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.isSystemInDarkTheme
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.outlined.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp

// ============================================================================
// 1. RENK PALETİ (COLOR TOKENS) - Light & AMOLED Dark + Status Colors
// ============================================================================

// Light Theme Tokens
val LightBackground = Color(0xFFFAFAF8)
val LightSurface = Color(0xFFF1F1EE)
val LightOutline = Color(0xFFDDDDD8)
val LightTextPrimary = Color(0xFF171917)
val LightTextSecondary = Color(0xFF5F645F)
val LightAccent = Color(0xFF1E6B5C)
val LightAccentContainer = Color(0xFFE6F0EE) // Seçili kart/chip yüzeyi için hafif yeşil tonu

// AMOLED Dark Theme Tokens
val AmoledBackground = Color(0xFF000000)
val AmoledSurface = Color(0xFF101311)
val AmoledOutline = Color(0xFF262B28)
val AmoledTextPrimary = Color(0xFFE8ECE9)
val AmoledTextSecondary = Color(0xFF9AA19C)
val AmoledAccent = Color(0xFF5CC4AA)
val AmoledAccentContainer = Color(0xFF11241F) // AMOLED seçili yüzey tonu

// Status Colors (Yalnızca ilerleme çubukları ve durum çipleri için)
val StatusGreen = Color(0xFF2E9E5B)
val StatusOrange = Color(0xFFE08A1E)
val StatusRed = Color(0xFFCF4040)

// Özel Semantik Renkler (MaterialTheme dışındaki özel token'lar)
@Immutable
data class NmrExtendedColors(
    val statusGreen: Color = StatusGreen,
    val statusGreenContainer: Color = StatusGreen.copy(alpha = 0.14f),
    val statusOrange: Color = StatusOrange,
    val statusOrangeContainer: Color = StatusOrange.copy(alpha = 0.14f),
    val statusRed: Color = StatusRed,
    val statusRedContainer: Color = StatusRed.copy(alpha = 0.14f),
    val selectedSurface: Color = LightAccentContainer,
    val selectedBorder: Color = LightAccent,
    val divider: Color = LightOutline,
    val capsuleBackground: Color = Color(0xFF000000),
    val capsuleText: Color = Color(0xFFFFFFFF)
)

val LocalNmrColors = staticCompositionLocalOf { NmrExtendedColors() }

private val LightColorScheme = lightColorScheme(
    primary = LightAccent,
    onPrimary = Color.White,
    primaryContainer = LightAccentContainer,
    onPrimaryContainer = LightAccent,
    secondary = LightAccent,
    onSecondary = Color.White,
    secondaryContainer = LightAccentContainer,
    onSecondaryContainer = LightAccent,
    background = LightBackground,
    onBackground = LightTextPrimary,
    surface = LightSurface,
    onSurface = LightTextPrimary,
    surfaceVariant = LightSurface,
    onSurfaceVariant = LightTextSecondary,
    outline = LightOutline,
    outlineVariant = LightOutline,
    error = StatusRed,
    onError = Color.White
)

private val AmoledDarkColorScheme = darkColorScheme(
    primary = AmoledAccent,
    onPrimary = Color(0xFF062019),
    primaryContainer = AmoledAccentContainer,
    onPrimaryContainer = AmoledAccent,
    secondary = AmoledAccent,
    onSecondary = Color(0xFF062019),
    secondaryContainer = AmoledAccentContainer,
    onSecondaryContainer = AmoledAccent,
    background = AmoledBackground,
    onBackground = AmoledTextPrimary,
    surface = AmoledSurface,
    onSurface = AmoledTextPrimary,
    surfaceVariant = AmoledSurface,
    onSurfaceVariant = AmoledTextSecondary,
    outline = AmoledOutline,
    outlineVariant = AmoledOutline,
    error = StatusRed,
    onError = Color.Black
)

// ============================================================================
// 2. TİPOGRAFİ (TYPOGRAPHY) - Inter & JetBrains Mono + Tabular Figures (tnum)
// ============================================================================

val InterFontFamily = FontFamily.SansSerif
val JetBrainsMonoFontFamily = FontFamily.Monospace

private const val TABULAR_NUMS = "tnum"

val NmrTypography = Typography(
    displayLarge = TextStyle(
        fontFamily = InterFontFamily,
        fontWeight = FontWeight.SemiBold,
        fontSize = 32.sp,
        lineHeight = 38.sp,
        letterSpacing = (-0.5).sp,
        fontFeatureSettings = TABULAR_NUMS
    ),
    titleLarge = TextStyle(
        fontFamily = InterFontFamily,
        fontWeight = FontWeight.SemiBold,
        fontSize = 20.sp,
        lineHeight = 28.sp,
        letterSpacing = (-0.2).sp,
        fontFeatureSettings = TABULAR_NUMS
    ),
    titleMedium = TextStyle(
        fontFamily = InterFontFamily,
        fontWeight = FontWeight.Medium,
        fontSize = 15.sp,
        lineHeight = 22.sp,
        fontFeatureSettings = TABULAR_NUMS
    ),
    bodyLarge = TextStyle(
        fontFamily = InterFontFamily,
        fontWeight = FontWeight.Normal,
        fontSize = 15.sp,
        lineHeight = 22.sp,
        fontFeatureSettings = TABULAR_NUMS
    ),
    bodyMedium = TextStyle(
        fontFamily = InterFontFamily,
        fontWeight = FontWeight.Normal,
        fontSize = 14.sp,
        lineHeight = 20.sp,
        fontFeatureSettings = TABULAR_NUMS
    ),
    labelMedium = TextStyle(
        fontFamily = InterFontFamily,
        fontWeight = FontWeight.Medium,
        fontSize = 12.sp,
        lineHeight = 16.sp,
        letterSpacing = 0.8.sp,
        fontFeatureSettings = TABULAR_NUMS
    ),
    labelSmall = TextStyle(
        fontFamily = InterFontFamily,
        fontWeight = FontWeight.Medium,
        fontSize = 11.sp,
        lineHeight = 16.sp,
        letterSpacing = 0.4.sp,
        fontFeatureSettings = TABULAR_NUMS
    )
)

val NmrLogTextStyle = TextStyle(
    fontFamily = JetBrainsMonoFontFamily,
    fontWeight = FontWeight.Normal,
    fontSize = 12.sp,
    lineHeight = 18.sp,
    fontFeatureSettings = TABULAR_NUMS
)

// ============================================================================
// 3. ŞEKİLLER & ÖLÇÜLER (SHAPES & DIMENSIONS)
// ============================================================================

val NmrShapes = Shapes(
    extraSmall = RoundedCornerShape(8.dp),
    small = RoundedCornerShape(8.dp),
    medium = RoundedCornerShape(16.dp),
    large = RoundedCornerShape(28.dp),
    extraLarge = CircleShape
)

@Immutable
data class NmrDimens(
    val screenHorizontalMargin: Dp = 16.dp,
    val gridUnit: Dp = 8.dp,
    val minTouchTarget: Dp = 48.dp,
    val cardRadius: Dp = 16.dp,
    val chipRadius: Dp = 8.dp,
    val dialogRadius: Dp = 28.dp,
    val iconSize: Dp = 24.dp,
    val heroIconSize: Dp = 64.dp,
    val dividerThickness: Dp = 1.dp,
    val selectedBorderWidth: Dp = 1.5.dp,
    val statusDotSize: Dp = 6.dp
)

val LocalNmrDimens = staticCompositionLocalOf { NmrDimens() }

// ============================================================================
// 4. İKON HARİTASI (MATERIAL SYMBOLS OUTLINED MAPPING)
// ============================================================================

object NmrIcons {
    val KompleYasak: ImageVector = Icons.Outlined.Block
    val SadeceBirTane: ImageVector = Icons.Outlined.LooksOne
    val XTane: ImageVector = Icons.Outlined.Tag
    val XDakika: ImageVector = Icons.Outlined.Timer
    val Dengeli: ImageVector = Icons.Outlined.Balance
    val Bilincli: ImageVector = Icons.Outlined.SelfImprovement
    val Dm: ImageVector = Icons.Outlined.ChatBubbleOutline
    val Kesfet: ImageVector = Icons.Outlined.Search
    val AnaSayfa: ImageVector = Icons.Outlined.Home
    val Profil: ImageVector = Icons.Outlined.PersonOutline
    val ReelsSekmesi: ImageVector = Icons.Outlined.Movie
    val GeceOdak: ImageVector = Icons.Outlined.Bedtime
    val IradeKilidi: ImageVector = Icons.Outlined.Lock
    val KameraKapsulu: ImageVector = Icons.Outlined.Visibility
    val KorumalarNav: ImageVector = Icons.Outlined.Shield
    val BolgelerNav: ImageVector = Icons.Outlined.GridView
    val KotaSureNav: ImageVector = Icons.Outlined.HourglassEmpty
    val AyarlarNav: ImageVector = Icons.Outlined.Settings
}

// ============================================================================
// 5. TEMA YÖNETİCİSİ (THEME COMPOSABLE) - Açık | Sistem | AMOLED
// ============================================================================

enum class NmrThemeMode(val labelTr: String, val labelEn: String = labelTr) {
    LIGHT("Açık", "Light"),
    SYSTEM("Sistem", "System"),
    AMOLED("AMOLED", "AMOLED")
}

enum class NmrLanguage(val code: String) {
    TR("TR"),
    EN("EN")
}

@Composable
fun NoMoreReelsTheme(
    themeMode: NmrThemeMode = NmrThemeMode.LIGHT,
    content: @Composable () -> Unit
) {
    val isDark = when (themeMode) {
        NmrThemeMode.LIGHT -> false
        NmrThemeMode.AMOLED -> true
        NmrThemeMode.SYSTEM -> isSystemInDarkTheme()
    }

    val colorScheme = if (isDark) AmoledDarkColorScheme else LightColorScheme

    val extendedColors = if (isDark) {
        NmrExtendedColors(
            selectedSurface = AmoledAccentContainer,
            selectedBorder = AmoledAccent,
            divider = AmoledOutline
        )
    } else {
        NmrExtendedColors(
            selectedSurface = LightAccentContainer,
            selectedBorder = LightAccent,
            divider = LightOutline
        )
    }

    CompositionLocalProvider(
        LocalNmrColors provides extendedColors,
        LocalNmrDimens provides NmrDimens()
    ) {
        MaterialTheme(
            colorScheme = colorScheme,
            typography = NmrTypography,
            shapes = NmrShapes,
            content = content
        )
    }
}

object NmrTheme {
    val colors: NmrExtendedColors
        @Composable
        @ReadOnlyComposable
        get() = LocalNmrColors.current

    val dimens: NmrDimens
        @Composable
        @ReadOnlyComposable
        get() = LocalNmrDimens.current
}

// ============================================================================
// 6. TASARIM SİSTEMİ BİLEŞENLERİ (PRODUCTION-READY M3 COMPONENTS)
// ============================================================================

@Composable
fun NmrHeroCard(
    modifier: Modifier = Modifier,
    contentPadding: PaddingValues = PaddingValues(16.dp),
    content: @Composable ColumnScope.() -> Unit
) {
    Surface(
        modifier = modifier.fillMaxWidth(),
        shape = RoundedCornerShape(NmrTheme.dimens.cardRadius),
        color = MaterialTheme.colorScheme.surface,
        border = BorderStroke(NmrTheme.dimens.dividerThickness, MaterialTheme.colorScheme.outline),
        tonalElevation = 0.dp,
        shadowElevation = 0.dp
    ) {
        Column(
            modifier = Modifier.padding(contentPadding),
            content = content
        )
    }
}

@Composable
fun NmrSelectableRow(
    title: String,
    subtitle: String,
    icon: ImageVector,
    selected: Boolean,
    onClick: () -> Unit,
    modifier: Modifier = Modifier,
    useRadioIndicator: Boolean = true
) {
    val backgroundColor = if (selected) NmrTheme.colors.selectedSurface else Color.Transparent
    val borderStroke = if (selected) {
        BorderStroke(NmrTheme.dimens.selectedBorderWidth, NmrTheme.colors.selectedBorder)
    } else null
    val iconAndControlTint = if (selected) {
        MaterialTheme.colorScheme.primary
    } else {
        MaterialTheme.colorScheme.onBackground
    }

    Surface(
        modifier = modifier
            .fillMaxWidth()
            .heightIn(min = NmrTheme.dimens.minTouchTarget)
            .clip(RoundedCornerShape(NmrTheme.dimens.cardRadius))
            .clickable(onClick = onClick),
        shape = RoundedCornerShape(NmrTheme.dimens.cardRadius),
        color = backgroundColor,
        border = borderStroke,
        tonalElevation = 0.dp,
        shadowElevation = 0.dp
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 16.dp, vertical = 14.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            Icon(
                imageVector = icon,
                contentDescription = null,
                tint = iconAndControlTint,
                modifier = Modifier.size(NmrTheme.dimens.iconSize)
            )
            Spacer(modifier = Modifier.width(16.dp))
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
            Spacer(modifier = Modifier.width(12.dp))
            if (useRadioIndicator) {
                RadioButton(
                    selected = selected,
                    onClick = null,
                    colors = RadioButtonDefaults.colors(
                        selectedColor = MaterialTheme.colorScheme.primary,
                        unselectedColor = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                )
            } else if (selected) {
                Icon(
                    imageVector = Icons.Outlined.CheckCircle,
                    contentDescription = null,
                    tint = MaterialTheme.colorScheme.primary,
                    modifier = Modifier.size(22.dp)
                )
            }
        }
    }
}

@Composable
fun NmrSectionHeader(
    title: String,
    modifier: Modifier = Modifier
) {
    Text(
        text = title.uppercase(),
        style = MaterialTheme.typography.labelMedium,
        color = MaterialTheme.colorScheme.onSurfaceVariant,
        modifier = modifier
            .fillMaxWidth()
            .padding(top = 20.dp, bottom = 8.dp)
    )
}

@Composable
fun NmrHairlineDivider(modifier: Modifier = Modifier) {
    HorizontalDivider(
        modifier = modifier,
        thickness = NmrTheme.dimens.dividerThickness,
        color = NmrTheme.colors.divider
    )
}

@Composable
fun NmrFilledButton(
    text: String,
    onClick: () -> Unit,
    modifier: Modifier = Modifier,
    enabled: Boolean = true
) {
    Button(
        onClick = onClick,
        enabled = enabled,
        modifier = modifier
            .fillMaxWidth()
            .heightIn(min = 52.dp),
        shape = CircleShape,
        elevation = ButtonDefaults.buttonElevation(0.dp, 0.dp, 0.dp, 0.dp, 0.dp),
        colors = ButtonDefaults.buttonColors(
            containerColor = MaterialTheme.colorScheme.primary,
            contentColor = MaterialTheme.colorScheme.onPrimary,
            disabledContainerColor = MaterialTheme.colorScheme.surface,
            disabledContentColor = MaterialTheme.colorScheme.onSurfaceVariant
        )
    ) {
        Text(
            text = text,
            style = MaterialTheme.typography.titleMedium
        )
    }
}

@Composable
fun NmrOutlinedButton(
    text: String,
    onClick: () -> Unit,
    modifier: Modifier = Modifier,
    enabled: Boolean = true
) {
    OutlinedButton(
        onClick = onClick,
        enabled = enabled,
        modifier = modifier
            .fillMaxWidth()
            .heightIn(min = 52.dp),
        shape = CircleShape,
        border = BorderStroke(1.dp, MaterialTheme.colorScheme.primary),
        colors = ButtonDefaults.outlinedButtonColors(
            contentColor = MaterialTheme.colorScheme.primary
        )
    ) {
        Text(
            text = text,
            style = MaterialTheme.typography.titleMedium
        )
    }
}


@Composable
fun NmrChip(
    label: String,
    selected: Boolean,
    onClick: () -> Unit,
    modifier: Modifier = Modifier
) {
    val containerColor = if (selected) NmrTheme.colors.selectedSurface else MaterialTheme.colorScheme.surface
    val borderColor = if (selected) MaterialTheme.colorScheme.primary else MaterialTheme.colorScheme.outline
    val textColor = if (selected) MaterialTheme.colorScheme.primary else MaterialTheme.colorScheme.onBackground

    Surface(
        modifier = modifier
            .heightIn(min = 36.dp)
            .clip(RoundedCornerShape(NmrTheme.dimens.chipRadius))
            .clickable(onClick = onClick),
        shape = RoundedCornerShape(NmrTheme.dimens.chipRadius),
        color = containerColor,
        border = BorderStroke(1.dp, borderColor),
        tonalElevation = 0.dp,
        shadowElevation = 0.dp
    ) {
        Box(
            modifier = Modifier.padding(horizontal = 14.dp, vertical = 8.dp),
            contentAlignment = Alignment.Center
        ) {
            Text(
                text = label,
                style = MaterialTheme.typography.bodyMedium.copy(
                    fontWeight = if (selected) FontWeight.Medium else FontWeight.Normal
                ),
                color = textColor
            )
        }
    }
}

@Composable
fun NmrStatusChip(
    text: String,
    statusColor: Color = NmrTheme.colors.statusOrange,
    modifier: Modifier = Modifier
) {
    Surface(
        modifier = modifier,
        shape = RoundedCornerShape(NmrTheme.dimens.chipRadius),
        color = statusColor.copy(alpha = 0.12f),
        border = BorderStroke(1.dp, statusColor.copy(alpha = 0.4f)),
        tonalElevation = 0.dp
    ) {
        Row(
            modifier = Modifier.padding(horizontal = 12.dp, vertical = 6.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            Box(
                modifier = Modifier
                    .size(6.dp)
                    .clip(CircleShape)
                    .background(statusColor)
            )
            Spacer(modifier = Modifier.width(8.dp))
            Text(
                text = text,
                style = MaterialTheme.typography.labelMedium.copy(letterSpacing = 0.sp),
                color = statusColor
            )
        }
    }
}

@Composable
fun <T> NmrSegmentedControl(
    options: List<T>,
    selectedOption: T,
    onOptionSelected: (T) -> Unit,
    labelProvider: (T) -> String,
    modifier: Modifier = Modifier
) {
    Surface(
        modifier = modifier,
        shape = RoundedCornerShape(NmrTheme.dimens.chipRadius),
        color = MaterialTheme.colorScheme.surface,
        border = BorderStroke(1.dp, MaterialTheme.colorScheme.outline),
        tonalElevation = 0.dp
    ) {
        Row(
            modifier = Modifier.padding(2.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            options.forEach { option ->
                val isSelected = option == selectedOption
                Surface(
                    modifier = Modifier
                        .clip(RoundedCornerShape(6.dp))
                        .clickable(
                            interactionSource = remember { MutableInteractionSource() },
                            indication = null
                        ) { onOptionSelected(option) },
                    shape = RoundedCornerShape(6.dp),
                    color = if (isSelected) MaterialTheme.colorScheme.primary else Color.Transparent,
                    tonalElevation = 0.dp
                ) {
                    Text(
                        text = labelProvider(option),
                        style = MaterialTheme.typography.labelMedium.copy(letterSpacing = 0.sp),
                        color = if (isSelected) MaterialTheme.colorScheme.onPrimary else MaterialTheme.colorScheme.onBackground,
                        modifier = Modifier.padding(horizontal = 10.dp, vertical = 6.dp)
                    )
                }
            }
        }
    }
}

@Composable
fun NmrCameraCapsuleOverlay(
    text: String,
    dotColor: Color,
    modifier: Modifier = Modifier
) {
    Surface(
        modifier = modifier,
        shape = CircleShape,
        color = NmrTheme.colors.capsuleBackground,
        tonalElevation = 0.dp,
        shadowElevation = 0.dp
    ) {
        Row(
            modifier = Modifier.padding(horizontal = 14.dp, vertical = 7.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            Box(
                modifier = Modifier
                    .size(NmrTheme.dimens.statusDotSize)
                    .clip(CircleShape)
                    .background(dotColor)
            )
            Spacer(modifier = Modifier.width(8.dp))
            Text(
                text = text,
                style = MaterialTheme.typography.labelMedium.copy(letterSpacing = 0.sp),
                color = NmrTheme.colors.capsuleText
            )
        }
    }
}

enum class NmrBottomTab(val label: String, val icon: ImageVector) {
    KORUMALAR("Korumalar", NmrIcons.KorumalarNav),
    BOLGELER("Bölgeler", NmrIcons.BolgelerNav),
    KOTA_SURE("Kota & Süre", NmrIcons.KotaSureNav),
    AYARLAR("Ayarlar", NmrIcons.AyarlarNav)
}

@Composable
fun NmrBottomNavigationBar(
    currentTab: NmrBottomTab,
    onTabSelected: (NmrBottomTab) -> Unit,
    modifier: Modifier = Modifier
) {
    val strings = com.nomorereels.app.ui.i18n.LocalAppStrings.current
    Column(modifier = modifier.fillMaxWidth()) {
        NmrHairlineDivider()
        NavigationBar(
            containerColor = MaterialTheme.colorScheme.background,
            tonalElevation = 0.dp
        ) {
            NmrBottomTab.entries.forEach { tab ->
                val selected = tab == currentTab
                val labelText = when (tab) {
                    NmrBottomTab.KORUMALAR -> strings.tabProtection
                    NmrBottomTab.BOLGELER -> strings.tabRegions
                    NmrBottomTab.KOTA_SURE -> strings.tabQuota
                    NmrBottomTab.AYARLAR -> strings.tabSettings
                }
                NavigationBarItem(
                    selected = selected,
                    onClick = { onTabSelected(tab) },
                    icon = {
                        Icon(
                            imageVector = tab.icon,
                            contentDescription = labelText,
                            modifier = Modifier.size(NmrTheme.dimens.iconSize)
                        )
                    },
                    label = {
                        Text(
                            text = labelText,
                            style = MaterialTheme.typography.labelSmall
                        )
                    },
                    colors = NavigationBarItemDefaults.colors(
                        selectedIconColor = MaterialTheme.colorScheme.primary,
                        selectedTextColor = MaterialTheme.colorScheme.primary,
                        indicatorColor = NmrTheme.colors.selectedSurface,
                        unselectedIconColor = MaterialTheme.colorScheme.onSurfaceVariant,
                        unselectedTextColor = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                )
            }
        }
    }
}


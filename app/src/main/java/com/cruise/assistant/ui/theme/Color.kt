package com.cruise.assistant.ui.theme

import androidx.compose.runtime.Composable
import androidx.compose.runtime.ReadOnlyComposable
import androidx.compose.runtime.staticCompositionLocalOf
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.lerp
import com.cruise.assistant.domain.AppColorTheme

// Core Neutrals
val Slate900 = Color(0xFF0F172A)
val Slate700 = Color(0xFF334155)
val Slate500 = Color(0xFF64748B)
val Slate400 = Color(0xFF94A3B8)
val Slate200 = Color(0xFFE2E8F0)
val Slate100 = Color(0xFFF1F5F9)

val SurfaceCard = Color(0xFFFFFFFF)
val GridLine = Color(0xFFF1F5F9)
/** 整点横向网格线：比半点线更深更清晰，突出整点 */
val GridLineHour = Color(0xFFE2E8F0)
val NowRed = Color(0xFFEF4444)

// Cruise Color Palette Specification
data class CruiseColorPalette(
    val primary: Color,
    val secondary: Color,
    val primaryContainer: Color,
    val onPrimaryContainer: Color,
    val primaryLight: Color,
    val accent: Color,
    val surfaceContainer: Color,
    val surfaceCard: Color = SurfaceCard,
)

// 1. 朱迪蓝 (Judy Blue)
val JudyBluePalette = CruiseColorPalette(
    primary = Color(0xFF0284C7),
    secondary = Color(0xFF0369A1),
    primaryContainer = Color(0xFFE0F2FE),
    onPrimaryContainer = Color(0xFF0369A1),
    primaryLight = Color(0xFF38BDF8),
    accent = Color(0xFF38BDF8),
    surfaceContainer = Color(0xFFF8FAFC),
)

// 2. 达菲蓝 (Duffy Blue)
val DuffyBluePalette = CruiseColorPalette(
    primary = Color(0xFF2563EB),
    secondary = Color(0xFF1E3A8A),
    primaryContainer = Color(0xFFEFF6FF),
    onPrimaryContainer = Color(0xFF1E3A8A),
    primaryLight = Color(0xFF60A5FA),
    accent = Color(0xFFB45309), // Duffy Bear warm brown
    surfaceContainer = Color(0xFFF8FAFD),
)

// 3. 贝儿粉 (LinaBell Pink - 低饱和柔和蜜桃粉)
val LinaBellPinkPalette = CruiseColorPalette(
    primary = Color(0xFFD47A70),
    secondary = Color(0xFF8C3E3E),
    primaryContainer = Color(0xFFFAF0EE),
    onPrimaryContainer = Color(0xFF8C3E3E),
    primaryLight = Color(0xFFE8A59E),
    accent = Color(0xFFA855F7), // LinaBell flower purple
    surfaceContainer = Color(0xFFFDFBFB),
)

// 4. 玫玫粉 (ShellieMay Pink - 复古烟熏玫瑰粉)
val ShellieMayPinkPalette = CruiseColorPalette(
    primary = Color(0xFFC06C84),
    secondary = Color(0xFF7A2B48),
    primaryContainer = Color(0xFFFDF1F5),
    onPrimaryContainer = Color(0xFF7A2B48),
    primaryLight = Color(0xFFE09BAE),
    accent = Color(0xFF9CA3AF), // ShellieMay bow gray
    surfaceContainer = Color(0xFFFDFBFB),
)

// 5. 露露紫 (StellaLou / Lulu Purple - 梦幻芭蕾紫)
val LuluPurplePalette = CruiseColorPalette(
    primary = Color(0xFF8B5CF6),
    secondary = Color(0xFF5B21B6),
    primaryContainer = Color(0xFFF5F3FF),
    onPrimaryContainer = Color(0xFF5B21B6),
    primaryLight = Color(0xFFC4B5FD),
    accent = Color(0xFF14B8A6), // StellaLou flower mint
    surfaceContainer = Color(0xFFFAFAFE),
)
val StellaLouPurplePalette = LuluPurplePalette

// 6. 托尼绿 (Gelatoni Green - 画家水彩薄荷绿)
val GelatoniGreenPalette = CruiseColorPalette(
    primary = Color(0xFF4E8A73),
    secondary = Color(0xFF1E4636),
    primaryContainer = Color(0xFFEEF7F2),
    onPrimaryContainer = Color(0xFF1E4636),
    primaryLight = Color(0xFF81B8A3),
    accent = Color(0xFF0284C7), // Gelatoni beret blue
    surfaceContainer = Color(0xFFF9FBF9),
)

// 7. olu绿 (Olu Green - 夏威夷海岛青柠绿)
val OluGreenPalette = CruiseColorPalette(
    primary = Color(0xFF65A30D),
    secondary = Color(0xFF365314),
    primaryContainer = Color(0xFFF4FBE8),
    onPrimaryContainer = Color(0xFF365314),
    primaryLight = Color(0xFFA3E635),
    accent = Color(0xFFD97706), // Ukulele amber
    surfaceContainer = Color(0xFFFAFDF6),
)

// 8. 饼饼黄 (CookieAnn Yellow - 烘焙暖心蜂蜜黄)
val CookieAnnYellowPalette = CruiseColorPalette(
    primary = Color(0xFFD97706),
    secondary = Color(0xFF78350F),
    primaryContainer = Color(0xFFFFFBEB),
    onPrimaryContainer = Color(0xFF78350F),
    primaryLight = Color(0xFFFBBF24),
    accent = Color(0xFFEC4899), // CookieAnn bow pink
    surfaceContainer = Color(0xFFFDFDF9),
)

fun paletteFor(theme: AppColorTheme): CruiseColorPalette = when (theme) {
    AppColorTheme.JUDY_BLUE -> JudyBluePalette
    AppColorTheme.DUFFY_BLUE -> DuffyBluePalette
    AppColorTheme.LINABELL_PINK -> LinaBellPinkPalette
    AppColorTheme.SHELLIEMAY_PINK -> ShellieMayPinkPalette
    AppColorTheme.LULU_PURPLE -> LuluPurplePalette
    AppColorTheme.GELATONI_GREEN -> GelatoniGreenPalette
    AppColorTheme.OLU_GREEN -> OluGreenPalette
    AppColorTheme.COOKIEANN_YELLOW -> CookieAnnYellowPalette
}

val LocalCruiseColors = staticCompositionLocalOf { JudyBluePalette }

// MaterialTheme extensions & Top-level Composable helpers for reactive access
val androidx.compose.material3.MaterialTheme.cruiseColors: CruiseColorPalette
    @Composable
    @ReadOnlyComposable
    get() = LocalCruiseColors.current

// Dynamic color accessors
val CruiseAzure: Color @Composable @ReadOnlyComposable get() = LocalCruiseColors.current.primary
val CruiseMarine: Color @Composable @ReadOnlyComposable get() = LocalCruiseColors.current.secondary
val CruiseAzureLight: Color @Composable @ReadOnlyComposable get() = LocalCruiseColors.current.primaryLight
val CruiseAzureContainer: Color @Composable @ReadOnlyComposable get() = LocalCruiseColors.current.primaryContainer
val CruiseBlue: Color @Composable @ReadOnlyComposable get() = LocalCruiseColors.current.primary
val CruiseBlueLight: Color @Composable @ReadOnlyComposable get() = LocalCruiseColors.current.primaryLight
val SurfaceContainer: Color @Composable @ReadOnlyComposable get() = LocalCruiseColors.current.surfaceContainer
val SurfaceGray: Color @Composable @ReadOnlyComposable get() = LocalCruiseColors.current.surfaceContainer

fun Int.toComposeColor(): Color = Color(this)

/** 卡片底色：类别色 8% 叠在白底上，明亮清爽 */
fun cardBackgroundFor(colorArgb: Int): Color =
    lerp(Color.White, Color(colorArgb), 0.08f)

/** 卡片文字色：类别色往 Slate900 深蓝黑压 45%，保证在浅底上对比度足够且视觉统一 */
fun cardTextColorFor(colorArgb: Int): Color =
    lerp(Color(colorArgb), Slate900, 0.45f)

/** 卡片文字色的通用别名：给 onSurfaceVariant 语义下的类别色文字用 */
fun Color.onSurfaceVariantFor(): Color = lerp(this, Slate900, 0.45f)


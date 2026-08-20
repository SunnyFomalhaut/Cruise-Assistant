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

// 2. 贝儿粉 (LinaBell Pink)
val LinaBellPinkPalette = CruiseColorPalette(
    primary = Color(0xFFE05375),
    secondary = Color(0xFF9E1C3F),
    primaryContainer = Color(0xFFFDF2F8),
    onPrimaryContainer = Color(0xFF9E1C3F),
    primaryLight = Color(0xFFF472B6),
    accent = Color(0xFFA855F7), // LinaBell flower purple
    surfaceContainer = Color(0xFFFDFBFB),
)

// 3. 星黛露紫 (StellaLou Purple)
val StellaLouPurplePalette = CruiseColorPalette(
    primary = Color(0xFF8B5CF6),
    secondary = Color(0xFF5B21B6),
    primaryContainer = Color(0xFFF5F3FF),
    onPrimaryContainer = Color(0xFF5B21B6),
    primaryLight = Color(0xFFC4B5FD),
    accent = Color(0xFF14B8A6), // StellaLou flower mint
    surfaceContainer = Color(0xFFFAFAFE),
)

fun paletteFor(theme: AppColorTheme): CruiseColorPalette = when (theme) {
    AppColorTheme.JUDY_BLUE -> JudyBluePalette
    AppColorTheme.LINABELL_PINK -> LinaBellPinkPalette
    AppColorTheme.STELLALOU_PURPLE -> StellaLouPurplePalette
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


package com.cruise.assistant.ui.theme

import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.lightColorScheme
import androidx.compose.runtime.Composable
import androidx.compose.runtime.CompositionLocalProvider
import androidx.compose.ui.graphics.Color
import com.cruise.assistant.domain.AppColorTheme

/**
 * 应用固定使用浅色配色，不跟随系统深色模式：spec §10「明确不做」把深色模式列为不做项，
 * 且 [cardBackgroundFor] / [cardTextColorFor] 都假定浅色底（以白为基混合类别色），
 * 若跟随系统深色模式会导致深色背景上浮出近白色卡片，视觉不自洽。
 *
 * 支持通过 [theme] 动态切换朱迪蓝、贝儿粉、星黛露紫三种主题调色板。
 */
@Composable
fun CruiseTheme(
    theme: AppColorTheme = AppColorTheme.JUDY_BLUE,
    content: @Composable () -> Unit,
) {
    val palette = paletteFor(theme)
    val colorScheme = lightColorScheme(
        primary = palette.primary,
        onPrimary = Color.White,
        primaryContainer = palette.primaryContainer,
        onPrimaryContainer = palette.onPrimaryContainer,
        secondary = palette.secondary,
        onSecondary = Color.White,
        secondaryContainer = palette.primaryContainer,
        onSecondaryContainer = palette.secondary,
        background = palette.surfaceContainer,
        onBackground = Slate900,
        surface = palette.surfaceCard,
        onSurface = Slate900,
        surfaceVariant = Slate100,
        onSurfaceVariant = Slate500,
        surfaceContainer = palette.surfaceContainer,
        surfaceContainerHigh = Color.White,
        outline = Slate200,
        outlineVariant = Slate100,
        error = NowRed,
        onError = Color.White,
    )

    CompositionLocalProvider(LocalCruiseColors provides palette) {
        MaterialTheme(
            colorScheme = colorScheme,
            typography = CruiseTypography,
            content = content,
        )
    }
}


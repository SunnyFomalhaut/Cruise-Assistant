package com.cruise.assistant.ui.theme

import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.lightColorScheme
import androidx.compose.runtime.Composable

private val LightColors = lightColorScheme(
    primary = CruiseBlue,
    secondary = CruiseBlueLight,
    background = SurfaceGray,
    surface = androidx.compose.ui.graphics.Color.White,
)

/**
 * 应用固定使用浅色配色，不跟随系统深色模式：spec §10「明确不做」把深色模式列为不做项，
 * 且 [cardBackgroundFor] / [cardTextColorFor] 都假定浅色底（以白为基混合类别色），
 * 若跟随系统深色模式会导致深色背景上浮出近白色卡片，视觉不自洽。
 */
@Composable
fun CruiseTheme(content: @Composable () -> Unit) {
    MaterialTheme(
        colorScheme = LightColors,
        typography = CruiseTypography,
        content = content,
    )
}

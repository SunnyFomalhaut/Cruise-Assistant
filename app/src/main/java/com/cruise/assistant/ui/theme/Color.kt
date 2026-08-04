package com.cruise.assistant.ui.theme

import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.lerp

val CruiseBlue = Color(0xFF2C5F8D)
val CruiseBlueLight = Color(0xFF5B8DEF)
val SurfaceGray = Color(0xFFF6F7F9)
val GridLine = Color(0xFFE8EAED)
val NowRed = Color(0xFFE8563F)

fun Int.toComposeColor(): Color = Color(this)

/** 卡片底色：类别色 12% 叠在白底上（截图1 的观感） */
fun cardBackgroundFor(colorArgb: Int): Color =
    lerp(Color.White, Color(colorArgb), 0.12f)

/** 卡片文字色：类别色往黑压 35%，保证在浅底上对比度足够 */
fun cardTextColorFor(colorArgb: Int): Color =
    lerp(Color(colorArgb), Color.Black, 0.35f)

/** 卡片文字色的通用别名：给 onSurfaceVariant 语义下的类别色文字用 */
fun Color.onSurfaceVariantFor(): Color = lerp(this, Color.Black, 0.35f)

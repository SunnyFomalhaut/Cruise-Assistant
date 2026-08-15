package com.cruise.assistant.ui.theme

import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.lerp

// Ocean Azure Theme Tokens
val CruiseAzure = Color(0xFF0284C7)
val CruiseMarine = Color(0xFF0369A1)
val CruiseAzureLight = Color(0xFF38BDF8)
val CruiseAzureContainer = Color(0xFFE0F2FE)

val SurfaceContainer = Color(0xFFF8FAFC)
val SurfaceCard = Color(0xFFFFFFFF)

val Slate900 = Color(0xFF0F172A)
val Slate700 = Color(0xFF334155)
val Slate500 = Color(0xFF64748B)
val Slate400 = Color(0xFF94A3B8)
val Slate200 = Color(0xFFE2E8F0)
val Slate100 = Color(0xFFF1F5F9)

val CruiseBlue = CruiseAzure
val CruiseBlueLight = CruiseAzureLight
val SurfaceGray = SurfaceContainer

val GridLine = Color(0xFFF1F5F9)
/** 整点横向网格线：比半点线更深更清晰，突出整点 */
val GridLineHour = Color(0xFFE2E8F0)
val NowRed = Color(0xFFEF4444)

fun Int.toComposeColor(): Color = Color(this)

/** 卡片底色：类别色 8% 叠在白底上，明亮清爽 */
fun cardBackgroundFor(colorArgb: Int): Color =
    lerp(Color.White, Color(colorArgb), 0.08f)

/** 卡片文字色：类别色往 Slate900 深蓝黑压 45%，保证在浅底上对比度足够且视觉统一 */
fun cardTextColorFor(colorArgb: Int): Color =
    lerp(Color(colorArgb), Slate900, 0.45f)

/** 卡片文字色的通用别名：给 onSurfaceVariant 语义下的类别色文字用 */
fun Color.onSurfaceVariantFor(): Color = lerp(this, Slate900, 0.45f)


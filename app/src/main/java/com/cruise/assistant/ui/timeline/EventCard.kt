package com.cruise.assistant.ui.timeline

import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.combinedClickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxHeight
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.alpha
import androidx.compose.ui.draw.clip
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.asImageBitmap
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.text.font.FontStyle
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.TextUnit
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.cruise.assistant.domain.Category
import com.cruise.assistant.domain.CruiseEvent
import com.cruise.assistant.domain.DollManager
import com.cruise.assistant.domain.endMinute
import com.cruise.assistant.domain.formatLocation
import com.cruise.assistant.domain.formatWaitStart
import com.cruise.assistant.domain.startMinute
import com.cruise.assistant.ui.common.CategoryChip
import com.cruise.assistant.ui.theme.cardBackgroundFor
import com.cruise.assistant.ui.theme.cardTextColorFor

/** 低于这个高度就压缩成单行显示 */
private val COMPACT_THRESHOLD = 44.dp

/** 贪心分级阈值：本体（演出段）能显示 2 行 / 3 行内容的最小高度（第 1 行按 bodySmall 20dp 行高计，+4dp 修正边界裁剪） */
private val TWO_LINE_MIN_DP = 42.dp
private val THREE_LINE_MIN_DP = 58.dp

/**
 * 需求3：事件卡片。
 * 第1行 [类别chip] 事件内容 / 第2行 地点 / 第3行 时间+时长。
 * 左侧一条类别色条（截图1）。
 */
@Composable
fun EventCard(
    event: CruiseEvent,
    category: Category?,
    heightDp: Dp,
    waitSegmentHeightDp: Dp = 0.dp,
    hasConflict: Boolean,
    onClick: () -> Unit,
    onLongClick: () -> Unit,
    modifier: Modifier = Modifier,
    /** 卡片所在栏是否是最窄的那栏（栏2），影响 compact 分支的排版取舍 */
    narrow: Boolean = false,
    dollsEnabled: Boolean = true,
) {
    val colorArgb = category?.colorArgb ?: 0xFF9E9E9E.toInt()
    val active = event.isActive
    val textColor = if (active) cardTextColorFor(colorArgb) else MaterialTheme.colorScheme.onSurfaceVariant
    val compact = heightDp < COMPACT_THRESHOLD
    val hasWaitSegment = waitSegmentHeightDp > 0.dp
    val location = formatLocation(event.deck, event.zone, event.placeName, compact = true)
    val cardShape = RoundedCornerShape(10.dp)
    // 贪心分级：本体剩余高度决定信息分配（tier 0 = 无候场段，走原有逻辑）
    val bodyHeightDp = heightDp - waitSegmentHeightDp - 2.dp
    val labelSmall = MaterialTheme.typography.labelSmall
    val labelLineHeight = labelSmall.lineHeight.takeIf { it != TextUnit.Unspecified }
        ?: (labelSmall.fontSize * 1.25f)
    val waitTwoLineMinDp = with(LocalDensity.current) {
        (labelLineHeight * 2).toDp() + 4.dp
    }
    val waitThreeLineMinDp = with(LocalDensity.current) {
        (labelLineHeight * 3).toDp() + 6.dp
    }
    val waitLines = when {
        waitSegmentHeightDp >= waitThreeLineMinDp -> 3
        waitSegmentHeightDp >= waitTwoLineMinDp -> 2
        else -> 1
    }
    val bodyLines = when {
        bodyHeightDp >= THREE_LINE_MIN_DP -> 3
        bodyHeightDp >= TWO_LINE_MIN_DP -> 2
        else -> 1
    }

    val dollAsset = if (dollsEnabled && active) {
        remember(event.id, heightDp) { DollManager.selectDollForCard(event.id, heightDp.value) }
    } else {
        null
    }

    Box(
        modifier
            .fillMaxWidth()
            .then(if (active) Modifier else Modifier.alpha(0.45f))
            .clip(cardShape)
            .background(cardBackgroundFor(colorArgb))
            .border(
                width = 1.dp,
                color = Color(colorArgb).copy(alpha = if (active) 0.28f else 0.15f),
                shape = cardShape,
            )
            .combinedClickable(onClick = onClick, onLongClick = onLongClick),
    ) {
        val context = LocalContext.current
        val dollBitmap = if (dollAsset != null) {
            remember(dollAsset) { DollManager.loadDollBitmap(context, dollAsset, 240) }
        } else {
            null
        }

        if (dollBitmap != null) {
            val dollHeight = (heightDp - 4.dp).coerceIn(24.dp, 120.dp)
            Image(
                bitmap = dollBitmap.asImageBitmap(),
                contentDescription = null,
                contentScale = ContentScale.Fit,
                alpha = 0.24f,
                modifier = Modifier
                    .align(if (compact) Alignment.CenterEnd else Alignment.BottomEnd)
                    .padding(end = 4.dp, bottom = 2.dp)
                    .height(dollHeight),
            )
        }

        Row(
            modifier = Modifier.fillMaxSize(),
        ) {
        // 左侧圆角微色柱
        Box(
            Modifier
                .padding(vertical = 4.dp, horizontal = 2.dp)
                .width(3.5.dp)
                .fillMaxHeight()
                .then(if (active) Modifier else Modifier.alpha(0.45f))
                .clip(RoundedCornerShape(2.dp))
                .background(Color(colorArgb)),
        )
        Column(
            Modifier
                .weight(1f)
                .padding(horizontal = if (compact) 3.dp else 5.dp, vertical = 2.dp),
        ) {
            // 等待段：淡色斜纹 + 随分级自适应的文案与排版
            if (hasWaitSegment) {
                val isExtremelyCompactWait = waitSegmentHeightDp < 20.dp
                val waitSingleLineStyle = if (isExtremelyCompactWait) {
                    MaterialTheme.typography.labelSmall.copy(
                        fontSize = 9.5.sp,
                        lineHeight = 11.sp,
                        platformStyle = androidx.compose.ui.text.PlatformTextStyle(includeFontPadding = false),
                    )
                } else {
                    MaterialTheme.typography.labelSmall
                }
                Column(
                    Modifier
                        .fillMaxWidth()
                        .height(waitSegmentHeightDp)
                        .clip(RoundedCornerShape(4.dp))
                        .background(waitSegmentBrush(colorArgb))
                        .padding(horizontal = 4.dp, vertical = if (isExtremelyCompactWait) 0.dp else 2.dp),
                    verticalArrangement = Arrangement.Center,
                ) {
                    when {
                        bodyLines == 3 -> {
                            // Case 1: 候场段 1 行
                            Text(
                                text = "${formatWaitStart(event)} 候场",
                                style = waitSingleLineStyle,
                                fontStyle = FontStyle.Italic,
                                fontWeight = FontWeight.Bold,
                                color = textColor.copy(alpha = 0.9f),
                                maxLines = 1,
                                overflow = TextOverflow.Ellipsis,
                            )
                        }
                        bodyLines == 1 && waitLines >= 3 -> {
                            // Case 2: 候场段 3 行
                            Text(
                                text = "${formatWaitStart(event)} 候场",
                                style = MaterialTheme.typography.labelSmall,
                                fontStyle = FontStyle.Italic,
                                fontWeight = FontWeight.Bold,
                                color = textColor.copy(alpha = 0.9f),
                                maxLines = 1,
                                overflow = TextOverflow.Ellipsis,
                            )
                            location?.let {
                                Text(
                                    text = it,
                                    style = MaterialTheme.typography.labelSmall,
                                    fontStyle = FontStyle.Italic,
                                    color = textColor.copy(alpha = 0.75f),
                                    maxLines = 1,
                                    overflow = TextOverflow.Ellipsis,
                                )
                            }
                            Text(
                                text = formatTimeRange(event),
                                style = MaterialTheme.typography.labelSmall.copy(fontSize = 10.sp),
                                fontStyle = FontStyle.Italic,
                                color = textColor.copy(alpha = 0.75f),
                                maxLines = 1,
                                overflow = TextOverflow.Ellipsis,
                            )
                        }
                        waitLines >= 2 -> {
                            // Case 3 & 过渡态 (waitLines == 2): 候场段 2 行
                            Text(
                                text = "${formatWaitStart(event)} 候场",
                                style = MaterialTheme.typography.labelSmall,
                                fontStyle = FontStyle.Italic,
                                fontWeight = FontWeight.Bold,
                                color = textColor.copy(alpha = 0.9f),
                                maxLines = 1,
                                overflow = TextOverflow.Ellipsis,
                            )
                            location?.let {
                                Text(
                                    text = it,
                                    style = MaterialTheme.typography.labelSmall,
                                    fontStyle = FontStyle.Italic,
                                    color = textColor.copy(alpha = 0.75f),
                                    maxLines = 1,
                                    overflow = TextOverflow.Ellipsis,
                                )
                            }
                        }
                        else -> {
                            // Case 4 & 过渡态 (waitLines == 1): 候场段 1 行
                            val singleLineText = if (location != null) {
                                "${formatWaitStart(event)} 候场 · $location"
                            } else {
                                "${formatWaitStart(event)} 候场"
                            }
                            Text(
                                text = singleLineText,
                                style = waitSingleLineStyle,
                                fontStyle = FontStyle.Italic,
                                fontWeight = FontWeight.Bold,
                                color = textColor.copy(alpha = 0.9f),
                                maxLines = 1,
                                overflow = TextOverflow.Ellipsis,
                            )
                        }
                    }
                }
                Spacer(Modifier.height(2.dp))
            }
            if (!hasWaitSegment) {
                if (compact) {
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        Text(
                            text = event.title,
                            style = MaterialTheme.typography.labelSmall,
                            fontWeight = FontWeight.SemiBold,
                            color = textColor,
                            maxLines = 1,
                            overflow = TextOverflow.Ellipsis,
                            modifier = Modifier.weight(1f),
                        )
                        if (hasConflict) {
                            ConflictBadge()
                        }
                        if (!narrow) {
                            Text(
                                text = formatStart(event),
                                style = MaterialTheme.typography.labelSmall.copy(fontSize = 10.sp),
                                color = textColor.copy(alpha = 0.8f),
                                maxLines = 1,
                                modifier = Modifier.padding(start = 2.dp),
                            )
                        }
                    }
                } else {
                    // 第1行：分类chip + 标题 + 冲突
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        category?.let {
                            CategoryChip(it.name, colorArgb)
                            Spacer(Modifier.width(4.dp))
                        }
                        Text(
                            text = event.title,
                            style = MaterialTheme.typography.labelMedium,
                            fontWeight = FontWeight.Bold,
                            color = textColor,
                            maxLines = 1,
                            overflow = TextOverflow.Ellipsis,
                            modifier = Modifier.weight(1f),
                        )
                        if (hasConflict) {
                            ConflictBadge()
                        }
                    }
                    // 第2/3行：地点与时间
                    location?.let {
                        Text(
                            text = it,
                            style = MaterialTheme.typography.labelSmall,
                            color = textColor.copy(alpha = 0.85f),
                            maxLines = 1,
                            overflow = TextOverflow.Ellipsis,
                        )
                    }
                    Text(
                        text = formatTimeRange(event),
                        style = MaterialTheme.typography.labelSmall.copy(fontSize = 10.sp),
                        color = textColor.copy(alpha = 0.75f),
                        maxLines = 1,
                    )
                }
            } else {
                when (bodyLines) {
                    3 -> {
                        // Case 1: 本体 3 行
                        // Line 1: CategoryChip + event.title + ConflictBadge
                        Row(verticalAlignment = Alignment.CenterVertically) {
                            category?.let {
                                CategoryChip(it.name, colorArgb)
                                Spacer(Modifier.width(4.dp))
                            }
                            Text(
                                text = event.title,
                                style = MaterialTheme.typography.labelMedium,
                                fontWeight = FontWeight.Bold,
                                color = textColor,
                                maxLines = 1,
                                overflow = TextOverflow.Ellipsis,
                                modifier = Modifier.weight(1f),
                            )
                            if (hasConflict) {
                                ConflictBadge()
                            }
                        }
                        // Line 2: location
                        location?.let {
                            Text(
                                text = it,
                                style = MaterialTheme.typography.labelSmall,
                                color = textColor.copy(alpha = 0.85f),
                                maxLines = 1,
                                overflow = TextOverflow.Ellipsis,
                            )
                        }
                        // Line 3: formatTimeRange(event)
                        Text(
                            text = formatTimeRange(event),
                            style = MaterialTheme.typography.labelSmall.copy(fontSize = 10.sp),
                            color = textColor.copy(alpha = 0.75f),
                            maxLines = 1,
                        )
                    }
                    2 -> {
                        // Case 3 & 过渡态 (bodyLines >= 2): 本体 2 行
                        // Line 1: CategoryChip + event.title + ConflictBadge
                        Row(verticalAlignment = Alignment.CenterVertically) {
                            category?.let {
                                CategoryChip(it.name, colorArgb)
                                Spacer(Modifier.width(4.dp))
                            }
                            Text(
                                text = event.title,
                                style = MaterialTheme.typography.labelMedium,
                                fontWeight = FontWeight.Bold,
                                color = textColor,
                                maxLines = 1,
                                overflow = TextOverflow.Ellipsis,
                                modifier = Modifier.weight(1f),
                            )
                            if (hasConflict) {
                                ConflictBadge()
                            }
                        }
                        // Line 2: formatTimeRange(event)
                        Text(
                            text = formatTimeRange(event),
                            style = MaterialTheme.typography.labelSmall.copy(fontSize = 10.sp),
                            color = textColor.copy(alpha = 0.75f),
                            maxLines = 1,
                        )
                    }
                    else -> {
                        // bodyLines == 1
                        if (waitLines >= 3) {
                            // Case 2: 候场区已放了时间段，本体单行显示 CategoryChip + event.title + ConflictBadge
                            Row(verticalAlignment = Alignment.CenterVertically) {
                                category?.let {
                                    CategoryChip(it.name, colorArgb)
                                    Spacer(Modifier.width(4.dp))
                                }
                                Text(
                                    text = event.title,
                                    style = MaterialTheme.typography.labelSmall,
                                    fontWeight = FontWeight.SemiBold,
                                    color = textColor,
                                    maxLines = 1,
                                    overflow = TextOverflow.Ellipsis,
                                    modifier = Modifier.weight(1f),
                                )
                                if (hasConflict) {
                                    ConflictBadge()
                                }
                            }
                        } else {
                            // Case 4 & 过渡态 (waitLines < 3): 候场区未放时间段，本体单行显示开场时间 + event.title + ConflictBadge
                            Row(verticalAlignment = Alignment.CenterVertically) {
                                Text(
                                    text = "${formatStart(event)} ${event.title}",
                                    style = MaterialTheme.typography.labelSmall,
                                    fontWeight = FontWeight.SemiBold,
                                    color = textColor,
                                    maxLines = 1,
                                    overflow = TextOverflow.Ellipsis,
                                    modifier = Modifier.weight(1f),
                                )
                                if (hasConflict) {
                                    ConflictBadge()
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

@Composable
private fun ConflictBadge() {
    Surface(
        shape = CircleShape,
        color = MaterialTheme.colorScheme.error,
        modifier = Modifier.padding(start = 2.dp).size(13.dp),
    ) {
        Box(contentAlignment = Alignment.Center) {
            Text(
                "!",
                fontSize = 9.sp,
                fontWeight = FontWeight.ExtraBold,
                color = Color.White,
            )
        }
    }
}

private fun formatStart(event: CruiseEvent): String =
    "%02d:%02d".format(event.startTime.hour, event.startTime.minute)

/** "13:15 – 13:45  30m" / 超过 1 小时 "1h 30m" */
private fun formatTimeRange(event: CruiseEvent): String {
    val endMin = event.endMinute
    val end = "%02d:%02d".format((endMin / 60) % 24, endMin % 60)
    return "${formatStart(event)}–$end  ${formatDuration(event.durationMinutes)}"
}

fun formatDuration(minutes: Int): String = when {
    minutes < 60 -> "${minutes}m"
    minutes % 60 == 0 -> "${minutes / 60}h"
    else -> "${minutes / 60}h ${minutes % 60}m"
}

/** 等待段斜纹背景：类别色淡化的 45° 条纹 */
private fun waitSegmentBrush(colorArgb: Int): Brush {
    val base = Color(colorArgb).copy(alpha = 0.18f)
    val stripe = Color(colorArgb).copy(alpha = 0.08f)
    return Brush.linearGradient(
        colors = listOf(base, stripe, base, stripe, base),
        start = Offset.Zero,
        end = Offset.Infinite,
    )
}


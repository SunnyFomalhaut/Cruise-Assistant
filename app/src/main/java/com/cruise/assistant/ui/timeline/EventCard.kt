package com.cruise.assistant.ui.timeline

import androidx.compose.foundation.background
import androidx.compose.foundation.combinedClickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxHeight
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.alpha
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.text.font.FontStyle
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.TextUnit
import androidx.compose.ui.unit.dp
import com.cruise.assistant.domain.Category
import com.cruise.assistant.domain.CruiseEvent
import com.cruise.assistant.domain.endMinute
import com.cruise.assistant.domain.formatLocation
import com.cruise.assistant.domain.startMinute
import com.cruise.assistant.ui.common.CategoryChip
import com.cruise.assistant.ui.theme.cardBackgroundFor
import com.cruise.assistant.ui.theme.cardTextColorFor

/** 低于这个高度就压缩成单行显示 */
private val COMPACT_THRESHOLD = 44.dp

/** 贪心分级阈值：本体（演出段）能显示 2 行 / 3 行内容的最小高度（第 1 行按 bodySmall 20dp 行高计，+4dp 修正边界裁剪） */
private val TWO_LINE_MIN_DP = 42.dp
private val THREE_LINE_MIN_DP = 58.dp
/** 候场段低于此高度时只显示"候场"，避免单行文本被裁剪 */
private val WAIT_TEXT_MIN_DP = 20.dp

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
) {
    val colorArgb = category?.colorArgb ?: 0xFF9E9E9E.toInt()
    val active = event.isActive
    val textColor = if (active) cardTextColorFor(colorArgb) else MaterialTheme.colorScheme.onSurfaceVariant
    val compact = heightDp < COMPACT_THRESHOLD
    val hasWaitSegment = waitSegmentHeightDp > 0.dp
    val location = formatLocation(event.deck, event.zone, event.placeName, compact = true)
    // 贪心分级：本体剩余高度决定信息分配（tier 0 = 无候场段，走原有逻辑）
    val bodyHeightDp = heightDp - waitSegmentHeightDp - 2.dp
    // 候场区两行阈值从 typography 派生（fontScale 自适应）：两行 labelSmall 行高 + 4dp padding
    // 注意：自定义主题的 labelSmall 未设 lineHeight（TextUnit.Unspecified），
    // 对 Unspecified 做算术会抛 "Cannot perform operation for Unspecified type"——
    // 未设 lineHeight 时按 fontSize×1.25 估算实际行高，避免两行在 26dp 段被裁剪。
    val labelSmall = MaterialTheme.typography.labelSmall
    val labelLineHeight = labelSmall.lineHeight.takeIf { it != TextUnit.Unspecified }
        ?: (labelSmall.fontSize * 1.25f)
    val waitTwoLineMinDp = with(LocalDensity.current) {
        (labelLineHeight * 2).toDp() + 4.dp
    }
    // 候场区是否够显示两行（tier 4 用）
    val waitTwoLines = waitSegmentHeightDp >= waitTwoLineMinDp
    val showTier = when {
        !hasWaitSegment -> 0
        bodyHeightDp >= THREE_LINE_MIN_DP -> 3
        bodyHeightDp >= TWO_LINE_MIN_DP -> 2
        waitTwoLines -> 4
        else -> 1
    }
    // 候场段右侧文案随 tier 与段高自适应
    val waitText = when {
        showTier == 0 || waitSegmentHeightDp < WAIT_TEXT_MIN_DP -> "候场"
        showTier == 3 -> "候场"
        showTier == 2 -> "候场 · ${formatStart(event)}"
        else -> buildString {
            append("候场")
            location?.let { append(" · ").append(it) }
            append(" · ").append(formatStart(event))
        }
    }

    Row(
        modifier
            .fillMaxWidth()
            .then(if (active) Modifier else Modifier.alpha(0.45f))
            .clip(RoundedCornerShape(8.dp))
            .background(cardBackgroundFor(colorArgb))
            .combinedClickable(onClick = onClick, onLongClick = onLongClick),
    ) {
        // 左侧色条
        Box(
            Modifier
                .width(4.dp)
                .fillMaxHeight()
                .then(if (active) Modifier else Modifier.alpha(0.45f))
                .background(Color(colorArgb)),
        )
        Column(
            Modifier
                .weight(1f)
                // compact 行挤在最窄的栏2（约18%宽）里，横向 padding 从 6dp 收紧到
                // 2dp，把这几 dp 让给标题——非 compact 分支不受影响，padding 仍是 6dp。
                .padding(horizontal = if (compact) 2.dp else 6.dp, vertical = 3.dp),
        ) {
            // 等待段：淡色斜纹 + 随分级自适应的 waitText（"候场" / "候场 · 时间" / "候场 · 地点 · 时间"）
            if (hasWaitSegment) {
                Column(
                    Modifier
                        .fillMaxWidth()
                        .height(waitSegmentHeightDp)
                        .clip(RoundedCornerShape(4.dp))
                        .background(waitSegmentBrush(colorArgb))
                        .padding(horizontal = 4.dp, vertical = 2.dp),
                    verticalArrangement = Arrangement.Center,
                ) {
                    if (showTier == 4) {
                        // 候场区两行：第 1 行"候场 · 开始时间"，第 2 行地点（充分利用空间）
                        Text(
                            text = "候场 · ${formatStart(event)}",
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
                    } else {
                        Text(
                            text = waitText,
                            style = MaterialTheme.typography.labelSmall,
                            fontStyle = FontStyle.Italic,
                            fontWeight = FontWeight.Bold,
                            color = textColor.copy(alpha = 0.9f),
                            maxLines = 1,
                            overflow = TextOverflow.Ellipsis,
                        )
                    }
                }
                Spacer(Modifier.height(2.dp))
            }
            if (showTier == 0) {
                // —— 原有 compact / 非 compact 逻辑保持不变 ——
                if (compact) {
                    // 太矮：一行只塞标题（+ 非窄栏下还能塞开始时间）。不放类别 chip——
                    // 左侧 4dp 色条已经传达了类别，chip 是非必要元素；在窄栏（栏2
                    // ≈18%宽）里 chip 若排在前面且不参与 weight 分配，会把标题挤到
                    // 0 宽度，导致标题和时间都被 clip() 裁掉、什么都看不见。标题用
                    // weight(1f, fill=true) 优先拿到剩余宽度；标题字号用 labelSmall
                    // （比非 compact 分支的 bodySmall 小 1sp）进一步给窄栏腾空间。
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        Text(
                            text = event.title.ifEmpty { category?.name ?: "" },
                            style = MaterialTheme.typography.labelSmall,
                            fontWeight = FontWeight.Medium,
                            color = textColor,
                            maxLines = 1,
                            overflow = TextOverflow.Ellipsis,
                            modifier = Modifier.weight(1f),
                        )
                        // 需求8：矮卡片（<44dp，常见于 30~40min 的短事件）此前完全没有渲染
                        // 冲突角标，导致"生成计划表"发现冲突后卡片上什么都看不出来。
                        // 角标是定长的"!"，即使在窄栏也只占极小宽度，直接加在标题右侧。
                        if (hasConflict) {
                            Text(
                                "!",
                                style = MaterialTheme.typography.labelSmall,
                                fontWeight = FontWeight.Bold,
                                color = MaterialTheme.colorScheme.error,
                                modifier = Modifier.padding(start = 2.dp),
                            )
                        }
                        // 栏2 太窄，标题 + 时间两个元素挤在一起时间会把标题压到只剩
                        // 一两个字。时间在这里是冗余信息——卡片在时间轴上的竖直位置
                        // 本身就对齐着起始时间的刻度线，栏2 里舍弃它，把宽度全部留给
                        // 标题，换取"看得出是什么事"这个更重要的信息。
                        if (!narrow) {
                            Text(
                                text = formatStart(event),
                                style = MaterialTheme.typography.labelSmall,
                                color = textColor.copy(alpha = 0.75f),
                                maxLines = 1,
                                modifier = Modifier.padding(start = 2.dp),
                            )
                        }
                    }
                } else {
                    // 第1行
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        category?.let {
                            CategoryChip(it.name, colorArgb)
                            Spacer(Modifier.width(4.dp))
                        }
                        Text(
                            text = event.title.ifEmpty { category?.name ?: "" },
                            style = MaterialTheme.typography.bodySmall,
                            fontWeight = FontWeight.SemiBold,
                            color = textColor,
                            maxLines = 1,
                            overflow = TextOverflow.Ellipsis,
                            modifier = Modifier.weight(1f),
                        )
                        if (hasConflict) {
                            Text(
                                "!",
                                style = MaterialTheme.typography.labelSmall,
                                fontWeight = FontWeight.Bold,
                                color = MaterialTheme.colorScheme.error,
                            )
                        }
                    }
                    // 第2/3行：地点与时间。等待段已显示过它们（show 事件），此处省略避免重复。
                    if (!hasWaitSegment) {
                        // 第2行：地点。deck 为 null 时整行不显示（登船/下船）
                        location?.let {
                            Text(
                                text = it,
                                style = MaterialTheme.typography.labelSmall,
                                color = textColor.copy(alpha = 0.85f),
                                maxLines = 1,
                                overflow = TextOverflow.Ellipsis,
                            )
                        }
                        // 第3行：时间 + 时长
                        Text(
                            text = formatTimeRange(event),
                            style = MaterialTheme.typography.labelSmall,
                            color = textColor.copy(alpha = 0.7f),
                            maxLines = 1,
                        )
                    }
                }
            } else if (showTier == 1 || showTier == 4) {
                // 本体只够 1 行：演出名（labelSmall），地点/时间都在候场段
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Text(
                        text = event.title.ifEmpty { category?.name ?: "" },
                        style = MaterialTheme.typography.labelSmall,
                        fontWeight = FontWeight.Medium,
                        color = textColor,
                        maxLines = 1,
                        overflow = TextOverflow.Ellipsis,
                        modifier = Modifier.weight(1f),
                    )
                    if (hasConflict) {
                        Text(
                            "!",
                            style = MaterialTheme.typography.labelSmall,
                            fontWeight = FontWeight.Bold,
                            color = MaterialTheme.colorScheme.error,
                            modifier = Modifier.padding(start = 2.dp),
                        )
                    }
                }
            } else {
                // 第 1 行：类别 chip + 演出名
                Row(verticalAlignment = Alignment.CenterVertically) {
                    category?.let {
                        CategoryChip(it.name, colorArgb)
                        Spacer(Modifier.width(4.dp))
                    }
                    Text(
                        text = event.title.ifEmpty { category?.name ?: "" },
                        style = MaterialTheme.typography.bodySmall,
                        fontWeight = FontWeight.SemiBold,
                        color = textColor,
                        maxLines = 1,
                        overflow = TextOverflow.Ellipsis,
                        modifier = Modifier.weight(1f),
                    )
                    if (hasConflict) {
                        Text(
                            "!",
                            style = MaterialTheme.typography.labelSmall,
                            fontWeight = FontWeight.Bold,
                            color = MaterialTheme.colorScheme.error,
                        )
                    }
                }
                // 第 2 行：地点（tier >= 2 都有）
                location?.let {
                    Text(
                        text = it,
                        style = MaterialTheme.typography.labelSmall,
                        color = textColor.copy(alpha = 0.85f),
                        maxLines = 1,
                        overflow = TextOverflow.Ellipsis,
                    )
                }
                // 第 3 行：时间范围（仅 tier 3）
                if (showTier == 3) {
                    Text(
                        text = formatTimeRange(event),
                        style = MaterialTheme.typography.labelSmall,
                        color = textColor.copy(alpha = 0.7f),
                        maxLines = 1,
                    )
                }
            }
        }
    }
}

private fun formatStart(event: CruiseEvent): String =
    "%02d:%02d".format(event.startTime.hour, event.startTime.minute)

/** "13:15 – 13:45  30m" / 超过 1 小时 "1h 30m" */
private fun formatTimeRange(event: CruiseEvent): String {
    val endMin = event.endMinute
    val end = "%02d:%02d".format((endMin / 60) % 24, endMin % 60)
    return "${formatStart(event)} – $end  ${formatDuration(event.durationMinutes)}"
}

fun formatDuration(minutes: Int): String = when {
    minutes < 60 -> "${minutes}m"
    minutes % 60 == 0 -> "${minutes / 60}h"
    else -> "${minutes / 60}h ${minutes % 60}m"
}

/** 等待段斜纹背景：类别色淡化的 45° 条纹 */
private fun waitSegmentBrush(colorArgb: Int): Brush {
    val base = Color(colorArgb).copy(alpha = 0.22f)
    val stripe = Color(colorArgb).copy(alpha = 0.10f)
    return Brush.linearGradient(
        colors = listOf(base, stripe, base, stripe, base),
        start = Offset.Zero,
        end = Offset.Infinite,
    )
}

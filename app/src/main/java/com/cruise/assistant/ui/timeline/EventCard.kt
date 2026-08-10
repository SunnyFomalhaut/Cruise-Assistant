package com.cruise.assistant.ui.timeline

import androidx.compose.foundation.background
import androidx.compose.foundation.combinedClickable
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxHeight
import androidx.compose.foundation.layout.fillMaxWidth
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
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.Dp
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
    val location = formatLocation(event.deck, event.zone, event.placeName)

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
            if (compact) {
                // 太矮：一行只塞标题（+ 非窄栏下还能塞开始时间）。不放类别 chip——
                // 左侧 4dp 色条已经传达了类别，chip 是非必要元素；在窄栏（栏2
                // ≈18%宽）里 chip 若排在前面且不参与 weight 分配，会把标题挤到
                // 0 宽度，导致标题和时间都被 clip() 裁掉、什么都看不见。标题用
                // weight(1f, fill=true) 优先拿到剩余宽度；标题字号用 labelSmall
                // （比非 compact 分支的 bodySmall 小 1sp）进一步给窄栏腾空间。
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Text(
                        text = event.title,
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
                        text = event.title,
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
                // 第2行：地点。deck 为 null 时整行不显示（上船/下船）
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

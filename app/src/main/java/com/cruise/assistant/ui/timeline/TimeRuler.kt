package com.cruise.assistant.ui.timeline

import androidx.compose.foundation.Canvas
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.offset
import androidx.compose.foundation.layout.width
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.unit.dp
import com.cruise.assistant.domain.TimelineLayout
import com.cruise.assistant.domain.hourTicks
import com.cruise.assistant.domain.minuteToDp
import com.cruise.assistant.ui.theme.GridLine

val RULER_WIDTH = 56.dp

/** 左侧时间刻度列。文字基线对齐刻度线；折叠段位置标一个 ⋯。 */
@Composable
fun TimeRuler(layout: TimelineLayout, modifier: Modifier = Modifier) {
    Box(modifier.width(RULER_WIDTH).height(layout.totalHeightDp.dp)) {
        layout.hourTicks().forEach { (minute, topDp, isHalf) ->
            Text(
                text = if (isHalf) "%d:%02d".format(minute / 60, minute % 60) else "%02d:00".format(minute / 60),
                // 半点用更小更浅的样式，与整点区分
                style = if (isHalf) MaterialTheme.typography.labelSmall else MaterialTheme.typography.bodySmall,
                color = if (isHalf) {
                    MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.55f)
                } else {
                    MaterialTheme.colorScheme.onSurfaceVariant
                },
                modifier = Modifier.offset(x = 8.dp, y = topDp.dp - 8.dp),
            )
        }
        // 折叠掉的空时段标个 ⋯，让用户知道这里被压缩了
        layout.segments.filter { it.collapsed }.forEach { segment ->
            Text(
                text = "⋯",
                style = MaterialTheme.typography.bodySmall,
                color = Color.Gray,
                modifier = Modifier.offset(
                    x = 20.dp,
                    y = (segment.topDp + segment.heightDp / 2f).dp - 8.dp,
                ),
            )
        }
    }
}

/** 事件区域背景：水平整点网格线 + 3 栏竖分隔线（截图1 的观感） */
@Composable
fun TimelineGrid(
    layout: TimelineLayout,
    laneBoundaries: List<Float>,
    modifier: Modifier = Modifier,
) {
    val ticks = layout.hourTicks()
    Canvas(modifier.fillMaxSize()) {
        // 横向网格线只画整点（规格 §1），半点不画线
        ticks.filter { !it.third }.forEach { (_, topDp) ->
            val y = topDp * density
            drawLine(
                color = GridLine,
                start = Offset(0f, y),
                end = Offset(size.width, y),
                strokeWidth = 1f,
            )
        }
        laneBoundaries.forEach { fraction ->
            val x = size.width * fraction
            drawLine(
                color = GridLine.copy(alpha = 0.6f),
                start = Offset(x, 0f),
                end = Offset(x, size.height),
                strokeWidth = 1f,
            )
        }
    }
}

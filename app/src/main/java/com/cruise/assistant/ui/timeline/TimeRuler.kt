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
import androidx.compose.runtime.remember
import androidx.compose.ui.Modifier
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.cruise.assistant.domain.TimelineLayout
import com.cruise.assistant.domain.hourTicks
import com.cruise.assistant.domain.minuteToDp
import com.cruise.assistant.ui.theme.GridLine
import com.cruise.assistant.ui.theme.GridLineHour
import com.cruise.assistant.ui.theme.Slate400
import com.cruise.assistant.ui.theme.Slate700
import com.cruise.assistant.ui.theme.Slate900

val RULER_WIDTH = 52.dp

/** 左侧时间刻度列。文字基线对齐刻度线；折叠段位置标一个 ⋯。 */
@Composable
fun TimeRuler(layout: TimelineLayout, modifier: Modifier = Modifier) {
    val ticks = remember(layout) { layout.hourTicks() }
    val collapsedSegments = remember(layout) { layout.segments.filter { it.collapsed } }

    Box(modifier.width(RULER_WIDTH).height(layout.totalHeightDp.dp)) {
        ticks.forEach { (minute, topDp, isHalf) ->
            Text(
                text = if (isHalf) "%d:%02d".format(minute / 60, minute % 60) else "%02d:00".format(minute / 60),
                // 半点用更小更浅的样式，与整点区分
                style = if (isHalf) MaterialTheme.typography.labelSmall.copy(fontSize = 10.sp) else MaterialTheme.typography.labelMedium,
                fontWeight = if (isHalf) FontWeight.Normal else FontWeight.SemiBold,
                color = if (isHalf) Slate400 else Slate700,
                modifier = Modifier.offset(x = 6.dp, y = topDp.dp - 8.dp),
            )
        }
        // 折叠掉的空时段标个 ⋯，让用户知道这里被压缩了
        collapsedSegments.forEach { segment ->
            Text(
                text = "⋯",
                style = MaterialTheme.typography.bodySmall,
                color = Slate400,
                modifier = Modifier.offset(
                    x = 18.dp,
                    y = (segment.topDp + segment.heightDp / 2f).dp - 8.dp,
                ),
            )
        }
    }
}

/** 事件区域背景：水平整点网格线 + 3 栏竖分隔线 */
@Composable
fun TimelineGrid(
    layout: TimelineLayout,
    laneBoundaries: List<Float>,
    modifier: Modifier = Modifier,
) {
    val ticks = remember(layout) { layout.hourTicks() }
    val boundaries = remember(laneBoundaries) { laneBoundaries }

    Canvas(modifier.fillMaxSize()) {
        val width = size.width
        val height = size.height

        // 横向网格线：整点 + 半点都画线。整点用更清晰的线，半点用细线
        ticks.forEach { (_, topDp, isHalf) ->
            val y = topDp * density
            drawLine(
                color = if (isHalf) GridLine else GridLineHour,
                start = Offset(0f, y),
                end = Offset(width, y),
                strokeWidth = if (isHalf) 1f else 1.5f,
            )
        }
        boundaries.forEach { fraction ->
            val x = width * fraction
            drawLine(
                color = GridLineHour.copy(alpha = 0.8f),
                start = Offset(x, 0f),
                end = Offset(x, height),
                strokeWidth = 1f,
            )
        }
    }
}



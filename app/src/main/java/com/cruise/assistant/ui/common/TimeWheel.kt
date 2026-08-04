package com.cruise.assistant.ui.common

import androidx.compose.foundation.background
import androidx.compose.foundation.gestures.snapping.rememberSnapFlingBehavior
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.rememberLazyListState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.derivedStateOf
import androidx.compose.runtime.getValue
import androidx.compose.runtime.remember
import androidx.compose.runtime.snapshotFlow
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import java.time.LocalTime
import kotlin.math.roundToInt

private val ROW_HEIGHT = 44.dp

/** 截图2 的时间滚轮：小时列 + 分钟列，中间行为选中值 */
@Composable
fun TimeWheel(
    value: LocalTime,
    onValueChange: (LocalTime) -> Unit,
    modifier: Modifier = Modifier,
    minuteStep: Int = 5,
) {
    val minuteValues = remember(minuteStep) { (0 until 60 step minuteStep).toList() }

    Box(
        modifier = modifier
            .clip(RoundedCornerShape(16.dp))
            .background(MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.4f)),
        contentAlignment = Alignment.Center,
    ) {
        // 中间高亮条
        Box(
            Modifier
                .fillMaxWidth()
                .height(ROW_HEIGHT)
                .background(MaterialTheme.colorScheme.surfaceVariant),
        )
        Row(
            Modifier.padding(vertical = 4.dp),
            horizontalArrangement = Arrangement.Center,
        ) {
            WheelColumn(
                items = (0..23).toList(),
                selected = value.hour,
                onSelect = { onValueChange(value.withHour(it)) },
                format = { "%02d".format(it) },
                modifier = Modifier.width(72.dp),
            )
            Text(
                ":",
                fontSize = 22.sp,
                fontWeight = FontWeight.Bold,
                modifier = Modifier.align(Alignment.CenterVertically),
            )
            WheelColumn(
                items = minuteValues,
                selected = minuteValues.minByOrNull { kotlin.math.abs(it - value.minute) }
                    ?: 0,
                onSelect = { onValueChange(value.withMinute(it)) },
                format = { "%02d".format(it) },
                modifier = Modifier.width(72.dp),
            )
        }
    }
}

@Composable
private fun WheelColumn(
    items: List<Int>,
    selected: Int,
    onSelect: (Int) -> Unit,
    format: (Int) -> String,
    modifier: Modifier = Modifier,
) {
    val selectedIndex = items.indexOf(selected).coerceAtLeast(0)
    val state = rememberLazyListState(initialFirstVisibleItemIndex = selectedIndex)
    val density = LocalDensity.current
    val rowHeightPx = remember(density) { with(density) { ROW_HEIGHT.toPx() } }

    // 外部值变化时同步滚动
    LaunchedEffect(selected) {
        val target = items.indexOf(selected).coerceAtLeast(0)
        if (state.firstVisibleItemIndex != target) state.scrollToItem(target)
    }

    // 当前对准中间行的条目：由 firstVisibleItemIndex 加上滚动偏移的整行数修正得到，
    // 静止吸附时 offset 恒为 0，此时 centeredIndex == firstVisibleItemIndex（居中行）。
    val centeredIndex by remember(items) {
        derivedStateOf {
            val rounded = (state.firstVisibleItemScrollOffset / rowHeightPx).roundToInt()
            (state.firstVisibleItemIndex + rounded).coerceIn(0, items.lastIndex)
        }
    }

    // 滚动停止后回调选中值
    LaunchedEffect(state) {
        snapshotFlow { state.isScrollInProgress to centeredIndex }
            .collect { (scrolling, index) ->
                if (!scrolling && items[index] != selected) onSelect(items[index])
            }
    }

    LazyColumn(
        state = state,
        modifier = modifier.height(ROW_HEIGHT * 3),
        flingBehavior = rememberSnapFlingBehavior(state),
        contentPadding = PaddingValues(vertical = ROW_HEIGHT),
    ) {
        items(items.size) { index ->
            val isSelected = index == centeredIndex
            Box(
                Modifier.fillMaxWidth().height(ROW_HEIGHT),
                contentAlignment = Alignment.Center,
            ) {
                Text(
                    text = format(items[index]),
                    fontSize = if (isSelected) 24.sp else 18.sp,
                    fontWeight = if (isSelected) FontWeight.Bold else FontWeight.Normal,
                    color = if (isSelected) {
                        MaterialTheme.colorScheme.onSurface
                    } else {
                        MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.5f)
                    },
                    textAlign = TextAlign.Center,
                )
            }
        }
    }
}

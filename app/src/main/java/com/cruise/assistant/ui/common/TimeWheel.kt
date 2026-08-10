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
import androidx.compose.runtime.rememberUpdatedState
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
    // 循环滚动：用大倍数假装无限列表，下标对 items.size 取模即可首尾相接。
    // REPEAT 选 Int.MAX_VALUE 让用户怎么滚都摸不到边界；
    // 初始 startPosition 居中落在一个远离边界的 chunk，避免滚动到 list 末端触底。
    val realSize = items.size
    val initialLogicalIndex = remember(selected, realSize) {
        // 把初始位置定位到"中间附近且映射回 selected"的地方：
        // 用一个固定的大偏移 (1_000_000 * realSize) + selected，确保物理下标远离 0/MaxValue 边界。
        val base = 1_000_000L * realSize
        (base + items.indexOf(selected).coerceAtLeast(0)).toInt()
    }
    val state = rememberLazyListState(initialFirstVisibleItemIndex = initialLogicalIndex)
    val density = LocalDensity.current
    val rowHeightPx = remember(density) { with(density) { ROW_HEIGHT.toPx() } }

    // rememberUpdatedState 保证 LaunchedEffect(state) 长期协程里读到的
    // selected / onSelect 始终是最新值，而非首次组合时的旧快照。
    // 之前 onSelect 捕获了旧的 value(LocalTime)，导致先改小时再滚分钟时
    // withMinute(it) 把小时回退成旧值（如无法选择 11:45）。
    val currentSelected = rememberUpdatedState(selected)
    val currentOnSelect = rememberUpdatedState(onSelect)

    // 外部值变化时同步滚动到对齐该值的最近 logical index（保持在当前可视区附近，
    // 不跳到 1_000_000 那个远端 chunk，避免视觉跳变）。
    LaunchedEffect(selected) {
        if (realSize == 0) return@LaunchedEffect
        val targetRealIndex = items.indexOf(selected).coerceAtLeast(0)
        // 计算当前可视区中心对应的目标 real index 与可视中心 raw 的差距，
        // 选一个最近的等价 raw 去 scrollToItem。
        val centeredRawApprox = state.firstVisibleItemIndex +
            (state.firstVisibleItemScrollOffset / rowHeightPx).roundToInt()
        val centeredRealApprox = ((centeredRawApprox % realSize) + realSize) % realSize
        val diffToTarget = ((targetRealIndex - centeredRealApprox + realSize / 2 + realSize) % realSize) - realSize / 2
        val targetRaw = centeredRawApprox + diffToTarget
        if (state.firstVisibleItemIndex != targetRaw) state.scrollToItem(targetRaw)
    }

    // 当前对准中间行的条目：raw index 对 realSize 取模得到真实 item 下标。
    val centeredIndex by remember(items) {
        derivedStateOf {
            val rawCentered = state.firstVisibleItemIndex +
                (state.firstVisibleItemScrollOffset / rowHeightPx).roundToInt()
            ((rawCentered % realSize) + realSize) % realSize
        }
    }

    // 滚动停止后回调选中值
    LaunchedEffect(state) {
        snapshotFlow { state.isScrollInProgress to centeredIndex }
            .collect { (scrolling, index) ->
                if (!scrolling && items[index] != currentSelected.value) currentOnSelect.value(items[index])
            }
    }

    LazyColumn(
        state = state,
        modifier = modifier.height(ROW_HEIGHT * 3),
        flingBehavior = rememberSnapFlingBehavior(state),
        contentPadding = PaddingValues(vertical = ROW_HEIGHT),
    ) {
        items(Int.MAX_VALUE) { rawIndex ->
            val index = ((rawIndex % realSize) + realSize) % realSize
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

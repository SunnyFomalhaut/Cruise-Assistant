package com.cruise.assistant.ui.common

import androidx.compose.foundation.background
import androidx.compose.foundation.border
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
import com.cruise.assistant.ui.theme.CruiseAzureContainer
import com.cruise.assistant.ui.theme.CruiseMarine
import com.cruise.assistant.ui.theme.Slate100
import com.cruise.assistant.ui.theme.Slate200
import com.cruise.assistant.ui.theme.Slate400
import com.cruise.assistant.ui.theme.Slate900
import java.time.LocalTime
import kotlin.math.roundToInt

private val ROW_HEIGHT = 44.dp

/** 时间滚轮：小时列 + 分钟列，中间行为选中值 */
@Composable
fun TimeWheel(
    value: LocalTime,
    onValueChange: (LocalTime) -> Unit,
    modifier: Modifier = Modifier,
    minuteStep: Int = 5,
) {
    val minuteValues = remember(minuteStep) { (0 until 60 step minuteStep).toList() }
    val shape = RoundedCornerShape(14.dp)

    Box(
        modifier = modifier
            .clip(shape)
            .background(Slate100)
            .border(1.dp, Slate200, shape),
        contentAlignment = Alignment.Center,
    ) {
        // 中间高亮胶囊视窗
        Box(
            Modifier
                .fillMaxWidth()
                .padding(horizontal = 6.dp)
                .height(ROW_HEIGHT - 6.dp)
                .clip(RoundedCornerShape(10.dp))
                .background(CruiseAzureContainer),
        )
        Row(
            Modifier.padding(vertical = 4.dp),
            horizontalArrangement = Arrangement.Center,
            verticalAlignment = Alignment.CenterVertically,
        ) {
            WheelColumn(
                items = (0..23).toList(),
                selected = value.hour,
                onSelect = { onValueChange(value.withHour(it)) },
                format = { "%02d".format(it) },
                modifier = Modifier.width(64.dp),
            )
            Text(
                ":",
                fontSize = 20.sp,
                fontWeight = FontWeight.Bold,
                color = CruiseMarine,
                modifier = Modifier.align(Alignment.CenterVertically),
            )
            WheelColumn(
                items = minuteValues,
                selected = minuteValues.minByOrNull { kotlin.math.abs(it - value.minute) }
                    ?: 0,
                onSelect = { onValueChange(value.withMinute(it)) },
                format = { "%02d".format(it) },
                modifier = Modifier.width(64.dp),
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
    val realSize = items.size
    val initialLogicalIndex = remember(selected, realSize) {
        val base = 1_000_000L * realSize
        (base + items.indexOf(selected).coerceAtLeast(0)).toInt()
    }
    val state = rememberLazyListState(initialFirstVisibleItemIndex = initialLogicalIndex)
    val density = LocalDensity.current
    val rowHeightPx = remember(density) { with(density) { ROW_HEIGHT.toPx() } }

    val currentSelected = rememberUpdatedState(selected)
    val currentOnSelect = rememberUpdatedState(onSelect)

    LaunchedEffect(selected) {
        if (realSize == 0) return@LaunchedEffect
        val targetRealIndex = items.indexOf(selected).coerceAtLeast(0)
        val centeredRawApprox = state.firstVisibleItemIndex +
            (state.firstVisibleItemScrollOffset / rowHeightPx).roundToInt()
        val centeredRealApprox = ((centeredRawApprox % realSize) + realSize) % realSize
        val diffToTarget = ((targetRealIndex - centeredRealApprox + realSize / 2 + realSize) % realSize) - realSize / 2
        val targetRaw = centeredRawApprox + diffToTarget
        if (state.firstVisibleItemIndex != targetRaw) state.scrollToItem(targetRaw)
    }

    val centeredIndex by remember(items) {
        derivedStateOf {
            val rawCentered = state.firstVisibleItemIndex +
                (state.firstVisibleItemScrollOffset / rowHeightPx).roundToInt()
            ((rawCentered % realSize) + realSize) % realSize
        }
    }

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
                    fontSize = if (isSelected) 22.sp else 16.sp,
                    fontWeight = if (isSelected) FontWeight.Bold else FontWeight.Medium,
                    color = if (isSelected) CruiseMarine else Slate400,
                    textAlign = TextAlign.Center,
                )
            }
        }
    }
}


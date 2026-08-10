package com.cruise.assistant.ui.timeline

import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.lazy.rememberLazyListState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import java.time.LocalDate
import java.time.format.TextStyle
import java.util.Locale

private val CHIP_WIDTH = 56.dp
private val CHIP_HEIGHT = 60.dp

/**
 * 顶部日期条：每个日期一个圆角方框。
 * 方框内两行：日序（"第 N 天" 省略为 N）+ 周几单字（一/二/.../日）。
 * 选中态：主色背景 12% 透明 + 主色加粗文字。
 * 已定稿态：在选中态之外额外加细边框（主色 50%）以区分。
 */
@Composable
fun DateStrip(
    dates: List<LocalDate>,
    selectedDate: LocalDate,
    finalizedDates: Set<LocalDate>,
    onSelect: (LocalDate) -> Unit,
    modifier: Modifier = Modifier,
) {
    val state = rememberLazyListState()
    val selectedIndex = dates.indexOf(selectedDate)
    LaunchedEffect(selectedIndex) {
        if (selectedIndex >= 0) state.animateScrollToItem(selectedIndex.coerceAtLeast(0))
    }

    LazyRow(
        state = state,
        modifier = modifier.padding(vertical = 4.dp),
        horizontalArrangement = Arrangement.spacedBy(8.dp),
        contentPadding = PaddingValues(horizontal = 12.dp),
    ) {
        items(dates) { date ->
            val isSelected = date == selectedDate
            val isFinalized = date in finalizedDates
            val dayIndex = dates.indexOf(date) + 1
            val weekDayLabel = date.dayOfWeek.getDisplayName(TextStyle.NARROW, Locale.CHINA)

            val shape = RoundedCornerShape(12.dp)
            Column(
                horizontalAlignment = Alignment.CenterHorizontally,
                verticalArrangement = Arrangement.Center,
                modifier = Modifier
                    .width(CHIP_WIDTH)
                    .height(CHIP_HEIGHT)
                    .clip(shape)
                    .background(
                        if (isSelected) {
                            MaterialTheme.colorScheme.primary.copy(alpha = 0.12f)
                        } else {
                            MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.3f)
                        },
                    )
                    .then(
                        if (isFinalized && !isSelected) {
                            Modifier.border(
                                width = 1.dp,
                                color = MaterialTheme.colorScheme.primary.copy(alpha = 0.5f),
                                shape = shape,
                            )
                        } else if (isFinalized && isSelected) {
                            Modifier.border(
                                width = 1.dp,
                                color = MaterialTheme.colorScheme.primary,
                                shape = shape,
                            )
                        } else {
                            Modifier
                        },
                    )
                    .clickable { onSelect(date) }
                    .padding(vertical = 4.dp),
            ) {
                Text(
                    text = "$dayIndex",
                    style = MaterialTheme.typography.titleMedium,
                    fontWeight = if (isSelected) FontWeight.Bold else FontWeight.SemiBold,
                    color = if (isSelected) {
                        MaterialTheme.colorScheme.primary
                    } else {
                        MaterialTheme.colorScheme.onSurface
                    },
                    textAlign = TextAlign.Center,
                )
                Text(
                    text = weekDayLabel,
                    style = MaterialTheme.typography.labelSmall,
                    fontWeight = if (isSelected) FontWeight.Bold else FontWeight.Normal,
                    color = if (isSelected) {
                        MaterialTheme.colorScheme.primary
                    } else {
                        MaterialTheme.colorScheme.onSurfaceVariant
                    },
                    textAlign = TextAlign.Center,
                )
            }
        }
    }
}
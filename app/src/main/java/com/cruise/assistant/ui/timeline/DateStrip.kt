package com.cruise.assistant.ui.timeline

import androidx.compose.animation.animateColorAsState
import androidx.compose.animation.core.tween
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.lazy.rememberLazyListState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.shadow
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.cruise.assistant.ui.theme.CruiseAzure
import com.cruise.assistant.ui.theme.CruiseAzureContainer
import com.cruise.assistant.ui.theme.CruiseMarine
import com.cruise.assistant.ui.theme.Slate200
import com.cruise.assistant.ui.theme.Slate400
import com.cruise.assistant.ui.theme.Slate700
import com.cruise.assistant.ui.theme.Slate900
import java.time.LocalDate
import java.time.format.TextStyle
import java.util.Locale

private val CHIP_WIDTH = 58.dp
private val CHIP_HEIGHT = 66.dp

/**
 * 顶部日期条：每个日期一个现代圆角胶囊卡片。
 * 上部：日序（"Day 1" 或大字 "1"）
 * 中部：周几/日期
 * 底部：定稿状态指示点（实心代表定稿）
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
        contentPadding = PaddingValues(horizontal = 14.dp),
    ) {
        items(dates) { date ->
            val isSelected = date == selectedDate
            val isFinalized = date in finalizedDates
            val dayIndex = dates.indexOf(date) + 1
            val weekDayLabel = date.dayOfWeek.getDisplayName(TextStyle.SHORT, Locale.CHINA)
            val shape = RoundedCornerShape(16.dp)

            val targetBg = if (isSelected) CruiseAzureContainer else MaterialTheme.colorScheme.surface
            val targetBorder = when {
                isSelected -> CruiseAzure
                isFinalized -> CruiseMarine.copy(alpha = 0.4f)
                else -> Slate200
            }

            val animatedBg by animateColorAsState(targetValue = targetBg, animationSpec = tween(200), label = "dateBg")
            val animatedBorder by animateColorAsState(targetValue = targetBorder, animationSpec = tween(200), label = "dateBorder")

            Column(
                horizontalAlignment = Alignment.CenterHorizontally,
                verticalArrangement = Arrangement.SpaceBetween,
                modifier = Modifier
                    .width(CHIP_WIDTH)
                    .height(CHIP_HEIGHT)
                    .then(if (isSelected) Modifier.shadow(3.dp, shape, spotColor = CruiseAzure.copy(alpha = 0.25f)) else Modifier)
                    .clip(shape)
                    .background(animatedBg)
                    .border(
                        width = if (isSelected) 1.5.dp else 1.dp,
                        color = animatedBorder,
                        shape = shape,
                    )
                    .clickable { onSelect(date) }
                    .padding(vertical = 6.dp, horizontal = 4.dp),
            ) {
                Text(
                    text = "D$dayIndex",
                    style = MaterialTheme.typography.labelSmall,
                    fontWeight = if (isSelected) FontWeight.Bold else FontWeight.Medium,
                    color = if (isSelected) CruiseMarine else Slate400,
                    textAlign = TextAlign.Center,
                )
                Text(
                    text = "${date.dayOfMonth}",
                    fontSize = 17.sp,
                    fontWeight = if (isSelected) FontWeight.Bold else FontWeight.SemiBold,
                    color = if (isSelected) CruiseMarine else Slate900,
                    textAlign = TextAlign.Center,
                    lineHeight = 18.sp,
                )
                Text(
                    text = weekDayLabel,
                    style = MaterialTheme.typography.labelSmall,
                    fontSize = 10.sp,
                    fontWeight = if (isSelected) FontWeight.Bold else FontWeight.Normal,
                    color = if (isSelected) CruiseMarine else Slate700,
                    textAlign = TextAlign.Center,
                )
                if (isFinalized) {
                    Box(
                        Modifier
                            .size(4.dp)
                            .clip(CircleShape)
                            .background(if (isSelected) CruiseMarine else CruiseAzure.copy(alpha = 0.7f)),
                    )
                } else {
                    Spacer(Modifier.height(4.dp))
                }
            }
        }
    }
}

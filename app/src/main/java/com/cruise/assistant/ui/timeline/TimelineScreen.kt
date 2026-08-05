package com.cruise.assistant.ui.timeline

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.outlined.Schedule
import androidx.compose.material.icons.outlined.Settings
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.DropdownMenu
import androidx.compose.material3.DropdownMenuItem
import androidx.compose.material3.FloatingActionButton
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import com.cruise.assistant.R
import com.cruise.assistant.data.repo.RecurrenceScope
import com.cruise.assistant.domain.CruiseEvent
import com.cruise.assistant.domain.LANE_COUNT
import com.cruise.assistant.domain.minuteToDp
import java.time.LocalDate
import java.time.format.DateTimeFormatter
import java.time.format.TextStyle
import java.util.Locale

private val HEADER_FMT = DateTimeFormatter.ofPattern("M月d日")

@Composable
fun TimelineScreen(
    state: TimelineUiState,
    onSelectDate: (LocalDate) -> Unit,
    onOpenSettings: () -> Unit,
    onOpenDisplayRange: () -> Unit,
    onAddEvent: (Int) -> Unit,
    onEditEvent: (CruiseEvent) -> Unit,
    onMoveToLane: (Long, Int) -> Unit,
    onDeleteEvent: (CruiseEvent, RecurrenceScope) -> Unit,
    onGeneratePlan: () -> Unit,
    onUnlockDay: () -> Unit,
    onToggleFullDay: () -> Unit,
    conflictIds: Set<Long>,
) {
    val scrollState = rememberScrollState()
    var menuEvent by remember { mutableStateOf<CruiseEvent?>(null) }

    // 打开当天时自动滚到"现在"附近
    LaunchedEffect(state.selectedDate, state.isToday) {
        if (state.isToday) {
            val y = state.layout.minuteToDp(state.nowMinute)
            scrollState.animateScrollTo((y - 200f).coerceAtLeast(0f).toInt())
        }
    }

    Scaffold(
        floatingActionButton = {
            if (!state.isFinalized) {
                FloatingActionButton(onClick = { onAddEvent(defaultStartMinute(state)) }) {
                    Icon(Icons.Filled.Add, contentDescription = stringResource(R.string.add_event))
                }
            }
        },
    ) { padding ->
        Column(Modifier.padding(padding).fillMaxSize()) {
            // ---- 顶栏 ----
            Row(
                Modifier.fillMaxWidth().padding(horizontal = 12.dp, vertical = 6.dp),
                verticalAlignment = Alignment.CenterVertically,
            ) {
                Column(Modifier.weight(1f)) {
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        Text(
                            state.selectedDate.format(HEADER_FMT),
                            style = MaterialTheme.typography.titleLarge,
                        )
                        Spacer(Modifier.width(6.dp))
                        Text(
                            state.selectedDate.dayOfWeek
                                .getDisplayName(TextStyle.SHORT, Locale.CHINA),
                            style = MaterialTheme.typography.bodyMedium,
                            color = MaterialTheme.colorScheme.onSurfaceVariant,
                        )
                    }
                    state.voyage?.dayIndexOf(state.selectedDate)?.let { index ->
                        Text(
                            stringResource(R.string.day_index_format, index, state.voyage.dayCount),
                            style = MaterialTheme.typography.bodySmall,
                            color = MaterialTheme.colorScheme.onSurfaceVariant,
                        )
                    }
                }
                IconButton(onClick = onOpenDisplayRange) {
                    Icon(Icons.Outlined.Schedule, contentDescription = stringResource(R.string.display_range_title))
                }
                IconButton(onClick = onOpenSettings) {
                    Icon(Icons.Outlined.Settings, contentDescription = stringResource(R.string.settings))
                }
            }

            // ---- 日期条 ----
            state.voyage?.let { voyage ->
                DateStrip(
                    dates = voyage.datesInVoyage(),
                    selectedDate = state.selectedDate,
                    finalizedDates = state.dayPlans.filterValues { it.finalized }.keys,
                    onSelect = onSelectDate,
                )
            }

            // ---- 定稿状态条 ----
            Row(
                Modifier.fillMaxWidth().padding(horizontal = 12.dp, vertical = 2.dp),
                verticalAlignment = Alignment.CenterVertically,
            ) {
                if (state.isFinalized) {
                    Text(
                        stringResource(R.string.finalized_badge),
                        style = MaterialTheme.typography.labelSmall,
                        fontWeight = FontWeight.Bold,
                        color = MaterialTheme.colorScheme.primary,
                        modifier = Modifier.weight(1f),
                    )
                    TextButton(onClick = onUnlockDay) { Text(stringResource(R.string.unlock)) }
                } else {
                    Spacer(Modifier.weight(1f))
                    TextButton(onClick = onGeneratePlan) {
                        Text(stringResource(R.string.generate_plan))
                    }
                }
            }

            // ---- 当天横幅 ----
            if (state.isToday) {
                NowBanner(state.ongoing, state.next, state.categories)
            }

            HorizontalDivider()

            // ---- 范围外提示 ----
            if (state.layout.hiddenBefore > 0) {
                HiddenHint(
                    text = stringResource(R.string.hidden_before_format, state.layout.hiddenBefore),
                    onClick = onToggleFullDay,
                )
            }

            // ---- 时间轴主体 ----
            if (state.events.isEmpty()) {
                Box(Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
                    Text(
                        stringResource(R.string.empty_day),
                        style = MaterialTheme.typography.bodyMedium,
                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                    )
                }
            } else {
                Box(Modifier.weight(1f).verticalScroll(scrollState)) {
                    Row(Modifier.fillMaxWidth().height(state.layout.totalHeightDp.dp)) {
                        TimeRuler(state.layout)
                        Box(Modifier.weight(1f)) {
                            TimelineGrid(
                                layout = state.layout,
                                laneBoundaries = if (state.isFinalized) {
                                    emptyList()
                                } else {
                                    laneBoundaryFractions()
                                },
                                modifier = Modifier.fillMaxSize(),
                            )
                            LaneCanvas(
                                layout = state.layout,
                                events = state.events,
                                categories = state.categories,
                                conflictIds = conflictIds,
                                singleLane = state.isFinalized,
                                onEventClick = { event -> if (!state.isFinalized) onEditEvent(event) },
                                onEventLongClick = { event -> if (!state.isFinalized) menuEvent = event },
                                modifier = Modifier.padding(end = 8.dp),
                            )
                            if (state.isToday) {
                                NowIndicator(state.layout, state.nowMinute)
                            }
                        }
                    }
                }
            }

            if (state.layout.hiddenAfter > 0) {
                HiddenHint(
                    text = stringResource(R.string.hidden_after_format, state.layout.hiddenAfter),
                    onClick = onToggleFullDay,
                )
            }
        }
    }

    // ---- 长按菜单：换栏 / 编辑 / 删除 ----
    menuEvent?.let { event ->
        DropdownMenu(expanded = true, onDismissRequest = { menuEvent = null }) {
            if (!state.isFinalized) {
                (0 until LANE_COUNT).filter { it != event.lane }.forEach { lane ->
                    DropdownMenuItem(
                        text = { Text(stringResource(R.string.move_to_lane_format, lane + 1)) },
                        onClick = {
                            onMoveToLane(event.id, lane)
                            menuEvent = null
                        },
                    )
                }
            }
            if (!state.isFinalized) {
                DropdownMenuItem(
                    text = { Text(stringResource(R.string.edit)) },
                    onClick = {
                        onEditEvent(event)
                        menuEvent = null
                    },
                )
            }
            if (!event.isSystemGenerated && !state.isFinalized) {
                DropdownMenuItem(
                    text = { Text(stringResource(R.string.delete)) },
                    onClick = {
                        // 选"仅本日 / 所有重复项"是 Task 8 的弹窗职责，这里先固定仅本日。
                        onDeleteEvent(event, RecurrenceScope.THIS_DAY_ONLY)
                        menuEvent = null
                    },
                )
            }
        }
    }
}

@Composable
private fun HiddenHint(text: String, onClick: () -> Unit) {
    Row(
        Modifier
            .fillMaxWidth()
            .background(MaterialTheme.colorScheme.secondaryContainer.copy(alpha = 0.4f))
            .padding(horizontal = 12.dp, vertical = 4.dp),
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.SpaceBetween,
    ) {
        Text(text, style = MaterialTheme.typography.labelSmall)
        TextButton(onClick = onClick) { Text(stringResource(R.string.show_full_day)) }
    }
}

/** FAB 默认开始时间：当天用下一个整刻，其他天用显示范围起点 */
private fun defaultStartMinute(state: TimelineUiState): Int =
    if (state.isToday) {
        ((state.nowMinute / 15) + 1) * 15
    } else {
        state.layout.rangeStartMinute
    }.coerceIn(0, 1439)

@Composable
fun TimelineLoading() {
    Box(Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
        CircularProgressIndicator()
    }
}

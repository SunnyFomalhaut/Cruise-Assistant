package com.cruise.assistant.ui.timeline

import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.Image
import androidx.compose.foundation.horizontalScroll
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.BoxWithConstraints
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.outlined.AutoAwesome
import androidx.compose.material.icons.outlined.CheckCircle
import androidx.compose.material.icons.outlined.Checklist
import androidx.compose.material.icons.outlined.Explore
import androidx.compose.material.icons.outlined.Map
import androidx.compose.material.icons.outlined.Schedule
import androidx.compose.material.icons.outlined.Settings
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.DropdownMenu
import androidx.compose.material3.DropdownMenuItem
import androidx.compose.material3.FilledTonalButton
import androidx.compose.material3.FloatingActionButton
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Scaffold
import androidx.compose.material3.SmallFloatingActionButton
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import kotlinx.coroutines.launch
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.asImageBitmap
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.Density
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.cruise.assistant.R
import com.cruise.assistant.data.repo.RecurrenceScope
import com.cruise.assistant.domain.CruiseEvent
import com.cruise.assistant.domain.LANE_COUNT
import com.cruise.assistant.domain.TimelineLayout
import com.cruise.assistant.domain.WallpaperManager
import com.cruise.assistant.domain.minuteToDp
import com.cruise.assistant.domain.nearestHourAtDp
import com.cruise.assistant.ui.theme.CruiseAzure
import com.cruise.assistant.ui.theme.CruiseAzureContainer
import com.cruise.assistant.ui.theme.CruiseMarine
import com.cruise.assistant.ui.theme.Slate100
import com.cruise.assistant.ui.theme.Slate200
import com.cruise.assistant.ui.theme.Slate500
import com.cruise.assistant.ui.theme.Slate700
import com.cruise.assistant.ui.theme.Slate900
import java.time.LocalDate
import java.time.format.DateTimeFormatter
import java.time.format.TextStyle
import java.util.Locale

private val HEADER_FMT = DateTimeFormatter.ofPattern("M月d日")

/** 规划模式栏区总宽 = 视口 × 1.2（三栏等宽、每栏 0.4× 视口，默认第三栏半露） */
private const val LANE_SCROLL_FACTOR = 1.2f

@Composable
fun TimelineScreen(
    state: TimelineUiState,
    onSelectDate: (LocalDate) -> Unit,
    onOpenSettings: () -> Unit,
    onOpenPixieDust: () -> Unit,
    onOpenWishlist: () -> Unit = {},
    onOpenFacilities: () -> Unit = {},
    onOpenDeckMap: () -> Unit = {},
    onNavigateToDeckMap: (deck: Int, target: String) -> Unit = { _, _ -> },
    onAddEvent: (Int) -> Unit,
    onEditEvent: (CruiseEvent) -> Unit,
    onMoveToLane: (Long, Int) -> Unit,
    onToggleActive: (Long, Boolean) -> Unit,
    onKeepOnlyThisShow: (CruiseEvent) -> Unit,
    onDeleteEvent: (CruiseEvent, RecurrenceScope) -> Unit,
    onGeneratePlan: () -> Unit,
    onUnlockDay: () -> Unit,
    onJumpToNow: () -> Unit,
    onToggleFullDay: () -> Unit,
    conflictIds: Set<Long>,
) {
    val density = LocalDensity.current
    val scrollState = rememberScrollState()
    val coroutineScope = rememberCoroutineScope()
    var menuEvent by remember { mutableStateOf<CruiseEvent?>(null) }
    var detailEvent by remember { mutableStateOf<CruiseEvent?>(null) }

    fun scrollToNow() {
        if (!state.isToday) return
        val redLinePx = with(density) { state.layout.minuteToDp(state.nowMinute).dp.toPx() }
        val targetPx = (redLinePx - with(density) { 100.dp.toPx() }).coerceAtLeast(0f).toInt()
        coroutineScope.launch {
            scrollState.animateScrollTo(targetPx)
        }
    }

    // 打开当天或切到当天时自动滚到"现在"红线附近
    LaunchedEffect(state.selectedDate, state.isToday) {
        if (state.isToday) {
            val redLinePx = with(density) { state.layout.minuteToDp(state.nowMinute).dp.toPx() }
            val targetPx = (redLinePx - with(density) { 100.dp.toPx() }).coerceAtLeast(0f).toInt()
            scrollState.animateScrollTo(targetPx)
        }
    }

    Scaffold(
        floatingActionButton = {
            Column(horizontalAlignment = Alignment.End) {
                FloatingActionButton(
                    onClick = onOpenWishlist,
                    containerColor = CruiseAzureContainer,
                    contentColor = CruiseMarine,
                    shape = RoundedCornerShape(16.dp),
                ) {
                    Icon(
                        Icons.Outlined.Checklist,
                        contentDescription = "随心打卡清单",
                    )
                }
                Spacer(Modifier.height(12.dp))
                FloatingActionButton(
                    onClick = onOpenPixieDust,
                    containerColor = CruiseAzureContainer,
                    contentColor = CruiseMarine,
                    shape = RoundedCornerShape(16.dp),
                ) {
                    Icon(
                        Icons.Outlined.AutoAwesome,
                        contentDescription = stringResource(R.string.pixie_open),
                    )
                }
                if (!state.isFinalized) {
                    Spacer(Modifier.height(12.dp))
                    FloatingActionButton(
                        onClick = {
                            val startMin = defaultStartMinute(
                                layout = state.layout,
                                scrollPx = scrollState.value,
                                viewportHeightPx = scrollState.viewportSize,
                                density = density,
                            )
                            onAddEvent(startMin)
                        },
                        containerColor = CruiseAzure,
                        contentColor = Color.White,
                        shape = RoundedCornerShape(16.dp),
                    ) {
                        Icon(Icons.Filled.Add, contentDescription = stringResource(R.string.add_event))
                    }
                }
            }
        },
    ) { padding ->
        Box(
            Modifier
                .padding(padding)
                .fillMaxSize()
                .background(MaterialTheme.colorScheme.background)
        ) {
            // ---- 半透明底图背景层 ----
            if (state.settings.wallpaperEnabled) {
                val context = LocalContext.current
                val wallpaperBitmap = remember(state.settings.wallpaperKey) {
                    WallpaperManager.loadWallpaperBitmap(context, state.settings.wallpaperKey)
                }
                if (wallpaperBitmap != null) {
                    Image(
                        bitmap = wallpaperBitmap.asImageBitmap(),
                        contentDescription = null,
                        contentScale = ContentScale.Crop,
                        alpha = state.settings.wallpaperAlpha,
                        modifier = Modifier.fillMaxSize(),
                    )
                }
            }

            Column(Modifier.fillMaxSize()) {
                // ---- 顶栏 ----
            Row(
                Modifier
                    .fillMaxWidth()
                    .padding(horizontal = 16.dp, vertical = 8.dp),
                verticalAlignment = Alignment.CenterVertically,
            ) {
                Column(Modifier.weight(1f)) {
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        Text(
                            text = state.selectedDate.format(HEADER_FMT),
                            style = MaterialTheme.typography.titleLarge,
                            fontWeight = FontWeight.Bold,
                            color = Slate900,
                        )
                        Spacer(Modifier.width(8.dp))
                        Text(
                            text = state.selectedDate.dayOfWeek
                                .getDisplayName(TextStyle.FULL, Locale.CHINA),
                            style = MaterialTheme.typography.titleSmall,
                            color = Slate500,
                        )
                    }
                    state.voyage?.let { voyage ->
                        val dayIdx = voyage.dayIndexOf(state.selectedDate)
                        if (dayIdx != null) {
                            Spacer(Modifier.height(2.dp))
                            Row(
                                modifier = Modifier
                                    .clip(RoundedCornerShape(6.dp))
                                    .background(Slate100)
                                    .padding(horizontal = 6.dp, vertical = 1.dp),
                                verticalAlignment = Alignment.CenterVertically,
                            ) {
                                Text(
                                    text = "🚢 ${voyage.shipName?.ifEmpty { "游轮" } ?: "游轮"} · " +
                                        stringResource(R.string.day_index_format, dayIdx, voyage.dayCount),
                                    style = MaterialTheme.typography.labelSmall,
                                    fontSize = 11.sp,
                                    color = CruiseMarine,
                                    fontWeight = FontWeight.Medium,
                                )
                            }
                        }
                    }
                }

                // 快捷操作按钮组（设施指南、甲板地图与设置）
                Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                    Surface(
                        onClick = onOpenFacilities,
                        shape = RoundedCornerShape(10.dp),
                        color = Slate100,
                        border = BorderStroke(1.dp, Slate200),
                        modifier = Modifier.size(38.dp),
                    ) {
                        Box(contentAlignment = Alignment.Center, modifier = Modifier.fillMaxSize()) {
                            Icon(
                                Icons.Outlined.Explore,
                                contentDescription = stringResource(R.string.facility_guide_title),
                                tint = CruiseMarine,
                                modifier = Modifier.size(20.dp),
                            )
                        }
                    }
                    Surface(
                        onClick = onOpenDeckMap,
                        shape = RoundedCornerShape(10.dp),
                        color = Slate100,
                        border = BorderStroke(1.dp, Slate200),
                        modifier = Modifier.size(38.dp),
                    ) {
                        Box(contentAlignment = Alignment.Center, modifier = Modifier.fillMaxSize()) {
                            Icon(
                                imageVector = Icons.Outlined.Map,
                                contentDescription = stringResource(R.string.deck_map_title),
                                tint = CruiseMarine,
                                modifier = Modifier.size(20.dp),
                            )
                        }
                    }
                    Surface(
                        onClick = onOpenSettings,
                        shape = RoundedCornerShape(10.dp),
                        color = Slate100,
                        border = BorderStroke(1.dp, Slate200),
                        modifier = Modifier.size(38.dp),
                    ) {
                        Box(contentAlignment = Alignment.Center, modifier = Modifier.fillMaxSize()) {
                            Icon(
                                Icons.Outlined.Settings,
                                contentDescription = stringResource(R.string.settings),
                                tint = Slate700,
                                modifier = Modifier.size(20.dp),
                            )
                        }
                    }
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

            // ---- 定稿状态与操作条 ----
            Row(
                Modifier
                    .fillMaxWidth()
                    .padding(horizontal = 14.dp, vertical = 4.dp),
                verticalAlignment = Alignment.CenterVertically,
            ) {
                if (state.isFinalized) {
                    Row(
                        modifier = Modifier
                            .clip(RoundedCornerShape(8.dp))
                            .background(CruiseAzureContainer)
                            .padding(horizontal = 8.dp, vertical = 4.dp),
                        verticalAlignment = Alignment.CenterVertically,
                    ) {
                        Icon(
                            Icons.Outlined.CheckCircle,
                            contentDescription = null,
                            tint = CruiseMarine,
                            modifier = Modifier.size(14.dp),
                        )
                        Spacer(Modifier.width(4.dp))
                        Text(
                            stringResource(R.string.finalized_badge),
                            style = MaterialTheme.typography.labelSmall,
                            fontWeight = FontWeight.Bold,
                            color = CruiseMarine,
                        )
                    }
                } else {
                    Text(
                        text = "3 车道规划中",
                        style = MaterialTheme.typography.labelSmall,
                        color = Slate500,
                    )
                }

                Spacer(Modifier.weight(1f))

                if (state.todayInVoyage) {
                    // "转跳当前时间"圆角方框按钮
                    FilledTonalButton(
                        onClick = {
                            if (state.isToday) {
                                scrollToNow()
                            } else {
                                onJumpToNow()
                            }
                        },
                        contentPadding = PaddingValues(horizontal = 10.dp, vertical = 4.dp),
                        shape = RoundedCornerShape(10.dp),
                        colors = ButtonDefaults.filledTonalButtonColors(
                            containerColor = if (state.isToday) CruiseAzureContainer else Slate100,
                            contentColor = if (state.isToday) CruiseMarine else Slate700,
                        ),
                    ) {
                        Icon(
                            Icons.Outlined.Schedule,
                            contentDescription = null,
                            modifier = Modifier.size(14.dp),
                        )
                        Spacer(Modifier.width(4.dp))
                        Text(
                            stringResource(R.string.jump_to_now),
                            style = MaterialTheme.typography.labelMedium,
                            fontWeight = FontWeight.SemiBold,
                        )
                    }
                }

                Spacer(Modifier.weight(1f))

                if (state.isFinalized) {
                    TextButton(
                        onClick = onUnlockDay,
                        contentPadding = PaddingValues(horizontal = 10.dp, vertical = 2.dp),
                    ) {
                        Text(
                            stringResource(R.string.unlock),
                            style = MaterialTheme.typography.labelMedium,
                            color = CruiseAzure,
                            fontWeight = FontWeight.SemiBold,
                        )
                    }
                } else {
                    Button(
                        onClick = onGeneratePlan,
                        contentPadding = PaddingValues(horizontal = 12.dp, vertical = 4.dp),
                        shape = RoundedCornerShape(12.dp),
                        colors = ButtonDefaults.buttonColors(
                            containerColor = CruiseAzure,
                            contentColor = Color.White,
                        ),
                    ) {
                        Text(
                            stringResource(R.string.generate_plan),
                            style = MaterialTheme.typography.labelMedium,
                            fontWeight = FontWeight.SemiBold,
                        )
                    }
                }
            }

            // ---- 当天横幅 ----
            if (state.isToday) {
                NowBanner(state.ongoing, state.next, state.categories)
            }

            HorizontalDivider(color = Slate200)

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
                    Column(horizontalAlignment = Alignment.CenterHorizontally) {
                        Text(
                            "🗓️",
                            fontSize = 32.sp,
                        )
                        Spacer(Modifier.height(8.dp))
                        Text(
                            stringResource(R.string.empty_day),
                            style = MaterialTheme.typography.bodyMedium,
                            color = Slate500,
                        )
                    }
                }
            } else {
                Box(Modifier.weight(1f).verticalScroll(scrollState)) {
                    Row(Modifier.fillMaxWidth().height(state.layout.totalHeightDp.dp)) {
                        TimeRuler(state.layout)
                        BoxWithConstraints(Modifier.weight(1f)) {
                            val laneAreaWidth = if (state.isFinalized) {
                                maxWidth
                            } else {
                                maxWidth * LANE_SCROLL_FACTOR
                            }
                            val laneScrollState = rememberScrollState()
                            Box(
                                Modifier
                                    .horizontalScroll(laneScrollState)
                                    .width(laneAreaWidth),
                            ) {
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
                                    onEventClick = { event ->
                                        if (state.isFinalized) {
                                            detailEvent = event
                                        } else {
                                            onEditEvent(event)
                                        }
                                    },
                                    onEventLongClick = { event -> if (!state.isFinalized) menuEvent = event },
                                    menuFor = menuEvent,
                                    menuContent = { event ->
                                        DropdownMenu(expanded = true, onDismissRequest = { menuEvent = null }) {
                                            val isShow = state.categories[event.categoryId]?.iconKey == "show"
                                            if (!state.isFinalized && isShow) {
                                                DropdownMenuItem(
                                                    text = { Text(stringResource(R.string.keep_only_this_show)) },
                                                    onClick = {
                                                        onKeepOnlyThisShow(event)
                                                        menuEvent = null
                                                    },
                                                )
                                            }
                                            if (!state.isFinalized && !event.isSystemGenerated) {
                                                DropdownMenuItem(
                                                    text = {
                                                        Text(
                                                            stringResource(
                                                                if (event.isActive) R.string.set_inactive else R.string.set_active,
                                                            ),
                                                        )
                                                    },
                                                    onClick = {
                                                        onToggleActive(event.id, !event.isActive)
                                                        menuEvent = null
                                                    },
                                                )
                                            }
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
                                                        onDeleteEvent(event, RecurrenceScope.THIS_DAY_ONLY)
                                                        menuEvent = null
                                                    },
                                                )
                                            }
                                        }
                                    },
                                    dollsEnabled = state.settings.cardDollsEnabled,
                                    dollDistributionMode = state.settings.cardDollDistribution,
                                    modifier = Modifier.fillMaxSize(),
                                )
                                if (state.isToday) {
                                    NowIndicator(state.layout, state.nowMinute)
                                }
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
}

    // 定稿状态点击任务卡片弹出详情展示框
    detailEvent?.let { event ->
        EventDetailDialog(
            event = event,
            category = state.categories[event.categoryId],
            onNavigateToMap = { deck, target ->
                detailEvent = null
                onNavigateToDeckMap(deck, target)
            },
            onDismiss = { detailEvent = null },
        )
    }
}

@Composable
private fun HiddenHint(text: String, onClick: () -> Unit) {
    Row(
        Modifier
            .fillMaxWidth()
            .background(CruiseAzureContainer.copy(alpha = 0.5f))
            .padding(horizontal = 14.dp, vertical = 4.dp),
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.SpaceBetween,
    ) {
        Text(text, style = MaterialTheme.typography.labelSmall, color = CruiseMarine)
        TextButton(onClick = onClick) {
            Text(stringResource(R.string.show_full_day), style = MaterialTheme.typography.labelSmall, color = CruiseAzure)
        }
    }
}

/** FAB 默认开始时间：就近屏幕可视区域中心点的整点时间 */
fun defaultStartMinute(
    layout: TimelineLayout,
    scrollPx: Int,
    viewportHeightPx: Int,
    density: Density,
): Int {
    if (viewportHeightPx <= 0) {
        return (((layout.rangeStartMinute + 30) / 60) * 60).coerceIn(0, 23 * 60)
    }
    val centerDp = with(density) { (scrollPx + viewportHeightPx / 2f).toDp().value }
    return layout.nearestHourAtDp(centerDp)
}

@Composable
fun TimelineLoading() {
    Box(Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
        CircularProgressIndicator(color = CruiseAzure)
    }
}


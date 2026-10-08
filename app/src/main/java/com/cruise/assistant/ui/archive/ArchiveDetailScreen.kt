package com.cruise.assistant.ui.archive

import androidx.compose.foundation.background
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
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.outlined.CheckCircle
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.FilterChip
import androidx.compose.material3.FilterChipDefaults
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Surface
import androidx.compose.material3.Tab
import androidx.compose.material3.TabRow
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.material3.TopAppBar
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextDecoration
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.cruise.assistant.R
import com.cruise.assistant.domain.CruiseEvent
import com.cruise.assistant.domain.WishlistItem
import com.cruise.assistant.domain.formatLocation
import com.cruise.assistant.ui.pixiedust.DeckSectionCard
import com.cruise.assistant.ui.pixiedust.ExpandedDecksSaver
import com.cruise.assistant.ui.pixiedust.toggleDeckExpanded
import com.cruise.assistant.ui.theme.CruiseAzureContainer
import com.cruise.assistant.ui.theme.CruiseMarine
import com.cruise.assistant.ui.theme.Slate100
import com.cruise.assistant.ui.theme.Slate200
import com.cruise.assistant.ui.theme.Slate400
import com.cruise.assistant.ui.theme.Slate500
import com.cruise.assistant.ui.theme.Slate700
import com.cruise.assistant.ui.theme.Slate800
import com.cruise.assistant.ui.theme.Slate900
import com.cruise.assistant.ui.timeline.DateStrip
import com.cruise.assistant.ui.timeline.EventDetailDialog
import com.cruise.assistant.ui.timeline.LaneCanvas
import com.cruise.assistant.ui.timeline.TimeRuler
import com.cruise.assistant.ui.timeline.TimelineGrid
import com.cruise.assistant.ui.timeline.laneBoundaryFractions
import java.time.Instant
import java.time.LocalDate
import java.time.ZoneId
import java.time.format.DateTimeFormatter
import java.time.format.TextStyle
import java.util.Locale

/** 回放头部日期大字格式（与 TimelineScreen 的 HEADER_FMT 一致） */
private val REPLAY_HEADER_FMT = DateTimeFormatter.ofPattern("M月d日")

/** 列表/概要的日期时间格式 */
private val ARCHIVE_DATE_FMT = DateTimeFormatter.ofPattern("yyyy-MM-dd")
private val ARCHIVE_DATETIME_FMT = DateTimeFormatter.ofPattern("yyyy-MM-dd HH:mm")

/** 规划模式栏区总宽 = 视口 × 1.2（与 TimelineScreen 的 LANE_SCROLL_FACTOR 保持一致） */
private const val LANE_SCROLL_FACTOR = 1.2f

/**
 * 航次存档详情：只读回放。四个 Tab——概要（航次信息）、时间轴（与实时渲染同款卡片的回放）、
 * 仙尘（收发明细按层折叠）、心愿（只读清单）。
 */
@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun ArchiveDetailScreen(
    state: ArchiveDetailUiState,
    onBack: () -> Unit,
    onSelectDate: (LocalDate) -> Unit,
    onToggleFullDay: () -> Unit,
    onSetTimelineMode: (ArchiveTimelineMode) -> Unit,
) {
    Scaffold(
        topBar = {
            TopAppBar(
                title = {
                    Text(
                        // 标题用船名（空显示"自定义航次"）
                        text = state.voyage?.shipName?.ifEmpty { null }
                            ?: stringResource(R.string.archive_custom_voyage),
                        fontWeight = FontWeight.Bold,
                        color = Slate900,
                        maxLines = 1,
                    )
                },
                navigationIcon = {
                    IconButton(onClick = onBack) {
                        Icon(
                            Icons.AutoMirrored.Filled.ArrowBack,
                            contentDescription = stringResource(R.string.pixie_back),
                        )
                    }
                },
            )
        },
    ) { padding ->
        when {
            // 存档缺失/损坏
            state.loadFailed -> {
                Box(
                    Modifier
                        .padding(padding)
                        .fillMaxSize(),
                    contentAlignment = Alignment.Center,
                ) {
                    Text(
                        stringResource(R.string.archive_load_failed),
                        style = MaterialTheme.typography.bodyMedium,
                        color = Slate500,
                    )
                }
            }
            // 快照还在解析
            state.snapshot == null -> {
                Box(
                    Modifier
                        .padding(padding)
                        .fillMaxSize(),
                    contentAlignment = Alignment.Center,
                ) {
                    CircularProgressIndicator(color = MaterialTheme.colorScheme.primary)
                }
            }
            else -> {
                ArchiveDetailContent(
                    state = state,
                    onSelectDate = onSelectDate,
                    onToggleFullDay = onToggleFullDay,
                    onSetTimelineMode = onSetTimelineMode,
                    modifier = Modifier
                        .padding(padding)
                        .fillMaxSize(),
                )
            }
        }
    }
}

@Composable
private fun ArchiveDetailContent(
    state: ArchiveDetailUiState,
    onSelectDate: (LocalDate) -> Unit,
    onToggleFullDay: () -> Unit,
    onSetTimelineMode: (ArchiveTimelineMode) -> Unit,
    modifier: Modifier = Modifier,
) {
    var selectedTab by rememberSaveable { mutableStateOf(0) }
    // 只读事件详情弹窗（readOnly=true：无任何操作按钮）
    var detailEvent by remember { mutableStateOf<CruiseEvent?>(null) }

    val tabLabels = listOf(
        stringResource(R.string.archive_tab_summary),
        stringResource(R.string.archive_tab_timeline),
        stringResource(R.string.archive_tab_pixie),
        stringResource(R.string.archive_tab_wishlist),
    )

    Column(modifier) {
        TabRow(selectedTabIndex = selectedTab) {
            tabLabels.forEachIndexed { index, label ->
                Tab(
                    selected = selectedTab == index,
                    onClick = { selectedTab = index },
                    text = { Text(label, fontWeight = FontWeight.SemiBold) },
                )
            }
        }
        when (selectedTab) {
            0 -> SummaryTab(state)
            1 -> TimelineTab(
                state = state,
                onSelectDate = onSelectDate,
                onToggleFullDay = onToggleFullDay,
                onSetTimelineMode = onSetTimelineMode,
                onEventClick = { detailEvent = it },
            )
            2 -> PixieTab(state)
            else -> WishlistTab(state)
        }
    }

    // 事件详情：存档回放一律 readOnly，隐藏"定位"等操作入口
    detailEvent?.let { event ->
        EventDetailDialog(
            event = event,
            category = state.categories[event.categoryId],
            onNavigateToMap = { _, _ -> },
            onDismiss = { detailEvent = null },
            readOnly = true,
        )
    }
}

// ---- Tab 1：概要 ----

@Composable
private fun SummaryTab(state: ArchiveDetailUiState) {
    val snapshot = state.snapshot ?: return
    val voyage = snapshot.voyage
    Column(
        Modifier
            .fillMaxSize()
            .verticalScroll(rememberScrollState())
            .padding(16.dp),
    ) {
        Surface(
            shape = RoundedCornerShape(16.dp),
            color = MaterialTheme.colorScheme.surface,
            border = androidx.compose.foundation.BorderStroke(1.dp, Slate200),
            modifier = Modifier.fillMaxWidth(),
        ) {
            Column(Modifier.padding(16.dp)) {
                // 船名 + 礼宾徽标
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Text(
                        text = voyage.shipName?.ifEmpty { null }
                            ?: stringResource(R.string.archive_custom_voyage),
                        style = MaterialTheme.typography.titleLarge,
                        fontWeight = FontWeight.Bold,
                        color = Slate900,
                        modifier = Modifier.weight(1f),
                    )
                    if (voyage.isConcierge) {
                        Spacer(Modifier.width(8.dp))
                        Surface(
                            shape = RoundedCornerShape(6.dp),
                            color = CruiseAzureContainer,
                        ) {
                            Text(
                                stringResource(R.string.concierge_badge),
                                style = MaterialTheme.typography.labelSmall,
                                fontWeight = FontWeight.SemiBold,
                                color = CruiseMarine,
                                modifier = Modifier.padding(horizontal = 6.dp, vertical = 2.dp),
                            )
                        }
                    }
                }
                Spacer(Modifier.height(12.dp))
                SummaryRow(stringResource(R.string.archive_board_label), boardLabel(voyage))
                Spacer(Modifier.height(6.dp))
                SummaryRow(stringResource(R.string.archive_disembark_label), disembarkLabel(voyage))
                Spacer(Modifier.height(6.dp))
                SummaryRow(stringResource(R.string.archive_days_label), "${voyage.dayCount}")
                Spacer(Modifier.height(6.dp))
                // 晚数摘要（联程 "3+4夜 · 共7晚"，单程 "4夜"）
                SummaryRow(
                    stringResource(R.string.archive_nights_label),
                    state.nightsSummary ?: "",
                )
                if (voyage.legs.isNotEmpty()) {
                    Spacer(Modifier.height(6.dp))
                    // 联程段明细："3+4夜"
                    SummaryRow(
                        stringResource(R.string.archive_legs_label),
                        voyage.legs.joinToString("+") + "夜",
                    )
                }
                Spacer(Modifier.height(6.dp))
                state.createdAt?.let { createdAt ->
                    SummaryRow(
                        stringResource(R.string.archive_saved_at_label),
                        Instant.ofEpochMilli(createdAt)
                            .atZone(ZoneId.systemDefault())
                            .format(ARCHIVE_DATETIME_FMT),
                    )
                }
            }
        }
    }
}

@Composable
private fun SummaryRow(label: String, value: String) {
    Row(verticalAlignment = Alignment.CenterVertically) {
        Text(
            text = label,
            style = MaterialTheme.typography.bodyMedium,
            color = Slate500,
            modifier = Modifier.width(72.dp),
        )
        Text(
            text = value,
            style = MaterialTheme.typography.bodyMedium,
            fontWeight = FontWeight.Medium,
            color = Slate800,
        )
    }
}

private fun boardLabel(voyage: com.cruise.assistant.domain.Voyage): String =
    "${voyage.boardDate.format(ARCHIVE_DATE_FMT)} ${voyage.boardTime}"

private fun disembarkLabel(voyage: com.cruise.assistant.domain.Voyage): String =
    "${voyage.disembarkDate.format(ARCHIVE_DATE_FMT)} ${voyage.disembarkTime}"

// ---- Tab 2：时间轴只读回放 ----

@Composable
private fun TimelineTab(
    state: ArchiveDetailUiState,
    onSelectDate: (LocalDate) -> Unit,
    onToggleFullDay: () -> Unit,
    onSetTimelineMode: (ArchiveTimelineMode) -> Unit,
    onEventClick: (CruiseEvent) -> Unit,
) {
    val voyage = state.voyage ?: return
    val layout = state.layout ?: return
    val selectedDate = state.selectedDate ?: return
    val scrollState = rememberScrollState()

    Column(Modifier.fillMaxSize()) {
        // ---- 当前选中日期 + 定稿徽标 + 回放模式切换（日程表/草稿） ----
        Row(
            Modifier
                .fillMaxWidth()
                .padding(horizontal = 14.dp, vertical = 6.dp),
            verticalAlignment = Alignment.CenterVertically,
        ) {
            Text(
                text = selectedDate.format(REPLAY_HEADER_FMT) + " " +
                    selectedDate.dayOfWeek.getDisplayName(TextStyle.FULL, Locale.CHINA),
                style = MaterialTheme.typography.titleMedium,
                fontWeight = FontWeight.Bold,
                color = Slate900,
                modifier = Modifier.weight(1f),
            )
            // 徽标=存档真相（当日是否定稿），不随回放模式变化；
            // 草稿模式下当日已定稿时仍出现，属预期（草稿=回看当时怎么排的）
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
            }
            // 回放模式两枚 chips（风格对齐引导页）：
            // 日程表=定稿样式（当天实际日程，仅生效项）；草稿=三栏卡片（含灰色未启用备选）
            Row(
                horizontalArrangement = Arrangement.spacedBy(6.dp),
                verticalAlignment = Alignment.CenterVertically,
            ) {
                FilterChip(
                    selected = state.timelineMode == ArchiveTimelineMode.SCHEDULE,
                    onClick = { onSetTimelineMode(ArchiveTimelineMode.SCHEDULE) },
                    label = {
                        Text(
                            stringResource(R.string.archive_mode_schedule),
                            style = MaterialTheme.typography.labelMedium,
                            maxLines = 1,
                        )
                    },
                    colors = FilterChipDefaults.filterChipColors(
                        selectedContainerColor = CruiseAzureContainer,
                        selectedLabelColor = CruiseMarine,
                    ),
                )
                FilterChip(
                    selected = state.timelineMode == ArchiveTimelineMode.DRAFT,
                    onClick = { onSetTimelineMode(ArchiveTimelineMode.DRAFT) },
                    label = {
                        Text(
                            stringResource(R.string.archive_mode_draft),
                            style = MaterialTheme.typography.labelMedium,
                            maxLines = 1,
                        )
                    },
                    colors = FilterChipDefaults.filterChipColors(
                        selectedContainerColor = CruiseAzureContainer,
                        selectedLabelColor = CruiseMarine,
                    ),
                )
            }
        }

        // ---- 语境化提示：仅当回放模式与存档定稿事实"错位"时出现，解释为何视角与徽标不一致 ----
        val modeHintRes = when {
            state.renderAsSchedule && !state.isFinalized ->
                R.string.archive_mode_schedule_unfinalized_hint
            !state.renderAsSchedule && state.isFinalized ->
                R.string.archive_mode_draft_finalized_hint
            else -> null
        }
        modeHintRes?.let { res ->
            Text(
                text = stringResource(res),
                style = MaterialTheme.typography.labelSmall,
                color = Slate500,
                modifier = Modifier.padding(start = 14.dp, end = 14.dp),
            )
        }

        // ---- 日期条（快照航次日期；定稿点与实时时间轴一致） ----
        DateStrip(
            dates = voyage.datesInVoyage(),
            selectedDate = selectedDate,
            finalizedDates = state.dayPlans.filterValues { it.finalized }.keys,
            onSelect = onSelectDate,
        )

        HorizontalDivider(color = Slate200)

        // ---- 范围外提示（"看全天"切换：查看偏好跟随当前设置，不属快照） ----
        if (layout.hiddenBefore > 0 || layout.hiddenAfter > 0) {
            Row(
                Modifier
                    .fillMaxWidth()
                    .background(CruiseAzureContainer.copy(alpha = 0.5f))
                    .padding(horizontal = 14.dp, vertical = 4.dp),
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.SpaceBetween,
            ) {
                Text(
                    text = listOf(
                        if (layout.hiddenBefore > 0) {
                            stringResource(R.string.hidden_before_format, layout.hiddenBefore)
                        } else null,
                        if (layout.hiddenAfter > 0) {
                            stringResource(R.string.hidden_after_format, layout.hiddenAfter)
                        } else null,
                    ).filterNotNull().joinToString("  "),
                    style = MaterialTheme.typography.labelSmall,
                    color = CruiseMarine,
                )
                TextButton(onClick = onToggleFullDay, contentPadding = PaddingValues(horizontal = 8.dp)) {
                    Text(
                        stringResource(R.string.show_full_day),
                        style = MaterialTheme.typography.labelSmall,
                        color = MaterialTheme.colorScheme.primary,
                    )
                }
            }
        }

        // ---- 时间轴主体（与 TimelineScreen 同构：TimeRuler + TimelineGrid + LaneCanvas） ----
        if (state.events.isEmpty()) {
            Box(Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
                Column(horizontalAlignment = Alignment.CenterHorizontally) {
                    Text("🗓️", fontSize = 32.sp)
                    Spacer(Modifier.height(8.dp))
                    Text(
                        stringResource(R.string.archive_empty_day),
                        style = MaterialTheme.typography.bodyMedium,
                        color = Slate500,
                    )
                }
            }
        } else {
            Box(Modifier.weight(1f).verticalScroll(scrollState)) {
                Row(Modifier.fillMaxWidth().height(layout.totalHeightDp.dp)) {
                    TimeRuler(layout)
                    BoxWithConstraints(Modifier.weight(1f)) {
                        // 日程表模式 → 单栏满宽；草稿模式 → 三栏泳道（可横滑 1.2 倍）
                        val laneAreaWidth = if (state.renderAsSchedule) {
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
                                layout = layout,
                                laneBoundaries = if (state.renderAsSchedule) {
                                    emptyList()
                                } else {
                                    laneBoundaryFractions()
                                },
                                modifier = Modifier.fillMaxSize(),
                            )
                            LaneCanvas(
                                layout = layout,
                                events = state.events,
                                categories = state.categories,
                                // 存档回放无冲突高亮
                                conflictIds = emptySet(),
                                singleLane = state.renderAsSchedule,
                                // 点击 → 只读事件详情弹窗
                                onEventClick = onEventClick,
                                // 长按菜单是编辑入口，回放禁用（no-op）
                                onEventLongClick = {},
                                // dollsEnabled 跟随当前查看偏好
                                dollsEnabled = state.settings.cardDollsEnabled,
                                modifier = Modifier.fillMaxSize(),
                            )
                            // 无 NowIndicator：存档没有"现在"
                        }
                    }
                }
            }
        }
    }
}

// ---- Tab 3：仙尘收发明细（按层折叠） ----

@Composable
private fun PixieTab(state: ArchiveDetailUiState) {
    val snapshot = state.snapshot ?: return
    val roomPairs = state.roomPairs
    // 默认全部收起（同联动检视）；ExpandedDecksSaver 保证配置重建后恢复
    var expandedDecks by rememberSaveable(stateSaver = ExpandedDecksSaver) {
        mutableStateOf(emptySet<Int>())
    }

    val totalRooms = roomPairs.size
    val unrepliedRooms = remember(roomPairs) { roomPairs.count { it.unrepliedReceived.isNotEmpty() } }
    val supplementRooms = remember(roomPairs) {
        roomPairs.count { it.unrepliedReceived.isEmpty() && it.received.size > it.given.size }
    }
    val completedRooms = remember(roomPairs) {
        roomPairs.count { it.unrepliedReceived.isEmpty() && it.given.size == it.received.size && it.given.isNotEmpty() }
    }
    val givenPendingRooms = remember(roomPairs) {
        roomPairs.count { it.unrepliedReceived.isEmpty() && it.given.size > it.received.size }
    }
    val groupedByDeck = remember(roomPairs) { roomPairs.groupBy { it.deck } }

    LazyColumn(
        Modifier.fillMaxSize(),
        contentPadding = PaddingValues(16.dp),
        verticalArrangement = Arrangement.spacedBy(14.dp),
    ) {
        // 全船四状态统计卡（文案与计算方式同 LinkageViewSheet）
        item {
            Surface(
                shape = RoundedCornerShape(12.dp),
                color = Slate100,
                border = androidx.compose.foundation.BorderStroke(1.dp, Slate200),
                modifier = Modifier.fillMaxWidth(),
            ) {
                Text(
                    text = stringResource(
                        R.string.pixie_linkage_summary_format,
                        totalRooms,
                        unrepliedRooms,
                        supplementRooms,
                        completedRooms,
                        givenPendingRooms,
                    ),
                    style = MaterialTheme.typography.bodyMedium,
                    fontWeight = FontWeight.Medium,
                    color = Slate700,
                    modifier = Modifier.padding(horizontal = 14.dp, vertical = 10.dp),
                )
            }
        }
        if (roomPairs.isEmpty()) {
            item {
                Box(
                    Modifier
                        .fillMaxWidth()
                        .padding(vertical = 48.dp),
                    contentAlignment = Alignment.Center,
                ) {
                    Text(
                        text = stringResource(R.string.pixie_linkage_empty),
                        style = MaterialTheme.typography.bodyMedium,
                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                    )
                }
            }
        } else {
            groupedByDeck.forEach { (deck, pairs) ->
                item(key = deck) {
                    // showMapButton = false：回放不给地图跳转；等级名取快照 pixieLevels
                    DeckSectionCard(
                        deck = deck,
                        pairs = pairs,
                        levels = snapshot.pixieLevels,
                        onNavigateToDeckMap = {},
                        expanded = deck in expandedDecks,
                        onToggleExpanded = {
                            expandedDecks = toggleDeckExpanded(expandedDecks, deck)
                        },
                        showMapButton = false,
                    )
                }
            }
        }
    }
}

// ---- Tab 4：心愿（只读清单） ----

@Composable
private fun WishlistTab(state: ArchiveDetailUiState) {
    val snapshot = state.snapshot ?: return
    val items = snapshot.wishlist

    if (items.isEmpty()) {
        Box(Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
            Text(
                stringResource(R.string.archive_wishlist_empty),
                style = MaterialTheme.typography.bodyMedium,
                color = Slate500,
            )
        }
        return
    }

    LazyColumn(
        Modifier.fillMaxSize(),
        contentPadding = PaddingValues(16.dp),
        verticalArrangement = Arrangement.spacedBy(10.dp),
    ) {
        items(items, key = { it.id }) { item ->
            // 类别映射用已建好的 Map<Long, Category>（来自快照 categories）
            WishlistArchiveRow(item, state.categories)
        }
    }
}

@Composable
private fun WishlistArchiveRow(
    item: WishlistItem,
    categories: Map<Long, com.cruise.assistant.domain.Category>,
) {
    val categoryColor = categories[item.categoryId]?.colorArgb ?: 0xFF9E9E9E.toInt() // 找不到类别用灰色
    Surface(
        shape = RoundedCornerShape(12.dp),
        color = MaterialTheme.colorScheme.surface,
        border = androidx.compose.foundation.BorderStroke(1.dp, Slate200),
        modifier = Modifier.fillMaxWidth(),
    ) {
        Column(Modifier.padding(horizontal = 14.dp, vertical = 10.dp)) {
            Row(verticalAlignment = Alignment.CenterVertically) {
                // 完成状态：✓ / ○
                Text(
                    text = if (item.isCompleted) "✓" else "○",
                    style = MaterialTheme.typography.titleMedium,
                    fontWeight = FontWeight.Bold,
                    color = if (item.isCompleted) CruiseMarine else Slate400,
                )
                Spacer(Modifier.width(8.dp))
                // 类别色点
                Box(
                    Modifier
                        .size(10.dp)
                        .clip(CircleShape)
                        .background(Color(categoryColor)),
                )
                Spacer(Modifier.width(8.dp))
                Text(
                    text = item.title,
                    style = MaterialTheme.typography.bodyMedium,
                    fontWeight = FontWeight.SemiBold,
                    color = if (item.isCompleted) Slate500 else Slate900,
                    textDecoration = if (item.isCompleted) TextDecoration.LineThrough else null,
                    modifier = Modifier.weight(1f),
                )
                Spacer(Modifier.width(8.dp))
                Text(
                    text = stringResource(
                        if (item.isCompleted) R.string.archive_wishlist_completed
                        else R.string.archive_wishlist_not_completed,
                    ),
                    style = MaterialTheme.typography.labelSmall,
                    color = Slate500,
                )
            }
            // 地点文本
            formatLocation(item.deck, item.zone, item.placeName, compact = false)?.let { location ->
                Spacer(Modifier.height(2.dp))
                Text(
                    text = location,
                    style = MaterialTheme.typography.bodySmall,
                    color = Slate500,
                )
            }
            // 备注
            if (!item.note.isNullOrBlank()) {
                Spacer(Modifier.height(2.dp))
                Text(
                    text = item.note,
                    style = MaterialTheme.typography.bodySmall,
                    color = Slate700,
                )
            }
            Spacer(Modifier.height(2.dp))
            Text(
                text = stringResource(
                    R.string.archive_wishlist_created_at_format,
                    Instant.ofEpochMilli(item.createdAt)
                        .atZone(ZoneId.systemDefault())
                        .format(ARCHIVE_DATETIME_FMT),
                ),
                style = MaterialTheme.typography.labelSmall,
                fontSize = 10.sp,
                color = Slate400,
            )
        }
    }
}

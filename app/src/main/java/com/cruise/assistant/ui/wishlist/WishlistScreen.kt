package com.cruise.assistant.ui.wishlist

import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.horizontalScroll
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
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
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.Check
import androidx.compose.material.icons.outlined.CalendarMonth
import androidx.compose.material.icons.outlined.LocationOn
import androidx.compose.material.icons.outlined.Map
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.CenterAlignedTopAppBar
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.FilterChip
import androidx.compose.material3.FilterChipDefaults
import androidx.compose.material3.FloatingActionButton
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.material3.TopAppBarDefaults
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextDecoration
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import com.cruise.assistant.R
import com.cruise.assistant.domain.WishlistItem
import com.cruise.assistant.domain.resolveDefaultWaitMinutes
import com.cruise.assistant.ui.common.TimeWheel
import com.cruise.assistant.ui.common.iconFor
import com.cruise.assistant.ui.theme.CruiseAzure
import com.cruise.assistant.ui.theme.CruiseAzureContainer
import com.cruise.assistant.ui.theme.CruiseMarine
import com.cruise.assistant.ui.theme.Slate100
import com.cruise.assistant.ui.theme.Slate200
import com.cruise.assistant.ui.theme.Slate500
import com.cruise.assistant.ui.theme.Slate700
import com.cruise.assistant.ui.theme.Slate900
import java.time.LocalDate
import java.time.LocalTime

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun WishlistScreen(
    state: WishlistUiState,
    onBack: () -> Unit,
    onNavigateToDeckMap: (deck: Int, target: String) -> Unit,
    onToggleCompleted: (WishlistItem) -> Unit,
    onSaveItem: (id: Long, title: String, categoryId: Long, deck: Int?, zone: com.cruise.assistant.domain.ShipZone?, placeName: String?, note: String?) -> Unit,
    onDeleteItem: (WishlistItem) -> Unit,
    onFilterCategory: (Long?) -> Unit,
    activeVoyageDates: List<LocalDate> = emptyList(),
    onConvertToSchedule: (item: WishlistItem, date: LocalDate, startTime: LocalTime, durationMinutes: Int) -> Unit = { _, _, _, _ -> },
) {
    var editingItem by remember { mutableStateOf<WishlistItem?>(null) }
    var convertingItem by remember { mutableStateOf<WishlistItem?>(null) }
    var showAddSheet by remember { mutableStateOf(false) }

    if (showAddSheet || editingItem != null) {
        WishlistEditSheet(
            initialItem = editingItem,
            categories = state.categories,
            onSave = { id, title, categoryId, deck, zone, placeName, note ->
                onSaveItem(id, title, categoryId, deck, zone, placeName, note)
                editingItem = null
                showAddSheet = false
            },
            onDelete = if (editingItem != null) { item -> onDeleteItem(item) } else null,
            onDismiss = {
                editingItem = null
                showAddSheet = false
            },
        )
    }

    convertingItem?.let { item ->
        WishlistConvertDialog(
            item = item,
            availableDates = activeVoyageDates,
            onDismiss = { convertingItem = null },
            onConfirm = { date, startTime, durationMinutes ->
                onConvertToSchedule(item, date, startTime, durationMinutes)
                convertingItem = null
            },
        )
    }

    Scaffold(
        topBar = {
            CenterAlignedTopAppBar(
                title = {
                    Column(horizontalAlignment = Alignment.CenterHorizontally) {
                        Text(
                            text = "随心打卡清单",
                            style = MaterialTheme.typography.titleMedium,
                            fontWeight = FontWeight.Bold,
                            color = Slate900,
                        )
                        Text(
                            text = "已完成 ${state.completedCount} / 共 ${state.totalCount} 项",
                            style = MaterialTheme.typography.labelSmall,
                            color = Slate500,
                        )
                    }
                },
                navigationIcon = {
                    IconButton(onClick = onBack) {
                        Icon(
                            imageVector = Icons.AutoMirrored.Filled.ArrowBack,
                            contentDescription = stringResource(R.string.cancel),
                            tint = Slate700,
                        )
                    }
                },
                colors = TopAppBarDefaults.topAppBarColors(
                    containerColor = MaterialTheme.colorScheme.surface,
                ),
            )
        },
        floatingActionButton = {
            FloatingActionButton(
                onClick = { showAddSheet = true },
                containerColor = CruiseAzure,
                contentColor = Color.White,
                shape = RoundedCornerShape(16.dp),
            ) {
                Icon(Icons.Default.Add, contentDescription = "添加随心项目")
            }
        },
        containerColor = MaterialTheme.colorScheme.background,
    ) { innerPadding ->
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(innerPadding),
        ) {
            // 类别筛选栏
            if (state.categories.isNotEmpty()) {
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .horizontalScroll(rememberScrollState())
                        .padding(horizontal = 16.dp, vertical = 6.dp),
                    horizontalArrangement = Arrangement.spacedBy(8.dp),
                ) {
                    FilterChip(
                        selected = state.selectedCategoryFilter == null,
                        onClick = { onFilterCategory(null) },
                        label = { Text("全部 (${state.totalCount})") },
                        colors = FilterChipDefaults.filterChipColors(
                            selectedContainerColor = CruiseAzure,
                            selectedLabelColor = Color.White,
                            containerColor = Slate100,
                            labelColor = Slate700,
                        ),
                        border = if (state.selectedCategoryFilter == null) null else BorderStroke(1.dp, Slate200),
                        shape = RoundedCornerShape(10.dp),
                    )

                    state.categories.forEach { cat ->
                        val isSelected = state.selectedCategoryFilter == cat.id
                        val color = Color(cat.colorArgb)
                        FilterChip(
                            selected = isSelected,
                            onClick = { onFilterCategory(if (isSelected) null else cat.id) },
                            label = { Text(cat.name) },
                            leadingIcon = {
                                Icon(
                                    imageVector = iconFor(cat.iconKey),
                                    contentDescription = null,
                                    tint = if (isSelected) Color.White else color,
                                    modifier = Modifier.size(16.dp),
                                )
                            },
                            colors = FilterChipDefaults.filterChipColors(
                                selectedContainerColor = color,
                                selectedLabelColor = Color.White,
                                containerColor = Slate100,
                                labelColor = Slate700,
                            ),
                            border = if (isSelected) null else BorderStroke(1.dp, Slate200),
                            shape = RoundedCornerShape(10.dp),
                        )
                    }
                }
            }

            // 清单列表
            if (state.items.isEmpty()) {
                Box(
                    modifier = Modifier
                        .fillMaxSize()
                        .padding(32.dp),
                    contentAlignment = Alignment.Center,
                ) {
                    Column(horizontalAlignment = Alignment.CenterHorizontally) {
                        Text(
                            text = "暂无随心项目",
                            style = MaterialTheme.typography.titleMedium,
                            fontWeight = FontWeight.Bold,
                            color = Slate700,
                        )
                        Spacer(Modifier.height(6.dp))
                        Text(
                            text = "点击右下角 \"+\" 添加不限时间的娱乐、购物或打卡愿望",
                            style = MaterialTheme.typography.bodyMedium,
                            color = Slate500,
                        )
                    }
                }
            } else {
                LazyColumn(
                    modifier = Modifier.fillMaxSize(),
                    contentPadding = PaddingValues(horizontal = 16.dp, vertical = 8.dp),
                    verticalArrangement = Arrangement.spacedBy(10.dp),
                ) {
                    items(state.items, key = { it.id }) { item ->
                        WishlistCard(
                            item = item,
                            onToggle = { onToggleCompleted(item) },
                            onClick = { editingItem = item },
                            onNavigateToMap = onNavigateToDeckMap,
                            onConvertToSchedule = { convertingItem = item },
                        )
                    }
                }
            }
        }
    }
}

@Composable
private fun WishlistCard(
    item: WishlistItem,
    onToggle: () -> Unit,
    onClick: () -> Unit,
    onNavigateToMap: (deck: Int, target: String) -> Unit,
    onConvertToSchedule: () -> Unit,
) {
    val categoryColor = item.category?.colorArgb?.let { Color(it) } ?: CruiseAzure

    Surface(
        shape = RoundedCornerShape(14.dp),
        color = if (item.isCompleted) Slate100.copy(alpha = 0.6f) else MaterialTheme.colorScheme.surface,
        border = BorderStroke(1.dp, if (item.isCompleted) Slate200.copy(alpha = 0.6f) else Slate200),
        modifier = Modifier
            .fillMaxWidth()
            .clickable { onClick() },
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(14.dp),
            verticalAlignment = Alignment.Top,
        ) {
            // 打卡勾选圈
            Surface(
                shape = CircleShape,
                color = if (item.isCompleted) categoryColor else Slate100,
                border = BorderStroke(1.5.dp, if (item.isCompleted) categoryColor else Slate200),
                modifier = Modifier
                    .size(24.dp)
                    .clip(CircleShape)
                    .clickable { onToggle() },
            ) {
                if (item.isCompleted) {
                    Icon(
                        imageVector = Icons.Default.Check,
                        contentDescription = "已打卡",
                        tint = Color.White,
                        modifier = Modifier.padding(3.dp),
                    )
                }
            }

            Spacer(Modifier.width(12.dp))

            Column(modifier = Modifier.weight(1f)) {
                // 头部：类别标签 + 标题
                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(6.dp),
                ) {
                    Surface(
                        shape = RoundedCornerShape(6.dp),
                        color = categoryColor.copy(alpha = 0.15f),
                    ) {
                        Row(
                            modifier = Modifier.padding(horizontal = 6.dp, vertical = 2.dp),
                            verticalAlignment = Alignment.CenterVertically,
                            horizontalArrangement = Arrangement.spacedBy(3.dp),
                        ) {
                            Icon(
                                imageVector = iconFor(item.category?.iconKey ?: "fun"),
                                contentDescription = null,
                                tint = categoryColor,
                                modifier = Modifier.size(12.dp),
                            )
                            Text(
                                text = item.category?.name ?: "项目",
                                style = MaterialTheme.typography.labelSmall,
                                fontWeight = FontWeight.Bold,
                                color = categoryColor,
                            )
                        }
                    }

                    Text(
                        text = item.title,
                        style = MaterialTheme.typography.titleMedium,
                        fontWeight = FontWeight.Bold,
                        color = if (item.isCompleted) Slate500 else Slate900,
                        textDecoration = if (item.isCompleted) TextDecoration.LineThrough else TextDecoration.None,
                        maxLines = 1,
                        overflow = TextOverflow.Ellipsis,
                    )
                }

                // 地点行与地图定位按钮
                item.formattedLocation?.let { loc ->
                    Spacer(Modifier.height(8.dp))
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically,
                    ) {
                        Row(
                            verticalAlignment = Alignment.CenterVertically,
                            modifier = Modifier.weight(1f),
                        ) {
                            Icon(
                                imageVector = Icons.Outlined.LocationOn,
                                contentDescription = null,
                                tint = if (item.isCompleted) Slate500 else CruiseAzure,
                                modifier = Modifier.size(14.dp),
                            )
                            Spacer(Modifier.width(3.dp))
                            Text(
                                text = loc,
                                style = MaterialTheme.typography.bodyMedium,
                                fontWeight = FontWeight.Medium,
                                color = if (item.isCompleted) Slate500 else CruiseMarine,
                            )
                        }

                        item.deck?.let { targetDeck ->
                            Surface(
                                shape = RoundedCornerShape(8.dp),
                                color = CruiseAzureContainer,
                                modifier = Modifier
                                    .clip(RoundedCornerShape(8.dp))
                                    .clickable {
                                        val targetQuery = item.placeName?.takeIf { it.isNotEmpty() } ?: item.title
                                        onNavigateToMap(targetDeck, targetQuery)
                                    },
                            ) {
                                Row(
                                    modifier = Modifier.padding(horizontal = 8.dp, vertical = 3.dp),
                                    verticalAlignment = Alignment.CenterVertically,
                                    horizontalArrangement = Arrangement.spacedBy(3.dp),
                                ) {
                                    Icon(
                                        imageVector = Icons.Outlined.Map,
                                        contentDescription = null,
                                        tint = CruiseMarine,
                                        modifier = Modifier.size(13.dp),
                                    )
                                    Text(
                                        text = "定位",
                                        style = MaterialTheme.typography.labelSmall,
                                        fontWeight = FontWeight.Bold,
                                        color = CruiseMarine,
                                    )
                                }
                            }
                        }
                    }
                }

                // 备注
                item.note?.let { n ->
                    Spacer(Modifier.height(6.dp))
                    Text(
                        text = n,
                        style = MaterialTheme.typography.bodySmall,
                        color = Slate500,
                        maxLines = 2,
                        overflow = TextOverflow.Ellipsis,
                    )
                }

                // 排入日程操作栏
                if (!item.isCompleted) {
                    Spacer(Modifier.height(10.dp))
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.End,
                    ) {
                        Surface(
                            shape = RoundedCornerShape(20.dp),
                            color = categoryColor.copy(alpha = 0.12f),
                            border = BorderStroke(1.dp, categoryColor.copy(alpha = 0.3f)),
                            modifier = Modifier
                                .clip(RoundedCornerShape(20.dp))
                                .clickable { onConvertToSchedule() },
                        ) {
                            Row(
                                modifier = Modifier.padding(horizontal = 10.dp, vertical = 5.dp),
                                verticalAlignment = Alignment.CenterVertically,
                                horizontalArrangement = Arrangement.spacedBy(4.dp),
                            ) {
                                Icon(
                                    imageVector = Icons.Outlined.CalendarMonth,
                                    contentDescription = "排入日程",
                                    tint = categoryColor,
                                    modifier = Modifier.size(14.dp),
                                )
                                Text(
                                    text = "📅 排入日程",
                                    style = MaterialTheme.typography.labelSmall,
                                    fontWeight = FontWeight.Bold,
                                    color = categoryColor,
                                )
                            }
                        }
                    }
                }
            }
        }
    }
}

@Composable
fun WishlistConvertDialog(
    item: WishlistItem,
    availableDates: List<LocalDate>,
    onDismiss: () -> Unit,
    onConfirm: (date: LocalDate, startTime: LocalTime, durationMinutes: Int) -> Unit,
) {
    var selectedDate by remember {
        mutableStateOf(availableDates.firstOrNull() ?: LocalDate.now())
    }
    var selectedTime by remember { mutableStateOf(LocalTime.of(14, 0)) }
    var durationMinutes by remember { mutableIntStateOf(60) }

    val defaultWaitMinutes = remember(item) {
        resolveDefaultWaitMinutes(
            placeName = item.placeName,
            deck = item.deck,
            title = item.title,
            presetKey = null,
        )
    }

    AlertDialog(
        onDismissRequest = onDismiss,
        title = {
            Row(
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.spacedBy(8.dp),
            ) {
                Surface(
                    shape = CircleShape,
                    color = CruiseAzureContainer,
                    modifier = Modifier.size(36.dp),
                ) {
                    Box(contentAlignment = Alignment.Center) {
                        Icon(
                            imageVector = Icons.Outlined.CalendarMonth,
                            contentDescription = null,
                            tint = CruiseMarine,
                            modifier = Modifier.size(20.dp),
                        )
                    }
                }
                Text(
                    text = "排入日程时间轴",
                    style = MaterialTheme.typography.titleMedium,
                    fontWeight = FontWeight.Bold,
                    color = Slate900,
                )
            }
        },
        text = {
            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .verticalScroll(rememberScrollState()),
                verticalArrangement = Arrangement.spacedBy(14.dp),
            ) {
                // 项目信息卡片
                Surface(
                    shape = RoundedCornerShape(12.dp),
                    color = Slate100,
                    modifier = Modifier.fillMaxWidth(),
                ) {
                    Column(
                        modifier = Modifier.padding(12.dp),
                        verticalArrangement = Arrangement.spacedBy(4.dp),
                    ) {
                        Text(
                            text = item.title,
                            style = MaterialTheme.typography.titleSmall,
                            fontWeight = FontWeight.Bold,
                            color = Slate900,
                        )
                        item.formattedLocation?.let { loc ->
                            Row(
                                verticalAlignment = Alignment.CenterVertically,
                                horizontalArrangement = Arrangement.spacedBy(4.dp),
                            ) {
                                Icon(
                                    imageVector = Icons.Outlined.LocationOn,
                                    contentDescription = null,
                                    tint = CruiseAzure,
                                    modifier = Modifier.size(13.dp),
                                )
                                Text(
                                    text = loc,
                                    style = MaterialTheme.typography.bodySmall,
                                    color = CruiseMarine,
                                    fontWeight = FontWeight.Medium,
                                )
                            }
                        }
                    }
                }

                // 1. 日期选择
                Column(verticalArrangement = Arrangement.spacedBy(6.dp)) {
                    Text(
                        text = "选择日期",
                        style = MaterialTheme.typography.labelMedium,
                        fontWeight = FontWeight.SemiBold,
                        color = Slate700,
                    )
                    if (availableDates.isNotEmpty()) {
                        Row(
                            modifier = Modifier
                                .fillMaxWidth()
                                .horizontalScroll(rememberScrollState()),
                            horizontalArrangement = Arrangement.spacedBy(8.dp),
                        ) {
                            availableDates.forEachIndexed { index, date ->
                                val isSelected = date == selectedDate
                                FilterChip(
                                    selected = isSelected,
                                    onClick = { selectedDate = date },
                                    label = {
                                        Text("第${index + 1}天 (${date.monthValue}/${date.dayOfMonth})")
                                    },
                                    colors = FilterChipDefaults.filterChipColors(
                                        selectedContainerColor = CruiseAzure,
                                        selectedLabelColor = Color.White,
                                        containerColor = Slate100,
                                        labelColor = Slate700,
                                    ),
                                    shape = RoundedCornerShape(8.dp),
                                )
                            }
                        }
                    } else {
                        Text(
                            text = "${selectedDate.year}年${selectedDate.monthValue}月${selectedDate.dayOfMonth}日",
                            style = MaterialTheme.typography.bodyMedium,
                            color = Slate700,
                        )
                    }
                }

                // 2. 开始时间
                Column(verticalArrangement = Arrangement.spacedBy(6.dp)) {
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically,
                    ) {
                        Text(
                            text = "开始时间",
                            style = MaterialTheme.typography.labelMedium,
                            fontWeight = FontWeight.SemiBold,
                            color = Slate700,
                        )
                        Text(
                            text = "${selectedTime.hour.toString().padStart(2, '0')}:${selectedTime.minute.toString().padStart(2, '0')}",
                            style = MaterialTheme.typography.labelMedium,
                            fontWeight = FontWeight.Bold,
                            color = CruiseAzure,
                        )
                    }
                    TimeWheel(
                        value = selectedTime,
                        onValueChange = { selectedTime = it },
                        modifier = Modifier.fillMaxWidth(),
                        minuteStep = 5,
                    )
                }

                // 3. 时长选择
                Column(verticalArrangement = Arrangement.spacedBy(6.dp)) {
                    Text(
                        text = "活动时长",
                        style = MaterialTheme.typography.labelMedium,
                        fontWeight = FontWeight.SemiBold,
                        color = Slate700,
                    )
                    val durations = listOf(15, 30, 45, 60, 90)
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .horizontalScroll(rememberScrollState()),
                        horizontalArrangement = Arrangement.spacedBy(8.dp),
                    ) {
                        durations.forEach { d ->
                            val isSelected = durationMinutes == d
                            FilterChip(
                                selected = isSelected,
                                onClick = { durationMinutes = d },
                                label = { Text("${d}分钟") },
                                colors = FilterChipDefaults.filterChipColors(
                                    selectedContainerColor = CruiseAzure,
                                    selectedLabelColor = Color.White,
                                    containerColor = Slate100,
                                    labelColor = Slate700,
                                ),
                                shape = RoundedCornerShape(8.dp),
                            )
                        }
                    }
                }

                // 4. 候场提示
                if (defaultWaitMinutes > 0) {
                    Surface(
                        shape = RoundedCornerShape(8.dp),
                        color = CruiseAzureContainer,
                        modifier = Modifier.fillMaxWidth(),
                    ) {
                        Row(
                            modifier = Modifier.padding(horizontal = 10.dp, vertical = 8.dp),
                            verticalAlignment = Alignment.CenterVertically,
                            horizontalArrangement = Arrangement.spacedBy(6.dp),
                        ) {
                            Text(
                                text = "💡 该地点排入后将自动包含 ${defaultWaitMinutes} 分钟候场时间",
                                style = MaterialTheme.typography.bodySmall,
                                color = CruiseMarine,
                                fontWeight = FontWeight.Medium,
                            )
                        }
                    }
                }
            }
        },
        confirmButton = {
            Button(
                onClick = { onConfirm(selectedDate, selectedTime, durationMinutes) },
                colors = ButtonDefaults.buttonColors(containerColor = CruiseAzure),
                shape = RoundedCornerShape(8.dp),
            ) {
                Text("确定排入")
            }
        },
        dismissButton = {
            TextButton(onClick = onDismiss) {
                Text("取消", color = Slate500)
            }
        },
        shape = RoundedCornerShape(16.dp),
    )
}

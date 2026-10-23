@file:OptIn(androidx.compose.material3.ExperimentalMaterial3Api::class)

package com.cruise.assistant.ui.onboarding

import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.outlined.CalendarMonth
import androidx.compose.material.icons.outlined.DirectionsBoat
import androidx.compose.material.icons.outlined.Layers
import androidx.compose.material.icons.outlined.Schedule
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.DatePicker
import androidx.compose.material3.DatePickerDialog
import androidx.compose.material3.DropdownMenuItem
import androidx.compose.material3.ExposedDropdownMenuAnchorType
import androidx.compose.material3.ExposedDropdownMenuBox
import androidx.compose.material3.ExposedDropdownMenuDefaults
import androidx.compose.material3.FilterChip
import androidx.compose.material3.FilterChipDefaults
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.SelectableDates
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.material3.rememberDatePickerState
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.cruise.assistant.R
import com.cruise.assistant.domain.BUILT_IN_SHIPS
import com.cruise.assistant.domain.boardingSlots
import com.cruise.assistant.ui.theme.CruiseAzure
import com.cruise.assistant.ui.theme.CruiseAzureContainer
import com.cruise.assistant.ui.theme.CruiseMarine
import com.cruise.assistant.ui.theme.Slate100
import com.cruise.assistant.ui.theme.Slate200
import com.cruise.assistant.ui.theme.Slate500
import com.cruise.assistant.ui.theme.Slate700
import com.cruise.assistant.ui.theme.Slate900
import java.time.Instant
import java.time.LocalDate
import java.time.ZoneId
import java.time.format.DateTimeFormatter

private val DATE_FMT = DateTimeFormatter.ofPattern("yyyy年M月d日")

@Composable
fun OnboardingScreen(
    state: OnboardingState,
    onShipNameChange: (String) -> Unit,
    onShipSelected: (Int) -> Unit,
    onScheduleSelected: (Int) -> Unit,
    onBoardDateChange: (LocalDate) -> Unit,
    onBoardTimeChange: (java.time.LocalTime) -> Unit,
    onDisembarkDateChange: (LocalDate) -> Unit,
    onMaxDeckChange: (Int) -> Unit,
    onDinnerTimeChange: (java.time.LocalTime) -> Unit,
    onConciergeToggle: (Boolean) -> Unit,
    onAddLeg: (Int) -> Unit,
    onRemoveLeg: (Int) -> Unit,
    onClearLegs: () -> Unit,
    onToggleAshore: (Int) -> Unit,
    onSubmit: () -> Unit,
    /** 已有存档数：>0 时 hero 区角落显示"查看航次存档"入口 */
    archiveCount: Int = 0,
    onViewArchives: () -> Unit = {},
) {
    var pickerTarget by remember { mutableStateOf<PickerTarget?>(null) }
    // 联程编排底部弹层：入口 chip / 摘要行点击打开
    var showLegEditor by remember { mutableStateOf(false) }
    val cardShape = RoundedCornerShape(16.dp)

    Column(
        Modifier
            .fillMaxSize()
            .background(MaterialTheme.colorScheme.background)
            .verticalScroll(rememberScrollState())
            .padding(horizontal = 16.dp, vertical = 20.dp),
    ) {
        // ---- Hero Banner ----
        Box(
            modifier = Modifier
                .fillMaxWidth()
                .clip(cardShape)
                .background(CruiseAzureContainer)
                .padding(20.dp),
        ) {
            Column {
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Box(
                        modifier = Modifier
                            .size(40.dp)
                            .clip(CircleShape)
                            .background(CruiseAzure),
                        contentAlignment = Alignment.Center,
                    ) {
                        Icon(
                            Icons.Outlined.DirectionsBoat,
                            contentDescription = null,
                            tint = Color.White,
                            modifier = Modifier.size(24.dp),
                        )
                    }
                    Spacer(Modifier.width(12.dp))
                    Column {
                        Text(
                            text = stringResource(R.string.onboarding_title),
                            style = MaterialTheme.typography.titleLarge,
                            fontWeight = FontWeight.Bold,
                            color = CruiseMarine,
                        )
                        Text(
                            text = stringResource(R.string.onboarding_subtitle),
                            style = MaterialTheme.typography.bodySmall,
                            color = Slate700,
                        )
                    }
                }
            }
            // 重建航次后回来：hero 区角落的存档入口（有存档才显示）
            if (archiveCount > 0) {
                TextButton(
                    onClick = onViewArchives,
                    modifier = Modifier.align(Alignment.TopEnd),
                ) {
                    Text(
                        stringResource(R.string.onboarding_view_archives),
                        style = MaterialTheme.typography.labelMedium,
                        fontWeight = FontWeight.SemiBold,
                        color = CruiseMarine,
                    )
                }
            }
        }

        Spacer(Modifier.height(16.dp))

        // ---- 卡片 1：船只与船期 ----
        Surface(
            shape = cardShape,
            color = MaterialTheme.colorScheme.surface,
            border = androidx.compose.foundation.BorderStroke(1.dp, Slate200),
            modifier = Modifier.fillMaxWidth(),
        ) {
            Column(Modifier.padding(16.dp)) {
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Icon(
                        Icons.Outlined.DirectionsBoat,
                        contentDescription = null,
                        tint = CruiseAzure,
                        modifier = Modifier.size(18.dp),
                    )
                    Spacer(Modifier.width(8.dp))
                    Text(
                        stringResource(R.string.ship_name_label),
                        style = MaterialTheme.typography.titleMedium,
                        color = Slate900,
                    )
                }
                Spacer(Modifier.height(12.dp))

                var shipMenuExpanded by remember { mutableStateOf(false) }
                ExposedDropdownMenuBox(
                    expanded = shipMenuExpanded,
                    onExpandedChange = { shipMenuExpanded = it },
                    modifier = Modifier.fillMaxWidth(),
                ) {
                    OutlinedTextField(
                        value = if (state.isBuiltInShip) state.shipName
                            else if (state.shipName.isEmpty()) stringResource(R.string.custom_ship)
                            else state.shipName,
                        onValueChange = { },
                        label = { Text(stringResource(R.string.ship_name_label)) },
                        singleLine = true,
                        readOnly = true,
                        trailingIcon = { ExposedDropdownMenuDefaults.TrailingIcon(expanded = shipMenuExpanded) },
                        shape = RoundedCornerShape(12.dp),
                        modifier = Modifier.menuAnchor(ExposedDropdownMenuAnchorType.PrimaryNotEditable, enabled = true).fillMaxWidth(),
                    )
                    ExposedDropdownMenu(
                        expanded = shipMenuExpanded,
                        onDismissRequest = { shipMenuExpanded = false },
                    ) {
                        BUILT_IN_SHIPS.forEachIndexed { index, ship ->
                            DropdownMenuItem(
                                text = { Text(ship.name, fontWeight = FontWeight.Medium) },
                                onClick = {
                                    onShipSelected(index)
                                    shipMenuExpanded = false
                                },
                            )
                        }
                        DropdownMenuItem(
                            text = { Text(stringResource(R.string.custom_ship)) },
                            onClick = {
                                onShipSelected(-1)
                                shipMenuExpanded = false
                            },
                        )
                    }
                }

                if (!state.isBuiltInShip) {
                    Spacer(Modifier.height(8.dp))
                    OutlinedTextField(
                        value = state.shipName,
                        onValueChange = onShipNameChange,
                        label = { Text(stringResource(R.string.ship_name_label)) },
                        singleLine = true,
                        shape = RoundedCornerShape(12.dp),
                        modifier = Modifier.fillMaxWidth(),
                    )
                }

                if (state.isBuiltInShip) {
                    Spacer(Modifier.height(16.dp))
                    Text(stringResource(R.string.schedule_label), style = MaterialTheme.typography.titleSmall, color = Slate700)
                    Spacer(Modifier.height(8.dp))
                    val ship = BUILT_IN_SHIPS[state.selectedShipIndex]
                    Row(horizontalArrangement = Arrangement.spacedBy(8.dp), verticalAlignment = Alignment.CenterVertically) {
                        ship.schedules.forEachIndexed { index, schedule ->
                            FilterChip(
                                selected = state.selectedScheduleIndex == index,
                                onClick = { onScheduleSelected(index) },
                                label = { Text(schedule.label, maxLines = 1) },
                                colors = FilterChipDefaults.filterChipColors(
                                    selectedContainerColor = CruiseAzureContainer,
                                    selectedLabelColor = CruiseMarine,
                                ),
                                modifier = Modifier.weight(1.4f),
                            )
                        }
                        FilterChip(
                            selected = state.selectedScheduleIndex < 0,
                            onClick = { onScheduleSelected(-1) },
                            label = { Text(stringResource(R.string.custom_schedule), style = MaterialTheme.typography.labelSmall, maxLines = 1) },
                            colors = FilterChipDefaults.filterChipColors(
                                selectedContainerColor = CruiseAzureContainer,
                                selectedLabelColor = CruiseMarine,
                            ),
                            modifier = Modifier.weight(0.7f),
                        )
                    }
                }

                if (state.isBuiltInShip && state.selectedScheduleIndex >= 0) {
                    // 联程航次（back-to-back）入口：仅内置船 + 已选船期时可用，
                    // 点击打开编排弹层强制 3/4 交替拼接（仅可从末尾追加/缩短）
                    Spacer(Modifier.height(16.dp))
                    FilterChip(
                        selected = state.isMultiLeg,
                        onClick = { showLegEditor = true },
                        label = { Text(stringResource(R.string.multileg_toggle_label)) },
                        colors = FilterChipDefaults.filterChipColors(
                            selectedContainerColor = CruiseAzureContainer,
                            selectedLabelColor = CruiseMarine,
                        ),
                    )
                    // 摘要文本行：联程时显示，点击也可打开编排弹层
                    state.multiLegSummary?.let { summary ->
                        Spacer(Modifier.height(8.dp))
                        Text(
                            text = stringResource(
                                R.string.multileg_summary_format,
                                summary.legsLabel,
                                summary.totalNights,
                                summary.boardDow,
                                summary.disembarkDow,
                            ),
                            style = MaterialTheme.typography.bodySmall,
                            fontWeight = FontWeight.Medium,
                            color = CruiseMarine,
                            modifier = Modifier
                                .fillMaxWidth()
                                .clip(RoundedCornerShape(8.dp))
                                .background(CruiseAzureContainer)
                                .clickable { showLegEditor = true }
                                .padding(horizontal = 10.dp, vertical = 6.dp),
                        )
                    }
                    // 兰卡威登岸开关：首程是 4夜（新加坡-兰卡威-新加坡航线）时显示，
                    // 勾选后在该程第三天生成 09:00–17:00「兰卡威登岸」上岸游玩日程
                    if (state.scheduleNights == 4) {
                        Spacer(Modifier.height(8.dp))
                        FilterChip(
                            selected = 0 in state.ashoreLegs,
                            onClick = { onToggleAshore(0) },
                            label = { Text(stringResource(R.string.ashore_toggle_label)) },
                            colors = FilterChipDefaults.filterChipColors(
                                selectedContainerColor = CruiseAzureContainer,
                                selectedLabelColor = CruiseMarine,
                            ),
                        )
                        Spacer(Modifier.height(4.dp))
                        Text(
                            text = stringResource(R.string.ashore_card_hint),
                            style = MaterialTheme.typography.bodySmall,
                            color = Slate500,
                        )
                    }
                }

                if (state.isBuiltInShip && state.selectedScheduleIndex >= 0) {
                    Spacer(Modifier.height(16.dp))
                    Text(stringResource(R.string.dinner_time_label), style = MaterialTheme.typography.titleSmall, color = Slate700)
                    Spacer(Modifier.height(8.dp))
                    Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                        listOf(
                            java.time.LocalTime.of(17, 45) to R.string.dinner_time_early,
                            java.time.LocalTime.of(20, 15) to R.string.dinner_time_late,
                        ).forEach { (time, labelRes) ->
                            FilterChip(
                                selected = state.dinnerTime == time,
                                onClick = { onDinnerTimeChange(time) },
                                label = { Text(stringResource(labelRes)) },
                                colors = FilterChipDefaults.filterChipColors(
                                    selectedContainerColor = CruiseAzureContainer,
                                    selectedLabelColor = CruiseMarine,
                                ),
                            )
                        }
                    }
                }
            }
        }

        Spacer(Modifier.height(14.dp))

        // ---- 卡片 2：航程时间与登船 ----
        Surface(
            shape = cardShape,
            color = MaterialTheme.colorScheme.surface,
            border = androidx.compose.foundation.BorderStroke(1.dp, Slate200),
            modifier = Modifier.fillMaxWidth(),
        ) {
            Column(Modifier.padding(16.dp)) {
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Icon(
                        Icons.Outlined.CalendarMonth,
                        contentDescription = null,
                        tint = CruiseAzure,
                        modifier = Modifier.size(18.dp),
                    )
                    Spacer(Modifier.width(8.dp))
                    Text(
                        stringResource(R.string.board_date_label),
                        style = MaterialTheme.typography.titleMedium,
                        color = Slate900,
                    )
                }
                Spacer(Modifier.height(10.dp))
                OutlinedButton(
                    onClick = { pickerTarget = PickerTarget.BOARD },
                    shape = RoundedCornerShape(12.dp),
                    modifier = Modifier.fillMaxWidth(),
                ) {
                    Icon(Icons.Outlined.CalendarMonth, contentDescription = null, modifier = Modifier.size(16.dp))
                    Spacer(Modifier.width(8.dp))
                    Text(state.boardDate.format(DATE_FMT), fontWeight = FontWeight.SemiBold)
                }

                Spacer(Modifier.height(14.dp))
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Icon(
                        Icons.Outlined.Schedule,
                        contentDescription = null,
                        tint = CruiseAzure,
                        modifier = Modifier.size(18.dp),
                    )
                    Spacer(Modifier.width(8.dp))
                    Text(
                        stringResource(R.string.board_time_label),
                        style = MaterialTheme.typography.titleMedium,
                        color = Slate900,
                    )
                }
                Spacer(Modifier.height(8.dp))
                // 礼宾优先登船开关：礼宾客户不受批次限制，11:00 即可登船（风格对齐晚餐时段 chips）
                FilterChip(
                    selected = state.isConcierge,
                    onClick = { onConciergeToggle(!state.isConcierge) },
                    label = { Text(stringResource(R.string.concierge_toggle_label)) },
                    colors = FilterChipDefaults.filterChipColors(
                        selectedContainerColor = CruiseAzureContainer,
                        selectedLabelColor = CruiseMarine,
                    ),
                )
                Spacer(Modifier.height(8.dp))
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .clip(RoundedCornerShape(8.dp))
                        .background(CruiseAzureContainer)
                        .padding(horizontal = 10.dp, vertical = 6.dp),
                    verticalAlignment = Alignment.CenterVertically,
                ) {
                    Text(
                        // 礼宾开启：显示优先登船横幅并隐藏批次网格；关闭：恢复批次区间横幅
                        text = if (state.isConcierge) {
                            stringResource(R.string.concierge_board_banner)
                        } else {
                            "登船批次：%02d:%02d – %02d:%02d".format(
                                state.boardTime.hour, state.boardTime.minute,
                                (state.boardTime.plusMinutes(15).hour + 24) % 24,
                                state.boardTime.plusMinutes(15).minute,
                            )
                        },
                        style = MaterialTheme.typography.titleSmall,
                        fontWeight = FontWeight.Bold,
                        color = CruiseMarine,
                    )
                }
                if (!state.isConcierge) {
                    Spacer(Modifier.height(8.dp))
                    boardingSlots().chunked(3).forEach { rowSlots ->
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.spacedBy(8.dp),
                        ) {
                            rowSlots.forEach { slot ->
                                FilterChip(
                                    selected = state.boardTime == slot,
                                    onClick = { onBoardTimeChange(slot) },
                                    label = {
                                        Text(
                                            text = "%02d:%02d".format(slot.hour, slot.minute),
                                            style = MaterialTheme.typography.labelMedium,
                                            fontWeight = if (state.boardTime == slot) FontWeight.Bold else FontWeight.Medium,
                                            textAlign = TextAlign.Center,
                                            modifier = Modifier.fillMaxWidth(),
                                        )
                                    },
                                    colors = FilterChipDefaults.filterChipColors(
                                        selectedContainerColor = CruiseAzureContainer,
                                        selectedLabelColor = CruiseMarine,
                                    ),
                                    modifier = Modifier.weight(1f),
                                )
                            }
                        }
                        Spacer(Modifier.height(4.dp))
                    }
                }

                Spacer(Modifier.height(14.dp))
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Icon(
                        Icons.Outlined.CalendarMonth,
                        contentDescription = null,
                        tint = CruiseAzure,
                        modifier = Modifier.size(18.dp),
                    )
                    Spacer(Modifier.width(8.dp))
                    Text(
                        stringResource(R.string.disembark_date_label),
                        style = MaterialTheme.typography.titleMedium,
                        color = Slate900,
                    )
                }
                Spacer(Modifier.height(10.dp))
                if (state.isMultiLeg) {
                    // 联程：下船日自动计算，不可点击（避免手动改动破坏联程链）
                    OutlinedButton(
                        onClick = { },
                        enabled = false,
                        shape = RoundedCornerShape(12.dp),
                        modifier = Modifier.fillMaxWidth(),
                    ) {
                        Icon(Icons.Outlined.CalendarMonth, contentDescription = null, modifier = Modifier.size(16.dp))
                        Spacer(Modifier.width(8.dp))
                        Text(state.disembarkDate.format(DATE_FMT), fontWeight = FontWeight.SemiBold)
                    }
                    Spacer(Modifier.height(4.dp))
                    Text(
                        stringResource(R.string.multileg_disembark_auto_hint),
                        style = MaterialTheme.typography.bodySmall,
                        color = Slate500,
                    )
                } else {
                    OutlinedButton(
                        onClick = { pickerTarget = PickerTarget.DISEMBARK },
                        shape = RoundedCornerShape(12.dp),
                        modifier = Modifier.fillMaxWidth(),
                    ) {
                        Icon(Icons.Outlined.CalendarMonth, contentDescription = null, modifier = Modifier.size(16.dp))
                        Spacer(Modifier.width(8.dp))
                        Text(state.disembarkDate.format(DATE_FMT), fontWeight = FontWeight.SemiBold)
                    }
                    Spacer(Modifier.height(4.dp))
                    Text(
                        stringResource(R.string.disembark_time_hint),
                        style = MaterialTheme.typography.bodySmall,
                        color = Slate500,
                    )
                }
            }
        }

        Spacer(Modifier.height(14.dp))

        // ---- 卡片 3：甲板配置 ----
        Surface(
            shape = cardShape,
            color = MaterialTheme.colorScheme.surface,
            border = androidx.compose.foundation.BorderStroke(1.dp, Slate200),
            modifier = Modifier.fillMaxWidth(),
        ) {
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(16.dp),
                verticalAlignment = Alignment.CenterVertically,
            ) {
                Icon(
                    Icons.Outlined.Layers,
                    contentDescription = null,
                    tint = CruiseAzure,
                    modifier = Modifier.size(18.dp),
                )
                Spacer(Modifier.width(8.dp))
                Text(
                    stringResource(R.string.max_deck_label),
                    style = MaterialTheme.typography.titleMedium,
                    color = Slate900,
                    modifier = Modifier.weight(1f),
                )

                val lockedDeck = state.isBuiltInShip &&
                    BUILT_IN_SHIPS.getOrNull(state.selectedShipIndex)?.maxDeck != null
                if (lockedDeck) {
                    Row(
                        modifier = Modifier
                            .clip(RoundedCornerShape(8.dp))
                            .background(CruiseAzureContainer)
                            .padding(horizontal = 10.dp, vertical = 6.dp),
                    ) {
                        Text(
                            "Deck ${state.minDeck}–${state.maxDeck}",
                            style = MaterialTheme.typography.titleSmall,
                            fontWeight = FontWeight.Bold,
                            color = CruiseMarine,
                        )
                    }
                } else {
                    Row(
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.spacedBy(4.dp),
                    ) {
                        IconButton(
                            onClick = { onMaxDeckChange(state.maxDeck - 1) },
                            enabled = state.maxDeck > 1,
                            modifier = Modifier
                                .size(36.dp)
                                .clip(CircleShape)
                                .background(Slate100),
                        ) {
                            Text("−", fontSize = 18.sp, fontWeight = FontWeight.Bold, color = Slate700)
                        }
                        Text(
                            "Deck ${state.maxDeck}",
                            style = MaterialTheme.typography.titleSmall,
                            fontWeight = FontWeight.Bold,
                            color = Slate900,
                            modifier = Modifier.padding(horizontal = 8.dp),
                        )
                        IconButton(
                            onClick = { onMaxDeckChange(state.maxDeck + 1) },
                            enabled = state.maxDeck < 30,
                            modifier = Modifier
                                .size(36.dp)
                                .clip(CircleShape)
                                .background(Slate100),
                        ) {
                            Text("+", fontSize = 18.sp, fontWeight = FontWeight.Bold, color = Slate700)
                        }
                    }
                }
            }
        }

        state.errorResId?.let { resId ->
            Spacer(Modifier.height(12.dp))
            Text(
                stringResource(resId),
                color = MaterialTheme.colorScheme.error,
                style = MaterialTheme.typography.bodySmall,
            )
        }

        Spacer(Modifier.height(28.dp))
        Button(
            onClick = onSubmit,
            enabled = !state.saving,
            shape = RoundedCornerShape(16.dp),
            colors = ButtonDefaults.buttonColors(containerColor = CruiseAzure),
            modifier = Modifier
                .fillMaxWidth()
                .height(54.dp),
        ) {
            Text(
                stringResource(R.string.generate_timeline),
                fontSize = 16.sp,
                fontWeight = FontWeight.Bold,
            )
        }
        Spacer(Modifier.height(24.dp))
    }

    // 联程编排底部弹层：强制 3/4 交替拼接，仅可从末尾追加/缩短；4夜程可勾选登岸兰卡威
    if (showLegEditor) {
        MultiLegEditorSheet(
            state = state,
            onAddLeg = onAddLeg,
            onRemoveLeg = onRemoveLeg,
            onClearLegs = onClearLegs,
            onToggleAshore = onToggleAshore,
            onDismiss = { showLegEditor = false },
        )
    }

    pickerTarget?.let { target ->
        val initial = when (target) {
            PickerTarget.BOARD -> state.boardDate
            PickerTarget.DISEMBARK -> state.disembarkDate
        }
        val lockedBoardDay = if (target == PickerTarget.BOARD && state.selectedScheduleIndex >= 0) {
            BUILT_IN_SHIPS.getOrNull(state.selectedShipIndex)
                ?.schedules?.getOrNull(state.selectedScheduleIndex)
                ?.boardDayOfWeek
        } else {
            null
        }
        val selectableDates = remember(lockedBoardDay) {
            object : SelectableDates {
                override fun isSelectableDate(utcTimeMillis: Long): Boolean {
                    val dow = lockedBoardDay ?: return true
                    val date = Instant.ofEpochMilli(utcTimeMillis)
                        .atZone(java.time.ZoneOffset.UTC).toLocalDate()
                    return date.dayOfWeek == dow
                }
            }
        }
        val pickerState = rememberDatePickerState(
            initialSelectedDateMillis = initial.atStartOfDay(java.time.ZoneOffset.UTC)
                .toInstant().toEpochMilli(),
            selectableDates = selectableDates,
        )
        DatePickerDialog(
            onDismissRequest = { pickerTarget = null },
            confirmButton = {
                TextButton(onClick = {
                    pickerState.selectedDateMillis?.let { millis ->
                        val picked = Instant.ofEpochMilli(millis)
                            .atZone(java.time.ZoneOffset.UTC).toLocalDate()
                        when (target) {
                            PickerTarget.BOARD -> onBoardDateChange(picked)
                            PickerTarget.DISEMBARK -> onDisembarkDateChange(picked)
                        }
                    }
                    pickerTarget = null
                }) { Text(stringResource(R.string.confirm), color = CruiseAzure) }
            },
            dismissButton = {
                TextButton(onClick = { pickerTarget = null }) {
                    Text(stringResource(R.string.cancel), color = Slate500)
                }
            },
        ) {
            if (lockedBoardDay != null) {
                val dowLabel = when (lockedBoardDay) {
                    java.time.DayOfWeek.MONDAY -> "周一"
                    java.time.DayOfWeek.THURSDAY -> "周四"
                    else -> lockedBoardDay.getDisplayName(java.time.format.TextStyle.NARROW, java.util.Locale.CHINA)
                }
                Text(
                    "已按船期锁定，仅可选择 $dowLabel 登船",
                    style = MaterialTheme.typography.bodySmall,
                    color = CruiseMarine,
                    modifier = Modifier.padding(horizontal = 24.dp, vertical = 4.dp),
                )
            }
            DatePicker(state = pickerState)
        }
    }
}

private enum class PickerTarget { BOARD, DISEMBARK }


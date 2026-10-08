@file:OptIn(
    androidx.compose.material3.ExperimentalMaterial3Api::class,
    androidx.compose.foundation.layout.ExperimentalLayoutApi::class,
)

package com.cruise.assistant.ui.event

import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.FlowRow
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.grid.GridCells
import androidx.compose.foundation.lazy.grid.LazyVerticalGrid
import androidx.compose.foundation.lazy.grid.items
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.outlined.Category
import androidx.compose.material.icons.outlined.Close
import androidx.compose.material.icons.outlined.EditNote
import androidx.compose.material.icons.outlined.Place
import androidx.compose.material.icons.outlined.Repeat
import androidx.compose.material.icons.outlined.Restaurant
import androidx.compose.material.icons.outlined.Schedule
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.DropdownMenuItem
import androidx.compose.material3.ExposedDropdownMenuAnchorType
import androidx.compose.material3.ExposedDropdownMenuBox
import androidx.compose.material3.ExposedDropdownMenuDefaults
import androidx.compose.material3.FilterChip
import androidx.compose.material3.FilterChipDefaults
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.ModalBottomSheet
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.OutlinedTextFieldDefaults
import androidx.compose.material3.Surface
import androidx.compose.material3.Switch
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.material3.rememberModalBottomSheetState
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.cruise.assistant.R
import com.cruise.assistant.domain.DISNEY_ADVENTURE_SHOWS
import com.cruise.assistant.domain.MealPeriod
import com.cruise.assistant.domain.ShipZone
import com.cruise.assistant.domain.ShowPreset
import com.cruise.assistant.domain.locationPresetOptions
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
import java.time.LocalTime
import java.time.format.DateTimeFormatter

private val RECUR_FMT = DateTimeFormatter.ofPattern("M月d日")

@Composable
fun EventSheet(
    state: EventSheetState,
    onCategorySelect: (Long) -> Unit,
    onManageCategories: () -> Unit,
    onTitleChange: (String) -> Unit,
    onDeckChange: (Int) -> Unit,
    onZoneChange: (ShipZone) -> Unit,
    onPlaceNameChange: (String) -> Unit,
    onNoLocationToggle: (Boolean) -> Unit,
    onSelectPresetLocation: (com.cruise.assistant.domain.LocationCatalogEntry) -> Unit,
    onCustomLocationSelected: () -> Unit,
    onShowPresetSelect: (ShowPreset) -> Unit = {},
    onCustomShowSelected: () -> Unit = {},
    onMealPeriodSelect: (MealPeriod?) -> Unit,
    onWaitMinutesChange: (Int) -> Unit = {},
    onNoteChange: (String) -> Unit = {},
    onStartChange: (LocalTime) -> Unit,
    onEndChange: (LocalTime) -> Unit,
    onRecurringToggle: (Boolean) -> Unit,
    onSave: () -> Unit,
    onHoursCheckOverride: () -> Unit,
    onHoursCheckDismiss: () -> Unit,
    onDismiss: () -> Unit,
) {
    val sheetState = rememberModalBottomSheetState(skipPartiallyExpanded = true)
    val cardShape = RoundedCornerShape(16.dp)

    ModalBottomSheet(
        onDismissRequest = onDismiss,
        sheetState = sheetState,
        containerColor = MaterialTheme.colorScheme.background,
    ) {
        Column(
            Modifier
                .verticalScroll(rememberScrollState())
                .padding(horizontal = 16.dp)
                .padding(bottom = 28.dp),
        ) {
            // ---- 顶栏标题与关闭按钮 ----
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(vertical = 4.dp),
                verticalAlignment = Alignment.CenterVertically,
            ) {
                Text(
                    stringResource(
                        if (state.isEditing) R.string.edit_event else R.string.add_event,
                    ),
                    style = MaterialTheme.typography.titleLarge,
                    fontWeight = FontWeight.Bold,
                    color = Slate900,
                    modifier = Modifier.weight(1f),
                )
                IconButton(
                    onClick = onDismiss,
                    modifier = Modifier
                        .size(36.dp)
                        .clip(CircleShape)
                        .background(Slate100),
                ) {
                    Icon(
                        Icons.Outlined.Close,
                        contentDescription = stringResource(R.string.cancel),
                        tint = Slate700,
                        modifier = Modifier.size(18.dp),
                    )
                }
            }

            Spacer(Modifier.height(12.dp))

            // ---- 卡片 1：类别选择与事件名称 ----
            Surface(
                shape = cardShape,
                color = MaterialTheme.colorScheme.surface,
                border = androidx.compose.foundation.BorderStroke(1.dp, Slate200),
                modifier = Modifier.fillMaxWidth(),
            ) {
                Column(Modifier.padding(16.dp)) {
                    SectionHeader(
                        title = stringResource(R.string.select_category),
                        icon = Icons.Outlined.Category,
                    )
                    Spacer(Modifier.height(10.dp))

                    val rowCount = ((state.categories.size + 1) + 3) / 4
                    LazyVerticalGrid(
                        columns = GridCells.Fixed(4),
                        modifier = Modifier.height((rowCount * 80).dp),
                        horizontalArrangement = Arrangement.spacedBy(8.dp),
                        verticalArrangement = Arrangement.spacedBy(8.dp),
                    ) {
                        items(state.categories) { category ->
                            val selected = category.id == state.selectedCategoryId
                            val tileShape = RoundedCornerShape(12.dp)
                            val catColor = Color(category.colorArgb)

                            Column(
                                horizontalAlignment = Alignment.CenterHorizontally,
                                modifier = Modifier
                                    .clip(tileShape)
                                    .background(if (selected) catColor.copy(alpha = 0.15f) else Slate100)
                                    .border(
                                        width = if (selected) 1.5.dp else 1.dp,
                                        color = if (selected) catColor else Slate200,
                                        shape = tileShape,
                                    )
                                    .then(
                                        if (state.isSystemEvent) Modifier
                                        else Modifier.clickable { onCategorySelect(category.id) }
                                    )
                                    .padding(vertical = 8.dp, horizontal = 4.dp),
                            ) {
                                Icon(
                                    iconFor(category.iconKey),
                                    contentDescription = category.name,
                                    tint = if (selected) catColor else Slate700,
                                    modifier = Modifier.size(24.dp),
                                )
                                Spacer(Modifier.height(4.dp))
                                Text(
                                    category.name,
                                    style = MaterialTheme.typography.labelSmall,
                                    fontSize = 11.sp,
                                    fontWeight = if (selected) FontWeight.Bold else FontWeight.Normal,
                                    color = if (selected) Slate900 else Slate700,
                                    maxLines = 1,
                                    overflow = TextOverflow.Ellipsis,
                                    textAlign = TextAlign.Center,
                                )
                            }
                        }
                        item {
                            val tileShape = RoundedCornerShape(12.dp)
                            Column(
                                horizontalAlignment = Alignment.CenterHorizontally,
                                modifier = Modifier
                                    .clip(tileShape)
                                    .background(Slate100)
                                    .border(1.dp, Slate200, tileShape)
                                    .clickable(onClick = onManageCategories)
                                    .padding(vertical = 8.dp, horizontal = 4.dp),
                            ) {
                                Icon(
                                    Icons.Filled.Add,
                                    contentDescription = null,
                                    tint = Slate500,
                                    modifier = Modifier.size(24.dp),
                                )
                                Spacer(Modifier.height(4.dp))
                                Text(
                                    stringResource(R.string.manage_categories),
                                    style = MaterialTheme.typography.labelSmall,
                                    fontSize = 11.sp,
                                    color = Slate500,
                                    maxLines = 1,
                                    overflow = TextOverflow.Ellipsis,
                                    textAlign = TextAlign.Center,
                                )
                            }
                        }
                    }

                    Spacer(Modifier.height(14.dp))
                    if (state.categoryIconKey == "show" && state.showPresetMode && !state.isSystemEvent) {
                        var showMenuOpen by remember { mutableStateOf(false) }
                        ExposedDropdownMenuBox(
                            expanded = showMenuOpen,
                            onExpandedChange = { showMenuOpen = it },
                            modifier = Modifier.fillMaxWidth(),
                        ) {
                            OutlinedTextField(
                                value = state.title.ifEmpty { "选择预置演出" },
                                onValueChange = {},
                                label = { Text(stringResource(R.string.event_title_hint)) },
                                readOnly = true,
                                singleLine = true,
                                trailingIcon = { ExposedDropdownMenuDefaults.TrailingIcon(expanded = showMenuOpen) },
                                shape = RoundedCornerShape(12.dp),
                                modifier = Modifier
                                    .menuAnchor(ExposedDropdownMenuAnchorType.PrimaryNotEditable, enabled = true)
                                    .fillMaxWidth(),
                            )
                            ExposedDropdownMenu(
                                expanded = showMenuOpen,
                                onDismissRequest = { showMenuOpen = false },
                            ) {
                                DISNEY_ADVENTURE_SHOWS.forEach { show ->
                                    val selected = state.title == show.title
                                    DropdownMenuItem(
                                        text = {
                                            Column {
                                                Text(
                                                    show.title,
                                                    fontWeight = if (selected) FontWeight.Bold else FontWeight.Normal,
                                                )
                                                Text(
                                                    "Deck ${show.deck} ${show.zone.label} · ${show.placeName}",
                                                    style = MaterialTheme.typography.bodySmall,
                                                    color = Slate500,
                                                )
                                            }
                                        },
                                        onClick = {
                                            onShowPresetSelect(show)
                                            showMenuOpen = false
                                        },
                                        modifier = if (selected) Modifier.background(CruiseAzureContainer) else Modifier,
                                    )
                                }
                                HorizontalDivider(color = Slate200)
                                DropdownMenuItem(
                                    text = {
                                        Text(
                                            "✍️ 自定义演出",
                                            color = CruiseAzure,
                                            fontWeight = FontWeight.SemiBold,
                                        )
                                    },
                                    onClick = {
                                        onCustomShowSelected()
                                        showMenuOpen = false
                                    },
                                )
                            }
                        }
                    } else {
                        OutlinedTextField(
                            value = state.title,
                            onValueChange = onTitleChange,
                            label = { Text(stringResource(R.string.event_title_hint)) },
                            singleLine = true,
                            shape = RoundedCornerShape(12.dp),
                            enabled = !state.isSystemEvent,
                            modifier = Modifier.fillMaxWidth(),
                        )
                    }

                    if (state.isSystemEvent) {
                        Spacer(Modifier.height(6.dp))
                        Text(
                            stringResource(R.string.system_event_time_only),
                            style = MaterialTheme.typography.bodySmall,
                            color = Slate500,
                        )
                    }
                }
            }

            // ---- 卡片 2：餐次二级选项（仅餐食类别且内置船显示）----
            if (!state.isSystemEvent && state.showMealPeriodRow) {
                Spacer(Modifier.height(12.dp))
                Surface(
                    shape = cardShape,
                    color = MaterialTheme.colorScheme.surface,
                    border = androidx.compose.foundation.BorderStroke(1.dp, Slate200),
                    modifier = Modifier.fillMaxWidth(),
                ) {
                    Column(Modifier.padding(16.dp)) {
                        SectionHeader(
                            title = stringResource(R.string.meal_period_label),
                            icon = Icons.Outlined.Restaurant,
                        )
                        Spacer(Modifier.height(10.dp))
                        FlowRow(
                            horizontalArrangement = Arrangement.spacedBy(8.dp),
                            verticalArrangement = Arrangement.spacedBy(8.dp),
                        ) {
                            FilterChip(
                                selected = state.selectedMealPeriod == null,
                                onClick = { onMealPeriodSelect(null) },
                                label = { Text(stringResource(R.string.meal_period_all)) },
                                colors = FilterChipDefaults.filterChipColors(
                                    selectedContainerColor = CruiseAzureContainer,
                                    selectedLabelColor = CruiseMarine,
                                ),
                            )
                            MealPeriod.entries.forEach { period ->
                                FilterChip(
                                    selected = state.selectedMealPeriod == period,
                                    onClick = { onMealPeriodSelect(period) },
                                    label = { Text(period.label) },
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

            // ---- 卡片 3：地点选择 ----
            if (!state.isSystemEvent) {
                Spacer(Modifier.height(12.dp))
                Surface(
                    shape = cardShape,
                    color = MaterialTheme.colorScheme.surface,
                    border = androidx.compose.foundation.BorderStroke(1.dp, Slate200),
                    modifier = Modifier.fillMaxWidth(),
                ) {
                    Column(Modifier.padding(16.dp)) {
                        SectionHeader(
                            title = stringResource(R.string.location_label),
                            icon = Icons.Outlined.Place,
                        )
                        Spacer(Modifier.height(10.dp))
                        LocationPicker(
                            maxDeck = state.maxDeck,
                            minDeck = state.minDeck,
                            deck = state.deck,
                            zone = state.zone,
                            placeName = state.placeName,
                            presets = state.presets,
                            noLocation = state.noLocation,
                            onDeckChange = onDeckChange,
                            onZoneChange = onZoneChange,
                            onPlaceNameChange = onPlaceNameChange,
                            onNoLocationToggle = onNoLocationToggle,
                            presetOptions = locationPresetOptions(
                                state.categoryIconKey ?: "",
                                state.selectedMealPeriod,
                            ),
                            presetMode = state.locationPresetMode,
                            onSelectPreset = onSelectPresetLocation,
                            onCustomLocation = onCustomLocationSelected,
                        )
                    }
                }
            }

            // ---- 卡片：提前候场时间（仅演出类别显示）----
            if (!state.isSystemEvent && state.categoryIconKey == "show") {
                Spacer(Modifier.height(12.dp))
                Surface(
                    shape = cardShape,
                    color = MaterialTheme.colorScheme.surface,
                    border = androidx.compose.foundation.BorderStroke(1.dp, Slate200),
                    modifier = Modifier.fillMaxWidth(),
                ) {
                    Column(Modifier.padding(16.dp)) {
                        SectionHeader(
                            title = stringResource(R.string.lead_time_label),
                            icon = Icons.Outlined.Schedule,
                        )
                        Spacer(Modifier.height(10.dp))
                        var waitMenuOpen by remember { mutableStateOf(false) }
                        ExposedDropdownMenuBox(
                            expanded = waitMenuOpen,
                            onExpandedChange = { waitMenuOpen = it },
                            modifier = Modifier.fillMaxWidth(),
                        ) {
                            OutlinedTextField(
                                value = if (state.waitMinutes == 0) {
                                    stringResource(R.string.lead_time_none)
                                } else {
                                    stringResource(R.string.lead_time_format, state.waitMinutes)
                                },
                                onValueChange = {},
                                label = { Text(stringResource(R.string.lead_time_label)) },
                                readOnly = true,
                                singleLine = true,
                                trailingIcon = { ExposedDropdownMenuDefaults.TrailingIcon(expanded = waitMenuOpen) },
                                shape = RoundedCornerShape(12.dp),
                                modifier = Modifier
                                    .menuAnchor(ExposedDropdownMenuAnchorType.PrimaryNotEditable, enabled = true)
                                    .fillMaxWidth(),
                            )
                            ExposedDropdownMenu(
                                expanded = waitMenuOpen,
                                onDismissRequest = { waitMenuOpen = false },
                            ) {
                                (0..120 step 5).forEach { mins ->
                                    val selected = state.waitMinutes == mins
                                    DropdownMenuItem(
                                        text = {
                                            Text(
                                                text = if (mins == 0) {
                                                    stringResource(R.string.lead_time_none)
                                                } else {
                                                    stringResource(R.string.lead_time_format, mins)
                                                },
                                                fontWeight = if (selected) FontWeight.Bold else FontWeight.Normal,
                                                color = if (selected) CruiseMarine else Slate900,
                                            )
                                        },
                                        onClick = {
                                            onWaitMinutesChange(mins)
                                            waitMenuOpen = false
                                        },
                                        modifier = if (selected) Modifier.background(CruiseAzureContainer) else Modifier,
                                    )
                                }
                            }
                        }
                        Spacer(Modifier.height(6.dp))
                        Text(
                            stringResource(R.string.lead_time_hint),
                            style = MaterialTheme.typography.bodySmall,
                            color = Slate500,
                        )
                    }
                }
            }

            // ---- 卡片：备注（非系统事件显示）----
            if (!state.isSystemEvent) {
                Spacer(Modifier.height(12.dp))
                Surface(
                    shape = cardShape,
                    color = MaterialTheme.colorScheme.surface,
                    border = androidx.compose.foundation.BorderStroke(1.dp, Slate200),
                    modifier = Modifier.fillMaxWidth(),
                ) {
                    Column(Modifier.padding(16.dp)) {
                        SectionHeader(
                            title = stringResource(R.string.note_label),
                            icon = Icons.Outlined.EditNote,
                        )
                        Spacer(Modifier.height(10.dp))
                        OutlinedTextField(
                            value = state.note,
                            onValueChange = onNoteChange,
                            placeholder = { Text(stringResource(R.string.note_hint)) },
                            minLines = 2,
                            maxLines = 4,
                            shape = RoundedCornerShape(12.dp),
                            colors = OutlinedTextFieldDefaults.colors(
                                focusedBorderColor = CruiseAzure,
                                focusedLabelColor = CruiseAzure,
                            ),
                            modifier = Modifier.fillMaxWidth(),
                        )
                    }
                }
            }

            // ---- 卡片 4：起止时间 ----
            Spacer(Modifier.height(12.dp))
            Surface(
                shape = cardShape,
                color = MaterialTheme.colorScheme.surface,
                border = androidx.compose.foundation.BorderStroke(1.dp, Slate200),
                modifier = Modifier.fillMaxWidth(),
            ) {
                Column(Modifier.padding(16.dp)) {
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        SectionHeader(
                            title = stringResource(R.string.time_label),
                            icon = Icons.Outlined.Schedule,
                        )
                        Spacer(Modifier.weight(1f))
                        val durationMinutes = calculateDuration(state.startTime, state.endTime)
                        Row(
                            modifier = Modifier
                                .clip(RoundedCornerShape(6.dp))
                                .background(CruiseAzureContainer)
                                .padding(horizontal = 8.dp, vertical = 2.dp),
                        ) {
                            Text(
                                text = "${formatTime(state.startTime)}–${formatTime(state.endTime)} (${durationMinutes}m)",
                                style = MaterialTheme.typography.labelSmall,
                                fontWeight = FontWeight.Bold,
                                color = CruiseMarine,
                                maxLines = 1,
                                softWrap = false,
                            )
                        }
                    }

                    Spacer(Modifier.height(12.dp))
                    Row(
                        Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.spacedBy(12.dp),
                    ) {
                        Column(Modifier.weight(1f)) {
                            Text(
                                stringResource(R.string.start_time),
                                style = MaterialTheme.typography.labelMedium,
                                fontWeight = FontWeight.Medium,
                                color = Slate700,
                            )
                            Spacer(Modifier.height(6.dp))
                            TimeWheel(state.startTime, onStartChange, Modifier.fillMaxWidth())
                        }
                        Column(Modifier.weight(1f)) {
                            Text(
                                stringResource(R.string.end_time),
                                style = MaterialTheme.typography.labelMedium,
                                fontWeight = FontWeight.Medium,
                                color = Slate700,
                            )
                            Spacer(Modifier.height(6.dp))
                            TimeWheel(state.endTime, onEndChange, Modifier.fillMaxWidth())
                        }
                    }
                }
            }

            // ---- 卡片 5：每日循环 ----
            if (!state.isEditing) {
                Spacer(Modifier.height(12.dp))
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
                        Box(
                            modifier = Modifier
                                .size(28.dp)
                                .clip(CircleShape)
                                .background(CruiseAzureContainer),
                            contentAlignment = Alignment.Center,
                        ) {
                            Icon(
                                Icons.Outlined.Repeat,
                                contentDescription = null,
                                tint = CruiseMarine,
                                modifier = Modifier.size(16.dp),
                            )
                        }
                        Spacer(Modifier.width(10.dp))
                        Column(Modifier.weight(1f)) {
                            Text(
                                stringResource(R.string.recur_daily),
                                style = MaterialTheme.typography.titleSmall,
                                fontWeight = FontWeight.Bold,
                                color = Slate900,
                            )
                            state.recurUntil?.let { until ->
                                if (state.recurring) {
                                    Text(
                                        stringResource(R.string.recur_until_format, until.format(RECUR_FMT)),
                                        style = MaterialTheme.typography.bodySmall,
                                        color = Slate500,
                                    )
                                }
                            }
                        }
                        Switch(checked = state.recurring, onCheckedChange = onRecurringToggle)
                    }
                }
            }

            state.errorResId?.let { resId ->
                Spacer(Modifier.height(10.dp))
                Text(
                    stringResource(resId),
                    color = MaterialTheme.colorScheme.error,
                    style = MaterialTheme.typography.bodySmall,
                )
            }

            // ---- 底部确认按钮 ----
            Spacer(Modifier.height(20.dp))
            Button(
                onClick = onSave,
                enabled = !state.saving,
                shape = RoundedCornerShape(16.dp),
                colors = ButtonDefaults.buttonColors(containerColor = CruiseAzure),
                modifier = Modifier
                    .fillMaxWidth()
                    .height(52.dp),
            ) {
                Text(
                    stringResource(R.string.confirm),
                    fontWeight = FontWeight.Bold,
                    fontSize = 16.sp,
                )
            }
        }

        // ---- 营业时间冲突提示 ----
        if (state.hoursCheckPending) {
            AlertDialog(
                onDismissRequest = onHoursCheckDismiss,
                title = { Text(stringResource(R.string.hours_check_title), fontWeight = FontWeight.Bold) },
                text = { Text(state.hoursCheckMessage.orEmpty()) },
                confirmButton = {
                    TextButton(onClick = onHoursCheckOverride) {
                        Text(stringResource(R.string.hours_check_confirm), color = CruiseAzure)
                    }
                },
                dismissButton = {
                    TextButton(onClick = onHoursCheckDismiss) {
                        Text(stringResource(R.string.hours_check_back), color = Slate500)
                    }
                },
            )
        }
    }
}

@Composable
private fun SectionHeader(title: String, icon: ImageVector) {
    Row(verticalAlignment = Alignment.CenterVertically) {
        Box(
            modifier = Modifier
                .size(28.dp)
                .clip(CircleShape)
                .background(CruiseAzureContainer),
            contentAlignment = Alignment.Center,
        ) {
            Icon(
                icon,
                contentDescription = null,
                tint = CruiseMarine,
                modifier = Modifier.size(16.dp),
            )
        }
        Spacer(Modifier.width(8.dp))
        Text(
            title,
            style = MaterialTheme.typography.titleSmall,
            fontWeight = FontWeight.Bold,
            color = Slate900,
        )
    }
}

private fun formatTime(t: LocalTime): String = "%02d:%02d".format(t.hour, t.minute)

private fun calculateDuration(start: LocalTime, end: LocalTime): Int {
    val startMin = start.hour * 60 + start.minute
    val endMin = end.hour * 60 + end.minute
    return (endMin - startMin).coerceAtLeast(0)
}


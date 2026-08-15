@file:OptIn(
    androidx.compose.material3.ExperimentalMaterial3Api::class,
    androidx.compose.foundation.layout.ExperimentalLayoutApi::class,
)

package com.cruise.assistant.ui.event

import androidx.compose.foundation.background
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
import androidx.compose.foundation.lazy.grid.GridCells
import androidx.compose.foundation.lazy.grid.LazyVerticalGrid
import androidx.compose.foundation.lazy.grid.items
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Add
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Button
import androidx.compose.material3.FilterChip
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.ModalBottomSheet
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Switch
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.material3.rememberModalBottomSheetState
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.FilterChipDefaults
import com.cruise.assistant.R
import com.cruise.assistant.ui.theme.CruiseAzure
import com.cruise.assistant.ui.theme.CruiseAzureContainer
import com.cruise.assistant.ui.theme.CruiseMarine
import com.cruise.assistant.ui.theme.Slate100
import com.cruise.assistant.ui.theme.Slate200
import com.cruise.assistant.ui.theme.Slate500
import com.cruise.assistant.ui.theme.Slate700
import com.cruise.assistant.ui.theme.Slate900
import com.cruise.assistant.domain.MealPeriod
import com.cruise.assistant.domain.ShipZone
import com.cruise.assistant.domain.locationPresetOptions
import com.cruise.assistant.ui.common.TimeWheel
import com.cruise.assistant.ui.common.iconFor
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
    onMealPeriodSelect: (MealPeriod?) -> Unit,
    onStartChange: (LocalTime) -> Unit,
    onEndChange: (LocalTime) -> Unit,
    onRecurringToggle: (Boolean) -> Unit,
    onSave: () -> Unit,
    onHoursCheckOverride: () -> Unit,
    onHoursCheckDismiss: () -> Unit,
    onDismiss: () -> Unit,
) {
    val sheetState = rememberModalBottomSheetState(skipPartiallyExpanded = true)

    ModalBottomSheet(onDismissRequest = onDismiss, sheetState = sheetState) {
        Column(
            Modifier
                .verticalScroll(rememberScrollState())
                .padding(horizontal = 20.dp)
                .padding(bottom = 24.dp),
        ) {
            Text(
                stringResource(
                    if (state.isEditing) R.string.edit_event else R.string.add_event,
                ),
                style = MaterialTheme.typography.titleLarge,
                fontWeight = FontWeight.Bold,
                color = Slate900,
            )
            Spacer(Modifier.height(16.dp))

            // ---- 选择事项（截图2 的图标网格）----
            Text(stringResource(R.string.select_category), style = MaterialTheme.typography.titleMedium, fontWeight = FontWeight.SemiBold, color = Slate900)
            Spacer(Modifier.height(8.dp))
            LazyVerticalGrid(
                columns = GridCells.Fixed(5),
                modifier = Modifier.height(if (state.categories.size > 5) 180.dp else 96.dp),
                horizontalArrangement = Arrangement.spacedBy(8.dp),
                verticalArrangement = Arrangement.spacedBy(8.dp),
            ) {
                items(state.categories) { category ->
                    val selected = category.id == state.selectedCategoryId
                    Column(horizontalAlignment = Alignment.CenterHorizontally) {
                        Box(
                            Modifier
                                .size(52.dp)
                                .clip(RoundedCornerShape(14.dp))
                                .background(
                                    if (selected) {
                                        Color(category.colorArgb)
                                    } else {
                                        Slate100
                                    },
                                )
                                .then(
                                    if (state.isSystemEvent) {
                                        Modifier
                                    } else {
                                        Modifier.clickable { onCategorySelect(category.id) }
                                    },
                                ),
                            contentAlignment = Alignment.Center,
                        ) {
                            Icon(
                                iconFor(category.iconKey),
                                contentDescription = category.name,
                                tint = if (selected) Color.White else Slate700,
                            )
                        }
                        Spacer(Modifier.height(2.dp))
                        Text(
                            category.name,
                            style = MaterialTheme.typography.labelSmall,
                            fontWeight = if (selected) FontWeight.Bold else FontWeight.Normal,
                            color = if (selected) Slate900 else Slate700,
                            maxLines = 1,
                            overflow = TextOverflow.Ellipsis,
                            textAlign = TextAlign.Center,
                        )
                    }
                }
                // 末位"编辑"入口 → 类别弹窗
                item {
                    Column(horizontalAlignment = Alignment.CenterHorizontally) {
                        Box(
                            Modifier
                                .size(52.dp)
                                .clip(RoundedCornerShape(14.dp))
                                .background(Slate100)
                                .clickable(onClick = onManageCategories),
                            contentAlignment = Alignment.Center,
                        ) { Icon(Icons.Filled.Add, contentDescription = null, tint = Slate700) }
                        Spacer(Modifier.height(2.dp))
                        Text(
                            stringResource(R.string.manage_categories),
                            style = MaterialTheme.typography.labelSmall,
                            color = Slate500,
                        )
                    }
                }
            }

            Spacer(Modifier.height(16.dp))
            OutlinedTextField(
                value = state.title,
                onValueChange = onTitleChange,
                label = { Text(stringResource(R.string.event_title_hint)) },
                singleLine = true,
                shape = RoundedCornerShape(12.dp),
                enabled = !state.isSystemEvent,
                modifier = Modifier.fillMaxWidth(),
            )

            if (state.isSystemEvent) {
                Spacer(Modifier.height(6.dp))
                Text(
                    stringResource(R.string.system_event_time_only),
                    style = MaterialTheme.typography.bodySmall,
                    color = Slate500,
                )
            }

            if (!state.isSystemEvent) {
                Spacer(Modifier.height(16.dp))
                HorizontalDivider(color = Slate200)
                Spacer(Modifier.height(16.dp))

                // ---- 餐次二级选项（仅餐食类别且内置船显示）----
                if (state.showMealPeriodRow) {
                    Spacer(Modifier.height(8.dp))
                    Text(stringResource(R.string.meal_period_label), style = MaterialTheme.typography.titleMedium, fontWeight = FontWeight.SemiBold, color = Slate900)
                    Spacer(Modifier.height(6.dp))
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

            Spacer(Modifier.height(16.dp))
            HorizontalDivider(color = Slate200)
            Spacer(Modifier.height(16.dp))

            // ---- 开始/结束时间 ----
            Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(12.dp)) {
                Column(Modifier.weight(1f)) {
                    Text(stringResource(R.string.start_time), style = MaterialTheme.typography.titleMedium, fontWeight = FontWeight.SemiBold, color = Slate900)
                    Spacer(Modifier.height(6.dp))
                    TimeWheel(state.startTime, onStartChange, Modifier.fillMaxWidth())
                }
                Column(Modifier.weight(1f)) {
                    Text(stringResource(R.string.end_time), style = MaterialTheme.typography.titleMedium, fontWeight = FontWeight.SemiBold, color = Slate900)
                    Spacer(Modifier.height(6.dp))
                    TimeWheel(state.endTime, onEndChange, Modifier.fillMaxWidth())
                }
            }

            // ---- 每日循环开关 ----
            if (!state.isEditing) {
                Spacer(Modifier.height(16.dp))
                HorizontalDivider(color = Slate200)
                Spacer(Modifier.height(12.dp))
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Column(Modifier.weight(1f)) {
                        Text(
                            stringResource(R.string.recur_daily),
                            style = MaterialTheme.typography.titleMedium,
                            fontWeight = FontWeight.SemiBold,
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

            state.errorResId?.let { resId ->
                Spacer(Modifier.height(12.dp))
                Text(
                    stringResource(resId),
                    color = MaterialTheme.colorScheme.error,
                    style = MaterialTheme.typography.bodySmall,
                )
            }

            Spacer(Modifier.height(20.dp))
            Button(
                onClick = onSave,
                enabled = !state.saving,
                shape = RoundedCornerShape(16.dp),
                colors = ButtonDefaults.buttonColors(containerColor = CruiseAzure),
                modifier = Modifier.fillMaxWidth().height(52.dp),
            ) { Text(stringResource(R.string.confirm), fontWeight = FontWeight.Bold, fontSize = 16.sp) }
        }

        // ---- 营业时间冲突提示（仿 pendingScopeChoice 弹窗模式）----
        if (state.hoursCheckPending) {
            AlertDialog(
                onDismissRequest = onHoursCheckDismiss,
                title = { Text(stringResource(R.string.hours_check_title)) },
                text = { Text(state.hoursCheckMessage.orEmpty()) },
                confirmButton = {
                    TextButton(onClick = onHoursCheckOverride) {
                        Text(stringResource(R.string.hours_check_confirm))
                    }
                },
                dismissButton = {
                    TextButton(onClick = onHoursCheckDismiss) {
                        Text(stringResource(R.string.hours_check_back))
                    }
                },
            )
        }
    }
}

@file:OptIn(androidx.compose.material3.ExperimentalMaterial3Api::class)

package com.cruise.assistant.ui.onboarding

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.Button
import androidx.compose.material3.DatePicker
import androidx.compose.material3.DatePickerDialog
import androidx.compose.material3.DropdownMenuItem
import androidx.compose.material3.ExposedDropdownMenuAnchorType
import androidx.compose.material3.ExposedDropdownMenuBox
import androidx.compose.material3.ExposedDropdownMenuDefaults
import androidx.compose.material3.FilterChip
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.SelectableDates
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
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.unit.dp
import com.cruise.assistant.R
import com.cruise.assistant.domain.BUILT_IN_SHIPS
import com.cruise.assistant.domain.boardingSlots
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
    onSubmit: () -> Unit,
) {
    var pickerTarget by remember { mutableStateOf<PickerTarget?>(null) }

    Column(
        Modifier
            .fillMaxSize()
            .verticalScroll(rememberScrollState())
            .padding(24.dp),
    ) {
        Text(stringResource(R.string.onboarding_title), style = MaterialTheme.typography.titleLarge)
        Spacer(Modifier.height(4.dp))
        Text(
            stringResource(R.string.onboarding_subtitle),
            style = MaterialTheme.typography.bodySmall,
            color = MaterialTheme.colorScheme.onSurfaceVariant,
        )
        Spacer(Modifier.height(24.dp))

        // ---- 船名下拉 ----
        Text(stringResource(R.string.ship_name_label), style = MaterialTheme.typography.titleMedium)
        Spacer(Modifier.height(8.dp))
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
                modifier = Modifier.menuAnchor(ExposedDropdownMenuAnchorType.PrimaryNotEditable, enabled = true).fillMaxWidth(),
            )
            ExposedDropdownMenu(
                expanded = shipMenuExpanded,
                onDismissRequest = { shipMenuExpanded = false },
            ) {
                BUILT_IN_SHIPS.forEachIndexed { index, ship ->
                    DropdownMenuItem(
                        text = { Text(ship.name) },
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
        // 自定义船名时显示输入框
        if (!state.isBuiltInShip) {
            Spacer(Modifier.height(8.dp))
            OutlinedTextField(
                value = state.shipName,
                onValueChange = onShipNameChange,
                label = { Text(stringResource(R.string.ship_name_label)) },
                singleLine = true,
                modifier = Modifier.fillMaxWidth(),
            )
        }
        Spacer(Modifier.height(20.dp))

        // ---- 船期下拉（仅内置船显示）----
        if (state.isBuiltInShip) {
            Text(stringResource(R.string.schedule_label), style = MaterialTheme.typography.titleMedium)
            Spacer(Modifier.height(8.dp))
            var scheduleMenuExpanded by remember { mutableStateOf(false) }
            val ship = BUILT_IN_SHIPS[state.selectedShipIndex]
            ExposedDropdownMenuBox(
                expanded = scheduleMenuExpanded,
                onExpandedChange = { scheduleMenuExpanded = it },
                modifier = Modifier.fillMaxWidth(),
            ) {
                OutlinedTextField(
                    value = if (state.selectedScheduleIndex >= 0)
                        ship.schedules[state.selectedScheduleIndex].label
                    else stringResource(R.string.custom_schedule),
                    onValueChange = { },
                    label = { Text(stringResource(R.string.schedule_label)) },
                    singleLine = true,
                    readOnly = true,
                    trailingIcon = { ExposedDropdownMenuDefaults.TrailingIcon(expanded = scheduleMenuExpanded) },
                    modifier = Modifier.menuAnchor(ExposedDropdownMenuAnchorType.PrimaryNotEditable, enabled = true).fillMaxWidth(),
                )
                ExposedDropdownMenu(
                    expanded = scheduleMenuExpanded,
                    onDismissRequest = { scheduleMenuExpanded = false },
                ) {
                    ship.schedules.forEachIndexed { index, schedule ->
                        DropdownMenuItem(
                            text = { Text(schedule.label) },
                            onClick = {
                                onScheduleSelected(index)
                                scheduleMenuExpanded = false
                            },
                        )
                    }
                    DropdownMenuItem(
                        text = { Text(stringResource(R.string.custom_schedule)) },
                        onClick = {
                            onScheduleSelected(-1)
                            scheduleMenuExpanded = false
                        },
                    )
                }
            }
            Spacer(Modifier.height(20.dp))
        }

        // ---- 晚餐时段（仅内置船 + 选中了预置船期时显示）----
        if (state.isBuiltInShip && state.selectedScheduleIndex >= 0) {
            Text(stringResource(R.string.dinner_time_label), style = MaterialTheme.typography.titleMedium)
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
                    )
                }
            }
            Spacer(Modifier.height(20.dp))
        }

        Text(stringResource(R.string.board_date_label), style = MaterialTheme.typography.titleMedium)
        Spacer(Modifier.height(8.dp))
        OutlinedButton(
            onClick = { pickerTarget = PickerTarget.BOARD },
            modifier = Modifier.fillMaxWidth(),
        ) { Text(state.boardDate.format(DATE_FMT)) }

        Spacer(Modifier.height(20.dp))
        Text(stringResource(R.string.board_time_label), style = MaterialTheme.typography.titleMedium)
        Spacer(Modifier.height(8.dp))
        var boardMenuExpanded by remember { mutableStateOf(false) }
        ExposedDropdownMenuBox(
            expanded = boardMenuExpanded,
            onExpandedChange = { boardMenuExpanded = it },
            modifier = Modifier.fillMaxWidth(),
        ) {
            OutlinedTextField(
                value = "%02d:%02d-%02d:%02d".format(
                    state.boardTime.hour, state.boardTime.minute,
                    (state.boardTime.plusMinutes(15).hour + 24) % 24,
                    state.boardTime.plusMinutes(15).minute,
                ),
                onValueChange = { },
                label = { Text(stringResource(R.string.board_time_label)) },
                readOnly = true,
                singleLine = true,
                trailingIcon = { ExposedDropdownMenuDefaults.TrailingIcon(expanded = boardMenuExpanded) },
                modifier = Modifier.menuAnchor(ExposedDropdownMenuAnchorType.PrimaryNotEditable, enabled = true).fillMaxWidth(),
            )
            ExposedDropdownMenu(
                expanded = boardMenuExpanded,
                onDismissRequest = { boardMenuExpanded = false },
                modifier = Modifier.heightIn(max = 320.dp),
            ) {
                boardingSlots().forEach { slot ->
                    DropdownMenuItem(
                        text = {
                            Text(
                                "%02d:%02d-%02d:%02d".format(
                                    slot.hour, slot.minute,
                                    (slot.plusMinutes(15).hour + 24) % 24,
                                    slot.plusMinutes(15).minute,
                                ),
                            )
                        },
                        onClick = {
                            onBoardTimeChange(slot)
                            boardMenuExpanded = false
                        },
                    )
                }
            }
        }

        Spacer(Modifier.height(20.dp))
        Text(stringResource(R.string.disembark_date_label), style = MaterialTheme.typography.titleMedium)
        Spacer(Modifier.height(8.dp))
        OutlinedButton(
            onClick = { pickerTarget = PickerTarget.DISEMBARK },
            modifier = Modifier.fillMaxWidth(),
        ) { Text(state.disembarkDate.format(DATE_FMT)) }
        Spacer(Modifier.height(4.dp))
        Text(
            stringResource(R.string.disembark_time_hint),
            style = MaterialTheme.typography.bodySmall,
            color = MaterialTheme.colorScheme.onSurfaceVariant,
        )

        Spacer(Modifier.height(20.dp))
        Row(verticalAlignment = Alignment.CenterVertically) {
            Text(
                stringResource(R.string.max_deck_label),
                style = MaterialTheme.typography.titleMedium,
                modifier = Modifier.weight(1f),
            )
            // 内置船带 maxDeck 时锁定（隐藏 +/- 调节器，仅显示范围），
            // 自定义船或无 maxDeck 的内置船保留调节器。
            val lockedDeck = state.isBuiltInShip &&
                BUILT_IN_SHIPS.getOrNull(state.selectedShipIndex)?.maxDeck != null
            if (lockedDeck) {
                Text(
                    "Deck ${state.minDeck}-${state.maxDeck}",
                    style = MaterialTheme.typography.titleMedium,
                    modifier = Modifier.padding(horizontal = 16.dp),
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                )
            } else {
                OutlinedButton(
                    onClick = { onMaxDeckChange(state.maxDeck - 1) },
                    enabled = state.maxDeck > 1,
                ) { Text("−") }
                Text(
                    "Deck ${state.maxDeck}",
                    style = MaterialTheme.typography.titleMedium,
                    modifier = Modifier.padding(horizontal = 16.dp),
                )
                OutlinedButton(
                    onClick = { onMaxDeckChange(state.maxDeck + 1) },
                    enabled = state.maxDeck < 30,
                ) { Text("+") }
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

        Spacer(Modifier.height(32.dp))
        Button(
            onClick = onSubmit,
            enabled = !state.saving,
            modifier = Modifier.fillMaxWidth().height(52.dp),
        ) { Text(stringResource(R.string.generate_timeline)) }
        Spacer(Modifier.height(24.dp))
    }

    pickerTarget?.let { target ->
        val initial = when (target) {
            PickerTarget.BOARD -> state.boardDate
            PickerTarget.DISEMBARK -> state.disembarkDate
        }
        // 选船期后，登船日只能选船期的登船日（周一/周四），其余日期置灰不可选。
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
                        .atZone(ZoneId.systemDefault()).toLocalDate()
                    return date.dayOfWeek == dow
                }
            }
        }
        val pickerState = rememberDatePickerState(
            initialSelectedDateMillis = initial.atStartOfDay(ZoneId.systemDefault())
                .toInstant().toEpochMilli(),
            selectableDates = selectableDates,
        )
        DatePickerDialog(
            onDismissRequest = { pickerTarget = null },
            confirmButton = {
                TextButton(onClick = {
                    pickerState.selectedDateMillis?.let { millis ->
                        val picked = Instant.ofEpochMilli(millis)
                            .atZone(ZoneId.systemDefault()).toLocalDate()
                        when (target) {
                            PickerTarget.BOARD -> onBoardDateChange(picked)
                            PickerTarget.DISEMBARK -> onDisembarkDateChange(picked)
                        }
                    }
                    pickerTarget = null
                }) { Text(stringResource(R.string.confirm)) }
            },
            dismissButton = {
                TextButton(onClick = { pickerTarget = null }) {
                    Text(stringResource(R.string.cancel))
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
                    color = MaterialTheme.colorScheme.primary,
                    modifier = Modifier.padding(horizontal = 24.dp, vertical = 4.dp),
                )
            }
            DatePicker(state = pickerState)
        }
    }
}

private enum class PickerTarget { BOARD, DISEMBARK }

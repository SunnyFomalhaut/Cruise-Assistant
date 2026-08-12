package com.cruise.assistant.ui.settings

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.FlowRow
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.ExperimentalLayoutApi
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.FilterChip
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.Slider
import androidx.compose.material3.Switch
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
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
import java.time.format.DateTimeFormatter

private val DATE_FMT = DateTimeFormatter.ofPattern("yyyy年M月d日")

@OptIn(ExperimentalLayoutApi::class)
@Composable
fun SettingsScreen(
    state: SettingsUiState,
    onRangeChange: (Int, Int) -> Unit,
    onHourHeightChange: (Int) -> Unit,
    onCollapseToggle: (Boolean) -> Unit,
    onNotificationsToggle: (Boolean) -> Unit,
    onRemindBefore15Change: (Boolean) -> Unit,
    onRemindBefore5Change: (Boolean) -> Unit,
    onRemindAtStartChange: (Boolean) -> Unit,
    onDinnerTimeChange: (java.time.LocalTime) -> Unit,
    onResetVoyage: () -> Unit,
    onManageCategories: () -> Unit,
    exactAlarmAvailable: Boolean,
) {
    var confirmReset by remember { mutableStateOf(false) }

    Column(
        Modifier
            .fillMaxSize()
            .verticalScroll(rememberScrollState())
            .padding(20.dp),
    ) {
        Text(stringResource(R.string.settings), style = MaterialTheme.typography.titleLarge)
        Spacer(Modifier.height(20.dp))

        // ---- 显示时间范围 ----
        Text(stringResource(R.string.display_range_title), style = MaterialTheme.typography.titleMedium)
        Spacer(Modifier.height(8.dp))
        Text(
            "%02d:%02d – %02d:%02d".format(
                state.settings.displayStartMinute / 60,
                state.settings.displayStartMinute % 60,
                state.settings.displayEndMinute / 60,
                state.settings.displayEndMinute % 60,
            ),
            style = MaterialTheme.typography.bodyMedium,
        )
        Spacer(Modifier.height(8.dp))
        FlowRow(
            horizontalArrangement = Arrangement.spacedBy(8.dp),
            verticalArrangement = Arrangement.spacedBy(8.dp),
        ) {
            listOf(
                7 * 60 to 24 * 60,
                8 * 60 to 23 * 60,
                6 * 60 to 24 * 60,
                0 to 24 * 60,
            ).forEach { (from, to) ->
                FilterChip(
                    selected = state.settings.displayStartMinute == from &&
                        state.settings.displayEndMinute == to,
                    onClick = { onRangeChange(from, to) },
                    label = { Text("%02d–%02d".format(from / 60, to / 60)) },
                )
            }
        }
        state.rangeErrorResId?.let { resId ->
            Spacer(Modifier.height(6.dp))
            Text(
                stringResource(resId),
                color = MaterialTheme.colorScheme.error,
                style = MaterialTheme.typography.bodySmall,
            )
        }

        Spacer(Modifier.height(16.dp))
        Row(verticalAlignment = Alignment.CenterVertically) {
            Text(
                stringResource(R.string.hour_height_label),
                style = MaterialTheme.typography.bodyMedium,
                modifier = Modifier.weight(1f),
            )
            Text("${state.settings.hourHeightDp}dp", style = MaterialTheme.typography.bodyMedium)
        }
        Slider(
            value = state.settings.hourHeightDp.toFloat(),
            onValueChange = { onHourHeightChange(it.toInt()) },
            valueRange = 60f..180f,
            steps = 19,
        )

        Row(verticalAlignment = Alignment.CenterVertically) {
            Text(
                stringResource(R.string.collapse_empty_gaps),
                style = MaterialTheme.typography.bodyMedium,
                modifier = Modifier.weight(1f),
            )
            Switch(checked = state.settings.collapseEmptyGaps, onCheckedChange = onCollapseToggle)
        }

        Spacer(Modifier.height(20.dp))
        HorizontalDivider()
        Spacer(Modifier.height(20.dp))

        // ---- 提醒 ----
        Text(stringResource(R.string.reminder_section), style = MaterialTheme.typography.titleMedium)
        Spacer(Modifier.height(8.dp))
        Row(verticalAlignment = Alignment.CenterVertically) {
            Text(
                stringResource(R.string.reminder_enabled),
                style = MaterialTheme.typography.bodyMedium,
                modifier = Modifier.weight(1f),
            )
            Switch(
                checked = state.settings.notificationsEnabled,
                onCheckedChange = onNotificationsToggle,
            )
        }
        if (state.settings.notificationsEnabled) {
            Spacer(Modifier.height(8.dp))
            Text(stringResource(R.string.reminder_lead_time), style = MaterialTheme.typography.bodyMedium)
            Spacer(Modifier.height(4.dp))
            Row(verticalAlignment = Alignment.CenterVertically) {
                Text(
                    stringResource(R.string.reminder_before_15),
                    style = MaterialTheme.typography.bodySmall,
                    modifier = Modifier.weight(1f),
                )
                Switch(
                    checked = state.settings.remindBefore15,
                    onCheckedChange = onRemindBefore15Change,
                )
            }
            Row(verticalAlignment = Alignment.CenterVertically) {
                Text(
                    stringResource(R.string.reminder_before_5),
                    style = MaterialTheme.typography.bodySmall,
                    modifier = Modifier.weight(1f),
                )
                Switch(
                    checked = state.settings.remindBefore5,
                    onCheckedChange = onRemindBefore5Change,
                )
            }
            Row(verticalAlignment = Alignment.CenterVertically) {
                Text(
                    stringResource(R.string.reminder_at_start),
                    style = MaterialTheme.typography.bodySmall,
                    modifier = Modifier.weight(1f),
                )
                Switch(
                    checked = state.settings.remindAtStart,
                    onCheckedChange = onRemindAtStartChange,
                )
            }
            if (!exactAlarmAvailable) {
                Spacer(Modifier.height(8.dp))
                Text(
                    stringResource(R.string.exact_alarm_unavailable),
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.error,
                )
            }
        }

        Spacer(Modifier.height(20.dp))
        HorizontalDivider()
        Spacer(Modifier.height(20.dp))

        // ---- 晚餐时段 ----
        Text(stringResource(R.string.dinner_time_section), style = MaterialTheme.typography.titleMedium)
        Spacer(Modifier.height(8.dp))
        FlowRow(
            horizontalArrangement = Arrangement.spacedBy(8.dp),
            verticalArrangement = Arrangement.spacedBy(8.dp),
        ) {
            listOf(
                java.time.LocalTime.of(17, 45) to R.string.dinner_time_1745,
                java.time.LocalTime.of(20, 0) to R.string.dinner_time_2000,
            ).forEach { (time, labelRes) ->
                FilterChip(
                    selected = state.settings.dinnerTime == time,
                    onClick = { onDinnerTimeChange(time) },
                    label = { Text(stringResource(labelRes)) },
                )
            }
        }

        Spacer(Modifier.height(20.dp))
        HorizontalDivider()
        Spacer(Modifier.height(20.dp))

        // ---- 航次 ----
        Text(stringResource(R.string.voyage_section), style = MaterialTheme.typography.titleMedium)
        Spacer(Modifier.height(8.dp))
        state.voyage?.let { voyage ->
            Text(
                "${voyage.boardDate.format(DATE_FMT)} ${voyage.boardTime} → " +
                    "${voyage.disembarkDate.format(DATE_FMT)} ${voyage.disembarkTime}",
                style = MaterialTheme.typography.bodyMedium,
            )
            Text(
                stringResource(R.string.voyage_summary_format, voyage.dayCount, voyage.maxDeck),
                style = MaterialTheme.typography.bodySmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
            )
        }
        Spacer(Modifier.height(12.dp))
        OutlinedButton(onClick = { confirmReset = true }, modifier = Modifier.fillMaxWidth()) {
            Text(stringResource(R.string.reset_voyage))
        }

        Spacer(Modifier.height(20.dp))
        HorizontalDivider()
        Spacer(Modifier.height(20.dp))

        OutlinedButton(onClick = onManageCategories, modifier = Modifier.fillMaxWidth()) {
            Text(stringResource(R.string.category_section))
        }
        Spacer(Modifier.height(40.dp))
    }

    if (confirmReset) {
        AlertDialog(
            onDismissRequest = { confirmReset = false },
            title = { Text(stringResource(R.string.reset_voyage)) },
            text = { Text(stringResource(R.string.reset_voyage_warning)) },
            confirmButton = {
                TextButton(onClick = {
                    confirmReset = false
                    onResetVoyage()
                }) { Text(stringResource(R.string.confirm)) }
            },
            dismissButton = {
                TextButton(onClick = { confirmReset = false }) {
                    Text(stringResource(R.string.cancel))
                }
            },
        )
    }
}

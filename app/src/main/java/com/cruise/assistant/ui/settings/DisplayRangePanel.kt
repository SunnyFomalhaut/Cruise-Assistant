@file:OptIn(androidx.compose.material3.ExperimentalMaterial3Api::class)

package com.cruise.assistant.ui.settings

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.ModalBottomSheet
import androidx.compose.material3.Slider
import androidx.compose.material3.Switch
import androidx.compose.material3.Text
import androidx.compose.material3.rememberModalBottomSheetState
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.unit.dp
import com.cruise.assistant.R
import com.cruise.assistant.ui.common.TimeWheel
import java.time.LocalTime

/**
 * 需求2：全局显示时间范围 + 每小时高度 + 自动隐藏空时段。
 * 止时间用 24:00 表示一天结束 —— TimeWheel 只能到 23:xx，所以 23:30 及之后视作 24:00。
 */
@Composable
fun DisplayRangePanel(
    startMinute: Int,
    endMinute: Int,
    hourHeightDp: Int,
    collapseEmptyGaps: Boolean,
    errorResId: Int?,
    onRangeChange: (Int, Int) -> Unit,
    onHourHeightChange: (Int) -> Unit,
    onCollapseToggle: (Boolean) -> Unit,
    onDismiss: () -> Unit,
) {
    val sheetState = rememberModalBottomSheetState(skipPartiallyExpanded = true)

    ModalBottomSheet(onDismissRequest = onDismiss, sheetState = sheetState) {
        Column(Modifier.padding(horizontal = 20.dp).padding(bottom = 24.dp)) {
            Text(
                stringResource(R.string.display_range_title),
                style = MaterialTheme.typography.titleLarge,
            )
            Spacer(Modifier.height(16.dp))

            Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(12.dp)) {
                Column(Modifier.weight(1f)) {
                    Text(stringResource(R.string.range_from), style = MaterialTheme.typography.titleMedium)
                    Spacer(Modifier.height(6.dp))
                    TimeWheel(
                        value = minuteToTime(startMinute),
                        onValueChange = { onRangeChange(it.hour * 60 + it.minute, endMinute) },
                        modifier = Modifier.fillMaxWidth(),
                        minuteStep = 30,
                    )
                }
                Column(Modifier.weight(1f)) {
                    Text(stringResource(R.string.range_to), style = MaterialTheme.typography.titleMedium)
                    Spacer(Modifier.height(6.dp))
                    TimeWheel(
                        value = minuteToTime(endMinute),
                        onValueChange = {
                            val minutes = it.hour * 60 + it.minute
                            // 23:30 之后视作 24:00，方便选"到一天结束"
                            onRangeChange(startMinute, if (minutes >= 23 * 60 + 30) 1440 else minutes)
                        },
                        modifier = Modifier.fillMaxWidth(),
                        minuteStep = 30,
                    )
                }
            }

            errorResId?.let { resId ->
                Spacer(Modifier.height(8.dp))
                Text(
                    stringResource(resId),
                    color = MaterialTheme.colorScheme.error,
                    style = MaterialTheme.typography.bodySmall,
                )
            }

            Spacer(Modifier.height(20.dp))
            Row(verticalAlignment = Alignment.CenterVertically) {
                Text(
                    stringResource(R.string.hour_height_label),
                    style = MaterialTheme.typography.titleMedium,
                    modifier = Modifier.weight(1f),
                )
                Text("${hourHeightDp}dp", style = MaterialTheme.typography.bodyMedium)
            }
            Slider(
                value = hourHeightDp.toFloat(),
                onValueChange = { onHourHeightChange(it.toInt()) },
                valueRange = 36f..140f,
                steps = 12,
            )

            Spacer(Modifier.height(8.dp))
            Row(verticalAlignment = Alignment.CenterVertically) {
                Text(
                    stringResource(R.string.collapse_empty_gaps),
                    style = MaterialTheme.typography.bodyMedium,
                    modifier = Modifier.weight(1f),
                )
                Switch(checked = collapseEmptyGaps, onCheckedChange = onCollapseToggle)
            }
        }
    }
}

private fun minuteToTime(minute: Int): LocalTime {
    val clamped = minute.coerceIn(0, 1439)
    return LocalTime.of(clamped / 60, clamped % 60)
}

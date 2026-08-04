@file:OptIn(androidx.compose.material3.ExperimentalMaterial3Api::class)

package com.cruise.assistant.ui.onboarding

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.Button
import androidx.compose.material3.DatePicker
import androidx.compose.material3.DatePickerDialog
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.OutlinedTextField
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
import com.cruise.assistant.ui.common.TimeWheel
import java.time.Instant
import java.time.LocalDate
import java.time.ZoneId
import java.time.format.DateTimeFormatter

private val DATE_FMT = DateTimeFormatter.ofPattern("yyyy年M月d日")

@Composable
fun OnboardingScreen(
    state: OnboardingState,
    onShipNameChange: (String) -> Unit,
    onBoardDateChange: (LocalDate) -> Unit,
    onBoardTimeChange: (java.time.LocalTime) -> Unit,
    onDisembarkDateChange: (LocalDate) -> Unit,
    onMaxDeckChange: (Int) -> Unit,
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

        OutlinedTextField(
            value = state.shipName,
            onValueChange = onShipNameChange,
            label = { Text(stringResource(R.string.ship_name_label)) },
            singleLine = true,
            modifier = Modifier.fillMaxWidth(),
        )
        Spacer(Modifier.height(20.dp))

        Text(stringResource(R.string.board_date_label), style = MaterialTheme.typography.titleMedium)
        Spacer(Modifier.height(8.dp))
        OutlinedButton(
            onClick = { pickerTarget = PickerTarget.BOARD },
            modifier = Modifier.fillMaxWidth(),
        ) { Text(state.boardDate.format(DATE_FMT)) }

        Spacer(Modifier.height(20.dp))
        Text(stringResource(R.string.board_time_label), style = MaterialTheme.typography.titleMedium)
        Spacer(Modifier.height(8.dp))
        TimeWheel(
            value = state.boardTime,
            onValueChange = onBoardTimeChange,
            modifier = Modifier.fillMaxWidth(),
        )

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
            OutlinedButton(
                onClick = { onMaxDeckChange(state.maxDeck - 1) },
                enabled = state.maxDeck > 1,
            ) { Text("−") }
            Text(
                "${state.maxDeck}F",
                style = MaterialTheme.typography.titleMedium,
                modifier = Modifier.padding(horizontal = 16.dp),
            )
            OutlinedButton(
                onClick = { onMaxDeckChange(state.maxDeck + 1) },
                enabled = state.maxDeck < 30,
            ) { Text("+") }
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
        val pickerState = rememberDatePickerState(
            initialSelectedDateMillis = initial.atStartOfDay(ZoneId.systemDefault())
                .toInstant().toEpochMilli(),
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
        ) { DatePicker(state = pickerState) }
    }
}

private enum class PickerTarget { BOARD, DISEMBARK }

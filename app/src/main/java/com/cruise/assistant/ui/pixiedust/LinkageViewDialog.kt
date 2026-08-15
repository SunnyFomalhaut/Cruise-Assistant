package com.cruise.assistant.ui.pixiedust

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.cruise.assistant.R
import com.cruise.assistant.domain.RoomPair
import com.cruise.assistant.domain.levelCountsOf

/** 联动检视：按房间配对显示发出↔收到，只读 */
@Composable
fun LinkageViewDialog(state: PixieDustUiState, onDismiss: () -> Unit) {
    AlertDialog(
        onDismissRequest = onDismiss,
        title = { Text(stringResource(R.string.pixie_linkage_view)) },
        text = {
            if (state.roomPairs.isEmpty()) {
                Box(Modifier.fillMaxWidth().padding(vertical = 24.dp), contentAlignment = Alignment.Center) {
                    Text(
                        stringResource(R.string.pixie_linkage_empty),
                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                    )
                }
            } else {
                LazyColumn(Modifier.fillMaxWidth()) {
                    items(state.roomPairs, key = { "${it.deck}-${it.room}" }) { pair ->
                        RoomPairRow(state, pair)
                        HorizontalDivider()
                    }
                }
            }
        },
        confirmButton = {
            TextButton(onClick = onDismiss) { Text(stringResource(R.string.pixie_back)) }
        },
    )
}

@Composable
private fun RoomPairRow(state: PixieDustUiState, pair: RoomPair) {
    Column(Modifier.fillMaxWidth().padding(horizontal = 4.dp, vertical = 8.dp)) {
        Row(verticalAlignment = Alignment.CenterVertically) {
            Text(
                "${pair.deck}${pair.room}",
                style = MaterialTheme.typography.titleMedium,
                fontWeight = FontWeight.Bold,
            )
            Spacer(Modifier.width(8.dp))
            if (pair.unrepliedReceived.isNotEmpty()) {
                Text(
                    stringResource(R.string.pixie_linkage_unreplied_format, pair.unrepliedReceived.size),
                    style = MaterialTheme.typography.labelMedium,
                    color = MaterialTheme.colorScheme.error,
                )
            }
        }
        Spacer(Modifier.padding(2.dp))
        Row {
            Column(Modifier.weight(1f)) {
                Text(
                    stringResource(R.string.pixie_linkage_given_label, pair.given.size),
                    style = MaterialTheme.typography.labelMedium,
                )
                Text(
                    pair.levelCountsText(state, pair.given),
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                )
            }
            Text("↔", fontSize = 18.sp, color = MaterialTheme.colorScheme.onSurfaceVariant)
            Column(Modifier.weight(1f)) {
                Text(
                    stringResource(R.string.pixie_linkage_received_label, pair.received.size),
                    style = MaterialTheme.typography.labelMedium,
                )
                Text(
                    pair.levelCountsText(state, pair.received),
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                )
            }
        }
    }
}

private fun RoomPair.levelCountsText(state: PixieDustUiState, records: List<com.cruise.assistant.domain.PixieDustRecord>): String =
    levelCountsOf(records, state.levels).joinToString("  ") { "${it.level.name}×${it.count}" }
        .ifEmpty { "—" }

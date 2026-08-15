package com.cruise.assistant.ui.pixiedust

import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Checkbox
import androidx.compose.material3.DropdownMenu
import androidx.compose.material3.DropdownMenuItem
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.OutlinedTextField
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
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.unit.dp
import com.cruise.assistant.R
import com.cruise.assistant.domain.PixieDustRecord
import com.cruise.assistant.domain.PixieLevel
import com.cruise.assistant.domain.isValidRoomInput

/** 编辑一条记录：楼层/房号/等级/备注（+收到的已回礼），底部删除。时间戳不可改。 */
@Composable
fun PixieDustEditDialog(
    initial: PixieDustRecord,
    isReceived: Boolean,
    deckRange: IntRange,
    levels: List<PixieLevel>,
    onRequestReply: () -> Unit,
    onSave: (PixieDustRecord) -> Unit,
    onDelete: (PixieDustRecord) -> Unit,
    onDismiss: () -> Unit,
) {
    var deck by remember { mutableStateOf(initial.deck) }
    var room by remember { mutableStateOf(initial.room) }
    var levelId by remember { mutableStateOf(initial.levelId) }
    var replied by remember { mutableStateOf(initial.replied) }
    var note by remember { mutableStateOf(initial.note) }
    var confirmDelete by remember { mutableStateOf(false) }

    fun build(): PixieDustRecord = initial.copy(
        deck = deck,
        room = room,
        levelId = levelId,
        levelNameSnapshot = levels.firstOrNull { it.id == levelId }?.name ?: initial.levelNameSnapshot,
        replied = replied,
        note = note,
    )

    if (confirmDelete) {
        AlertDialog(
            onDismissRequest = { confirmDelete = false },
            title = { Text(stringResource(R.string.pixie_delete)) },
            text = { Text(stringResource(R.string.pixie_delete_confirm)) },
            confirmButton = {
                TextButton(onClick = { onDelete(initial) }) {
                    Text(stringResource(R.string.pixie_delete), color = MaterialTheme.colorScheme.error)
                }
            },
            dismissButton = {
                TextButton(onClick = { confirmDelete = false }) {
                    Text(stringResource(R.string.pixie_cancel))
                }
            },
        )
        return
    }

    AlertDialog(
        onDismissRequest = onDismiss,
        title = { Text(stringResource(R.string.pixie_edit_title)) },
        text = {
            Column {
                // 楼层
                var deckExpanded by remember { mutableStateOf(false) }
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Text(stringResource(R.string.pixie_deck_label))
                    Spacer(Modifier.width(8.dp))
                    OutlinedButton(onClick = { deckExpanded = true }) {
                        Text(stringResource(R.string.pixie_deck_format, deck))
                    }
                    DropdownMenu(expanded = deckExpanded, onDismissRequest = { deckExpanded = false }) {
                        deckRange.forEach { d ->
                            DropdownMenuItem(
                                text = { Text(stringResource(R.string.pixie_deck_format, d)) },
                                onClick = { deck = d; deckExpanded = false },
                            )
                        }
                    }
                }
                Spacer(Modifier.height(8.dp))
                // 房号（仅数字，最多三位）
                OutlinedTextField(
                    value = room,
                    onValueChange = { room = it.filter { c -> c.isDigit() }.take(3) },
                    label = { Text(stringResource(R.string.pixie_room_hint)) },
                    keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number),
                    singleLine = true,
                    isError = !isValidRoomInput(room),
                )
                Spacer(Modifier.height(8.dp))
                // 等级
                Row(verticalAlignment = Alignment.CenterVertically) {
                    levels.forEach { level ->
                        LevelChip(
                            level = level,
                            selected = levelId == level.id,
                            onClick = { levelId = level.id },
                        )
                        Spacer(Modifier.width(4.dp))
                    }
                }
                if (isReceived) {
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        Checkbox(
                            checked = replied,
                            onCheckedChange = { checked ->
                                if (checked) onRequestReply()  // 上报请求，弹窗流程由外层接管
                                else replied = false
                            },
                        )
                        Text(stringResource(R.string.pixie_replied))
                    }
                }
                Spacer(Modifier.height(8.dp))
                OutlinedTextField(
                    value = note,
                    onValueChange = { note = it },
                    label = { Text(stringResource(R.string.pixie_note_label)) },
                    singleLine = true,
                    modifier = Modifier.fillMaxWidth(),
                )
            }
        },
        confirmButton = {
            TextButton(
                onClick = { onSave(build()) },
                enabled = isValidRoomInput(room) && levels.any { it.id == levelId },
            ) { Text(stringResource(R.string.pixie_level_save)) }
        },
        dismissButton = {
            Row {
                TextButton(onClick = { confirmDelete = true }) {
                    Text(stringResource(R.string.pixie_delete), color = MaterialTheme.colorScheme.error)
                }
                TextButton(onClick = onDismiss) {
                    Text(stringResource(R.string.pixie_cancel))
                }
            }
        },
    )
}

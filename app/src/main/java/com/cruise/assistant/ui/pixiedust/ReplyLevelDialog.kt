package com.cruise.assistant.ui.pixiedust

import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.unit.dp
import com.cruise.assistant.R
import com.cruise.assistant.domain.PixieDustRecord
import com.cruise.assistant.domain.PixieLevel

/**
 * 回礼等级弹窗：勾选"已回礼"后弹出。
 * 预选收到记录的同级等级（已删则回落第一个）；确认创建发出记录，"仅勾选"跳过创建。
 */
@Composable
fun ReplyLevelDialog(
    record: PixieDustRecord,
    levels: List<PixieLevel>,
    onConfirm: (PixieLevel?) -> Unit,
    onDismiss: () -> Unit,
) {
    // 预选同级；该等级已被删则回落第一个
    var selectedId by remember(record.id) {
        mutableStateOf(
            levels.firstOrNull { it.id == record.levelId }?.id ?: levels.firstOrNull()?.id,
        )
    }

    AlertDialog(
        onDismissRequest = onDismiss,
        title = { Text(stringResource(R.string.pixie_reply_level_title_format, "${record.deck}${record.room}")) },
        text = {
            Column {
                Row {
                    levels.forEach { level ->
                        LevelChip(
                            level = level,
                            selected = selectedId == level.id,
                            onClick = { selectedId = level.id },
                        )
                        Spacer(Modifier.padding(2.dp))
                    }
                }
            }
        },
        confirmButton = {
            TextButton(
                onClick = { onConfirm(levels.firstOrNull { it.id == selectedId }) },
                enabled = selectedId != null,
            ) { Text(stringResource(R.string.pixie_reply_confirm)) }
        },
        dismissButton = {
            Row {
                TextButton(onClick = { onConfirm(null) }) {
                    Text(stringResource(R.string.pixie_reply_only_check))
                }
                TextButton(onClick = onDismiss) {
                    Text(stringResource(R.string.pixie_cancel))
                }
            }
        },
    )
}

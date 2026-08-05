package com.cruise.assistant.ui.timeline

import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.unit.dp
import com.cruise.assistant.R
import com.cruise.assistant.domain.CruiseEvent
import com.cruise.assistant.domain.endMinute

/**
 * 需求8：定稿前列出冲突对。
 * 无冲突时直接显示"没有冲突"并把确认按钮变成生成。
 */
@Composable
fun ConflictDialog(
    conflicts: List<Pair<CruiseEvent, CruiseEvent>>,
    onGoFix: () -> Unit,
    onFinalize: () -> Unit,
    onDismiss: () -> Unit,
) {
    val hasConflicts = conflicts.isNotEmpty()

    AlertDialog(
        onDismissRequest = onDismiss,
        title = {
            Text(
                stringResource(
                    if (hasConflicts) R.string.conflict_dialog_title else R.string.conflict_dialog_none,
                ),
            )
        },
        text = if (!hasConflicts) {
            null
        } else {
            {
                Column(
                    Modifier
                        .heightIn(max = 320.dp)
                        .verticalScroll(rememberScrollState()),
                ) {
                    conflicts.forEach { (a, b) ->
                        Text(
                            stringResource(
                                R.string.conflict_item_format,
                                a.title, rangeText(a), b.title, rangeText(b),
                            ),
                            style = MaterialTheme.typography.bodySmall,
                        )
                        Spacer(Modifier.height(6.dp))
                    }
                }
            }
        },
        confirmButton = {
            TextButton(onClick = onFinalize) {
                Text(
                    stringResource(
                        if (hasConflicts) R.string.ignore_and_finalize else R.string.generate_plan,
                    ),
                )
            }
        },
        dismissButton = {
            TextButton(onClick = if (hasConflicts) onGoFix else onDismiss) {
                Text(stringResource(if (hasConflicts) R.string.go_fix else R.string.cancel))
            }
        },
    )
}

private fun rangeText(event: CruiseEvent): String {
    val end = event.endMinute
    return "%02d:%02d–%02d:%02d".format(
        event.startTime.hour, event.startTime.minute, (end / 60) % 24, end % 60,
    )
}

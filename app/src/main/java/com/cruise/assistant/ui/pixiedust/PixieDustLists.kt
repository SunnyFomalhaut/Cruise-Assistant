package com.cruise.assistant.ui.pixiedust

import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.material3.Checkbox
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import com.cruise.assistant.R
import com.cruise.assistant.domain.PixieDustRecord
import java.time.Instant
import java.time.ZoneId
import java.time.format.DateTimeFormatter

private val DATE_FMT = DateTimeFormatter.ofPattern("M月d日")

internal fun formatTs(ts: Long): String =
    Instant.ofEpochMilli(ts).atZone(ZoneId.systemDefault()).toLocalDate().format(DATE_FMT)

/** 记录显示名：等级还在用等级名，被删用快照 */
internal fun levelNameOf(state: PixieDustUiState, record: PixieDustRecord): String =
    state.levelById[record.levelId]?.name ?: record.levelNameSnapshot

internal fun levelColorOf(state: PixieDustUiState, record: PixieDustRecord): Color =
    Color(state.levelById[record.levelId]?.colorArgb ?: 0xFF9E9E9E.toInt())

/** 发出列表：按房间归组（默认，可展开）/ 时间流水，右上切换 */
@Composable
fun GivenList(
    state: PixieDustUiState,
    onToggleViewMode: () -> Unit,
    onEditRecord: (PixieDustRecord) -> Unit,
) {
    Column(Modifier.fillMaxSize()) {
        Row(
            Modifier.fillMaxWidth().padding(horizontal = 16.dp),
            verticalAlignment = Alignment.CenterVertically,
        ) {
            Text(
                stringResource(
                    if (state.viewMode == GivenViewMode.GROUPED) R.string.pixie_view_grouped
                    else R.string.pixie_view_flow
                ),
                style = MaterialTheme.typography.labelMedium,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
                modifier = Modifier.weight(1f),
            )
            TextButton(onClick = onToggleViewMode) {
                Text(
                    stringResource(
                        if (state.viewMode == GivenViewMode.GROUPED) R.string.pixie_view_flow
                        else R.string.pixie_view_grouped
                    )
                )
            }
        }
        if (state.given.isEmpty()) {
            EmptyHint(stringResource(R.string.pixie_empty_given))
        } else if (state.viewMode == GivenViewMode.GROUPED) {
            GroupedView(state, onEditRecord)
        } else {
            LazyColumn(Modifier.fillMaxSize()) {
                items(state.given, key = { it.id }) { rec ->
                    RecordRow(
                        state = state,
                        record = rec,
                        onClick = { onEditRecord(rec) },
                    )
                    HorizontalDivider()
                }
            }
        }
    }
}

@Composable
private fun GroupedView(state: PixieDustUiState, onEditRecord: (PixieDustRecord) -> Unit) {
    var expandedKey by remember { mutableStateOf<String?>(null) }
    LazyColumn(Modifier.fillMaxSize()) {
        items(state.grouped, key = { "${it.deck}-${it.room}" }) { group ->
            val key = "${group.deck}-${group.room}"
            Column(Modifier.fillMaxWidth().clickable {
                expandedKey = if (expandedKey == key) null else key
            }) {
                Row(
                    Modifier.fillMaxWidth().padding(horizontal = 16.dp, vertical = 10.dp),
                    verticalAlignment = Alignment.CenterVertically,
                ) {
                    Text(
                        "${group.deck}${group.room}",
                        style = MaterialTheme.typography.titleMedium,
                        fontWeight = FontWeight.Bold,
                    )
                    Spacer(Modifier.width(8.dp))
                    Text(
                        group.levelCounts.joinToString("  ") { "${it.level.name}×${it.count}" },
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                        modifier = Modifier.weight(1f),
                    )
                    Text(
                        formatTs(group.lastTimestamp) + " · " +
                            stringResource(R.string.pixie_group_total_format, group.total),
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                    )
                }
                if (expandedKey == key) {
                    state.given
                        .filter { it.deck == group.deck && it.room == group.room }
                        .forEach { rec ->
                            RecordRow(state = state, record = rec, onClick = { onEditRecord(rec) })
                        }
                }
                HorizontalDivider()
            }
        }
    }
}

/** 收到列表：未回礼在前贵重优先（state.received 已排好），右侧回礼勾选 */
@Composable
fun ReceivedList(
    state: PixieDustUiState,
    onEditRecord: (PixieDustRecord) -> Unit,
    onToggleReplied: (Long, Boolean) -> Unit,
) {
    if (state.received.isEmpty()) {
        EmptyHint(stringResource(R.string.pixie_empty_received))
        return
    }
    LazyColumn(Modifier.fillMaxSize()) {
        items(state.received, key = { it.id }) { rec ->
            Row(
                Modifier.fillMaxWidth().clickable { onEditRecord(rec) }.padding(horizontal = 16.dp, vertical = 4.dp),
                verticalAlignment = Alignment.CenterVertically,
            ) {
                LevelDot(levelColorOf(state, rec))
                Spacer(Modifier.width(12.dp))
                Column(Modifier.weight(1f)) {
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        Text(
                            "${rec.deck}${rec.room}",
                            style = MaterialTheme.typography.titleMedium,
                            fontWeight = FontWeight.Bold,
                        )
                        Spacer(Modifier.width(8.dp))
                        Text(
                            levelNameOf(state, rec) + " · " + formatTs(rec.timestamp),
                            style = MaterialTheme.typography.bodySmall,
                            color = MaterialTheme.colorScheme.onSurfaceVariant,
                        )
                    }
                    if (rec.note.isNotEmpty()) {
                        Text(
                            rec.note,
                            style = MaterialTheme.typography.bodySmall,
                            color = MaterialTheme.colorScheme.onSurfaceVariant,
                        )
                    }
                }
                Checkbox(
                    checked = rec.replied,
                    onCheckedChange = { onToggleReplied(rec.id, it) },
                )
            }
            HorizontalDivider()
        }
    }
}

/** 流水行：房号 + 等级 + 日期（+备注） */
@Composable
private fun RecordRow(state: PixieDustUiState, record: PixieDustRecord, onClick: () -> Unit) {
    Row(
        Modifier.fillMaxWidth().clickable(onClick = onClick).padding(horizontal = 16.dp, vertical = 4.dp),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        LevelDot(levelColorOf(state, record))
        Spacer(Modifier.width(12.dp))
        Column(Modifier.weight(1f)) {
            Row(verticalAlignment = Alignment.CenterVertically) {
                Text(
                    "${record.deck}${record.room}",
                    style = MaterialTheme.typography.titleMedium,
                    fontWeight = FontWeight.Bold,
                )
                Spacer(Modifier.width(8.dp))
                Text(
                    levelNameOf(state, record) + " · " + formatTs(record.timestamp),
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                )
            }
            if (record.note.isNotEmpty()) {
                Text(
                    record.note,
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                )
            }
        }
    }
}

@Composable
internal fun LevelDot(color: Color) {
    Surface(shape = CircleShape, color = color, modifier = Modifier.size(10.dp)) {}
}

@Composable
private fun EmptyHint(text: String) {
    Box(Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
        Text(
            text,
            style = MaterialTheme.typography.bodyMedium,
            color = MaterialTheme.colorScheme.onSurfaceVariant,
        )
    }
}

package com.cruise.assistant.ui.pixiedust

import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.outlined.Check
import androidx.compose.material.icons.outlined.Inbox
import androidx.compose.material3.Checkbox
import androidx.compose.material3.CheckboxDefaults
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
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
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.cruise.assistant.R
import com.cruise.assistant.domain.PixieDustRecord
import com.cruise.assistant.ui.theme.CruiseAzure
import com.cruise.assistant.ui.theme.CruiseAzureContainer
import com.cruise.assistant.ui.theme.CruiseMarine
import com.cruise.assistant.ui.theme.Slate100
import com.cruise.assistant.ui.theme.Slate200
import com.cruise.assistant.ui.theme.Slate500
import com.cruise.assistant.ui.theme.Slate700
import com.cruise.assistant.ui.theme.Slate900
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
            Modifier.fillMaxWidth().padding(horizontal = 16.dp, vertical = 2.dp),
            verticalAlignment = Alignment.CenterVertically,
        ) {
            Text(
                stringResource(
                    if (state.viewMode == GivenViewMode.GROUPED) R.string.pixie_view_grouped
                    else R.string.pixie_view_flow
                ),
                style = MaterialTheme.typography.labelSmall,
                color = Slate500,
                modifier = Modifier.weight(1f),
            )
            TextButton(onClick = onToggleViewMode) {
                Text(
                    stringResource(
                        if (state.viewMode == GivenViewMode.GROUPED) R.string.pixie_view_flow
                        else R.string.pixie_view_grouped
                    ),
                    color = CruiseAzure,
                    style = MaterialTheme.typography.labelMedium,
                    fontWeight = FontWeight.SemiBold,
                )
            }
        }
        if (state.given.isEmpty()) {
            EmptyHint(stringResource(R.string.pixie_empty_given))
        } else if (state.viewMode == GivenViewMode.GROUPED) {
            GroupedView(state, onEditRecord)
        } else {
            LazyColumn(
                Modifier.fillMaxSize(),
                contentPadding = PaddingValues(horizontal = 16.dp, vertical = 4.dp),
                verticalArrangement = Arrangement.spacedBy(8.dp),
            ) {
                items(state.given, key = { it.id }) { rec ->
                    RecordCard(
                        state = state,
                        record = rec,
                        onClick = { onEditRecord(rec) },
                    )
                }
            }
        }
    }
}

@Composable
private fun GroupedView(state: PixieDustUiState, onEditRecord: (PixieDustRecord) -> Unit) {
    var expandedKey by remember { mutableStateOf<String?>(null) }
    LazyColumn(
        Modifier.fillMaxSize(),
        contentPadding = PaddingValues(horizontal = 16.dp, vertical = 4.dp),
        verticalArrangement = Arrangement.spacedBy(8.dp),
    ) {
        items(state.grouped, key = { "${it.deck}-${it.room}" }) { group ->
            val key = "${group.deck}-${group.room}"
            val isExpanded = expandedKey == key
            Surface(
                shape = RoundedCornerShape(14.dp),
                color = MaterialTheme.colorScheme.surface,
                border = androidx.compose.foundation.BorderStroke(1.dp, Slate200),
                modifier = Modifier.fillMaxWidth(),
            ) {
                Column(Modifier.fillMaxWidth().clickable {
                    expandedKey = if (isExpanded) null else key
                }) {
                    Row(
                        Modifier.fillMaxWidth().padding(14.dp),
                        verticalAlignment = Alignment.CenterVertically,
                    ) {
                        Surface(
                            shape = RoundedCornerShape(8.dp),
                            color = CruiseAzureContainer,
                        ) {
                            Text(
                                "${group.deck}${group.room}",
                                style = MaterialTheme.typography.titleMedium,
                                fontWeight = FontWeight.Bold,
                                color = CruiseMarine,
                                modifier = Modifier.padding(horizontal = 8.dp, vertical = 4.dp),
                            )
                        }
                        Spacer(Modifier.width(10.dp))
                        Text(
                            group.levelCounts.joinToString("  ") { "${it.level.name}×${it.count}" },
                            style = MaterialTheme.typography.bodySmall,
                            color = Slate700,
                            modifier = Modifier.weight(1f),
                        )
                        Text(
                            formatTs(group.lastTimestamp) + " · " +
                                stringResource(R.string.pixie_group_total_format, group.total),
                            style = MaterialTheme.typography.labelSmall,
                            color = Slate500,
                        )
                    }
                    if (isExpanded) {
                        HorizontalDivider(color = Slate200)
                        state.given
                            .filter { it.deck == group.deck && it.room == group.room }
                            .forEach { rec ->
                                RecordCardRow(state = state, record = rec, onClick = { onEditRecord(rec) })
                            }
                    }
                }
            }
        }
    }
}

/** 收到列表：未回礼在前贵重优先（state.received 已排好），右侧回礼勾选 */
@Composable
fun ReceivedList(
    state: PixieDustUiState,
    onEditRecord: (PixieDustRecord) -> Unit,
    onRequestReply: (PixieDustRecord) -> Unit,
    onCancelReply: (Long) -> Unit,
) {
    if (state.received.isEmpty()) {
        EmptyHint(stringResource(R.string.pixie_empty_received))
        return
    }
    LazyColumn(
        Modifier.fillMaxSize(),
        contentPadding = PaddingValues(horizontal = 16.dp, vertical = 6.dp),
        verticalArrangement = Arrangement.spacedBy(8.dp),
    ) {
        items(state.received, key = { it.id }) { rec ->
            Surface(
                shape = RoundedCornerShape(14.dp),
                color = MaterialTheme.colorScheme.surface,
                border = androidx.compose.foundation.BorderStroke(1.dp, Slate200),
                modifier = Modifier.fillMaxWidth(),
            ) {
                Row(
                    Modifier.fillMaxWidth().clickable { onEditRecord(rec) }.padding(14.dp),
                    verticalAlignment = Alignment.CenterVertically,
                ) {
                    LevelDot(levelColorOf(state, rec))
                    Spacer(Modifier.width(10.dp))
                    Column(Modifier.weight(1f)) {
                        Row(verticalAlignment = Alignment.CenterVertically) {
                            Text(
                                "${rec.deck}${rec.room}",
                                style = MaterialTheme.typography.titleMedium,
                                fontWeight = FontWeight.Bold,
                                color = Slate900,
                            )
                            Spacer(Modifier.width(8.dp))
                            Row(
                                modifier = Modifier
                                    .clip(RoundedCornerShape(6.dp))
                                    .background(levelColorOf(state, rec).copy(alpha = 0.15f))
                                    .padding(horizontal = 6.dp, vertical = 1.dp),
                            ) {
                                Text(
                                    levelNameOf(state, rec),
                                    style = MaterialTheme.typography.labelSmall,
                                    color = Slate900,
                                    fontWeight = FontWeight.Medium,
                                )
                            }
                            Spacer(Modifier.width(6.dp))
                            Text(
                                formatTs(rec.timestamp),
                                style = MaterialTheme.typography.labelSmall,
                                color = Slate500,
                            )
                        }
                        if (rec.note.isNotEmpty()) {
                            Spacer(Modifier.height(2.dp))
                            Text(
                                rec.note,
                                style = MaterialTheme.typography.bodySmall,
                                color = Slate700,
                            )
                        }
                    }
                    Checkbox(
                        checked = rec.replied,
                        onCheckedChange = { checked ->
                            if (checked) onRequestReply(rec)
                            else onCancelReply(rec.id)
                        },
                        colors = CheckboxDefaults.colors(
                            checkedColor = CruiseAzure,
                            checkmarkColor = Color.White,
                        ),
                    )
                }
            }
        }
    }
}

/** 独立流水卡片 */
@Composable
private fun RecordCard(state: PixieDustUiState, record: PixieDustRecord, onClick: () -> Unit) {
    Surface(
        shape = RoundedCornerShape(14.dp),
        color = MaterialTheme.colorScheme.surface,
        border = androidx.compose.foundation.BorderStroke(1.dp, Slate200),
        modifier = Modifier.fillMaxWidth(),
    ) {
        RecordCardRow(state = state, record = record, onClick = onClick)
    }
}

@Composable
private fun RecordCardRow(state: PixieDustUiState, record: PixieDustRecord, onClick: () -> Unit) {
    Row(
        Modifier.fillMaxWidth().clickable(onClick = onClick).padding(12.dp),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        LevelDot(levelColorOf(state, record))
        Spacer(Modifier.width(10.dp))
        Column(Modifier.weight(1f)) {
            Row(verticalAlignment = Alignment.CenterVertically) {
                Text(
                    "${record.deck}${record.room}",
                    style = MaterialTheme.typography.titleMedium,
                    fontWeight = FontWeight.Bold,
                    color = Slate900,
                )
                Spacer(Modifier.width(8.dp))
                Row(
                    modifier = Modifier
                        .clip(RoundedCornerShape(6.dp))
                        .background(levelColorOf(state, record).copy(alpha = 0.15f))
                        .padding(horizontal = 6.dp, vertical = 1.dp),
                ) {
                    Text(
                        levelNameOf(state, record),
                        style = MaterialTheme.typography.labelSmall,
                        color = Slate900,
                        fontWeight = FontWeight.Medium,
                    )
                }
                Spacer(Modifier.width(6.dp))
                Text(
                    formatTs(record.timestamp),
                    style = MaterialTheme.typography.labelSmall,
                    color = Slate500,
                )
            }
            if (record.note.isNotEmpty()) {
                Spacer(Modifier.height(2.dp))
                Text(
                    record.note,
                    style = MaterialTheme.typography.bodySmall,
                    color = Slate700,
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
        Column(horizontalAlignment = Alignment.CenterHorizontally) {
            Text("✨", fontSize = 32.sp)
            Spacer(Modifier.height(8.dp))
            Text(
                text,
                style = MaterialTheme.typography.bodyMedium,
                color = Slate500,
            )
        }
    }
}


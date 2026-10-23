package com.cruise.assistant.ui.pixiedust

import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.KeyboardArrowDown
import androidx.compose.material.icons.filled.KeyboardArrowUp
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.unit.dp
import com.cruise.assistant.R
import com.cruise.assistant.data.CATEGORY_PALETTE
import com.cruise.assistant.domain.PixieLevel
import com.cruise.assistant.ui.common.iconFor

/** 等级管理：列表（上移/下移/点开编辑/删除）+ 新增 */
private val LEVEL_ICON_KEYS = listOf("gift", "sparkle", "diamond", "star", "heart")

@Composable
fun PixieLevelSheet(
    levels: List<PixieLevel>,
    onSave: (PixieLevel) -> Unit,
    onDelete: (PixieLevel) -> Unit,
    onMove: (PixieLevel, Boolean) -> Unit,
    countByLevel: suspend (Long) -> Int,
    onDismiss: () -> Unit,
) {
    var editing by remember { mutableStateOf<PixieLevel?>(null) }
    var confirmDelete by remember { mutableStateOf<PixieLevel?>(null) }

    // ---- 删除确认（区分有无引用）----
    confirmDelete?.let { target ->
        // countByLevel 是 suspend，Compose 文本 lambda 不是协程上下文，
        // 用 LaunchedEffect 拉一次引用数，加载完成前不显示正文（避免闪错误文案）。
        var usedCount by remember(target.id) { mutableStateOf<Int?>(null) }
        LaunchedEffect(target.id) { usedCount = countByLevel(target.id) }
        AlertDialog(
            onDismissRequest = { confirmDelete = null },
            title = { Text(stringResource(R.string.pixie_delete)) },
            text = {
                val used = usedCount
                if (used != null) {
                    Text(
                        if (used > 0) stringResource(R.string.pixie_level_delete_used_format, target.name, used)
                        else stringResource(R.string.pixie_level_delete_free_format, target.name)
                    )
                }
            },
            confirmButton = {
                TextButton(onClick = {
                    onDelete(target)
                    confirmDelete = null
                }) { Text(stringResource(R.string.pixie_delete), color = MaterialTheme.colorScheme.error) }
            },
            dismissButton = {
                TextButton(onClick = { confirmDelete = null }) {
                    Text(stringResource(R.string.pixie_cancel))
                }
            },
        )
    }

    // ---- 编辑器 ----
    editing?.let { target ->
        LevelEditorDialog(
            initial = target,
            isNew = target.id == 0L,
            onSave = {
                onSave(it)
                editing = null
            },
            onDismiss = { editing = null },
        )
        return
    }

    // ---- 列表 ----
    AlertDialog(
        onDismissRequest = onDismiss,
        title = { Text(stringResource(R.string.pixie_manage_levels)) },
        text = {
            Column(Modifier.verticalScroll(rememberScrollState())) {
                levels.forEachIndexed { index, level ->
                    Row(
                        Modifier.fillMaxWidth().padding(vertical = 4.dp),
                        verticalAlignment = Alignment.CenterVertically,
                    ) {
                        Surface(shape = CircleShape, color = Color(level.colorArgb), modifier = Modifier.size(12.dp)) {}
                        Spacer(Modifier.width(8.dp))
                        Text(
                            level.name,
                            modifier = Modifier.weight(1f).clickable { editing = level.copy() },
                        )
                        Text(
                            stringResource(R.string.pixie_level_rank_label) + " ${level.rank}",
                            style = MaterialTheme.typography.bodySmall,
                            color = MaterialTheme.colorScheme.onSurfaceVariant,
                        )
                        IconButton(onClick = { onMove(level, true) }, enabled = index > 0) {
                            Icon(Icons.Filled.KeyboardArrowUp, contentDescription = stringResource(R.string.pixie_level_move_up))
                        }
                        IconButton(onClick = { onMove(level, false) }, enabled = index < levels.size - 1) {
                            Icon(Icons.Filled.KeyboardArrowDown, contentDescription = stringResource(R.string.pixie_level_move_down))
                        }
                        // 只剩一个等级时不许删
                        IconButton(onClick = { confirmDelete = level }, enabled = levels.size > 1) {
                            Text("✕", color = MaterialTheme.colorScheme.error)
                        }
                    }
                }
                TextButton(onClick = {
                    editing = PixieLevel(
                        name = "",
                        colorArgb = CATEGORY_PALETTE.first(),
                        iconKey = LEVEL_ICON_KEYS.first(),
                        rank = (levels.maxOfOrNull { it.rank } ?: 0) + 1,
                        sortOrder = (levels.maxOfOrNull { it.sortOrder } ?: -1) + 1,
                    )
                }) { Text(stringResource(R.string.pixie_add_level)) }
            }
        },
        confirmButton = {
            TextButton(onClick = onDismiss) { Text(stringResource(R.string.pixie_back)) }
        },
    )
}

/** 单个等级编辑：名称/颜色/图标/贵重度(rank 1..9 步进) */
@Composable
private fun LevelEditorDialog(
    initial: PixieLevel,
    isNew: Boolean,
    onSave: (PixieLevel) -> Unit,
    onDismiss: () -> Unit,
) {
    var name by remember { mutableStateOf(initial.name) }
    var colorArgb by remember { mutableStateOf(initial.colorArgb) }
    var iconKey by remember { mutableStateOf(initial.iconKey) }
    var rank by remember { mutableStateOf(initial.rank) }

    AlertDialog(
        onDismissRequest = onDismiss,
        title = {
            Text(
                if (isNew) stringResource(R.string.pixie_add_level)
                else stringResource(R.string.pixie_manage_levels)
            )
        },
        text = {
            Column {
                OutlinedTextField(
                    value = name,
                    onValueChange = { name = it },
                    label = { Text(stringResource(R.string.pixie_level_name_label)) },
                    singleLine = true,
                )
                Spacer(Modifier.height(12.dp))
                // 颜色
                Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                    CATEGORY_PALETTE.forEach { c ->
                        val selected = c == colorArgb
                        Box(
                            modifier = Modifier
                                .size(28.dp)
                                .background(Color(c), CircleShape)
                                .then(
                                    if (selected) Modifier.border(2.dp, MaterialTheme.colorScheme.onSurface, CircleShape)
                                    else Modifier.clickable { colorArgb = c }
                                ),
                        )
                    }
                }
                Spacer(Modifier.height(12.dp))
                // 图标
                Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                    LEVEL_ICON_KEYS.forEach { key ->
                        val selected = key == iconKey
                        IconButton(onClick = { iconKey = key }) {
                            Icon(
                                iconFor(key),
                                contentDescription = key,
                                tint = if (selected) MaterialTheme.colorScheme.primary
                                else MaterialTheme.colorScheme.onSurfaceVariant,
                            )
                        }
                    }
                }
                Spacer(Modifier.height(12.dp))
                // 贵重度步进
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Text(stringResource(R.string.pixie_level_rank_label))
                    Spacer(Modifier.width(8.dp))
                    IconButton(onClick = { if (rank > 1) rank-- }) { Text("−") }
                    Text("$rank", style = MaterialTheme.typography.titleMedium)
                    IconButton(onClick = { if (rank < 9) rank++ }) { Text("＋") }
                    Spacer(Modifier.width(8.dp))
                    Text(
                        stringResource(R.string.pixie_level_rank_hint),
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                    )
                }
            }
        },
        confirmButton = {
            TextButton(
                onClick = { onSave(initial.copy(name = name.trim(), colorArgb = colorArgb, iconKey = iconKey, rank = rank)) },
                enabled = name.isNotBlank(),
            ) { Text(stringResource(R.string.pixie_level_save)) }
        },
        dismissButton = {
            TextButton(onClick = onDismiss) { Text(stringResource(R.string.pixie_cancel)) }
        },
    )
}

package com.cruise.assistant.ui.pixiedust

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
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.outlined.Backspace
import androidx.compose.material3.Button
import androidx.compose.material3.DropdownMenu
import androidx.compose.material3.DropdownMenuItem
import androidx.compose.material3.FilterChip
import androidx.compose.material3.FilterChipDefaults
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
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
import androidx.compose.ui.unit.sp
import com.cruise.assistant.R
import com.cruise.assistant.domain.PixieLevel
import com.cruise.assistant.domain.isValidRoomInput
import com.cruise.assistant.ui.common.iconFor

/** 快捷录入区：楼层下拉 + 大房号显示 + 数字键盘 + 等级按钮 + 记一笔 */
@Composable
fun PixieDustKeypad(
    state: PixieDustUiState,
    onDeckChange: (Int) -> Unit,
    onDigit: (Char) -> Unit,
    onBackspace: () -> Unit,
    onClearRoom: () -> Unit,
    onLevelSelect: (Long) -> Unit,
    onSave: () -> Unit,
) {
    Column(Modifier.fillMaxWidth().padding(horizontal = 16.dp)) {
        // ---- 楼层下拉（范围来自航次，记住上次） ----
        Row(verticalAlignment = Alignment.CenterVertically) {
            Text(stringResource(R.string.pixie_deck_label), style = MaterialTheme.typography.titleSmall)
            Spacer(Modifier.width(8.dp))
            DeckDropdown(
                deck = state.deck,
                deckRange = state.deckRange,
                onDeckChange = onDeckChange,
            )
            Spacer(Modifier.weight(1f))
            TextButton(onClick = onClearRoom) {
                Text(stringResource(R.string.pixie_keypad_clear))
            }
        }

        // ---- 房号大字显示 + 历史小字 ----
        Box(Modifier.fillMaxWidth().padding(vertical = 4.dp)) {
            Text(
                text = state.roomInput.ifEmpty { stringResource(R.string.pixie_room_hint) },
                fontSize = 40.sp,
                fontWeight = FontWeight.Bold,
                letterSpacing = 6.sp,
                color = if (state.roomInput.isEmpty()) MaterialTheme.colorScheme.onSurfaceVariant
                else MaterialTheme.colorScheme.onSurface,
                modifier = Modifier.align(Alignment.Center),
            )
        }
        state.roomHistory?.let { h ->
            Text(
                stringResource(R.string.pixie_last_level_format, h.levelName, h.count),
                style = MaterialTheme.typography.bodySmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
                modifier = Modifier.align(Alignment.CenterHorizontally),
            )
        }

        // ---- 数字键盘：123/456/789/清空 0 退格 ----
        val rows = listOf(
            listOf("1", "2", "3"),
            listOf("4", "5", "6"),
            listOf("7", "8", "9"),
        )
        rows.forEach { row ->
            Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceEvenly) {
                row.forEach { d ->
                    KeypadKey(text = d, modifier = Modifier.weight(1f)) { onDigit(d[0]) }
                }
            }
        }
        Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceEvenly) {
            // 清空整格（文字），0，退格
            OutlinedButton(
                onClick = onClearRoom,
                modifier = Modifier.weight(1f).padding(2.dp).height(56.dp),
            ) { Text(stringResource(R.string.pixie_keypad_clear)) }
            KeypadKey(text = "0", modifier = Modifier.weight(1f)) { onDigit('0') }
            OutlinedButton(
                onClick = onBackspace,
                modifier = Modifier.weight(1f).padding(2.dp).height(56.dp),
            ) {
                Icon(
                    Icons.AutoMirrored.Outlined.Backspace,
                    contentDescription = stringResource(R.string.pixie_keypad_backspace),
                )
            }
        }

        // ---- 等级按钮（选中 = 等级色填充） ----
        Row(
            Modifier.fillMaxWidth().padding(top = 8.dp),
            horizontalArrangement = Arrangement.SpaceEvenly,
        ) {
            state.levels.forEach { level ->
                LevelChip(
                    level = level,
                    selected = state.selectedLevelId == level.id,
                    onClick = { onLevelSelect(level.id) },
                    modifier = Modifier.weight(1f).padding(horizontal = 2.dp),
                )
            }
        }

        // ---- 记一笔（房号三位且选了等级才可用） ----
        Button(
            onClick = onSave,
            enabled = isValidRoomInput(state.roomInput) &&
                state.selectedLevelId != null &&
                state.selectedLevelId?.let { state.levelById.containsKey(it) } == true,
            modifier = Modifier.fillMaxWidth().padding(top = 8.dp).height(52.dp),
        ) {
            Text(stringResource(R.string.pixie_save), fontSize = 16.sp)
        }
    }
}

@Composable
private fun DeckDropdown(deck: Int, deckRange: IntRange, onDeckChange: (Int) -> Unit) {
    var expanded by remember { mutableStateOf(false) }
    Box {
        OutlinedButton(onClick = { expanded = true }) {
            Text(stringResource(R.string.pixie_deck_format, deck))
        }
        DropdownMenu(expanded = expanded, onDismissRequest = { expanded = false }) {
            deckRange.forEach { d ->
                DropdownMenuItem(
                    text = { Text(stringResource(R.string.pixie_deck_format, d)) },
                    onClick = {
                        expanded = false
                        onDeckChange(d)
                    },
                )
            }
        }
    }
}

@Composable
private fun KeypadKey(text: String, modifier: Modifier = Modifier, onClick: () -> Unit) {
    OutlinedButton(
        onClick = onClick,
        modifier = modifier.padding(2.dp).height(56.dp),
    ) {
        Text(text, fontSize = 22.sp)
    }
}

/** 等级选择大按钮：圆点图标 + 名称，选中时用等级色容器 */
@Composable
internal fun LevelChip(
    level: PixieLevel,
    selected: Boolean,
    onClick: () -> Unit,
    modifier: Modifier = Modifier,
) {
    FilterChip(
        selected = selected,
        onClick = onClick,
        modifier = modifier,
        label = {
            Row(verticalAlignment = Alignment.CenterVertically) {
                Icon(
                    iconFor(level.iconKey),
                    contentDescription = null,
                    tint = if (selected) Color(level.colorArgb) else MaterialTheme.colorScheme.onSurfaceVariant,
                    modifier = Modifier.size(18.dp),
                )
                Spacer(Modifier.width(4.dp))
                Text(level.name)
            }
        },
        colors = if (selected) {
            FilterChipDefaults.filterChipColors(
                containerColor = Color(level.colorArgb).copy(alpha = 0.25f),
                selectedContainerColor = Color(level.colorArgb).copy(alpha = 0.35f),
            )
        } else {
            FilterChipDefaults.filterChipColors()
        },
    )
}

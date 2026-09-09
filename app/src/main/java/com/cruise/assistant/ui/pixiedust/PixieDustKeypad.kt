package com.cruise.assistant.ui.pixiedust

import androidx.compose.foundation.background
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
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.outlined.Backspace
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.FilterChip
import androidx.compose.material3.FilterChipDefaults
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.cruise.assistant.R
import com.cruise.assistant.domain.PixieLevel
import com.cruise.assistant.domain.StateroomValidationStatus
import com.cruise.assistant.ui.common.iconFor
import com.cruise.assistant.ui.theme.CruiseAzure
import com.cruise.assistant.ui.theme.CruiseMarine
import com.cruise.assistant.ui.theme.Slate100
import com.cruise.assistant.ui.theme.Slate200
import com.cruise.assistant.ui.theme.Slate400
import com.cruise.assistant.ui.theme.Slate500
import com.cruise.assistant.ui.theme.Slate700
import com.cruise.assistant.ui.theme.Slate900

/** 快捷录入区：全房号直输 + 实时验房徽章 + 数字键盘 + 等级按钮 + 记一笔 */
@Composable
fun PixieDustKeypad(
    state: PixieDustUiState,
    onDeckChange: (Int) -> Unit = {},
    onDigit: (Char) -> Unit,
    onBackspace: () -> Unit,
    onClearRoom: () -> Unit,
    onLevelSelect: (Long) -> Unit,
    onSave: () -> Unit,
) {
    Surface(
        shape = RoundedCornerShape(16.dp),
        color = MaterialTheme.colorScheme.surface,
        border = androidx.compose.foundation.BorderStroke(1.dp, Slate200),
        modifier = Modifier
            .fillMaxWidth()
            .padding(horizontal = 16.dp, vertical = 4.dp),
    ) {
        Column(Modifier.padding(12.dp)) {
            // ---- 顶部清空与提示 ----
            Row(
                modifier = Modifier.fillMaxWidth(),
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.SpaceBetween,
            ) {
                Text(
                    stringResource(R.string.pixie_room_hint),
                    style = MaterialTheme.typography.labelMedium,
                    color = Slate500,
                    fontWeight = FontWeight.Medium,
                )
                TextButton(onClick = onClearRoom) {
                    Text(
                        stringResource(R.string.pixie_keypad_clear),
                        color = Slate500,
                        style = MaterialTheme.typography.labelSmall,
                    )
                }
            }

            // ---- 房号大字显示 + 历史小字 ----
            Box(
                Modifier
                    .fillMaxWidth()
                    .clip(RoundedCornerShape(12.dp))
                    .background(Slate100)
                    .padding(vertical = 8.dp),
            ) {
                Column(
                    Modifier.fillMaxWidth(),
                    horizontalAlignment = Alignment.CenterHorizontally,
                ) {
                    Text(
                        text = state.roomInput.ifEmpty { stringResource(R.string.pixie_room_hint_direct) },
                        fontSize = 30.sp,
                        fontWeight = FontWeight.Bold,
                        letterSpacing = 3.sp,
                        color = if (state.roomInput.isEmpty()) Slate400 else Slate900,
                    )
                    when (state.validation.status) {
                        StateroomValidationStatus.VALID -> {
                            Text(
                                text = stringResource(R.string.pixie_room_valid_format, state.validation.deck ?: 0),
                                style = MaterialTheme.typography.labelSmall,
                                color = Color(0xFF16A34A),
                                fontWeight = FontWeight.Bold,
                            )
                        }
                        StateroomValidationStatus.INVALID -> {
                            Text(
                                text = stringResource(R.string.pixie_room_invalid),
                                style = MaterialTheme.typography.labelSmall,
                                color = MaterialTheme.colorScheme.error,
                                fontWeight = FontWeight.Bold,
                            )
                        }
                        StateroomValidationStatus.INCOMPLETE -> {
                            Text(
                                text = stringResource(R.string.pixie_room_hint_direct),
                                style = MaterialTheme.typography.labelSmall,
                                color = Slate400,
                            )
                        }
                    }
                    state.roomHistory?.let { h ->
                        Text(
                            stringResource(R.string.pixie_last_level_format, h.levelName, h.count),
                            style = MaterialTheme.typography.labelSmall,
                            color = CruiseMarine,
                            fontWeight = FontWeight.Medium,
                        )
                    }
                }
            }

            Spacer(Modifier.height(8.dp))

            // ---- 数字键盘：123/456/789/清空 0 退格 ----
            val rows = listOf(
                listOf("1", "2", "3"),
                listOf("4", "5", "6"),
                listOf("7", "8", "9"),
            )
            rows.forEach { row ->
                Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(6.dp)) {
                    row.forEach { d ->
                        KeypadKey(text = d, modifier = Modifier.weight(1f)) { onDigit(d[0]) }
                    }
                }
                Spacer(Modifier.height(6.dp))
            }
            Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(6.dp)) {
                OutlinedButton(
                    onClick = onClearRoom,
                    shape = RoundedCornerShape(12.dp),
                    colors = ButtonDefaults.outlinedButtonColors(contentColor = Slate700),
                    modifier = Modifier.weight(1f).height(48.dp),
                ) {
                    Text(stringResource(R.string.pixie_keypad_clear), fontSize = 14.sp)
                }
                KeypadKey(text = "0", modifier = Modifier.weight(1f)) { onDigit('0') }
                OutlinedButton(
                    onClick = onBackspace,
                    shape = RoundedCornerShape(12.dp),
                    colors = ButtonDefaults.outlinedButtonColors(contentColor = Slate700),
                    modifier = Modifier.weight(1f).height(48.dp),
                ) {
                    Icon(
                        Icons.AutoMirrored.Outlined.Backspace,
                        contentDescription = stringResource(R.string.pixie_keypad_backspace),
                        modifier = Modifier.size(20.dp),
                    )
                }
            }

            // ---- 等级按钮 ----
            Spacer(Modifier.height(8.dp))
            Row(
                Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(4.dp),
            ) {
                state.levels.forEach { level ->
                    LevelChip(
                        level = level,
                        selected = state.selectedLevelId == level.id,
                        onClick = { onLevelSelect(level.id) },
                        modifier = Modifier.weight(1f),
                    )
                }
            }

            // ---- 记一笔 ----
            Spacer(Modifier.height(10.dp))
            Button(
                onClick = onSave,
                enabled = state.validation.status == StateroomValidationStatus.VALID &&
                    state.selectedLevelId != null &&
                    state.selectedLevelId?.let { state.levelById.containsKey(it) } == true,
                shape = RoundedCornerShape(14.dp),
                colors = ButtonDefaults.buttonColors(containerColor = CruiseAzure),
                modifier = Modifier.fillMaxWidth().height(48.dp),
            ) {
                Text(stringResource(R.string.pixie_save), fontSize = 15.sp, fontWeight = FontWeight.Bold)
            }
        }
    }
}


@Composable
private fun KeypadKey(text: String, modifier: Modifier = Modifier, onClick: () -> Unit) {
    Surface(
        shape = RoundedCornerShape(12.dp),
        color = Slate100,
        modifier = modifier.height(48.dp).clip(RoundedCornerShape(12.dp)),
    ) {
        Box(
            modifier = Modifier
                .fillMaxWidth()
                .clickable(onClick = onClick),
            contentAlignment = Alignment.Center,
        ) {
            Text(text, fontSize = 20.sp, fontWeight = FontWeight.SemiBold, color = Slate900)
        }
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
                    tint = if (selected) Color(level.colorArgb) else Slate500,
                    modifier = Modifier.size(16.dp),
                )
                Spacer(Modifier.width(3.dp))
                Text(level.name, fontSize = 11.sp)
            }
        },
        colors = if (selected) {
            FilterChipDefaults.filterChipColors(
                containerColor = Color(level.colorArgb).copy(alpha = 0.22f),
                selectedContainerColor = Color(level.colorArgb).copy(alpha = 0.32f),
                selectedLabelColor = Slate900,
            )
        } else {
            FilterChipDefaults.filterChipColors(
                containerColor = Slate100,
                labelColor = Slate700,
            )
        },
    )
}


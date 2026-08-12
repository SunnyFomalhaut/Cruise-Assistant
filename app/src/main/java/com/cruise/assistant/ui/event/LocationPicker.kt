package com.cruise.assistant.ui.event

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.width
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.ArrowDropDown
import androidx.compose.material3.Checkbox
import androidx.compose.material3.DropdownMenu
import androidx.compose.material3.DropdownMenuItem
import androidx.compose.material3.FilterChip
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Text
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
import com.cruise.assistant.domain.LocationPreset
import com.cruise.assistant.domain.ShipZone

/**
 * 需求3：楼层下拉 + 船头/船中/船尾 + 地点名（可输入，也可从历史里选）。
 * 勾"不设地点"时整块禁用，对应 deck=null。
 *
 * [enabled]：整个控件（含"不设地点"勾选框本身）的外层开关，默认 true。这是 Fix round 2
 * 补的 API：此前勾选框完全不受任何 enabled 计算影响 —— 内部的 `enabled = !noLocation` 只
 * 用来灰化楼层/区域/地点名，勾选框自己永远可点。调用方若需要整块彻底不可交互（例如系统事件），
 * 传 `enabled = false`；正常事件不传，用默认值，勾选框两个方向都能正常切换。
 */
@Composable
fun LocationPicker(
    maxDeck: Int,
    minDeck: Int = 1,
    deck: Int?,
    zone: ShipZone?,
    placeName: String,
    presets: List<LocationPreset>,
    noLocation: Boolean,
    onDeckChange: (Int) -> Unit,
    onZoneChange: (ShipZone) -> Unit,
    onPlaceNameChange: (String) -> Unit,
    onNoLocationToggle: (Boolean) -> Unit,
    modifier: Modifier = Modifier,
    enabled: Boolean = true,
    /** 预置地点下拉选项；非空且 [presetMode] 为 true 时渲染下拉模式 */
    presetOptions: List<com.cruise.assistant.domain.LocationPresetOption> = emptyList(),
    /** 下拉模式：true = 预置下拉 + 底部自定义；false = 自由输入 */
    presetMode: Boolean = false,
    onSelectPreset: (com.cruise.assistant.domain.LocationCatalogEntry) -> Unit = {},
    onCustomLocation: () -> Unit = {},
) {
    var deckMenuOpen by remember { mutableStateOf(false) }
    val fieldsEnabled = enabled && !noLocation

    // 同层同区的历史地点排前面
    val suggestions = remember(presets, deck, zone) {
        presets.sortedByDescending { p ->
            (if (p.deck == deck) 2 else 0) + (if (p.zone == zone) 1 else 0)
        }.take(12)
    }

    Column(modifier) {
        var presetMenuOpen by remember { mutableStateOf(false) }
        if (presetMode && presetOptions.isNotEmpty()) {
            Text(stringResource(R.string.location_label), style = MaterialTheme.typography.titleMedium)
            Spacer(Modifier.height(8.dp))
            Box {
                OutlinedTextField(
                    value = placeName.ifEmpty { stringResource(R.string.select_location_hint) },
                    onValueChange = { },
                    label = { Text(stringResource(R.string.location_label)) },
                    readOnly = true,
                    singleLine = true,
                    enabled = fieldsEnabled,
                    trailingIcon = {
                        IconButton(onClick = { presetMenuOpen = true }, enabled = fieldsEnabled) {
                            Icon(Icons.Filled.ArrowDropDown, contentDescription = null)
                        }
                    },
                    modifier = Modifier.fillMaxWidth(),
                )
                DropdownMenu(
                    expanded = presetMenuOpen,
                    onDismissRequest = { presetMenuOpen = false },
                    modifier = Modifier.heightIn(max = 360.dp),
                ) {
                    presetOptions.forEach { option ->
                        when (option) {
                            is com.cruise.assistant.domain.LocationPresetOption.Group -> {
                                DropdownMenuItem(
                                    text = {
                                        Text(
                                            option.label,
                                            style = MaterialTheme.typography.labelSmall,
                                            color = MaterialTheme.colorScheme.onSurfaceVariant,
                                        )
                                    },
                                    enabled = false,
                                    onClick = {},
                                )
                            }
                            is com.cruise.assistant.domain.LocationPresetOption.Entry -> {
                                DropdownMenuItem(
                                    text = {
                                        Text(
                                            "Deck ${option.entry.deck} ${option.entry.zone.label} ${option.entry.placeName}",
                                        )
                                    },
                                    onClick = {
                                        onSelectPreset(option.entry)
                                        presetMenuOpen = false
                                    },
                                )
                            }
                            is com.cruise.assistant.domain.LocationPresetOption.Custom -> {
                                DropdownMenuItem(
                                    text = { Text(stringResource(R.string.custom_location)) },
                                    onClick = {
                                        onCustomLocation()
                                        presetMenuOpen = false
                                    },
                                )
                            }
                        }
                    }
                }
            }
            Spacer(Modifier.height(4.dp))
            Row(verticalAlignment = Alignment.CenterVertically) {
                Checkbox(checked = noLocation, onCheckedChange = onNoLocationToggle, enabled = enabled)
                Text(stringResource(R.string.no_location), style = MaterialTheme.typography.bodySmall)
            }
        } else {
            Text(stringResource(R.string.location_label), style = MaterialTheme.typography.titleMedium)
            Spacer(Modifier.height(8.dp))

            Row(verticalAlignment = Alignment.CenterVertically) {
                // 楼层下拉
                Box {
                    OutlinedButton(
                        onClick = { deckMenuOpen = true },
                        enabled = fieldsEnabled,
                    ) { Text(if (deck != null) "Deck $deck" else stringResource(R.string.deck_label)) }
                    DropdownMenu(
                        expanded = deckMenuOpen,
                        onDismissRequest = { deckMenuOpen = false },
                        modifier = Modifier.heightIn(max = 280.dp),
                    ) {
                        (minDeck..maxDeck).forEach { floor ->
                            DropdownMenuItem(
                                text = { Text("Deck $floor") },
                                onClick = {
                                    onDeckChange(floor)
                                    deckMenuOpen = false
                                },
                            )
                        }
                    }
                }
                Spacer(Modifier.width(12.dp))
                // 区域三选一
                Row(horizontalArrangement = Arrangement.spacedBy(6.dp)) {
                    ShipZone.entries.forEach { z ->
                        FilterChip(
                            selected = zone == z,
                            onClick = { onZoneChange(z) },
                            enabled = fieldsEnabled,
                            label = { Text(z.label) },
                        )
                    }
                }
            }

            Spacer(Modifier.height(8.dp))
            Box {
                OutlinedTextField(
                    value = placeName,
                    onValueChange = onPlaceNameChange,
                    label = { Text(stringResource(R.string.place_name_hint)) },
                    enabled = fieldsEnabled,
                    singleLine = true,
                    trailingIcon = {
                        if (suggestions.isNotEmpty()) {
                            IconButton(
                                onClick = { presetMenuOpen = true },
                                enabled = fieldsEnabled,
                            ) {
                                Icon(
                                    Icons.Filled.ArrowDropDown,
                                    contentDescription = null,
                                )
                            }
                        }
                    },
                    modifier = Modifier.fillMaxWidth(),
                )
                DropdownMenu(
                    expanded = presetMenuOpen,
                    onDismissRequest = { presetMenuOpen = false },
                    modifier = Modifier.heightIn(max = 280.dp),
                ) {
                    suggestions.forEach { preset ->
                        DropdownMenuItem(
                            text = { Text("Deck ${preset.deck} ${preset.zone.label} ${preset.placeName}") },
                            onClick = {
                                onDeckChange(preset.deck)
                                onZoneChange(preset.zone)
                                onPlaceNameChange(preset.placeName)
                                presetMenuOpen = false
                            },
                        )
                    }
                }
            }

            Spacer(Modifier.height(4.dp))
            Row(verticalAlignment = Alignment.CenterVertically) {
                Checkbox(checked = noLocation, onCheckedChange = onNoLocationToggle, enabled = enabled)
                Text(stringResource(R.string.no_location), style = MaterialTheme.typography.bodySmall)
            }
        }
    }
}

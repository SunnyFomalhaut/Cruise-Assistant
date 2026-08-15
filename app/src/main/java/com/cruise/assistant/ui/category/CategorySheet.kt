@file:OptIn(androidx.compose.material3.ExperimentalMaterial3Api::class)

package com.cruise.assistant.ui.category

import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.horizontalScroll
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.grid.GridCells
import androidx.compose.foundation.lazy.grid.LazyVerticalGrid
import androidx.compose.foundation.lazy.grid.items
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Add
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.DropdownMenu
import androidx.compose.material3.DropdownMenuItem
import androidx.compose.material3.FilterChip
import androidx.compose.material3.FilterChipDefaults
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.ModalBottomSheet
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Switch
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.material3.rememberModalBottomSheetState
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
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
import com.cruise.assistant.data.CATEGORY_PALETTE
import com.cruise.assistant.ui.common.ColorDot
import com.cruise.assistant.ui.common.ICON_REGISTRY
import com.cruise.assistant.ui.common.iconFor
import com.cruise.assistant.ui.theme.CruiseAzure
import com.cruise.assistant.ui.theme.CruiseAzureContainer
import com.cruise.assistant.ui.theme.CruiseMarine
import com.cruise.assistant.ui.theme.Slate100
import com.cruise.assistant.ui.theme.Slate200
import com.cruise.assistant.ui.theme.Slate500
import com.cruise.assistant.ui.theme.Slate700
import com.cruise.assistant.ui.theme.Slate900

/**
 * 截图3 的"添加图标"弹窗。
 * 保留：名称、二级分类开关、归档、主题色、图标网格。
 * 删掉：计时模式（用户明确要求）。
 */
@Composable
fun CategorySheet(
    state: CategorySheetState,
    onNameChange: (String) -> Unit,
    onColorSelect: (Int) -> Unit,
    onIconSelect: (String) -> Unit,
    onSubCategoryToggle: (Boolean) -> Unit,
    onParentSelect: (Long) -> Unit,
    onArchiveToggle: (Boolean) -> Unit,
    onEditExisting: (Long) -> Unit,
    onNewCategory: () -> Unit,
    onSave: () -> Unit,
    onDelete: () -> Unit,
    onDismiss: () -> Unit,
) {
    val sheetState = rememberModalBottomSheetState(skipPartiallyExpanded = true)
    var parentMenuOpen by remember { mutableStateOf(false) }

    // 弹窗每次打开都重置成"新建"状态：VM 在导航图里活得比一次开合久（同一 Timeline
    // 条目复用），如果不主动重置，上次编辑/新建留下的名称、颜色、图标会原样留着，
    // 用户什么都没改就再点一次保存修改会把上次的内容插成第二条重复记录。
    LaunchedEffect(Unit) { onNewCategory() }

    ModalBottomSheet(onDismissRequest = onDismiss, sheetState = sheetState) {
        Column(
            Modifier
                .verticalScroll(rememberScrollState())
                .padding(horizontal = 20.dp)
                .padding(bottom = 24.dp),
        ) {
            Text(
                stringResource(
                    if (state.isEditing) {
                        R.string.category_sheet_title_edit
                    } else {
                        R.string.category_sheet_title_add
                    },
                ),
                style = MaterialTheme.typography.titleLarge,
                fontWeight = FontWeight.Bold,
                color = Slate900,
            )
            Spacer(Modifier.height(16.dp))

            // ---- 现有类别横排，点一下切到编辑它 ----
            if (state.allCategories.isNotEmpty()) {
                Row(
                    Modifier
                        .fillMaxWidth()
                        .horizontalScroll(rememberScrollState()),
                    horizontalArrangement = Arrangement.spacedBy(6.dp),
                ) {
                    state.allCategories.forEach { category ->
                        FilterChip(
                            selected = category.id == state.editingId,
                            onClick = { onEditExisting(category.id) },
                            label = { Text(category.name) },
                            colors = FilterChipDefaults.filterChipColors(
                                selectedContainerColor = CruiseAzureContainer,
                                selectedLabelColor = CruiseMarine,
                            ),
                        )
                    }
                    FilterChip(
                        selected = !state.isEditing,
                        onClick = onNewCategory,
                        enabled = !state.saving,
                        label = {
                            Icon(Icons.Filled.Add, contentDescription = stringResource(R.string.category_sheet_title_add))
                        },
                        colors = FilterChipDefaults.filterChipColors(
                            selectedContainerColor = CruiseAzureContainer,
                            selectedLabelColor = CruiseMarine,
                        ),
                    )
                }
                Spacer(Modifier.height(16.dp))
            }

            // ---- 名称 + 二级分类 + 归档 ----
            Row(verticalAlignment = Alignment.CenterVertically) {
                Text(
                    stringResource(R.string.category_name_label),
                    style = MaterialTheme.typography.titleMedium,
                    fontWeight = FontWeight.SemiBold,
                    color = Slate900,
                    modifier = Modifier.weight(1f),
                )
                Text(stringResource(R.string.sub_category), style = MaterialTheme.typography.bodySmall, color = Slate700)
                Spacer(Modifier.width(4.dp))
                Switch(checked = state.isSubCategory, onCheckedChange = onSubCategoryToggle)
                Spacer(Modifier.width(8.dp))
                FilterChip(
                    selected = state.isArchived,
                    onClick = { onArchiveToggle(!state.isArchived) },
                    label = { Text(stringResource(R.string.archive)) },
                    colors = FilterChipDefaults.filterChipColors(
                        selectedContainerColor = CruiseAzureContainer,
                        selectedLabelColor = CruiseMarine,
                    ),
                )
            }
            Spacer(Modifier.height(8.dp))
            OutlinedTextField(
                value = state.name,
                onValueChange = onNameChange,
                label = { Text(stringResource(R.string.category_name_hint)) },
                singleLine = true,
                shape = RoundedCornerShape(12.dp),
                modifier = Modifier.fillMaxWidth(),
            )

            // ---- 上级分类（仅二级分类开启时）----
            if (state.isSubCategory) {
                Spacer(Modifier.height(12.dp))
                Box {
                    OutlinedButton(
                        onClick = { parentMenuOpen = true },
                        shape = RoundedCornerShape(12.dp),
                        modifier = Modifier.fillMaxWidth(),
                    ) {
                        val parentName = state.allCategories
                            .firstOrNull { it.id == state.parentId }?.name
                        Text(parentName ?: stringResource(R.string.parent_category_label), fontWeight = FontWeight.Medium)
                    }
                    DropdownMenu(
                        expanded = parentMenuOpen,
                        onDismissRequest = { parentMenuOpen = false },
                        modifier = Modifier.heightIn(max = 280.dp),
                    ) {
                        state.parentCandidates.forEach { candidate ->
                            DropdownMenuItem(
                                text = { Text(candidate.name) },
                                onClick = {
                                    onParentSelect(candidate.id)
                                    parentMenuOpen = false
                                },
                            )
                        }
                    }
                }
            }

            // ---- 主题色 ----
            Spacer(Modifier.height(20.dp))
            Text(
                stringResource(R.string.timeline_theme_color),
                style = MaterialTheme.typography.titleMedium,
                fontWeight = FontWeight.SemiBold,
                color = Slate900,
            )
            Spacer(Modifier.height(8.dp))
            Row(
                Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
            ) {
                CATEGORY_PALETTE.forEach { argb ->
                    ColorDot(
                        colorArgb = argb,
                        selected = argb == state.colorArgb,
                        onClick = { onColorSelect(argb) },
                    )
                }
            }

            // ---- 图标网格 ----
            Spacer(Modifier.height(20.dp))
            Text(stringResource(R.string.select_icon), style = MaterialTheme.typography.titleMedium, fontWeight = FontWeight.SemiBold, color = Slate900)
            Spacer(Modifier.height(8.dp))
            LazyVerticalGrid(
                columns = GridCells.Fixed(7),
                modifier = Modifier.height(160.dp),
                horizontalArrangement = Arrangement.spacedBy(6.dp),
                verticalArrangement = Arrangement.spacedBy(6.dp),
            ) {
                items(ICON_REGISTRY) { (key, _) ->
                    val selected = key == state.iconKey
                    Box(
                        Modifier
                            .size(44.dp)
                            .clip(RoundedCornerShape(12.dp))
                            .background(
                                if (selected) {
                                    Color(state.colorArgb)
                                } else {
                                    Slate100
                                },
                            )
                            .clickable { onIconSelect(key) },
                        contentAlignment = Alignment.Center,
                    ) {
                        Icon(
                            iconFor(key),
                            contentDescription = key,
                            tint = if (selected) Color.White else Slate700,
                        )
                    }
                }
            }

            state.errorResId?.let { resId ->
                Spacer(Modifier.height(12.dp))
                Text(
                    stringResource(resId),
                    color = MaterialTheme.colorScheme.error,
                    style = MaterialTheme.typography.bodySmall,
                )
            }

            // ---- 删除提示（仅自定义类别且有事件引用时）----
            if (state.isEditing && !state.isBuiltIn) {
                Spacer(Modifier.height(12.dp))
                if (state.eventCount > 0) {
                    Text(
                        stringResource(R.string.delete_category_warning_format, state.eventCount),
                        style = MaterialTheme.typography.bodySmall,
                        color = Slate500,
                    )
                }
                TextButton(onClick = onDelete, enabled = !state.saving) {
                    Text(
                        stringResource(R.string.delete),
                        color = MaterialTheme.colorScheme.error,
                    )
                }
            } else if (state.isEditing && state.isBuiltIn) {
                Spacer(Modifier.height(12.dp))
                Text(
                    stringResource(R.string.builtin_undeletable),
                    style = MaterialTheme.typography.bodySmall,
                    color = Slate500,
                )
            }

            Spacer(Modifier.height(20.dp))
            Button(
                onClick = onSave,
                enabled = !state.saving,
                shape = RoundedCornerShape(16.dp),
                colors = ButtonDefaults.buttonColors(containerColor = CruiseAzure),
                modifier = Modifier.fillMaxWidth().height(52.dp),
            ) { Text(stringResource(R.string.save_changes), fontWeight = FontWeight.Bold, fontSize = 16.sp) }
        }
    }
}

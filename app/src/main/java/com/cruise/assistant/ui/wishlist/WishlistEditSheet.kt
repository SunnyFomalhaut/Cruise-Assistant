package com.cruise.assistant.ui.wishlist

import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.background
import androidx.compose.foundation.horizontalScroll
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Delete
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.FilterChip
import androidx.compose.material3.FilterChipDefaults
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.ModalBottomSheet
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.OutlinedTextFieldDefaults
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.material3.rememberModalBottomSheetState
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
import com.cruise.assistant.domain.Category
import com.cruise.assistant.domain.ShipZone
import com.cruise.assistant.domain.WishlistItem
import com.cruise.assistant.domain.locationPresetOptions
import com.cruise.assistant.ui.common.iconFor
import com.cruise.assistant.ui.event.LocationPicker
import com.cruise.assistant.ui.theme.CruiseAzure
import com.cruise.assistant.ui.theme.CruiseAzureContainer
import com.cruise.assistant.ui.theme.CruiseMarine
import com.cruise.assistant.ui.theme.Slate100
import com.cruise.assistant.ui.theme.Slate200
import com.cruise.assistant.ui.theme.Slate500
import com.cruise.assistant.ui.theme.Slate700
import com.cruise.assistant.ui.theme.Slate900

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun WishlistEditSheet(
    initialItem: WishlistItem?,
    categories: List<Category>,
    onSave: (id: Long, title: String, categoryId: Long, deck: Int?, zone: ShipZone?, placeName: String?, note: String?) -> Unit,
    onDelete: ((WishlistItem) -> Unit)? = null,
    onDismiss: () -> Unit,
) {
    val sheetState = rememberModalBottomSheetState(skipPartiallyExpanded = true)
    val defaultCategory = categories.firstOrNull { it.name == "娱乐" || it.iconKey == "fun" }
        ?: categories.firstOrNull()

    var selectedCategoryId by remember {
        mutableStateOf(initialItem?.categoryId ?: defaultCategory?.id ?: 1L)
    }
    var title by remember { mutableStateOf(initialItem?.title ?: "") }
    var deck by remember { mutableStateOf(initialItem?.deck) }
    var zone by remember { mutableStateOf(initialItem?.zone) }
    var placeName by remember { mutableStateOf(initialItem?.placeName ?: "") }
    var note by remember { mutableStateOf(initialItem?.note ?: "") }
    var noLocation by remember { mutableStateOf(initialItem != null && initialItem.deck == null && initialItem.placeName.isNullOrEmpty()) }
    var presetMode by remember { mutableStateOf(false) }

    val selectedCategory = categories.firstOrNull { it.id == selectedCategoryId } ?: defaultCategory
    val categoryIconKey = selectedCategory?.iconKey ?: "fun"
    val presetOptions = remember(categoryIconKey) {
        locationPresetOptions(categoryIconKey)
    }

    ModalBottomSheet(
        onDismissRequest = onDismiss,
        sheetState = sheetState,
        containerColor = MaterialTheme.colorScheme.surface,
        dragHandle = null,
        shape = RoundedCornerShape(topStart = 20.dp, topEnd = 20.dp),
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 20.dp, vertical = 16.dp)
                .verticalScroll(rememberScrollState()),
        ) {
            // 顶栏：标题与删除按钮
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically,
            ) {
                Text(
                    text = if (initialItem == null) "添加随心项目" else "编辑随心项目",
                    style = MaterialTheme.typography.titleLarge,
                    fontWeight = FontWeight.Bold,
                    color = Slate900,
                )
                if (initialItem != null && onDelete != null) {
                    IconButton(onClick = { onDelete(initialItem); onDismiss() }) {
                        Icon(
                            imageVector = Icons.Default.Delete,
                            contentDescription = "删除",
                            tint = MaterialTheme.colorScheme.error,
                        )
                    }
                }
            }

            Spacer(Modifier.height(14.dp))

            // 1. 类别选择 Chips
            Text(
                text = "项目类别",
                style = MaterialTheme.typography.labelMedium,
                fontWeight = FontWeight.Bold,
                color = Slate700,
            )
            Spacer(Modifier.height(6.dp))
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .horizontalScroll(rememberScrollState()),
                horizontalArrangement = Arrangement.spacedBy(8.dp),
            ) {
                categories.forEach { cat ->
                    val isSelected = cat.id == selectedCategoryId
                    val color = Color(cat.colorArgb)
                    FilterChip(
                        selected = isSelected,
                        onClick = {
                            selectedCategoryId = cat.id
                            if (title.isBlank() && initialItem == null) {
                                title = cat.name
                            }
                        },
                        label = { Text(cat.name) },
                        leadingIcon = {
                            Icon(
                                imageVector = iconFor(cat.iconKey),
                                contentDescription = null,
                                tint = if (isSelected) Color.White else color,
                                modifier = Modifier.size(16.dp),
                            )
                        },
                        colors = FilterChipDefaults.filterChipColors(
                            selectedContainerColor = color,
                            selectedLabelColor = Color.White,
                            containerColor = Slate100,
                            labelColor = Slate700,
                        ),
                        border = if (isSelected) null else BorderStroke(1.dp, Slate200),
                        shape = RoundedCornerShape(10.dp),
                    )
                }
            }

            Spacer(Modifier.height(14.dp))

            // 2. 标题输入
            Text(
                text = "项目名称",
                style = MaterialTheme.typography.labelMedium,
                fontWeight = FontWeight.Bold,
                color = Slate700,
            )
            Spacer(Modifier.height(6.dp))
            OutlinedTextField(
                value = title,
                onValueChange = { title = it },
                placeholder = { Text("如：玩钢铁侠过山车、达菲商店购物、看日落...") },
                modifier = Modifier.fillMaxWidth(),
                singleLine = true,
                shape = RoundedCornerShape(12.dp),
                colors = OutlinedTextFieldDefaults.colors(
                    focusedBorderColor = CruiseAzure,
                    unfocusedBorderColor = Slate200,
                ),
            )

            Spacer(Modifier.height(14.dp))

            // 3. 地点选择器
            LocationPicker(
                maxDeck = 19,
                minDeck = 5,
                deck = deck,
                zone = zone,
                placeName = placeName,
                presets = emptyList(),
                noLocation = noLocation,
                presetOptions = presetOptions,
                presetMode = presetMode,
                onDeckChange = { deck = it },
                onZoneChange = { zone = it },
                onPlaceNameChange = { placeName = it },
                onNoLocationToggle = {
                    noLocation = it
                    if (it) {
                        deck = null
                        zone = null
                        placeName = ""
                    } else if (deck == null) {
                        deck = 5
                    }
                },
                onSelectPreset = { entry ->
                    deck = entry.deck
                    zone = entry.zone
                    placeName = entry.placeName
                },
                onCustomLocation = { presetMode = false },
                modifier = Modifier.fillMaxWidth(),
            )

            Spacer(Modifier.height(14.dp))

            // 4. 备注输入
            Text(
                text = "备注备忘 (可选)",
                style = MaterialTheme.typography.labelMedium,
                fontWeight = FontWeight.Bold,
                color = Slate700,
            )
            Spacer(Modifier.height(6.dp))
            OutlinedTextField(
                value = note,
                onValueChange = { note = it },
                placeholder = { Text("如：需提前预约、带相机、晚上去更漂亮等...") },
                modifier = Modifier.fillMaxWidth(),
                minLines = 2,
                maxLines = 4,
                shape = RoundedCornerShape(12.dp),
                colors = OutlinedTextFieldDefaults.colors(
                    focusedBorderColor = CruiseAzure,
                    unfocusedBorderColor = Slate200,
                ),
            )

            Spacer(Modifier.height(20.dp))

            // 5. 保存与取消按钮
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(10.dp),
            ) {
                TextButton(
                    onClick = onDismiss,
                    modifier = Modifier.weight(1f),
                ) {
                    Text("取消", color = Slate700)
                }

                Button(
                    onClick = {
                        val finalTitle = title.trim().ifEmpty { selectedCategory?.name ?: "随心项目" }
                        onSave(
                            initialItem?.id ?: 0L,
                            finalTitle,
                            selectedCategoryId,
                            if (noLocation) null else deck,
                            if (noLocation) null else zone,
                            if (noLocation) null else placeName,
                            note.trim().takeIf { it.isNotEmpty() },
                        )
                        onDismiss()
                    },
                    modifier = Modifier.weight(1.5f),
                    colors = ButtonDefaults.buttonColors(
                        containerColor = CruiseAzure,
                        contentColor = Color.White,
                    ),
                    shape = RoundedCornerShape(12.dp),
                ) {
                    Text("保存项目", fontWeight = FontWeight.Bold)
                }
            }
            Spacer(Modifier.height(8.dp))
        }
    }
}

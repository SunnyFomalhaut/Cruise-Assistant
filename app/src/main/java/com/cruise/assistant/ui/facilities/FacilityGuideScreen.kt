package com.cruise.assistant.ui.facilities

import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.horizontalScroll
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.ExperimentalLayoutApi
import androidx.compose.foundation.layout.FlowRow
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
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.KeyboardActions
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.filled.Clear
import androidx.compose.material.icons.filled.Search
import androidx.compose.material.icons.outlined.AccessTime
import androidx.compose.material.icons.outlined.Check
import androidx.compose.material.icons.outlined.LocationOn
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.CenterAlignedTopAppBar
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.FilterChip
import androidx.compose.material3.FilterChipDefaults
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.OutlinedTextFieldDefaults
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.TopAppBarDefaults
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalFocusManager
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.ImeAction
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.cruise.assistant.R
import com.cruise.assistant.domain.FacilityItemUiModel
import com.cruise.assistant.domain.OperatingSlot
import com.cruise.assistant.ui.common.iconFor
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
fun FacilityGuideScreen(
    state: FacilityGuideUiState,
    categories: List<CategoryFilterOption>,
    onBack: () -> Unit,
    onSearchChange: (String) -> Unit,
    onCategorySelect: (String) -> Unit,
    onToggleOnlyOpenNow: (Boolean) -> Unit,
) {
    val focusManager = LocalFocusManager.current

    Scaffold(
        topBar = {
            CenterAlignedTopAppBar(
                title = {
                    Column(horizontalAlignment = Alignment.CenterHorizontally) {
                        Text(
                            stringResource(R.string.facility_guide_title),
                            style = MaterialTheme.typography.titleMedium,
                            fontWeight = FontWeight.Bold,
                            color = Slate900,
                        )
                        Text(
                            stringResource(R.string.facility_guide_count_format, state.totalCount),
                            style = MaterialTheme.typography.labelSmall,
                            color = Slate500,
                        )
                    }
                },
                navigationIcon = {
                    IconButton(onClick = onBack) {
                        Icon(
                            imageVector = Icons.AutoMirrored.Filled.ArrowBack,
                            contentDescription = stringResource(R.string.cancel),
                            tint = Slate700,
                        )
                    }
                },
                colors = TopAppBarDefaults.centerAlignedTopAppBarColors(
                    containerColor = MaterialTheme.colorScheme.surface,
                ),
            )
        },
        containerColor = MaterialTheme.colorScheme.background,
    ) { innerPadding ->
        Column(
            Modifier
                .fillMaxSize()
                .padding(innerPadding),
        ) {
            // ---- 搜索栏 ----
            Box(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(horizontal = 16.dp, vertical = 8.dp),
            ) {
                OutlinedTextField(
                    value = state.searchQuery,
                    onValueChange = onSearchChange,
                    modifier = Modifier.fillMaxWidth(),
                    placeholder = {
                        Text(
                            stringResource(R.string.facility_guide_search_placeholder),
                            style = MaterialTheme.typography.bodyMedium,
                            color = Slate500,
                            maxLines = 1,
                            overflow = TextOverflow.Ellipsis,
                        )
                    },
                    leadingIcon = {
                        Icon(
                            imageVector = Icons.Default.Search,
                            contentDescription = null,
                            tint = Slate500,
                            modifier = Modifier.size(20.dp),
                        )
                    },
                    trailingIcon = {
                        if (state.searchQuery.isNotEmpty()) {
                            IconButton(onClick = { onSearchChange("") }) {
                                Icon(
                                    imageVector = Icons.Default.Clear,
                                    contentDescription = null,
                                    tint = Slate500,
                                    modifier = Modifier.size(18.dp),
                                )
                            }
                        }
                    },
                    singleLine = true,
                    shape = RoundedCornerShape(12.dp),
                    colors = OutlinedTextFieldDefaults.colors(
                        focusedBorderColor = CruiseAzure,
                        unfocusedBorderColor = Slate200,
                        focusedContainerColor = MaterialTheme.colorScheme.surface,
                        unfocusedContainerColor = MaterialTheme.colorScheme.surface,
                    ),
                    keyboardOptions = KeyboardOptions(imeAction = ImeAction.Search),
                    keyboardActions = KeyboardActions(onSearch = { focusManager.clearFocus() }),
                )
            }

            // ---- 分类横滑栏 & 仅看营业中开关 ----
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .horizontalScroll(rememberScrollState())
                    .padding(horizontal = 16.dp, vertical = 4.dp),
                horizontalArrangement = Arrangement.spacedBy(8.dp),
                verticalAlignment = Alignment.CenterVertically,
            ) {
                // "仅看营业中" 快捷Chip
                FilterChip(
                    selected = state.onlyOpenNow,
                    onClick = { onToggleOnlyOpenNow(!state.onlyOpenNow) },
                    label = { Text(stringResource(R.string.facility_guide_filter_open_now)) },
                    leadingIcon = if (state.onlyOpenNow) {
                        { Icon(Icons.Outlined.Check, contentDescription = null, modifier = Modifier.size(14.dp)) }
                    } else null,
                    colors = FilterChipDefaults.filterChipColors(
                        selectedContainerColor = Color(0xFFE6F4EA),
                        selectedLabelColor = Color(0xFF137333),
                        selectedLeadingIconColor = Color(0xFF137333),
                    ),
                    shape = RoundedCornerShape(20.dp),
                )

                categories.forEach { cat ->
                    val isSelected = state.selectedCategory == cat.key
                    FilterChip(
                        selected = isSelected,
                        onClick = { onCategorySelect(cat.key) },
                        label = { Text(cat.label) },
                        leadingIcon = {
                            Icon(
                                imageVector = iconFor(cat.iconKey),
                                contentDescription = null,
                                modifier = Modifier.size(14.dp),
                            )
                        },
                        colors = FilterChipDefaults.filterChipColors(
                            selectedContainerColor = CruiseAzureContainer,
                            selectedLabelColor = CruiseMarine,
                            selectedLeadingIconColor = CruiseMarine,
                        ),
                        shape = RoundedCornerShape(20.dp),
                    )
                }
            }

            Spacer(Modifier.height(4.dp))

            // ---- 设施列表 ----
            if (state.facilities.isEmpty()) {
                Box(
                    modifier = Modifier
                        .fillMaxSize()
                        .padding(32.dp),
                    contentAlignment = Alignment.Center,
                ) {
                    Text(
                        stringResource(R.string.facility_guide_empty_search),
                        style = MaterialTheme.typography.bodyMedium,
                        color = Slate500,
                    )
                }
            } else {
                LazyColumn(
                    modifier = Modifier.fillMaxSize(),
                    contentPadding = PaddingValues(horizontal = 16.dp, vertical = 8.dp),
                    verticalArrangement = Arrangement.spacedBy(10.dp),
                ) {
                    items(state.facilities, key = { it.placeName + "_" + it.formattedLocation }) { facility ->
                        FacilityCard(facility)
                    }
                }
            }
        }
    }
}

@OptIn(ExperimentalLayoutApi::class)
@Composable
private fun FacilityCard(facility: FacilityItemUiModel) {
    Surface(
        shape = RoundedCornerShape(14.dp),
        color = MaterialTheme.colorScheme.surface,
        border = androidx.compose.foundation.BorderStroke(1.dp, Slate200),
        modifier = Modifier.fillMaxWidth(),
    ) {
        Column(
            Modifier
                .fillMaxWidth()
                .padding(14.dp),
        ) {
            // 头部：名称 + 类别徽标
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.Top,
            ) {
                Text(
                    text = facility.placeName,
                    style = MaterialTheme.typography.titleMedium,
                    fontWeight = FontWeight.Bold,
                    color = Slate900,
                    modifier = Modifier.weight(1f),
                )
                Spacer(Modifier.width(8.dp))
                // 类别与子类别徽标
                Row(horizontalArrangement = Arrangement.spacedBy(4.dp)) {
                    facility.diningGroupLabel?.let { groupLabel ->
                        Surface(
                            shape = RoundedCornerShape(6.dp),
                            color = CruiseAzureContainer,
                        ) {
                            Text(
                                text = groupLabel,
                                style = MaterialTheme.typography.labelSmall,
                                fontWeight = FontWeight.Medium,
                                color = CruiseMarine,
                                modifier = Modifier.padding(horizontal = 6.dp, vertical = 2.dp),
                            )
                        }
                    }
                    Surface(
                        shape = RoundedCornerShape(6.dp),
                        color = Slate100,
                    ) {
                        Row(
                            modifier = Modifier.padding(horizontal = 6.dp, vertical = 2.dp),
                            verticalAlignment = Alignment.CenterVertically,
                        ) {
                            Icon(
                                imageVector = iconFor(facility.categoryKey),
                                contentDescription = null,
                                tint = Slate700,
                                modifier = Modifier.size(12.dp),
                            )
                            Spacer(Modifier.width(3.dp))
                            Text(
                                text = facility.categoryLabel,
                                style = MaterialTheme.typography.labelSmall,
                                color = Slate700,
                            )
                        }
                    }
                }
            }

            Spacer(Modifier.height(8.dp))

            // 位置行
            Row(
                verticalAlignment = Alignment.CenterVertically,
                modifier = Modifier.fillMaxWidth(),
            ) {
                Icon(
                    imageVector = Icons.Outlined.LocationOn,
                    contentDescription = null,
                    tint = CruiseAzure,
                    modifier = Modifier.size(15.dp),
                )
                Spacer(Modifier.width(4.dp))
                Text(
                    text = facility.formattedLocation,
                    style = MaterialTheme.typography.bodyMedium,
                    fontWeight = FontWeight.SemiBold,
                    color = CruiseMarine,
                )
            }

            Spacer(Modifier.height(8.dp))

            // 运营时间行与状态
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.Top,
            ) {
                Row(
                    modifier = Modifier.weight(1f),
                    verticalAlignment = Alignment.Top,
                ) {
                    Icon(
                        imageVector = Icons.Outlined.AccessTime,
                        contentDescription = null,
                        tint = Slate500,
                        modifier = Modifier
                            .size(14.dp)
                            .padding(top = 2.dp),
                    )
                    Spacer(Modifier.width(4.dp))
                    Column {
                        when {
                            facility.is24Hours -> {
                                Text(
                                    text = stringResource(R.string.facility_guide_24_hours),
                                    style = MaterialTheme.typography.bodySmall,
                                    color = Slate700,
                                )
                            }
                            !facility.hasOperatingHours -> {
                                Text(
                                    text = stringResource(R.string.facility_guide_see_schedule),
                                    style = MaterialTheme.typography.bodySmall,
                                    color = Slate500,
                                )
                            }
                            else -> {
                                FlowRow(
                                    horizontalArrangement = Arrangement.spacedBy(8.dp),
                                    verticalArrangement = Arrangement.spacedBy(4.dp),
                                ) {
                                    facility.slots.forEach { slot ->
                                        val label = buildString {
                                            slot.meal?.let { append(it.label).append(" ") }
                                            append("%02d:%02d–%02d:%02d".format(slot.start.hour, slot.start.minute, slot.end.hour, slot.end.minute))
                                            if (slot.lateNight) append(" (夜宵)")
                                        }
                                        Text(
                                            text = label,
                                            style = MaterialTheme.typography.bodySmall,
                                            color = Slate700,
                                        )
                                    }
                                }
                            }
                        }
                    }
                }

                // 营业中/休息中 状态徽标
                if (facility.hasOperatingHours) {
                    Spacer(Modifier.width(8.dp))
                    Surface(
                        shape = RoundedCornerShape(10.dp),
                        color = if (facility.isOpenNow) Color(0xFFE6F4EA) else Slate100,
                    ) {
                        Text(
                            text = stringResource(
                                if (facility.isOpenNow) R.string.facility_guide_status_open
                                else R.string.facility_guide_status_closed
                            ),
                            style = MaterialTheme.typography.labelSmall,
                            fontWeight = FontWeight.Bold,
                            color = if (facility.isOpenNow) Color(0xFF137333) else Slate500,
                            modifier = Modifier.padding(horizontal = 7.dp, vertical = 3.dp),
                        )
                    }
                }
            }
        }
    }
}

package com.cruise.assistant.ui.deckmap

import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxHeight
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Clear
import androidx.compose.material.icons.filled.Search
import androidx.compose.material.icons.outlined.DoorSliding
import androidx.compose.material.icons.outlined.MeetingRoom
import androidx.compose.material.icons.outlined.Wc
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.ModalBottomSheet
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.OutlinedTextFieldDefaults
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.rememberModalBottomSheetState
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.focus.FocusRequester
import androidx.compose.ui.focus.focusRequester
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.cruise.assistant.R
import com.cruise.assistant.domain.DeckSearchResult
import com.cruise.assistant.domain.DeckSearchResultType
import com.cruise.assistant.domain.getAllFacilityItems
import com.cruise.assistant.domain.searchDeckMap
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
fun DeckMapSearchSheet(
    currentDeck: Int,
    onSelectResult: (deckNumber: Int, query: String) -> Unit,
    onDismiss: () -> Unit,
) {
    val sheetState = rememberModalBottomSheetState(skipPartiallyExpanded = true)
    var query by remember { mutableStateOf("") }
    val focusRequester = remember { FocusRequester() }
    val allFacilities = remember { getAllFacilityItems() }

    val results = remember(query, currentDeck) {
        searchDeckMap(query = query, currentDeck = currentDeck, facilities = allFacilities)
    }

    LaunchedEffect(Unit) {
        focusRequester.requestFocus()
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
                .fillMaxHeight(0.85f)
                .padding(horizontal = 16.dp, vertical = 14.dp),
        ) {
            // 顶部拉手条与标题
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically,
            ) {
                Text(
                    text = "全船地图检索",
                    style = MaterialTheme.typography.titleMedium,
                    fontWeight = FontWeight.Bold,
                    color = Slate900,
                )
                Text(
                    text = if (query.isEmpty()) "推荐位置" else "共 ${results.size} 条结果",
                    style = MaterialTheme.typography.labelSmall,
                    color = Slate500,
                )
            }

            Spacer(Modifier.height(10.dp))

            // 搜索输入框
            OutlinedTextField(
                value = query,
                onValueChange = { query = it },
                modifier = Modifier
                    .fillMaxWidth()
                    .focusRequester(focusRequester),
                placeholder = {
                    Text(
                        text = "搜索设施、房间号 (如 11770) 或 厕所...",
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
                    if (query.isNotEmpty()) {
                        IconButton(onClick = { query = "" }) {
                            Icon(
                                imageVector = Icons.Default.Clear,
                                contentDescription = "清除",
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
            )

            Spacer(Modifier.height(12.dp))

            // 结果列表
            if (results.isEmpty()) {
                Column(
                    modifier = Modifier
                        .fillMaxWidth()
                        .weight(1f)
                        .padding(top = 40.dp),
                    horizontalAlignment = Alignment.CenterHorizontally,
                ) {
                    Text(
                        text = "未找到相关设施或房间",
                        style = MaterialTheme.typography.bodyMedium,
                        color = Slate500,
                    )
                }
            } else {
                LazyColumn(
                    modifier = Modifier
                        .fillMaxWidth()
                        .weight(1f),
                    contentPadding = PaddingValues(vertical = 4.dp),
                    verticalArrangement = Arrangement.spacedBy(8.dp),
                ) {
                    items(results, key = { it.type.name + "_" + it.deckNumber + "_" + it.title }) { item ->
                        SearchResultCard(
                            result = item,
                            onClick = {
                                onSelectResult(item.deckNumber, item.highlightQuery)
                            },
                        )
                    }
                }
            }
        }
    }
}

@Composable
private fun SearchResultCard(
    result: DeckSearchResult,
    onClick: () -> Unit,
) {
    Surface(
        shape = RoundedCornerShape(12.dp),
        color = MaterialTheme.colorScheme.surface,
        border = BorderStroke(1.dp, Slate200),
        modifier = Modifier
            .fillMaxWidth()
            .clickable { onClick() },
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 14.dp, vertical = 12.dp),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.SpaceBetween,
        ) {
            Row(
                modifier = Modifier.weight(1f),
                verticalAlignment = Alignment.CenterVertically,
            ) {
                // 类型图标徽章
                Surface(
                    shape = RoundedCornerShape(8.dp),
                    color = when (result.type) {
                        DeckSearchResultType.STATEROOM -> CruiseAzureContainer
                        DeckSearchResultType.RESTROOM -> Slate100
                        DeckSearchResultType.FACILITY -> CruiseAzureContainer.copy(alpha = 0.6f)
                    },
                    modifier = Modifier.size(36.dp),
                ) {
                    Icon(
                        imageVector = resultIconFor(result),
                        contentDescription = null,
                        tint = when (result.type) {
                            DeckSearchResultType.STATEROOM -> CruiseMarine
                            DeckSearchResultType.RESTROOM -> Slate700
                            DeckSearchResultType.FACILITY -> CruiseMarine
                        },
                        modifier = Modifier
                            .padding(8.dp)
                            .size(20.dp),
                    )
                }

                Spacer(Modifier.width(12.dp))

                Column {
                    Text(
                        text = result.title,
                        style = MaterialTheme.typography.titleSmall,
                        fontWeight = FontWeight.Bold,
                        color = Slate900,
                        maxLines = 1,
                        overflow = TextOverflow.Ellipsis,
                    )
                    Spacer(Modifier.height(2.dp))
                    Text(
                        text = result.subtitle,
                        style = MaterialTheme.typography.bodySmall,
                        color = Slate500,
                        maxLines = 1,
                        overflow = TextOverflow.Ellipsis,
                    )
                }
            }

            Spacer(Modifier.width(8.dp))

            // 甲板标记 Chip
            Surface(
                shape = RoundedCornerShape(6.dp),
                color = CruiseAzureContainer,
            ) {
                Text(
                    text = "Deck ${result.deckNumber}",
                    style = MaterialTheme.typography.labelSmall,
                    fontWeight = FontWeight.Bold,
                    color = CruiseMarine,
                    modifier = Modifier.padding(horizontal = 8.dp, vertical = 4.dp),
                )
            }
        }
    }
}

private fun resultIconFor(result: DeckSearchResult): ImageVector {
    return when (result.type) {
        DeckSearchResultType.STATEROOM -> Icons.Outlined.MeetingRoom
        DeckSearchResultType.RESTROOM -> Icons.Outlined.Wc
        DeckSearchResultType.FACILITY -> iconFor(result.categoryKey ?: "meal")
    }
}

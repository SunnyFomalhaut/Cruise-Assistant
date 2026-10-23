package com.cruise.assistant.ui.pixiedust

import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxHeight
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.outlined.Close
import androidx.compose.material3.BottomSheetDefaults
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.ModalBottomSheet
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.rememberModalBottomSheetState
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import com.cruise.assistant.R
import com.cruise.assistant.ui.theme.Slate100
import com.cruise.assistant.ui.theme.Slate200
import com.cruise.assistant.ui.theme.Slate500
import com.cruise.assistant.ui.theme.Slate700
import com.cruise.assistant.ui.theme.Slate900

/**
 * 联动检视底部抽屉：按楼层归组展示全船仙尘互赠与回礼状态，支持一键跳转楼层地图。
 * 楼层默认全部收起（只显示概要），点击楼层头部展开/收起明细，允许多层同时展开。
 */
@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun LinkageViewSheet(
    state: PixieDustUiState,
    onNavigateToDeckMap: (deck: Int) -> Unit,
    onDismiss: () -> Unit,
) {
    val sheetState = rememberModalBottomSheetState(skipPartiallyExpanded = true)

    // 当前展开的楼层集合：默认空集 = 全部收起；ExpandedDecksSaver 持久化以在横屏重建后恢复
    var expandedDecks by rememberSaveable(stateSaver = ExpandedDecksSaver) {
        mutableStateOf(emptySet<Int>())
    }

    val summaryPairs = state.allRoomPairs.ifEmpty { state.roomPairs }
    val totalRooms = summaryPairs.size
    val unrepliedRooms = remember(summaryPairs) {
        summaryPairs.count { it.unrepliedReceived.isNotEmpty() }
    }
    val supplementRooms = remember(summaryPairs) {
        summaryPairs.count { it.unrepliedReceived.isEmpty() && it.received.size > it.given.size }
    }
    val completedRooms = remember(summaryPairs) {
        summaryPairs.count { it.unrepliedReceived.isEmpty() && it.given.size == it.received.size && it.given.isNotEmpty() }
    }
    val givenPendingRooms = remember(summaryPairs) {
        summaryPairs.count { it.unrepliedReceived.isEmpty() && it.given.size > it.received.size }
    }
    val groupedByDeck = remember(state.roomPairs) {
        state.roomPairs.groupBy { it.deck }
    }

    ModalBottomSheet(
        onDismissRequest = onDismiss,
        sheetState = sheetState,
        dragHandle = { BottomSheetDefaults.DragHandle() },
        shape = RoundedCornerShape(topStart = 20.dp, topEnd = 20.dp),
        containerColor = MaterialTheme.colorScheme.surface,
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .fillMaxHeight(0.9f)
                .padding(horizontal = 16.dp),
        ) {
            // 顶部标题与关闭按钮
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically,
            ) {
                Text(
                    text = stringResource(R.string.pixie_linkage_view),
                    style = MaterialTheme.typography.titleLarge,
                    fontWeight = FontWeight.Bold,
                    color = Slate900,
                )
                IconButton(onClick = onDismiss) {
                    Icon(
                        imageVector = Icons.Outlined.Close,
                        contentDescription = stringResource(R.string.pixie_cancel),
                        tint = Slate500,
                        modifier = Modifier.size(20.dp),
                    )
                }
            }

            Spacer(Modifier.height(8.dp))

            // 全船统计概览卡片
            Surface(
                modifier = Modifier.fillMaxWidth(),
                shape = RoundedCornerShape(12.dp),
                color = Slate100,
                border = BorderStroke(1.dp, Slate200),
            ) {
                Text(
                    text = stringResource(
                        R.string.pixie_linkage_summary_format,
                        totalRooms,
                        unrepliedRooms,
                        supplementRooms,
                        completedRooms,
                        givenPendingRooms,
                    ),
                    style = MaterialTheme.typography.bodyMedium,
                    fontWeight = FontWeight.Medium,
                    color = Slate700,
                    modifier = Modifier.padding(horizontal = 14.dp, vertical = 10.dp),
                )
            }

            Spacer(Modifier.height(14.dp))

            // 主列表内容
            if (state.roomPairs.isEmpty()) {
                Box(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(vertical = 48.dp),
                    contentAlignment = Alignment.Center,
                ) {
                    Text(
                        text = stringResource(R.string.pixie_linkage_empty),
                        style = MaterialTheme.typography.bodyMedium,
                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                    )
                }
            } else {
                LazyColumn(
                    modifier = Modifier.fillMaxWidth(),
                    verticalArrangement = Arrangement.spacedBy(14.dp),
                    contentPadding = PaddingValues(bottom = 24.dp),
                ) {
                    groupedByDeck.forEach { (deck, pairs) ->
                        item(key = deck) {
                            DeckSectionCard(
                                deck = deck,
                                pairs = pairs,
                                levels = state.levels,
                                onNavigateToDeckMap = onNavigateToDeckMap,
                                expanded = deck in expandedDecks,
                                onToggleExpanded = {
                                    expandedDecks = toggleDeckExpanded(expandedDecks, deck)
                                },
                            )
                        }
                    }
                }
            }
        }
    }
}

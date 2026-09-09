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
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.outlined.Close
import androidx.compose.material.icons.outlined.Map
import androidx.compose.material3.BottomSheetDefaults
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.FilledTonalButton
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.ModalBottomSheet
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.rememberModalBottomSheetState
import androidx.compose.runtime.Composable
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.cruise.assistant.R
import com.cruise.assistant.domain.PixieDustRecord
import com.cruise.assistant.domain.RoomPair
import com.cruise.assistant.domain.levelCountsOf
import com.cruise.assistant.ui.theme.Slate100
import com.cruise.assistant.ui.theme.Slate200
import com.cruise.assistant.ui.theme.Slate400
import com.cruise.assistant.ui.theme.Slate500
import com.cruise.assistant.ui.theme.Slate700
import com.cruise.assistant.ui.theme.Slate800
import com.cruise.assistant.ui.theme.Slate900

/**
 * 联动检视底部抽屉：按楼层归组展示全船仙尘互赠与回礼状态，支持一键跳转楼层地图。
 */
@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun LinkageViewSheet(
    state: PixieDustUiState,
    onNavigateToDeckMap: (deck: Int) -> Unit,
    onDismiss: () -> Unit,
) {
    val sheetState = rememberModalBottomSheetState(skipPartiallyExpanded = true)

    val totalRooms = state.roomPairs.size
    val unrepliedRooms = remember(state.roomPairs) {
        state.roomPairs.count { it.unrepliedReceived.isNotEmpty() }
    }
    val supplementRooms = remember(state.roomPairs) {
        state.roomPairs.count { it.unrepliedReceived.isEmpty() && it.received.size > it.given.size }
    }
    val completedRooms = remember(state.roomPairs) {
        state.roomPairs.count { it.unrepliedReceived.isEmpty() && it.given.size >= it.received.size }
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
                                state = state,
                                onNavigateToDeckMap = onNavigateToDeckMap,
                            )
                        }
                    }
                }
            }
        }
    }
}

/**
 * 楼层聚合卡片：展示楼层标题、统计徽标、地图跳转按钮及本层客房配对列表。
 */
@Composable
private fun DeckSectionCard(
    deck: Int,
    pairs: List<RoomPair>,
    state: PixieDustUiState,
    onNavigateToDeckMap: (deck: Int) -> Unit,
) {
    val deckUnreplied = remember(pairs) { pairs.count { it.unrepliedReceived.isNotEmpty() } }
    val deckSupplement = remember(pairs) {
        pairs.count { it.unrepliedReceived.isEmpty() && it.received.size > it.given.size }
    }
    val deckCompleted = remember(pairs) {
        pairs.count { it.unrepliedReceived.isEmpty() && it.given.size >= it.received.size }
    }

    Surface(
        modifier = Modifier.fillMaxWidth(),
        shape = RoundedCornerShape(14.dp),
        color = MaterialTheme.colorScheme.surface,
        border = BorderStroke(1.dp, Slate200),
    ) {
        Column(modifier = Modifier.fillMaxWidth()) {
            // 楼层头部
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(horizontal = 14.dp, vertical = 12.dp),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically,
            ) {
                Column(modifier = Modifier.weight(1f)) {
                    Text(
                        text = "Deck $deck",
                        style = MaterialTheme.typography.titleMedium,
                        fontWeight = FontWeight.Bold,
                        color = Slate900,
                    )
                    Spacer(Modifier.height(2.dp))
                    Text(
                        text = stringResource(
                            R.string.pixie_deck_summary_format,
                            deckUnreplied,
                            deckSupplement,
                            deckCompleted,
                        ),
                        style = MaterialTheme.typography.bodySmall,
                        color = Slate500,
                    )
                }

                Spacer(Modifier.width(8.dp))

                FilledTonalButton(
                    onClick = { onNavigateToDeckMap(deck) },
                    contentPadding = PaddingValues(horizontal = 12.dp, vertical = 6.dp),
                    shape = RoundedCornerShape(10.dp),
                ) {
                    Icon(
                        imageVector = Icons.Outlined.Map,
                        contentDescription = null,
                        modifier = Modifier.size(16.dp),
                    )
                    Spacer(Modifier.width(6.dp))
                    Text(
                        text = stringResource(R.string.pixie_linkage_view_map),
                        style = MaterialTheme.typography.labelMedium,
                        fontWeight = FontWeight.SemiBold,
                    )
                }
            }

            HorizontalDivider(color = Slate200)

            // 本层客房配对明细
            pairs.forEachIndexed { index, pair ->
                RoomPairRow(
                    state = state,
                    pair = pair,
                )
                if (index < pairs.size - 1) {
                    HorizontalDivider(
                        color = Slate100,
                        modifier = Modifier.padding(horizontal = 14.dp),
                    )
                }
            }
        }
    }
}

/**
 * 客房配对行：房号、三色状态胶囊及发出/收到明细对比。
 */
@Composable
private fun RoomPairRow(
    state: PixieDustUiState,
    pair: RoomPair,
) {
    Column(
        modifier = Modifier
            .fillMaxWidth()
            .padding(horizontal = 14.dp, vertical = 10.dp),
    ) {
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically,
        ) {
            Text(
                text = "${pair.deck}${pair.room}",
                style = MaterialTheme.typography.titleMedium,
                fontWeight = FontWeight.Bold,
                color = Slate800,
            )

            // 状态胶囊
            val (statusText, containerColor, contentColor, borderColor) = when {
                pair.unrepliedReceived.isNotEmpty() -> {
                    Quad(
                        "🔴 ${stringResource(R.string.pixie_status_unreplied)} (${pair.unrepliedReceived.size} 笔)",
                        Color(0xFFFEE2E2),
                        Color(0xFFDC2626),
                        Color(0xFFFECACA),
                    )
                }
                pair.received.size > pair.given.size -> {
                    val diff = pair.received.size - pair.given.size
                    Quad(
                        "🟡 ${stringResource(R.string.pixie_status_supplement)} (差额 $diff 笔)",
                        Color(0xFFFEF3C7),
                        Color(0xFFD97706),
                        Color(0xFFFDE68A),
                    )
                }
                else -> {
                    Quad(
                        "🟢 ${stringResource(R.string.pixie_status_completed)}",
                        Color(0xFFD1FAE5),
                        Color(0xFF059669),
                        Color(0xFFA7F3D0),
                    )
                }
            }

            Surface(
                shape = RoundedCornerShape(12.dp),
                color = containerColor,
                border = BorderStroke(1.dp, borderColor),
            ) {
                Text(
                    text = statusText,
                    style = MaterialTheme.typography.labelSmall,
                    fontWeight = FontWeight.Medium,
                    color = contentColor,
                    modifier = Modifier.padding(horizontal = 8.dp, vertical = 3.dp),
                )
            }
        }

        Spacer(Modifier.height(6.dp))

        // 发出 ↔ 收到对比
        Row(
            modifier = Modifier.fillMaxWidth(),
            verticalAlignment = Alignment.CenterVertically,
        ) {
            Column(modifier = Modifier.weight(1f)) {
                Text(
                    text = stringResource(R.string.pixie_linkage_given_label, pair.given.size),
                    style = MaterialTheme.typography.labelMedium,
                    fontWeight = FontWeight.SemiBold,
                    color = Slate700,
                )
                Text(
                    text = pair.levelCountsText(state, pair.given),
                    style = MaterialTheme.typography.bodySmall,
                    color = Slate500,
                )
            }

            Text(
                text = "↔",
                fontSize = 16.sp,
                color = Slate400,
                modifier = Modifier.padding(horizontal = 8.dp),
            )

            Column(modifier = Modifier.weight(1f)) {
                Text(
                    text = stringResource(R.string.pixie_linkage_received_label, pair.received.size),
                    style = MaterialTheme.typography.labelMedium,
                    fontWeight = FontWeight.SemiBold,
                    color = Slate700,
                )
                Text(
                    text = pair.levelCountsText(state, pair.received),
                    style = MaterialTheme.typography.bodySmall,
                    color = Slate500,
                )
            }
        }
    }
}

private data class Quad<A, B, C, D>(
    val first: A,
    val second: B,
    val third: C,
    val fourth: D,
)

private fun RoomPair.levelCountsText(
    state: PixieDustUiState,
    records: List<PixieDustRecord>,
): String =
    levelCountsOf(records, state.levels).joinToString("  ") { "${it.level.name}×${it.count}" }
        .ifEmpty { "—" }

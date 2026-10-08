package com.cruise.assistant.ui.pixiedust

import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.outlined.ExpandMore
import androidx.compose.material.icons.outlined.Map
import androidx.compose.material3.FilledTonalButton
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.remember
import androidx.compose.runtime.saveable.Saver
import androidx.compose.runtime.saveable.listSaver
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.rotate
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.cruise.assistant.R
import com.cruise.assistant.domain.PixieDustRecord
import com.cruise.assistant.domain.PixieLevel
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
 * 联动检视的共享 UI 组件：楼层聚合卡片、客房配对行及其辅助类型。
 * 独立成文件以便「航次存档」等后续视图复用（DeckSectionCard 传 showMapButton = false 即可隐藏地图入口）。
 */

/** 展开楼层集合的 Saver：Set<Int> ↔ List<Int>，横屏/配置重建后恢复各层展开状态 */
internal val ExpandedDecksSaver: Saver<Set<Int>, Any> = listSaver(
    save = { it.toList() },
    restore = { it.toSet() },
)

/** 纯逻辑：切换指定楼层的展开/收起状态。各层互不影响，允许多层同时展开 */
internal fun toggleDeckExpanded(expanded: Set<Int>, deck: Int): Set<Int> =
    if (deck in expanded) expanded - deck else expanded + deck

/**
 * 楼层聚合卡片：展示楼层标题、统计徽标、地图跳转按钮及本层客房配对列表。
 * 头部整行可点击切换展开/收起；收起时只渲染头部（标题 + 摘要 + 地图按钮 + 箭头），
 * 「查看本层地图」按钮位于头部内，收起状态下仍可直接点击（子级按钮优先消费点击，不会触发折叠）。
 */
@Composable
internal fun DeckSectionCard(
    deck: Int,
    pairs: List<RoomPair>,
    levels: List<PixieLevel>,
    onNavigateToDeckMap: (deck: Int) -> Unit,
    expanded: Boolean,
    onToggleExpanded: () -> Unit,
    showMapButton: Boolean = true,
) {
    val deckUnreplied = remember(pairs) { pairs.count { it.unrepliedReceived.isNotEmpty() } }
    val deckSupplement = remember(pairs) {
        pairs.count { it.unrepliedReceived.isEmpty() && it.received.size > it.given.size }
    }
    val deckCompleted = remember(pairs) {
        pairs.count { it.unrepliedReceived.isEmpty() && it.given.size == it.received.size && it.given.isNotEmpty() }
    }
    val deckGivenPending = remember(pairs) {
        pairs.count { it.unrepliedReceived.isEmpty() && it.given.size > it.received.size }
    }
    // 展开指示箭头：收起朝下（ExpandMore 原始方向），展开旋转 180° 朝上
    val arrowRotation by animateFloatAsState(
        targetValue = if (expanded) 180f else 0f,
        label = "deckExpandArrowRotation",
    )
    val toggleLabel = stringResource(
        if (expanded) R.string.pixie_linkage_deck_collapse else R.string.pixie_linkage_deck_expand
    )
    Surface(
        modifier = Modifier.fillMaxWidth(),
        shape = RoundedCornerShape(14.dp),
        color = MaterialTheme.colorScheme.surface,
        border = BorderStroke(1.dp, Slate200),
    ) {
        Column(modifier = Modifier.fillMaxWidth()) {
            // 楼层头部：整行可点击切换展开/收起（clickable 放在 padding 前扩大触控区）
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .clickable(onClickLabel = toggleLabel, onClick = onToggleExpanded)
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
                            deckGivenPending,
                        ),
                        style = MaterialTheme.typography.bodySmall,
                        color = Slate500,
                    )
                }

                if (showMapButton) {
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

                Spacer(Modifier.width(4.dp))

                // 展开/收起指示箭头
                Icon(
                    imageVector = Icons.Outlined.ExpandMore,
                    contentDescription = toggleLabel,
                    tint = Slate500,
                    modifier = Modifier
                        .size(22.dp)
                        .rotate(arrowRotation),
                )
            }

            // 收起时不渲染分隔线与房间明细，只保留头部概要
            if (expanded) {
                HorizontalDivider(color = Slate200)

                // 本层客房配对明细
                pairs.forEachIndexed { index, pair ->
                    RoomPairRow(
                        levels = levels,
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
}

/**
 * 客房配对行：房号、三色状态胶囊及发出/收到明细对比。
 */
@Composable
internal fun RoomPairRow(
    levels: List<PixieLevel>,
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
                pair.given.size > pair.received.size -> {
                    val label = if (pair.received.isEmpty()) {
                        "🔵 ${stringResource(R.string.pixie_status_given_pending)}"
                    } else {
                        val diff = pair.given.size - pair.received.size
                        "🔵 超发 $diff 笔"
                    }
                    Quad(
                        label,
                        Color(0xFFDBEAFE),
                        Color(0xFF1D4ED8),
                        Color(0xFFBFDBFE),
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
                    text = pair.levelCountsText(levels, pair.given),
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
                    text = pair.levelCountsText(levels, pair.received),
                    style = MaterialTheme.typography.bodySmall,
                    color = Slate500,
                )
            }
        }
    }
}

/** 状态胶囊的四元组：文案、底色、内容色、描边色 */
internal data class Quad<A, B, C, D>(
    val first: A,
    val second: B,
    val third: C,
    val fourth: D,
)

/** 等级计数摘要："等级A×2  等级B×1"，空列表显示占位符 "—" */
internal fun RoomPair.levelCountsText(
    levels: List<PixieLevel>,
    records: List<PixieDustRecord>,
): String =
    levelCountsOf(records, levels).joinToString("  ") { "${it.level.name}×${it.count}" }
        .ifEmpty { "—" }

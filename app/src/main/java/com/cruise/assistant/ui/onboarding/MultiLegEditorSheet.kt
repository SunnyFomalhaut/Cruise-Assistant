@file:OptIn(androidx.compose.material3.ExperimentalMaterial3Api::class)

package com.cruise.assistant.ui.onboarding

import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.itemsIndexed
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.outlined.Close
import androidx.compose.material3.BottomSheetDefaults
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.FilterChip
import androidx.compose.material3.FilterChipDefaults
import androidx.compose.material3.FilledTonalButton
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.ModalBottomSheet
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.material3.rememberModalBottomSheetState
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import com.cruise.assistant.R
import com.cruise.assistant.ui.theme.CruiseAzureContainer
import com.cruise.assistant.ui.theme.CruiseMarine
import com.cruise.assistant.ui.theme.Slate100
import com.cruise.assistant.ui.theme.Slate200
import com.cruise.assistant.ui.theme.Slate500
import com.cruise.assistant.ui.theme.Slate700
import com.cruise.assistant.ui.theme.Slate900

/**
 * 联程编排底部弹层：班次只有 3夜（周一登船）/4夜（周四登船）两种，链强制交替
 *（3+4+3+4…或 4+3+4+3…），仅可从末尾追加/缩短。第 1 程固定为所选船期晚数（不可删），
 * 后续程只有最后一程可删。链中存在 4夜程时展示「登岸设置」小节，按程勾选兰卡威登岸。
 */
@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun MultiLegEditorSheet(
    state: OnboardingState,
    onAddLeg: (Int) -> Unit,
    onRemoveLeg: (Int) -> Unit,
    onClearLegs: () -> Unit,
    onToggleAshore: (Int) -> Unit,
    onDismiss: () -> Unit,
) {
    val sheetState = rememberModalBottomSheetState(skipPartiallyExpanded = true)

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
                .padding(horizontal = 16.dp),
        ) {
            // 顶部标题与关闭按钮
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically,
            ) {
                Text(
                    text = stringResource(R.string.multileg_editor_title),
                    style = MaterialTheme.typography.titleLarge,
                    fontWeight = FontWeight.Bold,
                    color = Slate900,
                )
                IconButton(onClick = onDismiss) {
                    Icon(
                        imageVector = Icons.Outlined.Close,
                        contentDescription = stringResource(R.string.cancel),
                        tint = Slate500,
                        modifier = Modifier.size(20.dp),
                    )
                }
            }

            Spacer(Modifier.height(12.dp))

            // 联程链展示：第1程固定（所选船期晚数）+ 各后续程（仅最后一程可删——删中间等价于从末尾缩短）
            LazyRow(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(8.dp),
            ) {
                state.scheduleNights?.let { first ->
                    item(key = "first") {
                        LegChip(
                            index = 1,
                            nights = first,
                            removable = false,
                            onRemove = {},
                        )
                    }
                }
                itemsIndexed(
                    state.extraLegs,
                    key = { index, _ -> index },
                ) { index, nights ->
                    LegChip(
                        index = index + 2, // 第1程固定占首位，后续程从第2程起
                        nights = nights,
                        removable = index == state.extraLegs.lastIndex,
                        onRemove = { onRemoveLeg(index) },
                    )
                }
            }
            if (state.extraLegs.isEmpty()) {
                // 空态（只有第1程）：引导追加
                Spacer(Modifier.height(8.dp))
                Text(
                    text = stringResource(R.string.multileg_editor_empty_hint),
                    style = MaterialTheme.typography.bodySmall,
                    color = Slate500,
                )
            } else {
                // 交替规则说明：班次只有 3夜/4夜，链只能交替拼接、从末尾缩短
                Spacer(Modifier.height(8.dp))
                Text(
                    text = stringResource(R.string.multileg_alternate_hint),
                    style = MaterialTheme.typography.bodySmall,
                    color = Slate500,
                )
            }

            Spacer(Modifier.height(12.dp))

            // 追加按钮：交替规则给出的唯一下一程晚数（nextLegNights 为 null 不渲染）
            state.nextLegNights?.let { nights ->
                FilledTonalButton(
                    onClick = { onAddLeg(nights) },
                    shape = RoundedCornerShape(12.dp),
                ) {
                    Text(
                        text = stringResource(R.string.multileg_add_leg_format, nights),
                        fontWeight = FontWeight.SemiBold,
                    )
                }
            }

            // 登岸设置：链中存在 4夜程时按程勾选兰卡威登岸（仅 4夜程第三天靠港）
            // 单程（legs 空）时全链即首程船期晚数
            val legChain = state.legs.ifEmpty { listOfNotNull(state.scheduleNights) }
            val fourNightIndices = legChain.withIndex().filter { it.value == 4 }.map { it.index }
            if (fourNightIndices.isNotEmpty()) {
                Spacer(Modifier.height(12.dp))
                Text(
                    text = stringResource(R.string.ashore_section_title),
                    style = MaterialTheme.typography.titleSmall,
                    color = Slate700,
                )
                Spacer(Modifier.height(8.dp))
                fourNightIndices.forEach { legIndex ->
                    FilterChip(
                        selected = legIndex in state.ashoreLegs,
                        onClick = { onToggleAshore(legIndex) },
                        label = {
                            Text(
                                stringResource(R.string.ashore_leg_chip_format, legIndex + 1),
                                style = MaterialTheme.typography.labelLarge,
                            )
                        },
                        colors = FilterChipDefaults.filterChipColors(
                            selectedContainerColor = CruiseAzureContainer,
                            selectedLabelColor = CruiseMarine,
                        ),
                    )
                }
            }

            Spacer(Modifier.height(12.dp))

            // 实时摘要：随加程/删程即时刷新
            state.multiLegSummary?.let { summary ->
                Text(
                    text = stringResource(
                        R.string.multileg_summary_format,
                        summary.legsLabel,
                        summary.totalNights,
                        summary.boardDow,
                        summary.disembarkDow,
                    ),
                    style = MaterialTheme.typography.bodySmall,
                    fontWeight = FontWeight.Medium,
                    color = CruiseMarine,
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(vertical = 6.dp),
                )
            }

            // 恢复单程：仅在有后续程时显示
            if (state.extraLegs.isNotEmpty()) {
                TextButton(onClick = onClearLegs) {
                    Text(
                        text = stringResource(R.string.multileg_restore_single),
                        style = MaterialTheme.typography.labelLarge,
                        color = MaterialTheme.colorScheme.error,
                    )
                }
            }

            Spacer(Modifier.height(24.dp))
        }
    }
}

/** 单个程段 chip：第1程高亮不可删，后续程带小删除图标 */
@Composable
private fun LegChip(
    index: Int,
    nights: Int,
    removable: Boolean,
    onRemove: () -> Unit,
) {
    Surface(
        shape = RoundedCornerShape(10.dp),
        color = if (removable) Slate100 else CruiseAzureContainer,
        border = if (removable) BorderStroke(1.dp, Slate200) else null,
    ) {
        Row(
            verticalAlignment = Alignment.CenterVertically,
            modifier = Modifier.padding(
                start = 12.dp,
                end = if (removable) 2.dp else 12.dp,
                top = 8.dp,
                bottom = 8.dp,
            ),
        ) {
            Text(
                text = stringResource(R.string.multileg_leg_chip_format, index, nights),
                style = MaterialTheme.typography.labelLarge,
                fontWeight = FontWeight.SemiBold,
                color = if (removable) Slate700 else CruiseMarine,
            )
            if (removable) {
                IconButton(onClick = onRemove, modifier = Modifier.size(26.dp)) {
                    Icon(
                        imageVector = Icons.Outlined.Close,
                        contentDescription = null,
                        tint = Slate500,
                        modifier = Modifier.size(14.dp),
                    )
                }
            }
        }
    }
}

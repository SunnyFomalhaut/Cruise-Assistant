package com.cruise.assistant.ui.pixiedust

import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.outlined.Link
import androidx.compose.material.icons.outlined.Settings
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.SecondaryTabRow
import androidx.compose.material3.Surface
import androidx.compose.material3.Tab
import androidx.compose.material3.TabRowDefaults
import androidx.compose.material3.Text
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
import com.cruise.assistant.domain.LevelCount
import com.cruise.assistant.domain.levelCountsOf
import com.cruise.assistant.ui.theme.CruiseAzure
import com.cruise.assistant.ui.theme.CruiseAzureContainer
import com.cruise.assistant.ui.theme.CruiseMarine
import com.cruise.assistant.ui.theme.Slate100
import com.cruise.assistant.ui.theme.Slate200
import com.cruise.assistant.ui.theme.Slate500
import com.cruise.assistant.ui.theme.Slate700
import com.cruise.assistant.ui.theme.Slate900

/** 顶栏（返回/tab/齿轮）+ 分等级统计条。内容区由 content 槽位填充。 */
@Composable
fun PixieDustScreen(
    state: PixieDustUiState?,
    onBack: () -> Unit,
    onTabChange: (PixieTab) -> Unit,
    onOpenLevelManager: () -> Unit,
    onOpenLinkage: () -> Unit,
    content: @Composable (PixieDustUiState) -> Unit,
) {
    if (state == null) {
        Box(Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
            CircularProgressIndicator(color = CruiseAzure)
        }
        return
    }
    Column(Modifier.fillMaxSize().background(MaterialTheme.colorScheme.background)) {
        // ---- 顶栏：返回 / 双 tab / 等级管理齿轮 ----
        Row(
            Modifier
                .fillMaxWidth()
                .padding(horizontal = 8.dp, vertical = 4.dp),
            verticalAlignment = Alignment.CenterVertically,
        ) {
            IconButton(
                onClick = onBack,
                modifier = Modifier
                    .size(38.dp)
                    .clip(CircleShape)
                    .background(Slate100),
            ) {
                Icon(
                    Icons.AutoMirrored.Filled.ArrowBack,
                    contentDescription = stringResource(R.string.pixie_back),
                    tint = Slate900,
                    modifier = Modifier.size(20.dp),
                )
            }

            Spacer(Modifier.width(8.dp))

            SecondaryTabRow(
                selectedTabIndex = if (state.tab == PixieTab.GIVEN) 0 else 1,
                modifier = Modifier
                    .weight(1f)
                    .clip(RoundedCornerShape(14.dp)),
                containerColor = Slate100,
                indicator = {
                    TabRowDefaults.SecondaryIndicator(
                        modifier = Modifier.tabIndicatorOffset(if (state.tab == PixieTab.GIVEN) 0 else 1),
                        color = CruiseAzure,
                        height = 3.dp,
                    )
                },
                divider = {},
            ) {
                Tab(
                    selected = state.tab == PixieTab.GIVEN,
                    onClick = { if (state.tab != PixieTab.GIVEN) onTabChange(PixieTab.GIVEN) },
                    text = {
                        Text(
                            stringResource(R.string.pixie_tab_given),
                            fontWeight = if (state.tab == PixieTab.GIVEN) FontWeight.Bold else FontWeight.Normal,
                            color = if (state.tab == PixieTab.GIVEN) CruiseMarine else Slate500,
                        )
                    },
                )
                Tab(
                    selected = state.tab == PixieTab.RECEIVED,
                    onClick = { if (state.tab != PixieTab.RECEIVED) onTabChange(PixieTab.RECEIVED) },
                    text = {
                        Text(
                            stringResource(R.string.pixie_tab_received),
                            fontWeight = if (state.tab == PixieTab.RECEIVED) FontWeight.Bold else FontWeight.Normal,
                            color = if (state.tab == PixieTab.RECEIVED) CruiseMarine else Slate500,
                        )
                    },
                )
            }

            Spacer(Modifier.width(8.dp))

            IconButton(
                onClick = onOpenLevelManager,
                modifier = Modifier
                    .size(38.dp)
                    .clip(CircleShape)
                    .background(Slate100),
            ) {
                Icon(
                    Icons.Outlined.Settings,
                    contentDescription = stringResource(R.string.pixie_manage_levels),
                    tint = Slate700,
                    modifier = Modifier.size(20.dp),
                )
            }
        }

        // ---- 统计条 ----
        PixieStatBar(state, onOpenLinkage)

        // ---- 内容（键盘 + 列表）----
        content(state)
    }
}

/** 发出tab：发出总数+分等级；收到tab：收到总数+待回礼数（未回礼分等级，贵重在前）。末尾联动检视入口。 */
@Composable
private fun PixieStatBar(state: PixieDustUiState, onOpenLinkage: () -> Unit) {
    Surface(
        shape = RoundedCornerShape(12.dp),
        color = MaterialTheme.colorScheme.surface,
        border = androidx.compose.foundation.BorderStroke(1.dp, Slate200),
        modifier = Modifier
            .fillMaxWidth()
            .padding(horizontal = 16.dp, vertical = 6.dp),
    ) {
        Row(
            Modifier
                .fillMaxWidth()
                .padding(horizontal = 12.dp, vertical = 8.dp),
            verticalAlignment = Alignment.CenterVertically,
        ) {
            if (state.tab == PixieTab.GIVEN) {
                Text(
                    stringResource(R.string.pixie_given_count_format, state.given.size),
                    style = MaterialTheme.typography.titleSmall,
                    fontWeight = FontWeight.Bold,
                    color = Slate900,
                )
                LevelCountChips(levelCountsOf(state.given, state.levels))
            } else {
                val allReceived = state.allReceived.ifEmpty { state.received }
                val unreplied = allReceived.count { !it.replied }
                Text(
                    stringResource(R.string.pixie_received_count_format, allReceived.size) +
                        " · " +
                        stringResource(R.string.pixie_unreplied_count_format, unreplied),
                    style = MaterialTheme.typography.titleSmall,
                    fontWeight = FontWeight.Bold,
                    color = Slate900,
                )
                LevelCountChips(levelCountsOf(allReceived.filter { !it.replied }, state.levels))
            }
            Spacer(Modifier.weight(1f))
            IconButton(
                onClick = onOpenLinkage,
                modifier = Modifier
                    .size(32.dp)
                    .clip(CircleShape)
                    .background(CruiseAzureContainer),
            ) {
                Icon(
                    Icons.Outlined.Link,
                    contentDescription = stringResource(R.string.pixie_linkage_view),
                    tint = CruiseMarine,
                    modifier = Modifier.size(16.dp),
                )
            }
        }
    }
}

/** "● 3 ● 1" 分等级计数小条（圆点用等级色） */
@Composable
internal fun LevelCountChips(counts: List<LevelCount>) {
    counts.forEach { c ->
        Spacer(Modifier.width(8.dp))
        Row(
            verticalAlignment = Alignment.CenterVertically,
            modifier = Modifier
                .clip(RoundedCornerShape(6.dp))
                .background(Slate100)
                .padding(horizontal = 5.dp, vertical = 2.dp),
        ) {
            Surface(
                shape = CircleShape,
                color = Color(c.level.colorArgb),
                modifier = Modifier.size(8.dp),
            ) {}
            Spacer(Modifier.width(4.dp))
            Text(
                "${c.count}",
                style = MaterialTheme.typography.labelSmall,
                fontWeight = FontWeight.Bold,
                color = Slate700,
            )
        }
    }
}


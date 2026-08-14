package com.cruise.assistant.ui.pixiedust

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
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.outlined.Settings
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Tab
import androidx.compose.material3.TabRow
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.unit.dp
import com.cruise.assistant.R
import com.cruise.assistant.domain.LevelCount
import com.cruise.assistant.domain.levelCountsOf

/** 顶栏（返回/tab/齿轮）+ 分等级统计条。内容区由 content 槽位填充。 */
@Composable
fun PixieDustScreen(
    state: PixieDustUiState?,
    onBack: () -> Unit,
    onTabChange: (PixieTab) -> Unit,
    onOpenLevelManager: () -> Unit,
    content: @Composable (PixieDustUiState) -> Unit,
) {
    if (state == null) {
        Box(Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
            CircularProgressIndicator()
        }
        return
    }
    Column(Modifier.fillMaxSize()) {
        // ---- 顶栏：返回 / 双 tab / 等级管理齿轮 ----
        Row(
            Modifier.fillMaxWidth().padding(horizontal = 4.dp),
            verticalAlignment = Alignment.CenterVertically,
        ) {
            IconButton(onClick = onBack) {
                Icon(Icons.AutoMirrored.Filled.ArrowBack, contentDescription = stringResource(R.string.pixie_back))
            }
            TabRow(
                selectedTabIndex = if (state.tab == PixieTab.GIVEN) 0 else 1,
                modifier = Modifier.weight(1f),
            ) {
                Tab(
                    selected = state.tab == PixieTab.GIVEN,
                    onClick = { if (state.tab != PixieTab.GIVEN) onTabChange(PixieTab.GIVEN) },
                    text = { Text(stringResource(R.string.pixie_tab_given)) },
                )
                Tab(
                    selected = state.tab == PixieTab.RECEIVED,
                    onClick = { if (state.tab != PixieTab.RECEIVED) onTabChange(PixieTab.RECEIVED) },
                    text = { Text(stringResource(R.string.pixie_tab_received)) },
                )
            }
            IconButton(onClick = onOpenLevelManager) {
                Icon(Icons.Outlined.Settings, contentDescription = stringResource(R.string.pixie_manage_levels))
            }
        }

        // ---- 统计条 ----
        PixieStatBar(state)

        // ---- 内容（键盘 + 列表）----
        content(state)
    }
}

/** 发出tab：发出总数+分等级；收到tab：收到总数+待回礼数（未回礼分等级，贵重在前） */
@Composable
private fun PixieStatBar(state: PixieDustUiState) {
    Row(
        Modifier.fillMaxWidth().padding(horizontal = 16.dp, vertical = 6.dp),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        if (state.tab == PixieTab.GIVEN) {
            Text(
                stringResource(R.string.pixie_given_count_format, state.given.size),
                style = MaterialTheme.typography.titleSmall,
            )
            LevelCountChips(levelCountsOf(state.given, state.levels))
        } else {
            val unreplied = state.received.count { !it.replied }
            Text(
                stringResource(R.string.pixie_received_count_format, state.received.size) +
                    " · " +
                    stringResource(R.string.pixie_unreplied_count_format, unreplied),
                style = MaterialTheme.typography.titleSmall,
            )
            LevelCountChips(levelCountsOf(state.received.filter { !it.replied }, state.levels))
        }
    }
}

/** "● 3 ● 1" 分等级计数小条（圆点用等级色） */
@Composable
internal fun LevelCountChips(counts: List<LevelCount>) {
    counts.forEach { c ->
        Spacer(Modifier.width(8.dp))
        Row(verticalAlignment = Alignment.CenterVertically) {
            Surface(
                shape = CircleShape,
                color = Color(c.level.colorArgb),
                modifier = Modifier.size(8.dp),
            ) {}
            Spacer(Modifier.width(3.dp))
            Text(
                "${c.count}",
                style = MaterialTheme.typography.titleSmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
            )
        }
    }
}

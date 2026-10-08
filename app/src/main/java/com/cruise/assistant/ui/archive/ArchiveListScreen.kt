package com.cruise.assistant.ui.archive

import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.outlined.Delete
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.material3.TopAppBar
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.cruise.assistant.R
import com.cruise.assistant.data.db.entity.VoyageArchiveMeta
import com.cruise.assistant.ui.theme.CruiseMarine
import com.cruise.assistant.ui.theme.Slate100
import com.cruise.assistant.ui.theme.Slate200
import com.cruise.assistant.ui.theme.Slate500
import com.cruise.assistant.ui.theme.Slate700
import com.cruise.assistant.ui.theme.Slate900
import java.time.Instant
import java.time.ZoneId
import java.time.format.DateTimeFormatter

/** 列表日期范围与存档时间的统一格式（2026-10-06 / 2026-10-06 12:00） */
private val ARCHIVE_DATE_FMT = DateTimeFormatter.ofPattern("yyyy-MM-dd")
private val ARCHIVE_DATETIME_FMT = DateTimeFormatter.ofPattern("yyyy-MM-dd HH:mm")

/**
 * 航次存档列表：船名 + 日期范围 + 晚数摘要 + 存档时间，点击进只读回放详情，
 * 卡片尾部删除图标带二次确认。
 */
@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun ArchiveListScreen(
    state: ArchiveListUiState,
    onBack: () -> Unit,
    onOpenArchive: (Long) -> Unit,
    onRequestDelete: (Long) -> Unit,
    onConfirmDelete: (Long) -> Unit,
    onCancelDelete: () -> Unit,
) {
    Scaffold(
        topBar = {
            TopAppBar(
                title = {
                    Text(
                        stringResource(R.string.archive_list_title),
                        fontWeight = FontWeight.Bold,
                        color = Slate900,
                    )
                },
                navigationIcon = {
                    IconButton(onClick = onBack) {
                        Icon(
                            Icons.AutoMirrored.Filled.ArrowBack,
                            contentDescription = stringResource(R.string.pixie_back),
                        )
                    }
                },
            )
        },
    ) { padding ->
        if (state.archives.isEmpty()) {
            // 空态：居中提示
            Box(
                Modifier
                    .padding(padding)
                    .fillMaxSize(),
                contentAlignment = Alignment.Center,
            ) {
                Column(horizontalAlignment = Alignment.CenterHorizontally) {
                    Text("🗄️", fontSize = 32.sp)
                    Spacer(Modifier.height(8.dp))
                    Text(
                        stringResource(R.string.archive_empty),
                        style = MaterialTheme.typography.bodyMedium,
                        color = Slate500,
                    )
                }
            }
        } else {
            LazyColumn(
                Modifier
                    .padding(padding)
                    .fillMaxSize(),
                contentPadding = PaddingValues(16.dp),
                verticalArrangement = Arrangement.spacedBy(12.dp),
            ) {
                items(state.archives, key = { it.id }) { meta ->
                    ArchiveCard(
                        meta = meta,
                        onClick = { onOpenArchive(meta.id) },
                        onDelete = { onRequestDelete(meta.id) },
                    )
                }
            }
        }
    }

    // 删除二次确认（删除确认中 id 非空时弹出）
    state.pendingDeleteId?.let { targetId ->
        AlertDialog(
            onDismissRequest = onCancelDelete,
            title = {
                Text(stringResource(R.string.archive_delete_confirm_title), fontWeight = FontWeight.Bold)
            },
            text = { Text(stringResource(R.string.archive_delete_confirm_text)) },
            confirmButton = {
                TextButton(onClick = { onConfirmDelete(targetId) }) {
                    Text(stringResource(R.string.confirm), color = MaterialTheme.colorScheme.error)
                }
            },
            dismissButton = {
                TextButton(onClick = onCancelDelete) {
                    Text(stringResource(R.string.cancel), color = Slate500)
                }
            },
        )
    }
}

@Composable
private fun ArchiveCard(
    meta: VoyageArchiveMeta,
    onClick: () -> Unit,
    onDelete: () -> Unit,
) {
    Surface(
        shape = RoundedCornerShape(16.dp),
        color = MaterialTheme.colorScheme.surface,
        border = BorderStroke(1.dp, Slate200),
        modifier = Modifier
            .fillMaxWidth()
            .clickable(onClick = onClick),
    ) {
        Row(
            Modifier.padding(horizontal = 14.dp, vertical = 12.dp),
            verticalAlignment = Alignment.CenterVertically,
        ) {
            Column(Modifier.weight(1f)) {
                Text(
                    // 船名留空显示"自定义航次"
                    text = meta.shipName?.ifEmpty { null }
                        ?: stringResource(R.string.archive_custom_voyage),
                    style = MaterialTheme.typography.titleMedium,
                    fontWeight = FontWeight.Bold,
                    color = Slate900,
                )
                Spacer(Modifier.height(2.dp))
                Text(
                    text = stringResource(
                        R.string.archive_date_range_format,
                        meta.boardDate.format(ARCHIVE_DATE_FMT),
                        meta.disembarkDate.format(ARCHIVE_DATE_FMT),
                    ),
                    style = MaterialTheme.typography.bodySmall,
                    color = Slate700,
                )
                Spacer(Modifier.height(2.dp))
                Text(
                    text = meta.nightsSummary,
                    style = MaterialTheme.typography.bodySmall,
                    color = Slate500,
                )
                Spacer(Modifier.height(2.dp))
                Surface(
                    shape = RoundedCornerShape(6.dp),
                    color = Slate100,
                ) {
                    Text(
                        text = stringResource(
                            R.string.archive_created_at_format,
                            Instant.ofEpochMilli(meta.createdAt)
                                .atZone(ZoneId.systemDefault())
                                .format(ARCHIVE_DATETIME_FMT),
                        ),
                        style = MaterialTheme.typography.labelSmall,
                        fontSize = 10.sp,
                        color = CruiseMarine,
                        modifier = Modifier.padding(horizontal = 6.dp, vertical = 2.dp),
                    )
                }
            }
            // 尾部删除入口（点击后在上方弹二次确认）
            IconButton(onClick = onDelete) {
                Icon(
                    Icons.Outlined.Delete,
                    contentDescription = stringResource(R.string.delete),
                    tint = Slate500,
                    modifier = Modifier.size(20.dp),
                )
            }
        }
    }
}

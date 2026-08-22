package com.cruise.assistant.ui.timeline

import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.outlined.Close
import androidx.compose.material.icons.outlined.LocationOn
import androidx.compose.material.icons.outlined.Map
import androidx.compose.material.icons.outlined.Schedule
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.FilledTonalButton
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.drawWithContent
import androidx.compose.ui.graphics.BlendMode
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.CompositingStrategy
import androidx.compose.ui.graphics.asImageBitmap
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.ui.window.Dialog
import com.cruise.assistant.domain.Category
import com.cruise.assistant.domain.CruiseEvent
import com.cruise.assistant.domain.PosterManager
import com.cruise.assistant.domain.effectiveWaitMinutes
import com.cruise.assistant.domain.formatLocation
import com.cruise.assistant.domain.formatWaitStart
import com.cruise.assistant.ui.common.CategoryChip
import com.cruise.assistant.ui.theme.CruiseAzure
import com.cruise.assistant.ui.theme.CruiseAzureContainer
import com.cruise.assistant.ui.theme.CruiseMarine
import com.cruise.assistant.ui.theme.Slate100
import com.cruise.assistant.ui.theme.Slate200
import com.cruise.assistant.ui.theme.Slate500
import com.cruise.assistant.ui.theme.Slate700
import com.cruise.assistant.ui.theme.Slate900

@Composable
fun EventDetailDialog(
    event: CruiseEvent,
    category: Category?,
    onNavigateToMap: (deck: Int, target: String) -> Unit,
    onDismiss: () -> Unit,
) {
    val fullLocation = formatLocation(event.deck, event.zone, event.placeName, compact = false)

    val endTime = event.startTime.plusMinutes(event.durationMinutes.toLong())
    val startStr = "%02d:%02d".format(event.startTime.hour, event.startTime.minute)
    val endStr = "%02d:%02d".format(endTime.hour, endTime.minute)
    val durHours = event.durationMinutes / 60
    val durMins = event.durationMinutes % 60
    val durationStr = when {
        durHours > 0 && durMins > 0 -> "${durHours}小时${durMins}分钟"
        durHours > 0 -> "${durHours}小时"
        else -> "${durMins}分钟"
    }

    val isShow = category?.iconKey == "show"
    val waitStartStr = if (isShow) formatWaitStart(event) else null

    Dialog(onDismissRequest = onDismiss) {
        Surface(
            shape = RoundedCornerShape(20.dp),
            color = MaterialTheme.colorScheme.surface,
            border = BorderStroke(1.dp, Slate200),
            shadowElevation = 8.dp,
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 4.dp),
        ) {
            Box(Modifier.fillMaxWidth()) {
                // 半透明背景海报插槽（竖向淡入淡出渐变蒙版）
                val context = LocalContext.current
                val posterBitmap = remember(event.title, event.placeName, event.presetKey) {
                    PosterManager.loadEventPoster(context, event.title, event.presetKey)
                        ?: event.placeName?.let { PosterManager.loadFacilityPoster(context, it) }
                }
                if (posterBitmap != null) {
                    Image(
                        bitmap = posterBitmap.asImageBitmap(),
                        contentDescription = null,
                        contentScale = ContentScale.Crop,
                        alignment = Alignment.TopCenter,
                        modifier = Modifier
                            .matchParentSize()
                            .graphicsLayer { compositingStrategy = CompositingStrategy.Offscreen }
                            .drawWithContent {
                                drawContent()
                                drawRect(
                                    brush = Brush.verticalGradient(
                                        colors = listOf(
                                            Color.Black.copy(alpha = 0.38f),
                                            Color.Black.copy(alpha = 0.15f),
                                            Color.Transparent
                                        ),
                                        startY = 0f,
                                        endY = size.height * 0.90f,
                                    ),
                                    blendMode = BlendMode.DstIn,
                                )
                            },
                    )
                }

                Column(
                    modifier = Modifier
                        .fillMaxWidth()
                        .verticalScroll(rememberScrollState())
                        .padding(20.dp),
                ) {
                    // Top row: Category + Close Icon
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.SpaceBetween,
                    ) {
                        if (category != null) {
                            CategoryChip(
                                name = category.name,
                                colorArgb = category.colorArgb,
                            )
                        } else {
                            Spacer(Modifier.width(1.dp))
                        }
                        IconButton(
                            onClick = onDismiss,
                            modifier = Modifier.size(28.dp),
                        ) {
                            Icon(
                                Icons.Outlined.Close,
                                contentDescription = "关闭",
                                tint = Slate500,
                                modifier = Modifier.size(20.dp),
                            )
                        }
                    }

                    Spacer(Modifier.height(8.dp))

                    // Title
                    Text(
                        text = event.title,
                        style = MaterialTheme.typography.titleLarge,
                        fontWeight = FontWeight.Bold,
                        color = Slate900,
                    )

                    Spacer(Modifier.height(14.dp))
                    HorizontalDivider(color = Slate100)
                    Spacer(Modifier.height(14.dp))

                    // Time Section
                    Row(
                        verticalAlignment = Alignment.CenterVertically,
                        modifier = Modifier.fillMaxWidth(),
                    ) {
                        Box(
                            modifier = Modifier
                                .size(36.dp)
                                .clip(CircleShape)
                                .background(CruiseAzureContainer),
                            contentAlignment = Alignment.Center,
                        ) {
                            Icon(
                                Icons.Outlined.Schedule,
                                contentDescription = null,
                                tint = CruiseMarine,
                                modifier = Modifier.size(20.dp),
                            )
                        }
                        Spacer(Modifier.width(12.dp))
                        Column {
                            Text(
                                text = "$startStr – $endStr",
                                style = MaterialTheme.typography.titleMedium,
                                fontWeight = FontWeight.Bold,
                                color = Slate900,
                            )
                            Spacer(Modifier.height(2.dp))
                            Text(
                                text = "时长 $durationStr",
                                style = MaterialTheme.typography.bodySmall,
                                color = Slate500,
                            )
                        }
                    }

                    // Waiting hint (if show)
                    if (waitStartStr != null) {
                        Spacer(Modifier.height(10.dp))
                        Row(
                            modifier = Modifier
                                .fillMaxWidth()
                                .clip(RoundedCornerShape(8.dp))
                                .background(CruiseAzureContainer.copy(alpha = 0.6f))
                                .padding(horizontal = 10.dp, vertical = 6.dp),
                            verticalAlignment = Alignment.CenterVertically,
                        ) {
                            Text(
                                text = "建议候场：$waitStartStr 入场（提前 ${event.effectiveWaitMinutes} 分钟）",
                                style = MaterialTheme.typography.labelMedium,
                                fontWeight = FontWeight.SemiBold,
                                color = CruiseMarine,
                            )
                        }
                    }

                    // Location Section
                    if (fullLocation != null) {
                        Spacer(Modifier.height(12.dp))
                        Surface(
                            shape = RoundedCornerShape(12.dp),
                            color = Slate100,
                            border = BorderStroke(1.dp, Slate200),
                            modifier = Modifier.fillMaxWidth(),
                        ) {
                            Row(
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .padding(12.dp),
                                verticalAlignment = Alignment.CenterVertically,
                            ) {
                                Icon(
                                    Icons.Outlined.LocationOn,
                                    contentDescription = null,
                                    tint = CruiseAzure,
                                    modifier = Modifier.size(20.dp),
                                )
                                Spacer(Modifier.width(8.dp))
                                Text(
                                    text = fullLocation,
                                    style = MaterialTheme.typography.bodyMedium,
                                    fontWeight = FontWeight.Medium,
                                    color = Slate900,
                                    modifier = Modifier.weight(1f),
                                )
                                if (event.deck != null) {
                                    Spacer(Modifier.width(8.dp))
                                    FilledTonalButton(
                                        onClick = {
                                            onDismiss()
                                            val target = event.placeName?.trim() ?: ""
                                            onNavigateToMap(event.deck, target)
                                        },
                                        contentPadding = PaddingValues(
                                            horizontal = 10.dp,
                                            vertical = 4.dp
                                        ),
                                        shape = RoundedCornerShape(8.dp),
                                        colors = ButtonDefaults.filledTonalButtonColors(
                                            containerColor = CruiseAzureContainer,
                                            contentColor = CruiseMarine,
                                        ),
                                    ) {
                                        Icon(
                                            Icons.Outlined.Map,
                                            contentDescription = null,
                                            modifier = Modifier.size(14.dp),
                                        )
                                        Spacer(Modifier.width(4.dp))
                                        Text(
                                            text = "定位",
                                            style = MaterialTheme.typography.labelSmall,
                                            fontWeight = FontWeight.Bold,
                                        )
                                    }
                                }
                            }
                        }
                    }

                    Spacer(Modifier.height(18.dp))

                    // Bottom Action
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.End,
                    ) {
                        Button(
                            onClick = onDismiss,
                            shape = RoundedCornerShape(10.dp),
                            colors = ButtonDefaults.buttonColors(
                                containerColor = CruiseAzure,
                                contentColor = Color.White,
                            ),
                        ) {
                            Text("关闭", fontWeight = FontWeight.SemiBold)
                        }
                    }
                }
            }
        }
    }
}

package com.cruise.assistant.ui.settings

import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.FlowRow
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.layout.ExperimentalLayoutApi
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.outlined.ArrowForwardIos
import androidx.compose.material.icons.outlined.Category
import androidx.compose.material.icons.outlined.DirectionsBoat
import androidx.compose.material.icons.outlined.Notifications
import androidx.compose.material.icons.outlined.Restaurant
import androidx.compose.material.icons.outlined.Schedule
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.FilterChip
import androidx.compose.material3.FilterChipDefaults
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.Slider
import androidx.compose.material3.SliderDefaults
import androidx.compose.material3.Surface
import androidx.compose.material3.Switch
import androidx.compose.material3.SwitchDefaults
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.cruise.assistant.R
import com.cruise.assistant.ui.common.TimeWheel
import com.cruise.assistant.ui.theme.CruiseAzure
import com.cruise.assistant.ui.theme.CruiseAzureContainer
import com.cruise.assistant.ui.theme.CruiseMarine
import com.cruise.assistant.ui.theme.Slate100
import com.cruise.assistant.ui.theme.Slate200
import com.cruise.assistant.ui.theme.Slate500
import com.cruise.assistant.ui.theme.Slate700
import com.cruise.assistant.ui.theme.Slate900
import java.time.LocalTime
import java.time.format.DateTimeFormatter

private val DATE_FMT = DateTimeFormatter.ofPattern("yyyy年M月d日")

@OptIn(ExperimentalLayoutApi::class)
@Composable
fun SettingsScreen(
    state: SettingsUiState,
    onRangeChange: (Int, Int) -> Unit,
    onHourHeightChange: (Int) -> Unit,
    onCollapseToggle: (Boolean) -> Unit,
    onNotificationsToggle: (Boolean) -> Unit,
    onRemindBefore15Change: (Boolean) -> Unit,
    onRemindBefore5Change: (Boolean) -> Unit,
    onRemindAtStartChange: (Boolean) -> Unit,
    onDinnerTimeChange: (java.time.LocalTime) -> Unit,
    onResetVoyage: () -> Unit,
    onManageCategories: () -> Unit,
    exactAlarmAvailable: Boolean,
    onNavigateToFacilities: () -> Unit = {},
) {
    var confirmReset by remember { mutableStateOf(false) }
    val cardShape = RoundedCornerShape(16.dp)

    Column(
        Modifier
            .fillMaxSize()
            .background(MaterialTheme.colorScheme.background)
            .verticalScroll(rememberScrollState())
            .padding(16.dp),
    ) {
        Text(
            stringResource(R.string.settings),
            style = MaterialTheme.typography.titleLarge,
            fontWeight = FontWeight.Bold,
            color = Slate900,
        )
        Spacer(Modifier.height(16.dp))

        // ---- 顶部特色入口：船上设施指南 ----
        Surface(
            shape = cardShape,
            color = MaterialTheme.colorScheme.surface,
            border = androidx.compose.foundation.BorderStroke(1.5.dp, CruiseAzure.copy(alpha = 0.5f)),
            modifier = Modifier
                .fillMaxWidth()
                .clickable { onNavigateToFacilities() },
        ) {
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(16.dp),
                verticalAlignment = Alignment.CenterVertically,
            ) {
                Box(
                    modifier = Modifier
                        .size(40.dp)
                        .clip(CircleShape)
                        .background(CruiseAzureContainer),
                    contentAlignment = Alignment.Center,
                ) {
                    Icon(
                        imageVector = Icons.Outlined.DirectionsBoat,
                        contentDescription = null,
                        tint = CruiseMarine,
                        modifier = Modifier.size(22.dp),
                    )
                }
                Spacer(Modifier.width(12.dp))
                Column(Modifier.weight(1f)) {
                    Text(
                        stringResource(R.string.facility_guide_title),
                        style = MaterialTheme.typography.titleMedium,
                        fontWeight = FontWeight.Bold,
                        color = Slate900,
                    )
                    Spacer(Modifier.height(2.dp))
                    Text(
                        stringResource(R.string.facility_guide_subtitle),
                        style = MaterialTheme.typography.bodySmall,
                        color = Slate500,
                    )
                }
                Spacer(Modifier.width(8.dp))
                Icon(
                    imageVector = Icons.AutoMirrored.Outlined.ArrowForwardIos,
                    contentDescription = null,
                    tint = CruiseAzure,
                    modifier = Modifier.size(16.dp),
                )
            }
        }

        Spacer(Modifier.height(14.dp))

        // ---- 卡片 1：时间轴显示范围 ----
        SettingsCard(
            title = stringResource(R.string.display_range_title),
            icon = Icons.Outlined.Schedule,
            cardShape = cardShape,
        ) {
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .clip(RoundedCornerShape(8.dp))
                    .background(CruiseAzureContainer)
                    .padding(horizontal = 10.dp, vertical = 6.dp),
                verticalAlignment = Alignment.CenterVertically,
            ) {
                Text(
                    text = "%02d:%02d – %02d:%02d".format(
                        state.settings.displayStartMinute / 60,
                        state.settings.displayStartMinute % 60,
                        state.settings.displayEndMinute / 60,
                        state.settings.displayEndMinute % 60,
                    ),
                    style = MaterialTheme.typography.titleSmall,
                    fontWeight = FontWeight.Bold,
                    color = CruiseMarine,
                )
            }
            Spacer(Modifier.height(12.dp))

            // 范围时间滚轮
            Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(12.dp)) {
                Column(Modifier.weight(1f)) {
                    Text(
                        stringResource(R.string.range_from),
                        style = MaterialTheme.typography.labelMedium,
                        fontWeight = FontWeight.Medium,
                        color = Slate700,
                    )
                    Spacer(Modifier.height(6.dp))
                    TimeWheel(
                        value = minuteToTime(state.settings.displayStartMinute),
                        onValueChange = { onRangeChange(it.hour * 60 + it.minute, state.settings.displayEndMinute) },
                        modifier = Modifier.fillMaxWidth(),
                        minuteStep = 30,
                    )
                }
                Column(Modifier.weight(1f)) {
                    Text(
                        stringResource(R.string.range_to),
                        style = MaterialTheme.typography.labelMedium,
                        fontWeight = FontWeight.Medium,
                        color = Slate700,
                    )
                    Spacer(Modifier.height(6.dp))
                    TimeWheel(
                        value = minuteToTime(state.settings.displayEndMinute),
                        onValueChange = {
                            val minutes = it.hour * 60 + it.minute
                            onRangeChange(
                                state.settings.displayStartMinute,
                                if (minutes >= 23 * 60 + 30) 1440 else minutes,
                            )
                        },
                        modifier = Modifier.fillMaxWidth(),
                        minuteStep = 30,
                    )
                }
            }

            state.rangeErrorResId?.let { resId ->
                Spacer(Modifier.height(8.dp))
                Text(
                    stringResource(resId),
                    color = MaterialTheme.colorScheme.error,
                    style = MaterialTheme.typography.bodySmall,
                )
            }

            Spacer(Modifier.height(14.dp))
            HorizontalDivider(color = Slate200)
            Spacer(Modifier.height(14.dp))

            Row(verticalAlignment = Alignment.CenterVertically) {
                Text(
                    stringResource(R.string.hour_height_label),
                    style = MaterialTheme.typography.bodyMedium,
                    color = Slate700,
                    modifier = Modifier.weight(1f),
                )
                Text(
                    "${state.settings.hourHeightDp}dp",
                    style = MaterialTheme.typography.titleSmall,
                    fontWeight = FontWeight.Bold,
                    color = CruiseMarine,
                )
            }
            Slider(
                value = state.settings.hourHeightDp.toFloat(),
                onValueChange = { onHourHeightChange(it.toInt()) },
                valueRange = 60f..180f,
                steps = 19,
                colors = SliderDefaults.colors(
                    thumbColor = CruiseAzure,
                    activeTrackColor = CruiseAzure,
                    inactiveTrackColor = Slate200,
                ),
            )

            Row(verticalAlignment = Alignment.CenterVertically) {
                Text(
                    stringResource(R.string.collapse_empty_gaps),
                    style = MaterialTheme.typography.bodyMedium,
                    color = Slate700,
                    modifier = Modifier.weight(1f),
                )
                Switch(
                    checked = state.settings.collapseEmptyGaps,
                    onCheckedChange = onCollapseToggle,
                    colors = SwitchDefaults.colors(checkedThumbColor = Color.White, checkedTrackColor = CruiseAzure),
                )
            }
        }

        Spacer(Modifier.height(14.dp))

        // ---- 卡片 2：提醒配置 ----
        SettingsCard(
            title = stringResource(R.string.reminder_section),
            icon = Icons.Outlined.Notifications,
            cardShape = cardShape,
        ) {
            Row(verticalAlignment = Alignment.CenterVertically) {
                Text(
                    stringResource(R.string.reminder_enabled),
                    style = MaterialTheme.typography.bodyMedium,
                    fontWeight = FontWeight.Medium,
                    color = Slate900,
                    modifier = Modifier.weight(1f),
                )
                Switch(
                    checked = state.settings.notificationsEnabled,
                    onCheckedChange = onNotificationsToggle,
                    colors = SwitchDefaults.colors(checkedThumbColor = Color.White, checkedTrackColor = CruiseAzure),
                )
            }
            if (state.settings.notificationsEnabled) {
                Spacer(Modifier.height(10.dp))
                HorizontalDivider(color = Slate200)
                Spacer(Modifier.height(10.dp))

                Text(
                    stringResource(R.string.reminder_lead_time),
                    style = MaterialTheme.typography.labelMedium,
                    fontWeight = FontWeight.SemiBold,
                    color = Slate500,
                )
                Spacer(Modifier.height(6.dp))
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Text(
                        stringResource(R.string.reminder_before_15),
                        style = MaterialTheme.typography.bodySmall,
                        color = Slate700,
                        modifier = Modifier.weight(1f),
                    )
                    Switch(
                        checked = state.settings.remindBefore15,
                        onCheckedChange = onRemindBefore15Change,
                        colors = SwitchDefaults.colors(checkedThumbColor = Color.White, checkedTrackColor = CruiseAzure),
                    )
                }
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Text(
                        stringResource(R.string.reminder_before_5),
                        style = MaterialTheme.typography.bodySmall,
                        color = Slate700,
                        modifier = Modifier.weight(1f),
                    )
                    Switch(
                        checked = state.settings.remindBefore5,
                        onCheckedChange = onRemindBefore5Change,
                        colors = SwitchDefaults.colors(checkedThumbColor = Color.White, checkedTrackColor = CruiseAzure),
                    )
                }
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Text(
                        stringResource(R.string.reminder_at_start),
                        style = MaterialTheme.typography.bodySmall,
                        color = Slate700,
                        modifier = Modifier.weight(1f),
                    )
                    Switch(
                        checked = state.settings.remindAtStart,
                        onCheckedChange = onRemindAtStartChange,
                        colors = SwitchDefaults.colors(checkedThumbColor = Color.White, checkedTrackColor = CruiseAzure),
                    )
                }
                if (!exactAlarmAvailable) {
                    Spacer(Modifier.height(8.dp))
                    Text(
                        stringResource(R.string.exact_alarm_unavailable),
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.error,
                    )
                }
            }
        }

        Spacer(Modifier.height(14.dp))

        // ---- 卡片 3：晚餐时段 ----
        SettingsCard(
            title = stringResource(R.string.dinner_time_section),
            icon = Icons.Outlined.Restaurant,
            cardShape = cardShape,
        ) {
            FlowRow(
                horizontalArrangement = Arrangement.spacedBy(8.dp),
                verticalArrangement = Arrangement.spacedBy(8.dp),
            ) {
                listOf(
                    java.time.LocalTime.of(17, 45) to R.string.dinner_time_1745,
                    java.time.LocalTime.of(20, 15) to R.string.dinner_time_2015,
                ).forEach { (time, labelRes) ->
                    FilterChip(
                        selected = state.settings.dinnerTime == time,
                        onClick = { onDinnerTimeChange(time) },
                        label = { Text(stringResource(labelRes)) },
                        colors = FilterChipDefaults.filterChipColors(
                            selectedContainerColor = CruiseAzureContainer,
                            selectedLabelColor = CruiseMarine,
                        ),
                    )
                }
            }
        }

        Spacer(Modifier.height(14.dp))

        // ---- 卡片 4：航次概览与管理 ----
        SettingsCard(
            title = stringResource(R.string.voyage_section),
            icon = Icons.Outlined.DirectionsBoat,
            cardShape = cardShape,
        ) {
            state.voyage?.let { voyage ->
                Text(
                    "${voyage.boardDate.format(DATE_FMT)} ${voyage.boardTime} → " +
                        "${voyage.disembarkDate.format(DATE_FMT)} ${voyage.disembarkTime}",
                    style = MaterialTheme.typography.bodyMedium,
                    fontWeight = FontWeight.Medium,
                    color = Slate900,
                )
                Spacer(Modifier.height(2.dp))
                Text(
                    stringResource(R.string.voyage_summary_format, voyage.dayCount, voyage.maxDeck),
                    style = MaterialTheme.typography.bodySmall,
                    color = Slate500,
                )
            }
            Spacer(Modifier.height(12.dp))
            OutlinedButton(
                onClick = { confirmReset = true },
                shape = RoundedCornerShape(12.dp),
                colors = ButtonDefaults.outlinedButtonColors(contentColor = MaterialTheme.colorScheme.error),
                modifier = Modifier.fillMaxWidth(),
            ) {
                Text(stringResource(R.string.reset_voyage), fontWeight = FontWeight.SemiBold)
            }
        }

        Spacer(Modifier.height(14.dp))

        // ---- 卡片 5：事项管理 ----
        SettingsCard(
            title = stringResource(R.string.category_section),
            icon = Icons.Outlined.Category,
            cardShape = cardShape,
        ) {
            OutlinedButton(
                onClick = onManageCategories,
                shape = RoundedCornerShape(12.dp),
                colors = ButtonDefaults.outlinedButtonColors(contentColor = CruiseAzure),
                modifier = Modifier.fillMaxWidth(),
            ) {
                Text(stringResource(R.string.category_section), fontWeight = FontWeight.SemiBold)
            }
        }

        Spacer(Modifier.height(36.dp))
    }

    if (confirmReset) {
        AlertDialog(
            onDismissRequest = { confirmReset = false },
            title = { Text(stringResource(R.string.reset_voyage), fontWeight = FontWeight.Bold) },
            text = { Text(stringResource(R.string.reset_voyage_warning)) },
            confirmButton = {
                TextButton(onClick = {
                    confirmReset = false
                    onResetVoyage()
                }) { Text(stringResource(R.string.confirm), color = MaterialTheme.colorScheme.error) }
            },
            dismissButton = {
                TextButton(onClick = { confirmReset = false }) {
                    Text(stringResource(R.string.cancel), color = Slate500)
                }
            },
        )
    }
}

@Composable
private fun SettingsCard(
    title: String,
    icon: ImageVector,
    cardShape: RoundedCornerShape,
    content: @Composable () -> Unit,
) {
    Surface(
        shape = cardShape,
        color = MaterialTheme.colorScheme.surface,
        border = androidx.compose.foundation.BorderStroke(1.dp, Slate200),
        modifier = Modifier.fillMaxWidth(),
    ) {
        Column(Modifier.padding(16.dp)) {
            Row(verticalAlignment = Alignment.CenterVertically) {
                Box(
                    modifier = Modifier
                        .size(28.dp)
                        .clip(CircleShape)
                        .background(CruiseAzureContainer),
                    contentAlignment = Alignment.Center,
                ) {
                    Icon(
                        icon,
                        contentDescription = null,
                        tint = CruiseMarine,
                        modifier = Modifier.size(16.dp),
                    )
                }
                Spacer(Modifier.width(10.dp))
                Text(
                    title,
                    style = MaterialTheme.typography.titleMedium,
                    fontWeight = FontWeight.Bold,
                    color = Slate900,
                )
            }
            Spacer(Modifier.height(12.dp))
            content()
        }
    }
}

private fun minuteToTime(minute: Int): LocalTime {
    val clamped = minute.coerceIn(0, 1439)
    return LocalTime.of(clamped / 60, clamped % 60)
}



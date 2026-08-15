package com.cruise.assistant.ui.timeline

import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.cruise.assistant.R
import com.cruise.assistant.domain.Category
import com.cruise.assistant.domain.CruiseEvent
import com.cruise.assistant.domain.formatLocation
import com.cruise.assistant.ui.theme.CruiseAzure
import com.cruise.assistant.ui.theme.CruiseAzureContainer
import com.cruise.assistant.ui.theme.CruiseMarine
import com.cruise.assistant.ui.theme.Slate100
import com.cruise.assistant.ui.theme.Slate200
import com.cruise.assistant.ui.theme.Slate400
import com.cruise.assistant.ui.theme.Slate500
import com.cruise.assistant.ui.theme.Slate900

/** 应用内"进行中 / 下一个"横幅，仅查看当天时显示 */
@Composable
fun NowBanner(
    ongoing: CruiseEvent?,
    next: CruiseEvent?,
    categories: Map<Long, Category>,
    modifier: Modifier = Modifier,
) {
    if (ongoing == null && next == null) return
    val shape = RoundedCornerShape(14.dp)
    Column(
        modifier = modifier
            .fillMaxWidth()
            .padding(horizontal = 14.dp, vertical = 4.dp)
            .clip(shape)
            .background(MaterialTheme.colorScheme.surface)
            .border(1.dp, CruiseAzureContainer, shape)
            .padding(horizontal = 12.dp, vertical = 8.dp),
        verticalArrangement = Arrangement.spacedBy(6.dp),
    ) {
        ongoing?.let {
            BannerRow(
                label = stringResource(R.string.ongoing_label),
                isOngoing = true,
                event = it,
                categories = categories,
            )
        }
        next?.let {
            BannerRow(
                label = stringResource(R.string.next_label),
                isOngoing = false,
                event = it,
                categories = categories,
            )
        }
    }
}

@Composable
private fun BannerRow(
    label: String,
    isOngoing: Boolean,
    event: CruiseEvent,
    categories: Map<Long, Category>,
) {
    val location = formatLocation(event.deck, event.zone, event.placeName, compact = true)
    val tagBg = if (isOngoing) CruiseAzureContainer else Slate100
    val tagColor = if (isOngoing) CruiseMarine else Slate500
    val dotColor = if (isOngoing) Color(0xFF10B981) else Slate400

    Row(
        Modifier.fillMaxWidth(),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        // 状态徽标
        Row(
            modifier = Modifier
                .clip(RoundedCornerShape(6.dp))
                .background(tagBg)
                .padding(horizontal = 6.dp, vertical = 2.dp),
            verticalAlignment = Alignment.CenterVertically,
        ) {
            Box(
                modifier = Modifier
                    .size(6.dp)
                    .clip(CircleShape)
                    .background(dotColor),
            )
            Spacer(Modifier.width(4.dp))
            Text(
                label,
                style = MaterialTheme.typography.labelSmall,
                fontSize = 10.sp,
                fontWeight = FontWeight.Bold,
                color = tagColor,
            )
        }

        Spacer(Modifier.width(8.dp))

        // 时间
        Text(
            text = "%02d:%02d".format(event.startTime.hour, event.startTime.minute),
            style = MaterialTheme.typography.labelMedium,
            fontWeight = FontWeight.Bold,
            color = if (isOngoing) CruiseMarine else Slate900,
        )

        Spacer(Modifier.width(6.dp))

        // 标题与地点
        Text(
            text = event.title.ifEmpty { categories[event.categoryId]?.name ?: "" },
            style = MaterialTheme.typography.bodySmall,
            fontWeight = FontWeight.Medium,
            color = Slate900,
            maxLines = 1,
            overflow = TextOverflow.Ellipsis,
            modifier = Modifier.weight(1f, fill = false),
        )

        location?.let {
            Spacer(Modifier.width(6.dp))
            Text(
                text = it,
                style = MaterialTheme.typography.labelSmall,
                color = Slate500,
                maxLines = 1,
                overflow = TextOverflow.Ellipsis,
            )
        }
    }
}


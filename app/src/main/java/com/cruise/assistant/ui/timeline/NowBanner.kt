package com.cruise.assistant.ui.timeline

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import com.cruise.assistant.R
import com.cruise.assistant.domain.Category
import com.cruise.assistant.domain.CruiseEvent
import com.cruise.assistant.domain.formatLocation

/** 应用内"进行中 / 下一个"横幅，仅查看当天时显示 */
@Composable
fun NowBanner(
    ongoing: CruiseEvent?,
    next: CruiseEvent?,
    categories: Map<Long, Category>,
    modifier: Modifier = Modifier,
) {
    if (ongoing == null && next == null) return
    Column(
        modifier
            .fillMaxWidth()
            .padding(horizontal = 12.dp, vertical = 4.dp)
            .clip(RoundedCornerShape(10.dp))
            .background(MaterialTheme.colorScheme.primary.copy(alpha = 0.08f))
            .padding(horizontal = 10.dp, vertical = 6.dp),
    ) {
        ongoing?.let { BannerRow(stringResource(R.string.ongoing_label), it, categories) }
        next?.let { BannerRow(stringResource(R.string.next_label), it, categories) }
    }
}

@Composable
private fun BannerRow(label: String, event: CruiseEvent, categories: Map<Long, Category>) {
    val location = formatLocation(event.deck, event.zone, event.placeName, compact = true)
    Row(Modifier.fillMaxWidth()) {
        Text(
            label,
            style = MaterialTheme.typography.labelSmall,
            fontWeight = FontWeight.Bold,
            color = MaterialTheme.colorScheme.primary,
        )
        Spacer(Modifier.width(6.dp))
        Text(
            buildString {
                append("%02d:%02d".format(event.startTime.hour, event.startTime.minute))
                append("  ")
                append(event.title.ifEmpty { categories[event.categoryId]?.name ?: "" })
                location?.let { append("  ").append(it) }
            },
            style = MaterialTheme.typography.labelSmall,
            color = MaterialTheme.colorScheme.onSurface,
            maxLines = 1,
            overflow = TextOverflow.Ellipsis,
        )
    }
}

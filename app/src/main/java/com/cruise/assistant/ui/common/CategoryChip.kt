package com.cruise.assistant.ui.common

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.cruise.assistant.ui.theme.cardTextColorFor

/** 截图1 卡片第一行里的类别小标签 */
@Composable
fun CategoryChip(name: String, colorArgb: Int, modifier: Modifier = Modifier) {
    Text(
        text = name,
        style = MaterialTheme.typography.labelSmall,
        fontSize = 10.sp,
        fontWeight = FontWeight.SemiBold,
        color = cardTextColorFor(colorArgb),
        maxLines = 1,
        overflow = TextOverflow.Ellipsis,
        modifier = modifier
            .background(
                Color(colorArgb).copy(alpha = 0.18f),
                RoundedCornerShape(4.dp),
            )
            .padding(horizontal = 4.dp, vertical = 1.dp),
    )
}


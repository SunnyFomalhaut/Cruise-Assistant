package com.cruise.assistant.ui.common

import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.unit.dp

/** 截图3 的主题色圆点，选中时加深色描边 */
@Composable
fun ColorDot(
    colorArgb: Int,
    selected: Boolean,
    onClick: () -> Unit,
    modifier: Modifier = Modifier,
) {
    Box(
        modifier
            .size(40.dp)
            .background(Color(colorArgb), CircleShape)
            .then(
                if (selected) Modifier.border(3.dp, Color.Black, CircleShape) else Modifier,
            )
            .clickable(onClick = onClick),
    )
}

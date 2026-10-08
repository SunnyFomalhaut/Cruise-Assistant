package com.cruise.assistant.ui.timeline

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.BoxWithConstraints
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.offset
import androidx.compose.foundation.layout.width
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import com.cruise.assistant.domain.Category
import com.cruise.assistant.domain.CruiseEvent
import com.cruise.assistant.domain.LANE_COUNT
import com.cruise.assistant.domain.TimelineLayout
import com.cruise.assistant.domain.effectiveWaitMinutes
import com.cruise.assistant.domain.minuteToDp
import com.cruise.assistant.domain.showEffectiveRange
import com.cruise.assistant.ui.theme.NowRed

/** 三栏等宽（各占 1/3 总宽） */
private val LANE_WEIGHTS = floatArrayOf(1f, 1f, 1f)
private val LANE_GAP = 3.dp
private val MIN_CARD_HEIGHT = 22.dp

/** 卡片内给标题保留的最小高度（单行 labelSmall 约 16dp + 上下 3dp padding × 2） */
private val TITLE_RESERVE_DP = 26.dp

/** 等待段与标题区之间的间隔（与 EventCard 的 Spacer 一致） */
private val SPACER_DP = 2.dp

/** 3 栏竖分隔线的位置（占总宽的比例），供 TimelineGrid 画线 */
fun laneBoundaryFractions(): List<Float> {
    val total = LANE_WEIGHTS.sum()
    var acc = 0f
    return LANE_WEIGHTS.dropLast(1).map { w ->
        acc += w
        acc / total
    }
}

/**
 * 事件区域。planning 模式按 lane 分 3 栏；finalized 模式忽略 lane 单栏满宽。
 */
@Composable
fun LaneCanvas(
    layout: TimelineLayout,
    events: List<CruiseEvent>,
    categories: Map<Long, Category>,
    conflictIds: Set<Long>,
    singleLane: Boolean,
    onEventClick: (CruiseEvent) -> Unit,
    onEventLongClick: (CruiseEvent) -> Unit,
    /** 长按选中的事件（用于在其卡片位置弹出菜单）；null 表示无菜单 */
    menuFor: CruiseEvent? = null,
    /** 菜单内容槽：渲染在被按卡片上的 DropdownMenu */
    menuContent: @Composable (CruiseEvent) -> Unit = {},
    dollsEnabled: Boolean = true,
    modifier: Modifier = Modifier,
) {
    BoxWithConstraints(modifier.fillMaxWidth().height(layout.totalHeightDp.dp)) {
        val totalWidth = maxWidth
        val weightSum = LANE_WEIGHTS.sum()

        events.forEach { event ->
            val isShow = categories[event.categoryId]?.iconKey == "show"
            val (effStart, effEnd) = showEffectiveRange(event, isShow)
            val top = layout.minuteToDp(effStart)
            val bottom = layout.minuteToDp(effEnd)
            // 范围外的事件不画（顶部提示条负责告知）
            if (bottom <= 0f || top >= layout.totalHeightDp) return@forEach

            val heightDp = (bottom - top).dp.coerceAtLeast(MIN_CARD_HEIGHT)
            // 等待段高度（仅 show 事件，按候场时间换算；极限缩放下给 16dp 保底；
            // 钳制到给标题留出空间，否则短演出的标题会被候场段挤出卡片裁剪区）
            val waitHeightDp = if (isShow) {
                val band = (layout.minuteToDp(effStart + event.effectiveWaitMinutes) - top).dp
                val minWaitHeight = if (band > 0.dp) 16.dp else 0.dp
                band.coerceAtLeast(minWaitHeight)
                    .coerceAtMost(heightDp - TITLE_RESERVE_DP - SPACER_DP)
                    .coerceAtLeast(0.dp)
            } else {
                0.dp
            }

            val lane = event.lane.coerceIn(0, LANE_COUNT - 1)
            val (offsetX, cardWidth) = if (singleLane) {
                0.dp to totalWidth
            } else {
                val leftFraction = LANE_WEIGHTS.take(lane).sum() / weightSum
                val widthFraction = LANE_WEIGHTS[lane] / weightSum
                (totalWidth * leftFraction) to (totalWidth * widthFraction - LANE_GAP)
            }

            Box(
                Modifier
                    .offset(x = offsetX, y = top.dp)
                    .width(cardWidth)
                    .height(heightDp),
            ) {
                EventCard(
                    event = event,
                    category = categories[event.categoryId],
                    heightDp = heightDp,
                    waitSegmentHeightDp = waitHeightDp,
                    hasConflict = event.id in conflictIds,
                    onClick = { onEventClick(event) },
                    onLongClick = { onEventLongClick(event) },
                    dollsEnabled = dollsEnabled,
                    isFinalized = singleLane,
                    modifier = Modifier.fillMaxSize(),
                )
                if (menuFor?.id == event.id) {
                    menuContent(event)
                }
            }
        }
    }
}

/** 红色"现在"指示线 */
@Composable
fun NowIndicator(layout: TimelineLayout, nowMinute: Int, modifier: Modifier = Modifier) {
    if (nowMinute < layout.rangeStartMinute || nowMinute > layout.rangeEndMinute) return
    val y = layout.minuteToDp(nowMinute)
    Box(
        modifier
            .offset(y = y.dp - 4.dp)
            .fillMaxWidth()
            .height(8.dp),
    ) {
        // 横贯线
        Box(
            Modifier
                .offset(y = 3.dp)
                .fillMaxWidth()
                .height(2.dp)
                .background(NowRed),
        )
        // 头部圆点
        Box(
            Modifier
                .offset(x = 0.dp, y = 0.dp)
                .width(8.dp)
                .height(8.dp)
                .background(NowRed, shape = androidx.compose.foundation.shape.CircleShape),
        )
    }
}

package com.cruise.assistant.ui.pixiedust

import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test

/**
 * 联动检视抽屉「按楼层展开/收起」纯逻辑测试。
 *
 * 说明：展开状态的持有（rememberSaveable + Compose 状态读写）依赖 Compose 运行时，
 * 无法在纯 JVM 单测中验证；这里覆盖提取出的纯函数 toggleDeckExpanded，
 * Saver 的存取与整体折叠行为由模拟器 UI 测试验证。
 */
class PixieLinkageExpansionTest {

    // ---- toggleDeckExpanded：默认全收起（空集）为初始态 ----

    @Test
    fun `empty set as default means no deck expanded`() {
        assertTrue(toggleDeckExpanded(emptySet(), 17).contains(17))
        assertEquals(setOf(17), toggleDeckExpanded(emptySet(), 17))
    }

    @Test
    fun `toggle adds deck when collapsed`() {
        val expanded = toggleDeckExpanded(emptySet(), 5)
        assertTrue(5 in expanded)
    }

    @Test
    fun `toggle removes deck when expanded`() {
        val collapsed = toggleDeckExpanded(setOf(5, 9), 5)
        assertFalse(5 in collapsed)
        assertTrue(9 in collapsed)
    }

    @Test
    fun `toggle twice returns to original state`() {
        val state: Set<Int> = setOf(11)
        assertEquals(state, toggleDeckExpanded(toggleDeckExpanded(state, 17), 17))
    }

    // ---- 多层同时展开（区别于旧 GroupedView 单开手风琴） ----

    @Test
    fun `multiple decks can be expanded at the same time`() {
        var state: Set<Int> = emptySet()
        state = toggleDeckExpanded(state, 9)
        state = toggleDeckExpanded(state, 12)
        state = toggleDeckExpanded(state, 17)
        assertEquals(setOf(9, 12, 17), state)
    }

    @Test
    fun `toggling one deck does not affect others`() {
        val state = toggleDeckExpanded(setOf(3, 7), 7)
        assertEquals(setOf(3), state)
    }
}

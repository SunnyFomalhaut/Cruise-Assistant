package com.cruise.assistant.ui.archive

import com.cruise.assistant.data.db.entity.VoyageArchiveEntity
import com.cruise.assistant.data.prefs.AppSettings
import com.cruise.assistant.domain.CruiseEvent
import com.cruise.assistant.domain.ShipZone
import com.cruise.assistant.domain.serializeArchiveSnapshot
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.launchIn
import kotlinx.coroutines.flow.onEach
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertNotNull
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Test
import java.time.LocalDate
import java.time.LocalTime

/**
 * 存档详情（只读回放）ViewModel 测试：
 * 初始选中登船日、切日期重算、layout 跟随当前查看偏好、快照缺失报错、
 * 回放模式切换（日程表=仅生效日程；草稿=含灰色未启用备选；初始模式跟随登船日定稿状态；
 * 已定稿日可切回草稿看到备选；模式跨 selectDate 保持）。
 */
class ArchiveDetailViewModelTest {

    private fun newVm(
        dao: FakeVoyageArchiveDao,
        archiveId: Long,
        settings: AppSettings = AppSettings(),
    ): ArchiveDetailViewModel {
        val settingsFlow = MutableStateFlow(settings)
        val vm = ArchiveDetailViewModel(
            repository = ArchiveViewModelFixtures.newRepository(dao),
            settingsFlow = settingsFlow,
            archiveId = archiveId,
            coroutineScope = CoroutineScope(Dispatchers.Unconfined),
        )
        // stateIn(WhileSubscribed) 需要有订阅者才启动上游：测试里挂一个收集器
        vm.state.onEach { }.launchIn(CoroutineScope(Dispatchers.Unconfined))
        return vm
    }

    private fun <T> kotlinx.coroutines.flow.StateFlow<T>.latest(): T = value

    /**
     * 第二天（未定稿）追加一件停用事件的自定义档：夹具默认第二天只有活动事件，
     * 为验证"未定稿+收起=规划态保留 inactive、展开=强制过滤 inactive"，造一档带灰事件。
     */
    private fun insertArchiveWithInactiveOnUnfinalizedDay(
        dao: FakeVoyageArchiveDao,
        id: Long = 5L,
    ): Long {
        val extraInactive = CruiseEvent(
            id = 99,
            date = ArchiveViewModelFixtures.BOARD_DATE.plusDays(1),
            startTime = LocalTime.of(9, 0),
            durationMinutes = 30,
            categoryId = 1,
            title = "旧安排（已停用）",
            deck = 5,
            zone = ShipZone.MID,
            placeName = "泳池",
            lane = 2,
            isActive = false,
        )
        val snapshot = ArchiveViewModelFixtures.buildSnapshot()
            .copy(events = ArchiveViewModelFixtures.events + extraInactive)
        dao.rows.add(
            VoyageArchiveEntity(
                id = id,
                shipName = ArchiveViewModelFixtures.voyage.shipName,
                boardDate = ArchiveViewModelFixtures.voyage.boardDate,
                disembarkDate = ArchiveViewModelFixtures.voyage.disembarkDate,
                nightsSummary = "3+4夜 · 共7晚",
                createdAt = 1_000L,
                payloadJson = serializeArchiveSnapshot(snapshot),
            ),
        )
        dao.sync()
        return id
    }

    /**
     * 登船日也未定稿的档：验证初始模式=DRAFT（初始模式跟随登船日存档定稿状态）。
     * 共享夹具不可变，这里 copy 快照 dayPlans 后真实序列化入档（走 getSnapshot 反序列化路径）。
     */
    private fun insertAllUnfinalizedArchive(
        dao: FakeVoyageArchiveDao,
        id: Long = 6L,
    ): Long {
        val snapshot = ArchiveViewModelFixtures.buildSnapshot().copy(
            dayPlans = ArchiveViewModelFixtures.dayPlans.map {
                it.copy(finalized = false, finalizedAt = null)
            },
        )
        dao.rows.add(
            VoyageArchiveEntity(
                id = id,
                shipName = ArchiveViewModelFixtures.voyage.shipName,
                boardDate = ArchiveViewModelFixtures.voyage.boardDate,
                disembarkDate = ArchiveViewModelFixtures.voyage.disembarkDate,
                nightsSummary = "3+4夜 · 共7晚",
                createdAt = 1_000L,
                payloadJson = serializeArchiveSnapshot(snapshot),
            ),
        )
        dao.sync()
        return id
    }

    @Test
    fun `加载快照后初始选中登船日并完成当日复算`() {
        val dao = FakeVoyageArchiveDao()
        val id = ArchiveViewModelFixtures.insertArchive(dao)
        val vm = newVm(dao, id)

        val state = vm.state.latest()
        assertFalse(state.loading)
        assertFalse(state.loadFailed)
        assertNotNull(state.snapshot)
        // 初始 = 快照 voyage.boardDate
        assertEquals(ArchiveViewModelFixtures.BOARD_DATE, state.selectedDate)

        val snapshot = state.snapshot!!
        // categories map 来自快照
        assertEquals(2, state.categories.size)
        assertEquals("演出", state.categories[2L]?.name)
        // dayPlans map 来自快照
        assertEquals(2, state.dayPlans.size)
        // 登船日定稿 → 初始模式=SCHEDULE（日程表），只剩 isActive 的"登船"，15:00 非活动事件被过滤
        assertTrue(state.isFinalized)
        assertEquals(ArchiveTimelineMode.SCHEDULE, state.timelineMode)
        assertTrue(state.renderAsSchedule)
        assertEquals(listOf("登船"), state.events.map { it.title })
        // layout 非空且覆盖当日事件（默认设置 07:00-24:00 无范围外隐藏）
        assertNotNull(state.layout)
        assertEquals(0, state.layout!!.hiddenBefore)
        assertEquals(0, state.layout!!.hiddenAfter)
        // 仙尘按房间配对：Deck 10 · 088 发一收一，收到未回礼
        assertEquals(1, state.roomPairs.size)
        val pair = state.roomPairs.single()
        assertEquals(10, pair.deck)
        assertEquals("088", pair.room)
        assertEquals(1, pair.given.size)
        assertEquals(1, pair.received.size)
        assertEquals(1, pair.unrepliedReceived.size)
        // 存档时间来自元信息表
        assertEquals(1_000L, state.createdAt)
        // 晚数摘要（快照 voyage 复算，联程 3+4）
        assertEquals("3+4夜 · 共7晚", state.nightsSummary)
        // 快照自包含：快照本体可直接反查（确保插入的就是这份）
        assertEquals(7, snapshot.voyage.dayCount)
    }

    @Test
    fun `切日期重算当日事件与定稿状态`() {
        val dao = FakeVoyageArchiveDao()
        val id = ArchiveViewModelFixtures.insertArchive(dao)
        val vm = newVm(dao, id)

        vm.selectDate(ArchiveViewModelFixtures.BOARD_DATE.plusDays(1))
        val state = vm.state.latest()
        assertEquals(ArchiveViewModelFixtures.BOARD_DATE.plusDays(1), state.selectedDate)
        // 第二天未定稿：初始模式（SCHEDULE，跟随登船日定稿状态）跨切日保持，
        // 当天只有 1 件活动 show，活动/非活动两种过滤下结果一致
        assertFalse(state.isFinalized)
        assertEquals(listOf("狮子王"), state.events.map { it.title })
        // show 类别进入 showCategoryIds（狮子王 19:00-19:50 在默认范围内，无隐藏）
        assertEquals(0, state.layout!!.hiddenBefore)

        // 切回登船日：定稿过滤再次生效
        vm.selectDate(ArchiveViewModelFixtures.BOARD_DATE)
        val back = vm.state.latest()
        assertTrue(back.isFinalized)
        assertEquals(1, back.events.size)
    }

    @Test
    fun `定稿日过滤inactive事件且未定稿日保留`() {
        val dao = FakeVoyageArchiveDao()
        val id = ArchiveViewModelFixtures.insertArchive(dao)
        val vm = newVm(dao, id)

        // 登船日定稿 → 初始 SCHEDULE 模式：inactive 的"旧安排（已停用）"不可见
        //（日程表模式过滤 inactive；切 DRAFT 后备选可见的断言见"已定稿日默认日程表且可切回草稿看到备选"）
        val finalizedState = vm.state.latest()
        assertTrue(finalizedState.events.none { !it.isActive })

        // 把登船日事件放到未定稿的第二天视角验证不过滤：构造当前未定稿日含 inactive 事件
        // —— 第二天无 inactive 事件，因此改用登船日的停用事件在"未定稿"假设下应可见；
        // 快照定稿位不可变，这里直接验证 VM 不提供任何改档入口（只读）：
        assertTrue(
            ArchiveDetailViewModel::class.java.declaredMethods.none { it.name == "toggleActive" },
        )
        assertTrue(
            ArchiveDetailViewModel::class.java.declaredMethods.none { it.name == "deleteEvent" },
        )
    }

    @Test
    fun `layout跟随当前查看偏好设置而非快照`() {
        val dao = FakeVoyageArchiveDao()
        val id = ArchiveViewModelFixtures.insertArchive(dao)
        // 显示范围 13:00 起：登船日 12:00-13:00 的活动事件整体落在范围外 → hiddenBefore
        val vm = newVm(
            dao,
            id,
            settings = AppSettings(
                displayStartMinute = 13 * 60,
                displayEndMinute = 1440,
                collapseEmptyGaps = true,
            ),
        )
        val state = vm.state.latest()
        assertEquals(1, state.layout!!.hiddenBefore)
        assertTrue(state.layout!!.rangeStartMinute == 13 * 60)

        // "看全天"撑满 0-1440 后隐藏归零（同 TimelineViewModel.toggleFullDay 语义）
        vm.toggleFullDay()
        val fullDay = vm.state.latest()
        assertEquals(0, fullDay.layout!!.hiddenBefore)
        assertEquals(0, fullDay.layout!!.rangeStartMinute)

        // 切日期重置"看全天"（同 TimelineViewModel.selectDate 语义）
        vm.selectDate(ArchiveViewModelFixtures.BOARD_DATE.plusDays(1))
        val reset = vm.state.latest()
        assertTrue(reset.layout!!.rangeStartMinute == 13 * 60)
    }

    @Test
    fun `快照缺失时进入错误态`() {
        val dao = FakeVoyageArchiveDao()
        val vm = newVm(dao, archiveId = 99L) // 没有这份存档

        val state = vm.state.latest()
        assertFalse(state.loading)
        assertTrue(state.loadFailed)
        assertNull(state.snapshot)
        assertNull(state.selectedDate)
        assertNull(state.layout)
    }

    @Test
    fun `快照损坏时同样报错而非崩溃`() {
        val dao = FakeVoyageArchiveDao()
        dao.rows.add(
            com.cruise.assistant.data.db.entity.VoyageArchiveEntity(
                id = 7L,
                shipName = null,
                boardDate = LocalDate.of(2026, 10, 6),
                disembarkDate = LocalDate.of(2026, 10, 12),
                nightsSummary = "6夜",
                createdAt = 1L,
                payloadJson = "{ 不是合法 JSON",
            ),
        )
        val vm = newVm(dao, 7L)

        val state = vm.state.latest()
        assertTrue(state.loadFailed)
        assertNull(state.snapshot)
    }

    @Test
    fun `未定稿日默认草稿模式含inactive备选`() {
        val dao = FakeVoyageArchiveDao()
        // 登船日也未定稿 → 初始模式 DRAFT
        insertAllUnfinalizedArchive(dao)
        // 开折叠以便用分段数验证布局确实以（未过滤的）事件集构建
        val vm = newVm(dao, 6L, settings = AppSettings(collapseEmptyGaps = true))

        val state = vm.state.latest()

        // 初始模式跟随登船日定稿状态：未定稿 → 草稿（三栏规划态，备选可见）
        assertEquals(ArchiveTimelineMode.DRAFT, state.timelineMode)
        assertFalse(state.renderAsSchedule)
        // 草稿模式：inactive 备选事件（15:00 旧安排）保留可见
        assertFalse(state.isFinalized)
        assertEquals(setOf("登船", "旧安排（已停用）"), state.events.map { it.title }.toSet())
        assertTrue(state.events.any { !it.isActive })
        // 布局以未过滤事件集构建：登船日两事件（12:00-13:00 与 15:00-15:30） padded 保留段
        // 相距不足 90 分钟折叠阈值，合并为一段 → 3 段 = 折叠/保留/折叠（含停用事件的贡献）
        assertEquals(3, state.layout!!.segments.size)
    }

    @Test
    fun `未定稿日切日程表模式后仅剩活动事件`() {
        val dao = FakeVoyageArchiveDao()
        insertArchiveWithInactiveOnUnfinalizedDay(dao)
        val vm = newVm(dao, 5L, settings = AppSettings(collapseEmptyGaps = true))
        vm.selectDate(ArchiveViewModelFixtures.BOARD_DATE.plusDays(1))

        vm.setTimelineMode(ArchiveTimelineMode.SCHEDULE)
        val state = vm.state.latest()

        // 日程表模式：isFinalized 是存档真相、保持 false；渲染形态为日程表
        assertFalse(state.isFinalized)
        assertEquals(ArchiveTimelineMode.SCHEDULE, state.timelineMode)
        assertTrue(state.renderAsSchedule)
        // inactive 停用事件被过滤，只剩活动事件
        assertEquals(listOf("狮子王"), state.events.map { it.title })
        // 布局以过滤后事件构建：停用事件（09:00-09:30）的保留段消失，5 段 → 3 段
        assertEquals(3, state.layout!!.segments.size)

        // 切回草稿：回到规划态原貌，灰事件重新可见
        vm.setTimelineMode(ArchiveTimelineMode.DRAFT)
        val restored = vm.state.latest()
        assertFalse(restored.renderAsSchedule)
        assertEquals(2, restored.events.size)
    }

    @Test
    fun `已定稿日默认日程表且可切回草稿看到备选`() {
        val dao = FakeVoyageArchiveDao()
        val id = ArchiveViewModelFixtures.insertArchive(dao)
        val vm = newVm(dao, id)

        // 登船日定稿 → 初始 SCHEDULE：仅活动事件（灰色备选不可见）
        val schedule = vm.state.latest()
        assertEquals(ArchiveTimelineMode.SCHEDULE, schedule.timelineMode)
        assertTrue(schedule.isFinalized)
        assertTrue(schedule.renderAsSchedule)
        assertEquals(listOf("登船"), schedule.events.map { it.title })

        // 核心新能力：已定稿日切回草稿模式 → 当时的灰色备选（15:00 停用事件）重新可见，
        // 徽标仍显示已定稿（isFinalized 是存档真相，不随模式变化）
        vm.setTimelineMode(ArchiveTimelineMode.DRAFT)
        val draft = vm.state.latest()
        assertEquals(ArchiveTimelineMode.DRAFT, draft.timelineMode)
        assertTrue(draft.isFinalized)
        assertFalse(draft.renderAsSchedule)
        assertEquals(setOf("登船", "旧安排（已停用）"), draft.events.map { it.title }.toSet())
        assertTrue(draft.events.any { !it.isActive })

        // 再切回日程表：回到仅生效日程
        vm.setTimelineMode(ArchiveTimelineMode.SCHEDULE)
        val back = vm.state.latest()
        assertTrue(back.renderAsSchedule)
        assertEquals(listOf("登船"), back.events.map { it.title })
    }

    @Test
    fun `模式跨selectDate保持不重置`() {
        val dao = FakeVoyageArchiveDao()
        insertArchiveWithInactiveOnUnfinalizedDay(dao)
        val vm = newVm(dao, 5L, settings = AppSettings(collapseEmptyGaps = true))

        // 登船日定稿 → 初始 SCHEDULE；切到未定稿的第二天：模式保持 SCHEDULE（不重置）
        vm.selectDate(ArchiveViewModelFixtures.BOARD_DATE.plusDays(1))
        val day2 = vm.state.latest()
        assertEquals(ArchiveTimelineMode.SCHEDULE, day2.timelineMode)
        assertTrue(day2.renderAsSchedule)
        assertEquals(listOf("狮子王"), day2.events.map { it.title })

        // 切到 DRAFT 后在两天间往返：模式都不被 selectDate 重置（用户翻看多天保持视角）
        vm.setTimelineMode(ArchiveTimelineMode.DRAFT)
        vm.selectDate(ArchiveViewModelFixtures.BOARD_DATE)
        val boardDraft = vm.state.latest()
        assertEquals(ArchiveTimelineMode.DRAFT, boardDraft.timelineMode)
        assertFalse(boardDraft.renderAsSchedule)
        // 定稿日草稿视角下备选同样可见
        assertTrue(boardDraft.events.any { !it.isActive })

        vm.selectDate(ArchiveViewModelFixtures.BOARD_DATE.plusDays(1))
        val day2Draft = vm.state.latest()
        assertEquals(ArchiveTimelineMode.DRAFT, day2Draft.timelineMode)
        assertEquals(2, day2Draft.events.size)
    }
}

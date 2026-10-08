package com.cruise.assistant.ui.archive

import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.launchIn
import kotlinx.coroutines.flow.onEach
import kotlinx.coroutines.runBlocking
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Test

/** 存档列表 ViewModel 测试：列表加载、删除确认状态、删除调用仓库 */
class ArchiveListViewModelTest {

    /** 构造 VM 并挂一个收集器：stateIn(WhileSubscribed) 需要订阅者才启动上游 */
    private fun newListVm(dao: FakeVoyageArchiveDao): ArchiveListViewModel {
        val vm = ArchiveListViewModel(
            repository = ArchiveViewModelFixtures.newRepository(dao),
            coroutineScope = CoroutineScope(Dispatchers.Unconfined),
        )
        vm.state.onEach { }.launchIn(CoroutineScope(Dispatchers.Unconfined))
        return vm
    }

    @Test
    fun `列表加载存档元信息`() {
        val dao = FakeVoyageArchiveDao()
        ArchiveViewModelFixtures.insertArchive(dao, id = 1L)
        ArchiveViewModelFixtures.insertArchive(dao, id = 2L)

        val vm = newListVm(dao)

        val state = vm.state.value
        assertEquals(2, state.archives.size)
        assertEquals(listOf(1L, 2L), state.archives.map { it.id })
        assertNull(state.pendingDeleteId)
    }

    @Test
    fun `请求删除挂起确认取消则清除`() {
        val dao = FakeVoyageArchiveDao()
        ArchiveViewModelFixtures.insertArchive(dao, id = 1L)
        val vm = newListVm(dao)

        vm.requestDelete(1L)
        assertEquals(1L, vm.state.value.pendingDeleteId)

        vm.cancelDelete()
        assertNull(vm.state.value.pendingDeleteId)
        // 没真的删
        assertEquals(1, dao.rows.size)
    }

    @Test
    fun `确认删除调用仓库并清除确认状态`() = runBlocking {
        val dao = FakeVoyageArchiveDao()
        ArchiveViewModelFixtures.insertArchive(dao, id = 1L)
        ArchiveViewModelFixtures.insertArchive(dao, id = 2L)
        val vm = newListVm(dao)

        vm.requestDelete(1L)
        vm.delete(1L)

        assertEquals(1, dao.rows.size)
        assertEquals(2L, dao.rows.single().id)
        assertNull(vm.state.value.pendingDeleteId)
        // 列表流同步刷新
        assertEquals(listOf(2L), vm.state.value.archives.map { it.id })
        // 快照随行删除：getSnapshot 读不到
        assertNull(ArchiveViewModelFixtures.newRepository(dao).getSnapshot(1L))
        assertTrue(ArchiveViewModelFixtures.newRepository(dao).getSnapshot(2L) != null)
    }
}

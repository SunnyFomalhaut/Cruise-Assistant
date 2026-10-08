package com.cruise.assistant.ui.archive

import androidx.compose.runtime.Immutable
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.cruise.assistant.data.db.entity.VoyageArchiveMeta
import com.cruise.assistant.data.repo.VoyageArchiveRepository
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch

/** 存档列表页状态：存档元信息列表 + 当前处于"删除确认中"的存档 id（null = 无确认弹窗） */
@Immutable
data class ArchiveListUiState(
    val archives: List<VoyageArchiveMeta> = emptyList(),
    val pendingDeleteId: Long? = null,
)

/**
 * 航次存档列表 ViewModel：观察存档元信息（不含 payload，列表滚动不反序列化快照）、
 * 管理删除确认与删除动作。
 */
class ArchiveListViewModel(
    private val repository: VoyageArchiveRepository,
    // 与 WishlistViewModel 同一模式：测试注入 Unconfined 作用域，生产缺省走 viewModelScope
    coroutineScope: CoroutineScope? = null,
) : ViewModel() {

    private val scope: CoroutineScope = coroutineScope ?: viewModelScope

    /** 删除确认弹窗目标 id */
    private val pendingDeleteId = MutableStateFlow<Long?>(null)

    val state: StateFlow<ArchiveListUiState> = combine(
        repository.observeAll(),
        pendingDeleteId,
    ) { archives, pending ->
        ArchiveListUiState(archives = archives, pendingDeleteId = pending)
    }.stateIn(scope, SharingStarted.WhileSubscribed(5_000), ArchiveListUiState())

    /** 点击删除图标：挂起二次确认（记住目标 id，旋转不重复弹由 UI 层处理） */
    fun requestDelete(id: Long) {
        pendingDeleteId.value = id
    }

    /** 取消删除确认 */
    fun cancelDelete() {
        pendingDeleteId.value = null
    }

    /** 确认删除：仓库随行删除快照，不可恢复 */
    fun delete(id: Long) {
        pendingDeleteId.value = null
        scope.launch { repository.deleteArchive(id) }
    }
}

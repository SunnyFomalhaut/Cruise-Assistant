package com.cruise.assistant.ui.category

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.cruise.assistant.R
import com.cruise.assistant.data.CATEGORY_PALETTE
import com.cruise.assistant.data.repo.CategoryRepository
import com.cruise.assistant.domain.Category
import com.cruise.assistant.ui.common.ICON_REGISTRY
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch

/** 兜底类别（琐事）用图标 key 识别，而不是名字——名字可被用户改掉，图标 key 不可编辑、稳定。 */
private const val FALLBACK_ICON_KEY = "chore"

data class CategorySheetState(
    val editingId: Long = 0,
    val name: String = "",
    val colorArgb: Int = CATEGORY_PALETTE.first(),
    val iconKey: String = ICON_REGISTRY.first().first,
    val isSubCategory: Boolean = false,
    val parentId: Long? = null,
    val isArchived: Boolean = false,
    val isBuiltIn: Boolean = false,
    val allCategories: List<Category> = emptyList(),
    val eventCount: Int = 0,
    val errorResId: Int? = null,
    val saving: Boolean = false,
) {
    val isEditing: Boolean get() = editingId != 0L
    /** 可作为上级的类别：排除自己和已归档的 */
    val parentCandidates: List<Category>
        get() = allCategories.filter { it.id != editingId && !it.isArchived && it.parentId == null }
}

class CategoryViewModel(
    private val categoryRepository: CategoryRepository,
) : ViewModel() {

    private val _state = MutableStateFlow(CategorySheetState())
    val state: StateFlow<CategorySheetState> = _state.asStateFlow()

    init {
        viewModelScope.launch {
            categoryRepository.observeAll().collect { list ->
                _state.update { it.copy(allCategories = list) }
            }
        }
    }

    /** 新建：重置为默认色 + 默认图标 */
    fun startNew() = _state.update {
        CategorySheetState(
            allCategories = it.allCategories,
            colorArgb = CATEGORY_PALETTE.first(),
            iconKey = ICON_REGISTRY.first().first,
        )
    }

    fun startEdit(categoryId: Long) {
        viewModelScope.launch {
            val category = categoryRepository.getById(categoryId) ?: return@launch
            val count = categoryRepository.countEvents(categoryId)
            _state.update {
                it.copy(
                    editingId = category.id,
                    name = category.name,
                    colorArgb = category.colorArgb,
                    iconKey = category.iconKey,
                    isSubCategory = category.parentId != null,
                    parentId = category.parentId,
                    isArchived = category.isArchived,
                    isBuiltIn = category.isBuiltIn,
                    eventCount = count,
                    errorResId = null,
                )
            }
        }
    }

    fun onNameChange(value: String) = _state.update { it.copy(name = value, errorResId = null) }
    fun onColorSelect(argb: Int) = _state.update { it.copy(colorArgb = argb) }
    fun onIconSelect(key: String) = _state.update { it.copy(iconKey = key) }
    fun onArchiveToggle(value: Boolean) = _state.update { it.copy(isArchived = value) }

    fun onSubCategoryToggle(value: Boolean) = _state.update {
        if (value) {
            // 开二级分类时如果还没选上级，自动挑第一个候选，避免"开了开关但没选上级"
            // 时静默存成顶级类别。
            it.copy(isSubCategory = true, parentId = it.parentId ?: it.parentCandidates.firstOrNull()?.id)
        } else {
            it.copy(isSubCategory = false, parentId = null)
        }
    }

    fun onParentSelect(id: Long) = _state.update { it.copy(parentId = id, isSubCategory = true) }

    /**
     * 用 s.saving 挡快速二次点击：保存期间再次调用直接忽略，避免插入两次
     * （镜像 EventSheetViewModel.save 的做法）。
     */
    fun save(onDone: () -> Unit) {
        val s = _state.value
        if (s.saving) return
        if (s.name.isBlank()) {
            _state.update { it.copy(errorResId = R.string.error_title_required) }
            return
        }
        _state.update { it.copy(saving = true) }
        viewModelScope.launch {
            val existing = if (s.isEditing) categoryRepository.getById(s.editingId) else null
            val savedId = categoryRepository.save(
                Category(
                    id = s.editingId,
                    name = s.name.trim(),
                    colorArgb = s.colorArgb,
                    iconKey = s.iconKey,
                    parentId = if (s.isSubCategory) s.parentId else null,
                    isBuiltIn = existing?.isBuiltIn ?: false,
                    isArchived = s.isArchived,
                    sortOrder = existing?.sortOrder ?: (s.allCategories.size),
                ),
            )
            // 插入后把返回的新 id 记进 editingId：否则立刻再点一次"保存修改"
            // 会因 editingId 还是 0 而再插入一条重复行。
            _state.update { it.copy(saving = false, editingId = savedId) }
            onDone()
        }
    }

    /**
     * 内置类别不删；引用它的事件迁移到"琐事"。
     * 用 s.saving 挡快速二次点击（例如刚点了保存修改，紧接着点删除）。
     */
    fun delete(onDone: () -> Unit) {
        val s = _state.value
        if (s.saving) return
        if (!s.isEditing || s.isBuiltIn) return
        _state.update { it.copy(saving = true) }
        viewModelScope.launch {
            val category = categoryRepository.getById(s.editingId) ?: return@launch
            val others = categoryRepository.observeAll().first().filter { it.id != category.id }
            // 按 iconKey 识别兜底类别（琐事），而不是按名字——名字可被改掉，iconKey 不可编辑、稳定。
            // 只有在找不到时才退化为"随便挑第一个别的类别"，且该分支同样排除自己。
            val fallback = others.firstOrNull { it.iconKey == FALLBACK_ICON_KEY } ?: others.first()
            categoryRepository.delete(category, fallback.id)
            _state.update { it.copy(saving = false) }
            onDone()
        }
    }
}

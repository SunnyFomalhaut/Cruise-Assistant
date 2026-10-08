package com.cruise.assistant.ui.facilities

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.cruise.assistant.domain.FacilityItemUiModel
import com.cruise.assistant.domain.filterFacilities
import com.cruise.assistant.domain.getAllFacilityItems
import kotlinx.coroutines.delay
import kotlinx.coroutines.isActive
import kotlinx.coroutines.launch
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.stateIn
import java.time.LocalTime

data class FacilityGuideUiState(
    val searchQuery: String = "",
    val selectedCategory: String = "all",
    val onlyOpenNow: Boolean = false,
    val facilities: List<FacilityItemUiModel> = emptyList(),
    val totalCount: Int = 0,
)

data class CategoryFilterOption(
    val key: String,
    val label: String,
    val iconKey: String,
)

class FacilityGuideViewModel : ViewModel() {

    private val _searchQuery = MutableStateFlow("")
    private val _selectedCategory = MutableStateFlow("all")
    private val _onlyOpenNow = MutableStateFlow(false)

    // 所有可用分类
    val categories: List<CategoryFilterOption> = listOf(
        CategoryFilterOption("all", "全部", "sparkle"),
        CategoryFilterOption("meal", "餐食", "meal"),
        CategoryFilterOption("pool", "泳池娱乐", "pool"),
        CategoryFilterOption("shop", "商店", "shop"),
        CategoryFilterOption("youth", "青少年活动", "youth"),
        CategoryFilterOption("show", "演出场地", "show"),
        CategoryFilterOption("chore", "船上服务", "chore"),
    )

    private val allFacilities = MutableStateFlow(getAllFacilityItems())

    val state: StateFlow<FacilityGuideUiState> = combine(
        _searchQuery,
        _selectedCategory,
        _onlyOpenNow,
        allFacilities,
    ) { query, category, openNow, list ->
        val filtered = filterFacilities(list, query, category, openNow)
        FacilityGuideUiState(
            searchQuery = query,
            selectedCategory = category,
            onlyOpenNow = openNow,
            facilities = filtered,
            totalCount = filtered.size,
        )
    }.stateIn(
        scope = viewModelScope,
        started = SharingStarted.WhileSubscribed(5000),
        initialValue = FacilityGuideUiState(),
    )

    init {
        // allFacilities 里的 isOpen 是 VM 创建时刻用 LocalTime.now() 算的：页面开着跨过
        // 营业时间边界后（如 14:59 开到 15:01），"只看营业中"过滤会基于过期状态。
        // 这里每 60 秒用当前时间重算一次，不依赖用户手动刷新或重进页面。
        viewModelScope.launch {
            while (isActive) {
                delay(60_000)
                allFacilities.value = getAllFacilityItems(LocalTime.now())
            }
        }
    }

    fun refreshTime() {
        allFacilities.value = getAllFacilityItems(LocalTime.now())
    }

    fun onSearchQueryChange(query: String) {
        _searchQuery.value = query
    }

    fun onCategorySelect(categoryKey: String) {
        _selectedCategory.value = categoryKey
    }

    fun onToggleOnlyOpenNow(onlyOpen: Boolean) {
        _onlyOpenNow.value = onlyOpen
    }
}

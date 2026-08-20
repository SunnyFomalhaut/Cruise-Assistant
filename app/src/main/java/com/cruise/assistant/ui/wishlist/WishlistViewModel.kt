package com.cruise.assistant.ui.wishlist

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.cruise.assistant.data.repo.CategoryRepository
import com.cruise.assistant.data.repo.WishlistRepository
import com.cruise.assistant.domain.Category
import com.cruise.assistant.domain.ShipZone
import com.cruise.assistant.domain.WishlistItem
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch

data class WishlistUiState(
    val items: List<WishlistItem> = emptyList(),
    val categories: List<Category> = emptyList(),
    val selectedCategoryFilter: Long? = null,
    val completedCount: Int = 0,
    val totalCount: Int = 0,
)

class WishlistViewModel(
    private val wishlistRepository: WishlistRepository,
    private val categoryRepository: CategoryRepository,
) : ViewModel() {

    private val _selectedCategoryFilter = MutableStateFlow<Long?>(null)

    val state: StateFlow<WishlistUiState> = combine(
        wishlistRepository.observeAll(),
        categoryRepository.observeAll(),
        _selectedCategoryFilter,
    ) { allItems, categories, catFilter ->
        val filtered = if (catFilter == null) {
            allItems
        } else {
            allItems.filter { it.categoryId == catFilter }
        }
        val completed = allItems.count { it.isCompleted }
        WishlistUiState(
            items = filtered,
            categories = categories,
            selectedCategoryFilter = catFilter,
            completedCount = completed,
            totalCount = allItems.size,
        )
    }.stateIn(
        scope = viewModelScope,
        started = SharingStarted.WhileSubscribed(5000),
        initialValue = WishlistUiState(),
    )

    fun onCategoryFilterSelect(categoryId: Long?) {
        _selectedCategoryFilter.value = categoryId
    }

    fun toggleCompleted(item: WishlistItem) {
        viewModelScope.launch {
            wishlistRepository.toggleCompleted(item.id, !item.isCompleted)
        }
    }

    fun saveItem(
        id: Long,
        title: String,
        categoryId: Long,
        deck: Int?,
        zone: ShipZone?,
        placeName: String?,
        note: String?,
        onSuccess: () -> Unit = {},
    ) {
        viewModelScope.launch {
            val item = WishlistItem(
                id = id,
                title = title.trim(),
                categoryId = categoryId,
                deck = deck,
                zone = zone,
                placeName = placeName?.trim()?.takeIf { it.isNotEmpty() },
                note = note?.trim()?.takeIf { it.isNotEmpty() },
            )
            wishlistRepository.save(item)
            onSuccess()
        }
    }

    fun deleteItem(item: WishlistItem, onSuccess: () -> Unit = {}) {
        viewModelScope.launch {
            wishlistRepository.delete(item)
            onSuccess()
        }
    }
}

package com.cruise.assistant.ui.wishlist

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.cruise.assistant.data.repo.CategoryRepository
import com.cruise.assistant.data.repo.EventRepository
import com.cruise.assistant.data.repo.VoyageRepository
import com.cruise.assistant.data.repo.WishlistRepository
import com.cruise.assistant.domain.Category
import com.cruise.assistant.domain.CruiseEvent
import com.cruise.assistant.domain.ShipZone
import com.cruise.assistant.domain.WishlistItem
import com.cruise.assistant.domain.resolveDefaultWaitMinutes
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.emptyFlow
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch
import java.time.LocalDate
import java.time.LocalTime

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
    private val eventRepository: EventRepository? = null,
    private val voyageRepository: VoyageRepository? = null,
    coroutineScope: CoroutineScope? = null,
) : ViewModel() {

    private val scope: CoroutineScope = coroutineScope ?: viewModelScope

    private val _selectedCategoryFilter = MutableStateFlow<Long?>(null)

    val activeVoyageDates: StateFlow<List<LocalDate>> = (voyageRepository?.observeVoyage() ?: emptyFlow())
        .map { it?.datesInVoyage() ?: emptyList() }
        .stateIn(
            scope = scope,
            started = SharingStarted.WhileSubscribed(5000),
            initialValue = emptyList(),
        )

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
        scope = scope,
        started = SharingStarted.WhileSubscribed(5000),
        initialValue = WishlistUiState(),
    )

    fun onCategoryFilterSelect(categoryId: Long?) {
        _selectedCategoryFilter.value = categoryId
    }

    fun toggleCompleted(item: WishlistItem) {
        scope.launch {
            wishlistRepository.toggleCompleted(item.id, !item.isCompleted)
        }
    }

    fun convertToEvent(
        item: WishlistItem,
        date: LocalDate,
        startTime: LocalTime,
        durationMinutes: Int,
        onSuccess: () -> Unit = {},
    ) {
        val eventRepo = eventRepository ?: return
        scope.launch {
            val waitMinutes = resolveDefaultWaitMinutes(
                placeName = item.placeName,
                deck = item.deck,
                title = item.title,
                presetKey = null,
            )
            val event = CruiseEvent(
                date = date,
                startTime = startTime,
                durationMinutes = maxOf(15, durationMinutes),
                categoryId = item.categoryId,
                title = item.title,
                deck = item.deck,
                zone = item.zone,
                placeName = item.placeName,
                note = item.note,
                lane = 0,
                isSystemGenerated = false,
                waitMinutes = waitMinutes,
            )
            eventRepo.addEvent(event, recurring = false)
            wishlistRepository.toggleCompleted(item.id, true)
            onSuccess()
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
        scope.launch {
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
        scope.launch {
            wishlistRepository.delete(item)
            onSuccess()
        }
    }
}

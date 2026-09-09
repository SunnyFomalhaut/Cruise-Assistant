package com.cruise.assistant.ui.deckmap

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.cruise.assistant.data.repo.PixieDustRepository
import com.cruise.assistant.domain.DeckInfo
import com.cruise.assistant.domain.DeckMapRepository
import com.cruise.assistant.domain.PixieDustDirection
import com.cruise.assistant.domain.PixieRoomMarker
import com.cruise.assistant.domain.PixieRoomStatus
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.flowOf
import kotlinx.coroutines.flow.stateIn

data class DeckMapUiState(
    val selectedDeck: Int = 5,
    val decks: List<DeckInfo> = emptyList(),
    val hasMap: Boolean = true,
    val assetUrl: String = "",
    val highlightTarget: String? = null,
    val pixieMarkers: List<PixieRoomMarker> = emptyList(),
    val pixieMode: Boolean = false,
)

class DeckMapViewModel(
    initialDeck: Int = 5,
    initialHighlight: String? = null,
    initialPixieMode: Boolean = false,
    maxDeck: Int = DeckMapRepository.DEFAULT_MAX_DECK,
    private val pixieDustRepository: PixieDustRepository? = null,
    coroutineScope: CoroutineScope? = null,
) : ViewModel() {

    private val scope: CoroutineScope = coroutineScope ?: viewModelScope
    private val _selectedDeck = MutableStateFlow(
        if (DeckMapRepository.hasDeckMap(initialDeck)) initialDeck else 5
    )
    private val _highlightTarget = MutableStateFlow(initialHighlight)
    private val _decks = MutableStateFlow(DeckMapRepository.getDecks(maxDeck))
    private val _pixieMode = MutableStateFlow(initialPixieMode)

    val state: StateFlow<DeckMapUiState> = combine(
        _selectedDeck,
        _highlightTarget,
        _decks,
        _pixieMode,
        pixieDustRepository?.observeAll() ?: flowOf(emptyList()),
    ) { currentDeck, highlight, deckList, pixieMode, allRecords ->
        val hasMap = DeckMapRepository.hasDeckMap(currentDeck)
        val url = DeckMapRepository.assetUrlFor(currentDeck)
        val markers = if (pixieMode) {
            allRecords
                .filter { it.deck == currentDeck }
                .groupBy { it.room }
                .map { (room, records) ->
                    val unreplied = records.count { it.direction == PixieDustDirection.RECEIVED && !it.replied }
                    val givenCount = records.count { it.direction == PixieDustDirection.GIVEN }
                    val receivedCount = records.count { it.direction == PixieDustDirection.RECEIVED }
                    val status = when {
                        unreplied > 0 -> PixieRoomStatus.UNREPLIED
                        receivedCount > givenCount -> PixieRoomStatus.NEED_SUPPLEMENT
                        givenCount > receivedCount -> PixieRoomStatus.GIVEN_PENDING
                        else -> PixieRoomStatus.COMPLETED
                    }
                    PixieRoomMarker(
                        roomNumber = "$currentDeck$room",
                        status = status,
                        unrepliedCount = unreplied,
                        givenCount = givenCount,
                        receivedCount = receivedCount,
                    )
                }
        } else {
            emptyList()
        }
        DeckMapUiState(
            selectedDeck = currentDeck,
            highlightTarget = highlight,
            decks = deckList,
            hasMap = hasMap,
            assetUrl = url,
            pixieMarkers = markers,
            pixieMode = pixieMode,
        )
    }.stateIn(
        scope = scope,
        started = SharingStarted.WhileSubscribed(5000),
        initialValue = run {
            val initialSelected = if (DeckMapRepository.hasDeckMap(initialDeck)) initialDeck else 5
            val initialDecks = DeckMapRepository.getDecks(maxDeck)
            DeckMapUiState(
                selectedDeck = initialSelected,
                highlightTarget = initialHighlight,
                decks = initialDecks,
                hasMap = DeckMapRepository.hasDeckMap(initialSelected),
                assetUrl = DeckMapRepository.assetUrlFor(initialSelected),
                pixieMarkers = emptyList(),
                pixieMode = initialPixieMode,
            )
        },
    )

    fun onSelectDeck(deckNumber: Int) {
        _selectedDeck.value = deckNumber
    }

    fun navigateToFacility(deckNumber: Int, highlight: String) {
        _selectedDeck.value = deckNumber
        _highlightTarget.value = highlight
    }

    fun clearHighlight() {
        _highlightTarget.value = null
    }

    fun clearPixieMode() {
        _pixieMode.value = false
    }
}

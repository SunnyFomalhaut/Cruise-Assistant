package com.cruise.assistant.ui.deckmap

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.cruise.assistant.domain.DeckInfo
import com.cruise.assistant.domain.DeckMapRepository
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.stateIn

data class DeckMapUiState(
    val selectedDeck: Int = 5,
    val decks: List<DeckInfo> = emptyList(),
    val hasMap: Boolean = true,
    val assetUrl: String = "",
)

class DeckMapViewModel(
    initialDeck: Int = 5,
    maxDeck: Int = DeckMapRepository.DEFAULT_MAX_DECK,
) : ViewModel() {

    private val _selectedDeck = MutableStateFlow(
        if (DeckMapRepository.hasDeckMap(initialDeck)) initialDeck else 5
    )
    private val _decks = MutableStateFlow(DeckMapRepository.getDecks(maxDeck))

    val state: StateFlow<DeckMapUiState> = combine(
        _selectedDeck,
        _decks,
    ) { currentDeck, deckList ->
        val hasMap = DeckMapRepository.hasDeckMap(currentDeck)
        val url = DeckMapRepository.assetUrlFor(currentDeck)
        DeckMapUiState(
            selectedDeck = currentDeck,
            decks = deckList,
            hasMap = hasMap,
            assetUrl = url,
        )
    }.stateIn(
        scope = viewModelScope,
        started = SharingStarted.WhileSubscribed(5000),
        initialValue = DeckMapUiState(),
    )

    fun onSelectDeck(deckNumber: Int) {
        _selectedDeck.value = deckNumber
    }
}

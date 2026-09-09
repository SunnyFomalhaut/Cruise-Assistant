package com.cruise.assistant.ui.deckmap

import com.cruise.assistant.data.db.dao.PixieDustDao
import com.cruise.assistant.data.db.entity.PixieDustEntity
import com.cruise.assistant.data.repo.PixieDustRepository
import com.cruise.assistant.domain.PixieRoomStatus
import com.cruise.assistant.ui.Routes
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.launchIn
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test

class DeckMapViewModelTest {

    private class FakePixieDustDao(
        val flow: MutableStateFlow<List<PixieDustEntity>> = MutableStateFlow(emptyList()),
    ) : PixieDustDao {
        override fun observeAll(): Flow<List<PixieDustEntity>> = flow
        override suspend fun getById(id: Long): PixieDustEntity? = null
        override suspend fun insert(record: PixieDustEntity): Long = 1L
        override suspend fun update(record: PixieDustEntity) {}
        override suspend fun delete(record: PixieDustEntity) {}
        override suspend fun countByLevel(levelId: Long): Int = 0
        override suspend fun clearAll() {}
    }

    @Test
    fun testRoutesDeckMapPatternAndBuilder() {
        assertEquals(
            "deck_map?deck={deck}&highlight={highlight}&pixie={pixie}",
            Routes.DECK_MAP_PATTERN,
        )

        assertEquals("deck_map", Routes.deckMap())
        assertEquals("deck_map?deck=11", Routes.deckMap(deck = 11))
        assertEquals("deck_map?deck=11&pixie=true", Routes.deckMap(deck = 11, pixieMode = true))
    }

    @Test
    fun testDeckMapViewModelPixieMarkersComputation() {
        val records = listOf(
            // Deck 11 Room 216: 1 received unreplied -> UNREPLIED
            PixieDustEntity(
                id = 1,
                direction = "RECEIVED",
                deck = 11,
                room = "216",
                levelId = 1,
                levelNameSnapshot = "普通",
                replied = false,
                note = "",
                timestamp = 100,
            ),
            // Deck 11 Room 200: 2 received, 1 given -> NEED_SUPPLEMENT
            PixieDustEntity(
                id = 2,
                direction = "RECEIVED",
                deck = 11,
                room = "200",
                levelId = 1,
                levelNameSnapshot = "普通",
                replied = true,
                note = "",
                timestamp = 200,
            ),
            PixieDustEntity(
                id = 3,
                direction = "RECEIVED",
                deck = 11,
                room = "200",
                levelId = 1,
                levelNameSnapshot = "普通",
                replied = true,
                note = "",
                timestamp = 250,
            ),
            PixieDustEntity(
                id = 4,
                direction = "GIVEN",
                deck = 11,
                room = "200",
                levelId = 1,
                levelNameSnapshot = "普通",
                replied = false,
                note = "",
                timestamp = 300,
            ),
            // Deck 11 Room 220: 1 received (replied), 1 given -> COMPLETED
            PixieDustEntity(
                id = 5,
                direction = "RECEIVED",
                deck = 11,
                room = "220",
                levelId = 1,
                levelNameSnapshot = "普通",
                replied = true,
                note = "",
                timestamp = 400,
            ),
            PixieDustEntity(
                id = 6,
                direction = "GIVEN",
                deck = 11,
                room = "220",
                levelId = 1,
                levelNameSnapshot = "普通",
                replied = false,
                note = "",
                timestamp = 450,
            ),
            // Deck 6 Room 012: on Deck 6, should be ignored when Deck 11 is selected
            PixieDustEntity(
                id = 7,
                direction = "RECEIVED",
                deck = 6,
                room = "012",
                levelId = 1,
                levelNameSnapshot = "普通",
                replied = false,
                note = "",
                timestamp = 500,
            ),
        )

        val dao = FakePixieDustDao(MutableStateFlow(records))
        val repo = PixieDustRepository(dao)

        val vm = DeckMapViewModel(
            initialDeck = 11,
            initialPixieMode = true,
            pixieDustRepository = repo,
            coroutineScope = CoroutineScope(Dispatchers.Unconfined),
        )

        // Subscribe to StateFlow using Unconfined dispatcher
        val job = vm.state.launchIn(CoroutineScope(Dispatchers.Unconfined))

        val state = vm.state.value
        assertEquals(11, state.selectedDeck)
        assertTrue(state.pixieMode)
        assertEquals(3, state.pixieMarkers.size)

        val marker216 = state.pixieMarkers.first { it.roomNumber == "11216" }
        assertEquals(PixieRoomStatus.UNREPLIED, marker216.status)
        assertEquals(1, marker216.unrepliedCount)
        assertEquals(0, marker216.givenCount)
        assertEquals(1, marker216.receivedCount)

        val marker200 = state.pixieMarkers.first { it.roomNumber == "11200" }
        assertEquals(PixieRoomStatus.NEED_SUPPLEMENT, marker200.status)
        assertEquals(0, marker200.unrepliedCount)
        assertEquals(1, marker200.givenCount)
        assertEquals(2, marker200.receivedCount)

        val marker220 = state.pixieMarkers.first { it.roomNumber == "11220" }
        assertEquals(PixieRoomStatus.COMPLETED, marker220.status)
        assertEquals(0, marker220.unrepliedCount)
        assertEquals(1, marker220.givenCount)
        assertEquals(1, marker220.receivedCount)

        // Test clearPixieMode
        vm.clearPixieMode()
        val clearedState = vm.state.value
        assertFalse(clearedState.pixieMode)
        assertTrue(clearedState.pixieMarkers.isEmpty())

        job.cancel()
    }

    @Test
    fun testDeckMapViewModelDeckSwitchingUpdatesMarkers() {
        val records = listOf(
            PixieDustEntity(
                id = 1,
                direction = "RECEIVED",
                deck = 11,
                room = "216",
                levelId = 1,
                levelNameSnapshot = "普通",
                replied = false,
                note = "",
                timestamp = 100,
            ),
            PixieDustEntity(
                id = 2,
                direction = "RECEIVED",
                deck = 6,
                room = "012",
                levelId = 1,
                levelNameSnapshot = "普通",
                replied = false,
                note = "",
                timestamp = 200,
            ),
        )

        val dao = FakePixieDustDao(MutableStateFlow(records))
        val repo = PixieDustRepository(dao)

        val vm = DeckMapViewModel(
            initialDeck = 11,
            initialPixieMode = true,
            pixieDustRepository = repo,
            coroutineScope = CoroutineScope(Dispatchers.Unconfined),
        )

        val job = vm.state.launchIn(CoroutineScope(Dispatchers.Unconfined))

        assertEquals(1, vm.state.value.pixieMarkers.size)
        assertEquals("11216", vm.state.value.pixieMarkers[0].roomNumber)

        // Switch to deck 6
        vm.onSelectDeck(6)
        assertEquals(6, vm.state.value.selectedDeck)
        assertEquals(1, vm.state.value.pixieMarkers.size)
        assertEquals("6012", vm.state.value.pixieMarkers[0].roomNumber)

        job.cancel()
    }
}

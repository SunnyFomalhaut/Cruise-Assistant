package com.cruise.assistant.data.repo

import com.cruise.assistant.domain.Category
import com.cruise.assistant.domain.ShipZone
import com.cruise.assistant.domain.WishlistItem
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertNotNull
import org.junit.Assert.assertTrue
import org.junit.Test

class WishlistRepositoryTest {

    @Test
    fun `test WishlistItem formatted location and completion toggle`() {
        val item = WishlistItem(
            id = 1,
            title = "玩钢铁侠过山车",
            categoryId = 5,
            category = Category(id = 5, name = "娱乐", colorArgb = 0xFF0284C7.toInt(), iconKey = "fun"),
            deck = 18,
            zone = ShipZone.STERN,
            placeName = "旧京山街道",
            note = "记得带手机拍照",
            isCompleted = false,
        )

        assertEquals("Deck 18 船尾 旧京山街道", item.formattedLocation)
        assertFalse(item.isCompleted)

        val completedItem = item.copy(isCompleted = true)
        assertTrue(completedItem.isCompleted)
        assertNotNull(completedItem.category)
        assertEquals("娱乐", completedItem.category?.name)
    }
}

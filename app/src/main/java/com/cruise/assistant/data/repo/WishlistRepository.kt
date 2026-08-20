package com.cruise.assistant.data.repo

import com.cruise.assistant.data.db.dao.CategoryDao
import com.cruise.assistant.data.db.dao.WishlistDao
import com.cruise.assistant.data.db.toDomain
import com.cruise.assistant.data.db.toEntity
import com.cruise.assistant.domain.WishlistItem
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.combine

class WishlistRepository(
    private val wishlistDao: WishlistDao,
    private val categoryDao: CategoryDao,
) {
    fun observeAll(): Flow<List<WishlistItem>> {
        return combine(
            wishlistDao.observeAll(),
            categoryDao.observeAll(),
        ) { items, categories ->
            val catMap = categories.associate { it.id to it.toDomain() }
            items.map { it.toDomain(catMap[it.categoryId]) }
        }
    }

    suspend fun getById(id: Long): WishlistItem? {
        val entity = wishlistDao.getById(id) ?: return null
        val category = categoryDao.getById(entity.categoryId)?.toDomain()
        return entity.toDomain(category)
    }

    suspend fun save(item: WishlistItem): Long {
        return if (item.id == 0L) {
            wishlistDao.insert(item.toEntity())
        } else {
            wishlistDao.update(item.toEntity())
            item.id
        }
    }

    suspend fun delete(item: WishlistItem) {
        wishlistDao.delete(item.toEntity())
    }

    suspend fun toggleCompleted(id: Long, completed: Boolean) {
        wishlistDao.toggleCompleted(id, completed)
    }
}

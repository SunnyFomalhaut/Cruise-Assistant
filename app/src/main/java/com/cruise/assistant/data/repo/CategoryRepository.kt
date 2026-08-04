package com.cruise.assistant.data.repo

import com.cruise.assistant.data.db.dao.CategoryDao
import com.cruise.assistant.data.db.dao.EventDao
import com.cruise.assistant.data.db.toDomain
import com.cruise.assistant.data.db.toEntity
import com.cruise.assistant.domain.Category
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.map

class CategoryRepository(
    private val categoryDao: CategoryDao,
    private val eventDao: EventDao,
) {
    fun observeAll(): Flow<List<Category>> =
        categoryDao.observeAll().map { list -> list.map { it.toDomain() } }

    fun observeActive(): Flow<List<Category>> =
        categoryDao.observeActive().map { list -> list.map { it.toDomain() } }

    suspend fun getById(id: Long): Category? = categoryDao.getById(id)?.toDomain()

    /** id==0 插入，否则更新。返回类别 id。 */
    suspend fun save(category: Category): Long =
        if (category.id == 0L) {
            categoryDao.insert(category.toEntity())
        } else {
            categoryDao.update(category.toEntity())
            category.id
        }

    suspend fun countEvents(categoryId: Long): Int = eventDao.countByCategory(categoryId)

    /**
     * 删除自定义类别。内置类别不允许删除（静默忽略）。
     * 引用它的事件迁移到 fallbackCategoryId。
     */
    suspend fun delete(category: Category, fallbackCategoryId: Long) {
        if (category.isBuiltIn) return
        eventDao.reassignCategory(category.id, fallbackCategoryId)
        categoryDao.delete(category.toEntity())
    }
}

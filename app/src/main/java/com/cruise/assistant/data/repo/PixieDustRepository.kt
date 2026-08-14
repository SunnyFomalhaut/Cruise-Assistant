package com.cruise.assistant.data.repo

import com.cruise.assistant.data.db.dao.PixieDustDao
import com.cruise.assistant.data.db.toDomain
import com.cruise.assistant.data.db.toEntity
import com.cruise.assistant.domain.PixieDustRecord
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.map

class PixieDustRepository(private val dao: PixieDustDao) {
    fun observeAll(): Flow<List<PixieDustRecord>> =
        dao.observeAll().map { list -> list.map { it.toDomain() } }

    /** id==0 插入，否则更新。 */
    suspend fun save(record: PixieDustRecord): Long =
        if (record.id == 0L) {
            dao.insert(record.toEntity())
        } else {
            dao.update(record.toEntity())
            record.id
        }

    suspend fun delete(record: PixieDustRecord) = dao.delete(record.toEntity())

    suspend fun countByLevel(levelId: Long): Int = dao.countByLevel(levelId)

    suspend fun clearAll() = dao.clearAll()
}

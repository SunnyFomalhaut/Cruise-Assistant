package com.cruise.assistant.data.repo

import com.cruise.assistant.data.BUILT_IN_PIXIE_LEVELS
import com.cruise.assistant.data.db.dao.PixieLevelDao
import com.cruise.assistant.data.db.toDomain
import com.cruise.assistant.data.db.toEntity
import com.cruise.assistant.domain.PixieLevel
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.map

class PixieLevelRepository(private val dao: PixieLevelDao) {
    fun observeAll(): Flow<List<PixieLevel>> =
        dao.observeAll().map { list -> list.map { it.toDomain() } }

    /** id==0 插入，否则更新。 */
    suspend fun save(level: PixieLevel): Long =
        if (level.id == 0L) {
            dao.insert(level.toEntity())
        } else {
            dao.update(level.toEntity())
            level.id
        }

    suspend fun delete(level: PixieLevel) = dao.delete(level.toEntity())

    suspend fun count(): Int = dao.count()

    /** 表空时播种默认三级（幂等）。不按名字补缺——用户改名/删除内置等级后不能复活。 */
    suspend fun seedBuiltIns() {
        if (dao.count() == 0) {
            dao.insertAll(BUILT_IN_PIXIE_LEVELS.map { it.toEntity() })
        }
    }
}

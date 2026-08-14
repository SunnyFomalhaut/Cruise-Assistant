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

    /** 幂等补缺内置等级（按名字匹配），升级用户也能拿到默认三级 */
    suspend fun seedBuiltIns() {
        val existing = dao.observeAllOnce()
        val missing = BUILT_IN_PIXIE_LEVELS.filter { builtIn ->
            existing.none { it.name == builtIn.name }
        }
        if (missing.isNotEmpty()) dao.insertAll(missing.map { it.toEntity() })
    }
}

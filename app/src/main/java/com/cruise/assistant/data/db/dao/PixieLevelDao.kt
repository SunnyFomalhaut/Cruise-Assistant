package com.cruise.assistant.data.db.dao

import androidx.room.Dao
import androidx.room.Delete
import androidx.room.Insert
import androidx.room.Query
import androidx.room.Update
import com.cruise.assistant.data.db.entity.PixieLevelEntity
import kotlinx.coroutines.flow.Flow

@Dao
interface PixieLevelDao {
    @Query("SELECT * FROM pixie_level ORDER BY sortOrder, id")
    fun observeAll(): Flow<List<PixieLevelEntity>>

    @Query("SELECT * FROM pixie_level ORDER BY sortOrder, id")
    suspend fun observeAllOnce(): List<PixieLevelEntity>

    @Query("SELECT * FROM pixie_level WHERE id = :id")
    suspend fun getById(id: Long): PixieLevelEntity?

    @Insert
    suspend fun insert(level: PixieLevelEntity): Long

    @Insert
    suspend fun insertAll(levels: List<PixieLevelEntity>)

    @Update
    suspend fun update(level: PixieLevelEntity)

    @Delete
    suspend fun delete(level: PixieLevelEntity)

    @Query("SELECT COUNT(*) FROM pixie_level")
    suspend fun count(): Int

    @Query("DELETE FROM pixie_level")
    suspend fun clearAll()
}

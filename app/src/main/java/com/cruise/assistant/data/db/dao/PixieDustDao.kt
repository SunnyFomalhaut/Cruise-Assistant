package com.cruise.assistant.data.db.dao

import androidx.room.Dao
import androidx.room.Delete
import androidx.room.Insert
import androidx.room.Query
import androidx.room.Update
import com.cruise.assistant.data.db.entity.PixieDustEntity
import kotlinx.coroutines.flow.Flow

@Dao
interface PixieDustDao {
    @Query("SELECT * FROM pixie_dust ORDER BY timestamp DESC")
    fun observeAll(): Flow<List<PixieDustEntity>>

    @Query("SELECT * FROM pixie_dust WHERE id = :id")
    suspend fun getById(id: Long): PixieDustEntity?

    @Insert
    suspend fun insert(record: PixieDustEntity): Long

    @Update
    suspend fun update(record: PixieDustEntity)

    @Delete
    suspend fun delete(record: PixieDustEntity)

    @Query("SELECT COUNT(*) FROM pixie_dust WHERE levelId = :levelId")
    suspend fun countByLevel(levelId: Long): Int

    @Query("DELETE FROM pixie_dust")
    suspend fun clearAll()
}

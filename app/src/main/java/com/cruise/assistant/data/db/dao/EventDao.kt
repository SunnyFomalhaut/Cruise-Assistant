package com.cruise.assistant.data.db.dao

import androidx.room.Dao
import androidx.room.Delete
import androidx.room.Insert
import androidx.room.Query
import androidx.room.Update
import com.cruise.assistant.data.db.entity.EventEntity
import kotlinx.coroutines.flow.Flow
import java.time.LocalDate

@Dao
interface EventDao {
    @Query("SELECT * FROM event WHERE date = :date ORDER BY startTime, lane")
    fun observeByDate(date: LocalDate): Flow<List<EventEntity>>

    @Query("SELECT * FROM event WHERE date = :date ORDER BY startTime, lane")
    suspend fun getByDate(date: LocalDate): List<EventEntity>

    @Query("SELECT * FROM event ORDER BY date, startTime")
    suspend fun getAll(): List<EventEntity>

    @Query("SELECT * FROM event WHERE id = :id")
    suspend fun getById(id: Long): EventEntity?

    @Query("SELECT * FROM event WHERE recurrenceGroupId = :groupId ORDER BY date")
    suspend fun getByGroup(groupId: String): List<EventEntity>

    @Query("SELECT * FROM event WHERE presetKey = :key")
    suspend fun getByPresetKey(key: String): List<EventEntity>

    @Query("SELECT DISTINCT date FROM event ORDER BY date")
    fun observeDatesWithEvents(): Flow<List<LocalDate>>

    @Insert
    suspend fun insert(event: EventEntity): Long

    @Insert
    suspend fun insertAll(events: List<EventEntity>): List<Long>

    @Update
    suspend fun update(event: EventEntity)

    @Update
    suspend fun updateAll(events: List<EventEntity>)

    @Delete
    suspend fun delete(event: EventEntity)

    @Query("DELETE FROM event WHERE recurrenceGroupId = :groupId")
    suspend fun deleteGroup(groupId: String)

    @Query("UPDATE event SET categoryId = :toCategoryId WHERE categoryId = :fromCategoryId")
    suspend fun reassignCategory(fromCategoryId: Long, toCategoryId: Long)

    @Query("UPDATE event SET isActive = :active WHERE id = :id")
    suspend fun setActive(id: Long, active: Boolean)

    @Query("SELECT COUNT(*) FROM event WHERE categoryId = :categoryId")
    suspend fun countByCategory(categoryId: Long): Int

    @Query("DELETE FROM event")
    suspend fun clearAll()
}

package com.cruise.assistant.data.db.dao

import androidx.room.Dao
import androidx.room.Delete
import androidx.room.Query
import androidx.room.Upsert
import com.cruise.assistant.data.db.entity.LocationPresetEntity
import kotlinx.coroutines.flow.Flow

@Dao
interface LocationPresetDao {
    @Query("SELECT * FROM location_preset ORDER BY useCount DESC, lastUsedEpochMs DESC")
    fun observeAll(): Flow<List<LocationPresetEntity>>

    /**
     * 专门用于 [upsert] 前的查找：按自然键 (deck, zone, placeName) 定位已存在的记录，
     * 取得其真实 id 后再 `.copy(...)` 传给 [upsert]。见 [upsert] 的说明。
     */
    @Query("SELECT * FROM location_preset WHERE deck = :deck AND zone = :zone AND placeName = :placeName")
    suspend fun find(deck: Int, zone: String, placeName: String): LocationPresetEntity?

    /**
     * 调用前必须先用 [find] 按 (deck, zone, placeName) 查找：若已存在同一自然键的记录，
     * 必须把查到的实例用 `existing.copy(...)` 传入（保留其真实 id），
     * 不能直接传一个 id = 0 的新 [LocationPresetEntity]。
     *
     * 原因：Room 的 `@Upsert` 只按主键（id）判断冲突，不认识 (deck, zone, placeName)
     * 上的 unique index。当 id = 0 的新实例撞上该唯一索引时，Room 会捕获
     * UNIQUE constraint 异常并回退执行 `UPDATE ... WHERE id = ?`，但此时 id 仍是 0，
     * 匹配不到任何行 —— 写入会被静默丢弃（silently dropped），不抛异常、不报错。
     */
    @Upsert
    suspend fun upsert(preset: LocationPresetEntity)

    @Delete
    suspend fun delete(preset: LocationPresetEntity)

    @Query("DELETE FROM location_preset")
    suspend fun clearAll()
}

package com.cruise.assistant.data.db.dao

import androidx.room.Dao
import androidx.room.Insert
import androidx.room.Query
import com.cruise.assistant.data.db.entity.VoyageArchiveEntity
import com.cruise.assistant.data.db.entity.VoyageArchiveMeta
import kotlinx.coroutines.flow.Flow

@Dao
interface VoyageArchiveDao {
    /** 列表只查元信息列，不拉 payloadJson（快照可能很大）；新的在前 */
    @Query(
        "SELECT id, shipName, boardDate, disembarkDate, nightsSummary, createdAt " +
            "FROM voyage_archive ORDER BY createdAt DESC",
    )
    fun observeAll(): Flow<List<VoyageArchiveMeta>>

    @Query("SELECT * FROM voyage_archive WHERE id = :id")
    suspend fun getById(id: Long): VoyageArchiveEntity?

    @Insert
    suspend fun insert(entity: VoyageArchiveEntity): Long

    @Query("DELETE FROM voyage_archive WHERE id = :id")
    suspend fun deleteById(id: Long)
}

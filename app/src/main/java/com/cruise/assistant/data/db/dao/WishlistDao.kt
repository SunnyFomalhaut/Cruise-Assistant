package com.cruise.assistant.data.db.dao

import androidx.room.Dao
import androidx.room.Delete
import androidx.room.Insert
import androidx.room.OnConflictStrategy
import androidx.room.Query
import androidx.room.Update
import com.cruise.assistant.data.db.entity.WishlistItemEntity
import kotlinx.coroutines.flow.Flow

@Dao
interface WishlistDao {
    @Query("SELECT * FROM wishlist_item ORDER BY isCompleted ASC, createdAt DESC")
    fun observeAll(): Flow<List<WishlistItemEntity>>

    @Query("SELECT * FROM wishlist_item WHERE id = :id")
    suspend fun getById(id: Long): WishlistItemEntity?

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insert(item: WishlistItemEntity): Long

    @Update
    suspend fun update(item: WishlistItemEntity)

    @Delete
    suspend fun delete(item: WishlistItemEntity)

    @Query("UPDATE wishlist_item SET isCompleted = :completed WHERE id = :id")
    suspend fun toggleCompleted(id: Long, completed: Boolean)
}

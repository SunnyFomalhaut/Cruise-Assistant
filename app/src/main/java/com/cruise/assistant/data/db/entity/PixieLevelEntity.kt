package com.cruise.assistant.data.db.entity

import androidx.room.Entity
import androidx.room.PrimaryKey

@Entity(tableName = "pixie_level")
data class PixieLevelEntity(
    @PrimaryKey(autoGenerate = true) val id: Long = 0,
    val name: String,
    val colorArgb: Int,
    val iconKey: String,
    /** 越大越贵重 */
    val rank: Int,
    val sortOrder: Int,
)

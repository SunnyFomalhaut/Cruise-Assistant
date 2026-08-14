package com.cruise.assistant.data.db.entity

import androidx.room.Entity
import androidx.room.Index
import androidx.room.PrimaryKey

/** 单表存发出/收到，direction 字段区分（"GIVEN"/"RECEIVED"） */
@Entity(
    tableName = "pixie_dust",
    indices = [Index("direction"), Index("deck", "room")],
)
data class PixieDustEntity(
    @PrimaryKey(autoGenerate = true) val id: Long = 0,
    val direction: String,
    val deck: Int,
    val room: String,
    val levelId: Long,
    val levelNameSnapshot: String,
    val replied: Boolean,
    val note: String,
    val timestamp: Long,
)

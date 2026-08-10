package com.cruise.assistant.data.db.entity

import androidx.room.Entity
import androidx.room.Index
import androidx.room.PrimaryKey
import java.time.LocalDate
import java.time.LocalTime

@Entity(
    tableName = "event",
    indices = [Index("date"), Index("recurrenceGroupId")],
)
data class EventEntity(
    @PrimaryKey(autoGenerate = true) val id: Long = 0,
    val date: LocalDate,
    val startTime: LocalTime,
    val durationMinutes: Int,
    val categoryId: Long,
    val title: String,
    val deck: Int?,
    /** ShipZone.name，null 表示未选 */
    val zone: String?,
    val placeName: String?,
    val lane: Int,
    val recurrenceGroupId: String?,
    val reminderMinutesBefore: Int?,
    val isSystemGenerated: Boolean,
    val isActive: Boolean = true,
)

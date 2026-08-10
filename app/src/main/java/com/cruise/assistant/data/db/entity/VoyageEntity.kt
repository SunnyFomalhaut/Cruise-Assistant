package com.cruise.assistant.data.db.entity

import androidx.room.Entity
import androidx.room.PrimaryKey
import java.time.LocalDate
import java.time.LocalTime

@Entity(tableName = "voyage")
data class VoyageEntity(
    @PrimaryKey val id: Int = 1,
    val shipName: String?,
    val boardDate: LocalDate,
    val boardTime: LocalTime,
    val disembarkDate: LocalDate,
    val disembarkTime: LocalTime,
    val maxDeck: Int,
    /** 客用最低楼层，默认 1（无限制） */
    val minDeck: Int = 1,
)

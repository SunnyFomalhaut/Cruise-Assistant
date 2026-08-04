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
)

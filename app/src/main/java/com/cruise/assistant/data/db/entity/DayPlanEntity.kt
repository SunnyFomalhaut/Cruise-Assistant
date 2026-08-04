package com.cruise.assistant.data.db.entity

import androidx.room.Entity
import androidx.room.PrimaryKey
import java.time.LocalDate

@Entity(tableName = "day_plan")
data class DayPlanEntity(
    @PrimaryKey val date: LocalDate,
    val finalized: Boolean,
    val finalizedAt: Long?,
)

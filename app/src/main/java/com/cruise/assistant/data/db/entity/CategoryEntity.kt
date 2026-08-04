package com.cruise.assistant.data.db.entity

import androidx.room.Entity
import androidx.room.PrimaryKey

@Entity(tableName = "category")
data class CategoryEntity(
    @PrimaryKey(autoGenerate = true) val id: Long = 0,
    val name: String,
    val colorArgb: Int,
    val iconKey: String,
    val parentId: Long?,
    val isBuiltIn: Boolean,
    val isArchived: Boolean,
    val sortOrder: Int,
)

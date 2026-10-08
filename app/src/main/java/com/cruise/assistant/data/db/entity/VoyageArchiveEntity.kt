package com.cruise.assistant.data.db.entity

import androidx.room.Entity
import androidx.room.Index
import androidx.room.PrimaryKey
import java.time.LocalDate

/**
 * 航次存档表：一存档一行。
 * payloadJson 是 ArchiveSnapshot 的完整序列化（航次结束时全部业务表的精确快照，
 * 不随未来配置变化）；其余列是列表页要展示的冗余元信息，避免列表查询反序列化整个 payload。
 */
@Entity(
    tableName = "voyage_archive",
    indices = [Index("createdAt")],
)
data class VoyageArchiveEntity(
    @PrimaryKey(autoGenerate = true) val id: Long = 0,
    val shipName: String?,
    val boardDate: LocalDate,
    val disembarkDate: LocalDate,
    /** 列表展示用冗余文本：联程如 "3+4夜 · 共7晚"，单程如 "4夜" */
    val nightsSummary: String,
    /** 存档时刻 epoch ms */
    val createdAt: Long,
    val payloadJson: String,
)

/**
 * 存档列表页轻量投影（不含 payloadJson——快照可能很大，列表滚动不该反序列化它）。
 * 字段必须与 VoyageArchiveEntity 同名列一一对应。
 */
data class VoyageArchiveMeta(
    val id: Long,
    val shipName: String?,
    val boardDate: LocalDate,
    val disembarkDate: LocalDate,
    val nightsSummary: String,
    val createdAt: Long,
)

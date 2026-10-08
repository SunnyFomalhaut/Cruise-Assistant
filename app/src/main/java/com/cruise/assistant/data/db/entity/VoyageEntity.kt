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
    /** 礼宾（Concierge）客户：不受登船批次限制，11:00 即可优先登船 */
    val isConcierge: Boolean = false,
    /** 联程航次各程晚数的 JSON 数组文本（如 "[3,4]"）；null/空 = 单程 */
    val legNights: String? = null,
    /** 登岸兰卡威的程下标集合的 JSON int 数组文本（如 "[0,2]"）；null/空 = 无登岸程 */
    val ashoreLegs: String? = null,
)

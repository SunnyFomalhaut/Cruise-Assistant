package com.cruise.assistant.domain

/** 仙尘记录方向：发出（我给别间）或收到（别间给我） */
enum class PixieDustDirection { GIVEN, RECEIVED }

/** 礼物等级。rank 越大越贵重；sortOrder 是键盘/管理里的显示顺序。 */
data class PixieLevel(
    val id: Long = 0,
    val name: String,
    val colorArgb: Int,
    val iconKey: String,
    val rank: Int,
    val sortOrder: Int = 0,
)

data class PixieDustRecord(
    val id: Long = 0,
    val direction: PixieDustDirection,
    val deck: Int,
    val room: String,
    val levelId: Long,
    /** 等级被删后历史记录仍显示的名字 */
    val levelNameSnapshot: String,
    val replied: Boolean = false,
    val note: String = "",
    val timestamp: Long,
)

/** deck=11, room="550" → "11550" */
val PixieDustRecord.fullRoom: String get() = "$deck$room"

data class LevelCount(val level: PixieLevel, val count: Int)

/** 发出列表"按房间归组"视图的一行 */
data class RoomGroup(
    val deck: Int,
    val room: String,
    val lastTimestamp: Long,
    val total: Int,
    val levelCounts: List<LevelCount>,
)

/** 房号必须是恰好三位数字 */
fun isValidRoomInput(input: String): Boolean = input.length == 3 && input.all { it.isDigit() }

private const val DELETED_LEVEL_COLOR = 0xFF9E9E9E.toInt()

/** 按等级计数，rank 降序；等级已被删时用记录里的名称快照兜底成一个灰色等级 */
fun levelCountsOf(records: List<PixieDustRecord>, levels: List<PixieLevel>): List<LevelCount> {
    val byId = levels.associateBy { it.id }
    return records
        .groupBy { it.levelId }
        .map { (id, list) ->
            val level = byId[id] ?: PixieLevel(
                id = id,
                name = list.first().levelNameSnapshot,
                colorArgb = DELETED_LEVEL_COLOR,
                iconKey = "gift",
                rank = 0,
            )
            LevelCount(level, list.size)
        }
        .sortedByDescending { it.level.rank }
}

/** 发出记录按房间归组，房间按最近一次发出时间降序 */
fun groupGivenByRoom(records: List<PixieDustRecord>, levels: List<PixieLevel>): List<RoomGroup> =
    records
        .filter { it.direction == PixieDustDirection.GIVEN }
        .groupBy { it.deck to it.room }
        .map { (key, list) ->
            RoomGroup(
                deck = key.first,
                room = key.second,
                lastTimestamp = list.maxOf { it.timestamp },
                total = list.size,
                levelCounts = levelCountsOf(list, levels),
            )
        }
        .sortedByDescending { it.lastTimestamp }

/** 收到排序：未回礼在前按等级 rank 降序（贵重优先）、同级时间降序；已回礼沉底按时间降序 */
fun sortReceived(records: List<PixieDustRecord>, levels: List<PixieLevel>): List<PixieDustRecord> {
    val rank = levels.associate { it.id to it.rank }
    val unreplied = records
        .filter { it.direction == PixieDustDirection.RECEIVED && !it.replied }
        .sortedWith(compareByDescending<PixieDustRecord> { rank[it.levelId] ?: 0 }.thenByDescending { it.timestamp })
    val replied = records
        .filter { it.direction == PixieDustDirection.RECEIVED && it.replied }
        .sortedByDescending { it.timestamp }
    return unreplied + replied
}

/**
 * 扫楼回礼排序：优先按 Deck 升序、同层按 Room 数字升序；同房间内未回礼在先（高等级优先）、已回礼沉底
 */
fun sortForSweepDelivery(
    records: List<PixieDustRecord>,
    levels: List<PixieLevel>,
): List<PixieDustRecord> {
    val rank = levels.associate { it.id to it.rank }
    return records
        .sortedWith(
            compareBy<PixieDustRecord> { it.deck }
                .thenBy { it.room.toIntOrNull() ?: 0 }
                .thenBy { if (it.replied) 1 else 0 }
                .thenByDescending { rank[it.levelId] ?: 0 }
                .thenByDescending { it.timestamp }
        )
}

/** 该房间最近一次发出（用于"上次等级"预选与历史小字）。不含收到记录。 */
fun lastLevelForRoom(records: List<PixieDustRecord>, deck: Int, room: String): PixieDustRecord? =
    records
        .filter { it.direction == PixieDustDirection.GIVEN && it.deck == deck && it.room == room }
        .maxByOrNull { it.timestamp }

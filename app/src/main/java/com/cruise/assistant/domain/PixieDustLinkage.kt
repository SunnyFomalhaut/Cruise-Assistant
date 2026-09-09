package com.cruise.assistant.domain

/** 联动检视图的一行：一个房间的发出/收到配对 */
data class RoomPair(
    val deck: Int,
    val room: String,
    val given: List<PixieDustRecord>,
    val received: List<PixieDustRecord>,
    val unrepliedReceived: List<PixieDustRecord>,
    /** 双侧最大 timestamp，用于同组内排序 */
    val lastInteractTs: Long,
)

/**
 * 方向一联动：发出记录保存后，同房间未回礼的收到记录中最早（timestamp 最小）的一条。
 * 没有则返回 null（静默，无联动）。
 */
fun autoReplyTarget(received: List<PixieDustRecord>, deck: Int, room: String): PixieDustRecord? =
    received
        .filter {
            it.direction == PixieDustDirection.RECEIVED &&
                !it.replied && it.deck == deck && it.room == room
        }
        .minByOrNull { it.timestamp }

/**
 * 联动检视：按房间（deck+room）配对双侧记录。
 * 排序：① 有未回礼的最前（未回礼中最高等级 rank 降序——需要 levels 查 rank，
 * 调用方用 levelRankOf 传入；② 不平衡（发出数≠收到数）按最近互动倒序；
 * ③ 平衡按最近互动倒序。
 */
fun pairByRoom(
    given: List<PixieDustRecord>,
    received: List<PixieDustRecord>,
    levelRankOf: (PixieDustRecord) -> Int = { 0 },
): List<RoomPair> {
    val rooms = (given.map { it.deck to it.room } + received.map { it.deck to it.room }).toSet()
    val pairs = rooms.map { (deck, room) ->
        val g = given.filter { it.deck == deck && it.room == room }
        val r = received.filter { it.deck == deck && it.room == room }
        RoomPair(
            deck = deck,
            room = room,
            given = g,
            received = r,
            unrepliedReceived = r.filter { !it.replied },
            lastInteractTs = (g.map { it.timestamp } + r.map { it.timestamp }).max(),
        )
    }
    val hasUnreplied = pairs
        .filter { it.unrepliedReceived.isNotEmpty() }
        .sortedWith(
            compareByDescending<RoomPair> { it.unrepliedReceived.maxOfOrNull(levelRankOf) ?: -1 }
                .thenByDescending { it.lastInteractTs },
        )
    val unbalanced = pairs
        .filter { it.unrepliedReceived.isEmpty() && it.given.size != it.received.size }
        .sortedByDescending { it.lastInteractTs }
    val balanced = pairs
        .filter { it.unrepliedReceived.isEmpty() && it.given.size == it.received.size }
        .sortedByDescending { it.lastInteractTs }
    return hasUnreplied + unbalanced + balanced
}

/**
 * 按楼层和房号顺号配对，方便按楼层扫楼巡查
 */
fun pairByRoomForSweep(
    given: List<PixieDustRecord>,
    received: List<PixieDustRecord>,
    levelRankOf: (PixieDustRecord) -> Int = { 0 },
): List<RoomPair> {
    val rooms = (given.map { it.deck to it.room } + received.map { it.deck to it.room }).toSet()
    return rooms.map { (deck, room) ->
        val g = given.filter { it.deck == deck && it.room == room }
        val r = received.filter { it.deck == deck && it.room == room }
        RoomPair(
            deck = deck,
            room = room,
            given = g,
            received = r,
            unrepliedReceived = r.filter { !it.replied },
            lastInteractTs = (g.map { it.timestamp } + r.map { it.timestamp }).maxOrNull() ?: 0L,
        )
    }.sortedWith(
        compareBy<RoomPair> { it.deck }
            .thenBy { it.room.toIntOrNull() ?: 0 }
    )
}

/**
 * 方向一镜像：该房间是否已有发出记录。
 * 登记收到时判断要不要自动把刚登记的这条勾成已回礼（回礼动作在登记前已发生）。
 */
fun hasGivenForRoom(given: List<PixieDustRecord>, deck: Int, room: String): Boolean =
    given.any { it.direction == PixieDustDirection.GIVEN && it.deck == deck && it.room == room }

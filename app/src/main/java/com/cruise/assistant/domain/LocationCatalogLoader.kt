package com.cruise.assistant.domain

import org.json.JSONArray
import org.json.JSONObject
import java.time.LocalTime

/** 解析 locations.json 文本为地点列表；任何异常返回空列表（兜底不崩溃） */
fun parseLocationsJson(json: String): List<LocationCatalogEntry> = runCatching {
    val venues = JSONObject(json).getJSONArray("venues")
    buildList {
        for (i in 0 until venues.length()) {
            val v = venues.getJSONObject(i)
            val slots = v.optJSONArray("slots")
            val hours = if (slots == null || slots.length() == 0) {
                null
            } else {
                OperatingHours(buildList {
                    for (j in 0 until slots.length()) {
                        val s = slots.getJSONObject(j)
                        add(
                            OperatingSlot(
                                start = LocalTime.parse(s.getString("start")),
                                end = LocalTime.parse(s.getString("end")),
                                // Android 框架 org.json 对 JSON null 值 optString 返回字符串 "null"
                                //（JVM 库返回 null）——必须显式排除，否则 valueOf("NULL") 抛异常
                                // 导致整个目录解析失败（曾造成所有类别无预置地点）。
                                meal = s.optString("meal").takeIf { it.isNotEmpty() && it != "null" }
                                    ?.let { MealPeriod.valueOf(it.uppercase()) },
                                lateNight = s.optBoolean("lateNight", false),
                            ),
                        )
                    }
                })
            }
            val additionalDecks = v.optJSONArray("additionalDecks")?.let { arr ->
                buildList {
                    for (k in 0 until arr.length()) {
                        add(arr.getInt(k))
                    }
                }
            } ?: emptyList()
            add(
                LocationCatalogEntry(
                    deck = v.optInt("deck", -1).takeIf { it >= 0 },
                    zone = v.optString("zone").takeIf { it.isNotEmpty() && it != "null" }
                        ?.let { ShipZone.valueOf(it.uppercase()) },
                    placeName = v.getString("name"),
                    diningGroup = v.optString("group").takeIf { it.isNotEmpty() && it != "null" }
                        ?.let { DiningGroup.valueOf(it.uppercase()) },
                    category = v.optString("category", "meal"),
                    hours = hours,
                    additionalDecks = additionalDecks,
                ),
            )
        }
    }
}.getOrDefault(emptyList())

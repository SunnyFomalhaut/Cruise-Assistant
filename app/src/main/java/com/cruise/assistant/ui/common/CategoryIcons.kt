package com.cruise.assistant.ui.common

import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Search
import androidx.compose.material.icons.outlined.Apps
import androidx.compose.material.icons.outlined.AutoAwesome
import androidx.compose.material.icons.outlined.CardGiftcard
import androidx.compose.material.icons.outlined.Casino
import androidx.compose.material.icons.outlined.Celebration
import androidx.compose.material.icons.outlined.ChildCare
import androidx.compose.material.icons.outlined.CheckCircle
import androidx.compose.material.icons.outlined.Delete
import androidx.compose.material.icons.outlined.Diamond
import androidx.compose.material.icons.outlined.DirectionsBoat
import androidx.compose.material.icons.automirrored.outlined.DirectionsRun
import androidx.compose.material.icons.outlined.Favorite
import androidx.compose.material.icons.outlined.FitnessCenter
import androidx.compose.material.icons.outlined.Groups
import androidx.compose.material.icons.outlined.LocalBar
import androidx.compose.material.icons.automirrored.outlined.MenuBook
import androidx.compose.material.icons.outlined.NightsStay
import androidx.compose.material.icons.outlined.Person
import androidx.compose.material.icons.outlined.PhotoCamera
import androidx.compose.material.icons.outlined.PlayCircleOutline
import androidx.compose.material.icons.outlined.Pool
import androidx.compose.material.icons.outlined.Restaurant
import androidx.compose.material.icons.outlined.ShoppingBag
import androidx.compose.material.icons.outlined.Spa
import androidx.compose.material.icons.outlined.Star
import androidx.compose.material.icons.outlined.Warning
import androidx.compose.material.icons.outlined.WorkOutline
import androidx.compose.ui.graphics.vector.ImageVector

/** iconKey → 矢量图标。key 存库，图标可换而不迁移数据。 */
val ICON_REGISTRY: List<Pair<String, ImageVector>> = listOf(
    "meal" to Icons.Outlined.Restaurant,
    "show" to Icons.Outlined.PlayCircleOutline,
    "important" to Icons.Outlined.Warning,
    "chore" to Icons.Outlined.Apps,
    "fun" to Icons.Outlined.Casino,
    "meetup" to Icons.Outlined.Groups,
    "boat" to Icons.Outlined.DirectionsBoat,
    "pool" to Icons.Outlined.Pool,
    "bar" to Icons.Outlined.LocalBar,
    "spa" to Icons.Outlined.Spa,
    "gym" to Icons.Outlined.FitnessCenter,
    "run" to Icons.AutoMirrored.Outlined.DirectionsRun,
    "shop" to Icons.Outlined.ShoppingBag,
    "photo" to Icons.Outlined.PhotoCamera,
    "book" to Icons.AutoMirrored.Outlined.MenuBook,
    "party" to Icons.Outlined.Celebration,
    "sleep" to Icons.Outlined.NightsStay,
    "work" to Icons.Outlined.WorkOutline,
    "youth" to Icons.Outlined.ChildCare,
    "person" to Icons.Outlined.Person,
    "check" to Icons.Outlined.CheckCircle,
    "trash" to Icons.Outlined.Delete,
    "gift" to Icons.Outlined.CardGiftcard,
    "sparkle" to Icons.Outlined.AutoAwesome,
    "diamond" to Icons.Outlined.Diamond,
    "star" to Icons.Outlined.Star,
    "heart" to Icons.Outlined.Favorite,
    "search" to Icons.Filled.Search,
)

private val iconMap = ICON_REGISTRY.toMap()

/** 未知 key 兜底成"琐事"图标，绝不抛异常 */
fun iconFor(iconKey: String): ImageVector = iconMap[iconKey] ?: Icons.Outlined.Apps

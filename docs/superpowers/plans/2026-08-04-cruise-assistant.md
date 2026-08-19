# 邮轮助手 (Cruise Assistant) Implementation Plan

> **For agentic workers:** REQUIRED SUB-SKILL: Use superpowers:subagent-driven-development (recommended) or superpowers:executing-plans to implement this plan task-by-task. Steps use checkbox (`- [ ]`) syntax for tracking.

**Goal:** 一个纯本地 Android 应用，准备阶段用 3 栏时间轴编排邮轮每日行程，航行阶段变成单栏只读计划表并按时通知提醒。

**Architecture:** 单 module Compose 应用。Compose UI → ViewModel(StateFlow) → Repository → Room DAO / DataStore。所有布局与冲突计算逻辑抽到 `domain/` 纯 Kotlin 层，用 JVM 单测覆盖；时间轴与卡片全部自绘 Compose 布局（不用第三方日历库）。

**Tech Stack:** Kotlin 2.1.21 · Jetpack Compose (BOM 2026.06.01) · Material 3 · Room 2.8.4 + KSP · DataStore Preferences · AlarmManager · Gradle 8.13 / AGP 8.9.3

**Spec:** `docs/superpowers/specs/2026-08-04-cruise-assistant-design.md`

## Global Constraints

- 包名 `com.cruise.assistant`；单 module `:app`；无 DI 框架（手写 `AppContainer`）
- minSdk 26 · compileSdk 36 · targetSdk 36 · JVM target 17
- `gradle.properties` 必须含 `org.gradle.java.home=C\:\\Program Files\\Android\\Android Studio\\jbr`（系统 java 是 19，太旧）
- 版本全部锁定在 `gradle/libs.versions.toml`（版本目录），值见 Task 1
- 时间类型统一 `java.time`：`LocalDate` / `LocalTime`；Room 里存 ISO 字符串
- 禁止引入：网络库、图片加载库、图标包依赖、第三方日历/时间轴库
- 界面语言仅中文，所有用户可见文案走 `res/values/strings.xml`，代码中不硬编码中文字面量
- 不做跨日事件：`endTime` 必须 > `startTime` 且同日
- `keystore.properties` 和 `*.jks` 加入 `.gitignore`，本项目不生成密钥
- 每个 task 结束必须 commit；commit message 用 `feat:` / `test:` / `chore:` 前缀

## Verification Commands

工作目录 `D:\VibeCoding\Cruise-Assistant`，用 Git Bash：

```bash
./gradlew testDebugUnitTest          # 单元测试
./gradlew assembleDebug              # 构建 debug APK
./gradlew lintDebug                  # lint
```

---

## File Structure

```
app/src/main/java/com/cruise/assistant/
  CruiseApp.kt                      Application，持有 AppContainer
  AppContainer.kt                   手写依赖容器
  MainActivity.kt                   单 Activity + NavHost

  domain/                           纯 Kotlin，无 Android 依赖 —— 单测重点
    Models.kt                       Voyage/Category/CruiseEvent/DayPlan/ShipZone/LocationPreset
    LaneAssigner.kt                 assignLane / overlaps
    ConflictDetector.kt             findConflicts
    RecurrenceExpander.kt           expandRecurrence
    TimelineLayout.kt               buildTimelineLayout / minuteToDp / dpToMinute
    LocationFormatter.kt            formatLocation

  data/
    db/CruiseDatabase.kt            RoomDatabase
    db/Converters.kt                LocalDate/LocalTime/ShipZone TypeConverter
    db/entity/*.kt                  5 个 Entity
    db/dao/*.kt                     4 个 DAO
    prefs/SettingsStore.kt          DataStore 封装
    repo/VoyageRepository.kt        航次 + DayPlan + 引导播种
    repo/EventRepository.kt         事件 CRUD + 循环 + 换栏
    repo/CategoryRepository.kt      类别 CRUD
    repo/LocationPresetRepository.kt 地点记忆

  reminder/ReminderScheduler.kt     AlarmManager 排程
  reminder/ReminderReceiver.kt      闹钟接收 → 发通知
  reminder/BootReceiver.kt          开机重排
  reminder/NotificationHelper.kt    渠道 + 通知构建

  ui/theme/{Color,Theme,Type}.kt    主题
  ui/common/{TimeWheel,ColorDot,CategoryIcons}.kt  复用组件
  ui/onboarding/OnboardingScreen.kt + OnboardingViewModel.kt
  ui/timeline/TimelineScreen.kt     主页骨架
  ui/timeline/TimelineViewModel.kt
  ui/timeline/TimeRuler.kt          左侧时间刻度
  ui/timeline/EventCard.kt          三行卡片
  ui/timeline/LaneCanvas.kt         3 栏 / 单栏布局
  ui/timeline/DateStrip.kt          日期条
  ui/timeline/NowBanner.kt          进行中/下一个横幅
  ui/timeline/ConflictDialog.kt     定稿冲突弹窗
  ui/event/EventSheet.kt            添加/编辑事件弹窗
  ui/event/EventSheetViewModel.kt
  ui/event/LocationPicker.kt        楼层+区域+地点名
  ui/category/CategorySheet.kt      类别编辑弹窗
  ui/category/CategoryViewModel.kt
  ui/settings/SettingsScreen.kt + SettingsViewModel.kt
  ui/settings/DisplayRangePanel.kt  显示时间范围面板

app/src/test/java/com/cruise/assistant/domain/*Test.kt
```

## Task Dependency Order

```
1 构建骨架 ──> 2 domain 模型+纯逻辑 ──> 3 Room+DataStore ──> 4 Repository
                                                              │
   ┌──────────────────────────────────────────────────────────┤
   v                                                          v
5 主题+图标+复用组件 ──> 6 引导页 ──> 7 时间轴渲染 ──> 8 事件弹窗
                                          │                   │
                                          v                   v
                                   9 类别弹窗          10 设置+显示范围
                                          │
                                          v
                                   11 定稿流程 ──> 12 提醒 ──> 13 集成验证
```

---

## Task 1: 项目骨架与构建配置

**Files:**
- Create: `settings.gradle.kts`, `build.gradle.kts`, `gradle.properties`, `gradle/libs.versions.toml`
- Create: `gradle/wrapper/gradle-wrapper.properties`, `gradlew`, `gradlew.bat`, `gradle/wrapper/gradle-wrapper.jar`
- Create: `app/build.gradle.kts`, `app/proguard-rules.pro`
- Create: `app/src/main/AndroidManifest.xml`
- Create: `app/src/main/java/com/cruise/assistant/MainActivity.kt`
- Create: `app/src/main/res/values/strings.xml`, `res/values/themes.xml`
- Create: `.gitignore`

**Interfaces:**
- Consumes: 无
- Produces: 可构建的空 app；后续 task 依赖 `libs.versions.toml` 中的别名

- [ ] **Step 1: 生成 Gradle wrapper**

用本机已装的 Gradle 缓存生成 wrapper。先手写 `gradle/wrapper/gradle-wrapper.properties`：

```properties
distributionBase=GRADLE_USER_HOME
distributionPath=wrapper/dists
distributionUrl=https\://services.gradle.org/distributions/gradle-8.13-bin.zip
networkTimeout=10000
validateDistributionUrl=true
zipStoreBase=GRADLE_USER_HOME
zipStorePath=wrapper/dists
```

`gradlew` / `gradlew.bat` / `gradle-wrapper.jar` 从本机已有的 Gradle 发行版复制：

```bash
cd /d/VibeCoding/Cruise-Assistant
GRADLE_HOME=$(ls -d /c/Users/Administrator/.gradle/wrapper/dists/gradle-9.4.1-bin/*/gradle-9.4.1)
cp "$GRADLE_HOME/bin/gradle" /dev/null 2>/dev/null  # 仅确认路径存在
```

若上面找不到，直接用已缓存的 8.4 分发目录里的 wrapper 模板；最终以
`./gradlew --version` 能跑通为准。

- [ ] **Step 2: 写版本目录**

`gradle/libs.versions.toml` —— 版本全部选本机缓存中已有的，避免大量下载：

```toml
[versions]
agp = "8.9.3"
kotlin = "2.1.21"
ksp = "2.1.21-2.0.2"
composeBom = "2026.06.01"
room = "2.8.4"
coreKtx = "1.13.1"
lifecycle = "2.8.7"
activityCompose = "1.9.3"
navigation = "2.8.5"
datastore = "1.1.1"
junit = "4.13.2"

[libraries]
androidx-core-ktx = { module = "androidx.core:core-ktx", version.ref = "coreKtx" }
androidx-lifecycle-runtime-compose = { module = "androidx.lifecycle:lifecycle-runtime-compose", version.ref = "lifecycle" }
androidx-lifecycle-viewmodel-compose = { module = "androidx.lifecycle:lifecycle-viewmodel-compose", version.ref = "lifecycle" }
androidx-activity-compose = { module = "androidx.activity:activity-compose", version.ref = "activityCompose" }
androidx-navigation-compose = { module = "androidx.navigation:navigation-compose", version.ref = "navigation" }
androidx-compose-bom = { module = "androidx.compose:compose-bom", version.ref = "composeBom" }
androidx-compose-ui = { module = "androidx.compose.ui:ui" }
androidx-compose-ui-graphics = { module = "androidx.compose.ui:ui-graphics" }
androidx-compose-ui-tooling = { module = "androidx.compose.ui:ui-tooling" }
androidx-compose-ui-tooling-preview = { module = "androidx.compose.ui:ui-tooling-preview" }
androidx-compose-material3 = { module = "androidx.compose.material3:material3" }
androidx-compose-material-icons-extended = { module = "androidx.compose.material:material-icons-extended" }
androidx-room-runtime = { module = "androidx.room:room-runtime", version.ref = "room" }
androidx-room-ktx = { module = "androidx.room:room-ktx", version.ref = "room" }
androidx-room-compiler = { module = "androidx.room:room-compiler", version.ref = "room" }
androidx-datastore-preferences = { module = "androidx.datastore:datastore-preferences", version.ref = "datastore" }
junit = { module = "junit:junit", version.ref = "junit" }

[plugins]
android-application = { id = "com.android.application", version.ref = "agp" }
kotlin-android = { id = "org.jetbrains.kotlin.android", version.ref = "kotlin" }
kotlin-compose = { id = "org.jetbrains.kotlin.plugin.compose", version.ref = "kotlin" }
ksp = { id = "com.google.devtools.ksp", version.ref = "ksp" }
```

- [ ] **Step 3: 写根构建脚本与 gradle.properties**

`settings.gradle.kts`：

```kotlin
pluginManagement {
    repositories {
        google()
        mavenCentral()
        gradlePluginPortal()
    }
}
dependencyResolutionManagement {
    repositories {
        google()
        mavenCentral()
    }
}
rootProject.name = "CruiseAssistant"
include(":app")
```

`build.gradle.kts`（根）：

```kotlin
plugins {
    alias(libs.plugins.android.application) apply false
    alias(libs.plugins.kotlin.android) apply false
    alias(libs.plugins.kotlin.compose) apply false
    alias(libs.plugins.ksp) apply false
}
```

`gradle.properties`：

```properties
org.gradle.jvmargs=-Xmx3072m -Dfile.encoding=UTF-8
org.gradle.java.home=C\:\\Program Files\\Android\\Android Studio\\jbr
org.gradle.parallel=true
org.gradle.caching=true
android.useAndroidX=true
android.nonTransitiveRClass=true
kotlin.code.style=official
```

- [ ] **Step 4: 写 app 模块构建脚本**

`app/build.gradle.kts`：

```kotlin
import java.util.Properties

plugins {
    alias(libs.plugins.android.application)
    alias(libs.plugins.kotlin.android)
    alias(libs.plugins.kotlin.compose)
    alias(libs.plugins.ksp)
}

val keystorePropsFile = rootProject.file("keystore.properties")
val keystoreProps = Properties().apply {
    if (keystorePropsFile.exists()) keystorePropsFile.inputStream().use { load(it) }
}

android {
    namespace = "com.cruise.assistant"
    compileSdk = 36

    defaultConfig {
        applicationId = "com.cruise.assistant"
        minSdk = 26
        targetSdk = 36
        versionCode = 1
        versionName = "1.0"
    }

    signingConfigs {
        if (keystoreProps.isNotEmpty()) {
            create("release") {
                storeFile = file(keystoreProps.getProperty("storeFile"))
                storePassword = keystoreProps.getProperty("storePassword")
                keyAlias = keystoreProps.getProperty("keyAlias")
                keyPassword = keystoreProps.getProperty("keyPassword")
            }
        }
    }

    buildTypes {
        release {
            isMinifyEnabled = true
            proguardFiles(
                getDefaultProguardFile("proguard-android-optimize.txt"),
                "proguard-rules.pro"
            )
            if (keystoreProps.isNotEmpty()) {
                signingConfig = signingConfigs.getByName("release")
            }
        }
        debug {
            applicationIdSuffix = ".debug"
        }
    }

    compileOptions {
        sourceCompatibility = JavaVersion.VERSION_17
        targetCompatibility = JavaVersion.VERSION_17
    }
    kotlin {
        compilerOptions { jvmTarget.set(org.jetbrains.kotlin.gradle.dsl.JvmTarget.JVM_17) }
    }
    buildFeatures { compose = true }
    packaging { resources.excludes += "/META-INF/{AL2.0,LGPL2.1}" }
    ksp { arg("room.schemaLocation", "$projectDir/schemas") }
}

dependencies {
    implementation(libs.androidx.core.ktx)
    implementation(libs.androidx.lifecycle.runtime.compose)
    implementation(libs.androidx.lifecycle.viewmodel.compose)
    implementation(libs.androidx.activity.compose)
    implementation(libs.androidx.navigation.compose)
    implementation(platform(libs.androidx.compose.bom))
    implementation(libs.androidx.compose.ui)
    implementation(libs.androidx.compose.ui.graphics)
    implementation(libs.androidx.compose.ui.tooling.preview)
    implementation(libs.androidx.compose.material3)
    implementation(libs.androidx.compose.material.icons.extended)
    implementation(libs.androidx.room.runtime)
    implementation(libs.androidx.room.ktx)
    ksp(libs.androidx.room.compiler)
    implementation(libs.androidx.datastore.preferences)
    debugImplementation(libs.androidx.compose.ui.tooling)
    testImplementation(libs.junit)
}
```

`app/proguard-rules.pro`：

```
-keep class com.cruise.assistant.data.db.entity.** { *; }
```

- [ ] **Step 5: Manifest、空 Activity、主题、字符串**

`app/src/main/AndroidManifest.xml`：

```xml
<?xml version="1.0" encoding="utf-8"?>
<manifest xmlns:android="http://schemas.android.com/apk/res/android">
    <application
        android:name=".CruiseApp"
        android:allowBackup="true"
        android:label="@string/app_name"
        android:supportsRtl="false"
        android:theme="@style/Theme.CruiseAssistant">
        <activity
            android:name=".MainActivity"
            android:exported="true"
            android:windowSoftInputMode="adjustResize"
            android:theme="@style/Theme.CruiseAssistant">
            <intent-filter>
                <action android:name="android.intent.action.MAIN" />
                <category android:name="android.intent.category.LAUNCHER" />
            </intent-filter>
        </activity>
    </application>
</manifest>
```

`app/src/main/res/values/themes.xml`：

```xml
<?xml version="1.0" encoding="utf-8"?>
<resources>
    <style name="Theme.CruiseAssistant" parent="android:Theme.Material.Light.NoActionBar" />
</resources>
```

`app/src/main/res/values/strings.xml`：

```xml
<?xml version="1.0" encoding="utf-8"?>
<resources>
    <string name="app_name">邮轮助手</string>
</resources>
```

`CruiseApp.kt`（本 task 先放最简版，Task 4 再填 AppContainer）：

```kotlin
package com.cruise.assistant

import android.app.Application

class CruiseApp : Application()
```

`MainActivity.kt`：

```kotlin
package com.cruise.assistant

import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Text

class MainActivity : ComponentActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContent {
            MaterialTheme {
                Surface { Text("Cruise Assistant") }
            }
        }
    }
}
```

`.gitignore`：

```
*.iml
.gradle/
local.properties
.idea/
build/
captures/
.externalNativeBuild/
.cxx/
keystore.properties
*.jks
*.keystore
```

`local.properties`（不提交，但构建需要）：

```properties
sdk.dir=C\:\\Users\\Administrator\\AppData\\Local\\Android\\Sdk
```

- [ ] **Step 6: 验证构建通过**

```bash
cd /d/VibeCoding/Cruise-Assistant
./gradlew assembleDebug
```

Expected: `BUILD SUCCESSFUL`，产出 `app/build/outputs/apk/debug/app-debug.apk`

若报 wrapper jar 缺失，改用本机 Gradle 直接跑一次 `wrapper` 任务再重试。

- [ ] **Step 7: git init 并提交**

```bash
cd /d/VibeCoding/Cruise-Assistant
git init
git add -A
git commit -m "chore: scaffold Android Compose project with locked toolchain versions"
```

---

## Task 2: Domain 模型与纯逻辑（单测重点）

**Files:**
- Create: `domain/Models.kt`, `domain/LaneAssigner.kt`, `domain/ConflictDetector.kt`,
  `domain/RecurrenceExpander.kt`, `domain/TimelineLayout.kt`, `domain/LocationFormatter.kt`
- Test: `app/src/test/java/com/cruise/assistant/domain/` 下同名 `*Test.kt`

（以下路径均相对 `app/src/main/java/com/cruise/assistant/`）

**Interfaces:**
- Consumes: 无（纯 Kotlin，不依赖 Android）
- Produces:
  - `enum class ShipZone(val label: String) { BOW("船头"), MID("船中"), STERN("船尾") }`
  - `data class Voyage(id, shipName, boardDate, boardTime, disembarkDate, disembarkTime, maxDeck)`
    带 `dayCount` / `datesInVoyage()` / `containsDate(d)` / `dayIndexOf(d): Int?` / `clampDate(d): LocalDate`
  - `data class Category(id, name, colorArgb, iconKey, parentId, isBuiltIn, isArchived, sortOrder)`
  - `data class CruiseEvent(id, date, startTime, durationMinutes, categoryId, title, deck, zone, placeName, lane, recurrenceGroupId, reminderMinutesBefore, isSystemGenerated)`
  - `val CruiseEvent.startMinute: Int` / `val CruiseEvent.endMinute: Int`
  - `data class DayPlan(date, finalized, finalizedAt)`
  - `data class LocationPreset(id, deck, zone, placeName, lastUsedEpochMs, useCount)`
  - `const val LANE_COUNT = 3`
  - `fun overlaps(aStart: Int, aEnd: Int, bStart: Int, bEnd: Int): Boolean`
  - `fun assignLane(startMinute: Int, endMinute: Int, existing: List<CruiseEvent>, excludeId: Long? = null): Int`
  - `fun findConflicts(events: List<CruiseEvent>): List<Pair<CruiseEvent, CruiseEvent>>`
  - `fun expandRecurrence(template: CruiseEvent, disembarkDate: LocalDate, groupId: String): List<CruiseEvent>`
  - `data class TimelineLayout(hourHeightDp, rangeStartMinute, rangeEndMinute, segments, totalHeightDp)`
  - `data class Segment(startMinute, endMinute, topDp, heightDp, collapsed)`
  - `fun buildTimelineLayout(rangeStartMinute, rangeEndMinute, hourHeightDp, events, collapseEmptyGaps): TimelineLayout`
  - `fun TimelineLayout.minuteToDp(minute: Int): Float` / `dpToMinute(dp: Float): Int` / `hourTicks(): List<Pair<Int, Float>>`
  - `fun formatLocation(deck: Int?, zone: ShipZone?, placeName: String?): String?`

### Task 2a: Models.kt

- [ ] **Step 1: 写模型文件（纯数据，无需先写测试）**

`domain/Models.kt`：

```kotlin
package com.cruise.assistant.domain

import java.time.LocalDate
import java.time.LocalTime

/** 3 栏布局的栏数：栏0/栏1 等宽偏左，栏2 更窄贴最右 */
const val LANE_COUNT = 3

enum class ShipZone(val label: String) {
    BOW("船头"), MID("船中"), STERN("船尾")
}

data class Voyage(
    val id: Int = 1,
    val shipName: String? = null,
    val boardDate: LocalDate,
    val boardTime: LocalTime,
    val disembarkDate: LocalDate,
    val disembarkTime: LocalTime = LocalTime.of(7, 0),
    val maxDeck: Int = 20,
) {
    /** 航次总天数，含上船日和下船日 */
    val dayCount: Int
        get() = (disembarkDate.toEpochDay() - boardDate.toEpochDay()).toInt() + 1

    fun datesInVoyage(): List<LocalDate> =
        (0 until dayCount).map { boardDate.plusDays(it.toLong()) }

    fun containsDate(date: LocalDate): Boolean =
        !date.isBefore(boardDate) && !date.isAfter(disembarkDate)

    /** date 是第几天（1-based）；不在航次内返回 null */
    fun dayIndexOf(date: LocalDate): Int? =
        if (containsDate(date)) (date.toEpochDay() - boardDate.toEpochDay()).toInt() + 1 else null

    /** 把任意日期夹到航次范围内，用于启动时定位 */
    fun clampDate(date: LocalDate): LocalDate = when {
        date.isBefore(boardDate) -> boardDate
        date.isAfter(disembarkDate) -> disembarkDate
        else -> date
    }
}

data class Category(
    val id: Long = 0,
    val name: String,
    val colorArgb: Int,
    val iconKey: String,
    val parentId: Long? = null,
    val isBuiltIn: Boolean = false,
    val isArchived: Boolean = false,
    val sortOrder: Int = 0,
)

data class CruiseEvent(
    val id: Long = 0,
    val date: LocalDate,
    val startTime: LocalTime,
    val durationMinutes: Int,
    val categoryId: Long,
    val title: String,
    val deck: Int? = null,
    val zone: ShipZone? = null,
    val placeName: String? = null,
    val lane: Int = 0,
    val recurrenceGroupId: String? = null,
    val reminderMinutesBefore: Int? = null,
    val isSystemGenerated: Boolean = false,
)

val CruiseEvent.startMinute: Int get() = startTime.hour * 60 + startTime.minute
val CruiseEvent.endMinute: Int get() = startMinute + durationMinutes

data class DayPlan(
    val date: LocalDate,
    val finalized: Boolean = false,
    val finalizedAt: Long? = null,
)

data class LocationPreset(
    val id: Long = 0,
    val deck: Int,
    val zone: ShipZone,
    val placeName: String,
    val lastUsedEpochMs: Long = 0,
    val useCount: Int = 0,
)
```

- [ ] **Step 2: 编译验证**

Run: `./gradlew compileDebugKotlin`
Expected: `BUILD SUCCESSFUL`

- [ ] **Step 3: Commit**

```bash
git add app/src/main/java/com/cruise/assistant/domain/Models.kt
git commit -m "feat: add domain models for voyage, category, event, day plan"
```

### Task 2b: LaneAssigner —— 需求4 的 3 栏分配

- [ ] **Step 1: 写失败的测试**

`app/src/test/java/com/cruise/assistant/domain/LaneAssignerTest.kt`：

```kotlin
package com.cruise.assistant.domain

import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test
import java.time.LocalDate
import java.time.LocalTime

class LaneAssignerTest {

    private val day = LocalDate.of(2026, 9, 3)

    private fun ev(id: Long, hour: Int, minute: Int, durMin: Int, lane: Int) = CruiseEvent(
        id = id, date = day, startTime = LocalTime.of(hour, minute),
        durationMinutes = durMin, categoryId = 1, title = "e" + id, lane = lane,
    )

    @Test
    fun overlaps_isTrue_whenRangesIntersect() {
        assertTrue(overlaps(100, 200, 150, 250))
        assertTrue(overlaps(150, 250, 100, 200))
        assertTrue(overlaps(100, 300, 150, 200)) // 完全包含
    }

    @Test
    fun adjacentRanges_doNotOverlap() {
        // 13:00-13:30 与 13:30-14:00 相邻，不算冲突
        assertFalse(overlaps(780, 810, 810, 840))
        assertFalse(overlaps(810, 840, 780, 810))
    }

    @Test
    fun identicalRanges_overlap() {
        assertTrue(overlaps(780, 810, 780, 810))
    }

    @Test
    fun firstEvent_goesToLane0() {
        assertEquals(0, assignLane(780, 810, emptyList()))
    }

    @Test
    fun nonOverlappingEvent_reusesLane0() {
        val existing = listOf(ev(1, 13, 0, 30, lane = 0))
        // 14:00-14:30 与栏0 的 13:00-13:30 不重叠
        assertEquals(0, assignLane(840, 870, existing))
    }

    @Test
    fun overlappingEvent_fallsToLane1() {
        val existing = listOf(ev(1, 13, 0, 60, lane = 0))
        // 13:15-13:45 与栏0 重叠
        assertEquals(1, assignLane(795, 825, existing))
    }

    @Test
    fun twoOverlappingEvents_pushThirdToNarrowLane2() {
        val existing = listOf(
            ev(1, 13, 0, 60, lane = 0),
            ev(2, 13, 10, 60, lane = 1),
        )
        assertEquals(2, assignLane(800, 830, existing))
    }

    @Test
    fun allThreeLanesBusy_returnsLane2AsOverflow() {
        val existing = listOf(
            ev(1, 13, 0, 60, lane = 0),
            ev(2, 13, 0, 60, lane = 1),
            ev(3, 13, 0, 60, lane = 2),
        )
        assertEquals(LANE_COUNT - 1, assignLane(790, 820, existing))
    }

    @Test
    fun excludeId_letsEventKeepItsOwnLane_whenEditing() {
        val existing = listOf(ev(7, 13, 0, 60, lane = 0))
        // 编辑事件 7 本身，应忽略自己 → 仍可回到栏0
        assertEquals(0, assignLane(780, 840, existing, excludeId = 7L))
    }

    @Test
    fun higherLaneEvents_areIgnored_whenLowerLaneIsFree() {
        val existing = listOf(ev(1, 13, 0, 60, lane = 2))
        assertEquals(0, assignLane(780, 840, existing))
    }
}
```

- [ ] **Step 2: 跑测试确认失败**

Run: `./gradlew testDebugUnitTest --tests "*LaneAssignerTest*"`
Expected: FAIL —— `Unresolved reference: overlaps` / `assignLane`

- [ ] **Step 3: 实现**

`domain/LaneAssigner.kt`：

```kotlin
package com.cruise.assistant.domain

/**
 * 半开区间重叠判定：[aStart, aEnd) 与 [bStart, bEnd)。
 * 相邻（一个的结束等于另一个的开始）不算重叠。
 * 空区间（start == end，如零时长事件）不占据任何时间，永远不算重叠。
 */
fun overlaps(aStart: Int, aEnd: Int, bStart: Int, bEnd: Int): Boolean =
    aStart < aEnd && bStart < bEnd && aStart < bEnd && bStart < aEnd

/**
 * 需求4：为新事件挑选栏位。依次尝试栏 0 → 1 → 2，
 * 返回第一个与该栏已有事件都不重叠的栏号。
 * 三栏都冲突时返回最后一栏（窄栏）作为溢出位。
 *
 * @param excludeId 编辑已有事件时传入其 id，避免与自己比较。
 */
fun assignLane(
    startMinute: Int,
    endMinute: Int,
    existing: List<CruiseEvent>,
    excludeId: Long? = null,
): Int {
    val candidates = existing.filter { it.id != excludeId }
    for (lane in 0 until LANE_COUNT) {
        val busy = candidates.any { other ->
            other.lane == lane &&
                overlaps(startMinute, endMinute, other.startMinute, other.endMinute)
        }
        if (!busy) return lane
    }
    return LANE_COUNT - 1
}
```

- [ ] **Step 4: 跑测试确认通过**

Run: `./gradlew testDebugUnitTest --tests "*LaneAssignerTest*"`
Expected: PASS，10 个测试全绿

- [ ] **Step 5: Commit**

```bash
git add app/src/main/java/com/cruise/assistant/domain/LaneAssigner.kt app/src/test/java/com/cruise/assistant/domain/LaneAssignerTest.kt
git commit -m "feat: add three-lane assignment for overlapping events"
```

### Task 2c: ConflictDetector —— 需求8 的冲突检测

- [ ] **Step 1: 写失败的测试**

`app/src/test/java/com/cruise/assistant/domain/ConflictDetectorTest.kt`：

```kotlin
package com.cruise.assistant.domain

import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Test
import java.time.LocalDate
import java.time.LocalTime

class ConflictDetectorTest {

    private val day = LocalDate.of(2026, 9, 3)

    private fun ev(id: Long, hour: Int, minute: Int, durMin: Int) = CruiseEvent(
        id = id, date = day, startTime = LocalTime.of(hour, minute),
        durationMinutes = durMin, categoryId = 1, title = "e" + id,
    )

    @Test
    fun emptyList_hasNoConflicts() {
        assertTrue(findConflicts(emptyList()).isEmpty())
    }

    @Test
    fun singleEvent_hasNoConflicts() {
        assertTrue(findConflicts(listOf(ev(1, 13, 0, 30))).isEmpty())
    }

    @Test
    fun sequentialEvents_haveNoConflicts() {
        val events = listOf(ev(1, 12, 0, 30), ev(2, 13, 0, 30), ev(3, 14, 0, 30))
        assertTrue(findConflicts(events).isEmpty())
    }

    @Test
    fun backToBackEvents_haveNoConflicts() {
        // 13:00-13:30 紧接 13:30-14:00
        val events = listOf(ev(1, 13, 0, 30), ev(2, 13, 30, 30))
        assertTrue(findConflicts(events).isEmpty())
    }

    @Test
    fun overlappingPair_isReportedOnce() {
        val events = listOf(ev(1, 13, 0, 45), ev(2, 13, 20, 60))
        val conflicts = findConflicts(events)
        assertEquals(1, conflicts.size)
        assertEquals(1L, conflicts[0].first.id)
        assertEquals(2L, conflicts[0].second.id)
    }

    @Test
    fun threeMutuallyOverlappingEvents_yieldThreePairs() {
        val events = listOf(ev(1, 13, 0, 60), ev(2, 13, 10, 60), ev(3, 13, 20, 60))
        assertEquals(3, findConflicts(events).size)
    }

    @Test
    fun conflicts_areOrderedByStartTime() {
        val events = listOf(ev(2, 15, 0, 60), ev(1, 13, 0, 60), ev(3, 15, 30, 60))
        val conflicts = findConflicts(events)
        assertEquals(1, conflicts.size)
        // 先开始的排在 first
        assertEquals(2L, conflicts[0].first.id)
        assertEquals(3L, conflicts[0].second.id)
    }

    @Test
    fun identicalEvents_conflict() {
        val events = listOf(ev(1, 13, 0, 30), ev(2, 13, 0, 30))
        assertEquals(1, findConflicts(events).size)
    }

    @Test
    fun lane_doesNotAffectConflictDetection() {
        // 分在不同栏只是显示上不重叠，时间上仍然算冲突
        val events = listOf(
            ev(1, 13, 0, 60).copy(lane = 0),
            ev(2, 13, 10, 60).copy(lane = 1),
        )
        assertEquals(1, findConflicts(events).size)
    }

    @Test
    fun zeroDurationEvent_neverConflicts() {
        val events = listOf(ev(1, 13, 0, 60), ev(2, 13, 30, 0))
        assertTrue(findConflicts(events).isEmpty())
    }
}
```

- [ ] **Step 2: 跑测试确认失败**

Run: `./gradlew testDebugUnitTest --tests "*ConflictDetectorTest*"`
Expected: FAIL —— `Unresolved reference: findConflicts`

- [ ] **Step 3: 实现**

`domain/ConflictDetector.kt`：

```kotlin
package com.cruise.assistant.domain

/**
 * 需求8：找出同一天内所有时间重叠的事件对。
 * 每对只报一次，按开始时间排序后 first 早于（或同时但 id 更小）second。
 * 相邻（前一个结束 == 后一个开始）不算冲突。
 */
fun findConflicts(events: List<CruiseEvent>): List<Pair<CruiseEvent, CruiseEvent>> {
    val sorted = events.sortedWith(compareBy({ it.startMinute }, { it.id }))
    val result = mutableListOf<Pair<CruiseEvent, CruiseEvent>>()
    for (i in sorted.indices) {
        val a = sorted[i]
        for (j in i + 1 until sorted.size) {
            val b = sorted[j]
            // 排序后 b.startMinute >= a.startMinute，一旦不早于 a 的结束就不可能再重叠
            if (b.startMinute >= a.endMinute) break
            if (overlaps(a.startMinute, a.endMinute, b.startMinute, b.endMinute)) {
                result += a to b
            }
        }
    }
    return result
}
```

- [ ] **Step 4: 跑测试确认通过**

Run: `./gradlew testDebugUnitTest --tests "*ConflictDetectorTest*"`
Expected: PASS，10 个测试全绿

- [ ] **Step 5: Commit**

```bash
git add app/src/main/java/com/cruise/assistant/domain/ConflictDetector.kt app/src/test/java/com/cruise/assistant/domain/ConflictDetectorTest.kt
git commit -m "feat: add conflict detection for daily events"
```

### Task 2d: RecurrenceExpander —— 需求6 的每日循环（下船日排除）

- [ ] **Step 1: 写失败的测试**

`app/src/test/java/com/cruise/assistant/domain/RecurrenceExpanderTest.kt`：

```kotlin
package com.cruise.assistant.domain

import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Test
import java.time.LocalDate
import java.time.LocalTime

class RecurrenceExpanderTest {

    private val disembark = LocalDate.of(2026, 9, 8)

    private fun template(date: LocalDate) = CruiseEvent(
        id = 0, date = date, startTime = LocalTime.of(7, 30),
        durationMinutes = 45, categoryId = 1, title = "早餐",
        deck = 5, zone = ShipZone.MID, placeName = "自助餐厅",
    )

    @Test
    fun expandsFromTemplateDate_toDayBeforeDisembark() {
        // 9/3 创建，下船日 9/8 → 生成 9/3,9/4,9/5,9/6,9/7（9/8 不含）
        val result = expandRecurrence(template(LocalDate.of(2026, 9, 3)), disembark, "g1")
        assertEquals(5, result.size)
        assertEquals(LocalDate.of(2026, 9, 3), result.first().date)
        assertEquals(LocalDate.of(2026, 9, 7), result.last().date)
    }

    @Test
    fun disembarkDate_isNeverIncluded() {
        val result = expandRecurrence(template(LocalDate.of(2026, 9, 3)), disembark, "g1")
        assertTrue(result.none { it.date == disembark })
    }

    @Test
    fun templateOnDayBeforeDisembark_yieldsSingleEvent() {
        val result = expandRecurrence(template(LocalDate.of(2026, 9, 7)), disembark, "g1")
        assertEquals(1, result.size)
        assertEquals(LocalDate.of(2026, 9, 7), result[0].date)
    }

    @Test
    fun templateOnDisembarkDate_yieldsSingleEventOnThatDate() {
        // 用户在下船日当天手动创建的事件不应被循环吞掉，至少保留自己
        val result = expandRecurrence(template(disembark), disembark, "g1")
        assertEquals(1, result.size)
        assertEquals(disembark, result[0].date)
    }

    @Test
    fun allCopiesShareGroupId() {
        val result = expandRecurrence(template(LocalDate.of(2026, 9, 3)), disembark, "grp-42")
        assertTrue(result.all { it.recurrenceGroupId == "grp-42" })
    }

    @Test
    fun allCopiesPreserveTimeDurationAndLocation() {
        val result = expandRecurrence(template(LocalDate.of(2026, 9, 3)), disembark, "g1")
        assertTrue(result.all { it.startTime == LocalTime.of(7, 30) })
        assertTrue(result.all { it.durationMinutes == 45 })
        assertTrue(result.all { it.deck == 5 && it.zone == ShipZone.MID })
        assertTrue(result.all { it.placeName == "自助餐厅" })
        assertTrue(result.all { it.title == "早餐" })
    }

    @Test
    fun allCopiesHaveZeroId_soRoomAutoGenerates() {
        val result = expandRecurrence(
            template(LocalDate.of(2026, 9, 3)).copy(id = 99), disembark, "g1",
        )
        assertTrue(result.all { it.id == 0L })
    }

    @Test
    fun templateAfterDisembark_yieldsSingleEvent() {
        val late = disembark.plusDays(3)
        val result = expandRecurrence(template(late), disembark, "g1")
        assertEquals(1, result.size)
        assertEquals(late, result[0].date)
    }
}
```

- [ ] **Step 2: 跑测试确认失败**

Run: `./gradlew testDebugUnitTest --tests "*RecurrenceExpanderTest*"`
Expected: FAIL —— `Unresolved reference: expandRecurrence`

- [ ] **Step 3: 实现**

`domain/RecurrenceExpander.kt`：

```kotlin
package com.cruise.assistant.domain

import java.time.LocalDate

/**
 * 需求6：把一个事件按"每日同一时间"展开。
 *
 * 范围是 template.date 起、到下船日的前一天为止 —— 下船日只有一件"下船"，
 * 不生成循环项（用户明确要求）。
 *
 * 若 template.date 已经是下船日或更晚，只返回它自己一份，避免用户手建的事件被吞掉。
 * 所有副本 id = 0，交给 Room 自增；共享同一个 recurrenceGroupId。
 */
fun expandRecurrence(
    template: CruiseEvent,
    disembarkDate: LocalDate,
    groupId: String,
): List<CruiseEvent> {
    val base = template.copy(id = 0, recurrenceGroupId = groupId)
    val lastDate = disembarkDate.minusDays(1)
    if (template.date.isAfter(lastDate)) return listOf(base)

    val dayCount = (lastDate.toEpochDay() - template.date.toEpochDay()).toInt() + 1
    return (0 until dayCount).map { offset ->
        base.copy(date = template.date.plusDays(offset.toLong()))
    }
}
```

- [ ] **Step 4: 跑测试确认通过**

Run: `./gradlew testDebugUnitTest --tests "*RecurrenceExpanderTest*"`
Expected: PASS，8 个测试全绿

- [ ] **Step 5: Commit**

```bash
git add app/src/main/java/com/cruise/assistant/domain/RecurrenceExpander.kt app/src/test/java/com/cruise/assistant/domain/RecurrenceExpanderTest.kt
git commit -m "feat: add daily recurrence expansion excluding disembark day"
```

### Task 2e: LocationFormatter —— 需求3 的地点显示

- [ ] **Step 1: 写失败的测试**

`app/src/test/java/com/cruise/assistant/domain/LocationFormatterTest.kt`：

```kotlin
package com.cruise.assistant.domain

import org.junit.Assert.assertEquals
import org.junit.Assert.assertNull
import org.junit.Test

class LocationFormatterTest {

    @Test
    fun fullLocation_isDeckZonePlace() {
        assertEquals("7F船中花园", formatLocation(7, ShipZone.MID, "花园"))
        assertEquals("9F船尾剧场", formatLocation(9, ShipZone.STERN, "剧场"))
        assertEquals("3F船头健身房", formatLocation(3, ShipZone.BOW, "健身房"))
    }

    @Test
    fun blankPlaceName_isOmitted() {
        assertEquals("7F船中", formatLocation(7, ShipZone.MID, null))
        assertEquals("7F船中", formatLocation(7, ShipZone.MID, ""))
        assertEquals("7F船中", formatLocation(7, ShipZone.MID, "   "))
    }

    @Test
    fun placeNameIsTrimmed() {
        assertEquals("7F船中花园", formatLocation(7, ShipZone.MID, "  花园  "))
    }

    @Test
    fun nullDeck_returnsNull_soTheRowIsHidden() {
        // 上船/下船事件不设地址，卡片第二行整行不显示
        assertNull(formatLocation(null, null, null))
        assertNull(formatLocation(null, ShipZone.MID, "花园"))
    }

    @Test
    fun deckWithoutZone_showsDeckAndPlace() {
        assertEquals("7F花园", formatLocation(7, null, "花园"))
        assertEquals("7F", formatLocation(7, null, null))
    }

    @Test
    fun twoDigitDeck_works() {
        assertEquals("11F船中泳池", formatLocation(11, ShipZone.MID, "泳池"))
    }
}
```

- [ ] **Step 2: 跑测试确认失败**

Run: `./gradlew testDebugUnitTest --tests "*LocationFormatterTest*"`
Expected: FAIL —— `Unresolved reference: formatLocation`

- [ ] **Step 3: 实现**

`domain/LocationFormatter.kt`：

```kotlin
package com.cruise.assistant.domain

/**
 * 需求3：卡片第二行的地点文案，形如 "7F船中花园"。
 *
 * deck 为 null 表示不设地址（如上船/下船事件），返回 null 让 UI 整行隐藏。
 * zone 与 placeName 都是可选的，缺失时按序省略。
 */
fun formatLocation(deck: Int?, zone: ShipZone?, placeName: String?): String? {
    if (deck == null) return null
    return buildString {
        append(deck).append('F')
        zone?.let { append(it.label) }
        placeName?.trim()?.takeIf { it.isNotEmpty() }?.let { append(it) }
    }
}
```

- [ ] **Step 4: 跑测试确认通过**

Run: `./gradlew testDebugUnitTest --tests "*LocationFormatterTest*"`
Expected: PASS，6 个测试全绿

- [ ] **Step 5: Commit**

```bash
git add app/src/main/java/com/cruise/assistant/domain/LocationFormatter.kt app/src/test/java/com/cruise/assistant/domain/LocationFormatterTest.kt
git commit -m "feat: add ship location formatter"
```

### Task 2f: TimelineLayout —— 需求2 的时间↔像素映射与空时段折叠

这是最容易出错的一块。核心概念：时间轴被切成若干 **Segment**，每段要么正常按
`hourHeightDp` 比例展开，要么被折叠成固定 `COLLAPSED_SEGMENT_DP` 高度。
`minuteToDp` 在段内线性插值、跨段累加，必须**单调不减**。

- [ ] **Step 1: 写失败的测试**

`app/src/test/java/com/cruise/assistant/domain/TimelineLayoutTest.kt`：

```kotlin
package com.cruise.assistant.domain

import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Test
import java.time.LocalDate
import java.time.LocalTime

class TimelineLayoutTest {

    private val day = LocalDate.of(2026, 9, 3)
    private val EPS = 0.01f

    private fun ev(hour: Int, minute: Int, durMin: Int) = CruiseEvent(
        id = 0, date = day, startTime = LocalTime.of(hour, minute),
        durationMinutes = durMin, categoryId = 1, title = "e",
    )

    // ---------- 无折叠：纯线性映射 ----------

    @Test
    fun rangeStart_mapsToZero() {
        val l = buildTimelineLayout(420, 1440, 60, emptyList(), false)
        assertEquals(0f, l.minuteToDp(420), EPS)
    }

    @Test
    fun oneHourAfterStart_mapsToOneHourHeight() {
        val l = buildTimelineLayout(420, 1440, 60, emptyList(), false)
        assertEquals(60f, l.minuteToDp(480), EPS)
    }

    @Test
    fun halfHour_mapsToHalfHeight() {
        val l = buildTimelineLayout(420, 1440, 60, emptyList(), false)
        assertEquals(30f, l.minuteToDp(450), EPS)
    }

    @Test
    fun totalHeight_matchesRangeTimesHourHeight() {
        // 07:00-24:00 = 17 小时
        val l = buildTimelineLayout(420, 1440, 60, emptyList(), false)
        assertEquals(17 * 60f, l.totalHeightDp, EPS)
    }

    @Test
    fun customHourHeight_scalesProportionally() {
        val l = buildTimelineLayout(420, 1440, 100, emptyList(), false)
        assertEquals(100f, l.minuteToDp(480), EPS)
        assertEquals(17 * 100f, l.totalHeightDp, EPS)
    }

    @Test
    fun minutesBeforeRange_clampToZero() {
        val l = buildTimelineLayout(420, 1440, 60, emptyList(), false)
        assertEquals(0f, l.minuteToDp(0), EPS)
        assertEquals(0f, l.minuteToDp(300), EPS)
    }

    @Test
    fun minutesAfterRange_clampToTotalHeight() {
        val l = buildTimelineLayout(420, 1440, 60, emptyList(), false)
        assertEquals(l.totalHeightDp, l.minuteToDp(1500), EPS)
    }

    // ---------- 反向映射 ----------

    @Test
    fun dpToMinute_isInverseOfMinuteToDp() {
        val l = buildTimelineLayout(420, 1440, 60, emptyList(), false)
        for (minute in listOf(420, 500, 780, 1000, 1439)) {
            assertEquals(minute, l.dpToMinute(l.minuteToDp(minute)))
        }
    }

    @Test
    fun dpToMinute_clampsToRange() {
        val l = buildTimelineLayout(420, 1440, 60, emptyList(), false)
        assertEquals(420, l.dpToMinute(-50f))
        assertEquals(1440, l.dpToMinute(99999f))
    }

    // ---------- 小时刻度 ----------

    @Test
    fun hourTicks_coverEveryWholeHourInRange() {
        val l = buildTimelineLayout(420, 1440, 60, emptyList(), false)
        val ticks = l.hourTicks()
        // 07:00 到 24:00 含两端 = 18 个刻度
        assertEquals(18, ticks.size)
        assertEquals(420, ticks.first().first)
        assertEquals(1440, ticks.last().first)
        assertEquals(0f, ticks.first().second, EPS)
    }

    @Test
    fun hourTicks_startAtFirstWholeHourAtOrAfterRangeStart() {
        // 07:30 起 → 第一个刻度是 08:00
        val l = buildTimelineLayout(450, 1440, 60, emptyList(), false)
        assertEquals(480, l.hourTicks().first().first)
    }

    // ---------- 折叠空时段 ----------

    @Test
    fun collapseOff_keepsSingleSegment() {
        val l = buildTimelineLayout(420, 1440, 60, listOf(ev(13, 0, 60)), false)
        assertEquals(1, l.segments.size)
        assertTrue(l.segments.none { it.collapsed })
    }

    @Test
    fun collapseOn_withNoEvents_collapsesWholeRange() {
        val l = buildTimelineLayout(420, 1440, 60, emptyList(), true)
        assertTrue(l.segments.all { it.collapsed })
        assertEquals(COLLAPSED_SEGMENT_DP, l.totalHeightDp, EPS)
    }

    @Test
    fun collapseOn_shrinksLongEmptyGapBetweenEvents() {
        // 08:00-09:00 一件、20:00-21:00 一件，中间 11 小时空
        val events = listOf(ev(8, 0, 60), ev(20, 0, 60))
        val collapsed = buildTimelineLayout(420, 1440, 60, events, true)
        val expanded = buildTimelineLayout(420, 1440, 60, events, false)
        assertTrue(collapsed.totalHeightDp < expanded.totalHeightDp)
        assertTrue(collapsed.segments.any { it.collapsed })
    }

    @Test
    fun collapseOn_keepsEventSegmentsAtFullScale() {
        val events = listOf(ev(8, 0, 60))
        val l = buildTimelineLayout(420, 1440, 60, events, true)
        // 08:00→09:00 之间仍是完整 60dp
        assertEquals(60f, l.minuteToDp(540) - l.minuteToDp(480), EPS)
    }

    @Test
    fun collapseOn_keepsShortGapsExpanded() {
        // 两件事只隔 20 分钟，短于阈值，不该折叠
        val events = listOf(ev(8, 0, 60), ev(9, 20, 60))
        val l = buildTimelineLayout(480, 660, 60, events, true)
        assertTrue(l.segments.none { it.collapsed })
    }

    @Test
    fun minuteToDp_isMonotonic_whenCollapsing() {
        val events = listOf(ev(8, 0, 60), ev(20, 0, 60))
        val l = buildTimelineLayout(420, 1440, 60, events, true)
        var prev = -1f
        for (minute in 420..1440 step 5) {
            val dp = l.minuteToDp(minute)
            assertTrue("dp 必须单调不减 @minute=" + minute, dp >= prev - EPS)
            prev = dp
        }
    }

    @Test
    fun dpToMinute_isInverse_whenCollapsing() {
        val events = listOf(ev(8, 0, 60), ev(20, 0, 60))
        val l = buildTimelineLayout(420, 1440, 60, events, true)
        // 展开段内的分钟必须能精确往返
        for (minute in listOf(480, 510, 540, 1200, 1260)) {
            assertEquals(minute, l.dpToMinute(l.minuteToDp(minute)))
        }
    }

    // ---------- 范围外事件计数 ----------

    @Test
    fun countsEventsBeforeAndAfterRange() {
        val events = listOf(ev(3, 0, 60), ev(5, 0, 60), ev(13, 0, 60), ev(23, 30, 60))
        // 范围 07:00-23:00 → 前面 2 件、后面 1 件
        val l = buildTimelineLayout(420, 1380, 60, events, false)
        assertEquals(2, l.hiddenBefore)
        assertEquals(1, l.hiddenAfter)
    }

    @Test
    fun eventPartiallyInsideRange_isNotCountedAsHidden() {
        // 06:30-07:30 跨过范围起点，算在范围内
        val l = buildTimelineLayout(420, 1380, 60, listOf(ev(6, 30, 60)), false)
        assertEquals(0, l.hiddenBefore)
    }

    // ---------- 参数校验 ----------

    @Test
    fun invalidRange_isNormalizedToMinimumTwoHours() {
        val l = buildTimelineLayout(600, 600, 60, emptyList(), false)
        assertTrue(l.rangeEndMinute - l.rangeStartMinute >= 120)
    }
}
```

- [ ] **Step 2: 跑测试确认失败**

Run: `./gradlew testDebugUnitTest --tests "*TimelineLayoutTest*"`
Expected: FAIL —— `Unresolved reference: buildTimelineLayout`

- [ ] **Step 3: 实现**

`domain/TimelineLayout.kt`：

```kotlin
package com.cruise.assistant.domain

/** 折叠后的空时段固定高度 */
const val COLLAPSED_SEGMENT_DP = 28f

/** 空档超过这么多分钟才折叠 */
const val MIN_COLLAPSIBLE_GAP_MINUTES = 90

/** 折叠时在事件前后各保留的缓冲分钟数 */
private const val EVENT_PADDING_MINUTES = 30

/** 显示范围的最小跨度 */
private const val MIN_RANGE_MINUTES = 120

/**
 * 时间轴的一段。collapsed 段用固定高度，其余按 hourHeightDp 比例展开。
 * [startMinute, endMinute) 为半开区间；topDp 是该段顶部的累计偏移。
 */
data class Segment(
    val startMinute: Int,
    val endMinute: Int,
    val topDp: Float,
    val heightDp: Float,
    val collapsed: Boolean,
) {
    val spanMinutes: Int get() = endMinute - startMinute
}

data class TimelineLayout(
    val hourHeightDp: Int,
    val rangeStartMinute: Int,
    val rangeEndMinute: Int,
    val segments: List<Segment>,
    val totalHeightDp: Float,
    val hiddenBefore: Int,
    val hiddenAfter: Int,
)

/**
 * 需求2：构造时间轴布局。
 *
 * @param rangeStartMinute 显示起点（当日 0 点起的分钟数）
 * @param rangeEndMinute   显示终点，1440 表示 24:00
 * @param hourHeightDp     每小时高度，缩放滑块控制
 * @param events           当日事件，用于决定哪些空档可以折叠
 * @param collapseEmptyGaps 是否折叠无事件的长空档
 */
fun buildTimelineLayout(
    rangeStartMinute: Int,
    rangeEndMinute: Int,
    hourHeightDp: Int,
    events: List<CruiseEvent>,
    collapseEmptyGaps: Boolean,
): TimelineLayout {
    val start = rangeStartMinute.coerceIn(0, 1440 - MIN_RANGE_MINUTES)
    val end = rangeEndMinute.coerceIn(start + MIN_RANGE_MINUTES, 1440)

    val hiddenBefore = events.count { it.endMinute <= start }
    val hiddenAfter = events.count { it.startMinute >= end }

    val rawSegments = if (collapseEmptyGaps) {
        buildCollapsedSpans(start, end, events)
    } else {
        listOf(start to end to false)
    }

    var cursorDp = 0f
    val segments = rawSegments.map { (span, collapsed) ->
        val (segStart, segEnd) = span
        val height = if (collapsed) {
            COLLAPSED_SEGMENT_DP
        } else {
            (segEnd - segStart) / 60f * hourHeightDp
        }
        Segment(segStart, segEnd, cursorDp, height, collapsed).also { cursorDp += height }
    }

    return TimelineLayout(
        hourHeightDp = hourHeightDp,
        rangeStartMinute = start,
        rangeEndMinute = end,
        segments = segments,
        totalHeightDp = cursorDp,
        hiddenBefore = hiddenBefore,
        hiddenAfter = hiddenAfter,
    )
}

/**
 * 把 [start, end) 切成"保留段 / 折叠段"交替的序列。
 * 保留段 = 事件占用区间（前后各留 EVENT_PADDING_MINUTES 缓冲）的并集。
 * 两个保留段之间超过 MIN_COLLAPSIBLE_GAP_MINUTES 的空档才折叠。
 */
private fun buildCollapsedSpans(
    start: Int,
    end: Int,
    events: List<CruiseEvent>,
): List<Pair<Pair<Int, Int>, Boolean>> {
    val keepRanges = events
        .filter { it.endMinute > start && it.startMinute < end && it.durationMinutes > 0 }
        .map {
            (it.startMinute - EVENT_PADDING_MINUTES).coerceAtLeast(start) to
                (it.endMinute + EVENT_PADDING_MINUTES).coerceAtMost(end)
        }
        .sortedBy { it.first }
        .fold(mutableListOf<Pair<Int, Int>>()) { acc, range ->
            val last = acc.lastOrNull()
            if (last != null && range.first <= last.second) {
                acc[acc.lastIndex] = last.first to maxOf(last.second, range.second)
            } else {
                acc += range
            }
            acc
        }

    if (keepRanges.isEmpty()) return listOf((start to end) to true)

    val result = mutableListOf<Pair<Pair<Int, Int>, Boolean>>()
    var cursor = start
    for ((keepStart, keepEnd) in keepRanges) {
        if (keepStart > cursor) {
            val gap = keepStart - cursor
            result += (cursor to keepStart) to (gap >= MIN_COLLAPSIBLE_GAP_MINUTES)
        }
        result += (keepStart to keepEnd) to false
        cursor = keepEnd
    }
    if (cursor < end) {
        val gap = end - cursor
        result += (cursor to end) to (gap >= MIN_COLLAPSIBLE_GAP_MINUTES)
    }

    // 合并相邻的非折叠段，避免碎片
    return result.fold(mutableListOf()) { acc, item ->
        val last = acc.lastOrNull()
        if (last != null && !last.second && !item.second && last.first.second == item.first.first) {
            acc[acc.lastIndex] = (last.first.first to item.first.second) to false
        } else {
            acc += item
        }
        acc
    }
}

/** 分钟 → 垂直偏移 dp。范围外夹到 0 或 totalHeightDp。段内线性插值。 */
fun TimelineLayout.minuteToDp(minute: Int): Float {
    if (minute <= rangeStartMinute) return 0f
    if (minute >= rangeEndMinute) return totalHeightDp
    val seg = segments.firstOrNull { minute < it.endMinute } ?: return totalHeightDp
    if (minute <= seg.startMinute) return seg.topDp
    val ratio = (minute - seg.startMinute).toFloat() / seg.spanMinutes
    return seg.topDp + ratio * seg.heightDp
}

/** 垂直偏移 dp → 分钟，minuteToDp 的逆运算。 */
fun TimelineLayout.dpToMinute(dp: Float): Int {
    if (dp <= 0f) return rangeStartMinute
    if (dp >= totalHeightDp) return rangeEndMinute
    val seg = segments.firstOrNull { dp < it.topDp + it.heightDp } ?: return rangeEndMinute
    if (seg.heightDp <= 0f) return seg.startMinute
    val ratio = (dp - seg.topDp) / seg.heightDp
    return (seg.startMinute + ratio * seg.spanMinutes).toInt().coerceIn(seg.startMinute, seg.endMinute)
}

/**
 * 需要绘制的整点刻度：(分钟, dp偏移)。
 * 第一个刻度是 >= rangeStartMinute 的第一个整点；折叠段内的整点跳过不画。
 */
fun TimelineLayout.hourTicks(): List<Pair<Int, Float>> {
    val firstHour = ((rangeStartMinute + 59) / 60) * 60
    return (firstHour..rangeEndMinute step 60).mapNotNull { minute ->
        val inCollapsed = segments.any { it.collapsed && minute > it.startMinute && minute < it.endMinute }
        if (inCollapsed) null else minute to minuteToDp(minute)
    }
}
```

- [ ] **Step 4: 跑测试确认通过**

Run: `./gradlew testDebugUnitTest --tests "*TimelineLayoutTest*"`
Expected: PASS，21 个测试全绿

若 `dpToMinute_isInverse_whenCollapsing` 因浮点取整失败，把 `dpToMinute` 里的
`.toInt()` 改成 `Math.round(...)` 再跑；不要放宽断言。

- [ ] **Step 5: 跑全部 domain 测试**

Run: `./gradlew testDebugUnitTest`
Expected: PASS，共 55 个测试（10+10+8+6+21）

- [ ] **Step 6: Commit**

```bash
git add app/src/main/java/com/cruise/assistant/domain/TimelineLayout.kt app/src/test/java/com/cruise/assistant/domain/TimelineLayoutTest.kt
git commit -m "feat: add timeline layout math with adjustable range and gap collapsing"
```

## Task 3: Room 数据库与 DataStore 设置

**Files:**
- Create: `data/db/Converters.kt`, `data/db/CruiseDatabase.kt`
- Create: `data/db/entity/{VoyageEntity,CategoryEntity,EventEntity,DayPlanEntity,LocationPresetEntity}.kt`
- Create: `data/db/dao/{VoyageDao,CategoryDao,EventDao,LocationPresetDao}.kt`
- Create: `data/prefs/SettingsStore.kt`
- Create: `data/db/Mappers.kt`（Entity ↔ domain 双向转换）

**Interfaces:**
- Consumes: Task 2 的 `domain` 模型
- Produces:
  - `CruiseDatabase.get(context): CruiseDatabase`，暴露 `voyageDao()` / `categoryDao()` / `eventDao()` / `locationPresetDao()`
  - Entity → domain：`VoyageEntity.toDomain()` 等；domain → Entity：`Voyage.toEntity()` 等
  - `SettingsStore(context)` 暴露 `Flow<AppSettings>` 与 `suspend` setter
  - `data class AppSettings(lastViewedDate, displayStartMinute, displayEndMinute, hourHeightDp, collapseEmptyGaps, defaultReminderMinutes, notificationsEnabled, onboardingCompleted)`

- [ ] **Step 1: 写 TypeConverter 与 Entity**

`data/db/Converters.kt`：

```kotlin
package com.cruise.assistant.data.db

import androidx.room.TypeConverter
import java.time.LocalDate
import java.time.LocalTime

class Converters {
    @TypeConverter fun dateToString(value: LocalDate?): String? = value?.toString()
    @TypeConverter fun stringToDate(value: String?): LocalDate? = value?.let(LocalDate::parse)
    @TypeConverter fun timeToString(value: LocalTime?): String? = value?.toString()
    @TypeConverter fun stringToTime(value: String?): LocalTime? = value?.let(LocalTime::parse)
}
```

`data/db/entity/VoyageEntity.kt`：

```kotlin
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
```

`data/db/entity/CategoryEntity.kt`：

```kotlin
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
```

`data/db/entity/EventEntity.kt`：

```kotlin
package com.cruise.assistant.data.db.entity

import androidx.room.Entity
import androidx.room.Index
import androidx.room.PrimaryKey
import java.time.LocalDate
import java.time.LocalTime

@Entity(
    tableName = "event",
    indices = [Index("date"), Index("recurrenceGroupId")],
)
data class EventEntity(
    @PrimaryKey(autoGenerate = true) val id: Long = 0,
    val date: LocalDate,
    val startTime: LocalTime,
    val durationMinutes: Int,
    val categoryId: Long,
    val title: String,
    val deck: Int?,
    /** ShipZone.name，null 表示未选 */
    val zone: String?,
    val placeName: String?,
    val lane: Int,
    val recurrenceGroupId: String?,
    val reminderMinutesBefore: Int?,
    val isSystemGenerated: Boolean,
)
```

`data/db/entity/DayPlanEntity.kt`：

```kotlin
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
```

`data/db/entity/LocationPresetEntity.kt`：

```kotlin
package com.cruise.assistant.data.db.entity

import androidx.room.Entity
import androidx.room.Index
import androidx.room.PrimaryKey

@Entity(
    tableName = "location_preset",
    indices = [Index(value = ["deck", "zone", "placeName"], unique = true)],
)
data class LocationPresetEntity(
    @PrimaryKey(autoGenerate = true) val id: Long = 0,
    val deck: Int,
    /** ShipZone.name */
    val zone: String,
    val placeName: String,
    val lastUsedEpochMs: Long,
    val useCount: Int,
)
```

- [ ] **Step 2: 写 Mapper**

`data/db/Mappers.kt`：

```kotlin
package com.cruise.assistant.data.db

import com.cruise.assistant.data.db.entity.CategoryEntity
import com.cruise.assistant.data.db.entity.DayPlanEntity
import com.cruise.assistant.data.db.entity.EventEntity
import com.cruise.assistant.data.db.entity.LocationPresetEntity
import com.cruise.assistant.data.db.entity.VoyageEntity
import com.cruise.assistant.domain.Category
import com.cruise.assistant.domain.CruiseEvent
import com.cruise.assistant.domain.DayPlan
import com.cruise.assistant.domain.LocationPreset
import com.cruise.assistant.domain.ShipZone
import com.cruise.assistant.domain.Voyage

fun VoyageEntity.toDomain() = Voyage(
    id, shipName, boardDate, boardTime, disembarkDate, disembarkTime, maxDeck,
)

fun Voyage.toEntity() = VoyageEntity(
    id, shipName, boardDate, boardTime, disembarkDate, disembarkTime, maxDeck,
)

fun CategoryEntity.toDomain() = Category(
    id, name, colorArgb, iconKey, parentId, isBuiltIn, isArchived, sortOrder,
)

fun Category.toEntity() = CategoryEntity(
    id, name, colorArgb, iconKey, parentId, isBuiltIn, isArchived, sortOrder,
)

fun EventEntity.toDomain() = CruiseEvent(
    id = id, date = date, startTime = startTime, durationMinutes = durationMinutes,
    categoryId = categoryId, title = title, deck = deck,
    zone = zone?.let { runCatching { ShipZone.valueOf(it) }.getOrNull() },
    placeName = placeName, lane = lane, recurrenceGroupId = recurrenceGroupId,
    reminderMinutesBefore = reminderMinutesBefore, isSystemGenerated = isSystemGenerated,
)

fun CruiseEvent.toEntity() = EventEntity(
    id = id, date = date, startTime = startTime, durationMinutes = durationMinutes,
    categoryId = categoryId, title = title, deck = deck, zone = zone?.name,
    placeName = placeName, lane = lane, recurrenceGroupId = recurrenceGroupId,
    reminderMinutesBefore = reminderMinutesBefore, isSystemGenerated = isSystemGenerated,
)

fun DayPlanEntity.toDomain() = DayPlan(date, finalized, finalizedAt)
fun DayPlan.toEntity() = DayPlanEntity(date, finalized, finalizedAt)

fun LocationPresetEntity.toDomain() = LocationPreset(
    id, deck, ShipZone.valueOf(zone), placeName, lastUsedEpochMs, useCount,
)

fun LocationPreset.toEntity() = LocationPresetEntity(
    id, deck, zone.name, placeName, lastUsedEpochMs, useCount,
)
```

- [ ] **Step 3: 写 DAO**

`data/db/dao/VoyageDao.kt`：

```kotlin
package com.cruise.assistant.data.db.dao

import androidx.room.Dao
import androidx.room.Query
import androidx.room.Upsert
import com.cruise.assistant.data.db.entity.DayPlanEntity
import com.cruise.assistant.data.db.entity.VoyageEntity
import kotlinx.coroutines.flow.Flow
import java.time.LocalDate

@Dao
interface VoyageDao {
    @Query("SELECT * FROM voyage WHERE id = 1")
    fun observeVoyage(): Flow<VoyageEntity?>

    @Query("SELECT * FROM voyage WHERE id = 1")
    suspend fun getVoyage(): VoyageEntity?

    @Upsert
    suspend fun upsertVoyage(voyage: VoyageEntity)

    @Query("DELETE FROM voyage")
    suspend fun clearVoyage()

    @Query("SELECT * FROM day_plan ORDER BY date")
    fun observeDayPlans(): Flow<List<DayPlanEntity>>

    @Query("SELECT * FROM day_plan WHERE date = :date")
    fun observeDayPlan(date: LocalDate): Flow<DayPlanEntity?>

    @Upsert
    suspend fun upsertDayPlans(plans: List<DayPlanEntity>)

    @Upsert
    suspend fun upsertDayPlan(plan: DayPlanEntity)

    @Query("DELETE FROM day_plan")
    suspend fun clearDayPlans()
}
```

`data/db/dao/CategoryDao.kt`：

```kotlin
package com.cruise.assistant.data.db.dao

import androidx.room.Dao
import androidx.room.Delete
import androidx.room.Insert
import androidx.room.Query
import androidx.room.Update
import com.cruise.assistant.data.db.entity.CategoryEntity
import kotlinx.coroutines.flow.Flow

@Dao
interface CategoryDao {
    @Query("SELECT * FROM category ORDER BY sortOrder, id")
    fun observeAll(): Flow<List<CategoryEntity>>

    @Query("SELECT * FROM category WHERE isArchived = 0 ORDER BY sortOrder, id")
    fun observeActive(): Flow<List<CategoryEntity>>

    @Query("SELECT * FROM category WHERE id = :id")
    suspend fun getById(id: Long): CategoryEntity?

    @Query("SELECT COUNT(*) FROM category")
    suspend fun count(): Int

    @Insert
    suspend fun insert(category: CategoryEntity): Long

    @Insert
    suspend fun insertAll(categories: List<CategoryEntity>)

    @Update
    suspend fun update(category: CategoryEntity)

    @Delete
    suspend fun delete(category: CategoryEntity)

    @Query("DELETE FROM category")
    suspend fun clearAll()
}
```

`data/db/dao/EventDao.kt`：

```kotlin
package com.cruise.assistant.data.db.dao

import androidx.room.Dao
import androidx.room.Delete
import androidx.room.Insert
import androidx.room.Query
import androidx.room.Update
import com.cruise.assistant.data.db.entity.EventEntity
import kotlinx.coroutines.flow.Flow
import java.time.LocalDate

@Dao
interface EventDao {
    @Query("SELECT * FROM event WHERE date = :date ORDER BY startTime, lane")
    fun observeByDate(date: LocalDate): Flow<List<EventEntity>>

    @Query("SELECT * FROM event WHERE date = :date ORDER BY startTime, lane")
    suspend fun getByDate(date: LocalDate): List<EventEntity>

    @Query("SELECT * FROM event ORDER BY date, startTime")
    suspend fun getAll(): List<EventEntity>

    @Query("SELECT * FROM event WHERE id = :id")
    suspend fun getById(id: Long): EventEntity?

    @Query("SELECT * FROM event WHERE recurrenceGroupId = :groupId ORDER BY date")
    suspend fun getByGroup(groupId: String): List<EventEntity>

    @Query("SELECT DISTINCT date FROM event ORDER BY date")
    fun observeDatesWithEvents(): Flow<List<LocalDate>>

    @Insert
    suspend fun insert(event: EventEntity): Long

    @Insert
    suspend fun insertAll(events: List<EventEntity>): List<Long>

    @Update
    suspend fun update(event: EventEntity)

    @Update
    suspend fun updateAll(events: List<EventEntity>)

    @Delete
    suspend fun delete(event: EventEntity)

    @Query("DELETE FROM event WHERE recurrenceGroupId = :groupId")
    suspend fun deleteGroup(groupId: String)

    @Query("UPDATE event SET categoryId = :toCategoryId WHERE categoryId = :fromCategoryId")
    suspend fun reassignCategory(fromCategoryId: Long, toCategoryId: Long)

    @Query("SELECT COUNT(*) FROM event WHERE categoryId = :categoryId")
    suspend fun countByCategory(categoryId: Long): Int

    @Query("DELETE FROM event")
    suspend fun clearAll()
}
```

`data/db/dao/LocationPresetDao.kt`：

```kotlin
package com.cruise.assistant.data.db.dao

import androidx.room.Dao
import androidx.room.Delete
import androidx.room.Query
import androidx.room.Upsert
import com.cruise.assistant.data.db.entity.LocationPresetEntity
import kotlinx.coroutines.flow.Flow

@Dao
interface LocationPresetDao {
    @Query("SELECT * FROM location_preset ORDER BY useCount DESC, lastUsedEpochMs DESC")
    fun observeAll(): Flow<List<LocationPresetEntity>>

    @Query("SELECT * FROM location_preset WHERE deck = :deck AND zone = :zone AND placeName = :placeName")
    suspend fun find(deck: Int, zone: String, placeName: String): LocationPresetEntity?

    @Upsert
    suspend fun upsert(preset: LocationPresetEntity)

    @Delete
    suspend fun delete(preset: LocationPresetEntity)

    @Query("DELETE FROM location_preset")
    suspend fun clearAll()
}
```

- [ ] **Step 4: 写 Database**

`data/db/CruiseDatabase.kt`：

```kotlin
package com.cruise.assistant.data.db

import android.content.Context
import androidx.room.Database
import androidx.room.Room
import androidx.room.RoomDatabase
import androidx.room.TypeConverters
import com.cruise.assistant.data.db.dao.CategoryDao
import com.cruise.assistant.data.db.dao.EventDao
import com.cruise.assistant.data.db.dao.LocationPresetDao
import com.cruise.assistant.data.db.dao.VoyageDao
import com.cruise.assistant.data.db.entity.CategoryEntity
import com.cruise.assistant.data.db.entity.DayPlanEntity
import com.cruise.assistant.data.db.entity.EventEntity
import com.cruise.assistant.data.db.entity.LocationPresetEntity
import com.cruise.assistant.data.db.entity.VoyageEntity

@Database(
    entities = [
        VoyageEntity::class,
        CategoryEntity::class,
        EventEntity::class,
        DayPlanEntity::class,
        LocationPresetEntity::class,
    ],
    version = 1,
    exportSchema = true,
)
@TypeConverters(Converters::class)
abstract class CruiseDatabase : RoomDatabase() {
    abstract fun voyageDao(): VoyageDao
    abstract fun categoryDao(): CategoryDao
    abstract fun eventDao(): EventDao
    abstract fun locationPresetDao(): LocationPresetDao

    companion object {
        @Volatile private var instance: CruiseDatabase? = null

        fun get(context: Context): CruiseDatabase =
            instance ?: synchronized(this) {
                instance ?: Room.databaseBuilder(
                    context.applicationContext,
                    CruiseDatabase::class.java,
                    "cruise.db",
                ).build().also { instance = it }
            }
    }
}
```

- [ ] **Step 5: 写 SettingsStore**

`data/prefs/SettingsStore.kt`：

```kotlin
package com.cruise.assistant.data.prefs

import android.content.Context
import androidx.datastore.core.DataStore
import androidx.datastore.preferences.core.Preferences
import androidx.datastore.preferences.core.booleanPreferencesKey
import androidx.datastore.preferences.core.edit
import androidx.datastore.preferences.core.intPreferencesKey
import androidx.datastore.preferences.core.stringPreferencesKey
import androidx.datastore.preferences.preferencesDataStore
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.map
import java.time.LocalDate

data class AppSettings(
    val lastViewedDate: LocalDate? = null,
    val displayStartMinute: Int = 7 * 60,
    val displayEndMinute: Int = 24 * 60,
    val hourHeightDp: Int = 60,
    val collapseEmptyGaps: Boolean = false,
    val defaultReminderMinutes: Int = 15,
    val notificationsEnabled: Boolean = true,
    val onboardingCompleted: Boolean = false,
)

private val Context.dataStore: DataStore<Preferences> by preferencesDataStore("cruise_settings")

class SettingsStore(private val context: Context) {

    private object Keys {
        val lastViewedDate = stringPreferencesKey("last_viewed_date")
        val displayStart = intPreferencesKey("display_start_minute")
        val displayEnd = intPreferencesKey("display_end_minute")
        val hourHeight = intPreferencesKey("hour_height_dp")
        val collapseGaps = booleanPreferencesKey("collapse_empty_gaps")
        val reminderMinutes = intPreferencesKey("default_reminder_minutes")
        val notificationsEnabled = booleanPreferencesKey("notifications_enabled")
        val onboardingCompleted = booleanPreferencesKey("onboarding_completed")
    }

    val settings: Flow<AppSettings> = context.dataStore.data.map { prefs ->
        val defaults = AppSettings()
        AppSettings(
            lastViewedDate = prefs[Keys.lastViewedDate]
                ?.let { runCatching { LocalDate.parse(it) }.getOrNull() },
            displayStartMinute = prefs[Keys.displayStart] ?: defaults.displayStartMinute,
            displayEndMinute = prefs[Keys.displayEnd] ?: defaults.displayEndMinute,
            hourHeightDp = prefs[Keys.hourHeight] ?: defaults.hourHeightDp,
            collapseEmptyGaps = prefs[Keys.collapseGaps] ?: defaults.collapseEmptyGaps,
            defaultReminderMinutes = prefs[Keys.reminderMinutes] ?: defaults.defaultReminderMinutes,
            notificationsEnabled = prefs[Keys.notificationsEnabled] ?: defaults.notificationsEnabled,
            onboardingCompleted = prefs[Keys.onboardingCompleted] ?: defaults.onboardingCompleted,
        )
    }

    suspend fun setLastViewedDate(date: LocalDate) =
        edit { it[Keys.lastViewedDate] = date.toString() }

    suspend fun setDisplayRange(startMinute: Int, endMinute: Int) = edit {
        it[Keys.displayStart] = startMinute
        it[Keys.displayEnd] = endMinute
    }

    suspend fun setHourHeight(dp: Int) = edit { it[Keys.hourHeight] = dp }

    suspend fun setCollapseEmptyGaps(enabled: Boolean) = edit { it[Keys.collapseGaps] = enabled }

    suspend fun setDefaultReminderMinutes(minutes: Int) =
        edit { it[Keys.reminderMinutes] = minutes }

    suspend fun setNotificationsEnabled(enabled: Boolean) =
        edit { it[Keys.notificationsEnabled] = enabled }

    suspend fun setOnboardingCompleted(completed: Boolean) =
        edit { it[Keys.onboardingCompleted] = completed }

    private suspend fun edit(block: (androidx.datastore.preferences.core.MutablePreferences) -> Unit) {
        context.dataStore.edit(block)
    }
}
```

- [ ] **Step 6: 验证编译与 Room 注解处理**

Run: `./gradlew assembleDebug`
Expected: `BUILD SUCCESSFUL`，且 `app/schemas/com.cruise.assistant.data.db.CruiseDatabase/1.json` 已生成

若 KSP 报 "Cannot figure out how to save this field into database"，检查
`EventEntity.zone` 是否为 `String?`（不是 `ShipZone?`）—— zone 走字符串存储，
只有 `LocalDate`/`LocalTime` 由 `Converters` 处理。

- [ ] **Step 7: Commit**

```bash
git add app/src/main/java/com/cruise/assistant/data app/schemas
git commit -m "feat: add Room database, DAOs, mappers, and DataStore settings"
```

## Task 4: Repository 层与 AppContainer

**Files:**
- Create: `data/repo/VoyageRepository.kt`, `data/repo/EventRepository.kt`,
  `data/repo/CategoryRepository.kt`, `data/repo/LocationPresetRepository.kt`
- Create: `data/BuiltInCategories.kt`
- Modify: `AppContainer.kt`（新建）、`CruiseApp.kt`

**Interfaces:**
- Consumes: Task 2 domain（`assignLane` / `expandRecurrence` / `findConflicts`）、Task 3 DAO
- Produces:
  - `class VoyageRepository(voyageDao, eventDao, categoryDao, locationPresetDao)`
    - `observeVoyage(): Flow<Voyage?>` / `observeDayPlans(): Flow<Map<LocalDate, DayPlan>>`
    - `suspend fun createVoyage(shipName, boardDate, boardTime, disembarkDate, maxDeck)`
    - `suspend fun updateVoyage(voyage: Voyage)` / `suspend fun resetVoyage()`
    - `suspend fun setFinalized(date: LocalDate, finalized: Boolean, nowMs: Long)`
  - `class EventRepository(eventDao, voyageDao)`
    - `observeByDate(date): Flow<List<CruiseEvent>>`
    - `suspend fun addEvent(event, recurring: Boolean): Int`（返回写入条数）
    - `suspend fun updateEvent(event, scope: RecurrenceScope)`
    - `suspend fun deleteEvent(event, scope: RecurrenceScope)`
    - `suspend fun moveToLane(eventId: Long, lane: Int)`
    - `suspend fun conflictsOn(date): List<Pair<CruiseEvent, CruiseEvent>>`
    - `suspend fun getUpcoming(fromEpochMs: Long, limit: Int): List<CruiseEvent>`
    - `enum class RecurrenceScope { THIS_DAY_ONLY, WHOLE_GROUP }`
  - `class CategoryRepository(categoryDao, eventDao)`
    - `observeActive(): Flow<List<Category>>` / `observeAll(): Flow<List<Category>>`
    - `suspend fun save(category: Category): Long` / `suspend fun delete(category, fallbackCategoryId)`
    - `suspend fun countEvents(categoryId: Long): Int`
  - `class LocationPresetRepository(locationPresetDao)`
    - `observeAll(): Flow<List<LocationPreset>>` / `suspend fun remember(deck, zone, placeName, nowMs)`
  - `class AppContainer(context)` 暴露上述 4 个 repository、`settingsStore`、`reminderScheduler`（Task 12 填）
  - `CruiseApp.container: AppContainer`

- [ ] **Step 1: 写内置类别种子**

`data/BuiltInCategories.kt` —— 颜色与 iconKey 来自 spec §8：

```kotlin
package com.cruise.assistant.data

import com.cruise.assistant.domain.Category

/** spec §8：预制 6 个类别，颜色有区分度 */
val BUILT_IN_CATEGORIES: List<Category> = listOf(
    Category(name = "餐食", colorArgb = 0xFF34A853.toInt(), iconKey = "meal", isBuiltIn = true, sortOrder = 0),
    Category(name = "演出", colorArgb = 0xFFF5B301.toInt(), iconKey = "show", isBuiltIn = true, sortOrder = 1),
    Category(name = "重要事项", colorArgb = 0xFFE8563F.toInt(), iconKey = "important", isBuiltIn = true, sortOrder = 2),
    Category(name = "琐事", colorArgb = 0xFF4A7DFF.toInt(), iconKey = "chore", isBuiltIn = true, sortOrder = 3),
    Category(name = "娱乐项目", colorArgb = 0xFF9B5DE5.toInt(), iconKey = "fun", isBuiltIn = true, sortOrder = 4),
    Category(name = "见面会", colorArgb = 0xFF00A5A5.toInt(), iconKey = "meetup", isBuiltIn = true, sortOrder = 5),
)

/** 截图3 的 8 色调色板 */
val CATEGORY_PALETTE: List<Int> = listOf(
    0xFFEE6C7A.toInt(), // 珊瑚红
    0xFFF2994A.toInt(), // 橙
    0xFFF2C94C.toInt(), // 黄
    0xFF4ECD8F.toInt(), // 绿
    0xFF45C4B8.toInt(), // 青
    0xFF5B8DEF.toInt(), // 蓝
    0xFFA97BF0.toInt(), // 紫
    0xFF4A5568.toInt(), // 深灰蓝
)

/** 找不到类别时的兜底：琐事 */
const val FALLBACK_CATEGORY_NAME = "琐事"
```

- [ ] **Step 2: 写 VoyageRepository（含引导播种，spec §4.1）**

`data/repo/VoyageRepository.kt`：

```kotlin
package com.cruise.assistant.data.repo

import com.cruise.assistant.data.BUILT_IN_CATEGORIES
import com.cruise.assistant.data.db.dao.CategoryDao
import com.cruise.assistant.data.db.dao.EventDao
import com.cruise.assistant.data.db.dao.LocationPresetDao
import com.cruise.assistant.data.db.dao.VoyageDao
import com.cruise.assistant.data.db.toDomain
import com.cruise.assistant.data.db.toEntity
import com.cruise.assistant.domain.CruiseEvent
import com.cruise.assistant.domain.DayPlan
import com.cruise.assistant.domain.Voyage
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.map
import java.time.LocalDate
import java.time.LocalTime

/** 上船事件固定时长 15 分钟（需求7） */
private const val BOARDING_DURATION_MINUTES = 15
private const val DISEMBARK_DURATION_MINUTES = 30

class VoyageRepository(
    private val voyageDao: VoyageDao,
    private val eventDao: EventDao,
    private val categoryDao: CategoryDao,
    private val locationPresetDao: LocationPresetDao,
) {
    fun observeVoyage(): Flow<Voyage?> =
        voyageDao.observeVoyage().map { it?.toDomain() }

    suspend fun getVoyage(): Voyage? = voyageDao.getVoyage()?.toDomain()

    fun observeDayPlans(): Flow<Map<LocalDate, DayPlan>> =
        voyageDao.observeDayPlans().map { list ->
            list.associate { it.date to it.toDomain() }
        }

    /**
     * 需求7：创建航次并生成初始时间轴。
     * 1) 写 voyage  2) 每天建 DayPlan(finalized=false)
     * 3) 播种内置类别（仅当类别表为空）
     * 4) 插入上船（boardTime 起 15 分钟，不设地址）与下船（末日 07:00）两个系统事件
     */
    suspend fun createVoyage(
        shipName: String?,
        boardDate: LocalDate,
        boardTime: LocalTime,
        disembarkDate: LocalDate,
        maxDeck: Int,
    ): Voyage {
        val voyage = Voyage(
            id = 1,
            shipName = shipName?.trim()?.takeIf { it.isNotEmpty() },
            boardDate = boardDate,
            boardTime = boardTime,
            disembarkDate = disembarkDate,
            disembarkTime = LocalTime.of(7, 0),
            maxDeck = maxDeck,
        )
        voyageDao.upsertVoyage(voyage.toEntity())
        voyageDao.upsertDayPlans(
            voyage.datesInVoyage().map { DayPlan(date = it).toEntity() },
        )

        if (categoryDao.count() == 0) {
            categoryDao.insertAll(BUILT_IN_CATEGORIES.map { it.toEntity() })
        }
        val importantId = categoryDao.observeAllOnce().firstOrNull { it.iconKey == "important" }?.id
            ?: categoryDao.observeAllOnce().first().id

        eventDao.insertAll(
            listOf(
                CruiseEvent(
                    date = boardDate,
                    startTime = boardTime,
                    durationMinutes = BOARDING_DURATION_MINUTES,
                    categoryId = importantId,
                    title = "上船",
                    isSystemGenerated = true,
                ).toEntity(),
                CruiseEvent(
                    date = disembarkDate,
                    startTime = voyage.disembarkTime,
                    durationMinutes = DISEMBARK_DURATION_MINUTES,
                    categoryId = importantId,
                    title = "下船",
                    isSystemGenerated = true,
                ).toEntity(),
            ),
        )
        return voyage
    }

    /**
     * 修改航次日期/时间：同步 DayPlan 集合，并把上船/下船系统事件挪到新时间。
     * 落在新范围外的普通事件会被删除（调用方负责先提示用户）。
     */
    suspend fun updateVoyage(voyage: Voyage) {
        voyageDao.upsertVoyage(voyage.toEntity())

        val validDates = voyage.datesInVoyage().toSet()
        val existingPlans = voyageDao.observeDayPlansOnce().map { it.date }.toSet()
        val newPlans = validDates.filter { it !in existingPlans }.map { DayPlan(date = it).toEntity() }
        if (newPlans.isNotEmpty()) voyageDao.upsertDayPlans(newPlans)

        val allEvents = eventDao.getAll()

        // 系统事件（上船/下船）绝不删除 —— 它们要被挪到新时间。
        // 若先删掉，下面的 update 会打在已不存在的行上：Room 的
        // `UPDATE ... WHERE id = ?` 影响 0 行且不抛异常，事件会静默消失。
        allEvents
            .filter { it.date !in validDates && !it.isSystemGenerated }
            .forEach { eventDao.delete(it) }

        allEvents.forEach { entity ->
            if (!entity.isSystemGenerated) return@forEach
            when (entity.title) {
                "上船" -> eventDao.update(
                    entity.copy(date = voyage.boardDate, startTime = voyage.boardTime),
                )
                "下船" -> eventDao.update(
                    entity.copy(date = voyage.disembarkDate, startTime = voyage.disembarkTime),
                )
            }
        }
    }

    /** 设置页"重建航次"：清空一切（类别保留，因为用户可能改过颜色） */
    suspend fun resetVoyage() {
        eventDao.clearAll()
        voyageDao.clearDayPlans()
        voyageDao.clearVoyage()
        locationPresetDao.clearAll()
    }

    /** 需求8：标记/取消某天定稿 */
    suspend fun setFinalized(date: LocalDate, finalized: Boolean, nowMs: Long) {
        voyageDao.upsertDayPlan(
            DayPlan(
                date = date,
                finalized = finalized,
                finalizedAt = if (finalized) nowMs else null,
            ).toEntity(),
        )
    }
}
```

这需要给 DAO 补两个一次性查询方法。**修改** `data/db/dao/CategoryDao.kt`，加：

```kotlin
    @Query("SELECT * FROM category ORDER BY sortOrder, id")
    suspend fun observeAllOnce(): List<CategoryEntity>
```

**修改** `data/db/dao/VoyageDao.kt`，加：

```kotlin
    @Query("SELECT * FROM day_plan ORDER BY date")
    suspend fun observeDayPlansOnce(): List<DayPlanEntity>
```

- [ ] **Step 3: 写 EventRepository（接 assignLane 与 expandRecurrence）**

`data/repo/EventRepository.kt`：

```kotlin
package com.cruise.assistant.data.repo

import com.cruise.assistant.data.db.dao.EventDao
import com.cruise.assistant.data.db.dao.VoyageDao
import com.cruise.assistant.data.db.toDomain
import com.cruise.assistant.data.db.toEntity
import com.cruise.assistant.domain.CruiseEvent
import com.cruise.assistant.domain.assignLane
import com.cruise.assistant.domain.endMinute
import com.cruise.assistant.domain.expandRecurrence
import com.cruise.assistant.domain.findConflicts
import com.cruise.assistant.domain.startMinute
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.map
import java.time.LocalDate
import java.util.UUID

/** 编辑/删除循环组内事件时的作用域 */
enum class RecurrenceScope { THIS_DAY_ONLY, WHOLE_GROUP }

class EventRepository(
    private val eventDao: EventDao,
    private val voyageDao: VoyageDao,
) {
    fun observeByDate(date: LocalDate): Flow<List<CruiseEvent>> =
        eventDao.observeByDate(date).map { list -> list.map { it.toDomain() } }

    fun observeDatesWithEvents(): Flow<List<LocalDate>> = eventDao.observeDatesWithEvents()

    suspend fun getById(id: Long): CruiseEvent? = eventDao.getById(id)?.toDomain()

    /**
     * 新增事件。recurring=true 时按需求6 展开到下船日前一天（下船日不含）。
     * 每一天各自跑 assignLane，保证 3 栏不重叠。
     * @return 实际写入的条数
     */
    suspend fun addEvent(event: CruiseEvent, recurring: Boolean): Int {
        val voyage = voyageDao.getVoyage()?.toDomain()
        val copies = if (recurring && voyage != null) {
            expandRecurrence(event, voyage.disembarkDate, UUID.randomUUID().toString())
        } else {
            listOf(event.copy(id = 0))
        }
        copies.forEach { copy ->
            val sameDay = eventDao.getByDate(copy.date).map { it.toDomain() }
            val lane = assignLane(copy.startMinute, copy.endMinute, sameDay)
            eventDao.insert(copy.copy(lane = lane).toEntity())
        }
        return copies.size
    }

    /**
     * 更新事件。WHOLE_GROUP 时把同组所有事件的时间/内容/地点/类别一起改，
     * 但各自重算 lane（不同日期的邻居不同）。
     */
    suspend fun updateEvent(event: CruiseEvent, scope: RecurrenceScope) {
        val groupId = event.recurrenceGroupId
        val targets = if (scope == RecurrenceScope.WHOLE_GROUP && groupId != null) {
            eventDao.getByGroup(groupId).map { it.toDomain() }
        } else {
            listOf(event)
        }
        targets.forEach { target ->
            val merged = event.copy(
                id = target.id,
                date = target.date,
                recurrenceGroupId = target.recurrenceGroupId,
                isSystemGenerated = target.isSystemGenerated,
            )
            // spec §4.3：编辑时若时间未变则保留原 lane，避免改个标题就让卡片跳栏。
            // WHOLE_GROUP 时每个成员各自比自己的原时间，不能拿单一参照事件比。
            val timingUnchanged = merged.startTime == target.startTime &&
                merged.durationMinutes == target.durationMinutes
            val lane = if (timingUnchanged) {
                target.lane
            } else {
                val sameDay = eventDao.getByDate(merged.date).map { it.toDomain() }
                assignLane(merged.startMinute, merged.endMinute, sameDay, excludeId = merged.id)
            }
            eventDao.update(merged.copy(lane = lane).toEntity())
        }
    }

    /** 删除。系统生成的上船/下船事件不允许删除，静默忽略。 */
    suspend fun deleteEvent(event: CruiseEvent, scope: RecurrenceScope) {
        if (event.isSystemGenerated) return
        val groupId = event.recurrenceGroupId
        if (scope == RecurrenceScope.WHOLE_GROUP && groupId != null) {
            eventDao.deleteGroup(groupId)
        } else {
            eventDao.delete(event.toEntity())
        }
    }

    /** 长按卡片手动换栏（需求4） */
    suspend fun moveToLane(eventId: Long, lane: Int) {
        val entity = eventDao.getById(eventId) ?: return
        eventDao.update(entity.copy(lane = lane))
    }

    /** 需求8：定稿前的冲突检查 */
    suspend fun conflictsOn(date: LocalDate): List<Pair<CruiseEvent, CruiseEvent>> =
        findConflicts(eventDao.getByDate(date).map { it.toDomain() })

    /** 提醒排程用：取所有事件供 scheduler 过滤未来项 */
    suspend fun getAllEvents(): List<CruiseEvent> = eventDao.getAll().map { it.toDomain() }

    suspend fun getByDate(date: LocalDate): List<CruiseEvent> =
        eventDao.getByDate(date).map { it.toDomain() }
}
```

- [ ] **Step 4: 写 CategoryRepository 与 LocationPresetRepository**

`data/repo/CategoryRepository.kt`：

```kotlin
package com.cruise.assistant.data.repo

import com.cruise.assistant.data.db.dao.CategoryDao
import com.cruise.assistant.data.db.dao.EventDao
import com.cruise.assistant.data.db.toDomain
import com.cruise.assistant.data.db.toEntity
import com.cruise.assistant.domain.Category
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.map

class CategoryRepository(
    private val categoryDao: CategoryDao,
    private val eventDao: EventDao,
) {
    fun observeAll(): Flow<List<Category>> =
        categoryDao.observeAll().map { list -> list.map { it.toDomain() } }

    fun observeActive(): Flow<List<Category>> =
        categoryDao.observeActive().map { list -> list.map { it.toDomain() } }

    suspend fun getById(id: Long): Category? = categoryDao.getById(id)?.toDomain()

    /** id==0 插入，否则更新。返回类别 id。 */
    suspend fun save(category: Category): Long =
        if (category.id == 0L) {
            categoryDao.insert(category.toEntity())
        } else {
            categoryDao.update(category.toEntity())
            category.id
        }

    suspend fun countEvents(categoryId: Long): Int = eventDao.countByCategory(categoryId)

    /**
     * 删除自定义类别。内置类别不允许删除（静默忽略）。
     * 引用它的事件迁移到 fallbackCategoryId。
     */
    suspend fun delete(category: Category, fallbackCategoryId: Long) {
        if (category.isBuiltIn) return
        eventDao.reassignCategory(category.id, fallbackCategoryId)
        categoryDao.delete(category.toEntity())
    }
}
```

`data/repo/LocationPresetRepository.kt`：

```kotlin
package com.cruise.assistant.data.repo

import com.cruise.assistant.data.db.dao.LocationPresetDao
import com.cruise.assistant.data.db.toDomain
import com.cruise.assistant.data.db.toEntity
import com.cruise.assistant.domain.LocationPreset
import com.cruise.assistant.domain.ShipZone
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.map

class LocationPresetRepository(private val dao: LocationPresetDao) {

    fun observeAll(): Flow<List<LocationPreset>> =
        dao.observeAll().map { list -> list.map { it.toDomain() } }

    /** 保存事件时调用：已存在则 useCount+1 并更新时间，否则新建 */
    suspend fun remember(deck: Int, zone: ShipZone, placeName: String, nowMs: Long) {
        val trimmed = placeName.trim()
        if (trimmed.isEmpty()) return
        val existing = dao.find(deck, zone.name, trimmed)
        if (existing == null) {
            dao.upsert(
                LocationPreset(
                    deck = deck, zone = zone, placeName = trimmed,
                    lastUsedEpochMs = nowMs, useCount = 1,
                ).toEntity(),
            )
        } else {
            dao.upsert(existing.copy(lastUsedEpochMs = nowMs, useCount = existing.useCount + 1))
        }
    }

    suspend fun delete(preset: LocationPreset) = dao.delete(preset.toEntity())
}
```

- [ ] **Step 5: 写 AppContainer 并接到 Application**

`AppContainer.kt`：

```kotlin
package com.cruise.assistant

import android.content.Context
import com.cruise.assistant.data.db.CruiseDatabase
import com.cruise.assistant.data.prefs.SettingsStore
import com.cruise.assistant.data.repo.CategoryRepository
import com.cruise.assistant.data.repo.EventRepository
import com.cruise.assistant.data.repo.LocationPresetRepository
import com.cruise.assistant.data.repo.VoyageRepository

/** 手写依赖容器，不引入 DI 框架 */
class AppContainer(context: Context) {

    private val db = CruiseDatabase.get(context)

    val settingsStore = SettingsStore(context)

    val voyageRepository = VoyageRepository(
        voyageDao = db.voyageDao(),
        eventDao = db.eventDao(),
        categoryDao = db.categoryDao(),
        locationPresetDao = db.locationPresetDao(),
    )

    val eventRepository = EventRepository(
        eventDao = db.eventDao(),
        voyageDao = db.voyageDao(),
    )

    val categoryRepository = CategoryRepository(
        categoryDao = db.categoryDao(),
        eventDao = db.eventDao(),
    )

    val locationPresetRepository = LocationPresetRepository(db.locationPresetDao())
}
```

`CruiseApp.kt`（替换 Task 1 的最简版）：

```kotlin
package com.cruise.assistant

import android.app.Application

class CruiseApp : Application() {
    lateinit var container: AppContainer
        private set

    override fun onCreate() {
        super.onCreate()
        container = AppContainer(this)
    }
}
```

- [ ] **Step 6: 验证编译**

Run: `./gradlew assembleDebug`
Expected: `BUILD SUCCESSFUL`

- [ ] **Step 7: Commit**

```bash
git add app/src/main/java/com/cruise/assistant
git commit -m "feat: add repositories wiring lane assignment and recurrence to persistence"
```

## Task 5: 主题、图标注册表、复用组件

**Files:**
- Create: `ui/theme/Color.kt`, `ui/theme/Theme.kt`, `ui/theme/Type.kt`
- Create: `ui/common/CategoryIcons.kt`（iconKey → ImageVector 注册表）
- Create: `ui/common/TimeWheel.kt`（截图2 的双滚轮）
- Create: `ui/common/ColorDot.kt`（截图3 的调色圆点）
- Create: `ui/common/CategoryChip.kt`
- Modify: `app/src/main/res/values/strings.xml`（补全文案）

**Interfaces:**
- Consumes: Task 4 的 `CATEGORY_PALETTE`
- Produces:
  - `@Composable fun CruiseTheme(content: @Composable () -> Unit)`
  - `fun iconFor(iconKey: String): ImageVector`；`val ICON_REGISTRY: List<Pair<String, ImageVector>>`
  - `@Composable fun TimeWheel(value: LocalTime, onValueChange: (LocalTime) -> Unit, modifier)`
  - `@Composable fun ColorDot(colorArgb: Int, selected: Boolean, onClick: () -> Unit)`
  - `@Composable fun CategoryChip(name: String, colorArgb: Int, modifier)`
  - `fun Int.toComposeColor(): Color`；`fun Color.onSurfaceVariantFor(): Color`（卡片文字色）
  - `fun cardBackgroundFor(colorArgb: Int): Color`（12% alpha 底色）
  - `fun cardTextColorFor(colorArgb: Int): Color`（深化后的文字色）

- [ ] **Step 1: 写主题**

`ui/theme/Color.kt`：

```kotlin
package com.cruise.assistant.ui.theme

import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.lerp

val CruiseBlue = Color(0xFF2C5F8D)
val CruiseBlueLight = Color(0xFF5B8DEF)
val SurfaceGray = Color(0xFFF6F7F9)
val GridLine = Color(0xFFE8EAED)
val NowRed = Color(0xFFE8563F)

fun Int.toComposeColor(): Color = Color(this)

/** 卡片底色：类别色 12% 叠在白底上（截图1 的观感） */
fun cardBackgroundFor(colorArgb: Int): Color =
    lerp(Color.White, Color(colorArgb), 0.12f)

/** 卡片文字色：类别色往黑压 35%，保证在浅底上对比度足够 */
fun cardTextColorFor(colorArgb: Int): Color =
    lerp(Color(colorArgb), Color.Black, 0.35f)
```

`ui/theme/Type.kt`：

```kotlin
package com.cruise.assistant.ui.theme

import androidx.compose.material3.Typography
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.sp

val CruiseTypography = Typography(
    titleLarge = TextStyle(fontSize = 20.sp, fontWeight = FontWeight.SemiBold),
    titleMedium = TextStyle(fontSize = 16.sp, fontWeight = FontWeight.SemiBold),
    bodyMedium = TextStyle(fontSize = 14.sp),
    bodySmall = TextStyle(fontSize = 12.sp),
    labelSmall = TextStyle(fontSize = 11.sp, fontWeight = FontWeight.Medium),
)
```

`ui/theme/Theme.kt`：

```kotlin
package com.cruise.assistant.ui.theme

import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.lightColorScheme
import androidx.compose.runtime.Composable

private val LightColors = lightColorScheme(
    primary = CruiseBlue,
    secondary = CruiseBlueLight,
    background = SurfaceGray,
    surface = androidx.compose.ui.graphics.Color.White,
)

/**
 * 应用固定使用浅色配色，不跟随系统深色模式：spec §10「明确不做」把深色模式列为不做项，
 * 且 [cardBackgroundFor] / [cardTextColorFor] 都假定浅色底（以白为基混合类别色），
 * 若跟随系统深色模式会导致深色背景上浮出近白色卡片，视觉不自洽。
 */
@Composable
fun CruiseTheme(content: @Composable () -> Unit) {
    MaterialTheme(
        colorScheme = LightColors,
        typography = CruiseTypography,
        content = content,
    )
}
```

- [ ] **Step 2: 写图标注册表**

`ui/common/CategoryIcons.kt` —— 全部用 `material-icons-extended` 里的内置矢量，
不引入外部图标包。截图3 的图标网格就渲染 `ICON_REGISTRY`：

```kotlin
package com.cruise.assistant.ui.common

import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Search
import androidx.compose.material.icons.outlined.Apps
import androidx.compose.material.icons.outlined.Casino
import androidx.compose.material.icons.outlined.Celebration
import androidx.compose.material.icons.outlined.CheckCircle
import androidx.compose.material.icons.outlined.Delete
import androidx.compose.material.icons.outlined.DirectionsBoat
import androidx.compose.material.icons.outlined.DirectionsRun
import androidx.compose.material.icons.outlined.FitnessCenter
import androidx.compose.material.icons.outlined.Groups
import androidx.compose.material.icons.outlined.LocalBar
import androidx.compose.material.icons.outlined.MenuBook
import androidx.compose.material.icons.outlined.NightsStay
import androidx.compose.material.icons.outlined.Person
import androidx.compose.material.icons.outlined.PhotoCamera
import androidx.compose.material.icons.outlined.PlayCircleOutline
import androidx.compose.material.icons.outlined.Pool
import androidx.compose.material.icons.outlined.Restaurant
import androidx.compose.material.icons.outlined.ShoppingBag
import androidx.compose.material.icons.outlined.Spa
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
    "run" to Icons.Outlined.DirectionsRun,
    "shop" to Icons.Outlined.ShoppingBag,
    "photo" to Icons.Outlined.PhotoCamera,
    "book" to Icons.Outlined.MenuBook,
    "party" to Icons.Outlined.Celebration,
    "sleep" to Icons.Outlined.NightsStay,
    "work" to Icons.Outlined.WorkOutline,
    "person" to Icons.Outlined.Person,
    "check" to Icons.Outlined.CheckCircle,
    "trash" to Icons.Outlined.Delete,
    "search" to Icons.Filled.Search,
)

private val iconMap = ICON_REGISTRY.toMap()

/** 未知 key 兜底成"琐事"图标，绝不抛异常 */
fun iconFor(iconKey: String): ImageVector = iconMap[iconKey] ?: Icons.Outlined.Apps
```

- [ ] **Step 3: 写 TimeWheel（截图2 的双滚轮）**

`ui/common/TimeWheel.kt` —— 用 `LazyColumn` + `rememberLazyListState` 做吸附滚轮，
每列显示 3 行、中间高亮：

```kotlin
package com.cruise.assistant.ui.common

import androidx.compose.foundation.background
import androidx.compose.foundation.gestures.snapping.rememberSnapFlingBehavior
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.rememberLazyListState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.derivedStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.snapshotFlow
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import java.time.LocalTime

private val ROW_HEIGHT = 44.dp

/** 截图2 的时间滚轮：小时列 + 分钟列，中间行为选中值 */
@Composable
fun TimeWheel(
    value: LocalTime,
    onValueChange: (LocalTime) -> Unit,
    modifier: Modifier = Modifier,
    minuteStep: Int = 5,
) {
    val minuteValues = remember(minuteStep) { (0 until 60 step minuteStep).toList() }

    Box(
        modifier = modifier
            .clip(RoundedCornerShape(16.dp))
            .background(MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.4f)),
        contentAlignment = Alignment.Center,
    ) {
        // 中间高亮条
        Box(
            Modifier
                .fillMaxWidth()
                .height(ROW_HEIGHT)
                .background(MaterialTheme.colorScheme.surfaceVariant),
        )
        Row(
            Modifier.padding(vertical = 4.dp),
            horizontalArrangement = Arrangement.Center,
        ) {
            WheelColumn(
                items = (0..23).toList(),
                selected = value.hour,
                onSelect = { onValueChange(value.withHour(it)) },
                format = { "%02d".format(it) },
                modifier = Modifier.width(72.dp),
            )
            Text(
                ":",
                fontSize = 22.sp,
                fontWeight = FontWeight.Bold,
                modifier = Modifier.align(Alignment.CenterVertically),
            )
            WheelColumn(
                items = minuteValues,
                selected = minuteValues.minByOrNull { kotlin.math.abs(it - value.minute) }
                    ?: 0,
                onSelect = { onValueChange(value.withMinute(it)) },
                format = { "%02d".format(it) },
                modifier = Modifier.width(72.dp),
            )
        }
    }
}

@Composable
private fun WheelColumn(
    items: List<Int>,
    selected: Int,
    onSelect: (Int) -> Unit,
    format: (Int) -> String,
    modifier: Modifier = Modifier,
) {
    val selectedIndex = items.indexOf(selected).coerceAtLeast(0)
    val state = rememberLazyListState(initialFirstVisibleItemIndex = selectedIndex)

    // 外部值变化时同步滚动
    LaunchedEffect(selected) {
        val target = items.indexOf(selected).coerceAtLeast(0)
        if (state.firstVisibleItemIndex != target) state.scrollToItem(target)
    }

    // 滚动停止后回调选中值
    val centeredIndex by remember {
        derivedStateOf {
            val offset = if (state.firstVisibleItemScrollOffset > 0) 0 else 0
            (state.firstVisibleItemIndex + offset).coerceIn(0, items.lastIndex)
        }
    }
    LaunchedEffect(state) {
        snapshotFlow { state.isScrollInProgress to centeredIndex }
            .collect { (scrolling, index) ->
                if (!scrolling && items[index] != selected) onSelect(items[index])
            }
    }

    LazyColumn(
        state = state,
        modifier = modifier.height(ROW_HEIGHT * 3),
        flingBehavior = rememberSnapFlingBehavior(state),
        contentPadding = androidx.compose.foundation.layout.PaddingValues(vertical = ROW_HEIGHT),
    ) {
        items(items.size) { index ->
            val isSelected = index == centeredIndex
            Box(
                Modifier.fillMaxWidth().height(ROW_HEIGHT),
                contentAlignment = Alignment.Center,
            ) {
                Text(
                    text = format(items[index]),
                    fontSize = if (isSelected) 24.sp else 18.sp,
                    fontWeight = if (isSelected) FontWeight.Bold else FontWeight.Normal,
                    color = if (isSelected) {
                        MaterialTheme.colorScheme.onSurface
                    } else {
                        MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.5f)
                    },
                    textAlign = TextAlign.Center,
                )
            }
        }
    }
}
```

- [ ] **Step 4: 写 ColorDot 与 CategoryChip**

`ui/common/ColorDot.kt`：

```kotlin
package com.cruise.assistant.ui.common

import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.unit.dp

/** 截图3 的主题色圆点，选中时加深色描边 */
@Composable
fun ColorDot(
    colorArgb: Int,
    selected: Boolean,
    onClick: () -> Unit,
    modifier: Modifier = Modifier,
) {
    Box(
        modifier
            .size(40.dp)
            .background(Color(colorArgb), CircleShape)
            .then(
                if (selected) Modifier.border(3.dp, Color.Black, CircleShape) else Modifier,
            )
            .clickable(onClick = onClick),
    )
}
```

`ui/common/CategoryChip.kt`：

```kotlin
package com.cruise.assistant.ui.common

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import com.cruise.assistant.ui.theme.cardTextColorFor

/** 截图1 卡片第一行里的类别小标签 */
@Composable
fun CategoryChip(name: String, colorArgb: Int, modifier: Modifier = Modifier) {
    Text(
        text = name,
        style = MaterialTheme.typography.labelSmall,
        color = cardTextColorFor(colorArgb),
        maxLines = 1,
        overflow = TextOverflow.Ellipsis,
        modifier = modifier
            .background(
                androidx.compose.ui.graphics.Color(colorArgb).copy(alpha = 0.18f),
                RoundedCornerShape(4.dp),
            )
            .padding(horizontal = 5.dp, vertical = 1.dp),
    )
}
```

- [ ] **Step 5: 补全 strings.xml**

`app/src/main/res/values/strings.xml` —— 全量文案，后续 task 直接引用：

```xml
<?xml version="1.0" encoding="utf-8"?>
<resources>
    <string name="app_name">邮轮助手</string>

    <!-- 引导页 -->
    <string name="onboarding_title">添加航次</string>
    <string name="onboarding_subtitle">填好上下船时间，自动生成时间轴</string>
    <string name="ship_name_label">船名（可选）</string>
    <string name="board_date_label">上船日期</string>
    <string name="board_time_label">上船时间</string>
    <string name="disembark_date_label">下船日期</string>
    <string name="disembark_time_hint">下船时间固定为当天 07:00</string>
    <string name="max_deck_label">最高层数</string>
    <string name="generate_timeline">生成时间轴</string>
    <string name="error_disembark_before_board">下船日期不能早于上船日期</string>

    <!-- 时间轴 -->
    <string name="day_index_format">第 %1$d/%2$d 天</string>
    <string name="finalized_badge">已定稿</string>
    <string name="unlock">解锁</string>
    <string name="generate_plan">生成计划表</string>
    <string name="now_marker">现在</string>
    <string name="ongoing_label">进行中</string>
    <string name="next_label">下一个</string>
    <string name="hidden_before_format">上方还有 %1$d 件</string>
    <string name="hidden_after_format">下方还有 %1$d 件</string>
    <string name="show_full_day">看全天</string>
    <string name="empty_day">这一天还没有安排，点右下角 + 添加</string>
    <string name="add_event">添加事件</string>
    <string name="edit_event">编辑事件</string>
    <string name="move_to_lane_format">移到第 %1$d 栏</string>
    <string name="delete">删除</string>
    <string name="edit">编辑</string>
    <string name="cancel">取消</string>
    <string name="confirm">确认</string>
    <string name="system_event_undeletable">上船/下船不能删除，只能改时间</string>

    <!-- 事件弹窗 -->
    <string name="select_category">选择事项</string>
    <string name="manage_categories">编辑</string>
    <string name="event_title_hint">事件内容</string>
    <string name="location_label">地点</string>
    <string name="deck_label">楼层</string>
    <string name="place_name_hint">地点名（可选）</string>
    <string name="no_location">不设地点</string>
    <string name="start_time">开始时间</string>
    <string name="end_time">结束时间</string>
    <string name="recur_daily">每日同一时间循环</string>
    <string name="recur_until_format">循环至 %1$s（下船日不含）</string>
    <string name="error_end_before_start">结束时间必须晚于开始时间</string>
    <string name="error_title_required">请填写事件内容</string>
    <string name="error_no_category">请选择事项</string>
    <string name="recur_scope_title">这是一个重复事件</string>
    <string name="recur_scope_this_day">仅本日</string>
    <string name="recur_scope_whole_group">所有重复项</string>

    <!-- 类别弹窗 -->
    <string name="category_sheet_title_add">添加图标</string>
    <string name="category_sheet_title_edit">编辑图标</string>
    <string name="category_name_label">名称</string>
    <string name="category_name_hint">输入名称</string>
    <string name="sub_category">二级分类</string>
    <string name="parent_category_label">上级分类</string>
    <string name="archive">归档</string>
    <string name="timeline_theme_color">时间轴主题色</string>
    <string name="select_icon">选择图标</string>
    <string name="save_changes">保存修改</string>
    <string name="builtin_undeletable">内置事项不能删除</string>
    <string name="delete_category_warning_format">有 %1$d 个事件在用它，删除后会移到"琐事"</string>

    <!-- 定稿 -->
    <string name="conflict_dialog_title">发现时间冲突</string>
    <string name="conflict_dialog_none">没有冲突，可以生成计划表</string>
    <string name="conflict_item_format">%1$s %2$s ⇄ %3$s %4$s</string>
    <string name="go_fix">去修改</string>
    <string name="ignore_and_finalize">忽略冲突继续生成</string>

    <!-- 显示范围 -->
    <string name="display_range_title">显示时间范围</string>
    <string name="range_from">起</string>
    <string name="range_to">止</string>
    <string name="hour_height_label">每小时高度</string>
    <string name="collapse_empty_gaps">自动隐藏无事件的空时段</string>
    <string name="error_range_too_short">显示范围至少要 2 小时</string>

    <!-- 设置 -->
    <string name="settings">设置</string>
    <string name="reminder_section">提醒</string>
    <string name="reminder_enabled">开启提醒</string>
    <string name="reminder_lead_time">提前提醒</string>
    <string name="minutes_format">%1$d 分钟</string>
    <string name="voyage_section">航次</string>
    <string name="edit_voyage">修改航次</string>
    <string name="reset_voyage">重建航次</string>
    <string name="reset_voyage_warning">会清空所有事件和计划，无法恢复。确定吗？</string>
    <string name="category_section">事项管理</string>
    <string name="permission_notification_rationale">需要通知权限才能在事件开始前提醒你</string>
    <string name="exact_alarm_unavailable">系统未授予精确闹钟权限，提醒可能会延迟几分钟</string>

    <!-- 通知 -->
    <string name="notification_channel_name">行程提醒</string>
    <string name="notification_channel_desc">事件开始前提醒</string>
    <string name="notification_title_format">%1$s · %2$s</string>
</resources>
```

- [ ] **Step 6: 把 MainActivity 换成 CruiseTheme 并验证**

修改 `MainActivity.kt` 的 `setContent`：把 `MaterialTheme {` 换成 `CruiseTheme {`
（import `com.cruise.assistant.ui.theme.CruiseTheme`）。

Run: `./gradlew assembleDebug`
Expected: `BUILD SUCCESSFUL`

- [ ] **Step 7: Commit**

```bash
git add app/src/main/java/com/cruise/assistant/ui app/src/main/res
git commit -m "feat: add theme, icon registry, time wheel, and shared components"
```

## Task 6: 引导页与导航骨架（需求7）

**Files:**
- Create: `ui/onboarding/OnboardingScreen.kt`, `ui/onboarding/OnboardingViewModel.kt`
- Create: `ui/CruiseNavHost.kt`
- Create: `ui/common/ViewModelFactory.kt`
- Modify: `MainActivity.kt`

**Interfaces:**
- Consumes: `VoyageRepository.createVoyage(...)`、`SettingsStore.settings` / `setOnboardingCompleted`
- Produces:
  - `@Composable fun CruiseNavHost(container: AppContainer)`
  - `object Routes { const val ONBOARDING = "onboarding"; const val TIMELINE = "timeline"; const val SETTINGS = "settings" }`
  - `fun <T : ViewModel> viewModelFactory(create: () -> T): ViewModelProvider.Factory`
  - `class OnboardingViewModel(voyageRepository, settingsStore)` 暴露
    `state: StateFlow<OnboardingState>`、`onBoardDateChange` / `onBoardTimeChange` /
    `onDisembarkDateChange` / `onShipNameChange` / `onMaxDeckChange` / `submit(onDone: () -> Unit)`

- [ ] **Step 1: 写 ViewModel 工厂辅助**

`ui/common/ViewModelFactory.kt`：

```kotlin
package com.cruise.assistant.ui.common

import androidx.lifecycle.ViewModel
import androidx.lifecycle.ViewModelProvider

/** 无 DI 框架下的轻量工厂 */
@Suppress("UNCHECKED_CAST")
fun <T : ViewModel> viewModelFactory(create: () -> T): ViewModelProvider.Factory =
    object : ViewModelProvider.Factory {
        override fun <VM : ViewModel> create(modelClass: Class<VM>): VM = create() as VM
    }
```

- [ ] **Step 2: 写 OnboardingViewModel**

`ui/onboarding/OnboardingViewModel.kt`：

```kotlin
package com.cruise.assistant.ui.onboarding

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.cruise.assistant.data.prefs.SettingsStore
import com.cruise.assistant.data.repo.VoyageRepository
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch
import java.time.LocalDate
import java.time.LocalTime

data class OnboardingState(
    val shipName: String = "",
    val boardDate: LocalDate,
    val boardTime: LocalTime = LocalTime.of(12, 0),
    val disembarkDate: LocalDate,
    val maxDeck: Int = 20,
    val saving: Boolean = false,
    /** 已本地化的错误提示 resId，null 表示无错误 */
    val errorResId: Int? = null,
)

class OnboardingViewModel(
    private val voyageRepository: VoyageRepository,
    private val settingsStore: SettingsStore,
    today: LocalDate,
) : ViewModel() {

    private val _state = MutableStateFlow(
        OnboardingState(boardDate = today, disembarkDate = today.plusDays(5)),
    )
    val state: StateFlow<OnboardingState> = _state.asStateFlow()

    fun onShipNameChange(value: String) = _state.update { it.copy(shipName = value) }

    fun onBoardDateChange(value: LocalDate) = _state.update { current ->
        // 上船日往后挪时，下船日至少跟着不早于它
        val disembark = if (current.disembarkDate.isBefore(value)) value else current.disembarkDate
        current.copy(boardDate = value, disembarkDate = disembark, errorResId = null)
    }

    fun onBoardTimeChange(value: LocalTime) = _state.update { it.copy(boardTime = value) }

    fun onDisembarkDateChange(value: LocalDate) = _state.update {
        it.copy(disembarkDate = value, errorResId = null)
    }

    fun onMaxDeckChange(value: Int) = _state.update { it.copy(maxDeck = value.coerceIn(1, 30)) }

    fun submit(onDone: () -> Unit) {
        val current = _state.value
        // 防重入：Button(enabled = !saving) 的 enabled 取自上一次组合，比状态慢一帧，
        // 快速双击会两次进到这里 → createVoyage() 跑两遍 → 变成 4 个系统事件。
        if (current.saving) return
        if (current.disembarkDate.isBefore(current.boardDate)) {
            _state.update {
                it.copy(errorResId = com.cruise.assistant.R.string.error_disembark_before_board)
            }
            return
        }
        _state.update { it.copy(saving = true, errorResId = null) }
        viewModelScope.launch {
            voyageRepository.createVoyage(
                shipName = current.shipName,
                boardDate = current.boardDate,
                boardTime = current.boardTime,
                disembarkDate = current.disembarkDate,
                maxDeck = current.maxDeck,
            )
            settingsStore.setLastViewedDate(current.boardDate)
            settingsStore.setOnboardingCompleted(true)
            _state.update { it.copy(saving = false) }
            onDone()
        }
    }
}
```

- [ ] **Step 3: 写 OnboardingScreen**

`ui/onboarding/OnboardingScreen.kt` —— 日期选择用 Material3 的 `DatePickerDialog`，
时间用 Task 5 的 `TimeWheel`：

```kotlin
package com.cruise.assistant.ui.onboarding

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.Button
import androidx.compose.material3.DatePicker
import androidx.compose.material3.DatePickerDialog
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.material3.rememberDatePickerState
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.unit.dp
import com.cruise.assistant.R
import com.cruise.assistant.ui.common.TimeWheel
import java.time.Instant
import java.time.LocalDate
import java.time.ZoneId
import java.time.format.DateTimeFormatter

private val DATE_FMT = DateTimeFormatter.ofPattern("yyyy年M月d日")

@Composable
fun OnboardingScreen(
    state: OnboardingState,
    onShipNameChange: (String) -> Unit,
    onBoardDateChange: (LocalDate) -> Unit,
    onBoardTimeChange: (java.time.LocalTime) -> Unit,
    onDisembarkDateChange: (LocalDate) -> Unit,
    onMaxDeckChange: (Int) -> Unit,
    onSubmit: () -> Unit,
) {
    var pickerTarget by remember { mutableStateOf<PickerTarget?>(null) }

    Column(
        Modifier
            .fillMaxSize()
            .verticalScroll(rememberScrollState())
            .padding(24.dp),
    ) {
        Text(stringResource(R.string.onboarding_title), style = MaterialTheme.typography.titleLarge)
        Spacer(Modifier.height(4.dp))
        Text(
            stringResource(R.string.onboarding_subtitle),
            style = MaterialTheme.typography.bodySmall,
            color = MaterialTheme.colorScheme.onSurfaceVariant,
        )
        Spacer(Modifier.height(24.dp))

        OutlinedTextField(
            value = state.shipName,
            onValueChange = onShipNameChange,
            label = { Text(stringResource(R.string.ship_name_label)) },
            singleLine = true,
            modifier = Modifier.fillMaxWidth(),
        )
        Spacer(Modifier.height(20.dp))

        Text(stringResource(R.string.board_date_label), style = MaterialTheme.typography.titleMedium)
        Spacer(Modifier.height(8.dp))
        OutlinedButton(
            onClick = { pickerTarget = PickerTarget.BOARD },
            modifier = Modifier.fillMaxWidth(),
        ) { Text(state.boardDate.format(DATE_FMT)) }

        Spacer(Modifier.height(20.dp))
        Text(stringResource(R.string.board_time_label), style = MaterialTheme.typography.titleMedium)
        Spacer(Modifier.height(8.dp))
        TimeWheel(
            value = state.boardTime,
            onValueChange = onBoardTimeChange,
            modifier = Modifier.fillMaxWidth(),
        )

        Spacer(Modifier.height(20.dp))
        Text(stringResource(R.string.disembark_date_label), style = MaterialTheme.typography.titleMedium)
        Spacer(Modifier.height(8.dp))
        OutlinedButton(
            onClick = { pickerTarget = PickerTarget.DISEMBARK },
            modifier = Modifier.fillMaxWidth(),
        ) { Text(state.disembarkDate.format(DATE_FMT)) }
        Spacer(Modifier.height(4.dp))
        Text(
            stringResource(R.string.disembark_time_hint),
            style = MaterialTheme.typography.bodySmall,
            color = MaterialTheme.colorScheme.onSurfaceVariant,
        )

        Spacer(Modifier.height(20.dp))
        Row(verticalAlignment = Alignment.CenterVertically) {
            Text(
                stringResource(R.string.max_deck_label),
                style = MaterialTheme.typography.titleMedium,
                modifier = Modifier.weight(1f),
            )
            OutlinedButton(onClick = { onMaxDeckChange(state.maxDeck - 1) }) { Text("−") }
            Text(
                "${state.maxDeck}F",
                style = MaterialTheme.typography.titleMedium,
                modifier = Modifier.padding(horizontal = 16.dp),
            )
            OutlinedButton(onClick = { onMaxDeckChange(state.maxDeck + 1) }) { Text("+") }
        }

        state.errorResId?.let { resId ->
            Spacer(Modifier.height(12.dp))
            Text(
                stringResource(resId),
                color = MaterialTheme.colorScheme.error,
                style = MaterialTheme.typography.bodySmall,
            )
        }

        Spacer(Modifier.height(32.dp))
        Button(
            onClick = onSubmit,
            enabled = !state.saving,
            modifier = Modifier.fillMaxWidth().height(52.dp),
        ) { Text(stringResource(R.string.generate_timeline)) }
        Spacer(Modifier.height(24.dp))
    }

    pickerTarget?.let { target ->
        val initial = when (target) {
            PickerTarget.BOARD -> state.boardDate
            PickerTarget.DISEMBARK -> state.disembarkDate
        }
        val pickerState = rememberDatePickerState(
            initialSelectedDateMillis = initial.atStartOfDay(ZoneId.systemDefault())
                .toInstant().toEpochMilli(),
        )
        DatePickerDialog(
            onDismissRequest = { pickerTarget = null },
            confirmButton = {
                TextButton(onClick = {
                    pickerState.selectedDateMillis?.let { millis ->
                        val picked = Instant.ofEpochMilli(millis)
                            .atZone(ZoneId.systemDefault()).toLocalDate()
                        when (target) {
                            PickerTarget.BOARD -> onBoardDateChange(picked)
                            PickerTarget.DISEMBARK -> onDisembarkDateChange(picked)
                        }
                    }
                    pickerTarget = null
                }) { Text(stringResource(R.string.confirm)) }
            },
            dismissButton = {
                TextButton(onClick = { pickerTarget = null }) {
                    Text(stringResource(R.string.cancel))
                }
            },
        ) { DatePicker(state = pickerState) }
    }
}

private enum class PickerTarget { BOARD, DISEMBARK }
```

`DatePicker` 需要 opt-in：在 `OnboardingScreen` 上方加
`@file:OptIn(androidx.compose.material3.ExperimentalMaterial3Api::class)`（放在 package 声明之前）。

- [ ] **Step 4: 写 NavHost 与 MainActivity**

`ui/CruiseNavHost.kt`：

```kotlin
package com.cruise.assistant.ui

import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.lifecycle.viewmodel.compose.viewModel
import androidx.navigation.compose.NavHost
import androidx.navigation.compose.composable
import androidx.navigation.compose.rememberNavController
import com.cruise.assistant.AppContainer
import com.cruise.assistant.ui.common.viewModelFactory
import com.cruise.assistant.ui.onboarding.OnboardingScreen
import com.cruise.assistant.ui.onboarding.OnboardingViewModel
import java.time.LocalDate

object Routes {
    const val ONBOARDING = "onboarding"
    const val TIMELINE = "timeline"
    const val SETTINGS = "settings"
}

@Composable
fun CruiseNavHost(container: AppContainer) {
    val navController = rememberNavController()
    val settings by container.settingsStore.settings.collectAsState(initial = null)

    // 设置还没读出来，先转圈，避免闪一下引导页
    val loaded = settings
    if (loaded == null) {
        Box(Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
            CircularProgressIndicator()
        }
        return
    }

    NavHost(
        navController = navController,
        startDestination = if (loaded.onboardingCompleted) Routes.TIMELINE else Routes.ONBOARDING,
    ) {
        composable(Routes.ONBOARDING) {
            val vm: OnboardingViewModel = viewModel(
                factory = viewModelFactory {
                    OnboardingViewModel(
                        container.voyageRepository,
                        container.settingsStore,
                        LocalDate.now(),
                    )
                },
            )
            val state by vm.state.collectAsState()
            OnboardingScreen(
                state = state,
                onShipNameChange = vm::onShipNameChange,
                onBoardDateChange = vm::onBoardDateChange,
                onBoardTimeChange = vm::onBoardTimeChange,
                onDisembarkDateChange = vm::onDisembarkDateChange,
                onMaxDeckChange = vm::onMaxDeckChange,
                onSubmit = {
                    vm.submit {
                        navController.navigate(Routes.TIMELINE) {
                            popUpTo(Routes.ONBOARDING) { inclusive = true }
                        }
                    }
                },
            )
        }
        composable(Routes.TIMELINE) {
            // Task 7 填入 TimelineScreen
            Box(Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
                androidx.compose.material3.Text("时间轴（Task 7）")
            }
        }
        composable(Routes.SETTINGS) {
            // Task 10 填入 SettingsScreen
            Box(Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
                androidx.compose.material3.Text("设置（Task 10）")
            }
        }
    }
}
```

`MainActivity.kt` 全量替换：

```kotlin
package com.cruise.assistant

import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.compose.foundation.layout.WindowInsets
import androidx.compose.foundation.layout.consumeWindowInsets
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.safeDrawing
import androidx.compose.foundation.layout.windowInsetsPadding
import androidx.compose.material3.Surface
import androidx.compose.ui.Modifier
import com.cruise.assistant.ui.CruiseNavHost
import com.cruise.assistant.ui.theme.CruiseTheme

class MainActivity : ComponentActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()
        val container = (application as CruiseApp).container
        setContent {
            CruiseTheme {
                Surface(
                    Modifier
                        .fillMaxSize()
                        .windowInsetsPadding(WindowInsets.safeDrawing)
                        .consumeWindowInsets(WindowInsets.safeDrawing),
                ) {
                    CruiseNavHost(container)
                }
            }
        }
    }
}
```

- [ ] **Step 5: 在模拟器上验证引导页**

```bash
# 启动模拟器（若未开）
"/c/Users/Administrator/AppData/Local/Android/Sdk/emulator/emulator.exe" -avd Pixel_9 -no-snapshot-load &
# 等设备就绪
"/c/Users/Administrator/AppData/Local/Android/Sdk/platform-tools/adb.exe" wait-for-device
./gradlew installDebug
"/c/Users/Administrator/AppData/Local/Android/Sdk/platform-tools/adb.exe" shell am start -n com.cruise.assistant.debug/com.cruise.assistant.MainActivity
```

截图确认引导页渲染正常：

```bash
ADB="/c/Users/Administrator/AppData/Local/Android/Sdk/platform-tools/adb.exe"
"$ADB" exec-out screencap -p > /tmp/onboarding.png
```

Expected: 看到"添加航次"标题、船名输入框、上船日期按钮、时间滚轮、下船日期、层数加减、生成按钮。
用 Read 工具打开 `/tmp/onboarding.png` 确认。

- [ ] **Step 6: 验证数据真的写进去了**

点"生成时间轴"后：

```bash
ADB="/c/Users/Administrator/AppData/Local/Android/Sdk/platform-tools/adb.exe"
"$ADB" shell "run-as com.cruise.assistant.debug sqlite3 /data/data/com.cruise.assistant.debug/databases/cruise.db 'SELECT title, date, startTime, durationMinutes FROM event;'"
```

Expected: 两行 —— `上船|<上船日>|12:00|15` 和 `下船|<下船日>|07:00|30`

若 `sqlite3` 不在设备上，改用：

```bash
"$ADB" shell "run-as com.cruise.assistant.debug cat /data/data/com.cruise.assistant.debug/databases/cruise.db" > /tmp/cruise.db
```

再本地检查；或直接依赖 Task 7 的界面显示来确认。

- [ ] **Step 7: Commit**

```bash
git add app/src/main/java/com/cruise/assistant
git commit -m "feat: add onboarding screen creating voyage with boarding and disembark events"
```

## Task 7: 时间轴主页（需求1–5，核心）

**Files:**
- Create: `ui/timeline/TimelineViewModel.kt`, `ui/timeline/TimelineScreen.kt`
- Create: `ui/timeline/TimeRuler.kt`, `ui/timeline/EventCard.kt`,
  `ui/timeline/LaneCanvas.kt`, `ui/timeline/DateStrip.kt`, `ui/timeline/NowBanner.kt`
- Modify: `ui/CruiseNavHost.kt`（接入真正的 TimelineScreen）

**Interfaces:**
- Consumes: `buildTimelineLayout` / `minuteToDp` / `dpToMinute` / `hourTicks` /
  `formatLocation` / `EventRepository.observeByDate` / `VoyageRepository.observeDayPlans`
- Produces:
  - `data class TimelineUiState(voyage, selectedDate, events, categories, dayPlans, layout, settings, nowMinute, isToday, ongoing, next, loading)`
  - `class TimelineViewModel(...)` 暴露 `state: StateFlow<TimelineUiState?>`、
    `selectDate(LocalDate)` / `moveToLane(Long, Int)` / `deleteEvent(CruiseEvent, RecurrenceScope)` /
    `finalizeDay(LocalDate)` / `unlockDay(LocalDate)` / `conflictsOn(LocalDate)`
  - `@Composable fun TimelineScreen(container, onOpenSettings, onOpenAddEvent, onOpenEditEvent)`

### Task 7a: TimelineViewModel

- [ ] **Step 1: 写 ViewModel**

`ui/timeline/TimelineViewModel.kt`：

```kotlin
package com.cruise.assistant.ui.timeline

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.cruise.assistant.data.prefs.AppSettings
import com.cruise.assistant.data.prefs.SettingsStore
import com.cruise.assistant.data.repo.CategoryRepository
import com.cruise.assistant.data.repo.EventRepository
import com.cruise.assistant.data.repo.RecurrenceScope
import com.cruise.assistant.data.repo.VoyageRepository
import com.cruise.assistant.domain.Category
import com.cruise.assistant.domain.CruiseEvent
import com.cruise.assistant.domain.DayPlan
import com.cruise.assistant.domain.TimelineLayout
import com.cruise.assistant.domain.Voyage
import com.cruise.assistant.domain.buildTimelineLayout
import com.cruise.assistant.domain.endMinute
import com.cruise.assistant.domain.startMinute
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.flow.flatMapLatest
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.isActive
import kotlinx.coroutines.launch
import java.time.LocalDate
import java.time.LocalTime

data class TimelineUiState(
    val voyage: Voyage?,
    val selectedDate: LocalDate,
    val events: List<CruiseEvent>,
    val categories: Map<Long, Category>,
    val dayPlans: Map<LocalDate, DayPlan>,
    val layout: TimelineLayout,
    val settings: AppSettings,
    val nowMinute: Int,
    val isToday: Boolean,
) {
    val isFinalized: Boolean get() = dayPlans[selectedDate]?.finalized == true

    /** 进行中的事件（仅当天） */
    val ongoing: CruiseEvent?
        get() = if (!isToday) null else events.firstOrNull {
            nowMinute in it.startMinute until it.endMinute
        }

    /** 下一个事件（仅当天） */
    val next: CruiseEvent?
        get() = if (!isToday) null else events
            .filter { it.startMinute > nowMinute }
            .minByOrNull { it.startMinute }
}

@OptIn(ExperimentalCoroutinesApi::class)
class TimelineViewModel(
    private val voyageRepository: VoyageRepository,
    private val eventRepository: EventRepository,
    private val categoryRepository: CategoryRepository,
    private val settingsStore: SettingsStore,
    private val nowProvider: () -> LocalTime = { LocalTime.now() },
    private val todayProvider: () -> LocalDate = { LocalDate.now() },
) : ViewModel() {

    private val selectedDate = MutableStateFlow<LocalDate?>(null)
    private val nowMinute = MutableStateFlow(nowProvider().let { it.hour * 60 + it.minute })

    init {
        // 需求1：优先用上次退出时看的日期，探测不到用系统当日；再夹到航次范围内
        viewModelScope.launch {
            val saved = settingsStore.settings.first().lastViewedDate
            val voyage = voyageRepository.getVoyage()
            val target = saved ?: todayProvider()
            selectedDate.value = voyage?.clampDate(target) ?: target
        }
        // 每分钟刷新"现在"指示线与横幅
        viewModelScope.launch {
            while (isActive) {
                nowMinute.value = nowProvider().let { it.hour * 60 + it.minute }
                kotlinx.coroutines.delay(30_000)
            }
        }
    }

    val state: StateFlow<TimelineUiState?> =
        selectedDate.flatMapLatest { date ->
            if (date == null) {
                kotlinx.coroutines.flow.flowOf(null)
            } else {
                combine(
                    voyageRepository.observeVoyage(),
                    eventRepository.observeByDate(date),
                    categoryRepository.observeAll(),
                    voyageRepository.observeDayPlans(),
                    settingsStore.settings,
                    nowMinute,
                ) { values ->
                    @Suppress("UNCHECKED_CAST")
                    val voyage = values[0] as Voyage?
                    @Suppress("UNCHECKED_CAST")
                    val events = values[1] as List<CruiseEvent>
                    @Suppress("UNCHECKED_CAST")
                    val categories = values[2] as List<Category>
                    @Suppress("UNCHECKED_CAST")
                    val plans = values[3] as Map<LocalDate, DayPlan>
                    val settings = values[4] as AppSettings
                    val now = values[5] as Int

                    TimelineUiState(
                        voyage = voyage,
                        selectedDate = date,
                        events = events,
                        categories = categories.associateBy { it.id },
                        dayPlans = plans,
                        layout = buildTimelineLayout(
                            rangeStartMinute = settings.displayStartMinute,
                            rangeEndMinute = settings.displayEndMinute,
                            hourHeightDp = settings.hourHeightDp,
                            events = events,
                            collapseEmptyGaps = settings.collapseEmptyGaps,
                        ),
                        settings = settings,
                        nowMinute = now,
                        isToday = date == todayProvider(),
                    )
                }
            }
        }.stateIn(viewModelScope, SharingStarted.WhileSubscribed(5_000), null)

    fun selectDate(date: LocalDate) {
        selectedDate.value = date
        viewModelScope.launch { settingsStore.setLastViewedDate(date) }
    }

    fun moveToLane(eventId: Long, lane: Int) {
        viewModelScope.launch { eventRepository.moveToLane(eventId, lane) }
    }

    fun deleteEvent(event: CruiseEvent, scope: RecurrenceScope) {
        viewModelScope.launch { eventRepository.deleteEvent(event, scope) }
    }

    suspend fun conflictsOn(date: LocalDate): List<Pair<CruiseEvent, CruiseEvent>> =
        eventRepository.conflictsOn(date)

    fun finalizeDay(date: LocalDate) {
        viewModelScope.launch {
            voyageRepository.setFinalized(date, true, System.currentTimeMillis())
        }
    }

    fun unlockDay(date: LocalDate) {
        viewModelScope.launch { voyageRepository.setFinalized(date, false, 0L) }
    }
}
```

**注意**：`combine` 的 6 参数重载返回 `Array<*>`，所以上面用索引取值。若嫌丑，可拆成
两层 `combine`（5 个 + 1 个），但索引版能编译就行。

- [ ] **Step 2: 验证编译**

Run: `./gradlew compileDebugKotlin`
Expected: `BUILD SUCCESSFUL`

- [ ] **Step 3: Commit**

```bash
git add app/src/main/java/com/cruise/assistant/ui/timeline/TimelineViewModel.kt
git commit -m "feat: add timeline view model with date restore and now tracking"
```

### Task 7b: TimeRuler、EventCard、DateStrip、NowBanner

- [ ] **Step 1: 写 TimeRuler（需求2 左侧时间列）**

`ui/timeline/TimeRuler.kt`：

```kotlin
package com.cruise.assistant.ui.timeline

import androidx.compose.foundation.Canvas
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.offset
import androidx.compose.foundation.layout.width
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.unit.dp
import com.cruise.assistant.domain.TimelineLayout
import com.cruise.assistant.domain.hourTicks
import com.cruise.assistant.domain.minuteToDp
import com.cruise.assistant.ui.theme.GridLine

val RULER_WIDTH = 56.dp

/** 左侧时间刻度列。文字基线对齐刻度线；折叠段位置标一个 ⋯。 */
@Composable
fun TimeRuler(layout: TimelineLayout, modifier: Modifier = Modifier) {
    Box(modifier.width(RULER_WIDTH).height(layout.totalHeightDp.dp)) {
        layout.hourTicks().forEach { (minute, topDp) ->
            Text(
                text = "%02d:00".format(minute / 60),
                style = MaterialTheme.typography.bodySmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
                modifier = Modifier.offset(x = 8.dp, y = topDp.dp - 8.dp),
            )
        }
        // 折叠掉的空时段标个 ⋯，让用户知道这里被压缩了
        layout.segments.filter { it.collapsed }.forEach { segment ->
            Text(
                text = "⋯",
                style = MaterialTheme.typography.bodySmall,
                color = Color.Gray,
                modifier = Modifier.offset(
                    x = 20.dp,
                    y = (segment.topDp + segment.heightDp / 2f).dp - 8.dp,
                ),
            )
        }
    }
}

/** 事件区域背景：水平整点网格线 + 3 栏竖分隔线（截图1 的观感） */
@Composable
fun TimelineGrid(
    layout: TimelineLayout,
    laneBoundaries: List<Float>,
    modifier: Modifier = Modifier,
) {
    val ticks = layout.hourTicks()
    Canvas(modifier.fillMaxSize()) {
        ticks.forEach { (_, topDp) ->
            val y = topDp * density
            drawLine(
                color = GridLine,
                start = Offset(0f, y),
                end = Offset(size.width, y),
                strokeWidth = 1f,
            )
        }
        laneBoundaries.forEach { fraction ->
            val x = size.width * fraction
            drawLine(
                color = GridLine.copy(alpha = 0.6f),
                start = Offset(x, 0f),
                end = Offset(x, size.height),
                strokeWidth = 1f,
            )
        }
    }
}

```

- [ ] **Step 2: 写 EventCard（需求3 的三行卡片）**

`ui/timeline/EventCard.kt`：

```kotlin
package com.cruise.assistant.ui.timeline

import androidx.compose.foundation.background
import androidx.compose.foundation.combinedClickable
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxHeight
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import com.cruise.assistant.domain.Category
import com.cruise.assistant.domain.CruiseEvent
import com.cruise.assistant.domain.endMinute
import com.cruise.assistant.domain.formatLocation
import com.cruise.assistant.domain.startMinute
import com.cruise.assistant.ui.common.CategoryChip
import com.cruise.assistant.ui.theme.cardBackgroundFor
import com.cruise.assistant.ui.theme.cardTextColorFor

/** 低于这个高度就压缩成单行显示 */
private val COMPACT_THRESHOLD = 44.dp

/**
 * 需求3：事件卡片。
 * 第1行 [类别chip] 事件内容 / 第2行 地点 / 第3行 时间+时长。
 * 左侧一条类别色条（截图1）。
 */
@Composable
fun EventCard(
    event: CruiseEvent,
    category: Category?,
    heightDp: Dp,
    hasConflict: Boolean,
    onClick: () -> Unit,
    onLongClick: () -> Unit,
    modifier: Modifier = Modifier,
) {
    val colorArgb = category?.colorArgb ?: 0xFF9E9E9E.toInt()
    val textColor = cardTextColorFor(colorArgb)
    val compact = heightDp < COMPACT_THRESHOLD
    val location = formatLocation(event.deck, event.zone, event.placeName)

    Row(
        modifier
            .fillMaxWidth()
            .clip(RoundedCornerShape(8.dp))
            .background(cardBackgroundFor(colorArgb))
            .combinedClickable(onClick = onClick, onLongClick = onLongClick),
    ) {
        // 左侧色条
        Box(
            Modifier
                .width(4.dp)
                .fillMaxHeight()
                .background(Color(colorArgb)),
        )
        Column(
            Modifier
                .weight(1f)
                .padding(horizontal = 6.dp, vertical = 3.dp),
        ) {
            if (compact) {
                // 太矮：一行塞下类别、内容、开始时间
                Row(verticalAlignment = Alignment.CenterVertically) {
                    category?.let {
                        CategoryChip(it.name, colorArgb)
                        Spacer(Modifier.width(4.dp))
                    }
                    Text(
                        text = event.title,
                        style = MaterialTheme.typography.bodySmall,
                        fontWeight = FontWeight.Medium,
                        color = textColor,
                        maxLines = 1,
                        overflow = TextOverflow.Ellipsis,
                        modifier = Modifier.weight(1f, fill = false),
                    )
                    Spacer(Modifier.width(4.dp))
                    Text(
                        text = formatStart(event),
                        style = MaterialTheme.typography.labelSmall,
                        color = textColor.copy(alpha = 0.75f),
                        maxLines = 1,
                    )
                }
            } else {
                // 第1行
                Row(verticalAlignment = Alignment.CenterVertically) {
                    category?.let {
                        CategoryChip(it.name, colorArgb)
                        Spacer(Modifier.width(4.dp))
                    }
                    Text(
                        text = event.title,
                        style = MaterialTheme.typography.bodySmall,
                        fontWeight = FontWeight.SemiBold,
                        color = textColor,
                        maxLines = 1,
                        overflow = TextOverflow.Ellipsis,
                        modifier = Modifier.weight(1f),
                    )
                    if (hasConflict) {
                        Text(
                            "!",
                            style = MaterialTheme.typography.labelSmall,
                            fontWeight = FontWeight.Bold,
                            color = MaterialTheme.colorScheme.error,
                        )
                    }
                }
                // 第2行：地点。deck 为 null 时整行不显示（上船/下船）
                location?.let {
                    Text(
                        text = it,
                        style = MaterialTheme.typography.labelSmall,
                        color = textColor.copy(alpha = 0.85f),
                        maxLines = 1,
                        overflow = TextOverflow.Ellipsis,
                    )
                }
                // 第3行：时间 + 时长
                Text(
                    text = formatTimeRange(event),
                    style = MaterialTheme.typography.labelSmall,
                    color = textColor.copy(alpha = 0.7f),
                    maxLines = 1,
                )
            }
        }
    }
}

private fun formatStart(event: CruiseEvent): String =
    "%02d:%02d".format(event.startTime.hour, event.startTime.minute)

/** "13:15 – 13:45  30m" / 超过 1 小时 "1h 30m" */
private fun formatTimeRange(event: CruiseEvent): String {
    val endMin = event.endMinute
    val end = "%02d:%02d".format((endMin / 60) % 24, endMin % 60)
    return "${formatStart(event)} – $end  ${formatDuration(event.durationMinutes)}"
}

fun formatDuration(minutes: Int): String = when {
    minutes < 60 -> "${minutes}m"
    minutes % 60 == 0 -> "${minutes / 60}h"
    else -> "${minutes / 60}h ${minutes % 60}m"
}
```

- [ ] **Step 3: 写 DateStrip 与 NowBanner**

`ui/timeline/DateStrip.kt`：

```kotlin
package com.cruise.assistant.ui.timeline

import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.lazy.rememberLazyListState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import java.time.LocalDate

/** 顶部日期条：实心点 = 已定稿，空心点 = 规划中 */
@Composable
fun DateStrip(
    dates: List<LocalDate>,
    selectedDate: LocalDate,
    finalizedDates: Set<LocalDate>,
    onSelect: (LocalDate) -> Unit,
    modifier: Modifier = Modifier,
) {
    val state = rememberLazyListState()
    val selectedIndex = dates.indexOf(selectedDate)
    LaunchedEffect(selectedIndex) {
        if (selectedIndex >= 0) state.animateScrollToItem(selectedIndex.coerceAtLeast(0))
    }

    LazyRow(
        state = state,
        modifier = modifier.padding(vertical = 4.dp),
        horizontalArrangement = Arrangement.spacedBy(4.dp),
        contentPadding = androidx.compose.foundation.layout.PaddingValues(horizontal = 12.dp),
    ) {
        items(dates) { date ->
            val isSelected = date == selectedDate
            val isFinalized = date in finalizedDates
            val dayIndex = dates.indexOf(date) + 1
            Column(
                horizontalAlignment = Alignment.CenterHorizontally,
                modifier = Modifier
                    .clip(androidx.compose.foundation.shape.RoundedCornerShape(10.dp))
                    .background(
                        if (isSelected) {
                            MaterialTheme.colorScheme.primary.copy(alpha = 0.12f)
                        } else {
                            androidx.compose.ui.graphics.Color.Transparent
                        },
                    )
                    .clickable { onSelect(date) }
                    .padding(horizontal = 10.dp, vertical = 6.dp),
            ) {
                Text(
                    text = "$dayIndex",
                    style = MaterialTheme.typography.bodyMedium,
                    fontWeight = if (isSelected) FontWeight.Bold else FontWeight.Normal,
                    color = if (isSelected) {
                        MaterialTheme.colorScheme.primary
                    } else {
                        MaterialTheme.colorScheme.onSurfaceVariant
                    },
                )
                Box(
                    Modifier
                        .padding(top = 2.dp)
                        .size(6.dp)
                        .clip(CircleShape)
                        .background(
                            if (isFinalized) {
                                MaterialTheme.colorScheme.primary
                            } else {
                                MaterialTheme.colorScheme.outline.copy(alpha = 0.35f)
                            },
                        ),
                )
            }
        }
    }
}
```

`ui/timeline/NowBanner.kt`：

```kotlin
package com.cruise.assistant.ui.timeline

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import com.cruise.assistant.R
import com.cruise.assistant.domain.Category
import com.cruise.assistant.domain.CruiseEvent
import com.cruise.assistant.domain.formatLocation

/** 应用内"进行中 / 下一个"横幅，仅查看当天时显示 */
@Composable
fun NowBanner(
    ongoing: CruiseEvent?,
    next: CruiseEvent?,
    categories: Map<Long, Category>,
    modifier: Modifier = Modifier,
) {
    if (ongoing == null && next == null) return
    Column(
        modifier
            .fillMaxWidth()
            .padding(horizontal = 12.dp, vertical = 4.dp)
            .clip(RoundedCornerShape(10.dp))
            .background(MaterialTheme.colorScheme.primary.copy(alpha = 0.08f))
            .padding(horizontal = 10.dp, vertical = 6.dp),
    ) {
        ongoing?.let { BannerRow(stringResource(R.string.ongoing_label), it, categories) }
        next?.let { BannerRow(stringResource(R.string.next_label), it, categories) }
    }
}

@Composable
private fun BannerRow(label: String, event: CruiseEvent, categories: Map<Long, Category>) {
    val location = formatLocation(event.deck, event.zone, event.placeName)
    Row(Modifier.fillMaxWidth()) {
        Text(
            label,
            style = MaterialTheme.typography.labelSmall,
            fontWeight = FontWeight.Bold,
            color = MaterialTheme.colorScheme.primary,
        )
        Spacer(Modifier.width(6.dp))
        Text(
            buildString {
                append("%02d:%02d".format(event.startTime.hour, event.startTime.minute))
                append("  ")
                append(event.title)
                location?.let { append("  ").append(it) }
            },
            style = MaterialTheme.typography.labelSmall,
            color = MaterialTheme.colorScheme.onSurface,
            maxLines = 1,
            overflow = TextOverflow.Ellipsis,
        )
    }
}
```

- [ ] **Step 4: 验证编译**

Run: `./gradlew compileDebugKotlin`
Expected: `BUILD SUCCESSFUL`

- [ ] **Step 5: Commit**

```bash
git add app/src/main/java/com/cruise/assistant/ui/timeline
git commit -m "feat: add time ruler, three-line event card, date strip, now banner"
```

### Task 7c: LaneCanvas —— 3 栏 / 单栏绝对定位布局

需求4 的关键：**栏0 与栏1 等宽偏左，栏2 明显更窄且贴最右**。
用权重 `1.0 : 1.0 : 0.45`，即三栏宽度占比约 `41% : 41% : 18%`。
定稿后忽略 lane，单栏满宽。

- [ ] **Step 1: 写 LaneCanvas**

`ui/timeline/LaneCanvas.kt`：

```kotlin
package com.cruise.assistant.ui.timeline

import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.BoxWithConstraints
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.offset
import androidx.compose.foundation.layout.width
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import com.cruise.assistant.domain.Category
import com.cruise.assistant.domain.CruiseEvent
import com.cruise.assistant.domain.LANE_COUNT
import com.cruise.assistant.domain.TimelineLayout
import com.cruise.assistant.domain.endMinute
import com.cruise.assistant.domain.minuteToDp
import com.cruise.assistant.domain.startMinute

/** 需求4：栏0/栏1 等宽偏左，栏2 更窄贴最右 */
private val LANE_WEIGHTS = floatArrayOf(1f, 1f, 0.45f)
private val LANE_GAP = 3.dp
private val MIN_CARD_HEIGHT = 22.dp

/** 3 栏竖分隔线的位置（占总宽的比例），供 TimelineGrid 画线 */
fun laneBoundaryFractions(): List<Float> {
    val total = LANE_WEIGHTS.sum()
    var acc = 0f
    return LANE_WEIGHTS.dropLast(1).map { w ->
        acc += w
        acc / total
    }
}

/**
 * 事件区域。planning 模式按 lane 分 3 栏；finalized 模式忽略 lane 单栏满宽。
 */
@Composable
fun LaneCanvas(
    layout: TimelineLayout,
    events: List<CruiseEvent>,
    categories: Map<Long, Category>,
    conflictIds: Set<Long>,
    singleLane: Boolean,
    onEventClick: (CruiseEvent) -> Unit,
    onEventLongClick: (CruiseEvent) -> Unit,
    modifier: Modifier = Modifier,
) {
    BoxWithConstraints(modifier.fillMaxWidth().height(layout.totalHeightDp.dp)) {
        val totalWidth = maxWidth
        val weightSum = LANE_WEIGHTS.sum()

        events.forEach { event ->
            val top = layout.minuteToDp(event.startMinute)
            val bottom = layout.minuteToDp(event.endMinute)
            // 范围外的事件不画（顶部提示条负责告知）
            if (bottom <= 0f || top >= layout.totalHeightDp) return@forEach

            val heightDp = (bottom - top).dp.coerceAtLeast(MIN_CARD_HEIGHT)

            val (offsetX, cardWidth) = if (singleLane) {
                0.dp to totalWidth
            } else {
                val lane = event.lane.coerceIn(0, LANE_COUNT - 1)
                val leftFraction = LANE_WEIGHTS.take(lane).sum() / weightSum
                val widthFraction = LANE_WEIGHTS[lane] / weightSum
                (totalWidth * leftFraction) to (totalWidth * widthFraction - LANE_GAP)
            }

            EventCard(
                event = event,
                category = categories[event.categoryId],
                heightDp = heightDp,
                hasConflict = event.id in conflictIds,
                onClick = { onEventClick(event) },
                onLongClick = { onEventLongClick(event) },
                modifier = Modifier
                    .offset(x = offsetX, y = top.dp)
                    .width(cardWidth)
                    .height(heightDp),
            )
        }
    }
}

/** 红色"现在"指示线 */
@Composable
fun NowIndicator(layout: TimelineLayout, nowMinute: Int, modifier: Modifier = Modifier) {
    if (nowMinute < layout.rangeStartMinute || nowMinute > layout.rangeEndMinute) return
    val y = layout.minuteToDp(nowMinute)
    Box(
        modifier
            .offset(y = y.dp)
            .fillMaxWidth()
            .height(2.dp)
            .background(NowRed),
    )
}
```

本文件的 import 清单（注意 `background` 与 `NowRed`）：

```kotlin
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.BoxWithConstraints
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.offset
import androidx.compose.foundation.layout.width
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import com.cruise.assistant.domain.Category
import com.cruise.assistant.domain.CruiseEvent
import com.cruise.assistant.domain.LANE_COUNT
import com.cruise.assistant.domain.TimelineLayout
import com.cruise.assistant.domain.endMinute
import com.cruise.assistant.domain.minuteToDp
import com.cruise.assistant.domain.startMinute
import com.cruise.assistant.ui.theme.NowRed
```

- [ ] **Step 2: 验证编译**

Run: `./gradlew compileDebugKotlin`
Expected: `BUILD SUCCESSFUL`

- [ ] **Step 3: Commit**

```bash
git add app/src/main/java/com/cruise/assistant/ui/timeline/LaneCanvas.kt
git commit -m "feat: add three-lane event canvas with narrow right lane"
```

### Task 7d: TimelineScreen 组装

- [ ] **Step 1: 写 TimelineScreen**

`ui/timeline/TimelineScreen.kt`：

```kotlin
package com.cruise.assistant.ui.timeline

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.outlined.Schedule
import androidx.compose.material.icons.outlined.Settings
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.DropdownMenu
import androidx.compose.material3.DropdownMenuItem
import androidx.compose.material3.FloatingActionButton
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import com.cruise.assistant.R
import com.cruise.assistant.data.repo.RecurrenceScope
import com.cruise.assistant.domain.CruiseEvent
import com.cruise.assistant.domain.LANE_COUNT
import com.cruise.assistant.domain.minuteToDp
import java.time.format.DateTimeFormatter
import java.time.format.TextStyle
import java.util.Locale

private val HEADER_FMT = DateTimeFormatter.ofPattern("M月d日")

@Composable
fun TimelineScreen(
    state: TimelineUiState,
    onSelectDate: (java.time.LocalDate) -> Unit,
    onOpenSettings: () -> Unit,
    onOpenDisplayRange: () -> Unit,
    onAddEvent: (Int) -> Unit,
    onEditEvent: (CruiseEvent) -> Unit,
    onMoveToLane: (Long, Int) -> Unit,
    onDeleteEvent: (CruiseEvent, RecurrenceScope) -> Unit,
    onGeneratePlan: () -> Unit,
    onUnlockDay: () -> Unit,
    conflictIds: Set<Long>,
) {
    val scrollState = rememberScrollState()
    var menuEvent by remember { mutableStateOf<CruiseEvent?>(null) }
    var showFullDay by remember { mutableStateOf(false) }

    // 打开当天时自动滚到"现在"附近
    LaunchedEffect(state.selectedDate, state.isToday) {
        if (state.isToday) {
            val y = state.layout.minuteToDp(state.nowMinute)
            scrollState.animateScrollTo((y - 200f).coerceAtLeast(0f).toInt())
        }
    }

    Scaffold(
        floatingActionButton = {
            if (!state.isFinalized) {
                FloatingActionButton(onClick = { onAddEvent(defaultStartMinute(state)) }) {
                    Icon(Icons.Filled.Add, contentDescription = stringResource(R.string.add_event))
                }
            }
        },
    ) { padding ->
        Column(Modifier.padding(padding).fillMaxSize()) {
            // ---- 顶栏 ----
            Row(
                Modifier.fillMaxWidth().padding(horizontal = 12.dp, vertical = 6.dp),
                verticalAlignment = Alignment.CenterVertically,
            ) {
                Column(Modifier.weight(1f)) {
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        Text(
                            state.selectedDate.format(HEADER_FMT),
                            style = MaterialTheme.typography.titleLarge,
                        )
                        Spacer(Modifier.width(6.dp))
                        Text(
                            state.selectedDate.dayOfWeek
                                .getDisplayName(TextStyle.SHORT, Locale.CHINA),
                            style = MaterialTheme.typography.bodyMedium,
                            color = MaterialTheme.colorScheme.onSurfaceVariant,
                        )
                    }
                    state.voyage?.dayIndexOf(state.selectedDate)?.let { index ->
                        Text(
                            stringResource(R.string.day_index_format, index, state.voyage.dayCount),
                            style = MaterialTheme.typography.bodySmall,
                            color = MaterialTheme.colorScheme.onSurfaceVariant,
                        )
                    }
                }
                IconButton(onClick = onOpenDisplayRange) {
                    Icon(Icons.Outlined.Schedule, contentDescription = stringResource(R.string.display_range_title))
                }
                IconButton(onClick = onOpenSettings) {
                    Icon(Icons.Outlined.Settings, contentDescription = stringResource(R.string.settings))
                }
            }

            // ---- 日期条 ----
            state.voyage?.let { voyage ->
                DateStrip(
                    dates = voyage.datesInVoyage(),
                    selectedDate = state.selectedDate,
                    finalizedDates = state.dayPlans.filterValues { it.finalized }.keys,
                    onSelect = onSelectDate,
                )
            }

            // ---- 定稿状态条 ----
            Row(
                Modifier.fillMaxWidth().padding(horizontal = 12.dp, vertical = 2.dp),
                verticalAlignment = Alignment.CenterVertically,
            ) {
                if (state.isFinalized) {
                    Text(
                        stringResource(R.string.finalized_badge),
                        style = MaterialTheme.typography.labelSmall,
                        fontWeight = FontWeight.Bold,
                        color = MaterialTheme.colorScheme.primary,
                        modifier = Modifier.weight(1f),
                    )
                    TextButton(onClick = onUnlockDay) { Text(stringResource(R.string.unlock)) }
                } else {
                    Spacer(Modifier.weight(1f))
                    TextButton(onClick = onGeneratePlan) {
                        Text(stringResource(R.string.generate_plan))
                    }
                }
            }

            // ---- 当天横幅 ----
            if (state.isToday) {
                NowBanner(state.ongoing, state.next, state.categories)
            }

            HorizontalDivider()

            // ---- 范围外提示 ----
            if (state.layout.hiddenBefore > 0) {
                HiddenHint(
                    text = stringResource(R.string.hidden_before_format, state.layout.hiddenBefore),
                    onClick = { showFullDay = true },
                )
            }

            // ---- 时间轴主体 ----
            if (state.events.isEmpty()) {
                Box(Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
                    Text(
                        stringResource(R.string.empty_day),
                        style = MaterialTheme.typography.bodyMedium,
                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                    )
                }
            } else {
                Box(Modifier.weight(1f).verticalScroll(scrollState)) {
                    Row(Modifier.fillMaxWidth().height(state.layout.totalHeightDp.dp)) {
                        TimeRuler(state.layout)
                        Box(Modifier.weight(1f)) {
                            TimelineGrid(
                                layout = state.layout,
                                laneBoundaries = if (state.isFinalized) {
                                    emptyList()
                                } else {
                                    laneBoundaryFractions()
                                },
                                modifier = Modifier.fillMaxSize(),
                            )
                            LaneCanvas(
                                layout = state.layout,
                                events = state.events,
                                categories = state.categories,
                                conflictIds = conflictIds,
                                singleLane = state.isFinalized,
                                onEventClick = onEditEvent,
                                onEventLongClick = { menuEvent = it },
                                modifier = Modifier.padding(end = 8.dp),
                            )
                            if (state.isToday) {
                                NowIndicator(state.layout, state.nowMinute)
                            }
                        }
                    }
                }
            }

            if (state.layout.hiddenAfter > 0) {
                HiddenHint(
                    text = stringResource(R.string.hidden_after_format, state.layout.hiddenAfter),
                    onClick = { showFullDay = true },
                )
            }
        }
    }

    // ---- 长按菜单：换栏 / 编辑 / 删除 ----
    menuEvent?.let { event ->
        DropdownMenu(expanded = true, onDismissRequest = { menuEvent = null }) {
            if (!state.isFinalized) {
                (0 until LANE_COUNT).filter { it != event.lane }.forEach { lane ->
                    DropdownMenuItem(
                        text = { Text(stringResource(R.string.move_to_lane_format, lane + 1)) },
                        onClick = {
                            onMoveToLane(event.id, lane)
                            menuEvent = null
                        },
                    )
                }
            }
            DropdownMenuItem(
                text = { Text(stringResource(R.string.edit)) },
                onClick = {
                    onEditEvent(event)
                    menuEvent = null
                },
            )
            if (!event.isSystemGenerated && !state.isFinalized) {
                DropdownMenuItem(
                    text = { Text(stringResource(R.string.delete)) },
                    onClick = {
                        val scope = if (event.recurrenceGroupId != null) {
                            RecurrenceScope.THIS_DAY_ONLY
                        } else {
                            RecurrenceScope.THIS_DAY_ONLY
                        }
                        onDeleteEvent(event, scope)
                        menuEvent = null
                    },
                )
            }
        }
    }
}

@Composable
private fun HiddenHint(text: String, onClick: () -> Unit) {
    Row(
        Modifier
            .fillMaxWidth()
            .background(MaterialTheme.colorScheme.secondaryContainer.copy(alpha = 0.4f))
            .padding(horizontal = 12.dp, vertical = 4.dp),
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.SpaceBetween,
    ) {
        Text(text, style = MaterialTheme.typography.labelSmall)
        TextButton(onClick = onClick) { Text(stringResource(R.string.show_full_day)) }
    }
}

/** FAB 默认开始时间：当天用下一个整刻，其他天用显示范围起点 */
private fun defaultStartMinute(state: TimelineUiState): Int =
    if (state.isToday) {
        ((state.nowMinute / 15) + 1) * 15
    } else {
        state.layout.rangeStartMinute
    }.coerceIn(0, 1439)

@Composable
fun TimelineLoading() {
    Box(Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
        CircularProgressIndicator()
    }
}
```

**注意**：`showFullDay` 目前只是记下用户点击，实际"临时展开全天"在 Task 10 与
显示范围面板一起接通（把 override range 传给 ViewModel）。本 task 先让提示条能点、
不崩溃即可；Task 10 的 Step 会补上真正的展开行为，届时删掉这个未用变量的警告。

- [ ] **Step 2: 接入 NavHost**

修改 `ui/CruiseNavHost.kt` 的 `composable(Routes.TIMELINE)` 块：

```kotlin
        composable(Routes.TIMELINE) {
            val vm: TimelineViewModel = viewModel(
                factory = viewModelFactory {
                    TimelineViewModel(
                        container.voyageRepository,
                        container.eventRepository,
                        container.categoryRepository,
                        container.settingsStore,
                    )
                },
            )
            val timelineState by vm.state.collectAsState()
            val current = timelineState
            if (current == null) {
                TimelineLoading()
            } else {
                TimelineScreen(
                    state = current,
                    onSelectDate = vm::selectDate,
                    onOpenSettings = { navController.navigate(Routes.SETTINGS) },
                    onOpenDisplayRange = { /* Task 10 */ },
                    onAddEvent = { /* Task 8 */ },
                    onEditEvent = { /* Task 8 */ },
                    onMoveToLane = vm::moveToLane,
                    onDeleteEvent = vm::deleteEvent,
                    onGeneratePlan = { /* Task 11 */ },
                    onUnlockDay = { vm.unlockDay(current.selectedDate) },
                    conflictIds = emptySet(),
                )
            }
        }
```

补 import：`com.cruise.assistant.ui.timeline.TimelineScreen`、`TimelineViewModel`、`TimelineLoading`。

- [ ] **Step 3: 构建并在模拟器上看效果**

```bash
./gradlew installDebug
ADB="/c/Users/Administrator/AppData/Local/Android/Sdk/platform-tools/adb.exe"
"$ADB" shell am force-stop com.cruise.assistant.debug
"$ADB" shell am start -n com.cruise.assistant.debug/com.cruise.assistant.MainActivity
sleep 3
"$ADB" exec-out screencap -p > /tmp/timeline.png
```

用 Read 工具打开 `/tmp/timeline.png`。Expected: 左侧 07:00–24:00 时间刻度、
日期条上 6 个圆点、Day1 的 12:00 有一张"上船 15m"卡片、右下角 FAB。

- [ ] **Step 4: 用 adb 手动插几条重叠事件，验证 3 栏错开**

```bash
ADB="/c/Users/Administrator/AppData/Local/Android/Sdk/platform-tools/adb.exe"
DB=/data/data/com.cruise.assistant.debug/databases/cruise.db
"$ADB" shell "run-as com.cruise.assistant.debug sqlite3 $DB \"
INSERT INTO event (date,startTime,durationMinutes,categoryId,title,deck,zone,placeName,lane,recurrenceGroupId,reminderMinutesBefore,isSystemGenerated)
VALUES
 ('2026-09-03','13:15',30,2,'扬帆起航',9,'STERN','剧场',0,NULL,NULL,0),
 ('2026-09-03','13:20',60,5,'游泳',11,'MID','泳池',1,NULL,NULL,0),
 ('2026-09-03','13:30',20,4,'安全演习',NULL,NULL,NULL,2,NULL,NULL,0);
\""
```

把 `2026-09-03` 换成实际的 Day1 日期，`categoryId` 换成实际 id（先
`SELECT id,name FROM category;` 看一眼）。然后重启 App 截图。

Expected: 三张卡片并排，前两张等宽偏左、第三张明显更窄且贴最右，互不遮挡。

- [ ] **Step 5: Commit**

```bash
git add app/src/main/java/com/cruise/assistant/ui
git commit -m "feat: assemble timeline screen with three-lane layout and now indicator"
```

## Task 8: 添加/编辑事件弹窗（需求6，基于截图2）

> **Task 7 已落地的两处偏离，Task 8 必须知道**（以仓库中实际代码为准，不要按本文件旧片段回改）：
> 1. `EventCard` 多了一个 `narrow: Boolean = false` 参数。compact 分支里：非窄栏渲染
>    `[标题 + 开始时间]`，**窄栏（最后一栏）只渲染标题**——栏2 只有约 18% 宽，挤上时间会把
>    标题压到只剩一两个字；卡片在刻度线上的竖直位置本身已经表达了开始时间。
>    `narrow` 由 `LANE_COUNT - 1` 推导，不要写死 `2`。
> 2. `TimelineScreen` 的 `onEventClick` 与 `onEventLongClick` 都包了 `if (!state.isFinalized)`，
>    已定稿的那天点卡片和长按都不响应（只读）。Task 8 把真正的事件弹窗接到 `onEditEvent` 时
>    **不要绕过这个判断**，否则已定稿的日子会重新变成可编辑。
>

在截图2 的基础上加两块：**地点选择**、**每日同一时间循环开关**。

**Files:**
- Create: `ui/event/EventSheetViewModel.kt`, `ui/event/EventSheet.kt`, `ui/event/LocationPicker.kt`
- Modify: `ui/CruiseNavHost.kt`

**Interfaces:**
- Consumes: `EventRepository.addEvent/updateEvent`、`CategoryRepository.observeActive`、
  `LocationPresetRepository`、`VoyageRepository.getVoyage`、Task 5 的 `TimeWheel` / `iconFor`
- Produces:
  - `data class EventSheetState(...)`
  - `class EventSheetViewModel(...)` 暴露 `state: StateFlow<EventSheetState>` 与
    `onCategorySelect` / `onTitleChange` / `onDeckChange` / `onZoneChange` / `onPlaceNameChange` /
    `onNoLocationToggle` / `onStartChange` / `onEndChange` / `onRecurringToggle` / `save(onDone)`
  - `@Composable fun EventSheet(state, callbacks..., onDismiss)`
  - `@Composable fun LocationPicker(maxDeck, deck, zone, placeName, presets, noLocation, on*Change)`

- [ ] **Step 1: 写 LocationPicker（需求3 的地点选择器）**

`ui/event/LocationPicker.kt`：

```kotlin
package com.cruise.assistant.ui.event

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.width
import androidx.compose.material3.Checkbox
import androidx.compose.material3.DropdownMenu
import androidx.compose.material3.DropdownMenuItem
import androidx.compose.material3.FilterChip
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.unit.dp
import com.cruise.assistant.R
import com.cruise.assistant.domain.LocationPreset
import com.cruise.assistant.domain.ShipZone

/**
 * 需求3：楼层下拉 + 船头/船中/船尾 + 地点名（可输入，也可从历史里选）。
 * 勾"不设地点"时整块禁用，对应 deck=null。
 */
@Composable
fun LocationPicker(
    maxDeck: Int,
    deck: Int?,
    zone: ShipZone?,
    placeName: String,
    presets: List<LocationPreset>,
    noLocation: Boolean,
    onDeckChange: (Int) -> Unit,
    onZoneChange: (ShipZone) -> Unit,
    onPlaceNameChange: (String) -> Unit,
    onNoLocationToggle: (Boolean) -> Unit,
    modifier: Modifier = Modifier,
) {
    var deckMenuOpen by remember { mutableStateOf(false) }
    var presetMenuOpen by remember { mutableStateOf(false) }
    val enabled = !noLocation

    // 同层同区的历史地点排前面
    val suggestions = remember(presets, deck, zone) {
        presets.sortedByDescending { p ->
            (if (p.deck == deck) 2 else 0) + (if (p.zone == zone) 1 else 0)
        }.take(12)
    }

    Column(modifier) {
        Text(stringResource(R.string.location_label), style = MaterialTheme.typography.titleMedium)
        Spacer(Modifier.height(8.dp))

        Row(verticalAlignment = Alignment.CenterVertically) {
            // 楼层下拉
            Box {
                OutlinedButton(
                    onClick = { deckMenuOpen = true },
                    enabled = enabled,
                    modifier = Modifier.width(88.dp),
                ) { Text(if (deck != null) "${deck}F" else stringResource(R.string.deck_label)) }
                DropdownMenu(
                    expanded = deckMenuOpen,
                    onDismissRequest = { deckMenuOpen = false },
                    modifier = Modifier.heightIn(max = 280.dp),
                ) {
                    (1..maxDeck).forEach { floor ->
                        DropdownMenuItem(
                            text = { Text("${floor}F") },
                            onClick = {
                                onDeckChange(floor)
                                deckMenuOpen = false
                            },
                        )
                    }
                }
            }
            Spacer(Modifier.width(12.dp))
            // 区域三选一
            Row(horizontalArrangement = Arrangement.spacedBy(6.dp)) {
                ShipZone.entries.forEach { z ->
                    FilterChip(
                        selected = zone == z,
                        onClick = { onZoneChange(z) },
                        enabled = enabled,
                        label = { Text(z.label) },
                    )
                }
            }
        }

        Spacer(Modifier.height(8.dp))
        Box {
            OutlinedTextField(
                value = placeName,
                onValueChange = onPlaceNameChange,
                label = { Text(stringResource(R.string.place_name_hint)) },
                enabled = enabled,
                singleLine = true,
                trailingIcon = {
                    if (suggestions.isNotEmpty()) {
                        androidx.compose.material3.IconButton(
                            onClick = { presetMenuOpen = true },
                            enabled = enabled,
                        ) {
                            androidx.compose.material3.Icon(
                                androidx.compose.material.icons.Icons.Filled.ArrowDropDown,
                                contentDescription = null,
                            )
                        }
                    }
                },
                modifier = Modifier.fillMaxWidth(),
            )
            DropdownMenu(
                expanded = presetMenuOpen,
                onDismissRequest = { presetMenuOpen = false },
                modifier = Modifier.heightIn(max = 280.dp),
            ) {
                suggestions.forEach { preset ->
                    DropdownMenuItem(
                        text = { Text("${preset.deck}F${preset.zone.label}${preset.placeName}") },
                        onClick = {
                            onDeckChange(preset.deck)
                            onZoneChange(preset.zone)
                            onPlaceNameChange(preset.placeName)
                            presetMenuOpen = false
                        },
                    )
                }
            }
        }

        Spacer(Modifier.height(4.dp))
        Row(verticalAlignment = Alignment.CenterVertically) {
            Checkbox(checked = noLocation, onCheckedChange = onNoLocationToggle)
            Text(stringResource(R.string.no_location), style = MaterialTheme.typography.bodySmall)
        }
    }
}
```

`Spacer(Modifier.height(...))` 用标准 import，不要自己写扩展：

```kotlin
import androidx.compose.foundation.layout.height
```

（连同上面 import 块里已列出的 `Arrangement` / `Box` / `Column` / `Row` / `Spacer` /
`fillMaxWidth` / `heightIn` / `padding` / `width`）

- [ ] **Step 2: 写 EventSheetViewModel**

`ui/event/EventSheetViewModel.kt`：

```kotlin
package com.cruise.assistant.ui.event

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.cruise.assistant.R
import com.cruise.assistant.data.repo.CategoryRepository
import com.cruise.assistant.data.repo.EventRepository
import com.cruise.assistant.data.repo.LocationPresetRepository
import com.cruise.assistant.data.repo.RecurrenceScope
import com.cruise.assistant.data.repo.VoyageRepository
import com.cruise.assistant.domain.Category
import com.cruise.assistant.domain.CruiseEvent
import com.cruise.assistant.domain.LocationPreset
import com.cruise.assistant.domain.ShipZone
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch
import java.time.LocalDate
import java.time.LocalTime

data class EventSheetState(
    val editingId: Long = 0,
    val date: LocalDate,
    val categories: List<Category> = emptyList(),
    val selectedCategoryId: Long? = null,
    val title: String = "",
    val deck: Int? = null,
    val zone: ShipZone? = ShipZone.MID,
    val placeName: String = "",
    val noLocation: Boolean = true,
    val startTime: LocalTime,
    val endTime: LocalTime,
    val recurring: Boolean = false,
    val presets: List<LocationPreset> = emptyList(),
    val maxDeck: Int = 20,
    val disembarkDate: LocalDate? = null,
    val isSystemEvent: Boolean = false,
    val isRecurrenceMember: Boolean = false,
    val errorResId: Int? = null,
    val saving: Boolean = false,
) {
    val isEditing: Boolean get() = editingId != 0L
    /** 循环终止日 = 下船日前一天（需求6 澄清：下船日不含） */
    val recurUntil: LocalDate? get() = disembarkDate?.minusDays(1)
}

class EventSheetViewModel(
    private val eventRepository: EventRepository,
    private val categoryRepository: CategoryRepository,
    private val locationPresetRepository: LocationPresetRepository,
    private val voyageRepository: VoyageRepository,
    date: LocalDate,
    defaultStartMinute: Int,
    private val editingEventId: Long,
) : ViewModel() {

    private val _state = MutableStateFlow(
        EventSheetState(
            date = date,
            startTime = LocalTime.of(defaultStartMinute / 60, defaultStartMinute % 60),
            endTime = LocalTime.of(
                ((defaultStartMinute + 30) / 60).coerceAtMost(23),
                (defaultStartMinute + 30) % 60,
            ),
        ),
    )
    val state: StateFlow<EventSheetState> = _state.asStateFlow()

    init {
        viewModelScope.launch {
            val voyage = voyageRepository.getVoyage()
            _state.update {
                it.copy(maxDeck = voyage?.maxDeck ?: 20, disembarkDate = voyage?.disembarkDate)
            }
        }
        viewModelScope.launch {
            categoryRepository.observeActive().collect { list ->
                _state.update { current ->
                    current.copy(
                        categories = list,
                        selectedCategoryId = current.selectedCategoryId
                            ?: list.firstOrNull()?.id,
                    )
                }
            }
        }
        viewModelScope.launch {
            locationPresetRepository.observeAll().collect { presets ->
                _state.update { it.copy(presets = presets) }
            }
        }
        if (editingEventId != 0L) {
            viewModelScope.launch {
                eventRepository.getById(editingEventId)?.let { event ->
                    val endMinutes = event.startTime.hour * 60 + event.startTime.minute +
                        event.durationMinutes
                    _state.update {
                        it.copy(
                            editingId = event.id,
                            date = event.date,
                            selectedCategoryId = event.categoryId,
                            title = event.title,
                            deck = event.deck,
                            zone = event.zone ?: ShipZone.MID,
                            placeName = event.placeName.orEmpty(),
                            noLocation = event.deck == null,
                            startTime = event.startTime,
                            endTime = LocalTime.of(
                                (endMinutes / 60).coerceAtMost(23),
                                endMinutes % 60,
                            ),
                            isSystemEvent = event.isSystemGenerated,
                            isRecurrenceMember = event.recurrenceGroupId != null,
                        )
                    }
                }
            }
        }
    }

    fun onCategorySelect(id: Long) = _state.update { it.copy(selectedCategoryId = id, errorResId = null) }
    fun onTitleChange(value: String) = _state.update { it.copy(title = value, errorResId = null) }
    fun onDeckChange(value: Int) = _state.update { it.copy(deck = value, noLocation = false) }
    fun onZoneChange(value: ShipZone) = _state.update { it.copy(zone = value, noLocation = false) }
    fun onPlaceNameChange(value: String) = _state.update { it.copy(placeName = value) }
    fun onRecurringToggle(value: Boolean) = _state.update { it.copy(recurring = value) }

    fun onNoLocationToggle(value: Boolean) = _state.update {
        if (value) it.copy(noLocation = true, deck = null) else it.copy(noLocation = false)
    }

    fun onStartChange(value: LocalTime) = _state.update { current ->
        // 开始时间往后越过结束时间时，结束时间跟着推 30 分钟
        val end = if (!current.endTime.isAfter(value)) {
            value.plusMinutes(30).takeIf { it.isAfter(value) } ?: LocalTime.of(23, 59)
        } else {
            current.endTime
        }
        current.copy(startTime = value, endTime = end, errorResId = null)
    }

    fun onEndChange(value: LocalTime) = _state.update { it.copy(endTime = value, errorResId = null) }

    /** scope 仅在编辑循环组成员时生效 */
    fun save(scope: RecurrenceScope, onDone: () -> Unit) {
        val s = _state.value
        val categoryId = s.selectedCategoryId
        if (categoryId == null) {
            _state.update { it.copy(errorResId = R.string.error_no_category) }
            return
        }
        if (s.title.isBlank()) {
            _state.update { it.copy(errorResId = R.string.error_title_required) }
            return
        }
        val startMin = s.startTime.hour * 60 + s.startTime.minute
        val endMin = s.endTime.hour * 60 + s.endTime.minute
        if (endMin <= startMin) {
            _state.update { it.copy(errorResId = R.string.error_end_before_start) }
            return
        }

        val event = CruiseEvent(
            id = s.editingId,
            date = s.date,
            startTime = s.startTime,
            durationMinutes = endMin - startMin,
            categoryId = categoryId,
            title = s.title.trim(),
            deck = if (s.noLocation) null else s.deck,
            zone = if (s.noLocation) null else s.zone,
            placeName = if (s.noLocation) null else s.placeName.trim().takeIf { it.isNotEmpty() },
            isSystemGenerated = s.isSystemEvent,
        )

        _state.update { it.copy(saving = true) }
        viewModelScope.launch {
            if (s.isEditing) {
                eventRepository.updateEvent(event, scope)
            } else {
                eventRepository.addEvent(event, recurring = s.recurring)
            }
            // 记住地点，下次可直接选
            val deck = event.deck
            val zone = event.zone
            val place = event.placeName
            if (deck != null && zone != null && place != null) {
                locationPresetRepository.remember(deck, zone, place, System.currentTimeMillis())
            }
            _state.update { it.copy(saving = false) }
            onDone()
        }
    }
}
```

- [ ] **Step 3: 写 EventSheet**

`ui/event/EventSheet.kt`：

```kotlin
@file:OptIn(androidx.compose.material3.ExperimentalMaterial3Api::class)

package com.cruise.assistant.ui.event

import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.grid.GridCells
import androidx.compose.foundation.lazy.grid.LazyVerticalGrid
import androidx.compose.foundation.lazy.grid.items
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Add
import androidx.compose.material3.Button
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.ModalBottomSheet
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Switch
import androidx.compose.material3.Text
import androidx.compose.material3.rememberModalBottomSheetState
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import com.cruise.assistant.R
import com.cruise.assistant.domain.ShipZone
import com.cruise.assistant.ui.common.TimeWheel
import com.cruise.assistant.ui.common.iconFor
import java.time.LocalTime
import java.time.format.DateTimeFormatter

private val RECUR_FMT = DateTimeFormatter.ofPattern("M月d日")

@Composable
fun EventSheet(
    state: EventSheetState,
    onCategorySelect: (Long) -> Unit,
    onManageCategories: () -> Unit,
    onTitleChange: (String) -> Unit,
    onDeckChange: (Int) -> Unit,
    onZoneChange: (ShipZone) -> Unit,
    onPlaceNameChange: (String) -> Unit,
    onNoLocationToggle: (Boolean) -> Unit,
    onStartChange: (LocalTime) -> Unit,
    onEndChange: (LocalTime) -> Unit,
    onRecurringToggle: (Boolean) -> Unit,
    onSave: () -> Unit,
    onDismiss: () -> Unit,
) {
    val sheetState = rememberModalBottomSheetState(skipPartiallyExpanded = true)

    ModalBottomSheet(onDismissRequest = onDismiss, sheetState = sheetState) {
        Column(
            Modifier
                .verticalScroll(rememberScrollState())
                .padding(horizontal = 20.dp)
                .padding(bottom = 24.dp),
        ) {
            Text(
                stringResource(
                    if (state.isEditing) R.string.edit_event else R.string.add_event,
                ),
                style = MaterialTheme.typography.titleLarge,
            )
            Spacer(Modifier.height(16.dp))

            // ---- 选择事项（截图2 的图标网格）----
            Text(stringResource(R.string.select_category), style = MaterialTheme.typography.titleMedium)
            Spacer(Modifier.height(8.dp))
            LazyVerticalGrid(
                columns = GridCells.Fixed(5),
                modifier = Modifier.height(if (state.categories.size > 5) 180.dp else 96.dp),
                horizontalArrangement = Arrangement.spacedBy(8.dp),
                verticalArrangement = Arrangement.spacedBy(8.dp),
            ) {
                items(state.categories) { category ->
                    val selected = category.id == state.selectedCategoryId
                    Column(horizontalAlignment = Alignment.CenterHorizontally) {
                        Box(
                            Modifier
                                .size(52.dp)
                                .clip(RoundedCornerShape(14.dp))
                                .background(
                                    if (selected) {
                                        Color(category.colorArgb)
                                    } else {
                                        MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.5f)
                                    },
                                )
                                .clickable { onCategorySelect(category.id) },
                            contentAlignment = Alignment.Center,
                        ) {
                            Icon(
                                iconFor(category.iconKey),
                                contentDescription = category.name,
                                tint = if (selected) Color.White else MaterialTheme.colorScheme.onSurfaceVariant,
                            )
                        }
                        Text(
                            category.name,
                            style = MaterialTheme.typography.labelSmall,
                            maxLines = 1,
                            overflow = TextOverflow.Ellipsis,
                            textAlign = TextAlign.Center,
                        )
                    }
                }
                // 末位"编辑"入口 → 类别弹窗
                item {
                    Column(horizontalAlignment = Alignment.CenterHorizontally) {
                        Box(
                            Modifier
                                .size(52.dp)
                                .clip(RoundedCornerShape(14.dp))
                                .background(MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.5f))
                                .clickable(onClick = onManageCategories),
                            contentAlignment = Alignment.Center,
                        ) { Icon(Icons.Filled.Add, contentDescription = null) }
                        Text(
                            stringResource(R.string.manage_categories),
                            style = MaterialTheme.typography.labelSmall,
                        )
                    }
                }
            }

            Spacer(Modifier.height(16.dp))
            OutlinedTextField(
                value = state.title,
                onValueChange = onTitleChange,
                label = { Text(stringResource(R.string.event_title_hint)) },
                singleLine = true,
                modifier = Modifier.fillMaxWidth(),
            )

            Spacer(Modifier.height(16.dp))
            HorizontalDivider()
            Spacer(Modifier.height(16.dp))

            // ---- 地点（新增）----
            LocationPicker(
                maxDeck = state.maxDeck,
                deck = state.deck,
                zone = state.zone,
                placeName = state.placeName,
                presets = state.presets,
                noLocation = state.noLocation,
                onDeckChange = onDeckChange,
                onZoneChange = onZoneChange,
                onPlaceNameChange = onPlaceNameChange,
                onNoLocationToggle = onNoLocationToggle,
            )

            Spacer(Modifier.height(16.dp))
            HorizontalDivider()
            Spacer(Modifier.height(16.dp))

            // ---- 开始/结束时间（截图2 的双滚轮）----
            Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(12.dp)) {
                Column(Modifier.weight(1f)) {
                    Text(stringResource(R.string.start_time), style = MaterialTheme.typography.titleMedium)
                    Spacer(Modifier.height(6.dp))
                    TimeWheel(state.startTime, onStartChange, Modifier.fillMaxWidth())
                }
                Column(Modifier.weight(1f)) {
                    Text(stringResource(R.string.end_time), style = MaterialTheme.typography.titleMedium)
                    Spacer(Modifier.height(6.dp))
                    TimeWheel(state.endTime, onEndChange, Modifier.fillMaxWidth())
                }
            }

            // ---- 每日循环开关（新增，需求6）----
            if (!state.isEditing) {
                Spacer(Modifier.height(16.dp))
                HorizontalDivider()
                Spacer(Modifier.height(12.dp))
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Column(Modifier.weight(1f)) {
                        Text(
                            stringResource(R.string.recur_daily),
                            style = MaterialTheme.typography.titleMedium,
                        )
                        state.recurUntil?.let { until ->
                            if (state.recurring) {
                                Text(
                                    stringResource(R.string.recur_until_format, until.format(RECUR_FMT)),
                                    style = MaterialTheme.typography.bodySmall,
                                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                                )
                            }
                        }
                    }
                    Switch(checked = state.recurring, onCheckedChange = onRecurringToggle)
                }
            }

            state.errorResId?.let { resId ->
                Spacer(Modifier.height(12.dp))
                Text(
                    stringResource(resId),
                    color = MaterialTheme.colorScheme.error,
                    style = MaterialTheme.typography.bodySmall,
                )
            }

            Spacer(Modifier.height(20.dp))
            Button(
                onClick = onSave,
                enabled = !state.saving,
                modifier = Modifier.fillMaxWidth().height(52.dp),
            ) { Text(stringResource(R.string.confirm)) }
        }
    }
}
```

- [ ] **Step 4: 接入 NavHost**

在 `ui/CruiseNavHost.kt` 的 `composable(Routes.TIMELINE)` 块里，用局部状态控制弹窗：

```kotlin
            var sheetRequest by remember {
                mutableStateOf<EventSheetRequest?>(null)
            }
            // ...TimelineScreen(
            //     onAddEvent = { startMinute ->
            //         sheetRequest = EventSheetRequest(current.selectedDate, startMinute, 0L)
            //     },
            //     onEditEvent = { event ->
            //         sheetRequest = EventSheetRequest(event.date, 0, event.id)
            //     },
            // ...)

            sheetRequest?.let { request ->
                val sheetVm: EventSheetViewModel = viewModel(
                    key = "event-${request.date}-${request.editingId}-${request.startMinute}",
                    factory = viewModelFactory {
                        EventSheetViewModel(
                            container.eventRepository,
                            container.categoryRepository,
                            container.locationPresetRepository,
                            container.voyageRepository,
                            request.date,
                            request.startMinute,
                            request.editingId,
                        )
                    },
                )
                val sheetState by sheetVm.state.collectAsState()
                var showCategorySheet by remember { mutableStateOf(false) }

                EventSheet(
                    state = sheetState,
                    onCategorySelect = sheetVm::onCategorySelect,
                    onManageCategories = { showCategorySheet = true },
                    onTitleChange = sheetVm::onTitleChange,
                    onDeckChange = sheetVm::onDeckChange,
                    onZoneChange = sheetVm::onZoneChange,
                    onPlaceNameChange = sheetVm::onPlaceNameChange,
                    onNoLocationToggle = sheetVm::onNoLocationToggle,
                    onStartChange = sheetVm::onStartChange,
                    onEndChange = sheetVm::onEndChange,
                    onRecurringToggle = sheetVm::onRecurringToggle,
                    onSave = {
                        sheetVm.save(RecurrenceScope.THIS_DAY_ONLY) { sheetRequest = null }
                    },
                    onDismiss = { sheetRequest = null },
                )
                // Task 9 在这里挂 CategorySheet
            }
```

在 `ui/CruiseNavHost.kt` 文件底部加：

```kotlin
data class EventSheetRequest(
    val date: java.time.LocalDate,
    val startMinute: Int,
    val editingId: Long,
)
```

补 import：`androidx.compose.runtime.mutableStateOf` / `remember` / `setPropertyValue` 相关、
`com.cruise.assistant.ui.event.*`、`com.cruise.assistant.data.repo.RecurrenceScope`。

**编辑循环事件的 scope 选择**：当 `sheetState.isRecurrenceMember == true` 时，
`onSave` 先弹一个 `AlertDialog` 让用户选"仅本日"/"所有重复项"，再带对应 scope 调用
`sheetVm.save(...)`。实现：

```kotlin
                var pendingScopeChoice by remember { mutableStateOf(false) }
                // onSave = { if (sheetState.isRecurrenceMember) pendingScopeChoice = true
                //            else sheetVm.save(RecurrenceScope.THIS_DAY_ONLY) { sheetRequest = null } }

                if (pendingScopeChoice) {
                    androidx.compose.material3.AlertDialog(
                        onDismissRequest = { pendingScopeChoice = false },
                        title = { androidx.compose.material3.Text(stringResource(R.string.recur_scope_title)) },
                        confirmButton = {
                            androidx.compose.material3.TextButton(onClick = {
                                pendingScopeChoice = false
                                sheetVm.save(RecurrenceScope.WHOLE_GROUP) { sheetRequest = null }
                            }) { androidx.compose.material3.Text(stringResource(R.string.recur_scope_whole_group)) }
                        },
                        dismissButton = {
                            androidx.compose.material3.TextButton(onClick = {
                                pendingScopeChoice = false
                                sheetVm.save(RecurrenceScope.THIS_DAY_ONLY) { sheetRequest = null }
                            }) { androidx.compose.material3.Text(stringResource(R.string.recur_scope_this_day)) }
                        },
                    )
                }
```

- [ ] **Step 5: 在模拟器上验证添加事件全流程**

```bash
./gradlew installDebug
ADB="/c/Users/Administrator/AppData/Local/Android/Sdk/platform-tools/adb.exe"
"$ADB" shell am force-stop com.cruise.assistant.debug
"$ADB" shell am start -n com.cruise.assistant.debug/com.cruise.assistant.MainActivity
sleep 3
"$ADB" exec-out screencap -p > /tmp/before_add.png
```

手动（或用 `adb shell input tap`）点 FAB，截图 `/tmp/event_sheet.png`。
Expected: 看到"选择事项"6 个彩色图标 + 编辑按钮、事件内容输入框、
**地点**（楼层下拉 + 船头/船中/船尾 + 地点名 + 不设地点勾选）、
开始/结束双滚轮、**每日同一时间循环**开关、确认按钮。

填一个 `13:15 演出 / 9F船尾剧场`，确认后回时间轴看卡片三行是否正确。

- [ ] **Step 6: 验证循环开关**

新建一个 `07:30 早餐` 并打开循环开关，确认后：

```bash
ADB="/c/Users/Administrator/AppData/Local/Android/Sdk/platform-tools/adb.exe"
"$ADB" shell "run-as com.cruise.assistant.debug sqlite3 /data/data/com.cruise.assistant.debug/databases/cruise.db 'SELECT date, title FROM event WHERE title=\"早餐\" ORDER BY date;'"
```

Expected: 从创建日到**下船日前一天**每天一行，下船日那天没有。

- [ ] **Step 7: Commit**

```bash
git add app/src/main/java/com/cruise/assistant/ui
git commit -m "feat: add event sheet with location picker and daily recurrence toggle"
```

## Task 9: 类别编辑弹窗（需求6，基于截图3，删掉计时模式）

**Files:**
- Create: `ui/category/CategoryViewModel.kt`, `ui/category/CategorySheet.kt`
- Modify: `ui/CruiseNavHost.kt`（挂到事件弹窗的"编辑"入口）

**Interfaces:**
- Consumes: `CategoryRepository`、`CATEGORY_PALETTE`、`ICON_REGISTRY`、`iconFor`
- Produces:
  - `data class CategorySheetState(editingId, name, colorArgb, iconKey, isSubCategory, parentId, isArchived, isBuiltIn, allCategories, eventCount, errorResId, saving)`
  - `class CategoryViewModel(categoryRepository, editingCategoryId)` 暴露 `state` 与
    `onNameChange` / `onColorSelect` / `onIconSelect` / `onSubCategoryToggle` / `onParentSelect` /
    `onArchiveToggle` / `save(onDone)` / `delete(onDone)` / `startNew()` / `startEdit(id)`

- [ ] **Step 1: 写 CategoryViewModel**

`ui/category/CategoryViewModel.kt`：

```kotlin
package com.cruise.assistant.ui.category

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.cruise.assistant.R
import com.cruise.assistant.data.CATEGORY_PALETTE
import com.cruise.assistant.data.FALLBACK_CATEGORY_NAME
import com.cruise.assistant.data.repo.CategoryRepository
import com.cruise.assistant.domain.Category
import com.cruise.assistant.ui.common.ICON_REGISTRY
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch

data class CategorySheetState(
    val editingId: Long = 0,
    val name: String = "",
    val colorArgb: Int = CATEGORY_PALETTE.first(),
    val iconKey: String = ICON_REGISTRY.first().first,
    val isSubCategory: Boolean = false,
    val parentId: Long? = null,
    val isArchived: Boolean = false,
    val isBuiltIn: Boolean = false,
    val allCategories: List<Category> = emptyList(),
    val eventCount: Int = 0,
    val errorResId: Int? = null,
    val saving: Boolean = false,
) {
    val isEditing: Boolean get() = editingId != 0L
    /** 可作为上级的类别：排除自己和已归档的 */
    val parentCandidates: List<Category>
        get() = allCategories.filter { it.id != editingId && !it.isArchived && it.parentId == null }
}

class CategoryViewModel(
    private val categoryRepository: CategoryRepository,
) : ViewModel() {

    private val _state = MutableStateFlow(CategorySheetState())
    val state: StateFlow<CategorySheetState> = _state.asStateFlow()

    init {
        viewModelScope.launch {
            categoryRepository.observeAll().collect { list ->
                _state.update { it.copy(allCategories = list) }
            }
        }
    }

    /** 新建：重置为默认色 + 默认图标 */
    fun startNew() = _state.update {
        CategorySheetState(
            allCategories = it.allCategories,
            colorArgb = CATEGORY_PALETTE.first(),
            iconKey = ICON_REGISTRY.first().first,
        )
    }

    fun startEdit(categoryId: Long) {
        viewModelScope.launch {
            val category = categoryRepository.getById(categoryId) ?: return@launch
            val count = categoryRepository.countEvents(categoryId)
            _state.update {
                it.copy(
                    editingId = category.id,
                    name = category.name,
                    colorArgb = category.colorArgb,
                    iconKey = category.iconKey,
                    isSubCategory = category.parentId != null,
                    parentId = category.parentId,
                    isArchived = category.isArchived,
                    isBuiltIn = category.isBuiltIn,
                    eventCount = count,
                    errorResId = null,
                )
            }
        }
    }

    fun onNameChange(value: String) = _state.update { it.copy(name = value, errorResId = null) }
    fun onColorSelect(argb: Int) = _state.update { it.copy(colorArgb = argb) }
    fun onIconSelect(key: String) = _state.update { it.copy(iconKey = key) }
    fun onArchiveToggle(value: Boolean) = _state.update { it.copy(isArchived = value) }

    fun onSubCategoryToggle(value: Boolean) = _state.update {
        if (value) it.copy(isSubCategory = true) else it.copy(isSubCategory = false, parentId = null)
    }

    fun onParentSelect(id: Long) = _state.update { it.copy(parentId = id, isSubCategory = true) }

    fun save(onDone: () -> Unit) {
        val s = _state.value
        if (s.name.isBlank()) {
            _state.update { it.copy(errorResId = R.string.error_title_required) }
            return
        }
        _state.update { it.copy(saving = true) }
        viewModelScope.launch {
            val existing = if (s.isEditing) categoryRepository.getById(s.editingId) else null
            categoryRepository.save(
                Category(
                    id = s.editingId,
                    name = s.name.trim(),
                    colorArgb = s.colorArgb,
                    iconKey = s.iconKey,
                    parentId = if (s.isSubCategory) s.parentId else null,
                    isBuiltIn = existing?.isBuiltIn ?: false,
                    isArchived = s.isArchived,
                    sortOrder = existing?.sortOrder ?: (s.allCategories.size),
                ),
            )
            _state.update { it.copy(saving = false) }
            onDone()
        }
    }

    /** 内置类别不删；引用它的事件迁移到"琐事" */
    fun delete(onDone: () -> Unit) {
        val s = _state.value
        if (!s.isEditing || s.isBuiltIn) return
        viewModelScope.launch {
            val category = categoryRepository.getById(s.editingId) ?: return@launch
            val fallback = categoryRepository.observeAll().first()
                .firstOrNull { it.name == FALLBACK_CATEGORY_NAME && it.id != category.id }
                ?: categoryRepository.observeAll().first().first { it.id != category.id }
            categoryRepository.delete(category, fallback.id)
            onDone()
        }
    }
}
```

- [ ] **Step 2: 写 CategorySheet（截图3 结构，无计时模式）**

`ui/category/CategorySheet.kt`：

```kotlin
@file:OptIn(androidx.compose.material3.ExperimentalMaterial3Api::class)

package com.cruise.assistant.ui.category

import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.grid.GridCells
import androidx.compose.foundation.lazy.grid.LazyVerticalGrid
import androidx.compose.foundation.lazy.grid.items
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.Button
import androidx.compose.material3.DropdownMenu
import androidx.compose.material3.DropdownMenuItem
import androidx.compose.material3.FilterChip
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.ModalBottomSheet
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Switch
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.material3.rememberModalBottomSheetState
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.unit.dp
import com.cruise.assistant.R
import com.cruise.assistant.data.CATEGORY_PALETTE
import com.cruise.assistant.ui.common.ColorDot
import com.cruise.assistant.ui.common.ICON_REGISTRY
import com.cruise.assistant.ui.common.iconFor

/**
 * 截图3 的"添加图标"弹窗。
 * 保留：名称、二级分类开关、归档、主题色、图标网格。
 * 删掉：计时模式（用户明确要求）。
 */
@Composable
fun CategorySheet(
    state: CategorySheetState,
    onNameChange: (String) -> Unit,
    onColorSelect: (Int) -> Unit,
    onIconSelect: (String) -> Unit,
    onSubCategoryToggle: (Boolean) -> Unit,
    onParentSelect: (Long) -> Unit,
    onArchiveToggle: (Boolean) -> Unit,
    onEditExisting: (Long) -> Unit,
    onSave: () -> Unit,
    onDelete: () -> Unit,
    onDismiss: () -> Unit,
) {
    val sheetState = rememberModalBottomSheetState(skipPartiallyExpanded = true)
    var parentMenuOpen by remember { mutableStateOf(false) }

    ModalBottomSheet(onDismissRequest = onDismiss, sheetState = sheetState) {
        Column(
            Modifier
                .verticalScroll(rememberScrollState())
                .padding(horizontal = 20.dp)
                .padding(bottom = 24.dp),
        ) {
            Text(
                stringResource(
                    if (state.isEditing) {
                        R.string.category_sheet_title_edit
                    } else {
                        R.string.category_sheet_title_add
                    },
                ),
                style = MaterialTheme.typography.titleLarge,
            )
            Spacer(Modifier.height(16.dp))

            // ---- 现有类别横排，点一下切到编辑它 ----
            if (state.allCategories.isNotEmpty()) {
                Row(
                    Modifier
                        .fillMaxWidth()
                        .horizontalScroll(rememberScrollState()),
                    horizontalArrangement = Arrangement.spacedBy(6.dp),
                ) {
                    state.allCategories.forEach { category ->
                        FilterChip(
                            selected = category.id == state.editingId,
                            onClick = { onEditExisting(category.id) },
                            label = { Text(category.name) },
                        )
                    }
                }
                Spacer(Modifier.height(16.dp))
            }

            // ---- 名称 + 二级分类 + 归档 ----
            Row(verticalAlignment = Alignment.CenterVertically) {
                Text(
                    stringResource(R.string.category_name_label),
                    style = MaterialTheme.typography.titleMedium,
                    modifier = Modifier.weight(1f),
                )
                Text(stringResource(R.string.sub_category), style = MaterialTheme.typography.bodySmall)
                Spacer(Modifier.width(4.dp))
                Switch(checked = state.isSubCategory, onCheckedChange = onSubCategoryToggle)
                Spacer(Modifier.width(8.dp))
                FilterChip(
                    selected = state.isArchived,
                    onClick = { onArchiveToggle(!state.isArchived) },
                    label = { Text(stringResource(R.string.archive)) },
                )
            }
            Spacer(Modifier.height(8.dp))
            OutlinedTextField(
                value = state.name,
                onValueChange = onNameChange,
                label = { Text(stringResource(R.string.category_name_hint)) },
                singleLine = true,
                modifier = Modifier.fillMaxWidth(),
            )

            // ---- 上级分类（仅二级分类开启时）----
            if (state.isSubCategory) {
                Spacer(Modifier.height(12.dp))
                Box {
                    OutlinedButton(
                        onClick = { parentMenuOpen = true },
                        modifier = Modifier.fillMaxWidth(),
                    ) {
                        val parentName = state.allCategories
                            .firstOrNull { it.id == state.parentId }?.name
                        Text(parentName ?: stringResource(R.string.parent_category_label))
                    }
                    DropdownMenu(
                        expanded = parentMenuOpen,
                        onDismissRequest = { parentMenuOpen = false },
                        modifier = Modifier.heightIn(max = 280.dp),
                    ) {
                        state.parentCandidates.forEach { candidate ->
                            DropdownMenuItem(
                                text = { Text(candidate.name) },
                                onClick = {
                                    onParentSelect(candidate.id)
                                    parentMenuOpen = false
                                },
                            )
                        }
                    }
                }
            }

            // ---- 主题色 ----
            Spacer(Modifier.height(20.dp))
            Text(
                stringResource(R.string.timeline_theme_color),
                style = MaterialTheme.typography.titleMedium,
            )
            Spacer(Modifier.height(8.dp))
            Row(
                Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
            ) {
                CATEGORY_PALETTE.forEach { argb ->
                    ColorDot(
                        colorArgb = argb,
                        selected = argb == state.colorArgb,
                        onClick = { onColorSelect(argb) },
                    )
                }
            }

            // ---- 图标网格 ----
            Spacer(Modifier.height(20.dp))
            Text(stringResource(R.string.select_icon), style = MaterialTheme.typography.titleMedium)
            Spacer(Modifier.height(8.dp))
            LazyVerticalGrid(
                columns = GridCells.Fixed(7),
                modifier = Modifier.height(160.dp),
                horizontalArrangement = Arrangement.spacedBy(6.dp),
                verticalArrangement = Arrangement.spacedBy(6.dp),
            ) {
                items(ICON_REGISTRY) { (key, _) ->
                    val selected = key == state.iconKey
                    Box(
                        Modifier
                            .size(44.dp)
                            .clip(RoundedCornerShape(12.dp))
                            .background(
                                if (selected) {
                                    Color(state.colorArgb)
                                } else {
                                    MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.5f)
                                },
                            )
                            .clickable { onIconSelect(key) },
                        contentAlignment = Alignment.Center,
                    ) {
                        Icon(
                            iconFor(key),
                            contentDescription = key,
                            tint = if (selected) Color.White else MaterialTheme.colorScheme.onSurfaceVariant,
                        )
                    }
                }
            }

            state.errorResId?.let { resId ->
                Spacer(Modifier.height(12.dp))
                Text(
                    stringResource(resId),
                    color = MaterialTheme.colorScheme.error,
                    style = MaterialTheme.typography.bodySmall,
                )
            }

            // ---- 删除提示（仅自定义类别且有事件引用时）----
            if (state.isEditing && !state.isBuiltIn) {
                Spacer(Modifier.height(12.dp))
                if (state.eventCount > 0) {
                    Text(
                        stringResource(R.string.delete_category_warning_format, state.eventCount),
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                    )
                }
                TextButton(onClick = onDelete) {
                    Text(
                        stringResource(R.string.delete),
                        color = MaterialTheme.colorScheme.error,
                    )
                }
            } else if (state.isEditing && state.isBuiltIn) {
                Spacer(Modifier.height(12.dp))
                Text(
                    stringResource(R.string.builtin_undeletable),
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                )
            }

            Spacer(Modifier.height(20.dp))
            Button(
                onClick = onSave,
                enabled = !state.saving,
                modifier = Modifier.fillMaxWidth().height(52.dp),
            ) { Text(stringResource(R.string.save_changes)) }
        }
    }
}
```

类别 chip 横排需要横向滚动，用标准 import：

```kotlin
import androidx.compose.foundation.horizontalScroll
```

（`rememberScrollState` 已在上面 import 块里）

- [ ] **Step 3: 挂到事件弹窗的"编辑"入口**

在 Task 8 的 `sheetRequest?.let { ... }` 块内，`showCategorySheet` 为真时渲染：

```kotlin
                if (showCategorySheet) {
                    val catVm: CategoryViewModel = viewModel(
                        key = "category-sheet",
                        factory = viewModelFactory { CategoryViewModel(container.categoryRepository) },
                    )
                    val catState by catVm.state.collectAsState()
                    CategorySheet(
                        state = catState,
                        onNameChange = catVm::onNameChange,
                        onColorSelect = catVm::onColorSelect,
                        onIconSelect = catVm::onIconSelect,
                        onSubCategoryToggle = catVm::onSubCategoryToggle,
                        onParentSelect = catVm::onParentSelect,
                        onArchiveToggle = catVm::onArchiveToggle,
                        onEditExisting = catVm::startEdit,
                        onSave = { catVm.save { showCategorySheet = false } },
                        onDelete = { catVm.delete { showCategorySheet = false } },
                        onDismiss = { showCategorySheet = false },
                    )
                }
```

补 import：`com.cruise.assistant.ui.category.CategorySheet` / `CategoryViewModel`。

- [ ] **Step 4: 在模拟器上验证**

```bash
./gradlew installDebug
ADB="/c/Users/Administrator/AppData/Local/Android/Sdk/platform-tools/adb.exe"
"$ADB" shell am force-stop com.cruise.assistant.debug
"$ADB" shell am start -n com.cruise.assistant.debug/com.cruise.assistant.MainActivity
```

FAB → 事项网格末位"编辑" → 截图 `/tmp/category_sheet.png`。
Expected: 名称输入、二级分类开关、归档 chip、8 个彩色圆点（当前选中带黑描边）、
7 列图标网格、保存修改按钮。**不应出现"计时模式"三个按钮。**

新建一个"温泉"类别，选紫色 + spa 图标，保存。回事件弹窗确认网格里多了一个。

- [ ] **Step 5: Commit**

```bash
git add app/src/main/java/com/cruise/assistant/ui
git commit -m "feat: add category sheet with palette and icon grid, no timer mode"
```

## Task 10: 显示时间范围面板与设置页（需求2）

**Files:**
- Create: `ui/settings/DisplayRangePanel.kt`, `ui/settings/SettingsScreen.kt`,
  `ui/settings/SettingsViewModel.kt`
- Modify: `ui/CruiseNavHost.kt`、`ui/timeline/TimelineViewModel.kt`（加临时全天展开）

**Interfaces:**
- Consumes: `SettingsStore`、`VoyageRepository`
- Produces:
  - `@Composable fun DisplayRangePanel(settings, onRangeChange, onHourHeightChange, onCollapseToggle, onDismiss)`
  - `class SettingsViewModel(settingsStore, voyageRepository)` 暴露 `state: StateFlow<SettingsUiState>` 与
    `setRange` / `setHourHeight` / `setCollapse` / `setNotifications` / `setReminderMinutes` /
    `updateVoyage` / `resetVoyage`
  - `TimelineViewModel.toggleFullDay()` —— 临时把范围撑到 00:00–24:00，不写盘

- [ ] **Step 1: 写 DisplayRangePanel**

`ui/settings/DisplayRangePanel.kt`：

```kotlin
@file:OptIn(androidx.compose.material3.ExperimentalMaterial3Api::class)

package com.cruise.assistant.ui.settings

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.width
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.ModalBottomSheet
import androidx.compose.material3.Slider
import androidx.compose.material3.Switch
import androidx.compose.material3.Text
import androidx.compose.material3.rememberModalBottomSheetState
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.unit.dp
import com.cruise.assistant.R
import com.cruise.assistant.ui.common.TimeWheel
import java.time.LocalTime

/**
 * 需求2：全局显示时间范围 + 每小时高度 + 自动隐藏空时段。
 * 止时间用 24:00 表示一天结束 —— TimeWheel 只能到 23:xx，所以 23:59 视作 24:00。
 */
@Composable
fun DisplayRangePanel(
    startMinute: Int,
    endMinute: Int,
    hourHeightDp: Int,
    collapseEmptyGaps: Boolean,
    errorResId: Int?,
    onRangeChange: (Int, Int) -> Unit,
    onHourHeightChange: (Int) -> Unit,
    onCollapseToggle: (Boolean) -> Unit,
    onDismiss: () -> Unit,
) {
    val sheetState = rememberModalBottomSheetState(skipPartiallyExpanded = true)

    ModalBottomSheet(onDismissRequest = onDismiss, sheetState = sheetState) {
        Column(Modifier.padding(horizontal = 20.dp).padding(bottom = 24.dp)) {
            Text(
                stringResource(R.string.display_range_title),
                style = MaterialTheme.typography.titleLarge,
            )
            Spacer(Modifier.height(16.dp))

            Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(12.dp)) {
                Column(Modifier.weight(1f)) {
                    Text(stringResource(R.string.range_from), style = MaterialTheme.typography.titleMedium)
                    Spacer(Modifier.height(6.dp))
                    TimeWheel(
                        value = minuteToTime(startMinute),
                        onValueChange = { onRangeChange(it.hour * 60 + it.minute, endMinute) },
                        modifier = Modifier.fillMaxWidth(),
                        minuteStep = 30,
                    )
                }
                Column(Modifier.weight(1f)) {
                    Text(stringResource(R.string.range_to), style = MaterialTheme.typography.titleMedium)
                    Spacer(Modifier.height(6.dp))
                    TimeWheel(
                        value = minuteToTime(endMinute),
                        onValueChange = {
                            val minutes = it.hour * 60 + it.minute
                            // 23:30 之后视作 24:00，方便选"到一天结束"
                            onRangeChange(startMinute, if (minutes >= 23 * 60 + 30) 1440 else minutes)
                        },
                        modifier = Modifier.fillMaxWidth(),
                        minuteStep = 30,
                    )
                }
            }

            errorResId?.let { resId ->
                Spacer(Modifier.height(8.dp))
                Text(
                    stringResource(resId),
                    color = MaterialTheme.colorScheme.error,
                    style = MaterialTheme.typography.bodySmall,
                )
            }

            Spacer(Modifier.height(20.dp))
            Row(verticalAlignment = Alignment.CenterVertically) {
                Text(
                    stringResource(R.string.hour_height_label),
                    style = MaterialTheme.typography.titleMedium,
                    modifier = Modifier.weight(1f),
                )
                Text("${hourHeightDp}dp", style = MaterialTheme.typography.bodyMedium)
            }
            Slider(
                value = hourHeightDp.toFloat(),
                onValueChange = { onHourHeightChange(it.toInt()) },
                valueRange = 36f..140f,
                steps = 12,
            )

            Spacer(Modifier.height(8.dp))
            Row(verticalAlignment = Alignment.CenterVertically) {
                Text(
                    stringResource(R.string.collapse_empty_gaps),
                    style = MaterialTheme.typography.bodyMedium,
                    modifier = Modifier.weight(1f),
                )
                Switch(checked = collapseEmptyGaps, onCheckedChange = onCollapseToggle)
            }
        }
    }
}

private fun minuteToTime(minute: Int): LocalTime {
    val clamped = minute.coerceIn(0, 1439)
    return LocalTime.of(clamped / 60, clamped % 60)
}
```

- [ ] **Step 2: 写 SettingsViewModel 与 SettingsScreen**

`ui/settings/SettingsViewModel.kt`：

```kotlin
package com.cruise.assistant.ui.settings

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.cruise.assistant.R
import com.cruise.assistant.data.prefs.AppSettings
import com.cruise.assistant.data.prefs.SettingsStore
import com.cruise.assistant.data.repo.VoyageRepository
import com.cruise.assistant.domain.Voyage
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch

data class SettingsUiState(
    val settings: AppSettings,
    val voyage: Voyage?,
    val rangeErrorResId: Int? = null,
)

/** 显示范围最小跨度，和 domain 层保持一致 */
private const val MIN_RANGE_MINUTES = 120

class SettingsViewModel(
    private val settingsStore: SettingsStore,
    private val voyageRepository: VoyageRepository,
) : ViewModel() {

    private val rangeError = MutableStateFlow<Int?>(null)

    val state: StateFlow<SettingsUiState?> = combine(
        settingsStore.settings,
        voyageRepository.observeVoyage(),
        rangeError,
    ) { settings, voyage, error ->
        SettingsUiState(settings, voyage, error)
    }.stateIn(viewModelScope, SharingStarted.WhileSubscribed(5_000), null)

    fun setRange(startMinute: Int, endMinute: Int) {
        if (endMinute - startMinute < MIN_RANGE_MINUTES) {
            rangeError.value = R.string.error_range_too_short
            return
        }
        rangeError.value = null
        viewModelScope.launch { settingsStore.setDisplayRange(startMinute, endMinute) }
    }

    fun setHourHeight(dp: Int) {
        viewModelScope.launch { settingsStore.setHourHeight(dp.coerceIn(36, 140)) }
    }

    fun setCollapse(enabled: Boolean) {
        viewModelScope.launch { settingsStore.setCollapseEmptyGaps(enabled) }
    }

    fun setNotifications(enabled: Boolean) {
        viewModelScope.launch { settingsStore.setNotificationsEnabled(enabled) }
    }

    fun setReminderMinutes(minutes: Int) {
        viewModelScope.launch { settingsStore.setDefaultReminderMinutes(minutes.coerceIn(0, 240)) }
    }

    fun updateVoyage(voyage: Voyage) {
        viewModelScope.launch { voyageRepository.updateVoyage(voyage) }
    }

    fun resetVoyage(onDone: () -> Unit) {
        viewModelScope.launch {
            voyageRepository.resetVoyage()
            settingsStore.setOnboardingCompleted(false)
            onDone()
        }
    }
}
```

`ui/settings/SettingsScreen.kt`：

```kotlin
package com.cruise.assistant.ui.settings

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.FilterChip
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.Slider
import androidx.compose.material3.Switch
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.unit.dp
import com.cruise.assistant.R
import java.time.format.DateTimeFormatter

private val DATE_FMT = DateTimeFormatter.ofPattern("yyyy年M月d日")
private val REMINDER_PRESETS = listOf(5, 10, 15, 30, 60)

@Composable
fun SettingsScreen(
    state: SettingsUiState,
    onRangeChange: (Int, Int) -> Unit,
    onHourHeightChange: (Int) -> Unit,
    onCollapseToggle: (Boolean) -> Unit,
    onNotificationsToggle: (Boolean) -> Unit,
    onReminderMinutesChange: (Int) -> Unit,
    onResetVoyage: () -> Unit,
    onManageCategories: () -> Unit,
    exactAlarmAvailable: Boolean,
) {
    var confirmReset by remember { mutableStateOf(false) }

    Column(
        Modifier
            .fillMaxSize()
            .verticalScroll(rememberScrollState())
            .padding(20.dp),
    ) {
        Text(stringResource(R.string.settings), style = MaterialTheme.typography.titleLarge)
        Spacer(Modifier.height(20.dp))

        // ---- 显示时间范围 ----
        Text(stringResource(R.string.display_range_title), style = MaterialTheme.typography.titleMedium)
        Spacer(Modifier.height(8.dp))
        Text(
            "%02d:%02d – %02d:%02d".format(
                state.settings.displayStartMinute / 60,
                state.settings.displayStartMinute % 60,
                state.settings.displayEndMinute / 60,
                state.settings.displayEndMinute % 60,
            ),
            style = MaterialTheme.typography.bodyMedium,
        )
        Spacer(Modifier.height(8.dp))
        Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
            listOf(
                7 * 60 to 24 * 60,
                8 * 60 to 23 * 60,
                6 * 60 to 24 * 60,
                0 to 24 * 60,
            ).forEach { (from, to) ->
                FilterChip(
                    selected = state.settings.displayStartMinute == from &&
                        state.settings.displayEndMinute == to,
                    onClick = { onRangeChange(from, to) },
                    label = { Text("%02d–%02d".format(from / 60, to / 60)) },
                )
            }
        }
        state.rangeErrorResId?.let { resId ->
            Spacer(Modifier.height(6.dp))
            Text(
                stringResource(resId),
                color = MaterialTheme.colorScheme.error,
                style = MaterialTheme.typography.bodySmall,
            )
        }

        Spacer(Modifier.height(16.dp))
        Row(verticalAlignment = Alignment.CenterVertically) {
            Text(
                stringResource(R.string.hour_height_label),
                style = MaterialTheme.typography.bodyMedium,
                modifier = Modifier.weight(1f),
            )
            Text("${state.settings.hourHeightDp}dp", style = MaterialTheme.typography.bodyMedium)
        }
        Slider(
            value = state.settings.hourHeightDp.toFloat(),
            onValueChange = { onHourHeightChange(it.toInt()) },
            valueRange = 36f..140f,
            steps = 12,
        )

        Row(verticalAlignment = Alignment.CenterVertically) {
            Text(
                stringResource(R.string.collapse_empty_gaps),
                style = MaterialTheme.typography.bodyMedium,
                modifier = Modifier.weight(1f),
            )
            Switch(checked = state.settings.collapseEmptyGaps, onCheckedChange = onCollapseToggle)
        }

        Spacer(Modifier.height(20.dp))
        HorizontalDivider()
        Spacer(Modifier.height(20.dp))

        // ---- 提醒 ----
        Text(stringResource(R.string.reminder_section), style = MaterialTheme.typography.titleMedium)
        Spacer(Modifier.height(8.dp))
        Row(verticalAlignment = Alignment.CenterVertically) {
            Text(
                stringResource(R.string.reminder_enabled),
                style = MaterialTheme.typography.bodyMedium,
                modifier = Modifier.weight(1f),
            )
            Switch(
                checked = state.settings.notificationsEnabled,
                onCheckedChange = onNotificationsToggle,
            )
        }
        if (state.settings.notificationsEnabled) {
            Spacer(Modifier.height(8.dp))
            Text(stringResource(R.string.reminder_lead_time), style = MaterialTheme.typography.bodyMedium)
            Spacer(Modifier.height(6.dp))
            Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                REMINDER_PRESETS.forEach { minutes ->
                    FilterChip(
                        selected = state.settings.defaultReminderMinutes == minutes,
                        onClick = { onReminderMinutesChange(minutes) },
                        label = { Text(stringResource(R.string.minutes_format, minutes)) },
                    )
                }
            }
            if (!exactAlarmAvailable) {
                Spacer(Modifier.height(8.dp))
                Text(
                    stringResource(R.string.exact_alarm_unavailable),
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.error,
                )
            }
        }

        Spacer(Modifier.height(20.dp))
        HorizontalDivider()
        Spacer(Modifier.height(20.dp))

        // ---- 航次 ----
        Text(stringResource(R.string.voyage_section), style = MaterialTheme.typography.titleMedium)
        Spacer(Modifier.height(8.dp))
        state.voyage?.let { voyage ->
            Text(
                "${voyage.boardDate.format(DATE_FMT)} ${voyage.boardTime} → " +
                    "${voyage.disembarkDate.format(DATE_FMT)} ${voyage.disembarkTime}",
                style = MaterialTheme.typography.bodyMedium,
            )
            Text(
                "${voyage.dayCount} 天 · 最高 ${voyage.maxDeck}F",
                style = MaterialTheme.typography.bodySmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
            )
        }
        Spacer(Modifier.height(12.dp))
        OutlinedButton(onClick = { confirmReset = true }, modifier = Modifier.fillMaxWidth()) {
            Text(stringResource(R.string.reset_voyage))
        }

        Spacer(Modifier.height(20.dp))
        HorizontalDivider()
        Spacer(Modifier.height(20.dp))

        OutlinedButton(onClick = onManageCategories, modifier = Modifier.fillMaxWidth()) {
            Text(stringResource(R.string.category_section))
        }
        Spacer(Modifier.height(40.dp))
    }

    if (confirmReset) {
        AlertDialog(
            onDismissRequest = { confirmReset = false },
            title = { Text(stringResource(R.string.reset_voyage)) },
            text = { Text(stringResource(R.string.reset_voyage_warning)) },
            confirmButton = {
                TextButton(onClick = {
                    confirmReset = false
                    onResetVoyage()
                }) { Text(stringResource(R.string.confirm)) }
            },
            dismissButton = {
                TextButton(onClick = { confirmReset = false }) {
                    Text(stringResource(R.string.cancel))
                }
            },
        )
    }
}
```

**注意**：spec §4.6 提到"修改航次日期/时间"，`VoyageRepository.updateVoyage` 已实现，
但设置页只放"重建航次"入口 —— 修改日期会删掉范围外事件，风险高于收益，
用户真要改就重建。这是有意的范围收缩，已在 §10 之外单独说明。

- [ ] **Step 3: 给 TimelineViewModel 加临时全天展开**

在 `TimelineViewModel` 里加：

```kotlin
    private val fullDayOverride = MutableStateFlow(false)

    fun toggleFullDay() { fullDayOverride.value = !fullDayOverride.value }
```

并把 `state` 里 `buildTimelineLayout` 的两个范围参数改成：

```kotlin
                        rangeStartMinute = if (fullDay) 0 else settings.displayStartMinute,
                        rangeEndMinute = if (fullDay) 1440 else settings.displayEndMinute,
```

`fullDay` 从 `combine` 多加的一路 flow 取。`combine` 最多 5 个具名参数，超过要用
`Array` 版 —— 已经是 `Array` 版，把 `fullDayOverride` 加成第 7 路即可，
索引 6 取 `values[6] as Boolean`。切换日期时重置 `fullDayOverride.value = false`。

`TimelineScreen` 的两个 `HiddenHint` 的 `onClick` 改成调用传进来的 `onToggleFullDay`，
删掉未使用的 `showFullDay` 局部变量。

- [ ] **Step 4: 接入 NavHost**

`composable(Routes.SETTINGS)` 块：

```kotlin
        composable(Routes.SETTINGS) {
            val vm: SettingsViewModel = viewModel(
                factory = viewModelFactory {
                    SettingsViewModel(container.settingsStore, container.voyageRepository)
                },
            )
            val settingsState by vm.state.collectAsState()
            val current = settingsState
            if (current != null) {
                SettingsScreen(
                    state = current,
                    onRangeChange = vm::setRange,
                    onHourHeightChange = vm::setHourHeight,
                    onCollapseToggle = vm::setCollapse,
                    onNotificationsToggle = vm::setNotifications,
                    onReminderMinutesChange = vm::setReminderMinutes,
                    onResetVoyage = {
                        vm.resetVoyage {
                            navController.navigate(Routes.ONBOARDING) {
                                popUpTo(0) { inclusive = true }
                            }
                        }
                    },
                    onManageCategories = { /* 复用 CategorySheet，见下 */ },
                    exactAlarmAvailable = true, // Task 12 接真实检测
                )
            }
        }
```

时间轴顶栏 `⏱` 打开 `DisplayRangePanel`：在 `composable(Routes.TIMELINE)` 里加
`var showRangePanel by remember { mutableStateOf(false) }`，
`onOpenDisplayRange = { showRangePanel = true }`，并在块内渲染面板，
回调直接写 `container.settingsStore`（用 `rememberCoroutineScope().launch { ... }`）。

- [ ] **Step 5: 验证**

```bash
./gradlew installDebug
```

改范围为 09:00–23:00 → 时间轴刻度立刻从 09:00 起；杀进程重开仍是 09:00。
把每小时高度拉到 120dp → 卡片变高、间距变大。

- [ ] **Step 6: Commit**

```bash
git add app/src/main/java/com/cruise/assistant/ui
git commit -m "feat: add display range panel and settings screen"
```

---

## Task 11: 定稿流程与冲突弹窗（需求8）

**Files:**
- Create: `ui/timeline/ConflictDialog.kt`
- Modify: `ui/CruiseNavHost.kt`

**Interfaces:**
- Consumes: `TimelineViewModel.conflictsOn` / `finalizeDay` / `unlockDay`、`findConflicts`
- Produces: `@Composable fun ConflictDialog(conflicts, categories, onGoFix, onIgnoreAndFinalize, onDismiss)`

- [ ] **Step 1: 写 ConflictDialog**

`ui/timeline/ConflictDialog.kt`：

```kotlin
package com.cruise.assistant.ui.timeline

import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.unit.dp
import com.cruise.assistant.R
import com.cruise.assistant.domain.CruiseEvent
import com.cruise.assistant.domain.endMinute

/**
 * 需求8：定稿前列出冲突对。
 * 无冲突时直接显示"没有冲突"并把确认按钮变成生成。
 */
@Composable
fun ConflictDialog(
    conflicts: List<Pair<CruiseEvent, CruiseEvent>>,
    onGoFix: () -> Unit,
    onFinalize: () -> Unit,
    onDismiss: () -> Unit,
) {
    val hasConflicts = conflicts.isNotEmpty()

    AlertDialog(
        onDismissRequest = onDismiss,
        title = {
            Text(
                stringResource(
                    if (hasConflicts) R.string.conflict_dialog_title else R.string.conflict_dialog_none,
                ),
            )
        },
        text = if (!hasConflicts) {
            null
        } else {
            {
                Column(
                    Modifier
                        .heightIn(max = 320.dp)
                        .verticalScroll(rememberScrollState()),
                ) {
                    conflicts.forEach { (a, b) ->
                        Text(
                            stringResource(
                                R.string.conflict_item_format,
                                a.title, rangeText(a), b.title, rangeText(b),
                            ),
                            style = MaterialTheme.typography.bodySmall,
                        )
                        Spacer(Modifier.height(6.dp))
                    }
                }
            }
        },
        confirmButton = {
            TextButton(onClick = onFinalize) {
                Text(
                    stringResource(
                        if (hasConflicts) R.string.ignore_and_finalize else R.string.generate_plan,
                    ),
                )
            }
        },
        dismissButton = {
            TextButton(onClick = if (hasConflicts) onGoFix else onDismiss) {
                Text(stringResource(if (hasConflicts) R.string.go_fix else R.string.cancel))
            }
        },
    )
}

private fun rangeText(event: CruiseEvent): String {
    val end = event.endMinute
    return "%02d:%02d–%02d:%02d".format(
        event.startTime.hour, event.startTime.minute, (end / 60) % 24, end % 60,
    )
}
```

- [ ] **Step 2: 接到时间轴的"生成计划表"**

在 `composable(Routes.TIMELINE)` 里加：

```kotlin
            var conflictCheck by remember {
                mutableStateOf<List<Pair<CruiseEvent, CruiseEvent>>?>(null)
            }
            val scope = rememberCoroutineScope()

            // TimelineScreen(
            //     onGeneratePlan = {
            //         scope.launch { conflictCheck = vm.conflictsOn(current.selectedDate) }
            //     },
            //     conflictIds = conflictCheck?.flatMap { listOf(it.first.id, it.second.id) }
            //         ?.toSet() ?: emptySet(),
            // )

            conflictCheck?.let { conflicts ->
                ConflictDialog(
                    conflicts = conflicts,
                    onGoFix = { conflictCheck = null },  // 保留高亮：conflictIds 已从这里取
                    onFinalize = {
                        vm.finalizeDay(current.selectedDate)
                        conflictCheck = null
                    },
                    onDismiss = { conflictCheck = null },
                )
            }
```

**"去修改"要保留高亮**：把冲突 id 单独存一份不随弹窗关闭清空的 state：

```kotlin
            var highlightIds by remember { mutableStateOf<Set<Long>>(emptySet()) }
            // onGeneratePlan 里：
            //   scope.launch {
            //       val found = vm.conflictsOn(current.selectedDate)
            //       highlightIds = found.flatMap { listOf(it.first.id, it.second.id) }.toSet()
            //       conflictCheck = found
            //   }
            // onGoFix = { conflictCheck = null }        // 弹窗关但高亮留着
            // onFinalize = { ...; highlightIds = emptySet(); conflictCheck = null }
            // conflictIds = highlightIds
```

- [ ] **Step 3: 验证定稿流程**

```bash
./gradlew installDebug
```

1. 在某天造两个时间重叠的事件 → 点"生成计划表" → 弹窗列出这一对，卡片上出现 `!` 角标
2. 点"去修改" → 弹窗关闭、角标还在
3. 改掉冲突 → 再点"生成计划表" → 显示"没有冲突，可以生成计划表" → 确认
4. 该天变**单栏满宽只读**，FAB 消失，顶部出现"已定稿 · 解锁"，日期条圆点变实心
5. 点"解锁" → 回到 3 栏可编辑

截图定稿前后各一张：`/tmp/before_finalize.png` / `/tmp/after_finalize.png`

- [ ] **Step 4: Commit**

```bash
git add app/src/main/java/com/cruise/assistant/ui
git commit -m "feat: add day finalization with conflict check dialog"
```

## Task 12: 提醒通知（AlarmManager + 通知）

**Files:**
- Create: `reminder/NotificationHelper.kt`, `reminder/ReminderScheduler.kt`,
  `reminder/ReminderReceiver.kt`, `reminder/BootReceiver.kt`
- Modify: `AndroidManifest.xml`、`AppContainer.kt`、`CruiseApp.kt`、`MainActivity.kt`
- Modify: 各 Repository 的写操作后触发重排

**Interfaces:**
- Consumes: `EventRepository.getAllEvents()`、`CategoryRepository`、`SettingsStore`
- Produces:
  - `class ReminderScheduler(context, eventRepository, categoryRepository, settingsStore)`
    - `suspend fun rescheduleAll()`：取消全部旧闹钟 → 为未来事件重排（上限 100 个最近的）
    - `fun canScheduleExact(): Boolean`
  - `object NotificationHelper`：`ensureChannel(context)` / `notifyEvent(context, ...)`
  - `class ReminderReceiver : BroadcastReceiver`，`class BootReceiver : BroadcastReceiver`

- [ ] **Step 1: Manifest 加权限与 receiver**

在 `AndroidManifest.xml` 的 `<manifest>` 下、`<application>` 之前加：

```xml
    <uses-permission android:name="android.permission.POST_NOTIFICATIONS" />
    <uses-permission android:name="android.permission.SCHEDULE_EXACT_ALARM" />
    <uses-permission android:name="android.permission.USE_EXACT_ALARM" />
    <uses-permission android:name="android.permission.RECEIVE_BOOT_COMPLETED" />
```

在 `<application>` 内加：

```xml
        <receiver
            android:name=".reminder.ReminderReceiver"
            android:exported="false" />
        <receiver
            android:name=".reminder.BootReceiver"
            android:exported="false">
            <intent-filter>
                <action android:name="android.intent.action.BOOT_COMPLETED" />
            </intent-filter>
        </receiver>
```

- [ ] **Step 2: 写 NotificationHelper**

`reminder/NotificationHelper.kt`：

```kotlin
package com.cruise.assistant.reminder

import android.app.NotificationChannel
import android.app.NotificationManager
import android.app.PendingIntent
import android.content.Context
import android.content.Intent
import androidx.core.app.NotificationCompat
import androidx.core.app.NotificationManagerCompat
import com.cruise.assistant.MainActivity
import com.cruise.assistant.R

private const val CHANNEL_ID = "cruise_events"

object NotificationHelper {

    fun ensureChannel(context: Context) {
        val manager = context.getSystemService(NotificationManager::class.java) ?: return
        if (manager.getNotificationChannel(CHANNEL_ID) != null) return
        manager.createNotificationChannel(
            NotificationChannel(
                CHANNEL_ID,
                context.getString(R.string.notification_channel_name),
                NotificationManager.IMPORTANCE_HIGH,
            ).apply {
                description = context.getString(R.string.notification_channel_desc)
            },
        )
    }

    /**
     * @param titleLine 形如 "演出 · 扬帆起航"
     * @param bodyLine  形如 "13:15 · 9F船尾剧场"
     */
    fun notifyEvent(context: Context, eventId: Long, titleLine: String, bodyLine: String) {
        ensureChannel(context)
        val openIntent = PendingIntent.getActivity(
            context,
            eventId.toInt(),
            Intent(context, MainActivity::class.java)
                .addFlags(Intent.FLAG_ACTIVITY_CLEAR_TOP),
            PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE,
        )
        val notification = NotificationCompat.Builder(context, CHANNEL_ID)
            .setSmallIcon(android.R.drawable.ic_dialog_info)
            .setContentTitle(titleLine)
            .setContentText(bodyLine)
            .setContentIntent(openIntent)
            .setAutoCancel(true)
            .setPriority(NotificationCompat.PRIORITY_HIGH)
            .build()
        runCatching {
            NotificationManagerCompat.from(context).notify(eventId.toInt(), notification)
        }
    }
}
```

`NotificationManagerCompat` 需要 `androidx.core:core-ktx`（已有）。

- [ ] **Step 3: 写 ReminderScheduler**

`reminder/ReminderScheduler.kt`：

```kotlin
package com.cruise.assistant.reminder

import android.app.AlarmManager
import android.app.PendingIntent
import android.content.Context
import android.content.Intent
import android.os.Build
import com.cruise.assistant.data.prefs.SettingsStore
import com.cruise.assistant.data.repo.CategoryRepository
import com.cruise.assistant.data.repo.EventRepository
import kotlinx.coroutines.flow.first
import java.time.ZoneId

const val EXTRA_EVENT_ID = "event_id"
const val EXTRA_TITLE_LINE = "title_line"
const val EXTRA_BODY_LINE = "body_line"

/** 一次最多排这么多个闹钟，其余等 App 下次启动时补 */
private const val MAX_SCHEDULED = 100

class ReminderScheduler(
    private val context: Context,
    private val eventRepository: EventRepository,
    private val categoryRepository: CategoryRepository,
    private val settingsStore: SettingsStore,
) {
    private val alarmManager: AlarmManager? =
        context.getSystemService(AlarmManager::class.java)

    fun canScheduleExact(): Boolean =
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.S) {
            alarmManager?.canScheduleExactAlarms() == true
        } else {
            true
        }

    /**
     * 取消所有旧闹钟并按当前数据重排。
     * 事件增删改、设置变更、定稿/解锁、开机、App 启动时都要调。
     */
    suspend fun rescheduleAll() {
        val manager = alarmManager ?: return
        val settings = settingsStore.settings.first()
        val events = eventRepository.getAllEvents()
        val categories = categoryRepository.observeAll().first().associateBy { it.id }

        // 先全部取消（用同一 requestCode 规则即可覆盖）
        events.forEach { event ->
            manager.cancel(pendingIntentFor(event.id, null, null, cancelOnly = true))
        }
        if (!settings.notificationsEnabled) return

        val zone = ZoneId.systemDefault()
        val nowMs = System.currentTimeMillis()

        events.asSequence()
            .mapNotNull { event ->
                val lead = event.reminderMinutesBefore ?: settings.defaultReminderMinutes
                val triggerMs = event.date.atTime(event.startTime)
                    .minusMinutes(lead.toLong())
                    .atZone(zone).toInstant().toEpochMilli()
                if (triggerMs <= nowMs) null else event to triggerMs
            }
            .sortedBy { it.second }
            .take(MAX_SCHEDULED)
            .forEach { (event, triggerMs) ->
                val categoryName = categories[event.categoryId]?.name.orEmpty()
                val titleLine = if (categoryName.isEmpty()) {
                    event.title
                } else {
                    "$categoryName · ${event.title}"
                }
                val location = com.cruise.assistant.domain.formatLocation(
                    event.deck, event.zone, event.placeName,
                )
                val bodyLine = buildString {
                    append("%02d:%02d".format(event.startTime.hour, event.startTime.minute))
                    location?.let { append(" · ").append(it) }
                }
                val pending = pendingIntentFor(event.id, titleLine, bodyLine, cancelOnly = false)
                if (canScheduleExact()) {
                    manager.setExactAndAllowWhileIdle(
                        AlarmManager.RTC_WAKEUP, triggerMs, pending,
                    )
                } else {
                    // 降级：允许系统在 10 分钟窗口内择机触发
                    manager.setWindow(
                        AlarmManager.RTC_WAKEUP, triggerMs, 10 * 60_000L, pending,
                    )
                }
            }
    }

    private fun pendingIntentFor(
        eventId: Long,
        titleLine: String?,
        bodyLine: String?,
        cancelOnly: Boolean,
    ): PendingIntent {
        val intent = Intent(context, ReminderReceiver::class.java).apply {
            putExtra(EXTRA_EVENT_ID, eventId)
            titleLine?.let { putExtra(EXTRA_TITLE_LINE, it) }
            bodyLine?.let { putExtra(EXTRA_BODY_LINE, it) }
        }
        val flags = if (cancelOnly) {
            PendingIntent.FLAG_NO_CREATE or PendingIntent.FLAG_IMMUTABLE
        } else {
            PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE
        }
        // FLAG_NO_CREATE 可能返回 null，用 UPDATE_CURRENT 兜底以便 cancel 能匹配
        return PendingIntent.getBroadcast(context, eventId.toInt(), intent, flags)
            ?: PendingIntent.getBroadcast(
                context, eventId.toInt(), intent,
                PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE,
            )
    }
}
```

- [ ] **Step 4: 写两个 Receiver**

`reminder/ReminderReceiver.kt`：

```kotlin
package com.cruise.assistant.reminder

import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent

class ReminderReceiver : BroadcastReceiver() {
    override fun onReceive(context: Context, intent: Intent) {
        val eventId = intent.getLongExtra(EXTRA_EVENT_ID, 0L)
        val titleLine = intent.getStringExtra(EXTRA_TITLE_LINE) ?: return
        val bodyLine = intent.getStringExtra(EXTRA_BODY_LINE).orEmpty()
        NotificationHelper.notifyEvent(context, eventId, titleLine, bodyLine)
    }
}
```

`reminder/BootReceiver.kt`：

```kotlin
package com.cruise.assistant.reminder

import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent
import com.cruise.assistant.CruiseApp
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch

class BootReceiver : BroadcastReceiver() {
    override fun onReceive(context: Context, intent: Intent) {
        if (intent.action != Intent.ACTION_BOOT_COMPLETED) return
        val app = context.applicationContext as? CruiseApp ?: return
        val pending = goAsync()
        CoroutineScope(Dispatchers.Default).launch {
            try {
                app.container.reminderScheduler.rescheduleAll()
            } finally {
                pending.finish()
            }
        }
    }
}
```

- [ ] **Step 5: 接到 AppContainer 并在写操作后重排**

`AppContainer.kt` 加：

```kotlin
    val reminderScheduler = ReminderScheduler(
        context = context.applicationContext,
        eventRepository = eventRepository,
        categoryRepository = categoryRepository,
        settingsStore = settingsStore,
    )
```

（import `com.cruise.assistant.reminder.ReminderScheduler`）

`CruiseApp.onCreate` 末尾加启动时补排：

```kotlin
        NotificationHelper.ensureChannel(this)
        CoroutineScope(Dispatchers.Default).launch {
            container.reminderScheduler.rescheduleAll()
        }
```

（import `NotificationHelper`、`CoroutineScope`、`Dispatchers`、`launch`）

**重排触发点** —— 在这几个 ViewModel 的写操作 `launch` 块末尾追加
`reminderScheduler.rescheduleAll()`：

| ViewModel | 方法 |
|---|---|
| `EventSheetViewModel` | `save` |
| `TimelineViewModel` | `deleteEvent`、`finalizeDay`、`unlockDay` |
| `SettingsViewModel` | `setNotifications`、`setReminderMinutes` |

做法：给这三个 ViewModel 的构造参数各加一个 `private val reminderScheduler: ReminderScheduler`，
NavHost 里从 `container.reminderScheduler` 传入，然后在对应 `launch` 末尾调用。

- [ ] **Step 6: MainActivity 请求通知权限**

`MainActivity.onCreate` 里 `setContent` 之前加：

```kotlin
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU &&
            checkSelfPermission(android.Manifest.permission.POST_NOTIFICATIONS) !=
            android.content.pm.PackageManager.PERMISSION_GRANTED
        ) {
            requestPermissions(arrayOf(android.Manifest.permission.POST_NOTIFICATIONS), 1001)
        }
```

（import `android.os.Build`）

设置页的 `exactAlarmAvailable` 改成传 `container.reminderScheduler.canScheduleExact()`。

- [ ] **Step 7: 端到端验证通知真的响**

```bash
./gradlew installDebug
ADB="/c/Users/Administrator/AppData/Local/Android/Sdk/platform-tools/adb.exe"
"$ADB" shell am force-stop com.cruise.assistant.debug
"$ADB" shell am start -n com.cruise.assistant.debug/com.cruise.assistant.MainActivity
```

1. 授予通知权限
2. 设置页把提前提醒改成 5 分钟
3. 在**今天**建一个 6 分钟后开始的事件（若今天不在航次内，先在设置里重建一个含今天的航次）
4. 确认闹钟已排：

```bash
"$ADB" shell dumpsys alarm | grep -A3 "com.cruise.assistant.debug"
```

Expected: 能看到一条 `RTC_WAKEUP` 闹钟，`when=` 约为 1 分钟后

5. 等 1 分钟或直接手动触发验证 receiver 与通知渠道：

```bash
"$ADB" shell am broadcast -n com.cruise.assistant.debug/com.cruise.assistant.reminder.ReminderReceiver \
  --el event_id 1 --es title_line "演出 · 扬帆起航" --es body_line "13:15 · 9F船尾剧场"
sleep 2
"$ADB" exec-out screencap -p > /tmp/notification.png
```

Expected: 通知栏出现"演出 · 扬帆起航 / 13:15 · 9F船尾剧场"。用 Read 打开截图确认。

- [ ] **Step 8: Commit**

```bash
git add app/src/main/java/com/cruise/assistant app/src/main/AndroidManifest.xml
git commit -m "feat: add exact alarm reminders with notification channel and boot reschedule"
```

---

## Task 13: 集成验证与交付

**Files:**
- Create: `README.md`
- Modify: 修复验证中发现的问题

- [ ] **Step 1: 跑全部单元测试**

Run: `./gradlew testDebugUnitTest`
Expected: PASS，55 个测试全绿，无跳过

- [ ] **Step 2: 跑 lint 并处理 error 级问题**

Run: `./gradlew lintDebug`
Expected: 0 error。warning 可留，但如果出现 `MissingPermission`、
`UnspecifiedImmutableFlag`、`ScheduleExactAlarm` 这几类必须修掉。

报告在 `app/build/reports/lint-results-debug.html`。

- [ ] **Step 3: 全新安装走完整验收清单（spec §11）**

```bash
ADB="/c/Users/Administrator/AppData/Local/Android/Sdk/platform-tools/adb.exe"
"$ADB" uninstall com.cruise.assistant.debug
./gradlew installDebug
"$ADB" shell am start -n com.cruise.assistant.debug/com.cruise.assistant.MainActivity
```

逐条验证并各截一张图：

| # | 验收项 | 截图 |
|---|---|---|
| 1 | 引导页 → 选上船日 12:00、6 天后下船 → Day1 有 `12:00 上船 15m`（无地点行）、Day6 有 `07:00 下船` | `/tmp/v1_onboard.png` `/tmp/v1_day1.png` |
| 2 | 加两个重叠事件 → 自动分栏0/栏1；第三个重叠 → 进窄栏2 | `/tmp/v2_lanes.png` |
| 3 | 卡片三行：`[演出] 扬帆起航` / `9F船尾剧场` / `13:15 – 13:45  30m` | `/tmp/v3_card.png` |
| 4 | 范围改 09:00–23:00 → 刻度同步；杀进程重开仍保持 | `/tmp/v4_range.png` |
| 5 | 循环开关建 07:30 早餐 → Day2..Day5 有、Day6 无 | `/tmp/v5_recur.png` |
| 6 | 有冲突点"生成计划表" → 列冲突；忽略后定稿 → 单栏只读 | `/tmp/v6_conflict.png` `/tmp/v6_final.png` |
| 7 | 杀进程重开 → 回到上次查看的日期 | `/tmp/v7_restore.png` |
| 8 | 排一个近期事件 → 收到系统通知 | `/tmp/v8_notify.png` |
| 9 | 类别弹窗无"计时模式"；新建类别可选色可选图标 | `/tmp/v9_category.png` |

杀进程用：`"$ADB" shell am force-stop com.cruise.assistant.debug`

每张截图都用 Read 工具打开确认，不能只截不看。

- [ ] **Step 4: 修掉验证中发现的问题**

任何一条不通过就修，修完重跑该条。**不要**把"基本能用"当通过。
逐条记录实际结果，通过写 ✅、不通过写具体现象。

- [ ] **Step 5: 写 README**

`README.md`：

```markdown
# 邮轮助手 (Cruise Assistant)

纯本地的 Android 邮轮行程助手。准备阶段用 3 栏时间轴编排每日行程，
航行阶段变成单栏只读计划表并按时通知提醒。无网络、无账号。

## 环境要求

- Android Studio（本项目在 AI-253 上开发）
- Android SDK Platform 36、Build-Tools 36+
- JDK 21（用 Android Studio 内置 JBR，已在 `gradle.properties` 里指定）
- minSdk 26 (Android 8.0)

## 构建

```bash
./gradlew assembleDebug        # debug APK
./gradlew testDebugUnitTest    # 单元测试
./gradlew installDebug         # 装到已连接设备/模拟器
```

## 签名 release 版

1. 在 Android Studio 里 **Build → Generate Signed App Bundle / APK** 创建 keystore
2. 在项目根目录建 `keystore.properties`（已在 `.gitignore` 里，不会提交）：

```properties
storeFile=C:/path/to/your.jks
storePassword=你的密码
keyAlias=你的别名
keyPassword=你的密钥密码
```

3. 然后：

```bash
./gradlew assembleRelease
```

产物在 `app/build/outputs/apk/release/app-release.apk`。
若 `keystore.properties` 不存在，release 会构建成未签名版本。

## 功能

- **引导页**：选上下船日期与上船时间，自动生成时间轴并插入上船（15 分钟）、下船（07:00）
- **时间轴**：左侧可调时间刻度，事件区域 3 栏（两宽靠左 + 一窄贴右），时间重叠自动错开
- **事件卡片**：事件内容 / 地点（`7F船中花园`）/ 时间与时长，按类别着色
- **添加事件**：图标选事项、地点选择（楼层 + 船头中尾 + 地点名，记忆常用地点）、
  开始结束双滚轮、每日同一时间循环（循环至下船日前一天）
- **类别管理**：6 个内置类别，可改名/改色/改图标，可建自定义类别与二级分类
- **定稿**：某天点"生成计划表"，先查冲突，确认后变单栏只读，可解锁退回
- **提醒**：事件前 N 分钟系统通知；应用内"进行中/下一个"横幅与红色现在线

## 架构

```
ui/        Compose 界面 + ViewModel(StateFlow)
domain/    纯 Kotlin 逻辑（分栏、冲突、循环、时间轴映射），JVM 单测覆盖
data/      Room 数据库 + DataStore 设置 + Repository
reminder/  AlarmManager 排程 + 通知
```

`domain/` 不依赖 Android，所有易错的计算都在这一层并有测试。
```

- [ ] **Step 6: 最终确认并提交**

```bash
./gradlew testDebugUnitTest assembleDebug
git add -A
git commit -m "docs: add README with build and signing instructions"
git log --oneline
```

Expected: 测试全绿、构建成功、约 15 个提交。

**报告时必须包含**：测试实际数量与结果、每条验收项的实际结论（通过/不通过 + 现象）、
截图确认过哪些界面。不要在没跑过的情况下声称通过。

---

## 自查：spec 覆盖对照

| spec 要求 | 对应 Task |
|---|---|
| §2.1 按天独立定稿 | Task 11 |
| §2.2 3 栏布局（两宽靠左 + 一窄贴右） | Task 2b + Task 7c |
| §3 数据模型（5 个 Entity + DataStore） | Task 3 |
| §4.1 引导页 + 上下船事件 | Task 6 |
| §4.2 时间轴主页（含红线、横幅、日期条） | Task 7 |
| §4.3 事件弹窗（地点选择 + 循环开关） | Task 8 |
| §4.4 类别弹窗（无计时模式） | Task 9 |
| §4.5 显示时间范围面板 | Task 10 |
| §4.6 设置页 | Task 10 |
| §5 定稿流程 + 冲突弹窗 | Task 11 |
| §6 纯逻辑单测 | Task 2b–2f（55 个测试） |
| §7 提醒 | Task 12 |
| §8 内置 6 类别配色 + 8 色调色板 | Task 4 Step 1 |
| §9 构建配置与签名 | Task 1 + Task 13 Step 5 |
| §11 验收标准 | Task 13 Step 3 |

**已知的有意范围收缩**：设置页只提供"重建航次"，不提供"修改航次日期"入口
（`VoyageRepository.updateVoyage` 已实现但未接 UI —— 改日期会删掉范围外事件，
风险高于收益）。若后续需要，接一个 UI 即可，逻辑已就绪。

---

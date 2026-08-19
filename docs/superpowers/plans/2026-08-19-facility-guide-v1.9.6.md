# v1.9.6 船上设施与运营时间指南 Implementation Plan

> **For agentic workers:** REQUIRED SUB-SKILL: Use superpowers:subagent-driven-development (recommended) or superpowers:executing-plans to implement this plan task-by-task. Steps use checkbox (`- [ ]`) syntax for tracking.

**Goal:** 在设置界面顶部新增“船上设施指南”入口，点击进入独立全屏页面，展示全船各类别设施的位置、运营时间及营业状态，支持搜索与分类筛选，并将版本号升级至 v1.9.6。

**Architecture:** 
- 领域层基于已有的 `LocationCatalog` 内存数据构建 `FacilityItemUiModel` 及过滤/状态判定函数；
- UI 层新增 `FacilityGuideViewModel` 与 `FacilityGuideScreen`（包含顶部栏、搜索栏、分类 Chip 栏、状态过滤开关与设施卡片列表）；
- 设置界面 `SettingsScreen` 顶部新增入口卡片，并在 `CruiseNavHost` 中注册 `Routes.FACILITIES` 路由；
- 将 `build.gradle.kts` 版本升级为 1.9.6 (versionCode 22)。

**Tech Stack:** Kotlin 2.0, Jetpack Compose, Material 3, Android Navigation Compose, JUnit 4.

## Global Constraints

- **Version:** versionName = "1.9.6", versionCode = 22
- **Data Source:** `LocationCatalog.venuesByCategory()` (loaded at app startup from `locations.json`)
- **Strings:** All UI texts defined in `res/values/strings.xml`
- **TDD:** Write unit tests for domain transformations and filter logic before UI assembly

---

### Task 1: Domain Models & Logic for Facility Guide

**Files:**
- Create: `app/src/main/java/com/cruise/assistant/domain/FacilityGuide.kt`
- Create: `app/src/test/java/com/cruise/assistant/domain/FacilityGuideTest.kt`

**Interfaces:**
- Produces: 
  - `data class FacilityItemUiModel`
  - `fun LocationCatalogEntry.toFacilityItemUiModel(currentTime: LocalTime): FacilityItemUiModel`
  - `fun filterFacilities(facilities: List<FacilityItemUiModel>, query: String, categoryKey: String, onlyOpenNow: Boolean): List<FacilityItemUiModel>`
  - `fun getAllFacilityItems(currentTime: LocalTime): List<FacilityItemUiModel>`

- [ ] **Step 1: Write the failing unit tests for FacilityGuide domain logic**

Create `app/src/test/java/com/cruise/assistant/domain/FacilityGuideTest.kt`:

```kotlin
package com.cruise.assistant.domain

import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test
import java.time.LocalTime

class FacilityGuideTest {

    @Test
    fun `test 24 hours facility identification and status`() {
        val entry = LocationCatalogEntry(
            deck = 6,
            zone = ShipZone.MID,
            placeName = "宾客服务台 Guest Services",
            category = "chore",
            hours = OperatingHours(
                listOf(
                    OperatingSlot(
                        start = LocalTime.of(0, 0),
                        end = LocalTime.of(23, 59),
                    )
                )
            ),
        )

        val model = entry.toFacilityItemUiModel(currentTime = LocalTime.of(14, 0))
        assertTrue(model.is24Hours)
        assertTrue(model.isOpenNow)
        assertTrue(model.hasOperatingHours)
        assertEquals("chore", model.categoryKey)
        assertEquals("Deck 6 船中", model.formattedLocation)
    }

    @Test
    fun `test operating hours and status for specific slot`() {
        val entry = LocationCatalogEntry(
            deck = 10,
            zone = ShipZone.MID,
            placeName = "健身中心 Fitness Center",
            category = "chore",
            hours = OperatingHours(
                listOf(
                    OperatingSlot(
                        start = LocalTime.of(6, 0),
                        end = LocalTime.of(22, 0),
                    )
                )
            ),
        )

        val openModel = entry.toFacilityItemUiModel(currentTime = LocalTime.of(10, 0))
        assertTrue(openModel.isOpenNow)
        assertFalse(openModel.is24Hours)

        val closedModel = entry.toFacilityItemUiModel(currentTime = LocalTime.of(23, 0))
        assertFalse(closedModel.isOpenNow)
    }

    @Test
    fun `test facility without static hours like show stage`() {
        val entry = LocationCatalogEntry(
            deck = 6,
            zone = ShipZone.BOW,
            placeName = "华特迪士尼剧院 Walt Disney Theatre",
            category = "show",
            hours = null,
        )

        val model = entry.toFacilityItemUiModel(currentTime = LocalTime.of(15, 0))
        assertFalse(model.hasOperatingHours)
        assertFalse(model.is24Hours)
        assertTrue(model.isOpenNow) // non-configured hours default to open
    }

    @Test
    fun `test filter facilities by category, search query, and open now`() {
        val f1 = FacilityItemUiModel(
            placeName = "米奇泳池 Mickey Pool",
            categoryKey = "pool",
            categoryLabel = "泳池娱乐",
            deck = 11,
            zone = ShipZone.MID,
            formattedLocation = "Deck 11 船中",
            slots = listOf(OperatingSlot(LocalTime.of(8, 0), LocalTime.of(20, 0))),
            is24Hours = false,
            isOpenNow = true,
            hasOperatingHours = true,
            diningGroupLabel = null,
        )
        val f2 = FacilityItemUiModel(
            placeName = "宾客服务台 Guest Services",
            categoryKey = "chore",
            categoryLabel = "船上服务",
            deck = 6,
            zone = ShipZone.MID,
            formattedLocation = "Deck 6 船中",
            slots = listOf(OperatingSlot(LocalTime.of(0, 0), LocalTime.of(23, 59))),
            is24Hours = true,
            isOpenNow = true,
            hasOperatingHours = true,
            diningGroupLabel = null,
        )
        val f3 = FacilityItemUiModel(
            placeName = "漫威餐厅 Marvel Restaurant",
            categoryKey = "meal",
            categoryLabel = "餐食",
            deck = 4,
            zone = ShipZone.STERN,
            formattedLocation = "Deck 4 船尾",
            slots = listOf(OperatingSlot(LocalTime.of(17, 30), LocalTime.of(21, 0))),
            is24Hours = false,
            isOpenNow = false,
            hasOperatingHours = true,
            diningGroupLabel = "正餐餐厅",
        )

        val all = listOf(f1, f2, f3)

        // 1. All category without query
        assertEquals(3, filterFacilities(all, query = "", categoryKey = "all", onlyOpenNow = false).size)

        // 2. Filter by category
        val poolOnly = filterFacilities(all, query = "", categoryKey = "pool", onlyOpenNow = false)
        assertEquals(1, poolOnly.size)
        assertEquals("米奇泳池 Mickey Pool", poolOnly[0].placeName)

        // 3. Search by name (Chinese / English)
        val searchMickey = filterFacilities(all, query = "mickey", categoryKey = "all", onlyOpenNow = false)
        assertEquals(1, searchMickey.size)

        // 4. Search by Deck
        val searchDeck6 = filterFacilities(all, query = "Deck 6", categoryKey = "all", onlyOpenNow = false)
        assertEquals(1, searchDeck6.size)
        assertEquals("宾客服务台 Guest Services", searchDeck6[0].placeName)

        // 5. Search by Zone
        val searchStern = filterFacilities(all, query = "船尾", categoryKey = "all", onlyOpenNow = false)
        assertEquals(1, searchStern.size)
        assertEquals("漫威餐厅 Marvel Restaurant", searchStern[0].placeName)

        // 6. Filter by onlyOpenNow
        val openOnly = filterFacilities(all, query = "", categoryKey = "all", onlyOpenNow = true)
        assertEquals(2, openOnly.size)
    }
}
```

- [ ] **Step 2: Run test to verify it fails**

Run: `& ".\gradlew.bat" testDebugUnitTest --tests com.cruise.assistant.domain.FacilityGuideTest --no-daemon`
Expected: FAIL with compilation error (FacilityGuide not defined)

- [ ] **Step 3: Implement minimal domain logic**

Create `app/src/main/java/com/cruise/assistant/domain/FacilityGuide.kt`:

```kotlin
package com.cruise.assistant.domain

import java.time.LocalTime

/**
 * 设施指南 UI 模型
 */
data class FacilityItemUiModel(
    val placeName: String,
    val categoryKey: String,
    val categoryLabel: String,
    val deck: Int?,
    val zone: ShipZone?,
    val formattedLocation: String,
    val slots: List<OperatingSlot>,
    val is24Hours: Boolean,
    val isOpenNow: Boolean,
    val hasOperatingHours: Boolean,
    val diningGroupLabel: String? = null,
)

/** 类别 iconKey 到用户可读标签的映射 */
fun categoryLabelFor(categoryKey: String): String = when (categoryKey) {
    "meal" -> "餐食"
    "show" -> "演出场地"
    "chore" -> "船上服务"
    "pool" -> "泳池娱乐"
    "shop" -> "商店"
    "youth" -> "青少年活动"
    "fun" -> "娱乐项目"
    "meetup" -> "见面会"
    "important" -> "重要事项"
    else -> "其他设施"
}

/**
 * 将 LocationCatalogEntry 转换为 FacilityItemUiModel
 */
fun LocationCatalogEntry.toFacilityItemUiModel(currentTime: LocalTime = LocalTime.now()): FacilityItemUiModel {
    val configuredSlots = hours?.slots.orEmpty()
    val is24H = configuredSlots.any {
        it.start == LocalTime.of(0, 0) && (it.end == LocalTime.of(23, 59) || it.end == LocalTime.of(0, 0))
    }
    val hasHours = hours?.isConfigured == true
    val open = isOpenAt(currentTime)

    val locationStr = formatLocation(deck, zone, null, compact = false) ?: if (deck == null) "全船/Dial 0" else "Deck $deck"

    return FacilityItemUiModel(
        placeName = placeName,
        categoryKey = category,
        categoryLabel = categoryLabelFor(category),
        deck = deck,
        zone = zone,
        formattedLocation = locationStr,
        slots = configuredSlots,
        is24Hours = is24H,
        isOpenNow = open,
        hasOperatingHours = hasHours,
        diningGroupLabel = diningGroup?.label,
    )
}

/**
 * 检索所有内置设施列表
 */
fun getAllFacilityItems(currentTime: LocalTime = LocalTime.now()): List<FacilityItemUiModel> {
    return LocationCatalog.venuesByCategory().values.flatten().map {
        it.toFacilityItemUiModel(currentTime)
    }
}

/**
 * 设施多维筛选与搜索匹配
 */
fun filterFacilities(
    facilities: List<FacilityItemUiModel>,
    query: String,
    categoryKey: String,
    onlyOpenNow: Boolean,
): List<FacilityItemUiModel> {
    val trimmedQuery = query.trim().lowercase()

    return facilities.filter { item ->
        // 1. 分类筛选
        val matchesCategory = if (categoryKey == "all" || categoryKey.isEmpty()) {
            true
        } else {
            item.categoryKey == categoryKey
        }

        // 2. 营业中筛选
        val matchesOpen = if (!onlyOpenNow) {
            true
        } else {
            item.isOpenNow
        }

        // 3. 搜索匹配（名称、甲板、区域）
        val matchesSearch = if (trimmedQuery.isEmpty()) {
            true
        } else {
            val nameMatch = item.placeName.lowercase().contains(trimmedQuery)
            val locationMatch = item.formattedLocation.lowercase().contains(trimmedQuery)
            val deckMatch = item.deck?.let { "deck $it".contains(trimmedQuery) || "d$it".contains(trimmedQuery) || it.toString() == trimmedQuery } ?: false
            val zoneMatch = item.zone?.label?.contains(trimmedQuery) ?: false
            val diningMatch = item.diningGroupLabel?.lowercase()?.contains(trimmedQuery) ?: false

            nameMatch || locationMatch || deckMatch || zoneMatch || diningMatch
        }

        matchesCategory && matchesOpen && matchesSearch
    }
}
```

- [ ] **Step 4: Run test to verify it passes**

Run: `& ".\gradlew.bat" testDebugUnitTest --tests com.cruise.assistant.domain.FacilityGuideTest --no-daemon`
Expected: PASS

- [ ] **Step 5: Commit**

```bash
git add app/src/main/java/com/cruise/assistant/domain/FacilityGuide.kt app/src/test/java/com/cruise/assistant/domain/FacilityGuideTest.kt
git commit -m "feat: add FacilityGuide domain models and filtering logic with tests"
```

---

### Task 2: Strings & Resources for Facility Guide

**Files:**
- Modify: `app/src/main/res/values/strings.xml`

**Interfaces:**
- Produces string resource IDs for:
  - `facility_guide_title` ("船上设施指南")
  - `facility_guide_subtitle` ("查询全船餐饮、泳池、商店、活动中心等运营时间与位置")
  - `facility_guide_search_placeholder` ("搜索设施名称、甲板 (如 Deck 6) 或区域...")
  - `facility_guide_all_categories` ("全部")
  - `facility_guide_filter_open_now` ("仅看营业中")
  - `facility_guide_status_open` ("营业中")
  - `facility_guide_status_closed` ("休息中")
  - `facility_guide_24_hours` ("24小时全天开放")
  - `facility_guide_see_schedule` ("以演出时刻表为准")
  - `facility_guide_count_format` ("共 %d 处设施")
  - `facility_guide_empty_search` ("未找到符合条件的设施")

- [ ] **Step 1: Add string resources to `strings.xml`**

Edit `app/src/main/res/values/strings.xml` to include:
```xml
    <!-- 设施指南 (v1.9.6) -->
    <string name="facility_guide_title">船上设施指南</string>
    <string name="facility_guide_subtitle">查询全船餐饮、泳池、商店、活动中心等运营时间与位置</string>
    <string name="facility_guide_search_placeholder">搜索设施名称、甲板 (如 Deck 6) 或区域...</string>
    <string name="facility_guide_all_categories">全部</string>
    <string name="facility_guide_filter_open_now">仅看营业中</string>
    <string name="facility_guide_status_open">营业中</string>
    <string name="facility_guide_status_closed">休息中</string>
    <string name="facility_guide_24_hours">24小时全天开放</string>
    <string name="facility_guide_see_schedule">以演出时刻表为准</string>
    <string name="facility_guide_count_format">共 %d 处设施</string>
    <string name="facility_guide_empty_search">未找到符合条件的设施</string>
```

- [ ] **Step 2: Commit**

```bash
git add app/src/main/res/values/strings.xml
git commit -m "feat: add string resources for facility guide"
```

---

### Task 3: ViewModel and UI for Facility Guide Screen

**Files:**
- Create: `app/src/main/java/com/cruise/assistant/ui/facilities/FacilityGuideViewModel.kt`
- Create: `app/src/main/java/com/cruise/assistant/ui/facilities/FacilityGuideScreen.kt`

**Interfaces:**
- Produces:
  - `class FacilityGuideViewModel : ViewModel()`
  - `@Composable fun FacilityGuideScreen(...)`

- [ ] **Step 1: Implement `FacilityGuideViewModel.kt`**

Create `app/src/main/java/com/cruise/assistant/ui/facilities/FacilityGuideViewModel.kt`:

```kotlin
package com.cruise.assistant.ui.facilities

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.cruise.assistant.domain.FacilityItemUiModel
import com.cruise.assistant.domain.filterFacilities
import com.cruise.assistant.domain.getAllFacilityItems
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.flow.update
import java.time.LocalTime

data class FacilityGuideUiState(
    val searchQuery: String = "",
    val selectedCategory: String = "all",
    val onlyOpenNow: Boolean = false,
    val facilities: List<FacilityItemUiModel> = emptyList(),
    val totalCount: Int = 0,
)

data class CategoryFilterOption(
    val key: String,
    val label: String,
    val iconKey: String,
)

class FacilityGuideViewModel : ViewModel() {

    private val _searchQuery = MutableStateFlow("")
    private val _selectedCategory = MutableStateFlow("all")
    private val _onlyOpenNow = MutableStateFlow(false)

    // 所有可用分类
    val categories: List<CategoryFilterOption> = listOf(
        CategoryFilterOption("all", "全部", "sparkle"),
        CategoryFilterOption("meal", "餐食", "meal"),
        CategoryFilterOption("pool", "泳池娱乐", "pool"),
        CategoryFilterOption("shop", "商店", "shop"),
        CategoryFilterOption("youth", "青少年活动", "youth"),
        CategoryFilterOption("show", "演出场地", "show"),
        CategoryFilterOption("chore", "船上服务", "chore"),
    )

    private val allFacilities = MutableStateFlow(getAllFacilityItems())

    val state: StateFlow<FacilityGuideUiState> = combine(
        _searchQuery,
        _selectedCategory,
        _onlyOpenNow,
        allFacilities,
    ) { query, category, openNow, list ->
        val filtered = filterFacilities(list, query, category, openNow)
        FacilityGuideUiState(
            searchQuery = query,
            selectedCategory = category,
            onlyOpenNow = openNow,
            facilities = filtered,
            totalCount = filtered.size,
        )
    }.stateIn(
        scope = viewModelScope,
        started = SharingStarted.WhileSubscribed(5000),
        initialValue = FacilityGuideUiState(),
    )

    fun refreshTime() {
        allFacilities.value = getAllFacilityItems(LocalTime.now())
    }

    fun onSearchQueryChange(query: String) {
        _searchQuery.value = query
    }

    fun onCategorySelect(categoryKey: String) {
        _selectedCategory.value = categoryKey
    }

    fun onToggleOnlyOpenNow(onlyOpen: Boolean) {
        _onlyOpenNow.value = onlyOpen
    }
}
```

- [ ] **Step 2: Implement `FacilityGuideScreen.kt`**

Create `app/src/main/java/com/cruise/assistant/ui/facilities/FacilityGuideScreen.kt`:

```kotlin
package com.cruise.assistant.ui.facilities

import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.horizontalScroll
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.ExperimentalLayoutApi
import androidx.compose.foundation.layout.FlowRow
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.KeyboardActions
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.filled.Clear
import androidx.compose.material.icons.filled.Search
import androidx.compose.material.icons.outlined.AccessTime
import androidx.compose.material.icons.outlined.Check
import androidx.compose.material.icons.outlined.LocationOn
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.CenterAlignedTopAppBar
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.FilterChip
import androidx.compose.material3.FilterChipDefaults
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.OutlinedTextFieldDefaults
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.TopAppBarDefaults
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalFocusManager
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.ImeAction
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.cruise.assistant.R
import com.cruise.assistant.domain.FacilityItemUiModel
import com.cruise.assistant.domain.OperatingSlot
import com.cruise.assistant.ui.common.iconFor
import com.cruise.assistant.ui.theme.CruiseAzure
import com.cruise.assistant.ui.theme.CruiseAzureContainer
import com.cruise.assistant.ui.theme.CruiseMarine
import com.cruise.assistant.ui.theme.Slate100
import com.cruise.assistant.ui.theme.Slate200
import com.cruise.assistant.ui.theme.Slate500
import com.cruise.assistant.ui.theme.Slate700
import com.cruise.assistant.ui.theme.Slate900

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun FacilityGuideScreen(
    state: FacilityGuideUiState,
    categories: List<CategoryFilterOption>,
    onBack: () -> Unit,
    onSearchChange: (String) -> Unit,
    onCategorySelect: (String) -> Unit,
    onToggleOnlyOpenNow: (Boolean) -> Unit,
) {
    val focusManager = LocalFocusManager::current

    Scaffold(
        topBar = {
            CenterAlignedTopAppBar(
                title = {
                    Column(horizontalAlignment = Alignment.CenterHorizontally) {
                        Text(
                            stringResource(R.string.facility_guide_title),
                            style = MaterialTheme.typography.titleMedium,
                            fontWeight = FontWeight.Bold,
                            color = Slate900,
                        )
                        Text(
                            stringResource(R.string.facility_guide_count_format, state.totalCount),
                            style = MaterialTheme.typography.labelSmall,
                            color = Slate500,
                        )
                    }
                },
                navigationIcon = {
                    IconButton(onClick = onBack) {
                        Icon(
                            imageVector = Icons.AutoMirrored.Filled.ArrowBack,
                            contentDescription = stringResource(R.string.cancel),
                            tint = Slate700,
                        )
                    }
                },
                colors = TopAppBarDefaults.centerAlignedTopAppBarColors(
                    containerColor = MaterialTheme.colorScheme.surface,
                ),
            )
        },
        containerColor = MaterialTheme.colorScheme.background,
    ) { innerPadding ->
        Column(
            Modifier
                .fillMaxSize()
                .padding(innerPadding),
        ) {
            // ---- 搜索栏 ----
            Box(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(horizontal = 16.dp, vertical = 8.dp),
            ) {
                OutlinedTextField(
                    value = state.searchQuery,
                    onValueChange = onSearchChange,
                    modifier = Modifier.fillMaxWidth(),
                    placeholder = {
                        Text(
                            stringResource(R.string.facility_guide_search_placeholder),
                            style = MaterialTheme.typography.bodyMedium,
                            color = Slate500,
                            maxLines = 1,
                            overflow = TextOverflow.Ellipsis,
                        )
                    },
                    leadingIcon = {
                        Icon(
                            imageVector = Icons.Default.Search,
                            contentDescription = null,
                            tint = Slate500,
                            modifier = Modifier.size(20.dp),
                        )
                    },
                    trailingIcon = {
                        if (state.searchQuery.isNotEmpty()) {
                            IconButton(onClick = { onSearchChange("") }) {
                                Icon(
                                    imageVector = Icons.Default.Clear,
                                    contentDescription = null,
                                    tint = Slate500,
                                    modifier = Modifier.size(18.dp),
                                )
                            }
                        }
                    },
                    singleLine = true,
                    shape = RoundedCornerShape(12.dp),
                    colors = OutlinedTextFieldDefaults.colors(
                        focusedBorderColor = CruiseAzure,
                        unfocusedBorderColor = Slate200,
                        focusedContainerColor = MaterialTheme.colorScheme.surface,
                        unfocusedContainerColor = MaterialTheme.colorScheme.surface,
                    ),
                    keyboardOptions = KeyboardOptions(imeAction = ImeAction.Search),
                    keyboardActions = KeyboardActions(onSearch = { focusManager.clearFocus() }),
                )
            }

            // ---- 分类横滑栏 & 仅看营业中开关 ----
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .horizontalScroll(rememberScrollState())
                    .padding(horizontal = 16.dp, vertical = 4.dp),
                horizontalArrangement = Arrangement.spacedBy(8.dp),
                verticalAlignment = Alignment.CenterVertically,
            ) {
                // "仅看营业中" 快捷Chip
                FilterChip(
                    selected = state.onlyOpenNow,
                    onClick = { onToggleOnlyOpenNow(!state.onlyOpenNow) },
                    label = { Text(stringResource(R.string.facility_guide_filter_open_now)) },
                    leadingIcon = if (state.onlyOpenNow) {
                        { Icon(Icons.Outlined.Check, contentDescription = null, modifier = Modifier.size(14.dp)) }
                    } else null,
                    colors = FilterChipDefaults.filterChipColors(
                        selectedContainerColor = Color(0xFFE6F4EA),
                        selectedLabelColor = Color(0xFF137333),
                        selectedLeadingIconColor = Color(0xFF137333),
                    ),
                    shape = RoundedCornerShape(20.dp),
                )

                categories.forEach { cat ->
                    val isSelected = state.selectedCategory == cat.key
                    FilterChip(
                        selected = isSelected,
                        onClick = { onCategorySelect(cat.key) },
                        label = { Text(cat.label) },
                        leadingIcon = {
                            Icon(
                                imageVector = iconFor(cat.iconKey),
                                contentDescription = null,
                                modifier = Modifier.size(14.dp),
                            )
                        },
                        colors = FilterChipDefaults.filterChipColors(
                            selectedContainerColor = CruiseAzureContainer,
                            selectedLabelColor = CruiseMarine,
                            selectedLeadingIconColor = CruiseMarine,
                        ),
                        shape = RoundedCornerShape(20.dp),
                    )
                }
            }

            Spacer(Modifier.height(4.dp))

            // ---- 设施列表 ----
            if (state.facilities.isEmpty()) {
                Box(
                    modifier = Modifier
                        .fillMaxSize()
                        .padding(32.dp),
                    contentAlignment = Alignment.Center,
                ) {
                    Text(
                        stringResource(R.string.facility_guide_empty_search),
                        style = MaterialTheme.typography.bodyMedium,
                        color = Slate500,
                    )
                }
            } else {
                LazyColumn(
                    modifier = Modifier.fillMaxSize(),
                    contentPadding = PaddingValues(horizontal = 16.dp, vertical = 8.dp),
                    verticalArrangement = Arrangement.spacedBy(10.dp),
                ) {
                    items(state.facilities, key = { it.placeName + "_" + it.formattedLocation }) { facility ->
                        FacilityCard(facility)
                    }
                }
            }
        }
    }
}

@OptIn(ExperimentalLayoutApi::class)
@Composable
private fun FacilityCard(facility: FacilityItemUiModel) {
    Surface(
        shape = RoundedCornerShape(14.dp),
        color = MaterialTheme.colorScheme.surface,
        border = androidx.compose.foundation.BorderStroke(1.dp, Slate200),
        modifier = Modifier.fillMaxWidth(),
    ) {
        Column(
            Modifier
                .fillMaxWidth()
                .padding(14.dp),
        ) {
            // 头部：名称 + 类别徽标
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.Top,
            ) {
                Text(
                    text = facility.placeName,
                    style = MaterialTheme.typography.titleMedium,
                    fontWeight = FontWeight.Bold,
                    color = Slate900,
                    modifier = Modifier.weight(1f),
                )
                Spacer(Modifier.width(8.dp))
                // 类别与子类别徽标
                Row(horizontalArrangement = Arrangement.spacedBy(4.dp)) {
                    facility.diningGroupLabel?.let { groupLabel ->
                        Surface(
                            shape = RoundedCornerShape(6.dp),
                            color = CruiseAzureContainer,
                        ) {
                            Text(
                                text = groupLabel,
                                style = MaterialTheme.typography.labelSmall,
                                fontWeight = FontWeight.Medium,
                                color = CruiseMarine,
                                modifier = Modifier.padding(horizontal = 6.dp, vertical = 2.dp),
                            )
                        }
                    }
                    Surface(
                        shape = RoundedCornerShape(6.dp),
                        color = Slate100,
                    ) {
                        Row(
                            modifier = Modifier.padding(horizontal = 6.dp, vertical = 2.dp),
                            verticalAlignment = Alignment.CenterVertically,
                        ) {
                            Icon(
                                imageVector = iconFor(facility.categoryKey),
                                contentDescription = null,
                                tint = Slate700,
                                modifier = Modifier.size(12.dp),
                            )
                            Spacer(Modifier.width(3.dp))
                            Text(
                                text = facility.categoryLabel,
                                style = MaterialTheme.typography.labelSmall,
                                color = Slate700,
                            )
                        }
                    }
                }
            }

            Spacer(Modifier.height(8.dp))

            // 位置行
            Row(
                verticalAlignment = Alignment.CenterVertically,
                modifier = Modifier.fillMaxWidth(),
            ) {
                Icon(
                    imageVector = Icons.Outlined.LocationOn,
                    contentDescription = null,
                    tint = CruiseAzure,
                    modifier = Modifier.size(15.dp),
                )
                Spacer(Modifier.width(4.dp))
                Text(
                    text = facility.formattedLocation,
                    style = MaterialTheme.typography.bodyMedium,
                    fontWeight = FontWeight.SemiBold,
                    color = CruiseMarine,
                )
            }

            Spacer(Modifier.height(8.dp))

            // 运营时间行与状态
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.Top,
            ) {
                Row(
                    modifier = Modifier.weight(1f),
                    verticalAlignment = Alignment.Top,
                ) {
                    Icon(
                        imageVector = Icons.Outlined.AccessTime,
                        contentDescription = null,
                        tint = Slate500,
                        modifier = Modifier
                            .size(14.dp)
                            .padding(top = 2.dp),
                    )
                    Spacer(Modifier.width(4.dp))
                    Column {
                        when {
                            facility.is24Hours -> {
                                Text(
                                    text = stringResource(R.string.facility_guide_24_hours),
                                    style = MaterialTheme.typography.bodySmall,
                                    color = Slate700,
                                )
                            }
                            !facility.hasOperatingHours -> {
                                Text(
                                    text = stringResource(R.string.facility_guide_see_schedule),
                                    style = MaterialTheme.typography.bodySmall,
                                    color = Slate500,
                                )
                            }
                            else -> {
                                FlowRow(
                                    horizontalArrangement = Arrangement.spacedBy(8.dp),
                                    verticalArrangement = Arrangement.spacedBy(4.dp),
                                ) {
                                    facility.slots.forEach { slot ->
                                        val label = buildString {
                                            slot.meal?.let { append(it.label).append(" ") }
                                            append("%02d:%02d–%02d:%02d".format(slot.start.hour, slot.start.minute, slot.end.hour, slot.end.minute))
                                            if (slot.lateNight) append(" (夜宵)")
                                        }
                                        Text(
                                            text = label,
                                            style = MaterialTheme.typography.bodySmall,
                                            color = Slate700,
                                        )
                                    }
                                }
                            }
                        }
                    }
                }

                // 营业中/休息中 状态徽标
                if (facility.hasOperatingHours) {
                    Spacer(Modifier.width(8.dp))
                    Surface(
                        shape = RoundedCornerShape(10.dp),
                        color = if (facility.isOpenNow) Color(0xFFE6F4EA) else Slate100,
                    ) {
                        Text(
                            text = stringResource(
                                if (facility.isOpenNow) R.string.facility_guide_status_open
                                else R.string.facility_guide_status_closed
                            ),
                            style = MaterialTheme.typography.labelSmall,
                            fontWeight = FontWeight.Bold,
                            color = if (facility.isOpenNow) Color(0xFF137333) else Slate500,
                            modifier = Modifier.padding(horizontal = 7.dp, vertical = 3.dp),
                        )
                    }
                }
            }
        }
    }
}
```

- [ ] **Step 3: Run unit tests to verify**

Run: `& ".\gradlew.bat" testDebugUnitTest --no-daemon`
Expected: PASS

- [ ] **Step 4: Commit**

```bash
git add app/src/main/java/com/cruise/assistant/ui/facilities/FacilityGuideViewModel.kt app/src/main/java/com/cruise/assistant/ui/facilities/FacilityGuideScreen.kt
git commit -m "feat: implement FacilityGuideScreen and FacilityGuideViewModel"
```

---

### Task 4: Settings Screen Entry & CruiseNavHost Integration

**Files:**
- Modify: `app/src/main/java/com/cruise/assistant/ui/settings/SettingsScreen.kt`
- Modify: `app/src/main/java/com/cruise/assistant/ui/CruiseNavHost.kt`

**Interfaces:**
- Updates `SettingsScreen` parameter list with `onNavigateToFacilities: () -> Unit`
- Inserts top entry card into `SettingsScreen`
- Adds `Routes.FACILITIES = "facilities"`
- Registers `composable(Routes.FACILITIES)` in `CruiseNavHost`

- [ ] **Step 1: Update `SettingsScreen.kt` with facility guide entry card**

In `SettingsScreen.kt`:
1. Add `onNavigateToFacilities: () -> Unit` parameter to `SettingsScreen`.
2. Add the top entry card right above Card 1:
```kotlin
        // ---- 顶部特色入口：船上设施指南 ----
        Surface(
            shape = cardShape,
            color = MaterialTheme.colorScheme.surface,
            border = androidx.compose.foundation.BorderStroke(1.5.dp, CruiseAzure.copy(alpha = 0.5f)),
            modifier = Modifier
                .fillMaxWidth()
                .clickable { onNavigateToFacilities() },
        ) {
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(16.dp),
                verticalAlignment = Alignment.CenterVertically,
            ) {
                Box(
                    modifier = Modifier
                        .size(40.dp)
                        .clip(CircleShape)
                        .background(CruiseAzureContainer),
                    contentAlignment = Alignment.Center,
                ) {
                    Icon(
                        imageVector = Icons.Outlined.DirectionsBoat,
                        contentDescription = null,
                        tint = CruiseMarine,
                        modifier = Modifier.size(22.dp),
                    )
                }
                Spacer(Modifier.width(12.dp))
                Column(Modifier.weight(1f)) {
                    Text(
                        stringResource(R.string.facility_guide_title),
                        style = MaterialTheme.typography.titleMedium,
                        fontWeight = FontWeight.Bold,
                        color = Slate900,
                    )
                    Spacer(Modifier.height(2.dp))
                    Text(
                        stringResource(R.string.facility_guide_subtitle),
                        style = MaterialTheme.typography.bodySmall,
                        color = Slate500,
                    )
                }
                Spacer(Modifier.width(8.dp))
                Icon(
                    imageVector = androidx.compose.material.icons.automirrored.outlined.ArrowForwardIos,
                    contentDescription = null,
                    tint = CruiseAzure,
                    modifier = Modifier.size(16.dp),
                )
            }
        }

        Spacer(Modifier.height(14.dp))
```

- [ ] **Step 2: Update `CruiseNavHost.kt` with navigation route**

In `CruiseNavHost.kt`:
1. Add `const val FACILITIES = "facilities"` to `object Routes`.
2. Connect `onNavigateToFacilities = { navController.navigate(Routes.FACILITIES) }` inside `composable(Routes.SETTINGS)`.
3. Add `composable(Routes.FACILITIES)`:
```kotlin
        composable(Routes.FACILITIES) {
            val vm: FacilityGuideViewModel = viewModel()
            val state by vm.state.collectAsState()
            FacilityGuideScreen(
                state = state,
                categories = vm.categories,
                onBack = { navController.popBackStack() },
                onSearchChange = vm::onSearchQueryChange,
                onCategorySelect = vm::onCategorySelect,
                onToggleOnlyOpenNow = vm::onToggleOnlyOpenNow,
            )
        }
```

- [ ] **Step 3: Run unit tests and build check**

Run: `& ".\gradlew.bat" testDebugUnitTest --no-daemon`
Expected: PASS

- [ ] **Step 4: Commit**

```bash
git add app/src/main/java/com/cruise/assistant/ui/settings/SettingsScreen.kt app/src/main/java/com/cruise/assistant/ui/CruiseNavHost.kt
git commit -m "feat: integrate facility guide entrance in settings and register navigation route"
```

---

### Task 5: Version Upgrade to v1.9.6 and Final Build Verification

**Files:**
- Modify: `app/build.gradle.kts`

- [ ] **Step 1: Update `versionCode` and `versionName` in `app/build.gradle.kts`**

Change `versionCode = 21` to `versionCode = 22`
Change `versionName = "1.9.5"` to `versionName = "1.9.6"`

- [ ] **Step 2: Run all unit tests and assemble debug APK**

Run: `& ".\gradlew.bat" testDebugUnitTest assembleDebug --no-daemon`
Expected: BUILD SUCCESSFUL

- [ ] **Step 3: Commit**

```bash
git add app/build.gradle.kts
git commit -m "chore: bump version to v1.9.6 (versionCode 22)"
```

# Wishlist (Untimed Activities) & Timeline Jump Button Implementation Plan

> **For agentic workers:** REQUIRED SUB-SKILL: Use superpowers:subagent-driven-development (recommended) or superpowers:executing-plans to implement this plan task-by-task. Steps use checkbox (`- [ ]`) syntax for tracking.

**Goal:** Simplify timeline jump button text to "转跳当前时间" with rounded rectangle shape, and build the "随心打卡清单" (Wishlist / Untimed Activities) feature with persistence, dedicated screen, full event-style editor, completion toggle, and map pin linkage, bumping version to v1.10.0.

**Architecture:**
- Create `WishlistItemEntity`, `WishlistDao`, and `WishlistRepository` for Room database persistence.
- Implement `WishlistViewModel`, `WishlistEditSheet`, and `WishlistScreen`.
- Refine `TimelineScreen` jump button to "转跳当前时间" with `RoundedCornerShape(10.dp)`, and add Wishlist FAB to bottom-right button column.
- Bump version to `v1.10.0` (versionCode 27).

**Tech Stack:** Kotlin, Jetpack Compose, Room DB, Material3, JUnit4.

## Global Constraints
- Target Android SDK 36, minSdk 26.
- Version: `versionCode = 27`, `versionName = "1.10.0"`.
- All tests must pass.

---

### Task 1: Database & Repository for Wishlist

**Files:**
- Create: `app/src/main/java/com/cruise/assistant/data/db/entity/WishlistItemEntity.kt`
- Create: `app/src/main/java/com/cruise/assistant/data/db/dao/WishlistDao.kt`
- Modify: `app/src/main/java/com/cruise/assistant/data/db/CruiseDatabase.kt`
- Modify: `app/src/main/java/com/cruise/assistant/data/db/Mappers.kt`
- Create: `app/src/main/java/com/cruise/assistant/domain/Wishlist.kt`
- Create: `app/src/main/java/com/cruise/assistant/data/repo/WishlistRepository.kt`
- Modify: `app/src/main/java/com/cruise/assistant/AppContainer.kt`
- Create: `app/src/test/java/com/cruise/assistant/data/repo/WishlistRepositoryTest.kt`

- [ ] **Step 1: Write test for WishlistRepository**

In `app/src/test/java/com/cruise/assistant/data/repo/WishlistRepositoryTest.kt`, test:
- Inserting an item.
- Observing all items mapped to domain with category.
- Toggling completion status.
- Deleting an item.

- [ ] **Step 2: Implement Entity, DAO, Mappers and Repository**

- In `WishlistItemEntity`:
  ```kotlin
  @Entity(tableName = "wishlist_item", indices = [Index("createdAt")])
  data class WishlistItemEntity(
      @PrimaryKey(autoGenerate = true) val id: Long = 0,
      val title: String,
      val categoryId: Long,
      val deck: Int? = null,
      val zone: String? = null,
      val placeName: String? = null,
      val note: String? = null,
      val isCompleted: Boolean = false,
      val createdAt: Long = System.currentTimeMillis(),
  )
  ```
- In `WishlistDao`:
  ```kotlin
  @Dao
  interface WishlistDao {
      @Query("SELECT * FROM wishlist_item ORDER BY isCompleted ASC, createdAt DESC")
      fun observeAll(): Flow<List<WishlistItemEntity>>

      @Insert
      suspend fun insert(item: WishlistItemEntity): Long

      @Update
      suspend fun update(item: WishlistItemEntity)

      @Delete
      suspend fun delete(item: WishlistItemEntity)

      @Query("UPDATE wishlist_item SET isCompleted = :completed WHERE id = :id")
      suspend fun toggleCompleted(id: Long, completed: Boolean)
  }
  ```
- In `CruiseDatabase.kt`: register `WishlistItemEntity` and `abstract fun wishlistDao(): WishlistDao`.
- In `WishlistRepository`: expose `observeAll(): Flow<List<WishlistItem>>`, `save(item: WishlistItem): Long`, `delete(item: WishlistItem)`, `toggleCompleted(id: Long, completed: Boolean)`.
- In `AppContainer.kt`: instantiate `val wishlistRepository = WishlistRepository(db.wishlistDao(), categoryRepository)`.

- [ ] **Step 3: Run test to verify it passes**

Run: `.\gradlew.bat testDebugUnitTest --tests "com.cruise.assistant.data.repo.WishlistRepositoryTest" --no-daemon`
Expected: PASS.

- [ ] **Step 4: Commit**

```bash
git add app/src/main/java/com/cruise/assistant/data/db/entity/WishlistItemEntity.kt app/src/main/java/com/cruise/assistant/data/db/dao/WishlistDao.kt app/src/main/java/com/cruise/assistant/data/db/CruiseDatabase.kt app/src/main/java/com/cruise/assistant/data/db/Mappers.kt app/src/main/java/com/cruise/assistant/domain/Wishlist.kt app/src/main/java/com/cruise/assistant/data/repo/WishlistRepository.kt app/src/main/java/com/cruise/assistant/AppContainer.kt app/src/test/java/com/cruise/assistant/data/repo/WishlistRepositoryTest.kt
git commit -m "feat: implement WishlistItem entity, dao, and repository"
```

---

### Task 2: Build Wishlist UI Components (Screen, EditSheet, ViewModel)

**Files:**
- Create: `app/src/main/java/com/cruise/assistant/ui/wishlist/WishlistViewModel.kt`
- Create: `app/src/main/java/com/cruise/assistant/ui/wishlist/WishlistEditSheet.kt`
- Create: `app/src/main/java/com/cruise/assistant/ui/wishlist/WishlistScreen.kt`
- Modify: `app/src/main/res/values/strings.xml`

- [ ] **Step 1: Implement `WishlistViewModel`**

Manages `WishlistUiState(items: List<WishlistItem>, categories: List<Category>, ...)` with actions:
- `toggleCompleted(item)`
- `saveItem(item)`
- `deleteItem(item)`

- [ ] **Step 2: Implement `WishlistEditSheet`**

ModalBottomSheet containing:
- Category chip row (all user categories with colors & icons).
- Title OutlinedTextField (with quick suggestion chips).
- LocationPicker component (preset dropdown or custom deck & zone).
- Note OutlinedTextField.
- Save and Delete buttons.

- [ ] **Step 3: Implement `WishlistScreen`**

- TopAppBar with title "随心打卡清单" and subtitle "已完成 X / 共 Y 项", back navigation.
- Category filter row.
- `LazyColumn` of cards with:
  - Checkbox circle for instant completion toggle.
  - Category icon with color pill.
  - Title and note.
  - Formatted location chip + "定位" button (invokes `onNavigateToDeckMap(deck, placeName)`).
  - Edit on card click.
- FloatingActionButton to add new item.

- [ ] **Step 4: Commit**

```bash
git add app/src/main/java/com/cruise/assistant/ui/wishlist/WishlistViewModel.kt app/src/main/java/com/cruise/assistant/ui/wishlist/WishlistEditSheet.kt app/src/main/java/com/cruise/assistant/ui/wishlist/WishlistScreen.kt app/src/main/res/values/strings.xml
git commit -m "feat: implement WishlistViewModel, WishlistEditSheet, and WishlistScreen"
```

---

### Task 3: Update TimelineScreen & Wire Routes in CruiseNavHost

**Files:**
- Modify: `app/src/main/java/com/cruise/assistant/ui/timeline/TimelineScreen.kt`
- Modify: `app/src/main/java/com/cruise/assistant/ui/CruiseNavHost.kt`
- Modify: `app/src/main/res/values/strings.xml`

- [ ] **Step 1: Update TimelineScreen jump button & FAB column**

- Rename string `jump_to_now` -> "转跳当前时间".
- Update FilledTonalButton shape in `TimelineScreen` to `RoundedCornerShape(10.dp)`.
- Add parameter `onOpenWishlist: () -> Unit` in `TimelineScreen`.
- In FAB column, add Wishlist FAB button (`Icons.Outlined.Checklist`, tooltip "随心打卡清单", containerColor `CruiseAzureContainer`, contentColor `CruiseMarine`, shape `RoundedCornerShape(16.dp)`).

- [ ] **Step 2: Wire Routes in `CruiseNavHost`**

- Add `Routes.WISHLIST = "wishlist"`.
- In `TimelineScreen`: wire `onOpenWishlist = { navController.navigate(Routes.WISHLIST) }`.
- Add `composable(Routes.WISHLIST)`: instantiate `WishlistViewModel`, pass `onNavigateToDeckMap = { deck, venueName -> navController.navigate(Routes.deckMap(deck, venueName)) }`.

- [ ] **Step 3: Run unit tests**

Run: `.\gradlew.bat testDebugUnitTest --no-daemon`
Expected: PASS.

- [ ] **Step 4: Commit**

```bash
git add app/src/main/java/com/cruise/assistant/ui/timeline/TimelineScreen.kt app/src/main/java/com/cruise/assistant/ui/CruiseNavHost.kt app/src/main/res/values/strings.xml
git commit -m "feat: add wishlist FAB, update timeline jump button style, and wire wishlist route"
```

---

### Task 4: Bump Version to v1.10.0 & Verification on Emulator

**Files:**
- Modify: `app/build.gradle.kts`

- [ ] **Step 1: Bump version in `app/build.gradle.kts`**

Set `versionCode = 27`, `versionName = "1.10.0"`.

- [ ] **Step 2: Run all unit tests**

Run: `.\gradlew.bat testDebugUnitTest --no-daemon`
Expected: PASS.

- [ ] **Step 3: Build & install on emulator**

Run: `.\gradlew.bat installDebug --no-daemon`

- [ ] **Step 4: Verify on emulator**

- Check TimelineScreen: jump button displays "转跳当前时间" with rounded rectangle shape.
- Tap bottom-right Wishlist FAB -> opens `WishlistScreen`.
- Tap Add FAB -> opens `WishlistEditSheet`.
- Add item "玩钢铁侠过山车", Category "娱乐", Deck "18", save.
- Verify item card is displayed with "定位" button.
- Tap "定位" -> jumps to Deck 18 on DeckMap with pin.
- Tap checkbox on item card -> marks item as completed.

- [ ] **Step 5: Commit**

```bash
git add app/build.gradle.kts
git commit -m "chore: bump version to v1.10.0 (versionCode 27)"
```

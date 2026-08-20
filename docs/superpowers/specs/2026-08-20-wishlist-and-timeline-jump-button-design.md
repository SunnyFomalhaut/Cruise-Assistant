# Design: Wishlist (Untimed Activities) & Timeline Jump Button Update (v1.10.0)

- **Author**: opencode
- **Date**: 2026-08-20
- **Target Version**: v1.10.0 (versionCode 27)

## 1. Overview & Goals

This release delivers two key features:
1. **Timeline Jump Button Refinement**:
   - Rename "跳到当前时间" -> **"转跳当前时间"**.
   - Update button shape from full capsule (`20.dp`) to matching rounded rectangle (`RoundedCornerShape(10.dp)`), preserving existing container and text colors.
2. **Wishlist / Untimed Activities (随心打卡清单)**:
   - Floating Action Button (FAB) shortcut added to the bottom-right FAB column on the main screen (`TimelineScreen`).
   - Dedicated List Screen (`WishlistScreen`) for activities that don't have rigid time constraints (e.g., roller coaster on Deck 18, shopping at Duffy Store, photos at Infinity Pool).
   - Card items feature instant completion checkboxes (打卡), category icons with matching theme colors, formatted location with a direct **"定位"** button to DeckMap, and notes.
   - Creation/Editing Sheet (`WishlistEditSheet`) inherits the category picker, title input, and location picker from the main event editor, with time configuration removed.

---

## 2. Architecture & Detailed Specifications

### 2.1 Database & Persistence
- Table `wishlist_item`:
  ```kotlin
  @Entity(tableName = "wishlist_item")
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
- DAO `WishlistDao`:
  - `observeAll(): Flow<List<WishlistItemEntity>>`
  - `insert(item: WishlistItemEntity): Long`
  - `update(item: WishlistItemEntity)`
  - `delete(item: WishlistItemEntity)`
  - `toggleCompleted(id: Long, completed: Boolean)`
- Repository `WishlistRepository`:
  - Interacts with `WishlistDao` and `CategoryRepository`.
- Domain Model:
  ```kotlin
  data class WishlistItem(
      val id: Long = 0,
      val title: String,
      val category: Category?,
      val deck: Int? = null,
      val zone: ShipZone? = null,
      val placeName: String? = null,
      val note: String? = null,
      val isCompleted: Boolean = false,
      val createdAt: Long = System.currentTimeMillis(),
  )
  ```

### 2.2 UI Layer
- Route: `Routes.WISHLIST = "wishlist"`
- Screens:
  - `WishlistScreen`: Top bar with count "已完成 M / 共 N 项", filter by all/pending/completed, `LazyColumn` of cards with checkbox, location pin button, and bottom FAB to add new items.
  - `WishlistEditSheet`: ModalBottomSheet with category picker, title input, LocationPicker, and note field.
- `TimelineScreen.kt`:
  - Bottom-right FAB column:
    1. Wishlist FAB (`Icons.Outlined.Checklist`, tooltip "随心打卡清单")
    2. Pixie Dust FAB (`Icons.Outlined.AutoAwesome`)
    3. Add Event FAB (`Icons.Filled.Add`, if not finalized)
  - "转跳当前时间" button styled with `RoundedCornerShape(10.dp)`.

---

## 3. Testing & Verification

1. **Unit Tests**:
   - `WishlistRepositoryTest`: CRUD operations, toggling completion status.
   - `WishlistViewModelTest`: State flow emissions and editing actions.
2. **Emulator End-to-End Verification**:
   - Verify "转跳当前时间" button text and rounded square shape.
   - Tap bottom-right Wishlist FAB -> opens `WishlistScreen`.
   - Tap Add button -> opens `WishlistEditSheet`. Select "娱乐", Title "玩钢铁侠过山车", Location "Deck 18 船尾", save.
   - Verify card displays with "定位" button.
   - Tap "定位" -> jumps to Deck 18 on DeckMap and centers with pin.
   - Check off the item -> strike-through and marked completed.
   - Bump version to v1.10.0.

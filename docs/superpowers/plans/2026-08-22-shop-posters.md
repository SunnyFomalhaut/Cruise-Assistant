# 14 处商店底图集成与全场景适配实施计划 (v1.10.8)

> **For agentic workers:** REQUIRED SUB-SKILL: Use superpowers:subagent-driven-development (recommended) or superpowers:executing-plans to implement this plan task-by-task. Steps use checkbox (`- [ ]`) syntax for tracking.

**Goal:** 将 `商店参考图/` 中的 13 张高清商店图片优化集成至 `app/src/main/assets/posters/shops/`，在 `PosterManager.kt` 中注册 14 处商店（其中 Disney Studio 与 Royal Studio 共用单份资产），完成设施指南与日程弹窗全场景适配，并将版本号升级至 `v1.10.8`。

**Architecture:**
1. 预处理脚本将 `商店参考图/` 的 13 张图片转换并优化输出到 `app/src/main/assets/posters/shops/`；
2. 在 `PosterManager.kt` 中注册 14 处商店的中英文全称与核心关键词；
3. 更新 `PosterManagerTest.kt`，增加 14 处商店的路径匹配测试；
4. 升级版本号至 `v1.10.8` (`versionCode = 34`)，构建、安装并在模拟器上完成实机测试。

**Tech Stack:** Kotlin, Jetpack Compose, Android Assets, BitmapFactory, JUnit4, Python PIL.

## Global Constraints
- Target version: `v1.10.8` (`versionCode = 34`).
- Assets output dir: `app/src/main/assets/posters/shops/`.
- Disney Studio and Royal Studio share `posters/shops/disney_royal_studio.jpg`.
- Zero compile/test warnings or errors.

---

### Task 1: 商店剧照优化转换与 Asset 归档

**Files:**
- Create: `app/src/main/assets/posters/shops/3_wishes.jpg`
- Create: `app/src/main/assets/posters/shops/bibbidi_bobbidi_boutique.jpg`
- Create: `app/src/main/assets/posters/shops/castle_collection.jpg`
- Create: `app/src/main/assets/posters/shops/diamonds_and_wishes.jpg`
- Create: `app/src/main/assets/posters/shops/disney_royal_studio.jpg`
- Create: `app/src/main/assets/posters/shops/duffy_and_friends_shop.jpg`
- Create: `app/src/main/assets/posters/shops/marvel_style_studio.jpg`
- Create: `app/src/main/assets/posters/shops/national_geographic.jpg`
- Create: `app/src/main/assets/posters/shops/palace_treasures.jpg`
- Create: `app/src/main/assets/posters/shops/pics_photo_shop.jpg`
- Create: `app/src/main/assets/posters/shops/treasures_untold.jpg`
- Create: `app/src/main/assets/posters/shops/world_of_disney.jpg`
- Create: `app/src/main/assets/posters/shops/world_of_disney_too.jpg`

- [ ] **Step 1: 运行 Python 脚本生成 12 张优化 JPG**

```bash
python -c "
import os
from PIL import Image

out_dir = 'app/src/main/assets/posters/shops'
os.makedirs(out_dir, exist_ok=True)

mapping = {
    '3 Wishes.jpg': '3_wishes.jpg',
    'Bibbidi Bobbidi Boutique.jpg': 'bibbidi_bobbidi_boutique.jpg',
    'Castle Collection.jfif': 'castle_collection.jpg',
    'Diamonds and Wishes.jfif': 'diamonds_and_wishes.jpg',
    'Disney Studio & Royal Studio.jfif': 'disney_royal_studio.jpg',
    'Duffy and Friends Shop.jpg': 'duffy_and_friends_shop.jpg',
    'Marvel Style Studio.jfif': 'marvel_style_studio.jpg',
    'National Geographic.jfif': 'national_geographic.jpg',
    'Palace Treasures.jfif': 'palace_treasures.jpg',
    'Pics Photo Shop.jfif': 'pics_photo_shop.jpg',
    'Treasures Untold.jfif': 'treasures_untold.jpg',
    'World of Disney.jfif': 'world_of_disney.jpg',
    'World of Disney Too.jfif': 'world_of_disney_too.jpg'
}

for src_name, dst_name in mapping.items():
    src_p = os.path.join('商店参考图', src_name)
    dst_p = os.path.join(out_dir, dst_name)
    im = Image.open(src_p).convert('RGB')
    im.resize((1280, 720), Image.Resampling.LANCZOS).save(dst_p, quality=88, optimize=True)
    print(f'{dst_name}: {os.path.getsize(dst_p)} bytes')
"
```

- [ ] **Step 2: 验证 13 个商店资产文件生成成功**

```bash
python -c "
import os
files = sorted(os.listdir('app/src/main/assets/posters/shops'))
print('Count:', len(files))
assert len(files) == 13
"
```

- [ ] **Step 3: Commit**

```bash
git add app/src/main/assets/posters/shops/
git commit -m "feat(assets): add 13 optimized poster images for 14 shops"
```

---

### Task 2: 在 `PosterManager` 中注册 14 处商店并编写测试

**Files:**
- Modify: `app/src/main/java/com/cruise/assistant/domain/PosterManager.kt`
- Modify: `app/src/test/java/com/cruise/assistant/domain/PosterManagerTest.kt`

- [ ] **Step 1: 编写 `PosterManagerTest.kt` 单元测试**

在 `PosterManagerTest.kt` 中添加对全部 14 处商店的测试：

```kotlin
    @Test
    fun getFacilityPosterPath_all14Shops_matchCorrectly() {
        assertEquals("posters/shops/3_wishes.jpg", PosterManager.getFacilityPosterPath("三个愿望 3 Wishes"))
        assertEquals("posters/shops/bibbidi_bobbidi_boutique.jpg", PosterManager.getFacilityPosterPath("魔法变身精品店 Bibbidi Bobbidi Boutique"))
        assertEquals("posters/shops/castle_collection.jpg", PosterManager.getFacilityPosterPath("城堡典藏 Castle Collection"))
        assertEquals("posters/shops/diamonds_and_wishes.jpg", PosterManager.getFacilityPosterPath("钻石与愿望 Diamonds and Wishes"))
        assertEquals("posters/shops/disney_royal_studio.jpg", PosterManager.getFacilityPosterPath("迪士尼影楼 Disney Studio"))
        assertEquals("posters/shops/disney_royal_studio.jpg", PosterManager.getFacilityPosterPath("皇家影楼 Royal Studio"))
        assertEquals("posters/shops/duffy_and_friends_shop.jpg", PosterManager.getFacilityPosterPath("达菲与朋友商店 Duffy and Friends Shop"))
        assertEquals("posters/shops/marvel_style_studio.jpg", PosterManager.getFacilityPosterPath("漫威造型工作室 Marvel Style Studio"))
        assertEquals("posters/shops/national_geographic.jpg", PosterManager.getFacilityPosterPath("国家地理 National Geographic"))
        assertEquals("posters/shops/palace_treasures.jpg", PosterManager.getFacilityPosterPath("宫廷珍宝 Palace Treasures"))
        assertEquals("posters/shops/pics_photo_shop.jpg", PosterManager.getFacilityPosterPath("快照摄影店 Pics Photo Shop"))
        assertEquals("posters/shops/treasures_untold.jpg", PosterManager.getFacilityPosterPath("未诉珍宝 Treasures Untold"))
        assertEquals("posters/shops/world_of_disney.jpg", PosterManager.getFacilityPosterPath("迪士尼世界 World of Disney"))
        assertEquals("posters/shops/world_of_disney_too.jpg", PosterManager.getFacilityPosterPath("迪士尼世界二号店 World of Disney Too"))
    }
```

- [ ] **Step 2: 运行测试验证失败**

```bash
.\gradlew.bat testDebugUnitTest --tests "com.cruise.assistant.domain.PosterManagerTest"
```

- [ ] **Step 3: 在 `PosterManager.kt` 中注册 14 处商店**

在 `facilityPosterRegistry` 中加入 14 处商店的映射。

- [ ] **Step 4: 运行单元测试验证通过**

```bash
.\gradlew.bat testDebugUnitTest
```

- [ ] **Step 5: Commit**

```bash
git add app/src/main/java/com/cruise/assistant/domain/PosterManager.kt app/src/test/java/com/cruise/assistant/domain/PosterManagerTest.kt
git commit -m "feat(shops): register all 14 shops in PosterManager"
```

---

### Task 3: 升级版本号至 v1.10.8，全量构建并在模拟器验证

**Files:**
- Modify: `app/build.gradle.kts`

- [ ] **Step 1: 更新版本号**

在 `app/build.gradle.kts`：
- `versionCode = 34`
- `versionName = "1.10.8"`

- [ ] **Step 2: 运行全量单元测试与打包**

```bash
.\gradlew.bat testDebugUnitTest
.\gradlew.bat assembleDebug
```

- [ ] **Step 3: 安装到模拟器并在真机上验证设施指南的商店分类卡片**

```bash
.\gradlew.bat installDebug
```

- [ ] **Step 4: Commit**

```bash
git add app/build.gradle.kts
git commit -m "chore(release): bump version to v1.10.8 (versionCode 34)"
```

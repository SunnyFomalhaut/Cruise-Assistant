# 8部预置演出任务卡片专属背景与渐变渲染实施计划 (v1.10.7)

> **For agentic workers:** REQUIRED SUB-SKILL: Use superpowers:subagent-driven-development (recommended) or superpowers:executing-plans to implement this plan task-by-task. Steps use checkbox (`- [ ]`) syntax for tracking.

**Goal:** 将 8 部预置演出的官方剧照（含对《狮子王烟花》左上烟花专属裁切）集成到任务卡片点击弹窗（`EventDetailDialog`）中，升级 `PosterManager` 多级匹配逻辑，彻底替换通用设施底图，并升级版本号至 `v1.10.7`。

**Architecture:** 
1. 将 `演出库/` 8 张高清实景照片优化压缩并裁剪放入 `app/src/main/assets/posters/shows/`；
2. 在 `PosterManager.kt` 中建立演出专属注册表 `showPosterRegistry`，提供 `presetKey`、中英文全称及核心关键词匹配，确保系统生成与用户新建的演出均能匹配到专属海报；
3. 更新 `PosterManagerTest.kt` 单元测试，验证 8 部演出的加载机制；
4. 升级版本号至 `v1.10.7` (`versionCode = 33`)，构建并在模拟器上完成端到端验证。

**Tech Stack:** Kotlin, Jetpack Compose, Android Assets, BitmapFactory, JUnit4, Python PIL (for image preprocessing).

## Global Constraints
- Target version: `v1.10.7` (`versionCode = 33`).
- Asset directory: `app/src/main/assets/posters/shows/`.
- Lion King image crop requirement: Preserve top-left fireworks burst and bottom stage.
- Zero compile/lint/test warnings or errors.

---

### Task 1: 演出实景剧照处理与 Asset 归档

**Files:**
- Create: `app/src/main/assets/posters/shows/show_set_sail.jpg`
- Create: `app/src/main/assets/posters/shows/show_lion_king.jpg`
- Create: `app/src/main/assets/posters/shows/show_seas_adventure.jpg`
- Create: `app/src/main/assets/posters/shows/show_remember.jpg`
- Create: `app/src/main/assets/posters/shows/show_moana.jpg`
- Create: `app/src/main/assets/posters/shows/show_avengers.jpg`
- Create: `app/src/main/assets/posters/shows/show_baymax_expo.jpg`
- Create: `app/src/main/assets/posters/shows/show_duffy_friend_ship.jpg`

- [ ] **Step 1: 创建 `app/src/main/assets/posters/shows/` 目录并批量优化转换剧照**

执行 Python 脚本处理 8 张图片（狮子王烟花裁切为 1080x1080 左上烟花重点区域）：

```bash
python -c "
import os
from PIL import Image

out_dir = 'app/src/main/assets/posters/shows'
os.makedirs(out_dir, exist_ok=True)

# 1. Let's Set Sail
Image.open('演出库/Let\'s Set Sail.jfif').convert('RGB').save(os.path.join(out_dir, 'show_set_sail.jpg'), quality=88, optimize=True)

# 2. Lion King - Crop left fireworks and stage
im_lk = Image.open('演出库/The Lion King Celebration in the Sky.jfif').convert('RGB')
im_lk.crop((0, 0, 1080, 1080)).save(os.path.join(out_dir, 'show_lion_king.jpg'), quality=88, optimize=True)

# 3. Seas The Adventure
Image.open('演出库/Disney Seas The Adventure.jfif').convert('RGB').resize((1280, 720), Image.Resampling.LANCZOS).save(os.path.join(out_dir, 'show_seas_adventure.jpg'), quality=88, optimize=True)

# 4. Remember
Image.open('演出库/Remember.jfif').convert('RGB').resize((1280, 720), Image.Resampling.LANCZOS).save(os.path.join(out_dir, 'show_remember.jpg'), quality=88, optimize=True)

# 5. Moana
Image.open('演出库/Moana Call of the Sea.jfif').convert('RGB').resize((1280, 720), Image.Resampling.LANCZOS).save(os.path.join(out_dir, 'show_moana.jpg'), quality=88, optimize=True)

# 6. Avengers
Image.open('演出库/Avengers Assemble.jfif').convert('RGB').save(os.path.join(out_dir, 'show_avengers.jpg'), quality=88, optimize=True)

# 7. Baymax
Image.open('演出库/Baymax Super Exercise Expo.jfif').convert('RGB').resize((1280, 720), Image.Resampling.LANCZOS).save(os.path.join(out_dir, 'show_baymax_expo.jpg'), quality=88, optimize=True)

# 8. Duffy
Image.open('演出库/Duffy and The Friend Ship.png').convert('RGB').resize((1280, 719), Image.Resampling.LANCZOS).save(os.path.join(out_dir, 'show_duffy_friend_ship.jpg'), quality=88, optimize=True)
print('Done!')
"
```

- [ ] **Step 2: 验证 8 个 asset 文件存在且大小正常**

```bash
python -c "
import os
d = 'app/src/main/assets/posters/shows'
files = sorted(os.listdir(d))
print('Count:', len(files))
for f in files:
    print(f, os.path.getsize(os.path.join(d, f)))
"
```
Expected: Count 8, all files exist.

- [ ] **Step 3: Commit**

```bash
git add app/src/main/assets/posters/shows/
git commit -m "feat(assets): add 8 show poster images with Lion King firework crop"
```

---

### Task 2: 升级 `PosterManager` 注册表与多维度匹配

**Files:**
- Modify: `app/src/main/java/com/cruise/assistant/domain/PosterManager.kt`
- Modify: `app/src/test/java/com/cruise/assistant/domain/PosterManagerTest.kt`

- [ ] **Step 1: 编写 `PosterManagerTest.kt` 单元测试**

在 `app/src/test/java/com/cruise/assistant/domain/PosterManagerTest.kt` 中添加对 8 部演出的映射测试：

```kotlin
package com.cruise.assistant.domain

import org.junit.Assert.assertEquals
import org.junit.Assert.assertNotNull
import org.junit.Assert.assertNull
import org.junit.Test

class PosterManagerTest {

    @Test
    fun getEventPosterPath_byPresetKey_matchesAll8Shows() {
        assertEquals("posters/shows/show_set_sail.jpg", PosterManager.getEventPosterPath("任意标题", "show_set_sail"))
        assertEquals("posters/shows/show_lion_king.jpg", PosterManager.getEventPosterPath("任意标题", "show_lion_king"))
        assertEquals("posters/shows/show_seas_adventure.jpg", PosterManager.getEventPosterPath("任意标题", "show_seas_adventure"))
        assertEquals("posters/shows/show_remember.jpg", PosterManager.getEventPosterPath("任意标题", "show_remember"))
        assertEquals("posters/shows/show_moana.jpg", PosterManager.getEventPosterPath("任意标题", "show_moana"))
        assertEquals("posters/shows/show_avengers.jpg", PosterManager.getEventPosterPath("任意标题", "show_avengers"))
        assertEquals("posters/shows/show_baymax_expo.jpg", PosterManager.getEventPosterPath("任意标题", "show_baymax_expo"))
        assertEquals("posters/shows/show_duffy_friend_ship.jpg", PosterManager.getEventPosterPath("任意标题", "show_duffy_friend_ship"))
    }

    @Test
    fun getEventPosterPath_byTitleAndKeywords_matchesAll8Shows() {
        assertEquals("posters/shows/show_set_sail.jpg", PosterManager.getEventPosterPath("扬帆起航 Let's Set Sail", null))
        assertEquals("posters/shows/show_lion_king.jpg", PosterManager.getEventPosterPath("狮子王烟花 The Lion King: Celebration in the Sky", null))
        assertEquals("posters/shows/show_seas_adventure.jpg", PosterManager.getEventPosterPath("海洋奇遇 Disney Seas The Adventure", null))
        assertEquals("posters/shows/show_remember.jpg", PosterManager.getEventPosterPath("回忆：瓦力与伊芙 Remember", null))
        assertEquals("posters/shows/show_moana.jpg", PosterManager.getEventPosterPath("海洋奇缘 Moana: Call of the Sea", null))
        assertEquals("posters/shows/show_avengers.jpg", PosterManager.getEventPosterPath("复仇者集结 Avengers Assemble!", null))
        assertEquals("posters/shows/show_baymax_expo.jpg", PosterManager.getEventPosterPath("大白超级健身世博会 Baymax Super Exercise Expo", null))
        assertEquals("posters/shows/show_duffy_friend_ship.jpg", PosterManager.getEventPosterPath("达菲与友谊之船 Duffy and The Friend Ship", null))

        // 简写/关键词匹配
        assertEquals("posters/shows/show_lion_king.jpg", PosterManager.getEventPosterPath("狮子王烟花", null))
        assertEquals("posters/shows/show_duffy_friend_ship.jpg", PosterManager.getEventPosterPath("达菲", null))
        assertEquals("posters/shows/show_baymax_expo.jpg", PosterManager.getEventPosterPath("大白健身", null))
    }

    @Test
    fun getEventPosterPath_nonShowEvent_fallsBackToNull() {
        assertNull(PosterManager.getEventPosterPath("自由活动", null))
    }
}
```

- [ ] **Step 2: 运行单元测试验证失败**

```bash
.\gradlew.bat testDebugUnitTest --tests "com.cruise.assistant.domain.PosterManagerTest"
```
Expected: FAIL (method `getEventPosterPath` not defined yet).

- [ ] **Step 3: 更新 `PosterManager.kt` 实现**

在 `PosterManager.kt` 中：
1. 建立 `showPosterRegistry`；
2. 提取 `getEventPosterPath(eventTitle: String, presetKey: String?): String?` 方法；
3. 更新 `loadEventPoster` 使用该方法并从 asset 加载；
4. 移除旧的演出映射到设施通用图的代码。

- [ ] **Step 4: 运行单元测试验证通过**

```bash
.\gradlew.bat testDebugUnitTest
```
Expected: BUILD SUCCESSFUL, all tests PASS.

- [ ] **Step 5: Commit**

```bash
git add app/src/main/java/com/cruise/assistant/domain/PosterManager.kt app/src/test/java/com/cruise/assistant/domain/PosterManagerTest.kt
git commit -m "feat(shows): register 8 show posters and comprehensive multi-key lookup in PosterManager"
```

---

### Task 3: 升级版本号至 v1.10.7，全量构建并在模拟器验证

**Files:**
- Modify: `app/build.gradle.kts`
- Modify: `app/src/main/java/com/cruise/assistant/ui/settings/SettingsScreen.kt`

- [ ] **Step 1: 更新版本号**

在 `app/build.gradle.kts`：
- `versionCode = 33`
- `versionName = "1.10.7"`

在 `SettingsScreen.kt` 验证版本展示。

- [ ] **Step 2: 运行全量单元测试与构建**

```bash
.\gradlew.bat testDebugUnitTest
.\gradlew.bat assembleDebug
```
Expected: BUILD SUCCESSFUL.

- [ ] **Step 3: 安装到模拟器并在真机上测试 8 部演出弹窗**

```bash
.\gradlew.bat installDebug
```
使用 ADB 依次点击 8 部演出卡片，抓取截图并验证：《狮子王烟花》左上角烟花明显，8 部演出背景渐变效果正常。

- [ ] **Step 4: Commit**

```bash
git add app/build.gradle.kts
git commit -m "chore(release): bump version to v1.10.7 (versionCode 33)"
```

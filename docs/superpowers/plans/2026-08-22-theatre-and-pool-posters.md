# 华特迪士尼剧院与无边际泳池底图集成实施计划 (v1.10.10)

> **For agentic workers:** REQUIRED SUB-SKILL: Use superpowers:subagent-driven-development (recommended) or superpowers:executing-plans to implement this plan task-by-task. Steps use checkbox (`- [ ]`) syntax for tracking.

**Goal:** 将根目录下 `Walt Disney Theatre.jfif` 和 `infinity pool.jfif` 优化转换至 `app/src/main/assets/posters/facilities/`，在 `PosterManager.kt` 中精准注册，并严格隔离演出与酒吧背景，升级版本至 `v1.10.10`。

**Architecture:**
1. 预处理脚本将根目录下 `Walt Disney Theatre.jfif` 转换为 `walt_disney_theatre.jpg`，将 `infinity pool.jfif` 转换为 `infinity_pool.jpg`；
2. 在 `PosterManager.kt` 中注册 `walt_disney_theatre.jpg` 与 `infinity_pool.jpg`；
3. 更新 `PosterManagerTest.kt` 验证路径映射与演出/酒吧隔离；
4. 升级版本号至 `v1.10.10` (`versionCode = 36`)，构建并安装到模拟器完成验证。

**Tech Stack:** Kotlin, Jetpack Compose, Android Assets, BitmapFactory, JUnit4, Python PIL.

## Global Constraints
- Target version: `v1.10.10` (`versionCode = 36`).
- Theatre shows must continue to match their own show posters (`posters/shows/...`).
- Infinity Pool (`infinity_pool.jpg`) and Infinity Bar (`infinity_bar.jpg`) must be strictly distinguished.
- Zero compile/test warnings or errors.

---

### Task 1: 图片优化转换与 Asset 归档

**Files:**
- Create: `app/src/main/assets/posters/facilities/walt_disney_theatre.jpg`
- Create: `app/src/main/assets/posters/facilities/infinity_pool.jpg`

- [ ] **Step 1: 运行 Python 脚本生成 2 张优化 JPG**

```bash
python -c "
import os
from PIL import Image

out_dir = 'app/src/main/assets/posters/facilities'
os.makedirs(out_dir, exist_ok=True)

# 1. Walt Disney Theatre
im_t = Image.open('Walt Disney Theatre.jfif').convert('RGB')
im_t.resize((1280, 720), Image.Resampling.LANCZOS).save(os.path.join(out_dir, 'walt_disney_theatre.jpg'), quality=90, optimize=True)

# 2. Infinity Pool
im_p = Image.open('infinity pool.jfif').convert('RGB')
im_p.resize((1280, 720), Image.Resampling.LANCZOS).save(os.path.join(out_dir, 'infinity_pool.jpg'), quality=90, optimize=True)

print('Done!')
"
```

- [ ] **Step 2: Commit**

```bash
git add app/src/main/assets/posters/facilities/walt_disney_theatre.jpg app/src/main/assets/posters/facilities/infinity_pool.jpg
git commit -m "feat(assets): add Walt Disney Theatre and Infinity Pool facility poster images"
```

---

### Task 2: 在 `PosterManager` 中注册两处设施并编写测试

**Files:**
- Modify: `app/src/main/java/com/cruise/assistant/domain/PosterManager.kt`
- Modify: `app/src/test/java/com/cruise/assistant/domain/PosterManagerTest.kt`

- [ ] **Step 1: 编写 `PosterManagerTest.kt` 单元测试**

在 `PosterManagerTest.kt` 中添加：

```kotlin
    @Test
    fun getFacilityPosterPath_theatreAndPool_matchCorrectly() {
        assertEquals("posters/facilities/walt_disney_theatre.jpg", PosterManager.getFacilityPosterPath("华特迪士尼剧院 Walt Disney Theatre"))
        assertEquals("posters/facilities/infinity_pool.jpg", PosterManager.getFacilityPosterPath("无边际泳池 Infinity Pool"))
        // 隔离校验
        assertEquals("posters/dining/infinity_bar.jpg", PosterManager.getFacilityPosterPath("无边际酒吧 Infinity Bar"))
        assertEquals("posters/shows/show_seas_adventure.jpg", PosterManager.getEventPosterPath("海洋奇遇 Disney Seas The Adventure", "show_seas_adventure"))
    }
```

- [ ] **Step 2: 运行测试验证失败**

```bash
.\gradlew.bat testDebugUnitTest --tests "com.cruise.assistant.domain.PosterManagerTest"
```

- [ ] **Step 3: 在 `PosterManager.kt` 中注册**

在 `facilityPosterRegistry` 中加入：
```kotlin
        "华特迪士尼剧院" to "posters/facilities/walt_disney_theatre.jpg",
        "walt disney theatre" to "posters/facilities/walt_disney_theatre.jpg",
        "walt disney theater" to "posters/facilities/walt_disney_theatre.jpg",
        "无边际泳池" to "posters/facilities/infinity_pool.jpg",
        "infinity pool" to "posters/facilities/infinity_pool.jpg",
```

- [ ] **Step 4: 运行单元测试验证通过**

```bash
.\gradlew.bat testDebugUnitTest
```

- [ ] **Step 5: Commit**

```bash
git add app/src/main/java/com/cruise/assistant/domain/PosterManager.kt app/src/test/java/com/cruise/assistant/domain/PosterManagerTest.kt
git commit -m "feat(facilities): register Walt Disney Theatre and Infinity Pool in PosterManager with strict isolation"
```

---

### Task 3: 升级版本号至 v1.10.10，全量构建并在模拟器验证

**Files:**
- Modify: `app/build.gradle.kts`

- [ ] **Step 1: 更新版本号**

在 `app/build.gradle.kts`：
- `versionCode = 36`
- `versionName = "1.10.10"`

- [ ] **Step 2: 运行全量单元测试与打包**

```bash
.\gradlew.bat testDebugUnitTest
.\gradlew.bat assembleDebug
```

- [ ] **Step 3: 安装到模拟器并在真机上验证设施指南的剧院与无边泳池卡片**

```bash
.\gradlew.bat installDebug
```

- [ ] **Step 4: Commit**

```bash
git add app/build.gradle.kts
git commit -m "chore(release): bump version to v1.10.10 (versionCode 36)"
```

# 5 处青少年活动空间底图集成实施计划 (v1.10.11)

> **For agentic workers:** REQUIRED SUB-SKILL: Use superpowers:subagent-driven-development (recommended) or superpowers:executing-plans to implement this plan task-by-task. Steps use checkbox (`- [ ]`) syntax for tracking.

**Goal:** 将 `地点库/` 中的 5 张青少年与儿童活动空间照片优化转换至 `app/src/main/assets/posters/youth/`，在 `PosterManager.kt` 中完整注册，适配设施指南列表卡片与日程弹窗，并升级版本号至 `v1.10.11`。

**Architecture:**
1. 预处理脚本将 `地点库/` 的 5 张图片（含 AVIF, WEBP, JFIF）统一转换为优化后的高质量 JPG，存放于 `app/src/main/assets/posters/youth/`；
2. 在 `PosterManager.kt` 中注册 5 处青少年场所的中英文全称与核心关键词；
3. 更新 `PosterManagerTest.kt`，增加 5 处青少年场所的路径解析单元测试；
4. 升级版本号至 `v1.10.11` (`versionCode = 37`)，构建并安装到模拟器完成验证。

**Tech Stack:** Kotlin, Jetpack Compose, Android Assets, BitmapFactory, JUnit4, Python PIL.

## Global Constraints
- Target version: `v1.10.11` (`versionCode = 37`).
- Output directory: `app/src/main/assets/posters/youth/`.
- Zero compile/test warnings or errors.

---

### Task 1: 图片优化转换与 Asset 归档

**Files:**
- Create: `app/src/main/assets/posters/youth/its_a_small_world_nursery.jpg`
- Create: `app/src/main/assets/posters/youth/mickey_minnie_captains_deck.jpg`
- Create: `app/src/main/assets/posters/youth/disneys_oceaneer_club.jpg`
- Create: `app/src/main/assets/posters/youth/edge.jpg`
- Create: `app/src/main/assets/posters/youth/vibe.jpg`

- [ ] **Step 1: 运行 Python 脚本生成 5 张优化 JPG**

```bash
python -c "
import os
from PIL import Image

out_dir = 'app/src/main/assets/posters/youth'
os.makedirs(out_dir, exist_ok=True)

mapping = {
    'it’s a small world” nursery.avif': 'its_a_small_world_nursery.jpg',
    'Mickey & Minnie Captain’s Deck.jfif': 'mickey_minnie_captains_deck.jpg',
    'Disney\'s Oceaneer Club.webp': 'disneys_oceaneer_club.jpg',
    'Edge.jfif': 'edge.jpg',
    'Vibe.webp': 'vibe.jpg'
}

for src_name, dst_name in mapping.items():
    src_p = os.path.join('地点库', src_name)
    dst_p = os.path.join(out_dir, dst_name)
    im = Image.open(src_p).convert('RGB')
    im.resize((1000, int(1000 * im.size[1] / im.size[0])), Image.Resampling.LANCZOS).save(dst_p, quality=88, optimize=True)
    print(f'{dst_name}: {os.path.getsize(dst_p)} bytes')
"
```

- [ ] **Step 2: 验证 5 个青少年资产文件生成成功**

```bash
python -c "
import os
files = sorted(os.listdir('app/src/main/assets/posters/youth'))
print('Count:', len(files))
assert len(files) == 5
"
```

- [ ] **Step 3: Commit**

```bash
git add app/src/main/assets/posters/youth/
git commit -m "feat(assets): add 5 optimized poster images for youth and kids clubs"
```

---

### Task 2: 在 `PosterManager` 中注册 5 处青少年场所并编写测试

**Files:**
- Modify: `app/src/main/java/com/cruise/assistant/domain/PosterManager.kt`
- Modify: `app/src/test/java/com/cruise/assistant/domain/PosterManagerTest.kt`

- [ ] **Step 1: 编写 `PosterManagerTest.kt` 单元测试**

在 `PosterManagerTest.kt` 中添加：

```kotlin
    @Test
    fun getFacilityPosterPath_all5YouthClubs_matchCorrectly() {
        assertEquals("posters/youth/its_a_small_world_nursery.jpg", PosterManager.getFacilityPosterPath("小小世界托儿所 it's a small world nursery"))
        assertEquals("posters/youth/mickey_minnie_captains_deck.jpg", PosterManager.getFacilityPosterPath("米奇米妮船长甲板 Mickey & Minnie Captain's Deck"))
        assertEquals("posters/youth/disneys_oceaneer_club.jpg", PosterManager.getFacilityPosterPath("迪士尼海洋探险家俱乐部 Disney's Oceaneer Club"))
        assertEquals("posters/youth/edge.jpg", PosterManager.getFacilityPosterPath("边缘少年俱乐部 Edge"))
        assertEquals("posters/youth/vibe.jpg", PosterManager.getFacilityPosterPath("活力少年俱乐部 Vibe"))
    }
```

- [ ] **Step 2: 运行测试验证失败**

```bash
.\gradlew.bat testDebugUnitTest --tests "com.cruise.assistant.domain.PosterManagerTest"
```

- [ ] **Step 3: 在 `PosterManager.kt` 中注册 5 处青少年场所**

在 `facilityPosterRegistry` 与 `eventPosterRegistry` 中加入 5 处青少年空间映射。

- [ ] **Step 4: 运行单元测试验证通过**

```bash
.\gradlew.bat testDebugUnitTest
```

- [ ] **Step 5: Commit**

```bash
git add app/src/main/java/com/cruise/assistant/domain/PosterManager.kt app/src/test/java/com/cruise/assistant/domain/PosterManagerTest.kt
git commit -m "feat(youth): register 5 youth and kids club posters in PosterManager"
```

---

### Task 3: 升级版本号至 v1.10.11，全量构建并在模拟器验证

**Files:**
- Modify: `app/build.gradle.kts`

- [ ] **Step 1: 更新版本号**

在 `app/build.gradle.kts`：
- `versionCode = 37`
- `versionName = "1.10.11"`

- [ ] **Step 2: 运行全量单元测试与打包**

```bash
.\gradlew.bat testDebugUnitTest
.\gradlew.bat assembleDebug
```

- [ ] **Step 3: 安装到模拟器并在真机上验证设施指南的青少年活动卡片**

```bash
.\gradlew.bat installDebug
```

- [ ] **Step 4: Commit**

```bash
git add app/build.gradle.kts
git commit -m "chore(release): bump version to v1.10.11 (versionCode 37)"
```

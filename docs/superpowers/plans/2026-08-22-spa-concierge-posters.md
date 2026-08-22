# 4 处水疗与礼宾服务设施底图集成实施计划 (v1.10.12)

> **For agentic workers:** REQUIRED SUB-SKILL: Use superpowers:subagent-driven-development (recommended) or superpowers:executing-plans to implement this plan task-by-task. Steps use checkbox (`- [ ]`) syntax for tracking.

**Goal:** 将 `地点库/` 中的 4 张水疗与礼宾设施照片优化转换至 `app/src/main/assets/posters/facilities/`，在 `PosterManager.kt` 中完整注册，适配设施指南与日程弹窗，并升级版本号至 `v1.10.12`。

**Architecture:**
1. 预处理脚本将 `地点库/` 的 4 张图片优化转换为 1280px / 1000px 宽高质量 JPG，存放于 `app/src/main/assets/posters/facilities/`；
2. 在 `PosterManager.kt` 中注册 4 处设施的中英文全称与核心关键词；
3. 更新 `PosterManagerTest.kt`，增加 4 处设施的路径解析单元测试；
4. 升级版本号至 `v1.10.12` (`versionCode = 38`)，构建并安装到模拟器完成实机验证。

**Tech Stack:** Kotlin, Jetpack Compose, Android Assets, BitmapFactory, JUnit4, Python PIL.

## Global Constraints
- Target version: `v1.10.12` (`versionCode = 38`).
- Zero compile/test warnings or errors.

---

### Task 1: 图片优化转换与 Asset 归档

**Files:**
- Create: `app/src/main/assets/posters/facilities/infinite_bliss_spa.jpg`
- Create: `app/src/main/assets/posters/facilities/opulence_spa.jpg`
- Create: `app/src/main/assets/posters/facilities/concierge_lounge.jpg`
- Create: `app/src/main/assets/posters/facilities/concierge_sundeck.jpg`

- [ ] **Step 1: 运行 Python 脚本生成 4 张优化 JPG**

```bash
python -c "
import os
from PIL import Image

out_dir = 'app/src/main/assets/posters/facilities'
os.makedirs(out_dir, exist_ok=True)

mapping = {
    'Infinite Bliss Spa - Elemis at Sea.jfif': 'infinite_bliss_spa.jpg',
    'Opulence Spa - Elemis at Sea.jfif': 'opulence_spa.jpg',
    '礼宾休息室.jfif': 'concierge_lounge.jpg',
    'Concierge Sundeck.jfif': 'concierge_sundeck.jpg'
}

for src_name, dst_name in mapping.items():
    src_p = os.path.join('地点库', src_name)
    dst_p = os.path.join(out_dir, dst_name)
    im = Image.open(src_p).convert('RGB')
    target_w = 1280 if im.size[0] >= 1280 else im.size[0]
    im.resize((target_w, int(target_w * im.size[1] / im.size[0])), Image.Resampling.LANCZOS).save(dst_p, quality=88, optimize=True)
    print(f'{dst_name}: {os.path.getsize(dst_p)} bytes')
"
```

- [ ] **Step 2: Commit**

```bash
git add app/src/main/assets/posters/facilities/infinite_bliss_spa.jpg app/src/main/assets/posters/facilities/opulence_spa.jpg app/src/main/assets/posters/facilities/concierge_lounge.jpg app/src/main/assets/posters/facilities/concierge_sundeck.jpg
git commit -m "feat(assets): add 4 spa and concierge facility poster images"
```

---

### Task 2: 在 `PosterManager` 中注册 4 处设施并编写测试

**Files:**
- Modify: `app/src/main/java/com/cruise/assistant/domain/PosterManager.kt`
- Modify: `app/src/test/java/com/cruise/assistant/domain/PosterManagerTest.kt`

- [ ] **Step 1: 编写 `PosterManagerTest.kt` 单元测试**

在 `PosterManagerTest.kt` 中添加：

```kotlin
    @Test
    fun getFacilityPosterPath_spaAndConcierge_matchCorrectly() {
        assertEquals("posters/facilities/infinite_bliss_spa.jpg", PosterManager.getFacilityPosterPath("无限幸福水疗 Infinite Bliss Spa - Elemis at Sea"))
        assertEquals("posters/facilities/opulence_spa.jpg", PosterManager.getFacilityPosterPath("奢华水疗（礼宾） Opulence Spa - Elemis at Sea"))
        assertEquals("posters/facilities/concierge_lounge.jpg", PosterManager.getFacilityPosterPath("礼宾休息室 Concierge Lounge"))
        assertEquals("posters/facilities/concierge_lounge.jpg", PosterManager.getFacilityPosterPath("礼宾户外休息室 Concierge Lounge Outdoor"))
        assertEquals("posters/facilities/concierge_sundeck.jpg", PosterManager.getFacilityPosterPath("礼宾阳光甲板 Concierge Sundeck"))
    }
```

- [ ] **Step 2: 运行测试验证失败**

```bash
.\gradlew.bat testDebugUnitTest --tests "com.cruise.assistant.domain.PosterManagerTest"
```

- [ ] **Step 3: 在 `PosterManager.kt` 中注册 4 处设施**

在 `facilityPosterRegistry` 与 `eventPosterRegistry` 中加入 4 处水疗礼宾设施映射。

- [ ] **Step 4: 运行单元测试验证通过**

```bash
.\gradlew.bat testDebugUnitTest
```

- [ ] **Step 5: Commit**

```bash
git add app/src/main/java/com/cruise/assistant/domain/PosterManager.kt app/src/test/java/com/cruise/assistant/domain/PosterManagerTest.kt
git commit -m "feat(facilities): register spa and concierge posters in PosterManager"
```

---

### Task 3: 升级版本号至 v1.10.12，全量构建并在模拟器验证

**Files:**
- Modify: `app/build.gradle.kts`

- [ ] **Step 1: 更新版本号**

在 `app/build.gradle.kts`：
- `versionCode = 38`
- `versionName = "1.10.12"`

- [ ] **Step 2: 运行全量单元测试与打包**

```bash
.\gradlew.bat testDebugUnitTest
.\gradlew.bat assembleDebug
```

- [ ] **Step 3: 安装到模拟器并在真机上验证设施指南的船上服务卡片**

```bash
.\gradlew.bat installDebug
```

- [ ] **Step 4: Commit**

```bash
git add app/build.gradle.kts
git commit -m "chore(release): bump version to v1.10.12 (versionCode 38)"
```

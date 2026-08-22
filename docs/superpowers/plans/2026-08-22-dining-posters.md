# 22 处新增餐饮与客房送餐底图集成实施计划 (v1.10.9)

> **For agentic workers:** REQUIRED SUB-SKILL: Use superpowers:subagent-driven-development (recommended) or superpowers:executing-plans to implement this plan task-by-task. Steps use checkbox (`- [ ]`) syntax for tracking.

**Goal:** 将 `餐饮图库/` 中的 22 张高清实景/渲染图优化保存至 `app/src/main/assets/posters/dining/`，在 `PosterManager.kt` 中完整注册 22 处餐饮及客房送餐场所，适配设施指南与日程弹窗，并将版本号升级至 `v1.10.9`。

**Architecture:**
1. 预处理脚本将 `餐饮图库/` 的 22 张图片（含 JFIF、WEBP、AVIF）统一转换为优化后的 1280px 宽高质量 JPG，存放于 `app/src/main/assets/posters/dining/`；
2. 在 `PosterManager.kt` 中注册全部 22 处餐饮的中英文全称与核心关键词；
3. 更新 `PosterManagerTest.kt`，增加 22 处餐饮场所的路径解析单元测试；
4. 升级版本号至 `v1.10.9` (`versionCode = 35`)，编译并在模拟器上完成实机测试。

**Tech Stack:** Kotlin, Jetpack Compose, Android Assets, BitmapFactory, JUnit4, Python PIL / pillow-avif-plugin.

## Global Constraints
- Target version: `v1.10.9` (`versionCode = 35`).
- Output directory: `app/src/main/assets/posters/dining/`.
- Handle zero-width space in filename `Stitch's Ohana Grill\u200b.jfif`.
- Zero compile/test warnings or errors.

---

### Task 1: 餐饮图片优化转换与 Asset 归档

**Files:**
- Create: 22 files under `app/src/main/assets/posters/dining/*.jpg`

- [ ] **Step 1: 运行 Python 脚本生成 22 张优化 JPG**

```bash
python -c "
import os
from PIL import Image

out_dir = 'app/src/main/assets/posters/dining'
os.makedirs(out_dir, exist_ok=True)

for f in sorted(os.listdir('餐饮图库')):
    src_p = os.path.join('餐饮图库', f)
    # clean name
    clean_name = f.replace('\u200b', '').split('.')[0]
    # format output filename
    # e.g. Mike & Sulley's Flavors of Asia -> mike_sulleys_flavors_of_asia.jpg
    slug = clean_name.lower().replace('&', 'and').replace('\'', '').replace(' ', '_').replace('é', 'e')
    # special cleanup for multiple underscores
    slug = '_'.join([part for part in slug.split('_') if part])
    dst_name = slug + '.jpg'
    dst_p = os.path.join(out_dir, dst_name)
    im = Image.open(src_p).convert('RGB')
    im.resize((1280, int(1280 * im.size[1] / im.size[0])), Image.Resampling.LANCZOS).save(dst_p, quality=88, optimize=True)
    print(f'{f} -> {dst_name}: {os.path.getsize(dst_p)} bytes')
"
```

- [ ] **Step 2: 验证 22 个餐饮资产文件生成成功**

```bash
python -c "
import os
files = sorted(os.listdir('app/src/main/assets/posters/dining'))
print('Count:', len(files))
assert len(files) == 22
"
```

- [ ] **Step 3: Commit**

```bash
git add app/src/main/assets/posters/dining/
git commit -m "feat(assets): add 22 optimized poster images for dining and room service"
```

---

### Task 2: 在 `PosterManager` 中注册 22 处餐饮并编写测试

**Files:**
- Modify: `app/src/main/java/com/cruise/assistant/domain/PosterManager.kt`
- Modify: `app/src/test/java/com/cruise/assistant/domain/PosterManagerTest.kt`

- [ ] **Step 1: 编写 `PosterManagerTest.kt` 单元测试**

在 `PosterManagerTest.kt` 中添加对全部 22 处餐饮与客房送餐的测试：

```kotlin
    @Test
    fun getFacilityPosterPath_all22DiningVenues_matchCorrectly() {
        assertEquals("posters/dining/alley_cat_cafe.jpg", PosterManager.getFacilityPosterPath("流浪猫咖啡馆 Alley Cat Café"))
        assertEquals("posters/dining/bacha_coffee.jpg", PosterManager.getFacilityPosterPath("巴哈咖啡 Bacha Coffee"))
        assertEquals("posters/dining/bewitching_boba_and_brews.jpg", PosterManager.getFacilityPosterPath("迷魂珍珠奶茶 Bewitching Boba & Brews"))
        assertEquals("posters/dining/buccaneer_bar.jpg", PosterManager.getFacilityPosterPath("海盗酒吧 Buccaneer Bar"))
        assertEquals("posters/dining/cosmic_kebabs.jpg", PosterManager.getFacilityPosterPath("宇宙烤肉串 Cosmic Kebabs"))
        assertEquals("posters/dining/garden_bar.jpg", PosterManager.getFacilityPosterPath("花园酒吧 Garden Bar"))
        assertEquals("posters/dining/gramma_talas_kitchen.jpg", PosterManager.getFacilityPosterPath("塔拉奶奶的厨房 Gramma Talas Kitchen"))
        assertEquals("posters/dining/infinity_bar.jpg", PosterManager.getFacilityPosterPath("无边际酒吧 Infinity Bar"))
        assertEquals("posters/dining/market_bar.jpg", PosterManager.getFacilityPosterPath("市场酒吧 Market Bar"))
        assertEquals("posters/dining/mike_and_sulleys_flavors_of_asia.jpg", PosterManager.getFacilityPosterPath("怪兽亚洲风味 Mike & Sulley's Flavors of Asia"))
        assertEquals("posters/dining/mowglis_eatery.jpg", PosterManager.getFacilityPosterPath("莫格利餐厅 Mowgli's Eatery"))
        assertEquals("posters/dining/palo_cafe.jpg", PosterManager.getFacilityPosterPath("帕洛咖啡 Palo Café"))
        assertEquals("posters/dining/palo_trattoria.jpg", PosterManager.getFacilityPosterPath("帕洛意大利餐厅 Palo Trattoria"))
        assertEquals("posters/dining/pixar_market_restaurant.jpg", PosterManager.getFacilityPosterPath("皮克斯市场餐厅 Pixar Market Restaurant"))
        assertEquals("posters/dining/room_service.jpg", PosterManager.getFacilityPosterPath("客房送餐 Room Service"))
        assertEquals("posters/dining/spellbound.jpg", PosterManager.getFacilityPosterPath("咒语酒廊 Spellbound"))
        assertEquals("posters/dining/stitchs_ohana_grill.jpg", PosterManager.getFacilityPosterPath("史迪奇奥哈纳烧烤 Stitch's Ohana Grill"))
        assertEquals("posters/dining/twg_tea.jpg", PosterManager.getFacilityPosterPath("TWG 茶室 TWG Tea"))
        assertEquals("posters/dining/taverna_portorosso.jpg", PosterManager.getFacilityPosterPath("波尔托罗索小酒馆 Taverna Portorosso"))
        assertEquals("posters/dining/tianas_bayou_lounge.jpg", PosterManager.getFacilityPosterPath("蒂安娜河口酒廊 Tiana's Bayou Lounge"))
        assertEquals("posters/dining/wayfinder_bar.jpg", PosterManager.getFacilityPosterPath("寻路人酒吧 Wayfinder Bar"))
        assertEquals("posters/dining/wheezys_freezies.jpg", PosterManager.getFacilityPosterPath("威希冰品 Wheezy's Freezies"))
    }
```

- [ ] **Step 2: 运行测试验证失败**

```bash
.\gradlew.bat testDebugUnitTest --tests "com.cruise.assistant.domain.PosterManagerTest"
```

- [ ] **Step 3: 在 `PosterManager.kt` 中注册 22 处餐饮**

在 `facilityPosterRegistry` 与 `eventPosterRegistry` 中加入 22 处餐饮映射。

- [ ] **Step 4: 运行单元测试验证通过**

```bash
.\gradlew.bat testDebugUnitTest
```

- [ ] **Step 5: Commit**

```bash
git add app/src/main/java/com/cruise/assistant/domain/PosterManager.kt app/src/test/java/com/cruise/assistant/domain/PosterManagerTest.kt
git commit -m "feat(dining): register 22 dining and room service posters in PosterManager"
```

---

### Task 3: 升级版本号至 v1.10.9，全量构建并在模拟器验证

**Files:**
- Modify: `app/build.gradle.kts`

- [ ] **Step 1: 更新版本号**

在 `app/build.gradle.kts`：
- `versionCode = 35`
- `versionName = "1.10.9"`

- [ ] **Step 2: 运行全量单元测试与打包**

```bash
.\gradlew.bat testDebugUnitTest
.\gradlew.bat assembleDebug
```

- [ ] **Step 3: 安装到模拟器并在真机上验证设施指南的餐食/酒吧卡片**

```bash
.\gradlew.bat installDebug
```

- [ ] **Step 4: Commit**

```bash
git add app/build.gradle.kts
git commit -m "chore(release): bump version to v1.10.9 (versionCode 35)"
```

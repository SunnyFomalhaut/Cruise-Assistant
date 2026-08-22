# 5 处青少年与儿童活动空间底图集成设计规格 (v1.10.11)

## 1. 概述与目标

为迪士尼探险号（Disney Adventure）全部 5 处青少年与儿童活动空间（Youth & Kids Clubs）集成专属官方实景与渲染剧照：
1. 将 `地点库/` 目录中的 5 张高清照片优化转换保存至 `assets/posters/youth/`；
2. 在 `PosterManager.kt` 中注册全部 5 处青少年场所的中英文全称与核心关键词；
3. 支持「船上设施指南」列表卡片（水平渐变）与「日程定稿点击弹窗」（竖向渐变）；
4. 版本号升级为 **`v1.10.11`** (`versionCode = 37`)。

---

## 2. 青少年空间图片资产清单

| 序号 | 场所名称 | 来源图库文件 | 目标 Asset 路径 |
| :--- | :--- | :--- | :--- |
| 1 | **小小世界托儿所 it's a small world nursery** | `it’s a small world” nursery.avif` | `posters/youth/its_a_small_world_nursery.jpg` |
| 2 | **米奇米妮船长甲板 Mickey & Minnie Captain's Deck** | `Mickey & Minnie Captain’s Deck.jfif` | `posters/youth/mickey_minnie_captains_deck.jpg` |
| 3 | **迪士尼海洋探险家俱乐部 Disney's Oceaneer Club** | `Disney's Oceaneer Club.webp` | `posters/youth/disneys_oceaneer_club.jpg` |
| 4 | **边缘少年俱乐部 Edge** | `Edge.jfif` | `posters/youth/edge.jpg` |
| 5 | **活力少年俱乐部 Vibe** | `Vibe.webp` | `posters/youth/vibe.jpg` |

---

## 3. `PosterManager` 注册与匹配

在 `PosterManager.kt` 中：
- `facilityPosterRegistry` 中完整注册 5 处青少年场所的中英文名称；
- `eventPosterRegistry` 中同步注册青少年场所名称，确保以俱乐部名命名的日程事件也能加载专属底图。

---

## 4. 验证与发布

1. **单元测试 (`PosterManagerTest.kt`)**：覆盖 5 处青少年场所的路径查找断言；
2. **模拟器真机验证**：验证设施指南「青少年活动」分类卡片及日程定稿弹窗卡片背景。

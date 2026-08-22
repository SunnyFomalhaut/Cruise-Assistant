# 8部预置演出专属卡片背景与渐变渲染设计规格 (v1.10.7)

## 1. 概述与目标

在日程定稿模式下，点击主界面任意演出任务卡片会弹出事件详情卡片（`EventDetailDialog`）。此前部分演出使用通用剧场/设施照片（如花园舞台、漫威庭园）作为底图。
本次更新将全面升级为 **8 部预置演出专属实景剧照**，并应用半透明竖向渐变融合效果：
1. 将 `演出库/` 中的 8 部官方演出实景照片优化并归档至 `assets/posters/shows/`；
2. 针对《狮子王烟花》照片进行左上烟花取景裁切，确保在卡片弹窗中清晰明确地展现左上角盛开的烟花与下方舞台；
3. 升级 `PosterManager`，支持 `presetKey`、中英文全称及关键词多维度精确/模糊匹配，无论系统预置还是用户通过下拉列表/手动添加的演出，定稿后点击均能展示专属渐变背景；
4. 版本号升级为 **`v1.10.7`** (`versionCode = 33`)。

---

## 2. 演出图片资产清单与优化策略

将 `演出库/` 图片转换并归档至 `app/src/main/assets/posters/shows/`，保持清晰度（约 1200~1600px 宽）的同时控制体积在合理范围（约 100~250KB/张，总计 < 1.5MB）：

| 文件名 | 对应演出 | 来源原图 | 裁切与处理重点 |
| :--- | :--- | :--- | :--- |
| `show_set_sail.jpg` | **扬帆起航 Let's Set Sail** | `Let's Set Sail.jfif` | 优化为标准 JPG，增强舞台色彩饱和度 |
| `show_lion_king.jpg` | **狮子王烟花 The Lion King: Celebration in the Sky** | `The Lion King Celebration in the Sky.jfif` | **重点裁切**：左上方绚丽烟花 + 舞台主体（取景偏左偏上），避免弹窗居中裁切丢失烟花 |
| `show_seas_adventure.jpg` | **海洋奇遇 Disney Seas The Adventure** | `Disney Seas The Adventure.jfif` | 优化为标准 JPG，展现室内剧院宏大舞台 |
| `show_remember.jpg` | **回忆：瓦力与伊芙 Remember** | `Remember.jfif` | 优化为标准 JPG，展现瓦力与伊芙唯美舞台 |
| `show_moana.jpg` | **海洋奇缘 Moana: Call of the Sea** | `Moana Call of the Sea.jfif` | 优化为标准 JPG，展现寻路者湾水上舞台 |
| `show_avengers.jpg` | **复仇者集结 Avengers Assemble!** | `Avengers Assemble.jfif` | 优化为标准 JPG，展现漫威英雄舞台战斗特效 |
| `show_baymax_expo.jpg` | **大白超级健身世博会 Baymax Super Exercise Expo** | `Baymax Super Exercise Expo.jfif` | 优化为标准 JPG，展现大白舞台活力场景 |
| `show_duffy_friend_ship.jpg` | **达菲与友谊之船 Duffy and The Friend Ship** | `Duffy and The Friend Ship.png` | 转换优化为 JPG，展现达菲与朋友们在剧院舞台 |

---

## 3. `PosterManager` 演出底图匹配机制

### 3.1 独立演出海报注册表
在 `PosterManager.kt` 中维护专属 `showPosterRegistry`（或统一在 `eventPosterRegistry` 中），优先级高于设施底图：

```kotlin
// 映射示例
"show_set_sail" to "posters/shows/show_set_sail.jpg",
"show_lion_king" to "posters/shows/show_lion_king.jpg",
"show_seas_adventure" to "posters/shows/show_seas_adventure.jpg",
"show_remember" to "posters/shows/show_remember.jpg",
"show_moana" to "posters/shows/show_moana.jpg",
"show_avengers" to "posters/shows/show_avengers.jpg",
"show_baymax_expo" to "posters/shows/show_baymax_expo.jpg",
"show_duffy_friend_ship" to "posters/shows/show_duffy_friend_ship.jpg",
```

### 3.2 多维度命中策略
`loadEventPoster(context, eventTitle, presetKey)`：
1. **PresetKey 优先**：若 `presetKey != null`，直接查找匹配；
2. **演出完整标题 / 英文标题** 精确查找；
3. **中文核心关键词**（如 `"狮子王"`, `"烟花"`, `"大白"`, `"健身世博会"`, `"达菲"`, `"友谊之船"`, `"瓦力"`, `"回忆"`, `"海洋奇遇"`, `"海洋奇缘"`, `"复仇者"`, `"扬帆起航"`）包含匹配；
4. **英文核心关键词**（如 `"lion king"`, `"baymax"`, `"duffy"`, `"remember"`, `"seas the adventure"`, `"moana"`, `"avengers"`, `"set sail"`）包含匹配；
5. 若演出海报均未命中，且不是内置演出，才回退至地点设施海报（`loadFacilityPoster`）。

---

## 4. 视觉与 UI 效果（EventDetailDialog）

- 在 `EventDetailDialog.kt` 中：
  - 加载 `posterBitmap = PosterManager.loadEventPoster(context, event.title, event.presetKey)`；
  - 渐变渲染：顶部海报图以 `ContentScale.Crop` 呈现，通过竖向渐变蒙版（`Brush.verticalGradient`，透明度从顶部 0.35f 平滑过渡到中间 0.12f 直至底部 0f 纯白/底色），使得演出实景剧照与卡片文字排版完美融合，保证文字高对比度可读性。

---

## 5. 测试与验证计划

1. **单元测试 (`PosterManagerTest.kt`)**：
   - 验证全部 8 部演出的 `presetKey` 均可精确加载对应海报；
   - 验证 8 部演出的全称标题、中文核心词、英文核心词均可正确定位对应海报；
   - 验证不再错误匹配为旧的通用设施海报（例如 `moana` 不再映射到 `wayfinder_bay.jpg`，`duffy` 不再映射到 `garden_stage.jpg`）。
2. **模拟器真机验证**：
   - 查看定稿日程中的 8 部演出点击卡片；
   - 验证狮子王烟花卡片在弹窗中能清晰看到左上角的烟花与舞台；
   - 验证通过预置下拉新建的《达菲与友谊之船》、《大白超级健身世博会》等加入日程后，定稿点击弹窗拥有专属渐变海报。

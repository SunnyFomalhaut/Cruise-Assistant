# 14 处商店底图集成与全场景适配设计规格 (v1.10.8)

## 1. 概述与目标

为迪士尼探险号（Disney Adventure）全部 14 处商店集成官方专属渲染剧照，适配「船上设施指南」列表卡片（水平渐变）与「日程定稿点击弹窗」（竖向渐变）：
1. 将 `商店参考图/` 目录中的 13 张高清实景/渲染图优化保存至 `assets/posters/shops/`；
2. 在 `PosterManager.kt` 中注册 14 处商店的中英文全称与核心关键词；
3. 支持「船上设施指南」与「日程管理弹窗」多维度命中与渐变融合；
4. 版本号升级为 **`v1.10.8`** (`versionCode = 34`)。

---

## 2. 商店图片资产映射清单

| 序号 | 商店名称 | 来源文件 | 目标 Asset 路径 |
| :--- | :--- | :--- | :--- |
| 1 | **三个愿望 3 Wishes** | `3 Wishes.jpg` | `posters/shops/3_wishes.jpg` |
| 2 | **魔法变身精品店 Bibbidi Bobbidi Boutique** | `Bibbidi Bobbidi Boutique.jpg` | `posters/shops/bibbidi_bobbidi_boutique.jpg` |
| 3 | **城堡典藏 Castle Collection** | `Castle Collection.jfif` | `posters/shops/castle_collection.jpg` |
| 4 | **钻石与愿望 Diamonds and Wishes** | `Diamonds and Wishes.jfif` | `posters/shops/diamonds_and_wishes.jpg` |
| 5 | **迪士尼影楼 Disney Studio** | `Disney Studio & Royal Studio.jfif` | `posters/shops/disney_royal_studio.jpg` |
| 6 | **皇家影楼 Royal Studio** | `Disney Studio & Royal Studio.jfif` | `posters/shops/disney_royal_studio.jpg` |
| 7 | **达菲与朋友商店 Duffy and Friends Shop** | `Duffy and Friends Shop.jpg` | `posters/shops/duffy_and_friends_shop.jpg` |
| 8 | **漫威造型工作室 Marvel Style Studio** | `Marvel Style Studio.jfif` | `posters/shops/marvel_style_studio.jpg` |
| 9 | **国家地理 National Geographic** | `National Geographic.jfif` | `posters/shops/national_geographic.jpg` |
| 10 | **宫廷珍宝 Palace Treasures** | `Palace Treasures.jfif` | `posters/shops/palace_treasures.jpg` |
| 11 | **快照摄影店 Pics Photo Shop** | `Pics Photo Shop.jfif` | `posters/shops/pics_photo_shop.jpg` |
| 12 | **未诉珍宝 Treasures Untold** | `Treasures Untold.jfif` | `posters/shops/treasures_untold.jpg` |
| 13 | **迪士尼世界 World of Disney** | `World of Disney.jfif` | `posters/shops/world_of_disney.jpg` |
| 14 | **迪士尼世界二号店 World of Disney Too** | `World of Disney Too.jfif` | `posters/shops/world_of_disney_too.jpg` |

---

## 3. `PosterManager` 注册与匹配

在 `PosterManager.kt` 中：
- `facilityPosterRegistry` 增加 14 处商店全称与关键词映射；
- `eventPosterRegistry` 增加对商店名称的映射（便于以商店名称作为事件标题时的弹窗匹配）；
- `getFacilityPosterPath` 与 `getEventPosterPath` 支持精确命中与别名模糊命中。

---

## 4. 测试与验证计划

1. **单元测试 (`PosterManagerTest.kt`)**：
   - 验证全部 14 处商店名称均能正确返回对应的 `posters/shops/*.jpg` 路径；
   - 验证 `Disney Studio` 与 `Royal Studio` 均映射至 `disney_royal_studio.jpg`；
   - 验证无假阳性匹配。
2. **模拟器真机验证**：
   - 进入「船上设施指南」，切换至「商店」分类，验证 14 处商店卡片的左侧渐变底图；
   - 创建或点击商店类事件，验证定稿弹窗中的竖向渐变底图。

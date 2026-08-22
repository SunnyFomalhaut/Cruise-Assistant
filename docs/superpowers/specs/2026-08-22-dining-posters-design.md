# 22 处新增餐饮与客房送餐底图集成设计规格 (v1.10.9)

## 1. 概述与目标

为迪士尼探险号（Disney Adventure）全部 22 处未收录底图的餐厅、酒吧、小食店及全天候客房送餐（Room Service）集成专属官方实景与渲染剧照：
1. 将 `餐饮图库/` 目录中的 22 张高清实景照片优化转换保存至 `assets/posters/dining/`；
2. 在 `PosterManager.kt` 中注册全部 22 处餐饮的中英文全称与核心关键词；
3. 支持「船上设施指南」列表卡片（水平渐变）与「日程定稿点击弹窗」（竖向渐变）；
4. 版本号升级为 **`v1.10.9`** (`versionCode = 35`)。

---

## 2. 餐饮图片资产清单

| 序号 | 餐饮场所名称 | 来源图库文件 | 目标 Asset 路径 |
| :--- | :--- | :--- | :--- |
| 1 | **流浪猫咖啡馆 Alley Cat Café** | `Alley Cat Café.jfif` | `posters/dining/alley_cat_cafe.jpg` |
| 2 | **巴哈咖啡 Bacha Coffee** | `Bacha Coffee.jfif` | `posters/dining/bacha_coffee.jpg` |
| 3 | **迷魂珍珠奶茶 Bewitching Boba & Brews** | `Bewitching Boba & Brews.jfif` | `posters/dining/bewitching_boba_brews.jpg` |
| 4 | **海盗酒吧 Buccaneer Bar** | `Buccaneer Bar.jfif` | `posters/dining/buccaneer_bar.jpg` |
| 5 | **宇宙烤肉串 Cosmic Kebabs** | `Cosmic Kebabs.jfif` | `posters/dining/cosmic_kebabs.jpg` |
| 6 | **花园酒吧 Garden Bar** | `Garden Bar.jfif` | `posters/dining/garden_bar.jpg` |
| 7 | **塔拉奶奶的厨房 Gramma Talas Kitchen** | `Gramma Talas Kitchen.jfif` | `posters/dining/gramma_talas_kitchen.jpg` |
| 8 | **无边际酒吧 Infinity Bar** | `Infinity Bar.jfif` | `posters/dining/infinity_bar.jpg` |
| 9 | **市场酒吧 Market Bar** | `Market Bar.jfif` | `posters/dining/market_bar.jpg` |
| 10 | **怪兽亚洲风味 Mike & Sulley's Flavors of Asia** | `Mike & Sulley's Flavors of Asia.jfif` | `posters/dining/mike_sulleys_flavors_of_asia.jpg` |
| 11 | **莫格利餐厅 Mowgli's Eatery** | `Mowgli's Eatery.jfif` | `posters/dining/mowglis_eatery.jpg` |
| 12 | **帕洛咖啡 Palo Café** | `Palo Café.jfif` | `posters/dining/palo_cafe.jpg` |
| 13 | **帕洛意大利餐厅 Palo Trattoria** | `Palo Trattoria.jfif` | `posters/dining/palo_trattoria.jpg` |
| 14 | **皮克斯市场餐厅 Pixar Market Restaurant** | `Pixar Market Restaurant.jfif` | `posters/dining/pixar_market_restaurant.jpg` |
| 15 | **客房送餐 Room Service** | `Room Service.webp` | `posters/dining/room_service.jpg` |
| 16 | **咒语酒廊 Spellbound** | `Spellbound.jfif` | `posters/dining/spellbound.jpg` |
| 17 | **史迪奇奥哈纳烧烤 Stitch's Ohana Grill** | `Stitch's Ohana Grill​.jfif` | `posters/dining/stitchs_ohana_grill.jpg` |
| 18 | **TWG 茶室 TWG Tea** | `TWG Tea.jfif` | `posters/dining/twg_tea.jpg` |
| 19 | **波尔托罗索小酒馆 Taverna Portorosso** | `Taverna Portorosso.jfif` | `posters/dining/taverna_portorosso.jpg` |
| 20 | **蒂安娜河口酒廊 Tiana's Bayou Lounge** | `Tiana's Bayou Lounge.jfif` | `posters/dining/tianas_bayou_lounge.jpg` |
| 21 | **寻路人酒吧 Wayfinder Bar** | `Wayfinder Bar.jfif` | `posters/dining/wayfinder_bar.jpg` |
| 22 | **威希冰品 Wheezy's Freezies** | `Wheezy's Freezies.avif` | `posters/dining/wheezys_freezies.jpg` |

---

## 3. `PosterManager` 注册与匹配

在 `PosterManager.kt` 中：
- `facilityPosterRegistry` 中完整注册 22 处餐饮场所的中英文名称；
- `eventPosterRegistry` 中同步注册餐饮场所名称，确保以餐厅名命名的日程事件也能加载专属底图。

---

## 4. 验证与发布

1. **单元测试 (`PosterManagerTest.kt`)**：覆盖 22 处餐饮场所的路径查找断言；
2. **模拟器真机验证**：验证设施指南「餐食」和「酒吧」分类卡片及日程定稿弹窗卡片背景。

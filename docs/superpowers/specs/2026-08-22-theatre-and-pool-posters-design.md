# 华特迪士尼剧院与无边际泳池底图集成设计规格 (v1.10.10)

## 1. 概述与目标

为迪士尼探险号（Disney Adventure）新增两处重要设施的专属实景底图：
1. **华特迪士尼剧院（Walt Disney Theatre）**：从根目录 `Walt Disney Theatre.jfif` 优化转换至 `assets/posters/facilities/walt_disney_theatre.jpg`；
2. **无边际泳池（Infinity Pool）**：从根目录 `infinity pool.jfif` 优化转换至 `assets/posters/facilities/infinity_pool.jpg`；
3. **严格防混淆与隔离设计**：
   - 剧院内各大演出（《海洋奇遇》、《回忆：瓦力与伊芙》、《达菲与友谊之船》等）保持使用各自专属演出海报，不被剧院设施底图覆盖；
   - 《无边际泳池 Infinity Pool》（`infinity_pool.jpg`）与《无边际酒吧 Infinity Bar》（`infinity_bar.jpg`）严格隔离，互不混淆；
4. 版本号升级为 **`v1.10.10`** (`versionCode = 36`)。

---

## 2. 资产存储与映射

| 设施名称 | 来源文件 | 目标 Asset 路径 |
| :--- | :--- | :--- |
| **华特迪士尼剧院 Walt Disney Theatre** | `Walt Disney Theatre.jfif` | `posters/facilities/walt_disney_theatre.jpg` |
| **无边际泳池 Infinity Pool** | `infinity pool.jfif` | `posters/facilities/infinity_pool.jpg` |

---

## 3. `PosterManager` 匹配策略与测试验证

- `facilityPosterRegistry` 中添加：
  - `"华特迪士尼剧院"`, `"walt disney theatre"`, `"walt disney theater"` ➔ `"posters/facilities/walt_disney_theatre.jpg"`
  - `"无边际泳池"`, `"infinity pool"` ➔ `"posters/facilities/infinity_pool.jpg"`
- 单元测试验证：
  - `PosterManager.getFacilityPosterPath("华特迪士尼剧院 Walt Disney Theatre") == "posters/facilities/walt_disney_theatre.jpg"`
  - `PosterManager.getFacilityPosterPath("无边际泳池 Infinity Pool") == "posters/facilities/infinity_pool.jpg"`
  - `PosterManager.getFacilityPosterPath("无边际酒吧 Infinity Bar") == "posters/dining/infinity_bar.jpg"`
  - `PosterManager.getEventPosterPath("海洋奇遇 Disney Seas The Adventure", "show_seas_adventure") == "posters/shows/show_seas_adventure.jpg"`（演出优先）

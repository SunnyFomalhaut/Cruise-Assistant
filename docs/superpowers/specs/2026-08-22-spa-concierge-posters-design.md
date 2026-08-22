# 4 处水疗与礼宾服务设施底图集成设计规格 (v1.10.12)

## 1. 概述与目标

为迪士尼探险号（Disney Adventure）新增 4 处高端水疗沙龙与礼宾专属设施的实景剧照底图：
1. **无限幸福水疗（Infinite Bliss Spa - Elemis at Sea）** ➔ `assets/posters/facilities/infinite_bliss_spa.jpg`；
2. **奢华水疗（礼宾）（Opulence Spa - Elemis at Sea）** ➔ `assets/posters/facilities/opulence_spa.jpg`；
3. **礼宾休息室 / 礼宾户外休息室（Concierge Lounge / Outdoor）** ➔ `assets/posters/facilities/concierge_lounge.jpg`；
4. **礼宾阳光甲板（Concierge Sundeck）** ➔ `assets/posters/facilities/concierge_sundeck.jpg`；
5. 在 `PosterManager.kt` 中注册上述 4 处设施全称与别名；
6. 版本号升级为 **`v1.10.12`** (`versionCode = 38`)。

---

## 2. 资产存储清单

| 设施名称 | 来源文件 | 目标 Asset 路径 |
| :--- | :--- | :--- |
| **无限幸福水疗 Infinite Bliss Spa - Elemis at Sea** | `Infinite Bliss Spa - Elemis at Sea.jfif` | `posters/facilities/infinite_bliss_spa.jpg` |
| **奢华水疗（礼宾） Opulence Spa - Elemis at Sea** | `Opulence Spa - Elemis at Sea.jfif` | `posters/facilities/opulence_spa.jpg` |
| **礼宾休息室 Concierge Lounge** | `礼宾休息室.jfif` | `posters/facilities/concierge_lounge.jpg` |
| **礼宾户外休息室 Concierge Lounge Outdoor** | `礼宾休息室.jfif` | `posters/facilities/concierge_lounge.jpg` |
| **礼宾阳光甲板 Concierge Sundeck** | `Concierge Sundeck.jfif` | `posters/facilities/concierge_sundeck.jpg` |

---

## 3. `PosterManager` 匹配策略与测试验证

- `facilityPosterRegistry` 与 `eventPosterRegistry` 添加相应条目；
- 单元测试验证：
  - `PosterManager.getFacilityPosterPath("无限幸福水疗 Infinite Bliss Spa - Elemis at Sea") == "posters/facilities/infinite_bliss_spa.jpg"`
  - `PosterManager.getFacilityPosterPath("奢华水疗（礼宾） Opulence Spa - Elemis at Sea") == "posters/facilities/opulence_spa.jpg"`
  - `PosterManager.getFacilityPosterPath("礼宾休息室 Concierge Lounge") == "posters/facilities/concierge_lounge.jpg"`
  - `PosterManager.getFacilityPosterPath("礼宾户外休息室 Concierge Lounge Outdoor") == "posters/facilities/concierge_lounge.jpg"`
  - `PosterManager.getFacilityPosterPath("礼宾阳光甲板 Concierge Sundeck") == "posters/facilities/concierge_sundeck.jpg"`

# 需求设计文档：卡片萌趣人偶分级自适应与纯右侧点缀（v1.10.14）

## 1. 概述与目标

基于上一版本的实践与实际视觉效果反馈，本版本（v1.10.14）对日程时间轴主界面的卡片人偶点缀系统进行重构与升级：

### 核心设计变更
1. **纯右侧单偶点缀（Right-Aligned Single Doll）**：
   - 移除左下角的副偶，仅在卡片**右侧**展示单个人偶水印，保持日程表左侧文字与结构绝对清晰整洁。
2. **全员均匀随机（Uniform Random Only）**：
   - 彻底移除颜色偏好加权机制，达菲家族 7 位宝宝（达菲、雪莉玫、星黛露、玲娜贝儿、杰拉多尼、可琦安、奥乐米拉）以完全相等的概率均匀随机出场，打造丰富欢快的聚会氛围。
3. **三级形态资产分级（Head / Half-Body / Full-Body Tiers）**：
   - 60 张素材按人体截取范围重组分级：
     - **大头照 (head)**：10 张（纯头部/面部特写）
     - **半身照 (half_body)**：7 张（胸部以上或手部互动特写）
     - **全身照 (full_body)**：43 张（整身站立、全身坐姿、全身跳跃）
   - 根目录 `人偶素材/` 与内置 `assets/dolls/` 统一按三级子目录归档。
4. **卡片高度空间自适应算法（Height-Adaptive Tiering）**：
   - **紧凑短卡片 ($H_{card} < 44\text{dp}$)**：优先分配 **大头照 (head)**（如 30-40dp 的安全演习、简短娱乐等）。
   - **中等卡片 ($44\text{dp} \le H_{card} < 72\text{dp}$)**：优先分配 **半身照 (half_body)**（若无则回退至大头照或紧凑全身照，如 45-60dp 见面会等）。
   - **大型/长卡片 ($H_{card} \ge 72\text{dp}$)**：分配 **全身照 (full_body)**（如 75dp+ 晚餐、大型剧场演出等）。
   - **最大化高度自适应填充**：人偶高度紧密贴合卡片可用高度（最大至 120dp），在右侧尽显呆萌。
5. **版本号升级**：
   - `versionCode = 40`, `versionName = "1.10.14"`

---

## 2. 素材资产全量三级归档清单

| 角色 | 大头照 (`head/`) | 半身照 (`half_body/`) | 全身照 (`full_body/`) |
| :--- | :--- | :--- | :--- |
| **达菲 (duffy)** | `十月素材-星黛露-P2-9.png`<br>`十月素材-星黛露-P2-7.png` | *(回退使用 head/full)* | `十月素材-达菲家族-P3-8.png`<br>`十月素材-达菲家族-P3-6.png`<br>`十月素材-达菲家族-P3-21.png`<br>`十月素材-达菲家族-P3-19.png`<br>`十月素材-达菲家族-P3-17.png`<br>`十月素材-星黛露-P5-68.png`<br>`十月素材-星黛露-P2-31.png` |
| **雪莉玫 (shelliemay)** | `十月素材-达菲家族-P5-21.png`<br>`十月素材-星黛露-P2-10.png` | *(回退使用 head/full)* | `十月素材-达菲家族-P5-31.png`<br>`十月素材-达菲家族-P5-29.png`<br>`十月素材-达菲家族-P4-32.png`<br>`十月素材-达菲家族-P2-4.png`<br>`十月素材-达菲家族-P2-38.png`<br>`十月素材-达菲家族-P2-27.png`<br>`十月素材-星黛露-P2-43.png`<br>`十月素材-星黛露-P2-37.png` |
| **星黛露 (stellalou)** | `十月素材-达菲家族-P4-12.png` | *(回退使用 head/full)* | `十月素材-达菲家族-P4-28.png`<br>`十月素材-达菲家族-P4-25.png`<br>`十月素材-达菲家族-P4-20.png`<br>`十月素材-达菲家族-P4-15.png`<br>`十月素材-达菲家族-P4-11.png`<br>`十月素材-达菲家族-P3-47.png`<br>`十月素材-星黛露-P2-4.png`<br>`十月素材-星黛露-P2-24.png`<br>`十月素材-星黛露-P2-17.png` |
| **玲娜贝儿 (linabell)** | `十月素材-玲娜贝儿-P1-34.png`<br>`十月素材-玲娜贝儿-P1-33.png` | `十月素材-玲娜贝儿-P1-7.png`<br>`十月素材-玲娜贝儿-P1-4.png` | `十月素材-玲娜贝儿-P2-9.png`<br>`十月素材-玲娜贝儿-P2-3.png`<br>`十月素材-玲娜贝儿-P1-51.png`<br>`十月素材-玲娜贝儿-P1-30.png`<br>`十月素材-玲娜贝儿-P1-28.png`<br>`十月素材-玲娜贝儿-P1-21.png`<br>`十月素材-玲娜贝儿-P1-20.png` |
| **杰拉多尼 (gelatoni)** | `十月素材-星黛露-P2-8.png` | `十月素材-达菲家族-P1-5.png` | `十月素材-星黛露-P5-67.png`<br>`十月素材-星黛露-P5-64.png`<br>`十月素材-星黛露-P2-35.png`<br>`十月素材-星黛露-P2-32.png` |
| **可琦安 (cookieann)** | `十月素材-可琦安-P2-5.png` | `十月素材-可琦安-P2-4.png`<br>`十月素材-可琦安-P2-11.png` | `十月素材-星黛露-P5-11.png`<br>`十月素材-星黛露-P5-10.png`<br>`十月素材-可琦安-P2-7.png`<br>`十月素材-可琦安-P2-1.png` |
| **奥乐米拉 (olumel)** | `十月素材-奥乐米拉-P2-28.png` | `十月素材-星黛露-P5-13.png`<br>`十月素材-奥乐米拉-P2-62.png` | `十月素材-奥乐米拉-P3-9.png`<br>`十月素材-奥乐米拉-P3-3.png`<br>`十月素材-奥乐米拉-P3-10.png`<br>`十月素材-奥乐米拉-P2-25.png` |

---

## 3. 架构与领域逻辑

### 3.1 `DollTier` 领域枚举
```kotlin
enum class DollTier(val folderName: String) {
    HEAD("head"),
    HALF_BODY("half_body"),
    FULL_BODY("full_body");

    companion object {
        fun forCardHeight(heightDp: Float): DollTier = when {
            heightDp < 44f -> HEAD
            heightDp < 72f -> HALF_BODY
            else -> FULL_BODY
        }
    }
}
```

### 3.2 `DollManager` 选择算法
```kotlin
fun selectDollForCard(
    eventId: Long,
    cardHeightDp: Float,
): String?
```
1. 确定期望分级：`val tier = DollTier.forCardHeight(cardHeightDp)`
2. 确定角色：从 7 个角色中由 `eventId` 派生伪随机均匀选取一个角色 `character`
3. 获取素材：优先从该角色的 `tier` 列表中选取；若该角色在对应 `tier` 无素材，则回退查找策略（`HALF_BODY` 回退至 `HEAD`，再回退至 `FULL_BODY`；`HEAD` 回退至 `HALF_BODY` 再至 `FULL_BODY`；`FULL_BODY` 回退至 `HALF_BODY` 再至 `HEAD`），确保 100% 稳定获得该角色的最佳素材。
4. 始终返回单一素材路径 `String?`。

### 3.3 `EventCard` 渲染重构
- 移除 Secondary Doll 相关代码与参数。
- 仅保留右侧 `Alignment.BottomEnd` 的 `Image`：
  - 动态计算高度：`val dollHeight = (heightDp - 4.dp).coerceIn(24.dp, 120.dp)`
  - 半透明度：$\alpha = 0.24f$
  - 靠右对齐，填满右侧空白空间。

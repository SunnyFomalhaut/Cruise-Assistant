# 仙尘发出/收到双向联动（v1.8.1）— 设计文档

日期：2026-08-15
状态：已与用户确认
前置：`2026-08-14-pixie-dust-design.md`（v1.8.0 仙尘功能）

## 背景与目标

v1.8.0 的发出/收到是两个独立列表。实际使用中它们天然联动：给某房间发了礼物 = 该房间的待回礼可以勾掉；勾掉回礼时往往当场就要回一份礼物出去。本次把两个方向打通，并加一个联动检视图，让用户一眼看清每个房间的往来平衡。

同时：Room schema JSON（2-5.json）已提交版本控制（commit 76fd722），AutoMigration 干净构建问题解决。

## 联动规则

### 方向一：发出 → 自动勾选收到

- **触发**：仅快捷录入（键盘"记一笔"）保存新发出记录时。编辑弹窗保存、删除等其它路径不触发。
- **匹配**：在收到记录里找 `deck`、`room` 都相同的记录，且 `replied == false`。
- **多条时**：只勾**收到时间最早**的一条（timestamp 最小）。
- **提示**：勾选成功后 toast"已自动勾选 11552 的回礼"（房间号动态）。
- **找不到**：静默，无提示。
- **防循环**：方向二创建的发出记录**不**触发本方向（联动单向终止，不会 A→B→A）。

### 方向二：勾选回礼 → 弹窗选等级 → 自动创建发出

- **触发点两处**（行为一致）：
  1. 收到列表行的"已回礼" Checkbox 勾选时
  2. 编辑弹窗内"已回礼" Checkbox 勾选时
- **弹窗（ReplyLevelDialog）**：
  - 标题显示房间号（如"11550 · 选择回礼等级"）
  - 等级按钮复用 `LevelChip`，**预选同级**（收到记录的等级；该等级已删则回落到列表第一个等级）
  - 主按钮"确认"：勾选 replied=true + 自动创建一条发出记录（同 deck/room、所选等级、levelNameSnapshot 快照、当前时间戳、note 空）
  - 次按钮"仅勾选"：只置 replied=true，不创建发出（回礼非礼物场景）
  - 取消（点外部/dismiss）：勾选状态回退，什么也不发生
- **取消勾选**（已回礼→未回礼）：不弹窗，正常取消；**不删除**已创建的发出记录（发出记录独立存在，用户可自己在发出列表删）。

### 联动检视图（LinkageViewDialog）

统计条（"发出 N 件"/"收到 N 件 · 待回礼 M"那一行）**最右侧**加一个小按钮（Link 图标），点击打开全屏对话框：

- **内容**：按房间配对，每个有发出或收到记录的房间一行：
  ```
  11550   发出 3（普通×2 豪华×1）  ↔  收到 2（精致×2）[待回礼 1]
  11134   发出 1（普通×1）        ↔  收到 0            —
  ```
  - 发出侧分等级计数；收到侧分等级计数，未回礼的存在行尾标"待回礼 N"（红色）
  - 等级显示名用快照兜底（等级已删显示原名）
- **排序**：
  1. 有未回礼收到的房间最前，按未回礼中最高等级 rank 降序（贵重待办优先）
  2. 其次不平衡房间（发出件数 ≠ 收到件数），按最近互动时间倒序
  3. 平衡房间沉底，按最近互动时间倒序
- **只读**：行不可点、无操作（保持简单）。

## 数据模型

无 schema 变更（v5 不动）。联动全部基于现有 `pixie_dust` 表数据派生。

## Domain 层（纯函数，JUnit 测试）

新增 `domain/PixieDustLinkage.kt`：

```kotlin
data class RoomPair(
    val deck: Int,
    val room: String,
    val given: List<PixieDustRecord>,     // 该房间全部发出
    val received: List<PixieDustRecord>,  // 该房间全部收到
    val unrepliedReceived: List<PixieDustRecord>,  // 未回礼的收到
    val lastInteractTs: Long,             // 双侧最大 timestamp
)

/** 方向一：发出记录联动勾选的目标——同房间未回礼中最早的收到记录；无则 null */
fun autoReplyTarget(received: List<PixieDustRecord>, deck: Int, room: String): PixieDustRecord?

/** 联动检视：按房间配对并按 待回礼贵重 > 不平衡 > 平衡 排序 */
fun pairByRoom(given: List<PixieDustRecord>, received: List<PixieDustRecord>): List<RoomPair>
```

`autoReplyTarget` 语义：`received.filter { 未回礼 && deck && room 匹配 }.minByOrNull { timestamp }`。

## ViewModel 变更

- `saveRecord()`：保存成功后查 `autoReplyTarget`，有则更新该记录 `replied=true` 并 toast 提示（新增一次性事件 `autoRepliedRoom: String?`，UI 显示后调 `onAutoReplyToastShown()` 清空）。
- `onToggleReplied(id, true)` 改为：置 `pendingReply: PixieDustRecord?`（供弹窗），**不立即**写库。UI 据此显示 `ReplyLevelDialog`。
- 新增 `confirmReply(record, level: PixieLevel?)`：`level == null` 表示"仅勾选"；写 `replied=true`，level 非空时再插入发出记录（**不触发**方向一联动）。
- 新增 `cancelReply()`：清 pendingReply，不写库（勾选回退）。
- 取消勾选路径 `onToggleReplied(id, false)`：直接写库，无弹窗。
- 编辑弹窗内勾选同样走 pendingReply 流（EventSheet 的 PixieDustEditDialog 把勾选事件上报为"请求勾选"而非直接改本地状态）。

## UI 变更

- 新建 `ui/pixiedust/ReplyLevelDialog.kt`：等级选择 + 确认/仅勾选/取消。
- 新建 `ui/pixiedust/LinkageViewDialog.kt`：只读配对列表（LazyColumn），行内分等级计数（复用/参照 `PixieDustLists.kt` 的 `levelNameOf`/`LevelDot`）。
- `PixieDustScreen` 统计条 Row 末尾加 IconButton（`Icons.Outlined.Link`，contentDescription"联动检视"）。
- `PixieDustLists.kt` ReceivedList 的 Checkbox `onCheckedChange` 改为调 `onRequestReply(rec)`（pending 流）；取消勾选仍直接 `onToggleReplied(rec.id, false)`。
- `PixieDustEditDialog.kt`："已回礼"Checkbox 勾选时上报请求（pending 流），取消勾选正常本地改。

## 错误处理

- 回礼弹窗打开时目标等级（收到的 levelId）已被删：预选回落到当前等级列表第一个。
- 联动检视空数据（无任何记录）：显示空态文案"还没有仙尘记录"。
- confirmReply 写发出时等级列表为空（理论不可达，UI 已有 size>1 保护）：跳过创建只勾选。

## 版本

versionCode 15，versionName "1.8.1"。

## 不做的事（YAGNI）

- 配对视图只读，不可编辑/跳转
- 取消回礼勾选不回收已创建的发出记录
- 不做历史数据的批量补联动（只对新操作生效）
- 不记录"发出记录是由回礼创建的"标记（发出记录之间无差别；如需区分可用 note）

## 测试

- domain：`autoReplyTarget`（多条取最早/无匹配 null/已回礼排除）、`pairByRoom`（排序三分组、配对完整性、单侧房间）
- ViewModel 逻辑以编译 + 手动回归为准（项目无 VM 测试基建，与 v1.8.0 一致）

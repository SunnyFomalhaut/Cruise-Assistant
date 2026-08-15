# 仙尘反向联动补全（v1.8.2）— 设计文档

日期：2026-08-16
状态：已与用户确认
前置：`2026-08-15-pixie-dust-linkage-design.md`（v1.8.1 双向联动）

## 背景与问题

v1.8.1 的联动只有"先收后发"方向：登记发出时自动勾选同房间最早的未回礼收到记录。

反过来的"先发后收"没有联动：用户先记录了发出 8234，之后登记收到 8234 时，新收到的记录仍是未回礼状态。结果联动检视图出现矛盾——"8234 发出 1 收到 1 [待回礼 1]"：回礼明明已经给过了（发出记录在先），却还提示待回礼。

## 方案

镜像补全反向联动：`saveRecord()` 保存 RECEIVED 记录时，若该房间已存在任意 GIVEN 记录，则把**刚登记的这条收到记录**直接置 `replied = true`，并 toast 提示。

语义：回礼动作（发出）在登记收到之前就已发生并记录，所以新登记的收到天然"已回礼"。

## 规则细节

- **触发**：仅快捷录入（键盘"记一笔"）保存新 RECEIVED 记录时。编辑弹窗保存、删除等其它路径不触发（与方向一一致）。
- **匹配**：同 `deck` + `room` 存在**任意** GIVEN 记录（不论条数）即触发。
- **勾选对象**：刚保存的这条 RECEIVED 记录自身（不是旧的未回礼记录）。
- **提示**：toast"已自动勾选 8234 的回礼"（复用现有 `pixie_auto_replied_format` 与 `autoRepliedRoom` 事件流）。
- **防循环**：终态操作——只置 `replied=true`，不创建任何新记录，天然无循环。
- **历史数据**：不批量修复。已存在的矛盾数据（如先发后收未勾的）由用户在收到页手动勾选（选"仅勾选"即可）。当前无实际用户，无需对账迁移。

## Domain 层

新纯函数（`domain/PixieDustLinkage.kt` 追加）：

```kotlin
/** 方向一镜像：该房间是否已有发出记录（登记收到时判断要不要自动勾自己） */
fun hasGivenForRoom(given: List<PixieDustRecord>, deck: Int, room: String): Boolean
```

语义：`given.any { it.direction == GIVEN && it.deck == deck && it.room == room }`。JUnit 测试覆盖：有/无匹配、不同楼层同房号不算、RECEIVED 记录不算。

## ViewModel 变更

`saveRecord()` 的 launch 块：现有 GIVEN 分支旁加对称的 RECEIVED 分支：

```kotlin
if (record.direction == PixieDustDirection.RECEIVED) {
    val all = dustRepository.observeAll().first()
    val hasGiven = hasGivenForRoom(
        all.filter { it.direction == PixieDustDirection.GIVEN },
        record.deck, record.room,
    )
    if (hasGiven) {
        dustRepository.save(record.copy(replied = true))
        autoRepliedRoom.value = record.fullRoom
    }
}
```

注意：保存 `record.copy(replied = true)` 时 `record.id == 0`（插入前对象）——直接 copy 后 save 会**再插一条**而不是更新刚插入的。正确做法：`dustRepository.save(record)` 的返回值就是新插入的 id，用它构造 `record.copy(id = newId, replied = true)` 再 save（update）。实现时先接住返回值。

## 不做的事（YAGNI）

- 不做历史矛盾数据的批量对账
- 不做"数量匹配才勾"的复杂规则（有发出就勾）
- 不改联动检视图（矛盾消除后自然平衡）

## 版本

versionCode 16，versionName "1.8.2"。

## 测试

- domain：`hasGivenForRoom` 3 个用例（命中/未命中/RECEIVED 不算+跨楼层不算）
- ViewModel 以编译 + 手动回归为准（与既有约定一致）

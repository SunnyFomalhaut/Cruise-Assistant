import SwiftUI

/// 编辑仙尘礼物记录弹窗
public struct PixieDustEditDialog: View {
    public let record: PixieDustRecord
    public let levels: [PixieLevel]
    public let availableDecks: [Int]
    public let onSave: (PixieDustRecord) -> Void
    public let onDelete: (PixieDustRecord) -> Void
    public let onDismiss: () -> Void

    @State private var selectedDeck: Int
    @State private var roomInput: String
    @State private var selectedLevelId: Int64
    @State private var noteInput: String
    @State private var isReplied: Bool
    @State private var showDeleteConfirmation: Bool = false

    public init(
        record: PixieDustRecord,
        levels: [PixieLevel],
        availableDecks: [Int] = Array(1...20),
        onSave: @escaping (PixieDustRecord) -> Void,
        onDelete: @escaping (PixieDustRecord) -> Void,
        onDismiss: @escaping () -> Void
    ) {
        self.record = record
        self.levels = levels
        self.availableDecks = availableDecks
        self.onSave = onSave
        self.onDelete = onDelete
        self.onDismiss = onDismiss

        _selectedDeck = State(initialValue: record.deck)
        _roomInput = State(initialValue: record.room)
        _selectedLevelId = State(initialValue: record.levelId)
        _noteInput = State(initialValue: record.note)
        _isReplied = State(initialValue: record.replied)
    }

    public var body: some View {
        NavigationStack {
            Form {
                // Direction & Stateroom Section
                Section(header: Text("房号信息")) {
                    HStack {
                        Text("记录类型")
                        Spacer()
                        Text(record.direction == .given ? "发出 (我给别间)" : "收到 (别间给我)")
                            .foregroundColor(record.direction == .given ? CruiseMarine : Slate600)
                            .font(.system(size: 14, weight: .semibold))
                    }

                    Picker("所在甲板", selection: $selectedDeck) {
                        ForEach(availableDecks, id: \.self) { deck in
                            Text("Deck \(deck)").tag(deck)
                        }
                    }

                    HStack {
                        Text("三位房号")
                        Spacer()
                        TextField("如 550", text: $roomInput)
#if os(iOS)
                            .keyboardType(.numberPad)
#endif
                            .multilineTextAlignment(.trailing)
                            .onChange(of: roomInput) { _, newValue in
                                let filtered = newValue.filter { $0.isNumber }
                                if filtered.count > 3 {
                                    roomInput = String(filtered.prefix(3))
                                } else {
                                    roomInput = filtered
                                }
                            }
                    }

                    HStack {
                        Text("完整房号")
                        Spacer()
                        Text("\(selectedDeck)\(roomInput)")
                            .font(.system(size: 15, weight: .bold, design: .rounded))
                            .foregroundColor(isValidRoomInput(roomInput) ? Slate900 : Slate400)
                    }
                }

                // Gift Level Section
                Section(header: Text("礼物等级")) {
                    ForEach(levels) { level in
                        HStack {
                            Circle()
                                .fill(Color(argb: level.colorArgb))
                                .frame(width: 10, height: 10)
                            Text(level.name)
                                .font(.system(size: 15, weight: .medium))
                                .foregroundColor(Slate900)

                            Spacer()

                            StarRatingView(
                                rating: level.rank,
                                maxStars: 3,
                                starSize: 14,
                                spacing: 2,
                                activeColor: Color(hex: "#F59E0B"),
                                inactiveColor: Slate200
                            )

                            if selectedLevelId == level.id {
                                Image(systemName: "checkmark")
                                    .foregroundColor(CruiseAzure)
                                    .font(.system(size: 14, weight: .bold))
                                    .padding(.leading, 8)
                            }
                        }
                        .contentShape(Rectangle())
                        .onTapGesture {
                            selectedLevelId = level.id
                        }
                    }
                }

                // Status & Note Section
                Section(header: Text("状态与备注")) {
                    if record.direction == .received {
                        Toggle(isOn: $isReplied) {
                            Text("已回礼")
                                .font(.system(size: 15))
                        }
                    }

                    TextField("添加备注（可选）", text: $noteInput)
                }

                // Delete Section
                Section {
                    Button(role: .destructive) {
                        showDeleteConfirmation = true
                    } label: {
                        HStack {
                            Spacer()
                            Image(systemName: "trash")
                            Text("删除此条记录")
                            Spacer()
                        }
                    }
                }
            }
            .navigationTitle("编辑记录")
#if os(iOS)
            .navigationBarTitleDisplayMode(.inline)
#endif
            .toolbar {
                ToolbarItem(placement: .cancellationAction) {
                    Button("取消", action: onDismiss)
                }
                ToolbarItem(placement: .confirmationAction) {
                    Button("保存", action: handleSave)
                        .disabled(!isValidRoomInput(roomInput))
                        .fontWeight(.bold)
                }
            }
            .alert("确认删除记录？", isPresented: $showDeleteConfirmation) {
                Button("取消", role: .cancel) {}
                Button("删除", role: .destructive) {
                    onDelete(record)
                    onDismiss()
                }
            } message: {
                Text("删除后将无法恢复该条记录。")
            }
        }
    }

    private func handleSave() {
        guard isValidRoomInput(roomInput) else { return }
        let selectedLevel = levels.first(where: { $0.id == selectedLevelId })
        let levelName = selectedLevel?.name ?? record.levelNameSnapshot

        let updated = PixieDustRecord(
            id: record.id,
            direction: record.direction,
            deck: selectedDeck,
            room: roomInput,
            levelId: selectedLevelId,
            levelNameSnapshot: levelName,
            replied: isReplied,
            note: noteInput.trimmingCharacters(in: .whitespacesAndNewlines),
            timestamp: record.timestamp
        )

        onSave(updated)
        onDismiss()
    }
}

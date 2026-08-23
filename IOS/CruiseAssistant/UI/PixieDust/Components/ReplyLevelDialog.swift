import SwiftUI

/// 仙尘回礼等级确认弹窗（勾选"已回礼"后两步流选择回礼等级）
public struct ReplyLevelDialog: View {
    public let record: PixieDustRecord
    public let levels: [PixieLevel]
    public let onConfirm: (PixieLevel?) -> Void
    public let onDismiss: () -> Void

    @State private var selectedLevelId: Int64?

    public init(
        record: PixieDustRecord,
        levels: [PixieLevel],
        onConfirm: @escaping (PixieLevel?) -> Void,
        onDismiss: @escaping () -> Void
    ) {
        self.record = record
        self.levels = levels
        self.onConfirm = onConfirm
        self.onDismiss = onDismiss

        // 默认预选同等级，如果该等级已被删则回落到第一个等级
        let defaultId = levels.first(where: { $0.id == record.levelId })?.id ?? levels.first?.id
        _selectedLevelId = State(initialValue: defaultId)
    }

    public var body: some View {
        NavigationStack {
            VStack(spacing: 20) {
                // Header info
                VStack(spacing: 6) {
                    Text("向 \(record.fullRoom) 回礼")
                        .font(.system(size: 20, weight: .bold, design: .rounded))
                        .foregroundColor(Slate900)
                    Text("选择送出的礼物等级，将自动创建一条发出记录")
                        .font(.system(size: 13))
                        .foregroundColor(Slate500)
                        .multilineTextAlignment(.center)
                }
                .padding(.top, 16)

                // Level Selection Cards
                VStack(spacing: 10) {
                    ForEach(levels) { level in
                        Button {
                            selectedLevelId = level.id
                        } label: {
                            HStack {
                                Circle()
                                    .fill(Color(argb: level.colorArgb))
                                    .frame(width: 12, height: 12)
                                Text(level.name)
                                    .font(.system(size: 16, weight: .medium))
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
                                    Image(systemName: "checkmark.circle.fill")
                                        .foregroundColor(CruiseAzure)
                                        .font(.system(size: 18))
                                        .padding(.leading, 6)
                                } else {
                                    Image(systemName: "circle")
                                        .foregroundColor(Slate300)
                                        .font(.system(size: 18))
                                        .padding(.leading, 6)
                                }
                            }
                            .padding(14)
                            .background(selectedLevelId == level.id ? CruiseAzureContainer.opacity(0.3) : Color.white)
                            .clipShape(RoundedRectangle(cornerRadius: 12, style: .continuous))
                            .overlay(
                                RoundedRectangle(cornerRadius: 12, style: .continuous)
                                    .stroke(selectedLevelId == level.id ? CruiseAzure : Slate200, lineWidth: selectedLevelId == level.id ? 1.5 : 1)
                            )
                        }
                        .buttonStyle(.plain)
                    }
                }
                .padding(.horizontal, 16)

                Spacer()

                // Actions
                VStack(spacing: 10) {
                    Button {
                        let selected = levels.first(where: { $0.id == selectedLevelId })
                        onConfirm(selected)
                    } label: {
                        Text("确认回礼并创建记录")
                            .font(.system(size: 16, weight: .bold))
                            .foregroundColor(.white)
                            .frame(maxWidth: .infinity)
                            .frame(height: 48)
                            .background(selectedLevelId != nil ? CruiseAzure : Slate300)
                            .clipShape(RoundedRectangle(cornerRadius: 12, style: .continuous))
                    }
                    .disabled(selectedLevelId == nil)

                    Button {
                        onConfirm(nil) // 仅勾选，不创建发出记录
                    } label: {
                        Text("仅标记已回礼（不新建记录）")
                            .font(.system(size: 14, weight: .medium))
                            .foregroundColor(Slate600)
                            .frame(maxWidth: .infinity)
                            .frame(height: 40)
                    }
                }
                .padding(.horizontal, 16)
                .padding(.bottom, 16)
            }
            .background(Slate50)
            .navigationTitle("回礼登记")
#if os(iOS)
            .navigationBarTitleDisplayMode(.inline)
#endif
            .toolbar {
                ToolbarItem(placement: .cancellationAction) {
                    Button("取消", action: onDismiss)
                }
            }
        }
    }
}

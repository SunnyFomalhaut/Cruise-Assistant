import SwiftUI

/// Sheet / Dialog presenting detected schedule conflicts before day finalization.
public struct ConflictDialogView: View {
    public let conflicts: [(first: CruiseEvent, second: CruiseEvent)]
    public let onGoFix: () -> Void
    public let onFinalize: () -> Void
    public let onDismiss: () -> Void

    public init(
        conflicts: [(first: CruiseEvent, second: CruiseEvent)],
        onGoFix: @escaping () -> Void,
        onFinalize: @escaping () -> Void,
        onDismiss: @escaping () -> Void
    ) {
        self.conflicts = conflicts
        self.onGoFix = onGoFix
        self.onFinalize = onFinalize
        self.onDismiss = onDismiss
    }

    private var hasConflicts: Bool {
        !conflicts.isEmpty
    }

    private func formatTimeSpan(event: CruiseEvent) -> String {
        let endMin = event.endMinute
        let end = String(format: "%02d:%02d", (endMin / 60) % 24, endMin % 60)
        return "\(formatStart(event: event))–\(end)"
    }

    public var body: some View {
        NavigationStack {
            VStack(spacing: 16) {
                if hasConflicts {
                    VStack(alignment: .leading, spacing: 8) {
                        HStack(spacing: 8) {
                            Image(systemName: "exclamationmark.triangle.fill")
                                .foregroundColor(.red)
                                .font(.system(size: 20))

                            Text("发现 \(conflicts.count) 处时间冲突")
                                .font(.system(size: 17, weight: .bold))
                                .foregroundColor(Slate900)
                        }

                        Text("以下活动在同一时间段内重叠，定稿后仅活动事件会保留在计划表中。建议先返回调整栏位或时间。")
                            .font(.system(size: 13))
                            .foregroundColor(Slate600)
                            .lineSpacing(2)
                    }
                    .padding(.horizontal, 16)
                    .padding(.top, 12)

                    ScrollView {
                        VStack(spacing: 10) {
                            ForEach(Array(conflicts.enumerated()), id: \.offset) { index, pair in
                                VStack(alignment: .leading, spacing: 6) {
                                    HStack {
                                        Text("冲突 #\(index + 1)")
                                            .font(.system(size: 11, weight: .bold))
                                            .foregroundColor(CruiseMarine)
                                            .padding(.horizontal, 6)
                                            .padding(.vertical, 2)
                                            .background(CruiseAzureContainer)
                                            .clipShape(RoundedRectangle(cornerRadius: 4, style: .continuous))

                                        Spacer()
                                    }

                                    HStack(alignment: .top, spacing: 8) {
                                        VStack(alignment: .leading, spacing: 2) {
                                            Text(pair.first.title)
                                                .font(.system(size: 13, weight: .semibold))
                                                .foregroundColor(Slate900)
                                            Text(formatTimeSpan(event: pair.first))
                                                .font(.system(size: 11))
                                                .foregroundColor(Slate500)
                                        }

                                        Image(systemName: "arrow.left.and.right")
                                            .font(.system(size: 11, weight: .bold))
                                            .foregroundColor(Slate400)
                                            .padding(.top, 4)

                                        VStack(alignment: .leading, spacing: 2) {
                                            Text(pair.second.title)
                                                .font(.system(size: 13, weight: .semibold))
                                                .foregroundColor(Slate900)
                                            Text(formatTimeSpan(event: pair.second))
                                                .font(.system(size: 11))
                                                .foregroundColor(Slate500)
                                        }
                                    }
                                }
                                .padding(12)
                                .frame(maxWidth: .infinity, alignment: .leading)
                                .background(SurfaceCard)
                                .clipShape(RoundedRectangle(cornerRadius: 10, style: .continuous))
                                .overlay(
                                    RoundedRectangle(cornerRadius: 10, style: .continuous)
                                        .stroke(Color.red.opacity(0.2), lineWidth: 1)
                                )
                            }
                        }
                        .padding(.horizontal, 16)
                    }

                    // Action buttons for conflict state
                    VStack(spacing: 10) {
                        Button(action: onGoFix) {
                            Text("返回修改")
                                .font(.system(size: 15, weight: .bold))
                                .foregroundColor(.white)
                                .frame(maxWidth: .infinity)
                                .frame(height: 44)
                                .background(CruiseAzure)
                                .clipShape(RoundedRectangle(cornerRadius: 10, style: .continuous))
                        }

                        Button(action: onFinalize) {
                            Text("仍然定稿")
                                .font(.system(size: 14, weight: .medium))
                                .foregroundColor(Slate600)
                        }
                    }
                    .padding(.horizontal, 16)
                    .padding(.bottom, 16)
                } else {
                    // No conflicts state
                    VStack(spacing: 16) {
                        Spacer()

                        Image(systemName: "checkmark.circle.fill")
                            .font(.system(size: 48))
                            .foregroundColor(Color(hex: "#10B981"))

                        Text("无时间冲突")
                            .font(.system(size: 18, weight: .bold))
                            .foregroundColor(Slate900)

                        Text("当天所有活动时间均无重叠，可以放心生成计划表。")
                            .font(.system(size: 14))
                            .foregroundColor(Slate600)
                            .multilineTextAlignment(.center)
                            .padding(.horizontal, 24)

                        Spacer()

                        VStack(spacing: 10) {
                            Button(action: onFinalize) {
                                Text("生成计划表")
                                    .font(.system(size: 15, weight: .bold))
                                    .foregroundColor(.white)
                                    .frame(maxWidth: .infinity)
                                    .frame(height: 44)
                                    .background(CruiseAzure)
                                    .clipShape(RoundedRectangle(cornerRadius: 10, style: .continuous))
                            }

                            Button(action: onDismiss) {
                                Text("取消")
                                    .font(.system(size: 14, weight: .medium))
                                    .foregroundColor(Slate600)
                            }
                        }
                        .padding(.horizontal, 16)
                        .padding(.bottom, 16)
                    }
                }
            }
            .background(Slate50.ignoresSafeArea())
            .navigationTitle(hasConflicts ? "时间冲突" : "确认定稿")
#if os(iOS)
            .navigationBarTitleDisplayMode(.inline)
#endif
            .toolbar {
                ToolbarItem(placement: .cancellationAction) {
                    Button("关闭") {
                        onDismiss()
                    }
                }
            }
        }
    }
}

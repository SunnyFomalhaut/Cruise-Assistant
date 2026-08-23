import SwiftUI

/// Onboarding wizard screen guiding the user through initial voyage setup.
public struct OnboardingView: View {
    @ObservedObject public var viewModel: OnboardingViewModel
    public let onComplete: (() -> Void)?

    @State private var activeDateSheet: ActiveDatePicker? = nil

    private enum ActiveDatePicker: Identifiable {
        case board
        case disembark

        var id: Int {
            switch self {
            case .board: return 1
            case .disembark: return 2
            }
        }
    }

    public init(
        viewModel: OnboardingViewModel,
        onComplete: (() -> Void)? = nil
    ) {
        self.viewModel = viewModel
        self.onComplete = onComplete
    }

    public var body: some View {
        ScrollView {
            VStack(spacing: 16) {
                // MARK: - Hero Banner
                heroBanner

                // MARK: - Card 1: Ship & Schedule
                shipAndScheduleCard

                // MARK: - Card 2: Dates & Boarding
                datesAndBoardingCard

                // MARK: - Card 3: Deck Configuration
                deckConfigurationCard

                // MARK: - Error Message
                if let error = viewModel.errorMessage {
                    HStack(spacing: 6) {
                        Image(systemName: "exclamationmark.triangle.fill")
                            .foregroundColor(NowRed)
                        Text(error)
                            .font(.footnote)
                            .foregroundColor(NowRed)
                        Spacer()
                    }
                    .padding(.horizontal, 4)
                }

                // MARK: - Submit Button
                submitButton
            }
            .padding(.horizontal, 16)
            .padding(.vertical, 20)
        }
        .background(Slate50.ignoresSafeArea())
        .sheet(item: $activeDateSheet) { target in
            datePickerSheet(for: target)
        }
    }

    // MARK: - Hero Banner

    private var heroBanner: some View {
        HStack(spacing: 16) {
            ZStack {
                Circle()
                    .fill(CruiseAzure)
                    .frame(width: 48, height: 48)
                Image(systemName: "ferry.fill")
                    .resizable()
                    .scaledToFit()
                    .frame(width: 26, height: 26)
                    .foregroundColor(.white)
            }

            VStack(alignment: .leading, spacing: 4) {
                Text("欢迎登上邮轮助手")
                    .font(.headline)
                    .fontWeight(.bold)
                    .foregroundColor(CruiseMarine)

                Text("设置您的航程基本信息，我们将自动为您生成专属时间轴。")
                    .font(.caption)
                    .foregroundColor(Slate600)
                    .fixedSize(horizontal: false, vertical: true)
            }

            Spacer()
        }
        .padding(16)
        .background(
            RoundedRectangle(cornerRadius: 16)
                .fill(CruiseAzureContainer)
        )
    }

    // MARK: - Card 1: Ship & Schedule

    private var shipAndScheduleCard: some View {
        VStack(alignment: .leading, spacing: 14) {
            // Header
            HStack(spacing: 8) {
                Image(systemName: "sailboat.fill")
                    .foregroundColor(CruiseAzure)
                Text("船只与船期")
                    .font(.headline)
                    .foregroundColor(Slate900)
            }

            // Ship Selector
            VStack(alignment: .leading, spacing: 6) {
                Text("选择船只")
                    .font(.subheadline)
                    .foregroundColor(Slate700)

                Menu {
                    ForEach(BUILT_IN_SHIPS.indices, id: \.self) { index in
                        Button(BUILT_IN_SHIPS[index].name) {
                            viewModel.onShipSelected(index)
                        }
                    }
                    Button("自定义船只") {
                        viewModel.onShipSelected(-1)
                    }
                } label: {
                    HStack {
                        Text(viewModel.isBuiltInShip ? viewModel.shipName : (viewModel.shipName.isEmpty ? "自定义船只" : viewModel.shipName))
                            .foregroundColor(Slate900)
                            .fontWeight(.medium)
                        Spacer()
                        Image(systemName: "chevron.up.chevron.down")
                            .font(.caption)
                            .foregroundColor(Slate500)
                    }
                    .padding(.horizontal, 12)
                    .padding(.vertical, 10)
                    .background(
                        RoundedRectangle(cornerRadius: 10)
                            .stroke(Slate200, lineWidth: 1)
                            .background(RoundedRectangle(cornerRadius: 10).fill(Color.white))
                    )
                }
            }

            // Custom Ship Name Field
            if !viewModel.isBuiltInShip {
                VStack(alignment: .leading, spacing: 6) {
                    Text("输入船名")
                        .font(.subheadline)
                        .foregroundColor(Slate700)

                    TextField("如：海洋光谱号", text: Binding(
                        get: { viewModel.shipName },
                        set: { viewModel.onShipNameChange($0) }
                    ))
                    .padding(.horizontal, 12)
                    .padding(.vertical, 10)
                    .background(
                        RoundedRectangle(cornerRadius: 10)
                            .stroke(Slate200, lineWidth: 1)
                            .background(RoundedRectangle(cornerRadius: 10).fill(Color.white))
                    )
                }
            }

            // Preset Schedules (for built-in ship)
            if viewModel.isBuiltInShip {
                VStack(alignment: .leading, spacing: 8) {
                    Text("预置船期")
                        .font(.subheadline)
                        .foregroundColor(Slate700)

                    let ship = BUILT_IN_SHIPS[viewModel.selectedShipIndex]
                    HStack(spacing: 8) {
                        ForEach(ship.schedules.indices, id: \.self) { idx in
                            let schedule = ship.schedules[idx]
                            let isSelected = viewModel.selectedScheduleIndex == idx
                            Button(action: {
                                viewModel.onScheduleSelected(idx)
                            }) {
                                Text(schedule.label)
                                    .font(.subheadline)
                                    .fontWeight(isSelected ? .bold : .medium)
                                    .foregroundColor(isSelected ? CruiseMarine : Slate700)
                                    .frame(maxWidth: .infinity)
                                    .padding(.vertical, 8)
                                    .background(
                                        RoundedRectangle(cornerRadius: 8)
                                            .fill(isSelected ? CruiseAzureContainer : Slate100)
                                    )
                                    .overlay(
                                        RoundedRectangle(cornerRadius: 8)
                                            .stroke(isSelected ? CruiseAzure : Color.clear, lineWidth: 1)
                                    )
                            }
                            .buttonStyle(PlainButtonStyle())
                        }

                        // Custom Schedule Button
                        let isCustomSchedule = viewModel.selectedScheduleIndex < 0
                        Button(action: {
                            viewModel.onScheduleSelected(-1)
                        }) {
                            Text("自定义")
                                .font(.subheadline)
                                .fontWeight(isCustomSchedule ? .bold : .medium)
                                .foregroundColor(isCustomSchedule ? CruiseMarine : Slate700)
                                .padding(.horizontal, 12)
                                .padding(.vertical, 8)
                                .background(
                                    RoundedRectangle(cornerRadius: 8)
                                        .fill(isCustomSchedule ? CruiseAzureContainer : Slate100)
                                )
                                .overlay(
                                    RoundedRectangle(cornerRadius: 8)
                                        .stroke(isCustomSchedule ? CruiseAzure : Color.clear, lineWidth: 1)
                                )
                        }
                        .buttonStyle(PlainButtonStyle())
                    }
                }
            }

            // Dinner Time Selection
            if viewModel.isBuiltInShip && viewModel.selectedScheduleIndex >= 0 {
                VStack(alignment: .leading, spacing: 8) {
                    Text("晚餐时段")
                        .font(.subheadline)
                        .foregroundColor(Slate700)

                    HStack(spacing: 8) {
                        let earlyDinner = SimpleTime(hour: 17, minute: 45)
                        let lateDinner = SimpleTime(hour: 20, minute: 15)

                        dinnerButton(time: earlyDinner, label: "早场 (17:45)")
                        dinnerButton(time: lateDinner, label: "晚场 (20:15)")
                    }
                }
            }
        }
        .padding(16)
        .background(
            RoundedRectangle(cornerRadius: 16)
                .fill(Color.white)
                .shadow(color: Color.black.opacity(0.04), radius: 6, x: 0, y: 2)
        )
        .overlay(
            RoundedRectangle(cornerRadius: 16)
                .stroke(Slate200, lineWidth: 1)
        )
    }

    private func dinnerButton(time: SimpleTime, label: String) -> some View {
        let isSelected = viewModel.dinnerTime == time
        return Button(action: {
            viewModel.onDinnerTimeChange(time)
        }) {
            Text(label)
                .font(.subheadline)
                .fontWeight(isSelected ? .bold : .medium)
                .foregroundColor(isSelected ? CruiseMarine : Slate700)
                .frame(maxWidth: .infinity)
                .padding(.vertical, 8)
                .background(
                    RoundedRectangle(cornerRadius: 8)
                        .fill(isSelected ? CruiseAzureContainer : Slate100)
                )
                .overlay(
                    RoundedRectangle(cornerRadius: 8)
                        .stroke(isSelected ? CruiseAzure : Color.clear, lineWidth: 1)
                )
        }
        .buttonStyle(PlainButtonStyle())
    }

    // MARK: - Card 2: Dates & Boarding

    private var datesAndBoardingCard: some View {
        VStack(alignment: .leading, spacing: 14) {
            // Header
            HStack(spacing: 8) {
                Image(systemName: "calendar")
                    .foregroundColor(CruiseAzure)
                Text("航程时间与登船")
                    .font(.headline)
                    .foregroundColor(Slate900)
            }

            // Board Date Button
            VStack(alignment: .leading, spacing: 6) {
                Text("登船日期")
                    .font(.subheadline)
                    .foregroundColor(Slate700)

                Button(action: {
                    activeDateSheet = .board
                }) {
                    HStack {
                        Image(systemName: "calendar")
                            .foregroundColor(CruiseAzure)
                        Text(viewModel.boardDate.chineseFormatted)
                            .foregroundColor(Slate900)
                            .fontWeight(.medium)
                        Spacer()
                        Image(systemName: "chevron.right")
                            .font(.caption)
                            .foregroundColor(Slate400)
                    }
                    .padding(.horizontal, 12)
                    .padding(.vertical, 10)
                    .background(
                        RoundedRectangle(cornerRadius: 10)
                            .stroke(Slate200, lineWidth: 1)
                            .background(RoundedRectangle(cornerRadius: 10).fill(Color.white))
                    )
                }
                .buttonStyle(PlainButtonStyle())
            }

            // Board Time Slot Selection
            VStack(alignment: .leading, spacing: 8) {
                HStack {
                    Text("登船时间")
                        .font(.subheadline)
                        .foregroundColor(Slate700)
                    Spacer()
                    let endSlot = viewModel.boardTime.plusMinutes(15)
                    Text("登船批次：\(viewModel.boardTime.description) – \(endSlot.description)")
                        .font(.caption)
                        .fontWeight(.semibold)
                        .foregroundColor(CruiseMarine)
                }

                let slots = boardingSlots()
                LazyVGrid(columns: [GridItem(.flexible()), GridItem(.flexible()), GridItem(.flexible())], spacing: 8) {
                    ForEach(slots, id: \.self) { slot in
                        let isSelected = viewModel.boardTime == slot
                        Button(action: {
                            viewModel.onBoardTimeChange(slot)
                        }) {
                            Text(slot.description)
                                .font(.subheadline)
                                .fontWeight(isSelected ? .bold : .medium)
                                .foregroundColor(isSelected ? CruiseMarine : Slate700)
                                .frame(maxWidth: .infinity)
                                .padding(.vertical, 8)
                                .background(
                                    RoundedRectangle(cornerRadius: 8)
                                        .fill(isSelected ? CruiseAzureContainer : Slate100)
                                )
                                .overlay(
                                    RoundedRectangle(cornerRadius: 8)
                                        .stroke(isSelected ? CruiseAzure : Color.clear, lineWidth: 1)
                                )
                        }
                        .buttonStyle(PlainButtonStyle())
                    }
                }
            }

            // Disembark Date Button
            VStack(alignment: .leading, spacing: 6) {
                Text("下船日期")
                    .font(.subheadline)
                    .foregroundColor(Slate700)

                Button(action: {
                    activeDateSheet = .disembark
                }) {
                    HStack {
                        Image(systemName: "calendar")
                            .foregroundColor(CruiseAzure)
                        Text(viewModel.disembarkDate.chineseFormatted)
                            .foregroundColor(Slate900)
                            .fontWeight(.medium)
                        Spacer()
                        Image(systemName: "chevron.right")
                            .font(.caption)
                            .foregroundColor(Slate400)
                    }
                    .padding(.horizontal, 12)
                    .padding(.vertical, 10)
                    .background(
                        RoundedRectangle(cornerRadius: 10)
                            .stroke(Slate200, lineWidth: 1)
                            .background(RoundedRectangle(cornerRadius: 10).fill(Color.white))
                    )
                }
                .buttonStyle(PlainButtonStyle())

                Text("预计 07:00 开始离船")
                    .font(.caption)
                    .foregroundColor(Slate500)
            }
        }
        .padding(16)
        .background(
            RoundedRectangle(cornerRadius: 16)
                .fill(Color.white)
                .shadow(color: Color.black.opacity(0.04), radius: 6, x: 0, y: 2)
        )
        .overlay(
            RoundedRectangle(cornerRadius: 16)
                .stroke(Slate200, lineWidth: 1)
        )
    }

    // MARK: - Card 3: Deck Configuration

    private var deckConfigurationCard: some View {
        HStack {
            HStack(spacing: 8) {
                Image(systemName: "square.3.layers.3d.down.right.fill")
                    .foregroundColor(CruiseAzure)
                Text("甲板配置")
                    .font(.headline)
                    .foregroundColor(Slate900)
            }

            Spacer()

            if viewModel.isLockedDeck {
                Text("Deck \(viewModel.minDeck)–\(viewModel.maxDeck)")
                    .font(.subheadline)
                    .fontWeight(.bold)
                    .foregroundColor(CruiseMarine)
                    .padding(.horizontal, 10)
                    .padding(.vertical, 6)
                    .background(
                        RoundedRectangle(cornerRadius: 8)
                            .fill(CruiseAzureContainer)
                    )
            } else {
                HStack(spacing: 8) {
                    Button(action: {
                        viewModel.onMaxDeckChange(viewModel.maxDeck - 1)
                    }) {
                        Image(systemName: "minus")
                            .font(.subheadline)
                            .foregroundColor(viewModel.maxDeck > viewModel.minDeck ? Slate700 : Slate400)
                            .frame(width: 32, height: 32)
                            .background(Circle().fill(Slate100))
                    }
                    .disabled(viewModel.maxDeck <= viewModel.minDeck)

                    Text("Deck \(viewModel.maxDeck)")
                        .font(.subheadline)
                        .fontWeight(.bold)
                        .foregroundColor(Slate900)

                    Button(action: {
                        viewModel.onMaxDeckChange(viewModel.maxDeck + 1)
                    }) {
                        Image(systemName: "plus")
                            .font(.subheadline)
                            .foregroundColor(viewModel.maxDeck < 30 ? Slate700 : Slate400)
                            .frame(width: 32, height: 32)
                            .background(Circle().fill(Slate100))
                    }
                    .disabled(viewModel.maxDeck >= 30)
                }
            }
        }
        .padding(16)
        .background(
            RoundedRectangle(cornerRadius: 16)
                .fill(Color.white)
                .shadow(color: Color.black.opacity(0.04), radius: 6, x: 0, y: 2)
        )
        .overlay(
            RoundedRectangle(cornerRadius: 16)
                .stroke(Slate200, lineWidth: 1)
        )
    }

    // MARK: - Submit Button

    private var submitButton: some View {
        Button(action: {
            viewModel.submit {
                onComplete?()
            }
        }) {
            HStack(spacing: 8) {
                if viewModel.saving {
                    ProgressView()
                        .progressViewStyle(CircularProgressViewStyle(tint: .white))
                }
                Text("生成航程时间轴")
                    .font(.headline)
                    .fontWeight(.bold)
                    .foregroundColor(.white)
            }
            .frame(maxWidth: .infinity)
            .frame(height: 52)
            .background(
                RoundedRectangle(cornerRadius: 14)
                    .fill(CruiseAzure)
            )
        }
        .disabled(viewModel.saving)
        .padding(.top, 8)
    }

    // MARK: - Date Picker Sheet

    private func datePickerSheet(for target: ActiveDatePicker) -> some View {
        NavigationView {
            VStack {
                let binding = Binding<Date>(
                    get: {
                        switch target {
                        case .board:
                            return viewModel.boardDate.toDate()
                        case .disembark:
                            return viewModel.disembarkDate.toDate()
                        }
                    },
                    set: { newDate in
                        let simpleDate = SimpleDate(date: newDate)
                        switch target {
                        case .board:
                            viewModel.onBoardDateChange(simpleDate)
                        case .disembark:
                            viewModel.onDisembarkDateChange(simpleDate)
                        }
                    }
                )

                DatePicker(
                    "",
                    selection: binding,
                    displayedComponents: .date
                )
                .datePickerStyle(.graphical)
                .padding()

                Spacer()
            }
            .navigationTitle(target == .board ? "选择登船日期" : "选择下船日期")
            #if os(iOS)
            .navigationBarTitleDisplayMode(.inline)
            #endif
            .toolbar {
                ToolbarItem(placement: .confirmationAction) {
                    Button("完成") {
                        activeDateSheet = nil
                    }
                }
            }
        }
        .presentationDetents([.medium, .large])
    }
}

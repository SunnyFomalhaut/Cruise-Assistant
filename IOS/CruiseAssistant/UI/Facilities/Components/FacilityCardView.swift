import SwiftUI
import Foundation
#if canImport(UIKit)
import UIKit
public typealias FacilityPlatformImage = UIImage
#elseif canImport(AppKit)
import AppKit
public typealias FacilityPlatformImage = NSImage
#endif

extension Image {
    public init(facilityPlatformImage: FacilityPlatformImage) {
        #if canImport(UIKit)
        self.init(uiImage: facilityPlatformImage)
        #elseif canImport(AppKit)
        self.init(nsImage: facilityPlatformImage)
        #endif
    }
}

public func loadFacilityPosterImage(for placeName: String) -> FacilityPlatformImage? {
    guard let url = PosterManager.shared.posterUrl(for: placeName) else { return nil }
    #if canImport(UIKit)
    return UIImage(contentsOfFile: url.path)
    #elseif canImport(AppKit)
    return NSImage(contentsOf: url)
    #else
    return nil
    #endif
}

/// 设施标题解析结构体（拆分中英文双语）
public struct FacilityTitleParts: Equatable, Sendable {
    public let chinese: String
    public let english: String?

    public init(chinese: String, english: String? = nil) {
        self.chinese = chinese
        self.english = english
    }
}

/// 解析设施地点名称为中文与英文两部分
public func parseFacilityTitle(_ placeName: String) -> FacilityTitleParts {
    let trimmed = placeName.trimmingCharacters(in: .whitespacesAndNewlines)
    guard !trimmed.isEmpty else {
        return FacilityTitleParts(chinese: "", english: nil)
    }

    // 检查是否包含 CJK 字符
    let hasCJK = trimmed.unicodeScalars.contains { scalar in
        (0x4E00...0x9FFF).contains(scalar.value) ||
        (0x3400...0x4DBF).contains(scalar.value) ||
        (0x20000...0x2A6DF).contains(scalar.value)
    }

    if !hasCJK {
        return FacilityTitleParts(chinese: trimmed, english: nil)
    }

    // 按空格分词判断中英交界
    let words = trimmed.split(separator: " ")
    if words.count >= 2 {
        var cjkWords: [String] = []
        var latinWords: [String] = []
        var switchedToLatin = false

        for word in words {
            let containsCJK = word.unicodeScalars.contains { (0x4E00...0x9FFF).contains($0.value) }
            if containsCJK && !switchedToLatin {
                cjkWords.append(String(word))
            } else {
                switchedToLatin = true
                latinWords.append(String(word))
            }
        }

        if !cjkWords.isEmpty && !latinWords.isEmpty {
            return FacilityTitleParts(
                chinese: cjkWords.joined(separator: " "),
                english: latinWords.joined(separator: " ")
            )
        }
    }

    return FacilityTitleParts(chinese: trimmed, english: nil)
}

/// 获取 categoryKey 对应的标准 ARGB 颜色
public func categoryColorArgb(for categoryKey: String) -> Int64 {
    switch categoryKey {
    case "meal": return 0xFF34A853
    case "show": return 0xFFF5B301
    case "important": return 0xFFE8563F
    case "chore": return 0xFF4A7DFF
    case "fun": return 0xFF9B5DE5
    case "meetup": return 0xFF00A5A5
    case "pool": return 0xFF45C4B8
    case "shop": return 0xFFF2994A
    case "youth": return 0xFFA97BF0
    default: return 0xFF0284C7 // CruiseAzure
    }
}

/// 现代海报画廊卡片：展示地点海报、中英文名称、分类与甲板位置、营业状态
public struct FacilityCardView: View {
    public let facility: FacilityItemUiModel
    public let onTap: (() -> Void)?

    public init(
        facility: FacilityItemUiModel,
        onTap: (() -> Void)? = nil
    ) {
        self.facility = facility
        self.onTap = onTap
    }

    private var titleParts: FacilityTitleParts {
        parseFacilityTitle(facility.placeName)
    }

    private var posterImage: FacilityPlatformImage? {
        loadFacilityPosterImage(for: facility.placeName)
    }

    public var body: some View {
        Button(action: {
            onTap?()
        }) {
            VStack(alignment: .leading, spacing: 0) {
                // 1. 海报头部容器
                ZStack(alignment: .topTrailing) {
                    if let image = posterImage {
                        Image(facilityPlatformImage: image)
                            .resizable()
                            .aspectRatio(16 / 10, contentMode: .fill)
                            .frame(maxWidth: .infinity, maxHeight: 110)
                            .clipped()
                    } else {
                        ZStack {
                            LinearGradient(
                                colors: [
                                    Color(argb: categoryColorArgb(for: facility.categoryKey)).opacity(0.35),
                                    Color(argb: categoryColorArgb(for: facility.categoryKey)).opacity(0.12)
                                ],
                                startPoint: .topLeading,
                                endPoint: .bottomTrailing
                            )
                            Image(systemName: categoryIconSystemName(for: facility.categoryKey))
                                .font(.system(size: 32))
                                .foregroundColor(Color(argb: categoryColorArgb(for: facility.categoryKey)).opacity(0.55))
                        }
                        .frame(maxWidth: .infinity)
                        .frame(height: 110)
                    }

                    // 状态徽标 (右上角悬浮)
                    statusBadge
                        .padding(7)
                }

                // 2. 卡片内容详情
                VStack(alignment: .leading, spacing: 6) {
                    // 中英文标题
                    VStack(alignment: .leading, spacing: 1.5) {
                        Text(titleParts.chinese)
                            .font(.system(size: 13.5, weight: .bold))
                            .foregroundColor(Slate900)
                            .lineLimit(1)

                        if let english = titleParts.english {
                            Text(english)
                                .font(.system(size: 10, weight: .medium))
                                .foregroundColor(Slate500)
                                .lineLimit(1)
                        }
                    }

                    // 类别与餐饮子分组徽标行
                    HStack(spacing: 4) {
                        categoryBadge

                        if let dining = facility.diningGroupLabel {
                            Text(dining)
                                .font(.system(size: 9, weight: .medium))
                                .foregroundColor(Slate600)
                                .padding(.horizontal, 4)
                                .padding(.vertical, 2)
                                .background(Slate100)
                                .cornerRadius(4)
                                .lineLimit(1)
                        }
                        Spacer(minLength: 0)
                    }

                    // 位置徽标 ("Deck 6 船头")
                    HStack(spacing: 3) {
                        Image(systemName: "mappin.and.ellipse")
                            .font(.system(size: 9, weight: .semibold))
                            .foregroundColor(CruiseMarine)
                        Text(facility.formattedLocation)
                            .font(.system(size: 10, weight: .medium))
                            .foregroundColor(CruiseMarine)
                            .lineLimit(1)
                    }
                    .padding(.horizontal, 5)
                    .padding(.vertical, 2.5)
                    .background(CruiseAzureContainer.opacity(0.65))
                    .cornerRadius(5)

                    // 营业时间简要
                    if facility.is24Hours {
                        Text("24小时全天候")
                            .font(.system(size: 9.5, weight: .medium))
                            .foregroundColor(CruiseAzure)
                    } else if !facility.slots.isEmpty {
                        let slotsText = facility.slots.prefix(2).map { slot in
                            let mealLabel = slot.meal.map { "\($0.label) " } ?? ""
                            let time = String(format: "%02d:%02d–%02d:%02d", slot.start.hour, slot.start.minute, slot.end.hour, slot.end.minute)
                            return "\(mealLabel)\(time)"
                        }.joined(separator: ", ")
                        Text(slotsText)
                            .font(.system(size: 9.5, weight: .regular))
                            .foregroundColor(Slate600)
                            .lineLimit(1)
                    }
                }
                .padding(9)
            }
            .background(SurfaceCard)
            .cornerRadius(12)
            .overlay(
                RoundedRectangle(cornerRadius: 12)
                    .stroke(Slate200, lineWidth: 1)
            )
            .shadow(color: Color.black.opacity(0.04), radius: 3, x: 0, y: 1.5)
        }
        .buttonStyle(PlainButtonStyle())
    }

    // MARK: - Subviews

    @ViewBuilder
    private var statusBadge: some View {
        if facility.is24Hours {
            Text("24小时")
                .font(.system(size: 9, weight: .bold))
                .foregroundColor(CruiseMarine)
                .padding(.horizontal, 6)
                .padding(.vertical, 2.5)
                .background(CruiseAzureContainer)
                .cornerRadius(6)
                .shadow(color: Color.black.opacity(0.08), radius: 2, x: 0, y: 1)
        } else if facility.isOpenNow {
            Text("营业中")
                .font(.system(size: 9, weight: .bold))
                .foregroundColor(Color(hex: "#137333"))
                .padding(.horizontal, 6)
                .padding(.vertical, 2.5)
                .background(Color(hex: "#E6F4EA"))
                .cornerRadius(6)
                .shadow(color: Color.black.opacity(0.08), radius: 2, x: 0, y: 1)
        } else {
            Text("休息中")
                .font(.system(size: 9, weight: .bold))
                .foregroundColor(Slate500)
                .padding(.horizontal, 6)
                .padding(.vertical, 2.5)
                .background(Slate100.opacity(0.95))
                .cornerRadius(6)
                .shadow(color: Color.black.opacity(0.08), radius: 2, x: 0, y: 1)
        }
    }

    @ViewBuilder
    private var categoryBadge: some View {
        HStack(spacing: 3) {
            Image(systemName: categoryIconSystemName(for: facility.categoryKey))
                .font(.system(size: 8, weight: .semibold))
            Text(facility.categoryLabel)
                .font(.system(size: 9, weight: .semibold))
        }
        .padding(.horizontal, 5)
        .padding(.vertical, 2)
        .foregroundColor(cardTextColorFor(colorArgb: categoryColorArgb(for: facility.categoryKey)))
        .background(cardBackgroundFor(colorArgb: categoryColorArgb(for: facility.categoryKey)))
        .cornerRadius(5)
    }
}

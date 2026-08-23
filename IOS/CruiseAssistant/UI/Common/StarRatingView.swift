import SwiftUI

/// A 1 to 3 (or configurable max) star rating display and interactive rating picker.
public struct StarRatingView: View {
    public let rating: Int
    public let maxStars: Int
    public let starSize: CGFloat
    public let spacing: CGFloat
    public let activeColor: Color
    public let inactiveColor: Color
    public let isInteractive: Bool
    public let allowClear: Bool
    public let onRatingChanged: ((Int) -> Void)?

    private let bindingRating: Binding<Int>?

    /// Display-only initializer.
    public init(
        rating: Int,
        maxStars: Int = 3,
        starSize: CGFloat = 16,
        spacing: CGFloat = 4,
        activeColor: Color = Color(hex: "#F59E0B"),
        inactiveColor: Color = Slate200
    ) {
        self.rating = max(0, min(rating, maxStars))
        self.maxStars = maxStars
        self.starSize = starSize
        self.spacing = spacing
        self.activeColor = activeColor
        self.inactiveColor = inactiveColor
        self.isInteractive = false
        self.allowClear = false
        self.onRatingChanged = nil
        self.bindingRating = nil
    }

    /// Interactive rating picker initializer with a two-way Binding.
    public init(
        rating: Binding<Int>,
        maxStars: Int = 3,
        starSize: CGFloat = 22,
        spacing: CGFloat = 6,
        activeColor: Color = Color(hex: "#F59E0B"),
        inactiveColor: Color = Slate200,
        allowClear: Bool = false,
        onRatingChanged: ((Int) -> Void)? = nil
    ) {
        self.rating = max(0, min(rating.wrappedValue, maxStars))
        self.maxStars = maxStars
        self.starSize = starSize
        self.spacing = spacing
        self.activeColor = activeColor
        self.inactiveColor = inactiveColor
        self.isInteractive = true
        self.allowClear = allowClear
        self.onRatingChanged = onRatingChanged
        self.bindingRating = rating
    }

    public var body: some View {
        HStack(spacing: spacing) {
            ForEach(1...maxStars, id: \.self) { index in
                starButton(for: index)
            }
        }
    }

    @ViewBuilder
    private func starButton(for index: Int) -> some View {
        let isFilled = index <= currentRating
        let starImage = Image(systemName: isFilled ? "star.fill" : "star.fill")
            .resizable()
            .scaledToFit()
            .frame(width: starSize, height: starSize)
            .foregroundColor(isFilled ? activeColor : inactiveColor)

        if isInteractive {
            Button(action: {
                handleStarTap(index)
            }) {
                starImage
            }
            .buttonStyle(PlainButtonStyle())
            .accessibilityLabel("\(index) 星")
        } else {
            starImage
        }
    }

    private var currentRating: Int {
        bindingRating?.wrappedValue ?? rating
    }

    private func handleStarTap(_ index: Int) {
        guard isInteractive else { return }
        let newRating: Int
        if allowClear && currentRating == index {
            newRating = 0
        } else {
            newRating = index
        }

        bindingRating?.wrappedValue = newRating
        onRatingChanged?(newRating)
        HapticFeedbackService.shared.triggerSelection()
    }
}

#Preview {
    VStack(spacing: 16) {
        StarRatingView(rating: 1)
        StarRatingView(rating: 2)
        StarRatingView(rating: 3)
        StarRatingView(rating: .constant(2))
    }
    .padding()
}

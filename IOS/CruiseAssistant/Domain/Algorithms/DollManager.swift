import Foundation
#if canImport(UIKit)
import UIKit
public typealias PlatformDollImage = UIImage
#elseif canImport(AppKit)
import AppKit
public typealias PlatformDollImage = NSImage
#endif

/// Duffy & Friends character family for timeline event card watermarks.
public enum DuffyCharacter: String, CaseIterable, Sendable {
    case linabell
    case stellalou
    case gelatoni
    case cookieann
    case olu
    case duffy
    case shelliemay
}

/// Event card height tiers based on event duration.
public enum DollTier: Sendable {
    /// Short card (< 45 min): peek / half-body / compact sitting poses.
    case compact
    /// Medium card (45 - 89 min): sitting / crouching / compact poses.
    case medium
    /// Tall card (>= 90 min): full-body standing / jumping / dancing poses.
    case tall
}

/// Metadata for a single doll watermark asset.
public struct DollAsset: Equatable, Sendable {
    public let relativePath: String
    public let character: DuffyCharacter
    public let tier: DollTier

    public var fullAssetPath: String {
        "dolls/\(relativePath)"
    }
}

/// Event card doll watermark manager: selects character and pose deterministically by eventId and card dimensions.
public enum DollManager {
    private static let cacheLock = NSLock()
    /// Guarded by `cacheLock`.
    nonisolated(unsafe) private static var imageCache: [String: PlatformDollImage] = [:]

    /// Duo / group horizontal assets for single-lane long events (>= 60 min).
    public static let duoAssets: [String] = [
        "dolls/duo/duo_duffy_shelliemay_1.webp",
        "dolls/duo/duo_duffy_shelliemay_2.webp",
        "dolls/duo/duo_duffy_shelliemay_3.webp",
        "dolls/duo/duo_duffy_shelliemay_4.webp",
        "dolls/duo/duo_duffy_shelliemay_5.webp",
        "dolls/duo/duo_duffy_shelliemay_6.webp",
        "dolls/duo/duo_duffy_shelliemay_7.webp",
        "dolls/duo/duo_duffy_friends_1.webp",
        "dolls/duo/duo_duffy_friends_2.webp",
        "dolls/duo/duo_duffy_friends_3.webp",
        "dolls/duo/duo_duffy_friends_4.webp",
        "dolls/duo/duo_duffy_friends_5.webp",
        "dolls/duo/duo_duffy_friends_6.webp",
        "dolls/duo/duo_duffy_friends_7.webp",
        "dolls/duo/duo_duffy_friends_8.webp",
        "dolls/duo/duo_duffy_friends_9.webp",
        "dolls/duo/duo_duffy_friends_10.webp",
        "dolls/duo/duo_duffy_friends_11.webp"
    ]

    public static let tieredDolls: [DuffyCharacter: [DollTier: [String]]] = [
        .linabell: [
            .compact: [
                "linabell/linabell_peek_1.webp",
                "linabell/linabell_peek_2.webp",
                "linabell/linabell_sit_1.webp",
                "linabell/linabell_sit_2.webp"
            ],
            .medium: [
                "linabell/linabell_sit_1.webp",
                "linabell/linabell_sit_2.webp",
                "linabell/linabell_sit_3.webp",
                "linabell/linabell_sit_4.webp",
                "linabell/linabell_sit_5.webp",
                "linabell/linabell_sit_6.webp",
                "linabell/linabell_peek_1.webp"
            ],
            .tall: [
                "linabell/linabell_stand_1.webp",
                "linabell/linabell_stand_2.webp",
                "linabell/linabell_stand_3.webp",
                "linabell/linabell_stand_4.webp",
                "linabell/linabell_stand_5.webp",
                "linabell/linabell_stand_6.webp",
                "linabell/linabell_stand_7.webp",
                "linabell/linabell_stand_8.webp",
                "linabell/linabell_stand_9.webp",
                "linabell/linabell_stand_10.webp",
                "linabell/linabell_stand_11.webp"
            ]
        ],
        .stellalou: [
            .compact: [
                "stellalou/stellalou_peek_1.webp",
                "stellalou/stellalou_peek_2.webp",
                "stellalou/stellalou_sit_1.webp",
                "stellalou/stellalou_sit_2.webp"
            ],
            .medium: [
                "stellalou/stellalou_sit_1.webp",
                "stellalou/stellalou_sit_2.webp",
                "stellalou/stellalou_sit_3.webp",
                "stellalou/stellalou_sit_4.webp",
                "stellalou/stellalou_sit_5.webp",
                "stellalou/stellalou_peek_1.webp"
            ],
            .tall: [
                "stellalou/stellalou_stand_1.webp",
                "stellalou/stellalou_stand_2.webp",
                "stellalou/stellalou_stand_3.webp",
                "stellalou/stellalou_stand_4.webp",
                "stellalou/stellalou_stand_5.webp",
                "stellalou/stellalou_stand_6.webp",
                "stellalou/stellalou_stand_7.webp",
                "stellalou/stellalou_stand_8.webp",
                "stellalou/stellalou_stand_9.webp"
            ]
        ],
        .gelatoni: [
            .compact: [
                "gelatoni/gelatoni_peek_1.webp",
                "gelatoni/gelatoni_sit_1.webp",
                "gelatoni/gelatoni_sit_2.webp",
                "gelatoni/gelatoni_sit_3.webp"
            ],
            .medium: [
                "gelatoni/gelatoni_sit_1.webp",
                "gelatoni/gelatoni_sit_2.webp",
                "gelatoni/gelatoni_sit_3.webp",
                "gelatoni/gelatoni_sit_4.webp",
                "gelatoni/gelatoni_sit_5.webp",
                "gelatoni/gelatoni_peek_1.webp"
            ],
            .tall: [
                "gelatoni/gelatoni_stand_1.webp",
                "gelatoni/gelatoni_stand_2.webp",
                "gelatoni/gelatoni_stand_3.webp",
                "gelatoni/gelatoni_stand_4.webp",
                "gelatoni/gelatoni_stand_5.webp",
                "gelatoni/gelatoni_stand_6.webp"
            ]
        ],
        .cookieann: [
            .compact: [
                "cookieann/cookieann_peek_1.webp",
                "cookieann/cookieann_peek_2.webp",
                "cookieann/cookieann_sit_1.webp",
                "cookieann/cookieann_sit_2.webp"
            ],
            .medium: [
                "cookieann/cookieann_sit_1.webp",
                "cookieann/cookieann_sit_2.webp",
                "cookieann/cookieann_sit_3.webp",
                "cookieann/cookieann_sit_4.webp",
                "cookieann/cookieann_sit_5.webp",
                "cookieann/cookieann_peek_1.webp"
            ],
            .tall: [
                "cookieann/cookieann_stand_1.webp",
                "cookieann/cookieann_stand_2.webp",
                "cookieann/cookieann_stand_3.webp",
                "cookieann/cookieann_stand_4.webp",
                "cookieann/cookieann_stand_5.webp"
            ]
        ],
        .olu: [
            .compact: [
                "olu/olu_peek_1.webp",
                "olu/olu_peek_2.webp",
                "olu/olu_sit_1.webp",
                "olu/olu_sit_2.webp"
            ],
            .medium: [
                "olu/olu_sit_1.webp",
                "olu/olu_sit_2.webp",
                "olu/olu_sit_3.webp",
                "olu/olu_sit_4.webp",
                "olu/olu_peek_1.webp",
                "olu/olu_peek_2.webp"
            ],
            .tall: [
                "olu/olu_stand_1.webp",
                "olu/olu_stand_2.webp",
                "olu/olu_stand_3.webp",
                "olu/olu_stand_4.webp",
                "olu/olu_stand_5.webp",
                "olu/olu_stand_6.webp"
            ]
        ],
        .duffy: [
            .compact: [
                "duffy/duffy_sit_1.webp",
                "duffy/duffy_sit_2.webp",
                "duffy/duffy_sit_3.webp",
                "duffy/duffy_sit_4.webp"
            ],
            .medium: [
                "duffy/duffy_sit_1.webp",
                "duffy/duffy_sit_2.webp",
                "duffy/duffy_sit_3.webp",
                "duffy/duffy_sit_4.webp",
                "duffy/duffy_sit_5.webp"
            ],
            .tall: [
                "duffy/duffy_stand_1.webp",
                "duffy/duffy_stand_2.webp",
                "duffy/duffy_stand_3.webp"
            ]
        ],
        .shelliemay: [
            .compact: [
                "shelliemay/shelliemay_peek_1.webp",
                "shelliemay/shelliemay_sit_1.webp",
                "shelliemay/shelliemay_sit_2.webp"
            ],
            .medium: [
                "shelliemay/shelliemay_sit_1.webp",
                "shelliemay/shelliemay_sit_2.webp",
                "shelliemay/shelliemay_sit_3.webp",
                "shelliemay/shelliemay_peek_1.webp"
            ],
            .tall: [
                "shelliemay/shelliemay_stand_1.webp",
                "shelliemay/shelliemay_stand_2.webp",
                "shelliemay/shelliemay_stand_3.webp",
                "shelliemay/shelliemay_stand_4.webp",
                "shelliemay/shelliemay_stand_5.webp",
                "shelliemay/shelliemay_stand_6.webp"
            ]
        ]
    ]

    /// Selects the doll watermark asset path for an event card.
    public static func selectDollForCard(
        eventId: Int64,
        durationMinutes: Int,
        laneCount: Int = 1
    ) -> String {
        let seed = Int(abs(eventId &* 31 &+ Int64(durationMinutes) &* 7))

        // Single-lane long cards (>= 60 min) have a 50% chance to use a duo/group asset
        if laneCount <= 1 && durationMinutes >= 60 && !duoAssets.isEmpty {
            if seed % 2 == 0 {
                let duoIdx = (seed / 2) % duoAssets.count
                return duoAssets[duoIdx]
            }
        }

        let characters = DuffyCharacter.allCases
        let character = characters[seed % characters.count]

        let tier: DollTier
        if durationMinutes < 45 {
            tier = .compact
        } else if durationMinutes < 90 {
            tier = .medium
        } else {
            tier = .tall
        }

        let charMap = tieredDolls[character] ?? [:]
        let candidates = charMap[tier] ?? charMap[.medium] ?? ["linabell/linabell_sit_1.webp"]
        let poseIndex = (seed / characters.count) % candidates.count
        return "dolls/\(candidates[poseIndex])"
    }

    /// Loads and caches the platform image for a doll asset path (e.g. `"dolls/linabell/linabell_sit_1.webp"`).
    public static func loadDollImage(assetPath: String, bundle: Bundle? = nil) -> PlatformDollImage? {
        cacheLock.lock()
        if let cached = imageCache[assetPath] {
            cacheLock.unlock()
            return cached
        }
        cacheLock.unlock()

        let normalized = assetPath
            .replacingOccurrences(of: "dolls/", with: "")
            .replacingOccurrences(of: "Dolls/", with: "")
        let components = normalized.split(separator: "/").map(String.init)
        let filename = components.last ?? normalized
        let subdirPart = components.count > 1 ? components.dropLast().joined(separator: "/") : nil
        let name = (filename as NSString).deletingPathExtension
        let ext = (filename as NSString).pathExtension.isEmpty ? "webp" : (filename as NSString).pathExtension

        let bundlesToTry: [Bundle] = [
            bundle,
            CruiseAssistant.bundle,
            Bundle.main,
            Bundle(for: CruiseAssistantBundleMarker.self)
        ].compactMap { $0 }

        var subdirsToTry: [String?] = []
        if let sub = subdirPart {
            subdirsToTry.append("Dolls/\(sub)")
            subdirsToTry.append("dolls/\(sub)")
            subdirsToTry.append("Resources/Dolls/\(sub)")
            subdirsToTry.append("Resources/dolls/\(sub)")
            subdirsToTry.append(sub)
        }
        subdirsToTry.append("Dolls")
        subdirsToTry.append("dolls")
        subdirsToTry.append("Resources/Dolls")
        subdirsToTry.append("Resources/dolls")
        subdirsToTry.append(nil)

        for b in bundlesToTry {
            for sub in subdirsToTry {
                if let url = b.url(forResource: name, withExtension: ext, subdirectory: sub) {
                    #if canImport(UIKit)
                    if let img = UIImage(contentsOfFile: url.path) {
                        cacheLock.lock()
                        imageCache[assetPath] = img
                        cacheLock.unlock()
                        return img
                    }
                    #elseif canImport(AppKit)
                    if let img = NSImage(contentsOf: url) {
                        cacheLock.lock()
                        imageCache[assetPath] = img
                        cacheLock.unlock()
                        return img
                    }
                    #endif
                }
            }
        }
        return nil
    }
}

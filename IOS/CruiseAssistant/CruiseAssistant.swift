import Foundation

public final class CruiseAssistantBundleMarker: @unchecked Sendable {}

extension Bundle {
    public static var cruiseAssistantModule: Bundle {
        #if SWIFT_PACKAGE
        return Bundle.module
        #else
        let bundle = Bundle(for: CruiseAssistantBundleMarker.self)
        if let resourceUrl = bundle.url(forResource: "Resources", withExtension: "bundle"),
           let resBundle = Bundle(url: resourceUrl) {
            return resBundle
        }
        return bundle
        #endif
    }
}

public struct CruiseAssistant {
    public static let version = "1.12.1"
    public static var bundle: Bundle {
        Bundle.cruiseAssistantModule
    }

    public init() {}
}

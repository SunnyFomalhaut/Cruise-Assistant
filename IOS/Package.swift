// swift-tools-version: 5.9
import PackageDescription

let package = Package(
    name: "CruiseAssistant",
    platforms: [
        .iOS(.v17),
        .macOS(.v14)
    ],
    products: [
        .library(
            name: "CruiseAssistant",
            targets: ["CruiseAssistant"]
        )
    ],
    dependencies: [],
    targets: [
        .target(
            name: "CruiseAssistant",
            dependencies: [],
            path: "CruiseAssistant",
            resources: [
                .process("Resources")
            ]
        ),
        .testTarget(
            name: "CruiseAssistantTests",
            dependencies: ["CruiseAssistant"],
            path: "CruiseAssistantTests"
        )
    ]
)

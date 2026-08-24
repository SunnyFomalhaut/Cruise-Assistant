import os
import uuid

def generate_uuid():
    return uuid.uuid4().hex[:24].upper()

def create_xcodeproj():
    ios_dir = os.path.abspath("IOS")
    proj_dir = os.path.join(ios_dir, "CruiseAssistant.xcodeproj")
    os.makedirs(os.path.join(proj_dir, "xcshareddata", "xcschemes"), exist_ok=True)
    
    # Collect all swift files relative to IOS/CruiseAssistant
    source_files = []
    ca_dir = os.path.join(ios_dir, "CruiseAssistant")
    for root, dirs, files in os.walk(ca_dir):
        if "Resources" in root or "Assets.xcassets" in root:
            continue
        for f in files:
            if f.endswith(".swift"):
                rel_path = os.path.relpath(os.path.join(root, f), ca_dir)
                source_files.append(rel_path)
                
    source_files.sort()
    print(f"Found {len(source_files)} source files.")
    
    # Project UUIDs
    proj_uuid = generate_uuid()
    target_uuid = generate_uuid()
    config_list_proj = generate_uuid()
    config_list_target = generate_uuid()
    debug_config_proj = generate_uuid()
    release_config_proj = generate_uuid()
    debug_config_target = generate_uuid()
    release_config_target = generate_uuid()
    main_group_uuid = generate_uuid()
    sources_group_uuid = generate_uuid()
    products_group_uuid = generate_uuid()
    sources_phase_uuid = generate_uuid()
    resources_phase_uuid = generate_uuid()
    frameworks_phase_uuid = generate_uuid()
    app_product_uuid = generate_uuid()
    
    file_refs = {}
    for sf in source_files:
        f_uuid = generate_uuid()
        b_uuid = generate_uuid()
        file_refs[sf] = (f_uuid, b_uuid)
    resource_folders = ["Resources/Raw", "Resources/Posters", "Resources/Wallpapers", "Assets.xcassets"]
    for rf in resource_folders:
        f_uuid = generate_uuid()
        b_uuid = generate_uuid()
        file_refs[rf] = (f_uuid, b_uuid)
    pbxproj = []
    pbxproj.append("// !$*UTF8*$!")
    pbxproj.append("{")
    pbxproj.append("\tarchiveVersion = 1;")
    pbxproj.append("\tclasses = {")
    pbxproj.append("\t};")
    pbxproj.append("\tobjectVersion = 56;")
    pbxproj.append("\tobjects = {")
    
    # PBXBuildFile
    pbxproj.append("/* Begin PBXBuildFile section */")
    for sf, (f_uuid, b_uuid) in file_refs.items():
        pbxproj.append(f"\t\t{b_uuid} /* {os.path.basename(sf)} in {'Sources' if sf.endswith('.swift') else 'Resources'} */ = {{isa = PBXBuildFile; fileRef = {f_uuid} /* {os.path.basename(sf)} */; }};")
    pbxproj.append("/* End PBXBuildFile section */")
    
    # PBXFileReference
    pbxproj.append("/* Begin PBXFileReference section */")
    pbxproj.append(f"\t\t{app_product_uuid} /* CruiseAssistant.app */ = {{isa = PBXFileReference; explicitFileType = wrapper.application; includeInIndex = 0; path = CruiseAssistant.app; sourceTree = BUILT_PRODUCTS_DIR; }};")
    for sf, (f_uuid, b_uuid) in file_refs.items():
        if sf.endswith(".swift"):
            pbxproj.append(f"\t\t{f_uuid} /* {os.path.basename(sf)} */ = {{isa = PBXFileReference; lastKnownFileType = sourcecode.swift; path = \"{sf}\"; sourceTree = \"<group>\"; }};")
        elif sf.endswith(".xcassets"):
            pbxproj.append(f"\t\t{f_uuid} /* {os.path.basename(sf)} */ = {{isa = PBXFileReference; lastKnownFileType = folder.assetcatalog; path = \"{sf}\"; sourceTree = \"<group>\"; }};")
        else:
            pbxproj.append(f"\t\t{f_uuid} /* {os.path.basename(sf)} */ = {{isa = PBXFileReference; lastKnownFileType = folder; path = \"{sf}\"; sourceTree = \"<group>\"; }};")
    # PBXFrameworksBuildPhase
    pbxproj.append("/* Begin PBXFrameworksBuildPhase section */")
    pbxproj.append(f"\t\t{frameworks_phase_uuid} /* Frameworks */ = {{")
    pbxproj.append("\t\t\tisa = PBXFrameworksBuildPhase;")
    pbxproj.append("\t\t\tbuildActionMask = 2147483647;")
    pbxproj.append("\t\t\tfiles = (")
    pbxproj.append("\t\t\t);")
    pbxproj.append("\t\t\trunOnlyForDeploymentPostprocessing = 0;")
    pbxproj.append("\t\t};")
    pbxproj.append("/* End PBXFrameworksBuildPhase section */")
    
    # PBXGroup
    pbxproj.append("/* Begin PBXGroup section */")
    pbxproj.append(f"\t\t{main_group_uuid} = {{")
    pbxproj.append("\t\t\tisa = PBXGroup;")
    pbxproj.append("\t\t\tchildren = (")
    pbxproj.append(f"\t\t\t\t{sources_group_uuid} /* CruiseAssistant */,")
    pbxproj.append(f"\t\t\t\t{products_group_uuid} /* Products */,")
    pbxproj.append("\t\t\t);")
    pbxproj.append("\t\t\tsourceTree = \"<group>\";")
    pbxproj.append("\t\t};")
    
    pbxproj.append(f"\t\t{sources_group_uuid} /* CruiseAssistant */ = {{")
    pbxproj.append("\t\t\tisa = PBXGroup;")
    pbxproj.append("\t\t\tchildren = (")
    for sf, (f_uuid, _) in file_refs.items():
        pbxproj.append(f"\t\t\t\t{f_uuid} /* {os.path.basename(sf)} */,")
    pbxproj.append("\t\t\t);")
    pbxproj.append("\t\t\tpath = CruiseAssistant;")
    pbxproj.append("\t\t\tsourceTree = \"<group>\";")
    pbxproj.append("\t\t};")
    
    pbxproj.append(f"\t\t{products_group_uuid} /* Products */ = {{")
    pbxproj.append("\t\t\tisa = PBXGroup;")
    pbxproj.append("\t\t\tchildren = (")
    pbxproj.append(f"\t\t\t\t{app_product_uuid} /* CruiseAssistant.app */,")
    pbxproj.append("\t\t\t);")
    pbxproj.append("\t\t\tname = Products;")
    pbxproj.append("\t\t\tsourceTree = \"<group>\";")
    pbxproj.append("\t\t};")
    pbxproj.append("/* End PBXGroup section */")
    
    # PBXNativeTarget
    pbxproj.append("/* Begin PBXNativeTarget section */")
    pbxproj.append(f"\t\t{target_uuid} /* CruiseAssistant */ = {{")
    pbxproj.append("\t\t\tisa = PBXNativeTarget;")
    pbxproj.append(f"\t\t\tbuildConfigurationList = {config_list_target} /* Build configuration list for PBXNativeTarget \"CruiseAssistant\" */;")
    pbxproj.append("\t\t\tbuildPhases = (")
    pbxproj.append(f"\t\t\t\t{sources_phase_uuid} /* Sources */,")
    pbxproj.append(f"\t\t\t\t{frameworks_phase_uuid} /* Frameworks */,")
    pbxproj.append(f"\t\t\t\t{resources_phase_uuid} /* Resources */,")
    pbxproj.append("\t\t\t);")
    pbxproj.append("\t\t\tbuildRules = (")
    pbxproj.append("\t\t\t);")
    pbxproj.append("\t\t\tdependencies = (")
    pbxproj.append("\t\t\t);")
    pbxproj.append("\t\t\tname = CruiseAssistant;")
    pbxproj.append("\t\t\tproductName = CruiseAssistant;")
    pbxproj.append(f"\t\t\tproductReference = {app_product_uuid} /* CruiseAssistant.app */;")
    pbxproj.append("\t\t\tproductType = \"com.apple.product-type.application\";")
    pbxproj.append("\t\t};")
    pbxproj.append("/* End PBXNativeTarget section */")
    
    # PBXProject
    pbxproj.append("/* Begin PBXProject section */")
    pbxproj.append(f"\t\t{proj_uuid} /* Project object */ = {{")
    pbxproj.append("\t\t\tisa = PBXProject;")
    pbxproj.append("\t\t\tattributes = {")
    pbxproj.append("\t\t\t\tBuildIndependentTargetsInParallel = 1;")
    pbxproj.append("\t\t\t\tLastSwiftUpdateCheck = 1600;")
    pbxproj.append("\t\t\t\tLastUpgradeCheck = 1600;")
    pbxproj.append("\t\t\t\tTargetAttributes = {")
    pbxproj.append(f"\t\t\t\t\t{target_uuid} = {{")
    pbxproj.append("\t\t\t\t\t\tCreatedOnToolsVersion = 16.0;")
    pbxproj.append("\t\t\t\t\t\tDevelopmentTeam = AMLWQR59H4;")
    pbxproj.append("\t\t\t\t\t\tProvisioningStyle = Automatic;")
    pbxproj.append("\t\t\t\t\t};")
    pbxproj.append("\t\t\t\t};")
    pbxproj.append("\t\t\t};")
    pbxproj.append(f"\t\t\tbuildConfigurationList = {config_list_proj} /* Build configuration list for PBXProject \"CruiseAssistant\" */;")
    pbxproj.append("\t\t\tcompatibilityVersion = \"Xcode 14.0\";")
    pbxproj.append("\t\t\tdevelopmentRegion = zh-Hans;")
    pbxproj.append("\t\t\thasScannedForEncodings = 0;")
    pbxproj.append("\t\t\tknownRegions = (")
    pbxproj.append("\t\t\t\ten,")
    pbxproj.append("\t\t\t\tBase,")
    pbxproj.append("\t\t\t\t\"zh-Hans\",")
    pbxproj.append("\t\t\t);")
    pbxproj.append(f"\t\t\tmainGroup = {main_group_uuid};")
    pbxproj.append(f"\t\t\tproductRefGroup = {products_group_uuid} /* Products */;")
    pbxproj.append("\t\t\tprojectDirPath = \"\";")
    pbxproj.append("\t\t\tprojectRoot = \"\";")
    pbxproj.append("\t\t\ttargets = (")
    pbxproj.append(f"\t\t\t\t{target_uuid} /* CruiseAssistant */,")
    pbxproj.append("\t\t\t);")
    pbxproj.append("\t\t};")
    pbxproj.append("/* End PBXProject section */")
    
    # PBXResourcesBuildPhase
    pbxproj.append("/* Begin PBXResourcesBuildPhase section */")
    pbxproj.append(f"\t\t{resources_phase_uuid} /* Resources */ = {{")
    pbxproj.append("\t\t\tisa = PBXResourcesBuildPhase;")
    pbxproj.append("\t\t\tbuildActionMask = 2147483647;")
    pbxproj.append("\t\t\tfiles = (")
    for rf in resource_folders:
        _, b_uuid = file_refs[rf]
        pbxproj.append(f"\t\t\t\t{b_uuid} /* {os.path.basename(rf)} in Resources */,")
    pbxproj.append("\t\t\t);")
    pbxproj.append("\t\t\trunOnlyForDeploymentPostprocessing = 0;")
    pbxproj.append("\t\t};")
    pbxproj.append("/* End PBXResourcesBuildPhase section */")
    
    # PBXSourcesBuildPhase
    pbxproj.append("/* Begin PBXSourcesBuildPhase section */")
    pbxproj.append(f"\t\t{sources_phase_uuid} /* Sources */ = {{")
    pbxproj.append("\t\t\tisa = PBXSourcesBuildPhase;")
    pbxproj.append("\t\t\tbuildActionMask = 2147483647;")
    pbxproj.append("\t\t\tfiles = (")
    for sf in source_files:
        _, b_uuid = file_refs[sf]
        pbxproj.append(f"\t\t\t\t{b_uuid} /* {os.path.basename(sf)} in Sources */,")
    pbxproj.append("\t\t\t);")
    pbxproj.append("\t\t\trunOnlyForDeploymentPostprocessing = 0;")
    pbxproj.append("\t\t};")
    pbxproj.append("/* End PBXSourcesBuildPhase section */")
    
    # XCBuildConfiguration
    pbxproj.append("/* Begin XCBuildConfiguration section */")
    pbxproj.append(f"\t\t{debug_config_proj} /* Debug */ = {{")
    pbxproj.append("\t\t\tisa = XCBuildConfiguration;")
    pbxproj.append("\t\t\tbuildSettings = {")
    pbxproj.append("\t\t\t\tALWAYS_SEARCH_USER_PATHS = NO;")
    pbxproj.append("\t\t\t\tCLANG_ANALYZER_NONNULL = YES;")
    pbxproj.append("\t\t\t\tCLANG_ENABLE_MODULES = YES;")
    pbxproj.append("\t\t\t\tDEBUG_INFORMATION_FORMAT = dwarf;")
    pbxproj.append("\t\t\t\tENABLE_STRICT_OBJC_MSGSEND = YES;")
    pbxproj.append("\t\t\t\tENABLE_TESTABILITY = YES;")
    pbxproj.append("\t\t\t\tGCC_DYNAMIC_NO_PIC = NO;")
    pbxproj.append("\t\t\t\tGCC_NO_COMMON_BLOCKS = YES;")
    pbxproj.append("\t\t\t\tGCC_OPTIMIZATION_LEVEL = 0;")
    pbxproj.append("\t\t\t\tGCC_PREPROCESSOR_DEFINITIONS = (")
    pbxproj.append("\t\t\t\t\t\"DEBUG=1\",")
    pbxproj.append("\t\t\t\t\t\"$(inherited)\",")
    pbxproj.append("\t\t\t\t);")
    pbxproj.append("\t\t\t\tIPHONEOS_DEPLOYMENT_TARGET = 17.0;")
    pbxproj.append("\t\t\t\tMTL_ENABLE_DEBUG_INFO = INCLUDE_SOURCE;")
    pbxproj.append("\t\t\t\tONLY_ACTIVE_ARCH = YES;")
    pbxproj.append("\t\t\t\tSDKROOT = iphoneos;")
    pbxproj.append("\t\t\t\tSWIFT_ACTIVE_COMPILATION_CONDITIONS = \"DEBUG $(inherited)\";")
    pbxproj.append("\t\t\t\tSWIFT_OPTIMIZATION_LEVEL = \"-Onone\";")
    pbxproj.append("\t\t\t\tSWIFT_VERSION = 6.0;")
    pbxproj.append("\t\t\t};")
    pbxproj.append("\t\t\tname = Debug;")
    pbxproj.append("\t\t};")
    
    pbxproj.append(f"\t\t{release_config_proj} /* Release */ = {{")
    pbxproj.append("\t\t\tisa = XCBuildConfiguration;")
    pbxproj.append("\t\t\tbuildSettings = {")
    pbxproj.append("\t\t\t\tALWAYS_SEARCH_USER_PATHS = NO;")
    pbxproj.append("\t\t\t\tCLANG_ANALYZER_NONNULL = YES;")
    pbxproj.append("\t\t\t\tCLANG_ENABLE_MODULES = YES;")
    pbxproj.append("\t\t\t\tCLANG_ENABLE_OBJC_ARC = YES;")
    pbxproj.append("\t\t\t\tDEBUG_INFORMATION_FORMAT = \"dwarf-with-dsym\";")
    pbxproj.append("\t\t\t\tENABLE_NS_ASSERTIONS = NO;")
    pbxproj.append("\t\t\t\tENABLE_STRICT_OBJC_MSGSEND = YES;")
    pbxproj.append("\t\t\t\tGCC_NO_COMMON_BLOCKS = YES;")
    pbxproj.append("\t\t\t\tIPHONEOS_DEPLOYMENT_TARGET = 17.0;")
    pbxproj.append("\t\t\t\tMTL_ENABLE_DEBUG_INFO = NO;")
    pbxproj.append("\t\t\t\tSDKROOT = iphoneos;")
    pbxproj.append("\t\t\t\tSWIFT_COMPILATION_MODE = wholemodule;")
    pbxproj.append("\t\t\t\tSWIFT_OPTIMIZATION_LEVEL = \"-O\";")
    pbxproj.append("\t\t\t\tSWIFT_VERSION = 6.0;")
    pbxproj.append("\t\t\t\tVALIDATE_PRODUCT = YES;")
    pbxproj.append("\t\t\t};")
    pbxproj.append("\t\t\tname = Release;")
    pbxproj.append("\t\t};")
    
    pbxproj.append(f"\t\t{debug_config_target} /* Debug */ = {{")
    pbxproj.append("\t\t\tisa = XCBuildConfiguration;")
    pbxproj.append("\t\t\tbuildSettings = {")
    pbxproj.append("\t\t\t\tASSETCATALOG_COMPILER_APPICON_NAME = AppIcon;")
    pbxproj.append("\t\t\t\tASSETCATALOG_COMPILER_GLOBAL_ACCENT_COLOR_NAME = AccentColor;")
    pbxproj.append("\t\t\t\tCODE_SIGN_IDENTITY = \"Apple Development\";")
    pbxproj.append("\t\t\t\t\"CODE_SIGN_IDENTITY[sdk=iphoneos*]\" = \"Apple Development\";")
    pbxproj.append("\t\t\t\tCODE_SIGNING_ALLOWED = YES;")
    pbxproj.append("\t\t\t\tCODE_SIGN_STYLE = Automatic;")
    pbxproj.append("\t\t\t\tDEVELOPMENT_TEAM = AMLWQR59H4;")
    pbxproj.append("\t\t\t\tENABLE_PREVIEWS = NO;")
    pbxproj.append("\t\t\t\tENABLE_DEBUG_DYLIB = NO;")
    pbxproj.append("\t\t\t\tGENERATE_INFOPLIST_FILE = YES;")
    pbxproj.append("\t\t\t\tINFOPLIST_FILE = CruiseAssistant/Info.plist;")
    pbxproj.append("\t\t\t\tINFOPLIST_KEY_UILaunchScreen_Generation = YES;")
    pbxproj.append("\t\t\t\tINFOPLIST_KEY_UISupportedInterfaceOrientations = \"UIInterfaceOrientationPortrait UIInterfaceOrientationLandscapeLeft UIInterfaceOrientationLandscapeRight\";")
    pbxproj.append("\t\t\t\tIPHONEOS_DEPLOYMENT_TARGET = 17.0;")
    pbxproj.append("\t\t\t\tLD_RUNPATH_SEARCH_PATHS = (")
    pbxproj.append("\t\t\t\t\t\"$(inherited)\",")
    pbxproj.append("\t\t\t\t\t\"@executable_path/Frameworks\",")
    pbxproj.append("\t\t\t\t);")
    pbxproj.append("\t\t\t\tMARKETING_VERSION = 1.0;")
    pbxproj.append("\t\t\t\tPRODUCT_BUNDLE_IDENTIFIER = com.cruise.assistant.ios;")
    pbxproj.append("\t\t\t\tPRODUCT_NAME = \"$(TARGET_NAME)\";")
    pbxproj.append("\t\t\t\tSDKROOT = iphoneos;")
    pbxproj.append("\t\t\t\tSWIFT_EMIT_LOC_STRINGS = YES;")
    pbxproj.append("\t\t\t\tSWIFT_VERSION = 6.0;")
    pbxproj.append("\t\t\t\tTARGETED_DEVICE_FAMILY = \"1,2\";")
    pbxproj.append("\t\t\t};")
    pbxproj.append("\t\t\tname = Debug;")
    pbxproj.append("\t\t};")
    
    pbxproj.append(f"\t\t{release_config_target} /* Release */ = {{")
    pbxproj.append("\t\t\tisa = XCBuildConfiguration;")
    pbxproj.append("\t\t\tbuildSettings = {")
    pbxproj.append("\t\t\t\tCODE_SIGN_IDENTITY = \"Apple Development\";")
    pbxproj.append("\t\t\t\t\"CODE_SIGN_IDENTITY[sdk=iphoneos*]\" = \"Apple Development\";")
    pbxproj.append("\t\t\t\tCODE_SIGNING_ALLOWED = YES;")
    pbxproj.append("\t\t\t\tCODE_SIGN_STYLE = Automatic;")
    pbxproj.append("\t\t\t\tCURRENT_PROJECT_VERSION = 1;")
    pbxproj.append("\t\t\t\tDEVELOPMENT_TEAM = AMLWQR59H4;")
    pbxproj.append("\t\t\t\tGENERATE_INFOPLIST_FILE = YES;")
    pbxproj.append("\t\t\t\tINFOPLIST_FILE = CruiseAssistant/Info.plist;")
    pbxproj.append("\t\t\t\tINFOPLIST_KEY_UILaunchScreen_Generation = YES;")
    pbxproj.append("\t\t\t\tINFOPLIST_KEY_UISupportedInterfaceOrientations = \"UIInterfaceOrientationPortrait UIInterfaceOrientationLandscapeLeft UIInterfaceOrientationLandscapeRight\";")
    pbxproj.append("\t\t\t\tIPHONEOS_DEPLOYMENT_TARGET = 17.0;")
    pbxproj.append("\t\t\t\tLD_RUNPATH_SEARCH_PATHS = (")
    pbxproj.append("\t\t\t\t\t\"$(inherited)\",")
    pbxproj.append("\t\t\t\t\t\"@executable_path/Frameworks\",")
    pbxproj.append("\t\t\t\t);")
    pbxproj.append("\t\t\t\tPRODUCT_BUNDLE_IDENTIFIER = com.cruise.assistant.ios;")
    pbxproj.append("\t\t\t\tPRODUCT_NAME = \"$(TARGET_NAME)\";")
    pbxproj.append("\t\t\t\tSDKROOT = iphoneos;")
    pbxproj.append("\t\t\t\tSWIFT_EMIT_LOC_STRINGS = YES;")
    pbxproj.append("\t\t\t\tSWIFT_VERSION = 6.0;")
    pbxproj.append("\t\t\t\tTARGETED_DEVICE_FAMILY = \"1,2\";")
    pbxproj.append("\t\t\t};")
    pbxproj.append("\t\t\tname = Release;")
    pbxproj.append("\t\t};")
    pbxproj.append("/* End XCBuildConfiguration section */")
    
    # XCConfigurationList
    pbxproj.append("/* Begin XCConfigurationList section */")
    pbxproj.append(f"\t\t{config_list_proj} /* Build configuration list for PBXProject \"CruiseAssistant\" */ = {{")
    pbxproj.append("\t\t\tisa = XCConfigurationList;")
    pbxproj.append("\t\t\tbuildConfigurations = (")
    pbxproj.append(f"\t\t\t\t{debug_config_proj} /* Debug */,")
    pbxproj.append(f"\t\t\t\t{release_config_proj} /* Release */,")
    pbxproj.append("\t\t\t);")
    pbxproj.append("\t\t\tdefaultConfigurationIsVisible = 0;")
    pbxproj.append("\t\t\tdefaultConfigurationName = Release;")
    pbxproj.append("\t\t};")
    
    pbxproj.append(f"\t\t{config_list_target} /* Build configuration list for PBXNativeTarget \"CruiseAssistant\" */ = {{")
    pbxproj.append("\t\t\tisa = XCConfigurationList;")
    pbxproj.append("\t\t\tbuildConfigurations = (")
    pbxproj.append(f"\t\t\t\t{debug_config_target} /* Debug */,")
    pbxproj.append(f"\t\t\t\t{release_config_target} /* Release */,")
    pbxproj.append("\t\t\t);")
    pbxproj.append("\t\t\tdefaultConfigurationIsVisible = 0;")
    pbxproj.append("\t\t\tdefaultConfigurationName = Release;")
    pbxproj.append("\t\t};")
    pbxproj.append("/* End XCConfigurationList section */")
    
    pbxproj.append("\t};")
    pbxproj.append(f"\trootObject = {proj_uuid} /* Project object */;")
    pbxproj.append("}")
    
    with open(os.path.join(proj_dir, "project.pbxproj"), "w") as f:
        f.write("\n".join(pbxproj))
        
    # Create scheme
    scheme = f"""<?xml version="1.0" encoding="UTF-8"?>
<Scheme
   LastUpgradeVersion = "1600"
   version = "1.7">
   <BuildAction
      parallelizeBuildables = "YES"
      buildImplicitDependencies = "YES"
      buildArchitectures = "Automatic">
      <BuildActionEntries>
         <BuildActionEntry
            buildForTesting = "YES"
            buildForRunning = "YES"
            buildForProfiling = "YES"
            buildForArchiving = "YES"
            buildForAnalyzing = "YES">
            <BuildableReference
               BuildableIdentifier = "primary"
               BlueprintIdentifier = "{target_uuid}"
               BuildableName = "CruiseAssistant.app"
               BlueprintName = "CruiseAssistant"
               ReferencedContainer = "container:CruiseAssistant.xcodeproj">
            </BuildableReference>
         </BuildActionEntry>
      </BuildActionEntries>
   </BuildAction>
   <TestAction
      buildConfiguration = "Debug"
      selectedDebuggerIdentifier = "Xcode.DebuggerFoundation.Debugger.LLDB"
      selectedLauncherIdentifier = "Xcode.DebuggerFoundation.Launcher.LLDB"
      shouldUseLaunchSchemeArgsEnv = "YES"
      shouldAutocreateTestPlan = "YES">
   </TestAction>
   <LaunchAction
      buildConfiguration = "Debug"
      selectedDebuggerIdentifier = "Xcode.DebuggerFoundation.Debugger.LLDB"
      selectedLauncherIdentifier = "Xcode.DebuggerFoundation.Launcher.LLDB"
      launchStyle = "0"
      useCustomWorkingDirectory = "NO"
      ignoresPersistentStateOnLaunch = "NO"
      debugDocumentVersioning = "YES"
      debugServiceExtension = "internal"
      allowLocationSimulation = "YES">
      <BuildableProductRunnable
         runnableDebuggingMode = "0">
         <BuildableReference
            BuildableIdentifier = "primary"
            BlueprintIdentifier = "{target_uuid}"
            BuildableName = "CruiseAssistant.app"
            BlueprintName = "CruiseAssistant"
            ReferencedContainer = "container:CruiseAssistant.xcodeproj">
         </BuildableReference>
      </BuildableProductRunnable>
   </LaunchAction>
   <ProfileAction
      buildConfiguration = "Release"
      shouldUseLaunchSchemeArgsEnv = "YES"
      savedToolIdentifier = ""
      useCustomWorkingDirectory = "NO"
      debugDocumentVersioning = "YES">
      <BuildableProductRunnable
         runnableDebuggingMode = "0">
         <BuildableReference
            BuildableIdentifier = "primary"
            BlueprintIdentifier = "{target_uuid}"
            BuildableName = "CruiseAssistant.app"
            BlueprintName = "CruiseAssistant"
            ReferencedContainer = "container:CruiseAssistant.xcodeproj">
         </BuildableReference>
      </BuildableProductRunnable>
   </ProfileAction>
   <AnalyzeAction
      buildConfiguration = "Debug">
   </AnalyzeAction>
   <ArchiveAction
      buildConfiguration = "Release"
      revealArchiveInOrganizer = "YES">
   </ArchiveAction>
</Scheme>
"""
    with open(os.path.join(proj_dir, "xcshareddata", "xcschemes", "CruiseAssistant.xcscheme"), "w") as f:
        f.write(scheme)
    print("Project generated successfully!")

if __name__ == "__main__":
    create_xcodeproj()

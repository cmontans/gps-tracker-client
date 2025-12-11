// Copyright GPS Tracker Team. All Rights Reserved.

using UnrealBuildTool;

public class GPSTracker : ModuleRules
{
	public GPSTracker(ReadOnlyTargetRules Target) : base(Target)
	{
		PCHUsage = ModuleRules.PCHUsageMode.UseExplicitOrSharedPCHs;

		PublicIncludePaths.AddRange(
			new string[] {
				// ... add public include paths required here ...
			}
		);

		PrivateIncludePaths.AddRange(
			new string[] {
				// ... add other private include paths required here ...
			}
		);

		PublicDependencyModuleNames.AddRange(
			new string[]
			{
				"Core",
				"CoreUObject",
				"Engine",
				"WebSockets",
				"Json",
				"JsonUtilities",
				"HTTP"
			}
		);

		// Optional Cesium support
		if (Target.bBuildEditor)
		{
			PrivateDependencyModuleNames.Add("UnrealEd");
		}

		// Check if CesiumForUnreal plugin is available
		string CesiumPath = System.IO.Path.Combine(Target.ProjectFile.Directory.FullName, "Plugins", "CesiumForUnreal");
		if (System.IO.Directory.Exists(CesiumPath))
		{
			PublicDependencyModuleNames.AddRange(
				new string[]
				{
					"CesiumRuntime"
				}
			);
			PublicDefinitions.Add("WITH_CESIUM=1");
		}
		else
		{
			PublicDefinitions.Add("WITH_CESIUM=0");
		}

		PrivateDependencyModuleNames.AddRange(
			new string[]
			{
				// ... add private dependencies that you statically link with here ...
			}
		);

		DynamicallyLoadedModuleNames.AddRange(
			new string[]
			{
				// ... add any modules that your module loads dynamically here ...
			}
		);
	}
}

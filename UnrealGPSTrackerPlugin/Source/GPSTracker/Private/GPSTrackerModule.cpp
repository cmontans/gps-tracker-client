// Copyright GPS Tracker Team. All Rights Reserved.

#include "GPSTrackerModule.h"
#include "Modules/ModuleManager.h"

#define LOCTEXT_NAMESPACE "FGPSTrackerModule"

void FGPSTrackerModule::StartupModule()
{
	// This code will execute after your module is loaded into memory; the exact timing is specified in the .uplugin file per-module
	UE_LOG(LogTemp, Log, TEXT("GPSTracker module has been loaded"));
}

void FGPSTrackerModule::ShutdownModule()
{
	// This function may be called during shutdown to clean up your module.  For modules that support dynamic reloading,
	// we call this function before unloading the module.
	UE_LOG(LogTemp, Log, TEXT("GPSTracker module has been unloaded"));
}

#undef LOCTEXT_NAMESPACE

IMPLEMENT_MODULE(FGPSTrackerModule, GPSTracker)

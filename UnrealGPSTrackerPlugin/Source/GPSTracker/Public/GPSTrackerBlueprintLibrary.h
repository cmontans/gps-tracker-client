// Copyright GPS Tracker Team. All Rights Reserved.

#pragma once

#include "CoreMinimal.h"
#include "Kismet/BlueprintFunctionLibrary.h"
#include "GPSTrackerTypes.h"
#include "GPSTrackerBlueprintLibrary.generated.h"

class UGPSTrackerSubsystem;

/**
 * Blueprint function library providing convenient access to GPS Tracker functionality
 */
UCLASS()
class GPSTRACKER_API UGPSTrackerBlueprintLibrary : public UBlueprintFunctionLibrary
{
	GENERATED_BODY()

public:
	/**
	 * Get the GPS Tracker subsystem from the world context
	 */
	UFUNCTION(BlueprintPure, Category = "GPS Tracker", meta = (WorldContext = "WorldContextObject"))
	static UGPSTrackerSubsystem* GetGPSTrackerSubsystem(const UObject* WorldContextObject);

	/**
	 * Quick connect to GPS Tracker server with default settings
	 */
	UFUNCTION(BlueprintCallable, Category = "GPS Tracker", meta = (WorldContext = "WorldContextObject"))
	static void QuickConnect(const UObject* WorldContextObject, const FString& UserName, const FString& GroupName);

	/**
	 * Convert GPS coordinates to Unreal world position
	 * @param Latitude - GPS latitude
	 * @param Longitude - GPS longitude
	 * @param Scale - Scale factor (default 100000 = 1 degree = 100km)
	 * @param HeightOffset - Z axis offset
	 */
	UFUNCTION(BlueprintPure, Category = "GPS Tracker")
	static FVector GPSToWorldPosition(double Latitude, double Longitude, float Scale = 100000.0f, float HeightOffset = 0.0f);

	/**
	 * Convert Unreal world position to GPS coordinates
	 * @param WorldPosition - Position in Unreal world space
	 * @param Scale - Scale factor (default 100000 = 1 degree = 100km)
	 * @param OutLatitude - Output latitude
	 * @param OutLongitude - Output longitude
	 */
	UFUNCTION(BlueprintPure, Category = "GPS Tracker")
	static void WorldPositionToGPS(FVector WorldPosition, float Scale, double& OutLatitude, double& OutLongitude);

	/**
	 * Calculate distance between two GPS coordinates in kilometers
	 * Uses the Haversine formula for great-circle distance
	 */
	UFUNCTION(BlueprintPure, Category = "GPS Tracker")
	static double CalculateGPSDistance(double Lat1, double Lon1, double Lat2, double Lon2);

	/**
	 * Calculate bearing between two GPS coordinates in degrees (0-360)
	 */
	UFUNCTION(BlueprintPure, Category = "GPS Tracker")
	static float CalculateGPSBearing(double Lat1, double Lon1, double Lat2, double Lon2);

	/**
	 * Format speed value for display
	 */
	UFUNCTION(BlueprintPure, Category = "GPS Tracker")
	static FString FormatSpeed(double SpeedKmh, bool bShowUnit = true);

	/**
	 * Get color based on speed (blue = slow, green = medium, red = fast)
	 */
	UFUNCTION(BlueprintPure, Category = "GPS Tracker")
	static FLinearColor GetSpeedColor(double SpeedKmh, double MaxSpeed = 200.0);
};

// Copyright GPS Tracker Team. All Rights Reserved.

#include "GPSTrackerBlueprintLibrary.h"
#include "GPSTrackerSubsystem.h"
#include "Engine/World.h"
#include "Engine/GameInstance.h"
#include "Kismet/KismetMathLibrary.h"

UGPSTrackerSubsystem* UGPSTrackerBlueprintLibrary::GetGPSTrackerSubsystem(const UObject* WorldContextObject)
{
	if (!WorldContextObject)
	{
		return nullptr;
	}

	UWorld* World = GEngine->GetWorldFromContextObject(WorldContextObject, EGetWorldErrorMode::LogAndReturnNull);
	if (!World)
	{
		return nullptr;
	}

	UGameInstance* GameInstance = World->GetGameInstance();
	if (!GameInstance)
	{
		return nullptr;
	}

	return GameInstance->GetSubsystem<UGPSTrackerSubsystem>();
}

void UGPSTrackerBlueprintLibrary::QuickConnect(const UObject* WorldContextObject, const FString& UserName, const FString& GroupName)
{
	UGPSTrackerSubsystem* Subsystem = GetGPSTrackerSubsystem(WorldContextObject);
	if (Subsystem)
	{
		// Use default server URL and generate user ID
		Subsystem->Connect(
			TEXT("wss://gps-tracker-server-production-5900.up.railway.app"),
			TEXT(""), // Empty = auto-generate
			UserName,
			GroupName
		);
	}
	else
	{
		UE_LOG(LogTemp, Error, TEXT("Failed to get GPS Tracker subsystem for QuickConnect"));
	}
}

FVector UGPSTrackerBlueprintLibrary::GPSToWorldPosition(double Latitude, double Longitude, float Scale, float HeightOffset)
{
	return FVector(
		Longitude * Scale,
		Latitude * Scale,
		HeightOffset
	);
}

void UGPSTrackerBlueprintLibrary::WorldPositionToGPS(FVector WorldPosition, float Scale, double& OutLatitude, double& OutLongitude)
{
	OutLongitude = WorldPosition.X / Scale;
	OutLatitude = WorldPosition.Y / Scale;
}

double UGPSTrackerBlueprintLibrary::CalculateGPSDistance(double Lat1, double Lon1, double Lat2, double Lon2)
{
	// Haversine formula for great-circle distance
	const double EarthRadiusKm = 6371.0;

	double dLat = FMath::DegreesToRadians(Lat2 - Lat1);
	double dLon = FMath::DegreesToRadians(Lon2 - Lon1);

	double a = FMath::Sin(dLat / 2.0) * FMath::Sin(dLat / 2.0) +
			   FMath::Cos(FMath::DegreesToRadians(Lat1)) *
			   FMath::Cos(FMath::DegreesToRadians(Lat2)) *
			   FMath::Sin(dLon / 2.0) * FMath::Sin(dLon / 2.0);

	double c = 2.0 * FMath::Atan2(FMath::Sqrt(a), FMath::Sqrt(1.0 - a));

	return EarthRadiusKm * c;
}

float UGPSTrackerBlueprintLibrary::CalculateGPSBearing(double Lat1, double Lon1, double Lat2, double Lon2)
{
	// Calculate bearing between two GPS coordinates
	double dLon = FMath::DegreesToRadians(Lon2 - Lon1);
	double lat1Rad = FMath::DegreesToRadians(Lat1);
	double lat2Rad = FMath::DegreesToRadians(Lat2);

	double y = FMath::Sin(dLon) * FMath::Cos(lat2Rad);
	double x = FMath::Cos(lat1Rad) * FMath::Sin(lat2Rad) -
			   FMath::Sin(lat1Rad) * FMath::Cos(lat2Rad) * FMath::Cos(dLon);

	double bearingRad = FMath::Atan2(y, x);
	double bearingDeg = FMath::RadiansToDegrees(bearingRad);

	// Normalize to 0-360
	return FMath::Fmod(bearingDeg + 360.0, 360.0);
}

FString UGPSTrackerBlueprintLibrary::FormatSpeed(double SpeedKmh, bool bShowUnit)
{
	if (bShowUnit)
	{
		return FString::Printf(TEXT("%.1f km/h"), SpeedKmh);
	}
	else
	{
		return FString::Printf(TEXT("%.1f"), SpeedKmh);
	}
}

FLinearColor UGPSTrackerBlueprintLibrary::GetSpeedColor(double SpeedKmh, double MaxSpeed)
{
	// Normalize speed to 0-1 range
	float Normalized = FMath::Clamp(SpeedKmh / MaxSpeed, 0.0, 1.0);

	// Blue (slow) -> Green (medium) -> Red (fast)
	if (Normalized < 0.5f)
	{
		// Blue to Green
		float t = Normalized * 2.0f;
		return FLinearColor::LerpUsingHSV(FLinearColor::Blue, FLinearColor::Green, t);
	}
	else
	{
		// Green to Red
		float t = (Normalized - 0.5f) * 2.0f;
		return FLinearColor::LerpUsingHSV(FLinearColor::Green, FLinearColor::Red, t);
	}
}

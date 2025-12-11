// Copyright GPS Tracker Team. All Rights Reserved.

#pragma once

#include "CoreMinimal.h"
#include "GPSTrackerTypes.generated.h"

/**
 * Represents a user's position data from the GPS tracker server
 */
USTRUCT(BlueprintType)
struct FGPSUserData
{
	GENERATED_BODY()

	UPROPERTY(BlueprintReadOnly, Category = "GPS Tracker")
	FString UserId;

	UPROPERTY(BlueprintReadOnly, Category = "GPS Tracker")
	FString UserName;

	UPROPERTY(BlueprintReadOnly, Category = "GPS Tracker")
	double Speed;

	UPROPERTY(BlueprintReadOnly, Category = "GPS Tracker")
	double Latitude;

	UPROPERTY(BlueprintReadOnly, Category = "GPS Tracker")
	double Longitude;

	UPROPERTY(BlueprintReadOnly, Category = "GPS Tracker")
	float Bearing;

	UPROPERTY(BlueprintReadOnly, Category = "GPS Tracker")
	int64 Timestamp;

	UPROPERTY(BlueprintReadOnly, Category = "GPS Tracker")
	FString GroupName;

	FGPSUserData()
		: Speed(0.0)
		, Latitude(0.0)
		, Longitude(0.0)
		, Bearing(0.0f)
		, Timestamp(0)
	{
	}

	/**
	 * Convert latitude/longitude to Unreal world position
	 * Using a simple Mercator projection scaled for visualization
	 * @param Scale - Scale factor for world coordinates (default 100000 = 1 degree = 100km in Unreal units)
	 */
	FVector GetWorldPosition(float Scale = 100000.0f) const
	{
		// Simple Mercator projection
		// X = Longitude, Y = Latitude, Z = 0 (ground level)
		return FVector(
			Longitude * Scale,
			Latitude * Scale,
			0.0f
		);
	}

	/**
	 * Get rotation from bearing (0-360 degrees, where 0 = North)
	 */
	FRotator GetRotation() const
	{
		// Convert GPS bearing to Unreal rotation
		// GPS: 0 = North, 90 = East, 180 = South, 270 = West
		// Unreal Yaw: 0 = Forward (X), 90 = Right (Y)
		return FRotator(0.0f, Bearing, 0.0f);
	}
};

/**
 * Connection state enum
 */
UENUM(BlueprintType)
enum class EGPSTrackerConnectionState : uint8
{
	Disconnected UMETA(DisplayName = "Disconnected"),
	Connecting UMETA(DisplayName = "Connecting"),
	Connected UMETA(DisplayName = "Connected"),
	Error UMETA(DisplayName = "Error")
};

/**
 * Delegate for connection state changes
 */
DECLARE_DYNAMIC_MULTICAST_DELEGATE_OneParam(FOnConnectionStateChanged, EGPSTrackerConnectionState, NewState);

/**
 * Delegate for receiving user position updates
 */
DECLARE_DYNAMIC_MULTICAST_DELEGATE_OneParam(FOnUsersUpdated, const TArray<FGPSUserData>&, Users);

/**
 * Delegate for group horn alerts
 */
DECLARE_DYNAMIC_MULTICAST_DELEGATE(FOnGroupHorn);

/**
 * Delegate for errors
 */
DECLARE_DYNAMIC_MULTICAST_DELEGATE_OneParam(FOnTrackerError, const FString&, ErrorMessage);

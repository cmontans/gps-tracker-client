// Copyright GPS Tracker Team. All Rights Reserved.

#pragma once

#include "CoreMinimal.h"
#include "GameFramework/Actor.h"
#include "GPSTrackerTypes.h"
#include "Components/TextRenderComponent.h"
#include "GPSTrackerVisualizerActor.generated.h"

class UGPSTrackerSubsystem;

/**
 * Actor that visualizes GPS tracker user positions in the world
 * Automatically creates and updates visual representations for each user in the group
 */
UCLASS(Blueprintable)
class GPSTRACKER_API AGPSTrackerVisualizerActor : public AActor
{
	GENERATED_BODY()

public:
	AGPSTrackerVisualizerActor();

protected:
	virtual void BeginPlay() override;
	virtual void EndPlay(const EEndPlayReason::Type EndPlayReason) override;

public:
	virtual void Tick(float DeltaTime) override;

	/**
	 * Scale factor for converting GPS coordinates to Unreal world coordinates
	 * Default: 100000 = 1 degree latitude/longitude = 100km in Unreal units
	 */
	UPROPERTY(EditAnywhere, BlueprintReadWrite, Category = "GPS Tracker|Visualization")
	float CoordinateScale = 100000.0f;

	/**
	 * Height offset for spawning user markers (Z axis)
	 */
	UPROPERTY(EditAnywhere, BlueprintReadWrite, Category = "GPS Tracker|Visualization")
	float MarkerHeightOffset = 200.0f;

	/**
	 * Size of the user marker sphere
	 */
	UPROPERTY(EditAnywhere, BlueprintReadWrite, Category = "GPS Tracker|Visualization")
	float MarkerSize = 50.0f;

	/**
	 * Whether to show user names above markers
	 */
	UPROPERTY(EditAnywhere, BlueprintReadWrite, Category = "GPS Tracker|Visualization")
	bool bShowUserNames = true;

	/**
	 * Whether to show speed information
	 */
	UPROPERTY(EditAnywhere, BlueprintReadWrite, Category = "GPS Tracker|Visualization")
	bool bShowSpeed = true;

	/**
	 * Text size for user labels
	 */
	UPROPERTY(EditAnywhere, BlueprintReadWrite, Category = "GPS Tracker|Visualization")
	float TextSize = 20.0f;

	/**
	 * Color for user markers (can be overridden per user)
	 */
	UPROPERTY(EditAnywhere, BlueprintReadWrite, Category = "GPS Tracker|Visualization")
	FLinearColor DefaultMarkerColor = FLinearColor::Blue;

	/**
	 * Whether to draw trails behind moving users
	 */
	UPROPERTY(EditAnywhere, BlueprintReadWrite, Category = "GPS Tracker|Visualization")
	bool bDrawTrails = true;

	/**
	 * Maximum number of trail points per user
	 */
	UPROPERTY(EditAnywhere, BlueprintReadWrite, Category = "GPS Tracker|Visualization")
	int32 MaxTrailPoints = 100;

	/**
	 * Blueprint event called when users are updated
	 */
	UFUNCTION(BlueprintImplementableEvent, Category = "GPS Tracker")
	void OnUsersVisualizationUpdated(const TArray<FGPSUserData>& Users);

	/**
	 * Get all currently visualized user data
	 */
	UFUNCTION(BlueprintPure, Category = "GPS Tracker")
	TArray<FGPSUserData> GetVisualizedUsers() const { return CurrentUsers; }

protected:
	/**
	 * Called when users are updated from the subsystem
	 */
	UFUNCTION()
	void HandleUsersUpdated(const TArray<FGPSUserData>& Users);

	/**
	 * Create or update visual representation for a user
	 */
	void UpdateUserVisualization(const FGPSUserData& UserData);

	/**
	 * Remove visualization for a user that is no longer present
	 */
	void RemoveUserVisualization(const FString& UserId);

	/**
	 * Blueprint event for customizing user marker appearance
	 * Override this in Blueprint to create custom markers
	 */
	UFUNCTION(BlueprintNativeEvent, Category = "GPS Tracker")
	void CreateUserMarker(const FGPSUserData& UserData, const FVector& WorldLocation);

	virtual void CreateUserMarker_Implementation(const FGPSUserData& UserData, const FVector& WorldLocation);

private:
	// Reference to GPS Tracker subsystem
	UPROPERTY()
	UGPSTrackerSubsystem* TrackerSubsystem;

	// Current list of users
	TArray<FGPSUserData> CurrentUsers;

	// User marker data structure
	struct FUserMarker
	{
		USceneComponent* RootComponent;
		UStaticMeshComponent* MarkerMesh;
		UTextRenderComponent* NameText;
		UTextRenderComponent* SpeedText;
		TArray<FVector> TrailPoints;
		FGPSUserData LastData;

		FUserMarker()
			: RootComponent(nullptr)
			, MarkerMesh(nullptr)
			, NameText(nullptr)
			, SpeedText(nullptr)
		{
		}
	};

	// Map of user ID to marker data
	TMap<FString, FUserMarker> UserMarkers;

	// Helper to get default sphere mesh
	UStaticMesh* GetDefaultSphereMesh() const;
};

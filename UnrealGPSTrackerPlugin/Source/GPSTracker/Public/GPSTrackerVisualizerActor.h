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
	 * Enable dead reckoning to smooth position updates between GPS readings
	 */
	UPROPERTY(EditAnywhere, BlueprintReadWrite, Category = "GPS Tracker|Dead Reckoning")
	bool bEnableDeadReckoning = true;

	/**
	 * Smoothing factor for position interpolation (0 = instant snap, 1 = very smooth)
	 * Higher values create smoother movement but more lag
	 */
	UPROPERTY(EditAnywhere, BlueprintReadWrite, Category = "GPS Tracker|Dead Reckoning", meta = (ClampMin = "0.0", ClampMax = "1.0"))
	float PositionSmoothingFactor = 0.15f;

	/**
	 * Maximum time in seconds to extrapolate position beyond last GPS update
	 * After this time, marker will stop at predicted position
	 */
	UPROPERTY(EditAnywhere, BlueprintReadWrite, Category = "GPS Tracker|Dead Reckoning", meta = (ClampMin = "0.0", ClampMax = "30.0"))
	float MaxExtrapolationTime = 5.0f;

	/**
	 * Minimum speed (km/h) required to apply dead reckoning prediction
	 * Below this speed, marker will not extrapolate
	 */
	UPROPERTY(EditAnywhere, BlueprintReadWrite, Category = "GPS Tracker|Dead Reckoning", meta = (ClampMin = "0.0"))
	float MinSpeedForPrediction = 1.0f;

	/**
	 * Factor to reduce prediction accuracy over time (prevents over-shooting)
	 * 1.0 = full speed prediction, 0.5 = predict at half speed
	 */
	UPROPERTY(EditAnywhere, BlueprintReadWrite, Category = "GPS Tracker|Dead Reckoning", meta = (ClampMin = "0.0", ClampMax = "1.0"))
	float PredictionDampingFactor = 0.8f;

	/**
	 * Use interpolation buffer instead of dead reckoning for position smoothing
	 * Interpolation buffer interpolates between past positions (accurate but adds latency)
	 * Dead reckoning predicts future positions (responsive but can overshoot)
	 */
	UPROPERTY(EditAnywhere, BlueprintReadWrite, Category = "GPS Tracker|Interpolation Buffer")
	bool bUseInterpolationBuffer = false;

	/**
	 * Time in seconds to delay rendering for interpolation buffer
	 * Higher values = smoother but more lag (typical: 0.1-0.3 seconds)
	 * Must be less than the time between GPS updates for smooth interpolation
	 */
	UPROPERTY(EditAnywhere, BlueprintReadWrite, Category = "GPS Tracker|Interpolation Buffer", meta = (ClampMin = "0.05", ClampMax = "2.0", EditCondition = "bUseInterpolationBuffer"))
	float InterpolationBufferTime = 0.2f;

	/**
	 * Maximum number of positions to store in interpolation buffer
	 * Larger buffers handle more irregular update rates but use more memory
	 */
	UPROPERTY(EditAnywhere, BlueprintReadWrite, Category = "GPS Tracker|Interpolation Buffer", meta = (ClampMin = "2", ClampMax = "50", EditCondition = "bUseInterpolationBuffer"))
	int32 MaxBufferSize = 10;

	/**
	 * Use Cesium plugin for accurate geolocation and coordinate transformation
	 * Requires CesiumForUnreal plugin to be installed and enabled
	 * When enabled, GPS coordinates will be converted using Cesium's georeference system
	 */
	UPROPERTY(EditAnywhere, BlueprintReadWrite, Category = "GPS Tracker|Cesium Integration")
	bool bUseCesiumGeoreference = false;

	/**
	 * Clamp user markers to terrain height from Cesium 3D Tiles
	 * Requires bUseCesiumGeoreference to be enabled
	 * Markers will be positioned at the terrain elevation plus GroundClampingOffset
	 */
	UPROPERTY(EditAnywhere, BlueprintReadWrite, Category = "GPS Tracker|Cesium Integration", meta = (EditCondition = "bUseCesiumGeoreference"))
	bool bEnableGroundClamping = false;

	/**
	 * Additional height offset above terrain when ground clamping is enabled
	 * Useful to prevent markers from clipping into the ground
	 */
	UPROPERTY(EditAnywhere, BlueprintReadWrite, Category = "GPS Tracker|Cesium Integration", meta = (EditCondition = "bEnableGroundClamping"))
	float GroundClampingOffset = 100.0f;

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
	 * Update dead reckoning prediction for a user marker
	 */
	void UpdateDeadReckoning(FUserMarker& Marker, float DeltaTime);

	/**
	 * Calculate predicted position using dead reckoning
	 */
	FVector CalculatePredictedPosition(const FUserMarker& Marker, float TimeSinceLastUpdate) const;

	/**
	 * Update interpolation buffer for a user marker
	 */
	void UpdateInterpolationBuffer(FUserMarker& Marker, float DeltaTime);

	/**
	 * Calculate interpolated position from buffer
	 */
	FVector CalculateInterpolatedPosition(const FUserMarker& Marker, double RenderTime) const;

	/**
	 * Convert GPS coordinates to world position, optionally using Cesium georeference
	 */
	FVector ConvertGPSToWorldPosition(double Latitude, double Longitude, float& OutTerrainHeight);

	/**
	 * Get the Cesium georeference actor if available
	 */
	AActor* GetCesiumGeoreference();

	/**
	 * Sample terrain height at given GPS coordinates using Cesium
	 */
	bool SampleCesiumTerrainHeight(double Latitude, double Longitude, float& OutHeight);

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

	// Cached reference to Cesium Georeference actor (if available)
	UPROPERTY()
	AActor* CesiumGeoreferenceActor;

	// Current list of users
	TArray<FGPSUserData> CurrentUsers;

	// Buffered position for interpolation
	struct FBufferedPosition
	{
		FVector Position;
		FRotator Rotation;
		double Timestamp;

		FBufferedPosition()
			: Position(FVector::ZeroVector)
			, Rotation(FRotator::ZeroRotator)
			, Timestamp(0.0)
		{
		}

		FBufferedPosition(const FVector& InPosition, const FRotator& InRotation, double InTimestamp)
			: Position(InPosition)
			, Rotation(InRotation)
			, Timestamp(InTimestamp)
		{
		}
	};

	// User marker data structure
	struct FUserMarker
	{
		USceneComponent* RootComponent;
		UStaticMeshComponent* MarkerMesh;
		UTextRenderComponent* NameText;
		UTextRenderComponent* SpeedText;
		TArray<FVector> TrailPoints;
		FGPSUserData LastData;

		// Dead reckoning state
		FVector CurrentPosition;          // Current interpolated/predicted position
		FVector TargetPosition;           // Target position from last GPS update
		FVector VelocityVector;           // Velocity vector in world space (units/second)
		double LastUpdateTime;            // Time of last GPS update (world time)
		bool bHasInitialPosition;         // Whether we've received first position

		// Interpolation buffer state
		TArray<FBufferedPosition> PositionBuffer;  // Circular buffer of recent positions
		int32 BufferWriteIndex;                     // Next index to write in buffer

		FUserMarker()
			: RootComponent(nullptr)
			, MarkerMesh(nullptr)
			, NameText(nullptr)
			, SpeedText(nullptr)
			, CurrentPosition(FVector::ZeroVector)
			, TargetPosition(FVector::ZeroVector)
			, VelocityVector(FVector::ZeroVector)
			, LastUpdateTime(0.0)
			, bHasInitialPosition(false)
			, BufferWriteIndex(0)
		{
		}
	};

	// Map of user ID to marker data
	TMap<FString, FUserMarker> UserMarkers;

	// Helper to get default sphere mesh
	UStaticMesh* GetDefaultSphereMesh() const;
};

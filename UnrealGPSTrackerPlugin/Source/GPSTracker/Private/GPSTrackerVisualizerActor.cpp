// Copyright GPS Tracker Team. All Rights Reserved.

#include "GPSTrackerVisualizerActor.h"
#include "GPSTrackerSubsystem.h"
#include "Components/StaticMeshComponent.h"
#include "Components/TextRenderComponent.h"
#include "Engine/StaticMesh.h"
#include "UObject/ConstructorHelpers.h"
#include "Materials/MaterialInstanceDynamic.h"
#include "EngineUtils.h"
#include "Kismet/GameplayStatics.h"

// Conditional Cesium includes
#if WITH_CESIUM
#include "CesiumGeoreference.h"
#include "CesiumGlobeAnchorComponent.h"
#include "GlobeAwareDefaultPawn.h"
#endif

AGPSTrackerVisualizerActor::AGPSTrackerVisualizerActor()
	: CesiumGeoreferenceActor(nullptr)
{
	PrimaryActorTick.bCanEverTick = true;

	// Create root component
	USceneComponent* Root = CreateDefaultSubobject<USceneComponent>(TEXT("Root"));
	SetRootComponent(Root);
}

void AGPSTrackerVisualizerActor::BeginPlay()
{
	Super::BeginPlay();

	// Get GPS Tracker subsystem
	if (UGameInstance* GameInstance = GetWorld()->GetGameInstance())
	{
		TrackerSubsystem = GameInstance->GetSubsystem<UGPSTrackerSubsystem>();
		if (TrackerSubsystem)
		{
			// Bind to users updated event
			TrackerSubsystem->OnUsersUpdated.AddDynamic(this, &AGPSTrackerVisualizerActor::HandleUsersUpdated);
			UE_LOG(LogTemp, Log, TEXT("GPSTrackerVisualizerActor: Bound to tracker subsystem"));
		}
		else
		{
			UE_LOG(LogTemp, Error, TEXT("GPSTrackerVisualizerActor: Failed to get tracker subsystem"));
		}
	}
}

void AGPSTrackerVisualizerActor::EndPlay(const EEndPlayReason::Type EndPlayReason)
{
	// Unbind from subsystem
	if (TrackerSubsystem)
	{
		TrackerSubsystem->OnUsersUpdated.RemoveDynamic(this, &AGPSTrackerVisualizerActor::HandleUsersUpdated);
	}

	// Clean up all markers
	for (auto& Pair : UserMarkers)
	{
		if (Pair.Value.RootComponent)
		{
			Pair.Value.RootComponent->DestroyComponent();
		}
	}
	UserMarkers.Empty();

	Super::EndPlay(EndPlayReason);
}

void AGPSTrackerVisualizerActor::Tick(float DeltaTime)
{
	Super::Tick(DeltaTime);

	// Choose smoothing method
	if (bUseInterpolationBuffer)
	{
		// Update interpolation buffer for all markers
		for (auto& Pair : UserMarkers)
		{
			UpdateInterpolationBuffer(Pair.Value, DeltaTime);
		}
	}
	else if (bEnableDeadReckoning)
	{
		// Update dead reckoning for all markers
		for (auto& Pair : UserMarkers)
		{
			UpdateDeadReckoning(Pair.Value, DeltaTime);
		}
	}

	// Draw trails for users
	if (bDrawTrails)
	{
		for (const auto& Pair : UserMarkers)
		{
			const FUserMarker& Marker = Pair.Value;
			if (Marker.TrailPoints.Num() > 1)
			{
				// Draw lines between trail points
				for (int32 i = 0; i < Marker.TrailPoints.Num() - 1; ++i)
				{
					DrawDebugLine(
						GetWorld(),
						Marker.TrailPoints[i],
						Marker.TrailPoints[i + 1],
						FColor::Cyan,
						false,
						-1.0f,
						0,
						2.0f
					);
				}
			}
		}
	}
}

void AGPSTrackerVisualizerActor::HandleUsersUpdated(const TArray<FGPSUserData>& Users)
{
	UE_LOG(LogTemp, Log, TEXT("GPSTrackerVisualizerActor: Received %d users"), Users.Num());

	CurrentUsers = Users;

	// Track which users we've seen in this update
	TSet<FString> UpdatedUserIds;

	// Update or create visualization for each user
	for (const FGPSUserData& UserData : Users)
	{
		UpdatedUserIds.Add(UserData.UserId);
		UpdateUserVisualization(UserData);
	}

	// Remove visualizations for users no longer present
	TArray<FString> UsersToRemove;
	for (const auto& Pair : UserMarkers)
	{
		if (!UpdatedUserIds.Contains(Pair.Key))
		{
			UsersToRemove.Add(Pair.Key);
		}
	}

	for (const FString& UserId : UsersToRemove)
	{
		RemoveUserVisualization(UserId);
	}

	// Call Blueprint event
	OnUsersVisualizationUpdated(Users);
}

void AGPSTrackerVisualizerActor::UpdateUserVisualization(const FGPSUserData& UserData)
{
	// Calculate world position from GPS coordinates using Cesium if available
	float TerrainHeight = 0.0f;
	FVector WorldLocation = ConvertGPSToWorldPosition(UserData.Latitude, UserData.Longitude, TerrainHeight);

	// Check if marker already exists
	FUserMarker* ExistingMarker = UserMarkers.Find(UserData.UserId);

	if (ExistingMarker)
	{
		double CurrentTime = GetWorld()->GetTimeSeconds();

		// Choose smoothing method
		if (bUseInterpolationBuffer)
		{
			// Add position to interpolation buffer
			FBufferedPosition BufferedPos(WorldLocation, UserData.GetRotation(), CurrentTime);

			// Initialize buffer if needed
			if (ExistingMarker->PositionBuffer.Num() == 0)
			{
				ExistingMarker->PositionBuffer.Reserve(MaxBufferSize);
				ExistingMarker->PositionBuffer.Add(BufferedPos);
				ExistingMarker->bHasInitialPosition = true;
			}
			else if (ExistingMarker->PositionBuffer.Num() < MaxBufferSize)
			{
				// Buffer not full yet, just add
				ExistingMarker->PositionBuffer.Add(BufferedPos);
			}
			else
			{
				// Buffer is full, use circular write
				ExistingMarker->PositionBuffer[ExistingMarker->BufferWriteIndex] = BufferedPos;
				ExistingMarker->BufferWriteIndex = (ExistingMarker->BufferWriteIndex + 1) % MaxBufferSize;
			}
		}
		else if (bEnableDeadReckoning)
		{
			// Calculate velocity vector from speed and bearing
			// Convert km/h to Unreal units per second
			// Speed is in km/h, bearing is in degrees
			double SpeedUnitsPerSecond = (UserData.Speed * 1000.0 * CoordinateScale) / 3600.0; // km/h to units/sec
			float BearingRad = FMath::DegreesToRadians(UserData.Bearing);

			// GPS bearing: 0 = North (Y+), 90 = East (X+)
			// Create velocity vector in world space
			ExistingMarker->VelocityVector = FVector(
				SpeedUnitsPerSecond * FMath::Sin(BearingRad),  // X component (East)
				SpeedUnitsPerSecond * FMath::Cos(BearingRad),  // Y component (North)
				0.0f
			);

			// Set target position for interpolation
			ExistingMarker->TargetPosition = WorldLocation;

			// If this is first position, snap to it immediately
			if (!ExistingMarker->bHasInitialPosition)
			{
				ExistingMarker->CurrentPosition = WorldLocation;
				ExistingMarker->bHasInitialPosition = true;
			}

			// Update last update time
			ExistingMarker->LastUpdateTime = CurrentTime;
		}
		else
		{
			// No smoothing - snap to position immediately
			if (ExistingMarker->RootComponent)
			{
				ExistingMarker->RootComponent->SetWorldLocation(WorldLocation);
				ExistingMarker->RootComponent->SetWorldRotation(UserData.GetRotation());
			}
		}

		// Update text
		if (ExistingMarker->NameText && bShowUserNames)
		{
			ExistingMarker->NameText->SetText(FText::FromString(UserData.UserName));
		}

		if (ExistingMarker->SpeedText && bShowSpeed)
		{
			FString SpeedText = FString::Printf(TEXT("%.1f km/h"), UserData.Speed);
			ExistingMarker->SpeedText->SetText(FText::FromString(SpeedText));
		}

		// Add to trail (use current position for smooth trails)
		if (bDrawTrails)
		{
			FVector TrailPosition = WorldLocation;
			if (bUseInterpolationBuffer || bEnableDeadReckoning)
			{
				TrailPosition = ExistingMarker->CurrentPosition;
			}
			ExistingMarker->TrailPoints.Add(TrailPosition);
			if (ExistingMarker->TrailPoints.Num() > MaxTrailPoints)
			{
				ExistingMarker->TrailPoints.RemoveAt(0);
			}
		}

		ExistingMarker->LastData = UserData;
	}
	else
	{
		// Create new marker
		CreateUserMarker(UserData, WorldLocation);
	}
}

void AGPSTrackerVisualizerActor::RemoveUserVisualization(const FString& UserId)
{
	FUserMarker* Marker = UserMarkers.Find(UserId);
	if (Marker && Marker->RootComponent)
	{
		Marker->RootComponent->DestroyComponent();
		UserMarkers.Remove(UserId);
		UE_LOG(LogTemp, Log, TEXT("Removed visualization for user: %s"), *UserId);
	}
}

void AGPSTrackerVisualizerActor::CreateUserMarker_Implementation(const FGPSUserData& UserData, const FVector& WorldLocation)
{
	UE_LOG(LogTemp, Log, TEXT("Creating marker for user: %s at %s"), *UserData.UserName, *WorldLocation.ToString());

	FUserMarker NewMarker;

	// Create root component for this user
	NewMarker.RootComponent = NewObject<USceneComponent>(this, USceneComponent::StaticClass());
	NewMarker.RootComponent->RegisterComponent();
	NewMarker.RootComponent->AttachToComponent(GetRootComponent(), FAttachmentTransformRules::KeepRelativeTransform);
	NewMarker.RootComponent->SetWorldLocation(WorldLocation);
	NewMarker.RootComponent->SetWorldRotation(UserData.GetRotation());

	// Create mesh component (sphere)
	NewMarker.MarkerMesh = NewObject<UStaticMeshComponent>(NewMarker.RootComponent);
	NewMarker.MarkerMesh->RegisterComponent();
	NewMarker.MarkerMesh->AttachToComponent(NewMarker.RootComponent, FAttachmentTransformRules::KeepRelativeTransform);

	// Try to load sphere mesh
	UStaticMesh* SphereMesh = GetDefaultSphereMesh();
	if (SphereMesh)
	{
		NewMarker.MarkerMesh->SetStaticMesh(SphereMesh);
	}

	NewMarker.MarkerMesh->SetWorldScale3D(FVector(MarkerSize / 50.0f)); // Default sphere is ~50 units
	NewMarker.MarkerMesh->SetCollisionEnabled(ECollisionEnabled::NoCollision);

	// Create dynamic material to set color
	if (UMaterialInterface* Material = NewMarker.MarkerMesh->GetMaterial(0))
	{
		UMaterialInstanceDynamic* DynMaterial = UMaterialInstanceDynamic::Create(Material, this);
		if (DynMaterial)
		{
			DynMaterial->SetVectorParameterValue(FName("BaseColor"), DefaultMarkerColor);
			NewMarker.MarkerMesh->SetMaterial(0, DynMaterial);
		}
	}

	// Create name text component
	if (bShowUserNames)
	{
		NewMarker.NameText = NewObject<UTextRenderComponent>(NewMarker.RootComponent);
		NewMarker.NameText->RegisterComponent();
		NewMarker.NameText->AttachToComponent(NewMarker.RootComponent, FAttachmentTransformRules::KeepRelativeTransform);
		NewMarker.NameText->SetRelativeLocation(FVector(0, 0, MarkerSize + 20.0f));
		NewMarker.NameText->SetText(FText::FromString(UserData.UserName));
		NewMarker.NameText->SetWorldSize(TextSize);
		NewMarker.NameText->SetHorizontalAlignment(EHTA_Center);
		NewMarker.NameText->SetVerticalAlignment(EVRTA_TextBottom);
		NewMarker.NameText->SetTextRenderColor(FColor::White);
	}

	// Create speed text component
	if (bShowSpeed)
	{
		NewMarker.SpeedText = NewObject<UTextRenderComponent>(NewMarker.RootComponent);
		NewMarker.SpeedText->RegisterComponent();
		NewMarker.SpeedText->AttachToComponent(NewMarker.RootComponent, FAttachmentTransformRules::KeepRelativeTransform);
		NewMarker.SpeedText->SetRelativeLocation(FVector(0, 0, MarkerSize + 40.0f));

		FString SpeedText = FString::Printf(TEXT("%.1f km/h"), UserData.Speed);
		NewMarker.SpeedText->SetText(FText::FromString(SpeedText));
		NewMarker.SpeedText->SetWorldSize(TextSize * 0.8f);
		NewMarker.SpeedText->SetHorizontalAlignment(EHTA_Center);
		NewMarker.SpeedText->SetVerticalAlignment(EVRTA_TextBottom);
		NewMarker.SpeedText->SetTextRenderColor(FColor::Yellow);
	}

	// Initialize trail
	if (bDrawTrails)
	{
		NewMarker.TrailPoints.Add(WorldLocation);
	}

	NewMarker.LastData = UserData;

	// Initialize smoothing state
	double CurrentTime = GetWorld()->GetTimeSeconds();

	if (bUseInterpolationBuffer)
	{
		// Initialize interpolation buffer
		NewMarker.PositionBuffer.Reserve(MaxBufferSize);
		NewMarker.PositionBuffer.Add(FBufferedPosition(WorldLocation, UserData.GetRotation(), CurrentTime));
		NewMarker.CurrentPosition = WorldLocation;
		NewMarker.bHasInitialPosition = true;
		NewMarker.BufferWriteIndex = 0;
	}
	else if (bEnableDeadReckoning)
	{
		// Initialize dead reckoning state
		NewMarker.CurrentPosition = WorldLocation;
		NewMarker.TargetPosition = WorldLocation;
		NewMarker.bHasInitialPosition = true;
		NewMarker.LastUpdateTime = CurrentTime;

		// Calculate initial velocity vector
		double SpeedUnitsPerSecond = (UserData.Speed * 1000.0 * CoordinateScale) / 3600.0;
		float BearingRad = FMath::DegreesToRadians(UserData.Bearing);
		NewMarker.VelocityVector = FVector(
			SpeedUnitsPerSecond * FMath::Sin(BearingRad),
			SpeedUnitsPerSecond * FMath::Cos(BearingRad),
			0.0f
		);
	}

	// Add to map
	UserMarkers.Add(UserData.UserId, NewMarker);
}

void AGPSTrackerVisualizerActor::UpdateDeadReckoning(FUserMarker& Marker, float DeltaTime)
{
	if (!Marker.RootComponent || !Marker.bHasInitialPosition)
	{
		return;
	}

	double CurrentTime = GetWorld()->GetTimeSeconds();
	float TimeSinceLastUpdate = CurrentTime - Marker.LastUpdateTime;

	// Step 1: Interpolate towards target position (from last GPS update)
	FVector InterpolatedPosition = FMath::VInterpTo(
		Marker.CurrentPosition,
		Marker.TargetPosition,
		DeltaTime,
		PositionSmoothingFactor > 0.0f ? 1.0f / PositionSmoothingFactor : 10.0f
	);

	// Step 2: Apply dead reckoning prediction if user is moving
	FVector PredictedPosition = InterpolatedPosition;

	if (Marker.LastData.Speed >= MinSpeedForPrediction && TimeSinceLastUpdate <= MaxExtrapolationTime)
	{
		// Calculate how far to extrapolate based on time since last update
		float ExtrapolationFactor = FMath::Clamp(TimeSinceLastUpdate / MaxExtrapolationTime, 0.0f, 1.0f);

		// Apply damping to prevent over-shooting
		float DampedExtrapolation = ExtrapolationFactor * PredictionDampingFactor;

		// Calculate predicted offset from target position
		FVector PredictionOffset = Marker.VelocityVector * TimeSinceLastUpdate * DampedExtrapolation;

		// Add prediction to interpolated position
		PredictedPosition = InterpolatedPosition + PredictionOffset;
	}

	// Update marker position
	Marker.CurrentPosition = PredictedPosition;

	// Apply to visual component
	if (Marker.RootComponent)
	{
		Marker.RootComponent->SetWorldLocation(Marker.CurrentPosition);

		// Update rotation based on velocity direction (if moving)
		if (Marker.VelocityVector.SizeSquared() > 0.01f)
		{
			FRotator VelocityRotation = Marker.VelocityVector.Rotation();
			Marker.RootComponent->SetWorldRotation(VelocityRotation);
		}
		else
		{
			// Use bearing from GPS data
			Marker.RootComponent->SetWorldRotation(Marker.LastData.GetRotation());
		}
	}
}

FVector AGPSTrackerVisualizerActor::CalculatePredictedPosition(const FUserMarker& Marker, float TimeSinceLastUpdate) const
{
	// Simple dead reckoning: position = last_position + velocity * time
	FVector PredictedPosition = Marker.TargetPosition;

	if (Marker.LastData.Speed >= MinSpeedForPrediction && TimeSinceLastUpdate <= MaxExtrapolationTime)
	{
		// Calculate extrapolation factor with damping
		float ExtrapolationFactor = FMath::Clamp(TimeSinceLastUpdate / MaxExtrapolationTime, 0.0f, 1.0f);
		float DampedExtrapolation = ExtrapolationFactor * PredictionDampingFactor;

		// Add predicted movement
		PredictedPosition += Marker.VelocityVector * TimeSinceLastUpdate * DampedExtrapolation;
	}

	return PredictedPosition;
}

void AGPSTrackerVisualizerActor::UpdateInterpolationBuffer(FUserMarker& Marker, float DeltaTime)
{
	if (!Marker.RootComponent || !Marker.bHasInitialPosition || Marker.PositionBuffer.Num() < 2)
	{
		// Need at least 2 positions to interpolate
		if (Marker.bHasInitialPosition && Marker.PositionBuffer.Num() > 0)
		{
			// Only one position, just use it
			Marker.CurrentPosition = Marker.PositionBuffer[0].Position;
			Marker.RootComponent->SetWorldLocation(Marker.CurrentPosition);
			Marker.RootComponent->SetWorldRotation(Marker.PositionBuffer[0].Rotation);
		}
		return;
	}

	// Calculate render time (current time minus buffer delay)
	double CurrentTime = GetWorld()->GetTimeSeconds();
	double RenderTime = CurrentTime - InterpolationBufferTime;

	// Find the two positions to interpolate between
	FVector InterpolatedPosition = CalculateInterpolatedPosition(Marker, RenderTime);
	FRotator InterpolatedRotation = FRotator::ZeroRotator;

	// Find the two buffered positions surrounding RenderTime
	int32 OlderIndex = -1;
	int32 NewerIndex = -1;

	for (int32 i = 0; i < Marker.PositionBuffer.Num(); ++i)
	{
		if (Marker.PositionBuffer[i].Timestamp <= RenderTime)
		{
			if (OlderIndex == -1 || Marker.PositionBuffer[i].Timestamp > Marker.PositionBuffer[OlderIndex].Timestamp)
			{
				OlderIndex = i;
			}
		}
		if (Marker.PositionBuffer[i].Timestamp >= RenderTime)
		{
			if (NewerIndex == -1 || Marker.PositionBuffer[i].Timestamp < Marker.PositionBuffer[NewerIndex].Timestamp)
			{
				NewerIndex = i;
			}
		}
	}

	// Interpolate rotation as well
	if (OlderIndex >= 0 && NewerIndex >= 0 && OlderIndex != NewerIndex)
	{
		const FBufferedPosition& Older = Marker.PositionBuffer[OlderIndex];
		const FBufferedPosition& Newer = Marker.PositionBuffer[NewerIndex];

		double TimeDelta = Newer.Timestamp - Older.Timestamp;
		if (TimeDelta > 0.0)
		{
			float Alpha = FMath::Clamp((RenderTime - Older.Timestamp) / TimeDelta, 0.0, 1.0);
			InterpolatedRotation = FMath::Lerp(Older.Rotation, Newer.Rotation, Alpha);
		}
		else
		{
			InterpolatedRotation = Newer.Rotation;
		}
	}
	else if (OlderIndex >= 0)
	{
		InterpolatedRotation = Marker.PositionBuffer[OlderIndex].Rotation;
	}
	else if (NewerIndex >= 0)
	{
		InterpolatedRotation = Marker.PositionBuffer[NewerIndex].Rotation;
	}

	// Update marker
	Marker.CurrentPosition = InterpolatedPosition;
	Marker.RootComponent->SetWorldLocation(Marker.CurrentPosition);
	Marker.RootComponent->SetWorldRotation(InterpolatedRotation);
}

FVector AGPSTrackerVisualizerActor::CalculateInterpolatedPosition(const FUserMarker& Marker, double RenderTime) const
{
	if (Marker.PositionBuffer.Num() == 0)
	{
		return FVector::ZeroVector;
	}

	if (Marker.PositionBuffer.Num() == 1)
	{
		return Marker.PositionBuffer[0].Position;
	}

	// Find the two positions that bracket the render time
	int32 OlderIndex = -1;
	int32 NewerIndex = -1;

	for (int32 i = 0; i < Marker.PositionBuffer.Num(); ++i)
	{
		if (Marker.PositionBuffer[i].Timestamp <= RenderTime)
		{
			if (OlderIndex == -1 || Marker.PositionBuffer[i].Timestamp > Marker.PositionBuffer[OlderIndex].Timestamp)
			{
				OlderIndex = i;
			}
		}
		if (Marker.PositionBuffer[i].Timestamp >= RenderTime)
		{
			if (NewerIndex == -1 || Marker.PositionBuffer[i].Timestamp < Marker.PositionBuffer[NewerIndex].Timestamp)
			{
				NewerIndex = i;
			}
		}
	}

	// Interpolate between the two positions
	if (OlderIndex >= 0 && NewerIndex >= 0)
	{
		if (OlderIndex == NewerIndex)
		{
			// RenderTime exactly matches a buffered position
			return Marker.PositionBuffer[OlderIndex].Position;
		}

		const FBufferedPosition& Older = Marker.PositionBuffer[OlderIndex];
		const FBufferedPosition& Newer = Marker.PositionBuffer[NewerIndex];

		// Linear interpolation between the two positions
		double TimeDelta = Newer.Timestamp - Older.Timestamp;
		if (TimeDelta > 0.0)
		{
			float Alpha = FMath::Clamp((RenderTime - Older.Timestamp) / TimeDelta, 0.0, 1.0);
			return FMath::Lerp(Older.Position, Newer.Position, Alpha);
		}
		else
		{
			return Newer.Position;
		}
	}
	else if (OlderIndex >= 0)
	{
		// RenderTime is before all buffered positions, use oldest
		return Marker.PositionBuffer[OlderIndex].Position;
	}
	else if (NewerIndex >= 0)
	{
		// RenderTime is after all buffered positions, use newest
		return Marker.PositionBuffer[NewerIndex].Position;
	}

	// Fallback (shouldn't happen)
	return Marker.PositionBuffer[Marker.PositionBuffer.Num() - 1].Position;
}

AActor* AGPSTrackerVisualizerActor::GetCesiumGeoreference()
{
#if WITH_CESIUM
	// Return cached reference if available
	if (CesiumGeoreferenceActor && CesiumGeoreferenceActor->IsValidLowLevel())
	{
		return CesiumGeoreferenceActor;
	}

	// Search for Cesium Georeference actor in the level
	for (TActorIterator<AActor> It(GetWorld(), ACesiumGeoreference::StaticClass()); It; ++It)
	{
		CesiumGeoreferenceActor = *It;
		UE_LOG(LogTemp, Log, TEXT("GPSTrackerVisualizerActor: Found Cesium Georeference actor"));
		return CesiumGeoreferenceActor;
	}

	UE_LOG(LogTemp, Warning, TEXT("GPSTrackerVisualizerActor: Cesium Georeference actor not found in level"));
#else
	UE_LOG(LogTemp, Warning, TEXT("GPSTrackerVisualizerActor: Cesium plugin is not available (compiled without WITH_CESIUM)"));
#endif

	return nullptr;
}

FVector AGPSTrackerVisualizerActor::ConvertGPSToWorldPosition(double Latitude, double Longitude, float& OutTerrainHeight)
{
	OutTerrainHeight = 0.0f;

	// Use Cesium georeference if enabled and available
	if (bUseCesiumGeoreference)
	{
#if WITH_CESIUM
		AActor* GeoreferenceActor = GetCesiumGeoreference();
		if (GeoreferenceActor)
		{
			ACesiumGeoreference* Georeference = Cast<ACesiumGeoreference>(GeoreferenceActor);
			if (Georeference)
			{
				// Convert WGS84 coordinates to Unreal world position
				// Cesium uses glm::dvec3 for coordinates: longitude, latitude, height
				glm::dvec3 Coordinates(Longitude, Latitude, 0.0);

				// Transform to Unreal coordinates
				FVector UnrealPosition = Georeference->TransformLongitudeLatitudeHeightPositionToUnreal(Coordinates);

				// Sample terrain height if ground clamping is enabled
				if (bEnableGroundClamping)
				{
					float TerrainHeight = 0.0f;
					if (SampleCesiumTerrainHeight(Latitude, Longitude, TerrainHeight))
					{
						OutTerrainHeight = TerrainHeight;
						UnrealPosition.Z = TerrainHeight + GroundClampingOffset;
					}
					else
					{
						// Fallback to configured height offset if terrain sampling fails
						UnrealPosition.Z = GroundClampingOffset;
					}
				}
				else
				{
					// Apply configured height offset
					UnrealPosition.Z += MarkerHeightOffset;
				}

				return UnrealPosition;
			}
		}

		// Fallback warning if Cesium was requested but not available
		static bool bHasWarned = false;
		if (!bHasWarned)
		{
			UE_LOG(LogTemp, Warning, TEXT("GPSTrackerVisualizerActor: Cesium georeference requested but not available, falling back to simple Mercator projection"));
			bHasWarned = true;
		}
#else
		// Warn if Cesium is requested but not compiled
		static bool bHasWarned = false;
		if (!bHasWarned)
		{
			UE_LOG(LogTemp, Warning, TEXT("GPSTrackerVisualizerActor: Cesium georeference requested but plugin is not available (compiled without WITH_CESIUM)"));
			bHasWarned = true;
		}
#endif
	}

	// Fallback: Use simple Mercator projection
	FVector WorldPosition = FVector(
		Longitude * CoordinateScale,
		Latitude * CoordinateScale,
		MarkerHeightOffset
	);

	return WorldPosition;
}

bool AGPSTrackerVisualizerActor::SampleCesiumTerrainHeight(double Latitude, double Longitude, float& OutHeight)
{
#if WITH_CESIUM
	AActor* GeoreferenceActor = GetCesiumGeoreference();
	if (!GeoreferenceActor)
	{
		return false;
	}

	ACesiumGeoreference* Georeference = Cast<ACesiumGeoreference>(GeoreferenceActor);
	if (!Georeference)
	{
		return false;
	}

	// Convert GPS to world position at height 0
	glm::dvec3 Coordinates(Longitude, Latitude, 0.0);
	FVector WorldPosition = Georeference->TransformLongitudeLatitudeHeightPositionToUnreal(Coordinates);

	// Perform line trace downward to find terrain
	FHitResult HitResult;
	FVector TraceStart = WorldPosition + FVector(0.0f, 0.0f, 10000.0f); // Start high above
	FVector TraceEnd = WorldPosition - FVector(0.0f, 0.0f, 10000.0f);   // End far below

	FCollisionQueryParams QueryParams;
	QueryParams.AddIgnoredActor(this);
	QueryParams.bTraceComplex = true;

	// Perform line trace
	if (GetWorld()->LineTraceSingleByChannel(HitResult, TraceStart, TraceEnd, ECC_WorldStatic, QueryParams))
	{
		OutHeight = HitResult.Location.Z;
		return true;
	}

	// No terrain hit, try alternative method with sphere trace
	if (GetWorld()->SweepSingleByChannel(HitResult, TraceStart, TraceEnd, FQuat::Identity, ECC_WorldStatic,
		FCollisionShape::MakeSphere(50.0f), QueryParams))
	{
		OutHeight = HitResult.Location.Z;
		return true;
	}

	return false;
#else
	return false;
#endif
}

UStaticMesh* AGPSTrackerVisualizerActor::GetDefaultSphereMesh() const
{
	// Try to load engine's default sphere mesh
	static ConstructorHelpers::FObjectFinder<UStaticMesh> SphereMeshFinder(TEXT("/Engine/BasicShapes/Sphere"));
	if (SphereMeshFinder.Succeeded())
	{
		return SphereMeshFinder.Object;
	}

	// Fallback: try another path
	UStaticMesh* SphereMesh = LoadObject<UStaticMesh>(nullptr, TEXT("/Engine/BasicShapes/Sphere.Sphere"));
	if (SphereMesh)
	{
		return SphereMesh;
	}

	UE_LOG(LogTemp, Warning, TEXT("Failed to load default sphere mesh for GPS markers"));
	return nullptr;
}

// Copyright GPS Tracker Team. All Rights Reserved.

#include "GPSTrackerVisualizerActor.h"
#include "GPSTrackerSubsystem.h"
#include "Components/StaticMeshComponent.h"
#include "Components/TextRenderComponent.h"
#include "Engine/StaticMesh.h"
#include "UObject/ConstructorHelpers.h"
#include "Materials/MaterialInstanceDynamic.h"

AGPSTrackerVisualizerActor::AGPSTrackerVisualizerActor()
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
	// Calculate world position from GPS coordinates
	FVector WorldLocation = UserData.GetWorldPosition(CoordinateScale);
	WorldLocation.Z += MarkerHeightOffset;

	// Check if marker already exists
	FUserMarker* ExistingMarker = UserMarkers.Find(UserData.UserId);

	if (ExistingMarker)
	{
		// Update existing marker
		if (ExistingMarker->RootComponent)
		{
			ExistingMarker->RootComponent->SetWorldLocation(WorldLocation);
			ExistingMarker->RootComponent->SetWorldRotation(UserData.GetRotation());
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

		// Add to trail
		if (bDrawTrails)
		{
			ExistingMarker->TrailPoints.Add(WorldLocation);
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

	// Add to map
	UserMarkers.Add(UserData.UserId, NewMarker);
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

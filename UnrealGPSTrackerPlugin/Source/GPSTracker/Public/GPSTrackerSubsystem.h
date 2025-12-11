// Copyright GPS Tracker Team. All Rights Reserved.

#pragma once

#include "CoreMinimal.h"
#include "Subsystems/GameInstanceSubsystem.h"
#include "IWebSocket.h"
#include "GPSTrackerTypes.h"
#include "GPSTrackerSubsystem.generated.h"

/**
 * Game Instance Subsystem that manages WebSocket connection to GPS tracker server
 * and handles real-time position updates for all users in a group
 */
UCLASS()
class GPSTRACKER_API UGPSTrackerSubsystem : public UGameInstanceSubsystem
{
	GENERATED_BODY()

public:
	// Subsystem lifecycle
	virtual void Initialize(FSubsystemCollectionBase& Collection) override;
	virtual void Deinitialize() override;

	/**
	 * Connect to the GPS tracker server
	 * @param ServerURL - WebSocket server URL (default: wss://gps-tracker-server-production-5900.up.railway.app)
	 * @param InUserId - Unique user identifier (will be generated if empty)
	 * @param InUserName - Display name for the user
	 * @param InGroupName - Group name to join
	 */
	UFUNCTION(BlueprintCallable, Category = "GPS Tracker")
	void Connect(const FString& ServerURL = TEXT("wss://gps-tracker-server-production-5900.up.railway.app"),
				 const FString& InUserId = TEXT(""),
				 const FString& InUserName = TEXT("UnrealUser"),
				 const FString& InGroupName = TEXT("DefaultGroup"));

	/**
	 * Disconnect from the GPS tracker server
	 */
	UFUNCTION(BlueprintCallable, Category = "GPS Tracker")
	void Disconnect();

	/**
	 * Send a group horn alert to all group members
	 */
	UFUNCTION(BlueprintCallable, Category = "GPS Tracker")
	void SendGroupHorn();

	/**
	 * Send current position update to the server
	 * @param Latitude - Current latitude
	 * @param Longitude - Current longitude
	 * @param Speed - Current speed in km/h
	 * @param Bearing - Current bearing in degrees (0-360)
	 * @param MaxSpeed - Maximum speed recorded in the session
	 */
	UFUNCTION(BlueprintCallable, Category = "GPS Tracker")
	void SendPositionUpdate(double Latitude, double Longitude, double Speed, float Bearing, double MaxSpeed);

	/**
	 * Get current connection state
	 */
	UFUNCTION(BlueprintPure, Category = "GPS Tracker")
	EGPSTrackerConnectionState GetConnectionState() const { return ConnectionState; }

	/**
	 * Get list of all users in the group
	 */
	UFUNCTION(BlueprintPure, Category = "GPS Tracker")
	TArray<FGPSUserData> GetUsers() const { return Users; }

	/**
	 * Get a specific user by ID
	 */
	UFUNCTION(BlueprintPure, Category = "GPS Tracker")
	FGPSUserData GetUserById(const FString& UserId, bool& bFound) const;

	/**
	 * Check if connected to server
	 */
	UFUNCTION(BlueprintPure, Category = "GPS Tracker")
	bool IsConnected() const { return ConnectionState == EGPSTrackerConnectionState::Connected; }

	// Event delegates
	UPROPERTY(BlueprintAssignable, Category = "GPS Tracker")
	FOnConnectionStateChanged OnConnectionStateChanged;

	UPROPERTY(BlueprintAssignable, Category = "GPS Tracker")
	FOnUsersUpdated OnUsersUpdated;

	UPROPERTY(BlueprintAssignable, Category = "GPS Tracker")
	FOnGroupHorn OnGroupHorn;

	UPROPERTY(BlueprintAssignable, Category = "GPS Tracker")
	FOnTrackerError OnError;

private:
	// WebSocket instance
	TSharedPtr<IWebSocket> WebSocket;

	// Connection state
	EGPSTrackerConnectionState ConnectionState;

	// User data
	FString UserId;
	FString UserName;
	FString GroupName;

	// List of all users in the group
	TArray<FGPSUserData> Users;

	// Timer for keep-alive pings
	FTimerHandle PingTimerHandle;

	// WebSocket event handlers
	void OnConnected();
	void OnConnectionError(const FString& Error);
	void OnClosed(int32 StatusCode, const FString& Reason, bool bWasClean);
	void OnMessage(const FString& Message);
	void OnMessageSent(const FString& MessageString);

	// Message handlers
	void HandleUsersMessage(const TSharedPtr<FJsonObject>& JsonObject);
	void HandlePingMessage();

	// Helper functions
	void SendRegisterMessage();
	void SendPongMessage();
	void SetConnectionState(EGPSTrackerConnectionState NewState);
	FString GenerateUserId() const;
};

// Copyright GPS Tracker Team. All Rights Reserved.

#include "GPSTrackerSubsystem.h"
#include "WebSocketsModule.h"
#include "Json.h"
#include "JsonUtilities.h"
#include "Misc/Guid.h"
#include "TimerManager.h"

void UGPSTrackerSubsystem::Initialize(FSubsystemCollectionBase& Collection)
{
	Super::Initialize(Collection);

	ConnectionState = EGPSTrackerConnectionState::Disconnected;
	UE_LOG(LogTemp, Log, TEXT("GPSTrackerSubsystem initialized"));
}

void UGPSTrackerSubsystem::Deinitialize()
{
	Disconnect();
	Super::Deinitialize();
	UE_LOG(LogTemp, Log, TEXT("GPSTrackerSubsystem deinitialized"));
}

void UGPSTrackerSubsystem::Connect(const FString& ServerURL, const FString& InUserId, const FString& InUserName, const FString& InGroupName)
{
	// Disconnect if already connected
	if (WebSocket.IsValid() && WebSocket->IsConnected())
	{
		Disconnect();
	}

	// Set user data
	UserId = InUserId.IsEmpty() ? GenerateUserId() : InUserId;
	UserName = InUserName;
	GroupName = InGroupName;

	UE_LOG(LogTemp, Log, TEXT("Connecting to GPS Tracker Server: %s"), *ServerURL);
	UE_LOG(LogTemp, Log, TEXT("User: %s (%s), Group: %s"), *UserName, *UserId, *GroupName);

	SetConnectionState(EGPSTrackerConnectionState::Connecting);

	// Create WebSocket
	FModuleManager::LoadModuleChecked<FWebSocketsModule>("WebSockets");
	WebSocket = FWebSocketsModule::Get().CreateWebSocket(ServerURL, TEXT("wss"));

	// Bind event handlers
	WebSocket->OnConnected().AddUObject(this, &UGPSTrackerSubsystem::OnConnected);
	WebSocket->OnConnectionError().AddUObject(this, &UGPSTrackerSubsystem::OnConnectionError);
	WebSocket->OnClosed().AddUObject(this, &UGPSTrackerSubsystem::OnClosed);
	WebSocket->OnMessage().AddUObject(this, &UGPSTrackerSubsystem::OnMessage);
	WebSocket->OnMessageSent().AddUObject(this, &UGPSTrackerSubsystem::OnMessageSent);

	// Connect
	WebSocket->Connect();
}

void UGPSTrackerSubsystem::Disconnect()
{
	if (WebSocket.IsValid())
	{
		if (WebSocket->IsConnected())
		{
			WebSocket->Close();
		}
		WebSocket.Reset();
	}

	// Clear ping timer
	if (UWorld* World = GetWorld())
	{
		World->GetTimerManager().ClearTimer(PingTimerHandle);
	}

	Users.Empty();
	SetConnectionState(EGPSTrackerConnectionState::Disconnected);
	UE_LOG(LogTemp, Log, TEXT("Disconnected from GPS Tracker Server"));
}

void UGPSTrackerSubsystem::SendGroupHorn()
{
	if (!WebSocket.IsValid() || !WebSocket->IsConnected())
	{
		UE_LOG(LogTemp, Warning, TEXT("Cannot send group horn: not connected"));
		return;
	}

	// Create group horn message
	TSharedPtr<FJsonObject> JsonObject = MakeShared<FJsonObject>();
	JsonObject->SetStringField(TEXT("type"), TEXT("group-horn"));
	JsonObject->SetStringField(TEXT("userId"), UserId);

	// Serialize to string
	FString OutputString;
	TSharedRef<TJsonWriter<>> Writer = TJsonWriterFactory<>::Create(&OutputString);
	FJsonSerializer::Serialize(JsonObject.ToSharedRef(), Writer);

	// Send message
	WebSocket->Send(OutputString);
	UE_LOG(LogTemp, Log, TEXT("Sent group horn"));
}

void UGPSTrackerSubsystem::SendPositionUpdate(double Latitude, double Longitude, double Speed, float Bearing, double MaxSpeed)
{
	if (!WebSocket.IsValid() || !WebSocket->IsConnected())
	{
		UE_LOG(LogTemp, Warning, TEXT("Cannot send position update: not connected"));
		return;
	}

	// Create speed message
	TSharedPtr<FJsonObject> JsonObject = MakeShared<FJsonObject>();
	JsonObject->SetStringField(TEXT("type"), TEXT("speed"));
	JsonObject->SetStringField(TEXT("userId"), UserId);
	JsonObject->SetStringField(TEXT("userName"), UserName);
	JsonObject->SetStringField(TEXT("groupName"), GroupName);
	JsonObject->SetNumberField(TEXT("speed"), Speed);
	JsonObject->SetNumberField(TEXT("maxSpeed"), MaxSpeed);
	JsonObject->SetNumberField(TEXT("lat"), Latitude);
	JsonObject->SetNumberField(TEXT("lon"), Longitude);
	JsonObject->SetNumberField(TEXT("bearing"), Bearing);
	JsonObject->SetNumberField(TEXT("timestamp"), FDateTime::UtcNow().ToUnixTimestamp() * 1000);

	// Serialize to string
	FString OutputString;
	TSharedRef<TJsonWriter<>> Writer = TJsonWriterFactory<>::Create(&OutputString);
	FJsonSerializer::Serialize(JsonObject.ToSharedRef(), Writer);

	// Send message
	WebSocket->Send(OutputString);
}

FGPSUserData UGPSTrackerSubsystem::GetUserById(const FString& InUserId, bool& bFound) const
{
	for (const FGPSUserData& User : Users)
	{
		if (User.UserId == InUserId)
		{
			bFound = true;
			return User;
		}
	}

	bFound = false;
	return FGPSUserData();
}

void UGPSTrackerSubsystem::OnConnected()
{
	UE_LOG(LogTemp, Log, TEXT("Connected to GPS Tracker Server"));
	SetConnectionState(EGPSTrackerConnectionState::Connected);

	// Send register message
	SendRegisterMessage();

	// Start keep-alive timer (send pong every 25 seconds)
	if (UWorld* World = GetWorld())
	{
		World->GetTimerManager().SetTimer(
			PingTimerHandle,
			[this]()
			{
				// The server sends ping, we respond with pong
				// But we can also send periodic pongs as keep-alive
			},
			25.0f,
			true
		);
	}
}

void UGPSTrackerSubsystem::OnConnectionError(const FString& Error)
{
	UE_LOG(LogTemp, Error, TEXT("GPS Tracker connection error: %s"), *Error);
	SetConnectionState(EGPSTrackerConnectionState::Error);
	OnError.Broadcast(Error);
}

void UGPSTrackerSubsystem::OnClosed(int32 StatusCode, const FString& Reason, bool bWasClean)
{
	UE_LOG(LogTemp, Warning, TEXT("GPS Tracker connection closed: %d - %s (Clean: %d)"), StatusCode, *Reason, bWasClean);
	SetConnectionState(EGPSTrackerConnectionState::Disconnected);

	// Clear users list
	Users.Empty();
	OnUsersUpdated.Broadcast(Users);
}

void UGPSTrackerSubsystem::OnMessage(const FString& Message)
{
	// Parse JSON message
	TSharedPtr<FJsonObject> JsonObject;
	TSharedRef<TJsonReader<>> Reader = TJsonReaderFactory<>::Create(Message);

	if (!FJsonSerializer::Deserialize(Reader, JsonObject) || !JsonObject.IsValid())
	{
		UE_LOG(LogTemp, Warning, TEXT("Failed to parse GPS Tracker message: %s"), *Message);
		return;
	}

	// Get message type
	FString MessageType;
	if (!JsonObject->TryGetStringField(TEXT("type"), MessageType))
	{
		UE_LOG(LogTemp, Warning, TEXT("GPS Tracker message missing type field"));
		return;
	}

	// Handle different message types
	if (MessageType == TEXT("users"))
	{
		HandleUsersMessage(JsonObject);
	}
	else if (MessageType == TEXT("ping"))
	{
		HandlePingMessage();
	}
	else if (MessageType == TEXT("group-horn"))
	{
		UE_LOG(LogTemp, Log, TEXT("Received group horn alert"));
		OnGroupHorn.Broadcast();
	}
	else
	{
		UE_LOG(LogTemp, Log, TEXT("Received unknown message type: %s"), *MessageType);
	}
}

void UGPSTrackerSubsystem::OnMessageSent(const FString& MessageString)
{
	// Optional: log sent messages for debugging
	// UE_LOG(LogTemp, Verbose, TEXT("Sent message: %s"), *MessageString);
}

void UGPSTrackerSubsystem::HandleUsersMessage(const TSharedPtr<FJsonObject>& JsonObject)
{
	const TArray<TSharedPtr<FJsonValue>>* UsersArray;
	if (!JsonObject->TryGetArrayField(TEXT("users"), UsersArray))
	{
		UE_LOG(LogTemp, Warning, TEXT("Users message missing users array"));
		return;
	}

	// Clear current users list
	Users.Empty();

	// Parse each user
	for (const TSharedPtr<FJsonValue>& UserValue : *UsersArray)
	{
		const TSharedPtr<FJsonObject>* UserObject;
		if (!UserValue->TryGetObject(UserObject))
		{
			continue;
		}

		FGPSUserData UserData;
		(*UserObject)->TryGetStringField(TEXT("userId"), UserData.UserId);
		(*UserObject)->TryGetStringField(TEXT("userName"), UserData.UserName);
		(*UserObject)->TryGetStringField(TEXT("groupName"), UserData.GroupName);
		(*UserObject)->TryGetNumberField(TEXT("speed"), UserData.Speed);
		(*UserObject)->TryGetNumberField(TEXT("latitude"), UserData.Latitude);
		(*UserObject)->TryGetNumberField(TEXT("longitude"), UserData.Longitude);

		double BearingDouble;
		if ((*UserObject)->TryGetNumberField(TEXT("bearing"), BearingDouble))
		{
			UserData.Bearing = static_cast<float>(BearingDouble);
		}

		(*UserObject)->TryGetNumberField(TEXT("timestamp"), UserData.Timestamp);

		Users.Add(UserData);
	}

	UE_LOG(LogTemp, Log, TEXT("Received users update: %d users"), Users.Num());

	// Broadcast event
	OnUsersUpdated.Broadcast(Users);
}

void UGPSTrackerSubsystem::HandlePingMessage()
{
	// Respond with pong
	SendPongMessage();
}

void UGPSTrackerSubsystem::SendRegisterMessage()
{
	if (!WebSocket.IsValid() || !WebSocket->IsConnected())
	{
		return;
	}

	// Create register message
	TSharedPtr<FJsonObject> JsonObject = MakeShared<FJsonObject>();
	JsonObject->SetStringField(TEXT("type"), TEXT("register"));
	JsonObject->SetStringField(TEXT("userId"), UserId);
	JsonObject->SetStringField(TEXT("userName"), UserName);
	JsonObject->SetStringField(TEXT("groupName"), GroupName);

	// Serialize to string
	FString OutputString;
	TSharedRef<TJsonWriter<>> Writer = TJsonWriterFactory<>::Create(&OutputString);
	FJsonSerializer::Serialize(JsonObject.ToSharedRef(), Writer);

	// Send message
	WebSocket->Send(OutputString);
	UE_LOG(LogTemp, Log, TEXT("Sent register message"));
}

void UGPSTrackerSubsystem::SendPongMessage()
{
	if (!WebSocket.IsValid() || !WebSocket->IsConnected())
	{
		return;
	}

	// Create pong message
	TSharedPtr<FJsonObject> JsonObject = MakeShared<FJsonObject>();
	JsonObject->SetStringField(TEXT("type"), TEXT("pong"));

	// Serialize to string
	FString OutputString;
	TSharedRef<TJsonWriter<>> Writer = TJsonWriterFactory<>::Create(&OutputString);
	FJsonSerializer::Serialize(JsonObject.ToSharedRef(), Writer);

	// Send message
	WebSocket->Send(OutputString);
}

void UGPSTrackerSubsystem::SetConnectionState(EGPSTrackerConnectionState NewState)
{
	if (ConnectionState != NewState)
	{
		ConnectionState = NewState;
		OnConnectionStateChanged.Broadcast(NewState);
	}
}

FString UGPSTrackerSubsystem::GenerateUserId() const
{
	// Generate a unique user ID using GUID
	return FGuid::NewGuid().ToString();
}

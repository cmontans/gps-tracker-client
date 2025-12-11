// Copyright GPS Tracker Team. All Rights Reserved.

using System;
using System.Collections;
using System.Collections.Generic;
using System.Text;
using UnityEngine;
using UnityEngine.Networking;

#if !UNITY_WEBGL || UNITY_EDITOR
using NativeWebSocket;
#endif

namespace GPSTracker
{
    /// <summary>
    /// Connection state for GPS Tracker
    /// </summary>
    public enum GPSTrackerConnectionState
    {
        Disconnected,
        Connecting,
        Connected,
        Error
    }

    /// <summary>
    /// Manages WebSocket connection to GPS tracker server and handles real-time position updates
    /// This is a singleton MonoBehaviour that persists across scenes
    /// </summary>
    public class GPSTrackerManager : MonoBehaviour
    {
        // Singleton instance
        private static GPSTrackerManager _instance;
        public static GPSTrackerManager Instance
        {
            get
            {
                if (_instance == null)
                {
                    GameObject go = new GameObject("GPSTrackerManager");
                    _instance = go.AddComponent<GPSTrackerManager>();
                    DontDestroyOnLoad(go);
                }
                return _instance;
            }
        }

        // Events
        public event Action<GPSTrackerConnectionState> OnConnectionStateChanged;
        public event Action<List<GPSUserData>> OnUsersUpdated;
        public event Action OnGroupHorn;
        public event Action<string> OnError;

        // Connection state
        [SerializeField] private GPSTrackerConnectionState _connectionState = GPSTrackerConnectionState.Disconnected;
        public GPSTrackerConnectionState ConnectionState
        {
            get => _connectionState;
            private set
            {
                if (_connectionState != value)
                {
                    _connectionState = value;
                    OnConnectionStateChanged?.Invoke(value);
                }
            }
        }

        // User data
        private string _userId = "";
        private string _userName = "UnityUser";
        private string _groupName = "DefaultGroup";
        private List<GPSUserData> _users = new List<GPSUserData>();

        // WebSocket
#if !UNITY_WEBGL || UNITY_EDITOR
        private WebSocket _webSocket;
#endif

        // Keep-alive
        private Coroutine _pingCoroutine;
        private const float PingInterval = 30.0f;

        public bool IsConnected => ConnectionState == GPSTrackerConnectionState.Connected;
        public List<GPSUserData> Users => new List<GPSUserData>(_users);

        private void Awake()
        {
            if (_instance != null && _instance != this)
            {
                Destroy(gameObject);
                return;
            }
            _instance = this;
            DontDestroyOnLoad(gameObject);
        }

        private void Update()
        {
#if !UNITY_WEBGL || UNITY_EDITOR
            _webSocket?.DispatchMessageQueue();
#endif
        }

        private void OnDestroy()
        {
            if (_instance == this)
            {
                Disconnect();
            }
        }

        private void OnApplicationQuit()
        {
            Disconnect();
        }

        /// <summary>
        /// Connect to GPS tracker server
        /// </summary>
        /// <param name="serverUrl">WebSocket server URL</param>
        /// <param name="userId">Unique user identifier (auto-generated if empty)</param>
        /// <param name="userName">Display name</param>
        /// <param name="groupName">Group name to join</param>
        public async void Connect(string serverUrl = "wss://gps-tracker-server-production-5900.up.railway.app",
                                   string userId = "",
                                   string userName = "UnityUser",
                                   string groupName = "DefaultGroup")
        {
            if (ConnectionState == GPSTrackerConnectionState.Connected ||
                ConnectionState == GPSTrackerConnectionState.Connecting)
            {
                Debug.LogWarning("Already connected or connecting to GPS Tracker server");
                return;
            }

            _userId = string.IsNullOrEmpty(userId) ? GPSUtilities.GenerateUserId() : userId;
            _userName = userName;
            _groupName = groupName;

            ConnectionState = GPSTrackerConnectionState.Connecting;

#if !UNITY_WEBGL || UNITY_EDITOR
            try
            {
                _webSocket = new WebSocket(serverUrl);

                _webSocket.OnOpen += OnWebSocketOpen;
                _webSocket.OnMessage += OnWebSocketMessage;
                _webSocket.OnError += OnWebSocketError;
                _webSocket.OnClose += OnWebSocketClose;

                await _webSocket.Connect();
            }
            catch (Exception ex)
            {
                Debug.LogError($"Failed to connect to GPS Tracker server: {ex.Message}");
                ConnectionState = GPSTrackerConnectionState.Error;
                OnError?.Invoke(ex.Message);
            }
#else
            Debug.LogError("WebGL platform requires different WebSocket implementation");
            ConnectionState = GPSTrackerConnectionState.Error;
            OnError?.Invoke("WebGL not supported with NativeWebSocket library");
#endif
        }

        /// <summary>
        /// Disconnect from GPS tracker server
        /// </summary>
        public async void Disconnect()
        {
            if (_pingCoroutine != null)
            {
                StopCoroutine(_pingCoroutine);
                _pingCoroutine = null;
            }

#if !UNITY_WEBGL || UNITY_EDITOR
            if (_webSocket != null)
            {
                await _webSocket.Close();
                _webSocket = null;
            }
#endif

            ConnectionState = GPSTrackerConnectionState.Disconnected;
            _users.Clear();
        }

        /// <summary>
        /// Send position update to server
        /// </summary>
        public void SendPositionUpdate(double latitude, double longitude, double speed, float bearing, double maxSpeed)
        {
            if (!IsConnected)
            {
                Debug.LogWarning("Cannot send position update - not connected");
                return;
            }

            var message = new
            {
                type = "speed",
                userId = _userId,
                userName = _userName,
                speed,
                latitude,
                longitude,
                bearing,
                timestamp = GPSUtilities.GetCurrentTimestamp(),
                groupName = _groupName
            };

            SendMessage(message);
        }

        /// <summary>
        /// Send group horn alert
        /// </summary>
        public void SendGroupHorn()
        {
            if (!IsConnected)
            {
                Debug.LogWarning("Cannot send group horn - not connected");
                return;
            }

            var message = new
            {
                type = "group-horn",
                userId = _userId,
                groupName = _groupName
            };

            SendMessage(message);
        }

        /// <summary>
        /// Get user by ID
        /// </summary>
        public GPSUserData GetUserById(string userId)
        {
            return _users.Find(u => u.userId == userId);
        }

        // WebSocket event handlers
        private void OnWebSocketOpen()
        {
            Debug.Log("Connected to GPS Tracker server");
            ConnectionState = GPSTrackerConnectionState.Connected;

            // Send registration message
            SendRegisterMessage();

            // Start keep-alive ping
            _pingCoroutine = StartCoroutine(PingCoroutine());
        }

        private void OnWebSocketMessage(byte[] data)
        {
            string message = Encoding.UTF8.GetString(data);
            HandleMessage(message);
        }

        private void OnWebSocketError(string error)
        {
            Debug.LogError($"GPS Tracker WebSocket error: {error}");
            ConnectionState = GPSTrackerConnectionState.Error;
            OnError?.Invoke(error);
        }

        private void OnWebSocketClose(WebSocketCloseCode closeCode)
        {
            Debug.Log($"GPS Tracker WebSocket closed: {closeCode}");
            ConnectionState = GPSTrackerConnectionState.Disconnected;

            if (_pingCoroutine != null)
            {
                StopCoroutine(_pingCoroutine);
                _pingCoroutine = null;
            }
        }

        // Message handling
        private void HandleMessage(string messageJson)
        {
            try
            {
                var message = JsonUtility.FromJson<MessageWrapper>(messageJson);

                switch (message.type)
                {
                    case "users":
                        HandleUsersMessage(messageJson);
                        break;
                    case "ping":
                        SendPongMessage();
                        break;
                    case "group-horn":
                        OnGroupHorn?.Invoke();
                        break;
                }
            }
            catch (Exception ex)
            {
                Debug.LogError($"Error handling message: {ex.Message}");
            }
        }

        private void HandleUsersMessage(string messageJson)
        {
            try
            {
                var wrapper = JsonUtility.FromJson<UsersMessageWrapper>(messageJson);
                _users = new List<GPSUserData>(wrapper.users);
                OnUsersUpdated?.Invoke(_users);
            }
            catch (Exception ex)
            {
                Debug.LogError($"Error parsing users message: {ex.Message}");
            }
        }

        private void SendRegisterMessage()
        {
            var message = new
            {
                type = "register",
                userId = _userId,
                userName = _userName,
                groupName = _groupName
            };

            SendMessage(message);
        }

        private void SendPongMessage()
        {
            var message = new
            {
                type = "pong"
            };

            SendMessage(message);
        }

        private async void SendMessage(object message)
        {
#if !UNITY_WEBGL || UNITY_EDITOR
            if (_webSocket != null && _webSocket.State == WebSocketState.Open)
            {
                string json = JsonUtility.ToJson(message);
                await _webSocket.SendText(json);
            }
#endif
        }

        private IEnumerator PingCoroutine()
        {
            while (IsConnected)
            {
                yield return new WaitForSeconds(PingInterval);
                // Server sends ping, we respond with pong - no need to initiate
            }
        }

        // Helper classes for JSON parsing
        [Serializable]
        private class MessageWrapper
        {
            public string type;
        }

        [Serializable]
        private class UsersMessageWrapper
        {
            public string type;
            public GPSUserData[] users;
        }
    }
}

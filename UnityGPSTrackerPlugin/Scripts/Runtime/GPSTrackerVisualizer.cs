// Copyright GPS Tracker Team. All Rights Reserved.

using System;
using System.Collections.Generic;
using UnityEngine;

namespace GPSTracker
{
    /// <summary>
    /// MonoBehaviour that visualizes GPS tracker user positions in the world
    /// Automatically creates and updates visual representations for each user in the group
    /// </summary>
    public class GPSTrackerVisualizer : MonoBehaviour
    {
        [Header("GPS Coordinate Conversion")]
        [Tooltip("Scale factor for converting GPS coordinates to Unity world coordinates. Default: 100000 = 1 degree = 100km")]
        public float coordinateScale = 100000.0f;

        [Tooltip("Height offset for spawning user markers (Y axis)")]
        public float markerHeightOffset = 2.0f;

        [Header("Marker Appearance")]
        [Tooltip("Size of the user marker sphere")]
        public float markerSize = 1.0f;

        [Tooltip("Whether to show user names above markers")]
        public bool showUserNames = true;

        [Tooltip("Whether to show speed information")]
        public bool showSpeed = true;

        [Tooltip("Default color for user markers")]
        public Color defaultMarkerColor = Color.blue;

        [Header("Movement Trails")]
        [Tooltip("Whether to draw trails behind moving users")]
        public bool drawTrails = true;

        [Tooltip("Maximum number of trail points per user")]
        public int maxTrailPoints = 100;

        [Tooltip("Color of movement trails")]
        public Color trailColor = Color.cyan;

        [Tooltip("Width of trail lines")]
        public float trailWidth = 0.1f;

        [Header("Dead Reckoning")]
        [Tooltip("Enable dead reckoning to smooth position updates between GPS readings")]
        public bool enableDeadReckoning = true;

        [Tooltip("Smoothing factor for position interpolation (0 = instant snap, 1 = very smooth)")]
        [Range(0.0f, 1.0f)]
        public float positionSmoothingFactor = 0.15f;

        [Tooltip("Maximum time in seconds to extrapolate position beyond last GPS update")]
        [Range(0.0f, 30.0f)]
        public float maxExtrapolationTime = 5.0f;

        [Tooltip("Minimum speed (km/h) required to apply dead reckoning prediction")]
        public float minSpeedForPrediction = 1.0f;

        [Tooltip("Factor to reduce prediction accuracy over time (prevents over-shooting)")]
        [Range(0.0f, 1.0f)]
        public float predictionDampingFactor = 0.8f;

        [Header("Interpolation Buffer")]
        [Tooltip("Use interpolation buffer instead of dead reckoning for position smoothing")]
        public bool useInterpolationBuffer = false;

        [Tooltip("Time in seconds to delay rendering for interpolation buffer")]
        [Range(0.05f, 2.0f)]
        public float interpolationBufferTime = 0.2f;

        [Tooltip("Maximum number of positions to store in interpolation buffer")]
        [Range(2, 50)]
        public int maxBufferSize = 10;

        [Header("Prefabs (Optional)")]
        [Tooltip("Custom prefab for user markers. If null, creates default sphere")]
        public GameObject userMarkerPrefab;

        // Events
        public event Action<List<GPSUserData>> OnUsersVisualizationUpdated;

        // Internal state
        private Dictionary<string, UserMarker> _userMarkers = new Dictionary<string, UserMarker>();
        private List<GPSUserData> _currentUsers = new List<GPSUserData>();

        private void Start()
        {
            // Subscribe to GPS Tracker events
            if (GPSTrackerManager.Instance != null)
            {
                GPSTrackerManager.Instance.OnUsersUpdated += HandleUsersUpdated;
            }
        }

        private void OnDestroy()
        {
            // Unsubscribe from events
            if (GPSTrackerManager.Instance != null)
            {
                GPSTrackerManager.Instance.OnUsersUpdated -= HandleUsersUpdated;
            }

            // Clean up all markers
            foreach (var marker in _userMarkers.Values)
            {
                if (marker.gameObject != null)
                {
                    Destroy(marker.gameObject);
                }
            }
            _userMarkers.Clear();
        }

        private void Update()
        {
            // Update all markers with dead reckoning or interpolation
            foreach (var kvp in _userMarkers)
            {
                UpdateMarkerPosition(kvp.Value, Time.deltaTime);
            }
        }

        private void HandleUsersUpdated(List<GPSUserData> users)
        {
            _currentUsers = users;

            // Update or create markers for active users
            HashSet<string> activeUserIds = new HashSet<string>();
            foreach (var user in users)
            {
                activeUserIds.Add(user.userId);
                UpdateUserVisualization(user);
            }

            // Remove markers for users no longer in the list
            List<string> toRemove = new List<string>();
            foreach (var userId in _userMarkers.Keys)
            {
                if (!activeUserIds.Contains(userId))
                {
                    toRemove.Add(userId);
                }
            }

            foreach (var userId in toRemove)
            {
                RemoveUserVisualization(userId);
            }

            OnUsersVisualizationUpdated?.Invoke(users);
        }

        private void UpdateUserVisualization(GPSUserData userData)
        {
            if (!_userMarkers.TryGetValue(userData.userId, out UserMarker marker))
            {
                // Create new marker
                marker = CreateUserMarker(userData);
                _userMarkers[userData.userId] = marker;
            }

            // Update marker data
            UpdateMarkerData(marker, userData);
        }

        private UserMarker CreateUserMarker(GPSUserData userData)
        {
            GameObject markerObject;

            if (userMarkerPrefab != null)
            {
                markerObject = Instantiate(userMarkerPrefab, transform);
            }
            else
            {
                markerObject = CreateDefaultMarker();
            }

            markerObject.name = $"GPSMarker_{userData.userName}";

            UserMarker marker = new UserMarker
            {
                gameObject = markerObject,
                userData = userData.Clone(),
                currentPosition = Vector3.zero,
                targetPosition = Vector3.zero,
                velocityVector = Vector3.zero,
                lastUpdateTime = Time.time,
                hasInitialPosition = false,
                positionBuffer = new List<BufferedPosition>(),
                trailPoints = new List<Vector3>(),
                lineRenderer = markerObject.GetComponent<LineRenderer>()
            };

            // Setup trail line renderer if needed
            if (drawTrails && marker.lineRenderer == null)
            {
                marker.lineRenderer = markerObject.AddComponent<LineRenderer>();
                marker.lineRenderer.startWidth = trailWidth;
                marker.lineRenderer.endWidth = trailWidth;
                marker.lineRenderer.material = new Material(Shader.Find("Sprites/Default"));
                marker.lineRenderer.startColor = trailColor;
                marker.lineRenderer.endColor = trailColor;
                marker.lineRenderer.positionCount = 0;
            }

            // Setup name text
            if (showUserNames)
            {
                CreateMarkerText(markerObject, userData.userName, 1.5f, "NameText");
            }

            // Setup speed text
            if (showSpeed)
            {
                CreateMarkerText(markerObject, "", 1.0f, "SpeedText");
            }

            return marker;
        }

        private GameObject CreateDefaultMarker()
        {
            GameObject marker = GameObject.CreatePrimitive(PrimitiveType.Sphere);
            marker.transform.SetParent(transform);
            marker.transform.localScale = Vector3.one * markerSize;

            Renderer renderer = marker.GetComponent<Renderer>();
            if (renderer != null)
            {
                renderer.material.color = defaultMarkerColor;
            }

            return marker;
        }

        private void CreateMarkerText(GameObject parent, string text, float yOffset, string name)
        {
            GameObject textObject = new GameObject(name);
            textObject.transform.SetParent(parent.transform);
            textObject.transform.localPosition = new Vector3(0, yOffset, 0);

            TextMesh textMesh = textObject.AddComponent<TextMesh>();
            textMesh.text = text;
            textMesh.fontSize = 20;
            textMesh.color = Color.white;
            textMesh.anchor = TextAnchor.MiddleCenter;
            textMesh.alignment = TextAlignment.Center;

            // Make text face camera
            textObject.AddComponent<Billboard>();
        }

        private void UpdateMarkerData(UserMarker marker, GPSUserData userData)
        {
            Vector3 worldPos = userData.GetWorldPosition(coordinateScale);
            worldPos.y = markerHeightOffset;

            marker.userData = userData.Clone();
            marker.targetPosition = worldPos;
            marker.velocityVector = userData.GetVelocityVector() * coordinateScale;
            marker.lastUpdateTime = Time.time;

            if (!marker.hasInitialPosition)
            {
                marker.currentPosition = worldPos;
                marker.hasInitialPosition = true;
            }

            // Add to interpolation buffer if enabled
            if (useInterpolationBuffer)
            {
                marker.positionBuffer.Add(new BufferedPosition
                {
                    position = worldPos,
                    rotation = userData.GetRotation(),
                    timestamp = Time.time
                });

                // Keep buffer size limited
                if (marker.positionBuffer.Count > maxBufferSize)
                {
                    marker.positionBuffer.RemoveAt(0);
                }
            }

            // Update marker text
            UpdateMarkerText(marker);
        }

        private void UpdateMarkerText(UserMarker marker)
        {
            Transform nameTextTransform = marker.gameObject.transform.Find("NameText");
            if (nameTextTransform != null)
            {
                TextMesh textMesh = nameTextTransform.GetComponent<TextMesh>();
                if (textMesh != null)
                {
                    textMesh.text = marker.userData.userName;
                }
            }

            Transform speedTextTransform = marker.gameObject.transform.Find("SpeedText");
            if (speedTextTransform != null)
            {
                TextMesh textMesh = speedTextTransform.GetComponent<TextMesh>();
                if (textMesh != null)
                {
                    textMesh.text = GPSUtilities.FormatSpeed(marker.userData.speed);
                    textMesh.color = GPSUtilities.GetSpeedColor(marker.userData.speed);
                }
            }
        }

        private void UpdateMarkerPosition(UserMarker marker, float deltaTime)
        {
            Vector3 newPosition;

            if (useInterpolationBuffer)
            {
                newPosition = CalculateInterpolatedPosition(marker);
            }
            else if (enableDeadReckoning)
            {
                newPosition = CalculateDeadReckoningPosition(marker, deltaTime);
            }
            else
            {
                newPosition = marker.targetPosition;
            }

            marker.currentPosition = newPosition;
            marker.gameObject.transform.position = newPosition;
            marker.gameObject.transform.rotation = marker.userData.GetRotation();

            // Update trail
            if (drawTrails)
            {
                UpdateTrail(marker);
            }
        }

        private Vector3 CalculateDeadReckoningPosition(UserMarker marker, float deltaTime)
        {
            float timeSinceUpdate = Time.time - marker.lastUpdateTime;

            // Interpolate toward target position
            Vector3 interpolatedPos = Vector3.Lerp(
                marker.currentPosition,
                marker.targetPosition,
                positionSmoothingFactor
            );

            // Apply prediction if speed is above threshold and within extrapolation time
            if (marker.userData.speed >= minSpeedForPrediction && timeSinceUpdate <= maxExtrapolationTime)
            {
                // Calculate damping based on time since update
                float dampingMultiplier = Mathf.Lerp(1.0f, predictionDampingFactor, timeSinceUpdate / maxExtrapolationTime);

                // Add velocity-based prediction
                Vector3 prediction = marker.velocityVector * dampingMultiplier * deltaTime;
                return interpolatedPos + prediction;
            }

            return interpolatedPos;
        }

        private Vector3 CalculateInterpolatedPosition(UserMarker marker)
        {
            if (marker.positionBuffer.Count < 2)
            {
                return marker.targetPosition;
            }

            float renderTime = Time.time - interpolationBufferTime;

            // Find positions to interpolate between
            for (int i = 0; i < marker.positionBuffer.Count - 1; i++)
            {
                if (marker.positionBuffer[i].timestamp <= renderTime &&
                    marker.positionBuffer[i + 1].timestamp >= renderTime)
                {
                    float t = (renderTime - marker.positionBuffer[i].timestamp) /
                              (marker.positionBuffer[i + 1].timestamp - marker.positionBuffer[i].timestamp);

                    return Vector3.Lerp(
                        marker.positionBuffer[i].position,
                        marker.positionBuffer[i + 1].position,
                        t
                    );
                }
            }

            // If render time is beyond buffer, use most recent position
            return marker.positionBuffer[marker.positionBuffer.Count - 1].position;
        }

        private void UpdateTrail(UserMarker marker)
        {
            // Add current position to trail
            if (marker.trailPoints.Count == 0 ||
                Vector3.Distance(marker.currentPosition, marker.trailPoints[marker.trailPoints.Count - 1]) > 0.1f)
            {
                marker.trailPoints.Add(marker.currentPosition);

                // Limit trail points
                if (marker.trailPoints.Count > maxTrailPoints)
                {
                    marker.trailPoints.RemoveAt(0);
                }
            }

            // Update line renderer
            if (marker.lineRenderer != null)
            {
                marker.lineRenderer.positionCount = marker.trailPoints.Count;
                marker.lineRenderer.SetPositions(marker.trailPoints.ToArray());
            }
        }

        private void RemoveUserVisualization(string userId)
        {
            if (_userMarkers.TryGetValue(userId, out UserMarker marker))
            {
                if (marker.gameObject != null)
                {
                    Destroy(marker.gameObject);
                }
                _userMarkers.Remove(userId);
            }
        }

        // Internal classes
        private class UserMarker
        {
            public GameObject gameObject;
            public GPSUserData userData;
            public Vector3 currentPosition;
            public Vector3 targetPosition;
            public Vector3 velocityVector;
            public float lastUpdateTime;
            public bool hasInitialPosition;
            public List<BufferedPosition> positionBuffer;
            public List<Vector3> trailPoints;
            public LineRenderer lineRenderer;
        }

        private struct BufferedPosition
        {
            public Vector3 position;
            public Quaternion rotation;
            public float timestamp;
        }
    }

    /// <summary>
    /// Helper component to make text face camera
    /// </summary>
    public class Billboard : MonoBehaviour
    {
        private Camera _mainCamera;

        private void Start()
        {
            _mainCamera = Camera.main;
        }

        private void LateUpdate()
        {
            if (_mainCamera != null)
            {
                transform.LookAt(transform.position + _mainCamera.transform.rotation * Vector3.forward,
                                 _mainCamera.transform.rotation * Vector3.up);
            }
        }
    }
}

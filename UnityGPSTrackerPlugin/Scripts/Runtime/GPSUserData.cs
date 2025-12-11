// Copyright GPS Tracker Team. All Rights Reserved.

using System;
using UnityEngine;

namespace GPSTracker
{
    /// <summary>
    /// Represents a user's position data from the GPS tracker server
    /// </summary>
    [Serializable]
    public class GPSUserData
    {
        public string userId;
        public string userName;
        public double speed;
        public double latitude;
        public double longitude;
        public float bearing;
        public long timestamp;
        public string groupName;

        public GPSUserData()
        {
            userId = "";
            userName = "";
            speed = 0.0;
            latitude = 0.0;
            longitude = 0.0;
            bearing = 0.0f;
            timestamp = 0;
            groupName = "";
        }

        /// <summary>
        /// Convert latitude/longitude to Unity world position
        /// Using a simple Mercator projection scaled for visualization
        /// </summary>
        /// <param name="scale">Scale factor for world coordinates (default 100000 = 1 degree = 100km in Unity units)</param>
        /// <returns>World position in Unity coordinates</returns>
        public Vector3 GetWorldPosition(float scale = 100000.0f)
        {
            // Simple Mercator projection
            // X = Longitude, Z = Latitude, Y = 0 (ground level)
            return new Vector3(
                (float)(longitude * scale),
                0.0f,
                (float)(latitude * scale)
            );
        }

        /// <summary>
        /// Get rotation from bearing (0-360 degrees, where 0 = North)
        /// </summary>
        /// <returns>Unity Quaternion rotation</returns>
        public Quaternion GetRotation()
        {
            // Convert GPS bearing to Unity rotation
            // GPS: 0 = North, 90 = East, 180 = South, 270 = West
            // Unity: Y-axis rotation (0 = Forward/North, 90 = Right/East)
            return Quaternion.Euler(0.0f, bearing, 0.0f);
        }

        /// <summary>
        /// Get velocity vector based on speed and bearing
        /// </summary>
        /// <returns>Velocity vector in world space (meters/second)</returns>
        public Vector3 GetVelocityVector()
        {
            // Convert km/h to m/s
            float speedMS = (float)(speed / 3.6);

            // Calculate velocity components from bearing
            float bearingRad = bearing * Mathf.Deg2Rad;

            // GPS bearing: 0 = North (Z+), 90 = East (X+)
            return new Vector3(
                Mathf.Sin(bearingRad) * speedMS,
                0.0f,
                Mathf.Cos(bearingRad) * speedMS
            );
        }

        /// <summary>
        /// Create a copy of this user data
        /// </summary>
        public GPSUserData Clone()
        {
            return new GPSUserData
            {
                userId = this.userId,
                userName = this.userName,
                speed = this.speed,
                latitude = this.latitude,
                longitude = this.longitude,
                bearing = this.bearing,
                timestamp = this.timestamp,
                groupName = this.groupName
            };
        }
    }
}

// Copyright GPS Tracker Team. All Rights Reserved.

using System;
using UnityEngine;

namespace GPSTracker
{
    /// <summary>
    /// Utility functions for GPS coordinate conversion and calculations
    /// </summary>
    public static class GPSUtilities
    {
        private const double EarthRadiusKm = 6371.0;

        /// <summary>
        /// Convert GPS coordinates to Unity world position
        /// </summary>
        /// <param name="latitude">GPS latitude</param>
        /// <param name="longitude">GPS longitude</param>
        /// <param name="scale">Scale factor (default: 100000)</param>
        /// <param name="heightOffset">Y-axis offset</param>
        /// <returns>Unity world position</returns>
        public static Vector3 GPSToWorldPosition(double latitude, double longitude, float scale = 100000.0f, float heightOffset = 0.0f)
        {
            return new Vector3(
                (float)(longitude * scale),
                heightOffset,
                (float)(latitude * scale)
            );
        }

        /// <summary>
        /// Convert Unity world position to GPS coordinates
        /// </summary>
        /// <param name="worldPosition">Unity world position</param>
        /// <param name="scale">Scale factor (default: 100000)</param>
        /// <param name="latitude">Output latitude</param>
        /// <param name="longitude">Output longitude</param>
        public static void WorldPositionToGPS(Vector3 worldPosition, float scale, out double latitude, out double longitude)
        {
            longitude = worldPosition.x / scale;
            latitude = worldPosition.z / scale;
        }

        /// <summary>
        /// Calculate distance between two GPS coordinates using Haversine formula
        /// </summary>
        /// <param name="lat1">First point latitude</param>
        /// <param name="lon1">First point longitude</param>
        /// <param name="lat2">Second point latitude</param>
        /// <param name="lon2">Second point longitude</param>
        /// <returns>Distance in kilometers</returns>
        public static double CalculateGPSDistance(double lat1, double lon1, double lat2, double lon2)
        {
            double dLat = (lat2 - lat1) * Mathf.Deg2Rad;
            double dLon = (lon2 - lon1) * Mathf.Deg2Rad;

            double a = Math.Sin(dLat / 2) * Math.Sin(dLat / 2) +
                       Math.Cos(lat1 * Mathf.Deg2Rad) * Math.Cos(lat2 * Mathf.Deg2Rad) *
                       Math.Sin(dLon / 2) * Math.Sin(dLon / 2);

            double c = 2 * Math.Atan2(Math.Sqrt(a), Math.Sqrt(1 - a));

            return EarthRadiusKm * c;
        }

        /// <summary>
        /// Calculate bearing between two GPS coordinates
        /// </summary>
        /// <param name="lat1">Start point latitude</param>
        /// <param name="lon1">Start point longitude</param>
        /// <param name="lat2">End point latitude</param>
        /// <param name="lon2">End point longitude</param>
        /// <returns>Bearing in degrees (0-360, where 0 = North)</returns>
        public static float CalculateGPSBearing(double lat1, double lon1, double lat2, double lon2)
        {
            double dLon = (lon2 - lon1) * Mathf.Deg2Rad;
            double lat1Rad = lat1 * Mathf.Deg2Rad;
            double lat2Rad = lat2 * Mathf.Deg2Rad;

            double y = Math.Sin(dLon) * Math.Cos(lat2Rad);
            double x = Math.Cos(lat1Rad) * Math.Sin(lat2Rad) -
                       Math.Sin(lat1Rad) * Math.Cos(lat2Rad) * Math.Cos(dLon);

            double bearing = Math.Atan2(y, x) * Mathf.Rad2Deg;

            // Normalize to 0-360
            return (float)((bearing + 360.0) % 360.0);
        }

        /// <summary>
        /// Format speed for display
        /// </summary>
        /// <param name="speedKmh">Speed in km/h</param>
        /// <param name="showUnit">Whether to include "km/h" suffix</param>
        /// <returns>Formatted speed string</returns>
        public static string FormatSpeed(double speedKmh, bool showUnit = true)
        {
            if (showUnit)
            {
                return $"{speedKmh:F1} km/h";
            }
            return $"{speedKmh:F1}";
        }

        /// <summary>
        /// Get color based on speed (green = slow, red = fast)
        /// </summary>
        /// <param name="speedKmh">Current speed in km/h</param>
        /// <param name="maxSpeed">Maximum speed for color scale</param>
        /// <returns>Color gradient from green to red</returns>
        public static Color GetSpeedColor(double speedKmh, double maxSpeed = 100.0)
        {
            if (maxSpeed <= 0) maxSpeed = 100.0;

            float normalizedSpeed = Mathf.Clamp01((float)(speedKmh / maxSpeed));

            // Green to Yellow to Red gradient
            if (normalizedSpeed < 0.5f)
            {
                // Green to Yellow
                return Color.Lerp(Color.green, Color.yellow, normalizedSpeed * 2.0f);
            }
            else
            {
                // Yellow to Red
                return Color.Lerp(Color.yellow, Color.red, (normalizedSpeed - 0.5f) * 2.0f);
            }
        }

        /// <summary>
        /// Generate a unique user ID
        /// </summary>
        /// <returns>Unique user identifier</returns>
        public static string GenerateUserId()
        {
            return $"unity_{SystemInfo.deviceUniqueIdentifier}_{DateTime.UtcNow.Ticks}";
        }

        /// <summary>
        /// Get current Unix timestamp in milliseconds
        /// </summary>
        /// <returns>Timestamp in milliseconds since epoch</returns>
        public static long GetCurrentTimestamp()
        {
            return DateTimeOffset.UtcNow.ToUnixTimeMilliseconds();
        }
    }
}

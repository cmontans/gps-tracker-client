# GPS Tracker - Garmin User Manual

Welcome to the Garmin edition of the GPS Kitesurfing Tracker! This watch app is designed to monitor your speed and calculate your jumps in real-time, operating completely as a standalone tool on your wrist.

## 1. Getting Started
1. Open your Garmin watch's **Activities & Apps** menu (usually by pressing the top-right button).
2. Scroll to and select **"GPS Tracker"**.
3. **Wait for GPS**: The application will immediately activate the built-in GPS and Barometric altimeter. The status at the bottom will read **"Waiting for GPS..."** until it has a solid satellite lock.
4. **Tracking**: Once locked, the status will change to **"Tracking"** and you are ready to ride.

## 2. Main Screen Layout
The primary interface is designed to be highly glancing so you can read it quickly in the water:
* **Speed (Center-Top):** Displays your current live speed converted to Knots (e.g., `18.5 kts`).
* **Max Jump (Center):** Displays the maximum height of your highest jump during the current session (e.g., `12.5m`).
* **Status (Bottom):** Displays the GPS fix status.

## 3. Detecting Jumps
The watch automatically detects your jumps by reading internal G-forces from the accelerometer and smoothing altitude variations from the onboard barometer. 
* **Takeoff:** Jump detection starts when you edge hard and pop (the vertical G-forces spike above the threshold).
* **Landing:** Airtime concludes when the watch senses the heavy impact of your board hitting the water.
* **Notification:** As soon as you land safely, the main screen instantly updates if your new jump height beats your session's maximum.

## 4. Viewing Jump History
You don't need to guess if the app caught your last trick. You can scan your entire session's jumps on the watch:
1. From the main tracking screen, press the **Select / Enter** button (usually the Top-Right button).
2. The **Jump History Menu** will slide into view.
3. Use the Up/Down buttons to scroll through your recent jumps. The history displays in reverse chronological order (newest first).
4. **Reading the Menu:** The bold text indicates your jump height (e.g., `14.2m`) and the secondary text displays your hangtime (e.g., `1.8s`).
5. **Exit:** Press the **Back** button (Bottom-Right) to exit history and return to the live tracking screen.

## 5. Session Reset
The application retains your Max Jump and up to 50 of your recent jumps across application resets to prevent accidental data loss. To completely flush out an old session and reset the Max Jump counter to `0.0m`, you simply reboot the watch application before entering the water. 

## 6. Pairing with Android (Optional Feature)
If you have the GPS Tracker Companion App installed on your Android phone, and the phone is kept nearby (within Bluetooth range inside a waterproof pouch or bag):
* The Garmin app will automatically transmit your live coordinates and recorded jumps silently to the phone.
* The phone will then forward these statistics over WebSockets to the centralized tracking server.
*(Note: If the companion app is turned off or not in range, the Garmin watch simply ignores external routing and behaves as an offline local tracker—your jump metrics are never lost).*

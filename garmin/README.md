# Garmin Connect IQ Companion App

This directory contains the source code for the Garmin Connect IQ client for the GPS Tracker, matching the primary functionalities of the Wear OS application (Speed tracking, Kitesurfing Jump calculation, and Jump History persistence).

## Prerequisites
1. **Visual Studio Code**: Download and install [VS Code](https://code.visualstudio.com/).
2. **Connect IQ SDK Manager**: Needs to be installed to download device profiles (like Fenix and Instinct).
3. **Monkey C Extension**: Install the [Monkey C Extension](https://marketplace.visualstudio.com/items?itemName=garmin.monkey-c) from the VS Code extensions marketplace.

## 1. Initial Setup

### Open the Project
In VS Code, open the folder specifically pointing to this directory:
`File` > `Open Folder` > `e:\Carlos\GitHub\gps-tracker-client\garmin`
*This is required for the Monkey C extension to successfully detect the `monkey.jungle` and `manifest.xml` files root contexts.*

### Generate a Developer Key
Garmin applications must be cryptographically signed to run on simulators or sideloaded onto devices.
1. Press `Ctrl+Shift+P` (or `Cmd+Shift+P` on Mac).
2. Type and run `Monkey C: Generate a Developer Key`.
3. Save the `.der` file reliably located on your computer (e.g., `e:\Carlos\developer_key.der`).

### Download Device Profiles
1. Press `Ctrl+Shift+P`.
2. Type and run `Monkey C: Open SDK Manager`.
3. In the SDK Manager, download your corresponding Garmin watch profiles (e.g. `fenix7`, `instinct2`, `epix2`).
*Note: If your watch is not listed in `manifest.xml`, you will also need to add `<iq:product id="YOUR_WATCH_ID"/>` to the `<iq:products>` block inside the `manifest.xml` file.*

## 2. Compiling and Testing

### Testing In Simulator
You can test the app on your computer using the Garmin Simulator before pushing it to your watch:
1. Press `Ctrl+Shift+P` and select `Monkey C: Build and Run`.
2. Select your device (like `instinct2_sim`).
3. The Simulator application will pop up.
**Pro Tip**: You can simulate the GPS & barometric jumps by navigating to `Data Fields > Fit Data` inside the Simulator window and feeding it a pre-recorded `.fit` activity file!

### Sideloading To Your Watch
Once ready, you can deploy the standalone application directly to your watch via USB:
1. Connect your Garmin to your computer via USB Cable.
2. Press `Ctrl+Shift+P` and select `Monkey C: Build For Device`.
3. Choose your watch from the device list.
4. VS Code will generate a `.prg` compiled binary inside the `/bin` directory.
5. Copy the `.prg` file and paste it into the `GARMIN/APPS/` folder on your mounted watch drive.
6. Safely eject the watch. You will now see "GPS Tracker" in your activity or apps list!

## 3. Server Connectivity Note (Future)
Unlike Wear OS, Connect IQ **does not support long-lived WebSocket connections**.
Currently, the `CommListener.mc` attempts to broadcast jumps and speeds natively over Bluetooth using `Communications.transmit()`.
To complete the loop to your backend node server, you must integrate the **Connect IQ Android SDK** (downloadable manually from the Garmin Dev Portal). By wrapping the `.aar` file into the `android/app` folder, your phone can receive Bluetooth payloads from the watch and forward them down your existing `GPSWebSocketClient` tube.

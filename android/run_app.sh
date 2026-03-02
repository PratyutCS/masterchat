#!/bin/bash

# Configuration
APP_ID="com.masterapp.chat"
LAUNCHER_ACTIVITY=".ui.LoginActivity"
PROJECT_DIR="/home/pratyut/Desktop/master_app/android"
APK_PATH="app/build/outputs/apk/debug/app-debug.apk"
ADB="/home/pratyut/Android/Sdk/platform-tools/adb"

# Check if a device is connected and get the first serial
SERIAL=$($ADB devices | grep -v "List" | grep "device" | head -n 1 | awk '{print $1}')

if [ -z "$SERIAL" ]; then
    echo "Error: No Android device or emulator connected."
    exit 1
fi

DEVICE_COUNT=$($ADB devices | grep -v "List" | grep "device" | wc -l)
if [ "$DEVICE_COUNT" -gt 1 ]; then
    echo "⚠️  Multiple devices detected. Targeting: $SERIAL"
fi

echo "🚀 Starting Build Process..."
cd "$PROJECT_DIR" || exit

# 1. Compile the app
echo "📦 Compiling debug APK..."
./gradlew assembleDebug

if [ $? -ne 0 ]; then
    echo "❌ Build failed!"
    exit 1
fi

# 2. Install the app
echo "📲 Uninstalling old version from $SERIAL..."
$ADB -s "$SERIAL" uninstall "$APP_ID"

echo "📲 Installing APK on $SERIAL..."
$ADB -s "$SERIAL" install -r "$APK_PATH"

if [ $? -ne 0 ]; then
    echo "❌ Installation failed!"
    exit 1
fi

# 3. Launch the app
echo "✨ Launching $APP_ID..."
$ADB -s "$SERIAL" shell am start -n "$APP_ID/$LAUNCHER_ACTIVITY"

if [ $? -ne 0 ]; then
    echo "❌ Launch failed!"
    exit 1
fi

echo "✅ Success! App is running."

#!/system/bin/sh

SKIPUNZIP=0

ui_print "- Extracting module files..."

# Fix permissions for both the virtual system and vendor configuration trees
ui_print "- Configuring file permissions and security contexts..."
chown -R 0:0 "$MODPATH/system" "$MODPATH/vendor" 2>/dev/null
find "$MODPATH/system" "$MODPATH/vendor" -type d -exec chmod 755 {} + 2>/dev/null
find "$MODPATH/system" "$MODPATH/vendor" -type f -exec chmod 644 {} + 2>/dev/null

# Locate APK 
STAGED_APK="$MODPATH/common/TensorCardEmulator.apk"

if [ -f "$STAGED_APK" ]; then
    ui_print "- Automatically installing TensorCardEmulator as user app..."
    
    # Force background package manager to install the app cleanly
    # -r: reinstall, -d: allow version downgrade
    pm install -r -d --user 0 "$STAGED_APK"
    
    if [ $? -eq 0 ]; then
        ui_print "- App successfully installed in user space!"
    else
        ui_print "! Error: Package manager rejected the APK installation."
    fi
else
    ui_print "! Error: TensorCardEmulator.apk missing from common/ folder."
fi

# Create backup folder for original NFC files
mkdir -p /data/adb/TensorCardEmulator

# Run NFC Backup and Spoofing Configuration
ui_print "- Backing up and spoofing STMicroelectronics NFC configurations..."

# Create necessary tracking directories
mkdir -p /data/adb/TensorCardEmulator
mkdir -p "$MODPATH/system/vendor/etc"

# copy for backup
cp -f /vendor/etc/libnfc-hal-st.conf /data/adb/TensorCardEmulator/ 2>/dev/null
cp -f /vendor/etc/libnfc-hal-st-proto1.conf /data/adb/TensorCardEmulator/ 2>/dev/null

# copy for spoofing
cp -f /vendor/etc/libnfc-hal-st.conf "$MODPATH/system/vendor/etc/" 2>/dev/null
cp -f /vendor/etc/libnfc-hal-st-proto1.conf "$MODPATH/system/vendor/etc/" 2>/dev/null

ui_print "- Extraction and configuration successful!"
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
if cp -f /vendor/etc/libnfc-hal-st.conf /data/adb/TensorCardEmulator/ 2>/dev/null; then
    ui_print "- Backed up libnfc-hal-st.conf"
else
    ui_print "! Warning: libnfc-hal-st.conf not found, NFC config spoofing may not work"
fi

if cp -f /vendor/etc/libnfc-hal-st-proto1.conf /data/adb/TensorCardEmulator/ 2>/dev/null; then
    ui_print "- Backed up libnfc-hal-st-proto1.conf"
else
    ui_print "! Warning: libnfc-hal-st-proto1.conf not found"
fi

# copy for spoofing
cp -f /vendor/etc/libnfc-hal-st.conf "$MODPATH/system/vendor/etc/" 2>/dev/null
cp -f /vendor/etc/libnfc-hal-st-proto1.conf "$MODPATH/system/vendor/etc/" 2>/dev/null

set_perm "$MODPATH/uninstall.sh" root root 0755

ui_print "- Extraction and configuration successful!"


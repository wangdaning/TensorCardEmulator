#!/system/bin/sh

MODDIR="/data/adb/modules/TensorCardEmulator"

# Find the exact path to APK and mount
REAL_APK_PATH=$(find /data/app -type f -name "TensorCardEmulator.apk" -o -path "*/ru.extreames.tensorcardemulator*/*.apk" | head -n 1)

if [ -f "$MODDIR/assets/TensorCardEmulator.apk" ]; then
    mkdir -p "$MODDIR/system/product/priv-app/TensorCardEmulator"
    
    mount --bind "$MODDIR/assets/TensorCardEmulator.apk" "$MODDIR/system/product/priv-app/TensorCardEmulator/TensorCardEmulator.apk"
fi

# Prepare NFC backup/Spoofing directories
ui_print "- Backing up and spoofing STMicroelectronics NFC configurations..."

mkdir -p /data/adb/TensorCardEmulator
mkdir -p "$MODPATH/system/vendor/etc"

# backup
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

# spoofing
cp -f /vendor/etc/libnfc-hal-st.conf "$MODPATH/system/vendor/etc/" 2>/dev/null
cp -f /vendor/etc/libnfc-hal-st-proto1.conf "$MODPATH/system/vendor/etc/" 2>/dev/null


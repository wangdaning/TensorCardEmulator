#!/system/bin/sh

MODDIR="/data/adb/modules/TensorCardEmulator"

# Prepare NFC backup/Spoofing directories
echo "- Backing up and spoofing STMicroelectronics NFC configurations..."

mkdir -p /data/adb/TensorCardEmulator
mkdir -p "$MODDIR/system/vendor/etc"

# backup
if cp -f /vendor/etc/libnfc-hal-st.conf /data/adb/TensorCardEmulator/ 2>/dev/null; then
    echo "- Backed up libnfc-hal-st.conf"
else
    echo "! Warning: libnfc-hal-st.conf not found, NFC config spoofing may not work"
fi

if cp -f /vendor/etc/libnfc-hal-st-proto1.conf /data/adb/TensorCardEmulator/ 2>/dev/null; then
    echo "- Backed up libnfc-hal-st-proto1.conf"
else
    echo "! Warning: libnfc-hal-st-proto1.conf not found"
fi

# spoofing
cp -f /vendor/etc/libnfc-hal-st.conf "$MODDIR/system/vendor/etc/" 2>/dev/null
cp -f /vendor/etc/libnfc-hal-st-proto1.conf "$MODDIR/system/vendor/etc/" 2>/dev/null


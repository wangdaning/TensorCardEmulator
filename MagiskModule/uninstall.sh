#!/system/bin/sh

PKG="ru.extreames.tensorcardemulator"
PKGXML="/data/system/packages.xml"

if [ -f "/data/adb/TensorCardEmulator/libnfc-nci.conf.bak" ]; then
    cp -f "/data/adb/TensorCardEmulator/libnfc-nci.conf.bak" "/data/vendor/nfc/libnfc-nci.conf" 2>/dev/null
fi

pm uninstall --user 0 "$PKG" 2>/dev/null

if [ -f "${PKGXML}.tce_bak" ]; then
    cp -f "${PKGXML}.tce_bak" "$PKGXML"
    rm -f "${PKGXML}.tce_bak" "${PKGXML}.tce_pre_perms"
fi

rm -rf /data/adb/TensorCardEmulator

APKSIGNER=$(ps -A -o NAME | grep -i 'hardware.nfc' | head -1)
if [ -n "$APKSIGNER" ]; then
    killall "$APKSIGNER" 2>/dev/null
fi
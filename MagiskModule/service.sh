#!/system/bin/sh

MODDIR="/data/adb/modules/TensorCardEmulator"
PKG="ru.extreames.tensorcardemulator"
LOG="$MODDIR/service.log"

exec > "$LOG" 2>&1
echo "[$(date)] TensorCardEmulator service.sh starting"

# Wait for boot to complete
until [ "$(getprop sys.boot_completed)" = "1" ]; do
    sleep 2
done

# Actively wait until PackageManager binder is ready
until pm list packages > /dev/null 2>&1; do
    echo "[$(date)] Waiting for PackageManager to be ready..."
    sleep 3
done
echo "[$(date)] PackageManager is ready"

# Re-copy NFC vendor configs in case they changed after a system update
for CONF in libnfc-hal-st.conf libnfc-hal-st-proto1.conf; do
    if [ -f "/vendor/etc/$CONF" ]; then
        cp -f "/vendor/etc/$CONF" "$MODDIR/system/vendor/etc/$CONF"
        echo "[$(date)] Refreshed $CONF from vendor"
    else
        echo "[$(date)] Warning: /vendor/etc/$CONF not found, skipping"
    fi
done

# Install APK if missing
APK="$MODDIR/common/TensorCardEmulator.apk"
if ! pm list packages 2>/dev/null | grep -q "^package:${PKG}$"; then
    if [ -f "$APK" ]; then
        echo "[$(date)] Installing APK..."
        INSTALL_RESULT=$(pm install -r -d --user 0 "$APK" 2>&1)
        echo "[$(date)] pm install result: $INSTALL_RESULT"
        if pm list packages 2>/dev/null | grep -q "^package:${PKG}$"; then
            echo "[$(date)] APK installed successfully"
        else
            echo "[$(date)] APK install failed: $INSTALL_RESULT"
        fi
        sleep 4
    else
        echo "[$(date)] ERROR: APK not found at $APK"
    fi
else
    echo "[$(date)] Package already installed, skipping pm install"
fi

echo "[$(date)] service.sh complete."
#!/system/bin/sh

MODDIR=${0%/*}

# 2. Find the exact path to APK
REAL_APK_PATH=$(find /data/app -type f -name "TensorCardEmulator.apk" -o -path "*/ru.extreames.tensorcardemulator*/*.apk" | head -n 1)

# Mount bind the genuine package location directly to the privilege frame
if [ -n "$REAL_APK_PATH" ]; then
    mkdir -p "$MODDIR/system/product/priv-app/TensorCardEmulator"
    mount --bind "$REAL_APK_PATH" "$MODDIR/system/product/priv-app/TensorCardEmulator/TensorCardEmulator.apk"
fi

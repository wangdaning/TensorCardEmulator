#!/system/bin/sh

# copy for backup
cp -f /system/vendor/etc/libnfc-hal-st.conf /data/adb/TensorCardEmulator/ 2>/dev/null
cp -f /system/vendor/etc/libnfc-hal-st-proto1.conf /data/adb/TensorCardEmulator/ 2>/dev/null

# copy for spoofing
cp -f /system/vendor/etc/libnfc-hal-st.conf /data/adb/modules/TensorCardEmulator/system/vendor/etc/ 2>/dev/null
cp -f /system/vendor/etc/libnfc-hal-st-proto1.conf /data/adb/modules/TensorCardEmulator/system/vendor/etc/ 2>/dev/null
#!/sbin/sh

# 1. Prevent the installer script from messing up directory mapping
SKIPUNZIP=0

ui_print "- Forcing Android 16 Product Partition Layout..."

# 2. Re-verify the internal tracking paths are created cleanly inside the module directory
mkdir -p $MODPATH/system/product/priv-app/TensorCardEmulator
mkdir -p $MODPATH/system/product/etc/permissions
mkdir -p $MODPATH/vendor/etc

# 3. Explicitly set permissions for everything so the package manager registers them
set_perm_recursive $MODPATH/system/product/priv-app 0 0 0755 0644
set_perm_recursive $MODPATH/system/product/etc/permissions 0 0 0755 0644
set_perm_recursive $MODPATH/vendor/etc 0 0 0755 0644

# for backup original NFC files
mkdir -p /data/adb/TensorCardEmulator

ui_print "- Extraction permission overrides set successfully."

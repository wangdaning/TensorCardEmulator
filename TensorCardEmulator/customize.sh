#!/system/bin/sh

SKIPMOUNT=false

# 1. Clean up to avoid conflicts
rm -rf "$MODPATH/system/priv-app"
rm -rf "$MODPATH/system/vendor"

mkdir -p "$MODPATH/staging"

if [ -f "$MODPATH/common/TensorCardEmulator.apk" ]; then
    cp "$MODPATH/common/TensorCardEmulator.apk" "$MODPATH/staging/TensorCardEmulator.apk"
else
    ui_print "! Error: TensorCardEmulator.apk not found in common/ folder."
    abort
fi

# Install apk
ui_print "- Sideloading apk as user..."
pm install -r -g --user 0 "$MODPATH/staging/TensorCardEmulator.apk"

ui_print "- Flash installation complete. Processing boot scripts..."

set_perm "$MODPATH/uninstall.sh" root root 0755

ui_print "- Extraction and configuration successful!"


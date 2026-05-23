#!/system/bin/sh

SKIPMOUNT=false

rm -rf "$MODPATH/system/priv-app"
rm -rf "$MODPATH/system/vendor"

if [ ! -f "$MODPATH/common/TensorCardEmulator.apk" ]; then
    ui_print "! Error: TensorCardEmulator.apk not found in common/"
    abort
fi

ui_print "- APK staged"

set_perm "$MODPATH/service.sh" root root 0755
set_perm "$MODPATH/uninstall.sh" root root 0755

ui_print "- Flash complete. App will install on next boot."


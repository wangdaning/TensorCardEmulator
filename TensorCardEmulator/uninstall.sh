#!/system/bin/sh

PKG="ru.extreames.tensorcardemulator"
BACKUP_DIR="/data/adb/TensorCardEmulator"
PKGXML="/data/system/packages.xml"

# Uninstall the app
if pm list packages 2>/dev/null | grep -q "^package:${PKG}$"; then
    pm uninstall --user 0 "$PKG"
fi

# Restore original packages.xml if we patched it
if [ -f "${PKGXML}.tce_bak" ]; then
    cp -f "${PKGXML}.tce_bak" "$PKGXML"
    rm -f "${PKGXML}.tce_bak"
    rm -f "${PKGXML}.tce_pre_perms"
fi

# Remove backup folder
rm -rf "$BACKUP_DIR"
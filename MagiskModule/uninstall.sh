#!/system/bin/sh

PKG="ru.extreames.tensorcardemulator"
PKGXML="/data/system/packages.xml"

pm uninstall --user 0 "$PKG" 2>/dev/null

if [ -f "${PKGXML}.tce_bak" ]; then
    cp -f "${PKGXML}.tce_bak" "$PKGXML"
    rm -f "${PKGXML}.tce_bak" "${PKGXML}.tce_pre_perms"
fi

rm -rf /data/adb/TensorCardEmulator
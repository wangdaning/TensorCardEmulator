#!/system/bin/sh

MODDIR="/data/adb/modules/TensorCardEmulator"
PKG="ru.extreames.tensorcardemulator"
PKGXML="/data/system/packages.xml"
PKGXML_BAK="/data/system/packages.xml.tce_bak"

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

# Only patch packages.xml if the app is already installed
if [ ! -f "$PKGXML" ]; then
    exit 0
fi

if ! grep -q "\"${PKG}\"" "$PKGXML"; then
    # App not installed yet, nothing to patch
    exit 0
fi

# Patch FLAG_SYSTEM
CURRENT_FLAGS=$(grep -A2 "\"${PKG}\"" "$PKGXML" | grep -o 'flags="[0-9]*"' | head -1 | grep -o '[0-9]*')
if [ -n "$CURRENT_FLAGS" ]; then
    NEW_FLAGS=$(( CURRENT_FLAGS | 1 ))
    if [ "$NEW_FLAGS" != "$CURRENT_FLAGS" ]; then
        cp "$PKGXML" "$PKGXML_BAK"
        awk -v pkg="$PKG" -v old="$CURRENT_FLAGS" -v new="$NEW_FLAGS" '
            /package name="/ && $0 ~ pkg { found=1 }
            found && /flags="/ {
                sub("flags=\"" old "\"", "flags=\"" new "\"")
                found=0
            }
            { print }
        ' "$PKGXML_BAK" > "$PKGXML"
    fi
fi

# Inject NFC permissions
if ! grep -A20 "\"${PKG}\"" "$PKGXML" | grep -q "NFC_PREFERRED_PAYMENT_SERVICE"; then
    cp "$PKGXML" "${PKGXML}.tce_pre_perms"
    awk -v pkg="$PKG" '
        /package name="/ && $0 ~ pkg { in_pkg=1 }
        in_pkg && /<perms>/ {
            print
            print "            <item name=\"android.permission.NFC\" granted=\"true\" flags=\"0\" />"
            print "            <item name=\"android.permission.BIND_NFC_SERVICE\" granted=\"true\" flags=\"0\" />"
            print "            <item name=\"android.permission.NFC_PREFERRED_PAYMENT_SERVICE\" granted=\"true\" flags=\"0\" />"
            print "            <item name=\"android.permission.NFC_PREFERRED_PAYMENT_INFO\" granted=\"true\" flags=\"0\" />"
            print "            <item name=\"android.permission.CHANGE_NFC_STATE\" granted=\"true\" flags=\"0\" />"
            next
        }
        in_pkg && /<\/package>/ { in_pkg=0 }
        { print }
    ' "${PKGXML}.tce_pre_perms" > "$PKGXML"
fi
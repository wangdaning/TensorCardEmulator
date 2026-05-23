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
# Extra grace period for PackageManager to finish scanning
sleep 8

# Re-copy NFC vendor configs in case they changed
MODPATH="/data/adb/modules/TensorCardEmulator"

for CONF in libnfc-hal-st.conf libnfc-hal-st-proto1.conf; do
    if [ -f "/vendor/etc/$CONF" ]; then
        cp -f "/vendor/etc/$CONF" "$MODPATH/system/vendor/etc/$CONF"
        echo "[$(date)] Refreshed $CONF from vendor"
    else
        echo "[$(date)] Warning: /vendor/etc/$CONF not found, skipping"
    fi
done

# Install APK
APK="$MODDIR/common/TensorCardEmulator.apk"
if ! pm list packages 2>/dev/null | grep -q "^package:${PKG}$"; then
    if [ -f "$APK" ]; then
        echo "[$(date)] Installing APK..."
        pm install -r --bypass-low-target-sdk-block "$APK"
        sleep 4
    else
        echo "[$(date)] ERROR: app.apk not found at $APK"
    fi
else
    echo "[$(date)] Package already installed, skipping pm install"
fi

# Patch packages.xml to add SYSTEM flag 

PKGXML="/data/system/packages.xml"
PKGXML_BAK="/data/system/packages.xml.tce_bak"

if [ -f "$PKGXML" ]; then
    # Only patch if not already patched (avoid double-patching)
    if grep -q "\"${PKG}\"" "$PKGXML"; then
        # Check if SYSTEM flag already set (flags value will be odd if bit 0 set)
        CURRENT_FLAGS=$(grep -A2 "\"${PKG}\"" "$PKGXML" | grep -o 'flags="[0-9]*"' | head -1 | grep -o '[0-9]*')
        echo "[$(date)] Current package flags: $CURRENT_FLAGS"

        if [ -n "$CURRENT_FLAGS" ]; then
            # Bitwise OR with 1 to set FLAG_SYSTEM
            NEW_FLAGS=$(( CURRENT_FLAGS | 1 ))
            if [ "$NEW_FLAGS" != "$CURRENT_FLAGS" ]; then
                echo "[$(date)] Patching flags: $CURRENT_FLAGS -> $NEW_FLAGS"
                cp "$PKGXML" "$PKGXML_BAK"
                # Scoped replacement: only change the flags on the line
                # containing our package name (within a 3-line window)
                awk -v pkg="$PKG" -v old="$CURRENT_FLAGS" -v new="$NEW_FLAGS" '
                    /package name="/ && $0 ~ pkg { found=1 }
                    found && /flags="/ {
                        sub("flags=\"" old "\"", "flags=\"" new "\"")
                        found=0
                    }
                    { print }
                ' "$PKGXML_BAK" > "$PKGXML"
                echo "[$(date)] packages.xml patched"
            else
                echo "[$(date)] SYSTEM flag already set, no patch needed"
            fi
        fi
    else
        echo "[$(date)] Package not found in packages.xml yet, will retry next boot"
    fi
else
    echo "[$(date)] packages.xml not found"
fi

# Grant NFC privileged permissions

if [ -f "$PKGXML" ] && grep -q "\"${PKG}\"" "$PKGXML"; then
    # Check if perms block already contains our NFC permissions
    if ! grep -A 20 "\"${PKG}\"" "$PKGXML" | grep -q "NFC_PREFERRED_PAYMENT_SERVICE"; then
        echo "[$(date)] Injecting NFC install-time permission grants into packages.xml"
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

        echo "[$(date)] Permission grants injected"
    else
        echo "[$(date)] NFC permissions already present in packages.xml"
    fi
fi

# ── Step 4: Force PackageManager

echo "[$(date)] Notifying PackageManager..."
am broadcast -a android.intent.action.PACKAGE_CHANGED \
    --include-stopped-packages \
    -n "android/com.android.server.pm.PackageManagerReceiver" \
    --es "android.intent.extra.CHANGED_PACKAGE_DETAILS" "$PKG" 2>/dev/null || true

# Recompile/verify the package entry 

cmd package compile -m speed -f "$PKG" 2>/dev/null || true

echo "[$(date)] service.sh complete. Reboot required for packages.xml flag change to fully take effect."
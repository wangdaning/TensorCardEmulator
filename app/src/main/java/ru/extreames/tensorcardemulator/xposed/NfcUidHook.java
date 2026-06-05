package ru.extreames.tensorcardemulator.xposed;

import android.util.Log;

import java.io.File;
import java.util.Scanner;

import de.robv.android.xposed.IXposedHookLoadPackage;
import de.robv.android.xposed.XC_MethodHook;
import de.robv.android.xposed.XposedBridge;
import de.robv.android.xposed.XposedHelpers;
import de.robv.android.xposed.callbacks.XC_LoadPackage.LoadPackageParam;

public class NfcUidHook implements IXposedHookLoadPackage {
    private static final String TAG = "TensorCardEmulator";

    @Override
    public void handleLoadPackage(final LoadPackageParam lpparam) throws Throwable {
        if (!lpparam.packageName.equals("com.android.nfc")) {
            return;
        }

        XposedBridge.log("TensorCardEmulator: Hooking com.android.nfc");

        // Hook 1: android.nfc.Tag.getId()
        XposedHelpers.findAndHookMethod("android.nfc.Tag", lpparam.classLoader, "getId", new XC_MethodHook() {
            @Override
            protected void beforeHookedMethod(MethodHookParam param) throws Throwable {
                byte[] spoofed = getSpoofedUidBytes();
                if (spoofed != null) {
                    param.setResult(spoofed);
                }
            }
        });

        // Hook 2: NativeNfcTag getUid/uid
        try {
            Class<?> nativeTag = XposedHelpers.findClass("com.android.nfc.dhimpl.NativeNfcTag", lpparam.classLoader);
            XC_MethodHook nativeHook = new XC_MethodHook() {
                @Override
                protected void beforeHookedMethod(MethodHookParam param) throws Throwable {
                    byte[] spoofed = getSpoofedUidBytes();
                    if (spoofed != null) {
                        param.setResult(spoofed);
                    }
                }
            };

            XposedHelpers.findAndHookMethod(nativeTag, "getUid", nativeHook);
            XposedHelpers.findAndHookMethod(nativeTag, "uid", nativeHook);
        } catch (Throwable e) {
            XposedBridge.log("TensorCardEmulator: NativeNfcTag hook failed: " + e.getMessage());
        }
    }

    private static byte[] getSpoofedUidBytes() {
        try {
            File f = new File("/data/local/tmp/nfc_spoof_uid");
            if (!f.exists()) return null;
            try (Scanner sc = new Scanner(f)) {
                if (!sc.hasNextLine()) return null;
                String uid = sc.nextLine().trim();
                if (uid.isEmpty()) return null;
                String[] parts = uid.split(":");
                byte[] bytes = new byte[parts.length];
                for (int i = 0; i < parts.length; i++) {
                    bytes[i] = (byte) Integer.parseInt(parts[i], 16);
                }
                return bytes;
            }
        } catch (Exception e) {
            return null;
        }
    }
}

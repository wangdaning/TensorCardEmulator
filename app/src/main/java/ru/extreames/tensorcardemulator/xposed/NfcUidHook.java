package ru.extreames.tensorcardemulator.xposed;

import android.util.Log;
import androidx.annotation.NonNull;

import io.github.libxposed.api.XposedModule;

import java.io.File;
import java.lang.reflect.Method;
import java.util.Scanner;

public class NfcUidHook extends XposedModule {
    private static final String TAG = "TensorCardEmulator-Xposed";

    public NfcUidHook() {
        super();
    }

    @Override
    public void onPackageLoaded(@NonNull PackageLoadedParam param) {
        super.onPackageLoaded(param);
        if (param.getPackageName().equals("com.android.nfc")) {
            Log.d(TAG, "NFC process loaded: " + param.getPackageName());
        }
    }

    @Override
    public void onPackageReady(@NonNull PackageReadyParam param) {
        super.onPackageReady(param);

        if (!param.getPackageName().equals("com.android.nfc")) return;

        Log.d(TAG, "NFC process ready, applying hooks...");

        // Hook 1: Standard Public API - getId()
        try {
            Class<?> tagClass = param.getClassLoader().loadClass("android.nfc.Tag");
            Method getId = tagClass.getDeclaredMethod("getId");
            
            hook(getId).intercept(chain -> {
                byte[] spoofed = getSpoofedUidBytes();
                if (spoofed != null) {
                    Log.d(TAG, "Tag.getId() intercepted, returning spoofed UID");
                    return spoofed;
                }
                return chain.proceed();
            });
            
            Log.d(TAG, "Successfully hooked android.nfc.Tag.getId()");
        } catch (Throwable e) {
            Log.e(TAG, "Failed to hook Tag.getId(): " + e.getMessage());
        }

        // Hook 2: Tag.createMockTag
        try {
            Class<?> tagClass = param.getClassLoader().loadClass("android.nfc.Tag");
            for (Method m : tagClass.getDeclaredMethods()) {
                if (m.getName().equals("createMockTag")) {
                    hook(m).intercept(chain -> {
                        Object result = chain.proceed();
                        byte[] spoofed = getSpoofedUidBytes();
                        if (spoofed != null && result != null) {
                            Log.d(TAG, "Tag.createMockTag intercepted, will need further hacking if UID is immutable");
                        }
                        return result;
                    });
                }
            }
        } catch (Throwable ignored) {}

        // Hook 3: Internal NativeNfcTag
        String[] nativeTagClasses = {
            "com.android.nfc.dhimpl.NativeNfcTag",
            "com.android.nfc.nxp.NativeNfcTag",
            "com.android.nfc.gki.NativeNfcTag"
        };

        for (String className : nativeTagClasses) {
            try {
                Class<?> nativeTag = param.getClassLoader().loadClass(className);
                for (Method m : nativeTag.getDeclaredMethods()) {
                    String name = m.getName();
                    if (name.equals("getUid") || name.equals("uid") || name.equals("mUid")) {
                        hook(m).intercept(chain -> {
                            byte[] spoofed = getSpoofedUidBytes();
                            if (spoofed != null) {
                                Log.d(TAG, "Intercepted " + className + "." + name + ", returning spoofed UID");
                                return spoofed;
                            }
                            return chain.proceed();
                        });
                        Log.d(TAG, "Successfully hooked " + className + "." + name);
                    }
                }
            } catch (ClassNotFoundException ignored) {
            } catch (Throwable e) {
                Log.e(TAG, "Error hooking " + className + ": " + e.getMessage());
            }
        }
    }

    private static byte[] getSpoofedUidBytes() {
        try {
            File f = new File("/data/local/tmp/nfc_spoof_uid");
            if (!f.exists()) return null;
            
            if (!f.canRead()) {
                Log.e(TAG, "Cannot read spoof file! Check permissions: /data/local/tmp/nfc_spoof_uid");
                return null;
            }

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
            Log.e(TAG, "Error reading spoof file: " + e.getMessage());
            return null;
        }
    }
}

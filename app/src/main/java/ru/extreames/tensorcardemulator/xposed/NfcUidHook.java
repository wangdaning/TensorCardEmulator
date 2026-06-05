package ru.extreames.tensorcardemulator.xposed;

import android.util.Log;
import androidx.annotation.NonNull;

import io.github.libxposed.api.XposedModule;

import java.io.File;
import java.lang.reflect.Constructor;
import java.lang.reflect.Method;
import java.util.List;
import java.util.Scanner;

public class NfcUidHook extends XposedModule {
    private static final String TAG = "TensorCardEmulator-Xposed";

    public NfcUidHook() {
        super();
    }

    @Override
    public void onPackageLoaded(@NonNull PackageLoadedParam param) {
        super.onPackageLoaded(param);
        String pkg = param.getPackageName();
        if (pkg.equals("com.android.nfc") || pkg.equals("com.google.android.nfc")) {
            Log.d(TAG, "NFC process detected: " + pkg);
        }
    }

    @Override
    public void onPackageReady(@NonNull PackageReadyParam param) {
        super.onPackageReady(param);
        String pkg = param.getPackageName();

        if (!pkg.equals("com.android.nfc") && !pkg.equals("com.google.android.nfc")) return;

        Log.d(TAG, "Applying comprehensive NFC hooks for " + pkg);

        // Hook 1: android.nfc.Tag.getId()
        try {
            Class<?> tagClass = param.getClassLoader().loadClass("android.nfc.Tag");
            Method getId = tagClass.getDeclaredMethod("getId");
            
            hook(getId).intercept(chain -> {
                byte[] spoofed = getSpoofedUidBytes();
                if (spoofed != null) {
                    Log.d(TAG, "Tag.getId() spoofed successfully");
                    return spoofed;
                }
                return chain.proceed();
            });
            Log.d(TAG, "Hooked Tag.getId()");
        } catch (Throwable e) {
            Log.e(TAG, "Failed to hook Tag.getId(): " + e.getMessage());
        }

        // Hook 2: Tag Constructors
        try {
            Class<?> tagClass = param.getClassLoader().loadClass("android.nfc.Tag");
            for (Constructor<?> c : tagClass.getDeclaredConstructors()) {
                hook(c).intercept(chain -> {
                    byte[] spoofed = getSpoofedUidBytes();
                    if (spoofed != null) {
                        List<Object> args = chain.getArgs();
                        if (args != null && !args.isEmpty() && args.get(0) instanceof byte[]) {
                            args.set(0, spoofed);
                            Log.d(TAG, "Tag constructor argument spoofed");
                        }
                    }
                    return chain.proceed();
                });
            }
            Log.d(TAG, "Hooked all Tag constructors");
        } catch (Throwable e) {
            Log.e(TAG, "Failed to hook Tag constructors: " + e.getMessage());
        }

        // Hook 3: Service-level UID getters
        String[] nativeTagClasses = {
            "com.android.nfc.dhimpl.NativeNfcTag",
            "com.android.nfc.nxp.NativeNfcTag",
            "com.android.nfc.gki.NativeNfcTag",
            "com.google.android.nfc.dhimpl.NativeNfcTag"
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
                                Log.d(TAG, "Service-level UID method spoofed: " + className + "." + name);
                                return spoofed;
                            }
                            return chain.proceed();
                        });
                        Log.d(TAG, "Hooked service method: " + className + "." + name);
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
        } catch (Throwable e) {
            return null;
        }
    }
}

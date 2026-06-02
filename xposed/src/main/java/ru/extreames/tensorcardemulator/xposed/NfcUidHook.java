package ru.extreames.tensorcardemulator.xposed;

import androidx.annotation.NonNull;

import io.github.libxposed.api.XposedModule;

import java.io.File;
import java.lang.reflect.Method;
import java.util.Scanner;

public class NfcUidHook extends XposedModule {

    public NfcUidHook() {
        super();
    }

    @Override
    public void onPackageLoaded(@NonNull PackageLoadedParam param) {
        super.onPackageLoaded(param);
        if (param.getPackageName().equals("com.android.nfc")) {
            log(3, "TensorCardEmulator", "NFC process loaded");
        }
    }

    @Override
    public void onPackageReady(@NonNull PackageReadyParam param) {
        super.onPackageReady(param);

        if (!param.getPackageName().equals("com.android.nfc")) return;

        log(3, "TensorCardEmulator", "NFC process ready, applying hooks");

        try {
            Class<?> tagClass = param.getClassLoader().loadClass("android.nfc.Tag");
            Method getId = tagClass.getDeclaredMethod("getId");
            
            hook(getId).intercept(chain -> {
                byte[] spoofed = getSpoofedUidBytes();
                if (spoofed != null) {
                    return spoofed;
                }
                return chain.proceed();
            });
            
            log(3, "TensorCardEmulator", "hooked Tag.getId()");
        } catch (Exception e) {
            log(6, "TensorCardEmulator", "Tag.getId() hook failed: " + e.getMessage());
        }

        try {
            Class<?> nativeTag = param.getClassLoader().loadClass("com.android.nfc.dhimpl.NativeNfcTag");
            for (Method m : nativeTag.getDeclaredMethods()) {
                if (m.getName().equals("getUid") || m.getName().equals("uid")) {
                    hook(m).intercept(chain -> {
                        byte[] spoofed = getSpoofedUidBytes();
                        if (spoofed != null) {
                            return spoofed;
                        }
                        return chain.proceed();
                    });
                    log(3, "TensorCardEmulator", "hooked NativeNfcTag." + m.getName());
                }
            }
        } catch (Exception e) {
            log(6, "TensorCardEmulator", "NativeNfcTag hook failed: " + e.getMessage());
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

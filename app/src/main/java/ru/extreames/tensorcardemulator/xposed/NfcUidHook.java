package ru.extreames.tensorcardemulator.xposed;

import androidx.annotation.NonNull;

import io.github.libxposed.api.XposedModule;
import io.github.libxposed.api.XC_MethodHook;

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
            logD("NFC process loaded");
        }
    }

    @Override
    public void onPackageReady(@NonNull PackageReadyParam param) {
        super.onPackageReady(param);

        if (!param.getPackageName().equals("com.android.nfc")) return;

        logD("NFC process ready, applying hooks");

        try {
            Class<?> tagClass = param.getClassLoader().loadClass("android.nfc.Tag");
            Method getId = tagClass.getDeclaredMethod("getId");
            
            findAndHookMethod(getId, new XC_MethodHook() {
                @Override
                protected void beforeHookedMethod(MethodHookParam param) throws Throwable {
                    byte[] spoofed = getSpoofedUidBytes();
                    if (spoofed != null) {
                        param.setResult(spoofed);
                    }
                }
            });
            
            logD("Hooked Tag.getId()");
        } catch (Throwable e) {
            logE("Tag.getId() hook failed: " + e.getMessage());
        }

        try {
            Class<?> nativeTag = param.getClassLoader().loadClass("com.android.nfc.dhimpl.NativeNfcTag");
            for (Method m : nativeTag.getDeclaredMethods()) {
                if (m.getName().equals("getUid") || m.getName().equals("uid")) {
                    findAndHookMethod(m, new XC_MethodHook() {
                        @Override
                        protected void beforeHookedMethod(MethodHookParam param) throws Throwable {
                            byte[] spoofed = getSpoofedUidBytes();
                            if (spoofed != null) {
                                param.setResult(spoofed);
                            }
                        }
                    });
                    logD("Hooked NativeNfcTag." + m.getName());
                }
            }
        } catch (Throwable e) {
            logE("NativeNfcTag hook failed: " + e.getMessage());
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
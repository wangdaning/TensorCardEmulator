package ru.extreames.tensorcardemulator.xposed;

import io.github.libxposed.api.XposedInterface;
import io.github.libxposed.api.XposedModule;
import io.github.libxposed.api.annotations.BeforeInvocation;
import io.github.libxposed.api.annotations.XposedHooker;

import java.io.File;
import java.lang.reflect.Method;
import java.util.Scanner;

public class NfcUidHook extends XposedModule {

    public NfcUidHook(XposedInterface base, ModuleLoadedParam param) {
        super(base, param);
    }

    @Override
    public void onPackageLoaded(PackageLoadedParam param) {
        super.onPackageLoaded(param);

        if (!param.getPackageName().equals("com.android.nfc")) return;

        log("TensorCardEmulator: hooking com.android.nfc");

        // Hook 1: android.nfc.Tag.getId() — covers all apps reading NFC tags
        try {
            Class<?> tagClass = param.getClassLoader().loadClass("android.nfc.Tag");
            Method getId = tagClass.getDeclaredMethod("getId");
            hookMethod(getId, TagGetIdHooker.class);
            log("TensorCardEmulator: hooked Tag.getId()");
        } catch (Exception e) {
            log("TensorCardEmulator: Tag.getId() hook failed: " + e.getMessage());
        }

        // Hook 2: NativeNfcTag uid field reader
        try {
            Class<?> nativeTag = param.getClassLoader()
                    .loadClass("com.android.nfc.dhimpl.NativeNfcTag");
            for (Method m : nativeTag.getDeclaredMethods()) {
                if (m.getName().equals("getUid") || m.getName().equals("uid")) {
                    hookMethod(m, NativeTagUidHooker.class);
                    log("TensorCardEmulator: hooked NativeNfcTag." + m.getName());
                }
            }
        } catch (Exception e) {
            log("TensorCardEmulator: NativeNfcTag hook failed: " + e.getMessage());
        }
    }

    @XposedHooker
    static class TagGetIdHooker implements XposedInterface.Hooker {
        @BeforeInvocation
        public static void before(XposedInterface.BeforeHookCallback callback) {
            byte[] spoofed = getSpoofedUidBytes();
            if (spoofed != null) {
                callback.returnAndSkip(spoofed);
            }
        }
    }

    @XposedHooker
    static class NativeTagUidHooker implements XposedInterface.Hooker {
        @BeforeInvocation
        public static void before(XposedInterface.BeforeHookCallback callback) {
            byte[] spoofed = getSpoofedUidBytes();
            if (spoofed != null) {
                callback.returnAndSkip(spoofed);
            }
        }
    }

    private static byte[] getSpoofedUidBytes() {
        try {
            File f = new File("/data/local/tmp/nfc_spoof_uid");
            if (!f.exists()) return null;
            Scanner sc = new Scanner(f);
            if (!sc.hasNextLine()) return null;
            String uid = sc.nextLine().trim();
            sc.close();
            if (uid.isEmpty()) return null;
            String[] parts = uid.split(":");
            byte[] bytes = new byte[parts.length];
            for (int i = 0; i < parts.length; i++) {
                bytes[i] = (byte) Integer.parseInt(parts[i], 16);
            }
            return bytes;
        } catch (Exception e) {
            return null;
        }
    }
}

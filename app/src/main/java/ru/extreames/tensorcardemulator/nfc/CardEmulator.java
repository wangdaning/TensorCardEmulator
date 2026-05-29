package ru.extreames.tensorcardemulator.nfc;

import ru.extreames.tensorcardemulator.root.Shell;

public class CardEmulator {

    private static final String PATH_NFC_CONFIG = "/data/vendor/nfc/libnfc-nci.conf";
    private static final String PATH_NFC_CONFIG_BAK = "/data/adb/TensorCardEmulator/libnfc-nci.conf.bak";
    private static final String PATH_DEBUG_LOG = "/data/adb/TensorCardEmulator/debug.log";

    public boolean isSimulating() {
        return Shell.fileExists(PATH_NFC_CONFIG_BAK);
    }

    public boolean simulate(String serialNumber) {
        log("simulate() called with uid=" + serialNumber);
        try {
            Shell.runCommand("mkdir -p /data/adb/TensorCardEmulator");
            log("mkdir done");

            if (!Shell.fileExists(PATH_NFC_CONFIG_BAK)) {
                log("no backup exists, copying current config");
                Shell.copyFile(PATH_NFC_CONFIG, PATH_NFC_CONFIG_BAK);
                log("backup created: " + Shell.fileExists(PATH_NFC_CONFIG_BAK));
            } else {
                log("backup already exists");
            }

            String dmCfg = buildNfaDmStartUpCfg(serialNumber);
            log("dmCfg=" + dmCfg);

            StringBuilder newConfig = new StringBuilder();
            newConfig.append("## NFC_EMU_UID_OVERRIDE\n");
            newConfig.append("NFA_DM_START_UP_CFG=").append(dmCfg).append("\n");
            newConfig.append("## NFC_EMU_LISTEN_OVERRIDE\n");
            newConfig.append("NFA_LISTEN_TECH_MASK=0x07 # nfcemu\n");

            log("writing config...");
            Shell.writeFile(PATH_NFC_CONFIG, newConfig.toString());
            log("write done");

            // Verify what was actually written
            Shell.runCommand("echo 'file after write:' >> " + PATH_DEBUG_LOG);
            Shell.runCommand("cat " + PATH_NFC_CONFIG + " >> " + PATH_DEBUG_LOG);

            boolean killed = killNFC();
            log("killNFC returned: " + killed);
            return killed;

        } catch (Exception e) {
            log("EXCEPTION: " + e.getClass().getSimpleName() + " - " + e.getMessage());
            return false;
        }
    }

    public boolean restore() {
        log("restore() called");
        try {
            if (Shell.fileExists(PATH_NFC_CONFIG_BAK)) {
                Shell.copyFile(PATH_NFC_CONFIG_BAK, PATH_NFC_CONFIG);
                Shell.runCommand("rm -f " + PATH_NFC_CONFIG_BAK);
                Shell.runCommand("rm -f /data/vendor/nfc/libnfc-nci.conf.nfcemu.bak");
                boolean killed = killNFC();
                log("restore killNFC: " + killed);
                return killed;
            }
            log("no backup found, nothing to restore");
            return true;
        } catch (Exception e) {
            log("restore EXCEPTION: " + e.getMessage());
            return false;
        }
    }

    private boolean killNFC() {
        return Shell.restartNFC();
    }

    private void log(String msg) {
        try {
            Shell.runCommand("echo '[CardEmulator] " + msg.replace("'", "") + "' >> " + PATH_DEBUG_LOG);
        } catch (Exception ignored) {}
    }

    private String buildNfaDmStartUpCfg(String uid) {
        String[] hexParts = uid.split(":");
        int uidLen = hexParts.length;
        int totalLen = 1 + 9 + 2 + uidLen;

        StringBuilder sb = new StringBuilder("{ ");
        sb.append(String.format("%02X, ", 0x20));
        sb.append(String.format("%02X, ", 0x02));
        sb.append(String.format("%02X, ", totalLen));
        sb.append(String.format("%02X, ", 0x04));
        sb.append(String.format("%02X, %02X, %02X, ", 0x30, 0x01, 0x04));
        sb.append(String.format("%02X, %02X, %02X, ", 0x31, 0x01, 0x00));
        sb.append(String.format("%02X, %02X, %02X, ", 0x32, 0x01, 0x08));
        sb.append(String.format("%02X, %02X", 0x33, uidLen));

        for (String part : hexParts) {
            sb.append(String.format(", %02X", Integer.parseInt(part, 16)));
        }

        sb.append(" }");
        return sb.toString();
    }
}
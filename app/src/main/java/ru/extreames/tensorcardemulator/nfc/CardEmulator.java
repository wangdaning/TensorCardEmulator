package ru.extreames.tensorcardemulator.nfc;

import ru.extreames.tensorcardemulator.root.Shell;

public class CardEmulator {

    private static final String PATH_NFC_CONFIG = "/data/vendor/nfc/libnfc-nci.conf";
    private static final String PATH_NFC_CONFIG_BAK = "/data/adb/TensorCardEmulator/libnfc-nci.conf.bak";

    public boolean isSimulating() {
        return Shell.fileExists(PATH_NFC_CONFIG_BAK);
    }

    public boolean simulate(String serialNumber) {
        try {
            Shell.runCommand("mkdir -p /data/adb/TensorCardEmulator");
            if (!Shell.fileExists(PATH_NFC_CONFIG_BAK)) {
                Shell.copyFile(PATH_NFC_CONFIG, PATH_NFC_CONFIG_BAK);
            }

            String dmCfg = buildNfaDmStartUpCfg(serialNumber);
            StringBuilder newConfig = new StringBuilder();
            newConfig.append("## NFC_EMU_UID_OVERRIDE\n");
            newConfig.append("NFA_DM_START_UP_CFG=").append(dmCfg).append("\n");
            newConfig.append("## NFC_EMU_LISTEN_OVERRIDE\n");
            newConfig.append("NFA_LISTEN_TECH_MASK=0x07 # nfcemu\n");

            Shell.writeFile(PATH_NFC_CONFIG, newConfig.toString());
            return killNFC();
        } catch (Exception e) {
            return false;
        }
    }

    public boolean restore() {
        try {
            if (Shell.fileExists(PATH_NFC_CONFIG_BAK)) {
                Shell.copyFile(PATH_NFC_CONFIG_BAK, PATH_NFC_CONFIG);
                Shell.runCommand("rm -f " + PATH_NFC_CONFIG_BAK);
                Shell.runCommand("rm -f /data/vendor/nfc/libnfc-nci.conf.nfcemu.bak");
                return killNFC();
            }
            return true;
        } catch (Exception e) {
            return false;
        }
    }

    private boolean killNFC() {
        return Shell.restartNFC();
    }

    private String buildNfaDmStartUpCfg(String uid) {
        String[] hexParts = uid.split(":");
        int uidLen = hexParts.length;
        int totalLen = 1 + 9 + 2 + uidLen; 

        StringBuilder sb = new StringBuilder("{ ");
        sb.append(String.format("%02X, ", 0x20));
        sb.append(String.format("%02X, ", 0x02));
        sb.append(String.format("%02X, ", totalLen));
        sb.append(String.format("%02X, ", 0x04)); // 4 params
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
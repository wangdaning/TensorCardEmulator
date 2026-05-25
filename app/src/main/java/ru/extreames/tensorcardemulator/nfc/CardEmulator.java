package ru.extreames.tensorcardemulator.nfc;

import ru.extreames.tensorcardemulator.root.Shell;

public class CardEmulator {
    
    private static final String PATH_NFC_NCI_CONFIG = "/vendor/etc/libnfc-nci.conf";
    private static final String PATH_NFC_NCI_CONFIG_BAK = "/data/local/tmp/libnfc-nci.conf.bak";

    public boolean isSimulating() {
        return Shell.fileExists(PATH_NFC_NCI_CONFIG_BAK);
    }

    public boolean simulate(String serialNumber) {
        try {
            if (!Shell.fileExists(PATH_NFC_NCI_CONFIG_BAK)) {
                Shell.copyFile(PATH_NFC_NCI_CONFIG, PATH_NFC_NCI_CONFIG_BAK);
            }

            String originalConfig = Shell.readFile(PATH_NFC_NCI_CONFIG);
            String dmCfg = buildNfaDmStartUpCfg(serialNumber);
            StringBuilder newConfig = new StringBuilder();
            
            boolean foundDm = false;
            boolean foundListen = false;
            boolean foundUidMarker = false;

            for (String line : originalConfig.split("\n")) {
                String trimmed = line.trim();
                
                if (trimmed.startsWith("## NFC_EMU_UID_OVERRIDE")) {
                    foundUidMarker = true;
                    newConfig.append(line).append("\n");
                } else if (trimmed.startsWith("NFA_DM_START_UP_CFG")) {
                    newConfig.append("NFA_DM_START_UP_CFG=").append(dmCfg).append("\n");
                    foundDm = true;
                } else if (trimmed.startsWith("NFA_LISTEN_TECH_MASK")) {
                    newConfig.append("NFA_LISTEN_TECH_MASK=0x07 # nfcemu\n");
                    foundListen = true;
                } else {
                    newConfig.append(line).append("\n");
                }
            }

            if (!foundUidMarker) newConfig.insert(0, "## NFC_EMU_UID_OVERRIDE\n");
            if (!foundDm) newConfig.append("NFA_DM_START_UP_CFG=").append(dmCfg).append("\n");
            if (!foundListen) newConfig.append("NFA_LISTEN_TECH_MASK=0x07 # nfcemu\n");

            Shell.writeFile(PATH_NFC_NCI_CONFIG, newConfig.toString());
            return killNFC();
        } catch (Exception e) {
            return false;
        }
    }

    public boolean restore() {
        try {
            if (Shell.fileExists(PATH_NFC_NCI_CONFIG_BAK)) {
                Shell.copyFile(PATH_NFC_NCI_CONFIG_BAK, PATH_NFC_NCI_CONFIG);
                
                Process process = Runtime.getRuntime().exec(new String[]{"su", "-c", "rm " + PATH_NFC_NCI_CONFIG_BAK});
                process.waitFor();
                process.destroy();
                
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
        return "{12:CB:01:01:A5:07:01:02:03:04:" + uid + "}";
    }
}
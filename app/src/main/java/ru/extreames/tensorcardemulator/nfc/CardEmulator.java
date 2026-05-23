package ru.extreames.tensorcardemulator.nfc;

import ru.extreames.tensorcardemulator.root.Shell;

public class CardEmulator {
    private static final String PATH_NFC_NCI_CONFIG = "/data/vendor/nfc/libnfc-nci.conf";
    private static final String PATH_NFC_NCI_CONFIG_BAK = "/data/adb/TensorCardEmulator/libnfc-nci.conf.bak";

    private static final String NCI_CONFIG_EMULATING =
        "## NFC_EMU_UID_OVERRIDE\n" +
        "NFA_DM_START_UP_CFG=NFC_DM_CFG_PLACEHOLDER\n" +
        "## NFC_EMU_LISTEN_OVERRIDE\n" +
        "NFA_LISTEN_TECH_MASK=0x07 # nfcemu\n";

    public CardEmulator() {}

    public boolean isSimulating() {
        try {
            String content = Shell.readFile(PATH_NFC_NCI_CONFIG);
            return content.contains("## NFC_EMU_UID_OVERRIDE");
        } catch (Exception e) {
            return false;
        }
    }

    public boolean simulate(String serialNumber) {
        try {
            if (!Shell.fileExists(PATH_NFC_NCI_CONFIG_BAK)) {
                Shell.copyFile(PATH_NFC_NCI_CONFIG, PATH_NFC_NCI_CONFIG_BAK);
            }

            String dmCfg = buildNfaDmStartUpCfg(serialNumber);
            String config = NCI_CONFIG_EMULATING.replace("NFC_DM_CFG_PLACEHOLDER", dmCfg);
            Shell.writeFile(PATH_NFC_NCI_CONFIG, config);

            if (!killNFC())
                return false;

        } catch (Exception e) {
            return false;
        }
        return true;
    }

    public boolean restore() {
        try {
            if (Shell.fileExists(PATH_NFC_NCI_CONFIG_BAK)) {
                Shell.copyFile(PATH_NFC_NCI_CONFIG_BAK, PATH_NFC_NCI_CONFIG);
            } else {
                Shell.writeFile(PATH_NFC_NCI_CONFIG, buildMinimalRestoreConfig());
            }

            if (!killNFC())
                return false;

        } catch (Exception e) {
            return false;
        }
        return true;
    }

    private String buildMinimalRestoreConfig() {
        try {
            String vendorConf = Shell.readFile("/vendor/etc/libnfc-hal-st.conf");
            for (String line : vendorConf.split("\n")) {
                if (line.trim().startsWith("NFA_DM_START_UP_CFG")) {
                    return "NFA_DM_START_UP_CFG=" + line.split("=", 2)[1].trim() + "\n" +
                           "NFA_LISTEN_TECH_MASK=0x07\n";
                }
            }
        } catch (Exception ignored) {}
        return "NFA_LISTEN_TECH_MASK=0x07\n";
    }

    private String buildNfaDmStartUpCfg(String serialNumber) {
        String clean = serialNumber.replace(":", "").replace(" ", "");
        int uidLen = clean.length() / 2;
        byte[] uid = new byte[uidLen];
        for (int i = 0; i < uidLen; i++) {
            uid[i] = (byte) Integer.parseInt(clean.substring(i * 2, i * 2 + 2), 16);
        }

        int tag33FieldLen = 1 + uidLen + 4;
        int totalLen = 1 + 9 + 2 + tag33FieldLen;

        StringBuilder sb = new StringBuilder();
        sb.append("{ ");
        sb.append(String.format("20, 02, %02X, 04, ", totalLen));
        sb.append("30, 01, 04, ");
        sb.append("31, 01, 00, ");
        sb.append("32, 01, 08, ");
        sb.append(String.format("33, %02X, %02X, ", tag33FieldLen, uidLen));
        for (byte b : uid) {
            sb.append(String.format("%02X, ", b));
        }
        sb.append("04, 74, 1D, 18 }");
        return sb.toString();
    }

    private boolean killNFC() {
        String[] processes = Shell.getNFCProcesses();
        if (processes == null)
            return false;
        for (String process : processes) {
            if (!Shell.killProcess(process))
                return false;
        }
        return true;
    }
}
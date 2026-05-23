package ru.extreames.tensorcardemulator.nfc;

import ru.extreames.tensorcardemulator.root.Shell;

public class CardEmulator {
    // The file the HAL actually reads at runtime
    private static final String PATH_NFC_NCI_CONFIG = "/data/vendor/nfc/libnfc-nci.conf";

    // Backup stored in module directory
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
            // Back up original before first modification
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
                // Restore from our saved backup
                Shell.copyFile(PATH_NFC_NCI_CONFIG_BAK, PATH_NFC_NCI_CONFIG);
            } else {
                // No backup exists — build a safe generic restore dynamically
                // by reading whatever NFA_DM_START_UP_CFG the device currently has
                // in its live file and stripping our emulation lines, or if the
                // live file is already our emulation config, write a minimal safe default
                String currentContent = Shell.readFile(PATH_NFC_NCI_CONFIG);
                if (currentContent.contains("## NFC_EMU_UID_OVERRIDE")) {
                    // Live file is our modified version, we have no original to restore
                    // Write the minimal required content that keeps NFC functional
                    Shell.writeFile(PATH_NFC_NCI_CONFIG, buildMinimalRestoreConfig());
                }
                // If live file doesn't contain our marker, it's already original — don't touch it
            }

            if (!killNFC())
                return false;

        } catch (Exception e) {
            return false;
        }
        return true;
    }

    private String buildMinimalRestoreConfig() {
        // Reads the NFA_DM_START_UP_CFG from the vendor default file if accessible,
        // otherwise falls back to a minimal NCI config that leaves routing to defaults.
        // This keeps NFC functional without hardcoding device-specific bytes.
        try {
            // Try to read from the HAL config for the device's own default CFG value
            String vendorConf = Shell.readFile("/vendor/etc/libnfc-hal-st.conf");
            for (String line : vendorConf.split("\n")) {
                if (line.trim().startsWith("NFA_DM_START_UP_CFG")) {
                    // Found device's own default — use it
                    return "NFA_DM_START_UP_CFG=" + line.split("=", 2)[1].trim() + "\n" +
                           "NFA_LISTEN_TECH_MASK=0x07\n";
                }
            }
        } catch (Exception ignored) {}

        // Last resort — write only the listen mask, let the HAL use its own compiled defaults
        // for routing. This is safe on any ST NFC HAL device.
        return "NFA_LISTEN_TECH_MASK=0x07\n";
    }

    private String buildNfaDmStartUpCfg(String serialNumber) {
        // Parse UID bytes from serial number string (e.g. "04:AB:CD:EF" or "04ABCDEF")
        String clean = serialNumber.replace(":", "").replace(" ", "");
        int uidLen = clean.length() / 2;
        byte[] uid = new byte[uidLen];
        for (int i = 0; i < uidLen; i++) {
            uid[i] = (byte) Integer.parseInt(clean.substring(i * 2, i * 2 + 2), 16);
        }

        // Build NFA_DM_START_UP_CFG with UID injected into tag 0x33
        // Base params (without tag 33): 30 01 04, 31 01 00, 32 01 08 (3 TLVs = 9 bytes)
        // Tag 33 TLV: 33 <field_len> <uid_len> <uid_bytes...> 04 74 1D 18
        // field_len = 1 (uid_len byte) + uidLen + 4 (trailing 04 74 1D 18)
        int tag33FieldLen = 1 + uidLen + 4;
        // Total length = 1 (num_params) + 9 (first 3 TLVs) + 2 (tag+len for 33) + tag33FieldLen
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
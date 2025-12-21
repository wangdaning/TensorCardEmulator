package ru.extreames.tensorcardemulator.nfc;

import ru.extreames.tensorcardemulator.root.Shell;

public class CardEmulator {
    private static final String PATH_LIBNFC_HAL_CONFIG = "/data/adb/modules/TensorCardEmulator/system/vendor/etc/libnfc-hal-st.conf";
    private static final String PATH_LIBNFC_HAL_PROTO_CONFIG = "/data/adb/modules/TensorCardEmulator/system/vendor/etc/libnfc-hal-st-proto1.conf";

    private static final String PATH_BACKUP_LIBNFC_HAL_CONFIG = "/data/adb/TensorCardEmulator/libnfc-hal-st.conf";
    private static final String PATH_BACKUP_LIBNFC_HAL_PROTO_CONFIG = "/data/adb/TensorCardEmulator/libnfc-hal-st-proto1.conf";

    private static final String MODIFIED_LIBNFC_HAL_CONFIG = """
#TENSOR_CARD_EMULATOR
DEFAULT_SYS_CODE_PWR_STATE=0x3B
STNFC_FW_DEBUG_ENABLED=1
CORE_CONF_PROP={20,02,ALL_LENGTH,04,a1,01,1e,a2,01,19,80,01,01,33,CARD_SERIAL_NUMBER_LENGTH}
DEFAULT_OFFHOST_ROUTE_VALUE=0x81
NFA_PROPRIETARY_CFG={05:FF:FF:06:8A:90:77:FF:FF}
NCI_HAL_MODULE="nfc_nci.st21nfc"
STNFC_ACTIVERW_TIMER=0x01
STNFC_FW_PATH_STORAGE="/vendor/firmware"
CE_ON_SWITCH_OFF_STATE=1
OFF_HOST_ESE_PIPE_ID=0x5E
DEFAULT_ISODEP_ROUTE_VALUE=0x00
DEVICE_HOST_ALLOW_LIST={02:C0}
STNFC_HAL_LOGLEVEL=1
DEFAULT_SYS_CODE_ROUTE=0x00
DEFAULT_NFCF_ROUTE_VALUE=0x86
DEFAULT_ROUTE=0x00
DEFAULT_OFFHOST_ROUTE=0x00
STNFC_CONTROL_CLK=0x01
OFFHOST_ROUTE_ESE={86}
DEFAULT_SYS_CODE_ROUTE_VALUE=0x86
PRESERVE_STORAGE=1
STNFC_FW_BIN_NAME="/st54j_fw.bin"
STNFC_USB_CHARGING_MODE=1
ISO_DEP_MAX_TRANSCEIVE=0xFEFF
HAL_EVENT_LOG_STORAGE="/data/vendor/nfc"
OFFHOST_ROUTE_UICC={81}
DEFAULT_ROUTE_VALUE=0x00
DEFAULT_NFCF_ROUTE=0x00
OFF_HOST_SIM_PIPE_ID=0x3E
NFC_DEBUG_ENABLED=0
DEFAULT_ISODEP_ROUTE=0x00
POLL_BAIL_OUT_MODE=1
STNFC_FW_CONF_NAME="/st54j_conf.bin"
NFA_STORAGE="/data/nfc"
HAL_EVENT_LOG_DEBUG_ENABLED=1
PRESENCE_CHECK_ALGORITHM=5""";
    private static final String MODIFIED_LIBNFC_HAL_PROTO_CONFIG = """
#TENSOR_CARD_EMULATOR
ISO_DEP_MAX_TRANSCEIVE=0xFEFF
CE_ON_SWITCH_OFF_STATE=1
STNFC_USB_CHARGING_MODE=1
POLL_BAIL_OUT_MODE=1
OFFHOST_ROUTE_UICC={81}
DEFAULT_OFFHOST_ROUTE=0x00
STNFC_FW_CONF_NAME="/st54j_conf_PROTO1.bin"
STNFC_FW_DEBUG_ENABLED=1
NCI_HAL_MODULE="nfc_nci.st21nfc"
DEVICE_HOST_ALLOW_LIST={02:C0}
OFFHOST_ROUTE_ESE={86}
DEFAULT_ROUTE_VALUE=0x00
STNFC_HAL_LOGLEVEL=1
DEFAULT_SYS_CODE_ROUTE_VALUE=0x86
DEFAULT_ROUTE=0x00
DEFAULT_SYS_CODE_PWR_STATE=0x3B
STNFC_FW_PATH_STORAGE="/vendor/firmware"
NFA_STORAGE="/data/nfc"
DEFAULT_ISODEP_ROUTE_VALUE=0x81
OFF_HOST_SIM_PIPE_ID=0x3E
DEFAULT_NFCF_ROUTE=0x00
STNFC_CONTROL_CLK=0x01
DEFAULT_ISODEP_ROUTE=0x00
STNFC_ACTIVERW_TIMER=0x01
STNFC_FW_BIN_NAME="/st54j_fw.bin"
NFC_DEBUG_ENABLED=0
PRESENCE_CHECK_ALGORITHM=5
PRESERVE_STORAGE=1
DEFAULT_OFFHOST_ROUTE_VALUE=0x81
OFF_HOST_ESE_PIPE_ID=0x5E
DEFAULT_NFCF_ROUTE_VALUE=0x86
NFA_PROPRIETARY_CFG={05:FF:FF:06:8A:90:77:FF:FF}
DEFAULT_SYS_CODE_ROUTE=0x00
CORE_CONF_PROP={20,02,ALL_LENGTH,04,a1,01,1e,a2,01,19,80,01,01,33,CARD_SERIAL_NUMBER_LENGTH}
""";
    public CardEmulator() {

    }

    public boolean isSimulating() {
        try {
            String content = Shell.readFile(PATH_LIBNFC_HAL_CONFIG);
            return content.startsWith("#TENSOR_CARD_EMULATOR");
        } catch (Exception e) {
            return false;
        }
    }

    public boolean simulate(String serialNumber) {
        try {
            // EXAMPLE: CORE_CONF_PROP={20,02,13,04,a1,01,1e,a2,01,19,80,01,01,33,07,11,22,33,44,55,66,77}

            String transformedSerialNumber = convertSerialNumber(serialNumber);
            String allLength = String.format("%02X", 11 + transformedSerialNumber.split(",").length);

            String modified_libnfc_hal_config = MODIFIED_LIBNFC_HAL_CONFIG
                    .replace("CARD_SERIAL_NUMBER_LENGTH", transformedSerialNumber)
                    .replace("ALL_LENGTH", allLength);
            String modified_libnfc_hal_proto_config = MODIFIED_LIBNFC_HAL_PROTO_CONFIG
                    .replace("CARD_SERIAL_NUMBER_LENGTH", transformedSerialNumber)
                    .replace("ALL_LENGTH", allLength);

            Shell.writeFile(PATH_LIBNFC_HAL_CONFIG, modified_libnfc_hal_config);
            Shell.writeFile(PATH_LIBNFC_HAL_PROTO_CONFIG, modified_libnfc_hal_proto_config);

            if (!killNFC())
                return false;

        } catch (Exception e) {
            return false;
        }

        return true;
    }

    public boolean restore() {
        try {
            Shell.copyFile(PATH_BACKUP_LIBNFC_HAL_CONFIG, PATH_LIBNFC_HAL_CONFIG);
            Shell.copyFile(PATH_BACKUP_LIBNFC_HAL_PROTO_CONFIG, PATH_LIBNFC_HAL_PROTO_CONFIG);

            if (!killNFC())
                return false;

        } catch (Exception e) {
            return false;
        }

        return true;
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

    private String convertSerialNumber(String serialNumber) {
        String clean = serialNumber.replace(":", "");
        int length = clean.length() / 2;
        return String.format("%02X%s", length, clean)
                .replaceAll("(.{2})", "$1,")
                .replaceAll(",$", "");
    }
}

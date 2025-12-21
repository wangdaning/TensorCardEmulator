## TensorCardEmulator

An NFC card emulator for turnstiles, access control systems, and other systems that rely **only on the card serial number (UID)**.

### ⚠️ Limitations
This project emulates **UID (serial number) only**.

Not supported:
- Bank cards
- Transport cards (e.g. Troika, Oyster, etc.)
- Cards using secure elements, cryptography, or application-level authentication

⚠️ Such cards cannot be emulated because they require more than just a serial number.

### Supported Devices
- **Google Pixel devices only**
- **Tensor-based SoCs**
- **Tested on stock Android 16**
- Other Android versions are **likely supported**, but not yet tested

### Root Support
- KernelSU (KSU)
- Magisk (likely supported)

### Installation & Usage
1. Install the module
2. Reboot the device
3. Grant root access to the app (installed via module)
4. Open the app
5. Tap on the card serial number to enable scan mode
6. Tap the physical card to the phone
7. The card is now ready for emulation

### ⚠️ Disclaimer
This project is intended for research and testing purposes only with systems that rely on UID-based authentication.

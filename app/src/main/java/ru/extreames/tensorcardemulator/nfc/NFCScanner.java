package ru.extreames.tensorcardemulator.nfc;

import android.app.Activity;
import android.nfc.NfcAdapter;
import android.nfc.Tag;
import android.os.Bundle;
import android.os.Handler;
import android.os.Looper;

import java.util.concurrent.atomic.AtomicBoolean;

public class NFCScanner implements NfcAdapter.ReaderCallback {

    public interface Listener {
        void onScan(String uid);
    }

    private final Listener listener;
    private final NfcAdapter nfcAdapter;
    private final Handler mainHandler = new Handler(Looper.getMainLooper());
    private final AtomicBoolean isProcessing = new AtomicBoolean(false);

    public NFCScanner(Activity activity, Listener listener) {
        this.listener = listener;
        this.nfcAdapter = NfcAdapter.getDefaultAdapter(activity);
    }

    public void startScan(Activity activity) {
        if (nfcAdapter != null) {
            isProcessing.set(false);

            Bundle options = new Bundle();
            options.putInt(NfcAdapter.EXTRA_READER_PRESENCE_CHECK_DELAY, 250);

            nfcAdapter.enableReaderMode(activity, this,
                    NfcAdapter.FLAG_READER_NFC_A |
                    NfcAdapter.FLAG_READER_NFC_B |
                    NfcAdapter.FLAG_READER_NFC_F |
                    NfcAdapter.FLAG_READER_NFC_V |
                    NfcAdapter.FLAG_READER_SKIP_NDEF_CHECK |
                    NfcAdapter.FLAG_READER_NO_PLATFORM_SOUNDS,
                    options);
        }
    }

    public void stopScan(Activity activity) {
        if (nfcAdapter != null) {
            try {
                nfcAdapter.disableReaderMode(activity);
            } catch (IllegalStateException e) {
                // Handle edge context closures silently
            }
        }
    }

    @Override
    public void onTagDiscovered(Tag tag) {
        if (!isProcessing.compareAndSet(false, true)) {
            return;
        }

        byte[] id = tag.getId();
        if (id == null || id.length == 0) {
            isProcessing.set(false);
            return;
        }

        StringBuilder hexString = new StringBuilder();
        for (int i = 0; i < id.length; i++) {
            String hex = Integer.toHexString(0xFF & id[i]);
            if (hex.length() == 1) {
                hexString.append('0');
            }
            hexString.append(hex.toUpperCase());
            if (i < id.length - 1) {
                hexString.append(":");
            }
        }

        final String formattedUid = hexString.toString();
        mainHandler.post(() -> {
            if (listener != null) {
                listener.onScan(formattedUid);
            }
        });
    }
}
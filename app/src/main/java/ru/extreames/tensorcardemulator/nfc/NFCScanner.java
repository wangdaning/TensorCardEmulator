package ru.extreames.tensorcardemulator.nfc;

import android.app.Activity;
import android.nfc.NfcAdapter;
import android.nfc.Tag;
import android.os.Bundle;

public class NFCScanner implements NfcAdapter.ReaderCallback {
    private NfcAdapter nfcAdapter;
    private final OnTagReadListener listener;

    public NFCScanner(Activity activity, OnTagReadListener listener) {
        this.nfcAdapter = NfcAdapter.getDefaultAdapter(activity);
        this.listener = listener;
    }

    public void startScan(Activity activity) {
        try {
            if (nfcAdapter == null)
                nfcAdapter = NfcAdapter.getDefaultAdapter(activity);
            if (nfcAdapter != null) {
                Bundle options = new Bundle();
                int flags = NfcAdapter.FLAG_READER_NFC_A |
                        NfcAdapter.FLAG_READER_NFC_B |
                        NfcAdapter.FLAG_READER_NFC_F |
                        NfcAdapter.FLAG_READER_NFC_V;
                nfcAdapter.enableReaderMode(activity, this, flags, options);
            }
        } catch (Exception e) { // DeadObjectException
            nfcAdapter = null;
            this.startScan(activity);
        }
    }

    public void stopScan(Activity activity) {
        try {
            if (nfcAdapter == null)
                nfcAdapter = NfcAdapter.getDefaultAdapter(activity);
            if (nfcAdapter != null)
                nfcAdapter.disableReaderMode(activity);
        } catch (Exception e) { // DeadObjectException
            nfcAdapter = null;
            this.stopScan(activity);
        }
    }

    @Override
    public void onTagDiscovered(Tag tag) {
        if (listener != null) {
            String serialNumber = bytesToHex(tag.getId());
            listener.onTagRead(serialNumber);
        }
    }

    private String bytesToHex(byte[] bytes) {
        StringBuilder sb = new StringBuilder();
        for (int i = 0; i < bytes.length; i++) {
            sb.append(String.format("%02X", bytes[i]));
            if (i < bytes.length - 1)
                sb.append(":");
        }
        return sb.toString();
    }

    public interface OnTagReadListener {
        void onTagRead(String serialNumber);
    }
}
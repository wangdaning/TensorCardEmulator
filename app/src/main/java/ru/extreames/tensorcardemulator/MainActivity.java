package ru.extreames.tensorcardemulator;

import android.graphics.Color;
import android.os.Build;
import android.os.Bundle;
import android.view.View;
import android.view.animation.AlphaAnimation;
import android.view.animation.Animation;
import android.widget.EditText;
import android.widget.ImageView;
import android.widget.TextView;
import android.widget.Toast;

import androidx.appcompat.app.AlertDialog;
import androidx.appcompat.app.AppCompatActivity;
import androidx.core.view.WindowCompat;
import androidx.recyclerview.widget.LinearLayoutManager;
import androidx.recyclerview.widget.RecyclerView;

import com.google.android.material.materialswitch.MaterialSwitch;

import java.util.List;

import ru.extreames.tensorcardemulator.adapter.SavedCardsAdapter;
import ru.extreames.tensorcardemulator.model.AppDatabase;
import ru.extreames.tensorcardemulator.model.SavedCard;
import ru.extreames.tensorcardemulator.nfc.CardEmulator;
import ru.extreames.tensorcardemulator.nfc.NFCScanner;
import ru.extreames.tensorcardemulator.prefs.PrefsManager;
import ru.extreames.tensorcardemulator.root.Shell;

public class MainActivity extends AppCompatActivity {

    private boolean isSimulating = false;
    private int selectedCardId = -1;
    private int activeCardId = -1;

    private MaterialSwitch masterSwitch;
    private TextView masterToggleSubtext;
    private TextView statusText;
    private ImageView statusIcon;
    private TextView serialTextView;
    private TextView btnSaveCard;
    private TextView emptyCardsText;
    private RecyclerView savedCardsRecyclerView;

    private AlphaAnimation pulseAnimation;
    private NFCScanner nfcScanner;
    private CardEmulator cardEmulator;
    private PrefsManager prefs;
    private AppDatabase db;
    private SavedCardsAdapter adapter;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
       super.onCreate(savedInstanceState);
	   
	   if (!Shell.hasRoot()) {
            Toast.makeText(getApplicationContext(), "No root access =(", Toast.LENGTH_SHORT).show();
            this.finish();
            return;
        }

        WindowCompat.setDecorFitsSystemWindows(getWindow(), false);
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.R) {
			getWindow().setDecorFitsSystemWindows(false);
		} else {
			getWindow().setStatusBarColor(Color.TRANSPARENT);
}
        setContentView(R.layout.activity_nfc_emulator);

        masterSwitch = findViewById(R.id.masterSwitch);
        masterToggleSubtext = findViewById(R.id.masterToggleSubtext);
        statusText = findViewById(R.id.statusText);
        statusIcon = findViewById(R.id.statusIcon);
        serialTextView = findViewById(R.id.serialTextView);
        btnSaveCard = findViewById(R.id.btnSaveCard);
        emptyCardsText = findViewById(R.id.emptyCardsText);
        savedCardsRecyclerView = findViewById(R.id.savedCardsRecyclerView);
        View btnScan = findViewById(R.id.btnScan);

        pulseAnimation = new AlphaAnimation(1.0f, 0.4f);
        pulseAnimation.setDuration(1000);
        pulseAnimation.setRepeatCount(Animation.INFINITE);
        pulseAnimation.setRepeatMode(Animation.REVERSE);

        db = AppDatabase.getInstance(this);
        cardEmulator = new CardEmulator();
        prefs = new PrefsManager(this, "SAVED_CARD");

        serialTextView.setText(prefs.getValue("SERIAL_NUMBER",
                getString(R.string.DEFAULT_SERIAL_NUMBER)));

        nfcScanner = new NFCScanner(this, serialNumber -> runOnUiThread(() -> {
            prefs.setValue("SERIAL_NUMBER", serialNumber);
            serialTextView.setText(serialNumber);
            nfcScanner.stopScan(this);
            toggleScanning(false);
        }));

        adapter = new SavedCardsAdapter(new SavedCardsAdapter.Listener() {
            @Override
            public void onCardSelected(SavedCard card) {
                selectedCardId = card.id;
                adapter.setSelectedCardId(selectedCardId);
                prefs.setValue("SERIAL_NUMBER", card.uid);
                serialTextView.setText(card.uid);
                updateMasterSwitchState();
                if (isSimulating) {
                    doRestore();
                    doSimulate(card.uid, card.id);
                }
            }

            @Override
            public void onCardDeselected(SavedCard card) {
                if (card.id == selectedCardId) {
                    selectedCardId = -1;
                    adapter.setSelectedCardId(-1);
                    updateMasterSwitchState();
                    if (isSimulating) doRestore();
                }
            }

            @Override
            public void onDelete(SavedCard card) {
                new AlertDialog.Builder(MainActivity.this)
                    .setTitle("Delete card")
                    .setMessage("Delete \"" + card.name + "\"?")
                    .setPositiveButton("Delete", (d, w) -> {
                        db.savedCardDao().delete(card);
                        if (card.id == selectedCardId) {
                            selectedCardId = -1;
                            adapter.setSelectedCardId(-1);
                            updateMasterSwitchState();
                            if (isSimulating) doRestore();
                        }
                        refreshSavedCards();
                    })
                    .setNegativeButton("Cancel", null)
                    .show();
            }

            @Override
            public void onRename(SavedCard card) {
                showRenameDialog(card);
            }
        });

        savedCardsRecyclerView.setLayoutManager(new LinearLayoutManager(this));
        savedCardsRecyclerView.setAdapter(adapter);

        masterSwitch.setOnCheckedChangeListener((buttonView, isChecked) -> {
            if (!buttonView.isPressed()) return;
            if (isChecked) {
                if (selectedCardId == -1) {
                    masterSwitch.setChecked(false);
                    Toast.makeText(this, "Select a card first", Toast.LENGTH_SHORT).show();
                    return;
                }
                String uid = prefs.getValue("SERIAL_NUMBER", null);
                if (uid == null) {
                    masterSwitch.setChecked(false);
                    return;
                }
                doSimulate(uid, selectedCardId);
            } else {
                doRestore();
            }
        });

        btnScan.setOnClickListener(v -> {
            nfcScanner.startScan(this);
            toggleScanning(true);
        });

        btnSaveCard.setOnClickListener(v -> {
            String uid = prefs.getValue("SERIAL_NUMBER", null);
            if (uid == null || uid.equals(getString(R.string.DEFAULT_SERIAL_NUMBER))) {
                Toast.makeText(this, "Scan a card first", Toast.LENGTH_SHORT).show();
                return;
            }
            showSaveDialog(uid);
        });

        if (cardEmulator.isSimulating()) {
            isSimulating = true;
            updateSimulatingUI(true);
        }

        refreshSavedCards();
        updateMasterSwitchState();
    }

    private void updateMasterSwitchState() {
        boolean hasSelection = selectedCardId != -1;
        masterSwitch.setEnabled(hasSelection);
        masterToggleSubtext.setText(hasSelection
                ? getString(R.string.TOGGLE_TO_EMULATE)
                : getString(R.string.SELECT_CARD_FIRST));
    }

    private void doSimulate(String serialNumber, int cardId) {
        if (!cardEmulator.simulate(serialNumber)) {
            Toast.makeText(this, "Failed to simulate card =(", Toast.LENGTH_SHORT).show();
            masterSwitch.setChecked(false);
            return;
        }
        isSimulating = true;
        activeCardId = cardId;
        adapter.setSelectedCardId(cardId);
        updateSimulatingUI(true);
    }

    private void doRestore() {
        if (!cardEmulator.restore()) {
            Toast.makeText(this, "Failed to restore NFC =(", Toast.LENGTH_SHORT).show();
            return;
        }
        isSimulating = false;
        activeCardId = -1;
        updateSimulatingUI(false);
    }

    private void updateSimulatingUI(boolean simulating) {
        statusText.setText(simulating ? R.string.SIMULATING : R.string.IDLE);
        masterToggleSubtext.setText(simulating
                ? getString(R.string.EMULATING_NOW)
                : getString(R.string.TOGGLE_TO_EMULATE));

        masterSwitch.setOnCheckedChangeListener(null);
        masterSwitch.setChecked(simulating);
        masterSwitch.setOnCheckedChangeListener((buttonView, isChecked) -> {
            if (!buttonView.isPressed()) return;
            if (isChecked) {
                String uid = prefs.getValue("SERIAL_NUMBER", null);
                if (uid != null) doSimulate(uid, selectedCardId);
            } else {
                doRestore();
            }
        });

        if (simulating)
            statusIcon.startAnimation(pulseAnimation);
        else
            statusIcon.clearAnimation();
    }

    private void toggleScanning(boolean state) {
        if (state) {
            statusText.setText(R.string.SCANNING);
            statusIcon.startAnimation(pulseAnimation);
        } else {
            statusText.setText(isSimulating ? R.string.SIMULATING : R.string.IDLE);
            statusIcon.clearAnimation();
        }
    }

    private void refreshSavedCards() {
        List<SavedCard> cards = db.savedCardDao().getAll();
        adapter.setCards(cards);
        emptyCardsText.setVisibility(cards.isEmpty() ? View.VISIBLE : View.GONE);
    }

    private void showSaveDialog(String uid) {
        EditText input = new EditText(this);
        input.setHint(getString(R.string.CARD_NAME_HINT));
        input.setPadding(48, 24, 48, 24);

        new AlertDialog.Builder(this)
            .setTitle("Save card")
            .setView(input)
            .setPositiveButton("Save", (d, w) -> {
                String name = input.getText().toString().trim();
                if (name.isEmpty()) name = "Card " + (db.savedCardDao().getAll().size() + 1);
                db.savedCardDao().insert(new SavedCard(name, uid));
                refreshSavedCards();
            })
            .setNegativeButton("Cancel", null)
            .show();
    }

    private void showRenameDialog(SavedCard card) {
        EditText input = new EditText(this);
        input.setText(card.name);
        input.setPadding(48, 24, 48, 24);

        new AlertDialog.Builder(this)
            .setTitle("Rename card")
            .setView(input)
            .setPositiveButton("Save", (d, w) -> {
                String name = input.getText().toString().trim();
                if (!name.isEmpty()) {
                    card.name = name;
                    db.savedCardDao().update(card);
                    refreshSavedCards();
                }
            })
            .setNegativeButton("Cancel", null)
            .show();
    }
}
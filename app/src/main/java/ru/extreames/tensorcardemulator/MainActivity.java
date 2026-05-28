package ru.extreames.tensorcardemulator;

import android.os.Bundle;
import android.view.View;
import android.view.animation.AlphaAnimation;
import android.view.animation.Animation;
import android.widget.EditText;
import android.widget.ImageView;
import android.widget.TextView;
import android.widget.Toast;

import androidx.annotation.NonNull;
import androidx.appcompat.app.AlertDialog;
import androidx.appcompat.app.AppCompatActivity;
import androidx.recyclerview.widget.LinearLayoutManager;
import androidx.recyclerview.widget.RecyclerView;

import com.google.android.material.materialswitch.MaterialSwitch;

import java.lang.ref.WeakReference;
import java.util.List;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;

import ru.extreames.tensorcardemulator.adapter.SavedCardsAdapter;
import ru.extreames.tensorcardemulator.model.AppDatabase;
import ru.extreames.tensorcardemulator.model.SavedCard;
import ru.extreames.tensorcardemulator.nfc.CardEmulator;
import ru.extreames.tensorcardemulator.nfc.NFCScanner;
import ru.extreames.tensorcardemulator.prefs.PrefsManager;
import ru.extreames.tensorcardemulator.root.Shell;

public class MainActivity extends AppCompatActivity {

    private final ExecutorService executor = Executors.newSingleThreadExecutor();

    private MaterialSwitch masterSwitch;
    private TextView masterToggleSubtext;
    private TextView statusText;
    private ImageView statusIcon;
    private TextView serialTextView;
    private View btnSaveCard;
    private TextView emptyCardsText;
    private RecyclerView savedCardsRecyclerView;
    private View btnScan;

    private AlphaAnimation pulseAnimation;
    private AppDatabase db;
    private CardEmulator cardEmulator;
    private PrefsManager prefs;
    private NFCScanner nfcScanner;
    private SavedCardsAdapter adapter;

    private boolean isSimulating = false;
    private int selectedCardId = -1;
    private int activeCardId = -1;

    private interface SafeUiAction {
        void run(@NonNull MainActivity activity);
    }

    private void runOnSafeUi(SafeUiAction action) {
        final WeakReference<MainActivity> weakRef = new WeakReference<>(this);
        runOnUiThread(() -> {
            MainActivity activity = weakRef.get();
            if (activity != null && !activity.isFinishing() && !activity.isDestroyed()) {
                action.run(activity);
            }
        });
    }

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_nfc_emulator);

        masterSwitch = findViewById(R.id.masterSwitch);
        masterToggleSubtext = findViewById(R.id.masterToggleSubtext);
        statusText = findViewById(R.id.statusText);
        statusIcon = findViewById(R.id.statusIcon);
        serialTextView = findViewById(R.id.serialTextView);
        btnSaveCard = findViewById(R.id.btnSaveCard);
        emptyCardsText = findViewById(R.id.emptyCardsText);
        savedCardsRecyclerView = findViewById(R.id.savedCardsRecyclerView);
        btnScan = findViewById(R.id.btnScan);

        setLoading(true);

        executor.execute(() -> {
            boolean root = Shell.hasRoot();
            runOnSafeUi(activity -> {
                if (!root) {
                    new AlertDialog.Builder(activity)
                        .setTitle("Root Required")
                        .setMessage("This app requires root access to modify NFC configurations. Please grant root and restart.")
                        .setCancelable(false)
                        .setPositiveButton("Exit", (d, w) -> activity.finish())
                        .show();
                } else {
                    activity.initializeApp();
                    activity.setLoading(false);
                }
            });
        });
    }

    private void initializeApp() {
        db = AppDatabase.getInstance(this);
        cardEmulator = new CardEmulator();
        prefs = new PrefsManager(this, "SAVED_CARD");

        pulseAnimation = new AlphaAnimation(1.0f, 0.4f);
        pulseAnimation.setDuration(1000);
        pulseAnimation.setRepeatCount(Animation.INFINITE);
        pulseAnimation.setRepeatMode(Animation.REVERSE);

        serialTextView.setText(prefs.getValue("SERIAL_NUMBER",
                getString(R.string.default_serial_number)));

        nfcScanner = new NFCScanner(this, serialNumber -> runOnSafeUi(activity -> {
            prefs.setValue("SERIAL_NUMBER", serialNumber);
            serialTextView.setText(serialNumber);
            nfcScanner.stopScan(activity);
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
                        executor.execute(() -> {
                            db.savedCardDao().delete(card);
                            runOnSafeUi(activity -> {
                                if (card.id == selectedCardId) {
                                    selectedCardId = -1;
                                    adapter.setSelectedCardId(-1);
                                    updateMasterSwitchState();
                                    if (isSimulating) doRestore();
                                }
                                refreshSavedCards();
                            });
                        });
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
            if (uid == null || uid.equals(getString(R.string.default_serial_number))) {
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

    private void setLoading(boolean isLoading) {
        // Optionally show/hide a progress indicator here
        // For now just toggle the main content visibility
        View content = findViewById(R.id.savedCardsRecyclerView);
        if (content != null) {
            content.setVisibility(isLoading ? View.INVISIBLE : View.VISIBLE);
        }
    }

    private void updateMasterSwitchState() {
        boolean hasSelection = selectedCardId != -1;
        masterSwitch.setEnabled(hasSelection);
        masterToggleSubtext.setText(hasSelection
                ? getString(R.string.toggle_to_emulate)
                : getString(R.string.select_card_first));
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
        statusText.setText(getString(simulating ? R.string.simulating : R.string.idle));
        masterToggleSubtext.setText(simulating
                ? getString(R.string.emulating_now)
                : getString(R.string.toggle_to_emulate));

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
            statusText.setText(getString(R.string.scanning));
            statusIcon.startAnimation(pulseAnimation);
        } else {
            statusText.setText(getString(isSimulating ? R.string.simulating : R.string.idle));
            statusIcon.clearAnimation();
        }
    }

    private void refreshSavedCards() {
        executor.execute(() -> {
            List<SavedCard> cards = db.savedCardDao().getAll();
            runOnSafeUi(activity -> {
                adapter.setCards(cards);
                emptyCardsText.setVisibility(cards.isEmpty() ? View.VISIBLE : View.GONE);
            });
        });
    }

    private void showSaveDialog(String uid) {
        EditText input = new EditText(this);
        input.setHint(getString(R.string.card_name_hint));
        input.setPadding(48, 24, 48, 24);

        new AlertDialog.Builder(this)
            .setTitle("Save card")
            .setView(input)
            .setPositiveButton("Save", (d, w) -> {
                String userInput = input.getText().toString().trim();
                executor.execute(() -> {
                    String name = userInput.isEmpty()
                            ? "Card " + (adapter.getItemCount() + 1)
                            : userInput;
                    db.savedCardDao().insert(new SavedCard(name, uid));
                    runOnSafeUi(activity -> refreshSavedCards());
                });
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
                    executor.execute(() -> {
                        card.name = name;
                        db.savedCardDao().update(card);
                        runOnSafeUi(activity -> refreshSavedCards());
                    });
                }
            })
            .setNegativeButton("Cancel", null)
            .show();
    }

    @Override
    protected void onDestroy() {
        super.onDestroy();
        executor.shutdown();
        if (nfcScanner != null) {
            nfcScanner.stopScan(this);
        }
    }
}

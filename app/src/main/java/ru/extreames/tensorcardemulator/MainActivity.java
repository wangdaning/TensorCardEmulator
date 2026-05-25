package ru.extreames.tensorcardemulator;

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
import androidx.recyclerview.widget.LinearLayoutManager;
import androidx.recyclerview.widget.RecyclerView;
import com.google.android.material.materialswitch.MaterialSwitch;

import java.util.List;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;

import ru.extreames.tensorcardemulator.adapter.SavedCardsAdapter;
import ru.extreames.tensorcardemulator.model.AppDatabase;
import ru.extreames.tensorcardemulator.model.SavedCard;
import ru.extreames.tensorcardemulator.nfc.CardEmulator;
import ru.extreames.tensorcardemulator.nfc.NFCScanner;
import ru.extreames.tensorcardemulator.root.Shell;
import ru.extreames.tensorcardemulator.prefs.PrefsManager;

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

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        getWindow().setDecorFitsSystemWindows(false);
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
            runOnUiThread(() -> {
                if (!root) {
                    new AlertDialog.Builder(MainActivity.this)
                        .setTitle("Root Required")
                        .setMessage("This app requires root access to modify NFC configurations. Please grant root and restart.")
                        .setCancelable(false)
                        .setPositiveButton("Exit", (d, w) -> finish())
                        .show();
                } else {
                    setLoading(false);
                    initializeApp();
                }
            });
        });
    }

    private void initializeApp() {
        pulseAnimation = new AlphaAnimation(1.0f, 0.4f);
        pulseAnimation.setDuration(1000);
        pulseAnimation.setRepeatCount(Animation.INFINITE);
        pulseAnimation.setRepeatMode(Animation.REVERSE);

        db = AppDatabase.getInstance(this);
        cardEmulator = new CardEmulator();
        prefs = new PrefsManager(this, "SAVED_CARD");

        serialTextView.setText(prefs.getValue("SERIAL_NUMBER", getString(R.string.DEFAULT_SERIAL_NUMBER)));

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
                        executor.execute(() -> {
                            db.savedCardDao().delete(card);
                            runOnUiThread(() -> {
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
                if (uid == null) { masterSwitch.setChecked(false); return; }
                doSimulate(uid, selectedCardId);
            } else {
                doRestore();
            }
        });

        if (btnScan != null) {
            btnScan.setOnClickListener(v -> {
                nfcScanner.startScan(this);
                toggleScanning(true);
            });
        }

        if (btnSaveCard != null) {
            btnSaveCard.setOnClickListener(v -> {
                String uid = prefs.getValue("SERIAL_NUMBER", null);
                if (uid == null || uid.equals(getString(R.string.DEFAULT_SERIAL_NUMBER))) {
                    Toast.makeText(this, "Scan a card first", Toast.LENGTH_SHORT).show();
                    return;
                }
                showSaveDialog(uid);
            });
        }

        executor.execute(() -> {
            boolean simulating = cardEmulator.isSimulating();
            runOnUiThread(() -> {
                if (simulating) {
                    isSimulating = true;
                    updateSimulatingUI(true);
                }
                refreshSavedCards();
                updateMasterSwitchState();
            });
        });
    }

    private void doSimulate(String serialNumber, int cardId) {
        setLoading(true);
        executor.execute(() -> {
            boolean success = cardEmulator.simulate(serialNumber);
            runOnUiThread(() -> {
                setLoading(false);
                if (!success) {
                    Toast.makeText(this, "Failed to simulate card =(", Toast.LENGTH_SHORT).show();
                    masterSwitch.setChecked(false);
                    return;
                }
                isSimulating = true;
                activeCardId = cardId;
                adapter.setSelectedCardId(cardId);
                updateSimulatingUI(true);
            });
        });
    }

    private void doRestore() {
        setLoading(true);
        executor.execute(() -> {
            boolean success = cardEmulator.restore();
            runOnUiThread(() -> {
                setLoading(false);
                if (!success) {
                    Toast.makeText(this, "Failed to restore NFC =(", Toast.LENGTH_SHORT).show();
                    return;
                }
                isSimulating = false;
                activeCardId = -1;
                updateSimulatingUI(false);
            });
        });
    }

    private void refreshSavedCards() {
        executor.execute(() -> {
            List<SavedCard> cards = db.savedCardDao().getAll();
            runOnUiThread(() -> {
                adapter.submitList(cards);
                emptyCardsText.setVisibility(cards.isEmpty() ? View.VISIBLE : View.GONE);
            });
        });
    }

    private void setLoading(boolean loading) {
        runOnUiThread(() -> {
            if (masterSwitch != null) masterSwitch.setEnabled(!loading && selectedCardId != -1);
            if (btnScan != null) btnScan.setEnabled(!loading);
            if (btnSaveCard != null) btnSaveCard.setEnabled(!loading);
        });
    }

    private void showSaveDialog(String uid) {
        EditText input = new EditText(this);
        input.setHint(getString(R.string.CARD_NAME_HINT));
        input.setPadding(48, 24, 48, 24);
        new AlertDialog.Builder(this)
            .setTitle("Save card")
            .setView(input)
            .setPositiveButton("Save", (d, w) -> {
                executor.execute(() -> {
                    String name = input.getText().toString().trim();
                    if (name.isEmpty()) name = "Card " + (db.savedCardDao().getAll().size() + 1);
                    db.savedCardDao().insert(new SavedCard(name, uid));
                    runOnUiThread(this::refreshSavedCards);
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
                    card.name = name;
                    executor.execute(() -> {
                        db.savedCardDao().update(card);
                        runOnUiThread(this::refreshSavedCards);
                    });
                }
            })
            .setNegativeButton("Cancel", null)
            .show();
    }

    private void updateMasterSwitchState() {
        if (masterSwitch != null) {
            masterSwitch.setChecked(isSimulating && activeCardId == selectedCardId);
        }
    }

    private void updateSimulatingUI(boolean simulating) {
        if (statusText == null || statusIcon == null || masterToggleSubtext == null) return;
        if (simulating) {
            statusText.setText("Emulating");
            statusIcon.startAnimation(pulseAnimation);
            masterToggleSubtext.setText("NFC hardware modified");
        } else {
            statusText.setText("Idle");
            statusIcon.clearAnimation();
            masterToggleSubtext.setText("Standard system configuration");
        }
    }

    private void toggleScanning(boolean scanning) {}

    @Override
    protected void onResume() {
        super.onResume();
        if (cardEmulator != null) {
            executor.execute(() -> {
                boolean simulating = cardEmulator.isSimulating();
                runOnUiThread(() -> {
                    isSimulating = simulating;
                    updateSimulatingUI(simulating);
                });
            });
        }
    }

    @Override
    protected void onPause() {
        super.onPause();
        if (nfcScanner != null) {
            nfcScanner.stopScan(this);
            toggleScanning(false);
        }
    }

    @Override
    protected void onDestroy() {
        super.onDestroy();
        executor.shutdown();
        try {
            if (!executor.awaitTermination(800, java.util.concurrent.TimeUnit.MILLISECONDS)) {
                executor.shutdownNow();
            }
        } catch (InterruptedException e) {
            executor.shutdownNow();
            Thread.currentThread().interrupt();
        }
    }
}
package ru.extreames.tensorcardemulator;

import android.animation.AnimatorSet;
import android.animation.ValueAnimator;
import android.content.res.ColorStateList;
import android.graphics.Color;
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

import com.google.android.material.button.MaterialButton;

import java.util.List;
import java.util.Objects;

import ru.extreames.tensorcardemulator.adapter.SavedCardsAdapter;
import ru.extreames.tensorcardemulator.model.AppDatabase;
import ru.extreames.tensorcardemulator.model.SavedCard;
import ru.extreames.tensorcardemulator.nfc.CardEmulator;
import ru.extreames.tensorcardemulator.nfc.NFCScanner;
import ru.extreames.tensorcardemulator.prefs.PrefsManager;
import ru.extreames.tensorcardemulator.root.Shell;

public class MainActivity extends AppCompatActivity {
    private boolean isSimulating = false;
    private int activeCardId = -1;

    private MaterialButton btnToggle;
    private MaterialButton btnScan;
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
        if (!Shell.hasRoot()) {
            Toast.makeText(getApplicationContext(), "No root access =(", Toast.LENGTH_SHORT).show();
            this.finish();
            return;
        }

        super.onCreate(savedInstanceState);
        this.getWindow().setStatusBarColor(Color.parseColor("#FF0F0F0F"));
        this.setContentView(R.layout.activity_nfc_emulator);

        this.btnToggle = findViewById(R.id.btnToggle);
        this.btnScan = findViewById(R.id.btnScan);
        this.statusText = findViewById(R.id.statusText);
        this.statusIcon = findViewById(R.id.statusIcon);
        this.serialTextView = findViewById(R.id.serialTextView);
        this.btnSaveCard = findViewById(R.id.btnSaveCard);
        this.emptyCardsText = findViewById(R.id.emptyCardsText);
        this.savedCardsRecyclerView = findViewById(R.id.savedCardsRecyclerView);

        this.pulseAnimation = new AlphaAnimation(1.0f, 0.4f);
        this.pulseAnimation.setDuration(1000);
        this.pulseAnimation.setRepeatCount(Animation.INFINITE);
        this.pulseAnimation.setRepeatMode(Animation.REVERSE);

        this.db = AppDatabase.getInstance(this);
        this.cardEmulator = new CardEmulator();
        this.prefs = new PrefsManager(this, "SAVED_CARD");

        this.serialTextView.setText(
            this.prefs.getValue("SERIAL_NUMBER", getString(R.string.DEFAULT_SERIAL_NUMBER))
        );

        this.nfcScanner = new NFCScanner(this, serialNumber -> runOnUiThread(() -> {
            prefs.setValue("SERIAL_NUMBER", serialNumber);
            serialTextView.setText(serialNumber);
            nfcScanner.stopScan(this);
            toggleScanning(false);
        }));

        this.adapter = new SavedCardsAdapter(new SavedCardsAdapter.Listener() {
            @Override
            public void onSimulate(SavedCard card) {
                if (isSimulating && card.id == activeCardId) {
                    doRestore();
                } else {
                    prefs.setValue("SERIAL_NUMBER", card.uid);
                    serialTextView.setText(card.uid);
                    doSimulate(card.uid, card.id);
                }
            }

            @Override
            public void onDelete(SavedCard card) {
                new AlertDialog.Builder(MainActivity.this)
                    .setTitle("Delete card")
                    .setMessage("Delete \"" + card.name + "\"?")
                    .setPositiveButton("Delete", (d, w) -> {
                        db.savedCardDao().delete(card);
                        if (card.id == activeCardId) activeCardId = -1;
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

        this.savedCardsRecyclerView.setLayoutManager(new LinearLayoutManager(this));
        this.savedCardsRecyclerView.setAdapter(adapter);

        this.btnToggle.setOnClickListener(v -> {
            if (!isSimulating) {
                String serialNumber = prefs.getValue("SERIAL_NUMBER", null);
                if (serialNumber == null) {
                    Toast.makeText(this, "No card to simulate =(", Toast.LENGTH_SHORT).show();
                    return;
                }
                doSimulate(serialNumber, -1);
            } else {
                doRestore();
            }
        });

        this.btnScan.setOnClickListener(v -> {
            nfcScanner.startScan(this);
            toggleScanning(true);
        });

        this.btnSaveCard.setOnClickListener(v -> {
            String uid = prefs.getValue("SERIAL_NUMBER", null);
            if (uid == null || uid.equals(getString(R.string.DEFAULT_SERIAL_NUMBER))) {
                Toast.makeText(this, "Scan a card first", Toast.LENGTH_SHORT).show();
                return;
            }
            showSaveDialog(uid);
        });

        if (this.cardEmulator.isSimulating()) {
            isSimulating = true;
            updateSimulatingUI(true);
        }

        refreshSavedCards();
    }

    private void doSimulate(String serialNumber, int cardId) {
        if (!cardEmulator.simulate(serialNumber)) {
            Toast.makeText(this, "Failed to simulate card =(", Toast.LENGTH_SHORT).show();
            return;
        }
        isSimulating = true;
        activeCardId = cardId;
        adapter.setActiveCardId(cardId);
        updateSimulatingUI(true);
    }

    private void doRestore() {
        if (!cardEmulator.restore()) {
            Toast.makeText(this, "Failed to restore NFC =(", Toast.LENGTH_SHORT).show();
            return;
        }
        isSimulating = false;
        activeCardId = -1;
        adapter.setActiveCardId(-1);
        updateSimulatingUI(false);
    }

    private void updateSimulatingUI(boolean simulating) {
        statusText.setText(simulating ? R.string.SIMULATING : R.string.IDLE);
        btnToggle.setText(simulating ? R.string.RESTORE : R.string.SIMULATE);
        btnToggle.setRippleColor(ColorStateList.valueOf(Color.TRANSPARENT));

        if (simulating)
            statusIcon.startAnimation(pulseAnimation);
        else
            statusIcon.clearAnimation();

        int targetBg = simulating ? Color.parseColor("#262626") : Color.WHITE;
        int targetText = simulating ? Color.WHITE : Color.BLACK;
        animateColorChange(targetBg, targetText);
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

    private void animateColorChange(int bgColor, int textColor) {
        ValueAnimator bgAnim = ValueAnimator.ofArgb(
            Objects.requireNonNull(btnToggle.getBackgroundTintList()).getDefaultColor(), bgColor);
        bgAnim.addUpdateListener(a ->
            btnToggle.setBackgroundTintList(ColorStateList.valueOf((int) a.getAnimatedValue())));

        ValueAnimator txtAnim = ValueAnimator.ofArgb(btnToggle.getCurrentTextColor(), textColor);
        txtAnim.addUpdateListener(a -> btnToggle.setTextColor((int) a.getAnimatedValue()));

        AnimatorSet set = new AnimatorSet();
        set.playTogether(bgAnim, txtAnim);
        set.setDuration(300);
        set.start();
    }
}
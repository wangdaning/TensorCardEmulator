package ru.extreames.tensorcardemulator;

import android.animation.AnimatorSet;
import android.animation.ValueAnimator;
import android.content.res.ColorStateList;
import android.graphics.Color;
import android.os.Bundle;
import android.view.animation.AlphaAnimation;
import android.view.animation.Animation;
import android.widget.ImageView;
import android.widget.TextView;
import android.widget.Toast;

import androidx.appcompat.app.AppCompatActivity;

import com.google.android.material.button.MaterialButton;

import java.util.Objects;

import ru.extreames.tensorcardemulator.nfc.CardEmulator;
import ru.extreames.tensorcardemulator.nfc.NFCScanner;
import ru.extreames.tensorcardemulator.prefs.PrefsManager;
import ru.extreames.tensorcardemulator.root.Shell;

public class MainActivity extends AppCompatActivity {
    private boolean isSimulating = false;

    private MaterialButton btnToggle;
    private TextView statusText;
    private ImageView statusIcon;
    private TextView serialTextView;

    private AlphaAnimation pulseAnimation;
    private NFCScanner nfcScanner;
    private CardEmulator cardEmulator;
    private PrefsManager prefs;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        if (!Shell.hasRoot()) {
            Toast.makeText(getApplicationContext(), "No root access =(", Toast.LENGTH_SHORT).show();
            this.finish();
            return;
        }

        super.onCreate(savedInstanceState);
        this.getWindow().setStatusBarColor(Color.parseColor("#FF101010"));
        this.setContentView(R.layout.activity_nfc_emulator);

        this.btnToggle = findViewById(R.id.btnToggle);
        this.statusText = findViewById(R.id.statusText);
        this.statusIcon = findViewById(R.id.statusIcon);
        this.serialTextView = findViewById(R.id.serialTextView);

        this.pulseAnimation = new AlphaAnimation(1.0f, 0.4f);
        this.pulseAnimation.setDuration(1000);
        this.pulseAnimation.setRepeatCount(Animation.INFINITE);
        this.pulseAnimation.setRepeatMode(Animation.REVERSE);

        this.nfcScanner = new NFCScanner(this, serialNumber -> {
            runOnUiThread(() -> {
                prefs.setValue("SERIAL_NUMBER", serialNumber);

                serialTextView.setText(serialNumber);
                nfcScanner.stopScan(this);

                toggleScanning(false);
            });
        });

        this.cardEmulator = new CardEmulator();
        this.prefs = new PrefsManager(this, "SAVED_CARD");

        this.btnToggle.setOnClickListener(v -> toggleSimulation());
        this.serialTextView.setOnClickListener(v -> {
            nfcScanner.startScan(this);
            toggleScanning(true);
        });
        this.serialTextView.setText(this.prefs.getValue("SERIAL_NUMBER", getString(R.string.DEFAULT_SERIAL_NUMBER)));

        if (this.cardEmulator.isSimulating())
            toggleSimulation();
    }

    private void toggleScanning(boolean state) {
        if (state) {
            statusText.setText(R.string.SCANNING);
            statusIcon.startAnimation(pulseAnimation);
        }
        else {
            statusText.setText(isSimulating ? R.string.SIMULATING : R.string.IDLE);
            statusIcon.clearAnimation();
        }
    }

    private void toggleSimulation() {
        if (!isSimulating) {
            String serialNumber = this.prefs.getValue("SERIAL_NUMBER", null);
            if (serialNumber == null) {
                Toast.makeText(getApplicationContext(), "No card to simulate =(", Toast.LENGTH_SHORT).show();
                return;
            }
            if (!this.cardEmulator.simulate(serialNumber)) {
                Toast.makeText(getApplicationContext(), "Failed to simulate card =(", Toast.LENGTH_SHORT).show();
                return;
            }
        }
        else if (!this.cardEmulator.restore()) {
            Toast.makeText(getApplicationContext(), "Failed to restore NFC =(", Toast.LENGTH_SHORT).show();
            return;
        }

        isSimulating = !isSimulating;

        int targetBg = isSimulating ? Color.parseColor("#262626") : Color.WHITE;
        int targetText = isSimulating ? Color.WHITE : Color.BLACK;

        statusText.setText(isSimulating ? R.string.SIMULATING : R.string.IDLE);
        btnToggle.setText(isSimulating ? R.string.RESTORE : R.string.SIMULATE);
        btnToggle.setRippleColor(ColorStateList.valueOf(Color.TRANSPARENT));

        if (isSimulating)
            statusIcon.startAnimation(pulseAnimation);
        else
            statusIcon.clearAnimation();

        animateColorChange(targetBg, targetText);
    }

    private void animateColorChange(int bgColor, int textColor) {
        ValueAnimator bgAnim = ValueAnimator.ofArgb(Objects.requireNonNull(btnToggle.getBackgroundTintList()).getDefaultColor(), bgColor);
        bgAnim.addUpdateListener(a -> btnToggle.setBackgroundTintList(ColorStateList.valueOf((int)a.getAnimatedValue())));

        ValueAnimator txtAnim = ValueAnimator.ofArgb(btnToggle.getCurrentTextColor(), textColor);
        txtAnim.addUpdateListener(a -> btnToggle.setTextColor((int)a.getAnimatedValue()));

        AnimatorSet set = new AnimatorSet();
        set.playTogether(bgAnim, txtAnim);
        set.setDuration(300);
        set.start();
    }
}
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
    private volatile boolean isPendingOperation = false; 
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
    }

    private void setLoading(boolean isLoading) {
    }

}
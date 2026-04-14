package com.example.antyspamer;

import android.Manifest;
import android.app.AlertDialog;
import android.app.NotificationChannel;
import android.app.NotificationManager;
import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;
import android.content.IntentFilter;
import android.content.SharedPreferences;
import android.content.pm.PackageManager;
import android.database.Cursor;
import android.graphics.Color;
import android.media.Ringtone;
import android.media.RingtoneManager;
import android.net.Uri;
import android.os.Build;
import android.os.Bundle;
import android.provider.ContactsContract;
import android.text.SpannableString;
import android.text.Spanned;
import android.text.style.ForegroundColorSpan;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.view.WindowManager;
import android.widget.Button;
import android.widget.ImageButton;
import android.widget.TextView;
import android.widget.Toast;

import androidx.activity.result.ActivityResultLauncher;
import androidx.activity.result.contract.ActivityResultContracts;
import androidx.annotation.NonNull;
import androidx.appcompat.app.AppCompatActivity;
import androidx.core.app.ActivityCompat;
import androidx.core.app.NotificationCompat;
import androidx.core.app.NotificationManagerCompat;
import androidx.core.content.ContextCompat;
import androidx.recyclerview.widget.LinearLayoutManager;
import androidx.recyclerview.widget.RecyclerView;

import com.google.android.material.textfield.TextInputEditText;

import java.util.ArrayList;
import java.util.List;

public class MainActivity extends AppCompatActivity {

    private Button startBtn, addGuardianBtn, settingsBtn;
    private ImageButton historyBtn;
    private TextView statusText, speechText, warningText;
    private View micIndicator;
    private RecyclerView guardiansRecyclerView;
    private GuardianAdapter guardianAdapter;
    private List<DatabaseHelper.Guardian> guardianList;
    
    private SharedPreferences sharedPreferences;
    private DatabaseHelper dbHelper;

    private static final int PERMISSION_REQUEST_CODE = 101;
    private static final String CHANNEL_ID = "TrustCallAlerts";
    private boolean isProtectionActive = false;

    private TextInputEditText currentDialogNameInput;
    private TextInputEditText currentDialogPhoneInput;

    private final ActivityResultLauncher<Intent> contactPickerLauncher = registerForActivityResult(
            new ActivityResultContracts.StartActivityForResult(),
            result -> {
                if (result.getResultCode() == RESULT_OK && result.getData() != null) {
                    Uri contactUri = result.getData().getData();
                    queryContactData(contactUri);
                }
            }
    );

    private final String[] financialKeywords = {"blik", "przelew", "pieniądze", "bank", "karta", "pin", "kredyt"};
    private final String[] securityKeywords = {"hasło", "login", "weryfikacja", "autoryzacja", "zablokowane", "sms"};
    private final String[] urgentKeywords = {"policja", "prokurator", "wnuczek", "wypadek", "szpital", "szybko", "pilne"};

    private final BroadcastReceiver speechReceiver = new BroadcastReceiver() {
        @Override
        public void onReceive(Context context, Intent intent) {
            if (intent != null && intent.hasExtra("text")) {
                displayRecognizedText(intent.getStringExtra("text"), getActiveKeywords());
            }
        }
    };

    private final BroadcastReceiver confirmationReceiver = new BroadcastReceiver() {
        @Override
        public void onReceive(Context context, Intent intent) {
            if (intent != null && "POTWIERDZONO".equals(intent.getStringExtra("status"))) {
                playAlarmSound();
                showTopNotification("ZAGROŻENIE POTWIERDZONE!", "Rozłącz się natychmiast!");
                if (warningText != null) {
                    warningText.setText("!!! OPIEKUN POTWIERDZIŁ OSZUSTWO !!!");
                    warningText.setTextColor(Color.RED);
                }
            }
        }
    };

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);
        
        dbHelper = new DatabaseHelper(this);
        sharedPreferences = getSharedPreferences("SpamPrefs", MODE_PRIVATE);
        
        createNotificationChannel();
        getWindow().addFlags(WindowManager.LayoutParams.FLAG_KEEP_SCREEN_ON);

        startBtn = findViewById(R.id.startBtn);
        addGuardianBtn = findViewById(R.id.addGuardianBtn);
        historyBtn = findViewById(R.id.historyBtn);
        settingsBtn = findViewById(R.id.settingsBtn);
        statusText = findViewById(R.id.statusText);
        speechText = findViewById(R.id.speechText);
        warningText = findViewById(R.id.warningText);
        micIndicator = findViewById(R.id.micIndicator);
        guardiansRecyclerView = findViewById(R.id.guardiansRecyclerView);

        guardiansRecyclerView.setLayoutManager(new LinearLayoutManager(this));
        loadGuardians();

        addGuardianBtn.setOnClickListener(v -> showAddGuardianDialog());
        historyBtn.setOnClickListener(v -> startActivity(new Intent(this, HistoryActivity.class)));
        settingsBtn.setOnClickListener(v -> startActivity(new Intent(this, SettingsActivity.class)));
        startBtn.setOnClickListener(v -> toggleProtection());

        IntentFilter speechFilter = new IntentFilter("com.example.antyspamer.SPEECH_RESULT");
        IntentFilter confirmFilter = new IntentFilter("com.example.antyspamer.SMS_CONFIRMATION");
        
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
            registerReceiver(speechReceiver, speechFilter, Context.RECEIVER_EXPORTED);
            registerReceiver(confirmationReceiver, confirmFilter, Context.RECEIVER_EXPORTED);
        } else {
            registerReceiver(speechReceiver, speechFilter);
            registerReceiver(confirmationReceiver, confirmFilter);
        }
        updateUIState();
    }

    private void loadGuardians() {
        guardianList = dbHelper.getAllGuardians();
        guardianAdapter = new GuardianAdapter(guardianList);
        guardiansRecyclerView.setAdapter(guardianAdapter);
    }

    private void showAddGuardianDialog() {
        View view = LayoutInflater.from(this).inflate(R.layout.dialog_add_guardian, null);
        currentDialogNameInput = view.findViewById(R.id.dialogGuardianName);
        currentDialogPhoneInput = view.findViewById(R.id.dialogGuardianPhone);
        Button pickContactBtn = view.findViewById(R.id.pickContactBtn);

        pickContactBtn.setOnClickListener(v -> {
            if (ContextCompat.checkSelfPermission(this, Manifest.permission.READ_CONTACTS) != PackageManager.PERMISSION_GRANTED) {
                ActivityCompat.requestPermissions(this, new String[]{Manifest.permission.READ_CONTACTS}, 102);
            } else {
                pickContact();
            }
        });

        new AlertDialog.Builder(this)
            .setView(view)
            .setPositiveButton("Dodaj", (d, w) -> {
                String n = currentDialogNameInput.getText().toString().trim();
                String p = currentDialogPhoneInput.getText().toString().trim();
                if (!n.isEmpty() && !p.isEmpty()) {
                    dbHelper.addGuardian(n, p);
                    loadGuardians();
                } else {
                    Toast.makeText(this, "Wypełnij wszystkie pola", Toast.LENGTH_SHORT).show();
                }
            })
            .setNegativeButton("Anuluj", null)
            .show();
    }

    private void pickContact() {
        Intent intent = new Intent(Intent.ACTION_PICK, ContactsContract.CommonDataKinds.Phone.CONTENT_URI);
        contactPickerLauncher.launch(intent);
    }

    private void queryContactData(Uri contactUri) {
        String[] projection = {ContactsContract.CommonDataKinds.Phone.NUMBER, ContactsContract.CommonDataKinds.Phone.DISPLAY_NAME};
        try (Cursor cursor = getContentResolver().query(contactUri, projection, null, null, null)) {
            if (cursor != null && cursor.moveToFirst()) {
                String number = cursor.getString(0);
                String name = cursor.getString(1);
                if (currentDialogNameInput != null) currentDialogNameInput.setText(name);
                if (currentDialogPhoneInput != null) currentDialogPhoneInput.setText(number);
            }
        } catch (Exception e) {
            e.printStackTrace();
        }
    }

    private void toggleProtection() {
        if (isProtectionActive) {
            isProtectionActive = false;
            stopService(new Intent(this, MonitoringService.class));
            updateUIState();
        } else checkPermissions();
    }

    private void updateUIState() {
        if (isProtectionActive) {
            startBtn.setText("Wyłącz ochronę");
            statusText.setText("Ochrona aktywna");
            statusText.setTextColor(ContextCompat.getColor(this, R.color.mic_listening));
            if (micIndicator != null) micIndicator.setBackgroundTintList(ContextCompat.getColorStateList(this, R.color.mic_listening));
        } else {
            startBtn.setText("Włącz ochronę");
            statusText.setText("Ochrona wyłączona");
            statusText.setTextColor(ContextCompat.getColor(this, R.color.onSurfaceVariant)); // Naprawiono kolor dla trybu ciemnego
            if (micIndicator != null) micIndicator.setBackgroundTintList(ContextCompat.getColorStateList(this, R.color.mic_idle));
        }
    }

    private void createNotificationChannel() {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            NotificationChannel c = new NotificationChannel(CHANNEL_ID, "Alerty TrustCall", NotificationManager.IMPORTANCE_HIGH);
            NotificationManager manager = (NotificationManager) getSystemService(Context.NOTIFICATION_SERVICE);
            if (manager != null) manager.createNotificationChannel(c);
        }
    }

    private void showTopNotification(String t, String c) {
        NotificationCompat.Builder b = new NotificationCompat.Builder(this, CHANNEL_ID)
                .setSmallIcon(android.R.drawable.ic_dialog_alert).setContentTitle(t).setContentText(c)
                .setPriority(NotificationCompat.PRIORITY_HIGH).setAutoCancel(true);
        if (ActivityCompat.checkSelfPermission(this, Manifest.permission.POST_NOTIFICATIONS) == PackageManager.PERMISSION_GRANTED) {
            NotificationManagerCompat.from(this).notify(1, b.build());
        }
    }

    private void playAlarmSound() {
        try {
            Uri n = RingtoneManager.getDefaultUri(RingtoneManager.TYPE_ALARM);
            Ringtone r = RingtoneManager.getRingtone(getApplicationContext(), n);
            if (r != null) r.play();
        } catch (Exception e) {}
    }

    private ArrayList<String> getActiveKeywords() {
        ArrayList<String> list = new ArrayList<>();
        if (sharedPreferences.getBoolean("detect_financial", true)) {
            for (String s : financialKeywords) list.add(s);
        }
        if (sharedPreferences.getBoolean("detect_security", true)) {
            for (String s : securityKeywords) list.add(s);
        }
        if (sharedPreferences.getBoolean("detect_urgent", true)) {
            for (String s : urgentKeywords) list.add(s);
        }
        return list;
    }

    private void displayRecognizedText(String text, ArrayList<String> keywords) {
        SpannableString spannable = new SpannableString(text);
        for (String k : keywords) {
            int i = text.toLowerCase().indexOf(k.toLowerCase());
            while (i >= 0) {
                spannable.setSpan(new ForegroundColorSpan(Color.RED), i, i + k.length(), Spanned.SPAN_EXCLUSIVE_EXCLUSIVE);
                i = text.toLowerCase().indexOf(k.toLowerCase(), i + 1);
            }
        }
        speechText.setText(spannable);
    }

    private void checkPermissions() {
        ArrayList<String> toRequest = new ArrayList<>();
        toRequest.add(Manifest.permission.RECORD_AUDIO);
        toRequest.add(Manifest.permission.SEND_SMS);
        toRequest.add(Manifest.permission.RECEIVE_SMS);
        toRequest.add(Manifest.permission.READ_PHONE_STATE);
        toRequest.add(Manifest.permission.READ_CALL_LOG); // Dodano READ_CALL_LOG
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
            toRequest.add(Manifest.permission.POST_NOTIFICATIONS); // Dodano POST_NOTIFICATIONS dla nowszych Androidów
        }

        ArrayList<String> neededPermissions = new ArrayList<>();
        for (String p : toRequest) {
            if (ContextCompat.checkSelfPermission(this, p) != PackageManager.PERMISSION_GRANTED) {
                neededPermissions.add(p);
            }
        }

        if (neededPermissions.isEmpty()) {
            startMonitoringService();
        } else {
            ActivityCompat.requestPermissions(this, neededPermissions.toArray(new String[0]), PERMISSION_REQUEST_CODE);
        }
    }

    private void startMonitoringService() {
        isProtectionActive = true;
        ContextCompat.startForegroundService(this, new Intent(this, MonitoringService.class));
        updateUIState();
    }

    @Override
    public void onRequestPermissionsResult(int rc, @NonNull String[] ps, @NonNull int[] gr) {
        super.onRequestPermissionsResult(rc, ps, gr);
        if (rc == PERMISSION_REQUEST_CODE) {
            boolean allGranted = true;
            for (int res : gr) if (res != PackageManager.PERMISSION_GRANTED) allGranted = false;
            if (allGranted) startMonitoringService();
            else Toast.makeText(this, "Wymagane uprawnienia!", Toast.LENGTH_SHORT).show();
        } else if (rc == 102) {
            if (gr.length > 0 && gr[0] == PackageManager.PERMISSION_GRANTED) {
                pickContact();
            } else {
                Toast.makeText(this, "Dostęp do kontaktów odrzucony", Toast.LENGTH_SHORT).show();
            }
        }
    }

    @Override
    protected void onDestroy() {
        super.onDestroy();
        try { unregisterReceiver(speechReceiver); unregisterReceiver(confirmationReceiver); } catch (Exception e) {}
    }

    private class GuardianAdapter extends RecyclerView.Adapter<GuardianAdapter.VH> {
        private List<DatabaseHelper.Guardian> list;
        public GuardianAdapter(List<DatabaseHelper.Guardian> list) { this.list = list; }
        @NonNull @Override public VH onCreateViewHolder(@NonNull ViewGroup p, int vt) {
            return new VH(LayoutInflater.from(p.getContext()).inflate(R.layout.item_guardian, p, false));
        }
        @Override public void onBindViewHolder(@NonNull VH h, int pos) {
            DatabaseHelper.Guardian g = list.get(pos);
            h.n.setText(g.name); h.p.setText(g.phone);
            h.d.setOnClickListener(v -> { dbHelper.deleteGuardian(g.id); loadGuardians(); });
        }
        @Override public int getItemCount() { return list.size(); }
        class VH extends RecyclerView.ViewHolder {
            TextView n, p; ImageButton d;
            public VH(View v) { super(v); n = v.findViewById(R.id.guardianName); p = v.findViewById(R.id.guardianPhone); d = v.findViewById(R.id.deleteGuardianBtn); }
        }
    }
}

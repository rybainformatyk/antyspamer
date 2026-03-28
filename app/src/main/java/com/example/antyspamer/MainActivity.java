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
import android.graphics.Color;
import android.media.Ringtone;
import android.media.RingtoneManager;
import android.net.Uri;
import android.os.Build;
import android.os.Bundle;
import android.text.InputType;
import android.text.SpannableString;
import android.text.Spanned;
import android.text.style.ForegroundColorSpan;
import android.view.View;
import android.view.WindowManager;
import android.widget.Button;
import android.widget.EditText;
import android.widget.ImageButton;
import android.widget.LinearLayout;
import android.widget.TextView;
import android.widget.Toast;

import androidx.annotation.NonNull;
import androidx.appcompat.app.AppCompatActivity;
import androidx.core.app.ActivityCompat;
import androidx.core.app.NotificationCompat;
import androidx.core.app.NotificationManagerCompat;
import androidx.core.content.ContextCompat;
import androidx.recyclerview.widget.LinearLayoutManager;
import androidx.recyclerview.widget.RecyclerView;

import java.util.ArrayList;
import java.util.List;

public class MainActivity extends AppCompatActivity {

    private Button startBtn, addGuardianBtn, settingsBtn;
    private ImageButton historyBtn;
    private TextView statusText, speechText, warningText;
    private View micIndicator;
    private RecyclerView guardiansRecyclerView;
    private GuardianAdapter guardianAdapter;
    private List<Guardian> guardianList;
    
    private SharedPreferences sharedPreferences;
    private DatabaseHelper dbHelper;

    private static final int PERMISSION_REQUEST_CODE = 101;
    private static final String CHANNEL_ID = "TrustCallAlerts";
    private boolean isProtectionActive = false;

    private final String[] financialKeywords = {
            "blik", "blika", "bliku", "blikiem", "kodzik", "kod", "kodu", "przelew", "przelewik", 
            "pieniądze", "pieniążki", "pieniążków", "kasa", "kaska", "konto", "konta", "bank", 
            "banku", "karta", "karty", "pin", "pinu", "debet", "kredyt", "pożyczka", "pożyczkę"
    };
    private final String[] securityKeywords = {
            "hasło", "hasełko", "login", "loginek", "weryfikacja", "weryfikacji", "autoryzacja", 
            "dostęp", "dostępu", "zablokowane", "zablokowali", "potwierdź", "sms", "esemes"
    };
    private final String[] urgentKeywords = {
            "policja", "policjant", "policjancik", "prokurator", "komenda", "wnuk", "wnuczek", 
            "wnuczuś", "syn", "synuś", "córka", "córeczka", "nagroda", "nagródka", "wygrana", 
            "pilne", "szybko", "natychmiast", "wypadek", "szpital", "ratuj", "pomóż", "płacze"
    };

    private final BroadcastReceiver speechReceiver = new BroadcastReceiver() {
        @Override
        public void onReceive(Context context, Intent intent) {
            String text = intent.getStringExtra("text");
            if (text != null) displayRecognizedText(text, getActiveKeywords());
        }
    };

    private final BroadcastReceiver confirmationReceiver = new BroadcastReceiver() {
        @Override
        public void onReceive(Context context, Intent intent) {
            String status = intent.getStringExtra("status");
            if ("POTWIERDZONO".equals(status)) {
                playAlarmSound();
                showTopNotification("OPIEKUN POTWIERDZIŁ ZAGROŻENIE!", "Natychmiast zakończ rozmowę!");
                warningText.setText("!!! OPIEKUN POTWIERDZIŁ ZAGROŻENIE !!!");
                warningText.setTextColor(Color.RED);
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
        guardianAdapter = new GuardianAdapter(guardianList, guardian -> {
            dbHelper.deleteGuardian(guardian.getId());
            loadGuardians();
        });
        guardiansRecyclerView.setAdapter(guardianAdapter);
    }

    private void showAddGuardianDialog() {
        AlertDialog.Builder builder = new AlertDialog.Builder(this);
        builder.setTitle("Dodaj Opiekuna");

        LinearLayout layout = new LinearLayout(this);
        layout.setOrientation(LinearLayout.VERTICAL);
        layout.setPadding(50, 20, 50, 20);

        final EditText nameInput = new EditText(this);
        nameInput.setHint("Imię (np. Syn)");
        layout.addView(nameInput);

        final EditText phoneInput = new EditText(this);
        phoneInput.setHint("Numer telefonu");
        phoneInput.setInputType(InputType.TYPE_CLASS_PHONE);
        layout.addView(phoneInput);

        builder.setView(layout);

        builder.setPositiveButton("Dodaj", (dialog, which) -> {
            String name = nameInput.getText().toString();
            String phone = phoneInput.getText().toString();
            if (!name.isEmpty() && !phone.isEmpty()) {
                dbHelper.addGuardian(name, phone);
                loadGuardians();
            } else {
                Toast.makeText(this, "Wypełnij oba pola", Toast.LENGTH_SHORT).show();
            }
        });
        builder.setNegativeButton("Anuluj", (dialog, which) -> dialog.cancel());

        builder.show();
    }

    private void toggleProtection() {
        if (isProtectionActive) stopProtection();
        else checkPermissions();
    }

    private void stopProtection() {
        isProtectionActive = false;
        stopService(new Intent(this, MonitoringService.class));
        updateUIState();
    }

    private void updateUIState() {
        if (isProtectionActive) {
            startBtn.setText("Wyłącz ochronę");
            statusText.setText("Status: OCHRONA WŁĄCZONA");
            statusText.setTextColor(Color.GREEN);
            micIndicator.setBackgroundTintList(ContextCompat.getColorStateList(this, android.R.color.holo_green_light));
        } else {
            startBtn.setText("Włącz ochronę");
            statusText.setText("Status: OCHRONA WYŁĄCZONA");
            statusText.setTextColor(Color.RED);
            micIndicator.setBackgroundTintList(ContextCompat.getColorStateList(this, android.R.color.darker_gray));
        }
    }

    private void createNotificationChannel() {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            NotificationChannel channel = new NotificationChannel(CHANNEL_ID, "Alerty TrustCall", NotificationManager.IMPORTANCE_HIGH);
            NotificationManager manager = getSystemService(NotificationManager.class);
            if (manager != null) manager.createNotificationChannel(channel);
        }
    }

    private void showTopNotification(String title, String content) {
        NotificationCompat.Builder builder = new NotificationCompat.Builder(this, CHANNEL_ID)
                .setSmallIcon(android.R.drawable.ic_dialog_alert)
                .setContentTitle(title)
                .setContentText(content)
                .setPriority(NotificationCompat.PRIORITY_HIGH)
                .setAutoCancel(true);

        NotificationManagerCompat notificationManager = NotificationManagerCompat.from(this);
        if (ActivityCompat.checkSelfPermission(this, Manifest.permission.POST_NOTIFICATIONS) == PackageManager.PERMISSION_GRANTED) {
            notificationManager.notify(1, builder.build());
        }
    }

    private void playAlarmSound() {
        try {
            Uri notification = RingtoneManager.getDefaultUri(RingtoneManager.TYPE_ALARM);
            Ringtone r = RingtoneManager.getRingtone(getApplicationContext(), notification);
            r.play();
        } catch (Exception e) {}
    }

    private ArrayList<String> getActiveKeywords() {
        ArrayList<String> list = new ArrayList<>();
        if (sharedPreferences.getBoolean("detect_financial", true)) for (String s : financialKeywords) list.add(s);
        if (sharedPreferences.getBoolean("detect_security", true)) for (String s : securityKeywords) list.add(s);
        if (sharedPreferences.getBoolean("detect_urgent", true)) for (String s : urgentKeywords) list.add(s);
        return list;
    }

    private void displayRecognizedText(String text, ArrayList<String> activeKeywords) {
        SpannableString spannable = new SpannableString(text);
        for (String keyword : activeKeywords) {
            int index = text.toLowerCase().indexOf(keyword.toLowerCase());
            while (index >= 0) {
                spannable.setSpan(new ForegroundColorSpan(Color.RED), index, index + keyword.length(), Spanned.SPAN_EXCLUSIVE_EXCLUSIVE);
                index = text.toLowerCase().indexOf(keyword.toLowerCase(), index + 1);
            }
        }
        speechText.setText(spannable);
    }

    private void checkPermissions() {
        String[] permissions = Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU ?
            new String[]{Manifest.permission.RECORD_AUDIO, Manifest.permission.SEND_SMS, Manifest.permission.RECEIVE_SMS, Manifest.permission.POST_NOTIFICATIONS, Manifest.permission.READ_PHONE_STATE} :
            new String[]{Manifest.permission.RECORD_AUDIO, Manifest.permission.SEND_SMS, Manifest.permission.RECEIVE_SMS, Manifest.permission.READ_PHONE_STATE};

        ArrayList<String> toRequest = new ArrayList<>();
        for (String p : permissions) if (ContextCompat.checkSelfPermission(this, p) != PackageManager.PERMISSION_GRANTED) toRequest.add(p);
        
        if (toRequest.isEmpty()) startMonitoringService();
        else ActivityCompat.requestPermissions(this, toRequest.toArray(new String[0]), PERMISSION_REQUEST_CODE);
    }

    private void startMonitoringService() {
        isProtectionActive = true;
        ContextCompat.startForegroundService(this, new Intent(this, MonitoringService.class));
        updateUIState();
    }

    @Override
    public void onRequestPermissionsResult(int requestCode, @NonNull String[] permissions, @NonNull int[] grantResults) {
        super.onRequestPermissionsResult(requestCode, permissions, grantResults);
        if (requestCode == PERMISSION_REQUEST_CODE) {
            boolean allGranted = true;
            for (int res : grantResults) if (res != PackageManager.PERMISSION_GRANTED) allGranted = false;
            if (allGranted) startMonitoringService();
        }
    }

    @Override
    protected void onDestroy() {
        super.onDestroy();
        try {
            unregisterReceiver(speechReceiver);
            unregisterReceiver(confirmationReceiver);
        } catch (Exception e) {}
    }
}

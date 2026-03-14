package com.example.antyspamer;

import android.Manifest;
import android.app.NotificationChannel;
import android.app.NotificationManager;
import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;
import android.content.IntentFilter;
import android.content.SharedPreferences;
import android.content.pm.PackageManager;
import android.graphics.Color;
import android.os.Build;
import android.os.Bundle;
import android.os.Handler;
import android.os.Looper;
import android.speech.RecognitionListener;
import android.speech.RecognizerIntent;
import android.speech.SpeechRecognizer;
import android.telephony.SmsManager;
import android.text.SpannableString;
import android.text.Spanned;
import android.text.style.ForegroundColorSpan;
import android.util.Log;
import android.view.View;
import android.view.WindowManager;
import android.widget.Button;
import android.widget.TextView;
import android.widget.Toast;

import androidx.annotation.NonNull;
import androidx.appcompat.app.AppCompatActivity;
import androidx.core.app.ActivityCompat;
import androidx.core.app.NotificationCompat;
import androidx.core.app.NotificationManagerCompat;
import androidx.core.content.ContextCompat;

import com.google.android.material.textfield.TextInputLayout;
import com.google.android.material.textfield.TextInputEditText;

import java.util.ArrayList;

public class MainActivity extends AppCompatActivity {

    private TextInputEditText phone1, phone2;
    private TextInputLayout phone1Layout, phone2Layout;
    private Button saveBtn, startBtn;
    private TextView statusText, speechText, warningText;
    private View micIndicator;
    private SharedPreferences sharedPreferences;
    private SpeechRecognizer speechRecognizer;
    private Intent speechRecognizerIntent;

    private static final int PERMISSION_REQUEST_CODE = 101;
    private static final String CHANNEL_ID = "TrustCallAlerts";
    private boolean isListening = false;
    private boolean isProtectionActive = false; // Nowa zmienna stanu
    private final Handler restartHandler = new Handler(Looper.getMainLooper());
    private long lastSmsTime = 0; 

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

    private final BroadcastReceiver confirmationReceiver = new BroadcastReceiver() {
        @Override
        public void onReceive(Context context, Intent intent) {
            String status = intent.getStringExtra("status");
            if ("POTWIERDZONO".equals(status)) {
                showTopNotification("OPIEKUN POTWIERDZIŁ ZAGROŻENIE!", "Natychmiast zakończ rozmowę!");
                warningText.setText("!!! OPIEKUN POTWIERDZIŁ ZAGROŻENIE !!!");
                warningText.setTextColor(Color.RED);
                warningText.setTextSize(24);
            } else if ("ODRZUCONO".equals(status)) {
                warningText.setText("Opiekun zignorował alert.");
                warningText.setTextColor(Color.GRAY);
                warningText.setTextSize(16);
            }
        }
    };

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);
        
        createNotificationChannel();
        getWindow().addFlags(WindowManager.LayoutParams.FLAG_KEEP_SCREEN_ON);

        phone1 = findViewById(R.id.phone1);
        phone2 = findViewById(R.id.phone2);
        phone1Layout = findViewById(R.id.phone1Layout);
        phone2Layout = findViewById(R.id.phone2Layout);
        saveBtn = findViewById(R.id.saveBtn);
        startBtn = findViewById(R.id.startBtn);
        statusText = findViewById(R.id.statusText);
        speechText = findViewById(R.id.speechText);
        warningText = findViewById(R.id.warningText);
        micIndicator = findViewById(R.id.micIndicator);

        sharedPreferences = getSharedPreferences("SpamPrefs", MODE_PRIVATE);

        phone1.setText(sharedPreferences.getString("num1", ""));
        phone2.setText(sharedPreferences.getString("num2", ""));

        saveBtn.setOnClickListener(v -> {
            sharedPreferences.edit()
                    .putString("num1", phone1.getText().toString())
                    .putString("num2", phone2.getText().toString())
                    .apply();
            Toast.makeText(this, "Zapisano numery", Toast.LENGTH_SHORT).show();
        });

        startBtn.setOnClickListener(v -> toggleProtection());

        View.OnClickListener openSettings = v -> startActivity(new Intent(this, SettingsActivity.class));
        phone1Layout.setEndIconOnClickListener(openSettings);
        phone2Layout.setEndIconOnClickListener(openSettings);
        
        setupSpeechIntent();

        IntentFilter filter = new IntentFilter("com.example.antyspamer.SMS_CONFIRMATION");
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
            registerReceiver(confirmationReceiver, filter, Context.RECEIVER_EXPORTED);
        } else {
            registerReceiver(confirmationReceiver, filter);
        }
        
        updateUIState();
    }

    private void toggleProtection() {
        if (isProtectionActive) {
            stopProtection();
        } else {
            checkPermissions();
        }
    }

    private void stopProtection() {
        isProtectionActive = false;
        if (speechRecognizer != null) {
            speechRecognizer.stopListening();
            speechRecognizer.cancel();
            speechRecognizer.destroy();
            speechRecognizer = null;
        }
        isListening = false;
        restartHandler.removeCallbacksAndMessages(null);
        updateUIState();
        showTopNotification("TrustCall", "Ochrona została wyłączona.");
    }

    private void updateUIState() {
        if (isProtectionActive) {
            startBtn.setText("Wyłącz ochronę");
            statusText.setText("Status: OCHRONA WŁĄCZONA");
            statusText.setTextColor(Color.GREEN);
            micIndicator.setBackgroundTintList(ContextCompat.getColorStateList(this, R.color.mic_listening));
        } else {
            startBtn.setText("Włącz ochronę");
            statusText.setText("Status: OCHRONA WYŁĄCZONA");
            statusText.setTextColor(Color.RED);
            micIndicator.setBackgroundTintList(ContextCompat.getColorStateList(this, R.color.mic_idle));
            micIndicator.setScaleX(1f);
            micIndicator.setScaleY(1f);
        }
    }

    private void createNotificationChannel() {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            NotificationChannel channel = new NotificationChannel(CHANNEL_ID, "Alerty TrustCall", NotificationManager.IMPORTANCE_HIGH);
            channel.setDescription("Kanał dla pilnych powiadomień o oszustwach");
            channel.enableLights(true);
            channel.setLightColor(Color.RED);
            channel.enableVibration(true);
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
                .setCategory(NotificationCompat.CATEGORY_ALARM)
                .setAutoCancel(true);

        NotificationManagerCompat notificationManager = NotificationManagerCompat.from(this);
        if (ActivityCompat.checkSelfPermission(this, Manifest.permission.POST_NOTIFICATIONS) == PackageManager.PERMISSION_GRANTED) {
            notificationManager.notify(1, builder.build());
        }
    }

    private void setupSpeechIntent() {
        speechRecognizerIntent = new Intent(RecognizerIntent.ACTION_RECOGNIZE_SPEECH);
        speechRecognizerIntent.putExtra(RecognizerIntent.EXTRA_LANGUAGE_MODEL, RecognizerIntent.LANGUAGE_MODEL_FREE_FORM);
        speechRecognizerIntent.putExtra(RecognizerIntent.EXTRA_LANGUAGE, "pl-PL");
        speechRecognizerIntent.putExtra(RecognizerIntent.EXTRA_PARTIAL_RESULTS, true);
    }

    private void initSpeechRecognizer() {
        if (speechRecognizer != null) speechRecognizer.destroy();
        speechRecognizer = SpeechRecognizer.createSpeechRecognizer(getApplicationContext());
        speechRecognizer.setRecognitionListener(new RecognitionListener() {
            @Override public void onReadyForSpeech(Bundle params) {
                isListening = true;
                if (isProtectionActive) {
                    statusText.setText("Status: TrustCall czuwa...");
                    micIndicator.setBackgroundTintList(ContextCompat.getColorStateList(MainActivity.this, R.color.mic_listening));
                }
            }
            @Override public void onBeginningOfSpeech() {
                micIndicator.setBackgroundTintList(ContextCompat.getColorStateList(MainActivity.this, R.color.mic_active));
            }
            @Override public void onRmsChanged(float rmsdB) {
                float scale = 1.0f + (rmsdB / 10f);
                micIndicator.setScaleX(scale > 1 ? scale : 1);
                micIndicator.setScaleY(scale > 1 ? scale : 1);
            }
            @Override public void onBufferReceived(byte[] buffer) {}
            @Override public void onEndOfSpeech() {
                isListening = false;
                micIndicator.setBackgroundTintList(ContextCompat.getColorStateList(MainActivity.this, R.color.mic_idle));
            }
            @Override public void onError(int error) {
                isListening = false;
                micIndicator.setBackgroundTintList(ContextCompat.getColorStateList(MainActivity.this, R.color.mic_error));
                if (isProtectionActive && sharedPreferences.getBoolean("auto_restart", true)) {
                    restartHandler.postDelayed(() -> startMonitoring(), 1500);
                }
            }
            @Override public void onResults(Bundle results) { processSpeechResults(results); if (isProtectionActive) startMonitoring(); }
            @Override public void onPartialResults(Bundle partialResults) { processSpeechResults(partialResults); }
            @Override public void onEvent(int eventType, Bundle params) {}
        });
    }

    private void processSpeechResults(Bundle bundle) {
        ArrayList<String> matches = bundle.getStringArrayList(SpeechRecognizer.RESULTS_RECOGNITION);
        if (matches != null && !matches.isEmpty()) {
            String text = matches.get(0);
            ArrayList<String> activeKeywords = getActiveKeywords();
            displayRecognizedText(text, activeKeywords);
            checkForKeywords(text, activeKeywords);
        }
    }

    private ArrayList<String> getActiveKeywords() {
        ArrayList<String> list = new ArrayList<>();
        if (sharedPreferences.getBoolean("detect_financial", true)) for (String s : financialKeywords) list.add(s);
        if (sharedPreferences.getBoolean("detect_security", true)) for (String s : securityKeywords) list.add(s);
        if (sharedPreferences.getBoolean("detect_urgent", true)) for (String s : urgentKeywords) list.add(s);
        return list;
    }

    private void displayRecognizedText(String text, ArrayList<String> activeKeywords) {
        String lowerText = text.toLowerCase();
        SpannableString spannable = new SpannableString(text);
        for (String keyword : activeKeywords) {
            int index = lowerText.indexOf(keyword);
            while (index >= 0) {
                spannable.setSpan(new ForegroundColorSpan(Color.parseColor("#FF8A80")), index, index + keyword.length(), Spanned.SPAN_EXCLUSIVE_EXCLUSIVE);
                index = lowerText.indexOf(keyword, index + 1);
            }
        }
        speechText.setText(spannable);
    }

    private void checkForKeywords(String text, ArrayList<String> activeKeywords) {
        String lowerText = text.toLowerCase();
        for (String keyword : activeKeywords) {
            if (lowerText.contains(keyword)) {
                String warning = "WYKRYTO: " + keyword.toUpperCase();
                showTopNotification("TrustCall: ALERT OSZUSTWA", "Wykryto podejrzane słowo: " + keyword);
                warningText.setText("!!! " + warning + " !!!");
                sendAlertSms(keyword.toUpperCase());
                return;
            }
        }
    }

    private void sendAlertSms(String keyword) {
        long currentTime = System.currentTimeMillis();
        if (currentTime - lastSmsTime < 30000) return;

        String n1 = phone1.getText().toString();
        String n2 = phone2.getText().toString();
        String name = sharedPreferences.getString("user_name", "Bliska osoba");
        String surname = sharedPreferences.getString("user_surname", "");
        
        String fullMessage = "TrustCall ALERT! " + name + " " + surname + " moze rozmawiac z oszustem. Wykryto: " + keyword + 
                ". Odpisz POTWIERDZAM aby wyslac ostrzezenie, lub ODRZUCAM jesli rozmowa jest bezpieczna.";

        if (ContextCompat.checkSelfPermission(this, Manifest.permission.SEND_SMS) == PackageManager.PERMISSION_GRANTED) {
            try {
                SmsManager smsManager;
                if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.S) {
                    smsManager = this.getSystemService(SmsManager.class);
                } else {
                    smsManager = SmsManager.getDefault();
                }

                if (!n1.isEmpty()) smsManager.sendTextMessage(n1, null, fullMessage, null, null);
                if (!n2.isEmpty()) smsManager.sendTextMessage(n2, null, fullMessage, null, null);
                
                lastSmsTime = currentTime;
            } catch (Exception e) {
                Log.e("TrustCall", "SMS Error", e);
            }
        }
    }

    private void checkPermissions() {
        ArrayList<String> toRequest = new ArrayList<>();
        String[] permissions = {Manifest.permission.RECORD_AUDIO, Manifest.permission.SEND_SMS, Manifest.permission.RECEIVE_SMS};
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
            permissions = new String[]{Manifest.permission.RECORD_AUDIO, Manifest.permission.SEND_SMS, Manifest.permission.RECEIVE_SMS, Manifest.permission.POST_NOTIFICATIONS};
        }
        
        for (String p : permissions) {
            if (ContextCompat.checkSelfPermission(this, p) != PackageManager.PERMISSION_GRANTED) toRequest.add(p);
        }
        if (!toRequest.isEmpty()) ActivityCompat.requestPermissions(this, toRequest.toArray(new String[0]), PERMISSION_REQUEST_CODE);
        else {
            isProtectionActive = true;
            updateUIState();
            startMonitoring();
            showTopNotification("TrustCall", "Ochrona jest teraz aktywna.");
        }
    }

    @Override
    public void onRequestPermissionsResult(int requestCode, @NonNull String[] permissions, @NonNull int[] grantResults) {
        super.onRequestPermissionsResult(requestCode, permissions, grantResults);
        if (requestCode == PERMISSION_REQUEST_CODE) {
            boolean allGranted = true;
            for (int res : grantResults) if (res != PackageManager.PERMISSION_GRANTED) allGranted = false;
            if (allGranted) {
                isProtectionActive = true;
                updateUIState();
                startMonitoring();
                showTopNotification("TrustCall", "Ochrona jest teraz aktywna.");
            } else {
                Toast.makeText(this, "Wymagane uprawnienia do ochrony!", Toast.LENGTH_SHORT).show();
            }
        }
    }

    private void startMonitoring() {
        if (!isProtectionActive || isListening) return;
        initSpeechRecognizer();
        try { speechRecognizer.startListening(speechRecognizerIntent); } catch (Exception e) { Log.e("TrustCall", "Start error", e); }
    }

    @Override
    protected void onDestroy() {
        super.onDestroy();
        try { unregisterReceiver(confirmationReceiver); } catch (Exception e) {}
        if (speechRecognizer != null) speechRecognizer.destroy();
    }
}

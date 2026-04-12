package com.example.antyspamer;

import android.app.Notification;
import android.app.NotificationChannel;
import android.app.NotificationManager;
import android.app.PendingIntent;
import android.app.Service;
import android.content.Intent;
import android.content.SharedPreferences;
import android.os.Build;
import android.os.Bundle;
import android.os.IBinder;
import android.speech.RecognitionListener;
import android.speech.RecognizerIntent;
import android.speech.SpeechRecognizer;
import android.telephony.SmsManager;
import android.util.Log;

import androidx.annotation.Nullable;
import androidx.core.app.NotificationCompat;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;

public class MonitoringService extends Service {
    private static final String CHANNEL_ID = "MonitoringServiceChannel";
    private SpeechRecognizer speechRecognizer;
    private Intent speechRecognizerIntent;
    private SharedPreferences sharedPreferences;
    private DatabaseHelper dbHelper;
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

    @Override
    public void onCreate() {
        super.onCreate();
        sharedPreferences = getSharedPreferences("SpamPrefs", MODE_PRIVATE);
        dbHelper = new DatabaseHelper(this);
        createNotificationChannel();
        setupSpeechIntent();
        initSpeechRecognizer();
    }

    @Override
    public int onStartCommand(Intent intent, int flags, int startId) {
        Intent notificationIntent = new Intent(this, MainActivity.class);
        PendingIntent pendingIntent = PendingIntent.getActivity(this,
                0, notificationIntent, PendingIntent.FLAG_IMMUTABLE);

        Notification notification = new NotificationCompat.Builder(this, CHANNEL_ID)
                .setContentTitle("TrustCall Aktywny")
                .setContentText("Monitoruję rozmowę dla Twojego bezpieczeństwa...")
                .setSmallIcon(android.R.drawable.ic_lock_idle_lock)
                .setContentIntent(pendingIntent)
                .setPriority(NotificationCompat.PRIORITY_LOW)
                .setOngoing(true)
                .build();

        startForeground(1, notification);
        startMonitoring();

        return START_STICKY;
    }

    private void startMonitoring() {
        if (speechRecognizer != null) {
            speechRecognizer.startListening(speechRecognizerIntent);
        }
    }

    private void setupSpeechIntent() {
        speechRecognizerIntent = new Intent(RecognizerIntent.ACTION_RECOGNIZE_SPEECH);
        speechRecognizerIntent.putExtra(RecognizerIntent.EXTRA_LANGUAGE_MODEL, RecognizerIntent.LANGUAGE_MODEL_FREE_FORM);
        speechRecognizerIntent.putExtra(RecognizerIntent.EXTRA_LANGUAGE, "pl-PL");
        speechRecognizerIntent.putExtra(RecognizerIntent.EXTRA_PARTIAL_RESULTS, true);
    }

    private void initSpeechRecognizer() {
        speechRecognizer = SpeechRecognizer.createSpeechRecognizer(this);
        speechRecognizer.setRecognitionListener(new RecognitionListener() {
            @Override public void onReadyForSpeech(Bundle params) {}
            @Override public void onBeginningOfSpeech() {}
            @Override public void onRmsChanged(float rmsdB) {}
            @Override public void onBufferReceived(byte[] buffer) {}
            @Override public void onEndOfSpeech() {}
            @Override public void onError(int error) {
                if (error == SpeechRecognizer.ERROR_NO_MATCH || error == SpeechRecognizer.ERROR_SPEECH_TIMEOUT) {
                    startMonitoring();
                } else {
                    Log.e("TrustCallService", "Speech error: " + error);
                    new android.os.Handler(getMainLooper()).postDelayed(() -> startMonitoring(), 1000);
                }
            }
            @Override public void onResults(Bundle results) {
                processResults(results);
                startMonitoring();
            }
            @Override public void onPartialResults(Bundle partialResults) {
                processResults(partialResults);
            }
            @Override public void onEvent(int eventType, Bundle params) {}
        });
    }

    private void processResults(Bundle bundle) {
        ArrayList<String> matches = bundle.getStringArrayList(SpeechRecognizer.RESULTS_RECOGNITION);
        if (matches != null && !matches.isEmpty()) {
            String text = matches.get(0);
            
            Intent intent = new Intent("com.example.antyspamer.SPEECH_RESULT");
            intent.putExtra("text", text);
            sendBroadcast(intent);

            checkForKeywords(text);
        }
    }

    private void checkForKeywords(String text) {
        ArrayList<String> activeKeywords = getActiveKeywords();
        String lowerText = text.toLowerCase();
        for (String keyword : activeKeywords) {
            if (lowerText.contains(keyword.toLowerCase())) {
                dbHelper.addAlert(keyword, text, "PENDING");
                sendAlertToGuardians(keyword, text);
                break;
            }
        }
    }

    private ArrayList<String> getActiveKeywords() {
        ArrayList<String> list = new ArrayList<>();
        if (sharedPreferences.getBoolean("detect_financial", true)) {
            list.addAll(Arrays.asList(financialKeywords));
        }
        if (sharedPreferences.getBoolean("detect_security", true)) {
            list.addAll(Arrays.asList(securityKeywords));
        }
        if (sharedPreferences.getBoolean("detect_urgent", true)) {
            list.addAll(Arrays.asList(urgentKeywords));
        }
        return list;
    }

    private void sendAlertToGuardians(String keyword, String fullText) {
        long currentTime = System.currentTimeMillis();
        if (currentTime - lastSmsTime < 120000) return;

        String userName = sharedPreferences.getString("user_name", "Twój bliski");
        String message = "ALERT TrustCall! " + userName + " może być oszukiwany. Wykryto: \"" + 
                         keyword + "\". Treść: \"" + fullText + "\". Odpisz POTWIERDZAM aby go ostrzec.";

        try {
            SmsManager smsManager;
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.S) {
                smsManager = this.getSystemService(SmsManager.class);
            } else {
                smsManager = SmsManager.getDefault();
            }

            List<DatabaseHelper.Guardian> guardians = dbHelper.getAllGuardians();
            boolean sent = false;
            for (DatabaseHelper.Guardian guardian : guardians) {
                String num = guardian.phone;
                if (num != null && !num.isEmpty()) {
                    smsManager.sendTextMessage(num, null, message, null, null);
                    sent = true;
                }
            }
            if (sent) lastSmsTime = currentTime;
        } catch (Exception e) {
            Log.e("TrustCallService", "Błąd wysyłania SMS", e);
        }
    }

    private void createNotificationChannel() {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            NotificationChannel serviceChannel = new NotificationChannel(
                    CHANNEL_ID,
                    "TrustCall Monitoring Service",
                    NotificationManager.IMPORTANCE_LOW
            );
            NotificationManager manager = getSystemService(NotificationManager.class);
            if (manager != null) manager.createNotificationChannel(serviceChannel);
        }
    }

    @Override
    public void onDestroy() {
        super.onDestroy();
        if (speechRecognizer != null) {
            speechRecognizer.destroy();
        }
    }

    @Nullable
    @Override
    public IBinder onBind(Intent intent) {
        return null;
    }
}

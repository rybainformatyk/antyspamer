package com.example.antyspamer;

import android.app.PendingIntent;
import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;
import android.telephony.TelephonyManager;
import android.widget.Toast;
import androidx.core.app.NotificationCompat;
import androidx.core.app.NotificationManagerCompat;
import androidx.core.content.ContextCompat;
import java.util.List;

public class CallReceiver extends BroadcastReceiver {
    private static String lastState = TelephonyManager.EXTRA_STATE_IDLE;
    private static final String CHANNEL_ID = "TrustCallAlerts";

    @Override
    public void onReceive(Context context, Intent intent) {
        if (intent == null || intent.getAction() == null) return;
        if (TelephonyManager.ACTION_PHONE_STATE_CHANGED.equals(intent.getAction())) {
            String state = intent.getStringExtra(TelephonyManager.EXTRA_STATE);
            String incomingNumber = intent.getStringExtra(TelephonyManager.EXTRA_INCOMING_NUMBER);
            
            if (state == null) return;

            if (state.equals(TelephonyManager.EXTRA_STATE_OFFHOOK)) {
                if (!lastState.equals(TelephonyManager.EXTRA_STATE_OFFHOOK)) {
                    checkAndStartMonitoring(context, incomingNumber);
                }
            } else if (state.equals(TelephonyManager.EXTRA_STATE_IDLE)) {
                context.stopService(new Intent(context, MonitoringService.class));
                
                // Anuluj powiadomienie z poradą, gdy rozmowa się zakończy
                try {
                    NotificationManagerCompat.from(context).cancel(2);
                } catch (Exception e) {
                    e.printStackTrace();
                }
            }
            lastState = state;
        }
    }

    private void checkAndStartMonitoring(Context context, String incomingNumber) {
        DatabaseHelper dbHelper = new DatabaseHelper(context);
        List<DatabaseHelper.Guardian> guardians = dbHelper.getAllGuardians();
        
        boolean isGuardian = false;
        if (incomingNumber != null) {
            String normalizedIncoming = incomingNumber.replaceAll("\\s+", "");
            for (DatabaseHelper.Guardian g : guardians) {
                if (g.phone != null && !g.phone.isEmpty()) {
                    String normalizedGuardian = g.phone.replaceAll("\\s+", "");
                    if (normalizedIncoming.contains(normalizedGuardian)) {
                        isGuardian = true;
                        break;
                    }
                }
            }
        }

        if (!isGuardian) {
            // Pokaż Toast na dole ekranu
            Toast.makeText(context, "TrustCall: Zalecamy włączyć tryb głośnomówiący!", Toast.LENGTH_LONG).show();
            
            // Pokaż wysuwane powiadomienie
            showSpeakerphoneTip(context);
            
            // Uruchom nasłuch
            ContextCompat.startForegroundService(context, new Intent(context, MonitoringService.class));
        } else {
            Toast.makeText(context, "Rozmowa z zaufanym opiekunem. TrustCall wstrzymany.", Toast.LENGTH_SHORT).show();
        }
    }

    private void showSpeakerphoneTip(Context context) {
        Intent intent = new Intent(context, MainActivity.class);
        PendingIntent pendingIntent = PendingIntent.getActivity(context, 0, intent, PendingIntent.FLAG_IMMUTABLE);

        NotificationCompat.Builder builder = new NotificationCompat.Builder(context, CHANNEL_ID)
                .setSmallIcon(android.R.drawable.ic_btn_speak_now)
                .setContentTitle("Włącz tryb głośnomówiący \uD83D\uDD0A")
                .setContentText("Aby TrustCall słyszał również rozmówcę, włącz głośnik telefonu.")
                .setPriority(NotificationCompat.PRIORITY_MAX)
                .setCategory(NotificationCompat.CATEGORY_MESSAGE)
                .setContentIntent(pendingIntent)
                .setAutoCancel(true);

        try {
            NotificationManagerCompat.from(context).notify(2, builder.build());
        } catch (SecurityException e) {
            e.printStackTrace(); // Uprawnienia powiadomień
        }
    }
}
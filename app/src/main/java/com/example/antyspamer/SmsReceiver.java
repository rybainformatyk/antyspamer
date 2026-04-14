package com.example.antyspamer;

import android.app.PendingIntent;
import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;
import android.os.Bundle;
import android.telephony.SmsMessage;
import androidx.core.app.NotificationCompat;
import androidx.core.app.NotificationManagerCompat;
import java.util.List;

public class SmsReceiver extends BroadcastReceiver {
    private static final String CHANNEL_ID = "TrustCallAlerts";

    @Override
    public void onReceive(Context context, Intent intent) {
        if ("android.provider.Telephony.SMS_RECEIVED".equals(intent.getAction())) {
            Bundle bundle = intent.getExtras();
            if (bundle != null) {
                Object[] pdus = (Object[]) bundle.get("pdus");
                if (pdus != null) {
                    DatabaseHelper dbHelper = new DatabaseHelper(context);
                    List<DatabaseHelper.Guardian> guardians = dbHelper.getAllGuardians();
                    
                    for (Object pdu : pdus) {
                        SmsMessage smsMessage;
                        if (android.os.Build.VERSION.SDK_INT >= android.os.Build.VERSION_CODES.M) {
                            String format = bundle.getString("format");
                            smsMessage = SmsMessage.createFromPdu((byte[]) pdu, format);
                        } else {
                            smsMessage = SmsMessage.createFromPdu((byte[]) pdu);
                        }
                        
                        String sender = smsMessage.getDisplayOriginatingAddress();
                        String messageBody = smsMessage.getMessageBody().toLowerCase().trim();

                        boolean isTrusted = false;
                        if (sender != null) {
                            String cleanSender = sender.replaceAll("\\s+", "");
                            for (DatabaseHelper.Guardian g : guardians) {
                                if (g.phone != null && !g.phone.isEmpty()) {
                                    String cleanGuardian = g.phone.replaceAll("\\s+", "");
                                    if (cleanSender.contains(cleanGuardian)) {
                                        isTrusted = true;
                                        break;
                                    }
                                }
                            }
                        }

                        if (isTrusted) {
                            Intent statusIntent = new Intent("com.example.antyspamer.SMS_CONFIRMATION");
                            if (messageBody.contains("potwierdzam")) {
                                statusIntent.putExtra("status", "POTWIERDZONO");
                                dbHelper.updateLastAlertStatus("CONFIRMED");
                                context.sendBroadcast(statusIntent);
                                
                                triggerFullScreenAlert(context);
                            } else if (messageBody.contains("odrzucam")) {
                                statusIntent.putExtra("status", "ODRZUCONO");
                                dbHelper.updateLastAlertStatus("REJECTED");
                                context.sendBroadcast(statusIntent);
                            }
                        }
                    }
                }
            }
        }
    }

    private void triggerFullScreenAlert(Context context) {
        Intent fullScreenIntent = new Intent(context, AlertActivity.class);
        fullScreenIntent.setFlags(Intent.FLAG_ACTIVITY_NEW_TASK | Intent.FLAG_ACTIVITY_CLEAR_TOP);

        PendingIntent fullScreenPendingIntent = PendingIntent.getActivity(
                context, 
                0, 
                fullScreenIntent, 
                PendingIntent.FLAG_UPDATE_CURRENT | PendingIntent.FLAG_IMMUTABLE
        );

        NotificationCompat.Builder builder = new NotificationCompat.Builder(context, CHANNEL_ID)
                .setSmallIcon(android.R.drawable.ic_dialog_alert)
                .setContentTitle("OSZUSTWO POTWIERDZONE!")
                .setContentText("Rozłącz się natychmiast!")
                .setPriority(NotificationCompat.PRIORITY_MAX)
                .setCategory(NotificationCompat.CATEGORY_ALARM)
                .setFullScreenIntent(fullScreenPendingIntent, true)
                .setAutoCancel(true);

        try {
            NotificationManagerCompat notificationManager = NotificationManagerCompat.from(context);
            notificationManager.notify(999, builder.build());
        } catch (SecurityException e) {
            // Ignorowane jeżeli brak uprawnień POST_NOTIFICATIONS
            e.printStackTrace();
        }
        
        // Zapasowe bezpośrednie wywołanie (na wypadek gdyby FullScreenIntent nie zadziałał np. na starszych Androidach)
        try {
            context.startActivity(fullScreenIntent);
        } catch (Exception e) {}
    }
}
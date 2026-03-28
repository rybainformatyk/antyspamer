package com.example.antyspamer;

import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;
import android.content.SharedPreferences;
import android.os.Bundle;
import android.telephony.SmsMessage;
import android.util.Log;

public class SmsReceiver extends BroadcastReceiver {
    @Override
    public void onReceive(Context context, Intent intent) {
        if ("android.provider.Telephony.SMS_RECEIVED".equals(intent.getAction())) {
            Bundle bundle = intent.getExtras();
            if (bundle != null) {
                Object[] pdus = (Object[]) bundle.get("pdus");
                if (pdus != null) {
                    SharedPreferences prefs = context.getSharedPreferences("SpamPrefs", Context.MODE_PRIVATE);
                    DatabaseHelper dbHelper = new DatabaseHelper(context);
                    
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

                        Log.d("TrustCallSms", "SMS od: " + sender + " Treść: " + messageBody);

                        boolean isTrusted = false;
                        if (sender != null) {
                            for (int i = 1; i <= 5; i++) {
                                String guardianNum = prefs.getString("num" + i, "");
                                if (!guardianNum.isEmpty() && sender.contains(guardianNum)) {
                                    isTrusted = true;
                                    break;
                                }
                            }
                        }

                        if (isTrusted) {
                            Intent statusIntent = new Intent("com.example.antyspamer.SMS_CONFIRMATION");
                            if (messageBody.contains("potwierdzam")) {
                                statusIntent.putExtra("status", "POTWIERDZONO");
                                dbHelper.updateLastAlertStatus("CONFIRMED");
                                context.sendBroadcast(statusIntent);
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
}

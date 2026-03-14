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
                    String num1 = prefs.getString("num1", "");
                    String num2 = prefs.getString("num2", "");

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

                        // Sprawdzenie czy nadawca jest na liście zaufanych
                        boolean isTrusted = (num1 != null && !num1.isEmpty() && sender.contains(num1)) ||
                                            (num2 != null && !num2.isEmpty() && sender.contains(num2));

                        if (isTrusted) {
                            Intent statusIntent = new Intent("com.example.antyspamer.SMS_CONFIRMATION");
                            if (messageBody.contains("potwierdzam")) {
                                statusIntent.putExtra("status", "POTWIERDZONO");
                                context.sendBroadcast(statusIntent);
                            } else if (messageBody.contains("odrzucam")) {
                                statusIntent.putExtra("status", "ODRZUCONO");
                                context.sendBroadcast(statusIntent);
                            }
                        }
                    }
                }
            }
        }
    }
}

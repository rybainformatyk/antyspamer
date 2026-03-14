package com.example.antyspamer;

import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;
import android.content.SharedPreferences;
import android.telephony.TelephonyManager;
import android.widget.Toast;

public class CallReceiver extends BroadcastReceiver {
    @Override
    public void onReceive(Context context, Intent intent) {
        if (TelephonyManager.ACTION_PHONE_STATE_CHANGED.equals(intent.getAction())) {
            String state = intent.getStringExtra(TelephonyManager.EXTRA_STATE);
            if (TelephonyManager.EXTRA_STATE_RINGING.equals(state)) {
                String incomingNumber = intent.getStringExtra(TelephonyManager.EXTRA_INCOMING_NUMBER);
                
                if (incomingNumber != null) {
                    SharedPreferences prefs = context.getSharedPreferences("SpamPrefs", Context.MODE_PRIVATE);
                    String num1 = prefs.getString("num1", "");
                    String num2 = prefs.getString("num2", "");

                    if ((!num1.isEmpty() && incomingNumber.contains(num1)) ||
                        (!num2.isEmpty() && incomingNumber.contains(num2))) {
                        Toast.makeText(context, "UWAGA: Spam z numeru: " + incomingNumber, Toast.LENGTH_LONG).show();
                    }
                }
            }
        }
    }
}

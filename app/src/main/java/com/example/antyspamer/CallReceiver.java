package com.example.antyspamer;

import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;
import android.telephony.TelephonyManager;
import android.util.Log;
import android.widget.Toast;
import androidx.core.content.ContextCompat;
import java.util.List;

public class CallReceiver extends BroadcastReceiver {
    private static String lastState = TelephonyManager.EXTRA_STATE_IDLE;

    @Override
    public void onReceive(Context context, Intent intent) {
        if (TelephonyManager.ACTION_PHONE_STATE_CHANGED.equals(intent.getAction())) {
            String state = intent.getStringExtra(TelephonyManager.EXTRA_STATE);
            String incomingNumber = intent.getStringExtra(TelephonyManager.EXTRA_INCOMING_NUMBER);
            
            if (state == null) return;

            if (state.equals(TelephonyManager.EXTRA_STATE_OFFHOOK)) {
                if (!lastState.equals(TelephonyManager.EXTRA_STATE_OFFHOOK)) {
                    checkAndStartMonitoring(context, incomingNumber);
                }
            } else if (state.equals(TelephonyManager.EXTRA_STATE_IDLE)) {
                Intent serviceIntent = new Intent(context, MonitoringService.class);
                context.stopService(serviceIntent);
            }
            lastState = state;
        }
    }

    private void checkAndStartMonitoring(Context context, String incomingNumber) {
        DatabaseHelper dbHelper = new DatabaseHelper(context);
        List<Guardian> guardians = dbHelper.getAllGuardians();
        
        boolean isGuardian = false;
        if (incomingNumber != null) {
            for (Guardian g : guardians) {
                if (!g.getPhone().isEmpty() && incomingNumber.contains(g.getPhone())) {
                    isGuardian = true;
                    break;
                }
            }
        }

        if (!isGuardian) {
            Log.d("TrustCall", "Uruchamiam monitoring dla: " + incomingNumber);
            Intent serviceIntent = new Intent(context, MonitoringService.class);
            ContextCompat.startForegroundService(context, serviceIntent);
        } else {
            Toast.makeText(context, "Rozmowa z zaufanym opiekunem.", Toast.LENGTH_SHORT).show();
        }
    }
}

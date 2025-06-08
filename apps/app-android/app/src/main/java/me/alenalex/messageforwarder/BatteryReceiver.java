package me.alenalex.messageforwarder;

import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;

import androidx.work.OneTimeWorkRequest;
import androidx.work.WorkManager;

import me.alenalex.messageforwarder.worker.AppWorkers;

public class BatteryReceiver extends BroadcastReceiver {
    @Override
    public void onReceive(Context context, Intent intent) {
//        if (Intent.ACTION_BATTERY_LOW.equals(intent.getAction())) {
//            // You can create a different worker or reuse the same one with different data
//            OneTimeWorkRequest batteryWorkRequest =
//                    new OneTimeWorkRequest.Builder(AppWorkers.BatteryNotificationWorker.class).build();
//            WorkManager.getInstance(context).enqueue(batteryWorkRequest);
//        }
    }
}

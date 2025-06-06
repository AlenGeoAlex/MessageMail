package me.alenalex.messageforwarder;

import static android.content.ContentValues.TAG;

import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;
import android.provider.Telephony;
import android.telephony.SmsMessage;
import android.util.Log;

import androidx.work.Data;
import androidx.work.OneTimeWorkRequest;
import androidx.work.WorkManager;

import me.alenalex.messageforwarder.worker.AppWorkers;

public class SmsReceiver extends BroadcastReceiver {
    @Override
    public void onReceive(Context context, Intent intent) {
        if (Telephony.Sms.Intents.SMS_RECEIVED_ACTION.equals(intent.getAction())) {
            StringBuilder messageBody = new StringBuilder();
            String sender = "";
            for (SmsMessage smsMessage : Telephony.Sms.Intents.getMessagesFromIntent(intent)) {
                sender = smsMessage.getDisplayOriginatingAddress();
                messageBody.append(smsMessage.getMessageBody());
            }

            Log.d(TAG, "SMS received from: " + sender + ", Message: " + messageBody.toString());

            // Pass the data to our Worker
            Data data = new Data.Builder()
                    .putString("sender", sender)
                    .putString("message", messageBody.toString())
                    .build();

            // Schedule the background work
            OneTimeWorkRequest smsWorkRequest = new OneTimeWorkRequest.Builder(AppWorkers.SmsForwarderWorker.class)
                    .setInputData(data)
                    .build();

            WorkManager.getInstance(context).enqueue(smsWorkRequest);
        }
    }
}

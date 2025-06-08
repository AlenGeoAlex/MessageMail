package me.alenalex.messageforwarder;

import static android.content.ContentValues.TAG;

import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;
import android.content.SharedPreferences;
import android.os.Build;
import android.os.Bundle;
import android.provider.Telephony;
import android.telephony.SmsMessage;
import android.util.Log;

import androidx.work.BackoffPolicy;
import androidx.work.Data;
import androidx.work.OneTimeWorkRequest;
import androidx.work.WorkManager;

import java.time.Duration;
import java.time.temporal.TemporalUnit;
import java.util.concurrent.TimeUnit;

import me.alenalex.messageforwarder.constants.SharedPrefConstants;
import me.alenalex.messageforwarder.worker.AppWorkers;

public class SmsReceiver extends BroadcastReceiver {
    // For Android 5.1 (API 22) and above, the subId is in the intent extras
    private static final String SUBSCRIPTION_KEY = "subscription"; // This is the commonly used extra key
    // For some older versions or specific vendor implementations, it might be different, e.g., "subId"
    // You might need to check for multiple known keys if one doesn't work.

    @Override
    public void onReceive(Context context, Intent intent) {
        Log.d(TAG, "SMS Received Intent: " + intent.getAction());

        SharedPreferences sharedPreferences = context.getSharedPreferences(
                SharedPrefConstants.SHARED_PREF,
                Context.MODE_PRIVATE
        );

        boolean isForwardingGloballyEnabled = sharedPreferences.getBoolean(SharedPrefConstants.PREF_FORWARDING_ENABLED, true);
        if (!isForwardingGloballyEnabled) {
            Log.d(TAG, "SMS forwarding is globally disabled. Skipping.");
            return;
        }

        if (Telephony.Sms.Intents.SMS_RECEIVED_ACTION.equals(intent.getAction())) {
            Bundle bundle = intent.getExtras();
            if (bundle != null) {
                Object[] pdus = (Object[]) bundle.get("pdus");
                if (pdus == null || pdus.length == 0) {
                    Log.e(TAG, "PDUs are null or empty");
                    return;
                }

                SmsMessage[] messages = new SmsMessage[pdus.length];
                StringBuilder messageBody = new StringBuilder();
                String sender = null;
                int messageSubId = -1; // Default to -1 (unknown or any)
                if (bundle.containsKey(SUBSCRIPTION_KEY)) {
                    messageSubId = bundle.getInt(SUBSCRIPTION_KEY, -1);
                    Log.d(TAG, "Subscription ID from intent extra ('" + SUBSCRIPTION_KEY + "'): " + messageSubId);
                } else if (bundle.containsKey("subId")) { // Fallback for some devices
                    messageSubId = bundle.getInt("subId", -1);
                    Log.d(TAG, "Subscription ID from intent extra ('subId'): " + messageSubId);
                } else if (bundle.containsKey("slot")) { // Another fallback
                    messageSubId = bundle.getInt("slot", -1); // Slot ID might sometimes be usable as subId or to derive it
                    Log.d(TAG, "Subscription ID from intent extra ('slot'): " + messageSubId);
                }


                for (int i = 0; i < pdus.length; i++) {
                    String format = bundle.getString("format");
                    messages[i] = SmsMessage.createFromPdu((byte[]) pdus[i], format);

                    if (sender == null) {
                        sender = messages[i].getDisplayOriginatingAddress();
                    }
                    messageBody.append(messages[i].getMessageBody());
                }

                if (sender == null) {
                    Log.e(TAG, "Could not extract sender from SMS.");
                    return;
                }

                boolean shouldForwardBasedOnSim = false;
                int preferredSubId = sharedPreferences.getInt(SharedPrefConstants.PREF_SELECTED_SUB_ID, -1);
                Log.d(TAG, "Message Sub ID (from intent): " + messageSubId + ", Preferred Sub ID: " + preferredSubId);

                if (messageSubId == -1 && preferredSubId != -1) {
                    Log.w(TAG, "Could not determine SIM for incoming message, but user has a preferred SIM. Skipping (conservative).");
                } else if (preferredSubId == -1 || messageSubId == preferredSubId /* if messageSubId couldn't be found, forward if 'any' is selected */) {
                    shouldForwardBasedOnSim = true;
                } else {
                    Log.d(TAG, "SMS is not from the preferred SIM (" + preferredSubId + "). Current SIM: " + messageSubId + ". Skipping.");
                }

                if (shouldForwardBasedOnSim) {
                    Log.d(TAG, "Forwarding SMS from: " + sender + ", Message: " + messageBody.toString() + ", Sub ID (if available): " + messageSubId);
                    Data inputData = new Data.Builder()
                            .putString("sender", sender)
                            .putString("message", messageBody.toString())
                            .build();

                    OneTimeWorkRequest smsWorkRequest = new OneTimeWorkRequest.Builder(AppWorkers.SmsForwarderWorker.class)
                            .setInputData(inputData)
                            .setBackoffCriteria(BackoffPolicy.EXPONENTIAL, 1, TimeUnit.MINUTES)
                            .build();

                    WorkManager.getInstance(context).enqueue(smsWorkRequest);
                } else {
                    Log.d(TAG, "SMS from " + sender + " not forwarded due to SIM preference or missing message SIM info.");
                }
            } else {
                Log.e(TAG, "SMS intent bundle is null.");
            }
        }
    }
}


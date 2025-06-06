package me.alenalex.messageforwarder.worker;

import android.content.Context;
import android.content.SharedPreferences;

import androidx.annotation.NonNull;
import androidx.work.Worker;
import androidx.work.WorkerParameters;

import me.alenalex.messageforwarder.constants.SharedPrefConstants;
import okhttp3.MediaType;
import okhttp3.OkHttpClient;
import okhttp3.Request;
import okhttp3.RequestBody;
import okhttp3.Response;

public class AppWorkers {

    public static class SmsForwarderWorker extends Worker {

        public SmsForwarderWorker(@NonNull Context context, @NonNull WorkerParameters workerParams) {
            super(context, workerParams);
        }

        @NonNull
        @Override
        public Result doWork() {
            // Get data passed from the receiver
            String sender = getInputData().getString("sender");
            String message = getInputData().getString("message");

            // Get saved API URL from SharedPreferences
            SharedPreferences sharedPreferences = getApplicationContext().getSharedPreferences(SharedPrefConstants.SHARED_PREF, Context.MODE_PRIVATE);
            String apiUrl = sharedPreferences.getString(SharedPrefConstants.API_URL, null);
            String secretKet = sharedPreferences.getString(SharedPrefConstants.SECRET_KEY, null);

            if (apiUrl == null || apiUrl.isEmpty()) {
                return Result.failure(); // Can't work without an API URL
            }

            try {
                OkHttpClient client = new OkHttpClient();
                // Create a JSON body. Adjust this to match what your Cloudflare worker expects.
                String json = "{\"from\":\"" + sender + "\", \"text\":\"" + message + "\"}";
                RequestBody body = RequestBody.create(json, MediaType.get("application/json; charset=utf-8"));

                Request request = new Request.Builder()
                        .url(apiUrl)
                        .post(body)
                        .build();

                try (Response response = client.newCall(request).execute()) {
                    if (response.isSuccessful()) {
                        return Result.success();
                    } else {
                        return Result.retry(); // Something went wrong, maybe retry later
                    }
                }
            } catch (Exception e) {
                e.printStackTrace();
                return Result.failure();
            }
        }
    }

    public static class BatteryNotificationWorker extends Worker {

        public BatteryNotificationWorker(@NonNull Context context, @NonNull WorkerParameters workerParams) {
            super(context, workerParams);
        }

        @NonNull
        @Override
        public Result doWork() {
            return null;
        }
    }

}

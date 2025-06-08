package me.alenalex.messageforwarder.worker;

import static android.content.ContentValues.TAG;

import static me.alenalex.messageforwarder.MainActivity.CHANNEL_ID;
import static me.alenalex.messageforwarder.MainActivity.NOTIFICATION_ID_CONFIG_ERROR;
import static me.alenalex.messageforwarder.MainActivity.NOTIFICATION_ID_FORWARD_FAILURE;
import static me.alenalex.messageforwarder.MainActivity.NOTIFICATION_ID_FORWARD_SUCCESS;

import android.app.PendingIntent;
import android.content.Context;
import android.content.Intent;
import android.content.SharedPreferences;
import android.util.Log;
import android.webkit.WebSettings;

import androidx.annotation.NonNull;
import androidx.core.app.NotificationCompat;
import androidx.core.app.NotificationManagerCompat;
import androidx.work.Worker;
import androidx.work.WorkerParameters;

import java.text.SimpleDateFormat;
import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.util.Date;
import java.util.Locale;
import java.util.TimeZone;

import me.alenalex.messageforwarder.MainActivity;
import me.alenalex.messageforwarder.R;
import me.alenalex.messageforwarder.constants.SharedPrefConstants;
import okhttp3.HttpUrl;
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
            Context context = getApplicationContext();
            String sender = getInputData().getString("sender");
            String message = getInputData().getString("message");

            // Get saved API URL from SharedPreferences
            SharedPreferences sharedPreferences = getApplicationContext().getSharedPreferences(SharedPrefConstants.SHARED_PREF, Context.MODE_PRIVATE);
            String apiUrl = sharedPreferences.getString(SharedPrefConstants.API_URL, null);
            String secretKey = sharedPreferences.getString(SharedPrefConstants.SECRET_KEY, null);

            if(secretKey == null || secretKey.isEmpty()){
                sendConfigurationErrorNotification(context, "Setup Required", "Secret key is missing. Please configure the app.");
                return Result.failure();
            }

            if (apiUrl == null || apiUrl.isEmpty()) {
                sendConfigurationErrorNotification(context, "Setup Required", "API URL is missing. Please configure the app.");
                return Result.failure(); // Can't work without an API URL
            }

            HttpUrl url = HttpUrl.parse(apiUrl + "/notifications/message");
            if(url == null){
                Log.e(TAG, "Configuration Error: API URL is invalid and could not be parsed: " + apiUrl);
                return Result.failure();
            }

            try {
                OkHttpClient client = new OkHttpClient();
                // Create a JSON body. Adjust this to match what your Cloudflare worker expects.
                SimpleDateFormat sdf = new SimpleDateFormat("yyyy-MM-dd'T'HH:mm:ss", Locale.US);
                sdf.setTimeZone(TimeZone.getTimeZone("UTC"));
                String timestamp = sdf.format(new Date());

                String safeSender = sender != null ? sender : "Unknown";
                String safeMessage = message != null ? message : "";

                String json = "{\"from\":\"" + sender + "\", \"message\":\"" + message + "\", \"on\":\"" + timestamp + "\"}";
                RequestBody body = RequestBody.create(json, MediaType.get("application/json; charset=utf-8"));

                Request request = new Request.Builder()
                        .header("x-secret-key", secretKey)
                        .header("User-Agent", WebSettings.getDefaultUserAgent(context))
                        .url(url)
                        .post(body)
                        .build();

                try (Response response = client.newCall(request).execute()) {
                    if (response.isSuccessful()) {
                        sendApiCallNotification(context,"Forwarding Success", "SMS from " + safeSender + " forwarded.", NOTIFICATION_ID_FORWARD_SUCCESS, true);
                        return Result.success();
                    } else {
                        sendApiCallNotification(context,"Forwarding Failed", "Server error: " + response.code() + ". Tap to check settings.", NOTIFICATION_ID_FORWARD_FAILURE, false);
                        return Result.retry(); // Something went wrong, maybe retry later
                    }
                }
            } catch (Exception e) {
                e.printStackTrace();
                sendApiCallNotification(context,"Forwarding Error", "Network or app error occurred. Tap to check settings.", NOTIFICATION_ID_FORWARD_FAILURE, false);
                return Result.failure();
            }
        }

        private void sendApiCallNotification(Context context, String title, String content, int notificationId, boolean isSuccess) {
            Intent notificationIntent = new Intent(context, MainActivity.class);
            notificationIntent.setFlags(Intent.FLAG_ACTIVITY_NEW_TASK | Intent.FLAG_ACTIVITY_CLEAR_TASK | Intent.FLAG_ACTIVITY_SINGLE_TOP);
            PendingIntent pendingIntent = PendingIntent.getActivity(
                    context,
                    1, // Different request code for this type of notification
                    notificationIntent,
                    PendingIntent.FLAG_UPDATE_CURRENT | PendingIntent.FLAG_IMMUTABLE
            );

            NotificationCompat.Builder builder = new NotificationCompat.Builder(context, CHANNEL_ID)
                    // Use a different icon for success vs failure if desired
                    .setSmallIcon(isSuccess ? R.drawable.ic_notification_success : R.drawable.ic_notification_error) // CREATE THESE
                    .setContentTitle(title)
                    .setContentText(content)
                    .setStyle(new NotificationCompat.BigTextStyle().bigText(content))
                    .setPriority(NotificationCompat.PRIORITY_DEFAULT)
                    .setContentIntent(pendingIntent)
                    .setAutoCancel(true);

            NotificationManagerCompat notificationManager = NotificationManagerCompat.from(context);
            try {
                notificationManager.notify(notificationId, builder.build());
                Log.d(TAG, "API call status notification sent. ID: " + notificationId);
            } catch (SecurityException se) {
                Log.e(TAG, "Failed to send API call status notification due to SecurityException. ID: " + notificationId, se);
            }
        }

        private void sendConfigurationErrorNotification(Context context, String title, String content) {
            // Intent to open MainActivity when notification is tapped
            Intent notificationIntent = new Intent(context, MainActivity.class); // Ensure MainActivity is your settings screen
            notificationIntent.setFlags(Intent.FLAG_ACTIVITY_NEW_TASK | Intent.FLAG_ACTIVITY_CLEAR_TASK | Intent.FLAG_ACTIVITY_SINGLE_TOP);
            PendingIntent pendingIntent = PendingIntent.getActivity(
                    context,
                    0, // Request code
                    notificationIntent,
                    PendingIntent.FLAG_UPDATE_CURRENT | PendingIntent.FLAG_IMMUTABLE
            );

            NotificationCompat.Builder builder = new NotificationCompat.Builder(context, CHANNEL_ID)
                    .setSmallIcon(R.drawable.ic_notification_error) // CREATE THIS ICON
                    .setContentTitle(title)
                    .setContentText(content)
                    .setStyle(new NotificationCompat.BigTextStyle().bigText(content)) // For longer text
                    .setPriority(NotificationCompat.PRIORITY_HIGH) // Important for errors
                    .setContentIntent(pendingIntent) // Action on tap
                    .setAutoCancel(true); // Dismiss notification on tap

            NotificationManagerCompat notificationManager = NotificationManagerCompat.from(context);
            // Permission check for Android 13+ is recommended before calling notify.
            // If permission is not granted, this call will silently fail on API 33+.
            // It's best to request permission from the Activity before enabling the feature.
            try {
                notificationManager.notify(NOTIFICATION_ID_CONFIG_ERROR, builder.build());
                Log.d(TAG, "Configuration error notification sent.");
            } catch (SecurityException se) {
                Log.e(TAG, "Failed to send configuration error notification due to SecurityException. " +
                        "Ensure POST_NOTIFICATIONS permission is granted on Android 13+.", se);
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

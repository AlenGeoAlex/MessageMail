package me.alenalex.messageforwarder.worker;

import static android.content.ContentValues.TAG;


import android.content.Context;
import android.content.SharedPreferences;
import android.util.Log;
import android.webkit.WebSettings;

import androidx.annotation.NonNull;
import androidx.work.Worker;
import androidx.work.WorkerParameters;

import java.text.SimpleDateFormat;
import java.util.Date;
import java.util.Locale;
import java.util.TimeZone;

import me.alenalex.messageforwarder.AppContainer;
import me.alenalex.messageforwarder.constants.SharedPrefConstants;
import me.alenalex.messageforwarder.dto.message.MessageNotificationCommand;
import me.alenalex.messageforwarder.dto.message.MessageNotificationResponse;
import me.alenalex.messageforwarder.services.NotificationService;
import me.alenalex.messageforwarder.services.api.WebRequestOptions;
import okhttp3.HttpUrl;
import okhttp3.MediaType;
import okhttp3.OkHttpClient;
import okhttp3.Request;
import okhttp3.RequestBody;
import okhttp3.Response;
import okhttp3.ResponseBody;

public class AppWorkers {

    public static class SmsForwarderWorker extends Worker {

        public SmsForwarderWorker(@NonNull Context context, @NonNull WorkerParameters workerParams) {
            super(context, workerParams);
        }

        @NonNull
        @Override
        public Result doWork() {
            int runAttemptCount = getRunAttemptCount();

            if(runAttemptCount > 5)
                return Result.success();

            Context context = getApplicationContext();
            String sender = getInputData().getString("sender");
            String message = getInputData().getString("message");

            SharedPreferences sharedPreferences = getApplicationContext().getSharedPreferences(SharedPrefConstants.SHARED_PREF, Context.MODE_PRIVATE);
            String apiUrl = sharedPreferences.getString(SharedPrefConstants.API_URL, null);
            String secretKey = sharedPreferences.getString(SharedPrefConstants.SECRET_KEY, null);

            if(secretKey == null || secretKey.isEmpty()){
                NotificationService.error(context, "Setup Required", "Secret key is missing. Please configure the app.", NotificationService.NotificationId.NOTIFICATION_ID_CONFIG_ERROR);
                return Result.failure();
            }

            if (apiUrl == null || apiUrl.isEmpty()) {
                NotificationService.error(context, "Setup Required", "API URL is missing. Please configure the app.", NotificationService.NotificationId.NOTIFICATION_ID_CONFIG_ERROR);
                return Result.failure();
            }

            HttpUrl url = HttpUrl.parse(apiUrl + "/notifications/message");
            if(url == null){
                Log.e(TAG, "Configuration Error: API URL is invalid and could not be parsed: " + apiUrl);
                NotificationService.error(context, "Setup Required", "API URL is invalid. Please configure the app", NotificationService.NotificationId.NOTIFICATION_ID_CONFIG_ERROR);
                return Result.failure();
            }

            try {
                String safeSender = sender != null ? sender : "Unknown";
                String safeMessage = message != null ? message : "";
                safeMessage = safeMessage
                        .replace("\"", "\\\"")
                        .replace("'", "\\'");

                MessageNotificationCommand command = new MessageNotificationCommand()
                        .setFrom(safeSender)
                        .setMessage(safeMessage)
                        .setOnNow();

                MessageNotificationResponse apiResponse = AppContainer.container().apiService().message(new WebRequestOptions(context), command);

                if(apiResponse.success())
                {
                    NotificationService.success(context, "Forwarding Success", "SMS from " + safeSender + " forwarded.", NotificationService.NotificationId.NOTIFICATION_ID_FORWARD_SUCCESS);
                    return Result.success();
                }

                if(apiResponse.shouldRetry()){
                    NotificationService.error(context, "Forwarding Failed for "+runAttemptCount+" attempts", "The server has responded with error. Please check the logs for more", NotificationService.NotificationId.NOTIFICATION_ID_CONFIG_ERROR);
                    return Result.retry();
                }else{
                    NotificationService.error(context, "Forwarding Failed", "The server has responded with error. Please check the logs for more", NotificationService.NotificationId.NOTIFICATION_ID_FORWARD_FAILURE);
                    return Result.failure();
                }
            } catch (Exception e) {
                Log.e(TAG, "Failed to send API call: " + e.getMessage(), e);
                NotificationService.error(context,"Forwarding Error", "Network or app error occurred. Tap to check the logs.", NotificationService.NotificationId.NOTIFICATION_ID_FORWARD_FAILURE);
                return Result.failure();
            }
        }
    }
}

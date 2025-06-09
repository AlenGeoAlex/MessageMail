package me.alenalex.messageforwarder.services;

import static android.content.ContentValues.TAG;

import android.app.PendingIntent;
import android.content.Context;
import android.content.Intent;
import android.util.Log;

import androidx.core.app.NotificationCompat;
import androidx.core.app.NotificationManagerCompat;

import me.alenalex.messageforwarder.MainActivity;
import me.alenalex.messageforwarder.R;

public class NotificationService {

    private static final String CHANNEL_ID = "SMS_FORWARDER_CHANNEL_ID";

    public static String channelId(){
        return CHANNEL_ID;
    }

    public static class NotificationId {
        public static final int NOTIFICATION_ID_CONFIG_ERROR = 101;

        public static final int NOTIFICATION_ID_FORWARD_SUCCESS = 102;

        public static final int NOTIFICATION_ID_FORWARD_FAILURE = 103;
    }


    public static void error(Context context, String title, String content, int notificationId) {
        Intent notificationIntent = new Intent(context, MainActivity.class);
        notificationIntent.setFlags(Intent.FLAG_ACTIVITY_NEW_TASK | Intent.FLAG_ACTIVITY_CLEAR_TASK | Intent.FLAG_ACTIVITY_SINGLE_TOP);
        PendingIntent pendingIntent = PendingIntent.getActivity(
                context,
                0,
                notificationIntent,
                PendingIntent.FLAG_UPDATE_CURRENT | PendingIntent.FLAG_IMMUTABLE
        );

        NotificationCompat.Builder builder = new NotificationCompat.Builder(context, CHANNEL_ID)
                .setSmallIcon(R.drawable.ic_notification_error)
                .setContentTitle(title)
                .setContentText(content)
                .setStyle(new NotificationCompat.BigTextStyle().bigText(content))
                .setPriority(NotificationCompat.PRIORITY_HIGH)
                .setContentIntent(pendingIntent)
                .setAutoCancel(true);

        NotificationManagerCompat notificationManager = NotificationManagerCompat.from(context);

        try {
            notificationManager.notify(notificationId, builder.build());
        } catch (SecurityException se) {
            Log.e(TAG, "Failed to send notification due to SecurityException. " +
                    "Ensure POST_NOTIFICATIONS permission is granted on Android 13+.", se);
        }
    }

    public static void success(Context context, String title, String content, int notificationId) {
        Intent notificationIntent = new Intent(context, MainActivity.class);
        notificationIntent.setFlags(Intent.FLAG_ACTIVITY_NEW_TASK | Intent.FLAG_ACTIVITY_CLEAR_TASK | Intent.FLAG_ACTIVITY_SINGLE_TOP);
        PendingIntent pendingIntent = PendingIntent.getActivity(
                context,
                1,
                notificationIntent,
                PendingIntent.FLAG_UPDATE_CURRENT | PendingIntent.FLAG_IMMUTABLE
        );

        NotificationCompat.Builder builder = new NotificationCompat.Builder(context, CHANNEL_ID)
                .setSmallIcon(R.drawable.ic_notification_success)
                .setContentTitle(title)
                .setContentText(content)
                .setStyle(new NotificationCompat.BigTextStyle().bigText(content))
                .setPriority(NotificationCompat.PRIORITY_DEFAULT)
                .setContentIntent(pendingIntent)
                .setAutoCancel(true);

        NotificationManagerCompat notificationManager = NotificationManagerCompat.from(context);
        try {
            notificationManager.notify(notificationId, builder.build());
        } catch (SecurityException se) {
            Log.e(TAG, "Failed to send notification due to SecurityException. ID: " + notificationId, se);
        }
    }

}

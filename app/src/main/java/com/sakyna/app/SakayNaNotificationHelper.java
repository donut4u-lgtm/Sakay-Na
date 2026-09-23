
package com.sakyna.app;

import android.Manifest;
import android.app.Activity;
import android.app.NotificationChannel;
import android.app.NotificationManager;
import android.app.PendingIntent;
import android.content.Context;
import android.content.SharedPreferences;
import android.content.pm.PackageManager;
import android.os.Build;

import androidx.core.app.ActivityCompat;
import androidx.core.app.NotificationCompat;
import androidx.core.app.NotificationManagerCompat;

public final class SakayNaNotificationHelper {

    private static final String CHANNEL_ID =
            "sakayna_ride_updates";

    private static final int PERMISSION_REQUEST =
            9101;

    private static final String PREFS =
            "SakayNa";

    private static final String NOTIFICATIONS_ENABLED =
            "notifications_enabled";

    private SakayNaNotificationHelper() {
    }

    public static boolean areNotificationsEnabled(
            Context context
    ) {
        SharedPreferences preferences =
                context.getSharedPreferences(
                        PREFS,
                        Context.MODE_PRIVATE
                );

        return preferences.getBoolean(
                NOTIFICATIONS_ENABLED,
                true
        );
    }

    public static void setNotificationsEnabled(
            Context context,
            boolean enabled
    ) {
        context.getSharedPreferences(
                PREFS,
                Context.MODE_PRIVATE
        )
                .edit()
                .putBoolean(
                        NOTIFICATIONS_ENABLED,
                        enabled
                )
                .apply();
    }

    public static void requestPermission(
            Activity activity
    ) {
        if (Build.VERSION.SDK_INT >= 33
                && ActivityCompat.checkSelfPermission(
                activity,
                Manifest.permission.POST_NOTIFICATIONS
        ) != PackageManager.PERMISSION_GRANTED) {

            ActivityCompat.requestPermissions(
                    activity,
                    new String[]{
                            Manifest.permission.POST_NOTIFICATIONS
                    },
                    PERMISSION_REQUEST
            );
        }
    }

    public static void show(
            Context context,
            int notificationId,
            String title,
            String message
    ) {
        show(
                context,
                notificationId,
                title,
                message,
                null
        );
    }

    public static void show(
            Context context,
            int notificationId,
            String title,
            String message,
            PendingIntent pendingIntent
    ) {

        /*
         * App-level notification toggle.
         * Default is ON.
         */
        if (!areNotificationsEnabled(context)) {
            return;
        }

        createChannel(context);

        /*
         * Android 13+ permission.
         */
        if (Build.VERSION.SDK_INT >= 33
                && ActivityCompat.checkSelfPermission(
                context,
                Manifest.permission.POST_NOTIFICATIONS
        ) != PackageManager.PERMISSION_GRANTED) {

            return;
        }

        NotificationCompat.Builder builder =
                new NotificationCompat.Builder(
                        context,
                        CHANNEL_ID
                )
                        .setSmallIcon(
                                android.R.drawable.ic_dialog_info
                        )
                        .setContentTitle(title)
                        .setContentText(message)
                        .setStyle(
                                new NotificationCompat
                                        .BigTextStyle()
                                        .bigText(message)
                        )
                        .setPriority(
                                NotificationCompat
                                        .PRIORITY_HIGH
                        )
                        .setAutoCancel(true);

        if (pendingIntent != null) {
            builder.setContentIntent(
                    pendingIntent
            );
        }

        NotificationManagerCompat
                .from(context)
                .notify(
                        notificationId,
                        builder.build()
                );
    }

    private static void createChannel(
            Context context
    ) {

        if (Build.VERSION.SDK_INT < 26) {
            return;
        }

        NotificationChannel channel =
                new NotificationChannel(
                        CHANNEL_ID,
                        "Sakay Na Ride Updates",
                        NotificationManager
                                .IMPORTANCE_HIGH
                );

        channel.setDescription(
                "Notifications for Sakay Na ride updates."
        );

        NotificationManager manager =
                context.getSystemService(
                        NotificationManager.class
                );

        if (manager != null) {
            manager.createNotificationChannel(
                    channel
            );
        }
    }
}

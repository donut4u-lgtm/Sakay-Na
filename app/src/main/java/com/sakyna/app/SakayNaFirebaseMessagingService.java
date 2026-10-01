package com.sakyna.app;

import android.app.PendingIntent;
import android.content.Intent;
import android.os.Build;

import androidx.annotation.NonNull;

import com.google.firebase.auth.FirebaseAuth;
import com.google.firebase.firestore.FieldValue;
import com.google.firebase.firestore.FirebaseFirestore;
import com.google.firebase.firestore.SetOptions;
import com.google.firebase.messaging.FirebaseMessagingService;
import com.google.firebase.messaging.RemoteMessage;

import java.util.HashMap;
import java.util.Map;

public class SakayNaFirebaseMessagingService
        extends FirebaseMessagingService {

    @Override
    public void onNewToken(@NonNull String token) {
        super.onNewToken(token);
        saveToken(token);
    }

    private void saveToken(String token) {

        FirebaseAuth auth =
                FirebaseAuth.getInstance();

        if (auth.getCurrentUser() == null) {
            return;
        }

        String uid =
                auth.getCurrentUser().getUid();

        Map<String, Object> data =
                new HashMap<>();

        data.put(
                "fcmToken",
                token
        );

        data.put(
                "fcmTokenUpdatedAt",
                FieldValue.serverTimestamp()
        );

        FirebaseFirestore.getInstance()
                .collection("users")
                .document(uid)
                .set(
                        data,
                        SetOptions.merge()
                );
    }

    @Override
    public void onMessageReceived(
            @NonNull RemoteMessage remoteMessage
    ) {
        super.onMessageReceived(
                remoteMessage
        );

        String title =
                "Sakay Na";

        String message =
                "You have a Sakay Na update.";

        String type =
                "";

        String rideId =
                "";

        String messageId =
                "";

        String senderId =
                "";

        String senderRole =
                "";

        /*
         * -------------------------------------------------
         * READ NOTIFICATION PAYLOAD
         * -------------------------------------------------
         */

        if (remoteMessage.getNotification() != null) {

            if (remoteMessage
                    .getNotification()
                    .getTitle() != null) {

                title =
                        remoteMessage
                                .getNotification()
                                .getTitle();
            }

            if (remoteMessage
                    .getNotification()
                    .getBody() != null) {

                message =
                        remoteMessage
                                .getNotification()
                                .getBody();
            }
        }

        Map<String, String> data =
                remoteMessage.getData();

        if (data != null) {

            if (data.containsKey("title")) {

                title =
                        data.get("title");
            }

            if (data.containsKey("message")) {

                message =
                        data.get("message");
            }

            if (data.containsKey("type")) {

                type =
                        data.get("type");
            }

            if (data.containsKey("rideId")) {

                rideId =
                        data.get("rideId");
            }

            if (data.containsKey("messageId")) {

                messageId =
                        data.get("messageId");
            }

            if (data.containsKey("senderId")) {

                senderId =
                        data.get("senderId");
            }

            if (data.containsKey("senderRole")) {

                senderRole =
                        data.get("senderRole");
            }
        }

        /*
         * -------------------------------------------------
         * CHOOSE DESTINATION
         * -------------------------------------------------
         */

        Intent intent;

        /*
         * RIDE CHAT
         *
         * Passenger -> Driver
         * Driver -> Passenger
         *
         * Open the exact ride chat.
         */

        if ("RIDE_CHAT_MESSAGE"
                .equalsIgnoreCase(type)) {

            intent =
                    new Intent(
                            this,
                            RideChatActivity.class
                    );

            if (rideId != null
                    && !rideId.isEmpty()) {

                intent.putExtra(
                        "ride_id",
                        rideId
                );

                intent.putExtra(
                        "rideId",
                        rideId
                );
            }

            if (messageId != null
                    && !messageId.isEmpty()) {

                intent.putExtra(
                        "messageId",
                        messageId
                );
            }

            if (senderId != null
                    && !senderId.isEmpty()) {

                intent.putExtra(
                        "senderId",
                        senderId
                );
            }

            if (senderRole != null
                    && !senderRole.isEmpty()) {

                intent.putExtra(
                        "senderRole",
                        senderRole
                );
            }

            intent.putExtra(
                    "notification_type",
                    "RIDE_CHAT_MESSAGE"
            );

        }

        /*
         * ADMIN DRIVER APPLICATION
         */

        else if ("ADMIN_DRIVER_APPLICATION"
                .equalsIgnoreCase(type)) {

            intent =
                    new Intent(
                            this,
                            AdminActivity.class
                    );

            intent.putExtra(
                    "notification_type",
                    "ADMIN_DRIVER_APPLICATION"
            );

            if (data != null
                    && data.containsKey("driverId")) {

                intent.putExtra(
                        "driverId",
                        data.get("driverId")
                );
            }
        }

        /*
         * DRIVER NOTIFICATIONS
         */

        else if (
                type != null
                        &&
                type.toUpperCase()
                        .contains("DRIVER")
        ) {

            intent =
                    new Intent(
                            this,
                            DriverActivity.class
                    );

        }

        /*
         * PASSENGER NOTIFICATIONS
         */

        else {

            intent =
                    new Intent(
                            this,
                            PassengerActivity.class
                    );
        }

        /*
         * -------------------------------------------------
         * COMMON RIDE INFORMATION
         * -------------------------------------------------
         */

        if (rideId != null
                && !rideId.isEmpty()) {

            intent.putExtra(
                    "ride_id",
                    rideId
            );

            intent.putExtra(
                    "rideId",
                    rideId
            );
        }

        /*
         * -------------------------------------------------
         * ACTIVITY FLAGS
         * -------------------------------------------------
         *
         * CLEAR_TOP:
         * Reuses an existing activity where appropriate.
         *
         * SINGLE_TOP:
         * Prevents unnecessary duplicate activity
         * instances.
         *
         * NEW_TASK:
         * Required because this service is not an Activity.
         */

        intent.addFlags(
                Intent.FLAG_ACTIVITY_NEW_TASK
                        |
                Intent.FLAG_ACTIVITY_CLEAR_TOP
                        |
                Intent.FLAG_ACTIVITY_SINGLE_TOP
        );

        /*
         * -------------------------------------------------
         * UNIQUE NOTIFICATION ID
         * -------------------------------------------------
         *
         * Include messageId so separate chat messages
         * don't intentionally replace each other.
         */

        int notificationId =
                createNotificationId(
                        rideId,
                        type,
                        messageId
                );

        int flags =
                PendingIntent.FLAG_UPDATE_CURRENT;

        if (Build.VERSION.SDK_INT
                >= Build.VERSION_CODES.M) {

            flags |=
                    PendingIntent.FLAG_IMMUTABLE;
        }

        PendingIntent pendingIntent =
                PendingIntent.getActivity(
                        this,
                        notificationId,
                        intent,
                        flags
                );

        /*
         * -------------------------------------------------
         * SHOW NOTIFICATION
         * -------------------------------------------------
         */

        SakayNaNotificationHelper.show(
                this,
                notificationId,
                title,
                message,
                pendingIntent
        );
    }

    private int createNotificationId(
            String rideId,
            String type,
            String messageId
    ) {

        String value =
                String.valueOf(rideId)
                        + ":"
                        + String.valueOf(type)
                        + ":"
                        + String.valueOf(messageId);

        int hash =
                value.hashCode();

        /*
         * Math.abs(Integer.MIN_VALUE) remains negative.
         * Handle that one edge case safely.
         */

        if (hash == Integer.MIN_VALUE) {
            return 1;
        }

        return Math.abs(hash);
    }
}

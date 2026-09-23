package com.sakyna.app;

import android.app.PendingIntent;
import android.content.Intent;
import android.os.Build;

import androidx.annotation.NonNull;

import com.google.firebase.auth.FirebaseAuth;
import com.google.firebase.firestore.FieldValue;
import com.google.firebase.firestore.FirebaseFirestore;
import com.google.firebase.messaging.FirebaseMessagingService;
import com.google.firebase.messaging.RemoteMessage;

import java.util.HashMap;
import java.util.Map;

public class SakayNaFirebaseMessagingService extends FirebaseMessagingService {

    @Override
    public void onNewToken(@NonNull String token) {
        super.onNewToken(token);

        FirebaseAuth auth = FirebaseAuth.getInstance();

        if (auth.getCurrentUser() == null) {
            return;
        }

        String uid = auth.getCurrentUser().getUid();

        Map<String, Object> data = new HashMap<>();
        data.put("fcmToken", token);
        data.put("fcmTokenUpdatedAt", FieldValue.serverTimestamp());

        FirebaseFirestore.getInstance()
                .collection("users")
                .document(uid)
                .set(data, com.google.firebase.firestore.SetOptions.merge());
    }

    @Override
    public void onMessageReceived(@NonNull RemoteMessage remoteMessage) {
        super.onMessageReceived(remoteMessage);

        String title = "Sakay Na";
        String message = "You have a ride update.";
        String type = "";
        String rideId = "";

        if (remoteMessage.getNotification() != null) {
            if (remoteMessage.getNotification().getTitle() != null) {
                title = remoteMessage.getNotification().getTitle();
            }

            if (remoteMessage.getNotification().getBody() != null) {
                message = remoteMessage.getNotification().getBody();
            }
        }

        if (remoteMessage.getData() != null) {
            if (remoteMessage.getData().containsKey("title")) {
                title = remoteMessage.getData().get("title");
            }

            if (remoteMessage.getData().containsKey("message")) {
                message = remoteMessage.getData().get("message");
            }

            if (remoteMessage.getData().containsKey("type")) {
                type = remoteMessage.getData().get("type");
            }

            if (remoteMessage.getData().containsKey("rideId")) {
                rideId = remoteMessage.getData().get("rideId");
            }
        }

        Intent intent;

        if (type.contains("DRIVER")) {
            intent = new Intent(this, DriverActivity.class);
        } else {
            intent = new Intent(this, PassengerActivity.class);
        }

        if (!rideId.isEmpty()) {
            intent.putExtra("ride_id", rideId);
            intent.putExtra("rideId", rideId);
        }

        intent.addFlags(
                Intent.FLAG_ACTIVITY_NEW_TASK
                        | Intent.FLAG_ACTIVITY_CLEAR_TOP
                        | Intent.FLAG_ACTIVITY_SINGLE_TOP
        );

        int flags = PendingIntent.FLAG_UPDATE_CURRENT;

        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.M) {
            flags |= PendingIntent.FLAG_IMMUTABLE;
        }

        PendingIntent pendingIntent =
                PendingIntent.getActivity(
                        this,
                        createNotificationId(rideId, type),
                        intent,
                        flags
                );

        SakayNaNotificationHelper.show(
                this,
                createNotificationId(rideId, type),
                title,
                message,
                pendingIntent
        );
    }

    private int createNotificationId(String rideId, String type) {
        String value = rideId + ":" + type;
        return Math.abs(value.hashCode());
    }
}

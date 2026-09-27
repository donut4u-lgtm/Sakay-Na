package com.sakyna.app;

import android.app.Notification;
import android.app.NotificationChannel;
import android.app.NotificationManager;
import android.app.PendingIntent;
import android.app.Service;
import android.content.Intent;
import android.content.pm.ServiceInfo;
import android.os.Build;
import android.os.IBinder;

import androidx.annotation.Nullable;
import androidx.core.app.NotificationCompat;

import com.google.firebase.Timestamp;
import com.google.firebase.auth.FirebaseAuth;
import com.google.firebase.auth.FirebaseUser;
import com.google.firebase.firestore.DocumentChange;
import com.google.firebase.firestore.DocumentSnapshot;
import com.google.firebase.firestore.FirebaseFirestore;
import com.google.firebase.firestore.ListenerRegistration;
import com.google.firebase.firestore.Query;
import com.google.firebase.firestore.QuerySnapshot;

import java.util.HashMap;
import java.util.HashSet;
import java.util.Map;
import java.util.Set;

public class SakayNaChatNotificationService extends Service {

    private static final String SERVICE_CHANNEL =
            "sakayna_chat_monitor";

    private static final int SERVICE_NOTIFICATION_ID =
            9200;

    private FirebaseFirestore db;
    private FirebaseAuth auth;

    private ListenerRegistration passengerRideListener;
    private ListenerRegistration driverRideListener;

    private final Map<String, ListenerRegistration>
            messageListeners = new HashMap<>();

    private final Set<String> knownRideIds =
            new HashSet<>();

    private final Set<String> initializedRides =
            new HashSet<>();

    @Override
    public void onCreate() {
        super.onCreate();

        db = FirebaseFirestore.getInstance();
        auth = FirebaseAuth.getInstance();

        createServiceChannel();
        startAsForegroundService();

        startRideMonitoring();
    }

    private void createServiceChannel() {

        if (Build.VERSION.SDK_INT < 26) {
            return;
        }

        NotificationChannel channel =
                new NotificationChannel(
                        SERVICE_CHANNEL,
                        "Sakay Na Chat Monitoring",
                        NotificationManager.IMPORTANCE_LOW
                );

        channel.setDescription(
                "Keeps Sakay Na chat notifications available during an active session."
        );

        channel.setShowBadge(false);

        NotificationManager manager =
                getSystemService(
                        NotificationManager.class
                );

        if (manager != null) {
            manager.createNotificationChannel(channel);
        }
    }

    private void startAsForegroundService() {

        Notification notification =
                new NotificationCompat.Builder(
                        this,
                        SERVICE_CHANNEL
                )
                        .setSmallIcon(
                                android.R.drawable.ic_dialog_info
                        )
                        .setContentTitle(
                                "Sakay Na"
                        )
                        .setContentText(
                                "Chat notifications are active"
                        )
                        .setOngoing(true)
                        .setPriority(
                                NotificationCompat.PRIORITY_LOW
                        )
                        .build();

        if (Build.VERSION.SDK_INT >= 34) {

            startForeground(
                    SERVICE_NOTIFICATION_ID,
                    notification,
                    ServiceInfo
                            .FOREGROUND_SERVICE_TYPE_REMOTE_MESSAGING
            );

        } else {

            startForeground(
                    SERVICE_NOTIFICATION_ID,
                    notification
            );
        }
    }

    private void startRideMonitoring() {

        FirebaseUser user =
                auth.getCurrentUser();

        if (user == null) {
            stopSelf();
            return;
        }

        String uid =
                user.getUid();

        /*
         * PASSENGER RIDES
         */
        passengerRideListener =
                db.collection("rides")
                        .whereEqualTo(
                                "passengerId",
                                uid
                        )
                        .addSnapshotListener(
                                (snapshot, error) -> {

                                    if (error != null
                                            || snapshot == null) {
                                        return;
                                    }

                                    processRideSnapshot(
                                            snapshot
                                    );
                                }
                        );

        /*
         * DRIVER RIDES
         */
        driverRideListener =
                db.collection("rides")
                        .whereEqualTo(
                                "driverId",
                                uid
                        )
                        .addSnapshotListener(
                                (snapshot, error) -> {

                                    if (error != null
                                            || snapshot == null) {
                                        return;
                                    }

                                    processRideSnapshot(
                                            snapshot
                                    );
                                }
                        );
    }

    private void processRideSnapshot(
            QuerySnapshot snapshot
    ) {

        for (DocumentSnapshot ride
                : snapshot.getDocuments()) {

            String rideId =
                    ride.getId();

            String status =
                    getText(
                            ride,
                            "status"
                    );

            if (!isChatRideStatus(status)) {

                removeRideListener(
                        rideId
                );

                continue;
            }

            String passengerId =
                    getText(
                            ride,
                            "passengerId"
                    );

            String driverId =
                    getText(
                            ride,
                            "driverId"
                    );

            if (passengerId.isEmpty()
                    || driverId.isEmpty()) {

                continue;
            }

            knownRideIds.add(
                    rideId
            );

            attachMessageListener(
                    rideId
            );
        }
    }

    private boolean isChatRideStatus(
            String status
    ) {

        if (status == null) {
            return false;
        }

        String value =
                status.trim()
                        .toUpperCase();

        return value.equals("ACCEPTED")
                || value.equals("DRIVER_ON_THE_WAY")
                || value.equals("DRIVER_ARRIVED")
                || value.equals("ARRIVED")
                || value.equals("IN_PROGRESS")
                || value.equals("ONGOING");
    }

    private void attachMessageListener(
            String rideId
    ) {

        if (rideId == null
                || rideId.trim().isEmpty()) {
            return;
        }

        if (messageListeners.containsKey(
                rideId
        )) {
            return;
        }

        ListenerRegistration registration =
                db.collection("rides")
                        .document(rideId)
                        .collection("messages")
                        .addSnapshotListener(
                                (snapshot, error) -> {

                                    if (error != null
                                            || snapshot == null) {
                                        return;
                                    }

                                    /*
                                     * The first snapshot contains
                                     * existing messages.
                                     *
                                     * Do NOT notify the user about
                                     * old messages.
                                     */
                                    if (!initializedRides.contains(
                                            rideId
                                    )) {

                                        initializedRides.add(
                                                rideId
                                        );

                                        return;
                                    }

                                    for (DocumentChange change
                                            : snapshot
                                            .getDocumentChanges()) {

                                        if (change.getType()
                                                != DocumentChange
                                                .Type.ADDED) {
                                            continue;
                                        }

                                        showIncomingMessage(
                                                rideId,
                                                change.getDocument()
                                        );
                                    }
                                }
                        );

        messageListeners.put(
                rideId,
                registration
        );
    }

    private void showIncomingMessage(
            String rideId,
            DocumentSnapshot messageDocument
    ) {

        FirebaseUser user =
                auth.getCurrentUser();

        if (user == null) {
            return;
        }

        String senderId =
                getText(
                        messageDocument,
                        "senderId"
                );

        /*
         * Never notify the person who sent the message.
         */
        if (user.getUid().equals(senderId)) {
            return;
        }

        String message =
                getText(
                        messageDocument,
                        "message"
                );

        if (message.isEmpty()) {
            return;
        }

        String senderRole =
                getText(
                        messageDocument,
                        "senderRole"
                );

        String title;

        if ("DRIVER".equalsIgnoreCase(
                senderRole
        )) {

            title =
                    "🚕 Sakay Na — Driver Message";

        } else if ("PASSENGER".equalsIgnoreCase(
                senderRole
        )) {

            title =
                    "👤 Sakay Na — Passenger Message";

        } else {

            title =
                    "💬 Sakay Na — New Message";
        }

        Intent intent =
                new Intent(
                        this,
                        RideChatActivity.class
                );

        intent.putExtra(
                "ride_id",
                rideId
        );

        intent.putExtra(
                "rideId",
                rideId
        );

        intent.addFlags(
                Intent.FLAG_ACTIVITY_CLEAR_TOP
                        | Intent.FLAG_ACTIVITY_SINGLE_TOP
        );

        int flags =
                PendingIntent.FLAG_UPDATE_CURRENT;

        if (Build.VERSION.SDK_INT >= 23) {
            flags |= PendingIntent.FLAG_IMMUTABLE;
        }

        PendingIntent pendingIntent =
                PendingIntent.getActivity(
                        this,
                        Math.abs(
                                (
                                        rideId
                                                + messageDocument.getId()
                                ).hashCode()
                        ),
                        intent,
                        flags
                );

        int notificationId =
                Math.abs(
                        (
                                rideId
                                        + messageDocument.getId()
                        ).hashCode()
                );

        SakayNaNotificationHelper.show(
                this,
                notificationId,
                title,
                message,
                pendingIntent
        );
    }

    private void removeRideListener(
            String rideId
    ) {

        ListenerRegistration registration =
                messageListeners.remove(
                        rideId
                );

        if (registration != null) {
            registration.remove();
        }

        initializedRides.remove(
                rideId
        );

        knownRideIds.remove(
                rideId
        );
    }

    private String getText(
            DocumentSnapshot document,
            String field
    ) {

        String value =
                document.getString(field);

        if (value == null) {
            return "";
        }

        return value.trim();
    }

    @Override
    public int onStartCommand(
            Intent intent,
            int flags,
            int startId
    ) {

        /*
         * Keep monitoring after the activity is moved
         * to the background.
         */
        return START_STICKY;
    }

    @Override
    public void onDestroy() {

        if (passengerRideListener != null) {
            passengerRideListener.remove();
            passengerRideListener = null;
        }

        if (driverRideListener != null) {
            driverRideListener.remove();
            driverRideListener = null;
        }

        for (ListenerRegistration registration
                : messageListeners.values()) {

            if (registration != null) {
                registration.remove();
            }
        }

        messageListeners.clear();
        initializedRides.clear();
        knownRideIds.clear();

        super.onDestroy();
    }

    @Nullable
    @Override
    public IBinder onBind(
            Intent intent
    ) {
        return null;
    }
}

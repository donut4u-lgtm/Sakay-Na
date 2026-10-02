package com.sakyna.app;

import android.app.Notification;
import android.app.NotificationChannel;
import android.app.NotificationManager;
import android.app.PendingIntent;
import android.app.Service;
import android.content.Intent;
import android.content.pm.ServiceInfo;
import android.os.Build;
import android.os.Handler;
import android.os.IBinder;
import android.os.Looper;

import androidx.annotation.Nullable;
import androidx.core.app.NotificationCompat;

import com.google.firebase.auth.FirebaseAuth;
import com.google.firebase.auth.FirebaseUser;
import com.google.firebase.firestore.DocumentChange;
import com.google.firebase.firestore.DocumentSnapshot;
import com.google.firebase.firestore.FirebaseFirestore;
import com.google.firebase.firestore.ListenerRegistration;
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

    private static final long RETRY_DELAY_MS =
            5000L;

    private FirebaseFirestore db;
    private FirebaseAuth auth;

    private ListenerRegistration passengerRideListener;
    private ListenerRegistration driverRideListener;

    private final Map<String, ListenerRegistration>
            messageListeners = new HashMap<>();

    private final Set<String> initializedRides =
            new HashSet<>();

    private final Handler mainHandler =
            new Handler(Looper.getMainLooper());

    private final Runnable retryMonitoring =
            () -> startRideMonitoring();

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
                "Keeps Sakay Na chat notifications active during an ongoing ride."
        );

        channel.setShowBadge(false);

        NotificationManager manager =
                getSystemService(NotificationManager.class);

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
                    ServiceInfo.FOREGROUND_SERVICE_TYPE_REMOTE_MESSAGING
            );

        } else {

            startForeground(
                    SERVICE_NOTIFICATION_ID,
                    notification
            );
        }
    }

    private void startRideMonitoring() {

        mainHandler.removeCallbacks(
                retryMonitoring
        );

        FirebaseUser user =
                auth.getCurrentUser();

        if (user == null) {
            scheduleMonitoringRetry();
            return;
        }

        removeRideListenersOnly();

        final String uid =
                user.getUid();

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

                                        scheduleMonitoringRetry();
                                        return;
                                    }

                                    processRideSnapshot(
                                            snapshot
                                    );
                                }
                        );

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

                                        scheduleMonitoringRetry();
                                        return;
                                    }

                                    processRideSnapshot(
                                            snapshot
                                    );
                                }
                        );
    }

    private void scheduleMonitoringRetry() {

        mainHandler.removeCallbacks(
                retryMonitoring
        );

        mainHandler.postDelayed(
                retryMonitoring,
                RETRY_DELAY_MS
        );
    }

    private void processRideSnapshot(
            QuerySnapshot snapshot
    ) {

        for (DocumentSnapshot ride :
                snapshot.getDocuments()) {

            String rideId =
                    ride.getId();

            String status =
                    getText(
                            ride,
                            "status"
                    );

            if (!isChatRideStatus(status)) {

                removeMessageListener(
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
                || rideId.trim().isEmpty()
                || messageListeners.containsKey(
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

                                        removeMessageListener(
                                                rideId
                                        );

                                        scheduleMonitoringRetry();
                                        return;
                                    }

                                    /*
                                     * First snapshot is existing
                                     * chat history.
                                     *
                                     * Never notify old messages.
                                     */
                                    if (!initializedRides.contains(
                                            rideId
                                    )) {

                                        initializedRides.add(
                                                rideId
                                        );

                                        return;
                                    }

                                    for (DocumentChange change :
                                            snapshot
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
         * Never notify the person who
         * sent the message.
         */
        if (user.getUid().equals(
                senderId
        )) {
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

        int notificationId =
                createNotificationId(
                        rideId,
                        messageDocument.getId()
                );

        int flags =
                PendingIntent.FLAG_UPDATE_CURRENT;

        if (Build.VERSION.SDK_INT >= 23) {

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
            String messageId
    ) {

        int hash =
                (
                        rideId
                                + ":"
                                + messageId
                ).hashCode();

        if (hash == Integer.MIN_VALUE) {
            return 1;
        }

        return Math.abs(hash);
    }

    private void removeMessageListener(
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
    }

    private void removeRideListenersOnly() {

        if (passengerRideListener != null) {

            passengerRideListener.remove();

            passengerRideListener = null;
        }

        if (driverRideListener != null) {

            driverRideListener.remove();

            driverRideListener = null;
        }
    }

    private void stopAllListeners() {

        removeRideListenersOnly();

        for (ListenerRegistration registration :
                messageListeners.values()) {

            if (registration != null) {
                registration.remove();
            }
        }

        messageListeners.clear();
        initializedRides.clear();
    }

    private String getText(
            DocumentSnapshot document,
            String field
    ) {

        String value =
                document.getString(
                        field
                );

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
         * Ask Android to recreate this foreground
         * service if its process is killed.
         */
        if (passengerRideListener == null
                && driverRideListener == null) {

            startRideMonitoring();
        }

        return START_STICKY;
    }

    @Override
    public void onTaskRemoved(
            Intent rootIntent
    ) {

        /*
         * Do not stop the service when the user
         * removes Sakay Na from Recents.
         */
        super.onTaskRemoved(
                rootIntent
        );
    }

    @Override
    public void onDestroy() {

        mainHandler.removeCallbacks(
                retryMonitoring
        );

        stopAllListeners();

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

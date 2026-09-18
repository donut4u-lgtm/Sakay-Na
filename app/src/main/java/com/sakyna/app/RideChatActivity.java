package com.sakyna.app;

import android.os.Bundle;
import android.text.TextUtils;
import android.view.Gravity;
import android.view.View;
import android.widget.Button;
import android.widget.EditText;
import android.widget.LinearLayout;
import android.widget.ScrollView;
import android.widget.TextView;
import android.widget.Toast;

import androidx.appcompat.app.AppCompatActivity;

import com.google.firebase.Timestamp;
import com.google.firebase.auth.FirebaseAuth;
import com.google.firebase.auth.FirebaseUser;
import com.google.firebase.firestore.DocumentSnapshot;
import com.google.firebase.firestore.FirebaseFirestore;
import com.google.firebase.firestore.ListenerRegistration;

import java.util.ArrayList;
import java.util.Collections;
import java.util.Comparator;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

public class RideChatActivity extends AppCompatActivity {

    private FirebaseAuth auth;
    private FirebaseFirestore db;

    private String rideId;
    private String currentUid;
    private String currentRole;

    private LinearLayout messageContainer;
    private EditText messageInput;
    private Button sendButton;
    private TextView statusText;
    private ScrollView scrollView;

    private ListenerRegistration messageListener;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);

        auth = FirebaseAuth.getInstance();
        db = FirebaseFirestore.getInstance();

        FirebaseUser user = auth.getCurrentUser();

        if (user == null) {
            Toast.makeText(
                    this,
                    "Please login first.",
                    Toast.LENGTH_LONG
            ).show();
            finish();
            return;
        }

        currentUid = user.getUid();

        rideId = getIntent().getStringExtra("ride_id");

        if (TextUtils.isEmpty(rideId)) {
            rideId = getIntent().getStringExtra("rideId");
        }

        if (TextUtils.isEmpty(rideId)) {
            Toast.makeText(
                    this,
                    "No ride selected.",
                    Toast.LENGTH_LONG
            ).show();
            finish();
            return;
        }

        buildChatScreen();
        verifyRide();
    }

    private void buildChatScreen() {

        LinearLayout root = new LinearLayout(this);
        root.setOrientation(LinearLayout.VERTICAL);
        root.setBackgroundColor(0xFFF5F5F5);

        TextView title = new TextView(this);
        title.setText("💬 SAKAY NA RIDE CHAT");
        title.setTextSize(21);
        title.setTextColor(0xFFFFFFFF);
        title.setGravity(Gravity.CENTER_VERTICAL);
        title.setPadding(dp(20), 0, dp(20), 0);
        title.setBackgroundColor(0xFF008000);

        root.addView(
                title,
                new LinearLayout.LayoutParams(
                        LinearLayout.LayoutParams.MATCH_PARENT,
                        dp(60)
                )
        );

        statusText = new TextView(this);
        statusText.setText("Checking ride...");
        statusText.setTextSize(14);
        statusText.setTextColor(0xFF555555);
        statusText.setPadding(
                dp(15),
                dp(10),
                dp(15),
                dp(10)
        );

        root.addView(
                statusText,
                new LinearLayout.LayoutParams(
                        LinearLayout.LayoutParams.MATCH_PARENT,
                        LinearLayout.LayoutParams.WRAP_CONTENT
                )
        );

        scrollView = new ScrollView(this);

        messageContainer = new LinearLayout(this);
        messageContainer.setOrientation(LinearLayout.VERTICAL);
        messageContainer.setPadding(
                dp(12),
                dp(10),
                dp(12),
                dp(10)
        );

        scrollView.addView(messageContainer);

        root.addView(
                scrollView,
                new LinearLayout.LayoutParams(
                        LinearLayout.LayoutParams.MATCH_PARENT,
                        0,
                        1
                )
        );

        LinearLayout bottom = new LinearLayout(this);
        bottom.setOrientation(LinearLayout.HORIZONTAL);
        bottom.setPadding(
                dp(8),
                dp(8),
                dp(8),
                dp(8)
        );
        bottom.setBackgroundColor(0xFFFFFFFF);

        messageInput = new EditText(this);
        messageInput.setHint("Type a message...");
        messageInput.setSingleLine(false);
        messageInput.setMaxLines(3);
        messageInput.setTextSize(16);

        bottom.addView(
                messageInput,
                new LinearLayout.LayoutParams(
                        0,
                        LinearLayout.LayoutParams.WRAP_CONTENT,
                        1
                )
        );

        sendButton = new Button(this);
        sendButton.setText("SEND");
        sendButton.setTextColor(0xFFFFFFFF);
        sendButton.setBackgroundColor(0xFF008000);

        sendButton.setOnClickListener(v -> sendMessage());

        bottom.addView(
                sendButton,
                new LinearLayout.LayoutParams(
                        dp(95),
                        dp(55)
                )
        );

        root.addView(
                bottom,
                new LinearLayout.LayoutParams(
                        LinearLayout.LayoutParams.MATCH_PARENT,
                        LinearLayout.LayoutParams.WRAP_CONTENT
                )
        );

        setContentView(root);
    }

    private void verifyRide() {

        statusText.setText(
                "Checking ride...\nRide ID: " + rideId
        );

        db.collection("rides")
                .document(rideId)
                .get()
                .addOnSuccessListener(this::handleRide)
                .addOnFailureListener(e -> {

                    statusText.setText(
                            "🔴 RIDE READ ERROR\n" +
                            e.getMessage()
                    );

                    messageInput.setEnabled(false);
                    sendButton.setEnabled(false);

                    Toast.makeText(
                            RideChatActivity.this,
                            "Cannot open ride: " + e.getMessage(),
                            Toast.LENGTH_LONG
                    ).show();
                });
    }

    private void handleRide(DocumentSnapshot ride) {

        if (!ride.exists()) {

            statusText.setText(
                    "🔴 Ride does not exist.\n" +
                    "Ride ID: " + rideId
            );

            messageInput.setEnabled(false);
            sendButton.setEnabled(false);

            Toast.makeText(
                    this,
                    "Ride not found.",
                    Toast.LENGTH_LONG
            ).show();

            return;
        }

        String passengerId = ride.getString("passengerId");
        String driverId = ride.getString("driverId");
        String status = ride.getString("status");

        boolean passenger =
                passengerId != null &&
                passengerId.equals(currentUid);

        boolean driver =
                driverId != null &&
                driverId.equals(currentUid);

        if (!passenger && !driver) {

            statusText.setText(
                    "🔴 NOT A RIDE PARTICIPANT\n" +
                    "Ride: " + rideId
            );

            messageInput.setEnabled(false);
            sendButton.setEnabled(false);

            Toast.makeText(
                    this,
                    "Chat is only available to the passenger and driver.",
                    Toast.LENGTH_LONG
            ).show();

            return;
        }

        currentRole = passenger ? "PASSENGER" : "DRIVER";

        /*
         * Chat is available only after a driver has been assigned.
         */
        if (driverId == null || driverId.trim().isEmpty()) {

            statusText.setText(
                    "🟠 WAITING FOR DRIVER\n" +
                    "Chat opens after driver acceptance.\n" +
                    "Ride: " + rideId
            );

            messageInput.setEnabled(false);
            sendButton.setEnabled(false);

            return;
        }

        messageInput.setEnabled(true);
        sendButton.setEnabled(true);

        statusText.setText(
                "🟢 CHAT READY\n" +
                currentRole +
                "\nRide: " +
                rideId +
                "\nStatus: " +
                (status == null ? "UNKNOWN" : status)
        );

        startMessageListener();
    }

    private void startMessageListener() {

        if (messageListener != null) {
            messageListener.remove();
            messageListener = null;
        }

        messageContainer.removeAllViews();

        statusText.setText(
                "🟢 CHAT CONNECTED\n" +
                "Listening: rides/" +
                rideId +
                "/messages"
        );

        /*
         * IMPORTANT:
         *
         * There is deliberately NO orderBy() here.
         *
         * We listen directly to:
         *
         * rides/{rideId}/messages
         *
         * This avoids Firestore query/index problems.
         */
        messageListener =
                db.collection("rides")
                        .document(rideId)
                        .collection("messages")
                        .addSnapshotListener((snapshot, error) -> {

                            if (error != null) {

                                statusText.setText(
                                        "🔴 CHAT LISTENER ERROR\n" +
                                        error.getMessage() +
                                        "\nRide: " +
                                        rideId
                                );

                                Toast.makeText(
                                        RideChatActivity.this,
                                        "Chat listener error: " +
                                                error.getMessage(),
                                        Toast.LENGTH_LONG
                                ).show();

                                return;
                            }

                            if (snapshot == null) {
                                return;
                            }

                            List<DocumentSnapshot> messages =
                                    new ArrayList<>(
                                            snapshot.getDocuments()
                                    );

                            /*
                             * Sort locally by createdAt.
                             *
                             * This does not require a Firestore
                             * orderBy query or composite index.
                             */
                            Collections.sort(
                                    messages,
                                    new Comparator<DocumentSnapshot>() {
                                        @Override
                                        public int compare(
                                                DocumentSnapshot a,
                                                DocumentSnapshot b
                                        ) {

                                            Timestamp ta =
                                                    a.getTimestamp("createdAt");

                                            Timestamp tb =
                                                    b.getTimestamp("createdAt");

                                            if (ta == null && tb == null) {
                                                return 0;
                                            }

                                            if (ta == null) {
                                                return -1;
                                            }

                                            if (tb == null) {
                                                return 1;
                                            }

                                            return ta.compareTo(tb);
                                        }
                                    }
                            );

                            messageContainer.removeAllViews();

                            if (messages.isEmpty()) {

                                TextView empty =
                                        new TextView(
                                                RideChatActivity.this
                                        );

                                empty.setText(
                                        "No messages yet.\n" +
                                        "Send the first message."
                                );

                                empty.setTextSize(16);
                                empty.setTextColor(0xFF777777);
                                empty.setGravity(Gravity.CENTER);
                                empty.setPadding(
                                        dp(20),
                                        dp(40),
                                        dp(20),
                                        dp(40)
                                );

                                messageContainer.addView(
                                        empty,
                                        new LinearLayout.LayoutParams(
                                                LinearLayout.LayoutParams.MATCH_PARENT,
                                                LinearLayout.LayoutParams.WRAP_CONTENT
                                        )
                                );

                                statusText.setText(
                                        "🟢 CHAT CONNECTED\n" +
                                        "No messages yet\n" +
                                        "Ride: " +
                                        rideId
                                );

                                return;
                            }

                            for (DocumentSnapshot document : messages) {
                                addMessageToScreen(document);
                            }

                            statusText.setText(
                                    "🟢 CHAT LIVE\n" +
                                    messages.size() +
                                    " message(s)\n" +
                                    "Ride: " +
                                    rideId
                            );

                            scrollToBottom();
                        });
    }

    private void addMessageToScreen(
            DocumentSnapshot document
    ) {

        String senderId =
                document.getString("senderId");

        String senderRole =
                document.getString("senderRole");

        String message =
                document.getString("message");

        if (message == null) {
            return;
        }

        boolean mine =
                senderId != null &&
                senderId.equals(currentUid);

        LinearLayout row =
                new LinearLayout(this);

        row.setOrientation(
                LinearLayout.VERTICAL
        );

        if (mine) {
            row.setGravity(Gravity.RIGHT);
        } else {
            row.setGravity(Gravity.LEFT);
        }

        TextView bubble =
                new TextView(this);

        String name;

        if (mine) {
            name = "You";
        } else if ("DRIVER".equalsIgnoreCase(senderRole)) {
            name = "Driver";
        } else {
            name = "Passenger";
        }

        bubble.setText(
                name +
                "\n" +
                message
        );

        bubble.setTextSize(16);
        bubble.setTextColor(0xFF000000);
        bubble.setPadding(
                dp(20),
                dp(12),
                dp(20),
                dp(12)
        );

        if (mine) {
            bubble.setBackgroundColor(0xFFD7F8D7);
        } else {
            bubble.setBackgroundColor(0xFFFFFFFF);
        }

        LinearLayout.LayoutParams params =
                new LinearLayout.LayoutParams(
                        LinearLayout.LayoutParams.WRAP_CONTENT,
                        LinearLayout.LayoutParams.WRAP_CONTENT
                );

        params.setMargins(
                dp(6),
                dp(5),
                dp(6),
                dp(5)
        );

        row.addView(
                bubble,
                params
        );

        messageContainer.addView(row);
    }

    private void sendMessage() {

        String text =
                messageInput
                        .getText()
                        .toString()
                        .trim();

        if (text.isEmpty()) {
            return;
        }

        if (TextUtils.isEmpty(rideId)) {

            Toast.makeText(
                    this,
                    "No ride selected.",
                    Toast.LENGTH_LONG
            ).show();

            return;
        }

        if (TextUtils.isEmpty(currentRole)) {

            Toast.makeText(
                    this,
                    "Chat is not ready.",
                    Toast.LENGTH_LONG
            ).show();

            return;
        }

        messageInput.setEnabled(false);
        sendButton.setEnabled(false);

        statusText.setText(
                "Sending message...\nRide: " +
                rideId
        );

        Map<String, Object> data =
                new HashMap<>();

        data.put(
                "senderId",
                currentUid
        );

        data.put(
                "senderRole",
                currentRole
        );

        data.put(
                "message",
                text
        );

        data.put(
                "createdAt",
                Timestamp.now()
        );

        String messageId =
                db.collection("rides")
                        .document(rideId)
                        .collection("messages")
                        .document()
                        .getId();

        db.collection("rides")
                .document(rideId)
                .collection("messages")
                .document(messageId)
                .set(data)
                .addOnSuccessListener(unused -> {

                    messageInput.setText("");

                    messageInput.setEnabled(true);
                    sendButton.setEnabled(true);

                    statusText.setText(
                            "🟢 MESSAGE SENT\n" +
                            "Ride: " +
                            rideId
                    );

                    /*
                     * Do not manually insert the message.
                     *
                     * The Firestore listener displays it.
                     */
                })
                .addOnFailureListener(e -> {

                    messageInput.setEnabled(true);
                    sendButton.setEnabled(true);

                    statusText.setText(
                            "🔴 MESSAGE SEND FAILED\n" +
                            e.getMessage() +
                            "\nRide: " +
                            rideId
                    );

                    Toast.makeText(
                            RideChatActivity.this,
                            "Message failed: " +
                                    e.getMessage(),
                            Toast.LENGTH_LONG
                    ).show();
                });
    }

    private void scrollToBottom() {

        scrollView.post(() ->
                scrollView.fullScroll(
                        View.FOCUS_DOWN
                )
        );
    }

    private int dp(int value) {

        float density =
                getResources()
                        .getDisplayMetrics()
                        .density;

        return (int)
                (value * density + 0.5f);
    }

    @Override
    protected void onDestroy() {

        if (messageListener != null) {
            messageListener.remove();
            messageListener = null;
        }

        super.onDestroy();
    }
}

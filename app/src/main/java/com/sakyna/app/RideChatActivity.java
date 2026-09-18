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
import com.google.firebase.firestore.DocumentChange;
import com.google.firebase.firestore.DocumentSnapshot;
import com.google.firebase.firestore.FirebaseFirestore;
import com.google.firebase.firestore.ListenerRegistration;
import com.google.firebase.firestore.Query;

import java.util.HashMap;
import java.util.Map;

public class RideChatActivity extends AppCompatActivity {

    private FirebaseAuth auth;
    private FirebaseFirestore db;

    private String rideId;
    private String currentUid;
    private String currentRole;

    private LinearLayout messageContainer;
    private EditText messageInput;
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
            Toast.makeText(this, "Please login first.", Toast.LENGTH_LONG).show();
            finish();
            return;
        }

        currentUid = user.getUid();

        rideId = getIntent().getStringExtra("ride_id");

        if (TextUtils.isEmpty(rideId)) {
            rideId = getIntent().getStringExtra("rideId");
        }

        if (TextUtils.isEmpty(rideId)) {
            Toast.makeText(this, "No ride selected.", Toast.LENGTH_LONG).show();
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
        title.setPadding(24, 0, 24, 0);
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
        statusText.setPadding(20, 12, 20, 12);

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
        messageContainer.setPadding(16, 12, 16, 12);

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
        bottom.setPadding(10, 10, 10, 10);
        bottom.setBackgroundColor(0xFFFFFFFF);

        messageInput = new EditText(this);
        messageInput.setHint("Type a message...");
        messageInput.setSingleLine(false);
        messageInput.setMaxLines(3);

        bottom.addView(
                messageInput,
                new LinearLayout.LayoutParams(
                        0,
                        LinearLayout.LayoutParams.WRAP_CONTENT,
                        1
                )
        );

        Button sendButton = new Button(this);
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

        statusText.setText("Checking ride...");

        db.collection("rides")
                .document(rideId)
                .get()
                .addOnSuccessListener(this::handleRide)
                .addOnFailureListener(e -> {
                    statusText.setText("🔴 RIDE READ ERROR: " + e.getMessage());

                    Toast.makeText(
                            RideChatActivity.this,
                            "Cannot open ride: " + e.getMessage(),
                            Toast.LENGTH_LONG
                    ).show();
                });
    }

    private void handleRide(DocumentSnapshot ride) {

        if (!ride.exists()) {
            statusText.setText("🔴 Ride does not exist.");
            Toast.makeText(this, "Ride not found.", Toast.LENGTH_LONG).show();
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
            statusText.setText("🔴 You are not a participant in this ride.");
            Toast.makeText(
                    this,
                    "Chat is only available to the passenger and driver.",
                    Toast.LENGTH_LONG
            ).show();
            return;
        }

        currentRole = passenger ? "PASSENGER" : "DRIVER";

        /*
         * Chat requires an actual booking.
         *
         * For the passenger, a driver must already be assigned.
         * This prevents pre-booking/general chat.
         */
        if (driverId == null || driverId.trim().isEmpty()) {

            statusText.setText(
                    "🟠 Waiting for a driver. Chat will open after acceptance."
            );

            messageInput.setEnabled(false);

            return;
        }

        messageInput.setEnabled(true);

        statusText.setText(
                "🟢 CONNECTED • " +
                currentRole +
                " • Ride: " +
                rideId
        );

        startMessageListener();
    }

    private void startMessageListener() {

        if (messageListener != null) {
            messageListener.remove();
        }

        messageContainer.removeAllViews();

        statusText.setText(
                "🟢 CHAT CONNECTED • Waiting for messages..."
        );

        /*
         * IMPORTANT:
         *
         * Both passenger and driver listen to this exact same path:
         *
         * rides/{rideId}/messages
         *
         * Therefore a message written by one side is immediately
         * delivered to the other side.
         */
        messageListener = db.collection("rides")
                .document(rideId)
                .collection("messages")
                .orderBy("createdAt", Query.Direction.ASCENDING)
                .addSnapshotListener((snapshot, error) -> {

                    if (error != null) {

                        statusText.setText(
                                "🔴 CHAT LISTENER ERROR: " +
                                error.getMessage()
                        );

                        Toast.makeText(
                                RideChatActivity.this,
                                "Chat listener error: " + error.getMessage(),
                                Toast.LENGTH_LONG
                        ).show();

                        return;
                    }

                    if (snapshot == null) {
                        return;
                    }

                    if (snapshot.isEmpty()) {

                        messageContainer.removeAllViews();

                        TextView empty = new TextView(
                                RideChatActivity.this
                        );

                        empty.setText(
                                "No messages yet.\nSend the first message."
                        );

                        empty.setTextSize(16);
                        empty.setTextColor(0xFF777777);
                        empty.setGravity(Gravity.CENTER);
                        empty.setPadding(20, 40, 20, 40);

                        messageContainer.addView(
                                empty,
                                new LinearLayout.LayoutParams(
                                        LinearLayout.LayoutParams.MATCH_PARENT,
                                        LinearLayout.LayoutParams.WRAP_CONTENT
                                )
                        );

                        statusText.setText(
                                "🟢 CHAT CONNECTED • No messages yet"
                        );

                        return;
                    }

                    /*
                     * Rebuild the visible message list from Firestore.
                     *
                     * This avoids duplicate messages and guarantees
                     * both sides display the same conversation.
                     */
                    messageContainer.removeAllViews();

                    for (DocumentSnapshot document : snapshot.getDocuments()) {
                        addMessageToScreen(document);
                    }

                    statusText.setText(
                            "🟢 CHAT CONNECTED • " +
                            snapshot.size() +
                            " message(s)"
                    );

                    scrollToBottom();
                });
    }

    private void addMessageToScreen(DocumentSnapshot document) {

        String senderId = document.getString("senderId");
        String senderRole = document.getString("senderRole");
        String message = document.getString("message");

        if (message == null) {
            return;
        }

        boolean mine =
                senderId != null &&
                senderId.equals(currentUid);

        LinearLayout row = new LinearLayout(this);
        row.setOrientation(LinearLayout.VERTICAL);

        if (mine) {
            row.setGravity(Gravity.RIGHT);
        } else {
            row.setGravity(Gravity.LEFT);
        }

        TextView bubble = new TextView(this);

        String name;

        if (mine) {
            name = "You";
        } else if ("DRIVER".equalsIgnoreCase(senderRole)) {
            name = "Driver";
        } else {
            name = "Passenger";
        }

        bubble.setText(name + "\n" + message);
        bubble.setTextSize(16);
        bubble.setTextColor(0xFF000000);
        bubble.setPadding(22, 14, 22, 14);

        if (mine) {
            bubble.setBackgroundColor(0xFFD7F8D7);
        } else {
            bubble.setBackgroundColor(0xFFFFFFFF);
        }

        LinearLayout.LayoutParams bubbleParams =
                new LinearLayout.LayoutParams(
                        LinearLayout.LayoutParams.WRAP_CONTENT,
                        LinearLayout.LayoutParams.WRAP_CONTENT
                );

        bubbleParams.setMargins(
                8,
                6,
                8,
                6
        );

        row.addView(bubble, bubbleParams);

        messageContainer.addView(row);
    }

    private void sendMessage() {

        String text = messageInput.getText()
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

        statusText.setText("Sending...");

        Map<String, Object> data = new HashMap<>();

        data.put("senderId", currentUid);
        data.put("senderRole", currentRole);
        data.put("message", text);
        data.put("createdAt", Timestamp.now());

        /*
         * Explicit document ID.
         *
         * This guarantees that the message is written into:
         *
         * rides/{rideId}/messages/{messageId}
         */
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

                    statusText.setText(
                            "🟢 MESSAGE SENT • " + messageId
                    );

                    messageInput.setEnabled(true);

                    /*
                     * Do NOT manually add the message here.
                     *
                     * Firestore snapshot listener will receive it
                     * and display it. The other person's device will
                     * receive the same snapshot automatically.
                     */
                })
                .addOnFailureListener(e -> {

                    messageInput.setEnabled(true);

                    statusText.setText(
                            "🔴 MESSAGE SEND FAILED\n" +
                            e.getMessage()
                    );

                    Toast.makeText(
                            RideChatActivity.this,
                            "Message failed: " + e.getMessage(),
                            Toast.LENGTH_LONG
                    ).show();
                });
    }

    private void scrollToBottom() {

        scrollView.post(() ->
                scrollView.fullScroll(View.FOCUS_DOWN)
        );
    }

    private int dp(int value) {

        float density =
                getResources()
                        .getDisplayMetrics()
                        .density;

        return (int) (value * density + 0.5f);
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

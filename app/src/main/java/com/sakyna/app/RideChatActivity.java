
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

    private ListenerRegistration rideListener;
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
        listenToRide();
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
        statusText.setText("Connecting to ride...");
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
        sendButton.setEnabled(false);

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

    private void listenToRide() {

        if (rideListener != null) {
            rideListener.remove();
            rideListener = null;
        }

        rideListener =
                db.collection("rides")
                        .document(rideId)
                        .addSnapshotListener((ride, error) -> {

                            if (error != null) {

                                statusText.setText(
                                        "🔴 RIDE ERROR\n"
                                                + error.getMessage()
                                );

                                messageInput.setEnabled(false);
                                sendButton.setEnabled(false);

                                return;
                            }

                            if (ride == null || !ride.exists()) {

                                statusText.setText(
                                        "🔴 Ride not found."
                                );

                                messageInput.setEnabled(false);
                                sendButton.setEnabled(false);

                                stopMessageListener();

                                return;
                            }

                            String passengerId =
                                    ride.getString("passengerId");

                            String driverId =
                                    ride.getString("driverId");

                            String status =
                                    ride.getString("status");

                            boolean isPassenger =
                                    currentUid.equals(passengerId);

                            boolean isDriver =
                                    currentUid.equals(driverId);

                            if (!isPassenger && !isDriver) {

                                statusText.setText(
                                        "🔴 NOT A RIDE PARTICIPANT"
                                );

                                messageInput.setEnabled(false);
                                sendButton.setEnabled(false);

                                stopMessageListener();

                                return;
                            }

                            currentRole =
                                    isPassenger
                                            ? "PASSENGER"
                                            : "DRIVER";

                            if (driverId == null
                                    || driverId.trim().isEmpty()) {

                                statusText.setText(
                                        "🟠 WAITING FOR DRIVER\n"
                                                + "Chat opens after driver acceptance."
                                );

                                messageInput.setEnabled(false);
                                sendButton.setEnabled(false);

                                stopMessageListener();

                                return;
                            }

                            messageInput.setEnabled(true);
                            sendButton.setEnabled(true);

                            statusText.setText(
                                    "🟢 CHAT LIVE\n"
                                            + currentRole
                                            + "\nRide: "
                                            + rideId
                                            + "\nStatus: "
                                            + (
                                            status == null
                                                    ? "UNKNOWN"
                                                    : status
                                    )
                            );

                            startMessageListener();
                        });
    }

    private void startMessageListener() {

        if (messageListener != null) {
            return;
        }

        messageContainer.removeAllViews();

        messageListener =
                db.collection("rides")
                        .document(rideId)
                        .collection("messages")
                        .addSnapshotListener((snapshot, error) -> {

                            if (error != null) {

                                statusText.setText(
                                        "🔴 CHAT ERROR\n"
                                                + error.getMessage()
                                );

                                return;
                            }

                            if (snapshot == null) {
                                return;
                            }

                            List<DocumentSnapshot> messages =
                                    new ArrayList<>(
                                            snapshot.getDocuments()
                                    );

                            Collections.sort(
                                    messages,
                                    new Comparator<DocumentSnapshot>() {

                                        @Override
                                        public int compare(
                                                DocumentSnapshot a,
                                                DocumentSnapshot b
                                        ) {

                                            Timestamp ta =
                                                    a.getTimestamp(
                                                            "createdAt"
                                                    );

                                            Timestamp tb =
                                                    b.getTimestamp(
                                                            "createdAt"
                                                    );

                                            if (ta == null && tb == null) {
                                                return a.getId().compareTo(
                                                        b.getId()
                                                );
                                            }

                                            if (ta == null) {
                                                return -1;
                                            }

                                            if (tb == null) {
                                                return 1;
                                            }

                                            int result =
                                                    ta.compareTo(tb);

                                            if (result == 0) {
                                                return a.getId().compareTo(
                                                        b.getId()
                                                );
                                            }

                                            return result;
                                        }
                                    }
                            );

                            messageContainer.removeAllViews();

                            if (messages.isEmpty()) {

                                TextView empty =
                                        new TextView(this);

                                empty.setText(
                                        "No messages yet.\n"
                                                + "Send the first message."
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

                                messageContainer.addView(empty);

                                statusText.setText(
                                        "🟢 CHAT LIVE\n"
                                                + "No messages yet"
                                );

                                return;
                            }

                            for (DocumentSnapshot message
                                    : messages) {

                                addMessageToScreen(message);
                            }

                            statusText.setText(
                                    "🟢 CHAT LIVE\n"
                                            + messages.size()
                                            + " message(s)"
                            );

                            scrollToBottom();
                        });
    }

    private void stopMessageListener() {

        if (messageListener != null) {
            messageListener.remove();
            messageListener = null;
        }

        if (messageContainer != null) {
            messageContainer.removeAllViews();
        }
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

        if (message == null
                || message.trim().isEmpty()) {
            return;
        }

        boolean mine =
                currentUid.equals(senderId);

        LinearLayout row =
                new LinearLayout(this);

        row.setOrientation(
                LinearLayout.VERTICAL
        );

        row.setGravity(
                mine
                        ? Gravity.RIGHT
                        : Gravity.LEFT
        );

        TextView bubble =
                new TextView(this);

        String senderName;

        if (mine) {
            senderName = "You";
        } else if ("DRIVER".equalsIgnoreCase(senderRole)) {
            senderName = "Driver";
        } else {
            senderName = "Passenger";
        }

        bubble.setText(
                senderName
                        + "\n"
                        + message
        );

        bubble.setTextSize(16);
        bubble.setTextColor(0xFF000000);

        bubble.setPadding(
                dp(20),
                dp(12),
                dp(20),
                dp(12)
        );

        bubble.setBackgroundColor(
                mine
                        ? 0xFFD7F8D7
                        : 0xFFFFFFFF
        );

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

        if (TextUtils.isEmpty(rideId)
                || TextUtils.isEmpty(currentUid)
                || TextUtils.isEmpty(currentRole)) {

            Toast.makeText(
                    this,
                    "Chat is not ready.",
                    Toast.LENGTH_SHORT
            ).show();

            return;
        }

        messageInput.setEnabled(false);
        sendButton.setEnabled(false);

        statusText.setText(
                "Sending message..."
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

        db.collection("rides")
                .document(rideId)
                .collection("messages")
                .add(data)
                .addOnSuccessListener(documentReference -> {

                    messageInput.setText("");

                    messageInput.setEnabled(true);
                    sendButton.setEnabled(true);

                    statusText.setText(
                            "🟢 MESSAGE SENT\n"
                                    + "Waiting for live update..."
                    );
                })
                .addOnFailureListener(e -> {

                    messageInput.setEnabled(true);
                    sendButton.setEnabled(true);

                    statusText.setText(
                            "🔴 MESSAGE FAILED\n"
                                    + e.getMessage()
                    );

                    Toast.makeText(
                            this,
                            "Message failed:\n"
                                    + e.getMessage(),
                            Toast.LENGTH_LONG
                    ).show();
                });
    }

    private void scrollToBottom() {

        if (scrollView == null) {
            return;
        }

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

        if (rideListener != null) {
            rideListener.remove();
            rideListener = null;
        }

        if (messageListener != null) {
            messageListener.remove();
            messageListener = null;
        }

        super.onDestroy();
    }
}

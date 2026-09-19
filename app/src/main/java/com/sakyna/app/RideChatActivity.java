
package com.sakyna.app;

import android.app.Activity;
import android.graphics.Color;
import android.graphics.Typeface;
import android.os.Bundle;
import android.text.InputType;
import android.view.Gravity;
import android.view.View;
import android.widget.Button;
import android.widget.EditText;
import android.widget.LinearLayout;
import android.widget.ScrollView;
import android.widget.TextView;
import android.widget.Toast;

import androidx.annotation.NonNull;

import com.google.firebase.auth.FirebaseAuth;
import com.google.firebase.auth.FirebaseUser;
import com.google.firebase.firestore.DocumentSnapshot;
import com.google.firebase.firestore.FirebaseFirestore;
import com.google.firebase.firestore.ListenerRegistration;
import com.google.firebase.firestore.Timestamp;

import java.util.ArrayList;
import java.util.Collections;
import java.util.Comparator;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

public class RideChatActivity extends Activity {

    private FirebaseAuth auth;
    private FirebaseFirestore db;

    private String rideId = "";
    private String currentUid = "";
    private String currentRole = "";

    private TextView statusText;
    private TextView rideInfoText;
    private LinearLayout messageContainer;
    private ScrollView scrollView;

    private EditText messageInput;
    private Button sendButton;
    private Button backButton;

    private ListenerRegistration rideListener;
    private ListenerRegistration messageListener;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);

        auth = FirebaseAuth.getInstance();
        db = FirebaseFirestore.getInstance();

        readRideId();

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

        buildInterface();

        if (rideId.isEmpty()) {
            statusText.setText("🔴 No ride selected.");
            disableChat();
            return;
        }

        listenToRide();
    }

    private void readRideId() {

        if (getIntent() == null) {
            return;
        }

        rideId = getIntent().getStringExtra("ride_id");

        if (rideId == null || rideId.trim().isEmpty()) {
            rideId = getIntent().getStringExtra("rideId");
        }

        if (rideId == null) {
            rideId = "";
        }

        rideId = rideId.trim();
    }

    private void buildInterface() {

        LinearLayout root = new LinearLayout(this);
        root.setOrientation(LinearLayout.VERTICAL);
        root.setBackgroundColor(Color.rgb(245, 248, 246));

        TextView header = new TextView(this);
        header.setText("💬 SAKAY NA CHAT");
        header.setTextSize(24);
        header.setTypeface(null, Typeface.BOLD);
        header.setTextColor(Color.WHITE);
        header.setGravity(Gravity.CENTER);
        header.setPadding(10, 20, 10, 20);
        header.setBackgroundColor(Color.rgb(0, 125, 75));

        root.addView(header, fullParams());

        rideInfoText = new TextView(this);
        rideInfoText.setText("Ride: " + rideId);
        rideInfoText.setTextSize(14);
        rideInfoText.setTypeface(null, Typeface.BOLD);
        rideInfoText.setTextColor(Color.rgb(30, 80, 55));
        rideInfoText.setGravity(Gravity.CENTER);
        rideInfoText.setPadding(10, 10, 10, 5);

        root.addView(rideInfoText, fullParams());

        statusText = new TextView(this);
        statusText.setText("🟠 Loading ride...");
        statusText.setTextSize(14);
        statusText.setTextColor(Color.DKGRAY);
        statusText.setGravity(Gravity.CENTER);
        statusText.setPadding(10, 5, 10, 10);

        root.addView(statusText, fullParams());

        scrollView = new ScrollView(this);

        messageContainer = new LinearLayout(this);
        messageContainer.setOrientation(LinearLayout.VERTICAL);
        messageContainer.setPadding(12, 12, 12, 12);

        scrollView.addView(
                messageContainer,
                new ScrollView.LayoutParams(-1, -2)
        );

        LinearLayout.LayoutParams scrollParams =
                new LinearLayout.LayoutParams(-1, 0, 1);

        root.addView(scrollView, scrollParams);

        LinearLayout inputRow = new LinearLayout(this);
        inputRow.setOrientation(LinearLayout.HORIZONTAL);
        inputRow.setGravity(Gravity.CENTER_VERTICAL);
        inputRow.setPadding(8, 6, 8, 6);

        messageInput = new EditText(this);
        messageInput.setHint("Type a message...");
        messageInput.setTextSize(16);
        messageInput.setSingleLine(false);
        messageInput.setInputType(
                InputType.TYPE_CLASS_TEXT |
                InputType.TYPE_TEXT_FLAG_CAP_SENTENCES |
                InputType.TYPE_TEXT_FLAG_MULTI_LINE
        );

        LinearLayout.LayoutParams inputParams =
                new LinearLayout.LayoutParams(0, -2, 1);

        inputParams.setMargins(0, 0, 8, 0);

        inputRow.addView(
                messageInput,
                inputParams
        );

        sendButton = makeButton("SEND");

        LinearLayout.LayoutParams sendParams =
                new LinearLayout.LayoutParams(105, -2);

        inputRow.addView(
                sendButton,
                sendParams
        );

        sendButton.setOnClickListener(
                v -> sendMessage()
        );

        root.addView(inputRow);

        backButton = makeButton("← BACK");

        backButton.setOnClickListener(
                v -> finish()
        );

        LinearLayout.LayoutParams backParams =
                new LinearLayout.LayoutParams(-1, -2);

        backParams.setMargins(8, 3, 8, 8);

        root.addView(backButton, backParams);

        setContentView(root);

        disableChat();
    }

    private Button makeButton(String text) {

        Button button = new Button(this);

        button.setText(text);
        button.setTextSize(15);
        button.setTypeface(null, Typeface.BOLD);
        button.setTextColor(Color.WHITE);
        button.setAllCaps(false);

        button.setBackgroundColor(
                Color.rgb(0, 125, 75)
        );

        return button;
    }

    private LinearLayout.LayoutParams fullParams() {

        return new LinearLayout.LayoutParams(-1, -2);
    }

    private void listenToRide() {

        if (rideListener != null) {
            rideListener.remove();
        }

        rideListener =
                db.collection("rides")
                        .document(rideId)
                        .addSnapshotListener(
                                (snapshot, error) -> {

            if (error != null) {

                statusText.setText(
                        "🔴 Ride error:\n" +
                        error.getMessage()
                );

                disableChat();
                return;
            }

            if (snapshot == null ||
                    !snapshot.exists()) {

                statusText.setText(
                        "🔴 Ride not found."
                );

                disableChat();
                return;
            }

            String passengerId =
                    snapshot.getString("passengerId");

            String driverId =
                    snapshot.getString("driverId");

            String status =
                    snapshot.getString("status");

            boolean isPassenger =
                    currentUid.equals(passengerId);

            boolean isDriver =
                    currentUid.equals(driverId);

            if (!isPassenger && !isDriver) {

                statusText.setText(
                        "🔴 You are not a member of this ride."
                );

                disableChat();
                return;
            }

            currentRole =
                    isPassenger
                            ? "PASSENGER"
                            : "DRIVER";

            String pickupName =
                    firstNonEmpty(
                            snapshot.getString("pickupName"),
                            snapshot.getString("pickup")
                    );

            String destinationName =
                    firstNonEmpty(
                            snapshot.getString("destinationName"),
                            snapshot.getString("destination")
                    );

            rideInfoText.setText(
                    "🛺 " +
                    pickupName +
                    "\n→ " +
                    destinationName
            );

            if (driverId == null ||
                    driverId.trim().isEmpty()) {

                statusText.setText(
                        "🟠 WAITING FOR DRIVER\n" +
                        "Chat opens after driver acceptance."
                );

                disableChat();
                stopMessageListener();
                return;
            }

            if (status == null ||
                    status.trim().isEmpty()) {

                status = "ACTIVE";
            }

            statusText.setText(
                    "🟢 CHAT ACTIVE • " + status
            );

            enableChat();

            startMessageListener();
        });
    }

    private String firstNonEmpty(
            String first,
            String second) {

        if (first != null &&
                !first.trim().isEmpty()) {

            return first;
        }

        if (second != null &&
                !second.trim().isEmpty()) {

            return second;
        }

        return "Unknown location";
    }

    private void enableChat() {

        if (messageInput != null) {
            messageInput.setEnabled(true);
        }

        if (sendButton != null) {
            sendButton.setEnabled(true);
        }
    }

    private void disableChat() {

        if (messageInput != null) {
            messageInput.setEnabled(false);
        }

        if (sendButton != null) {
            sendButton.setEnabled(false);
        }
    }

    private void startMessageListener() {

        if (messageListener != null) {
            return;
        }

        messageListener =
                db.collection("rides")
                        .document(rideId)
                        .collection("messages")
                        .addSnapshotListener(
                                (snapshots, error) -> {

            if (error != null) {

                statusText.setText(
                        "🔴 Chat error:\n" +
                        error.getMessage()
                );

                return;
            }

            messageContainer.removeAllViews();

            if (snapshots == null ||
                    snapshots.isEmpty()) {

                showEmptyMessages();
                return;
            }

            List<DocumentSnapshot> messages =
                    new ArrayList<>(
                            snapshots.getDocuments()
                    );

            Collections.sort(
                    messages,
                    new Comparator<DocumentSnapshot>() {

                @Override
                public int compare(
                        DocumentSnapshot a,
                        DocumentSnapshot b) {

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
            });

            for (DocumentSnapshot message : messages) {
                addMessage(message);
            }

            scrollToBottom();
        });
    }

    private void stopMessageListener() {

        if (messageListener != null) {
            messageListener.remove();
            messageListener = null;
        }
    }

    private void showEmptyMessages() {

        TextView empty = new TextView(this);

        empty.setText(
                "No messages yet.\n" +
                "You can start the conversation."
        );

        empty.setTextSize(15);
        empty.setTextColor(Color.GRAY);
        empty.setGravity(Gravity.CENTER);
        empty.setPadding(10, 30, 10, 30);

        messageContainer.addView(empty);
    }

    private void addMessage(
            DocumentSnapshot message) {

        String senderId =
                message.getString("senderId");

        String senderRole =
                message.getString("senderRole");

        String text =
                message.getString("message");

        if (text == null) {
            text = "";
        }

        boolean mine =
                currentUid.equals(senderId);

        TextView bubble =
                new TextView(this);

        String prefix;

        if (mine) {
            prefix = "YOU";
        } else if (
                "DRIVER".equalsIgnoreCase(
                        senderRole)) {

            prefix = "DRIVER";
        } else {
            prefix = "PASSENGER";
        }

        bubble.setText(
                prefix +
                "\n" +
                text
        );

        bubble.setTextSize(16);
        bubble.setTextColor(Color.DKGRAY);
        bubble.setPadding(18, 12, 18, 12);

        if (mine) {
            bubble.setGravity(Gravity.END);
        } else {
            bubble.setGravity(Gravity.START);
        }

        LinearLayout.LayoutParams params =
                new LinearLayout.LayoutParams(
                        -1,
                        -2
                );

        params.setMargins(4, 4, 4, 4);

        messageContainer.addView(
                bubble,
                params
        );
    }

    private void sendMessage() {

        FirebaseUser user =
                auth.getCurrentUser();

        if (user == null) {

            Toast.makeText(
                    this,
                    "Please login again.",
                    Toast.LENGTH_SHORT
            ).show();

            return;
        }

        if (rideId.isEmpty()) {

            Toast.makeText(
                    this,
                    "No ride selected.",
                    Toast.LENGTH_SHORT
            ).show();

            return;
        }

        String text =
                messageInput.getText()
                        .toString()
                        .trim();

        if (text.isEmpty()) {

            Toast.makeText(
                    this,
                    "Type a message first.",
                    Toast.LENGTH_SHORT
            ).show();

            return;
        }

        sendButton.setEnabled(false);

        Map<String, Object> data =
                new HashMap<>();

        data.put(
                "senderId",
                user.getUid()
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
                .addOnSuccessListener(
                        documentReference -> {

            messageInput.setText("");

            sendButton.setEnabled(true);

            scrollToBottom();
        })
                .addOnFailureListener(
                        e -> {

            sendButton.setEnabled(true);

            Toast.makeText(
                    this,
                    "Message failed:\n" +
                    e.getMessage(),
                    Toast.LENGTH_LONG
            ).show();
        });
    }

    private void scrollToBottom() {

        if (scrollView == null) {
            return;
        }

        scrollView.post(
                () -> scrollView.fullScroll(
                        View.FOCUS_DOWN
                )
        );
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

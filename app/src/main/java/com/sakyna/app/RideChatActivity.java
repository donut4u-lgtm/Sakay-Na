
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

    private LinearLayout messageContainer;
    private EditText messageInput;
    private Button sendButton;
    private TextView titleText;
    private TextView statusText;
    private ScrollView scrollView;

    private ListenerRegistration rideListener;
    private ListenerRegistration messageListener;

    private String rideId = "";
    private String passengerId = "";
    private String driverId = "";
    private String myRole = "";

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);

        auth = FirebaseAuth.getInstance();
        db = FirebaseFirestore.getInstance();

        rideId = getIntent().getStringExtra("ride_id");

        if (rideId == null || rideId.trim().isEmpty()) {
            rideId = getIntent().getStringExtra("rideId");
        }

        if (rideId == null || rideId.trim().isEmpty()) {
            Toast.makeText(this, "Ride ID not found.", Toast.LENGTH_LONG).show();
            finish();
            return;
        }

        FirebaseUser user = auth.getCurrentUser();

        if (user == null) {
            Toast.makeText(this, "Please login again.", Toast.LENGTH_LONG).show();
            finish();
            return;
        }

        buildScreen();
        listenToRide();
    }

    private void buildScreen() {
        LinearLayout root = new LinearLayout(this);
        root.setOrientation(LinearLayout.VERTICAL);
        root.setBackgroundColor(Color.WHITE);

        titleText = new TextView(this);
        titleText.setText("💬 RIDE CHAT");
        titleText.setTextSize(24);
        titleText.setTypeface(null, Typeface.BOLD);
        titleText.setTextColor(Color.rgb(0, 110, 80));
        titleText.setGravity(Gravity.CENTER);
        titleText.setPadding(16, 20, 16, 12);
        root.addView(titleText);

        statusText = new TextView(this);
        statusText.setText("Loading ride...");
        statusText.setTextSize(15);
        statusText.setTextColor(Color.DKGRAY);
        statusText.setGravity(Gravity.CENTER);
        statusText.setPadding(12, 5, 12, 12);
        root.addView(statusText);

        scrollView = new ScrollView(this);

        messageContainer = new LinearLayout(this);
        messageContainer.setOrientation(LinearLayout.VERTICAL);
        messageContainer.setPadding(12, 12, 12, 12);

        scrollView.addView(messageContainer);

        root.addView(
                scrollView,
                new LinearLayout.LayoutParams(
                        LinearLayout.LayoutParams.MATCH_PARENT,
                        0,
                        1
                )
        );

        LinearLayout inputRow = new LinearLayout(this);
        inputRow.setOrientation(LinearLayout.HORIZONTAL);
        inputRow.setPadding(8, 8, 8, 8);
        inputRow.setGravity(Gravity.CENTER_VERTICAL);

        messageInput = new EditText(this);
        messageInput.setHint("Type a message...");
        messageInput.setTextSize(16);
        messageInput.setSingleLine(false);
        messageInput.setMaxLines(3);
        messageInput.setInputType(
                InputType.TYPE_CLASS_TEXT |
                InputType.TYPE_TEXT_FLAG_CAP_SENTENCES |
                InputType.TYPE_TEXT_FLAG_MULTI_LINE
        );

        inputRow.addView(
                messageInput,
                new LinearLayout.LayoutParams(
                        0,
                        LinearLayout.LayoutParams.WRAP_CONTENT,
                        1
                )
        );

        sendButton = new Button(this);
        sendButton.setText("SEND");
        sendButton.setTextSize(14);
        sendButton.setEnabled(false);
        sendButton.setOnClickListener(v -> sendMessage());

        inputRow.addView(
                sendButton,
                new LinearLayout.LayoutParams(
                        LinearLayout.LayoutParams.WRAP_CONTENT,
                        LinearLayout.LayoutParams.WRAP_CONTENT
                )
        );

        root.addView(inputRow);

        Button backButton = new Button(this);
        backButton.setText("BACK");
        backButton.setOnClickListener(v -> finish());
        root.addView(backButton);

        setContentView(root);
    }

    private void listenToRide() {
        if (rideListener != null) {
            rideListener.remove();
        }

        FirebaseUser user = auth.getCurrentUser();

        if (user == null) {
            finish();
            return;
        }

        final String uid = user.getUid();

        rideListener = db.collection("rides")
                .document(rideId)
                .addSnapshotListener((snapshot, error) -> {

                    if (error != null) {
                        statusText.setText(
                                "Unable to load ride:\n" + safeError(error)
                        );
                        sendButton.setEnabled(false);
                        return;
                    }

                    if (snapshot == null || !snapshot.exists()) {
                        statusText.setText("Ride not found.");
                        sendButton.setEnabled(false);
                        return;
                    }

                    passengerId = value(snapshot, "passengerId");
                    driverId = value(snapshot, "driverId");

                    if (uid.equals(passengerId)) {
                        myRole = "PASSENGER";
                    } else if (uid.equals(driverId)) {
                        myRole = "DRIVER";
                    } else {
                        myRole = "";
                    }

                    if (myRole.isEmpty()) {
                        statusText.setText(
                                "You are not a participant in this ride."
                        );
                        sendButton.setEnabled(false);
                        stopMessageListener();
                        return;
                    }

                    String status = value(snapshot, "status");

                    if (status.isEmpty()) {
                        status = "REQUESTED";
                    }

                    if (driverId.isEmpty()) {
                        statusText.setText(
                                "Ride: " + status +
                                "\nWaiting for driver..."
                        );

                        sendButton.setEnabled(false);
                        stopMessageListener();

                        messageContainer.removeAllViews();

                        addSystemMessage(
                                "Chat will be available after a driver accepts the ride."
                        );

                    } else {
                        statusText.setText(
                                "Ride: " + status +
                                "\nChat with " +
                                ("PASSENGER".equals(myRole)
                                        ? "Driver"
                                        : "Passenger")
                        );

                        sendButton.setEnabled(true);
                        startMessageListener();
                    }
                });
    }

    private void startMessageListener() {
        if (messageListener != null) {
            return;
        }

        messageListener = db.collection("rides")
                .document(rideId)
                .collection("messages")
                .addSnapshotListener((snapshots, error) -> {

                    if (error != null) {
                        statusText.setText(
                                "Chat error:\n" + safeError(error)
                        );
                        return;
                    }

                    if (snapshots == null) {
                        return;
                    }

                    List<DocumentSnapshot> messages =
                            new ArrayList<>(snapshots.getDocuments());

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

                                    return ta.compareTo(tb);
                                }
                            }
                    );

                    messageContainer.removeAllViews();

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

    private void addMessage(DocumentSnapshot document) {
        String senderId = value(document, "senderId");
        String message = value(document, "message");
        String senderRole = value(document, "senderRole");

        if (message.isEmpty()) {
            return;
        }

        FirebaseUser user = auth.getCurrentUser();

        boolean mine =
                user != null &&
                user.getUid().equals(senderId);

        LinearLayout row = new LinearLayout(this);
        row.setOrientation(LinearLayout.VERTICAL);

        if (mine) {
            row.setGravity(Gravity.RIGHT);
        } else {
            row.setGravity(Gravity.LEFT);
        }

        TextView bubble = new TextView(this);

        String who;

        if (mine) {
            who = "You";
        } else if ("DRIVER".equalsIgnoreCase(senderRole)) {
            who = "Driver";
        } else if ("PASSENGER".equalsIgnoreCase(senderRole)) {
            who = "Passenger";
        } else {
            who = "User";
        }

        bubble.setText(who + "\n" + message);
        bubble.setTextSize(16);
        bubble.setTextColor(Color.DKGRAY);
        bubble.setPadding(18, 12, 18, 12);

        if (mine) {
            bubble.setBackgroundColor(
                    Color.rgb(220, 245, 232)
            );
        } else {
            bubble.setBackgroundColor(
                    Color.rgb(238, 238, 238)
            );
        }

        LinearLayout.LayoutParams bubbleParams =
                new LinearLayout.LayoutParams(
                        LinearLayout.LayoutParams.WRAP_CONTENT,
                        LinearLayout.LayoutParams.WRAP_CONTENT
                );

        bubbleParams.setMargins(8, 6, 8, 6);

        row.addView(bubble, bubbleParams);

        messageContainer.addView(row);
    }

    private void addSystemMessage(String text) {
        TextView t = new TextView(this);
        t.setText(text);
        t.setTextSize(15);
        t.setTextColor(Color.GRAY);
        t.setGravity(Gravity.CENTER);
        t.setPadding(20, 30, 20, 30);

        messageContainer.addView(t);
    }

    private void sendMessage() {
        FirebaseUser user = auth.getCurrentUser();

        if (user == null) {
            Toast.makeText(
                    this,
                    "Please login again.",
                    Toast.LENGTH_SHORT
            ).show();
            return;
        }

        if (driverId.isEmpty()) {
            Toast.makeText(
                    this,
                    "Waiting for driver.",
                    Toast.LENGTH_SHORT
            ).show();
            return;
        }

        if (myRole.isEmpty()) {
            Toast.makeText(
                    this,
                    "You cannot send messages in this ride.",
                    Toast.LENGTH_SHORT
            ).show();
            return;
        }

        String text = messageInput.getText()
                .toString()
                .trim();

        if (text.isEmpty()) {
            return;
        }

        sendButton.setEnabled(false);

        Map<String, Object> data =
                new HashMap<>();

        data.put("senderId", user.getUid());
        data.put("senderRole", myRole);
        data.put("message", text);
        data.put("createdAt", Timestamp.now());

        db.collection("rides")
                .document(rideId)
                .collection("messages")
                .add(data)
                .addOnSuccessListener(documentReference -> {
                    messageInput.setText("");
                    sendButton.setEnabled(true);
                    scrollToBottom();
                })
                .addOnFailureListener(e -> {
                    sendButton.setEnabled(true);

                    Toast.makeText(
                            this,
                            "Message failed: " + safeError(e),
                            Toast.LENGTH_LONG
                    ).show();
                });
    }

    private String value(
            DocumentSnapshot document,
            String field) {

        String value = document.getString(field);

        return value == null ? "" : value.trim();
    }

    private String safeError(Exception e) {
        if (e == null ||
                e.getMessage() == null ||
                e.getMessage().trim().isEmpty()) {
            return "Unknown error.";
        }

        return e.getMessage();
    }

    private void scrollToBottom() {
        if (scrollView == null) {
            return;
        }

        scrollView.post(() ->
                scrollView.fullScroll(View.FOCUS_DOWN)
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

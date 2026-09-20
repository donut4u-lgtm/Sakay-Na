
package com.sakyna.app;

import android.app.Activity;
import android.graphics.Color;
import android.os.Bundle;
import android.view.Gravity;
import android.view.View;
import android.widget.Button;
import android.widget.EditText;
import android.widget.LinearLayout;
import android.widget.ScrollView;
import android.widget.TextView;
import android.widget.Toast;

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

public class RideChatActivity extends Activity {

    private FirebaseAuth auth;
    private FirebaseFirestore db;

    private LinearLayout messagesLayout;
    private EditText messageInput;
    private Button sendButton;
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

        if (rideId == null) {
            rideId = "";
        }

        rideId = rideId.trim();

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

        if (rideId.isEmpty()) {
            Toast.makeText(
                    this,
                    "Ride ID is missing.",
                    Toast.LENGTH_LONG
            ).show();

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

        TextView title = new TextView(this);
        title.setText("💬 RIDE CHAT");
        title.setTextSize(25);
        title.setGravity(Gravity.CENTER);
        title.setTextColor(Color.rgb(0, 120, 80));
        title.setPadding(10, 22, 10, 15);

        root.addView(title);

        statusText = new TextView(this);
        statusText.setText("Connecting to ride...");
        statusText.setTextSize(15);
        statusText.setGravity(Gravity.CENTER);
        statusText.setTextColor(Color.DKGRAY);
        statusText.setPadding(10, 5, 10, 12);

        root.addView(statusText);

        scrollView = new ScrollView(this);

        messagesLayout = new LinearLayout(this);
        messagesLayout.setOrientation(LinearLayout.VERTICAL);
        messagesLayout.setPadding(10, 10, 10, 10);

        scrollView.addView(messagesLayout);

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
        bottom.setPadding(8, 8, 8, 8);

        messageInput = new EditText(this);
        messageInput.setHint("Type a message...");
        messageInput.setTextSize(16);
        messageInput.setSingleLine(true);

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
        sendButton.setEnabled(false);
        sendButton.setOnClickListener(v -> sendMessage());

        bottom.addView(sendButton);

        root.addView(bottom);

        Button backButton = new Button(this);
        backButton.setText("BACK");
        backButton.setOnClickListener(v -> finish());

        root.addView(backButton);

        setContentView(root);
    }

    private void listenToRide() {

        FirebaseUser user = auth.getCurrentUser();

        if (user == null) {
            finish();
            return;
        }

        stopRideListener();

        rideListener =
                db.collection("rides")
                        .document(rideId)
                        .addSnapshotListener(
                                (snapshot, error) -> {

                                    if (error != null) {

                                        statusText.setText(
                                                "❌ Ride error:\n"
                                                        + error.getMessage()
                                        );

                                        sendButton.setEnabled(false);
                                        return;
                                    }

                                    if (snapshot == null
                                            || !snapshot.exists()) {

                                        statusText.setText(
                                                "❌ Ride not found."
                                        );

                                        sendButton.setEnabled(false);
                                        return;
                                    }

                                    passengerId =
                                            getText(
                                                    snapshot,
                                                    "passengerId"
                                            );

                                    driverId =
                                            getText(
                                                    snapshot,
                                                    "driverId"
                                            );

                                    String uid =
                                            user.getUid();

                                    if (uid.equals(passengerId)) {

                                        myRole = "PASSENGER";

                                    } else if (uid.equals(driverId)) {

                                        myRole = "DRIVER";

                                    } else {

                                        myRole = "";
                                    }

                                    if (myRole.isEmpty()) {

                                        statusText.setText(
                                                "❌ You are not part of this ride."
                                        );

                                        sendButton.setEnabled(false);
                                        stopMessageListener();
                                        return;
                                    }

                                    String rideStatus =
                                            getText(
                                                    snapshot,
                                                    "status"
                                            );

                                    if (driverId.isEmpty()) {

                                        statusText.setText(
                                                "Ride: "
                                                        + rideStatus
                                                        + "\n"
                                                        + "Waiting for driver..."
                                        );

                                        sendButton.setEnabled(false);

                                        stopMessageListener();

                                        messagesLayout.removeAllViews();

                                        addSystemMessage(
                                                "💬 Chat will become available after a driver accepts the ride."
                                        );

                                        return;
                                    }

                                    statusText.setText(
                                            "🟢 CHAT CONNECTED\n"
                                                    + "Ride: "
                                                    + rideStatus
                                                    + "\n"
                                                    + "Chatting with "
                                                    + (
                                                    "PASSENGER".equals(myRole)
                                                            ? "Driver"
                                                            : "Passenger"
                                            )
                                    );

                                    sendButton.setEnabled(true);

                                    startMessageListener();
                                }
                        );
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
                                (snapshot, error) -> {

                                    if (error != null) {

                                        statusText.setText(
                                                "❌ CHAT ERROR\n"
                                                        + error.getMessage()
                                        );

                                        sendButton.setEnabled(false);
                                        return;
                                    }

                                    if (snapshot == null) {
                                        return;
                                    }

                                    List<DocumentSnapshot> messageList =
                                            new ArrayList<>(
                                                    snapshot.getDocuments()
                                            );

                                    Collections.sort(
                                            messageList,
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

                                                    if (ta == null
                                                            && tb == null) {
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

                                    messagesLayout.removeAllViews();

                                    if (messageList.isEmpty()) {

                                        addSystemMessage(
                                                "No messages yet. Say hello! 👋"
                                        );

                                        return;
                                    }

                                    FirebaseUser currentUser =
                                            auth.getCurrentUser();

                                    for (DocumentSnapshot doc
                                            : messageList) {

                                        String message =
                                                getText(
                                                        doc,
                                                        "message"
                                                );

                                        if (message.isEmpty()) {
                                            continue;
                                        }

                                        String senderId =
                                                getText(
                                                        doc,
                                                        "senderId"
                                                );

                                        String senderRole =
                                                getText(
                                                        doc,
                                                        "senderRole"
                                                );

                                        boolean mine =
                                                currentUser != null
                                                        && currentUser
                                                        .getUid()
                                                        .equals(senderId);

                                        String who;

                                        if (mine) {

                                            who = "You";

                                        } else if (
                                                "DRIVER".equalsIgnoreCase(
                                                        senderRole
                                                )) {

                                            who = "Driver";

                                        } else if (
                                                "PASSENGER".equalsIgnoreCase(
                                                        senderRole
                                                )) {

                                            who = "Passenger";

                                        } else {

                                            who = "Other";
                                        }

                                        addMessage(
                                                who,
                                                message,
                                                mine
                                        );
                                    }

                                    scrollView.post(() ->
                                            scrollView.fullScroll(
                                                    View.FOCUS_DOWN
                                            )
                                    );
                                }
                        );
    }

    private void sendMessage() {

        FirebaseUser user =
                auth.getCurrentUser();

        if (user == null) {
            return;
        }

        if (rideId.isEmpty()) {

            Toast.makeText(
                    this,
                    "Ride ID is missing.",
                    Toast.LENGTH_SHORT
            ).show();

            return;
        }

        if (passengerId.isEmpty()
                || driverId.isEmpty()) {

            Toast.makeText(
                    this,
                    "Waiting for both passenger and driver.",
                    Toast.LENGTH_SHORT
            ).show();

            return;
        }

        if (myRole.isEmpty()) {
            return;
        }

        String message =
                messageInput
                        .getText()
                        .toString()
                        .trim();

        if (message.isEmpty()) {
            return;
        }

        sendButton.setEnabled(false);

        String messageId =
                db.collection("rides")
                        .document(rideId)
                        .collection("messages")
                        .document()
                        .getId();

        Map<String, Object> data =
                new HashMap<>();

        data.put(
                "senderId",
                user.getUid()
        );

        data.put(
                "senderRole",
                myRole
        );

        data.put(
                "message",
                message
        );

        data.put(
                "createdAt",
                Timestamp.now()
        );

        data.put(
                "messageId",
                messageId
        );

        db.collection("rides")
                .document(rideId)
                .collection("messages")
                .document(messageId)
                .set(data)
                .addOnSuccessListener(v -> {

                    messageInput.setText("");

                    sendButton.setEnabled(true);

                    statusText.setText(
                            "🟢 MESSAGE SENT"
                                    + "\n"
                                    + "Chatting with "
                                    + (
                                    "PASSENGER".equals(myRole)
                                            ? "Driver"
                                            : "Passenger"
                            )
                    );
                })
                .addOnFailureListener(e -> {

                    sendButton.setEnabled(true);

                    statusText.setText(
                            "❌ MESSAGE FAILED\n"
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

    private void addMessage(
            String who,
            String message,
            boolean mine
    ) {

        TextView item = new TextView(this);

        item.setText(
                who
                        + "\n"
                        + message
        );

        item.setTextSize(16);
        item.setTextColor(Color.DKGRAY);
        item.setPadding(18, 14, 18, 14);

        if (mine) {

            item.setGravity(Gravity.RIGHT);

            item.setBackgroundColor(
                    Color.rgb(220, 245, 232)
            );

        } else {

            item.setGravity(Gravity.LEFT);

            item.setBackgroundColor(
                    Color.rgb(238, 238, 238)
            );
        }

        LinearLayout.LayoutParams params =
                new LinearLayout.LayoutParams(
                        LinearLayout.LayoutParams.MATCH_PARENT,
                        LinearLayout.LayoutParams.WRAP_CONTENT
                );

        params.setMargins(
                8,
                5,
                8,
                5
        );

        messagesLayout.addView(
                item,
                params
        );
    }

    private void addSystemMessage(
            String message
    ) {

        TextView item = new TextView(this);

        item.setText(message);
        item.setTextSize(15);
        item.setTextColor(Color.GRAY);
        item.setGravity(Gravity.CENTER);
        item.setPadding(20, 30, 20, 30);

        messagesLayout.addView(item);
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

    private void stopRideListener() {

        if (rideListener != null) {

            rideListener.remove();
            rideListener = null;
        }
    }

    private void stopMessageListener() {

        if (messageListener != null) {

            messageListener.remove();
            messageListener = null;
        }
    }

    @Override
    protected void onDestroy() {

        stopRideListener();
        stopMessageListener();

        super.onDestroy();
    }
}

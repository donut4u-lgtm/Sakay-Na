
package com.sakyna.app;

import android.app.Activity;
import android.graphics.Color;
import android.os.Bundle;
import android.view.Gravity;
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

import java.util.HashMap;
import java.util.Map;

public class RideChatActivity extends Activity {

    private FirebaseAuth auth;
    private FirebaseFirestore db;

    private LinearLayout messages;
    private EditText input;
    private Button sendButton;
    private TextView status;
    private ScrollView scroll;

    private ListenerRegistration rideListener;
    private ListenerRegistration messageListener;

    private String rideId = "";
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

        FirebaseUser user = auth.getCurrentUser();

        if (user == null || rideId == null || rideId.trim().isEmpty()) {
            Toast.makeText(this, "Ride chat unavailable.", Toast.LENGTH_LONG).show();
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
        title.setTextSize(24);
        title.setGravity(Gravity.CENTER);
        title.setTextColor(Color.rgb(0, 110, 80));
        title.setPadding(10, 20, 10, 15);
        root.addView(title);

        status = new TextView(this);
        status.setText("Loading ride...");
        status.setTextSize(15);
        status.setGravity(Gravity.CENTER);
        status.setTextColor(Color.DKGRAY);
        status.setPadding(10, 5, 10, 10);
        root.addView(status);

        scroll = new ScrollView(this);

        messages = new LinearLayout(this);
        messages.setOrientation(LinearLayout.VERTICAL);
        messages.setPadding(10, 10, 10, 10);

        scroll.addView(messages);

        root.addView(
                scroll,
                new LinearLayout.LayoutParams(
                        LinearLayout.LayoutParams.MATCH_PARENT,
                        0,
                        1
                )
        );

        LinearLayout bottom = new LinearLayout(this);
        bottom.setOrientation(LinearLayout.HORIZONTAL);
        bottom.setPadding(8, 8, 8, 8);

        input = new EditText(this);
        input.setHint("Type a message");
        input.setTextSize(16);
        input.setSingleLine(true);

        bottom.addView(
                input,
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

        Button back = new Button(this);
        back.setText("BACK");
        back.setOnClickListener(v -> finish());
        root.addView(back);

        setContentView(root);
    }

    private void listenToRide() {
        FirebaseUser user = auth.getCurrentUser();

        if (user == null) {
            finish();
            return;
        }

        String uid = user.getUid();

        rideListener = db.collection("rides")
                .document(rideId)
                .addSnapshotListener((snapshot, error) -> {

                    if (error != null) {
                        status.setText("Ride error: " + error.getMessage());
                        sendButton.setEnabled(false);
                        return;
                    }

                    if (snapshot == null || !snapshot.exists()) {
                        status.setText("Ride not found.");
                        sendButton.setEnabled(false);
                        return;
                    }

                    String passengerId = text(snapshot, "passengerId");
                    driverId = text(snapshot, "driverId");

                    if (uid.equals(passengerId)) {
                        myRole = "PASSENGER";
                    } else if (uid.equals(driverId)) {
                        myRole = "DRIVER";
                    } else {
                        myRole = "";
                    }

                    if (myRole.isEmpty()) {
                        status.setText("You are not part of this ride.");
                        sendButton.setEnabled(false);
                        return;
                    }

                    String rideStatus = text(snapshot, "status");

                    if (driverId.isEmpty()) {
                        status.setText(
                                "Ride: " + rideStatus +
                                "\nWaiting for driver..."
                        );
                        sendButton.setEnabled(false);
                        messages.removeAllViews();
                        addSystem("Chat opens after driver accepts.");
                        return;
                    }

                    status.setText(
                            "Ride: " + rideStatus +
                            "\nChat with " +
                            ("PASSENGER".equals(myRole)
                                    ? "Driver"
                                    : "Passenger")
                    );

                    sendButton.setEnabled(true);
                    startMessageListener();
                });
    }

    private void startMessageListener() {
        if (messageListener != null) {
            return;
        }

        messageListener = db.collection("rides")
                .document(rideId)
                .collection("messages")
                .addSnapshotListener((snapshot, error) -> {

                    if (error != null) {
                        status.setText("Chat error: " + error.getMessage());
                        return;
                    }

                    if (snapshot == null) {
                        return;
                    }

                    messages.removeAllViews();

                    for (DocumentSnapshot doc : snapshot.getDocuments()) {
                        String message = text(doc, "message");

                        if (message.isEmpty()) {
                            continue;
                        }

                        String senderId = text(doc, "senderId");

                        FirebaseUser user = auth.getCurrentUser();

                        boolean mine =
                                user != null &&
                                user.getUid().equals(senderId);

                        TextView item = new TextView(this);

                        item.setText(
                                (mine ? "You" : "Other") +
                                "\n" +
                                message
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

                        LinearLayout.LayoutParams p =
                                new LinearLayout.LayoutParams(
                                        LinearLayout.LayoutParams.WRAP_CONTENT,
                                        LinearLayout.LayoutParams.WRAP_CONTENT
                                );

                        p.setMargins(8, 6, 8, 6);

                        messages.addView(item, p);
                    }

                    scroll.post(() ->
                            scroll.fullScroll(ScrollView.FOCUS_DOWN)
                    );
                });
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

        String message = input.getText().toString().trim();

        if (message.isEmpty()) {
            return;
        }

        sendButton.setEnabled(false);

        Map<String, Object> data = new HashMap<>();

        data.put("senderId", user.getUid());
        data.put("senderRole", myRole);
        data.put("message", message);
        data.put("createdAt", Timestamp.now());

        db.collection("rides")
                .document(rideId)
                .collection("messages")
                .add(data)
                .addOnSuccessListener(v -> {
                    input.setText("");
                    sendButton.setEnabled(true);
                })
                .addOnFailureListener(e -> {
                    sendButton.setEnabled(true);

                    Toast.makeText(
                            this,
                            "Message failed: " + e.getMessage(),
                            Toast.LENGTH_LONG
                    ).show();
                });
    }

    private String text(DocumentSnapshot doc, String field) {
        String value = doc.getString(field);
        return value == null ? "" : value.trim();
    }

    private void addSystem(String message) {
        TextView item = new TextView(this);
        item.setText(message);
        item.setTextSize(15);
        item.setTextColor(Color.GRAY);
        item.setGravity(Gravity.CENTER);
        item.setPadding(20, 30, 20, 30);
        messages.addView(item);
    }

    @Override
    protected void onDestroy() {
        if (rideListener != null) {
            rideListener.remove();
        }

        if (messageListener != null) {
            messageListener.remove();
        }

        super.onDestroy();
    }
}

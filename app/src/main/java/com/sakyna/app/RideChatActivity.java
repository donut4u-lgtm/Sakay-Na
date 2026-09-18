
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

    private String rideId = "";
    private String passengerId = "";
    private String driverId = "";

    private LinearLayout messagesLayout;
    private EditText messageInput;
    private ScrollView scrollView;
    private Button sendButton;

    private ListenerRegistration messageListener;

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

        buildScreen();

        if (rideId.trim().isEmpty()) {
            showError("Chat error: Ride ID is missing.");
            return;
        }

        verifyRide();
    }

    private void buildScreen() {

        LinearLayout root = new LinearLayout(this);
        root.setOrientation(LinearLayout.VERTICAL);
        root.setBackgroundColor(Color.WHITE);

        TextView title = new TextView(this);
        title.setText("Sakay Na Ride Chat");
        title.setTextSize(23);
        title.setTextColor(Color.BLACK);
        title.setGravity(Gravity.CENTER);
        title.setPadding(16, 20, 16, 20);

        root.addView(title);

        scrollView = new ScrollView(this);

        messagesLayout = new LinearLayout(this);
        messagesLayout.setOrientation(LinearLayout.VERTICAL);
        messagesLayout.setPadding(16, 16, 16, 16);

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
        bottom.setPadding(12, 12, 12, 12);

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

        bottom.addView(
                sendButton,
                new LinearLayout.LayoutParams(
                        LinearLayout.LayoutParams.WRAP_CONTENT,
                        LinearLayout.LayoutParams.WRAP_CONTENT
                )
        );

        sendButton.setOnClickListener(v -> sendMessage());

        root.addView(bottom);

        setContentView(root);
    }

    private void verifyRide() {

        FirebaseUser user = auth.getCurrentUser();

        if (user == null) {
            showError("Chat error: You are not logged in.");
            return;
        }

        String uid = user.getUid();

        db.collection("rides")
                .document(rideId)
                .get()
                .addOnSuccessListener(snapshot -> {

                    if (!snapshot.exists()) {
                        showError("Chat error: Ride not found.");
                        return;
                    }

                    passengerId = snapshot.getString("passengerId");
                    driverId = snapshot.getString("driverId");

                    boolean isPassenger =
                            passengerId != null &&
                            passengerId.equals(uid);

                    boolean isDriver =
                            driverId != null &&
                            driverId.equals(uid);

                    if (!isPassenger && !isDriver) {
                        showError(
                                "Chat error: You are not a participant."
                        );
                        return;
                    }

                    sendButton.setEnabled(true);

                    startMessageListener();
                })
                .addOnFailureListener(error -> {

                    showError(
                            "Ride read failed:\n"
                                    + firebaseError(error)
                    );
                });
    }

    private void startMessageListener() {

        if (messageListener != null) {
            messageListener.remove();
        }

        messageListener =
                db.collection("rides")
                        .document(rideId)
                        .collection("messages")
                        .addSnapshotListener(
                                (snapshot, error) -> {

                                    if (error != null) {
                                        showError(
                                                "CHAT READ ERROR:\n"
                                                        + firebaseError(error)
                                        );
                                        return;
                                    }

                                    if (snapshot == null) {
                                        return;
                                    }

                                    List<DocumentSnapshot> list =
                                            new ArrayList<>(
                                                    snapshot.getDocuments()
                                            );

                                    Collections.sort(
                                            list,
                                            new Comparator<DocumentSnapshot>() {
                                                @Override
                                                public int compare(
                                                        DocumentSnapshot a,
                                                        DocumentSnapshot b) {

                                                    Timestamp ta =
                                                            a.getTimestamp(
                                                                    "createdAt"
                                                            );

                                                    Timestamp tb =
                                                            b.getTimestamp(
                                                                    "createdAt"
                                                            );

                                                    if (ta == null &&
                                                            tb == null) {
                                                        return 0;
                                                    }

                                                    if (ta == null) {
                                                        return 1;
                                                    }

                                                    if (tb == null) {
                                                        return -1;
                                                    }

                                                    return ta.compareTo(tb);
                                                }
                                            }
                                    );

                                    messagesLayout.removeAllViews();

                                    for (DocumentSnapshot doc : list) {
                                        addMessage(doc);
                                    }

                                    scrollView.post(
                                            () -> scrollView.fullScroll(
                                                    View.FOCUS_DOWN
                                            )
                                    );
                                }
                        );
    }

    private void addMessage(DocumentSnapshot doc) {

        FirebaseUser user = auth.getCurrentUser();

        if (user == null) {
            return;
        }

        String senderId =
                doc.getString("senderId");

        String senderRole =
                doc.getString("senderRole");

        String message =
                doc.getString("message");

        if (message == null) {
            message = "";
        }

        boolean mine =
                user.getUid().equals(senderId);

        LinearLayout box =
                new LinearLayout(this);

        box.setOrientation(
                LinearLayout.VERTICAL
        );

        box.setPadding(
                14,
                10,
                14,
                10
        );

        TextView sender =
                new TextView(this);

        if (mine) {
            sender.setText("You");
        } else if ("DRIVER".equals(senderRole)) {
            sender.setText("Driver");
        } else {
            sender.setText("Passenger");
        }

        sender.setTextSize(13);
        sender.setTextColor(Color.GRAY);

        TextView text =
                new TextView(this);

        text.setText(message);
        text.setTextSize(17);
        text.setTextColor(Color.BLACK);

        box.addView(sender);
        box.addView(text);

        LinearLayout.LayoutParams params =
                new LinearLayout.LayoutParams(
                        LinearLayout.LayoutParams.WRAP_CONTENT,
                        LinearLayout.LayoutParams.WRAP_CONTENT
                );

        params.bottomMargin = 10;

        if (mine) {
            params.gravity = Gravity.END;
        } else {
            params.gravity = Gravity.START;
        }

        messagesLayout.addView(
                box,
                params
        );
    }

    private void sendMessage() {

        FirebaseUser user =
                auth.getCurrentUser();

        if (user == null) {
            showError(
                    "MESSAGE FAILED: Not logged in."
            );
            return;
        }

        if (rideId.trim().isEmpty()) {
            showError(
                    "MESSAGE FAILED: Ride ID missing."
            );
            return;
        }

        String message =
                messageInput.getText()
                        .toString()
                        .trim();

        if (message.isEmpty()) {
            return;
        }

        String uid = user.getUid();

        boolean participant =
                uid.equals(passengerId)
                        || uid.equals(driverId);

        if (!participant) {
            showError(
                    "MESSAGE FAILED: Not a ride participant."
            );
            return;
        }

        sendButton.setEnabled(false);

        Map<String, Object> data =
                new HashMap<>();

        data.put(
                "senderId",
                uid
        );

        data.put(
                "senderRole",
                uid.equals(driverId)
                        ? "DRIVER"
                        : "PASSENGER"
        );

        data.put(
                "message",
                message
        );

        // Use a normal Timestamp for this test.
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

                    sendButton.setEnabled(true);

                    Toast.makeText(
                            RideChatActivity.this,
                            "MESSAGE SAVED",
                            Toast.LENGTH_SHORT
                    ).show();
                })
                .addOnFailureListener(error -> {

                    sendButton.setEnabled(true);

                    showError(
                            "MESSAGE WRITE ERROR:\n"
                                    + firebaseError(error)
                    );
                });
    }

    private String firebaseError(Exception e) {

        if (e == null) {
            return "Unknown Firebase error.";
        }

        String message = e.getMessage();

        if (message == null ||
                message.trim().isEmpty()) {
            return e.toString();
        }

        return message;
    }

    private void showError(String message) {

        Toast.makeText(
                this,
                message,
                Toast.LENGTH_LONG
        ).show();
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

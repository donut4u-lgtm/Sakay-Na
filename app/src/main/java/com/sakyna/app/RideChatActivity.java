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

import com.google.firebase.auth.FirebaseAuth;
import com.google.firebase.auth.FirebaseUser;
import com.google.firebase.firestore.DocumentChange;
import com.google.firebase.firestore.DocumentSnapshot;
import com.google.firebase.firestore.FieldValue;
import com.google.firebase.firestore.FirebaseFirestore;
import com.google.firebase.firestore.ListenerRegistration;
import com.google.firebase.firestore.Query;

import java.util.HashMap;
import java.util.Map;

public class RideChatActivity extends Activity {

    private FirebaseAuth auth;
    private FirebaseFirestore db;

    private String rideId = "";

    private String passengerId = "";
    private String driverId = "";
    private String myRole = "";

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

        if (rideId == null) {
            rideId = "";
        }

        buildScreen();

        if (rideId.trim().isEmpty()) {

            Toast.makeText(
                    this,
                    "Chat error: Ride ID is missing.",
                    Toast.LENGTH_LONG
            ).show();

            return;
        }

        verifyRide();
    }

    private void buildScreen() {

        LinearLayout root =
                new LinearLayout(this);

        root.setOrientation(
                LinearLayout.VERTICAL
        );

        root.setBackgroundColor(
                Color.WHITE
        );

        TextView title =
                new TextView(this);

        title.setText(
                "Sakay Na Ride Chat"
        );

        title.setTextSize(23);

        title.setTextColor(
                Color.BLACK
        );

        title.setGravity(
                Gravity.CENTER
        );

        title.setPadding(
                16,
                20,
                16,
                20
        );

        root.addView(
                title,
                new LinearLayout.LayoutParams(
                        LinearLayout.LayoutParams.MATCH_PARENT,
                        LinearLayout.LayoutParams.WRAP_CONTENT
                )
        );

        scrollView =
                new ScrollView(this);

        messagesLayout =
                new LinearLayout(this);

        messagesLayout.setOrientation(
                LinearLayout.VERTICAL
        );

        messagesLayout.setPadding(
                16,
                16,
                16,
                16
        );

        scrollView.addView(
                messagesLayout
        );

        root.addView(
                scrollView,
                new LinearLayout.LayoutParams(
                        LinearLayout.LayoutParams.MATCH_PARENT,
                        0,
                        1
                )
        );

        LinearLayout bottom =
                new LinearLayout(this);

        bottom.setOrientation(
                LinearLayout.HORIZONTAL
        );

        bottom.setPadding(
                12,
                12,
                12,
                12
        );

        messageInput =
                new EditText(this);

        messageInput.setHint(
                "Type a message..."
        );

        messageInput.setTextSize(16);

        messageInput.setSingleLine(
                true
        );

        bottom.addView(
                messageInput,
                new LinearLayout.LayoutParams(
                        0,
                        LinearLayout.LayoutParams.WRAP_CONTENT,
                        1
                )
        );

        sendButton =
                new Button(this);

        sendButton.setText(
                "SEND"
        );

        sendButton.setEnabled(
                false
        );

        bottom.addView(
                sendButton,
                new LinearLayout.LayoutParams(
                        LinearLayout.LayoutParams.WRAP_CONTENT,
                        LinearLayout.LayoutParams.WRAP_CONTENT
                )
        );

        sendButton.setOnClickListener(
                v -> sendMessage()
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

        FirebaseUser user =
                auth.getCurrentUser();

        if (user == null) {

            showError(
                    "Chat error: You are not logged in."
            );

            return;
        }

        db.collection("rides")
                .document(rideId)
                .get()
                .addOnSuccessListener(
                        snapshot -> {

                            if (!snapshot.exists()) {

                                showError(
                                        "Chat error: Ride "
                                                + rideId
                                                + " was not found."
                                );

                                return;
                            }

                            passengerId =
                                    snapshot.getString(
                                            "passengerId"
                                    );

                            driverId =
                                    snapshot.getString(
                                            "driverId"
                                    );

                            String uid =
                                    user.getUid();

                            if (
                                    passengerId != null
                                            && uid.equals(
                                            passengerId
                                    )
                            ) {

                                myRole = "PASSENGER";

                            } else if (
                                    driverId != null
                                            && uid.equals(
                                            driverId
                                    )
                            ) {

                                myRole = "DRIVER";

                            } else {

                                showError(
                                        "Chat error: This account is not assigned to this ride."
                                );

                                return;
                            }

                            sendButton.setEnabled(
                                    true
                            );

                            listenForMessages();
                        }
                )
                .addOnFailureListener(
                        e -> {

                            showError(
                                    "Chat ride check failed: "
                                            + firebaseError(e)
                            );
                        }
                );
    }

    private void listenForMessages() {

        if (messageListener != null) {
            messageListener.remove();
        }

        messageListener =
                db.collection("rides")
                        .document(rideId)
                        .collection("messages")
                        .orderBy(
                                "createdAt",
                                Query.Direction.ASCENDING
                        )
                        .addSnapshotListener(
                                (snapshots, error) -> {

                                    if (error != null) {

                                        showError(
                                                "Chat read failed: "
                                                        + firebaseError(error)
                                        );

                                        return;
                                    }

                                    if (snapshots == null) {
                                        return;
                                    }

                                    messagesLayout.removeAllViews();

                                    for (
                                            DocumentChange change
                                            : snapshots.getDocumentChanges()
                                    ) {

                                        if (
                                                change.getType()
                                                        == DocumentChange.Type.ADDED
                                        ) {

                                            addMessage(
                                                    change.getDocument()
                                            );
                                        }
                                    }
                                }
                        );
    }

    private void addMessage(
            DocumentSnapshot snapshot
    ) {

        FirebaseUser user =
                auth.getCurrentUser();

        if (user == null) {
            return;
        }

        String senderId =
                snapshot.getString(
                        "senderId"
                );

        String senderRole =
                snapshot.getString(
                        "senderRole"
                );

        String message =
                snapshot.getString(
                        "message"
                );

        if (message == null) {
            message = "";
        }

        boolean mine =
                user.getUid().equals(
                        senderId
                );

        LinearLayout messageBox =
                new LinearLayout(this);

        messageBox.setOrientation(
                LinearLayout.VERTICAL
        );

        messageBox.setPadding(
                14,
                10,
                14,
                10
        );

        TextView senderText =
                new TextView(this);

        if (mine) {

            senderText.setText(
                    "You"
            );

        } else if (
                "DRIVER".equals(senderRole)
        ) {

            senderText.setText(
                    "Driver"
            );

        } else {

            senderText.setText(
                    "Passenger"
            );
        }

        senderText.setTextSize(13);

        senderText.setTextColor(
                Color.GRAY
        );

        TextView messageText =
                new TextView(this);

        messageText.setText(
                message
        );

        messageText.setTextSize(17);

        messageText.setTextColor(
                Color.BLACK
        );

        messageText.setPadding(
                0,
                4,
                0,
                4
        );

        messageBox.addView(
                senderText
        );

        messageBox.addView(
                messageText
        );

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
                messageBox,
                params
        );

        scrollView.post(
                () -> scrollView.fullScroll(
                        View.FOCUS_DOWN
                )
        );
    }

    private void sendMessage() {

        FirebaseUser user =
                auth.getCurrentUser();

        if (user == null) {

            showError(
                    "Message failed: You are not logged in."
            );

            return;
        }

        if (rideId.trim().isEmpty()) {

            showError(
                    "Message failed: Ride ID is missing."
            );

            return;
        }

        if (myRole.isEmpty()) {

            showError(
                    "Message failed: Ride has not been verified yet."
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

        sendButton.setEnabled(
                false
        );

        Map<String, Object>
                chatMessage =
                new HashMap<>();

        chatMessage.put(
                "senderId",
                user.getUid()
        );

        chatMessage.put(
                "senderRole",
                myRole
        );

        chatMessage.put(
                "message",
                message
        );

        chatMessage.put(
                "createdAt",
                FieldValue.serverTimestamp()
        );

        db.collection("rides")
                .document(rideId)
                .collection("messages")
                .add(chatMessage)
                .addOnSuccessListener(
                        documentReference -> {

                            messageInput.setText("");

                            sendButton.setEnabled(
                                    true
                            );
                        }
                )
                .addOnFailureListener(
                        e -> {

                            sendButton.setEnabled(
                                    true
                            );

                            showError(
                                    "Message failed: "
                                            + firebaseError(e)
                            );
                        }
                );
    }

    private String firebaseError(
            Exception e
    ) {

        if (e == null) {
            return "Unknown Firebase error.";
        }

        String message =
                e.getMessage();

        if (message == null
                || message.trim().isEmpty()) {

            return e.toString();
        }

        return message;
    }

    private void showError(
            String message
    ) {

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
        }

        super.onDestroy();
    }
}

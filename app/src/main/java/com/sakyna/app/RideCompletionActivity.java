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
import android.widget.RatingBar;
import android.widget.Toast;

import com.google.firebase.auth.FirebaseAuth;
import com.google.firebase.auth.FirebaseUser;
import com.google.firebase.firestore.DocumentSnapshot;
import com.google.firebase.firestore.FieldValue;
import com.google.firebase.firestore.FirebaseFirestore;

import java.util.HashMap;
import java.util.Map;

public class RideCompletionActivity extends Activity {

    private FirebaseAuth auth;
    private FirebaseFirestore db;

    private String rideId = "";

    private TextView titleText;
    private TextView rideText;
    private TextView fareText;
    private TextView paymentText;
    private TextView statusText;

    private RatingBar ratingBar;
    private EditText commentInput;

    private Button confirmPaymentButton;
    private Button submitRatingButton;

    private boolean paymentConfirmed = false;
    private boolean ratingSubmitted = false;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);

        auth = FirebaseAuth.getInstance();
        db = FirebaseFirestore.getInstance();

        rideId = getIntent().getStringExtra("ride_id");

        showScreen();

        if (rideId == null || rideId.isEmpty()) {

            showMessage("Ride ID is missing.");
            finish();

            return;
        }

        loadRide();
    }

    private void showScreen() {

        ScrollView scrollView =
                new ScrollView(this);

        LinearLayout root =
                new LinearLayout(this);

        root.setOrientation(
                LinearLayout.VERTICAL
        );

        root.setPadding(
                30,
                35,
                30,
                35
        );

        root.setBackgroundColor(
                Color.WHITE
        );

        scrollView.addView(root);

        titleText =
                text(
                        "🛺 SAKAY NA",
                        30,
                        Color.BLACK
                );

        titleText.setGravity(
                Gravity.CENTER
        );

        root.addView(titleText);

        TextView heading =
                text(
                        "RIDE COMPLETED",
                        24,
                        Color.rgb(20, 120, 70)
                );

        heading.setGravity(
                Gravity.CENTER
        );

        root.addView(heading);

        addSpace(root, 15);

        rideText =
                text(
                        "Loading ride...",
                        17,
                        Color.DKGRAY
                );

        root.addView(rideText);

        addSpace(root, 10);

        fareText =
                text(
                        "💰 Fare: Loading...",
                        22,
                        Color.rgb(20, 120, 70)
                );

        fareText.setGravity(
                Gravity.CENTER
        );

        root.addView(fareText);

        addSpace(root, 10);

        paymentText =
                text(
                        "💳 Payment: Loading...",
                        19,
                        Color.BLACK
                );

        paymentText.setGravity(
                Gravity.CENTER
        );

        root.addView(paymentText);

        addSpace(root, 10);

        statusText =
                text(
                        "Status: Loading...",
                        17,
                        Color.DKGRAY
                );

        statusText.setGravity(
                Gravity.CENTER
        );

        root.addView(statusText);

        addSpace(root, 20);

        confirmPaymentButton =
                button(
                        "✅ CONFIRM PAYMENT"
                );

        confirmPaymentButton.setOnClickListener(
                v -> confirmPayment()
        );

        root.addView(
                confirmPaymentButton
        );

        addSpace(root, 25);

        TextView ratingTitle =
                text(
                        "⭐ RATE YOUR DRIVER",
                        21,
                        Color.BLACK
                );

        ratingTitle.setGravity(
                Gravity.CENTER
        );

        root.addView(ratingTitle);

        addSpace(root, 10);

        ratingBar =
                new RatingBar(
                        this,
                        null,
                        android.R.attr.ratingBarStyleLarge
                );

        ratingBar.setNumStars(5);
        ratingBar.setStepSize(1.0f);
        ratingBar.setRating(5.0f);

        LinearLayout.LayoutParams ratingParams =
                new LinearLayout.LayoutParams(
                        LinearLayout.LayoutParams.WRAP_CONTENT,
                        LinearLayout.LayoutParams.WRAP_CONTENT
                );

        ratingParams.gravity =
                Gravity.CENTER;

        ratingBar.setLayoutParams(
                ratingParams
        );

        root.addView(
                ratingBar
        );

        commentInput =
                new EditText(this);

        commentInput.setHint(
                "Driver comment (optional)"
        );

        commentInput.setTextSize(16);

        commentInput.setMinLines(3);

        commentInput.setGravity(
                Gravity.TOP
        );

        root.addView(
                commentInput
        );

        addSpace(root, 10);

        submitRatingButton =
                button(
                        "⭐ SUBMIT RATING"
                );

        submitRatingButton.setOnClickListener(
                v -> submitRating()
        );

        root.addView(
                submitRatingButton
        );

        addSpace(root, 20);

        Button doneButton =
                button(
                        "DONE"
                );

        doneButton.setOnClickListener(
                v -> finish()
        );

        root.addView(doneButton);

        setContentView(
                scrollView
        );

        confirmPaymentButton.setEnabled(false);
        submitRatingButton.setEnabled(false);
    }

    private void loadRide() {

        FirebaseUser user =
                auth.getCurrentUser();

        if (user == null) {

            showMessage(
                    "Please login again."
            );

            finish();

            return;
        }

        db.collection("rides")
                .document(rideId)
                .get()
                .addOnSuccessListener(
                        document -> {

                            if (!document.exists()) {

                                showMessage(
                                        "Ride not found."
                                );

                                finish();

                                return;
                            }

                            String passengerId =
                                    document.getString(
                                            "passengerId"
                                    );

                            if (passengerId == null ||
                                    !passengerId.equals(
                                            user.getUid()
                                    )) {

                                showMessage(
                                        "This ride does not belong to you."
                                );

                                finish();

                                return;
                            }

                            displayRide(
                                    document
                            );
                        }
                )
                .addOnFailureListener(
                        e -> {

                            showMessage(
                                    "Failed to load ride: " +
                                    e.getMessage()
                            );

                            finish();
                        }
                );
    }

    private void displayRide(
            DocumentSnapshot document) {

        String pickup =
                document.getString(
                        "pickup"
                );

        String destination =
                document.getString(
                        "destination"
                );

        String status =
                document.getString(
                        "status"
                );

        String paymentMethod =
                document.getString(
                        "paymentMethod"
                );

        String paymentStatus =
                document.getString(
                        "paymentStatus"
                );

        Double fare =
                document.getDouble(
                        "fare"
                );

        StringBuilder rideInfo =
                new StringBuilder();

        rideInfo.append(
                "📍 Pickup: "
        ).append(
                pickup == null
                        ? "Unknown"
                        : pickup
        );

        rideInfo.append(
                "\n📍 Destination: "
        ).append(
                destination == null
                        ? "Unknown"
                        : destination
        );

        rideText.setText(
                rideInfo.toString()
        );

        if (fare != null) {

            fareText.setText(
                    "💰 TOTAL FARE: ₱" +
                    money(fare)
            );
        }

        paymentText.setText(
                "💳 Payment: " +
                paymentLabel(
                        paymentMethod
                )
        );

        statusText.setText(
                "Status: " +
                formatStatus(status)
        );

        if ("CONFIRMED".equals(
                paymentStatus
        ) ||
                "COMPLETED".equals(
                        status
                )) {

            paymentConfirmed = true;

            confirmPaymentButton.setEnabled(
                    false
            );

            confirmPaymentButton.setText(
                    "✅ PAYMENT CONFIRMED"
            );

            submitRatingButton.setEnabled(
                    true
            );

        } else {

            confirmPaymentButton.setEnabled(
                    true
            );
        }

        if (document.contains(
                "driverRating"
        )) {

            Double savedRating =
                    document.getDouble(
                            "driverRating"
                    );

            if (savedRating != null) {

                ratingBar.setRating(
                        savedRating.floatValue()
                );

                ratingSubmitted = true;

                submitRatingButton.setEnabled(
                        false
                );

                submitRatingButton.setText(
                        "⭐ RATING SUBMITTED"
                );
            }
        }

        String savedComment =
                document.getString(
                        "driverRatingComment"
                );

        if (savedComment != null) {

            commentInput.setText(
                    savedComment
            );
        }
    }

    private void confirmPayment() {

        if (rideId.isEmpty()) {
            return;
        }

        confirmPaymentButton.setEnabled(
                false
        );

        Map<String, Object> payment =
                new HashMap<>();

        payment.put(
                "paymentStatus",
                "CONFIRMED"
        );

        payment.put(
                "paymentConfirmedAt",
                FieldValue.serverTimestamp()
        );

        payment.put(
                "status",
                "COMPLETED"
        );

        payment.put(
                "completedAt",
                FieldValue.serverTimestamp()
        );

        db.collection("rides")
                .document(rideId)
                .update(payment)
                .addOnSuccessListener(
                        unused -> {

                            paymentConfirmed =
                                    true;

                            statusText.setText(
                                    "Status: COMPLETED"
                            );

                            confirmPaymentButton.setText(
                                    "✅ PAYMENT CONFIRMED"
                            );

                            submitRatingButton.setEnabled(
                                    true
                            );

                            showMessage(
                                    "🟢 Payment confirmed.\n" +
                                    "Ride completed."
                            );
                        }
                )
                .addOnFailureListener(
                        e -> {

                            confirmPaymentButton.setEnabled(
                                    true
                            );

                            showMessage(
                                    "Payment confirmation failed: " +
                                    e.getMessage()
                            );
                        }
                );
    }

    private void submitRating() {

        if (!paymentConfirmed) {

            showMessage(
                    "Please confirm payment first."
            );

            return;
        }

        if (ratingSubmitted) {

            showMessage(
                    "Rating already submitted."
            );

            return;
        }

        float rating =
                ratingBar.getRating();

        if (rating < 1) {

            showMessage(
                    "Please select a rating."
            );

            return;
        }

        String comment =
                commentInput
                        .getText()
                        .toString()
                        .trim();

        Map<String, Object> ratingData =
                new HashMap<>();

        ratingData.put(
                "driverRating",
                (double) rating
        );

        ratingData.put(
                "driverRatingComment",
                comment
        );

        ratingData.put(
                "ratedAt",
                FieldValue.serverTimestamp()
        );

        submitRatingButton.setEnabled(
                false
        );

        db.collection("rides")
                .document(rideId)
                .update(ratingData)
                .addOnSuccessListener(
                        unused -> {

                            ratingSubmitted =
                                    true;

                            submitRatingButton.setText(
                                    "⭐ RATING SUBMITTED"
                            );

                            showMessage(
                                    "🟢 Thank you for rating your driver!"
                            );
                        }
                )
                .addOnFailureListener(
                        e -> {

                            submitRatingButton.setEnabled(
                                    true
                            );

                            showMessage(
                                    "Rating failed: " +
                                    e.getMessage()
                            );
                        }
                );
    }

    private String paymentLabel(
            String method) {

        if ("GCASH".equals(method)) {
            return "GCash";
        }

        if ("MAYA".equals(method)) {
            return "Maya / PayMaya";
        }

        return "Cash";
    }

    private String formatStatus(
            String status) {

        if (status == null ||
                status.isEmpty()) {

            return "UNKNOWN";
        }

        return status.replace(
                "_",
                " "
        );
    }

    private String money(
            double value) {

        return String.format(
                "%.2f",
                value
        );
    }

    private Button button(
            String label) {

        Button button =
                new Button(this);

        button.setText(label);
        button.setTextSize(15);
        button.setAllCaps(false);

        return button;
    }

    private TextView text(
            String value,
            int size,
            int color) {

        TextView view =
                new TextView(this);

        view.setText(value);
        view.setTextSize(size);
        view.setTextColor(color);

        view.setPadding(
                10,
                10,
                10,
                10
        );

        return view;
    }

    private void addSpace(
            LinearLayout parent,
            int height) {

        TextView space =
                new TextView(this);

        space.setHeight(height);

        parent.addView(
                space
        );
    }

    private void showMessage(
            String message) {

        Toast.makeText(
                this,
                message,
                Toast.LENGTH_LONG
        ).show();
    }
}

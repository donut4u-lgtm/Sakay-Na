
package com.sakyna.app;

import android.app.Activity;
import android.os.Bundle;
import android.graphics.Color;
import android.view.Gravity;
import android.view.View;
import android.widget.Button;
import android.widget.EditText;
import android.widget.LinearLayout;
import android.widget.RatingBar;
import android.widget.ScrollView;
import android.widget.TextView;
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

    private TextView rideInfo;
    private TextView paymentStatusText;
    private RatingBar ratingBar;
    private EditText commentInput;

    private boolean paymentConfirmed = false;
    private boolean ratingSubmitted = false;

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

        if (rideId.isEmpty()) {
            Toast.makeText(
                    this,
                    "Ride ID is missing.",
                    Toast.LENGTH_LONG
            ).show();
            return;
        }

        loadRide();
    }

    private void buildScreen() {

        ScrollView scrollView = new ScrollView(this);

        LinearLayout root = new LinearLayout(this);
        root.setOrientation(LinearLayout.VERTICAL);
        root.setPadding(32, 32, 32, 32);
        root.setBackgroundColor(Color.WHITE);

        scrollView.addView(root);

        TextView title = new TextView(this);
        title.setText("Ride Completed");
        title.setTextSize(28);
        title.setTextColor(Color.BLACK);
        title.setGravity(Gravity.CENTER);
        title.setPadding(0, 0, 0, 24);

        root.addView(
                title,
                new LinearLayout.LayoutParams(
                        LinearLayout.LayoutParams.MATCH_PARENT,
                        LinearLayout.LayoutParams.WRAP_CONTENT
                )
        );

        rideInfo = new TextView(this);
        rideInfo.setText("Loading ride...");
        rideInfo.setTextSize(17);
        rideInfo.setTextColor(Color.DKGRAY);
        rideInfo.setPadding(0, 0, 0, 24);

        root.addView(
                rideInfo,
                new LinearLayout.LayoutParams(
                        LinearLayout.LayoutParams.MATCH_PARENT,
                        LinearLayout.LayoutParams.WRAP_CONTENT
                )
        );

        paymentStatusText = new TextView(this);
        paymentStatusText.setText("Payment: PENDING");
        paymentStatusText.setTextSize(18);
        paymentStatusText.setTextColor(Color.BLACK);
        paymentStatusText.setPadding(0, 0, 0, 16);

        root.addView(
                paymentStatusText,
                new LinearLayout.LayoutParams(
                        LinearLayout.LayoutParams.MATCH_PARENT,
                        LinearLayout.LayoutParams.WRAP_CONTENT
                )
        );

        Button confirmPaymentButton = new Button(this);
        confirmPaymentButton.setText("CONFIRM PAYMENT");

        root.addView(
                confirmPaymentButton,
                new LinearLayout.LayoutParams(
                        LinearLayout.LayoutParams.MATCH_PARENT,
                        LinearLayout.LayoutParams.WRAP_CONTENT
                )
        );

        confirmPaymentButton.setOnClickListener(
                new View.OnClickListener() {
                    @Override
                    public void onClick(View view) {
                        confirmPayment();
                    }
                }
        );

        TextView ratingTitle = new TextView(this);
        ratingTitle.setText("Rate your driver");
        ratingTitle.setTextSize(21);
        ratingTitle.setTextColor(Color.BLACK);
        ratingTitle.setPadding(0, 32, 0, 12);

        root.addView(
                ratingTitle,
                new LinearLayout.LayoutParams(
                        LinearLayout.LayoutParams.MATCH_PARENT,
                        LinearLayout.LayoutParams.WRAP_CONTENT
                )
        );

        ratingBar = new RatingBar(this);
        ratingBar.setNumStars(5);
        ratingBar.setStepSize(1.0f);
        ratingBar.setRating(5.0f);

        root.addView(
                ratingBar,
                new LinearLayout.LayoutParams(
                        LinearLayout.LayoutParams.WRAP_CONTENT,
                        LinearLayout.LayoutParams.WRAP_CONTENT
                )
        );

        commentInput = new EditText(this);
        commentInput.setHint("Optional comment");
        commentInput.setTextSize(16);
        commentInput.setMinLines(3);
        commentInput.setGravity(Gravity.TOP);
        commentInput.setPadding(16, 16, 16, 16);

        LinearLayout.LayoutParams commentParams =
                new LinearLayout.LayoutParams(
                        LinearLayout.LayoutParams.MATCH_PARENT,
                        LinearLayout.LayoutParams.WRAP_CONTENT
                );

        commentParams.topMargin = 16;

        root.addView(
                commentInput,
                commentParams
        );

        Button submitRatingButton = new Button(this);
        submitRatingButton.setText("SUBMIT RATING");

        LinearLayout.LayoutParams ratingButtonParams =
                new LinearLayout.LayoutParams(
                        LinearLayout.LayoutParams.MATCH_PARENT,
                        LinearLayout.LayoutParams.WRAP_CONTENT
                );

        ratingButtonParams.topMargin = 16;

        root.addView(
                submitRatingButton,
                ratingButtonParams
        );

        submitRatingButton.setOnClickListener(
                new View.OnClickListener() {
                    @Override
                    public void onClick(View view) {
                        submitRating();
                    }
                }
        );

        Button doneButton = new Button(this);
        doneButton.setText("DONE");

        LinearLayout.LayoutParams doneParams =
                new LinearLayout.LayoutParams(
                        LinearLayout.LayoutParams.MATCH_PARENT,
                        LinearLayout.LayoutParams.WRAP_CONTENT
                );

        doneParams.topMargin = 24;

        root.addView(
                doneButton,
                doneParams
        );

        doneButton.setOnClickListener(
                new View.OnClickListener() {
                    @Override
                    public void onClick(View view) {
                        finish();
                    }
                }
        );

        setContentView(scrollView);
    }

    private void loadRide() {

        FirebaseUser user = auth.getCurrentUser();

        if (user == null) {
            Toast.makeText(
                    this,
                    "Please login again.",
                    Toast.LENGTH_LONG
            ).show();
            return;
        }

        db.collection("rides")
                .document(rideId)
                .get()
                .addOnSuccessListener(
                        snapshot -> {

                            if (!snapshot.exists()) {

                                Toast.makeText(
                                        this,
                                        "Ride not found.",
                                        Toast.LENGTH_LONG
                                ).show();

                                return;
                            }

                            String passengerId =
                                    getStringValue(
                                            snapshot,
                                            "passengerId"
                                    );

                            if (!user.getUid().equals(passengerId)) {

                                Toast.makeText(
                                        this,
                                        "You cannot access this ride.",
                                        Toast.LENGTH_LONG
                                ).show();

                                return;
                            }

                            displayRide(snapshot);
                        }
                )
                .addOnFailureListener(
                        e -> Toast.makeText(
                                this,
                                "Failed to load ride: "
                                        + e.getMessage(),
                                Toast.LENGTH_LONG
                        ).show()
                );
    }

    private void displayRide(DocumentSnapshot snapshot) {

        String pickup =
                getStringValue(snapshot, "pickup");

        String destination =
                getStringValue(snapshot, "destination");

        String paymentMethod =
                getStringValue(snapshot, "paymentMethod");

        String status =
                getStringValue(snapshot, "status");

        String paymentStatus =
                getStringValue(snapshot, "paymentStatus");

        Object fareObject =
                snapshot.get("fare");

        String fareText = "0.00";

        if (fareObject instanceof Number) {

            double fare =
                    ((Number) fareObject).doubleValue();

            fareText =
                    String.format(
                            java.util.Locale.US,
                            "%.2f",
                            fare
                    );
        }

        rideInfo.setText(
                "Pickup:\n"
                        + pickup
                        + "\n\nDestination:\n"
                        + destination
                        + "\n\nFare: ₱"
                        + fareText
                        + "\n\nPayment Method: "
                        + paymentMethod
                        + "\n\nRide Status: "
                        + status
        );

        if ("CONFIRMED".equals(paymentStatus)) {

            paymentConfirmed = true;

            paymentStatusText.setText(
                    "Payment: CONFIRMED"
            );

        } else {

            paymentStatusText.setText(
                    "Payment: PENDING"
            );
        }

        if ("COMPLETED".equals(status)) {

            paymentConfirmed = true;

            paymentStatusText.setText(
                    "Payment: CONFIRMED"
            );
        }
    }

    private void confirmPayment() {

        if (rideId.isEmpty()) {
            return;
        }

        if (paymentConfirmed) {

            Toast.makeText(
                    this,
                    "Payment is already confirmed.",
                    Toast.LENGTH_SHORT
            ).show();

            return;
        }

        Map<String, Object> updates =
                new HashMap<>();

        updates.put(
                "paymentStatus",
                "CONFIRMED"
        );

        updates.put(
                "paymentConfirmedAt",
                FieldValue.serverTimestamp()
        );

        updates.put(
                "status",
                "COMPLETED"
        );

        updates.put(
                "completedAt",
                FieldValue.serverTimestamp()
        );

        db.collection("rides")
                .document(rideId)
                .update(updates)
                .addOnSuccessListener(
                        unused -> {

                            paymentConfirmed = true;

                            paymentStatusText.setText(
                                    "Payment: CONFIRMED"
                            );

                            Toast.makeText(
                                    this,
                                    "Payment confirmed.",
                                    Toast.LENGTH_SHORT
                            ).show();

                            loadRide();
                        }
                )
                .addOnFailureListener(
                        e -> Toast.makeText(
                                this,
                                "Payment confirmation failed: "
                                        + e.getMessage(),
                                Toast.LENGTH_LONG
                        ).show()
                );
    }

    private void submitRating() {

        if (rideId.isEmpty()) {
            return;
        }

        if (ratingSubmitted) {

            Toast.makeText(
                    this,
                    "Rating already submitted.",
                    Toast.LENGTH_SHORT
            ).show();

            return;
        }

        float rating =
                ratingBar.getRating();

        String comment =
                commentInput.getText()
                        .toString()
                        .trim();

        if (rating <= 0) {

            Toast.makeText(
                    this,
                    "Please select a rating.",
                    Toast.LENGTH_SHORT
            ).show();

            return;
        }

        Map<String, Object> updates =
                new HashMap<>();

        updates.put(
                "driverRating",
                (double) rating
        );

        updates.put(
                "driverRatingComment",
                comment
        );

        updates.put(
                "ratedAt",
                FieldValue.serverTimestamp()
        );

        db.collection("rides")
                .document(rideId)
                .update(updates)
                .addOnSuccessListener(
                        unused -> {

                            ratingSubmitted = true;

                            Toast.makeText(
                                    this,
                                    "Thank you for rating your driver.",
                                    Toast.LENGTH_SHORT
                            ).show();
                        }
                )
                .addOnFailureListener(
                        e -> Toast.makeText(
                                this,
                                "Rating failed: "
                                        + e.getMessage(),
                                Toast.LENGTH_LONG
                        ).show()
                );
    }

    private String getStringValue(
            DocumentSnapshot snapshot,
            String field
    ) {

        String value =
                snapshot.getString(field);

        if (value == null) {
            return "";
        }

        return value;
    }
}

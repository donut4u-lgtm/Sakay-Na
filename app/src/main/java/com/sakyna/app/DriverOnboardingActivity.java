package com.sakyna.app;

import android.app.Activity;
import android.content.Intent;
import android.graphics.Color;
import android.os.Bundle;
import android.view.Gravity;
import android.widget.Button;
import android.widget.EditText;
import android.widget.LinearLayout;
import android.widget.TextView;
import android.widget.Toast;

import com.google.firebase.auth.FirebaseAuth;
import com.google.firebase.auth.FirebaseUser;
import com.google.firebase.firestore.FirebaseFirestore;

import java.util.HashMap;
import java.util.Map;

public class DriverOnboardingActivity extends Activity {

    private FirebaseAuth auth;
    private FirebaseFirestore db;

    private EditText nameInput;
    private EditText phoneInput;
    private EditText plateInput;
    private EditText vehicleInput;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);

        auth = FirebaseAuth.getInstance();

        db = FirebaseFirestore.getInstance();

        showScreen();

        loadExistingProfile();
    }

    private void showScreen() {

        LinearLayout root =
                new LinearLayout(this);

        root.setOrientation(
                LinearLayout.VERTICAL
        );

        root.setGravity(
                Gravity.CENTER_HORIZONTAL
        );

        root.setPadding(
                30,
                40,
                30,
                30
        );

        root.setBackgroundColor(
                Color.WHITE
        );

        TextView title =
                text(
                        "🛺 SAKAY NA",
                        30,
                        Color.BLACK
                );

        title.setGravity(
                Gravity.CENTER
        );

        root.addView(title);

        TextView heading =
                text(
                        "DRIVER ONBOARDING",
                        22,
                        Color.rgb(20, 120, 70)
                );

        heading.setGravity(
                Gravity.CENTER
        );

        root.addView(heading);

        TextView info =
                text(
                        "Complete your driver profile " +
                        "before accepting passenger rides.",
                        15,
                        Color.DKGRAY
                );

        info.setGravity(
                Gravity.CENTER
        );

        root.addView(info);

        nameInput =
                input(
                        "Driver full name"
                );

        root.addView(nameInput);

        phoneInput =
                input(
                        "Mobile number"
                );

        root.addView(phoneInput);

        plateInput =
                input(
                        "Tricycle plate / registration"
                );

        root.addView(plateInput);

        vehicleInput =
                input(
                        "Vehicle description"
                );

        root.addView(vehicleInput);

        Button save =
                button(
                        "💾 SAVE DRIVER PROFILE"
                );

        save.setOnClickListener(
                v -> saveProfile()
        );

        root.addView(save);

        Button continueButton =
                button(
                        "🚕 CONTINUE TO DRIVER"
                );

        continueButton.setOnClickListener(
                v -> openDriver()
        );

        root.addView(
                continueButton
        );

        Button logout =
                button(
                        "🚪 LOGOUT"
                );

        logout.setOnClickListener(
                v -> logout()
        );

        root.addView(logout);

        setContentView(root);
    }

    private void loadExistingProfile() {

        FirebaseUser user =
                auth.getCurrentUser();

        if (user == null) {
            return;
        }

        db.collection("users")
                .document(user.getUid())
                .get()
                .addOnSuccessListener(
                        document -> {

                            if (!document.exists()) {
                                return;
                            }

                            String name =
                                    document.getString(
                                            "driverName"
                                    );

                            String phone =
                                    document.getString(
                                            "phone"
                                    );

                            String plate =
                                    document.getString(
                                            "plateNumber"
                                    );

                            String vehicle =
                                    document.getString(
                                            "vehicleDescription"
                                    );

                            if (name != null) {
                                nameInput.setText(name);
                            }

                            if (phone != null) {
                                phoneInput.setText(phone);
                            }

                            if (plate != null) {
                                plateInput.setText(plate);
                            }

                            if (vehicle != null) {
                                vehicleInput.setText(vehicle);
                            }
                        }
                );
    }

    private void saveProfile() {

        FirebaseUser user =
                auth.getCurrentUser();

        if (user == null) {

            showMessage(
                    "Please login again."
            );

            return;
        }

        String name =
                nameInput
                        .getText()
                        .toString()
                        .trim();

        String phone =
                phoneInput
                        .getText()
                        .toString()
                        .trim();

        String plate =
                plateInput
                        .getText()
                        .toString()
                        .trim();

        String vehicle =
                vehicleInput
                        .getText()
                        .toString()
                        .trim();

        if (name.isEmpty()) {

            showMessage(
                    "Enter driver name."
            );

            return;
        }

        if (phone.isEmpty()) {

            showMessage(
                    "Enter mobile number."
            );

            return;
        }

        if (plate.isEmpty()) {

            showMessage(
                    "Enter plate / registration."
            );

            return;
        }

        if (vehicle.isEmpty()) {

            showMessage(
                    "Enter vehicle description."
            );

            return;
        }

        Map<String, Object> profile =
                new HashMap<>();

        profile.put(
                "driverName",
                name
        );

        profile.put(
                "phone",
                phone
        );

        profile.put(
                "plateNumber",
                plate
        );

        profile.put(
                "vehicleDescription",
                vehicle
        );

        profile.put(
                "driverProfileComplete",
                true
        );

        profile.put(
                "updatedAt",
                com.google.firebase.firestore.FieldValue
                        .serverTimestamp()
        );

        db.collection("users")
                .document(user.getUid())
                .update(profile)
                .addOnSuccessListener(
                        unused -> {

                            showMessage(
                                    "🟢 Driver profile saved."
                            );

                            openDriver();
                        }
                )
                .addOnFailureListener(
                        e -> showMessage(
                                "Save failed: " +
                                e.getMessage()
                        )
                );
    }

    private void openDriver() {

        Intent intent =
                new Intent(
                        this,
                        DriverActivity.class
                );

        startActivity(intent);

        finish();
    }

    private void logout() {

        auth.signOut();

        Intent intent =
                new Intent(
                        this,
                        MainActivity.class
                );

        intent.addFlags(
                Intent.FLAG_ACTIVITY_CLEAR_TOP |
                Intent.FLAG_ACTIVITY_NEW_TASK |
                Intent.FLAG_ACTIVITY_CLEAR_TASK
        );

        startActivity(intent);

        finish();
    }

    private EditText input(
            String hint) {

        EditText input =
                new EditText(this);

        input.setHint(hint);

        input.setTextSize(16);

        input.setSingleLine(true);

        input.setPadding(
                15,
                12,
                15,
                12
        );

        LinearLayout.LayoutParams params =
                new LinearLayout.LayoutParams(
                        LinearLayout.LayoutParams.MATCH_PARENT,
                        LinearLayout.LayoutParams.WRAP_CONTENT
                );

        params.setMargins(
                0,
                8,
                0,
                8
        );

        input.setLayoutParams(params);

        return input;
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

    private void showMessage(
            String message) {

        Toast.makeText(
                this,
                message,
                Toast.LENGTH_LONG
        ).show();
    }
}

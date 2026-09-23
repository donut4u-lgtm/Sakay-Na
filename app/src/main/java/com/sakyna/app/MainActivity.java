package com.sakyna.app;

import android.Manifest;
import android.app.Activity;
import android.content.Intent;
import android.content.pm.PackageManager;
import android.os.Build;
import android.os.Bundle;
import android.text.InputType;
import android.view.Gravity;
import android.view.View;
import android.widget.Button;
import android.widget.EditText;
import android.widget.LinearLayout;
import android.widget.TextView;
import android.widget.Toast;

import androidx.annotation.NonNull;
import androidx.core.app.ActivityCompat;

import com.google.firebase.auth.FirebaseAuth;
import com.google.firebase.auth.FirebaseUser;
import com.google.firebase.firestore.DocumentSnapshot;
import com.google.firebase.firestore.FieldValue;
import com.google.firebase.firestore.FirebaseFirestore;
import com.google.firebase.firestore.SetOptions;

import java.util.HashMap;
import java.util.Locale;
import java.util.Map;

public class MainActivity extends Activity {

    private FirebaseAuth auth;
    private FirebaseFirestore db;

    private EditText phoneField;
    private EditText passwordField;

    private Button passengerButton;
    private Button driverButton;
    private Button adminButton;
    private Button loginButton;
    private Button registerButton;

    private TextView statusText;

    private String selectedRole = "PASSENGER";

    private static final int NOTIFICATION_PERMISSION_REQUEST = 9101;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);

        auth = FirebaseAuth.getInstance();
        db = FirebaseFirestore.getInstance();

        requestNotificationPermission();

        showLoginScreen();
    }

    private void showLoginScreen() {

        LinearLayout root =
                new LinearLayout(this);

        root.setOrientation(
                LinearLayout.VERTICAL
        );

        root.setGravity(
                Gravity.CENTER_HORIZONTAL
        );

        root.setPadding(
                40,
                50,
                40,
                40
        );

        TextView title =
                new TextView(this);

        title.setText(
                "SAKAY NA"
        );

        title.setTextSize(32);
        title.setGravity(Gravity.CENTER);
        title.setPadding(0, 0, 0, 20);

        root.addView(title);

        TextView subtitle =
                new TextView(this);

        subtitle.setText(
                "Tricycle Ride Booking"
        );

        subtitle.setTextSize(18);
        subtitle.setGravity(Gravity.CENTER);
        subtitle.setPadding(0, 0, 0, 35);

        root.addView(subtitle);

        phoneField =
                new EditText(this);

        phoneField.setHint(
                "Phone Number"
        );

        phoneField.setInputType(
                InputType.TYPE_CLASS_PHONE
        );

        root.addView(
                phoneField,
                new LinearLayout.LayoutParams(
                        -1,
                        -2
                )
        );

        passwordField =
                new EditText(this);

        passwordField.setHint(
                "Password"
        );

        passwordField.setInputType(
                InputType.TYPE_CLASS_TEXT
                        | InputType.TYPE_TEXT_VARIATION_PASSWORD
        );

        LinearLayout.LayoutParams passwordParams =
                new LinearLayout.LayoutParams(
                        -1,
                        -2
                );

        passwordParams.topMargin = 20;

        root.addView(
                passwordField,
                passwordParams
        );

        TextView roleLabel =
                new TextView(this);

        roleLabel.setText(
                "Select Account Type"
        );

        roleLabel.setTextSize(17);
        roleLabel.setGravity(Gravity.CENTER);

        LinearLayout.LayoutParams roleLabelParams =
                new LinearLayout.LayoutParams(
                        -1,
                        -2
                );

        roleLabelParams.topMargin = 30;

        root.addView(
                roleLabel,
                roleLabelParams
        );

        LinearLayout roleRow =
                new LinearLayout(this);

        roleRow.setOrientation(
                LinearLayout.HORIZONTAL
        );

        roleRow.setGravity(
                Gravity.CENTER
        );

        passengerButton =
                new Button(this);

        passengerButton.setText(
                "PASSENGER"
        );

        driverButton =
                new Button(this);

        driverButton.setText(
                "DRIVER"
        );

        adminButton =
                new Button(this);

        adminButton.setText(
                "ADMIN"
        );

        roleRow.addView(
                passengerButton,
                new LinearLayout.LayoutParams(
                        0,
                        -2,
                        1
                )
        );

        roleRow.addView(
                driverButton,
                new LinearLayout.LayoutParams(
                        0,
                        -2,
                        1
                )
        );

        roleRow.addView(
                adminButton,
                new LinearLayout.LayoutParams(
                        0,
                        -2,
                        1
                )
        );

        root.addView(roleRow);

        loginButton =
                new Button(this);

        loginButton.setText(
                "LOGIN"
        );

        LinearLayout.LayoutParams loginParams =
                new LinearLayout.LayoutParams(
                        -1,
                        -2
                );

        loginParams.topMargin = 30;

        root.addView(
                loginButton,
                loginParams
        );

        registerButton =
                new Button(this);

        registerButton.setText(
                "CREATE ACCOUNT"
        );

        root.addView(
                registerButton,
                new LinearLayout.LayoutParams(
                        -1,
                        -2
                )
        );

        statusText =
                new TextView(this);

        statusText.setText(
                "Phone number + password only"
        );

        statusText.setGravity(
                Gravity.CENTER
        );

        statusText.setPadding(
                0,
                25,
                0,
                0
        );

        root.addView(statusText);

        setContentView(root);

        selectedRole = "PASSENGER";

        passengerButton.setOnClickListener(
                v -> {

                    selectedRole =
                            "PASSENGER";

                    toast(
                            "Passenger selected"
                    );
                }
        );

        driverButton.setOnClickListener(
                v -> {

                    selectedRole =
                            "DRIVER";

                    toast(
                            "Driver selected"
                    );
                }
        );

        adminButton.setOnClickListener(
                v -> {

                    selectedRole =
                            "ADMIN";

                    toast(
                            "Admin selected"
                    );
                }
        );

        loginButton.setOnClickListener(
                v -> login()
        );

        registerButton.setOnClickListener(
                v -> openRegistration()
        );
    }

    private void login() {

        String phone =
                phoneField.getText()
                        .toString()
                        .trim();

        String password =
                passwordField.getText()
                        .toString();

        if (phone.isEmpty()) {

            phoneField.setError(
                    "Enter phone number"
            );

            return;
        }

        if (password.isEmpty()) {

            passwordField.setError(
                    "Enter password"
            );

            return;
        }

        String normalizedPhone =
                normalizePhone(phone);

        if (normalizedPhone.isEmpty()) {

            phoneField.setError(
                    "Enter a valid Philippine phone number"
            );

            return;
        }

        String authEmail =
                normalizedPhone
                        + "@sakyna.app";

        loginButton.setEnabled(false);

        statusText.setText(
                "Logging in..."
        );

        auth.signInWithEmailAndPassword(
                        authEmail,
                        password
                )
                .addOnSuccessListener(
                        result -> {

                            FirebaseUser user =
                                    result.getUser();

                            if (user == null) {

                                loginButton.setEnabled(
                                        true
                                );

                                showLoginError(
                                        "Login failed."
                                );

                                return;
                            }

                            loadUserProfile(
                                    user,
                                    normalizedPhone
                            );
                        }
                )
                .addOnFailureListener(
                        e -> {

                            loginButton.setEnabled(
                                    true
                            );

                            statusText.setText(
                                    "Login failed"
                            );

                            showLoginError(
                                    "Invalid phone number or password."
                            );
                        }
                );
    }

    private void loadUserProfile(
            FirebaseUser user,
            String normalizedPhone
    ) {

        String uid =
                user.getUid();

        db.collection("users")
                .document(uid)
                .get()
                .addOnSuccessListener(
                        document -> {

                            if (document.exists()) {

                                routeUsingRole(
                                        document,
                                        normalizedPhone
                                );

                                return;
                            }

                            findProfileByPhone(
                                    normalizedPhone
                            );
                        }
                )
                .addOnFailureListener(
                        e -> {

                            loginButton.setEnabled(
                                    true
                            );

                            showLoginError(
                                    "Unable to load account profile."
                            );
                        }
                );
    }

    private void findProfileByPhone(
            String normalizedPhone
    ) {

        db.collection("users")
                .whereEqualTo(
                        "phone",
                        normalizedPhone
                )
                .limit(1)
                .get()
                .addOnSuccessListener(
                        querySnapshot -> {

                            if (querySnapshot.isEmpty()) {

                                loginButton.setEnabled(
                                        true
                                );

                                showLoginError(
                                        "User profile was not found."
                                );

                                return;
                            }

                            DocumentSnapshot document =
                                    querySnapshot
                                            .getDocuments()
                                            .get(0);

                            routeUsingRole(
                                    document,
                                    normalizedPhone
                            );
                        }
                )
                .addOnFailureListener(
                        e -> {

                            loginButton.setEnabled(
                                    true
                            );

                            showLoginError(
                                    "Unable to find account profile."
                            );
                        }
                );
    }

    private void routeUsingRole(
            DocumentSnapshot document,
            String normalizedPhone
    ) {

        if (document == null
                || !document.exists()) {

            showLoginError(
                    "User profile was not found."
            );

            return;
        }

        String securityStatus =
                document.getString(
                        "securityStatus"
                );

        if ("BANNED".equalsIgnoreCase(
                securityStatus
        )) {

            auth.signOut();

            showLoginScreen();

            toast(
                    "This account has been banned."
            );

            return;
        }

        if ("SUSPENDED".equalsIgnoreCase(
                securityStatus
        )) {

            auth.signOut();

            showLoginScreen();

            toast(
                    "This account is suspended."
            );

            return;
        }

        String role =
                document.getString(
                        "role"
                );

        /*
         * LEGACY ACCOUNT REPAIR
         *
         * Older Sakay Na accounts may not have
         * a role field.
         *
         * The selected login role is used to repair
         * the missing role.
         *
         * ADMIN is NEVER automatically assigned.
         */

        if (role == null
                || role.trim().isEmpty()) {

            String selected =
                    selectedRole == null
                            ? "PASSENGER"
                            : selectedRole
                            .trim()
                            .toUpperCase(
                                    Locale.US
                            );

            if ("DRIVER".equals(
                    selected
            )) {

                role = "DRIVER";

            } else {

                role = "PASSENGER";
            }

            Map<String, Object> repairedRole =
                    new HashMap<>();

            repairedRole.put(
                    "role",
                    role
            );

            repairedRole.put(
                    "roleRepairedAt",
                    FieldValue.serverTimestamp()
            );

            db.collection("users")
                    .document(
                            document.getId()
                    )
                    .set(
                            repairedRole,
                            SetOptions.merge()
                    )
                    .addOnFailureListener(
                            e -> {
                                // Routing can continue even if
                                // the legacy repair write fails.
                            }
                    );
        }

        role =
                role.trim()
                        .toUpperCase(
                                Locale.US
                        );

        if ("DRIVER".equals(role)) {

            openScreen(
                    DriverActivity.class
            );

            return;
        }

        if ("PASSENGER".equals(role)) {

            openScreen(
                    PassengerActivity.class
            );

            return;
        }

        if ("ADMIN".equals(role)) {

            openScreen(
                    AdminActivity.class
            );

            return;
        }

        loginButton.setEnabled(true);

        showLoginError(
                "Unknown account role: "
                        + role
        );
    }

    private void openRegistration() {

        /*
         * Keep the existing registration flow.
         *
         * Driver/Passenger registration remains
         * controlled by the existing application.
         */
        try {

            Intent intent =
                    new Intent(
                            MainActivity.this,
                            DriverOnboardingActivity.class
                    );

            intent.putExtra(
                    "registration",
                    true
            );

            startActivity(intent);

        } catch (Exception e) {

            toast(
                    "Registration screen unavailable."
            );
        }
    }

    private void openScreen(
            Class<?> target
    ) {

        Intent intent =
                new Intent(
                        MainActivity.this,
                        target
                );

        intent.addFlags(
                Intent.FLAG_ACTIVITY_NEW_TASK
                        | Intent.FLAG_ACTIVITY_CLEAR_TASK
        );

        startActivity(intent);

        finish();
    }

    private String normalizePhone(
            String input
    ) {

        if (input == null) {
            return "";
        }

        String value =
                input.trim()
                        .replace(
                                " ",
                                ""
                        )
                        .replace(
                                "-",
                                ""
                        )
                        .replace(
                                "(",
                                ""
                        )
                        .replace(
                                ")",
                                ""
                        );

        if (value.startsWith("+63")) {

            value =
                    "63"
                            + value.substring(3);

        } else if (value.startsWith("09")) {

            value =
                    "63"
                            + value.substring(1);

        } else if (value.startsWith("9")
                && value.length() == 10) {

            value =
                    "63"
                            + value;

        }

        if (!value.matches(
                "63[0-9]{10}"
        )) {

            return "";
        }

        return value;
    }

    private void showLoginError(
            String message
    ) {

        statusText.setText(
                message
        );

        toast(message);
    }

    private void toast(
            String message
    ) {

        Toast.makeText(
                this,
                message,
                Toast.LENGTH_LONG
        ).show();
    }

    private void requestNotificationPermission() {

        if (Build.VERSION.SDK_INT >= 33) {

            if (ActivityCompat.checkSelfPermission(
                    this,
                    Manifest.permission.POST_NOTIFICATIONS
            ) != PackageManager.PERMISSION_GRANTED) {

                ActivityCompat.requestPermissions(
                        this,
                        new String[]{
                                Manifest.permission.POST_NOTIFICATIONS
                        },
                        NOTIFICATION_PERMISSION_REQUEST
                );
            }
        }
    }

    @Override
    public void onRequestPermissionsResult(
            int requestCode,
            @NonNull String[] permissions,
            @NonNull int[] grantResults
    ) {

        super.onRequestPermissionsResult(
                requestCode,
                permissions,
                grantResults
        );
    }
}

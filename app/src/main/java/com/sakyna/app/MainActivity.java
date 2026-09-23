
package com.sakyna.app;

import android.Manifest;
import android.app.Activity;
import android.content.Intent;
import android.content.pm.PackageManager;
import android.graphics.Color;
import android.graphics.Typeface;
import android.os.Build;
import android.os.Bundle;
import android.text.InputType;
import android.view.Gravity;
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
                40,
                40,
                35
        );

        root.setBackgroundColor(
                Color.rgb(238, 248, 243)
        );

        TextView title =
                new TextView(this);

        title.setText("🛺 SAKAY NA");
        title.setTextSize(32);
        title.setTextColor(Color.WHITE);
        title.setTypeface(
                null,
                Typeface.BOLD
        );
        title.setGravity(Gravity.CENTER);
        title.setPadding(10, 22, 10, 22);
        title.setBackgroundColor(
                Color.rgb(0, 125, 75)
        );

        root.addView(
                title,
                new LinearLayout.LayoutParams(
                        -1,
                        -2
                )
        );

        TextView subtitle =
                new TextView(this);

        subtitle.setText(
                "Tricycle Ride Booking"
        );

        subtitle.setTextSize(19);
        subtitle.setTextColor(
                Color.rgb(0, 110, 65)
        );
        subtitle.setTypeface(
                null,
                Typeface.BOLD
        );
        subtitle.setGravity(Gravity.CENTER);
        subtitle.setPadding(0, 25, 0, 25);

        root.addView(subtitle);

        phoneField =
                new EditText(this);

        phoneField.setHint(
                "📱 Phone Number"
        );

        phoneField.setTextSize(17);

        phoneField.setSingleLine(true);

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
                "🔒 Password"
        );

        passwordField.setTextSize(17);

        passwordField.setSingleLine(true);

        passwordField.setInputType(
                InputType.TYPE_CLASS_TEXT
                        | InputType.TYPE_TEXT_VARIATION_PASSWORD
        );

        LinearLayout.LayoutParams passwordParams =
                new LinearLayout.LayoutParams(
                        -1,
                        -2
                );

        passwordParams.topMargin = 12;

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

        roleLabel.setTextColor(
                Color.rgb(0, 110, 65)
        );

        roleLabel.setTypeface(
                null,
                Typeface.BOLD
        );

        roleLabel.setGravity(
                Gravity.CENTER
        );

        LinearLayout.LayoutParams roleLabelParams =
                new LinearLayout.LayoutParams(
                        -1,
                        -2
                );

        roleLabelParams.topMargin = 22;

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
                createRoleButton(
                        "PASSENGER"
                );

        driverButton =
                createRoleButton(
                        "DRIVER"
                );

        adminButton =
                createRoleButton(
                        "ADMIN"
                );

        roleRow.addView(
                passengerButton,
                roleButtonParams()
        );

        roleRow.addView(
                driverButton,
                roleButtonParams()
        );

        roleRow.addView(
                adminButton,
                roleButtonParams()
        );

        root.addView(roleRow);

        loginButton =
                new Button(this);

        loginButton.setText(
                "🚀 LOGIN"
        );

        loginButton.setTextSize(17);

        loginButton.setTextColor(
                Color.WHITE
        );

        loginButton.setTypeface(
                null,
                Typeface.BOLD
        );

        loginButton.setAllCaps(false);

        loginButton.setBackgroundColor(
                Color.rgb(0, 150, 80)
        );

        LinearLayout.LayoutParams loginParams =
                new LinearLayout.LayoutParams(
                        -1,
                        -2
                );

        loginParams.topMargin = 24;

        root.addView(
                loginButton,
                loginParams
        );

        registerButton =
                new Button(this);

        registerButton.setText(
                "✨ CREATE ACCOUNT"
        );

        registerButton.setTextSize(16);

        registerButton.setTextColor(
                Color.WHITE
        );

        registerButton.setTypeface(
                null,
                Typeface.BOLD
        );

        registerButton.setAllCaps(false);

        registerButton.setBackgroundColor(
                Color.rgb(40, 115, 190)
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
                "📱 Phone number + password only"
        );

        statusText.setTextSize(14);

        statusText.setTextColor(
                Color.rgb(0, 110, 65)
        );

        statusText.setTypeface(
                null,
                Typeface.BOLD
        );

        statusText.setGravity(
                Gravity.CENTER
        );

        statusText.setPadding(
                0,
                20,
                0,
                0
        );

        root.addView(statusText);

        setContentView(root);

        selectedRole = "PASSENGER";

        updateRoleButtons();

        passengerButton.setOnClickListener(
                v -> {

                    selectedRole =
                            "PASSENGER";

                    updateRoleButtons();

                    statusText.setText(
                            "Passenger selected"
                    );
                }
        );

        driverButton.setOnClickListener(
                v -> {

                    selectedRole =
                            "DRIVER";

                    updateRoleButtons();

                    statusText.setText(
                            "Driver selected"
                    );
                }
        );

        adminButton.setOnClickListener(
                v -> {

                    selectedRole =
                            "ADMIN";

                    updateRoleButtons();

                    statusText.setText(
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

    private Button createRoleButton(
            String text
    ) {

        Button button =
                new Button(this);

        button.setText(text);
        button.setTextSize(12);
        button.setAllCaps(false);
        button.setTypeface(
                null,
                Typeface.BOLD
        );

        return button;
    }

    private LinearLayout.LayoutParams roleButtonParams() {

        LinearLayout.LayoutParams params =
                new LinearLayout.LayoutParams(
                        0,
                        -2,
                        1
                );

        params.setMargins(
                3,
                5,
                3,
                5
        );

        return params;
    }

    private void updateRoleButtons() {

        if (passengerButton == null
                || driverButton == null
                || adminButton == null) {
            return;
        }

        passengerButton.setTextColor(
                Color.WHITE
        );

        driverButton.setTextColor(
                Color.WHITE
        );

        adminButton.setTextColor(
                Color.WHITE
        );

        passengerButton.setBackgroundColor(
                Color.rgb(0, 125, 75)
        );

        driverButton.setBackgroundColor(
                Color.rgb(40, 115, 190)
        );

        adminButton.setBackgroundColor(
                Color.rgb(145, 75, 160)
        );

        if ("PASSENGER".equals(
                selectedRole
        )) {

            passengerButton.setBackgroundColor(
                    Color.rgb(0, 175, 90)
            );

        } else if ("DRIVER".equals(
                selectedRole
        )) {

            driverButton.setBackgroundColor(
                    Color.rgb(25, 135, 220)
            );

        } else if ("ADMIN".equals(
                selectedRole
        )) {

            adminButton.setBackgroundColor(
                    Color.rgb(180, 75, 190)
            );
        }
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
         * Passenger and Driver accounts can have
         * their missing role repaired.
         *
         * ADMIN IS NEVER converted to PASSENGER.
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

            } else if ("PASSENGER".equals(
                    selected
            )) {

                role = "PASSENGER";

            } else if ("ADMIN".equals(
                    selected
            )) {

                loginButton.setEnabled(true);

                showLoginError(
                        "Admin account role is missing. Please contact the administrator."
                );

                return;

            } else {

                loginButton.setEnabled(true);

                showLoginError(
                        "Invalid account role."
                );

                return;
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
                                // Continue routing.
                            }
                    );
        }

        role =
                role.trim()
                        .toUpperCase(
                                Locale.US
                        );

        /*
         * ADMIN MUST BE CHECKED BEFORE
         * PASSENGER/DRIVER FALLBACK.
         */

        if ("ADMIN".equals(role)) {

            openScreen(
                    AdminActivity.class
            );

            return;
        }

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

        loginButton.setEnabled(true);

        showLoginError(
                "Unknown account role: "
                        + role
        );
    }

    private void openRegistration() {

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

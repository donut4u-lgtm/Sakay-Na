
package com.sakyna.app;

import android.app.Activity;
import android.content.Intent;
import android.graphics.Color;
import android.os.Bundle;
import android.text.InputType;
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

public class MainActivity extends Activity {

    private FirebaseAuth auth;
    private FirebaseFirestore db;

    private EditText phoneField;
    private EditText passwordField;

    private Button passengerButton;
    private Button driverButton;
    private Button adminButton;

    private String selectedRole = "PASSENGER";

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);

        auth = FirebaseAuth.getInstance();
        db = FirebaseFirestore.getInstance();

        /*
         * IMPORTANT:
         *
         * DO NOT sign out here.
         *
         * Passenger/Driver Back can return to MainActivity.
         * If Firebase still has a valid user, we route that
         * user back to the correct home screen.
         *
         * Logout is handled separately by the Logout buttons.
         */
        FirebaseUser currentUser = auth.getCurrentUser();

        if (currentUser != null) {
            openAuthenticatedHome(currentUser);
        } else {
            buildLoginScreen();
        }
    }

    private void openAuthenticatedHome(FirebaseUser user) {

        db.collection("users")
                .document(user.getUid())
                .get()
                .addOnSuccessListener(document -> {

                    if (!document.exists()) {

                        auth.signOut();
                        buildLoginScreen();

                        Toast.makeText(
                                this,
                                "User profile was not found.",
                                Toast.LENGTH_LONG
                        ).show();

                        return;
                    }

                    String role =
                            document.getString("role");

                    if (role == null) {
                        auth.signOut();
                        buildLoginScreen();

                        Toast.makeText(
                                this,
                                "User role was not found.",
                                Toast.LENGTH_LONG
                        ).show();

                        return;
                    }

                    role = role.toUpperCase();

                    if (role.equals("ADMIN")) {

                        openActivity(
                                AdminActivity.class
                        );

                        return;
                    }

                    if (role.equals("DRIVER")) {

                        Boolean approved =
                                document.getBoolean("approved");

                        if (approved == null) {
                            approved = false;
                        }

                        if (!approved) {

                            auth.signOut();
                            buildLoginScreen();

                            Toast.makeText(
                                    this,
                                    "Driver account is waiting for Admin approval.",
                                    Toast.LENGTH_LONG
                            ).show();

                            return;
                        }

                        openActivity(
                                DriverActivity.class
                        );

                        return;
                    }

                    if (role.equals("PASSENGER")) {

                        openActivity(
                                PassengerActivity.class
                        );

                        return;
                    }

                    auth.signOut();
                    buildLoginScreen();

                    Toast.makeText(
                            this,
                            "Unknown account role.",
                            Toast.LENGTH_LONG
                    ).show();
                })
                .addOnFailureListener(e -> {

                    /*
                     * Do not silently log the user out because of
                     * a temporary Firestore/network failure.
                     *
                     * If the user is coming Back from PassengerActivity,
                     * keeping Firebase authentication is important.
                     */
                    Toast.makeText(
                            this,
                            "Unable to load account profile: "
                                    + e.getMessage(),
                            Toast.LENGTH_LONG
                    ).show();

                    buildLoginScreen();
                });
    }

    private void buildLoginScreen() {

        LinearLayout root =
                new LinearLayout(this);

        root.setOrientation(
                LinearLayout.VERTICAL
        );

        root.setPadding(
                35,
                40,
                35,
                35
        );

        root.setGravity(
                Gravity.CENTER_HORIZONTAL
        );

        root.setBackgroundColor(
                Color.WHITE
        );

        TextView logo =
                new TextView(this);

        logo.setText("SAKAY NA");
        logo.setTextSize(32);
        logo.setTextColor(
                Color.rgb(0, 120, 70)
        );

        logo.setGravity(
                Gravity.CENTER
        );

        logo.setPadding(
                0,
                20,
                0,
                10
        );

        root.addView(logo);

        TextView subtitle =
                new TextView(this);

        subtitle.setText(
                "Ride anywhere. Sakay Na."
        );

        subtitle.setTextSize(17);
        subtitle.setTextColor(
                Color.DKGRAY
        );

        subtitle.setGravity(
                Gravity.CENTER
        );

        subtitle.setPadding(
                0,
                0,
                0,
                30
        );

        root.addView(subtitle);

        TextView roleLabel =
                new TextView(this);

        roleLabel.setText(
                "SELECT ACCOUNT TYPE"
        );

        roleLabel.setTextSize(15);
        roleLabel.setTextColor(
                Color.DKGRAY
        );

        roleLabel.setGravity(
                Gravity.CENTER
        );

        roleLabel.setPadding(
                0,
                10,
                0,
                10
        );

        root.addView(roleLabel);

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
                        LinearLayout.LayoutParams.WRAP_CONTENT,
                        1
                )
        );

        roleRow.addView(
                driverButton,
                new LinearLayout.LayoutParams(
                        0,
                        LinearLayout.LayoutParams.WRAP_CONTENT,
                        1
                )
        );

        roleRow.addView(
                adminButton,
                new LinearLayout.LayoutParams(
                        0,
                        LinearLayout.LayoutParams.WRAP_CONTENT,
                        1
                )
        );

        root.addView(roleRow);

        passengerButton.setOnClickListener(
                v -> selectRole("PASSENGER")
        );

        driverButton.setOnClickListener(
                v -> selectRole("DRIVER")
        );

        adminButton.setOnClickListener(
                v -> selectRole("ADMIN")
        );

        selectRole("PASSENGER");

        TextView phoneLabel =
                new TextView(this);

        phoneLabel.setText(
                "PHONE NUMBER"
        );

        phoneLabel.setTextSize(14);
        phoneLabel.setTextColor(
                Color.DKGRAY
        );

        phoneLabel.setPadding(
                0,
                30,
                0,
                5
        );

        root.addView(phoneLabel);

        phoneField =
                new EditText(this);

        phoneField.setHint(
                "09XXXXXXXXX"
        );

        phoneField.setTextSize(18);

        phoneField.setSingleLine(
                true
        );

        phoneField.setInputType(
                InputType.TYPE_CLASS_PHONE
        );

        root.addView(phoneField);

        TextView passwordLabel =
                new TextView(this);

        passwordLabel.setText(
                "PASSWORD"
        );

        passwordLabel.setTextSize(14);
        passwordLabel.setTextColor(
                Color.DKGRAY
        );

        passwordLabel.setPadding(
                0,
                20,
                0,
                5
        );

        root.addView(passwordLabel);

        passwordField =
                new EditText(this);

        passwordField.setHint(
                "Password"
        );

        passwordField.setTextSize(18);

        passwordField.setSingleLine(
                true
        );

        passwordField.setInputType(
                InputType.TYPE_CLASS_TEXT |
                InputType.TYPE_TEXT_VARIATION_PASSWORD
        );

        root.addView(passwordField);

        Button loginButton =
                new Button(this);

        loginButton.setText(
                "LOGIN"
        );

        loginButton.setTextSize(18);

        loginButton.setOnClickListener(
                v -> login()
        );

        root.addView(
                loginButton,
                new LinearLayout.LayoutParams(
                        LinearLayout.LayoutParams.MATCH_PARENT,
                        LinearLayout.LayoutParams.WRAP_CONTENT
                )
        );

        Button registerButton =
                new Button(this);

        registerButton.setText(
                "CREATE ACCOUNT"
        );

        registerButton.setTextSize(17);

        registerButton.setOnClickListener(
                v -> register()
        );

        root.addView(
                registerButton,
                new LinearLayout.LayoutParams(
                        LinearLayout.LayoutParams.MATCH_PARENT,
                        LinearLayout.LayoutParams.WRAP_CONTENT
                )
        );

        TextView info =
                new TextView(this);

        info.setText(
                "\nPhone number + password only\n" +
                "No email • No OTP"
        );

        info.setTextSize(14);

        info.setTextColor(
                Color.GRAY
        );

        info.setGravity(
                Gravity.CENTER
        );

        root.addView(info);

        setContentView(root);
    }

    private void selectRole(String role) {

        selectedRole = role;

        passengerButton.setText(
                "PASSENGER"
        );

        driverButton.setText(
                "DRIVER"
        );

        adminButton.setText(
                "ADMIN"
        );

        passengerButton.setTextColor(
                Color.DKGRAY
        );

        driverButton.setTextColor(
                Color.DKGRAY
        );

        adminButton.setTextColor(
                Color.DKGRAY
        );

        if (role.equals("PASSENGER")) {

            passengerButton.setText(
                    "✓ PASSENGER"
            );

            passengerButton.setTextColor(
                    Color.rgb(0, 120, 70)
            );
        }

        if (role.equals("DRIVER")) {

            driverButton.setText(
                    "✓ DRIVER"
            );

            driverButton.setTextColor(
                    Color.rgb(0, 120, 70)
            );
        }

        if (role.equals("ADMIN")) {

            adminButton.setText(
                    "✓ ADMIN"
            );

            adminButton.setTextColor(
                    Color.rgb(0, 120, 70)
            );
        }
    }

    private String normalizePhone(String input) {

        if (input == null) {
            return "";
        }

        String phone =
                input.replaceAll(
                        "[^0-9]",
                        ""
                );

        if (phone.startsWith("0")) {

            phone =
                    "63" +
                    phone.substring(1);
        }

        if (phone.startsWith("9")
                && phone.length() == 10) {

            phone =
                    "63" +
                    phone;
        }

        return phone;
    }

    /*
     * Firebase uses the internal identifier only.
     *
     * The user NEVER enters an email address.
     *
     * User authentication remains:
     *
     * PHONE NUMBER + PASSWORD
     */
    private String firebaseIdentifier(
            String phone
    ) {

        return phone + "@sakyna.app";
    }

    private boolean validateFields() {

        String phone =
                normalizePhone(
                        phoneField
                                .getText()
                                .toString()
                );

        String password =
                passwordField
                        .getText()
                        .toString();

        if (phone.length() < 11) {

            Toast.makeText(
                    this,
                    "Enter a valid Philippine phone number.",
                    Toast.LENGTH_LONG
            ).show();

            return false;
        }

        if (password.length() < 6) {

            Toast.makeText(
                    this,
                    "Password must be at least 6 characters.",
                    Toast.LENGTH_LONG
            ).show();

            return false;
        }

        return true;
    }

    private void login() {

        if (!validateFields()) {
            return;
        }

        String phone =
                normalizePhone(
                        phoneField
                                .getText()
                                .toString()
                );

        String password =
                passwordField
                        .getText()
                        .toString();

        String identifier =
                firebaseIdentifier(phone);

        auth.signInWithEmailAndPassword(
                        identifier,
                        password
                )
                .addOnSuccessListener(result -> {

                    FirebaseUser user =
                            auth.getCurrentUser();

                    if (user == null) {

                        showError(
                                "Login failed. Please try again."
                        );

                        return;
                    }

                    loadUserProfile(
                            user.getUid(),
                            phone
                    );
                })
                .addOnFailureListener(e -> {

                    String message =
                            e.getMessage();

                    if (message == null) {

                        message =
                                "Invalid phone number or password.";
                    }

                    Toast.makeText(
                            this,
                            message,
                            Toast.LENGTH_LONG
                    ).show();
                });
    }

    private void loadUserProfile(
            String uid,
            String phone
    ) {

        db.collection("users")
                .document(uid)
                .get()
                .addOnSuccessListener(document -> {

                    if (!document.exists()) {

                        auth.signOut();

                        showError(
                                "User profile was not found."
                        );

                        return;
                    }

                    String storedRole =
                            document.getString(
                                    "role"
                            );

                    if (storedRole == null) {
                        storedRole = "";
                    }

                    storedRole =
                            storedRole.toUpperCase();

                    if (!storedRole.equals(
                            selectedRole
                    )) {

                        auth.signOut();

                        Toast.makeText(
                                this,
                                "This account is registered as "
                                        + storedRole
                                        + ". Select "
                                        + storedRole
                                        + " to continue.",
                                Toast.LENGTH_LONG
                        ).show();

                        return;
                    }

                    if (selectedRole.equals(
                            "ADMIN"
                    )) {

                        openActivity(
                                AdminActivity.class
                        );

                        return;
                    }

                    if (selectedRole.equals(
                            "DRIVER"
                    )) {

                        Boolean approved =
                                document.getBoolean(
                                        "approved"
                                );

                        if (approved == null) {
                            approved = false;
                        }

                        if (!approved) {

                            auth.signOut();

                            Toast.makeText(
                                    this,
                                    "Driver account is waiting for Admin approval.",
                                    Toast.LENGTH_LONG
                            ).show();

                            return;
                        }

                        openActivity(
                                DriverActivity.class
                        );

                        return;
                    }

                    openActivity(
                            PassengerActivity.class
                    );
                })
                .addOnFailureListener(e -> {

                    auth.signOut();

                    showError(
                            "Unable to load user profile: "
                                    + e.getMessage()
                    );
                });
    }

    private void register() {

        if (!validateFields()) {
            return;
        }

        if (selectedRole.equals(
                "ADMIN"
        )) {

            Toast.makeText(
                    this,
                    "Admin accounts cannot be created here.",
                    Toast.LENGTH_LONG
            ).show();

            return;
        }

        String phone =
                normalizePhone(
                        phoneField
                                .getText()
                                .toString()
                );

        String password =
                passwordField
                        .getText()
                        .toString();

        String identifier =
                firebaseIdentifier(phone);

        auth.createUserWithEmailAndPassword(
                        identifier,
                        password
                )
                .addOnSuccessListener(result -> {

                    FirebaseUser user =
                            auth.getCurrentUser();

                    if (user == null) {

                        showError(
                                "Account creation failed."
                        );

                        return;
                    }

                    String uid =
                            user.getUid();

                    Map<String, Object> profile =
                            new HashMap<>();

                    profile.put(
                            "phone",
                            phone
                    );

                    profile.put(
                            "role",
                            selectedRole
                    );

                    profile.put(
                            "approved",
                            selectedRole.equals(
                                    "PASSENGER"
                            )
                    );

                    profile.put(
                            "createdAt",
                            com.google.firebase.firestore.FieldValue
                                    .serverTimestamp()
                    );

                    db.collection("users")
                            .document(uid)
                            .set(profile)
                            .addOnSuccessListener(v -> {

                                auth.signOut();

                                Toast.makeText(
                                        this,
                                        "Account created successfully. Please login.",
                                        Toast.LENGTH_LONG
                                ).show();

                                phoneField.setText(
                                        phone
                                );

                                passwordField.setText("");

                                selectRole(
                                        selectedRole
                                );
                            })
                            .addOnFailureListener(e -> {

                                auth.signOut();

                                showError(
                                        "Account created but profile failed: "
                                                + e.getMessage()
                                );
                            });
                })
                .addOnFailureListener(e -> {

                    String message =
                            e.getMessage();

                    if (message == null) {

                        message =
                                "Unable to create account.";
                    }

                    Toast.makeText(
                            this,
                            message,
                            Toast.LENGTH_LONG
                    ).show();
                });
    }

    private void openActivity(
            Class<?> activityClass
    ) {

        Intent intent =
                new Intent(
                        this,
                        activityClass
                );

        startActivity(intent);
        finish();
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
}

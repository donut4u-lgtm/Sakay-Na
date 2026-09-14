
package com.sakyna.app;

import android.app.Activity;
import android.content.Intent;
import android.graphics.Color;
import android.os.Bundle;
import android.text.InputType;
import android.view.Gravity;
import android.view.View;
import android.widget.Button;
import android.widget.EditText;
import android.widget.LinearLayout;
import android.widget.ProgressBar;
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

    private EditText emailInput;
    private EditText passwordInput;

    private TextView modeText;

    private Button actionButton;
    private Button switchButton;

    private ProgressBar progressBar;

    private boolean registerMode = false;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);

        auth = FirebaseAuth.getInstance();

        db = FirebaseFirestore.getInstance();

        showLoginScreen();

        checkExistingUser();
    }

    private void checkExistingUser() {

        FirebaseUser user =
                auth.getCurrentUser();

        if (user == null) {
            return;
        }

        showLoading(
                "Checking account..."
        );

        loadUserRole(
                user.getUid()
        );
    }

    private void showLoginScreen() {

        registerMode = false;

        LinearLayout root =
                createRoot();

        TextView title =
                text(
                        "🛺 SAKAY NA",
                        30,
                        Color.rgb(20, 20, 20)
                );

        title.setGravity(
                Gravity.CENTER
        );

        root.addView(title);

        TextView subtitle =
                text(
                        "Ride anywhere. Ride safely.",
                        16,
                        Color.DKGRAY
                );

        subtitle.setGravity(
                Gravity.CENTER
        );

        root.addView(subtitle);

        modeText =
                text(
                        "LOGIN",
                        22,
                        Color.rgb(20, 120, 70)
                );

        modeText.setGravity(
                Gravity.CENTER
        );

        root.addView(modeText);

        emailInput =
                new EditText(this);

        emailInput.setHint(
                "Email"
        );

        emailInput.setInputType(
                InputType.TYPE_CLASS_TEXT |
                InputType.TYPE_TEXT_VARIATION_EMAIL_ADDRESS
        );

        root.addView(
                emailInput,
                inputParams()
        );

        passwordInput =
                new EditText(this);

        passwordInput.setHint(
                "Password"
        );

        passwordInput.setInputType(
                InputType.TYPE_CLASS_TEXT |
                InputType.TYPE_TEXT_VARIATION_PASSWORD
        );

        root.addView(
                passwordInput,
                inputParams()
        );

        actionButton =
                button(
                        "LOGIN"
                );

        actionButton.setOnClickListener(
                v -> performLogin()
        );

        root.addView(actionButton);

        switchButton =
                button(
                        "CREATE NEW ACCOUNT"
                );

        switchButton.setOnClickListener(
                v -> showRegisterScreen()
        );

        root.addView(switchButton);

        progressBar =
                new ProgressBar(this);

        progressBar.setVisibility(
                View.GONE
        );

        root.addView(progressBar);

        setContentView(root);
    }

    private void showRegisterScreen() {

        registerMode = true;

        LinearLayout root =
                createRoot();

        TextView title =
                text(
                        "🛺 SAKAY NA",
                        30,
                        Color.rgb(20, 20, 20)
                );

        title.setGravity(
                Gravity.CENTER
        );

        root.addView(title);

        TextView subtitle =
                text(
                        "Create your Sakay Na account",
                        16,
                        Color.DKGRAY
                );

        subtitle.setGravity(
                Gravity.CENTER
        );

        root.addView(subtitle);

        modeText =
                text(
                        "REGISTER",
                        22,
                        Color.rgb(20, 120, 70)
                );

        modeText.setGravity(
                Gravity.CENTER
        );

        root.addView(modeText);

        emailInput =
                new EditText(this);

        emailInput.setHint(
                "Email"
        );

        emailInput.setInputType(
                InputType.TYPE_CLASS_TEXT |
                InputType.TYPE_TEXT_VARIATION_EMAIL_ADDRESS
        );

        root.addView(
                emailInput,
                inputParams()
        );

        passwordInput =
                new EditText(this);

        passwordInput.setHint(
                "Password - minimum 6 characters"
        );

        passwordInput.setInputType(
                InputType.TYPE_CLASS_TEXT |
                InputType.TYPE_TEXT_VARIATION_PASSWORD
        );

        root.addView(
                passwordInput,
                inputParams()
        );

        actionButton =
                button(
                        "CREATE PASSENGER ACCOUNT"
                );

        actionButton.setOnClickListener(
                v -> registerAccount(
                        "PASSENGER"
                )
        );

        root.addView(actionButton);

        Button driverButton =
                button(
                        "CREATE DRIVER ACCOUNT"
                );

        driverButton.setOnClickListener(
                v -> registerAccount(
                        "DRIVER"
                )
        );

        root.addView(driverButton);

        switchButton =
                button(
                        "BACK TO LOGIN"
                );

        switchButton.setOnClickListener(
                v -> showLoginScreen()
        );

        root.addView(switchButton);

        progressBar =
                new ProgressBar(this);

        progressBar.setVisibility(
                View.GONE
        );

        root.addView(progressBar);

        TextView info =
                text(
                        "🛺 Driver accounts require " +
                        "Admin approval before the driver " +
                        "can accept rides.",
                        13,
                        Color.GRAY
                );

        info.setGravity(
                Gravity.CENTER
        );

        root.addView(info);

        setContentView(root);
    }

    private void performLogin() {

        String email =
                emailInput
                        .getText()
                        .toString()
                        .trim();

        String password =
                passwordInput
                        .getText()
                        .toString();

        if (email.isEmpty()) {

            showMessage(
                    "Enter your email."
            );

            return;
        }

        if (password.isEmpty()) {

            showMessage(
                    "Enter your password."
            );

            return;
        }

        showLoading(
                "Signing in..."
        );

        auth.signInWithEmailAndPassword(
                email,
                password
        ).addOnCompleteListener(
                task -> {

                    if (!task.isSuccessful()) {

                        hideLoading();

                        String message =
                                "Login failed.";

                        if (task.getException() != null) {

                            message =
                                    task.getException()
                                            .getMessage();
                        }

                        showMessage(message);

                        return;
                    }

                    FirebaseUser user =
                            auth.getCurrentUser();

                    if (user == null) {

                        hideLoading();

                        showMessage(
                                "Account not found."
                        );

                        return;
                    }

                    loadUserRole(
                            user.getUid()
                    );
                }
        );
    }

    private void registerAccount(
            String role) {

        String email =
                emailInput
                        .getText()
                        .toString()
                        .trim();

        String password =
                passwordInput
                        .getText()
                        .toString();

        if (email.isEmpty()) {

            showMessage(
                    "Enter your email."
            );

            return;
        }

        if (password.length() < 6) {

            showMessage(
                    "Password must be at least 6 characters."
            );

            return;
        }

        showLoading(
                "Creating account..."
        );

        auth.createUserWithEmailAndPassword(
                email,
                password
        ).addOnCompleteListener(
                task -> {

                    if (!task.isSuccessful()) {

                        hideLoading();

                        String message =
                                "Registration failed.";

                        if (task.getException() != null) {

                            message =
                                    task.getException()
                                            .getMessage();
                        }

                        showMessage(message);

                        return;
                    }

                    FirebaseUser user =
                            auth.getCurrentUser();

                    if (user == null) {

                        hideLoading();

                        showMessage(
                                "Account creation failed."
                        );

                        return;
                    }

                    saveUserProfile(
                            user,
                            role
                    );
                }
        );
    }

    private void saveUserProfile(
            FirebaseUser user,
            String role) {

        String uid =
                user.getUid();

        Map<String, Object> profile =
                new HashMap<>();

        profile.put(
                "uid",
                uid
        );

        profile.put(
                "email",
                user.getEmail()
        );

        profile.put(
                "role",
                role
        );

        profile.put(
                "active",
                true
        );

        /*
         * Passenger:
         * immediately approved.
         *
         * Driver:
         * MUST wait for Admin approval.
         */
        profile.put(
                "approved",
                role.equals("PASSENGER")
        );

        /*
         * Driver onboarding state.
         */
        if (role.equals("DRIVER")) {

            profile.put(
                    "driverStatus",
                    "PENDING_APPROVAL"
            );

            profile.put(
                    "canAcceptRides",
                    false
            );

        } else {

            profile.put(
                    "driverStatus",
                    "NOT_DRIVER"
            );

            profile.put(
                    "canAcceptRides",
                    false
            );
        }

        profile.put(
                "createdAt",
                com.google.firebase.firestore.FieldValue
                        .serverTimestamp()
        );

        db.collection("users")
                .document(uid)
                .set(profile)
                .addOnSuccessListener(
                        unused -> {

                            if (role.equals(
                                    "DRIVER"
                            )) {

                                auth.signOut();

                                hideLoading();

                                showLoginScreen();

                                showMessage(
                                        "🟡 Driver account created. " +
                                        "Wait for Admin approval."
                                );

                            } else {

                                openPassenger();
                            }
                        }
                )
                .addOnFailureListener(
                        e -> {

                            hideLoading();

                            showMessage(
                                    "Profile save failed: " +
                                    e.getMessage()
                            );
                        }
                );
    }

    private void loadUserRole(
            String uid) {

        db.collection("users")
                .document(uid)
                .get()
                .addOnSuccessListener(
                        document -> {

                            if (!document.exists()) {

                                hideLoading();

                                showMessage(
                                        "User profile not found."
                                );

                                auth.signOut();

                                showLoginScreen();

                                return;
                            }

                            String role =
                                    document.getString(
                                            "role"
                                    );

                            Boolean active =
                                    document.getBoolean(
                                            "active"
                                    );

                            if (active != null &&
                                    !active) {

                                hideLoading();

                                showMessage(
                                        "⛔ This account is disabled."
                                );

                                auth.signOut();

                                showLoginScreen();

                                return;
                            }

                            if (role == null) {

                                hideLoading();

                                showMessage(
                                        "Account role is missing."
                                );

                                auth.signOut();

                                showLoginScreen();

                                return;
                            }

                            if (role.equals(
                                    "DRIVER"
                            )) {

                                checkDriverApproval(
                                        document
                                );

                                return;
                            }

                            switch (role) {

                                case "PASSENGER":

                                    openPassenger();

                                    break;

                                case "ADMIN":

                                    openAdmin();

                                    break;

                                default:

                                    hideLoading();

                                    showMessage(
                                            "Unknown account role."
                                    );

                                    auth.signOut();

                                    showLoginScreen();
                            }
                        }
                )
                .addOnFailureListener(
                        e -> {

                            hideLoading();

                            showMessage(
                                    "Unable to load account: " +
                                    e.getMessage()
                            );
                        }
                );
    }

    private void checkDriverApproval(
            com.google.firebase.firestore.DocumentSnapshot document) {

        Boolean approved =
                document.getBoolean(
                        "approved"
                );

        Boolean canAccept =
                document.getBoolean(
                        "canAcceptRides"
                );

        String driverStatus =
                document.getString(
                        "driverStatus"
                );

        boolean isApproved =
                approved != null &&
                approved;

        boolean canOperate =
                canAccept != null &&
                canAccept;

        if (!isApproved ||
                !canOperate ||
                !"APPROVED".equals(
                        driverStatus
                )) {

            hideLoading();

            showDriverPendingScreen(
                    driverStatus
            );

            return;
        }

        openDriver();
    }

    private void showDriverPendingScreen(
            String status) {

        LinearLayout root =
                createRoot();

        TextView title =
                text(
                        "🛺 SAKAY NA",
                        30,
                        Color.rgb(20, 20, 20)
                );

        title.setGravity(
                Gravity.CENTER
        );

        root.addView(title);

        TextView heading =
                text(
                        "🟡 DRIVER APPROVAL PENDING",
                        22,
                        Color.rgb(180, 120, 0)
                );

        heading.setGravity(
                Gravity.CENTER
        );

        root.addView(heading);

        String displayStatus =
                status == null ||
                status.isEmpty()
                        ? "PENDING_APPROVAL"
                        : status;

        TextView message =
                text(
                        "Your driver account is registered.\n\n" +
                        "Current status:\n" +
                        displayStatus +
                        "\n\n" +
                        "An Admin must approve your " +
                        "driver account before you can " +
                        "accept passenger rides.",
                        16,
                        Color.DKGRAY
                );

        message.setGravity(
                Gravity.CENTER
        );

        root.addView(message);

        Button refresh =
                button(
                        "🔄 CHECK APPROVAL AGAIN"
                );

        refresh.setOnClickListener(
                v -> {

                    FirebaseUser user =
                            auth.getCurrentUser();

                    if (user == null) {

                        showLoginScreen();

                        return;
                    }

                    showLoading(
                            "Checking approval..."
                    );

                    loadUserRole(
                            user.getUid()
                    );
                }
        );

        root.addView(refresh);

        Button logout =
                button(
                        "🚪 LOGOUT"
                );

        logout.setOnClickListener(
                v -> {

                    auth.signOut();

                    showLoginScreen();
                }
        );

        root.addView(logout);

        setContentView(root);
    }

    private void openPassenger() {

        hideLoading();

        Intent intent =
                new Intent(
                        this,
                        PassengerActivity.class
                );

        startActivity(intent);

        finish();
    }

    private void openDriver() {

        hideLoading();

        Intent intent =
                new Intent(
                        this,
                        DriverActivity.class
                );

        startActivity(intent);

        finish();
    }

    private void openAdmin() {

        hideLoading();

        Intent intent =
                new Intent(
                        this,
                        AdminActivity.class
                );

        startActivity(intent);

        finish();
    }

    private void showLoading(
            String message) {

        if (progressBar != null) {

            progressBar.setVisibility(
                    View.VISIBLE
            );
        }

        if (actionButton != null) {

            actionButton.setEnabled(
                    false
            );
        }

        if (switchButton != null) {

            switchButton.setEnabled(
                    false
            );
        }

        if (modeText != null) {

            modeText.setText(
                    message
            );
        }
    }

    private void hideLoading() {

        if (progressBar != null) {

            progressBar.setVisibility(
                    View.GONE
            );
        }

        if (actionButton != null) {

            actionButton.setEnabled(
                    true
            );
        }

        if (switchButton != null) {

            switchButton.setEnabled(
                    true
            );
        }

        if (modeText != null) {

            modeText.setText(
                    registerMode
                            ? "REGISTER"
                            : "LOGIN"
            );
        }
    }

    private void showMessage(
            String message) {

        Toast.makeText(
                this,
                message,
                Toast.LENGTH_LONG
        ).show();
    }

    private LinearLayout createRoot() {

        LinearLayout root =
                new LinearLayout(this);

        root.setOrientation(
                LinearLayout.VERTICAL
        );

        root.setGravity(
                Gravity.CENTER_HORIZONTAL
        );

        root.setPadding(
                32,
                60,
                32,
                32
        );

        root.setBackgroundColor(
                Color.WHITE
        );

        return root;
    }

    private TextView text(
            String value,
            int size,
            int color) {

        TextView view =
                new TextView(this);

        view.setText(
                value
        );

        view.setTextSize(
                size
        );

        view.setTextColor(
                color
        );

        view.setPadding(
                10,
                12,
                10,
                12
        );

        return view;
    }

    private Button button(
            String label) {

        Button button =
                new Button(this);

        button.setText(
                label
        );

        button.setTextSize(
                15
        );

        button.setAllCaps(
                false
        );

        return button;
    }

    private LinearLayout.LayoutParams
    inputParams() {

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

        return params;
    }
}

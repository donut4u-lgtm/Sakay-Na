
package com.sakyna.app;

import android.app.Activity;
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

    private TextView titleText;
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

        titleText =
                text(
                        "🛺 SAKAY NA",
                        30,
                        Color.rgb(20, 20, 20)
                );

        titleText.setGravity(
                Gravity.CENTER
        );

        root.addView(
                titleText
        );

        TextView subtitle =
                text(
                        "Ride anywhere. Ride safely.",
                        16,
                        Color.DKGRAY
                );

        subtitle.setGravity(
                Gravity.CENTER
        );

        root.addView(
                subtitle
        );

        modeText =
                text(
                        "LOGIN",
                        22,
                        Color.rgb(20, 120, 70)
                );

        modeText.setGravity(
                Gravity.CENTER
        );

        root.addView(
                modeText
        );

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

        root.addView(
                actionButton
        );

        switchButton =
                button(
                        "CREATE NEW ACCOUNT"
                );

        switchButton.setOnClickListener(
                v -> showRegisterScreen()
        );

        root.addView(
                switchButton
        );

        progressBar =
                new ProgressBar(this);

        progressBar.setVisibility(
                View.GONE
        );

        root.addView(
                progressBar
        );

        TextView info =
                text(
                        "Your account determines whether " +
                        "you enter Passenger, Driver, " +
                        "or Admin.",
                        13,
                        Color.GRAY
                );

        info.setGravity(
                Gravity.CENTER
        );

        root.addView(
                info
        );

        setContentView(
                root
        );
    }

    private void showRegisterScreen() {

        registerMode = true;

        LinearLayout root =
                createRoot();

        titleText =
                text(
                        "🛺 SAKAY NA",
                        30,
                        Color.rgb(20, 20, 20)
                );

        titleText.setGravity(
                Gravity.CENTER
        );

        root.addView(
                titleText
        );

        TextView subtitle =
                text(
                        "Create your Sakay Na account",
                        16,
                        Color.DKGRAY
                );

        subtitle.setGravity(
                Gravity.CENTER
        );

        root.addView(
                subtitle
        );

        modeText =
                text(
                        "REGISTER",
                        22,
                        Color.rgb(20, 120, 70)
                );

        modeText.setGravity(
                Gravity.CENTER
        );

        root.addView(
                modeText
        );

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
                v -> performRegistration()
        );

        root.addView(
                actionButton
        );

        Button driverButton =
                button(
                        "CREATE DRIVER ACCOUNT"
                );

        driverButton.setOnClickListener(
                v -> performDriverRegistration()
        );

        root.addView(
                driverButton
        );

        switchButton =
                button(
                        "BACK TO LOGIN"
                );

        switchButton.setOnClickListener(
                v -> showLoginScreen()
        );

        root.addView(
                switchButton
        );

        progressBar =
                new ProgressBar(this);

        progressBar.setVisibility(
                View.GONE
        );

        root.addView(
                progressBar
        );

        TextView driverInfo =
                text(
                        "Driver accounts will later go " +
                        "through the Sakay Na onboarding " +
                        "and approval process.",
                        13,
                        Color.GRAY
                );

        driverInfo.setGravity(
                Gravity.CENTER
        );

        root.addView(
                driverInfo
        );

        setContentView(
                root
        );
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

                        showMessage(
                                message
                        );

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

    private void performRegistration() {

        registerAccount(
                "PASSENGER"
        );
    }

    private void performDriverRegistration() {

        registerAccount(
                "DRIVER"
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

                        showMessage(
                                message
                        );

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

        profile.put(
                "approved",
                role.equals("PASSENGER")
        );

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

                                showMessage(
                                        "Driver account created."
                                );

                                showLoginScreen();

                            } else {

                                openPassenger();
                            }
                        }
                )
                .addOnFailureListener(
                        e -> {

                            hideLoading();

                            showMessage(
                                    "Account created, " +
                                    "but profile save failed: " +
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
                                        "This account is disabled."
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

                            switch (role) {

                                case "PASSENGER":

                                    openPassenger();

                                    break;

                                case "DRIVER":

                                    openDriver();

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

    private void openPassenger() {

        hideLoading();

        android.content.Intent intent =
                new android.content.Intent(
                        this,
                        PassengerActivity.class
                );

        startActivity(intent);

        finish();
    }

    private void openDriver() {

        hideLoading();

        android.content.Intent intent =
                new android.content.Intent(
                        this,
                        DriverActivity.class
                );

        startActivity(intent);

        finish();
    }

    private void openAdmin() {

        hideLoading();

        android.content.Intent intent =
                new android.content.Intent(
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

        button.setPadding(
                10,
                8,
                10,
                8
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

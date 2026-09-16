
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
import com.google.firebase.firestore.DocumentSnapshot;
import com.google.firebase.firestore.FieldValue;
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

    private static final int GREEN = Color.rgb(0, 120, 70);
    private static final int BLUE = Color.rgb(30, 90, 180);
    private static final int ORANGE = Color.rgb(180, 90, 0);

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);

        auth = FirebaseAuth.getInstance();
        db = FirebaseFirestore.getInstance();

        FirebaseUser user = auth.getCurrentUser();

        if (user != null) {
            loadUserProfile(user);
        } else {
            buildLoginScreen();
        }
    }

    private void buildLoginScreen() {

        LinearLayout root = new LinearLayout(this);
        root.setOrientation(LinearLayout.VERTICAL);
        root.setPadding(32, 28, 32, 28);
        root.setGravity(Gravity.CENTER_HORIZONTAL);
        root.setBackgroundColor(Color.WHITE);

        TextView logo = new TextView(this);
        logo.setText("🛺 SAKAY NA");
        logo.setTextSize(32);
        logo.setTypeface(null, android.graphics.Typeface.BOLD);
        logo.setTextColor(GREEN);
        logo.setGravity(Gravity.CENTER);
        root.addView(logo, lp());

        TextView subtitle = new TextView(this);
        subtitle.setText("Ride anywhere. Sakay Na.");
        subtitle.setTextSize(17);
        subtitle.setTextColor(Color.DKGRAY);
        subtitle.setGravity(Gravity.CENTER);
        subtitle.setPadding(0, 5, 0, 22);
        root.addView(subtitle, lp());

        TextView roleTitle = new TextView(this);
        roleTitle.setText("SELECT ACCOUNT TYPE");
        roleTitle.setTextSize(15);
        roleTitle.setTypeface(null, android.graphics.Typeface.BOLD);
        roleTitle.setTextColor(Color.DKGRAY);
        roleTitle.setGravity(Gravity.CENTER);
        roleTitle.setPadding(0, 8, 0, 10);
        root.addView(roleTitle, lp());

        LinearLayout roleRow = new LinearLayout(this);
        roleRow.setOrientation(LinearLayout.HORIZONTAL);
        roleRow.setGravity(Gravity.CENTER);

        passengerButton = roleButton("PASSENGER");
        driverButton = roleButton("DRIVER");
        adminButton = roleButton("ADMIN");

        roleRow.addView(passengerButton, weight());
        roleRow.addView(driverButton, weight());
        roleRow.addView(adminButton, weight());

        root.addView(roleRow, lp());

        passengerButton.setOnClickListener(v -> selectRole("PASSENGER"));
        driverButton.setOnClickListener(v -> selectRole("DRIVER"));
        adminButton.setOnClickListener(v -> selectRole("ADMIN"));

        TextView phoneLabel = label("Phone number");
        phoneLabel.setPadding(0, 18, 0, 4);
        root.addView(phoneLabel, lp());

        phoneField = new EditText(this);
        phoneField.setHint("09XXXXXXXXX");
        phoneField.setTextSize(18);
        phoneField.setSingleLine(true);
        phoneField.setInputType(InputType.TYPE_CLASS_PHONE);
        root.addView(phoneField, lp());

        TextView passwordLabel = label("Password");
        passwordLabel.setPadding(0, 14, 0, 4);
        root.addView(passwordLabel, lp());

        passwordField = new EditText(this);
        passwordField.setHint("Password");
        passwordField.setTextSize(18);
        passwordField.setSingleLine(true);
        passwordField.setInputType(
                InputType.TYPE_CLASS_TEXT |
                InputType.TYPE_TEXT_VARIATION_PASSWORD
        );
        root.addView(passwordField, lp());

        Button loginButton = new Button(this);
        loginButton.setText("LOGIN");
        loginButton.setTextSize(17);
        loginButton.setTextColor(Color.WHITE);
        loginButton.setBackgroundColor(GREEN);
        loginButton.setOnClickListener(v -> login());

        LinearLayout.LayoutParams loginParams = lp();
        loginParams.topMargin = 18;
        root.addView(loginButton, loginParams);

        Button createButton = new Button(this);
        createButton.setText("CREATE ACCOUNT");
        createButton.setTextSize(17);
        createButton.setOnClickListener(v -> register());
        root.addView(createButton, lp());

        TextView info = new TextView(this);
        info.setText(
                "Phone number + password only\n" +
                "Driver accounts require Admin approval before GO ONLINE."
        );
        info.setTextSize(14);
        info.setTextColor(Color.GRAY);
        info.setGravity(Gravity.CENTER);
        info.setPadding(0, 14, 0, 0);
        root.addView(info, lp());

        setContentView(root);

        selectRole("PASSENGER");
    }

    private Button roleButton(String text) {
        Button b = new Button(this);
        b.setText(text);
        b.setTextSize(13);
        return b;
    }

    private TextView label(String text) {
        TextView t = new TextView(this);
        t.setText(text);
        t.setTextSize(14);
        t.setTypeface(null, android.graphics.Typeface.BOLD);
        t.setTextColor(Color.DKGRAY);
        return t;
    }

    private LinearLayout.LayoutParams lp() {
        return new LinearLayout.LayoutParams(
                LinearLayout.LayoutParams.MATCH_PARENT,
                LinearLayout.LayoutParams.WRAP_CONTENT
        );
    }

    private LinearLayout.LayoutParams weight() {
        return new LinearLayout.LayoutParams(
                0,
                LinearLayout.LayoutParams.WRAP_CONTENT,
                1f
        );
    }

    private void selectRole(String role) {

        if (role == null) {
            role = "PASSENGER";
        }

        selectedRole = role.trim().toUpperCase();

        passengerButton.setText("PASSENGER");
        driverButton.setText("DRIVER");
        adminButton.setText("ADMIN");

        passengerButton.setTextColor(Color.DKGRAY);
        driverButton.setTextColor(Color.DKGRAY);
        adminButton.setTextColor(Color.DKGRAY);

        passengerButton.setBackgroundColor(Color.TRANSPARENT);
        driverButton.setBackgroundColor(Color.TRANSPARENT);
        adminButton.setBackgroundColor(Color.TRANSPARENT);

        if ("DRIVER".equals(selectedRole)) {

            driverButton.setTextColor(Color.WHITE);
            driverButton.setBackgroundColor(BLUE);

        } else if ("ADMIN".equals(selectedRole)) {

            adminButton.setTextColor(Color.WHITE);
            adminButton.setBackgroundColor(ORANGE);

        } else {

            selectedRole = "PASSENGER";
            passengerButton.setTextColor(Color.WHITE);
            passengerButton.setBackgroundColor(GREEN);
        }
    }

    private String normalizePhone(String input) {

        if (input == null) {
            return "";
        }

        String p = input.replaceAll("[^0-9]", "");

        if (p.startsWith("0") && p.length() == 11) {
            p = "63" + p.substring(1);
        } else if (p.startsWith("9") && p.length() == 10) {
            p = "63" + p;
        }

        return p;
    }

    private String firebaseIdentifier(String phone) {
        return phone + "@sakyna.app";
    }

    private boolean validateFields() {

        String phone = normalizePhone(
                phoneField == null ? "" : phoneField.getText().toString()
        );

        String password = passwordField == null
                ? ""
                : passwordField.getText().toString();

        if (phone.length() != 12 || !phone.startsWith("63")) {
            toast("Enter a valid Philippine phone number.");
            return false;
        }

        if (password.length() < 6) {
            toast("Password must be at least 6 characters.");
            return false;
        }

        return true;
    }

    private void login() {

        if (!validateFields()) {
            return;
        }

        String phone = normalizePhone(
                phoneField.getText().toString()
        );

        String password = passwordField.getText().toString();

        auth.signInWithEmailAndPassword(
                firebaseIdentifier(phone),
                password
        ).addOnSuccessListener(result -> {

            FirebaseUser user = auth.getCurrentUser();

            if (user == null) {
                toast("Login failed.");
                return;
            }

            loadUserProfile(user);

        }).addOnFailureListener(e -> {

            String message = e.getMessage();

            if (message == null || message.trim().isEmpty()) {
                message = "Invalid phone number or password.";
            }

            toast(message);
        });
    }

    private void loadUserProfile(FirebaseUser user) {

        if (user == null) {
            showLoginError("Unable to load account.");
            return;
        }

        db.collection("users")
                .document(user.getUid())
                .get()
                .addOnSuccessListener(document -> {

                    if (document.exists()) {
                        openUserProfile(document);
                    } else {
                        findUserByPhone(user);
                    }

                })
                .addOnFailureListener(e ->
                        showLoginError(
                                "Unable to load account: " +
                                safeMessage(e)
                        )
                );
    }

    private void findUserByPhone(FirebaseUser user) {

        String phone = "";

        String identifier = user.getEmail();

        if (identifier != null &&
                identifier.endsWith("@sakyna.app")) {

            phone = identifier.substring(
                    0,
                    identifier.length() - "@sakyna.app".length()
            );
        }

        if (phone.isEmpty() && phoneField != null) {
            phone = normalizePhone(
                    phoneField.getText().toString()
            );
        }

        if (phone.isEmpty()) {
            auth.signOut();
            buildLoginScreen();
            toast("User profile was not found.");
            return;
        }

        db.collection("users")
                .whereEqualTo("phone", phone)
                .limit(1)
                .get()
                .addOnSuccessListener(query -> {

                    if (query.isEmpty()) {

                        auth.signOut();
                        buildLoginScreen();
                        toast("User profile was not found.");

                    } else {

                        openUserProfile(
                                query.getDocuments().get(0)
                        );
                    }

                })
                .addOnFailureListener(e -> {

                    auth.signOut();
                    buildLoginScreen();

                    toast(
                            "Unable to load account: " +
                            safeMessage(e)
                    );
                });
    }

    private void openUserProfile(DocumentSnapshot document) {

        if (document == null || !document.exists()) {
            auth.signOut();
            buildLoginScreen();
            toast("Account profile not found.");
            return;
        }

        String role = document.getString("role");

        if (role == null || role.trim().isEmpty()) {
            auth.signOut();
            buildLoginScreen();
            toast("Account role is missing.");
            return;
        }

        role = role.trim().toUpperCase();

        /*
         * IMPORTANT:
         *
         * Login destination is determined ONLY by the
         * Firestore profile role.
         *
         * The login screen's selected button does NOT
         * change the account's role.
         */

        if ("DRIVER".equals(role)) {

            openActivity(DriverActivity.class);
            return;
        }

        if ("PASSENGER".equals(role)) {

            openActivity(PassengerActivity.class);
            return;
        }

        if ("ADMIN".equals(role)) {

            openActivity(AdminActivity.class);
            return;
        }

        auth.signOut();
        buildLoginScreen();
        toast("Unknown account role: " + role);
    }

    private void register() {

        if (!validateFields()) {
            return;
        }

        /*
         * Capture the role NOW.
         * This prevents the role from accidentally changing
         * while Firebase is creating the account.
         */
        final String accountRole =
                selectedRole == null
                        ? "PASSENGER"
                        : selectedRole.trim().toUpperCase();

        if (!"PASSENGER".equals(accountRole) &&
                !"DRIVER".equals(accountRole)) {

            toast("Choose PASSENGER or DRIVER to create an account.");
            return;
        }

        final String phone =
                normalizePhone(phoneField.getText().toString());

        final String password =
                passwordField.getText().toString();

        /*
         * Phone number + password only.
         *
         * The Firebase identifier is internal.
         * The user interface remains phone + password.
         */
        auth.createUserWithEmailAndPassword(
                firebaseIdentifier(phone),
                password
        ).addOnSuccessListener(result -> {

            FirebaseUser user = auth.getCurrentUser();

            if (user == null) {
                toast("Account creation failed.");
                return;
            }

            Map<String, Object> profile =
                    new HashMap<>();

            profile.put("phone", phone);
            profile.put("role", accountRole);

            if ("DRIVER".equals(accountRole)) {

                /*
                 * DRIVER:
                 * Can create account.
                 * Can login.
                 * Cannot GO ONLINE until Admin approval.
                 */
                profile.put("approved", false);
                profile.put(
                        "approvalStatus",
                        "PENDING_APPROVAL"
                );
                profile.put("online", false);

            } else {

                /*
                 * PASSENGER:
                 * Normal passenger account.
                 */
                profile.put("approved", true);
                profile.put(
                        "approvalStatus",
                        "APPROVED"
                );
                profile.put("online", false);
            }

            profile.put(
                    "createdAt",
                    FieldValue.serverTimestamp()
            );

            db.collection("users")
                    .document(user.getUid())
                    .set(profile)
                    .addOnSuccessListener(v -> {

                        auth.signOut();

                        passwordField.setText("");

                        if ("DRIVER".equals(accountRole)) {

                            toast(
                                    "DRIVER account created.\n" +
                                    "You can LOGIN now.\n" +
                                    "GO ONLINE will require Admin approval."
                            );

                        } else {

                            toast(
                                    "PASSENGER account created.\n" +
                                    "Please login."
                            );
                        }

                    })
                    .addOnFailureListener(e -> {

                        auth.signOut();

                        toast(
                                "Unable to save account: " +
                                safeMessage(e)
                        );
                    });

        }).addOnFailureListener(e -> {

            String message = e.getMessage();

            if (message == null ||
                    message.trim().isEmpty()) {

                message = "Unable to create account.";
            }

            toast(message);
        });
    }

    private void openActivity(Class<?> activityClass) {

        Intent intent =
                new Intent(this, activityClass);

        intent.addFlags(
                Intent.FLAG_ACTIVITY_NEW_TASK |
                Intent.FLAG_ACTIVITY_CLEAR_TASK
        );

        startActivity(intent);
        finish();
    }

    private String safeMessage(Exception e) {

        if (e == null ||
                e.getMessage() == null ||
                e.getMessage().trim().isEmpty()) {

            return "Unknown Firebase error.";
        }

        return e.getMessage();
    }

    private void showLoginError(String message) {

        auth.signOut();
        buildLoginScreen();
        toast(message);
    }

    private void toast(String message) {

        Toast.makeText(
                this,
                message,
                Toast.LENGTH_LONG
        ).show();
    }
}

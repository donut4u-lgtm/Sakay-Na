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
        logo.setTextSize(34);
        logo.setTypeface(null, android.graphics.Typeface.BOLD);
        logo.setTextColor(GREEN);
        logo.setGravity(Gravity.CENTER);

        root.addView(logo, lp());

        TextView sub = new TextView(this);
        sub.setText("Ride anywhere. Sakay Na.");
        sub.setTextSize(17);
        sub.setTextColor(Color.DKGRAY);
        sub.setGravity(Gravity.CENTER);
        sub.setPadding(0, 4, 0, 22);

        root.addView(sub, lp());

        TextView role = label("SELECT ACCOUNT TYPE");
        role.setGravity(Gravity.CENTER);
        role.setPadding(0, 8, 0, 10);

        root.addView(role, lp());

        LinearLayout row = new LinearLayout(this);
        row.setOrientation(LinearLayout.HORIZONTAL);
        row.setGravity(Gravity.CENTER);

        passengerButton = roleButton("PASSENGER");
        driverButton = roleButton("DRIVER");
        adminButton = roleButton("ADMIN");

        row.addView(passengerButton, weight());
        row.addView(driverButton, weight());
        row.addView(adminButton, weight());

        root.addView(row, lp());

        passengerButton.setOnClickListener(v -> selectRole("PASSENGER"));
        driverButton.setOnClickListener(v -> selectRole("DRIVER"));
        adminButton.setOnClickListener(v -> selectRole("ADMIN"));

        TextView phoneLabel = label("Phone number");
        phoneLabel.setPadding(0, 12, 0, 4);

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

        Button login = new Button(this);
        login.setText("LOGIN");
        login.setTextSize(17);
        login.setTextColor(Color.WHITE);
        login.setBackgroundColor(GREEN);
        login.setOnClickListener(v -> login());

        LinearLayout.LayoutParams loginParams = lp();
        loginParams.topMargin = 18;
        root.addView(login, loginParams);

        Button register = new Button(this);
        register.setText("CREATE ACCOUNT");
        register.setTextSize(17);
        register.setOnClickListener(v -> register());

        root.addView(register, lp());

        TextView info = new TextView(this);
        info.setText(
                "Phone number + password only\n\n" +
                "Driver accounts can register and login immediately.\n" +
                "Admin approval is required before a driver can GO ONLINE."
        );
        info.setTextSize(14);
        info.setTextColor(Color.GRAY);
        info.setGravity(Gravity.CENTER);
        info.setPadding(0, 12, 0, 0);

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

        selectedRole = role;

        passengerButton.setText("PASSENGER");
        driverButton.setText("DRIVER");
        adminButton.setText("ADMIN");

        passengerButton.setTextColor(Color.DKGRAY);
        driverButton.setTextColor(Color.DKGRAY);
        adminButton.setTextColor(Color.DKGRAY);

        passengerButton.setBackgroundColor(Color.TRANSPARENT);
        driverButton.setBackgroundColor(Color.TRANSPARENT);
        adminButton.setBackgroundColor(Color.TRANSPARENT);

        if ("PASSENGER".equals(role)) {

            passengerButton.setTextColor(GREEN);
            passengerButton.setBackgroundColor(
                    Color.rgb(225, 245, 235)
            );

        } else if ("DRIVER".equals(role)) {

            driverButton.setTextColor(BLUE);
            driverButton.setBackgroundColor(
                    Color.rgb(230, 240, 255)
            );

        } else {

            adminButton.setTextColor(ORANGE);
            adminButton.setBackgroundColor(
                    Color.rgb(255, 240, 220)
            );
        }
    }

    private String normalizePhone(String input) {

        if (input == null) return "";

        String p = input.replaceAll("[^0-9]", "");

        if (p.startsWith("0") && p.length() == 11) {
            p = "63" + p.substring(1);
        } else if (p.startsWith("9") && p.length() == 10) {
            p = "63" + p;
        }

        return p;
    }

    /*
     * Firebase uses this internal identifier so the user does not
     * need an email address. The user only enters phone + password.
     */
    private String firebaseIdentifier(String phone) {
        return phone + "@sakyna.app";
    }

    private boolean validateFields() {

        String phone = normalizePhone(
                phoneField == null
                        ? ""
                        : phoneField.getText().toString()
        );

        String password =
                passwordField == null
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

        if (!validateFields()) return;

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
                showError("Login failed. Please try again.");
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
            showError("Unable to load account profile.");
            return;
        }

        db.collection("users")
                .document(user.getUid())
                .get()
                .addOnSuccessListener(d -> {

                    if (d.exists()) {
                        openUserProfile(d);
                    } else {
                        findUserByPhoneFromUser(user);
                    }

                })
                .addOnFailureListener(e ->
                        showError(
                                "Unable to load account profile: "
                                        + safeMessage(e)
                        )
                );
    }

    private void findUserByPhoneFromUser(FirebaseUser user) {

        String phone = "";

        String internalEmail = user.getEmail();

        if (internalEmail != null &&
                internalEmail.endsWith("@sakyna.app")) {

            phone = internalEmail.substring(
                    0,
                    internalEmail.length() -
                            "@sakyna.app".length()
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
            showError("User profile was not found.");
            return;
        }

        final String finalPhone = phone;

        db.collection("users")
                .whereEqualTo("phone", finalPhone)
                .limit(1)
                .get()
                .addOnSuccessListener(q -> {

                    if (q.isEmpty()) {

                        auth.signOut();
                        buildLoginScreen();
                        showError("User profile was not found.");

                    } else {

                        openUserProfile(
                                q.getDocuments().get(0)
                        );
                    }

                })
                .addOnFailureListener(e -> {

                    auth.signOut();
                    buildLoginScreen();

                    showError(
                            "Unable to load account profile: "
                                    + safeMessage(e)
                    );
                });
    }

    private void openUserProfile(DocumentSnapshot d) {

        String role = d.getString("role");

        if (role == null || role.trim().isEmpty()) {

            auth.signOut();
            buildLoginScreen();
            showError("Unknown account role.");
            return;
        }

        role = role.trim().toUpperCase();

        if ("ADMIN".equals(role)) {

            openActivity(AdminActivity.class);
            return;
        }

        if ("DRIVER".equals(role)) {

            /*
             * IMPORTANT:
             * A driver DOES NOT need approval to LOGIN.
             *
             * Approval is checked by DriverActivity when
             * the driver attempts to go ONLINE.
             */

            openActivity(DriverActivity.class);
            return;
        }

        if ("PASSENGER".equals(role)) {

            openActivity(PassengerActivity.class);
            return;
        }

        auth.signOut();
        buildLoginScreen();
        showError("Unknown account role.");
    }

    private void register() {

        if (!validateFields()) return;

        if ("ADMIN".equals(selectedRole)) {

            toast("Admin accounts cannot be created here.");
            return;
        }

        String phone = normalizePhone(
                phoneField.getText().toString()
        );

        String password = passwordField.getText().toString();

        auth.createUserWithEmailAndPassword(
                firebaseIdentifier(phone),
                password
        ).addOnSuccessListener(result -> {

            FirebaseUser user = auth.getCurrentUser();

            if (user == null) {
                showError("Unable to create account.");
                return;
            }

            Map<String, Object> profile =
                    new HashMap<>();

            profile.put("phone", phone);
            profile.put("role", selectedRole);

            /*
             * PASSENGER:
             * immediately approved.
             *
             * DRIVER:
             * account is created immediately,
             * but starts NOT approved.
             */
            if ("DRIVER".equals(selectedRole)) {

                profile.put("approved", false);
                profile.put(
                        "approvalStatus",
                        "PENDING_APPROVAL"
                );
                profile.put("online", false);

            } else {

                profile.put("approved", true);
                profile.put(
                        "approvalStatus",
                        "APPROVED"
                );
            }

            profile.put(
                    "createdAt",
                    com.google.firebase.firestore.FieldValue
                            .serverTimestamp()
            );

            db.collection("users")
                    .document(user.getUid())
                    .set(profile)
                    .addOnSuccessListener(v -> {

                        auth.signOut();

                        passwordField.setText("");

                        if ("DRIVER".equals(selectedRole)) {

                            toast(
                                    "Driver account created! " +
                                    "You can login now. " +
                                    "Admin approval is required " +
                                    "before you can GO ONLINE."
                            );

                        } else {

                            toast(
                                    "Account created successfully. " +
                                    "Please login."
                            );
                        }

                    })
                    .addOnFailureListener(e -> {

                        auth.signOut();

                        showError(
                                "Unable to save account: "
                                        + safeMessage(e)
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

    private String safeMessage(Exception e) {

        if (e == null ||
                e.getMessage() == null ||
                e.getMessage().trim().isEmpty()) {

            return "Unknown Firebase error.";
        }

        return e.getMessage();
    }

    private void openActivity(Class<?> activityClass) {

        Intent intent =
                new Intent(this, activityClass);

        startActivity(intent);
        finish();
    }

    private void toast(String message) {

        Toast.makeText(
                this,
                message,
                Toast.LENGTH_LONG
        ).show();
    }

    private void showError(String message) {
        toast(message);
    }
}

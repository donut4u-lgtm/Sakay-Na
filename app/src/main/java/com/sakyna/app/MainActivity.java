
package com.sakyna.app;

import android.app.Activity;
import android.os.Bundle;
import android.graphics.Color;
import android.graphics.Typeface;
import android.content.Intent;
import android.view.Gravity;
import android.view.View;
import android.widget.Button;
import android.widget.EditText;
import android.widget.LinearLayout;
import android.widget.TextView;
import android.widget.Toast;

import com.google.firebase.auth.FirebaseAuth;
import com.google.firebase.auth.FirebaseUser;
import com.google.firebase.firestore.FirebaseFirestore;

public class MainActivity extends Activity {

    private FirebaseAuth auth;
    private FirebaseFirestore db;

    private EditText emailInput;
    private EditText passwordInput;

    private LinearLayout root;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);

        auth = FirebaseAuth.getInstance();
        db = FirebaseFirestore.getInstance();

        buildMainPage();

        FirebaseUser user = auth.getCurrentUser();

        if (user != null) {
            loadUserAndOpen(user);
        }
    }

    private void buildMainPage() {

        root = new LinearLayout(this);
        root.setOrientation(LinearLayout.VERTICAL);
        root.setGravity(Gravity.CENTER_HORIZONTAL);
        root.setPadding(40, 50, 40, 40);
        root.setBackgroundColor(Color.WHITE);

        TextView logo = new TextView(this);
        logo.setText("🛺");
        logo.setTextSize(58);
        logo.setGravity(Gravity.CENTER);

        root.addView(
                logo,
                new LinearLayout.LayoutParams(
                        -1,
                        100
                )
        );

        TextView title = new TextView(this);
        title.setText("SAKAY NA");
        title.setTextSize(34);
        title.setTypeface(Typeface.DEFAULT, Typeface.BOLD);
        title.setTextColor(Color.rgb(20, 20, 20));
        title.setGravity(Gravity.CENTER);

        root.addView(
                title,
                new LinearLayout.LayoutParams(
                        -1,
                        70
                )
        );

        TextView subtitle = new TextView(this);
        subtitle.setText("Your local tricycle ride, made simple.");
        subtitle.setTextSize(16);
        subtitle.setTextColor(Color.DKGRAY);
        subtitle.setGravity(Gravity.CENTER);

        root.addView(
                subtitle,
                new LinearLayout.LayoutParams(
                        -1,
                        60
                )
        );

        addSpace(20);

        emailInput = new EditText(this);
        emailInput.setHint("Email");
        emailInput.setSingleLine(true);
        emailInput.setInputType(
                android.text.InputType.TYPE_CLASS_TEXT
                        | android.text.InputType.TYPE_TEXT_VARIATION_EMAIL_ADDRESS
        );

        root.addView(
                emailInput,
                new LinearLayout.LayoutParams(
                        -1,
                        60
                )
        );

        addSpace(10);

        passwordInput = new EditText(this);
        passwordInput.setHint("Password");
        passwordInput.setSingleLine(true);
        passwordInput.setInputType(
                android.text.InputType.TYPE_CLASS_TEXT
                        | android.text.InputType.TYPE_TEXT_VARIATION_PASSWORD
        );

        root.addView(
                passwordInput,
                new LinearLayout.LayoutParams(
                        -1,
                        60
                )
        );

        addSpace(20);

        Button loginButton = new Button(this);
        loginButton.setText("LOGIN");
        loginButton.setTextSize(16);
        loginButton.setAllCaps(false);

        root.addView(
                loginButton,
                new LinearLayout.LayoutParams(
                        -1,
                        60
                )
        );

        loginButton.setOnClickListener(v -> loginUser());

        addSpace(10);

        Button registerButton = new Button(this);
        registerButton.setText("CREATE ACCOUNT");
        registerButton.setTextSize(16);
        registerButton.setAllCaps(false);

        root.addView(
                registerButton,
                new LinearLayout.LayoutParams(
                        -1,
                        60
                )
        );

        registerButton.setOnClickListener(v -> registerUser());

        addSpace(30);

        TextView footer = new TextView(this);
        footer.setText("Passenger • Driver • Admin");
        footer.setTextSize(14);
        footer.setTextColor(Color.GRAY);
        footer.setGravity(Gravity.CENTER);

        root.addView(
                footer,
                new LinearLayout.LayoutParams(
                        -1,
                        50
                )
        );

        setContentView(root);
    }

    private void addSpace(int height) {
        View space = new View(this);

        root.addView(
                space,
                new LinearLayout.LayoutParams(
                        1,
                        height
                )
        );
    }

    private void loginUser() {

        String email = emailInput.getText().toString().trim();
        String password = passwordInput.getText().toString();

        if (email.isEmpty()) {
            emailInput.setError("Enter your email");
            return;
        }

        if (password.isEmpty()) {
            passwordInput.setError("Enter your password");
            return;
        }

        Toast.makeText(
                this,
                "Signing in...",
                Toast.LENGTH_SHORT
        ).show();

        auth.signInWithEmailAndPassword(email, password)
                .addOnSuccessListener(result -> {

                    FirebaseUser user = auth.getCurrentUser();

                    if (user != null) {
                        loadUserAndOpen(user);
                    }

                })
                .addOnFailureListener(e ->
                        Toast.makeText(
                                this,
                                "Login failed: " + e.getMessage(),
                                Toast.LENGTH_LONG
                        ).show()
                );
    }

    private void registerUser() {

        String email = emailInput.getText().toString().trim();
        String password = passwordInput.getText().toString();

        if (email.isEmpty()) {
            emailInput.setError("Enter your email");
            return;
        }

        if (password.length() < 6) {
            passwordInput.setError("Password must be at least 6 characters");
            return;
        }

        Toast.makeText(
                this,
                "Creating account...",
                Toast.LENGTH_SHORT
        ).show();

        auth.createUserWithEmailAndPassword(email, password)
                .addOnSuccessListener(result -> {

                    FirebaseUser user = auth.getCurrentUser();

                    if (user == null) {
                        return;
                    }

                    saveNewPassenger(user);

                })
                .addOnFailureListener(e ->
                        Toast.makeText(
                                this,
                                "Registration failed: " + e.getMessage(),
                                Toast.LENGTH_LONG
                        ).show()
                );
    }

    private void saveNewPassenger(FirebaseUser user) {

        java.util.HashMap<String, Object> data =
                new java.util.HashMap<>();

        data.put("email", user.getEmail());
        data.put("role", "PASSENGER");
        data.put("approved", true);
        data.put("driverStatus", "NOT_DRIVER");
        data.put("canAcceptRides", false);
        data.put("createdAt",
                com.google.firebase.firestore.FieldValue.serverTimestamp());

        db.collection("users")
                .document(user.getUid())
                .set(data)
                .addOnSuccessListener(v -> {

                    Toast.makeText(
                            this,
                            "Account created!",
                            Toast.LENGTH_SHORT
                    ).show();

                    openPassenger();

                })
                .addOnFailureListener(e ->
                        Toast.makeText(
                                this,
                                "Profile error: " + e.getMessage(),
                                Toast.LENGTH_LONG
                        ).show()
                );
    }

    private void loadUserAndOpen(FirebaseUser user) {

        db.collection("users")
                .document(user.getUid())
                .get()
                .addOnSuccessListener(document -> {

                    if (!document.exists()) {

                        Toast.makeText(
                                this,
                                "User profile not found.",
                                Toast.LENGTH_LONG
                        ).show();

                        return;
                    }

                    String role = document.getString("role");

                    if (role == null) {
                        Toast.makeText(
                                this,
                                "User role is missing.",
                                Toast.LENGTH_LONG
                        ).show();

                        return;
                    }

                    if ("ADMIN".equals(role)) {

                        openAdmin();

                    } else if ("DRIVER".equals(role)) {

                        Boolean approved =
                                document.getBoolean("approved");

                        Boolean canAccept =
                                document.getBoolean("canAcceptRides");

                        String driverStatus =
                                document.getString("driverStatus");

                        if (Boolean.TRUE.equals(approved)
                                && Boolean.TRUE.equals(canAccept)
                                && "APPROVED".equals(driverStatus)) {

                            openDriver();

                        } else {

                            showDriverPending();

                        }

                    } else {

                        openPassenger();
                    }
                })
                .addOnFailureListener(e ->
                        Toast.makeText(
                                this,
                                "Unable to load profile: "
                                        + e.getMessage(),
                                Toast.LENGTH_LONG
                        ).show()
                );
    }

    private void showDriverPending() {

        root.removeAllViews();

        TextView icon = new TextView(this);
        icon.setText("🧑‍✈️");
        icon.setTextSize(60);
        icon.setGravity(Gravity.CENTER);

        root.addView(
                icon,
                new LinearLayout.LayoutParams(-1, 100)
        );

        TextView title = new TextView(this);
        title.setText("DRIVER APPROVAL");
        title.setTextSize(26);
        title.setTypeface(Typeface.DEFAULT, Typeface.BOLD);
        title.setGravity(Gravity.CENTER);

        root.addView(
                title,
                new LinearLayout.LayoutParams(-1, 70)
        );

        TextView message = new TextView(this);
        message.setText(
                "Your driver account is waiting for approval.\n\n"
                        + "You cannot accept rides until an administrator approves your driver account."
        );
        message.setTextSize(17);
        message.setGravity(Gravity.CENTER);
        message.setPadding(10, 20, 10, 20);

        root.addView(
                message,
                new LinearLayout.LayoutParams(-1, 180)
        );

        Button check = new Button(this);
        check.setText("CHECK APPROVAL AGAIN");
        check.setAllCaps(false);

        root.addView(
                check,
                new LinearLayout.LayoutParams(-1, 60)
        );

        check.setOnClickListener(v -> {

            FirebaseUser user = auth.getCurrentUser();

            if (user != null) {
                loadUserAndOpen(user);
            }
        });

        addSpace(15);

        Button logout = new Button(this);
        logout.setText("LOG OUT");
        logout.setAllCaps(false);

        root.addView(
                logout,
                new LinearLayout.LayoutParams(-1, 60)
        );

        logout.setOnClickListener(v -> logout());
    }

    private void openPassenger() {

        startActivity(
                new Intent(
                        MainActivity.this,
                        PassengerActivity.class
                )
        );

        finish();
    }

    private void openDriver() {

        startActivity(
                new Intent(
                        MainActivity.this,
                        DriverActivity.class
                )
        );

        finish();
    }

    private void openAdmin() {

        startActivity(
                new Intent(
                        MainActivity.this,
                        AdminActivity.class
                )
        );

        finish();
    }

    private void logout() {

        auth.signOut();

        buildMainPage();

        Toast.makeText(
                this,
                "Logged out",
                Toast.LENGTH_SHORT
        ).show();
    }
}

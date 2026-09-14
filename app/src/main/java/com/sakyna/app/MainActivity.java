
package com.sakyna.app;

import android.app.Activity;
import android.os.Bundle;
import android.graphics.Color;
import android.graphics.Typeface;
import android.graphics.drawable.GradientDrawable;
import android.text.InputType;
import android.view.Gravity;
import android.view.View;
import android.view.ViewGroup;
import android.widget.Button;
import android.widget.EditText;
import android.widget.LinearLayout;
import android.widget.ScrollView;
import android.widget.TextView;
import android.widget.Toast;

import com.google.firebase.auth.FirebaseAuth;
import com.google.firebase.firestore.FirebaseFirestore;

import java.util.HashMap;
import java.util.Map;

public class MainActivity extends Activity {

    private FirebaseAuth auth;
    private FirebaseFirestore db;

    private EditText emailInput;
    private EditText passwordInput;

    private int darkText = Color.rgb(35, 35, 35);
    private int grayText = Color.rgb(90, 90, 90);
    private int green = Color.rgb(0, 150, 80);

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);

        auth = FirebaseAuth.getInstance();
        db = FirebaseFirestore.getInstance();

        showLoginScreen();
    }

    private void showLoginScreen() {

        ScrollView scrollView = new ScrollView(this);
        scrollView.setFillViewport(true);
        scrollView.setBackgroundColor(Color.WHITE);

        LinearLayout root = new LinearLayout(this);
        root.setOrientation(LinearLayout.VERTICAL);
        root.setGravity(Gravity.CENTER_HORIZONTAL);
        root.setPadding(32, 40, 32, 40);
        root.setBackgroundColor(Color.WHITE);

        scrollView.addView(root);

        // ---------------------------------------------------------
        // LOGO
        // ---------------------------------------------------------

        TextView logo = new TextView(this);
        logo.setText("🛺");
        logo.setTextSize(54);
        logo.setGravity(Gravity.CENTER);

        root.addView(
                logo,
                new LinearLayout.LayoutParams(
                        ViewGroup.LayoutParams.MATCH_PARENT,
                        ViewGroup.LayoutParams.WRAP_CONTENT
                )
        );

        // ---------------------------------------------------------
        // TITLE
        // ---------------------------------------------------------

        TextView title = new TextView(this);
        title.setText("SAKAY NA");
        title.setTextColor(Color.rgb(20, 20, 20));
        title.setTextSize(34);
        title.setTypeface(Typeface.DEFAULT, Typeface.BOLD);
        title.setGravity(Gravity.CENTER);
        title.setIncludeFontPadding(true);

        LinearLayout.LayoutParams titleParams =
                new LinearLayout.LayoutParams(
                        ViewGroup.LayoutParams.MATCH_PARENT,
                        ViewGroup.LayoutParams.WRAP_CONTENT
                );

        titleParams.setMargins(0, 4, 0, 4);
        root.addView(title, titleParams);

        // ---------------------------------------------------------
        // SUBTITLE
        // ---------------------------------------------------------

        TextView subtitle = new TextView(this);
        subtitle.setText("Your local tricycle ride, made simple.");
        subtitle.setTextColor(grayText);
        subtitle.setTextSize(18);
        subtitle.setGravity(Gravity.CENTER);
        subtitle.setIncludeFontPadding(true);

        LinearLayout.LayoutParams subtitleParams =
                new LinearLayout.LayoutParams(
                        ViewGroup.LayoutParams.MATCH_PARENT,
                        ViewGroup.LayoutParams.WRAP_CONTENT
                );

        subtitleParams.setMargins(0, 0, 0, 28);
        root.addView(subtitle, subtitleParams);

        // ---------------------------------------------------------
        // EMAIL
        // ---------------------------------------------------------

        emailInput = createInput(
                "Email",
                InputType.TYPE_CLASS_TEXT |
                        InputType.TYPE_TEXT_VARIATION_EMAIL_ADDRESS
        );

        root.addView(
                emailInput,
                new LinearLayout.LayoutParams(
                        ViewGroup.LayoutParams.MATCH_PARENT,
                        60
                )
        );

        // ---------------------------------------------------------
        // PASSWORD
        // ---------------------------------------------------------

        passwordInput = createInput(
                "Password",
                InputType.TYPE_CLASS_TEXT |
                        InputType.TYPE_TEXT_VARIATION_PASSWORD
        );

        LinearLayout.LayoutParams passwordParams =
                new LinearLayout.LayoutParams(
                        ViewGroup.LayoutParams.MATCH_PARENT,
                        60
                );

        passwordParams.setMargins(0, 14, 0, 0);
        root.addView(passwordInput, passwordParams);

        // ---------------------------------------------------------
        // LOGIN BUTTON
        // ---------------------------------------------------------

        Button loginButton = createButton("LOGIN");

        LinearLayout.LayoutParams loginParams =
                new LinearLayout.LayoutParams(
                        ViewGroup.LayoutParams.MATCH_PARENT,
                        58
                );

        loginParams.setMargins(0, 26, 0, 0);
        root.addView(loginButton, loginParams);

        loginButton.setOnClickListener(v -> loginUser());

        // ---------------------------------------------------------
        // CREATE ACCOUNT BUTTON
        // ---------------------------------------------------------

        Button createButton = createOutlineButton("CREATE ACCOUNT");

        LinearLayout.LayoutParams createParams =
                new LinearLayout.LayoutParams(
                        ViewGroup.LayoutParams.MATCH_PARENT,
                        58
                );

        createParams.setMargins(0, 14, 0, 0);
        root.addView(createButton, createParams);

        createButton.setOnClickListener(v -> createAccount());

        // ---------------------------------------------------------
        // ROLE INFORMATION
        // ---------------------------------------------------------

        TextView roles = new TextView(this);
        roles.setText("Passenger  •  Driver  •  Admin");
        roles.setTextColor(Color.rgb(110, 110, 110));
        roles.setTextSize(17);
        roles.setGravity(Gravity.CENTER);
        roles.setTypeface(Typeface.DEFAULT, Typeface.NORMAL);

        LinearLayout.LayoutParams rolesParams =
                new LinearLayout.LayoutParams(
                        ViewGroup.LayoutParams.MATCH_PARENT,
                        ViewGroup.LayoutParams.WRAP_CONTENT
                );

        rolesParams.setMargins(0, 30, 0, 0);
        root.addView(roles, rolesParams);

        // ---------------------------------------------------------
        // FOOTER
        // ---------------------------------------------------------

        TextView footer = new TextView(this);
        footer.setText("Safe rides. Simple booking. Local service.");
        footer.setTextColor(Color.rgb(140, 140, 140));
        footer.setTextSize(13);
        footer.setGravity(Gravity.CENTER);

        LinearLayout.LayoutParams footerParams =
                new LinearLayout.LayoutParams(
                        ViewGroup.LayoutParams.MATCH_PARENT,
                        ViewGroup.LayoutParams.WRAP_CONTENT
                );

        footerParams.setMargins(0, 18, 0, 0);
        root.addView(footer, footerParams);

        setContentView(scrollView);
    }

    // =============================================================
    // INPUT STYLE
    // =============================================================

    private EditText createInput(String hint, int inputType) {

        EditText input = new EditText(this);

        input.setHint(hint);
        input.setHintTextColor(Color.rgb(120, 120, 120));
        input.setTextColor(darkText);
        input.setTextSize(17);
        input.setSingleLine(true);
        input.setInputType(inputType);
        input.setPadding(18, 0, 18, 0);

        GradientDrawable background = new GradientDrawable();
        background.setColor(Color.rgb(248, 248, 248));
        background.setStroke(2, Color.rgb(190, 190, 190));
        background.setCornerRadius(12);

        input.setBackground(background);

        return input;
    }

    // =============================================================
    // GREEN BUTTON
    // =============================================================

    private Button createButton(String text) {

        Button button = new Button(this);

        button.setText(text);
        button.setTextColor(Color.WHITE);
        button.setTextSize(17);
        button.setTypeface(Typeface.DEFAULT, Typeface.BOLD);
        button.setGravity(Gravity.CENTER);
        button.setAllCaps(false);
        button.setPadding(0, 0, 0, 0);

        GradientDrawable background = new GradientDrawable();
        background.setColor(green);
        background.setCornerRadius(14);

        button.setBackground(background);

        return button;
    }

    // =============================================================
    // OUTLINE BUTTON
    // =============================================================

    private Button createOutlineButton(String text) {

        Button button = new Button(this);

        button.setText(text);
        button.setTextColor(green);
        button.setTextSize(17);
        button.setTypeface(Typeface.DEFAULT, Typeface.BOLD);
        button.setGravity(Gravity.CENTER);
        button.setAllCaps(false);
        button.setPadding(0, 0, 0, 0);

        GradientDrawable background = new GradientDrawable();
        background.setColor(Color.WHITE);
        background.setStroke(3, green);
        background.setCornerRadius(14);

        button.setBackground(background);

        return button;
    }

    // =============================================================
    // LOGIN
    // =============================================================

    private void loginUser() {

        String email = emailInput.getText().toString().trim();
        String password = passwordInput.getText().toString();

        if (email.isEmpty()) {
            emailInput.setError("Enter your email");
            emailInput.requestFocus();
            return;
        }

        if (password.isEmpty()) {
            passwordInput.setError("Enter your password");
            passwordInput.requestFocus();
            return;
        }

        Toast.makeText(
                this,
                "Logging in...",
                Toast.LENGTH_SHORT
        ).show();

        auth.signInWithEmailAndPassword(email, password)
                .addOnSuccessListener(result -> {

                    if (auth.getCurrentUser() == null) {
                        Toast.makeText(
                                this,
                                "Login error.",
                                Toast.LENGTH_LONG
                        ).show();
                        return;
                    }

                    String uid = auth.getCurrentUser().getUid();

                    db.collection("users")
                            .document(uid)
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

                                String role =
                                        document.getString("role");

                                if (role == null) {
                                    role = "PASSENGER";
                                }

                                // -------------------------------------------------
                                // ADMIN
                                // -------------------------------------------------

                                if (role.equals("ADMIN")) {

                                    startActivity(
                                            new android.content.Intent(
                                                    this,
                                                    AdminActivity.class
                                            )
                                    );

                                    finish();
                                    return;
                                }

                                // -------------------------------------------------
                                // DRIVER
                                // -------------------------------------------------

                                if (role.equals("DRIVER")) {

                                    Boolean approved =
                                            document.getBoolean("approved");

                                    Boolean canAcceptRides =
                                            document.getBoolean(
                                                    "canAcceptRides"
                                            );

                                    String driverStatus =
                                            document.getString(
                                                    "driverStatus"
                                            );

                                    boolean driverApproved =
                                            Boolean.TRUE.equals(approved)
                                                    &&
                                            Boolean.TRUE.equals(
                                                    canAcceptRides
                                            )
                                                    &&
                                            "APPROVED".equals(
                                                    driverStatus
                                            );

                                    if (driverApproved) {

                                        startActivity(
                                                new android.content.Intent(
                                                        this,
                                                        DriverActivity.class
                                                )
                                        );

                                        finish();

                                    } else {

                                        showDriverPendingScreen();
                                    }

                                    return;
                                }

                                // -------------------------------------------------
                                // PASSENGER
                                // -------------------------------------------------

                                startActivity(
                                        new android.content.Intent(
                                                this,
                                                PassengerActivity.class
                                        )
                                );

                                finish();

                            })
                            .addOnFailureListener(error ->
                                    Toast.makeText(
                                            this,
                                            "Profile error: "
                                                    + error.getMessage(),
                                            Toast.LENGTH_LONG
                                    ).show()
                            );
                })
                .addOnFailureListener(error ->
                        Toast.makeText(
                                this,
                                "Login failed: "
                                        + error.getMessage(),
                                Toast.LENGTH_LONG
                        ).show()
                );
    }

    // =============================================================
    // CREATE ACCOUNT
    // =============================================================

    private void createAccount() {

        String email = emailInput.getText().toString().trim();
        String password = passwordInput.getText().toString();

        if (email.isEmpty()) {
            emailInput.setError("Enter your email");
            emailInput.requestFocus();
            return;
        }

        if (password.length() < 6) {
            passwordInput.setError(
                    "Password must be at least 6 characters"
            );
            passwordInput.requestFocus();
            return;
        }

        Toast.makeText(
                this,
                "Creating account...",
                Toast.LENGTH_SHORT
        ).show();

        auth.createUserWithEmailAndPassword(email, password)
                .addOnSuccessListener(result -> {

                    if (auth.getCurrentUser() == null) {
                        Toast.makeText(
                                this,
                                "Account creation error.",
                                Toast.LENGTH_LONG
                        ).show();
                        return;
                    }

                    String uid =
                            auth.getCurrentUser().getUid();

                    Map<String, Object> user =
                            new HashMap<>();

                    user.put("role", "PASSENGER");
                    user.put("approved", true);
                    user.put("driverStatus", "NOT_DRIVER");
                    user.put("canAcceptRides", false);
                    user.put("email", email);
                    user.put(
                            "createdAt",
                            com.google.firebase.firestore.FieldValue
                                    .serverTimestamp()
                    );

                    db.collection("users")
                            .document(uid)
                            .set(user)
                            .addOnSuccessListener(unused -> {

                                Toast.makeText(
                                        this,
                                        "Account created!",
                                        Toast.LENGTH_SHORT
                                ).show();

                                startActivity(
                                        new android.content.Intent(
                                                this,
                                                PassengerActivity.class
                                        )
                                );

                                finish();
                            })
                            .addOnFailureListener(error ->
                                    Toast.makeText(
                                            this,
                                            "Profile creation failed: "
                                                    + error.getMessage(),
                                            Toast.LENGTH_LONG
                                    ).show()
                            );
                })
                .addOnFailureListener(error ->
                        Toast.makeText(
                                this,
                                "Account creation failed: "
                                        + error.getMessage(),
                                Toast.LENGTH_LONG
                        ).show()
                );
    }

    // =============================================================
    // DRIVER APPROVAL SCREEN
    // =============================================================

    private void showDriverPendingScreen() {

        LinearLayout root = new LinearLayout(this);

        root.setOrientation(LinearLayout.VERTICAL);
        root.setGravity(Gravity.CENTER);
        root.setPadding(32, 40, 32, 40);
        root.setBackgroundColor(Color.WHITE);

        TextView icon = new TextView(this);
        icon.setText("🛺");
        icon.setTextSize(54);
        icon.setGravity(Gravity.CENTER);

        root.addView(
                icon,
                new LinearLayout.LayoutParams(
                        ViewGroup.LayoutParams.MATCH_PARENT,
                        ViewGroup.LayoutParams.WRAP_CONTENT
                )
        );

        TextView title = new TextView(this);
        title.setText("Driver Approval");
        title.setTextColor(darkText);
        title.setTextSize(28);
        title.setTypeface(Typeface.DEFAULT, Typeface.BOLD);
        title.setGravity(Gravity.CENTER);

        LinearLayout.LayoutParams titleParams =
                new LinearLayout.LayoutParams(
                        ViewGroup.LayoutParams.MATCH_PARENT,
                        ViewGroup.LayoutParams.WRAP_CONTENT
                );

        titleParams.setMargins(0, 15, 0, 10);
        root.addView(title, titleParams);

        TextView message = new TextView(this);
        message.setText(
                "Your driver account is waiting for admin approval."
                        + "\n\n"
                        + "You cannot accept rides until your account "
                        + "has been approved."
        );

        message.setTextColor(grayText);
        message.setTextSize(17);
        message.setGravity(Gravity.CENTER);

        LinearLayout.LayoutParams messageParams =
                new LinearLayout.LayoutParams(
                        ViewGroup.LayoutParams.MATCH_PARENT,
                        ViewGroup.LayoutParams.WRAP_CONTENT
                );

        messageParams.setMargins(0, 0, 0, 25);
        root.addView(message, messageParams);

        Button checkButton =
                createButton("CHECK APPROVAL AGAIN");

        root.addView(
                checkButton,
                new LinearLayout.LayoutParams(
                        ViewGroup.LayoutParams.MATCH_PARENT,
                        58
                )
        );

        checkButton.setOnClickListener(v -> {

            FirebaseAuth currentAuth =
                    FirebaseAuth.getInstance();

            if (currentAuth.getCurrentUser() == null) {
                showLoginScreen();
                return;
            }

            String uid =
                    currentAuth.getCurrentUser().getUid();

            db.collection("users")
                    .document(uid)
                    .get()
                    .addOnSuccessListener(document -> {

                        Boolean approved =
                                document.getBoolean("approved");

                        Boolean canAcceptRides =
                                document.getBoolean(
                                        "canAcceptRides"
                                );

                        String driverStatus =
                                document.getString(
                                        "driverStatus"
                                );

                        boolean approvedDriver =
                                Boolean.TRUE.equals(approved)
                                        &&
                                Boolean.TRUE.equals(
                                        canAcceptRides
                                )
                                        &&
                                "APPROVED".equals(
                                        driverStatus
                                );

                        if (approvedDriver) {

                            startActivity(
                                    new android.content.Intent(
                                            this,
                                            DriverActivity.class
                                    )
                            );

                            finish();

                        } else {

                            Toast.makeText(
                                    this,
                                    "Still waiting for admin approval.",
                                    Toast.LENGTH_LONG
                            ).show();
                        }
                    });
        });

        Button logoutButton =
                createOutlineButton("LOG OUT");

        LinearLayout.LayoutParams logoutParams =
                new LinearLayout.LayoutParams(
                        ViewGroup.LayoutParams.MATCH_PARENT,
                        58
                );

        logoutParams.setMargins(0, 14, 0, 0);
        root.addView(logoutButton, logoutParams);

        logoutButton.setOnClickListener(v -> {

            FirebaseAuth.getInstance().signOut();

            showLoginScreen();
        });

        setContentView(root);
    }
}

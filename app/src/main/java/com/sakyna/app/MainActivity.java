
package com.sakyna.app;

import android.content.Intent;
import android.graphics.Color;
import android.graphics.drawable.GradientDrawable;
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
import androidx.appcompat.app.AppCompatActivity;

import com.google.firebase.FirebaseException;
import com.google.firebase.auth.FirebaseAuth;
import com.google.firebase.auth.PhoneAuthCredential;
import com.google.firebase.auth.PhoneAuthOptions;
import com.google.firebase.auth.PhoneAuthProvider;
import com.google.firebase.firestore.FirebaseFirestore;

import java.util.HashMap;
import java.util.Map;
import java.util.concurrent.TimeUnit;

public class MainActivity extends AppCompatActivity {

    private FirebaseAuth auth;
    private FirebaseFirestore db;

    private String selectedRole = "";
    private String pendingName = "";
    private String pendingPhone = "";
    private String verificationId = "";

    private PhoneAuthProvider.ForceResendingToken resendToken;

    private LinearLayout root;

    private final int GREEN = Color.rgb(25, 135, 84);
    private final int DARK_GREEN = Color.rgb(16, 100, 62);
    private final int ORANGE = Color.rgb(245, 145, 30);
    private final int BLUE = Color.rgb(35, 105, 190);
    private final int PURPLE = Color.rgb(125, 70, 170);

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);

        auth = FirebaseAuth.getInstance();
        db = FirebaseFirestore.getInstance();

        showHome();
    }

    private void setupScreen(String title) {
        root = new LinearLayout(this);
        root.setOrientation(LinearLayout.VERTICAL);
        root.setGravity(Gravity.CENTER_HORIZONTAL);
        root.setPadding(32, 40, 32, 32);
        root.setBackgroundColor(Color.rgb(248, 250, 249));

        TextView titleView = new TextView(this);
        titleView.setText(title);
        titleView.setTextSize(28);
        titleView.setTextColor(DARK_GREEN);
        titleView.setTypeface(null, android.graphics.Typeface.BOLD);
        titleView.setGravity(Gravity.CENTER);
        titleView.setPadding(0, 0, 0, 25);

        root.addView(titleView);

        setContentView(root);
    }

    private TextView text(String value, float size) {
        TextView view = new TextView(this);
        view.setText(value);
        view.setTextSize(size);
        view.setTextColor(Color.DKGRAY);
        view.setGravity(Gravity.CENTER);
        return view;
    }

    private Button button(String text, int color) {
        Button button = new Button(this);
        button.setText(text);
        button.setTextSize(18);
        button.setTextColor(Color.WHITE);
        button.setAllCaps(false);
        button.setGravity(Gravity.CENTER);

        GradientDrawable background = new GradientDrawable();
        background.setColor(color);
        background.setCornerRadius(30);
        button.setBackground(background);

        LinearLayout.LayoutParams params =
                new LinearLayout.LayoutParams(
                        LinearLayout.LayoutParams.MATCH_PARENT,
                        60
                );

        params.setMargins(0, 10, 0, 10);
        root.addView(button, params);

        return button;
    }

    private EditText input(String hint) {
        EditText editText = new EditText(this);
        editText.setHint(hint);
        editText.setTextSize(17);
        editText.setSingleLine(true);
        editText.setPadding(25, 5, 25, 5);

        GradientDrawable background = new GradientDrawable();
        background.setColor(Color.WHITE);
        background.setCornerRadius(25);
        background.setStroke(2, Color.LTGRAY);
        editText.setBackground(background);

        LinearLayout.LayoutParams params =
                new LinearLayout.LayoutParams(
                        LinearLayout.LayoutParams.MATCH_PARENT,
                        60
                );

        params.setMargins(0, 6, 0, 6);
        root.addView(editText, params);

        return editText;
    }

    private void showHome() {
        setupScreen("");

        TextView logo = text("🛺", 64);
        root.addView(logo);

        TextView title = text("SAKAY NA", 34);
        title.setTextColor(GREEN);
        title.setTypeface(null, android.graphics.Typeface.BOLD);
        title.setPadding(0, 5, 0, 5);
        root.addView(title);

        TextView subtitle = text("Tricycle Ride-Hailing", 18);
        subtitle.setTextColor(Color.DKGRAY);
        subtitle.setPadding(0, 0, 0, 30);
        root.addView(subtitle);

        Button login = button("LOGIN", GREEN);
        login.setOnClickListener(v -> showLogin());

        Button register = button("REGISTER", ORANGE);
        register.setOnClickListener(v -> showRoleSelection());

        Button about = button("ABOUT", BLUE);
        about.setOnClickListener(v ->
                Toast.makeText(
                        this,
                        "Sakay Na\nYour local tricycle ride-hailing app.",
                        Toast.LENGTH_LONG
                ).show()
        );

        TextView footer = text("Safe • Simple • Local", 14);
        footer.setTextColor(Color.GRAY);
        footer.setPadding(0, 25, 0, 0);
        root.addView(footer);
    }

    private void showRoleSelection() {
        setupScreen("Choose Account Type");

        TextView info = text("How will you use Sakay Na?", 17);
        info.setPadding(0, 0, 0, 20);
        root.addView(info);

        Button passenger = button("🟠  Passenger", ORANGE);
        passenger.setOnClickListener(v -> {
            selectedRole = "Passenger";
            showRegistration();
        });

        Button driver = button("🔵  Driver", BLUE);
        driver.setOnClickListener(v -> {
            selectedRole = "Driver";
            showRegistration();
        });

        Button admin = button("🟣  Admin", PURPLE);
        admin.setOnClickListener(v -> {
            selectedRole = "Admin";
            showRegistration();
        });

        Button back = button("BACK", Color.GRAY);
        back.setOnClickListener(v -> showHome());
    }

    private void showRegistration() {
        setupScreen("Register — " + selectedRole);

        EditText nameInput = input("Full name");

        EditText phoneInput = input("Phone number");
        phoneInput.setInputType(InputType.TYPE_CLASS_PHONE);

        TextView format = text("Example: +639171234567", 14);
        format.setTextColor(Color.GRAY);
        root.addView(format);

        Button continueButton = button("SEND OTP", GREEN);

        continueButton.setOnClickListener(v -> {
            String name = nameInput.getText().toString().trim();
            String phone = phoneInput.getText().toString().trim();

            if (name.length() < 2) {
                toast("Enter your full name.");
                return;
            }

            if (!phone.startsWith("+63") || phone.length() < 12) {
                toast("Use Philippine format: +639XXXXXXXXX");
                return;
            }

            pendingName = name;
            pendingPhone = phone;

            sendOtp(phone, true);
        });

        Button back = button("BACK", Color.GRAY);
        back.setOnClickListener(v -> showRoleSelection());
    }

    private void showLogin() {
        setupScreen("LOGIN");

        TextView info = text(
                "Enter your registered phone number.\nWe'll send a one-time verification code.",
                17
        );
        info.setPadding(0, 0, 0, 20);
        root.addView(info);

        EditText phoneInput = input("Phone number");
        phoneInput.setInputType(InputType.TYPE_CLASS_PHONE);

        Button login = button("SEND OTP", GREEN);

        login.setOnClickListener(v -> {
            String phone = phoneInput.getText().toString().trim();

            if (!phone.startsWith("+63") || phone.length() < 12) {
                toast("Use Philippine format: +639XXXXXXXXX");
                return;
            }

            pendingPhone = phone;
            sendOtp(phone, false);
        });

        Button back = button("BACK", Color.GRAY);
        back.setOnClickListener(v -> showHome());
    }

    private void sendOtp(String phone, boolean registration) {

        toast("Sending OTP...");

        PhoneAuthOptions options =
                PhoneAuthOptions.newBuilder(auth)
                        .setPhoneNumber(phone)
                        .setTimeout(60L, TimeUnit.SECONDS)
                        .setActivity(this)
                        .setCallbacks(
                                new PhoneAuthProvider.OnVerificationStateChangedCallbacks() {

                                    @Override
                                    public void onVerificationCompleted(
                                            @NonNull PhoneAuthCredential credential) {

                                        verifyCredential(credential, registration);
                                    }

                                    @Override
                                    public void onVerificationFailed(
                                            @NonNull FirebaseException e) {

                                        toast("OTP failed: " + e.getMessage());
                                    }

                                    @Override
                                    public void onCodeSent(
                                            @NonNull String id,
                                            @NonNull PhoneAuthProvider.ForceResendingToken token) {

                                        verificationId = id;
                                        resendToken = token;

                                        showOtpScreen(registration);
                                    }
                                }
                        )
                        .build();

        PhoneAuthProvider.verifyPhoneNumber(options);
    }

    private void showOtpScreen(boolean registration) {
        setupScreen("VERIFY PHONE");

        TextView info = text(
                "OTP sent to:\n" + pendingPhone,
                17
        );
        info.setPadding(0, 0, 0, 20);
        root.addView(info);

        EditText otpInput = input("6-digit OTP");
        otpInput.setInputType(InputType.TYPE_CLASS_NUMBER);
        otpInput.setGravity(Gravity.CENTER);

        Button verify = button("VERIFY OTP", GREEN);

        verify.setOnClickListener(v -> {

            String code = otpInput.getText().toString().trim();

            if (code.length() != 6) {
                toast("Enter the 6-digit OTP.");
                return;
            }

            PhoneAuthCredential credential =
                    PhoneAuthProvider.getCredential(
                            verificationId,
                            code
                    );

            verifyCredential(credential, registration);
        });

        Button resend = button("RESEND OTP", BLUE);

        resend.setOnClickListener(v -> {
            if (resendToken != null) {
                resendOtp(registration);
            } else {
                sendOtp(pendingPhone, registration);
            }
        });

        Button back = button("BACK", Color.GRAY);
        back.setOnClickListener(v -> showHome());
    }

    private void resendOtp(boolean registration) {

        PhoneAuthOptions options =
                PhoneAuthOptions.newBuilder(auth)
                        .setPhoneNumber(pendingPhone)
                        .setTimeout(60L, TimeUnit.SECONDS)
                        .setActivity(this)
                        .setForceResendingToken(resendToken)
                        .setCallbacks(
                                new PhoneAuthProvider.OnVerificationStateChangedCallbacks() {

                                    @Override
                                    public void onVerificationCompleted(
                                            @NonNull PhoneAuthCredential credential) {

                                        verifyCredential(credential, registration);
                                    }

                                    @Override
                                    public void onVerificationFailed(
                                            @NonNull FirebaseException e) {

                                        toast("Resend failed: " + e.getMessage());
                                    }

                                    @Override
                                    public void onCodeSent(
                                            @NonNull String id,
                                            @NonNull PhoneAuthProvider.ForceResendingToken token) {

                                        verificationId = id;
                                        resendToken = token;

                                        toast("New OTP sent.");
                                    }
                                }
                        )
                        .build();

        PhoneAuthProvider.verifyPhoneNumber(options);
    }

    private void verifyCredential(
            PhoneAuthCredential credential,
            boolean registration) {

        auth.signInWithCredential(credential)
                .addOnCompleteListener(this, task -> {

                    if (!task.isSuccessful()) {
                        toast("Verification failed.");
                        return;
                    }

                    if (registration) {
                        createUserProfile();
                    } else {
                        loadExistingProfile();
                    }
                });
    }

    private void createUserProfile() {

        if (auth.getCurrentUser() == null) {
            toast("Authentication error.");
            return;
        }

        String uid = auth.getCurrentUser().getUid();

        Map<String, Object> profile = new HashMap<>();
        profile.put("name", pendingName);
        profile.put("phone", pendingPhone);
        profile.put("role", selectedRole);
        profile.put("suspended", false);

        db.collection("users")
                .document(uid)
                .set(profile)
                .addOnSuccessListener(unused -> {

                    getSharedPreferences("SakayNa", MODE_PRIVATE)
                            .edit()
                            .putString("current_phone", pendingPhone)
                            .putString("current_name", pendingName)
                            .putString("current_role", selectedRole)
                            .apply();

                    toast("Account created successfully!");

                    openRoleScreen(selectedRole);
                })
                .addOnFailureListener(e ->
                        toast("Could not save profile: " + e.getMessage()));
    }

    private void loadExistingProfile() {

        if (auth.getCurrentUser() == null) {
            toast("Authentication error.");
            return;
        }

        String uid = auth.getCurrentUser().getUid();

        db.collection("users")
                .document(uid)
                .get()
                .addOnSuccessListener(document -> {

                    if (!document.exists()) {
                        toast("No Sakay Na profile found. Please register first.");
                        auth.signOut();
                        showHome();
                        return;
                    }

                    Boolean suspended = document.getBoolean("suspended");

                    if (Boolean.TRUE.equals(suspended)) {
                        toast("This account is suspended.");
                        auth.signOut();
                        showHome();
                        return;
                    }

                    String name = document.getString("name");
                    String phone = document.getString("phone");
                    String role = document.getString("role");

                    if (role == null) {
                        toast("Account role is missing.");
                        auth.signOut();
                        showHome();
                        return;
                    }

                    getSharedPreferences("SakayNa", MODE_PRIVATE)
                            .edit()
                            .putString("current_phone", phone)
                            .putString("current_name", name)
                            .putString("current_role", role)
                            .apply();

                    toast("Login successful!");

                    openRoleScreen(role);
                })
                .addOnFailureListener(e ->
                        toast("Could not load profile: " + e.getMessage()));
    }

    private void openRoleScreen(String role) {

        if ("Passenger".equals(role)) {

            startActivity(
                    new Intent(
                            MainActivity.this,
                            PassengerActivity.class
                    )
            );

        } else if ("Driver".equals(role)) {

            startActivity(
                    new Intent(
                            MainActivity.this,
                            DriverActivity.class
                    )
            );

        } else if ("Admin".equals(role)) {

            startActivity(
                    new Intent(
                            MainActivity.this,
                            AdminActivity.class
                    )
            );

        } else {
            toast("Unknown account role.");
            showHome();
        }
    }

    private void toast(String message) {
        Toast.makeText(
                this,
                message,
                Toast.LENGTH_LONG
        ).show();
    }

    @Override
    public void onBackPressed() {
        showHome();
    }
}

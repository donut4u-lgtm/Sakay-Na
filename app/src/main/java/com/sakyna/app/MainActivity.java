
package com.sakyna.app;

import android.content.Intent;
import android.os.Bundle;
import android.text.InputType;
import android.view.Gravity;
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
        root.setPadding(32, 40, 32, 32);
        root.setGravity(Gravity.CENTER_HORIZONTAL);

        TextView titleView = new TextView(this);
        titleView.setText(title);
        titleView.setTextSize(28);
        titleView.setGravity(Gravity.CENTER);
        titleView.setPadding(0, 0, 0, 30);

        root.addView(titleView);

        setContentView(root);
    }

    private Button button(String text) {
        Button button = new Button(this);
        button.setText(text);
        button.setTextSize(17);
        button.setAllCaps(false);

        LinearLayout.LayoutParams params =
                new LinearLayout.LayoutParams(
                        LinearLayout.LayoutParams.MATCH_PARENT,
                        LinearLayout.LayoutParams.WRAP_CONTENT
                );

        params.setMargins(0, 8, 0, 8);
        root.addView(button, params);

        return button;
    }

    private EditText input(String hint) {
        EditText editText = new EditText(this);
        editText.setHint(hint);
        editText.setTextSize(17);
        editText.setSingleLine(true);

        LinearLayout.LayoutParams params =
                new LinearLayout.LayoutParams(
                        LinearLayout.LayoutParams.MATCH_PARENT,
                        LinearLayout.LayoutParams.WRAP_CONTENT
                );

        params.setMargins(0, 5, 0, 5);
        root.addView(editText, params);

        return editText;
    }

    private void showHome() {
        setupScreen("🛺 SAKAY NA");

        TextView welcome = new TextView(this);
        welcome.setText("Tricycle Ride-Hailing");
        welcome.setTextSize(20);
        welcome.setGravity(Gravity.CENTER);
        root.addView(welcome);

        Button login = button("LOGIN");
        login.setOnClickListener(v -> showLogin());

        Button register = button("REGISTER");
        register.setOnClickListener(v -> showRoleSelection());

        Button about = button("ABOUT");
        about.setOnClickListener(v ->
                Toast.makeText(
                        this,
                        "Sakay Na\nYour local tricycle ride-hailing app.",
                        Toast.LENGTH_LONG
                ).show()
        );
    }

    private void showRoleSelection() {
        setupScreen("Choose Account Type");

        Button passenger = button("🟠 Passenger");
        passenger.setOnClickListener(v -> {
            selectedRole = "Passenger";
            showRegistration();
        });

        Button driver = button("🔵 Driver");
        driver.setOnClickListener(v -> {
            selectedRole = "Driver";
            showRegistration();
        });

        Button admin = button("🟣 Admin");
        admin.setOnClickListener(v -> {
            selectedRole = "Admin";
            showRegistration();
        });

        Button back = button("BACK");
        back.setOnClickListener(v -> showHome());
    }

    private void showRegistration() {
        setupScreen("Register — " + selectedRole);

        EditText nameInput = input("Full name");

        EditText phoneInput = input("Phone number");
        phoneInput.setInputType(InputType.TYPE_CLASS_PHONE);

        TextView format = new TextView(this);
        format.setText("Example: +639171234567");
        root.addView(format);

        Button continueButton = button("SEND OTP");

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

        Button back = button("BACK");
        back.setOnClickListener(v -> showRoleSelection());
    }

    private void showLogin() {
        setupScreen("Login");

        EditText phoneInput = input("Phone number");
        phoneInput.setInputType(InputType.TYPE_CLASS_PHONE);

        TextView info = new TextView(this);
        info.setText("We'll send a one-time verification code.");
        root.addView(info);

        Button login = button("SEND OTP");

        login.setOnClickListener(v -> {
            String phone = phoneInput.getText().toString().trim();

            if (!phone.startsWith("+63") || phone.length() < 12) {
                toast("Use Philippine format: +639XXXXXXXXX");
                return;
            }

            pendingPhone = phone;
            sendOtp(phone, false);
        });

        Button back = button("BACK");
        back.setOnClickListener(v -> showHome());
    }

    private void sendOtp(String phone, boolean registration) {

        Toast.makeText(this, "Sending OTP...", Toast.LENGTH_SHORT).show();

        PhoneAuthOptions options =
                PhoneAuthOptions.newBuilder(auth)
                        .setPhoneNumber(phone)
                        .setTimeout(60L, TimeUnit.SECONDS)
                        .setActivity(this)
                        .setCallbacks(new PhoneAuthProvider.OnVerificationStateChangedCallbacks() {

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
                        })
                        .build();

        PhoneAuthProvider.verifyPhoneNumber(options);
    }

    private void showOtpScreen(boolean registration) {
        setupScreen("Enter OTP");

        TextView info = new TextView(this);
        info.setText("A verification code was sent to:\n" + pendingPhone);
        info.setTextSize(17);
        info.setGravity(Gravity.CENTER);
        root.addView(info);

        EditText otpInput = input("6-digit OTP");
        otpInput.setInputType(InputType.TYPE_CLASS_NUMBER);
        otpInput.setGravity(Gravity.CENTER);

        Button verify = button("VERIFY OTP");

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

        Button resend = button("RESEND OTP");

        resend.setOnClickListener(v -> {
            if (resendToken != null) {
                resendOtp(registration);
            } else {
                sendOtp(pendingPhone, registration);
            }
        });

        Button back = button("BACK");
        back.setOnClickListener(v -> showHome());
    }

    private void resendOtp(boolean registration) {

        PhoneAuthOptions options =
                PhoneAuthOptions.newBuilder(auth)
                        .setPhoneNumber(pendingPhone)
                        .setTimeout(60L, TimeUnit.SECONDS)
                        .setActivity(this)
                        .setForceResendingToken(resendToken)
                        .setCallbacks(new PhoneAuthProvider.OnVerificationStateChangedCallbacks() {

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
                        })
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

                    Boolean suspended =
                            document.getBoolean("suspended");

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

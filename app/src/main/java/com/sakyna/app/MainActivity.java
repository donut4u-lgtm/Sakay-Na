
package com.sakyna.app;

import android.content.Intent;
import android.content.SharedPreferences;
import android.graphics.Color;
import android.os.Bundle;
import android.text.InputType;
import android.view.Gravity;
import android.view.View;
import android.widget.Button;
import android.widget.EditText;
import android.widget.LinearLayout;
import android.widget.RadioButton;
import android.widget.RadioGroup;
import android.widget.ScrollView;
import android.widget.TextView;
import android.widget.Toast;

import androidx.appcompat.app.AppCompatActivity;

import com.google.firebase.Timestamp;
import com.google.firebase.auth.FirebaseAuth;
import com.google.firebase.auth.FirebaseUser;
import com.google.firebase.firestore.FirebaseFirestore;

import java.util.HashMap;
import java.util.Map;

public class MainActivity extends AppCompatActivity {

    private FirebaseAuth auth;
    private FirebaseFirestore db;

    private LinearLayout root;
    private EditText phoneInput;
    private EditText passwordInput;
    private EditText nameInput;
    private EditText confirmPasswordInput;
    private RadioGroup roleGroup;

    private static final int GREEN = Color.rgb(0, 150, 80);
    private static final int DARK = Color.rgb(30, 30, 30);
    private static final int LIGHT = Color.rgb(245, 248, 246);
    private static final int ORANGE = Color.rgb(245, 150, 30);
    private static final int BLUE = Color.rgb(35, 110, 210);
    private static final int PURPLE = Color.rgb(125, 70, 180);

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);

        auth = FirebaseAuth.getInstance();
        db = FirebaseFirestore.getInstance();

        showHome();
    }

    private void setupRoot() {
        ScrollView scrollView = new ScrollView(this);

        root = new LinearLayout(this);
        root.setOrientation(LinearLayout.VERTICAL);
        root.setPadding(35, 45, 35, 45);
        root.setGravity(Gravity.CENTER_HORIZONTAL);
        root.setBackgroundColor(LIGHT);

        scrollView.addView(root);

        setContentView(scrollView);
    }

    private TextView title(String text, int size) {
        TextView tv = new TextView(this);
        tv.setText(text);
        tv.setTextSize(size);
        tv.setTextColor(DARK);
        tv.setGravity(Gravity.CENTER);
        tv.setPadding(10, 15, 10, 15);

        return tv;
    }

    private EditText input(String hint) {
        EditText edit = new EditText(this);
        edit.setHint(hint);
        edit.setTextSize(17);
        edit.setSingleLine(true);
        edit.setPadding(25, 18, 25, 18);

        LinearLayout.LayoutParams params =
                new LinearLayout.LayoutParams(
                        LinearLayout.LayoutParams.MATCH_PARENT,
                        LinearLayout.LayoutParams.WRAP_CONTENT);

        params.setMargins(0, 8, 0, 8);

        edit.setLayoutParams(params);

        return edit;
    }

    private Button button(String text, int color) {
        Button button = new Button(this);
        button.setText(text);
        button.setTextSize(17);
        button.setTextColor(Color.WHITE);
        button.setBackgroundColor(color);
        button.setAllCaps(false);

        LinearLayout.LayoutParams params =
                new LinearLayout.LayoutParams(
                        LinearLayout.LayoutParams.MATCH_PARENT,
                        LinearLayout.LayoutParams.WRAP_CONTENT);

        params.setMargins(0, 10, 0, 10);

        button.setLayoutParams(params);

        return button;
    }

    private void showHome() {

        setupRoot();

        TextView logo = title("🛺", 60);
        root.addView(logo);

        TextView appName = title("SAKAY NA", 34);
        appName.setTextColor(GREEN);
        appName.setTypeface(null, android.graphics.Typeface.BOLD);
        root.addView(appName);

        TextView subtitle =
                title("TRICYCLE RIDE-HAILING", 18);
        root.addView(subtitle);

        TextView description =
                title("Safe • Simple • Local", 16);
        root.addView(description);

        TextView loginInfo =
                title("PHONE NUMBER + PASSWORD\nNO SMS • NO OTP", 14);
        loginInfo.setTextColor(GREEN);
        root.addView(loginInfo);

        Button login = button("LOGIN", GREEN);
        root.addView(login);

        login.setOnClickListener(v -> showLogin());

        Button register = button("REGISTER", BLUE);
        root.addView(register);

        register.setOnClickListener(v -> showRegister());

        Button about = button("ABOUT SAKAY NA", Color.DKGRAY);
        root.addView(about);

        about.setOnClickListener(v -> showAbout());
    }

    private void showLogin() {

        setupRoot();

        TextView heading = title("LOGIN", 30);
        heading.setTextColor(GREEN);
        heading.setTypeface(null, android.graphics.Typeface.BOLD);
        root.addView(heading);

        TextView info =
                title("Enter your registered phone number and password.\n\nNo OTP required.", 15);
        info.setTextColor(DARK);
        root.addView(info);

        phoneInput = input("Phone number");
        phoneInput.setInputType(InputType.TYPE_CLASS_PHONE);
        root.addView(phoneInput);

        passwordInput = input("Password");
        passwordInput.setInputType(
                InputType.TYPE_CLASS_TEXT |
                        InputType.TYPE_TEXT_VARIATION_PASSWORD);
        root.addView(passwordInput);

        Button login = button("LOGIN", GREEN);
        root.addView(login);

        login.setOnClickListener(v -> loginUser());

        Button register = button("CREATE NEW ACCOUNT", BLUE);
        root.addView(register);

        register.setOnClickListener(v -> showRegister());

        Button back = button("BACK", Color.DKGRAY);
        root.addView(back);

        back.setOnClickListener(v -> showHome());
    }

    private void showRegister() {

        setupRoot();

        TextView heading = title("CREATE ACCOUNT", 28);
        heading.setTextColor(BLUE);
        heading.setTypeface(null, android.graphics.Typeface.BOLD);
        root.addView(heading);

        TextView info =
                title("Create your Sakay Na account.\n\nPhone number + password only.\nNO OTP.", 15);
        root.addView(info);

        nameInput = input("Full name");
        root.addView(nameInput);

        phoneInput = input("Phone number");
        phoneInput.setInputType(InputType.TYPE_CLASS_PHONE);
        root.addView(phoneInput);

        passwordInput = input("Password");
        passwordInput.setInputType(
                InputType.TYPE_CLASS_TEXT |
                        InputType.TYPE_TEXT_VARIATION_PASSWORD);
        root.addView(passwordInput);

        confirmPasswordInput = input("Confirm password");
        confirmPasswordInput.setInputType(
                InputType.TYPE_CLASS_TEXT |
                        InputType.TYPE_TEXT_VARIATION_PASSWORD);
        root.addView(confirmPasswordInput);

        TextView roleTitle = title("SELECT ACCOUNT TYPE", 18);
        roleTitle.setTextColor(DARK);
        root.addView(roleTitle);

        roleGroup = new RadioGroup(this);
        roleGroup.setOrientation(RadioGroup.VERTICAL);

        RadioButton passenger = new RadioButton(this);
        passenger.setText("Passenger");
        passenger.setTextSize(17);
        passenger.setChecked(true);

        RadioButton driver = new RadioButton(this);
        driver.setText("Driver");
        driver.setTextSize(17);

        RadioButton admin = new RadioButton(this);
        admin.setText("Admin");
        admin.setTextSize(17);

        roleGroup.addView(passenger);
        roleGroup.addView(driver);
        roleGroup.addView(admin);

        root.addView(roleGroup);

        Button create = button("CREATE ACCOUNT", BLUE);
        root.addView(create);

        create.setOnClickListener(v -> registerUser());

        Button back = button("BACK", Color.DKGRAY);
        root.addView(back);

        back.setOnClickListener(v -> showHome());
    }

    private void registerUser() {

        String name = nameInput.getText().toString().trim();
        String phone = phoneInput.getText().toString().trim();
        String password = passwordInput.getText().toString();
        String confirm = confirmPasswordInput.getText().toString();

        if (name.isEmpty()) {
            toast("Enter your full name.");
            return;
        }

        if (phone.isEmpty()) {
            toast("Enter your phone number.");
            return;
        }

        if (password.length() < 6) {
            toast("Password must be at least 6 characters.");
            return;
        }

        if (!password.equals(confirm)) {
            toast("Passwords do not match.");
            return;
        }

        String role = getSelectedRole();

        if (role.isEmpty()) {
            toast("Select an account type.");
            return;
        }

        String email = makeFirebaseEmail(phone);

        Toast.makeText(
                this,
                "Creating account...",
                Toast.LENGTH_SHORT
        ).show();

        auth.createUserWithEmailAndPassword(email, password)
                .addOnCompleteListener(task -> {

                    if (!task.isSuccessful()) {

                        String message = "Registration failed.";

                        if (task.getException() != null) {
                            message = task.getException().getMessage();
                        }

                        toast(message);
                        return;
                    }

                    FirebaseUser user = auth.getCurrentUser();

                    if (user == null) {
                        toast("Account created but user session is missing.");
                        return;
                    }

                    String uid = user.getUid();

                    Map<String, Object> profile = new HashMap<>();

                    profile.put("uid", uid);
                    profile.put("name", name);
                    profile.put("phone", phone);
                    profile.put("role", role);
                    profile.put("email", email);
                    profile.put("createdAt", Timestamp.now());
                    profile.put("active", true);

                    db.collection("users")
                            .document(uid)
                            .set(profile)
                            .addOnSuccessListener(unused -> {

                                saveCurrentUser(
                                        uid,
                                        name,
                                        phone,
                                        role
                                );

                                toast("Account created successfully.");

                                openRoleScreen(role);
                            })
                            .addOnFailureListener(e -> {

                                toast(
                                        "Account created, but profile save failed: "
                                                + e.getMessage()
                                );
                            });
                });
    }

    private void loginUser() {

        String phone = phoneInput.getText().toString().trim();
        String password = passwordInput.getText().toString();

        if (phone.isEmpty()) {
            toast("Enter your phone number.");
            return;
        }

        if (password.isEmpty()) {
            toast("Enter your password.");
            return;
        }

        String email = makeFirebaseEmail(phone);

        Toast.makeText(
                this,
                "Logging in...",
                Toast.LENGTH_SHORT
        ).show();

        auth.signInWithEmailAndPassword(email, password)
                .addOnCompleteListener(task -> {

                    if (!task.isSuccessful()) {

                        String message = "Login failed.";

                        if (task.getException() != null) {
                            message = task.getException().getMessage();
                        }

                        toast(message);
                        return;
                    }

                    FirebaseUser user = auth.getCurrentUser();

                    if (user == null) {
                        toast("Login succeeded but user session is missing.");
                        return;
                    }

                    loadUserProfile(user.getUid());
                });
    }

    private void loadUserProfile(String uid) {

        db.collection("users")
                .document(uid)
                .get()
                .addOnSuccessListener(document -> {

                    if (!document.exists()) {
                        toast("Account profile not found.");
                        return;
                    }

                    String name = document.getString("name");
                    String phone = document.getString("phone");
                    String role = document.getString("role");

                    if (name == null) {
                        name = "";
                    }

                    if (phone == null) {
                        phone = "";
                    }

                    if (role == null) {
                        role = "";
                    }

                    saveCurrentUser(
                            uid,
                            name,
                            phone,
                            role
                    );

                    openRoleScreen(role);
                })
                .addOnFailureListener(e ->
                        toast(
                                "Could not load account: "
                                        + e.getMessage()
                        )
                );
    }

    private void openRoleScreen(String role) {

        if (role.equalsIgnoreCase("Passenger")) {

            startActivity(
                    new Intent(
                            MainActivity.this,
                            PassengerActivity.class
                    )
            );

            finish();

        } else if (role.equalsIgnoreCase("Driver")) {

            startActivity(
                    new Intent(
                            MainActivity.this,
                            DriverActivity.class
                    )
            );

            finish();

        } else if (role.equalsIgnoreCase("Admin")) {

            startActivity(
                    new Intent(
                            MainActivity.this,
                            AdminActivity.class
                    )
            );

            finish();

        } else {

            toast("Unknown account type: " + role);
        }
    }

    private String getSelectedRole() {

        int selectedId = roleGroup.getCheckedRadioButtonId();

        if (selectedId == -1) {
            return "";
        }

        RadioButton selected =
                roleGroup.findViewById(selectedId);

        if (selected == null) {
            return "";
        }

        return selected.getText().toString();
    }

    private String makeFirebaseEmail(String phone) {

        String clean =
                phone.replaceAll("[^0-9]", "");

        if (clean.startsWith("0")) {
            clean = "63" + clean.substring(1);
        }

        return clean + "@sakyna.app";
    }

    private void saveCurrentUser(
            String uid,
            String name,
            String phone,
            String role) {

        SharedPreferences preferences =
                getSharedPreferences(
                        "SakayNa",
                        MODE_PRIVATE
                );

        preferences.edit()
                .putString("current_user", uid)
                .putString("name", name)
                .putString("phone", phone)
                .putString("role", role)
                .apply();
    }

    private void showAbout() {

        setupRoot();

        TextView heading = title("ABOUT SAKAY NA", 28);
        heading.setTextColor(GREEN);
        heading.setTypeface(null, android.graphics.Typeface.BOLD);
        root.addView(heading);

        TextView about = title(
                "SAKAY NA\n\n" +
                        "A local tricycle ride-hailing application.\n\n" +
                        "Passengers can request rides.\n" +
                        "Drivers can accept rides.\n" +
                        "Admins can manage the system.\n\n" +
                        "ACCOUNT SYSTEM\n" +
                        "Phone number + password\n" +
                        "No SMS OTP required.",
                17
        );

        root.addView(about);

        Button back = button("BACK", GREEN);
        root.addView(back);

        back.setOnClickListener(v -> showHome());
    }

    private void toast(String message) {
        Toast.makeText(
                this,
                message,
                Toast.LENGTH_LONG
        ).show();
    }
}

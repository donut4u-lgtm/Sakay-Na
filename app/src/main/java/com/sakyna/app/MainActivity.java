
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
import android.widget.TextView;
import android.widget.Toast;

import androidx.appcompat.app.AppCompatActivity;

import com.google.firebase.auth.FirebaseAuth;
import com.google.firebase.firestore.FirebaseFirestore;

import java.util.HashMap;
import java.util.Map;

public class MainActivity extends AppCompatActivity {

    private FirebaseAuth auth;
    private FirebaseFirestore db;
    private SharedPreferences prefs;

    private final int GREEN = Color.rgb(20, 150, 80);
    private final int ORANGE = Color.rgb(245, 150, 30);
    private final int BLUE = Color.rgb(40, 110, 220);
    private final int PURPLE = Color.rgb(120, 70, 180);
    private final int DARK = Color.rgb(35, 35, 35);
    private final int LIGHT = Color.rgb(245, 248, 246);

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);

        auth = FirebaseAuth.getInstance();
        db = FirebaseFirestore.getInstance();
        prefs = getSharedPreferences("SakayNa", MODE_PRIVATE);

        showHome();
    }

    private LinearLayout baseLayout() {
        LinearLayout layout = new LinearLayout(this);
        layout.setOrientation(LinearLayout.VERTICAL);
        layout.setGravity(Gravity.CENTER);
        layout.setPadding(40, 40, 40, 40);
        layout.setBackgroundColor(LIGHT);
        return layout;
    }

    private TextView title(String text) {
        TextView view = new TextView(this);
        view.setText(text);
        view.setTextColor(DARK);
        view.setTextSize(30);
        view.setGravity(Gravity.CENTER);
        view.setPadding(10, 20, 10, 20);
        return view;
    }

    private TextView subtitle(String text) {
        TextView view = new TextView(this);
        view.setText(text);
        view.setTextColor(Color.DKGRAY);
        view.setTextSize(16);
        view.setGravity(Gravity.CENTER);
        view.setPadding(10, 5, 10, 25);
        return view;
    }

    private EditText field(String hint) {
        EditText edit = new EditText(this);
        edit.setHint(hint);
        edit.setTextSize(17);
        edit.setSingleLine(true);
        edit.setPadding(25, 18, 25, 18);

        LinearLayout.LayoutParams params =
                new LinearLayout.LayoutParams(
                        LinearLayout.LayoutParams.MATCH_PARENT,
                        LinearLayout.LayoutParams.WRAP_CONTENT
                );

        params.setMargins(0, 8, 0, 8);
        edit.setLayoutParams(params);

        return edit;
    }

    private Button button(String text, int color) {
        Button b = new Button(this);
        b.setText(text);
        b.setTextSize(17);
        b.setTextColor(Color.WHITE);
        b.setAllCaps(false);
        b.setBackgroundColor(color);

        LinearLayout.LayoutParams params =
                new LinearLayout.LayoutParams(
                        LinearLayout.LayoutParams.MATCH_PARENT,
                        LinearLayout.LayoutParams.WRAP_CONTENT
                );

        params.setMargins(0, 10, 0, 10);
        b.setLayoutParams(params);

        return b;
    }

    private void showHome() {

        LinearLayout layout = baseLayout();

        TextView logo = title("🛺 SAKAY NA");
        logo.setTextSize(34);

        layout.addView(logo);
        layout.addView(subtitle(
                "TRICYCLE RIDE-HAILING\n\nSafe • Simple • Local"
        ));

        Button login = button("LOGIN", GREEN);
        Button register = button("REGISTER", BLUE);
        Button about = button("ABOUT SAKAY NA", PURPLE);

        layout.addView(login);
        layout.addView(register);
        layout.addView(about);

        login.setOnClickListener(v -> showLogin());
        register.setOnClickListener(v -> showRegister());
        about.setOnClickListener(v -> showAbout());

        setContentView(layout);
    }

    private void showLogin() {

        LinearLayout layout = baseLayout();

        layout.addView(title("LOGIN"));
        layout.addView(subtitle("Phone number + password"));

        EditText phone = field("Phone Number");
        phone.setInputType(InputType.TYPE_CLASS_PHONE);

        EditText password = field("Password");
        password.setInputType(
                InputType.TYPE_CLASS_TEXT |
                InputType.TYPE_TEXT_VARIATION_PASSWORD
        );

        Button login = button("LOGIN", GREEN);
        Button back = button("BACK", Color.GRAY);

        layout.addView(phone);
        layout.addView(password);
        layout.addView(login);
        layout.addView(back);

        login.setOnClickListener(v -> {

            String phoneText = phone.getText().toString().trim();
            String passText = password.getText().toString();

            if (phoneText.isEmpty()) {
                phone.setError("Enter phone number");
                return;
            }

            if (passText.isEmpty()) {
                password.setError("Enter password");
                return;
            }

            login.setEnabled(false);
            login.setText("LOGGING IN...");

            String email = makeFirebaseEmail(phoneText);

            auth.signInWithEmailAndPassword(email, passText)
                    .addOnCompleteListener(task -> {

                        if (task.isSuccessful()) {

                            String uid = auth.getCurrentUser().getUid();

                            db.collection("users")
                                    .document(uid)
                                    .get()
                                    .addOnSuccessListener(document -> {

                                        if (!document.exists()) {
                                            login.setEnabled(true);
                                            login.setText("LOGIN");
                                            Toast.makeText(
                                                    this,
                                                    "Account profile not found.",
                                                    Toast.LENGTH_LONG
                                            ).show();
                                            return;
                                        }

                                        String role =
                                                document.getString("role");

                                        String name =
                                                document.getString("name");

                                        String savedPhone =
                                                document.getString("phone");

                                        saveUser(
                                                uid,
                                                name,
                                                savedPhone,
                                                role
                                        );

                                        openRole(role);

                                    })
                                    .addOnFailureListener(e -> {

                                        login.setEnabled(true);
                                        login.setText("LOGIN");

                                        Toast.makeText(
                                                this,
                                                "Database error: " +
                                                        e.getMessage(),
                                                Toast.LENGTH_LONG
                                        ).show();
                                    });

                        } else {

                            login.setEnabled(true);
                            login.setText("LOGIN");

                            String message = "Login failed.";

                            if (task.getException() != null) {
                                message =
                                        task.getException().getMessage();
                            }

                            Toast.makeText(
                                    this,
                                    message,
                                    Toast.LENGTH_LONG
                            ).show();
                        }
                    });
        });

        back.setOnClickListener(v -> showHome());

        setContentView(layout);
    }

    private void showRegister() {

        LinearLayout layout = baseLayout();

        layout.addView(title("CREATE ACCOUNT"));
        layout.addView(subtitle(
                "Register using your phone number\nNo SMS • No OTP • No email required"
        ));

        EditText name = field("Full Name");

        EditText phone = field("Phone Number");
        phone.setInputType(InputType.TYPE_CLASS_PHONE);

        EditText password = field("Password");
        password.setInputType(
                InputType.TYPE_CLASS_TEXT |
                InputType.TYPE_TEXT_VARIATION_PASSWORD
        );

        EditText confirm = field("Confirm Password");
        confirm.setInputType(
                InputType.TYPE_CLASS_TEXT |
                InputType.TYPE_TEXT_VARIATION_PASSWORD
        );

        layout.addView(name);
        layout.addView(phone);
        layout.addView(password);
        layout.addView(confirm);

        TextView roleTitle = new TextView(this);
        roleTitle.setText("SELECT ACCOUNT TYPE");
        roleTitle.setTextSize(18);
        roleTitle.setTextColor(DARK);
        roleTitle.setGravity(Gravity.CENTER);
        roleTitle.setPadding(0, 25, 0, 10);

        layout.addView(roleTitle);

        final String[] selectedRole = {"PASSENGER"};

        Button passenger = button("🟧  PASSENGER", ORANGE);
        Button driver = button("🟦  DRIVER", BLUE);
        Button admin = button("🟪  ADMIN", PURPLE);

        layout.addView(passenger);
        layout.addView(driver);
        layout.addView(admin);

        TextView selectedText = new TextView(this);
        selectedText.setText(
                "Selected: PASSENGER"
        );
        selectedText.setTextSize(16);
        selectedText.setGravity(Gravity.CENTER);
        selectedText.setTextColor(GREEN);
        selectedText.setPadding(0, 10, 0, 10);

        layout.addView(selectedText);

        passenger.setOnClickListener(v -> {
            selectedRole[0] = "PASSENGER";
            selectedText.setText("Selected: PASSENGER");
        });

        driver.setOnClickListener(v -> {
            selectedRole[0] = "DRIVER";
            selectedText.setText("Selected: DRIVER");
        });

        admin.setOnClickListener(v -> {
            selectedRole[0] = "ADMIN";
            selectedText.setText("Selected: ADMIN");
        });

        Button register = button("REGISTER", GREEN);
        Button back = button("BACK", Color.GRAY);

        layout.addView(register);
        layout.addView(back);

        register.setOnClickListener(v -> {

            String nameText = name.getText().toString().trim();
            String phoneText = phone.getText().toString().trim();
            String passText = password.getText().toString();
            String confirmText = confirm.getText().toString();

            if (nameText.isEmpty()) {
                name.setError("Enter your name");
                return;
            }

            if (phoneText.isEmpty()) {
                phone.setError("Enter phone number");
                return;
            }

            if (passText.length() < 6) {
                password.setError(
                        "Password must be at least 6 characters"
                );
                return;
            }

            if (!passText.equals(confirmText)) {
                confirm.setError("Passwords do not match");
                return;
            }

            String role = selectedRole[0];

            register.setEnabled(false);
            register.setText("CREATING ACCOUNT...");

            String email = makeFirebaseEmail(phoneText);

            auth.createUserWithEmailAndPassword(
                            email,
                            passText
                    )
                    .addOnCompleteListener(task -> {

                        if (!task.isSuccessful()) {

                            register.setEnabled(true);
                            register.setText("REGISTER");

                            String message =
                                    "Registration failed.";

                            if (task.getException() != null) {
                                message =
                                        task.getException().getMessage();
                            }

                            Toast.makeText(
                                    this,
                                    message,
                                    Toast.LENGTH_LONG
                            ).show();

                            return;
                        }

                        String uid =
                                auth.getCurrentUser().getUid();

                        Map<String, Object> user =
                                new HashMap<>();

                        user.put("uid", uid);
                        user.put("name", nameText);
                        user.put("phone", normalizePhone(phoneText));
                        user.put("role", role);
                        user.put("active", true);

                        db.collection("users")
                                .document(uid)
                                .set(user)
                                .addOnSuccessListener(unused -> {

                                    saveUser(
                                            uid,
                                            nameText,
                                            normalizePhone(phoneText),
                                            role
                                    );

                                    Toast.makeText(
                                            this,
                                            "Account created successfully!",
                                            Toast.LENGTH_LONG
                                    ).show();

                                    openRole(role);
                                })
                                .addOnFailureListener(e -> {

                                    register.setEnabled(true);
                                    register.setText("REGISTER");

                                    Toast.makeText(
                                            this,
                                            "Profile error: " +
                                                    e.getMessage(),
                                            Toast.LENGTH_LONG
                                    ).show();
                                });
                    });
        });

        back.setOnClickListener(v -> showHome());

        setContentView(layout);
    }

    private void showAbout() {

        LinearLayout layout = baseLayout();

        layout.addView(title("🛺 SAKAY NA"));

        layout.addView(subtitle(
                "A local tricycle ride-hailing application.\n\n" +
                "Passenger • Driver • Admin\n\n" +
                "Phone number + password\n" +
                "No SMS OTP"
        ));

        Button back = button("BACK", GREEN);

        layout.addView(back);

        back.setOnClickListener(v -> showHome());

        setContentView(layout);
    }

    private String normalizePhone(String phone) {

        String clean =
                phone.replaceAll("[^0-9]", "");

        if (clean.startsWith("0")) {
            clean = "63" + clean.substring(1);
        }

        return clean;
    }

    private String makeFirebaseEmail(String phone) {

        return normalizePhone(phone) + "@sakyna.app";
    }

    private void saveUser(
            String uid,
            String name,
            String phone,
            String role) {

        prefs.edit()
                .putString("uid", uid)
                .putString("name", name)
                .putString("phone", phone)
                .putString("role", role)
                .apply();
    }

    private void openRole(String role) {

        if (role == null) {
            Toast.makeText(
                    this,
                    "Account type not found.",
                    Toast.LENGTH_LONG
            ).show();
            return;
        }

        if (role.equalsIgnoreCase("PASSENGER")) {

            startActivity(
                    new Intent(
                            this,
                            PassengerActivity.class
                    )
            );

        } else if (role.equalsIgnoreCase("DRIVER")) {

            startActivity(
                    new Intent(
                            this,
                            DriverActivity.class
                    )
            );

        } else if (role.equalsIgnoreCase("ADMIN")) {

            startActivity(
                    new Intent(
                            this,
                            AdminActivity.class
                    )
            );
        }
    }
}

package com.sakyna.app;

import android.app.Activity;
import android.graphics.Color;
import android.os.Bundle;
import android.text.InputType;
import android.view.Gravity;
import android.widget.Button;
import android.widget.EditText;
import android.widget.LinearLayout;
import android.widget.ScrollView;
import android.widget.TextView;
import android.widget.Toast;

import com.google.firebase.auth.FirebaseAuth;
import com.google.firebase.functions.FirebaseFunctions;
import com.google.firebase.functions.HttpsCallableResult;

import java.util.HashMap;
import java.util.Map;

public class AdminPasswordResetActivity extends Activity {

    private static final String ADMIN_UID =
            "Ld3rzaCvAGNlXBDCofB3mWjgXWp2";

    private FirebaseAuth auth;
    private FirebaseFunctions functions;

    private EditText phoneField;
    private EditText passwordField;
    private TextView accountResult;
    private Button resetButton;

    private String targetUid = "";

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);

        auth = FirebaseAuth.getInstance();
        functions = FirebaseFunctions.getInstance("asia-southeast1");

        buildScreen();
    }

    private void buildScreen() {

        LinearLayout root = new LinearLayout(this);
        root.setOrientation(LinearLayout.VERTICAL);
        root.setPadding(28, 28, 28, 28);
        root.setBackgroundColor(Color.WHITE);

        TextView title = new TextView(this);
        title.setText("🔐 SAKAY NA ADMIN\nPASSWORD RESET");
        title.setTextSize(25);
        title.setTextColor(Color.rgb(0, 125, 80));
        title.setGravity(Gravity.CENTER);
        title.setPadding(10, 10, 10, 25);
        root.addView(title);

        TextView instructions = new TextView(this);
        instructions.setText(
                "Admin only.\n\n" +
                "1. Enter the registered phone number.\n" +
                "2. Tap SEARCH ACCOUNT.\n" +
                "3. Verify the account owner and role.\n" +
                "4. Set a temporary password.\n" +
                "5. Give the temporary password to the owner privately.\n\n" +
                "Never ask the owner to send an existing password."
        );
        instructions.setTextSize(16);
        instructions.setTextColor(Color.DKGRAY);
        instructions.setPadding(5, 5, 5, 20);
        root.addView(instructions);

        TextView phoneLabel = new TextView(this);
        phoneLabel.setText("Registered Phone Number");
        phoneLabel.setTextSize(16);
        phoneLabel.setTextColor(Color.BLACK);
        root.addView(phoneLabel);

        phoneField = new EditText(this);
        phoneField.setHint("097... or 095...");
        phoneField.setTextSize(17);
        phoneField.setSingleLine(true);
        phoneField.setInputType(InputType.TYPE_CLASS_PHONE);
        root.addView(phoneField);

        Button searchButton = new Button(this);
        searchButton.setText("🔎 SEARCH ACCOUNT");
        searchButton.setTextSize(17);
        root.addView(searchButton);

        accountResult = new TextView(this);
        accountResult.setText("No account searched yet.");
        accountResult.setTextSize(17);
        accountResult.setTextColor(Color.DKGRAY);
        accountResult.setPadding(8, 18, 8, 18);
        root.addView(accountResult);

        TextView passwordLabel = new TextView(this);
        passwordLabel.setText("Temporary Password");
        passwordLabel.setTextSize(16);
        passwordLabel.setTextColor(Color.BLACK);
        root.addView(passwordLabel);

        passwordField = new EditText(this);
        passwordField.setHint("At least 6 characters");
        passwordField.setTextSize(17);
        passwordField.setSingleLine(true);
        passwordField.setInputType(
                InputType.TYPE_CLASS_TEXT |
                InputType.TYPE_TEXT_VARIATION_PASSWORD
        );
        root.addView(passwordField);

        resetButton = new Button(this);
        resetButton.setText("🔑 SET TEMPORARY PASSWORD");
        resetButton.setTextSize(17);
        resetButton.setEnabled(false);
        root.addView(resetButton);

        Button backButton = new Button(this);
        backButton.setText("BACK TO ADMIN");
        backButton.setTextSize(16);
        root.addView(backButton);

        ScrollView scroll = new ScrollView(this);
        scroll.addView(root);
        setContentView(scroll);

        searchButton.setOnClickListener(v -> searchAccount());
        resetButton.setOnClickListener(v -> resetPassword());
        backButton.setOnClickListener(v -> finish());
    }

    private boolean isAdmin() {

        return auth.getCurrentUser() != null
                && ADMIN_UID.equals(
                auth.getCurrentUser().getUid()
        );
    }

    private void searchAccount() {

        if (!isAdmin()) {

            Toast.makeText(
                    this,
                    "ACCESS DENIED: Admin only.",
                    Toast.LENGTH_LONG
            ).show();

            return;
        }

        String phone = phoneField.getText()
                .toString()
                .trim();

        if (phone.isEmpty()) {

            phoneField.setError(
                    "Enter the registered phone number."
            );

            return;
        }

        targetUid = "";
        resetButton.setEnabled(false);
        accountResult.setText(
                "Searching account..."
        );

        Map<String, Object> data =
                new HashMap<>();

        data.put("phone", phone);

        functions
                .getHttpsCallable(
                        "adminFindUserByPhone"
                )
                .call(data)
                .addOnSuccessListener(
                        (HttpsCallableResult result) -> {

                            Object raw =
                                    result.getData();

                            if (!(raw instanceof Map)) {

                                accountResult.setText(
                                        "No account found."
                                );

                                return;
                            }

                            Map<?, ?> map =
                                    (Map<?, ?>) raw;

                            Object uidValue =
                                    map.get("uid");

                            if (uidValue == null) {

                                accountResult.setText(
                                        "No Passenger / Driver account found."
                                );

                                return;
                            }

                            targetUid =
                                    String.valueOf(
                                            uidValue
                                    );

                            String name =
                                    value(map.get("name"));

                            String role =
                                    value(map.get("role"));

                            String registeredPhone =
                                    value(
                                            map.get("phone")
                                    );

                            accountResult.setText(
                                    "✅ ACCOUNT FOUND\n\n" +
                                    "Name: " + name + "\n" +
                                    "Role: " + role + "\n" +
                                    "Phone: " +
                                    registeredPhone + "\n\n" +
                                    "UID: " + targetUid
                            );

                            resetButton.setEnabled(
                                    !"ADMIN".equalsIgnoreCase(
                                            role
                                    )
                            );
                        }
                )
                .addOnFailureListener(error -> {

                    targetUid = "";
                    resetButton.setEnabled(false);

                    String message =
                            error.getMessage();

                    if (message == null ||
                            message.trim().isEmpty()) {

                        message =
                                "Account search failed.";
                    }

                    accountResult.setText(
                            "❌ " + message
                    );
                });
    }

    private void resetPassword() {

        if (!isAdmin()) {

            Toast.makeText(
                    this,
                    "ACCESS DENIED: Admin only.",
                    Toast.LENGTH_LONG
            ).show();

            return;
        }

        if (targetUid.isEmpty()) {

            Toast.makeText(
                    this,
                    "Search the account first.",
                    Toast.LENGTH_LONG
            ).show();

            return;
        }

        String newPassword =
                passwordField.getText().toString();

        if (newPassword.length() < 6) {

            passwordField.setError(
                    "Temporary password must be at least 6 characters."
            );

            return;
        }

        resetButton.setEnabled(false);
        resetButton.setText(
                "SETTING PASSWORD..."
        );

        Map<String, Object> data =
                new HashMap<>();

        data.put(
                "targetUid",
                targetUid
        );

        data.put(
                "newPassword",
                newPassword
        );

        functions
                .getHttpsCallable(
                        "adminResetUserPassword"
                )
                .call(data)
                .addOnSuccessListener(
                        result -> {

                            resetButton.setEnabled(false);

                            resetButton.setText(
                                    "🔑 PASSWORD SET"
                            );

                            passwordField.setText("");

                            Toast.makeText(
                                    this,
                                    "TEMPORARY PASSWORD SET. Send it privately to the account owner.",
                                    Toast.LENGTH_LONG
                            ).show();
                        }
                )
                .addOnFailureListener(error -> {

                    resetButton.setEnabled(true);

                    resetButton.setText(
                            "🔑 SET TEMPORARY PASSWORD"
                    );

                    String message =
                            error.getMessage();

                    if (message == null ||
                            message.trim().isEmpty()) {

                        message =
                                "Password reset failed.";
                    }

                    Toast.makeText(
                            this,
                            message,
                            Toast.LENGTH_LONG
                    ).show();
                });
    }

    private String value(Object value) {

        if (value == null) {
            return "Not provided";
        }

        String text =
                String.valueOf(value).trim();

        return text.isEmpty()
                ? "Not provided"
                : text;
    }
}

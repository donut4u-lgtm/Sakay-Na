package com.sakyna.app;

import android.app.Activity;
import android.os.Bundle;
import android.graphics.Color;
import android.text.InputType;
import android.view.Gravity;
import android.view.View;
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

    private FirebaseAuth auth;
    private FirebaseFunctions functions;

    private EditText uidField;
    private EditText passwordField;
    private Button resetButton;

    private static final String ADMIN_UID =
            "Ld3rzaCvAGNlXBDCofB3mWjgXWp2";

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
        root.setPadding(35, 35, 35, 35);
        root.setBackgroundColor(Color.WHITE);

        TextView title = new TextView(this);
        title.setText("SAKAY NA ADMIN\nPASSWORD RESET");
        title.setTextSize(24);
        title.setTextColor(Color.BLACK);
        title.setGravity(Gravity.CENTER);
        title.setPadding(10, 10, 10, 30);

        root.addView(title);

        TextView warning = new TextView(this);
        warning.setText(
                "Admin only.\n\n" +
                "This changes the Firebase Authentication password " +
                "without deleting the user's account or changing the UID."
        );
        warning.setTextSize(16);
        warning.setTextColor(Color.DKGRAY);
        warning.setPadding(10, 10, 10, 25);

        root.addView(warning);

        TextView uidLabel = new TextView(this);
        uidLabel.setText("User UID");
        uidLabel.setTextSize(16);
        uidLabel.setTextColor(Color.BLACK);

        root.addView(uidLabel);

        uidField = new EditText(this);
        uidField.setHint("Enter Passenger / Driver UID");
        uidField.setText(
                "K67cgAVvsyYEJxPFvAgGfOIPCE63"
        );
        uidField.setTextSize(16);
        uidField.setSingleLine(true);

        root.addView(uidField,
                new LinearLayout.LayoutParams(
                        -1,
                        LinearLayout.LayoutParams.WRAP_CONTENT
                )
        );

        TextView passwordLabel = new TextView(this);
        passwordLabel.setText("New Password");
        passwordLabel.setTextSize(16);
        passwordLabel.setTextColor(Color.BLACK);
        passwordLabel.setPadding(0, 25, 0, 0);

        root.addView(passwordLabel);

        passwordField = new EditText(this);
        passwordField.setHint("Enter new password");
        passwordField.setTextSize(16);
        passwordField.setSingleLine(true);
        passwordField.setInputType(
                InputType.TYPE_CLASS_TEXT |
                InputType.TYPE_TEXT_VARIATION_PASSWORD
        );

        root.addView(passwordField,
                new LinearLayout.LayoutParams(
                        -1,
                        LinearLayout.LayoutParams.WRAP_CONTENT
                )
        );

        resetButton = new Button(this);
        resetButton.setText("RESET PASSWORD");
        resetButton.setTextSize(17);
        resetButton.setAllCaps(false);

        LinearLayout.LayoutParams buttonParams =
                new LinearLayout.LayoutParams(
                        -1,
                        LinearLayout.LayoutParams.WRAP_CONTENT
                );

        buttonParams.setMargins(0, 35, 0, 15);

        root.addView(resetButton, buttonParams);

        Button closeButton = new Button(this);
        closeButton.setText("BACK TO ADMIN");
        closeButton.setTextSize(16);

        root.addView(closeButton);

        ScrollView scrollView = new ScrollView(this);
        scrollView.addView(root);

        setContentView(scrollView);

        resetButton.setOnClickListener(v -> resetPassword());

        closeButton.setOnClickListener(v -> finish());
    }

    private void resetPassword() {

        if (auth.getCurrentUser() == null) {
            Toast.makeText(
                    this,
                    "Admin is not logged in.",
                    Toast.LENGTH_LONG
            ).show();
            return;
        }

        String currentUid =
                auth.getCurrentUser().getUid();

        if (!ADMIN_UID.equals(currentUid)) {
            Toast.makeText(
                    this,
                    "ACCESS DENIED: Admin only.",
                    Toast.LENGTH_LONG
            ).show();
            return;
        }

        String targetUid =
                uidField.getText().toString().trim();

        String newPassword =
                passwordField.getText().toString();

        if (targetUid.isEmpty()) {
            uidField.setError("Enter the user UID");
            return;
        }

        if (targetUid.equals(ADMIN_UID)) {
            Toast.makeText(
                    this,
                    "You cannot reset the Admin account here.",
                    Toast.LENGTH_LONG
            ).show();
            return;
        }

        if (newPassword.length() < 6) {
            passwordField.setError(
                    "Password must be at least 6 characters"
            );
            return;
        }

        resetButton.setEnabled(false);
        resetButton.setText("RESETTING...");

        Map<String, Object> data =
                new HashMap<>();

        data.put("targetUid", targetUid);
        data.put("newPassword", newPassword);

        functions
                .getHttpsCallable("adminResetUserPassword")
                .call(data)
                .addOnSuccessListener(
                        (HttpsCallableResult result) -> {

                            resetButton.setEnabled(true);
                            resetButton.setText(
                                    "RESET PASSWORD"
                            );

                            passwordField.setText("");

                            Toast.makeText(
                                    this,
                                    "PASSWORD UPDATED SUCCESSFULLY",
                                    Toast.LENGTH_LONG
                            ).show();
                        }
                )
                .addOnFailureListener(error -> {

                    resetButton.setEnabled(true);
                    resetButton.setText(
                            "RESET PASSWORD"
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
}

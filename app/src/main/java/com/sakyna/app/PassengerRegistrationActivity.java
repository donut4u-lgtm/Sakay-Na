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
import android.widget.ScrollView;
import android.widget.TextView;
import android.widget.Toast;

import com.google.firebase.auth.FirebaseAuth;
import com.google.firebase.auth.FirebaseUser;
import com.google.firebase.firestore.FieldValue;
import com.google.firebase.firestore.FirebaseFirestore;

import java.util.HashMap;
import java.util.Map;

public class PassengerRegistrationActivity extends Activity {

    private FirebaseAuth auth;
    private FirebaseFirestore db;

    private EditText nameInput;
    private EditText phoneInput;
    private EditText passwordInput;
    private EditText confirmPasswordInput;
    private EditText provinceInput;
    private EditText townInput;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);

        auth = FirebaseAuth.getInstance();
        db = FirebaseFirestore.getInstance();

        buildScreen();
    }

    private void buildScreen() {

        ScrollView scrollView = new ScrollView(this);

        LinearLayout root = new LinearLayout(this);
        root.setOrientation(LinearLayout.VERTICAL);
        root.setPadding(30, 30, 30, 30);
        root.setBackgroundColor(Color.rgb(238, 248, 243));

        scrollView.addView(root);

        TextView title = new TextView(this);
        title.setText("🧑‍🤝‍🧑 SAKAY NA\nPASSENGER ACCOUNT");
        title.setTextSize(27);
        title.setTextColor(Color.WHITE);
        title.setGravity(Gravity.CENTER);
        title.setPadding(12, 25, 12, 25);
        title.setBackgroundColor(Color.rgb(0, 125, 75));

        root.addView(title);

        TextView description = new TextView(this);
        description.setText(
                "Create your passenger account using your phone number and password."
        );
        description.setTextSize(16);
        description.setTextColor(Color.rgb(0, 100, 60));
        description.setGravity(Gravity.CENTER);
        description.setPadding(10, 22, 10, 22);

        root.addView(description);

        nameInput = createInput("Full name");
        root.addView(nameInput);

        phoneInput = createInput("Phone number");
        phoneInput.setInputType(
                InputType.TYPE_CLASS_PHONE
        );
        root.addView(phoneInput);

        passwordInput = createInput("Password");
        passwordInput.setInputType(
                InputType.TYPE_CLASS_TEXT
                        | InputType.TYPE_TEXT_VARIATION_PASSWORD
        );
        root.addView(passwordInput);

        confirmPasswordInput = createInput(
                "Confirm password"
        );
        confirmPasswordInput.setInputType(
                InputType.TYPE_CLASS_TEXT
                        | InputType.TYPE_TEXT_VARIATION_PASSWORD
        );
        root.addView(confirmPasswordInput);

        provinceInput = createInput("Province");
        root.addView(provinceInput);

        townInput = createInput("Town / City");
        root.addView(townInput);

        Button createButton = new Button(this);
        createButton.setText(
                "✅ CREATE PASSENGER ACCOUNT"
        );
        createButton.setTextSize(17);
        createButton.setTextColor(Color.WHITE);
        createButton.setAllCaps(false);
        createButton.setBackgroundColor(
                Color.rgb(0, 150, 80)
        );

        LinearLayout.LayoutParams createParams =
                new LinearLayout.LayoutParams(
                        -1,
                        -2
                );

        createParams.topMargin = 20;

        root.addView(
                createButton,
                createParams
        );

        Button backButton = new Button(this);
        backButton.setText(
                "← BACK TO LOGIN"
        );
        backButton.setAllCaps(false);

        root.addView(backButton);

        createButton.setOnClickListener(
                v -> createPassengerAccount()
        );

        backButton.setOnClickListener(
                v -> finish()
        );

        setContentView(scrollView);
    }

    private EditText createInput(
            String hint
    ) {

        EditText input =
                new EditText(this);

        input.setHint(hint);
        input.setTextSize(17);
        input.setSingleLine(true);
        input.setTextColor(Color.BLACK);
        input.setHintTextColor(Color.GRAY);
        input.setPadding(
                16,
                16,
                16,
                16
        );

        LinearLayout.LayoutParams params =
                new LinearLayout.LayoutParams(
                        -1,
                        -2
                );

        params.bottomMargin = 10;

        input.setLayoutParams(params);

        return input;
    }

    private void createPassengerAccount() {

        String name =
                nameInput
                        .getText()
                        .toString()
                        .trim();

        String phone =
                normalizePhone(
                        phoneInput
                                .getText()
                                .toString()
                );

        String password =
                passwordInput
                        .getText()
                        .toString();

        String confirmPassword =
                confirmPasswordInput
                        .getText()
                        .toString();

        String province =
                provinceInput
                        .getText()
                        .toString()
                        .trim();

        String town =
                townInput
                        .getText()
                        .toString()
                        .trim();

        if (name.isEmpty()
                || phone.isEmpty()
                || password.isEmpty()
                || province.isEmpty()
                || town.isEmpty()) {

            Toast.makeText(
                    this,
                    "Please complete all fields.",
                    Toast.LENGTH_LONG
            ).show();

            return;
        }

        if (!password.equals(
                confirmPassword
        )) {

            Toast.makeText(
                    this,
                    "Passwords do not match.",
                    Toast.LENGTH_LONG
            ).show();

            return;
        }

        if (password.length() < 6) {

            Toast.makeText(
                    this,
                    "Password must be at least 6 characters.",
                    Toast.LENGTH_LONG
            ).show();

            return;
        }

        String email =
                phone + "@sakyna.app";

        Toast.makeText(
                this,
                "Creating passenger account...",
                Toast.LENGTH_SHORT
        ).show();

        auth.createUserWithEmailAndPassword(
                        email,
                        password
                )
                .addOnSuccessListener(
                        result -> saveProfile(
                                result.getUser(),
                                name,
                                phone,
                                province,
                                town
                        )
                )
                .addOnFailureListener(
                        error -> Toast.makeText(
                                this,
                                "Account creation failed: "
                                        + error.getMessage(),
                                Toast.LENGTH_LONG
                        ).show()
                );
    }

    private void saveProfile(
            FirebaseUser user,
            String name,
            String phone,
            String province,
            String town
    ) {

        if (user == null) {

            Toast.makeText(
                    this,
                    "Account was created but user session is unavailable.",
                    Toast.LENGTH_LONG
            ).show();

            return;
        }

        Map<String, Object> profile =
                new HashMap<>();

        profile.put(
                "role",
                "PASSENGER"
        );

        profile.put(
                "accountType",
                "PASSENGER"
        );

        profile.put(
                "name",
                name
        );

        profile.put(
                "passengerName",
                name
        );

        profile.put(
                "phone",
                phone
        );

        profile.put(
                "province",
                province
        );

        profile.put(
                "townCity",
                town
        );

        profile.put(
                "city",
                town
        );

        profile.put(
                "createdAt",
                FieldValue.serverTimestamp()
        );

        profile.put(
                "updatedAt",
                FieldValue.serverTimestamp()
        );

        db.collection("users")
                .document(user.getUid())
                .set(profile)
                .addOnSuccessListener(
                        unused -> {

                            Toast.makeText(
                                    this,
                                    "✅ Passenger account created.",
                                    Toast.LENGTH_LONG
                            ).show();

                            Intent intent =
                                    new Intent(
                                            this,
                                            PassengerActivity.class
                                    );

                            intent.addFlags(
                                    Intent.FLAG_ACTIVITY_NEW_TASK
                                            | Intent.FLAG_ACTIVITY_CLEAR_TASK
                            );

                            startActivity(intent);

                            finish();
                        }
                )
                .addOnFailureListener(
                        error -> Toast.makeText(
                                this,
                                "Profile save failed: "
                                        + error.getMessage(),
                                Toast.LENGTH_LONG
                        ).show()
                );
    }

    private String normalizePhone(
            String input
    ) {

        if (input == null) {
            return "";
        }

        String value =
                input.trim()
                        .replace(" ", "")
                        .replace("-", "")
                        .replace("(", "")
                        .replace(")", "");

        if (value.startsWith("+63")) {

            value =
                    "63"
                            + value.substring(3);

        } else if (value.startsWith("09")) {

            value =
                    "63"
                            + value.substring(1);

        } else if (value.startsWith("9")
                && value.length() == 10) {

            value =
                    "63"
                            + value;
        }

        if (!value.matches(
                "63[0-9]{10}"
        )) {

            return "";
        }

        return value;
    }
}

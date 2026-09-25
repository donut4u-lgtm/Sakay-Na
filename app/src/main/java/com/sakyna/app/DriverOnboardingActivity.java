package com.sakyna.app;

import android.app.Activity;
import android.content.Intent;
import android.graphics.Color;
import android.os.Bundle;
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
import com.google.firebase.auth.FirebaseUser;
import com.google.firebase.firestore.DocumentSnapshot;
import com.google.firebase.firestore.FieldValue;
import com.google.firebase.firestore.FirebaseFirestore;
import com.google.firebase.firestore.Source;

import java.util.HashMap;
import java.util.Map;

public class DriverOnboardingActivity extends Activity {

    private FirebaseAuth auth;
    private FirebaseFirestore db;

    private EditText nameInput;
    private EditText phoneInput;
    private EditText passwordInput;
    private EditText confirmPasswordInput;
    private EditText provinceInput;
    private EditText townCityInput;
    private EditText plateInput;
    private EditText franchiseInput;
    private EditText vehicleInput;

    private TextView approvalStatus;
    private Button saveButton;

    private boolean registrationMode = false;
    private boolean registrationInProgress = false;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);

        auth = FirebaseAuth.getInstance();
        db = FirebaseFirestore.getInstance();

        registrationMode = getIntent().getBooleanExtra("registration", false);

        buildUi();

        if (!registrationMode) {
            loadDriverProfile();
        }
    }

    private void buildUi() {

        ScrollView scrollView = new ScrollView(this);

        LinearLayout root = new LinearLayout(this);
        root.setOrientation(LinearLayout.VERTICAL);
        root.setPadding(35, 35, 35, 35);
        root.setBackgroundColor(Color.rgb(245, 248, 255));

        scrollView.addView(root);

        TextView title = new TextView(this);
        title.setText(
                registrationMode
                        ? "🛺 SAKAY NA\nDRIVER ACCOUNT"
                        : "🛺 Sakay Na Driver Profile"
        );
        title.setTextSize(26);
        title.setTextColor(Color.rgb(20, 70, 150));
        title.setGravity(Gravity.CENTER);
        title.setPadding(10, 10, 10, 20);
        root.addView(title);

        TextView subtitle = new TextView(this);
        subtitle.setText(
                registrationMode
                        ? "Create your driver account using your phone number and password."
                        : "Manage your driver information and approval status."
        );
        subtitle.setTextSize(16);
        subtitle.setTextColor(Color.DKGRAY);
        subtitle.setGravity(Gravity.CENTER);
        subtitle.setPadding(10, 0, 10, 20);
        root.addView(subtitle);

        approvalStatus = new TextView(this);
        approvalStatus.setText("Approval status: Checking...");
        approvalStatus.setTextSize(16);
        approvalStatus.setTextColor(Color.rgb(180, 100, 0));
        approvalStatus.setGravity(Gravity.CENTER);
        approvalStatus.setPadding(10, 15, 10, 20);
        root.addView(approvalStatus);

        nameInput = createInput("Driver Full Name");
        root.addView(nameInput);

        phoneInput = createInput("Phone Number");
        phoneInput.setInputType(InputType.TYPE_CLASS_PHONE);
        root.addView(phoneInput);

        passwordInput = createInput("Password");
        passwordInput.setInputType(
                InputType.TYPE_CLASS_TEXT |
                InputType.TYPE_TEXT_VARIATION_PASSWORD
        );
        root.addView(passwordInput);

        confirmPasswordInput = createInput("Confirm Password");
        confirmPasswordInput.setInputType(
                InputType.TYPE_CLASS_TEXT |
                InputType.TYPE_TEXT_VARIATION_PASSWORD
        );
        root.addView(confirmPasswordInput);

        if (!registrationMode) {
            passwordInput.setVisibility(View.GONE);
            confirmPasswordInput.setVisibility(View.GONE);
        }

        TextView locationTitle = new TextView(this);
        locationTitle.setText("📍 LOCATION");
        locationTitle.setTextSize(19);
        locationTitle.setTextColor(Color.rgb(20, 90, 160));
        locationTitle.setPadding(5, 25, 5, 10);
        root.addView(locationTitle);

        provinceInput = createInput("Province");
        root.addView(provinceInput);

        townCityInput = createInput("Town / City");
        root.addView(townCityInput);

        TextView vehicleTitle = new TextView(this);
        vehicleTitle.setText("🛺 VEHICLE INFORMATION");
        vehicleTitle.setTextSize(19);
        vehicleTitle.setTextColor(Color.rgb(20, 90, 160));
        vehicleTitle.setPadding(5, 25, 5, 10);
        root.addView(vehicleTitle);

        plateInput = createInput("Plate Number");
        root.addView(plateInput);

        franchiseInput = createInput("Franchise Number");
        root.addView(franchiseInput);

        vehicleInput = createInput("Vehicle Description");
        root.addView(vehicleInput);

        TextView note = new TextView(this);
        note.setText(
                "At least one of Plate Number or Franchise Number should be provided."
        );
        note.setTextSize(14);
        note.setTextColor(Color.DKGRAY);
        note.setPadding(5, 10, 5, 20);
        root.addView(note);

        saveButton = new Button(this);
        saveButton.setText(
                registrationMode
                        ? "✅ CREATE DRIVER ACCOUNT"
                        : "💾 SAVE DRIVER PROFILE"
        );
        saveButton.setTextSize(17);
        saveButton.setTextColor(Color.WHITE);
        saveButton.setBackgroundColor(Color.rgb(0, 150, 90));
        saveButton.setAllCaps(false);

        LinearLayout.LayoutParams saveParams =
                new LinearLayout.LayoutParams(
                        LinearLayout.LayoutParams.MATCH_PARENT,
                        LinearLayout.LayoutParams.WRAP_CONTENT
                );
        saveParams.setMargins(0, 15, 0, 15);

        root.addView(saveButton, saveParams);

        saveButton.setOnClickListener(v -> saveDriverProfile());

        if (!registrationMode) {

            Button dashboardButton = new Button(this);
            dashboardButton.setText("🛺 OPEN DRIVER DASHBOARD");
            dashboardButton.setTextSize(16);
            dashboardButton.setTextColor(Color.WHITE);
            dashboardButton.setBackgroundColor(Color.rgb(20, 110, 190));
            dashboardButton.setAllCaps(false);

            root.addView(dashboardButton);

            dashboardButton.setOnClickListener(v -> openDriverDashboard());
        }

        Button backButton = new Button(this);

        if (registrationMode) {
            backButton.setText("← BACK TO LOGIN");
        } else {
            backButton.setText("🚪 LOGOUT / BACK");
        }

        backButton.setTextSize(16);
        backButton.setAllCaps(false);
        backButton.setBackgroundColor(Color.rgb(220, 70, 70));
        backButton.setTextColor(Color.WHITE);

        root.addView(backButton);

        backButton.setOnClickListener(v -> {

            if (!registrationMode) {
                auth.signOut();
            }

            Intent intent = new Intent(
                    DriverOnboardingActivity.this,
                    MainActivity.class
            );

            intent.addFlags(
                    Intent.FLAG_ACTIVITY_NEW_TASK |
                    Intent.FLAG_ACTIVITY_CLEAR_TASK
            );

            startActivity(intent);
        });

        setContentView(scrollView);
    }

    private EditText createInput(String hint) {

        EditText input = new EditText(this);

        input.setHint(hint);
        input.setTextSize(17);
        input.setSingleLine(true);
        input.setPadding(25, 18, 25, 18);
        input.setBackgroundColor(Color.WHITE);

        LinearLayout.LayoutParams params =
                new LinearLayout.LayoutParams(
                        LinearLayout.LayoutParams.MATCH_PARENT,
                        LinearLayout.LayoutParams.WRAP_CONTENT
                );

        params.setMargins(0, 7, 0, 7);

        input.setLayoutParams(params);

        return input;
    }

    private void loadDriverProfile() {

        FirebaseUser user = auth.getCurrentUser();

        if (user == null) {
            approvalStatus.setText("Not logged in.");
            return;
        }

        db.collection("users")
                .document(user.getUid())
                .get()
                .addOnSuccessListener(this::setProfileFields)
                .addOnFailureListener(e ->
                        Toast.makeText(
                                this,
                                "Unable to load driver profile.",
                                Toast.LENGTH_LONG
                        ).show()
                );
    }

    private void setProfileFields(DocumentSnapshot doc) {

        if (!doc.exists()) {
            approvalStatus.setText("Driver profile not found.");
            return;
        }

        nameInput.setText(doc.getString("name"));

        String phone = doc.getString("phone");
        if (phone != null) {
            phoneInput.setText(phone);
        }

        provinceInput.setText(doc.getString("province"));
        townCityInput.setText(doc.getString("townCity"));
        plateInput.setText(doc.getString("plateNumber"));
        franchiseInput.setText(doc.getString("franchiseNumber"));
        vehicleInput.setText(doc.getString("vehicleDescription"));

        updateApprovalStatus(doc);
    }

    private void updateApprovalStatus(DocumentSnapshot doc) {

        Boolean approved = doc.getBoolean("approved");

        String driverStatus = doc.getString("driverStatus");

        if (Boolean.TRUE.equals(approved)) {

            approvalStatus.setText("✅ APPROVED DRIVER — You can accept rides.");
            approvalStatus.setTextColor(Color.rgb(0, 130, 70));

        } else if ("REJECTED".equalsIgnoreCase(driverStatus)) {

            approvalStatus.setText("❌ DRIVER APPLICATION REJECTED");
            approvalStatus.setTextColor(Color.RED);

        } else {

            approvalStatus.setText(
                    "⏳ PENDING ADMIN APPROVAL"
            );
            approvalStatus.setTextColor(Color.rgb(190, 110, 0));
        }
    }

    private boolean isProfileComplete() {

        String name = nameInput.getText().toString().trim();
        String phone = phoneInput.getText().toString().trim();
        String province = provinceInput.getText().toString().trim();
        String town = townCityInput.getText().toString().trim();
        String plate = plateInput.getText().toString().trim();
        String franchise = franchiseInput.getText().toString().trim();

        return !name.isEmpty()
                && !phone.isEmpty()
                && !province.isEmpty()
                && !town.isEmpty()
                && (!plate.isEmpty() || !franchise.isEmpty());
    }

    private void saveDriverProfile() {

        if (registrationMode && registrationInProgress) {

            Toast.makeText(
                    this,
                    "Registration is already in progress. Please wait.",
                    Toast.LENGTH_SHORT
            ).show();

            return;
        }

        String name = nameInput.getText().toString().trim();
        String phone = normalizePhone(
                phoneInput.getText().toString().trim()
        );

        String province = provinceInput.getText().toString().trim();
        String town = townCityInput.getText().toString().trim();
        String plate = plateInput.getText().toString().trim();
        String franchise = franchiseInput.getText().toString().trim();
        String vehicle = vehicleInput.getText().toString().trim();

        if (name.isEmpty()) {
            phoneInput.requestFocus();
            Toast.makeText(
                    this,
                    "Please enter driver name.",
                    Toast.LENGTH_SHORT
            ).show();
            return;
        }

        if (phone.isEmpty()) {
            phoneInput.requestFocus();
            Toast.makeText(
                    this,
                    "Please enter a valid Philippine phone number.",
                    Toast.LENGTH_SHORT
            ).show();
            return;
        }

        if (province.isEmpty()) {

            Toast.makeText(
                    this,
                    "Please enter province.",
                    Toast.LENGTH_SHORT
            ).show();

            return;
        }

        if (town.isEmpty()) {

            Toast.makeText(
                    this,
                    "Please enter town/city.",
                    Toast.LENGTH_SHORT
            ).show();

            return;
        }

        if (plate.isEmpty() && franchise.isEmpty()) {

            Toast.makeText(
                    this,
                    "Enter Plate Number or Franchise Number.",
                    Toast.LENGTH_SHORT
            ).show();

            return;
        }

        if (registrationMode) {

            String password =
                    passwordInput.getText().toString();

            String confirmPassword =
                    confirmPasswordInput.getText().toString();

            if (password.isEmpty() || confirmPassword.isEmpty()) {

                Toast.makeText(
                        this,
                        "Please enter password and confirmation.",
                        Toast.LENGTH_SHORT
                ).show();

                return;
            }

            if (!password.equals(confirmPassword)) {

                Toast.makeText(
                        this,
                        "Passwords do not match.",
                        Toast.LENGTH_SHORT
                ).show();

                return;
            }

            if (password.length() < 6) {

                Toast.makeText(
                        this,
                        "Password must be at least 6 characters.",
                        Toast.LENGTH_SHORT
                ).show();

                return;
            }

            createDriverAccount(
                    name,
                    phone,
                    password,
                    province,
                    town,
                    plate,
                    franchise,
                    vehicle
            );

        } else {

            FirebaseUser user = auth.getCurrentUser();

            if (user == null) {

                Toast.makeText(
                        this,
                        "Please login again.",
                        Toast.LENGTH_SHORT
                ).show();

                return;
            }

            updateDriverProfile(
                    user.getUid(),
                    name,
                    phone,
                    province,
                    town,
                    plate,
                    franchise,
                    vehicle
            );
        }
    }

    private void createDriverAccount(
            String name,
            String phone,
            String password,
            String province,
            String town,
            String plate,
            String franchise,
            String vehicle
    ) {

        if (registrationInProgress) {
            return;
        }

        registrationInProgress = true;

        saveButton.setEnabled(false);
        saveButton.setText("⏳ CHECKING PHONE...");

        db.collection("users")
                .whereEqualTo("phone", phone)
                .limit(1)
                .get(Source.SERVER)
                .addOnSuccessListener(snapshot -> {

                    if (!snapshot.isEmpty()) {

                        resetDriverRegistrationButton();

                        Toast.makeText(
                                this,
                                "This phone number already has a Sakay Na account.",
                                Toast.LENGTH_LONG
                        ).show();

                        return;
                    }

                    createFirebaseDriverAccount(
                            name,
                            phone,
                            password,
                            province,
                            town,
                            plate,
                            franchise,
                            vehicle
                    );
                })
                .addOnFailureListener(e -> {

                    resetDriverRegistrationButton();

                    Toast.makeText(
                            this,
                            "Unable to check phone number. Please try again.",
                            Toast.LENGTH_LONG
                    ).show();
                });
    }

    private void createFirebaseDriverAccount(
            String name,
            String phone,
            String password,
            String province,
            String town,
            String plate,
            String franchise,
            String vehicle
    ) {

        String email = phone + "@sakyna.app";

        saveButton.setText("⏳ CREATING DRIVER ACCOUNT...");

        auth.createUserWithEmailAndPassword(email, password)
                .addOnSuccessListener(result -> {

                    FirebaseUser user = auth.getCurrentUser();

                    if (user == null) {

                        resetDriverRegistrationButton();

                        Toast.makeText(
                                this,
                                "Account was created but user session was not found.",
                                Toast.LENGTH_LONG
                        ).show();

                        return;
                    }

                    saveDriverAccountProfile(
                            user,
                            name,
                            phone,
                            province,
                            town,
                            plate,
                            franchise,
                            vehicle
                    );
                })
                .addOnFailureListener(e -> {

                    resetDriverRegistrationButton();

                    Toast.makeText(
                            this,
                            "Driver account creation failed: "
                                    + e.getMessage(),
                            Toast.LENGTH_LONG
                    ).show();
                });
    }

    private void saveDriverAccountProfile(
            FirebaseUser user,
            String name,
            String phone,
            String province,
            String town,
            String plate,
            String franchise,
            String vehicle
    ) {

        Map<String, Object> profile = new HashMap<>();

        profile.put("role", "DRIVER");
        profile.put("accountType", "DRIVER");

        profile.put("name", name);
        profile.put("driverName", name);

        profile.put("phone", phone);

        profile.put("province", province);
        profile.put("townCity", town);
        profile.put("city", town);

        profile.put("plateNumber", plate);
        profile.put("franchiseNumber", franchise);
        profile.put("vehicleDescription", vehicle);

        profile.put("driverProfileComplete", true);

        profile.put("approved", false);
        profile.put("driverStatus", "PENDING");
        profile.put("canAcceptRides", false);

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
                .addOnSuccessListener(unused -> {

                    registrationInProgress = false;

                    Toast.makeText(
                            this,
                            "✅ Driver account created. Waiting for Admin approval.",
                            Toast.LENGTH_LONG
                    ).show();

                    Intent intent = new Intent(
                            DriverOnboardingActivity.this,
                            DriverActivity.class
                    );

                    intent.addFlags(
                            Intent.FLAG_ACTIVITY_NEW_TASK |
                            Intent.FLAG_ACTIVITY_CLEAR_TASK
                    );

                    startActivity(intent);
                    finish();
                })
                .addOnFailureListener(e -> {

                    resetDriverRegistrationButton();

                    Toast.makeText(
                            this,
                            "Unable to save driver profile: "
                                    + e.getMessage(),
                            Toast.LENGTH_LONG
                    ).show();
                });
    }

    private void updateDriverProfile(
            String uid,
            String name,
            String phone,
            String province,
            String town,
            String plate,
            String franchise,
            String vehicle
    ) {

        saveButton.setEnabled(false);
        saveButton.setText("⏳ SAVING...");

        Map<String, Object> updates = new HashMap<>();

        updates.put("name", name);
        updates.put("driverName", name);
        updates.put("phone", phone);

        updates.put("province", province);
        updates.put("townCity", town);
        updates.put("city", town);

        updates.put("plateNumber", plate);
        updates.put("franchiseNumber", franchise);
        updates.put("vehicleDescription", vehicle);

        updates.put(
                "driverProfileComplete",
                isProfileComplete()
        );

        updates.put(
                "updatedAt",
                FieldValue.serverTimestamp()
        );

        db.collection("users")
                .document(uid)
                .update(updates)
                .addOnSuccessListener(unused -> {

                    saveButton.setEnabled(true);
                    saveButton.setText("💾 SAVE DRIVER PROFILE");

                    Toast.makeText(
                            this,
                            "✅ Driver profile saved.",
                            Toast.LENGTH_SHORT
                    ).show();

                    loadDriverProfile();
                })
                .addOnFailureListener(e -> {

                    saveButton.setEnabled(true);
                    saveButton.setText("💾 SAVE DRIVER PROFILE");

                    Toast.makeText(
                            this,
                            "Unable to save profile: "
                                    + e.getMessage(),
                            Toast.LENGTH_LONG
                    ).show();
                });
    }

    private void resetDriverRegistrationButton() {

        registrationInProgress = false;

        if (saveButton != null) {
            saveButton.setEnabled(true);
            saveButton.setText("✅ CREATE DRIVER ACCOUNT");
        }
    }

    private void openDriverDashboard() {

        Intent intent = new Intent(
                DriverOnboardingActivity.this,
                DriverActivity.class
        );

        startActivity(intent);
    }

    private String normalizePhone(String phone) {

        phone = phone
                .replace(" ", "")
                .replace("-", "")
                .replace("(", "")
                .replace(")", "");

        if (phone.startsWith("+63")) {

            phone = "63" + phone.substring(3);

        } else if (phone.startsWith("09")) {

            phone = "63" + phone.substring(1);

        } else if (
                phone.matches("9[0-9]{9}")
        ) {

            phone = "63" + phone;
        }

        if (!phone.matches("63[0-9]{10}")) {
            return "";
        }

        return phone;
    }
}

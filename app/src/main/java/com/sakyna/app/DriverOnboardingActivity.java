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

    private boolean registrationMode;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);

        auth = FirebaseAuth.getInstance();
        db = FirebaseFirestore.getInstance();

        registrationMode =
                getIntent().getBooleanExtra(
                        "registration",
                        false
                );

        buildScreen();

        if (registrationMode) {

            approvalStatus.setText(
                    "📝 CREATE DRIVER ACCOUNT"
            );

            approvalStatus.setTextColor(
                    Color.rgb(0, 110, 180)
            );

        } else {

            loadDriverProfile();
        }
    }

    private void buildScreen() {

        ScrollView scrollView = new ScrollView(this);

        LinearLayout root = new LinearLayout(this);
        root.setOrientation(LinearLayout.VERTICAL);
        root.setPadding(30, 30, 30, 30);
        root.setBackgroundColor(
                Color.rgb(245, 250, 247)
        );

        scrollView.addView(root);

        TextView title = new TextView(this);

        title.setText(
                registrationMode
                        ? "🛺 SAKAY NA\nDRIVER ACCOUNT"
                        : "🛺 Sakay Na Driver Profile"
        );

        title.setTextSize(27);
        title.setTextColor(Color.WHITE);
        title.setGravity(Gravity.CENTER);
        title.setPadding(12, 25, 12, 25);

        title.setBackgroundColor(
                Color.rgb(0, 115, 75)
        );

        root.addView(title);

        TextView subtitle = new TextView(this);

        subtitle.setText(
                registrationMode
                        ? "Create your driver account using your phone number and password."
                        : "Complete your driver information before accepting rides."
        );

        subtitle.setTextSize(16);
        subtitle.setTextColor(
                Color.rgb(0, 90, 60)
        );
        subtitle.setGravity(Gravity.CENTER);
        subtitle.setPadding(10, 22, 10, 22);

        root.addView(subtitle);

        approvalStatus = new TextView(this);
        approvalStatus.setTextSize(18);
        approvalStatus.setGravity(Gravity.CENTER);
        approvalStatus.setPadding(
                0,
                0,
                0,
                20
        );

        root.addView(approvalStatus);

        nameInput =
                createInput("Driver full name");

        root.addView(nameInput);

        phoneInput =
                createInput("Phone number");

        phoneInput.setInputType(
                InputType.TYPE_CLASS_PHONE
        );

        root.addView(phoneInput);

        /*
         * DRIVER PASSWORD
         */
        passwordInput =
                createInput("Password");

        passwordInput.setInputType(
                InputType.TYPE_CLASS_TEXT
                        | InputType.TYPE_TEXT_VARIATION_PASSWORD
        );

        root.addView(passwordInput);

        /*
         * CONFIRM DRIVER PASSWORD
         */
        confirmPasswordInput =
                createInput("Confirm password");

        confirmPasswordInput.setInputType(
                InputType.TYPE_CLASS_TEXT
                        | InputType.TYPE_TEXT_VARIATION_PASSWORD
        );

        root.addView(confirmPasswordInput);

        /*
         * Password fields are only needed when
         * creating the account.
         */
        if (!registrationMode) {

            passwordInput.setVisibility(
                    View.GONE
            );

            confirmPasswordInput.setVisibility(
                    View.GONE
            );
        }

        TextView locationTitle =
                new TextView(this);

        locationTitle.setText(
                "📍 Driver Location"
        );

        locationTitle.setTextSize(20);

        locationTitle.setTypeface(
                null,
                android.graphics.Typeface.BOLD
        );

        locationTitle.setTextColor(
                Color.rgb(0, 100, 180)
        );

        locationTitle.setPadding(
                0,
                16,
                0,
                10
        );

        root.addView(locationTitle);

        provinceInput =
                createInput("Province");

        root.addView(provinceInput);

        townCityInput =
                createInput("Town / City");

        root.addView(townCityInput);

        TextView locationNote =
                new TextView(this);

        locationNote.setText(
                "Enter the province and town/city where you operate."
        );

        locationNote.setTextSize(14);
        locationNote.setTextColor(Color.DKGRAY);
        locationNote.setPadding(
                0,
                0,
                0,
                16
        );

        root.addView(locationNote);

        TextView vehicleTitle =
                new TextView(this);

        vehicleTitle.setText(
                "🛺 Vehicle Information"
        );

        vehicleTitle.setTextSize(20);

        vehicleTitle.setTypeface(
                null,
                android.graphics.Typeface.BOLD
        );

        vehicleTitle.setTextColor(
                Color.rgb(0, 130, 80)
        );

        vehicleTitle.setPadding(
                0,
                8,
                0,
                10
        );

        root.addView(vehicleTitle);

        plateInput =
                createInput(
                        "Tricycle plate number (optional if franchise is provided)"
                );

        root.addView(plateInput);

        franchiseInput =
                createInput(
                        "Franchise number (optional if plate is provided)"
                );

        root.addView(franchiseInput);

        vehicleInput =
                createInput(
                        "Vehicle description"
                );

        root.addView(vehicleInput);

        TextView vehicleNote =
                new TextView(this);

        vehicleNote.setText(
                "ℹ️ Provide at least ONE: Plate Number or Franchise Number."
        );

        vehicleNote.setTextSize(14);
        vehicleNote.setTextColor(Color.DKGRAY);
        vehicleNote.setPadding(
                0,
                0,
                0,
                12
        );

        root.addView(vehicleNote);

        saveButton = new Button(this);

        saveButton.setText(
                registrationMode
                        ? "✅ CREATE DRIVER ACCOUNT"
                        : "💾 SAVE DRIVER PROFILE"
        );

        saveButton.setTextSize(17);
        saveButton.setTextColor(Color.WHITE);
        saveButton.setAllCaps(false);

        saveButton.setBackgroundColor(
                Color.rgb(0, 145, 80)
        );

        LinearLayout.LayoutParams saveParams =
                new LinearLayout.LayoutParams(
                        LinearLayout.LayoutParams.MATCH_PARENT,
                        LinearLayout.LayoutParams.WRAP_CONTENT
                );

        saveParams.topMargin = 20;

        root.addView(
                saveButton,
                saveParams
        );

        saveButton.setOnClickListener(
                view -> saveDriverProfile()
        );

        if (!registrationMode) {

            Button driverDashboardButton =
                    new Button(this);

            driverDashboardButton.setText(
                    "🚕 OPEN DRIVER DASHBOARD"
            );

            LinearLayout.LayoutParams dashboardParams =
                    new LinearLayout.LayoutParams(
                            LinearLayout.LayoutParams.MATCH_PARENT,
                            LinearLayout.LayoutParams.WRAP_CONTENT
                    );

            dashboardParams.topMargin = 16;

            root.addView(
                    driverDashboardButton,
                    dashboardParams
            );

            driverDashboardButton.setOnClickListener(
                    view -> {

                        if (isProfileComplete()) {

                            openDriverDashboard();

                        } else {

                            Toast.makeText(
                                    DriverOnboardingActivity.this,
                                    "Complete your driver profile first.",
                                    Toast.LENGTH_LONG
                            ).show();
                        }
                    }
            );
        }

        Button backButton =
                new Button(this);

        backButton.setText(
                registrationMode
                        ? "← BACK TO LOGIN"
                        : "LOGOUT"
        );

        backButton.setAllCaps(false);

        LinearLayout.LayoutParams backParams =
                new LinearLayout.LayoutParams(
                        LinearLayout.LayoutParams.MATCH_PARENT,
                        LinearLayout.LayoutParams.WRAP_CONTENT
                );

        backParams.topMargin = 12;

        root.addView(
                backButton,
                backParams
        );

        backButton.setOnClickListener(
                view -> {

                    if (registrationMode) {

                        finish();

                    } else {

                        auth.signOut();
                        finish();
                    }
                }
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
        input.setTextColor(Color.BLACK);
        input.setHintTextColor(Color.GRAY);
        input.setSingleLine(true);

        input.setPadding(
                16,
                16,
                16,
                16
        );

        LinearLayout.LayoutParams params =
                new LinearLayout.LayoutParams(
                        LinearLayout.LayoutParams.MATCH_PARENT,
                        LinearLayout.LayoutParams.WRAP_CONTENT
                );

        params.bottomMargin = 10;

        input.setLayoutParams(params);

        return input;
    }

    private void loadDriverProfile() {

        FirebaseUser user =
                auth.getCurrentUser();

        if (user == null) {

            approvalStatus.setText(
                    "Please login again."
            );

            return;
        }

        db.collection("users")
                .document(user.getUid())
                .get()
                .addOnSuccessListener(
                        snapshot -> {

                            if (!snapshot.exists()) {

                                approvalStatus.setText(
                                        "Profile not found."
                                );

                                return;
                            }

                            setProfileFields(
                                    snapshot
                            );

                            updateApprovalStatus(
                                    snapshot
                            );
                        }
                )
                .addOnFailureListener(
                        e ->
                                approvalStatus.setText(
                                        "Unable to load profile."
                                )
                );
    }

    private void setProfileFields(
            DocumentSnapshot snapshot
    ) {

        String name =
                snapshot.getString(
                        "driverName"
                );

        String phone =
                snapshot.getString(
                        "phone"
                );

        String province =
                snapshot.getString(
                        "province"
                );

        String townCity =
                snapshot.getString(
                        "townCity"
                );

        if (townCity == null) {

            townCity =
                    snapshot.getString(
                            "city"
                    );
        }

        String plate =
                snapshot.getString(
                        "plateNumber"
                );

        String franchise =
                snapshot.getString(
                        "franchiseNumber"
                );

        String vehicle =
                snapshot.getString(
                        "vehicleDescription"
                );

        if (name != null) {
            nameInput.setText(name);
        }

        if (phone != null) {
            phoneInput.setText(phone);
        }

        if (province != null) {
            provinceInput.setText(province);
        }

        if (townCity != null) {
            townCityInput.setText(townCity);
        }

        if (plate != null) {
            plateInput.setText(plate);
        }

        if (franchise != null) {
            franchiseInput.setText(franchise);
        }

        if (vehicle != null) {
            vehicleInput.setText(vehicle);
        }
    }

    private void updateApprovalStatus(
            DocumentSnapshot snapshot
    ) {

        Boolean approved =
                snapshot.getBoolean(
                        "approved"
                );

        String driverStatus =
                snapshot.getString(
                        "driverStatus"
                );

        Boolean canAcceptRides =
                snapshot.getBoolean(
                        "canAcceptRides"
                );

        if (Boolean.TRUE.equals(approved)
                && "APPROVED".equals(
                        driverStatus
                )
                && Boolean.TRUE.equals(
                        canAcceptRides
                )) {

            approvalStatus.setText(
                    "✅ DRIVER APPROVED\n\n"
                            + "You can accept rides."
            );

            approvalStatus.setTextColor(
                    Color.rgb(0, 130, 0)
            );

        } else {

            approvalStatus.setText(
                    "⏳ DRIVER APPROVAL PENDING\n\n"
                            + "You cannot accept rides yet."
            );

            approvalStatus.setTextColor(
                    Color.rgb(180, 90, 0)
            );
        }
    }

    private boolean isProfileComplete() {

        String name =
                nameInput.getText()
                        .toString()
                        .trim();

        String phone =
                phoneInput.getText()
                        .toString()
                        .trim();

        String province =
                provinceInput.getText()
                        .toString()
                        .trim();

        String townCity =
                townCityInput.getText()
                        .toString()
                        .trim();

        String plate =
                plateInput.getText()
                        .toString()
                        .trim();

        String franchise =
                franchiseInput.getText()
                        .toString()
                        .trim();

        String vehicle =
                vehicleInput.getText()
                        .toString()
                        .trim();

        return !name.isEmpty()
                && !phone.isEmpty()
                && !province.isEmpty()
                && !townCity.isEmpty()
                && (!plate.isEmpty()
                    || !franchise.isEmpty())
                && !vehicle.isEmpty();
    }

    private void saveDriverProfile() {

        String name =
                nameInput.getText()
                        .toString()
                        .trim();

        String phone =
                normalizePhone(
                        phoneInput.getText()
                                .toString()
                );

        String password =
                passwordInput.getText()
                        .toString();

        String confirmPassword =
                confirmPasswordInput.getText()
                        .toString();

        String province =
                provinceInput.getText()
                        .toString()
                        .trim();

        String townCity =
                townCityInput.getText()
                        .toString()
                        .trim();

        String plate =
                plateInput.getText()
                        .toString()
                        .trim();

        String franchise =
                franchiseInput.getText()
                        .toString()
                        .trim();

        String vehicle =
                vehicleInput.getText()
                        .toString()
                        .trim();

        if (name.isEmpty()
                || phone.isEmpty()
                || province.isEmpty()
                || townCity.isEmpty()
                || vehicle.isEmpty()) {

            Toast.makeText(
                    this,
                    "Please complete all required fields.",
                    Toast.LENGTH_LONG
            ).show();

            return;
        }

        if (plate.isEmpty()
                && franchise.isEmpty()) {

            Toast.makeText(
                    this,
                    "Enter at least Plate Number or Franchise Number.",
                    Toast.LENGTH_LONG
            ).show();

            return;
        }

        /*
         * PASSWORD VALIDATION
         * Only required during account creation.
         */
        if (registrationMode) {

            if (password.isEmpty()
                    || confirmPassword.isEmpty()) {

                Toast.makeText(
                        this,
                        "Please enter and confirm your password.",
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

            createDriverAccount(
                    name,
                    phone,
                    password,
                    province,
                    townCity,
                    plate,
                    franchise,
                    vehicle
            );

            return;
        }

        FirebaseUser user =
                auth.getCurrentUser();

        if (user == null) {

            Toast.makeText(
                    this,
                    "Please login again.",
                    Toast.LENGTH_LONG
            ).show();

            return;
        }

        updateDriverProfile(
                user,
                name,
                phone,
                province,
                townCity,
                plate,
                franchise,
                vehicle
        );
    }

    private void createDriverAccount(
            String name,
            String phone,
            String password,
            String province,
            String townCity,
            String plate,
            String franchise,
            String vehicle
    ) {

        String email =
                phone + "@sakyna.app";

        saveButton.setEnabled(false);

        saveButton.setText(
                "CREATING DRIVER ACCOUNT..."
        );

        auth.createUserWithEmailAndPassword(
                        email,
                        password
                )
                .addOnSuccessListener(
                        result -> {

                            FirebaseUser user =
                                    result.getUser();

                            if (user == null) {

                                saveButton.setEnabled(
                                        true
                                );

                                saveButton.setText(
                                        "✅ CREATE DRIVER ACCOUNT"
                                );

                                Toast.makeText(
                                        this,
                                        "Account created but session is unavailable.",
                                        Toast.LENGTH_LONG
                                ).show();

                                return;
                            }

                            saveDriverAccountProfile(
                                    user,
                                    name,
                                    phone,
                                    province,
                                    townCity,
                                    plate,
                                    franchise,
                                    vehicle
                            );
                        }
                )
                .addOnFailureListener(
                        error -> {

                            saveButton.setEnabled(
                                    true
                            );

                            saveButton.setText(
                                    "✅ CREATE DRIVER ACCOUNT"
                            );

                            Toast.makeText(
                                    this,
                                    "Account creation failed: "
                                            + error.getMessage(),
                                    Toast.LENGTH_LONG
                            ).show();
                        }
                );
    }

    private void saveDriverAccountProfile(
            FirebaseUser user,
            String name,
            String phone,
            String province,
            String townCity,
            String plate,
            String franchise,
            String vehicle
    ) {

        Map<String, Object> profile =
                new HashMap<>();

        profile.put(
                "role",
                "DRIVER"
        );

        profile.put(
                "accountType",
                "DRIVER"
        );

        profile.put(
                "name",
                name
        );

        profile.put(
                "driverName",
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
                townCity
        );

        profile.put(
                "city",
                townCity
        );

        profile.put(
                "plateNumber",
                plate
        );

        profile.put(
                "franchiseNumber",
                franchise
        );

        profile.put(
                "vehicleDescription",
                vehicle
        );

        profile.put(
                "driverProfileComplete",
                true
        );

        profile.put(
                "approved",
                false
        );

        profile.put(
                "driverStatus",
                "PENDING"
        );

        profile.put(
                "canAcceptRides",
                false
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
                                    "✅ Driver account created. Waiting for Admin approval.",
                                    Toast.LENGTH_LONG
                            ).show();

                            Intent intent =
                                    new Intent(
                                            this,
                                            DriverActivity.class
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
                        error -> {

                            saveButton.setEnabled(
                                    true
                            );

                            saveButton.setText(
                                    "✅ CREATE DRIVER ACCOUNT"
                            );

                            Toast.makeText(
                                    this,
                                    "Profile save failed: "
                                            + error.getMessage(),
                                    Toast.LENGTH_LONG
                            ).show();
                        }
                );
    }

    private void updateDriverProfile(
            FirebaseUser user,
            String name,
            String phone,
            String province,
            String townCity,
            String plate,
            String franchise,
            String vehicle
    ) {

        Map<String, Object> profile =
                new HashMap<>();

        profile.put(
                "driverName",
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
                townCity
        );

        profile.put(
                "city",
                townCity
        );

        profile.put(
                "plateNumber",
                plate
        );

        profile.put(
                "franchiseNumber",
                franchise
        );

        profile.put(
                "vehicleDescription",
                vehicle
        );

        profile.put(
                "driverProfileComplete",
                true
        );

        profile.put(
                "profileUpdatedAt",
                FieldValue.serverTimestamp()
        );

        db.collection("users")
                .document(user.getUid())
                .update(profile)
                .addOnSuccessListener(
                        unused -> {

                            Toast.makeText(
                                    this,
                                    "✅ Driver profile saved.",
                                    Toast.LENGTH_SHORT
                            ).show();

                            loadDriverProfile();
                        }
                )
                .addOnFailureListener(
                        error -> Toast.makeText(
                                this,
                                "Save failed: "
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

    private void openDriverDashboard() {

        Intent intent =
                new Intent(
                        this,
                        DriverActivity.class
                );

        startActivity(intent);
    }
}

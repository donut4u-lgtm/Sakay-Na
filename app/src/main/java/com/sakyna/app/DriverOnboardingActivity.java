
package com.sakyna.app;

import android.app.Activity;
import android.os.Bundle;
import android.graphics.Color;
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
private EditText plateInput;
private EditText franchiseInput;
private EditText vehicleInput;

private TextView approvalStatus;

@Override
protected void onCreate(Bundle savedInstanceState) {
    super.onCreate(savedInstanceState);

    auth = FirebaseAuth.getInstance();
    db = FirebaseFirestore.getInstance();

    buildScreen();
    loadDriverProfile();
}

private void buildScreen() {

    ScrollView scrollView = new ScrollView(this);

    LinearLayout root = new LinearLayout(this);
    root.setOrientation(LinearLayout.VERTICAL);
    root.setPadding(32, 32, 32, 32);
    root.setBackgroundColor(Color.WHITE);

    scrollView.addView(root);

    TextView title = new TextView(this);
    title.setText("🛺 Sakay Na Driver Profile");
    title.setTextSize(26);
    title.setTextColor(Color.BLACK);
    title.setGravity(Gravity.CENTER);
    title.setPadding(0, 0, 0, 24);

    root.addView(title);

    TextView subtitle = new TextView(this);
    subtitle.setText(
            "Complete your driver information before accepting rides."
    );
    subtitle.setTextSize(16);
    subtitle.setTextColor(Color.DKGRAY);
    subtitle.setGravity(Gravity.CENTER);
    subtitle.setPadding(0, 0, 0, 24);

    root.addView(subtitle);

    approvalStatus = new TextView(this);
    approvalStatus.setText("Approval status: CHECKING...");
    approvalStatus.setTextSize(18);
    approvalStatus.setTextColor(Color.BLACK);
    approvalStatus.setGravity(Gravity.CENTER);
    approvalStatus.setPadding(0, 0, 0, 20);

    root.addView(approvalStatus);

    nameInput = createInput("Driver full name");
    root.addView(nameInput);

    phoneInput = createInput("Phone number");
    root.addView(phoneInput);

    plateInput = createInput(
            "Tricycle plate number (optional if franchise is provided)"
    );
    root.addView(plateInput);

    franchiseInput = createInput(
            "Franchise number (optional if plate is provided)"
    );
    root.addView(franchiseInput);

    vehicleInput = createInput("Vehicle description");
    root.addView(vehicleInput);

    TextView vehicleNote = new TextView(this);
    vehicleNote.setText(
            "ℹ️ Provide at least ONE: Plate Number or Franchise Number."
    );
    vehicleNote.setTextSize(14);
    vehicleNote.setTextColor(Color.DKGRAY);
    vehicleNote.setPadding(0, 0, 0, 12);
    root.addView(vehicleNote);

    Button saveButton = new Button(this);
    saveButton.setText("💾 SAVE DRIVER PROFILE");

    LinearLayout.LayoutParams saveParams =
            new LinearLayout.LayoutParams(
                    LinearLayout.LayoutParams.MATCH_PARENT,
                    LinearLayout.LayoutParams.WRAP_CONTENT
            );

    saveParams.topMargin = 24;

    root.addView(
            saveButton,
            saveParams
    );

    saveButton.setOnClickListener(
            new View.OnClickListener() {
                @Override
                public void onClick(View view) {
                    saveDriverProfile();
                }
            }
    );

    Button driverDashboardButton = new Button(this);
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
            new View.OnClickListener() {
                @Override
                public void onClick(View view) {

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
            }
    );

    Button logoutButton = new Button(this);
    logoutButton.setText("LOGOUT");

    LinearLayout.LayoutParams logoutParams =
            new LinearLayout.LayoutParams(
                    LinearLayout.LayoutParams.MATCH_PARENT,
                    LinearLayout.LayoutParams.WRAP_CONTENT
            );

    logoutParams.topMargin = 16;

    root.addView(
            logoutButton,
            logoutParams
    );

    logoutButton.setOnClickListener(
            new View.OnClickListener() {
                @Override
                public void onClick(View view) {

                    auth.signOut();
                    finish();
                }
            }
    );

    setContentView(scrollView);
}

private EditText createInput(String hint) {

    EditText input = new EditText(this);

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

    params.bottomMargin = 12;

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

                        String name =
                                snapshot.getString(
                                        "driverName"
                                );

                        String phone =
                                snapshot.getString(
                                        "phone"
                                );

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

                        if (plate != null) {
                            plateInput.setText(plate);
                        }

                        if (franchise != null) {
                            franchiseInput.setText(franchise);
                        }

                        if (vehicle != null) {
                            vehicleInput.setText(vehicle);
                        }

                        updateApprovalStatus(snapshot);
                    }
            )
            .addOnFailureListener(
                    e -> approvalStatus.setText(
                            "Unable to load profile."
                    )
            );
}

private void updateApprovalStatus(
        DocumentSnapshot snapshot
) {

    Boolean approved =
            snapshot.getBoolean("approved");

    String driverStatus =
            snapshot.getString("driverStatus");

    Boolean canAcceptRides =
            snapshot.getBoolean(
                    "canAcceptRides"
            );

    if (Boolean.TRUE.equals(approved)
            && "APPROVED".equals(driverStatus)
            && Boolean.TRUE.equals(canAcceptRides)) {

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

    boolean hasPlate =
            !plate.isEmpty();

    boolean hasFranchise =
            !franchise.isEmpty();

    return !name.isEmpty()
            && !phone.isEmpty()
            && (hasPlate || hasFranchise)
            && !vehicle.isEmpty();
}

private void saveDriverProfile() {

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

    String name =
            nameInput.getText()
                    .toString()
                    .trim();

    String phone =
            phoneInput.getText()
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

    if (name.isEmpty()) {

        Toast.makeText(
                this,
                "Please enter driver full name.",
                Toast.LENGTH_LONG
        ).show();

        return;
    }

    if (phone.isEmpty()) {

        Toast.makeText(
                this,
                "Please enter phone number.",
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

    if (vehicle.isEmpty()) {

        Toast.makeText(
                this,
                "Please enter vehicle description.",
                Toast.LENGTH_LONG
        ).show();

        return;
    }

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
                    e -> Toast.makeText(
                            this,
                            "Save failed: "
                                    + e.getMessage(),
                            Toast.LENGTH_LONG
                    ).show()
            );
}

private void openDriverDashboard() {

    android.content.Intent intent =
            new android.content.Intent(
                    this,
                    DriverActivity.class
            );

    startActivity(intent);
}

}

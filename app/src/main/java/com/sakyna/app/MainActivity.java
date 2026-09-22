
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

import com.google.firebase.auth.AuthResult;
import com.google.firebase.auth.FirebaseAuth;
import com.google.firebase.auth.FirebaseUser;
import com.google.firebase.firestore.DocumentSnapshot;
import com.google.firebase.firestore.FieldValue;
import com.google.firebase.firestore.FirebaseFirestore;

import java.util.HashMap;
import java.util.Map;

public class MainActivity extends Activity {

private FirebaseAuth auth;
private FirebaseFirestore db;

private EditText nameInput;
private EditText townInput;
private EditText provinceInput;
private EditText phoneInput;
private EditText passwordInput;

private EditText plateInput;
private EditText franchiseInput;
private EditText vehicleInput;

private TextView driverInfoTitle;

private Button passengerButton;
private Button driverButton;
private Button adminButton;
private Button loginButton;
private Button createButton;

private String selectedRole = "PASSENGER";

private static final int GREEN =
        Color.rgb(0, 120, 70);

private static final int BLUE =
        Color.rgb(30, 90, 180);

private static final int ORANGE =
        Color.rgb(190, 95, 0);

private static final String ADMIN_UID =
        "Ld3rzaCvAGNlXBDCofB3mWjgXWp2";

@Override
protected void onCreate(Bundle savedInstanceState) {
    super.onCreate(savedInstanceState);

    auth = FirebaseAuth.getInstance();
    db = FirebaseFirestore.getInstance();

    showLoginScreen();
}

private void showLoginScreen() {

    ScrollView scrollView = new ScrollView(this);

    LinearLayout root = new LinearLayout(this);
    root.setOrientation(LinearLayout.VERTICAL);
    root.setGravity(Gravity.CENTER_HORIZONTAL);
    root.setPadding(28, 25, 28, 35);
    root.setBackgroundColor(Color.WHITE);

    scrollView.addView(root);

    TextView title = new TextView(this);
    title.setText("🛺 SAKAY NA");
    title.setTextSize(32);
    title.setTypeface(
            null,
            android.graphics.Typeface.BOLD
    );
    title.setTextColor(GREEN);
    title.setGravity(Gravity.CENTER);
    root.addView(title, full());

    TextView subtitle = new TextView(this);
    subtitle.setText("Ride anywhere. Sakay Na.");
    subtitle.setTextSize(17);
    subtitle.setTextColor(Color.DKGRAY);
    subtitle.setGravity(Gravity.CENTER);
    subtitle.setPadding(0, 5, 0, 20);
    root.addView(subtitle, full());

    TextView roleLabel = new TextView(this);
    roleLabel.setText("SELECT ACCOUNT TYPE");
    roleLabel.setTextSize(15);
    roleLabel.setTypeface(
            null,
            android.graphics.Typeface.BOLD
    );
    roleLabel.setTextColor(Color.DKGRAY);
    roleLabel.setGravity(Gravity.CENTER);
    root.addView(roleLabel, full());

    LinearLayout roleRow = new LinearLayout(this);
    roleRow.setOrientation(LinearLayout.HORIZONTAL);
    roleRow.setGravity(Gravity.CENTER);

    passengerButton =
            makeRoleButton("PASSENGER");

    driverButton =
            makeRoleButton("DRIVER");

    adminButton =
            makeRoleButton("ADMIN");

    roleRow.addView(
            passengerButton,
            weighted()
    );

    roleRow.addView(
            driverButton,
            weighted()
    );

    roleRow.addView(
            adminButton,
            weighted()
    );

    root.addView(roleRow, full());

    passengerButton.setOnClickListener(
            v -> selectRole("PASSENGER")
    );

    driverButton.setOnClickListener(
            v -> selectRole("DRIVER")
    );

    adminButton.setOnClickListener(
            v -> selectRole("ADMIN")
    );

    TextView nameLabel =
            label("Full name");

    nameLabel.setPadding(0, 18, 0, 3);
    root.addView(nameLabel, full());

    nameInput = new EditText(this);
    nameInput.setHint("Full name");
    nameInput.setTextSize(18);
    nameInput.setSingleLine(true);
    root.addView(nameInput, full());

    TextView townLabel =
            label("Town / City");

    townLabel.setPadding(0, 14, 0, 3);
    root.addView(townLabel, full());

    townInput = new EditText(this);
    townInput.setHint("Town / City");
    townInput.setTextSize(18);
    townInput.setSingleLine(true);
    root.addView(townInput, full());

    TextView provinceLabel =
            label("Province");

    provinceLabel.setPadding(0, 14, 0, 3);
    root.addView(provinceLabel, full());

    provinceInput = new EditText(this);
    provinceInput.setHint("Province");
    provinceInput.setTextSize(18);
    provinceInput.setSingleLine(true);
    root.addView(provinceInput, full());

    driverInfoTitle = new TextView(this);
    driverInfoTitle.setText(
            "🛺 DRIVER INFORMATION"
    );
    driverInfoTitle.setTextSize(20);
    driverInfoTitle.setTypeface(
            null,
            android.graphics.Typeface.BOLD
    );
    driverInfoTitle.setTextColor(BLUE);
    driverInfoTitle.setPadding(0, 22, 0, 8);
    root.addView(
            driverInfoTitle,
            full()
    );

    plateInput = new EditText(this);
    plateInput.setHint(
            "Tricycle Plate Number"
    );
    plateInput.setTextSize(18);
    plateInput.setSingleLine(true);
    root.addView(
            plateInput,
            full()
    );

    franchiseInput = new EditText(this);
    franchiseInput.setHint(
            "Franchise Number"
    );
    franchiseInput.setTextSize(18);
    franchiseInput.setSingleLine(true);
    root.addView(
            franchiseInput,
            full()
    );

    vehicleInput = new EditText(this);
    vehicleInput.setHint(
            "Description of Tricycle"
    );
    vehicleInput.setTextSize(18);
    vehicleInput.setSingleLine(true);
    root.addView(
            vehicleInput,
            full()
    );

    TextView driverNote = new TextView(this);
    driverNote.setText(
            "Driver: provide at least ONE of Plate Number or Franchise Number, plus a tricycle description."
    );
    driverNote.setTextSize(14);
    driverNote.setTextColor(Color.DKGRAY);
    driverNote.setPadding(0, 5, 0, 8);
    root.addView(
            driverNote,
            full()
    );

    TextView phoneLabel =
            label("Phone number");

    phoneLabel.setPadding(0, 18, 0, 3);
    root.addView(phoneLabel, full());

    phoneInput = new EditText(this);
    phoneInput.setHint("09XXXXXXXXX");
    phoneInput.setTextSize(18);
    phoneInput.setSingleLine(true);
    phoneInput.setInputType(
            InputType.TYPE_CLASS_PHONE
    );
    root.addView(phoneInput, full());

    TextView passwordLabel =
            label("Password");

    passwordLabel.setPadding(0, 14, 0, 3);
    root.addView(passwordLabel, full());

    passwordInput = new EditText(this);
    passwordInput.setHint("Password");
    passwordInput.setTextSize(18);
    passwordInput.setSingleLine(true);
    passwordInput.setInputType(
            InputType.TYPE_CLASS_TEXT
                    | InputType.TYPE_TEXT_VARIATION_PASSWORD
    );
    root.addView(passwordInput, full());

    loginButton = new Button(this);
    loginButton.setText("LOGIN");
    loginButton.setTextSize(17);
    loginButton.setTextColor(Color.WHITE);
    loginButton.setBackgroundColor(GREEN);
    loginButton.setOnClickListener(
            v -> login()
    );

    LinearLayout.LayoutParams loginParams =
            full();

    loginParams.topMargin = 18;

    root.addView(
            loginButton,
            loginParams
    );

    createButton = new Button(this);
    createButton.setText("CREATE ACCOUNT");
    createButton.setTextSize(17);
    createButton.setOnClickListener(
            v -> createAccount()
    );

    root.addView(
            createButton,
            full()
    );

    TextView information = new TextView(this);

    information.setText(
            "LOGIN: Phone number + password only\n"
                    + "PASSENGER CREATE: Name + Town/City + Province\n"
                    + "DRIVER CREATE: Name + Town/City + Province + Plate/Franchise + Tricycle Description\n"
                    + "Driver accounts require Admin approval before GO ONLINE."
    );

    information.setTextSize(14);
    information.setTextColor(Color.GRAY);
    information.setGravity(Gravity.CENTER);
    information.setPadding(0, 14, 0, 0);

    root.addView(
            information,
            full()
    );

    setContentView(scrollView);

    selectRole("PASSENGER");
}

private TextView label(String text) {

    TextView t = new TextView(this);

    t.setText(text);
    t.setTextSize(14);
    t.setTypeface(
            null,
            android.graphics.Typeface.BOLD
    );
    t.setTextColor(Color.DKGRAY);

    return t;
}

private Button makeRoleButton(String text) {

    Button button = new Button(this);

    button.setText(text);
    button.setTextSize(12);

    return button;
}

private LinearLayout.LayoutParams full() {

    return new LinearLayout.LayoutParams(
            LinearLayout.LayoutParams.MATCH_PARENT,
            LinearLayout.LayoutParams.WRAP_CONTENT
    );
}

private LinearLayout.LayoutParams weighted() {

    return new LinearLayout.LayoutParams(
            0,
            LinearLayout.LayoutParams.WRAP_CONTENT,
            1f
    );
}

private void selectRole(String role) {

    selectedRole =
            role.trim().toUpperCase();

    passengerButton.setTextColor(Color.DKGRAY);
    driverButton.setTextColor(Color.DKGRAY);
    adminButton.setTextColor(Color.DKGRAY);

    passengerButton.setBackgroundColor(
            Color.TRANSPARENT
    );

    driverButton.setBackgroundColor(
            Color.TRANSPARENT
    );

    adminButton.setBackgroundColor(
            Color.TRANSPARENT
    );

    if ("DRIVER".equals(selectedRole)) {

        driverButton.setTextColor(Color.WHITE);
        driverButton.setBackgroundColor(BLUE);

        driverInfoTitle.setVisibility(View.VISIBLE);
        plateInput.setVisibility(View.VISIBLE);
        franchiseInput.setVisibility(View.VISIBLE);
        vehicleInput.setVisibility(View.VISIBLE);

    } else if ("ADMIN".equals(selectedRole)) {

        adminButton.setTextColor(Color.WHITE);
        adminButton.setBackgroundColor(ORANGE);

        driverInfoTitle.setVisibility(View.GONE);
        plateInput.setVisibility(View.GONE);
        franchiseInput.setVisibility(View.GONE);
        vehicleInput.setVisibility(View.GONE);

    } else {

        selectedRole = "PASSENGER";

        passengerButton.setTextColor(Color.WHITE);
        passengerButton.setBackgroundColor(GREEN);

        driverInfoTitle.setVisibility(View.GONE);
        plateInput.setVisibility(View.GONE);
        franchiseInput.setVisibility(View.GONE);
        vehicleInput.setVisibility(View.GONE);
    }
}

private String normalizePhone(String input) {

    if (input == null) {
        return "";
    }

    String phone =
            input.replaceAll(
                    "[^0-9]",
                    ""
            );

    if (phone.startsWith("0")
            && phone.length() == 11) {

        phone =
                "63"
                        + phone.substring(1);

    } else if (
            phone.startsWith("9")
                    && phone.length() == 10) {

        phone =
                "63"
                        + phone;
    }

    return phone;
}

private String firebaseIdentifier(
        String phone) {

    return phone + "@sakyna.app";
}

private boolean validLoginInput() {

    String phone =
            normalizePhone(
                    phoneInput == null
                            ? ""
                            : phoneInput.getText()
                            .toString()
            );

    String password =
            passwordInput == null
                    ? ""
                    : passwordInput.getText()
                    .toString();

    if (phone.length() != 12
            || !phone.startsWith("63")) {

        toast(
                "Enter a valid Philippine phone number."
        );

        return false;
    }

    if (password.length() < 6) {

        toast(
                "Password must be at least 6 characters."
        );

        return false;
    }

    return true;
}

private boolean validCreateInput() {

    String name =
            nameInput == null
                    ? ""
                    : nameInput.getText()
                    .toString()
                    .trim();

    String town =
            townInput == null
                    ? ""
                    : townInput.getText()
                    .toString()
                    .trim();

    String province =
            provinceInput == null
                    ? ""
                    : provinceInput.getText()
                    .toString()
                    .trim();

    String phone =
            normalizePhone(
                    phoneInput == null
                            ? ""
                            : phoneInput.getText()
                            .toString()
            );

    String password =
            passwordInput == null
                    ? ""
                    : passwordInput.getText()
                    .toString();

    if (name.isEmpty()) {

        toast(
                "Enter your full name."
        );

        return false;
    }

    if (town.isEmpty()) {

        toast(
                "Enter your town or city."
        );

        return false;
    }

    if (province.isEmpty()) {

        toast(
                "Enter your province."
        );

        return false;
    }

    if (phone.length() != 12
            || !phone.startsWith("63")) {

        toast(
                "Enter a valid Philippine phone number."
        );

        return false;
    }

    if (password.length() < 6) {

        toast(
                "Password must be at least 6 characters."
        );

        return false;
    }

    if ("DRIVER".equals(selectedRole)) {

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

        if (plate.isEmpty()
                && franchise.isEmpty()) {

            toast(
                    "Driver: enter Plate Number or Franchise Number."
            );

            return false;
        }

        if (vehicle.isEmpty()) {

            toast(
                    "Driver: enter the description of your tricycle."
            );

            return false;
        }
    }

    return true;
}

private void login() {

    if (!validLoginInput()) {
        return;
    }

    final String phone =
            normalizePhone(
                    phoneInput
                            .getText()
                            .toString()
            );

    final String password =
            passwordInput
                    .getText()
                    .toString();

    loginButton.setEnabled(false);
    createButton.setEnabled(false);
    loginButton.setText("LOGGING IN...");

    auth.signInWithEmailAndPassword(
            firebaseIdentifier(phone),
            password
    )
    .addOnSuccessListener(
            result -> {

                AuthResult authResult = result;

                FirebaseUser user =
                        authResult.getUser();

                if (user == null) {

                    resetLoginButtons();

                    toast(
                            "Login failed."
                    );

                    return;
                }

                loadUserRole(user);
            }
    )
    .addOnFailureListener(
            e -> {

                resetLoginButtons();

                String message =
                        e.getMessage();

                if (message == null
                        || message.trim().isEmpty()) {

                    message =
                            "Invalid phone number or password.";
                }

                toast(message);
            }
    );
}

private void resetLoginButtons() {

    if (loginButton != null) {

        loginButton.setEnabled(true);
        loginButton.setText("LOGIN");
    }

    if (createButton != null) {

        createButton.setEnabled(true);
    }
}

private void loadUserRole(
        FirebaseUser user) {

    if (user == null) {

        showLoginError(
                "Account not found."
        );

        return;
    }

    final String uid =
            user.getUid();

    if (ADMIN_UID.equals(uid)) {

        openScreen(
                AdminActivity.class
        );

        return;
    }

    db.collection("users")
            .document(uid)
            .get()
            .addOnSuccessListener(
                    document -> {

                        if (!document.exists()) {

                            findProfileByPhone(
                                    user
                            );

                            return;
                        }

                        routeUsingRole(
                                document
                        );
                    }
            )
            .addOnFailureListener(
                    e -> showLoginError(
                            "Unable to load user profile."
                    )
            );
}

private void findProfileByPhone(
        FirebaseUser user) {

    String phone = "";

    String internalEmail =
            user.getEmail();

    if (internalEmail != null
            && internalEmail.endsWith(
            "@sakyna.app")) {

        phone =
                internalEmail.substring(
                        0,
                        internalEmail.length()
                                - "@sakyna.app".length()
                );
    }

    if (phone.isEmpty()
            && phoneInput != null) {

        phone =
                normalizePhone(
                        phoneInput
                                .getText()
                                .toString()
                );
    }

    if (phone.isEmpty()) {

        showLoginError(
                "User profile was not found."
        );

        return;
    }

    final String searchedPhone =
            phone;

    db.collection("users")
            .whereEqualTo(
                    "phone",
                    searchedPhone
            )
            .limit(1)
            .get()
            .addOnSuccessListener(
                    query -> {

                        if (query.isEmpty()) {

                            showLoginError(
                                    "User profile was not found."
                            );

                        } else {

                            routeUsingRole(
                                    query.getDocuments()
                                            .get(0)
                            );
                        }
                    }
            )
            .addOnFailureListener(
                    e -> showLoginError(
                            "Unable to load user profile."
                    )
            );
}

private void routeUsingRole(
        DocumentSnapshot document) {

    if (document == null
            || !document.exists()) {

        showLoginError(
                "User profile was not found."
        );

        return;
    }

    String role =
            document.getString("role");

    if (role == null) {

        showLoginError(
                "This account has no role."
        );

        return;
    }

    role =
            role.trim().toUpperCase();

    if ("DRIVER".equals(role)) {

        openScreen(
                DriverActivity.class
        );

        return;
    }

    if ("PASSENGER".equals(role)) {

        openScreen(
                PassengerActivity.class
        );

        return;
    }

    if ("ADMIN".equals(role)) {

        openScreen(
                AdminActivity.class
        );

        return;
    }

    showLoginError(
            "Unknown account role: "
                    + role
    );
}

private void createAccount() {

    if (!validCreateInput()) {
        return;
    }

    final String role =
            selectedRole
                    .trim()
                    .toUpperCase();

    if ("ADMIN".equals(role)) {

        toast(
                "Admin accounts are handled separately."
        );

        return;
    }

    if (!"PASSENGER".equals(role)
            && !"DRIVER".equals(role)) {

        toast(
                "Select PASSENGER or DRIVER."
        );

        return;
    }

    final String name =
            nameInput.getText()
                    .toString()
                    .trim();

    final String town =
            townInput.getText()
                    .toString()
                    .trim();

    final String province =
            provinceInput.getText()
                    .toString()
                    .trim();

    final String phone =
            normalizePhone(
                    phoneInput
                            .getText()
                            .toString()
            );

    final String password =
            passwordInput
                    .getText()
                    .toString();

    final String plate =
            plateInput.getText()
                    .toString()
                    .trim();

    final String franchise =
            franchiseInput.getText()
                    .toString()
                    .trim();

    final String vehicle =
            vehicleInput.getText()
                    .toString()
                    .trim();

    loginButton.setEnabled(false);
    createButton.setEnabled(false);
    createButton.setText("CREATING...");

    auth.createUserWithEmailAndPassword(
            firebaseIdentifier(phone),
            password
    )
    .addOnSuccessListener(
            result -> {

                FirebaseUser user =
                        result.getUser();

                if (user == null) {

                    resetCreateButton();

                    toast(
                            "Account creation failed."
                    );

                    return;
                }

                Map<String, Object> profile =
                        new HashMap<>();

                profile.put(
                        "name",
                        name
                );

                profile.put(
                        "town",
                        town
                );

                profile.put(
                        "province",
                        province
                );

                profile.put(
                        "phone",
                        phone
                );

                profile.put(
                        "role",
                        role
                );

                if ("DRIVER".equals(role)) {

                    profile.put(
                            "driverName",
                            name
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
                            "driverStatus",
                            "PENDING_APPROVAL"
                    );

                    profile.put(
                            "canAcceptRides",
                            false
                    );
                }

                profile.put(
                        "approved",
                        !"DRIVER".equals(role)
                );

                profile.put(
                        "approvalStatus",
                        "DRIVER".equals(role)
                                ? "PENDING_APPROVAL"
                                : "APPROVED"
                );

                profile.put(
                        "online",
                        false
                );

                profile.put(
                        "createdAt",
                        FieldValue.serverTimestamp()
                );

                db.collection("users")
                        .document(user.getUid())
                        .set(profile)
                        .addOnSuccessListener(
                                v -> {

                                    auth.signOut();

                                    passwordInput
                                            .setText("");

                                    resetCreateButton();

                                    if ("DRIVER".equals(role)) {

                                        toast(
                                                "DRIVER account created.\n"
                                                        + "Plate/Franchise and tricycle description saved.\n"
                                                        + "Please LOGIN.\n"
                                                        + "Admin approval is required before GO ONLINE."
                                        );

                                    } else {

                                        toast(
                                                "PASSENGER account created.\n"
                                                        + "Please LOGIN."
                                        );
                                    }
                                }
                        )
                        .addOnFailureListener(
                                e -> {

                                    auth.signOut();

                                    resetCreateButton();

                                    toast(
                                            "Could not save account."
                                    );
                                }
                        );
            }
    )
    .addOnFailureListener(
            e -> {

                resetCreateButton();

                String message =
                        e.getMessage();

                if (message == null
                        || message.trim().isEmpty()) {

                    message =
                            "Account creation failed.";
                }

                toast(message);
            }
    );
}

private void resetCreateButton() {

    if (loginButton != null) {

        loginButton.setEnabled(true);
        loginButton.setText("LOGIN");
    }

    if (createButton != null) {

        createButton.setEnabled(true);
        createButton.setText(
                "CREATE ACCOUNT"
        );
    }
}

private void openScreen(
        Class<?> activityClass) {

    Intent intent =
            new Intent(
                    this,
                    activityClass
            );

    intent.addFlags(
            Intent.FLAG_ACTIVITY_NEW_TASK
                    | Intent.FLAG_ACTIVITY_CLEAR_TASK
    );

    startActivity(intent);
    finish();
}

private void showLoginError(
        String message) {

    auth.signOut();

    showLoginScreen();

    toast(message);
}

private void toast(String message) {

    Toast.makeText(
            this,
            message,
            Toast.LENGTH_LONG
    ).show();
}

}

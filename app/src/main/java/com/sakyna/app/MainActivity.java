
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
import android.widget.TextView;
import android.widget.Toast;

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

    private EditText phoneInput;
    private EditText passwordInput;

    private Button passengerButton;
    private Button driverButton;
    private Button adminButton;

    private String selectedRole = "PASSENGER";

    private static final int GREEN = Color.rgb(0, 120, 70);
    private static final int BLUE = Color.rgb(30, 90, 180);
    private static final int ORANGE = Color.rgb(190, 95, 0);

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);

        auth = FirebaseAuth.getInstance();
        db = FirebaseFirestore.getInstance();

        FirebaseUser currentUser = auth.getCurrentUser();

        if (currentUser != null) {
            loadUserRole(currentUser);
        } else {
            showLoginScreen();
        }
    }

    private void showLoginScreen() {

        LinearLayout root = new LinearLayout(this);
        root.setOrientation(LinearLayout.VERTICAL);
        root.setGravity(Gravity.CENTER_HORIZONTAL);
        root.setPadding(28, 25, 28, 25);
        root.setBackgroundColor(Color.WHITE);

        TextView title = new TextView(this);
        title.setText("🛺 SAKAY NA");
        title.setTextSize(32);
        title.setTypeface(null, android.graphics.Typeface.BOLD);
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
        roleLabel.setTypeface(null, android.graphics.Typeface.BOLD);
        roleLabel.setTextColor(Color.DKGRAY);
        roleLabel.setGravity(Gravity.CENTER);
        root.addView(roleLabel, full());

        LinearLayout roleRow = new LinearLayout(this);
        roleRow.setOrientation(LinearLayout.HORIZONTAL);
        roleRow.setGravity(Gravity.CENTER);

        passengerButton = makeRoleButton("PASSENGER");
        driverButton = makeRoleButton("DRIVER");
        adminButton = makeRoleButton("ADMIN");

        roleRow.addView(passengerButton, weighted());
        roleRow.addView(driverButton, weighted());
        roleRow.addView(adminButton, weighted());

        root.addView(roleRow, full());

        passengerButton.setOnClickListener(v ->
                selectRole("PASSENGER"));

        driverButton.setOnClickListener(v ->
                selectRole("DRIVER"));

        adminButton.setOnClickListener(v ->
                selectRole("ADMIN"));

        TextView phoneLabel = label("Phone number");
        phoneLabel.setPadding(0, 18, 0, 3);
        root.addView(phoneLabel, full());

        phoneInput = new EditText(this);
        phoneInput.setHint("09XXXXXXXXX");
        phoneInput.setTextSize(18);
        phoneInput.setSingleLine(true);
        phoneInput.setInputType(InputType.TYPE_CLASS_PHONE);
        root.addView(phoneInput, full());

        TextView passwordLabel = label("Password");
        passwordLabel.setPadding(0, 14, 0, 3);
        root.addView(passwordLabel, full());

        passwordInput = new EditText(this);
        passwordInput.setHint("Password");
        passwordInput.setTextSize(18);
        passwordInput.setTextSize(18);
        passwordInput.setSingleLine(true);
        passwordInput.setInputType(
                InputType.TYPE_CLASS_TEXT |
                InputType.TYPE_TEXT_VARIATION_PASSWORD
        );
        root.addView(passwordInput, full());

        Button loginButton = new Button(this);
        loginButton.setText("LOGIN");
        loginButton.setTextSize(17);
        loginButton.setTextColor(Color.WHITE);
        loginButton.setBackgroundColor(GREEN);

        loginButton.setOnClickListener(v -> login());

        LinearLayout.LayoutParams loginParams = full();
        loginParams.topMargin = 18;
        root.addView(loginButton, loginParams);

        Button createButton = new Button(this);
        createButton.setText("CREATE ACCOUNT");
        createButton.setTextSize(17);

        createButton.setOnClickListener(v -> createAccount());

        root.addView(createButton, full());

        TextView information = new TextView(this);
        information.setText(
                "Phone number + password only\n" +
                "Driver accounts require Admin approval before GO ONLINE."
        );
        information.setTextSize(14);
        information.setTextColor(Color.GRAY);
        information.setGravity(Gravity.CENTER);
        information.setPadding(0, 14, 0, 0);

        root.addView(information, full());

        setContentView(root);

        selectRole("PASSENGER");
    }

    private TextView label(String text) {

        TextView t = new TextView(this);
        t.setText(text);
        t.setTextSize(14);
        t.setTypeface(null, android.graphics.Typeface.BOLD);
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

        selectedRole = role.trim().toUpperCase();

        passengerButton.setText("PASSENGER");
        driverButton.setText("DRIVER");
        adminButton.setText("ADMIN");

        passengerButton.setTextColor(Color.DKGRAY);
        driverButton.setTextColor(Color.DKGRAY);
        adminButton.setTextColor(Color.DKGRAY);

        passengerButton.setBackgroundColor(Color.TRANSPARENT);
        driverButton.setBackgroundColor(Color.TRANSPARENT);
        adminButton.setBackgroundColor(Color.TRANSPARENT);

        if ("DRIVER".equals(selectedRole)) {

            driverButton.setTextColor(Color.WHITE);
            driverButton.setBackgroundColor(BLUE);

        } else if ("ADMIN".equals(selectedRole)) {

            adminButton.setTextColor(Color.WHITE);
            adminButton.setBackgroundColor(ORANGE);

        } else {

            selectedRole = "PASSENGER";

            passengerButton.setTextColor(Color.WHITE);
            passengerButton.setBackgroundColor(GREEN);
        }
    }

    private String normalizePhone(String input) {

        if (input == null) {
            return "";
        }

        String phone = input.replaceAll("[^0-9]", "");

        if (phone.startsWith("0") && phone.length() == 11) {
            phone = "63" + phone.substring(1);
        } else if (phone.startsWith("9") && phone.length() == 10) {
            phone = "63" + phone;
        }

        return phone;
    }

    private String firebaseIdentifier(String phone) {
        return phone + "@sakyna.app";
    }

    private boolean validInput() {

        String phone = normalizePhone(
                phoneInput == null
                        ? ""
                        : phoneInput.getText().toString()
        );

        String password = passwordInput == null
                ? ""
                : passwordInput.getText().toString();

        if (phone.length() != 12 || !phone.startsWith("63")) {
            toast("Enter a valid Philippine phone number.");
            return false;
        }

        if (password.length() < 6) {
            toast("Password must be at least 6 characters.");
            return false;
        }

        return true;
    }

    private void login() {

        if (!validInput()) {
            return;
        }

        String phone = normalizePhone(
                phoneInput.getText().toString()
        );

        String password =
                passwordInput.getText().toString();

        auth.signInWithEmailAndPassword(
                firebaseIdentifier(phone),
                password
        )
        .addOnSuccessListener(result -> {

            FirebaseUser user = auth.getCurrentUser();

            if (user == null) {
                toast("Login failed.");
                return;
            }

            loadUserRole(user);
        })
        .addOnFailureListener(e -> {

            toast("Invalid phone number or password.");
        });
    }

    /*
     * Load the profile using the AUTHENTICATED Firebase UID.
     *
     * This is the important Admin fix.
     */
    private void loadUserRole(FirebaseUser user) {

        if (user == null) {
            showLoginError("Account not found.");
            return;
        }

        final String uid = user.getUid();

        db.collection("users")
                .document(uid)
                .get()
                .addOnSuccessListener(document -> {

                    if (document.exists()) {

                        String role =
                                document.getString("role");

                        /*
                         * If the UID document has a role,
                         * route immediately.
                         */
                        if (role != null &&
                                !role.trim().isEmpty()) {

                            routeUsingRole(document);
                            return;
                        }

                        /*
                         * Existing document but missing role.
                         * Try the phone profile before failing.
                         */
                        findProfileByPhone(user, uid);
                        return;
                    }

                    /*
                     * UID document doesn't exist.
                     * Try finding the profile by phone.
                     */
                    findProfileByPhone(user, uid);
                })
                .addOnFailureListener(e -> {

                    showLoginError(
                            "Unable to load account."
                    );
                });
    }

    private void findProfileByPhone(
            FirebaseUser user,
            String authenticatedUid) {

        String phone = "";

        String internalEmail = user.getEmail();

        if (internalEmail != null &&
                internalEmail.endsWith("@sakyna.app")) {

            phone = internalEmail.substring(
                    0,
                    internalEmail.length()
                            - "@sakyna.app".length()
            );
        }

        if (phone.isEmpty() && phoneInput != null) {

            phone = normalizePhone(
                    phoneInput.getText().toString()
            );
        }

        if (phone.isEmpty()) {

            showLoginError(
                    "User profile was not found."
            );
            return;
        }

        final String searchedPhone = phone;

        db.collection("users")
                .whereEqualTo("phone", searchedPhone)
                .limit(5)
                .get()
                .addOnSuccessListener(query -> {

                    if (query.isEmpty()) {

                        showLoginError(
                                "User profile was not found."
                        );
                        return;
                    }

                    /*
                     * Prefer the document belonging to
                     * the currently authenticated Firebase UID.
                     */
                    for (DocumentSnapshot doc :
                            query.getDocuments()) {

                        if (authenticatedUid.equals(
                                doc.getId())) {

                            routeUsingRole(doc);
                            return;
                        }
                    }

                    /*
                     * If no matching UID was found,
                     * use a profile that actually contains
                     * a valid role.
                     */
                    for (DocumentSnapshot doc :
                            query.getDocuments()) {

                        String role =
                                doc.getString("role");

                        if (role != null &&
                                !role.trim().isEmpty()) {

                            routeUsingRole(doc);
                            return;
                        }
                    }

                    showLoginError(
                            "This account has no role."
                    );
                })
                .addOnFailureListener(e -> {

                    showLoginError(
                            "Unable to load user profile."
                    );
                });
    }

    private void routeUsingRole(
            DocumentSnapshot document) {

        if (document == null ||
                !document.exists()) {

            showLoginError(
                    "User profile was not found."
            );
            return;
        }

        String role =
                document.getString("role");

        if (role == null ||
                role.trim().isEmpty()) {

            showLoginError(
                    "This account has no role."
            );
            return;
        }

        role = role.trim().toUpperCase();

        /*
         * PASSENGER -> PassengerActivity
         * DRIVER    -> DriverActivity
         * ADMIN     -> AdminActivity
         */

        if ("PASSENGER".equals(role)) {

            openScreen(PassengerActivity.class);
            return;
        }

        if ("DRIVER".equals(role)) {

            openScreen(DriverActivity.class);
            return;
        }

        if ("ADMIN".equals(role)) {

            openScreen(AdminActivity.class);
            return;
        }

        showLoginError(
                "Unknown account role: " + role
        );
    }

    private void createAccount() {

        if (!validInput()) {
            return;
        }

        final String role =
                selectedRole.trim().toUpperCase();

        /*
         * Admin creation remains separate.
         */
        if ("ADMIN".equals(role)) {

            toast(
                    "Admin accounts are handled separately."
            );
            return;
        }

        if (!"PASSENGER".equals(role) &&
                !"DRIVER".equals(role)) {

            toast(
                    "Select PASSENGER or DRIVER."
            );
            return;
        }

        final String phone =
                normalizePhone(
                        phoneInput.getText().toString()
                );

        final String password =
                passwordInput.getText().toString();

        auth.createUserWithEmailAndPassword(
                firebaseIdentifier(phone),
                password
        )
        .addOnSuccessListener(result -> {

            FirebaseUser user =
                    auth.getCurrentUser();

            if (user == null) {

                toast("Account creation failed.");
                return;
            }

            Map<String, Object> profile =
                    new HashMap<>();

            profile.put("phone", phone);
            profile.put("role", role);

            if ("DRIVER".equals(role)) {

                profile.put("approved", false);
                profile.put(
                        "approvalStatus",
                        "PENDING_APPROVAL"
                );
                profile.put("online", false);

            } else {

                profile.put("approved", true);
                profile.put(
                        "approvalStatus",
                        "APPROVED"
                );
                profile.put("online", false);
            }

            profile.put(
                    "createdAt",
                    FieldValue.serverTimestamp()
            );

            db.collection("users")
                    .document(user.getUid())
                    .set(profile)
                    .addOnSuccessListener(v -> {

                        auth.signOut();

                        passwordInput.setText("");

                        if ("DRIVER".equals(role)) {

                            toast(
                                    "DRIVER account created.\n" +
                                    "Please LOGIN.\n" +
                                    "Admin approval is required before GO ONLINE."
                            );

                        } else {

                            toast(
                                    "PASSENGER account created.\n" +
                                    "Please LOGIN."
                            );
                        }
                    })
                    .addOnFailureListener(e -> {

                        auth.signOut();

                        toast(
                                "Could not save account."
                        );
                    });
        })
        .addOnFailureListener(e -> {

            String message = e.getMessage();

            if (message == null ||
                    message.trim().isEmpty()) {

                message = "Account creation failed.";
            }

            toast(message);
        });
    }

    private void openScreen(
            Class<?> activityClass) {

        Intent intent =
                new Intent(this, activityClass);

        intent.addFlags(
                Intent.FLAG_ACTIVITY_NEW_TASK |
                Intent.FLAG_ACTIVITY_CLEAR_TASK
        );

        startActivity(intent);
        finish();
    }

    private void showLoginError(String message) {

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

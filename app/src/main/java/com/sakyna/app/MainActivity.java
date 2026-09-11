
package com.sakyna.app;

import android.app.Activity;
import android.graphics.Color;
import android.graphics.drawable.GradientDrawable;
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

import java.util.ArrayList;
import java.util.Random;

public class MainActivity extends Activity {

    private final int GREEN = Color.rgb(0, 150, 90);
    private final int DARK_GREEN = Color.rgb(0, 105, 65);
    private final int ORANGE = Color.rgb(245, 140, 35);
    private final int BLUE = Color.rgb(35, 115, 210);
    private final int PURPLE = Color.rgb(125, 70, 190);
    private final int RED = Color.rgb(210, 55, 55);
    private final int WHITE = Color.WHITE;
    private final int DARK = Color.rgb(35, 35, 35);

    private LinearLayout root;

    private static class User {
        String name;
        String mobile;
        String password;
        String role;
        boolean verified;

        User(String name, String mobile, String password, String role) {
            this.name = name;
            this.mobile = mobile;
            this.password = password;
            this.role = role;
            this.verified = false;
        }
    }

    private static class Transaction {
        String passenger;
        String driver;
        String pickup;
        String destination;
        int fare;
        String status;

        Transaction(String passenger, String driver, String pickup,
                    String destination, int fare, String status) {
            this.passenger = passenger;
            this.driver = driver;
            this.pickup = pickup;
            this.destination = destination;
            this.fare = fare;
            this.status = status;
        }
    }

    private final ArrayList<User> users = new ArrayList<>();
    private final ArrayList<Transaction> transactions = new ArrayList<>();

    private User pendingUser;
    private String pendingOtp = "";

    private String loggedInName = "";
    private String loggedInMobile = "";
    private String loggedInRole = "";

    private String pickup = "";
    private String destination = "";
    private int fare = 50;

    private int rideStatus = 0;
    private String currentDriver = "";

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);

        seedDemoAccounts();
        showHome();
    }

    private void seedDemoAccounts() {
        User admin = new User("Administrator", "09000000000",
                "admin123", "Admin");
        admin.verified = true;
        users.add(admin);
    }

    private void setupRoot() {
        root = new LinearLayout(this);
        root.setOrientation(LinearLayout.VERTICAL);
        root.setPadding(28, 30, 28, 30);

        GradientDrawable bg = new GradientDrawable(
                GradientDrawable.Orientation.TL_BR,
                new int[]{
                        Color.rgb(0, 150, 90),
                        Color.rgb(0, 105, 160)
                }
        );
        root.setBackground(bg);

        ScrollView scroll = new ScrollView(this);
        scroll.setFillViewport(true);
        scroll.addView(root);

        setContentView(scroll);
    }

    private TextView title(String text) {
        TextView t = new TextView(this);
        t.setText(text);
        t.setTextColor(WHITE);
        t.setTextSize(30);
        t.setGravity(Gravity.CENTER);
        t.setTypeface(null, android.graphics.Typeface.BOLD);
        t.setPadding(10, 20, 10, 20);

        root.addView(t, new LinearLayout.LayoutParams(
                -1, LinearLayout.LayoutParams.WRAP_CONTENT
        ));

        return t;
    }

    private TextView subtitle(String text) {
        TextView t = new TextView(this);
        t.setText(text);
        t.setTextColor(WHITE);
        t.setTextSize(16);
        t.setGravity(Gravity.CENTER);
        t.setPadding(10, 5, 10, 25);

        root.addView(t, new LinearLayout.LayoutParams(
                -1, LinearLayout.LayoutParams.WRAP_CONTENT
        ));

        return t;
    }

    private Button button(String text, int color) {
        Button b = new Button(this);
        b.setText(text);
        b.setTextColor(WHITE);
        b.setTextSize(17);
        b.setAllCaps(false);
        b.setTypeface(null, android.graphics.Typeface.BOLD);

        GradientDrawable drawable = new GradientDrawable();
        drawable.setColor(color);
        drawable.setCornerRadius(35);
        b.setBackground(drawable);

        LinearLayout.LayoutParams p =
                new LinearLayout.LayoutParams(-1, 62);
        p.setMargins(0, 10, 0, 10);

        root.addView(b, p);
        return b;
    }

    private EditText field(String hint) {
        EditText e = new EditText(this);
        e.setHint(hint);
        e.setTextSize(16);
        e.setTextColor(DARK);
        e.setHintTextColor(Color.GRAY);
        e.setSingleLine(true);
        e.setPadding(22, 0, 22, 0);

        GradientDrawable bg = new GradientDrawable();
        bg.setColor(WHITE);
        bg.setCornerRadius(25);
        e.setBackground(bg);

        LinearLayout.LayoutParams p =
                new LinearLayout.LayoutParams(-1, 58);
        p.setMargins(0, 8, 0, 8);

        root.addView(e, p);
        return e;
    }

    private void message(String text) {
        TextView t = new TextView(this);
        t.setText(text);
        t.setTextColor(WHITE);
        t.setTextSize(15);
        t.setPadding(12, 15, 12, 15);
        root.addView(t);
    }

    private void backButton(final Runnable action) {
        Button b = button("← Back", DARK_GREEN);
        b.setOnClickListener(v -> action.run());
    }

    // ------------------------------------------------------------
    // HOME
    // ------------------------------------------------------------

    private void showHome() {
        setupRoot();

        title("SAKAY NA");
        subtitle("Your tricycle ride companion");

        TextView welcome = new TextView(this);
        welcome.setText("Ride safe. Ride easy.");
        welcome.setTextColor(WHITE);
        welcome.setTextSize(19);
        welcome.setGravity(Gravity.CENTER);
        welcome.setTypeface(null, android.graphics.Typeface.BOLD);
        welcome.setPadding(10, 5, 10, 30);

        root.addView(welcome);

        Button login = button("🔐  Log In", BLUE);
        login.setOnClickListener(v -> showLogin());

        Button register = button("📝  Register", ORANGE);
        register.setOnClickListener(v -> showRegisterRoles());

        message("Passenger • Driver • Admin");
    }

    // ------------------------------------------------------------
    // REGISTER ROLE
    // ------------------------------------------------------------

    private void showRegisterRoles() {
        setupRoot();

        title("REGISTER");
        subtitle("Choose your account type");

        Button passenger = button("🟠  Passenger", ORANGE);
        passenger.setOnClickListener(v -> showRegistration("Passenger"));

        Button driver = button("🔵  Driver", BLUE);
        driver.setOnClickListener(v -> showRegistration("Driver"));

        Button admin = button("🟣  Admin", PURPLE);
        admin.setOnClickListener(v -> showRegistration("Admin"));

        backButton(this::showHome);
    }

    // ------------------------------------------------------------
    // REGISTRATION
    // ------------------------------------------------------------

    private void showRegistration(String role) {
        setupRoot();

        title(role + " Registration");
        subtitle("Create your Sakay Na account");

        EditText name = field("Full Name");

        EditText mobile = field("Mobile Number");
        mobile.setInputType(InputType.TYPE_CLASS_PHONE);

        EditText password = field("Password");
        password.setInputType(
                InputType.TYPE_CLASS_TEXT |
                InputType.TYPE_TEXT_VARIATION_PASSWORD
        );

        Button register = button("SEND OTP", GREEN);

        register.setOnClickListener(v -> {

            String n = name.getText().toString().trim();
            String m = mobile.getText().toString().trim();
            String p = password.getText().toString();

            if (n.isEmpty()) {
                toast("Enter your full name.");
                return;
            }

            if (!m.matches("09[0-9]{9}")) {
                toast("Use a valid 11-digit mobile number.");
                return;
            }

            if (p.length() < 4) {
                toast("Password must be at least 4 characters.");
                return;
            }

            for (User u : users) {
                if (u.mobile.equals(m)) {
                    toast("This mobile number is already registered.");
                    return;
                }
            }

            pendingUser = new User(n, m, p, role);

            pendingOtp = String.format(
                    "%06d",
                    new Random().nextInt(1000000)
            );

            showOtp();
        });

        backButton(this::showRegisterRoles);
    }

    // ------------------------------------------------------------
    // OTP
    // ------------------------------------------------------------

    private void showOtp() {
        setupRoot();

        title("VERIFY MOBILE");
        subtitle("Enter the OTP sent to your mobile");

        message("DEMO OTP: " + pendingOtp);

        EditText otp = field("Enter 6-digit OTP");
        otp.setInputType(InputType.TYPE_CLASS_NUMBER);

        Button verify = button("VERIFY OTP", GREEN);

        verify.setOnClickListener(v -> {

            if (otp.getText().toString().trim().equals(pendingOtp)) {

                pendingUser.verified = true;
                users.add(pendingUser);

                toast("Registration successful!");

                loggedInName = pendingUser.name;
                loggedInMobile = pendingUser.mobile;
                loggedInRole = pendingUser.role;

                pendingUser = null;
                pendingOtp = "";

                openDashboard();

            } else {
                toast("Incorrect OTP.");
            }
        });

        backButton(() -> {
            pendingUser = null;
            pendingOtp = "";
            showRegisterRoles();
        });
    }

    // ------------------------------------------------------------
    // LOGIN
    // ------------------------------------------------------------

    private void showLogin() {
        setupRoot();

        title("LOG IN");
        subtitle("Welcome back to Sakay Na");

        EditText mobile = field("Mobile Number");
        mobile.setInputType(InputType.TYPE_CLASS_PHONE);

        EditText password = field("Password");
        password.setInputType(
                InputType.TYPE_CLASS_TEXT |
                InputType.TYPE_TEXT_VARIATION_PASSWORD
        );

        Button login = button("LOG IN", BLUE);

        login.setOnClickListener(v -> {

            String m = mobile.getText().toString().trim();
            String p = password.getText().toString();

            for (User u : users) {

                if (u.mobile.equals(m) && u.password.equals(p)) {

                    if (!u.verified) {
                        toast("Account is not verified.");
                        return;
                    }

                    loggedInName = u.name;
                    loggedInMobile = u.mobile;
                    loggedInRole = u.role;

                    openDashboard();
                    return;
                }
            }

            toast("Invalid mobile number or password.");
        });

        Button demo = button("Demo Admin Login", PURPLE);
        demo.setOnClickListener(v -> {
            mobile.setText("09000000000");
            password.setText("admin123");
        });

        backButton(this::showHome);
    }

    // ------------------------------------------------------------
    // DASHBOARD
    // ------------------------------------------------------------

    private void openDashboard() {

        if (loggedInRole.equals("Passenger")) {
            showPassenger();
        } else if (loggedInRole.equals("Driver")) {
            showDriver();
        } else {
            showAdmin();
        }
    }

    // ------------------------------------------------------------
    // PASSENGER
    // ------------------------------------------------------------

    private void showPassenger() {
        setupRoot();

        title("PASSENGER");
        subtitle("Welcome, " + loggedInName);

        if (rideStatus == 0) {

            EditText pickupField = field("Pickup location");
            EditText destinationField = field("Destination");

            TextView fareText = new TextView(this);
            fareText.setText("Estimated Fare: ₱50");
            fareText.setTextColor(WHITE);
            fareText.setTextSize(18);
            fareText.setGravity(Gravity.CENTER);
            fareText.setPadding(10, 15, 10, 15);

            root.addView(fareText);

            Button request = button("🚕 REQUEST RIDE", ORANGE);

            request.setOnClickListener(v -> {

                pickup = pickupField.getText().toString().trim();
                destination = destinationField.getText().toString().trim();

                if (pickup.isEmpty() || destination.isEmpty()) {
                    toast("Enter pickup and destination.");
                    return;
                }

                fare = 50;
                rideStatus = 1;

                transactions.add(new Transaction(
                        loggedInName,
                        "Searching for driver",
                        pickup,
                        destination,
                        fare,
                        "Searching"
                ));

                showPassenger();
            });

        } else {

            message(
                    "Pickup: " + pickup +
                    "\nDestination: " + destination +
                    "\nFare: ₱" + fare +
                    "\n\nRide status: " + rideStatusText()
            );

            if (!currentDriver.isEmpty()) {
                message("Driver: " + currentDriver);
            }

            if (rideStatus == 6) {
                Button newRide = button("REQUEST NEW RIDE", GREEN);
                newRide.setOnClickListener(v -> {
                    rideStatus = 0;
                    currentDriver = "";
                    showPassenger();
                });
            }
        }

        Button logout = button("LOG OUT", RED);
        logout.setOnClickListener(v -> logout());

    }

    // ------------------------------------------------------------
    // DRIVER
    // ------------------------------------------------------------

    private void showDriver() {
        setupRoot();

        title("DRIVER");
        subtitle("Welcome, " + loggedInName);

        if (rideStatus == 0) {

            message("No active ride request.");

        } else {

            message(
                    "Passenger: " + getCurrentPassenger() +
                    "\nPickup: " + pickup +
                    "\nDestination: " + destination +
                    "\nFare: ₱" + fare +
                    "\n\nStatus: " + rideStatusText()
            );

            Button action = button(driverActionText(), BLUE);

            action.setOnClickListener(v -> {

                if (rideStatus == 1) {
                    currentDriver = loggedInName;
                    rideStatus = 2;
                    updateTransaction("Accepted");
                } else if (rideStatus == 2) {
                    rideStatus = 3;
                    updateTransaction("Driver on the way");
                } else if (rideStatus == 3) {
                    rideStatus = 4;
                    updateTransaction("Driver arrived");
                } else if (rideStatus == 4) {
                    rideStatus = 5;
                    updateTransaction("Trip in progress");
                } else if (rideStatus == 5) {
                    rideStatus = 6;
                    updateTransaction("Completed");
                }

                showDriver();
            });
        }

        Button logout = button("LOG OUT", RED);
        logout.setOnClickListener(v -> logout());
    }

    private String driverActionText() {
        if (rideStatus == 1) return "ACCEPT RIDE";
        if (rideStatus == 2) return "START DRIVING";
        if (rideStatus == 3) return "MARK AS ARRIVED";
        if (rideStatus == 4) return "START TRIP";
        if (rideStatus == 5) return "COMPLETE TRIP";
        return "RIDE COMPLETED";
    }

    // ------------------------------------------------------------
    // ADMIN
    // ------------------------------------------------------------

    private void showAdmin() {
        setupRoot();

        title("ADMIN");
        subtitle("Sakay Na Management");

        int passengers = 0;
        int drivers = 0;
        int admins = 0;

        for (User u : users) {
            if (u.role.equals("Passenger")) passengers++;
            if (u.role.equals("Driver")) drivers++;
            if (u.role.equals("Admin")) admins++;
        }

        message(
                "REGISTERED USERS\n\n" +
                "Passengers: " + passengers +
                "\nDrivers: " + drivers +
                "\nAdmins: " + admins
        );

        Button usersButton = button("👥 VIEW ALL USERS", PURPLE);
        usersButton.setOnClickListener(v -> showAllUsers());

        Button transactionsButton =
                button("💰 VIEW TRANSACTIONS", ORANGE);

        transactionsButton.setOnClickListener(
                v -> showTransactions()
        );

        Button monitor = button("📍 MONITOR CURRENT RIDE", BLUE);
        monitor.setOnClickListener(v -> showCurrentRide());

        Button reset = button("RESET CURRENT RIDE", RED);
        reset.setOnClickListener(v -> {
            rideStatus = 0;
            currentDriver = "";
            toast("Current ride reset.");
            showAdmin();
        });

        Button logout = button("LOG OUT", RED);
        logout.setOnClickListener(v -> logout());
    }

    private void showAllUsers() {
        setupRoot();

        title("ALL USERS");

        if (users.isEmpty()) {
            message("No registered users.");
        }

        for (User u : users) {
            message(
                    "Name: " + u.name +
                    "\nMobile: " + u.mobile +
                    "\nRole: " + u.role +
                    "\nVerified: " + u.verified
            );
        }

        backButton(this::showAdmin);
    }

    private void showTransactions() {
        setupRoot();

        title("TRANSACTIONS");

        if (transactions.isEmpty()) {
            message("No transactions yet.");
        }

        for (Transaction t : transactions) {
            message(
                    "Passenger: " + t.passenger +
                    "\nDriver: " + t.driver +
                    "\nPickup: " + t.pickup +
                    "\nDestination: " + t.destination +
                    "\nFare: ₱" + t.fare +
                    "\nStatus: " + t.status
            );
        }

        backButton(this::showAdmin);
    }

    private void showCurrentRide() {
        setupRoot();

        title("CURRENT RIDE");

        if (rideStatus == 0) {
            message("There is no active ride.");
        } else {
            message(
                    "Passenger: " + getCurrentPassenger() +
                    "\nDriver: " +
                    (currentDriver.isEmpty()
                            ? "Not assigned"
                            : currentDriver) +
                    "\nPickup: " + pickup +
                    "\nDestination: " + destination +
                    "\nFare: ₱" + fare +
                    "\nStatus: " + rideStatusText()
            );
        }

        backButton(this::showAdmin);
    }

    // ------------------------------------------------------------
    // HELPERS
    // ------------------------------------------------------------

    private String getCurrentPassenger() {
        if (loggedInRole.equals("Passenger")) {
            return loggedInName;
        }

        for (Transaction t : transactions) {
            if (t.pickup.equals(pickup) &&
                    t.destination.equals(destination)) {
                return t.passenger;
            }
        }

        return "Passenger";
    }

    private String rideStatusText() {
        switch (rideStatus) {
            case 1:
                return "Searching for driver";
            case 2:
                return "Driver accepted";
            case 3:
                return "Driver on the way";
            case 4:
                return "Driver arrived";
            case 5:
                return "Trip in progress";
            case 6:
                return "Trip completed";
            default:
                return "No active ride";
        }
    }

    private void updateTransaction(String status) {
        if (!transactions.isEmpty()) {
            Transaction t =
                    transactions.get(transactions.size() - 1);

            t.driver = currentDriver.isEmpty()
                    ? "Not assigned"
                    : currentDriver;

            t.status = status;
        }
    }

    private void logout() {
        loggedInName = "";
        loggedInMobile = "";
        loggedInRole = "";
        showHome();
    }

    private void toast(String text) {
        Toast.makeText(
                this,
                text,
                Toast.LENGTH_SHORT
        ).show();
    }
}

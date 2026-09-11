
package com.sakyna.app;

import android.app.Activity;
import android.graphics.Color;
import android.graphics.Typeface;
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

    // =========================
    // COLORS
    // =========================
    private static final int GREEN = Color.rgb(0, 155, 85);
    private static final int DARK_GREEN = Color.rgb(0, 105, 60);
    private static final int BLUE = Color.rgb(35, 115, 210);
    private static final int ORANGE = Color.rgb(245, 135, 25);
    private static final int PURPLE = Color.rgb(125, 70, 190);
    private static final int RED = Color.rgb(205, 55, 55);
    private static final int CYAN = Color.rgb(0, 150, 180);
    private static final int WHITE = Color.WHITE;
    private static final int DARK = Color.rgb(35, 35, 35);

    private LinearLayout root;

    // =========================
    // USERS
    // =========================
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

    private final ArrayList<User> users = new ArrayList<>();

    // =========================
    // TRANSACTIONS
    // =========================
    private static class Transaction {
        String passenger;
        String passengerMobile;
        String driver;
        String driverMobile;
        String pickup;
        String destination;
        int fare;
        String status;

        Transaction(
                String passenger,
                String passengerMobile,
                String driver,
                String driverMobile,
                String pickup,
                String destination,
                int fare,
                String status
        ) {
            this.passenger = passenger;
            this.passengerMobile = passengerMobile;
            this.driver = driver;
            this.driverMobile = driverMobile;
            this.pickup = pickup;
            this.destination = destination;
            this.fare = fare;
            this.status = status;
        }
    }

    private final ArrayList<Transaction> transactions =
            new ArrayList<>();

    // =========================
    // LOGIN / OTP
    // =========================
    private User pendingUser;
    private String pendingOtp = "";

    private String loggedName = "";
    private String loggedMobile = "";
    private String loggedRole = "";

    // =========================
    // CURRENT RIDE
    // =========================
    /*
       0 = No booking
       1 = Searching
       2 = Accepted
       3 = Driver on the way
       4 = Driver arrived
       5 = Trip started
       6 = Finished
    */

    private int rideStatus = 0;

    private String ridePassenger = "";
    private String ridePassengerMobile = "";

    private String rideDriver = "";
    private String rideDriverMobile = "";

    private String ridePickup = "";
    private String rideDestination = "";

    private int rideFare = 50;

    // =========================
    // APP START
    // =========================

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);

        createDemoAdmin();
        showHome();
    }

    private void createDemoAdmin() {

        User admin = new User(
                "Administrator",
                "09000000000",
                "admin123",
                "Admin"
        );

        admin.verified = true;
        users.add(admin);
    }

    // =========================
    // BASIC UI
    // =========================

    private void setupRoot() {

        root = new LinearLayout(this);
        root.setOrientation(LinearLayout.VERTICAL);
        root.setPadding(25, 25, 25, 30);

        GradientDrawable background =
                new GradientDrawable(
                        GradientDrawable.Orientation.TL_BR,
                        new int[]{
                                Color.rgb(0, 160, 90),
                                Color.rgb(0, 105, 175)
                        }
                );

        root.setBackground(background);

        ScrollView scroll = new ScrollView(this);
        scroll.setFillViewport(true);
        scroll.addView(root);

        setContentView(scroll);
    }

    private TextView heading(String text) {

        TextView view = new TextView(this);

        view.setText(text);
        view.setTextColor(WHITE);
        view.setTextSize(30);
        view.setTypeface(null, Typeface.BOLD);
        view.setGravity(Gravity.CENTER);
        view.setPadding(10, 20, 10, 10);

        root.addView(
                view,
                new LinearLayout.LayoutParams(
                        -1,
                        -2
                )
        );

        return view;
    }

    private TextView subheading(String text) {

        TextView view = new TextView(this);

        view.setText(text);
        view.setTextColor(WHITE);
        view.setTextSize(16);
        view.setGravity(Gravity.CENTER);
        view.setPadding(10, 5, 10, 20);

        root.addView(
                view,
                new LinearLayout.LayoutParams(
                        -1,
                        -2
                )
        );

        return view;
    }

    private TextView info(String text) {

        TextView view = new TextView(this);

        view.setText(text);
        view.setTextColor(WHITE);
        view.setTextSize(16);
        view.setPadding(15, 15, 15, 15);

        root.addView(
                view,
                new LinearLayout.LayoutParams(
                        -1,
                        -2
                )
        );

        return view;
    }

    private Button button(String text, int color) {

        Button button = new Button(this);

        button.setText(text);
        button.setTextColor(WHITE);
        button.setTextSize(17);
        button.setAllCaps(false);
        button.setTypeface(null, Typeface.BOLD);

        GradientDrawable drawable = new GradientDrawable();
        drawable.setColor(color);
        drawable.setCornerRadius(40);

        button.setBackground(drawable);

        LinearLayout.LayoutParams params =
                new LinearLayout.LayoutParams(
                        -1,
                        62
                );

        params.setMargins(0, 8, 0, 8);

        root.addView(button, params);

        return button;
    }

    private EditText field(String hint) {

        EditText field = new EditText(this);

        field.setHint(hint);
        field.setTextSize(16);
        field.setTextColor(DARK);
        field.setHintTextColor(Color.GRAY);
        field.setSingleLine(true);
        field.setPadding(20, 0, 20, 0);

        GradientDrawable drawable = new GradientDrawable();
        drawable.setColor(WHITE);
        drawable.setCornerRadius(30);

        field.setBackground(drawable);

        LinearLayout.LayoutParams params =
                new LinearLayout.LayoutParams(
                        -1,
                        58
                );

        params.setMargins(0, 7, 0, 7);

        root.addView(field, params);

        return field;
    }

    private void back(Runnable action) {

        Button button = button(
                "← Back",
                DARK_GREEN
        );

        button.setOnClickListener(v -> action.run());
    }

    private void toast(String text) {

        Toast.makeText(
                this,
                text,
                Toast.LENGTH_SHORT
        ).show();
    }

    // =========================
    // HOME
    // =========================

    private void showHome() {

        setupRoot();

        heading("SAKAY NA");
        subheading("Tricycle ride service");

        info("Ride safe. Ride easy.");

        Button login = button(
                "🔐  LOG IN",
                BLUE
        );

        login.setOnClickListener(
                v -> showLogin()
        );

        Button register = button(
                "📝  REGISTER",
                ORANGE
        );

        register.setOnClickListener(
                v -> showRegisterRoles()
        );

        info(
                "PASSENGER  •  DRIVER  •  ADMIN"
        );
    }

    // =========================
    // REGISTER ROLE
    // =========================

    private void showRegisterRoles() {

        setupRoot();

        heading("REGISTER");
        subheading("Choose account type");

        Button passenger = button(
                "🟠  Passenger",
                ORANGE
        );

        passenger.setOnClickListener(
                v -> showRegistration("Passenger")
        );

        Button driver = button(
                "🔵  Driver",
                BLUE
        );

        driver.setOnClickListener(
                v -> showRegistration("Driver")
        );

        Button admin = button(
                "🟣  Admin",
                PURPLE
        );

        admin.setOnClickListener(
                v -> showRegistration("Admin")
        );

        back(this::showHome);
    }

    // =========================
    // REGISTRATION
    // =========================

    private void showRegistration(String role) {

        setupRoot();

        heading(role + " Registration");
        subheading("Create your Sakay Na account");

        EditText name =
                field("Full Name");

        EditText mobile =
                field("Mobile Number");

        mobile.setInputType(
                InputType.TYPE_CLASS_PHONE
        );

        EditText password =
                field("Password");

        password.setInputType(
                InputType.TYPE_CLASS_TEXT |
                InputType.TYPE_TEXT_VARIATION_PASSWORD
        );

        Button sendOtp =
                button(
                        "SEND OTP",
                        GREEN
                );

        sendOtp.setOnClickListener(v -> {

            String n =
                    name.getText()
                            .toString()
                            .trim();

            String m =
                    mobile.getText()
                            .toString()
                            .trim();

            String p =
                    password.getText()
                            .toString();

            if (n.isEmpty()) {
                toast("Enter your full name.");
                return;
            }

            if (!m.matches("09[0-9]{9}")) {
                toast(
                        "Enter a valid 11-digit mobile number."
                );
                return;
            }

            if (p.length() < 4) {
                toast(
                        "Password must be at least 4 characters."
                );
                return;
            }

            for (User user : users) {

                if (user.mobile.equals(m)) {

                    toast(
                            "Mobile number already registered."
                    );

                    return;
                }
            }

            pendingUser =
                    new User(
                            n,
                            m,
                            p,
                            role
                    );

            pendingOtp =
                    String.format(
                            "%06d",
                            new Random()
                                    .nextInt(1000000)
                    );

            showOtp();
        });

        back(this::showRegisterRoles);
    }

    // =========================
    // OTP
    // =========================

    private void showOtp() {

        setupRoot();

        heading("OTP VERIFICATION");
        subheading("Verify your mobile number");

        info(
                "DEMO OTP: " + pendingOtp
        );

        EditText otp =
                field("Enter 6-digit OTP");

        otp.setInputType(
                InputType.TYPE_CLASS_NUMBER
        );

        Button verify =
                button(
                        "VERIFY OTP",
                        GREEN
                );

        verify.setOnClickListener(v -> {

            if (
                    otp.getText()
                            .toString()
                            .trim()
                            .equals(pendingOtp)
            ) {

                pendingUser.verified = true;

                users.add(pendingUser);

                loggedName =
                        pendingUser.name;

                loggedMobile =
                        pendingUser.mobile;

                loggedRole =
                        pendingUser.role;

                pendingUser = null;
                pendingOtp = "";

                toast(
                        "Registration successful!"
                );

                openDashboard();

            } else {

                toast(
                        "Incorrect OTP."
                );
            }
        });

        back(() -> {

            pendingUser = null;
            pendingOtp = "";

            showRegisterRoles();
        });
    }

    // =========================
    // LOGIN
    // =========================

    private void showLogin() {

        setupRoot();

        heading("LOG IN");
        subheading("Welcome back to Sakay Na");

        EditText mobile =
                field("Mobile Number");

        mobile.setInputType(
                InputType.TYPE_CLASS_PHONE
        );

        EditText password =
                field("Password");

        password.setInputType(
                InputType.TYPE_CLASS_TEXT |
                InputType.TYPE_TEXT_VARIATION_PASSWORD
        );

        Button login =
                button(
                        "LOG IN",
                        BLUE
                );

        login.setOnClickListener(v -> {

            String m =
                    mobile.getText()
                            .toString()
                            .trim();

            String p =
                    password.getText()
                            .toString();

            for (User user : users) {

                if (
                        user.mobile.equals(m) &&
                        user.password.equals(p)
                ) {

                    if (!user.verified) {

                        toast(
                                "Account not verified."
                        );

                        return;
                    }

                    loggedName = user.name;
                    loggedMobile = user.mobile;
                    loggedRole = user.role;

                    openDashboard();

                    return;
                }
            }

            toast(
                    "Invalid mobile number or password."
            );
        });

        Button demoAdmin =
                button(
                        "Demo Admin Login",
                        PURPLE
                );

        demoAdmin.setOnClickListener(v -> {

            mobile.setText("09000000000");
            password.setText("admin123");
        });

        back(this::showHome);
    }

    // =========================
    // DASHBOARD
    // =========================

    private void openDashboard() {

        if (loggedRole.equals("Passenger")) {

            showPassenger();

        } else if (
                loggedRole.equals("Driver")
        ) {

            showDriver();

        } else {

            showAdmin();
        }
    }

    // =========================
    // PASSENGER
    // =========================

    private void showPassenger() {

        setupRoot();

        heading("PASSENGER");
        subheading(
                "Welcome, " + loggedName
        );

        if (rideStatus == 0) {

            EditText pickup =
                    field("📍 Pickup location");

            EditText destination =
                    field("🏁 Destination");

            info(
                    "Estimated Fare: ₱50"
            );

            Button book =
                    button(
                            "🚕 BOOK / REQUEST RIDE",
                            ORANGE
                    );

            book.setOnClickListener(v -> {

                String p =
                        pickup.getText()
                                .toString()
                                .trim();

                String d =
                        destination.getText()
                                .toString()
                                .trim();

                if (p.isEmpty()) {

                    toast(
                            "Enter pickup location."
                    );

                    return;
                }

                if (d.isEmpty()) {

                    toast(
                            "Enter destination."
                    );

                    return;
                }

                ridePassenger =
                        loggedName;

                ridePassengerMobile =
                        loggedMobile;

                ridePickup = p;
                rideDestination = d;

                rideFare = 50;

                rideDriver = "";
                rideDriverMobile = "";

                rideStatus = 1;

                transactions.add(
                        new Transaction(
                                ridePassenger,
                                ridePassengerMobile,
                                "Searching",
                                "",
                                ridePickup,
                                rideDestination,
                                rideFare,
                                "Booking requested"
                        )
                );

                toast(
                        "Ride requested."
                );

                showPassenger();
            });

        } else {

            showPassengerRideStatus();
        }

        Button logout =
                button(
                        "LOG OUT",
                        RED
                );

        logout.setOnClickListener(
                v -> logout()
        );
    }

    private void showPassengerRideStatus() {

        info(
                "BOOKING DETAILS\n\n" +
                "Passenger: " +
                ridePassenger +
                "\n\nPickup: " +
                ridePickup +
                "\nDestination: " +
                rideDestination +
                "\nFare: ₱" +
                rideFare +
                "\n\nSTATUS: " +
                rideStatusText()
        );

        if (!rideDriver.isEmpty()) {

            info(
                    "Driver: " +
                    rideDriver +
                    "\nDriver Mobile: " +
                    rideDriverMobile
            );
        }

        if (rideStatus == 6) {

            Button done =
                    button(
                            "✓ RIDE FINISHED",
                            GREEN
                    );

            done.setEnabled(false);

            Button newRide =
                    button(
                            "BOOK ANOTHER RIDE",
                            ORANGE
                    );

            newRide.setOnClickListener(v -> {

                rideStatus = 0;
                ridePassenger = "";
                ridePassengerMobile = "";
                rideDriver = "";
                rideDriverMobile = "";

                showPassenger();
            });
        }
    }

    // =========================
    // DRIVER
    // =========================

    private void showDriver() {

        setupRoot();

        heading("DRIVER");
        subheading(
                "Welcome, " + loggedName
        );

        if (rideStatus == 0) {

            info(
                    "No active ride request."
            );

        } else if (
                rideStatus == 6
        ) {

            info(
                    "Last ride completed.\n\n" +
                    "Passenger: " +
                    ridePassenger +
                    "\nFare: ₱" +
                    rideFare
            );

        } else {

            info(
                    "NEW RIDE / ACTIVE RIDE\n\n" +
                    "Passenger: " +
                    ridePassenger +
                    "\nPassenger Mobile: " +
                    ridePassengerMobile +
                    "\n\nPickup: " +
                    ridePickup +
                    "\nDestination: " +
                    rideDestination +
                    "\nFare: ₱" +
                    rideFare +
                    "\n\nSTATUS: " +
                    rideStatusText()
            );

            Button action =
                    button(
                            driverActionText(),
                            BLUE
                    );

            action.setOnClickListener(
                    v -> driverAction()
            );
        }

        Button logout =
                button(
                        "LOG OUT",
                        RED
                );

        logout.setOnClickListener(
                v -> logout()
        );
    }

    private String driverActionText() {

        switch (rideStatus) {

            case 1:
                return "✅ ACCEPT RIDE";

            case 2:
                return "🚗 START / ON THE WAY";

            case 3:
                return "📍 MARK AS ARRIVED";

            case 4:
                return "▶ START TRIP";

            case 5:
                return "🏁 FINISH TRIP";

            default:
                return "RIDE FINISHED";
        }
    }

    private void driverAction() {

        if (rideStatus == 1) {

            rideDriver =
                    loggedName;

            rideDriverMobile =
                    loggedMobile;

            rideStatus = 2;

            updateCurrentTransaction(
                    "Driver accepted"
            );

            toast(
                    "Ride accepted."
            );

        } else if (
                rideStatus == 2
        ) {

            rideStatus = 3;

            updateCurrentTransaction(
                    "Driver on the way"
            );

            toast(
                    "Driver is on the way."
            );

        } else if (
                rideStatus == 3
        ) {

            rideStatus = 4;

            updateCurrentTransaction(
                    "Driver arrived"
            );

            toast(
                    "Driver arrived."
            );

        } else if (
                rideStatus == 4
        ) {

            rideStatus = 5;

            updateCurrentTransaction(
                    "Trip started"
            );

            toast(
                    "Trip started."
            );

        } else if (
                rideStatus == 5
        ) {

            rideStatus = 6;

            updateCurrentTransaction(
                    "Trip finished"
            );

            toast(
                    "Trip finished."
            );
        }

        showDriver();
    }

    // =========================
    // ADMIN
    // =========================

    private void showAdmin() {

        setupRoot();

        heading("ADMIN");
        subheading(
                "Sakay Na Management"
        );

        int passengerCount = 0;
        int driverCount = 0;
        int adminCount = 0;

        for (User user : users) {

            if (user.role.equals("Passenger"))
                passengerCount++;

            if (user.role.equals("Driver"))
                driverCount++;

            if (user.role.equals("Admin"))
                adminCount++;
        }

        info(
                "USER COUNTS\n\n" +
                "Passengers: " +
                passengerCount +
                "\nDrivers: " +
                driverCount +
                "\nAdmins: " +
                adminCount
        );

        Button usersButton =
                button(
                        "👥 VIEW ALL USERS",
                        PURPLE
                );

        usersButton.setOnClickListener(
                v -> showAllUsers()
        );

        Button transactionsButton =
                button(
                        "💰 VIEW ALL TRANSACTIONS",
                        ORANGE
                );

        transactionsButton.setOnClickListener(
                v -> showTransactions()
        );

        Button monitor =
                button(
                        "📍 MONITOR CURRENT RIDE",
                        BLUE
                );

        monitor.setOnClickListener(
                v -> showCurrentRide()
        );

        Button reset =
                button(
                        "RESET CURRENT RIDE",
                        RED
                );

        reset.setOnClickListener(v -> {

            rideStatus = 0;

            ridePassenger = "";
            ridePassengerMobile = "";

            rideDriver = "";
            rideDriverMobile = "";

            ridePickup = "";
            rideDestination = "";

            toast(
                    "Current ride reset."
            );

            showAdmin();
        });

        Button logout =
                button(
                        "LOG OUT",
                        RED
                );

        logout.setOnClickListener(
                v -> logout()
        );
    }

    // =========================
    // ADMIN USERS
    // =========================

    private void showAllUsers() {

        setupRoot();

        heading("ALL USERS");

        for (User user : users) {

            info(
                    "NAME: " +
                    user.name +
                    "\nMOBILE: " +
                    user.mobile +
                    "\nROLE: " +
                    user.role +
                    "\nVERIFIED: " +
                    user.verified
            );
        }

        back(this::showAdmin);
    }

    // =========================
    // ADMIN TRANSACTIONS
    // =========================

    private void showTransactions() {

        setupRoot();

        heading("TRANSACTIONS");

        if (transactions.isEmpty()) {

            info(
                    "No transactions yet."
            );

        } else {

            for (
                    int i = 0;
                    i < transactions.size();
                    i++
            ) {

                Transaction t =
                        transactions.get(i);

                info(
                        "TRANSACTION #" +
                        (i + 1) +
                        "\n\nPassenger: " +
                        t.passenger +
                        "\nPassenger Mobile: " +
                        t.passengerMobile +
                        "\nDriver: " +
                        t.driver +
                        "\nDriver Mobile: " +
                        t.driverMobile +
                        "\nPickup: " +
                        t.pickup +
                        "\nDestination: " +
                        t.destination +
                        "\nFare: ₱" +
                        t.fare +
                        "\nStatus: " +
                        t.status
                );
            }
        }

        back(this::showAdmin);
    }

    // =========================
    // ADMIN CURRENT RIDE
    // =========================

    private void showCurrentRide() {

        setupRoot();

        heading("CURRENT RIDE");

        if (rideStatus == 0) {

            info(
                    "No active booking."
            );

        } else {

            info(
                    "PASSENGER\n" +
                    ridePassenger +
                    "\n" +
                    ridePassengerMobile +
                    "\n\nDRIVER\n" +
                    (
                            rideDriver.isEmpty()
                                    ? "Not assigned"
                                    : rideDriver
                    ) +
                    "\n" +
                    (
                            rideDriverMobile.isEmpty()
                                    ? ""
                                    : rideDriverMobile
                    ) +
                    "\n\nPICKUP\n" +
                    ridePickup +
                    "\n\nDESTINATION\n" +
                    rideDestination +
                    "\n\nFARE\n₱" +
                    rideFare +
                    "\n\nSTATUS\n" +
                    rideStatusText()
            );
        }

        back(this::showAdmin);
    }

    // =========================
    // STATUS
    // =========================

    private String rideStatusText() {

        switch (rideStatus) {

            case 1:
                return "BOOKING REQUESTED / SEARCHING FOR DRIVER";

            case 2:
                return "DRIVER ACCEPTED";

            case 3:
                return "DRIVER ON THE WAY";

            case 4:
                return "DRIVER ARRIVED";

            case 5:
                return "TRIP IN PROGRESS";

            case 6:
                return "TRIP FINISHED";

            default:
                return "NO ACTIVE RIDE";
        }
    }

    // =========================
    // TRANSACTION UPDATE
    // =========================

    private void updateCurrentTransaction(
            String status
    ) {

        if (transactions.isEmpty())
            return;

        Transaction t =
                transactions.get(
                        transactions.size() - 1
                );

        t.driver =
                rideDriver.isEmpty()
                        ? "Not assigned"
                        : rideDriver;

        t.driverMobile =
                rideDriverMobile;

        t.status = status;
    }

    // =========================
    // LOGOUT
    // =========================

    private void logout() {

        loggedName = "";
        loggedMobile = "";
        loggedRole = "";

        showHome();
    }

    // =========================
    // ANDROID BACK BUTTON
    // =========================

    @Override
    public void onBackPressed() {

        // Return to home instead of closing immediately.
        showHome();
    }
}


package com.sakyna.app;

import android.app.Activity;
import android.content.Context;
import android.content.SharedPreferences;
import android.graphics.Color;
import android.graphics.Typeface;
import android.os.Bundle;
import android.text.InputType;
import android.view.Gravity;
import android.view.View;
import android.widget.Button;
import android.widget.EditText;
import android.widget.LinearLayout;
import android.widget.TextView;
import android.widget.Toast;

public class MainActivity extends Activity {

    private SharedPreferences prefs;

    private int dp(float value) {
        return (int) (value * getResources().getDisplayMetrics().density + 0.5f);
    }

    private TextView text(String value, float size, int color, boolean bold) {
        TextView t = new TextView(this);
        t.setText(value);
        t.setTextSize(size);
        t.setTextColor(color);
        t.setGravity(Gravity.CENTER);

        if (bold) {
            t.setTypeface(Typeface.DEFAULT, Typeface.BOLD);
        }

        return t;
    }

    private Button button(String title, int color) {
        Button b = new Button(this);
        b.setText(title);
        b.setTextSize(17);
        b.setTextColor(Color.WHITE);
        b.setTypeface(Typeface.DEFAULT, Typeface.BOLD);
        b.setAllCaps(false);
        b.setBackgroundColor(color);

        LinearLayout.LayoutParams p =
                new LinearLayout.LayoutParams(
                        LinearLayout.LayoutParams.MATCH_PARENT,
                        dp(58)
                );

        p.setMargins(dp(20), dp(7), dp(20), dp(7));
        b.setLayoutParams(p);

        return b;
    }

    private EditText field(String hint) {
        EditText e = new EditText(this);
        e.setHint(hint);
        e.setTextSize(16);
        e.setSingleLine(true);
        e.setPadding(dp(15), 0, dp(15), 0);

        LinearLayout.LayoutParams p =
                new LinearLayout.LayoutParams(
                        LinearLayout.LayoutParams.MATCH_PARENT,
                        dp(55)
                );

        p.setMargins(dp(20), dp(6), dp(20), dp(6));
        e.setLayoutParams(p);

        return e;
    }

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);

        prefs = getSharedPreferences(
                "SakayNaAccounts",
                Context.MODE_PRIVATE
        );

        showHome();
    }

    // ============================================================
    // HOME
    // ============================================================

    private void showHome() {
        LinearLayout main = baseLayout();

        main.addView(
                text("🛺", 60, Color.rgb(0, 150, 80), false),
                new LinearLayout.LayoutParams(
                        LinearLayout.LayoutParams.MATCH_PARENT,
                        dp(75)
                )
        );

        main.addView(
                text("SAKAY NA", 32, Color.rgb(0, 140, 70), true),
                titleParams()
        );

        main.addView(
                text(
                        "Your Tricycle Ride App",
                        17,
                        Color.DKGRAY,
                        false
                ),
                titleParams()
        );

        Button login = button(
                "🔐  LOGIN",
                Color.rgb(0, 150, 80)
        );

        login.setOnClickListener(v -> showLogin());
        main.addView(login);

        Button register = button(
                "📝  REGISTER",
                Color.rgb(255, 140, 0)
        );

        register.setOnClickListener(v -> showRegisterRole());
        main.addView(register);

        Button about = button(
                "ℹ️  ABOUT SAKAY NA",
                Color.rgb(120, 80, 180)
        );

        about.setOnClickListener(v ->
                Toast.makeText(
                        this,
                        "Sakay Na - Tricycle Ride Booking",
                        Toast.LENGTH_LONG
                ).show()
        );

        main.addView(about);

        main.addView(
                text(
                        "Safe • Simple • Local",
                        15,
                        Color.GRAY,
                        false
                ),
                titleParams()
        );

        setContentView(main);
    }

    // ============================================================
    // REGISTER ROLE
    // ============================================================

    private void showRegisterRole() {
        LinearLayout layout = baseLayout();

        layout.addView(
                text(
                        "📝 CREATE ACCOUNT",
                        26,
                        Color.rgb(0, 140, 70),
                        true
                ),
                titleParams()
        );

        layout.addView(
                text(
                        "Choose your account type",
                        17,
                        Color.DKGRAY,
                        false
                ),
                titleParams()
        );

        Button passenger = button(
                "🟠  PASSENGER",
                Color.rgb(255, 140, 0)
        );

        passenger.setOnClickListener(
                v -> showRegistration("PASSENGER")
        );

        layout.addView(passenger);

        Button driver = button(
                "🔵  DRIVER",
                Color.rgb(30, 110, 220)
        );

        driver.setOnClickListener(
                v -> showRegistration("DRIVER")
        );

        layout.addView(driver);

        Button admin = button(
                "🟣  ADMIN",
                Color.rgb(125, 70, 180)
        );

        admin.setOnClickListener(
                v -> showRegistration("ADMIN")
        );

        layout.addView(admin);

        Button back = button(
                "← BACK",
                Color.DKGRAY
        );

        back.setOnClickListener(v -> showHome());
        layout.addView(back);

        setContentView(layout);
    }

    // ============================================================
    // REGISTRATION
    // ============================================================

    private void showRegistration(String role) {

        LinearLayout layout = baseLayout();

        int roleColor = Color.rgb(0, 140, 70);

        if (role.equals("PASSENGER")) {
            roleColor = Color.rgb(255, 140, 0);
        }

        if (role.equals("DRIVER")) {
            roleColor = Color.rgb(30, 110, 220);
        }

        if (role.equals("ADMIN")) {
            roleColor = Color.rgb(125, 70, 180);
        }

        layout.addView(
                text(
                        role + " REGISTRATION",
                        25,
                        roleColor,
                        true
                ),
                titleParams()
        );

        EditText name = field("Full Name");
        layout.addView(name);

        EditText phone = field("Phone Number");
        phone.setInputType(InputType.TYPE_CLASS_PHONE);
        layout.addView(phone);

        EditText password = field("Password");
        password.setInputType(
                InputType.TYPE_CLASS_TEXT |
                InputType.TYPE_TEXT_VARIATION_PASSWORD
        );
        layout.addView(password);

        EditText confirm = field("Confirm Password");
        confirm.setInputType(
                InputType.TYPE_CLASS_TEXT |
                InputType.TYPE_TEXT_VARIATION_PASSWORD
        );
        layout.addView(confirm);

        Button continueButton = button(
                "CONTINUE TO OTP →",
                roleColor
        );

        continueButton.setOnClickListener(v -> {

            String fullName =
                    name.getText().toString().trim();

            String phoneNumber =
                    phone.getText().toString().trim();

            String pass =
                    password.getText().toString();

            String confirmPass =
                    confirm.getText().toString();

            if (fullName.isEmpty() ||
                    phoneNumber.isEmpty() ||
                    pass.isEmpty() ||
                    confirmPass.isEmpty()) {

                Toast.makeText(
                        this,
                        "Please complete all fields",
                        Toast.LENGTH_SHORT
                ).show();

                return;
            }

            if (pass.length() < 4) {

                Toast.makeText(
                        this,
                        "Password must be at least 4 characters",
                        Toast.LENGTH_SHORT
                ).show();

                return;
            }

            if (!pass.equals(confirmPass)) {

                Toast.makeText(
                        this,
                        "Passwords do not match",
                        Toast.LENGTH_SHORT
                ).show();

                return;
            }

            if (prefs.contains("phone_" + phoneNumber)) {

                Toast.makeText(
                        this,
                        "This phone number is already registered",
                        Toast.LENGTH_LONG
                ).show();

                return;
            }

            showOtp(
                    role,
                    fullName,
                    phoneNumber,
                    pass
            );
        });

        layout.addView(continueButton);

        Button back = button(
                "← BACK",
                Color.DKGRAY
        );

        back.setOnClickListener(
                v -> showRegisterRole()
        );

        layout.addView(back);

        setContentView(layout);
    }

    // ============================================================
    // OTP
    // ============================================================

    private void showOtp(
            String role,
            String fullName,
            String phoneNumber,
            String password) {

        LinearLayout layout = baseLayout();

        layout.addView(
                text(
                        "📱 OTP VERIFICATION",
                        26,
                        Color.rgb(0, 140, 70),
                        true
                ),
                titleParams()
        );

        layout.addView(
                text(
                        "Demo OTP sent to\n" + phoneNumber,
                        17,
                        Color.DKGRAY,
                        false
                ),
                titleParams()
        );

        EditText otp = field(
                "Enter 6-digit OTP"
        );

        otp.setInputType(
                InputType.TYPE_CLASS_NUMBER
        );

        layout.addView(otp);

        layout.addView(
                text(
                        "Demo OTP: 123456",
                        16,
                        Color.rgb(220, 80, 0),
                        true
                ),
                titleParams()
        );

        Button verify = button(
                "✅ VERIFY & CREATE ACCOUNT",
                Color.rgb(0, 150, 80)
        );

        verify.setOnClickListener(v -> {

            String code =
                    otp.getText().toString().trim();

            if (!code.equals("123456")) {

                Toast.makeText(
                        this,
                        "Incorrect OTP. Use 123456 for demo.",
                        Toast.LENGTH_SHORT
                ).show();

                return;
            }

            prefs.edit()
                    .putString(
                            "phone_" + phoneNumber,
                            password
                    )
                    .putString(
                            "name_" + phoneNumber,
                            fullName
                    )
                    .putString(
                            "role_" + phoneNumber,
                            role
                    )
                    .apply();

            Toast.makeText(
                    this,
                    role + " account created successfully!",
                    Toast.LENGTH_LONG
            ).show();

            showLogin();
        });

        layout.addView(verify);

        Button back = button(
                "← BACK",
                Color.DKGRAY
        );

        back.setOnClickListener(
                v -> showRegistration(role)
        );

        layout.addView(back);

        setContentView(layout);
    }

    // ============================================================
    // LOGIN
    // ============================================================

    private void showLogin() {

        LinearLayout layout = baseLayout();

        layout.addView(
                text(
                        "🔐 LOGIN",
                        28,
                        Color.rgb(0, 140, 70),
                        true
                ),
                titleParams()
        );

        layout.addView(
                text(
                        "Phone number + password",
                        16,
                        Color.DKGRAY,
                        false
                ),
                titleParams()
        );

        EditText phone = field("Phone Number");
        phone.setInputType(
                InputType.TYPE_CLASS_PHONE
        );
        layout.addView(phone);

        EditText password = field("Password");
        password.setInputType(
                InputType.TYPE_CLASS_TEXT |
                InputType.TYPE_TEXT_VARIATION_PASSWORD
        );
        layout.addView(password);

        Button login = button(
                "LOGIN",
                Color.rgb(0, 150, 80)
        );

        login.setOnClickListener(v -> {

            String phoneNumber =
                    phone.getText().toString().trim();

            String enteredPassword =
                    password.getText().toString();

            if (phoneNumber.isEmpty() ||
                    enteredPassword.isEmpty()) {

                Toast.makeText(
                        this,
                        "Enter phone number and password",
                        Toast.LENGTH_SHORT
                ).show();

                return;
            }

            String savedPassword =
                    prefs.getString(
                            "phone_" + phoneNumber,
                            null
                    );

            if (savedPassword == null) {

                Toast.makeText(
                        this,
                        "Account not found. Please register first.",
                        Toast.LENGTH_LONG
                ).show();

                return;
            }

            if (!savedPassword.equals(enteredPassword)) {

                Toast.makeText(
                        this,
                        "Incorrect password",
                        Toast.LENGTH_SHORT
                ).show();

                return;
            }

            String name =
                    prefs.getString(
                            "name_" + phoneNumber,
                            "User"
                    );

            String role =
                    prefs.getString(
                            "role_" + phoneNumber,
                            "PASSENGER"
                    );

            prefs.edit()
                    .putString(
                            "current_phone",
                            phoneNumber
                    )
                    .putString(
                            "current_name",
                            name
                    )
                    .putString(
                            "current_role",
                            role
                    )
                    .apply();

            if (role.equals("PASSENGER")) {
                showPassengerHome(name);
            } else if (role.equals("DRIVER")) {
                showDriverHome(name);
            } else if (role.equals("ADMIN")) {
                showAdminHome(name);
            }
        });

        layout.addView(login);

        Button register = button(
                "CREATE NEW ACCOUNT",
                Color.rgb(255, 140, 0)
        );

        register.setOnClickListener(
                v -> showRegisterRole()
        );

        layout.addView(register);

        Button back = button(
                "← BACK",
                Color.DKGRAY
        );

        back.setOnClickListener(
                v -> showHome()
        );

        layout.addView(back);

        setContentView(layout);
    }

    // ============================================================
    // PASSENGER HOME
    // ============================================================

    private void showPassengerHome(String name) {

        LinearLayout layout = baseLayout();

        layout.addView(
                text(
                        "🟠 PASSENGER",
                        28,
                        Color.rgb(255, 140, 0),
                        true
                ),
                titleParams()
        );

        layout.addView(
                text(
                        "Welcome, " + name,
                        20,
                        Color.DKGRAY,
                        true
                ),
                titleParams()
        );

        Button book = button(
                "🛺  BOOK A RIDE",
                Color.rgb(255, 140, 0)
        );

        book.setOnClickListener(
                v -> showPassengerBooking(name)
        );

        layout.addView(book);

        Button history = button(
                "📋  RIDE HISTORY",
                Color.rgb(0, 140, 180)
        );

        history.setOnClickListener(v ->
                Toast.makeText(
                        this,
                        "Ride history will be added next.",
                        Toast.LENGTH_SHORT
                ).show()
        );

        layout.addView(history);

        Button help = button(
                "🆘  HELP / EMERGENCY",
                Color.rgb(220, 60, 60)
        );

        help.setOnClickListener(v ->
                Toast.makeText(
                        this,
                        "Emergency and support will be added next.",
                        Toast.LENGTH_LONG
                ).show()
        );

        layout.addView(help);

        Button logout = button(
                "LOG OUT",
                Color.DKGRAY
        );

        logout.setOnClickListener(v -> logout());

        layout.addView(logout);

        setContentView(layout);
    }

    // ============================================================
    // PASSENGER BOOKING
    // ============================================================

    private void showPassengerBooking(String name) {

        LinearLayout layout = baseLayout();

        layout.addView(
                text(
                        "🛺 BOOK A RIDE",
                        27,
                        Color.rgb(255, 140, 0),
                        true
                ),
                titleParams()
        );

        EditText pickup =
                field("Pickup Location");

        layout.addView(pickup);

        EditText destination =
                field("Destination");

        layout.addView(destination);

        EditText fare =
                field("Estimated Fare (₱)");

        fare.setInputType(
                InputType.TYPE_CLASS_NUMBER |
                InputType.TYPE_NUMBER_FLAG_DECIMAL
        );

        layout.addView(fare);

        Button request = button(
                "🚕 REQUEST RIDE",
                Color.rgb(255, 140, 0)
        );

        request.setOnClickListener(v -> {

            String p =
                    pickup.getText().toString().trim();

            String d =
                    destination.getText().toString().trim();

            String f =
                    fare.getText().toString().trim();

            if (p.isEmpty() ||
                    d.isEmpty() ||
                    f.isEmpty()) {

                Toast.makeText(
                        this,
                        "Enter pickup, destination and fare",
                        Toast.LENGTH_SHORT
                ).show();

                return;
            }

            saveRide(
                    p,
                    d,
                    f
            );

            showPassengerRideStatus(
                    name,
                    p,
                    d,
                    f
            );
        });

        layout.addView(request);

        Button back = button(
                "← BACK",
                Color.DKGRAY
        );

        back.setOnClickListener(
                v -> showPassengerHome(name)
        );

        layout.addView(back);

        setContentView(layout);
    }

    // ============================================================
    // SAVE RIDE
    // ============================================================

    private void saveRide(
            String pickup,
            String destination,
            String fare) {

        prefs.edit()
                .putString("ride_pickup", pickup)
                .putString("ride_destination", destination)
                .putString("ride_fare", fare)
                .putString("ride_status", "REQUESTED")
                .putString("ride_driver", "Waiting for driver")
                .apply();
    }

    // ============================================================
    // PASSENGER RIDE STATUS
    // ============================================================

    private void showPassengerRideStatus(
            String name,
            String pickup,
            String destination,
            String fare) {

        LinearLayout layout = baseLayout();

        layout.addView(
                text(
                        "🚕 YOUR RIDE",
                        27,
                        Color.rgb(0, 150, 80),
                        true
                ),
                titleParams()
        );

        layout.addView(
                text(
                        "Pickup\n" + pickup,
                        17,
                        Color.DKGRAY,
                        true
                ),
                titleParams()
        );

        layout.addView(
                text(
                        "Destination\n" + destination,
                        17,
                        Color.DKGRAY,
                        true
                ),
                titleParams()
        );

        layout.addView(
                text(
                        "Fare: ₱" + fare,
                        21,
                        Color.rgb(0, 140, 70),
                        true
                ),
                titleParams()
        );

        String status =
                prefs.getString(
                        "ride_status",
                        "REQUESTED"
                );

        String driver =
                prefs.getString(
                        "ride_driver",
                        "Waiting for driver"
                );

        layout.addView(
                text(
                        "STATUS\n" + status,
                        20,
                        Color.rgb(255, 140, 0),
                        true
                ),
                titleParams()
        );

        layout.addView(
                text(
                        "DRIVER\n" + driver,
                        17,
                        Color.DKGRAY,
                        true
                ),
                titleParams()
        );

        Button cancel = button(
                "❌ CANCEL RIDE",
                Color.rgb(210, 60, 60)
        );

        cancel.setOnClickListener(v -> {

            prefs.edit()
                    .putString(
                            "ride_status",
                            "CANCELLED"
                    )
                    .apply();

            Toast.makeText(
                    this,
                    "Ride cancelled",
                    Toast.LENGTH_SHORT
            ).show();

            showPassengerHome(name);
        });

        layout.addView(cancel);

        Button receipt = button(
                "🧾 VIEW RECEIPT",
                Color.rgb(0, 140, 180)
        );

        receipt.setOnClickListener(v ->
                showReceipt(
                        name,
                        pickup,
                        destination,
                        fare
                )
        );

        layout.addView(receipt);

        Button back = button(
                "← PASSENGER HOME",
                Color.DKGRAY
        );

        back.setOnClickListener(
                v -> showPassengerHome(name)
        );

        layout.addView(back);

        setContentView(layout);
    }

    // ============================================================
    // RECEIPT
    // ============================================================

    private void showReceipt(
            String name,
            String pickup,
            String destination,
            String fare) {

        LinearLayout layout = baseLayout();

        layout.addView(
                text(
                        "🧾 RIDE RECEIPT",
                        27,
                        Color.rgb(0, 140, 180),
                        true
                ),
                titleParams()
        );

        layout.addView(
                text(
                        "Passenger: " + name +
                        "\n\nPickup: " + pickup +
                        "\n\nDestination: " + destination +
                        "\n\nFare: ₱" + fare +
                        "\n\nStatus: COMPLETED",
                        18,
                        Color.DKGRAY,
                        true
                ),
                new LinearLayout.LayoutParams(
                        LinearLayout.LayoutParams.MATCH_PARENT,
                        dp(260)
                )
        );

        Button rating = button(
                "⭐ RATE YOUR DRIVER",
                Color.rgb(255, 170, 0)
        );

        rating.setOnClickListener(
                v -> showRating(name)
        );

        layout.addView(rating);

        Button back = button(
                "← PASSENGER HOME",
                Color.DKGRAY
        );

        back.setOnClickListener(
                v -> showPassengerHome(name)
        );

        layout.addView(back);

        setContentView(layout);
    }

    // ============================================================
    // RATING
    // ============================================================

    private void showRating(String name) {

        LinearLayout layout = baseLayout();

        layout.addView(
                text(
                        "⭐ RATE YOUR DRIVER",
                        27,
                        Color.rgb(255, 170, 0),
                        true
                ),
                titleParams()
        );

        layout.addView(
                text(
                        "How was your ride?",
                        18,
                        Color.DKGRAY,
                        true
                ),
                titleParams()
        );

        Button five = button(
                "⭐⭐⭐⭐⭐  Excellent",
                Color.rgb(0, 150, 80)
        );

        five.setOnClickListener(v ->
                ratingSaved(name, 5)
        );

        layout.addView(five);

        Button four = button(
                "⭐⭐⭐⭐  Good",
                Color.rgb(30, 130, 220)
        );

        four.setOnClickListener(v ->
                ratingSaved(name, 4)
        );

        layout.addView(four);

        Button three = button(
                "⭐⭐⭐  Okay",
                Color.rgb(255, 170, 0)
        );

        three.setOnClickListener(v ->
                ratingSaved(name, 3)
        );

        layout.addView(three);

        Button two = button(
                "⭐⭐  Poor",
                Color.rgb(220, 100, 60)
        );

        two.setOnClickListener(v ->
                ratingSaved(name, 2)
        );

        layout.addView(two);

        Button one = button(
                "⭐  Very Poor",
                Color.rgb(210, 60, 60)
        );

        one.setOnClickListener(v ->
                ratingSaved(name, 1)
        );

        layout.addView(one);

        setContentView(layout);
    }

    private void ratingSaved(
            String name,
            int rating) {

        prefs.edit()
                .putInt(
                        "last_rating",
                        rating
                )
                .apply();

        Toast.makeText(
                this,
                "Thank you for your " +
                        rating +
                        "-star rating!",
                Toast.LENGTH_LONG
        ).show();

        showPassengerHome(name);
    }

    // ============================================================
    // DRIVER HOME
    // ============================================================

    private void showDriverHome(String name) {

        LinearLayout layout = baseLayout();

        layout.addView(
                text(
                        "🔵 DRIVER",
                        28,
                        Color.rgb(30, 110, 220),
                        true
                ),
                titleParams()
        );

        layout.addView(
                text(
                        "Welcome, " + name,
                        20,
                        Color.DKGRAY,
                        true
                ),
                titleParams()
        );

        Button online = button(
                "🟢 GO ONLINE",
                Color.rgb(0, 150, 80)
        );

        online.setOnClickListener(v -> {

            prefs.edit()
                    .putBoolean(
                            "driver_online",
                            true
                    )
                    .apply();

            Toast.makeText(
                    this,
                    "Driver is now ONLINE",
                    Toast.LENGTH_LONG
            ).show();
        });

        layout.addView(online);

        Button offline = button(
                "⚫ GO OFFLINE",
                Color.DKGRAY
        );

        offline.setOnClickListener(v -> {

            prefs.edit()
                    .putBoolean(
                            "driver_online",
                            false
                    )
                    .apply();

            Toast.makeText(
                    this,
                    "Driver is now OFFLINE",
                    Toast.LENGTH_LONG
            ).show();
        });

        layout.addView(offline);

        Button bookings = button(
                "📥 BOOKING REQUESTS",
                Color.rgb(30, 110, 220)
        );

        bookings.setOnClickListener(
                v -> showDriverBooking(name)
        );

        layout.addView(bookings);

        Button earnings = button(
                "💰 EARNINGS",
                Color.rgb(0, 150, 80)
        );

        earnings.setOnClickListener(v ->
                Toast.makeText(
                        this,
                        "Earnings will be connected to completed rides.",
                        Toast.LENGTH_SHORT
                ).show()
        );

        layout.addView(earnings);

        Button logout = button(
                "LOG OUT",
                Color.DKGRAY
        );

        logout.setOnClickListener(v -> logout());

        layout.addView(logout);

        setContentView(layout);
    }

    // ============================================================
    // DRIVER BOOKING
    // ============================================================

    private void showDriverBooking(String name) {

        LinearLayout layout = baseLayout();

        layout.addView(
                text(
                        "📥 BOOKING REQUEST",
                        26,
                        Color.rgb(30, 110, 220),
                        true
                ),
                titleParams()
        );

        String pickup =
                prefs.getString(
                        "ride_pickup",
                        "No active ride"
                );

        String destination =
                prefs.getString(
                        "ride_destination",
                        "No active ride"
                );

        String fare =
                prefs.getString(
                        "ride_fare",
                        "0"
                );

        layout.addView(
                text(
                        "Passenger Ride Request\n\n" +
                        "Pickup: " + pickup +
                        "\n\nDestination: " + destination +
                        "\n\nFare: ₱" + fare,
                        18,
                        Color.DKGRAY,
                        true
                ),
                new LinearLayout.LayoutParams(
                        LinearLayout.LayoutParams.MATCH_PARENT,
                        dp(230)
                )
        );

        Button accept = button(
                "✅ ACCEPT RIDE",
                Color.rgb(0, 150, 80)
        );

        accept.setOnClickListener(v -> {

            prefs.edit()
                    .putString(
                            "ride_status",
                            "ACCEPTED"
                    )
                    .putString(
                            "ride_driver",
                            name
                    )
                    .apply();

            showDriverTrip(name);
        });

        layout.addView(accept);

        Button decline = button(
                "❌ DECLINE",
                Color.rgb(210, 60, 60)
        );

        decline.setOnClickListener(v -> {
            prefs.edit()
                    .putString(
                            "ride_status",
                            "REQUESTED"
                    )
                    .apply();

            showDriverHome(name);
        });

        layout.addView(decline);

        Button back = button(
                "← BACK",
                Color.DKGRAY
        );

        back.setOnClickListener(
                v -> showDriverHome(name)
        );

        layout.addView(back);

        setContentView(layout);
    }

    // ============================================================
    // DRIVER TRIP
    // ============================================================

    private void showDriverTrip(String name) {

        LinearLayout layout = baseLayout();

        layout.addView(
                text(
                        "🚕 ACTIVE TRIP",
                        27,
                        Color.rgb(30, 110, 220),
                        true
                ),
                titleParams()
        );

        TextView status = text(
                "Status: " +
                prefs.getString(
                        "ride_status",
                        "ACCEPTED"
                ),
                20,
                Color.rgb(0, 150, 80),
                true
        );

        layout.addView(
                status,
                titleParams()
        );

        Button onWay = button(
                "🚗 DRIVER ON THE WAY",
                Color.rgb(30, 110, 220)
        );

        onWay.setOnClickListener(v -> {

            prefs.edit()
                    .putString(
                            "ride_status",
                            "DRIVER_ON_THE_WAY"
                    )
                    .apply();

            status.setText(
                    "Status: DRIVER_ON_THE_WAY"
            );
        });

        layout.addView(onWay);

        Button arrived = button(
                "📍 ARRIVED",
                Color.rgb(255, 170, 0)
        );

        arrived.setOnClickListener(v -> {

            prefs.edit()
                    .putString(
                            "ride_status",
                            "DRIVER_ARRIVED"
                    )
                    .apply();

            status.setText(
                    "Status: DRIVER_ARRIVED"
            );
        });

        layout.addView(arrived);

        Button start = button(
                "▶ START TRIP",
                Color.rgb(0, 150, 80)
        );

        start.setOnClickListener(v -> {

            prefs.edit()
                    .putString(
                            "ride_status",
                            "IN_PROGRESS"
                    )
                    .apply();

            status.setText(
                    "Status: IN_PROGRESS"
            );
        });

        layout.addView(start);

        Button finish = button(
                "🏁 FINISH TRIP",
                Color.rgb(125, 70, 180)
        );

        finish.setOnClickListener(v -> {

            prefs.edit()
                    .putString(
                            "ride_status",
                            "COMPLETED"
                    )
                    .apply();

            Toast.makeText(
                    this,
                    "Trip completed!",
                    Toast.LENGTH_LONG
            ).show();

            showDriverHome(name);
        });

        layout.addView(finish);

        Button back = button(
                "← DRIVER HOME",
                Color.DKGRAY
        );

        back.setOnClickListener(
                v -> showDriverHome(name)
        );

        layout.addView(back);

        setContentView(layout);
    }

    // ============================================================
    // ADMIN
    // ============================================================

    private void showAdminHome(String name) {

        LinearLayout layout = baseLayout();

        layout.addView(
                text(
                        "🟣 ADMIN DASHBOARD",
                        27,
                        Color.rgb(125, 70, 180),
                        true
                ),
                titleParams()
        );

        layout.addView(
                text(
                        "Welcome, " + name,
                        20,
                        Color.DKGRAY,
                        true
                ),
                titleParams()
        );

        Button users = button(
                "👥 USERS",
                Color.rgb(125, 70, 180)
        );

        users.setOnClickListener(v ->
                Toast.makeText(
                        this,
                        "User management will be added next.",
                        Toast.LENGTH_SHORT
                ).show()
        );

        layout.addView(users);

        Button drivers = button(
                "🚕 DRIVERS",
                Color.rgb(30, 110, 220)
        );

        drivers.setOnClickListener(v ->
                Toast.makeText(
                        this,
                        "Driver management will be added next.",
                        Toast.LENGTH_SHORT
                ).show()
        );

        layout.addView(drivers);

        Button rides = button(
                "🛺 ALL RIDES",
                Color.rgb(0, 150, 80)
        );

        rides.setOnClickListener(v ->
                Toast.makeText(
                        this,
                        "Ride monitoring will be added next.",
                        Toast.LENGTH_SHORT
                ).show()
        );

        layout.addView(rides);

        Button reports = button(
                "📊 REPORTS",
                Color.rgb(0, 140, 180)
        );

        reports.setOnClickListener(v ->
                Toast.makeText(
                        this,
                        "Reports will be added next.",
                        Toast.LENGTH_SHORT
                ).show()
        );

        layout.addView(reports);

        Button logout = button(
                "LOG OUT",
                Color.DKGRAY
        );

        logout.setOnClickListener(v -> logout());

        layout.addView(logout);

        setContentView(layout);
    }

    // ============================================================
    // LOGOUT
    // ============================================================

    private void logout() {

        prefs.edit()
                .remove("current_phone")
                .remove("current_name")
                .remove("current_role")
                .apply();

        showHome();
    }

    // ============================================================
    // COMMON LAYOUT
    // ============================================================

    private LinearLayout baseLayout() {

        LinearLayout layout =
                new LinearLayout(this);

        layout.setOrientation(
                LinearLayout.VERTICAL
        );

        layout.setGravity(
                Gravity.CENTER_HORIZONTAL
        );

        layout.setBackgroundColor(
                Color.rgb(245, 255, 248)
        );

        layout.setPadding(
                0,
                dp(20),
                0,
                dp(20)
        );

        return layout;
    }

    private LinearLayout.LayoutParams titleParams() {

        LinearLayout.LayoutParams p =
                new LinearLayout.LayoutParams(
                        LinearLayout.LayoutParams.MATCH_PARENT,
                        dp(55)
                );

        p.setMargins(
                dp(15),
                dp(5),
                dp(15),
                dp(5)
        );

        return p;
    }

    @Override
    public void onBackPressed() {
        showHome();
    }
}

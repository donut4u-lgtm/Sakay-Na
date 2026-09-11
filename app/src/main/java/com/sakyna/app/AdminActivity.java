
package com.sakyna.app;

import android.app.Activity;
import android.content.Intent;
import android.content.SharedPreferences;
import android.graphics.Color;
import android.graphics.Typeface;
import android.os.Bundle;
import android.view.Gravity;
import android.widget.Button;
import android.widget.LinearLayout;
import android.widget.TextView;
import android.widget.Toast;

import java.util.Map;

public class AdminActivity extends Activity {

    private SharedPreferences prefs;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);

        prefs = getSharedPreferences(
                "SakayNa",
                MODE_PRIVATE
        );

        showAdminHome();
    }

    private TextView makeText(String text, int size) {

        TextView view = new TextView(this);

        view.setText(text);
        view.setTextSize(size);
        view.setTextColor(Color.DKGRAY);

        view.setPadding(
                20,
                20,
                20,
                20
        );

        return view;
    }

    private Button makeButton(String text) {

        Button button = new Button(this);

        button.setText(text);
        button.setTextSize(17);
        button.setAllCaps(false);

        return button;
    }

    private LinearLayout createPage(String titleText) {

        LinearLayout layout =
                new LinearLayout(this);

        layout.setOrientation(
                LinearLayout.VERTICAL
        );

        layout.setPadding(
                25,
                25,
                25,
                25
        );

        TextView title =
                makeText(
                        titleText,
                        27
                );

        title.setTypeface(
                null,
                Typeface.BOLD
        );

        title.setTextColor(
                Color.rgb(
                        130,
                        60,
                        180
                )
        );

        title.setGravity(
                Gravity.CENTER
        );

        layout.addView(title);

        return layout;
    }

    private void addBackButton(
            LinearLayout layout
    ) {

        Button back =
                makeButton(
                        "Back to Dashboard"
                );

        back.setOnClickListener(
                v -> showAdminHome()
        );

        layout.addView(back);
    }

    private void showAdminHome() {

        LinearLayout layout =
                createPage(
                        "SAKAY NA"
                );

        TextView subtitle =
                makeText(
                        "ADMIN DASHBOARD",
                        21
                );

        subtitle.setGravity(
                Gravity.CENTER
        );

        layout.addView(subtitle);

        Button users =
                makeButton(
                        "👥 Manage Users"
                );

        users.setOnClickListener(
                v -> showUsers()
        );

        layout.addView(users);

        Button drivers =
                makeButton(
                        "🚗 Drivers"
                );

        drivers.setOnClickListener(
                v -> showDrivers()
        );

        layout.addView(drivers);

        Button passengers =
                makeButton(
                        "🧑‍🤝‍🧑 Passengers"
                );

        passengers.setOnClickListener(
                v -> showPassengers()
        );

        layout.addView(passengers);

        Button currentRide =
                makeButton(
                        "🚕 Current Ride"
                );

        currentRide.setOnClickListener(
                v -> showCurrentRide()
        );

        layout.addView(currentRide);

        Button reset =
                makeButton(
                        "🔄 Reset Ride"
                );

        reset.setOnClickListener(
                v -> resetRide()
        );

        layout.addView(reset);

        Button reports =
                makeButton(
                        "📊 Reports"
                );

        reports.setOnClickListener(
                v -> showReports()
        );

        layout.addView(reports);

        Button transactions =
                makeButton(
                        "💰 Transactions"
                );

        transactions.setOnClickListener(
                v -> showTransactions()
        );

        layout.addView(transactions);

        Button logout =
                makeButton(
                        "Logout"
                );

        logout.setOnClickListener(
                v -> logout()
        );

        layout.addView(logout);

        setContentView(layout);
    }

    private void showUsers() {

        LinearLayout layout =
                createPage(
                        "MANAGE USERS"
                );

        Map<String, ?> all =
                prefs.getAll();

        int count = 0;

        for (Map.Entry<String, ?> entry :
                all.entrySet()) {

            String key =
                    entry.getKey();

            if (!key.startsWith("phone_")) {
                continue;
            }

            String phone =
                    key.substring(6);

            String name =
                    prefs.getString(
                            "name_" + phone,
                            "Unknown"
                    );

            String role =
                    prefs.getString(
                            "role_" + phone,
                            "Unknown"
                    );

            boolean suspended =
                    prefs.getBoolean(
                            "suspended_" + phone,
                            false
                    );

            TextView user =
                    makeText(
                            "Name: " +
                            name +
                            "\nPhone: " +
                            phone +
                            "\nRole: " +
                            role +
                            "\nStatus: " +
                            (
                                suspended
                                ? "SUSPENDED"
                                : "ACTIVE"
                            ),
                            18
                    );

            user.setTypeface(
                    null,
                    Typeface.BOLD
            );

            user.setTextColor(
                    suspended
                    ? Color.rgb(
                            200,
                            40,
                            40
                    )
                    : Color.rgb(
                            0,
                            120,
                            70
                    )
            );

            layout.addView(user);

            Button control;

            if (suspended) {

                control =
                        makeButton(
                                "RESTORE ACCOUNT"
                        );

            } else {

                control =
                        makeButton(
                                "SUSPEND ACCOUNT"
                        );
            }

            control.setOnClickListener(
                    v -> toggleSuspension(
                            phone
                    )
            );

            layout.addView(control);

            count++;
        }

        if (count == 0) {

            TextView none =
                    makeText(
                            "No registered users yet.",
                            19
                    );

            none.setGravity(
                    Gravity.CENTER
            );

            layout.addView(none);
        }

        addBackButton(layout);

        setContentView(layout);
    }

    private void toggleSuspension(
            String phone
    ) {

        boolean suspended =
                prefs.getBoolean(
                        "suspended_" + phone,
                        false
                );

        prefs.edit()
                .putBoolean(
                        "suspended_" + phone,
                        !suspended
                )
                .apply();

        Toast.makeText(
                this,
                suspended
                ? "Account restored."
                : "Account suspended.",
                Toast.LENGTH_LONG
        ).show();

        showUsers();
    }

    private void showDrivers() {

        LinearLayout layout =
                createPage(
                        "DRIVERS"
                );

        Map<String, ?> all =
                prefs.getAll();

        int count = 0;

        for (Map.Entry<String, ?> entry :
                all.entrySet()) {

            String key =
                    entry.getKey();

            if (!key.startsWith("role_")) {
                continue;
            }

            String phone =
                    key.substring(5);

            String role =
                    prefs.getString(
                            key,
                            ""
                    );

            if (!role.equals("Driver")) {
                continue;
            }

            String name =
                    prefs.getString(
                            "name_" + phone,
                            "Unknown"
                    );

            boolean suspended =
                    prefs.getBoolean(
                            "suspended_" + phone,
                            false
                    );

            TextView driver =
                    makeText(
                            "DRIVER\n\n" +
                            "Name: " +
                            name +
                            "\nPhone: " +
                            phone +
                            "\nStatus: " +
                            (
                                suspended
                                ? "SUSPENDED"
                                : "ACTIVE"
                            ),
                            18
                    );

            layout.addView(driver);

            count++;
        }

        if (count == 0) {

            TextView none =
                    makeText(
                            "No registered drivers yet.",
                            19
                    );

            none.setGravity(
                    Gravity.CENTER
            );

            layout.addView(none);
        }

        addBackButton(layout);

        setContentView(layout);
    }

    private void showPassengers() {

        LinearLayout layout =
                createPage(
                        "PASSENGERS"
                );

        Map<String, ?> all =
                prefs.getAll();

        int count = 0;

        for (Map.Entry<String, ?> entry :
                all.entrySet()) {

            String key =
                    entry.getKey();

            if (!key.startsWith("role_")) {
                continue;
            }

            String phone =
                    key.substring(5);

            String role =
                    prefs.getString(
                            key,
                            ""
                    );

            if (!role.equals("Passenger")) {
                continue;
            }

            String name =
                    prefs.getString(
                            "name_" + phone,
                            "Unknown"
                    );

            boolean suspended =
                    prefs.getBoolean(
                            "suspended_" + phone,
                            false
                    );

            TextView passenger =
                    makeText(
                            "PASSENGER\n\n" +
                            "Name: " +
                            name +
                            "\nPhone: " +
                            phone +
                            "\nStatus: " +
                            (
                                suspended
                                ? "SUSPENDED"
                                : "ACTIVE"
                            ),
                            18
                    );

            layout.addView(
                    passenger
            );

            count++;
        }

        if (count == 0) {

            TextView none =
                    makeText(
                            "No registered passengers yet.",
                            19
                    );

            none.setGravity(
                    Gravity.CENTER
            );

            layout.addView(none);
        }

        addBackButton(layout);

        setContentView(layout);
    }

    private void showCurrentRide() {

        LinearLayout layout =
                createPage(
                        "CURRENT RIDE"
                );

        String pickup =
                prefs.getString(
                        "ride_pickup",
                        ""
                );

        String destination =
                prefs.getString(
                        "ride_destination",
                        ""
                );

        String status =
                prefs.getString(
                        "ride_status",
                        ""
                );

        String driver =
                prefs.getString(
                        "ride_driver",
                        ""
                );

        int fare =
                prefs.getInt(
                        "ride_final_fare",
                        prefs.getInt(
                                "ride_fare",
                                0
                        )
                );

        if (pickup.isEmpty() ||
                destination.isEmpty() ||
                status.isEmpty()) {

            TextView none =
                    makeText(
                            "No current ride.",
                            20
                    );

            none.setGravity(
                    Gravity.CENTER
            );

            layout.addView(none);

        } else {

            TextView ride =
                    makeText(
                            "RIDE INFORMATION\n\n" +
                            "Pickup:\n" +
                            pickup +
                            "\n\n" +
                            "Destination:\n" +
                            destination +
                            "\n\n" +
                            "Fare: ₱" +
                            fare +
                            "\n\n" +
                            "Status:\n" +
                            getReadableStatus(
                                    status
                            ) +
                            "\n\n" +
                            "Driver:\n" +
                            (
                                driver.isEmpty()
                                ? "Not assigned"
                                : driver
                            ),
                            19
                    );

            ride.setTypeface(
                    null,
                    Typeface.BOLD
            );

            layout.addView(ride);
        }

        addBackButton(layout);

        setContentView(layout);
    }

    private void resetRide() {

        prefs.edit()
                .remove("ride_pickup")
                .remove("ride_destination")
                .remove("ride_fare")
                .remove("ride_final_fare")
                .remove("ride_status")
                .remove("ride_driver")
                .remove("ride_rating")
                .apply();

        Toast.makeText(
                this,
                "Current ride has been reset.",
                Toast.LENGTH_LONG
        ).show();

        showAdminHome();
    }

    private void showReports() {

        LinearLayout layout =
                createPage(
                        "REPORTS"
                );

        Map<String, ?> all =
                prefs.getAll();

        int totalUsers = 0;
        int drivers = 0;
        int passengers = 0;
        int suspended = 0;

        for (Map.Entry<String, ?> entry :
                all.entrySet()) {

            String key =
                    entry.getKey();

            if (key.startsWith(
                    "phone_"
            )) {

                totalUsers++;

                String phone =
                        key.substring(6);

                if (prefs.getBoolean(
                        "suspended_" + phone,
                        false
                )) {

                    suspended++;
                }
            }

            if (key.startsWith(
                    "role_"
            )) {

                String role =
                        prefs.getString(
                                key,
                                ""
                        );

                if (role.equals(
                        "Driver"
                )) {

                    drivers++;

                } else if (role.equals(
                        "Passenger"
                )) {

                    passengers++;
                }
            }
        }

        String status =
                prefs.getString(
                        "ride_status",
                        ""
                );

        int fare =
                prefs.getInt(
                        "ride_final_fare",
                        prefs.getInt(
                                "ride_fare",
                                0
                        )
                );

        int completed =
                status.equals(
                        "COMPLETED"
                )
                ? 1
                : 0;

        int cancelled =
                status.equals(
                        "CANCELLED"
                )
                ? 1
                : 0;

        TextView report =
                makeText(
                        "SAKAY NA REPORT\n\n" +
                        "Total Users: " +
                        totalUsers +
                        "\n\n" +
                        "Drivers: " +
                        drivers +
                        "\n\n" +
                        "Passengers: " +
                        passengers +
                        "\n\n" +
                        "Suspended Accounts: " +
                        suspended +
                        "\n\n" +
                        "Completed Rides: " +
                        completed +
                        "\n\n" +
                        "Cancelled Rides: " +
                        cancelled +
                        "\n\n" +
                        "Current Ride Fare: ₱" +
                        fare,
                        20
                );

        layout.addView(report);

        addBackButton(layout);

        setContentView(layout);
    }

    private void showTransactions() {

        LinearLayout layout =
                createPage(
                        "TRANSACTIONS"
                );

        String status =
                prefs.getString(
                        "ride_status",
                        ""
                );

        String pickup =
                prefs.getString(
                        "ride_pickup",
                        ""
                );

        String destination =
                prefs.getString(
                        "ride_destination",
                        ""
                );

        String driver =
                prefs.getString(
                        "ride_driver",
                        ""
                );

        int fare =
                prefs.getInt(
                        "ride_final_fare",
                        prefs.getInt(
                                "ride_fare",
                                0
                        )
                );

        if (pickup.isEmpty() ||
                destination.isEmpty()) {

            TextView none =
                    makeText(
                            "No transactions yet.",
                            20
                    );

            none.setGravity(
                    Gravity.CENTER
            );

            layout.addView(none);

        } else {

            TextView transaction =
                    makeText(
                            "LATEST TRANSACTION\n\n" +
                            "Pickup:\n" +
                            pickup +
                            "\n\n" +
                            "Destination:\n" +
                            destination +
                            "\n\n" +
                            "Driver:\n" +
                            (
                                driver.isEmpty()
                                ? "Not assigned"
                                : driver
                            ) +
                            "\n\n" +
                            "Ride Status:\n" +
                            getReadableStatus(
                                    status
                            ) +
                            "\n\n" +
                            "Final Fare:\n" +
                            "₱" +
                            fare +
                            "\n\n" +
                            "Transaction Status:\n" +
                            (
                                status.equals(
                                        "COMPLETED"
                                )
                                ? "PAID / COMPLETED"
                                : "PENDING"
                            ),
                            19
                    );

            transaction.setTypeface(
                    null,
                    Typeface.BOLD
            );

            layout.addView(transaction);
        }

        addBackButton(layout);

        setContentView(layout);
    }

    private String getReadableStatus(
            String status
    ) {

        if (status.equals(
                "REQUESTED"
        )) {
            return "WAITING FOR DRIVER";
        }

        if (status.equals(
                "ACCEPTED"
        )) {
            return "DRIVER ACCEPTED";
        }

        if (status.equals(
                "DRIVER_ON_THE_WAY"
        )) {
            return "DRIVER ON THE WAY";
        }

        if (status.equals(
                "DRIVER_ARRIVED"
        )) {
            return "DRIVER ARRIVED";
        }

        if (status.equals(
                "IN_PROGRESS"
        )) {
            return "TRIP IN PROGRESS";
        }

        if (status.equals(
                "COMPLETED"
        )) {
            return "COMPLETED";
        }

        if (status.equals(
                "CANCELLED"
        )) {
            return "CANCELLED";
        }

        if (status.equals(
                "DECLINED"
        )) {
            return "DECLINED";
        }

        return status;
    }

    private void logout() {

        prefs.edit()
                .remove("current_phone")
                .remove("current_name")
                .remove("current_role")
                .apply();

        Intent intent =
                new Intent(
                        this,
                        MainActivity.class
                );

        intent.setFlags(
                Intent.FLAG_ACTIVITY_CLEAR_TOP |
                Intent.FLAG_ACTIVITY_NEW_TASK
        );

        startActivity(intent);

        finish();
    }
}

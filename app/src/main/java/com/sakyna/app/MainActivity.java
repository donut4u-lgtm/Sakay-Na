
package com.sakyna.app;

import android.app.Activity;
import android.content.Intent;
import android.graphics.Color;
import android.graphics.Typeface;
import android.os.Bundle;
import android.view.Gravity;
import android.view.View;
import android.widget.Button;
import android.widget.EditText;
import android.widget.LinearLayout;
import android.widget.TextView;
import android.widget.Toast;

public class MainActivity extends Activity {

    private String selectedRole = "";

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        showHome();
    }

    private LinearLayout baseLayout() {
        LinearLayout layout = new LinearLayout(this);
        layout.setOrientation(LinearLayout.VERTICAL);
        layout.setPadding(30, 30, 30, 30);
        layout.setGravity(Gravity.CENTER_HORIZONTAL);
        return layout;
    }

    private TextView makeTitle(String text) {
        TextView title = new TextView(this);
        title.setText(text);
        title.setTextSize(32);
        title.setTypeface(null, Typeface.BOLD);
        title.setTextColor(Color.rgb(0, 150, 80));
        title.setGravity(Gravity.CENTER);
        title.setPadding(10, 20, 10, 20);
        return title;
    }

    private TextView makeText(String text, int size) {
        TextView view = new TextView(this);
        view.setText(text);
        view.setTextSize(size);
        view.setTextColor(Color.DKGRAY);
        view.setPadding(10, 15, 10, 15);
        return view;
    }

    private EditText makeInput(String hint) {
        EditText input = new EditText(this);
        input.setHint(hint);
        input.setTextSize(17);
        input.setPadding(20, 15, 20, 15);

        LinearLayout.LayoutParams params =
                new LinearLayout.LayoutParams(
                        LinearLayout.LayoutParams.MATCH_PARENT,
                        LinearLayout.LayoutParams.WRAP_CONTENT
                );

        params.setMargins(0, 8, 0, 8);
        input.setLayoutParams(params);

        return input;
    }

    private Button makeButton(String text) {
        Button button = new Button(this);
        button.setText(text);
        button.setTextSize(17);
        button.setAllCaps(false);

        LinearLayout.LayoutParams params =
                new LinearLayout.LayoutParams(
                        LinearLayout.LayoutParams.MATCH_PARENT,
                        LinearLayout.LayoutParams.WRAP_CONTENT
                );

        params.setMargins(0, 8, 0, 8);
        button.setLayoutParams(params);

        return button;
    }

    private void showHome() {

        LinearLayout layout = baseLayout();

        TextView title = makeTitle("SAKAY NA");
        layout.addView(title);

        TextView subtitle =
                makeText("Tricycle Ride Booking", 20);
        subtitle.setGravity(Gravity.CENTER);
        layout.addView(subtitle);

        TextView welcome =
                makeText("Your local ride, made easy.", 17);
        welcome.setGravity(Gravity.CENTER);
        layout.addView(welcome);

        Button login = makeButton("LOGIN");
        login.setOnClickListener(v -> showLogin());
        layout.addView(login);

        Button register = makeButton("REGISTER");
        register.setOnClickListener(v -> showRegisterRole());
        layout.addView(register);

        Button about = makeButton("About Sakay Na");
        about.setOnClickListener(v ->
                Toast.makeText(
                        this,
                        "Sakay Na - Tricycle Ride Booking",
                        Toast.LENGTH_LONG
                ).show()
        );
        layout.addView(about);

        setContentView(layout);
    }

    private void showRegisterRole() {

        LinearLayout layout = baseLayout();

        TextView title = makeTitle("REGISTER");
        layout.addView(title);

        TextView text =
                makeText("Choose your account type", 19);
        text.setGravity(Gravity.CENTER);
        layout.addView(text);

        Button passenger = makeButton("Passenger");
        passenger.setOnClickListener(v -> {
            selectedRole = "Passenger";
            showRegistration();
        });
        layout.addView(passenger);

        Button driver = makeButton("Driver");
        driver.setOnClickListener(v -> {
            selectedRole = "Driver";
            showRegistration();
        });
        layout.addView(driver);

        Button admin = makeButton("Admin");
        admin.setOnClickListener(v -> {
            selectedRole = "Admin";
            showRegistration();
        });
        layout.addView(admin);

        Button back = makeButton("Back");
        back.setOnClickListener(v -> showHome());
        layout.addView(back);

        setContentView(layout);
    }

    private void showRegistration() {

        LinearLayout layout = baseLayout();

        TextView title = makeTitle("REGISTER");
        layout.addView(title);

        TextView role =
                makeText("Account type: " + selectedRole, 18);
        role.setGravity(Gravity.CENTER);
        layout.addView(role);

        EditText name = makeInput("Full Name");
        layout.addView(name);

        EditText phone = makeInput("Phone Number");
        phone.setInputType(
                android.text.InputType.TYPE_CLASS_PHONE
        );
        layout.addView(phone);

        EditText password = makeInput("Password");
        password.setInputType(
                android.text.InputType.TYPE_CLASS_TEXT |
                android.text.InputType.TYPE_TEXT_VARIATION_PASSWORD
        );
        layout.addView(password);

        EditText confirmPassword =
                makeInput("Confirm Password");

        confirmPassword.setInputType(
                android.text.InputType.TYPE_CLASS_TEXT |
                android.text.InputType.TYPE_TEXT_VARIATION_PASSWORD
        );

        layout.addView(confirmPassword);

        Button continueButton =
                makeButton("Continue to OTP");

        continueButton.setOnClickListener(v -> {

            String fullName =
                    name.getText().toString().trim();

            String phoneNumber =
                    phone.getText().toString().trim();

            String pass =
                    password.getText().toString();

            String confirm =
                    confirmPassword.getText().toString();

            if (fullName.isEmpty()) {
                Toast.makeText(
                        this,
                        "Please enter your full name",
                        Toast.LENGTH_SHORT
                ).show();
                return;
            }

            if (phoneNumber.isEmpty()) {
                Toast.makeText(
                        this,
                        "Please enter your phone number",
                        Toast.LENGTH_SHORT
                ).show();
                return;
            }

            if (pass.isEmpty()) {
                Toast.makeText(
                        this,
                        "Please enter a password",
                        Toast.LENGTH_SHORT
                ).show();
                return;
            }

            if (!pass.equals(confirm)) {
                Toast.makeText(
                        this,
                        "Passwords do not match",
                        Toast.LENGTH_SHORT
                ).show();
                return;
            }

            showOtp(
                    fullName,
                    phoneNumber,
                    pass
            );
        });

        layout.addView(continueButton);

        Button back = makeButton("Back");
        back.setOnClickListener(v -> showRegisterRole());
        layout.addView(back);

        setContentView(layout);
    }

    private void showOtp(
            String fullName,
            String phone,
            String password
    ) {

        LinearLayout layout = baseLayout();

        TextView title = makeTitle("VERIFY PHONE");
        layout.addView(title);

        TextView info =
                makeText(
                        "Demo OTP: 123456\n\nEnter the OTP sent to your phone.",
                        18
                );

        info.setGravity(Gravity.CENTER);
        layout.addView(info);

        EditText otp = makeInput("Enter OTP");
        otp.setInputType(
                android.text.InputType.TYPE_CLASS_NUMBER
        );
        layout.addView(otp);

        Button verify = makeButton("VERIFY & CREATE ACCOUNT");

        verify.setOnClickListener(v -> {

            String enteredOtp =
                    otp.getText().toString().trim();

            if (!enteredOtp.equals("123456")) {
                Toast.makeText(
                        this,
                        "Invalid OTP. Use 123456 for this demo.",
                        Toast.LENGTH_LONG
                ).show();
                return;
            }

            getSharedPreferences("SakayNa", MODE_PRIVATE)
                    .edit()
                    .putString("phone_" + phone, password)
                    .putString("name_" + phone, fullName)
                    .putString("role_" + phone, selectedRole)
                    .apply();

            Toast.makeText(
                    this,
                    "Account created successfully!",
                    Toast.LENGTH_LONG
            ).show();

            showLogin();
        });

        layout.addView(verify);

        Button back = makeButton("Back");
        back.setOnClickListener(v -> showRegistration());
        layout.addView(back);

        setContentView(layout);
    }

    private void showLogin() {

        LinearLayout layout = baseLayout();

        TextView title = makeTitle("LOGIN");
        layout.addView(title);

        EditText phone = makeInput("Phone Number");
        phone.setInputType(
                android.text.InputType.TYPE_CLASS_PHONE
        );
        layout.addView(phone);

        EditText password = makeInput("Password");
        password.setInputType(
                android.text.InputType.TYPE_CLASS_TEXT |
                android.text.InputType.TYPE_TEXT_VARIATION_PASSWORD
        );
        layout.addView(password);

        Button login = makeButton("LOGIN");

        login.setOnClickListener(v -> {

            String phoneNumber =
                    phone.getText().toString().trim();

            String pass =
                    password.getText().toString();

            if (phoneNumber.isEmpty() ||
                    pass.isEmpty()) {

                Toast.makeText(
                        this,
                        "Please enter phone number and password",
                        Toast.LENGTH_SHORT
                ).show();

                return;
            }

            String savedPassword =
                    getSharedPreferences(
                            "SakayNa",
                            MODE_PRIVATE
                    ).getString(
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

            if (!savedPassword.equals(pass)) {

                Toast.makeText(
                        this,
                        "Incorrect password",
                        Toast.LENGTH_SHORT
                ).show();

                return;
            }

            String name =
                    getSharedPreferences(
                            "SakayNa",
                            MODE_PRIVATE
                    ).getString(
                            "name_" + phoneNumber,
                            ""
                    );

            String role =
                    getSharedPreferences(
                            "SakayNa",
                            MODE_PRIVATE
                    ).getString(
                            "role_" + phoneNumber,
                            ""
                    );

            getSharedPreferences(
                    "SakayNa",
                    MODE_PRIVATE
            )
                    .edit()
                    .putString("current_phone", phoneNumber)
                    .putString("current_name", name)
                    .putString("current_role", role)
                    .apply();

            openRoleActivity(role);
        });

        layout.addView(login);

        Button register =
                makeButton("Create New Account");

        register.setOnClickListener(
                v -> showRegisterRole()
        );

        layout.addView(register);

        Button back = makeButton("Back");
        back.setOnClickListener(v -> showHome());
        layout.addView(back);

        setContentView(layout);
    }

    private void openRoleActivity(String role) {

        if (role.equals("Passenger")) {

            Intent intent =
                    new Intent(
                            this,
                            PassengerActivity.class
                    );

            startActivity(intent);
            finish();

        } else if (role.equals("Driver")) {

            Intent intent =
                    new Intent(
                            this,
                            DriverActivity.class
                    );

            startActivity(intent);
            finish();

        } else if (role.equals("Admin")) {

            Intent intent =
                    new Intent(
                            this,
                            AdminActivity.class
                    );

            startActivity(intent);
            finish();

        } else {

            Toast.makeText(
                    this,
                    "Unknown account type",
                    Toast.LENGTH_LONG
            ).show();

            showHome();
        }
    }

    @Override
    public void onBackPressed() {
        showHome();
    }
}

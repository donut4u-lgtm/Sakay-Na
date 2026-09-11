
package com.sakyna.app;

import android.app.Activity;
import android.os.Bundle;
import android.graphics.Color;
import android.graphics.Typeface;
import android.view.Gravity;
import android.view.View;
import android.widget.Button;
import android.widget.EditText;
import android.widget.LinearLayout;
import android.widget.TextView;
import android.widget.Toast;

public class MainActivity extends Activity {

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

        p.setMargins(dp(25), dp(7), dp(25), dp(7));
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

        p.setMargins(dp(25), dp(6), dp(25), dp(6));
        e.setLayoutParams(p);

        return e;
    }

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        showHome();
    }

    private void showHome() {
        LinearLayout main = new LinearLayout(this);
        main.setOrientation(LinearLayout.VERTICAL);
        main.setGravity(Gravity.CENTER_HORIZONTAL);
        main.setBackgroundColor(Color.rgb(245, 255, 248));
        main.setPadding(0, dp(30), 0, dp(20));

        main.addView(
                text("🛺", 60, Color.rgb(0, 150, 80), false),
                new LinearLayout.LayoutParams(
                        LinearLayout.LayoutParams.MATCH_PARENT,
                        dp(75)
                )
        );

        main.addView(
                text("SAKAY NA", 32, Color.rgb(0, 140, 70), true),
                new LinearLayout.LayoutParams(
                        LinearLayout.LayoutParams.MATCH_PARENT,
                        dp(55)
                )
        );

        main.addView(
                text("Your Tricycle Ride App", 17, Color.DKGRAY, false),
                new LinearLayout.LayoutParams(
                        LinearLayout.LayoutParams.MATCH_PARENT,
                        dp(45)
                )
        );

        Button login = button("🔐  LOGIN", Color.rgb(0, 150, 80));
        login.setOnClickListener(v -> showLogin());
        main.addView(login);

        Button register = button("📝  REGISTER", Color.rgb(255, 140, 0));
        register.setOnClickListener(v -> showRegisterRole());
        main.addView(register);

        Button about = button("ℹ️  ABOUT SAKAY NA", Color.rgb(120, 80, 180));
        about.setOnClickListener(v ->
                Toast.makeText(
                        this,
                        "Sakay Na - Tricycle Ride Booking",
                        Toast.LENGTH_LONG
                ).show()
        );
        main.addView(about);

        main.addView(
                text("Safe • Simple • Local", 15, Color.GRAY, false),
                new LinearLayout.LayoutParams(
                        LinearLayout.LayoutParams.MATCH_PARENT,
                        dp(50)
                )
        );

        setContentView(main);
    }

    private void showRegisterRole() {
        LinearLayout layout = baseLayout();

        layout.addView(
                text("📝 CREATE ACCOUNT", 26, Color.rgb(0, 140, 70), true),
                titleParams()
        );

        layout.addView(
                text("Choose your account type", 17, Color.DKGRAY, false),
                titleParams()
        );

        Button passenger =
                button("🟠  PASSENGER", Color.rgb(255, 140, 0));

        passenger.setOnClickListener(v ->
                showRegistration("PASSENGER")
        );

        layout.addView(passenger);

        Button driver =
                button("🔵  DRIVER", Color.rgb(30, 110, 220));

        driver.setOnClickListener(v ->
                showRegistration("DRIVER")
        );

        layout.addView(driver);

        Button admin =
                button("🟣  ADMIN", Color.rgb(125, 70, 180));

        admin.setOnClickListener(v ->
                showRegistration("ADMIN")
        );

        layout.addView(admin);

        Button back = button("← BACK", Color.DKGRAY);
        back.setOnClickListener(v -> showHome());
        layout.addView(back);

        setContentView(layout);
    }

    private void showRegistration(String role) {
        LinearLayout layout = baseLayout();

        int roleColor = Color.rgb(0, 140, 70);

        if (role.equals("PASSENGER")) {
            roleColor = Color.rgb(255, 140, 0);
        } else if (role.equals("DRIVER")) {
            roleColor = Color.rgb(30, 110, 220);
        } else if (role.equals("ADMIN")) {
            roleColor = Color.rgb(125, 70, 180);
        }

        layout.addView(
                text(role + " REGISTRATION", 25, roleColor, true),
                titleParams()
        );

        EditText name = field("Full Name");
        layout.addView(name);

        EditText phone = field("Phone Number");
        phone.setInputType(
                android.text.InputType.TYPE_CLASS_PHONE
        );
        layout.addView(phone);

        EditText password = field("Password");
        password.setInputType(
                android.text.InputType.TYPE_CLASS_TEXT |
                android.text.InputType.TYPE_TEXT_VARIATION_PASSWORD
        );
        layout.addView(password);

        EditText confirm = field("Confirm Password");
        confirm.setInputType(
                android.text.InputType.TYPE_CLASS_TEXT |
                android.text.InputType.TYPE_TEXT_VARIATION_PASSWORD
        );
        layout.addView(confirm);

        Button continueButton =
                button("CONTINUE TO OTP →", roleColor);

        continueButton.setOnClickListener(v -> {

            String fullName = name.getText().toString().trim();
            String phoneNumber = phone.getText().toString().trim();
            String pass = password.getText().toString();
            String confirmPass = confirm.getText().toString();

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

            if (!pass.equals(confirmPass)) {
                Toast.makeText(
                        this,
                        "Passwords do not match",
                        Toast.LENGTH_SHORT
                ).show();
                return;
            }

            showOtp(role, fullName, phoneNumber);
        });

        layout.addView(continueButton);

        Button back = button("← BACK", Color.DKGRAY);
        back.setOnClickListener(v -> showRegisterRole());
        layout.addView(back);

        setContentView(layout);
    }

    private void showOtp(
            String role,
            String fullName,
            String phoneNumber) {

        LinearLayout layout = baseLayout();

        layout.addView(
                text("📱 OTP VERIFICATION",
                        26,
                        Color.rgb(0, 140, 70),
                        true),
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

        EditText otp = field("Enter 6-digit OTP");
        otp.setInputType(
                android.text.InputType.TYPE_CLASS_NUMBER
        );
        layout.addView(otp);

        TextView demo = text(
                "Demo OTP: 123456",
                16,
                Color.rgb(220, 80, 0),
                true
        );

        demo.setPadding(0, dp(10), 0, dp(10));
        layout.addView(demo);

        Button verify =
                button("✅ VERIFY & CREATE ACCOUNT",
                        Color.rgb(0, 150, 80));

        verify.setOnClickListener(v -> {

            String code = otp.getText().toString().trim();

            if (!code.equals("123456")) {
                Toast.makeText(
                        this,
                        "Incorrect OTP. Use 123456 for demo.",
                        Toast.LENGTH_SHORT
                ).show();
                return;
            }

            Toast.makeText(
                    this,
                    role + " account created!",
                    Toast.LENGTH_LONG
            ).show();

            showLogin();
        });

        layout.addView(verify);

        Button back = button("← BACK", Color.DKGRAY);
        back.setOnClickListener(v -> showRegistration(role));
        layout.addView(back);

        setContentView(layout);
    }

    private void showLogin() {
        LinearLayout layout = baseLayout();

        layout.addView(
                text("🔐 LOGIN", 28, Color.rgb(0, 140, 70), true),
                titleParams()
        );

        layout.addView(
                text(
                        "Login using your phone number and password",
                        16,
                        Color.DKGRAY,
                        false
                ),
                titleParams()
        );

        EditText phone = field("Phone Number");
        phone.setInputType(
                android.text.InputType.TYPE_CLASS_PHONE
        );
        layout.addView(phone);

        EditText password = field("Password");
        password.setInputType(
                android.text.InputType.TYPE_CLASS_TEXT |
                android.text.InputType.TYPE_TEXT_VARIATION_PASSWORD
        );
        layout.addView(password);

        Button login =
                button("LOGIN", Color.rgb(0, 150, 80));

        login.setOnClickListener(v -> {

            if (phone.getText().toString().trim().isEmpty() ||
                    password.getText().toString().isEmpty()) {

                Toast.makeText(
                        this,
                        "Enter phone number and password",
                        Toast.LENGTH_SHORT
                ).show();
                return;
            }

            Toast.makeText(
                    this,
                    "Login system will be connected next.",
                    Toast.LENGTH_SHORT
            ).show();
        });

        layout.addView(login);

        Button register =
                button("CREATE NEW ACCOUNT",
                        Color.rgb(255, 140, 0));

        register.setOnClickListener(v -> showRegisterRole());
        layout.addView(register);

        Button back = button("← BACK", Color.DKGRAY);
        back.setOnClickListener(v -> showHome());
        layout.addView(back);

        setContentView(layout);
    }

    private LinearLayout baseLayout() {
        LinearLayout layout = new LinearLayout(this);
        layout.setOrientation(LinearLayout.VERTICAL);
        layout.setGravity(Gravity.CENTER_HORIZONTAL);
        layout.setBackgroundColor(Color.rgb(245, 255, 248));
        layout.setPadding(0, dp(25), 0, dp(20));
        return layout;
    }

    private LinearLayout.LayoutParams titleParams() {
        LinearLayout.LayoutParams p =
                new LinearLayout.LayoutParams(
                        LinearLayout.LayoutParams.MATCH_PARENT,
                        dp(55)
                );

        p.setMargins(dp(15), dp(5), dp(15), dp(5));
        return p;
    }

    @Override
    public void onBackPressed() {
        showHome();
    }
}

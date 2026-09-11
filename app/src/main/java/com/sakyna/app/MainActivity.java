
package com.sakyna.app;

import android.app.Activity;
import android.os.Bundle;
import android.graphics.Color;
import android.graphics.Typeface;
import android.view.Gravity;
import android.view.View;
import android.widget.Button;
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

        p.setMargins(dp(28), dp(8), dp(28), dp(8));
        b.setLayoutParams(p);

        return b;
    }

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);

        // Main background
        LinearLayout main = new LinearLayout(this);
        main.setOrientation(LinearLayout.VERTICAL);
        main.setGravity(Gravity.CENTER_HORIZONTAL);
        main.setBackgroundColor(Color.rgb(245, 255, 248));
        main.setPadding(0, dp(35), 0, dp(20));

        // Logo
        TextView logo = text(
                "🛺",
                64,
                Color.rgb(0, 150, 80),
                false
        );

        main.addView(
                logo,
                new LinearLayout.LayoutParams(
                        LinearLayout.LayoutParams.MATCH_PARENT,
                        dp(80)
                )
        );

        // App name
        TextView title = text(
                "SAKAY NA",
                32,
                Color.rgb(0, 140, 70),
                true
        );

        main.addView(
                title,
                new LinearLayout.LayoutParams(
                        LinearLayout.LayoutParams.MATCH_PARENT,
                        dp(55)
                )
        );

        // Subtitle
        TextView subtitle = text(
                "Your Tricycle Ride App",
                17,
                Color.DKGRAY,
                false
        );

        LinearLayout.LayoutParams subtitleParams =
                new LinearLayout.LayoutParams(
                        LinearLayout.LayoutParams.MATCH_PARENT,
                        dp(45)
                );

        subtitleParams.setMargins(0, 0, 0, dp(25));
        main.addView(subtitle, subtitleParams);

        // Login button
        Button login = button(
                "🔐  LOGIN",
                Color.rgb(0, 150, 80)
        );

        login.setOnClickListener(v ->
                Toast.makeText(
                        MainActivity.this,
                        "Login screen coming next",
                        Toast.LENGTH_SHORT
                ).show()
        );

        main.addView(login);

        // Register button
        Button register = button(
                "📝  REGISTER",
                Color.rgb(255, 140, 0)
        );

        register.setOnClickListener(v ->
                Toast.makeText(
                        MainActivity.this,
                        "Choose Passenger, Driver, or Admin",
                        Toast.LENGTH_SHORT
                ).show()
        );

        main.addView(register);

        // About button
        Button about = button(
                "ℹ️  ABOUT SAKAY NA",
                Color.rgb(120, 80, 180)
        );

        about.setOnClickListener(v ->
                Toast.makeText(
                        MainActivity.this,
                        "Sakay Na - Tricycle Ride Booking",
                        Toast.LENGTH_LONG
                ).show()
        );

        main.addView(about);

        // Bottom message
        TextView footer = text(
                "Safe • Simple • Local",
                15,
                Color.GRAY,
                false
        );

        LinearLayout.LayoutParams footerParams =
                new LinearLayout.LayoutParams(
                        LinearLayout.LayoutParams.MATCH_PARENT,
                        dp(50)
                );

        footerParams.setMargins(0, dp(25), 0, 0);
        main.addView(footer, footerParams);

        setContentView(main);
    }
}

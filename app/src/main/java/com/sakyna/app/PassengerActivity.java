package com.sakyna.app;

import android.app.Activity;
import android.content.Intent;
import android.os.Bundle;
import android.graphics.Color;
import android.graphics.Typeface;
import android.view.Gravity;
import android.view.View;
import android.widget.Button;
import android.widget.LinearLayout;
import android.widget.TextView;
import android.widget.Toast;

public class PassengerActivity extends Activity {

    private LinearLayout layout;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        showPassengerHome();
    }

    private TextView text(String value, int size) {
        TextView t = new TextView(this);
        t.setText(value);
        t.setTextSize(size);
        t.setTextColor(Color.DKGRAY);
        t.setPadding(20, 20, 20, 20);
        return t;
    }

    private Button button(String value) {
        Button b = new Button(this);
        b.setText(value);
        b.setTextSize(17);
        b.setAllCaps(false);
        return b;
    }

    private void showPassengerHome() {
        layout = new LinearLayout(this);
        layout.setOrientation(LinearLayout.VERTICAL);
        layout.setPadding(25, 25, 25, 25);
        layout.setGravity(Gravity.CENTER_HORIZONTAL);

        TextView title = text("SAKAY NA", 30);
        title.setTypeface(null, Typeface.BOLD);
        title.setTextColor(Color.rgb(0, 150, 80));
        title.setGravity(Gravity.CENTER);

        layout.addView(title);

        TextView subtitle = text("Passenger", 20);
        subtitle.setGravity(Gravity.CENTER);
        layout.addView(subtitle);

        Button book = button("🚕 Book a Ride");
        book.setOnClickListener(v ->
                Toast.makeText(this, "Booking screen coming next", Toast.LENGTH_SHORT).show()
        );
        layout.addView(book);

        Button history = button("📋 Ride History");
        history.setOnClickListener(v ->
                Toast.makeText(this, "Ride history coming next", Toast.LENGTH_SHORT).show()
        );
        layout.addView(history);

        Button help = button("🆘 Help / Emergency");
        help.setOnClickListener(v ->
                Toast.makeText(this, "Emergency feature coming next", Toast.LENGTH_SHORT).show()
        );
        layout.addView(help);

        Button logout = button("Logout");
        logout.setOnClickListener(v -> {
            getSharedPreferences("SakayNa", MODE_PRIVATE)
                    .edit()
                    .remove("current_phone")
                    .remove("current_name")
                    .remove("current_role")
                    .apply();

            Intent intent = new Intent(this, MainActivity.class);
            intent.setFlags(Intent.FLAG_ACTIVITY_CLEAR_TOP |
                    Intent.FLAG_ACTIVITY_NEW_TASK);
            startActivity(intent);
            finish();
        });
        layout.addView(logout);

        setContentView(layout);
    }
}

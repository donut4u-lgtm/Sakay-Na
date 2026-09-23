package com.sakyna.app;

import android.app.Activity;
import android.content.Intent;
import android.graphics.Color;
import android.os.Bundle;
import android.provider.Settings;
import android.view.Gravity;
import android.widget.Button;
import android.widget.LinearLayout;
import android.widget.Switch;
import android.widget.TextView;

public class NotificationSettingsActivity extends Activity {

    private Switch notificationSwitch;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);

        buildScreen();
    }

    private void buildScreen() {
        LinearLayout root = new LinearLayout(this);
        root.setOrientation(LinearLayout.VERTICAL);
        root.setPadding(28, 28, 28, 35);
        root.setGravity(Gravity.CENTER_HORIZONTAL);
        root.setBackgroundColor(Color.rgb(248, 250, 252));

        TextView title = new TextView(this);
        title.setText("🔔 SAKAY NA NOTIFICATIONS");
        title.setTextSize(26);
        title.setTypeface(null, android.graphics.Typeface.BOLD);
        title.setTextColor(Color.rgb(0, 70, 120));
        title.setGravity(Gravity.CENTER);
        title.setPadding(10, 20, 10, 25);
        root.addView(title, full());

        TextView info = new TextView(this);
        info.setText(
                "Control notifications displayed by Sakay Na on this phone.\n\n"
                        + "When ON, Sakay Na can display ride notifications.\n"
                        + "When OFF, Sakay Na will not display its local notifications."
        );
        info.setTextSize(17);
        info.setTextColor(Color.DKGRAY);
        info.setGravity(Gravity.CENTER);
        info.setPadding(5, 5, 5, 20);
        root.addView(info, full());

        notificationSwitch = new Switch(this);
        notificationSwitch.setText("🔔 Notifications ON");
        notificationSwitch.setTextSize(19);
        notificationSwitch.setTextColor(Color.rgb(0, 120, 70));
        notificationSwitch.setChecked(
                SakayNaNotificationHelper.areNotificationsEnabled(this)
        );

        notificationSwitch.setOnCheckedChangeListener(
                (buttonView, isChecked) -> {

                    SakayNaNotificationHelper.setNotificationsEnabled(
                            this,
                            isChecked
                    );

                    if (isChecked) {
                        notificationSwitch.setText(
                                "🔔 Notifications ON"
                        );
                        notificationSwitch.setTextColor(
                                Color.rgb(0, 120, 70)
                        );
                    } else {
                        notificationSwitch.setText(
                                "🔕 Notifications OFF"
                        );
                        notificationSwitch.setTextColor(
                                Color.rgb(190, 45, 45)
                        );
                    }
                }
        );

        if (!notificationSwitch.isChecked()) {
            notificationSwitch.setText("🔕 Notifications OFF");
            notificationSwitch.setTextColor(
                    Color.rgb(190, 45, 45)
            );
        }

        LinearLayout.LayoutParams switchParams = full();
        switchParams.topMargin = 10;
        switchParams.bottomMargin = 25;

        root.addView(
                notificationSwitch,
                switchParams
        );

        Button phoneSettings = new Button(this);
        phoneSettings.setText(
                "📱 PHONE NOTIFICATION SETTINGS"
        );
        phoneSettings.setTextSize(16);
        phoneSettings.setTextColor(Color.WHITE);
        phoneSettings.setBackgroundColor(
                Color.rgb(0, 120, 200)
        );

        phoneSettings.setOnClickListener(
                v -> openPhoneNotificationSettings()
        );

        root.addView(
                phoneSettings,
                full()
        );

        Button back = new Button(this);
        back.setText("← BACK");
        back.setTextSize(16);
        back.setOnClickListener(v -> finish());

        root.addView(
                back,
                full()
        );

        setContentView(root);
    }

    private void openPhoneNotificationSettings() {

        Intent intent = new Intent(
                Settings.ACTION_APP_NOTIFICATION_SETTINGS
        );

        intent.putExtra(
                Settings.EXTRA_APP_PACKAGE,
                getPackageName()
        );

        startActivity(intent);
    }

    private LinearLayout.LayoutParams full() {

        return new LinearLayout.LayoutParams(
                LinearLayout.LayoutParams.MATCH_PARENT,
                LinearLayout.LayoutParams.WRAP_CONTENT
        );
    }
}

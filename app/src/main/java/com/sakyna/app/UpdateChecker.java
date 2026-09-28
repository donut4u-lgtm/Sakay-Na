package com.sakyna.app;

import android.app.AlertDialog;
import android.content.Context;
import android.content.Intent;
import android.net.Uri;
import android.os.Build;
import android.os.Handler;
import android.os.Looper;

import org.json.JSONObject;

import java.io.BufferedReader;
import java.io.InputStreamReader;
import java.net.HttpURLConnection;
import java.net.URL;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

public final class UpdateChecker {

    private static final String RELEASE_API =
            "https://api.github.com/repos/donut4u-lgtm/Sakay-Na/releases/latest";

    private static final String DOWNLOAD_URL =
            "https://github.com/donut4u-lgtm/Sakay-Na/releases/download/v1.0.1/Sakay-Na-v2.apk";

    private UpdateChecker() {
    }

    public static void check(Context context) {

        ExecutorService executor =
                Executors.newSingleThreadExecutor();

        Handler mainHandler =
                new Handler(Looper.getMainLooper());

        executor.execute(() -> {

            ReleaseInfo release =
                    fetchLatestRelease();

            mainHandler.post(() -> {

                executor.shutdown();

                if (release == null || context == null) {
                    return;
                }

                int currentCode =
                        com.sakyna.app.BuildConfig.VERSION_CODE;

                if (release.versionCode <= currentCode) {
                    return;
                }

                if (!(context instanceof android.app.Activity)) {
                    return;
                }

                showUpdateDialog(
                        (android.app.Activity) context,
                        release.versionName,
                        release.versionCode
                );
            });
        });
    }

    private static ReleaseInfo fetchLatestRelease() {

        HttpURLConnection connection = null;

        try {

            URL url =
                    new URL(RELEASE_API);

            connection =
                    (HttpURLConnection)
                            url.openConnection();

            connection.setRequestMethod("GET");

            connection.setConnectTimeout(7000);

            connection.setReadTimeout(7000);

            connection.setRequestProperty(
                    "Accept",
                    "application/vnd.github+json"
            );

            connection.setRequestProperty(
                    "User-Agent",
                    "Sakay-Na-Android"
            );

            int responseCode =
                    connection.getResponseCode();

            if (responseCode !=
                    HttpURLConnection.HTTP_OK) {

                return null;
            }

            BufferedReader reader =
                    new BufferedReader(
                            new InputStreamReader(
                                    connection.getInputStream()
                            )
                    );

            StringBuilder jsonText =
                    new StringBuilder();

            String line;

            while ((line =
                    reader.readLine()) != null) {

                jsonText.append(line);
            }

            reader.close();

            JSONObject json =
                    new JSONObject(
                            jsonText.toString()
                    );

            String tagName =
                    json.optString(
                            "tag_name",
                            ""
                    );

            String body =
                    json.optString(
                            "body",
                            ""
                    );

            int versionCode =
                    extractVersionCode(body);

            if (versionCode <= 0) {
                return null;
            }

            String versionName =
                    tagName.startsWith("v")
                            ? tagName.substring(1)
                            : tagName;

            return new ReleaseInfo(
                    versionName,
                    versionCode
            );

        } catch (Exception ignored) {

            return null;

        } finally {

            if (connection != null) {
                connection.disconnect();
            }
        }
    }

    private static int extractVersionCode(
            String body
    ) {

        if (body == null) {
            return 0;
        }

        Pattern pattern =
                Pattern.compile(
                        "(?i)version\\s*code\\s*[:=]?\\s*(\\d+)"
                );

        Matcher matcher =
                pattern.matcher(body);

        if (!matcher.find()) {
            return 0;
        }

        try {

            return Integer.parseInt(
                    matcher.group(1)
            );

        } catch (Exception ignored) {

            return 0;
        }
    }

    private static void showUpdateDialog(
            android.app.Activity activity,
            String versionName,
            int versionCode
    ) {

        if (activity.isFinishing()) {
            return;
        }

        if (Build.VERSION.SDK_INT >= 17
                && activity.isDestroyed()) {

            return;
        }

        new AlertDialog.Builder(activity)

                .setTitle(
                        "🛺 Sakay Na Update Available"
                )

                .setMessage(
                        "A newer Sakay Na version is available.\n\n"
                                + "Version "
                                + versionName
                                + " ("
                                + versionCode
                                + ")"
                                + "\n\n"
                                + "Update using the existing Sakay Na download page."
                )

                .setPositiveButton(
                        "UPDATE NOW",
                        (dialog, which) -> {

                            try {

                                Intent intent =
                                        new Intent(
                                                Intent.ACTION_VIEW,
                                                Uri.parse(
                                                        DOWNLOAD_URL
                                                )
                                        );

                                activity.startActivity(
                                        intent
                                );

                            } catch (Exception ignored) {
                            }
                        }
                )

                .setNegativeButton(
                        "LATER",
                        null
                )

                .setCancelable(true)

                .show();
    }

    private static final class ReleaseInfo {

        final String versionName;

        final int versionCode;

        ReleaseInfo(
                String versionName,
                int versionCode
        ) {

            this.versionName =
                    versionName;

            this.versionCode =
                    versionCode;
        }
    }
}

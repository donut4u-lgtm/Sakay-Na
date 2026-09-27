package com.sakyna.app;

import android.app.Activity;
import android.content.ActivityNotFoundException;
import android.content.Intent;
import android.content.pm.PackageInfo;
import android.net.Uri;

import org.json.JSONArray;
import org.json.JSONObject;

import java.io.BufferedReader;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.net.HttpURLConnection;
import java.net.URL;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

public final class UpdateChecker {

    private static final String LATEST_RELEASE_API =
            "https://api.github.com/repos/donut4u-lgtm/Sakay-Na/releases/latest";

    private static final Pattern VERSION_CODE_PATTERN =
            Pattern.compile(
                    "Sakay-Na-v(\\d+)\\.apk",
                    Pattern.CASE_INSENSITIVE
            );

    private UpdateChecker() {
    }

    public static void check(Activity activity) {

        if (activity == null || activity.isFinishing()) {
            return;
        }

        Thread thread = new Thread(() -> {

            HttpURLConnection connection = null;

            try {

                URL url =
                        new URL(LATEST_RELEASE_API);

                connection =
                        (HttpURLConnection) url.openConnection();

                connection.setRequestMethod("GET");

                connection.setConnectTimeout(6000);

                connection.setReadTimeout(8000);

                connection.setRequestProperty(
                        "Accept",
                        "application/vnd.github+json"
                );

                connection.setRequestProperty(
                        "User-Agent",
                        "SakayNa-Android"
                );

                if (connection.getResponseCode()
                        != HttpURLConnection.HTTP_OK) {

                    return;
                }

                String response =
                        readStream(
                                connection.getInputStream()
                        );

                JSONObject release =
                        new JSONObject(response);

                JSONArray assets =
                        release.optJSONArray("assets");

                if (assets == null) {
                    return;
                }

                long remoteVersionCode = -1;

                String downloadUrl = null;

                String remoteVersionName =
                        release.optString(
                                "tag_name",
                                ""
                        );

                for (int i = 0;
                     i < assets.length();
                     i++) {

                    JSONObject asset =
                            assets.optJSONObject(i);

                    if (asset == null) {
                        continue;
                    }

                    String name =
                            asset.optString(
                                    "name",
                                    ""
                            );

                    Matcher matcher =
                            VERSION_CODE_PATTERN.matcher(
                                    name
                            );

                    if (!matcher.matches()) {
                        continue;
                    }

                    long candidate =
                            Long.parseLong(
                                    matcher.group(1)
                            );

                    if (candidate >
                            remoteVersionCode) {

                        remoteVersionCode =
                                candidate;

                        downloadUrl =
                                asset.optString(
                                        "browser_download_url",
                                        null
                                );
                    }
                }

                if (remoteVersionCode < 0
                        || downloadUrl == null
                        || downloadUrl.trim().isEmpty()) {

                    return;
                }

                final String finalDownloadUrl =
                        downloadUrl;

                final String finalVersionName =
                        remoteVersionName;

                PackageInfo packageInfo =
                        activity
                                .getPackageManager()
                                .getPackageInfo(
                                        activity.getPackageName(),
                                        0
                                );

                long localVersionCode;

                if (android.os.Build.VERSION.SDK_INT >= 28) {

                    localVersionCode =
                            packageInfo.getLongVersionCode();

                } else {

                    localVersionCode =
                            packageInfo.versionCode;
                }

                if (remoteVersionCode
                        <= localVersionCode) {

                    return;
                }

                activity.runOnUiThread(() -> {

                    if (activity.isFinishing()
                            || activity.isDestroyed()) {

                        return;
                    }

                    showUpdateDialog(
                            activity,
                            finalVersionName,
                            finalDownloadUrl
                    );
                });

            } catch (Exception ignored) {

                // Update checking must never
                // crash Sakay Na.

            } finally {

                if (connection != null) {
                    connection.disconnect();
                }
            }

        });

        thread.setDaemon(true);

        thread.start();
    }

    private static String readStream(
            InputStream inputStream
    ) throws Exception {

        StringBuilder result =
                new StringBuilder();

        try (
                BufferedReader reader =
                        new BufferedReader(
                                new InputStreamReader(
                                        inputStream,
                                        "UTF-8"
                                )
                        )
        ) {

            String line;

            while ((line =
                    reader.readLine()) != null) {

                result.append(line);
            }
        }

        return result.toString();
    }

    private static void showUpdateDialog(
            Activity activity,
            String versionName,
            String downloadUrl
    ) {

        String shownVersion =
                versionName == null
                        || versionName.trim().isEmpty()
                        ? "new version"
                        : versionName;

        new android.app.AlertDialog.Builder(
                activity
        )
                .setTitle(
                        "🛺 Sakay Na Update Available"
                )
                .setMessage(
                        "A newer Sakay Na APK is available ("
                                + shownVersion
                                + ").\n\n"
                                + "Tap UPDATE NOW to download "
                                + "the new signed APK. It will "
                                + "update the existing Sakay Na app."
                )
                .setNegativeButton(
                        "LATER",
                        null
                )
                .setPositiveButton(
                        "UPDATE NOW",
                        (dialog, which) ->
                                openDownloadPage(
                                        activity,
                                        downloadUrl
                                )
                )
                .show();
    }

    private static void openDownloadPage(
            Activity activity,
            String url
    ) {

        try {

            Intent intent =
                    new Intent(
                            Intent.ACTION_VIEW,
                            Uri.parse(url)
                    );

            activity.startActivity(intent);

        } catch (ActivityNotFoundException ignored) {

            // No browser available.
        }
    }
}

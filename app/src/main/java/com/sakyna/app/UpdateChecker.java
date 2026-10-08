package com.sakyna.app;

import android.app.Activity;
import android.app.AlertDialog;
import android.content.Context;
import android.content.Intent;
import android.content.pm.PackageInfo;
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

    /*
     * PERMANENT SAKAY NA APK DOWNLOAD
     *
     * This always points to the APK asset named SakayNa.apk
     * in the latest published GitHub release.
     */
    private static final String DOWNLOAD_URL =
            "https://github.com/donut4u-lgtm/Sakay-Na/releases/latest/download/SakayNa.apk";

    private UpdateChecker() {
    }

    public static void check(Context context) {

        if (context == null) {
            return;
        }

        ExecutorService executor =
                Executors.newSingleThreadExecutor();

        Handler mainHandler =
                new Handler(Looper.getMainLooper());

        executor.execute(() -> {

            ReleaseInfo release =
                    fetchLatestRelease();

            mainHandler.post(() -> {

                executor.shutdown();

                /*
                 * IMPORTANT:
                 *
                 * If GitHub cannot be reached, do NOT block the app.
                 * Mandatory update happens only when a newer release
                 * has been positively confirmed.
                 */
                if (release == null || context == null) {
                    return;
                }

                long currentCode =
                        getInstalledVersionCode(context);

                /*
                 * Current app is already the same version or newer.
                 */
                if (release.versionCode <= currentCode) {
                    return;
                }

                /*
                 * Update dialog requires an Activity.
                 */
                if (!(context instanceof Activity)) {
                    return;
                }

                Activity activity =
                        (Activity) context;

                showMandatoryUpdateDialog(
                        activity,
                        release.versionName,
                        release.versionCode
                );
            });
        });
    }

    private static long getInstalledVersionCode(
            Context context
    ) {

        try {

            PackageInfo packageInfo =
                    context.getPackageManager()
                            .getPackageInfo(
                                    context.getPackageName(),
                                    0
                            );

            if (Build.VERSION.SDK_INT >= 28) {

                return packageInfo.getLongVersionCode();

            } else {

                return packageInfo.versionCode;
            }

        } catch (Exception ignored) {

            return 0;
        }
    }

    private static ReleaseInfo fetchLatestRelease() {

        HttpURLConnection connection = null;
        BufferedReader reader = null;

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

            reader =
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

            long versionCode =
                    extractVersionCode(body);

            /*
             * A release without a valid version code is not considered
             * an update. This prevents a bad/malformed GitHub release
             * from locking users out.
             */
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

            if (reader != null) {

                try {

                    reader.close();

                } catch (Exception ignored) {
                }
            }

            if (connection != null) {

                connection.disconnect();
            }
        }
    }

    private static long extractVersionCode(
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

            return Long.parseLong(
                    matcher.group(1)
            );

        } catch (Exception ignored) {

            return 0;
        }
    }

    private static void showMandatoryUpdateDialog(
            Activity activity,
            String versionName,
            long versionCode
    ) {

        if (activity == null) {
            return;
        }

        if (activity.isFinishing()) {
            return;
        }

        if (Build.VERSION.SDK_INT >= 17
                && activity.isDestroyed()) {

            return;
        }

        AlertDialog dialog =
                new AlertDialog.Builder(activity)

                        .setTitle(
                                "🛺 Sakay Na Update Required"
                        )

                        .setMessage(
                                "A newer version of Sakay Na is required.\n\n"
                                        + "New version: "
                                        + versionName
                                        + " ("
                                        + versionCode
                                        + ")"
                                        + "\n\n"
                                        + "Please update the app to continue using "
                                        + "the latest Sakay Na features and services."
                        )

                        .setPositiveButton(
                                "UPDATE NOW",
                                (dialogInterface, which) -> {

                                    openLatestApk(
                                            activity
                                    );
                                }
                        )

                        /*
                         * NO LATER BUTTON.
                         *
                         * The user must update after a newer version
                         * has been confirmed.
                         */
                        .setCancelable(false)

                        .create();

        dialog.setCanceledOnTouchOutside(false);

        dialog.setOnShowListener(
                dialogInterface -> {

                    /*
                     * Make sure the only available action is UPDATE NOW.
                     */
                    if (dialog.getButton(
                            AlertDialog.BUTTON_POSITIVE
                    ) != null) {

                        dialog.getButton(
                                AlertDialog.BUTTON_POSITIVE
                        ).setText(
                                "UPDATE NOW"
                        );
                    }
                }
        );

        dialog.show();
    }

    private static void openLatestApk(
            Activity activity
    ) {

        if (activity == null) {
            return;
        }

        try {

            Intent intent =
                    new Intent(
                            Intent.ACTION_VIEW,
                            Uri.parse(
                                    DOWNLOAD_URL
                            )
                    );

            intent.addFlags(
                    Intent.FLAG_ACTIVITY_NEW_TASK
            );

            activity.startActivity(
                    intent
            );

        } catch (Exception ignored) {

            /*
             * If Android cannot find a browser/app capable of
             * opening the download URL, do nothing rather than
             * crashing Sakay Na.
             */
        }
    }

    private static final class ReleaseInfo {

        final String versionName;

        final long versionCode;

        ReleaseInfo(
                String versionName,
                long versionCode
        ) {

            this.versionName =
                    versionName;

            this.versionCode =
                    versionCode;
        }
    }
}

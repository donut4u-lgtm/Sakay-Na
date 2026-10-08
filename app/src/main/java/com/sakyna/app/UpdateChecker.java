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
     * Always points to the APK asset named SakayNa.apk
     * in the latest published GitHub release.
     */
    private static final String DOWNLOAD_URL =
            "https://github.com/donut4u-lgtm/Sakay-Na/releases/latest/download/SakayNa.apk";

    private UpdateChecker() {
    }

    /*
     * ============================================================
     * NORMAL APP UPDATE CHECK
     * ============================================================
     *
     * Used by the normal Sakay Na application startup.
     *
     * If a newer release is positively confirmed, the user must
     * update before continuing.
     */
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
                 * If GitHub cannot be reached, do not block the
                 * normal application startup.
                 */
                if (release == null || context == null) {
                    return;
                }

                long currentCode =
                        getInstalledVersionCode(context);

                /*
                 * Same or newer version = already current.
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

    /*
     * ============================================================
     * DRIVER VERSION GATE
     * ============================================================
     *
     * This is separate from the normal update notification.
     *
     * Driver operations must only be allowed when the installed
     * APK versionCode is equal to or newer than the latest
     * published release.
     *
     * If GitHub cannot be verified, the driver gate FAILS CLOSED.
     * That means the driver cannot go online or accept rides until
     * the application can verify its version.
     */
    public interface DriverVersionCallback {

        void onResult(
                boolean allowed,
                boolean updateRequired,
                String latestVersionName,
                long latestVersionCode
        );
    }

    public static void checkDriverVersion(
            Context context,
            DriverVersionCallback callback
    ) {

        if (context == null || callback == null) {
            return;
        }

        ExecutorService executor =
                Executors.newSingleThreadExecutor();

        Handler mainHandler =
                new Handler(Looper.getMainLooper());

        executor.execute(() -> {

            ReleaseInfo release =
                    fetchLatestRelease();

            long currentCode =
                    getInstalledVersionCode(context);

            mainHandler.post(() -> {

                executor.shutdown();

                /*
                 * Cannot verify the current published release.
                 *
                 * FAIL CLOSED for drivers.
                 */
                if (release == null) {

                    callback.onResult(
                            false,
                            false,
                            "",
                            0
                    );

                    return;
                }

                boolean allowed =
                        currentCode >= release.versionCode;

                boolean updateRequired =
                        currentCode < release.versionCode;

                callback.onResult(
                        allowed,
                        updateRequired,
                        release.versionName,
                        release.versionCode
                );
            });
        });
    }

    /*
     * ============================================================
     * INSTALLED VERSION
     * ============================================================
     */
    public static long getInstalledVersionCode(
            Context context
    ) {

        if (context == null) {
            return 0;
        }

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

    /*
     * ============================================================
     * FETCH LATEST GITHUB RELEASE
     * ============================================================
     */
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

            /*
             * The release body MUST contain:
             *
             * Version Code: 6
             *
             * for a future versionCode 6 release.
             */
            long versionCode =
                    extractVersionCode(body);

            /*
             * Never consider a release valid without a version
             * code. This prevents malformed releases from
             * accidentally blocking the application.
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

    /*
     * ============================================================
     * VERSION CODE EXTRACTION
     * ============================================================
     */
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

    /*
     * ============================================================
     * NORMAL UPDATE DIALOG
     * ============================================================
     */
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
                         */
                        .setCancelable(false)

                        .create();

        dialog.setCanceledOnTouchOutside(false);

        dialog.setOnShowListener(
                dialogInterface -> {

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

    /*
     * ============================================================
     * OPEN LATEST APK
     * ============================================================
     *
     * PUBLIC because DriverActivity also needs to open the
     * mandatory update download.
     */
    public static void openLatestApk(
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
             * Do not crash Sakay Na if Android cannot open the URL.
             */
        }
    }

    /*
     * ============================================================
     * RELEASE INFORMATION
     * ============================================================
     */
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

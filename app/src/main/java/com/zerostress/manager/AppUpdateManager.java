package com.zerostress.manager;

import android.app.DownloadManager;
import android.app.NotificationChannel;
import android.app.NotificationManager;
import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;
import android.content.IntentFilter;
import android.database.Cursor;
import android.graphics.Color;
import android.net.ConnectivityManager;
import android.net.NetworkInfo;
import android.net.Uri;
import android.os.Build;
import android.os.Environment;
import android.os.Handler;
import android.os.Looper;
import android.util.Log;
import android.view.Gravity;
import android.widget.LinearLayout;
import android.widget.ProgressBar;
import android.widget.TextView;

import androidx.appcompat.app.AlertDialog;
import androidx.core.app.NotificationCompat;
import androidx.core.app.NotificationManagerCompat;
import androidx.core.content.ContextCompat;
import androidx.core.content.FileProvider;

import com.google.firebase.firestore.FirebaseFirestore;

import java.io.File;
import java.util.HashMap;
import java.util.Map;

/**
 * Handles in-app updates:
 * 1. Checks Firestore for latest version info on app start
 * 2. Compares with installed version
 * 3. Shows update dialog with changelog
 * 4. Downloads APK with progress notification
 * 5. Prompts user to install
 *
 * Admin sets update info in Firestore:
 *   appSettings/updates → { versionCode, versionName, downloadUrl, changelog,强制 }
 */
public class AppUpdateManager {
    private static final String TAG = "AppUpdateManager";
    private static final String CHANNEL_ID = "app_updates";
    private static final int NOTIFICATION_ID = 9999;
    private static final String PREF_NAME = "zerostress_prefs";
    private static final String KEY_SKIP_VERSION = "skipped_version";

    private final Context context;
    private final FirebaseFirestore db;
    private final Handler mainHandler;
    private long downloadId = -1;

    public AppUpdateManager(Context context) {
        this.context = context;
        this.db = FirebaseFirestore.getInstance();
        this.mainHandler = new Handler(Looper.getMainLooper());
        createNotificationChannel();
    }

    /**
     * Check for updates. Call this in MainActivity.onCreate()
     * @param currentVersionCode the app's current versionCode from BuildConfig
     * @param isAdmin whether the current user is admin (for force update option)
     */
    public void checkForUpdates(int currentVersionCode, boolean isAdmin) {
        db.collection("appSettings").document("updates")
            .get()
            .addOnSuccessListener(doc -> {
                if (doc == null || !doc.exists()) return;

                Long latestVersionCode = doc.getLong("versionCode");
                String versionName = doc.getString("versionName");
                String downloadUrl = doc.getString("downloadUrl");
                String changelog = doc.getString("changelog");
                Boolean forceUpdate = doc.getBoolean("forceUpdate");

                if (latestVersionCode == null || downloadUrl == null || downloadUrl.isEmpty()) return;
                if (latestVersionCode <= currentVersionCode) return;

                // Check if user skipped this version
                String skipped = context.getSharedPreferences(PREF_NAME, Context.MODE_PRIVATE)
                    .getString(KEY_SKIP_VERSION, "");
                if (!isAdmin && forceUpdate == null && skipped.equals(String.valueOf(latestVersionCode))) return;

                boolean force = forceUpdate != null && forceUpdate;

                mainHandler.post(() -> showUpdateDialog(
                    versionName != null ? versionName : "New Version",
                    changelog != null ? changelog : "Bug fixes and improvements",
                    downloadUrl,
                    force
                ));
            })
            .addOnFailureListener(e -> Log.w(TAG, "Update check failed: " + e.getMessage()));
    }

    private void showUpdateDialog(String versionName, String changelog, String downloadUrl, boolean force) {
        AlertDialog.Builder builder = new AlertDialog.Builder(context);
        builder.setTitle("🔄 Update Available — v" + versionName);

        LinearLayout layout = new LinearLayout(context);
        layout.setOrientation(LinearLayout.VERTICAL);
        layout.setPadding(48, 24, 48, 16);

        TextView changelogTv = new TextView(context);
        changelogTv.setText("What's New:\n\n" + changelog);
        changelogTv.setTextColor(Color.parseColor("#f1f5f9"));
        changelogTv.setTextSize(14);
        changelogTv.setLineSpacing(4, 1.2f);
        layout.addView(changelogTv);

        // Progress bar (hidden initially)
        ProgressBar progressBar = new ProgressBar(context, null, android.R.attr.progressBarStyleHorizontal);
        progressBar.setMax(100);
        progressBar.setProgress(0);
        progressBar.setVisibility(android.view.View.GONE);
        LinearLayout.LayoutParams progressParams = new LinearLayout.LayoutParams(
            LinearLayout.LayoutParams.MATCH_PARENT, LinearLayout.LayoutParams.WRAP_CONTENT);
        progressParams.setMargins(0, 16, 0, 0);
        layout.addView(progressBar, progressParams);

        TextView progressText = new TextView(context);
        progressText.setText("");
        progressText.setTextColor(Color.parseColor("#94a3b8"));
        progressText.setTextSize(12);
        progressText.setVisibility(android.view.View.GONE);
        layout.addView(progressText);

        builder.setView(layout);

        if (!force) {
            builder.setNegativeButton("Skip", (d, w) -> {
                // Remember this version as skipped
                context.getSharedPreferences(PREF_NAME, Context.MODE_PRIVATE)
                    .edit()
                    .putString(KEY_SKIP_VERSION, String.valueOf(
                        System.currentTimeMillis())) // Store temp
                    .apply();
            });
        }

        builder.setPositiveButton("Update", (d, w) -> {
            startDownload(downloadUrl, progressBar, progressText);
        });

        AlertDialog dialog = builder.create();
        if (force) {
            dialog.setCancelable(false);
            dialog.setCanceledOnTouchOutside(false);
        }
        dialog.show();
    }

    // ==================== DOWNLOAD ====================

    private void startDownload(String url, ProgressBar progressBar, TextView progressText) {
        progressBar.setVisibility(android.view.View.VISIBLE);
        progressText.setVisibility(android.view.View.VISIBLE);
        progressText.setText("Starting download...");

        DownloadManager.Request request = new DownloadManager.Request(Uri.parse(url));
        request.setTitle("Zero Stress Update");
        request.setDescription("Downloading latest version...");
        request.setNotificationVisibility(DownloadManager.Request.VISIBILITY_VISIBLE_NOTIFY_COMPLETED);
        request.setDestinationInExternalPublicDir(Environment.DIRECTORY_DOWNLOADS,
            "ZeroStress-update.apk");
        request.setAllowedOverMetered(true);
        request.setAllowedOverRoaming(true);

        DownloadManager downloadManager = (DownloadManager) context.getSystemService(Context.DOWNLOAD_SERVICE);
        if (downloadManager == null) return;

        downloadId = downloadManager.enqueue(request);

        // Register receiver to track download progress
        BroadcastReceiver receiver = new BroadcastReceiver() {
            @Override
            public void onReceive(Context ctx, Intent intent) {
                long id = intent.getLongExtra(DownloadManager.EXTRA_DOWNLOAD_ID, -1);
                if (id != downloadId) return;

                DownloadManager.Query query = new DownloadManager.Query();
                query.setFilterById(downloadId);
                Cursor cursor = downloadManager.query(query);

                if (cursor != null && cursor.moveToFirst()) {
                    int status = cursor.getInt(cursor.getColumnIndexOrThrow(DownloadManager.COLUMN_STATUS));

                    if (status == DownloadManager.STATUS_RUNNING) {
                        long bytesDownloaded = cursor.getLong(
                            cursor.getColumnIndexOrThrow(DownloadManager.COLUMN_BYTES_DOWNLOADED_SO_FAR));
                        long totalBytes = cursor.getLong(
                            cursor.getColumnIndexOrThrow(DownloadManager.COLUMN_TOTAL_SIZE_BYTES));

                        if (totalBytes > 0) {
                            int percent = (int) ((bytesDownloaded * 100) / totalBytes);
                            int downloadedKB = (int) (bytesDownloaded / 1024);
                            int totalKB = (int) (totalBytes / 1024);

                            mainHandler.post(() -> {
                                progressBar.setProgress(percent);
                                progressText.setText(percent + "% — " + downloadedKB + "KB / " + totalKB + "KB");
                            });
                        }
                    } else if (status == DownloadManager.STATUS_SUCCESSFUL) {
                        mainHandler.post(() -> {
                            progressBar.setProgress(100);
                            progressText.setText("✅ Download complete! Tap Install.");
                            installApk();
                        });
                        try { context.unregisterReceiver(this); } catch (Exception ignored) {}
                    } else if (status == DownloadManager.STATUS_FAILED) {
                        mainHandler.post(() -> {
                            progressText.setText("❌ Download failed. Try again.");
                            progressBar.setVisibility(android.view.View.GONE);
                        });
                        try { context.unregisterReceiver(this); } catch (Exception ignored) {}
                    }
                }
                if (cursor != null) cursor.close();
            }
        };

        IntentFilter filter = new IntentFilter(DownloadManager.ACTION_DOWNLOAD_COMPLETE);
        ContextCompat.registerReceiver(context, receiver, filter, ContextCompat.RECEIVER_NOT_EXPORTED);
    }

    // ==================== INSTALL ====================

    private void installApk() {
        File downloadDir = Environment.getExternalStoragePublicDirectory(Environment.DIRECTORY_DOWNLOADS);
        File apkFile = new File(downloadDir, "ZeroStress-update.apk");

        if (!apkFile.exists()) {
            android.widget.Toast.makeText(context, "APK file not found", android.widget.Toast.LENGTH_SHORT).show();
            return;
        }

        try {
            Uri apkUri;
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.N) {
                apkUri = FileProvider.getUriForFile(context,
                    context.getPackageName() + ".fileprovider", apkFile);
            } else {
                apkUri = Uri.fromFile(apkFile);
            }

            Intent installIntent = new Intent(Intent.ACTION_VIEW);
            installIntent.setDataAndType(apkUri, "application/vnd.android.package-archive");
            installIntent.addFlags(Intent.FLAG_GRANT_READ_URI_PERMISSION);
            installIntent.addFlags(Intent.FLAG_ACTIVITY_NEW_TASK);
            context.startActivity(installIntent);
        } catch (Exception e) {
            // Fallback: open downloads folder
            Intent intent = new Intent(Intent.ACTION_VIEW);
            intent.setDataAndType(Uri.fromFile(apkFile), "application/vnd.android.package-archive");
            intent.addFlags(Intent.FLAG_ACTIVITY_NEW_TASK);
            context.startActivity(intent);
        }
    }

    // ==================== NOTIFICATION CHANNEL ====================

    private void createNotificationChannel() {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            NotificationChannel channel = new NotificationChannel(
                CHANNEL_ID, "App Updates",
                NotificationManager.IMPORTANCE_HIGH);
            channel.setDescription("App update download progress");
            NotificationManager manager = context.getSystemService(NotificationManager.class);
            if (manager != null) manager.createNotificationChannel(channel);
        }
    }

    // ==================== ADMIN: SET UPDATE INFO ====================

    /**
     * Call this from AdminCustomizerActivity to set update info.
     * Admin provides: version code, version name, download URL, changelog
     */
    public static void setUpdateInfo(int versionCode, String versionName,
                                      String downloadUrl, String changelog,
                                      boolean forceUpdate, OnUpdateSetCallback callback) {
        FirebaseFirestore db = FirebaseFirestore.getInstance();
        Map<String, Object> data = new HashMap<>();
        data.put("versionCode", versionCode);
        data.put("versionName", versionName);
        data.put("downloadUrl", downloadUrl);
        data.put("changelog", changelog);
        data.put("forceUpdate", forceUpdate);
        data.put("timestamp", System.currentTimeMillis());

        db.collection("appSettings").document("updates")
            .set(data)
            .addOnSuccessListener(aVoid -> {
                if (callback != null) callback.onSuccess();
            })
            .addOnFailureListener(e -> {
                if (callback != null) callback.onFailure(e.getMessage());
            });
    }

    public interface OnUpdateSetCallback {
        void onSuccess();
        void onFailure(String error);
    }
}

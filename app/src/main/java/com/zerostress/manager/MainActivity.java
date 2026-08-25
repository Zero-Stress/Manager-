package com.zerostress.manager;

import android.Manifest;
import android.annotation.SuppressLint;
import android.app.Activity;
import android.content.Intent;
import android.content.pm.PackageManager;
import android.net.Uri;
import android.os.Build;
import android.os.Bundle;
import android.os.Environment;
import android.provider.MediaStore;
import android.util.Log;
import android.view.KeyEvent;
import android.view.View;
import android.view.Window;
import android.view.WindowManager;
import android.webkit.JavascriptInterface;
import android.webkit.ValueCallback;
import android.webkit.WebChromeClient;
import android.webkit.WebResourceRequest;
import android.webkit.WebSettings;
import android.webkit.WebView;
import android.webkit.WebViewClient;
import android.widget.Toast;

import androidx.appcompat.app.AppCompatActivity;
import androidx.core.app.ActivityCompat;
import androidx.core.app.NotificationManagerCompat;
import androidx.core.content.ContextCompat;
import androidx.core.content.FileProvider;

import com.google.firebase.firestore.FirebaseFirestore;

import java.io.File;
import java.io.IOException;
import java.text.SimpleDateFormat;
import java.util.Date;
import java.util.HashMap;
import java.util.Locale;
import java.util.Map;

public class MainActivity extends AppCompatActivity {

    private static final String TAG = "MainActivity";
    private WebView webView;
    private static final int FILE_CHOOSER_REQUEST_CODE = 100;
    private static final int CAMERA_PERMISSION_REQUEST_CODE = 200;
    private static final int NOTIFICATION_PERMISSION_CODE = 300;
    private ValueCallback<Uri[]> fileUploadCallback;
    private String cameraPhotoPath;

    // Notification helper
    private NotificationHelper notificationHelper;

    @SuppressLint("SetJavaScriptEnabled")
    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);

        // Full-screen immersive mode
        requestWindowFeature(Window.FEATURE_NO_TITLE);
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.P) {
            getWindow().getAttributes().layoutInDisplayCutoutMode =
                    WindowManager.LayoutParams.LAYOUT_IN_DISPLAY_CUTOUT_MODE_SHORT_EDGES;
        }

        setContentView(R.layout.activity_main);

        // Initialize notification helper
        notificationHelper = new NotificationHelper(this);

        webView = findViewById(R.id.webView);

        // Configure WebView settings
        WebSettings webSettings = webView.getSettings();
        webSettings.setJavaScriptEnabled(true);
        webSettings.setDomStorageEnabled(true);
        webSettings.setDatabaseEnabled(true);
        webSettings.setAllowFileAccess(true);
        webSettings.setAllowContentAccess(true);
        webSettings.setMediaPlaybackRequiresUserGesture(false);
        webSettings.setCacheMode(WebSettings.LOAD_DEFAULT);
        webSettings.setMixedContentMode(WebSettings.MIXED_CONTENT_ALWAYS_ALLOW);

        // Set viewport meta tag for responsive mobile layout
        webSettings.setUseWideViewPort(true);
        webSettings.setLoadWithOverviewMode(true);

        // Enable Zoom controls (optional, users can zoom if needed)
        webSettings.setBuiltInZoomControls(true);
        webSettings.setDisplayZoomControls(false);

        // Add JavaScript interface for notifications
        webView.addJavascriptInterface(new NotificationJSInterface(), "AndroidNotification");

        // Set a WebViewClient to handle page navigation
        webView.setWebViewClient(new WebViewClient() {
            @Override
            public boolean shouldOverrideUrlLoading(WebView view, WebResourceRequest request) {
                // Keep navigation inside the WebView
                view.loadUrl(request.getUrl().toString());
                return true;
            }

            @Override
            public void onPageFinished(WebView view, String url) {
                super.onPageFinished(view, url);
                // Inject viewport meta tag for proper mobile scaling
                view.evaluateJavascript(
                    "(function() {" +
                    "  var meta = document.querySelector('meta[name=\"viewport\"]');" +
                    "  if (!meta) {" +
                    "    meta = document.createElement('meta');" +
                    "    meta.name = 'viewport';" +
                    "    meta.content = 'width=device-width, initial-scale=1.0, maximum-scale=1.0, user-scalable=no';" +
                    "    document.head.appendChild(meta);" +
                    "  }" +
                    "})()", null
                );
            }
        });

        // Set a WebChromeClient to handle file uploads (for screenshot/OCR feature)
        webView.setWebChromeClient(new WebChromeClient() {
            @Override
            public boolean onShowFileChooser(WebView webView, ValueCallback<Uri[]> filePathCallback, FileChooserParams fileChooserParams) {
                // Cancel any existing file upload callback
                if (fileUploadCallback != null) {
                    fileUploadCallback.onReceiveValue(null);
                }
                fileUploadCallback = filePathCallback;

                // Check camera permission
                if (ContextCompat.checkSelfPermission(MainActivity.this, Manifest.permission.CAMERA)
                        != PackageManager.PERMISSION_GRANTED) {
                    ActivityCompat.requestPermissions(MainActivity.this,
                            new String[]{Manifest.permission.CAMERA},
                            CAMERA_PERMISSION_REQUEST_CODE);
                    return true;
                }

                // Launch file chooser with camera option
                launchFileChooser();
                return true;
            }
        });

        // Request notification permission on Android 13+
        requestNotificationPermission();

        // Load the local HTML file from assets
        webView.loadUrl("file:///android_asset/index.html");

        // Handle intent extras for notification navigation
        handleNotificationIntent(getIntent());
    }

    @Override
    protected void onNewIntent(Intent intent) {
        super.onNewIntent(intent);
        handleNotificationIntent(intent);
    }

    /**
     * Handle navigation from notification tap.
     */
    private void handleNotificationIntent(Intent intent) {
        if (intent != null && intent.hasExtra("navigate_to")) {
            String destination = intent.getStringExtra("navigate_to");
            // Delay to ensure page is loaded
            webView.postDelayed(() -> {
                webView.evaluateJavascript(
                    "if (typeof navigateTo === 'function') { navigateTo('" + destination + "'); }",
                    null
                );
            }, 1000);
        }
    }

    /**
     * Request notification permission for Android 13+.
     */
    private void requestNotificationPermission() {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
            if (ContextCompat.checkSelfPermission(this, Manifest.permission.POST_NOTIFICATIONS)
                    != PackageManager.PERMISSION_GRANTED) {
                ActivityCompat.requestPermissions(this,
                        new String[]{Manifest.permission.POST_NOTIFICATIONS},
                        NOTIFICATION_PERMISSION_CODE);
            }
        }
    }

    /**
     * JavaScript interface to allow web app to trigger notifications.
     */
    public class NotificationJSInterface {

        @JavascriptInterface
        public void showNotification(String title, String body, String type) {
            runOnUiThread(() -> {
                if (notificationHelper != null) {
                    notificationHelper.showNotification(title, body, type);
                }
            });
        }

        @JavascriptInterface
        public void syncFCMToken(String userPhone) {
            runOnUiThread(() -> {
                String token = notificationHelper.getStoredToken();
                if (token != null && userPhone != null) {
                    notificationHelper.saveTokenToUser(userPhone, token);
                    notificationHelper.saveUserPhone(userPhone);
                } else {
                    // Retrieve fresh token
                    MyFirebaseMessagingService.retrieveToken(MainActivity.this);
                }
            });
        }

        @JavascriptInterface
        public void startLeaderboardListener() {
            runOnUiThread(() -> {
                if (notificationHelper != null) {
                    notificationHelper.setLeaderboardUpdateCallback((type, message) -> {
                        // Show in-app notification when leaderboard changes
                        runOnUiThread(() -> {
                            Toast.makeText(MainActivity.this, message, Toast.LENGTH_LONG).show();
                        });
                    });
                    notificationHelper.startLeaderboardListener();
                }
            });
        }

        @JavascriptInterface
        public void stopLeaderboardListener() {
            runOnUiThread(() -> {
                if (notificationHelper != null) {
                    notificationHelper.stopLeaderboardListener();
                }
            });
        }

        @JavascriptInterface
        public boolean areNotificationsEnabled() {
            return NotificationManagerCompat.from(MainActivity.this).areNotificationsEnabled();
        }
    }

    private void launchFileChooser() {
        Intent contentSelectionIntent = new Intent(Intent.ACTION_GET_CONTENT);
        contentSelectionIntent.addCategory(Intent.CATEGORY_OPENABLE);
        contentSelectionIntent.setType("image/*");

        Intent cameraIntent = new Intent(MediaStore.ACTION_IMAGE_CAPTURE);
        File photoFile = null;
        try {
            photoFile = createImageFile();
        } catch (IOException ex) {
            // Error creating file
        }

        if (photoFile != null) {
            cameraPhotoPath = "file:" + photoFile.getAbsolutePath();
            cameraIntent.putExtra(MediaStore.EXTRA_OUTPUT,
                    FileProvider.getUriForFile(this,
                            getPackageName() + ".fileprovider",
                            photoFile));
        }

        Intent chooserIntent = new Intent(Intent.ACTION_CHOOSER);
        chooserIntent.putExtra(Intent.EXTRA_INTENT, contentSelectionIntent);
        chooserIntent.putExtra(Intent.EXTRA_TITLE, "Select Image for OCR");
        chooserIntent.putExtra(Intent.EXTRA_INITIAL_INTENTS, new Intent[]{cameraIntent});

        startActivityForResult(chooserIntent, FILE_CHOOSER_REQUEST_CODE);
    }

    private File createImageFile() throws IOException {
        String timeStamp = new SimpleDateFormat("yyyyMMdd_HHmmss", Locale.getDefault()).format(new Date());
        String imageFileName = "JPEG_" + timeStamp + "_";
        File storageDir = getExternalFilesDir(Environment.DIRECTORY_PICTURES);
        return File.createTempFile(imageFileName, ".jpg", storageDir);
    }

    @Override
    public void onRequestPermissionsResult(int requestCode, String[] permissions, int[] grantResults) {
        super.onRequestPermissionsResult(requestCode, permissions, grantResults);

        switch (requestCode) {
            case CAMERA_PERMISSION_REQUEST_CODE:
                if (grantResults.length > 0 && grantResults[0] == PackageManager.PERMISSION_GRANTED) {
                    launchFileChooser();
                } else {
                    // Camera permission denied, still allow file selection
                    Intent intent = new Intent(Intent.ACTION_GET_CONTENT);
                    intent.addCategory(Intent.CATEGORY_OPENABLE);
                    intent.setType("image/*");
                    startActivityForResult(intent, FILE_CHOOSER_REQUEST_CODE);
                }
                break;

            case NOTIFICATION_PERMISSION_CODE:
                if (grantResults.length > 0 && grantResults[0] == PackageManager.PERMISSION_GRANTED) {
                    Log.d(TAG, "Notification permission granted");
                    // Retrieve FCM token
                    MyFirebaseMessagingService.retrieveToken(this);
                } else {
                    Log.d(TAG, "Notification permission denied");
                }
                break;
        }
    }

    @Override
    protected void onActivityResult(int requestCode, int resultCode, Intent data) {
        super.onActivityResult(requestCode, resultCode, data);

        if (requestCode != FILE_CHOOSER_REQUEST_CODE || fileUploadCallback == null) {
            return;
        }

        Uri[] results = null;
        if (resultCode == Activity.RESULT_OK) {
            if (data == null || data.getData() == null) {
                // Camera photo
                if (cameraPhotoPath != null) {
                    results = new Uri[]{Uri.parse(cameraPhotoPath)};
                }
            } else {
                // File chooser
                String dataString = data.getDataString();
                if (dataString != null) {
                    results = new Uri[]{Uri.parse(dataString)};
                }
            }
        }

        fileUploadCallback.onReceiveValue(results);
        fileUploadCallback = null;
    }

    // Handle back button to navigate within WebView
    @Override
    public boolean onKeyDown(int keyCode, KeyEvent event) {
        if (keyCode == KeyEvent.KEYCODE_BACK && webView.canGoBack()) {
            webView.goBack();
            return true;
        }
        return super.onKeyDown(keyCode, event);
    }

    @Override
    protected void onResume() {
        super.onResume();
        webView.onResume();
        // Refresh FCM token on resume
        MyFirebaseMessagingService.retrieveToken(this);
    }

    @Override
    protected void onPause() {
        super.onPause();
        webView.onPause();
    }

    @Override
    protected void onDestroy() {
        if (notificationHelper != null) {
            notificationHelper.cleanup();
        }
        if (webView != null) {
            webView.destroy();
        }
        super.onDestroy();
    }
}

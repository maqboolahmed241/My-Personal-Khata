package com.personal.khata;

import android.app.Activity;
import android.os.Bundle;
import android.os.Build;
import android.webkit.WebChromeClient;
import android.webkit.WebSettings;
import android.webkit.WebView;
import android.webkit.WebViewClient;
import android.webkit.ValueCallback;
import android.webkit.JavascriptInterface;
import android.content.Intent;
import android.content.ContentValues;
import android.net.Uri;
import android.provider.MediaStore;
import android.widget.Toast;
import android.graphics.Color;
import java.io.OutputStream;
import java.io.File;
import java.io.FileOutputStream;
import java.io.IOException;

public class MainActivity extends Activity {
    private WebView webView;
    private ValueCallback<Uri[]> fileCallback;
    private static final int FILE_CHOOSER_REQUEST = 101;
    @Override public void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        getWindow().setStatusBarColor(Color.rgb(23,20,17));
        getWindow().setNavigationBarColor(Color.rgb(23,20,17));
        webView = new WebView(this);
        WebSettings settings = webView.getSettings();
        settings.setJavaScriptEnabled(true);
        settings.setDomStorageEnabled(true);
        settings.setDatabaseEnabled(true);
        settings.setAllowFileAccess(true);
        settings.setAllowContentAccess(true);
        settings.setJavaScriptCanOpenWindowsAutomatically(true);
        settings.setSupportMultipleWindows(false);
        webView.setBackgroundColor(Color.rgb(18,17,15));
        webView.addJavascriptInterface(new KhataBridge(), "KhataBridge");
        webView.setWebViewClient(new WebViewClient() {
            @Override public boolean shouldOverrideUrlLoading(WebView view, String url) {
                if (url != null && (url.startsWith("https://") || url.startsWith("http://") || url.startsWith("mailto:"))) {
                    try { startActivity(new Intent(Intent.ACTION_VIEW, Uri.parse(url))); }
                    catch (Exception e) { Toast.makeText(MainActivity.this, "Link could not be opened", Toast.LENGTH_SHORT).show(); }
                    return true;
                }
                return false;
            }
        });
        webView.setWebChromeClient(new WebChromeClient() {
            @Override public boolean onShowFileChooser(WebView view, ValueCallback<Uri[]> callback, FileChooserParams params) {
                if (fileCallback != null) fileCallback.onReceiveValue(null);
                fileCallback = callback;
                Intent intent = params.createIntent();
                try { startActivityForResult(intent, FILE_CHOOSER_REQUEST); }
                catch (Exception e) { fileCallback = null; Toast.makeText(MainActivity.this, "File picker unavailable", Toast.LENGTH_SHORT).show(); return false; }
                return true;
            }
        });
        setContentView(webView);
        webView.loadUrl("file:///android_asset/index.html");
    }
    private class KhataBridge {
        @JavascriptInterface
        public void saveBackup(String content, String filename) {
            if (content == null || filename == null) {
                runOnUiThread(() -> Toast.makeText(MainActivity.this, "بیک اپ محفوظ نہیں ہو سکا", Toast.LENGTH_LONG).show());
                return;
            }
            String safeName = filename.replaceAll("[^a-zA-Z0-9._-]", "_");
            try {
                Uri uri;
                if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.Q) {
                    ContentValues values = new ContentValues();
                    values.put(MediaStore.MediaColumns.DISPLAY_NAME, safeName);
                    values.put(MediaStore.MediaColumns.MIME_TYPE, "application/json");
                    values.put(MediaStore.MediaColumns.RELATIVE_PATH, android.os.Environment.DIRECTORY_DOWNLOADS + "/");
                    values.put(MediaStore.MediaColumns.IS_PENDING, 1);
                    uri = getContentResolver().insert(MediaStore.Downloads.EXTERNAL_CONTENT_URI, values);
                    if (uri == null) throw new IOException("Could not create backup file");
                    try (OutputStream out = getContentResolver().openOutputStream(uri, "w")) {
                        if (out == null) throw new IOException("Could not open backup file");
                        out.write(content.getBytes("UTF-8"));
                        out.flush();
                    }
                    ContentValues published = new ContentValues();
                    published.put(MediaStore.MediaColumns.IS_PENDING, 0);
                    int updatedRows = getContentResolver().update(uri, published, null, null);
                    if (updatedRows < 1) throw new IOException("Could not publish backup file");
                } else {
                    File dir = android.os.Environment.getExternalStoragePublicDirectory(android.os.Environment.DIRECTORY_DOWNLOADS);
                    if (!dir.exists() && !dir.mkdirs()) throw new IOException("Could not create Downloads folder");
                    try (FileOutputStream out = new FileOutputStream(new File(dir, safeName))) {
                        out.write(content.getBytes("UTF-8"));
                        out.flush();
                    }
                }
                runOnUiThread(() -> Toast.makeText(MainActivity.this, "بیک اپ Downloads فولڈر میں محفوظ ہو گیا", Toast.LENGTH_LONG).show());
            } catch (Exception e) {
                runOnUiThread(() -> Toast.makeText(MainActivity.this, "بیک اپ محفوظ نہیں ہو سکا۔ دوبارہ کوشش کریں", Toast.LENGTH_LONG).show());
            }
        }
    }
    @Override protected void onActivityResult(int requestCode, int resultCode, Intent data) {
        super.onActivityResult(requestCode, resultCode, data);
        if (requestCode == FILE_CHOOSER_REQUEST && fileCallback != null) {
            Uri[] results = WebChromeClient.FileChooserParams.parseResult(resultCode, data);
            fileCallback.onReceiveValue(results);
            fileCallback = null;
        }
    }
    @Override public void onBackPressed() {
        if (webView != null && webView.canGoBack()) webView.goBack(); else super.onBackPressed();
    }
}
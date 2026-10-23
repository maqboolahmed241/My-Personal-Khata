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
import android.provider.DocumentsContract;
import android.widget.Toast;
import android.graphics.Color;
import android.content.SharedPreferences;
import java.io.OutputStream;
import java.io.IOException;

public class MainActivity extends Activity {
    private WebView webView;
    private ValueCallback<Uri[]> fileCallback;
    private static final int FILE_CHOOSER_REQUEST = 101;
    private static final int BACKUP_FOLDER_REQUEST = 102;
    private static final String PREFS = "khata_settings";
    private static final String BACKUP_TREE_URI = "backup_tree_uri";
    private String pendingBackupContent;
    private String pendingBackupFilename;

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
            pendingBackupContent = content;
            pendingBackupFilename = filename.replaceAll("[^a-zA-Z0-9._-]", "_");
            runOnUiThread(() -> saveBackupOrAskForFolder());
        }

        @JavascriptInterface
        public void chooseBackupFolder() {
            runOnUiThread(() -> {
                pendingBackupContent = null;
                pendingBackupFilename = null;
                openBackupFolderPicker();
            });
        }
    }

    private void saveBackupOrAskForFolder() {
        String saved = getSharedPreferences(PREFS, MODE_PRIVATE).getString(BACKUP_TREE_URI, null);
        if (saved == null) {
            Toast.makeText(this, "بیک اپ فولڈر منتخب کریں، چاہیں تو نیا Maqbool Ahmed فولڈر بنائیں", Toast.LENGTH_LONG).show();
            openBackupFolderPicker();
            return;
        }
        try {
            Uri treeUri = Uri.parse(saved);
            saveIntoSelectedFolder(treeUri, pendingBackupContent, pendingBackupFilename);
            pendingBackupContent = null;
            pendingBackupFilename = null;
            Toast.makeText(this, "بیک اپ منتخب فولڈر میں محفوظ ہو گیا", Toast.LENGTH_LONG).show();
        } catch (Exception e) {
            getSharedPreferences(PREFS, MODE_PRIVATE).edit().remove(BACKUP_TREE_URI).apply();
            Toast.makeText(this, "پرانا فولڈر دستیاب نہیں۔ براہ کرم دوبارہ فولڈر منتخب کریں", Toast.LENGTH_LONG).show();
            openBackupFolderPicker();
        }
    }

    private void openBackupFolderPicker() {
        Intent intent = new Intent(Intent.ACTION_OPEN_DOCUMENT_TREE);
        intent.addFlags(Intent.FLAG_GRANT_READ_URI_PERMISSION
                | Intent.FLAG_GRANT_WRITE_URI_PERMISSION
                | Intent.FLAG_GRANT_PERSISTABLE_URI_PERMISSION);
        try {
            startActivityForResult(intent, BACKUP_FOLDER_REQUEST);
        } catch (Exception e) {
            Toast.makeText(this, "فولڈر منتخب کرنے کا آپشن نہیں کھل سکا", Toast.LENGTH_LONG).show();
        }
    }

    private void saveIntoSelectedFolder(Uri treeUri, String content, String filename) throws IOException {
        if (content == null || filename == null) throw new IOException("No backup data");
        Uri directory = DocumentsContract.buildDocumentUriUsingTree(
                treeUri, DocumentsContract.getTreeDocumentId(treeUri));
        Uri fileUri = DocumentsContract.createDocument(
                getContentResolver(), directory, "application/json", filename);
        if (fileUri == null) throw new IOException("Could not create backup file");
        try (OutputStream out = getContentResolver().openOutputStream(fileUri, "w")) {
            if (out == null) throw new IOException("Could not open backup file");
            out.write(content.getBytes(java.nio.charset.StandardCharsets.UTF_8));
            out.flush();
        }
    }

    @Override protected void onActivityResult(int requestCode, int resultCode, Intent data) {
        super.onActivityResult(requestCode, resultCode, data);
        if (requestCode == FILE_CHOOSER_REQUEST && fileCallback != null) {
            Uri[] results = WebChromeClient.FileChooserParams.parseResult(resultCode, data);
            fileCallback.onReceiveValue(results);
            fileCallback = null;
        } else if (requestCode == BACKUP_FOLDER_REQUEST) {
            if (resultCode == RESULT_OK && data != null && data.getData() != null) {
                Uri treeUri = data.getData();
                int flags = data.getFlags() & (Intent.FLAG_GRANT_READ_URI_PERMISSION | Intent.FLAG_GRANT_WRITE_URI_PERMISSION);
                try {
                    getContentResolver().takePersistableUriPermission(treeUri, flags);
                    getSharedPreferences(PREFS, MODE_PRIVATE).edit().putString(BACKUP_TREE_URI, treeUri.toString()).apply();
                    if (pendingBackupContent != null && pendingBackupFilename != null) {
                        try {
                            saveIntoSelectedFolder(treeUri, pendingBackupContent, pendingBackupFilename);
                            pendingBackupContent = null;
                            pendingBackupFilename = null;
                            Toast.makeText(this, "بیک اپ منتخب فولڈر میں کامیابی سے محفوظ ہو گیا", Toast.LENGTH_LONG).show();
                        } catch (Exception e) {
                            Toast.makeText(this, "فائل محفوظ نہیں ہوئی۔ دوسرا فولڈر منتخب کر کے دوبارہ کوشش کریں", Toast.LENGTH_LONG).show();
                        }
                    } else {
                        Toast.makeText(this, "بیک اپ فولڈر منتخب ہو گیا۔ اگلا بیک اپ اسی میں محفوظ ہوگا", Toast.LENGTH_LONG).show();
                    }
                } catch (Exception e) {
                    Toast.makeText(this, "فولڈر کی اجازت محفوظ نہیں ہو سکی۔ دوبارہ منتخب کریں", Toast.LENGTH_LONG).show();
                }
            } else {
                pendingBackupContent = null;
                pendingBackupFilename = null;
                Toast.makeText(this, "فولڈر منتخب نہیں کیا گیا، بیک اپ محفوظ نہیں ہوا", Toast.LENGTH_LONG).show();
            }
        }
    }

    @Override public void onBackPressed() {
        if (webView != null && webView.canGoBack()) webView.goBack(); else super.onBackPressed();
    }
}
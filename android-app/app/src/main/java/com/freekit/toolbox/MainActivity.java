package com.freekit.toolbox;

import android.annotation.SuppressLint;
import android.app.Activity;
import android.app.DownloadManager;
import android.content.Intent;
import android.net.Uri;
import android.os.Build;
import android.os.Bundle;
import android.os.Environment;
import android.util.Base64;
import android.view.KeyEvent;
import android.view.View;
import android.webkit.CookieManager;
import android.webkit.URLUtil;
import android.webkit.ValueCallback;
import android.webkit.WebChromeClient;
import android.webkit.WebResourceRequest;
import android.webkit.WebSettings;
import android.webkit.WebView;
import android.webkit.WebViewClient;
import android.widget.FrameLayout;
import android.widget.Toast;

import java.io.File;
import java.io.FileOutputStream;
import java.io.OutputStream;

public class MainActivity extends Activity {

    private static final int FILE_CHOOSER_REQUEST = 1001;

    private WebView webView;
    private ValueCallback<Uri[]> filePathCallback;
    private View customView;
    private WebChromeClient.CustomViewCallback customViewCallback;
    private FrameLayout containerView;

    @SuppressLint("SetJavaScriptEnabled")
    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);

        containerView = new FrameLayout(this);
        setContentView(containerView);

        webView = new WebView(this);
        containerView.addView(webView, new FrameLayout.LayoutParams(
                FrameLayout.LayoutParams.MATCH_PARENT, FrameLayout.LayoutParams.MATCH_PARENT));

        WebSettings settings = webView.getSettings();
        settings.setJavaScriptEnabled(true);
        settings.setDomStorageEnabled(true);
        settings.setDatabaseEnabled(true);
        settings.setMediaPlaybackRequiresUserGesture(false);
        settings.setAllowFileAccess(true);
        settings.setAllowContentAccess(true);
        settings.setUseWideViewPort(true);
        settings.setLoadWithOverviewMode(true);
        settings.setSupportZoom(false);
        settings.setCacheMode(WebSettings.LOAD_DEFAULT);
        settings.setMixedContentMode(WebSettings.MIXED_CONTENT_COMPATIBILITY_MODE);
        settings.setAllowUniversalAccessFromFileURLs(true);
        settings.setUserAgentString(settings.getUserAgentString() + " FreeKitToolbox/1.0 Edg/143.0.0.0");

        CookieManager.getInstance().setAcceptThirdPartyCookies(webView, true);

        webView.setWebViewClient(new WebViewClient() {
            @Override
            public boolean shouldOverrideUrlLoading(WebView view, WebResourceRequest request) {
                Uri uri = request.getUrl();
                String scheme = uri.getScheme();
                if ("http".equals(scheme) || "https".equals(scheme)) {
                    return false;
                }
                try {
                    startActivity(new Intent(Intent.ACTION_VIEW, uri));
                } catch (Exception ignored) {
                }
                return true;
            }

        });

        webView.setWebChromeClient(new WebChromeClient() {
            @Override
            public boolean onConsoleMessage(android.webkit.ConsoleMessage cm) {
                try {
                    android.util.Log.i("FreeKitJS", cm.messageLevel() + " " + cm.message() + " @" + cm.lineNumber());
                } catch (Exception ignored) {
                }
                return true;
            }

            @Override
            public boolean onShowFileChooser(WebView view, ValueCallback<Uri[]> callback,
                                             FileChooserParams params) {
                if (filePathCallback != null) {
                    filePathCallback.onReceiveValue(null);
                }
                filePathCallback = callback;
                try {
                    startActivityForResult(params.createIntent(), FILE_CHOOSER_REQUEST);
                } catch (Exception e) {
                    filePathCallback = null;
                    callback.onReceiveValue(null);
                }
                return true;
            }

            @Override
            public void onShowCustomView(View view, CustomViewCallback callback) {
                if (customView != null) {
                    callback.onCustomViewHidden();
                    return;
                }
                customView = view;
                customViewCallback = callback;
                webView.setVisibility(View.GONE);
                containerView.addView(customView, new FrameLayout.LayoutParams(
                        FrameLayout.LayoutParams.MATCH_PARENT, FrameLayout.LayoutParams.MATCH_PARENT));
            }

            @Override
            public void onHideCustomView() {
                exitCustomView();
            }
        });

        webView.setDownloadListener((url, userAgent, contentDisposition, mimeType, contentLength) -> {
            try {
                DownloadManager.Request req = new DownloadManager.Request(Uri.parse(url));
                req.setMimeType(mimeType);
                req.addRequestHeader("User-Agent", userAgent);
                req.setNotificationVisibility(DownloadManager.Request.VISIBILITY_VISIBLE_NOTIFY_COMPLETED);
                String fileName = URLUtil.guessFileName(url, contentDisposition, mimeType);
                req.setDestinationInExternalPublicDir(Environment.DIRECTORY_DOWNLOADS, fileName);
                ((DownloadManager) getSystemService(DOWNLOAD_SERVICE)).enqueue(req);
                Toast.makeText(this, "开始下载: " + fileName, Toast.LENGTH_SHORT).show();
            } catch (Exception e) {
                Toast.makeText(this, "下载失败", Toast.LENGTH_SHORT).show();
            }
        });

        webView.addJavascriptInterface(new Bridge(), "AndroidBridge");

        webView.loadUrl("file:///android_asset/index.html");
    }

    private void exitCustomView() {
        if (customView == null) return;
        containerView.removeView(customView);
        customView = null;
        if (customViewCallback != null) {
            customViewCallback.onCustomViewHidden();
            customViewCallback = null;
        }
        webView.setVisibility(View.VISIBLE);
    }

    @Override
    protected void onActivityResult(int requestCode, int resultCode, Intent data) {
        super.onActivityResult(requestCode, resultCode, data);
        if (requestCode == FILE_CHOOSER_REQUEST && filePathCallback != null) {
            Uri[] results = null;
            if (resultCode == RESULT_OK && data != null && data.getData() != null) {
                results = new Uri[]{data.getData()};
            }
            filePathCallback.onReceiveValue(results);
            filePathCallback = null;
        }
    }

    @Override
    public boolean onKeyDown(int keyCode, KeyEvent event) {
        if (keyCode == KeyEvent.KEYCODE_BACK && customView != null) {
            exitCustomView();
            return true;
        }
        if (keyCode == KeyEvent.KEYCODE_BACK && webView.canGoBack()) {
            webView.goBack();
            return true;
        }
        return super.onKeyDown(keyCode, event);
    }

    public class Bridge {

        @android.webkit.JavascriptInterface
        public void saveFile(String name, String base64Data) {
            try {
                String clean = base64Data;
                int comma = clean.indexOf(',');
                if (comma >= 0) clean = clean.substring(comma + 1);
                byte[] bytes = Base64.decode(clean, Base64.DEFAULT);

                File dir = Environment.getExternalStoragePublicDirectory(Environment.DIRECTORY_DOWNLOADS);
                if (!dir.exists()) dir.mkdirs();
                File out = new File(dir, sanitize(name));
                try (OutputStream os = new FileOutputStream(out)) {
                    os.write(bytes);
                }
                runOnUiThread(() -> Toast.makeText(MainActivity.this,
                        "已保存到下载目录: " + out.getName(), Toast.LENGTH_SHORT).show());
            } catch (Exception e) {
                runOnUiThread(() -> Toast.makeText(MainActivity.this,
                        "保存失败: " + e.getMessage(), Toast.LENGTH_SHORT).show());
            }
        }

        private String sanitize(String name) {
            if (name == null || name.trim().isEmpty()) return "download.bin";
            return name.replaceAll("[\\\\/:*?\"<>|]", "_");
        }
    }
}

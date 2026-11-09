package cn.roadsense.app;

import android.app.Activity;
import android.graphics.Bitmap;
import android.content.Intent;
import android.net.Uri;
import android.os.Bundle;
import android.view.KeyEvent;
import android.view.View;
import android.webkit.CookieManager;
import android.webkit.SslError;
import android.webkit.SslErrorHandler;
import android.webkit.WebChromeClient;
import android.webkit.WebResourceError;
import android.webkit.WebResourceRequest;
import android.webkit.WebSettings;
import android.webkit.WebView;
import android.webkit.WebViewClient;
import android.widget.ProgressBar;
import android.widget.Toast;

/**
 * 路感 RoadSense · Android 壳
 * 纯 Android SDK（不依赖 AndroidX），WebView 承载线上三端工作台。
 * 数据、账号、实时同步全部由后端负责，App 只负责全屏容器与原生交互。
 */
public class MainActivity extends Activity {

    /* 换成你自己的部署地址时，只改这里；同时记得同步 manifest.json 的 start_url */
    private static final String BASE_URL =
            "https://roadsense-v2x.app.workbuddy.host/app.html?sharecode=SOFDUZBXLY";
    private static final String HOST = "roadsense-v2x.app.workbuddy.host";

    private WebView web;
    private ProgressBar bar;
    private View offline;
    private long lastBack = 0L;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);

        web = findViewById(R.id.web);
        bar = findViewById(R.id.bar);
        offline = findViewById(R.id.offline);
        findViewById(R.id.btnRetry).setOnClickListener(new View.OnClickListener() {
            @Override public void onClick(View v) {
                offline.setVisibility(View.GONE);
                web.setVisibility(View.VISIBLE);
                web.reload();
            }
        });

        WebSettings s = web.getSettings();
        s.setJavaScriptEnabled(true);
        s.setDomStorageEnabled(true);                 // 共享身份 rs_auth_v1 与云端 token 都存这里
        s.setDatabaseEnabled(true);
        s.setCacheMode(WebSettings.LOAD_DEFAULT);
        s.setMixedContentMode(WebSettings.MIXED_CONTENT_NEVER_ALLOW);
        s.setUseWideViewPort(true);
        s.setLoadWithOverviewMode(false);
        s.setJavaScriptCanOpenWindowsAutomatically(false);
        s.setSupportMultipleWindows(false);
        s.setMediaPlaybackRequiresUserGesture(true);
        s.setUserAgentString(s.getUserAgentString() + " RoadSenseApp/2.0");

        CookieManager cm = CookieManager.getInstance();
        cm.setAcceptCookie(true);                     // 托管网关用 cookie 放行后续请求
        cm.setAcceptThirdPartyCookies(web, true);

        web.setWebViewClient(new WebViewClient() {

            /* API 24+ */
            @Override
            public boolean shouldOverrideUrlLoading(WebView v, WebResourceRequest r) {
                return handleUrl(r.getUrl());
            }

            /* API 21~23 兼容 */
            @Override
            public boolean shouldOverrideUrlLoading(WebView v, String url) {
                return handleUrl(Uri.parse(url));
            }

            private boolean handleUrl(Uri u) {
                if (u == null) return true;
                if (HOST.equals(u.getHost())) return false;      // 站内留在 WebView
                try {                                            // 站外用系统浏览器
                    startActivity(new Intent(Intent.ACTION_VIEW, u));
                } catch (Exception ignored) {}
                return true;
            }

            @Override
            public void onPageStarted(WebView v, String url, Bitmap favicon) {
                bar.setVisibility(View.VISIBLE);
            }

            @Override
            public void onPageFinished(WebView v, String url) {
                bar.setVisibility(View.GONE);
            }

            /* API 23+ */
            @Override
            public void onReceivedError(WebView v, WebResourceRequest r, WebResourceError e) {
                if (r.isForMainFrame()) showOffline();
            }

            /* API 21~22 兼容 */
            @Override
            public void onReceivedError(WebView v, int code, String desc, String failingUrl) {
                if (failingUrl != null && failingUrl.equals(BASE_URL)) showOffline();
            }

            /* 证书异常一律拒绝，不静默放行 */
            @Override
            public void onReceivedSslError(WebView v, SslErrorHandler handler, SslError error) {
                handler.cancel();
                showOffline();
            }
        });

        web.setWebChromeClient(new WebChromeClient() {
            @Override public void onProgressChanged(WebView v, int progress) {
                bar.setProgress(progress);
            }
        });

        web.setDownloadListener((url, userAgent, contentDisposition, mimeType, length) -> {
            try { startActivity(new Intent(Intent.ACTION_VIEW, Uri.parse(url))); }
            catch (Exception ignored) {}
        });

        web.loadUrl(BASE_URL);
    }

    private void showOffline() {
        web.setVisibility(View.GONE);
        bar.setVisibility(View.GONE);
        offline.setVisibility(View.VISIBLE);
    }

    @Override
    public boolean onKeyDown(int keyCode, KeyEvent event) {
        if (keyCode == KeyEvent.KEYCODE_BACK) {
            if (web.getVisibility() == View.GONE) { finish(); return true; }
            if (web.canGoBack()) { web.goBack(); return true; }
            long now = System.currentTimeMillis();
            if (now - lastBack < 2000L) { finish(); }
            else {
                lastBack = now;
                Toast.makeText(this, "再按一次退出路感", Toast.LENGTH_SHORT).show();
            }
            return true;
        }
        return super.onKeyDown(keyCode, event);
    }
}

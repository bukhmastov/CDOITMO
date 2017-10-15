package com.bukhmastov.cdoitmo.activities;

import android.app.Activity;
import android.content.Intent;
import android.graphics.Bitmap;
import android.os.Bundle;
import android.support.v4.widget.SwipeRefreshLayout;
import android.support.v7.app.ActionBar;
import android.support.v7.app.AppCompatActivity;
import android.support.v7.widget.Toolbar;
import android.view.MenuItem;
import android.view.View;
import android.webkit.WebChromeClient;
import android.webkit.WebResourceRequest;
import android.webkit.WebView;
import android.webkit.WebViewClient;
import android.widget.ProgressBar;

import com.bukhmastov.cdoitmo.R;
import com.bukhmastov.cdoitmo.firebase.FirebaseAnalyticsProvider;
import com.bukhmastov.cdoitmo.utils.Log;
import com.bukhmastov.cdoitmo.utils.Static;

public class WebViewActivity extends AppCompatActivity {

    private static final String TAG = "WebViewActivity";
    private final Activity activity = this;
    private String url = null;
    private String title = null;
    private WebView webview = null;
    private ProgressBar webviewProgressBar = null;
    private SwipeRefreshLayout swipe = null;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        Static.applyActivityTheme(this);
        super.onCreate(savedInstanceState);
        Log.i(TAG, "Activity created");
        try {
            Intent intent = getIntent();
            if (intent == null) throw new NullPointerException("intent is null");
            Bundle extras = intent.getExtras();
            if (extras == null) throw new NullPointerException("extras are null");
            url = extras.getString("url");
            if (url == null) throw new NullPointerException("url is null");
            title = extras.getString("title");
        } catch (Exception e) {
            finish();
        }
        FirebaseAnalyticsProvider.logCurrentScreen(this);
        setContentView(R.layout.activity_webview);
        Toolbar toolbar = findViewById(R.id.toolbar_webview);
        if (toolbar != null) {
            Static.applyToolbarTheme(activity, toolbar);
            setSupportActionBar(toolbar);
        }
        ActionBar actionBar = getSupportActionBar();
        if (actionBar != null) {
            actionBar.setHomeButtonEnabled(true);
            actionBar.setDisplayHomeAsUpEnabled(true);
            actionBar.setTitle(title == null ? activity.getString(R.string.web_browser) : title);
        }
        // инициализируем
        webview = findViewById(R.id.webview);
        webviewProgressBar = findViewById(R.id.webviewProgressBar);
        swipe = findViewById(R.id.swipe);
        // работаем со свайпом
        if (swipe != null) {
            swipe.setColorSchemeColors(Static.colorAccent);
            swipe.setProgressBackgroundColorSchemeColor(Static.colorBackgroundRefresh);
            swipe.setOnRefreshListener(new SwipeRefreshLayout.OnRefreshListener() {
                @Override
                public void onRefresh() {
                    if (swipe != null && swipe.isRefreshing()) {
                        swipe.setRefreshing(false);
                    }
                    if (webview != null) {
                        webview.stopLoading();
                        webview.reload();
                    }
                }
            });
        }
        // загружаем
        if (webview != null) {
            webview.setWebViewClient(new WebViewClient() {
                @Override
                public boolean shouldOverrideUrlLoading(WebView view, WebResourceRequest request) {
                    try {
                        startActivity(new Intent(Intent.ACTION_VIEW, request.getUrl()));
                    } catch (Exception e) {
                        Static.snackBar(activity, activity.getString(R.string.something_went_wrong));
                    }
                    return true;
                }
                @Override
                public void onPageStarted(WebView view, String url, Bitmap favicon) {
                    if (webviewProgressBar != null) {
                        webviewProgressBar.setVisibility(View.VISIBLE);
                    }
                }
                @Override
                public void onPageFinished(WebView view, String url) {
                    if (webviewProgressBar != null) {
                        webviewProgressBar.setVisibility(View.GONE);
                    }
                }
            });
            webview.setWebChromeClient(new WebChromeClient() {
                @Override
                public void onProgressChanged(WebView view, int progress) {
                    if (webviewProgressBar != null) {
                        webviewProgressBar.setProgress(progress);
                    }
                }
            });
            webview.loadUrl(url);
        }
    }

    @Override
    protected void onDestroy() {
        super.onDestroy();
        Log.i(TAG, "Activity destroyed");
    }

    @Override
    public boolean onOptionsItemSelected(MenuItem item) {
        switch (item.getItemId()) {
            case android.R.id.home: finish(); return true;
            default: return super.onOptionsItemSelected(item);
        }
    }

    @Override
    public void onBackPressed() {
        if (webview.canGoBack()) {
            webview.goBack();
        } else {
            super.onBackPressed();
        }
    }
}

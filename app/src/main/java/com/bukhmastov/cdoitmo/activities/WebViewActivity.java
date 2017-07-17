package com.bukhmastov.cdoitmo.activities;

import android.content.Intent;
import android.os.Bundle;
import android.support.v7.app.ActionBar;
import android.support.v7.app.AppCompatActivity;
import android.support.v7.widget.Toolbar;
import android.view.MenuItem;
import android.webkit.WebResourceRequest;
import android.webkit.WebView;
import android.webkit.WebViewClient;

import com.bukhmastov.cdoitmo.R;
import com.bukhmastov.cdoitmo.firebase.FirebaseAnalyticsProvider;
import com.bukhmastov.cdoitmo.utils.Log;
import com.bukhmastov.cdoitmo.utils.Static;

public class WebViewActivity extends AppCompatActivity {

    private static final String TAG = "WebViewActivity";
    private String url = null;
    private String title = null;
    private WebView webview = null;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        if (Static.darkTheme) setTheme(R.style.AppTheme_Dark);
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
        setSupportActionBar((Toolbar) findViewById(R.id.toolbar_webview));
        ActionBar actionBar = getSupportActionBar();
        if (actionBar != null) {
            actionBar.setHomeButtonEnabled(true);
            actionBar.setDisplayHomeAsUpEnabled(true);
            actionBar.setTitle(title == null ? getString(R.string.web_browser) : title);
        }
        webview = (WebView) findViewById(R.id.webview);
        if (webview != null) {
            webview.loadUrl(url);
            webview.setWebViewClient(new MyWebViewClient());
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

    private class MyWebViewClient extends WebViewClient {
        @Override
        public boolean shouldOverrideUrlLoading(WebView view, WebResourceRequest request) {
            Intent intent = new Intent(getBaseContext(), WebViewActivity.class);
            Bundle extras = new Bundle();
            extras.putString("url", request.getUrl().toString());
            intent.putExtras(extras);
            startActivity(intent);
            return true;
        }
    }

}

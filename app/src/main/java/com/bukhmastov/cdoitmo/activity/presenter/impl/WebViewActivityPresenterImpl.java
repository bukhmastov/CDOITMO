package com.bukhmastov.cdoitmo.activity.presenter.impl;

import android.content.Intent;
import android.graphics.Bitmap;
import android.os.Bundle;
import android.support.annotation.NonNull;
import android.support.annotation.Nullable;
import android.support.v4.widget.SwipeRefreshLayout;
import android.support.v7.app.ActionBar;
import android.support.v7.widget.Toolbar;
import android.view.MenuItem;
import android.view.View;
import android.webkit.WebChromeClient;
import android.webkit.WebResourceRequest;
import android.webkit.WebView;
import android.webkit.WebViewClient;
import android.widget.ProgressBar;

import com.bukhmastov.cdoitmo.R;
import com.bukhmastov.cdoitmo.activity.WebViewActivity;
import com.bukhmastov.cdoitmo.activity.presenter.WebViewActivityPresenter;
import com.bukhmastov.cdoitmo.event.bus.EventBus;
import com.bukhmastov.cdoitmo.event.events.OpenIntentEvent;
import com.bukhmastov.cdoitmo.factory.AppComponentProvider;
import com.bukhmastov.cdoitmo.firebase.FirebaseAnalyticsProvider;
import com.bukhmastov.cdoitmo.util.Log;
import com.bukhmastov.cdoitmo.util.NotificationMessage;
import com.bukhmastov.cdoitmo.util.Theme;
import com.bukhmastov.cdoitmo.util.Thread;
import com.bukhmastov.cdoitmo.util.singleton.Color;

import javax.inject.Inject;

public class WebViewActivityPresenterImpl implements WebViewActivityPresenter {

    private static final String TAG = "WebViewActivity";
    private WebViewActivity activity = null;
    private String url = null;
    private String title = null;
    private WebView webview = null;
    private ProgressBar webviewProgressBar = null;
    private SwipeRefreshLayout swipe = null;

    @Inject
    Log log;
    @Inject
    Thread thread;
    @Inject
    EventBus eventBus;
    @Inject
    NotificationMessage notificationMessage;
    @Inject
    Theme theme;
    @Inject
    FirebaseAnalyticsProvider firebaseAnalyticsProvider;

    public WebViewActivityPresenterImpl() {
        AppComponentProvider.getComponent().inject(this);
    }

    @Override
    public void setActivity(@NonNull WebViewActivity activity) {
        this.activity = activity;
    }

    @Override
    public void onCreate(@Nullable Bundle savedInstanceState) {
        thread.runOnUI(() -> {
            log.i(TAG, "Activity created");
            try {
                Intent intent = activity.getIntent();
                if (intent == null) throw new NullPointerException("intent is null");
                Bundle extras = intent.getExtras();
                if (extras == null) throw new NullPointerException("extras are null");
                url = extras.getString("url");
                if (url == null) throw new NullPointerException("url is null");
                title = extras.getString("title");
            } catch (Exception e) {
                activity.finish();
            }
            firebaseAnalyticsProvider.logCurrentScreen(activity);
            Toolbar toolbar = activity.findViewById(R.id.toolbar_webview);
            if (toolbar != null) {
                theme.applyToolbarTheme(activity, toolbar);
                activity.setSupportActionBar(toolbar);
            }
            ActionBar actionBar = activity.getSupportActionBar();
            if (actionBar != null) {
                actionBar.setHomeButtonEnabled(true);
                actionBar.setDisplayHomeAsUpEnabled(true);
                actionBar.setTitle(title == null ? activity.getString(R.string.web_browser) : title);
            }
            // инициализируем
            webview = activity.findViewById(R.id.webview);
            webviewProgressBar = activity.findViewById(R.id.webviewProgressBar);
            swipe = activity.findViewById(R.id.swipe);
            // работаем со свайпом
            if (swipe != null) {
                swipe.setColorSchemeColors(Color.resolve(activity, R.attr.colorAccent));
                swipe.setProgressBackgroundColorSchemeColor(Color.resolve(activity, R.attr.colorBackgroundRefresh));
                swipe.setOnRefreshListener(() -> {
                    if (swipe != null && swipe.isRefreshing()) {
                        swipe.setRefreshing(false);
                    }
                    if (webview != null) {
                        webview.stopLoading();
                        webview.reload();
                    }
                });
            }
            // загружаем
            if (webview != null) {
                webview.setWebViewClient(new WebViewClient() {
                    @Override
                    public boolean shouldOverrideUrlLoading(WebView view, WebResourceRequest request) {
                        eventBus.fire(new OpenIntentEvent(new Intent(Intent.ACTION_VIEW, request.getUrl())));
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
        });
    }

    @Override
    public void onDestroy() {
        log.i(TAG, "Activity destroyed");
    }

    @Override
    public boolean onToolbarSelected(MenuItem item) {
        switch (item.getItemId()) {
            case android.R.id.home:
                activity.finish();
                return false;
            default:
                return true;
        }
    }

    @Override
    public boolean onBackPressed() {
        if (webview.canGoBack()) {
            webview.goBack();
            return false;
        } else {
            return true;
        }
    }
}

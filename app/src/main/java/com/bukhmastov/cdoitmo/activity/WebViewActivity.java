package com.bukhmastov.cdoitmo.activity;

import android.os.Bundle;
import android.view.MenuItem;

import com.bukhmastov.cdoitmo.R;
import com.bukhmastov.cdoitmo.activity.presenter.WebViewActivityPresenter;
import com.bukhmastov.cdoitmo.factory.AppComponentProvider;
import com.bukhmastov.cdoitmo.util.Theme;

import javax.inject.Inject;

public class WebViewActivity extends BaseActivity {

    @Inject
    WebViewActivityPresenter presenter;
    @Inject
    Theme theme;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        AppComponentProvider.getComponent().inject(this);
        presenter.setActivity(this);
        theme.applyActivityTheme(this);
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_webview);
        presenter.onCreate(savedInstanceState);
    }

    @Override
    protected void onDestroy() {
        super.onDestroy();
        presenter.onDestroy();
    }

    @Override
    public boolean onOptionsItemSelected(MenuItem item) {
        if (android.R.id.home == item.getItemId()) {
            if (presenter.onBackPressed()) {
                finish();
            }
            return true;
        }
        if (presenter.onToolbarSelected(item)) {
            return true;
        }
        return super.onOptionsItemSelected(item);
    }

    @Override
    public void onBackPressed() {
        if (presenter.onBackPressed()) {
            super.onBackPressed();
        }
    }
}

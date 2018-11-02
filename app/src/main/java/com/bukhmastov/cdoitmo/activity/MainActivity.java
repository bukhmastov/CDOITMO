package com.bukhmastov.cdoitmo.activity;

import android.os.Bundle;
import android.view.Menu;
import android.view.MenuItem;

import com.bukhmastov.cdoitmo.R;
import com.bukhmastov.cdoitmo.activity.presenter.MainActivityPresenter;
import com.bukhmastov.cdoitmo.factory.AppComponentProvider;
import com.bukhmastov.cdoitmo.util.Theme;

import javax.inject.Inject;

public class MainActivity extends ConnectedActivity {

    @Inject
    MainActivityPresenter presenter;
    @Inject
    Theme theme;

    @Override
    protected void onCreate(final Bundle savedInstanceState) {
        AppComponentProvider.getComponent().inject(this);
        presenter.setActivity(this);
        theme.applyActivityTheme(activity);
        super.onCreate(savedInstanceState);
        if (presenter.isInitialized()) {
            setContentView(R.layout.activity_main);
        }
        presenter.onCreate(savedInstanceState);
    }

    @Override
    protected void onResume() {
        super.onResume();
        presenter.onResume();
    }

    @Override
    protected void onPause() {
        super.onPause();
        presenter.onPause();
    }

    @Override
    protected void onDestroy() {
        super.onDestroy();
        presenter.onDestroy();
    }

    @Override
    public void onBackPressed() {
        if (presenter.onBackButtonPressed()) {
            super.onBackPressed();
        }
    }

    @Override
    public boolean onCreateOptionsMenu(Menu menu) {
        getMenuInflater().inflate(R.menu.toolbar, menu);
        toolbar = menu;
        presenter.onToolbarSetup();
        return true;
    }

    @Override
    public boolean onOptionsItemSelected(MenuItem item) {
        return presenter.onToolbarSelected(item);
    }

    @Override
    public void onSaveInstanceState(Bundle savedInstanceState) {
        presenter.onSaveInstanceState(savedInstanceState);
        super.onSaveInstanceState(savedInstanceState);
    }

    @Override
    public int getRootViewId() {
        return R.id.activity_main;
    }
}

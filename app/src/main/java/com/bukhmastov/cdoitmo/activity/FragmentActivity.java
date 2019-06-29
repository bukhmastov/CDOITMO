package com.bukhmastov.cdoitmo.activity;

import android.os.Bundle;
import androidx.annotation.Nullable;
import android.view.Menu;
import android.view.MenuItem;

import com.bukhmastov.cdoitmo.R;
import com.bukhmastov.cdoitmo.activity.presenter.FragmentActivityPresenter;
import com.bukhmastov.cdoitmo.factory.AppComponentProvider;
import com.bukhmastov.cdoitmo.util.Static;
import com.bukhmastov.cdoitmo.util.Theme;

import javax.inject.Inject;

public class FragmentActivity extends ConnectedActivity {

    @Inject
    FragmentActivityPresenter presenter;
    @Inject
    Theme theme;
    @Inject
    Static staticUtil;

    @Override
    protected void onCreate(@Nullable Bundle savedInstanceState) {
        AppComponentProvider.getComponent().inject(this);
        super.onCreate(savedInstanceState);
        presenter.setActivity(this);
        theme.applyActivityTheme(this);
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
    public int getRootViewId() {
        return R.id.activity_fragment;
    }

    @Override
    protected String getLogTag() {
        return "FragmentActivity";
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

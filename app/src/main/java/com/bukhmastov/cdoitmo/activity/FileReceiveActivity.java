package com.bukhmastov.cdoitmo.activity;

import android.os.Bundle;
import android.support.annotation.Nullable;
import android.view.MenuItem;

import com.bukhmastov.cdoitmo.R;
import com.bukhmastov.cdoitmo.activity.presenter.FileReceiveActivityPresenter;
import com.bukhmastov.cdoitmo.factory.AppComponentProvider;
import com.bukhmastov.cdoitmo.util.Theme;

import javax.inject.Inject;

public class FileReceiveActivity extends ConnectedActivity {

    @Inject
    FileReceiveActivityPresenter presenter;
    @Inject
    Theme theme;

    @Override
    protected void onCreate(@Nullable Bundle savedInstanceState) {
        AppComponentProvider.getComponent().inject(this);
        presenter.setActivity(this);
        theme.applyActivityTheme(this);
        super.onCreate(savedInstanceState);
        presenter.onCreate(savedInstanceState);
    }

    @Override
    public void onBackPressed() {
        if (presenter.onBackPressed()) {
            super.onBackPressed();
        }
    }

    @Override
    public boolean onOptionsItemSelected(MenuItem item) {
        return !presenter.onToolbarSelected(item) || super.onOptionsItemSelected(item);
    }

    @Override
    public int getRootViewId() {
        return R.id.activity_file_receive;
    }
}

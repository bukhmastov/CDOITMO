package com.bukhmastov.cdoitmo.activity;

import android.os.Bundle;

import com.bukhmastov.cdoitmo.R;
import com.bukhmastov.cdoitmo.activity.presenter.UniversityPersonCardActivityPresenter;
import com.bukhmastov.cdoitmo.factory.AppComponentProvider;
import com.bukhmastov.cdoitmo.util.Theme;

import javax.inject.Inject;

public class UniversityPersonCardActivity extends ConnectedActivity {

    @Inject
    UniversityPersonCardActivityPresenter presenter;
    @Inject
    Theme theme;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        AppComponentProvider.getComponent().inject(this);
        presenter.setActivity(this);
        switch (theme.getAppTheme(this)) {
            case "light":
            default: setTheme(R.style.AppTheme_TransparentStatusBar); break;
            case "dark": setTheme(R.style.AppTheme_Dark_TransparentStatusBar); break;
            case "white": setTheme(R.style.AppTheme_White_TransparentStatusBar); break;
            case "black": setTheme(R.style.AppTheme_Black_TransparentStatusBar); break;
        }
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_university_person_card);
        presenter.onCreate(savedInstanceState);
    }

    @Override
    public void onDestroy() {
        super.onDestroy();
        presenter.onDestroy();
    }

    @Override
    public void onResume() {
        super.onResume();
        presenter.onResume();
    }

    @Override
    public void onPause() {
        super.onPause();
        presenter.onPause();
    }

    @Override
    public int getRootViewId() {
        return R.id.person_common_container;
    }

    @Override
    protected String getLogTag() {
        return "UniversityPersonCardActivity";
    }
}

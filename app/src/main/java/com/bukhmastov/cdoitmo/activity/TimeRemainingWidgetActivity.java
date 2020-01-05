package com.bukhmastov.cdoitmo.activity;

import android.content.Context;
import android.os.Bundle;

import com.bukhmastov.cdoitmo.R;
import com.bukhmastov.cdoitmo.activity.presenter.TimeRemainingWidgetActivityPresenter;
import com.bukhmastov.cdoitmo.factory.AppComponentProvider;
import com.bukhmastov.cdoitmo.util.StoragePref;
import com.bukhmastov.cdoitmo.util.Theme;
import com.bukhmastov.cdoitmo.util.singleton.CtxWrapper;

import javax.inject.Inject;

public class TimeRemainingWidgetActivity extends BaseActivity {

    @Inject
    TimeRemainingWidgetActivityPresenter presenter;
    @Inject
    StoragePref storagePref;
    @Inject
    Theme theme;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        AppComponentProvider.getComponent().inject(this);
        presenter.setActivity(this);
        switch (theme.getAppTheme(this)) {
            case "light":
            default: setTheme(R.style.AppTheme_Popup); break;
            case "dark": setTheme(R.style.AppTheme_Popup_Dark); break;
            case "white": setTheme(R.style.AppTheme_Popup_White); break;
            case "black": setTheme(R.style.AppTheme_Popup_Black); break;
        }
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_widget_remaining);
        presenter.onCreate(savedInstanceState);
    }

    @Override
    protected void onDestroy() {
        super.onDestroy();
        presenter.onDestroy();
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
    protected void attachBaseContext(Context context) {
        AppComponentProvider.getComponent().inject(this);
        super.attachBaseContext(CtxWrapper.wrap(context, storagePref));
    }
}

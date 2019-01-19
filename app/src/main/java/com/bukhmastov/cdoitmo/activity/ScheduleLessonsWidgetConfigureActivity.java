package com.bukhmastov.cdoitmo.activity;

import android.content.Context;
import android.os.Bundle;

import com.bukhmastov.cdoitmo.R;
import com.bukhmastov.cdoitmo.activity.presenter.ScheduleLessonsWidgetConfigureActivityPresenter;
import com.bukhmastov.cdoitmo.factory.AppComponentProvider;
import com.bukhmastov.cdoitmo.util.Log;
import com.bukhmastov.cdoitmo.util.StoragePref;
import com.bukhmastov.cdoitmo.util.TextUtils;
import com.bukhmastov.cdoitmo.util.Theme;
import com.bukhmastov.cdoitmo.util.singleton.CtxWrapper;

import javax.inject.Inject;

import androidx.appcompat.app.AppCompatActivity;

public class ScheduleLessonsWidgetConfigureActivity extends AppCompatActivity {

    @Inject
    ScheduleLessonsWidgetConfigureActivityPresenter presenter;
    @Inject
    Theme theme;
    @Inject
    Log log;
    @Inject
    StoragePref storagePref;
    @Inject
    TextUtils textUtils;

    @Override
    public void onCreate(Bundle savedInstanceState) {
        AppComponentProvider.getComponent().inject(this);
        presenter.setActivity(this);
        theme.applyActivityTheme(this);
        super.onCreate(savedInstanceState);
        setContentView(R.layout.widget_configure_schedule_lessons);
        presenter.onCreate(savedInstanceState);
    }

    @Override
    protected void onDestroy() {
        super.onDestroy();
        presenter.onDestroy();
    }

    @Override
    protected void attachBaseContext(Context context) {
        AppComponentProvider.getComponent().inject(this);
        super.attachBaseContext(CtxWrapper.wrap(context, storagePref, log, textUtils));
    }
}

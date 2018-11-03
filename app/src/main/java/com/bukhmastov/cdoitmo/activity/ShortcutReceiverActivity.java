package com.bukhmastov.cdoitmo.activity;

import android.app.Activity;
import android.content.Context;
import android.os.Bundle;

import com.bukhmastov.cdoitmo.activity.presenter.ShortcutReceiverActivityPresenter;
import com.bukhmastov.cdoitmo.factory.AppComponentProvider;
import com.bukhmastov.cdoitmo.util.Log;
import com.bukhmastov.cdoitmo.util.StoragePref;
import com.bukhmastov.cdoitmo.util.TextUtils;
import com.bukhmastov.cdoitmo.util.singleton.CtxWrapper;

import javax.inject.Inject;

import androidx.annotation.Nullable;

public class ShortcutReceiverActivity extends Activity {

    @Inject
    ShortcutReceiverActivityPresenter presenter;
    @Inject
    Log log;
    @Inject
    StoragePref storagePref;
    @Inject
    TextUtils textUtils;

    @Override
    protected void onCreate(@Nullable Bundle savedInstanceState) {
        AppComponentProvider.getComponent().inject(this);
        presenter.setActivity(this);
        super.onCreate(savedInstanceState);
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

package com.bukhmastov.cdoitmo.activity;

import android.content.Context;
import android.os.Bundle;

import com.bukhmastov.cdoitmo.activity.presenter.ShortcutReceiverActivityPresenter;
import com.bukhmastov.cdoitmo.factory.AppComponentProvider;
import com.bukhmastov.cdoitmo.util.StoragePref;
import com.bukhmastov.cdoitmo.util.singleton.CtxWrapper;

import javax.inject.Inject;

import androidx.annotation.Nullable;

public class ShortcutReceiverActivity extends BaseActivity {

    @Inject
    ShortcutReceiverActivityPresenter presenter;
    @Inject
    StoragePref storagePref;

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
        super.attachBaseContext(CtxWrapper.wrap(context, storagePref));
    }
}

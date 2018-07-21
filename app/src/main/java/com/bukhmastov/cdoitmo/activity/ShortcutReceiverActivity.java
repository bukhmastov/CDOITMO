package com.bukhmastov.cdoitmo.activity;

import android.content.Context;
import android.content.Intent;
import android.os.Bundle;
import android.support.annotation.Nullable;
import android.support.v7.app.AppCompatActivity;

import com.bukhmastov.cdoitmo.factory.AppComponentProvider;
import com.bukhmastov.cdoitmo.receiver.ShortcutReceiver;
import com.bukhmastov.cdoitmo.util.TextUtils;
import com.bukhmastov.cdoitmo.util.singleton.CtxWrapper;
import com.bukhmastov.cdoitmo.util.Log;
import com.bukhmastov.cdoitmo.util.StoragePref;

import javax.inject.Inject;

public class ShortcutReceiverActivity extends AppCompatActivity {

    private static final String TAG = "ShortcutReceiverActivity";

    @Inject
    Log log;
    @Inject
    StoragePref storagePref;
    @Inject
    TextUtils textUtils;

    private void inject() {
        if (log == null) {
            AppComponentProvider.getComponent().inject(this);
        }
    }

    @Override
    protected void onCreate(@Nullable Bundle savedInstanceState) {
        inject();
        super.onCreate(savedInstanceState);
        Intent intent = getIntent();
        Bundle extras = intent.getExtras();
        Intent remoteIntent = new Intent();
        remoteIntent.setAction(intent.getAction());
        if (extras != null) remoteIntent.putExtras(extras);
        log.v(TAG, "onCreate | action=" + remoteIntent.getAction() + " | " + remoteIntent.toString());
        if (android.os.Build.VERSION.SDK_INT >= android.os.Build.VERSION_CODES.O) {
            new ShortcutReceiver().onReceive(this, remoteIntent);
        } else {
            sendBroadcast(remoteIntent);
        }
        finish();
    }

    @Override
    protected void attachBaseContext(Context context) {
        inject();
        super.attachBaseContext(CtxWrapper.wrap(context, storagePref, log, textUtils));
    }
}

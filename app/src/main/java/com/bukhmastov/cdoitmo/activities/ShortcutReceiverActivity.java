package com.bukhmastov.cdoitmo.activities;

import android.content.Context;
import android.content.Intent;
import android.os.Bundle;
import android.support.annotation.Nullable;
import android.support.v7.app.AppCompatActivity;

import com.bukhmastov.cdoitmo.receivers.ShortcutReceiver;
import com.bukhmastov.cdoitmo.utils.CtxWrapper;
import com.bukhmastov.cdoitmo.utils.Log;

public class ShortcutReceiverActivity extends AppCompatActivity {

    private static final String TAG = "ShortcutReceiverActivity";

    @Override
    protected void onCreate(@Nullable Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        Intent intent = getIntent();
        Bundle extras = intent.getExtras();
        Intent remoteIntent = new Intent();
        remoteIntent.setAction(intent.getAction());
        if (extras != null) remoteIntent.putExtras(extras);
        Log.v(TAG, "onCreate | action=" + remoteIntent.getAction() + " | " + remoteIntent.toString());
        if (android.os.Build.VERSION.SDK_INT >= android.os.Build.VERSION_CODES.O) {
            new ShortcutReceiver().onReceive(this, remoteIntent);
        } else {
            sendBroadcast(remoteIntent);
        }
        finish();
    }

    @Override
    protected void attachBaseContext(Context context) {
        super.attachBaseContext(CtxWrapper.wrap(context));
    }
}

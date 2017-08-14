package com.bukhmastov.cdoitmo.fragments;

import android.content.IntentFilter;
import android.os.Bundle;
import android.support.annotation.Nullable;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;

import com.bukhmastov.cdoitmo.R;
import com.bukhmastov.cdoitmo.firebase.FirebaseAnalyticsProvider;
import com.bukhmastov.cdoitmo.objects.ShortcutCreator;
import com.bukhmastov.cdoitmo.receivers.ShortcutReceiver;
import com.bukhmastov.cdoitmo.utils.Log;
import com.bukhmastov.cdoitmo.utils.Static;

public class ShortcutCreateFragment extends ConnectedFragment implements ShortcutCreator.response {

    private static final String TAG = "ShortcutCreateFragment";
    private ShortcutCreator shortcutCreator = null;
    private ShortcutReceiver receiver = new ShortcutReceiver();

    @Override
    public void onCreate(@Nullable Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        FirebaseAnalyticsProvider.logCurrentScreen(activity, this);
    }

    @Override
    public View onCreateView(LayoutInflater inflater, ViewGroup container, Bundle savedInstanceState) {
        return inflater.inflate(R.layout.fragment_shortcut_create, container, false);
    }

    @Override
    public void onResume() {
        super.onResume();
        Log.v(TAG, "Fragment resumed");
        FirebaseAnalyticsProvider.setCurrentScreen(activity, this);
        Data data = getData(activity, this.getClass());
        if (data != null) {
            activity.updateToolbar(data.title, data.image);
        }
        activity.registerReceiver(receiver, new IntentFilter(ShortcutReceiver.ACTION_INSTALL_SHORTCUT));
        if (shortcutCreator == null) shortcutCreator = new ShortcutCreator(activity, this);
        shortcutCreator.onResume();
    }

    @Override
    public void onPause() {
        super.onPause();
        Log.v(TAG, "Fragment paused");
        activity.unregisterReceiver(receiver);
        shortcutCreator.onPause();
    }

    @Override
    public void onDisplay(final View view) {
        Static.T.runOnUiThread(new Runnable() {
            @Override
            public void run() {
                Log.v(TAG, "onDisplay");
                ViewGroup shortcut_create_content = (ViewGroup) activity.findViewById(R.id.shortcut_create_content);
                if (shortcut_create_content != null) {
                    shortcut_create_content.removeAllViews();
                    shortcut_create_content.addView(view);
                }
            }
        });
    }

    @Override
    public void onError() {
        Log.v(TAG, "onError");
        activity.back();
    }

}

package com.bukhmastov.cdoitmo.fragment.settings;

import android.os.Bundle;
import android.support.v4.app.Fragment;
import android.view.View;
import android.view.ViewGroup;
import android.widget.TextView;

import com.bukhmastov.cdoitmo.R;
import com.bukhmastov.cdoitmo.firebase.FirebaseAnalyticsProvider;
import com.bukhmastov.cdoitmo.fragment.ConnectedFragment;
import com.bukhmastov.cdoitmo.object.preference.PreferenceHeader;
import com.bukhmastov.cdoitmo.util.Log;

import java.util.List;

public abstract class SettingsTemplateHeadersFragment extends ConnectedFragment {

    private boolean loaded = false;

    //@Inject
    private FirebaseAnalyticsProvider firebaseAnalyticsProvider = FirebaseAnalyticsProvider.instance();

    @Override
    public void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        Log.v(getTAG(), "Fragment created");
        firebaseAnalyticsProvider.logCurrentScreen(activity, getSelf());
    }

    @Override
    public void onDestroy() {
        super.onDestroy();
        Log.v(getTAG(), "Fragment destroyed");
        loaded = false;
    }

    @Override
    public void onResume() {
        super.onResume();
        Log.v(getTAG(), "resumed");
        firebaseAnalyticsProvider.setCurrentScreen(activity, getSelf());
        if (!loaded) {
            load();
        }
    }

    @Override
    public void onPause() {
        super.onPause();
        Log.v(getTAG(), "paused");
    }

    @Override
    protected int getLayoutId() {
        return R.layout.fragment_settings;
    }

    @Override
    protected int getRootId() {
        return R.id.settings_container;
    }

    private void load() {
        try {
            ViewGroup settings_container = activity.findViewById(getRootId());
            if (settings_container != null) {
                settings_container.removeAllViews();
                for (PreferenceHeader preferenceHeader : getPreferenceHeaders()) {
                    settings_container.addView(PreferenceHeader.getView(activity, preferenceHeader));
                }
                loaded = true;
            }
        } catch (Exception e) {
            Log.exception(e);
            failed();
        }
    }
    private void failed() {
        try {
            View view = inflate(R.layout.state_failed_button);
            ((TextView) view.findViewById(R.id.try_again_message)).setText(R.string.error_occurred);
            view.findViewById(R.id.try_again_reload).setOnClickListener(v -> {
                try {
                    ViewGroup content = activity.findViewById(android.R.id.content);
                    if (content != null) {
                        content.addView(inflate(getLayoutId()));
                        loaded = false;
                        load();
                    }
                } catch (Exception e) {
                    Log.exception(e);
                }
            });
        } catch (Exception e) {
            Log.exception(e);
        }
    }

    protected abstract List<PreferenceHeader> getPreferenceHeaders();
    protected abstract String getTAG();
    protected abstract Fragment getSelf();
}

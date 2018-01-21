package com.bukhmastov.cdoitmo.fragments.settings;

import android.content.Context;
import android.os.Bundle;
import android.support.annotation.NonNull;
import android.support.v4.app.Fragment;
import android.view.InflateException;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.TextView;

import com.bukhmastov.cdoitmo.R;
import com.bukhmastov.cdoitmo.firebase.FirebaseAnalyticsProvider;
import com.bukhmastov.cdoitmo.fragments.ConnectedFragment;
import com.bukhmastov.cdoitmo.objects.preferences.PreferenceHeader;
import com.bukhmastov.cdoitmo.utils.Log;
import com.bukhmastov.cdoitmo.utils.Static;

import java.util.List;

public abstract class SettingsTemplateHeadersFragment extends ConnectedFragment {

    private boolean loaded = false;

    @Override
    public void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        Log.v(getTAG(), "Fragment created");
        FirebaseAnalyticsProvider.logCurrentScreen(activity, getSelf());
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
        FirebaseAnalyticsProvider.setCurrentScreen(activity, getSelf());
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
    public View onCreateView(@NonNull LayoutInflater inflater, ViewGroup container, Bundle savedInstanceState) {
        return inflater.inflate(R.layout.fragment_settings, container, false);
    }

    private void load() {
        try {
            ViewGroup settings_container = activity.findViewById(R.id.settings_container);
            if (settings_container != null) {
                settings_container.removeAllViews();
                for (PreferenceHeader preferenceHeader : getPreferenceHeaders()) {
                    settings_container.addView(PreferenceHeader.getView(activity, preferenceHeader));
                }
                loaded = true;
            }
        } catch (Exception e) {
            Static.error(e);
            failed();
        }
    }
    private void failed() {
        try {
            View view = inflate(R.layout.state_try_again);
            ((TextView) view.findViewById(R.id.try_again_message)).setText(R.string.error_occurred);
            view.findViewById(R.id.try_again_reload).setOnClickListener(v -> {
                try {
                    ViewGroup content = activity.findViewById(android.R.id.content);
                    if (content != null) {
                        content.addView(inflate(R.layout.fragment_settings));
                        loaded = false;
                        load();
                    }
                } catch (Exception e) {
                    Static.error(e);
                }
            });
        } catch (Exception e) {
            Static.error(e);
        }
    }

    private View inflate(int layoutId) throws InflateException {
        return ((LayoutInflater) activity.getSystemService(Context.LAYOUT_INFLATER_SERVICE)).inflate(layoutId, null);
    }

    protected abstract List<PreferenceHeader> getPreferenceHeaders();
    protected abstract String getTAG();
    protected abstract Fragment getSelf();
}

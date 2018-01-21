package com.bukhmastov.cdoitmo.fragments.settings;

import android.content.Context;
import android.os.Bundle;
import android.support.annotation.IdRes;
import android.support.annotation.LayoutRes;
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
import com.bukhmastov.cdoitmo.objects.preferences.Preference;
import com.bukhmastov.cdoitmo.objects.preferences.PreferenceSwitch;
import com.bukhmastov.cdoitmo.utils.Log;
import com.bukhmastov.cdoitmo.utils.Static;
import com.bukhmastov.cdoitmo.utils.Storage;

import java.util.ArrayList;
import java.util.List;

public abstract class SettingsTemplatePreferencesFragment extends ConnectedFragment {

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
        return inflater.inflate(getRootLayout(), container, false);
    }

    private void load() {
        try {
            ViewGroup settings_container = activity.findViewById(getRootView());
            if (settings_container != null) {
                settings_container.removeAllViews();
                for (Preference preference : getPreferences()) {
                    settings_container.addView(Preference.getView(activity, preference));
                }
                for (Preference preference : getPreferences()) {
                    if (preference instanceof PreferenceSwitch) {
                        final PreferenceSwitch preferenceSwitch = (PreferenceSwitch) preference;
                        final ArrayList<String> dependencies = preferenceSwitch.getDependencies();
                        if (dependencies.size() > 0) {
                            PreferenceSwitch.toggleDependencies(activity, preferenceSwitch, Storage.pref.get(activity, preference.key, (Boolean) preference.defaultValue));
                            for (Preference pref : getPreferences()) {
                                if (dependencies.contains(pref.key)) {
                                    pref.setPreferenceDependency(preferenceSwitch);
                                }
                            }
                        }
                    }
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
            View view = inflate(activity, R.layout.state_try_again);
            ((TextView) view.findViewById(R.id.try_again_message)).setText(R.string.error_occurred);
            view.findViewById(R.id.try_again_reload).setOnClickListener(v -> {
                try {
                    ViewGroup content = activity.findViewById(android.R.id.content);
                    if (content != null) {
                        content.addView(inflate(activity, getRootLayout()));
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

    protected static View inflate(Context context, int layoutId) throws InflateException {
        return ((LayoutInflater) context.getSystemService(Context.LAYOUT_INFLATER_SERVICE)).inflate(layoutId, null);
    }

    private @LayoutRes int getRootLayout() {
        return R.layout.fragment_settings;
    }
    private @IdRes int getRootView() {
        return R.id.settings_container;
    }
    protected abstract List<Preference> getPreferences();
    protected abstract String getTAG();
    protected abstract Fragment getSelf();
}

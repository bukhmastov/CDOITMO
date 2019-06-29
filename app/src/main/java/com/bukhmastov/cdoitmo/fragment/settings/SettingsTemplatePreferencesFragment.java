package com.bukhmastov.cdoitmo.fragment.settings;

import android.content.Context;
import android.view.View;
import android.view.ViewGroup;
import android.widget.TextView;

import androidx.fragment.app.Fragment;

import com.bukhmastov.cdoitmo.R;
import com.bukhmastov.cdoitmo.factory.AppComponentProvider;
import com.bukhmastov.cdoitmo.firebase.FirebaseAnalyticsProvider;
import com.bukhmastov.cdoitmo.fragment.ConnectedFragment;
import com.bukhmastov.cdoitmo.fragment.presenter.ConnectedFragmentPresenter;
import com.bukhmastov.cdoitmo.object.preference.Preference;
import com.bukhmastov.cdoitmo.object.preference.PreferenceSwitch;
import com.bukhmastov.cdoitmo.provider.InjectProvider;
import com.bukhmastov.cdoitmo.util.Log;
import com.bukhmastov.cdoitmo.util.StoragePref;

import java.util.List;

import javax.inject.Inject;

public abstract class SettingsTemplatePreferencesFragment extends ConnectedFragment {

    private boolean loaded = false;

    @Inject
    Log log;
    @Inject
    StoragePref storagePref;
    @Inject
    InjectProvider injectProvider;
    @Inject
    FirebaseAnalyticsProvider firebaseAnalyticsProvider;

    @Override
    protected ConnectedFragmentPresenter getPresenter() {
        return null;
    }

    @Override
    public void onAttach(Context context) {
        AppComponentProvider.getComponent().inject(this);
        super.onAttach(context);
    }

    @Override
    public void onStart() {
        super.onStart();
        firebaseAnalyticsProvider.logCurrentScreen(activity(), getSelf());
    }

    @Override
    public void onDestroy() {
        super.onDestroy();
        loaded = false;
    }

    @Override
    public void onResume() {
        super.onResume();
        firebaseAnalyticsProvider.setCurrentScreen(activity(), getSelf());
        if (!loaded) {
            load();
        }
    }

    @Override
    protected int getLayoutId() {
        return R.layout.fragment_settings;
    }

    @Override
    protected int getRootId() {
        return R.id.settings_container;
    }

    @Override
    protected String getLogTag() {
        return getTAG();
    }

    private void load() {
        try {
            if (activity() == null) {
                return;
            }
            ViewGroup settingsContainer = activity().findViewById(getRootId());
            if (settingsContainer != null) {
                settingsContainer.removeAllViews();
                for (Preference preference : getPreferences()) {
                    settingsContainer.addView(Preference.getView(activity(), preference, injectProvider));
                }
                for (Preference preference : getPreferences()) {
                    if (preference instanceof PreferenceSwitch) {
                        final PreferenceSwitch preferenceSwitch = (PreferenceSwitch) preference;
                        final List<String> dependencies = preferenceSwitch.getDependencies();
                        if (dependencies.size() > 0) {
                            PreferenceSwitch.toggleDependencies(activity(), preferenceSwitch, storagePref.get(activity(), preference.key, (Boolean) preference.defaultValue));
                            for (Preference pref : getPreferences()) {
                                if (dependencies.contains(pref.key)) {
                                    pref.addPreferenceDependency(preferenceSwitch);
                                }
                            }
                        }
                    }
                }
                loaded = true;
            }
        } catch (Exception e) {
            log.exception(e);
            failed();
        }
    }

    private void failed() {
        try {
            View view = inflate(R.layout.state_failed_button);
            ((TextView) view.findViewById(R.id.try_again_message)).setText(R.string.error_occurred);
            view.findViewById(R.id.try_again_reload).setOnClickListener(v -> {
                try {
                    if (activity() == null) {
                        return;
                    }
                    ViewGroup content = activity().findViewById(android.R.id.content);
                    if (content != null) {
                        content.addView(inflate(getLayoutId()));
                        loaded = false;
                        load();
                    }
                } catch (Exception e) {
                    log.exception(e);
                }
            });
        } catch (Exception e) {
            log.exception(e);
        }
    }

    protected abstract List<Preference> getPreferences();
    protected abstract String getTAG();
    protected abstract Fragment getSelf();
}

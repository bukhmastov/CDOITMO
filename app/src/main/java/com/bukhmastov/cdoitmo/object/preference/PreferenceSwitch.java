package com.bukhmastov.cdoitmo.object.preference;

import androidx.annotation.Nullable;
import androidx.annotation.StringRes;
import android.view.View;
import android.view.ViewGroup;
import android.widget.Switch;
import android.widget.TextView;

import com.bukhmastov.cdoitmo.R;
import com.bukhmastov.cdoitmo.activity.ConnectedActivity;
import com.bukhmastov.cdoitmo.factory.AppComponentProvider;
import com.bukhmastov.cdoitmo.provider.InjectProvider;
import com.bukhmastov.cdoitmo.util.Log;

import java.util.LinkedList;
import java.util.List;

import javax.inject.Inject;

public class PreferenceSwitch extends Preference {

    public interface ApproveChangeCallback {
        void onDecisionMade(ConnectedActivity activity, PreferenceSwitch preference, boolean decision);
    }
    public interface Callback {
        void onApproveChange(ConnectedActivity activity, PreferenceSwitch preference, boolean value, ApproveChangeCallback callback);
    }

    private Callback callback;
    private List<String> dependencies = new LinkedList<>();
    public boolean enabled = false;

    @Inject
    Log log;

    public PreferenceSwitch(String key, Object defaultValue, @StringRes int title, @StringRes int summary, List<String> dependencies, Callback callback) {
        super(key, defaultValue, title, summary);
        AppComponentProvider.getComponent().inject(this);
        this.callback = callback;
        if (dependencies != null) {
            this.dependencies = dependencies;
        }
        if (defaultValue == null) {
            log.w(TAG, "PreferenceSwitch | defaultValue should not be null!");
        }
    }
    public PreferenceSwitch(String key, Object defaultValue, @StringRes int title, List<String> dependencies, Callback callback) {
        super(key, defaultValue, title);
        AppComponentProvider.getComponent().inject(this);
        this.callback = callback;
        if (dependencies != null) {
            this.dependencies = dependencies;
        }
        if (defaultValue == null) {
            log.w(TAG, "PreferenceSwitch | defaultValue should not be null!");
        }
    }

    public List<String> getDependencies() {
        return dependencies;
    }

    public static void toggleDependencies(ConnectedActivity activity, PreferenceSwitch preference, boolean enabled) {
        if (preference.dependencies != null && preference.dependencies.size() > 0) {
            View content = activity.findViewById(android.R.id.content);
            if (content != null) {
                for (String key : preference.dependencies) {
                    View view = content.findViewWithTag(key);
                    if (view != null) {
                        view.setAlpha(enabled ? 1F : 0.3F);
                    }
                }
            }
        }
    }

    @Nullable
    public static View getView(ConnectedActivity activity, PreferenceSwitch preference, InjectProvider injectProvider) {
        View preferenceLayout = inflate(activity, R.layout.preference_switcher);
        if (preferenceLayout == null) {
            return null;
        }
        ViewGroup preferenceSwitcher = preferenceLayout.findViewById(R.id.preference_switcher);
        Switch preferenceSwitcherSwitch = preferenceLayout.findViewById(R.id.preference_switcher_switch);
        TextView preferenceSwitcherTitle = preferenceLayout.findViewById(R.id.preference_switcher_title);
         TextView preferenceSwitcherSummary = preferenceLayout.findViewById(R.id.preference_switcher_summary);
        preferenceSwitcherTitle.setText(preference.title);
        if (preference.summary != 0) {
            preferenceSwitcherSummary.setVisibility(View.VISIBLE);
            preferenceSwitcherSummary.setText(preference.summary);
        } else {
            preferenceSwitcherSummary.setVisibility(View.GONE);
        }
        if (preference.defaultValue != null) {
            preference.enabled = injectProvider.getStoragePref().get(activity, preference.key, (Boolean) preference.defaultValue);
            preferenceSwitcherSwitch.setChecked(preference.enabled);
        }
        preferenceSwitcherSwitch.setOnCheckedChangeListener((buttonView, isChecked) -> {
            if (preference.isDisabled()) {
                buttonView.setChecked(!isChecked);
                return;
            }
            if (preference.callback == null) {
                preference.enabled = isChecked;
                injectProvider.getStoragePref().put(activity, preference.key, isChecked);
                preference.onPreferenceChanged(activity);
                toggleDependencies(activity, preference, isChecked);
            } else {
                preference.callback.onApproveChange(activity, preference, isChecked, (activity1, preference1, decision) -> {
                    if (decision) {
                        preference1.enabled = isChecked;
                        injectProvider.getStoragePref().put(activity1, preference1.key, isChecked);
                        preference1.onPreferenceChanged(activity1);
                        toggleDependencies(activity1, preference1, isChecked);
                    } else {
                        preferenceSwitcherSwitch.setChecked(!isChecked);
                    }
                });
            }
        });
        preferenceSwitcher.setOnClickListener(v -> {
            if (preference.isDisabled()) {
                return;
            }
            preferenceSwitcherSwitch.setChecked(!preferenceSwitcherSwitch.isChecked());
        });
        preferenceSwitcher.setTag(preference.key);
        return preferenceLayout;
    }
}

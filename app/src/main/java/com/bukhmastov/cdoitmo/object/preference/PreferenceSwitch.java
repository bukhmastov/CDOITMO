package com.bukhmastov.cdoitmo.object.preference;

import android.support.annotation.Nullable;
import android.support.annotation.StringRes;
import android.view.View;
import android.view.ViewGroup;
import android.widget.Switch;
import android.widget.TextView;

import com.bukhmastov.cdoitmo.R;
import com.bukhmastov.cdoitmo.activity.ConnectedActivity;
import com.bukhmastov.cdoitmo.factory.AppComponentProvider;
import com.bukhmastov.cdoitmo.provider.InjectProvider;
import com.bukhmastov.cdoitmo.util.Log;

import java.util.ArrayList;

import javax.inject.Inject;

public class PreferenceSwitch extends Preference {

    public interface ApproveChangeCallback {
        void onDecisionMade(ConnectedActivity activity, PreferenceSwitch preference, boolean decision);
    }
    public interface Callback {
        void onApproveChange(ConnectedActivity activity, PreferenceSwitch preference, boolean value, ApproveChangeCallback callback);
    }

    private Callback callback = null;
    private ArrayList<String> dependencies = new ArrayList<>();
    public boolean enabled = false;

    @Inject
    Log log;

    public PreferenceSwitch(String key, Object defaultValue, @StringRes int title, @StringRes int summary, ArrayList<String> dependencies, Callback callback) {
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
    public PreferenceSwitch(String key, Object defaultValue, @StringRes int title, ArrayList<String> dependencies, Callback callback) {
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

    public ArrayList<String> getDependencies() {
        return dependencies;
    }

    public static void toggleDependencies(final ConnectedActivity activity, final PreferenceSwitch preference, final boolean enabled) {
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
    public static View getView(final ConnectedActivity activity, final PreferenceSwitch preference, final InjectProvider injectProvider) {
        final View preference_layout = inflate(activity, R.layout.preference_switcher);
        if (preference_layout == null) {
            return null;
        }
        final ViewGroup preference_switcher = preference_layout.findViewById(R.id.preference_switcher);
        final Switch preference_switcher_switch = preference_layout.findViewById(R.id.preference_switcher_switch);
        final TextView preference_switcher_title = preference_layout.findViewById(R.id.preference_switcher_title);
        final TextView preference_switcher_summary = preference_layout.findViewById(R.id.preference_switcher_summary);
        preference_switcher_title.setText(preference.title);
        if (preference.summary != 0) {
            preference_switcher_summary.setVisibility(View.VISIBLE);
            preference_switcher_summary.setText(preference.summary);
        } else {
            preference_switcher_summary.setVisibility(View.GONE);
        }
        if (preference.defaultValue != null) {
            preference.enabled = injectProvider.getStoragePref().get(activity, preference.key, (Boolean) preference.defaultValue);
            preference_switcher_switch.setChecked(preference.enabled);
        }
        preference_switcher_switch.setOnCheckedChangeListener((buttonView, isChecked) -> {
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
                        preference_switcher_switch.setChecked(!isChecked);
                    }
                });
            }
        });
        preference_switcher.setOnClickListener(v -> {
            if (preference.isDisabled()) return;
            preference_switcher_switch.setChecked(!preference_switcher_switch.isChecked());
        });
        preference_switcher.setTag(preference.key);
        return preference_layout;
    }
}

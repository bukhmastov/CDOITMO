package com.bukhmastov.cdoitmo.object.preference;

import androidx.annotation.Nullable;
import androidx.annotation.StringRes;
import android.view.View;
import android.widget.TextView;

import com.bukhmastov.cdoitmo.R;
import com.bukhmastov.cdoitmo.activity.ConnectedActivity;
import com.bukhmastov.cdoitmo.provider.InjectProvider;

public class PreferenceBasic extends Preference {

    private boolean changeSummary = true;
    private final Callback callback;

    public interface OnPreferenceClickedCallback {
        void onSetSummary(final ConnectedActivity activity, final String value);
    }

    public interface Callback {
        void onPreferenceClicked(final ConnectedActivity activity, final Preference preference, final InjectProvider injectProvider, final OnPreferenceClickedCallback callback);
        String onGetSummary(final ConnectedActivity activity, final String value);
    }

    public PreferenceBasic(String key, Object defaultValue, @StringRes int title, @StringRes int summary, boolean changeSummary, Callback callback) {
        super(key, defaultValue, title, summary);
        this.changeSummary = changeSummary;
        this.callback = callback;
    }
    public PreferenceBasic(String key, Object defaultValue, @StringRes int title, boolean changeSummary, Callback callback) {
        super(key, defaultValue, title);
        this.changeSummary = changeSummary;
        this.callback = callback;
    }

    @Nullable
    public static View getView(final ConnectedActivity activity, final PreferenceBasic preference, final InjectProvider injectProvider) {
        final View preference_layout = inflate(activity, R.layout.preference_basic);
        if (preference_layout == null) {
            return null;
        }
        final View preference_basic = preference_layout.findViewById(R.id.preference_basic);
        final TextView preference_basic_title = preference_layout.findViewById(R.id.preference_basic_title);
        final TextView preference_basic_summary = preference_layout.findViewById(R.id.preference_basic_summary);
        preference_basic_title.setText(preference.title);
        if (preference.summary != 0) {
            preference_basic_summary.setVisibility(View.VISIBLE);
            preference_basic_summary.setText(preference.summary);
        } else {
            if (preference.changeSummary) {
                preference_basic_summary.setVisibility(View.VISIBLE);
                preference_basic_summary.setText(preference.callback.onGetSummary(activity, injectProvider.getStoragePref().get(activity, preference.key, preference.defaultValue == null ? "" : (String) preference.defaultValue)));
            } else {
                preference_basic_summary.setVisibility(View.GONE);
            }
        }
        preference_basic.setOnClickListener(view -> {
            if (preference.isDisabled()) return;
            preference.callback.onPreferenceClicked(activity, preference, injectProvider, (a, v) -> injectProvider.getThread().runOnUI(() -> {
                if (preference.changeSummary && v != null) {
                    final String summary = preference.callback.onGetSummary(a, v);
                    if (summary != null) {
                        preference_basic_summary.setVisibility(View.VISIBLE);
                        preference_basic_summary.setText(summary);
                    } else {
                        preference_basic_summary.setVisibility(View.GONE);
                    }
                } else {
                    preference_basic_summary.setVisibility(View.GONE);
                }
            }));
        });
        preference_basic.setTag(preference.key);
        return preference_layout;
    }
}

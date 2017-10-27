package com.bukhmastov.cdoitmo.objects.preferences;

import android.support.annotation.StringRes;
import android.view.View;
import android.widget.TextView;

import com.bukhmastov.cdoitmo.R;
import com.bukhmastov.cdoitmo.activities.ConnectedActivity;
import com.bukhmastov.cdoitmo.utils.Static;
import com.bukhmastov.cdoitmo.utils.Storage;

public class PreferenceBasic extends Preference {
    public interface OnPreferenceClickedCallback {
        void onSetSummary(final ConnectedActivity activity, final String value);
    }
    public interface Callback {
        void onPreferenceClicked(final ConnectedActivity activity, final Preference preference, final OnPreferenceClickedCallback callback);
        String onGetSummary(final ConnectedActivity activity, final String value);
    }
    private boolean changeSummary = true;
    private Callback callback;
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
    public static View getView(final ConnectedActivity activity, final PreferenceBasic preference) {
        final View preference_layout = inflate(activity, R.layout.layout_preference_basic);
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
                preference_basic_summary.setText(preference.callback.onGetSummary(activity, Storage.pref.get(activity, preference.key, preference.defaultValue == null ? "" : (String) preference.defaultValue)));
            } else {
                preference_basic_summary.setVisibility(View.GONE);
            }
        }
        preference_basic.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                if (!preference.isEnabled()) return;
                preference.callback.onPreferenceClicked(activity, preference, new OnPreferenceClickedCallback() {
                    @Override
                    public void onSetSummary(final ConnectedActivity activity, final String value) {
                        Static.T.runOnUiThread(new Runnable() {
                            @Override
                            public void run() {
                                if (preference.changeSummary && value != null) {
                                    final String summary = preference.callback.onGetSummary(activity, value);
                                    if (summary != null) {
                                        preference_basic_summary.setVisibility(View.VISIBLE);
                                        preference_basic_summary.setText(summary);
                                    } else {
                                        preference_basic_summary.setVisibility(View.GONE);
                                    }
                                } else {
                                    preference_basic_summary.setVisibility(View.GONE);
                                }
                            }
                        });
                    }
                });
            }
        });
        preference_basic.setTag(preference.key);
        return preference_layout;
    }
}

package com.bukhmastov.cdoitmo.object.preference;

import androidx.annotation.Nullable;
import androidx.annotation.StringRes;

import android.content.Context;
import android.view.View;
import android.widget.TextView;

import com.bukhmastov.cdoitmo.R;
import com.bukhmastov.cdoitmo.activity.ConnectedActivity;
import com.bukhmastov.cdoitmo.provider.InjectProvider;

public class PreferenceBasic extends Preference {

    private final boolean changeSummary;
    private final Callback callback;

    public interface OnPreferenceClickedCallback {
        void onSetSummary(ConnectedActivity activity, String value);
    }

    public interface Callback {
        void onPreferenceClicked(ConnectedActivity activity, Preference preference, InjectProvider injectProvider, OnPreferenceClickedCallback callback);
        String onGetSummary(Context context, String value);
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
    public static View getView(ConnectedActivity activity, PreferenceBasic preference, InjectProvider injectProvider) {
        View preferenceLayout = inflate(activity, R.layout.preference_basic);
        if (preferenceLayout == null) {
            return null;
        }
        View preferenceBasic = preferenceLayout.findViewById(R.id.preference_basic);
        TextView preferenceBasicTitle = preferenceLayout.findViewById(R.id.preference_basic_title);
        TextView preferenceBasicSummary = preferenceLayout.findViewById(R.id.preference_basic_summary);
        preferenceBasicTitle.setText(preference.title);
        if (preference.summary != 0) {
            preferenceBasicSummary.setVisibility(View.VISIBLE);
            preferenceBasicSummary.setText(preference.summary);
        } else {
            if (preference.changeSummary) {
                String summary = preference.callback.onGetSummary(
                        activity,
                        injectProvider.getStoragePref().get(activity, preference.key, preference.defaultValue == null ? "" : (String) preference.defaultValue)
                );
                preferenceBasicSummary.setText(summary);
                preferenceBasicSummary.setVisibility(View.VISIBLE);
            } else {
                preferenceBasicSummary.setVisibility(View.GONE);
            }
        }
        preferenceBasic.setOnClickListener(view -> {
            if (preference.isDisabled()) {
                return;
            }
            preference.callback.onPreferenceClicked(activity, preference, injectProvider, (a, v) -> {
                if (preference.changeSummary && v != null) {
                    String summary = preference.callback.onGetSummary(a, v);
                    if (summary != null) {
                        injectProvider.getThread().runOnUI(() -> {
                            preferenceBasicSummary.setVisibility(View.VISIBLE);
                            preferenceBasicSummary.setText(summary);
                        });
                        return;
                    }
                }
                injectProvider.getThread().runOnUI(() -> {
                    preferenceBasicSummary.setVisibility(View.GONE);
                });
            });
        });
        preferenceBasic.setTag(preference.key);
        return preferenceLayout;
    }
}

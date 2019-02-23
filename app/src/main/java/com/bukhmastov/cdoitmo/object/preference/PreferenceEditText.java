package com.bukhmastov.cdoitmo.object.preference;

import android.app.AlertDialog;
import android.content.Context;
import android.view.View;
import android.widget.EditText;
import android.widget.TextView;

import com.bukhmastov.cdoitmo.R;
import com.bukhmastov.cdoitmo.activity.ConnectedActivity;
import com.bukhmastov.cdoitmo.provider.InjectProvider;

import androidx.annotation.Nullable;
import androidx.annotation.StringRes;

public class PreferenceEditText extends Preference {

    private @StringRes int message;
    private @StringRes int hint;
    private boolean changeSummary;
    private Callback callback;

    public interface Callback {
        String onSetText(Context context, InjectProvider injectProvider, String value);
        String onGetText(Context context, InjectProvider injectProvider, String value);
    }

    public PreferenceEditText(String key, Object defaultValue, @StringRes int title, @StringRes int summary,
                              @StringRes int message, @StringRes int hint, boolean changeSummary, @Nullable Callback callback) {
        super(key, defaultValue, title, summary);
        this.message = message;
        this.hint = hint;
        this.changeSummary = changeSummary;
        this.callback = callback;
    }
    public PreferenceEditText(String key, Object defaultValue, @StringRes int title, @StringRes int message,
                              @StringRes int hint, boolean changeSummary, @Nullable Callback callback) {
        super(key, defaultValue, title);
        this.message = message;
        this.hint = hint;
        this.changeSummary = changeSummary;
        this.callback = callback;
    }

    @Nullable
    public static View getView(ConnectedActivity activity, PreferenceEditText preference, InjectProvider injectProvider) {
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
            String value = injectProvider.getStoragePref().get(activity, preference.key, (String) preference.defaultValue);
            if (preference.callback != null) {
                value = preference.callback.onSetText(activity, injectProvider, value);
            }
            if (preference.changeSummary && !value.isEmpty()) {
                preferenceBasicSummary.setVisibility(View.VISIBLE);
                preferenceBasicSummary.setText(value);
            } else {
                preferenceBasicSummary.setVisibility(View.GONE);
            }
        }
        preferenceBasic.setOnClickListener(v -> {
            if (activity.isFinishing() || activity.isDestroyed() || preference.isDisabled()) {
                return;
            }
            View view = inflate(activity, R.layout.preference_dialog_input);
            TextView message = view.findViewById(R.id.message);
            EditText edittext = view.findViewById(R.id.edittext);
            String value = injectProvider.getStoragePref().get(activity, preference.key, preference.defaultValue == null ? "" : (String) preference.defaultValue);
            if (preference.callback != null) {
                value = preference.callback.onSetText(activity, injectProvider, value);
            }
            edittext.setText(value);
            if (preference.hint != 0) {
                edittext.setHint(preference.hint);
            }
            if (preference.message != 0) {
                message.setText(preference.message);
                message.setVisibility(View.VISIBLE);
            }
            new AlertDialog.Builder(activity)
                    .setTitle(preference.title)
                    .setView(view)
                    .setPositiveButton(R.string.accept, (dialog, which) -> {
                        String val = edittext.getText().toString().trim();
                        if (preference.callback != null) {
                            val = preference.callback.onGetText(activity, injectProvider, val);
                        }
                        injectProvider.getStoragePref().put(activity, preference.key, val);
                        preference.onPreferenceChanged(activity);
                        if (preference.changeSummary) {
                            preferenceBasicSummary.setVisibility(View.VISIBLE);
                            preferenceBasicSummary.setText(val);
                        }
                    })
                    .setNegativeButton(R.string.cancel, null)
                    .create().show();
        });
        preferenceBasic.setTag(preference.key);
        return preferenceLayout;
    }
}

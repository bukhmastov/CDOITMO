package com.bukhmastov.cdoitmo.objects.preferences;

import android.content.Context;
import android.support.annotation.Nullable;
import android.support.annotation.StringRes;
import android.support.v7.app.AlertDialog;
import android.view.View;
import android.widget.EditText;
import android.widget.TextView;

import com.bukhmastov.cdoitmo.R;
import com.bukhmastov.cdoitmo.activities.ConnectedActivity;
import com.bukhmastov.cdoitmo.utils.Storage;

public class PreferenceEditText extends Preference {

    public interface Callback {
        String onSetText(final Context context, final String value);
        String onGetText(final Context context, final String value);
    }
    public @StringRes int message = 0;
    public @StringRes int hint = 0;
    public boolean changeSummary = true;
    public Callback callback = null;

    public PreferenceEditText(String key, Object defaultValue, @StringRes int title, @StringRes int summary, @StringRes int message, @StringRes int hint, boolean changeSummary, @Nullable Callback callback) {
        super(key, defaultValue, title, summary);
        this.message = message;
        this.hint = hint;
        this.changeSummary = changeSummary;
        this.callback = callback;
    }
    public PreferenceEditText(String key, Object defaultValue, @StringRes int title, @StringRes int message, @StringRes int hint, boolean changeSummary, @Nullable Callback callback) {
        super(key, defaultValue, title);
        this.message = message;
        this.hint = hint;
        this.changeSummary = changeSummary;
        this.callback = callback;
    }
    public static View getView(final ConnectedActivity activity, final PreferenceEditText preference) {
        final View preference_layout = inflate(activity, R.layout.layout_preference_basic);
        final View preference_basic = preference_layout.findViewById(R.id.preference_basic);
        final TextView preference_basic_title = preference_layout.findViewById(R.id.preference_basic_title);
        final TextView preference_basic_summary = preference_layout.findViewById(R.id.preference_basic_summary);
        preference_basic_title.setText(preference.title);
        if (preference.summary != 0) {
            preference_basic_summary.setVisibility(View.VISIBLE);
            preference_basic_summary.setText(preference.summary);
        } else {
            preference_basic_summary.setVisibility(View.GONE);
        }
        preference_basic.setOnClickListener(v -> {
            if (preference.isDisabled()) return;
            final View view = inflate(activity, R.layout.layout_preference_alert_edittext);
            final EditText editText = view.findViewById(R.id.edittext);
            final AlertDialog.Builder builder = new AlertDialog.Builder(activity);
            builder.setTitle(preference.title);
            if (preference.message != 0) {
                builder.setMessage(preference.message);
            }
            if (preference.hint != 0) {
                editText.setHint(preference.hint);
            }
            String value = Storage.pref.get(activity, preference.key, preference.defaultValue == null ? "" : (String) preference.defaultValue);
            if (preference.callback != null) {
                value = preference.callback.onSetText(activity, value);
            }
            editText.setText(value);
            builder.setView(view);
            builder.setPositiveButton(R.string.accept, (dialog, which) -> {
                String val = editText.getText().toString().trim();
                if (preference.callback != null) {
                    val = preference.callback.onGetText(activity, val);
                }
                Storage.pref.put(activity, preference.key, val);
                Preference.onPreferenceChanged(activity, preference.key);
                if (preference.changeSummary) {
                    preference_basic_summary.setText(val);
                }
            });
            builder.setNegativeButton(R.string.cancel, null);
            builder.create().show();
        });
        preference_basic.setTag(preference.key);
        return preference_layout;
    }
}

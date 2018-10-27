package com.bukhmastov.cdoitmo.object.preference;

import android.content.Context;
import androidx.annotation.Nullable;
import androidx.annotation.StringRes;
import androidx.appcompat.app.AlertDialog;
import android.view.View;
import android.widget.EditText;
import android.widget.TextView;

import com.bukhmastov.cdoitmo.R;
import com.bukhmastov.cdoitmo.activity.ConnectedActivity;
import com.bukhmastov.cdoitmo.provider.InjectProvider;

public class PreferenceEditText extends Preference {

    public @StringRes int message;
    public @StringRes int hint;
    public boolean changeSummary;
    public Callback callback;

    public interface Callback {
        String onSetText(final Context context, final InjectProvider injectProvider, final String value);
        String onGetText(final Context context, final InjectProvider injectProvider, final String value);
    }

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

    @Nullable
    public static View getView(final ConnectedActivity activity, final PreferenceEditText preference, final InjectProvider injectProvider) {
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
            String value = injectProvider.getStoragePref().get(activity, preference.key, (String) preference.defaultValue);
            if (preference.callback != null) {
                value = preference.callback.onSetText(activity, injectProvider, value);
            }
            if (preference.changeSummary && !value.isEmpty()) {
                preference_basic_summary.setVisibility(View.VISIBLE);
                preference_basic_summary.setText(value);
            } else {
                preference_basic_summary.setVisibility(View.GONE);
            }
        }
        preference_basic.setOnClickListener(v -> {
            if (activity.isFinishing() || activity.isDestroyed() || preference.isDisabled()) {
                return;
            }
            final View view = inflate(activity, R.layout.preference_dialog_input);
            final TextView message = view.findViewById(R.id.message);
            final EditText edittext = view.findViewById(R.id.edittext);
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
                            preference_basic_summary.setVisibility(View.VISIBLE);
                            preference_basic_summary.setText(val);
                        }
                    })
                    .setNegativeButton(R.string.cancel, null)
                    .create().show();
        });
        preference_basic.setTag(preference.key);
        return preference_layout;
    }
}

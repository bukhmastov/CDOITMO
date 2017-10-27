package com.bukhmastov.cdoitmo.objects.preferences;

import android.content.DialogInterface;
import android.support.annotation.ArrayRes;
import android.support.annotation.StringRes;
import android.support.v7.app.AlertDialog;
import android.view.View;
import android.widget.TextView;

import com.bukhmastov.cdoitmo.R;
import com.bukhmastov.cdoitmo.activities.ConnectedActivity;
import com.bukhmastov.cdoitmo.utils.Storage;

import java.util.Arrays;
import java.util.List;

public class PreferenceList extends Preference {
    public @ArrayRes int arrayTitles = 0;
    public @ArrayRes int arrayValues = 0;
    public boolean changeSummary = true;
    public PreferenceList(String key, Object defaultValue, @StringRes int title, @StringRes int summary, @ArrayRes int arrayTitles, @ArrayRes int arrayValues, boolean changeSummary) {
        super(key, defaultValue, title, summary);
        this.arrayTitles = arrayTitles;
        this.arrayValues = arrayValues;
        this.changeSummary = changeSummary;
    }
    public PreferenceList(String key, Object defaultValue, @StringRes int title, @ArrayRes int arrayTitles, @ArrayRes int arrayValues, boolean changeSummary) {
        super(key, defaultValue, title);
        this.arrayTitles = arrayTitles;
        this.arrayValues = arrayValues;
        this.changeSummary = changeSummary;
    }
    public static View getView(final ConnectedActivity activity, final PreferenceList preference) {
        final List<String> titles = Arrays.asList(activity.getResources().getStringArray(preference.arrayTitles));
        final List<String> values = Arrays.asList(activity.getResources().getStringArray(preference.arrayValues));
        final View preference_layout = inflate(activity, R.layout.layout_preference_list);
        final View preference_list = preference_layout.findViewById(R.id.preference_list);
        final TextView preference_list_title = preference_layout.findViewById(R.id.preference_list_title);
        final TextView preference_list_summary = preference_layout.findViewById(R.id.preference_list_summary);
        preference_list_title.setText(preference.title);
        preference_list_summary.setVisibility(View.VISIBLE);
        if (preference.summary != 0) {
            preference_list_summary.setText(preference.summary);
        } else {
            preference_list_summary.setText(titles.get(values.indexOf(Storage.pref.get(activity, preference.key, preference.defaultValue == null ? "" : (String) preference.defaultValue))));
        }
        preference_list.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                if (!preference.isEnabled()) return;
                int checked = 0;
                if (preference.defaultValue != null) {
                    checked = values.indexOf(Storage.pref.get(activity, preference.key, (String) preference.defaultValue));
                }
                new AlertDialog.Builder(activity)
                        .setTitle(preference.title)
                        .setSingleChoiceItems(preference.arrayTitles, checked, new DialogInterface.OnClickListener() {
                            @Override
                            public void onClick(DialogInterface dialog, int which) {
                                String value = values.get(which);
                                Storage.pref.put(activity, preference.key, value);
                                Preference.onPreferenceChanged(activity, preference.key);
                                if (preference.changeSummary) {
                                    preference_list_summary.setText(titles.get(which));
                                }
                                dialog.dismiss();
                            }
                        })
                        .setNegativeButton(R.string.cancel, null)
                        .create().show();
            }
        });
        preference_list.setTag(preference.key);
        return preference_layout;
    }
}

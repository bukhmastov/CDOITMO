package com.bukhmastov.cdoitmo.object.preference;

import android.support.annotation.ArrayRes;
import android.support.annotation.Nullable;
import android.support.annotation.StringRes;
import android.support.v7.app.AlertDialog;
import android.view.View;
import android.view.ViewGroup;
import android.widget.CompoundButton;
import android.widget.RadioButton;
import android.widget.RadioGroup;
import android.widget.TextView;

import com.bukhmastov.cdoitmo.R;
import com.bukhmastov.cdoitmo.activity.ConnectedActivity;
import com.bukhmastov.cdoitmo.util.StoragePref;
import com.bukhmastov.cdoitmo.util.StorageProvider;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;

public class PreferenceList extends Preference {

    private interface OnCheckedChangeListener {
        void onCheckedChanged(CompoundButton buttonView, boolean isChecked, int index);
    }

    private  @ArrayRes int arrayTitles = 0;
    private @ArrayRes int arrayValues = 0;
    private @ArrayRes int arrayDesc = 0;
    private boolean changeSummary = true;

    public PreferenceList(String key, Object defaultValue, @StringRes int title, @StringRes int summary, @ArrayRes int arrayTitles, @ArrayRes int arrayDesc, @ArrayRes int arrayValues, boolean changeSummary) {
        super(key, defaultValue, title, summary);
        this.arrayTitles = arrayTitles;
        this.arrayValues = arrayValues;
        this.arrayDesc = arrayDesc;
        this.changeSummary = changeSummary;
    }
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

    @Nullable
    public static View getView(final ConnectedActivity activity, final PreferenceList preference, final StorageProvider storageProvider) {
        final List<String> titles = preference.arrayTitles != 0 ? Arrays.asList(activity.getResources().getStringArray(preference.arrayTitles)) : new ArrayList<>();
        final List<String> values = preference.arrayValues != 0 ? Arrays.asList(activity.getResources().getStringArray(preference.arrayValues)) : new ArrayList<>();
        final List<String> descs = preference.arrayDesc != 0 ? Arrays.asList(activity.getResources().getStringArray(preference.arrayDesc)) : new ArrayList<>();
        final View preference_layout = inflate(activity, R.layout.preference_list);
        if (preference_layout == null) {
            return null;
        }
        final View preference_list = preference_layout.findViewById(R.id.preference_list);
        final TextView preference_list_title = preference_layout.findViewById(R.id.preference_list_title);
        final TextView preference_list_summary = preference_layout.findViewById(R.id.preference_list_summary);
        preference_list_title.setText(preference.title);
        preference_list_summary.setVisibility(View.VISIBLE);
        if (preference.summary != 0) {
            preference_list_summary.setText(preference.summary);
        } else {
            preference_list_summary.setText(titles.get(values.indexOf(storageProvider.getStoragePref().get(activity, preference.key, preference.defaultValue == null ? "" : (String) preference.defaultValue))));
        }
        preference_list.setOnClickListener(v -> {
            if (preference.isDisabled()) return;
            int checked = 0;
            if (preference.defaultValue != null) {
                checked = values.indexOf(storageProvider.getStoragePref().get(activity, preference.key, (String) preference.defaultValue));
            }
            final View view = inflate(activity, R.layout.preference_list_single_choice);
            if (view == null) {
                return;
            }
            final AlertDialog dialog = new AlertDialog.Builder(activity)
                    .setTitle(preference.title)
                    .setView(view)
                    .setNegativeButton(R.string.cancel, null)
                    .create();
            final RadioGroup radio_group = view.findViewById(R.id.radio_group);
            final OnCheckedChangeListener onCheckedChangeListener = (buttonView, isChecked, index) -> {
                if (isChecked) {
                    String value = values.get(index);
                    storageProvider.getStoragePref().put(activity, preference.key, value);
                    preference.onPreferenceChanged(activity);
                    if (preference.changeSummary) {
                        preference_list_summary.setText(titles.get(index));
                    }
                    dialog.dismiss();
                }
            };
            for (int i = 0; i < titles.size(); i++) {
                final int index = i;
                final View item = inflate(activity, R.layout.preference_list_single_choice_item);
                if (item == null) {
                    continue;
                }
                final ViewGroup content = item.findViewById(R.id.content);
                final RadioButton button = item.findViewById(R.id.button);
                final TextView desc = item.findViewById(R.id.desc);
                content.setOnClickListener(v1 -> button.toggle());
                button.setText(titles.get(index));
                button.setChecked(checked == index);
                button.setOnCheckedChangeListener((buttonView, isChecked) -> onCheckedChangeListener.onCheckedChanged(buttonView, isChecked, index));
                if (index < descs.size()) {
                    String text = descs.get(index);
                    if (!text.isEmpty()) {
                        desc.setText(descs.get(index));
                        desc.setVisibility(View.VISIBLE);
                    }
                }
                radio_group.addView(item, RadioGroup.LayoutParams.MATCH_PARENT, RadioGroup.LayoutParams.WRAP_CONTENT);
            }
            dialog.show();
        });
        preference_list.setTag(preference.key);
        return preference_layout;
    }
}

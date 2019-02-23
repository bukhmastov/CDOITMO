package com.bukhmastov.cdoitmo.object.preference;

import android.app.AlertDialog;
import android.view.View;
import android.view.ViewGroup;
import android.widget.CompoundButton;
import android.widget.RadioButton;
import android.widget.RadioGroup;
import android.widget.TextView;

import com.bukhmastov.cdoitmo.R;
import com.bukhmastov.cdoitmo.activity.ConnectedActivity;
import com.bukhmastov.cdoitmo.provider.InjectProvider;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;

import androidx.annotation.ArrayRes;
import androidx.annotation.Nullable;
import androidx.annotation.StringRes;

public class PreferenceList extends Preference {

    private interface OnCheckedChangeListener {
        void onCheckedChanged(CompoundButton buttonView, boolean isChecked, int index);
    }

    private @StringRes int description = 0;
    private @ArrayRes int arrayTitles;
    private @ArrayRes int arrayValues;
    private @ArrayRes int arrayDesc = 0;
    private boolean changeSummary;

    public PreferenceList(String key, Object defaultValue, @StringRes int title, @StringRes int summary,
                          @StringRes int description, @ArrayRes int arrayTitles, @ArrayRes int arrayDesc,
                          @ArrayRes int arrayValues, boolean changeSummary) {
        super(key, defaultValue, title, summary);
        this.description = description;
        this.arrayTitles = arrayTitles;
        this.arrayValues = arrayValues;
        this.arrayDesc = arrayDesc;
        this.changeSummary = changeSummary;
    }
    public PreferenceList(String key, Object defaultValue, @StringRes int title, @StringRes int summary,
                          @ArrayRes int arrayTitles, @ArrayRes int arrayDesc, @ArrayRes int arrayValues,
                          boolean changeSummary) {
        super(key, defaultValue, title, summary);
        this.arrayTitles = arrayTitles;
        this.arrayValues = arrayValues;
        this.arrayDesc = arrayDesc;
        this.changeSummary = changeSummary;
    }
    public PreferenceList(String key, Object defaultValue, @StringRes int title, @StringRes int summary,
                          @ArrayRes int arrayTitles, @ArrayRes int arrayValues, boolean changeSummary) {
        super(key, defaultValue, title, summary);
        this.arrayTitles = arrayTitles;
        this.arrayValues = arrayValues;
        this.changeSummary = changeSummary;
    }
    public PreferenceList(String key, Object defaultValue, @StringRes int title, @ArrayRes int arrayTitles,
                          @ArrayRes int arrayValues, boolean changeSummary) {
        super(key, defaultValue, title);
        this.arrayTitles = arrayTitles;
        this.arrayValues = arrayValues;
        this.changeSummary = changeSummary;
    }

    @Nullable
    public static View getView(ConnectedActivity activity, PreferenceList preference, InjectProvider injectProvider) {
        List<String> titles = preference.arrayTitles != 0 ? Arrays.asList(activity.getResources().getStringArray(preference.arrayTitles)) : new ArrayList<>();
        List<String> values = preference.arrayValues != 0 ? Arrays.asList(activity.getResources().getStringArray(preference.arrayValues)) : new ArrayList<>();
        List<String> descs = preference.arrayDesc != 0 ? Arrays.asList(activity.getResources().getStringArray(preference.arrayDesc)) : new ArrayList<>();
        View preferenceLayout = inflate(activity, R.layout.preference_list);
        if (preferenceLayout == null) {
            return null;
        }
        View preferenceList = preferenceLayout.findViewById(R.id.preference_list);
        TextView preferenceListTitle = preferenceLayout.findViewById(R.id.preference_list_title);
        TextView preferenceListSummary = preferenceLayout.findViewById(R.id.preference_list_summary);
        preferenceListTitle.setText(preference.title);
        preferenceListSummary.setVisibility(View.VISIBLE);
        if (preference.summary != 0) {
            preferenceListSummary.setText(preference.summary);
        } else {
            String selected = injectProvider.getStoragePref().get(activity, preference.key, preference.defaultValue == null ? "" : (String) preference.defaultValue);
            String summary = titles.get(values.indexOf(selected));
            preferenceListSummary.setText(summary);
        }
        preferenceList.setOnClickListener(v -> {
            if (activity.isFinishing() || activity.isDestroyed() || preference.isDisabled()) {
                return;
            }
            int checked = 0;
            if (preference.defaultValue != null) {
                checked = values.indexOf(injectProvider.getStoragePref().get(activity, preference.key, (String) preference.defaultValue));
            }
            View view = inflate(activity, R.layout.preference_list_single_choice);
            if (view == null) {
                return;
            }
            AlertDialog dialog = new AlertDialog.Builder(activity)
                    .setTitle(preference.title)
                    .setView(view)
                    .setNegativeButton(R.string.cancel, null)
                    .create();
            TextView message = view.findViewById(R.id.message);
            if (preference.description != 0) {
                message.setVisibility(View.VISIBLE);
                message.setText(preference.description);
            } else {
                message.setVisibility(View.GONE);
            }
            RadioGroup radioGroup = view.findViewById(R.id.radio_group);
            OnCheckedChangeListener onCheckedChangeListener = (buttonView, isChecked, index) -> {
                if (isChecked) {
                    String value = values.get(index);
                    injectProvider.getStoragePref().put(activity, preference.key, value);
                    preference.onPreferenceChanged(activity);
                    if (preference.changeSummary) {
                        preferenceListSummary.setText(titles.get(index));
                    }
                    dialog.dismiss();
                }
            };
            for (int i = 0; i < titles.size(); i++) {
                int index = i;
                View item = inflate(activity, R.layout.preference_list_single_choice_item);
                if (item == null) {
                    continue;
                }
                ViewGroup content = item.findViewById(R.id.content);
                RadioButton button = item.findViewById(R.id.button);
                TextView desc = item.findViewById(R.id.desc);
                content.setOnClickListener(v1 -> button.toggle());
                button.setText(titles.get(index));
                button.setChecked(checked == index);
                button.setOnCheckedChangeListener((buttonView, isChecked) -> {
                    onCheckedChangeListener.onCheckedChanged(buttonView, isChecked, index);
                });
                if (index < descs.size()) {
                    String text = descs.get(index);
                    if (!text.isEmpty()) {
                        desc.setText(descs.get(index));
                        desc.setVisibility(View.VISIBLE);
                    }
                }
                radioGroup.addView(item, RadioGroup.LayoutParams.MATCH_PARENT, RadioGroup.LayoutParams.WRAP_CONTENT);
            }
            dialog.show();
        });
        preferenceList.setTag(preference.key);
        return preferenceLayout;
    }
}

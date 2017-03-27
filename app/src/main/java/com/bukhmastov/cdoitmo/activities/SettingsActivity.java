package com.bukhmastov.cdoitmo.activities;

import android.annotation.TargetApi;
import android.content.DialogInterface;
import android.content.Intent;
import android.content.SharedPreferences;
import android.content.res.Configuration;
import android.media.Ringtone;
import android.media.RingtoneManager;
import android.net.Uri;
import android.os.Build;
import android.os.Bundle;
import android.preference.ListPreference;
import android.preference.Preference;
import android.preference.PreferenceFragment;
import android.preference.PreferenceManager;
import android.preference.RingtonePreference;
import android.support.v4.app.NavUtils;
import android.support.v7.app.ActionBar;
import android.support.v7.app.AlertDialog;
import android.support.v7.widget.Toolbar;
import android.text.TextUtils;
import android.util.TypedValue;
import android.view.LayoutInflater;
import android.view.MenuItem;
import android.view.View;
import android.view.ViewGroup;
import android.widget.LinearLayout;
import android.widget.ListView;

import com.bukhmastov.cdoitmo.R;
import com.bukhmastov.cdoitmo.preferences.SchedulePreference;
import com.bukhmastov.cdoitmo.utils.ProtocolTracker;
import com.bukhmastov.cdoitmo.utils.Static;
import com.bukhmastov.cdoitmo.utils.Storage;

import org.json.JSONException;
import org.json.JSONObject;

import java.util.List;

public class SettingsActivity extends AppCompatPreferenceActivity implements SharedPreferences.OnSharedPreferenceChangeListener {

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        if (Static.darkTheme) setTheme(R.style.AppTheme_Dark);
        super.onCreate(savedInstanceState);
    }

    @Override
    protected void onResume() {
        super.onResume();
        PreferenceManager.getDefaultSharedPreferences(this).registerOnSharedPreferenceChangeListener(this);
    }

    @Override
    protected void onPause() {
        super.onPause();
        PreferenceManager.getDefaultSharedPreferences(this).unregisterOnSharedPreferenceChangeListener(this);
    }

    @Override
    protected void onPostCreate(Bundle savedInstanceState) {
        super.onPostCreate(savedInstanceState);
        Toolbar bar;
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.JELLY_BEAN) {
            LinearLayout root = (LinearLayout) findViewById(android.R.id.list).getParent().getParent().getParent();
            bar = (Toolbar) LayoutInflater.from(this).inflate(R.layout.toolbar_settings, root, false);
            root.addView(bar, 0); // insert at top
        } else {
            ViewGroup root = (ViewGroup) findViewById(android.R.id.content);
            ListView content = (ListView) root.getChildAt(0);
            root.removeAllViews();
            bar = (Toolbar) LayoutInflater.from(this).inflate(R.layout.toolbar_settings, root, false);
            int height;
            TypedValue tv = new TypedValue();
            if (getTheme().resolveAttribute(R.attr.actionBarSize, tv, true)) {
                height = TypedValue.complexToDimensionPixelSize(tv.data, getResources().getDisplayMetrics());
            }else{
                height = bar.getHeight();
            }
            content.setPadding(0, height, 0, 0);
            root.addView(content);
            root.addView(bar);
        }
        setSupportActionBar((Toolbar) findViewById(R.id.toolbar_settings));
        ActionBar actionBar = getSupportActionBar();
        if(actionBar != null){
            actionBar.setHomeButtonEnabled(true);
            actionBar.setDisplayHomeAsUpEnabled(true);
        }
        bar.setNavigationOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                finish();
            }
        });
    }

    @Override
    public boolean onMenuItemSelected(int featureId, MenuItem item) {
        int id = item.getItemId();
        if (id == android.R.id.home) {
            if (!super.onMenuItemSelected(featureId, item)) {
                NavUtils.navigateUpFromSameTask(this);
            }
            return true;
        }
        return super.onMenuItemSelected(featureId, item);
    }

    @Override
    public void onSharedPreferenceChanged(final SharedPreferences sharedPreferences, String key) {
        switch(key){
            case "pref_use_notifications":
            case "pref_notify_frequency":
                new ProtocolTracker(this).restart();
                break;
            case "pref_dark_theme":
                Static.reLaunch(this);
                break;
            case "pref_protocol_changes_track":
                if (Storage.pref.get(this, "pref_protocol_changes_track", true)) {
                    Static.protocolChangesTrackSetup(this, 0);
                } else {
                    Storage.file.cache.clear(this, "rating#log");
                }
                break;
        }
    }

    @Override
    public boolean onIsMultiPane() {
        return (this.getResources().getConfiguration().screenLayout & Configuration.SCREENLAYOUT_SIZE_MASK) >= Configuration.SCREENLAYOUT_SIZE_XLARGE;
    }

    @Override
    @TargetApi(Build.VERSION_CODES.HONEYCOMB)
    public void onBuildHeaders(List<Header> target) {
        loadHeadersFromResource(R.xml.pref_headers, target);
    }

    public static abstract class TemplatePreferenceFragment extends PreferenceFragment {
        @Override
        public boolean onOptionsItemSelected(MenuItem item) {
            int id = item.getItemId();
            if (id == android.R.id.home) {
                startActivity(new Intent(getActivity(), SettingsActivity.class));
                return true;
            }
            return super.onOptionsItemSelected(item);
        }
    }

    public static class GeneralPreferenceFragment extends TemplatePreferenceFragment {
        @Override
        public void onCreate(Bundle savedInstanceState) {
            super.onCreate(savedInstanceState);
            addPreferencesFromResource(R.xml.pref_general);
            setHasOptionsMenu(true);
            bindPreferenceSummaryToValue(findPreference("pref_default_fragment"));
            Preference pref_reset_application = findPreference("pref_reset_application");
            if (pref_reset_application != null) {
                pref_reset_application.setOnPreferenceClickListener(new Preference.OnPreferenceClickListener() {
                    @Override
                    public boolean onPreferenceClick(Preference preference) {
                        new AlertDialog.Builder(getActivity())
                                .setTitle(getString(R.string.pref_reset_application_summary))
                                .setMessage(R.string.pref_reset_application_warning)
                                .setIcon(R.drawable.ic_warning)
                                .setPositiveButton(R.string.proceed, new DialogInterface.OnClickListener() {
                                    @Override
                                    public void onClick(DialogInterface dialog, int which) {
                                        Static.hardReset(getActivity());
                                    }
                                })
                                .setNegativeButton(android.R.string.cancel, null)
                                .create().show();
                        return false;
                    }
                });
            }
        }
    }

    public static class CachePreferenceFragment extends TemplatePreferenceFragment {
        @Override
        public void onCreate(Bundle savedInstanceState) {
            super.onCreate(savedInstanceState);
            addPreferencesFromResource(R.xml.pref_cache);
            setHasOptionsMenu(true);
            bindPreferenceSummaryToValue(findPreference("pref_tab_refresh"));
            bindPreferenceSummaryToValue(findPreference("pref_schedule_refresh"));
            Preference pref_clear_cache = findPreference("pref_clear_cache");
            if (pref_clear_cache != null) {
                pref_clear_cache.setOnPreferenceClickListener(new Preference.OnPreferenceClickListener() {
                    @Override
                    public boolean onPreferenceClick(Preference preference) {
                        boolean success = Storage.file.cache.clear(getActivity());
                        Static.snackBar(getActivity(), getActivity().getString(success ? R.string.cache_cleared : R.string.something_went_wrong));
                        return false;
                    }
                });
            }
        }
    }

    public static class NotificationsPreferenceFragment extends TemplatePreferenceFragment {
        @Override
        public void onCreate(Bundle savedInstanceState) {
            super.onCreate(savedInstanceState);
            addPreferencesFromResource(R.xml.pref_notifications);
            setHasOptionsMenu(true);
            bindPreferenceSummaryToValue(findPreference("pref_notify_frequency"));
            bindPreferenceSummaryToValue(findPreference("pref_notify_sound"));
        }
    }

    public static class AdditionalPreferenceFragment extends TemplatePreferenceFragment {
        @Override
        public void onCreate(Bundle savedInstanceState) {
            super.onCreate(savedInstanceState);
            addPreferencesFromResource(R.xml.pref_additional);
            setHasOptionsMenu(true);
            bindPreferenceSummaryToValue(findPreference("pref_e_journal_term"));
            bindPreferenceSummaryToValue(findPreference("pref_protocol_changes_weeks"));
            bindPreferenceSummaryToValue(findPreference("pref_schedule_lessons_default"));
            bindPreferenceSummaryToValue(findPreference("pref_schedule_lessons_week"));
            bindPreferenceSummaryToValue(findPreference("pref_schedule_exams_default"));
            Preference pref_schedule_lessons_clear_cache = findPreference("pref_schedule_lessons_clear_cache");
            if (pref_schedule_lessons_clear_cache != null) {
                pref_schedule_lessons_clear_cache.setOnPreferenceClickListener(new Preference.OnPreferenceClickListener() {
                    @Override
                    public boolean onPreferenceClick(Preference preference) {
                        boolean success = Storage.file.cache.clear(getActivity(), "schedule_lessons");
                        Static.snackBar(getActivity(), getActivity().getString(success ? R.string.cache_cleared : R.string.something_went_wrong));
                        return false;
                    }
                });
            }
            Preference pref_schedule_exams_clear_cache = findPreference("pref_schedule_exams_clear_cache");
            if (pref_schedule_exams_clear_cache != null) {
                pref_schedule_exams_clear_cache.setOnPreferenceClickListener(new Preference.OnPreferenceClickListener() {
                    @Override
                    public boolean onPreferenceClick(Preference preference) {
                        boolean success = Storage.file.cache.clear(getActivity(), "schedule_exams");
                        Static.snackBar(getActivity(), getActivity().getString(success ? R.string.cache_cleared : R.string.something_went_wrong));
                        return false;
                    }
                });
            }

            Preference pref_schedule_lessons_clear_additional = findPreference("pref_schedule_lessons_clear_additional");
            if (pref_schedule_lessons_clear_additional != null) {
                pref_schedule_lessons_clear_additional.setOnPreferenceClickListener(new Preference.OnPreferenceClickListener() {
                    @Override
                    public boolean onPreferenceClick(Preference preference) {
                        new AlertDialog.Builder(getActivity())
                                .setTitle(getString(R.string.pref_schedule_lessons_clear_additional_title))
                                .setMessage(R.string.pref_schedule_lessons_clear_additional_warning)
                                .setIcon(R.drawable.ic_warning)
                                .setPositiveButton(R.string.proceed, new DialogInterface.OnClickListener() {
                                    @Override
                                    public void onClick(DialogInterface dialog, int which) {
                                        boolean success = Storage.file.perm.clear(getActivity(), "schedule_lessons");
                                        Static.snackBar(getActivity(), getActivity().getString(success ? R.string.changes_cleared : R.string.something_went_wrong));
                                    }
                                })
                                .setNegativeButton(android.R.string.cancel, null)
                                .create().show();
                        return false;
                    }
                });
            }

        }
    }

    public static class AboutPreferenceFragment extends PreferenceFragment {
        @Override
        public void onCreate(Bundle savedInstanceState) {
            super.onCreate(savedInstanceState);
            getActivity().finish();
            startActivity(new Intent(getActivity(), AboutActivity.class));
        }
    }

    protected boolean isValidFragment(String fragmentName) {
        return PreferenceFragment.class.getName().equals(fragmentName)
                || GeneralPreferenceFragment.class.getName().equals(fragmentName)
                || CachePreferenceFragment.class.getName().equals(fragmentName)
                || NotificationsPreferenceFragment.class.getName().equals(fragmentName)
                || AdditionalPreferenceFragment.class.getName().equals(fragmentName)
                || AboutPreferenceFragment.class.getName().equals(fragmentName);
    }

    private static void bindPreferenceSummaryToValue(Preference preference) {
        preference.setOnPreferenceChangeListener(sBindPreferenceSummaryToValueListener);
        sBindPreferenceSummaryToValueListener.onPreferenceChange(preference, PreferenceManager.getDefaultSharedPreferences(preference.getContext()).getString(preference.getKey(), ""));
    }

    private static Preference.OnPreferenceChangeListener sBindPreferenceSummaryToValueListener = new Preference.OnPreferenceChangeListener() {
        @Override
        public boolean onPreferenceChange(Preference preference, Object value) {
            String stringValue = value.toString();
            if (preference instanceof ListPreference) {
                ListPreference listPreference = (ListPreference) preference;
                int index = listPreference.findIndexOfValue(stringValue);
                preference.setSummary(index >= 0 ? listPreference.getEntries()[index] : null);
            } else if (preference instanceof SchedulePreference) {
                String summary;
                try {
                    JSONObject jsonValue = new JSONObject(stringValue);
                    switch (jsonValue.getString("query")) {
                        case "": summary = null; break;
                        case "auto": summary = preference.getContext().getString(R.string.current_group); break;
                        default: summary = jsonValue.getString("title"); break;
                    }
                } catch (JSONException e){
                    Static.error(e);
                    summary = null;
                }
                preference.setSummary(summary);
            } else if (preference instanceof RingtonePreference) {
                if (TextUtils.isEmpty(stringValue)) {
                    preference.setSummary(R.string.pref_ringtone_silent);
                } else {
                    Ringtone ringtone = RingtoneManager.getRingtone(preference.getContext(), Uri.parse(stringValue));
                    if (ringtone == null) {
                        preference.setSummary(null);
                    } else {
                        String name = ringtone.getTitle(preference.getContext());
                        preference.setSummary(name);
                    }
                }
            } else {
                preference.setSummary(stringValue);
            }
            return true;
        }
    };
}
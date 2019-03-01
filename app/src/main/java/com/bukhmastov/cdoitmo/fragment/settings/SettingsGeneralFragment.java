package com.bukhmastov.cdoitmo.fragment.settings;

import android.app.AlertDialog;
import android.content.Context;
import android.content.Intent;

import com.bukhmastov.cdoitmo.R;
import com.bukhmastov.cdoitmo.activity.ConnectedActivity;
import com.bukhmastov.cdoitmo.view.dialog.ThemeDialog;
import com.bukhmastov.cdoitmo.event.events.OpenIntentEvent;
import com.bukhmastov.cdoitmo.object.preference.Preference;
import com.bukhmastov.cdoitmo.object.preference.PreferenceBasic;
import com.bukhmastov.cdoitmo.object.preference.PreferenceEditText;
import com.bukhmastov.cdoitmo.object.preference.PreferenceList;
import com.bukhmastov.cdoitmo.object.preference.PreferenceSwitch;
import com.bukhmastov.cdoitmo.provider.InjectProvider;
import com.bukhmastov.cdoitmo.util.NotificationMessage;

import java.util.Calendar;
import java.util.LinkedList;
import java.util.List;

import androidx.fragment.app.Fragment;

public class SettingsGeneralFragment extends SettingsTemplatePreferencesFragment {

    private static final String TAG = "SettingsGeneralFragment";
    public static final List<Preference> preferences;
    static {
        preferences = new LinkedList<>();
        preferences.add(new PreferenceList("pref_lang", "default", R.string.pref_lang_title, R.string.pref_lang_title_summary, R.array.pref_lang_titles, R.array.pref_lang_values, false));
        preferences.add(new PreferenceList("pref_default_fragment", "e_journal", R.string.pref_default_fragment, R.array.pref_general_default_fragment_titles, R.array.pref_general_default_fragment_values, true));
        preferences.add(new PreferenceBasic("pref_theme", "light", R.string.theme, true, new PreferenceBasic.Callback() {
            @Override
            public void onPreferenceClicked(ConnectedActivity activity, Preference preference, InjectProvider injectProvider, PreferenceBasic.OnPreferenceClickedCallback callback) {
                String theme = injectProvider.getStoragePref().get(activity, "pref_theme", "light");
                new ThemeDialog(activity, theme, (thm, desc) -> {
                    injectProvider.getStoragePref().put(activity, "pref_theme", thm);
                    callback.onSetSummary(activity, desc);
                    injectProvider.getNotificationMessage().snackBar(activity, activity.getString(R.string.restart_required), activity.getString(R.string.restart), NotificationMessage.LENGTH_LONG, view -> {
                        injectProvider.getTheme().updateAppTheme(activity);
                        injectProvider.getStaticUtil().reLaunch(activity);
                    });
                }).show();
            }
            @Override
            public String onGetSummary(Context context, String value) {
                return ThemeDialog.getThemeDesc(context, value);
            }
        }));
        preferences.add(new PreferenceSwitch("pref_auto_logout", false, R.string.pref_auto_logout, R.string.pref_auto_logout_summary, null, null));
        preferences.add(new PreferenceSwitch("pref_initial_offline", false, R.string.pref_initial_offline, R.string.pref_initial_offline_summary, null, null));
        preferences.add(new PreferenceEditText("pref_group_force_override", "", R.string.pref_group_force_override, R.string.pref_group_force_override_summary, R.string.pref_group_force_override_message, R.string.pref_group_force_override_hint, false, null));
        preferences.add(new PreferenceEditText("pref_week_force_override", "", R.string.pref_week_force_override, R.string.pref_week_force_override_summary, R.string.pref_week_force_override_message, R.string.pref_week_force_override_hint, false, new PreferenceEditText.Callback() {
            @Override
            public String onSetText(Context context, InjectProvider injectProvider, String value) {
                try {
                    if (value.isEmpty()) {
                        return value;
                    }
                    String[] v = value.split("#");
                    if (v.length == 2) {
                        final int week = Integer.parseInt(v[0]);
                        final long ts = Long.parseLong(v[1]);
                        final Calendar today = injectProvider.getTime().getCalendar();
                        final Calendar past = (Calendar) today.clone();
                        past.setTimeInMillis(ts);
                        value = String.valueOf(week + (today.get(Calendar.WEEK_OF_YEAR) - past.get(Calendar.WEEK_OF_YEAR)));
                    } else {
                        value = "";
                    }
                } catch (Exception e) {
                    value = "";
                }
                return value;
            }
            @Override
            public String onGetText(Context context, InjectProvider injectProvider, String value) {
                try {
                    if (value.isEmpty()) {
                        return value;
                    }
                    final int week = Integer.parseInt(value);
                    final long ts = injectProvider.getTime().getCalendar().getTimeInMillis();
                    if (week > 0) {
                        value = String.valueOf(week) + "#" + String.valueOf(ts);
                    }
                } catch (Exception e) {
                    value = "";
                }
                return value;
            }
        }));
        preferences.add(new PreferenceBasic("pref_open_system_settings", null, R.string.pref_open_system_settings, false, new PreferenceBasic.Callback() {
            @Override
            public void onPreferenceClicked(ConnectedActivity activity, Preference preference, InjectProvider injectProvider, PreferenceBasic.OnPreferenceClickedCallback callback) {
                try {
                    Intent intent = new Intent(android.provider.Settings.ACTION_APPLICATION_DETAILS_SETTINGS);
                    intent.setData(android.net.Uri.parse("package:" + activity.getPackageName()));
                    intent.addCategory(Intent.CATEGORY_DEFAULT);
                    intent.setFlags(Intent.FLAG_ACTIVITY_NEW_TASK);
                    injectProvider.getEventBus().fire(new OpenIntentEvent(intent));
                } catch (Exception e) {
                    try {
                        Intent intent = new Intent(android.provider.Settings.ACTION_MANAGE_APPLICATIONS_SETTINGS);
                        intent.setFlags(Intent.FLAG_ACTIVITY_NEW_TASK);
                        injectProvider.getEventBus().fire(new OpenIntentEvent(intent));
                    } catch (Exception ignore) {
                        injectProvider.getNotificationMessage().snackBar(activity, activity.getString(R.string.something_went_wrong));
                    }
                }
            }
            @Override
            public String onGetSummary(Context context, String value) {
                return null;
            }
        }));
        preferences.add(new PreferenceBasic("pref_reset_application", null, R.string.pref_reset_application_summary, false, new PreferenceBasic.Callback() {
            @Override
            public void onPreferenceClicked(ConnectedActivity activity, Preference preference, InjectProvider injectProvider, PreferenceBasic.OnPreferenceClickedCallback callback) {
                injectProvider.getThread().runOnUI(() -> {
                    if (activity.isFinishing() || activity.isDestroyed()) {
                        return;
                    }
                    new AlertDialog.Builder(activity)
                            .setTitle(R.string.pref_reset_application_summary)
                            .setMessage(R.string.pref_reset_application_warning)
                            .setIcon(R.drawable.ic_warning)
                            .setPositiveButton(R.string.proceed, (dialog, which) -> {
                                injectProvider.getThread().standalone(() -> {
                                    injectProvider.getStaticUtil().hardReset(activity);
                                });
                            })
                            .setNegativeButton(R.string.cancel, null)
                            .create().show();
                });
            }
            @Override
            public String onGetSummary(Context context, String value) {
                return null;
            }
        }));
    }

    @Override
    protected List<Preference> getPreferences() {
        return preferences;
    }

    @Override
    protected String getTAG() {
        return TAG;
    }

    @Override
    protected Fragment getSelf() {
        return this;
    }
}

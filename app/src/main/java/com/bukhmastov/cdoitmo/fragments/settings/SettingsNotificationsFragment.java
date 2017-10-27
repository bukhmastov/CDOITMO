package com.bukhmastov.cdoitmo.fragments.settings;

import android.app.Activity;
import android.content.Intent;
import android.media.Ringtone;
import android.media.RingtoneManager;
import android.net.Uri;
import android.support.v4.app.Fragment;
import android.support.v4.app.FragmentManager;
import android.text.TextUtils;

import com.bukhmastov.cdoitmo.R;
import com.bukhmastov.cdoitmo.activities.ConnectedActivity;
import com.bukhmastov.cdoitmo.objects.preferences.Preference;
import com.bukhmastov.cdoitmo.objects.preferences.PreferenceBasic;
import com.bukhmastov.cdoitmo.objects.preferences.PreferenceList;
import com.bukhmastov.cdoitmo.objects.preferences.PreferenceSwitch;
import com.bukhmastov.cdoitmo.utils.Static;
import com.bukhmastov.cdoitmo.utils.Storage;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;

public class SettingsNotificationsFragment extends SettingsTemplatePreferencesFragment {

    private static final String TAG = "SettingsNotificationsFragment";
    private static final int TONE_PICKER = 5523;
    private static class RingtoneHelper {
        private static String preference_key = null;
        private static PreferenceBasic.OnPreferenceClickedCallback callback = null;
    }
    public static ArrayList<Preference> preferences;
    static {
        preferences = new ArrayList<>();
        preferences.add(new PreferenceSwitch(
                "pref_use_notifications",
                true,
                R.string.pref_use_notifications,
                R.string.pref_use_notifications_summary,
                new ArrayList<>((android.os.Build.VERSION.SDK_INT < android.os.Build.VERSION_CODES.O ?
                        Arrays.asList("pref_notify_frequency", "pref_notify_network_unmetered", "pref_notify_sound", "pref_notify_vibrate", "pref_open_system_notifications_settings") :
                        Arrays.asList("pref_notify_frequency", "pref_notify_network_unmetered", "pref_open_system_notifications_settings")
                )),
                null
        ));
        preferences.add(new PreferenceList("pref_notify_frequency", "30", R.string.pref_notify_frequency, R.array.pref_notifications_interval_titles, R.array.pref_notifications_interval_values, true));
        preferences.add(new PreferenceSwitch("pref_notify_network_unmetered", false, R.string.pref_notify_network_unmetered, R.string.pref_notify_network_unmetered_summary, null, null));
        if (android.os.Build.VERSION.SDK_INT < android.os.Build.VERSION_CODES.O) {
            preferences.add(new PreferenceBasic("pref_notify_sound", "content://settings/system/notification_sound", R.string.pref_notify_sound, true, new PreferenceBasic.Callback() {
                @Override
                public void onPreferenceClicked(ConnectedActivity activity, Preference preference, PreferenceBasic.OnPreferenceClickedCallback callback) {
                    final String value = Storage.pref.get(activity, preference.key, (String) preference.defaultValue).trim();
                    final Uri currentTone = value.isEmpty() ? null : Uri.parse(value);
                    final Intent intent = new Intent(RingtoneManager.ACTION_RINGTONE_PICKER);
                    intent.putExtra(RingtoneManager.EXTRA_RINGTONE_TYPE, RingtoneManager.TYPE_NOTIFICATION);
                    intent.putExtra(RingtoneManager.EXTRA_RINGTONE_TITLE, activity.getString(R.string.notification_sound));
                    intent.putExtra(RingtoneManager.EXTRA_RINGTONE_EXISTING_URI, currentTone);
                    intent.putExtra(RingtoneManager.EXTRA_RINGTONE_SHOW_SILENT, true);
                    intent.putExtra(RingtoneManager.EXTRA_RINGTONE_SHOW_DEFAULT, true);
                    intent.putExtra("com.bukhmastov.cdoitmo.PREFERENCE_KEY", preference.key);
                    final FragmentManager fragmentManager = activity.getSupportFragmentManager();
                    if (fragmentManager != null) {
                        final List<Fragment> fragments = fragmentManager.getFragments();
                        for (Fragment fragment : fragments) {
                            if ("SettingsNotificationsFragment".equals(fragment.getClass().getSimpleName())) {
                                RingtoneHelper.preference_key = preference.key;
                                RingtoneHelper.callback = callback;
                                fragment.startActivityForResult(intent, TONE_PICKER);
                                break;
                            }
                        }
                    }
                }
                @Override
                public String onGetSummary(ConnectedActivity activity, String value) {
                    try {
                        if (TextUtils.isEmpty(value)) {
                            return activity.getString(R.string.pref_ringtone_silent);
                        } else {
                            Ringtone ringtone = RingtoneManager.getRingtone(activity, Uri.parse(value));
                            if (ringtone == null) {
                                return null;
                            } else {
                                return ringtone.getTitle(activity);
                            }
                        }
                    } catch (Exception e) {
                        Static.error(e);
                        return null;
                    }
                }
            }));
            preferences.add(new PreferenceSwitch("pref_notify_vibrate", false, R.string.pref_notify_vibrate, null, null));
        }
        preferences.add(new PreferenceBasic("pref_open_system_notifications_settings", null, R.string.pref_open_system_notifications_settings, false, new PreferenceBasic.Callback() {
            @Override
            public void onPreferenceClicked(final ConnectedActivity activity, final Preference preference, final PreferenceBasic.OnPreferenceClickedCallback callback) {
                Static.T.runOnUiThread(new Runnable() {
                    @Override
                    public void run() {
                        try {
                            Intent intent = new Intent("android.settings.APP_NOTIFICATION_SETTINGS");
                            intent.putExtra("android.provider.extra.APP_PACKAGE", activity.getPackageName());
                            intent.putExtra("app_package", activity.getPackageName());
                            intent.putExtra("app_uid", activity.getApplicationInfo().uid);
                            intent.setFlags(Intent.FLAG_ACTIVITY_NEW_TASK);
                            activity.startActivity(intent);
                        } catch (Exception e) {
                            Static.snackBar(activity, activity.getString(R.string.something_went_wrong));
                        }
                    }
                });
            }
            @Override
            public String onGetSummary(ConnectedActivity activity, String value) {
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

    @Override
    public void onActivityResult(int requestCode, int resultCode, Intent data) {
        if (resultCode == Activity.RESULT_OK) {
            switch (requestCode) {
                case TONE_PICKER: {
                    if (RingtoneHelper.preference_key != null) {
                        Uri ringtone = data.getParcelableExtra(RingtoneManager.EXTRA_RINGTONE_PICKED_URI);
                        String value = ringtone == null ? "" : ringtone.toString();
                        Storage.pref.put(activity, RingtoneHelper.preference_key, value);
                        RingtoneHelper.callback.onSetSummary(activity, value);
                        RingtoneHelper.preference_key = null;
                        RingtoneHelper.callback = null;
                    }
                    break;
                }
            }
        }
    }
}

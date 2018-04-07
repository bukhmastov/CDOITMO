package com.bukhmastov.cdoitmo.object.preference;

import android.content.Context;
import android.support.annotation.StringRes;
import android.view.InflateException;
import android.view.LayoutInflater;
import android.view.View;

import com.bukhmastov.cdoitmo.R;
import com.bukhmastov.cdoitmo.activity.ConnectedActivity;
import com.bukhmastov.cdoitmo.firebase.FirebaseAnalyticsProvider;
import com.bukhmastov.cdoitmo.firebase.FirebaseCrashlyticsProvider;
import com.bukhmastov.cdoitmo.util.ProtocolTracker;
import com.bukhmastov.cdoitmo.util.Static;
import com.bukhmastov.cdoitmo.util.Storage;

import java.util.ArrayList;

public abstract class Preference {
    protected static final String TAG = "Preference";
    public String key = null;
    public Object defaultValue = null;
    public @StringRes int title = 0;
    public @StringRes int summary = 0;
    protected final ArrayList<PreferenceSwitch> preferenceDependencies = new ArrayList<>();
    public Preference(String key, Object defaultValue, @StringRes int title, @StringRes int summary) {
        this.key = key;
        this.defaultValue = defaultValue;
        this.title = title;
        this.summary = summary;
    }
    public Preference(String key, Object defaultValue, @StringRes int title) {
        this.key = key;
        this.defaultValue = defaultValue;
        this.title = title;
    }
    public void setPreferenceDependency(PreferenceSwitch preferenceDependency) {
        if (preferenceDependency != null) {
            preferenceDependencies.add(preferenceDependency);
        }
    }
    protected boolean isDisabled() {
        for (PreferenceSwitch preferenceSwitch : preferenceDependencies) {
            if (!preferenceSwitch.enabled) return true;
        }
        return false;
    }
    public static View getView(final ConnectedActivity activity, final Preference preference) {
        if (preference instanceof PreferenceList) {
            return PreferenceList.getView(activity, (PreferenceList) preference);
        } else if (preference instanceof PreferenceSwitch) {
            return PreferenceSwitch.getView(activity, (PreferenceSwitch) preference);
        } else if (preference instanceof PreferenceEditText) {
            return PreferenceEditText.getView(activity, (PreferenceEditText) preference);
        } else if (preference instanceof PreferenceBasic) {
            return PreferenceBasic.getView(activity, (PreferenceBasic) preference);
        } else {
            return null;
        }
    }
    protected static View inflate(final Context context, final int layoutId) throws InflateException {
        return ((LayoutInflater) context.getSystemService(Context.LAYOUT_INFLATER_SERVICE)).inflate(layoutId, null);
    }
    protected static void onPreferenceChanged(final ConnectedActivity activity, final String key) {
        switch (key) {
            case "pref_use_notifications":
            case "pref_notify_frequency":
            case "pref_notify_network_unmetered":
                Static.T.runThread(Static.T.BACKGROUND, () -> new ProtocolTracker(activity).restart());
                break;
            case "pref_protocol_changes_track":
                if (Storage.pref.get(activity, "pref_protocol_changes_track", true)) {
                    Static.protocolChangesTrackSetup(activity, 0);
                } else {
                    Storage.file.cache.clear(activity, "protocol#log");
                }
                break;
            case "pref_allow_send_reports":
                FirebaseCrashlyticsProvider.setEnabled(activity, Storage.pref.get(activity, "pref_allow_send_reports", true));
                break;
            case "pref_allow_collect_analytics":
                FirebaseAnalyticsProvider.setEnabled(activity, Storage.pref.get(activity, "pref_allow_collect_analytics", true), true);
                break;
            case "pref_use_cache":
                if (!Storage.pref.get(activity, "pref_use_cache", true)) {
                    Storage.file.cache.clear(activity);
                }
                break;
            case "pref_use_university_cache":
                if (!Storage.pref.get(activity, "pref_use_university_cache", false)) {
                    Storage.file.general.cache.clear(activity, "university");
                }
                break;
            case "pref_group_force_override":
                Storage.pref.put(activity, "pref_group_force_override", Static.prettifyGroupNumber(Storage.pref.get(activity, "pref_group_force_override", "")));
                break;
            case "pref_lang":
                Static.snackBar(activity, activity.getString(R.string.restart_required), activity.getString(R.string.restart), v -> Static.reLaunch(activity));
                break;
        }
    }
}

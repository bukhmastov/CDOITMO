package com.bukhmastov.cdoitmo.object.preference;

import android.content.Context;
import android.support.annotation.LayoutRes;
import android.support.annotation.Nullable;
import android.support.annotation.StringRes;
import android.view.InflateException;
import android.view.LayoutInflater;
import android.view.View;

import com.bukhmastov.cdoitmo.R;
import com.bukhmastov.cdoitmo.activity.ConnectedActivity;
import com.bukhmastov.cdoitmo.firebase.FirebaseAnalyticsProvider;
import com.bukhmastov.cdoitmo.firebase.FirebaseCrashlyticsProvider;
import com.bukhmastov.cdoitmo.network.DeIfmoRestClient;
import com.bukhmastov.cdoitmo.object.ProtocolTracker;
import com.bukhmastov.cdoitmo.util.BottomBar;
import com.bukhmastov.cdoitmo.util.Log;
import com.bukhmastov.cdoitmo.util.Static;
import com.bukhmastov.cdoitmo.util.Storage;
import com.bukhmastov.cdoitmo.util.StoragePref;
import com.bukhmastov.cdoitmo.util.TextUtils;
import com.bukhmastov.cdoitmo.util.Thread;

import java.util.ArrayList;

public abstract class Preference {

    protected static final String TAG = "Preference";
    public String key = null;
    public Object defaultValue = null;
    public @StringRes int title = 0;
    public @StringRes int summary = 0;

    //@Inject
    protected Storage storage = Storage.instance();
    //@Inject
    protected StoragePref storagePref = StoragePref.instance();
    //@Inject
    private DeIfmoRestClient deIfmoRestClient = DeIfmoRestClient.instance();

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

    public void applyDefaultValue(final Context context) {
        if (defaultValue != null && !storagePref.exists(context, key)) {
            if (defaultValue instanceof String) {
                storagePref.put(context, key, (String) defaultValue);
            } else if (defaultValue instanceof Integer) {
                storagePref.put(context, key, (Integer) defaultValue);
            } else if (defaultValue instanceof Boolean) {
                storagePref.put(context, key, (Boolean) defaultValue);
            }
        }
    }

    protected boolean isDisabled() {
        for (PreferenceSwitch preferenceSwitch : preferenceDependencies) {
            if (!preferenceSwitch.enabled) return true;
        }
        return false;
    }

    protected void onPreferenceChanged(final ConnectedActivity activity) {
        if (key == null) {
            return;
        }
        switch (key) {
            case "pref_use_notifications":
            case "pref_notify_frequency":
            case "pref_notify_network_unmetered":
                Thread.run(Thread.BACKGROUND, () -> new ProtocolTracker(activity).restart());
                break;
            case "pref_protocol_changes_track":
                if (storagePref.get(activity, "pref_protocol_changes_track", true)) {
                    ProtocolTracker.setup(activity, deIfmoRestClient, storagePref, 0);
                } else {
                    storage.clear(activity, Storage.CACHE, Storage.USER, "protocol#log");
                }
                break;
            case "pref_allow_send_reports":
                FirebaseCrashlyticsProvider.setEnabled(activity, storagePref.get(activity, "pref_allow_send_reports", true));
                break;
            case "pref_allow_collect_analytics":
                FirebaseAnalyticsProvider.setEnabled(activity, storagePref.get(activity, "pref_allow_collect_analytics", true), true);
                break;
            case "pref_use_cache":
                if (!storagePref.get(activity, "pref_use_cache", true)) {
                    storage.clear(activity, Storage.CACHE, Storage.USER);
                }
                break;
            case "pref_use_university_cache":
                if (!storagePref.get(activity, "pref_use_university_cache", false)) {
                    storage.clear(activity, Storage.CACHE, Storage.GLOBAL, "university");
                }
                break;
            case "pref_group_force_override":
                storagePref.put(activity, "pref_group_force_override", TextUtils.prettifyGroupNumber(storagePref.get(activity, "pref_group_force_override", "")));
                break;
            case "pref_lang":
                BottomBar.snackBar(activity, activity.getString(R.string.restart_required), activity.getString(R.string.restart), v -> Static.reLaunch(activity));
                break;
        }
    }

    @Nullable
    protected static View inflate(final Context context, @LayoutRes final int layout) throws InflateException {
        if (context == null) {
            Log.e(TAG, "Failed to inflate layout, context is null");
            return null;
        }
        LayoutInflater inflater = (LayoutInflater) context.getSystemService(Context.LAYOUT_INFLATER_SERVICE);
        if (inflater == null) {
            Log.e(TAG, "Failed to inflate layout, inflater is null");
            return null;
        }
        return inflater.inflate(layout, null);
    }

    @Nullable
    public static View getView(final ConnectedActivity activity, final Preference preference, final StoragePref storagePref) {
        if (preference instanceof PreferenceList) {
            return PreferenceList.getView(activity, (PreferenceList) preference, storagePref);
        } else if (preference instanceof PreferenceSwitch) {
            return PreferenceSwitch.getView(activity, (PreferenceSwitch) preference, storagePref);
        } else if (preference instanceof PreferenceEditText) {
            return PreferenceEditText.getView(activity, (PreferenceEditText) preference, storagePref);
        } else if (preference instanceof PreferenceBasic) {
            return PreferenceBasic.getView(activity, (PreferenceBasic) preference, storagePref);
        } else {
            return null;
        }
    }
}

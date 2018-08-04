package com.bukhmastov.cdoitmo.object.preference;

import android.content.Context;
import androidx.annotation.LayoutRes;
import androidx.annotation.Nullable;
import androidx.annotation.StringRes;
import android.view.InflateException;
import android.view.LayoutInflater;
import android.view.View;

import com.bukhmastov.cdoitmo.R;
import com.bukhmastov.cdoitmo.activity.ConnectedActivity;
import com.bukhmastov.cdoitmo.factory.AppComponentProvider;
import com.bukhmastov.cdoitmo.firebase.FirebaseAnalyticsProvider;
import com.bukhmastov.cdoitmo.firebase.FirebaseCrashlyticsProvider;
import com.bukhmastov.cdoitmo.network.DeIfmoRestClient;
import com.bukhmastov.cdoitmo.object.ProtocolTracker;
import com.bukhmastov.cdoitmo.provider.InjectProvider;
import com.bukhmastov.cdoitmo.util.NotificationMessage;
import com.bukhmastov.cdoitmo.util.Log;
import com.bukhmastov.cdoitmo.util.Static;
import com.bukhmastov.cdoitmo.util.Storage;
import com.bukhmastov.cdoitmo.util.StoragePref;
import com.bukhmastov.cdoitmo.util.TextUtils;
import com.bukhmastov.cdoitmo.util.Thread;

import java.util.ArrayList;

import javax.inject.Inject;

public abstract class Preference {

    protected static final String TAG = "Preference";
    public String key = null;
    public Object defaultValue = null;
    public @StringRes int title = 0;
    public @StringRes int summary = 0;

    @Inject
    Log log;
    @Inject
    Thread thread;
    @Inject
    Storage storage;
    @Inject
    StoragePref storagePref;
    @Inject
    DeIfmoRestClient deIfmoRestClient;
    @Inject
    ProtocolTracker protocolTracker;
    @Inject
    NotificationMessage notificationMessage;
    @Inject
    Static staticUtil;
    @Inject
    TextUtils textUtils;
    @Inject
    FirebaseAnalyticsProvider firebaseAnalyticsProvider;
    @Inject
    FirebaseCrashlyticsProvider firebaseCrashlyticsProvider;

    protected final ArrayList<PreferenceSwitch> preferenceDependencies = new ArrayList<>();

    public Preference(String key, Object defaultValue, @StringRes int title) {
        this(key, defaultValue, title, 0);
    }
    
    public Preference(String key, Object defaultValue, @StringRes int title, @StringRes int summary) {
        AppComponentProvider.getComponent().inject(this);
        this.key = key;
        this.defaultValue = defaultValue;
        this.title = title;
        this.summary = summary;
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
        log.d(TAG, "onPreferenceChanged | key=", key);
        switch (key) {
            case "pref_use_notifications":
            case "pref_notify_frequency":
            case "pref_notify_network_unmetered":
                thread.run(Thread.BACKGROUND, () -> protocolTracker.restart(activity));
                break;
            case "pref_protocol_changes_track":
                if (storagePref.get(activity, "pref_protocol_changes_track", true)) {
                    protocolTracker.setup(activity, deIfmoRestClient, 0);
                } else {
                    storage.clear(activity, Storage.CACHE, Storage.USER, "protocol#log");
                }
                break;
            case "pref_allow_send_reports":
                firebaseCrashlyticsProvider.setEnabled(activity, storagePref.get(activity, "pref_allow_send_reports", true));
                break;
            case "pref_allow_collect_analytics":
                firebaseAnalyticsProvider.setEnabled(activity, storagePref.get(activity, "pref_allow_collect_analytics", true), true);
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
                storagePref.put(activity, "pref_group_force_override", textUtils.prettifyGroupNumber(storagePref.get(activity, "pref_group_force_override", "")));
                break;
            case "pref_lang":
                notificationMessage.snackBar(activity, activity.getString(R.string.restart_required), activity.getString(R.string.restart), v -> staticUtil.reLaunch(activity));
                break;
        }
    }

    @Nullable
    protected static View inflate(final Context context, @LayoutRes final int layout) throws InflateException {
        if (context == null) {
            return null;
        }
        LayoutInflater inflater = (LayoutInflater) context.getSystemService(Context.LAYOUT_INFLATER_SERVICE);
        if (inflater == null) {
            return null;
        }
        return inflater.inflate(layout, null);
    }

    @Nullable
    public static View getView(final ConnectedActivity activity, final Preference preference, final InjectProvider injectProvider) {
        if (preference instanceof PreferenceList) {
            return PreferenceList.getView(activity, (PreferenceList) preference, injectProvider);
        } else if (preference instanceof PreferenceSwitch) {
            return PreferenceSwitch.getView(activity, (PreferenceSwitch) preference, injectProvider);
        } else if (preference instanceof PreferenceEditText) {
            return PreferenceEditText.getView(activity, (PreferenceEditText) preference, injectProvider);
        } else if (preference instanceof PreferenceBasic) {
            return PreferenceBasic.getView(activity, (PreferenceBasic) preference, injectProvider);
        } else {
            return null;
        }
    }
}

package com.bukhmastov.cdoitmo.firebase;

import com.bukhmastov.cdoitmo.R;
import com.bukhmastov.cdoitmo.util.Log;
import com.bukhmastov.cdoitmo.util.Thread;
import com.google.firebase.remoteconfig.FirebaseRemoteConfig;
import com.google.firebase.remoteconfig.FirebaseRemoteConfigSettings;

import org.json.JSONObject;
import org.json.JSONTokener;

public class FirebaseConfigProvider {

    private static final String TAG = "FirebaseConfigProvider";
    private static final boolean DEBUG = false;

    public static final String MESSAGE_LOGIN = "message_login";
    public static final String MESSAGE_MENU = "message_menu";
    public static final String PERFORMANCE_ENABLED = "performance_enabled";

    private static final long cacheExpiration = 7200; // 2 hours
    private interface Callback {
        void onComplete(boolean successful);
    }
    public interface Result {
        void onResult(String value);
    }
    public interface ResultJson {
        void onResult(JSONObject value);
    }

    private static FirebaseRemoteConfig getFirebaseRemoteConfig() {
        FirebaseRemoteConfig firebaseRemoteConfig = FirebaseRemoteConfig.getInstance();
        if (DEBUG) {
            firebaseRemoteConfig.setConfigSettings(new FirebaseRemoteConfigSettings.Builder()
                    .setDeveloperModeEnabled(true)
                    .build());
        }
        firebaseRemoteConfig.setDefaults(R.xml.remote_config_defaults);
        return firebaseRemoteConfig;
    }

    public static void getString(final String key, final Result result) {
        Log.v(TAG, "getString | key=" + key);
        fetch(successful -> {
            String value = getFirebaseRemoteConfig().getString(key);
            Log.v(TAG, "getString | onComplete | key=" + key + " | value=" + value);
            result.onResult(value);
        });
    }
    public static void getJson(final String key, final ResultJson result) {
        Log.v(TAG, "getJson | key=" + key);
        fetch(successful -> {
            try {
                String value = getFirebaseRemoteConfig().getString(key);
                Log.v(TAG, "getJson | onComplete | key=" + key + " | value=" + value);
                if (value == null || value.isEmpty()) {
                    result.onResult(null);
                    return;
                }
                Object object = new JSONTokener(value).nextValue();
                if (object instanceof JSONObject) {
                    result.onResult((JSONObject) object);
                } else {
                    result.onResult(null);
                }
            } catch (Exception ignore) {
                result.onResult(null);
            }
        });
    }

    private static void fetch(final Callback callback) {
        Thread.run(() -> {
            Log.v(TAG, "fetch");
            getFirebaseRemoteConfig().fetch(DEBUG ? 0 : cacheExpiration)
                    .addOnCompleteListener(task -> Thread.run(() -> {
                        boolean successful = task.isSuccessful();
                        Log.v(TAG, "fetch | onComplete | successful=", successful);
                        if (successful) {
                            getFirebaseRemoteConfig().activateFetched();
                        }
                        callback.onComplete(successful);
                    }));
        });
    }
}

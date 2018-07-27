package com.bukhmastov.cdoitmo.firebase.impl;

import com.bukhmastov.cdoitmo.R;
import com.bukhmastov.cdoitmo.factory.AppComponentProvider;
import com.bukhmastov.cdoitmo.firebase.FirebaseConfigProvider;
import com.bukhmastov.cdoitmo.util.Log;
import com.bukhmastov.cdoitmo.util.Thread;

import org.json.JSONObject;
import org.json.JSONTokener;

import javax.inject.Inject;

public class FirebaseConfigProviderImpl implements FirebaseConfigProvider {

    private static final String TAG = "FirebaseConfigProvider";
    private static final boolean DEBUG = false;
    private static final long cacheExpiration = 7200; // 2 hours

    private interface Callback {
        void onComplete(boolean successful);
    }

    @Inject
    Log log;
    @Inject
    Thread thread;

    public FirebaseConfigProviderImpl() {
        AppComponentProvider.getComponent().inject(this);
    }

    private FirebaseRemoteConfig getFirebaseRemoteConfig() {
        FirebaseRemoteConfig firebaseRemoteConfig = FirebaseRemoteConfig.getInstance();
        if (DEBUG) {
            firebaseRemoteConfig.setConfigSettings(new FirebaseRemoteConfigSettings.Builder()
                    .setDeveloperModeEnabled(true)
                    .build());
        }
        firebaseRemoteConfig.setDefaults(R.xml.remote_config_defaults);
        return firebaseRemoteConfig;
    }

    @Override
    public void getString(final String key, final Result result) {
        log.v(TAG, "getString | key=" + key);
        fetch(successful -> {
            String value = getFirebaseRemoteConfig().getString(key);
            log.v(TAG, "getString | onComplete | key=" + key + " | value=" + value);
            result.onResult(value);
        });
    }

    @Override
    public void getJson(final String key, final ResultJson result) {
        log.v(TAG, "getJson | key=" + key);
        fetch(successful -> {
            try {
                String value = getFirebaseRemoteConfig().getString(key);
                log.v(TAG, "getJson | onComplete | key=" + key + " | value=" + value);
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

    private void fetch(final Callback callback) {
        thread.run(() -> {
            log.v(TAG, "fetch");
            getFirebaseRemoteConfig().fetch(DEBUG ? 0 : cacheExpiration)
                    .addOnCompleteListener(task -> thread.run(() -> {
                        boolean successful = task.isSuccessful();
                        log.v(TAG, "fetch | onComplete | successful=", successful);
                        if (successful) {
                            getFirebaseRemoteConfig().activateFetched();
                        }
                        callback.onComplete(successful);
                    }));
        });
    }
}

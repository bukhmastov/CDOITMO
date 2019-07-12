package com.bukhmastov.cdoitmo.firebase.impl;

import android.content.Context;

import androidx.annotation.NonNull;

import com.bukhmastov.cdoitmo.R;
import com.bukhmastov.cdoitmo.factory.AppComponentProvider;
import com.bukhmastov.cdoitmo.firebase.FirebaseConfigProvider;
import com.bukhmastov.cdoitmo.function.Consumer;
import com.bukhmastov.cdoitmo.model.firebase.config.FBConfigMessage;
import com.bukhmastov.cdoitmo.util.Log;
import com.bukhmastov.cdoitmo.util.StoragePref;
import com.bukhmastov.cdoitmo.util.Thread;
import com.google.firebase.remoteconfig.FirebaseRemoteConfig;
import com.google.firebase.remoteconfig.FirebaseRemoteConfigSettings;

import javax.inject.Inject;

public class FirebaseConfigProviderImpl implements FirebaseConfigProvider {

    private static final String TAG = "FirebaseConfigProvider";
    private static final boolean DEBUG = false;
    private static final long CACHE_EXPIRATION_SEC = 12 * 60 * 60;

    @Inject
    Log log;
    @Inject
    Thread thread;
    @Inject
    StoragePref storagePref;

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
    public void getString(@NonNull Context context, String key, Result result) {
        log.v(TAG, "getString | key=", key);
        thread.assertNotUI();
        fetch(context, successful -> {
            String value = getFirebaseRemoteConfig().getString(key);
            log.v(TAG, "getString | onComplete | key=", key, " | value=", value);
            result.onResult(value);
        });
    }

    @Override
    public void getMessage(@NonNull Context context, String key, ResultMessage result) {
        log.v(TAG, "getMessage | key=", key);
        thread.assertNotUI();
        fetch(context, successful -> {
            try {
                String value = getFirebaseRemoteConfig().getString(key);
                log.v(TAG, "getMessage | onComplete | key=", key, " | value=", value);
                if (value == null || value.isEmpty()) {
                    result.onResult(null);
                    return;
                }
                result.onResult(new FBConfigMessage().fromJsonString(value));
            } catch (Exception ignore) {
                result.onResult(null);
            }
        });
    }

    private void fetch(Context context, Consumer<Boolean> onComplete) {

        long expiration = DEBUG ? 0 : CACHE_EXPIRATION_SEC;
        if (storagePref.get(context, "pref_remote_config_invalidate", false)) {
            expiration = 0;
            storagePref.put(context, "pref_remote_config_invalidate", false);
        }

        log.v(TAG, "fetch | expiration=", expiration);

        getFirebaseRemoteConfig()
                .fetch(expiration)
                .addOnCompleteListener(task -> thread.standalone(() -> {
                    boolean successful = task.isSuccessful();
                    log.v(TAG, "fetch | onComplete | successful=", successful);
                    if (successful) {
                        getFirebaseRemoteConfig().activate();
                    }
                    onComplete.accept(successful);
                }));
    }
}

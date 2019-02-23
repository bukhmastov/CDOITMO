package com.bukhmastov.cdoitmo.firebase.impl;

import com.bukhmastov.cdoitmo.R;
import com.bukhmastov.cdoitmo.factory.AppComponentProvider;
import com.bukhmastov.cdoitmo.firebase.FirebaseConfigProvider;
import com.bukhmastov.cdoitmo.function.Consumer;
import com.bukhmastov.cdoitmo.model.firebase.config.FBConfigMessage;
import com.bukhmastov.cdoitmo.util.Log;
import com.bukhmastov.cdoitmo.util.Thread;
import com.google.firebase.remoteconfig.FirebaseRemoteConfig;
import com.google.firebase.remoteconfig.FirebaseRemoteConfigSettings;

import javax.inject.Inject;

public class FirebaseConfigProviderImpl implements FirebaseConfigProvider {

    private static final String TAG = "FirebaseConfigProvider";
    private static final boolean DEBUG = false;
    private static final long cacheExpiration = 7200; // 2 hours


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
    public void getString(String key, Result result) {
        log.v(TAG, "getString | key=", key);
        thread.assertNotUI();
        fetch(successful -> {
            String value = getFirebaseRemoteConfig().getString(key);
            log.v(TAG, "getString | onComplete | key=", key, " | value=", value);
            result.onResult(value);
        });
    }

    @Override
    public void getMessage(String key, ResultMessage result) {
        log.v(TAG, "getMessage | key=", key);
        thread.assertNotUI();
        fetch(successful -> {
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

    private void fetch(Consumer<Boolean> onComplete) {
        log.v(TAG, "fetch");
        getFirebaseRemoteConfig().fetch(DEBUG ? 0 : cacheExpiration)
                .addOnCompleteListener(task -> thread.standalone(() -> {
                    boolean successful = task.isSuccessful();
                    log.v(TAG, "fetch | onComplete | successful=", successful);
                    if (successful) {
                        getFirebaseRemoteConfig().activateFetched();
                    }
                    onComplete.accept(successful);
                }));
    }
}

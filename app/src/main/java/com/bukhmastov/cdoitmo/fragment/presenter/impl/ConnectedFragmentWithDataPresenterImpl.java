package com.bukhmastov.cdoitmo.fragment.presenter.impl;

import com.bukhmastov.cdoitmo.fragment.presenter.ConnectedFragmentPresenter;
import com.bukhmastov.cdoitmo.model.JsonEntity;
import com.bukhmastov.cdoitmo.util.Storage;
import com.bukhmastov.cdoitmo.util.singleton.StringUtils;

import org.json.JSONException;

import androidx.annotation.Nullable;

public abstract class ConnectedFragmentWithDataPresenterImpl<T extends JsonEntity>
        extends ConnectedFragmentPresenterImpl implements ConnectedFragmentPresenter {

    private T data = null;
    private final Class<T> entityClass;

    public ConnectedFragmentWithDataPresenterImpl(Class<T> entityClass) {
        super();
        this.entityClass = entityClass;
    }

    protected abstract String getLogTag();
    protected abstract @Storage.Type String getCacheType();
    protected abstract String getCachePath();
    protected abstract void load();
    protected abstract void display();

    @Override
    public void onResume() {
        thread.run(() -> {
            log.v(getLogTag(), "Fragment resumed");
            firebaseAnalyticsProvider.setCurrentScreen(activity, fragment);
            if (!loaded) {
                loaded = true;
                if (getData() == null) {
                    load();
                } else {
                    display();
                }
            }
        });
    }

    protected void putToCache(@Nullable T entity) throws JSONException, IllegalAccessException {
        thread.assertNotUI();
        if (getCachePath() == null || getCacheType() == null) {
            return;
        }
        if (entity == null) {
            return;
        }
        if (storagePref.get(activity, "pref_use_cache", true)) {
            storage.put(activity, Storage.CACHE, getCacheType(), getCachePath(), entity.toJsonString());
        }
    }

    protected @Nullable T getFromCache() {
        thread.assertNotUI();
        if (getCachePath() == null || getCacheType() == null) {
            return null;
        }
        if (entityClass == null) {
            return null;
        }
        String cache = storage.get(activity, Storage.CACHE, getCacheType(), getCachePath()).trim();
        if (StringUtils.isBlank(cache)) {
            return null;
        }
        try {
            return entityClass.newInstance().fromJsonString(cache);
        } catch (Exception e) {
            storage.delete(activity, Storage.CACHE, getCacheType(), getCachePath());
            return null;
        }
    }

    protected void clearData() {
        setData(null);
    }

    protected void setData(@Nullable T data) {
        try {
            thread.assertNotUI();
            this.data = data;
            if (fragment == null) {
                return;
            }
            if (data == null) {
                fragment.clearData();
            } else {
                fragment.storeData(data.toJsonString());
            }
        } catch (Exception exception) {
            log.w(getLogTag(), "Failed to set data | exception=", exception);
            log.exception(exception);
        }
    }

    protected @Nullable T getData() {
        try {
            thread.assertNotUI();
            if (data != null) {
                return data;
            }
            if (fragment != null && entityClass != null) {
                String stored = fragment.restoreData();
                if (StringUtils.isNotBlank(stored)) {
                    data = entityClass.newInstance().fromJsonString(stored);
                    return data;
                }
            }
            data = getFromCache();
            return data;
        } catch (Exception exception) {
            log.w(getLogTag(), "Failed to get data | exception=", exception);
            log.exception(exception);
        }
        return null;
    }
}

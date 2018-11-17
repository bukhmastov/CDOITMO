package com.bukhmastov.cdoitmo.widget.impl;

import android.appwidget.AppWidgetManager;
import android.content.Context;

import com.bukhmastov.cdoitmo.factory.AppComponentProvider;
import com.bukhmastov.cdoitmo.model.schedule.lessons.SLessons;
import com.bukhmastov.cdoitmo.model.widget.schedule.lessons.WSLSettings;
import com.bukhmastov.cdoitmo.util.Log;
import com.bukhmastov.cdoitmo.util.Storage;
import com.bukhmastov.cdoitmo.util.singleton.StringUtils;
import com.bukhmastov.cdoitmo.widget.ScheduleLessonsWidgetStorage;

import javax.inject.Inject;

public class ScheduleLessonsWidgetStorageImpl implements ScheduleLessonsWidgetStorage {

    private final String TAG = "SLWidgetStorage";

    @Inject
    Log log;
    @Inject
    Storage storage;
    @Inject
    Context context;

    public ScheduleLessonsWidgetStorageImpl() {
        AppComponentProvider.getComponent().inject(this);
    }

    @Override
    public WSLSettings getSettings(int appWidgetId) throws Exception {
        String settings = get(appWidgetId, "settings");
        if (StringUtils.isBlank(settings)) {
            return null;
        }
        return new WSLSettings().fromJsonString(settings);
    }

    @Override
    public SLessons getConvertedCache(int appWidgetId) throws Exception {
        String cache = get(appWidgetId, "cache_converted");
        if (StringUtils.isBlank(cache)) {
            return null;
        }
        return new SLessons().fromJsonString(cache);
    }

    @Override
    public void save(int appWidgetId, SLessons cache) throws Exception {
        save(appWidgetId, "cache_converted", cache.toJsonString());
    }

    @Override
    public void save(int appWidgetId, WSLSettings settings) throws Exception {
        save(appWidgetId, "settings", settings.toJsonString());
    }

    @Override
    public void delete(int appWidgetId) {
        log.v(TAG, "delete | appWidgetId=", appWidgetId);
        if (appWidgetId == AppWidgetManager.INVALID_APPWIDGET_ID) {
            log.w(TAG, "delete | prevented due to invalid appwidget id");
            return;
        }
        storage.clear(context, Storage.PERMANENT, Storage.GLOBAL, "widget_schedule_lessons#" + appWidgetId);
    }

    private String get(int appWidgetId, String type) {
        log.v(TAG, "get | appWidgetId=", appWidgetId, " | type=", type);
        String pref;
        if (appWidgetId == AppWidgetManager.INVALID_APPWIDGET_ID) {
            log.w(TAG, "get | prevented due to invalid appwidget id");
            pref = "";
        } else {
            pref = storage.get(context, Storage.PERMANENT, Storage.GLOBAL, "widget_schedule_lessons#" + appWidgetId + "#" + type).trim();
        }
        return pref.isEmpty() ? null : pref;
    }

    private void save(int appWidgetId, String type, String text) {
        log.v(TAG, "save | appWidgetId=", appWidgetId + " | type=", type);
        if (appWidgetId == AppWidgetManager.INVALID_APPWIDGET_ID) {
            log.w(TAG, "save | prevented due to invalid appwidget id");
            return;
        }
        storage.put(context, Storage.PERMANENT, Storage.GLOBAL, "widget_schedule_lessons#" + appWidgetId + "#" + type, text);
    }

    private void delete(int appWidgetId, String type) {
        log.v(TAG, "delete | appWidgetId=", appWidgetId, " | type=", type);
        if (appWidgetId == AppWidgetManager.INVALID_APPWIDGET_ID) {
            log.w(TAG, "delete | prevented due to invalid appwidget id");
            return;
        }
        storage.delete(context, Storage.PERMANENT, Storage.GLOBAL, "widget_schedule_lessons#" + appWidgetId + "#" + type);
    }
}

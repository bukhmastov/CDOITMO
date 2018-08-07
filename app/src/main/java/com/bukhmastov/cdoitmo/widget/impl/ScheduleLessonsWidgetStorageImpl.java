package com.bukhmastov.cdoitmo.widget.impl;

import android.appwidget.AppWidgetManager;
import android.content.Context;

import com.bukhmastov.cdoitmo.factory.AppComponentProvider;
import com.bukhmastov.cdoitmo.util.Log;
import com.bukhmastov.cdoitmo.util.Storage;
import com.bukhmastov.cdoitmo.widget.ScheduleLessonsWidgetStorage;

import org.json.JSONObject;

import javax.inject.Inject;

public class ScheduleLessonsWidgetStorageImpl implements ScheduleLessonsWidgetStorage {

    private final String TAG = "SLWidgetStorage";

    @Inject
    Log log;
    @Inject
    Storage storage;

    public ScheduleLessonsWidgetStorageImpl() {
        AppComponentProvider.getComponent().inject(this);
    }

    @Override
    public String get(Context context, int appWidgetId, String type) {
        log.v(TAG, "get | appWidgetId=" + appWidgetId + " | type=" + type);
        String pref;
        if (appWidgetId == AppWidgetManager.INVALID_APPWIDGET_ID) {
            log.w(TAG, "get | prevented due to invalid appwidget id");
            pref = "";
        } else {
            pref = storage.get(context, Storage.PERMANENT, Storage.GLOBAL, "widget_schedule_lessons#" + appWidgetId + "#" + type).trim();
        }
        return pref.isEmpty() ? null : pref;
    }

    @Override
    public JSONObject getJson(Context context, int appWidgetId, String type) {
        log.v(TAG, "getJson | appWidgetId=" + appWidgetId + " | type=" + type);
        JSONObject pref;
        try {
            String tmp = get(context, appWidgetId, type);
            if (tmp == null) throw new NullPointerException(type + " is null");
            pref = new JSONObject(tmp);
        } catch (Exception e) {
            pref = null;
        }
        return pref;
    }

    @Override
    public void save(Context context, int appWidgetId, String type, String text) {
        log.v(TAG, "save | appWidgetId=" + appWidgetId + " | type=" + type);
        if (appWidgetId == AppWidgetManager.INVALID_APPWIDGET_ID) {
            log.w(TAG, "save | prevented due to invalid appwidget id");
            return;
        }
        storage.put(context, Storage.PERMANENT, Storage.GLOBAL, "widget_schedule_lessons#" + appWidgetId + "#" + type, text);
    }

    @Override
    public void delete(Context context, int appWidgetId, String type) {
        log.v(TAG, "delete | appWidgetId=" + appWidgetId + " | type=" + type);
        if (appWidgetId == AppWidgetManager.INVALID_APPWIDGET_ID) {
            log.w(TAG, "delete | prevented due to invalid appwidget id");
            return;
        }
        storage.delete(context, Storage.PERMANENT, Storage.GLOBAL, "widget_schedule_lessons#" + appWidgetId + "#" + type);
    }

    @Override
    public void delete(Context context, int appWidgetId) {
        log.v(TAG, "delete | appWidgetId=" + appWidgetId);
        if (appWidgetId == AppWidgetManager.INVALID_APPWIDGET_ID) {
            log.w(TAG, "delete | prevented due to invalid appwidget id");
            return;
        }
        storage.clear(context, Storage.PERMANENT, Storage.GLOBAL, "widget_schedule_lessons#" + appWidgetId);
    }
}

package com.bukhmastov.cdoitmo.widget;

import android.appwidget.AppWidgetManager;
import android.content.Context;

import com.bukhmastov.cdoitmo.util.Log;
import com.bukhmastov.cdoitmo.util.Storage;

import org.json.JSONObject;

//TODO interface - impl
public class ScheduleLessonsWidgetStorage {

    private static final String TAG = "SLWidget";

    //@Inject
    //TODO interface - impl: remove static
    private static Log log = Log.instance();
    //@Inject
    //TODO interface - impl: remove static
    private static Storage storage = Storage.instance();

    public static String get(Context context, int appWidgetId, String type) {
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
    public static JSONObject getJson(Context context, int appWidgetId, String type) {
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
    public static void save(Context context, int appWidgetId, String type, String text) {
        log.v(TAG, "save | appWidgetId=" + appWidgetId + " | type=" + type);
        if (appWidgetId == AppWidgetManager.INVALID_APPWIDGET_ID) {
            log.w(TAG, "save | prevented due to invalid appwidget id");
            return;
        }
        storage.put(context, Storage.PERMANENT, Storage.GLOBAL, "widget_schedule_lessons#" + appWidgetId + "#" + type, text);
    }
    public static void delete(Context context, int appWidgetId, String type) {
        log.v(TAG, "delete | appWidgetId=" + appWidgetId + " | type=" + type);
        if (appWidgetId == AppWidgetManager.INVALID_APPWIDGET_ID) {
            log.w(TAG, "delete | prevented due to invalid appwidget id");
            return;
        }
        storage.delete(context, Storage.PERMANENT, Storage.GLOBAL, "widget_schedule_lessons#" + appWidgetId + "#" + type);
    }
    public static void delete(Context context, int appWidgetId) {
        log.v(TAG, "delete | appWidgetId=" + appWidgetId);
        if (appWidgetId == AppWidgetManager.INVALID_APPWIDGET_ID) {
            log.w(TAG, "delete | prevented due to invalid appwidget id");
            return;
        }
        storage.clear(context, Storage.PERMANENT, Storage.GLOBAL, "widget_schedule_lessons#" + appWidgetId);
    }
}

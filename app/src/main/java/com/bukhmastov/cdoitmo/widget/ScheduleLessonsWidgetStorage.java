package com.bukhmastov.cdoitmo.widget;

import android.content.Context;

import org.json.JSONObject;

public interface ScheduleLessonsWidgetStorage  {

    String get(Context context, int appWidgetId, String type);

    JSONObject getJson(Context context, int appWidgetId, String type);

    void save(Context context, int appWidgetId, String type, String text);

    void delete(Context context, int appWidgetId, String type);

    void delete(Context context, int appWidgetId);
}

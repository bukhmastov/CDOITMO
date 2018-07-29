package com.bukhmastov.cdoitmo.object.schedule;

import android.content.Context;

import com.bukhmastov.cdoitmo.network.model.Client;

import org.json.JSONObject;

public interface Schedule {

    int FAILED_LOAD = 100;
    int FAILED_OFFLINE = 101;
    int FAILED_EMPTY_QUERY = 102;
    int FAILED_MINE_NEED_ISU = 103;
    int FAILED_INVALID_QUERY = 104;
    int FAILED_NOT_FOUND = 105;

    void search(final Context context, final Handler handler, final String query);

    void search(final Context context, final Handler handler, final String query, final int refreshRate);

    void search(final Context context, final Handler handler, final String query, final boolean forceToCache);

    void search(final Context context, final Handler handler, final String query, final boolean forceToCache, final boolean withUserChanges);

    void search(final Context context, final Handler handler, final String query, final int refreshRate, final boolean forceToCache);

    void search(final Context context, final Handler handler, final String query, final int refreshRate, final boolean forceToCache, final boolean withUserChanges);

    // Returns the default query string for a schedule search
    String getDefaultScope(final Context context);

    // Returns main title for schedules
    String getScheduleHeader(final Context context, String title, String type);

    // Returns second title for schedules
    String getScheduleWeek(final Context context, int week);

    interface Handler {
        void onSuccess(JSONObject json, boolean fromCache);
        void onFailure(int state);
        void onFailure(int statusCode, Client.Headers headers, int state);
        void onProgress(int state);
        void onNewRequest(Client.Request request);
        void onCancelRequest();
    }

    interface ScheduleSearchProvider {
        void onSearch(Context context, String query, Handler handler);
    }
}

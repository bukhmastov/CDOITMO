package com.bukhmastov.cdoitmo.object.impl;

import android.content.Context;
import android.os.Handler;

import com.bukhmastov.cdoitmo.factory.AppComponentProvider;
import com.bukhmastov.cdoitmo.network.model.Client;
import com.bukhmastov.cdoitmo.object.TeacherSearch;
import com.bukhmastov.cdoitmo.object.schedule.Schedule;
import com.bukhmastov.cdoitmo.object.schedule.ScheduleLessons;
import com.bukhmastov.cdoitmo.util.Log;
import com.bukhmastov.cdoitmo.util.Thread;

import org.json.JSONObject;

import javax.inject.Inject;

public class TeacherSearchImpl implements TeacherSearch {

    private static final String TAG = "TeacherSearch";
    private static final int SEARCH_DELAY_MS = 250;
    private final Handler handler = new Handler();
    private Client.Request requestHandle = null;
    //private String query = "";

    @Inject
    Log log;
    @Inject
    Thread thread;
    @Inject
    Context context;
    @Inject
    ScheduleLessons scheduleLessons;

    public TeacherSearchImpl() {
        AppComponentProvider.getComponent().inject(this);
    }

    @Override
    public void search(final String query, final TeacherSearchCallback callback) {
        thread.run(() -> {
            if (callback == null) {
                return;
            }

            log.v(TAG, "search | query = ", query);

            if (requestHandle != null) {
                requestHandle.cancel();
            }
            handler.removeCallbacksAndMessages(null);

            boolean isEmptyQuery = query == null || query.isEmpty();
            if (isEmptyQuery) {
                log.v(TAG, "search | query = ", query, " | empty");
                callback.onState(REJECTED_EMPTY);
                return;
            }

            //boolean isInvalidQuery = this.query.equals(query); // should be different
            //if (!isInvalidQuery) {
            boolean isInvalidQuery = false;
            try {
                new JSONObject(query);
                isInvalidQuery = true; // should not be json
            } catch (Exception ignore) {
                // ignore
            }
            //}
            if (isInvalidQuery) {
                log.v(TAG, "search | query = ", query, " | rejected");
                callback.onState(REJECTED);
                return;
            }

            //this.query = query;
            callback.onState(ACCEPTED);

            handler.postDelayed(() -> {
                log.v(TAG, "search | query = ", query, " | searching");
                callback.onState(SEARCHING);
                scheduleLessons.search(context, new Schedule.Handler() {
                    @Override
                    public void onSuccess(JSONObject json, boolean fromCache) {
                        log.v(TAG, "search | query = ", query, " | found");
                        callback.onState(FOUND);
                        callback.onSuccess(json);
                    }
                    @Override
                    public void onFailure(int state) {
                        this.onFailure(0, null, state);
                    }
                    @Override
                    public void onFailure(int statusCode, Client.Headers headers, int state) {
                        log.v(TAG, "search | query = ", query, " | not found");
                        callback.onState(NOT_FOUND);
                    }
                    @Override
                    public void onProgress(int state) {}
                    @Override
                    public void onNewRequest(Client.Request request) {
                        requestHandle = request;
                    }
                    @Override
                    public void onCancelRequest() {
                        if (requestHandle != null) {
                            requestHandle.cancel();
                        }
                    }
                }, query);
            }, SEARCH_DELAY_MS);
        });
    }

    //@Override
    //public void setQuery(String query) {
    //    this.query = query;
    //}
}

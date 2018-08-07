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
    private final int interval = 250;
    private final Handler handler = new Handler();
    private Client.Request requestHandle = null;
    private boolean blockNextCall = false;
    private String query = "";

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

            boolean reject = query == null || query.isEmpty() || this.query.equals(query);
            if (!reject) {
                try {
                    new JSONObject(query);
                    reject = true;
                } catch (Exception ignore) {
                    // ignore
                }
            }
            if (blockNextCall || reject) {
                blockNextCall = false;
                callback.onState(REJECTED);
                log.v(TAG, "search | query = ", query, " | rejected");
                return;
            }

            this.query = query;
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
            }, interval);
        });
    }

    @Override
    public void setQuery(String query) {
        this.query = query;
    }

    @Override
    public void blockNextCall() {
        blockNextCall = true;
    }
}

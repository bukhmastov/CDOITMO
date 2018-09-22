package com.bukhmastov.cdoitmo.object.schedule.impl;

import android.content.Context;

import com.bukhmastov.cdoitmo.converter.schedule.lessons.ScheduleLessonsAdditionalConverter;
import com.bukhmastov.cdoitmo.converter.schedule.lessons.ScheduleLessonsConverterIfmo;
import com.bukhmastov.cdoitmo.event.bus.EventBus;
import com.bukhmastov.cdoitmo.event.bus.annotation.Event;
import com.bukhmastov.cdoitmo.event.events.ClearCacheEvent;
import com.bukhmastov.cdoitmo.factory.AppComponentProvider;
import com.bukhmastov.cdoitmo.network.IfmoRestClient;
import com.bukhmastov.cdoitmo.network.handlers.RestResponseHandler;
import com.bukhmastov.cdoitmo.network.model.Client;
import com.bukhmastov.cdoitmo.object.schedule.ScheduleLessons;
import com.bukhmastov.cdoitmo.util.Log;
import com.bukhmastov.cdoitmo.util.Thread;

import org.json.JSONArray;
import org.json.JSONObject;

import javax.inject.Inject;

public class ScheduleLessonsImpl extends ScheduleImpl implements ScheduleLessons {

    private static final String TAG = "ScheduleLessons";

    @Inject
    Log log;
    @Inject
    Thread thread;
    @Inject
    EventBus eventBus;
    @Inject
    IfmoRestClient ifmoRestClient;

    public ScheduleLessonsImpl() {
        AppComponentProvider.getComponent().inject(this);
        eventBus.register(this);
    }

    @Event
    public void onClearCacheEvent(ClearCacheEvent event) {
        if (event.isNot(ClearCacheEvent.SCHEDULE_LESSONS)) {
            return;
        }
        clearLocalCache();
    }

    @Override
    protected void searchMine(final Context context, final int refreshRate, final boolean forceToCache, final boolean withUserChanges) {
        log.v(TAG, "searchMine | personal schedule is unavailable");
        invokePending("mine", withUserChanges, true, handler -> handler.onFailure(FAILED_INVALID_QUERY));
    }
    @Override
    protected void searchGroup(final Context context, final String group, final int refreshRate, final boolean forceToCache, final boolean withUserChanges) {
        final @Source String source = getSource(context);
        log.v(TAG, "searchGroup | group=", group, " | refreshRate=", refreshRate, " | forceToCache=", forceToCache, " | withUserChanges=", withUserChanges, " | source=" + source);
        thread.run(() -> searchByQuery(context, "group", group, refreshRate, withUserChanges, new SearchByQuery() {
            @Override
            public boolean isWebAvailable() {
                return true;
            }
            @Override
            public void onWebRequest(final String query, final String cache, final RestResponseHandler restResponseHandler) {
                switch (source) {
                    case SOURCE.ISU: invokePending(query, withUserChanges, true, handler -> handler.onFailure(FAILED_INVALID_QUERY)); break;
                    case SOURCE.IFMO: ifmoRestClient.get(context, "schedule_lesson_group/" + query, null, restResponseHandler); break;
                }
            }
            @Override
            public void onWebRequestSuccess(final String query, final JSONObject data, final JSONObject template) {
                switch (source) {
                    case SOURCE.ISU: break;
                    case SOURCE.IFMO: ScheduleLessonsImpl.this.onWebRequestSuccessIfmo(this, query, data, template); break;
                }
            }
            @Override
            public void onWebRequestFailed(final int statusCode, final Client.Headers headers, final int state) {
                invokePending(group, withUserChanges, true, handler -> handler.onFailure(statusCode, headers, state));
            }
            @Override
            public void onWebRequestProgress(final int state) {
                invokePending(group, withUserChanges, false, handler -> handler.onProgress(state));
            }
            @Override
            public void onWebNewRequest(final Client.Request request) {
                invokePending(group, withUserChanges, false, handler -> handler.onNewRequest(request));
            }
            @Override
            public void onFound(final String query, final JSONObject data, final boolean putToCache, boolean fromCache) {
                ScheduleLessonsImpl.this.onFound(context, query, data, putToCache, forceToCache, fromCache, withUserChanges);
            }
        }));
    }
    @Override
    protected void searchRoom(final Context context, final String room, final int refreshRate, final boolean forceToCache, final boolean withUserChanges) {
        log.v(TAG, "searchRoom | room=", room, " | refreshRate=", refreshRate, " | forceToCache=", forceToCache, " | withUserChanges=", withUserChanges);
        thread.run(() -> searchByQuery(context, "room", room, refreshRate, withUserChanges, new SearchByQuery() {
            @Override
            public boolean isWebAvailable() {
                return true;
            }
            @Override
            public void onWebRequest(final String query, final String cache, final RestResponseHandler restResponseHandler) {
                ifmoRestClient.get(context, "schedule_lesson_room/" + query, null, restResponseHandler);
            }
            @Override
            public void onWebRequestSuccess(final String query, final JSONObject data, final JSONObject template) {
                ScheduleLessonsImpl.this.onWebRequestSuccessIfmo(this, query, data, template);
            }
            @Override
            public void onWebRequestFailed(final int statusCode, final Client.Headers headers, final int state) {
                invokePending(room, withUserChanges, true, handler -> handler.onFailure(statusCode, headers, state));
            }
            @Override
            public void onWebRequestProgress(final int state) {
                invokePending(room, withUserChanges, false, handler -> handler.onProgress(state));
            }
            @Override
            public void onWebNewRequest(final Client.Request request) {
                invokePending(room, withUserChanges, false, handler -> handler.onNewRequest(request));
            }
            @Override
            public void onFound(final String query, final JSONObject data, final boolean putToCache, boolean fromCache) {
                ScheduleLessonsImpl.this.onFound(context, query, data, putToCache, forceToCache, fromCache, withUserChanges);
            }
        }));
    }
    @Override
    protected void searchTeacher(final Context context, final String teacherId, final int refreshRate, final boolean forceToCache, final boolean withUserChanges) {
        final @Source String source = getSource(context);
        log.v(TAG, "searchTeacher | teacherId=", teacherId, " | refreshRate=", refreshRate, " | forceToCache=", forceToCache, " | withUserChanges=", withUserChanges, " | source=", source);
        thread.run(() -> searchByQuery(context, "teacher", teacherId, refreshRate, withUserChanges, new SearchByQuery() {
            @Override
            public boolean isWebAvailable() {
                return true;
            }
            @Override
            public void onWebRequest(final String query, final String cache, final RestResponseHandler restResponseHandler) {
                switch (source) {
                    case SOURCE.ISU: invokePending(query, withUserChanges, true, handler -> handler.onFailure(FAILED_INVALID_QUERY)); break;
                    case SOURCE.IFMO: ifmoRestClient.get(context, "schedule_lesson_person/" + query, null, restResponseHandler); break;
                }
            }
            @Override
            public void onWebRequestSuccess(final String query, final JSONObject data, final JSONObject template) {
                switch (source) {
                    case SOURCE.ISU: break;
                    case SOURCE.IFMO: ScheduleLessonsImpl.this.onWebRequestSuccessIfmo(this, query, data, template); break;
                }
            }
            @Override
            public void onWebRequestFailed(final int statusCode, final Client.Headers headers, final int state) {
                invokePending(teacherId, withUserChanges, true, handler -> handler.onFailure(statusCode, headers, state));
            }
            @Override
            public void onWebRequestProgress(final int state) {
                invokePending(teacherId, withUserChanges, false, handler -> handler.onProgress(state));
            }
            @Override
            public void onWebNewRequest(final Client.Request request) {
                invokePending(teacherId, withUserChanges, false, handler -> handler.onNewRequest(request));
            }
            @Override
            public void onFound(final String query, final JSONObject data, final boolean putToCache, boolean fromCache) {
                ScheduleLessonsImpl.this.onFound(context, query, data, putToCache, forceToCache, fromCache, withUserChanges);
            }
        }));
    }
    @Override
    protected boolean searchTeachersAvailable() {
        return true;
    }
    @Override
    protected String getType() {
        return TYPE;
    }
    @Override
    protected @Source String getDefaultSource() {
        return SOURCE.IFMO;
    }

    private void onWebRequestSuccessIfmo(final SearchByQuery searchByQuery, final String query, final JSONObject data, final JSONObject template) {
        thread.run(new ScheduleLessonsConverterIfmo(data, template, json -> searchByQuery.onFound(query, json, true, false)));
    }
    private void onFound(final Context context, final String query, final JSONObject data, final boolean putToCache, final boolean forceToCache, final boolean fromCache, final boolean withUserChanges) {
        thread.run(() -> {
            try {
                if (context == null || query == null || data == null) {
                    log.w(TAG, "onFound | some values are null | context=", context, " | query=", query, " | data=", data);
                    if (query == null) {
                        return;
                    }
                    invokePending(query, withUserChanges, true, handler -> handler.onFailure(FAILED_LOAD));
                    return;
                }
                boolean valid = false;
                final JSONArray schedule = data.getJSONArray("schedule");
                for (int i = 0; i < schedule.length(); i++) {
                    final JSONObject day = schedule.getJSONObject(i);
                    if (day != null) {
                        final JSONArray lessons = day.getJSONArray("lessons");
                        if (lessons != null && lessons.length() > 0) {
                            valid = true;
                            break;
                        }
                    }
                }
                if (valid) {
                    if (putToCache) {
                        putCache(context, query, data.toString(), forceToCache);
                    }
                    if (withUserChanges) {
                        new ScheduleLessonsAdditionalConverter(context, data, json -> invokePending(query, true, true, handler -> handler.onSuccess(json, fromCache))).run();
                    } else {
                        invokePending(query, false, true, handler -> handler.onSuccess(data, fromCache));
                    }
                } else {
                    if (putToCache) {
                        putLocalCache(query, data.toString());
                    }
                    invokePending(query, withUserChanges, true, handler -> handler.onFailure(FAILED_NOT_FOUND));
                }
            } catch (Exception e) {
                log.exception(e);
                invokePending(query, withUserChanges, true, handler -> handler.onFailure(FAILED_LOAD));
            }
        });
    }

}

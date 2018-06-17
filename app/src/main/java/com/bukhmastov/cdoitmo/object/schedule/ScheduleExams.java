package com.bukhmastov.cdoitmo.object.schedule;

import android.content.Context;

import com.bukhmastov.cdoitmo.converter.schedule.exams.ScheduleExamsConverterIfmo;
import com.bukhmastov.cdoitmo.network.IfmoClient;
import com.bukhmastov.cdoitmo.network.interfaces.ResponseHandler;
import com.bukhmastov.cdoitmo.network.interfaces.RestResponseHandler;
import com.bukhmastov.cdoitmo.network.model.Client;
import com.bukhmastov.cdoitmo.parse.schedule.ScheduleExamsGroupParse;
import com.bukhmastov.cdoitmo.parse.schedule.ScheduleExamsTeacherParse;
import com.bukhmastov.cdoitmo.util.Log;
import com.bukhmastov.cdoitmo.util.Thread;

import org.json.JSONObject;

public class ScheduleExams extends Schedule {

    private static final String TAG = "ScheduleExams";
    public static final String TYPE = "exams";

    public ScheduleExams(Handler handler) {
        super(handler);
    }

    @Override
    protected void searchMine(final Context context, final int refreshRate, final boolean forceToCache, final boolean withUserChanges) {
        Log.v(TAG, "searchMine | personal schedule is unavailable");
        invokePending("mine", withUserChanges, true, handler -> handler.onFailure(FAILED_INVALID_QUERY));
    }
    @Override
    protected void searchGroup(final Context context, final String group, final int refreshRate, final boolean forceToCache, final boolean withUserChanges) {
        final @Source String source = getSource(context);
        Log.v(TAG, "searchGroup | group=", group, " | refreshRate=", refreshRate, " | forceToCache=", forceToCache, " | withUserChanges=", withUserChanges, " | source=" + source);
        Thread.run(() -> searchByQuery(context, "group", group, refreshRate, withUserChanges, new SearchByQuery() {
            @Override
            public boolean isWebAvailable() {
                return true;
            }
            @Override
            public void onWebRequest(final String query, final String cache, final RestResponseHandler restResponseHandler) {
                switch (source) {
                    case SOURCE.ISU: invokePending(query, withUserChanges, true, handler -> handler.onFailure(FAILED_INVALID_QUERY)); break;
                    case SOURCE.IFMO: IfmoClient.get(context, "ru/exam/0/" + group + "/raspisanie_sessii.htm", null, new ResponseHandler() {
                        @Override
                        public void onSuccess(final int statusCode, final Client.Headers headers, final String response) {
                            Thread.run(new ScheduleExamsGroupParse(response, query, json -> restResponseHandler.onSuccess(statusCode, headers, json, null)));
                        }
                        @Override
                        public void onFailure(int statusCode, Client.Headers headers, int state) {
                            restResponseHandler.onFailure(statusCode, headers, state);
                        }
                        @Override
                        public void onProgress(int state) {
                            restResponseHandler.onProgress(state);
                        }
                        @Override
                        public void onNewRequest(Client.Request request) {
                            restResponseHandler.onNewRequest(request);
                        }
                    }); break;
                }
            }
            @Override
            public void onWebRequestSuccess(final String query, final JSONObject data, final JSONObject template) {
                switch (source) {
                    case SOURCE.ISU: break;
                    case SOURCE.IFMO: ScheduleExams.this.onWebRequestSuccessIfmo(this, query, data, template); break;
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
                ScheduleExams.this.onFound(context, query, data, putToCache, forceToCache, fromCache, withUserChanges);
            }
        }));
    }
    @Override
    protected void searchRoom(final Context context, final String room, final int refreshRate, final boolean forceToCache, final boolean withUserChanges) {
        Log.v(TAG, "searchRoom | actually, rooms unavailable at schedule of exams");
        invokePending(room, withUserChanges, true, handler -> handler.onFailure(FAILED_INVALID_QUERY));
    }
    @Override
    protected void searchTeacher(final Context context, final String teacherId, final int refreshRate, final boolean forceToCache, final boolean withUserChanges) {
        final @Source String source = getSource(context);
        Log.v(TAG, "searchTeacher | teacherId=", teacherId, " | refreshRate=", refreshRate, " | forceToCache=", forceToCache, " | withUserChanges=", withUserChanges, " | source=" + source);
        Thread.run(() -> searchByQuery(context, "teacher", teacherId, refreshRate, withUserChanges, new SearchByQuery() {
            @Override
            public boolean isWebAvailable() {
                return true;
            }
            @Override
            public void onWebRequest(final String query, final String cache, final RestResponseHandler restResponseHandler) {
                switch (source) {
                    case SOURCE.ISU: invokePending(query, withUserChanges, true, handler -> handler.onFailure(FAILED_INVALID_QUERY)); break;
                    case SOURCE.IFMO: IfmoClient.get(context, "ru/exam/3/" + query + "/raspisanie_sessii.htm", null, new ResponseHandler() {
                        @Override
                        public void onSuccess(final int statusCode, final Client.Headers headers, final String response) {
                            Thread.run(new ScheduleExamsTeacherParse(response, query, json -> restResponseHandler.onSuccess(statusCode, headers, json, null)));
                        }
                        @Override
                        public void onFailure(int statusCode, Client.Headers headers, int state) {
                            restResponseHandler.onFailure(statusCode, headers, state);
                        }
                        @Override
                        public void onProgress(int state) {
                            restResponseHandler.onProgress(state);
                        }
                        @Override
                        public void onNewRequest(Client.Request request) {
                            restResponseHandler.onNewRequest(request);
                        }
                    }); break;
                }
            }
            @Override
            public void onWebRequestSuccess(final String query, final JSONObject data, final JSONObject template) {
                switch (source) {
                    case SOURCE.ISU: break;
                    case SOURCE.IFMO: ScheduleExams.this.onWebRequestSuccessIfmo(this, query, data, template); break;
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
                ScheduleExams.this.onFound(context, query, data, putToCache, forceToCache, fromCache, withUserChanges);
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
        Thread.run(new ScheduleExamsConverterIfmo(data, template, json -> searchByQuery.onFound(query, json, true, false)));
    }
    private void onFound(final Context context, final String query, final JSONObject data, final boolean putToCache, final boolean forceToCache, final boolean fromCache, final boolean withUserChanges) {
        Thread.run(() -> {
            try {
                if (context == null || query == null || data == null) {
                    Log.w(TAG, "onFound | some values are null | context=", context, " | query=", query, " | data=", data);
                    if (query == null) {
                        return;
                    }
                    invokePending(query, withUserChanges, true, handler -> handler.onFailure(FAILED_LOAD));
                    return;
                }
                if (data.getJSONArray("schedule").length() == 0) {
                    invokePending(query, withUserChanges, true, handler -> handler.onFailure(FAILED_NOT_FOUND));
                } else {
                    if (putToCache) {
                        putCache(context, query, data.toString(), forceToCache);
                    }
                    invokePending(query, withUserChanges, true, handler -> handler.onSuccess(data, fromCache));
                }
            } catch (Exception e) {
                Log.exception(e);
                invokePending(query, withUserChanges, true, handler -> handler.onFailure(FAILED_LOAD));
            }
        });
    }
}

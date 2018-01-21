package com.bukhmastov.cdoitmo.objects.schedule;

import android.content.Context;

import com.bukhmastov.cdoitmo.converters.schedule.exams.ScheduleExamsConverterIfmo;
import com.bukhmastov.cdoitmo.network.IfmoClient;
import com.bukhmastov.cdoitmo.network.interfaces.ResponseHandler;
import com.bukhmastov.cdoitmo.network.interfaces.RestResponseHandler;
import com.bukhmastov.cdoitmo.network.models.Client;
import com.bukhmastov.cdoitmo.parse.ScheduleExamsGroupParse;
import com.bukhmastov.cdoitmo.parse.ScheduleExamsTeacherParse;
import com.bukhmastov.cdoitmo.utils.Log;
import com.bukhmastov.cdoitmo.utils.Static;

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
        final SOURCE source = getSource(context);
        Log.v(TAG, "searchGroup | group=" + group + " | refreshRate=" + refreshRate + " | forceToCache=" + Log.lBool(forceToCache) + " | withUserChanges=" + Log.lBool(withUserChanges) + " | source=" + source2string(source));
        Static.T.runThread(() -> searchByQuery(context, "group", group, refreshRate, withUserChanges, new SearchByQuery() {
            @Override
            public boolean isWebAvailable() {
                return true;
            }
            @Override
            public void onWebRequest(final String query, final String cache, final RestResponseHandler restResponseHandler) {
                switch (source) {
                    case ISU:
                        invokePending(query, withUserChanges, true, handler -> handler.onFailure(FAILED_INVALID_QUERY));
                        break;
                    case IFMO: IfmoClient.get(context, "ru/exam/0/" + group + "/raspisanie_sessii.htm", null, new ResponseHandler() {
                        @Override
                        public void onSuccess(final int statusCode, final Client.Headers headers, final String response) {
                            Static.T.runThread(new ScheduleExamsGroupParse(response, query, json -> restResponseHandler.onSuccess(statusCode, headers, json, null)));
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
                    case ISU:  break;
                    case IFMO: ScheduleExams.this.onWebRequestSuccessIfmo(this, query, data, template); break;
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
        final SOURCE source = getSource(context);
        Log.v(TAG, "searchTeacher | teacherId=" + teacherId + " | refreshRate=" + refreshRate + " | forceToCache=" + Log.lBool(forceToCache) + " | withUserChanges=" + Log.lBool(withUserChanges) + " | source=" + source2string(source));
        Static.T.runThread(() -> searchByQuery(context, "teacher", teacherId, refreshRate, withUserChanges, new SearchByQuery() {
            @Override
            public boolean isWebAvailable() {
                return true;
            }
            @Override
            public void onWebRequest(final String query, final String cache, final RestResponseHandler restResponseHandler) {
                switch (source) {
                    case ISU:
                        invokePending(query, withUserChanges, true, handler -> handler.onFailure(FAILED_INVALID_QUERY));
                        break;
                    case IFMO: IfmoClient.get(context, "ru/exam/3/" + query + "/raspisanie_sessii.htm", null, new ResponseHandler() {
                        @Override
                        public void onSuccess(final int statusCode, final Client.Headers headers, final String response) {
                            Static.T.runThread(new ScheduleExamsTeacherParse(response, query, json -> restResponseHandler.onSuccess(statusCode, headers, json, null)));
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
                    case ISU:  break;
                    case IFMO: ScheduleExams.this.onWebRequestSuccessIfmo(this, query, data, template); break;
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
    protected String getType() {
        return TYPE;
    }
    @Override
    protected SOURCE getDefaultSource() {
        return SOURCE.IFMO;
    }

    private void onWebRequestSuccessIfmo(final SearchByQuery searchByQuery, final String query, final JSONObject data, final JSONObject template) {
        Static.T.runThread(new ScheduleExamsConverterIfmo(data, template, json -> searchByQuery.onFound(query, json, true, false)));
    }
    private void onFound(final Context context, final String query, final JSONObject data, final boolean putToCache, final boolean forceToCache, final boolean fromCache, final boolean withUserChanges) {
        Static.T.runThread(() -> {
            try {
                if (context == null || query == null || data == null) {
                    Log.w(TAG, "onFound | some values are null | context=" + Log.lNull(context) + " | query=" + Log.lNull(query) + " | data=" + Log.lNull(data));
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
                Static.error(e);
                invokePending(query, withUserChanges, true, handler -> handler.onFailure(FAILED_LOAD));
            }
        });
    }
}

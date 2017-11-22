package com.bukhmastov.cdoitmo.objects.schedule;

import android.content.Context;

import com.bukhmastov.cdoitmo.converters.schedule.ScheduleExamsConverterIsu;
import com.bukhmastov.cdoitmo.network.IsuRestClient;
import com.bukhmastov.cdoitmo.network.interfaces.RestResponseHandler;
import com.bukhmastov.cdoitmo.network.models.Client;
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
        Static.T.runThread(new Runnable() {
            @Override
            public void run() {
                Log.v(TAG, "searchMine | refreshRate=" + refreshRate + " | forceToCache=" + (forceToCache ? "true" : "false") + " | withUserChanges=" + (withUserChanges ? "true" : "false"));
                searchByQuery(context, "mine", "mine", refreshRate, new SearchByQuery() {
                    @Override
                    public boolean isWebAvailable() {
                        if (!IsuRestClient.isAuthorized(context)) {
                            Log.v(TAG, "searchMine | isu auth required");
                            invokePending("mine", true, new Pending() {
                                @Override
                                public void invoke(Handler handler) {
                                    handler.onFailure(FAILED_MINE_NEED_ISU);
                                }
                            });
                            return false;
                        } else {
                            return true;
                        }
                    }
                    @Override
                    public void onWebRequest(final String query, final String cache, final RestResponseHandler restResponseHandler) {
                        IsuRestClient.Private.get(context, "exams/personal/student/%key%/%token%", null, restResponseHandler);
                    }
                    @Override
                    public void onWebRequestSuccess(final String query, final JSONObject data, final JSONObject template) {
                        ScheduleExams.this.onWebRequestSuccessIsu(this, query, data, template);
                    }
                    @Override
                    public void onWebRequestFailed(final int statusCode, final Client.Headers headers, final int state) {
                        invokePending("mine", true, new Pending() {
                            @Override
                            public void invoke(Handler handler) {
                                handler.onFailure(statusCode, headers, state);
                            }
                        });
                    }
                    @Override
                    public void onWebRequestProgress(final int state) {
                        invokePending("mine", false, new Pending() {
                            @Override
                            public void invoke(Handler handler) {
                                handler.onProgress(state);
                            }
                        });
                    }
                    @Override
                    public void onWebNewRequest(final Client.Request request) {
                        invokePending("mine", false, new Pending() {
                            @Override
                            public void invoke(Handler handler) {
                                handler.onNewRequest(request);
                            }
                        });
                    }
                    @Override
                    public void onFound(final String query, final JSONObject data, final boolean putToCache, boolean fromCache) {
                        ScheduleExams.this.onFound(context, query, data, putToCache, forceToCache, fromCache, withUserChanges);
                    }
                });
            }
        });
    }
    @Override
    protected void searchGroup(final Context context, final String group, final int refreshRate, final boolean forceToCache, final boolean withUserChanges) {
        Static.T.runThread(new Runnable() {
            @Override
            public void run() {
                Log.v(TAG, "searchGroup | group=" + group + " | refreshRate=" + refreshRate + " | forceToCache=" + (forceToCache ? "true" : "false") + " | withUserChanges=" + (withUserChanges ? "true" : "false"));
                searchByQuery(context, "group", group, refreshRate, new SearchByQuery() {
                    @Override
                    public boolean isWebAvailable() {
                        return true;
                    }
                    @Override
                    public void onWebRequest(final String query, final String cache, final RestResponseHandler restResponseHandler) {
                        IsuRestClient.Public.get(context, "exams/common/group/%key%/" + query, null, restResponseHandler);
                    }
                    @Override
                    public void onWebRequestSuccess(final String query, final JSONObject data, final JSONObject template) {
                        ScheduleExams.this.onWebRequestSuccessIsu(this, query, data, template);
                    }
                    @Override
                    public void onWebRequestFailed(final int statusCode, final Client.Headers headers, final int state) {
                        invokePending(group, true, new Pending() {
                            @Override
                            public void invoke(Handler handler) {
                                handler.onFailure(statusCode, headers, state);
                            }
                        });
                    }
                    @Override
                    public void onWebRequestProgress(final int state) {
                        invokePending(group, false, new Pending() {
                            @Override
                            public void invoke(Handler handler) {
                                handler.onProgress(state);
                            }
                        });
                    }
                    @Override
                    public void onWebNewRequest(final Client.Request request) {
                        invokePending(group, false, new Pending() {
                            @Override
                            public void invoke(Handler handler) {
                                handler.onNewRequest(request);
                            }
                        });
                    }
                    @Override
                    public void onFound(final String query, final JSONObject data, final boolean putToCache, boolean fromCache) {
                        ScheduleExams.this.onFound(context, query, data, putToCache, forceToCache, fromCache, withUserChanges);
                    }
                });
            }
        });
    }
    @Override
    protected void searchRoom(final Context context, final String room, final int refreshRate, final boolean forceToCache, final boolean withUserChanges) {
        Log.wtf(TAG, "searchRoom | actually, rooms unavailable at schedule of exams");
        invokePending(room, true, new Pending() {
            @Override
            public void invoke(Handler handler) {
                handler.onFailure(FAILED_INVALID_QUERY);
            }
        });
    }
    @Override
    protected void searchTeacher(final Context context, final String teacherId, final int refreshRate, final boolean forceToCache, final boolean withUserChanges) {
        Static.T.runThread(new Runnable() {
            @Override
            public void run() {
                Log.v(TAG, "searchTeacher | teacherId=" + teacherId + " | refreshRate=" + refreshRate + " | forceToCache=" + (forceToCache ? "true" : "false") + " | withUserChanges=" + (withUserChanges ? "true" : "false"));
                searchByQuery(context, "teacher", teacherId, refreshRate, new SearchByQuery() {
                    @Override
                    public boolean isWebAvailable() {
                        return true;
                    }
                    @Override
                    public void onWebRequest(final String query, final String cache, final RestResponseHandler restResponseHandler) {
                        IsuRestClient.Public.get(context, "exams/common/teacher/%key%/" + query, null, restResponseHandler);
                    }
                    @Override
                    public void onWebRequestSuccess(final String query, final JSONObject data, final JSONObject template) {
                        ScheduleExams.this.onWebRequestSuccessIsu(this, query, data, template);
                    }
                    @Override
                    public void onWebRequestFailed(final int statusCode, final Client.Headers headers, final int state) {
                        invokePending(teacherId, true, new Pending() {
                            @Override
                            public void invoke(Handler handler) {
                                handler.onFailure(statusCode, headers, state);
                            }
                        });
                    }
                    @Override
                    public void onWebRequestProgress(final int state) {
                        invokePending(teacherId, false, new Pending() {
                            @Override
                            public void invoke(Handler handler) {
                                handler.onProgress(state);
                            }
                        });
                    }
                    @Override
                    public void onWebNewRequest(final Client.Request request) {
                        invokePending(teacherId, false, new Pending() {
                            @Override
                            public void invoke(Handler handler) {
                                handler.onNewRequest(request);
                            }
                        });
                    }
                    @Override
                    public void onFound(final String query, final JSONObject data, final boolean putToCache, boolean fromCache) {
                        ScheduleExams.this.onFound(context, query, data, putToCache, forceToCache, fromCache, withUserChanges);
                    }
                });
            }
        });
    }
    @Override
    protected String getType() {
        return TYPE;
    }

    private void onWebRequestSuccessIsu(final SearchByQuery searchByQuery, final String query, final JSONObject data, final JSONObject template) {
        Static.T.runThread(new ScheduleExamsConverterIsu(data, template, new ScheduleExamsConverterIsu.response() {
            @Override
            public void finish(final JSONObject json) {
                searchByQuery.onFound(query, json, true, false);
            }
        }));
    }
    private void onFound(final Context context, final String query, final JSONObject data, final boolean putToCache, final boolean forceToCache, final boolean fromCache, final boolean withUserChanges) {
        Static.T.runThread(new Runnable() {
            @Override
            public void run() {
                try {
                    if (data.getJSONArray("schedule").length() == 0) {
                        invokePending(query, true, new Pending() {
                            @Override
                            public void invoke(Handler handler) {
                                handler.onFailure(FAILED_LOAD);
                            }
                        });
                    } else {
                        if (putToCache) {
                            putCache(context, query, data.toString(), forceToCache);
                        }
                        invokePending(query, true, new Pending() {
                            @Override
                            public void invoke(Handler handler) {
                                handler.onSuccess(data, fromCache);
                            }
                        });
                    }
                } catch (Exception e) {
                    Static.error(e);
                    invokePending(query, true, new Pending() {
                        @Override
                        public void invoke(Handler handler) {
                            handler.onFailure(FAILED_LOAD);
                        }
                    });
                }
            }
        });
    }
}

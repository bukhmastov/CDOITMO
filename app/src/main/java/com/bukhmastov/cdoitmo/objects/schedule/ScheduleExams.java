package com.bukhmastov.cdoitmo.objects.schedule;

import android.content.Context;

import com.bukhmastov.cdoitmo.converters.schedule.exams.ScheduleExamsConverterIfmo;
import com.bukhmastov.cdoitmo.converters.schedule.exams.ScheduleExamsConverterIsu;
import com.bukhmastov.cdoitmo.network.IfmoClient;
import com.bukhmastov.cdoitmo.network.IsuRestClient;
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
        Static.T.runThread(new Runnable() {
            @Override
            public void run() {
                Log.v(TAG, "searchMine | refreshRate=" + refreshRate + " | forceToCache=" + Static.logBoolean(forceToCache) + " | withUserChanges=" + Static.logBoolean(withUserChanges));
                searchByQuery(context, "mine", "mine", refreshRate, withUserChanges, new SearchByQuery() {
                    @Override
                    public boolean isWebAvailable() {
                        if (!IsuRestClient.isAuthorized(context)) {
                            Log.v(TAG, "searchMine | isu auth required");
                            invokePending("mine", withUserChanges, true, new Pending() {
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
                        invokePending("mine", withUserChanges, true, new Pending() {
                            @Override
                            public void invoke(Handler handler) {
                                handler.onFailure(statusCode, headers, state);
                            }
                        });
                    }
                    @Override
                    public void onWebRequestProgress(final int state) {
                        invokePending("mine", withUserChanges, false, new Pending() {
                            @Override
                            public void invoke(Handler handler) {
                                handler.onProgress(state);
                            }
                        });
                    }
                    @Override
                    public void onWebNewRequest(final Client.Request request) {
                        invokePending("mine", withUserChanges, false, new Pending() {
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
                final SOURCE source = getSource(context);
                Log.v(TAG, "searchGroup | group=" + group + " | refreshRate=" + refreshRate + " | forceToCache=" + Static.logBoolean(forceToCache) + " | withUserChanges=" + Static.logBoolean(withUserChanges) + " | source=" + source2string(source));
                searchByQuery(context, "group", group, refreshRate, withUserChanges, new SearchByQuery() {
                    @Override
                    public boolean isWebAvailable() {
                        return true;
                    }
                    @Override
                    public void onWebRequest(final String query, final String cache, final RestResponseHandler restResponseHandler) {
                        switch (source) {
                            case ISU:  IsuRestClient.Public.get(context, "exams/common/group/%key%/" + query, null, restResponseHandler); break;
                            case IFMO: IfmoClient.get(context, "ru/exam/0/" + group + "/raspisanie_sessii.htm", null, new ResponseHandler() {
                                @Override
                                public void onSuccess(final int statusCode, final Client.Headers headers, final String response) {
                                    Static.T.runThread(new ScheduleExamsGroupParse(response, query, new ScheduleExamsGroupParse.response() {
                                        @Override
                                        public void finish(JSONObject json) {
                                            restResponseHandler.onSuccess(statusCode, headers, json, null);
                                        }
                                    }));
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
                            case ISU:  ScheduleExams.this.onWebRequestSuccessIsu(this, query, data, template); break;
                            case IFMO: ScheduleExams.this.onWebRequestSuccessIfmo(this, query, data, template); break;
                        }
                    }
                    @Override
                    public void onWebRequestFailed(final int statusCode, final Client.Headers headers, final int state) {
                        invokePending(group, withUserChanges, true, new Pending() {
                            @Override
                            public void invoke(Handler handler) {
                                handler.onFailure(statusCode, headers, state);
                            }
                        });
                    }
                    @Override
                    public void onWebRequestProgress(final int state) {
                        invokePending(group, withUserChanges, false, new Pending() {
                            @Override
                            public void invoke(Handler handler) {
                                handler.onProgress(state);
                            }
                        });
                    }
                    @Override
                    public void onWebNewRequest(final Client.Request request) {
                        invokePending(group, withUserChanges, false, new Pending() {
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
        Log.v(TAG, "searchRoom | actually, rooms unavailable at schedule of exams");
        invokePending(room, withUserChanges, true, new Pending() {
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
                final SOURCE source = getSource(context);
                Log.v(TAG, "searchTeacher | teacherId=" + teacherId + " | refreshRate=" + refreshRate + " | forceToCache=" + Static.logBoolean(forceToCache) + " | withUserChanges=" + Static.logBoolean(withUserChanges) + " | source=" + source2string(source));
                searchByQuery(context, "teacher", teacherId, refreshRate, withUserChanges, new SearchByQuery() {
                    @Override
                    public boolean isWebAvailable() {
                        return true;
                    }
                    @Override
                    public void onWebRequest(final String query, final String cache, final RestResponseHandler restResponseHandler) {
                        switch (source) {
                            case ISU:  IsuRestClient.Public.get(context, "exams/common/teacher/%key%/" + query, null, restResponseHandler); break;
                            case IFMO: IfmoClient.get(context, "ru/exam/3/" + query + "/raspisanie_sessii.htm", null, new ResponseHandler() {
                                @Override
                                public void onSuccess(final int statusCode, final Client.Headers headers, final String response) {
                                    Static.T.runThread(new ScheduleExamsTeacherParse(response, query, new ScheduleExamsTeacherParse.response() {
                                        @Override
                                        public void finish(JSONObject json) {
                                            restResponseHandler.onSuccess(statusCode, headers, json, null);
                                        }
                                    }));
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
                            case ISU:  ScheduleExams.this.onWebRequestSuccessIsu(this, query, data, template); break;
                            case IFMO: ScheduleExams.this.onWebRequestSuccessIfmo(this, query, data, template); break;
                        }
                    }
                    @Override
                    public void onWebRequestFailed(final int statusCode, final Client.Headers headers, final int state) {
                        invokePending(teacherId, withUserChanges, true, new Pending() {
                            @Override
                            public void invoke(Handler handler) {
                                handler.onFailure(statusCode, headers, state);
                            }
                        });
                    }
                    @Override
                    public void onWebRequestProgress(final int state) {
                        invokePending(teacherId, withUserChanges, false, new Pending() {
                            @Override
                            public void invoke(Handler handler) {
                                handler.onProgress(state);
                            }
                        });
                    }
                    @Override
                    public void onWebNewRequest(final Client.Request request) {
                        invokePending(teacherId, withUserChanges, false, new Pending() {
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
    @Override
    protected SOURCE getDefaultSource() {
        return SOURCE.IFMO;
    }

    private void onWebRequestSuccessIsu(final SearchByQuery searchByQuery, final String query, final JSONObject data, final JSONObject template) {
        Static.T.runThread(new ScheduleExamsConverterIsu(data, template, new ScheduleExamsConverterIsu.response() {
            @Override
            public void finish(final JSONObject json) {
                searchByQuery.onFound(query, json, true, false);
            }
        }));
    }
    private void onWebRequestSuccessIfmo(final SearchByQuery searchByQuery, final String query, final JSONObject data, final JSONObject template) {
        Static.T.runThread(new ScheduleExamsConverterIfmo(data, template, new ScheduleExamsConverterIfmo.response() {
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
                        invokePending(query, withUserChanges, true, new Pending() {
                            @Override
                            public void invoke(Handler handler) {
                                handler.onFailure(FAILED_LOAD);
                            }
                        });
                    } else {
                        if (putToCache) {
                            putCache(context, query, data.toString(), forceToCache);
                        }
                        invokePending(query, withUserChanges, true, new Pending() {
                            @Override
                            public void invoke(Handler handler) {
                                handler.onSuccess(data, fromCache);
                            }
                        });
                    }
                } catch (Exception e) {
                    Static.error(e);
                    invokePending(query, withUserChanges, true, new Pending() {
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

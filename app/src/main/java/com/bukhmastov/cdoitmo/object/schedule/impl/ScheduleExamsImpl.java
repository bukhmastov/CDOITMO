package com.bukhmastov.cdoitmo.object.schedule.impl;

import com.bukhmastov.cdoitmo.event.bus.annotation.Event;
import com.bukhmastov.cdoitmo.event.events.ClearCacheEvent;
import com.bukhmastov.cdoitmo.firebase.FirebasePerformanceProvider;
import com.bukhmastov.cdoitmo.model.parser.ScheduleExamsGroupParser;
import com.bukhmastov.cdoitmo.model.parser.ScheduleExamsTeacherParser;
import com.bukhmastov.cdoitmo.model.schedule.exams.SExams;
import com.bukhmastov.cdoitmo.model.schedule.teachers.STeachers;
import com.bukhmastov.cdoitmo.network.handlers.ResponseHandler;
import com.bukhmastov.cdoitmo.network.handlers.RestResponseHandler;
import com.bukhmastov.cdoitmo.network.model.Client;
import com.bukhmastov.cdoitmo.object.schedule.ScheduleExams;
import com.bukhmastov.cdoitmo.util.singleton.CollectionUtils;

import org.json.JSONObject;

public class ScheduleExamsImpl extends ScheduleImpl<SExams> implements ScheduleExams {

    private static final String TAG = "ScheduleExams";

    public ScheduleExamsImpl() {
        super();
        eventBus.register(this);
    }

    @Event
    public void onClearCacheEvent(ClearCacheEvent event) {
        if (event.isNot(ClearCacheEvent.SCHEDULE_EXAMS)) {
            return;
        }
        clearLocalCache();
    }

    @Override
    protected void searchMine(int refreshRate, boolean forceToCache, boolean withUserChanges) {
        thread.run(() -> {
            log.v(TAG, "searchMine | personal schedule is unavailable");
            invokePendingAndClose("mine", withUserChanges, handler -> handler.onFailure(FAILED_INVALID_QUERY));
        });
    }

    @Override
    protected void searchGroup(String group, int refreshRate, boolean forceToCache, boolean withUserChanges) {
        thread.run(() -> {
            @Source String source = getSource();
            log.v(TAG, "searchGroup | group=", group, " | refreshRate=", refreshRate, " | forceToCache=", forceToCache, " | withUserChanges=", withUserChanges, " | source=", source);
            searchByQuery(group, source, refreshRate, withUserChanges, new SearchByQuery<SExams>() {
                @Override
                public void onWebRequest(String query, String source, RestResponseHandler restResponseHandler) {
                    switch (source) {
                        case SOURCE.ISU: invokePendingAndClose(query, withUserChanges, handler -> handler.onFailure(FAILED_INVALID_QUERY)); break;
                        case SOURCE.IFMO: {
                            ifmoClient.get(context, "ru/exam/0/" + group + "/raspisanie_sessii.htm", null, new ResponseHandler() {
                                @Override
                                public void onSuccess(final int statusCode, final Client.Headers headers, final String response) {
                                    thread.run(() -> {
                                        SExams schedule = new ScheduleExamsGroupParser(response, query).parse();
                                        if (schedule != null) {
                                            schedule.setQuery(query);
                                            schedule.setType("group");
                                            schedule.setTimestamp(time.getTimeInMillis());
                                            restResponseHandler.onSuccess(statusCode, headers, schedule.toJson(), null);
                                        } else {
                                            restResponseHandler.onFailure(statusCode, headers, FAILED_LOAD);
                                        }
                                    }, throwable -> {
                                        restResponseHandler.onFailure(statusCode, headers, FAILED_LOAD);
                                    });
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
                            });
                            break;
                        }
                    }
                }
                @Override
                public SExams onGetScheduleFromJson(String query, String source, JSONObject json) throws Exception {
                    return getNewInstance().fromJson(json);
                }
                @Override
                public void onFound(String query, SExams schedule, boolean fromCache) {
                    onScheduleFound(query, schedule, forceToCache, fromCache, withUserChanges);
                }
            });
        });
    }

    @Override
    protected void searchRoom(String room, int refreshRate, boolean forceToCache, boolean withUserChanges) {
        thread.run(() -> {
            log.v(TAG, "searchRoom | rooms schedule is unavailable");
            invokePendingAndClose(room, withUserChanges, handler -> handler.onFailure(FAILED_INVALID_QUERY));
        });
    }

    @Override
    protected void searchTeacher(String teacherId, int refreshRate, boolean forceToCache, boolean withUserChanges) {
        thread.run(() -> {
            @Source String source = getSource();
            log.v(TAG, "searchTeacher | teacherId=", teacherId, " | refreshRate=", refreshRate, " | forceToCache=", forceToCache, " | withUserChanges=", withUserChanges, " | source=", source);
            searchByQuery(teacherId, source, refreshRate, withUserChanges, new SearchByQuery<SExams>() {
                @Override
                public void onWebRequest(String query, String source, RestResponseHandler restResponseHandler) {
                    switch (source) {
                        case SOURCE.ISU: invokePendingAndClose(query, withUserChanges, handler -> handler.onFailure(FAILED_INVALID_QUERY)); break;
                        case SOURCE.IFMO: {
                            ifmoClient.get(context, "ru/exam/3/" + query + "/raspisanie_sessii.htm", null, new ResponseHandler() {
                                @Override
                                public void onSuccess(final int statusCode, final Client.Headers headers, final String response) {
                                    thread.run(() -> {
                                        SExams schedule = new ScheduleExamsTeacherParser(response, query).parse();
                                        if (schedule != null) {
                                            schedule.setQuery(query);
                                            schedule.setType("teacher");
                                            schedule.setTimestamp(time.getTimeInMillis());
                                            restResponseHandler.onSuccess(statusCode, headers, schedule.toJson(), null);
                                        } else {
                                            restResponseHandler.onFailure(statusCode, headers, FAILED_LOAD);
                                        }
                                    }, throwable -> {
                                        restResponseHandler.onFailure(statusCode, headers, FAILED_LOAD);
                                    });
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
                            });
                            break;
                        }
                    }
                }
                @Override
                public SExams onGetScheduleFromJson(String query, String source, JSONObject json) throws Exception {
                    return getNewInstance().fromJson(json);
                }
                @Override
                public void onFound(String query, SExams schedule, boolean fromCache) {
                    onScheduleFound(query, schedule, forceToCache, fromCache, withUserChanges);
                }
            });
        });
    }

    @Override
    protected void searchTeachers(String lastname, boolean withUserChanges) {
        thread.run(() -> {
            @Source String source = getSource();
            log.v(TAG, "searchTeachers | lastname=", lastname);
            searchByQuery(lastname, source, 0, withUserChanges, new SearchByQuery<SExams>() {
                @Override
                public void onWebRequest(String query, String source, RestResponseHandler restResponseHandler) {
                    switch (source) {
                        case SOURCE.ISU: invokePendingAndClose(query, withUserChanges, handler -> handler.onFailure(FAILED_INVALID_QUERY)); break;
                        case SOURCE.IFMO: ifmoRestClient.get(context, "schedule_person?lastname=" + lastname, null, restResponseHandler); break;
                    }
                }
                @Override
                public SExams onGetScheduleFromJson(String query, String source, JSONObject json) throws Exception {
                    STeachers teachers = new STeachers().fromJson(json);
                    if (teachers == null) {
                        return null;
                    }
                    SExams schedule = new SExams();
                    schedule.setQuery(query);
                    schedule.setType("teachers");
                    schedule.setTimestamp(time.getTimeInMillis());
                    schedule.setTeachers(teachers);
                    return schedule;
                }
                @Override
                public void onFound(String query, SExams schedule, boolean fromCache) {
                    onScheduleFound(query, schedule, false, false, withUserChanges);
                }
            });
        });
    }

    @Override
    protected String getType() {
        return TYPE;
    }

    @Override
    protected String getDefaultSource() {
        return SOURCE.IFMO;
    }

    @Override
    protected SExams getNewInstance() {
        return new SExams();
    }

    @Override
    protected String getTraceName() {
        return FirebasePerformanceProvider.Trace.Schedule.EXAMS;
    }

    private void onScheduleFound(String query, SExams schedule, boolean forceToCache, boolean fromCache, boolean withUserChanges) {
        thread.run(() -> {
            if (context == null || query == null || schedule == null) {
                log.w(TAG, "onFound | some values are null | context=", context, " | query=", query, " | data=", schedule);
                if (query == null) {
                    return;
                }
                invokePendingAndClose(query, withUserChanges, handler -> handler.onFailure(FAILED_LOAD));
                return;
            }
            if ("teachers".equals(schedule.getType()) && schedule.getTeachers() != null) {
                if (CollectionUtils.isNotEmpty(schedule.getTeachers().getTeachers())) {
                    invokePendingAndClose(query, withUserChanges, handler -> handler.onSuccess(schedule, fromCache));
                } else {
                    invokePendingAndClose(query, withUserChanges, handler -> handler.onFailure(FAILED_NOT_FOUND));
                }
                return;
            }
            if (CollectionUtils.isNotEmpty(schedule.getSchedule())) {
                if (!fromCache) {
                    putToCache(query, schedule, forceToCache);
                }
                invokePendingAndClose(query, withUserChanges, handler -> handler.onSuccess(schedule, fromCache));
                return;
            }
            invokePendingAndClose(query, withUserChanges, handler -> handler.onFailure(FAILED_NOT_FOUND));
        }, throwable -> {
            log.exception(throwable);
            invokePendingAndClose(query, withUserChanges, handler -> handler.onFailure(FAILED_LOAD));
        });
    }
}

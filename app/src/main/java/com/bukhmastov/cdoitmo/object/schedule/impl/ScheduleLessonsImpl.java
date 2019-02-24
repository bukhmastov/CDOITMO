package com.bukhmastov.cdoitmo.object.schedule.impl;

import com.bukhmastov.cdoitmo.R;
import com.bukhmastov.cdoitmo.event.bus.annotation.Event;
import com.bukhmastov.cdoitmo.event.events.ClearCacheEvent;
import com.bukhmastov.cdoitmo.firebase.FirebasePerformanceProvider;
import com.bukhmastov.cdoitmo.model.converter.ScheduleLessonsAdditionalConverter;
import com.bukhmastov.cdoitmo.model.converter.ScheduleLessonsIsuConverter;
import com.bukhmastov.cdoitmo.model.converter.ScheduleLessonsItmoConverter;
import com.bukhmastov.cdoitmo.model.schedule.lessons.SLessons;
import com.bukhmastov.cdoitmo.model.schedule.remote.isu.ISUScheduleApiResponse;
import com.bukhmastov.cdoitmo.model.schedule.remote.itmo.ITMOSLessons;
import com.bukhmastov.cdoitmo.model.schedule.teachers.STeachers;
import com.bukhmastov.cdoitmo.network.handlers.RestResponseHandler;
import com.bukhmastov.cdoitmo.network.handlers.joiner.RestRestResponseHandlerJoiner;
import com.bukhmastov.cdoitmo.network.model.Client;
import com.bukhmastov.cdoitmo.object.schedule.ScheduleLessons;
import com.bukhmastov.cdoitmo.util.singleton.CollectionUtils;
import com.bukhmastov.cdoitmo.util.singleton.StringUtils;

public class ScheduleLessonsImpl extends ScheduleImpl<SLessons> implements ScheduleLessons {

    private static final String TAG = "ScheduleLessons";

    public ScheduleLessonsImpl() {
        super();
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
    protected void searchPersonal(int refreshRate, boolean forceToCache, boolean withUserChanges) throws Exception {
        @Source String source = SOURCE.ISU/*getSource()*/;
        log.v(TAG, "searchPersonal | refreshRate=", refreshRate, " | forceToCache=", forceToCache,
                " | withUserChanges=", withUserChanges, " | source=", source);
        searchByQuery("personal", source, refreshRate, withUserChanges, new SearchByQuery<SLessons>() {
            @Override
            public void onWebRequest(String query, String source, RestResponseHandler<SLessons> handler) {
                switch (source) {
                    case SOURCE.IFMO: // not available, using isu source
                    case SOURCE.ISU: {
                        String url = "schedule/personal/student/%apikey%/%isutoken%";
                        isuPrivateRestClient.get().get(context, url, null, new RestRestResponseHandlerJoiner<SLessons, ISUScheduleApiResponse>(handler) {
                            @Override
                            public void onSuccess(int code, Client.Headers headers, ISUScheduleApiResponse response) throws Exception {
                                handler.onSuccess(code, headers, convertIsuSchedule("personal", query, response));
                            }
                            @Override
                            public ISUScheduleApiResponse newInstance() {
                                return new ISUScheduleApiResponse();
                            }
                        });
                        break;
                    }
                }
            }
            @Override
            public void onFound(String query, SLessons schedule, boolean fromCache) {
                onScheduleFound(query, schedule, forceToCache, fromCache, withUserChanges);
            }
        });
    }

    @Override
    protected void searchGroup(String group, int refreshRate, boolean forceToCache, boolean withUserChanges) throws Exception {
        @Source String source = getSource();
        log.v(TAG, "searchGroup | group=", group, " | refreshRate=", refreshRate,
                " | forceToCache=", forceToCache, " | withUserChanges=", withUserChanges, " | source=", source);
        searchByQuery(group, source, refreshRate, withUserChanges, new SearchByQuery<SLessons>() {
            @Override
            public void onWebRequest(String query, String source, RestResponseHandler<SLessons> handler) {
                switch (source) {
                    case SOURCE.ISU: {
                        String url = "schedule/common/group/%apikey%/" + query;
                        isuRestClient.get().get(context, url, null, new RestRestResponseHandlerJoiner<SLessons, ISUScheduleApiResponse>(handler) {
                            @Override
                            public void onSuccess(int code, Client.Headers headers, ISUScheduleApiResponse response) throws Exception {
                                handler.onSuccess(code, headers, convertIsuSchedule("group", query, response));
                            }
                            @Override
                            public ISUScheduleApiResponse newInstance() {
                                return new ISUScheduleApiResponse();
                            }
                        });
                        break;
                    }
                    case SOURCE.IFMO: {
                        String url = "schedule_lesson_group/" + query;
                        ifmoRestClient.get().get(context, url, null, new RestRestResponseHandlerJoiner<SLessons, ITMOSLessons>(handler) {
                            @Override
                            public void onSuccess(int code, Client.Headers headers, ITMOSLessons response) throws Exception {
                                handler.onSuccess(code, headers, convertIfmoSchedule("group", query, response));
                            }
                            @Override
                            public ITMOSLessons newInstance() {
                                return new ITMOSLessons();
                            }
                        });
                        break;
                    }
                }
            }
            @Override
            public void onFound(String query, SLessons schedule, boolean fromCache) {
                onScheduleFound(query, schedule, forceToCache, fromCache, withUserChanges);
            }
        });
    }

    @Override
    protected void searchRoom(String room, int refreshRate, boolean forceToCache, boolean withUserChanges) throws Exception {
        @Source String source = SOURCE.IFMO/*getSource()*/;
        log.v(TAG, "searchRoom | room=", room, " | refreshRate=", refreshRate,
                " | forceToCache=", forceToCache, " | withUserChanges=", withUserChanges, " | source=", source);
        searchByQuery(room, source, refreshRate, withUserChanges, new SearchByQuery<SLessons>() {
            @Override
            public void onWebRequest(String query, String source, RestResponseHandler<SLessons> handler) {
                switch (source) {
                    case SOURCE.ISU: // not available, using ifmo source
                    case SOURCE.IFMO: {
                        String url = "schedule_lesson_room/" + query;
                        ifmoRestClient.get().get(context, url, null, new RestRestResponseHandlerJoiner<SLessons, ITMOSLessons>(handler) {
                            @Override
                            public void onSuccess(int code, Client.Headers headers, ITMOSLessons response) throws Exception {
                                handler.onSuccess(code, headers, convertIfmoSchedule("room", query, response));
                            }
                            @Override
                            public ITMOSLessons newInstance() {
                                return new ITMOSLessons();
                            }
                        });
                        break;
                    }
                }
            }
            @Override
            public void onFound(String query, SLessons schedule, boolean fromCache) {
                onScheduleFound(query, schedule, forceToCache, fromCache, withUserChanges);
            }
        });
    }

    @Override
    protected void searchTeacher(String teacherId, int refreshRate, boolean forceToCache, boolean withUserChanges) throws Exception {
        @Source String source = getSource();
        log.v(TAG, "searchTeacher | teacherId=", teacherId, " | refreshRate=", refreshRate,
                " | forceToCache=", forceToCache, " | withUserChanges=", withUserChanges, " | source=", source);
        searchByQuery(teacherId, source, refreshRate, withUserChanges, new SearchByQuery<SLessons>() {
            @Override
            public void onWebRequest(String query, String source, RestResponseHandler<SLessons> handler) {
                switch (source) {
                    case SOURCE.ISU: {
                        String url = "schedule/common/teacher/%apikey%/" + query;
                        isuRestClient.get().get(context, url, null, new RestRestResponseHandlerJoiner<SLessons, ISUScheduleApiResponse>(handler) {
                            @Override
                            public void onSuccess(int code, Client.Headers headers, ISUScheduleApiResponse response) throws Exception {
                                handler.onSuccess(code, headers, convertIsuSchedule("teacher", query, response));
                            }
                            @Override
                            public ISUScheduleApiResponse newInstance() {
                                return new ISUScheduleApiResponse();
                            }
                        });
                        break;
                    }
                    case SOURCE.IFMO: {
                        String url = "schedule_lesson_person/" + query;
                        ifmoRestClient.get().get(context, url, null, new RestRestResponseHandlerJoiner<SLessons, ITMOSLessons>(handler) {
                            @Override
                            public void onSuccess(int code, Client.Headers headers, ITMOSLessons response) throws Exception {
                                handler.onSuccess(code, headers, convertIfmoSchedule("teacher", query, response));
                            }
                            @Override
                            public ITMOSLessons newInstance() {
                                return new ITMOSLessons();
                            }
                        });
                        break;
                    }
                }
            }
            @Override
            public void onFound(String query, SLessons schedule, boolean fromCache) {
                onScheduleFound(query, schedule, forceToCache, fromCache, withUserChanges);
            }
        });
    }

    @Override
    protected void searchTeachers(String lastname, boolean withUserChanges) throws Exception {
        @Source String source = SOURCE.IFMO/*getSource()*/;
        log.v(TAG, "searchTeachers | lastname=", lastname);
        searchByQuery(lastname, source, 0, withUserChanges, new SearchByQuery<SLessons>() {
            @Override
            public void onWebRequest(String query, String source, RestResponseHandler<SLessons> handler) {
                switch (source) {
                    case SOURCE.ISU: // not available, using ifmo source
                    case SOURCE.IFMO: {
                        String url = "schedule_person?lastname=" + lastname;
                        ifmoRestClient.get().get(context, url, null, new RestRestResponseHandlerJoiner<SLessons, STeachers>(handler) {
                            @Override
                            public void onSuccess(int code, Client.Headers headers, STeachers response) throws Exception {
                                if (response == null) {
                                    onFailure(code, headers, FAILED_LOAD);
                                    return;
                                }
                                SLessons schedule = new SLessons();
                                schedule.setQuery(query);
                                schedule.setType("teachers");
                                schedule.setTimestamp(time.getTimeInMillis());
                                schedule.setTeachers(response);
                                handler.onSuccess(code, headers, schedule);
                            }
                            @Override
                            public STeachers newInstance() {
                                return new STeachers();
                            }
                        });
                        break;
                    }
                }
            }
            @Override
            public void onFound(String query, SLessons schedule, boolean fromCache) {
                onScheduleFound(query, schedule, false, false, withUserChanges);
            }
        });
    }

    @Override
    protected String getType() {
        return TYPE;
    }

    @Override
    protected String getDefaultSource() {
        return SOURCE.ISU;
    }

    @Override
    protected SLessons getNewInstance() {
        return new SLessons();
    }

    @Override
    protected String getTraceName() {
        return FirebasePerformanceProvider.Trace.Schedule.LESSONS;
    }

    private SLessons convertIsuSchedule(String type, String query, ISUScheduleApiResponse isuSchedule) {
        if (isuSchedule == null) {
            return null;
        }
        SLessons schedule = new ScheduleLessonsIsuConverter(isuSchedule)
                .setType(type)
                .convert();
        return setupSchedule(schedule, type, query);
    }

    private SLessons convertIfmoSchedule(String type, String query, ITMOSLessons itmoSchedule) {
        if (itmoSchedule == null) {
            return null;
        }
        SLessons schedule = new ScheduleLessonsItmoConverter(itmoSchedule)
                .convert();
        return setupSchedule(schedule, type, query);
    }

    private SLessons setupSchedule(SLessons schedule, String type, String query) {
        if (schedule == null) {
            return null;
        }
        if (StringUtils.isBlank(schedule.getTitle())) {
            schedule.setTitle(query);
        }
        if ("personal".equals(type)) {
            schedule.setTitle(context.getString(R.string.personal_schedule));
        }
        schedule.setQuery(query);
        schedule.setType(type);
        schedule.setTimestamp(time.getTimeInMillis());
        return schedule;
    }

    private void onScheduleFound(String query, SLessons schedule, boolean forceToCache, boolean fromCache, boolean withUserChanges) {
        try {
            if (schedule == null) {
                invokePendingAndClose(query, withUserChanges, handler -> handler.onFailure(FAILED_NOT_FOUND));
                return;
            }
            if ("teachers".equals(schedule.getType())) {
                if (schedule.getTeachers() == null) {
                    invokePendingAndClose(query, withUserChanges, handler -> handler.onFailure(FAILED_LOAD));
                    return;
                }
                if (CollectionUtils.isNotEmpty(schedule.getTeachers().getTeachers())) {
                    invokePendingAndClose(query, withUserChanges, handler -> handler.onSuccess(schedule, fromCache));
                } else {
                    invokePendingAndClose(query, withUserChanges, handler -> handler.onFailure(FAILED_NOT_FOUND));
                }
                return;
            }
            if (!fromCache) {
                putToCache(query, schedule, forceToCache);
            }
            if (withUserChanges) {
                SLessons converted = new ScheduleLessonsAdditionalConverter(schedule.copy()).convert();
                invokePendingAndClose(query, true, handler -> handler.onSuccess(converted != null ? converted : schedule, fromCache));
            } else {
                invokePendingAndClose(query, false, handler -> handler.onSuccess(schedule, fromCache));
            }
        } catch (Throwable throwable) {
            log.exception(throwable);
            invokePendingAndClose(query, withUserChanges, handler -> handler.onFailure(FAILED_LOAD));
        }
    }
}
package com.bukhmastov.cdoitmo.object.schedule.impl;

import com.bukhmastov.cdoitmo.event.bus.annotation.Event;
import com.bukhmastov.cdoitmo.event.events.ClearCacheEvent;
import com.bukhmastov.cdoitmo.firebase.FirebasePerformanceProvider;
import com.bukhmastov.cdoitmo.model.converter.ScheduleLessonsAdditionalConverter;
import com.bukhmastov.cdoitmo.model.converter.ScheduleLessonsItmoConverter;
import com.bukhmastov.cdoitmo.model.schedule.lessons.SDay;
import com.bukhmastov.cdoitmo.model.schedule.lessons.SLessons;
import com.bukhmastov.cdoitmo.model.schedule.lessons.itmo.ITMOSLessons;
import com.bukhmastov.cdoitmo.model.schedule.teachers.STeachers;
import com.bukhmastov.cdoitmo.network.handlers.RestResponseHandler;
import com.bukhmastov.cdoitmo.object.schedule.ScheduleLessons;
import com.bukhmastov.cdoitmo.util.singleton.CollectionUtils;

import org.json.JSONObject;

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
            searchByQuery(group, source, refreshRate, withUserChanges, new SearchByQuery<SLessons>() {
                @Override
                public void onWebRequest(String query, String source, RestResponseHandler restResponseHandler) {
                    switch (source) {
                        case SOURCE.ISU: invokePendingAndClose(query, withUserChanges, handler -> handler.onFailure(FAILED_INVALID_QUERY)); break;
                        case SOURCE.IFMO: ifmoRestClient.get(context, "schedule_lesson_group/" + query, null, restResponseHandler); break;
                    }
                }
                @Override
                public SLessons onGetScheduleFromJson(String query, String source, JSONObject json) throws Exception {
                    return makeSchedule(query, source, "group", json);
                }
                @Override
                public void onFound(String query, SLessons schedule, boolean fromCache) {
                    onScheduleFound(query, schedule, forceToCache, fromCache, withUserChanges);
                }
            });
        });
    }

    @Override
    protected void searchRoom(String room, int refreshRate, boolean forceToCache, boolean withUserChanges) {
        thread.run(() -> {
            @Source String source = getSource();
            log.v(TAG, "searchRoom | room=", room, " | refreshRate=", refreshRate, " | forceToCache=", forceToCache, " | withUserChanges=", withUserChanges, " | source=", source);
            searchByQuery(room, source, refreshRate, withUserChanges, new SearchByQuery<SLessons>() {
                @Override
                public void onWebRequest(String query, String source, RestResponseHandler restResponseHandler) {
                    switch (source) {
                        case SOURCE.ISU: invokePendingAndClose(query, withUserChanges, handler -> handler.onFailure(FAILED_INVALID_QUERY)); break;
                        case SOURCE.IFMO: ifmoRestClient.get(context, "schedule_lesson_room/" + query, null, restResponseHandler); break;
                    }
                }
                @Override
                public SLessons onGetScheduleFromJson(String query, String source, JSONObject json) throws Exception {
                    return makeSchedule(query, source, "room", json);
                }
                @Override
                public void onFound(String query, SLessons schedule, boolean fromCache) {
                    onScheduleFound(query, schedule, forceToCache, fromCache, withUserChanges);
                }
            });
        });
    }

    @Override
    protected void searchTeacher(String teacherId, int refreshRate, boolean forceToCache, boolean withUserChanges) {
        thread.run(() -> {
            @Source String source = getSource();
            log.v(TAG, "searchTeacher | teacherId=", teacherId, " | refreshRate=", refreshRate, " | forceToCache=", forceToCache, " | withUserChanges=", withUserChanges, " | source=", source);
            searchByQuery(teacherId, source, refreshRate, withUserChanges, new SearchByQuery<SLessons>() {
                @Override
                public void onWebRequest(String query, String source, RestResponseHandler restResponseHandler) {
                    switch (source) {
                        case SOURCE.ISU: invokePendingAndClose(query, withUserChanges, handler -> handler.onFailure(FAILED_INVALID_QUERY)); break;
                        case SOURCE.IFMO: ifmoRestClient.get(context, "schedule_lesson_person/" + query, null, restResponseHandler); break;
                    }
                }
                @Override
                public SLessons onGetScheduleFromJson(String query, String source, JSONObject json) throws Exception {
                    return makeSchedule(query, source, "teacher", json);
                }
                @Override
                public void onFound(String query, SLessons schedule, boolean fromCache) {
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
            searchByQuery(lastname, source, 0, withUserChanges, new SearchByQuery<SLessons>() {
                @Override
                public void onWebRequest(String query, String source, RestResponseHandler restResponseHandler) {
                    switch (source) {
                        case SOURCE.ISU: invokePendingAndClose(query, withUserChanges, handler -> handler.onFailure(FAILED_INVALID_QUERY)); break;
                        case SOURCE.IFMO: ifmoRestClient.get(context, "schedule_person?lastname=" + lastname, null, restResponseHandler); break;
                    }
                }
                @Override
                public SLessons onGetScheduleFromJson(String query, String source, JSONObject json) throws Exception {
                    STeachers teachers = new STeachers().fromJson(json);
                    if (teachers == null) {
                        return null;
                    }
                    SLessons schedule = new SLessons();
                    schedule.setQuery(query);
                    schedule.setType("teachers");
                    schedule.setTimestamp(time.getTimeInMillis());
                    schedule.setTeachers(teachers);
                    return schedule;
                }
                @Override
                public void onFound(String query, SLessons schedule, boolean fromCache) {
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
    protected SLessons getNewInstance() {
        return new SLessons();
    }

    @Override
    protected String getTraceName() {
        return FirebasePerformanceProvider.Trace.Schedule.LESSONS;
    }

    private SLessons makeSchedule(String query, String source, String type, JSONObject json) throws Exception {
        switch (source) {
            case SOURCE.ISU: return null;
            case SOURCE.IFMO: {
                ITMOSLessons itmoSchedule = new ITMOSLessons().fromJson(json);
                if (itmoSchedule != null) {
                    SLessons schedule = new ScheduleLessonsItmoConverter(itmoSchedule).convert();
                    if (schedule != null) {
                        schedule.setQuery(query);
                        schedule.setType(type);
                        schedule.setTimestamp(time.getTimeInMillis());
                        return schedule;
                    }
                }
            }
        }
        return null;
    }

    private void onScheduleFound(String query, SLessons schedule, boolean forceToCache, boolean fromCache, boolean withUserChanges) {
        thread.run(() -> {
            if (context == null || query == null || schedule == null) {
                log.w(TAG, "onFound | some values are null | context=", context, " | query=", query, " | data=", schedule);
                if (query == null) {
                    return;
                }
                invokePendingAndClose(query, withUserChanges, handler -> handler.onFailure(FAILED_LOAD));
                return;
            }
            boolean valid = false;
            for (SDay day : schedule.getSchedule()) {
                if (day == null) {
                    continue;
                }
                if (CollectionUtils.isNotEmpty(day.getLessons())) {
                    valid = true;
                    break;
                }
            }
            if (valid) {
                if (!fromCache) {
                    putToCache(query, schedule, forceToCache);
                }
                if (withUserChanges) {
                    SLessons converted = new ScheduleLessonsAdditionalConverter(schedule).convert();
                    invokePendingAndClose(query, true, handler -> handler.onSuccess(converted != null ? converted : schedule, fromCache));
                } else {
                    invokePendingAndClose(query, false, handler -> handler.onSuccess(schedule, fromCache));
                }
                return;
            }
            if (!fromCache) {
                putToLocalCache(query, schedule);
            }
            invokePendingAndClose(query, withUserChanges, handler -> handler.onFailure(FAILED_NOT_FOUND));
        }, throwable -> {
            log.exception(throwable);
            invokePendingAndClose(query, withUserChanges, handler -> handler.onFailure(FAILED_LOAD));
        });
    }
}
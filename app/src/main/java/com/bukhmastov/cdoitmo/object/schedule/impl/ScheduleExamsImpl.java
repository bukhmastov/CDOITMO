package com.bukhmastov.cdoitmo.object.schedule.impl;

import com.bukhmastov.cdoitmo.R;
import com.bukhmastov.cdoitmo.event.bus.annotation.Event;
import com.bukhmastov.cdoitmo.event.events.ClearCacheEvent;
import com.bukhmastov.cdoitmo.firebase.FirebasePerformanceProvider;
import com.bukhmastov.cdoitmo.model.converter.ScheduleExamsIsuConverter;
import com.bukhmastov.cdoitmo.model.parser.ScheduleExamsGroupParser;
import com.bukhmastov.cdoitmo.model.parser.ScheduleExamsTeacherParser;
import com.bukhmastov.cdoitmo.model.schedule.exams.SExams;
import com.bukhmastov.cdoitmo.model.schedule.remote.isu.ISUScheduleApiResponse;
import com.bukhmastov.cdoitmo.model.schedule.teachers.STeachers;
import com.bukhmastov.cdoitmo.network.handlers.RestResponseHandler;
import com.bukhmastov.cdoitmo.network.handlers.joiner.RestRestResponseHandlerJoiner;
import com.bukhmastov.cdoitmo.network.handlers.joiner.RestStringResponseHandlerJoiner;
import com.bukhmastov.cdoitmo.network.model.Client;
import com.bukhmastov.cdoitmo.object.schedule.ScheduleExams;
import com.bukhmastov.cdoitmo.util.singleton.CollectionUtils;
import com.bukhmastov.cdoitmo.util.singleton.StringUtils;

import java.util.Arrays;
import java.util.List;

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
    protected void searchPersonal(int refreshRate, boolean forceToCache, boolean withUserChanges) throws Exception {
        List<String> sources = makeSources(SOURCE.ISU);
        log.v(TAG, "searchPersonal | refreshRate=", refreshRate, " | forceToCache=", forceToCache,
                " | withUserChanges=", withUserChanges, " | sources=", sources);
        searchByQuery("personal", sources, refreshRate, withUserChanges, new SearchByQuery<SExams>() {
            @Override
            public void onWebRequest(String query, String source, RestResponseHandler<SExams> handler) {
                switch (source) {
                    case SOURCE.IFMO: // not available, using isu source
                    case SOURCE.ISU: {
                        String url = "exams/personal/student/%apikey%/%isutoken%";
                        isuPrivateRestClient.get().get(context, url, null, new RestRestResponseHandlerJoiner<SExams, ISUScheduleApiResponse>(handler) {
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
            public void onFound(String query, SExams schedule, boolean fromCache) {
                onScheduleFound(query, schedule, forceToCache, fromCache, withUserChanges);
            }
        });
    }

    @Override
    protected void searchGroup(String group, int refreshRate, boolean forceToCache, boolean withUserChanges) throws Exception {
        List<String> sources = makeSources(SOURCE.ISU, SOURCE.IFMO);
        log.v(TAG, "searchGroup | group=", group, " | refreshRate=", refreshRate,
                " | forceToCache=", forceToCache, " | withUserChanges=", withUserChanges, " | sources=", sources);
        searchByQuery(group, sources, refreshRate, withUserChanges, new SearchByQuery<SExams>() {
            @Override
            public void onWebRequest(String query, String source, RestResponseHandler<SExams> handler) {
                switch (source) {
                    case SOURCE.ISU: {
                        String url = "exams/common/group/%apikey%/" + query;
                        isuRestClient.get().get(context, url, null, new RestRestResponseHandlerJoiner<SExams, ISUScheduleApiResponse>(handler) {
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
                        String url = "ru/exam/0/" + group + "/raspisanie_sessii.htm";
                        ifmoClient.get().get(context, url, null, new RestStringResponseHandlerJoiner(handler) {
                            @Override
                            public void onSuccess(int code, Client.Headers headers, String response) throws Exception {
                                SExams schedule = new ScheduleExamsGroupParser(response, query).parse();
                                if (schedule == null) {
                                    handler.onFailure(code, headers, FAILED_LOAD);
                                    return;
                                }
                                handler.onSuccess(code, headers, convertIfmoSchedule("group", query, schedule));
                            }
                        });
                        break;
                    }
                }
            }
            @Override
            public void onFound(String query, SExams schedule, boolean fromCache) {
                onScheduleFound(query, schedule, forceToCache, fromCache, withUserChanges);
            }
        });
    }

    @Override
    protected void searchRoom(String room, int refreshRate, boolean forceToCache, boolean withUserChanges) throws Exception {
        log.v(TAG, "searchRoom | rooms schedule is unavailable");
        invokePendingAndClose(room, withUserChanges, handler -> handler.onFailure(FAILED_INVALID_QUERY));
    }

    @Override
    protected void searchTeacher(String teacherId, int refreshRate, boolean forceToCache, boolean withUserChanges) throws Exception {
        List<String> sources = makeSources(SOURCE.ISU, SOURCE.IFMO);
        log.v(TAG, "searchTeacher | teacherId=", teacherId, " | refreshRate=", refreshRate,
                " | forceToCache=", forceToCache, " | withUserChanges=", withUserChanges, " | sources=", sources);
        searchByQuery(teacherId, sources, refreshRate, withUserChanges, new SearchByQuery<SExams>() {
            @Override
            public void onWebRequest(String query, String source, RestResponseHandler<SExams> handler) {
                switch (source) {
                    case SOURCE.ISU: {
                        String url = "exams/common/teacher/%apikey%/" + query;
                        isuRestClient.get().get(context, url, null, new RestRestResponseHandlerJoiner<SExams, ISUScheduleApiResponse>(handler) {
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
                        String url = "ru/exam/3/" + query + "/raspisanie_sessii.htm";
                        ifmoClient.get().get(context, url, null, new RestStringResponseHandlerJoiner(handler) {
                            @Override
                            public void onSuccess(int code, Client.Headers headers, String response) throws Exception {
                                SExams schedule = new ScheduleExamsTeacherParser(response, query).parse();
                                if (schedule == null) {
                                    handler.onFailure(code, headers, FAILED_LOAD);
                                    return;
                                }
                                handler.onSuccess(code, headers, convertIfmoSchedule("teacher", query, schedule));
                            }
                        });
                        break;
                    }
                }
            }
            @Override
            public void onFound(String query, SExams schedule, boolean fromCache) {
                onScheduleFound(query, schedule, forceToCache, fromCache, withUserChanges);
            }
        });
    }

    @Override
    protected void searchTeachers(String lastname, boolean withUserChanges) throws Exception {
        List<String> sources = makeSources(SOURCE.IFMO);
        log.v(TAG, "searchTeachers | lastname=", lastname, " | withUserChanges=", withUserChanges, " | sources=", sources);
        searchByQuery(lastname, sources, 0, withUserChanges, new SearchByQuery<SExams>() {
            @Override
            public void onWebRequest(String query, String source, RestResponseHandler<SExams> handler) {
                switch (source) {
                    case SOURCE.ISU: // not available, using ifmo source
                    case SOURCE.IFMO: {
                        String url = "schedule_person?lastname=" + lastname;
                        ifmoRestClient.get().get(context, url, null, new RestRestResponseHandlerJoiner<SExams, STeachers>(handler) {
                            @Override
                            public void onSuccess(int code, Client.Headers headers, STeachers response) throws Exception {
                                if (response == null) {
                                    onFailure(code, headers, FAILED_LOAD);
                                    return;
                                }
                                SExams schedule = new SExams();
                                schedule.setQuery(query);
                                schedule.setType("teachers");
                                schedule.setTimestamp(time.getTimeInMillis());
                                schedule.setDataSource(source);
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
            public void onFound(String query, SExams schedule, boolean fromCache) {
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
    protected List<String> getSupportedSources() {
        return Arrays.asList(SOURCE.ISU, SOURCE.IFMO);
    }

    @Override
    protected SExams getNewInstance() {
        return new SExams();
    }

    @Override
    protected String getTraceName() {
        return FirebasePerformanceProvider.Trace.Schedule.EXAMS;
    }

    private SExams convertIsuSchedule(String type, String query, ISUScheduleApiResponse isuSchedule) {
        if (isuSchedule == null) {
            return null;
        }
        SExams schedule = new ScheduleExamsIsuConverter(isuSchedule)
                .setType(type)
                .convert();
        return setupSchedule(schedule, type, query, SOURCE.ISU);
    }

    private SExams convertIfmoSchedule(String type, String query, SExams itmoSchedule) {
        return setupSchedule(itmoSchedule, type, query, SOURCE.IFMO);
    }

    private SExams setupSchedule(SExams schedule, String type, String query, String source) {
        if ("personal".equals(type)) {
            schedule.setTitle(context.getString(R.string.personal_schedule));
        } else if (StringUtils.isBlank(schedule.getTitle())) {
            schedule.setTitle(query);
        }
        schedule.setQuery(query);
        schedule.setType(type);
        schedule.setTimestamp(time.getTimeInMillis());
        schedule.setDataSource(source);
        return schedule;
    }

    private void onScheduleFound(String query, SExams schedule, boolean forceToCache, boolean fromCache, boolean withUserChanges) {
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
            invokePendingAndClose(query, withUserChanges, handler -> handler.onSuccess(schedule, fromCache));
        } catch (Throwable throwable) {
            log.exception(throwable);
            invokePendingAndClose(query, withUserChanges, handler -> handler.onFailure(FAILED_LOAD));
        }
    }
}

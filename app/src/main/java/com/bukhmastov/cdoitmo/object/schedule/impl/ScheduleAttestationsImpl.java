package com.bukhmastov.cdoitmo.object.schedule.impl;

import com.bukhmastov.cdoitmo.event.bus.annotation.Event;
import com.bukhmastov.cdoitmo.event.events.ClearCacheEvent;
import com.bukhmastov.cdoitmo.factory.AppComponentProvider;
import com.bukhmastov.cdoitmo.firebase.FirebasePerformanceProvider;
import com.bukhmastov.cdoitmo.model.parser.ScheduleAttestationsParser;
import com.bukhmastov.cdoitmo.model.schedule.attestations.SAttestations;
import com.bukhmastov.cdoitmo.network.DeIfmoClient;
import com.bukhmastov.cdoitmo.network.handlers.RestResponseHandler;
import com.bukhmastov.cdoitmo.network.handlers.joiner.RestStringResponseHandlerJoiner;
import com.bukhmastov.cdoitmo.network.model.Client;
import com.bukhmastov.cdoitmo.object.schedule.ScheduleAttestations;
import com.bukhmastov.cdoitmo.util.singleton.StringUtils;

import java.util.Calendar;

import javax.inject.Inject;

public class ScheduleAttestationsImpl extends ScheduleImpl<SAttestations> implements ScheduleAttestations {

    private static final String TAG = "ScheduleExams";

    @Inject
    DeIfmoClient deIfmoClient;

    public ScheduleAttestationsImpl() {
        AppComponentProvider.getComponent().inject(this);
        eventBus.register(this);
    }

    @Event
    public void onClearCacheEvent(ClearCacheEvent event) {
        if (event.isNot(ClearCacheEvent.SCHEDULE_ATTESTATIONS)) {
            return;
        }
        clearLocalCache();
    }

    @Override
    protected void searchPersonal(int refreshRate, boolean forceToCache, boolean withUserChanges) throws Exception {
        log.v(TAG, "searchPersonal | personal schedule is unavailable");
        invokePendingAndClose("personal", withUserChanges, handler -> handler.onFailure(FAILED_INVALID_QUERY));
    }

    @Override
    protected void searchGroup(String group, int refreshRate, boolean forceToCache, boolean withUserChanges) throws Exception {
        @Source String source = SOURCE.IFMO;
        log.v(TAG, "searchGroup | group=", group, " | refreshRate=", refreshRate,
                " | forceToCache=", forceToCache, " | withUserChanges=", withUserChanges, " | source=", source);
        searchByQuery(group, source, refreshRate, withUserChanges, new SearchByQuery<SAttestations>() {
            @Override
            public void onWebRequest(String query, String source, RestResponseHandler<SAttestations> handler) {
                switch (source) {
                    case SOURCE.ISU: // not available, using ifmo source
                    case SOURCE.IFMO: {
                        int term = getTerm();
                        String url = String.format("index.php?node=schedule&index=sched&semiId=%s&group=%s",
                                String.valueOf(term), StringUtils.prettifyGroupNumber(group));
                        deIfmoClient.get(context, url, null, new RestStringResponseHandlerJoiner(handler) {
                            @Override
                            public void onSuccess(int code, Client.Headers headers, String response) throws Exception {
                                SAttestations schedule = new ScheduleAttestationsParser(response, term).parse();
                                if (schedule == null) {
                                    handler.onFailure(code, headers, FAILED_LOAD);
                                    return;
                                }
                                schedule.setQuery(query);
                                schedule.setType("group");
                                schedule.setTitle(StringUtils.prettifyGroupNumber(group));
                                schedule.setTimestamp(time.getTimeInMillis());
                                handler.onSuccess(code, headers, schedule);
                            }
                        });
                        break;
                    }
                }
            }
            @Override
            public void onFound(String query, SAttestations schedule, boolean fromCache) {
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
        log.v(TAG, "searchTeacher | teacher schedule is unavailable");
        invokePendingAndClose(teacherId, withUserChanges, handler -> handler.onFailure(FAILED_INVALID_QUERY));
    }

    @Override
    protected void searchTeachers(String lastname, boolean withUserChanges) throws Exception {
        log.v(TAG, "searchTeachers | teachers schedule is unavailable");
        invokePendingAndClose(lastname, withUserChanges, handler -> handler.onFailure(FAILED_INVALID_QUERY));
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
    protected SAttestations getNewInstance() {
        return new SAttestations();
    }

    @Override
    protected String getTraceName() {
        return FirebasePerformanceProvider.Trace.Schedule.ATTESTATIONS;
    }

    private int getTerm() {
        int term;
        try {
            term = Integer.parseInt(storagePref.get(context, "pref_schedule_attestations_term", "0"));
            if (term == 0) {
                int month = time.getCalendar().get(Calendar.MONTH);
                if (month >= Calendar.SEPTEMBER || month == Calendar.JANUARY) {
                    term = 1;
                } else {
                    term = 2;
                }
            } else if (term < 1) {
                term = 1;
            } else if (term > 2) {
                term = 2;
            }
        } catch (Exception e) {
            term = 1;
        }
        return term;
    }

    private void onScheduleFound(String query, SAttestations schedule, boolean forceToCache, boolean fromCache, boolean withUserChanges) {
        try {
            if (schedule == null) {
                invokePendingAndClose(query, withUserChanges, handler -> handler.onFailure(FAILED_NOT_FOUND));
                return;
            }
            if (!fromCache) {
                putToCache(query, schedule, forceToCache);
            }
            invokePendingAndClose(query, withUserChanges, handler -> handler.onSuccess(schedule, fromCache));
        } catch (Exception e) {
            log.exception(e);
            invokePendingAndClose(query, withUserChanges, handler -> handler.onFailure(FAILED_LOAD));
        }
    }
}

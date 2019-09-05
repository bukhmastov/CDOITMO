package com.bukhmastov.cdoitmo.object.schedule.impl;

import com.bukhmastov.cdoitmo.App;
import com.bukhmastov.cdoitmo.R;
import com.bukhmastov.cdoitmo.firebase.FirebasePerformanceProvider;
import com.bukhmastov.cdoitmo.model.entity.SettingsQuery;
import com.bukhmastov.cdoitmo.model.schedule.ScheduleJsonEntity;
import com.bukhmastov.cdoitmo.network.handlers.RestResponseHandler;
import com.bukhmastov.cdoitmo.network.model.Client;
import com.bukhmastov.cdoitmo.object.schedule.Schedule;
import com.bukhmastov.cdoitmo.util.Storage;
import com.bukhmastov.cdoitmo.util.singleton.CollectionUtils;
import com.bukhmastov.cdoitmo.util.singleton.StringUtils;

import java.lang.annotation.Retention;
import java.lang.annotation.RetentionPolicy;
import java.text.ParseException;
import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Date;
import java.util.HashMap;
import java.util.LinkedList;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.Set;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

import androidx.annotation.NonNull;
import androidx.annotation.StringDef;

public abstract class ScheduleImpl<T extends ScheduleJsonEntity> extends ScheduleBase implements Schedule<T> {

    private static final String TAG = "Schedule";
    private static final int LOCAL_CACHE_THRESHOLD = 3;
    private final Map<String, T> localCache = new HashMap<>();
    private final Map<String, List<Handler<T>>> pendingStack = new HashMap<>();
    private final Map<String, String> trace = new HashMap<>();

    public ScheduleImpl() {
        super();
    }

    @Override
    public void search(@NonNull String query, @NonNull Handler<T> handler) {
        search(query, getRefreshRate(), handler);
    }

    @Override
    public void search(@NonNull String query, int refreshRate, @NonNull Handler<T> handler) {
        search(query, refreshRate, storagePref.get(context, "pref_schedule_" + getType() + "_use_cache", false), handler);
    }

    @Override
    public void search(@NonNull String query, boolean forceToCache, @NonNull Handler<T> handler) {
        search(query, getRefreshRate(), forceToCache, handler);
    }

    @Override
    public void search(@NonNull String query, boolean forceToCache, boolean withUserChanges, @NonNull Handler<T> handler) {
        search(query, getRefreshRate(), forceToCache, withUserChanges, handler);
    }

    @Override
    public void search(@NonNull String query, int refreshRate, boolean forceToCache, @NonNull Handler<T> handler) {
        search(query, refreshRate, forceToCache, true, handler);
    }

    @Override
    public void search(@NonNull String query, int refreshRate, boolean forceToCache, boolean withUserChanges, @NonNull Handler<T> handler) {
        try {
            thread.assertNotUI();
            if (StringUtils.isBlank(query)) {
                log.v(TAG, "search | empty query provided");
                handler.onFailure(FAILED_EMPTY_QUERY);
                return;
            }
            query = query.trim();
            log.v(TAG, "search | query=", query, " | refreshRate=", refreshRate, " | forceToCache=", forceToCache, " | withUserChanges=", withUserChanges);
            if ("personal".equals(query) && !isuPrivateRestClient.get().isAuthorized(context)) {
                log.v(TAG, "search | personal | isu auth required");
                handler.onFailure(FAILED_PERSONAL_NEED_ISU);
                return;
            }
            handler.onCancelRequest();
            if (query.contains(" ")) {
                query = query.split(" ")[0].trim();
            }
            if (!addPending(query, withUserChanges, handler)) {
                log.v(TAG, "search | query=", query, " | attached to the pending stack");
                return;
            }
            log.v(TAG, "search | query=", query, " | added to the pending stack | starting the search procedure");
            if (query.equals("personal")) {
                searchPersonal(refreshRate, forceToCache, withUserChanges);
                return;
            }
            if (query.matches("^[0-9]{6}$")) {
                searchTeacher(query, refreshRate, forceToCache, withUserChanges);
                return;
            }
            if (query.matches("^[0-9](.)*$")) {
                searchRoom(query, refreshRate, forceToCache, withUserChanges);
                return;
            }
            if (query.matches("^[a-zA-Z](.)*$")) {
                Matcher m = Pattern.compile("^([a-z])(\\d+.*)", Pattern.CASE_INSENSITIVE).matcher(query);
                if (m.find()) {
                    query = m.group(1).toUpperCase() + m.group(2).toLowerCase();
                }
                searchGroup(query, refreshRate, forceToCache, withUserChanges);
                return;
            }
            if (query.matches("^[а-яА-Я\\s]+$")) {
                query = query.toLowerCase();
                searchTeachers(query, withUserChanges);
                return;
            }
            log.v(TAG, "search | got invalid query: " + query);
            invokePendingAndClose(query, withUserChanges, h -> h.onFailure(FAILED_INVALID_QUERY));
        } catch (Throwable throwable) {
            log.exception(throwable);
            invokePendingAndClose(query, withUserChanges, h -> h.onFailure(FAILED_LOAD));
        }
    }

    /**
     * Remote source of schedules to be downloaded from
     */
    @Retention(RetentionPolicy.SOURCE)
    @StringDef({SOURCE.ISU, SOURCE.IFMO, SOURCE.DE_IFMO})
    protected @interface Source {}
    protected static class SOURCE {
        protected static final String ISU = "isu";
        protected static final String IFMO = "ifmo";
        protected static final String DE_IFMO = "de.ifmo";
    }

    /**
     * Search personal schedule, only from Source.ISU
     * @see Source
     */
    protected abstract void searchPersonal(int refreshRate, boolean forceToCache, boolean withUserChanges) throws Exception;

    /**
     * Searches schedule for defined group
     */
    protected abstract void searchGroup(String group, int refreshRate, boolean forceToCache, boolean withUserChanges) throws Exception;

    /**
     * Searches schedule for defined room
     */
    protected abstract void searchRoom(String room, int refreshRate, boolean forceToCache, boolean withUserChanges) throws Exception;

    /**
     * Searches schedule for defined teacher
     */
    protected abstract void searchTeacher(String teacherId, int refreshRate, boolean forceToCache, boolean withUserChanges) throws Exception;

    /**
     * Searches list of teachers by surname
     * Surname supports no spaces
     */
    protected abstract void searchTeachers(String lastname, boolean withUserChanges) throws Exception;

    /**
     * Defines the type of the current schedule
     * @return {"lessons", "exams", "attestations"}
     */
    protected abstract String getType();

    /**
     * Defines default source of the schedule
     * @return @Source
     * @see Source
     */
    protected abstract @Source String getDefaultSource();

    /**
     * Defines set of supported sources
     * @return set of supported @Source
     * @see Source
     */
    protected abstract @Source List<String> getSupportedSources();

    /**
     * Get new T instance
     */
    protected abstract T getNewInstance();

    /**
     * Indicates if teachers search available
     */
    protected abstract @FirebasePerformanceProvider.TRACE String getTraceName();

    /**
     * Defines the source set of the schedule
     * @return set of @Source
     * @see Source
     */
    protected @Source List<String> makeSources(@Source String...sources) {
        String token = "pref_schedule_" + getType() + "_source";
        String primarySource = storagePref.get(context, token, getDefaultSource());
        if (!getSupportedSources().contains(primarySource)) {
            primarySource = getDefaultSource();
            storagePref.put(context, token, primarySource);
        }
        LinkedList<String> sourceList = new LinkedList<>(Arrays.asList(sources));
        sourceList.remove(primarySource);
        sourceList.addFirst(primarySource);
        return sourceList;
    }

    /**
     * Returns the number of hours after which the schedule is considered to be overdue
     */
    protected int getRefreshRate() {
        try {
            return storagePref.get(context, "pref_use_cache", true) ?
                    Integer.parseInt(storagePref.get(context, "pref_static_refresh", "168")) :
                    0;
        } catch (Exception e) {
            return 0;
        }
    }

    /**
     * Returns a flag indicating that the schedule has expired and needs to be updated
     */
    protected boolean isForceRefresh(T schedule, int refreshRate) {
        if (refreshRate == 0 || schedule == null) {
            return true;
        }
        if (refreshRate > 0) {
            return schedule.getTimestamp() + refreshRate * 3600000L < time.getTimeInMillis();
        }
        return false;
    }

    /**
     * Returns the default query string for a schedule search
     */
    @Override
    public String getDefaultScope() {
        String preference = storagePref.get(context, "pref_schedule_" + getType() + "_default", "").trim();
        String scope;
        if (StringUtils.isBlank(preference)) {
            scope = "auto";
        } else {
            try {
                SettingsQuery settingsQuery = new SettingsQuery().fromJsonString(preference);
                scope = settingsQuery.getQuery();
            } catch (Exception e) {
                scope = "auto";
            }
        }
        switch (scope) {
            case "auto":
                String group = storage.get(context, Storage.PERMANENT, Storage.USER, "user#group");
                if (StringUtils.isNotBlank(group)) {
                    return group;
                }
            case "personal": return "personal";
            default: return scope;
        }
    }

    /**
     * Returns main title for schedules
     */
    @Override
    public String getScheduleHeader(String title, String type) {
        switch (type) {
            case "personal": title = context.getString(R.string.schedule_personal); break;
            case "group": title = (context.getString(R.string.schedule_group) + " " + title).trim(); break;
            case "teacher": title = (context.getString(R.string.schedule_teacher) + " " + title).trim(); break;
            case "room": title = (context.getString(R.string.schedule_room) + " " + title).trim(); break;
            case "teachers": break;
        }
        return title;
    }

    /**
     * Returns second title for schedules
     */
    @Override
    public String getScheduleWeek(int week) {
        if (week >= 0) {
            return week + " " + context.getString(R.string.school_week);
        } else {
            String pattern = "dd.MM.yyyy";
            String date = new SimpleDateFormat(pattern, Locale.ROOT).format(new Date(time.getCalendar().getTimeInMillis()));
            try {
                return dateUtils.cuteDateWithoutTime(context, pattern, date);
            } catch (ParseException e) {
                return date;
            }
        }
    }

    @Override
    public String getProgressMessage(int state) {
        return isuRestClient.get().getProgressMessage(context, state);
    }

    @Override
    public String getFailedMessage(int code, int state) {
        switch (state) {
            case FAILED_LOAD: return context.getString(R.string.schedule_failed_load);
            case FAILED_EMPTY_QUERY: return context.getString(R.string.schedule_failed_empty_query);
            case FAILED_INVALID_QUERY: return context.getString(R.string.schedule_failed_invalid_query);
            case FAILED_PERSONAL_NEED_ISU: return context.getString(R.string.schedule_failed_need_isu);
            case FAILED_NOT_FOUND: return context.getString(R.string.schedule_failed_not_found);
        }
        return isuRestClient.get().getFailedMessage(context, code, state);
    }

    // -->- Pending mechanism ->--

    /**
     * The mechanism of caching requests
     * Designed to prevent simultaneous search for schedules with the same request
     */
    @FunctionalInterface
    protected interface Pending<J extends ScheduleJsonEntity> {
        void invoke(Handler<J> handler);
    }

    /**
     * Add pending request to existing request or initialize new one
     * @return true - initialized new stack, false - attached to existing one
     */
    private boolean addPending(@NonNull String query, boolean withUserChanges, @NonNull Handler<T> handler) {
        synchronized (pendingStack) {
            log.v(TAG, "addPending | query=", query, " | withUserChanges=", withUserChanges);
            String token = getType() + "_" + query.toLowerCase() + "_" + (withUserChanges ? "t" : "f");
            if (pendingStack.containsKey(token)) {
                List<Handler<T>> handlers = pendingStack.get(token);
                if (CollectionUtils.isEmpty(handlers)) {
                    pendingStack.remove(token);
                    return addPending(query, withUserChanges, handler);
                }
                handlers.add(handler);
                return false;
            }
            List<Handler<T>> handlers = new ArrayList<>();
            handlers.add(handler);
            trace.put(token, firebasePerformanceProvider.startTrace(getTraceName()));
            pendingStack.put(token, handlers);
        }
        return true;
    }

    /**
     * Invokes new state on all handlers from related stack. Then clears stack, if remove=true
     */
    private void invokePending(String query, boolean withUserChanges, boolean remove, Pending<T> pending) {
        synchronized (pendingStack) {
            log.v(TAG, "invokePending | query=", query, " | withUserChanges=", withUserChanges, " | remove=", remove);
            String token = getType() + "_" + query.toLowerCase() + "_" + (withUserChanges ? "t" : "f");
            List<Handler<T>> handlers = pendingStack.get(token);
            if (handlers != null) {
                for (int i = handlers.size() - 1; i >= 0; i--) {
                    Handler<T> handler = handlers.get(i);
                    if (handler != null) {
                        log.v(TAG, "invokePending | query=", query, " | invoke");
                        pending.invoke(handler);
                    }
                }
            }
            if (remove) {
                log.v(TAG, "invokePending | query=", query, " | remove");
                pendingStack.remove(token);
                if (trace.containsKey(token)) {
                    firebasePerformanceProvider.stopTrace(trace.get(token));
                    trace.remove(token);
                }
            }
        }
    }

    protected void invokePending(String query, boolean withUserChanges, Pending<T> pending) {
        invokePending(query, withUserChanges, false, pending);
    }

    protected void invokePendingAndClose(String query, boolean withUserChanges, Pending<T> pending) {
        invokePending(query, withUserChanges, true, pending);
    }

    // --<- Pending mechanism -<--  //  -->- Cache mechanism ->--

    protected T getFromCache(String token) throws Exception {
        token = token.toLowerCase();
        log.v(TAG, "getCache | token=", token);
        T cache;
        synchronized (localCache) {
            cache = localCache.get(token);
        }
        if (cache == null) {
            String cached = storage.get(context, Storage.CACHE, Storage.GLOBAL, "schedule_" + getType() + "#lessons#" + token, null);
            if (StringUtils.isNotBlank(cached)) {
                cache = getNewInstance().fromJsonString(cached);
            }
        }
        return cache;
    }

    protected void putToCache(String token, T schedule, boolean forceToCache) throws Exception {
        if (schedule == null) {
            return;
        }
        log.v(TAG, "putToCache | token=", token, " | forceToCache=", forceToCache);
        token = token.toLowerCase();
        putToLocalCache(token, schedule);
        if (forceToCache || token.equals(getDefaultScope().toLowerCase()) || storage.exists(context, Storage.CACHE, Storage.GLOBAL, "schedule_" + getType() + "#lessons#" + token)) {
            log.v(TAG, "putToCache | token=", token, " | proceed");
            storage.put(context, Storage.CACHE, Storage.GLOBAL, "schedule_" + getType() + "#lessons#" + token, schedule.toJsonString());
        }
    }

    protected void putToLocalCache(String token, T schedule) {
        if (schedule == null) {
            return;
        }
        log.v(TAG, "putToLocalCache | token=", token);
        token = token.toLowerCase();
        synchronized (localCache) {
            if (localCache.size() > LOCAL_CACHE_THRESHOLD - 1) {
                Set<String> keySet = localCache.keySet();
                int i = 0;
                for (String key : keySet) {
                    if (i++ < LOCAL_CACHE_THRESHOLD) {
                        continue;
                    }
                    localCache.remove(key);
                }
            }
            localCache.put(token, schedule);
        }
    }

    protected void removeFromCache(String token) {
        token = token.toLowerCase();
        log.v(TAG, "removeFromCache | token=", token);
        synchronized (localCache) {
            localCache.remove(token);
        }
        storage.delete(context, Storage.CACHE, Storage.GLOBAL, "schedule_" + getType() + "#lessons#" + token);
    }

    protected void clearLocalCache() {
        log.v(TAG, "clearLocalCache");
        synchronized (localCache) {
            localCache.clear();
        }
    }

    // --<- Cache mechanism -<--

    protected void searchByQuery(String query, @Source List<String> sources, int refreshRate, boolean withUserChanges, SearchByQuery<T> search) throws Exception {
        log.v(TAG, "searchByQuery | query=", query, " | sources=", sources, " | refreshRate=", refreshRate, " | withUserChanges=", withUserChanges);
        T cached = getFromCache(query);
        if (App.OFFLINE_MODE) {
            if (cached != null) {
                search.onFound(query, cached, true);
            } else {
                invokePendingAndClose(query, withUserChanges, handler -> handler.onFailure(Client.FAILED_OFFLINE));
            }
            return;
        }
        if (cached != null && !isForceRefresh(cached, refreshRate)) {
            search.onFound(query, cached, true);
            return;
        }
        if (sources.size() < 1) {
            log.wtf(TAG, "searchByQuery | query=", query, " | zero sources provided");
            invokePendingAndClose(query, withUserChanges, handler -> handler.onFailure(FAILED_LOAD));
            return;
        }
        searchByQuery(query, cached, sources, 0, withUserChanges, search);
    }

    private void searchByQuery(String query, T cached, @Source List<String> sources, int sourceIndex, boolean withUserChanges, SearchByQuery<T> search) {
        String source = sources.get(sourceIndex);
        log.v(TAG, "searchByQuery | query=", query, " | sources=", sources, " | source=", source, " | withUserChanges=", withUserChanges);
        search.onWebRequest(query, source, new RestResponseHandler<T>() {
            @Override
            public void onSuccess(int code, Client.Headers headers, T schedule) throws Exception {
                log.v(TAG, "searchByQuery | query=", query, " || onSuccess | code=", code, " | schedule=", schedule);
                if (code == 200 && schedule != null) {
                    if (schedule.isEmptySchedule()) {
                        onSourceFailed(schedule, code, headers, FAILED_NOT_FOUND);
                    } else {
                        search.onFound(query, schedule, false);
                    }
                    return;
                }
                if (cached != null) {
                    search.onFound(query, cached, true);
                } else {
                    onSourceFailed(schedule, code, headers, FAILED_LOAD);
                }
            }
            @Override
            public void onFailure(int code, Client.Headers headers, int state) {
                log.v(TAG, "searchByQuery | query=", query, " || onFailure | code=", code, " | state=", state);
                onSourceFailed(null, code, headers, state);
            }
            @Override
            public void onProgress(int state) {
                log.v(TAG, "searchByQuery | query=", query, " || onProgress | state=", state);
                invokePending(query, withUserChanges, handler -> handler.onProgress(state));
            }
            @Override
            public void onNewRequest(final Client.Request request) {
                invokePending(query, withUserChanges, handler -> handler.onNewRequest(request));
            }
            @Override
            public T newInstance() {
                return getNewInstance();
            }

            private void onSourceFailed(T schedule, int code, Client.Headers headers, int state) {
                int nextSourceIndex = sourceIndex + 1;
                if (nextSourceIndex < sources.size() && state != Client.FAILED_INTERRUPTED) {
                    searchByQuery(query, cached, sources, nextSourceIndex, withUserChanges, search);
                    return;
                }
                if (schedule != null && schedule.isEmptySchedule()) {
                    search.onFound(query, schedule, false);
                    return;
                }
                if (cached != null && state != Client.FAILED_INTERRUPTED) {
                    search.onFound(query, cached, true);
                    return;
                }
                if (code == 404) {
                    invokePendingAndClose(query, withUserChanges, handler -> handler.onFailure(code, headers, FAILED_NOT_FOUND));
                    return;
                }
                invokePendingAndClose(query, withUserChanges, handler -> handler.onFailure(code, headers, state));
            }
        });
    }

    protected interface SearchByQuery<S extends ScheduleJsonEntity> {
        void onWebRequest(String query, @Source String source, RestResponseHandler<S> restResponseHandler);
        void onFound(String query, S schedule, boolean fromCache);
    }
}

package com.bukhmastov.cdoitmo.object.schedule.impl;

import android.support.annotation.NonNull;
import android.support.annotation.StringDef;

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

import org.json.JSONArray;
import org.json.JSONObject;

import java.lang.annotation.Retention;
import java.lang.annotation.RetentionPolicy;
import java.text.ParseException;
import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.Date;
import java.util.HashMap;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.Set;

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
        thread.run(() -> {
            if (StringUtils.isBlank(query)) {
                log.v(TAG, "search | empty query provided");
                handler.onFailure(FAILED_EMPTY_QUERY);
                return;
            }
            String q = query.trim();
            log.v(TAG, "search | query=", q, " | refreshRate=", refreshRate, " | forceToCache=", forceToCache, " | withUserChanges=", withUserChanges);
            handler.onCancelRequest();
            if (q.contains(" ")) {
                q = q.split(" ")[0].trim();
            }
            if (!addPending(q, withUserChanges, handler)) {
                log.v(TAG, "search | query=", q, " | attached to the pending stack");
                return;
            }
            log.v(TAG, "search | query=", q, " | added to the pending stack | starting the search procedure");
            if (q.equals("mine")) {
                searchMine(refreshRate, forceToCache, withUserChanges);
                return;
            }
            if (q.matches("^[0-9]{6}$")) {
                searchTeacher(q, refreshRate, forceToCache, withUserChanges);
                return;
            }
            if (q.matches("^[0-9](.)*$")) {
                searchRoom(q, refreshRate, forceToCache, withUserChanges);
                return;
            }
            if (q.matches("^[a-zA-Z](.)*$")) {
                if (q.matches("^[a-zA-Z][0-9]{4}[a-zA-Z]?$")) {
                    q = q.substring(0, 1).toUpperCase() + q.substring(1).toLowerCase();
                }
                searchGroup(q, refreshRate, forceToCache, withUserChanges);
                return;
            }
            if (q.matches("^[а-яА-Я\\s]+$")) {
                q = q.toLowerCase();
                searchTeachers(q, withUserChanges);
                return;
            }
            log.v(TAG, "search | got invalid query: " + q);
            invokePendingAndClose(q, withUserChanges, h -> h.onFailure(FAILED_INVALID_QUERY));
        });
    }

    /**
     * Remote source of schedules to be downloaded from
     * Source.ISU - Currently unavailable
     */
    @Retention(RetentionPolicy.SOURCE)
    @StringDef({SOURCE.ISU, SOURCE.IFMO})
    protected @interface Source {}
    protected static class SOURCE {
        protected static final String ISU = "isu"; // Currently unavailable
        protected static final String IFMO = "ifmo";
    }

    /**
     * Search personal schedule, only from Source.ISU
     * Currently unavailable
     * @see Source
     */
    protected abstract void searchMine(int refreshRate, boolean forceToCache, boolean withUserChanges);

    /**
     * Searches schedule for defined group
     */
    protected abstract void searchGroup(String group, int refreshRate, boolean forceToCache, boolean withUserChanges);

    /**
     * Searches schedule for defined room
     */
    protected abstract void searchRoom(String room, int refreshRate, boolean forceToCache, boolean withUserChanges);

    /**
     * Searches schedule for defined teacher
     */
    protected abstract void searchTeacher(String teacherId, int refreshRate, boolean forceToCache, boolean withUserChanges);

    /**
     * Searches list of teachers by surname
     * Surname supports no spaces
     */
    protected abstract void searchTeachers(String lastname, boolean withUserChanges);

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
     * Get new T instance
     */
    protected abstract T getNewInstance();

    /**
     * Indicates if teachers search available
     */
    protected abstract @FirebasePerformanceProvider.TRACE String getTraceName();

    /**
     * Defines the source of the schedule
     * @see Source
     */
    protected @Source String getSource() {
        String token = "pref_schedule_" + getType() + "_source";
        String source = storagePref.get(context, token, getDefaultSource());
        switch (source) {
            case "ifmo": case "isu": break;
            default: {
                source = getDefaultSource();
                storagePref.put(context, token, source);
            }
        }
        return source;
    }

    /**
     * Returns the number of hours after which the schedule is considered to be overdue
     */
    protected int getRefreshRate() {
        try {
            return storagePref.get(context, "pref_use_cache", true) ? Integer.parseInt(storagePref.get(context, "pref_static_refresh", "168")) : 0;
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
        String preference = storageProvider.getStoragePref().get(context, "pref_schedule_" + getType() + "_default", "").trim();
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
            case "auto": return storageProvider.getStorage().get(context, Storage.PERMANENT, Storage.USER, "user#group");
            case "mine":
            default: return scope;
        }
    }

    /**
     * Returns main title for schedules
     */
    @Override
    public String getScheduleHeader(String title, String type) {
        switch (type) {
            case "mine": title = context.getString(R.string.schedule_personal); break;
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
                return textUtils.cuteDateWithoutTime(context, storagePref, pattern, date);
            } catch (ParseException e) {
                return date;
            }
        }
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
    protected boolean addPending(@NonNull String query, boolean withUserChanges, @NonNull Handler<T> handler) {
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
        return true;
    }

    /**
     * Invokes new state on all handlers from related stack. Then clears stack, if remove=true
     */
    protected void invokePending(String query, boolean withUserChanges, boolean remove, Pending<T> pending) {
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
            pendingStack.remove(token);
            if (trace.containsKey(token)) {
                firebasePerformanceProvider.stopTrace(trace.get(token));
                trace.remove(token);
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

    protected void searchByQuery(String query, @Source String source, int refreshRate, boolean withUserChanges, SearchByQuery<T> search) {
        thread.run(() -> {
            log.v(TAG, "searchByQuery | query=", query, " | source=", source, " | refreshRate=", refreshRate, " | withUserChanges=", withUserChanges);
            T cached = getFromCache(query);
            if (App.OFFLINE_MODE) {
                if (cached != null) {
                    search.onFound(query, cached, true);
                } else {
                    invokePendingAndClose(query, withUserChanges, handler -> handler.onFailure(FAILED_OFFLINE));
                }
                return;
            }
            RestResponseHandler restResponseHandler = new RestResponseHandler() {
                @Override
                public void onSuccess(final int statusCode, final Client.Headers headers, final JSONObject obj, final JSONArray arr) {
                    thread.run(() -> {
                        log.v(TAG, "searchByQuery | query=", query, " || onSuccess | statusCode=", statusCode, " | data=", obj);
                        if (statusCode == 200 && obj != null) {
                            T schedule = search.onGetScheduleFromJson(query, source, obj);
                            if (schedule != null) {
                                search.onFound(query, schedule, false);
                                return;
                            }
                        }
                        if (cached != null) {
                            search.onFound(query, cached, true);
                        } else {
                            invokePendingAndClose(query, withUserChanges, handler -> handler.onFailure(statusCode, headers, FAILED_LOAD));
                        }
                    }, throwable -> {
                        invokePendingAndClose(query, withUserChanges, handler -> handler.onFailure(statusCode, headers, FAILED_LOAD));
                    });
                }
                @Override
                public void onFailure(final int statusCode, final Client.Headers headers, final int state) {
                    log.v(TAG, "searchByQuery | query=", query, " || onFailure | statusCode=", statusCode, " | state=", state);
                    if (cached != null) {
                        search.onFound(query, cached, true);
                    } else {
                        invokePendingAndClose(query, withUserChanges, handler -> handler.onFailure(statusCode, headers, state));
                    }
                }
                @Override
                public void onProgress(final int state) {
                    log.v(TAG, "searchByQuery | query=", query, " || onProgress | state=", state);
                    invokePending(query, withUserChanges, handler -> handler.onProgress(state));
                }
                @Override
                public void onNewRequest(final Client.Request request) {
                    invokePending(query, withUserChanges, handler -> handler.onNewRequest(request));
                }
            };
            if (isForceRefresh(cached, refreshRate)) {
                log.v(TAG, "searchByQuery | query=", query, " | force refresh");
                search.onWebRequest(query, source, restResponseHandler);
                return;
            }
            if (cached != null) {
                search.onFound(query, cached, true);
            } else {
                search.onWebRequest(query, source, restResponseHandler);
            }
        });
    }

    protected interface SearchByQuery<J extends ScheduleJsonEntity> {
        void onWebRequest(String query, @Source String source, RestResponseHandler restResponseHandler);
        J onGetScheduleFromJson(String query, @Source String source, JSONObject json) throws Exception;
        void onFound(String query, J schedule, boolean fromCache);
    }
}

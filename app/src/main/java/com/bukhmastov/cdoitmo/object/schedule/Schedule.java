package com.bukhmastov.cdoitmo.object.schedule;

import android.content.Context;
import android.support.annotation.StringDef;

import com.bukhmastov.cdoitmo.R;
import com.bukhmastov.cdoitmo.converter.schedule.ScheduleTeachersConverter;
import com.bukhmastov.cdoitmo.firebase.FirebasePerformanceProvider;
import com.bukhmastov.cdoitmo.network.IfmoRestClient;
import com.bukhmastov.cdoitmo.network.interfaces.RestResponseHandler;
import com.bukhmastov.cdoitmo.network.model.Client;
import com.bukhmastov.cdoitmo.util.Log;
import com.bukhmastov.cdoitmo.util.Static;
import com.bukhmastov.cdoitmo.util.Storage;

import org.json.JSONArray;
import org.json.JSONException;
import org.json.JSONObject;

import java.lang.annotation.Retention;
import java.lang.annotation.RetentionPolicy;
import java.text.ParseException;
import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.Date;
import java.util.HashMap;
import java.util.Locale;
import java.util.Map;
import java.util.Set;

public abstract class Schedule {

    private static final String TAG = "Schedule";
    public interface Handler {
        void onSuccess(JSONObject json, boolean fromCache);
        void onFailure(int state);
        void onFailure(int statusCode, Client.Headers headers, int state);
        void onProgress(int state);
        void onNewRequest(Client.Request request);
        void onCancelRequest();
    }
    public interface ScheduleSearchProvider {
        void onSearch(Context context, String query, Handler handler);
    }
    protected interface SearchByQuery {
        boolean isWebAvailable();
        void onWebRequest(String query, String cache, RestResponseHandler restResponseHandler);
        void onWebRequestSuccess(String query, JSONObject data, JSONObject template);
        void onWebRequestFailed(int statusCode, Client.Headers headers, int state);
        void onWebRequestProgress(int state);
        void onWebNewRequest(Client.Request request);
        void onFound(String query, JSONObject data, boolean putToCache, boolean fromCache);
    }
    protected interface SearchFromCache {
        void onDone(String query, JSONObject data);
        void onEmpty(String query);
    }
    private final Handler handler;
    public static final int FAILED_LOAD = 100;
    public static final int FAILED_OFFLINE = 101;
    public static final int FAILED_EMPTY_QUERY = 102;
    public static final int FAILED_MINE_NEED_ISU = 103;
    public static final int FAILED_INVALID_QUERY = 104;
    public static final int FAILED_NOT_FOUND = 105;

    // Remote source of schedules to be downloaded from
    @Retention(RetentionPolicy.SOURCE)
    @StringDef({SOURCE.ISU, SOURCE.IFMO})
    protected @interface Source {}
    protected static class SOURCE {
        protected static final String ISU = "isu"; // currently unused
        protected static final String IFMO = "ifmo";
    }

    public Schedule(final Handler handler) {
        this.handler = handler;
    }

    // Defines the type of the current schedule.
    // Actual values: "lessons", "exams", "attestations".
    protected abstract String getType();

    // Defines default source of the schedule
    protected abstract @Source String getDefaultSource();

    // -->- Search schedule ->--
    // Search functions to be invoked.
    public void search(final Context context, final String query) {
        Static.T.runThread(() -> search(context, query, getRefreshRate(context)));
    }
    public void search(final Context context, final String query, final int refreshRate) {
        Static.T.runThread(() -> search(context, query, refreshRate, Storage.pref.get(context, "pref_schedule_" + getType() + "_use_cache", false)));
    }
    public void search(final Context context, final String query, final boolean forceToCache) {
        Static.T.runThread(() -> search(context, query, getRefreshRate(context), forceToCache));
    }
    public void search(final Context context, final String query, final boolean forceToCache, final boolean withUserChanges) {
        Static.T.runThread(() -> search(context, query, getRefreshRate(context), forceToCache, withUserChanges));
    }
    public void search(final Context context, final String query, final int refreshRate, final boolean forceToCache) {
        Static.T.runThread(() -> search(context, query, refreshRate, forceToCache, true));
    }
    public void search(final Context context, final String query, final int refreshRate, final boolean forceToCache, final boolean withUserChanges) {
        Static.T.runThread(() -> {
            String q = query.trim();
            Log.v(TAG, "search | query=", q, " | refreshRate=", refreshRate, " | forceToCache=", forceToCache, " | withUserChanges=", withUserChanges);
            if (q.isEmpty()) {
                handler.onFailure(FAILED_EMPTY_QUERY);
                return;
            }
            handler.onCancelRequest();
            if (q.contains(" ")) {
                q = q.split(" ")[0].trim();
            }
            if (addPending(q, withUserChanges, handler)) {
                Log.v(TAG, "search | query=", q, " | initialized the pending stack | starting the search procedure");
                if (q.equals("mine")) {
                    searchMine(context, refreshRate, forceToCache, withUserChanges);
                } else if (q.matches("^[0-9]{6}$")) {
                    searchTeacher(context, q, refreshRate, forceToCache, withUserChanges);
                } else if (q.matches("^[0-9](.)*$")) {
                    searchRoom(context, q, refreshRate, forceToCache, withUserChanges);
                } else if (q.matches("^[a-zA-Z](.)*$")) {
                    if (q.matches("^[a-zA-Z][0-9]{4}[a-zA-Z]?$")) {
                        q = q.substring(0, 1).toUpperCase() + q.substring(1).toLowerCase();
                    }
                    searchGroup(context, q, refreshRate, forceToCache, withUserChanges);
                } else if (q.matches("^[а-яА-Я\\s]+$")) {
                    q = q.toLowerCase();
                    searchTeachers(context, q, refreshRate, forceToCache, withUserChanges);
                } else {
                    Log.v(TAG, "search | got invalid query: " + q);
                    invokePending(q, withUserChanges, true, handler -> handler.onFailure(FAILED_INVALID_QUERY));
                }
            } else {
                Log.v(TAG, "search | query=", q, " | added to the pending stack");
            }
        });
    }
    // The mechanism of caching requests.
    // Designed to prevent simultaneous search for schedules with the same request.
    private final static Map<String, ArrayList<Handler>> pending = new HashMap<>();
    private final static Map<String, String> trace = new HashMap<>();
    protected interface Pending {
        void invoke(Handler handler);
    }
    protected boolean addPending(String query, boolean withUserChanges, Handler handler) {
        Log.v(TAG, "addPending | query=", query, " | withUserChanges=", withUserChanges);
        final String token = getType() + "_" + query.toLowerCase() + "_" + (withUserChanges ? "t" : "f");
        if (Schedule.pending.containsKey(token)) {
            Schedule.pending.get(token).add(handler);
            return false;
        } else {
            ArrayList<Handler> handlers = new ArrayList<>();
            handlers.add(handler);
            String name;
            switch (getType()) {
                case "attestations": name = FirebasePerformanceProvider.Trace.Schedule.ATTESTATIONS; break;
                case "exams": name = FirebasePerformanceProvider.Trace.Schedule.EXAMS; break;
                case "lessons": default: name = FirebasePerformanceProvider.Trace.Schedule.LESSONS; break;
            }
            Schedule.trace.put(token, FirebasePerformanceProvider.startTrace(name));
            Schedule.pending.put(token, handlers);
            return true;
        }
    }
    protected void invokePending(String query, boolean withUserChanges, boolean remove, Pending pending) {
        Log.v(TAG, "invokePending | query=", query, " | withUserChanges=", withUserChanges, " | remove=", remove);
        final String token = getType() + "_" + query.toLowerCase() + "_" + (withUserChanges ? "t" : "f");
        final ArrayList<Handler> handlers = Schedule.pending.get(token);
        if (handlers != null) {
            for (int i = handlers.size() - 1; i >= 0; i--) {
                Handler handler = handlers.get(i);
                if (handler != null) {
                    Log.v(TAG, "invokePending | query=", query, " | invoke");
                    pending.invoke(handler);
                }
            }
        }
        if (remove) {
            Schedule.pending.remove(token);
            if (Schedule.trace.containsKey(token)) {
                FirebasePerformanceProvider.stopTrace(Schedule.trace.get(token));
                Schedule.trace.remove(token);
            }
        }
    }
    // Private search functions to search each type of the schedule.
    protected abstract void searchMine(final Context context, final int refreshRate, final boolean forceToCache, final boolean withUserChanges);
    protected abstract void searchGroup(final Context context, final String group, final int refreshRate, final boolean forceToCache, final boolean withUserChanges);
    protected abstract void searchRoom(final Context context, final String room, final int refreshRate, final boolean forceToCache, final boolean withUserChanges);
    protected abstract void searchTeacher(final Context context, final String teacherId, final int refreshRate, final boolean forceToCache, final boolean withUserChanges);
    protected abstract boolean searchTeachersAvailable();
    protected void searchTeachers(final Context context, final String teacherName, final int refreshRate, final boolean forceToCache, final boolean withUserChanges) {
        // Teachers search is the same for lessons and exams
        Log.v(TAG, "searchTeachers | teacherName=", teacherName, " | refreshRate=", refreshRate, " | forceToCache=", forceToCache, " | withUserChanges=", withUserChanges);
        if (!searchTeachersAvailable()) {
            Log.v(TAG, "searchTeachers | not available");
            invokePending(teacherName, withUserChanges, true, handler -> handler.onFailure(FAILED_INVALID_QUERY));
            return;
        }
        Static.T.runThread(() -> searchByQuery(context, "teachers", teacherName, refreshRate, withUserChanges, new SearchByQuery() {
            @Override
            public boolean isWebAvailable() {
                return true;
            }
            @Override
            public void onWebRequest(final String query, final String cache, final RestResponseHandler restResponseHandler) {
                IfmoRestClient.get(context, "schedule_person?lastname=" + query, null, restResponseHandler);
            }
            @Override
            public void onWebRequestSuccess(final String query, final JSONObject data, final JSONObject template) {
                final SearchByQuery self = this;
                Static.T.runThread(new ScheduleTeachersConverter(data, template, json -> self.onFound(query, json, false, false)));
            }
            @Override
            public void onWebRequestFailed(final int statusCode, final Client.Headers headers, final int state) {
                invokePending(teacherName, withUserChanges, true, handler -> handler.onFailure(statusCode, headers, state));
            }
            @Override
            public void onWebRequestProgress(final int state) {
                invokePending(teacherName, withUserChanges, false, handler -> handler.onProgress(state));
            }
            @Override
            public void onWebNewRequest(final Client.Request request) {
                invokePending(teacherName, withUserChanges, false, handler -> handler.onNewRequest(request));
            }
            @Override
            public void onFound(final String query, final JSONObject data, final boolean putToCache, boolean fromCache) {
                invokePending(teacherName, withUserChanges, true, handler -> {
                    putLocalCache(query, data.toString());
                    handler.onSuccess(data, false);
                });
            }
        }));
    }
    // Private functions to proceed search and get schedule from cache
    protected void searchByQuery(final Context context, final String type, final String query, final int refreshRate, final boolean withUserChanges, final SearchByQuery search) {
        Static.T.runThread(() -> {
            Log.v(TAG, "searchByQuery | type=", type, " | query=", query, " | refreshRate=", refreshRate);
            final String cache = getCache(context, query);
            if (!Static.OFFLINE_MODE) {
                if (search.isWebAvailable()) {
                    final RestResponseHandler restResponseHandler = new RestResponseHandler() {
                        @Override
                        public void onSuccess(final int statusCode, final Client.Headers headers, final JSONObject data, final JSONArray responseArr) {
                            Static.T.runThread(() -> {
                                Log.v(TAG, "searchByQuery | type=", type, " | query=", query, " || onSuccess | statusCode=", statusCode, " | data=", data);
                                if (statusCode == 200 && data != null) {
                                    final JSONObject template = getTemplate(query, type);
                                    if (template == null) {
                                        search.onWebRequestFailed(statusCode, headers, FAILED_LOAD);
                                    } else {
                                        search.onWebRequestSuccess(query, data, template);
                                    }
                                } else {
                                    searchFromCache(context, type, cache, query, new SearchFromCache() {
                                        @Override
                                        public void onDone(final String q, final JSONObject d) {
                                            search.onFound(q, d, false, true);
                                        }
                                        @Override
                                        public void onEmpty(String q) {
                                            search.onWebRequestFailed(statusCode, headers, FAILED_LOAD);
                                        }
                                    });
                                }
                            });
                        }
                        @Override
                        public void onFailure(final int statusCode, final Client.Headers headers, final int state) {
                            Log.v(TAG, "searchByQuery | type=", type, " | query=", query, " || onFailure | statusCode=", statusCode, " | state=", state);
                            searchFromCache(context, type, cache, query, new SearchFromCache() {
                                @Override
                                public void onDone(final String q, final JSONObject d) {
                                    search.onFound(q, d, false, true);
                                }
                                @Override
                                public void onEmpty(String q) {
                                    search.onWebRequestFailed(statusCode, headers, state);
                                }
                            });
                        }
                        @Override
                        public void onProgress(final int state) {
                            Log.v(TAG, "searchByQuery | type=", type, " | query=", query, " || onProgress | state=", state);
                            search.onWebRequestProgress(state);
                        }
                        @Override
                        public void onNewRequest(final Client.Request request) {
                            search.onWebNewRequest(request);
                        }
                    };
                    if (isForceRefresh(cache, refreshRate)) {
                        Log.v(TAG, "searchByQuery | type=", type, " | query=", query, " | force refresh");
                        search.onWebRequest(query, cache, restResponseHandler);
                    } else {
                        searchFromCache(context, type, cache, query, new SearchFromCache() {
                            @Override
                            public void onDone(String q, JSONObject d) {
                                search.onFound(q, d, false, true);
                            }
                            @Override
                            public void onEmpty(String q) {
                                search.onWebRequest(q, cache, restResponseHandler);
                            }
                        });
                    }
                }
            } else {
                searchFromCache(context, type, cache, query, new SearchFromCache() {
                    @Override
                    public void onDone(String q, JSONObject d) {
                        search.onFound(q, d, false, true);
                    }
                    @Override
                    public void onEmpty(String q) {
                        invokePending(q, withUserChanges,true, handler -> handler.onFailure(FAILED_OFFLINE));
                    }
                });
            }
        });
    }
    protected void searchFromCache(final Context context, final String type, String cache, final String query, final SearchFromCache searchFromCache) {
        Log.v(TAG, "searchFromCache | type=", type, " | query=", query, " | cache=", (cache == null || cache.isEmpty() ? "<empty>" : "<string>"));
        JSONObject cacheJson = null;
        if (cache == null) {
            cache = getCache(context, query);
        }
        if (cache != null && !cache.isEmpty()) {
            try {
                cacheJson = new JSONObject(cache);
            } catch (Exception e) {
                Log.v(TAG, "searchFromCache | type=", type, " | got invalid cache, going to remove it...");
                removeCache(context, query);
                cacheJson = null;
            }
        }
        if (cacheJson == null) {
            Log.v(TAG, "searchFromCache | type=", type, " | empty cache");
            searchFromCache.onEmpty(query);
        } else {
            searchFromCache.onDone(query, cacheJson);
        }
    }
    // --<- Search schedule -<--

    // -->- Cache schedule ->--
    private static final Map<String, String> localCache = new HashMap<>();
    private static final int LOCAL_CACHE_THRESHOLD = 3;
    protected String getCache(Context context, String token) {
        token = token.toLowerCase();
        Log.v(TAG, "getCache | token=", token);
        String cache = null;
        synchronized (Schedule.localCache) {
            final String memoizeKey = getType() + "_" + token;
            if (Schedule.localCache.containsKey(memoizeKey)) {
                cache = Schedule.localCache.get(memoizeKey);
            }
        }
        if (cache == null || cache.isEmpty()) {
            cache = Storage.file.general.cache.get(context, "schedule_" + getType() + "#lessons#" + token, "");
        }
        return cache;
    }
    protected void putCache(Context context, String token, String value, boolean forceToCache) {
        if (value != null && !value.isEmpty()) {
            Log.v(TAG, "putCache | token=", token, " | forceToCache=", forceToCache);
            token = token.toLowerCase();
            putLocalCache(token, value);
            if (forceToCache || token.equals(getDefaultScope(context).toLowerCase()) || Storage.file.general.cache.exists(context, "schedule_" + getType() + "#lessons#" + token)) {
                Log.v(TAG, "putCache | token=", token, " | proceed");
                Storage.file.general.cache.put(context, "schedule_" + getType() + "#lessons#" + token, value);
            }
        }
    }
    protected void putLocalCache(String token, String value) {
        if (value != null && !value.isEmpty()) {
            Log.v(TAG, "putLocalCache | token=", token);
            token = token.toLowerCase();
            synchronized (localCache) {
                if (localCache.size() > LOCAL_CACHE_THRESHOLD - 1) {
                    Set<String> keySet = localCache.keySet();
                    int i = 0;
                    for (String key : keySet) {
                        if (i++ < LOCAL_CACHE_THRESHOLD) continue;
                        localCache.remove(key);
                    }
                }
                localCache.put(getType() + "_" + token, value);
            }
        }
    }
    protected void removeCache(Context context, String token) {
        token = token.toLowerCase();
        Log.v(TAG, "removeCache | token=", token);
        synchronized (localCache) {
            final String memoizeKey = getType() + "_" + token;
            if (localCache.containsKey(memoizeKey)) {
                localCache.remove(memoizeKey);
            }
        }
        Storage.file.general.cache.delete(context, "schedule_" + getType() + "#lessons#" + token);
    }
    // --<- Cache schedule -<--

    // Defines the source of the schedule
    protected @Source String getSource(Context context) {
        String token = "pref_schedule_" + getType() + "_source";
        String source = Storage.pref.get(context, token, getDefaultSource());
        switch (source) {
            case "ifmo": case "isu": break;
            default: {
                source = getDefaultSource();
                Storage.pref.put(context, token, source);
            }
        }
        return source;
    }

    // Returns template for all schedules
    protected JSONObject getTemplate(final String query, final String type) {
        try {
            JSONObject template = new JSONObject();
            // Общий тип расписания: "lessons", "exams", "attestations"
            template.put("schedule_type", getType());
            // Строка, по которой идет поиск: "mine", "K3320", "123456", "336", "Зинчик" (Используется для повторного поиска и кэширования)
            template.put("query", query);
            // Тип расписания: "mine", "group", "teacher", "room", "teachers"
            template.put("type", type);
            // Заголовок расписания: "K3320", "336", "Зинчик Александр Адольфович"
            template.put("title", "");
            // Текущее время
            template.put("timestamp", Static.getCalendar().getTimeInMillis());
            // Расписание собственной персоной
            template.put("schedule", new JSONArray());
            return template;
        } catch (JSONException e) {
            Static.error(e);
            return null;
        }
    }

    // Returns the number of hours after which the schedule is considered to be overdue
    protected int getRefreshRate(final Context context) {
        Log.v(TAG, "getRefreshRate");
        try {
            return Storage.pref.get(context, "pref_use_cache", true) ? Integer.parseInt(Storage.pref.get(context, "pref_static_refresh", "168")) : 0;
        } catch (Exception e) {
            return 0;
        }
    }

    // Returns a flag indicating that the schedule has expired and needs to be updated
    protected boolean isForceRefresh(final String cache, final int refreshRate) {
        Log.v(TAG, "isForceUpdate | cache=", (cache == null || cache.isEmpty() ? "<empty>" : "<string>"), " | refreshRate=", refreshRate);
        if (refreshRate == 0 || cache == null || cache.isEmpty()) {
            return true;
        } else if (refreshRate > 0) {
            try {
                return new JSONObject(cache).getLong("timestamp") + refreshRate * 3600000L < Static.getCalendar().getTimeInMillis();
            } catch (JSONException e) {
                return true;
            }
        } else {
            return false;
        }
    }

    // Returns the default query string for a schedule search
    public String getDefaultScope(final Context context) {
        return getDefaultScope(context, getType());
    }
    public static String getDefaultScope(final Context context, final String type) {
        Log.v(TAG, "getDefaultScope | type=", type);
        final String pref = Storage.pref.get(context, "pref_schedule_" + type + "_default", "").trim();
        String scope;
        if (pref.isEmpty()) {
            scope = "auto";
        } else {
            try {
                scope = (new JSONObject(pref)).getString("query");
            } catch (Exception e) {
                scope = "auto";
            }
        }
        switch (scope) {
            case "auto": return Storage.file.perm.get(context, "user#group");
            case "mine":
            default: return scope;
        }
    }

    // Returns the signature of the lesson
    public static String getLessonSignature(JSONObject lesson) throws JSONException {
        JSONObject replica = new JSONObject();
        replica.put("subject", lesson.getString("subject"));
        replica.put("type", lesson.getString("type"));
        replica.put("week", lesson.getInt("week"));
        replica.put("timeStart", lesson.getString("timeStart"));
        replica.put("timeEnd", lesson.getString("timeEnd"));
        replica.put("group", lesson.getString("group"));
        replica.put("teacher", lesson.getString("teacher"));
        replica.put("teacher_id", lesson.getString("teacher_id"));
        replica.put("room", lesson.getString("room"));
        replica.put("building", lesson.getString("building"));
        return replica.toString();
    }

    // Returns the hash of the lesson
    public static String getLessonHash(JSONObject lesson) throws JSONException {
        return Static.crypt(getLessonSignature(lesson));
    }

    // Returns main title for schedules
    public static String getScheduleHeader(Context context, String title, String type) {
        switch (type) {
            case "mine": title = context.getString(R.string.schedule_personal); break;
            case "group": title = (context.getString(R.string.schedule_group) + " " + title).trim(); break;
            case "teacher": title = (context.getString(R.string.schedule_teacher) + " " + title).trim(); break;
            case "room": title = (context.getString(R.string.schedule_room) + " " + title).trim(); break;
            case "teachers": break;
        }
        return title;
    }

    // Returns second title for schedules
    public static String getScheduleWeek(Context context, int week) {
        if (week >= 0) {
            return week + " " + context.getString(R.string.school_week);
        } else {
            String pattern = "dd.MM.yyyy";
            String date = new SimpleDateFormat(pattern, Locale.ROOT).format(new Date(Static.getCalendar().getTimeInMillis()));
            try {
                return Static.cuteDateWithoutTime(context, pattern, date);
            } catch (ParseException e) {
                return date;
            }
        }
    }
}

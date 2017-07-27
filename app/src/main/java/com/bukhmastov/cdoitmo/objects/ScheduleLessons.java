package com.bukhmastov.cdoitmo.objects;

import android.content.Context;
import android.os.Bundle;
import android.support.annotation.Nullable;
import android.support.v4.widget.SwipeRefreshLayout;

import com.bukhmastov.cdoitmo.R;
import com.bukhmastov.cdoitmo.activities.ConnectedActivity;
import com.bukhmastov.cdoitmo.converters.ScheduleLessonsAdditionalConverter;
import com.bukhmastov.cdoitmo.converters.ScheduleLessonsConverter;
import com.bukhmastov.cdoitmo.firebase.FirebaseAnalyticsProvider;
import com.bukhmastov.cdoitmo.fragments.ScheduleLessonsFragment;
import com.bukhmastov.cdoitmo.fragments.ScheduleLessonsModifyFragment;
import com.bukhmastov.cdoitmo.network.IfmoRestClient;
import com.bukhmastov.cdoitmo.network.interfaces.IfmoRestClientResponseHandler;
import com.bukhmastov.cdoitmo.objects.entities.LessonUnit;
import com.bukhmastov.cdoitmo.utils.Log;
import com.bukhmastov.cdoitmo.utils.Static;
import com.bukhmastov.cdoitmo.utils.Storage;
import com.loopj.android.http.RequestHandle;

import org.json.JSONArray;
import org.json.JSONException;
import org.json.JSONObject;

import java.util.Calendar;
import java.util.Objects;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

public class ScheduleLessons implements SwipeRefreshLayout.OnRefreshListener {

    private static final String TAG = "ScheduleLessons";
    public interface response {
        void onProgress(int state);
        void onFailure(int state);
        void onSuccess(JSONObject json);
        void onNewHandle(RequestHandle requestHandle);
    }
    private ScheduleLessons.response handler = null;
    private Context context;
    public static final int FAILED_LOAD = 100;
    public static final int FAILED_OFFLINE = 101;

    public ScheduleLessons(Context context) {
        this.context = context;
    }

    @Override
    public void onRefresh() {
        Log.v(TAG, "refreshed");
        search(ScheduleLessonsFragment.query, 0);
    }

    public void setHandler(ScheduleLessons.response handler) {
        Log.v(TAG, "handler set");
        this.handler = handler;
    }

    public void search(final String query) {
        Static.T.runThread(new Runnable() {
            @Override
            public void run() {
                search(query, getRefreshRate());
            }
        });
    }
    public void search(final String query, final int refresh_rate) {
        Static.T.runThread(new Runnable() {
            @Override
            public void run() {
                search(query, refresh_rate, Storage.pref.get(context, "pref_schedule_lessons_use_cache", false));
            }
        });
    }
    public void search(final String query, final boolean toCache) {
        Static.T.runThread(new Runnable() {
            @Override
            public void run() {
                search(query, getRefreshRate(), toCache);
            }
        });
    }
    public void search(final String query, final int refresh_rate, final boolean toCache) {
        Static.T.runThread(new Runnable() {
            @Override
            public void run() {
                search(query, refresh_rate, toCache, true);
            }
        });
    }
    public void search(final String query, final int refresh_rate, final boolean toCache, final boolean additionalConversion) {
        Static.T.runThread(new Runnable() {
            @Override
            public void run() {
                Log.v(TAG, "search | query=" + query + " | refresh_rate=" + refresh_rate + " | toCache=" + (toCache ? "true" : "false") + " | additionalConversion=" + (additionalConversion ? "true" : "false"));
                if (handler == null) return;
                String q = query.trim();
                if (ScheduleLessonsFragment.fragmentRequestHandle != null) ScheduleLessonsFragment.fragmentRequestHandle.cancel(true);
                Matcher matcherGroup = Pattern.compile("^([a-zA-Z]{1,3})(\\d+[a-zA-Z]?)$").matcher(q);
                if (matcherGroup.find()) {
                    searchGroup(matcherGroup.group(1).toUpperCase() + matcherGroup.group(2), refresh_rate, toCache, additionalConversion);
                } else if (Pattern.compile("^[0-9]{6}$").matcher(q).find()) {
                    searchDefinedTeacher(q, refresh_rate, toCache, additionalConversion);
                } else if (Pattern.compile("^[0-9].*$").matcher(q).find()) {
                    searchRoom(q, refresh_rate, toCache, additionalConversion);
                } else {
                    searchTeacher(q.split(" ")[0].trim(), refresh_rate, toCache);
                }
            }
        });
    }

    private void searchGroup(final String group, final int refresh_rate, final boolean toCache, final boolean additionalConversion) {
        Static.T.runThread(new Runnable() {
            @Override
            public void run() {
                Log.v(TAG, "searchGroup | group=" + group + " | refresh_rate=" + refresh_rate + " | toCache=" + (toCache ? "true" : "false") + " | additionalConversion=" + (additionalConversion ? "true" : "false"));
                final String cache_token = "group_" + group;
                final String cache = getCache(cache_token);
                if (getForce(cache, refresh_rate) && !Static.OFFLINE_MODE) {
                    IfmoRestClient.get(context, "schedule_lesson_group/" + group, null, new IfmoRestClientResponseHandler() {
                        @Override
                        public void onSuccess(int statusCode, JSONObject responseObj, JSONArray responseArr) {
                            if (statusCode == 200 && responseObj != null) {
                                JSONObject template = getTemplate("group", cache_token, responseObj);
                                if (template == null) {
                                    handler.onFailure(FAILED_LOAD);
                                    return;
                                }
                                new ScheduleLessonsConverter(new ScheduleLessonsConverter.response() {
                                    @Override
                                    public void finish(JSONObject json) {
                                        try {
                                            if (json.getJSONArray("schedule").length() > 0) putCache(cache_token, json.toString(), toCache);
                                            if (additionalConversion) {
                                                new ScheduleLessonsAdditionalConverter(context, new ScheduleLessonsAdditionalConverter.response() {
                                                    @Override
                                                    public void finish(JSONObject json) {
                                                        handler.onSuccess(json);
                                                    }
                                                }).execute(json);
                                            } else {
                                                handler.onSuccess(json);
                                            }
                                        } catch (Exception e) {
                                            Static.error(e);
                                            handler.onSuccess(json);
                                        }
                                    }
                                }).execute(responseObj, template);
                            } else {
                                if (Objects.equals(cache, "")) {
                                    handler.onFailure(FAILED_LOAD);
                                } else {
                                    try {
                                        handler.onSuccess(new JSONObject(cache));
                                    } catch (JSONException e) {
                                        Static.error(e);
                                        handler.onFailure(FAILED_LOAD);
                                    }
                                }
                            }
                        }
                        @Override
                        public void onProgress(int state) {
                            handler.onProgress(state);
                        }
                        @Override
                        public void onFailure(int statusCode, int state) {
                            handler.onFailure(state);
                        }
                        @Override
                        public void onNewHandle(RequestHandle requestHandle) {
                            handler.onNewHandle(requestHandle);
                        }
                    });
                } else if (Static.OFFLINE_MODE && Objects.equals(cache, "")) {
                    Log.v(TAG, "searchGroup | offline");
                    handler.onFailure(FAILED_OFFLINE);
                } else {
                    try {
                        Log.v(TAG, "searchGroup | from cache");
                        if (additionalConversion) {
                            new ScheduleLessonsAdditionalConverter(context, new ScheduleLessonsAdditionalConverter.response() {
                                @Override
                                public void finish(JSONObject json) {
                                    handler.onSuccess(json);
                                }
                            }).execute(new JSONObject(cache));
                        } else {
                            handler.onSuccess(new JSONObject(cache));
                        }
                    } catch (JSONException e) {
                        Static.error(e);
                        handler.onFailure(FAILED_LOAD);
                    }
                }
            }
        });
    }
    private void searchRoom(final String room, final int refresh_rate, final boolean toCache, final boolean additionalConversion) {
        Static.T.runThread(new Runnable() {
            @Override
            public void run() {
                Log.v(TAG, "searchRoom | room=" + room + " | refresh_rate=" + refresh_rate + " | toCache=" + (toCache ? "true" : "false") + " | additionalConversion=" + (additionalConversion ? "true" : "false"));
                final String cache_token = "room_" + room;
                final String cache = getCache(cache_token);
                if (getForce(cache, refresh_rate) && !Static.OFFLINE_MODE) {
                    IfmoRestClient.get(context, "schedule_lesson_room/" + room, null, new IfmoRestClientResponseHandler() {
                        @Override
                        public void onSuccess(int statusCode, JSONObject responseObj, JSONArray responseArr) {
                            if (statusCode == 200 && responseObj != null) {
                                JSONObject template = getTemplate("room", cache_token, responseObj);
                                if (template == null) {
                                    handler.onFailure(FAILED_LOAD);
                                    return;
                                }
                                new ScheduleLessonsConverter(new ScheduleLessonsConverter.response() {
                                    @Override
                                    public void finish(JSONObject json) {
                                        try {
                                            if (json.getJSONArray("schedule").length() > 0) putCache(cache_token, json.toString(), toCache);
                                            if (additionalConversion) {
                                                new ScheduleLessonsAdditionalConverter(context, new ScheduleLessonsAdditionalConverter.response() {
                                                    @Override
                                                    public void finish(JSONObject json) {
                                                        handler.onSuccess(json);
                                                    }
                                                }).execute(json);
                                            } else {
                                                handler.onSuccess(json);
                                            }
                                        } catch (Exception e) {
                                            Static.error(e);
                                            handler.onSuccess(json);
                                        }
                                    }
                                }).execute(responseObj, template);
                            } else {
                                if (Objects.equals(cache, "")) {
                                    handler.onFailure(FAILED_LOAD);
                                } else {
                                    try {
                                        handler.onSuccess(new JSONObject(cache));
                                    } catch (JSONException e) {
                                        Static.error(e);
                                        handler.onFailure(FAILED_LOAD);
                                    }
                                }
                            }
                        }
                        @Override
                        public void onProgress(int state) {
                            handler.onProgress(state);
                        }
                        @Override
                        public void onFailure(int statusCode, int state) {
                            handler.onFailure(state);
                        }
                        @Override
                        public void onNewHandle(RequestHandle requestHandle) {
                            handler.onNewHandle(requestHandle);
                        }
                    });
                } else if (Static.OFFLINE_MODE && Objects.equals(cache, "")) {
                    Log.v(TAG, "searchRoom | offline");
                    handler.onFailure(FAILED_OFFLINE);
                } else {
                    try {
                        Log.v(TAG, "searchRoom | from cache");
                        if (additionalConversion) {
                            new ScheduleLessonsAdditionalConverter(context, new ScheduleLessonsAdditionalConverter.response() {
                                @Override
                                public void finish(JSONObject json) {
                                    handler.onSuccess(json);
                                }
                            }).execute(new JSONObject(cache));
                        } else {
                            handler.onSuccess(new JSONObject(cache));
                        }
                    } catch (JSONException e) {
                        Static.error(e);
                        handler.onFailure(FAILED_LOAD);
                    }
                }
            }
        });
    }
    private void searchDefinedTeacher(final String teacherId, final int refresh_rate, final boolean toCache, final boolean additionalConversion) {
        Static.T.runThread(new Runnable() {
            @Override
            public void run() {
                Log.v(TAG, "searchDefinedTeacher | teacherId=" + teacherId + " | refresh_rate=" + refresh_rate + " | toCache=" + (toCache ? "true" : "false") + " | additionalConversion=" + (additionalConversion ? "true" : "false"));
                final String cache_token = teacherId;
                final String cache = getCache(cache_token);
                if(getForce(cache, refresh_rate) && !Static.OFFLINE_MODE) {
                    IfmoRestClient.get(context, "schedule_lesson_person/" + teacherId, null, new IfmoRestClientResponseHandler() {
                        @Override
                        public void onSuccess(int statusCode, JSONObject responseObj, JSONArray responseArr) {
                            if (statusCode == 200 && responseObj != null) {
                                JSONObject template = getTemplate("teacher", cache_token, responseObj);
                                if (template == null) {
                                    handler.onFailure(FAILED_LOAD);
                                    return;
                                }
                                new ScheduleLessonsConverter(new ScheduleLessonsConverter.response() {
                                    @Override
                                    public void finish(JSONObject json) {
                                        try {
                                            if (json.getJSONArray("schedule").length() > 0) putCache(cache_token, json.toString(), toCache);
                                            if (additionalConversion) {
                                                new ScheduleLessonsAdditionalConverter(context, new ScheduleLessonsAdditionalConverter.response() {
                                                    @Override
                                                    public void finish(JSONObject json) {
                                                        handler.onSuccess(json);
                                                    }
                                                }).execute(json);
                                            } else {
                                                handler.onSuccess(json);
                                            }
                                        } catch (Exception e) {
                                            Static.error(e);
                                            handler.onSuccess(json);
                                        }
                                    }
                                }).execute(responseObj, template);
                            } else {
                                if (Objects.equals(cache, "")) {
                                    handler.onFailure(FAILED_LOAD);
                                } else {
                                    try {
                                        handler.onSuccess(new JSONObject(cache));
                                    } catch (JSONException e) {
                                        Static.error(e);
                                        handler.onFailure(FAILED_LOAD);
                                    }
                                }
                            }
                        }
                        @Override
                        public void onProgress(int state) {
                            handler.onProgress(state);
                        }
                        @Override
                        public void onFailure(int statusCode, int state) {
                            handler.onFailure(state);
                        }
                        @Override
                        public void onNewHandle(RequestHandle requestHandle) {
                            handler.onNewHandle(requestHandle);
                        }
                    });
                } else if (Static.OFFLINE_MODE && Objects.equals(cache, "")) {
                    Log.v(TAG, "searchDefinedTeacher | offline");
                    handler.onFailure(FAILED_OFFLINE);
                } else {
                    try {
                        Log.v(TAG, "searchDefinedTeacher | from cache");
                        if (additionalConversion) {
                            new ScheduleLessonsAdditionalConverter(context, new ScheduleLessonsAdditionalConverter.response() {
                                @Override
                                public void finish(JSONObject json) {
                                    handler.onSuccess(json);
                                }
                            }).execute(new JSONObject(cache));
                        } else {
                            handler.onSuccess(new JSONObject(cache));
                        }
                    } catch (JSONException e) {
                        Static.error(e);
                        handler.onFailure(FAILED_LOAD);
                    }
                }
            }
        });
    }
    private void searchTeacher(final String teacher, final int refresh_rate, final boolean toCache) {
        Static.T.runThread(new Runnable() {
            @Override
            public void run() {
                Log.v(TAG, "searchTeacher | teacher=" + teacher + " | refresh_rate=" + refresh_rate + " | toCache=" + (toCache ? "true" : "false"));
                final String cache_token = "teacher_picker_" + teacher;
                final String cache = getCache(cache_token);
                if (getForce(cache, refresh_rate) && !Static.OFFLINE_MODE) {
                    IfmoRestClient.get(context, "schedule_person?lastname=" + teacher, null, new IfmoRestClientResponseHandler() {
                        @Override
                        public void onSuccess(int statusCode, JSONObject responseObj, JSONArray responseArr) {
                            if (statusCode == 200 && responseObj != null) {
                                try {
                                    responseObj.put("query", teacher);
                                    responseObj.put("type", "teacher_picker");
                                    responseObj.put("timestamp", Calendar.getInstance().getTimeInMillis());
                                    responseObj.put("cache_token", cache_token);
                                    if (responseObj.getJSONArray("list").length() > 0) putCache(cache_token, responseObj.toString(), toCache);
                                    handler.onSuccess(responseObj);
                                } catch (JSONException e) {
                                    Static.error(e);
                                    handler.onFailure(FAILED_LOAD);
                                }
                            } else {
                                if (Objects.equals(cache, "")) {
                                    handler.onFailure(FAILED_LOAD);
                                } else {
                                    try {
                                        handler.onSuccess(new JSONObject(cache));
                                    } catch (JSONException e) {
                                        Static.error(e);
                                        handler.onFailure(FAILED_LOAD);
                                    }
                                }
                            }
                        }
                        @Override
                        public void onProgress(int state) {
                            handler.onProgress(state);
                        }
                        @Override
                        public void onFailure(int statusCode, int state) {
                            handler.onFailure(state);
                        }
                        @Override
                        public void onNewHandle(RequestHandle requestHandle) {
                            handler.onNewHandle(requestHandle);
                        }
                    });
                } else if (Static.OFFLINE_MODE && Objects.equals(cache, "")) {
                    Log.v(TAG, "searchTeacher | offline");
                    handler.onFailure(FAILED_OFFLINE);
                } else {
                    try {
                        Log.v(TAG, "searchTeacher | from cache");
                        JSONObject list = new JSONObject(cache);
                        if (list.getJSONArray("teachers").length() == 1) {
                            search(list.getJSONArray("teachers").getJSONObject(0).getString("scope"), refresh_rate, toCache);
                        } else {
                            handler.onSuccess(list);
                        }
                    } catch (JSONException e) {
                        Static.error(e);
                        handler.onFailure(FAILED_LOAD);
                    }
                }
            }
        });
    }

    @Nullable
    private JSONObject getTemplate(String type, String cache_token, JSONObject remote) {
        try {
            JSONObject template = new JSONObject();
            template.put("query", remote.get("query").toString());
            template.put("type", type);
            template.put("title", remote.getString("type_title"));
            template.put("label", remote.getString("label"));
            template.put("pid", Objects.equals(type, "teacher") ? String.valueOf(remote.getInt("pid")) : null);
            template.put("timestamp", Calendar.getInstance().getTimeInMillis());
            template.put("cache_token", cache_token);
            template.put("schedule", new JSONArray());
            return template;
        } catch (JSONException e) {
            Static.error(e);
            return null;
        }
    }
    private int getRefreshRate() {
        return Storage.pref.get(context, "pref_use_cache", true) ? Integer.parseInt(Storage.pref.get(context, "pref_static_refresh", "168")) : 0;
    }
    private boolean getForce(String cache, int refresh_rate) {
        Log.v(TAG, "getForce | refresh_rate=" + refresh_rate);
        boolean force;
        if (Objects.equals(cache, "") || refresh_rate == 0) {
            force = true;
        } else if (refresh_rate >= 0){
            try {
                force = new JSONObject(cache).getLong("timestamp") + refresh_rate * 3600000L < Calendar.getInstance().getTimeInMillis();
            } catch (JSONException e) {
                Static.error(e);
                force = true;
            }
        } else {
            force = false;
        }
        return force;
    }
    public String getDefault() {
        Log.v(TAG, "getDefault");
        String scope;
        String pref = Storage.pref.get(context, "pref_schedule_lessons_default", "");
        try {
            if (Objects.equals(pref, "")) throw new Exception("pref_schedule_lessons_default empty");
            JSONObject jsonObject = new JSONObject(pref);
            scope = jsonObject.getString("query");
            if (Objects.equals(scope, "auto")) throw new Exception("pref_schedule_lessons_default query=auto");
        } catch (Exception e) {
            scope = Storage.file.perm.get(context, "user#group");
        }
        return scope;
    }

    private void putCache(String token, String value, boolean toCache) {
        Log.v(TAG, "putCache | token=" + token);
        String def = getDefault();
        if (toCache || hasCache(token) || Objects.equals(def, token) || Objects.equals("group_" + def, token) || Objects.equals("room_" + def, token) || Objects.equals("teacher_picker_" + def, token)) {
            Storage.file.cache.put(context, "schedule_lessons#lessons#" + token, value);
        }
    }
    private boolean hasCache(String token) {
        Log.v(TAG, "hasCache | token=" + token);
        return Storage.file.cache.exists(context, "schedule_lessons#lessons#" + token);
    }
    public String getCache(String token) {
        Log.v(TAG, "getCache | token=" + token);
        return Storage.file.cache.get(context, "schedule_lessons#lessons#" + token);
    }
    private void removeCache(String token) {
        Log.v(TAG, "removeCache | token=" + token);
        Storage.file.cache.delete(context, "schedule_lessons#lessons#" + token);
    }
    public Boolean toggleCache() {
        Log.v(TAG, "toggleCache");
        try {
            String token = ScheduleLessonsFragment.schedule.getString("cache_token");
            if (Objects.equals(getCache(token), "")) {
                putCache(token, ScheduleLessonsFragment.schedule.toString(), true);
                ScheduleLessonsFragment.schedule_cached = true;
                return true;
            } else {
                removeCache(token);
                ScheduleLessonsFragment.schedule_cached = false;
                return false;
            }
        } catch (JSONException e) {
            Static.error(e);
            return null;
        }
    }

    public static void reduceLesson(final Context context, final String cache_token, final int index, final JSONObject lesson) {
        Static.T.runThread(new Runnable() {
            @Override
            public void run() {
                Log.v(TAG, "reduceLesson | cache_token=" + cache_token);
                try {
                    if (!Objects.equals(lesson.getString("cdoitmo_type"), "normal")) throw new Exception("Wrong cdoitmo_type type");
                    String hash = Static.crypt(ScheduleLessons.getCast(lesson));
                    String reducedStr = Storage.file.perm.get(context, "schedule_lessons#reduced#" + cache_token, "");
                    JSONArray reduced;
                    if (reducedStr.isEmpty()) {
                        reduced = new JSONArray();
                    } else {
                        reduced = new JSONArray(reducedStr);
                    }
                    boolean found = false;
                    for (int i = 0; i < reduced.length(); i++) {
                        JSONObject day = reduced.getJSONObject(i);
                        if (day.getInt("index") == index) {
                            JSONArray lessons = day.getJSONArray("lessons");
                            boolean foundLesson = false;
                            for (int j = 0; j < lessons.length(); j++) {
                                if (Objects.equals(hash, lessons.getString(j))) {
                                    foundLesson = true;
                                    break;
                                }
                            }
                            if (!foundLesson) lessons.put(hash);
                            found = true;
                            break;
                        }
                    }
                    if (!found) {
                        JSONObject day = new JSONObject();
                        JSONArray lessons = new JSONArray();
                        lessons.put(hash);
                        day.put("index", index);
                        day.put("lessons", lessons);
                        reduced.put(day);
                    }
                    Storage.file.perm.put(context, "schedule_lessons#reduced#" + cache_token, reduced.toString());
                    ScheduleLessonsFragment.reSchedule(context);
                    FirebaseAnalyticsProvider.logEvent(
                            context,
                            FirebaseAnalyticsProvider.Event.SCHEDULE_LESSON_REDUCE,
                            FirebaseAnalyticsProvider.getBundle(FirebaseAnalyticsProvider.Param.LESSON_TITLE, lesson.getString("subject"))
                    );
                } catch (Exception e) {
                    Static.error(e);
                    Static.toast(context, context.getString(R.string.something_went_wrong));
                }
            }
        });
    }
    public static void restoreLesson(final Context context, final String cache_token, final int index, final JSONObject lesson) {
        Static.T.runThread(new Runnable() {
            @Override
            public void run() {
                Log.v(TAG, "restoreLesson | cache_token=" + cache_token);
                try {
                    if (!Objects.equals(lesson.getString("cdoitmo_type"), "reduced")) throw new Exception("Wrong cdoitmo_type type");
                    String hash = Static.crypt(ScheduleLessons.getCast(lesson));
                    String reducedStr = Storage.file.perm.get(context, "schedule_lessons#reduced#" + cache_token, "");
                    JSONArray reduced;
                    if (reducedStr.isEmpty()) {
                        reduced = new JSONArray();
                    } else {
                        reduced = new JSONArray(reducedStr);
                    }
                    for (int i = 0; i < reduced.length(); i++) {
                        JSONObject day = reduced.getJSONObject(i);
                        if (day.getInt("index") == index) {
                            JSONArray lessons = day.getJSONArray("lessons");
                            for (int j = 0; j < lessons.length(); j++) {
                                if (Objects.equals(hash, lessons.getString(j))) {
                                    lessons.remove(j);
                                    break;
                                }
                            }
                            if (lessons.length() == 0) {
                                reduced.remove(i);
                            }
                            break;
                        }
                    }
                    if (reduced.length() == 0) {
                        Storage.file.perm.delete(context, "schedule_lessons#reduced#" + cache_token);
                    } else {
                        Storage.file.perm.put(context, "schedule_lessons#reduced#" + cache_token, reduced.toString());
                    }
                    ScheduleLessonsFragment.reSchedule(context);
                } catch (Exception e) {
                    Static.error(e);
                    Static.toast(context, context.getString(R.string.something_went_wrong));
                }
            }
        });
    }
    public static void createLesson(final ConnectedActivity activity, final JSONObject schedule, final int dayIndex, final int week) {
        Static.T.runThread(new Runnable() {
            @Override
            public void run() {
                try {
                    Log.v(TAG, "createLesson | cache_token=" + schedule.getString("cache_token"));
                    Bundle extras = new Bundle();
                    extras.putSerializable("action_type", ScheduleLessonsModifyFragment.TYPE.create);
                    extras.putString("header", schedule.getString("title") + " " + schedule.getString("label"));
                    extras.putString("cache_token", schedule.getString("cache_token"));
                    extras.putInt("day", dayIndex);
                    extras.putInt("week", week);
                    switch (schedule.getString("type")) {
                        case "group":
                            extras.putString("group", schedule.getString("label"));
                            break;
                        case "teacher":
                            extras.putString("teacher", schedule.getString("label"));
                            extras.putString("teacher_id", schedule.getString("pid"));
                            break;
                        case "room":
                            extras.putString("room", schedule.getString("label"));
                            break;
                    }
                    activity.openActivityOrFragment(ScheduleLessonsModifyFragment.class, extras);
                } catch (Exception e) {
                    Static.error(e);
                    Static.snackBar(activity, activity.getString(R.string.something_went_wrong));
                }
            }
        });
    }
    public static void createLesson(final ConnectedActivity activity, final JSONObject schedule, final JSONObject lesson, final int dayIndex, final int week) {
        Static.T.runThread(new Runnable() {
            @Override
            public void run() {
                try {
                    Log.v(TAG, "createLesson(JSONObject) | cache_token=" + schedule.getString("cache_token"));
                    Bundle extras = new Bundle();
                    extras.putSerializable("action_type", ScheduleLessonsModifyFragment.TYPE.create);
                    extras.putString("header", schedule.getString("title") + " " + schedule.getString("label"));
                    extras.putString("cache_token", schedule.getString("cache_token"));
                    extras.putInt("day", dayIndex);
                    extras.putInt("week", week);
                    if (lesson.getString("subject") != null) extras.putString("title", lesson.getString("subject"));
                    if (lesson.getString("timeStart") != null) extras.putString("timeStart", lesson.getString("timeStart"));
                    if (lesson.getString("timeEnd") != null) extras.putString("timeEnd", lesson.getString("timeEnd"));
                    if (lesson.getString("type") != null) extras.putString("type", lesson.getString("type"));
                    if (lesson.getString("group") != null) extras.putString("group", lesson.getString("group"));
                    if (lesson.getString("teacher") != null) extras.putString("teacher", lesson.getString("teacher"));
                    if (lesson.getString("teacher_id") != null) extras.putString("teacher_id", lesson.getString("teacher_id"));
                    if (lesson.getString("room") != null) extras.putString("room", lesson.getString("room"));
                    if (lesson.getString("building") != null) extras.putString("building", lesson.getString("building"));
                    activity.openActivityOrFragment(ScheduleLessonsModifyFragment.class, extras);
                } catch (Exception e) {
                    Static.error(e);
                    Static.snackBar(activity, activity.getString(R.string.something_went_wrong));
                }
            }
        });
    }
    public static boolean createLesson(Context context, LessonUnit lessonUnit) {
        Log.v(TAG, "createLesson(lessonUnit) | cache_token=" + lessonUnit.cache_token);
        boolean result = true;
        try {
            JSONObject lesson = new JSONObject();
            lesson.put("subject", getLessonField(lessonUnit.title, ""));
            lesson.put("week", lessonUnit.week);
            lesson.put("timeStart", getLessonField(lessonUnit.timeStart, "∞"));
            lesson.put("timeEnd", getLessonField(lessonUnit.timeEnd, "∞"));
            lesson.put("group", getLessonField(lessonUnit.group, ""));
            lesson.put("teacher", getLessonField(lessonUnit.teacher, ""));
            lesson.put("teacher_id", getLessonField(lessonUnit.teacher_id, ""));
            lesson.put("room", getLessonField(lessonUnit.room, ""));
            lesson.put("building", getLessonField(lessonUnit.building, ""));
            lesson.put("type", getLessonField(lessonUnit.type, ""));
            lesson.put("cdoitmo_type", "synthetic");
            String addedStr = Storage.file.perm.get(context, "schedule_lessons#added#" + lessonUnit.cache_token, "");
            JSONArray added;
            if (addedStr.isEmpty()) {
                added = new JSONArray();
            } else {
                added = new JSONArray(addedStr);
            }
            boolean found = false;
            for (int i = 0; i < added.length(); i++) {
                JSONObject day = added.getJSONObject(i);
                if (day.getInt("index") == lessonUnit.day) {
                    found = true;
                    day.getJSONArray("lessons").put(lesson);
                }
            }
            if (!found) {
                JSONObject day = new JSONObject();
                JSONArray lessons = new JSONArray();
                lessons.put(lesson);
                day.put("index", lessonUnit.day);
                day.put("lessons", lessons);
                added.put(day);
            }
            Storage.file.perm.put(context, "schedule_lessons#added#" + lessonUnit.cache_token, added.toString());
            FirebaseAnalyticsProvider.logEvent(
                    context,
                    FirebaseAnalyticsProvider.Event.SCHEDULE_LESSON_ADD,
                    FirebaseAnalyticsProvider.getBundle(FirebaseAnalyticsProvider.Param.LESSON_TITLE, lessonUnit.title)
            );
        } catch (Exception e) {
            Static.error(e);
            result = false;
        }
        return result;
    }
    public static void deleteLesson(final Context context, final String cache_token, final int index, final JSONObject lesson) {
        Static.T.runThread(new Runnable() {
            @Override
            public void run() {
                Log.v(TAG, "deleteLesson | cache_token=" + cache_token);
                try {
                    if (!Objects.equals(lesson.getString("cdoitmo_type"), "synthetic")) throw new Exception("Wrong cdoitmo_type type");
                    if (!deleteLesson(context, cache_token, index, Static.crypt(lesson.toString()))) throw new Exception("Failed to delete lesson");
                    ScheduleLessonsFragment.reSchedule(context);
                } catch (Exception e) {
                    Static.error(e);
                    Static.toast(context, context.getString(R.string.something_went_wrong));
                }
            }
        });
    }
    public static boolean deleteLesson(Context context, String cache_token, int index, String hash) {
        Log.v(TAG, "deleteLesson(hash) | cache_token=" + cache_token);
        try {
            String addedStr = Storage.file.perm.get(context, "schedule_lessons#added#" + cache_token, "");
            JSONArray added;
            if (addedStr.isEmpty()) {
                added = new JSONArray();
            } else {
                added = new JSONArray(addedStr);
            }
            // remove
            for (int i = 0; i < added.length(); i++) {
                JSONObject day = added.getJSONObject(i);
                if (day.getInt("index") == index) {
                    JSONArray lessons = day.getJSONArray("lessons");
                    for (int j = 0; j < lessons.length(); j++) {
                        if (Objects.equals(hash, Static.crypt(lessons.getJSONObject(j).toString()))) {
                            lessons.remove(j);
                            break;
                        }
                    }
                    if (lessons.length() == 0) {
                        added.remove(i);
                    }
                    break;
                }
            }
            if (added.length() == 0) {
                Storage.file.perm.delete(context, "schedule_lessons#added#" + cache_token);
            } else {
                Storage.file.perm.put(context, "schedule_lessons#added#" + cache_token, added.toString());
            }
            return true;
        } catch (Exception e) {
            Static.error(e);
            return false;
        }
    }
    public static void editLesson(final ConnectedActivity activity, final JSONObject schedule, final JSONObject lesson, final int dayIndex, final int week) {
        Static.T.runThread(new Runnable() {
            @Override
            public void run() {
                try {
                    Log.v(TAG, "editLesson | cache_token=" + schedule.getString("cache_token"));
                    if (!Objects.equals(lesson.getString("cdoitmo_type"), "synthetic")) throw new Exception("Wrong cdoitmo_type type");
                    Bundle extras = new Bundle();
                    extras.putSerializable("action_type", ScheduleLessonsModifyFragment.TYPE.edit);
                    extras.putString("header", schedule.getString("title") + " " + schedule.getString("label"));
                    extras.putString("cache_token", schedule.getString("cache_token"));
                    extras.putInt("day", dayIndex);
                    extras.putInt("week", week);
                    extras.putString("hash", Static.crypt(lesson.toString()));
                    activity.openActivityOrFragment(ScheduleLessonsModifyFragment.class, extras);
                } catch (Exception e) {
                    Static.error(e);
                    Static.snackBar(activity, activity.getString(R.string.something_went_wrong));
                }
            }
        });
    }
    public static boolean editLesson(Context context, String cache_token, int index, String hash, LessonUnit lessonUnit) {
        Log.v(TAG, "editLesson(LessonUnit) | cache_token=" + cache_token);
        return deleteLesson(context, cache_token, index, hash) && createLesson(context, lessonUnit);
    }
    public static String getCast(JSONObject lesson) throws JSONException {
        JSONObject replica = new JSONObject(lesson.toString());
        replica.remove("cdoitmo_type");
        return replica.toString();
    }
    private static String getLessonField(String field, String def) {
        return field == null || field.isEmpty() ? def : field;
    }
}

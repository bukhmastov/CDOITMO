package com.bukhmastov.cdoitmo.objects;

import android.content.Context;
import android.content.SharedPreferences;
import android.preference.PreferenceManager;
import android.support.annotation.Nullable;
import android.support.v4.widget.SwipeRefreshLayout;

import com.bukhmastov.cdoitmo.converters.ScheduleLessonsConverter;
import com.bukhmastov.cdoitmo.fragments.ScheduleLessonsFragment;
import com.bukhmastov.cdoitmo.network.IfmoRestClient;
import com.bukhmastov.cdoitmo.network.interfaces.IfmoRestClientResponseHandler;
import com.bukhmastov.cdoitmo.utils.Cache;
import com.bukhmastov.cdoitmo.utils.Static;
import com.bukhmastov.cdoitmo.utils.Storage;
import com.loopj.android.http.RequestHandle;

import org.json.JSONArray;
import org.json.JSONException;
import org.json.JSONObject;

import java.util.Calendar;
import java.util.Objects;
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

    public ScheduleLessons(Context context){
        this.context = context;
    }

    @Override
    public void onRefresh() {
        search(ScheduleLessonsFragment.query, 0);
    }

    public void setHandler(ScheduleLessons.response handler){
        this.handler = handler;
    }

    public void search(String query){
        search(query, getRefreshRate());
    }
    public void search(String query, int refresh_rate){
        search(query, refresh_rate, PreferenceManager.getDefaultSharedPreferences(context).getBoolean("pref_schedule_lessons_use_cache", false));
    }
    public void search(String query, boolean toCache){
        search(query, getRefreshRate(), toCache);
    }
    public void search(String query, int refresh_rate, boolean toCache){
        if (handler == null) return;
        query = query.trim();
        ScheduleLessonsFragment.query = query;
        if (ScheduleLessonsFragment.fragmentRequestHandle != null) ScheduleLessonsFragment.fragmentRequestHandle.cancel(true);
        if (Pattern.compile("^[a-zA-Z]{1,3}\\d+[a-zA-Z]?$").matcher(query).find()) {
            searchGroup(query.toUpperCase(), refresh_rate, toCache);
        }
        else if (Pattern.compile("^[0-9]{6}$").matcher(query).find()) {
            searchDefinedTeacher(query, refresh_rate, toCache);
        }
        else if (Pattern.compile("^[0-9].*$").matcher(query).find()) {
            searchRoom(query, refresh_rate, toCache);
        }
        else {
            searchTeacher(query.split(" ")[0].trim(), refresh_rate, toCache);
        }
    }

    private void searchGroup(final String group, final int refresh_rate, final boolean toCache){
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
                                    handler.onSuccess(json);
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
                public void onFailure(int state) {
                    handler.onFailure(state);
                }
                @Override
                public void onNewHandle(RequestHandle requestHandle) {
                    handler.onNewHandle(requestHandle);
                }
            });
        } else if (Static.OFFLINE_MODE && Objects.equals(cache, "")) {
            handler.onFailure(FAILED_OFFLINE);
        } else {
            try {
                handler.onSuccess(new JSONObject(cache));
            } catch (JSONException e) {
                Static.error(e);
                handler.onFailure(FAILED_LOAD);
            }
        }
    }
    private void searchRoom(final String room, final int refresh_rate, final boolean toCache){
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
                                    handler.onSuccess(json);
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
                public void onFailure(int state) {
                    handler.onFailure(state);
                }
                @Override
                public void onNewHandle(RequestHandle requestHandle) {
                    handler.onNewHandle(requestHandle);
                }
            });
        } else if (Static.OFFLINE_MODE && Objects.equals(cache, "")) {
            handler.onFailure(FAILED_OFFLINE);
        } else {
            try {
                handler.onSuccess(new JSONObject(cache));
            } catch (JSONException e) {
                Static.error(e);
                handler.onFailure(FAILED_LOAD);
            }
        }
    }
    private void searchTeacher(final String teacher, final int refresh_rate, final boolean toCache){
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
                public void onFailure(int state) {
                    handler.onFailure(state);
                }
                @Override
                public void onNewHandle(RequestHandle requestHandle) {
                    handler.onNewHandle(requestHandle);
                }
            });
        } else if (Static.OFFLINE_MODE && Objects.equals(cache, "")) {
            handler.onFailure(FAILED_OFFLINE);
        } else {
            try {
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
    private void searchDefinedTeacher(final String teacherId, final int refresh_rate, final boolean toCache){
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
                                    handler.onSuccess(json);
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
                public void onFailure(int state) {
                    handler.onFailure(state);
                }
                @Override
                public void onNewHandle(RequestHandle requestHandle) {
                    handler.onNewHandle(requestHandle);
                }
            });
        } else if (Static.OFFLINE_MODE && Objects.equals(cache, "")) {
            handler.onFailure(FAILED_OFFLINE);
        } else {
            try {
                handler.onSuccess(new JSONObject(cache));
            } catch (JSONException e) {
                Static.error(e);
                handler.onFailure(FAILED_LOAD);
            }
        }
    }

    @Nullable
    private JSONObject getTemplate(String type, String cache_token, JSONObject remote){
        try {
            JSONObject template = new JSONObject();
            template.put("query", remote.get("query").toString());
            template.put("type", type);
            template.put("title", remote.getString("type_title"));
            template.put("label", remote.getString("label"));
            template.put("timestamp", Calendar.getInstance().getTimeInMillis());
            template.put("cache_token", cache_token);
            template.put("schedule", new JSONArray());
            return template;
        } catch (JSONException e) {
            Static.error(e);
            return null;
        }
    }
    private int getRefreshRate(){
        SharedPreferences sharedPreferences = PreferenceManager.getDefaultSharedPreferences(context);
        return sharedPreferences.getBoolean("pref_use_cache", true) ? Integer.parseInt(sharedPreferences.getString("pref_schedule_refresh", "168")) : 0;
    }
    private boolean getForce(String cache, int refresh_rate){
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
    public String getDefault(){
        String scope;
        String pref = PreferenceManager.getDefaultSharedPreferences(context).getString("pref_schedule_lessons_default", "");
        try {
            if (Objects.equals(pref, "")) throw new Exception("pref_schedule_lessons_default empty");
            JSONObject jsonObject = new JSONObject(pref);
            scope = jsonObject.getString("query");
            if (Objects.equals(scope, "auto")) throw new Exception("pref_schedule_lessons_default query=auto");
        } catch (Exception e) {
            scope = Storage.get(context, "group");
        }
        return scope;
    }

    private void putCache(String token, String value, boolean toCache){
        try {
            String def = getDefault();
            if (toCache || hasCache(token) || Objects.equals(def, token) || Objects.equals("group_" + def, token) || Objects.equals("room_" + def, token) || Objects.equals("teacher_picker_" + def, token)) {
                String jsonStr = Cache.get(context, "schedule_lessons");
                JSONObject json;
                if (Objects.equals(jsonStr, "")) {
                    json = new JSONObject();
                } else {
                    json = new JSONObject(jsonStr);
                }
                if (json.has(token)) json.remove(token);
                json.put(token, value);
                Cache.put(context, "schedule_lessons", json.toString());
            }
        } catch (JSONException e) {
            Static.error(e);
        }
    }
    private boolean hasCache(String token){
        try {
            String jsonStr = Cache.get(context, "schedule_lessons");
            if (Objects.equals(jsonStr, "")) {
                return false;
            } else {
                return (new JSONObject(jsonStr)).has(token);
            }
        } catch (JSONException e) {
            Static.error(e);
            return false;
        }
    }
    public String getCache(String token){
        try {
            String jsonStr = Cache.get(context, "schedule_lessons");
            if (Objects.equals(jsonStr, "")) {
                return "";
            } else {
                JSONObject json = new JSONObject(jsonStr);
                if (json.has(token)) {
                    return json.getString(token);
                } else {
                    return "";
                }
            }
        } catch (JSONException e) {
            Static.error(e);
            return "";
        }
    }
    private void removeCache(String token){
        try {
            String jsonStr = Cache.get(context, "schedule_lessons");
            JSONObject json;
            if (Objects.equals(jsonStr, "")) {
                json = new JSONObject();
            } else {
                json = new JSONObject(jsonStr);
            }
            if (json.has(token)) json.remove(token);
            Cache.put(context, "schedule_lessons", json.toString());
        } catch (JSONException e) {
            Static.error(e);
        }
    }
    public Boolean toggleCache(){
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

}
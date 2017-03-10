package com.bukhmastov.cdoitmo.objects;

import android.content.Context;
import android.content.SharedPreferences;
import android.preference.PreferenceManager;
import android.support.v4.widget.SwipeRefreshLayout;

import com.bukhmastov.cdoitmo.fragments.ScheduleExamsFragment;
import com.bukhmastov.cdoitmo.network.IfmoClient;
import com.bukhmastov.cdoitmo.network.interfaces.IfmoClientResponseHandler;
import com.bukhmastov.cdoitmo.parse.ScheduleExamsGroupParse;
import com.bukhmastov.cdoitmo.parse.ScheduleExamsTeacherParse;
import com.bukhmastov.cdoitmo.parse.ScheduleExamsTeacherPickerParse;
import com.bukhmastov.cdoitmo.utils.Cache;
import com.bukhmastov.cdoitmo.utils.Static;
import com.bukhmastov.cdoitmo.utils.Storage;
import com.loopj.android.http.RequestHandle;

import org.json.JSONException;
import org.json.JSONObject;

import java.util.Calendar;
import java.util.Objects;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

public class ScheduleExams implements SwipeRefreshLayout.OnRefreshListener {

    private static final String TAG = "ScheduleExams";
    public interface response {
        void onProgress(int state);
        void onFailure(int state);
        void onSuccess(JSONObject json);
        void onNewHandle(RequestHandle requestHandle);
    }
    private ScheduleExams.response handler = null;
    private Context context;
    public static final int FAILED_LOAD = 100;
    public static final int FAILED_OFFLINE = 101;

    public ScheduleExams(Context context){
        this.context = context;
    }

    @Override
    public void onRefresh() {
        search(ScheduleExamsFragment.query, 0);
    }

    public void setHandler(ScheduleExams.response handler){
        this.handler = handler;
    }

    public void search(String query){
        SharedPreferences sharedPreferences = PreferenceManager.getDefaultSharedPreferences(context);
        search(query, sharedPreferences.getBoolean("pref_use_cache", true) ? Integer.parseInt(sharedPreferences.getString("pref_schedule_refresh", "168")) : 0);
    }
    public void search(String query, int refresh_rate){
        search(query, refresh_rate, PreferenceManager.getDefaultSharedPreferences(context).getBoolean("pref_schedule_exams_use_cache", false));
    }
    public void search(String query, int refresh_rate, boolean toCache){
        query = query.trim();
        ScheduleExamsFragment.query = query;
        if(ScheduleExamsFragment.fragmentRequestHandle != null) ScheduleExamsFragment.fragmentRequestHandle.cancel(true);
        if(Pattern.compile("^\\w{1,3}\\d{4}\\w?$").matcher(query).find()){
            searchGroup(query.toUpperCase(), refresh_rate, toCache);
        }
        else if(Pattern.compile("^teacher\\d+$").matcher(query).find()){
            searchDefinedTeacher(query, refresh_rate, toCache);
        }
        else {
            searchTeacher(query, refresh_rate, toCache);
        }
    }

    private void searchGroup(final String group, final int refresh_rate, final boolean toCache){
        final String cache_token = "group_" + group;
        final String cache = getCache(cache_token);
        if(getForce(cache, refresh_rate) && !Static.OFFLINE_MODE) {
            IfmoClient.get(context, "ru/exam/0/" + group + "/raspisanie_sessii.htm", null, new IfmoClientResponseHandler() {
                @Override
                public void onSuccess(int statusCode, String response) {
                    if (statusCode == 200) {
                        new ScheduleExamsGroupParse(new ScheduleExamsGroupParse.response() {
                            @Override
                            public void finish(JSONObject json) {
                                try {
                                    if (json == null) throw new NullPointerException("json cannot be null");
                                    if (toCache || Objects.equals(Storage.get(context, "group").toUpperCase(), group)){
                                        if(json.getJSONArray("schedule").length() > 0) putCache(cache_token, json.toString(), toCache);
                                    }
                                    handler.onSuccess(json);
                                } catch (Exception e) {
                                    Static.error(e);
                                    handler.onFailure(FAILED_LOAD);
                                }
                            }
                        }).execute(response, cache_token);
                    } else {
                        if(Objects.equals(cache, "")){
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
        } else if (Static.OFFLINE_MODE) {
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
        if(getForce(cache, refresh_rate) && !Static.OFFLINE_MODE) {
            IfmoClient.get(context, "ru/exam/1/" + teacher + "/raspisanie_sessii.htm", null, new IfmoClientResponseHandler() {
                @Override
                public void onSuccess(int statusCode, String response) {
                    if (statusCode == 200) {
                        new ScheduleExamsTeacherPickerParse(new ScheduleExamsTeacherPickerParse.response() {
                            @Override
                            public void finish(JSONObject json) {
                                try {
                                    if (json == null) throw new NullPointerException("json cannot be null");
                                    if (toCache){
                                        if(json.getJSONArray("teachers").length() > 0) putCache(cache_token, json.toString(), toCache);
                                    }
                                    if (json.getJSONArray("teachers").length() == 1){
                                        search(json.getJSONArray("teachers").getJSONObject(0).getString("scope"), refresh_rate, toCache);
                                    } else {
                                        handler.onSuccess(json);
                                    }
                                } catch (Exception e) {
                                    Static.error(e);
                                    handler.onFailure(FAILED_LOAD);
                                }
                            }
                        }).execute(response, cache_token);
                    } else {
                        if(Objects.equals(cache, "")){
                            handler.onFailure(FAILED_LOAD);
                        } else {
                            try {
                                JSONObject list = new JSONObject(cache);
                                if(list.getJSONArray("teachers").length() == 1){
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
        } else if (Static.OFFLINE_MODE) {
            handler.onFailure(FAILED_OFFLINE);
        } else {
            try {
                JSONObject list = new JSONObject(cache);
                if(list.getJSONArray("teachers").length() == 1){
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
        Matcher m = Pattern.compile("^teacher(\\d+)$").matcher(teacherId);
        if(m.find()){
            final String id = m.group(1);
            final String cache_token = "teacher" + id;
            final String cache = getCache(cache_token);
            if (getForce(cache, refresh_rate) && !Static.OFFLINE_MODE) {
                IfmoClient.get(context, "ru/exam/3/" + id + "/raspisanie_sessii.htm", null, new IfmoClientResponseHandler() {
                    @Override
                    public void onSuccess(int statusCode, String response) {
                        if (statusCode == 200) {
                            new ScheduleExamsTeacherParse(new ScheduleExamsTeacherParse.response() {
                                @Override
                                public void finish(JSONObject json) {
                                    try {
                                        if(json == null) throw new NullPointerException("json cannot be null");
                                        if (toCache) {
                                            if(json.getJSONArray("schedule").length() > 0) putCache(cache_token, json.toString(), toCache);
                                        }
                                        handler.onSuccess(json);
                                    } catch (Exception e) {
                                        Static.error(e);
                                        handler.onFailure(FAILED_LOAD);
                                    }
                                }
                            }).execute(response, cache_token);
                        } else {
                            if(Objects.equals(cache, "")){
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
            } else if (Static.OFFLINE_MODE) {
                handler.onFailure(FAILED_OFFLINE);
            } else {
                try {
                    handler.onSuccess(new JSONObject(cache));
                } catch (JSONException e) {
                    Static.error(e);
                    handler.onFailure(FAILED_LOAD);
                }
            }
        } else {
            handler.onFailure(FAILED_LOAD);
        }
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
        String pref = PreferenceManager.getDefaultSharedPreferences(context).getString("pref_schedule_exams_default", "");
        try {
            if (Objects.equals(pref, "")) throw new Exception("pref_schedule_exams_default empty");
            JSONObject jsonObject = new JSONObject(pref);
            scope = jsonObject.getString("query");
            if (Objects.equals(scope, "auto")) throw new Exception("pref_schedule_exams_default query=auto");
        } catch (Exception e) {
            scope = Storage.get(context, "group");
        }
        return scope;
    }

    private void putCache(String token, String value, boolean toCache){
        try {
            String def = getDefault();
            if (toCache || hasCache(token) || Objects.equals(def, token) || Objects.equals("group_" + def, token) || Objects.equals("teacher_picker_" + def, token)) {
                String jsonStr = Cache.get(context, "schedule_exams");
                JSONObject json;
                if (Objects.equals(jsonStr, "")) {
                    json = new JSONObject();
                } else {
                    json = new JSONObject(jsonStr);
                }
                if (json.has(token)) json.remove(token);
                json.put(token, value);
                Cache.put(context, "schedule_exams", json.toString());
            }
        } catch (JSONException e) {
            Static.error(e);
        }
    }
    private boolean hasCache(String token){
        try {
            String jsonStr = Cache.get(context, "schedule_exams");
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
            String jsonStr = Cache.get(context, "schedule_exams");
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
            String jsonStr = Cache.get(context, "schedule_exams");
            JSONObject json;
            if (Objects.equals(jsonStr, "")) {
                json = new JSONObject();
            } else {
                json = new JSONObject(jsonStr);
            }
            if (json.has(token)) json.remove(token);
            Cache.put(context, "schedule_exams", json.toString());
        } catch (JSONException e) {
            Static.error(e);
        }
    }
    public Boolean toggleCache(){
        try {
            String token = ScheduleExamsFragment.schedule.getString("cache_token");
            if (Objects.equals(getCache(token), "")) {
                putCache(token, ScheduleExamsFragment.schedule.toString(), true);
                ScheduleExamsFragment.schedule_cached = true;
                return true;
            } else {
                removeCache(token);
                ScheduleExamsFragment.schedule_cached = false;
                return false;
            }
        } catch (JSONException e) {
            Static.error(e);
            return null;
        }
    }

}

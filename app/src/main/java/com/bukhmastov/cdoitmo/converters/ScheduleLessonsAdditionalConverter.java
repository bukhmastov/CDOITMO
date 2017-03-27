package com.bukhmastov.cdoitmo.converters;

import android.content.Context;
import android.os.AsyncTask;

import com.bukhmastov.cdoitmo.objects.ScheduleLessons;
import com.bukhmastov.cdoitmo.utils.Static;
import com.bukhmastov.cdoitmo.utils.Storage;

import org.json.JSONArray;
import org.json.JSONException;
import org.json.JSONObject;

import java.util.ArrayList;
import java.util.Calendar;
import java.util.Collections;
import java.util.Comparator;
import java.util.Objects;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

public class ScheduleLessonsAdditionalConverter extends AsyncTask<JSONObject, Void, JSONObject> {
    public interface response {
        void finish(JSONObject json);
    }
    private Context context;
    private response delegate = null;
    public ScheduleLessonsAdditionalConverter(Context context, response delegate){
        this.context = context;
        this.delegate = delegate;
    }
    @Override
    protected JSONObject doInBackground(JSONObject... params) {
        JSONObject response = params[0];
        try {
            String cache_token = response.getString("cache_token");
            if (cache_token.isEmpty()) throw new Exception("cache_token is empty");
            JSONArray schedule = response.getJSONArray("schedule");
            JSONArray scheduleAdded = string2json(Storage.file.perm.get(context, "schedule_lessons#added#" + cache_token, ""));
            JSONArray scheduleReduced = string2json(Storage.file.perm.get(context, "schedule_lessons#reduced#" + cache_token, ""));
            for (int i = 0; i < schedule.length(); i++) {
                JSONObject day = schedule.getJSONObject(i);
                JSONArray lessons = day.getJSONArray("lessons");
                for (int j = 0; j < lessons.length(); j++) {
                    JSONObject lesson = lessons.getJSONObject(j);
                    String hash = Static.crypt(ScheduleLessons.getCast(lesson));
                    for (int k = 0; k < scheduleReduced.length(); k++) {
                        JSONObject dayReduced = scheduleReduced.getJSONObject(k);
                        if (dayReduced.getInt("index") != day.getInt("index")) continue;
                        JSONArray lessonsReduced = dayReduced.getJSONArray("lessons");
                        for (int a = 0; a < lessonsReduced.length(); a++) {
                            if (Objects.equals(hash, lessonsReduced.getString(a))) {
                                lesson.put("cdoitmo_type", "reduced");
                                break;
                            }
                        }
                        break;
                    }
                }
                for (int j = 0; j < scheduleAdded.length(); j++) {
                    JSONObject dayAdded = scheduleAdded.getJSONObject(j);
                    if (dayAdded.getInt("index") != day.getInt("index")) continue;
                    JSONArray lessonsAdded = dayAdded.getJSONArray("lessons");
                    if (lessonsAdded.length() < 1) continue;
                    ArrayList<LessonSort> lessonSorts = new ArrayList<>();
                    for (int k = 0; k < lessons.length(); k++) {
                        JSONObject lesson = lessons.getJSONObject(k);
                        lessonSorts.add(new LessonSort(getTime(lesson).start, lesson));
                    }
                    for (int k = 0; k < lessonsAdded.length(); k++) {
                        JSONObject lesson = lessonsAdded.getJSONObject(k);
                        lessonSorts.add(new LessonSort(getTime(lesson).start, lesson));
                    }
                    Collections.sort(lessonSorts, new Comparator<LessonSort>() {
                        public int compare(LessonSort o1, LessonSort o2) {
                            return o1.ts > o2.ts ? 1 : (o1.ts < o2.ts ? -1 : 0);
                        }
                    });
                    lessons = new JSONArray();
                    for (LessonSort lessonSort : lessonSorts) {
                        lessons.put(lessonSort.lesson);
                    }
                    day.put("lessons", lessons);
                }
            }
        } catch (Exception e) {
            Static.error(e);
        }
        return response;
    }
    private JSONArray string2json(String text) throws JSONException {
        JSONArray json;
        if (text.isEmpty()) {
            json = new JSONArray();
        } else {
            json = new JSONArray(text);
        }
        return json;
    }
    private Time getTime(JSONObject lesson) throws JSONException {
        Pattern timePattern = Pattern.compile("^(\\d{1,2}):(\\d{2})$");
        Time time = new Time();
        time.start = getTime(timePattern, lesson.getString("timeStart"));
        time.end = getTime(timePattern, lesson.getString("timeEnd"));
        return time;
    }
    private long getTime(Pattern pattern, String value){
        Matcher time = pattern.matcher(value);
        if (time.find()) {
            Calendar calendar = Calendar.getInstance();
            calendar.set(Calendar.HOUR_OF_DAY, Integer.parseInt(time.group(1)));
            calendar.set(Calendar.MINUTE, Integer.parseInt(time.group(2)));
            calendar.set(Calendar.SECOND, 0);
            return calendar.getTimeInMillis();
        } else {
            return -1;
        }
    }
    private class Time {
        public long start;
        public long end;
    }
    private class LessonSort {
        public long ts;
        public JSONObject lesson;
        LessonSort(long ts, JSONObject lesson){
            this.ts = ts;
            this.lesson = lesson;
        }
    }
    @Override
    protected void onPostExecute(JSONObject json) {
        delegate.finish(json);
    }
}
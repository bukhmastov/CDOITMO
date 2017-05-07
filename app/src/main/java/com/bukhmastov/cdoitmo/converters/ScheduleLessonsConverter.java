package com.bukhmastov.cdoitmo.converters;

import android.content.Context;
import android.os.AsyncTask;

import com.bukhmastov.cdoitmo.utils.Log;
import com.bukhmastov.cdoitmo.utils.Static;

import org.json.JSONArray;
import org.json.JSONException;
import org.json.JSONObject;

import java.util.Objects;

public class ScheduleLessonsConverter extends AsyncTask<JSONObject, Void, JSONObject> {

    private static final String TAG = "SLConverter";
    public interface response {
        void finish(JSONObject json);
    }
    private Context context;
    private response delegate = null;

    public ScheduleLessonsConverter(Context context, response delegate){
        this.context = context;
        this.delegate = delegate;
    }

    @Override
    protected JSONObject doInBackground(JSONObject... params) {
        Log.i(TAG, "started");
        JSONObject response = params[1];
        try {
            JSONArray remoteSchedule = params[0].getJSONArray("schedule");
            JSONArray schedule = new JSONArray();
            for (int i = 0; i < 7; i++) {
                JSONObject dayObj = new JSONObject();
                dayObj.put("index", i);
                switch (i) {
                    case 0: dayObj.put("title", "Понедельник"); dayObj.put("titleShort", "Пн"); break;
                    case 1: dayObj.put("title", "Вторник"); dayObj.put("titleShort", "Вт"); break;
                    case 2: dayObj.put("title", "Среда"); dayObj.put("titleShort", "Ср"); break;
                    case 3: dayObj.put("title", "Четверг"); dayObj.put("titleShort", "Чт"); break;
                    case 4: dayObj.put("title", "Пятница"); dayObj.put("titleShort", "Пт"); break;
                    case 5: dayObj.put("title", "Суббота"); dayObj.put("titleShort", "Сб"); break;
                    case 6: dayObj.put("title", "Воскресенье"); dayObj.put("titleShort", "Вс"); break;
                    default: dayObj.put("title", ""); dayObj.put("titleShort", ""); break;
                }
                JSONArray lessons = new JSONArray();
                for (int j = 0; j < remoteSchedule.length(); j++) {
                    JSONObject remoteLesson = remoteSchedule.getJSONObject(j);
                    if (remoteLesson.getInt("data_day") == i) {
                        JSONObject lesson = new JSONObject();
                        String subject = get(remoteLesson, "title", "");
                        String note = get(remoteLesson, "note", null);
                        String status = get(remoteLesson, "status", "");
                        if (note != null) {
                            if (!Objects.equals(subject, "")) subject += ": ";
                            subject += note;
                        }
                        lesson.put("subject", subject);
                        lesson.put("week", remoteLesson.has("data_week") ? remoteLesson.getInt("data_week") - 1 : 2);
                        if (lesson.getInt("week") < 0) lesson.put("week", 2);
                        lesson.put("timeStart", get(remoteLesson, "start_time", "∞"));
                        lesson.put("timeEnd", get(remoteLesson, "end_time", "∞"));
                        lesson.put("group", get(remoteLesson, "gr", ""));
                        lesson.put("teacher", get(remoteLesson, "person", ""));
                        lesson.put("teacher_id", get(remoteLesson, "pid", ""));
                        lesson.put("room", get(remoteLesson, "room", ""));
                        lesson.put("building", get(remoteLesson, "place", ""));
                        switch (status) {
                            case "Лек": case "Лекция": lesson.put("type", "lecture"); break;
                            case "Прак": case "Практика": lesson.put("type", "practice"); break;
                            case "Лаб": case "Лабораторная": lesson.put("type", "lab"); break;
                            default:
                                if (!Objects.equals(status, "")) lesson.put("type", status);
                                break;
                        }
                        lesson.put("cdoitmo_type", "normal");
                        lessons.put(lesson);
                    }
                }
                dayObj.put("lessons", lessons);
                schedule.put(i, dayObj);
            }
            response.put("schedule", schedule);
        } catch (Exception e) {
            Static.error(e);
        }
        return response;
    }

    private String get(JSONObject json, String key, String replace) throws JSONException {
        return json.has(key) && json.get(key).toString() != null && !Objects.equals(json.get(key).toString(), "") && !Objects.equals(json.get(key).toString(), "null") ? json.get(key).toString() : replace;
    }

    @Override
    protected void onPostExecute(JSONObject json) {
        Log.i(TAG, "finished");
        delegate.finish(json);
    }

}

package com.bukhmastov.cdoitmo.converters.schedule.lessons;

import org.json.JSONArray;
import org.json.JSONException;
import org.json.JSONObject;

public abstract class ScheduleLessonsConverter extends ScheduleConverter {

    public interface response {
        void finish(JSONObject json);
    }
    protected final response delegate;
    protected final JSONObject data;
    protected final JSONObject template;
    protected String templateType = "";
    protected String templateTitle = null;

    public ScheduleLessonsConverter(JSONObject data, JSONObject template, response delegate) {
        this.data = data;
        this.template = template;
        this.delegate = delegate;
    }

    protected abstract JSONObject getConvertedLesson(JSONObject lesson, String group_name) throws Exception;
    protected JSONArray getEmptySchedule() throws Exception {
        final JSONArray schedule = new JSONArray();
        for (int i = 0; i < 7; i++) {
            schedule.put(i, new JSONObject()
                    .put("weekday", i)
                    .put("lessons",  new JSONArray())
            );
        }
        return schedule;
    }
    protected String getString(JSONObject json, String key) throws JSONException {
        return getString(json, key, "");
    }
    protected String getString(JSONObject json, String key, String replace) throws JSONException {
        return json.has(key) && !json.isNull(key) && !json.get(key).toString().isEmpty() && !json.get(key).toString().equals("null") ? json.get(key).toString() : replace;
    }
}

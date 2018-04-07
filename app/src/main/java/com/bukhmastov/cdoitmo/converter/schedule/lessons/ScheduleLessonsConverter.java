package com.bukhmastov.cdoitmo.converter.schedule.lessons;

import com.bukhmastov.cdoitmo.firebase.FirebasePerformanceProvider;

import org.json.JSONArray;
import org.json.JSONObject;

public abstract class ScheduleLessonsConverter extends ScheduleConverter {

    protected final JSONObject data;
    protected final JSONObject template;
    protected String templateType = "";
    protected String templateTitle = null;

    public ScheduleLessonsConverter(JSONObject data, JSONObject template, Response delegate) {
        super(delegate);
        this.data = data;
        this.template = template;
    }

    @Override
    protected String getTraceName() {
        return FirebasePerformanceProvider.Trace.Convert.Schedule.LESSONS;
    }

    protected abstract JSONObject getConvertedLesson(JSONObject lesson, String group_name) throws Exception;

    protected JSONArray getEmptySchedule() throws Exception {
        final JSONArray schedule = new JSONArray();
        for (int i = 0; i < 7; i++) {
            schedule.put(i, new JSONObject()
                    .put("weekday", i)
                    .put("lessons", new JSONArray())
            );
        }
        return schedule;
    }
}

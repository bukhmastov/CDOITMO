package com.bukhmastov.cdoitmo.converter.schedule;

import com.bukhmastov.cdoitmo.converter.Converter;
import com.bukhmastov.cdoitmo.firebase.FirebasePerformanceProvider;

import org.json.JSONArray;
import org.json.JSONObject;

public class ScheduleTeachersConverter extends Converter {

    private final JSONObject data;
    private final JSONObject template;

    public ScheduleTeachersConverter(JSONObject data, JSONObject template, Response delegate) {
        super(delegate);
        this.data = data;
        this.template = template;
    }

    @Override
    protected JSONObject convert() throws Throwable {
        final JSONArray listConverted = new JSONArray();
        final JSONArray list = data.getJSONArray("list");
        for (int i = 0; i < list.length(); i++) {
            final JSONObject teacher = list.getJSONObject(i);
            listConverted.put(new JSONObject()
                    .put("person", getString(teacher, "person"))
                    .put("post", getString(teacher, "post"))
                    .put("pid", teacher.isNull("pid") ? "" : String.valueOf(teacher.getInt("pid")))
            );
        }
        template.put("title", "");
        template.put("schedule", listConverted);
        return template;
    }

    @Override
    protected String getTraceName() {
        return FirebasePerformanceProvider.Trace.Convert.Schedule.TEACHERS;
    }
}

package com.bukhmastov.cdoitmo.converters.schedule;

import com.bukhmastov.cdoitmo.utils.Log;
import com.bukhmastov.cdoitmo.utils.Static;

import org.json.JSONArray;
import org.json.JSONException;
import org.json.JSONObject;

public class ScheduleTeachersConverter implements Runnable {

    private static final String TAG = "STConverter";
    public interface response {
        void finish(JSONObject json);
    }
    protected final response delegate;
    protected final JSONObject data;
    protected final JSONObject template;

    public ScheduleTeachersConverter(JSONObject data, JSONObject template, response delegate) {
        this.data = data;
        this.template = template;
        this.delegate = delegate;
    }

    @Override
    public void run() {
        Log.v(TAG, "converting");
        try {
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
            Log.v(TAG, "converted");
            delegate.finish(template);
        } catch (Exception e) {
            Log.v(TAG, "conversion failed");
            Static.error(e);
            delegate.finish(null);
        }
    }

    protected String getString(JSONObject json, String key) throws JSONException {
        return getString(json, key, "");
    }
    protected String getString(JSONObject json, String key, String replace) throws JSONException {
        return json.has(key) && !json.isNull(key) && !json.get(key).toString().isEmpty() && !json.get(key).toString().equals("null") ? json.get(key).toString() : replace;
    }
}

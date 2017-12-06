package com.bukhmastov.cdoitmo.converters.schedule.exams;

import com.bukhmastov.cdoitmo.utils.Log;
import com.bukhmastov.cdoitmo.utils.Static;

import org.json.JSONArray;
import org.json.JSONObject;

public class ScheduleExamsConverterIfmo extends ScheduleExamsConverter {

    private static final String TAG = "SEConverterIsu";

    public ScheduleExamsConverterIfmo(JSONObject data, JSONObject template, response delegate) {
        super(data, template, delegate);
    }

    @Override
    public void run() {
        Log.v(TAG, "converting");
        try {
            templateType = template.getString("type");
            templateTitle = templateType.equals("mine") ? "" : null;
            final JSONArray examsConverted = new JSONArray();
            final JSONArray exams = data.getJSONArray("exams");
            for (int i = 0; i < exams.length(); i++) {
                final JSONObject exam = exams.getJSONObject(i);
                examsConverted.put(getConvertedExam(exam));
            }
            // finish
            template.put("title", templateTitle == null ? "" : templateTitle);
            template.put("schedule", sortExamsByTime(examsConverted));
            Log.v(TAG, "converted");
            delegate.finish(template);
        } catch (Exception e) {
            Log.v(TAG, "conversion failed");
            Static.error(e);
            delegate.finish(null);
        }
    }

    private JSONObject getConvertedExam(JSONObject exam) throws Exception {
        final JSONObject ex = exam.getJSONObject("exam");
        final JSONObject ad = exam.getJSONObject("advice");
        final String group = exam.has("group") ? getString(exam, "group") : data.getString("label");
        final String teacher = exam.has("teacher") ? getString(exam, "teacher") : data.getString("label");
        if (templateTitle == null && templateType.equals("group")) templateTitle = group;
        if (templateTitle == null && templateType.equals("teacher")) templateTitle = teacher;
        final JSONObject examConverted = new JSONObject();
        examConverted.put("subject", getString(exam, "subject"));
        examConverted.put("group", group);
        examConverted.put("teacher", teacher);
        examConverted.put("teacher_id", "");
        examConverted.put("exam", new JSONObject()
                .put("date", getString(ex, "date"))
                .put("time", getString(ex, "time"))
                .put("room", getString(ex, "room"))
                .put("building", "")
        );
        examConverted.put("advice", new JSONObject()
                .put("date", getString(ad, "date"))
                .put("time", getString(ad, "time"))
                .put("room", getString(ad, "room"))
                .put("building", "")
        );
        return examConverted;
    }
}

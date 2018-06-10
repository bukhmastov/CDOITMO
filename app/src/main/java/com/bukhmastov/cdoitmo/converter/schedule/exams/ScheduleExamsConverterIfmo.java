package com.bukhmastov.cdoitmo.converter.schedule.exams;

import org.json.JSONArray;
import org.json.JSONObject;

public class ScheduleExamsConverterIfmo extends ScheduleExamsConverter {

    public ScheduleExamsConverterIfmo(JSONObject data, JSONObject template, Response delegate) {
        super(data, template, delegate);
    }

    @Override
    protected JSONObject convert() throws Throwable {
        templateType = template.getString("type");
        templateTitle = templateType.equals("mine") ? "" : null;
        final JSONArray examsConverted = new JSONArray();
        final JSONArray exams = data.getJSONArray("exams");
        for (int i = 0; i < exams.length(); i++) {
            final JSONObject exam = exams.getJSONObject(i);
            examsConverted.put(getConvertedExam(exam));
        }
        template.put("title", templateTitle == null ? "" : templateTitle);
        template.put("schedule", sortExamsByTime(examsConverted));
        return template;
    }

    private JSONObject getConvertedExam(JSONObject exam) throws Exception {
        final JSONObject ex = exam.getJSONObject("exam");
        final JSONObject ad = exam.getJSONObject("advice");
        final String type = exam.has("type") ? getString(exam, "type") : "exam";
        final String group = exam.has("group") ? getString(exam, "group") : data.getString("label");
        final String teacher = exam.has("teacher") ? getString(exam, "teacher") : data.getString("label");
        if (templateTitle == null && templateType.equals("group")) templateTitle = group;
        if (templateTitle == null && templateType.equals("teacher")) templateTitle = teacher;
        final JSONObject examConverted = new JSONObject();
        examConverted.put("type", type);
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
        if (!"credit".equals(type)) {
            examConverted.put("advice", new JSONObject()
                    .put("date", getString(ad, "date"))
                    .put("time", getString(ad, "time"))
                    .put("room", getString(ad, "room"))
                    .put("building", "")
            );
        }
        return examConverted;
    }
}

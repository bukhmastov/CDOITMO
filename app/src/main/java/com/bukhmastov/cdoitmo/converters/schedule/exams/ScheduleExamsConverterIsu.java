package com.bukhmastov.cdoitmo.converters.schedule.exams;

import com.bukhmastov.cdoitmo.utils.Log;
import com.bukhmastov.cdoitmo.utils.Static;

import org.json.JSONArray;
import org.json.JSONObject;

public class ScheduleExamsConverterIsu extends ScheduleExamsConverter {

    private static final String TAG = "SEConverterIsu";

    public ScheduleExamsConverterIsu(JSONObject data, JSONObject template, response delegate) {
        super(data, template, delegate);
    }

    @Override
    public void run() {
        Log.v(TAG, "converting");
        try {
            templateType = template.getString("type");
            templateTitle = templateType.equals("mine") ? "" : null;
            final JSONArray examsConverted = new JSONArray();
            final JSONArray faculties = data.getJSONArray("faculties");
            for (int i = 0; i < faculties.length(); i++) {
                final JSONObject faculty = faculties.getJSONObject(i);
                final JSONArray departments = faculty.getJSONArray("departments");
                for (int j = 0; j < departments.length(); j++) {
                    final JSONObject department = departments.getJSONObject(j);
                    final JSONArray groups = department.getJSONArray("groups");
                    for (int k = 0; k < groups.length(); k++) {
                        final JSONObject group = groups.getJSONObject(k);
                        final String group_name = getString(group, "group_name");
                        if (templateTitle == null && templateType.equals("group")) templateTitle = group_name;
                        final JSONArray exams_schedule = group.getJSONArray("exams_schedule");
                        for (int l = 0; l < exams_schedule.length(); l++) {
                            examsConverted.put(getConvertedExam(exams_schedule.getJSONObject(l), group_name));
                        }
                    }
                }
            }
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

    private JSONObject getConvertedExam(JSONObject exam, String group_name) throws Exception {
        final JSONObject examConverted = new JSONObject();
        final JSONArray auditories = exam.getJSONArray("auditories");
        String room, building;
        // subject
        examConverted.put("subject", getString(exam, "subject"));
        // group
        examConverted.put("group", group_name);
        // teacher
        String teacher_name = "";
        String teacher_id = "";
        JSONArray teachers = exam.getJSONArray("teachers");
        if (teachers.length() > 0) {
            JSONObject teacher = teachers.getJSONObject(0);
            teacher_name = getString(teacher, "teacher_name");
            teacher_id = String.valueOf(teacher.isNull("teacher_id") ? "" : teacher.getInt("teacher_id"));
            if (templateTitle == null && templateType.equals("teacher")) templateTitle = teacher_name;
        }
        examConverted.put("teacher", teacher_name);
        examConverted.put("teacher_id", teacher_id);
        // exam
        room = "";
        building = "";
        if (auditories.length() > 0) {
            JSONObject auditory = auditories.getJSONObject(0);
            if (auditory.getString("type").equals("exam")) {
                room = getString(auditory, "auditory_name");
                building = getString(auditory, "auditory_address");
            }
        }
        examConverted.put("exam", new JSONObject()
            .put("date", getString(exam, "exam_date"))
            .put("time", getString(exam, "exam_time"))
            .put("room", room)
            .put("building", building)
        );
        // advice
        room = "";
        building = "";
        if (auditories.length() > 0) {
            JSONObject auditory = auditories.getJSONObject(0);
            if (auditory.getString("type").equals("advice")) {
                room = getString(auditory, "auditory_name");
                building = getString(auditory, "auditory_address");
            }
        }
        examConverted.put("advice", new JSONObject()
                .put("date", getString(exam, "advice_date"))
                .put("time", getString(exam, "advice_time"))
                .put("room", room)
                .put("building", building)
        );
        return examConverted;
    }
}

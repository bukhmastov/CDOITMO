package com.bukhmastov.cdoitmo.converters.schedule;

import com.bukhmastov.cdoitmo.utils.Log;
import com.bukhmastov.cdoitmo.utils.Static;

import org.json.JSONArray;
import org.json.JSONObject;

public class ScheduleLessonsConverterIsu extends ScheduleLessonsConverter {

    private static final String TAG = "SLConverterIsu";

    public ScheduleLessonsConverterIsu(JSONObject data, JSONObject template, response delegate) {
        super(data, template, delegate);
    }

    @Override
    public void run() {
        Log.v(TAG, "converting");
        try {
            templateType = template.getString("type");
            templateTitle = templateType.equals("mine") ? "" : null;
            final JSONArray scheduleConverted = getEmptySchedule();
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
                        final JSONArray study_schedule = group.getJSONArray("study_schedule");
                        for (int l = 0; l < study_schedule.length(); l++) {
                            final JSONObject day = study_schedule.getJSONObject(l);
                            final int weekday = day.has("weekday") ? day.getInt("weekday") - 1 : -1;
                            if (weekday < 0 || weekday > 6) continue;
                            final JSONArray lessons = day.getJSONArray("lessons");
                            for (int m = 0; m < lessons.length(); m++) {
                                JSONObject dayConverted = scheduleConverted.getJSONObject(weekday);
                                JSONArray lessonsConverted = dayConverted.getJSONArray("lessons");
                                lessonsConverted.put(getConvertedLesson(lessons.getJSONObject(m), group_name));
                            }
                        }
                    }
                }
            }
            // sort lessons of each day
            for (int i = 0; i < scheduleConverted.length(); i++) {
                JSONObject dayConverted = scheduleConverted.getJSONObject(i);
                dayConverted.put("lessons", sortLessonsByTime(dayConverted.getJSONArray("lessons")));
            }
            // finish
            template.put("title", templateTitle == null ? "" : templateTitle);
            template.put("schedule", scheduleConverted);
            Log.v(TAG, "converted");
            delegate.finish(template);
        } catch (Exception e) {
            Log.v(TAG, "conversion failed");
            Static.error(e);
            delegate.finish(null);
        }
    }

    @Override
    protected JSONObject getConvertedLesson(JSONObject lesson, String group_name) throws Exception {
        JSONObject lessonConverted = new JSONObject();
        // title
        String subject = Static.escapeString(getString(lesson, "subject"));
        lessonConverted.put("subject", subject);
        // type
        String type = getString(lesson, "type");
        switch (type) {
            case "1": type = "lecture"; break;
            case "2": type = "lab"; break;
            case "3": type = "practice"; break;
            default: {
                String type_name = getString(lesson, "type_name");
                if (type_name.contains("Лекц")) {
                    type = "lecture";
                } else if (type_name.contains("Лаборатор")) {
                    type = "lab";
                } else if (type_name.contains("Практи")) {
                    type = "practice";
                } else if (!type_name.isEmpty()) {
                    type = type_name;
                } else {
                    type = "";
                }
                break;
            }
        }
        lessonConverted.put("type", type);
        // parity
        int parity = lesson.has("parity") ? lesson.getInt("parity") : 0;
        switch (parity) {
            case 2: parity = 0; break; // even
            case 1: parity = 1; break; // odd
            case 0:
            default: parity = 2; break; // both
        }
        lessonConverted.put("week", parity);
        // time
        lessonConverted.put("timeStart", getString(lesson, "time_start", "∞"));
        lessonConverted.put("timeEnd", getString(lesson, "time_end", "∞"));
        // group
        lessonConverted.put("group", group_name);
        // teacher
        String teacher_name = "";
        String teacher_id = "";
        JSONArray teachers = lesson.getJSONArray("teachers");
        if (teachers.length() > 0) {
            JSONObject teacher = teachers.getJSONObject(0);
            teacher_name = teacher.getString("teacher_name");
            teacher_id = String.valueOf(teacher.isNull("teacher_id") ? "" : teacher.getInt("teacher_id"));
            if (templateTitle == null && templateType.equals("teacher")) templateTitle = teacher_name;
        }
        lessonConverted.put("teacher", teacher_name);
        lessonConverted.put("teacher_id", teacher_id);
        // place
        String auditory_name = "";
        String auditory_address = "";
        JSONArray auditories = lesson.getJSONArray("auditories");
        if (auditories.length() > 0) {
            JSONObject auditory = auditories.getJSONObject(0);
            auditory_name = auditory.getString("auditory_name");
            auditory_address = auditory.getString("auditory_address");
            if (templateTitle == null && templateType.equals("room")) templateTitle = auditory_name;
        }
        lessonConverted.put("room", auditory_name);
        lessonConverted.put("building", auditory_address);
        // in app type
        lessonConverted.put("cdoitmo_type", "normal");
        // that's all
        return lessonConverted;
    }
}

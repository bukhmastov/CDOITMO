package com.bukhmastov.cdoitmo.converters.schedule;

import com.bukhmastov.cdoitmo.utils.Log;
import com.bukhmastov.cdoitmo.utils.Static;

import org.json.JSONArray;
import org.json.JSONException;
import org.json.JSONObject;

import java.util.ArrayList;
import java.util.Calendar;
import java.util.Collections;
import java.util.Comparator;
import java.util.List;

public class ScheduleExamsConverterIsu implements Runnable {

    private static final String TAG = "SEConverterIsu";
    public interface response {
        void finish(JSONObject json);
    }
    protected final response delegate;
    protected final JSONObject data;
    protected final JSONObject template;
    protected String templateType = "";
    protected String templateTitle = null;

    public ScheduleExamsConverterIsu(JSONObject data, JSONObject template, response delegate) {
        this.data = data;
        this.template = template;
        this.delegate = delegate;
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
    private JSONArray sortExamsByTime(JSONArray exams) throws Exception {
        JSONArray examsSorted = new JSONArray();
        List<JSONObject> list = new ArrayList<>();
        for (int j = 0; j < exams.length(); j++) {
            list.add(exams.getJSONObject(j));
        }
        Collections.sort(list, new Comparator<JSONObject>() {
            @Override
            public int compare(JSONObject a, JSONObject b) {
                try {
                    final JSONObject examA = a.getJSONObject("exam");
                    final JSONObject examB = b.getJSONObject("exam");
                    final String[] dateA = examA.getString("date").split(".");
                    final String[] timeStartA = examA.getString("time").split(":");
                    final String[] dateB = examB.getString("date").split(".");
                    final String[] timeStartB = examB.getString("time").split(":");
                    final Calendar calendarA = Calendar.getInstance();
                    calendarA.set(Calendar.DAY_OF_MONTH, Integer.parseInt(dateA[0]));
                    calendarA.set(Calendar.MONTH, Integer.parseInt(dateA[1]));
                    calendarA.set(Calendar.YEAR, Integer.parseInt(dateA[2]));
                    calendarA.set(Calendar.HOUR_OF_DAY, Integer.parseInt(timeStartA[0]));
                    calendarA.set(Calendar.MINUTE, Integer.parseInt(timeStartA[1]));
                    calendarA.set(Calendar.SECOND, 0);
                    final Calendar calendarB = Calendar.getInstance();
                    calendarB.set(Calendar.DAY_OF_MONTH, Integer.parseInt(dateB[0]));
                    calendarB.set(Calendar.MONTH, Integer.parseInt(dateB[1]));
                    calendarB.set(Calendar.YEAR, Integer.parseInt(dateB[2]));
                    calendarB.set(Calendar.HOUR_OF_DAY, Integer.parseInt(timeStartB[0]));
                    calendarB.set(Calendar.MINUTE, Integer.parseInt(timeStartB[1]));
                    calendarB.set(Calendar.SECOND, 0);
                    return calendarA.compareTo(calendarB);
                } catch (Exception e) {
                    return 0;
                }
            }
        });
        for (int j = 0; j < list.size(); j++) {
            examsSorted.put(list.get(j));
        }
        return examsSorted;
    }
    private String getString(JSONObject json, String key) throws JSONException {
        return getString(json, key, "");
    }
    private String getString(JSONObject json, String key, String replace) throws JSONException {
        return json.has(key) && !json.isNull(key) && !json.get(key).toString().isEmpty() && !json.get(key).toString().equals("null") ? json.get(key).toString() : replace;
    }
}

package com.bukhmastov.cdoitmo.converter.schedule.exams;

import com.bukhmastov.cdoitmo.converter.Converter;
import com.bukhmastov.cdoitmo.firebase.FirebasePerformanceProvider;
import com.bukhmastov.cdoitmo.util.Time;

import org.json.JSONArray;
import org.json.JSONObject;

import java.util.ArrayList;
import java.util.Calendar;
import java.util.Collections;
import java.util.List;

public abstract class ScheduleExamsConverter extends Converter {

    protected final JSONObject data;
    protected final JSONObject template;
    protected String templateType = "";
    protected String templateTitle = null;

    public ScheduleExamsConverter(JSONObject data, JSONObject template, Response delegate) {
        super(delegate);
        this.data = data;
        this.template = template;
    }

    @Override
    protected String getTraceName() {
        return FirebasePerformanceProvider.Trace.Convert.Schedule.EXAMS;
    }

    protected JSONArray sortExamsByTime(JSONArray exams) throws Exception {
        JSONArray examsSorted = new JSONArray();
        List<JSONObject> list = new ArrayList<>();
        for (int j = 0; j < exams.length(); j++) {
            list.add(exams.getJSONObject(j));
        }
        Collections.sort(list, (a, b) -> {
            try {
                final JSONObject examA = a.getJSONObject("exam");
                final JSONObject examB = b.getJSONObject("exam");
                final String[] dateA = examA.getString("date").split(".");
                final String[] timeStartA = examA.getString("time").split(":");
                final String[] dateB = examB.getString("date").split(".");
                final String[] timeStartB = examB.getString("time").split(":");
                final Calendar calendarA = Time.getCalendar();
                calendarA.set(Calendar.DAY_OF_MONTH, Integer.parseInt(dateA[0]));
                calendarA.set(Calendar.MONTH, Integer.parseInt(dateA[1]));
                calendarA.set(Calendar.YEAR, Integer.parseInt(dateA[2]));
                calendarA.set(Calendar.HOUR_OF_DAY, Integer.parseInt(timeStartA[0]));
                calendarA.set(Calendar.MINUTE, Integer.parseInt(timeStartA[1]));
                calendarA.set(Calendar.SECOND, 0);
                final Calendar calendarB = Time.getCalendar();
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
        });
        for (int j = 0; j < list.size(); j++) {
            examsSorted.put(list.get(j));
        }
        return examsSorted;
    }
}

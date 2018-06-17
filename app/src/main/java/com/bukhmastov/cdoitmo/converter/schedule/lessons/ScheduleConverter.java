package com.bukhmastov.cdoitmo.converter.schedule.lessons;

import com.bukhmastov.cdoitmo.converter.Converter;
import com.bukhmastov.cdoitmo.util.Time;

import org.json.JSONArray;
import org.json.JSONObject;

import java.util.ArrayList;
import java.util.Calendar;
import java.util.Collections;
import java.util.List;

public abstract class ScheduleConverter extends Converter {

    public ScheduleConverter(Response delegate) {
        super(delegate);
    }

    protected JSONArray sortLessonsByTime(JSONArray lessons) throws Exception {
        JSONArray lessonsSorted = new JSONArray();
        List<JSONObject> list = new ArrayList<>();
        for (int j = 0; j < lessons.length(); j++) {
            list.add(lessons.getJSONObject(j));
        }
        Collections.sort(list, (a, b) -> {
            try {
                final String[] timeStartA = a.getString("timeStart").split(":");
                final String[] timeStartB = b.getString("timeStart").split(":");
                final Calendar calendarA = Time.getCalendar();
                calendarA.set(Calendar.HOUR_OF_DAY, Integer.parseInt(timeStartA[0]));
                calendarA.set(Calendar.MINUTE, Integer.parseInt(timeStartA[1]));
                calendarA.set(Calendar.SECOND, 0);
                final Calendar calendarB = Time.getCalendar();
                calendarB.set(Calendar.HOUR_OF_DAY, Integer.parseInt(timeStartB[0]));
                calendarB.set(Calendar.MINUTE, Integer.parseInt(timeStartB[1]));
                calendarB.set(Calendar.SECOND, 0);
                return calendarA.compareTo(calendarB);
            } catch (Exception e) {
                return 0;
            }
        });
        for (int j = 0; j < list.size(); j++) {
            lessonsSorted.put(list.get(j));
        }
        return lessonsSorted;
    }
}

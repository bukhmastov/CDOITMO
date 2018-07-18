package com.bukhmastov.cdoitmo.util;

import android.content.Context;

import com.bukhmastov.cdoitmo.util.impl.TimeImpl;

import java.util.Calendar;

public interface Time {

    // future: replace with DI factory
    Time instance = new TimeImpl();
    static Time instance() {
        return instance;
    }

    Calendar getCalendar();

    int getWeek(Context context);

    int getWeek(Context context, Calendar calendar);

    int getWeekDay();

    int getWeekDay(Calendar calendar);

    String getGenitiveMonth(Context context, String month);

    String getGenitiveMonth(Context context, int month);

    String getDay(Context context, int day);

    String getUpdateTime(Context context, long time);
}

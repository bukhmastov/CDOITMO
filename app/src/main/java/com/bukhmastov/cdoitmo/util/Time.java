package com.bukhmastov.cdoitmo.util;

import android.content.Context;

import java.util.Calendar;
import java.util.Locale;

public interface Time {

    Locale DEFAULT_LOCALE = Locale.GERMANY;

    Calendar getCalendar();

    long getTimeInMillis();

    int getWeek(Context context);

    int getWeek(Context context, Calendar calendar);

    int getWeekDay();

    int getWeekDay(Calendar calendar);

    String getMonth(Context context, int month);

    String getGenitiveMonth(Context context, String month);

    String getGenitiveMonth(Context context, int month);

    String getDay(Context context, int day);

    String getUpdateTime(Context context, long time);

    String getScheduleCustomDayRaw(Calendar calendar);

    String getScheduleCustomDayTitle(Context context, String customDay);

    long getScheduleCustomDayTimestamp(String customDay);
}

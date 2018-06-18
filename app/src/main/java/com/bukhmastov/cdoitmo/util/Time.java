package com.bukhmastov.cdoitmo.util;

import android.content.Context;

import com.bukhmastov.cdoitmo.R;
import com.bukhmastov.cdoitmo.data.Week;

import org.json.JSONObject;

import java.text.SimpleDateFormat;
import java.util.Calendar;
import java.util.Date;
import java.util.Locale;

public class Time {

    private static final String TAG = "Time";

    public static Calendar getCalendar() {
        return Calendar.getInstance(Locale.GERMANY);
    }

    public static int getWeek(Context context) {
        return Week.getCurrent(new Storage.Proxy(context));
    }

    public static int getWeek(Context context, Calendar calendar) {
        return Week.getCurrent(new Storage.Proxy(context), calendar);
    }

    public static int getWeekDay() {
        return getWeekDay(getCalendar());
    }

    public static int getWeekDay(Calendar calendar) {
        int weekday = 0;
        switch (calendar.get(Calendar.DAY_OF_WEEK)) {
            case Calendar.MONDAY: weekday = 0; break;
            case Calendar.TUESDAY: weekday = 1; break;
            case Calendar.WEDNESDAY: weekday = 2; break;
            case Calendar.THURSDAY: weekday = 3; break;
            case Calendar.FRIDAY: weekday = 4; break;
            case Calendar.SATURDAY: weekday = 5; break;
            case Calendar.SUNDAY: weekday = 6; break;
        }
        return weekday;
    }

    public static String getGenitiveMonth(Context context, String month) {
        if (context == null) {
            Log.w(TAG, "getGenitiveMonth | context is null");
            return month;
        }
        switch (month) {
            case "01": month = context.getString(R.string.january_genitive); break;
            case "02": month = context.getString(R.string.february_genitive); break;
            case "03": month = context.getString(R.string.march_genitive); break;
            case "04": month = context.getString(R.string.april_genitive); break;
            case "05": month = context.getString(R.string.may_genitive); break;
            case "06": month = context.getString(R.string.june_genitive); break;
            case "07": month = context.getString(R.string.july_genitive); break;
            case "08": month = context.getString(R.string.august_genitive); break;
            case "09": month = context.getString(R.string.september_genitive); break;
            case "10": month = context.getString(R.string.october_genitive); break;
            case "11": month = context.getString(R.string.november_genitive); break;
            case "12": month = context.getString(R.string.december_genitive); break;
        }
        return month;
    }

    public static String getGenitiveMonth(Context context, int month) {
        String m = "";
        if (context == null) {
            Log.w(TAG, "getGenitiveMonth | context is null");
            return m;
        }
        switch (month) {
            case Calendar.JANUARY: m = context.getString(R.string.january_genitive); break;
            case Calendar.FEBRUARY: m = context.getString(R.string.february_genitive); break;
            case Calendar.MARCH: m = context.getString(R.string.march_genitive); break;
            case Calendar.APRIL: m = context.getString(R.string.april_genitive); break;
            case Calendar.MAY: m = context.getString(R.string.may_genitive); break;
            case Calendar.JUNE: m = context.getString(R.string.june_genitive); break;
            case Calendar.JULY: m = context.getString(R.string.july_genitive); break;
            case Calendar.AUGUST: m = context.getString(R.string.august_genitive); break;
            case Calendar.SEPTEMBER: m = context.getString(R.string.september_genitive); break;
            case Calendar.OCTOBER: m = context.getString(R.string.october_genitive); break;
            case Calendar.NOVEMBER: m = context.getString(R.string.november_genitive); break;
            case Calendar.DECEMBER: m = context.getString(R.string.december_genitive); break;
        }
        return m;
    }

    public static String getDay(Context context, int day) {
        String ret = "";
        switch (day) {
            case Calendar.MONDAY: ret = context.getString(R.string.monday); break;
            case Calendar.TUESDAY: ret = context.getString(R.string.tuesday); break;
            case Calendar.WEDNESDAY: ret = context.getString(R.string.wednesday); break;
            case Calendar.THURSDAY: ret = context.getString(R.string.thursday); break;
            case Calendar.FRIDAY: ret = context.getString(R.string.friday); break;
            case Calendar.SATURDAY: ret = context.getString(R.string.saturday); break;
            case Calendar.SUNDAY: ret = context.getString(R.string.sunday); break;
        }
        return ret;
    }

    public static String getUpdateTime(Context context, long time) {
        int shift = (int) ((getCalendar().getTimeInMillis() - time) / 1000L);
        String message;
        if (shift < 21600 && context != null) {
            if (shift < 5) {
                message = context.getString(R.string.right_now);
            } else if (shift < 60) {
                message = shift + " " + context.getString(R.string.sec_past);
            } else if (shift < 3600) {
                message = shift / 60 + " " + context.getString(R.string.min_past);
            } else {
                message = shift / 3600 + " " + context.getString(R.string.hour_past);
            }
        } else {
            message = new SimpleDateFormat("dd.MM.yyyy HH:mm", Locale.ROOT).format(new Date(time));
        }
        return message;
    }
}

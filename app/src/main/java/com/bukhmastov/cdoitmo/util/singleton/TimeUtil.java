package com.bukhmastov.cdoitmo.util.singleton;

import androidx.annotation.Nullable;

import java.util.Calendar;

public class TimeUtil {

    /**
     * "time" format is "HH:MM"
     */
    @Nullable
    public static Calendar time2calendar(Calendar calendar, String time) {
        String[] times = time.split(":");
        if (times.length < 2) {
            return null;
        }
        calendar.set(Calendar.HOUR_OF_DAY, Integer.parseInt(times[0]));
        calendar.set(Calendar.MINUTE, Integer.parseInt(times[1]));
        calendar.set(Calendar.SECOND, 0);
        return calendar;
    }
}

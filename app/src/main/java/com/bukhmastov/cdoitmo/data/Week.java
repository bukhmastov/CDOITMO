package com.bukhmastov.cdoitmo.data;

import org.json.JSONException;
import org.json.JSONObject;

import java.util.Calendar;

import static com.bukhmastov.cdoitmo.util.Storage.StorageType.*;
import static com.bukhmastov.cdoitmo.util.Time.getCalendar;

public class Week {
    private int week;
    private long timestamp;

    public Week(String week) {
        this(week, getCalendar().getTimeInMillis());
    }

    public Week(String week, Long timestamp) {
        try {
            this.week = Integer.parseInt(week);
        }
        catch (NumberFormatException e) {
            this.week = -1;
        }
        this.timestamp = timestamp;
    }

    public int getWeek() { return week; }

    public long getTimestamp() { return timestamp; }

    public void store(StorageProxy proxy) {
        try {
            proxy.put(GLOBAL, "user#week", new JSONObject().put("week", week).put("timestamp", timestamp).toString());
        }
        catch (JSONException e) {/* ignore */}
    }

    public static int getCurrent(StorageProxy proxy) {
        return getCurrent(proxy, getCalendar());
    }

    public static int getCurrent(StorageProxy proxy, Calendar calendar) {
        int week = -1;
        long ts = 0;
        try {
            final String override = proxy.get(PREFS,"pref_week_force_override");
            if (!override.isEmpty()) {
                try {
                    String[] v = override.split("#");
                    if (v.length == 2) {
                        week = Integer.parseInt(v[0]);
                        ts = Long.parseLong(v[1]);
                    }
                } catch (Exception ignore) {/* ignore */}
            }
            if (week < 0) {
                final String stored = proxy.get(GLOBAL, "user#week").trim();
                if (!stored.isEmpty()) {
                    try {
                        JSONObject json = new JSONObject(stored);
                        week = json.getInt("week");
                        ts = json.getLong("timestamp");
                    } catch (Exception e) {
                        proxy.delete(GLOBAL, "user#week");
                    }
                }
            }
            if (week >= 0) {
                final Calendar past = (Calendar) calendar.clone();
                past.setTimeInMillis(ts);
                return week + (calendar.get(Calendar.WEEK_OF_YEAR) - past.get(Calendar.WEEK_OF_YEAR));
            }
        } catch (Exception ignore) {/* ignore */}
        return week;
    }
}

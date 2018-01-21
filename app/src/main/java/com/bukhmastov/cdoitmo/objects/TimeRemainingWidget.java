package com.bukhmastov.cdoitmo.objects;

import android.content.Context;

import com.bukhmastov.cdoitmo.utils.Log;
import com.bukhmastov.cdoitmo.utils.Static;

import org.json.JSONArray;
import org.json.JSONObject;

import java.util.Calendar;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

public class TimeRemainingWidget {

    private static final String TAG = "TRWidget";
    public interface response {
        void onAction(TimeRemainingWidget.Data json);
        void onCancelled();
    }
    public class Data {
        public String current = null;
        public String current_15min = null;
        public String next = null;
        public String day = null;
    }
    private final response delegate;
    private Executor executor;

    public TimeRemainingWidget(response delegate) {
        this.delegate = delegate;
    }

    public void start(Context context, JSONObject schedule) {
        Log.v(TAG, "start");
        executor = new Executor(context, schedule);
    }

    public void stop() {
        Log.v(TAG, "stop");
        if (executor != null) {
            executor.cancel();
        }
    }

    private class Executor extends Thread {

        private static final String TAG = "TRWidget.Executor";
        private boolean running = false;
        private final long delay = 1000;
        private final JSONObject full_schedule;
        private JSONArray lessons;
        private final Context context;
        private boolean first_init = true;
        private int week = -1;
        private int weekday = -1;

        Executor(Context context, JSONObject full_schedule){
            Log.i(TAG, "started");
            this.context = context;
            this.full_schedule = full_schedule;
            this.running = true;
            start();
        }

        public void run() {
            while (!Thread.currentThread().isInterrupted() && running) {
                try {
                    long ts = System.currentTimeMillis();
                    if (ts % 3600000L <= 1000 || first_init) {
                        Log.v(TAG, "update data");
                        first_init = false;
                        week = Static.getWeek(context) % 2;
                        weekday = Static.getWeekDay();
                        lessons = null;
                        JSONArray schedule = full_schedule.getJSONArray("schedule");
                        for (int i = 0; i < schedule.length(); i++) {
                            JSONObject dayObj = schedule.getJSONObject(i);
                            if (dayObj.getInt("weekday") == weekday) {
                                lessons = dayObj.getJSONArray("lessons");
                                break;
                            }
                        }
                    }
                    if (lessons == null) throw new NullPointerException("lessons is null");
                    long current = -1;
                    long current_15min = -1;
                    long next = -1;
                    long day = -1;
                    for (int i = 0; i < lessons.length(); i++) {
                        JSONObject lesson = lessons.getJSONObject(i);
                        if ("reduced".equals(lesson.getString("cdoitmo_type"))) continue;
                        int lesson_week = lesson.getInt("week");
                        if (!(week == lesson_week || lesson_week == 2 || week < 0)) continue;
                        Matcher timeStart = Pattern.compile("^(\\d{1,2}):(\\d{2})$").matcher(lesson.getString("timeStart"));
                        Matcher timeEnd = Pattern.compile("^(\\d{1,2}):(\\d{2})$").matcher(lesson.getString("timeEnd"));
                        if (timeStart.find() && timeEnd.find()) {
                            Calendar calendarTS = Static.getCalendar();
                            calendarTS.set(Calendar.HOUR_OF_DAY, Integer.parseInt(timeStart.group(1)));
                            calendarTS.set(Calendar.MINUTE, Integer.parseInt(timeStart.group(2)));
                            calendarTS.set(Calendar.SECOND, 0);
                            Calendar calendarTE = Static.getCalendar();
                            calendarTE.set(Calendar.HOUR_OF_DAY, Integer.parseInt(timeEnd.group(1)));
                            calendarTE.set(Calendar.MINUTE, Integer.parseInt(timeEnd.group(2)));
                            calendarTE.set(Calendar.SECOND, 0);
                            long timestampTS = calendarTS.getTimeInMillis();
                            long timestampTE = calendarTE.getTimeInMillis();
                            if (ts >= timestampTS && ts <= timestampTE) {
                                current = timestampTE - ts;
                                current_15min = 15 * 60000 - (ts - timestampTS);
                            }
                            if (next == -1 && ts < timestampTS) {
                                next = timestampTS - ts;
                            }
                            if (timestampTE > day) {
                                day = timestampTE;
                            }
                        }
                    }
                    day = day - ts;
                    day = day < 0 ? -1 : day;
                    Data data = new Data();
                    if (current >= 0) data.current = ts2date(current);
                    if (current_15min >= 0) data.current_15min = ts2date(current_15min);
                    if (next >= 0) data.next = ts2date(next);
                    if (day >= 0) data.day = ts2date(day);
                    delegate.onAction(data);
                    long delta = delay - (System.currentTimeMillis() - ts);
                    try {
                        Thread.sleep(delta > 0 ? delta : 1);
                    } catch (InterruptedException e) {
                        break;
                    }
                } catch (Exception e) {
                    Static.error(e);
                    this.cancel();
                }
            }
            if (!Thread.currentThread().isInterrupted() || running) this.cancel();
        }
        public void cancel() {
            running = false;
            interrupt();
            delegate.onCancelled();
            Log.i(TAG, "interrupted");
        }
        private String ts2date(long ts) {
            int time = (int) (ts / 1000L);
            int hours = time / 3600;
            int minutes = (time - hours * 3600) / 60;
            int seconds = (time - hours * 3600 - minutes * 60) % 60;
            String response;
            if (minutes > 0 || hours > 0) {
                response = Static.ldgZero(seconds);
            } else {
                response = String.valueOf(seconds);
            }
            if (hours > 0) {
                response = Static.ldgZero(minutes) + ":" + response;
            } else {
                if (minutes > 0) {
                    response = String.valueOf(minutes) + ":" + response;
                }
            }
            if (hours > 0) {
                response = String.valueOf(hours) + ":" + response;
            }
            return response;
        }
    }
}

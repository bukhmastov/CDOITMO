package com.bukhmastov.cdoitmo.object.impl;

import android.content.Context;

import com.bukhmastov.cdoitmo.factory.AppComponentProvider;
import com.bukhmastov.cdoitmo.object.TimeRemainingWidget;
import com.bukhmastov.cdoitmo.util.Log;
import com.bukhmastov.cdoitmo.util.Time;
import com.bukhmastov.cdoitmo.util.TextUtils;

import org.json.JSONArray;
import org.json.JSONObject;

import java.util.Calendar;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

import javax.inject.Inject;

public class TimeRemainingWidgetImpl implements TimeRemainingWidget {

    private static final String TAG = "TRWidget";
    private Executor executor;

    @Inject
    Log log;
    @Inject
    Time time;
    @Inject
    TextUtils textUtils;

    public TimeRemainingWidgetImpl() {
        AppComponentProvider.getComponent().inject(this);
    }

    @Override
    public void start(Context context, Delegate delegate, JSONObject schedule) {
        log.v(TAG, "start");
        executor = new Executor(context, delegate, schedule);
    }

    @Override
    public void stop() {
        log.v(TAG, "stop");
        if (executor != null) {
            executor.cancel();
        }
    }

    private class Executor extends Thread {

        private static final String TAG = "TRWidget.Executor";
        private final Context context;
        private final Delegate delegate;
        private final JSONObject full_schedule;
        private final long delay = 1000;
        private boolean running = false;
        private JSONArray lessons;
        private boolean first_init = true;
        private int week = -1;
        private int weekday = -1;

        Executor(Context context, Delegate delegate, JSONObject full_schedule){
            log.i(TAG, "started");
            this.context = context;
            this.delegate = delegate;
            this.full_schedule = full_schedule;
            this.running = true;
            start();
        }

        public void run() {
            while (!Thread.currentThread().isInterrupted() && running) {
                try {
                    long ts = System.currentTimeMillis();
                    if (ts % 3600000L <= 1000 || first_init) {
                        log.v(TAG, "update data");
                        first_init = false;
                        week = time.getWeek(context) % 2;
                        weekday = time.getWeekDay();
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
                            Calendar calendarTS = time.getCalendar();
                            calendarTS.set(Calendar.HOUR_OF_DAY, Integer.parseInt(timeStart.group(1)));
                            calendarTS.set(Calendar.MINUTE, Integer.parseInt(timeStart.group(2)));
                            calendarTS.set(Calendar.SECOND, 0);
                            Calendar calendarTE = time.getCalendar();
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
                    log.exception(e);
                    this.cancel();
                }
            }
            if (!Thread.currentThread().isInterrupted() || running) this.cancel();
        }

        public void cancel() {
            running = false;
            interrupt();
            delegate.onCancelled();
            log.i(TAG, "interrupted");
        }

        private String ts2date(long ts) {
            int time = (int) (ts / 1000L);
            int hours = time / 3600;
            int minutes = (time - hours * 3600) / 60;
            int seconds = (time - hours * 3600 - minutes * 60) % 60;
            String response;
            if (minutes > 0 || hours > 0) {
                response = textUtils.ldgZero(seconds);
            } else {
                response = String.valueOf(seconds);
            }
            if (hours > 0) {
                response = textUtils.ldgZero(minutes) + ":" + response;
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

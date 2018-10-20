package com.bukhmastov.cdoitmo.object.impl;

import android.content.Context;

import com.bukhmastov.cdoitmo.factory.AppComponentProvider;
import com.bukhmastov.cdoitmo.model.schedule.lessons.SDay;
import com.bukhmastov.cdoitmo.model.schedule.lessons.SLesson;
import com.bukhmastov.cdoitmo.model.schedule.lessons.SLessons;
import com.bukhmastov.cdoitmo.object.TimeRemainingWidget;
import com.bukhmastov.cdoitmo.util.Log;
import com.bukhmastov.cdoitmo.util.TextUtils;
import com.bukhmastov.cdoitmo.util.Time;

import java.util.ArrayList;
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
    Context context;
    @Inject
    Time time;
    @Inject
    TextUtils textUtils;

    public TimeRemainingWidgetImpl() {
        AppComponentProvider.getComponent().inject(this);
    }

    @Override
    public void start(SLessons schedule, Delegate delegate) {
        log.v(TAG, "start");
        executor = new Executor(schedule, delegate);
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
        private final Delegate delegate;
        private final SLessons schedule;
        private final long delay = 1000;
        private ArrayList<SLesson> lessons;
        private boolean running;
        private boolean firstInit;
        private int week = -1;
        private int weekday = -1;

        Executor(SLessons schedule, Delegate delegate){
            log.i(TAG, "started");
            this.delegate = delegate;
            this.schedule = schedule;
            this.lessons = new ArrayList<>();
            this.running = true;
            this.firstInit = true;
            start();
        }

        public void run() {
            while (!Thread.currentThread().isInterrupted() && running) {
                try {
                    long ts = System.currentTimeMillis();
                    if (ts % 3600000L <= 1000 || firstInit) {
                        log.v(TAG, "update data");
                        firstInit = false;
                        week = time.getWeek(context) % 2;
                        weekday = time.getWeekDay();
                        lessons.clear();
                        for (SDay day : schedule.getSchedule()) {
                            if (day.getWeekday() != weekday) {
                                continue;
                            }
                            lessons.addAll(day.getLessons());
                            break;
                        }
                    }
                    long current = -1;
                    long current15min = -1;
                    long next = -1;
                    long day = -1;
                    for (SLesson lesson : lessons) {
                        if ("reduced".equals(lesson.getCdoitmoType())) {
                            continue;
                        }
                        if (!(week == lesson.getParity() || lesson.getParity() == 2 || week < 0)) {
                            continue;
                        }
                        Matcher timeStart = Pattern.compile("^(\\d{1,2}):(\\d{2})$").matcher(lesson.getTimeStart());
                        Matcher timeEnd = Pattern.compile("^(\\d{1,2}):(\\d{2})$").matcher(lesson.getTimeEnd());
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
                                current15min = 15 * 60000 - (ts - timestampTS);
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
                    if (current >= 0) {
                        data.current = ts2date(current);
                    }
                    if (current15min >= 0) {
                        data.current15min = ts2date(current15min);
                    }
                    if (next >= 0) {
                        data.next = ts2date(next);
                    }
                    if (day >= 0) {
                        data.day = ts2date(day);
                    }
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
            if (!Thread.currentThread().isInterrupted() || running) {
                this.cancel();
            }
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

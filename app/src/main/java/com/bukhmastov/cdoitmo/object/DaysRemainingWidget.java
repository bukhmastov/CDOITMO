package com.bukhmastov.cdoitmo.object;

import android.content.Context;

import com.bukhmastov.cdoitmo.util.Log;
import com.bukhmastov.cdoitmo.util.Static;

import org.json.JSONArray;
import org.json.JSONObject;

import java.util.ArrayList;
import java.util.Calendar;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

public class DaysRemainingWidget {

    private static final String TAG = "DRWidget";
    public interface response {
        void onAction(ArrayList<DaysRemainingWidget.Data> data);
        void onCancelled();
    }
    public class Data {
        public String subject = null;
        public String desc = null;
        public Time time = null;
    }
    public class Time {
        public String day = null;
        public String hour = null;
        public String min = null;
        public String sec = null;
    }
    private final response delegate;
    private Executor executor;

    public DaysRemainingWidget(response delegate) {
        this.delegate = delegate;
    }

    public void start(Context context, JSONObject schedule) {
        Log.v(TAG, "start");
        executor = new Executor(schedule);
    }

    public void stop() {
        Log.v(TAG, "stop");
        if (executor != null) {
            executor.cancel();
        }
    }

    private class Executor extends Thread {

        private static final String TAG = "DRWidget.Executor";
        private boolean running = false;
        private final long delay = 1000;
        private final JSONObject full_schedule;

        Executor(JSONObject full_schedule){
            Log.i(TAG, "started");
            this.full_schedule = full_schedule;
            this.running = true;
            start();
        }

        public void run() {
            while (!Thread.currentThread().isInterrupted() && running) {
                try {
                    final long ts = System.currentTimeMillis();
                    final ArrayList<DaysRemainingWidget.Data> dataArray = new ArrayList<>();
                    final JSONArray schedule = full_schedule.getJSONArray("schedule");
                    final String schedule_type = full_schedule.getString("type");
                    for (int i = 0; i < schedule.length(); i++) {
                        try {
                            final JSONObject fullExam = schedule.getJSONObject(i);
                            final JSONObject exam = fullExam.getJSONObject("exam");
                            final String subject = fullExam.has("subject") ? fullExam.getString("subject") : "";
                            final String group = fullExam.has("group") ? fullExam.getString("group") : "";
                            final String teacher = fullExam.has("teacher") ? fullExam.getString("teacher") : "";
                            String time = exam.getString("time");
                            String date = exam.getString("date");
                            // convert "10 янв" date to "10.01"
                            Matcher originDateMatcher = Pattern.compile("^(\\d{1,2})(\\D*)$").matcher(date);
                            if (originDateMatcher.find()) {
                                String day = originDateMatcher.group(1);
                                String month = originDateMatcher.group(2).trim();
                                if (month.startsWith("янв")) month = "01";
                                if (month.startsWith("фев")) month = "02";
                                if (month.startsWith("мар")) month = "03";
                                if (month.startsWith("апр")) month = "04";
                                if (month.startsWith("май")) month = "05";
                                if (month.startsWith("июн")) month = "06";
                                if (month.startsWith("июл")) month = "07";
                                if (month.startsWith("авг")) month = "08";
                                if (month.startsWith("сен")) month = "09";
                                if (month.startsWith("окт")) month = "10";
                                if (month.startsWith("ноя")) month = "11";
                                if (month.startsWith("дек")) month = "12";
                                date = day + "." + month;
                            }
                            // verify time and date
                            Matcher timeMatcher = Pattern.compile("^(\\d{1,2}):(\\d{2})$").matcher(time);
                            Matcher dateMatcher = Pattern.compile("^(\\d{1,2})\\.(\\d{2})(\\.(\\d{4}))?$").matcher(date);
                            if (timeMatcher.find() && dateMatcher.find()) {
                                // build calendar instance
                                final Calendar calendar = Static.getCalendar();
                                int year = calendar.get(Calendar.YEAR);
                                int month = Integer.parseInt(dateMatcher.group(2));
                                String yearStr = dateMatcher.groupCount() == 4 ? dateMatcher.group(4) : null;
                                if (yearStr != null) {
                                    year = Integer.parseInt(yearStr);
                                } else {
                                    int m = calendar.get(Calendar.MONTH);
                                    if (m > Calendar.AUGUST && m <= Calendar.DECEMBER && !(month > 9)) {
                                        year += 1;
                                    }
                                }
                                calendar.set(Calendar.YEAR, year);
                                calendar.set(Calendar.MONTH, month - 1);
                                calendar.set(Calendar.DAY_OF_MONTH, Integer.parseInt(dateMatcher.group(1)));
                                calendar.set(Calendar.HOUR_OF_DAY, Integer.parseInt(timeMatcher.group(1)));
                                calendar.set(Calendar.MINUTE, Integer.parseInt(timeMatcher.group(2)));
                                calendar.set(Calendar.SECOND, 0);
                                long examTS = calendar.getTimeInMillis();
                                if (ts < examTS) {
                                    // add exam that not yet passed
                                    Data data = new Data();
                                    data.subject = subject;
                                    data.desc = schedule_type.equals("teacher") ? group : teacher;
                                    data.time = ts2time(examTS - ts);
                                    dataArray.add(data);
                                }
                            } else {
                                throw new Exception("Invalid date/time: " + date + "/" + time);
                            }
                        } catch (Exception e) {
                            Log.e(TAG, e.getMessage());
                        }
                    }
                    delegate.onAction(dataArray);
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
    }
    private Time ts2time(long ts) {
        Time time = new Time();
        int elapsed = (int) (ts / 1000L);
        int days = elapsed / 86400;
        int hours = (elapsed - days * 86400) / 3600;
        int minutes = (elapsed - days * 86400 - hours * 3600) / 60;
        int seconds = (elapsed - days * 86400 - hours * 3600 - minutes * 60) % 60;
        time.day = days > 0 ? String.valueOf(days) : null;
        time.hour = days > 0 || hours > 0 ? String.valueOf(hours) : null;
        time.min = days > 0 || hours > 0 || minutes > 0 ? String.valueOf(minutes) : null;
        time.sec = days > 0 || hours > 0 || minutes > 0 || seconds > 0 ? String.valueOf(seconds) : null;
        return time;
    }
}

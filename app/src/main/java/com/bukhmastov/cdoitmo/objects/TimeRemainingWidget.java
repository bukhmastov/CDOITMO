package com.bukhmastov.cdoitmo.objects;

import android.content.Context;
import android.util.Log;

import com.bukhmastov.cdoitmo.utils.Static;
import com.bukhmastov.cdoitmo.utils.Storage;

import org.json.JSONArray;
import org.json.JSONException;
import org.json.JSONObject;

import java.util.Calendar;
import java.util.Objects;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

public class TimeRemainingWidget {

    public interface response {
        void onAction(TimeRemainingWidget.Data json);
        void onCancelled();
    }
    public class Data {
        public String current = null;
        public String next = null;
        public String day = null;
    }
    private static final String TAG = "TimeRemainingWidget";
    private response delegate;
    private Executor executor;

    public TimeRemainingWidget(response delegate){
        this.delegate = delegate;
    }

    public void start(Context context, JSONObject schedule){
        executor = new Executor(context, schedule);
    }

    public void stop(){
        if (executor != null) {
            executor.cancel();
        }
    }

    private class Executor extends Thread {

        private boolean running = false;
        private long delay = 1000;
        private JSONObject full_schedule;
        private JSONArray lessons;
        private Context context;
        private boolean first_init = true;
        private int week = -1;
        private int day_of_the_week = -1;

        Executor(Context context, JSONObject full_schedule){
            Log.i(TAG, "Executor thread started");
            this.context = context;
            this.full_schedule = full_schedule;
            this.running = true;
            start();
        }
        public void run(){
            while (!Thread.currentThread().isInterrupted() && running) {
                try {
                    long ts = System.currentTimeMillis();
                    if (ts % 3600000L <= 1000 || first_init) {
                        Log.d(TAG, "Update data");
                        first_init = false;
                        week = getWeek() % 2;
                        day_of_the_week = getDayOfTheWeek(Calendar.getInstance().get(Calendar.DAY_OF_WEEK));
                        lessons = null;
                        JSONArray schedule = full_schedule.getJSONArray("schedule");
                        for (int i = 0; i < schedule.length(); i++) {
                            JSONObject dayObj = schedule.getJSONObject(i);
                            if (dayObj.getInt("index") == day_of_the_week) {
                                lessons = dayObj.getJSONArray("lessons");
                                break;
                            }
                        }
                    }
                    if (lessons == null) throw new Exception("lessons is null");
                    long current = -1;
                    long next = -1;
                    long day = -1;
                    for (int i = 0; i < lessons.length(); i++) {
                        JSONObject lesson = lessons.getJSONObject(i);
                        int lesson_week = lesson.getInt("week");
                        if (!(week == lesson_week || lesson_week == 2 || week < 0)) continue;
                        Matcher timeStart = Pattern.compile("^(\\d{1,2}):(\\d{2})$").matcher(lesson.getString("timeStart"));
                        Matcher timeEnd = Pattern.compile("^(\\d{1,2}):(\\d{2})$").matcher(lesson.getString("timeEnd"));
                        if (timeStart.find() && timeEnd.find()) {
                            Calendar calendarTS = Calendar.getInstance();
                            calendarTS.set(Calendar.HOUR_OF_DAY, Integer.parseInt(timeStart.group(1)));
                            calendarTS.set(Calendar.MINUTE, Integer.parseInt(timeStart.group(2)));
                            calendarTS.set(Calendar.SECOND, 0);
                            Calendar calendarTE = Calendar.getInstance();
                            calendarTE.set(Calendar.HOUR_OF_DAY, Integer.parseInt(timeEnd.group(1)));
                            calendarTE.set(Calendar.MINUTE, Integer.parseInt(timeEnd.group(2)));
                            calendarTE.set(Calendar.SECOND, 0);
                            long timestampTS = calendarTS.getTimeInMillis();
                            long timestampTE = calendarTE.getTimeInMillis();
                            if (ts >= timestampTS && ts <= timestampTE) {
                                current = timestampTE - ts;
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
                    //Log.v(TAG, "Step | current=" + ts2date(current) + " | next=" + ts2date(next) + " | day=" + ts2date(day));
                    Data data = new Data();
                    if (current >= 0) data.current = ts2date(current);
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
        public void cancel(){
            running = false;
            interrupt();
            delegate.onCancelled();
            Log.i(TAG, "Executor thread interrupted");
        }
        private int getWeek(){
            try {
                String weekStr = Storage.file.perm.get(context, "user#week");
                if (!Objects.equals(weekStr, "")) {
                    JSONObject jsonObject = new JSONObject(weekStr);
                    int week = jsonObject.getInt("week");
                    if (week >= 0){
                        Calendar past = Calendar.getInstance();
                        past.setTimeInMillis(jsonObject.getLong("timestamp"));
                        return week + (Calendar.getInstance().get(Calendar.WEEK_OF_YEAR) - past.get(Calendar.WEEK_OF_YEAR));
                    }
                }
            } catch (JSONException e) {
                Storage.file.perm.delete(context, "user#week");
                return -1;
            }
            return -1;
        }
        private int getDayOfTheWeek(int day_of_the_week){
            switch (day_of_the_week) {
                case Calendar.MONDAY:
                    day_of_the_week = 0;
                    break;
                case Calendar.TUESDAY:
                    day_of_the_week = 1;
                    break;
                case Calendar.WEDNESDAY:
                    day_of_the_week = 2;
                    break;
                case Calendar.THURSDAY:
                    day_of_the_week = 3;
                    break;
                case Calendar.FRIDAY:
                    day_of_the_week = 4;
                    break;
                case Calendar.SATURDAY:
                    day_of_the_week = 5;
                    break;
                case Calendar.SUNDAY:
                    day_of_the_week = 6;
                    break;
                default:
                    day_of_the_week = -1;
                    break;
            }
            return day_of_the_week;
        }
        private String ts2date(long ts){
            int time = (int) (ts / 1000L);
            int hours = time / 3600;
            int minutes = (time - hours * 3600) / 60;
            int seconds = (time - hours * 3600 - minutes * 60) % 60;

            String response;
            if (minutes > 0 || hours > 0) {
                response = ldgZero(seconds);
            } else {
                response = String.valueOf(seconds);
            }
            if (hours > 0) {
                response = ldgZero(minutes) + ":" + response;
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
        private String ldgZero(int time){
            return time % 10 == time ? "0" + String.valueOf(time) : String.valueOf(time);
        }
    }

}

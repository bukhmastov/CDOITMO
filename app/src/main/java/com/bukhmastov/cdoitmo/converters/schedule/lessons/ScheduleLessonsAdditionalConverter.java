package com.bukhmastov.cdoitmo.converters.schedule.lessons;

import android.content.Context;

import com.bukhmastov.cdoitmo.exceptions.SilentException;
import com.bukhmastov.cdoitmo.objects.schedule.ScheduleLessons;
import com.bukhmastov.cdoitmo.utils.Log;
import com.bukhmastov.cdoitmo.utils.Static;
import com.bukhmastov.cdoitmo.utils.Storage;

import org.json.JSONArray;
import org.json.JSONException;
import org.json.JSONObject;

public class ScheduleLessonsAdditionalConverter extends ScheduleConverter {

    private static final String TAG = "SLAdditionalConverter";
    public interface response {
        void finish(JSONObject json);
    }
    private final Context context;
    private final response delegate;
    private final JSONObject data;

    public ScheduleLessonsAdditionalConverter(Context context, JSONObject data, response delegate) {
        this.context = context;
        this.data = data;
        this.delegate = delegate;
    }

    @Override
    public void run() {
        Log.v(TAG, "converting");
        try {
            if (!data.has("query") || !data.has("schedule")) {
                throw new SilentException();
            }
            final String query = data.getString("query");
            if (query == null || query.isEmpty()) {
                throw new SilentException();
            }
            final String token = query.toLowerCase();
            final JSONArray schedule = data.getJSONArray("schedule");
            final JSONArray scheduleAdded = string2jsonArray(Storage.file.perm.get(context, "schedule_lessons#added#" + token, ""));
            final JSONArray scheduleReduced = string2jsonArray(Storage.file.perm.get(context, "schedule_lessons#reduced#" + token, ""));
            if (scheduleAdded.length() > 0 || scheduleReduced.length() > 0) {
                for (int i = 0; i < 7; i++) {
                    final JSONObject day = schedule.getJSONObject(i);
                    if (day == null) continue;
                    final int weekday = day.getInt("weekday");
                    final JSONArray lessons = day.getJSONArray("lessons");
                    if (scheduleReduced.length() > 0 && lessons.length() > 0) {
                        for (int j = 0; j < lessons.length(); j++) {
                            final JSONObject lesson = lessons.getJSONObject(j);
                            final String signature = ScheduleLessons.getLessonHash(lesson);
                            for (int k = 0; k < scheduleReduced.length(); k++) {
                                final JSONObject dayReduced = scheduleReduced.getJSONObject(k);
                                if (dayReduced.getInt("weekday") != weekday) continue;
                                final JSONArray lessonsReduced = dayReduced.getJSONArray("lessons");
                                for (int a = 0; a < lessonsReduced.length(); a++) {
                                    if (lessonsReduced.getString(a).equals(signature)) {
                                        lesson.put("cdoitmo_type", "reduced");
                                        break;
                                    }
                                }
                                break;
                            }
                        }
                    }
                    if (scheduleAdded.length() > 0) {
                        for (int j = 0; j < scheduleAdded.length(); j++) {
                            final JSONObject dayAdded = scheduleAdded.getJSONObject(j);
                            if (dayAdded.getInt("weekday") != weekday) continue;
                            final JSONArray lessonsAdded = dayAdded.getJSONArray("lessons");
                            if (lessonsAdded.length() > 0) {
                                for (int k = 0; k < lessonsAdded.length(); k++) {
                                    lessons.put(lessonsAdded.getJSONObject(k));
                                }
                                day.put("lessons", sortLessonsByTime(lessons));
                            }
                        }
                    }
                }
            }
            Log.v(TAG, "converted");
        } catch (SilentException e) {
            Log.v(TAG, "conversion failed with SilentException");
        } catch (Exception e) {
            Log.v(TAG, "conversion failed");
            Static.error(e);
        }
        delegate.finish(data);
    }

    private JSONArray string2jsonArray(String text) throws JSONException {
        if (text == null || text.isEmpty()) {
            return new JSONArray();
        } else {
            return new JSONArray(text);
        }
    }
}

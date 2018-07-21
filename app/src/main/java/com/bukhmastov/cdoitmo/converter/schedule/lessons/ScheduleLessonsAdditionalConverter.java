package com.bukhmastov.cdoitmo.converter.schedule.lessons;

import android.content.Context;

import com.bukhmastov.cdoitmo.exception.SilentException;
import com.bukhmastov.cdoitmo.factory.AppComponentProvider;
import com.bukhmastov.cdoitmo.firebase.FirebasePerformanceProvider;
import com.bukhmastov.cdoitmo.object.schedule.ScheduleLessonsHelper;
import com.bukhmastov.cdoitmo.util.Storage;

import org.json.JSONArray;
import org.json.JSONException;
import org.json.JSONObject;

import javax.inject.Inject;

public class ScheduleLessonsAdditionalConverter extends ScheduleConverter {

    private final Context context;
    private final JSONObject data;

    @Inject
    Storage storage;
    @Inject
    ScheduleLessonsHelper scheduleLessonsHelper;

    public ScheduleLessonsAdditionalConverter(Context context, JSONObject data, Response delegate) {
        super(delegate);
        AppComponentProvider.getComponent().inject(this);
        this.context = context;
        this.data = data;
    }

    @Override
    protected JSONObject convert() throws Throwable {
        if (!data.has("query") || !data.has("schedule")) {
            throw new SilentException();
        }
        final String query = data.getString("query");
        if (query == null || query.isEmpty()) {
            throw new SilentException();
        }
        final String token = query.toLowerCase();
        final JSONArray schedule = data.getJSONArray("schedule");
        final JSONArray scheduleAdded = string2jsonArray(storage.get(context, Storage.PERMANENT, Storage.USER, "schedule_lessons#added#" + token, ""));
        final JSONArray scheduleReduced = string2jsonArray(storage.get(context, Storage.PERMANENT, Storage.USER, "schedule_lessons#reduced#" + token, ""));
        if (scheduleAdded.length() > 0 || scheduleReduced.length() > 0) {
            for (int i = 0; i < schedule.length(); i++) {
                final JSONObject day = schedule.getJSONObject(i);
                if (day == null) continue;
                final int weekday = day.getInt("weekday");
                final JSONArray lessons = day.getJSONArray("lessons");
                if (scheduleReduced.length() > 0 && lessons.length() > 0) {
                    for (int j = 0; j < lessons.length(); j++) {
                        final JSONObject lesson = lessons.getJSONObject(j);
                        final String signature = scheduleLessonsHelper.getLessonHash(lesson);
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
        return data;
    }

    @Override
    protected String getTraceName() {
        return FirebasePerformanceProvider.Trace.Convert.Schedule.ADDITIONAL;
    }

    private JSONArray string2jsonArray(String text) throws JSONException {
        if (text == null || text.isEmpty()) {
            return new JSONArray();
        } else {
            return new JSONArray(text);
        }
    }
}

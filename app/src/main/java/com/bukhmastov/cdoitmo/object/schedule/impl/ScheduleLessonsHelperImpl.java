package com.bukhmastov.cdoitmo.object.schedule.impl;

import android.content.Context;
import android.os.Bundle;

import com.bukhmastov.cdoitmo.activity.ConnectedActivity;
import com.bukhmastov.cdoitmo.factory.AppComponentProvider;
import com.bukhmastov.cdoitmo.firebase.FirebaseAnalyticsProvider;
import com.bukhmastov.cdoitmo.fragment.ScheduleLessonsModifyFragment;
import com.bukhmastov.cdoitmo.interfaces.Callable;
import com.bukhmastov.cdoitmo.object.schedule.ScheduleLessonsHelper;
import com.bukhmastov.cdoitmo.util.Log;
import com.bukhmastov.cdoitmo.util.Storage;
import com.bukhmastov.cdoitmo.util.Thread;
import com.bukhmastov.cdoitmo.util.TextUtils;

import org.json.JSONArray;
import org.json.JSONException;
import org.json.JSONObject;

import javax.inject.Inject;

public class ScheduleLessonsHelperImpl implements ScheduleLessonsHelper {

    private static final String TAG = "ScheduleLessonsHelper";

    @Inject
    Log log;
    @Inject
    Thread thread;
    @Inject
    TextUtils textUtils;
    @Inject
    FirebaseAnalyticsProvider firebaseAnalyticsProvider;

    public ScheduleLessonsHelperImpl() {
        AppComponentProvider.getComponent().inject(this);
    }

    @Override
    public boolean clearChanges(final Context context, final Storage storage, final String query, final Callable callback) {
        try {
            if (context == null) throw new NullPointerException("context cannot be null");
            if (query == null) throw new NullPointerException("query cannot be null");
            if (query.isEmpty()) throw new IllegalArgumentException("query cannot be empty");
            log.v(TAG, "clearChanges | query=", query);
            final String token = query.toLowerCase();
            boolean added = false;
            boolean reduced = false;
            if (storage.exists(context, Storage.PERMANENT, Storage.USER, "schedule_lessons#added#" + token)) {
                added = storage.delete(context, Storage.PERMANENT, Storage.USER, "schedule_lessons#added#" + token);
                if (storage.list(context, Storage.PERMANENT, Storage.USER, "schedule_lessons#added").size() == 0) {
                    storage.clear(context, Storage.PERMANENT, Storage.USER, "schedule_lessons#added");
                }
            }
            if (storage.exists(context, Storage.PERMANENT, Storage.USER, "schedule_lessons#reduced#" + token)) {
                reduced = storage.delete(context, Storage.PERMANENT, Storage.USER, "schedule_lessons#reduced#" + token);
                if (storage.list(context, Storage.PERMANENT, Storage.USER, "schedule_lessons#reduced").size() == 0) {
                    storage.clear(context, Storage.PERMANENT, Storage.USER, "schedule_lessons#reduced");
                }
            }
            if (callback != null && (added || reduced)) {
                callback.call();
            }
            return added || reduced;
        } catch (Exception e) {
            log.exception(e);
            return false;
        }
    }

    @Override
    public boolean reduceLesson(final Context context, final Storage storage, final String query, final int weekday, final JSONObject lesson, final Callable callback) {
        try {
            if (context == null) throw new NullPointerException("context cannot be null");
            if (query == null) throw new NullPointerException("query cannot be null");
            if (query.isEmpty()) throw new IllegalArgumentException("query cannot be empty");
            if (lesson == null) throw new NullPointerException("lesson cannot be null");
            log.v(TAG, "reduceLesson | query=", query, " | weekday=", weekday, " | lesson=", lesson.toString());
            final String cdoitmo_type = lesson.getString("cdoitmo_type");
            if (!cdoitmo_type.equals("normal")) throw new Exception("wrong cdoitmo_type type: " + cdoitmo_type);
            final String token = query.toLowerCase();
            final String hash = getLessonHash(lesson);
            final JSONArray reduced = textUtils.string2jsonArray(storage.get(context, Storage.PERMANENT, Storage.USER, "schedule_lessons#reduced#" + token, ""));
            boolean found = false;
            for (int i = 0; i < reduced.length(); i++) {
                final JSONObject day = reduced.getJSONObject(i);
                if (day.getInt("weekday") == weekday) {
                    final JSONArray lessons = day.getJSONArray("lessons");
                    boolean foundLesson = false;
                    for (int j = 0; j < lessons.length(); j++) {
                        if (lessons.getString(j).equals(hash)) {
                            foundLesson = true;
                            break;
                        }
                    }
                    if (!foundLesson) lessons.put(hash);
                    found = true;
                    break;
                }
            }
            if (!found) {
                reduced.put(new JSONObject()
                        .put("weekday", weekday)
                        .put("lessons", new JSONArray().put(hash))
                );
            }
            storage.put(context, Storage.PERMANENT, Storage.USER, "schedule_lessons#reduced#" + token, reduced.toString());
            if (callback != null) {
                callback.call();
            }
            firebaseAnalyticsProvider.logEvent(
                    context,
                    FirebaseAnalyticsProvider.Event.SCHEDULE_LESSON_REDUCE,
                    firebaseAnalyticsProvider.getBundle(FirebaseAnalyticsProvider.Param.LESSON_TITLE, lesson.getString("subject"))
            );
            return true;
        } catch (Exception e) {
            log.exception(e);
            return false;
        }
    }

    @Override
    public boolean restoreLesson(final Context context, final Storage storage, final String query, final int weekday, final JSONObject lesson, final Callable callback) {
        try {
            if (context == null) throw new NullPointerException("context cannot be null");
            if (query == null) throw new NullPointerException("query cannot be null");
            if (query.isEmpty()) throw new IllegalArgumentException("query cannot be empty");
            if (lesson == null) throw new NullPointerException("lesson cannot be null");
            log.v(TAG, "restoreLesson | query=", query, " | weekday=", weekday, " | lesson=", lesson.toString());
            final String cdoitmo_type = lesson.getString("cdoitmo_type");
            if (!cdoitmo_type.equals("reduced")) throw new Exception("wrong cdoitmo_type type: " + cdoitmo_type);
            final String token = query.toLowerCase();
            final String hash = getLessonHash(lesson);
            final JSONArray reduced = textUtils.string2jsonArray(storage.get(context, Storage.PERMANENT, Storage.USER, "schedule_lessons#reduced#" + token, ""));
            for (int i = 0; i < reduced.length(); i++) {
                final JSONObject day = reduced.getJSONObject(i);
                if (day.getInt("weekday") == weekday) {
                    final JSONArray lessons = day.getJSONArray("lessons");
                    for (int j = 0; j < lessons.length(); j++) {
                        if (lessons.getString(j).equals(hash)) {
                            lessons.remove(j);
                            break;
                        }
                    }
                    if (lessons.length() == 0) {
                        reduced.remove(i);
                    }
                    break;
                }
            }
            if (reduced.length() == 0) {
                storage.delete(context, Storage.PERMANENT, Storage.USER, "schedule_lessons#reduced#" + token);
                if (storage.list(context, Storage.PERMANENT, Storage.USER, "schedule_lessons#reduced").size() == 0) {
                    storage.clear(context, Storage.PERMANENT, Storage.USER, "schedule_lessons#reduced");
                }
            } else {
                storage.put(context, Storage.PERMANENT, Storage.USER, "schedule_lessons#reduced#" + token, reduced.toString());
            }
            if (callback != null) {
                callback.call();
            }
            return true;
        } catch (Exception e) {
            log.exception(e);
            return false;
        }
    }

    @Override
    public boolean createLesson(final ConnectedActivity activity, final String query, final String title, final String type, final int weekday, final JSONObject lesson, final Callable callback) {
        try {
            if (activity == null) throw new NullPointerException("activity cannot be null");
            if (query == null) throw new NullPointerException("query cannot be null");
            if (query.isEmpty()) throw new IllegalArgumentException("query cannot be empty");
            if (title == null) throw new NullPointerException("title cannot be null");
            if (type == null) throw new NullPointerException("type cannot be null");
            if (lesson == null) throw new NullPointerException("lesson cannot be null");
            log.v(TAG, "createLesson | open fragment | query=", query, " | weekday=", weekday, " | lesson=", lesson.toString());
            final Bundle extras = new Bundle();
            extras.putString("action_type", ScheduleLessonsModifyFragment.CREATE);
            extras.putString("query", query);
            extras.putString("type", type);
            extras.putString("title", title);
            extras.putInt("weekday", weekday);
            extras.putString("lesson", lesson.toString());
            thread.runOnUI(() -> {
                if (activity.openActivityOrFragment(ScheduleLessonsModifyFragment.class, extras) && callback != null) {
                    thread.run(callback::call);
                }
            });
            return true;
        } catch (Exception e) {
            log.exception(e);
            return false;
        }
    }

    @Override
    public boolean createLesson(final Context context, final Storage storage, final String query, final int weekday, final JSONObject lesson, final Callable callback) {
        try {
            if (context == null) throw new NullPointerException("context cannot be null");
            if (query == null) throw new NullPointerException("query cannot be null");
            if (query.isEmpty()) throw new IllegalArgumentException("query cannot be empty");
            if (lesson == null) throw new NullPointerException("lesson cannot be null");
            log.v(TAG, "createLesson | query=", query, " | weekday=", weekday, " | lesson=", lesson.toString());
            lesson.put("cdoitmo_type", "synthetic");
            final String subject = lesson.getString("subject");
            final String token = query.toLowerCase();
            final JSONArray added = textUtils.string2jsonArray(storage.get(context, Storage.PERMANENT, Storage.USER, "schedule_lessons#added#" + token, ""));
            boolean found = false;
            for (int i = 0; i < added.length(); i++) {
                final JSONObject day = added.getJSONObject(i);
                if (day.getInt("weekday") == weekday) {
                    found = true;
                    day.getJSONArray("lessons").put(lesson);
                }
            }
            if (!found) {
                added.put(new JSONObject()
                        .put("weekday", weekday)
                        .put("lessons", new JSONArray().put(lesson))
                );
            }
            storage.put(context, Storage.PERMANENT, Storage.USER, "schedule_lessons#added#" + token, added.toString());
            firebaseAnalyticsProvider.logEvent(
                    context,
                    FirebaseAnalyticsProvider.Event.SCHEDULE_LESSON_ADD,
                    firebaseAnalyticsProvider.getBundle(FirebaseAnalyticsProvider.Param.LESSON_TITLE, subject)
            );
            if (callback != null) {
                callback.call();
            }
            return true;
        } catch (Exception e) {
            log.exception(e);
            return false;
        }
    }

    @Override
    public boolean deleteLesson(final Context context, final Storage storage, final String query, final int weekday, final JSONObject lesson, final Callable callback) {
        try {
            if (context == null) throw new NullPointerException("context cannot be null");
            if (query == null) throw new NullPointerException("query cannot be null");
            if (query.isEmpty()) throw new IllegalArgumentException("query cannot be empty");
            if (lesson == null) throw new NullPointerException("lesson cannot be null");
            log.v(TAG, "deleteLesson | query=", query, " | weekday=", weekday, " | lesson=", lesson.toString());
            final String cdoitmo_type = lesson.getString("cdoitmo_type");
            if (!cdoitmo_type.equals("synthetic")) throw new Exception("wrong cdoitmo_type type: " + cdoitmo_type);
            final String hash = getLessonHash(lesson);
            final String token = query.toLowerCase();
            final JSONArray added = textUtils.string2jsonArray(storage.get(context, Storage.PERMANENT, Storage.USER, "schedule_lessons#added#" + token, ""));
            for (int i = 0; i < added.length(); i++) {
                final JSONObject day = added.getJSONObject(i);
                if (day.getInt("weekday") == weekday) {
                    final JSONArray lessons = day.getJSONArray("lessons");
                    for (int j = 0; j < lessons.length(); j++) {
                        if (getLessonHash(lessons.getJSONObject(j)).equals(hash)) {
                            lessons.remove(j);
                            break;
                        }
                    }
                    if (lessons.length() == 0) {
                        added.remove(i);
                    }
                    break;
                }
            }
            if (added.length() == 0) {
                storage.delete(context, Storage.PERMANENT, Storage.USER, "schedule_lessons#added#" + token);
                if (storage.list(context, Storage.PERMANENT, Storage.USER, "schedule_lessons#added").size() == 0) {
                    storage.clear(context, Storage.PERMANENT, Storage.USER, "schedule_lessons#added");
                }
            } else {
                storage.put(context, Storage.PERMANENT, Storage.USER, "schedule_lessons#added#" + token, added.toString());
            }
            if (callback != null) {
                callback.call();
            }
            return true;
        } catch (Exception e) {
            log.exception(e);
            return false;
        }
    }

    @Override
    public boolean editLesson(final ConnectedActivity activity, final String query, final String title, final String type, final int weekday, final JSONObject lesson, final Callable callback) {
        try {
            if (activity == null) throw new NullPointerException("activity cannot be null");
            if (query == null) throw new NullPointerException("query cannot be null");
            if (query.isEmpty()) throw new IllegalArgumentException("query cannot be empty");
            if (title == null) throw new NullPointerException("title cannot be null");
            if (type == null) throw new NullPointerException("type cannot be null");
            if (lesson == null) throw new NullPointerException("lesson cannot be null");
            log.v(TAG, "editLesson | open fragment | query=", query, " | weekday=", weekday, " | lesson=", lesson.toString());
            final Bundle extras = new Bundle();
            extras.putString("action_type", ScheduleLessonsModifyFragment.EDIT);
            extras.putString("query", query);
            extras.putString("type", type);
            extras.putString("title", title);
            extras.putInt("weekday", weekday);
            extras.putString("lesson", lesson.toString());
            thread.runOnUI(() -> {
                if (activity.openActivityOrFragment(ScheduleLessonsModifyFragment.class, extras) && callback != null) {
                    thread.run(callback::call);
                }
            });
            return true;
        } catch (Exception e) {
            log.exception(e);
            return false;
        }
    }

    @Override
    public String getLessonHash(JSONObject lesson) throws JSONException {
        return textUtils.crypt(getLessonSignature(lesson));
    }

    @Override
    public String getLessonSignature(JSONObject lesson) throws JSONException {
        JSONObject replica = new JSONObject();
        replica.put("subject", lesson.getString("subject"));
        replica.put("type", lesson.getString("type"));
        replica.put("week", lesson.getInt("week"));
        replica.put("timeStart", lesson.getString("timeStart"));
        replica.put("timeEnd", lesson.getString("timeEnd"));
        replica.put("group", lesson.getString("group"));
        replica.put("teacher", lesson.getString("teacher"));
        replica.put("teacher_id", lesson.getString("teacher_id"));
        replica.put("room", lesson.getString("room"));
        replica.put("building", lesson.getString("building"));
        return replica.toString();
    }
}

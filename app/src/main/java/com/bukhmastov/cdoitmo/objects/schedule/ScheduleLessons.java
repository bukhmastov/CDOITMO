package com.bukhmastov.cdoitmo.objects.schedule;

import android.content.Context;
import android.os.Bundle;

import com.bukhmastov.cdoitmo.activities.ConnectedActivity;
import com.bukhmastov.cdoitmo.converters.schedule.lessons.ScheduleLessonsAdditionalConverter;
import com.bukhmastov.cdoitmo.converters.schedule.lessons.ScheduleLessonsConverterIfmo;
import com.bukhmastov.cdoitmo.converters.schedule.lessons.ScheduleLessonsConverterIsu;
import com.bukhmastov.cdoitmo.firebase.FirebaseAnalyticsProvider;
import com.bukhmastov.cdoitmo.fragments.ScheduleLessonsModifyFragment;
import com.bukhmastov.cdoitmo.network.IfmoRestClient;
import com.bukhmastov.cdoitmo.network.IsuRestClient;
import com.bukhmastov.cdoitmo.network.interfaces.RestResponseHandler;
import com.bukhmastov.cdoitmo.network.models.Client;
import com.bukhmastov.cdoitmo.utils.Log;
import com.bukhmastov.cdoitmo.utils.Static;
import com.bukhmastov.cdoitmo.utils.Storage;

import org.json.JSONArray;
import org.json.JSONObject;

public class ScheduleLessons extends Schedule {

    private static final String TAG = "ScheduleLessons";
    public static final String TYPE = "lessons";

    public ScheduleLessons(Handler handler) {
        super(handler);
    }

    @Override
    protected void searchMine(final Context context, final int refreshRate, final boolean forceToCache, final boolean withUserChanges) {
        Static.T.runThread(new Runnable() {
            @Override
            public void run() {
                Log.v(TAG, "searchMine | refreshRate=" + refreshRate + " | forceToCache=" + Static.logBoolean(forceToCache) + " | withUserChanges=" + Static.logBoolean(withUserChanges));
                searchByQuery(context, "mine", "mine", refreshRate, withUserChanges, new SearchByQuery() {
                    @Override
                    public boolean isWebAvailable() {
                        if (!IsuRestClient.isAuthorized(context)) {
                            Log.v(TAG, "searchMine | isu auth required");
                            invokePending("mine", withUserChanges, true, new Pending() {
                                @Override
                                public void invoke(Handler handler) {
                                    handler.onFailure(FAILED_MINE_NEED_ISU);
                                }
                            });
                            return false;
                        } else {
                            return true;
                        }
                    }
                    @Override
                    public void onWebRequest(final String query, final String cache, final RestResponseHandler restResponseHandler) {
                        IsuRestClient.Private.get(context, "schedule/personal/student/%key%/%token%", null, restResponseHandler);
                    }
                    @Override
                    public void onWebRequestSuccess(final String query, final JSONObject data, final JSONObject template) {
                        ScheduleLessons.this.onWebRequestSuccessIsu(this, query, data, template);
                    }
                    @Override
                    public void onWebRequestFailed(final int statusCode, final Client.Headers headers, final int state) {
                        invokePending("mine", withUserChanges, true, new Pending() {
                            @Override
                            public void invoke(Handler handler) {
                                handler.onFailure(statusCode, headers, state);
                            }
                        });
                    }
                    @Override
                    public void onWebRequestProgress(final int state) {
                        invokePending("mine", withUserChanges, false, new Pending() {
                            @Override
                            public void invoke(Handler handler) {
                                handler.onProgress(state);
                            }
                        });
                    }
                    @Override
                    public void onWebNewRequest(final Client.Request request) {
                        invokePending("mine", withUserChanges, false, new Pending() {
                            @Override
                            public void invoke(Handler handler) {
                                handler.onNewRequest(request);
                            }
                        });
                    }
                    @Override
                    public void onFound(final String query, final JSONObject data, final boolean putToCache, boolean fromCache) {
                        ScheduleLessons.this.onFound(context, query, data, putToCache, forceToCache, fromCache, withUserChanges);
                    }
                });
            }
        });
    }
    @Override
    protected void searchGroup(final Context context, final String group, final int refreshRate, final boolean forceToCache, final boolean withUserChanges) {
        Static.T.runThread(new Runnable() {
            @Override
            public void run() {
                final SOURCE source = getSource(context);
                Log.v(TAG, "searchGroup | group=" + group + " | refreshRate=" + refreshRate + " | forceToCache=" + Static.logBoolean(forceToCache) + " | withUserChanges=" + Static.logBoolean(withUserChanges) + " | source=" + source2string(source));
                searchByQuery(context, "group", group, refreshRate, withUserChanges, new SearchByQuery() {
                    @Override
                    public boolean isWebAvailable() {
                        return true;
                    }
                    @Override
                    public void onWebRequest(final String query, final String cache, final RestResponseHandler restResponseHandler) {
                        switch (source) {
                            case ISU:  IsuRestClient.Public.get(context, "schedule/common/group/%key%/" + query, null, restResponseHandler); break;
                            case IFMO: IfmoRestClient.get(context, "schedule_lesson_group/" + query, null, restResponseHandler); break;
                        }
                    }
                    @Override
                    public void onWebRequestSuccess(final String query, final JSONObject data, final JSONObject template) {
                        switch (source) {
                            case ISU:  ScheduleLessons.this.onWebRequestSuccessIsu(this, query, data, template); break;
                            case IFMO: ScheduleLessons.this.onWebRequestSuccessIfmo(this, query, data, template); break;
                        }
                    }
                    @Override
                    public void onWebRequestFailed(final int statusCode, final Client.Headers headers, final int state) {
                        invokePending(group, withUserChanges, true, new Pending() {
                            @Override
                            public void invoke(Handler handler) {
                                handler.onFailure(statusCode, headers, state);
                            }
                        });
                    }
                    @Override
                    public void onWebRequestProgress(final int state) {
                        invokePending(group, withUserChanges, false, new Pending() {
                            @Override
                            public void invoke(Handler handler) {
                                handler.onProgress(state);
                            }
                        });
                    }
                    @Override
                    public void onWebNewRequest(final Client.Request request) {
                        invokePending(group, withUserChanges, false, new Pending() {
                            @Override
                            public void invoke(Handler handler) {
                                handler.onNewRequest(request);
                            }
                        });
                    }
                    @Override
                    public void onFound(final String query, final JSONObject data, final boolean putToCache, boolean fromCache) {
                        ScheduleLessons.this.onFound(context, query, data, putToCache, forceToCache, fromCache, withUserChanges);
                    }
                });
            }
        });
    }
    @Override
    protected void searchRoom(final Context context, final String room, final int refreshRate, final boolean forceToCache, final boolean withUserChanges) {
        Static.T.runThread(new Runnable() {
            @Override
            public void run() {
                Log.v(TAG, "searchRoom | room=" + room + " | refreshRate=" + refreshRate + " | forceToCache=" + Static.logBoolean(forceToCache) + " | withUserChanges=" + Static.logBoolean(withUserChanges));
                searchByQuery(context, "room", room, refreshRate, withUserChanges, new SearchByQuery() {
                    @Override
                    public boolean isWebAvailable() {
                        return true;
                    }
                    @Override
                    public void onWebRequest(final String query, final String cache, final RestResponseHandler restResponseHandler) {
                        IfmoRestClient.get(context, "schedule_lesson_room/" + query, null, restResponseHandler);
                    }
                    @Override
                    public void onWebRequestSuccess(final String query, final JSONObject data, final JSONObject template) {
                        ScheduleLessons.this.onWebRequestSuccessIfmo(this, query, data, template);
                    }
                    @Override
                    public void onWebRequestFailed(final int statusCode, final Client.Headers headers, final int state) {
                        invokePending(room, withUserChanges, true, new Pending() {
                            @Override
                            public void invoke(Handler handler) {
                                handler.onFailure(statusCode, headers, state);
                            }
                        });
                    }
                    @Override
                    public void onWebRequestProgress(final int state) {
                        invokePending(room, withUserChanges, false, new Pending() {
                            @Override
                            public void invoke(Handler handler) {
                                handler.onProgress(state);
                            }
                        });
                    }
                    @Override
                    public void onWebNewRequest(final Client.Request request) {
                        invokePending(room, withUserChanges, false, new Pending() {
                            @Override
                            public void invoke(Handler handler) {
                                handler.onNewRequest(request);
                            }
                        });
                    }
                    @Override
                    public void onFound(final String query, final JSONObject data, final boolean putToCache, boolean fromCache) {
                        ScheduleLessons.this.onFound(context, query, data, putToCache, forceToCache, fromCache, withUserChanges);
                    }
                });
            }
        });
    }
    @Override
    protected void searchTeacher(final Context context, final String teacherId, final int refreshRate, final boolean forceToCache, final boolean withUserChanges) {
        Static.T.runThread(new Runnable() {
            @Override
            public void run() {
                final SOURCE source = getSource(context);
                Log.v(TAG, "searchTeacher | teacherId=" + teacherId + " | refreshRate=" + refreshRate + " | forceToCache=" + Static.logBoolean(forceToCache) + " | withUserChanges=" + Static.logBoolean(withUserChanges) + " | source=" + source2string(source));
                searchByQuery(context, "teacher", teacherId, refreshRate, withUserChanges, new SearchByQuery() {
                    @Override
                    public boolean isWebAvailable() {
                        return true;
                    }
                    @Override
                    public void onWebRequest(final String query, final String cache, final RestResponseHandler restResponseHandler) {
                        switch (source) {
                            case ISU:  IsuRestClient.Public.get(context, "schedule/common/teacher/%key%/" + query, null, restResponseHandler); break;
                            case IFMO: IfmoRestClient.get(context, "schedule_lesson_person/" + query, null, restResponseHandler); break;
                        }
                    }
                    @Override
                    public void onWebRequestSuccess(final String query, final JSONObject data, final JSONObject template) {
                        switch (source) {
                            case ISU:  ScheduleLessons.this.onWebRequestSuccessIsu(this, query, data, template); break;
                            case IFMO: ScheduleLessons.this.onWebRequestSuccessIfmo(this, query, data, template); break;
                        }
                    }
                    @Override
                    public void onWebRequestFailed(final int statusCode, final Client.Headers headers, final int state) {
                        invokePending(teacherId, withUserChanges, true, new Pending() {
                            @Override
                            public void invoke(Handler handler) {
                                handler.onFailure(statusCode, headers, state);
                            }
                        });
                    }
                    @Override
                    public void onWebRequestProgress(final int state) {
                        invokePending(teacherId, withUserChanges, false, new Pending() {
                            @Override
                            public void invoke(Handler handler) {
                                handler.onProgress(state);
                            }
                        });
                    }
                    @Override
                    public void onWebNewRequest(final Client.Request request) {
                        invokePending(teacherId, withUserChanges, false, new Pending() {
                            @Override
                            public void invoke(Handler handler) {
                                handler.onNewRequest(request);
                            }
                        });
                    }
                    @Override
                    public void onFound(final String query, final JSONObject data, final boolean putToCache, boolean fromCache) {
                        ScheduleLessons.this.onFound(context, query, data, putToCache, forceToCache, fromCache, withUserChanges);
                    }
                });
            }
        });
    }
    @Override
    protected String getType() {
        return TYPE;
    }
    @Override
    protected SOURCE getDefaultSource() {
        return SOURCE.IFMO;
    }

    private void onWebRequestSuccessIsu(final SearchByQuery searchByQuery, final String query, final JSONObject data, final JSONObject template) {
        Static.T.runThread(new ScheduleLessonsConverterIsu(data, template, new ScheduleLessonsConverterIsu.response() {
            @Override
            public void finish(final JSONObject json) {
                searchByQuery.onFound(query, json, true, false);
            }
        }));
    }
    private void onWebRequestSuccessIfmo(final SearchByQuery searchByQuery, final String query, final JSONObject data, final JSONObject template) {
        Static.T.runThread(new ScheduleLessonsConverterIfmo(data, template, new ScheduleLessonsConverterIsu.response() {
            @Override
            public void finish(final JSONObject json) {
                searchByQuery.onFound(query, json, true, false);
            }
        }));
    }
    private void onFound(final Context context, final String query, final JSONObject data, final boolean putToCache, final boolean forceToCache, final boolean fromCache, final boolean withUserChanges) {
        Static.T.runThread(new Runnable() {
            @Override
            public void run() {
                try {
                    boolean valid = false;
                    final JSONArray schedule = data.getJSONArray("schedule");
                    for (int i = 0; i < schedule.length(); i++) {
                        final JSONObject day = schedule.getJSONObject(i);
                        if (day != null) {
                            final JSONArray lessons = day.getJSONArray("lessons");
                            if (lessons != null && lessons.length() > 0) {
                                valid = true;
                                break;
                            }
                        }
                    }
                    if (valid) {
                        if (putToCache) {
                            putCache(context, query, data.toString(), forceToCache);
                        }
                        if (withUserChanges) {
                            new ScheduleLessonsAdditionalConverter(context, data, new ScheduleLessonsAdditionalConverter.response() {
                                @Override
                                public void finish(final JSONObject json) {
                                    invokePending(query, true, true, new Pending() {
                                        @Override
                                        public void invoke(Handler handler) {
                                            handler.onSuccess(json, fromCache);
                                        }
                                    });
                                }
                            }).run();
                        } else {
                            invokePending(query, false, true, new Pending() {
                                @Override
                                public void invoke(Handler handler) {
                                    handler.onSuccess(data, fromCache);
                                }
                            });
                        }
                    } else {
                        if (putToCache) {
                            putLocalCache(query, data.toString());
                        }
                        invokePending(query, withUserChanges, true, new Pending() {
                            @Override
                            public void invoke(Handler handler) {
                                handler.onFailure(FAILED_NOT_FOUND);
                            }
                        });
                    }
                } catch (Exception e) {
                    Static.error(e);
                    invokePending(query, withUserChanges, true, new Pending() {
                        @Override
                        public void invoke(Handler handler) {
                            handler.onFailure(FAILED_LOAD);
                        }
                    });
                }
            }
        });
    }

    public static boolean clearChanges(final Context context, final String query, final Static.SimpleCallback callback) {
        try {
            if (context == null) throw new NullPointerException("context cannot be null");
            if (query == null) throw new NullPointerException("query cannot be null");
            if (query.isEmpty()) throw new IllegalArgumentException("query cannot be empty");
            Log.v(TAG, "clearChanges | query=" + query);
            final String token = query.toLowerCase();
            boolean added = false;
            boolean reduced = false;
            if (Storage.file.perm.exists(context, "schedule_lessons#added#" + token)) {
                added = Storage.file.perm.delete(context, "schedule_lessons#added#" + token);
                if (Storage.file.perm.list(context, "schedule_lessons#added").size() == 0) {
                    Storage.file.perm.clear(context, "schedule_lessons#added");
                }
            }
            if (Storage.file.perm.exists(context, "schedule_lessons#reduced#" + token)) {
                reduced = Storage.file.perm.delete(context, "schedule_lessons#reduced#" + token);
                if (Storage.file.perm.list(context, "schedule_lessons#reduced").size() == 0) {
                    Storage.file.perm.clear(context, "schedule_lessons#reduced");
                }
            }
            if (callback != null && (added || reduced)) {
                callback.onCall();
            }
            return added || reduced;
        } catch (Exception e) {
            Static.error(e);
            return false;
        }
    }
    public static boolean reduceLesson(final Context context, final String query, final int weekday, final JSONObject lesson, final Static.SimpleCallback callback) {
        try {
            if (context == null) throw new NullPointerException("context cannot be null");
            if (query == null) throw new NullPointerException("query cannot be null");
            if (query.isEmpty()) throw new IllegalArgumentException("query cannot be empty");
            if (lesson == null) throw new NullPointerException("lesson cannot be null");
            Log.v(TAG, "reduceLesson | query=" + query + " | weekday=" + weekday + " | lesson=" + lesson.toString());
            final String cdoitmo_type = lesson.getString("cdoitmo_type");
            if (!cdoitmo_type.equals("normal")) throw new Exception("wrong cdoitmo_type type: " + cdoitmo_type);
            final String token = query.toLowerCase();
            final String hash = ScheduleLessons.getLessonHash(lesson);
            final JSONArray reduced = Static.string2jsonArray(Storage.file.perm.get(context, "schedule_lessons#reduced#" + token, ""));
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
            Storage.file.perm.put(context, "schedule_lessons#reduced#" + token, reduced.toString());
            if (callback != null) {
                callback.onCall();
            }
            FirebaseAnalyticsProvider.logEvent(
                    context,
                    FirebaseAnalyticsProvider.Event.SCHEDULE_LESSON_REDUCE,
                    FirebaseAnalyticsProvider.getBundle(FirebaseAnalyticsProvider.Param.LESSON_TITLE, lesson.getString("subject"))
            );
            return true;
        } catch (Exception e) {
            Static.error(e);
            return false;
        }
    }
    public static boolean restoreLesson(final Context context, final String query, final int weekday, final JSONObject lesson, final Static.SimpleCallback callback) {
        try {
            if (context == null) throw new NullPointerException("context cannot be null");
            if (query == null) throw new NullPointerException("query cannot be null");
            if (query.isEmpty()) throw new IllegalArgumentException("query cannot be empty");
            if (lesson == null) throw new NullPointerException("lesson cannot be null");
            Log.v(TAG, "restoreLesson | query=" + query + " | weekday=" + weekday + " | lesson=" + lesson.toString());
            final String cdoitmo_type = lesson.getString("cdoitmo_type");
            if (!cdoitmo_type.equals("reduced")) throw new Exception("wrong cdoitmo_type type: " + cdoitmo_type);
            final String token = query.toLowerCase();
            final String hash = ScheduleLessons.getLessonHash(lesson);
            final JSONArray reduced = Static.string2jsonArray(Storage.file.perm.get(context, "schedule_lessons#reduced#" + token, ""));
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
                Storage.file.perm.delete(context, "schedule_lessons#reduced#" + token);
                if (Storage.file.perm.list(context, "schedule_lessons#reduced").size() == 0) {
                    Storage.file.perm.clear(context, "schedule_lessons#reduced");
                }
            } else {
                Storage.file.perm.put(context, "schedule_lessons#reduced#" + token, reduced.toString());
            }
            if (callback != null) {
                callback.onCall();
            }
            return true;
        } catch (Exception e) {
            Static.error(e);
            return false;
        }
    }
    public static boolean createLesson(final ConnectedActivity activity, final String query, final String title, final String type, final int weekday, final JSONObject lesson, final Static.SimpleCallback callback) {
        try {
            if (activity == null) throw new NullPointerException("activity cannot be null");
            if (query == null) throw new NullPointerException("query cannot be null");
            if (query.isEmpty()) throw new IllegalArgumentException("query cannot be empty");
            if (title == null) throw new NullPointerException("title cannot be null");
            if (type == null) throw new NullPointerException("type cannot be null");
            if (lesson == null) throw new NullPointerException("lesson cannot be null");
            Log.v(TAG, "createLesson | open fragment | query=" + query + " | weekday=" + weekday + " | lesson=" + lesson.toString());
            final Bundle extras = new Bundle();
            extras.putSerializable("action_type", ScheduleLessonsModifyFragment.TYPE.create);
            extras.putString("query", query);
            extras.putString("type", type);
            extras.putString("title", title);
            extras.putInt("weekday", weekday);
            extras.putString("lesson", lesson.toString());
            Static.T.runOnUiThread(new Runnable() {
                @Override
                public void run() {
                    if (activity.openActivityOrFragment(ScheduleLessonsModifyFragment.class, extras) && callback != null) {
                        Static.T.runThread(new Runnable() {
                            @Override
                            public void run() {
                                callback.onCall();
                            }
                        });
                    }
                }
            });
            return true;
        } catch (Exception e) {
            Static.error(e);
            return false;
        }
    }
    public static boolean createLesson(final Context context, final String query, final int weekday, final JSONObject lesson, final Static.SimpleCallback callback) {
        try {
            if (context == null) throw new NullPointerException("context cannot be null");
            if (query == null) throw new NullPointerException("query cannot be null");
            if (query.isEmpty()) throw new IllegalArgumentException("query cannot be empty");
            if (lesson == null) throw new NullPointerException("lesson cannot be null");
            Log.v(TAG, "createLesson | query=" + query + " | weekday=" + weekday + " | lesson=" + lesson.toString());
            lesson.put("cdoitmo_type", "synthetic");
            final String subject = lesson.getString("subject");
            final String token = query.toLowerCase();
            final JSONArray added = Static.string2jsonArray(Storage.file.perm.get(context, "schedule_lessons#added#" + token, ""));
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
            Storage.file.perm.put(context, "schedule_lessons#added#" + token, added.toString());
            FirebaseAnalyticsProvider.logEvent(
                    context,
                    FirebaseAnalyticsProvider.Event.SCHEDULE_LESSON_ADD,
                    FirebaseAnalyticsProvider.getBundle(FirebaseAnalyticsProvider.Param.LESSON_TITLE, subject)
            );
            if (callback != null) {
                callback.onCall();
            }
            return true;
        } catch (Exception e) {
            Static.error(e);
            return false;
        }
    }
    public static boolean deleteLesson(final Context context, final String query, final int weekday, final JSONObject lesson, final Static.SimpleCallback callback) {
        try {
            if (context == null) throw new NullPointerException("context cannot be null");
            if (query == null) throw new NullPointerException("query cannot be null");
            if (query.isEmpty()) throw new IllegalArgumentException("query cannot be empty");
            if (lesson == null) throw new NullPointerException("lesson cannot be null");
            Log.v(TAG, "deleteLesson | query=" + query + " | weekday=" + weekday + " | lesson=" + lesson.toString());
            final String cdoitmo_type = lesson.getString("cdoitmo_type");
            if (!cdoitmo_type.equals("synthetic")) throw new Exception("wrong cdoitmo_type type: " + cdoitmo_type);
            final String hash = getLessonHash(lesson);
            final String token = query.toLowerCase();
            final JSONArray added = Static.string2jsonArray(Storage.file.perm.get(context, "schedule_lessons#added#" + token, ""));
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
                Storage.file.perm.delete(context, "schedule_lessons#added#" + token);
                if (Storage.file.perm.list(context, "schedule_lessons#added").size() == 0) {
                    Storage.file.perm.clear(context, "schedule_lessons#added");
                }
            } else {
                Storage.file.perm.put(context, "schedule_lessons#added#" + token, added.toString());
            }
            if (callback != null) {
                callback.onCall();
            }
            return true;
        } catch (Exception e) {
            Static.error(e);
            return false;
        }
    }
    public static boolean editLesson(final ConnectedActivity activity, final String query, final String title, final String type, final int weekday, final JSONObject lesson, final Static.SimpleCallback callback) {
        try {
            if (activity == null) throw new NullPointerException("activity cannot be null");
            if (query == null) throw new NullPointerException("query cannot be null");
            if (query.isEmpty()) throw new IllegalArgumentException("query cannot be empty");
            if (title == null) throw new NullPointerException("title cannot be null");
            if (type == null) throw new NullPointerException("type cannot be null");
            if (lesson == null) throw new NullPointerException("lesson cannot be null");
            Log.v(TAG, "editLesson | open fragment | query=" + query + " | weekday=" + weekday + " | lesson=" + lesson.toString());
            final Bundle extras = new Bundle();
            extras.putSerializable("action_type", ScheduleLessonsModifyFragment.TYPE.edit);
            extras.putString("query", query);
            extras.putString("type", type);
            extras.putString("title", title);
            extras.putInt("weekday", weekday);
            extras.putString("lesson", lesson.toString());
            Static.T.runOnUiThread(new Runnable() {
                @Override
                public void run() {
                    if (activity.openActivityOrFragment(ScheduleLessonsModifyFragment.class, extras) && callback != null) {
                        Static.T.runThread(new Runnable() {
                            @Override
                            public void run() {
                                callback.onCall();
                            }
                        });
                    }
                }
            });
            return true;
        } catch (Exception e) {
            Static.error(e);
            return false;
        }
    }
}

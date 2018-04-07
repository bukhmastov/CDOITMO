package com.bukhmastov.cdoitmo.fragment;

import android.content.Context;
import android.content.Intent;
import android.net.Uri;
import android.os.Bundle;
import android.support.annotation.NonNull;
import android.support.annotation.Nullable;
import android.support.annotation.StringDef;
import android.support.v4.content.FileProvider;
import android.support.v7.app.AlertDialog;
import android.view.InflateException;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.Button;
import android.widget.CheckBox;
import android.widget.FrameLayout;
import android.widget.TextView;

import com.bukhmastov.cdoitmo.R;
import com.bukhmastov.cdoitmo.firebase.FirebaseAnalyticsProvider;
import com.bukhmastov.cdoitmo.network.model.Client;
import com.bukhmastov.cdoitmo.object.schedule.Schedule;
import com.bukhmastov.cdoitmo.object.schedule.ScheduleLessons;
import com.bukhmastov.cdoitmo.util.Log;
import com.bukhmastov.cdoitmo.util.Static;
import com.bukhmastov.cdoitmo.util.Storage;

import org.json.JSONArray;
import org.json.JSONException;
import org.json.JSONObject;
import org.json.JSONTokener;

import java.io.BufferedOutputStream;
import java.io.File;
import java.io.FileOutputStream;
import java.lang.annotation.Retention;
import java.lang.annotation.RetentionPolicy;
import java.util.ArrayList;

public class ScheduleLessonsShareFragment extends ConnectedFragment {

    private static final String TAG = "SLShareFragment";
    private static class Change {
        private final @TYPE String type;
        private final int weekday;
        private final JSONObject content;
        private boolean enabled;
        private Change(@TYPE String type, boolean enabled, int weekday, JSONObject content) {
            this.type = type;
            this.enabled = enabled;
            this.weekday = weekday;
            this.content = content;
        }
    }
    private boolean keepGoing = true;
    private String action = "";
    private String query = "";
    private String type = "";
    private String title = "";
    private JSONObject file = null;
    private final ArrayList<Change> changes = new ArrayList<>();
    private boolean loaded = false;
    private Client.Request requestHandle = null;
    private int colorScheduleFlagTEXT = -1, colorScheduleFlagCommonBG = -1, colorScheduleFlagPracticeBG = -1, colorScheduleFlagLectureBG = -1, colorScheduleFlagLabBG = -1, colorScheduleFlagIwsBG = -1;

    @Retention(RetentionPolicy.SOURCE)
    @StringDef({ADDED, REDUCED})
    public @interface TYPE {}
    public static final String ADDED = "added";
    public static final String REDUCED = "reduced";

    @Override
    public void onCreate(@Nullable Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        FirebaseAnalyticsProvider.logCurrentScreen(activity, this);
        action = extras.getString("action");
        Log.v(TAG, "Fragment created | action=" + action);
        if (action == null || !(action.equals("share") || action.equals("handle"))) {
            keepGoing = false;
            Static.toast(activity, activity.getString(R.string.corrupted_data));
            finish();
        }
        switch (action) {
            case "share":
            default: {
                activity.updateToolbar(activity, activity.getString(R.string.share_changes), R.drawable.ic_share);
                break;
            }
            case "handle": {
                activity.updateToolbar(activity, activity.getString(R.string.accept_changes), R.drawable.ic_share);
                break;
            }
        }
    }

    @Override
    public View onCreateView(@NonNull LayoutInflater inflater, ViewGroup container, Bundle savedInstanceState) {
        return inflater.inflate("handle".equals(action) ? R.layout.fragment_schedule_lessons_share_receive : R.layout.fragment_schedule_lessons_share, container, false);
    }

    @Override
    public void onResume() {
        super.onResume();
        if (!keepGoing) {
            return;
        }
        FirebaseAnalyticsProvider.setCurrentScreen(activity, this);
        if (!loaded) {
            loaded = true;
            try {
                Bundle extras = getArguments();
                if (extras == null) {
                    throw new NullPointerException("extras cannot be null");
                }
                load(extras);
            } catch (Exception e) {
                Static.error(e);
                finish();
            }
        }
    }

    @Override
    public void onPause() {
        super.onPause();
        if (requestHandle != null && requestHandle.cancel()) {
            loaded = false;
        }
    }

    private void load(final Bundle extras) {
        Static.T.runThread(() -> {
            try {
                Log.v(TAG, "load | action=" + action);
                switch (action) {
                    case "handle": loadHandle(extras); break;
                    case "share": loadShare(extras); break;
                    default: throw new Exception();
                }
            } catch (Exception e) {
                if (e.getMessage() != null && "corrupted file".equals(e.getMessage().toLowerCase())) {
                    Static.toast(activity, activity.getString(R.string.corrupted_file));
                } else {
                    Static.error(e);
                    Static.toast(activity, activity.getString(R.string.something_went_wrong));
                }
                finish();
            }
        });
    }
    private void loadHandle(final Bundle extras) throws Exception {
        final String data = extras.getString("data");
        if (data == null) {
            throw new NullPointerException("Extra(data) is null");
        }
        file = (JSONObject) new JSONTokener(data).nextValue();
        if (!"share_schedule_of_lessons".equals(file.getString("type"))) {
            throw new Exception("Wrong type of provided data");
        }
        int version = 1;
        if (file.has("version")) {
            try {
                version = file.getInt("version");
            } catch (JSONException ignore) {
                // ignore
            }
        }
        if (!file.has("content")) {
            throw new Exception("Corrupted file");
        }
        final JSONObject content = file.getJSONObject("content");
        if (
                !(content.has("query") && content.get("query") instanceof String) ||
                !(content.has("title") && content.get("title") instanceof String) ||
                !(content.has("added") && content.get("added") instanceof JSONArray) ||
                !(content.has("reduced") && content.get("reduced") instanceof JSONArray)
        ) {
            throw new Exception("Corrupted file");
        }
        if (version == 1) {
            content.put("type", "");
        } else if (version > 1) {
            if (!(content.has("type") && content.get("type") instanceof String)) {
                throw new Exception("Corrupted file");
            }
        }
        Static.T.runOnUiThread(() -> {
            try {
                TextView share_title = activity.findViewById(R.id.share_title);
                if (share_title != null) {
                    share_title.setText(ScheduleLessons.getScheduleHeader(activity, content.getString("title"), content.getString("type")));
                }
            } catch (Exception e) {
                Static.error(e);
                Static.toast(activity, activity.getString(R.string.something_went_wrong));
                finish();
            }
        });
        final JSONArray scheduleAdded = content.getJSONArray("added");
        final JSONArray scheduleReduced = content.getJSONArray("reduced");
        for (int i = 0; i < scheduleAdded.length(); i++) {
            try {
                JSONObject day = scheduleAdded.getJSONObject(i);
                JSONObject lesson = day.getJSONObject("lesson");
                int dayIndex = day.getInt("day");
                changes.add(new Change(ADDED, true, dayIndex, lesson));
            } catch (Exception ignore) {
                // ignore
            }
        }
        if (version != 1) {
            for (int i = 0; i < scheduleReduced.length(); i++) {
                try {
                    JSONObject day = scheduleReduced.getJSONObject(i);
                    JSONObject lesson = day.getJSONObject("lesson");
                    int dayIndex = day.getInt("day");
                    changes.add(new Change(REDUCED, true, dayIndex, lesson));
                } catch (Exception ignore) {
                    // ignore
                }
            }
        }
        String notification = null;
        if (version == 1 && scheduleReduced.length() > 0) {
            int count = scheduleReduced.length();
            notification = count + " " + (count % 10 == 1 ? activity.getString(R.string.share_schedule_notification1_1_single) : (count % 100 == count % 10 && count % 10 != 0 && count % 10 < 5 ? activity.getString(R.string.share_schedule_notification1_1_plural2) : activity.getString(R.string.share_schedule_notification1_1_plural))) + ", " + activity.getString(R.string.share_schedule_notification1_2) + ".";
        }
        display(notification);
    }
    private void loadShare(final Bundle extras) throws Exception {
        query = extras.getString("query");
        title = extras.getString("title");
        type = extras.getString("type");
        if (query == null || title == null || type == null) {
            throw new NullPointerException("Some extras are null: " + query + " | " + title + " | " + type);
        }
        Static.T.runOnUiThread(() -> {
            try {
                TextView share_title = activity.findViewById(R.id.share_title);
                if (share_title != null) {
                    share_title.setText(ScheduleLessons.getScheduleHeader(activity, title, type));
                }
                ViewGroup share_info = activity.findViewById(R.id.share_info);
                if (share_info != null) {
                    share_info.setOnClickListener(view -> Static.T.runThread(() -> {
                        if (activity != null) {
                            new AlertDialog.Builder(activity)
                                    .setTitle(R.string.share_changes)
                                    .setMessage(R.string.share_changes_info)
                                    .setPositiveButton(R.string.close, null)
                                    .create().show();
                        }
                    }));
                }
            } catch (Exception e) {
                Static.error(e);
                Static.toast(activity, activity.getString(R.string.something_went_wrong));
                finish();
            }
        });
        new ScheduleLessons(new Schedule.Handler() {
            @Override
            public void onSuccess(final JSONObject json, final boolean fromCache) {
                Static.T.runThread(() -> {
                    try {
                        Log.v(TAG, "loadShare | success | json=" + (json == null ? "null" : "notnull"));
                        if (json == null || json.getString("type").equals("teachers")) {
                            Static.toast(activity, activity.getString(R.string.something_went_wrong));
                            finish();
                            return;
                        }
                        final String token = query.toLowerCase();
                        final JSONArray schedule = json.getJSONArray("schedule");
                        final JSONArray scheduleAdded = string2json(Storage.file.perm.get(activity, "schedule_lessons#added#" + token, ""));
                        final JSONArray scheduleReduced = string2json(Storage.file.perm.get(activity, "schedule_lessons#reduced#" + token, ""));
                        for (int i = 0; i < scheduleAdded.length(); i++) {
                            try {
                                JSONObject day = scheduleAdded.getJSONObject(i);
                                JSONArray lessons = day.getJSONArray("lessons");
                                int weekday = day.getInt("weekday");
                                if (lessons.length() > 0) {
                                    for (int j = 0; j < lessons.length(); j++) {
                                        try {
                                            JSONObject lesson = lessons.getJSONObject(j);
                                            changes.add(new Change(ADDED, true, weekday, lesson));
                                        } catch (Exception ignore) {
                                            // ignore
                                        }
                                    }
                                }
                            } catch (Exception ignore) {
                                // ignore
                            }
                        }
                        for (int i = 0; i < scheduleReduced.length(); i++) {
                            try {
                                JSONObject day = scheduleReduced.getJSONObject(i);
                                JSONArray lessons = day.getJSONArray("lessons");
                                int weekday = day.getInt("weekday");
                                if (lessons.length() > 0) {
                                    for (int j = 0; j < lessons.length(); j++) {
                                        try {
                                            String hash = lessons.getString(j);
                                            for (int k = 0; k < 7; k++) {
                                                JSONObject dayOriginal = schedule.getJSONObject(k);
                                                JSONArray lessonsOriginal = dayOriginal.getJSONArray("lessons");
                                                for (int a = 0; a < lessonsOriginal.length(); a++) {
                                                    JSONObject lessonOriginal = lessonsOriginal.getJSONObject(a);
                                                    String hashOriginal = ScheduleLessons.getLessonHash(lessonOriginal);
                                                    if (hashOriginal.equals(hash)) {
                                                        lessonOriginal.put("hash", hash);
                                                        changes.add(new Change(REDUCED, true, weekday, lessonOriginal));
                                                    }
                                                }
                                            }
                                        } catch (Exception ignore) {
                                            // ignore
                                        }
                                    }
                                }
                            } catch (Exception ignore) {
                                // ignore
                            }
                        }
                        display(null);
                    } catch (Exception e) {
                        Static.error(e);
                        Static.toast(activity, activity.getString(R.string.something_went_wrong));
                        finish();
                    }
                });
            }
            @Override
            public void onFailure(int state) {
                this.onFailure(0, null, state);
            }
            @Override
            public void onFailure(final int statusCode, final Client.Headers headers, final int state) {
                Static.T.runOnUiThread(() -> {
                    try {
                        Log.v(TAG, "loadShare | failure " + state);
                        ViewGroup share_content = activity.findViewById(R.id.share_content);
                        if (share_content != null) {
                            share_content.removeAllViews();
                            View view;
                            switch (state) {
                                case Client.FAILED_OFFLINE:
                                case ScheduleLessons.FAILED_OFFLINE:
                                    share_content.addView(inflate(R.layout.state_offline_without_align));
                                    break;
                                case Client.FAILED_SERVER_ERROR:
                                    view = inflate(R.layout.state_failed_without_align);
                                    ((TextView) view.findViewById(R.id.text)).setText(Client.getFailureMessage(activity, statusCode));
                                    share_content.addView(view);
                                    break;
                                case Client.FAILED_CORRUPTED_JSON:
                                    view = inflate(R.layout.state_failed_without_align);
                                    ((TextView) view.findViewById(R.id.text)).setText(R.string.server_provided_corrupted_json);
                                    share_content.addView(view);
                                    break;
                                case Client.FAILED_TRY_AGAIN:
                                case ScheduleLessons.FAILED_LOAD:
                                case ScheduleLessons.FAILED_EMPTY_QUERY:
                                case ScheduleLessons.FAILED_NOT_FOUND:
                                case ScheduleLessons.FAILED_INVALID_QUERY:
                                case ScheduleLessons.FAILED_MINE_NEED_ISU:
                                default:
                                    share_content.addView(inflate(R.layout.state_failed_without_align));
                                    break;
                            }
                        }
                    } catch (Exception e) {
                        Static.error(e);
                    }
                });
            }
            @Override
            public void onProgress(final int state) {
                Static.T.runOnUiThread(() -> {
                    Log.v(TAG, "loadShare | progress " + state);
                    try {
                        ViewGroup share_content = activity.findViewById(R.id.share_content);
                        if (share_content != null) {
                            share_content.removeAllViews();
                            share_content.addView(inflate(R.layout.state_loading_without_align));
                        }
                    } catch (Exception e) {
                        Static.error(e);
                    }
                });
            }
            @Override
            public void onNewRequest(Client.Request request) {
                requestHandle = request;
            }
            @Override
            public void onCancelRequest() {
                if (requestHandle != null) {
                    requestHandle.cancel();
                }
            }
        }).search(
                activity,
                query,
                false,
                false
        );
    }
    private void display(final String notification) {
        Static.T.runOnUiThread(() -> {
            try {
                Log.v(TAG, "display | action=" + action);
                ViewGroup share_content = activity.findViewById(R.id.share_content);
                if (share_content == null) {
                    return;
                }
                share_content.removeAllViews();
                if (notification != null) {
                    View layout_notification_warning = inflate(R.layout.layout_notification_warning);
                    ((TextView) layout_notification_warning.findViewById(R.id.notification_warning_text)).setText(notification);
                    share_content.addView(layout_notification_warning);
                }
                if (changes.size() == 0) {
                    ViewGroup nothing_to_display = (ViewGroup) inflate(R.layout.nothing_to_display);
                    ((TextView) nothing_to_display.findViewById(R.id.ntd_text)).setText(R.string.nothing_to_share);
                    share_content.addView(nothing_to_display);
                    return;
                }
                boolean headerAdded = true;
                boolean headerReduced = true;
                for (final Change change : changes) {
                    try {
                        if (headerAdded && change.type.equals(ADDED)) {
                            headerAdded = false;
                            final View header = inflate(R.layout.fragment_schedule_lessons_share_header);
                            ((TextView) header.findViewById(R.id.text)).setText("handle".equals(action) ? R.string.add_lessons : R.string.added_lessons);
                            share_content.addView(header);
                        }
                        if (headerReduced && change.type.equals(REDUCED)) {
                            headerReduced = false;
                            final View header = inflate(R.layout.fragment_schedule_lessons_share_header);
                            ((TextView) header.findViewById(R.id.text)).setText("handle".equals(action) ? R.string.reduce_lessons : R.string.reduced_lessons);
                            share_content.addView(header);
                        }
                        final View item = inflate(R.layout.fragment_schedule_lessons_share_item);
                        final ViewGroup lesson = (ViewGroup) inflate(R.layout.layout_schedule_lessons_item);
                        final CheckBox checkbox = item.findViewById(R.id.checkbox);
                        final ViewGroup content = item.findViewById(R.id.content);
                        checkbox.setChecked(change.enabled);
                        lesson.findViewById(R.id.lesson_reduced_icon).setVisibility(View.GONE);
                        lesson.findViewById(R.id.lesson_synthetic_icon).setVisibility(View.GONE);
                        lesson.findViewById(R.id.lesson_touch_icon).setVisibility(View.GONE);
                        ((TextView) lesson.findViewById(R.id.lesson_time_start)).setText(change.content.getString("timeStart"));
                        ((TextView) lesson.findViewById(R.id.lesson_time_end)).setText(change.content.getString("timeEnd"));
                        ((TextView) lesson.findViewById(R.id.lesson_title)).setText(change.content.getString("subject"));
                        setDesc(change.content, lesson.findViewById(R.id.lesson_desc));
                        setFlags(change.content, lesson.findViewById(R.id.lesson_flags));
                        setMeta(change.content, lesson.findViewById(R.id.lesson_meta));
                        checkbox.setOnCheckedChangeListener((compoundButton, checked) -> change.enabled = checked);
                        item.setOnClickListener(view -> checkbox.setChecked(!checkbox.isChecked()));
                        content.addView(lesson);
                        share_content.addView(item);
                    } catch (Exception e) {
                        Static.error(e);
                    }
                }
                switch (action) {
                    case "share":
                    default: {
                        Button share_execute = activity.findViewById(R.id.share_execute);
                        if (share_execute != null) {
                            share_execute.setVisibility(View.VISIBLE);
                            share_execute.setOnClickListener(view -> execute());
                        }
                        break;
                    }
                    case "handle": {
                        Button action_deny = activity.findViewById(R.id.action_deny);
                        Button action_accept = activity.findViewById(R.id.action_accept);
                        if (action_deny != null) {
                            action_deny.setOnClickListener(view -> finish());
                        }
                        if (action_accept != null) {
                            action_accept.setVisibility(View.VISIBLE);
                            action_accept.setOnClickListener(view -> execute());
                        }
                        break;
                    }
                }
            } catch (Exception e) {
                Static.error(e);
                Static.toast(activity, activity.getString(R.string.something_went_wrong));
                finish();
            }
        });
    }
    private void execute() {
        Static.T.runThread(() -> {
            try {
                Log.v(TAG, "execute | action=" + action);
                boolean selected = false;
                for (Change change : changes) {
                    if (change.enabled) {
                        selected = true;
                        break;
                    }
                }
                if (!selected) {
                    Static.snackBar(activity, activity.getString(R.string.nothing_to_share));
                    return;
                }
                switch (action) {
                    case "handle": executeHandle(); break;
                    case "share":
                    default: executeShare(); break;
                }
            } catch (Exception e) {
                Static.error(e);
                Static.snackBar(activity, activity.getString(R.string.something_went_wrong));
            }
        });
    }
    private void executeHandle() throws Exception {
        final String token = file.getJSONObject("content").getString("query").toLowerCase();
        for (Change change : changes) {
            if (!change.enabled) continue;
            if (change.type.equals(ADDED)) {
                final JSONArray added = string2json(Storage.file.perm.get(activity, "schedule_lessons#added#" + token, ""));
                boolean found = false;
                for (int i = 0; i < added.length(); i++) {
                    JSONObject day = added.getJSONObject(i);
                    if (day.getInt("weekday") == change.weekday) {
                        found = true;
                        day.getJSONArray("lessons").put(change.content);
                    }
                }
                if (!found) {
                    added.put(new JSONObject()
                            .put("weekday", change.weekday)
                            .put("lessons", new JSONArray().put(change.content))
                    );
                }
                Storage.file.perm.put(activity, "schedule_lessons#added#" + token, added.toString());
            } else {
                final String hash = change.content.getString("hash");
                final JSONArray reduced = string2json(Storage.file.perm.get(activity, "schedule_lessons#reduced#" + token, ""));
                boolean found = false;
                for (int i = 0; i < reduced.length(); i++) {
                    JSONObject day = reduced.getJSONObject(i);
                    if (day.getInt("weekday") == change.weekday) {
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
                            .put("weekday", change.weekday)
                            .put("lessons", new JSONArray().put(hash))
                    );
                }
                Storage.file.perm.put(activity, "schedule_lessons#reduced#" + token, reduced.toString());
            }
        }
        FirebaseAnalyticsProvider.logEvent(
                activity,
                FirebaseAnalyticsProvider.Event.SCHEDULE_LESSONS_RECEIVE,
                FirebaseAnalyticsProvider.getBundle(FirebaseAnalyticsProvider.Param.QUERY, query)
        );
        Static.toast(activity, getString(R.string.changes_applied));
        finish();
    }
    private void executeShare() throws Exception {
        JSONArray added = new JSONArray();
        JSONArray reduced = new JSONArray();
        for (Change change : changes) {
            if (!change.enabled) continue;
            (change.type.equals(ADDED) ? added : reduced).put(new JSONObject().put("day", change.weekday).put("lesson", change.content));
        }
        JSONObject share = new JSONObject();
        share.put("type", "share_schedule_of_lessons");
        share.put("version", 2);
        share.put("content", new JSONObject()
                .put("query", query)
                .put("type", type)
                .put("title", title)
                .put("added", added)
                .put("reduced", reduced)
        );
        byte[] bytes = share.toString().getBytes("UTF-8");
        File file = getFile(bytes);
        if (file != null) {
            Uri uri = FileProvider.getUriForFile(activity, "com.bukhmastov.cdoitmo.fileprovider", file);
            Intent intent = new Intent(Intent.ACTION_SEND);
            intent.addFlags(Intent.FLAG_GRANT_READ_URI_PERMISSION);
            intent.setType("application/cdoitmo" /*activity.getContentResolver().getType(uri)*/ );
            intent.putExtra(Intent.EXTRA_STREAM, uri);
            try {
                startActivity(Intent.createChooser(intent, activity.getString(R.string.share) + "..."));
                FirebaseAnalyticsProvider.logEvent(
                        activity,
                        FirebaseAnalyticsProvider.Event.SCHEDULE_LESSONS_SHARE,
                        FirebaseAnalyticsProvider.getBundle(FirebaseAnalyticsProvider.Param.QUERY, query)
                );
            } catch (Exception ignore) {
                Static.toast(activity, activity.getString(R.string.failed_to_share_file));
            }
        }
    }

    private void setDesc(final JSONObject lesson, TextView textView) throws Exception {
        String desc;
        String teacher = "";
        String group = "";
        if (lesson.has("teacher") && !lesson.getString("teacher").trim().isEmpty()) {
            teacher = lesson.getString("teacher").trim();
        }
        if (lesson.has("group") && !lesson.getString("group").trim().isEmpty()) {
            group = lesson.getString("group").trim();
        }
        if (group.isEmpty()) {
            desc = teacher;
        } else {
            desc = group;
            if (!teacher.isEmpty()) desc += " (" + teacher + ")";
        }
        desc = desc.isEmpty() ? null : desc;
        if (desc == null) {
            textView.setHeight(0);
        } else {
            textView.setText(desc);
        }
    }
    private void setMeta(final JSONObject lesson, TextView textView) throws Exception {
        String meta;
        String room = "";
        String building = "";
        if (lesson.has("room") && !lesson.getString("room").trim().isEmpty()) {
            room = lesson.getString("room").trim();
        }
        if (lesson.has("building") && !lesson.getString("building").trim().isEmpty()) {
            building = lesson.getString("building").trim();
        }
        if (room.isEmpty()) {
            meta = building;
        } else {
            meta = activity.getString(R.string.room_short) + " " + room;
            if (!building.isEmpty()) meta += " (" + building + ")";
        }
        meta = meta.isEmpty() ? null : meta;
        if (meta == null) {
            textView.setHeight(0);
        } else {
            textView.setText(meta);
        }
    }
    private void setFlags(JSONObject lesson, ViewGroup viewGroup) throws Exception {
        String type = lesson.getString("type");
        if (colorScheduleFlagTEXT == -1) colorScheduleFlagTEXT = Static.resolveColor(activity, R.attr.colorScheduleFlagTEXT);
        if (colorScheduleFlagCommonBG == -1) colorScheduleFlagCommonBG = Static.resolveColor(activity, R.attr.colorScheduleFlagCommonBG);
        if (colorScheduleFlagPracticeBG == -1) colorScheduleFlagPracticeBG = Static.resolveColor(activity, R.attr.colorScheduleFlagPracticeBG);
        if (colorScheduleFlagLectureBG == -1) colorScheduleFlagLectureBG = Static.resolveColor(activity, R.attr.colorScheduleFlagLectureBG);
        if (colorScheduleFlagLabBG == -1) colorScheduleFlagLabBG = Static.resolveColor(activity, R.attr.colorScheduleFlagLabBG);
        if (colorScheduleFlagIwsBG == -1) colorScheduleFlagIwsBG = Static.resolveColor(activity, R.attr.colorScheduleFlagIwsBG);
        if (!type.isEmpty()) {
            switch (type) {
                case "practice":
                    viewGroup.addView(getFlag(activity.getString(R.string.practice), colorScheduleFlagTEXT, colorScheduleFlagPracticeBG));
                    break;
                case "lecture":
                    viewGroup.addView(getFlag(activity.getString(R.string.lecture), colorScheduleFlagTEXT, colorScheduleFlagLectureBG));
                    break;
                case "lab":
                    viewGroup.addView(getFlag(activity.getString(R.string.lab), colorScheduleFlagTEXT, colorScheduleFlagLabBG));
                    break;
                case "iws":
                    viewGroup.addView(getFlag(activity.getString(R.string.iws), colorScheduleFlagTEXT, colorScheduleFlagIwsBG));
                    break;
                default:
                    viewGroup.addView(getFlag(type, colorScheduleFlagTEXT, colorScheduleFlagCommonBG));
                    break;
            }
        }
        int week = lesson.getInt("week");
        if (week == 0 || week == 1) {
            viewGroup.addView(getFlag(week == 0 ? activity.getString(R.string.tab_even) : activity.getString(R.string.tab_odd), colorScheduleFlagTEXT, colorScheduleFlagCommonBG));
        }
    }
    private FrameLayout getFlag(String text, int textColor, int backgroundColor) throws Exception {
        FrameLayout flagContainer = (FrameLayout) inflate(R.layout.layout_schedule_lessons_flag);
        TextView flag_content = flagContainer.findViewById(R.id.flag_content);
        flag_content.setText(text);
        flag_content.setBackgroundColor(backgroundColor);
        flag_content.setTextColor(textColor);
        return flagContainer;
    }
    @Nullable
    private File getFile(byte[] data) {
        try {
            File temp = new File(activity.getCacheDir(), "shared" + File.separator + "share_schedule_of_lessons.cdoitmo");
            if (!temp.exists()) {
                if (!temp.getParentFile().mkdirs() && !temp.createNewFile()) {
                    throw new Exception("Failed to create file: " + temp.getPath());
                }
            }
            temp.deleteOnExit();
            BufferedOutputStream bos = new BufferedOutputStream(new FileOutputStream(temp));
            bos.write(data);
            bos.flush();
            bos.close();
            return temp;
        } catch (Exception e) {
            Static.error(e);
            Static.toast(activity, activity.getString(R.string.something_went_wrong));
            return null;
        }
    }

    private void finish() {
        Log.v(TAG, "finish");
        if ("handle".equals(action)) {
            activity.finish();
        } else {
            activity.back();
        }
    }

    private JSONArray string2json(String text) throws JSONException {
        JSONArray json;
        if (text.isEmpty()) {
            json = new JSONArray();
        } else {
            json = new JSONArray(text);
        }
        return json;
    }
    private View inflate(int layoutId) throws InflateException {
        return ((LayoutInflater) activity.getSystemService(Context.LAYOUT_INFLATER_SERVICE)).inflate(layoutId, null);
    }
}

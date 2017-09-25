package com.bukhmastov.cdoitmo.fragments;

import android.content.Context;
import android.content.Intent;
import android.net.Uri;
import android.os.Bundle;
import android.support.annotation.Nullable;
import android.support.v4.content.FileProvider;
import android.support.v7.app.AlertDialog;
import android.view.InflateException;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.Button;
import android.widget.CheckBox;
import android.widget.CompoundButton;
import android.widget.FrameLayout;
import android.widget.LinearLayout;
import android.widget.TextView;

import com.bukhmastov.cdoitmo.R;
import com.bukhmastov.cdoitmo.firebase.FirebaseAnalyticsProvider;
import com.bukhmastov.cdoitmo.network.IfmoRestClient;
import com.bukhmastov.cdoitmo.network.models.Client;
import com.bukhmastov.cdoitmo.objects.ScheduleLessons;
import com.bukhmastov.cdoitmo.utils.Log;
import com.bukhmastov.cdoitmo.utils.Static;
import com.bukhmastov.cdoitmo.utils.Storage;

import org.json.JSONArray;
import org.json.JSONException;
import org.json.JSONObject;
import org.json.JSONTokener;

import java.io.BufferedOutputStream;
import java.io.File;
import java.io.FileOutputStream;
import java.util.ArrayList;
import java.util.Objects;

public class ScheduleLessonsShareFragment extends ConnectedFragment {

    private static final String TAG = "SLShareFragment";
    private enum TYPE {ADDED, REDUCED}
    private static class Change {
        private TYPE type;
        private boolean enabled;
        private int day;
        private JSONObject content;
        private Change(TYPE type, boolean enabled, int day, JSONObject content) {
            this.type = type;
            this.enabled = enabled;
            this.day = day;
            this.content = content;
        }
    }
    private boolean keepGoing = true;
    private String type = "";
    private String query = "";
    private String title = "";
    private String token = "";
    private JSONObject file = null;
    private ArrayList<Change> changes = new ArrayList<>();
    private boolean loaded = false;
    private Client.Request requestHandle = null;
    private int colorScheduleFlagTEXT = -1, colorScheduleFlagCommonBG = -1, colorScheduleFlagPracticeBG = -1, colorScheduleFlagLectureBG = -1, colorScheduleFlagLabBG = -1;

    @Override
    public void onCreate(@Nullable Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        FirebaseAnalyticsProvider.logCurrentScreen(activity, this);
        type = extras.getString("type");
        Log.v(TAG, "Fragment created | type=" + type);
        if (type == null || !(Objects.equals(type, "share") || Objects.equals(type, "handle"))) {
            keepGoing = false;
            Static.snackBar(activity, activity.getString(R.string.something_went_wrong));
            close();
        }
        switch (type) {
            case "share":
            default: {
                activity.updateToolbar(activity.getString(R.string.share_changes), R.drawable.ic_share);
                break;
            }
            case "handle": {
                activity.updateToolbar(activity.getString(R.string.accept_changes), R.drawable.ic_share);
                break;
            }
        }
    }

    @Override
    public View onCreateView(LayoutInflater inflater, ViewGroup container, Bundle savedInstanceState) {
        return inflater.inflate("handle".equals(type) ? R.layout.fragment_schedule_lessons_share_receive : R.layout.fragment_schedule_lessons_share, container, false);
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
                close();
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
        Static.T.runThread(new Runnable() {
            @Override
            public void run() {
                try {
                    Log.v(TAG, "load | type=" + type);
                    if ("handle".equals(type)) {
                        final String data = extras.getString("data");
                        if (data == null) {
                            throw new NullPointerException("Extra(data) is null");
                        }
                        file = (JSONObject) new JSONTokener(data).nextValue();
                        if (!"share_schedule_of_lessons".equals(file.getString("type"))) {
                            throw new Exception("Wrong type of provided data");
                        }
                        if (file.has("content")) {
                            final JSONObject content = file.getJSONObject("content");
                            if (
                                    !(content.has("query") && content.get("query") instanceof String) ||
                                    !(content.has("title") && content.get("title") instanceof String) ||
                                    !(content.has("token") && content.get("token") instanceof String) ||
                                    !(content.has("added") && content.get("added") instanceof JSONArray) ||
                                    !(content.has("reduced") && content.get("reduced") instanceof JSONArray)
                            ) {
                                throw new Exception("Corrupted file");
                            }
                        } else {
                            throw new Exception("Corrupted file");
                        }
                        Static.T.runOnUiThread(new Runnable() {
                            @Override
                            public void run() {
                                try {
                                    TextView share_title = activity.findViewById(R.id.share_title);
                                    if (share_title != null) {
                                        share_title.setText(file.getJSONObject("content").getString("title"));
                                    }
                                } catch (Exception e) {
                                    Static.error(e);
                                    Static.snackBar(activity, activity.getString(R.string.something_went_wrong));
                                    close();
                                }
                            }
                        });
                        final JSONArray scheduleAdded = file.getJSONObject("content").getJSONArray("added");
                        final JSONArray scheduleReduced = file.getJSONObject("content").getJSONArray("reduced");
                        for (int i = 0; i < scheduleAdded.length(); i++) {
                            try {
                                JSONObject day = scheduleAdded.getJSONObject(i);
                                JSONObject lesson = day.getJSONObject("lesson");
                                int dayIndex = day.getInt("day");
                                changes.add(new Change(TYPE.ADDED, true, dayIndex, lesson));
                            } catch (Exception ignore) {
                                // ignore
                            }
                        }
                        for (int i = 0; i < scheduleReduced.length(); i++) {
                            try {
                                JSONObject day = scheduleReduced.getJSONObject(i);
                                JSONObject lesson = day.getJSONObject("lesson");
                                int dayIndex = day.getInt("day");
                                changes.add(new Change(TYPE.REDUCED, true, dayIndex, lesson));
                            } catch (Exception ignore) {
                                // ignore
                            }
                        }
                        display();
                    } else {
                        query = extras.getString("query");
                        title = extras.getString("title");
                        token = extras.getString("token");
                        if (query == null || title == null || token == null) {
                            throw new NullPointerException("Some extras are null: " + query + " | " + title + " | " + token);
                        }
                        Static.T.runOnUiThread(new Runnable() {
                            @Override
                            public void run() {
                                try {
                                    TextView share_title = activity.findViewById(R.id.share_title);
                                    if (share_title != null) {
                                        share_title.setText(title);
                                    }
                                    ViewGroup share_info = activity.findViewById(R.id.share_info);
                                    if (share_info != null) {
                                        share_info.setOnClickListener(new View.OnClickListener() {
                                            @Override
                                            public void onClick(View view) {
                                                Static.T.runThread(new Runnable() {
                                                    @Override
                                                    public void run() {
                                                        if (activity != null) {
                                                            new AlertDialog.Builder(activity)
                                                                    .setTitle(R.string.share_changes)
                                                                    .setMessage(R.string.share_changes_info)
                                                                    .setPositiveButton(R.string.close, null)
                                                                    .create().show();
                                                        }
                                                    }
                                                });
                                            }
                                        });
                                    }
                                } catch (Exception e) {
                                    Static.error(e);
                                    Static.snackBar(activity, activity.getString(R.string.something_went_wrong));
                                    close();
                                }
                            }
                        });
                        ScheduleLessons scheduleLessons = new ScheduleLessons(activity);
                        scheduleLessons.setHandler(new ScheduleLessons.response() {
                            @Override
                            public void onSuccess(final JSONObject json) {
                                Static.T.runThread(new Runnable() {
                                    @Override
                                    public void run() {
                                        try {
                                            Log.v(TAG, "load | success | json=" + (json == null ? "null" : "notnull"));
                                            if (json == null || Objects.equals(json.getString("type"), "teacher_picker")) {
                                                Static.snackBar(activity, activity.getString(R.string.something_went_wrong));
                                                close();
                                                return;
                                            }
                                            final JSONArray schedule = json.getJSONArray("schedule");
                                            final JSONArray scheduleAdded = string2json(Storage.file.perm.get(activity, "schedule_lessons#added#" + token, ""));
                                            final JSONArray scheduleReduced = string2json(Storage.file.perm.get(activity, "schedule_lessons#reduced#" + token, ""));
                                            for (int i = 0; i < scheduleAdded.length(); i++) {
                                                try {
                                                    JSONObject day = scheduleAdded.getJSONObject(i);
                                                    JSONArray lessons = day.getJSONArray("lessons");
                                                    int dayIndex = day.getInt("index");
                                                    if (lessons.length() > 0) {
                                                        for (int j = 0; j < lessons.length(); j++) {
                                                            try {
                                                                JSONObject lesson = lessons.getJSONObject(j);
                                                                changes.add(new Change(TYPE.ADDED, true, dayIndex, lesson));
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
                                                    int dayIndex = day.getInt("index");
                                                    if (lessons.length() > 0) {
                                                        for (int j = 0; j < lessons.length(); j++) {
                                                            try {
                                                                String hash = lessons.getString(j);
                                                                for (int k = 0; k < schedule.length(); k++) {
                                                                    JSONObject dayOriginal = schedule.getJSONObject(k);
                                                                    JSONArray lessonsOriginal = dayOriginal.getJSONArray("lessons");
                                                                    for (int a = 0; a < lessonsOriginal.length(); a++) {
                                                                        JSONObject lessonOriginal = lessonsOriginal.getJSONObject(a);
                                                                        String hashOriginal = Static.crypt(ScheduleLessons.getCast(lessonOriginal));
                                                                        if (Objects.equals(hashOriginal, hash)) {
                                                                            lessonOriginal.put("hash", hash);
                                                                            changes.add(new Change(TYPE.REDUCED, true, dayIndex, lessonOriginal));
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
                                            display();
                                        } catch (Exception e) {
                                            Static.error(e);
                                            Static.snackBar(activity, activity.getString(R.string.something_went_wrong));
                                            close();
                                        }
                                    }
                                });
                            }
                            @Override
                            public void onFailure(final int state) {
                                Log.v(TAG, "load | failure " + state);
                                Static.T.runOnUiThread(new Runnable() {
                                    @Override
                                    public void run() {
                                        try {
                                            ViewGroup share_content = activity.findViewById(R.id.share_content);
                                            if (share_content != null) {
                                                share_content.removeAllViews();
                                                switch (state) {
                                                    case IfmoRestClient.FAILED_OFFLINE:
                                                    case ScheduleLessons.FAILED_OFFLINE:
                                                        share_content.addView(inflate(R.layout.state_offline_without_align));
                                                        break;
                                                    case IfmoRestClient.FAILED_TRY_AGAIN:
                                                    case ScheduleLessons.FAILED_LOAD:
                                                    case ScheduleLessons.FAILED_EMPTY_QUERY:
                                                    default:
                                                        share_content.addView(inflate(R.layout.state_failed_without_align));
                                                        break;
                                                }
                                            }
                                        } catch (Exception e) {
                                            Static.error(e);
                                        }
                                    }
                                });
                            }
                            @Override
                            public void onProgress(final int state) {
                                Static.T.runOnUiThread(new Runnable() {
                                    @Override
                                    public void run() {
                                        Log.v(TAG, "load | progress " + state);
                                        try {
                                            ViewGroup share_content = activity.findViewById(R.id.share_content);
                                            if (share_content != null) {
                                                share_content.removeAllViews();
                                                share_content.addView(inflate(R.layout.state_loading_without_align));
                                            }
                                        } catch (Exception e) {
                                            Static.error(e);
                                        }
                                    }
                                });
                            }
                            @Override
                            public void onNewRequest(Client.Request request) {
                                requestHandle = request;
                            }
                        });
                        scheduleLessons.search(
                                query,
                                Storage.pref.get(activity, "pref_use_cache", true) ? Integer.parseInt(Storage.pref.get(activity, "pref_static_refresh", "168")) : 0,
                                false,
                                false
                        );
                    }
                } catch (Exception e) {
                    Static.error(e);
                    Static.snackBar(activity, activity.getString(R.string.something_went_wrong));
                    close();
                }
            }
        });
    }
    private void display() {
        Static.T.runOnUiThread(new Runnable() {
            @Override
            public void run() {
                try {
                    Log.v(TAG, "display | type=" + type);
                    ViewGroup share_content = activity.findViewById(R.id.share_content);
                    if (share_content == null) {
                        return;
                    }
                    share_content.removeAllViews();
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
                            if (headerAdded && change.type == TYPE.ADDED) {
                                headerAdded = false;
                                final View header = inflate(R.layout.fragment_schedule_lessons_share_header);
                                ((TextView) header.findViewById(R.id.text)).setText("handle".equals(type) ? R.string.add_lessons : R.string.added_lessons);
                                share_content.addView(header);
                            }
                            if (headerReduced && change.type == TYPE.REDUCED) {
                                headerReduced = false;
                                final View header = inflate(R.layout.fragment_schedule_lessons_share_header);
                                ((TextView) header.findViewById(R.id.text)).setText("handle".equals(type) ? R.string.reduce_lessons : R.string.reduced_lessons);
                                share_content.addView(header);
                            }
                            final View item = inflate(R.layout.fragment_schedule_lessons_share_item);
                            final ViewGroup lesson = (ViewGroup) inflate(R.layout.layout_schedule_lessons_item);
                            final CheckBox checkbox = item.findViewById(R.id.checkbox);
                            final ViewGroup content = item.findViewById(R.id.content);
                            checkbox.setChecked(change.enabled);
                            lesson.findViewById(R.id.lesson_reduced_icon).setLayoutParams(new LinearLayout.LayoutParams(0, 0));
                            lesson.findViewById(R.id.lesson_synthetic_icon).setLayoutParams(new LinearLayout.LayoutParams(0, 0));
                            lesson.findViewById(R.id.lesson_touch_icon).setLayoutParams(new LinearLayout.LayoutParams(0, 0));
                            ((TextView) lesson.findViewById(R.id.lesson_time_start)).setText(change.content.getString("timeStart"));
                            ((TextView) lesson.findViewById(R.id.lesson_time_end)).setText(change.content.getString("timeEnd"));
                            ((TextView) lesson.findViewById(R.id.lesson_title)).setText(change.content.getString("subject"));
                            setDesc(change.content, (TextView) lesson.findViewById(R.id.lesson_desc));
                            setFlags(change.content, (ViewGroup) lesson.findViewById(R.id.lesson_flags));
                            setMeta(change.content, (TextView) lesson.findViewById(R.id.lesson_meta));
                            checkbox.setOnCheckedChangeListener(new CompoundButton.OnCheckedChangeListener() {
                                @Override
                                public void onCheckedChanged(CompoundButton compoundButton, boolean checked) {
                                    change.enabled = checked;
                                }
                            });
                            item.setOnClickListener(new View.OnClickListener() {
                                @Override
                                public void onClick(View view) {
                                    checkbox.setChecked(!checkbox.isChecked());
                                }
                            });
                            content.addView(lesson);
                            share_content.addView(item);
                        } catch (Exception e) {
                            Static.error(e);
                        }
                    }
                    switch (type) {
                        case "share":
                        default: {
                            Button share_execute = activity.findViewById(R.id.share_execute);
                            if (share_execute != null) {
                                share_execute.setVisibility(View.VISIBLE);
                                share_execute.setOnClickListener(new View.OnClickListener() {
                                    @Override
                                    public void onClick(View view) {
                                        execute();
                                    }
                                });
                            }
                            break;
                        }
                        case "handle": {
                            Button action_deny = activity.findViewById(R.id.action_deny);
                            Button action_accept = activity.findViewById(R.id.action_accept);
                            if (action_deny != null) {
                                action_deny.setOnClickListener(new View.OnClickListener() {
                                    @Override
                                    public void onClick(View view) {
                                        close();
                                    }
                                });
                            }
                            if (action_accept != null) {
                                action_accept.setVisibility(View.VISIBLE);
                                action_accept.setOnClickListener(new View.OnClickListener() {
                                    @Override
                                    public void onClick(View view) {
                                        execute();
                                    }
                                });
                            }
                            break;
                        }
                    }
                } catch (Exception e) {
                    Static.error(e);
                    Static.snackBar(activity, activity.getString(R.string.something_went_wrong));
                    close();
                }
            }
        });
    }
    private void execute() {
        Static.T.runThread(new Runnable() {
            @Override
            public void run() {
                try {
                    Log.v(TAG, "execute | type=" + type);
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
                    switch (type) {
                        case "share":
                        default: {
                            JSONArray added = new JSONArray();
                            JSONArray reduced = new JSONArray();
                            for (Change change : changes) {
                                if (!change.enabled) continue;
                                (change.type == TYPE.ADDED ? added : reduced).put(new JSONObject().put("day", change.day).put("lesson", change.content));
                            }
                            JSONObject share = new JSONObject();
                            share.put("type", "share_schedule_of_lessons");
                            share.put("content", new JSONObject()
                                    .put("query", query)
                                    .put("title", title)
                                    .put("token", token)
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
                                } catch (Exception ignore) {
                                    Static.toast(activity, activity.getString(R.string.failed_to_share_file));
                                }
                            }
                            break;
                        }
                        case "handle": {
                            final String token = file.getJSONObject("content").getString("token");
                            for (Change change : changes) {
                                if (!change.enabled) continue;
                                if (change.type == TYPE.ADDED) {
                                    final String addedStr = Storage.file.perm.get(activity, "schedule_lessons#added#" + token, "");
                                    JSONArray added;
                                    if (addedStr.isEmpty()) {
                                        added = new JSONArray();
                                    } else {
                                        added = new JSONArray(addedStr);
                                    }
                                    boolean found = false;
                                    for (int i = 0; i < added.length(); i++) {
                                        JSONObject day = added.getJSONObject(i);
                                        if (day.getInt("index") == change.day) {
                                            found = true;
                                            day.getJSONArray("lessons").put(change.content);
                                        }
                                    }
                                    if (!found) {
                                        added.put(new JSONObject()
                                                .put("index", change.day)
                                                .put("lessons", new JSONArray().put(change.content))
                                        );
                                    }
                                    Storage.file.perm.put(activity, "schedule_lessons#added#" + token, added.toString());
                                } else {
                                    final String hash = change.content.getString("hash");
                                    final String reducedStr = Storage.file.perm.get(activity, "schedule_lessons#reduced#" + token, "");
                                    JSONArray reduced;
                                    if (reducedStr.isEmpty()) {
                                        reduced = new JSONArray();
                                    } else {
                                        reduced = new JSONArray(reducedStr);
                                    }
                                    boolean found = false;
                                    for (int i = 0; i < reduced.length(); i++) {
                                        JSONObject day = reduced.getJSONObject(i);
                                        if (day.getInt("index") == change.day) {
                                            JSONArray lessons = day.getJSONArray("lessons");
                                            boolean foundLesson = false;
                                            for (int j = 0; j < lessons.length(); j++) {
                                                if (Objects.equals(hash, lessons.getString(j))) {
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
                                                .put("index", change.day)
                                                .put("lessons", new JSONArray().put(hash))
                                        );
                                    }
                                    Storage.file.perm.put(activity, "schedule_lessons#reduced#" + token, reduced.toString());
                                }
                            }
                            Static.toast(activity, getString(R.string.changes_applied));
                            close();
                            break;
                        }
                    }
                } catch (Exception e) {
                    Static.error(e);
                    Static.snackBar(activity, activity.getString(R.string.something_went_wrong));
                }
            }
        });
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
        meta = Objects.equals(meta, "") ? null : meta;
        if (meta == null) {
            textView.setHeight(0);
        } else {
            textView.setText(meta);
        }
    }
    private void setFlags(JSONObject lesson, ViewGroup viewGroup) throws Exception {
        String lType = lesson.getString("type");
        if (colorScheduleFlagTEXT == -1) colorScheduleFlagTEXT = Static.resolveColor(activity, R.attr.colorScheduleFlagTEXT);
        if (colorScheduleFlagCommonBG == -1) colorScheduleFlagCommonBG = Static.resolveColor(activity, R.attr.colorScheduleFlagCommonBG);
        if (colorScheduleFlagPracticeBG == -1) colorScheduleFlagPracticeBG = Static.resolveColor(activity, R.attr.colorScheduleFlagPracticeBG);
        if (colorScheduleFlagLectureBG == -1) colorScheduleFlagLectureBG = Static.resolveColor(activity, R.attr.colorScheduleFlagLectureBG);
        if (colorScheduleFlagLabBG == -1) colorScheduleFlagLabBG = Static.resolveColor(activity, R.attr.colorScheduleFlagLabBG);
        if (!lType.isEmpty()) {
            switch (lType) {
                case "practice":
                    viewGroup.addView(getFlag(activity.getString(R.string.practice), colorScheduleFlagTEXT, colorScheduleFlagPracticeBG));
                    break;
                case "lecture":
                    viewGroup.addView(getFlag(activity.getString(R.string.lecture), colorScheduleFlagTEXT, colorScheduleFlagLectureBG));
                    break;
                case "lab":
                    viewGroup.addView(getFlag(activity.getString(R.string.lab), colorScheduleFlagTEXT, colorScheduleFlagLabBG));
                    break;
                default:
                    viewGroup.addView(getFlag(lType, colorScheduleFlagTEXT, colorScheduleFlagCommonBG));
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
                temp.getParentFile().mkdirs();
                if (!temp.createNewFile()) {
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

    private void close() {
        Log.v(TAG, "close");
        if ("handle".equals(type)) {
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

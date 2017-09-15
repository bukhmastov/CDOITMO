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
    private String query = "";
    private String title = "";
    private String token = "";
    private ArrayList<Change> changes = new ArrayList<>();
    private boolean loaded = false;
    private Client.Request requestHandle = null;
    private int colorScheduleFlagTEXT = -1, colorScheduleFlagCommonBG = -1, colorScheduleFlagPracticeBG = -1, colorScheduleFlagLectureBG = -1, colorScheduleFlagLabBG = -1;

    @Override
    public void onCreate(@Nullable Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        FirebaseAnalyticsProvider.logCurrentScreen(activity, this);
        activity.updateToolbar(activity.getString(R.string.share_changes), R.drawable.ic_share);
    }

    @Override
    public View onCreateView(LayoutInflater inflater, ViewGroup container, Bundle savedInstanceState) {
        return inflater.inflate(R.layout.fragment_schedule_lessons_share, container, false);
    }

    @Override
    public void onResume() {
        super.onResume();
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
                activity.back();
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
                    Log.v(TAG, "load");
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
                                activity.back();
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
                                            activity.back();
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
                                        activity.back();
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
                                    } catch (Exception e){
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
                } catch (Exception e) {
                    Static.error(e);
                    Static.snackBar(activity, activity.getString(R.string.something_went_wrong));
                    activity.back();
                }
            }
        });
    }
    private void display() {
        Static.T.runOnUiThread(new Runnable() {
            @Override
            public void run() {
                try {
                    Log.v(TAG, "display");
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
                                ((TextView) header.findViewById(R.id.text)).setText(R.string.added_lessons);
                                share_content.addView(header);
                            }
                            if (headerReduced && change.type == TYPE.REDUCED) {
                                headerReduced = false;
                                final View header = inflate(R.layout.fragment_schedule_lessons_share_header);
                                ((TextView) header.findViewById(R.id.text)).setText(R.string.reduced_lessons);
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
                } catch (Exception e) {
                    Static.error(e);
                    Static.snackBar(activity, activity.getString(R.string.something_went_wrong));
                    activity.back();
                }
            }
        });
    }
    private void execute() {
        Static.T.runThread(new Runnable() {
            @Override
            public void run() {
                try {
                    Log.v(TAG, "execute");
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
                    JSONArray added = new JSONArray();
                    JSONArray reduced = new JSONArray();
                    for (Change change : changes) {
                        if (!change.enabled) continue;
                        (change.type == TYPE.ADDED ? added : reduced).put(new JSONObject().put("day", change.day).put("lesson", change.content));
                    }
                    JSONObject share = new JSONObject();
                    share.put("type", "share");
                    share.put("share", "schedule_lessons");
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
                        intent.setType(activity.getContentResolver().getType(uri));
                        intent.putExtra(Intent.EXTRA_STREAM, uri);
                        startActivity(Intent.createChooser(intent, activity.getString(R.string.share) + "..."));
                    }
                } catch (Exception e) {
                    Static.error(e);
                    Static.snackBar(activity, activity.getString(R.string.something_went_wrong));
                }
            }
        });
    }

    private void setDesc(final JSONObject lesson, TextView textView) throws Exception {
        String desc = null;
        switch (ScheduleLessonsFragment.schedule.getString("type")) {
            case "group":
                desc = lesson.getString("teacher");
                break;
            case "teacher":
                desc = lesson.getString("group");
                break;
            case "room":
                String group = lesson.getString("group");
                String teacher = lesson.getString("teacher");
                if (Objects.equals(group, "")) {
                    desc = teacher;
                } else {
                    desc = group;
                    if (!Objects.equals(teacher, "")) desc += " (" + teacher + ")";
                }
                break;
        }
        desc = Objects.equals(desc, "") ? null : desc;
        if (desc == null) {
            textView.setHeight(0);
        } else {
            textView.setText(desc);
        }
    }
    private void setMeta(final JSONObject lesson, TextView textView) throws Exception {
        String meta = null;
        switch (ScheduleLessonsFragment.schedule.getString("type")) {
            case "group":
            case "teacher":
                String room = lesson.getString("room");
                String building = lesson.getString("building");
                if (Objects.equals(room, "")) {
                    meta = building;
                } else {
                    meta = activity.getString(R.string.room_short) + " " + room;
                    if (!Objects.equals(building, "")) {
                        meta += " (" + building + ")";
                    }
                }
                break;
            case "room":
                meta = lesson.getString("building");
                break;
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
            File temp = new File(new File(activity.getCacheDir(), "shared"), "share_schedule_of_lessons.cdoitmo");
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

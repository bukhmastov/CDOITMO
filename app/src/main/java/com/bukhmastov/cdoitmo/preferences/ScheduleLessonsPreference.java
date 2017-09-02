package com.bukhmastov.cdoitmo.preferences;

import android.app.Activity;
import android.content.Context;
import android.util.AttributeSet;
import android.view.View;
import android.widget.AdapterView;
import android.widget.FrameLayout;
import android.widget.LinearLayout;
import android.widget.ListView;

import com.bukhmastov.cdoitmo.R;
import com.bukhmastov.cdoitmo.adapters.TeacherPickerListView;
import com.bukhmastov.cdoitmo.objects.ScheduleLessons;
import com.bukhmastov.cdoitmo.utils.Static;

import org.json.JSONArray;
import org.json.JSONObject;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.Objects;

public class ScheduleLessonsPreference extends SchedulePreference implements ScheduleLessons.response {

    private ScheduleLessons scheduleLessons = null;

    public ScheduleLessonsPreference(Context context, AttributeSet attrs, int defStyleAttr, int defStyleRes) {
        super(context, attrs, defStyleAttr, defStyleRes);
        init(context);
    }
    public ScheduleLessonsPreference(Context context, AttributeSet attrs, int defStyleAttr) {
        super(context, attrs, defStyleAttr);
        init(context);
    }
    public ScheduleLessonsPreference(Context context, AttributeSet attrs) {
        super(context, attrs);
        init(context);
    }
    public ScheduleLessonsPreference(Context context) {
        super(context);
        init(context);
    }
    private void init(Context context) {
        scheduleLessons = new ScheduleLessons(context);
        scheduleLessons.setHandler(this);
    }

    @Override
    protected void search(String search) {
        scheduleLessons.search(search);
    }
    public void onSuccess(final JSONObject json) {
        Static.T.runOnUiThread(new Runnable() {
            @Override
            public void run() {
                try {
                    if (json == null) throw new NullPointerException("json cannot be null");
                    if (Objects.equals(json.getString("type"), "teacher_picker")) {
                        JSONArray teachers = json.getJSONArray("list");
                        if (teachers.length() > 0) {
                            if (teachers.length() == 1) {
                                JSONObject teacher = teachers.getJSONObject(0);
                                String name = teacher.getString("person") + " (" + teacher.getString("post") + ")";
                                setValue(teacher.getString("pid"), name);
                                found(getContext().getString(R.string.schedule_teacher_set) + " \"" + name + "\"");
                            } else {
                                if (preference_schedule == null) return;
                                FrameLayout schedule_preference_list = preference_schedule.findViewById(R.id.schedule_preference_list);
                                if (schedule_preference_list == null) throw new NullPointerException("slw_container cannot be null");
                                ListView listView = new ListView(getContext());
                                listView.setLayoutParams(new LinearLayout.LayoutParams(LinearLayout.LayoutParams.MATCH_PARENT, LinearLayout.LayoutParams.MATCH_PARENT));
                                final ArrayList<HashMap<String, String>> teachersMap = new ArrayList<>();
                                for (int i = 0; i < teachers.length(); i++) {
                                    JSONObject teacher = teachers.getJSONObject(i);
                                    HashMap<String, String> teacherMap = new HashMap<>();
                                    teacherMap.put("pid", String.valueOf(teacher.getInt("pid")));
                                    teacherMap.put("person", teacher.getString("person"));
                                    teacherMap.put("post", teacher.getString("post"));
                                    teachersMap.add(teacherMap);
                                }
                                listView.setAdapter(new TeacherPickerListView((Activity) getContext(), teachersMap));
                                listView.setOnItemClickListener(new AdapterView.OnItemClickListener() {
                                    public void onItemClick(AdapterView<?> parent, View view, int position, long id) {
                                        HashMap<String, String> teacherMap = teachersMap.get(position);
                                        String text = teacherMap.get("person");
                                        if (teacherMap.get("post") != null && !Objects.equals(teacherMap.get("post"), "") && !Objects.equals(teacherMap.get("post"), "null")) {
                                            text += " (" + teacherMap.get("post") + ")";
                                        }
                                        setValue(teacherMap.get("pid"), text);
                                        found(getContext().getString(R.string.schedule_teacher_set) + " \"" + text + "\"");
                                    }
                                });
                                schedule_preference_list.removeAllViews();
                                schedule_preference_list.addView(listView);
                            }
                        } else {
                            found(getContext().getString(R.string.schedule_not_found));
                        }
                    } else {
                        if (json.getJSONArray("schedule").length() > 0) {
                            switch(json.getString("type")){
                                case "group":
                                    setValue(json.getString("query"), getContext().getString(R.string.group) + " " + json.getString("label"));
                                    found(getContext().getString(R.string.schedule_group_set) + " \"" + json.getString("label") + "\"");
                                    break;
                                case "room":
                                    setValue(json.getString("query"), getContext().getString(R.string.room) + " " + json.getString("label"));
                                    found(getContext().getString(R.string.schedule_room_set) + " \"" + json.getString("label") + "\"");
                                    break;
                                default:
                                    found(getContext().getString(R.string.schedule_not_found));
                                    break;
                            }
                        } else {
                            found(getContext().getString(R.string.schedule_not_found));
                        }
                    }
                } catch (Exception e){
                    Static.error(e);
                    found(getContext().getString(R.string.schedule_not_found));
                }
            }
        });
    }
}

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
import com.bukhmastov.cdoitmo.objects.ScheduleExams;
import com.bukhmastov.cdoitmo.utils.Static;

import org.json.JSONArray;
import org.json.JSONObject;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.Objects;

public class ScheduleExamsPreference extends SchedulePreference implements ScheduleExams.response {

    private ScheduleExams scheduleExams = null;

    public ScheduleExamsPreference(Context context, AttributeSet attrs, int defStyleAttr, int defStyleRes) {
        super(context, attrs, defStyleAttr, defStyleRes);
        init(context);
    }
    public ScheduleExamsPreference(Context context, AttributeSet attrs, int defStyleAttr) {
        super(context, attrs, defStyleAttr);
        init(context);
    }
    public ScheduleExamsPreference(Context context, AttributeSet attrs) {
        super(context, attrs);
        init(context);
    }
    public ScheduleExamsPreference(Context context) {
        super(context);
        init(context);
    }
    private void init(Context context) {
        scheduleExams = new ScheduleExams(context);
        scheduleExams.setHandler(this);
    }

    @Override
    protected void search(String search) {
        scheduleExams.search(search);
    }
    public void onSuccess(final JSONObject json) {
        Static.T.runOnUiThread(new Runnable() {
            @Override
            public void run() {
                try {
                    if (json == null) throw new NullPointerException("json cannot be null");
                    if (Objects.equals(json.getString("type"), "teacher_picker")) {
                        JSONArray teachers = json.getJSONArray("teachers");
                        if (teachers.length() > 0) {
                            if (teachers.length() == 1) {
                                JSONObject teacher = teachers.getJSONObject(0);
                                setValue(teacher.getString("scope"), teacher.getString("name"));
                                found(getContext().getString(R.string.schedule_teacher_set) + " \"" + teacher.getString("name") + "\"");
                            } else {
                                if (preference_schedule == null) return;
                                FrameLayout schedule_preference_list = (FrameLayout) preference_schedule.findViewById(R.id.schedule_preference_list);
                                if (schedule_preference_list == null)
                                    throw new NullPointerException("slw_container cannot be null");
                                ListView listView = new ListView(getContext());
                                listView.setLayoutParams(new LinearLayout.LayoutParams(LinearLayout.LayoutParams.MATCH_PARENT, LinearLayout.LayoutParams.MATCH_PARENT));
                                final ArrayList<HashMap<String, String>> teachersMap = new ArrayList<>();
                                for (int i = 0; i < teachers.length(); i++) {
                                    JSONObject teacher = teachers.getJSONObject(i);
                                    HashMap<String, String> teacherMap = new HashMap<>();
                                    teacherMap.put("pid", teacher.getString("scope"));
                                    teacherMap.put("person", teacher.getString("name"));
                                    teacherMap.put("post", "");
                                    teachersMap.add(teacherMap);
                                }
                                listView.setAdapter(new TeacherPickerListView((Activity) getContext(), teachersMap));
                                listView.setOnItemClickListener(new AdapterView.OnItemClickListener() {
                                    public void onItemClick(AdapterView<?> parent, View view, int position, long id) {
                                        HashMap<String, String> teacherMap = teachersMap.get(position);
                                        setValue(teacherMap.get("pid"), teacherMap.get("person"));
                                        found(getContext().getString(R.string.schedule_teacher_set) + " \"" + teacherMap.get("person") + "\"");
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
                            switch (json.getString("type")) {
                                case "group":
                                    setValue(json.getString("scope"), getContext().getString(R.string.group) + " " + json.getString("scope"));
                                    found(getContext().getString(R.string.schedule_group_set) + " \"" + json.getString("scope") + "\"");
                                    break;
                                case "room":
                                    setValue(json.getString("scope"), getContext().getString(R.string.room) + " " + json.getString("scope"));
                                    found(getContext().getString(R.string.schedule_room_set) + " \"" + json.getString("scope") + "\"");
                                    break;
                                default:
                                    found(getContext().getString(R.string.schedule_not_found));
                                    break;
                            }
                        } else {
                            found(getContext().getString(R.string.schedule_not_found));
                        }
                    }
                } catch (Exception e) {
                    Static.error(e);
                    found(getContext().getString(R.string.schedule_not_found));
                }
            }
        });
    }
}

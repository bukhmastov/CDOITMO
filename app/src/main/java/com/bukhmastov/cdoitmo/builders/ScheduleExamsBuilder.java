package com.bukhmastov.cdoitmo.builders;

import android.app.Activity;
import android.content.Context;
import android.view.ContextMenu;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.LinearLayout;
import android.widget.TextView;

import com.bukhmastov.cdoitmo.R;
import com.bukhmastov.cdoitmo.fragments.ScheduleExamsFragment;
import com.bukhmastov.cdoitmo.utils.Log;
import com.bukhmastov.cdoitmo.utils.Static;

import org.json.JSONArray;
import org.json.JSONObject;

import java.util.Objects;

public class ScheduleExamsBuilder extends Thread {

    private static final String TAG = "ScheduleExamsBuilder";
    public interface response {
        void state(int state, View layout);
    }
    private response delegate = null;
    private Activity activity;
    private float destiny;

    public static final int STATE_FAILED = 0;
    public static final int STATE_LOADING = 1;
    public static final int STATE_DONE = 2;

    public ScheduleExamsBuilder(Activity activity, ScheduleExamsBuilder.response delegate){
        Log.i(TAG, "created");
        this.activity = activity;
        this.delegate = delegate;
        this.destiny = activity.getResources().getDisplayMetrics().density;
    }
    public void run(){
        Log.v(TAG, "started");
        LinearLayout container = new LinearLayout(activity);
        container.setOrientation(LinearLayout.VERTICAL);
        container.setLayoutParams(new LinearLayout.LayoutParams(LinearLayout.LayoutParams.MATCH_PARENT, LinearLayout.LayoutParams.WRAP_CONTENT));
        try {
            delegate.state(STATE_LOADING, inflate(R.layout.state_loading_compact));
            JSONArray schedule = ScheduleExamsFragment.schedule.getJSONArray("schedule");
            String type = ScheduleExamsFragment.schedule.getString("type");
            for (int i = 0; i < schedule.length(); i++) {
                JSONObject exam = schedule.getJSONObject(i);
                LinearLayout examsLayout = (LinearLayout) inflate(R.layout.layout_schedule_exams_item);
                ((TextView) examsLayout.findViewById(R.id.exam_header)).setText(exam.getString("subject").toUpperCase());
                String desc = null;
                switch (type) {
                    case "group": desc = exam.getString("teacher"); break;
                    case "teacher": desc = exam.getString("group"); break;
                }
                ((TextView) examsLayout.findViewById(R.id.exam_desc)).setText(desc);
                int count = 2;
                if (exam.has("consult") && exam.getJSONObject("consult").has("date")) {
                    ((TextView) examsLayout.findViewById(R.id.exam_ie_title)).setText(activity.getString(R.string.consult).toUpperCase());
                    ((TextView) examsLayout.findViewById(R.id.exam_ie_date)).setText((exam.getJSONObject("consult").getString("date") + " " + exam.getJSONObject("consult").getString("time")).trim());
                    ((TextView) examsLayout.findViewById(R.id.exam_ie_place)).setText(Objects.equals(exam.getJSONObject("consult").getString("room"), "") ? "" : activity.getString(R.string.place) + ": " + exam.getJSONObject("consult").getString("room"));
                } else {
                    examsLayout.removeView(examsLayout.findViewById(R.id.exam_info_exam));
                    count--;
                }
                if (exam.has("exam") && exam.getJSONObject("exam").has("date")) {
                    ((TextView) examsLayout.findViewById(R.id.exam_ic_title)).setText(activity.getString(R.string.exam).toUpperCase());
                    ((TextView) examsLayout.findViewById(R.id.exam_ic_date)).setText((exam.getJSONObject("exam").getString("date") + " " + exam.getJSONObject("exam").getString("time")).trim());
                    ((TextView) examsLayout.findViewById(R.id.exam_ic_place)).setText(Objects.equals(exam.getJSONObject("exam").getString("room"), "") ? "" : activity.getString(R.string.place) + ": " + exam.getJSONObject("exam").getString("room"));
                } else {
                    examsLayout.removeView(examsLayout.findViewById(R.id.exam_info_consult));
                    count--;
                }
                if (count == 1) examsLayout.removeView(examsLayout.findViewById(R.id.separator_small));
                if (count == 0) examsLayout.removeView(examsLayout.findViewById(R.id.separator_big));
                final String group = exam.has("group") ? (Objects.equals(exam.getString("group"), "") ? null : exam.getString("group")) : null;
                final String teacher = exam.has("teacher") ? (Objects.equals(exam.getString("teacher"), "") ? null : exam.getString("teacher")) : null;
                if (group != null || teacher != null) {
                    examsLayout.setOnCreateContextMenuListener(new View.OnCreateContextMenuListener() {
                        @Override
                        public void onCreateContextMenu(ContextMenu menu, View v, ContextMenu.ContextMenuInfo menuInfo) {
                            Log.v(TAG, "onCreateContextMenu");
                            menu.setHeaderTitle(R.string.open_schedule);
                            if (group != null) menu.add(activity.getString(R.string.group) + " " + group);
                            if (teacher != null) menu.add(teacher);
                        }
                    });
                }
                container.addView(examsLayout);
            }
            if (schedule.length() == 0) {
                Log.v(TAG, "schedule.length() == 0");
                container.addView(inflate(R.layout.layout_schedule_exams_without_exams));
            } else {
                TextView textView = new TextView(activity);
                textView.setLayoutParams(new ViewGroup.LayoutParams(ViewGroup.LayoutParams.MATCH_PARENT, ViewGroup.LayoutParams.WRAP_CONTENT));
                textView.setPadding((int) (16 * destiny), (int) (10 * destiny), (int) (16 * destiny), 0);
                textView.setTextColor(Static.textColorSecondary);
                textView.setTextSize(13);
                textView.setText(activity.getString(R.string.update_date) + " " + Static.getUpdateTime(activity, ScheduleExamsFragment.schedule.getLong("timestamp")));
                container.addView(textView);
            }
            delegate.state(STATE_DONE, container);
        } catch (Exception e) {
            Static.error(e);
            delegate.state(STATE_FAILED, container);
        }
        Log.v(TAG, "finished");
    }

    private View inflate(int layout) throws Exception {
        return ((LayoutInflater) activity.getSystemService(Context.LAYOUT_INFLATER_SERVICE)).inflate(layout, null);
    }

}

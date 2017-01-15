package com.bukhmastov.cdoitmo;

import android.content.Context;
import android.os.Bundle;
import android.support.v4.app.Fragment;
import android.support.v4.widget.SwipeRefreshLayout;
import android.util.TypedValue;
import android.view.ContextMenu;
import android.view.LayoutInflater;
import android.view.MenuItem;
import android.view.View;
import android.view.ViewGroup;
import android.widget.LinearLayout;
import android.widget.TextView;

import org.json.JSONArray;
import org.json.JSONObject;
import org.w3c.dom.Text;

import java.text.SimpleDateFormat;
import java.util.Calendar;
import java.util.Date;
import java.util.Locale;
import java.util.Objects;

public class ScheduleLessonsAllFragment extends Fragment {

    private int type = 2;
    private boolean displayed = false;

    public ScheduleLessonsAllFragment() {}

    @Override
    public View onCreateView(LayoutInflater inflater, ViewGroup container, Bundle savedInstanceState) {
        return inflater.inflate(R.layout.fragment_schedule_lessons_all, container, false);
    }

    @Override
    public void onResume() {
        if(!displayed) display();
        super.onResume();
    }

    @Override
    public void onDestroy() {
        displayed = false;
        super.onDestroy();
    }

    @Override
    public void onCreateContextMenu(ContextMenu menu, View v, ContextMenu.ContextMenuInfo menuInfo) {
        super.onCreateContextMenu(menu, v, menuInfo);
        menu.setHeaderTitle("Открыть распиание");
        String room = v.getTag(R.id.schedule_lessons_room).toString();
        String teacher = v.getTag(R.id.schedule_lessons_teacher).toString();
        String group = v.getTag(R.id.schedule_lessons_group).toString();
        if(!Objects.equals(group, "")) menu.add(getString(R.string.group) + " " + group);
        if(!Objects.equals(teacher, "")) menu.add(teacher);
        if(!Objects.equals(room, "")) menu.add(getString(R.string.room) + " " + room);
    }

    @Override
    public boolean onContextItemSelected(MenuItem item) {
        if(getUserVisibleHint()){
            ScheduleLessonsFragment.scheduleLessons.search(item.getTitle().toString().replace(getString(R.string.group), "").replace(getString(R.string.room), "").trim(), false);
            return true;
        }
        return super.onContextItemSelected(item);
    }

    private void display(){
        try {
            if(ScheduleLessonsFragment.schedule == null) throw new NullPointerException("ScheduleLessonsFragment.schedule cannot be null");
            TextView schedule_c_header = (TextView) getActivity().findViewById(R.id.schedule_lessons_all_header);
            switch (ScheduleLessonsFragment.schedule.getString("type")){
                case "group": schedule_c_header.setText("Расписание группы" + " " + ScheduleLessonsFragment.schedule.getString("scope")); break;
                case "room": schedule_c_header.setText("Расписание в аудитории" + " " + ScheduleLessonsFragment.schedule.getString("scope")); break;
                case "teacher": schedule_c_header.setText("Расписание преподавателя" + " " + ScheduleLessonsFragment.schedule.getString("scope")); break;
                default: throw new Exception("Wrong ScheduleLessonsFragment.schedule.type value");
            }
            TextView schedule_lessons_all_week = (TextView) getActivity().findViewById(R.id.schedule_lessons_all_week);
            if(MainActivity.week >= 0){
                schedule_lessons_all_week.setText(MainActivity.week + " " + getString(R.string.school_week));
            } else {
                schedule_lessons_all_week.setText(new SimpleDateFormat("dd.MM.yyyy", Locale.ROOT).format(new Date(Calendar.getInstance().getTimeInMillis())));
            }
            // работаем со свайпом
            SwipeRefreshLayout mSwipeRefreshLayout = (SwipeRefreshLayout) getActivity().findViewById(R.id.schedule_lessons_all_container);
            TypedValue typedValue = new TypedValue();
            getActivity().getTheme().resolveAttribute(R.attr.colorAccent, typedValue, true);
            mSwipeRefreshLayout.setColorSchemeColors(typedValue.data);
            getActivity().getTheme().resolveAttribute(R.attr.colorBackgroundRefresh, typedValue, true);
            mSwipeRefreshLayout.setProgressBackgroundColorSchemeColor(typedValue.data);
            mSwipeRefreshLayout.setOnRefreshListener(ScheduleLessonsFragment.scheduleLessons);
            // отображаем расписание
            LinearLayout linearLayout = (LinearLayout) getActivity().findViewById(R.id.schedule_lessons_all_content);
            linearLayout.removeAllViews();
            ScheduleLessonsFragment.scheduleLessons.getSchedule(getContext(), type, linearLayout, new ScheduleLessons.contextMenu() {
                @Override
                public void register(View view) { registerForContextMenu(view); }
            });
            displayed = true;
        } catch (Exception e){
            e.printStackTrace();
            failed();
        }
    }

    private void failed(){
        try {
            draw(R.layout.state_try_again);
            getActivity().findViewById(R.id.try_again_reload).setOnClickListener(new View.OnClickListener() {
                @Override
                public void onClick(View v) {
                    ScheduleLessonsFragment.scheduleLessons.search(ScheduleLessonsFragment.query, false);
                }
            });
        } catch (Exception e){
            e.printStackTrace();
        }
    }
    private void draw(int layoutId){
        try {
            ViewGroup vg = ((ViewGroup) getActivity().findViewById(R.id.container_schedule_lessons_all));
            vg.removeAllViews();
            vg.addView(((LayoutInflater) getActivity().getSystemService(Context.LAYOUT_INFLATER_SERVICE)).inflate(layoutId, null), 0, new ViewGroup.LayoutParams(ViewGroup.LayoutParams.MATCH_PARENT, ViewGroup.LayoutParams.MATCH_PARENT));
        } catch (Exception e){
            e.printStackTrace();
        }
    }
}

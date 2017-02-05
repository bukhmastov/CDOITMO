package com.bukhmastov.cdoitmo;

import android.content.Context;
import android.os.Bundle;
import android.support.v4.app.Fragment;
import android.support.v4.widget.SwipeRefreshLayout;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.LinearLayout;
import android.widget.TextView;

import java.text.SimpleDateFormat;
import java.util.Calendar;
import java.util.Date;
import java.util.Locale;

public class ScheduleLessonsEvenFragment extends Fragment {

    private static final int TYPE = 0;
    private boolean displayed = false;

    public ScheduleLessonsEvenFragment() {}

    @Override
    public View onCreateView(LayoutInflater inflater, ViewGroup container, Bundle savedInstanceState) {
        return inflater.inflate(R.layout.fragment_schedule_lessons_even, container, false);
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

    private void display(){
        try {
            if(ScheduleLessonsFragment.schedule == null) throw new NullPointerException("ScheduleLessonsFragment.schedule cannot be null");
            TextView schedule_c_header = (TextView) getActivity().findViewById(R.id.schedule_lessons_even_header);
            switch (ScheduleLessonsFragment.schedule.getString("type")){
                case "group": if(schedule_c_header != null) schedule_c_header.setText("Расписание группы" + " " + ScheduleLessonsFragment.schedule.getString("scope")); break;
                case "room": if(schedule_c_header != null) schedule_c_header.setText("Расписание в аудитории" + " " + ScheduleLessonsFragment.schedule.getString("scope")); break;
                case "teacher": if(schedule_c_header != null) schedule_c_header.setText("Расписание преподавателя" + " " + ScheduleLessonsFragment.schedule.getString("scope")); break;
                default: throw new Exception("Wrong ScheduleLessonsFragment.schedule.TYPE value");
            }
            TextView schedule_lessons_even_week = (TextView) getActivity().findViewById(R.id.schedule_lessons_even_week);
            if (schedule_lessons_even_week != null) {
                if (MainActivity.week >= 0) {
                    schedule_lessons_even_week.setText(MainActivity.week + " " + getString(R.string.school_week));
                } else {
                    schedule_lessons_even_week.setText(new SimpleDateFormat("dd.MM.yyyy", Locale.ROOT).format(new Date(Calendar.getInstance().getTimeInMillis())));
                }
            }
            // работаем со свайпом
            SwipeRefreshLayout mSwipeRefreshLayout = (SwipeRefreshLayout) getActivity().findViewById(R.id.schedule_lessons_even_container);
            if (mSwipeRefreshLayout != null) {
                mSwipeRefreshLayout.setColorSchemeColors(MainActivity.colorAccent);
                mSwipeRefreshLayout.setProgressBackgroundColorSchemeColor(MainActivity.colorBackgroundRefresh);
                mSwipeRefreshLayout.setOnRefreshListener(ScheduleLessonsFragment.scheduleLessons);
            }
            // отображаем расписание
            final ViewGroup linearLayout = (ViewGroup) getActivity().findViewById(R.id.schedule_lessons_even_content);
            (new ScheduleLessonsBuilder(getActivity(), TYPE, new ScheduleLessonsBuilder.response(){
                public void state(final int state, final LinearLayout layout){
                    try {
                        getActivity().runOnUiThread(new Runnable() {
                            @Override
                            public void run() {
                                if (linearLayout != null) {
                                    linearLayout.removeAllViews();
                                    if (state == ScheduleLessonsBuilder.STATE_DONE) {
                                        linearLayout.addView(layout);
                                        displayed = true;
                                    } else if (state == ScheduleLessonsBuilder.STATE_LOADING) {
                                        linearLayout.addView(layout);
                                    } else if (state == ScheduleLessonsBuilder.STATE_FAILED) {
                                        failed();
                                    }
                                }
                            }
                        });
                    } catch (NullPointerException e){
                        if(LoginActivity.errorTracker != null) LoginActivity.errorTracker.add(e);
                        failed();
                    }
                }
            })).start();
        } catch (Exception e){
            if(LoginActivity.errorTracker != null) LoginActivity.errorTracker.add(e);
            failed();
        }
    }

    private void failed(){
        try {
            draw(R.layout.state_try_again);
            View try_again_reload = getActivity().findViewById(R.id.try_again_reload);
            if (try_again_reload != null) {
                try_again_reload.setOnClickListener(new View.OnClickListener() {
                    @Override
                    public void onClick(View v) {
                        ScheduleLessonsFragment.scheduleLessons.search(ScheduleLessonsFragment.query, false);
                    }
                });
            }
        } catch (Exception e){
            if(LoginActivity.errorTracker != null) LoginActivity.errorTracker.add(e);
        }
    }
    private void draw(int layoutId){
        try {
            ViewGroup vg = ((ViewGroup) getActivity().findViewById(R.id.container_schedule_lessons_even));
            if (vg != null) {
                vg.removeAllViews();
                vg.addView(((LayoutInflater) getActivity().getSystemService(Context.LAYOUT_INFLATER_SERVICE)).inflate(layoutId, null), 0, new ViewGroup.LayoutParams(ViewGroup.LayoutParams.MATCH_PARENT, ViewGroup.LayoutParams.MATCH_PARENT));
            }
        } catch (Exception e){
            if(LoginActivity.errorTracker != null) LoginActivity.errorTracker.add(e);
        }
    }
}

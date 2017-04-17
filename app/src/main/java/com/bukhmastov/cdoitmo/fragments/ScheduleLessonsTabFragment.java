package com.bukhmastov.cdoitmo.fragments;

import android.content.Context;
import android.os.Build;
import android.os.Bundle;
import android.support.annotation.Nullable;
import android.support.design.widget.Snackbar;
import android.support.v4.app.Fragment;
import android.support.v4.widget.SwipeRefreshLayout;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.FrameLayout;
import android.widget.ImageView;
import android.widget.LinearLayout;
import android.widget.ScrollView;
import android.widget.TextView;

import com.bukhmastov.cdoitmo.R;
import com.bukhmastov.cdoitmo.builders.ScheduleLessonsBuilder;
import com.bukhmastov.cdoitmo.utils.Log;
import com.bukhmastov.cdoitmo.utils.Static;
import com.bukhmastov.cdoitmo.utils.Storage;

import java.text.SimpleDateFormat;
import java.util.Calendar;
import java.util.Date;
import java.util.Locale;

public class ScheduleLessonsTabFragment extends Fragment {

    private static final String TAG = "SLTabFragment";
    private int TYPE = -1;
    private boolean displayed = false;
    private View fragment_schedule_lessons = null;

    public ScheduleLessonsTabFragment() {
        super();
    }

    @Override
    public void onCreate(@Nullable Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        Log.v(TAG, "Fragment created");
    }

    @Override
    public void onDestroy() {
        displayed = false;
        Log.v(TAG, "Fragment destroyed");
        super.onDestroy();
    }

    @Override
    public View onCreateView(LayoutInflater inflater, ViewGroup container, Bundle savedInstanceState) {
        Bundle bundle = getArguments();
        TYPE = bundle.getInt("type");
        Log.v(TAG, "onCreateView | TYPE=" + TYPE);
        fragment_schedule_lessons = inflater.inflate(R.layout.fragment_tab_schedule_lessons, container, false);
        return fragment_schedule_lessons;
    }

    @Override
    public void onResume() {
        super.onResume();
        Log.v(TAG, "resumed");
        if (!displayed) display();
    }

    @Override
    public void onPause() {
        super.onPause();
        Log.v(TAG, "paused");
    }

    private void display(){
        Log.v(TAG, "display");
        try {
            if (TYPE < 0) throw new NullPointerException("ScheduleLessonsTabFragment.TYPE negative");
            if (fragment_schedule_lessons == null) throw new NullPointerException("ScheduleLessonsTabFragment.fragment_schedule_lessons cannot be null");
            if (ScheduleLessonsFragment.schedule == null) throw new NullPointerException("ScheduleLessonsFragment.schedule cannot be null");
            TextView schedule_c_header = (TextView) fragment_schedule_lessons.findViewById(R.id.schedule_lessons_header);
            switch (ScheduleLessonsFragment.schedule.getString("type")){
                case "group":
                case "room":
                case "teacher":
                    if (schedule_c_header != null) {
                        schedule_c_header.setText(ScheduleLessonsFragment.schedule.getString("title") + " " + ScheduleLessonsFragment.schedule.getString("label"));
                    }
                    break;
                default:
                    String exception = "Wrong ScheduleLessonsFragment.schedule.TYPE value: " + ScheduleLessonsFragment.schedule.getString("type");
                    Log.wtf(TAG, exception);
                    throw new Exception(exception);
            }
            TextView schedule_lessons_all_week = (TextView) fragment_schedule_lessons.findViewById(R.id.schedule_lessons_week);
            if (schedule_lessons_all_week != null) {
                if (Static.week >= 0) {
                    schedule_lessons_all_week.setText(Static.week + " " + getString(R.string.school_week));
                } else {
                    schedule_lessons_all_week.setText(new SimpleDateFormat("dd.MM.yyyy", Locale.ROOT).format(new Date(Calendar.getInstance().getTimeInMillis())));
                }
            }
            FrameLayout schedule_lessons_cache = (FrameLayout) fragment_schedule_lessons.findViewById(R.id.schedule_lessons_cache);
            if (schedule_lessons_cache != null) {
                ImageView cacheImage = new ImageView(getContext());
                cacheImage.setImageDrawable(getActivity().getResources().getDrawable(ScheduleLessonsFragment.schedule_cached ? R.drawable.ic_cached : R.drawable.ic_cache, getActivity().getTheme()));
                cacheImage.setLayoutParams(new LinearLayout.LayoutParams(LinearLayout.LayoutParams.MATCH_PARENT, LinearLayout.LayoutParams.MATCH_PARENT));
                int padding = (int) (getActivity().getResources().getDisplayMetrics().density * 4);
                cacheImage.setPadding(padding, padding, padding, padding);
                schedule_lessons_cache.addView(cacheImage);
                schedule_lessons_cache.setOnClickListener(new View.OnClickListener() {
                    @Override
                    public void onClick(View v) {
                        Log.v(TAG, "schedule_lessons_cache clicked");
                        if (ScheduleLessonsFragment.scheduleLessons != null) {
                            Boolean result = ScheduleLessonsFragment.scheduleLessons.toggleCache();
                            if (result == null) {
                                Log.w(TAG, "failed to toggle cache");
                                snackBar(getString(R.string.cache_failed));
                            } else {
                                snackBar(result ? getString(R.string.cache_true) : getString(R.string.cache_false));
                                if (fragment_schedule_lessons != null) {
                                    FrameLayout schedule_lessons_cache = (FrameLayout) fragment_schedule_lessons.findViewById(R.id.schedule_lessons_cache);
                                    if (schedule_lessons_cache != null) {
                                        ImageView cacheImage = new ImageView(getContext());
                                        cacheImage.setImageDrawable(getActivity().getResources().getDrawable(result ? R.drawable.ic_cached : R.drawable.ic_cache, getActivity().getTheme()));
                                        cacheImage.setLayoutParams(new LinearLayout.LayoutParams(LinearLayout.LayoutParams.MATCH_PARENT, LinearLayout.LayoutParams.MATCH_PARENT));
                                        int padding = (int) (getActivity().getResources().getDisplayMetrics().density * 4);
                                        cacheImage.setPadding(padding, padding, padding, padding);
                                        schedule_lessons_cache.removeAllViews();
                                        schedule_lessons_cache.addView(cacheImage);
                                    }
                                }
                            }
                        } else {
                            Log.v(TAG, "schedule_lessons_cache clicked | ScheduleLessonsFragment.scheduleLessons is null");
                        }
                    }
                });
            }
            // работаем со свайпом
            SwipeRefreshLayout mSwipeRefreshLayout = (SwipeRefreshLayout) fragment_schedule_lessons.findViewById(R.id.swipe_schedule_lessons);
            if (mSwipeRefreshLayout != null) {
                mSwipeRefreshLayout.setColorSchemeColors(Static.colorAccent);
                mSwipeRefreshLayout.setProgressBackgroundColorSchemeColor(Static.colorBackgroundRefresh);
                mSwipeRefreshLayout.setOnRefreshListener(ScheduleLessonsFragment.scheduleLessons);
            }
            // отображаем расписание
            final ViewGroup schedule_lessons_content = (ViewGroup) fragment_schedule_lessons.findViewById(R.id.schedule_lessons_content);
            (new ScheduleLessonsBuilder(getActivity(), TYPE, new ScheduleLessonsBuilder.response(){
                public void state(final int state, final View layout){
                    try {
                        getActivity().runOnUiThread(new Runnable() {
                            @Override
                            public void run() {
                                if (schedule_lessons_content != null) {
                                    schedule_lessons_content.removeAllViews();
                                    if (state == ScheduleLessonsBuilder.STATE_DONE) {
                                        schedule_lessons_content.addView(layout);
                                        displayed = true;
                                        if (Storage.pref.get(getContext(), "pref_schedule_lessons_scroll_to_day", true)) {
                                            final ScrollView scroll_schedule_lessons = (ScrollView) fragment_schedule_lessons.findViewById(R.id.scroll_schedule_lessons);
                                            scroll_schedule_lessons.post(new Runnable() {
                                                @Override
                                                public void run() {
                                                    Integer scroll = ScheduleLessonsFragment.scroll.get(TYPE);
                                                    if (scroll == 0) {
                                                        View day;
                                                        switch (Calendar.getInstance().get(Calendar.DAY_OF_WEEK)) {
                                                            case Calendar.MONDAY:
                                                                day = fragment_schedule_lessons.findViewById(R.id.monday);
                                                                if (day != null) break;
                                                            case Calendar.TUESDAY:
                                                                day = fragment_schedule_lessons.findViewById(R.id.tuesday);
                                                                if (day != null) break;
                                                            case Calendar.WEDNESDAY:
                                                                day = fragment_schedule_lessons.findViewById(R.id.wednesday);
                                                                if (day != null) break;
                                                            case Calendar.THURSDAY:
                                                                day = fragment_schedule_lessons.findViewById(R.id.thursday);
                                                                if (day != null) break;
                                                            case Calendar.FRIDAY:
                                                                day = fragment_schedule_lessons.findViewById(R.id.friday);
                                                                if (day != null) break;
                                                            case Calendar.SATURDAY:
                                                                day = fragment_schedule_lessons.findViewById(R.id.saturday);
                                                                if (day != null) break;
                                                            case Calendar.SUNDAY:
                                                                day = fragment_schedule_lessons.findViewById(R.id.sunday);
                                                                if (day != null) break;
                                                            default:
                                                                day = null;
                                                                break;
                                                        }
                                                        if (day != null) {
                                                            int y = day.getTop();
                                                            View header_schedule_lessons = fragment_schedule_lessons.findViewById(R.id.header_schedule_lessons);
                                                            if (header_schedule_lessons != null) {
                                                                y += header_schedule_lessons.getMeasuredHeight();
                                                            }
                                                            y += schedule_lessons_content.getPaddingTop();
                                                            y += 1;
                                                            scroll_schedule_lessons.scrollTo(0, y);
                                                        }
                                                    } else {
                                                        scroll_schedule_lessons.scrollTo(0, scroll);
                                                    }
                                                }
                                            });
                                            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.M) {
                                                scroll_schedule_lessons.setOnScrollChangeListener(new View.OnScrollChangeListener() {
                                                    @Override
                                                    public void onScrollChange(View v, int scrollX, int scrollY, int oldScrollX, int oldScrollY) {
                                                        ScheduleLessonsFragment.scroll.put(TYPE, scrollY);
                                                    }
                                                });
                                            }
                                        }
                                    } else if (state == ScheduleLessonsBuilder.STATE_LOADING) {
                                        schedule_lessons_content.addView(layout);
                                    } else if (state == ScheduleLessonsBuilder.STATE_FAILED) {
                                        failed();
                                    }
                                }
                            }
                        });
                    } catch (NullPointerException e){
                        Static.error(e);
                        failed();
                    }
                }
            })).start();
        } catch (Exception e){
            Static.error(e);
            failed();
        }
    }

    private void failed(){
        Log.v(TAG, "failed");
        try {
            if (fragment_schedule_lessons == null) throw new NullPointerException("ScheduleLessonsTabFragment.fragment_schedule_lessons cannot be null");
            draw(R.layout.state_try_again);
            View try_again_reload = fragment_schedule_lessons.findViewById(R.id.try_again_reload);
            if (try_again_reload != null) {
                try_again_reload.setOnClickListener(new View.OnClickListener() {
                    @Override
                    public void onClick(View v) {
                        ScheduleLessonsFragment.search(ScheduleLessonsFragment.query);
                    }
                });
            }
        } catch (Exception e){
            Static.error(e);
        }
    }
    private void draw(int layoutId){
        try {
            if (fragment_schedule_lessons == null) throw new NullPointerException("ScheduleLessonsTabFragment.fragment_schedule_lessons cannot be null");
            ViewGroup vg = ((ViewGroup) fragment_schedule_lessons.findViewById(R.id.container_schedule_lessons));
            if (vg != null) {
                vg.removeAllViews();
                vg.addView(((LayoutInflater) getActivity().getSystemService(Context.LAYOUT_INFLATER_SERVICE)).inflate(layoutId, null), 0, new ViewGroup.LayoutParams(ViewGroup.LayoutParams.MATCH_PARENT, ViewGroup.LayoutParams.MATCH_PARENT));
            }
        } catch (Exception e){
            Static.error(e);
        }
    }
    private void snackBar(String text){
        try {
            if (fragment_schedule_lessons == null) throw new NullPointerException("ScheduleLessonsTabFragment.fragment_schedule_lessons cannot be null");
            View content_container = fragment_schedule_lessons.findViewById(R.id.container_schedule_lessons);
            if (content_container != null) {
                Snackbar snackbar = Snackbar.make(content_container, text, Snackbar.LENGTH_SHORT);
                snackbar.getView().setBackgroundColor(Static.colorBackgroundSnackBar);
                snackbar.show();
            }
        } catch (Exception e){
            Static.error(e);
        }
    }

}
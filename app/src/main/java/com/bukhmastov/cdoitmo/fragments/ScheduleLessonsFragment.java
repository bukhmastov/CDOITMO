package com.bukhmastov.cdoitmo.fragments;

import android.app.Activity;
import android.content.Context;
import android.content.Intent;
import android.os.Bundle;
import android.support.design.widget.TabLayout;
import android.support.v4.view.ViewPager;
import android.util.SparseIntArray;
import android.view.InflateException;
import android.view.LayoutInflater;
import android.view.MenuItem;
import android.view.View;
import android.view.ViewGroup;
import android.widget.AdapterView;
import android.widget.ListView;
import android.widget.TextView;

import com.bukhmastov.cdoitmo.R;
import com.bukhmastov.cdoitmo.activities.MainActivity;
import com.bukhmastov.cdoitmo.activities.ScheduleLessonsSearchActivity;
import com.bukhmastov.cdoitmo.adapters.PagerAdapter;
import com.bukhmastov.cdoitmo.adapters.TeacherPickerListView;
import com.bukhmastov.cdoitmo.converters.ScheduleLessonsAdditionalConverter;
import com.bukhmastov.cdoitmo.firebase.FirebaseAnalyticsProvider;
import com.bukhmastov.cdoitmo.network.IfmoRestClient;
import com.bukhmastov.cdoitmo.objects.ScheduleLessons;
import com.bukhmastov.cdoitmo.utils.Log;
import com.bukhmastov.cdoitmo.utils.Static;
import com.bukhmastov.cdoitmo.utils.Storage;
import com.loopj.android.http.RequestHandle;

import org.json.JSONArray;
import org.json.JSONObject;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.Objects;

public class ScheduleLessonsFragment extends ConnectedFragment implements ScheduleLessons.response, ViewPager.OnPageChangeListener {

    private static final String TAG = "SLFragment";
    public static ScheduleLessons scheduleLessons;
    private boolean loaded = false;
    public static RequestHandle fragmentRequestHandle = null;
    public static String query = null;
    public static JSONObject schedule;
    public static boolean schedule_cached = false;
    public static SparseIntArray scroll = new SparseIntArray();
    public static int tabSelected = -1;
    public static boolean reScheduleRequired = false;

    @Override
    public void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        Log.v(TAG, "Fragment created");
        FirebaseAnalyticsProvider.logCurrentScreen(activity, this);
        scheduleLessons = new ScheduleLessons(getContext());
        scheduleLessons.setHandler(this);
    }

    @Override
    public View onCreateView(LayoutInflater inflater, ViewGroup container, Bundle savedInstanceState) {
        return inflater.inflate(R.layout.fragment_schedule_lessons, container, false);
    }

    @Override
    public void onResume() {
        super.onResume();
        Log.v(TAG, "resumed");
        FirebaseAnalyticsProvider.setCurrentScreen(activity, this);
        try {
            if (MainActivity.menu != null && !Static.OFFLINE_MODE) {
                MenuItem action_schedule_lessons_search = MainActivity.menu.findItem(R.id.action_schedule_lessons_search);
                if (action_schedule_lessons_search != null && !action_schedule_lessons_search.isVisible()) {
                    action_schedule_lessons_search.setVisible(true);
                    action_schedule_lessons_search.setOnMenuItemClickListener(new MenuItem.OnMenuItemClickListener() {
                        @Override
                        public boolean onMenuItemClick(MenuItem item) {
                            Log.v(TAG, "action_schedule_lessons_search clicked");
                            startActivity(new Intent(getContext(), ScheduleLessonsSearchActivity.class));
                            return false;
                        }
                    });
                }
            }
        } catch (Exception e){
            Static.error(e);
        }
        if (!loaded) {
            loaded = true;
            String scope = null;
            Activity activity = getActivity();
            if (activity != null) {
                Intent intent = activity.getIntent();
                if (intent != null) {
                    String action_extra = intent.getStringExtra("action_extra");
                    if (action_extra != null) {
                        intent.removeExtra("action_extra");
                        scope = action_extra;
                    }
                }
            }
            if (scope == null) {
                scope = scheduleLessons.getDefault();
            }
            Log.v(TAG, "search(" + scope + ")");
            search(scope);
        }
        if (reScheduleRequired) {
            reScheduleRequired = false;
            reSchedule(activity);
        }
    }

    @Override
    public void onPause() {
        super.onPause();
        Log.v(TAG, "paused");
        ScheduleLessonsFragment.scroll.clear();
        ScheduleLessonsFragment.tabSelected = -1;
        if (fragmentRequestHandle != null) {
            loaded = false;
            fragmentRequestHandle.cancel(true);
        }
    }

    @Override
    public void onDestroy() {
        super.onDestroy();
        Log.v(TAG, "Fragment destroyed");
        try {
            activity.findViewById(R.id.fixed_tabs).setVisibility(View.GONE);
            if (MainActivity.menu != null) {
                MenuItem action_schedule_lessons_search = MainActivity.menu.findItem(R.id.action_schedule_lessons_search);
                if (action_schedule_lessons_search != null && action_schedule_lessons_search.isVisible()) {
                    Log.v(TAG, "Hiding action_schedule_lessons_search");
                    action_schedule_lessons_search.setVisible(false);
                    action_schedule_lessons_search.setOnMenuItemClickListener(null);
                }
            }
        } catch (Exception e){
            Static.error(e);
        }
    }

    @Override
    public void onProgress(int state){
        Log.v(TAG, "progress " + state);
        try {
            activity.findViewById(R.id.fixed_tabs).setVisibility(View.GONE);
            draw(R.layout.state_loading);
            TextView loading_message = (TextView) activity.findViewById(R.id.loading_message);
            if (loading_message != null) {
                switch (state) {
                    case IfmoRestClient.STATE_HANDLING: loading_message.setText(R.string.loading); break;
                }
            }
        } catch (Exception e) {
            Static.error(e);
        }
    }

    @Override
    public void onNewHandle(RequestHandle requestHandle) {
        fragmentRequestHandle = requestHandle;
    }

    @Override
    public void onFailure(int state){
        Log.v(TAG, "failure " + state);
        try {
            switch (state) {
                case IfmoRestClient.FAILED_OFFLINE:
                case ScheduleLessons.FAILED_OFFLINE:
                    draw(R.layout.state_offline);
                    View offline_reload = activity.findViewById(R.id.offline_reload);
                    if (offline_reload != null) {
                        offline_reload.setOnClickListener(new View.OnClickListener() {
                            @Override
                            public void onClick(View v) {
                                search(query);
                            }
                        });
                    }
                    break;
                case IfmoRestClient.FAILED_TRY_AGAIN:
                case ScheduleLessons.FAILED_LOAD:
                    draw(R.layout.state_try_again);
                    View try_again_reload = activity.findViewById(R.id.try_again_reload);
                    if (try_again_reload != null) {
                        try_again_reload.setOnClickListener(new View.OnClickListener() {
                            @Override
                            public void onClick(View v) {
                                search(query);
                            }
                        });
                    }
                    break;
            }
        } catch (Exception e){
            Static.error(e);
        }
    }

    @Override
    public void onSuccess(JSONObject json){
        Log.v(TAG, "success | json=" + (json == null ? "null" : "notnull"));
        try {
            if (json == null) throw new NullPointerException("json cannot be null");
            schedule = json;
            activity.findViewById(R.id.fixed_tabs).setVisibility(View.GONE);
            if (Objects.equals(json.getString("type"), "teacher_picker")) {
                schedule_cached = false;
                JSONArray teachers = json.getJSONArray("list");
                if (teachers.length() > 0) {
                    if (teachers.length() == 1) {
                        search(teachers.getJSONObject(0).getString("pid"));
                        return;
                    }
                    draw(R.layout.layout_schedule_lessons_teacher_picker);
                    TextView teacher_picker_header = (TextView) activity.findViewById(R.id.teacher_picker_header);
                    ListView teacher_picker_list_view = (ListView) activity.findViewById(R.id.teacher_picker_list_view);
                    if (teacher_picker_header != null) teacher_picker_header.setText(R.string.choose_teacher);
                    if (teacher_picker_list_view != null) {
                        final ArrayList<HashMap<String, String>> teachersMap = new ArrayList<>();
                        for (int i = 0; i < teachers.length(); i++) {
                            JSONObject teacher = teachers.getJSONObject(i);
                            HashMap<String, String> teacherMap = new HashMap<>();
                            teacherMap.put("pid", String.valueOf(teacher.getInt("pid")));
                            teacherMap.put("person", teacher.getString("person"));
                            teacherMap.put("post", teacher.getString("post"));
                            teachersMap.add(teacherMap);
                        }
                        teacher_picker_list_view.setAdapter(new TeacherPickerListView(activity, teachersMap));
                        teacher_picker_list_view.setOnItemClickListener(new AdapterView.OnItemClickListener() {
                            public void onItemClick(AdapterView<?> parent, View view, int position, long id) {
                                Log.v(TAG, "teacher_picker_list_view clicked | scope=" + teachersMap.get(position).get("pid"));
                                search(teachersMap.get(position).get("pid"));
                            }
                        });
                    }
                } else {
                    notFound();
                }
            } else {
                schedule_cached = !Objects.equals(scheduleLessons.getCache(schedule.getString("cache_token")), "");
                if (schedule.getJSONArray("schedule").length() > 0) {
                    TabLayout schedule_tabs = (TabLayout) activity.findViewById(R.id.fixed_tabs);
                    schedule_tabs.setVisibility(View.VISIBLE);
                    draw(R.layout.layout_schedule_lessons_tabs);
                    ViewPager schedule_view = (ViewPager) activity.findViewById(R.id.schedule_pager);
                    if (schedule_view != null) {
                        schedule_view.setAdapter(new PagerAdapter(getFragmentManager(), getContext(), activity));
                        schedule_view.addOnPageChangeListener(this);
                        schedule_tabs.setupWithViewPager(schedule_view);
                    }
                    TabLayout.Tab tab;
                    if (ScheduleLessonsFragment.tabSelected == -1) {
                        int pref = Integer.parseInt(Storage.pref.get(getContext(), "pref_schedule_lessons_week", "-1"));
                        if (pref == -1) {
                            int week = Static.getWeek(getContext());
                            tab = schedule_tabs.getTabAt(week >= 0 ? (week % 2) + 1 : 0);
                        } else {
                            tab = schedule_tabs.getTabAt(pref);
                        }
                    } else {
                        try {
                            tab = schedule_tabs.getTabAt(ScheduleLessonsFragment.tabSelected);
                        } catch (Exception e) {
                            tab = null;
                        }
                    }
                    if (tab != null) tab.select();
                } else {
                    notFound();
                }
            }
        } catch (Exception e) {
            Static.error(e);
            onFailure(ScheduleLessons.FAILED_LOAD);
        }
    }

    @Override
    public void onPageSelected(int position) {
        ScheduleLessonsFragment.tabSelected = position;
    }

    @Override
    public void onPageScrolled(int position, float positionOffset, int positionOffsetPixels) {}

    @Override
    public void onPageScrollStateChanged(int state) {}

    public static void searchAndClear(String query){
        Log.v(TAG, "searchAndClear | query=" + query);
        ScheduleLessonsFragment.scroll.clear();
        ScheduleLessonsFragment.tabSelected = -1;
        search(query);
    }
    public static void search(String query){
        Log.v(TAG, "search | query=" + query);
        if (ScheduleLessonsFragment.scheduleLessons != null) {
            ScheduleLessonsFragment.query = query;
            ScheduleLessonsFragment.scheduleLessons.search(query);
        }
    }
    private void notFound(){
        Log.v(TAG, "notFound");
        ViewGroup container_schedule = ((ViewGroup) activity.findViewById(R.id.container_schedule));
        if (container_schedule != null) {
            container_schedule.removeAllViews();
            View view = inflate(R.layout.nothing_to_display);
            ((TextView) view.findViewById(R.id.ntd_text)).setText(getString(R.string.on_demand) + " \"" + query + "\" " + getString(R.string.schedule_not_found_2));
            container_schedule.addView(view);
        }
    }
    public static void reSchedule(final Context context){
        Log.v(TAG, "reSchedule");
        new ScheduleLessonsAdditionalConverter(context, new ScheduleLessonsAdditionalConverter.response() {
            @Override
            public void finish(JSONObject json) {
                Log.v(TAG, "reScheduled");
                schedule = json;
                try {
                    Static.snackBar(((Activity) context).findViewById(android.R.id.content), context.getString(R.string.schedule_refresh), context.getString(R.string.update), new View.OnClickListener() {
                        @Override
                        public void onClick(View v) {
                            search(ScheduleLessonsFragment.query);
                        }
                    });
                } catch (Exception e) {
                    Static.error(e);
                }
            }
        }).execute(schedule);
    }

    private void draw(int layoutId){
        try {
            ViewGroup vg = ((ViewGroup) activity.findViewById(R.id.container_schedule));
            if (vg != null) {
                vg.removeAllViews();
                vg.addView(inflate(layoutId), 0, new ViewGroup.LayoutParams(ViewGroup.LayoutParams.MATCH_PARENT, ViewGroup.LayoutParams.MATCH_PARENT));
            }
        } catch (Exception e){
            Static.error(e);
        }
    }
    private View inflate(int layoutId) throws InflateException {
        return ((LayoutInflater) activity.getSystemService(Context.LAYOUT_INFLATER_SERVICE)).inflate(layoutId, null);
    }

}
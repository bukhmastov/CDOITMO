package com.bukhmastov.cdoitmo.fragments;

import android.content.Context;
import android.content.Intent;
import android.os.Bundle;
import android.support.v4.widget.SwipeRefreshLayout;
import android.view.InflateException;
import android.view.LayoutInflater;
import android.view.MenuItem;
import android.view.View;
import android.view.ViewGroup;
import android.widget.AdapterView;
import android.widget.FrameLayout;
import android.widget.ImageView;
import android.widget.LinearLayout;
import android.widget.ListView;
import android.widget.TextView;

import com.bukhmastov.cdoitmo.R;
import com.bukhmastov.cdoitmo.activities.LoginActivity;
import com.bukhmastov.cdoitmo.activities.MainActivity;
import com.bukhmastov.cdoitmo.activities.ScheduleExamsSearchActivity;
import com.bukhmastov.cdoitmo.adapters.TeacherPickerListView;
import com.bukhmastov.cdoitmo.builders.ScheduleExamsBuilder;
import com.bukhmastov.cdoitmo.network.DeIfmoClient;
import com.bukhmastov.cdoitmo.objects.ScheduleExams;
import com.bukhmastov.cdoitmo.utils.Log;
import com.bukhmastov.cdoitmo.utils.Static;
import com.loopj.android.http.RequestHandle;

import org.json.JSONArray;
import org.json.JSONObject;

import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.Calendar;
import java.util.Date;
import java.util.HashMap;
import java.util.Locale;
import java.util.Objects;

public class ScheduleExamsFragment extends ConnectedFragment implements ScheduleExams.response {

    private static final String TAG = "SEFragment";
    public static ScheduleExams scheduleExams;
    private boolean loaded = false;
    public static RequestHandle fragmentRequestHandle = null;
    public static String query = null;
    public static JSONObject schedule;
    public static boolean schedule_cached = false;

    @Override
    public void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        Log.v(TAG, "Fragment created");
        scheduleExams = new ScheduleExams(getContext());
        scheduleExams.setHandler(this);
    }

    @Override
    public View onCreateView(LayoutInflater inflater, ViewGroup container, Bundle savedInstanceState) {
        return inflater.inflate(R.layout.fragment_schedule_exams, container, false);
    }

    @Override
    public void onResume() {
        super.onResume();
        Log.v(TAG, "resumed");
        try {
            if (MainActivity.menu != null && !Static.OFFLINE_MODE) {
                MenuItem action_schedule_exams_search = MainActivity.menu.findItem(R.id.action_schedule_exams_search);
                if (action_schedule_exams_search != null && !action_schedule_exams_search.isVisible()) {
                    Log.v(TAG, "Revealing action_schedule_exams_search");
                    action_schedule_exams_search.setVisible(true);
                    action_schedule_exams_search.setOnMenuItemClickListener(new MenuItem.OnMenuItemClickListener() {
                        @Override
                        public boolean onMenuItemClick(MenuItem item) {
                            Log.v(TAG, "action_schedule_exams_search clicked");
                            startActivity(new Intent(getContext(), ScheduleExamsSearchActivity.class));
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
            String scope;
            String action_extra = getActivity().getIntent().getStringExtra("action_extra");
            if (action_extra != null) {
                getActivity().getIntent().removeExtra("action_extra");
                scope = action_extra;
            } else {
                scope = scheduleExams.getDefault();
            }
            Log.v(TAG, "scheduleExams.search(" + scope + ")");
            scheduleExams.search(scope);
        }
    }

    @Override
    public void onPause() {
        super.onPause();
        Log.v(TAG, "paused");
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
            getActivity().findViewById(R.id.schedule_tabs).setVisibility(View.GONE);
            if (MainActivity.menu != null) {
                MenuItem action_schedule_exams_search = MainActivity.menu.findItem(R.id.action_schedule_exams_search);
                if (action_schedule_exams_search != null && action_schedule_exams_search.isVisible()) {
                    Log.v(TAG, "Hiding action_schedule_exams_search");
                    action_schedule_exams_search.setVisible(false);
                    action_schedule_exams_search.setOnMenuItemClickListener(null);
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
            draw(R.layout.state_loading);
            TextView loading_message = (TextView) getActivity().findViewById(R.id.loading_message);
            if (loading_message != null) {
                switch (state) {
                    case DeIfmoClient.STATE_HANDLING: loading_message.setText(R.string.loading); break;
                    case DeIfmoClient.STATE_AUTHORIZATION: loading_message.setText(R.string.authorization); break;
                    case DeIfmoClient.STATE_AUTHORIZED: loading_message.setText(R.string.authorized); break;
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
                case DeIfmoClient.FAILED_OFFLINE:
                case ScheduleExams.FAILED_OFFLINE:
                    draw(R.layout.state_offline);
                    View offline_reload = getActivity().findViewById(R.id.offline_reload);
                    if (offline_reload != null) {
                        offline_reload.setOnClickListener(new View.OnClickListener() {
                            @Override
                            public void onClick(View v) {
                                scheduleExams.search(query);
                            }
                        });
                    }
                    break;
                case DeIfmoClient.FAILED_TRY_AGAIN:
                case DeIfmoClient.FAILED_AUTH_TRY_AGAIN:
                case ScheduleExams.FAILED_LOAD:
                    draw(R.layout.state_try_again);
                    if (state == DeIfmoClient.FAILED_AUTH_TRY_AGAIN){
                        TextView try_again_message = (TextView) getActivity().findViewById(R.id.try_again_message);
                        if (try_again_message != null) try_again_message.setText(R.string.auth_failed);
                    }
                    View try_again_reload = getActivity().findViewById(R.id.try_again_reload);
                    if (try_again_reload != null) {
                        try_again_reload.setOnClickListener(new View.OnClickListener() {
                            @Override
                            public void onClick(View v) {
                                scheduleExams.search(query);
                            }
                        });
                    }
                    break;
                case DeIfmoClient.FAILED_AUTH_CREDENTIALS_REQUIRED: gotoLogin(LoginActivity.SIGNAL_CREDENTIALS_REQUIRED); break;
                case DeIfmoClient.FAILED_AUTH_CREDENTIALS_FAILED: gotoLogin(LoginActivity.SIGNAL_CREDENTIALS_FAILED); break;
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
            if (Objects.equals(json.getString("type"), "teacher_picker")) {
                schedule_cached = false;
                JSONArray teachers = json.getJSONArray("teachers");
                if (teachers.length() > 0) {
                    draw(R.layout.layout_schedule_lessons_teacher_picker);
                    TextView teacher_picker_header = (TextView) getActivity().findViewById(R.id.teacher_picker_header);
                    ListView teacher_picker_list_view = (ListView) getActivity().findViewById(R.id.teacher_picker_list_view);
                    if (teacher_picker_header != null) teacher_picker_header.setText(R.string.choose_teacher);
                    if (teacher_picker_list_view != null) {
                        final ArrayList<HashMap<String, String>> teachersMap = new ArrayList<>();
                        for (int i = 0; i < teachers.length(); i++) {
                            JSONObject teacher = teachers.getJSONObject(i);
                            HashMap<String, String> teacherMap = new HashMap<>();
                            teacherMap.put("pid", "teacher" + teacher.getString("id"));
                            teacherMap.put("person", teacher.getString("name"));
                            teacherMap.put("post", "");
                            teachersMap.add(teacherMap);
                        }
                        teacher_picker_list_view.setAdapter(new TeacherPickerListView(getActivity(), teachersMap));
                        teacher_picker_list_view.setOnItemClickListener(new AdapterView.OnItemClickListener() {
                            public void onItemClick(AdapterView<?> parent, View view, int position, long id) {
                                HashMap<String, String> teacherMap = teachersMap.get(position);
                                Log.v(TAG, "teacher_picker_list_view clicked | scope=" + teacherMap.get("pid"));
                                scheduleExams.search(teacherMap.get("pid"));
                            }
                        });
                    }
                } else {
                    notFound();
                }
            } else {
                schedule_cached = !Objects.equals(scheduleExams.getCache(schedule.getString("cache_token")), "");
                if (schedule.getJSONArray("schedule").length() > 0) {
                    draw(R.layout.layout_schedule_exams);
                    TextView schedule_exams_header = (TextView) getActivity().findViewById(R.id.schedule_exams_header);
                    switch (schedule.getString("type")){
                        case "group": if (schedule_exams_header != null) schedule_exams_header.setText("Расписание группы" + " " + schedule.getString("scope")); break;
                        case "teacher": if (schedule_exams_header != null) schedule_exams_header.setText("Расписание преподавателя" + " " + schedule.getString("scope")); break;
                        default:
                            String exception = "Wrong ScheduleExamsFragment.schedule.TYPE value: " + schedule.getString("type");
                            Log.wtf(TAG, exception);
                            throw new Exception(exception);
                    }
                    TextView schedule_exams_week = (TextView) getActivity().findViewById(R.id.schedule_exams_week);
                    if (schedule_exams_week != null) {
                        if (Static.week >= 0) {
                            schedule_exams_week.setText(Static.week + " " + getString(R.string.school_week));
                        } else {
                            schedule_exams_week.setText(new SimpleDateFormat("dd.MM.yyyy", Locale.ROOT).format(new Date(Calendar.getInstance().getTimeInMillis())));
                        }
                    }
                    FrameLayout schedule_exams_cache = (FrameLayout) getActivity().findViewById(R.id.schedule_exams_cache);
                    if (schedule_exams_cache != null) {
                        ImageView cacheImage = new ImageView(getContext());
                        cacheImage.setImageDrawable(getActivity().getResources().getDrawable(ScheduleExamsFragment.schedule_cached ? R.drawable.ic_cached : R.drawable.ic_cache, getActivity().getTheme()));
                        cacheImage.setLayoutParams(new LinearLayout.LayoutParams(LinearLayout.LayoutParams.MATCH_PARENT, LinearLayout.LayoutParams.MATCH_PARENT));
                        int padding = (int) (getActivity().getResources().getDisplayMetrics().density * 4);
                        cacheImage.setPadding(padding, padding, padding, padding);
                        schedule_exams_cache.addView(cacheImage);
                        schedule_exams_cache.setOnClickListener(new View.OnClickListener() {
                            @Override
                            public void onClick(View v) {
                                Log.v(TAG, "schedule_exams_cache clicked");
                                if (ScheduleExamsFragment.scheduleExams != null) {
                                    Boolean result = ScheduleExamsFragment.scheduleExams.toggleCache();
                                    if (result == null) {
                                        Log.w(TAG, "failed to toggle cache");
                                        Static.snackBar(getActivity(), getString(R.string.cache_failed));
                                    } else {
                                        Static.snackBar(getActivity(), result ? getString(R.string.cache_true) : getString(R.string.cache_false));
                                        FrameLayout schedule_exams_cache = (FrameLayout) getActivity().findViewById(R.id.schedule_exams_cache);
                                        if (schedule_exams_cache != null) {
                                            ImageView cacheImage = new ImageView(getContext());
                                            cacheImage.setImageDrawable(getActivity().getResources().getDrawable(result ? R.drawable.ic_cached : R.drawable.ic_cache, getActivity().getTheme()));
                                            cacheImage.setLayoutParams(new LinearLayout.LayoutParams(LinearLayout.LayoutParams.MATCH_PARENT, LinearLayout.LayoutParams.MATCH_PARENT));
                                            int padding = (int) (getActivity().getResources().getDisplayMetrics().density * 4);
                                            cacheImage.setPadding(padding, padding, padding, padding);
                                            schedule_exams_cache.removeAllViews();
                                            schedule_exams_cache.addView(cacheImage);
                                        }
                                    }
                                } else {
                                    Log.v(TAG, "schedule_exams_cache clicked | ScheduleExamsFragment.scheduleExams is null");
                                }
                            }
                        });
                    }
                    // работаем со свайпом
                    SwipeRefreshLayout mSwipeRefreshLayout = (SwipeRefreshLayout) getActivity().findViewById(R.id.swipe_schedule_exams);
                    if (mSwipeRefreshLayout != null) {
                        mSwipeRefreshLayout.setColorSchemeColors(Static.colorAccent);
                        mSwipeRefreshLayout.setProgressBackgroundColorSchemeColor(Static.colorBackgroundRefresh);
                        mSwipeRefreshLayout.setOnRefreshListener(scheduleExams);
                    }
                    // отображаем расписание
                    final ViewGroup linearLayout = (ViewGroup) getActivity().findViewById(R.id.schedule_exams_content);
                    (new ScheduleExamsBuilder(getActivity(), new ScheduleExamsBuilder.response(){
                        public void state(final int state, final View layout){
                            try {
                                getActivity().runOnUiThread(new Runnable() {
                                    @Override
                                    public void run() {
                                        if (linearLayout != null) {
                                            linearLayout.removeAllViews();
                                            if (state == ScheduleExamsBuilder.STATE_DONE || state == ScheduleExamsBuilder.STATE_LOADING) {
                                                linearLayout.addView(layout);
                                            } else if (state == ScheduleExamsBuilder.STATE_FAILED) {
                                                onFailure(ScheduleExams.FAILED_LOAD);
                                            }
                                        }
                                    }
                                });
                            } catch (NullPointerException e){
                                Static.error(e);
                                onFailure(ScheduleExams.FAILED_LOAD);
                            }
                        }
                    })).start();
                } else {
                    notFound();
                }
            }
        } catch (Exception e) {
            Static.error(e);
            onFailure(ScheduleExams.FAILED_LOAD);
        }
    }

    private void notFound(){
        Log.v(TAG, "notFound");
        ViewGroup container_schedule = ((ViewGroup) getActivity().findViewById(R.id.container_schedule_exams));
        if (container_schedule != null) {
            container_schedule.removeAllViews();
            View view = inflate(R.layout.nothing_to_display);
            ((TextView) view.findViewById(R.id.ntd_text)).setText(getString(R.string.on_demand) + " \"" + query + "\" " + getString(R.string.schedule_not_found_2));
            container_schedule.addView(view);
        }
    }
    public void gotoLogin(int state){
        Log.v(TAG, "gotoLogin | state=" + state);
        Intent intent = new Intent(getContext(), LoginActivity.class);
        intent.putExtra("state", state);
        startActivity(intent);
    }

    private void draw(int layoutId){
        try {
            ViewGroup vg = ((ViewGroup) getActivity().findViewById(R.id.container_schedule_exams));
            if (vg != null) {
                vg.removeAllViews();
                vg.addView(inflate(layoutId), 0, new ViewGroup.LayoutParams(ViewGroup.LayoutParams.MATCH_PARENT, ViewGroup.LayoutParams.MATCH_PARENT));
            }
        } catch (Exception e){
            Static.error(e);
        }
    }
    private View inflate(int layoutId) throws InflateException {
        return ((LayoutInflater) getActivity().getSystemService(Context.LAYOUT_INFLATER_SERVICE)).inflate(layoutId, null);
    }

}
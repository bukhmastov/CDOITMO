package com.bukhmastov.cdoitmo.fragments;

import android.app.Activity;
import android.content.Context;
import android.content.Intent;
import android.os.Bundle;
import android.support.v4.widget.SwipeRefreshLayout;
import android.support.v7.widget.PopupMenu;
import android.view.InflateException;
import android.view.LayoutInflater;
import android.view.Menu;
import android.view.MenuItem;
import android.view.View;
import android.view.ViewGroup;
import android.widget.AdapterView;
import android.widget.ListView;
import android.widget.TextView;

import com.bukhmastov.cdoitmo.R;
import com.bukhmastov.cdoitmo.activities.ConnectedActivity;
import com.bukhmastov.cdoitmo.activities.MainActivity;
import com.bukhmastov.cdoitmo.activities.ScheduleExamsSearchActivity;
import com.bukhmastov.cdoitmo.adapters.TeacherPickerListView;
import com.bukhmastov.cdoitmo.builders.ScheduleExamsBuilder;
import com.bukhmastov.cdoitmo.firebase.FirebaseAnalyticsProvider;
import com.bukhmastov.cdoitmo.fragments.settings.SettingsScheduleExamsFragment;
import com.bukhmastov.cdoitmo.network.IfmoClient;
import com.bukhmastov.cdoitmo.network.models.Client;
import com.bukhmastov.cdoitmo.objects.ScheduleExams;
import com.bukhmastov.cdoitmo.utils.Log;
import com.bukhmastov.cdoitmo.utils.Static;

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
    public static Client.Request requestHandle = null;
    public static String query = null;
    public static JSONObject schedule;
    public static boolean schedule_cached = false;

    @Override
    public void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        Log.v(TAG, "Fragment created");
        FirebaseAnalyticsProvider.logCurrentScreen(activity, this);
        scheduleExams = new ScheduleExams(activity);
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
        FirebaseAnalyticsProvider.setCurrentScreen(activity, this);
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
                            startActivity(new Intent(activity, ScheduleExamsSearchActivity.class));
                            return false;
                        }
                    });
                }
            }
        } catch (Exception e){
            Static.error(e);
        }
        Static.T.runThread(new Runnable() {
            @Override
            public void run() {
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
                        scope = scheduleExams.getDefault();
                    }
                    Log.v(TAG, "scheduleExams.search(" + scope + ")");
                    scheduleExams.search(scope);
                }
            }
        });
    }

    @Override
    public void onPause() {
        super.onPause();
        Log.v(TAG, "paused");
        if (requestHandle != null && requestHandle.cancel()) {
            loaded = false;
        }
    }

    @Override
    public void onDestroy() {
        super.onDestroy();
        Log.v(TAG, "Fragment destroyed");
        try {
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
    public void onProgress(final int state) {
        Static.T.runOnUiThread(new Runnable() {
            @Override
            public void run() {
                Log.v(TAG, "progress " + state);
                try {
                    draw(R.layout.state_loading);
                    TextView loading_message = activity.findViewById(R.id.loading_message);
                    if (loading_message != null) {
                        switch (state) {
                            case IfmoClient.STATE_HANDLING: loading_message.setText(R.string.loading); break;
                        }
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

    @Override
    public void onFailure(final int state) {
        Static.T.runOnUiThread(new Runnable() {
            @Override
            public void run() {
                Log.v(TAG, "failure " + state);
                try {
                    switch (state) {
                        case IfmoClient.FAILED_OFFLINE:
                        case ScheduleExams.FAILED_OFFLINE:
                            draw(R.layout.state_offline);
                            View offline_reload = activity.findViewById(R.id.offline_reload);
                            if (offline_reload != null) {
                                offline_reload.setOnClickListener(new View.OnClickListener() {
                                    @Override
                                    public void onClick(View v) {
                                        scheduleExams.search(query);
                                    }
                                });
                            }
                            break;
                        case IfmoClient.FAILED_TRY_AGAIN:
                        case IfmoClient.FAILED_SERVER_ERROR:
                        case ScheduleExams.FAILED_LOAD:
                            draw(R.layout.state_try_again);
                            View try_again_reload = activity.findViewById(R.id.try_again_reload);
                            if (state == IfmoClient.FAILED_SERVER_ERROR) {
                                TextView try_again_message = activity.findViewById(R.id.try_again_message);
                                if (try_again_message != null) {
                                    try_again_message.setText(IfmoClient.getFailureMessage(activity, -1));
                                }
                            }
                            if (try_again_reload != null) {
                                try_again_reload.setOnClickListener(new View.OnClickListener() {
                                    @Override
                                    public void onClick(View v) {
                                        scheduleExams.search(query);
                                    }
                                });
                            }
                            break;
                        case ScheduleExams.FAILED_EMPTY_QUERY:
                            draw(R.layout.schedule_empty_query);
                            TextView seq_text = activity.findViewById(R.id.seq_text);
                            if (seq_text != null) {
                                seq_text.setText(activity.getString(R.string.settings) + " > " + activity.getString(R.string.extended_prefs) + " > " + activity.getString(R.string.schedule_exams) + " > " + activity.getString(R.string.default_schedule));
                            }
                            break;
                    }
                } catch (Exception e){
                    Static.error(e);
                }
            }
        });
    }

    @Override
    public void onSuccess(final JSONObject json) {
        Static.T.runThread(new Runnable() {
            @Override
            public void run() {
                Log.v(TAG, "success | json=" + (json == null ? "null" : "notnull"));
                try {
                    if (json == null) throw new NullPointerException("json cannot be null");
                    schedule = json;
                    if (Objects.equals(json.getString("type"), "teacher_picker")) {
                        schedule_cached = false;
                        JSONArray teachers = json.getJSONArray("teachers");
                        if (teachers.length() > 0) {
                            final ArrayList<HashMap<String, String>> teachersMap = new ArrayList<>();
                            for (int i = 0; i < teachers.length(); i++) {
                                JSONObject teacher = teachers.getJSONObject(i);
                                HashMap<String, String> teacherMap = new HashMap<>();
                                teacherMap.put("pid", "teacher" + teacher.getString("id"));
                                teacherMap.put("person", teacher.getString("name"));
                                teacherMap.put("post", "");
                                teachersMap.add(teacherMap);
                            }
                            Static.T.runOnUiThread(new Runnable() {
                                @Override
                                public void run() {
                                    draw(R.layout.layout_schedule_lessons_teacher_picker);
                                    TextView teacher_picker_header = activity.findViewById(R.id.teacher_picker_header);
                                    ListView teacher_picker_list_view = activity.findViewById(R.id.teacher_picker_list_view);
                                    if (teacher_picker_header != null) {
                                        teacher_picker_header.setText(R.string.choose_teacher);
                                    }
                                    if (teacher_picker_list_view != null) {
                                        teacher_picker_list_view.setAdapter(new TeacherPickerListView(activity, teachersMap));
                                        teacher_picker_list_view.setOnItemClickListener(new AdapterView.OnItemClickListener() {
                                            public void onItemClick(final AdapterView<?> parent, final View view, final int position, final long id) {
                                                Static.T.runThread(new Runnable() {
                                                    @Override
                                                    public void run() {
                                                        HashMap<String, String> teacherMap = teachersMap.get(position);
                                                        Log.v(TAG, "teacher_picker_list_view clicked | scope=" + teacherMap.get("pid"));
                                                        scheduleExams.search(teacherMap.get("pid"));
                                                    }
                                                });
                                            }
                                        });
                                    }
                                }
                            });
                        } else {
                            notFound();
                        }
                    } else {
                        schedule_cached = !Objects.equals(scheduleExams.getCache(schedule.getString("cache_token")), "");
                        if (schedule.getJSONArray("schedule").length() > 0) {
                            final int week = Static.getWeek(activity);
                            Static.T.runOnUiThread(new Runnable() {
                                @Override
                                public void run() {
                                    try {
                                        draw(R.layout.layout_schedule_exams);
                                        TextView schedule_exams_header = activity.findViewById(R.id.schedule_exams_header);
                                        switch (schedule.getString("type")){
                                            case "group": if (schedule_exams_header != null) schedule_exams_header.setText("Расписание группы" + " " + schedule.getString("scope")); break;
                                            case "teacher": if (schedule_exams_header != null) schedule_exams_header.setText("Расписание преподавателя" + " " + schedule.getString("scope")); break;
                                            default:
                                                String exception = "Wrong ScheduleExamsFragment.schedule.TYPE value: " + schedule.getString("type");
                                                Log.wtf(TAG, exception);
                                                throw new Exception(exception);
                                        }
                                        TextView schedule_exams_week = activity.findViewById(R.id.schedule_exams_week);
                                        if (schedule_exams_week != null) {
                                            if (week >= 0) {
                                                schedule_exams_week.setText(week + " " + activity.getString(R.string.school_week));
                                            } else {
                                                schedule_exams_week.setText(new SimpleDateFormat("dd.MM.yyyy", Locale.ROOT).format(new Date(Calendar.getInstance().getTimeInMillis())));
                                            }
                                        }
                                        // меню расписания
                                        ViewGroup schedule_exams_menu = activity.findViewById(R.id.schedule_exams_menu);
                                        if (schedule_exams_menu != null) {
                                            schedule_exams_menu.setOnClickListener(new View.OnClickListener() {
                                                @Override
                                                public void onClick(View view) {
                                                    try {
                                                        final PopupMenu popup = new PopupMenu(activity, view);
                                                        final Menu menu = popup.getMenu();
                                                        popup.getMenuInflater().inflate(R.menu.schedule_exams, menu);
                                                        if (ScheduleExamsFragment.schedule_cached) {
                                                            menu.findItem(R.id.add_to_cache).setVisible(false);
                                                        } else {
                                                            menu.findItem(R.id.remove_from_cache).setVisible(false);
                                                        }
                                                        popup.setOnMenuItemClickListener(onScheduleMenuClickListener);
                                                        popup.show();
                                                    } catch (Exception e) {
                                                        Static.error(e);
                                                        Static.snackBar(activity, activity.getString(R.string.something_went_wrong));
                                                    }
                                                }
                                            });
                                        }
                                        // работаем со свайпом
                                        SwipeRefreshLayout swipe = activity.findViewById(R.id.swipe_schedule_exams);
                                        if (swipe != null) {
                                            swipe.setColorSchemeColors(Static.colorAccent);
                                            swipe.setProgressBackgroundColorSchemeColor(Static.colorBackgroundRefresh);
                                            swipe.setOnRefreshListener(scheduleExams);
                                        }
                                        // отображаем расписание
                                        final ViewGroup linearLayout = activity.findViewById(R.id.schedule_exams_content);
                                        Static.T.runThread(new ScheduleExamsBuilder(activity, new ScheduleExamsBuilder.response(){
                                            public void state(final int state, final View layout){
                                                try {
                                                    Static.T.runOnUiThread(new Runnable() {
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
                                        }));
                                    } catch (Exception e) {
                                        Static.error(e);
                                        onFailure(ScheduleExams.FAILED_LOAD);
                                    }
                                }
                            });
                        } else {
                            notFound();
                        }
                    }
                } catch (Exception e) {
                    Static.error(e);
                    onFailure(ScheduleExams.FAILED_LOAD);
                }
            }
        });
    }

    private void notFound() {
        Static.T.runOnUiThread(new Runnable() {
            @Override
            public void run() {
                Log.v(TAG, "notFound");
                ViewGroup container_schedule = activity.findViewById(R.id.container_schedule_exams);
                if (container_schedule != null) {
                    container_schedule.removeAllViews();
                    View view = inflate(R.layout.nothing_to_display);
                    ((TextView) view.findViewById(R.id.ntd_text)).setText(activity.getString(R.string.on_demand) + " \"" + query + "\" " + activity.getString(R.string.schedule_not_found_2));
                    container_schedule.addView(view);
                }
            }
        });
    }

    private void draw(final int layoutId){
        Static.T.runOnUiThread(new Runnable() {
            @Override
            public void run() {
                try {
                    ViewGroup vg = activity.findViewById(R.id.container_schedule_exams);
                    if (vg != null) {
                        vg.removeAllViews();
                        vg.addView(inflate(layoutId), 0, new ViewGroup.LayoutParams(ViewGroup.LayoutParams.MATCH_PARENT, ViewGroup.LayoutParams.MATCH_PARENT));
                    }
                } catch (Exception e){
                    Static.error(e);
                }
            }
        });
    }
    private View inflate(int layoutId) throws InflateException {
        return ((LayoutInflater) activity.getSystemService(Context.LAYOUT_INFLATER_SERVICE)).inflate(layoutId, null);
    }

    private final PopupMenu.OnMenuItemClickListener onScheduleMenuClickListener = new PopupMenu.OnMenuItemClickListener() {
        @Override
        public boolean onMenuItemClick(MenuItem item) {
            Log.v(TAG, "menu | popup item | clicked | " + item.getTitle().toString());
            switch (item.getItemId()) {
                case R.id.add_to_cache:
                case R.id.remove_from_cache: {
                    Static.T.runThread(new Runnable() {
                        @Override
                        public void run() {
                            if (ScheduleExamsFragment.scheduleExams != null) {
                                final Boolean result = ScheduleExamsFragment.scheduleExams.toggleCache();
                                if (result == null) {
                                    Log.w(TAG, "menu | popup item | cache | failed to toggle cache");
                                    Static.snackBar(activity, activity.getString(R.string.cache_failed));
                                } else {
                                    Static.snackBar(activity, result ? activity.getString(R.string.cache_true) : activity.getString(R.string.cache_false));
                                }
                            } else {
                                Log.v(TAG, "menu | popup item | cache | ScheduleExamsFragment.scheduleExams is null");
                            }
                        }
                    });
                    break;
                }
                case R.id.open_settings: {
                    activity.openActivityOrFragment(ConnectedActivity.TYPE.stackable, SettingsScheduleExamsFragment.class, null);
                    break;
                }
            }
            return false;
        }
    };
}

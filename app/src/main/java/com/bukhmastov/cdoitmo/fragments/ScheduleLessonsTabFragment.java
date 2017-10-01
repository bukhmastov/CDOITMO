package com.bukhmastov.cdoitmo.fragments;

import android.content.Context;
import android.content.DialogInterface;
import android.content.Intent;
import android.os.Build;
import android.os.Bundle;
import android.preference.PreferenceActivity;
import android.support.annotation.Nullable;
import android.support.v4.app.Fragment;
import android.support.v4.widget.SwipeRefreshLayout;
import android.support.v7.app.AlertDialog;
import android.support.v7.widget.PopupMenu;
import android.view.InflateException;
import android.view.LayoutInflater;
import android.view.Menu;
import android.view.MenuItem;
import android.view.View;
import android.view.ViewGroup;
import android.widget.ScrollView;
import android.widget.TextView;

import com.bukhmastov.cdoitmo.R;
import com.bukhmastov.cdoitmo.activities.ConnectedActivity;
import com.bukhmastov.cdoitmo.activities.SettingsActivity;
import com.bukhmastov.cdoitmo.builders.ScheduleLessonsBuilder;
import com.bukhmastov.cdoitmo.objects.ScheduleLessons;
import com.bukhmastov.cdoitmo.utils.Log;
import com.bukhmastov.cdoitmo.utils.Static;
import com.bukhmastov.cdoitmo.utils.Storage;

import org.json.JSONObject;

import java.text.SimpleDateFormat;
import java.util.Calendar;
import java.util.Date;
import java.util.Locale;

public class ScheduleLessonsTabFragment extends Fragment {

    private static final String TAG = "SLTabFragment";
    private static final int DEFAULT_TYPE = 2;
    private int TYPE = -1;
    private ConnectedActivity activity;
    private boolean displayed = false;
    private View container = null;

    public ScheduleLessonsTabFragment() {
        super();
    }

    @Override
    public void onCreate(@Nullable Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        try {
            activity = (ConnectedActivity) getActivity();
        } catch (Exception e) {
            Static.error(e);
            activity = null;
        }
        Bundle bundle = getArguments();
        if (bundle != null && bundle.containsKey("type")) {
            TYPE = bundle.getInt("type");
        } else {
            Log.w(TAG, "onCreate | UNDEFINED TYPE, going to use TYPE=" + DEFAULT_TYPE);
            TYPE = DEFAULT_TYPE;
        }
        Log.v(TAG, "Fragment created | TYPE=" + TYPE);
    }

    @Override
    public void onDestroy() {
        displayed = false;
        Log.v(TAG, "Fragment destroyed | TYPE=" + TYPE);
        super.onDestroy();
    }

    @Override
    public View onCreateView(LayoutInflater inflater, ViewGroup container, Bundle savedInstanceState) {
        Log.v(TAG, "onCreateView | TYPE=" + TYPE);
        this.container = inflater.inflate(R.layout.fragment_tab_schedule_lessons, container, false);
        return this.container;
    }

    @Override
    public void onResume() {
        super.onResume();
        Log.v(TAG, "resumed | TYPE=" + TYPE);
        if (!displayed) {
            check();
        }
    }

    @Override
    public void onPause() {
        super.onPause();
        Log.v(TAG, "paused | TYPE=" + TYPE);
    }

    private void check() {
        Static.T.runThread(new Runnable() {
            @Override
            public void run() {
                Log.v(TAG, "check | TYPE=" + TYPE);
                try {
                    if (ScheduleLessonsFragment.schedule != null) {
                        display(ScheduleLessonsFragment.schedule);
                    } else {
                        Log.v(TAG, "check | TYPE=" + TYPE + " | ScheduleLessonsFragment.schedule is null, going to get schedule manually");
                        ScheduleLessonsFragment.getSchedule(activity, new ScheduleLessonsFragment.getScheduleResponse() {
                            @Override
                            public void onReady(JSONObject json) {
                                display(json);
                            }
                        });
                    }
                } catch (Exception e){
                    Static.error(e);
                    failed();
                }
            }
        });
    }
    private void display(final JSONObject schedule) {
        Static.T.runThread(new Runnable() {
            @Override
            public void run() {
                Log.v(TAG, "display | TYPE=" + TYPE);
                try {
                    if (TYPE < 0) throw new Exception("ScheduleLessonsTabFragment.TYPE negative");
                    if (container == null) throw new NullPointerException("ScheduleLessonsTabFragment.container cannot be null");
                    if (schedule == null) throw new NullPointerException("schedule cannot be null");
                    final int week = Static.getWeek(activity);
                    Static.T.runOnUiThread(new Runnable() {
                        @Override
                        public void run() {
                            try {
                                TextView schedule_c_header = container.findViewById(R.id.schedule_lessons_header);
                                switch (schedule.getString("type")){
                                    case "group":
                                    case "room":
                                    case "teacher":
                                        if (schedule_c_header != null) {
                                            schedule_c_header.setText(schedule.getString("title") + " " + schedule.getString("label"));
                                        }
                                        break;
                                    default:
                                        String exception = "Wrong schedule.TYPE value: " + schedule.getString("type");
                                        Log.wtf(TAG, exception);
                                        throw new Exception(exception);
                                }
                                TextView schedule_lessons_all_week = container.findViewById(R.id.schedule_lessons_week);
                                if (schedule_lessons_all_week != null) {
                                    if (week >= 0) {
                                        schedule_lessons_all_week.setText(week + " " + activity.getString(R.string.school_week));
                                    } else {
                                        schedule_lessons_all_week.setText(new SimpleDateFormat("dd.MM.yyyy", Locale.ROOT).format(new Date(Calendar.getInstance().getTimeInMillis())));
                                    }
                                }
                                // меню расписания
                                ViewGroup schedule_lessons_menu = container.findViewById(R.id.schedule_lessons_menu);
                                if (schedule_lessons_menu != null) {
                                    schedule_lessons_menu.setOnClickListener(new View.OnClickListener() {
                                        @Override
                                        public void onClick(View view) {
                                            try {
                                                final PopupMenu popup = new PopupMenu(activity, view);
                                                final Menu menu = popup.getMenu();
                                                popup.getMenuInflater().inflate(R.menu.schedule_lessons, menu);
                                                if (ScheduleLessonsFragment.schedule_cached) {
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
                                SwipeRefreshLayout swipe = container.findViewById(R.id.swipe_schedule_lessons);
                                if (swipe != null) {
                                    swipe.setColorSchemeColors(Static.colorAccent);
                                    swipe.setProgressBackgroundColorSchemeColor(Static.colorBackgroundRefresh);
                                    swipe.setOnRefreshListener(ScheduleLessonsFragment.scheduleLessons);
                                }
                                // отображаем расписание
                                final ViewGroup schedule_lessons_content = container.findViewById(R.id.schedule_lessons_content);
                                Static.T.runThread(new ScheduleLessonsBuilder(activity, TYPE, new ScheduleLessonsBuilder.response(){
                                    public void state(final int state, final View layout) {
                                        try {
                                            Static.T.runOnUiThread(new Runnable() {
                                                @Override
                                                public void run() {
                                                    if (schedule_lessons_content != null) {
                                                        schedule_lessons_content.removeAllViews();
                                                        if (state == ScheduleLessonsBuilder.STATE_DONE) {
                                                            schedule_lessons_content.addView(layout);
                                                            displayed = true;
                                                            if (Storage.pref.get(activity, "pref_schedule_lessons_scroll_to_day", true)) {
                                                                final ScrollView scroll_schedule_lessons = container.findViewById(R.id.scroll_schedule_lessons);
                                                                scroll_schedule_lessons.post(new Runnable() {
                                                                    @Override
                                                                    public void run() {
                                                                        Integer scroll = ScheduleLessonsFragment.scroll.get(TYPE);
                                                                        if (scroll == 0) {
                                                                            View day;
                                                                            switch (Calendar.getInstance().get(Calendar.DAY_OF_WEEK)) {
                                                                                case Calendar.MONDAY:
                                                                                    day = container.findViewById(R.id.monday);
                                                                                    if (day != null) break;
                                                                                case Calendar.TUESDAY:
                                                                                    day = container.findViewById(R.id.tuesday);
                                                                                    if (day != null) break;
                                                                                case Calendar.WEDNESDAY:
                                                                                    day = container.findViewById(R.id.wednesday);
                                                                                    if (day != null) break;
                                                                                case Calendar.THURSDAY:
                                                                                    day = container.findViewById(R.id.thursday);
                                                                                    if (day != null) break;
                                                                                case Calendar.FRIDAY:
                                                                                    day = container.findViewById(R.id.friday);
                                                                                    if (day != null) break;
                                                                                case Calendar.SATURDAY:
                                                                                    day = container.findViewById(R.id.saturday);
                                                                                    if (day != null) break;
                                                                                case Calendar.SUNDAY:
                                                                                    day = container.findViewById(R.id.sunday);
                                                                                    if (day != null) break;
                                                                                default:
                                                                                    day = null;
                                                                                    break;
                                                                            }
                                                                            if (day != null) {
                                                                                int y = day.getTop();
                                                                                View header_schedule_lessons = container.findViewById(R.id.header_schedule_lessons);
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
                                }));
                            } catch (Exception e){
                                Static.error(e);
                                failed();
                            }
                        }
                    });
                } catch (Exception e){
                    Static.error(e);
                    failed();
                }
            }
        });
    }
    private void failed() {
        Static.T.runOnUiThread(new Runnable() {
            @Override
            public void run() {
                Log.v(TAG, "failed | TYPE=" + TYPE);
                try {
                    if (container == null) throw new NullPointerException("ScheduleLessonsTabFragment.container cannot be null");
                    draw(R.layout.state_try_again);
                    View try_again_reload = container.findViewById(R.id.try_again_reload);
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
        });
    }

    private void draw(final int layoutId) {
        Static.T.runOnUiThread(new Runnable() {
            @Override
            public void run() {
                try {
                    if (container == null) throw new NullPointerException("ScheduleLessonsTabFragment.container cannot be null");
                    ViewGroup vg = container.findViewById(R.id.container_schedule_lessons);
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

    private PopupMenu.OnMenuItemClickListener onScheduleMenuClickListener = new PopupMenu.OnMenuItemClickListener() {
        @Override
        public boolean onMenuItemClick(MenuItem item) {
            Log.v(TAG, "menu | popup item | clicked | " + item.getTitle().toString());
            switch (item.getItemId()) {
                case R.id.add_to_cache:
                case R.id.remove_from_cache: {
                    Static.T.runThread(new Runnable() {
                        @Override
                        public void run() {
                            if (ScheduleLessonsFragment.scheduleLessons != null) {
                                final Boolean result = ScheduleLessonsFragment.scheduleLessons.toggleCache();
                                if (result == null) {
                                    Log.w(TAG, "menu | popup item | cache | failed to toggle cache");
                                    Static.snackBar(activity, activity.getString(R.string.cache_failed));
                                } else {
                                    Static.snackBar(activity, result ? activity.getString(R.string.cache_true) : activity.getString(R.string.cache_false));
                                }
                            } else {
                                Log.v(TAG, "menu | popup item | cache | ScheduleLessonsFragment.scheduleLessons is null");
                            }
                        }
                    });
                    break;
                }
                case R.id.add_lesson: {
                    Static.T.runThread(new Runnable() {
                        @Override
                        public void run() {
                            try {
                                int day;
                                switch (Calendar.getInstance().get(Calendar.DAY_OF_WEEK)) {
                                    case Calendar.MONDAY: day = 0; break;
                                    case Calendar.TUESDAY: day = 1; break;
                                    case Calendar.WEDNESDAY: day = 2; break;
                                    case Calendar.THURSDAY: day = 3; break;
                                    case Calendar.FRIDAY: day = 4; break;
                                    case Calendar.SATURDAY: day = 5; break;
                                    case Calendar.SUNDAY: day = 6; break;
                                    default: day = 0; break;
                                }
                                ScheduleLessons.createLesson(activity, ScheduleLessonsFragment.schedule, day, TYPE);
                            } catch (Exception e) {
                                Static.error(e);
                                Static.snackBar(activity, activity.getString(R.string.something_went_wrong));
                            }
                        }
                    });
                    break;
                }
                case R.id.share_changes: {
                    Static.T.runThread(new Runnable() {
                        @Override
                        public void run() {
                            try {
                                if (ScheduleLessonsFragment.schedule != null && ScheduleLessonsFragment.schedule.has("cache_token")) {
                                    String token = ScheduleLessonsFragment.schedule.getString("cache_token");
                                    if (token != null && !token.isEmpty()) {
                                        String title = "";
                                        String query = "";
                                        if (ScheduleLessonsFragment.schedule.has("title")) {
                                            title += " " + ScheduleLessonsFragment.schedule.getString("title");
                                        }
                                        if (ScheduleLessonsFragment.schedule.has("label")) {
                                            title += " " + ScheduleLessonsFragment.schedule.getString("label");
                                        }
                                        if (ScheduleLessonsFragment.schedule.has("query")) {
                                            query = ScheduleLessonsFragment.schedule.getString("query");
                                        }
                                        title = title.trim();
                                        final Bundle extras = new Bundle();
                                        extras.putString("type", "share");
                                        extras.putString("query", query);
                                        extras.putString("title", title);
                                        extras.putString("token", token);
                                        Static.T.runOnUiThread(new Runnable() {
                                            @Override
                                            public void run() {
                                                activity.openActivityOrFragment(ScheduleLessonsShareFragment.class, extras);
                                            }
                                        });
                                    }
                                }
                            } catch (Exception e) {
                                Static.error(e);
                            }
                        }
                    });
                    break;
                }
                case R.id.remove_changes: {
                    Static.T.runThread(new Runnable() {
                        @Override
                        public void run() {
                            if (activity != null) {
                                new AlertDialog.Builder(activity)
                                        .setTitle(R.string.pref_schedule_lessons_clear_additional_title)
                                        .setMessage(R.string.pref_schedule_lessons_clear_direct_additional_warning)
                                        .setIcon(R.drawable.ic_warning)
                                        .setPositiveButton(R.string.proceed, new DialogInterface.OnClickListener() {
                                            @Override
                                            public void onClick(DialogInterface dialog, int which) {
                                                Static.T.runThread(new Runnable() {
                                                    @Override
                                                    public void run() {
                                                        Log.v(TAG, "menu | popup item | remove_changes | dialog accepted");
                                                        if (ScheduleLessonsFragment.scheduleLessons != null) {
                                                            if (ScheduleLessonsFragment.scheduleLessons.clearChanges()) {
                                                                ScheduleLessonsFragment.reSchedule(activity);
                                                            }
                                                        } else {
                                                            Log.v(TAG, "menu | popup item | remove_changes | dialog accepted | ScheduleLessonsFragment.scheduleLessons is null");
                                                        }
                                                    }
                                                });
                                            }
                                        })
                                        .setNegativeButton(R.string.cancel, null)
                                        .create().show();
                            }
                        }
                    });
                    break;
                }
                case R.id.open_settings: {
                    Intent intent = new Intent(activity, SettingsActivity.class);
                    intent.putExtra(PreferenceActivity.EXTRA_SHOW_FRAGMENT, SettingsActivity.AdditionalPreferenceFragment.class.getName());
                    startActivity(intent);
                    break;
                }
            }
            return false;
        }
    };
}
package com.bukhmastov.cdoitmo.fragments;

import android.app.Activity;
import android.content.Context;
import android.os.Bundle;
import android.support.design.widget.TabLayout;
import android.support.v4.app.Fragment;
import android.support.v4.view.ViewPager;
import android.support.v7.widget.SearchView;
import android.util.SparseIntArray;
import android.util.TypedValue;
import android.view.LayoutInflater;
import android.view.MenuItem;
import android.view.View;
import android.view.ViewGroup;
import android.widget.AdapterView;
import android.widget.LinearLayout;
import android.widget.ListView;
import android.widget.TextView;

import com.bukhmastov.cdoitmo.R;
import com.bukhmastov.cdoitmo.activities.MainActivity;
import com.bukhmastov.cdoitmo.adapters.PagerAdapter;
import com.bukhmastov.cdoitmo.adapters.TeacherPickerListView;
import com.bukhmastov.cdoitmo.converters.ScheduleLessonsAdditionalConverter;
import com.bukhmastov.cdoitmo.network.IfmoRestClient;
import com.bukhmastov.cdoitmo.objects.ScheduleLessons;
import com.bukhmastov.cdoitmo.utils.Static;
import com.bukhmastov.cdoitmo.utils.Storage;
import com.loopj.android.http.RequestHandle;

import org.json.JSONArray;
import org.json.JSONObject;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.Objects;

public class ScheduleLessonsFragment extends Fragment implements ScheduleLessons.response, ViewPager.OnPageChangeListener {

    private static final String TAG = "ScheduleLessonsFragment";
    public static ScheduleLessons scheduleLessons;
    private boolean loaded = false;
    public static RequestHandle fragmentRequestHandle = null;
    public static String query = null;
    private TabLayout schedule_tabs;
    public static JSONObject schedule;
    public static boolean schedule_cached = false;
    public static SparseIntArray scroll = new SparseIntArray();
    public static int tabSelected = -1;
    public static boolean reScheduleRequired = false;

    @Override
    public void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
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
        try {
            schedule_tabs = (TabLayout) getActivity().findViewById(R.id.schedule_tabs);
            if (!Static.OFFLINE_MODE) {
                MenuItem menuItem = MainActivity.menu.findItem(R.id.action_search);
                if (menuItem != null) {
                    menuItem.setVisible(true);
                    menuItem.setOnMenuItemClickListener(new MenuItem.OnMenuItemClickListener() {
                        @Override
                        public boolean onMenuItemClick(MenuItem item) {
                            MainActivity.menu.findItem(R.id.action_search).expandActionView();
                            return false;
                        }
                    });
                    SearchView searchView = (SearchView) menuItem.getActionView();
                    if (searchView != null) {
                        searchView.setQueryHint(getString(R.string.schedule_lessons_search_view_hint));
                    }
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
                scope = scheduleLessons.getDefault();
            }
            search(scope);
        }
        if (reScheduleRequired) {
            reScheduleRequired = false;
            reSchedule(getActivity());
        }
    }

    @Override
    public void onPause() {
        super.onPause();
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
        try {
            schedule_tabs.setVisibility(View.GONE);
            if (!Static.OFFLINE_MODE) {
                MenuItem menuItem = MainActivity.menu.findItem(R.id.action_search);
                if (menuItem != null) {
                    menuItem.setVisible(false);
                }
            }
        } catch (Exception e){
            Static.error(e);
        }
    }

    public static void searchAndClear(String query){
        ScheduleLessonsFragment.scroll.clear();
        ScheduleLessonsFragment.tabSelected = -1;
        search(query);
    }

    public static void search(String query){
        if (ScheduleLessonsFragment.scheduleLessons != null) {
            ScheduleLessonsFragment.query = query;
            ScheduleLessonsFragment.scheduleLessons.search(query);
        }
    }

    @Override
    public void onProgress(int state){
        try {
            schedule_tabs.setVisibility(View.GONE);
            draw(R.layout.state_loading);
            TextView loading_message = (TextView) getActivity().findViewById(R.id.loading_message);
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
        try {
            switch (state) {
                case IfmoRestClient.FAILED_OFFLINE:
                case ScheduleLessons.FAILED_OFFLINE:
                    draw(R.layout.state_offline);
                    View offline_reload = getActivity().findViewById(R.id.offline_reload);
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
                    View try_again_reload = getActivity().findViewById(R.id.try_again_reload);
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
        try {
            if (json == null) throw new NullPointerException("json cannot be null");
            schedule = json;
            schedule_tabs.setVisibility(View.GONE);
            if (Objects.equals(json.getString("type"), "teacher_picker")) {
                schedule_cached = false;
                JSONArray teachers = json.getJSONArray("list");
                if (teachers.length() > 0) {
                    if (teachers.length() == 1) {
                        search(teachers.getJSONObject(0).getString("pid"));
                        return;
                    }
                    draw(R.layout.layout_schedule_lessons_teacher_picker);
                    TextView teacher_picker_header = (TextView) getActivity().findViewById(R.id.teacher_picker_header);
                    ListView teacher_picker_list_view = (ListView) getActivity().findViewById(R.id.teacher_picker_list_view);
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
                        teacher_picker_list_view.setAdapter(new TeacherPickerListView(getActivity(), teachersMap));
                        teacher_picker_list_view.setOnItemClickListener(new AdapterView.OnItemClickListener() {
                            public void onItemClick(AdapterView<?> parent, View view, int position, long id) {
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
                    schedule_tabs.setVisibility(View.VISIBLE);
                    draw(R.layout.layout_schedule_lessons_tabs);
                    ViewPager schedule_view = (ViewPager) getActivity().findViewById(R.id.schedule_pager);
                    if (schedule_view != null) {
                        schedule_view.setAdapter(new PagerAdapter(getFragmentManager(), getContext()));
                        schedule_view.addOnPageChangeListener(this);
                        schedule_tabs.setupWithViewPager(schedule_view);
                    }
                    TabLayout.Tab tab;
                    if (ScheduleLessonsFragment.tabSelected == -1) {
                        int pref = Integer.parseInt(Storage.pref.get(getContext(), "pref_schedule_lessons_week", "-1"));
                        if (pref == -1) {
                            tab = schedule_tabs.getTabAt(Static.week >= 0 ? (Static.week % 2) + 1 : 0);
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

    private void notFound(){
        TypedValue typedValue = new TypedValue();
        getActivity().getTheme().resolveAttribute(android.R.attr.textColorPrimary, typedValue, true);
        int textColorPrimary = getActivity().obtainStyledAttributes(typedValue.data, new int[]{android.R.attr.textColorPrimary}).getColor(0, -1);
        float destiny = getContext().getResources().getDisplayMetrics().density;
        LinearLayout linearLayout = new LinearLayout(getContext());
        linearLayout.setOrientation(LinearLayout.VERTICAL);
        linearLayout.setLayoutParams(new LinearLayout.LayoutParams(LinearLayout.LayoutParams.MATCH_PARENT, LinearLayout.LayoutParams.WRAP_CONTENT));
        linearLayout.setPadding((int) (16 * destiny), (int) (10 * destiny), (int) (16 * destiny), (int) (10 * destiny));
        TextView title = new TextView(getContext());
        title.setText(":c");
        title.setLayoutParams(new LinearLayout.LayoutParams(LinearLayout.LayoutParams.MATCH_PARENT, LinearLayout.LayoutParams.WRAP_CONTENT));
        title.setTextColor(textColorPrimary);
        title.setTextSize(32);
        title.setPadding(0, 0, 0, (int) (10 * destiny));
        title.setTextAlignment(TextView.TEXT_ALIGNMENT_CENTER);
        linearLayout.addView(title);
        TextView desc = new TextView(getContext());
        desc.setText("По запросу" + " \"" + query + "\" " + " расписания не найдено");
        desc.setLayoutParams(new LinearLayout.LayoutParams(LinearLayout.LayoutParams.MATCH_PARENT, LinearLayout.LayoutParams.WRAP_CONTENT));
        desc.setTextColor(textColorPrimary);
        desc.setTextSize(16);
        desc.setTextAlignment(TextView.TEXT_ALIGNMENT_CENTER);
        linearLayout.addView(desc);
        ViewGroup vg = ((ViewGroup) getActivity().findViewById(R.id.container_schedule));
        if (vg != null) {
            vg.removeAllViews();
            vg.addView(linearLayout);
        }
    }
    private void draw(int layoutId){
        try {
            ViewGroup vg = ((ViewGroup) getActivity().findViewById(R.id.container_schedule));
            if (vg != null) {
                vg.removeAllViews();
                vg.addView(((LayoutInflater) getActivity().getSystemService(Context.LAYOUT_INFLATER_SERVICE)).inflate(layoutId, null), 0, new ViewGroup.LayoutParams(ViewGroup.LayoutParams.MATCH_PARENT, ViewGroup.LayoutParams.MATCH_PARENT));
            }
        } catch (Exception e){
            Static.error(e);
        }
    }

    public static void reSchedule(final Context context){
        new ScheduleLessonsAdditionalConverter(context, new ScheduleLessonsAdditionalConverter.response() {
            @Override
            public void finish(JSONObject json) {
                schedule = json;
                try {
                    Static.snackBar(((Activity) context).findViewById(android.R.id.content), context.getString(R.string.schedule_update_required), context.getString(R.string.update), new View.OnClickListener() {
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

    @Override
    public void onPageSelected(int position) {
        ScheduleLessonsFragment.tabSelected = position;
    }

    @Override
    public void onPageScrolled(int position, float positionOffset, int positionOffsetPixels) {}

    @Override
    public void onPageScrollStateChanged(int state) {}

}
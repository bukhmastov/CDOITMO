package com.bukhmastov.cdoitmo;

import android.app.Activity;
import android.content.Context;
import android.graphics.Typeface;
import android.os.AsyncTask;
import android.os.Build;
import android.os.Bundle;
import android.support.annotation.NonNull;
import android.support.design.widget.TabLayout;
import android.support.v4.app.Fragment;
import android.support.v4.app.FragmentManager;
import android.support.v4.app.FragmentStatePagerAdapter;
import android.support.v4.view.ViewPager;
import android.support.v4.widget.SwipeRefreshLayout;
import android.support.v7.widget.SearchView;
import android.util.Log;
import android.util.TypedValue;
import android.view.Gravity;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.AdapterView;
import android.widget.ArrayAdapter;
import android.widget.GridLayout;
import android.widget.ImageView;
import android.widget.LinearLayout;
import android.widget.ListView;
import android.widget.TextView;

import com.loopj.android.http.RequestHandle;

import org.htmlcleaner.HtmlCleaner;
import org.htmlcleaner.TagNode;
import org.json.JSONArray;
import org.json.JSONException;
import org.json.JSONObject;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.Objects;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

public class ScheduleLessonsFragment extends Fragment implements ScheduleLessons.response {

    private static final String TAG = "ScheduleLessonsFragment";
    static ScheduleLessons scheduleLessons;
    private boolean loaded = false;
    static RequestHandle fragmentRequestHandle = null;
    static String query = null;
    private TabLayout schedule_tabs;
    private ViewPager schedule_view;
    static JSONObject schedule;

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
        schedule_tabs = (TabLayout) getActivity().findViewById(R.id.schedule_tabs);
        if(!MainActivity.OFFLINE_MODE) MainActivity.menu.findItem(R.id.action_search).setVisible(true);
        if(!loaded) {
            loaded = true;
            scheduleLessons.search(MainActivity.group, false);
        }
    }

    @Override
    public void onPause() {
        super.onPause();
        if(fragmentRequestHandle != null) {
            loaded = false;
            fragmentRequestHandle.cancel(true);
        }
    }

    @Override
    public void onDestroy() {
        super.onDestroy();
        schedule_tabs.setVisibility(View.GONE);
        if(!MainActivity.OFFLINE_MODE) MainActivity.menu.findItem(R.id.action_search).setVisible(false);
    }

    @Override
    public void onProgress(int state){
        try {
            schedule_tabs.setVisibility(View.GONE);
            draw(R.layout.state_loading);
            TextView loading_message = (TextView) getActivity().findViewById(R.id.loading_message);
            switch (state) {
                case DeIfmoRestClient.STATE_HANDLING:
                    loading_message.setText(R.string.loading);
                    break;
                case DeIfmoRestClient.STATE_AUTHORIZATION:
                    loading_message.setText(R.string.authorization);
                    break;
                case DeIfmoRestClient.STATE_AUTHORIZED:
                    loading_message.setText(R.string.authorized);
                    break;
            }
        } catch (Exception e) {
            e.printStackTrace();
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
                case DeIfmoRestClient.FAILED_OFFLINE:
                    draw(R.layout.state_offline);
                    getActivity().findViewById(R.id.offline_reload).setOnClickListener(new View.OnClickListener() {
                        @Override
                        public void onClick(View v) {
                            scheduleLessons.search(query, false);
                        }
                    });
                    break;
                case DeIfmoRestClient.FAILED_TRY_AGAIN:
                case DeIfmoRestClient.FAILED_AUTH_TRY_AGAIN:
                case ScheduleLessons.FAILED_LOAD:
                    draw(R.layout.state_try_again);
                    if (state == DeIfmoRestClient.FAILED_AUTH_TRY_AGAIN)
                        ((TextView) getActivity().findViewById(R.id.try_again_message)).setText(R.string.auth_failed);
                    getActivity().findViewById(R.id.try_again_reload).setOnClickListener(new View.OnClickListener() {
                        @Override
                        public void onClick(View v) {
                            scheduleLessons.search(query, false);
                        }
                    });
                    break;
                case DeIfmoRestClient.FAILED_AUTH_CREDENTIALS_REQUIRED:
                    gotoLogin(LoginActivity.SIGNAL_CREDENTIALS_REQUIRED);
                    break;
                case DeIfmoRestClient.FAILED_AUTH_CREDENTIALS_FAILED:
                    gotoLogin(LoginActivity.SIGNAL_CREDENTIALS_FAILED);
                    break;
            }
        } catch (Exception e){
            e.printStackTrace();
        }
    }

    @Override
    public void onSuccess(JSONObject json){
        try {
            if(json == null) throw new NullPointerException("json cannot be null");
            schedule = json;
            schedule_tabs.setVisibility(View.GONE);
            if(Objects.equals(json.getString("type"), "teacher_picker")){
                draw(R.layout.layout_schedule_lessons_teacher_picker);
                TextView teacher_picker_header = (TextView) getActivity().findViewById(R.id.teacher_picker_header);
                ListView teacher_picker_list_view = (ListView) getActivity().findViewById(R.id.teacher_picker_list_view);
                JSONArray teachers = json.getJSONArray("teachers");
                if (teachers.length() > 0){
                    teacher_picker_header.setText(R.string.choose_teacher);
                    final ArrayList<HashMap<String, String>> teachersMap = new ArrayList<>();
                    for(int i = 0; i < teachers.length(); i++){
                        JSONObject teacher = teachers.getJSONObject(i);
                        HashMap<String, String> teacherMap = new HashMap<>();
                        teacherMap.put("name", teacher.getString("name"));
                        teacherMap.put("id", teacher.getString("id"));
                        teachersMap.add(teacherMap);
                    }
                    teacher_picker_list_view.setAdapter(new TeacherPickerListView(getActivity(), teachersMap));
                    teacher_picker_list_view.setOnItemClickListener(new AdapterView.OnItemClickListener(){
                        public void onItemClick(AdapterView<?> parent, View view, int position, long id) {
                            HashMap<String, String> teacherMap = teachersMap.get(position);
                            scheduleLessons.loadTeacherById(teacherMap.get("id"), false, false);
                        }
                    });
                } else {
                    teacher_picker_header.setText(R.string.teacher_not_found);
                }
            } else {
                if(schedule.getJSONArray("schedule").length() > 0){
                    schedule_tabs.setVisibility(View.VISIBLE);
                    draw(R.layout.layout_schedule_lessons_tabs);
                    schedule_view = (ViewPager) getActivity().findViewById(R.id.schedule_pager);
                    schedule_view.setAdapter(new PagerAdapter(getFragmentManager(), getContext()));
                    schedule_tabs.setupWithViewPager(schedule_view);
                } else {
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
                    vg.removeAllViews();
                    vg.addView(linearLayout);
                }
            }
        } catch (Exception e) {
            e.printStackTrace();
            onFailure(ScheduleLessons.FAILED_LOAD);
        }
    }
        
    void gotoLogin(int state){
        LoginActivity.state = state;
        getActivity().finish();
    }
    private void draw(int layoutId){
        try {
            ViewGroup vg = ((ViewGroup) getActivity().findViewById(R.id.container_schedule));
            vg.removeAllViews();
            vg.addView(((LayoutInflater) getActivity().getSystemService(Context.LAYOUT_INFLATER_SERVICE)).inflate(layoutId, null), 0, new ViewGroup.LayoutParams(ViewGroup.LayoutParams.MATCH_PARENT, ViewGroup.LayoutParams.MATCH_PARENT));
        } catch (Exception e){
            e.printStackTrace();
        }
    }
}

class ScheduleLessons implements SwipeRefreshLayout.OnRefreshListener {

    private static final String TAG = "ScheduleLessons";
    interface response {
        void onProgress(int state);
        void onFailure(int state);
        void onSuccess(JSONObject json);
        void onNewHandle(RequestHandle requestHandle);
    }
    private ScheduleLessons.response handler = null;
    private Context context;
    final static int FAILED_LOAD = 100;

    ScheduleLessons(Context context){
        this.context = context;
    }

    @Override
    public void onRefresh() {
        search(ScheduleLessonsFragment.query, true);
    }

    void setHandler(ScheduleLessons.response handler){
        this.handler = handler;
    }

    void search(String query, boolean force){
        query = query.trim();
        ScheduleLessonsFragment.query = query;
        if(ScheduleLessonsFragment.fragmentRequestHandle != null) ScheduleLessonsFragment.fragmentRequestHandle.cancel(true);
        if(Pattern.compile("^\\w{1,3}\\d{4}\\w?$").matcher(query).find()){
            searchGroup(query.toUpperCase(), force, false);
        }
        else if(Pattern.compile("^\\d+\\w*$").matcher(query).find()){
            searchRoom(query, force, false);
        }
        else {
            searchTeacher(query, force, false);
        }
    }
    private void searchGroup(final String group, final boolean force, final boolean toCache){
        final String cache = getCache("group_" + group);
        if((force || Objects.equals(cache, "")) && !MainActivity.OFFLINE_MODE) {
            DeIfmoRestClient.get("ru/schedule/0/" + group + "/schedule.htm", null, true, new DeIfmoRestClientResponseHandler() {
                @Override
                public void onSuccess(int statusCode, String response) {
                    if (statusCode == 200) {
                        new ScheduleLessonsGroupParse(new ScheduleLessonsGroupParse.response() {
                            @Override
                            public void finish(JSONObject json) {
                                if (toCache || Objects.equals(MainActivity.group.toUpperCase(), group)) putCache("group_" + group, json.toString());
                                handler.onSuccess(json);
                            }
                        }).execute(response);
                    } else {
                        if(Objects.equals(cache, "")){
                            handler.onFailure(FAILED_LOAD);
                        } else {
                            try {
                                handler.onSuccess(new JSONObject(cache));
                            } catch (JSONException e) {
                                e.printStackTrace();
                                handler.onFailure(FAILED_LOAD);
                            }
                        }
                    }
                }
                @Override
                public void onProgress(int state) {
                    handler.onProgress(state);
                }
                @Override
                public void onFailure(int state) {
                    handler.onFailure(state);
                }
                @Override
                public void onNewHandle(RequestHandle requestHandle) {
                    handler.onNewHandle(requestHandle);
                }
            });
        } else {
            try {
                handler.onSuccess(new JSONObject(cache));
            } catch (JSONException e) {
                e.printStackTrace();
                handler.onFailure(FAILED_LOAD);
            }
        }
    }
    private void searchRoom(final String room, final boolean force, final boolean toCache){
        final String cache = getCache("room_" + room);
        if((force || Objects.equals(cache, "")) && !MainActivity.OFFLINE_MODE) {
            DeIfmoRestClient.get("ru/schedule/2/" + room + "/schedule.htm", null, true, new DeIfmoRestClientResponseHandler() {
                @Override
                public void onSuccess(int statusCode, String response) {
                    if (statusCode == 200) {
                        new ScheduleLessonsRoomParse(new ScheduleLessonsRoomParse.response() {
                            @Override
                            public void finish(JSONObject json) {
                                if (toCache) putCache("room_" + room, json.toString());
                                handler.onSuccess(json);
                            }
                        }).execute(response);
                    } else {
                        if(Objects.equals(cache, "")){
                            handler.onFailure(FAILED_LOAD);
                        } else {
                            try {
                                handler.onSuccess(new JSONObject(cache));
                            } catch (JSONException e) {
                                e.printStackTrace();
                                handler.onFailure(FAILED_LOAD);
                            }
                        }
                    }
                }
                @Override
                public void onProgress(int state) {
                    handler.onProgress(state);
                }
                @Override
                public void onFailure(int state) {
                    handler.onFailure(state);
                }
                @Override
                public void onNewHandle(RequestHandle requestHandle) {
                    handler.onNewHandle(requestHandle);
                }
            });
        } else {
            try {
                handler.onSuccess(new JSONObject(cache));
            } catch (JSONException e) {
                e.printStackTrace();
                handler.onFailure(FAILED_LOAD);
            }
        }
    }
    private void searchTeacher(final String teacher, final boolean force, final boolean toCache){
        final String cache = getCache("teacher_picker_" + teacher);
        if((force || Objects.equals(cache, "")) && !MainActivity.OFFLINE_MODE) {
            DeIfmoRestClient.get("ru/schedule/1/" + teacher + "/schedule.htm", null, true, new DeIfmoRestClientResponseHandler() {
                @Override
                public void onSuccess(int statusCode, String response) {
                    if (statusCode == 200) {
                        new ScheduleLessonsTeacherPickerParse(new ScheduleLessonsTeacherPickerParse.response() {
                            @Override
                            public void finish(JSONObject json) {
                                if (toCache) putCache("teacher_picker_" + teacher, json.toString());
                                try {
                                    if(json.getJSONArray("teachers").length() == 1){
                                        loadTeacherById(json.getJSONArray("teachers").getJSONObject(0).getString("id"), force, toCache);
                                    } else {
                                        handler.onSuccess(json);
                                    }
                                } catch (JSONException e) {
                                    e.printStackTrace();
                                    handler.onFailure(FAILED_LOAD);
                                }
                            }
                        }).execute(response);
                    } else {
                        if(Objects.equals(cache, "")){
                            handler.onFailure(FAILED_LOAD);
                        } else {
                            try {
                                JSONObject list = new JSONObject(cache);
                                if(list.getJSONArray("teachers").length() == 1){
                                    loadTeacherById(list.getJSONArray("teachers").getJSONObject(0).getString("id"), force, toCache);
                                } else {
                                    handler.onSuccess(list);
                                }
                            } catch (JSONException e) {
                                e.printStackTrace();
                                handler.onFailure(FAILED_LOAD);
                            }
                        }
                    }
                }
                @Override
                public void onProgress(int state) {
                    handler.onProgress(state);
                }
                @Override
                public void onFailure(int state) {
                    handler.onFailure(state);
                }
                @Override
                public void onNewHandle(RequestHandle requestHandle) {
                    handler.onNewHandle(requestHandle);
                }
            });
        } else {
            try {
                JSONObject list = new JSONObject(cache);
                if(list.getJSONArray("teachers").length() == 1){
                    loadTeacherById(list.getJSONArray("teachers").getJSONObject(0).getString("id"), force, toCache);
                } else {
                    handler.onSuccess(list);
                }
            } catch (JSONException e) {
                e.printStackTrace();
                handler.onFailure(FAILED_LOAD);
            }
        }
    }
    void loadTeacherById(final String id, final boolean force, final boolean toCache){
        final String cache = getCache("teacher_" + id);
        if((force || Objects.equals(cache, "")) && !MainActivity.OFFLINE_MODE) {
            DeIfmoRestClient.get("ru/schedule/3/" + id + "/schedule.htm", null, true, new DeIfmoRestClientResponseHandler() {
                @Override
                public void onSuccess(int statusCode, String response) {
                    if (statusCode == 200) {
                        new ScheduleLessonsTeacherParse(new ScheduleLessonsTeacherParse.response() {
                            @Override
                            public void finish(JSONObject json) {
                                if (toCache) putCache("teacher_" + id, json.toString());
                                handler.onSuccess(json);
                            }
                        }).execute(response);
                    } else {
                        if(Objects.equals(cache, "")){
                            handler.onFailure(FAILED_LOAD);
                        } else {
                            try {
                                handler.onSuccess(new JSONObject(cache));
                            } catch (JSONException e) {
                                e.printStackTrace();
                                handler.onFailure(FAILED_LOAD);
                            }
                        }
                    }
                }
                @Override
                public void onProgress(int state) {
                    handler.onProgress(state);
                }
                @Override
                public void onFailure(int state) {
                    handler.onFailure(state);
                }
                @Override
                public void onNewHandle(RequestHandle requestHandle) {
                    handler.onNewHandle(requestHandle);
                }
            });
        } else {
            try {
                handler.onSuccess(new JSONObject(cache));
            } catch (JSONException e) {
                e.printStackTrace();
                handler.onFailure(FAILED_LOAD);
            }
        }
    }

    void getSchedule(Context context, int type, LinearLayout container){
        try {
            JSONArray schedule = ScheduleLessonsFragment.schedule.getJSONArray("schedule");
            float destiny = context.getResources().getDisplayMetrics().density;
            TypedValue typedValue = new TypedValue();
            context.getTheme().resolveAttribute(android.R.attr.textColorPrimary, typedValue, true);
            int textColorPrimary = context.obtainStyledAttributes(typedValue.data, new int[]{android.R.attr.textColorPrimary}).getColor(0, -1);
            context.getTheme().resolveAttribute(android.R.attr.textColorSecondary, typedValue, true);
            int textColorSecondary = context.obtainStyledAttributes(typedValue.data, new int[]{android.R.attr.textColorSecondary}).getColor(0, -1);
            context.getTheme().resolveAttribute(R.attr.colorSeparator, typedValue, true);
            int colorSeparator = context.obtainStyledAttributes(typedValue.data, new int[]{R.attr.colorSeparator}).getColor(0, -1);
            int daysCount = 0;
            for (int i = 0; i < schedule.length(); i++) {
                int lessonsCount = 0;
                JSONObject day = schedule.getJSONObject(i);
                // -- день
                LinearLayout dayLayout = new LinearLayout(context);
                dayLayout.setOrientation(LinearLayout.VERTICAL);
                dayLayout.setLayoutParams(new LinearLayout.LayoutParams(LinearLayout.LayoutParams.MATCH_PARENT, LinearLayout.LayoutParams.WRAP_CONTENT));
                dayLayout.setPadding((int) (16 * destiny), (int) (10 * destiny), (int) (16 * destiny), 0);
                // заголовок дня
                TextView dayTitle = new TextView(context);
                dayTitle.setText(day.getString("title").toUpperCase());
                dayTitle.setTypeface(null, Typeface.BOLD);
                dayTitle.setLayoutParams(new LinearLayout.LayoutParams(LinearLayout.LayoutParams.MATCH_PARENT, LinearLayout.LayoutParams.WRAP_CONTENT));
                dayTitle.setTextColor(textColorPrimary);
                dayLayout.addView(dayTitle);
                // -- занятия
                LinearLayout lessonsLayout = new LinearLayout(context);
                lessonsLayout.setOrientation(LinearLayout.VERTICAL);
                LinearLayout.LayoutParams layoutParams = new LinearLayout.LayoutParams(LinearLayout.LayoutParams.MATCH_PARENT, LinearLayout.LayoutParams.WRAP_CONTENT);
                layoutParams.setMargins(0, (int) (10 * destiny), 0, 0);
                lessonsLayout.setLayoutParams(layoutParams);
                lessonsLayout.setBackground(context.getResources().getDrawable(R.drawable.shape_border_round, context.getTheme()));
                JSONArray lessons = day.getJSONArray("lessons");
                for (int j = 0; j < lessons.length(); j++) {
                    final JSONObject lesson = lessons.getJSONObject(j);
                    if(!(type == 2 || type == lesson.getInt("week") || lesson.getInt("week") == 2)) continue;
                    lessonsCount++;
                    // разделитель занятий
                    if (j != 0) {
                        View separator = new View(context);
                        separator.setLayoutParams(new LinearLayout.LayoutParams(LinearLayout.LayoutParams.MATCH_PARENT, (int) (1 * destiny)));
                        separator.setBackgroundColor(colorSeparator);
                        lessonsLayout.addView(separator);
                    }
                    // --- занятие
                    LinearLayout lessonLayout = new LinearLayout(context);
                    lessonLayout.setId(Integer.parseInt(1 + "" + type + "" + i + "" + j));
                    lessonLayout.setOrientation(LinearLayout.HORIZONTAL);
                    lessonLayout.setLayoutParams(new LinearLayout.LayoutParams(LinearLayout.LayoutParams.MATCH_PARENT, LinearLayout.LayoutParams.WRAP_CONTENT));
                    lessonLayout.setPadding(0, 0, (int) (2 * destiny), 0);
                    // -- время занятия
                    LinearLayout lessonTimeLayout = new LinearLayout(context);
                    lessonTimeLayout.setOrientation(LinearLayout.VERTICAL);
                    lessonTimeLayout.setLayoutParams(new LinearLayout.LayoutParams(LinearLayout.LayoutParams.WRAP_CONTENT, LinearLayout.LayoutParams.MATCH_PARENT));
                    lessonTimeLayout.setGravity(Gravity.CENTER_VERTICAL);
                    lessonTimeLayout.setPadding((int) (10 * destiny), (int) (10 * destiny), (int) (10 * destiny), (int) (10 * destiny));
                    lessonTimeLayout.setMinimumWidth((int) (60 * destiny));
                    // время начала
                    TextView rangeStart = new TextView(context);
                    rangeStart.setText(Objects.equals(lesson.getString("timeStart"), "") ? "∞" : lesson.getString("timeStart"));
                    rangeStart.setLayoutParams(new LinearLayout.LayoutParams(LinearLayout.LayoutParams.MATCH_PARENT, LinearLayout.LayoutParams.WRAP_CONTENT));
                    rangeStart.setTextColor(textColorPrimary);
                    rangeStart.setTextSize(15);
                    rangeStart.setTextAlignment(TextView.TEXT_ALIGNMENT_CENTER);
                    rangeStart.setGravity(Gravity.BOTTOM);
                    lessonTimeLayout.addView(rangeStart);
                    // иконка времени
                    ImageView timeIcon = new ImageView(context);
                    timeIcon.setImageDrawable(context.getResources().getDrawable(R.drawable.ic_access_time, context.getTheme()));
                    timeIcon.setLayoutParams(new LinearLayout.LayoutParams(LinearLayout.LayoutParams.MATCH_PARENT, (int) (16 * destiny)));
                    lessonTimeLayout.addView(timeIcon);
                    // время конца
                    TextView rangeEnd = new TextView(context);
                    rangeEnd.setText(Objects.equals(lesson.getString("timeEnd"), "") ? "∞" : lesson.getString("timeEnd"));
                    rangeEnd.setLayoutParams(new LinearLayout.LayoutParams(LinearLayout.LayoutParams.MATCH_PARENT, LinearLayout.LayoutParams.WRAP_CONTENT));
                    rangeEnd.setTextColor(textColorPrimary);
                    rangeEnd.setTextSize(15);
                    rangeEnd.setTextAlignment(TextView.TEXT_ALIGNMENT_CENTER);
                    rangeEnd.setGravity(Gravity.TOP);
                    lessonTimeLayout.addView(rangeEnd);
                    lessonLayout.addView(lessonTimeLayout);
                    // -- информация о занятии
                    LinearLayout lessonContentLayout = new LinearLayout(context);
                    lessonContentLayout.setOrientation(LinearLayout.VERTICAL);
                    lessonContentLayout.setLayoutParams(new LinearLayout.LayoutParams(LinearLayout.LayoutParams.MATCH_PARENT, LinearLayout.LayoutParams.MATCH_PARENT));
                    lessonContentLayout.setGravity(Gravity.CENTER_VERTICAL);
                    lessonContentLayout.setPadding(0, (int) (5 * destiny), 0, (int) (10 * destiny));
                    // - заголовок занятия и иконка
                    LinearLayout lessonTitleLayout = new LinearLayout(context);
                    lessonTitleLayout.setOrientation(LinearLayout.HORIZONTAL);
                    lessonTitleLayout.setLayoutParams(new LinearLayout.LayoutParams(LinearLayout.LayoutParams.MATCH_PARENT, LinearLayout.LayoutParams.WRAP_CONTENT));
                    // заголовок
                    TextView subjectTitle = new TextView(context);
                    subjectTitle.setText(lesson.getString("subject"));
                    LinearLayout.LayoutParams layoutParams1 = new LinearLayout.LayoutParams(LinearLayout.LayoutParams.WRAP_CONTENT, LinearLayout.LayoutParams.WRAP_CONTENT);
                    layoutParams1.gravity = Gravity.CENTER_VERTICAL;
                    layoutParams1.weight = 1;
                    subjectTitle.setLayoutParams(layoutParams1);
                    subjectTitle.setTextColor(textColorPrimary);
                    lessonTitleLayout.addView(subjectTitle);
                    // иконка
                    if((lesson.has("room") && !Objects.equals(lesson.getString("room"), "")) ||
                            (lesson.has("teacher") && !Objects.equals(lesson.getString("teacher"), "")) ||
                            (lesson.has("group") && !Objects.equals(lesson.getString("group"), ""))
                            ) {
                        ImageView moreIcon = new ImageView(context);
                        moreIcon.setImageDrawable(context.getResources().getDrawable(R.drawable.ic_touch, context.getTheme()));
                        moreIcon.setLayoutParams(new LinearLayout.LayoutParams((int) (24 * destiny), (int) (24 * destiny)));
                        lessonTitleLayout.addView(moreIcon);
                    }
                    lessonContentLayout.addView(lessonTitleLayout);
                    // - главное описание занятия
                    TextView descMain = new TextView(context);
                    String descMainText = "";
                    switch (ScheduleLessonsFragment.schedule.getString("type")) {
                        case "group":
                            descMainText = lesson.getString("teacher");
                            break;
                        case "teacher":
                            descMainText = lesson.getString("group");
                            break;
                        case "room":
                            String group = lesson.getString("group");
                            String teacher = lesson.getString("teacher");
                            if (Objects.equals(group, "")) {
                                descMainText = teacher;
                            } else {
                                descMainText = group;
                                if (!Objects.equals(teacher, ""))
                                    descMainText += " (" + teacher + ")";
                            }
                            break;
                    }
                    if (!Objects.equals(descMainText, "")) {
                        descMain.setText(descMainText);
                    } else {
                        descMain.setHeight(0);
                    }
                    descMain.setLayoutParams(new LinearLayout.LayoutParams(LinearLayout.LayoutParams.MATCH_PARENT, LinearLayout.LayoutParams.WRAP_CONTENT));
                    descMain.setTextColor(textColorSecondary);
                    lessonContentLayout.addView(descMain);
                    // - флаги занятия
                    GridLayout flagsContainer = new GridLayout(context);
                    flagsContainer.setOrientation(GridLayout.HORIZONTAL);
                    flagsContainer.setLayoutParams(new LinearLayout.LayoutParams(LinearLayout.LayoutParams.MATCH_PARENT, LinearLayout.LayoutParams.WRAP_CONTENT));
                    int week = lesson.getInt("week");
                    if (type == 2 && (week == 0 || week == 1)) {
                        TextView weekFlag = new TextView(context);
                        weekFlag.setText(week == 0 ? context.getString(R.string.tab_even) : context.getString(R.string.tab_odd));
                        LinearLayout.LayoutParams layoutParams2 = new LinearLayout.LayoutParams(LinearLayout.LayoutParams.WRAP_CONTENT, LinearLayout.LayoutParams.WRAP_CONTENT);
                        layoutParams2.setMargins(0, (int) (2 * destiny), (int) (5 * destiny), (int) (2 * destiny));
                        weekFlag.setLayoutParams(layoutParams2);
                        weekFlag.setPadding((int) (6 * destiny), (int) (2 * destiny), (int) (6 * destiny), (int) (2 * destiny));
                        context.getTheme().resolveAttribute(R.attr.colorScheduleFlagCommonBG, typedValue, true);
                        weekFlag.setBackgroundColor(typedValue.data);
                        context.getTheme().resolveAttribute(R.attr.colorScheduleFlagTEXT, typedValue, true);
                        weekFlag.setTextColor(typedValue.data);
                        flagsContainer.addView(weekFlag);
                    }
                    if (!Objects.equals(lesson.getString("type"), "")) {
                        TextView typeFlag = new TextView(context);
                        int backgroundColor, textColor;
                        switch (lesson.getString("type")) {
                            case "practice":
                                context.getTheme().resolveAttribute(R.attr.colorScheduleFlagPracticeBG, typedValue, true);
                                backgroundColor = typedValue.data;
                                context.getTheme().resolveAttribute(R.attr.colorScheduleFlagTEXT, typedValue, true);
                                textColor = typedValue.data;
                                typeFlag.setText(R.string.practice);
                                break;
                            case "lecture":
                                context.getTheme().resolveAttribute(R.attr.colorScheduleFlagLectureBG, typedValue, true);
                                backgroundColor = typedValue.data;
                                context.getTheme().resolveAttribute(R.attr.colorScheduleFlagTEXT, typedValue, true);
                                textColor = typedValue.data;
                                typeFlag.setText(R.string.lecture);
                                break;
                            case "lab":
                                context.getTheme().resolveAttribute(R.attr.colorScheduleFlagLabBG, typedValue, true);
                                backgroundColor = typedValue.data;
                                context.getTheme().resolveAttribute(R.attr.colorScheduleFlagTEXT, typedValue, true);
                                textColor = typedValue.data;
                                typeFlag.setText(R.string.lab);
                                break;
                            default:
                                context.getTheme().resolveAttribute(R.attr.colorScheduleFlagCommonBG, typedValue, true);
                                backgroundColor = typedValue.data;
                                context.getTheme().resolveAttribute(R.attr.colorScheduleFlagTEXT, typedValue, true);
                                textColor = typedValue.data;
                                typeFlag.setText(lesson.getString("type"));
                                break;
                        }
                        LinearLayout.LayoutParams layoutParams3 = new LinearLayout.LayoutParams(LinearLayout.LayoutParams.WRAP_CONTENT, LinearLayout.LayoutParams.WRAP_CONTENT);
                        layoutParams3.setMargins(0, (int) (2 * destiny), (int) (5 * destiny), (int) (2 * destiny));
                        typeFlag.setLayoutParams(layoutParams3);
                        typeFlag.setPadding((int) (6 * destiny), (int) (2 * destiny), (int) (6 * destiny), (int) (2 * destiny));
                        typeFlag.setBackgroundColor(backgroundColor);
                        typeFlag.setTextColor(textColor);
                        flagsContainer.addView(typeFlag);
                    }
                    lessonContentLayout.addView(flagsContainer);
                    // - второстепенное описание занятия
                    TextView descSecondary = new TextView(context);
                    String descSecondaryText = "";
                    switch (ScheduleLessonsFragment.schedule.getString("type")) {
                        case "group":
                        case "teacher":
                            String room = lesson.getString("room");
                            String building = lesson.getString("building");
                            if (Objects.equals(room, "")) {
                                descSecondaryText = building;
                            } else {
                                descSecondaryText = "Ауд." + " " + room;
                                if (!Objects.equals(building, ""))
                                    descSecondaryText += " (" + building + ")";
                            }
                            break;
                        case "room":
                            descSecondaryText += lesson.getString("building");
                            break;
                    }
                    if (!Objects.equals(descSecondaryText, "")) {
                        descSecondary.setText(descSecondaryText);
                    } else {
                        descSecondary.setHeight(0);
                    }
                    descSecondary.setLayoutParams(new LinearLayout.LayoutParams(LinearLayout.LayoutParams.MATCH_PARENT, LinearLayout.LayoutParams.WRAP_CONTENT));
                    descSecondary.setTextColor(textColorSecondary);
                    lessonContentLayout.addView(descSecondary);
                    lessonLayout.addView(lessonContentLayout);
                    lessonsLayout.addView(lessonLayout);
                }
                dayLayout.addView(lessonsLayout);
                if(lessonsCount > 0){
                    container.addView(dayLayout);
                    daysCount++;
                }
            }
            if(daysCount == 0) {
                LinearLayout scheduleEmptyLayout = new LinearLayout(context);
                scheduleEmptyLayout.setOrientation(LinearLayout.VERTICAL);
                scheduleEmptyLayout.setLayoutParams(new LinearLayout.LayoutParams(LinearLayout.LayoutParams.MATCH_PARENT, LinearLayout.LayoutParams.WRAP_CONTENT));
                TextView textView = new TextView(context);
                textView.setText("Занятий нет");
                textView.setLayoutParams(new LinearLayout.LayoutParams(LinearLayout.LayoutParams.MATCH_PARENT, LinearLayout.LayoutParams.WRAP_CONTENT));
                textView.setTextColor(textColorPrimary);
                textView.setGravity(Gravity.CENTER);
                textView.setHeight((int) (60 * destiny));
                scheduleEmptyLayout.addView(textView);
                container.addView(scheduleEmptyLayout);
            }
        } catch (Exception e){
            e.printStackTrace();
        }
    }
    
    private String getCache(String token){
        return Cache.get(context, "schedule_lessons_" + token.replace(" ", "-"));
    }
    private void putCache(String token, String value){
        Cache.put(context, "schedule_lessons_" + token.replace(" ", "-"), value);
    }
}

class ScheduleLessonsSearchView extends SearchView {
    ScheduleLessonsSearchView(Context context){
        super(context);
        this.setQueryHint(context.getString(R.string.schedule_lessons_search_view_hint));
        this.setSubmitButtonEnabled(true);
        this.setElevation(4);
        this.setOnQueryTextListener(new OnQueryTextListener(){
            @Override
            public boolean onQueryTextSubmit(String query) {
                MainActivity.menu.findItem(R.id.action_search).collapseActionView();
                ScheduleLessonsFragment.scheduleLessons.search(query, false);
                return false;
            }
            @Override
            public boolean onQueryTextChange(String newText) { return false; }
        });
    }
}

class ScheduleLessonsGroupParse extends AsyncTask<String, Void, JSONObject> {
    interface response {
        void finish(JSONObject json);
    }
    private response delegate = null;
    ScheduleLessonsGroupParse(response delegate){
        this.delegate = delegate;
    }
    @Override
    protected JSONObject doInBackground(String... params) {
        try {
            Matcher m;
            HtmlCleaner cleaner = new HtmlCleaner();
            TagNode root = cleaner.clean(params[0].replace("&nbsp;", " "));
            TagNode[] days = root.getElementsByAttValue("class", "rasp_tabl_day", true, false);
            JSONArray schedule = new JSONArray();
            for(TagNode day : days){
                TagNode table = day.getAllElements(false)[0];
                m = Pattern.compile("^(.)day$").matcher(table.getAttributeByName("id"));
                if(m.find()){
                    JSONObject dayObj = new JSONObject();
                    int dayNumber = Integer.parseInt(m.group(1));
                    dayObj.put("number", dayNumber);
                    switch (dayNumber){
                        case 1: dayObj.put("title", "Понедельник"); dayObj.put("titleShort", "Пн"); break;
                        case 2: dayObj.put("title", "Вторник"); dayObj.put("titleShort", "Вт"); break;
                        case 3: dayObj.put("title", "Среда"); dayObj.put("titleShort", "Ср"); break;
                        case 4: dayObj.put("title", "Четверг"); dayObj.put("titleShort", "Чт"); break;
                        case 5: dayObj.put("title", "Пятница"); dayObj.put("titleShort", "Пт"); break;
                        case 6: dayObj.put("title", "Суббота"); dayObj.put("titleShort", "Сб"); break;
                        case 7: dayObj.put("title", "Воскресенье"); dayObj.put("titleShort", "Вс"); break;
                        default: dayObj.put("title", ""); dayObj.put("titleShort", ""); break;
                    }
                    JSONArray lessonsObj = new JSONArray();
                    TagNode[] lessons = table.getAllElements(false)[0].getElementsByName("tr", false);
                    for(TagNode lesson : lessons){
                        if(!lesson.hasChildren()) continue;
                        JSONObject lessonObj = new JSONObject();
                        TagNode[] time = lesson.getElementsByAttValue("class", "time", false, false)[0].getAllElements(false);
                        switch (time[1].getAllElements(false)[0].getText().toString().trim()){
                            case "четная неделя": lessonObj.put("week", 0); break;
                            case "нечетная неделя": lessonObj.put("week", 1); break;
                            default: lessonObj.put("week", 2); break;
                        }
                        String[] range = time[0].getText().toString().split("-");
                        lessonObj.put("timeStart", range.length > 0 ? range[0].trim() : "∞");
                        lessonObj.put("timeEnd", range.length > 1 ? range[1].trim() : "∞");
                        TagNode[] room = lesson.getElementsByAttValue("class", "room", false, false)[0].getAllElements(false)[0].getAllElements(false);
                        lessonObj.put("room", room[0].getText().toString().replace("ауд.", "").trim());
                        lessonObj.put("building", room[1].getText().toString().trim());
                        TagNode[] meta = lesson.getElementsByAttValue("class", "lesson", false, false)[0].getAllElements(false)[0].getAllElements(false);
                        String subject = meta[0].getText().toString().trim();
                        m = Pattern.compile("(.*)\\(Прак\\)(.*)").matcher(subject);
                        if(m.find()){
                            lessonObj.put("subject", (m.group(1).trim() + " " + m.group(2).trim()).trim());
                            lessonObj.put("type", "practice");
                        } else {
                            m = Pattern.compile("(.*)\\(Лек\\)(.*)").matcher(subject);
                            if(m.find()){
                                lessonObj.put("subject", (m.group(1).trim() + " " + m.group(2).trim()).trim());
                                lessonObj.put("type", "lecture");
                            } else {
                                m = Pattern.compile("(.*)\\(Лаб\\)(.*)").matcher(subject);
                                if(m.find()){
                                    lessonObj.put("subject", (m.group(1).trim() + " " + m.group(2).trim()).trim());
                                    lessonObj.put("type", "lab");
                                } else {
                                    lessonObj.put("subject", subject);
                                    lessonObj.put("type", "");
                                }
                            }
                        }
                        lessonObj.put("teacher", meta[1].getText().toString().trim());
                        lessonsObj.put(lessonObj);
                    }
                    dayObj.put("lessons", lessonsObj);
                    schedule.put(dayObj);
                }
            }
            JSONObject response = new JSONObject();
            response.put("type", "group");
            TagNode[] title = root.getElementsByAttValue("class", "schedule-title", true, false);
            response.put("scope", title.length > 0 ? title[0].getText().toString().replace("Расписание группы", "").trim() : "");
            response.put("schedule", schedule);
            return response;
        } catch (Exception e) {
            e.printStackTrace();
            return null;
        }
    }
    @Override
    protected void onPostExecute(JSONObject json) {
        delegate.finish(json);
    }
}
class ScheduleLessonsRoomParse extends AsyncTask<String, Void, JSONObject> {
    interface response {
        void finish(JSONObject json);
    }
    private response delegate = null;
    ScheduleLessonsRoomParse(response delegate){
        this.delegate = delegate;
    }
    @Override
    protected JSONObject doInBackground(String... params) {
        try {
            Matcher m;
            HtmlCleaner cleaner = new HtmlCleaner();
            TagNode root = cleaner.clean(params[0].replace("&nbsp;", " "));
            TagNode[] days = root.getElementsByAttValue("class", "rasp_tabl_day", true, false);
            JSONArray schedule = new JSONArray();
            for(TagNode day : days){
                TagNode table = day.getAllElements(false)[0];
                m = Pattern.compile("^(.)day$").matcher(table.getAttributeByName("id"));
                if(m.find()){
                    JSONObject dayObj = new JSONObject();
                    int dayNumber = Integer.parseInt(m.group(1));
                    dayObj.put("number", dayNumber);
                    switch (dayNumber){
                        case 1: dayObj.put("title", "Понедельник"); dayObj.put("titleShort", "Пн"); break;
                        case 2: dayObj.put("title", "Вторник"); dayObj.put("titleShort", "Вт"); break;
                        case 3: dayObj.put("title", "Среда"); dayObj.put("titleShort", "Ср"); break;
                        case 4: dayObj.put("title", "Четверг"); dayObj.put("titleShort", "Чт"); break;
                        case 5: dayObj.put("title", "Пятница"); dayObj.put("titleShort", "Пт"); break;
                        case 6: dayObj.put("title", "Суббота"); dayObj.put("titleShort", "Сб"); break;
                        case 7: dayObj.put("title", "Воскресенье"); dayObj.put("titleShort", "Вс"); break;
                        default: dayObj.put("title", ""); dayObj.put("titleShort", ""); break;
                    }
                    JSONArray lessonsObj = new JSONArray();
                    TagNode[] lessons = table.getAllElements(false)[0].getElementsByName("tr", false);
                    for(TagNode lesson : lessons) {
                        if (!lesson.hasChildren()) continue;
                        JSONObject lessonObj = new JSONObject();
                        TagNode[] time = lesson.getElementsByAttValue("class", "time", false, false)[0].getAllElements(false);
                        String[] range = time[0].getText().toString().split("-");
                        lessonObj.put("timeStart", range.length > 0 ? range[0].trim() : "∞");
                        lessonObj.put("timeEnd", range.length > 1 ? range[1].trim() : "∞");
                        TagNode[] room = lesson.getElementsByAttValue("class", "room", false, false)[0].getAllElements(false)[0].getAllElements(false);
                        //lessonObj.put("room", room[0].getText().toString().replace("ауд.", "").trim());
                        lessonObj.put("building", room[1].getText().toString().trim());
                        TagNode[] group = lesson.getElementsByAttValue("class", "time", false, false)[1].getAllElements(false);
                        lessonObj.put("group", group[0].getText().toString().trim());
                        TagNode[] meta = lesson.getElementsByAttValue("class", "lesson", false, false)[0].getAllElements(false)[0].getAllElements(false);
                        String subject = meta[0].getText().toString().trim();
                        m = Pattern.compile("(.*)\\(Прак\\)(.*)").matcher(subject);
                        if(m.find()){
                            lessonObj.put("subject", (m.group(1).trim() + " " + m.group(2).trim()).trim());
                            lessonObj.put("type", "practice");
                        } else {
                            m = Pattern.compile("(.*)\\(Лек\\)(.*)").matcher(subject);
                            if(m.find()){
                                lessonObj.put("subject", (m.group(1).trim() + " " + m.group(2).trim()).trim());
                                lessonObj.put("type", "lecture");
                            } else {
                                m = Pattern.compile("(.*)\\(Лаб\\)(.*)").matcher(subject);
                                if(m.find()){
                                    lessonObj.put("subject", (m.group(1).trim() + " " + m.group(2).trim()).trim());
                                    lessonObj.put("type", "lab");
                                } else {
                                    lessonObj.put("subject", subject);
                                    lessonObj.put("type", "");
                                }
                            }
                        }
                        switch (meta[1].getText().toString().trim()){
                            case "четная неделя": lessonObj.put("week", 0); break;
                            case "нечетная неделя": lessonObj.put("week", 1); break;
                            default: lessonObj.put("week", 2); break;
                        }
                        lessonObj.put("teacher", meta[2].getText().toString().trim());
                        lessonsObj.put(lessonObj);
                    }
                    dayObj.put("lessons", lessonsObj);
                    schedule.put(dayObj);
                }
            }
            JSONObject response = new JSONObject();
            response.put("type", "room");
            response.put("scope", root.getElementsByAttValue("class", "page-header", true, false)[0].getText().toString().replace("Расписание занятий в аудитории", "").trim());
            response.put("schedule", schedule);
            return response;
        } catch (Exception e) {
            e.printStackTrace();
            return null;
        }
    }
    @Override
    protected void onPostExecute(JSONObject json) {
        delegate.finish(json);
    }
}
class ScheduleLessonsTeacherPickerParse extends AsyncTask<String, Void, JSONObject> {
    interface response {
        void finish(JSONObject json);
    }
    private response delegate = null;
    ScheduleLessonsTeacherPickerParse(response delegate){
        this.delegate = delegate;
    }
    @Override
    protected JSONObject doInBackground(String... params) {
        try {
            Matcher m;
            HtmlCleaner cleaner = new HtmlCleaner();
            TagNode root = cleaner.clean(params[0].replace("&nbsp;", " "));
            TagNode content = root.getElementsByAttValue("class", "content_block", true, false)[0];
            JSONArray teachers = new JSONArray();
            TagNode[] p = content.getElementsByName("p", false);
            for(TagNode item : p){
                try {
                    TagNode[] elements = item.getAllElements(false);
                    if(elements.length == 0) continue;
                    m = Pattern.compile("^/ru/schedule/3/(.+)/.*$").matcher(elements[0].getAttributeByName("href"));
                    if(m.find()){
                        JSONObject teacher = new JSONObject();
                        teacher.put("name", elements[0].getText().toString().trim());
                        teacher.put("id", m.group(1));
                        teachers.put(teacher);
                    }
                } catch (Exception e){
                    e.printStackTrace();
                }
            }
            JSONObject response = new JSONObject();
            response.put("type", "teacher_picker");
            response.put("teachers", teachers);
            return response;
        } catch (Exception e) {
            e.printStackTrace();
            return null;
        }
    }
    @Override
    protected void onPostExecute(JSONObject json) {
        delegate.finish(json);
    }
}
class ScheduleLessonsTeacherParse extends AsyncTask<String, Void, JSONObject> {
    interface response {
        void finish(JSONObject json);
    }
    private response delegate = null;
    ScheduleLessonsTeacherParse(response delegate){
        this.delegate = delegate;
    }
    @Override
    protected JSONObject doInBackground(String... params) {
        try {
            Matcher m;
            HtmlCleaner cleaner = new HtmlCleaner();
            TagNode root = cleaner.clean(params[0].replace("&nbsp;", " "));
            TagNode[] days = root.getElementsByAttValue("class", "rasp_tabl_day", true, false);
            JSONArray schedule = new JSONArray();
            for(TagNode day : days) {
                TagNode table = day.getAllElements(false)[0];
                m = Pattern.compile("^(.)day$").matcher(table.getAttributeByName("id"));
                if (m.find()) {
                    JSONObject dayObj = new JSONObject();
                    int dayNumber = Integer.parseInt(m.group(1));
                    dayObj.put("number", dayNumber);
                    switch (dayNumber){
                        case 1: dayObj.put("title", "Понедельник"); dayObj.put("titleShort", "Пн"); break;
                        case 2: dayObj.put("title", "Вторник"); dayObj.put("titleShort", "Вт"); break;
                        case 3: dayObj.put("title", "Среда"); dayObj.put("titleShort", "Ср"); break;
                        case 4: dayObj.put("title", "Четверг"); dayObj.put("titleShort", "Чт"); break;
                        case 5: dayObj.put("title", "Пятница"); dayObj.put("titleShort", "Пт"); break;
                        case 6: dayObj.put("title", "Суббота"); dayObj.put("titleShort", "Сб"); break;
                        case 7: dayObj.put("title", "Воскресенье"); dayObj.put("titleShort", "Вс"); break;
                        default: dayObj.put("title", ""); dayObj.put("titleShort", ""); break;
                    }
                    JSONArray lessonsObj = new JSONArray();
                    TagNode[] lessons = table.getAllElements(false)[0].getElementsByName("tr", false);
                    for(TagNode lesson : lessons) {
                        if (!lesson.hasChildren()) continue;
                        JSONObject lessonObj = new JSONObject();
                        TagNode time = lesson.getElementsByAttValue("class", "time", false, false)[0].getAllElements(false)[0];
                        TagNode[] dl = time.getElementsByName("dl", false);
                        String week = "";
                        if(dl.length > 0){
                            week = dl[0].getText().toString().trim();
                            switch (week){
                                case "четная неделя": lessonObj.put("week", 0); break;
                                case "нечетная неделя": lessonObj.put("week", 1); break;
                                default: lessonObj.put("week", 2); break;
                            }
                        } else {
                            lessonObj.put("week", 2);
                        }
                        String range = time.getText().toString().replace(week, "").trim();
                        if(!Objects.equals(range, "")){
                            String[] rangeArr = range.split("-");
                            lessonObj.put("timeStart", rangeArr.length > 0 ? rangeArr[0].trim() : "∞");
                            lessonObj.put("timeEnd", rangeArr.length > 1 ? rangeArr[1].trim() : "∞");
                        } else {
                            lessonObj.put("timeStart", "");
                            lessonObj.put("timeEnd", "");
                        }
                        TagNode group = lesson.getAllElements(false)[2];
                        lessonObj.put("group", group.getText().toString().trim());
                        TagNode[] room = lesson.getElementsByAttValue("class", "room", false, false)[0].getAllElements(false)[0].getAllElements(false);
                        lessonObj.put("room", room[0].getText().toString().replace("ауд.", "").trim());
                        lessonObj.put("building", room[1].getText().toString().trim());
                        TagNode[] meta = lesson.getElementsByAttValue("class", "lesson", false, false)[0].getAllElements(false)[0].getAllElements(false);
                        String subject = meta[0].getText().toString().trim();
                        m = Pattern.compile("(.*)\\(Прак\\)(.*)").matcher(subject);
                        if(m.find()){
                            lessonObj.put("subject", (m.group(1).trim() + " " + m.group(2).trim()).trim());
                            lessonObj.put("type", "practice");
                        } else {
                            m = Pattern.compile("(.*)\\(Лек\\)(.*)").matcher(subject);
                            if(m.find()){
                                lessonObj.put("subject", (m.group(1).trim() + " " + m.group(2).trim()).trim());
                                lessonObj.put("type", "lecture");
                            } else {
                                m = Pattern.compile("(.*)\\(Лаб\\)(.*)").matcher(subject);
                                if(m.find()){
                                    lessonObj.put("subject", (m.group(1).trim() + " " + m.group(2).trim()).trim());
                                    lessonObj.put("type", "lab");
                                } else {
                                    lessonObj.put("subject", subject);
                                    lessonObj.put("type", "");
                                }
                            }
                        }
                        lessonsObj.put(lessonObj);
                    }
                    dayObj.put("lessons", lessonsObj);
                    schedule.put(dayObj);
                }
            }
            JSONObject response = new JSONObject();
            response.put("type", "teacher");
            response.put("scope", root.getElementsByAttValue("class", "page-header", true, false)[0].getText().toString().replaceAll("Расписание занятий|\\(|\\)", "").trim());
            response.put("schedule", schedule);
            return response;
        } catch (Exception e) {
            e.printStackTrace();
            return null;
        }
    }
    @Override
    protected void onPostExecute(JSONObject json) {
        delegate.finish(json);
    }
}

class TeacherPickerListView extends ArrayAdapter<HashMap<String, String>> {

    private final Activity context;
    private final ArrayList<HashMap<String, String>> teachersMap;

    TeacherPickerListView(Activity context, ArrayList<HashMap<String, String>> teachersMap) {
        super(context, R.layout.listview_teacher_picker, teachersMap);
        this.context = context;
        this.teachersMap = teachersMap;
    }

    @NonNull
    @Override
    public View getView(int position, View view, @NonNull ViewGroup parent) {
        LayoutInflater inflater = context.getLayoutInflater();
        HashMap<String, String> teacherMap = teachersMap.get(position);
        View rowView;
        rowView = inflater.inflate(R.layout.listview_teacher_picker, null, true);
        ((TextView) rowView.findViewById(R.id.lv_teacher_picker_name)).setText(teacherMap.get("name"));
        return rowView;
    }
}

class PagerAdapter extends FragmentStatePagerAdapter {

    private Context context;

    public PagerAdapter(FragmentManager fm, Context context) {
        super(fm);
        this.context = context;
    }

    @Override
    public Fragment getItem(int position) {
        switch (position){
            case 2: return new ScheduleLessonsOddFragment();
            case 1: return new ScheduleLessonsEvenFragment();
            default:
            case 0: return new ScheduleLessonsAllFragment();
        }
    }

    @Override
    public int getCount() {
        return 3;
    }

    @Override
    public CharSequence getPageTitle(int position) {
        switch (position){
            case 2: return context.getString(R.string.tab_odd);
            case 1: return context.getString(R.string.tab_even);
            default:
            case 0: return context.getString(R.string.tab_all);
        }
    }
}

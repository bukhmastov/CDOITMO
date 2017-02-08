package com.bukhmastov.cdoitmo;

import android.app.Activity;
import android.content.Context;
import android.content.SharedPreferences;
import android.graphics.Typeface;
import android.os.AsyncTask;
import android.os.Bundle;
import android.preference.PreferenceManager;
import android.support.v4.app.Fragment;
import android.support.v4.widget.SwipeRefreshLayout;
import android.support.v7.widget.SearchView;
import android.view.ContextMenu;
import android.view.Gravity;
import android.view.LayoutInflater;
import android.view.MenuItem;
import android.view.View;
import android.view.ViewGroup;
import android.widget.AdapterView;
import android.widget.LinearLayout;
import android.widget.ListView;
import android.widget.ProgressBar;
import android.widget.TextView;

import com.loopj.android.http.RequestHandle;

import org.htmlcleaner.HtmlCleaner;
import org.htmlcleaner.TagNode;
import org.json.JSONArray;
import org.json.JSONException;
import org.json.JSONObject;

import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.Calendar;
import java.util.Date;
import java.util.HashMap;
import java.util.Locale;
import java.util.Objects;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

public class ScheduleExamsFragment extends Fragment implements ScheduleExams.response {

    private static final String TAG = "ScheduleExamsFragment";
    static ScheduleExams scheduleExams;
    private boolean loaded = false;
    static RequestHandle fragmentRequestHandle = null;
    static String query = null;
    static JSONObject schedule;

    @Override
    public void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
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
        try {
            if(!MainActivity.OFFLINE_MODE){
                MenuItem action_search = MainActivity.menu.findItem(R.id.action_search);
                if (action_search != null){
                    action_search.setVisible(true);
                    SearchView searchView = (SearchView) action_search.getActionView();
                    if (searchView != null) {
                        searchView.setQueryHint(getString(R.string.schedule_exams_search_view_hint));
                    }
                }
            }
        } catch (Exception e){
            if(LoginActivity.errorTracker != null) LoginActivity.errorTracker.add(e);
        }
        if (!loaded) {
            loaded = true;
            SharedPreferences sharedPreferences = PreferenceManager.getDefaultSharedPreferences(getActivity());
            scheduleExams.search(MainActivity.group, sharedPreferences.getBoolean("pref_use_cache", true) && sharedPreferences.getBoolean("pref_force_load_schedule", false));
        }
    }

    @Override
    public void onPause() {
        super.onPause();
        if (fragmentRequestHandle != null) {
            loaded = false;
            fragmentRequestHandle.cancel(true);
        }
    }

    @Override
    public void onDestroy() {
        super.onDestroy();
        try {
            if (!MainActivity.OFFLINE_MODE) {
                MenuItem action_search = MainActivity.menu.findItem(R.id.action_search);
                if (action_search != null) {
                    action_search.setVisible(false);
                }
            }
        } catch (Exception e){
            if(LoginActivity.errorTracker != null) LoginActivity.errorTracker.add(e);
        }
    }

    @Override
    public void onProgress(int state){
        try {
            draw(R.layout.state_loading);
            TextView loading_message = (TextView) getActivity().findViewById(R.id.loading_message);
            if (loading_message != null) {
                switch (state) {
                    case DeIfmoRestClient.STATE_HANDLING: loading_message.setText(R.string.loading); break;
                    case DeIfmoRestClient.STATE_AUTHORIZATION: loading_message.setText(R.string.authorization); break;
                    case DeIfmoRestClient.STATE_AUTHORIZED: loading_message.setText(R.string.authorized); break;
                }
            }
        } catch (Exception e) {
            if(LoginActivity.errorTracker != null) LoginActivity.errorTracker.add(e);
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
                    View offline_reload = getActivity().findViewById(R.id.offline_reload);
                    if (offline_reload != null) {
                        offline_reload.setOnClickListener(new View.OnClickListener() {
                            @Override
                            public void onClick(View v) {
                                scheduleExams.search(query, false);
                            }
                        });
                    }
                    break;
                case DeIfmoRestClient.FAILED_TRY_AGAIN:
                case DeIfmoRestClient.FAILED_AUTH_TRY_AGAIN:
                case ScheduleExams.FAILED_LOAD:
                    draw(R.layout.state_try_again);
                    if (state == DeIfmoRestClient.FAILED_AUTH_TRY_AGAIN){
                        TextView try_again_message = (TextView) getActivity().findViewById(R.id.try_again_message);
                        if (try_again_message != null) try_again_message.setText(R.string.auth_failed);
                    }
                    View try_again_reload = getActivity().findViewById(R.id.try_again_reload);
                    if (try_again_reload != null) {
                        try_again_reload.setOnClickListener(new View.OnClickListener() {
                            @Override
                            public void onClick(View v) {
                                scheduleExams.search(query, false);
                            }
                        });
                    }
                    break;
                case DeIfmoRestClient.FAILED_AUTH_CREDENTIALS_REQUIRED: gotoLogin(LoginActivity.SIGNAL_CREDENTIALS_REQUIRED); break;
                case DeIfmoRestClient.FAILED_AUTH_CREDENTIALS_FAILED: gotoLogin(LoginActivity.SIGNAL_CREDENTIALS_FAILED); break;
            }
        } catch (Exception e){
            if(LoginActivity.errorTracker != null) LoginActivity.errorTracker.add(e);
        }
    }

    @Override
    public void onSuccess(JSONObject json){
        try {
            if (json == null) throw new NullPointerException("json cannot be null");
            schedule = json;
            if (Objects.equals(json.getString("type"), "teacher_picker")) {
                JSONArray teachers = json.getJSONArray("teachers");
                if (teachers.length() > 0){
                    draw(R.layout.layout_schedule_lessons_teacher_picker);
                    TextView teacher_picker_header = (TextView) getActivity().findViewById(R.id.teacher_picker_header);
                    ListView teacher_picker_list_view = (ListView) getActivity().findViewById(R.id.teacher_picker_list_view);
                    if (teacher_picker_header != null) teacher_picker_header.setText(R.string.choose_teacher);
                    if (teacher_picker_list_view != null) {
                        final ArrayList<HashMap<String, String>> teachersMap = new ArrayList<>();
                        for (int i = 0; i < teachers.length(); i++) {
                            JSONObject teacher = teachers.getJSONObject(i);
                            HashMap<String, String> teacherMap = new HashMap<>();
                            teacherMap.put("name", teacher.getString("name"));
                            teacherMap.put("scope", teacher.getString("scope"));
                            teacherMap.put("id", teacher.getString("id"));
                            teachersMap.add(teacherMap);
                        }
                        teacher_picker_list_view.setAdapter(new TeacherPickerListView(getActivity(), teachersMap));
                        teacher_picker_list_view.setOnItemClickListener(new AdapterView.OnItemClickListener() {
                            public void onItemClick(AdapterView<?> parent, View view, int position, long id) {
                                HashMap<String, String> teacherMap = teachersMap.get(position);
                                scheduleExams.search(teacherMap.get("scope"), false);
                            }
                        });
                    }
                } else {
                    notFound();
                }
            } else {
                if (schedule.getJSONArray("schedule").length() > 0) {
                    draw(R.layout.layout_schedule_exams);
                    TextView schedule_exams_header = (TextView) getActivity().findViewById(R.id.schedule_exams_header);
                    switch (schedule.getString("type")){
                        case "group": if (schedule_exams_header != null) schedule_exams_header.setText("Расписание группы" + " " + schedule.getString("scope")); break;
                        case "teacher": if (schedule_exams_header != null) schedule_exams_header.setText("Расписание преподавателя" + " " + schedule.getString("scope")); break;
                        default: throw new Exception("Wrong ScheduleExamsFragment.schedule.TYPE value");
                    }
                    TextView schedule_exams_week = (TextView) getActivity().findViewById(R.id.schedule_exams_week);
                    if (schedule_exams_week != null) {
                        if (MainActivity.week >= 0) {
                            schedule_exams_week.setText(MainActivity.week + " " + getString(R.string.school_week));
                        } else {
                            schedule_exams_week.setText(new SimpleDateFormat("dd.MM.yyyy", Locale.ROOT).format(new Date(Calendar.getInstance().getTimeInMillis())));
                        }
                    }
                    // работаем со свайпом
                    SwipeRefreshLayout mSwipeRefreshLayout = (SwipeRefreshLayout) getActivity().findViewById(R.id.schedule_exams_container);
                    if (mSwipeRefreshLayout != null) {
                        mSwipeRefreshLayout.setColorSchemeColors(MainActivity.colorAccent);
                        mSwipeRefreshLayout.setProgressBackgroundColorSchemeColor(MainActivity.colorBackgroundRefresh);
                        mSwipeRefreshLayout.setOnRefreshListener(scheduleExams);
                    }
                    // отображаем расписание
                    final ViewGroup linearLayout = (ViewGroup) getActivity().findViewById(R.id.schedule_exams_content);
                    (new ScheduleExamsBuilder(getActivity(), new ScheduleExamsBuilder.response(){
                        public void state(final int state, final LinearLayout layout){
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
                                if(LoginActivity.errorTracker != null) LoginActivity.errorTracker.add(e);
                                onFailure(ScheduleExams.FAILED_LOAD);
                            }
                        }
                    })).start();
                } else {
                    notFound();
                }
            }
        } catch (Exception e) {
            if (LoginActivity.errorTracker != null) LoginActivity.errorTracker.add(e);
            onFailure(ScheduleExams.FAILED_LOAD);
        }
    }

    @Override
    public boolean onContextItemSelected(MenuItem item) {
        if (getUserVisibleHint()) {
            if (ScheduleExamsFragment.scheduleExams != null) ScheduleExamsFragment.scheduleExams.search(item.getTitle().toString().replace(getString(R.string.group), "").trim(), false);
            return true;
        }
        return super.onContextItemSelected(item);
    }

    private void notFound(){
        LinearLayout linearLayout = new LinearLayout(getContext());
        linearLayout.setOrientation(LinearLayout.VERTICAL);
        linearLayout.setLayoutParams(new LinearLayout.LayoutParams(LinearLayout.LayoutParams.MATCH_PARENT, LinearLayout.LayoutParams.WRAP_CONTENT));
        linearLayout.setPadding((int) (16 * MainActivity.destiny), (int) (10 * MainActivity.destiny), (int) (16 * MainActivity.destiny), (int) (10 * MainActivity.destiny));
        TextView title = new TextView(getContext());
        title.setText(":c");
        title.setLayoutParams(new LinearLayout.LayoutParams(LinearLayout.LayoutParams.MATCH_PARENT, LinearLayout.LayoutParams.WRAP_CONTENT));
        title.setTextColor(MainActivity.textColorPrimary);
        title.setTextSize(32);
        title.setPadding(0, 0, 0, (int) (10 * MainActivity.destiny));
        title.setTextAlignment(TextView.TEXT_ALIGNMENT_CENTER);
        linearLayout.addView(title);
        TextView desc = new TextView(getContext());
        desc.setText("По запросу" + " \"" + query + "\" " + " расписания не найдено");
        desc.setLayoutParams(new LinearLayout.LayoutParams(LinearLayout.LayoutParams.MATCH_PARENT, LinearLayout.LayoutParams.WRAP_CONTENT));
        desc.setTextColor(MainActivity.textColorPrimary);
        desc.setTextSize(16);
        desc.setTextAlignment(TextView.TEXT_ALIGNMENT_CENTER);
        linearLayout.addView(desc);
        ViewGroup vg = ((ViewGroup) getActivity().findViewById(R.id.container_schedule_exams));
        if (vg != null) {
            vg.removeAllViews();
            vg.addView(linearLayout);
        }
    }
    void gotoLogin(int state){
        LoginActivity.state = state;
        getActivity().finish();
    }
    private void draw(int layoutId){
        try {
            ViewGroup vg = ((ViewGroup) getActivity().findViewById(R.id.container_schedule_exams));
            if (vg != null) {
                vg.removeAllViews();
                vg.addView(((LayoutInflater) getActivity().getSystemService(Context.LAYOUT_INFLATER_SERVICE)).inflate(layoutId, null), 0, new ViewGroup.LayoutParams(ViewGroup.LayoutParams.MATCH_PARENT, ViewGroup.LayoutParams.MATCH_PARENT));
            }
        } catch (Exception e){
            if (LoginActivity.errorTracker != null) LoginActivity.errorTracker.add(e);
        }
    }
}

class ScheduleExams implements SwipeRefreshLayout.OnRefreshListener {

    private static final String TAG = "ScheduleExams";
    interface response {
        void onProgress(int state);
        void onFailure(int state);
        void onSuccess(JSONObject json);
        void onNewHandle(RequestHandle requestHandle);
    }
    private ScheduleExams.response handler = null;
    private Context context;
    final static int FAILED_LOAD = 100;

    ScheduleExams(Context context){
        this.context = context;
    }

    @Override
    public void onRefresh() {
        search(ScheduleExamsFragment.query, true);
    }

    void setHandler(ScheduleExams.response handler){
        this.handler = handler;
    }

    void search(String query, boolean force){
        search(query, force, false);
    }
    void search(String query, boolean force, boolean toCache){
        query = query.trim();
        ScheduleExamsFragment.query = query;
        if(ScheduleExamsFragment.fragmentRequestHandle != null) ScheduleExamsFragment.fragmentRequestHandle.cancel(true);
        if(Pattern.compile("^\\w{1,3}\\d{4}\\w?$").matcher(query).find()){
            searchGroup(query.toUpperCase(), force, toCache);
        }
        else if(Pattern.compile("^teacher\\d+$").matcher(query).find()){
            searchDefinedTeacher(query, force, toCache);
        }
        else {
            searchTeacher(query, force, toCache);
        }
    }

    private void searchGroup(final String group, final boolean force, final boolean toCache){
        final String cache = getCache("group_" + group);
        if((force || Objects.equals(cache, "")) && !MainActivity.OFFLINE_MODE) {
            DeIfmoRestClient.get(context, "ru/exam/0/" + group + "/raspisanie_sessii.htm", null, true, new DeIfmoRestClientResponseHandler() {
                @Override
                public void onSuccess(int statusCode, String response) {
                    if (statusCode == 200) {
                        new ScheduleExamsGroupParse(new ScheduleExamsGroupParse.response() {
                            @Override
                            public void finish(JSONObject json) {
                                try {
                                    if (json == null) throw new NullPointerException("json cannot be null");
                                    if (toCache || Objects.equals(MainActivity.group.toUpperCase(), group)){
                                        if(json.getJSONArray("schedule").length() > 0) putCache("group_" + group, json.toString());
                                    }
                                    handler.onSuccess(json);
                                } catch (Exception e) {
                                    if(LoginActivity.errorTracker != null) LoginActivity.errorTracker.add(e);
                                    handler.onFailure(FAILED_LOAD);
                                }
                            }
                        }).execute(response);
                    } else {
                        if(Objects.equals(cache, "")){
                            handler.onFailure(FAILED_LOAD);
                        } else {
                            try {
                                handler.onSuccess(new JSONObject(cache));
                            } catch (JSONException e) {
                                if(LoginActivity.errorTracker != null) LoginActivity.errorTracker.add(e);
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
                if(LoginActivity.errorTracker != null) LoginActivity.errorTracker.add(e);
                handler.onFailure(FAILED_LOAD);
            }
        }
    }
    private void searchTeacher(final String teacher, final boolean force, final boolean toCache){
        final String cache = getCache("teacher_picker_" + teacher);
        if((force || Objects.equals(cache, "")) && !MainActivity.OFFLINE_MODE) {
            DeIfmoRestClient.get(context, "ru/exam/1/" + teacher + "/raspisanie_sessii.htm", null, true, new DeIfmoRestClientResponseHandler() {
                @Override
                public void onSuccess(int statusCode, String response) {
                    if (statusCode == 200) {
                        new ScheduleExamsTeacherPickerParse(new ScheduleExamsTeacherPickerParse.response() {
                            @Override
                            public void finish(JSONObject json) {
                                try {
                                    if (json == null) throw new NullPointerException("json cannot be null");
                                    if (toCache){
                                        if(json.getJSONArray("teachers").length() > 0) putCache("teacher_picker_" + teacher, json.toString());
                                    }
                                    if (json.getJSONArray("teachers").length() == 1){
                                        search(json.getJSONArray("teachers").getJSONObject(0).getString("scope"), force, toCache);
                                    } else {
                                        handler.onSuccess(json);
                                    }
                                } catch (Exception e) {
                                    if(LoginActivity.errorTracker != null) LoginActivity.errorTracker.add(e);
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
                                    search(list.getJSONArray("teachers").getJSONObject(0).getString("scope"), force, toCache);
                                } else {
                                    handler.onSuccess(list);
                                }
                            } catch (JSONException e) {
                                if(LoginActivity.errorTracker != null) LoginActivity.errorTracker.add(e);
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
                    search(list.getJSONArray("teachers").getJSONObject(0).getString("scope"), force, toCache);
                } else {
                    handler.onSuccess(list);
                }
            } catch (JSONException e) {
                if(LoginActivity.errorTracker != null) LoginActivity.errorTracker.add(e);
                handler.onFailure(FAILED_LOAD);
            }
        }
    }
    private void searchDefinedTeacher(final String teacherId, final boolean force, final boolean toCache){
        Matcher m = Pattern.compile("^teacher(\\d+)$").matcher(teacherId);
        if(m.find()){
            final String id = m.group(1);
            final String cache = getCache("teacher_" + id);
            if((force || Objects.equals(cache, "")) && !MainActivity.OFFLINE_MODE) {
                DeIfmoRestClient.get(context, "ru/exam/3/" + id + "/raspisanie_sessii.htm", null, true, new DeIfmoRestClientResponseHandler() {
                    @Override
                    public void onSuccess(int statusCode, String response) {
                        if (statusCode == 200) {
                            new ScheduleExamsTeacherParse(new ScheduleExamsTeacherParse.response() {
                                @Override
                                public void finish(JSONObject json) {
                                    try {
                                        if(json == null) throw new NullPointerException("json cannot be null");
                                        if (toCache) {
                                            if(json.getJSONArray("schedule").length() > 0) putCache("teacher_" + id, json.toString());
                                        }
                                        handler.onSuccess(json);
                                    } catch (Exception e) {
                                        if(LoginActivity.errorTracker != null) LoginActivity.errorTracker.add(e);
                                        handler.onFailure(FAILED_LOAD);
                                    }
                                }
                            }).execute(response);
                        } else {
                            if(Objects.equals(cache, "")){
                                handler.onFailure(FAILED_LOAD);
                            } else {
                                try {
                                    handler.onSuccess(new JSONObject(cache));
                                } catch (JSONException e) {
                                    if(LoginActivity.errorTracker != null) LoginActivity.errorTracker.add(e);
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
                    if(LoginActivity.errorTracker != null) LoginActivity.errorTracker.add(e);
                    handler.onFailure(FAILED_LOAD);
                }
            }
        } else {
            handler.onFailure(FAILED_LOAD);
        }
    }

    private String getCache(String token){
        try {
            String jsonStr = Cache.get(context, "schedule_exams");
            if(Objects.equals(jsonStr, "")){
                return "";
            } else {
                JSONObject json = new JSONObject(jsonStr);
                if(json.has(token)){
                    return json.getString(token);
                } else {
                    return "";
                }
            }
        } catch (JSONException e) {
            if(LoginActivity.errorTracker != null) LoginActivity.errorTracker.add(e);
            return "";
        }
    }
    private void putCache(String token, String value){
        try {
            String jsonStr = Cache.get(context, "schedule_exams");
            JSONObject json;
            if(Objects.equals(jsonStr, "")){
                json = new JSONObject();
            } else {
                json = new JSONObject(jsonStr);
            }
            if(json.has(token)) json.remove(token);
            json.put(token, value);
            Cache.put(context, "schedule_exams", json.toString());
        } catch (JSONException e) {
            if(LoginActivity.errorTracker != null) LoginActivity.errorTracker.add(e);
        }
    }
}

class ScheduleExamsGroupParse extends AsyncTask<String, Void, JSONObject> {
    interface response {
        void finish(JSONObject json);
    }
    private response delegate = null;
    ScheduleExamsGroupParse(response delegate){
        this.delegate = delegate;
    }
    @Override
    protected JSONObject doInBackground(String... params) {
        try {
            Matcher m;
            HtmlCleaner cleaner = new HtmlCleaner();
            TagNode root = cleaner.clean(params[0].replace("&nbsp;", " "));
            TagNode[] exams = root.getElementsByAttValue("class", "rasp_tabl_day", true, false);
            JSONArray schedule = new JSONArray();
            for(TagNode exam : exams){
                JSONObject examContainerObj = new JSONObject();
                JSONObject examObj = new JSONObject();
                JSONObject consultObj = new JSONObject();
                TagNode[] fields = exam.getAllElements(false)[0].getAllElements(false)[0].getAllElements(false)[0].getAllElements(false);
                examObj.put("date", fields[0].getText().toString().trim());
                examObj.put("time", fields[1].getAllElements(false)[0].getText().toString().trim());
                examObj.put("room", fields[2].getAllElements(false)[0].getAllElements(false)[0].getText().toString().trim().replace(".", "").trim());
                TagNode meta = fields[3].getAllElements(false)[0];
                examContainerObj.put("subject", meta.getAllElements(false)[0].getText().toString().trim());
                examContainerObj.put("teacher", meta.getAllElements(false)[1].getText().toString().trim());
                m = Pattern.compile("^Консультация (.{1,10}) в (\\d{1,2}:\\d{1,2}) Место:(.*)$").matcher(meta.getAllElements(false)[2].getText().toString().trim());
                if(m.find()){
                    consultObj.put("date", m.group(1));
                    consultObj.put("time", m.group(2));
                    consultObj.put("room", m.group(3).replace(".", "").trim());
                }
                examContainerObj.put("exam", examObj);
                examContainerObj.put("consult", consultObj);
                schedule.put(examContainerObj);
            }
            JSONObject response = new JSONObject();
            response.put("type", "group");
            TagNode[] title = root.getElementsByAttValue("class", "page-header", true, false);
            response.put("scope", title.length > 0 ? title[0].getText().toString().replace("Расписание группы", "").trim() : "");
            response.put("schedule", schedule);
            return response;
        } catch (Exception e) {
            if(LoginActivity.errorTracker != null) LoginActivity.errorTracker.add(e);
            return null;
        }
    }
    @Override
    protected void onPostExecute(JSONObject json) {
        delegate.finish(json);
    }
}
class ScheduleExamsTeacherPickerParse extends AsyncTask<String, Void, JSONObject> {
    interface response {
        void finish(JSONObject json);
    }
    private response delegate = null;
    ScheduleExamsTeacherPickerParse(response delegate){
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
                    m = Pattern.compile("^/ru/exam/3/(.+)/.*$").matcher(elements[0].getAttributeByName("href"));
                    if(m.find()){
                        JSONObject teacher = new JSONObject();
                        teacher.put("name", elements[0].getText().toString().trim());
                        teacher.put("scope", "teacher" + m.group(1));
                        teacher.put("id", m.group(1));
                        teachers.put(teacher);
                    }
                } catch (Exception e){
                    if(LoginActivity.errorTracker != null) LoginActivity.errorTracker.add(e);
                }
            }
            JSONObject response = new JSONObject();
            response.put("type", "teacher_picker");
            response.put("teachers", teachers);
            return response;
        } catch (Exception e) {
            if(LoginActivity.errorTracker != null) LoginActivity.errorTracker.add(e);
            return null;
        }
    }
    @Override
    protected void onPostExecute(JSONObject json) {
        delegate.finish(json);
    }
}
class ScheduleExamsTeacherParse extends AsyncTask<String, Void, JSONObject> {
    interface response {
        void finish(JSONObject json);
    }
    private response delegate = null;
    ScheduleExamsTeacherParse(response delegate){
        this.delegate = delegate;
    }
    @Override
    protected JSONObject doInBackground(String... params) {
        try {
            Matcher m;
            HtmlCleaner cleaner = new HtmlCleaner();
            TagNode root = cleaner.clean(params[0].replace("&nbsp;", " "));
            TagNode[] exams = root.getElementsByAttValue("class", "rasp_tabl_day", true, false);
            JSONArray schedule = new JSONArray();
            String teacher = "";
            for(TagNode exam : exams){
                JSONObject examContainerObj = new JSONObject();
                JSONObject examObj = new JSONObject();
                JSONObject consultObj = new JSONObject();
                TagNode[] fields = exam.getAllElements(false)[0].getAllElements(false)[0].getAllElements(false)[0].getAllElements(false);
                examObj.put("date", fields[0].getAllElements(false)[0].getText().toString().trim());
                examObj.put("time", fields[1].getAllElements(false)[0].getText().toString().trim());
                examObj.put("room", fields[2].getAllElements(false)[0].getAllElements(false)[0].getText().toString().trim().replace(".", "").trim());
                examContainerObj.put("group", fields[3].getAllElements(false)[0].getText().toString().trim());
                TagNode meta = fields[4].getAllElements(false)[0];
                examContainerObj.put("subject", meta.getAllElements(false)[0].getText().toString().trim());
                teacher = meta.getAllElements(false)[1].getText().toString().trim();
                m = Pattern.compile("^Консультация (.{1,10}) в (\\d{1,2}:\\d{1,2}) Место:(.*)$").matcher(meta.getAllElements(false)[2].getText().toString().trim());
                if(m.find()){
                    consultObj.put("date", m.group(1));
                    consultObj.put("time", m.group(2));
                    consultObj.put("room", m.group(3).replace(".", "").trim());
                }
                examContainerObj.put("exam", examObj);
                examContainerObj.put("consult", consultObj);
                schedule.put(examContainerObj);
            }
            JSONObject response = new JSONObject();
            response.put("type", "teacher");
            response.put("scope", teacher);
            response.put("schedule", schedule);
            return response;
        } catch (Exception e) {
            if(LoginActivity.errorTracker != null) LoginActivity.errorTracker.add(e);
            return null;
        }
    }
    @Override
    protected void onPostExecute(JSONObject json) {
        delegate.finish(json);
    }
}

class ScheduleExamsBuilder extends Thread {

    interface response {
        void state(int state, LinearLayout layout);
    }
    private response delegate = null;
    private Activity activity;
    private float destiny;

    static final int STATE_FAILED = 0;
    static final int STATE_LOADING = 1;
    static final int STATE_DONE = 2;

    ScheduleExamsBuilder(Activity activity, ScheduleExamsBuilder.response delegate){
        this.activity = activity;
        this.delegate = delegate;
        this.destiny = activity.getResources().getDisplayMetrics().density;
    }
    public void run(){
        LinearLayout container = new LinearLayout(activity);
        container.setOrientation(LinearLayout.VERTICAL);
        container.setLayoutParams(new LinearLayout.LayoutParams(LinearLayout.LayoutParams.MATCH_PARENT, LinearLayout.LayoutParams.WRAP_CONTENT));
        try {
            delegate.state(STATE_LOADING, getLoadingScreen());
            String type = ScheduleExamsFragment.schedule.getString("type");
            JSONArray schedule = ScheduleExamsFragment.schedule.getJSONArray("schedule");
            for(int i = 0; i < schedule.length(); i++){
                JSONObject exam = schedule.getJSONObject(i);
                LinearLayout examContainer = new LinearLayout(activity);
                examContainer.setOrientation(LinearLayout.VERTICAL);
                LinearLayout.LayoutParams lp = new LinearLayout.LayoutParams(LinearLayout.LayoutParams.MATCH_PARENT, LinearLayout.LayoutParams.WRAP_CONTENT);
                lp.setMargins((int) (16 * destiny), (int) (16 * destiny), (int) (16 * destiny), 0);
                examContainer.setLayoutParams(lp);
                examContainer.setBackground(activity.getResources().getDrawable(R.drawable.shape_border_round, activity.getTheme()));
                // заголовок экзамена
                LinearLayout headerContainer = new LinearLayout(activity);
                headerContainer.setOrientation(LinearLayout.VERTICAL);
                headerContainer.setLayoutParams(new LinearLayout.LayoutParams(LinearLayout.LayoutParams.MATCH_PARENT, LinearLayout.LayoutParams.WRAP_CONTENT));
                headerContainer.setPadding((int) (12 * destiny), (int) (10 * destiny), (int) (12 * destiny), (int) (10 * destiny));
                TextView headerTextCommon = new TextView(activity);
                headerTextCommon.setText(exam.getString("subject").toUpperCase());
                headerTextCommon.setTypeface(null, Typeface.BOLD);
                headerTextCommon.setLayoutParams(new LinearLayout.LayoutParams(LinearLayout.LayoutParams.MATCH_PARENT, LinearLayout.LayoutParams.WRAP_CONTENT));
                headerTextCommon.setTextColor(MainActivity.textColorPrimary);
                headerContainer.addView(headerTextCommon);
                TextView headerTextSecondary = new TextView(activity);
                switch (type){
                    case "group": headerTextSecondary.setText(exam.getString("teacher")); break;
                    case "teacher": headerTextSecondary.setText(exam.getString("group")); break;
                }
                headerTextSecondary.setTypeface(null, Typeface.BOLD);
                headerTextSecondary.setLayoutParams(new LinearLayout.LayoutParams(LinearLayout.LayoutParams.MATCH_PARENT, LinearLayout.LayoutParams.WRAP_CONTENT));
                headerTextSecondary.setTextColor(MainActivity.textColorSecondary);
                headerContainer.addView(headerTextSecondary);
                examContainer.addView(headerContainer);
                // информация о консультации
                if(exam.has("consult") && exam.getJSONObject("consult").has("date")) {
                    // separator
                    View separator = new View(activity);
                    separator.setLayoutParams(new LinearLayout.LayoutParams(LinearLayout.LayoutParams.MATCH_PARENT, (int) (1 * destiny)));
                    separator.setBackgroundColor(MainActivity.colorSeparator);
                    examContainer.addView(separator);
                    // информация о консультации
                    examContainer.addView(
                            getUnit(
                                    activity.getString(R.string.consult).toUpperCase(),
                                    (exam.getJSONObject("consult").getString("date") + " " + exam.getJSONObject("consult").getString("time")).trim(),
                                    Objects.equals(exam.getJSONObject("consult").getString("room"), "") ? "" : activity.getString(R.string.place) + ": " + exam.getJSONObject("consult").getString("room")
                            )
                    );
                }
                // информация об экзамене
                if(exam.has("exam") && exam.getJSONObject("exam").has("date")) {
                    // separator
                    View separator = new View(activity);
                    lp = new LinearLayout.LayoutParams(LinearLayout.LayoutParams.MATCH_PARENT, (int) (1 * destiny));
                    lp.setMargins((int) (12 * destiny), 0, (int) (12 * destiny), 0);
                    separator.setLayoutParams(lp);
                    separator.setBackgroundColor(MainActivity.colorSeparator);
                    examContainer.addView(separator);
                    // информация об экзамене
                    examContainer.addView(
                            getUnit(
                                    activity.getString(R.string.exam).toUpperCase(),
                                    (exam.getJSONObject("exam").getString("date") + " " + exam.getJSONObject("exam").getString("time")).trim(),
                                    Objects.equals(exam.getJSONObject("exam").getString("room"), "") ? "" : activity.getString(R.string.place) + ": " + exam.getJSONObject("exam").getString("room")
                            )
                    );
                }
                // контекстное меню
                if((exam.has("teacher") && !Objects.equals(exam.getString("teacher"), "")) || (exam.has("group") && !Objects.equals(exam.getString("group"), ""))) {
                    examContainer.setTag(R.id.schedule_lessons_teacher, exam.has("teacher") ? exam.getString("teacher") : "");
                    examContainer.setTag(R.id.schedule_lessons_group, exam.has("group") ? exam.getString("group") : "");
                    examContainer.setOnCreateContextMenuListener(new View.OnCreateContextMenuListener() {
                        @Override
                        public void onCreateContextMenu(ContextMenu menu, View v, ContextMenu.ContextMenuInfo menuInfo) {
                            menu.setHeaderTitle(R.string.open_schedule);
                            String teacher = v.getTag(R.id.schedule_lessons_teacher).toString();
                            String group = v.getTag(R.id.schedule_lessons_group).toString();
                            if(!Objects.equals(group, "")) menu.add(activity.getString(R.string.group) + " " + group);
                            if(!Objects.equals(teacher, "")) menu.add(teacher);
                        }
                    });
                }
                container.addView(examContainer);
            }
            if(schedule.length() == 0) container.addView(getEmptyScreen());
            delegate.state(STATE_DONE, container);
        } catch (Exception e){
            if(LoginActivity.errorTracker != null) LoginActivity.errorTracker.add(e);
            delegate.state(STATE_FAILED, container);
        }
    }

    private LinearLayout getLoadingScreen() throws Exception {
        LinearLayout loadingLayout = new LinearLayout(activity);
        loadingLayout.setOrientation(LinearLayout.VERTICAL);
        loadingLayout.setLayoutParams(new LinearLayout.LayoutParams(LinearLayout.LayoutParams.MATCH_PARENT, LinearLayout.LayoutParams.MATCH_PARENT));
        loadingLayout.setPadding(0, (int) (24 * destiny), 0, (int) (24 * destiny));
        ProgressBar progressBar = new ProgressBar(activity);
        LinearLayout.LayoutParams lp = new LinearLayout.LayoutParams(LinearLayout.LayoutParams.WRAP_CONTENT, LinearLayout.LayoutParams.WRAP_CONTENT);
        lp.gravity = Gravity.CENTER;
        progressBar.setLayoutParams(lp);
        loadingLayout.addView(progressBar);
        return loadingLayout;
    }
    private LinearLayout getEmptyScreen() throws Exception {
        LinearLayout emptyLayout = new LinearLayout(activity);
        emptyLayout.setOrientation(LinearLayout.VERTICAL);
        emptyLayout.setLayoutParams(new LinearLayout.LayoutParams(LinearLayout.LayoutParams.MATCH_PARENT, LinearLayout.LayoutParams.WRAP_CONTENT));
        TextView textView = new TextView(activity);
        textView.setText(R.string.no_exams);
        textView.setLayoutParams(new LinearLayout.LayoutParams(LinearLayout.LayoutParams.MATCH_PARENT, LinearLayout.LayoutParams.WRAP_CONTENT));
        textView.setTextColor(MainActivity.textColorPrimary);
        textView.setGravity(Gravity.CENTER);
        textView.setHeight((int) (60 * destiny));
        emptyLayout.addView(textView);
        return emptyLayout;
    }

    private LinearLayout getUnit(String title, String text1, String text2) throws Exception {
        LinearLayout examInfoContainer = new LinearLayout(activity);
        examInfoContainer.setOrientation(LinearLayout.VERTICAL);
        examInfoContainer.setLayoutParams(new LinearLayout.LayoutParams(LinearLayout.LayoutParams.MATCH_PARENT, LinearLayout.LayoutParams.WRAP_CONTENT));
        examInfoContainer.setPadding((int) (12 * destiny), (int) (10 * destiny), (int) (12 * destiny), (int) (10 * destiny));
        TextView examInfoHeader = new TextView(activity);
        examInfoHeader.setText(title);
        examInfoHeader.setTextSize(13);
        examInfoHeader.setLayoutParams(new LinearLayout.LayoutParams(LinearLayout.LayoutParams.MATCH_PARENT, LinearLayout.LayoutParams.WRAP_CONTENT));
        examInfoHeader.setTextColor(MainActivity.textColorPrimary);
        examInfoContainer.addView(examInfoHeader);
        LinearLayout examInfo = new LinearLayout(activity);
        examInfo.setOrientation(LinearLayout.HORIZONTAL);
        examInfo.setLayoutParams(new LinearLayout.LayoutParams(LinearLayout.LayoutParams.MATCH_PARENT, LinearLayout.LayoutParams.WRAP_CONTENT));
        TextView examInfo1 = new TextView(activity);
        examInfo1.setText(text1);
        LinearLayout.LayoutParams lp1 = new LinearLayout.LayoutParams(LinearLayout.LayoutParams.WRAP_CONTENT, LinearLayout.LayoutParams.WRAP_CONTENT);
        lp1.weight = 1;
        examInfo1.setLayoutParams(lp1);
        examInfo1.setTextColor(MainActivity.textColorPrimary);
        examInfo.addView(examInfo1);
        TextView examInfo2 = new TextView(activity);
        examInfo2.setText(text2);
        LinearLayout.LayoutParams lp2 = new LinearLayout.LayoutParams(LinearLayout.LayoutParams.WRAP_CONTENT, LinearLayout.LayoutParams.WRAP_CONTENT);
        lp2.weight = 20;
        examInfo2.setLayoutParams(lp2);
        examInfo2.setTextColor(MainActivity.textColorSecondary);
        examInfo.addView(examInfo2);
        examInfoContainer.addView(examInfo);
        return examInfoContainer;
    }
}
package com.bukhmastov.cdoitmo;

import android.app.Activity;
import android.content.Context;
import android.content.Intent;
import android.content.SharedPreferences;
import android.os.Bundle;
import android.preference.PreferenceManager;
import android.support.annotation.NonNull;
import android.support.design.widget.Snackbar;
import android.support.v4.app.Fragment;
import android.support.v4.widget.SwipeRefreshLayout;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.AdapterView;
import android.widget.ArrayAdapter;
import android.widget.ListView;
import android.widget.Spinner;
import android.widget.TextView;

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

public class ERegisterFragment extends Fragment implements SwipeRefreshLayout.OnRefreshListener {

    private static final String TAG = "ERegisterFragment";
    public static ERegister eRegister = null;
    private String group = "";
    private int term = 0;
    private boolean spinner_group_blocker = true, spinner_period_blocker = true;
    private boolean notifyAboutDateUpdate = false;
    private boolean loaded = false;
    private RequestHandle fragmentRequestHandle = null;

    @Override
    public void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        eRegister = new ERegister(getActivity());
    }

    @Override
    public View onCreateView(LayoutInflater inflater, ViewGroup container, Bundle savedInstanceState) {
        return inflater.inflate(R.layout.fragment_eregister, container, false);
    }

    @Override
    public void onResume() {
        super.onResume();
        if (!loaded) {
            loaded = true;
            SharedPreferences sharedPreferences = PreferenceManager.getDefaultSharedPreferences(getActivity());
            if (sharedPreferences.getBoolean("pref_use_cache", true) && sharedPreferences.getBoolean("pref_force_load", true)) {
                forceLoad();
            } else {
                load();
            }
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
    public void onRefresh() {
        forceLoad();
    }

    private void load(){
        if (eRegister.is()) {
            display();
        } else {
            forceLoad();
        }
    }
    private void forceLoad(){
        notifyAboutDateUpdate = true;
        if (!MainActivity.OFFLINE_MODE) {
            DeIfmoRestClient.getJSON(getContext(), "api/private/eregister", null, new DeIfmoRestClientJsonResponseHandler() {
                @Override
                public void onSuccess(int statusCode, JSONObject response) {
                    if (statusCode == 200) {
                        eRegister.put(response);
                        display();
                    } else {
                        if (eRegister.is()) {
                            display();
                        } else {
                            loadFailed();
                        }
                    }
                }
                @Override
                public void onProgress(int state) {
                    draw(R.layout.state_loading);
                    TextView loading_message = (TextView) getActivity().findViewById(R.id.loading_message);
                    if (loading_message != null) {
                        switch (state) {
                            case DeIfmoRestClient.STATE_HANDLING: loading_message.setText(R.string.loading); break;
                            case DeIfmoRestClient.STATE_AUTHORIZATION: loading_message.setText(R.string.authorization); break;
                            case DeIfmoRestClient.STATE_AUTHORIZED: loading_message.setText(R.string.authorized); break;
                        }
                    }
                }
                @Override
                public void onFailure(int state) {
                    switch (state) {
                        case DeIfmoRestClient.FAILED_OFFLINE:
                            if (eRegister.is()) {
                                display();
                            } else {
                                draw(R.layout.state_offline);
                                View offline_reload = getActivity().findViewById(R.id.offline_reload);
                                if (offline_reload != null) {
                                    offline_reload.setOnClickListener(new View.OnClickListener() {
                                        @Override
                                        public void onClick(View v) {
                                            forceLoad();
                                        }
                                    });
                                }
                            }
                            break;
                        case DeIfmoRestClient.FAILED_TRY_AGAIN:
                        case DeIfmoRestClient.FAILED_AUTH_TRY_AGAIN:
                            draw(R.layout.state_try_again);
                            if (state == DeIfmoRestClient.FAILED_AUTH_TRY_AGAIN) {
                                TextView try_again_message = (TextView) getActivity().findViewById(R.id.try_again_message);
                                if (try_again_message != null) try_again_message.setText(R.string.auth_failed);
                            }
                            View try_again_reload = getActivity().findViewById(R.id.try_again_reload);
                            if (try_again_reload != null) {
                                try_again_reload.setOnClickListener(new View.OnClickListener() {
                                    @Override
                                    public void onClick(View v) {
                                        forceLoad();
                                    }
                                });
                            }
                            break;
                        case DeIfmoRestClient.FAILED_AUTH_CREDENTIALS_REQUIRED: gotoLogin(LoginActivity.SIGNAL_CREDENTIALS_REQUIRED); break;
                        case DeIfmoRestClient.FAILED_AUTH_CREDENTIALS_FAILED: gotoLogin(LoginActivity.SIGNAL_CREDENTIALS_FAILED); break;
                    }
                }
                @Override
                public void onNewHandle(RequestHandle requestHandle) {
                    fragmentRequestHandle = requestHandle;
                }
            });
        } else {
            if(eRegister.is()){
                display();
            } else {
                try {
                    draw(R.layout.state_offline);
                    View offline_reload = getActivity().findViewById(R.id.offline_reload);
                    if (offline_reload != null) {
                        offline_reload.setOnClickListener(new View.OnClickListener() {
                            @Override
                            public void onClick(View v) {
                                forceLoad();
                            }
                        });
                    }
                } catch (Exception e) {
                    if(LoginActivity.errorTracker != null) LoginActivity.errorTracker.add(e);
                }
            }
        }
    }
    private void loadFailed(){
        try {
            draw(R.layout.state_try_again);
            TextView try_again_message = (TextView) getActivity().findViewById(R.id.try_again_message);
            if (try_again_message != null) try_again_message.setText(R.string.load_failed_retry_in_minute);
            View try_again_reload = getActivity().findViewById(R.id.try_again_reload);
            if (try_again_reload != null) {
                try_again_reload.setOnClickListener(new View.OnClickListener() {
                    @Override
                    public void onClick(View v) {
                        forceLoad();
                    }
                });
            }
        } catch (Exception e) {
            if(LoginActivity.errorTracker != null) LoginActivity.errorTracker.add(e);
        }
    }
    private void display(){
        try {
            ParsedERegister parsedERegister = eRegister.get();
            if (parsedERegister == null) throw new NullPointerException("parsedERegister cannot be null");
            checkData(parsedERegister);
            // получаем список предметов для отображения
            final ArrayList<HashMap<String, String>> subjects = new ArrayList<>();
            for (Group group : parsedERegister.groups) {
                if (Objects.equals(group.name, this.group)) {
                    for (Term term : group.terms) {
                        if (this.term == -1 || this.term == term.number) {
                            for (Subject subject : term.subjects) {
                                HashMap<String, String> subj = new HashMap<>();
                                subj.put("group", group.name);
                                subj.put("name", subject.name);
                                subj.put("semester", String.valueOf(term.number));
                                subj.put("value", String.valueOf(subject.currentPoints));
                                subj.put("type", String.valueOf(subject.type));
                                subjects.add(subj);
                            }
                        }
                    }
                    break;
                }
            }
            // отображаем интерфейс
            draw(R.layout.eregister_layout);
            // работаем со списком
            ListView erl_list_view = (ListView) getActivity().findViewById(R.id.erl_list_view);
            if (erl_list_view != null) {
                erl_list_view.setAdapter(new SubjectListView(getActivity(), subjects));
                erl_list_view.setOnItemClickListener(new AdapterView.OnItemClickListener() {
                    public void onItemClick(AdapterView<?> parent, View view, int position, long id) {
                        HashMap<String, String> subj = subjects.get(position);
                        Intent intent = new Intent(getActivity(), SubjectActivity.class);
                        intent.putExtra("group", subj.get("group"));
                        intent.putExtra("term", subj.get("semester"));
                        intent.putExtra("name", subj.get("name"));
                        startActivity(intent);
                    }
                });
            }
            // работаем со свайпом
            SwipeRefreshLayout mSwipeRefreshLayout = (SwipeRefreshLayout) getActivity().findViewById(R.id.swipe_container);
            if (mSwipeRefreshLayout != null) {
                mSwipeRefreshLayout.setColorSchemeColors(MainActivity.colorAccent);
                mSwipeRefreshLayout.setProgressBackgroundColorSchemeColor(MainActivity.colorBackgroundRefresh);
                mSwipeRefreshLayout.setOnRefreshListener(this);
            }
            // работаем с раскрывающимися списками
            int selection = 0, counter = 0;
            // список групп
            Spinner spinner_group = (Spinner) getActivity().findViewById(R.id.erl_group_spinner);
            if (spinner_group != null) {
                final ArrayList<String> spinner_group_arr = new ArrayList<>();
                final ArrayList<String> spinner_group_arr_names = new ArrayList<>();
                for (Group group : parsedERegister.groups) {
                    spinner_group_arr.add(group.name + " (" + group.year[0] + "/" + group.year[1] + ")");
                    spinner_group_arr_names.add(group.name);
                    if (Objects.equals(group.name, this.group)) selection = counter;
                    counter++;
                }
                spinner_group.setAdapter(new ArrayAdapter<>(getActivity(), R.layout.spinner_layout, spinner_group_arr));
                spinner_group.setSelection(selection);
                spinner_group_blocker = true;
                spinner_group.setOnItemSelectedListener(new AdapterView.OnItemSelectedListener() {
                    public void onItemSelected(AdapterView<?> parent, View item, int position, long selectedId) {
                        if (spinner_group_blocker) {
                            spinner_group_blocker = false;
                            return;
                        }
                        group = spinner_group_arr_names.get(position);
                        load();
                    }
                    public void onNothingSelected(AdapterView<?> parent) {}
                });
            }
            // список семестров
            Spinner spinner_period = (Spinner) getActivity().findViewById(R.id.erl_period_spinner);
            if (spinner_period != null) {
                final ArrayList<String> spinner_period_arr = new ArrayList<>();
                final ArrayList<Integer> spinner_period_arr_values = new ArrayList<>();
                selection = 2;
                for (Group group : parsedERegister.groups) {
                    if (Objects.equals(group.name, this.group)) {
                        spinner_period_arr.add(group.terms.get(0).number + " " + getString(R.string.semester));
                        spinner_period_arr.add(group.terms.get(1).number + " " + getString(R.string.semester));
                        spinner_period_arr.add(getString(R.string.year));
                        spinner_period_arr_values.add(group.terms.get(0).number);
                        spinner_period_arr_values.add(group.terms.get(1).number);
                        spinner_period_arr_values.add(-1);
                        if (this.term == group.terms.get(0).number) selection = 0;
                        if (this.term == group.terms.get(1).number) selection = 1;
                        break;
                    }
                }
                spinner_period.setAdapter(new ArrayAdapter<>(getActivity(), R.layout.spinner_layout, spinner_period_arr));
                spinner_period.setSelection(selection);
                spinner_period_blocker = true;
                spinner_period.setOnItemSelectedListener(new AdapterView.OnItemSelectedListener() {
                    public void onItemSelected(AdapterView<?> parent, View item, int position, long selectedId) {
                        if (spinner_period_blocker) {
                            spinner_period_blocker = false;
                            return;
                        }
                        term = spinner_period_arr_values.get(position);
                        load();
                    }
                    public void onNothingSelected(AdapterView<?> parent) {
                    }
                });
            }
            // показываем снекбар с датой обновления
            if (notifyAboutDateUpdate) {
                int shift = (int) ((Calendar.getInstance().getTimeInMillis() - parsedERegister.timestamp) / 1000);
                String message;
                if (shift < 21600) {
                    if (shift < 5) {
                        message = "только что";
                    } else if (shift < 60) {
                        message = shift + " " + "сек. назад";
                    } else if (shift < 3600) {
                        message = shift / 60 + " " + "мин. назад";
                    } else {
                        message = shift / 3600 + " " + "час. назад";
                    }
                } else {
                    message = parsedERegister.date;
                }
                View eregister_layout = getActivity().findViewById(R.id.eregister_layout);
                if (eregister_layout != null) {
                    Snackbar snackbar = Snackbar.make(eregister_layout, getString(R.string.update_date) + " " + message, Snackbar.LENGTH_SHORT);
                    snackbar.getView().setBackgroundColor(MainActivity.colorBackgroundSnackBar);
                    snackbar.show();
                }
                notifyAboutDateUpdate = false;
            }
        } catch (Exception e){
            if(LoginActivity.errorTracker != null) LoginActivity.errorTracker.add(e);
            loadFailed();
        }
    }
    private void checkData(ParsedERegister parsedERegister){
        Calendar now = Calendar.getInstance();
        int year = now.get(Calendar.YEAR);
        int month = now.get(Calendar.MONTH);
        String currentGroup = "";
        int currentTerm = -1, maxYear = 0;
        for(Group group : parsedERegister.groups){
            if (!Objects.equals(this.group, "") && Objects.equals(this.group, group.name)) { // мы нашли назначенную группу
                this.group = group.name;
                // теперь проверяем семестр
                boolean isTermOk = false;
                for (Term term : group.terms) {
                    if (this.term != -1 && this.term == term.number) { // мы нашли семестр в найденной группе
                        this.term = term.number;
                        isTermOk = true;
                        break;
                    }
                }
                // семестр неверен, выбираем весь год
                if(!isTermOk) this.term = -1;
                break;
            } else { // группа до сих пор не найдена
                if (Objects.equals(currentGroup, "")) {
                    if (year == group.year[month > Calendar.AUGUST ? 0 : 1]) {
                        currentGroup = group.name;
                        currentTerm = group.terms.get(month > Calendar.AUGUST || month == Calendar.JANUARY ? 0 : 1).number;
                    }
                }
                if (maxYear < group.year[0]) maxYear = group.year[0];
            }
        }
        if (Objects.equals(this.group, "")) {
            if (!Objects.equals(currentGroup, "")) {
                this.group = currentGroup;
                this.term = currentTerm;
            } else {
                for(Group group : parsedERegister.groups){
                    if(group.year[0] == maxYear){
                        this.group = group.name;
                        break;
                    }
                }
                this.term = -1;
            }
        }
    }
    void gotoLogin(int state){
        LoginActivity.state = state;
        getActivity().finish();
    }
    private void draw(int layoutId){
        try {
            ViewGroup vg = ((ViewGroup) getActivity().findViewById(R.id.container_eregister));
            if (vg != null) {
                vg.removeAllViews();
                vg.addView(((LayoutInflater) getActivity().getSystemService(Context.LAYOUT_INFLATER_SERVICE)).inflate(layoutId, null), 0, new ViewGroup.LayoutParams(ViewGroup.LayoutParams.MATCH_PARENT, ViewGroup.LayoutParams.MATCH_PARENT));
            }
        } catch (Exception e){
            if(LoginActivity.errorTracker != null) LoginActivity.errorTracker.add(e);
        }
    }
}

class ERegister {

    private static final String TAG = "ERegister";
    private Context context;
    private ParsedERegister parsedERegister = null;

    ERegister(Context context){
        this.context = context;
        String eRegister = Cache.get(context, "ERegister");
        if(!Objects.equals(eRegister, "")){
            try {
                parse(new JSONObject(eRegister));
            } catch (Exception e) {
                if(LoginActivity.errorTracker != null) LoginActivity.errorTracker.add(e);
            }
        }
    }
    void put(JSONObject data){
        try {
            JSONObject json = new JSONObject();
            json.put("timestamp", Calendar.getInstance().getTimeInMillis());
            json.put("eregister", data);
            parse(json);
            Cache.put(context, "ERegister", json.toString());
        } catch (Exception e) {
            if(LoginActivity.errorTracker != null) LoginActivity.errorTracker.add(e);
        }
    }
    ParsedERegister get(){
        return parsedERegister;
    }
    boolean is(){
        return this.parsedERegister != null;
    }
    private void parse(JSONObject json){
        if(json == null){
            parsedERegister = null;
        } else {
            try {
                parsedERegister = new ParsedERegister();
                parsedERegister.timestamp = json.getLong("timestamp");
                parsedERegister.date = new SimpleDateFormat("dd.MM.yyyy HH:mm", Locale.ROOT).format(new Date(parsedERegister.timestamp));
                JSONArray years = json.getJSONObject("eregister").getJSONArray("years");
                for(int i = 0; i < years.length(); i++){
                    JSONObject groupData = years.getJSONObject(i);
                    Group group = new Group();
                    group.name = groupData.getString("group");
                    String studyyear = groupData.getString("studyyear");
                    int year1 = Integer.parseInt(studyyear.split("/")[0]);
                    int year2 = Integer.parseInt(studyyear.split("/")[1]);
                    if(year1 < year2){
                        group.year[0] = year1;
                        group.year[1] = year2;
                    } else {
                        group.year[0] = year2;
                        group.year[1] = year1;
                    }
                    Term firstTerm = new Term();
                    Term secondTerm = new Term();
                    int t1 = 0, t2 = 0;
                    JSONArray subjects = groupData.getJSONArray("subjects");
                    for(int j = 0; j < subjects.length(); j++){
                        JSONObject subjectData = subjects.getJSONObject(j);
                        int term = Integer.parseInt(subjectData.getString("semester"));
                        if(t1 == 0){
                            t1 = term;
                        }
                        if(t1 != term){
                            t2 = term;
                        }
                        if(t1 != 0 && t2 != 0){
                            firstTerm.number = Math.min(t1, t2);
                            secondTerm.number = Math.max(t1, t2);
                            break;
                        }
                    }
                    for(int j = 0; j < subjects.length(); j++){
                        JSONObject subjectData = subjects.getJSONObject(j);
                        Subject subject = new Subject();
                        subject.name = subjectData.getString("name");
                        subject.currentPoints = -1.0;
                        if(subjectData.has("points")){
                            JSONArray pointsARR = subjectData.getJSONArray("points");
                            for (int k = 0; k < pointsARR.length(); k++) {
                                JSONObject point = pointsARR.getJSONObject(k);
                                if(Objects.equals(point.getString("max"), "100")){
                                    subject.currentPoints = Double.parseDouble(point.getString("value").replace(',', '.'));
                                    break;
                                }
                            }
                        }
                        if(subjectData.has("marks")){
                            JSONArray marks = subjectData.getJSONArray("marks");
                            if(marks.length() > 0) {
                                JSONObject marksData = marks.getJSONObject(0);
                                subject.type = marksData.getString("worktype");
                                subject.mark = marksData.getString("mark");
                                subject.markDate = marksData.getString("markdate");
                            }
                        }
                        if(subjectData.has("points")){
                            JSONArray pointsARR = subjectData.getJSONArray("points");
                            for(int k = 0; k < pointsARR.length(); k++){
                                JSONObject pointData = pointsARR.getJSONObject(k);
                                if(pointData.getString("variable").contains("Семестр")) continue;
                                Point point = new Point();
                                point.name = pointData.getString("variable");
                                String value = pointData.getString("value").replace(",", ".");
                                point.value = Objects.equals(value, "") ? 0.0 : Double.parseDouble(value);
                                String limit = pointData.getString("limit").replace(",", ".");
                                point.limit = Objects.equals(limit, "") ? 0.0 : Double.parseDouble(limit);
                                String max = pointData.getString("max").replace(",", ".");
                                point.max = Objects.equals(max, "") ? 0.0 : Double.parseDouble(max);
                                subject.points.add(point);
                            }
                        }
                        int term = Integer.parseInt(subjectData.getString("semester"));
                        if(term == firstTerm.number) firstTerm.subjects.add(subject);
                        if(term == secondTerm.number) secondTerm.subjects.add(subject);
                    }
                    group.terms.add(firstTerm);
                    group.terms.add(secondTerm);
                    parsedERegister.groups.add(group);
                }
            } catch (Exception e) {
                if(LoginActivity.errorTracker != null) LoginActivity.errorTracker.add(e);
                parsedERegister = null;
            }
        }
    }
}
class ParsedERegister {
    String date;
    long timestamp;
    ArrayList<Group> groups = new ArrayList<>();
}
class Group {
    String name;
    int[] year = {0, 0};
    ArrayList<Term> terms = new ArrayList<>();
}
class Term {
    int number;
    ArrayList<Subject> subjects = new ArrayList<>();
}
class Subject {
    String name;
    Double currentPoints;
    String type = "";
    String mark = "";
    String markDate = "";
    ArrayList<Point> points = new ArrayList<>();
}
class Point {
    String name;
    Double value;
    Double limit;
    Double max;
}

class SubjectListView extends ArrayAdapter<HashMap<String, String>> {

    private final Activity context;
    private final ArrayList<HashMap<String, String>> subj;

    SubjectListView(Activity context, ArrayList<HashMap<String, String>> subj) {
        super(context, R.layout.listview_subject, subj);
        this.context = context;
        this.subj = subj;
    }

    @NonNull
    @Override
    public View getView(int position, View view, @NonNull ViewGroup parent) {
        LayoutInflater inflater = context.getLayoutInflater();
        HashMap<String, String> sub = subj.get(position);
        View rowView = inflater.inflate(R.layout.listview_subject, null, true);
        TextView lv_subject_name = ((TextView) rowView.findViewById(R.id.lv_subject_name));
        TextView lv_subject_sem = ((TextView) rowView.findViewById(R.id.lv_subject_sem));
        TextView lv_subject_points = ((TextView) rowView.findViewById(R.id.lv_point_value));
        if (lv_subject_name != null) lv_subject_name.setText(sub.get("name"));
        if (lv_subject_sem != null) lv_subject_sem.setText(sub.get("semester") + " " + context.getString(R.string.semester) + (Objects.equals(sub.get("type"), "") ? "" : " | " + sub.get("type")));
        if (lv_subject_points != null) lv_subject_points.setText(double2string(Double.parseDouble(sub.get("value"))));
        if(Double.parseDouble(sub.get("value")) >= 60.0){
            context.getTheme().resolveAttribute(R.attr.textColorPassed, MainActivity.typedValue, true);
            if (lv_subject_name != null) lv_subject_name.setTextColor(MainActivity.typedValue.data);
            if (lv_subject_sem != null) lv_subject_sem.setTextColor(MainActivity.typedValue.data);
            if (lv_subject_points != null) lv_subject_points.setTextColor(MainActivity.typedValue.data);
        }
        return rowView;
    }

    private String double2string(Double value){
        String valueStr = String.valueOf(value);
        if(value != -1.0){
            if(value == Double.parseDouble(value.intValue() + ".0")){
                valueStr = value.intValue() + "";
            }
        } else {
            valueStr = "";
        }
        return valueStr;
    }
}
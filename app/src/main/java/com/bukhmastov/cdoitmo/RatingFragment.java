package com.bukhmastov.cdoitmo;

import android.app.Activity;
import android.content.Context;
import android.content.Intent;
import android.os.AsyncTask;
import android.os.Bundle;
import android.support.annotation.NonNull;
import android.support.v4.app.Fragment;
import android.support.v4.widget.SwipeRefreshLayout;
import android.util.TypedValue;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.AdapterView;
import android.widget.ArrayAdapter;
import android.widget.ImageButton;
import android.widget.ListView;
import android.widget.Spinner;
import android.widget.TextView;

import com.loopj.android.http.RequestHandle;

import org.htmlcleaner.HtmlCleaner;
import org.htmlcleaner.TagNode;
import org.json.JSONArray;
import org.json.JSONObject;

import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.Calendar;
import java.util.Date;
import java.util.HashMap;
import java.util.List;
import java.util.Locale;
import java.util.Objects;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

public class RatingFragment extends Fragment implements SwipeRefreshLayout.OnRefreshListener {

    private static final String TAG = "RatingFragment";
    private boolean interrupted = false;
    public static Rating rating = null;
    private boolean loaded = false;
    private HashMap<String, Integer> ready;
    private HashMap<String, RequestHandle> fragmentRequestHandle;

    @Override
    public void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        rating = new Rating(getActivity());
        ready = new HashMap<>();
        fragmentRequestHandle = new HashMap<>();
        ready.put("Rating", 0);
        ready.put("RatingList", 0);
        fragmentRequestHandle.put("Rating", null);
        fragmentRequestHandle.put("RatingList", null);
    }

    @Override
    public View onCreateView(LayoutInflater inflater, ViewGroup container, Bundle savedInstanceState) {
        return inflater.inflate(R.layout.fragment_rating, container, false);
    }

    @Override
    public void onResume() {
        super.onResume();
        relaunch();
    }

    @Override
    public void onPause() {
        super.onPause();
        interrupt();
    }

    @Override
    public void onRefresh() {
        forceLoad();
    }

    private void load(){
        draw(R.layout.state_loading);
        loadPart("Rating");
        if(!rating.is("RatingList")) {
            loadPart("RatingList");
        } else {
            ready("RatingList");
        }
    }
    private void forceLoad(){
        draw(R.layout.state_loading);
        loadPart("Rating");
        loadPart("RatingList");
    }
    private void loadPart(String type){
        if(!MainActivity.OFFLINE_MODE) {
            if (Objects.equals(type, "Rating")) {
                DeIfmoRestClient.get(getContext(), "servlet/distributedCDE?Rule=REP_EXECUTE_PRINT&REP_ID=1441", null, new DeIfmoRestClientResponseHandler() {
                    @Override
                    public void onSuccess(int statusCode, String response) {
                        if (statusCode == 200) {
                            new RatingParse(new RatingParse.response() {
                                @Override
                                public void finish(JSONObject json) {
                                    rating.put("Rating", json);
                                    ready("Rating");
                                }
                            }).execute(response);
                        } else {
                            if (rating.is("Rating")) {
                                ready("Rating");
                            } else {
                                failed("Rating");
                            }
                        }
                    }
                    @Override
                    public void onProgress(int state) {}
                    @Override
                    public void onFailure(int state) {
                        if(isLaunched()) {
                            switch (state) {
                                case DeIfmoRestClient.FAILED_AUTH_CREDENTIALS_REQUIRED:
                                    gotoLogin(LoginActivity.SIGNAL_CREDENTIALS_REQUIRED);
                                    break;
                                case DeIfmoRestClient.FAILED_AUTH_CREDENTIALS_FAILED:
                                    gotoLogin(LoginActivity.SIGNAL_CREDENTIALS_FAILED);
                                    break;
                                default:
                                    failed("Rating");
                                    break;
                            }
                        } else {
                            failed("Rating");
                        }
                    }
                    @Override
                    public void onNewHandle(RequestHandle requestHandle) {
                        fragmentRequestHandle.put("Rating", requestHandle);
                    }
                });
            } else {
                DeIfmoRestClient.get(getContext(), "index.php?node=rating", null, new DeIfmoRestClientResponseHandler() {
                    @Override
                    public void onSuccess(int statusCode, String response) {
                        if (statusCode == 200) {
                            new RatingListParse(new RatingListParse.response() {
                                @Override
                                public void finish(JSONObject json) {
                                    rating.put("RatingList", json);
                                    ready("RatingList");
                                }
                            }).execute(response);
                        } else {
                            if (rating.is("RatingList")) {
                                ready("RatingList");
                            } else {
                                failed("RatingList");
                            }
                        }
                    }
                    @Override
                    public void onProgress(int state) {}
                    @Override
                    public void onFailure(int state) {
                        if(isLaunched()) {
                            switch (state) {
                                case DeIfmoRestClient.FAILED_AUTH_CREDENTIALS_REQUIRED:
                                    gotoLogin(LoginActivity.SIGNAL_CREDENTIALS_REQUIRED);
                                    break;
                                case DeIfmoRestClient.FAILED_AUTH_CREDENTIALS_FAILED:
                                    gotoLogin(LoginActivity.SIGNAL_CREDENTIALS_FAILED);
                                    break;
                                default:
                                    failed("RatingList");
                                    break;
                            }
                        } else {
                            failed("RatingList");
                        }
                    }
                    @Override
                    public void onNewHandle(RequestHandle requestHandle) {
                        fragmentRequestHandle.put("RatingList", requestHandle);
                    }
                });
            }
        } else {
            if (rating.is(type)) {
                ready(type);
            } else {
                failed(type);
            }
        }
    }
    private void failed(String type){
        ready.put(type, 1);
        if(ready.get("Rating") != 0 && ready.get("RatingList") != 0) display();
    }
    private void ready(String type){
        ready.put(type, 2);
        if(ready.get("Rating") != 0 && ready.get("RatingList") != 0) display();
    }
    private void loadFailed(){
        try {
            if(isLaunched()) {
                draw(R.layout.state_try_again);
                getActivity().findViewById(R.id.try_again_reload).setOnClickListener(new View.OnClickListener() {
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
            if(!isLaunched()) return;
            final JSONObject dataR = rating.get("Rating");
            final JSONObject dataRL = rating.get("RatingList");
            // отображаем интерфейс
            draw(R.layout.rating_layout);
            if(dataR == null){
                ViewGroup vg = ((ViewGroup) getActivity().findViewById(R.id.swipe_container));
                vg.removeAllViews();
                vg.addView(((LayoutInflater) getActivity().getSystemService(Context.LAYOUT_INFLATER_SERVICE)).inflate(R.layout.state_failed_compact, null), 0, new ViewGroup.LayoutParams(ViewGroup.LayoutParams.MATCH_PARENT, ViewGroup.LayoutParams.MATCH_PARENT));
            } else {
                // получаем список для отображения
                final ArrayList<HashMap<String, String>> courses = new ArrayList<>();
                JSONArray jsonArray = dataR.getJSONObject("rating").getJSONArray("courses");
                for(int i = 0; i < jsonArray.length(); i++){
                    JSONObject jsonObject = jsonArray.getJSONObject(i);
                    HashMap<String, String> hashMap = new HashMap<>();
                    hashMap.put("name", jsonObject.getString("faculty") + " — " + jsonObject.getInt("course") + " " + getString(R.string.course));
                    hashMap.put("position", jsonObject.getString("position"));
                    hashMap.put("faculty", jsonObject.getString("faculty"));
                    hashMap.put("course", String.valueOf(jsonObject.getInt("course")));
                    courses.add(hashMap);
                }
                // работаем со списком
                ListView rl_list_view = (ListView) getActivity().findViewById(R.id.rl_list_view);
                rl_list_view.setAdapter(new RatingListView(getActivity(), courses));
                rl_list_view.setOnItemClickListener(new AdapterView.OnItemClickListener() {
                    public void onItemClick(AdapterView<?> parent, View view, int position, long id) {
                        try {
                            HashMap<String, String> hashMap = courses.get(position);
                            JSONArray array = dataRL.getJSONObject("rating").getJSONArray("faculties");
                            int max_course = dataR.getJSONObject("rating").getInt("max_course");
                            for(int i = 0; i < array.length(); i++){
                                JSONObject obj = array.getJSONObject(i);
                                if(obj.getString("name").contains(hashMap.get("faculty"))){
                                    int course_delta = (max_course - Integer.parseInt(hashMap.get("course")));
                                    Calendar now = Calendar.getInstance();
                                    int year = now.get(Calendar.YEAR) - course_delta;
                                    int month = now.get(Calendar.MONTH);
                                    Intent intent = new Intent(getActivity(), RatingListActivity.class);
                                    intent.putExtra("faculty", obj.getString("depId"));
                                    intent.putExtra("course", hashMap.get("course"));
                                    intent.putExtra("years", (month > Calendar.AUGUST ? year + "/" + (year + 1) : (year - 1) + "/" + year));
                                    startActivity(intent);
                                    break;
                                }
                            }
                        } catch (Exception e) {
                            if(LoginActivity.errorTracker != null) LoginActivity.errorTracker.add(e);
                        }
                    }
                });
            }
            // работаем со свайпом
            SwipeRefreshLayout mSwipeRefreshLayout = (SwipeRefreshLayout) getActivity().findViewById(R.id.swipe_container);
            TypedValue typedValue = new TypedValue();
            getActivity().getTheme().resolveAttribute(R.attr.colorAccent, typedValue, true);
            mSwipeRefreshLayout.setColorSchemeColors(typedValue.data);
            getActivity().getTheme().resolveAttribute(R.attr.colorBackgroundRefresh, typedValue, true);
            mSwipeRefreshLayout.setProgressBackgroundColorSchemeColor(typedValue.data);
            mSwipeRefreshLayout.setOnRefreshListener(this);
            if(dataRL == null){
                ViewGroup vg = ((ViewGroup) getActivity().findViewById(R.id.rl_list_container));
                vg.removeAllViews();
                vg.addView(((LayoutInflater) getActivity().getSystemService(Context.LAYOUT_INFLATER_SERVICE)).inflate(R.layout.state_failed_compact, null), 0, new ViewGroup.LayoutParams(ViewGroup.LayoutParams.MATCH_PARENT, ViewGroup.LayoutParams.MATCH_PARENT));
            } else {
                ArrayAdapter<String> adapter;
                int choose;
                String cache;
                // работаем с выбором факультета
                Spinner rl_spinner_faculty = (Spinner) getActivity().findViewById(R.id.rl_spinner_faculty);
                final ArrayList<String> rl_spinner_faculty_arr = new ArrayList<>();
                final ArrayList<String> rl_spinner_faculty_arr_ids = new ArrayList<>();
                JSONArray array = dataRL.getJSONObject("rating").getJSONArray("faculties");
                choose = 0;
                cache = Cache.get(getContext(), "rating_list_choose_faculty");
                for(int i = 0; i < array.length(); i++){
                    JSONObject obj = array.getJSONObject(i);
                    rl_spinner_faculty_arr.add(obj.getString("name"));
                    rl_spinner_faculty_arr_ids.add(obj.getString("depId"));
                    if(Objects.equals(cache, obj.getString("depId"))) choose = i;
                }
                adapter = new ArrayAdapter<>(getActivity(), R.layout.spinner_rating_layout, rl_spinner_faculty_arr);
                adapter.setDropDownViewResource(R.layout.spinner_rating_dropdown_layout);
                rl_spinner_faculty.setAdapter(adapter);
                rl_spinner_faculty.setSelection(choose);
                rl_spinner_faculty.setOnItemSelectedListener(new AdapterView.OnItemSelectedListener() {
                    public void onItemSelected(AdapterView<?> parent, View item, int position, long selectedId) {
                        Cache.put(getContext(), "rating_list_choose_faculty", rl_spinner_faculty_arr_ids.get(position));
                    }
                    public void onNothingSelected(AdapterView<?> parent) {}
                });
                // работаем с выбором курса
                Spinner rl_spinner_course = (Spinner) getActivity().findViewById(R.id.rl_spinner_course);
                final ArrayList<String> rl_spinner_course_arr = new ArrayList<>();
                final ArrayList<String> rl_spinner_course_arr_ids = new ArrayList<>();
                choose = 0;
                cache = Cache.get(getContext(), "rating_list_choose_course");
                for(int i = 1; i <= 4; i++){
                    rl_spinner_course_arr.add(i + " " + getString(R.string.course));
                    rl_spinner_course_arr_ids.add(String.valueOf(i));
                    if(Objects.equals(cache, String.valueOf(i))) choose = i - 1;
                }
                adapter = new ArrayAdapter<>(getActivity(), R.layout.spinner_rating_layout, rl_spinner_course_arr);
                adapter.setDropDownViewResource(R.layout.spinner_rating_dropdown_layout);
                rl_spinner_course.setAdapter(adapter);
                rl_spinner_course.setSelection(choose);
                rl_spinner_course.setOnItemSelectedListener(new AdapterView.OnItemSelectedListener() {
                    public void onItemSelected(AdapterView<?> parent, View item, int position, long selectedId) {
                        Cache.put(getContext(), "rating_list_choose_course", rl_spinner_course_arr_ids.get(position));
                    }
                    public void onNothingSelected(AdapterView<?> parent) {}
                });
                // инициализируем кнопку
                ImageButton rl_button = (ImageButton) getActivity().findViewById(R.id.rl_button);
                rl_button.setOnClickListener(new View.OnClickListener() {
                    @Override
                    public void onClick(View v) {
                        Intent intent = new Intent(getActivity(), RatingListActivity.class);
                        intent.putExtra("faculty", Cache.get(getContext(), "rating_list_choose_faculty"));
                        intent.putExtra("course", Cache.get(getContext(), "rating_list_choose_course"));
                        startActivity(intent);
                    }
                });
            }
        } catch (Exception e) {
            if(LoginActivity.errorTracker != null) LoginActivity.errorTracker.add(e);
            loadFailed();
        }
    }
    private void gotoLogin(int state){
        LoginActivity.state = state;
        getActivity().finish();
    }
    private void draw(int layoutId){
        try {
            if(isLaunched()) {
                ViewGroup vg = ((ViewGroup) getActivity().findViewById(R.id.container_rating));
                vg.removeAllViews();
                vg.addView(((LayoutInflater) getActivity().getSystemService(Context.LAYOUT_INFLATER_SERVICE)).inflate(layoutId, null), 0, new ViewGroup.LayoutParams(ViewGroup.LayoutParams.MATCH_PARENT, ViewGroup.LayoutParams.MATCH_PARENT));
            }
        } catch (Exception e){
            if(LoginActivity.errorTracker != null) LoginActivity.errorTracker.add(e);
        }
    }
    private void interrupt(){
        interrupted = true;
        if(fragmentRequestHandle.get("Rating") != null){
            loaded = false;
            fragmentRequestHandle.get("Rating").cancel(true);
        }
        if(fragmentRequestHandle.get("RatingList") != null){
            loaded = false;
            fragmentRequestHandle.get("RatingList").cancel(true);
        }
    }
    private void relaunch(){
        interrupted = false;
        if(!loaded) {
            loaded = true;
            load();
        }
    }
    private boolean isLaunched(){
        return !interrupted;
    }
}

class Rating {

    private static final String TAG = "Rating";
    private Context context;
    private JSONObject rating = null;
    private JSONObject ratingList = null;

    Rating(Context context){
        this.context = context;
        String protocol;
        protocol = Cache.get(context, "Rating");
        if(!Objects.equals(protocol, "")){
            try {
                this.rating = new JSONObject(protocol);
            } catch (Exception e) {
                if(LoginActivity.errorTracker != null) LoginActivity.errorTracker.add(e);
            }
        }
        protocol = Cache.get(context, "RatingList");
        if(!Objects.equals(protocol, "")){
            try {
                this.ratingList = new JSONObject(protocol);
            } catch (Exception e) {
                if(LoginActivity.errorTracker != null) LoginActivity.errorTracker.add(e);
            }
        }
    }
    void put(String type, JSONObject data){
        try {
            JSONObject json = new JSONObject();
            json.put("timestamp", Calendar.getInstance().getTimeInMillis());
            json.put("date", new SimpleDateFormat("dd.MM.yyyy HH:mm", Locale.ROOT).format(new Date(Calendar.getInstance().getTimeInMillis())));
            json.put("rating", data);
            if(Objects.equals(type, "Rating")){
                rating = json;
            } else {
                ratingList = json;
            }
            Cache.put(context, type, json.toString());
        } catch (Exception e) {
            if(LoginActivity.errorTracker != null) LoginActivity.errorTracker.add(e);
        }
    }
    JSONObject get(String type){
        if(Objects.equals(type, "Rating")){
            return rating;
        } else {
            return ratingList;
        }
    }
    boolean is(String type){
        if(Objects.equals(type, "Rating")){
            return this.rating != null;
        } else {
            return this.ratingList != null;
        }
    }
}

class RatingParse extends AsyncTask<String, Void, JSONObject> {
    interface response {
        void finish(JSONObject json);
    }
    private response delegate = null;
    RatingParse(response delegate){
        this.delegate = delegate;
    }
    @Override
    protected JSONObject doInBackground(String... params) {
        try {
            String response = params[0].replace("&nbsp;", " ");
            HtmlCleaner cleaner = new HtmlCleaner();
            TagNode root = cleaner.clean(response);
            JSONObject json = new JSONObject();
            TagNode div = root.findElementByAttValue("class", "d_text", true, false);
            TagNode table = div.getElementListByAttValue("class", "d_table", true, false).get(1);
            List<? extends TagNode> rows = table.getAllElementsList(false).get(0).getAllElementsList(false);
            int max_course = 1;
            JSONArray courses = new JSONArray();
            for(TagNode row : rows){
                if(row.getText().toString().contains("Позиция")) continue;
                List<? extends TagNode> columns = row.getAllElementsList(false);
                JSONObject course = new JSONObject();
                course.put("faculty", columns.get(0).getText().toString().trim());
                int course_value = Integer.parseInt(columns.get(1).getText().toString().trim());
                if(course_value > max_course) max_course = course_value;
                course.put("course", course_value);
                course.put("position", columns.get(2).getText().toString().trim());
                courses.put(course);
            }
            json.put("courses", courses);
            json.put("max_course", max_course);
            return json;
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
class RatingListParse extends AsyncTask<String, Void, JSONObject> {
    interface response {
        void finish(JSONObject json);
    }
    private response delegate = null;
    RatingListParse(response delegate){
        this.delegate = delegate;
    }
    @Override
    protected JSONObject doInBackground(String... params) {
        try {
            String response = params[0].replace("&nbsp;", " ");
            HtmlCleaner cleaner = new HtmlCleaner();
            TagNode root = cleaner.clean(response);
            TagNode div = root.findElementByAttValue("class", "c-page", true, false).findElementByAttValue("class", "p-inner nobt", false, false);
            TagNode[] spans = div.getElementsByName("span", false);
            JSONArray faculties = new JSONArray();
            for(TagNode span : spans){
                JSONObject faculty = new JSONObject();
                String name = span.getText().toString().trim();
                Matcher matcher = Pattern.compile("^(.*) \\((.{1,10})\\)$").matcher(name);
                if(matcher.find()) name = matcher.group(2) + " (" + matcher.group(1) + ")";
                faculty.put("name", name);
                faculties.put(faculty);
            }
            TagNode[] links = div.getElementsByAttValue("class", "big-links left", false, false);
            for(int i = 0; i < faculties.length(); i++) {
                TagNode link = links[i];
                TagNode[] as = link.getElementsByName("a", false);
                if(as != null && as.length > 0){
                    String[] attrs = as[0].getAttributeByName("href").replace("&amp;", "&").split("&");
                    for(String attr : attrs){
                        String[] pair = attr.split("=");
                        if(Objects.equals(pair[0], "depId")) faculties.getJSONObject(i).put("depId", pair[1]);
                    }
                }
            }
            JSONObject json = new JSONObject();
            json.put("faculties", faculties);
            return json;
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

class RatingListView extends ArrayAdapter<HashMap<String, String>> {

    private final Activity context;
    private final ArrayList<HashMap<String, String>> courses;

    RatingListView(Activity context, ArrayList<HashMap<String, String>> courses) {
        super(context, R.layout.listview_rating, courses);
        this.context = context;
        this.courses = courses;
    }

    @NonNull
    @Override
    public View getView(int position, View view, @NonNull ViewGroup parent) {
        LayoutInflater inflater = context.getLayoutInflater();
        HashMap<String, String> change = courses.get(position);
        View rowView;
        rowView = inflater.inflate(R.layout.listview_rating, null, true);
        TextView lv_rating_name = ((TextView) rowView.findViewById(R.id.lv_rating_name));
        TextView lv_rating_position = ((TextView) rowView.findViewById(R.id.lv_rating_position));
        lv_rating_name.setText(change.get("name"));
        lv_rating_position.setText(change.get("position"));
        return rowView;
    }
}
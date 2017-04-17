package com.bukhmastov.cdoitmo.fragments;

import android.content.Context;
import android.content.Intent;
import android.os.Bundle;
import android.support.v4.app.Fragment;
import android.support.v4.widget.SwipeRefreshLayout;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.AdapterView;
import android.widget.ArrayAdapter;
import android.widget.ImageButton;
import android.widget.ListView;
import android.widget.Spinner;

import com.bukhmastov.cdoitmo.R;
import com.bukhmastov.cdoitmo.activities.LoginActivity;
import com.bukhmastov.cdoitmo.activities.RatingListActivity;
import com.bukhmastov.cdoitmo.adapters.RatingListView;
import com.bukhmastov.cdoitmo.network.DeIfmoClient;
import com.bukhmastov.cdoitmo.network.interfaces.DeIfmoClientResponseHandler;
import com.bukhmastov.cdoitmo.objects.Rating;
import com.bukhmastov.cdoitmo.parse.RatingListParse;
import com.bukhmastov.cdoitmo.parse.RatingParse;
import com.bukhmastov.cdoitmo.utils.Log;
import com.bukhmastov.cdoitmo.utils.Static;
import com.bukhmastov.cdoitmo.utils.Storage;
import com.loopj.android.http.RequestHandle;

import org.json.JSONArray;
import org.json.JSONObject;

import java.util.ArrayList;
import java.util.Calendar;
import java.util.HashMap;
import java.util.Objects;

public class RatingFragment extends Fragment implements SwipeRefreshLayout.OnRefreshListener {

    private static final String TAG = "RatingFragment";
    public static Rating rating = null;
    private boolean loaded = false;
    private HashMap<String, Integer> ready;
    private HashMap<String, RequestHandle> fragmentRequestHandle;
    private String rating_list_choose_faculty, rating_list_choose_course;

    @Override
    public void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        Log.v(TAG, "Fragment created");
        rating = new Rating(getActivity());
        ready = new HashMap<>();
        fragmentRequestHandle = new HashMap<>();
        ready.put("Rating", 0);
        ready.put("RatingList", 0);
        fragmentRequestHandle.put("Rating", null);
        fragmentRequestHandle.put("RatingList", null);
        rating_list_choose_faculty = Storage.file.cache.get(getContext(), "rating#choose#faculty");
        rating_list_choose_course = Storage.file.cache.get(getContext(), "rating#choose#course");
    }

    @Override
    public void onDestroy() {
        super.onDestroy();
        Log.v(TAG, "Fragment destroyed");
    }

    @Override
    public View onCreateView(LayoutInflater inflater, ViewGroup container, Bundle savedInstanceState) {
        return inflater.inflate(R.layout.fragment_rating, container, false);
    }

    @Override
    public void onResume() {
        super.onResume();
        Log.v(TAG, "resumed");
        if (!loaded) {
            loaded = true;
            load();
        }
    }

    @Override
    public void onPause() {
        super.onPause();
        Log.v(TAG, "paused");
        if (fragmentRequestHandle.get("Rating") != null) {
            loaded = false;
            fragmentRequestHandle.get("Rating").cancel(true);
        }
        if (fragmentRequestHandle.get("RatingList") != null) {
            loaded = false;
            fragmentRequestHandle.get("RatingList").cancel(true);
        }
    }

    @Override
    public void onRefresh() {
        Log.v(TAG, "refreshed");
        forceLoad();
    }

    private void load(){
        load(Storage.pref.get(getContext(), "pref_use_cache", true) ? Integer.parseInt(Storage.pref.get(getContext(), "pref_tab_refresh", "0")) : 0);
    }
    private void load(int refresh_rate){
        draw(R.layout.state_loading);
        load(refresh_rate, "Rating");
        load(refresh_rate, "RatingList");
    }
    private void load(int refresh_rate, String type){
        Log.v(TAG, "load | refresh_rate=" + refresh_rate + " | type=" + type);
        if (!rating.is(type) || refresh_rate == 0) {
            loadPart(type);
        } else if (refresh_rate >= 0){
            JSONObject rating = RatingFragment.rating.get(type);
            try {
                if (rating == null) throw new Exception("rating is null");
                if (rating.getLong("timestamp") + refresh_rate * 3600000L < Calendar.getInstance().getTimeInMillis()) {
                    loadPart(type);
                } else {
                    ready(type);
                }
            } catch (Exception e) {
                Static.error(e);
                loadPart(type);
            }
        } else {
            ready(type);
        }
    }
    private void forceLoad(){
        Log.v(TAG, "forceLoad");
        draw(R.layout.state_loading);
        loadPart("Rating");
        loadPart("RatingList");
    }
    private void loadPart(final String type){
        Log.v(TAG, "loadPart | type=" + type);
        if (!Static.OFFLINE_MODE) {
            if (Objects.equals(type, "Rating")) {
                DeIfmoClient.get(getContext(), "servlet/distributedCDE?Rule=REP_EXECUTE_PRINT&REP_ID=1441", null, new DeIfmoClientResponseHandler() {
                    @Override
                    public void onSuccess(int statusCode, String response) {
                        Log.v(TAG, "loadPart | type=" + type + " | success | statusCode=" + statusCode + " | response=" + (response == null ? "null" : "notnull"));
                        if (statusCode == 200) {
                            new RatingParse(new RatingParse.response() {
                                @Override
                                public void finish(JSONObject json) {
                                    if (json != null) {
                                        rating.put("Rating", json);
                                        ready("Rating");
                                    } else {
                                        if (rating.is("Rating")) {
                                            ready("Rating");
                                        } else {
                                            failed("Rating");
                                        }
                                    }
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
                    public void onProgress(int state) {
                        Log.v(TAG, "loadPart | type=" + type + " | progress " + state);
                    }
                    @Override
                    public void onFailure(int state) {
                        Log.v(TAG, "loadPart | type=" + type + " | failure " + state);
                        switch (state) {
                            case DeIfmoClient.FAILED_AUTH_CREDENTIALS_REQUIRED: gotoLogin(LoginActivity.SIGNAL_CREDENTIALS_REQUIRED); break;
                            case DeIfmoClient.FAILED_AUTH_CREDENTIALS_FAILED: gotoLogin(LoginActivity.SIGNAL_CREDENTIALS_FAILED); break;
                            default: failed("Rating"); break;
                        }
                    }
                    @Override
                    public void onNewHandle(RequestHandle requestHandle) {
                        fragmentRequestHandle.put("Rating", requestHandle);
                    }
                });
            } else {
                DeIfmoClient.get(getContext(), "index.php?node=rating", null, new DeIfmoClientResponseHandler() {
                    @Override
                    public void onSuccess(int statusCode, String response) {
                        Log.v(TAG, "loadPart | type=" + type + " | success | statusCode=" + statusCode + " | response=" + (response == null ? "null" : "notnull"));
                        if (statusCode == 200) {
                            new RatingListParse(new RatingListParse.response() {
                                @Override
                                public void finish(JSONObject json) {
                                    if (json != null) {
                                        rating.put("RatingList", json);
                                        ready("RatingList");
                                    } else {
                                        if (rating.is("RatingList")) {
                                            ready("RatingList");
                                        } else {
                                            failed("RatingList");
                                        }
                                    }
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
                    public void onProgress(int state) {
                        Log.v(TAG, "loadPart | type=" + type + " | progress " + state);
                    }
                    @Override
                    public void onFailure(int state) {
                        Log.v(TAG, "loadPart | type=" + type + " | failure " + state);
                        failed("RatingList");
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
        Log.v(TAG, "failed | type=" + type);
        ready.put(type, 1);
        if (ready.get("Rating") != 0 && ready.get("RatingList") != 0) display();
    }
    private void ready(String type){
        Log.v(TAG, "ready | type=" + type);
        ready.put(type, 2);
        if (ready.get("Rating") != 0 && ready.get("RatingList") != 0) display();
    }
    private void loadFailed(){
        Log.v(TAG, "loadFailed");
        try {
            draw(R.layout.state_try_again);
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
            Static.error(e);
        }
    }
    private void display(){
        Log.v(TAG, "display");
        try {
            final JSONObject dataR = rating.get("Rating");
            final JSONObject dataRL = rating.get("RatingList");
            // отображаем интерфейс
            draw(R.layout.rating_layout);
            if(dataR == null){
                ViewGroup vg = ((ViewGroup) getActivity().findViewById(R.id.swipe_container));
                if (vg != null) {
                    vg.removeAllViews();
                    vg.addView(((LayoutInflater) getActivity().getSystemService(Context.LAYOUT_INFLATER_SERVICE)).inflate(R.layout.state_failed_compact, null), 0, new ViewGroup.LayoutParams(ViewGroup.LayoutParams.MATCH_PARENT, ViewGroup.LayoutParams.MATCH_PARENT));
                }
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
                if (rl_list_view != null) {
                    rl_list_view.setAdapter(new RatingListView(getActivity(), courses));
                    rl_list_view.setOnItemClickListener(new AdapterView.OnItemClickListener() {
                        public void onItemClick(AdapterView<?> parent, View view, int position, long id) {
                            Log.v(TAG, "rl_list_view clicked");
                            try {
                                HashMap<String, String> hashMap = courses.get(position);
                                JSONArray array = dataRL.getJSONObject("rating").getJSONArray("faculties");
                                int max_course = dataR.getJSONObject("rating").getInt("max_course");
                                for (int i = 0; i < array.length(); i++) {
                                    JSONObject obj = array.getJSONObject(i);
                                    if (obj.getString("name").contains(hashMap.get("faculty"))) {
                                        int course_delta = (max_course - Integer.parseInt(hashMap.get("course")));
                                        Calendar now = Calendar.getInstance();
                                        int year = now.get(Calendar.YEAR) - course_delta;
                                        int month = now.get(Calendar.MONTH);
                                        String years = month > Calendar.AUGUST ? year + "/" + (year + 1) : (year - 1) + "/" + year;
                                        Intent intent = new Intent(getActivity(), RatingListActivity.class);
                                        intent.putExtra("faculty", obj.getString("depId"));
                                        intent.putExtra("course", hashMap.get("course"));
                                        intent.putExtra("years", years);
                                        startActivity(intent);
                                        Log.v(TAG, "rl_list_view clicked and found | faculty=" + obj.getString("depId") + " | course=" + hashMap.get("course") + " | years=" + years);
                                        break;
                                    }
                                }
                            } catch (Exception e) {
                                Static.error(e);
                            }
                        }
                    });
                }
            }
            // работаем со свайпом
            SwipeRefreshLayout mSwipeRefreshLayout = (SwipeRefreshLayout) getActivity().findViewById(R.id.swipe_container);
            if (mSwipeRefreshLayout != null) {
                mSwipeRefreshLayout.setColorSchemeColors(Static.colorAccent);
                mSwipeRefreshLayout.setProgressBackgroundColorSchemeColor(Static.colorBackgroundRefresh);
                mSwipeRefreshLayout.setOnRefreshListener(this);
            }
            if(dataRL == null){
                ViewGroup vg = ((ViewGroup) getActivity().findViewById(R.id.rl_list_container));
                if (vg != null) {
                    vg.removeAllViews();
                    vg.addView(((LayoutInflater) getActivity().getSystemService(Context.LAYOUT_INFLATER_SERVICE)).inflate(R.layout.state_failed_compact, null), 0, new ViewGroup.LayoutParams(ViewGroup.LayoutParams.MATCH_PARENT, ViewGroup.LayoutParams.MATCH_PARENT));
                }
            } else {
                ArrayAdapter<String> adapter;
                int choose;
                // работаем с выбором факультета
                Spinner rl_spinner_faculty = (Spinner) getActivity().findViewById(R.id.rl_spinner_faculty);
                if (rl_spinner_faculty != null) {
                    final ArrayList<String> rl_spinner_faculty_arr = new ArrayList<>();
                    final ArrayList<String> rl_spinner_faculty_arr_ids = new ArrayList<>();
                    JSONArray array = dataRL.getJSONObject("rating").getJSONArray("faculties");
                    choose = 0;
                    for (int i = 0; i < array.length(); i++) {
                        JSONObject obj = array.getJSONObject(i);
                        rl_spinner_faculty_arr.add(obj.getString("name"));
                        rl_spinner_faculty_arr_ids.add(obj.getString("depId"));
                        if (Objects.equals(rating_list_choose_faculty, obj.getString("depId"))) choose = i;
                    }
                    adapter = new ArrayAdapter<>(getActivity(), R.layout.spinner_rating_layout, rl_spinner_faculty_arr);
                    adapter.setDropDownViewResource(R.layout.spinner_rating_dropdown_layout);
                    rl_spinner_faculty.setAdapter(adapter);
                    rl_spinner_faculty.setSelection(choose);
                    rl_spinner_faculty.setOnItemSelectedListener(new AdapterView.OnItemSelectedListener() {
                        public void onItemSelected(AdapterView<?> parent, View item, int position, long selectedId) {
                            rating_list_choose_faculty = rl_spinner_faculty_arr_ids.get(position);
                            Log.v(TAG, "rl_spinner_faculty clicked | rating_list_choose_faculty=" + rating_list_choose_faculty);
                            Storage.file.cache.put(getContext(), "rating#choose#faculty", rating_list_choose_faculty);
                        }
                        public void onNothingSelected(AdapterView<?> parent) {}
                    });
                }
                // работаем с выбором курса
                Spinner rl_spinner_course = (Spinner) getActivity().findViewById(R.id.rl_spinner_course);
                if (rl_spinner_course != null) {
                    final ArrayList<String> rl_spinner_course_arr = new ArrayList<>();
                    final ArrayList<String> rl_spinner_course_arr_ids = new ArrayList<>();
                    choose = 0;
                    for (int i = 1; i <= 4; i++) {
                        rl_spinner_course_arr.add(i + " " + getString(R.string.course));
                        rl_spinner_course_arr_ids.add(String.valueOf(i));
                        if (Objects.equals(rating_list_choose_course, String.valueOf(i))) choose = i - 1;
                    }
                    adapter = new ArrayAdapter<>(getActivity(), R.layout.spinner_rating_layout, rl_spinner_course_arr);
                    adapter.setDropDownViewResource(R.layout.spinner_rating_dropdown_layout);
                    rl_spinner_course.setAdapter(adapter);
                    rl_spinner_course.setSelection(choose);
                    rl_spinner_course.setOnItemSelectedListener(new AdapterView.OnItemSelectedListener() {
                        public void onItemSelected(AdapterView<?> parent, View item, int position, long selectedId) {
                            rating_list_choose_course = rl_spinner_course_arr_ids.get(position);
                            Log.v(TAG, "rl_spinner_course clicked | rating_list_choose_course=" + rating_list_choose_course);
                            Storage.file.cache.put(getContext(), "rating#choose#course", rating_list_choose_course);
                        }
                        public void onNothingSelected(AdapterView<?> parent) {}
                    });
                }
                // инициализируем кнопку
                ImageButton rl_button = (ImageButton) getActivity().findViewById(R.id.rl_button);
                if (rl_button != null) {
                    rl_button.setOnClickListener(new View.OnClickListener() {
                        @Override
                        public void onClick(View v) {
                            Intent intent = new Intent(getActivity(), RatingListActivity.class);
                            intent.putExtra("faculty", rating_list_choose_faculty);
                            intent.putExtra("course", rating_list_choose_course);
                            startActivity(intent);
                            Log.v(TAG, "rl_button clicked | faculty=" + rating_list_choose_faculty + " | course=" + rating_list_choose_course);
                        }
                    });
                }
            }
        } catch (Exception e) {
            Static.error(e);
            loadFailed();
        }
    }
    private void gotoLogin(int state){
        Log.v(TAG, "gotoLogin | state=" + state);
        Intent intent = new Intent(getContext(), LoginActivity.class);
        intent.putExtra("state", state);
        startActivity(intent);
    }
    private void draw(int layoutId){
        try {
            ViewGroup vg = ((ViewGroup) getActivity().findViewById(R.id.container_rating));
            if (vg != null) {
                vg.removeAllViews();
                vg.addView(((LayoutInflater) getActivity().getSystemService(Context.LAYOUT_INFLATER_SERVICE)).inflate(layoutId, null), 0, new ViewGroup.LayoutParams(ViewGroup.LayoutParams.MATCH_PARENT, ViewGroup.LayoutParams.MATCH_PARENT));
            }
        } catch (Exception e){
            Static.error(e);
        }
    }
}
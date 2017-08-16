package com.bukhmastov.cdoitmo.fragments;

import android.content.Context;
import android.content.Intent;
import android.os.Bundle;
import android.support.v4.widget.SwipeRefreshLayout;
import android.util.ArrayMap;
import android.view.InflateException;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.AdapterView;
import android.widget.ArrayAdapter;
import android.widget.ImageButton;
import android.widget.ListView;
import android.widget.Spinner;
import android.widget.TextView;

import com.bukhmastov.cdoitmo.R;
import com.bukhmastov.cdoitmo.activities.LoginActivity;
import com.bukhmastov.cdoitmo.adapters.RatingListView;
import com.bukhmastov.cdoitmo.firebase.FirebaseAnalyticsProvider;
import com.bukhmastov.cdoitmo.network.DeIfmoClient;
import com.bukhmastov.cdoitmo.network.interfaces.DeIfmoClientResponseHandler;
import com.bukhmastov.cdoitmo.parse.RatingListParse;
import com.bukhmastov.cdoitmo.parse.RatingParse;
import com.bukhmastov.cdoitmo.utils.Log;
import com.bukhmastov.cdoitmo.utils.Static;
import com.bukhmastov.cdoitmo.utils.Storage;
import com.loopj.android.http.RequestHandle;

import org.json.JSONArray;
import org.json.JSONException;
import org.json.JSONObject;

import java.util.ArrayList;
import java.util.Calendar;
import java.util.HashMap;
import java.util.Objects;

public class RatingFragment extends ConnectedFragment implements SwipeRefreshLayout.OnRefreshListener {

    private static final String TAG = "RatingFragment";
    private enum TYPE {common, own}
    private enum STATUS {empty, loaded, failed, offline}
    private ArrayMap<TYPE, Info> data = new ArrayMap<>();
    private class Info {
        public STATUS status = STATUS.empty;
        public JSONObject data = null;
        public Info(STATUS status, JSONObject data) {
            this.status = status;
            this.data = data;
        }
    }
    private String rating_list_choose_faculty, rating_list_choose_course;
    private boolean loaded = false;
    private RequestHandle fragmentRequestHandle = null;

    @Override
    public void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        Log.v(TAG, "Fragment created");
        FirebaseAnalyticsProvider.logCurrentScreen(activity, this);
        data.put(TYPE.common, new Info(STATUS.empty, null));
        data.put(TYPE.own,    new Info(STATUS.empty, null));
        rating_list_choose_faculty = Storage.file.cache.get(activity, "rating#choose#faculty");
        rating_list_choose_course = Storage.file.cache.get(activity, "rating#choose#course");
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
        FirebaseAnalyticsProvider.setCurrentScreen(activity, this);
        if (!loaded) {
            loaded = true;
            load();
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
    public void onRefresh() {
        Log.v(TAG, "refreshed");
        load(TYPE.common, true);
    }

    private void load() {
        Static.T.runThread(new Runnable() {
            @Override
            public void run() {
                load(TYPE.common);
            }
        });
    }
    private void load(final TYPE type) {
        Static.T.runThread(new Runnable() {
            @Override
            public void run() {
                switch (type) {
                    case common: {
                        load(type, Storage.pref.get(activity, "pref_use_cache", true) ? Integer.parseInt(Storage.pref.get(activity, "pref_static_refresh", "168")) : 0);
                        break;
                    }
                    case own: {
                        load(type, Storage.pref.get(activity, "pref_use_cache", true) ? Integer.parseInt(Storage.pref.get(activity, "pref_dynamic_refresh", "0")) : 0);
                        break;
                    }
                }
            }
        });
    }
    private void load(final TYPE type, final int refresh_rate) {
        Static.T.runThread(new Runnable() {
            @Override
            public void run() {
                Log.v(TAG, "load | type=" + type.toString() + " | refresh_rate=" + refresh_rate);
                if (Storage.pref.get(activity, "pref_use_cache", true)) {
                    String cache = "";
                    switch (type) {
                        case common: {
                            cache = Storage.file.cache.get(activity, "rating#list").trim();
                            break;
                        }
                        case own: {
                            cache = Storage.file.cache.get(activity, "rating#core").trim();
                            break;
                        }
                    }
                    if (!cache.isEmpty()) {
                        try {
                            data.get(type).data = new JSONObject(cache);
                            if (data.get(type).data.getLong("timestamp") + refresh_rate * 3600000L < Calendar.getInstance().getTimeInMillis()) {
                                load(type, true, cache);
                            } else {
                                load(type, false, cache);
                            }
                        } catch (JSONException e) {
                            Static.error(e);
                            load(type, true, cache);
                        }
                    } else {
                        load(type, false);
                    }
                } else {
                    load(type, false);
                }
            }
        });
    }
    private void load(final TYPE type, final boolean force) {
        Static.T.runThread(new Runnable() {
            @Override
            public void run() {
                load(type, force, "");
            }
        });
    }
    private void load(final TYPE type, final boolean force, final String cache) {
        Static.T.runOnUiThread(new Runnable() {
            @Override
            public void run() {
                draw(R.layout.state_loading);
                if (activity != null) {
                    TextView loading_message = (TextView) activity.findViewById(R.id.loading_message);
                    if (loading_message != null) {
                        loading_message.setText(R.string.loading);
                    }
                }
            }
        });
        Static.T.runThread(new Runnable() {
            @Override
            public void run() {
                Log.v(TAG, "load | type=" + type.toString() + " | force=" + (force ? "true" : "false"));
                if ((!force || !Static.isOnline(activity)) && Storage.pref.get(activity, "pref_use_cache", true)) {
                    try {
                        String c = "";
                        if (cache.isEmpty()) {
                            switch (type) {
                                case common: {
                                    c = Storage.file.cache.get(activity, "rating#list").trim();
                                    break;
                                }
                                case own: {
                                    c = Storage.file.cache.get(activity, "rating#core").trim();
                                    break;
                                }
                            }
                        } else {
                            c = cache;
                        }
                        if (!c.isEmpty()) {
                            Log.v(TAG, "load | type=" + type.toString() + " | from cache");
                            data.put(type, new Info(STATUS.loaded, new JSONObject(c)));
                            loaded(type);
                            return;
                        }
                    } catch (Exception e) {
                        Log.v(TAG, "load | type=" + type.toString() + " | failed to load from cache");
                        switch (type) {
                            case common: {
                                Storage.file.cache.delete(activity, "rating#list");
                                break;
                            }
                            case own: {
                                Storage.file.cache.delete(activity, "rating#core");
                                break;
                            }
                        }
                    }
                }
                if (!Static.OFFLINE_MODE) {
                    String url = "";
                    switch (type) {
                        case common: {
                            url = "index.php?node=rating";
                            break;
                        }
                        case own: {
                            url = "servlet/distributedCDE?Rule=REP_EXECUTE_PRINT&REP_ID=1441";
                            break;
                        }
                    }
                    DeIfmoClient.get(activity, url, null, new DeIfmoClientResponseHandler() {
                        @Override
                        public void onSuccess(int statusCode, String response) {
                            Log.v(TAG, "load | type=" + type + " | success | statusCode=" + statusCode + " | response=" + (response == null ? "null" : "notnull"));
                            if (statusCode == 200) {
                                switch (type) {
                                    case common: {
                                        new RatingListParse(new RatingListParse.response() {
                                            @Override
                                            public void finish(JSONObject json) {
                                                if (json != null) {
                                                    try {
                                                        json = new JSONObject()
                                                                .put("timestamp", Calendar.getInstance().getTimeInMillis())
                                                                .put("rating", json);
                                                        if (Storage.pref.get(activity, "pref_use_cache", true)) {
                                                            Storage.file.cache.put(activity, "rating#list", json.toString());
                                                        }
                                                        data.put(type, new Info(STATUS.loaded, json));
                                                    } catch (JSONException e) {
                                                        Static.error(e);
                                                        if (data.get(type).data != null) {
                                                            data.get(type).status = STATUS.loaded;
                                                            loaded(type);
                                                        } else {
                                                            data.put(type, new Info(STATUS.failed, null));
                                                        }
                                                    }
                                                } else {
                                                    if (data.get(type).data != null) {
                                                        data.get(type).status = STATUS.loaded;
                                                        loaded(type);
                                                    } else {
                                                        data.put(type, new Info(STATUS.failed, null));
                                                    }
                                                }
                                                loaded(type);
                                            }
                                        }).execute(response);
                                        break;
                                    }
                                    case own: {
                                        new RatingParse(new RatingParse.response() {
                                            @Override
                                            public void finish(JSONObject json) {
                                                if (json != null) {
                                                    try {
                                                        json = new JSONObject()
                                                                .put("timestamp", Calendar.getInstance().getTimeInMillis())
                                                                .put("rating", json);
                                                        if (Storage.pref.get(activity, "pref_use_cache", true)) {
                                                            Storage.file.cache.put(activity, "rating#core", json.toString());
                                                        }
                                                        data.put(type, new Info(STATUS.loaded, json));
                                                    } catch (JSONException e) {
                                                        Static.error(e);
                                                        if (data.get(type).data != null) {
                                                            data.get(type).status = STATUS.loaded;
                                                            loaded(type);
                                                        } else {
                                                            data.put(type, new Info(STATUS.failed, null));
                                                        }
                                                    }
                                                } else {
                                                    if (data.get(type).data != null) {
                                                        data.get(type).status = STATUS.loaded;
                                                        loaded(type);
                                                    } else {
                                                        data.put(type, new Info(STATUS.failed, null));
                                                    }
                                                }
                                                loaded(type);
                                            }
                                        }).execute(response);
                                        break;
                                    }
                                }
                            } else {
                                if (data.get(type).data != null) {
                                    data.get(type).status = STATUS.loaded;
                                    loaded(type);
                                } else {
                                    data.put(type, new Info(STATUS.failed, null));
                                    loaded(type);
                                }
                            }
                        }
                        @Override
                        public void onProgress(int state) {
                            Log.v(TAG, "load | type=" + type.toString() + " | progress " + state);
                        }
                        @Override
                        public void onFailure(int statusCode, int state) {
                            Log.v(TAG, "load | type=" + type.toString() + " | failure " + state);
                            switch (state) {
                                case DeIfmoClient.FAILED_AUTH_CREDENTIALS_REQUIRED: {
                                    loaded = false;
                                    gotoLogin(LoginActivity.SIGNAL_CREDENTIALS_REQUIRED);
                                    break;
                                }
                                case DeIfmoClient.FAILED_AUTH_CREDENTIALS_FAILED: {
                                    loaded = false;
                                    gotoLogin(LoginActivity.SIGNAL_CREDENTIALS_FAILED);
                                    break;
                                }
                                default: {
                                    if (data.get(type).data != null) {
                                        data.get(type).status = STATUS.loaded;
                                        loaded(type);
                                    } else {
                                        data.put(type, new Info(STATUS.failed, null));
                                        loaded(type);
                                    }
                                    break;
                                }
                            }
                        }
                        @Override
                        public void onNewHandle(RequestHandle requestHandle) {
                            fragmentRequestHandle = requestHandle;
                        }
                    });
                } else {
                    if (data.get(type).data != null) {
                        data.get(type).status = STATUS.loaded;
                        loaded(type);
                    } else {
                        data.put(type, new Info(STATUS.offline, null));
                        loaded(type);
                    }
                }
            }
        });
    }
    private void loaded(final TYPE type) {
        Static.T.runThread(new Runnable() {
            @Override
            public void run() {
                switch (type) {
                    case common: {
                        load(TYPE.own);
                        break;
                    }
                    case own: {
                        display();
                        break;
                    }
                }
            }
        });
    }
    private void loadFailed() {
        Static.T.runOnUiThread(new Runnable() {
            @Override
            public void run() {
                Log.v(TAG, "loadFailed");
                try {
                    draw(R.layout.state_try_again);
                    View try_again_reload = activity.findViewById(R.id.try_again_reload);
                    if (try_again_reload != null) {
                        try_again_reload.setOnClickListener(new View.OnClickListener() {
                            @Override
                            public void onClick(View v) {
                                load();
                            }
                        });
                    }
                } catch (Exception e) {
                    Static.error(e);
                }
            }
        });
    }

    private void display() {
        final RatingFragment self = this;
        Static.T.runOnUiThread(new Runnable() {
            @Override
            public void run() {
                Log.v(TAG, "display");
                try {
                    // отображаем интерфейс
                    draw(R.layout.rating_layout);
                    // извлекаем информацию
                    final Info common = data.get(TYPE.common);
                    final Info own = data.get(TYPE.own);
                    // подробный рейтинг
                    if (common.status == STATUS.loaded && common.data != null) {
                        ArrayAdapter<String> adapter;
                        int choose;
                        // работаем с выбором факультета
                        Spinner rl_spinner_faculty = (Spinner) activity.findViewById(R.id.rl_spinner_faculty);
                        if (rl_spinner_faculty != null) {
                            final ArrayList<String> rl_spinner_faculty_arr = new ArrayList<>();
                            final ArrayList<String> rl_spinner_faculty_arr_ids = new ArrayList<>();
                            JSONArray array = common.data.getJSONObject("rating").getJSONArray("faculties");
                            choose = 0;
                            for (int i = 0; i < array.length(); i++) {
                                JSONObject obj = array.getJSONObject(i);
                                rl_spinner_faculty_arr.add(obj.getString("name"));
                                rl_spinner_faculty_arr_ids.add(obj.getString("depId"));
                                if (Objects.equals(rating_list_choose_faculty, obj.getString("depId"))) choose = i;
                            }
                            adapter = new ArrayAdapter<>(activity, R.layout.spinner_rating_layout, rl_spinner_faculty_arr);
                            adapter.setDropDownViewResource(R.layout.spinner_rating_dropdown_layout);
                            rl_spinner_faculty.setAdapter(adapter);
                            rl_spinner_faculty.setSelection(choose);
                            rl_spinner_faculty.setOnItemSelectedListener(new AdapterView.OnItemSelectedListener() {
                                public void onItemSelected(AdapterView<?> parent, View item, int position, long selectedId) {
                                    rating_list_choose_faculty = rl_spinner_faculty_arr_ids.get(position);
                                    Log.v(TAG, "rl_spinner_faculty clicked | rating_list_choose_faculty=" + rating_list_choose_faculty);
                                    Storage.file.cache.put(activity, "rating#choose#faculty", rating_list_choose_faculty);
                                }
                                public void onNothingSelected(AdapterView<?> parent) {}
                            });
                        }
                        // работаем с выбором курса
                        Spinner rl_spinner_course = (Spinner) activity.findViewById(R.id.rl_spinner_course);
                        if (rl_spinner_course != null) {
                            final ArrayList<String> rl_spinner_course_arr = new ArrayList<>();
                            final ArrayList<String> rl_spinner_course_arr_ids = new ArrayList<>();
                            choose = 0;
                            for (int i = 1; i <= 4; i++) {
                                rl_spinner_course_arr.add(i + " " + activity.getString(R.string.course));
                                rl_spinner_course_arr_ids.add(String.valueOf(i));
                                if (Objects.equals(rating_list_choose_course, String.valueOf(i))) choose = i - 1;
                            }
                            adapter = new ArrayAdapter<>(activity, R.layout.spinner_rating_layout, rl_spinner_course_arr);
                            adapter.setDropDownViewResource(R.layout.spinner_rating_dropdown_layout);
                            rl_spinner_course.setAdapter(adapter);
                            rl_spinner_course.setSelection(choose);
                            rl_spinner_course.setOnItemSelectedListener(new AdapterView.OnItemSelectedListener() {
                                public void onItemSelected(AdapterView<?> parent, View item, int position, long selectedId) {
                                    rating_list_choose_course = rl_spinner_course_arr_ids.get(position);
                                    Log.v(TAG, "rl_spinner_course clicked | rating_list_choose_course=" + rating_list_choose_course);
                                    Storage.file.cache.put(activity, "rating#choose#course", rating_list_choose_course);
                                }
                                public void onNothingSelected(AdapterView<?> parent) {}
                            });
                        }
                        // инициализируем кнопку
                        ImageButton rl_button = (ImageButton) activity.findViewById(R.id.rl_button);
                        if (rl_button != null) {
                            rl_button.setOnClickListener(new View.OnClickListener() {
                                @Override
                                public void onClick(View v) {
                                    FirebaseAnalyticsProvider.logBasicEvent(activity, "Detailed rating used");
                                    Bundle extras = new Bundle();
                                    extras.putString("faculty", rating_list_choose_faculty);
                                    extras.putString("course", rating_list_choose_course);
                                    activity.openActivityOrFragment(RatingListFragment.class, extras);
                                    Log.v(TAG, "rl_button clicked | faculty=" + rating_list_choose_faculty + " | course=" + rating_list_choose_course);
                                }
                            });
                        }
                    } else {
                        ViewGroup vg = ((ViewGroup) activity.findViewById(R.id.rl_list_container));
                        if (vg != null) {
                            vg.removeAllViews();
                            vg.addView(inflate(common.status == STATUS.offline ? R.layout.state_offline_compact : R.layout.state_failed_compact));
                        }
                    }
                    // ваш рейтинг
                    if (own.status == STATUS.loaded && own.data != null) {
                        // получаем список для отображения
                        final ArrayList<HashMap<String, String>> courses = new ArrayList<>();
                        JSONArray jsonArray = own.data.getJSONObject("rating").getJSONArray("courses");
                        for (int i = 0; i < jsonArray.length(); i++) {
                            JSONObject jsonObject = jsonArray.getJSONObject(i);
                            HashMap<String, String> hashMap = new HashMap<>();
                            hashMap.put("name", jsonObject.getString("faculty") + " — " + jsonObject.getInt("course") + " " + getString(R.string.course));
                            hashMap.put("position", jsonObject.getString("position"));
                            hashMap.put("faculty", jsonObject.getString("faculty"));
                            hashMap.put("course", String.valueOf(jsonObject.getInt("course")));
                            courses.add(hashMap);
                        }
                        // работаем со списком
                        ListView rl_list_view = (ListView) activity.findViewById(R.id.rl_list_view);
                        if (rl_list_view != null) {
                            rl_list_view.setAdapter(new RatingListView(activity, courses));
                            rl_list_view.setOnItemClickListener(new AdapterView.OnItemClickListener() {
                                public void onItemClick(AdapterView<?> parent, View view, int position, long id) {
                                    FirebaseAnalyticsProvider.logBasicEvent(activity, "Own rating used");
                                    Log.v(TAG, "rl_list_view clicked");
                                    try {
                                        if (common.status == STATUS.loaded && common.data != null) {
                                            HashMap<String, String> hashMap = courses.get(position);
                                            JSONArray array = common.data.getJSONObject("rating").getJSONArray("faculties");
                                            int max_course = own.data.getJSONObject("rating").getInt("max_course");
                                            for (int i = 0; i < array.length(); i++) {
                                                JSONObject obj = array.getJSONObject(i);
                                                if (obj.getString("name").contains(hashMap.get("faculty"))) {
                                                    int course_delta = (max_course - Integer.parseInt(hashMap.get("course")));
                                                    Calendar now = Calendar.getInstance();
                                                    int year = now.get(Calendar.YEAR) - course_delta;
                                                    int month = now.get(Calendar.MONTH);
                                                    String years = month > Calendar.AUGUST ? year + "/" + (year + 1) : (year - 1) + "/" + year;
                                                    Bundle extras = new Bundle();
                                                    extras.putString("faculty", obj.getString("depId"));
                                                    extras.putString("course", hashMap.get("course"));
                                                    extras.putString("years", years);
                                                    activity.openActivityOrFragment(RatingListFragment.class, extras);
                                                    Log.v(TAG, "rl_list_view clicked and found | faculty=" + obj.getString("depId") + " | course=" + hashMap.get("course") + " | years=" + years);
                                                    break;
                                                }
                                            }
                                        } else {
                                            Log.v(TAG, "Info.common is null");
                                        }
                                    } catch (Exception e) {
                                        Static.error(e);
                                    }
                                }
                            });
                        }
                    } else {
                        ViewGroup vg = ((ViewGroup) activity.findViewById(R.id.swipe_container));
                        if (vg != null) {
                            vg.removeAllViews();
                            vg.addView(inflate(own.status == STATUS.offline ? R.layout.state_offline_compact : R.layout.state_failed_compact));
                        }
                    }
                    // работаем со свайпом
                    SwipeRefreshLayout mSwipeRefreshLayout = (SwipeRefreshLayout) activity.findViewById(R.id.swipe_container);
                    if (mSwipeRefreshLayout != null) {
                        mSwipeRefreshLayout.setColorSchemeColors(Static.colorAccent);
                        mSwipeRefreshLayout.setProgressBackgroundColorSchemeColor(Static.colorBackgroundRefresh);
                        mSwipeRefreshLayout.setOnRefreshListener(self);
                    }
                } catch (Exception e) {
                    Static.error(e);
                    loadFailed();
                }
            }
        });
    }
    private void gotoLogin(final int state) {
        Static.T.runOnUiThread(new Runnable() {
            @Override
            public void run() {
                try {
                    Log.v(TAG, "gotoLogin | state=" + state);
                    Intent intent = new Intent(activity, LoginActivity.class);
                    intent.putExtra("state", state);
                    startActivity(intent);
                } catch (Exception e) {
                    Static.error(e);
                    loadFailed();
                }
            }
        });
    }

    private void draw(final int layoutId) {
        Static.T.runOnUiThread(new Runnable() {
            @Override
            public void run() {
                try {
                    ViewGroup vg = ((ViewGroup) activity.findViewById(R.id.container_rating));
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
}

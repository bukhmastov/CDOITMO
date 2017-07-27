package com.bukhmastov.cdoitmo.fragments;

import android.content.Context;
import android.os.Bundle;
import android.support.v4.widget.SwipeRefreshLayout;
import android.view.InflateException;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.AdapterView;
import android.widget.ArrayAdapter;
import android.widget.ListView;
import android.widget.Spinner;
import android.widget.TextView;

import com.bukhmastov.cdoitmo.R;
import com.bukhmastov.cdoitmo.adapters.SubjectListView;
import com.bukhmastov.cdoitmo.converters.ERegisterConverter;
import com.bukhmastov.cdoitmo.firebase.FirebaseAnalyticsProvider;
import com.bukhmastov.cdoitmo.network.DeIfmoRestClient;
import com.bukhmastov.cdoitmo.network.interfaces.DeIfmoRestClientResponseHandler;
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

public class ERegisterFragment extends ConnectedFragment implements SwipeRefreshLayout.OnRefreshListener {

    private static final String TAG = "ERegisterFragment";
    public static JSONObject data = null;
    private String group;
    private int term;
    private boolean spinner_group_blocker = true, spinner_period_blocker = true;
    private boolean loaded = false;
    private RequestHandle fragmentRequestHandle = null;

    @Override
    public void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        Log.v(TAG, "Fragment created");
        FirebaseAnalyticsProvider.logCurrentScreen(activity, this);
        group = Storage.file.cache.get(getContext(), "eregister#params#selected_group", "");
        term = -2;
    }

    @Override
    public void onDestroy() {
        super.onDestroy();
        Log.v(TAG, "Fragment destroyed");
    }

    @Override
    public View onCreateView(LayoutInflater inflater, ViewGroup container, Bundle savedInstanceState) {
        return inflater.inflate(R.layout.fragment_eregister, container, false);
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
        load(true);
    }

    private void load() {
        Static.T.runThread(new Runnable() {
            @Override
            public void run() {
                load(Storage.pref.get(getContext(), "pref_use_cache", true) ? Integer.parseInt(Storage.pref.get(getContext(), "pref_dynamic_refresh", "0")) : 0);
            }
        });
    }
    private void load(final int refresh_rate) {
        Static.T.runThread(new Runnable() {
            @Override
            public void run() {
                Log.v(TAG, "load | refresh_rate=" + refresh_rate);
                if (Storage.pref.get(getContext(), "pref_use_cache", true)) {
                    String cache = Storage.file.cache.get(getContext(), "eregister#core").trim();
                    if (!cache.isEmpty()) {
                        try {
                            data = new JSONObject(cache);
                            if (data.getLong("timestamp") + refresh_rate * 3600000L < Calendar.getInstance().getTimeInMillis()) {
                                load(true, cache);
                            } else {
                                load(false, cache);
                            }
                        } catch (JSONException e) {
                            Static.error(e);
                            load(true, cache);
                        }
                    } else {
                        load(false);
                    }
                } else {
                    load(false);
                }
            }
        });
    }
    private void load(final boolean force) {
        Static.T.runThread(new Runnable() {
            @Override
            public void run() {
                load(force, "");
            }
        });
    }
    private void load(final boolean force, final String cache) {
        Static.T.runThread(new Runnable() {
            @Override
            public void run() {
                Log.v(TAG, "load | force=" + (force ? "true" : "false"));
                if ((!force || !Static.isOnline(getContext())) && Storage.pref.get(getContext(), "pref_use_cache", true)) {
                    try {
                        String c = cache.isEmpty() ? Storage.file.cache.get(getContext(), "eregister#core").trim() : cache;
                        if (!c.isEmpty()) {
                            Log.v(TAG, "load | from cache");
                            data = new JSONObject(c);
                            display();
                            return;
                        }
                    } catch (Exception e) {
                        Log.v(TAG, "load | failed to load from cache");
                        Storage.file.cache.delete(getContext(), "eregister#core");
                    }
                }
                if (!Static.OFFLINE_MODE) {
                    DeIfmoRestClient.get(getContext(), "eregister", null, new DeIfmoRestClientResponseHandler() {
                        @Override
                        public void onSuccess(int statusCode, JSONObject responseObj, JSONArray responseArr) {
                            Log.v(TAG, "load | success | statusCode=" + statusCode + " | responseObj=" + (responseObj == null ? "null" : "notnull"));
                            if (statusCode == 200 && responseObj != null) {
                                new ERegisterConverter(new ERegisterConverter.response() {
                                    @Override
                                    public void finish(JSONObject json) {
                                        if (Storage.pref.get(getContext(), "pref_use_cache", true)) {
                                            Storage.file.cache.put(getContext(), "eregister#core", json.toString());
                                        }
                                        data = json;
                                        display();
                                    }
                                }).execute(responseObj);
                            } else {
                                if (data != null) {
                                    display();
                                } else {
                                    loadFailed();
                                }
                            }
                        }
                        @Override
                        public void onProgress(final int state) {
                            Static.T.runOnUiThread(new Runnable() {
                                @Override
                                public void run() {
                                    Log.v(TAG, "load | progress " + state);
                                    draw(R.layout.state_loading);
                                    if (activity != null) {
                                        TextView loading_message = (TextView) activity.findViewById(R.id.loading_message);
                                        if (loading_message != null) {
                                            switch (state) {
                                                case DeIfmoRestClient.STATE_HANDLING: loading_message.setText(R.string.loading); break;
                                            }
                                        }
                                    }
                                }
                            });
                        }
                        @Override
                        public void onFailure(final int statusCode, final int state) {
                            Static.T.runOnUiThread(new Runnable() {
                                @Override
                                public void run() {
                                    Log.v(TAG, "load | failure " + state);
                                    switch (state) {
                                        case DeIfmoRestClient.FAILED_OFFLINE:
                                            if (data != null) {
                                                display();
                                            } else {
                                                draw(R.layout.state_offline);
                                                if (activity != null) {
                                                    View offline_reload = activity.findViewById(R.id.offline_reload);
                                                    if (offline_reload != null) {
                                                        offline_reload.setOnClickListener(new View.OnClickListener() {
                                                            @Override
                                                            public void onClick(View v) {
                                                                load();
                                                            }
                                                        });
                                                    }
                                                }
                                            }
                                            break;
                                        case DeIfmoRestClient.FAILED_TRY_AGAIN:
                                            draw(R.layout.state_try_again);
                                            if (activity != null) {
                                                View try_again_reload = activity.findViewById(R.id.try_again_reload);
                                                if (try_again_reload != null) {
                                                    try_again_reload.setOnClickListener(new View.OnClickListener() {
                                                        @Override
                                                        public void onClick(View v) {
                                                            load();
                                                        }
                                                    });
                                                }
                                            }
                                            break;
                                    }
                                }
                            });
                        }
                        @Override
                        public void onNewHandle(RequestHandle requestHandle) {
                            fragmentRequestHandle = requestHandle;
                        }
                    });
                } else {
                    Static.T.runOnUiThread(new Runnable() {
                        @Override
                        public void run() {
                            if (data != null) {
                                display();
                            } else {
                                draw(R.layout.state_offline);
                                if (activity != null) {
                                    View offline_reload = activity.findViewById(R.id.offline_reload);
                                    if (offline_reload != null) {
                                        offline_reload.setOnClickListener(new View.OnClickListener() {
                                            @Override
                                            public void onClick(View v) {
                                                load();
                                            }
                                        });
                                    }
                                }
                            }
                        }
                    });
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
                    TextView try_again_message = (TextView) activity.findViewById(R.id.try_again_message);
                    if (try_again_message != null) try_again_message.setText(R.string.eregister_load_failed_retry_in_minute);
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
        final ERegisterFragment self = this;
        Static.T.runOnUiThread(new Runnable() {
            @Override
            public void run() {
                Log.v(TAG, "display");
                try {
                    if (data == null) throw new NullPointerException("data cannot be null");
                    checkData(data);
                    // получаем список предметов для отображения
                    final ArrayList<HashMap<String, String>> subjects = new ArrayList<>();
                    JSONArray groups = data.getJSONArray("groups");
                    for (int i = 0; i < groups.length(); i++) {
                        JSONObject group = groups.getJSONObject(i);
                        if (Objects.equals(group.getString("name"), self.group)) {
                            JSONArray terms = group.getJSONArray("terms");
                            for (int j = 0; j < terms.length(); j++) {
                                JSONObject term = terms.getJSONObject(j);
                                if (self.term == -1 || self.term == term.getInt("number")) {
                                    JSONArray subjArr = term.getJSONArray("subjects");
                                    for (int k = 0; k < subjArr.length(); k++) {
                                        JSONObject subj = subjArr.getJSONObject(k);
                                        HashMap<String, String> subjObj = new HashMap<>();
                                        subjObj.put("group", group.getString("name"));
                                        subjObj.put("semester", String.valueOf(term.getInt("number")));
                                        subjObj.put("name", subj.getString("name"));
                                        subjObj.put("type", subj.getString("type"));
                                        subjObj.put("value", String.valueOf(subj.getDouble("currentPoints")));
                                        subjObj.put("mark", subj.getString("mark"));
                                        subjects.add(subjObj);
                                    }
                                }
                            }
                            break;
                        }
                    }
                    // отображаем интерфейс
                    draw(R.layout.eregister_layout);
                    // работаем со списком
                    ListView erl_list_view = (ListView) activity.findViewById(R.id.erl_list_view);
                    if (erl_list_view != null) {
                        erl_list_view.setAdapter(new SubjectListView(activity, subjects));
                        erl_list_view.setOnItemClickListener(new AdapterView.OnItemClickListener() {
                            public void onItemClick(AdapterView<?> parent, View view, int position, long id) {
                                Log.v(TAG, "erl_list_view clicked");
                                HashMap<String, String> subj = subjects.get(position);
                                Bundle extras = new Bundle();
                                extras.putString("group", subj.get("group"));
                                extras.putString("term", subj.get("semester"));
                                extras.putString("name", subj.get("name"));
                                activity.openActivityOrFragment(SubjectShowFragment.class, extras);
                            }
                        });
                    }
                    // работаем со свайпом
                    SwipeRefreshLayout mSwipeRefreshLayout = (SwipeRefreshLayout) activity.findViewById(R.id.swipe_container);
                    if (mSwipeRefreshLayout != null) {
                        mSwipeRefreshLayout.setColorSchemeColors(Static.colorAccent);
                        mSwipeRefreshLayout.setProgressBackgroundColorSchemeColor(Static.colorBackgroundRefresh);
                        mSwipeRefreshLayout.setOnRefreshListener(self);
                    }
                    // работаем с раскрывающимися списками
                    int selection = 0, counter = 0;
                    // список групп
                    Spinner spinner_group = (Spinner) activity.findViewById(R.id.erl_group_spinner);
                    if (spinner_group != null) {
                        final ArrayList<String> spinner_group_arr = new ArrayList<>();
                        final ArrayList<String> spinner_group_arr_names = new ArrayList<>();
                        for (int i = 0; i < groups.length(); i++) {
                            JSONObject group = groups.getJSONObject(i);
                            spinner_group_arr.add(group.getString("name") + " (" + group.getJSONArray("years").getInt(0) + "/" + group.getJSONArray("years").getInt(1) + ")");
                            spinner_group_arr_names.add(group.getString("name"));
                            if (Objects.equals(group.getString("name"), self.group)) selection = counter;
                            counter++;
                        }
                        spinner_group.setAdapter(new ArrayAdapter<>(activity, R.layout.spinner_layout, spinner_group_arr));
                        spinner_group.setSelection(selection);
                        spinner_group_blocker = true;
                        spinner_group.setOnItemSelectedListener(new AdapterView.OnItemSelectedListener() {
                            public void onItemSelected(AdapterView<?> parent, View item, int position, long selectedId) {
                                if (spinner_group_blocker) {
                                    spinner_group_blocker = false;
                                    return;
                                }
                                group = spinner_group_arr_names.get(position);
                                Log.v(TAG, "spinner_group clicked | group=" + group);
                                Storage.file.cache.put(getContext(), "eregister#params#selected_group", group);
                                load(false);
                            }
                            public void onNothingSelected(AdapterView<?> parent) {}
                        });
                    }
                    // список семестров
                    Spinner spinner_period = (Spinner) activity.findViewById(R.id.erl_period_spinner);
                    if (spinner_period != null) {
                        final ArrayList<String> spinner_period_arr = new ArrayList<>();
                        final ArrayList<Integer> spinner_period_arr_values = new ArrayList<>();
                        selection = 2;
                        for (int i = 0; i < groups.length(); i++) {
                            JSONObject group = groups.getJSONObject(i);
                            if (Objects.equals(group.getString("name"), self.group)) {
                                int first = group.getJSONArray("terms").getJSONObject(0).getInt("number");
                                int second = group.getJSONArray("terms").getJSONObject(1).getInt("number");
                                spinner_period_arr.add(first + " " + activity.getString(R.string.semester));
                                spinner_period_arr.add(second + " " + activity.getString(R.string.semester));
                                spinner_period_arr.add(activity.getString(R.string.year));
                                spinner_period_arr_values.add(first);
                                spinner_period_arr_values.add(second);
                                spinner_period_arr_values.add(-1);
                                if (self.term == first) selection = 0;
                                if (self.term == second) selection = 1;
                                break;
                            }
                        }
                        spinner_period.setAdapter(new ArrayAdapter<>(activity, R.layout.spinner_layout, spinner_period_arr));
                        spinner_period.setSelection(selection);
                        spinner_period_blocker = true;
                        spinner_period.setOnItemSelectedListener(new AdapterView.OnItemSelectedListener() {
                            public void onItemSelected(AdapterView<?> parent, View item, int position, long selectedId) {
                                if (spinner_period_blocker) {
                                    spinner_period_blocker = false;
                                    return;
                                }
                                term = spinner_period_arr_values.get(position);
                                Log.v(TAG, "spinner_period clicked | term=" + term);
                                load(false);
                            }
                            public void onNothingSelected(AdapterView<?> parent) {}
                        });
                    }
                    Static.showUpdateTime(activity, data.getLong("timestamp"), true);
                } catch (Exception e) {
                    Static.error(e);
                    loadFailed();
                }
            }
        });
    }
    private void checkData(JSONObject data) throws Exception {
        Log.v(TAG, "checkData");
        Calendar now = Calendar.getInstance();
        int year = now.get(Calendar.YEAR);
        int month = now.get(Calendar.MONTH);
        String currentGroup = "";
        int currentTerm = -1, maxYear = 0;
        JSONArray groups = data.getJSONArray("groups");
        for (int i = 0; i < groups.length(); i++) {
            JSONObject group = groups.getJSONObject(i);
            if (!this.group.isEmpty() && Objects.equals(this.group, group.getString("name"))) { // мы нашли назначенную группу
                this.group = group.getString("name");
                // теперь проверяем семестр
                JSONArray terms = group.getJSONArray("terms");
                boolean isTermOk = false;
                if (this.term == -2) {
                    JSONArray years = group.getJSONArray("years");
                    if (year == years.getInt(month > Calendar.AUGUST ? 0 : 1)) {
                        if (Integer.parseInt(Storage.pref.get(getContext(), "pref_e_journal_term", "0")) == 1) {
                            this.term = -1;
                        } else {
                            this.term = group.getJSONArray("terms").getJSONObject(month > Calendar.AUGUST || month == Calendar.JANUARY ? 0 : 1).getInt("number");
                        }
                    } else {
                        this.term = -1;
                    }
                    isTermOk = true;
                }
                for (int j = 0; j < terms.length(); j++) {
                    JSONObject term = terms.getJSONObject(j);
                    if (this.term != -1 && this.term == term.getInt("number")) { // мы нашли семестр в найденной группе
                        this.term = term.getInt("number");
                        isTermOk = true;
                        break;
                    }
                }
                if (!isTermOk) { // семестр неверен, выбираем весь год
                    this.term = -1;
                }
                break;
            } else { // группа до сих пор не найдена
                JSONArray years = group.getJSONArray("years");
                if (currentGroup.isEmpty()) {
                    if (year == years.getInt(month > Calendar.AUGUST ? 0 : 1)) {
                        currentGroup = group.getString("name");
                        if (Integer.parseInt(Storage.pref.get(getContext(), "pref_e_journal_term", "0")) == 1) {
                            currentTerm = -1;
                        } else {
                            currentTerm = group.getJSONArray("terms").getJSONObject(month > Calendar.AUGUST || month == Calendar.JANUARY ? 0 : 1).getInt("number");
                        }
                    }
                }
                if (maxYear < years.getInt(0)) {
                    maxYear = years.getInt(0);
                }
            }
        }
        if (this.group.isEmpty()) {
            if (!currentGroup.isEmpty()) {
                this.group = currentGroup;
                this.term = currentTerm;
            } else {
                for (int i = 0; i < groups.length(); i++) {
                    JSONObject group = groups.getJSONObject(i);
                    if (group.getJSONArray("years").getInt(0) == maxYear) {
                        this.group = group.getString("name");
                        break;
                    }
                }
                this.term = -1;
            }
        }
    }

    private void draw(final int layoutId) {
        Static.T.runOnUiThread(new Runnable() {
            @Override
            public void run() {
                try {
                    ViewGroup vg = ((ViewGroup) activity.findViewById(R.id.container_eregister));
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

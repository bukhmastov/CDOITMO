package com.bukhmastov.cdoitmo.fragments;

import android.content.Context;
import android.os.Bundle;
import android.support.annotation.NonNull;
import android.support.v4.widget.SwipeRefreshLayout;
import android.support.v7.widget.LinearLayoutManager;
import android.support.v7.widget.RecyclerView;
import android.view.InflateException;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.AdapterView;
import android.widget.ArrayAdapter;
import android.widget.Spinner;
import android.widget.TextView;

import com.bukhmastov.cdoitmo.R;
import com.bukhmastov.cdoitmo.adapters.rva.ERegisterSubjectsRVA;
import com.bukhmastov.cdoitmo.converters.ERegisterConverter;
import com.bukhmastov.cdoitmo.firebase.FirebaseAnalyticsProvider;
import com.bukhmastov.cdoitmo.network.DeIfmoRestClient;
import com.bukhmastov.cdoitmo.network.interfaces.RestResponseHandler;
import com.bukhmastov.cdoitmo.network.models.Client;
import com.bukhmastov.cdoitmo.utils.Log;
import com.bukhmastov.cdoitmo.utils.Static;
import com.bukhmastov.cdoitmo.utils.Storage;

import org.json.JSONArray;
import org.json.JSONObject;

import java.util.ArrayList;
import java.util.Calendar;

public class ERegisterFragment extends ConnectedFragment implements SwipeRefreshLayout.OnRefreshListener {

    private static final String TAG = "ERegisterFragment";
    private JSONObject data = null;
    private String group;
    private int term;
    private boolean spinner_group_blocker = true, spinner_period_blocker = true;
    private boolean loaded = false;
    private Client.Request requestHandle = null;
    protected boolean forbidden = false;

    @Override
    public void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        Log.v(TAG, "Fragment created");
        if (Static.UNAUTHORIZED_MODE) {
            forbidden = true;
            Log.w(TAG, "Fragment created | UNAUTHORIZED_MODE not allowed, closing fragment...");
            close();
            return;
        }
        FirebaseAnalyticsProvider.logCurrentScreen(activity, this);
        group = Storage.file.cache.get(activity, "eregister#params#selected_group", "");
        term = -2;
    }

    @Override
    public void onDestroy() {
        super.onDestroy();
        Log.v(TAG, "Fragment destroyed");
    }

    @Override
    public View onCreateView(@NonNull LayoutInflater inflater, ViewGroup container, Bundle savedInstanceState) {
        return inflater.inflate(R.layout.fragment_eregister, container, false);
    }

    @Override
    public void onResume() {
        super.onResume();
        Log.v(TAG, "resumed");
        if (forbidden) {
            return;
        }
        FirebaseAnalyticsProvider.setCurrentScreen(activity, this);
        if (!loaded) {
            loaded = true;
            if (getData() == null) {
                load();
            } else {
                display();
            }
        }
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
    public void onRefresh() {
        Log.v(TAG, "refreshing");
        load(true);
    }

    private void load() {
        Static.T.runThread(() -> load(Storage.pref.get(activity, "pref_use_cache", true) ? Integer.parseInt(Storage.pref.get(activity, "pref_dynamic_refresh", "0")) : 0));
    }
    private void load(final int refresh_rate) {
        Static.T.runThread(() -> {
            Log.v(TAG, "load | refresh_rate=" + refresh_rate);
            if (Storage.pref.get(activity, "pref_use_cache", true)) {
                String cache = Storage.file.cache.get(activity, "eregister#core").trim();
                if (!cache.isEmpty()) {
                    try {
                        JSONObject data = new JSONObject(cache);
                        setData(data);
                        if (data.getLong("timestamp") + refresh_rate * 3600000L < Static.getCalendar().getTimeInMillis()) {
                            load(true, cache);
                        } else {
                            load(false, cache);
                        }
                    } catch (Exception e) {
                        Log.e(TAG, "load | exception=", e);
                        load(true, cache);
                    }
                } else {
                    load(false);
                }
            } else {
                load(false);
            }
        });
    }
    private void load(final boolean force) {
        Static.T.runThread(() -> load(force, ""));
    }
    private void load(final boolean force, final String cache) {
        Static.T.runThread(() -> {
            Log.v(TAG, "load | force=" + (force ? "true" : "false"));
            if ((!force || !Static.isOnline(activity)) && Storage.pref.get(activity, "pref_use_cache", true)) {
                try {
                    String c = cache.isEmpty() ? Storage.file.cache.get(activity, "eregister#core").trim() : cache;
                    if (!c.isEmpty()) {
                        Log.v(TAG, "load | from cache");
                        setData(new JSONObject(c));
                        display();
                        return;
                    }
                } catch (Exception e) {
                    Log.v(TAG, "load | failed to load from cache");
                    Storage.file.cache.delete(activity, "eregister#core");
                }
            }
            if (!Static.OFFLINE_MODE) {
                DeIfmoRestClient.get(activity, "eregister", null, new RestResponseHandler() {
                    @Override
                    public void onSuccess(final int statusCode, final Client.Headers headers, final JSONObject responseObj, final JSONArray responseArr) {
                        Static.T.runThread(() -> {
                            Log.v(TAG, "load | success | statusCode=" + statusCode + " | responseObj=" + (responseObj == null ? "null" : "notnull"));
                            if (statusCode == 200 && responseObj != null) {
                                new ERegisterConverter(responseObj, json -> {
                                    if (Storage.pref.get(activity, "pref_use_cache", true)) {
                                        Storage.file.cache.put(activity, "eregister#core", json.toString());
                                    }
                                    setData(json);
                                    display();
                                }).run();
                            } else {
                                if (getData() != null) {
                                    display();
                                } else {
                                    loadFailed();
                                }
                            }
                        });
                    }
                    @Override
                    public void onFailure(final int statusCode, final Client.Headers headers, final int state) {
                        Static.T.runOnUiThread(() -> {
                            Log.v(TAG, "load | failure " + state);
                            switch (state) {
                                case DeIfmoRestClient.FAILED_OFFLINE:
                                    if (getData() != null) {
                                        display();
                                    } else {
                                        draw(R.layout.state_offline);
                                        if (activity != null) {
                                            View offline_reload = activity.findViewById(R.id.offline_reload);
                                            if (offline_reload != null) {
                                                offline_reload.setOnClickListener(v -> load());
                                            }
                                        }
                                    }
                                    break;
                                case DeIfmoRestClient.FAILED_TRY_AGAIN:
                                case DeIfmoRestClient.FAILED_SERVER_ERROR:
                                    draw(R.layout.state_try_again);
                                    if (activity != null) {
                                        if (state == DeIfmoRestClient.FAILED_SERVER_ERROR) {
                                            TextView try_again_message = activity.findViewById(R.id.try_again_message);
                                            if (try_again_message != null) {
                                                try_again_message.setText(DeIfmoRestClient.getFailureMessage(activity, statusCode));
                                            }
                                        }
                                        View try_again_reload = activity.findViewById(R.id.try_again_reload);
                                        if (try_again_reload != null) {
                                            try_again_reload.setOnClickListener(v -> load());
                                        }
                                    }
                                    break;
                            }
                        });
                    }
                    @Override
                    public void onProgress(final int state) {
                        Static.T.runOnUiThread(() -> {
                            Log.v(TAG, "load | progress " + state);
                            draw(R.layout.state_loading);
                            if (activity != null) {
                                TextView loading_message = activity.findViewById(R.id.loading_message);
                                if (loading_message != null) {
                                    switch (state) {
                                        case DeIfmoRestClient.STATE_HANDLING: loading_message.setText(R.string.loading); break;
                                    }
                                }
                            }
                        });
                    }
                    @Override
                    public void onNewRequest(Client.Request request) {
                        requestHandle = request;
                    }
                });
            } else {
                Static.T.runOnUiThread(() -> {
                    if (getData() != null) {
                        display();
                    } else {
                        draw(R.layout.state_offline);
                        if (activity != null) {
                            View offline_reload = activity.findViewById(R.id.offline_reload);
                            if (offline_reload != null) {
                                offline_reload.setOnClickListener(v -> load());
                            }
                        }
                    }
                });
            }
        });
    }
    private void loadFailed() {
        Static.T.runOnUiThread(() -> {
            Log.v(TAG, "loadFailed");
            try {
                draw(R.layout.state_try_again);
                TextView try_again_message = activity.findViewById(R.id.try_again_message);
                if (try_again_message != null) try_again_message.setText(R.string.eregister_load_failed_retry_in_minute);
                View try_again_reload = activity.findViewById(R.id.try_again_reload);
                if (try_again_reload != null) {
                    try_again_reload.setOnClickListener(v -> load());
                }
            } catch (Exception e) {
                Static.error(e);
            }
        });
    }
    private void display() {
        Static.T.runThread(() -> {
            Log.v(TAG, "display");
            try {
                if (getData() == null) throw new NullPointerException("data cannot be null");
                checkData(getData());
                // creating adapter
                final JSONArray subjectsList = new JSONArray();
                final JSONArray groups = getData().getJSONArray("groups");
                for (int i = 0; i < groups.length(); i++) {
                    final JSONObject group = groups.getJSONObject(i);
                    if (group.getString("name").equals(this.group)) {
                        final JSONArray terms = group.getJSONArray("terms");
                        for (int j = 0; j < terms.length(); j++) {
                            final JSONObject term = terms.getJSONObject(j);
                            final int termNumber = term.getInt("number");
                            if (this.term == -1 || this.term == termNumber) {
                                final JSONArray subjects = term.getJSONArray("subjects");
                                for (int k = 0; k < subjects.length(); k++) {
                                    final JSONObject subject = subjects.getJSONObject(k);
                                    subjectsList.put(new JSONObject()
                                            .put("term", termNumber)
                                            .put("subject", subject)
                                    );
                                }
                            }
                        }
                        break;
                    }
                }
                final ERegisterSubjectsRVA adapter = new ERegisterSubjectsRVA(activity, subjectsList);
                adapter.setOnElementClickListener(R.id.subject, (v, data) -> Static.T.runThread(() -> {
                    try {
                        Log.v(TAG, "erl_list_view clicked");
                        final Bundle extras = new Bundle();
                        extras.putString("data", data.get("data").toString());
                        Static.T.runOnUiThread(() -> activity.openActivityOrFragment(SubjectShowFragment.class, extras));
                    } catch (Exception e) {
                        Static.error(e);
                        Static.snackBar(activity, activity.getString(R.string.something_went_wrong));
                    }
                }));
                Static.T.runOnUiThread(() -> {
                    try {
                        draw(R.layout.layout_eregister);
                        // set adapter to recycler view
                        final LinearLayoutManager layoutManager = new LinearLayoutManager(activity, LinearLayoutManager.VERTICAL, false);
                        final RecyclerView erl_list_view = activity.findViewById(R.id.erl_list_view);
                        if (erl_list_view != null) {
                            erl_list_view.setLayoutManager(layoutManager);
                            erl_list_view.setAdapter(adapter);
                            erl_list_view.setHasFixedSize(true);
                        }
                        // setup swipe
                        final SwipeRefreshLayout swipe_container = activity.findViewById(R.id.swipe_container);
                        if (swipe_container != null) {
                            swipe_container.setColorSchemeColors(Static.colorAccent);
                            swipe_container.setProgressBackgroundColorSchemeColor(Static.colorBackgroundRefresh);
                            swipe_container.setOnRefreshListener(this);
                        }
                        // setup spinners
                        int selection = 0, counter = 0;
                        // spinner: groups
                        final Spinner spinner_group = activity.findViewById(R.id.erl_group_spinner);
                        if (spinner_group != null) {
                            final ArrayList<String> spinner_group_arr = new ArrayList<>();
                            final ArrayList<String> spinner_group_arr_names = new ArrayList<>();
                            for (int i = 0; i < groups.length(); i++) {
                                JSONObject group = groups.getJSONObject(i);
                                spinner_group_arr.add(group.getString("name") + " (" + group.getJSONArray("years").getInt(0) + "/" + group.getJSONArray("years").getInt(1) + ")");
                                spinner_group_arr_names.add(group.getString("name"));
                                if (group.getString("name").equals(this.group)) selection = counter;
                                counter++;
                            }
                            spinner_group.setAdapter(new ArrayAdapter<>(activity, R.layout.spinner_layout_single_line, spinner_group_arr));
                            spinner_group.setSelection(selection);
                            spinner_group_blocker = true;
                            spinner_group.setOnItemSelectedListener(new AdapterView.OnItemSelectedListener() {
                                public void onItemSelected(final AdapterView<?> parent, final View item, final int position, final long selectedId) {
                                    Static.T.runThread(() -> {
                                        if (spinner_group_blocker) {
                                            spinner_group_blocker = false;
                                            return;
                                        }
                                        group = spinner_group_arr_names.get(position);
                                        Log.v(TAG, "spinner_group clicked | group=" + group);
                                        Storage.file.cache.put(activity, "eregister#params#selected_group", group);
                                        load(false);
                                    });
                                }
                                public void onNothingSelected(AdapterView<?> parent) {}
                            });
                        }
                        // spinner: terms
                        final Spinner spinner_period = activity.findViewById(R.id.erl_period_spinner);
                        if (spinner_period != null) {
                            final ArrayList<String> spinner_period_arr = new ArrayList<>();
                            final ArrayList<Integer> spinner_period_arr_values = new ArrayList<>();
                            selection = 2;
                            for (int i = 0; i < groups.length(); i++) {
                                JSONObject group = groups.getJSONObject(i);
                                if (group.getString("name").equals(this.group)) {
                                    int first = group.getJSONArray("terms").getJSONObject(0).getInt("number");
                                    int second = group.getJSONArray("terms").getJSONObject(1).getInt("number");
                                    spinner_period_arr.add(first + " " + activity.getString(R.string.semester));
                                    spinner_period_arr.add(second + " " + activity.getString(R.string.semester));
                                    spinner_period_arr.add(activity.getString(R.string.year));
                                    spinner_period_arr_values.add(first);
                                    spinner_period_arr_values.add(second);
                                    spinner_period_arr_values.add(-1);
                                    if (this.term == first) selection = 0;
                                    if (this.term == second) selection = 1;
                                    break;
                                }
                            }
                            spinner_period.setAdapter(new ArrayAdapter<>(activity, R.layout.spinner_layout_single_line, spinner_period_arr));
                            spinner_period.setSelection(selection);
                            spinner_period_blocker = true;
                            spinner_period.setOnItemSelectedListener(new AdapterView.OnItemSelectedListener() {
                                public void onItemSelected(final AdapterView<?> parent, final View item, final int position, final long selectedId) {
                                    Static.T.runThread(() -> {
                                        if (spinner_period_blocker) {
                                            spinner_period_blocker = false;
                                            return;
                                        }
                                        term = spinner_period_arr_values.get(position);
                                        Log.v(TAG, "spinner_period clicked | term=" + term);
                                        load(false);
                                    });
                                }
                                public void onNothingSelected(AdapterView<?> parent) {}
                            });
                        }
                        // show update time
                        Static.showUpdateTime(activity, getData().getLong("timestamp"), true);
                    } catch (Exception e) {
                        Static.error(e);
                        loadFailed();
                    }
                });
            } catch (Exception e) {
                Static.error(e);
                loadFailed();
            }
        });
    }
    private void checkData(JSONObject data) throws Exception {
        Log.v(TAG, "checkData");
        final Calendar now = Static.getCalendar();
        final int year = now.get(Calendar.YEAR);
        final int month = now.get(Calendar.MONTH);
        String currentGroup = "";
        int currentTerm = -1, maxYear = 0;
        final JSONArray groups = data.getJSONArray("groups");
        for (int i = 0; i < groups.length(); i++) {
            final JSONObject group = groups.getJSONObject(i);
            if (!this.group.isEmpty() && group.getString("name").equals(this.group)) { // мы нашли назначенную группу
                this.group = group.getString("name");
                // теперь проверяем семестр
                final JSONArray terms = group.getJSONArray("terms");
                boolean isTermOk = false;
                if (this.term == -2) {
                    final JSONArray years = group.getJSONArray("years");
                    if (year == years.getInt(month > Calendar.AUGUST ? 0 : 1)) {
                        switch (Integer.parseInt(Storage.pref.get(activity, "pref_e_journal_term", "0"))) {
                            default: case 0: this.term = group.getJSONArray("terms").getJSONObject(month > Calendar.AUGUST || month == Calendar.JANUARY ? 0 : 1).getInt("number"); break;
                            case 1: this.term = group.getJSONArray("terms").getJSONObject(0).getInt("number"); break;
                            case 2: this.term = group.getJSONArray("terms").getJSONObject(1).getInt("number"); break;
                            case 3: this.term = -1; break;
                        }
                    } else {
                        this.term = -1;
                    }
                    isTermOk = true;
                }
                for (int j = 0; j < terms.length(); j++) {
                    final JSONObject term = terms.getJSONObject(j);
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
                final JSONArray years = group.getJSONArray("years");
                if (currentGroup.isEmpty()) {
                    if (year == years.getInt(month > Calendar.AUGUST ? 0 : 1)) {
                        currentGroup = group.getString("name");
                        switch (Integer.parseInt(Storage.pref.get(activity, "pref_e_journal_term", "0"))) {
                            default: case 0: currentTerm = group.getJSONArray("terms").getJSONObject(month > Calendar.AUGUST || month == Calendar.JANUARY ? 0 : 1).getInt("number"); break;
                            case 1: currentTerm = group.getJSONArray("terms").getJSONObject(0).getInt("number"); break;
                            case 2: currentTerm = group.getJSONArray("terms").getJSONObject(1).getInt("number"); break;
                            case 3: currentTerm = -1; break;
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
                    final JSONObject group = groups.getJSONObject(i);
                    if (group.getJSONArray("years").getInt(0) == maxYear) {
                        this.group = group.getString("name");
                        break;
                    }
                }
                this.term = -1;
            }
        }
    }

    private void setData(JSONObject data) {
        this.data = data;
        storeData(this, data.toString());
    }
    private JSONObject getData() {
        if (data != null) {
            return data;
        }
        try {
            String stored = restoreData(this);
            if (stored != null && !stored.isEmpty()) {
                data = Static.string2json(stored);
                return data;
            }
        } catch (Exception e) {
            Static.error(e);
        }
        return null;
    }

    private void draw(int layoutId) {
        try {
            ViewGroup vg = activity.findViewById(R.id.container_eregister);
            if (vg != null) {
                vg.removeAllViews();
                vg.addView(inflate(layoutId), 0, new ViewGroup.LayoutParams(ViewGroup.LayoutParams.MATCH_PARENT, ViewGroup.LayoutParams.MATCH_PARENT));
            }
        } catch (Exception e){
            Static.error(e);
        }
    }
    private View inflate(int layoutId) throws InflateException {
        return ((LayoutInflater) activity.getSystemService(Context.LAYOUT_INFLATER_SERVICE)).inflate(layoutId, null);
    }
}

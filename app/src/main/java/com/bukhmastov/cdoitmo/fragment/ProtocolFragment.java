package com.bukhmastov.cdoitmo.fragment;

import android.os.Bundle;
import android.support.v4.widget.SwipeRefreshLayout;
import android.support.v7.widget.LinearLayoutManager;
import android.support.v7.widget.RecyclerView;
import android.view.MenuItem;
import android.view.View;
import android.widget.AdapterView;
import android.widget.ArrayAdapter;
import android.widget.Spinner;
import android.widget.TextView;

import com.bukhmastov.cdoitmo.App;
import com.bukhmastov.cdoitmo.R;
import com.bukhmastov.cdoitmo.adapter.rva.ProtocolRVA;
import com.bukhmastov.cdoitmo.converter.ProtocolConverter;
import com.bukhmastov.cdoitmo.firebase.FirebaseAnalyticsProvider;
import com.bukhmastov.cdoitmo.network.DeIfmoRestClient;
import com.bukhmastov.cdoitmo.network.interfaces.RestResponseHandler;
import com.bukhmastov.cdoitmo.network.model.Client;
import com.bukhmastov.cdoitmo.util.BottomBar;
import com.bukhmastov.cdoitmo.util.Color;
import com.bukhmastov.cdoitmo.util.Log;
import com.bukhmastov.cdoitmo.util.Storage;
import com.bukhmastov.cdoitmo.util.StoragePref;
import com.bukhmastov.cdoitmo.util.TextUtils;
import com.bukhmastov.cdoitmo.util.Thread;
import com.bukhmastov.cdoitmo.util.Time;

import org.json.JSONArray;
import org.json.JSONException;
import org.json.JSONObject;

import java.util.ArrayList;

public class ProtocolFragment extends ConnectedFragment implements SwipeRefreshLayout.OnRefreshListener {

    private static final String TAG = "ProtocolFragment";
    private static final int maxAttempts = 3;
    private JSONObject data = null;
    private int number_of_weeks = 1;
    private boolean spinner_weeks_blocker = true;
    private boolean loaded = false;
    private Client.Request requestHandle = null;
    protected boolean forbidden = false;

    //@Inject
    private Storage storage = Storage.instance();
    //@Inject
    private StoragePref storagePref = StoragePref.instance();

    @Override
    public void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        Log.v(TAG, "Fragment created");
        if (App.UNAUTHORIZED_MODE) {
            forbidden = true;
            Log.w(TAG, "Fragment created | UNAUTHORIZED_MODE not allowed, closing fragment...");
            close();
            return;
        }
        FirebaseAnalyticsProvider.logCurrentScreen(activity, this);
        number_of_weeks = Integer.parseInt(storagePref.get(activity, "pref_protocol_changes_weeks", "1"));
    }

    @Override
    public void onDestroy() {
        super.onDestroy();
        Log.v(TAG, "Fragment destroyed");
        try {
            if (activity.toolbar != null) {
                MenuItem simple = activity.toolbar.findItem(R.id.action_protocol_changes_switch_to_simple);
                MenuItem advanced = activity.toolbar.findItem(R.id.action_protocol_changes_switch_to_advanced);
                if (simple != null) simple.setVisible(false);
                if (advanced != null) advanced.setVisible(false);
            }
        } catch (Exception e){
            Log.exception(e);
        }
    }

    @Override
    public void onResume() {
        super.onResume();
        Log.v(TAG, "resumed");
        if (forbidden) {
            return;
        }
        FirebaseAnalyticsProvider.setCurrentScreen(activity, this);
        try {
            if (activity.toolbar != null) {
                final MenuItem simple = activity.toolbar.findItem(R.id.action_protocol_changes_switch_to_simple);
                final MenuItem advanced = activity.toolbar.findItem(R.id.action_protocol_changes_switch_to_advanced);
                if (simple != null && advanced != null) {
                    switch (storagePref.get(activity, "pref_protocol_changes_mode", "advanced")) {
                        case "simple": advanced.setVisible(true); break;
                        case "advanced": simple.setVisible(true); break;
                    }
                    simple.setOnMenuItemClickListener(item -> {
                        storagePref.put(activity, "pref_protocol_changes_mode", "simple");
                        simple.setVisible(false);
                        advanced.setVisible(true);
                        load(false);
                        return false;
                    });
                    advanced.setOnMenuItemClickListener(item -> {
                        storagePref.put(activity, "pref_protocol_changes_mode", "advanced");
                        simple.setVisible(true);
                        advanced.setVisible(false);
                        load(false);
                        return false;
                    });
                }
            }
        } catch (Exception e){
            Log.exception(e);
        }
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

    @Override
    protected int getLayoutId() {
        return R.layout.fragment_container;
    }

    @Override
    protected int getRootId() {
        return R.id.container;
    }

    private void load() {
        Thread.run(() -> load(storagePref.get(activity, "pref_use_cache", true) ? Integer.parseInt(storagePref.get(activity, "pref_dynamic_refresh", "0")) : 0));
    }
    private void load(final int refresh_rate) {
        Thread.run(() -> {
            Log.v(TAG, "load | refresh_rate=" + refresh_rate);
            if (storagePref.get(activity, "pref_use_cache", true)) {
                String cache = storage.get(activity, Storage.CACHE, Storage.USER, "protocol#core").trim();
                if (!cache.isEmpty()) {
                    try {
                        JSONObject data = new JSONObject(cache);
                        setData(data);
                        if (data.getLong("timestamp") + refresh_rate * 3600000L < Time.getCalendar().getTimeInMillis()) {
                            load(true, cache);
                        } else {
                            load(false, cache);
                        }
                    } catch (JSONException e) {
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
        Thread.run(() -> load(force, "", 0));
    }
    private void load(final boolean force, final String cache) {
        Thread.run(() -> load(force, cache, 0));
    }
    private void load(final boolean force, final String cache, final int attempt) {
        Thread.run(() -> {
            Log.v(TAG, "load | force=" + (force ? "true" : "false") + " | attempt=" + attempt);
            if ((!force || !Client.isOnline(activity)) && storagePref.get(activity, "pref_use_cache", true)) {
                try {
                    String c = cache.isEmpty() ? storage.get(activity, Storage.CACHE, Storage.USER, "protocol#core").trim() : cache;
                    if (!c.isEmpty()) {
                        Log.v(TAG, "load | from cache");
                        JSONObject d = new JSONObject(c);
                        if (d.getInt("number_of_weeks") == number_of_weeks || !Client.isOnline(activity) || attempt >= maxAttempts) {
                            setData(new JSONObject(c));
                            display();
                            return;
                        }
                    }
                } catch (Exception e) {
                    Log.v(TAG, "load | failed to load from cache");
                    storage.delete(activity, Storage.CACHE, Storage.USER, "protocol#core");
                }
            }
            if (!App.OFFLINE_MODE) {
                if (attempt >= maxAttempts) {
                    if (force) {
                        load(false, cache, attempt + 1);
                    } else {
                        if (getData() != null) {
                            display();
                        } else {
                            loadFailed();
                        }
                    }
                } else {
                    DeIfmoRestClient.get(activity, "eregisterlog?days=" + String.valueOf(number_of_weeks * 7), null, new RestResponseHandler() {
                        @Override
                        public void onSuccess(final int statusCode, final Client.Headers headers, final JSONObject responseObj, final JSONArray responseArr) {
                            Thread.run(() -> {
                                Log.v(TAG, "load | success | statusCode=" + statusCode + " | responseArr=" + (responseArr == null ? "null" : "notnull"));
                                if (statusCode == 200 && responseArr != null) {
                                    new ProtocolConverter(activity, responseArr, number_of_weeks, json -> {
                                        try {
                                            if (storagePref.get(activity, "pref_use_cache", true)) {
                                                storage.put(activity, Storage.CACHE, Storage.USER, "protocol#core", json.toString());
                                                storage.put(activity, Storage.PERMANENT, Storage.USER, "protocol_tracker#protocol", json.getJSONArray("protocol").toString());
                                            }
                                        } catch (JSONException e) {
                                            Log.exception(e);
                                        }
                                        setData(json);
                                        display();
                                    }).run();
                                } else {
                                    load(force, cache, attempt + 1);
                                }
                            });
                        }
                        @Override
                        public void onFailure(final int statusCode, final Client.Headers headers, final int state) {
                            Thread.runOnUI(() -> {
                                Log.v(TAG, "load | failure " + state);
                                switch (state) {
                                    case DeIfmoRestClient.FAILED_OFFLINE:
                                        if (getData() != null) {
                                            display();
                                        } else {
                                            draw(R.layout.state_offline_text);
                                            if (activity != null) {
                                                View offline_reload = container.findViewById(R.id.offline_reload);
                                                if (offline_reload != null) {
                                                    offline_reload.setOnClickListener(v -> load());
                                                }
                                            }
                                        }
                                        break;
                                    case DeIfmoRestClient.FAILED_TRY_AGAIN:
                                    case DeIfmoRestClient.FAILED_SERVER_ERROR:
                                    case DeIfmoRestClient.FAILED_CORRUPTED_JSON:
                                        draw(R.layout.state_failed_button);
                                        if (activity != null) {
                                            TextView try_again_message = container.findViewById(R.id.try_again_message);
                                            if (try_again_message != null) {
                                                switch (state) {
                                                    case DeIfmoRestClient.FAILED_SERVER_ERROR:   try_again_message.setText(DeIfmoRestClient.getFailureMessage(activity, statusCode)); break;
                                                    case DeIfmoRestClient.FAILED_CORRUPTED_JSON: try_again_message.setText(R.string.server_provided_corrupted_json); break;
                                                }
                                            }
                                            View try_again_reload = container.findViewById(R.id.try_again_reload);
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
                            Thread.runOnUI(() -> {
                                Log.v(TAG, "load | progress " + state);
                                draw(R.layout.state_loading_text);
                                if (activity != null) {
                                    TextView loading_message = container.findViewById(R.id.loading_message);
                                    if (loading_message != null) {
                                        switch (state) {
                                            case DeIfmoRestClient.STATE_HANDLING:
                                                loading_message.setText(R.string.loading);
                                                break;
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
                }
            } else {
                Thread.runOnUI(() -> {
                    if (getData() != null) {
                        display();
                    } else {
                        draw(R.layout.state_offline_text);
                        if (activity != null) {
                            View offline_reload = container.findViewById(R.id.offline_reload);
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
        Thread.runOnUI(() -> {
            Log.v(TAG, "loadFailed");
            try {
                draw(R.layout.state_failed_button);
                TextView try_again_message = container.findViewById(R.id.try_again_message);
                if (try_again_message != null) try_again_message.setText(R.string.load_failed_retry_in_minute);
                View try_again_reload = container.findViewById(R.id.try_again_reload);
                if (try_again_reload != null) {
                    try_again_reload.setOnClickListener(v -> load());
                }
            } catch (Exception e) {
                Log.exception(e);
            }
        });
    }
    private void display() {
        Thread.run(() -> {
            Log.v(TAG, "display");
            try {
                if (getData() == null) throw new NullPointerException("data cannot be null");
                final ProtocolRVA adapter = new ProtocolRVA(activity, getData().getJSONArray("protocol"), "advanced".equals(storagePref.get(activity, "pref_protocol_changes_mode", "advanced")));
                Thread.runOnUI(() -> {
                    try {
                        draw(R.layout.layout_protocol);
                        // set adapter to recycler view
                        final LinearLayoutManager layoutManager = new LinearLayoutManager(activity, LinearLayoutManager.VERTICAL, false);
                        final RecyclerView protocol_list = container.findViewById(R.id.protocol_list);
                        if (protocol_list != null) {
                            protocol_list.setLayoutManager(layoutManager);
                            protocol_list.setAdapter(adapter);
                            protocol_list.setHasFixedSize(true);
                        }
                        // setup swipe
                        final SwipeRefreshLayout swipe_container = container.findViewById(R.id.swipe_container);
                        if (swipe_container != null) {
                            swipe_container.setColorSchemeColors(Color.resolve(activity, R.attr.colorAccent));
                            swipe_container.setProgressBackgroundColorSchemeColor(Color.resolve(activity, R.attr.colorBackgroundRefresh));
                            swipe_container.setOnRefreshListener(this);
                        }
                        // setup spinner: weeks
                        final Spinner spinner_weeks = container.findViewById(R.id.pl_weeks_spinner);
                        if (spinner_weeks != null) {
                            final ArrayList<String> spinner_weeks_arr = new ArrayList<>();
                            final ArrayList<Integer> spinner_weeks_arr_values = new ArrayList<>();
                            for (int i = 1; i <= 4; i++) {
                                String value = activity.getString(R.string.for_the) + " ";
                                switch (i){
                                    case 1: value += activity.getString(R.string.last_week); break;
                                    case 2: value += activity.getString(R.string.last_2_weeks); break;
                                    case 3: value += activity.getString(R.string.last_3_weeks); break;
                                    case 4: value += activity.getString(R.string.last_4_weeks); break;
                                }
                                spinner_weeks_arr.add(value);
                                spinner_weeks_arr_values.add(i);
                            }
                            spinner_weeks.setAdapter(new ArrayAdapter<>(activity, R.layout.spinner_center_single_line, spinner_weeks_arr));
                            spinner_weeks.setSelection(getData().getInt("number_of_weeks") - 1);
                            spinner_weeks_blocker = true;
                            spinner_weeks.setOnItemSelectedListener(new AdapterView.OnItemSelectedListener() {
                                public void onItemSelected(final AdapterView<?> parent, final View item, final int position, final long selectedId) {
                                    Thread.run(() -> {
                                        if (spinner_weeks_blocker) {
                                            spinner_weeks_blocker = false;
                                            return;
                                        }
                                        number_of_weeks = spinner_weeks_arr_values.get(position);
                                        Log.v(TAG, "spinner_weeks clicked | number_of_weeks=" + number_of_weeks);
                                        load(true);
                                    });
                                }
                                public void onNothingSelected(AdapterView<?> parent) {}
                            });
                        }
                        // show update time
                        BottomBar.showUpdateTime(activity, getData().getLong("timestamp"));
                    } catch (Exception e) {
                        Log.exception(e);
                        loadFailed();
                    }
                });
            } catch (Exception e) {
                Log.exception(e);
                loadFailed();
            }
        });
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
                data = TextUtils.string2json(stored);
                return data;
            }
        } catch (Exception e) {
            Log.exception(e);
        }
        return null;
    }
}

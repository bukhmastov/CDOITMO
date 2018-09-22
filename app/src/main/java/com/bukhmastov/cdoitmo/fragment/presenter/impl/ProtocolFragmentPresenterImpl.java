package com.bukhmastov.cdoitmo.fragment.presenter.impl;

import android.os.Bundle;
import android.support.annotation.Nullable;
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
import com.bukhmastov.cdoitmo.activity.ConnectedActivity;
import com.bukhmastov.cdoitmo.adapter.rva.ProtocolRVA;
import com.bukhmastov.cdoitmo.converter.ProtocolConverter;
import com.bukhmastov.cdoitmo.event.bus.EventBus;
import com.bukhmastov.cdoitmo.event.bus.annotation.Event;
import com.bukhmastov.cdoitmo.event.events.ClearCacheEvent;
import com.bukhmastov.cdoitmo.factory.AppComponentProvider;
import com.bukhmastov.cdoitmo.firebase.FirebaseAnalyticsProvider;
import com.bukhmastov.cdoitmo.fragment.ConnectedFragment;
import com.bukhmastov.cdoitmo.fragment.presenter.ProtocolFragmentPresenter;
import com.bukhmastov.cdoitmo.network.DeIfmoRestClient;
import com.bukhmastov.cdoitmo.network.handlers.RestResponseHandler;
import com.bukhmastov.cdoitmo.network.model.Client;
import com.bukhmastov.cdoitmo.util.Log;
import com.bukhmastov.cdoitmo.util.NotificationMessage;
import com.bukhmastov.cdoitmo.util.Storage;
import com.bukhmastov.cdoitmo.util.StoragePref;
import com.bukhmastov.cdoitmo.util.TextUtils;
import com.bukhmastov.cdoitmo.util.Thread;
import com.bukhmastov.cdoitmo.util.Time;
import com.bukhmastov.cdoitmo.util.singleton.Color;

import org.json.JSONArray;
import org.json.JSONException;
import org.json.JSONObject;

import java.util.ArrayList;

import javax.inject.Inject;

public class ProtocolFragmentPresenterImpl implements ProtocolFragmentPresenter, SwipeRefreshLayout.OnRefreshListener {

    private static final String TAG = "ProtocolFragment";
    private static final int maxAttempts = 3;
    private ConnectedFragment fragment = null;
    private ConnectedActivity activity = null;
    private JSONObject data = null;
    private int number_of_weeks = 1;
    private boolean spinner_weeks_blocker = true;
    private boolean loaded = false;
    private Client.Request requestHandle = null;
    private boolean forbidden = false;

    @Inject
    Log log;
    @Inject
    Thread thread;
    @Inject
    EventBus eventBus;
    @Inject
    Storage storage;
    @Inject
    StoragePref storagePref;
    @Inject
    DeIfmoRestClient deIfmoRestClient;
    @Inject
    NotificationMessage notificationMessage;
    @Inject
    Time time;
    @Inject
    TextUtils textUtils;
    @Inject
    FirebaseAnalyticsProvider firebaseAnalyticsProvider;

    public ProtocolFragmentPresenterImpl() {
        AppComponentProvider.getComponent().inject(this);
        eventBus.register(this);
    }

    @Event
    public void onClearCacheEvent(ClearCacheEvent event) {
        if (event.isNot(ClearCacheEvent.PROTOCOL)) {
            return;
        }
        data = null;
        fragment.clearData(fragment);
    }

    @Override
    public void setFragment(ConnectedFragment fragment) {
        this.fragment = fragment;
        this.activity = fragment.activity();
    }

    @Override
    public void onCreate(@Nullable Bundle savedInstanceState) {
        thread.run(() -> {
            log.v(TAG, "Fragment created");
            if (App.UNAUTHORIZED_MODE) {
                forbidden = true;
                log.w(TAG, "Fragment created | UNAUTHORIZED_MODE not allowed, closing fragment...");
                thread.runOnUI(() -> fragment.close());
                return;
            }
            firebaseAnalyticsProvider.logCurrentScreen(activity, fragment);
            number_of_weeks = Integer.parseInt(storagePref.get(activity, "pref_protocol_changes_weeks", "1"));
        });
    }

    @Override
    public void onDestroy() {
        thread.runOnUI(() -> {
            log.v(TAG, "Fragment destroyed");
            loaded = false;
            if (activity != null && activity.toolbar != null) {
                MenuItem simple = activity.toolbar.findItem(R.id.action_protocol_changes_switch_to_simple);
                MenuItem advanced = activity.toolbar.findItem(R.id.action_protocol_changes_switch_to_advanced);
                if (simple != null) simple.setVisible(false);
                if (advanced != null) advanced.setVisible(false);
            }
        });
    }

    @Override
    public void onResume() {
        thread.runOnUI(() -> {
            log.v(TAG, "Fragment resumed");
            if (forbidden) {
                return;
            }
            firebaseAnalyticsProvider.setCurrentScreen(activity, fragment);
            if (activity != null && activity.toolbar != null) {
                final MenuItem simple = activity.toolbar.findItem(R.id.action_protocol_changes_switch_to_simple);
                final MenuItem advanced = activity.toolbar.findItem(R.id.action_protocol_changes_switch_to_advanced);
                if (simple != null && advanced != null) {
                    switch (storagePref.get(activity, "pref_protocol_changes_mode", "advanced")) {
                        case "simple": advanced.setVisible(true); break;
                        case "advanced": simple.setVisible(true); break;
                    }
                    simple.setOnMenuItemClickListener(item -> {
                        thread.runOnUI(() -> {
                            storagePref.put(activity, "pref_protocol_changes_mode", "simple");
                            simple.setVisible(false);
                            advanced.setVisible(true);
                            load(false);
                        });
                        return false;
                    });
                    advanced.setOnMenuItemClickListener(item -> {
                        thread.runOnUI(() -> {
                            storagePref.put(activity, "pref_protocol_changes_mode", "advanced");
                            simple.setVisible(true);
                            advanced.setVisible(false);
                            load(false);
                        });
                        return false;
                    });
                }
            }
            if (!loaded) {
                loaded = true;
                if (getData() == null) {
                    load();
                } else {
                    display();
                }
            }
        });
    }

    @Override
    public void onPause() {
        thread.run(() -> {
            log.v(TAG, "Fragment paused");
            if (requestHandle != null && requestHandle.cancel()) {
                loaded = false;
            }
        });
    }

    @Override
    public void onRefresh() {
        thread.run(() -> {
            log.v(TAG, "refreshing");
            load(true);
        });
    }

    private void load() {
        thread.run(() -> load(storagePref.get(activity, "pref_use_cache", true) ? Integer.parseInt(storagePref.get(activity, "pref_dynamic_refresh", "0")) : 0));
    }

    private void load(final int refresh_rate) {
        thread.run(() -> {
            log.v(TAG, "load | refresh_rate=" + refresh_rate);
            if (storagePref.get(activity, "pref_use_cache", true)) {
                String cache = storage.get(activity, Storage.CACHE, Storage.USER, "protocol#core").trim();
                if (!cache.isEmpty()) {
                    try {
                        JSONObject data = new JSONObject(cache);
                        setData(data);
                        if (data.getLong("timestamp") + refresh_rate * 3600000L < time.getCalendar().getTimeInMillis()) {
                            load(true, cache);
                        } else {
                            load(false, cache);
                        }
                    } catch (JSONException e) {
                        log.e(TAG, "load | exception=", e);
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
        thread.run(() -> load(force, "", 0));
    }

    private void load(final boolean force, final String cache) {
        thread.run(() -> load(force, cache, 0));
    }

    private void load(final boolean force, final String cache, final int attempt) {
        thread.run(() -> {
            log.v(TAG, "load | force=" + (force ? "true" : "false") + " | attempt=" + attempt);
            if ((!force || !Client.isOnline(activity)) && storagePref.get(activity, "pref_use_cache", true)) {
                try {
                    String c = cache.isEmpty() ? storage.get(activity, Storage.CACHE, Storage.USER, "protocol#core").trim() : cache;
                    if (!c.isEmpty()) {
                        log.v(TAG, "load | from cache");
                        JSONObject d = new JSONObject(c);
                        if (d.getInt("number_of_weeks") == number_of_weeks || !Client.isOnline(activity) || attempt >= maxAttempts) {
                            setData(new JSONObject(c));
                            display();
                            return;
                        }
                    }
                } catch (Exception e) {
                    log.v(TAG, "load | failed to load from cache");
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
                    deIfmoRestClient.get(activity, "eregisterlog?days=" + String.valueOf(number_of_weeks * 7), null, new RestResponseHandler() {
                        @Override
                        public void onSuccess(final int statusCode, final Client.Headers headers, final JSONObject responseObj, final JSONArray responseArr) {
                            thread.run(() -> {
                                log.v(TAG, "load | success | statusCode=" + statusCode + " | responseArr=" + (responseArr == null ? "null" : "notnull"));
                                if (statusCode == 200 && responseArr != null) {
                                    new ProtocolConverter(activity, responseArr, number_of_weeks, json -> {
                                        try {
                                            if (storagePref.get(activity, "pref_use_cache", true)) {
                                                storage.put(activity, Storage.CACHE, Storage.USER, "protocol#core", json.toString());
                                                storage.put(activity, Storage.PERMANENT, Storage.USER, "protocol_tracker#protocol", json.getJSONArray("protocol").toString());
                                            }
                                        } catch (JSONException e) {
                                            log.exception(e);
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
                            thread.runOnUI(() -> {
                                log.v(TAG, "load | failure " + state);
                                switch (state) {
                                    case DeIfmoRestClient.FAILED_OFFLINE:
                                        if (getData() != null) {
                                            display();
                                        } else {
                                            fragment.draw(R.layout.state_offline_text);
                                            if (activity != null) {
                                                View offline_reload = fragment.container().findViewById(R.id.offline_reload);
                                                if (offline_reload != null) {
                                                    offline_reload.setOnClickListener(v -> load());
                                                }
                                            }
                                        }
                                        break;
                                    case DeIfmoRestClient.FAILED_TRY_AGAIN:
                                    case DeIfmoRestClient.FAILED_SERVER_ERROR:
                                    case DeIfmoRestClient.FAILED_CORRUPTED_JSON:
                                        fragment.draw(R.layout.state_failed_button);
                                        if (activity != null) {
                                            TextView try_again_message = fragment.container().findViewById(R.id.try_again_message);
                                            if (try_again_message != null) {
                                                switch (state) {
                                                    case DeIfmoRestClient.FAILED_SERVER_ERROR:   try_again_message.setText(DeIfmoRestClient.getFailureMessage(activity, statusCode)); break;
                                                    case DeIfmoRestClient.FAILED_CORRUPTED_JSON: try_again_message.setText(R.string.server_provided_corrupted_json); break;
                                                }
                                            }
                                            View try_again_reload = fragment.container().findViewById(R.id.try_again_reload);
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
                            thread.runOnUI(() -> {
                                log.v(TAG, "load | progress " + state);
                                fragment.draw(R.layout.state_loading_text);
                                if (activity != null) {
                                    TextView loading_message = fragment.container().findViewById(R.id.loading_message);
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
                thread.runOnUI(() -> {
                    if (getData() != null) {
                        display();
                    } else {
                        fragment.draw(R.layout.state_offline_text);
                        if (activity != null) {
                            View offline_reload = fragment.container().findViewById(R.id.offline_reload);
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
        thread.runOnUI(() -> {
            log.v(TAG, "loadFailed");
            try {
                fragment.draw(R.layout.state_failed_button);
                TextView try_again_message = fragment.container().findViewById(R.id.try_again_message);
                if (try_again_message != null) try_again_message.setText(R.string.load_failed_retry_in_minute);
                View try_again_reload = fragment.container().findViewById(R.id.try_again_reload);
                if (try_again_reload != null) {
                    try_again_reload.setOnClickListener(v -> load());
                }
            } catch (Exception e) {
                log.exception(e);
            }
        });
    }

    private void display() {
        thread.run(() -> {
            log.v(TAG, "display");
            try {
                if (getData() == null) throw new NullPointerException("data cannot be null");
                final ProtocolRVA adapter = new ProtocolRVA(activity, getData().getJSONArray("protocol"), "advanced".equals(storagePref.get(activity, "pref_protocol_changes_mode", "advanced")));
                thread.runOnUI(() -> {
                    try {
                        fragment.draw(R.layout.layout_protocol);
                        // set adapter to recycler view
                        final LinearLayoutManager layoutManager = new LinearLayoutManager(activity, LinearLayoutManager.VERTICAL, false);
                        final RecyclerView protocol_list = fragment.container().findViewById(R.id.protocol_list);
                        if (protocol_list != null) {
                            protocol_list.setLayoutManager(layoutManager);
                            protocol_list.setAdapter(adapter);
                            protocol_list.setHasFixedSize(true);
                        }
                        // setup swipe
                        final SwipeRefreshLayout swipe_container = fragment.container().findViewById(R.id.swipe_container);
                        if (swipe_container != null) {
                            swipe_container.setColorSchemeColors(Color.resolve(activity, R.attr.colorAccent));
                            swipe_container.setProgressBackgroundColorSchemeColor(Color.resolve(activity, R.attr.colorBackgroundRefresh));
                            swipe_container.setOnRefreshListener(this);
                        }
                        // setup spinner: weeks
                        final Spinner spinner_weeks = fragment.container().findViewById(R.id.pl_weeks_spinner);
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
                                    thread.run(() -> {
                                        if (spinner_weeks_blocker) {
                                            spinner_weeks_blocker = false;
                                            return;
                                        }
                                        number_of_weeks = spinner_weeks_arr_values.get(position);
                                        log.v(TAG, "spinner_weeks clicked | number_of_weeks=" + number_of_weeks);
                                        load(true);
                                    });
                                }
                                public void onNothingSelected(AdapterView<?> parent) {}
                            });
                        }
                        // show update time
                        notificationMessage.showUpdateTime(activity, getData().getLong("timestamp"));
                    } catch (Exception e) {
                        log.exception(e);
                        loadFailed();
                    }
                });
            } catch (Exception e) {
                log.exception(e);
                loadFailed();
            }
        });
    }

    private void setData(JSONObject data) {
        this.data = data;
        fragment.storeData(fragment, data.toString());
    }

    private JSONObject getData() {
        if (data != null) {
            return data;
        }
        try {
            String stored = fragment.restoreData(fragment);
            if (stored != null && !stored.isEmpty()) {
                data = textUtils.string2json(stored);
                return data;
            }
        } catch (Exception e) {
            log.exception(e);
        }
        return null;
    }
}

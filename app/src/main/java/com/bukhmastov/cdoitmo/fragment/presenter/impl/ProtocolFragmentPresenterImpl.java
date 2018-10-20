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
import com.bukhmastov.cdoitmo.event.bus.EventBus;
import com.bukhmastov.cdoitmo.event.bus.annotation.Event;
import com.bukhmastov.cdoitmo.event.events.ClearCacheEvent;
import com.bukhmastov.cdoitmo.factory.AppComponentProvider;
import com.bukhmastov.cdoitmo.firebase.FirebaseAnalyticsProvider;
import com.bukhmastov.cdoitmo.fragment.ConnectedFragment;
import com.bukhmastov.cdoitmo.fragment.presenter.ProtocolFragmentPresenter;
import com.bukhmastov.cdoitmo.model.converter.ProtocolConverter;
import com.bukhmastov.cdoitmo.model.protocol.Protocol;
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
import com.bukhmastov.cdoitmo.util.singleton.StringUtils;

import org.json.JSONArray;
import org.json.JSONObject;

import java.util.ArrayList;

import javax.inject.Inject;

public class ProtocolFragmentPresenterImpl implements ProtocolFragmentPresenter, SwipeRefreshLayout.OnRefreshListener {

    private static final String TAG = "ProtocolFragment";
    private static final int maxAttempts = 3;
    private ConnectedFragment fragment = null;
    private ConnectedActivity activity = null;
    private Protocol data = null;
    private int numberOfWeeks = 1;
    private boolean spinnerWeeksBlocker = true;
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
            numberOfWeeks = Integer.parseInt(storagePref.get(activity, "pref_protocol_changes_weeks", "1"));
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
        thread.run(() -> {
            log.v(TAG, "Fragment resumed");
            if (forbidden) {
                return;
            }
            firebaseAnalyticsProvider.setCurrentScreen(activity, fragment);
            if (activity != null && activity.toolbar != null) {
                thread.runOnUI(() -> {
                    MenuItem simple = activity.toolbar.findItem(R.id.action_protocol_changes_switch_to_simple);
                    MenuItem advanced = activity.toolbar.findItem(R.id.action_protocol_changes_switch_to_advanced);
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
                });
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
            if (!storagePref.get(activity, "pref_use_cache", true)) {
                load(false);
                return;
            }
            Protocol cache = getFromCache();
            if (cache == null) {
                load(true, null);
                return;
            }
            setData(cache);
            if (cache.getTimestamp() + refresh_rate * 3600000L < time.getTimeInMillis()) {
                load(true, cache);
            } else {
                load(false, cache);
            }
        });
    }

    private void load(final boolean force) {
        thread.run(() -> load(force, null, 0));
    }

    private void load(final boolean force, final Protocol cached) {
        thread.run(() -> load(force, cached, 0));
    }

    private void load(final boolean force, final Protocol cached, final int attempt) {
        thread.run(() -> {
            log.v(TAG, "load | force=" + (force ? "true" : "false") + " | attempt=" + attempt);
            if ((!force || !Client.isOnline(activity)) && storagePref.get(activity, "pref_use_cache", true)) {
                try {
                    Protocol cache = cached == null ? getFromCache() : cached;
                    if (cache != null) {
                        log.v(TAG, "load | from cache");
                        setData(cache);
                        display();
                        return;
                    }
                } catch (Exception e) {
                    log.v(TAG, "load | failed to load from cache");
                }
            }
            if (App.OFFLINE_MODE) {
                if (getData() != null) {
                    display();
                    return;
                }
                thread.runOnUI(() -> {
                    fragment.draw(R.layout.state_offline_text);
                    View reload = fragment.container().findViewById(R.id.offline_reload);
                    if (reload != null) {
                        reload.setOnClickListener(v -> load());
                    }
                });
                return;
            }
            if (attempt >= maxAttempts) {
                if (force) {
                    load(false, cached, attempt + 1);
                    return;
                }
                if (getData() != null) {
                    display();
                } else {
                    loadFailed();
                }
                return;
            }
            deIfmoRestClient.get(activity, "eregisterlog?days=" + String.valueOf(numberOfWeeks * 7), null, new RestResponseHandler() {
                @Override
                public void onSuccess(final int statusCode, final Client.Headers headers, final JSONObject obj, final JSONArray arr) {
                    thread.run(() -> {
                        log.v(TAG, "load | success | statusCode=" + statusCode + " | arr=" + (arr == null ? "null" : "notnull"));
                        if (statusCode == 200 && arr != null) {
                            Protocol data = new Protocol().fromJson(new JSONObject().put("protocol", arr));
                            data.setTimestamp(time.getTimeInMillis());
                            data.setNumberOfWeeks(numberOfWeeks);
                            data = new ProtocolConverter(data).convert();
                            if (data != null && storagePref.get(activity, "pref_use_cache", true)) {
                                String json = data.toJsonString();
                                storage.put(activity, Storage.CACHE, Storage.USER, "protocol#core", json);
                                storage.put(activity, Storage.PERMANENT, Storage.USER, "protocol_tracker#protocol", json);
                            }
                            setData(data);
                            display();
                            return;
                        }
                        load(force, cached, attempt + 1);
                    }, throwable -> {
                        loadFailed();
                    });
                }
                @Override
                public void onFailure(final int statusCode, final Client.Headers headers, final int state) {
                    thread.run(() -> {
                        log.v(TAG, "load | failure " + state);
                        switch (state) {
                            case DeIfmoRestClient.FAILED_OFFLINE:
                                if (getData() != null) {
                                    display();
                                    return;
                                }
                                thread.runOnUI(() -> {
                                    fragment.draw(R.layout.state_offline_text);
                                    View reload = fragment.container().findViewById(R.id.offline_reload);
                                    if (reload != null) {
                                        reload.setOnClickListener(v -> load());
                                    }
                                }, throwable -> {
                                    loadFailed();
                                });
                                break;
                            case DeIfmoRestClient.FAILED_TRY_AGAIN:
                            case DeIfmoRestClient.FAILED_SERVER_ERROR:
                            case DeIfmoRestClient.FAILED_CORRUPTED_JSON:
                                thread.runOnUI(() -> {
                                    fragment.draw(R.layout.state_failed_button);
                                    TextView message = fragment.container().findViewById(R.id.try_again_message);
                                    if (message != null) {
                                        switch (state) {
                                            case DeIfmoRestClient.FAILED_SERVER_ERROR:
                                                if (activity == null) {
                                                    message.setText(DeIfmoRestClient.getFailureMessage(statusCode));
                                                } else {
                                                    message.setText(DeIfmoRestClient.getFailureMessage(activity, statusCode));
                                                }
                                                break;
                                            case DeIfmoRestClient.FAILED_CORRUPTED_JSON:
                                                message.setText(R.string.server_provided_corrupted_json);
                                                break;
                                        }
                                    }
                                    View reload = fragment.container().findViewById(R.id.try_again_reload);
                                    if (reload != null) {
                                        reload.setOnClickListener(v -> load());
                                    }
                                }, throwable -> {
                                    loadFailed();
                                });
                                break;
                        }
                    }, throwable -> {
                        loadFailed();
                    });
                }
                @Override
                public void onProgress(final int state) {
                    thread.runOnUI(() -> {
                        log.v(TAG, "load | progress " + state);
                        fragment.draw(R.layout.state_loading_text);
                        TextView message = fragment.container().findViewById(R.id.loading_message);
                        if (message != null) {
                            switch (state) {
                                case DeIfmoRestClient.STATE_HANDLING:
                                    message.setText(R.string.loading);
                                    break;
                            }
                        }
                    });
                }
                @Override
                public void onNewRequest(Client.Request request) {
                    requestHandle = request;
                }
            });
        }, throwable -> {
            loadFailed();
        });
    }

    private void loadFailed() {
        thread.runOnUI(() -> {
            log.v(TAG, "loadFailed");
            fragment.draw(R.layout.state_failed_button);
            TextView message = fragment.container().findViewById(R.id.try_again_message);
            if (message != null) {
                message.setText(R.string.load_failed_retry_in_minute);
            }
            View reload = fragment.container().findViewById(R.id.try_again_reload);
            if (reload != null) {
                reload.setOnClickListener(v -> load());
            }
        }, throwable -> {});
    }

    private void display() {
        thread.run(() -> {
            log.v(TAG, "display");
            Protocol data = getData();
            if (data == null) {
                loadFailed();
                return;
            }
            ProtocolRVA adapter = new ProtocolRVA(activity, data, "advanced".equals(storagePref.get(activity, "pref_protocol_changes_mode", "advanced")));
            thread.runOnUI(() -> {
                fragment.draw(R.layout.layout_protocol);
                // set adapter to recycler view
                LinearLayoutManager layoutManager = new LinearLayoutManager(activity, LinearLayoutManager.VERTICAL, false);
                RecyclerView recyclerView = fragment.container().findViewById(R.id.protocol_list);
                if (recyclerView != null) {
                    recyclerView.setLayoutManager(layoutManager);
                    recyclerView.setAdapter(adapter);
                    recyclerView.setHasFixedSize(true);
                }
                // setup swipe
                SwipeRefreshLayout swipe = fragment.container().findViewById(R.id.swipe_container);
                if (swipe != null) {
                    swipe.setColorSchemeColors(Color.resolve(activity, R.attr.colorAccent));
                    swipe.setProgressBackgroundColorSchemeColor(Color.resolve(activity, R.attr.colorBackgroundRefresh));
                    swipe.setOnRefreshListener(this);
                }
                // setup spinner: weeks
                Spinner spinner = fragment.container().findViewById(R.id.pl_weeks_spinner);
                if (spinner != null) {
                    final ArrayList<String> weekLabelArr = new ArrayList<>();
                    final ArrayList<Integer> weekArr = new ArrayList<>();
                    for (int i = 1; i <= 4; i++) {
                        String value = activity.getString(R.string.for_the) + " ";
                        switch (i){
                            case 1: value += activity.getString(R.string.last_week); break;
                            case 2: value += activity.getString(R.string.last_2_weeks); break;
                            case 3: value += activity.getString(R.string.last_3_weeks); break;
                            case 4: value += activity.getString(R.string.last_4_weeks); break;
                        }
                        weekArr.add(i);
                        weekLabelArr.add(value);
                    }
                    spinner.setAdapter(new ArrayAdapter<>(activity, R.layout.spinner_center_single_line, weekLabelArr));
                    spinner.setSelection(data.getNumberOfWeeks() - 1);
                    spinnerWeeksBlocker = true;
                    spinner.setOnItemSelectedListener(new AdapterView.OnItemSelectedListener() {
                        public void onItemSelected(final AdapterView<?> parent, final View item, final int position, final long selectedId) {
                            thread.run(() -> {
                                if (spinnerWeeksBlocker) {
                                    spinnerWeeksBlocker = false;
                                    return;
                                }
                                numberOfWeeks = weekArr.get(position);
                                log.v(TAG, "Number of weeks selected | numberOfWeeks=" + numberOfWeeks);
                                load(true);
                            });
                        }
                        public void onNothingSelected(AdapterView<?> parent) {}
                    });
                }
                // show update time
                notificationMessage.showUpdateTime(activity, data.getTimestamp());
            }, throwable -> {
                log.exception(throwable);
                loadFailed();
            });
        }, throwable -> {
            log.exception(throwable);
            loadFailed();
        });
    }

    private Protocol getFromCache() {
        thread.assertNotUI();
        String cache = storage.get(activity, Storage.CACHE, Storage.USER, "protocol#core").trim();
        if (StringUtils.isBlank(cache)) {
            return null;
        }
        try {
            return new Protocol().fromJsonString(cache);
        } catch (Exception e) {
            storage.delete(activity, Storage.CACHE, Storage.USER, "protocol#core");
            return null;
        }
    }

    private void setData(Protocol data) {
        thread.assertNotUI();
        try {
            this.data = data;
            fragment.storeData(fragment, data.toJsonString());
        } catch (Exception e) {
            log.exception(e);
        }
    }

    private Protocol getData() {
        thread.assertNotUI();
        if (data != null) {
            return data;
        }
        try {
            String stored = fragment.restoreData(fragment);
            if (stored != null && !stored.isEmpty()) {
                data = new Protocol().fromJsonString(stored);
                return data;
            }
        } catch (Exception e) {
            log.exception(e);
        }
        return null;
    }
}

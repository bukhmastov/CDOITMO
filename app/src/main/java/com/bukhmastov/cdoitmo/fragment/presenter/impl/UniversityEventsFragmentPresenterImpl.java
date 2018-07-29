package com.bukhmastov.cdoitmo.fragment.presenter.impl;

import android.content.Context;
import android.os.Bundle;
import android.support.annotation.LayoutRes;
import android.support.annotation.Nullable;
import android.support.v4.app.Fragment;
import android.support.v4.app.FragmentActivity;
import android.support.v4.widget.SwipeRefreshLayout;
import android.support.v7.widget.LinearLayoutManager;
import android.support.v7.widget.RecyclerView;
import android.view.InflateException;
import android.view.KeyEvent;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.EditText;
import android.widget.FrameLayout;
import android.widget.TextView;

import com.bukhmastov.cdoitmo.App;
import com.bukhmastov.cdoitmo.R;
import com.bukhmastov.cdoitmo.adapter.rva.RecyclerViewOnScrollListener;
import com.bukhmastov.cdoitmo.adapter.rva.university.UniversityEventsRVA;
import com.bukhmastov.cdoitmo.adapter.rva.university.UniversityRVA;
import com.bukhmastov.cdoitmo.event.bus.EventBus;
import com.bukhmastov.cdoitmo.event.bus.annotation.Event;
import com.bukhmastov.cdoitmo.event.events.ClearCacheEvent;
import com.bukhmastov.cdoitmo.factory.AppComponentProvider;
import com.bukhmastov.cdoitmo.firebase.FirebaseAnalyticsProvider;
import com.bukhmastov.cdoitmo.fragment.presenter.UniversityEventsFragmentPresenter;
import com.bukhmastov.cdoitmo.network.IfmoRestClient;
import com.bukhmastov.cdoitmo.network.handlers.RestResponseHandler;
import com.bukhmastov.cdoitmo.network.model.Client;
import com.bukhmastov.cdoitmo.util.Log;
import com.bukhmastov.cdoitmo.util.Storage;
import com.bukhmastov.cdoitmo.util.StoragePref;
import com.bukhmastov.cdoitmo.util.Thread;
import com.bukhmastov.cdoitmo.util.Time;
import com.bukhmastov.cdoitmo.util.singleton.Color;

import org.json.JSONArray;
import org.json.JSONException;
import org.json.JSONObject;

import java.util.ArrayList;

import javax.inject.Inject;

public class UniversityEventsFragmentPresenterImpl implements UniversityEventsFragmentPresenter, SwipeRefreshLayout.OnRefreshListener {

    private static final String TAG = "UniversityEventsFragment";
    private FragmentActivity activity = null;
    private Fragment fragment = null;
    private View container;
    private Client.Request requestHandle = null;
    private boolean loaded = false;
    private JSONObject events = null;
    private final int limit = 10;
    private int offset = 0;
    private String search = "";
    private UniversityEventsRVA eventsRecyclerViewAdapter = null;
    private long timestamp = 0;

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
    IfmoRestClient ifmoRestClient;
    @Inject
    Time time;
    @Inject
    FirebaseAnalyticsProvider firebaseAnalyticsProvider;

    public UniversityEventsFragmentPresenterImpl() {
        AppComponentProvider.getComponent().inject(this);
        eventBus.register(this);
    }

    @Event
    public void onClearCacheEvent(ClearCacheEvent event) {
        if (event.isNot("university")) {
            return;
        }
        events = null;
    }

    @Override
    public void setFragment(Fragment fragment) {
        this.fragment = fragment;
        this.activity = fragment.getActivity();
    }

    @Override
    public void onCreate(@Nullable Bundle savedInstanceState) {
        log.v(TAG, "Fragment created");
        firebaseAnalyticsProvider.logCurrentScreen(activity, fragment);
    }

    @Override
    public void onDestroy() {
        log.v(TAG, "Fragment destroyed");
        loaded = false;
    }

    @Override
    public void onResume() {
        log.v(TAG, "Fragment resumed");
        firebaseAnalyticsProvider.setCurrentScreen(activity, fragment);
        if (!loaded) {
            loaded = true;
            load();
        }
    }

    @Override
    public void onPause() {
        log.v(TAG, "Fragment paused");
        if (requestHandle != null && requestHandle.cancel()) {
            loaded = false;
        }
    }

    @Override
    public void onCreateView(View container) {
        this.container = container;
    }

    @Override
    public void onRefresh() {
        log.v(TAG, "refreshing");
        load(search, true);
    }

    private void load() {
        thread.run(() -> load(""));
    }

    private void load(final String search) {
        thread.run(() -> load(search, storagePref.get(activity, "pref_use_cache", true) && storagePref.get(activity, "pref_use_university_cache", false)
                ? Integer.parseInt(storagePref.get(activity, "pref_dynamic_refresh", "0"))
                : 0));
    }

    private void load(final String search, final int refresh_rate) {
        thread.run(() -> {
            log.v(TAG, "load | search=" + search + " | refresh_rate=" + refresh_rate);
            if (storagePref.get(activity, "pref_use_cache", true) && storagePref.get(activity, "pref_use_university_cache", false)) {
                String cache = storage.get(activity, Storage.CACHE, Storage.GLOBAL, "university#events").trim();
                if (!cache.isEmpty()) {
                    try {
                        JSONObject cacheJson = new JSONObject(cache);
                        events = cacheJson.getJSONObject("data");
                        timestamp = cacheJson.getLong("timestamp");
                        if (timestamp + refresh_rate * 3600000L < time.getCalendar().getTimeInMillis()) {
                            load(search, true);
                        } else {
                            load(search, false);
                        }
                    } catch (JSONException e) {
                        log.exception(e);
                        load(search, true);
                    }
                } else {
                    load(search, false);
                }
            } else {
                load(search, false);
            }
        });
    }

    private void load(final String search, final boolean force) {
        thread.run(() -> {
            log.v(TAG, "load | search=" + search + " | force=" + (force ? "true" : "false"));
            if ((!force || !Client.isOnline(activity)) && events != null) {
                display();
                return;
            }
            if (!App.OFFLINE_MODE) {
                this.offset = 0;
                this.search = search;
                loadProvider(new RestResponseHandler() {
                    @Override
                    public void onSuccess(final int statusCode, final Client.Headers headers, final JSONObject json, final JSONArray responseArr) {
                        thread.run(() -> {
                            if (statusCode == 200) {
                                long now = time.getCalendar().getTimeInMillis();
                                if (json != null && storagePref.get(activity, "pref_use_cache", true) && storagePref.get(activity, "pref_use_university_cache", false)) {
                                    try {
                                        storage.put(activity, Storage.CACHE, Storage.GLOBAL, "university#events", new JSONObject()
                                                .put("timestamp", now)
                                                .put("data", json)
                                                .toString()
                                        );
                                    } catch (JSONException e) {
                                        log.exception(e);
                                    }
                                }
                                events = json;
                                timestamp = now;
                                display();
                            } else {
                                loadFailed();
                            }
                        });
                    }
                    @Override
                    public void onFailure(final int statusCode, final Client.Headers headers, final int state) {
                        thread.runOnUI(() -> {
                            log.v(TAG, "load | failure " + state);
                            switch (state) {
                                case IfmoRestClient.FAILED_OFFLINE:
                                    draw(R.layout.state_offline_text);
                                    if (activity != null) {
                                        View offline_reload = container.findViewById(R.id.offline_reload);
                                        if (offline_reload != null) {
                                            offline_reload.setOnClickListener(v -> load());
                                        }
                                    }
                                    break;
                                case IfmoRestClient.FAILED_CORRUPTED_JSON:
                                case IfmoRestClient.FAILED_SERVER_ERROR:
                                case IfmoRestClient.FAILED_TRY_AGAIN:
                                    draw(R.layout.state_failed_button);
                                    TextView try_again_message = activity.findViewById(R.id.try_again_message);
                                    if (try_again_message != null) {
                                        switch (state) {
                                            case IfmoRestClient.FAILED_SERVER_ERROR:   try_again_message.setText(IfmoRestClient.getFailureMessage(activity, statusCode)); break;
                                            case IfmoRestClient.FAILED_CORRUPTED_JSON: try_again_message.setText(R.string.server_provided_corrupted_json); break;
                                        }
                                    }
                                    if (activity != null) {
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
                        thread.runOnUI(() -> {
                            log.v(TAG, "load | progress " + state);
                            draw(R.layout.state_loading_text);
                            if (activity != null) {
                                TextView loading_message = container.findViewById(R.id.loading_message);
                                if (loading_message != null) {
                                    switch (state) {
                                        case IfmoRestClient.STATE_HANDLING:
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
            } else {
                thread.runOnUI(() -> {
                    draw(R.layout.state_offline_text);
                    if (activity != null) {
                        View offline_reload = activity.findViewById(R.id.offline_reload);
                        if (offline_reload != null) {
                            offline_reload.setOnClickListener(v -> load(search));
                        }
                    }
                });
            }
        });
    }

    private void loadProvider(RestResponseHandler handler) {
        log.v(TAG, "loadProvider");
        ifmoRestClient.get(activity, "event?limit=" + limit + "&offset=" + offset + "&search=" + search, null, handler);
    }

    private void loadFailed() {
        thread.runOnUI(() -> {
            log.v(TAG, "loadFailed");
            try {
                draw(R.layout.state_failed_button);
                TextView try_again_message = container.findViewById(R.id.try_again_message);
                if (try_again_message != null) try_again_message.setText(R.string.load_failed);
                View try_again_reload = container.findViewById(R.id.try_again_reload);
                if (try_again_reload != null) {
                    try_again_reload.setOnClickListener(v -> load());
                }
            } catch (Exception e) {
                log.exception(e);
            }
        });
    }

    private void display() {
        thread.runOnUI(() -> {
            log.v(TAG, "display");
            if (events == null) {
                loadFailed();
                return;
            }
            try {
                draw(R.layout.layout_university_list_infinite);
                // поиск
                final EditText search_input = container.findViewById(R.id.search_input);
                final FrameLayout search_action = container.findViewById(R.id.search_action);
                search_action.setOnClickListener(v -> load(search_input.getText().toString().trim(), true));
                search_input.setOnKeyListener((v, keyCode, event) -> {
                    if (event.getAction() == KeyEvent.ACTION_DOWN && keyCode == KeyEvent.KEYCODE_ENTER) {
                        load(search_input.getText().toString().trim(), true);
                        return true;
                    }
                    return false;
                });
                search_input.setText(search);
                // очищаем сообщение
                ViewGroup infinite_list_info = container.findViewById(R.id.infinite_list_info);
                infinite_list_info.removeAllViews();
                infinite_list_info.setPadding(0, 0, 0, 0);
                // список
                JSONArray list = events.getJSONArray("list");
                if (list.length() > 0) {
                    eventsRecyclerViewAdapter = new UniversityEventsRVA(activity);
                    final RecyclerView infinite_list = container.findViewById(R.id.infinite_list);
                    if (infinite_list != null) {
                        infinite_list.setLayoutManager(new LinearLayoutManager(activity));
                        infinite_list.setAdapter(eventsRecyclerViewAdapter);
                        infinite_list.addOnScrollListener(new RecyclerViewOnScrollListener(container));
                    }
                    eventsRecyclerViewAdapter.setOnStateClickListener(R.id.load_more, v -> {
                        offset += limit;
                        eventsRecyclerViewAdapter.setState(R.id.loading_more);
                        thread.run(() -> loadProvider(new RestResponseHandler() {
                            @Override
                            public void onSuccess(final int statusCode, final Client.Headers headers, final JSONObject json, final JSONArray responseArr) {
                                thread.run(() -> {
                                    try {
                                        events.put("count", json.getInt("count"));
                                        events.put("limit", json.getInt("limit"));
                                        events.put("offset", json.getInt("offset"));
                                        JSONArray list_original = events.getJSONArray("list");
                                        JSONArray list1 = json.getJSONArray("list");
                                        for (int i = 0; i < list1.length(); i++) {
                                            list_original.put(list1.getJSONObject(i));
                                        }
                                        long now = time.getCalendar().getTimeInMillis();
                                        timestamp = now;
                                        if (storagePref.get(activity, "pref_use_cache", true) && storagePref.get(activity, "pref_use_university_cache", false)) {
                                            try {
                                                storage.put(activity, Storage.CACHE, Storage.GLOBAL, "university#events", new JSONObject()
                                                        .put("timestamp", now)
                                                        .put("data", events)
                                                        .toString()
                                                );
                                            } catch (JSONException e) {
                                                log.exception(e);
                                            }
                                        }
                                        displayContent(list1);
                                    } catch (Exception e) {
                                        log.exception(e);
                                        thread.runOnUI(() -> eventsRecyclerViewAdapter.setState(R.id.load_more));
                                    }
                                });
                            }
                            @Override
                            public void onFailure(int statusCode, Client.Headers headers, int state) {
                                thread.runOnUI(() -> eventsRecyclerViewAdapter.setState(R.id.load_more));
                            }
                            @Override
                            public void onProgress(int state) {}
                            @Override
                            public void onNewRequest(Client.Request request) {
                                requestHandle = request;
                            }
                        }));
                    });
                    if (timestamp > 0 && timestamp + 5000 < time.getCalendar().getTimeInMillis()) {
                        UniversityRVA.Item item = new UniversityRVA.Item();
                        item.type = UniversityRVA.TYPE_INFO_ABOUT_UPDATE_TIME;
                        item.data = new JSONObject().put("title", activity.getString(R.string.update_date) + " " + time.getUpdateTime(activity, timestamp));
                        eventsRecyclerViewAdapter.addItem(item);
                    }
                    displayContent(list);
                } else {
                    View view = inflate(R.layout.state_nothing_to_display_compact);
                    ((TextView) view.findViewById(R.id.ntd_text)).setText(R.string.no_events);
                    infinite_list_info.addView(view);
                }
                // добавляем отступ
                container.findViewById(R.id.top_panel).post(() -> {
                    try {
                        int height = container.findViewById(R.id.top_panel).getHeight();
                        RecyclerView infinite_list = container.findViewById(R.id.infinite_list);
                        infinite_list.setPadding(0, height, 0, 0);
                        infinite_list.scrollToPosition(0);
                        if (infinite_list_info.getChildCount() > 0) {
                            infinite_list_info.setPadding(0, height, 0, 0);
                        }
                    } catch (Exception ignore) {
                        // ignore
                    }
                });
                // работаем со свайпом
                SwipeRefreshLayout mSwipeRefreshLayout = container.findViewById(R.id.infinite_list_swipe);
                if (mSwipeRefreshLayout != null) {
                    mSwipeRefreshLayout.setColorSchemeColors(Color.resolve(activity, R.attr.colorAccent));
                    mSwipeRefreshLayout.setProgressBackgroundColorSchemeColor(Color.resolve(activity, R.attr.colorBackgroundRefresh));
                    mSwipeRefreshLayout.setOnRefreshListener(this);
                }
            } catch (Exception e) {
                log.exception(e);
                loadFailed();
            }
        });
    }

    private void displayContent(final JSONArray list) {
        thread.run(() -> {
            try {
                final ArrayList<UniversityEventsRVA.Item> items = new ArrayList<>();
                for (int i = 0; i < list.length(); i++) {
                    try {
                        final JSONObject event = list.getJSONObject(i);
                        UniversityEventsRVA.Item item = new UniversityEventsRVA.Item();
                        item.type = UniversityEventsRVA.TYPE_MINOR;
                        item.data = event;
                        items.add(item);
                    } catch (Exception e) {
                        log.exception(e);
                    }
                }
                thread.runOnUI(() -> {
                    try {
                        if (eventsRecyclerViewAdapter != null) {
                            eventsRecyclerViewAdapter.addItem(items);
                            if (offset + limit < events.getInt("count")) {
                                eventsRecyclerViewAdapter.setState(R.id.load_more);
                            } else {
                                eventsRecyclerViewAdapter.setState(R.id.no_more);
                            }
                        }
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

    private void draw(int layoutId) {
        try {
            ViewGroup vg = ((ViewGroup) container);
            if (vg != null) {
                vg.removeAllViews();
                vg.addView(inflate(layoutId), 0, new ViewGroup.LayoutParams(ViewGroup.LayoutParams.MATCH_PARENT, ViewGroup.LayoutParams.MATCH_PARENT));
            }
        } catch (Exception e){
            log.exception(e);
        }
    }

    private View inflate(@LayoutRes int layout) throws InflateException {
        LayoutInflater inflater = (LayoutInflater) activity.getSystemService(Context.LAYOUT_INFLATER_SERVICE);
        if (inflater == null) {
            log.e(TAG, "Failed to inflate layout, inflater is null");
            return null;
        }
        return inflater.inflate(layout, null);
    }
}

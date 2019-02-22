package com.bukhmastov.cdoitmo.fragment.presenter.impl;

import android.content.Context;
import android.os.Bundle;
import androidx.annotation.LayoutRes;
import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.fragment.app.Fragment;
import androidx.fragment.app.FragmentActivity;
import androidx.swiperefreshlayout.widget.SwipeRefreshLayout;
import androidx.recyclerview.widget.LinearLayoutManager;
import androidx.recyclerview.widget.RecyclerView;
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
import com.bukhmastov.cdoitmo.activity.WebViewActivity;
import com.bukhmastov.cdoitmo.adapter.rva.RVA;
import com.bukhmastov.cdoitmo.adapter.rva.RecyclerViewOnScrollListener;
import com.bukhmastov.cdoitmo.adapter.rva.university.UniversityEventsRVA;
import com.bukhmastov.cdoitmo.adapter.rva.university.UniversityRVA;
import com.bukhmastov.cdoitmo.event.bus.EventBus;
import com.bukhmastov.cdoitmo.event.bus.annotation.Event;
import com.bukhmastov.cdoitmo.event.events.ClearCacheEvent;
import com.bukhmastov.cdoitmo.event.events.OpenActivityEvent;
import com.bukhmastov.cdoitmo.factory.AppComponentProvider;
import com.bukhmastov.cdoitmo.firebase.FirebaseAnalyticsProvider;
import com.bukhmastov.cdoitmo.fragment.presenter.UniversityEventsFragmentPresenter;
import com.bukhmastov.cdoitmo.model.rva.RVASingleValue;
import com.bukhmastov.cdoitmo.model.university.events.UEvent;
import com.bukhmastov.cdoitmo.model.university.events.UEvents;
import com.bukhmastov.cdoitmo.network.IfmoRestClient;
import com.bukhmastov.cdoitmo.network.handlers.RestResponseHandler;
import com.bukhmastov.cdoitmo.network.model.Client;
import com.bukhmastov.cdoitmo.util.Log;
import com.bukhmastov.cdoitmo.util.Storage;
import com.bukhmastov.cdoitmo.util.StoragePref;
import com.bukhmastov.cdoitmo.util.Thread;
import com.bukhmastov.cdoitmo.util.Time;
import com.bukhmastov.cdoitmo.util.singleton.CollectionUtils;
import com.bukhmastov.cdoitmo.util.singleton.Color;
import com.bukhmastov.cdoitmo.util.singleton.StringUtils;

import org.json.JSONArray;
import org.json.JSONObject;

import java.util.ArrayList;
import java.util.Collection;

import javax.inject.Inject;

import static com.bukhmastov.cdoitmo.util.Thread.UE;

public class UniversityEventsFragmentPresenterImpl implements UniversityEventsFragmentPresenter, SwipeRefreshLayout.OnRefreshListener {

    private static final String TAG = "UniversityEventsFragment";
    private FragmentActivity activity = null;
    private Fragment fragment = null;
    private View container;
    private Client.Request requestHandle = null;
    private boolean loaded = false;
    private UEvents events = null;
    private final int limit = 10;
    private int offset = 0;
    private String search = "";
    private UniversityEventsRVA eventsRecyclerViewAdapter = null;

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
        if (event.isNot(ClearCacheEvent.UNIVERSITY)) {
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
        thread.initialize(UE);
        log.v(TAG, "Fragment created");
        firebaseAnalyticsProvider.logCurrentScreen(activity, fragment);
    }

    @Override
    public void onDestroy() {
        log.v(TAG, "Fragment destroyed");
        loaded = false;
        thread.interrupt(UE);
    }

    @Override
    public void onResume() {
        thread.run(UE, () -> {
            log.v(TAG, "Fragment resumed");
            firebaseAnalyticsProvider.setCurrentScreen(activity, fragment);
            if (!loaded) {
                loaded = true;
                load();
            }
        });
    }

    @Override
    public void onPause() {
        log.v(TAG, "Fragment paused");
        thread.standalone(() -> {
            if (requestHandle != null && requestHandle.cancel()) {
                loaded = false;
            }
        });
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
        thread.run(UE, () -> load(""));
    }

    private void load(String search) {
        thread.run(UE, () -> {
            load(search, storagePref.get(activity, "pref_use_cache", true) && storagePref.get(activity, "pref_use_university_cache", false)
                    ? Integer.parseInt(storagePref.get(activity, "pref_dynamic_refresh", "0"))
                    : 0);
        });
    }

    private void load(String search, int refresh_rate) {
        thread.run(UE, () -> {
            log.v(TAG, "load | search=", search, " | refresh_rate=", refresh_rate);
            if (!(storagePref.get(activity, "pref_use_cache", true) && storagePref.get(activity, "pref_use_university_cache", false))) {
                load(search, false);
                return;
            }
            UEvents cache = getFromCache();
            if (cache == null) {
                load(search, true);
                return;
            }
            events = cache;
            if (cache.getTimestamp() + refresh_rate * 3600000L < time.getTimeInMillis()) {
                load(search, true);
            } else {
                load(search, false);
            }
        }, throwable -> {
            loadFailed();
        });
    }

    private void load(String search, boolean force) {
        thread.run(UE, () -> {
            log.v(TAG, "load | search=", search, " | force=", force);
            if ((!force || !Client.isOnline(activity)) && events != null) {
                display();
                return;
            }
            if (App.OFFLINE_MODE) {
                thread.runOnUI(UE, () -> {
                    draw(R.layout.state_offline_text);
                    View reload = container.findViewById(R.id.offline_reload);
                    if (reload != null) {
                        reload.setOnClickListener(v -> load(search));
                    }
                }, throwable -> {
                    loadFailed();
                });
                return;
            }
            this.offset = 0;
            this.search = search;
            loadProvider(new RestResponseHandler() {
                @Override
                public void onSuccess(int code, Client.Headers headers, JSONObject obj, JSONArray arr) throws Exception {
                    if (code == 200 && obj != null) {
                        UEvents data = new UEvents().fromJson(obj);
                        data.setTimestamp(time.getTimeInMillis());
                        if (storagePref.get(activity, "pref_use_cache", true) && storagePref.get(activity, "pref_use_university_cache", false)) {
                            storage.put(activity, Storage.CACHE, Storage.GLOBAL, "university#events", data.toJsonString());
                        }
                        events = data;
                        display();
                        return;
                    }
                    loadFailed();
                }
                @Override
                public void onFailure(int code, Client.Headers headers, int state) {
                    thread.runOnUI(UE, () -> {
                        log.v(TAG, "load | failure ", state);
                        switch (state) {
                            case IfmoRestClient.FAILED_OFFLINE: {
                                draw(R.layout.state_offline_text);
                                View reload = container.findViewById(R.id.offline_reload);
                                if (reload != null) {
                                    reload.setOnClickListener(v -> load());
                                }
                                break;
                            }
                            case IfmoRestClient.FAILED_CORRUPTED_JSON:
                            case IfmoRestClient.FAILED_SERVER_ERROR:
                            case IfmoRestClient.FAILED_TRY_AGAIN: {
                                draw(R.layout.state_failed_button);
                                TextView message = activity.findViewById(R.id.try_again_message);
                                if (message != null) {
                                    switch (state) {
                                        case IfmoRestClient.FAILED_SERVER_ERROR: message.setText(IfmoRestClient.getFailureMessage(activity, code)); break;
                                        case IfmoRestClient.FAILED_CORRUPTED_JSON: message.setText(R.string.server_provided_corrupted_json); break;
                                    }
                                }
                                View reload = container.findViewById(R.id.try_again_reload);
                                if (reload != null) {
                                    reload.setOnClickListener(v -> load());
                                }
                                break;
                            }
                        }
                    }, throwable -> {
                        loadFailed();
                    });
                }
                @Override
                public void onProgress(int state) {
                    thread.runOnUI(UE, () -> {
                        log.v(TAG, "load | progress " + state);
                        draw(R.layout.state_loading_text);
                        TextView message = container.findViewById(R.id.loading_message);
                        if (message != null) {
                            switch (state) {
                                case IfmoRestClient.STATE_HANDLING:
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

    private void loadProvider(RestResponseHandler handler) {
        log.v(TAG, "loadProvider");
        ifmoRestClient.get(activity, "event?limit=" + limit + "&offset=" + offset + "&search=" + search, null, handler);
    }

    private void loadFailed() {
        thread.runOnUI(UE, () -> {
            log.v(TAG, "loadFailed");
            draw(R.layout.state_failed_button);
            TextView message = container.findViewById(R.id.try_again_message);
            if (message != null) {
                message.setText(R.string.load_failed);
            }
            View reload = container.findViewById(R.id.try_again_reload);
            if (reload != null) {
                reload.setOnClickListener(v -> load());
            }
        }, throwable -> {
            log.exception(throwable);
        });
    }

    private void display() {
        thread.runOnUI(UE, () -> {
            log.v(TAG, "display");
            if (events == null) {
                loadFailed();
                return;
            }
            draw(R.layout.layout_university_list_infinite);
            // поиск
            EditText searchInput = container.findViewById(R.id.search_input);
            FrameLayout searchAction = container.findViewById(R.id.search_action);
            searchAction.setOnClickListener(v -> load(searchInput.getText().toString().trim(), true));
            searchInput.setOnKeyListener((v, keyCode, event) -> {
                if (event.getAction() == KeyEvent.ACTION_DOWN && keyCode == KeyEvent.KEYCODE_ENTER) {
                    load(searchInput.getText().toString().trim(), true);
                    return true;
                }
                return false;
            });
            searchInput.setText(search);
            // очищаем сообщение
            ViewGroup listInfo = container.findViewById(R.id.infinite_list_info);
            listInfo.removeAllViews();
            listInfo.setPadding(0, 0, 0, 0);
            // список
            if (CollectionUtils.isNotEmpty(events.getEvents())) {
                eventsRecyclerViewAdapter = new UniversityEventsRVA(activity);
                RecyclerView list = container.findViewById(R.id.infinite_list);
                if (list != null) {
                    list.setLayoutManager(new LinearLayoutManager(activity));
                    list.setAdapter(eventsRecyclerViewAdapter);
                    list.addOnScrollListener(new RecyclerViewOnScrollListener(container));
                }
                eventsRecyclerViewAdapter.setClickListener(R.id.news_click, (v, entity) -> thread.run(UE, () -> {
                    if (!(entity.getEntity() instanceof UEvent)) {
                        return;
                    }
                    UEvent event = (UEvent) entity.getEntity();
                    if (StringUtils.isBlank(event.getUrlWebview())) {
                        return;
                    }
                    Bundle extras = new Bundle();
                    extras.putString("url", event.getUrlWebview().trim());
                    extras.putString("title", activity.getString(R.string.events));
                    eventBus.fire(new OpenActivityEvent(WebViewActivity.class, extras));
                }));
                eventsRecyclerViewAdapter.setClickListener(R.id.load_more, (v, entity) -> thread.run(UE, () -> {
                    offset += limit;
                    thread.runOnUI(UE, () -> eventsRecyclerViewAdapter.setState(R.id.loading_more));
                    loadProvider(new RestResponseHandler() {
                        @Override
                        public void onSuccess(int code, Client.Headers headers, JSONObject obj, JSONArray arr) throws Exception {
                            if (code == 200 && obj != null) {
                                UEvents data = new UEvents().fromJson(obj);
                                events.getEvents().addAll(data.getEvents());
                                events.setCount(data.getCount());
                                events.setLimit(data.getLimit());
                                events.setOffset(data.getOffset());
                                events.setTimestamp(time.getTimeInMillis());
                                if (storagePref.get(activity, "pref_use_cache", true) && storagePref.get(activity, "pref_use_university_cache", false)) {
                                    storage.put(activity, Storage.CACHE, Storage.GLOBAL, "university#events", data.toJsonString());
                                }
                                displayEvents(data.getEvents());
                                return;
                            }
                            thread.runOnUI(UE, () -> eventsRecyclerViewAdapter.setState(R.id.load_more));
                        }
                        @Override
                        public void onFailure(int code, Client.Headers headers, int state) {
                            thread.runOnUI(UE, () -> eventsRecyclerViewAdapter.setState(R.id.load_more));
                        }
                        @Override
                        public void onProgress(int state) {}
                        @Override
                        public void onNewRequest(Client.Request request) {
                            requestHandle = request;
                        }
                    });
                }));
                if (events.getTimestamp() > 0 && events.getTimestamp() + 5000 < time.getCalendar().getTimeInMillis()) {
                    eventsRecyclerViewAdapter.addItem(new UniversityRVA.Item<>(
                            UniversityRVA.TYPE_INFO_ABOUT_UPDATE_TIME,
                            new RVASingleValue(activity.getString(R.string.update_date) + " " + time.getUpdateTime(activity, events.getTimestamp()))
                    ));
                }
                displayEvents(events.getEvents());
            } else {
                View view = inflate(R.layout.state_nothing_to_display_compact);
                ((TextView) view.findViewById(R.id.ntd_text)).setText(R.string.no_events);
                listInfo.addView(view);
            }
            // добавляем отступ
            container.findViewById(R.id.top_panel).post(() -> {
                try {
                    int height = container.findViewById(R.id.top_panel).getHeight();
                    RecyclerView list = container.findViewById(R.id.infinite_list);
                    list.setPadding(0, height, 0, 0);
                    list.scrollToPosition(0);
                    if (listInfo.getChildCount() > 0) {
                        listInfo.setPadding(0, height, 0, 0);
                    }
                } catch (Exception ignore) {
                    // ignore
                }
            });
            // работаем со свайпом
            SwipeRefreshLayout swipe = container.findViewById(R.id.infinite_list_swipe);
            if (swipe != null) {
                swipe.setColorSchemeColors(Color.resolve(activity, R.attr.colorAccent));
                swipe.setProgressBackgroundColorSchemeColor(Color.resolve(activity, R.attr.colorBackgroundRefresh));
                swipe.setOnRefreshListener(this);
            }
        }, throwable -> {
            log.exception(throwable);
            loadFailed();
        });
    }

    private void displayEvents(Collection<UEvent> uEvents) {
        thread.run(UE, () -> {
            Collection<RVA.Item> items = new ArrayList<>();
            for (UEvent event : uEvents) {
                items.add(new UniversityEventsRVA.Item<>(
                        UniversityEventsRVA.TYPE_MINOR,
                        event
                ));
            }
            if (items.size() == 0) {
                items.add(new UniversityRVA.Item(UniversityEventsRVA.TYPE_NO_DATA));
            }
            thread.runOnUI(UE, () -> {
                if (eventsRecyclerViewAdapter != null) {
                    eventsRecyclerViewAdapter.addItems(items);
                    if (events != null && offset + limit < events.getCount()) {
                        eventsRecyclerViewAdapter.setState(R.id.load_more);
                    } else {
                        eventsRecyclerViewAdapter.setState(R.id.no_more);
                    }
                }
            }, throwable -> {
                log.exception(throwable);
                loadFailed();
            });
        }, throwable -> {
            log.exception(throwable);
            loadFailed();
        });
    }

    private UEvents getFromCache() {
        thread.assertNotUI();
        String cache = storage.get(activity, Storage.CACHE, Storage.GLOBAL, "university#events").trim();
        if (StringUtils.isBlank(cache)) {
            return null;
        }
        try {
            return new UEvents().fromJsonString(cache);
        } catch (Exception e) {
            storage.delete(activity, Storage.CACHE, Storage.GLOBAL, "university#events");
            return null;
        }
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

    @NonNull
    private View inflate(@LayoutRes int layout) throws InflateException {
        LayoutInflater inflater = (LayoutInflater) activity.getSystemService(Context.LAYOUT_INFLATER_SERVICE);
        return inflater.inflate(layout, null);
    }
}

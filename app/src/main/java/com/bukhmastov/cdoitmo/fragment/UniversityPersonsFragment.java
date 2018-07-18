package com.bukhmastov.cdoitmo.fragment;

import android.app.Activity;
import android.content.Context;
import android.os.Bundle;
import android.support.annotation.LayoutRes;
import android.support.annotation.NonNull;
import android.support.annotation.Nullable;
import android.support.v4.app.Fragment;
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
import com.bukhmastov.cdoitmo.adapter.rva.university.UniversityPersonsRVA;
import com.bukhmastov.cdoitmo.adapter.rva.RecyclerViewOnScrollListener;
import com.bukhmastov.cdoitmo.adapter.rva.university.UniversityRVA;
import com.bukhmastov.cdoitmo.firebase.FirebaseAnalyticsProvider;
import com.bukhmastov.cdoitmo.network.IfmoRestClient;
import com.bukhmastov.cdoitmo.network.handlers.RestResponseHandler;
import com.bukhmastov.cdoitmo.network.model.Client;
import com.bukhmastov.cdoitmo.util.singleton.Color;
import com.bukhmastov.cdoitmo.util.Log;
import com.bukhmastov.cdoitmo.util.Storage;
import com.bukhmastov.cdoitmo.util.StoragePref;
import com.bukhmastov.cdoitmo.util.Thread;
import com.bukhmastov.cdoitmo.util.Time;

import org.json.JSONArray;
import org.json.JSONException;
import org.json.JSONObject;

import java.util.ArrayList;

public class UniversityPersonsFragment extends Fragment implements SwipeRefreshLayout.OnRefreshListener {

    private static final String TAG = "UniversityPersonsFragment";
    private Activity activity;
    private View container;
    private Client.Request requestHandle = null;
    private boolean loaded = false;
    private JSONObject persons = null;
    private final int limit = 20;
    private int offset = 0;
    private String search = "";
    private UniversityPersonsRVA personsRecyclerViewAdapter = null;
    private long timestamp = 0;

    //@Inject
    private Log log = Log.instance();
    //@Inject
    private Thread thread = Thread.instance();
    //@Inject
    private Storage storage = Storage.instance();
    //@Inject
    private StoragePref storagePref = StoragePref.instance();
    //@Inject
    private IfmoRestClient ifmoRestClient = IfmoRestClient.instance();
    //@Inject
    private Time time = Time.instance();
    //@Inject
    private FirebaseAnalyticsProvider firebaseAnalyticsProvider = FirebaseAnalyticsProvider.instance();

    @Override
    public void onCreate(@Nullable Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        log.v(TAG, "Fragment created");
        activity = getActivity();
        firebaseAnalyticsProvider.logCurrentScreen(activity, this);
    }

    @Override
    public void onDestroy() {
        super.onDestroy();
        log.v(TAG, "Fragment destroyed");
    }

    @Override
    public void onResume() {
        super.onResume();
        log.v(TAG, "resumed");
        firebaseAnalyticsProvider.setCurrentScreen(activity, this);
        if (!loaded) {
            loaded = true;
            load();
        }
    }

    @Override
    public void onPause() {
        super.onPause();
        log.v(TAG, "paused");
        if (requestHandle != null && requestHandle.cancel()) {
            loaded = false;
        }
    }

    @Override
    public View onCreateView(@NonNull LayoutInflater inflater, ViewGroup c, Bundle savedInstanceState) {
        View view = inflater.inflate(R.layout.fragment_container, c, false);
        container = view.findViewById(R.id.container);
        return view;
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
                        ? Integer.parseInt(storagePref.get(activity, "pref_static_refresh", "168"))
                        : 0));
    }
    private void load(final String search, final int refresh_rate) {
        thread.run(() -> {
            log.v(TAG, "load | search=" + search + " | refresh_rate=" + refresh_rate);
            if (storagePref.get(activity, "pref_use_cache", true) && storagePref.get(activity, "pref_use_university_cache", false)) {
                String cache = storage.get(activity, Storage.CACHE, Storage.GLOBAL, "university#persons").trim();
                if (!cache.isEmpty()) {
                    try {
                        JSONObject cacheJson = new JSONObject(cache);
                        persons = cacheJson.getJSONObject("data");
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
            if ((!force || !Client.isOnline(activity)) && persons != null) {
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
                                        storage.put(activity, Storage.CACHE, Storage.GLOBAL, "university#persons", new JSONObject()
                                                .put("timestamp", now)
                                                .put("data", json)
                                                .toString()
                                        );
                                    } catch (JSONException e) {
                                        log.exception(e);
                                    }
                                }
                                persons = json;
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
                                    if (activity != null) {
                                        TextView try_again_message = activity.findViewById(R.id.try_again_message);
                                        if (try_again_message != null) {
                                            switch (state) {
                                                case IfmoRestClient.FAILED_SERVER_ERROR:   try_again_message.setText(IfmoRestClient.getFailureMessage(activity, statusCode)); break;
                                                case IfmoRestClient.FAILED_CORRUPTED_JSON: try_again_message.setText(R.string.server_provided_corrupted_json); break;
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
    private void loadProvider(final RestResponseHandler handler) {
        log.v(TAG, "loadProvider");
        ifmoRestClient.get(activity, "person?limit=" + limit + "&offset=" + offset + "&search=" + search, null, handler);
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
            if (persons == null) {
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
                JSONArray list = persons.getJSONArray("list");
                if (list.length() > 0) {
                    personsRecyclerViewAdapter = new UniversityPersonsRVA(activity);
                    final RecyclerView infinite_list = container.findViewById(R.id.infinite_list);
                    if (infinite_list != null) {
                        infinite_list.setLayoutManager(new LinearLayoutManager(activity));
                        infinite_list.setAdapter(personsRecyclerViewAdapter);
                        infinite_list.addOnScrollListener(new RecyclerViewOnScrollListener(container));
                    }
                    personsRecyclerViewAdapter.setOnStateClickListener(R.id.load_more, v -> {
                        offset += limit;
                        personsRecyclerViewAdapter.setState(R.id.loading_more);
                        thread.run(() -> loadProvider(new RestResponseHandler() {
                            @Override
                            public void onSuccess(final int statusCode, final Client.Headers headers, final JSONObject json, final JSONArray responseArr) {
                                thread.run(() -> {
                                    try {
                                        persons.put("count", json.getInt("count"));
                                        persons.put("limit", json.getInt("limit"));
                                        persons.put("offset", json.getInt("offset"));
                                        JSONArray list_original = persons.getJSONArray("list");
                                        JSONArray list1 = json.getJSONArray("list");
                                        for (int i = 0; i < list1.length(); i++) {
                                            list_original.put(list1.getJSONObject(i));
                                        }
                                        long now = time.getCalendar().getTimeInMillis();
                                        timestamp = now;
                                        if (storagePref.get(activity, "pref_use_cache", true) && storagePref.get(activity, "pref_use_university_cache", false)) {
                                            try {
                                                storage.put(activity, Storage.CACHE, Storage.GLOBAL, "university#persons", new JSONObject()
                                                        .put("timestamp", now)
                                                        .put("data", persons)
                                                        .toString()
                                                );
                                            } catch (JSONException e) {
                                                log.exception(e);
                                            }
                                        }
                                        displayContent(list1);
                                    } catch (Exception e) {
                                        log.exception(e);
                                        thread.runOnUI(() -> personsRecyclerViewAdapter.setState(R.id.load_more));
                                    }
                                });
                            }
                            @Override
                            public void onFailure(int statusCode, Client.Headers headers, int state) {
                                thread.runOnUI(() -> personsRecyclerViewAdapter.setState(R.id.load_more));
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
                        personsRecyclerViewAdapter.addItem(item);
                    }
                    displayContent(list);
                } else {
                    View view = inflate(R.layout.state_nothing_to_display_compact);
                    ((TextView) view.findViewById(R.id.ntd_text)).setText(R.string.no_persons);
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
                final ArrayList<UniversityPersonsRVA.Item> items = new ArrayList<>();
                for (int i = 0; i < list.length(); i++) {
                    try {
                        final JSONObject news = list.getJSONObject(i);
                        UniversityPersonsRVA.Item item = new UniversityPersonsRVA.Item();
                        item.type = UniversityPersonsRVA.TYPE_MAIN;
                        item.data = news;
                        items.add(item);
                    } catch (Exception e) {
                        log.exception(e);
                    }
                }
                thread.runOnUI(() -> {
                    try {
                        if (personsRecyclerViewAdapter != null) {
                            personsRecyclerViewAdapter.addItem(items);
                            if (offset + limit < persons.getInt("count")) {
                                personsRecyclerViewAdapter.setState(R.id.load_more);
                            } else {
                                personsRecyclerViewAdapter.setState(R.id.no_more);
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
    private View inflate(@LayoutRes int layoutId) throws InflateException {
        return ((LayoutInflater) activity.getSystemService(Context.LAYOUT_INFLATER_SERVICE)).inflate(layoutId, null);
    }
}

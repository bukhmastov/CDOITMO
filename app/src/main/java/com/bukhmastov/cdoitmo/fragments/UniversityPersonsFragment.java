package com.bukhmastov.cdoitmo.fragments;

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

import com.bukhmastov.cdoitmo.R;
import com.bukhmastov.cdoitmo.adapters.rva.university.UniversityPersonsRVA;
import com.bukhmastov.cdoitmo.adapters.rva.RecyclerViewOnScrollListener;
import com.bukhmastov.cdoitmo.adapters.rva.university.UniversityRVA;
import com.bukhmastov.cdoitmo.firebase.FirebaseAnalyticsProvider;
import com.bukhmastov.cdoitmo.network.IfmoRestClient;
import com.bukhmastov.cdoitmo.network.interfaces.RestResponseHandler;
import com.bukhmastov.cdoitmo.network.models.Client;
import com.bukhmastov.cdoitmo.utils.Log;
import com.bukhmastov.cdoitmo.utils.Static;
import com.bukhmastov.cdoitmo.utils.Storage;

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

    @Override
    public void onCreate(@Nullable Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        Log.v(TAG, "Fragment created");
        activity = getActivity();
        FirebaseAnalyticsProvider.logCurrentScreen(activity, this);
    }

    @Override
    public void onDestroy() {
        super.onDestroy();
        Log.v(TAG, "Fragment destroyed");
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
        if (requestHandle != null && requestHandle.cancel()) {
            loaded = false;
        }
    }

    @Override
    public View onCreateView(@NonNull LayoutInflater inflater, ViewGroup cont, Bundle savedInstanceState) {
        View view = inflater.inflate(R.layout.fragment_university_tab, cont, false);
        container = view.findViewById(R.id.university_tab_container);
        return view;
    }

    @Override
    public void onRefresh() {
        Log.v(TAG, "refreshing");
        load(search, true);
    }

    private void load() {
        Static.T.runThread(() -> load(""));
    }
    private void load(final String search) {
        Static.T.runThread(() -> load(search, Storage.pref.get(activity, "pref_use_cache", true) && Storage.pref.get(activity, "pref_use_university_cache", false)
                        ? Integer.parseInt(Storage.pref.get(activity, "pref_static_refresh", "168"))
                        : 0));
    }
    private void load(final String search, final int refresh_rate) {
        Static.T.runThread(() -> {
            Log.v(TAG, "load | search=" + search + " | refresh_rate=" + refresh_rate);
            if (Storage.pref.get(activity, "pref_use_cache", true) && Storage.pref.get(activity, "pref_use_university_cache", false)) {
                String cache = Storage.file.general.cache.get(activity, "university#persons").trim();
                if (!cache.isEmpty()) {
                    try {
                        JSONObject cacheJson = new JSONObject(cache);
                        persons = cacheJson.getJSONObject("data");
                        timestamp = cacheJson.getLong("timestamp");
                        if (timestamp + refresh_rate * 3600000L < Static.getCalendar().getTimeInMillis()) {
                            load(search, true);
                        } else {
                            load(search, false);
                        }
                    } catch (JSONException e) {
                        Static.error(e);
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
        Static.T.runThread(() -> {
            Log.v(TAG, "load | search=" + search + " | force=" + (force ? "true" : "false"));
            if ((!force || !Static.isOnline(activity)) && persons != null) {
                display();
                return;
            }
            if (!Static.OFFLINE_MODE) {
                this.offset = 0;
                this.search = search;
                loadProvider(new RestResponseHandler() {
                    @Override
                    public void onSuccess(final int statusCode, final Client.Headers headers, final JSONObject json, final JSONArray responseArr) {
                        Static.T.runThread(() -> {
                            if (statusCode == 200) {
                                long now = Static.getCalendar().getTimeInMillis();
                                if (json != null && Storage.pref.get(activity, "pref_use_cache", true) && Storage.pref.get(activity, "pref_use_university_cache", false)) {
                                    try {
                                        Storage.file.general.cache.put(activity, "university#persons", new JSONObject()
                                                .put("timestamp", now)
                                                .put("data", json)
                                                .toString()
                                        );
                                    } catch (JSONException e) {
                                        Static.error(e);
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
                        Static.T.runOnUiThread(() -> {
                            Log.v(TAG, "load | failure " + state);
                            switch (state) {
                                case IfmoRestClient.FAILED_OFFLINE:
                                    draw(R.layout.state_offline);
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
                                    draw(R.layout.state_try_again);
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
                        Static.T.runOnUiThread(() -> {
                            Log.v(TAG, "load | progress " + state);
                            draw(R.layout.state_loading);
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
                Static.T.runOnUiThread(() -> {
                    draw(R.layout.state_offline);
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
        Log.v(TAG, "loadProvider");
        IfmoRestClient.get(activity, "person?limit=" + limit + "&offset=" + offset + "&search=" + search, null, handler);
    }
    private void loadFailed() {
        Static.T.runOnUiThread(() -> {
            Log.v(TAG, "loadFailed");
            try {
                draw(R.layout.state_try_again);
                TextView try_again_message = container.findViewById(R.id.try_again_message);
                if (try_again_message != null) try_again_message.setText(R.string.load_failed);
                View try_again_reload = container.findViewById(R.id.try_again_reload);
                if (try_again_reload != null) {
                    try_again_reload.setOnClickListener(v -> load());
                }
            } catch (Exception e) {
                Static.error(e);
            }
        });
    }
    private void display() {
        Static.T.runOnUiThread(() -> {
            Log.v(TAG, "display");
            if (persons == null) {
                loadFailed();
                return;
            }
            try {
                draw(R.layout.layout_university_persons_list);
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
                ViewGroup persons_list_info = container.findViewById(R.id.persons_list_info);
                persons_list_info.removeAllViews();
                persons_list_info.setPadding(0, 0, 0, 0);
                // список
                JSONArray list = persons.getJSONArray("list");
                if (list.length() > 0) {
                    personsRecyclerViewAdapter = new UniversityPersonsRVA(activity);
                    final RecyclerView persons_list = container.findViewById(R.id.persons_list);
                    if (persons_list != null) {
                        persons_list.setLayoutManager(new LinearLayoutManager(activity));
                        persons_list.setAdapter(personsRecyclerViewAdapter);
                        persons_list.addOnScrollListener(new RecyclerViewOnScrollListener(container));
                    }
                    personsRecyclerViewAdapter.setOnStateClickListener(R.id.load_more, v -> {
                        offset += limit;
                        personsRecyclerViewAdapter.setState(R.id.loading_more);
                        Static.T.runThread(() -> loadProvider(new RestResponseHandler() {
                            @Override
                            public void onSuccess(final int statusCode, final Client.Headers headers, final JSONObject json, final JSONArray responseArr) {
                                Static.T.runThread(() -> {
                                    try {
                                        persons.put("count", json.getInt("count"));
                                        persons.put("limit", json.getInt("limit"));
                                        persons.put("offset", json.getInt("offset"));
                                        JSONArray list_original = persons.getJSONArray("list");
                                        JSONArray list1 = json.getJSONArray("list");
                                        for (int i = 0; i < list1.length(); i++) {
                                            list_original.put(list1.getJSONObject(i));
                                        }
                                        long now = Static.getCalendar().getTimeInMillis();
                                        timestamp = now;
                                        if (Storage.pref.get(activity, "pref_use_cache", true) && Storage.pref.get(activity, "pref_use_university_cache", false)) {
                                            try {
                                                Storage.file.general.cache.put(activity, "university#persons", new JSONObject()
                                                        .put("timestamp", now)
                                                        .put("data", persons)
                                                        .toString()
                                                );
                                            } catch (JSONException e) {
                                                Static.error(e);
                                            }
                                        }
                                        displayContent(list1);
                                    } catch (Exception e) {
                                        Static.error(e);
                                        Static.T.runOnUiThread(() -> personsRecyclerViewAdapter.setState(R.id.load_more));
                                    }
                                });
                            }
                            @Override
                            public void onFailure(int statusCode, Client.Headers headers, int state) {
                                Static.T.runOnUiThread(() -> personsRecyclerViewAdapter.setState(R.id.load_more));
                            }
                            @Override
                            public void onProgress(int state) {}
                            @Override
                            public void onNewRequest(Client.Request request) {
                                requestHandle = request;
                            }
                        }));
                    });
                    if (timestamp > 0 && timestamp + 5000 < Static.getCalendar().getTimeInMillis()) {
                        UniversityRVA.Item item = new UniversityRVA.Item();
                        item.type = UniversityRVA.TYPE_INFO_ABOUT_UPDATE_TIME;
                        item.data = new JSONObject().put("title", activity.getString(R.string.update_date) + " " + Static.getUpdateTime(activity, timestamp));
                        personsRecyclerViewAdapter.addItem(item);
                    }
                    displayContent(list);
                } else {
                    View view = inflate(R.layout.nothing_to_display);
                    ((TextView) view.findViewById(R.id.ntd_text)).setText(R.string.no_persons);
                    persons_list_info.addView(view);
                }
                // добавляем отступ
                container.findViewById(R.id.top_panel).post(() -> {
                    try {
                        int height = container.findViewById(R.id.top_panel).getHeight();
                        RecyclerView persons_list = container.findViewById(R.id.persons_list);
                        persons_list.setPadding(0, height, 0, 0);
                        persons_list.scrollToPosition(0);
                        if (persons_list_info.getChildCount() > 0) {
                            persons_list_info.setPadding(0, height, 0, 0);
                        }
                    } catch (Exception ignore) {
                        // ignore
                    }
                });
                // работаем со свайпом
                SwipeRefreshLayout mSwipeRefreshLayout = container.findViewById(R.id.persons_list_swipe);
                if (mSwipeRefreshLayout != null) {
                    mSwipeRefreshLayout.setColorSchemeColors(Static.colorAccent);
                    mSwipeRefreshLayout.setProgressBackgroundColorSchemeColor(Static.colorBackgroundRefresh);
                    mSwipeRefreshLayout.setOnRefreshListener(this);
                }
            } catch (Exception e) {
                Static.error(e);
                loadFailed();
            }
        });
    }
    private void displayContent(final JSONArray list) {
        Static.T.runThread(() -> {
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
                        Static.error(e);
                    }
                }
                Static.T.runOnUiThread(() -> {
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

    private void draw(int layoutId) {
        try {
            ViewGroup vg = ((ViewGroup) container);
            if (vg != null) {
                vg.removeAllViews();
                vg.addView(inflate(layoutId), 0, new ViewGroup.LayoutParams(ViewGroup.LayoutParams.MATCH_PARENT, ViewGroup.LayoutParams.MATCH_PARENT));
            }
        } catch (Exception e){
            Static.error(e);
        }
    }
    private View inflate(@LayoutRes int layoutId) throws InflateException {
        return ((LayoutInflater) activity.getSystemService(Context.LAYOUT_INFLATER_SERVICE)).inflate(layoutId, null);
    }
}

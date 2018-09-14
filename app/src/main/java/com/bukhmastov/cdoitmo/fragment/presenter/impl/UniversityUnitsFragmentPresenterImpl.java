package com.bukhmastov.cdoitmo.fragment.presenter.impl;

import android.content.Context;
import android.content.Intent;
import android.net.Uri;
import android.os.Bundle;
import android.support.annotation.LayoutRes;
import android.support.annotation.Nullable;
import android.support.v4.app.Fragment;
import android.support.v4.app.FragmentActivity;
import android.support.v4.widget.SwipeRefreshLayout;
import android.support.v7.widget.LinearLayoutManager;
import android.support.v7.widget.RecyclerView;
import android.util.ArrayMap;
import android.view.InflateException;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.ImageView;
import android.widget.LinearLayout;
import android.widget.TextView;

import com.bukhmastov.cdoitmo.App;
import com.bukhmastov.cdoitmo.R;
import com.bukhmastov.cdoitmo.adapter.rva.RecyclerViewOnScrollListener;
import com.bukhmastov.cdoitmo.adapter.rva.university.UniversityFacultiesRVA;
import com.bukhmastov.cdoitmo.adapter.rva.university.UniversityRVA;
import com.bukhmastov.cdoitmo.event.bus.EventBus;
import com.bukhmastov.cdoitmo.event.bus.annotation.Event;
import com.bukhmastov.cdoitmo.event.events.ClearCacheEvent;
import com.bukhmastov.cdoitmo.event.events.OpenIntentEvent;
import com.bukhmastov.cdoitmo.factory.AppComponentProvider;
import com.bukhmastov.cdoitmo.firebase.FirebaseAnalyticsProvider;
import com.bukhmastov.cdoitmo.fragment.presenter.UniversityUnitsFragmentPresenter;
import com.bukhmastov.cdoitmo.network.IfmoRestClient;
import com.bukhmastov.cdoitmo.network.handlers.RestResponseHandler;
import com.bukhmastov.cdoitmo.network.model.Client;
import com.bukhmastov.cdoitmo.util.Log;
import com.bukhmastov.cdoitmo.util.Static;
import com.bukhmastov.cdoitmo.util.Storage;
import com.bukhmastov.cdoitmo.util.StoragePref;
import com.bukhmastov.cdoitmo.util.Thread;
import com.bukhmastov.cdoitmo.util.Time;
import com.bukhmastov.cdoitmo.util.singleton.Color;
import com.bukhmastov.cdoitmo.util.singleton.JsonUtils;

import org.json.JSONArray;
import org.json.JSONException;
import org.json.JSONObject;

import java.util.ArrayList;

import javax.inject.Inject;

public class UniversityUnitsFragmentPresenterImpl implements UniversityUnitsFragmentPresenter, SwipeRefreshLayout.OnRefreshListener {

    private static final String TAG = "UniversityUnitsFragment";
    private FragmentActivity activity = null;
    private Fragment fragment = null;
    private View container;
    private Client.Request requestHandle = null;
    private boolean loaded = false;
    private final ArrayList<String> stack = new ArrayList<>();
    private final ArrayMap<String, String> history = new ArrayMap<>();
    private UniversityFacultiesRVA facultiesRecyclerViewAdapter = null;
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
    Static staticUtil;
    @Inject
    Time time;
    @Inject
    FirebaseAnalyticsProvider firebaseAnalyticsProvider;

    public UniversityUnitsFragmentPresenterImpl() {
        AppComponentProvider.getComponent().inject(this);
        eventBus.register(this);
    }

    @Event
    public void onClearCacheEvent(ClearCacheEvent event) {
        if (event.isNot("university")) {
            return;
        }
        history.clear();
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
        load(true);
    }

    private void load() {
        thread.run(() -> load(storagePref.get(activity, "pref_use_cache", true) && storagePref.get(activity, "pref_use_university_cache", false)
                ? Integer.parseInt(storagePref.get(activity, "pref_static_refresh", "168"))
                : 0));
    }

    private void load(final int refresh_rate) {
        thread.run(() -> {
            log.v(TAG, "load | refresh_rate=" + refresh_rate);
            String uid = stack.size() == 0 ? "0" : stack.get(stack.size() - 1);
            if (storagePref.get(activity, "pref_use_cache", true) && storagePref.get(activity, "pref_use_university_cache", false)) {
                String cache = storage.get(activity, Storage.CACHE, Storage.GLOBAL, "university#units#" + uid).trim();
                if (!cache.isEmpty()) {
                    try {
                        JSONObject cacheJson = new JSONObject(cache);
                        timestamp = cacheJson.getLong("timestamp");
                        if (timestamp + refresh_rate * 3600000L < time.getCalendar().getTimeInMillis()) {
                            load(true, cache);
                        } else {
                            load(false, cache);
                        }
                    } catch (JSONException e) {
                        log.exception(e);
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
        thread.run(() -> load(force, ""));
    }

    private void load(final boolean force, final String cache) {
        thread.run(() -> {
            log.v(TAG, "load | force=" + (force ? "true" : "false"));
            final String uid = stack.size() == 0 ? "0" : stack.get(stack.size() - 1);
            if (!force && history.containsKey(uid)) {
                try {
                    log.v(TAG, "load | from local cache");
                    String local = history.get(uid);
                    JSONObject localObj = new JSONObject(local);
                    timestamp = time.getCalendar().getTimeInMillis();
                    display(localObj);
                    return;
                } catch (Exception e) {
                    log.v(TAG, "load | failed to load from local cache");
                    history.remove(uid);
                }
            }
            if ((!force || !Client.isOnline(activity)) && storagePref.get(activity, "pref_use_cache", true) && storagePref.get(activity, "pref_use_university_cache", false)) {
                try {
                    String c = cache.isEmpty() ? storage.get(activity, Storage.CACHE, Storage.GLOBAL, "university#units#" + uid).trim() : cache;
                    if (!c.isEmpty()) {
                        log.v(TAG, "load | from cache");
                        display(new JSONObject(c).getJSONObject("data"));
                        return;
                    }
                } catch (Exception e) {
                    log.v(TAG, "load | failed to load from cache");
                    storage.delete(activity, Storage.CACHE, Storage.GLOBAL, "university#units#" + uid);
                }
            }
            if (!App.OFFLINE_MODE) {
                loadProvider(new RestResponseHandler() {
                    @Override
                    public void onSuccess(final int statusCode, final Client.Headers headers, final JSONObject json, final JSONArray responseArr) {
                        thread.run(() -> {
                            if (statusCode == 200) {
                                long now = time.getCalendar().getTimeInMillis();
                                if (json != null && storagePref.get(activity, "pref_use_cache", true) && storagePref.get(activity, "pref_use_university_cache", false)) {
                                    try {
                                        storage.put(activity, Storage.CACHE, Storage.GLOBAL, "university#units#" + uid, new JSONObject()
                                                .put("timestamp", now)
                                                .put("data", json)
                                                .toString()
                                        );
                                    } catch (JSONException e) {
                                        log.exception(e);
                                    }
                                }
                                timestamp = now;
                                if (json != null) {
                                    history.put(uid, json.toString());
                                }
                                display(json);
                            } else {
                                loadFailed();
                            }
                        });
                    }
                    @Override
                    public void onFailure(final int statusCode, final Client.Headers headers, final int state) {
                        thread.runOnUI(() -> {
                            log.v(TAG, "forceLoad | failure " + state);
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
                            log.v(TAG, "forceLoad | progress " + state);
                            draw(R.layout.state_loading_text);
                            if (activity != null) {
                                TextView loading_message = container.findViewById(R.id.loading_message);
                                if (loading_message != null) {
                                    switch (state) {
                                        case IfmoRestClient.STATE_HANDLING: loading_message.setText(R.string.loading); break;
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
                            offline_reload.setOnClickListener(v -> load());
                        }
                    }
                });
            }
        });
    }

    private void loadProvider(RestResponseHandler handler) {
        log.v(TAG, "loadProvider");
        String unit_id = "";
        if (stack.size() > 0) {
            unit_id = stack.get(stack.size() - 1);
        }
        ifmoRestClient.get(activity, "unit" + (unit_id.isEmpty() ? "" : "/" + unit_id), null, handler);
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

    private void display(final JSONObject json) {
        thread.runOnUI(() -> {
            try {
                JSONObject unit = JsonUtils.getJsonObject(json, "unit");
                JSONArray divisions = JsonUtils.getJsonArray(json, "divisions");
                draw(R.layout.layout_university_list_finite);
                // заголовок
                if (stack.size() == 0 || unit == null) {
                    ((ImageView) ((ViewGroup) container.findViewById(R.id.back)).getChildAt(0)).setImageResource(R.drawable.ic_refresh);
                    container.findViewById(R.id.back).setOnClickListener(v -> load(true));
                    ((TextView) container.findViewById(R.id.title)).setText(R.string.unit_general);
                    staticUtil.removeView(container.findViewById(R.id.web));
                } else {
                    container.findViewById(R.id.back).setOnClickListener(v -> {
                        stack.remove(stack.size() - 1);
                        load();
                    });
                    final String name = JsonUtils.getString(unit, "unit_title");
                    if (name != null && !name.trim().isEmpty()) {
                        ((TextView) container.findViewById(R.id.title)).setText(name.trim());
                    } else {
                        staticUtil.removeView(container.findViewById(R.id.title));
                    }
                    final int unit_id = stack.size() > 0 ? JsonUtils.getInt(unit, "unit_id", -1) : -1;
                    final String link = isValid(unit_id) ? "http://www.ifmo.ru/ru/viewunit/" + unit_id + "/" : null;
                    if (link != null) {
                        container.findViewById(R.id.web).setOnClickListener(view -> {
                            eventBus.fire(new OpenIntentEvent(new Intent(Intent.ACTION_VIEW, Uri.parse(link.trim()))));
                        });
                    } else {
                        staticUtil.removeView(container.findViewById(R.id.web));
                    }
                }
                // список
                facultiesRecyclerViewAdapter = new UniversityFacultiesRVA(activity);
                final RecyclerView finite_list = container.findViewById(R.id.finite_list);
                if (finite_list != null) {
                    finite_list.setLayoutManager(new LinearLayoutManager(activity));
                    finite_list.setAdapter(facultiesRecyclerViewAdapter);
                    finite_list.addOnScrollListener(new RecyclerViewOnScrollListener(container));
                }
                displayContent(unit, divisions);
                if (timestamp > 0 && timestamp + 5000 < time.getCalendar().getTimeInMillis()) {
                    UniversityRVA.Item item = new UniversityRVA.Item();
                    item.type = UniversityRVA.TYPE_INFO_ABOUT_UPDATE_TIME;
                    item.data = new JSONObject().put("title", activity.getString(R.string.update_date) + " " + time.getUpdateTime(activity, timestamp));
                    facultiesRecyclerViewAdapter.addItem(item);
                }
                // добавляем отступ
                container.findViewById(R.id.top_panel).post(() -> {
                    try {
                        int height = container.findViewById(R.id.top_panel).getHeight();
                        if (finite_list != null) {
                            finite_list.setPadding(0, height, 0, 0);
                            finite_list.scrollToPosition(0);
                        }
                        LinearLayout finite_list_info = container.findViewById(R.id.finite_list_info);
                        if (finite_list_info != null && finite_list_info.getChildCount() > 0) {
                            finite_list_info.setPadding(0, height, 0, 0);
                        }
                    } catch (Exception ignore) {
                        // ignore
                    }
                });
                // работаем со свайпом
                SwipeRefreshLayout mSwipeRefreshLayout = container.findViewById(R.id.finite_list_swipe);
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

    private void displayContent(final JSONObject unit, final JSONArray divisions) {
        thread.run(() -> {
            try {
                final ArrayList<UniversityFacultiesRVA.Item> items = new ArrayList<>();
                if (unit != null) {
                    // основная информация
                    final String address = JsonUtils.getString(unit, "address");
                    final String phone = JsonUtils.getString(unit, "phone");
                    final String email = JsonUtils.getString(unit, "email");
                    final String site = JsonUtils.getString(unit, "site");
                    final String working_hours = JsonUtils.getString(unit, "working_hours");
                    if (isValid(address) || isValid(phone) || isValid(email) || isValid(site) || isValid(working_hours)) {
                        UniversityFacultiesRVA.Item item = new UniversityFacultiesRVA.Item();
                        item.type = UniversityFacultiesRVA.TYPE_UNIT_STRUCTURE_COMMON;
                        item.data = new JSONObject()
                                .put("header", activity.getString(R.string.faculty_section_general))
                                .put("address", isValid(address) ? address : null)
                                .put("phone", isValid(phone) ? phone : null)
                                .put("email", isValid(email) ? email : null)
                                .put("site", isValid(site) ? site : null)
                                .put("working_hours", isValid(working_hours) ? working_hours : null);
                        items.add(item);
                    }
                    // глава
                    final String head_post = JsonUtils.getString(unit, "post");
                    final String head_lastname = JsonUtils.getString(unit, "lastname");
                    final String head_firstname = JsonUtils.getString(unit, "firstname");
                    final String head_middlename = JsonUtils.getString(unit, "middlename");
                    final String head_avatar = JsonUtils.getString(unit, "avatar");
                    final int head_pid = JsonUtils.getInt(unit, "ifmo_person_id", -1);
                    if (isValid(head_lastname) || isValid(head_firstname) || isValid(head_middlename)) {
                        UniversityFacultiesRVA.Item item = new UniversityFacultiesRVA.Item();
                        item.type = UniversityFacultiesRVA.TYPE_UNIT_STRUCTURE_HEAD;
                        item.data = new JSONObject()
                                .put("header", isValid(head_post) ? head_post : activity.getString(R.string.faculty_section_head))
                                .put("head_lastname", isValid(head_lastname) ? head_lastname : null)
                                .put("head_firstname", isValid(head_firstname) ? head_firstname : null)
                                .put("head_middlename", isValid(head_middlename) ? head_middlename : null)
                                .put("head_avatar", isValid(head_avatar) ? head_avatar : null)
                                .put("head_pid", isValid(head_pid) ? head_pid : -1);
                        items.add(item);
                    }
                }
                if (divisions != null && divisions.length() > 0) {
                    // дивизионы
                    JSONArray d = new JSONArray();
                    for (int i = 0; i < divisions.length(); i++) {
                        final JSONObject division = divisions.getJSONObject(i);
                        d.put(new JSONObject()
                                .put("title", JsonUtils.getString(division, "unit_title"))
                                .put("id", JsonUtils.getInt(division, "unit_id", -1))
                        );
                    }
                    UniversityFacultiesRVA.Item item = new UniversityFacultiesRVA.Item();
                    item.type = UniversityFacultiesRVA.TYPE_UNIT_DIVISIONS;
                    item.data = new JSONObject()
                            .put("header", stack.size() == 0 ? null : activity.getString(R.string.faculty_section_divisions))
                            .put("divisions", d);
                    items.add(item);
                }
                if (items.size() == 0) {
                    items.add(new UniversityRVA.Item(UniversityFacultiesRVA.TYPE_NO_DATA, null));
                }
                thread.runOnUI(() -> {
                    if (facultiesRecyclerViewAdapter != null) {
                        facultiesRecyclerViewAdapter.setOnDivisionClickListener(dep_id -> {
                            stack.add(String.valueOf(dep_id));
                            load();
                        });
                        facultiesRecyclerViewAdapter.addItem(items);
                    }
                });
            } catch (Exception e) {
                log.exception(e);
                loadFailed();
            }
        });

    }

    private boolean isValid(String text) {
        return text != null && !text.trim().isEmpty();
    }

    private boolean isValid(int number) {
        return number >= 0;
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

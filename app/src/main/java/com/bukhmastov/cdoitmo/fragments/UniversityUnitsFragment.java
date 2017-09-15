package com.bukhmastov.cdoitmo.fragments;

import android.app.Activity;
import android.content.Context;
import android.content.Intent;
import android.net.Uri;
import android.os.Bundle;
import android.support.annotation.LayoutRes;
import android.support.annotation.Nullable;
import android.support.v4.app.Fragment;
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

import com.bukhmastov.cdoitmo.R;
import com.bukhmastov.cdoitmo.adapters.FacultiesRecyclerViewAdapter;
import com.bukhmastov.cdoitmo.adapters.RecyclerViewOnScrollListener;
import com.bukhmastov.cdoitmo.adapters.UniversityRecyclerViewAdapter;
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
import java.util.Calendar;

public class UniversityUnitsFragment extends Fragment implements SwipeRefreshLayout.OnRefreshListener {

    private static final String TAG = "UniversityUnitsFragment";
    private Activity activity;
    private View container;
    private Client.Request requestHandle = null;
    private boolean loaded = false;
    private final ArrayList<String> stack = new ArrayList<>();
    private final ArrayMap<String, String> history = new ArrayMap<>();
    private FacultiesRecyclerViewAdapter facultiesRecyclerViewAdapter = null;
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
    public View onCreateView(LayoutInflater inflater, ViewGroup cont, Bundle savedInstanceState) {
        View view = inflater.inflate(R.layout.fragment_university_tab, cont, false);
        container = view.findViewById(R.id.university_tab_container);
        return view;
    }

    @Override
    public void onRefresh() {
        Log.v(TAG, "refreshing");
        load(true);
    }

    private void load() {
        Static.T.runThread(new Runnable() {
            @Override
            public void run() {
                load(Storage.pref.get(activity, "pref_use_cache", true) && Storage.pref.get(activity, "pref_use_university_cache", false)
                        ? Integer.parseInt(Storage.pref.get(activity, "pref_static_refresh", "168"))
                        : 0);
            }
        });
    }
    private void load(final int refresh_rate) {
        Static.T.runThread(new Runnable() {
            @Override
            public void run() {
                Log.v(TAG, "load | refresh_rate=" + refresh_rate);
                String uid = stack.size() == 0 ? "0" : stack.get(stack.size() - 1);
                if (Storage.pref.get(activity, "pref_use_cache", true) && Storage.pref.get(activity, "pref_use_university_cache", false)) {
                    String cache = Storage.file.cache.get(activity, "university#units#" + uid).trim();
                    if (!cache.isEmpty()) {
                        try {
                            JSONObject cacheJson = new JSONObject(cache);
                            timestamp = cacheJson.getLong("timestamp");
                            if (timestamp + refresh_rate * 3600000L < Calendar.getInstance().getTimeInMillis()) {
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
                final String uid = stack.size() == 0 ? "0" : stack.get(stack.size() - 1);
                if (!force && history.containsKey(uid)) {
                    try {
                        Log.v(TAG, "load | from local cache");
                        String local = history.get(uid);
                        JSONObject localObj = new JSONObject(local);
                        timestamp = Calendar.getInstance().getTimeInMillis();
                        display(localObj);
                        return;
                    } catch (Exception e) {
                        Log.v(TAG, "load | failed to load from local cache");
                        history.remove(uid);
                    }
                }
                if ((!force || !Static.isOnline(activity)) && Storage.pref.get(activity, "pref_use_cache", true) && Storage.pref.get(activity, "pref_use_university_cache", false)) {
                    try {
                        String c = cache.isEmpty() ? Storage.file.cache.get(activity, "university#units#" + uid).trim() : cache;
                        if (!c.isEmpty()) {
                            Log.v(TAG, "load | from cache");
                            display(new JSONObject(c).getJSONObject("data"));
                            return;
                        }
                    } catch (Exception e) {
                        Log.v(TAG, "load | failed to load from cache");
                        Storage.file.cache.delete(activity, "university#units#" + uid);
                    }
                }
                if (!Static.OFFLINE_MODE) {
                    loadProvider(new RestResponseHandler() {
                        @Override
                        public void onSuccess(final int statusCode, final Client.Headers headers, final JSONObject json, final JSONArray responseArr) {
                            Static.T.runThread(new Runnable() {
                                @Override
                                public void run() {
                                    if (statusCode == 200) {
                                        long now = Calendar.getInstance().getTimeInMillis();
                                        if (json != null && Storage.pref.get(activity, "pref_use_cache", true) && Storage.pref.get(activity, "pref_use_university_cache", false)) {
                                            try {
                                                Storage.file.cache.put(activity, "university#units#" + uid, new JSONObject()
                                                        .put("timestamp", now)
                                                        .put("data", json)
                                                        .toString()
                                                );
                                            } catch (JSONException e) {
                                                Static.error(e);
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
                                }
                            });
                        }
                        @Override
                        public void onFailure(final int statusCode, final Client.Headers headers, final int state) {
                            Static.T.runOnUiThread(new Runnable() {
                                @Override
                                public void run() {
                                    Log.v(TAG, "forceLoad | failure " + state);
                                    switch (state) {
                                        case IfmoRestClient.FAILED_OFFLINE:
                                            draw(R.layout.state_offline);
                                            if (activity != null) {
                                                View offline_reload = container.findViewById(R.id.offline_reload);
                                                if (offline_reload != null) {
                                                    offline_reload.setOnClickListener(new View.OnClickListener() {
                                                        @Override
                                                        public void onClick(View v) {
                                                            load();
                                                        }
                                                    });
                                                }
                                            }
                                            break;
                                        case IfmoRestClient.FAILED_TRY_AGAIN:
                                            draw(R.layout.state_try_again);
                                            if (activity != null) {
                                                View try_again_reload = container.findViewById(R.id.try_again_reload);
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
                        public void onProgress(final int state) {
                            Static.T.runOnUiThread(new Runnable() {
                                @Override
                                public void run() {
                                    Log.v(TAG, "forceLoad | progress " + state);
                                    draw(R.layout.state_loading);
                                    if (activity != null) {
                                        TextView loading_message = container.findViewById(R.id.loading_message);
                                        if (loading_message != null) {
                                            switch (state) {
                                                case IfmoRestClient.STATE_HANDLING: loading_message.setText(R.string.loading); break;
                                            }
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
                    Static.T.runOnUiThread(new Runnable() {
                        @Override
                        public void run() {
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
                    });
                }
            }
        });
    }
    private void loadProvider(RestResponseHandler handler) {
        Log.v(TAG, "loadProvider");
        String unit_id = "";
        if (stack.size() > 0) {
            unit_id = stack.get(stack.size() - 1);
        }
        IfmoRestClient.get(activity, "unit" + (unit_id.isEmpty() ? "" : "/" + unit_id), null, handler);
    }
    private void loadFailed() {
        Static.T.runOnUiThread(new Runnable() {
            @Override
            public void run() {
                Log.v(TAG, "loadFailed");
                try {
                    draw(R.layout.state_try_again);
                    TextView try_again_message = container.findViewById(R.id.try_again_message);
                    if (try_again_message != null) try_again_message.setText(R.string.load_failed);
                    View try_again_reload = container.findViewById(R.id.try_again_reload);
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
    private void display(final JSONObject json) {
        final SwipeRefreshLayout.OnRefreshListener onRefreshListener = this;
        Static.T.runOnUiThread(new Runnable() {
            @Override
            public void run() {
                try {
                    JSONObject unit = getJsonObject(json, "unit");
                    JSONArray divisions = getJsonArray(json, "divisions");
                    draw(R.layout.layout_university_faculties);
                    // заголовок
                    if (stack.size() == 0 || unit == null) {
                        ((ImageView) ((ViewGroup) container.findViewById(R.id.back)).getChildAt(0)).setImageResource(R.drawable.ic_refresh);
                        container.findViewById(R.id.back).setOnClickListener(new View.OnClickListener() {
                            @Override
                            public void onClick(View v) {
                                load(true);
                            }
                        });
                        ((TextView) container.findViewById(R.id.title)).setText(R.string.unit_general);
                        Static.removeView(container.findViewById(R.id.web));
                    } else {
                        container.findViewById(R.id.back).setOnClickListener(new View.OnClickListener() {
                            @Override
                            public void onClick(View v) {
                                stack.remove(stack.size() - 1);
                                load();
                            }
                        });
                        final String name = getString(unit, "unit_title");
                        if (name != null && !name.trim().isEmpty()) {
                            ((TextView) container.findViewById(R.id.title)).setText(name.trim());
                        } else {
                            Static.removeView(container.findViewById(R.id.title));
                        }
                        final int unit_id = stack.size() > 0 ? getInt(unit, "unit_id") : -1;
                        final String link = isValid(unit_id) ? "http://www.ifmo.ru/ru/viewunit/" + unit_id + "/" : null;
                        if (link != null) {
                            container.findViewById(R.id.web).setOnClickListener(new View.OnClickListener() {
                                @Override
                                public void onClick(View view) {
                                    startActivity(new Intent(Intent.ACTION_VIEW, Uri.parse(link.trim())));
                                }
                            });
                        } else {
                            Static.removeView(container.findViewById(R.id.web));
                        }
                    }
                    // список
                    facultiesRecyclerViewAdapter = new FacultiesRecyclerViewAdapter(activity);
                    final RecyclerView list = container.findViewById(R.id.list);
                    list.setLayoutManager(new LinearLayoutManager(activity));
                    list.setAdapter(facultiesRecyclerViewAdapter);
                    list.addOnScrollListener(new RecyclerViewOnScrollListener(container));
                    displayContent(unit, divisions);
                    if (timestamp > 0 && timestamp + 5000 < Calendar.getInstance().getTimeInMillis()) {
                        UniversityRecyclerViewAdapter.Item item = new UniversityRecyclerViewAdapter.Item();
                        item.type = UniversityRecyclerViewAdapter.TYPE_INFO_ABOUT_UPDATE_TIME;
                        item.data = new JSONObject().put("title", activity.getString(R.string.update_date) + " " + Static.getUpdateTime(activity, timestamp));
                        facultiesRecyclerViewAdapter.addItem(item);
                    }
                    // добавляем отступ
                    container.findViewById(R.id.top_panel).post(new Runnable() {
                        @Override
                        public void run() {
                            try {
                                int height = container.findViewById(R.id.top_panel).getHeight();
                                RecyclerView list = container.findViewById(R.id.list);
                                list.setPadding(0, height, 0, 0);
                                list.scrollToPosition(0);
                                LinearLayout list_info = container.findViewById(R.id.list_info);
                                if (list_info.getChildCount() > 0) {
                                    list_info.setPadding(0, height, 0, 0);
                                }
                            } catch (Exception ignore) {
                                // ignore
                            }
                        }
                    });
                    // работаем со свайпом
                    SwipeRefreshLayout mSwipeRefreshLayout = container.findViewById(R.id.list_swipe);
                    if (mSwipeRefreshLayout != null) {
                        mSwipeRefreshLayout.setColorSchemeColors(Static.colorAccent);
                        mSwipeRefreshLayout.setProgressBackgroundColorSchemeColor(Static.colorBackgroundRefresh);
                        mSwipeRefreshLayout.setOnRefreshListener(onRefreshListener);
                    }
                } catch (Exception e) {
                    Static.error(e);
                    loadFailed();
                }
            }
        });
    }
    private void displayContent(final JSONObject unit, final JSONArray divisions) {
        Static.T.runThread(new Runnable() {
            @Override
            public void run() {
                try {
                    final ArrayList<FacultiesRecyclerViewAdapter.Item> items = new ArrayList<>();
                    if (unit != null) {
                        // основная информация
                        final String address = getString(unit, "address");
                        final String phone = getString(unit, "phone");
                        final String email = getString(unit, "email");
                        final String site = getString(unit, "site");
                        final String working_hours = getString(unit, "working_hours");
                        if (isValid(address) || isValid(phone) || isValid(email) || isValid(site) || isValid(working_hours)) {
                            FacultiesRecyclerViewAdapter.Item item = new FacultiesRecyclerViewAdapter.Item();
                            item.type = FacultiesRecyclerViewAdapter.TYPE_UNIT_STRUCTURE_COMMON;
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
                        final String head_post = getString(unit, "post");
                        final String head_lastname = getString(unit, "lastname");
                        final String head_firstname = getString(unit, "firstname");
                        final String head_middlename = getString(unit, "middlename");
                        final String head_avatar = getString(unit, "avatar");
                        final int head_pid = getInt(unit, "ifmo_person_id");
                        if (isValid(head_lastname) || isValid(head_firstname) || isValid(head_middlename)) {
                            FacultiesRecyclerViewAdapter.Item item = new FacultiesRecyclerViewAdapter.Item();
                            item.type = FacultiesRecyclerViewAdapter.TYPE_UNIT_STRUCTURE_HEAD;
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
                                    .put("title", getString(division, "unit_title"))
                                    .put("id", getInt(division, "unit_id"))
                            );
                        }
                        FacultiesRecyclerViewAdapter.Item item = new FacultiesRecyclerViewAdapter.Item();
                        item.type = FacultiesRecyclerViewAdapter.TYPE_UNIT_DIVISIONS;
                        item.data = new JSONObject()
                                .put("header", stack.size() == 0 ? null : activity.getString(R.string.faculty_section_divisions))
                                .put("divisions", d);
                        items.add(item);
                    }
                    Static.T.runOnUiThread(new Runnable() {
                        @Override
                        public void run() {
                            if (facultiesRecyclerViewAdapter != null) {
                                facultiesRecyclerViewAdapter.setOnDivisionClickListener(new FacultiesRecyclerViewAdapter.OnDivisionClickListener() {
                                    @Override
                                    public void onClick(int dep_id) {
                                        stack.add(String.valueOf(dep_id));
                                        load();
                                    }
                                });
                                facultiesRecyclerViewAdapter.addItem(items);
                            }
                        }
                    });
                } catch (Exception e) {
                    Static.error(e);
                    loadFailed();
                }
            }
        });

    }
    private boolean isValid(String text) {
        return text != null && !text.trim().isEmpty();
    }
    private boolean isValid(int number) {
        return number >= 0;
    }

    private JSONObject getJsonObject(JSONObject json, String key) throws JSONException {
        if (json.has(key)) {
            Object object = json.get(key);
            if (object == null) {
                return null;
            } else {
                try {
                    return (JSONObject) object;
                } catch (Exception e) {
                    return null;
                }
            }
        } else {
            return null;
        }
    }
    private JSONArray getJsonArray(JSONObject json, String key) throws JSONException {
        if (json.has(key)) {
            Object object = json.get(key);
            if (object == null) {
                return null;
            } else {
                try {
                    return (JSONArray) object;
                } catch (Exception e) {
                    return null;
                }
            }
        } else {
            return null;
        }
    }
    private String getString(JSONObject json, String key) throws JSONException {
        if (json.has(key)) {
            Object object = json.get(key);
            if (object == null) {
                return null;
            } else {
                try {
                    return (String) object;
                } catch (Exception e) {
                    return null;
                }
            }
        } else {
            return null;
        }
    }
    private int getInt(JSONObject json, String key) throws JSONException {
        if (json.has(key)) {
            try {
                return json.getInt(key);
            } catch (Exception e) {
                return -1;
            }
        } else {
            return -1;
        }
    }

    private void draw(final int layoutId) {
        Static.T.runOnUiThread(new Runnable() {
            @Override
            public void run() {
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
        });
    }
    private View inflate(@LayoutRes int layoutId) throws InflateException {
        return ((LayoutInflater) activity.getSystemService(Context.LAYOUT_INFLATER_SERVICE)).inflate(layoutId, null);
    }
}

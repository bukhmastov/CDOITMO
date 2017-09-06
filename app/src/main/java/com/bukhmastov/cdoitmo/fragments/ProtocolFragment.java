package com.bukhmastov.cdoitmo.fragments;

import android.content.Context;
import android.os.Bundle;
import android.support.v4.widget.SwipeRefreshLayout;
import android.util.SparseArray;
import android.view.InflateException;
import android.view.LayoutInflater;
import android.view.MenuItem;
import android.view.View;
import android.view.ViewGroup;
import android.widget.AdapterView;
import android.widget.ArrayAdapter;
import android.widget.LinearLayout;
import android.widget.ListView;
import android.widget.Spinner;
import android.widget.TextView;

import com.bukhmastov.cdoitmo.R;
import com.bukhmastov.cdoitmo.activities.MainActivity;
import com.bukhmastov.cdoitmo.adapters.ProtocolListView;
import com.bukhmastov.cdoitmo.converters.ProtocolConverter;
import com.bukhmastov.cdoitmo.firebase.FirebaseAnalyticsProvider;
import com.bukhmastov.cdoitmo.network.DeIfmoRestClient;
import com.bukhmastov.cdoitmo.network.interfaces.DeIfmoRestClientResponseHandler;
import com.bukhmastov.cdoitmo.utils.Log;
import com.bukhmastov.cdoitmo.utils.Static;
import com.bukhmastov.cdoitmo.utils.Storage;
import com.loopj.android.http.RequestHandle;

import org.json.JSONArray;
import org.json.JSONException;
import org.json.JSONObject;

import java.util.ArrayList;
import java.util.Calendar;
import java.util.HashMap;
import java.util.Objects;

public class ProtocolFragment extends ConnectedFragment implements SwipeRefreshLayout.OnRefreshListener {

    private static final String TAG = "ProtocolFragment";
    public static JSONObject data = null;
    private static final int maxAttempts = 3;
    private int number_of_weeks = 1;
    private boolean spinner_weeks_blocker = true;
    private boolean loaded = false;
    private RequestHandle fragmentRequestHandle = null;

    @Override
    public void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        Log.v(TAG, "Fragment created");
        FirebaseAnalyticsProvider.logCurrentScreen(activity, this);
        number_of_weeks = Integer.parseInt(Storage.pref.get(activity, "pref_protocol_changes_weeks", "1"));
    }

    @Override
    public void onDestroy() {
        super.onDestroy();
        Log.v(TAG, "Fragment destroyed");
        try {
            if (MainActivity.menu != null) {
                MenuItem simple = MainActivity.menu.findItem(R.id.action_protocol_changes_switch_to_simple);
                MenuItem advanced = MainActivity.menu.findItem(R.id.action_protocol_changes_switch_to_advanced);
                if (simple != null) simple.setVisible(false);
                if (advanced != null) advanced.setVisible(false);
            }
        } catch (Exception e){
            Static.error(e);
        }
    }

    @Override
    public View onCreateView(LayoutInflater inflater, ViewGroup container, Bundle savedInstanceState) {
        return inflater.inflate(R.layout.fragment_protocol, container, false);
    }

    @Override
    public void onResume() {
        super.onResume();
        Log.v(TAG, "resumed");
        FirebaseAnalyticsProvider.setCurrentScreen(activity, this);
        try {
            if (MainActivity.menu != null) {
                final MenuItem simple = MainActivity.menu.findItem(R.id.action_protocol_changes_switch_to_simple);
                final MenuItem advanced = MainActivity.menu.findItem(R.id.action_protocol_changes_switch_to_advanced);
                if (simple != null && advanced != null) {
                    switch (Storage.pref.get(activity, "pref_protocol_changes_mode", "advanced")) {
                        case "simple": advanced.setVisible(true); break;
                        case "advanced": simple.setVisible(true); break;
                    }
                    simple.setOnMenuItemClickListener(new MenuItem.OnMenuItemClickListener() {
                        @Override
                        public boolean onMenuItemClick(MenuItem item) {
                            Storage.pref.put(activity, "pref_protocol_changes_mode", "simple");
                            simple.setVisible(false);
                            advanced.setVisible(true);
                            load(false);
                            return false;
                        }
                    });
                    advanced.setOnMenuItemClickListener(new MenuItem.OnMenuItemClickListener() {
                        @Override
                        public boolean onMenuItemClick(MenuItem item) {
                            Storage.pref.put(activity, "pref_protocol_changes_mode", "advanced");
                            simple.setVisible(true);
                            advanced.setVisible(false);
                            load(false);
                            return false;
                        }
                    });
                }
            }
        } catch (Exception e){
            Static.error(e);
        }
        if (!loaded) {
            loaded = true;
            load();
        }
    }

    @Override
    public void onPause() {
        super.onPause();
        Log.v(TAG, "paused");
        if (fragmentRequestHandle != null) {
            loaded = false;
            fragmentRequestHandle.cancel(true);
        }
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
                load(Storage.pref.get(activity, "pref_use_cache", true) ? Integer.parseInt(Storage.pref.get(activity, "pref_dynamic_refresh", "0")) : 0);
            }
        });
    }
    private void load(final int refresh_rate) {
        Static.T.runThread(new Runnable() {
            @Override
            public void run() {
                Log.v(TAG, "load | refresh_rate=" + refresh_rate);
                if (Storage.pref.get(activity, "pref_use_cache", true)) {
                    String cache = Storage.file.cache.get(activity, "protocol#core").trim();
                    if (!cache.isEmpty()) {
                        try {
                            data = new JSONObject(cache);
                            if (data.getLong("timestamp") + refresh_rate * 3600000L < Calendar.getInstance().getTimeInMillis()) {
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
                load(force, "", 0);
            }
        });
    }
    private void load(final boolean force, final String cache) {
        Static.T.runThread(new Runnable() {
            @Override
            public void run() {
                load(force, cache, 0);
            }
        });
    }
    private void load(final boolean force, final String cache, final int attempt) {
        Static.T.runThread(new Runnable() {
            @Override
            public void run() {
                Log.v(TAG, "load | force=" + (force ? "true" : "false") + " | attempt=" + attempt);
                if ((!force || !Static.isOnline(activity)) && Storage.pref.get(activity, "pref_use_cache", true)) {
                    try {
                        String c = cache.isEmpty() ? Storage.file.cache.get(activity, "protocol#core").trim() : cache;
                        if (!c.isEmpty()) {
                            Log.v(TAG, "load | from cache");
                            JSONObject d = new JSONObject(c);
                            if (d.getInt("number_of_weeks") == number_of_weeks || !Static.isOnline(activity) || attempt >= maxAttempts) {
                                data = new JSONObject(c);
                                display();
                                return;
                            }
                        }
                    } catch (Exception e) {
                        Log.v(TAG, "load | failed to load from cache");
                        Storage.file.cache.delete(activity, "protocol#core");
                    }
                }
                if (!Static.OFFLINE_MODE) {
                    if (attempt >= maxAttempts) {
                        if (force) {
                            load(false, cache, attempt + 1);
                        } else {
                            if (data != null) {
                                display();
                            } else {
                                loadFailed();
                            }
                        }
                    } else {
                        DeIfmoRestClient.get(activity, "eregisterlog?days=" + String.valueOf(number_of_weeks * 7), null, new DeIfmoRestClientResponseHandler() {
                            @Override
                            public void onSuccess(final int statusCode, final JSONObject responseObj, final JSONArray responseArr) {
                                Static.T.runThread(new Runnable() {
                                    @Override
                                    public void run() {
                                        Log.v(TAG, "load | success | statusCode=" + statusCode + " | responseArr=" + (responseArr == null ? "null" : "notnull"));
                                        if (statusCode == 200 && responseArr != null) {
                                            new ProtocolConverter(activity, responseArr, number_of_weeks, new ProtocolConverter.response() {
                                                @Override
                                                public void finish(JSONObject json) {
                                                    try {
                                                        if (Storage.pref.get(activity, "pref_use_cache", true)) {
                                                            Storage.file.cache.put(activity, "protocol#core", json.toString());
                                                            Storage.file.perm.put(activity, "protocol_tracker#protocol", json.getJSONArray("protocol").toString());
                                                        }
                                                    } catch (JSONException e) {
                                                        Static.error(e);
                                                    }
                                                    data = json;
                                                    display();
                                                }
                                            }).run();
                                        } else {
                                            load(force, cache, attempt + 1);
                                        }
                                    }
                                });
                            }
                            @Override
                            public void onProgress(final int state) {
                                Static.T.runOnUiThread(new Runnable() {
                                    @Override
                                    public void run() {
                                        Log.v(TAG, "load | progress " + state);
                                        draw(R.layout.state_loading);
                                        if (activity != null) {
                                            TextView loading_message = activity.findViewById(R.id.loading_message);
                                            if (loading_message != null) {
                                                switch (state) {
                                                    case DeIfmoRestClient.STATE_HANDLING:
                                                        loading_message.setText(R.string.loading);
                                                        break;
                                                }
                                            }
                                        }
                                    }
                                });
                            }
                            @Override
                            public void onFailure(final int statusCode, final int state) {
                                Static.T.runOnUiThread(new Runnable() {
                                    @Override
                                    public void run() {
                                        Log.v(TAG, "load | failure " + state);
                                        switch (state) {
                                            case DeIfmoRestClient.FAILED_OFFLINE:
                                                if (data != null) {
                                                    display();
                                                } else {
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
                                                break;
                                            case DeIfmoRestClient.FAILED_TRY_AGAIN:
                                                draw(R.layout.state_try_again);
                                                if (activity != null) {
                                                    View try_again_reload = activity.findViewById(R.id.try_again_reload);
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
                            public void onNewHandle(RequestHandle requestHandle) {
                                fragmentRequestHandle = requestHandle;
                            }
                        });
                    }
                } else {
                    Static.T.runOnUiThread(new Runnable() {
                        @Override
                        public void run() {
                            if (data != null) {
                                display();
                            } else {
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
                        }
                    });
                }

            }
        });
    }
    private void loadFailed() {
        Static.T.runOnUiThread(new Runnable() {
            @Override
            public void run() {
                Log.v(TAG, "loadFailed");
                try {
                    draw(R.layout.state_try_again);
                    TextView try_again_message = activity.findViewById(R.id.try_again_message);
                    if (try_again_message != null) try_again_message.setText(R.string.load_failed_retry_in_minute);
                    View try_again_reload = activity.findViewById(R.id.try_again_reload);
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
    private void display() {
        Static.T.runThread(new Runnable() {
            @Override
            public void run() {
                Log.v(TAG, "display");
                try {
                    if (data == null) throw new NullPointerException("data cannot be null");
                    // отображаем нужный режим
                    switch (Storage.pref.get(activity, "pref_protocol_changes_mode", "advanced")) {
                        case "simple": {
                            // получаем список предметов для отображения
                            number_of_weeks = data.getInt("number_of_weeks");
                            JSONArray jsonArray = data.getJSONArray("protocol");
                            final ArrayList<HashMap<String, String>> changes = new ArrayList<>();
                            for (int i = 0; i < jsonArray.length(); i++) {
                                JSONObject jsonObject = jsonArray.getJSONObject(i);
                                HashMap<String, String> hashMap = new HashMap<>();
                                JSONObject var = jsonObject.getJSONObject("var");
                                hashMap.put("name", jsonObject.getString("subject"));
                                hashMap.put("desc", var.getString("name") + " [" + var.getString("min") + "/" + var.getString("threshold") + "/" + var.getString("max") + "]");
                                hashMap.put("meta", (Objects.equals(jsonObject.getString("sign"), "..") ? "" : jsonObject.getString("sign") + " | ") + jsonObject.getString("date"));
                                hashMap.put("value", jsonObject.getString("value"));
                                hashMap.put("delta", jsonObject.getString("cdoitmo_delta"));
                                hashMap.put("delta_here", jsonObject.getDouble("cdoitmo_delta_double") == 0 ? "false" : "true");
                                hashMap.put("delta_negative", jsonObject.getDouble("cdoitmo_delta_double") < 0 ? "true" : "false");
                                changes.add(hashMap);
                            }
                            Static.T.runOnUiThread(new Runnable() {
                                @Override
                                public void run() {
                                    try {
                                        // отображаем интерфейс
                                        draw(R.layout.protocol_layout);
                                        // отображаем интерфейс простого режима
                                        ViewGroup protocol_container = activity.findViewById(R.id.protocol_container);
                                        if (protocol_container == null) throw new NullPointerException("protocol_container is null");
                                        protocol_container.addView(inflate(R.layout.protocol_layout_mode_simple));
                                        // работаем со списком
                                        ListView pl_list_view = activity.findViewById(R.id.pl_list_view);
                                        if (pl_list_view != null) {
                                            if (changes.size() == 0) {
                                                ViewGroup swipe_container = activity.findViewById(R.id.swipe_container);
                                                if (swipe_container != null) {
                                                    swipe_container.removeView(pl_list_view);
                                                    View view = inflate(R.layout.nothing_to_display);
                                                    ((TextView) view.findViewById(R.id.ntd_text)).setText(R.string.no_changes_for_period);
                                                    swipe_container.addView(view);
                                                }
                                            } else {
                                                pl_list_view.setAdapter(new ProtocolListView(activity, changes));
                                            }
                                        }
                                        displayCommonPart();
                                    } catch (Exception e) {
                                        Static.error(e);
                                        loadFailed();
                                    }
                                }
                            });
                            break;
                        }
                        case "advanced": {
                            // Формируем группированный список из изменений по предметам и датам
                            final JSONArray protocol = data.getJSONArray("protocol");
                            final SparseArray<JSONObject> groups = new SparseArray<>();
                            int key = 0;
                            for (int i = 0; i < protocol.length(); i++) {
                                JSONObject item = protocol.getJSONObject(i);
                                String subject = item.getString("subject");
                                String date = item.getString("date");
                                String token = subject + "#" + date;
                                boolean found = false;
                                for (int j = 0; j < groups.size(); j++) {
                                    JSONObject obj = groups.get(groups.keyAt(j));
                                    if (Objects.equals(obj.getString("token"), token)) {
                                        found = true;
                                        break;
                                    }
                                }
                                if (!found) {
                                    JSONObject obj = new JSONObject();
                                    obj.put("token", token);
                                    obj.put("subject", subject);
                                    obj.put("changes", new JSONArray());
                                    groups.append(key++, obj);
                                }
                                for (int j = 0; j < groups.size(); j++) {
                                    JSONObject obj = groups.get(groups.keyAt(j));
                                    if (Objects.equals(obj.getString("token"), token)) {
                                        obj.getJSONArray("changes").put(item);
                                        break;
                                    }
                                }
                            }
                            // Объединяем одинаковые предметы, идущие подряд
                            for (int i = 1; i < groups.size(); i++) {
                                JSONObject groupPrevious = groups.get(groups.keyAt(i - 1));
                                JSONObject group = groups.get(groups.keyAt(i));
                                if (Objects.equals(groupPrevious.getString("subject"), group.getString("subject"))) {
                                    JSONArray changesPrevious = groupPrevious.getJSONArray("changes");
                                    JSONArray changes = group.getJSONArray("changes");
                                    for (int j = 0; j < changes.length(); j++) {
                                        changesPrevious.put(changes.getJSONObject(j));
                                    }
                                    group.put("changes", changesPrevious);
                                    groups.remove(groups.keyAt(i - 1));
                                    i--;
                                }
                            }
                            Static.T.runOnUiThread(new Runnable() {
                                @Override
                                public void run() {
                                    try {
                                        // отображаем интерфейс
                                        draw(R.layout.protocol_layout);
                                        // Отображаем группированный список изменений
                                        ViewGroup protocol_container = activity.findViewById(R.id.protocol_container);
                                        if (protocol_container == null) throw new NullPointerException("");
                                        protocol_container.addView(inflate(R.layout.protocol_layout_mode_advanced));
                                        ViewGroup pl_advanced_container = activity.findViewById(R.id.pl_advanced_container);
                                        if (protocol.length() == 0) {
                                            ViewGroup swipe_container = activity.findViewById(R.id.swipe_container);
                                            if (swipe_container != null) {
                                                swipe_container.removeAllViews();
                                                View view = inflate(R.layout.nothing_to_display);
                                                ((TextView) view.findViewById(R.id.ntd_text)).setText(R.string.no_changes_for_period);
                                                swipe_container.addView(view);
                                            }
                                        } else {
                                            for (int j = 0; j < groups.size(); j++) {
                                                JSONObject group = groups.get(groups.keyAt(j));
                                                String title = group.getString("subject");
                                                JSONArray changes = group.getJSONArray("changes");
                                                LinearLayout header = (LinearLayout) inflate(R.layout.protocol_layout_mode_advanced_header);
                                                ((TextView) header.findViewById(R.id.lv_protocol_name)).setText(title);
                                                pl_advanced_container.addView(header);
                                                for (int i = 0; i < changes.length(); i++) {
                                                    JSONObject change = changes.getJSONObject(i);
                                                    JSONObject var = change.getJSONObject("var");
                                                    LinearLayout element = (LinearLayout) inflate(R.layout.protocol_layout_mode_advanced_change);
                                                    if (i == changes.length() - 1 && j != groups.size() - 1) {
                                                        Static.removeView(element.findViewById(R.id.lv_protocol_separator));
                                                    }
                                                    ((TextView) element.findViewById(R.id.lv_protocol_desc)).setText(var.getString("name") + " [" + var.getString("min") + "/" + var.getString("threshold") + "/" + var.getString("max") + "]");
                                                    ((TextView) element.findViewById(R.id.lv_protocol_meta)).setText(Objects.equals(change.getString("sign"), "..") ? "" : change.getString("sign") + " | " + change.getString("date"));
                                                    ((TextView) element.findViewById(R.id.lv_protocol_value)).setText(change.getString("value"));
                                                    TextView lv_protocol_delta = element.findViewById(R.id.lv_protocol_delta);
                                                    if (change.getDouble("cdoitmo_delta_double") != 0.0) {
                                                        lv_protocol_delta.setText(change.getString("cdoitmo_delta"));
                                                        try {
                                                            lv_protocol_delta.setTextColor(Static.resolveColor(activity, change.getDouble("cdoitmo_delta_double") < 0.0 ? R.attr.textColorDegrade : R.attr.textColorPassed));
                                                        } catch (Exception e) {
                                                            Static.error(e);
                                                        }
                                                    } else {
                                                        lv_protocol_delta.setWidth(0);
                                                        lv_protocol_delta.setHeight(0);
                                                    }
                                                    pl_advanced_container.addView(element);
                                                }
                                            }
                                        }
                                        displayCommonPart();
                                    } catch (Exception e) {
                                        Static.error(e);
                                        loadFailed();
                                    }
                                }
                            });
                            break;
                        }
                        default: {
                            Log.wtf(TAG, "preference pref_protocol_changes_mode with wrong value: " + Storage.pref.get(activity, "pref_protocol_changes_mode", "simple") + ". Going to reset preference.");
                            Storage.pref.put(activity, "pref_protocol_changes_mode", "advanced");
                            display();
                            break;
                        }
                    }
                } catch (Exception e) {
                    Static.error(e);
                    loadFailed();
                }
            }
        });
    }
    private void displayCommonPart() {
        final ProtocolFragment self = this;
        Static.T.runOnUiThread(new Runnable() {
            @Override
            public void run() {
                try {
                    // работаем со свайпом
                    SwipeRefreshLayout swipe_container = activity.findViewById(R.id.swipe_container);
                    if (swipe_container != null) {
                        swipe_container.setColorSchemeColors(Static.colorAccent);
                        swipe_container.setProgressBackgroundColorSchemeColor(Static.colorBackgroundRefresh);
                        swipe_container.setOnRefreshListener(self);
                    }
                    // работаем с раскрывающимся списком
                    Spinner spinner_weeks = activity.findViewById(R.id.pl_weeks_spinner);
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
                        spinner_weeks.setAdapter(new ArrayAdapter<>(activity, R.layout.spinner_layout, spinner_weeks_arr));
                        spinner_weeks.setSelection(data.getInt("number_of_weeks") - 1);
                        spinner_weeks_blocker = true;
                        spinner_weeks.setOnItemSelectedListener(new AdapterView.OnItemSelectedListener() {
                            public void onItemSelected(final AdapterView<?> parent, final View item, final int position, final long selectedId) {
                                Static.T.runThread(new Runnable() {
                                    @Override
                                    public void run() {
                                        if (spinner_weeks_blocker) {
                                            spinner_weeks_blocker = false;
                                            return;
                                        }
                                        number_of_weeks = spinner_weeks_arr_values.get(position);
                                        Log.v(TAG, "spinner_weeks clicked | number_of_weeks=" + number_of_weeks);
                                        load(true);
                                    }
                                });
                            }
                            public void onNothingSelected(AdapterView<?> parent) {}
                        });
                    }
                    Static.showUpdateTime(activity, data.getLong("timestamp"), false);
                } catch (Exception e) {
                    Static.error(e);
                    loadFailed();
                }
            }
        });
    }

    private void draw(final int layoutId) {
        Static.T.runOnUiThread(new Runnable() {
            @Override
            public void run() {
                try {
                    ViewGroup vg = activity.findViewById(R.id.container_protocol);
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
    private View inflate(int layoutId) throws InflateException {
        return ((LayoutInflater) activity.getSystemService(Context.LAYOUT_INFLATER_SERVICE)).inflate(layoutId, null);
    }
}

package com.bukhmastov.cdoitmo.fragments;

import android.content.Context;
import android.os.Bundle;
import android.os.Handler;
import android.os.Message;
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
import com.bukhmastov.cdoitmo.firebase.FirebaseAnalyticsProvider;
import com.bukhmastov.cdoitmo.network.DeIfmoRestClient;
import com.bukhmastov.cdoitmo.network.interfaces.DeIfmoRestClientResponseHandler;
import com.bukhmastov.cdoitmo.objects.Protocol;
import com.bukhmastov.cdoitmo.utils.Log;
import com.bukhmastov.cdoitmo.utils.Static;
import com.bukhmastov.cdoitmo.utils.Storage;
import com.loopj.android.http.RequestHandle;

import org.json.JSONArray;
import org.json.JSONObject;

import java.util.ArrayList;
import java.util.Calendar;
import java.util.HashMap;
import java.util.Objects;

public class ProtocolFragment extends ConnectedFragment implements SwipeRefreshLayout.OnRefreshListener {

    private static final String TAG = "ProtocolFragment";
    private Protocol protocol = null;
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
        number_of_weeks = Integer.parseInt(Storage.pref.get(getContext(), "pref_protocol_changes_weeks", "1"));
        protocol = new Protocol(activity);
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
        FirebaseAnalyticsProvider.setCurrentScreen(activity, this.getClass());
        try {
            if (MainActivity.menu != null) {
                final MenuItem simple = MainActivity.menu.findItem(R.id.action_protocol_changes_switch_to_simple);
                final MenuItem advanced = MainActivity.menu.findItem(R.id.action_protocol_changes_switch_to_advanced);
                if (simple != null && advanced != null) {
                    switch (Storage.pref.get(getContext(), "pref_protocol_changes_mode", "advanced")) {
                        case "simple": advanced.setVisible(true); break;
                        case "advanced": simple.setVisible(true); break;
                    }
                    simple.setOnMenuItemClickListener(new MenuItem.OnMenuItemClickListener() {
                        @Override
                        public boolean onMenuItemClick(MenuItem item) {
                            Storage.pref.put(getContext(), "pref_protocol_changes_mode", "simple");
                            simple.setVisible(false);
                            advanced.setVisible(true);
                            load(168);
                            return false;
                        }
                    });
                    advanced.setOnMenuItemClickListener(new MenuItem.OnMenuItemClickListener() {
                        @Override
                        public boolean onMenuItemClick(MenuItem item) {
                            Storage.pref.put(getContext(), "pref_protocol_changes_mode", "advanced");
                            simple.setVisible(true);
                            advanced.setVisible(false);
                            load(168);
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
        Log.v(TAG, "refreshed");
        forceLoad();
    }

    private void load(){
        load(Storage.pref.get(getContext(), "pref_use_cache", true) ? Integer.parseInt(Storage.pref.get(getContext(), "pref_tab_refresh", "0")) : 0);
    }
    private void load(final int refresh_rate){
        Log.v(TAG, "load | refresh_rate=" + refresh_rate);
        draw(R.layout.state_loading);
        protocol.is(new Protocol.Callback() {
            @Override
            public void onDone(JSONObject protocol) {}
            @Override
            public void onChecked(boolean is){
                Log.v(TAG, "load | protocol.is=" + (is ? "true" : "false"));
                if (!is || refresh_rate == 0) {
                    forceLoad();
                } else if (refresh_rate >= 0){
                    protocol.get(new Protocol.Callback() {
                        @Override
                        public void onDone(JSONObject p){
                            Log.v(TAG, "load | protocol.get=" + (p == null ? "null" : "notnull"));
                            try {
                                if (p == null) throw new Exception("protocol is null");
                                if (p.getLong("timestamp") + refresh_rate * 3600000L < Calendar.getInstance().getTimeInMillis()) {
                                    forceLoad();
                                } else {
                                    display();
                                }
                            } catch (Exception e) {
                                Static.error(e);
                                forceLoad();
                            }
                        }
                        @Override
                        public void onChecked(boolean is) {}
                    });
                } else {
                    display();
                }
            }
        }, number_of_weeks);
    }
    private void forceLoad(){
        forceLoad(0);
    }
    private void forceLoad(int attempt){
        Log.v(TAG, "forceLoad | attempt=" + attempt);
        if (!Static.OFFLINE_MODE) {
            if (++attempt > maxAttempts) {
                protocol.is(new Protocol.Callback() {
                    @Override
                    public void onDone(JSONObject protocol) {}
                    @Override
                    public void onChecked(boolean is) {
                        Log.v(TAG, "forceLoad | protocol.is=" + (is ? "true" : "false"));
                        if (is) {
                            display();
                        } else {
                            loadFailed();
                        }
                    }
                });
                return;
            }
            final int finalAttempt = attempt;
            DeIfmoRestClient.get(getContext(), "eregisterlog?days=" + String.valueOf(number_of_weeks * 7), null, new DeIfmoRestClientResponseHandler() {
                @Override
                public void onSuccess(int statusCode, JSONObject responseObj, JSONArray responseArr) {
                    Log.v(TAG, "forceLoad | success | statusCode=" + statusCode + " | responseArr=" + (responseArr == null ? "null" : "notnull"));
                    if (statusCode == 200 && responseArr != null) {
                        protocol.put(responseArr, number_of_weeks, new Handler(){
                            @Override
                            public void handleMessage (Message msg) {
                                display();
                            }
                        });
                    } else {
                        forceLoad(finalAttempt);
                    }
                }
                @Override
                public void onProgress(int state) {
                    Log.v(TAG, "forceLoad | progress " + state);
                    draw(R.layout.state_loading);
                    if (activity != null) {
                        TextView loading_message = (TextView) activity.findViewById(R.id.loading_message);
                        if (loading_message != null) {
                            switch (state) {
                                case DeIfmoRestClient.STATE_HANDLING: loading_message.setText(R.string.loading); break;
                            }
                        }
                    }
                }
                @Override
                public void onFailure(int state) {
                    Log.v(TAG, "forceLoad | failure " + state);
                    switch (state) {
                        case DeIfmoRestClient.FAILED_OFFLINE:
                            protocol.is(new Protocol.Callback() {
                                @Override
                                public void onDone(JSONObject protocol) {}
                                @Override
                                public void onChecked(boolean is){
                                    if (is) {
                                        display();
                                    } else {
                                        draw(R.layout.state_offline);
                                        if (activity != null) {
                                            View offline_reload = activity.findViewById(R.id.offline_reload);
                                            if (offline_reload != null) {
                                                offline_reload.setOnClickListener(new View.OnClickListener() {
                                                    @Override
                                                    public void onClick(View v) {
                                                        forceLoad();
                                                    }
                                                });
                                            }
                                        }
                                    }
                                }
                            }, number_of_weeks);
                            break;
                        case DeIfmoRestClient.FAILED_TRY_AGAIN:
                            draw(R.layout.state_try_again);
                            if (activity != null) {
                                View try_again_reload = activity.findViewById(R.id.try_again_reload);
                                if (try_again_reload != null) {
                                    try_again_reload.setOnClickListener(new View.OnClickListener() {
                                        @Override
                                        public void onClick(View v) {
                                            forceLoad();
                                        }
                                    });
                                }
                            }
                            break;
                    }
                }
                @Override
                public void onNewHandle(RequestHandle requestHandle) {
                    fragmentRequestHandle = requestHandle;
                }
            });
        } else {
            protocol.is(new Protocol.Callback() {
                @Override
                public void onDone(JSONObject protocol) {}
                @Override
                public void onChecked(boolean is) {
                    Log.v(TAG, "forceLoad | protocol.is=" + (is ? "true" : "false"));
                    if (is) {
                        display();
                    } else {
                        draw(R.layout.state_offline);
                        if (activity != null) {
                            View offline_reload = activity.findViewById(R.id.offline_reload);
                            if (offline_reload != null) {
                                offline_reload.setOnClickListener(new View.OnClickListener() {
                                    @Override
                                    public void onClick(View v) {
                                        forceLoad();
                                    }
                                });
                            }
                        }
                    }
                }
            });
        }
    }
    private void loadFailed(){
        Log.v(TAG, "loadFailed");
        try {
            draw(R.layout.state_try_again);
            TextView try_again_message = (TextView) activity.findViewById(R.id.try_again_message);
            if (try_again_message != null) try_again_message.setText(R.string.load_failed_retry_in_minute);
            View try_again_reload = activity.findViewById(R.id.try_again_reload);
            if (try_again_reload != null) {
                try_again_reload.setOnClickListener(new View.OnClickListener() {
                    @Override
                    public void onClick(View v) {
                        forceLoad();
                    }
                });
            }
        } catch (Exception e) {
            Static.error(e);
        }
    }
    private void display(){
        Log.v(TAG, "display");
        final ProtocolFragment self = this;
        protocol.get(new Protocol.Callback() {
            @Override
            public void onDone(JSONObject data){
                Log.v(TAG, "display | protocol.get=" + (data == null ? "null" : "notnull"));
                try {
                    if (data == null) throw new NullPointerException("Protocol.protocol can't be null");
                    // отображаем интерфейс
                    draw(R.layout.protocol_layout);
                    // отображаем нужный режим
                    switch (Storage.pref.get(getContext(), "pref_protocol_changes_mode", "advanced")) {
                        case "simple": {
                            ViewGroup protocol_container = (ViewGroup) activity.findViewById(R.id.protocol_container);
                            if (protocol_container == null) throw new NullPointerException("");
                            protocol_container.addView(inflate(R.layout.protocol_layout_mode_simple));
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
                            // работаем со списком
                            ListView pl_list_view = (ListView) activity.findViewById(R.id.pl_list_view);
                            if (pl_list_view != null) {
                                if (changes.size() > 0) {
                                    pl_list_view.setAdapter(new ProtocolListView(activity, changes));
                                } else {
                                    ViewGroup mSwipeRefreshLayout = (SwipeRefreshLayout) activity.findViewById(R.id.swipe_container);
                                    if (mSwipeRefreshLayout != null) {
                                        mSwipeRefreshLayout.removeView(pl_list_view);
                                        View view = inflate(R.layout.nothing_to_display);
                                        ((TextView) view.findViewById(R.id.ntd_text)).setText(R.string.no_changes_for_period);
                                        mSwipeRefreshLayout.addView(view);
                                    }
                                }
                            }
                            break;
                        }
                        case "advanced": {
                            // Формируем группированный список из изменений по предметам и датам
                            JSONArray protocol = data.getJSONArray("protocol");
                            SparseArray<JSONObject> groups = new SparseArray<>();
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
                            // Отображаем группированный список изменений
                            ViewGroup protocol_container = (ViewGroup) activity.findViewById(R.id.protocol_container);
                            if (protocol_container == null) throw new NullPointerException("");
                            protocol_container.addView(inflate(R.layout.protocol_layout_mode_advanced));
                            ViewGroup pl_advanced_container = (ViewGroup) activity.findViewById(R.id.pl_advanced_container);
                            if (protocol.length() == 0) {
                                ViewGroup mSwipeRefreshLayout = (SwipeRefreshLayout) activity.findViewById(R.id.swipe_container);
                                if (mSwipeRefreshLayout != null) {
                                    mSwipeRefreshLayout.removeAllViews();
                                    View view = inflate(R.layout.nothing_to_display);
                                    ((TextView) view.findViewById(R.id.ntd_text)).setText(R.string.no_changes_for_period);
                                    mSwipeRefreshLayout.addView(view);
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
                                        TextView lv_protocol_delta = ((TextView) element.findViewById(R.id.lv_protocol_delta));
                                        if (change.getDouble("cdoitmo_delta_double") != 0.0) {
                                            lv_protocol_delta.setText(change.getString("cdoitmo_delta"));
                                            try {
                                                lv_protocol_delta.setTextColor(Static.resolveColor(getContext(), change.getDouble("cdoitmo_delta_double") < 0.0 ? R.attr.textColorDegrade : R.attr.textColorPassed));
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
                            break;
                        }
                        default: {
                            Log.wtf(TAG, "preference pref_protocol_changes_mode with wrong value: " + Storage.pref.get(getContext(), "pref_protocol_changes_mode", "simple") + ". Going to reset preference.");
                            Storage.pref.put(getContext(), "pref_protocol_changes_mode", "advanced");
                            display();
                            break;
                        }
                    }
                    // работаем со свайпом
                    SwipeRefreshLayout mSwipeRefreshLayout = (SwipeRefreshLayout) activity.findViewById(R.id.swipe_container);
                    if (mSwipeRefreshLayout != null) {
                        mSwipeRefreshLayout.setColorSchemeColors(Static.colorAccent);
                        mSwipeRefreshLayout.setProgressBackgroundColorSchemeColor(Static.colorBackgroundRefresh);
                        mSwipeRefreshLayout.setOnRefreshListener(self);
                    }
                    // работаем с раскрывающимся списком
                    Spinner spinner_weeks = (Spinner) activity.findViewById(R.id.pl_weeks_spinner);
                    if (spinner_weeks != null) {
                        final ArrayList<String> spinner_weeks_arr = new ArrayList<>();
                        final ArrayList<Integer> spinner_weeks_arr_values = new ArrayList<>();
                        for(int i = 1; i <= 4; i++){
                            String value = getString(R.string.for_the) + " ";
                            switch (i){
                                case 1: value += getString(R.string.last_week); break;
                                case 2: value += getString(R.string.last_2_weeks); break;
                                case 3: value += getString(R.string.last_3_weeks); break;
                                case 4: value += getString(R.string.last_4_weeks); break;
                            }
                            spinner_weeks_arr.add(value);
                            spinner_weeks_arr_values.add(i);
                        }
                        spinner_weeks.setAdapter(new ArrayAdapter<>(activity, R.layout.spinner_layout, spinner_weeks_arr));
                        spinner_weeks.setSelection(data.getInt("number_of_weeks") - 1);
                        spinner_weeks_blocker = true;
                        spinner_weeks.setOnItemSelectedListener(new AdapterView.OnItemSelectedListener() {
                            public void onItemSelected(AdapterView<?> parent, View item, int position, long selectedId) {
                                if (spinner_weeks_blocker) {
                                    spinner_weeks_blocker = false;
                                    return;
                                }
                                number_of_weeks = spinner_weeks_arr_values.get(position);
                                Log.v(TAG, "spinner_weeks clicked | number_of_weeks=" + number_of_weeks);
                                forceLoad();
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
            @Override
            public void onChecked(boolean is) {}
        });
    }

    private void draw(int layoutId){
        try {
            ViewGroup vg = ((ViewGroup) activity.findViewById(R.id.container_protocol));
            if (vg != null) {
                vg.removeAllViews();
                vg.addView(inflate(layoutId), 0, new ViewGroup.LayoutParams(ViewGroup.LayoutParams.MATCH_PARENT, ViewGroup.LayoutParams.MATCH_PARENT));
            }
        } catch (Exception e){
            Static.error(e);
        }
    }
    private View inflate(int layoutId) throws InflateException {
        return ((LayoutInflater) activity.getSystemService(Context.LAYOUT_INFLATER_SERVICE)).inflate(layoutId, null);
    }

}
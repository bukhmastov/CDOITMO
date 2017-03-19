package com.bukhmastov.cdoitmo.fragments;

import android.app.Activity;
import android.content.Context;
import android.os.Bundle;
import android.os.Handler;
import android.os.Message;
import android.support.v4.app.Fragment;
import android.support.v4.widget.SwipeRefreshLayout;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.AdapterView;
import android.widget.ArrayAdapter;
import android.widget.ListView;
import android.widget.Spinner;
import android.widget.TextView;

import com.bukhmastov.cdoitmo.R;
import com.bukhmastov.cdoitmo.adapters.ProtocolListView;
import com.bukhmastov.cdoitmo.network.DeIfmoRestClient;
import com.bukhmastov.cdoitmo.network.interfaces.DeIfmoRestClientResponseHandler;
import com.bukhmastov.cdoitmo.objects.Protocol;
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

public class ProtocolFragment extends Fragment implements SwipeRefreshLayout.OnRefreshListener {

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
        number_of_weeks = Integer.parseInt(Storage.pref.get(getContext(), "pref_protocol_changes_weeks", "1"));
        protocol = new Protocol(getActivity());
    }

    @Override
    public View onCreateView(LayoutInflater inflater, ViewGroup container, Bundle savedInstanceState) {
        return inflater.inflate(R.layout.fragment_protocol, container, false);
    }

    @Override
    public void onResume() {
        super.onResume();
        if (!loaded) {
            loaded = true;
            load();
        }
    }

    @Override
    public void onPause() {
        super.onPause();
        if (fragmentRequestHandle != null) {
            loaded = false;
            fragmentRequestHandle.cancel(true);
        }
    }

    @Override
    public void onRefresh() {
        forceLoad();
    }

    private void load(){
        load(Storage.pref.get(getContext(), "pref_use_cache", true) ? Integer.parseInt(Storage.pref.get(getContext(), "pref_tab_refresh", "0")) : 0);
    }
    private void load(int refresh_rate){
        if (!protocol.is(number_of_weeks) || refresh_rate == 0) {
            forceLoad();
        } else if (refresh_rate >= 0){
            JSONObject p = protocol.get();
            try {
                if (p.getLong("timestamp") + refresh_rate * 3600000L < Calendar.getInstance().getTimeInMillis()) {
                    forceLoad();
                } else {
                    display();
                }
            } catch (JSONException e) {
                Static.error(e);
                forceLoad();
            }
        } else {
            display();
        }
    }
    private void forceLoad(){
        forceLoad(0);
    }
    private void forceLoad(int attempt){
        if (!Static.OFFLINE_MODE) {
            if (++attempt > maxAttempts) {
                if (protocol.is()) {
                    display();
                } else {
                    loadFailed();
                }
                return;
            }
            final int finalAttempt = attempt;
            DeIfmoRestClient.get(getContext(), "eregisterlog?days=" + String.valueOf(number_of_weeks * 7), null, new DeIfmoRestClientResponseHandler() {
                @Override
                public void onSuccess(int statusCode, JSONObject responseObj, JSONArray responseArr) {
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
                    draw(R.layout.state_loading);
                    TextView loading_message = (TextView) getActivity().findViewById(R.id.loading_message);
                    if (loading_message != null) {
                        switch (state) {
                            case DeIfmoRestClient.STATE_HANDLING: loading_message.setText(R.string.loading); break;
                        }
                    }
                }
                @Override
                public void onFailure(int state) {
                    Activity activity = getActivity();
                    switch (state) {
                        case DeIfmoRestClient.FAILED_OFFLINE:
                            if (protocol.is(number_of_weeks)) {
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
            if (protocol.is()) {
                display();
            } else {
                draw(R.layout.state_offline);
                Activity activity = getActivity();
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
    }
    private void loadFailed(){
        try {
            draw(R.layout.state_try_again);
            TextView try_again_message = (TextView) getActivity().findViewById(R.id.try_again_message);
            if (try_again_message != null) try_again_message.setText(R.string.load_failed_retry_in_minute);
            View try_again_reload = getActivity().findViewById(R.id.try_again_reload);
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
        try {
            JSONObject data = protocol.get();
            if (data == null) throw new NullPointerException("Protocol.protocol can't be null");
            // получаем список предметов для отображения
            number_of_weeks = data.getInt("number_of_weeks");
            JSONArray jsonArray = data.getJSONArray("protocol");
            final ArrayList<HashMap<String, String>> changes = new ArrayList<>();
            for(int i = 0; i < jsonArray.length(); i++){
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
            // отображаем интерфейс
            draw(R.layout.protocol_layout);
            // работаем со списком
            ListView pl_list_view = (ListView) getActivity().findViewById(R.id.pl_list_view);
            if (pl_list_view != null) {
                if (changes.size() > 0) {
                    pl_list_view.setAdapter(new ProtocolListView(getActivity(), changes));
                } else {
                    ViewGroup mSwipeRefreshLayout = (SwipeRefreshLayout) getActivity().findViewById(R.id.swipe_container);
                    if (mSwipeRefreshLayout != null) {
                        mSwipeRefreshLayout.removeView(pl_list_view);
                        mSwipeRefreshLayout.addView(((LayoutInflater) getActivity().getSystemService(Context.LAYOUT_INFLATER_SERVICE)).inflate(R.layout.state_protocol_empty, null), 0, new ViewGroup.LayoutParams(ViewGroup.LayoutParams.MATCH_PARENT, ViewGroup.LayoutParams.MATCH_PARENT));
                    }
                }
            }
            // работаем со свайпом
            SwipeRefreshLayout mSwipeRefreshLayout = (SwipeRefreshLayout) getActivity().findViewById(R.id.swipe_container);
            if (mSwipeRefreshLayout != null) {
                mSwipeRefreshLayout.setColorSchemeColors(Static.colorAccent);
                mSwipeRefreshLayout.setProgressBackgroundColorSchemeColor(Static.colorBackgroundRefresh);
                mSwipeRefreshLayout.setOnRefreshListener(this);
            }
            // работаем с раскрывающимся списком
            Spinner spinner_weeks = (Spinner) getActivity().findViewById(R.id.pl_weeks_spinner);
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
                spinner_weeks.setAdapter(new ArrayAdapter<>(getActivity(), R.layout.spinner_layout, spinner_weeks_arr));
                spinner_weeks.setSelection(data.getInt("number_of_weeks") - 1);
                spinner_weeks_blocker = true;
                spinner_weeks.setOnItemSelectedListener(new AdapterView.OnItemSelectedListener() {
                    public void onItemSelected(AdapterView<?> parent, View item, int position, long selectedId) {
                        if (spinner_weeks_blocker) {
                            spinner_weeks_blocker = false;
                            return;
                        }
                        number_of_weeks = spinner_weeks_arr_values.get(position);
                        forceLoad();
                    }
                    public void onNothingSelected(AdapterView<?> parent) {}
                });
            }
            Static.showUpdateTime(getActivity(), data.getLong("timestamp"), R.id.protocol_layout, false);
        } catch (Exception e) {
            Static.error(e);
            loadFailed();
        }
    }
    private void draw(int layoutId){
        try {
            ViewGroup vg = ((ViewGroup) getActivity().findViewById(R.id.container_protocol));
            if (vg != null) {
                vg.removeAllViews();
                vg.addView(((LayoutInflater) getActivity().getSystemService(Context.LAYOUT_INFLATER_SERVICE)).inflate(layoutId, null), 0, new ViewGroup.LayoutParams(ViewGroup.LayoutParams.MATCH_PARENT, ViewGroup.LayoutParams.MATCH_PARENT));
            }
        } catch (Exception e){
            Static.error(e);
        }
    }

}
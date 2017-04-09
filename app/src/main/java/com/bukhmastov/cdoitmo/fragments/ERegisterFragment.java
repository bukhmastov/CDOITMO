package com.bukhmastov.cdoitmo.fragments;

import android.app.Activity;
import android.content.Context;
import android.content.Intent;
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
import com.bukhmastov.cdoitmo.activities.LoginActivity;
import com.bukhmastov.cdoitmo.activities.SubjectActivity;
import com.bukhmastov.cdoitmo.adapters.SubjectListView;
import com.bukhmastov.cdoitmo.network.DeIfmoClient;
import com.bukhmastov.cdoitmo.network.DeIfmoRestClient;
import com.bukhmastov.cdoitmo.network.interfaces.DeIfmoRestClientResponseHandler;
import com.bukhmastov.cdoitmo.objects.ERegister;
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

public class ERegisterFragment extends Fragment implements SwipeRefreshLayout.OnRefreshListener {

    private static final String TAG = "ERegisterFragment";
    public static ERegister eRegister = null;
    private String group = "";
    private int term = -1;
    private boolean spinner_group_blocker = true, spinner_period_blocker = true;
    private boolean loaded = false;
    private RequestHandle fragmentRequestHandle = null;

    @Override
    public void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        eRegister = new ERegister(getActivity());
    }

    @Override
    public View onCreateView(LayoutInflater inflater, ViewGroup container, Bundle savedInstanceState) {
        return inflater.inflate(R.layout.fragment_eregister, container, false);
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
        if (!eRegister.is() || refresh_rate == 0) {
            forceLoad();
        } else if (refresh_rate >= 0){
            JSONObject eregister = eRegister.get();
            try {
                if (eregister.getLong("timestamp") + refresh_rate * 3600000L < Calendar.getInstance().getTimeInMillis()) {
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
        if (!Static.OFFLINE_MODE) {
            DeIfmoRestClient.get(getContext(), "eregister", null, new DeIfmoRestClientResponseHandler() {
                @Override
                public void onSuccess(int statusCode, JSONObject responseObj, JSONArray responseArr) {
                    if (statusCode == 200 && responseObj != null) {
                        eRegister.put(responseObj, new Handler(){
                            @Override
                            public void handleMessage(Message msg) {
                                display();
                            }
                        });
                    } else {
                        if (eRegister.is()) {
                            display();
                        } else {
                            loadFailed();
                        }
                    }
                }
                @Override
                public void onProgress(int state) {
                    draw(R.layout.state_loading);
                    Activity activity = getActivity();
                    if (activity != null) {
                        TextView loading_message = (TextView) activity.findViewById(R.id.loading_message);
                        if (loading_message != null) {
                            switch (state) {
                                case DeIfmoClient.STATE_HANDLING:
                                    loading_message.setText(R.string.loading);
                                    break;
                                case DeIfmoClient.STATE_AUTHORIZATION:
                                    loading_message.setText(R.string.authorization);
                                    break;
                                case DeIfmoClient.STATE_AUTHORIZED:
                                    loading_message.setText(R.string.authorized);
                                    break;
                            }
                        }
                    }
                }
                @Override
                public void onFailure(int state) {
                    Activity activity = getActivity();
                    switch (state) {
                        case DeIfmoClient.FAILED_OFFLINE:
                            if (eRegister.is()) {
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
                        case DeIfmoClient.FAILED_TRY_AGAIN:
                        case DeIfmoClient.FAILED_AUTH_TRY_AGAIN:
                            draw(R.layout.state_try_again);
                            if (activity != null) {
                                if (state == DeIfmoClient.FAILED_AUTH_TRY_AGAIN) {
                                    TextView try_again_message = (TextView) activity.findViewById(R.id.try_again_message);
                                    if (try_again_message != null) try_again_message.setText(R.string.auth_failed);
                                }
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
                        case DeIfmoClient.FAILED_AUTH_CREDENTIALS_REQUIRED: gotoLogin(LoginActivity.SIGNAL_CREDENTIALS_REQUIRED); break;
                        case DeIfmoClient.FAILED_AUTH_CREDENTIALS_FAILED: gotoLogin(LoginActivity.SIGNAL_CREDENTIALS_FAILED); break;
                    }
                }
                @Override
                public void onNewHandle(RequestHandle requestHandle) {
                    fragmentRequestHandle = requestHandle;
                }
            });
        } else {
            if(eRegister.is()){
                display();
            } else {
                try {
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
                } catch (Exception e) {
                    Static.error(e);
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
            JSONObject data = eRegister.get();
            if (data == null) throw new NullPointerException("parsedERegister cannot be null");
            checkData(data);
            // получаем список предметов для отображения
            final ArrayList<HashMap<String, String>> subjects = new ArrayList<>();
            JSONArray groups = data.getJSONArray("groups");
            for (int i = 0; i < groups.length(); i++) {
                JSONObject group = groups.getJSONObject(i);
                if (Objects.equals(group.getString("name"), this.group)) {
                    JSONArray terms = group.getJSONArray("terms");
                    for (int j = 0; j < terms.length(); j++) {
                        JSONObject term = terms.getJSONObject(j);
                        if (this.term == -1 || this.term == term.getInt("number")) {
                            JSONArray subjArr = term.getJSONArray("subjects");
                            for (int k = 0; k < subjArr.length(); k++) {
                                JSONObject subj = subjArr.getJSONObject(k);
                                HashMap<String, String> subjObj = new HashMap<>();
                                subjObj.put("group", group.getString("name"));
                                subjObj.put("name", subj.getString("name"));
                                subjObj.put("semester", String.valueOf(term.getInt("number")));
                                subjObj.put("value", String.valueOf(subj.getDouble("currentPoints")));
                                subjObj.put("type", subj.getString("type"));
                                subjects.add(subjObj);
                            }
                        }
                    }
                    break;
                }
            }
            // отображаем интерфейс
            draw(R.layout.eregister_layout);
            // работаем со списком
            ListView erl_list_view = (ListView) getActivity().findViewById(R.id.erl_list_view);
            if (erl_list_view != null) {
                erl_list_view.setAdapter(new SubjectListView(getActivity(), subjects));
                erl_list_view.setOnItemClickListener(new AdapterView.OnItemClickListener() {
                    public void onItemClick(AdapterView<?> parent, View view, int position, long id) {
                        HashMap<String, String> subj = subjects.get(position);
                        Intent intent = new Intent(getActivity(), SubjectActivity.class);
                        intent.putExtra("group", subj.get("group"));
                        intent.putExtra("term", subj.get("semester"));
                        intent.putExtra("name", subj.get("name"));
                        startActivity(intent);
                    }
                });
            }
            // работаем со свайпом
            SwipeRefreshLayout mSwipeRefreshLayout = (SwipeRefreshLayout) getActivity().findViewById(R.id.swipe_container);
            if (mSwipeRefreshLayout != null) {
                mSwipeRefreshLayout.setColorSchemeColors(Static.colorAccent);
                mSwipeRefreshLayout.setProgressBackgroundColorSchemeColor(Static.colorBackgroundRefresh);
                mSwipeRefreshLayout.setOnRefreshListener(this);
            }
            // работаем с раскрывающимися списками
            int selection = 0, counter = 0;
            // список групп
            Spinner spinner_group = (Spinner) getActivity().findViewById(R.id.erl_group_spinner);
            if (spinner_group != null) {
                final ArrayList<String> spinner_group_arr = new ArrayList<>();
                final ArrayList<String> spinner_group_arr_names = new ArrayList<>();
                for (int i = 0; i < groups.length(); i++) {
                    JSONObject group = groups.getJSONObject(i);
                    spinner_group_arr.add(group.getString("name") + " (" + group.getJSONArray("years").getInt(0) + "/" + group.getJSONArray("years").getInt(1) + ")");
                    spinner_group_arr_names.add(group.getString("name"));
                    if (Objects.equals(group.getString("name"), this.group)) selection = counter;
                    counter++;
                }
                spinner_group.setAdapter(new ArrayAdapter<>(getActivity(), R.layout.spinner_layout, spinner_group_arr));
                spinner_group.setSelection(selection);
                spinner_group_blocker = true;
                spinner_group.setOnItemSelectedListener(new AdapterView.OnItemSelectedListener() {
                    public void onItemSelected(AdapterView<?> parent, View item, int position, long selectedId) {
                        if (spinner_group_blocker) {
                            spinner_group_blocker = false;
                            return;
                        }
                        group = spinner_group_arr_names.get(position);
                        load(-1);
                    }
                    public void onNothingSelected(AdapterView<?> parent) {}
                });
            }
            // список семестров
            Spinner spinner_period = (Spinner) getActivity().findViewById(R.id.erl_period_spinner);
            if (spinner_period != null) {
                final ArrayList<String> spinner_period_arr = new ArrayList<>();
                final ArrayList<Integer> spinner_period_arr_values = new ArrayList<>();
                selection = 2;
                for (int i = 0; i < groups.length(); i++) {
                    JSONObject group = groups.getJSONObject(i);
                    if (Objects.equals(group.getString("name"), this.group)) {
                        int first = group.getJSONArray("terms").getJSONObject(0).getInt("number");
                        int second = group.getJSONArray("terms").getJSONObject(1).getInt("number");
                        spinner_period_arr.add(first + " " + getString(R.string.semester));
                        spinner_period_arr.add(second + " " + getString(R.string.semester));
                        spinner_period_arr.add(getString(R.string.year));
                        spinner_period_arr_values.add(first);
                        spinner_period_arr_values.add(second);
                        spinner_period_arr_values.add(-1);
                        if (this.term == first) selection = 0;
                        if (this.term == second) selection = 1;
                        break;
                    }
                }
                spinner_period.setAdapter(new ArrayAdapter<>(getActivity(), R.layout.spinner_layout, spinner_period_arr));
                spinner_period.setSelection(selection);
                spinner_period_blocker = true;
                spinner_period.setOnItemSelectedListener(new AdapterView.OnItemSelectedListener() {
                    public void onItemSelected(AdapterView<?> parent, View item, int position, long selectedId) {
                        if (spinner_period_blocker) {
                            spinner_period_blocker = false;
                            return;
                        }
                        term = spinner_period_arr_values.get(position);
                        load(-1);
                    }
                    public void onNothingSelected(AdapterView<?> parent) {
                    }
                });
            }
            Static.showUpdateTime(getActivity(), data.getLong("timestamp"), R.id.eregister_layout, true);
        } catch (Exception e) {
            Static.error(e);
            loadFailed();
        }
    }
    private void checkData(JSONObject data) throws Exception {
        Calendar now = Calendar.getInstance();
        int year = now.get(Calendar.YEAR);
        int month = now.get(Calendar.MONTH);
        String currentGroup = "";
        int currentTerm = -1, maxYear = 0;
        JSONArray groups = data.getJSONArray("groups");
        for (int i = 0; i < groups.length(); i++) {
            JSONObject group = groups.getJSONObject(i);
            if (!Objects.equals(this.group, "") && Objects.equals(this.group, group.getString("name"))) { // мы нашли назначенную группу
                this.group = group.getString("name");
                // теперь проверяем семестр
                JSONArray terms = group.getJSONArray("terms");
                boolean isTermOk = false;
                for (int j = 0; j < terms.length(); j++) {
                    JSONObject term = terms.getJSONObject(j);
                    if (this.term != -1 && this.term == term.getInt("number")) { // мы нашли семестр в найденной группе
                        this.term = term.getInt("number");
                        isTermOk = true;
                        break;
                    }
                }
                if (!isTermOk) { // семестр неверен, выбираем весь год
                    this.term = -1;
                }
                break;
            } else { // группа до сих пор не найдена
                JSONArray years = group.getJSONArray("years");
                if (Objects.equals(currentGroup, "")) {
                    if (year == years.getInt(month > Calendar.AUGUST ? 0 : 1)) {
                        currentGroup = group.getString("name");
                        if (Integer.parseInt(Storage.pref.get(getContext(), "pref_e_journal_term", "0")) == 1) {
                            currentTerm = -1;
                        } else {
                            currentTerm = group.getJSONArray("terms").getJSONObject(month > Calendar.AUGUST || month == Calendar.JANUARY ? 0 : 1).getInt("number");
                        }
                    }
                }
                if (maxYear < years.getInt(0)) {
                    maxYear = years.getInt(0);
                }
            }
        }
        if (Objects.equals(this.group, "")) {
            if (!Objects.equals(currentGroup, "")) {
                this.group = currentGroup;
                this.term = currentTerm;
            } else {
                for (int i = 0; i < groups.length(); i++) {
                    JSONObject group = groups.getJSONObject(i);
                    if (group.getJSONArray("years").getInt(0) == maxYear) {
                        this.group = group.getString("name");
                        break;
                    }
                }
                this.term = -1;
            }
        }
    }
    void gotoLogin(int state){
        Intent intent = new Intent(getContext(), LoginActivity.class);
        intent.putExtra("state", state);
        startActivity(intent);
    }
    private void draw(int layoutId){
        try {
            ViewGroup vg = ((ViewGroup) getActivity().findViewById(R.id.container_eregister));
            if (vg != null) {
                vg.removeAllViews();
                vg.addView(((LayoutInflater) getActivity().getSystemService(Context.LAYOUT_INFLATER_SERVICE)).inflate(layoutId, null), 0, new ViewGroup.LayoutParams(ViewGroup.LayoutParams.MATCH_PARENT, ViewGroup.LayoutParams.MATCH_PARENT));
            }
        } catch (Exception e){
            Static.error(e);
        }
    }

}
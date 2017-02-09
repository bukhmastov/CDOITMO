package com.bukhmastov.cdoitmo;

import android.app.Activity;
import android.content.Context;
import android.content.SharedPreferences;
import android.os.AsyncTask;
import android.os.Bundle;
import android.preference.PreferenceManager;
import android.support.annotation.NonNull;
import android.support.design.widget.Snackbar;
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

import com.loopj.android.http.RequestHandle;
import com.loopj.android.http.RequestParams;

import org.htmlcleaner.HtmlCleaner;
import org.htmlcleaner.TagNode;
import org.json.JSONArray;
import org.json.JSONException;
import org.json.JSONObject;

import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.Calendar;
import java.util.Date;
import java.util.HashMap;
import java.util.List;
import java.util.Locale;
import java.util.Objects;

public class ProtocolFragment extends Fragment implements SwipeRefreshLayout.OnRefreshListener {

    private static final String TAG = "ProtocolFragment";
    public static Protocol protocol = null;
    private int number_of_weeks = 1;
    private boolean notifyAboutDateUpdate = false;
    private boolean spinner_weeks_blocker = true;
    private boolean loaded = false;
    private RequestHandle fragmentRequestHandle = null;

    @Override
    public void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
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
        SharedPreferences sharedPreferences = PreferenceManager.getDefaultSharedPreferences(getActivity());
        load(sharedPreferences.getBoolean("pref_use_cache", true) ? Integer.parseInt(sharedPreferences.getString("pref_tab_refresh", "0")) : 0);
    }
    private void load(int refresh_rate){
        if (!protocol.is(number_of_weeks) || refresh_rate == 0) {
            forceLoad();
        } else if (refresh_rate >= 0){
            JSONObject protocol = ProtocolFragment.protocol.get();
            try {
                if (protocol.getLong("timestamp") + refresh_rate * 3600000L < Calendar.getInstance().getTimeInMillis()) {
                    forceLoad();
                } else {
                    display();
                }
            } catch (JSONException e) {
                if(LoginActivity.errorTracker != null) LoginActivity.errorTracker.add(e);
                forceLoad();
            }
        } else {
            display();
        }
    }
    private void forceLoad(){
        if (MainActivity.group == null) {
            loadFailed();
            return;
        }
        notifyAboutDateUpdate = true;
        if (!MainActivity.OFFLINE_MODE) {
            Calendar now = Calendar.getInstance();
            int year = now.get(Calendar.YEAR);
            int month = now.get(Calendar.MONTH);
            RequestParams params = new RequestParams();
            params.put("Rule", "eRegisterGetProtokolVariable");
            params.put("ST_GRP", MainActivity.group);
            params.put("PERSONID", Storage.get(getContext(), "login"));
            params.put("SYU_ID", "0");
            params.put("UNIVER", "1");
            params.put("APPRENTICESHIP", month > Calendar.AUGUST ? year + "/" + (year + 1) : (year - 1) + "/" + year);
            params.put("PERIOD", String.valueOf(number_of_weeks * 7));
            DeIfmoRestClient.post(getContext(), "servlet/distributedCDE", params, new DeIfmoRestClientResponseHandler() {
                @Override
                public void onSuccess(int statusCode, String response) {
                    if (statusCode == 200) {
                        new ProtocolParse(new ProtocolParse.response() {
                            @Override
                            public void finish(JSONObject json) {
                                protocol.put(json, number_of_weeks);
                                display();
                            }
                        }).execute(response);
                    } else {
                        if (protocol.is(number_of_weeks)) {
                            display();
                        } else {
                            loadFailed();
                        }
                    }
                }
                @Override
                public void onProgress(int state) {
                    draw(R.layout.state_loading);
                    TextView loading_message = (TextView) getActivity().findViewById(R.id.loading_message);
                    if (loading_message != null) {
                        switch (state) {
                            case DeIfmoRestClient.STATE_HANDLING: loading_message.setText(R.string.loading); break;
                            case DeIfmoRestClient.STATE_AUTHORIZATION: loading_message.setText(R.string.authorization); break;
                            case DeIfmoRestClient.STATE_AUTHORIZED: loading_message.setText(R.string.authorized); break;
                        }
                    }
                }
                @Override
                public void onFailure(int state) {
                    switch (state) {
                        case DeIfmoRestClient.FAILED_OFFLINE:
                            if (protocol.is(number_of_weeks)) {
                                display();
                            } else {
                                draw(R.layout.state_offline);
                                View offline_reload = getActivity().findViewById(R.id.offline_reload);
                                if (offline_reload != null) {
                                    offline_reload.setOnClickListener(new View.OnClickListener() {
                                        @Override
                                        public void onClick(View v) {
                                            forceLoad();
                                        }
                                    });
                                }
                            }
                            break;
                        case DeIfmoRestClient.FAILED_TRY_AGAIN:
                        case DeIfmoRestClient.FAILED_AUTH_TRY_AGAIN:
                            draw(R.layout.state_try_again);
                            if (state == DeIfmoRestClient.FAILED_AUTH_TRY_AGAIN) {
                                TextView try_again_message = (TextView) getActivity().findViewById(R.id.try_again_message);
                                if (try_again_message != null) try_again_message.setText(R.string.auth_failed);
                            }
                            View try_again_reload = getActivity().findViewById(R.id.try_again_reload);
                            if (try_again_reload != null) {
                                try_again_reload.setOnClickListener(new View.OnClickListener() {
                                    @Override
                                    public void onClick(View v) {
                                        forceLoad();
                                    }
                                });
                            }
                            break;
                        case DeIfmoRestClient.FAILED_AUTH_CREDENTIALS_REQUIRED: gotoLogin(LoginActivity.SIGNAL_CREDENTIALS_REQUIRED); break;
                        case DeIfmoRestClient.FAILED_AUTH_CREDENTIALS_FAILED: gotoLogin(LoginActivity.SIGNAL_CREDENTIALS_FAILED); break;
                    }
                }
                @Override
                public void onNewHandle(RequestHandle requestHandle) {
                    fragmentRequestHandle = requestHandle;
                }
            });
        } else {
            if(protocol.is(number_of_weeks)){
                display();
            } else {
                draw(R.layout.state_offline);
                View offline_reload = getActivity().findViewById(R.id.offline_reload);
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
    private void loadFailed(){
        try {
            draw(R.layout.state_try_again);
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
            if(LoginActivity.errorTracker != null) LoginActivity.errorTracker.add(e);
        }
    }
    void display(){
        try {
            JSONObject data = protocol.get();
            if(data == null) throw new NullPointerException("Protocol.protocol can't be null");
            // получаем список предметов для отображения
            final ArrayList<HashMap<String, String>> changes = new ArrayList<>();
            JSONArray jsonArray = data.getJSONObject("protocol").getJSONArray("changes");
            for(int i = 0; i < jsonArray.length(); i++){
                JSONObject jsonObject = jsonArray.getJSONObject(i);
                HashMap<String, String> hashMap = new HashMap<>();
                hashMap.put("name", jsonObject.getString("subject"));
                hashMap.put("desc", jsonObject.getString("field") + " [" + double2string(jsonObject.getDouble("min")) + "/" + double2string(jsonObject.getDouble("limit")) + "/" + double2string(jsonObject.getDouble("max")) + "]");
                hashMap.put("meta", (Objects.equals(jsonObject.getString("sign"), "..") ? "" : jsonObject.getString("sign") + " | ") + jsonObject.getString("date"));
                hashMap.put("value", double2string(jsonObject.getDouble("value")));
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
                mSwipeRefreshLayout.setColorSchemeColors(MainActivity.colorAccent);
                mSwipeRefreshLayout.setProgressBackgroundColorSchemeColor(MainActivity.colorBackgroundRefresh);
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
            // показываем снекбар с датой обновления
            if(notifyAboutDateUpdate){
                int shift = (int)((Calendar.getInstance().getTimeInMillis() - data.getLong("timestamp")) / 1000);
                String message;
                if(shift < 21600){
                    if(shift < 3600) {
                        message = shift / 60 + " " + "мин. назад";
                    } else {
                        message = shift / 3600 + " " + "час. назад";
                    }
                } else {
                    message = data.getString("date");
                }
                if(shift >= 60){
                    View protocol_layout = getActivity().findViewById(R.id.protocol_layout);
                    if (protocol_layout != null) {
                        Snackbar snackbar = Snackbar.make(protocol_layout, getString(R.string.update_date) + " " + message, Snackbar.LENGTH_SHORT);
                        snackbar.getView().setBackgroundColor(MainActivity.colorBackgroundSnackBar);
                        snackbar.show();
                    }
                }
                notifyAboutDateUpdate = false;
            }
        } catch (Exception e) {
            if(LoginActivity.errorTracker != null) LoginActivity.errorTracker.add(e);
            loadFailed();
        }
    }
    void gotoLogin(int state){
        LoginActivity.state = state;
        getActivity().finish();
    }
    private void draw(int layoutId){
        try {
            ViewGroup vg = ((ViewGroup) getActivity().findViewById(R.id.container_protocol));
            if (vg != null) {
                vg.removeAllViews();
                vg.addView(((LayoutInflater) getActivity().getSystemService(Context.LAYOUT_INFLATER_SERVICE)).inflate(layoutId, null), 0, new ViewGroup.LayoutParams(ViewGroup.LayoutParams.MATCH_PARENT, ViewGroup.LayoutParams.MATCH_PARENT));
            }
        } catch (Exception e){
            if(LoginActivity.errorTracker != null) LoginActivity.errorTracker.add(e);
        }
    }
    private String double2string(Double value){
        String valueStr = String.valueOf(value);
        if(value != -1.0){
            if(value == Double.parseDouble(value.intValue() + ".0")){
                valueStr = value.intValue() + "";
            }
        } else {
            valueStr = "-";
        }
        return valueStr;
    }
}

class Protocol {

    private static final String TAG = "Protocol";
    private Context context;
    private JSONObject protocol = null;

    Protocol(Context context){
        this.context = context;
        String protocol = Cache.get(context, "Protocol");
        if(!Objects.equals(protocol, "")){
            try {
                this.protocol = new JSONObject(protocol);
            } catch (Exception e) {
                if(LoginActivity.errorTracker != null) LoginActivity.errorTracker.add(e);
            }
        }
    }
    void put(JSONObject data, int number_of_weeks){
        try {
            JSONObject json = new JSONObject();
            json.put("timestamp", Calendar.getInstance().getTimeInMillis());
            json.put("date", new SimpleDateFormat("dd.MM.yyyy HH:mm", Locale.ROOT).format(new Date(Calendar.getInstance().getTimeInMillis())));
            json.put("number_of_weeks", number_of_weeks);
            json.put("protocol", data);
            protocol = json;
            Cache.put(context, "Protocol", protocol.toString());
            if(number_of_weeks == 1) PreferenceManager.getDefaultSharedPreferences(context).edit().putString("ProtocolTrackerHISTORY", data.getJSONArray("changes").toString()).apply();
        } catch (Exception e) {
            if(LoginActivity.errorTracker != null) LoginActivity.errorTracker.add(e);
        }
    }
    JSONObject get(){
        return protocol;
    }
    boolean is(int number_of_weeks){
        if(protocol == null){
            return false;
        } else {
            try {
                return protocol.getInt("number_of_weeks") == number_of_weeks;
            } catch (JSONException e) {
                if(LoginActivity.errorTracker != null) LoginActivity.errorTracker.add(e);
                return false;
            }
        }
    }
}

class ProtocolParse extends AsyncTask<String, Void, JSONObject> {
    interface response {
        void finish(JSONObject json);
    }
    private response delegate = null;
    ProtocolParse(response delegate){
        this.delegate = delegate;
    }
    @Override
    protected JSONObject doInBackground(String... params) {
        try {
            String response = params[0].replace("&nbsp;", " ");
            HtmlCleaner cleaner = new HtmlCleaner();
            TagNode root = cleaner.clean(response);
            List<? extends TagNode> divs = root.getElementListByAttValue("class", "d_text", true, false);
            for(TagNode div : divs){
                List<? extends TagNode> tables = div.getElementListByAttValue("class", "d_table", false, false);
                for(TagNode table : tables){
                    if(table.getText().toString().contains("Показать протокол изменений за")) continue;
                    List<? extends TagNode> rows = table.getAllElementsList(false).get(0).getAllElementsList(false);
                    JSONObject json = new JSONObject();
                    JSONObject headers = new JSONObject();
                    JSONArray changes = new JSONArray();
                    int counter;
                    for(TagNode row : rows){
                        List<? extends TagNode> columns = row.getAllElementsList(false);
                        if(row.getText().toString().contains("[Минимум/Порог/Максимум]")){
                            counter = -1;
                            for(TagNode column : columns){
                                counter++;
                                if(counter == 0) continue;
                                if(counter == 1) continue;
                                if(counter == 2) headers.put("subject", column.getText().toString().trim());
                                if(counter == 3){
                                    String[] parts = column.getText().toString().split("\\[");
                                    headers.put("field", parts[0].trim());
                                    String[] mlm = parts[1].replaceAll(System.getProperty("line.separator") + "|\\]| ", "").trim().split("/");
                                    headers.put("min", mlm[0]);
                                    headers.put("limit", mlm[1]);
                                    headers.put("max", mlm[2]);
                                }
                                if(counter == 4) headers.put("value", column.getText().toString().trim());
                                if(counter == 5) headers.put("date", column.getText().toString().trim());
                                if(counter == 6) headers.put("sign", column.getText().toString().trim());
                            }
                        } else {
                            counter = -1;
                            JSONObject change = new JSONObject();
                            for(TagNode column : columns){
                                counter++;
                                if(counter == 0) continue;
                                if(counter == 1) continue;
                                if(counter == 2) change.put("subject", column.getText().toString().trim());
                                if(counter == 3){
                                    String[] parts = column.getText().toString().split("\\[");
                                    change.put("field", parts[0].trim());
                                    String[] mlm = parts[1].replaceAll(System.getProperty("line.separator") + "|\\]| ", "").trim().split("/");
                                    change.put("min", Objects.equals(mlm[0], "-") ? -1.0 : Double.parseDouble(mlm[0].replace(",", ".")));
                                    change.put("limit", Objects.equals(mlm[1], "-") ? -1.0 : Double.parseDouble(mlm[1].replace(",", ".")));
                                    change.put("max", Objects.equals(mlm[2], "-") ? -1.0 : Double.parseDouble(mlm[2].replace(",", ".")));
                                }
                                if(counter == 4) change.put("value", Double.parseDouble(column.getText().toString().trim().replace(",", ".")));
                                if(counter == 5) change.put("date", column.getText().toString().trim());
                                if(counter == 6) change.put("sign", column.getText().toString().trim());
                            }
                            changes.put(change);
                        }
                    }
                    json.put("headers", headers);
                    json.put("changes", changes);
                    return json;
                }
            }
        } catch (Exception e) {
            if(LoginActivity.errorTracker != null) LoginActivity.errorTracker.add(e);
            return null;
        }
        return null;
    }
    @Override
    protected void onPostExecute(JSONObject json) {
        delegate.finish(json);
    }
}

class ProtocolListView extends ArrayAdapter<HashMap<String, String>> {

    private final Activity context;
    private final ArrayList<HashMap<String, String>> changes;

    ProtocolListView(Activity context, ArrayList<HashMap<String, String>> changes) {
        super(context, R.layout.listview_protocol, changes);
        this.context = context;
        this.changes = changes;
    }

    @NonNull
    @Override
    public View getView(int position, View view, @NonNull ViewGroup parent) {
        LayoutInflater inflater = context.getLayoutInflater();
        HashMap<String, String> change = changes.get(position);
        View rowView;
        rowView = inflater.inflate(R.layout.listview_protocol, null, true);
        TextView lv_protocol_name = ((TextView) rowView.findViewById(R.id.lv_protocol_name));
        TextView lv_protocol_desc = ((TextView) rowView.findViewById(R.id.lv_protocol_desc));
        TextView lv_protocol_meta = ((TextView) rowView.findViewById(R.id.lv_protocol_meta));
        TextView lv_protocol_value = ((TextView) rowView.findViewById(R.id.lv_protocol_value));
        lv_protocol_name.setText(change.get("name"));
        lv_protocol_desc.setText(change.get("desc"));
        lv_protocol_meta.setText(change.get("meta"));
        lv_protocol_value.setText(change.get("value"));
        return rowView;
    }
}
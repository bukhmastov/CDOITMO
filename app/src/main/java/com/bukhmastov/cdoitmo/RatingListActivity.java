package com.bukhmastov.cdoitmo;

import android.app.Activity;
import android.content.Context;
import android.os.AsyncTask;
import android.os.Bundle;
import android.preference.PreferenceManager;
import android.support.annotation.NonNull;
import android.support.v4.widget.SwipeRefreshLayout;
import android.support.v7.app.ActionBar;
import android.support.v7.app.AppCompatActivity;
import android.support.v7.widget.Toolbar;
import android.util.TypedValue;
import android.view.LayoutInflater;
import android.view.MenuItem;
import android.view.View;
import android.view.ViewGroup;
import android.widget.ArrayAdapter;
import android.widget.ListView;
import android.widget.TextView;

import com.loopj.android.http.RequestHandle;

import org.htmlcleaner.HtmlCleaner;
import org.htmlcleaner.TagNode;
import org.json.JSONArray;
import org.json.JSONObject;

import java.util.ArrayList;
import java.util.Calendar;
import java.util.HashMap;
import java.util.Objects;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

public class RatingListActivity extends AppCompatActivity implements SwipeRefreshLayout.OnRefreshListener {

    private static final String TAG = "RatingFragment";
    private String faculty = null;
    private String course = null;
    private String years = null;
    private boolean loaded = false;
    private RequestHandle activityRequestHandle = null;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        if(PreferenceManager.getDefaultSharedPreferences(this).getBoolean("pref_dark_theme", false)) setTheme(R.style.AppTheme_Dark);
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_rating_list);
        setSupportActionBar((Toolbar) findViewById(R.id.toolbar_rating_list));
        ActionBar actionBar = getSupportActionBar();
        if(actionBar != null){
            actionBar.setTitle("Топ-рейтинг");
            actionBar.setHomeButtonEnabled(true);
            actionBar.setDisplayHomeAsUpEnabled(true);
        }
        // получаем реквизиты рейтинга
        faculty = getIntent().getStringExtra("faculty");
        course = getIntent().getStringExtra("course");
        years = getIntent().getStringExtra("years");
        if(Objects.equals(years, "") || years == null){
            Calendar now = Calendar.getInstance();
            int year = now.get(Calendar.YEAR);
            int month = now.get(Calendar.MONTH);
            years = month > Calendar.AUGUST ? year + "/" + (year + 1) : (year - 1) + "/" + year;
        }
        if(Objects.equals(faculty, "") || faculty == null || Objects.equals(course, "") || course == null) finish();
    }

    @Override
    public void onResume() {
        super.onResume();
        if(!loaded) {
            loaded = true;
            load();
        }
    }

    @Override
    protected void onPause() {
        super.onPause();
        if(activityRequestHandle != null){
            loaded = false;
            activityRequestHandle.cancel(true);
        }
    }

    @Override
    public boolean onOptionsItemSelected(MenuItem item) {
        switch (item.getItemId()) {
            case android.R.id.home: finish(); return true;
            default: return super.onOptionsItemSelected(item);
        }
    }

    @Override
    public void onRefresh() {
        load();
    }

    private void load(){
        if(getSupportActionBar() != null) getSupportActionBar().setTitle("Топ-рейтинг");
        DeIfmoRestClient.get("index.php?doc_open=-tops.php&view=topStudent&depId=" + faculty + "&year_=" + course + "&app_=" + years, null, new DeIfmoRestClientResponseHandler() {
            @Override
            public void onSuccess(int statusCode, String response) {
                if(statusCode == 200){
                    new RatingTopListParse(new RatingTopListParse.response() {
                        @Override
                        public void finish(JSONObject json) {
                            display(json);
                        }
                    }).execute(response);
                } else {
                    loadFailed();
                }
            }
            @Override
            public void onProgress(int state) {
                draw(R.layout.state_loading);
                TextView loading_message = (TextView) findViewById(R.id.loading_message);
                switch(state){
                    case DeIfmoRestClient.STATE_HANDLING: loading_message.setText(R.string.loading); break;
                    case DeIfmoRestClient.STATE_AUTHORIZATION: loading_message.setText(R.string.authorization); break;
                    case DeIfmoRestClient.STATE_AUTHORIZED: loading_message.setText(R.string.authorized); break;
                }
            }
            @Override
            public void onFailure(int state) {
                switch(state){
                    case DeIfmoRestClient.FAILED_OFFLINE:
                        draw(R.layout.state_offline);
                        findViewById(R.id.offline_reload).setOnClickListener(new View.OnClickListener() {
                            @Override
                            public void onClick(View v) {
                                load();
                            }
                        });
                        break;
                    case DeIfmoRestClient.FAILED_TRY_AGAIN:
                    case DeIfmoRestClient.FAILED_AUTH_TRY_AGAIN:
                        draw(R.layout.state_try_again);
                        if(state == DeIfmoRestClient.FAILED_AUTH_TRY_AGAIN) ((TextView) findViewById(R.id.try_again_message)).setText(R.string.auth_failed);
                        findViewById(R.id.try_again_reload).setOnClickListener(new View.OnClickListener() {
                            @Override
                            public void onClick(View v) {
                                load();
                            }
                        });
                        break;
                    case DeIfmoRestClient.FAILED_AUTH_CREDENTIALS_REQUIRED: gotoLogin(LoginActivity.SIGNAL_CREDENTIALS_REQUIRED); break;
                    case DeIfmoRestClient.FAILED_AUTH_CREDENTIALS_FAILED: gotoLogin(LoginActivity.SIGNAL_CREDENTIALS_FAILED); break;
                }
            }
            @Override
            public void onNewHandle(RequestHandle requestHandle) {
                activityRequestHandle = requestHandle;
            }
        });
    }
    private void loadFailed(){
        try {
            draw(R.layout.state_try_again);
            findViewById(R.id.try_again_reload).setOnClickListener(new View.OnClickListener() {
                @Override
                public void onClick(View v) {
                    load();
                }
            });
        } catch (Exception e) {
            LoginActivity.errorTracker.add(e);
        }
    }
    private void display(JSONObject data){
        try {
            if(data == null) throw new NullPointerException("display(JSONObject data) can't be null");
            if(getSupportActionBar() != null) getSupportActionBar().setTitle(data.getString("header"));
            // получаем список для отображения рейтинга
            final ArrayList<HashMap<String, String>> users = new ArrayList<>();
            JSONArray jsonArray = data.getJSONArray("list");
            for(int i = 0; i < jsonArray.length(); i++){
                JSONObject jsonObject = jsonArray.getJSONObject(i);
                HashMap<String, String> hashMap = new HashMap<>();
                hashMap.put("number", String.valueOf(jsonObject.getInt("number")));
                hashMap.put("fio", jsonObject.getString("fio"));
                hashMap.put("meta", jsonObject.getString("group") + " — " + jsonObject.getString("department"));
                hashMap.put("is_me", jsonObject.getBoolean("is_me") ? "1" : "0");
                users.add(hashMap);
            }
            // отображаем интерфейс
            draw(R.layout.rating_list_layout);
            // работаем со списком
            ListView rl_list_view = (ListView) findViewById(R.id.rl_list_view);
            rl_list_view.setAdapter(new RatingListListView(this, users));
            // работаем со свайпом
            SwipeRefreshLayout mSwipeRefreshLayout = (SwipeRefreshLayout) findViewById(R.id.swipe_container);
            TypedValue typedValue = new TypedValue();
            getTheme().resolveAttribute(R.attr.colorAccent, typedValue, true);
            mSwipeRefreshLayout.setColorSchemeColors(typedValue.data);
            getTheme().resolveAttribute(R.attr.colorBackgroundRefresh, typedValue, true);
            mSwipeRefreshLayout.setProgressBackgroundColorSchemeColor(typedValue.data);
            mSwipeRefreshLayout.setOnRefreshListener(this);
        } catch(Exception e){
            LoginActivity.errorTracker.add(e);
            loadFailed();
        }
    }
    void gotoLogin(int state){
        LoginActivity.state = state;
        finish();
    }
    private void draw(int layoutId){
        try {
            ViewGroup vg = ((ViewGroup) findViewById(R.id.rating_list_container));
            vg.removeAllViews();
            vg.addView(((LayoutInflater) getSystemService(Context.LAYOUT_INFLATER_SERVICE)).inflate(layoutId, null), 0, new ViewGroup.LayoutParams(ViewGroup.LayoutParams.MATCH_PARENT, ViewGroup.LayoutParams.MATCH_PARENT));
        } catch (Exception e) {
            LoginActivity.errorTracker.add(e);
        }
    }
}

class RatingTopListParse extends AsyncTask<String, Void, JSONObject> {
    interface response {
        void finish(JSONObject json);
    }
    private response delegate = null;
    RatingTopListParse(response delegate){
        this.delegate = delegate;
    }
    @Override
    protected JSONObject doInBackground(String... params) {
        try {
            String response = params[0].replace("&nbsp;", " ");
            HtmlCleaner cleaner = new HtmlCleaner();
            TagNode root = cleaner.clean(response);
            JSONObject json = new JSONObject();
            TagNode div = root.findElementByAttValue("class", "content__________document", true, false);
            TagNode table = div.findElementByAttValue("class", "pblock", false, false);
            String[] header = table.getAllElementsList(false).get(0).getAllElementsList(false).get(0).getAllElementsList(false).get(0).getText().toString().split("/");
            String[] year = table.getAllElementsList(false).get(0).getAllElementsList(false).get(1).getText().toString().trim().split("\n");
            json.put("header", header[1].trim() + " - " + header[2].trim() + " (" + year[1].trim() + ")");
            TagNode column = table.getAllElementsList(false).get(0).getAllElementsList(false).get(2).getAllElementsList(false).get(0);
            TagNode td = column.getAllElementsList(false).get(0).getAllElementsList(false).get(0).getAllElementsList(false).get(0).getAllElementsList(false).get(0);
            String[] topList = td.getText().toString().split(" {3,5}");
            JSONArray list = new JSONArray();
            for(String item : topList){
                Pattern pattern = Pattern.compile("^(\\d{1,4})\\. (.*), гр\\. (.{1,10}), каф\\. (.{1,20})$");
                Matcher matcher = pattern.matcher(item.trim());
                if(matcher.find()){
                    JSONObject user = new JSONObject();
                    user.put("number", Integer.parseInt(matcher.group(1)));
                    String fio = matcher.group(2).trim();
                    Matcher m = Pattern.compile("^(.*) \\(\\)$").matcher(fio);
                    if(m.find()) fio = m.group(1);
                    user.put("fio", fio);
                    user.put("group", matcher.group(3));
                    user.put("department", matcher.group(4));
                    user.put("is_me", Objects.equals(fio, MainActivity.name));
                    list.put(user);
                }
            }
            json.put("list", list);
            return json;
        } catch (Exception e) {
            LoginActivity.errorTracker.add(e);
            return null;
        }
    }
    @Override
    protected void onPostExecute(JSONObject json) {
        delegate.finish(json);
    }
}

class RatingListListView extends ArrayAdapter<HashMap<String, String>> {

    private final Activity context;
    private final ArrayList<HashMap<String, String>> users;

    RatingListListView(Activity context, ArrayList<HashMap<String, String>> users) {
        super(context, R.layout.listview_rating_list, users);
        this.context = context;
        this.users = users;
    }

    @NonNull
    @Override
    public View getView(int position, View view, @NonNull ViewGroup parent) {
        LayoutInflater inflater = context.getLayoutInflater();
        HashMap<String, String> user = users.get(position);
        View rowView;
        rowView = inflater.inflate(R.layout.listview_rating_list, null, true);
        if(Objects.equals(user.get("is_me"), "1")){
            ViewGroup vg = ((ViewGroup) rowView.findViewById(R.id.lvrl_number_layout));
            vg.removeAllViews();
            vg.addView(((LayoutInflater) context.getSystemService(Context.LAYOUT_INFLATER_SERVICE)).inflate(R.layout.triangle_mark_layout, null), 0, new ViewGroup.LayoutParams(ViewGroup.LayoutParams.MATCH_PARENT, ViewGroup.LayoutParams.MATCH_PARENT));
            TypedValue typedValue = new TypedValue();
            this.context.getTheme().resolveAttribute(R.attr.colorPrimaryOpacity, typedValue, true);
            rowView.setBackgroundColor(typedValue.data);
            rowView.findViewById(R.id.lvrl_layout).setPadding(32, 0, 16, 0);
        }
        ((TextView) rowView.findViewById(R.id.lvrl_number)).setText(user.get("number"));
        ((TextView) rowView.findViewById(R.id.lvrl_fio)).setText(user.get("fio"));
        ((TextView) rowView.findViewById(R.id.lvrl_meta)).setText(user.get("meta"));
        return rowView;
    }
}
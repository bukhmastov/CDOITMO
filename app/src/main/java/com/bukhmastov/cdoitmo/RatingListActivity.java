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
        if (!loaded) {
            loaded = true;
            load();
        }
    }

    @Override
    protected void onPause() {
        super.onPause();
        if (activityRequestHandle != null) {
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
        if (getSupportActionBar() != null) getSupportActionBar().setTitle("Топ-рейтинг");
        DeIfmoRestClient.get(this, "?node=rating&std&depId=" + faculty + "&year=" + course + "&app=" + years, null, new DeIfmoRestClientResponseHandler() {
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
                        draw(R.layout.state_offline);
                        View offline_reload = findViewById(R.id.offline_reload);
                        if (offline_reload != null) {
                            offline_reload.setOnClickListener(new View.OnClickListener() {
                                @Override
                                public void onClick(View v) {
                                    load();
                                }
                            });
                        }
                        break;
                    case DeIfmoRestClient.FAILED_TRY_AGAIN:
                    case DeIfmoRestClient.FAILED_AUTH_TRY_AGAIN:
                        draw(R.layout.state_try_again);
                        if (state == DeIfmoRestClient.FAILED_AUTH_TRY_AGAIN) {
                            TextView try_again_message = (TextView) findViewById(R.id.try_again_message);
                            if (try_again_message != null) try_again_message.setText(R.string.auth_failed);
                        }
                        View try_again_reload = findViewById(R.id.try_again_reload);
                        if (try_again_reload != null) {
                            try_again_reload.setOnClickListener(new View.OnClickListener() {
                                @Override
                                public void onClick(View v) {
                                    load();
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
                activityRequestHandle = requestHandle;
            }
        });
    }
    private void loadFailed(){
        try {
            draw(R.layout.state_try_again);
            View try_again_reload = findViewById(R.id.try_again_reload);
            if (try_again_reload != null) {
                try_again_reload.setOnClickListener(new View.OnClickListener() {
                    @Override
                    public void onClick(View v) {
                        load();
                    }
                });
            }
        } catch (Exception e) {
            if(LoginActivity.errorTracker != null) LoginActivity.errorTracker.add(e);
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
                hashMap.put("change", jsonObject.getString("change"));
                hashMap.put("delta", jsonObject.getString("delta"));
                users.add(hashMap);
            }
            // отображаем интерфейс
            draw(R.layout.rating_list_layout);
            // работаем со списком
            ListView rl_list_view = (ListView) findViewById(R.id.rl_list_view);
            if (rl_list_view != null) rl_list_view.setAdapter(new RatingListListView(this, users));
            // работаем со свайпом
            SwipeRefreshLayout mSwipeRefreshLayout = (SwipeRefreshLayout) findViewById(R.id.swipe_container);
            if (mSwipeRefreshLayout != null) {
                mSwipeRefreshLayout.setColorSchemeColors(MainActivity.colorAccent);
                mSwipeRefreshLayout.setProgressBackgroundColorSchemeColor(MainActivity.colorBackgroundRefresh);
                mSwipeRefreshLayout.setOnRefreshListener(this);
            }
        } catch(Exception e){
            if(LoginActivity.errorTracker != null) LoginActivity.errorTracker.add(e);
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
            if (vg != null) {
                vg.removeAllViews();
                vg.addView(((LayoutInflater) getSystemService(Context.LAYOUT_INFLATER_SERVICE)).inflate(layoutId, null), 0, new ViewGroup.LayoutParams(ViewGroup.LayoutParams.MATCH_PARENT, ViewGroup.LayoutParams.MATCH_PARENT));
            }
        } catch (Exception e) {
            if(LoginActivity.errorTracker != null) LoginActivity.errorTracker.add(e);
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
            JSONArray list = new JSONArray();
            TagNode div = root.findElementByAttValue("class", "c-page", true, false).findElementByAttValue("class", "p-inner nobt", false, false);
            String header = "";
            Matcher m;
            m = Pattern.compile("Рейтинг студентов (.*)").matcher(div.findElementByAttValue("class", "notop", false, false).getText().toString().trim());
            if(m.find()) header = m.group(1).trim();
            m = Pattern.compile("^(.*) учебный год, (.*)$").matcher(div.findElementByAttValue("class", "info", false, false).getText().toString().trim());
            if(m.find()){
                if(!Objects.equals(header, "")) header += " - ";
                header += m.group(2) + " (" + m.group(1).replace(" ", "") + ")";
            }
            json.put("header", header);
            TagNode[] trs = div.findElementByAttValue("class", "table-rating", false, false).getElementsByName("tbody", false)[0].getElementsByName("tr", false);
            int counter = 0;
            for(TagNode tr : trs){
                if(counter++ == 0) continue;
                TagNode[] tds = tr.getElementsByName("td", false);
                if(tds == null || tds.length == 0) continue;
                JSONObject user = new JSONObject();
                user.put("number", Integer.parseInt(tds[0].getText().toString().trim()));
                String fio = tds[1].getText().toString().trim();
                user.put("fio", fio);
                String meta = tds[3].getText().toString().trim();
                m = Pattern.compile("гр. (.*), каф. (.*)").matcher(meta);
                if(m.find()){
                    user.put("group", m.group(1).trim());
                    user.put("department", m.group(2).trim());
                } else {
                    user.put("group", "");
                    user.put("department", "");
                }
                user.put("is_me", Objects.equals(fio, MainActivity.name));
                TagNode[] is = tds[2].getAllElements(false);
                if(is != null && is.length > 0){
                    m = Pattern.compile("^icon-expand_.* (.*)$").matcher(is[0].getAttributeByName("class"));
                    if(m.find()){
                        user.put("change", m.group(1).trim());
                    } else {
                        user.put("change", "none");
                    }
                    user.put("delta", is[0].getAttributeByName("title"));
                } else {
                    user.put("change", "none");
                    user.put("delta", "0");
                }
                list.put(user);
            }
            json.put("list", list);
            return json;
        } catch (Exception e) {
            if(LoginActivity.errorTracker != null) LoginActivity.errorTracker.add(e);
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
    public View getView(int position, View view, @NonNull ViewGroup parent) { //?attr/textColorPassed
        LayoutInflater inflater = context.getLayoutInflater();
        HashMap<String, String> user = users.get(position);
        View rowView = inflater.inflate(R.layout.listview_rating_list, null, true);
        TypedValue typedValue = new TypedValue();
        if(Objects.equals(user.get("is_me"), "1")){
            ViewGroup vg = ((ViewGroup) rowView.findViewById(R.id.lvrl_number_layout));
            if (vg != null) {
                vg.removeAllViews();
                vg.addView(((LayoutInflater) context.getSystemService(Context.LAYOUT_INFLATER_SERVICE)).inflate(R.layout.triangle_mark_layout, null), 0, new ViewGroup.LayoutParams(ViewGroup.LayoutParams.MATCH_PARENT, ViewGroup.LayoutParams.MATCH_PARENT));
            }
            this.context.getTheme().resolveAttribute(R.attr.colorPrimaryOpacity, typedValue, true);
            rowView.setBackgroundColor(typedValue.data);
            rowView.findViewById(R.id.lvrl_layout).setPadding(32, 0, 16, 0);
        }
        ((TextView) rowView.findViewById(R.id.lvrl_number)).setText(user.get("number"));
        ((TextView) rowView.findViewById(R.id.lvrl_fio)).setText(user.get("fio"));
        ((TextView) rowView.findViewById(R.id.lvrl_meta)).setText(user.get("meta"));
        if(!Objects.equals(user.get("change"), "none")){
            TextView lvrl_delta = (TextView) rowView.findViewById(R.id.lvrl_delta);
            lvrl_delta.setText(user.get("delta"));
            switch (user.get("change")){
                case "up":
                    this.context.getTheme().resolveAttribute(R.attr.textColorPassed, typedValue, true);
                    lvrl_delta.setTextColor(typedValue.data);
                    break;
                case "down":
                    this.context.getTheme().resolveAttribute(R.attr.textColorDegrade, typedValue, true);
                    lvrl_delta.setTextColor(typedValue.data);
                    break;
            }
        }
        return rowView;
    }
}
package com.bukhmastov.cdoitmo.activities;

import android.content.Context;
import android.os.Bundle;
import android.support.v4.widget.SwipeRefreshLayout;
import android.support.v7.app.ActionBar;
import android.support.v7.app.AppCompatActivity;
import android.support.v7.widget.Toolbar;
import android.view.LayoutInflater;
import android.view.MenuItem;
import android.view.View;
import android.view.ViewGroup;
import android.widget.ListView;
import android.widget.TextView;

import com.bukhmastov.cdoitmo.R;
import com.bukhmastov.cdoitmo.adapters.RatingTopListView;
import com.bukhmastov.cdoitmo.network.Client;
import com.bukhmastov.cdoitmo.network.DeIfmoClient;
import com.bukhmastov.cdoitmo.network.IfmoClient;
import com.bukhmastov.cdoitmo.network.interfaces.DeIfmoClientResponseHandler;
import com.bukhmastov.cdoitmo.parse.RatingTopListParse;
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

public class RatingListActivity extends AppCompatActivity implements SwipeRefreshLayout.OnRefreshListener {

    private static final String TAG = "RatingListActivity";
    private String faculty = null;
    private String course = null;
    private String years = null;
    private boolean loaded = false;
    private RequestHandle activityRequestHandle = null;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        if (Static.darkTheme) setTheme(R.style.AppTheme_Dark);
        super.onCreate(savedInstanceState);
        Log.i(TAG, "Activity created");
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
        if (Objects.equals(years, "") || years == null) {
            Calendar now = Calendar.getInstance();
            int year = now.get(Calendar.YEAR);
            int month = now.get(Calendar.MONTH);
            years = month > Calendar.AUGUST ? year + "/" + (year + 1) : (year - 1) + "/" + year;
        }
        Log.v(TAG, "faculty=" + faculty + " | course=" + course + " | years=" + years);
        if (Objects.equals(faculty, "") || faculty == null || Objects.equals(course, "") || course == null) {
            Log.w(TAG, "wrong intent extras provided | faculty=" + faculty + " | course=" + course);
            finish();
        }
    }

    @Override
    protected void onDestroy() {
        super.onDestroy();
        Log.i(TAG, "Activity destroyed");
    }

    @Override
    public void onResume() {
        super.onResume();
        Log.v(TAG, "Activity resumed");
        if (!loaded) {
            loaded = true;
            load();
        }
    }

    @Override
    protected void onPause() {
        super.onPause();
        Log.v(TAG, "Activity paused");
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
        Log.v(TAG, "load");
        if (getSupportActionBar() != null) getSupportActionBar().setTitle("Топ-рейтинг");
        if (Static.OFFLINE_MODE) {
            try {
                Static.snackBar(this, getString(R.string.offline_mode_on));
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
            } catch (Exception e) {
                Static.error(e);
            }
            return;
        }
        DeIfmoClient.get(this, Client.Protocol.HTTP, "?node=rating&std&depId=" + faculty + "&year=" + course + "&app=" + years, null, new DeIfmoClientResponseHandler() {
            @Override
            public void onSuccess(int statusCode, String response) {
                Log.v(TAG, "load | success | statusCode=" + statusCode);
                if (statusCode == 200) {
                    new RatingTopListParse(new RatingTopListParse.response() {
                        @Override
                        public void finish(JSONObject json) {
                            display(json);
                        }
                    }).execute(response, Storage.file.perm.get(getBaseContext(), "user#name"));
                } else {
                    loadFailed();
                }
            }
            @Override
            public void onProgress(int state) {
                Log.v(TAG, "load | progress " + state);
                draw(R.layout.state_loading);
                TextView loading_message = (TextView) findViewById(R.id.loading_message);
                if (loading_message != null) {
                    switch (state) {
                        case IfmoClient.STATE_HANDLING: loading_message.setText(R.string.loading); break;
                    }
                }
            }
            @Override
            public void onFailure(int state) {
                Log.v(TAG, "load | failure " + state);
                switch (state) {
                    case IfmoClient.FAILED_OFFLINE:
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
                    case IfmoClient.FAILED_TRY_AGAIN:
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
                        break;
                }
            }
            @Override
            public void onNewHandle(RequestHandle requestHandle) {
                activityRequestHandle = requestHandle;
            }
        });
    }
    private void loadFailed(){
        Log.v(TAG, "loadFailed");
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
            Static.error(e);
        }
    }
    private void display(JSONObject data){
        Log.v(TAG, "display");
        try {
            if (data == null) throw new NullPointerException("display(JSONObject data) can't be null");
            if (getSupportActionBar() != null) getSupportActionBar().setTitle(data.getString("header"));
            // получаем список для отображения рейтинга
            final ArrayList<HashMap<String, String>> users = new ArrayList<>();
            JSONArray jsonArray = data.getJSONArray("list");
            for (int i = 0; i < jsonArray.length(); i++) {
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
            if (rl_list_view != null) rl_list_view.setAdapter(new RatingTopListView(this, users));
            // работаем со свайпом
            SwipeRefreshLayout mSwipeRefreshLayout = (SwipeRefreshLayout) findViewById(R.id.swipe_container);
            if (mSwipeRefreshLayout != null) {
                mSwipeRefreshLayout.setColorSchemeColors(Static.colorAccent);
                mSwipeRefreshLayout.setProgressBackgroundColorSchemeColor(Static.colorBackgroundRefresh);
                mSwipeRefreshLayout.setOnRefreshListener(this);
            }
        } catch(Exception e){
            Static.error(e);
            loadFailed();
        }
    }
    private void draw(int layoutId){
        try {
            ViewGroup vg = ((ViewGroup) findViewById(R.id.rating_list_container));
            if (vg != null) {
                vg.removeAllViews();
                vg.addView(((LayoutInflater) getSystemService(Context.LAYOUT_INFLATER_SERVICE)).inflate(layoutId, null), 0, new ViewGroup.LayoutParams(ViewGroup.LayoutParams.MATCH_PARENT, ViewGroup.LayoutParams.MATCH_PARENT));
            }
        } catch (Exception e) {
            Static.error(e);
        }
    }

}
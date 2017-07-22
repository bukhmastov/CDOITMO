package com.bukhmastov.cdoitmo.fragments;

import android.app.Activity;
import android.content.Context;
import android.os.Bundle;
import android.support.annotation.LayoutRes;
import android.support.annotation.Nullable;
import android.support.v4.app.Fragment;
import android.support.v4.widget.SwipeRefreshLayout;
import android.support.v7.widget.LinearLayoutManager;
import android.support.v7.widget.RecyclerView;
import android.view.InflateException;
import android.view.KeyEvent;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.EditText;
import android.widget.FrameLayout;
import android.widget.LinearLayout;
import android.widget.TextView;

import com.bukhmastov.cdoitmo.R;
import com.bukhmastov.cdoitmo.adapters.NewsRecyclerViewAdapter;
import com.bukhmastov.cdoitmo.adapters.RecyclerViewOnScrollListener;
import com.bukhmastov.cdoitmo.adapters.UniversityRecyclerViewAdapter;
import com.bukhmastov.cdoitmo.firebase.FirebaseAnalyticsProvider;
import com.bukhmastov.cdoitmo.network.IfmoRestClient;
import com.bukhmastov.cdoitmo.network.interfaces.IfmoClientResponseHandler;
import com.bukhmastov.cdoitmo.network.interfaces.IfmoRestClientResponseHandler;
import com.bukhmastov.cdoitmo.utils.Log;
import com.bukhmastov.cdoitmo.utils.Static;
import com.bukhmastov.cdoitmo.utils.Storage;
import com.loopj.android.http.RequestHandle;

import org.json.JSONArray;
import org.json.JSONException;
import org.json.JSONObject;

import java.util.ArrayList;
import java.util.Calendar;

public class UniversityNewsFragment extends Fragment implements SwipeRefreshLayout.OnRefreshListener {

    private static final String TAG = "UniversityNewsFragment";
    private Activity activity;
    private View container;
    private RequestHandle fragmentRequestHandle = null;
    private boolean loaded = false;
    private JSONObject news = null;
    private int limit = 10;
    private int offset = 0;
    private String search = "";
    private NewsRecyclerViewAdapter newsRecyclerViewAdapter = null;
    private long timestamp = 0;
    private Thread thread = null;

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
            clearAndLoad();
        }
    }

    @Override
    public void onPause() {
        super.onPause();
        Log.v(TAG, "paused");
    }

    @Override
    public View onCreateView(LayoutInflater inflater, ViewGroup cont, Bundle savedInstanceState) {
        View view = inflater.inflate(R.layout.fragment_university_tab, cont, false);
        container = view.findViewById(R.id.university_tab_container);
        return view;
    }

    @Override
    public void onRefresh() {
        Log.v(TAG, "refreshed");
        searchAndLoad(search, 0);
    }

    private void clearAndLoad() {
        searchAndLoad("");
    }
    private void searchAndLoad(String search) {
        searchAndLoad(search,
                Storage.pref.get(getContext(), "pref_use_cache", true) && Storage.pref.get(getContext(), "pref_use_university_cache", false)
                        ? Integer.parseInt(Storage.pref.get(getContext(), "pref_dynamic_refresh", "0"))
                        : 0);
    }
    private void searchAndLoad(final String search, final int refresh_rate) {
        if (thread != null) {
            if (thread.isAlive() && !thread.isInterrupted()) {
                thread.interrupt();
            }
            thread = null;
        }
        thread = new Thread(new Runnable() {
            @Override
            public void run() {
                String cache = Storage.file.cache.get(getContext(), "university#news").trim();
                if (cache.isEmpty()) {
                    searchAndLoad(search, refresh_rate, true);
                } else {
                    try {
                        JSONObject cacheJson = new JSONObject(cache);
                        news = cacheJson.getJSONObject("data");
                        timestamp = cacheJson.getLong("timestamp");
                        if (timestamp + refresh_rate * 3600000L < Calendar.getInstance().getTimeInMillis()) {
                            searchAndLoad(search, refresh_rate, true);
                        } else {
                            searchAndLoad(search, refresh_rate, false);
                        }
                    } catch (JSONException e) {
                        Static.error(e);
                        searchAndLoad(search, refresh_rate, true);
                    }
                }
            }
        });
        thread.setName("UniversityNewsThread");
        thread.start();
    }
    private void searchAndLoad(final String search, final int refresh_rate, final boolean force) {
        Log.v(TAG, "searchAndLoad | search=" + search + " | refresh_rate=" + refresh_rate + " | force=" + (force ? "true" : "false"));
        if ((!force || !Static.isOnline(getContext())) && news != null) {
            display();
            return;
        }
        if (!Static.OFFLINE_MODE) {
            this.offset = 0;
            this.search = search;
            loadProvider(new IfmoRestClientResponseHandler() {
                @Override
                public void onSuccess(int statusCode, JSONObject json, JSONArray responseArr) {
                    if (statusCode == 200) {
                        long now = Calendar.getInstance().getTimeInMillis();
                        if (json != null && Storage.pref.get(getContext(), "pref_use_cache", true) && Storage.pref.get(getContext(), "pref_use_university_cache", false)) {
                            try {
                                Storage.file.cache.put(getContext(), "university#news", new JSONObject()
                                        .put("timestamp", now)
                                        .put("data", json)
                                        .toString()
                                );
                            } catch (JSONException e) {
                                Static.error(e);
                            }
                        }
                        news = json;
                        timestamp = now;
                        display();
                    } else {
                        loadFailed();
                    }
                }
                @Override
                public void onProgress(final int state) {
                    activity.runOnUiThread(new Runnable() {
                        @Override
                        public void run() {
                            Log.v(TAG, "searchAndLoad | progress " + state);
                            draw(R.layout.state_loading);
                            if (activity != null) {
                                TextView loading_message = (TextView) container.findViewById(R.id.loading_message);
                                if (loading_message != null) {
                                    switch (state) {
                                        case IfmoRestClient.STATE_HANDLING:
                                            loading_message.setText(R.string.loading);
                                            break;
                                    }
                                }
                            }
                        }
                    });
                }
                @Override
                public void onFailure(final int state) {
                    activity.runOnUiThread(new Runnable() {
                        @Override
                        public void run() {
                            Log.v(TAG, "searchAndLoad | failure " + state);
                            switch (state) {
                                case IfmoRestClient.FAILED_OFFLINE:
                                    draw(R.layout.state_offline);
                                    if (activity != null) {
                                        View offline_reload = container.findViewById(R.id.offline_reload);
                                        if (offline_reload != null) {
                                            offline_reload.setOnClickListener(new View.OnClickListener() {
                                                @Override
                                                public void onClick(View v) {
                                                    clearAndLoad();
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
                                                    clearAndLoad();
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
        } else {
            draw(R.layout.state_offline);
            if (activity != null) {
                View offline_reload = activity.findViewById(R.id.offline_reload);
                if (offline_reload != null) {
                    offline_reload.setOnClickListener(new View.OnClickListener() {
                        @Override
                        public void onClick(View v) {
                            searchAndLoad(search, refresh_rate);
                        }
                    });
                }
            }
        }
    }
    private void loadProvider(IfmoRestClientResponseHandler handler) {
        loadProvider(handler, 0);
    }
    private void loadProvider(final IfmoRestClientResponseHandler handler, final int attempt) {
        Log.v(TAG, "loadProvider | attempt=" + attempt);
        IfmoRestClient.getPlainSync(getContext(), "news.ifmo.ru/news?limit=" + limit + "&offset=" + offset + "&search=" + search, null, new IfmoClientResponseHandler() {
            @Override
            public void onSuccess(int statusCode, String response) {
                try {
                    if (statusCode == 200) {
                        handler.onSuccess(statusCode, new JSONObject(response), null);
                    } else {
                        handler.onFailure(IfmoRestClient.FAILED_TRY_AGAIN);
                    }
                } catch (Exception e) {
                    if (attempt < 3) {
                        loadProvider(handler, attempt + 1);
                    } else {
                        handler.onFailure(IfmoRestClient.FAILED_TRY_AGAIN);
                    }
                }
            }
            @Override
            public void onProgress(int state) {
                handler.onProgress(state);
            }
            @Override
            public void onFailure(int state) {
                handler.onFailure(state);
            }
            @Override
            public void onNewHandle(RequestHandle requestHandle) {
                handler.onNewHandle(requestHandle);
            }
        });
    }
    private void loadFailed(){
        activity.runOnUiThread(new Runnable() {
            @Override
            public void run() {
                Log.v(TAG, "loadFailed");
                try {
                    draw(R.layout.state_try_again);
                    TextView try_again_message = (TextView) container.findViewById(R.id.try_again_message);
                    if (try_again_message != null) try_again_message.setText(R.string.load_failed);
                    View try_again_reload = container.findViewById(R.id.try_again_reload);
                    if (try_again_reload != null) {
                        try_again_reload.setOnClickListener(new View.OnClickListener() {
                            @Override
                            public void onClick(View v) {
                                clearAndLoad();
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
        final SwipeRefreshLayout.OnRefreshListener onRefreshListener = this;
        activity.runOnUiThread(new Runnable() {
            @Override
            public void run() {
                Log.v(TAG, "display");
                if (news == null) {
                    loadFailed();
                    return;
                }
                try {
                    draw(R.layout.layout_university_news_list);
                    // поиск
                    final EditText search_input = (EditText) container.findViewById(R.id.search_input);
                    final FrameLayout search_action = (FrameLayout) container.findViewById(R.id.search_action);
                    search_action.setOnClickListener(new View.OnClickListener() {
                        @Override
                        public void onClick(View v) {
                            searchAndLoad(search_input.getText().toString().trim());
                        }
                    });
                    search_input.setOnKeyListener(new View.OnKeyListener() {
                        @Override
                        public boolean onKey(View v, int keyCode, KeyEvent event) {
                            if (event.getAction() == KeyEvent.ACTION_DOWN && keyCode == KeyEvent.KEYCODE_ENTER) {
                                searchAndLoad(search_input.getText().toString().trim());
                                return true;
                            }
                            return false;
                        }
                    });
                    search_input.setText(search);
                    // очищаем сообщение
                    ViewGroup news_list_info = (ViewGroup) container.findViewById(R.id.news_list_info);
                    news_list_info.removeAllViews();
                    news_list_info.setPadding(0, 0, 0, 0);
                    // список
                    JSONArray list = news.getJSONArray("list");
                    if (list.length() > 0) {
                        newsRecyclerViewAdapter = new NewsRecyclerViewAdapter(getContext());
                        final RecyclerView news_list = (RecyclerView) container.findViewById(R.id.news_list);
                        news_list.setLayoutManager(new LinearLayoutManager(getContext()));
                        news_list.setAdapter(newsRecyclerViewAdapter);
                        news_list.addOnScrollListener(new RecyclerViewOnScrollListener(container));
                        newsRecyclerViewAdapter.setOnStateClickListener(R.id.load_more, new View.OnClickListener() {
                            @Override
                            public void onClick(View v) {
                                offset += limit;
                                newsRecyclerViewAdapter.setState(R.id.loading_more);
                                if (thread != null) {
                                    if (thread.isAlive() && !thread.isInterrupted()) {
                                        thread.interrupt();
                                    }
                                    thread = null;
                                }
                                thread = new Thread(new Runnable() {
                                    @Override
                                    public void run() {
                                        loadProvider(new IfmoRestClientResponseHandler() {
                                            @Override
                                            public void onSuccess(int statusCode, final JSONObject json, JSONArray responseArr) {
                                                activity.runOnUiThread(new Runnable() {
                                                    @Override
                                                    public void run() {
                                                        try {
                                                            news.put("count", json.getInt("count"));
                                                            news.put("limit", json.getInt("limit"));
                                                            news.put("offset", json.getInt("offset"));
                                                            JSONArray list_original = news.getJSONArray("list");
                                                            JSONArray list = json.getJSONArray("list");
                                                            for (int i = 0; i < list.length(); i++) {
                                                                list_original.put(list.getJSONObject(i));
                                                            }
                                                            displayContent(list);
                                                        } catch (Exception e) {
                                                            Static.error(e);
                                                            newsRecyclerViewAdapter.setState(R.id.load_more);
                                                        }
                                                    }
                                                });
                                            }
                                            @Override
                                            public void onProgress(int state) {}
                                            @Override
                                            public void onFailure(int state) {
                                                activity.runOnUiThread(new Runnable() {
                                                    @Override
                                                    public void run() {
                                                        newsRecyclerViewAdapter.setState(R.id.load_more);
                                                    }
                                                });
                                            }
                                            @Override
                                            public void onNewHandle(RequestHandle requestHandle) {
                                                fragmentRequestHandle = requestHandle;
                                            }
                                        });
                                    }
                                });
                                thread.setName("UniversityNewsThread");
                                thread.start();
                            }
                        });
                        if (timestamp > 0 && timestamp + 5000 < Calendar.getInstance().getTimeInMillis()) {
                            UniversityRecyclerViewAdapter.Item item = new UniversityRecyclerViewAdapter.Item();
                            item.type = UniversityRecyclerViewAdapter.TYPE_INFO_ABOUT_UPDATE_TIME;
                            item.data = new JSONObject().put("title", getString(R.string.update_date) + " " + Static.getUpdateTime(activity, timestamp));
                            newsRecyclerViewAdapter.addItem(item);
                        }
                        displayContent(list);
                    } else {
                        View view = inflate(R.layout.nothing_to_display);
                        ((TextView) view.findViewById(R.id.ntd_text)).setText(R.string.no_news);
                        news_list_info.addView(view);
                    }
                    // добавляем отступ
                    container.findViewById(R.id.top_panel).post(new Runnable() {
                        @Override
                        public void run() {
                            try {
                                int height = container.findViewById(R.id.top_panel).getHeight();
                                RecyclerView news_list = (RecyclerView) container.findViewById(R.id.news_list);
                                news_list.setPadding(0, height, 0, 0);
                                news_list.scrollToPosition(0);
                                LinearLayout news_list_info = (LinearLayout) container.findViewById(R.id.news_list_info);
                                if (news_list_info.getChildCount() > 0) {
                                    news_list_info.setPadding(0, height, 0, 0);
                                }
                            } catch (Exception e) {
                                e.printStackTrace();
                            }
                        }
                    });
                    // работаем со свайпом
                    SwipeRefreshLayout mSwipeRefreshLayout = (SwipeRefreshLayout) container.findViewById(R.id.news_list_swipe);
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
    private void displayContent(final JSONArray list) {
        activity.runOnUiThread(new Runnable() {
            @Override
            public void run() {
                try {
                    if (newsRecyclerViewAdapter != null) {
                        ArrayList<NewsRecyclerViewAdapter.Item> items = new ArrayList<>();
                        for (int i = 0; i < list.length(); i++) {
                            try {
                                final JSONObject news = list.getJSONObject(i);
                                NewsRecyclerViewAdapter.Item item = new NewsRecyclerViewAdapter.Item();
                                item.type = getBoolean(news, "main") ? NewsRecyclerViewAdapter.TYPE_MAIN : NewsRecyclerViewAdapter.TYPE_MINOR;
                                item.data = news;
                                items.add(item);
                            } catch (Exception e) {
                                Static.error(e);
                            }
                        }
                        newsRecyclerViewAdapter.addItem(items);
                        if (offset + limit < news.getInt("count")) {
                            newsRecyclerViewAdapter.setState(R.id.load_more);
                        } else {
                            newsRecyclerViewAdapter.setState(R.id.no_more);
                        }
                    }
                } catch (Exception e) {
                    Static.error(e);
                    loadFailed();
                }
            }
        });
    }

    private boolean getBoolean(JSONObject json, String key) throws JSONException {
        if (json.has(key)) {
            try {
                return json.getBoolean(key);
            } catch (Exception e) {
                return true;
            }
        } else {
            return true;
        }
    }

    private void draw(final int layoutId){
        activity.runOnUiThread(new Runnable() {
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

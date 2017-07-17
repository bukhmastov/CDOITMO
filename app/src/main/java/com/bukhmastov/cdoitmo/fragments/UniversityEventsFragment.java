package com.bukhmastov.cdoitmo.fragments;

import android.app.Activity;
import android.content.Context;
import android.content.Intent;
import android.graphics.Color;
import android.os.Bundle;
import android.support.annotation.IdRes;
import android.support.annotation.LayoutRes;
import android.support.annotation.Nullable;
import android.support.v4.app.Fragment;
import android.support.v4.widget.SwipeRefreshLayout;
import android.view.InflateException;
import android.view.KeyEvent;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.EditText;
import android.widget.FrameLayout;
import android.widget.ImageView;
import android.widget.TextView;

import com.bukhmastov.cdoitmo.R;
import com.bukhmastov.cdoitmo.activities.WebViewActivity;
import com.bukhmastov.cdoitmo.firebase.FirebaseAnalyticsProvider;
import com.bukhmastov.cdoitmo.network.IfmoRestClient;
import com.bukhmastov.cdoitmo.network.interfaces.IfmoRestClientResponseHandler;
import com.bukhmastov.cdoitmo.utils.Log;
import com.bukhmastov.cdoitmo.utils.Static;
import com.loopj.android.http.RequestHandle;
import com.squareup.picasso.Callback;
import com.squareup.picasso.Picasso;

import org.json.JSONArray;
import org.json.JSONException;
import org.json.JSONObject;

public class UniversityEventsFragment extends Fragment implements SwipeRefreshLayout.OnRefreshListener {

    private static final String TAG = "UniversityEventsFragment";
    private Activity activity;
    private View container;
    private RequestHandle fragmentRequestHandle = null;
    private boolean loaded = false;
    private JSONObject events = null;
    private int limit = 20;
    private int offset = 0;
    private String search = "";

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
        if (fragmentRequestHandle != null) {
            loaded = false;
            fragmentRequestHandle.cancel(true);
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
        Log.v(TAG, "refreshed");
        searchAndLoad(search);
    }

    private void clearAndLoad() {
        searchAndLoad("");
    }
    private void searchAndLoad(String search) {
        Log.v(TAG, "searchAndLoad");
        this.offset = 0;
        this.search = search;
        loadProvider(new IfmoRestClientResponseHandler() {
            @Override
            public void onSuccess(int statusCode, JSONObject json, JSONArray responseArr) {
                if (statusCode == 200) {
                    events = json;
                    display();
                } else {
                    loadFailed();
                }
            }
            @Override
            public void onProgress(int state) {
                Log.v(TAG, "searchAndLoad | progress " + state);
                draw(R.layout.state_loading);
                if (activity != null) {
                    TextView loading_message = (TextView) container.findViewById(R.id.loading_message);
                    if (loading_message != null) {
                        switch (state) {
                            case IfmoRestClient.STATE_HANDLING: loading_message.setText(R.string.loading); break;
                        }
                    }
                }
            }
            @Override
            public void onFailure(int state) {
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
            @Override
            public void onNewHandle(RequestHandle requestHandle) {
                fragmentRequestHandle = requestHandle;
            }
        });
    }
    private void loadProvider(IfmoRestClientResponseHandler handler) {
        loadProvider(handler, 0);
    }
    private void loadProvider(final IfmoRestClientResponseHandler handler, final int attempt) {
        Log.v(TAG, "loadProvider | attempt=" + attempt);
        IfmoRestClient.get(getContext(), "event?limit=" + limit + "&offset=" + offset + "&search=" + search, null, new IfmoRestClientResponseHandler() {
            @Override
            public void onSuccess(int statusCode, JSONObject responseObj, JSONArray responseArr) {
                handler.onSuccess(statusCode, responseObj, responseArr);
            }
            @Override
            public void onProgress(int state) {
                handler.onProgress(state);
            }
            @Override
            public void onFailure(int state) {
                if (state == IfmoRestClient.FAILED_TRY_AGAIN && attempt < 3) {
                    loadProvider(handler, attempt + 1);
                } else {
                    handler.onFailure(state);
                }
            }
            @Override
            public void onNewHandle(RequestHandle requestHandle) {
                handler.onNewHandle(requestHandle);
            }
        });
    }
    private void loadFailed(){
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
    private void display() {
        Log.v(TAG, "display");
        if (events == null) {
            loadFailed();
            return;
        }
        try {
            draw(R.layout.layout_university_events_list);
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
            // список
            ViewGroup events_list = (ViewGroup) container.findViewById(R.id.events_list);
            JSONArray list = events.getJSONArray("list");
            if (list.length() > 0) {
                displayContent(list, events_list);
            } else {
                View view = inflate(R.layout.nothing_to_display);
                ((TextView) view.findViewById(R.id.ntd_text)).setText(R.string.no_events);
                events_list.addView(view);
            }
            // работаем со свайпом
            SwipeRefreshLayout mSwipeRefreshLayout = (SwipeRefreshLayout) container.findViewById(R.id.events_list_swipe);
            if (mSwipeRefreshLayout != null) {
                mSwipeRefreshLayout.setColorSchemeColors(Static.colorAccent);
                mSwipeRefreshLayout.setProgressBackgroundColorSchemeColor(Static.colorBackgroundRefresh);
                mSwipeRefreshLayout.setOnRefreshListener(this);
            }
        } catch (Exception e) {
            Static.error(e);
            loadFailed();
        }
    }
    private void displayContent(final JSONArray list, final ViewGroup container) throws Exception {
        for (int i = 0; i < list.length(); i++) {
            try {
                final JSONObject event = list.getJSONObject(i);
                String title = getString(event, "name");
                String img = getString(event, "logo");
                String type = getString(event, "type_name");
                String color_hex = "#DF1843";
                String date_begin = getString(event, "date_begin");
                String date_end = getString(event, "date_end");
                final String webview = getString(event, "url_webview");
                if (title == null || title.trim().isEmpty()) {
                    // skip event with empty title
                    continue;
                }
                View layout = inflate(R.layout.layout_university_news_card_compact);
                final View news_image_container = layout.findViewById(R.id.news_image_container);
                if (img != null && !img.trim().isEmpty()) {
                    Picasso.with(getContext())
                            .load(img)
                            .into((ImageView) layout.findViewById(R.id.news_image), new Callback() {
                                @Override
                                public void onSuccess() {}
                                @Override
                                public void onError() {
                                    Static.removeView(news_image_container);
                                }
                            });
                } else {
                    Static.removeView(news_image_container);
                }
                ((TextView) layout.findViewById(R.id.title)).setText(Static.escapeString(title));
                if (type != null && !type.trim().isEmpty()) {
                    TextView categories = (TextView) layout.findViewById(R.id.categories);
                    categories.setText("● " + type);
                    categories.setTextColor(Color.parseColor(color_hex));
                } else {
                    Static.removeView(layout.findViewById(R.id.categories));
                }
                boolean date_begin_exists = date_begin != null && !date_begin.trim().isEmpty();
                boolean date_end_exists = date_end != null && !date_end.trim().isEmpty();
                if (date_begin_exists || date_end_exists) {
                    String date = null;
                    if (date_begin_exists && date_end_exists) {
                        date = Static.cuteDate(getContext(), "yyyy-MM-dd HH:mm:ss", date_begin, date_end);
                    } else if (date_begin_exists) {
                        date = Static.cuteDate(getContext(), "yyyy-MM-dd HH:mm:ss", date_begin);
                    } else if (date_end_exists) {
                        date = Static.cuteDate(getContext(), "yyyy-MM-dd HH:mm:ss", date_end);
                    }
                    ((TextView) layout.findViewById(R.id.date)).setText(date);
                } else {
                    Static.removeView(layout.findViewById(R.id.date));
                }
                Static.removeView(layout.findViewById(R.id.count_view_container));
                if (webview != null && !webview.trim().isEmpty()) {
                    layout.findViewById(R.id.news_click).setOnClickListener(new View.OnClickListener() {
                        @Override
                        public void onClick(View v) {
                            Intent intent = new Intent(getContext(), WebViewActivity.class);
                            Bundle extras = new Bundle();
                            extras.putString("url", webview.trim());
                            extras.putString("title", getString(R.string.events));
                            intent.putExtras(extras);
                            startActivity(intent);
                        }
                    });
                }
                container.addView(layout);
            } catch (Exception e) {
                Static.error(e);
            }
        }
        if (offset + limit < events.getInt("count")) {
            manageLayoutUniversityListItemState(container, R.id.load_more, loadMoreListener(container));
        } else {
            manageLayoutUniversityListItemState(container, R.id.no_more, null);
        }
    }

    private void manageLayoutUniversityListItemState(ViewGroup container, @IdRes int keep, View.OnClickListener onClickListener) {
        View load_manager = container.findViewById(R.id.load_manager);
        if (load_manager != null) {
            Static.removeView(load_manager);
        }
        View item_state = inflate(R.layout.layout_university_list_item_state);
        manageLayoutUniversityListItemState(item_state, keep, onClickListener);
        container.addView(item_state);
    }
    private void manageLayoutUniversityListItemState(View item_state_view, @IdRes int keep, View.OnClickListener onClickListener) {
        ViewGroup item_state = (ViewGroup) item_state_view;
        for (int i = item_state.getChildCount() - 1; i >= 0; i--) {
            View item = item_state.getChildAt(i);
            if (item.getId() != keep) {
                Static.removeView(item);
            }
        }
        if (onClickListener != null) {
            item_state.setOnClickListener(onClickListener);
        }
    }
    private View.OnClickListener loadMoreListener(final ViewGroup container) {
        return new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                offset += limit;
                manageLayoutUniversityListItemState(container, R.id.loading_more, null);
                loadProvider(new IfmoRestClientResponseHandler() {
                    @Override
                    public void onSuccess(int statusCode, JSONObject json, JSONArray responseArr) {
                        try {
                            events.put("count", json.getInt("count"));
                            events.put("limit", json.getInt("limit"));
                            events.put("offset", json.getInt("offset"));
                            JSONArray list_original = events.getJSONArray("list");
                            JSONArray list = json.getJSONArray("list");
                            for (int i = 0; i < list.length(); i++) {
                                list_original.put(list.getJSONObject(i));
                            }
                            displayContent(list, container);
                        } catch (Exception e) {
                            Static.error(e);
                            manageLayoutUniversityListItemState(container, R.id.load_more, loadMoreListener(container));
                        }
                    }
                    @Override
                    public void onProgress(int state) {}
                    @Override
                    public void onFailure(int state) {
                        manageLayoutUniversityListItemState(container, R.id.load_more, loadMoreListener(container));
                    }
                    @Override
                    public void onNewHandle(RequestHandle requestHandle) {
                        fragmentRequestHandle = requestHandle;
                    }
                });
            }
        };
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

    private void draw(int layoutId){
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
    private View inflate(@LayoutRes int layoutId) throws InflateException {
        return ((LayoutInflater) activity.getSystemService(Context.LAYOUT_INFLATER_SERVICE)).inflate(layoutId, null);
    }
}

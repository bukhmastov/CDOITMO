package com.bukhmastov.cdoitmo.fragments;

import android.app.Activity;
import android.content.Context;
import android.content.Intent;
import android.os.Bundle;
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
import android.widget.LinearLayout;
import android.widget.TextView;

import com.bukhmastov.cdoitmo.R;
import com.bukhmastov.cdoitmo.activities.UniversityPersonCardActivity;
import com.bukhmastov.cdoitmo.firebase.FirebaseAnalyticsProvider;
import com.bukhmastov.cdoitmo.network.IfmoRestClient;
import com.bukhmastov.cdoitmo.network.interfaces.IfmoRestClientResponseHandler;
import com.bukhmastov.cdoitmo.utils.CircularTransformation;
import com.bukhmastov.cdoitmo.utils.Log;
import com.bukhmastov.cdoitmo.utils.Static;
import com.loopj.android.http.RequestHandle;
import com.squareup.picasso.Picasso;

import org.json.JSONArray;
import org.json.JSONObject;

public class UniversityPersonsFragment extends Fragment implements SwipeRefreshLayout.OnRefreshListener {

    private static final String TAG = "UniversityPersonsFragment";
    private Activity activity;
    private View container;
    private RequestHandle fragmentRequestHandle = null;
    private boolean loaded = false;
    private JSONObject persons = null;
    private int limit = 20;
    private int offset = 0;
    private String search = "";
    private boolean block_load_more = false;

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
                    persons = json;
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
        IfmoRestClient.get(getContext(), "person?limit=" + limit + "&offset=" + offset + "&search=" + search, null, handler);
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
        if (persons == null) {
            loadFailed();
            return;
        }
        try {
            draw(R.layout.layout_university_persons_list);
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
            LinearLayout persons_list = (LinearLayout) container.findViewById(R.id.persons_list);
            JSONArray list = persons.getJSONArray("list");
            if (list.length() > 0) {
                displayContent(list, persons_list);
            } else {
                View view = inflate(R.layout.nothing_to_display);
                ((TextView) view.findViewById(R.id.ntd_text)).setText(R.string.no_persons);
                persons_list.addView(view);
            }
            // работаем со свайпом
            SwipeRefreshLayout mSwipeRefreshLayout = (SwipeRefreshLayout) container.findViewById(R.id.persons_list_swipe);
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
    private void displayContent(final JSONArray list, final LinearLayout container) throws Exception {
        for (int i = 0; i < list.length(); i++) {
            final JSONObject person = list.getJSONObject(i);
            final String name = (person.getString("title_l") + " " + person.getString("title_f") + " " + person.getString("title_m")).trim();
            final String degree = person.getString("degree").trim();
            final String image = person.getString("image");
            final LinearLayout layout_university_persons_list_item = (LinearLayout) inflate(R.layout.layout_university_persons_list_item);
            ((TextView) layout_university_persons_list_item.findViewById(R.id.name)).setText(name);
            if (!degree.isEmpty()) {
                ((TextView) layout_university_persons_list_item.findViewById(R.id.post)).setText(degree.substring(0, 1).toUpperCase() + degree.substring(1));
            } else {
                Static.removeView(layout_university_persons_list_item.findViewById(R.id.post));
            }
            Picasso.with(getContext())
                    .load(image)
                    .error(R.drawable.ic_sentiment_very_satisfied)
                    .transform(new CircularTransformation())
                    .into((ImageView) layout_university_persons_list_item.findViewById(R.id.avatar));
            layout_university_persons_list_item.setOnClickListener(new View.OnClickListener() {
                @Override
                public void onClick(View v) {
                    try {
                        Intent intent = new Intent(getContext(), UniversityPersonCardActivity.class);
                        intent.putExtra("pid", person.getInt("persons_id"));
                        intent.putExtra("person", person.toString());
                        startActivity(intent);
                    } catch (Exception e) {
                        Static.error(e);
                    }
                }
            });
            container.addView(layout_university_persons_list_item);
        }
        int count = persons.getInt("count");
        final View layout_university_persons_list_item_state = inflate(R.layout.layout_university_persons_list_item_state);
        container.addView(layout_university_persons_list_item_state);
        if (offset + limit < count) {
            Static.removeView(layout_university_persons_list_item_state.findViewById(R.id.no_more));
            layout_university_persons_list_item_state.setOnClickListener(new View.OnClickListener() {
                @Override
                public void onClick(View v) {
                    if (block_load_more) {
                        return;
                    }
                    block_load_more = true;
                    offset += limit;
                    loadProvider(new IfmoRestClientResponseHandler() {
                        @Override
                        public void onSuccess(int statusCode, JSONObject json, JSONArray responseArr) {
                            block_load_more = false;
                            Static.removeView(layout_university_persons_list_item_state);
                            try {
                                persons.put("count", json.getInt("count"));
                                persons.put("limit", json.getInt("limit"));
                                persons.put("offset", json.getInt("offset"));
                                JSONArray list_original = persons.getJSONArray("list");
                                JSONArray list = json.getJSONArray("list");
                                for (int i = 0; i < list.length(); i++) {
                                    list_original.put(list.getJSONObject(i));
                                }
                                displayContent(list, container);
                            } catch (Exception e) {
                                Static.error(e);
                                final View layout_university_persons_list_item_state = inflate(R.layout.layout_university_persons_list_item_state);
                                container.addView(layout_university_persons_list_item_state);
                                Static.removeView(layout_university_persons_list_item_state.findViewById(R.id.no_more));
                            }
                        }
                        @Override
                        public void onProgress(int state) {}
                        @Override
                        public void onFailure(int state) {
                            block_load_more = false;
                            Static.removeView(layout_university_persons_list_item_state);
                            final View layout_university_persons_list_item_state = inflate(R.layout.layout_university_persons_list_item_state);
                            container.addView(layout_university_persons_list_item_state);
                            Static.removeView(layout_university_persons_list_item_state.findViewById(R.id.no_more));
                        }
                        @Override
                        public void onNewHandle(RequestHandle requestHandle) {
                            fragmentRequestHandle = requestHandle;
                        }
                    });
                }
            });
        } else {
            Static.removeView(layout_university_persons_list_item_state.findViewById(R.id.load_more));
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

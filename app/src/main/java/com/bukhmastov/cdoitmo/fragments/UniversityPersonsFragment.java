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
import com.bukhmastov.cdoitmo.adapters.PersonsRecyclerViewAdapter;
import com.bukhmastov.cdoitmo.adapters.RecyclerViewOnScrollListener;
import com.bukhmastov.cdoitmo.firebase.FirebaseAnalyticsProvider;
import com.bukhmastov.cdoitmo.network.IfmoRestClient;
import com.bukhmastov.cdoitmo.network.interfaces.IfmoClientResponseHandler;
import com.bukhmastov.cdoitmo.network.interfaces.IfmoRestClientResponseHandler;
import com.bukhmastov.cdoitmo.utils.Log;
import com.bukhmastov.cdoitmo.utils.Static;
import com.loopj.android.http.RequestHandle;

import org.json.JSONArray;
import org.json.JSONObject;

import java.util.ArrayList;

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
    private PersonsRecyclerViewAdapter personsRecyclerViewAdapter = null;

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
        loadProvider(handler, 0);
    }
    private void loadProvider(final IfmoRestClientResponseHandler handler, final int attempt) {
        Log.v(TAG, "loadProvider | attempt=" + attempt);
        IfmoRestClient.getPlain(getContext(), "person?limit=" + limit + "&offset=" + offset + "&search=" + search, null, new IfmoClientResponseHandler() {
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
            // очищаем сообщение
            ViewGroup persons_list_info = (ViewGroup) container.findViewById(R.id.persons_list_info);
            persons_list_info.removeAllViews();
            persons_list_info.setPadding(0, 0, 0, 0);
            // список
            JSONArray list = persons.getJSONArray("list");
            if (list.length() > 0) {
                personsRecyclerViewAdapter = new PersonsRecyclerViewAdapter(getContext());
                final RecyclerView persons_list = (RecyclerView) container.findViewById(R.id.persons_list);
                persons_list.setLayoutManager(new LinearLayoutManager(getContext()));
                persons_list.setAdapter(personsRecyclerViewAdapter);
                persons_list.addOnScrollListener(new RecyclerViewOnScrollListener(container));
                personsRecyclerViewAdapter.setOnStateClickListener(R.id.load_more, new View.OnClickListener() {
                    @Override
                    public void onClick(View v) {
                        offset += limit;
                        personsRecyclerViewAdapter.setState(R.id.loading_more);
                        loadProvider(new IfmoRestClientResponseHandler() {
                            @Override
                            public void onSuccess(int statusCode, JSONObject json, JSONArray responseArr) {
                                try {
                                    persons.put("count", json.getInt("count"));
                                    persons.put("limit", json.getInt("limit"));
                                    persons.put("offset", json.getInt("offset"));
                                    JSONArray list_original = persons.getJSONArray("list");
                                    JSONArray list = json.getJSONArray("list");
                                    for (int i = 0; i < list.length(); i++) {
                                        list_original.put(list.getJSONObject(i));
                                    }
                                    displayContent(list);
                                } catch (Exception e) {
                                    Static.error(e);
                                    personsRecyclerViewAdapter.setState(R.id.load_more);
                                }
                            }
                            @Override
                            public void onProgress(int state) {}
                            @Override
                            public void onFailure(int state) {
                                personsRecyclerViewAdapter.setState(R.id.load_more);
                            }
                            @Override
                            public void onNewHandle(RequestHandle requestHandle) {
                                fragmentRequestHandle = requestHandle;
                            }
                        });
                    }
                });
                displayContent(list);
            } else {
                View view = inflate(R.layout.nothing_to_display);
                ((TextView) view.findViewById(R.id.ntd_text)).setText(R.string.no_persons);
                persons_list_info.addView(view);
            }
            // добавляем отступ
            container.findViewById(R.id.top_panel).post(new Runnable() {
                @Override
                public void run() {
                    try {
                        int height = container.findViewById(R.id.top_panel).getHeight();
                        RecyclerView persons_list = (RecyclerView) container.findViewById(R.id.persons_list);
                        persons_list.setPadding(0, height, 0, 0);
                        persons_list.scrollToPosition(0);
                        LinearLayout persons_list_info = (LinearLayout) container.findViewById(R.id.persons_list_info);
                        if (persons_list_info.getChildCount() > 0) {
                            persons_list_info.setPadding(0, height, 0, 0);
                        }
                    } catch (Exception e) {
                        e.printStackTrace();
                    }
                }
            });
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
    private void displayContent(final JSONArray list) throws Exception {
        if (personsRecyclerViewAdapter != null) {
            ArrayList<PersonsRecyclerViewAdapter.Item> items = new ArrayList<>();
            for (int i = 0; i < list.length(); i++) {
                try {
                    final JSONObject news = list.getJSONObject(i);
                    PersonsRecyclerViewAdapter.Item item = new PersonsRecyclerViewAdapter.Item();
                    item.type = PersonsRecyclerViewAdapter.TYPE_MAIN;
                    item.data = news;
                    items.add(item);
                } catch (Exception e) {
                    Static.error(e);
                }
            }
            personsRecyclerViewAdapter.addItem(items);
            if (offset + limit < persons.getInt("count")) {
                personsRecyclerViewAdapter.setState(R.id.load_more);
            } else {
                personsRecyclerViewAdapter.setState(R.id.no_more);
            }
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

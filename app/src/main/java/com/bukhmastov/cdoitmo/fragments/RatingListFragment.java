package com.bukhmastov.cdoitmo.fragments;

import android.content.Context;
import android.os.Bundle;
import android.support.annotation.Nullable;
import android.support.v4.widget.SwipeRefreshLayout;
import android.view.InflateException;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.ListView;
import android.widget.TextView;

import com.bukhmastov.cdoitmo.R;
import com.bukhmastov.cdoitmo.adapters.RatingTopListView;
import com.bukhmastov.cdoitmo.firebase.FirebaseAnalyticsProvider;
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

public class RatingListFragment extends ConnectedFragment implements SwipeRefreshLayout.OnRefreshListener {

    private static final String TAG = "RatingListFragment";
    private String faculty = null;
    private String course = null;
    private String years = null;
    private boolean loaded = false;
    private RequestHandle fragmentRequestHandle = null;

    @Override
    public void onCreate(@Nullable Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        FirebaseAnalyticsProvider.logCurrentScreen(activity, this);
    }

    @Override
    public View onCreateView(LayoutInflater inflater, ViewGroup container, Bundle savedInstanceState) {
        return inflater.inflate(R.layout.fragment_rating_list, container, false);
    }

    @Override
    public void onViewCreated(View view, @Nullable Bundle savedInstanceState) {
        super.onViewCreated(view, savedInstanceState);
        activity.updateToolbar(activity.getString(R.string.top_rating), R.drawable.ic_rating);
        try {
            Bundle extras = getArguments();
            if (extras == null) {
                throw new NullPointerException("extras are null");
            }
            faculty = extras.getString("faculty");
            course = extras.getString("course");
            years = extras.getString("years");
            if (Objects.equals(years, "") || years == null) {
                Calendar now = Calendar.getInstance();
                int year = now.get(Calendar.YEAR);
                int month = now.get(Calendar.MONTH);
                years = month > Calendar.AUGUST ? year + "/" + (year + 1) : (year - 1) + "/" + year;
            }
            Log.v(TAG, "faculty=" + faculty + " | course=" + course + " | years=" + years);
            if (Objects.equals(faculty, "") || faculty == null || Objects.equals(course, "") || course == null) {
                throw new Exception("wrong extras provided | faculty=" + faculty + " | course=" + course);
            }
        } catch (Exception e) {
            Static.error(e);
            activity.back();
        }
    }

    @Override
    public void onResume() {
        super.onResume();
        Log.v(TAG, "Fragment resumed");
        FirebaseAnalyticsProvider.setCurrentScreen(activity, this);
        if (!loaded) {
            loaded = true;
            load();
        }
    }

    @Override
    public void onPause() {
        super.onPause();
        Log.v(TAG, "Fragment paused");
        if (fragmentRequestHandle != null) {
            loaded = false;
            fragmentRequestHandle.cancel(true);
        }
    }

    @Override
    public void onRefresh() {
        load();
    }

    private void load() {
        Static.T.runThread(new Runnable() {
            @Override
            public void run() {
                Log.v(TAG, "load");
                activity.updateToolbar(activity.getString(R.string.top_rating), R.drawable.ic_rating);
                if (!Static.OFFLINE_MODE) {
                    DeIfmoClient.get(activity, Client.Protocol.HTTP, "?node=rating&std&depId=" + faculty + "&year=" + course + "&app=" + years, null, new DeIfmoClientResponseHandler() {
                        @Override
                        public void onSuccess(int statusCode, String response) {
                            Log.v(TAG, "load | success | statusCode=" + statusCode);
                            if (statusCode == 200) {
                                new RatingTopListParse(new RatingTopListParse.response() {
                                    @Override
                                    public void finish(JSONObject json) {
                                        display(json);
                                    }
                                }).execute(response, Storage.file.perm.get(activity, "user#name"));
                            } else {
                                loadFailed();
                            }
                        }
                        @Override
                        public void onProgress(final int state) {
                            Static.T.runOnUiThread(new Runnable() {
                                @Override
                                public void run() {
                                    Log.v(TAG, "load | progress " + state);
                                    draw(R.layout.state_loading);
                                    TextView loading_message = activity.findViewById(R.id.loading_message);
                                    if (loading_message != null) {
                                        switch (state) {
                                            case IfmoClient.STATE_HANDLING:
                                                loading_message.setText(R.string.loading);
                                                break;
                                        }
                                    }
                                }
                            });
                        }
                        @Override
                        public void onFailure(final int statusCode, final int state) {
                            Static.T.runOnUiThread(new Runnable() {
                                @Override
                                public void run() {
                                    Log.v(TAG, "load | failure " + state);
                                    switch (state) {
                                        case IfmoClient.FAILED_OFFLINE:
                                            draw(R.layout.state_offline);
                                            View offline_reload = activity.findViewById(R.id.offline_reload);
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
                                            View try_again_reload = activity.findViewById(R.id.try_again_reload);
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
                            });
                        }
                        @Override
                        public void onNewHandle(RequestHandle requestHandle) {
                            fragmentRequestHandle = requestHandle;
                        }
                    });
                } else {
                    Static.T.runOnUiThread(new Runnable() {
                        @Override
                        public void run() {
                            try {
                                Static.snackBar(activity, activity.getString(R.string.offline_mode_on));
                                draw(R.layout.state_offline);
                                View offline_reload = activity.findViewById(R.id.offline_reload);
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
                        }
                    });
                }
            }
        });
    }
    private void loadFailed() {
        Static.T.runOnUiThread(new Runnable() {
            @Override
            public void run() {
                Log.v(TAG, "loadFailed");
                try {
                    draw(R.layout.state_try_again);
                    View try_again_reload = activity.findViewById(R.id.try_again_reload);
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
        });
    }

    private void display(final JSONObject data) {
        final RatingListFragment self = this;
        Static.T.runOnUiThread(new Runnable() {
            @Override
            public void run() {
                Log.v(TAG, "display");
                try {
                    if (data == null) throw new NullPointerException("display(JSONObject data) can't be null");
                    activity.updateToolbar(Static.capitalizeFirstLetter(data.getString("header")), R.drawable.ic_rating);
                    // получаем список для отображения рейтинга
                    final ArrayList<HashMap<String, String>> users = new ArrayList<>();
                    JSONArray list = data.getJSONArray("list");
                    if (list.length() > 0) {
                        for (int i = 0; i < list.length(); i++) {
                            JSONObject jsonObject = list.getJSONObject(i);
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
                        ListView rl_list_view = activity.findViewById(R.id.rl_list_view);
                        if (rl_list_view != null) rl_list_view.setAdapter(new RatingTopListView(activity, users));
                        // работаем со свайпом
                        SwipeRefreshLayout swipe_container = activity.findViewById(R.id.swipe_container);
                        if (swipe_container != null) {
                            swipe_container.setColorSchemeColors(Static.colorAccent);
                            swipe_container.setProgressBackgroundColorSchemeColor(Static.colorBackgroundRefresh);
                            swipe_container.setOnRefreshListener(self);
                        }
                    } else {
                        draw(R.layout.nothing_to_display);
                        ((TextView) activity.findViewById(R.id.ntd_text)).setText(R.string.no_rating);
                    }
                } catch (Exception e) {
                    Static.error(e);
                    loadFailed();
                }
            }
        });
    }

    private void draw(final int layoutId) {
        Static.T.runOnUiThread(new Runnable() {
            @Override
            public void run() {
                try {
                    ViewGroup vg = activity.findViewById(R.id.rating_list_container);
                    if (vg != null) {
                        vg.removeAllViews();
                        vg.addView(inflate(layoutId), 0, new ViewGroup.LayoutParams(ViewGroup.LayoutParams.MATCH_PARENT, ViewGroup.LayoutParams.MATCH_PARENT));
                    }
                } catch (Exception e) {
                    Static.error(e);
                }
            }
        });
    }
    private View inflate(int layoutId) throws InflateException {
        return ((LayoutInflater) activity.getSystemService(Context.LAYOUT_INFLATER_SERVICE)).inflate(layoutId, null);
    }
}

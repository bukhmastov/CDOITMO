package com.bukhmastov.cdoitmo.fragments;

import android.content.Context;
import android.content.Intent;
import android.net.Uri;
import android.os.Bundle;
import android.support.annotation.NonNull;
import android.support.annotation.Nullable;
import android.support.v4.widget.SwipeRefreshLayout;
import android.support.v7.widget.PopupMenu;
import android.view.InflateException;
import android.view.LayoutInflater;
import android.view.Menu;
import android.view.MenuItem;
import android.view.View;
import android.view.ViewGroup;
import android.widget.ListView;
import android.widget.TextView;

import com.bukhmastov.cdoitmo.R;
import com.bukhmastov.cdoitmo.adapters.RatingTopListView;
import com.bukhmastov.cdoitmo.exceptions.SilentException;
import com.bukhmastov.cdoitmo.firebase.FirebaseAnalyticsProvider;
import com.bukhmastov.cdoitmo.network.DeIfmoClient;
import com.bukhmastov.cdoitmo.network.interfaces.ResponseHandler;
import com.bukhmastov.cdoitmo.network.models.Client;
import com.bukhmastov.cdoitmo.parse.RatingTopListParse;
import com.bukhmastov.cdoitmo.utils.Log;
import com.bukhmastov.cdoitmo.utils.Static;
import com.bukhmastov.cdoitmo.utils.Storage;

import org.json.JSONArray;
import org.json.JSONObject;

import java.util.ArrayList;
import java.util.Calendar;
import java.util.HashMap;
import java.util.Objects;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

public class RatingListFragment extends ConnectedFragment implements SwipeRefreshLayout.OnRefreshListener {

    private static final String TAG = "RatingListFragment";
    private String faculty = null;
    private String course = null;
    private String years = null;
    private boolean loaded = false;
    private Client.Request requestHandle = null;
    private int minePosition = -1;
    private String mineFaculty = "";

    @Override
    public void onCreate(@Nullable Bundle savedInstanceState) {
        setHasOptionsMenu(true);
        super.onCreate(savedInstanceState);
        FirebaseAnalyticsProvider.logCurrentScreen(activity, this);
    }

    @Override
    public void onDestroy() {
        super.onDestroy();
        hideShareButton();
    }

    @Override
    public View onCreateView(@NonNull LayoutInflater inflater, ViewGroup container, Bundle savedInstanceState) {
        return inflater.inflate(R.layout.fragment_rating_list, container, false);
    }

    @Override
    public void onViewCreated(@NonNull View view, @Nullable Bundle savedInstanceState) {
        super.onViewCreated(view, savedInstanceState);
        activity.updateToolbar(activity, activity.getString(R.string.top_rating), R.drawable.ic_rating);
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
        if (requestHandle != null && requestHandle.cancel()) {
            loaded = false;
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
                activity.updateToolbar(activity, activity.getString(R.string.top_rating), R.drawable.ic_rating);
                minePosition = -1;
                mineFaculty = "";
                hideShareButton();
                if (!Static.OFFLINE_MODE) {
                    DeIfmoClient.get(activity, Client.Protocol.HTTP, "?node=rating&std&depId=" + faculty + "&year=" + course + "&app=" + years, null, new ResponseHandler() {
                        @Override
                        public void onSuccess(final int statusCode, final Client.Headers headers, final String response) {
                            Static.T.runThread(new Runnable() {
                                @Override
                                public void run() {
                                    Log.v(TAG, "load | success | statusCode=" + statusCode);
                                    if (statusCode == 200) {
                                        new RatingTopListParse(response, Storage.file.perm.get(activity, "user#name"), new RatingTopListParse.response() {
                                            @Override
                                            public void finish(JSONObject json) {
                                                display(json);
                                            }
                                        }).run();
                                    } else {
                                        loadFailed();
                                    }
                                }
                            });
                        }
                        @Override
                        public void onFailure(final int statusCode, final Client.Headers headers, final int state) {
                            Static.T.runOnUiThread(new Runnable() {
                                @Override
                                public void run() {
                                    Log.v(TAG, "load | failure " + state);
                                    switch (state) {
                                        case DeIfmoClient.FAILED_OFFLINE:
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
                                        case DeIfmoClient.FAILED_TRY_AGAIN:
                                        case DeIfmoClient.FAILED_SERVER_ERROR:
                                            draw(R.layout.state_try_again);
                                            if (state == DeIfmoClient.FAILED_SERVER_ERROR) {
                                                TextView try_again_message = activity.findViewById(R.id.try_again_message);
                                                if (try_again_message != null) {
                                                    try_again_message.setText(DeIfmoClient.getFailureMessage(activity, statusCode));
                                                }
                                            }
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
                        public void onProgress(final int state) {
                            Static.T.runOnUiThread(new Runnable() {
                                @Override
                                public void run() {
                                    Log.v(TAG, "load | progress " + state);
                                    draw(R.layout.state_loading);
                                    TextView loading_message = activity.findViewById(R.id.loading_message);
                                    if (loading_message != null) {
                                        switch (state) {
                                            case DeIfmoClient.STATE_HANDLING:
                                                loading_message.setText(R.string.loading);
                                                break;
                                        }
                                    }
                                }
                            });
                        }
                        @Override
                        public void onNewRequest(Client.Request request) {
                            requestHandle = request;
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
        Static.T.runThread(new Runnable() {
            @Override
            public void run() {
                Log.v(TAG, "display");
                try {
                    if (data == null) throw new SilentException();
                    final String title = Static.capitalizeFirstLetter(data.getString("header"));
                    activity.updateToolbar(activity, title, R.drawable.ic_rating);
                    minePosition = -1;
                    mineFaculty = "";
                    hideShareButton();
                    // получаем список для отображения рейтинга
                    final ArrayList<HashMap<String, String>> users = new ArrayList<>();
                    JSONArray list = data.getJSONArray("list");
                    if (list != null && list.length() > 0) {
                        for (int i = 0; i < list.length(); i++) {
                            JSONObject jsonObject = list.getJSONObject(i);
                            if (jsonObject == null) continue;
                            int position = jsonObject.getInt("number");
                            boolean is_me = jsonObject.getBoolean("is_me");
                            if (minePosition == -1 && is_me) {
                                minePosition = position;
                                Matcher m = Pattern.compile("^(.*)\\(.*\\)$").matcher(title);
                                if (m.find()) {
                                    mineFaculty = m.group(1).trim();
                                } else {
                                    mineFaculty = title;
                                }
                            }
                            HashMap<String, String> hashMap = new HashMap<>();
                            hashMap.put("number", String.valueOf(position));
                            hashMap.put("fio", jsonObject.getString("fio"));
                            hashMap.put("meta", jsonObject.getString("group") + " — " + jsonObject.getString("department"));
                            hashMap.put("is_me", is_me ? "1" : "0");
                            hashMap.put("change", jsonObject.getString("change"));
                            hashMap.put("delta", jsonObject.getString("delta"));
                            users.add(hashMap);
                        }
                    }
                    if (minePosition != -1) {
                        showShareButton();
                    }
                    Static.T.runOnUiThread(new Runnable() {
                        @Override
                        public void run() {
                            try {
                                if (users.size() > 0) {
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
                } catch (SilentException ignore) {
                    loadFailed();
                } catch (Exception e) {
                    Static.error(e);
                    loadFailed();
                }
            }
        });
    }
    private void showShareButton() {
        Static.T.runOnUiThread(new Runnable() {
            @Override
            public void run() {
                try {
                    if (activity.toolbar != null) {
                        final MenuItem action_share = activity.toolbar.findItem(R.id.action_share);
                        if (action_share != null && minePosition != -1) {
                            action_share.setVisible(true);
                            action_share.setOnMenuItemClickListener(new MenuItem.OnMenuItemClickListener() {
                                @Override
                                public boolean onMenuItemClick(MenuItem menuItem) {
                                    try {
                                        View view = activity.findViewById(R.id.action_share);
                                        String title = "Я на %position% позиции в рейтинге %faculty%!"
                                                .replace("%position%", String.valueOf(minePosition))
                                                .replace("%faculty%", mineFaculty.isEmpty() ? "факультета" : mineFaculty);
                                        String description = "Узнай свои успехи!";
                                        share(view, title, description);
                                    } catch (Exception e) {
                                        Static.error(e);
                                        Static.snackBar(activity, activity.getString(R.string.something_went_wrong));
                                    }
                                    return false;
                                }
                            });
                        }
                    }
                } catch (Exception e){
                    Static.error(e);
                }
            }
        });
    }
    private void hideShareButton() {
        Static.T.runOnUiThread(new Runnable() {
            @Override
            public void run() {
                try {
                    if (activity != null && activity.toolbar != null) {
                        MenuItem action_share = activity.toolbar.findItem(R.id.action_share);
                        if (action_share != null) action_share.setVisible(false);
                    }
                } catch (Exception e){
                    Static.error(e);
                }
            }
        });
    }
    private void share(final View anchor, final String title, final String description) throws Exception {
        final PopupMenu popup = new PopupMenu(activity, anchor);
        final Menu menu = popup.getMenu();
        popup.getMenuInflater().inflate(R.menu.social_share, menu);
        popup.setOnMenuItemClickListener(new PopupMenu.OnMenuItemClickListener() {
            @Override
            public boolean onMenuItemClick(MenuItem item) {
                try {
                    String url = "https://goo.gl/NpAhMF";
                    String social = "";
                    String link = "";
                    switch (item.getItemId()) {
                        case R.id.share_vk: social = "vk"; link = "https://vk.com/share.php?url=%url%&noparse=true&title=%title%&description=%description%"; break;
                        case R.id.share_tg: social = "tg"; link = "https://t.me/share/url?url=%url%&text=%title%"; break;
                        case R.id.share_tw: social = "tw"; link = "https://twitter.com/intent/tweet?url=%url%&text=%title%"; break;
                        case R.id.share_fb: social = "fb"; link = "https://www.facebook.com/sharer.php?u=%url%&t=%title%"; break;
                    }
                    if (!link.isEmpty()) {
                        link = link.replace("%url%", url).replace("%title%", title).replace("%description%", description);
                        startActivity(new Intent(Intent.ACTION_VIEW, Uri.parse(link)));
                        // track statistics
                        Bundle bundle;
                        bundle = FirebaseAnalyticsProvider.getBundle(FirebaseAnalyticsProvider.Param.TITLE, title.substring(0, title.length() > 100 ? 100 : title.length()));
                        bundle = FirebaseAnalyticsProvider.getBundle(FirebaseAnalyticsProvider.Param.SOCIAL, social, bundle);
                        FirebaseAnalyticsProvider.logEvent(
                                activity,
                                FirebaseAnalyticsProvider.Event.RATING_SHARE,
                                bundle
                        );
                    }
                } catch (Exception e) {
                    Static.snackBar(activity, activity.getString(R.string.something_went_wrong));
                }
                return false;
            }
        });
        popup.show();
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

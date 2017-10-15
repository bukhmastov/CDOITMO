package com.bukhmastov.cdoitmo.activities;

import android.app.Activity;
import android.content.Context;
import android.content.Intent;
import android.net.Uri;
import android.os.Bundle;
import android.support.annotation.DrawableRes;
import android.support.annotation.LayoutRes;
import android.support.v4.widget.SwipeRefreshLayout;
import android.view.InflateException;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.ImageView;
import android.widget.TextView;

import com.bukhmastov.cdoitmo.R;
import com.bukhmastov.cdoitmo.firebase.FirebaseAnalyticsProvider;
import com.bukhmastov.cdoitmo.network.IfmoRestClient;
import com.bukhmastov.cdoitmo.network.interfaces.RestResponseHandler;
import com.bukhmastov.cdoitmo.network.models.Client;
import com.bukhmastov.cdoitmo.utils.CircularTransformation;
import com.bukhmastov.cdoitmo.utils.Log;
import com.bukhmastov.cdoitmo.utils.Static;
import com.squareup.picasso.Picasso;

import org.json.JSONArray;
import org.json.JSONObject;

public class UniversityPersonCardActivity extends ConnectedActivity implements SwipeRefreshLayout.OnRefreshListener {

    private static final String TAG = "UniversityPersonCard";
    private final Activity activity = this;
    private Client.Request requestHandle = null;
    private boolean loaded = false;
    private boolean first_load = true;
    private JSONObject person = null;
    private int pid = -1;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        switch (Static.getAppTheme(activity)) {
            case "light":
            default: setTheme(R.style.AppTheme_TransparentStatusBar); break;
            case "dark": setTheme(R.style.AppTheme_Dark_TransparentStatusBar); break;
            case "white": setTheme(R.style.AppTheme_White_TransparentStatusBar); break;
            case "black": setTheme(R.style.AppTheme_Black_TransparentStatusBar); break;
        }
        super.onCreate(savedInstanceState);
        Log.i(TAG, "Activity created");
        try {
            Intent intent = getIntent();
            if (intent == null) throw new NullPointerException("intent is null");
            Bundle extras = intent.getExtras();
            if (extras == null) throw new NullPointerException("extras are null");
            boolean ok = false;
            if (extras.containsKey("person")) {
                try {
                    person = new JSONObject(extras.getString("person"));
                    pid = person.getInt("persons_id");
                    ok = true;
                } catch (Exception e) {
                    ok = false;
                }
            }
            if (!ok && extras.containsKey("pid")) {
                try {
                    person = null;
                    pid = extras.getInt("pid");
                    ok = true;
                } catch (Exception e) {
                    ok = false;
                }
            }
            if (!ok) throw new Exception("failed to get info from extras");
            if (pid < 0) throw new Exception("Invalid person id provided");
        } catch (Exception e) {
            finish();
        }

        FirebaseAnalyticsProvider.logCurrentScreen(this);
        setContentView(R.layout.activity_university_person_card);

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
        FirebaseAnalyticsProvider.setCurrentScreen(this);
        if (!loaded) {
            loaded = true;
            load();
        }
    }

    @Override
    public void onPause() {
        super.onPause();
        Log.v(TAG, "paused");
        if (requestHandle != null && requestHandle.cancel()) {
            loaded = false;
        }
    }

    @Override
    public void onRefresh() {
        Log.v(TAG, "refreshing");
        person = null;
        load();
    }

    private void load() {
        Static.T.runThread(new Runnable() {
            @Override
            public void run() {
                if (person != null) {
                    display();
                    return;
                }
                loadProvider(new RestResponseHandler() {
                    @Override
                    public void onSuccess(final int statusCode, final Client.Headers headers, final JSONObject json, final JSONArray responseArr) {
                        Static.T.runOnUiThread(new Runnable() {
                            @Override
                            public void run() {
                                SwipeRefreshLayout mSwipeRefreshLayout = findViewById(R.id.person_swipe);
                                if (mSwipeRefreshLayout != null) {
                                    mSwipeRefreshLayout.setRefreshing(false);
                                }
                            }
                        });
                        Static.T.runThread(new Runnable() {
                            @Override
                            public void run() {
                                if (statusCode == 200 && json != null) {
                                    try {
                                        String post = json.getString("post");
                                        if (android.os.Build.VERSION.SDK_INT >= android.os.Build.VERSION_CODES.N) {
                                            json.put("post", android.text.Html.fromHtml(post, android.text.Html.FROM_HTML_MODE_LEGACY));
                                        } else {
                                            //noinspection deprecation
                                            json.put("post", android.text.Html.fromHtml(post));
                                        }
                                    } catch (Exception ignore) {
                                        // ignore
                                    }
                                    person = json;
                                    display();
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
                                Log.v(TAG, "load | statusCode = " + statusCode + " | failure " + state);
                                SwipeRefreshLayout mSwipeRefreshLayout = findViewById(R.id.person_swipe);
                                if (mSwipeRefreshLayout != null) {
                                    mSwipeRefreshLayout.setRefreshing(false);
                                }
                                if (statusCode == 404) {
                                    loadNotFound();
                                } else {
                                    switch (state) {
                                        case IfmoRestClient.FAILED_OFFLINE:
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
                                        case IfmoRestClient.FAILED_TRY_AGAIN:
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
                            }
                        });
                    }
                    @Override
                    public void onProgress(final int state) {
                        Static.T.runOnUiThread(new Runnable() {
                            @Override
                            public void run() {
                                Log.v(TAG, "load | progress " + state);
                                if (first_load) {
                                    draw(R.layout.state_loading);
                                    TextView loading_message = findViewById(R.id.loading_message);
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
                    public void onNewRequest(Client.Request request) {
                        requestHandle = request;
                    }
                });
            }
        });
    }
    private void loadProvider(RestResponseHandler handler) {
        Log.v(TAG, "loadProvider");
        IfmoRestClient.get(activity, "person/" + pid, null, handler);
    }
    private void loadFailed() {
        Static.T.runOnUiThread(new Runnable() {
            @Override
            public void run() {
                Log.v(TAG, "loadFailed");
                try {
                    draw(R.layout.state_try_again);
                    TextView try_again_message = findViewById(R.id.try_again_message);
                    if (try_again_message != null) try_again_message.setText(R.string.load_failed);
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
        });
    }
    private void loadNotFound() {
        Static.T.runOnUiThread(new Runnable() {
            @Override
            public void run() {
                Log.v(TAG, "loadNotFound");
                try {
                    draw(R.layout.nothing_to_display_center_person);
                    findViewById(R.id.web).setOnClickListener(new View.OnClickListener() {
                        @Override
                        public void onClick(View view) {
                            startActivity(new Intent(Intent.ACTION_VIEW, Uri.parse("http://www.ifmo.ru/ru/viewperson/" + pid + "/")));
                        }
                    });
                } catch (Exception e) {
                    Static.error(e);
                }
            }
        });
    }

    private void display() {
        final UniversityPersonCardActivity self = this;
        Static.T.runOnUiThread(new Runnable() {
            @Override
            public void run() {
                try {
                    first_load = false;
                    draw(R.layout.layout_university_person_card);
                    findViewById(R.id.person_header).setPadding(0, getStatusBarHeight(), 0, 0);
                    // кнопка назад
                    findViewById(R.id.back).setOnClickListener(new View.OnClickListener() {
                        @Override
                        public void onClick(View v) {
                            finish();
                        }
                    });
                    // кнопка сайта
                    final String persons_id = person.getString("persons_id");
                    if (persons_id != null && !persons_id.trim().isEmpty()) {
                        findViewById(R.id.web).setOnClickListener(new View.OnClickListener() {
                            @Override
                            public void onClick(View view) {
                                startActivity(new Intent(Intent.ACTION_VIEW, Uri.parse("http://www.ifmo.ru/ru/viewperson/" + persons_id.trim() + "/")));
                            }
                        });
                    } else {
                        Static.removeView(findViewById(R.id.web));
                    }
                    // заголовок
                    final String name = (person.getString("title_l") + " " + person.getString("title_f") + " " + person.getString("title_m")).trim();
                    final String degree = person.getString("degree").trim();
                    final String image = person.getString("image");
                    ((TextView) findViewById(R.id.name)).setText(name);
                    if (!degree.isEmpty()) {
                        ((TextView) findViewById(R.id.degree)).setText(Static.capitalizeFirstLetter(degree));
                    } else {
                        Static.removeView(findViewById(R.id.degree));
                    }
                    Picasso.with(self)
                            .load(image)
                            .error(R.drawable.ic_sentiment_very_satisfied_white)
                            .transform(new CircularTransformation())
                            .into((ImageView) findViewById(R.id.avatar));
                    // контент
                    ViewGroup info_connect_container = findViewById(R.id.info_connect_container);
                    if (info_connect_container != null) {
                        boolean exists = false;
                        final String[] phones = person.getString("phone").trim().split(";|,");
                        final String[] emails = person.getString("email").trim().split(";|,");
                        final String[] webs = person.getString("www").trim().split(";|,");
                        for (final String phone : phones) {
                            if (!phone.isEmpty()) {
                                info_connect_container.addView(getConnectContainer(R.drawable.ic_phone, phone, exists, new View.OnClickListener() {
                                    @Override
                                    public void onClick(View v) {
                                        Intent intent = new Intent(Intent.ACTION_DIAL, Uri.parse("tel:" + phone.trim()));
                                        intent.addFlags(Intent.FLAG_ACTIVITY_NEW_TASK);
                                        startActivity(intent);
                                    }
                                }));
                                exists = true;
                            }
                        }
                        for (final String email : emails) {
                            if (!email.isEmpty()) {
                                info_connect_container.addView(getConnectContainer(R.drawable.ic_email, email, exists, new View.OnClickListener() {
                                    @Override
                                    public void onClick(View v) {
                                        Intent emailIntent = new Intent(Intent.ACTION_SEND);
                                        emailIntent.setType("message/rfc822");
                                        emailIntent.putExtra(Intent.EXTRA_EMAIL, new String[]{email.trim()});
                                        startActivity(Intent.createChooser(emailIntent, activity.getString(R.string.send_mail) + "..."));
                                    }
                                }));
                                exists = true;
                            }
                        }
                        for (final String web : webs) {
                            if (!web.isEmpty()) {
                                info_connect_container.addView(getConnectContainer(R.drawable.ic_web, web, exists, new View.OnClickListener() {
                                    @Override
                                    public void onClick(View v) {
                                        startActivity(new Intent(Intent.ACTION_VIEW, Uri.parse(web.trim())));
                                    }
                                }));
                                exists = true;
                            }
                        }
                    }
                    ViewGroup info_about_container = findViewById(R.id.info_about_container);
                    if (info_about_container != null) {
                        final String rank = person.getString("rank").trim();
                        final String post = person.getString("post").trim();
                        final String bio = person.getString("text").trim();
                        if (!rank.isEmpty()) {
                            info_about_container.addView(getAboutContainer(activity.getString(R.string.person_rank), Static.capitalizeFirstLetter(rank)));
                        }
                        if (!post.isEmpty()) {
                            info_about_container.addView(getAboutContainer(activity.getString(R.string.person_post), Static.capitalizeFirstLetter(post)));
                        }
                        if (!bio.isEmpty()) {
                            info_about_container.addView(getAboutContainer(activity.getString(R.string.person_bio), bio));
                        }
                    }
                    // работаем со свайпом
                    SwipeRefreshLayout mSwipeRefreshLayout = findViewById(R.id.person_swipe);
                    if (mSwipeRefreshLayout != null) {
                        mSwipeRefreshLayout.setColorSchemeColors(Static.colorAccent);
                        mSwipeRefreshLayout.setProgressBackgroundColorSchemeColor(Static.colorBackgroundRefresh);
                        mSwipeRefreshLayout.setOnRefreshListener(self);
                    }
                } catch (Exception e) {
                    Static.error(e);
                }
            }
        });
    }
    private View getConnectContainer(@DrawableRes int icon, String text, boolean first_block, View.OnClickListener listener) {
        View activity_university_person_card_connect = inflate(R.layout.layout_university_connect);
        ((ImageView) activity_university_person_card_connect.findViewById(R.id.connect_image)).setImageResource(icon);
        ((TextView) activity_university_person_card_connect.findViewById(R.id.connect_text)).setText(text.trim());
        if (listener != null) {
            activity_university_person_card_connect.setOnClickListener(listener);
        }
        if (!first_block) {
            Static.removeView(activity_university_person_card_connect.findViewById(R.id.separator));
        }
        return activity_university_person_card_connect;
    }
    private View getAboutContainer(String title, String text) {
        View activity_university_person_card_about = inflate(R.layout.layout_university_person_card_about);
        ((TextView) activity_university_person_card_about.findViewById(R.id.title)).setText(title);
        TextView textView = activity_university_person_card_about.findViewById(R.id.text);
        if (android.os.Build.VERSION.SDK_INT >= android.os.Build.VERSION_CODES.N) {
            textView.setText(android.text.Html.fromHtml(text, android.text.Html.FROM_HTML_MODE_LEGACY).toString().trim());
        } else {
            //noinspection deprecation
            textView.setText(android.text.Html.fromHtml(text).toString().trim());
        }
        return activity_university_person_card_about;
    }

    private void draw(@LayoutRes final int layoutId) {
        Static.T.runOnUiThread(new Runnable() {
            @Override
            public void run() {
                try {
                    ViewGroup vg = findViewById(R.id.person_common_container);
                    if (vg != null) {
                        vg.removeAllViews();
                        vg.addView(inflate(layoutId));
                    }
                } catch (Exception e){
                    Static.error(e);
                }
            }
        });
    }
    private View inflate(@LayoutRes int layoutId) throws InflateException {
        return ((LayoutInflater) getSystemService(Context.LAYOUT_INFLATER_SERVICE)).inflate(layoutId, null);
    }

    public int getStatusBarHeight() {
        int result = 0;
        int resourceId = getResources().getIdentifier("status_bar_height", "dimen", "android");
        if (resourceId > 0) {
            result = getResources().getDimensionPixelSize(resourceId);
        }
        return result;
    }

    @Override
    protected int getRootViewId() {
        return R.id.person_common_container;
    }
}

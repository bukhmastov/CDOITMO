package com.bukhmastov.cdoitmo.activity.presenter.impl;

import android.content.Intent;
import android.net.Uri;
import android.os.Bundle;
import androidx.annotation.DrawableRes;
import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.swiperefreshlayout.widget.SwipeRefreshLayout;
import android.view.View;
import android.view.ViewGroup;
import android.widget.ImageView;
import android.widget.TextView;

import com.bukhmastov.cdoitmo.R;
import com.bukhmastov.cdoitmo.activity.UniversityPersonCardActivity;
import com.bukhmastov.cdoitmo.activity.presenter.UniversityPersonCardActivityPresenter;
import com.bukhmastov.cdoitmo.event.bus.EventBus;
import com.bukhmastov.cdoitmo.event.events.OpenIntentEvent;
import com.bukhmastov.cdoitmo.factory.AppComponentProvider;
import com.bukhmastov.cdoitmo.firebase.FirebaseAnalyticsProvider;
import com.bukhmastov.cdoitmo.network.IfmoRestClient;
import com.bukhmastov.cdoitmo.network.handlers.RestResponseHandler;
import com.bukhmastov.cdoitmo.network.model.Client;
import com.bukhmastov.cdoitmo.util.Log;
import com.bukhmastov.cdoitmo.util.Static;
import com.bukhmastov.cdoitmo.util.TextUtils;
import com.bukhmastov.cdoitmo.util.Thread;
import com.bukhmastov.cdoitmo.util.singleton.Color;
import com.bukhmastov.cdoitmo.view.CircularTransformation;
import com.squareup.picasso.Picasso;

import org.json.JSONArray;
import org.json.JSONObject;

import javax.inject.Inject;

public class UniversityPersonCardActivityPresenterImpl implements UniversityPersonCardActivityPresenter, SwipeRefreshLayout.OnRefreshListener {

    private static final String TAG = "UniversityPersonCard";
    private UniversityPersonCardActivity activity = null;
    private Client.Request requestHandle = null;
    private boolean loaded = false;
    private boolean first_load = true;
    private JSONObject person = null;
    private int pid = -1;

    @Inject
    Log log;
    @Inject
    Thread thread;
    @Inject
    EventBus eventBus;
    @Inject
    IfmoRestClient ifmoRestClient;
    @Inject
    Static staticUtil;
    @Inject
    TextUtils textUtils;
    @Inject
    FirebaseAnalyticsProvider firebaseAnalyticsProvider;

    public UniversityPersonCardActivityPresenterImpl() {
        AppComponentProvider.getComponent().inject(this);
    }

    @Override
    public void setActivity(@NonNull UniversityPersonCardActivity activity) {
        this.activity = activity;
    }

    @Override
    public void onCreate(@Nullable Bundle savedInstanceState) {
        log.i(TAG, "Activity created");
        try {
            Intent intent = activity.getIntent();
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
            activity.finish();
        }
        firebaseAnalyticsProvider.logCurrentScreen(activity);
    }

    @Override
    public void onResume() {
        log.v(TAG, "resumed");
        firebaseAnalyticsProvider.setCurrentScreen(activity);
        if (!loaded) {
            loaded = true;
            load();
        }
    }

    @Override
    public void onPause() {
        log.v(TAG, "paused");
        if (requestHandle != null && requestHandle.cancel()) {
            loaded = false;
        }
    }

    @Override
    public void onDestroy() {
        log.v(TAG, "Fragment destroyed");
        loaded = false;
    }

    @Override
    public void onRefresh() {
        log.v(TAG, "refreshing");
        person = null;
        load();
    }

    private void load() {
        thread.run(() -> {
            if (person != null) {
                display();
                return;
            }
            loadProvider(new RestResponseHandler() {
                @Override
                public void onSuccess(final int statusCode, final Client.Headers headers, final JSONObject json, final JSONArray responseArr) {
                    thread.runOnUI(() -> {
                        SwipeRefreshLayout mSwipeRefreshLayout = activity.findViewById(R.id.person_swipe);
                        if (mSwipeRefreshLayout != null) {
                            mSwipeRefreshLayout.setRefreshing(false);
                        }
                    });
                    thread.run(() -> {
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
                    });
                }
                @Override
                public void onFailure(final int statusCode, final Client.Headers headers, final int state) {
                    thread.runOnUI(() -> {
                        log.v(TAG, "load | statusCode = " + statusCode + " | failure " + state);
                        SwipeRefreshLayout mSwipeRefreshLayout = activity.findViewById(R.id.person_swipe);
                        if (mSwipeRefreshLayout != null) {
                            mSwipeRefreshLayout.setRefreshing(false);
                        }
                        if (statusCode == 404) {
                            loadNotFound();
                        } else {
                            switch (state) {
                                case IfmoRestClient.FAILED_OFFLINE:
                                    activity.draw(R.layout.state_offline_text);
                                    View offline_reload = activity.findViewById(R.id.offline_reload);
                                    if (offline_reload != null) {
                                        offline_reload.setOnClickListener(v -> load());
                                    }
                                    break;
                                case IfmoRestClient.FAILED_CORRUPTED_JSON:
                                case IfmoRestClient.FAILED_SERVER_ERROR:
                                case IfmoRestClient.FAILED_TRY_AGAIN:
                                    activity.draw(R.layout.state_failed_button);
                                    TextView try_again_message = activity.findViewById(R.id.try_again_message);
                                    if (try_again_message != null) {
                                        switch (state) {
                                            case IfmoRestClient.FAILED_SERVER_ERROR:   try_again_message.setText(IfmoRestClient.getFailureMessage(activity, statusCode)); break;
                                            case IfmoRestClient.FAILED_CORRUPTED_JSON: try_again_message.setText(R.string.server_provided_corrupted_json); break;
                                        }
                                    }
                                    View try_again_reload = activity.findViewById(R.id.try_again_reload);
                                    if (try_again_reload != null) {
                                        try_again_reload.setOnClickListener(v -> load());
                                    }
                                    break;
                            }
                        }
                    });
                }
                @Override
                public void onProgress(final int state) {
                    thread.runOnUI(() -> {
                        log.v(TAG, "load | progress " + state);
                        if (first_load) {
                            activity.draw(R.layout.state_loading_text);
                            TextView loading_message = activity.findViewById(R.id.loading_message);
                            if (loading_message != null) {
                                switch (state) {
                                    case IfmoRestClient.STATE_HANDLING:
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
        });
    }

    private void loadProvider(RestResponseHandler handler) {
        log.v(TAG, "loadProvider");
        ifmoRestClient.get(activity, "person/" + pid, null, handler);
    }

    private void loadFailed() {
        thread.runOnUI(() -> {
            log.v(TAG, "loadFailed");
            try {
                activity.draw(R.layout.state_failed_button);
                TextView try_again_message = activity.findViewById(R.id.try_again_message);
                if (try_again_message != null) try_again_message.setText(R.string.load_failed);
                View try_again_reload = activity.findViewById(R.id.try_again_reload);
                if (try_again_reload != null) {
                    try_again_reload.setOnClickListener(v -> load());
                }
            } catch (Exception e) {
                log.exception(e);
            }
        });
    }

    private void loadNotFound() {
        thread.runOnUI(() -> {
            log.v(TAG, "loadNotFound");
            try {
                activity.draw(R.layout.state_nothing_to_display_person);
                activity.findViewById(R.id.web).setOnClickListener(view -> {
                    eventBus.fire(new OpenIntentEvent(new Intent(Intent.ACTION_VIEW, Uri.parse("http://www.ifmo.ru/ru/viewperson/" + pid + "/"))));
                });
            } catch (Exception e) {
                log.exception(e);
            }
        });
    }

    private void display() {
        thread.runOnUI(() -> {
            try {
                first_load = false;
                activity.draw(R.layout.layout_university_person_card);
                activity.findViewById(R.id.person_header).setPadding(0, getStatusBarHeight(), 0, 0);
                // кнопка назад
                activity.findViewById(R.id.back).setOnClickListener(v -> activity.finish());
                // кнопка сайта
                final String persons_id = person.getString("persons_id");
                if (persons_id != null && !persons_id.trim().isEmpty()) {
                    activity.findViewById(R.id.web).setOnClickListener(view -> {
                        eventBus.fire(new OpenIntentEvent(new Intent(Intent.ACTION_VIEW, Uri.parse("http://www.ifmo.ru/ru/viewperson/" + persons_id.trim() + "/"))));
                    });
                } else {
                    staticUtil.removeView(activity.findViewById(R.id.web));
                }
                // заголовок
                final String name = (person.getString("title_l") + " " + person.getString("title_f") + " " + person.getString("title_m")).trim();
                final String degree = person.getString("degree").trim();
                final String image = person.getString("image");
                ((TextView) activity.findViewById(R.id.name)).setText(name);
                if (!degree.isEmpty()) {
                    ((TextView) activity.findViewById(R.id.degree)).setText(textUtils.capitalizeFirstLetter(degree));
                } else {
                    staticUtil.removeView(activity.findViewById(R.id.degree));
                }
                Picasso.with(activity)
                        .load(image)
                        .error(R.drawable.ic_sentiment_very_satisfied_white)
                        .transform(new CircularTransformation())
                        .into((ImageView) activity.findViewById(R.id.avatar));
                // контент
                ViewGroup info_connect_container = activity.findViewById(R.id.info_connect_container);
                if (info_connect_container != null) {
                    boolean exists = false;
                    final String[] phones = person.getString("phone").trim().split("[;,]");
                    final String[] emails = person.getString("email").trim().split("[;,]");
                    final String[] webs = person.getString("www").trim().split("[;,]");
                    for (final String phone : phones) {
                        if (!phone.isEmpty()) {
                            info_connect_container.addView(getConnectContainer(R.drawable.ic_phone, phone, exists, v -> {
                                Intent intent = new Intent(Intent.ACTION_DIAL, Uri.parse("tel:" + phone.trim()));
                                intent.addFlags(Intent.FLAG_ACTIVITY_NEW_TASK);
                                eventBus.fire(new OpenIntentEvent(intent));
                            }));
                            exists = true;
                        }
                    }
                    for (final String email : emails) {
                        if (!email.isEmpty()) {
                            info_connect_container.addView(getConnectContainer(R.drawable.ic_email, email, exists, v -> {
                                Intent emailIntent = new Intent(Intent.ACTION_SEND);
                                emailIntent.setType("message/rfc822");
                                emailIntent.putExtra(Intent.EXTRA_EMAIL, new String[]{email.trim()});
                                eventBus.fire(new OpenIntentEvent(Intent.createChooser(emailIntent, activity.getString(R.string.send_mail) + "...")));
                            }));
                            exists = true;
                        }
                    }
                    for (final String web : webs) {
                        if (!web.isEmpty()) {
                            info_connect_container.addView(getConnectContainer(R.drawable.ic_web, web, exists, v -> {
                                eventBus.fire(new OpenIntentEvent(new Intent(Intent.ACTION_VIEW, Uri.parse(web.trim()))));
                            }));
                            exists = true;
                        }
                    }
                }
                ViewGroup info_about_container = activity.findViewById(R.id.info_about_container);
                if (info_about_container != null) {
                    final String rank = person.getString("rank").trim();
                    final String post = person.getString("post").trim();
                    final String bio = person.getString("text").trim();
                    if (!rank.isEmpty()) {
                        info_about_container.addView(getAboutContainer(activity.getString(R.string.person_rank), textUtils.capitalizeFirstLetter(rank)));
                    }
                    if (!post.isEmpty()) {
                        info_about_container.addView(getAboutContainer(activity.getString(R.string.person_post), textUtils.capitalizeFirstLetter(post)));
                    }
                    if (!bio.isEmpty()) {
                        info_about_container.addView(getAboutContainer(activity.getString(R.string.person_bio), bio));
                    }
                }
                // работаем со свайпом
                SwipeRefreshLayout mSwipeRefreshLayout = activity.findViewById(R.id.person_swipe);
                if (mSwipeRefreshLayout != null) {
                    mSwipeRefreshLayout.setColorSchemeColors(Color.resolve(activity, R.attr.colorAccent));
                    mSwipeRefreshLayout.setProgressBackgroundColorSchemeColor(Color.resolve(activity, R.attr.colorBackgroundRefresh));
                    mSwipeRefreshLayout.setOnRefreshListener(this);
                }
            } catch (Exception e) {
                log.exception(e);
            }
        });
    }

    private View getConnectContainer(@DrawableRes int icon, String text, boolean first_block, View.OnClickListener listener) {
        View activity_university_person_card_connect = activity.inflate(R.layout.layout_university_connect);
        ((ImageView) activity_university_person_card_connect.findViewById(R.id.connect_image)).setImageResource(icon);
        ((TextView) activity_university_person_card_connect.findViewById(R.id.connect_text)).setText(text.trim());
        if (listener != null) {
            activity_university_person_card_connect.setOnClickListener(listener);
        }
        if (!first_block) {
            staticUtil.removeView(activity_university_person_card_connect.findViewById(R.id.separator));
        }
        return activity_university_person_card_connect;
    }

    private View getAboutContainer(String title, String text) {
        View activity_university_person_card_about = activity.inflate(R.layout.layout_university_person_card_about);
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

    private int getStatusBarHeight() {
        int result = 0;
        int resourceId = activity.getResources().getIdentifier("status_bar_height", "dimen", "android");
        if (resourceId > 0) {
            result = activity.getResources().getDimensionPixelSize(resourceId);
        }
        return result;
    }
}

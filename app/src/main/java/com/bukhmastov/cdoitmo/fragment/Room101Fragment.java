package com.bukhmastov.cdoitmo.fragment;

import android.app.Activity;
import android.content.Context;
import android.content.Intent;
import android.os.Bundle;
import android.support.annotation.Nullable;
import android.support.design.widget.FloatingActionButton;
import android.support.v4.widget.SwipeRefreshLayout;
import android.support.v7.app.AlertDialog;
import android.view.View;
import android.view.ViewGroup;
import android.widget.LinearLayout;
import android.widget.ProgressBar;
import android.widget.TextView;

import com.bukhmastov.cdoitmo.App;
import com.bukhmastov.cdoitmo.R;
import com.bukhmastov.cdoitmo.builder.Room101ReviewBuilder;
import com.bukhmastov.cdoitmo.factory.AppComponentProvider;
import com.bukhmastov.cdoitmo.firebase.FirebaseAnalyticsProvider;
import com.bukhmastov.cdoitmo.network.Room101Client;
import com.bukhmastov.cdoitmo.network.handlers.ResponseHandler;
import com.bukhmastov.cdoitmo.network.model.Client;
import com.bukhmastov.cdoitmo.network.model.Room101;
import com.bukhmastov.cdoitmo.object.Room101AddRequest;
import com.bukhmastov.cdoitmo.parse.room101.Room101ViewRequestParse;
import com.bukhmastov.cdoitmo.provider.InjectProvider;
import com.bukhmastov.cdoitmo.util.NotificationMessage;
import com.bukhmastov.cdoitmo.util.singleton.Color;
import com.bukhmastov.cdoitmo.util.Log;
import com.bukhmastov.cdoitmo.util.Static;
import com.bukhmastov.cdoitmo.util.Storage;
import com.bukhmastov.cdoitmo.util.StoragePref;
import com.bukhmastov.cdoitmo.util.TextUtils;
import com.bukhmastov.cdoitmo.util.Thread;
import com.bukhmastov.cdoitmo.util.Time;

import org.json.JSONException;
import org.json.JSONObject;

import java.util.HashMap;

import javax.inject.Inject;

public class Room101Fragment extends ConnectedFragment implements SwipeRefreshLayout.OnRefreshListener, Room101ReviewBuilder.register {

    private static final String TAG = "Room101Fragment";
    private JSONObject data = null;
    private boolean loaded = false;
    public static Client.Request requestHandle = null;
    private String action_extra = null;
    protected boolean forbidden = false;

    @Inject
    Log log;
    @Inject
    Thread thread;
    @Inject
    Storage storage;
    @Inject
    StoragePref storagePref;
    @Inject
    InjectProvider injectProvider;
    @Inject
    Room101Client room101Client;
    @Inject
    Room101AddRequest room101AddRequest;
    @Inject
    NotificationMessage notificationMessage;
    @Inject
    Static staticUtil;
    @Inject
    Time time;
    @Inject
    TextUtils textUtils;
    @Inject
    FirebaseAnalyticsProvider firebaseAnalyticsProvider;

    @Override
    public void onCreate(@Nullable Bundle savedInstanceState) {
        AppComponentProvider.getComponent().inject(this);
        super.onCreate(savedInstanceState);
        log.v(TAG, "Fragment created");
        if (App.UNAUTHORIZED_MODE) {
            forbidden = true;
            log.w(TAG, "Fragment created | UNAUTHORIZED_MODE not allowed, closing fragment...");
            close();
            return;
        }
        firebaseAnalyticsProvider.logCurrentScreen(activity, this);
        Activity activity = getActivity();
        if (activity != null) {
            Intent intent = activity.getIntent();
            if (intent != null) {
                action_extra = intent.getStringExtra("action_extra");
                if (action_extra != null) {
                    intent.removeExtra("action_extra");
                }
            }
        }
    }

    @Override
    public void onDestroy() {
        super.onDestroy();
        log.v(TAG, "Fragment destroyed");
    }

    @Override
    public void onResume() {
        super.onResume();
        log.v(TAG, "resumed");
        if (forbidden) {
            return;
        }
        firebaseAnalyticsProvider.setCurrentScreen(activity, this);
        if (!loaded) {
            loaded = true;
            if (getData() == null) {
                load();
            } else {
                display();
            }
        }
    }

    @Override
    public void onPause() {
        super.onPause();
        log.v(TAG, "paused");
        if (requestHandle != null && requestHandle.cancel()) {
            loaded = false;
        }
    }

    @Override
    public void onRefresh() {
        log.v(TAG, "refreshing");
        load(true);
    }

    @Override
    protected int getLayoutId() {
        return R.layout.fragment_container;
    }

    @Override
    protected int getRootId() {
        return R.id.container;
    }

    @Override
    public void onDenyRequest(final int reid, final int status) {
        thread.runOnUI(() -> {
            log.v(TAG, "onDenyRequest | reid=" + reid + " | status=" + status);
            if (App.OFFLINE_MODE) {
                log.v(TAG, "onDenyRequest rejected: offline mode");
                notificationMessage.snackBar(activity, R.id.room101_review_swipe, activity.getString(R.string.device_offline_action_refused));
            } else {
                (new AlertDialog.Builder(activity)
                        .setTitle(R.string.request_deny)
                        .setMessage(activity.getString(R.string.request_deny_1) + "\n" + activity.getString(R.string.request_deny_2))
                        .setCancelable(true)
                        .setPositiveButton(R.string.delete, (dialog, which) -> {
                            denyRequest(reid, status);
                            dialog.cancel();
                        })
                        .setNegativeButton(R.string.cancel, (dialog, which) -> dialog.cancel())
                        .create()
                ).show();
            }
        });
    }
    private void denyRequest(final int reid, final int status) {
        thread.run(() -> {
            log.v(TAG, "denyRequest | reid=" + reid + " | status=" + status);
            staticUtil.lockOrientation(activity, true);
            HashMap<String, String> params = new HashMap<>();
            switch (status) {
                case 1: params.put("getFunc", "snatRequest"); break;
                default: params.put("getFunc", "delRequest"); break;
            }
            params.put("reid", String.valueOf(reid));
            params.put("login", storage.get(activity, Storage.PERMANENT, Storage.USER, "user#deifmo#login"));
            params.put("password", storage.get(activity, Storage.PERMANENT, Storage.USER, "user#deifmo#password"));
            room101Client.post(activity, "delRequest.php", params, new ResponseHandler() {
                @Override
                public void onSuccess(final int statusCode, final Client.Headers headers, final String response) {
                    thread.runOnUI(() -> {
                        if (statusCode == 302) {
                            log.v(TAG, "denyRequest | reid=" + reid + " | status=" + status + " | success | statusCode=" + statusCode);
                            load(true);
                            firebaseAnalyticsProvider.logEvent(
                                    activity,
                                    FirebaseAnalyticsProvider.Event.ROOM101_REQUEST_DENIED
                            );
                        } else {
                            log.v(TAG, "denyRequest | reid=" + reid + " | status=" + status + " | success(not really) | statusCode=" + statusCode);
                            draw(R.layout.state_failed_button);
                            TextView try_again_message = container.findViewById(R.id.try_again_message);
                            if (try_again_message != null) try_again_message.setText(R.string.wrong_response_from_server);
                            View try_again_reload = container.findViewById(R.id.try_again_reload);
                            if (try_again_reload != null) {
                                try_again_reload.setOnClickListener(v -> denyRequest(reid, status));
                            }
                        }
                        staticUtil.lockOrientation(activity, false);
                    });
                }
                @Override
                public void onFailure(final int statusCode, final Client.Headers headers, final int state) {
                    thread.runOnUI(() -> {
                        log.v(TAG, "denyRequest | reid=" + reid + " | status=" + status + " | failure | statusCode=" + statusCode);
                        draw(R.layout.state_failed_button);
                        TextView try_again_message = container.findViewById(R.id.try_again_message);
                        if (try_again_message != null) {
                            if (state == Room101Client.FAILED_SERVER_ERROR) {
                                try_again_message.setText(Room101Client.getFailureMessage(activity, statusCode));
                            } else {
                                try_again_message.setText(R.string.wrong_response_from_server);
                            }
                        }
                        View try_again_reload = container.findViewById(R.id.try_again_reload);
                        if (try_again_reload != null) {
                            try_again_reload.setOnClickListener(v -> denyRequest(reid, status));
                        }
                        staticUtil.lockOrientation(activity, false);
                    });
                }
                @Override
                public void onProgress(final int state) {
                    thread.runOnUI(() -> {
                        log.v(TAG, "denyRequest | reid=" + reid + " | status=" + status + " | progress " + state);
                        draw(R.layout.state_loading_text);
                        TextView loading_message = container.findViewById(R.id.loading_message);
                        if (loading_message != null) {
                            switch (state) {
                                case Room101Client.STATE_HANDLING: loading_message.setText(R.string.deny_request); break;
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

    private void addRequest() {
        thread.runOnUI(() -> {
            log.v(TAG, "addRequest");
            if (App.OFFLINE_MODE) {
                log.v(TAG, "addRequest rejected: offline mode");
                notificationMessage.snackBar(activity, R.id.room101_review_swipe, activity.getString(R.string.device_offline_action_refused));
            } else {
                draw(R.layout.layout_room101_add_request);
                staticUtil.lockOrientation(activity, true);
                final View room101_close_add_request = container.findViewById(R.id.room101_close_add_request);
                final LinearLayout room101_back = container.findViewById(R.id.room101_back);
                final LinearLayout room101_forward = container.findViewById(R.id.room101_forward);
                final TextView room101_back_text = container.findViewById(R.id.room101_back_text);
                final TextView room101_forward_text = container.findViewById(R.id.room101_forward_text);
                final ProgressBar progressBar = container.findViewById(R.id.room101_progress_bar);
                room101AddRequest.start(activity, new Room101AddRequest.Callback() {
                    @Override
                    public void onProgress(@Room101AddRequest.Stage final int stage) {
                        thread.runOnUI(() -> {
                            try {
                                progressBar.setProgress((stage * 100) / Room101AddRequest.STAGE_TOTAL);
                                room101_back.setAlpha(1f);
                                room101_forward.setAlpha(1f);
                                switch (stage) {
                                    case Room101AddRequest.STAGE_PICK_DATE_LOAD:
                                    case Room101AddRequest.STAGE_PICK_TIME_START_LOAD:
                                    case Room101AddRequest.STAGE_PICK_TIME_END_LOAD:
                                    case Room101AddRequest.STAGE_PICK_CONFIRMATION_LOAD:
                                    case Room101AddRequest.STAGE_PICK_CREATE:
                                        room101_back.setAlpha(0.2f);
                                        room101_forward.setAlpha(0.2f);
                                        if (stage == Room101AddRequest.STAGE_PICK_DATE_LOAD) {
                                            room101_back_text.setText(R.string.do_cancel);
                                            room101_forward_text.setText(R.string.next);
                                        }
                                        if (stage == Room101AddRequest.STAGE_PICK_TIME_START_LOAD) {
                                            room101_back_text.setText(R.string.back);
                                        }
                                        break;
                                    case Room101AddRequest.STAGE_PICK_DATE:
                                        room101_back_text.setText(R.string.do_cancel);
                                        room101_forward_text.setText(R.string.next);
                                        break;
                                    case Room101AddRequest.STAGE_PICK_TIME_START:
                                    case Room101AddRequest.STAGE_PICK_TIME_END:
                                        room101_back_text.setText(R.string.back);
                                        room101_forward_text.setText(R.string.next);
                                        break;
                                    case Room101AddRequest.STAGE_PICK_CONFIRMATION:
                                        room101_back_text.setText(R.string.back);
                                        room101_forward_text.setText(R.string.create);
                                        break;
                                    case Room101AddRequest.STAGE_PICK_DONE:
                                        room101_back.setAlpha(0.0f);
                                        room101_back_text.setText(R.string.close);
                                        room101_forward_text.setText(R.string.close);
                                        break;
                                }
                            } catch (Exception e){
                                log.exception(e);
                                notificationMessage.snackBar(activity, R.id.room101_review_swipe, activity.getString(R.string.error_occurred_while_room101_request));
                                load(false);
                            }
                        });
                    }
                    @Override
                    public void onDraw(final View view) {
                        thread.runOnUI(() -> {
                            try {
                                if (view == null) {
                                    notificationMessage.snackBar(activity, R.id.room101_review_swipe, activity.getString(R.string.error_occurred));
                                    load(false);
                                    return;
                                }
                                ViewGroup vg = container.findViewById(R.id.room101_add_request_container);
                                if (vg != null) {
                                    vg.removeAllViews();
                                    vg.addView(view);
                                }
                            } catch (Exception e){
                                log.exception(e);
                                notificationMessage.snackBar(activity, R.id.room101_review_swipe, activity.getString(R.string.error_occurred));
                                load(false);
                            }
                        });
                    }
                    @Override
                    public void onClose() {
                        load(false);
                        staticUtil.lockOrientation(activity, false);
                    }
                    @Override
                    public void onDone() {
                        load(true);
                        staticUtil.lockOrientation(activity, false);
                    }
                });
                if (room101_close_add_request != null) {
                    room101_close_add_request.setOnClickListener(v -> {
                        log.v(TAG, "room101_close_add_request clicked");
                        room101AddRequest.close(false);
                    });
                }
                if (room101_back != null) {
                    room101_back.setOnClickListener(v -> {
                        log.v(TAG, "room101_back clicked");
                        room101AddRequest.back();
                    });
                }
                if (room101_forward != null) {
                    room101_forward.setOnClickListener(v -> {
                        log.v(TAG, "room101_forward clicked");
                        room101AddRequest.forward();
                    });
                }
            }
        });
    }

    public static void execute(final Context context, final Room101Client room101Client, final InjectProvider injectProvider, final String scope, final ResponseHandler responseHandler) {
        injectProvider.getThread().run(Thread.BACKGROUND, () -> {
            injectProvider.getLog().v(TAG, "execute | scope=" + scope);
            HashMap<String, String> params = new HashMap<>();
            params.put("getFunc", "isLoginPassword");
            params.put("view", scope);
            params.put("login", injectProvider.getStorage().get(context, Storage.PERMANENT, Storage.USER, "user#deifmo#login"));
            params.put("password", injectProvider.getStorage().get(context, Storage.PERMANENT, Storage.USER, "user#deifmo#password"));
            room101Client.post(context, "index.php", params, new ResponseHandler() {
                @Override
                public void onSuccess(final int statusCode, final Client.Headers headers, final String response) {
                    //noinspection Convert2Lambda
                    injectProvider.getThread().run(new Runnable() {
                        @Override
                        public void run() {
                            if (statusCode == 302) {
                                injectProvider.getLog().v(TAG, "execute | scope=" + scope + " | success | statusCode=" + statusCode);
                                String location = headers.getValue("Location");
                                if (location != null && !location.trim().isEmpty()) {
                                    room101Client.get(context, location, null, new ResponseHandler() {
                                        @Override
                                        public void onSuccess(int sc, Client.Headers h, String r) {
                                            responseHandler.onSuccess(sc, h, r);
                                        }
                                        @Override
                                        public void onFailure(int sc, Client.Headers h, int s) {
                                            responseHandler.onFailure(sc, h, s);
                                        }
                                        @Override
                                        public void onProgress(int s) {
                                            responseHandler.onProgress(s);
                                        }
                                        @Override
                                        public void onNewRequest(Client.Request r) {
                                            responseHandler.onNewRequest(r);
                                        }
                                    });
                                } else {
                                    responseHandler.onFailure(statusCode, headers, Room101.FAILED_EXPECTED_REDIRECTION);
                                }
                            } else {
                                injectProvider.getLog().v(TAG, "execute | scope=" + scope + " | success(not really) | statusCode=" + statusCode);
                                if (response.contains("Доступ запрещен") || (response.contains("Неверный") && response.contains("логин/пароль"))) {
                                    responseHandler.onFailure(statusCode, headers, Room101.FAILED_AUTH);
                                } else {
                                    responseHandler.onFailure(statusCode, headers, Room101.FAILED_EXPECTED_REDIRECTION);
                                }
                            }
                        }
                    });
                }
                @Override
                public void onFailure(final int statusCode, final Client.Headers headers, final int state) {
                    injectProvider.getThread().run(Thread.BACKGROUND, () -> responseHandler.onFailure(statusCode, headers, state));
                }
                @Override
                public void onProgress(final int state) {
                    injectProvider.getThread().run(() -> {
                        injectProvider.getLog().v(TAG, "execute | scope=" + scope + " | progress " + state);
                        responseHandler.onProgress(state);
                    });
                }
                @Override
                public void onNewRequest(Client.Request request) {
                    responseHandler.onNewRequest(request);
                }
            });
        });
    }

    private void load() {
        thread.run(() -> load(storagePref.get(activity, "pref_use_cache", true) ? Integer.parseInt(storagePref.get(activity, "pref_dynamic_refresh", "0")) : 0));
    }
    private void load(final int refresh_rate) {
        thread.run(() -> {
            log.v(TAG, "load | refresh_rate=" + refresh_rate);
            if (storagePref.get(activity, "pref_use_cache", true)) {
                String cache = storage.get(activity, Storage.CACHE, Storage.USER, "room101#core").trim();
                if (!cache.isEmpty()) {
                    try {
                        setData(new JSONObject(cache));
                        if (getData() == null || getData().getLong("timestamp") + refresh_rate * 3600000L < time.getCalendar().getTimeInMillis()) {
                            load(true, cache);
                        } else {
                            load(false, cache);
                        }
                    } catch (JSONException e) {
                        log.exception(e);
                        load(true, cache);
                    }
                } else {
                    load(false);
                }
            } else {
                load(false);
            }
        });
    }
    private void load(final boolean force) {
        thread.run(() -> load(force, ""));
    }
    private void load(final boolean force, final String cache) {
        thread.run(() -> {
            log.v(TAG, "load | force=" + (force ? "true" : "false"));
            if ((!force || !Client.isOnline(activity)) && storagePref.get(activity, "pref_use_cache", true)) {
                try {
                    String c = cache.isEmpty() ? storage.get(activity, Storage.CACHE, Storage.USER, "room101#core").trim() : cache;
                    if (!c.isEmpty()) {
                        log.v(TAG, "load | from cache");
                        setData(new JSONObject(c));
                        display();
                        return;
                    }
                } catch (Exception e) {
                    log.v(TAG, "load | failed to load from cache");
                    storage.delete(activity, Storage.CACHE, Storage.USER, "room101#core");
                }
            }
            if (!App.OFFLINE_MODE) {
                execute(activity, room101Client, injectProvider, "delRequest", new ResponseHandler() {
                    @Override
                    public void onSuccess(final int statusCode, final Client.Headers headers, final String response) {
                        thread.run(() -> {
                            log.v(TAG, "load | success | statusCode=" + statusCode);
                            if (statusCode == 200) {
                                new Room101ViewRequestParse(activity, response, json -> {
                                    if (json != null) {
                                        try {
                                            json = new JSONObject()
                                                    .put("timestamp", time.getCalendar().getTimeInMillis())
                                                    .put("data", json);
                                            if (storagePref.get(activity, "pref_use_cache", true)) {
                                                storage.put(activity, Storage.CACHE, Storage.USER, "room101#core", json.toString());
                                            }
                                            setData(json);
                                            display();
                                        } catch (JSONException e) {
                                            log.exception(e);
                                            if (getData() != null) {
                                                display();
                                            } else {
                                                loadFailed();
                                            }
                                        }
                                    } else {
                                        if (getData() != null) {
                                            display();
                                        } else {
                                            loadFailed();
                                        }
                                    }
                                }).run();
                            } else {
                                if (getData() != null) {
                                    display();
                                } else {
                                    loadFailed();
                                }
                            }
                        });
                    }
                    @Override
                    public void onFailure(final int statusCode, final Client.Headers headers, final int state) {
                        thread.runOnUI(() -> {
                            log.v(TAG, "load | failure " + state);
                            switch (state) {
                                case Room101Client.FAILED_OFFLINE:
                                    if (getData() != null) {
                                        display();
                                    } else {
                                        draw(R.layout.state_offline_text);
                                        if (activity != null) {
                                            View offline_reload = container.findViewById(R.id.offline_reload);
                                            if (offline_reload != null) {
                                                offline_reload.setOnClickListener(v -> load(true));
                                            }
                                        }
                                    }
                                    break;
                                case Room101Client.FAILED_SERVER_ERROR:
                                case Room101Client.FAILED_TRY_AGAIN:
                                case Room101Client.FAILED_EXPECTED_REDIRECTION:
                                    draw(R.layout.state_failed_button);
                                    if (activity != null) {
                                        if (state == Room101Client.FAILED_EXPECTED_REDIRECTION) {
                                            TextView try_again_message = container.findViewById(R.id.try_again_message);
                                            if (try_again_message != null) try_again_message.setText(R.string.wrong_response_from_server);
                                        }
                                        if (state == Room101Client.FAILED_SERVER_ERROR) {
                                            TextView try_again_message = container.findViewById(R.id.try_again_message);
                                            if (try_again_message != null) try_again_message.setText(Room101Client.getFailureMessage(activity, statusCode));
                                        }
                                        View try_again_reload = container.findViewById(R.id.try_again_reload);
                                        if (try_again_reload != null) {
                                            try_again_reload.setOnClickListener(v -> load(true));
                                        }
                                    }
                                    break;
                                case Room101Client.FAILED_AUTH:
                                    draw(R.layout.state_failed_button);
                                    if (activity != null) {
                                        View try_again_reload = container.findViewById(R.id.try_again_reload);
                                        if (try_again_reload != null) {
                                            ((ViewGroup) try_again_reload.getParent()).removeView(try_again_reload);
                                            TextView try_again_message = container.findViewById(R.id.try_again_message);
                                            if (try_again_message != null) try_again_message.setText(R.string.room101_auth_failed);
                                        }
                                    }
                                    break;
                            }
                        });
                    }
                    @Override
                    public void onProgress(final int state) {
                        thread.runOnUI(() -> {
                            log.v(TAG, "load | progress " + state);
                            draw(R.layout.state_loading_text);
                            if (activity != null) {
                                TextView loading_message = container.findViewById(R.id.loading_message);
                                if (loading_message != null) {
                                    switch (state) {
                                        case Room101Client.STATE_HANDLING:
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
                thread.runOnUI(() -> {
                    if (getData() != null) {
                        display();
                    } else {
                        draw(R.layout.state_offline_text);
                        if (activity != null) {
                            View offline_reload = container.findViewById(R.id.offline_reload);
                            if (offline_reload != null) {
                                offline_reload.setOnClickListener(v -> load());
                            }
                        }
                    }
                });
            }
        });
    }
    private void loadFailed() {
        thread.runOnUI(() -> {
            log.v(TAG, "loadFailed");
            try {
                draw(R.layout.state_failed_button);
                View try_again_reload = container.findViewById(R.id.try_again_reload);
                if (try_again_reload != null) {
                    try_again_reload.setOnClickListener(v -> load(true));
                }
            } catch (Exception e) {
                log.exception(e);
            }
        });
    }
    private void display() {
        thread.runOnUI(() -> {
            log.v(TAG, "display");
            try {
                if (getData() == null) throw new NullPointerException("data cannot be null");
                JSONObject viewRequest = getData().getJSONObject("data");
                if (viewRequest == null) throw new NullPointerException("viewRequest cannot be null");
                if (action_extra != null) {
                    switch (action_extra) {
                        case "create":
                            action_extra = null;
                            addRequest();
                            return;
                    }
                    action_extra = null;
                }
                draw(R.layout.layout_room101_review);
                TextView room101_limit = container.findViewById(R.id.room101_limit);
                TextView room101_last = container.findViewById(R.id.room101_last);
                TextView room101_penalty = container.findViewById(R.id.room101_penalty);
                if (room101_limit != null) room101_limit.setText(viewRequest.getString("limit"));
                if (room101_last != null) room101_last.setText(viewRequest.getString("left"));
                if (room101_penalty != null) room101_penalty.setText(viewRequest.getString("penalty"));
                final LinearLayout room101_review_container = container.findViewById(R.id.room101_review_container);
                thread.run(new Room101ReviewBuilder(activity, this, viewRequest.getJSONArray("sessions"), (state, layout) -> thread.runOnUI(() -> {
                    try {
                        if (room101_review_container != null) {
                            room101_review_container.removeAllViews();
                            if (state == Room101ReviewBuilder.STATE_DONE || state == Room101ReviewBuilder.STATE_LOADING) {
                                room101_review_container.addView(layout);
                            } else if (state == Room101ReviewBuilder.STATE_FAILED) {
                                loadFailed();
                            }
                        }
                    } catch (Exception e) {
                        log.exception(e);
                        loadFailed();
                    }
                })));
                // работаем со свайпом
                SwipeRefreshLayout mSwipeRefreshLayout = container.findViewById(R.id.room101_review_swipe);
                if (mSwipeRefreshLayout != null) {
                    mSwipeRefreshLayout.setColorSchemeColors(Color.resolve(activity, R.attr.colorAccent));
                    mSwipeRefreshLayout.setProgressBackgroundColorSchemeColor(Color.resolve(activity, R.attr.colorBackgroundRefresh));
                    mSwipeRefreshLayout.setOnRefreshListener(this);
                }
                // плавающая кнопка
                FloatingActionButton fab = container.findViewById(R.id.fab);
                if (fab != null) {
                    fab.setOnClickListener(v -> {
                        log.v(TAG, "fab button clicked");
                        addRequest();
                    });
                }
                notificationMessage.showUpdateTime(activity, R.id.room101_review_swipe, viewRequest.getLong("timestamp"));
            } catch (Exception e){
                log.exception(e);
                loadFailed();
            }
        });
    }

    private void setData(JSONObject data) {
        this.data = data;
        storeData(this, data.toString());
    }
    private JSONObject getData() {
        if (data != null) {
            return data;
        }
        try {
            String stored = restoreData(this);
            if (stored != null && !stored.isEmpty()) {
                data = textUtils.string2json(stored);
                return data;
            }
        } catch (Exception e) {
            log.exception(e);
        }
        return null;
    }
}

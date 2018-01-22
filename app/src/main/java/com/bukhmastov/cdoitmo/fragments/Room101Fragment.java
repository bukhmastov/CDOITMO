package com.bukhmastov.cdoitmo.fragments;

import android.app.Activity;
import android.content.Context;
import android.content.Intent;
import android.os.Bundle;
import android.support.annotation.NonNull;
import android.support.annotation.Nullable;
import android.support.design.widget.FloatingActionButton;
import android.support.v4.widget.SwipeRefreshLayout;
import android.support.v7.app.AlertDialog;
import android.view.InflateException;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.LinearLayout;
import android.widget.ProgressBar;
import android.widget.TextView;

import com.bukhmastov.cdoitmo.R;
import com.bukhmastov.cdoitmo.builders.Room101ReviewBuilder;
import com.bukhmastov.cdoitmo.firebase.FirebaseAnalyticsProvider;
import com.bukhmastov.cdoitmo.network.Room101Client;
import com.bukhmastov.cdoitmo.network.interfaces.ResponseHandler;
import com.bukhmastov.cdoitmo.network.models.Client;
import com.bukhmastov.cdoitmo.network.models.Room101;
import com.bukhmastov.cdoitmo.objects.Room101AddRequest;
import com.bukhmastov.cdoitmo.parse.Room101ViewRequestParse;
import com.bukhmastov.cdoitmo.utils.Log;
import com.bukhmastov.cdoitmo.utils.Static;
import com.bukhmastov.cdoitmo.utils.Storage;

import org.json.JSONException;
import org.json.JSONObject;

import java.util.HashMap;

public class Room101Fragment extends ConnectedFragment implements SwipeRefreshLayout.OnRefreshListener, Room101ReviewBuilder.register {

    private static final String TAG = "Room101Fragment";
    public static JSONObject data = null;
    private boolean loaded = false;
    public static Client.Request requestHandle = null;
    private String action_extra = null;
    protected boolean forbidden = false;

    @Override
    public void onCreate(@Nullable Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        Log.v(TAG, "Fragment created");
        if (Static.UNAUTHORIZED_MODE) {
            forbidden = true;
            Log.w(TAG, "Fragment created | UNAUTHORIZED_MODE not allowed, closing fragment...");
            close();
            return;
        }
        FirebaseAnalyticsProvider.logCurrentScreen(activity, this);
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
        Log.v(TAG, "Fragment destroyed");
    }

    @Override
    public View onCreateView(@NonNull LayoutInflater inflater, ViewGroup container, Bundle savedInstanceState) {
        return inflater.inflate(R.layout.fragment_room101, container, false);
    }

    @Override
    public void onResume() {
        super.onResume();
        Log.v(TAG, "resumed");
        if (forbidden) {
            return;
        }
        FirebaseAnalyticsProvider.setCurrentScreen(activity, this);
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
        load(true);
    }

    @Override
    public void onDenyRequest(final int reid, final int status) {
        Static.T.runOnUiThread(() -> {
            Log.v(TAG, "onDenyRequest | reid=" + reid + " | status=" + status);
            if (Static.OFFLINE_MODE) {
                Log.v(TAG, "onDenyRequest rejected: offline mode");
                Static.snackBar(activity, R.id.room101_review_swipe, activity.getString(R.string.device_offline_action_refused));
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
        Static.T.runThread(() -> {
            Log.v(TAG, "denyRequest | reid=" + reid + " | status=" + status);
            HashMap<String, String> params = new HashMap<>();
            switch (status) {
                case 1: params.put("getFunc", "snatRequest"); break;
                default: params.put("getFunc", "delRequest"); break;
            }
            params.put("reid", String.valueOf(reid));
            params.put("login", Storage.file.perm.get(activity, "user#deifmo#login"));
            params.put("password", Storage.file.perm.get(activity, "user#deifmo#password"));
            Room101Client.post(activity, "delRequest.php", params, new ResponseHandler() {
                @Override
                public void onSuccess(final int statusCode, final Client.Headers headers, final String response) {
                    Static.T.runOnUiThread(() -> {
                        if (statusCode == 302) {
                            Log.v(TAG, "denyRequest | reid=" + reid + " | status=" + status + " | success | statusCode=" + statusCode);
                            load(true);
                            FirebaseAnalyticsProvider.logEvent(
                                    activity,
                                    FirebaseAnalyticsProvider.Event.ROOM101_REQUEST_DENIED
                            );
                        } else {
                            Log.v(TAG, "denyRequest | reid=" + reid + " | status=" + status + " | success(not really) | statusCode=" + statusCode);
                            draw(R.layout.state_try_again);
                            TextView try_again_message = activity.findViewById(R.id.try_again_message);
                            if (try_again_message != null) try_again_message.setText(R.string.wrong_response_from_server);
                            View try_again_reload = activity.findViewById(R.id.try_again_reload);
                            if (try_again_reload != null) {
                                try_again_reload.setOnClickListener(v -> denyRequest(reid, status));
                            }
                        }
                    });
                }
                @Override
                public void onFailure(final int statusCode, final Client.Headers headers, final int state) {
                    Static.T.runOnUiThread(() -> {
                        Log.v(TAG, "denyRequest | reid=" + reid + " | status=" + status + " | failure | statusCode=" + statusCode);
                        draw(R.layout.state_try_again);
                        TextView try_again_message = activity.findViewById(R.id.try_again_message);
                        if (try_again_message != null) {
                            if (state == Room101Client.FAILED_SERVER_ERROR) {
                                try_again_message.setText(Room101Client.getFailureMessage(activity, statusCode));
                            } else {
                                try_again_message.setText(R.string.wrong_response_from_server);
                            }
                        }
                        View try_again_reload = activity.findViewById(R.id.try_again_reload);
                        if (try_again_reload != null) {
                            try_again_reload.setOnClickListener(v -> denyRequest(reid, status));
                        }
                    });
                }
                @Override
                public void onProgress(final int state) {
                    Static.T.runOnUiThread(() -> {
                        Log.v(TAG, "denyRequest | reid=" + reid + " | status=" + status + " | progress " + state);
                        draw(R.layout.state_loading);
                        TextView loading_message = activity.findViewById(R.id.loading_message);
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
        Static.T.runOnUiThread(() -> {
            Log.v(TAG, "addRequest");
            if (Static.OFFLINE_MODE) {
                Log.v(TAG, "addRequest rejected: offline mode");
                Static.snackBar(activity, R.id.room101_review_swipe, activity.getString(R.string.device_offline_action_refused));
            } else {
                draw(R.layout.layout_room101_add_request);
                final View room101_close_add_request = activity.findViewById(R.id.room101_close_add_request);
                final LinearLayout room101_back = activity.findViewById(R.id.room101_back);
                final LinearLayout room101_forward = activity.findViewById(R.id.room101_forward);
                final TextView room101_back_text = activity.findViewById(R.id.room101_back_text);
                final TextView room101_forward_text = activity.findViewById(R.id.room101_forward_text);
                final ProgressBar progressBar = activity.findViewById(R.id.room101_progress_bar);
                final Room101AddRequest room101AddRequest = new Room101AddRequest(activity, new Room101AddRequest.callback() {
                    @Override
                    public void onProgress(final int stage) {
                        Static.T.runOnUiThread(() -> {
                            try {
                                progressBar.setProgress((stage * 100) / Room101AddRequest.STAGES_COUNT);
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
                                Static.error(e);
                                Static.snackBar(activity, R.id.room101_review_swipe, activity.getString(R.string.error_occurred_while_room101_request));
                                load(false);
                            }
                        });
                    }
                    @Override
                    public void onDraw(final View view) {
                        Static.T.runOnUiThread(() -> {
                            try {
                                ViewGroup vg = activity.findViewById(R.id.room101_add_request_container);
                                if (vg != null) {
                                    vg.removeAllViews();
                                    vg.addView(view);
                                }
                            } catch (Exception e){
                                Static.error(e);
                                Static.snackBar(activity, R.id.room101_review_swipe, activity.getString(R.string.error_occurred));
                                load(false);
                            }
                        });
                    }
                    @Override
                    public void onClose() {
                        load(false);
                    }
                    @Override
                    public void onDone() {
                        load(true);
                    }
                });
                if (room101_close_add_request != null) {
                    room101_close_add_request.setOnClickListener(v -> {
                        Log.v(TAG, "room101_close_add_request clicked");
                        room101AddRequest.close(false);
                    });
                }
                if (room101_back != null) {
                    room101_back.setOnClickListener(v -> {
                        Log.v(TAG, "room101_back clicked");
                        room101AddRequest.back();
                    });
                }
                if (room101_forward != null) {
                    room101_forward.setOnClickListener(v -> {
                        Log.v(TAG, "room101_forward clicked");
                        room101AddRequest.forward();
                    });
                }
            }
        });
    }

    public static void execute(final Context context, final String scope, final ResponseHandler responseHandler) {
        Static.T.runThread(Static.T.TYPE.BACKGROUND, () -> {
            Log.v(TAG, "execute | scope=" + scope);
            HashMap<String, String> params = new HashMap<>();
            params.put("getFunc", "isLoginPassword");
            params.put("view", scope);
            params.put("login", Storage.file.perm.get(context, "user#deifmo#login"));
            params.put("password", Storage.file.perm.get(context, "user#deifmo#password"));
            Room101Client.post(context, "index.php", params, new ResponseHandler() {
                @Override
                public void onSuccess(final int statusCode, final Client.Headers headers, final String response) {
                    //noinspection Convert2Lambda
                    Static.T.runThread(new Runnable() {
                        @Override
                        public void run() {
                            if (statusCode == 302) {
                                Log.v(TAG, "execute | scope=" + scope + " | success | statusCode=" + statusCode);
                                String location = headers.getValue("Location");
                                if (location != null && !location.trim().isEmpty()) {
                                    Room101Client.get(context, location, null, new ResponseHandler() {
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
                                Log.v(TAG, "execute | scope=" + scope + " | success(not really) | statusCode=" + statusCode);
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
                    Static.T.runThread(Static.T.TYPE.BACKGROUND, () -> responseHandler.onFailure(statusCode, headers, state));
                }
                @Override
                public void onProgress(final int state) {
                    Static.T.runThread(() -> {
                        Log.v(TAG, "execute | scope=" + scope + " | progress " + state);
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
        Static.T.runThread(() -> load(Storage.pref.get(activity, "pref_use_cache", true) ? Integer.parseInt(Storage.pref.get(activity, "pref_dynamic_refresh", "0")) : 0));
    }
    private void load(final int refresh_rate) {
        Static.T.runThread(() -> {
            Log.v(TAG, "load | refresh_rate=" + refresh_rate);
            if (Storage.pref.get(activity, "pref_use_cache", true)) {
                String cache = Storage.file.cache.get(activity, "room101#core").trim();
                if (!cache.isEmpty()) {
                    try {
                        data = new JSONObject(cache);
                        if (data.getLong("timestamp") + refresh_rate * 3600000L < Static.getCalendar().getTimeInMillis()) {
                            load(true, cache);
                        } else {
                            load(false, cache);
                        }
                    } catch (JSONException e) {
                        Static.error(e);
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
        Static.T.runThread(() -> load(force, ""));
    }
    private void load(final boolean force, final String cache) {
        Static.T.runThread(() -> {
            Log.v(TAG, "load | force=" + (force ? "true" : "false"));
            if ((!force || !Static.isOnline(activity)) && Storage.pref.get(activity, "pref_use_cache", true)) {
                try {
                    String c = cache.isEmpty() ? Storage.file.cache.get(activity, "room101#core").trim() : cache;
                    if (!c.isEmpty()) {
                        Log.v(TAG, "load | from cache");
                        data = new JSONObject(c);
                        display();
                        return;
                    }
                } catch (Exception e) {
                    Log.v(TAG, "load | failed to load from cache");
                    Storage.file.cache.delete(activity, "room101#core");
                }
            }
            if (!Static.OFFLINE_MODE) {
                execute(activity, "delRequest", new ResponseHandler() {
                    @Override
                    public void onSuccess(final int statusCode, final Client.Headers headers, final String response) {
                        Static.T.runThread(() -> {
                            Log.v(TAG, "load | success | statusCode=" + statusCode);
                            if (statusCode == 200) {
                                new Room101ViewRequestParse(activity, response, json -> {
                                    if (json != null) {
                                        try {
                                            json = new JSONObject()
                                                    .put("timestamp", Static.getCalendar().getTimeInMillis())
                                                    .put("data", json);
                                            if (Storage.pref.get(activity, "pref_use_cache", true)) {
                                                Storage.file.cache.put(activity, "room101#core", json.toString());
                                            }
                                            data = json;
                                            display();
                                        } catch (JSONException e) {
                                            Static.error(e);
                                            if (data != null) {
                                                display();
                                            } else {
                                                loadFailed();
                                            }
                                        }
                                    } else {
                                        if (data != null) {
                                            display();
                                        } else {
                                            loadFailed();
                                        }
                                    }
                                }).run();
                            } else {
                                if (data != null) {
                                    display();
                                } else {
                                    loadFailed();
                                }
                            }
                        });
                    }
                    @Override
                    public void onFailure(final int statusCode, final Client.Headers headers, final int state) {
                        Static.T.runOnUiThread(() -> {
                            Log.v(TAG, "load | failure " + state);
                            switch (state) {
                                case Room101Client.FAILED_OFFLINE:
                                    if (data != null) {
                                        display();
                                    } else {
                                        draw(R.layout.state_offline);
                                        if (activity != null) {
                                            View offline_reload = activity.findViewById(R.id.offline_reload);
                                            if (offline_reload != null) {
                                                offline_reload.setOnClickListener(v -> load(true));
                                            }
                                        }
                                    }
                                    break;
                                case Room101Client.FAILED_SERVER_ERROR:
                                case Room101Client.FAILED_TRY_AGAIN:
                                case Room101Client.FAILED_EXPECTED_REDIRECTION:
                                    draw(R.layout.state_try_again);
                                    if (activity != null) {
                                        if (state == Room101Client.FAILED_EXPECTED_REDIRECTION) {
                                            TextView try_again_message = activity.findViewById(R.id.try_again_message);
                                            if (try_again_message != null) try_again_message.setText(R.string.wrong_response_from_server);
                                        }
                                        if (state == Room101Client.FAILED_SERVER_ERROR) {
                                            TextView try_again_message = activity.findViewById(R.id.try_again_message);
                                            if (try_again_message != null) try_again_message.setText(Room101Client.getFailureMessage(activity, statusCode));
                                        }
                                        View try_again_reload = activity.findViewById(R.id.try_again_reload);
                                        if (try_again_reload != null) {
                                            try_again_reload.setOnClickListener(v -> load(true));
                                        }
                                    }
                                    break;
                                case Room101Client.FAILED_AUTH:
                                    draw(R.layout.state_try_again);
                                    if (activity != null) {
                                        View try_again_reload = activity.findViewById(R.id.try_again_reload);
                                        if (try_again_reload != null) {
                                            ((ViewGroup) try_again_reload.getParent()).removeView(try_again_reload);
                                            TextView try_again_message = activity.findViewById(R.id.try_again_message);
                                            if (try_again_message != null) try_again_message.setText(R.string.room101_auth_failed);
                                        }
                                    }
                                    break;
                            }
                        });
                    }
                    @Override
                    public void onProgress(final int state) {
                        Static.T.runOnUiThread(() -> {
                            Log.v(TAG, "load | progress " + state);
                            draw(R.layout.state_loading);
                            if (activity != null) {
                                TextView loading_message = activity.findViewById(R.id.loading_message);
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
                Static.T.runOnUiThread(() -> {
                    if (data != null) {
                        display();
                    } else {
                        draw(R.layout.state_offline);
                        if (activity != null) {
                            View offline_reload = activity.findViewById(R.id.offline_reload);
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
        Static.T.runOnUiThread(() -> {
            Log.v(TAG, "loadFailed");
            try {
                draw(R.layout.state_try_again);
                View try_again_reload = activity.findViewById(R.id.try_again_reload);
                if (try_again_reload != null) {
                    try_again_reload.setOnClickListener(v -> load(true));
                }
            } catch (Exception e) {
                Static.error(e);
            }
        });
    }

    private void display() {
        Static.T.runOnUiThread(() -> {
            Log.v(TAG, "display");
            try {
                if (data == null) throw new NullPointerException("data cannot be null");
                JSONObject viewRequest = data.getJSONObject("data");
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
                TextView room101_limit = activity.findViewById(R.id.room101_limit);
                TextView room101_last = activity.findViewById(R.id.room101_last);
                TextView room101_penalty = activity.findViewById(R.id.room101_penalty);
                if (room101_limit != null) room101_limit.setText(viewRequest.getString("limit"));
                if (room101_last != null) room101_last.setText(viewRequest.getString("left"));
                if (room101_penalty != null) room101_penalty.setText(viewRequest.getString("penalty"));
                final LinearLayout room101_review_container = activity.findViewById(R.id.room101_review_container);
                Static.T.runThread(new Room101ReviewBuilder(activity, this, viewRequest.getJSONArray("sessions"), (state, layout) -> Static.T.runOnUiThread(() -> {
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
                        Static.error(e);
                        loadFailed();
                    }
                })));
                // работаем со свайпом
                SwipeRefreshLayout mSwipeRefreshLayout = activity.findViewById(R.id.room101_review_swipe);
                if (mSwipeRefreshLayout != null) {
                    mSwipeRefreshLayout.setColorSchemeColors(Static.colorAccent);
                    mSwipeRefreshLayout.setProgressBackgroundColorSchemeColor(Static.colorBackgroundRefresh);
                    mSwipeRefreshLayout.setOnRefreshListener(this);
                }
                // плавающая кнопка
                FloatingActionButton fab = activity.findViewById(R.id.fab);
                if (fab != null) {
                    fab.setOnClickListener(v -> {
                        Log.v(TAG, "fab button clicked");
                        addRequest();
                    });
                }
                Static.showUpdateTime(activity, R.id.room101_review_swipe, viewRequest.getLong("timestamp"), false);
            } catch (Exception e){
                Static.error(e);
                loadFailed();
            }
        });
    }

    private void draw(int layoutId) {
        try {
            ViewGroup vg = activity.findViewById(R.id.container_room101);
            if (vg != null) {
                vg.removeAllViews();
                vg.addView(inflate(layoutId), 0, new ViewGroup.LayoutParams(ViewGroup.LayoutParams.MATCH_PARENT, ViewGroup.LayoutParams.MATCH_PARENT));
            }
        } catch (Exception e){
            Static.error(e);
        }
    }
    private View inflate(int layoutId) throws InflateException {
        return ((LayoutInflater) activity.getSystemService(Context.LAYOUT_INFLATER_SERVICE)).inflate(layoutId, null);
    }
}

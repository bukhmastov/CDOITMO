package com.bukhmastov.cdoitmo.fragments;

import android.app.Activity;
import android.content.Context;
import android.content.DialogInterface;
import android.content.Intent;
import android.os.Bundle;
import android.support.annotation.Nullable;
import android.support.design.widget.FloatingActionButton;
import android.support.v4.widget.SwipeRefreshLayout;
import android.support.v7.app.AlertDialog;
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
import com.bukhmastov.cdoitmo.network.interfaces.Room101ClientResponseHandler;
import com.bukhmastov.cdoitmo.objects.Room101AddRequest;
import com.bukhmastov.cdoitmo.parse.Room101ViewRequestParse;
import com.bukhmastov.cdoitmo.utils.Log;
import com.bukhmastov.cdoitmo.utils.Static;
import com.bukhmastov.cdoitmo.utils.Storage;
import com.loopj.android.http.RequestHandle;
import com.loopj.android.http.RequestParams;

import org.json.JSONException;
import org.json.JSONObject;

import java.util.Calendar;
import java.util.Objects;

import cz.msebera.android.httpclient.Header;

public class Room101Fragment extends ConnectedFragment implements SwipeRefreshLayout.OnRefreshListener, Room101ReviewBuilder.register {

    private static final String TAG = "Room101Fragment";
    private boolean loaded = false;
    public static RequestHandle fragmentRequestHandle = null;
    private String action_extra = null;

    @Override
    public void onCreate(@Nullable Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        Log.v(TAG, "Fragment created");
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
    public View onCreateView(LayoutInflater inflater, ViewGroup container, Bundle savedInstanceState) {
        return inflater.inflate(R.layout.fragment_room101, container, false);
    }

    @Override
    public void onResume() {
        super.onResume();
        Log.v(TAG, "resumed");
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
        if (fragmentRequestHandle != null) {
            loaded = false;
            fragmentRequestHandle.cancel(true);
        }
    }

    @Override
    public void onRefresh() {
        Log.v(TAG, "refreshed");
        load(true);
    }

    @Override
    public void onDenyRequest(final int reid, final int status) {
        Log.v(TAG, "onDenyRequest | reid=" + reid + " | status=" + status);
        if (Static.OFFLINE_MODE) {
            Log.v(TAG, "onDenyRequest rejected: offline mode");
            Static.snackBar(activity, R.id.room101_review_swipe, getString(R.string.device_offline_action_refused));
        } else {
            (new AlertDialog.Builder(getContext())
                    .setTitle(R.string.request_deny)
                    .setMessage(getString(R.string.request_deny_1) + "\n" + getString(R.string.request_deny_2))
                    .setCancelable(true)
                    .setPositiveButton(R.string.delete, new DialogInterface.OnClickListener() {
                        @Override
                        public void onClick(DialogInterface dialog, int which) {
                            denyRequest(reid, status);
                            dialog.cancel();
                        }
                    })
                    .setNegativeButton(R.string.cancel, new DialogInterface.OnClickListener() {
                        @Override
                        public void onClick(DialogInterface dialog, int which) {
                            dialog.cancel();
                        }
                    })
                    .create()
            ).show();
        }
    }
    private void denyRequest(final int reid, final int status){
        Log.v(TAG, "denyRequest | reid=" + reid + " | status=" + status);
        RequestParams params = new RequestParams();
        switch (status) {
            case 1: params.put("getFunc", "snatRequest"); break;
            default: params.put("getFunc", "delRequest"); break;
        }
        params.put("reid", reid);
        params.put("login", Storage.file.perm.get(getContext(), "user#login"));
        params.put("password", Storage.file.perm.get(getContext(), "user#password"));
        Room101Client.post(getContext(), "delRequest.php", params, new Room101ClientResponseHandler() {
            @Override
            public void onSuccess(int statusCode, String response) {
                Log.v(TAG, "denyRequest | reid=" + reid + " | status=" + status + " | success(not really) | statusCode=" + statusCode);
                draw(R.layout.state_try_again);
                TextView try_again_message = (TextView) activity.findViewById(R.id.try_again_message);
                if (try_again_message != null) try_again_message.setText(R.string.wrong_response_from_server);
                View try_again_reload = activity.findViewById(R.id.try_again_reload);
                if (try_again_reload != null) {
                    try_again_reload.setOnClickListener(new View.OnClickListener() {
                        @Override
                        public void onClick(View v) {
                            denyRequest(reid, status);
                        }
                    });
                }
            }
            @Override
            public void onProgress(int state) {
                Log.v(TAG, "denyRequest | reid=" + reid + " | status=" + status + " | progress " + state);
                draw(R.layout.state_loading);
                TextView loading_message = (TextView) activity.findViewById(R.id.loading_message);
                if (loading_message != null) {
                    switch (state) {
                        case Room101Client.STATE_HANDLING: loading_message.setText(R.string.deny_request); break;
                    }
                }
            }
            @Override
            public void onFailure(int state, int statusCode, Header[] headers) {
                Log.v(TAG, "denyRequest | reid=" + reid + " | status=" + status + " | failure(rather success) | statusCode=" + statusCode);
                if (statusCode == 302) {
                    load(true);
                    FirebaseAnalyticsProvider.logEvent(
                            getContext(),
                            FirebaseAnalyticsProvider.Event.ROOM101_REQUEST_DENIED
                    );
                } else {
                    draw(R.layout.state_try_again);
                    TextView try_again_message = (TextView) activity.findViewById(R.id.try_again_message);
                    if (try_again_message != null) try_again_message.setText(R.string.wrong_response_from_server);
                    View try_again_reload = activity.findViewById(R.id.try_again_reload);
                    if (try_again_reload != null) {
                        try_again_reload.setOnClickListener(new View.OnClickListener() {
                            @Override
                            public void onClick(View v) {
                                denyRequest(reid, status);
                            }
                        });
                    }
                }
            }
            @Override
            public void onNewHandle(RequestHandle requestHandle) {
                fragmentRequestHandle = requestHandle;
            }
        });
    }

    private void addRequest(){
        Log.v(TAG, "addRequest");
        if (Static.OFFLINE_MODE) {
            Log.v(TAG, "addRequest rejected: offline mode");
            Static.snackBar(activity, R.id.room101_review_swipe, getString(R.string.device_offline_action_refused));
        } else {
            draw(R.layout.layout_room101_add_request);
            final View room101_close_add_request = activity.findViewById(R.id.room101_close_add_request);
            final LinearLayout room101_back = (LinearLayout) activity.findViewById(R.id.room101_back);
            final LinearLayout room101_forward = (LinearLayout) activity.findViewById(R.id.room101_forward);
            final TextView room101_back_text = (TextView) activity.findViewById(R.id.room101_back_text);
            final TextView room101_forward_text = (TextView) activity.findViewById(R.id.room101_forward_text);
            final ProgressBar progressBar = (ProgressBar) activity.findViewById(R.id.room101_progress_bar);
            final Room101AddRequest room101AddRequest = new Room101AddRequest(activity, new Room101AddRequest.callback() {
                @Override
                public void onProgress(int stage) {
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
                        Static.snackBar(activity, R.id.room101_review_swipe, getString(R.string.error_occurred_while_room101_request));
                        load(false);
                    }
                }
                @Override
                public void onDraw(View view) {
                    try {
                        ViewGroup vg = ((ViewGroup) activity.findViewById(R.id.room101_add_request_container));
                        if (vg != null) {
                            vg.removeAllViews();
                            vg.addView(view);
                        }
                    } catch (Exception e){
                        Static.error(e);
                        Static.snackBar(activity, R.id.room101_review_swipe, getString(R.string.error_occurred));
                        load(false);
                    }
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
                room101_close_add_request.setOnClickListener(new View.OnClickListener() {
                    @Override
                    public void onClick(View v) {
                        Log.v(TAG, "room101_close_add_request clicked");
                        room101AddRequest.close(false);
                    }
                });
            }
            if (room101_back != null) {
                room101_back.setOnClickListener(new View.OnClickListener() {
                    @Override
                    public void onClick(View v) {
                        Log.v(TAG, "room101_back clicked");
                        room101AddRequest.back();
                    }
                });
            }
            if (room101_forward != null) {
                room101_forward.setOnClickListener(new View.OnClickListener() {
                    @Override
                    public void onClick(View v) {
                        Log.v(TAG, "room101_forward clicked");
                        room101AddRequest.forward();
                    }
                });
            }
        }
    }

    private void load(){
        load(Storage.pref.get(getContext(), "pref_use_cache", true) ? Integer.parseInt(Storage.pref.get(getContext(), "pref_tab_refresh", "0")) : 0);
    }
    private void load(int refresh_rate){
        Log.v(TAG, "load | refresh_rate=" + refresh_rate);
        String cache = Storage.file.cache.get(getContext(), "room101#core");
        if (Objects.equals(cache, "") || refresh_rate == 0) {
            load(true);
        } else if (refresh_rate >= 0){
            try {
                if (new JSONObject(cache).getLong("timestamp") + refresh_rate * 3600000L < Calendar.getInstance().getTimeInMillis()) {
                    load(true);
                } else {
                    load(false);
                }
            } catch (JSONException e) {
                Static.error(e);
                load(true);
            }
        } else {
            load(false);
        }
    }
    private void load(boolean force){
        Log.v(TAG, "load | force=" + (force ? "true" : "false"));
        if (!force || Static.OFFLINE_MODE) {
            if (!Objects.equals(Storage.file.cache.get(getContext(), "room101#core"), "")) {
                display();
                return;
            }
        }
        if (!Static.OFFLINE_MODE) {
            execute(getContext(), "delRequest", new Room101ClientResponseHandler() {
                @Override
                public void onSuccess(int statusCode, String response) {
                    Log.v(TAG, "load | success | statusCode=" + statusCode);
                    if (statusCode == 200) {
                        new Room101ViewRequestParse(new Room101ViewRequestParse.response() {
                            @Override
                            public void finish(JSONObject json) {
                                if (json != null) {
                                    try {
                                        JSONObject jsonObject = new JSONObject();
                                        jsonObject.put("timestamp", Calendar.getInstance().getTimeInMillis());
                                        jsonObject.put("data", json);
                                        Storage.file.cache.put(getContext(), "room101#core", jsonObject.toString());
                                    } catch (JSONException e) {
                                        Static.error(e);
                                    }
                                }
                                display();
                            }
                        }, getContext()).execute(response);
                    } else {
                        loadFailed();
                    }
                }
                @Override
                public void onProgress(int state) {
                    Log.v(TAG, "load | progress " + state);
                    draw(R.layout.state_loading);
                    if (activity != null) {
                        TextView loading_message = (TextView) activity.findViewById(R.id.loading_message);
                        if (loading_message != null) {
                            switch (state) {
                                case Room101Client.STATE_HANDLING:
                                    loading_message.setText(R.string.loading);
                                    break;
                            }
                        }
                    }
                }
                @Override
                public void onFailure(int state, int statusCode, Header[] headers) {
                    Log.v(TAG, "load | failure " + state);
                    switch (state) {
                        case Room101Client.FAILED_OFFLINE:
                            try {
                                if (!Objects.equals(Storage.file.cache.get(getContext(), "room101#core"), "")) {
                                    display();
                                    return;
                                }
                            } catch (Exception e) {
                                Static.error(e);
                            }
                            draw(R.layout.state_offline);
                            if (activity != null) {
                                View offline_reload = activity.findViewById(R.id.offline_reload);
                                if (offline_reload != null) {
                                    offline_reload.setOnClickListener(new View.OnClickListener() {
                                        @Override
                                        public void onClick(View v) {
                                            load(true);
                                        }
                                    });
                                }
                            }
                            break;
                        case Room101Client.FAILED_TRY_AGAIN:
                        case Room101Client.FAILED_EXPECTED_REDIRECTION:
                            draw(R.layout.state_try_again);
                            if (activity != null) {
                                if (state == Room101Client.FAILED_EXPECTED_REDIRECTION) {
                                    TextView try_again_message = (TextView) activity.findViewById(R.id.try_again_message);
                                    if (try_again_message != null) try_again_message.setText(R.string.wrong_response_from_server);
                                }
                                View try_again_reload = activity.findViewById(R.id.try_again_reload);
                                if (try_again_reload != null) {
                                    try_again_reload.setOnClickListener(new View.OnClickListener() {
                                        @Override
                                        public void onClick(View v) {
                                            load(true);
                                        }
                                    });
                                }
                            }
                            break;
                        case Room101Client.FAILED_AUTH:
                            draw(R.layout.state_try_again);
                            if (activity != null) {
                                View try_again_reload = activity.findViewById(R.id.try_again_reload);
                                if (try_again_reload != null) {
                                    ((ViewGroup) try_again_reload.getParent()).removeView(try_again_reload);
                                    TextView try_again_message = (TextView) activity.findViewById(R.id.try_again_message);
                                    if (try_again_message != null) try_again_message.setText(R.string.room101_auth_failed);
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
        } else {
            draw(R.layout.state_offline);
            if (activity != null) {
                View offline_reload = activity.findViewById(R.id.offline_reload);
                if (offline_reload != null) {
                    offline_reload.setOnClickListener(new View.OnClickListener() {
                        @Override
                        public void onClick(View v) {
                            load(true);
                        }
                    });
                }
            }
        }
    }
    private void display(){
        Log.v(TAG, "display");
        try {
            JSONObject viewRequest = null;
            String cache = Storage.file.cache.get(getContext(), "room101#core");
            if (!cache.isEmpty()) {
                viewRequest = new JSONObject(cache).getJSONObject("data");
            }
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
            TextView room101_limit = (TextView) activity.findViewById(R.id.room101_limit);
            TextView room101_last = (TextView) activity.findViewById(R.id.room101_last);
            TextView room101_penalty = (TextView) activity.findViewById(R.id.room101_penalty);
            if (room101_limit != null) room101_limit.setText(viewRequest.getString("limit"));
            if (room101_last != null) room101_last.setText(viewRequest.getString("left"));
            if (room101_penalty != null) room101_penalty.setText(viewRequest.getString("penalty"));
            final LinearLayout room101_review_container = (LinearLayout) activity.findViewById(R.id.room101_review_container);
            (new Room101ReviewBuilder(activity, this, viewRequest.getJSONArray("sessions"), new Room101ReviewBuilder.response(){
                public void state(final int state, final View layout){
                    try {
                        activity.runOnUiThread(new Runnable() {
                            @Override
                            public void run() {
                                if (room101_review_container != null) {
                                    room101_review_container.removeAllViews();
                                    if (state == Room101ReviewBuilder.STATE_DONE || state == Room101ReviewBuilder.STATE_LOADING) {
                                        room101_review_container.addView(layout);
                                    } else if (state == Room101ReviewBuilder.STATE_FAILED) {
                                        loadFailed();
                                    }
                                }
                            }
                        });
                    } catch (NullPointerException e){
                        Static.error(e);
                        loadFailed();
                    }
                }
            })).start();
            // работаем со свайпом
            SwipeRefreshLayout mSwipeRefreshLayout = (SwipeRefreshLayout) activity.findViewById(R.id.room101_review_swipe);
            if (mSwipeRefreshLayout != null) {
                mSwipeRefreshLayout.setColorSchemeColors(Static.colorAccent);
                mSwipeRefreshLayout.setProgressBackgroundColorSchemeColor(Static.colorBackgroundRefresh);
                mSwipeRefreshLayout.setOnRefreshListener(this);
            }
            // плавающая кнопка
            FloatingActionButton fab = (FloatingActionButton) activity.findViewById(R.id.fab);
            if (fab != null) {
                fab.setOnClickListener(new View.OnClickListener() {
                    @Override
                    public void onClick(View view) {
                        Log.v(TAG, "fab button clicked");
                        addRequest();
                    }
                });
            }
            Static.showUpdateTime(activity, R.id.room101_review_swipe, viewRequest.getLong("timestamp"), false);
        } catch (Exception e){
            Static.error(e);
            loadFailed();
        }
    }
    private void loadFailed(){
        Log.v(TAG, "loadFailed");
        try {
            draw(R.layout.state_try_again);
            View try_again_reload = activity.findViewById(R.id.try_again_reload);
            if (try_again_reload != null) {
                try_again_reload.setOnClickListener(new View.OnClickListener() {
                    @Override
                    public void onClick(View v) {
                        load(true);
                    }
                });
            }
        } catch (Exception e) {
            Static.error(e);
        }
    }
    private void draw(int layoutId){
        try {
            ViewGroup vg = ((ViewGroup) activity.findViewById(R.id.container_room101));
            if (vg != null) {
                vg.removeAllViews();
                vg.addView(((LayoutInflater) activity.getSystemService(Context.LAYOUT_INFLATER_SERVICE)).inflate(layoutId, null), 0, new ViewGroup.LayoutParams(ViewGroup.LayoutParams.MATCH_PARENT, ViewGroup.LayoutParams.MATCH_PARENT));
            }
        } catch (Exception e){
            Static.error(e);
        }
    }
    public static void execute(final Context context, final String scope, final Room101ClientResponseHandler responseHandler){
        Log.v(TAG, "execute | scope=" + scope);
        RequestParams params = new RequestParams();
        params.put("getFunc", "isLoginPassword");
        params.put("view", scope);
        params.put("login", Storage.file.perm.get(context, "user#login"));
        params.put("password", Storage.file.perm.get(context, "user#password"));
        Room101Client.post(context, "index.php", params, new Room101ClientResponseHandler() {
            @Override
            public void onSuccess(int statusCode, String response) {
                Log.v(TAG, "execute | scope=" + scope + " | success(not really) | statusCode=" + statusCode);
                if (response.contains("Доступ запрещен") || (response.contains("Неверный") && response.contains("логин/пароль"))) {
                    responseHandler.onFailure(Room101Client.FAILED_AUTH, Room101Client.STATUS_CODE_EMPTY, null);
                } else {
                    responseHandler.onFailure(Room101Client.FAILED_EXPECTED_REDIRECTION, Room101Client.STATUS_CODE_EMPTY, null);
                }
            }
            @Override
            public void onProgress(int state) {
                Log.v(TAG, "execute | scope=" + scope + " | progress " + state);
                responseHandler.onProgress(state);
            }
            @Override
            public void onFailure(int state, int statusCode, Header[] headers) {
                Log.v(TAG, "execute | scope=" + scope + " | failure(rather success) | statusCode=" + statusCode);
                if (statusCode == 302) {
                    boolean found = false;
                    for (Header header : headers) {
                        if (Objects.equals(header.getName(), "Location")) {
                            String url = header.getValue().trim();
                            if (!url.isEmpty()) {
                                found = true;
                                Room101Client.get(context, url, null, new Room101ClientResponseHandler() {
                                    @Override
                                    public void onSuccess(int statusCode, String response) {
                                        responseHandler.onSuccess(statusCode, response);
                                    }
                                    @Override
                                    public void onProgress(int state) {
                                        responseHandler.onProgress(state);
                                    }
                                    @Override
                                    public void onFailure(int state, int statusCode, Header[] headers) {
                                        responseHandler.onFailure(state, statusCode, headers);
                                    }
                                    @Override
                                    public void onNewHandle(RequestHandle requestHandle) {
                                        responseHandler.onNewHandle(requestHandle);
                                    }
                                });
                            }
                            break;
                        }
                    }
                    if (!found) {
                        responseHandler.onFailure(Room101Client.FAILED_EXPECTED_REDIRECTION, Room101Client.STATUS_CODE_EMPTY, null);
                    }
                } else {
                    responseHandler.onFailure(Room101Client.FAILED_EXPECTED_REDIRECTION, Room101Client.STATUS_CODE_EMPTY, null);
                }
            }
            @Override
            public void onNewHandle(RequestHandle requestHandle) {
                responseHandler.onNewHandle(requestHandle);
            }
        });
    }

}
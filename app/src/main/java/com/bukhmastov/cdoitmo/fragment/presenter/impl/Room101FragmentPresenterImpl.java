package com.bukhmastov.cdoitmo.fragment.presenter.impl;

import android.app.AlertDialog;
import android.content.Context;
import android.content.Intent;
import android.view.View;
import android.view.ViewGroup;
import android.widget.LinearLayout;
import android.widget.ProgressBar;
import android.widget.TextView;

import com.bukhmastov.cdoitmo.App;
import com.bukhmastov.cdoitmo.R;
import com.bukhmastov.cdoitmo.builder.Room101ReviewBuilder;
import com.bukhmastov.cdoitmo.event.bus.EventBus;
import com.bukhmastov.cdoitmo.event.bus.annotation.Event;
import com.bukhmastov.cdoitmo.event.events.ClearCacheEvent;
import com.bukhmastov.cdoitmo.factory.AppComponentProvider;
import com.bukhmastov.cdoitmo.firebase.FirebaseAnalyticsProvider;
import com.bukhmastov.cdoitmo.fragment.presenter.Room101FragmentPresenter;
import com.bukhmastov.cdoitmo.model.parser.Room101RequestsParser;
import com.bukhmastov.cdoitmo.model.room101.requests.Room101Requests;
import com.bukhmastov.cdoitmo.network.Room101Client;
import com.bukhmastov.cdoitmo.network.handlers.ResponseHandler;
import com.bukhmastov.cdoitmo.network.handlers.joiner.ResponseHandlerJoiner;
import com.bukhmastov.cdoitmo.network.model.Client;
import com.bukhmastov.cdoitmo.object.Room101AddRequest;
import com.bukhmastov.cdoitmo.util.Log;
import com.bukhmastov.cdoitmo.util.NotificationMessage;
import com.bukhmastov.cdoitmo.util.Static;
import com.bukhmastov.cdoitmo.util.Storage;
import com.bukhmastov.cdoitmo.util.StoragePref;
import com.bukhmastov.cdoitmo.util.Thread;
import com.bukhmastov.cdoitmo.util.Time;
import com.bukhmastov.cdoitmo.util.singleton.Color;
import com.bukhmastov.cdoitmo.util.singleton.StringUtils;
import com.google.android.material.floatingactionbutton.FloatingActionButton;

import java.util.HashMap;

import javax.inject.Inject;

import androidx.swiperefreshlayout.widget.SwipeRefreshLayout;

import static com.bukhmastov.cdoitmo.util.Thread.R101;

public class Room101FragmentPresenterImpl extends ConnectedFragmentWithDataPresenterImpl<Room101Requests>
        implements Room101FragmentPresenter, SwipeRefreshLayout.OnRefreshListener, Room101ReviewBuilder.Request {

    private static final String TAG = "Room101Fragment";
    private String actionExtra = null;

    @Inject
    Log log;
    @Inject
    Thread thread;
    @Inject
    EventBus eventBus;
    @Inject
    Storage storage;
    @Inject
    StoragePref storagePref;
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
    FirebaseAnalyticsProvider firebaseAnalyticsProvider;

    public Room101FragmentPresenterImpl() {
        super(Room101Requests.class);
        AppComponentProvider.getComponent().inject(this);
        eventBus.register(this);
    }

    @Event
    public void onClearCacheEvent(ClearCacheEvent event) {
        if (event.isNot(ClearCacheEvent.ROOM101)) {
            return;
        }
        clearData();
    }

    @Override
    public void onStart() {
        super.onStart();
        thread.run(R101, () -> {
            if (App.UNAUTHORIZED_MODE) {
                forbidden = true;
                log.w(TAG, "UNAUTHORIZED_MODE not allowed, closing fragment...");
                thread.runOnUI(R101, () -> fragment.close());
                return;
            }
            firebaseAnalyticsProvider.logCurrentScreen(activity, fragment);
            Intent intent = activity.getIntent();
            if (intent != null) {
                actionExtra = intent.getStringExtra("action_extra");
                if (actionExtra != null) {
                    intent.removeExtra("action_extra");
                }
            }
        });
    }

    @Override
    public void onResume() {
        thread.run(R101, () -> {
            if (forbidden) {
                return;
            }
            firebaseAnalyticsProvider.setCurrentScreen(activity, fragment);
            if (actionExtra != null) {
                switch (actionExtra) {
                    case "create":
                        if (!App.OFFLINE_MODE) {
                            actionExtra = null;
                            addRequest();
                            return;
                        }
                }
            }
            if (!loaded) {
                loaded = true;
                if (getRestoredData() == null) {
                    load();
                } else {
                    display();
                }
            } else if (getData() == null) {
                load();
            } else {
                display();
            }
        });
    }

    @Override
    public void onRefresh() {
        thread.run(R101, () -> {
            log.v(TAG, "refreshing");
            load(true);
        });
    }

    @Override
    public void execute(Context context, String scope, ResponseHandler handler) {
        thread.assertNotUI();
        log.v(TAG, "execute | scope=" + scope);
        HashMap<String, String> params = new HashMap<>();
        params.put("getFunc", "isLoginPassword");
        params.put("view", scope);
        params.put("login", storage.get(context, Storage.PERMANENT, Storage.USER, "user#deifmo#login"));
        params.put("password", storage.get(context, Storage.PERMANENT, Storage.USER, "user#deifmo#password"));
        room101Client.post(context, "index.php", params, new ResponseHandlerJoiner(handler) {
            @Override
            public void onSuccess(int code, Client.Headers headers, String response) throws Exception {
                if (code == 302) {
                    log.v(TAG, "execute | scope=", scope, " | success | code=", code);
                    String location = headers.getValue("Location");
                    if (StringUtils.isBlank(location)) {
                        handler.onFailure(code, headers, Client.FAILED_EXPECTED_REDIRECTION);
                        return;
                    }
                    room101Client.get(context, location, null, handler);
                    return;
                }
                log.v(TAG, "execute | scope=", scope, " | success(not really) | code=", code);
                if (response != null && response.contains("Доступ запрещен")) {
                    if (response.contains("Неверный") && response.contains("логин/пароль")) {
                        handler.onFailure(code, headers, Client.FAILED_AUTH_CREDENTIALS_FAILED);
                        return;
                    }
                    handler.onFailure(code, headers, Client.FAILED_AUTH_REQUIRED);
                    return;
                }
                handler.onFailure(code, headers, Client.FAILED_EXPECTED_REDIRECTION);
            }
        });
    }

    @Override
    public void onDenyRequest(int reid, int status) {
        thread.runOnUI(R101, () -> {
            log.v(TAG, "onDenyRequest | reid=", reid, " | status=", status);
            if (App.OFFLINE_MODE) {
                log.v(TAG, "onDenyRequest rejected: offline mode");
                notificationMessage.snackBar(activity, R.id.room101_review_swipe, activity.getString(R.string.device_offline_action_refused));
                return;
            }
            if (activity.isFinishing() || activity.isDestroyed()) {
                return;
            }
            (new AlertDialog.Builder(activity)
                    .setTitle(R.string.request_deny)
                    .setMessage(activity.getString(R.string.request_deny_1) + "\n" + activity.getString(R.string.request_deny_2))
                    .setCancelable(true)
                    .setPositiveButton(R.string.delete, (dialog, which) -> {
                        thread.standalone(() -> denyRequest(reid, status));
                        dialog.cancel();
                    })
                    .setNegativeButton(R.string.cancel, (dialog, which) -> dialog.cancel())
                    .create()
            ).show();
        });
    }

    private void denyRequest(int reid, int status) {
        thread.assertNotUI();
        log.v(TAG, "denyRequest | reid=", reid, " | status=", status);
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
            public void onSuccess(int code, Client.Headers headers, String response) {
                if (code == 302) {
                    log.v(TAG, "denyRequest | reid=", reid, " | status=", status, " | success | code=", code);
                    load(true);
                    firebaseAnalyticsProvider.logEvent(
                            activity,
                            FirebaseAnalyticsProvider.Event.ROOM101_REQUEST_DENIED
                    );
                    staticUtil.lockOrientation(activity, false);
                    return;
                }
                log.v(TAG, "denyRequest | reid=", reid, " | status=", status, " | success(not really) | code=", code);
                onFailure(code, headers, Client.FAILED);
            }
            @Override
            public void onFailure(int code, Client.Headers headers, int state) {
                thread.runOnUI(R101, () -> {
                    log.v(TAG, "denyRequest | reid=", reid, " | status=", status, " | failure | code=", code, " | state=", state);
                    fragment.draw(R.layout.state_failed_button);
                    TextView message = fragment.container().findViewById(R.id.try_again_message);
                    if (message != null) {
                        message.setText(room101Client.getFailedMessage(activity, code, state));
                    }
                    View reload = fragment.container().findViewById(R.id.try_again_reload);
                    if (reload != null) {
                        reload.setOnClickListener(v -> {
                            thread.standalone(() -> denyRequest(reid, status));
                        });
                    }
                    staticUtil.lockOrientation(activity, false);
                }, throwable -> {
                    staticUtil.lockOrientation(activity, false);
                });
            }
            @Override
            public void onProgress(int state) {
                thread.runOnUI(R101, () -> {
                    log.v(TAG, "denyRequest | reid=", reid, " | status=", status, " | progress | state=", state);
                    fragment.draw(R.layout.state_loading_text);
                    TextView message = fragment.container().findViewById(R.id.loading_message);
                    if (message != null) {
                        if (state == Client.STATE_HANDLING) {
                            message.setText(R.string.deny_request);
                            return;
                        }
                        message.setText(room101Client.getProgressMessage(activity, state));
                    }
                });
            }
            @Override
            public void onNewRequest(Client.Request request) {
                requestHandle = request;
            }
        });
    }

    private void addRequest() {
        thread.runOnUI(R101, () -> {
            log.v(TAG, "addRequest");
            if (App.OFFLINE_MODE) {
                log.v(TAG, "addRequest rejected: offline mode");
                notificationMessage.snackBar(activity, R.id.room101_review_swipe, activity.getString(R.string.device_offline_action_refused));
                return;
            }
            fragment.draw(R.layout.layout_room101_add_request);
            staticUtil.lockOrientation(activity, true);
            View close = fragment.container().findViewById(R.id.room101_close_add_request);
            LinearLayout back = fragment.container().findViewById(R.id.room101_back);
            LinearLayout forward = fragment.container().findViewById(R.id.room101_forward);
            TextView backText = fragment.container().findViewById(R.id.room101_back_text);
            TextView forwardText = fragment.container().findViewById(R.id.room101_forward_text);
            ProgressBar progressBar = fragment.container().findViewById(R.id.room101_progress_bar);
            close.setOnClickListener(v -> thread.standalone(() -> {
                log.v(TAG, "room101_close_add_request clicked");
                room101AddRequest.close(false);
            }));
            back.setOnClickListener(v -> thread.run(R101, () -> {
                log.v(TAG, "room101_back clicked");
                room101AddRequest.back();
            }));
            forward.setOnClickListener(v -> thread.run(R101, () -> {
                log.v(TAG, "room101_forward clicked");
                room101AddRequest.forward();
            }));
            thread.run(R101, () -> {
                room101AddRequest.start(activity, new Room101AddRequest.Callback() {
                    @Override
                    public void onProgress(@Room101AddRequest.Stage int stage) {
                        thread.runOnUI(R101, () -> {
                            progressBar.setProgress((stage * 100) / Room101AddRequest.STAGE_TOTAL);
                            back.setAlpha(1f);
                            forward.setAlpha(1f);
                            switch (stage) {
                                case Room101AddRequest.STAGE_PICK_DATE_LOAD:
                                case Room101AddRequest.STAGE_PICK_TIME_START_LOAD:
                                case Room101AddRequest.STAGE_PICK_TIME_END_LOAD:
                                case Room101AddRequest.STAGE_PICK_CONFIRMATION_LOAD:
                                case Room101AddRequest.STAGE_PICK_CREATE:
                                    back.setAlpha(0.2f);
                                    forward.setAlpha(0.2f);
                                    if (stage == Room101AddRequest.STAGE_PICK_DATE_LOAD) {
                                        backText.setText(R.string.do_cancel);
                                        forwardText.setText(R.string.next);
                                    }
                                    if (stage == Room101AddRequest.STAGE_PICK_TIME_START_LOAD) {
                                        backText.setText(R.string.back);
                                    }
                                    break;
                                case Room101AddRequest.STAGE_PICK_DATE:
                                    backText.setText(R.string.do_cancel);
                                    forwardText.setText(R.string.next);
                                    break;
                                case Room101AddRequest.STAGE_PICK_TIME_START:
                                case Room101AddRequest.STAGE_PICK_TIME_END:
                                    backText.setText(R.string.back);
                                    forwardText.setText(R.string.next);
                                    break;
                                case Room101AddRequest.STAGE_PICK_CONFIRMATION:
                                    backText.setText(R.string.back);
                                    forwardText.setText(R.string.create);
                                    break;
                                case Room101AddRequest.STAGE_PICK_DONE:
                                    back.setAlpha(0.0f);
                                    backText.setText(R.string.close);
                                    forwardText.setText(R.string.close);
                                    break;
                            }
                        }, throwable -> {
                            log.exception(throwable);
                            notificationMessage.snackBar(activity, R.id.room101_review_swipe, activity.getString(R.string.error_occurred_while_room101_request));
                            load(false);
                        });
                    }
                    @Override
                    public void onDraw(View view) {
                        thread.runOnUI(R101, () -> {
                            if (view == null) {
                                notificationMessage.snackBar(activity, R.id.room101_review_swipe, activity.getString(R.string.error_occurred));
                                load(false);
                                return;
                            }
                            ViewGroup vg = fragment.container().findViewById(R.id.room101_add_request_container);
                            if (vg != null) {
                                vg.removeAllViews();
                                vg.addView(view);
                            }
                        }, throwable -> {
                            log.exception(throwable);
                            notificationMessage.snackBar(activity, R.id.room101_review_swipe, activity.getString(R.string.error_occurred));
                            load(false);
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
            }, throwable -> {
                loadFailed();
            });
        }, throwable -> {
            loadFailed();
        });
    }

    protected void load() {
        thread.run(R101, () -> {
            load(storagePref.get(activity, "pref_use_cache", true) ?
                    Integer.parseInt(storagePref.get(activity, "pref_dynamic_refresh", "0")) :
                    0);
        });
    }

    private void load(int refresh_rate) {
        thread.run(R101, () -> {
            log.v(TAG, "load | refresh_rate=" + refresh_rate);
            if (!storagePref.get(activity, "pref_use_cache", true)) {
                load(false);
                return;
            }
            Room101Requests cache = getFromCache();
            if (cache == null) {
                load(true, null);
                return;
            }
            setData(cache);
            if (cache.getTimestamp() + refresh_rate * 3600000L < time.getTimeInMillis()) {
                load(true, cache);
            } else {
                load(false, cache);
            }
        }, throwable -> {
            loadFailed();
        });
    }

    private void load(boolean force) {
        thread.run(R101, () -> load(force, null));
    }

    private void load(boolean force, Room101Requests cached) {
        thread.run(R101, () -> {
            log.v(TAG, "load | force=" + (force ? "true" : "false"));
            if ((!force || Client.isOffline(activity)) && storagePref.get(activity, "pref_use_cache", true)) {
                try {
                    Room101Requests cache = cached == null ? getFromCache() : cached;
                    if (cache != null) {
                        log.v(TAG, "load | from cache");
                        setData(cache);
                        display();
                        return;
                    }
                } catch (Exception e) {
                    log.v(TAG, "load | failed to load from cache");
                }
            }
            if (App.OFFLINE_MODE) {
                if (getData() != null) {
                    display();
                    return;
                }
                thread.runOnUI(R101, () -> {
                    fragment.draw(R.layout.state_offline_text);
                    View reload = fragment.container().findViewById(R.id.offline_reload);
                    if (reload != null) {
                        reload.setOnClickListener(v -> load());
                    }
                });
                return;
            }
            execute(activity, "delRequest", new ResponseHandler() {
                @Override
                public void onSuccess(int code, Client.Headers headers, String response) throws Exception {
                    log.v(TAG, "load | success | code=" + code);
                    if (code == 200) {
                        Room101Requests room101Requests = new Room101RequestsParser(response).parse();
                        if (room101Requests == null) {
                            if (getData() != null) {
                                display();
                                return;
                            }
                            loadFailed();
                            return;
                        }
                        room101Requests.setTimestamp(time.getTimeInMillis());
                        putToCache(room101Requests);
                        setData(room101Requests);
                        display();
                        return;
                    }
                    if (getData() != null) {
                        display();
                        return;
                    }
                    loadFailed();
                }
                @Override
                public void onFailure(int code, Client.Headers headers, int state) {
                    thread.run(R101, () -> {
                        log.v(TAG, "load | failure | code=", code, " | state=", state);
                        if (state == Client.FAILED_OFFLINE) {
                            if (getData() != null) {
                                display();
                                return;
                            }
                            thread.runOnUI(R101, () -> {
                                fragment.draw(R.layout.state_offline_text);
                                View reload = fragment.container().findViewById(R.id.offline_reload);
                                if (reload != null) {
                                    reload.setOnClickListener(v -> load(true));
                                }
                            });
                            return;
                        }
                        if (room101Client.isFailedAuth(state)) {
                            thread.runOnUI(R101, () -> {
                                fragment.draw(R.layout.state_failed_button);
                                TextView message = fragment.container().findViewById(R.id.try_again_message);
                                if (message != null) {
                                    message.setText(activity.getString(R.string.room101_auth_failed) + "\n" +
                                            room101Client.getFailedMessage(activity, code, state));
                                }
                                View reload = fragment.container().findViewById(R.id.try_again_reload);
                                if (reload != null) {
                                    ((ViewGroup) reload.getParent()).removeView(reload);
                                }
                            });
                            return;
                        }
                        thread.runOnUI(R101, () -> {
                            fragment.draw(R.layout.state_failed_button);
                            TextView message = fragment.container().findViewById(R.id.try_again_message);
                            if (message != null) {
                                if (state == Client.FAILED_EXPECTED_REDIRECTION) {
                                    message.setText(R.string.wrong_response_from_server);
                                } else {
                                    message.setText(room101Client.getFailedMessage(activity, code, state));
                                }
                            }
                            View reload = fragment.container().findViewById(R.id.try_again_reload);
                            if (reload != null) {
                                reload.setOnClickListener(v -> load(true));
                            }
                        });
                    }, throwable -> {
                        loadFailed();
                    });
                }
                @Override
                public void onProgress(int state) {
                    thread.runOnUI(R101, () -> {
                        log.v(TAG, "load | progress | state=", state);
                        fragment.draw(R.layout.state_loading_text);
                        TextView message = fragment.container().findViewById(R.id.loading_message);
                        if (message != null) {
                            message.setText(room101Client.getProgressMessage(activity, state));
                        }
                    });
                }
                @Override
                public void onNewRequest(Client.Request request) {
                    requestHandle = request;
                }
            });
        }, throwable -> {
            loadFailed();
        });
    }

    private void loadFailed() {
        thread.runOnUI(R101, () -> {
            log.v(TAG, "loadFailed");
            fragment.draw(R.layout.state_failed_button);
            View reload = fragment.container().findViewById(R.id.try_again_reload);
            if (reload != null) {
                reload.setOnClickListener(v -> load(true));
            }
        }, throwable -> {
            log.exception(throwable);
        });
    }

    protected void display() {
        thread.run(R101, () -> {
            log.v(TAG, "display");
            Room101Requests data = getData();
            if (data == null) {
                loadFailed();
                return;
            }
            thread.runOnUI(R101, () -> {
                fragment.draw(R.layout.layout_room101_review);
                TextView limit = fragment.container().findViewById(R.id.room101_limit);
                if (limit != null) {
                    limit.setText(data.getLimit());
                }
                TextView left = fragment.container().findViewById(R.id.room101_last);
                if (left != null) {
                    left.setText(data.getLeft());
                }
                TextView penalty = fragment.container().findViewById(R.id.room101_penalty);
                if (penalty != null) {
                    penalty.setText(data.getPenalty());
                }
                // работаем со свайпом
                SwipeRefreshLayout swipe = fragment.container().findViewById(R.id.room101_review_swipe);
                if (swipe != null) {
                    swipe.setColorSchemeColors(Color.resolve(activity, R.attr.colorAccent));
                    swipe.setProgressBackgroundColorSchemeColor(Color.resolve(activity, R.attr.colorBackgroundRefresh));
                    swipe.setOnRefreshListener(this);
                }
                // плавающая кнопка
                FloatingActionButton fab = fragment.container().findViewById(R.id.fab);
                if (fab != null) {
                    fab.setOnClickListener(v -> {
                        log.v(TAG, "fab button clicked");
                        addRequest();
                    });
                }
                // update time
                notificationMessage.showUpdateTime(activity, R.id.room101_review_swipe, data.getTimestamp());
                // view builder
                LinearLayout reviewContainer = fragment.container().findViewById(R.id.room101_review_container);
                thread.run(R101, new Room101ReviewBuilder(activity, this, data, (state, layout) -> {
                    thread.runOnUI(R101, () -> {
                        if (reviewContainer != null && layout != null) {
                            reviewContainer.removeAllViews();
                            reviewContainer.addView(layout);
                            return;
                        }
                        loadFailed();
                    }, throwable -> {
                        log.exception(throwable);
                        loadFailed();
                    });
                }), throwable -> {
                    log.exception(throwable);
                    loadFailed();
                });
            }, throwable -> {
                log.exception(throwable);
                loadFailed();
            });
        }, throwable -> {
            log.exception(throwable);
            loadFailed();
        });
    }

    @Override
    protected String getLogTag() {
        return TAG;
    }

    @Override
    protected String getCacheType() {
        return Storage.USER;
    }

    @Override
    protected String getCachePath() {
        return "room101#core";
    }

    @Override
    protected String getThreadToken() {
        return R101;
    }
}

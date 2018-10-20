package com.bukhmastov.cdoitmo.fragment.presenter.impl;

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
import com.bukhmastov.cdoitmo.activity.ConnectedActivity;
import com.bukhmastov.cdoitmo.builder.Room101ReviewBuilder;
import com.bukhmastov.cdoitmo.event.bus.EventBus;
import com.bukhmastov.cdoitmo.event.bus.annotation.Event;
import com.bukhmastov.cdoitmo.event.events.ClearCacheEvent;
import com.bukhmastov.cdoitmo.factory.AppComponentProvider;
import com.bukhmastov.cdoitmo.firebase.FirebaseAnalyticsProvider;
import com.bukhmastov.cdoitmo.fragment.ConnectedFragment;
import com.bukhmastov.cdoitmo.fragment.presenter.Room101FragmentPresenter;
import com.bukhmastov.cdoitmo.function.ThrowingConsumer;
import com.bukhmastov.cdoitmo.function.ThrowingRunnable;
import com.bukhmastov.cdoitmo.model.parser.Room101RequestsParser;
import com.bukhmastov.cdoitmo.model.room101.requests.Room101Requests;
import com.bukhmastov.cdoitmo.network.Room101Client;
import com.bukhmastov.cdoitmo.network.handlers.ResponseHandler;
import com.bukhmastov.cdoitmo.network.model.Client;
import com.bukhmastov.cdoitmo.network.model.Room101;
import com.bukhmastov.cdoitmo.object.Room101AddRequest;
import com.bukhmastov.cdoitmo.provider.InjectProvider;
import com.bukhmastov.cdoitmo.util.Log;
import com.bukhmastov.cdoitmo.util.NotificationMessage;
import com.bukhmastov.cdoitmo.util.Static;
import com.bukhmastov.cdoitmo.util.Storage;
import com.bukhmastov.cdoitmo.util.StoragePref;
import com.bukhmastov.cdoitmo.util.TextUtils;
import com.bukhmastov.cdoitmo.util.Thread;
import com.bukhmastov.cdoitmo.util.Time;
import com.bukhmastov.cdoitmo.util.singleton.Color;
import com.bukhmastov.cdoitmo.util.singleton.StringUtils;

import java.util.HashMap;

import javax.inject.Inject;

public class Room101FragmentPresenterImpl implements Room101FragmentPresenter, SwipeRefreshLayout.OnRefreshListener, Room101ReviewBuilder.Request {

    private static final String TAG = "Room101Fragment";
    private ConnectedFragment fragment = null;
    private ConnectedActivity activity = null;
    private Room101Requests data = null;
    private boolean loaded = false;
    public static Client.Request requestHandle = null;
    private String actionExtra = null;
    protected boolean forbidden = false;

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

    public Room101FragmentPresenterImpl() {
        AppComponentProvider.getComponent().inject(this);
        eventBus.register(this);
    }

    @Event
    public void onClearCacheEvent(ClearCacheEvent event) {
        if (event.isNot(ClearCacheEvent.ROOM101)) {
            return;
        }
        data = null;
        fragment.clearData(fragment);
    }

    @Override
    public void setFragment(ConnectedFragment fragment) {
        this.fragment = fragment;
        this.activity = fragment.activity();
    }

    @Override
    public void onCreate(@Nullable Bundle savedInstanceState) {
        thread.run(() -> {
            log.v(TAG, "Fragment created");
            if (App.UNAUTHORIZED_MODE) {
                forbidden = true;
                log.w(TAG, "Fragment created | UNAUTHORIZED_MODE not allowed, closing fragment...");
                thread.runOnUI(() -> fragment.close());
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
    public void onDestroy() {
        thread.run(() -> {
            log.v(TAG, "Fragment destroyed");
            loaded = false;
        });
    }

    @Override
    public void onResume() {
        thread.run(() -> {
            log.v(TAG, "Fragment resumed");
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
            if (loaded) {
                return;
            }
            loaded = true;
            if (getData() == null) {
                load();
            } else {
                display();
            }
        });
    }

    @Override
    public void onPause() {
        thread.run(() -> {
            log.v(TAG, "paused");
            if (requestHandle != null && requestHandle.cancel()) {
                loaded = false;
            }
        });
    }

    @Override
    public void onRefresh() {
        thread.run(() -> {
            log.v(TAG, "refreshing");
            load(true);
        });
    }

    @Override
    public void execute(final Context context, final String scope, final ResponseHandler responseHandler) {
        thread.run(Thread.BACKGROUND, () -> {
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
                    injectProvider.getThread().run(new ThrowingRunnable() {
                        @Override
                        public void run() {
                            if (statusCode == 302) {
                                injectProvider.getLog().v(TAG, "execute | scope=" + scope + " | success | statusCode=" + statusCode);
                                String location = headers.getValue("Location");
                                if (StringUtils.isBlank(location)) {
                                    responseHandler.onFailure(statusCode, headers, Room101.FAILED_EXPECTED_REDIRECTION);
                                    return;
                                }
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
                                return;
                            }
                            injectProvider.getLog().v(TAG, "execute | scope=" + scope + " | success(not really) | statusCode=" + statusCode);
                            if (response != null && (response.contains("Доступ запрещен") || (response.contains("Неверный") && response.contains("логин/пароль")))) {
                                responseHandler.onFailure(statusCode, headers, Room101.FAILED_AUTH);
                            } else {
                                responseHandler.onFailure(statusCode, headers, Room101.FAILED_EXPECTED_REDIRECTION);
                            }
                        }
                    }, new ThrowingConsumer<Throwable, Throwable>() {
                        @Override
                        public void accept(Throwable throwable) {
                            injectProvider.getLog().v(TAG, "execute | scope=" + scope + " | catch | throwable=" + throwable.getMessage());
                            responseHandler.onFailure(statusCode, headers, Room101.FAILED_TRY_AGAIN);
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

    @Override
    public void onDenyRequest(final int reid, final int status) {
        thread.runOnUI(() -> {
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
                        denyRequest(reid, status);
                        dialog.cancel();
                    })
                    .setNegativeButton(R.string.cancel, (dialog, which) -> dialog.cancel())
                    .create()
            ).show();
        });
    }

    private void denyRequest(final int reid, final int status) {
        thread.run(() -> {
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
                public void onSuccess(final int statusCode, final Client.Headers headers, final String response) {
                    thread.runOnUI(() -> {
                        if (statusCode == 302) {
                            log.v(TAG, "denyRequest | reid=", reid, " | status=", status, " | success | statusCode=", statusCode);
                            load(true);
                            firebaseAnalyticsProvider.logEvent(
                                    activity,
                                    FirebaseAnalyticsProvider.Event.ROOM101_REQUEST_DENIED
                            );
                        } else {
                            log.v(TAG, "denyRequest | reid=", reid, " | status=", status, " | success(not really) | statusCode=", statusCode);
                            onFailure(statusCode, headers, Room101Client.FAILED_TRY_AGAIN);
                        }
                        staticUtil.lockOrientation(activity, false);
                    }, throwable -> {
                        staticUtil.lockOrientation(activity, false);
                    });
                }
                @Override
                public void onFailure(final int statusCode, final Client.Headers headers, final int state) {
                    thread.runOnUI(() -> {
                        log.v(TAG, "denyRequest | reid=", reid, " | status=", status, " | failure | statusCode=", statusCode);
                        fragment.draw(R.layout.state_failed_button);
                        TextView message = fragment.container().findViewById(R.id.try_again_message);
                        if (message != null) {
                            if (state == Room101Client.FAILED_SERVER_ERROR) {
                                message.setText(Room101Client.getFailureMessage(activity, statusCode));
                            } else {
                                message.setText(R.string.wrong_response_from_server);
                            }
                        }
                        View reload = fragment.container().findViewById(R.id.try_again_reload);
                        if (reload != null) {
                            reload.setOnClickListener(v -> denyRequest(reid, status));
                        }
                        staticUtil.lockOrientation(activity, false);
                    }, throwable -> {
                        staticUtil.lockOrientation(activity, false);
                    });
                }
                @Override
                public void onProgress(final int state) {
                    thread.runOnUI(() -> {
                        log.v(TAG, "denyRequest | reid=", reid, " | status=", status, " | progress ", state);
                        fragment.draw(R.layout.state_loading_text);
                        TextView message = fragment.container().findViewById(R.id.loading_message);
                        if (message != null) {
                            switch (state) {
                                case Room101Client.STATE_HANDLING: message.setText(R.string.deny_request); break;
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
            close.setOnClickListener(v -> {
                log.v(TAG, "room101_close_add_request clicked");
                room101AddRequest.close(false);
            });
            back.setOnClickListener(v -> {
                log.v(TAG, "room101_back clicked");
                room101AddRequest.back();
            });
            forward.setOnClickListener(v -> {
                log.v(TAG, "room101_forward clicked");
                room101AddRequest.forward();
            });
            room101AddRequest.start(activity, new Room101AddRequest.Callback() {
                @Override
                public void onProgress(@Room101AddRequest.Stage final int stage) {
                    thread.runOnUI(() -> {
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
                public void onDraw(final View view) {
                    thread.runOnUI(() -> {
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
    }

    private void load() {
        thread.run(() -> load(storagePref.get(activity, "pref_use_cache", true) ? Integer.parseInt(storagePref.get(activity, "pref_dynamic_refresh", "0")) : 0));
    }

    private void load(final int refresh_rate) {
        thread.run(() -> {
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

    private void load(final boolean force) {
        thread.run(() -> load(force, null));
    }

    private void load(final boolean force, final Room101Requests cached) {
        thread.run(() -> {
            log.v(TAG, "load | force=" + (force ? "true" : "false"));
            if ((!force || !Client.isOnline(activity)) && storagePref.get(activity, "pref_use_cache", true)) {
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
                thread.runOnUI(() -> {
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
                public void onSuccess(final int statusCode, final Client.Headers headers, final String response) {
                    thread.run(() -> {
                        log.v(TAG, "load | success | statusCode=" + statusCode);
                        if (statusCode == 200) {
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
                            if (storagePref.get(activity, "pref_use_cache", true)) {
                                storage.put(activity, Storage.CACHE, Storage.USER, "room101#core", room101Requests.toJsonString());
                            }
                            setData(room101Requests);
                            display();
                            return;
                        }
                        if (getData() != null) {
                            display();
                            return;
                        }
                        loadFailed();
                    }, throwable -> {
                        loadFailed();
                    });
                }
                @Override
                public void onFailure(final int statusCode, final Client.Headers headers, final int state) {
                    thread.run(() -> {
                        log.v(TAG, "load | failure " + state);
                        switch (state) {
                            case Room101Client.FAILED_OFFLINE:
                                if (getData() != null) {
                                    display();
                                    return;
                                }
                                thread.runOnUI(() -> {
                                    fragment.draw(R.layout.state_offline_text);
                                    View reload = fragment.container().findViewById(R.id.offline_reload);
                                    if (reload != null) {
                                        reload.setOnClickListener(v -> load(true));
                                    }
                                });
                                break;
                            case Room101Client.FAILED_SERVER_ERROR:
                            case Room101Client.FAILED_TRY_AGAIN:
                            case Room101Client.FAILED_EXPECTED_REDIRECTION:
                                thread.runOnUI(() -> {
                                    fragment.draw(R.layout.state_failed_button);
                                    if (state == Room101Client.FAILED_EXPECTED_REDIRECTION) {
                                        TextView message = fragment.container().findViewById(R.id.try_again_message);
                                        if (message != null) {
                                            message.setText(R.string.wrong_response_from_server);
                                        }
                                    }
                                    if (state == Room101Client.FAILED_SERVER_ERROR) {
                                        TextView message = fragment.container().findViewById(R.id.try_again_message);
                                        if (message != null) {
                                            if (activity != null) {
                                                message.setText(Room101Client.getFailureMessage(activity, statusCode));
                                            } else {
                                                message.setText(Room101Client.getFailureMessage(statusCode));
                                            }
                                        }
                                    }
                                    View reload = fragment.container().findViewById(R.id.try_again_reload);
                                    if (reload != null) {
                                        reload.setOnClickListener(v -> load(true));
                                    }
                                });

                                break;
                            case Room101Client.FAILED_AUTH:
                                thread.runOnUI(() -> {
                                    fragment.draw(R.layout.state_failed_button);
                                    TextView message = fragment.container().findViewById(R.id.try_again_message);
                                    if (message != null) {
                                        message.setText(R.string.room101_auth_failed);
                                    }
                                    View reload = fragment.container().findViewById(R.id.try_again_reload);
                                    if (reload != null) {
                                        ((ViewGroup) reload.getParent()).removeView(reload);
                                    }
                                });
                                break;
                        }
                    }, throwable -> {
                        loadFailed();
                    });
                }
                @Override
                public void onProgress(final int state) {
                    thread.runOnUI(() -> {
                        log.v(TAG, "load | progress " + state);
                        fragment.draw(R.layout.state_loading_text);
                        TextView message = fragment.container().findViewById(R.id.loading_message);
                        if (message != null) {
                            switch (state) {
                                case Room101Client.STATE_HANDLING:
                                    message.setText(R.string.loading);
                                    break;
                            }
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
        thread.runOnUI(() -> {
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

    private void display() {
        thread.run(() -> {
            log.v(TAG, "display");
            Room101Requests data = getData();
            if (data == null) {
                loadFailed();
                return;
            }
            thread.runOnUI(() -> {
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
                thread.run(new Room101ReviewBuilder(activity, this, data, (state, layout) -> {
                    thread.runOnUI(() -> {
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

    private Room101Requests getFromCache() {
        thread.assertNotUI();
        String cache = storage.get(activity, Storage.CACHE, Storage.USER, "room101#core").trim();
        if (StringUtils.isBlank(cache)) {
            return null;
        }
        try {
            return new Room101Requests().fromJsonString(cache);
        } catch (Exception e) {
            storage.delete(activity, Storage.CACHE, Storage.USER, "room101#core");
            return null;
        }
    }

    private void setData(Room101Requests data) {
        thread.assertNotUI();
        try {
            this.data = data;
            fragment.storeData(fragment, data.toJsonString());
        } catch (Exception e) {
            log.exception(e);
        }
    }

    private Room101Requests getData() {
        thread.assertNotUI();
        if (data != null) {
            return data;
        }
        try {
            String stored = fragment.restoreData(fragment);
            if (stored != null && !stored.isEmpty()) {
                data = new Room101Requests().fromJsonString(stored);
                return data;
            }
        } catch (Exception e) {
            log.exception(e);
        }
        return null;
    }
}

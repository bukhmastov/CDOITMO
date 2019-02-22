package com.bukhmastov.cdoitmo.fragment.presenter.impl;

import android.content.Intent;
import android.net.Uri;
import android.os.Bundle;
import android.view.View;
import android.widget.PopupMenu;
import android.widget.TextView;

import com.bukhmastov.cdoitmo.R;
import com.bukhmastov.cdoitmo.activity.ConnectedActivity;
import com.bukhmastov.cdoitmo.event.bus.EventBus;
import com.bukhmastov.cdoitmo.event.events.OpenIntentEvent;
import com.bukhmastov.cdoitmo.factory.AppComponentProvider;
import com.bukhmastov.cdoitmo.firebase.FirebaseAnalyticsProvider;
import com.bukhmastov.cdoitmo.fragment.LinkAccountFragment;
import com.bukhmastov.cdoitmo.fragment.presenter.LinkAccountFragmentPresenter;
import com.bukhmastov.cdoitmo.fragment.presenter.LinkedAccountsFragmentPresenter;
import com.bukhmastov.cdoitmo.network.IsuPrivateRestClient;
import com.bukhmastov.cdoitmo.network.handlers.ResponseHandler;
import com.bukhmastov.cdoitmo.network.model.Client;
import com.bukhmastov.cdoitmo.network.model.Isu;
import com.bukhmastov.cdoitmo.util.Log;
import com.bukhmastov.cdoitmo.util.NotificationMessage;
import com.bukhmastov.cdoitmo.util.Storage;
import com.bukhmastov.cdoitmo.util.Thread;

import javax.inject.Inject;

public class LinkedAccountsFragmentPresenterImpl extends ConnectedFragmentPresenterImpl
        implements LinkedAccountsFragmentPresenter {

    private static final String TAG = "LinkedAccountsFragment";
    
    @Inject
    Log log;
    @Inject
    Thread thread;
    @Inject
    EventBus eventBus;
    @Inject
    IsuPrivateRestClient isuPrivateRestClient;
    @Inject
    Storage storage;
    @Inject
    NotificationMessage notificationMessage;
    @Inject
    FirebaseAnalyticsProvider firebaseAnalyticsProvider;
    
    public LinkedAccountsFragmentPresenterImpl() {
        super();
        AppComponentProvider.getComponent().inject(this);
    }

    @Override
    public void onResume() {
        log.v(TAG, "Fragment resumed");
        firebaseAnalyticsProvider.setCurrentScreen(activity, fragment);
        initDeIfmo();
        initIsu();
    }

    private void initDeIfmo() {
        thread.runOnUI(() -> {
            View accountCdoLink = fragment.container().findViewById(R.id.account_cdo_link);
            View accountCdoInfo = fragment.container().findViewById(R.id.account_cdo_info);
            if (accountCdoLink != null) {
                accountCdoLink.setOnClickListener(v -> thread.standalone(() -> {
                    log.v(TAG, "accountCdoLink clicked");
                    eventBus.fire(new OpenIntentEvent(new Intent(Intent.ACTION_VIEW, Uri.parse("http://de.ifmo.ru"))));
                }));
            }
            thread.standalone(() -> {
                String login = storage.get(activity, Storage.PERMANENT, Storage.USER, "user#deifmo#login", "").trim();
                String name = storage.get(activity, Storage.PERMANENT, Storage.USER, "user#name", "").trim();
                thread.runOnUI(() -> {
                    if (accountCdoInfo != null) {
                        ((TextView) accountCdoInfo).setText(login + " (" + name + ")");
                    }
                });
            });
        });
    }

    private void initIsu() {
        thread.standalone(() -> {
            boolean authorized = isuPrivateRestClient.isAuthorized(activity);
            thread.runOnUI(() -> {
                View accountIsuLoading = activity.findViewById(R.id.account_isu_loading);
                View accountIsuContent = activity.findViewById(R.id.account_isu_content);
                TextView accountIsuInfo = activity.findViewById(R.id.account_isu_info);
                accountIsuLoading.setVisibility(View.VISIBLE);
                accountIsuContent.setVisibility(View.GONE);
                accountIsuInfo.setText(authorized ? R.string.authorized : R.string.not_authorized);
                accountIsuLoading.setVisibility(View.GONE);
                accountIsuContent.setVisibility(View.VISIBLE);
                accountIsuContent.setOnClickListener(v -> thread.runOnUI(() -> {
                    if (!authorized) {
                        Bundle extras = new Bundle();
                        extras.putString("type", LinkAccountFragmentPresenter.ISU);
                        if (!activity.openFragment(ConnectedActivity.TYPE.STACKABLE, LinkAccountFragment.class, extras)) {
                            notificationMessage.snackBar(activity, activity.getString(R.string.failed_to_open_fragment));
                        }
                        return;
                    }
                    PopupMenu popup = new PopupMenu(activity, v);
                    popup.inflate(R.menu.linked_accounts_logout);
                    popup.setOnMenuItemClickListener(item -> {
                        switch (item.getItemId()) {
                            case R.id.logout: isuLogout(); break;
                            case R.id.update_connection: isuLogin(); break;
                        }
                        popup.dismiss();
                        return true;
                    });
                    popup.show();
                }));
            });
        });
    }

    private void isuLogin() {
        thread.standalone(() -> isuPrivateRestClient.authorize(activity, new ResponseHandler() {
            @Override
            public void onSuccess(int statusCode, Client.Headers headers, String response) {
                thread.runOnUI(() -> {
                    log.v(TAG, "isuLogin | success | statusCode=", statusCode, " | response=", response);
                    if ("authorized".equals(response)) {
                        notificationMessage.snackBar(activity, activity.getString(R.string.authorized));
                        initIsu();
                    } else {
                        notificationMessage.snackBar(activity, activity.getString(R.string.auth_failed));
                    }
                });
            }
            @Override
            public void onFailure(final int statusCode, final Client.Headers headers, final int state) {
                thread.runOnUI(() -> {
                    log.v(TAG, "isuLogin | failure | state=", state, " | statusCode=", statusCode);
                    switch (state) {
                        case Isu.FAILED_OFFLINE:
                            notificationMessage.snackBar(activity, activity.getString(R.string.device_offline_action_refused));
                            break;
                        case Isu.FAILED_SERVER_ERROR:
                            notificationMessage.snackBar(activity, activity.getString(R.string.auth_failed) + ". " + Isu.getFailureMessage(activity, statusCode));
                            break;
                        case Isu.FAILED_TRY_AGAIN:
                        case Isu.FAILED_AUTH_TRY_AGAIN:
                            notificationMessage.snackBar(activity, activity.getString(R.string.auth_failed));
                            break;
                        case Isu.FAILED_AUTH_CREDENTIALS_REQUIRED:
                            notificationMessage.snackBar(activity, activity.getString(R.string.required_login_password));
                            break;
                        case Isu.FAILED_AUTH_CREDENTIALS_FAILED:
                            notificationMessage.snackBar(activity, activity.getString(R.string.invalid_login_password));
                            break;
                    }
                });
            }
            @Override
            public void onProgress(final int state) {
                log.v(TAG, "isuLogin | progress | state=", state);
            }
            @Override
            public void onNewRequest(Client.Request request) {
                if (requestHandle != null) {
                    requestHandle.cancel();
                }
                requestHandle = request;
            }
        }));
    }

    private void isuLogout() {
        thread.standalone(() -> {
            storage.delete(activity, Storage.PERMANENT, Storage.USER, "user#isu#access_token");
            storage.delete(activity, Storage.PERMANENT, Storage.USER, "user#isu#refresh_token");
            storage.delete(activity, Storage.PERMANENT, Storage.USER, "user#isu#expires_at");
            firebaseAnalyticsProvider.logEvent(activity, FirebaseAnalyticsProvider.Event.LOGOUT_ISU);
            initIsu();
        });
    }

    @Override
    protected String getLogTag() {
        return TAG;
    }
}

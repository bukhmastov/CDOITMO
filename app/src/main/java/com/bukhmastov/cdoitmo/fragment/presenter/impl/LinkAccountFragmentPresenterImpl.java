package com.bukhmastov.cdoitmo.fragment.presenter.impl;

import android.os.Bundle;
import android.view.View;
import android.widget.Button;
import android.widget.EditText;
import android.widget.TextView;

import com.bukhmastov.cdoitmo.R;
import com.bukhmastov.cdoitmo.factory.AppComponentProvider;
import com.bukhmastov.cdoitmo.firebase.FirebaseAnalyticsProvider;
import com.bukhmastov.cdoitmo.fragment.presenter.LinkAccountFragmentPresenter;
import com.bukhmastov.cdoitmo.network.IsuPrivateRestClient;
import com.bukhmastov.cdoitmo.network.handlers.ResponseHandler;
import com.bukhmastov.cdoitmo.network.model.Client;
import com.bukhmastov.cdoitmo.network.model.Isu;
import com.bukhmastov.cdoitmo.util.Log;
import com.bukhmastov.cdoitmo.util.NotificationMessage;
import com.bukhmastov.cdoitmo.util.Storage;
import com.bukhmastov.cdoitmo.util.Thread;
import com.bukhmastov.cdoitmo.util.singleton.StringUtils;

import javax.inject.Inject;

public class LinkAccountFragmentPresenterImpl extends ConnectedFragmentPresenterImpl
        implements LinkAccountFragmentPresenter {

    private static final String TAG = "LinkAccountFragment";
    private @Type String type = null;
    private View linkAccountForm = null;
    private View linkAccountProgress = null;

    @Inject
    Log log;
    @Inject
    Thread thread;
    @Inject
    Storage storage;
    @Inject
    IsuPrivateRestClient isuPrivateRestClient;
    @Inject
    NotificationMessage notificationMessage;
    @Inject
    FirebaseAnalyticsProvider firebaseAnalyticsProvider;

    public LinkAccountFragmentPresenterImpl() {
        super();
        AppComponentProvider.getComponent().inject(this);
    }

    @Override
    public void onViewCreated() {
        thread.runOnUI(() -> {
            Bundle extras = fragment.getArguments();
            if (extras == null) {
                log.w(TAG, "onViewCreated | extras are null");
                fragment.close();
                return;
            }
            if (!extras.containsKey("type")) {
                log.w(TAG, "onViewCreated | extras does not contain 'type'");
                fragment.close();
                return;
            }
            type = extras.getString("type");
            if (StringUtils.isBlank(type)) {
                log.w(TAG, "onViewCreated | type is blank");
                fragment.close();
                return;
            }
            switch (type) {
                case ISU: break;
                default: {
                    log.w(TAG, "onViewCreated | wrong 'type' provided: ", type);
                    fragment.close();
                    return;
                }
            }
            // Setup components
            TextView header = activity.findViewById(R.id.header);
            Button loginBtn = activity.findViewById(R.id.login);
            EditText inputLogin = activity.findViewById(R.id.input_login);
            EditText inputPassword = activity.findViewById(R.id.input_password);
            linkAccountForm = activity.findViewById(R.id.link_account_form);
            linkAccountProgress = activity.findViewById(R.id.link_account_progress);
            linkAccountForm.setVisibility(View.VISIBLE);
            linkAccountProgress.setVisibility(View.GONE);
            String text = activity.getString(R.string.link_account_with) + " ";
            switch (type) {
                case ISU: text += activity.getString(R.string.isu_ifmo); break;
            }
            if (header != null) {
                header.setText(text);
            }
            if (loginBtn != null) {
                loginBtn.setOnClickListener(view -> {
                    log.v(TAG, "login clicked");
                    String login = "";
                    String password = "";
                    if (inputLogin != null) {
                        login = inputLogin.getText().toString();
                    }
                    if (inputPassword != null) {
                        password = inputPassword.getText().toString();
                    }
                    auth(type, login, password);
                });
            }
        }, throwable -> {
            log.exception(throwable);
            fragment.close();
        });
    }

    private void auth(@Type String type, String login, String password) {
        log.v(TAG, "auth | type=", type, " | login=", login);
        if (StringUtils.isBlank(login) || StringUtils.isBlank(password)) {
            log.v(TAG, "auth | empty fields");
            notificationMessage.snackBar(activity, activity.getString(R.string.fill_fields));
            return;
        }
        switch (type) {
            case ISU: authIsu(login, password); break;
            /* Place for future types, if any */
        }
    }

    private void authIsu(String login, String password) {
        thread.standalone(() -> {
            storage.delete(activity, Storage.PERMANENT, Storage.USER, "user#isu#access_token");
            storage.delete(activity, Storage.PERMANENT, Storage.USER, "user#isu#refresh_token");
            storage.delete(activity, Storage.PERMANENT, Storage.USER, "user#isu#expires_at");
            isuPrivateRestClient.authorize(activity, login, password, new ResponseHandler() {
                @Override
                public void onSuccess(int statusCode, Client.Headers headers, String response) {
                    thread.runOnUI(() -> {
                        if ("authorized".equals(response)) {
                            firebaseAnalyticsProvider.logEvent(activity, FirebaseAnalyticsProvider.Event.LOGIN_ISU);
                            notificationMessage.toast(activity, activity.getString(R.string.account_linked));
                            fragment.close();
                        } else {
                            firebaseAnalyticsProvider.logEvent(
                                    activity,
                                    FirebaseAnalyticsProvider.Event.LOGIN_ISU_FAILED,
                                    firebaseAnalyticsProvider.getBundle(FirebaseAnalyticsProvider.Param.TYPE, "Failed " + response)
                            );
                            linkAccountForm.setVisibility(View.VISIBLE);
                            linkAccountProgress.setVisibility(View.GONE);
                            notificationMessage.snackBar(activity, activity.getString(R.string.auth_failed));
                        }
                    });
                }
                @Override
                public void onFailure(int statusCode, Client.Headers headers, int state) {
                    thread.runOnUI(() -> {
                        log.v(TAG, "auth | failure | state=", state, " | statusCode=", statusCode);
                        firebaseAnalyticsProvider.logEvent(
                                activity,
                                FirebaseAnalyticsProvider.Event.LOGIN_ISU_FAILED,
                                firebaseAnalyticsProvider.getBundle(FirebaseAnalyticsProvider.Param.TYPE, "State " + state)
                        );
                        linkAccountForm.setVisibility(View.VISIBLE);
                        linkAccountProgress.setVisibility(View.GONE);
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
                public void onProgress(int state) {
                    thread.runOnUI(() -> {
                        log.v(TAG, "authIsu | progress | state=", state);
                        linkAccountForm.setVisibility(View.GONE);
                        linkAccountProgress.setVisibility(View.VISIBLE);
                        TextView message = activity.findViewById(R.id.link_account_progress_message);
                        if (message != null) {
                            switch (state) {
                                default:
                                case Isu.STATE_HANDLING: message.setText(R.string.loading); break;
                                case Isu.STATE_AUTHORIZED: message.setText(R.string.authorized); break;
                                case Isu.STATE_AUTHORIZATION: message.setText(R.string.authorization); break;
                            }
                        }
                    });
                }
                @Override
                public void onNewRequest(Client.Request request) {
                    if (requestHandle != null) {
                        requestHandle.cancel();
                    }
                    requestHandle = request;
                }
            });
        });
    }

    @Override
    protected String getLogTag() {
        return TAG;
    }
}

package com.bukhmastov.cdoitmo.fragment.presenter.impl;

import android.os.Bundle;
import android.view.View;
import android.widget.Button;
import android.widget.EditText;
import android.widget.TextView;

import com.bukhmastov.cdoitmo.R;
import com.bukhmastov.cdoitmo.event.bus.EventBus;
import com.bukhmastov.cdoitmo.event.events.UserInfoChangedEvent;
import com.bukhmastov.cdoitmo.factory.AppComponentProvider;
import com.bukhmastov.cdoitmo.firebase.FirebaseAnalyticsProvider;
import com.bukhmastov.cdoitmo.fragment.presenter.LinkAccountFragmentPresenter;
import com.bukhmastov.cdoitmo.model.user.isu.IsuUserData;
import com.bukhmastov.cdoitmo.model.user.isu.IsuUserDataGroup;
import com.bukhmastov.cdoitmo.network.IsuPrivateRestClient;
import com.bukhmastov.cdoitmo.network.handlers.ResponseHandler;
import com.bukhmastov.cdoitmo.network.handlers.RestResponseHandler;
import com.bukhmastov.cdoitmo.network.model.Client;
import com.bukhmastov.cdoitmo.util.Account;
import com.bukhmastov.cdoitmo.util.Log;
import com.bukhmastov.cdoitmo.util.NotificationMessage;
import com.bukhmastov.cdoitmo.util.Storage;
import com.bukhmastov.cdoitmo.util.Thread;
import com.bukhmastov.cdoitmo.util.singleton.CollectionUtils;
import com.bukhmastov.cdoitmo.util.singleton.StringUtils;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;

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
    Account account;
    @Inject
    EventBus eventBus;
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
            case ISU: isuAuth(login, password); break;
            /* Place for future types, if any */
        }
    }

    private void isuAuth(String login, String password) {
        thread.standalone(() -> {
            storage.delete(activity, Storage.PERMANENT, Storage.USER, "user#isu#access_token");
            storage.delete(activity, Storage.PERMANENT, Storage.USER, "user#isu#refresh_token");
            storage.delete(activity, Storage.PERMANENT, Storage.USER, "user#isu#expires_at");
            isuPrivateRestClient.authorize(activity, login, password, new ResponseHandler() {
                @Override
                public void onSuccess(int code, Client.Headers headers, String response) {
                    if (!"authorized".equals(response)) {
                        thread.runOnUI(() -> {
                            firebaseAnalyticsProvider.logEvent(
                                    activity,
                                    FirebaseAnalyticsProvider.Event.LOGIN_ISU_FAILED,
                                    firebaseAnalyticsProvider.getBundle(FirebaseAnalyticsProvider.Param.TYPE, "Failed " + response)
                            );
                            linkAccountForm.setVisibility(View.VISIBLE);
                            linkAccountProgress.setVisibility(View.GONE);
                            notificationMessage.snackBar(activity, activity.getString(R.string.auth_failed));
                        });
                        return;
                    }
                    isuPrivateRestClient.get(activity, "userdata/%apikey%/%isutoken%", null, new RestResponseHandler<IsuUserData>() {
                        @Override
                        public void onSuccess(int code, Client.Headers headers, IsuUserData isuUserData) throws Exception {
                            if (isuUserData == null) {
                                onDone();
                                return;
                            }
                            String surname = StringUtils.defaultIfBlank(isuUserData.getSurname(), null);
                            String nameO = StringUtils.defaultIfBlank(isuUserData.getName(), null);
                            String patronymic = StringUtils.defaultIfBlank(isuUserData.getPatronymic(), null);
                            String name = "";
                            if (surname != null) {
                                name += surname.trim();
                                name = name.trim();
                            }
                            if (nameO != null) {
                                name += " " + nameO;
                                name = name.trim();
                            }
                            if (patronymic != null) {
                                name += " " + patronymic;
                                name = name.trim();
                            }
                            if (StringUtils.isBlank(name)) {
                                name = null;
                            }

                            List<String> groups = new ArrayList<>();
                            List<IsuUserDataGroup> isuGroups = CollectionUtils.emptyIfNull(isuUserData.getGroups());
                            Collections.reverse(isuGroups);
                            for (IsuUserDataGroup group : isuGroups) {
                                if (StringUtils.isBlank(group.getGroup())) {
                                    continue;
                                }
                                groups.add(group.getGroup());
                            }
                            if (CollectionUtils.isEmpty(groups)) {
                                groups = null;
                            }

                            String avatar = null;
                            if (isuUserData.getAvatar() != null && StringUtils.isNotBlank(isuUserData.getAvatar().getUrl())) {
                                avatar = isuUserData.getAvatar().getUrl();
                            }
                            if (StringUtils.isBlank(avatar)) {
                                avatar = null;
                            }

                            account.setUserInfo(activity, name, groups, avatar);
                            eventBus.fire(new UserInfoChangedEvent());

                            onDone();
                        }
                        @Override
                        public void onFailure(int code, Client.Headers headers, int state) {
                            onDone();
                        }
                        @Override
                        public void onProgress(int state) {
                            onProgressMessage(activity.getString(R.string.data_initializing));
                        }
                        @Override
                        public void onNewRequest(Client.Request request) {
                            if (requestHandle != null) {
                                requestHandle.cancel();
                            }
                            requestHandle = request;
                        }
                        @Override
                        public IsuUserData newInstance() {
                            return new IsuUserData();
                        }
                        private void onDone() {
                            thread.runOnUI(() -> {
                                firebaseAnalyticsProvider.logEvent(activity, FirebaseAnalyticsProvider.Event.LOGIN_ISU);
                                notificationMessage.toast(activity, activity.getString(R.string.account_linked));
                                fragment.close();
                            });
                        }
                    });
                }
                @Override
                public void onFailure(int code, Client.Headers headers, int state) {
                    thread.runOnUI(() -> {
                        log.v(TAG, "auth | failure | state=", state, " | code=", code);
                        firebaseAnalyticsProvider.logEvent(
                                activity,
                                FirebaseAnalyticsProvider.Event.LOGIN_ISU_FAILED,
                                firebaseAnalyticsProvider.getBundle(FirebaseAnalyticsProvider.Param.TYPE, "State " + state)
                        );
                        linkAccountForm.setVisibility(View.VISIBLE);
                        linkAccountProgress.setVisibility(View.GONE);
                        notificationMessage.snackBar(activity, isuPrivateRestClient.getFailedMessage(activity, code, state));
                    });
                }
                @Override
                public void onProgress(int state) {
                    log.v(TAG, "isuAuth | progress | state=", state);
                    onProgressMessage(isuPrivateRestClient.getProgressMessage(activity, state));
                }
                @Override
                public void onNewRequest(Client.Request request) {
                    if (requestHandle != null) {
                        requestHandle.cancel();
                    }
                    requestHandle = request;
                }
                private void onProgressMessage(String msg) {
                    thread.runOnUI(() -> {
                        linkAccountForm.setVisibility(View.GONE);
                        linkAccountProgress.setVisibility(View.VISIBLE);
                        TextView message = activity.findViewById(R.id.link_account_progress_message);
                        if (message != null) {
                            message.setText(msg);
                        }
                    });
                }
            });
        });
    }

    @Override
    protected String getLogTag() {
        return TAG;
    }
}

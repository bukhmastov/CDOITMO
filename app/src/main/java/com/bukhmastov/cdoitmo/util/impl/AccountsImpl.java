package com.bukhmastov.cdoitmo.util.impl;

import android.content.Context;
import android.os.Bundle;
import androidx.annotation.NonNull;

import com.bukhmastov.cdoitmo.factory.AppComponentProvider;
import com.bukhmastov.cdoitmo.firebase.FirebaseAnalyticsProvider;
import com.bukhmastov.cdoitmo.model.user.UsersList;
import com.bukhmastov.cdoitmo.util.Account;
import com.bukhmastov.cdoitmo.util.Accounts;
import com.bukhmastov.cdoitmo.util.DateUtils;
import com.bukhmastov.cdoitmo.util.Log;
import com.bukhmastov.cdoitmo.util.Storage;
import com.bukhmastov.cdoitmo.util.Thread;
import com.bukhmastov.cdoitmo.util.singleton.CollectionUtils;
import com.bukhmastov.cdoitmo.util.singleton.StringUtils;

import java.util.ArrayList;
import java.util.List;
import java.util.Objects;

import javax.inject.Inject;

public class AccountsImpl implements Accounts {

    private static final String TAG = "Accounts";

    @Inject
    Log log;
    @Inject
    Thread thread;
    @Inject
    Storage storage;
    @Inject
    DateUtils dateUtils;
    @Inject
    FirebaseAnalyticsProvider firebaseAnalyticsProvider;

    public AccountsImpl() {
        AppComponentProvider.getComponent().inject(this);
    }

    @Override
    public void add(@NonNull Context context, @NonNull String login) {
        thread.assertNotUI();
        if (Account.USER_UNAUTHORIZED.equals(login)) {
            return;
        }
        try {
            log.v(TAG, "add | login=", login);
            boolean isNewAuthorization = true;
            // save login on top of the list of authorized users
            UsersList list = get(context);
            ArrayList<String> accounts = new ArrayList<>();
            accounts.add(login);
            if (CollectionUtils.isNotEmpty(list.getLogins())) {
                for (String entry : list.getLogins()) {
                    if (Objects.equals(entry, login)) {
                        isNewAuthorization = false;
                    } else {
                        accounts.add(entry);
                    }
                }
            }
            list.setLogins(accounts);
            storage.put(context, Storage.PERMANENT, Storage.GLOBAL, "users#list", list.toJsonString());
            // track statistics
            Bundle bundle;
            bundle = firebaseAnalyticsProvider.getBundle(FirebaseAnalyticsProvider.Param.LOGIN_COUNT, list.getLogins().size());
            bundle = firebaseAnalyticsProvider.getBundle(FirebaseAnalyticsProvider.Param.LOGIN_NEW, isNewAuthorization ? "new" : "old", bundle);
            firebaseAnalyticsProvider.logEvent(
                    context,
                    FirebaseAnalyticsProvider.Event.LOGIN,
                    bundle
            );
        } catch (Throwable throwable) {
            log.exception(throwable);
        }
    }

    @Override
    public void remove(@NonNull Context context, @NonNull String login) {
        thread.assertNotUI();
        if (Account.USER_UNAUTHORIZED.equals(login)) {
            return;
        }
        try {
            log.v(TAG, "remove | login=", login);
            // remove login from the list of authorized users
            UsersList list = get(context);
            if (CollectionUtils.isNotEmpty(list.getLogins())) {
                List<String> accounts = list.getLogins();
                for (String entry : accounts) {
                    if (Objects.equals(entry, login)) {
                        accounts.remove(entry);
                        break;
                    }
                }
                storage.put(context, Storage.PERMANENT, Storage.GLOBAL, "users#list", list.toJsonString());
            }
            // track statistics
            firebaseAnalyticsProvider.logEvent(
                    context,
                    FirebaseAnalyticsProvider.Event.LOGOUT,
                    firebaseAnalyticsProvider.getBundle(FirebaseAnalyticsProvider.Param.LOGIN_COUNT, list.getLogins().size())
            );
        } catch (Throwable throwable) {
            log.exception(throwable);
        }
    }

    @Override
    public @NonNull UsersList get(@NonNull Context context) {
        log.v(TAG, "get");
        UsersList usersList = new UsersList();
        usersList.setLogins(new ArrayList<>());
        try {
            String accounts = storage.get(context, Storage.PERMANENT, Storage.GLOBAL, "users#list", "");
            if (StringUtils.isNotBlank(accounts)) {
                usersList.fromJsonString(accounts);
            }
            if (usersList.getLogins() == null) {
                usersList.setLogins(new ArrayList<>());
            }
        } catch (Exception e) {
            log.exception(e);
        }
        return usersList;
    }
}

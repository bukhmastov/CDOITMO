package com.bukhmastov.cdoitmo.util.impl;

import android.content.Context;
import android.os.Bundle;
import android.support.annotation.NonNull;

import com.bukhmastov.cdoitmo.factory.AppComponentProvider;
import com.bukhmastov.cdoitmo.firebase.FirebaseAnalyticsProvider;
import com.bukhmastov.cdoitmo.util.Account;
import com.bukhmastov.cdoitmo.util.Accounts;
import com.bukhmastov.cdoitmo.util.Log;
import com.bukhmastov.cdoitmo.util.Storage;
import com.bukhmastov.cdoitmo.util.Thread;
import com.bukhmastov.cdoitmo.util.TextUtils;

import org.json.JSONArray;

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
    TextUtils textUtils;
    @Inject
    FirebaseAnalyticsProvider firebaseAnalyticsProvider;

    public AccountsImpl() {
        AppComponentProvider.getComponent().inject(this);
    }

    @Override
    public void add(@NonNull final Context context, @NonNull final String login) {
        if (Account.USER_UNAUTHORIZED.equals(login)) return;
        thread.run(() -> {
            try {
                log.v(TAG, "add | login=", login);
                boolean isNewAuthorization = true;
                // save login on top of the list of authorized users
                JSONArray list = get(context);
                JSONArray accounts = new JSONArray();
                accounts.put(login);
                for (int i = 0; i < list.length(); i++) {
                    String entry = list.getString(i);
                    if (entry.equals(login)) {
                        isNewAuthorization = false;
                    } else {
                        accounts.put(entry);
                    }
                }
                storage.put(context, Storage.PERMANENT, Storage.GLOBAL, "users#list", accounts.toString());
                // track statistics
                Bundle bundle;
                bundle = firebaseAnalyticsProvider.getBundle(FirebaseAnalyticsProvider.Param.LOGIN_COUNT, accounts.length());
                bundle = firebaseAnalyticsProvider.getBundle(FirebaseAnalyticsProvider.Param.LOGIN_NEW, isNewAuthorization ? "new" : "old", bundle);
                firebaseAnalyticsProvider.logEvent(
                        context,
                        FirebaseAnalyticsProvider.Event.LOGIN,
                        bundle
                );
            } catch (Exception e) {
                log.exception(e);
            }
        });
    }

    @Override
    public void remove(@NonNull final Context context, @NonNull final String login) {
        if (Account.USER_UNAUTHORIZED.equals(login)) return;
        thread.run(() -> {
            try {
                log.v(TAG, "remove | login=", login);
                // remove login from the list of authorized users
                JSONArray list = get(context);
                for (int i = 0; i < list.length(); i++) {
                    if (list.getString(i).equals(login)) {
                        list.remove(i);
                        break;
                    }
                }
                storage.put(context, Storage.PERMANENT, Storage.GLOBAL, "users#list", list.toString());
                // track statistics
                firebaseAnalyticsProvider.logEvent(
                        context,
                        FirebaseAnalyticsProvider.Event.LOGOUT,
                        firebaseAnalyticsProvider.getBundle(FirebaseAnalyticsProvider.Param.LOGIN_COUNT, list.length())
                );
            } catch (Exception e) {
                log.exception(e);
            }
        });
    }

    @Override
    public JSONArray get(@NonNull Context context) {
        try {
            log.v(TAG, "get");
            try {
                return textUtils.string2jsonArray(storage.get(context, Storage.PERMANENT, Storage.GLOBAL, "users#list", ""));
            } catch (Exception e) {
                return new JSONArray();
            }
        } catch (Exception e) {
            log.exception(e);
            return new JSONArray();
        }
    }
}

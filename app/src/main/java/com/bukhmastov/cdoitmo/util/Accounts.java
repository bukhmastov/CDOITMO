package com.bukhmastov.cdoitmo.util;

import android.content.Context;
import android.os.Bundle;
import android.support.annotation.NonNull;

import com.bukhmastov.cdoitmo.firebase.FirebaseAnalyticsProvider;

import org.json.JSONArray;

//TODO interface - impl
public class Accounts {

    private static final String TAG = "Accounts";

    //@Inject
    //TODO interface - impl: remove static
    private static Log log = Log.instance();
    //@Inject
    //TODO interface - impl: remove static
    private static Storage storage = Storage.instance();
    //@Inject
    //TODO interface - impl: remove static
    private static FirebaseAnalyticsProvider firebaseAnalyticsProvider = FirebaseAnalyticsProvider.instance();

    public static void push(@NonNull final Context context, @NonNull final String login) {
        if (Account.USER_UNAUTHORIZED.equals(login)) return;
        Thread.run(() -> {
            try {
                log.v(TAG, "push | login=", login);
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
    public static void remove(@NonNull final Context context, @NonNull final String login) {
        if (Account.USER_UNAUTHORIZED.equals(login)) return;
        Thread.run(() -> {
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
    public static JSONArray get(@NonNull Context context) {
        try {
            log.v(TAG, "get");
            try {
                return TextUtils.string2jsonArray(storage.get(context, Storage.PERMANENT, Storage.GLOBAL, "users#list", ""));
            } catch (Exception e) {
                return new JSONArray();
            }
        } catch (Exception e) {
            log.exception(e);
            return new JSONArray();
        }
    }
}

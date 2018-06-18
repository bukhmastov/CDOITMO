package com.bukhmastov.cdoitmo.data;

import android.support.annotation.NonNull;

import com.bukhmastov.cdoitmo.util.Log;
import com.bukhmastov.cdoitmo.util.TextUtils;

import org.json.JSONArray;
import org.json.JSONException;

import java.util.Iterator;

import static com.bukhmastov.cdoitmo.util.Storage.StorageType.GLOBAL;

public class AccountList implements Iterable<AccountList.AccountEntry> {
    private final StorageProxy storage;
    private int size = -1;

    public AccountList(StorageProxy storage) {
        this.storage = storage;
    }

    public int length() {
        if (size >= 0) return size;
        return get().length();
    }

    public JSONArray get() {
        try {
            return TextUtils.string2jsonArray(storage.get(GLOBAL, "users#list"));
        } catch (Exception e) {
            Log.exception(e);
            return new JSONArray();
        }
    }

    public boolean add(@NonNull String login) {
        if (login.equals(UserCredentials.LOGIN_UNAUTHORIZED)) return false;
        try {
            boolean isNewAuthorization = true;
            // save login on top of the list of authorized users
            JSONArray list = get();
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
            storage.put(GLOBAL, "users#list", accounts.toString());
            size = accounts.length();
            return isNewAuthorization;
        } catch (Exception e) {
            Log.exception(e);
        }
        return false;
    }

    public void remove(@NonNull final String login) {
        if (login.equals(UserCredentials.LOGIN_UNAUTHORIZED)) return;
        try {
            JSONArray accounts = get();
            for (int i = 0; i < accounts.length(); i++) {
                if (accounts.getString(i).equals(login)) {
                    accounts.remove(i);
                    break;
                }
            }
            storage.put(GLOBAL, "users#list", accounts.toString());
            size = accounts.length();
        } catch (Exception e) {
            Log.exception(e);
        }
    }

    public static class AccountEntry {
        public final UserCredentials creds;
        public final String name;

        AccountEntry(UserCredentials creds, String name) {
            this.creds = creds;
            this.name = name;
        }
    }

    @NonNull
    @Override
    public Iterator<AccountEntry> iterator() {
        return new Iterator<AccountEntry>() {
            private final JSONArray accounts = get();
            private int index = 0;

            @Override
            public boolean hasNext() {
                return index < accounts.length();
            }

            @Override
            public AccountEntry next() {
                try {
                    final String login = accounts.getString(index++);
                    UserCredentials.setCurrentLogin(storage, login);
                    UserCredentials creds = UserCredentials.load(storage);
                    String name = User.getName(storage);
                    UserCredentials.resetCurrentLogin(storage);
                    return new AccountEntry(creds, name);
                }
                catch (JSONException e) {
                    return null;
                }
            }
        };
    }
}

package com.bukhmastov.cdoitmo.data;

import android.support.annotation.Nullable;

import static com.bukhmastov.cdoitmo.util.Storage.StorageType.*;

public class UserCredentials {
    private final String login;
    private final String password;
    private final String role;

    public static final String LOGIN_UNAUTHORIZED = "unauthorized";

    public static final UserCredentials UNAUTHORIZED =
            new UserCredentials(LOGIN_UNAUTHORIZED, LOGIN_UNAUTHORIZED, "anonymous");

    public UserCredentials(String login, String password, String role) {
        this.login = login;
        this.password = password;
        this.role = role;
    }

    public String getLogin() { return login; }

    public String getPassword() { return password; }

    public String getRole() { return role; }

    public boolean areInvalid() { return this.login.isEmpty() || this.password.isEmpty(); }

    public static boolean setCurrentLogin(StorageProxy proxy, String login) {
        return proxy.put(GLOBAL, "users#current_login", login);
    }

    public static boolean resetCurrentLogin(StorageProxy proxy) {
        return proxy.delete(GLOBAL, "users#current_login");
    }

    public static boolean hasCurrentLogin(StorageProxy proxy) {
        return !getCurrentLogin(proxy).isEmpty();
    }

    public static void changePasswordByLogin(StorageProxy proxy, String login, String password) {
        setCurrentLogin(proxy, login);
        proxy.put(PER_USER, "user#deifmo#password", password);
        resetCurrentLogin(proxy);
    }

    public static String getCurrentLogin(StorageProxy proxy) {
        return proxy.get(GLOBAL, "users#current_login");
    }

    public static void clearCookies(StorageProxy proxy) {
        if (!hasCurrentLogin(proxy)) return;
        proxy.delete(PER_USER, "user#deifmo#cookies");
    }

    public static void clearPassword(StorageProxy proxy) {
        if (!hasCurrentLogin(proxy)) return;
        proxy.delete(PER_USER, "user#deifmo#password");
    }

    public static @Nullable UserCredentials load(StorageProxy proxy) {
        String login = proxy.get(PER_USER, "user#deifmo#login");
        String password = proxy.get(PER_USER, "user#deifmo#password");

        if (login.isEmpty() || password.isEmpty()) return null;

        return new UserCredentials(login, password, proxy.get(PER_USER, "user#role"));
    }

    public void store(StorageProxy proxy) {
        proxy.put(PER_USER, "user#deifmo#login", login);
        proxy.put(PER_USER, "user#deifmo#password", password);
        proxy.put(PER_USER, "user#role", role);
    }
}

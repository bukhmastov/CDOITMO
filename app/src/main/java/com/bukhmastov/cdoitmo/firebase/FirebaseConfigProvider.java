package com.bukhmastov.cdoitmo.firebase;

import com.bukhmastov.cdoitmo.firebase.impl.FirebaseConfigProviderImpl;

import org.json.JSONObject;

public interface FirebaseConfigProvider {

    // future: replace with DI factory
    FirebaseConfigProvider instance = new FirebaseConfigProviderImpl();
    static FirebaseConfigProvider instance() {
        return instance;
    }

    String MESSAGE_LOGIN = "message_login";
    String MESSAGE_MENU = "message_menu";
    String PERFORMANCE_ENABLED = "performance_enabled";

    interface Result {
        void onResult(String value);
    }
    interface ResultJson {
        void onResult(JSONObject value);
    }

    void getString(String key, Result result);
    void getJson(String key, ResultJson result);
}

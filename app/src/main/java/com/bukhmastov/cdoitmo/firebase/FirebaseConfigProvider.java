package com.bukhmastov.cdoitmo.firebase;

import org.json.JSONObject;

public interface FirebaseConfigProvider {

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

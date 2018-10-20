package com.bukhmastov.cdoitmo.firebase;

import com.bukhmastov.cdoitmo.model.firebase.config.FBConfigMessage;

public interface FirebaseConfigProvider {

    String MESSAGE_LOGIN = "message_login";
    String MESSAGE_MENU = "message_menu";
    String PERFORMANCE_ENABLED = "performance_enabled";

    interface Result {
        void onResult(String value);
    }
    interface ResultMessage {
        void onResult(FBConfigMessage configMessage);
    }

    void getString(String key, Result result);
    void getMessage(String key, ResultMessage result);
}

package com.bukhmastov.cdoitmo.fragment.presenter;

import android.os.Bundle;
import androidx.annotation.Nullable;
import androidx.annotation.StringDef;

import com.bukhmastov.cdoitmo.fragment.ConnectedFragment;

import org.json.JSONObject;

import java.lang.annotation.Retention;
import java.lang.annotation.RetentionPolicy;

public interface RatingFragmentPresenter {

    void setFragment(ConnectedFragment fragment);

    void onCreate(@Nullable Bundle savedInstanceState);

    void onDestroy();

    void onResume();

    void onPause();

    @Retention(RetentionPolicy.SOURCE)
    @StringDef({COMMON, OWN})
    @interface TYPE {}
    String COMMON = "common";
    String OWN = "own";

    @Retention(RetentionPolicy.SOURCE)
    @StringDef({EMPTY, LOADED, FAILED, OFFLINE, SERVER_ERROR})
    @interface STATUS {}
    String EMPTY = "empty";
    String LOADED = "loaded";
    String FAILED = "failed";
    String OFFLINE = "offline";
    String SERVER_ERROR = "server_error";

    class Info {
        public @STATUS String status;
        public JSONObject data;
        public Info(@STATUS String status, JSONObject data) {
            this.status = status;
            this.data = data;
        }
    }
}

package com.bukhmastov.cdoitmo.fragment.presenter;

import android.os.Bundle;
import android.support.annotation.Nullable;
import android.support.annotation.StringDef;

import com.bukhmastov.cdoitmo.fragment.ConnectedFragment;
import com.bukhmastov.cdoitmo.model.JsonEntity;

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

    class Info<T extends JsonEntity> {
        public @STATUS String status;
        public T data;
        public Info(@STATUS String status) {
            this.status = status;
        }
        public Info(@STATUS String status, T data) {
            this.status = status;
            this.data = data;
        }
    }
}

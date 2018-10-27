package com.bukhmastov.cdoitmo.fragment.presenter;

import androidx.annotation.StringDef;

import com.bukhmastov.cdoitmo.model.JsonEntity;

import java.lang.annotation.Retention;
import java.lang.annotation.RetentionPolicy;

public interface RatingFragmentPresenter extends ConnectedFragmentPresenter {

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

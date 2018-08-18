package com.bukhmastov.cdoitmo.object;

import org.json.JSONObject;

import java.lang.annotation.Retention;
import java.lang.annotation.RetentionPolicy;

import android.support.annotation.IntDef;

public interface TeacherSearch {

    void search(final String query, final TeacherSearchCallback callback);

    void setQuery(String query);

    void blockNextCall();

    interface TeacherSearchCallback {
        void onState(@State int state);
        void onSuccess(JSONObject json);
    }

    @Retention(RetentionPolicy.SOURCE)
    @IntDef({REJECTED, ACCEPTED, SEARCHING, NOT_FOUND, FOUND})
    @interface State {}
    int REJECTED = 0;
    int ACCEPTED = 1;
    int SEARCHING = 2;
    int NOT_FOUND = 3;
    int FOUND = 4;
}

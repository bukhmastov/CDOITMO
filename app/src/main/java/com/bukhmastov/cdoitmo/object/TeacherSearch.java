package com.bukhmastov.cdoitmo.object;

import java.lang.annotation.Retention;
import java.lang.annotation.RetentionPolicy;

import android.support.annotation.IntDef;

import com.bukhmastov.cdoitmo.model.schedule.teachers.STeachers;

public interface TeacherSearch {

    void search(final String query, final TeacherSearchCallback callback);

    //void setQuery(String query);

    interface TeacherSearchCallback {
        void onState(@State int state);
        void onSuccess(STeachers teachers);
    }

    @Retention(RetentionPolicy.SOURCE)
    @IntDef({REJECTED_EMPTY, REJECTED, ACCEPTED, SEARCHING, NOT_FOUND, FOUND})
    @interface State {}
    int REJECTED_EMPTY = 0;
    int REJECTED = 1;
    int ACCEPTED = 2;
    int SEARCHING = 3;
    int NOT_FOUND = 4;
    int FOUND = 5;
}
